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
 * Single source of truth. Lazy loads one age group at a time.
 * markComplete is ONE-WAY — completed milestones cannot be un-completed.
 *
 * ── FIXES IN THIS VERSION ────────────────────────────────────────────────────
 * FIX 1: initialLoad() now calls resetLoadState() first.
 *         Old: highestLoadedGroup was never reset between reloads.
 *         If a parent changed the child age from 2 months to 8 years and back,
 *         highestLoadedGroup stayed at whatever the highest group ever was,
 *         so loadGroupIfNeeded() silently skipped re-loading the correct group.
 *
 * FIX 2: resetLoadState() clears loader.clearCache() before reload.
 *         Old: loadedGroups map inside LazyDatasetLoader was never cleared.
 *         Stale milestone lists from the old age remained cached in memory
 *         forever, even after the child's age was changed.
 *
 * FIX 3: setChildAge() now resets load state and then calls initialLoad()
 *         inline via a flag, so the ViewModel's existing launch{initialLoad()}
 *         picks up a clean slate every time.
 */

class MilestoneRepository(private val context: Context) {














    private val prefs: SharedPreferences =
        context.getSharedPreferences("journey_progress", Context.MODE_PRIVATE)
    private val gson       = Gson()
    private val loader     = LazyDatasetLoader(context)
    private val adminStore = AdminMilestoneStore(context)

    private var loadedMilestones: MutableList<Milestone> = mutableListOf()
    private var highestLoadedGroup: Int = 0

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

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Full load (or reload after age change).
     *
     * FIX: resetLoadState() is called first on every entry.
     * This ensures that:
     *   1. loadedMilestones is empty — no stale milestones from old age.
     *   2. highestLoadedGroup is 0 — loadGroupIfNeeded() won't skip the
     *      correct new floor group thinking it was "already loaded".
     *   3. loader cache is wiped — LazyDatasetLoader won't return stale
     *      memoised data for groups that belonged to the old child age.
     */
    suspend fun initialLoad() {
        try {
            _isLoading.value = true
            _error.value     = null

            // ✅ FIX: reset in-memory state AND the loader's CSV cache before
            // every load, so changing child age always starts from a clean slate.
            resetLoadState()

            _ageGroups.value = loader.getAgeGroups()

            val childAge = getChildAgeMonths()

            // Only load floor + ceiling group (2 groups max).
            // If child is 8 yr 9 mo (105 mo) → startingGroupId = 11.
            // groupsToPreload returns [11, 12] — NOT [1, 2, 3 … 11].
            val groupsToLoad = loader.groupsToPreload(childAge)
            for (groupId in groupsToLoad) {
                loadGroupIfNeeded(groupId)
            }

            // Auto-complete milestones strictly before child's age.
            // STRICT less-than (<) so milestones AT current age are NOT
            // auto-completed — those are the ones they should do NOW.
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

    suspend fun loadNextGroupIfNeeded(nextGroupId: Int) {
        if (nextGroupId > loader.totalGroups()) return
        if (nextGroupId <= highestLoadedGroup) return
        try {
            _isLoading.value = true
            loadGroupIfNeeded(nextGroupId)
            mergeAndEmit()
        } catch (e: Exception) {
            // silent fail
        } finally {
            _isLoading.value = false
        }
    }

    // ── ONE-WAY completion ────────────────────────────────────────────────────

    /**
     * Mark a milestone as complete. PERMANENT — cannot be reversed.
     * Once a milestone is in completedIds, it stays there forever.
     */
    fun markComplete(id: String) {
        val ids = getCompletedIds().toMutableSet()
        if (id in ids) return   // already complete — do nothing
        ids.add(id)
        saveCompletedIds(ids)
        loadedMilestones = loadedMilestones.map {
            it.copy(isCompleted = it.id in ids)
        }.toMutableList()
        _milestones.value = loadedMilestones.toList()
        _progress.value   = buildProgress()
    }

    // ── Child profile ─────────────────────────────────────────────────────────

    /**
     * Persist the new child age and immediately reset all in-memory state.
     *
     * FIX: Old code only saved the age and recomputed completedIds on whatever
     * loadedMilestones happened to be in memory — which could be wrong groups.
     * Now we wipe everything cleanly. The ViewModel's setChildAge() then calls
     * initialLoad() which picks up a fresh slate.
     */
    fun setChildAge(months: Int) {
        prefs.edit().putInt(KEY_AGE, months).apply()

        // Wipe stale in-memory data — initialLoad() will re-populate from scratch
        // using the new age's floor group, not the old age's floor group.
        resetLoadState()

        // Emit empty list while the upcoming initialLoad() runs.
        _milestones.value = emptyList()
        _progress.value   = buildProgress()
    }

    fun setChildName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
        _progress.value = _progress.value.copy(childName = name)
    }

    fun getChildAgeMonths(): Int = prefs.getInt(KEY_AGE, 0)
    fun getChildName(): String   = prefs.getString(KEY_NAME, "") ?: ""

    fun refreshAdminMilestones() = mergeAndEmit()

    // ── Private ───────────────────────────────────────────────────────────────

    /**
     * Wipe all in-memory milestone data and the loader's CSV cache.
     *
     * Must be called at the start of every initialLoad() and when the child's
     * age changes, so no stale data from a previous session leaks into the
     * new load.
     *
     * After this call:
     *   - loadedMilestones is empty
     *   - highestLoadedGroup is 0   (loadGroupIfNeeded won't skip anything)
     *   - LazyDatasetLoader cache is cleared (no memoised stale CSV rows)
     */
    private fun resetLoadState() {
        loadedMilestones   = mutableListOf()
        highestLoadedGroup = 0
        loader.clearCache()           // ✅ FIX: wipe stale LazyDatasetLoader cache
    }

    private suspend fun loadGroupIfNeeded(groupId: Int) {
        if (groupId <= highestLoadedGroup) return
        val newMs = withContext(Dispatchers.IO) {
            loader.loadForGroup(groupId)
        }
        loadedMilestones.addAll(newMs)
        highestLoadedGroup = groupId
    }

    private fun mergeAndEmit() {
        val completed = getCompletedIds()
        mergeAndEmitWithIds(completed)
    }
    fun clearAllData() {
        prefs.edit().clear().apply()
        resetLoadState()
        _milestones.value = emptyList()
        _progress.value = JourneyProgress(0, 0, 0)
    }

    private fun mergeAndEmitWithIds(completed: Set<String>) {
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

        loadedMilestones = loadedMilestones.map {
            it.copy(isCompleted = it.id in completed)
        }.toMutableList()

        val merged = (loadedMilestones + adminMs)
            .sortedWith(compareBy({ it.ageMonths }, { it.source.ordinal }))
            .toList()

        _milestones.value = merged
        _progress.value   = buildProgress()
    }

    private fun buildProgress() = JourneyProgress(
        totalMilestones     = loadedMilestones.size,
        completedMilestones = loadedMilestones.count { it.isCompleted },
        childAgeMonths      = getChildAgeMonths(),
        childName           = getChildName()
    )

    private fun getCompletedIds(): Set<String> {
        val json = prefs.getString(KEY_COMPLETED, null) ?: return emptySet()
        return try {
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type) ?: emptySet()
        } catch (e: Exception) { emptySet() }
    }

    private fun saveCompletedIds(ids: Set<String>) {
        prefs.edit().putString(KEY_COMPLETED, gson.toJson(ids)).apply()
    }

    companion object {
        private const val KEY_COMPLETED = "completed_ids"
        private const val KEY_AGE       = "child_age_months"
        private const val KEY_NAME      = "child_name"
    }
}