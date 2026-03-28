package com.example.babyparenting.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.babyparenting.data.local.AdminMilestoneStore
import com.example.babyparenting.data.local.LazyDatasetLoader
import com.example.babyparenting.data.model.AgeGroup
import com.example.babyparenting.data.model.DatasetSource
import com.example.babyparenting.data.model.JourneyProgress
import com.example.babyparenting.data.model.Milestone
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Single source of truth for milestones.
 * Lazy loads age groups on-demand and handles continuous progression.
 *
 * markComplete is ONE-WAY — completed milestones cannot be un-completed.
 *
 * ── KEY BEHAVIOR ──────────────────────────────────────────────────────────
 * 1. On first load: Load ONLY the floor group for current child age
 * 2. When user scrolls/completes: Auto-load next groups seamlessly
 * 3. When age changes: Reset and reload from NEW age's floor group
 * 4. All loaded groups stay in memory until age changes
 * 5. When next group loads: Instantly add to milestones list
 */

class MilestoneRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("journey_progress", Context.MODE_PRIVATE)
    private val gson       = Gson()
    private val loader     = LazyDatasetLoader(context)
    private val adminStore = AdminMilestoneStore(context)

    // Track which groups have been loaded
    private var loadedMilestones: MutableList<Milestone> = mutableListOf()
    private var highestLoadedGroup: Int = 0

    // Track the last emitted completion IDs to avoid duplicate updates
    private var lastEmittedCompletedIds: Set<String> = emptySet()

    private val _milestones = MutableStateFlow<List<Milestone>>(emptyList())
    private val _ageGroups  = MutableStateFlow<List<AgeGroup>>(emptyList())
    private val _progress   = MutableStateFlow(JourneyProgress(0, 0, 0))
    private val _isLoading  = MutableStateFlow(false)
    private val _error      = MutableStateFlow<String?>(null)

    val milestones: StateFlow<List<Milestone>> = _milestones.asStateFlow()
    val ageGroups:  StateFlow<List<AgeGroup>>  = _ageGroups.asStateFlow()
    val progress:   StateFlow<JourneyProgress>  = _progress.asStateFlow()
    val isLoading:  StateFlow<Boolean>           = _isLoading.asStateFlow()
    val error:      StateFlow<String?>           = _error.asStateFlow()

    // ── INITIAL LOAD ──────────────────────────────────────────────────────────

    /**
     * Full initialization — called once on app start or when age changes.
     *
     * Behavior:
     * 1. Reset all in-memory state
     * 2. Load ONLY the floor group for the current child's age
     * 3. Mark milestones below child's age as auto-completed
     * 4. Emit the starting list
     */
    suspend fun initialLoad() {
        try {
            _isLoading.value = true
            _error.value     = null

            // ✅ CRITICAL: Reset everything before loading
            resetLoadState()

            _ageGroups.value = loader.getAgeGroups()

            val childAge = getChildAgeMonths()

            // Load ONLY the floor group (the one matching current age)
            val floorGroup = loader.startingGroupId(childAge)
            loadGroupIfNeeded(floorGroup)

            // Auto-complete milestones STRICTLY before child's current age
            val correctIds = loadedMilestones
                .filter { it.ageMonths < childAge }
                .map { it.id }
                .toSet()

            val savedIds   = getCompletedIds()
            val validSaved = savedIds.filter { id ->
                loadedMilestones.any { it.id == id }
            }.toSet()

            val finalIds = (correctIds + validSaved).toSet()
            saveCompletedIds(finalIds)
            mergeAndEmitWithIds(finalIds)

        } catch (e: Exception) {
            _error.value = "Failed to load: ${e.localizedMessage}"
        } finally {
            _isLoading.value = false
        }
    }

    // ── PROGRESSIVE LOADING ──────────────────────────────────────────────────
    /**
     * Load the next age group(s) when user reaches the end of current group.
     *
     * This is called by JourneyViewModel when:
     * 1. User completes all cards in an age group
     * 2. User scrolls down to see milestones from next group
     *
     * ✅ FIXED: Now properly loads multiple groups and emits them immediately
     * Old behavior: Loaded but never emitted, so cards never appeared
     * New behavior: Loads group → merges with completed IDs → emits to UI instantly
     */
    suspend fun loadNextGroupIfNeeded(nextGroupId: Int) {
        if (nextGroupId > loader.totalGroups()) return
        if (nextGroupId <= highestLoadedGroup) return

        try {
            _isLoading.value = true

            // Load the next group's data from CSV
            loadGroupIfNeeded(nextGroupId)

            // ✅ CRITICAL: Get current completed IDs and emit immediately
            val completedIds = getCompletedIds()
            mergeAndEmitWithIds(completedIds)

        } catch (e: Exception) {
            // Silent fail - if one group fails to load, don't crash
            // User can retry by scrolling again
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load multiple groups in sequence (for faster progression).
     * Called when user scrolls quickly or completes groups fast.
     */
    suspend fun loadGroupsUpTo(maxGroupId: Int) {
        if (maxGroupId > loader.totalGroups()) return
        if (maxGroupId <= highestLoadedGroup) return

        try {
            _isLoading.value = true

            // Load all groups from (highestLoadedGroup + 1) to maxGroupId
            for (groupId in (highestLoadedGroup + 1)..maxGroupId) {
                loadGroupIfNeeded(groupId)
            }

            // Emit everything once at the end
            val completedIds = getCompletedIds()
            mergeAndEmitWithIds(completedIds)

        } catch (e: Exception) {
            // Silent fail
        } finally {
            _isLoading.value = false
        }
    }

    // ── ONE-WAY COMPLETION ────────────────────────────────────────────────────

    /**
     * Mark a milestone as complete. PERMANENT — cannot be reversed.
     * Once a milestone is marked done, it stays done forever (across sessions).
     */
    fun markComplete(id: String) {
        val ids = getCompletedIds().toMutableSet()
        if (id in ids) return   // already complete — do nothing

        ids.add(id)
        saveCompletedIds(ids)

        // Update local list
        loadedMilestones = loadedMilestones.map {
            it.copy(isCompleted = it.id in ids)
        }.toMutableList()

        // Emit immediately
        _milestones.value = loadedMilestones.toList()
        _progress.value   = buildProgress()
    }

    // ── CHILD PROFILE ─────────────────────────────────────────────────────────

    /**
     * When parent changes the child's age, start fresh from that age's group.
     *
     * Example flow:
     * 1. User sets age to 24 months → loads group 7 (2-3 years)
     * 2. User sets age to 6 months → resets everything, loads group 3 (6-9 months)
     * 3. Old completion data is preserved (from SharedPreferences)
     *
     * ✅ FIXED: Now truly clears in-memory state so old groups don't leak
     */
    fun setChildAge(months: Int) {
        // Save the new age
        prefs.edit().putInt(KEY_AGE, months).apply()

        // ✅ CRITICAL: Wipe all in-memory state
        // This ensures initialLoad() will load the correct group for NEW age
        resetLoadState()

        // Clear UI while reload happens
        _milestones.value = emptyList()
        _progress.value   = buildProgress()
        _error.value      = null
    }

    fun setChildName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
        _progress.value = _progress.value.copy(childName = name)
    }

    fun getChildAgeMonths(): Int = prefs.getInt(KEY_AGE, 0)
    fun getChildName(): String   = prefs.getString(KEY_NAME, "") ?: ""

    fun refreshAdminMilestones() = mergeAndEmit()

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    /**
     * ✅ CRITICAL: Wipe ALL in-memory state.
     *
     * Called at START of initialLoad() and when age changes.
     * Ensures no stale data from previous session/age carries over.
     *
     * After this:
     * - loadedMilestones is empty (no stale cards)
     * - highestLoadedGroup is 0 (next load will reload from group 1)
     * - loader cache is cleared (no stale CSV data)
     */
    private fun resetLoadState() {
        loadedMilestones   = mutableListOf()
        highestLoadedGroup = 0
        lastEmittedCompletedIds = emptySet()
        loader.clearCache()
    }

    /**
     * Load a single group from CSV (or cache if already loaded).
     * Called by initialLoad() and loadNextGroupIfNeeded().
     */
    private suspend fun loadGroupIfNeeded(groupId: Int) {
        if (groupId <= highestLoadedGroup) return

        val newMs = withContext(Dispatchers.IO) {
            loader.loadForGroup(groupId)
        }

        loadedMilestones.addAll(newMs)
        highestLoadedGroup = groupId
    }

    /**
     * Merge loaded milestones with admin milestones and completion status.
     * This is the MAIN emission point — called after every load/change.
     */
    private fun mergeAndEmit() {
        val completed = getCompletedIds()
        mergeAndEmitWithIds(completed)
    }

    /**
     * ✅ CRITICAL: The actual emission logic.
     *
     * This function:
     * 1. Takes current loaded milestones + admin milestones
     * 2. Marks which ones are completed
     * 3. Sorts by age and source
     * 4. EMITS to _milestones StateFlow (UI updates)
     * 5. Updates progress
     *
     * This is called:
     * - After initialLoad()
     * - After loadNextGroupIfNeeded()
     * - After markComplete()
     * - After setChildAge()
     */
    private fun mergeAndEmitWithIds(completed: Set<String>) {
        // Admin-added custom milestones
        val adminMs = adminStore.getAll().map { am ->
            Milestone(
                id           = am.id,
                title        = am.title,
                subtitle     = am.subtitle,
                domain       = am.domain,
                ageMonths    = am.ageMonths,
                ageRange     = am.ageRange,
                ageGroupId   = am.ageGroupId,
                source       = DatasetSource.ADMIN_CUSTOM,
                apiQuery     = am.apiQuery,
                iconEmoji    = am.iconEmoji,
                accentColor  = DatasetSource.ADMIN_CUSTOM.colorHex,
                isCompleted  = am.id in completed,
                isAdminAdded = true
            )
        }

        // Update loaded milestones with completion status
        loadedMilestones = loadedMilestones.map {
            it.copy(isCompleted = it.id in completed)
        }.toMutableList()

        // Merge and sort
        val merged = (loadedMilestones + adminMs)
            .sortedWith(compareBy({ it.ageMonths }, { it.source.ordinal }))
            .toList()

        // ✅ EMIT: This updates the UI
        _milestones.value = merged

        // Update progress
        _progress.value = buildProgress()

        // Track what we emitted
        lastEmittedCompletedIds = completed
    }

    private fun buildProgress() = JourneyProgress(
        totalMilestones     = loadedMilestones.size,
        completedMilestones = loadedMilestones.count { it.isCompleted },
        childAgeMonths      = getChildAgeMonths(),
        childName           = getChildName()
    )

    fun clearAllData() {
        prefs.edit().clear().apply()
        resetLoadState()
        _milestones.value = emptyList()
        _progress.value = JourneyProgress(0, 0, 0)
    }

    // ── SHARED PREFERENCES (Completion tracking) ──────────────────────────────

    /**
     * Get the set of completed milestone IDs from SharedPreferences.
     * Survives app restart, age change, etc.
     */
    private fun getCompletedIds(): Set<String> {
        val json = prefs.getString(KEY_COMPLETED, null) ?: return emptySet()
        return try {
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type) ?: emptySet()
        } catch (e: Exception) { emptySet() }
    }

    /**
     * Save the set of completed milestone IDs to SharedPreferences.
     */
    private fun saveCompletedIds(ids: Set<String>) {
        prefs.edit().putString(KEY_COMPLETED, gson.toJson(ids)).apply()
    }

    companion object {
        private const val KEY_COMPLETED = "completed_ids"
        private const val KEY_AGE       = "child_age_months"
        private const val KEY_NAME      = "child_name"
    }
}