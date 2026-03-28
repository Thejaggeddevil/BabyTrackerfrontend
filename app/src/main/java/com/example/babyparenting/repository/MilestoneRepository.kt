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

    suspend fun initialLoad() {
        try {
            _isLoading.value = true
            _error.value     = null

            resetLoadState()

            _ageGroups.value = loader.getAgeGroups()

            val childAge     = getChildAgeMonths()
            val groupsToLoad = loader.groupsToPreload(childAge)
            for (groupId in groupsToLoad) {
                loadGroupIfNeeded(groupId)
            }

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

    /**
     * FREEZE FIX: isLoading guard lagaya.
     *
     * Pehle: checkAndLoadNextGroup() bar-bar call hota tha (har milestone
     * complete hone pe). Agar pichla load abhi chal raha tha toh duplicate
     * load shuru ho jaata tha — ek saath kai groups load hote, memory spike
     * aata tha aur UI freeze ho jaata tha.
     *
     * Ab: agar load chal raha hai toh turant return — koi duplicate load nahi.
     */
    suspend fun loadNextGroupIfNeeded(nextGroupId: Int) {
        if (nextGroupId > loader.totalGroups()) return
        if (nextGroupId <= highestLoadedGroup) return
        if (_isLoading.value) return   // ← FREEZE FIX: duplicate load block

        try {
            _isLoading.value = true
            loadGroupIfNeeded(nextGroupId)
            mergeAndEmit()
        } catch (e: Exception) {
            // silent fail — UI already has previous milestones
        } finally {
            _isLoading.value = false
        }
    }

    // ── ONE-WAY completion ────────────────────────────────────────────────────

    fun markComplete(id: String) {
        val ids = getCompletedIds().toMutableSet()
        if (id in ids) return
        ids.add(id)
        saveCompletedIds(ids)
        loadedMilestones = loadedMilestones.map {
            it.copy(isCompleted = it.id in ids)
        }.toMutableList()
        _milestones.value = loadedMilestones.toList()
        _progress.value   = buildProgress()
    }

    // ── Child profile ─────────────────────────────────────────────────────────

    fun setChildAge(months: Int) {
        prefs.edit().putInt(KEY_AGE, months).apply()
        resetLoadState()
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

    private fun resetLoadState() {
        loadedMilestones   = mutableListOf()
        highestLoadedGroup = 0
        loader.clearCache()
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