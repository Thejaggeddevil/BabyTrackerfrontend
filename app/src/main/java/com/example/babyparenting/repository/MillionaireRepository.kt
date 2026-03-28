package com.example.babyparenting.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.babyparenting.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.example.babyparenting.data.api.MillionaireApiService
import com.example.babyparenting.data.model.ProgressSummary
private val Context.millionaireDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "millionaire_club_prefs"
)

@Singleton
class MillionaireRepository @Inject constructor(
    private val apiService: MillionaireApiService,
    private val context: Context
) {

//    private var strategies: Result<List<Strategy>> = TODO("initialize me")
    private val dataStore = context.millionaireDataStore

    // DataStore Keys
    companion object {
        private val COMPLETED_ACTIVITIES = stringSetPreferencesKey("completed_activities")
        private val CHILD_AGE = intPreferencesKey("child_age")
        private val USER_ID = stringPreferencesKey("user_id")
        private val LAST_SYNC = longPreferencesKey("last_sync_time")
    }

    /**
     * Fetch strategies from backend
     */
    suspend fun getStrategies(): Result<List<Strategy>> = try {
        val strategies = apiService.getStrategies()
        Result.success(strategies)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Fetch activities for a specific strategy
     */
    suspend fun getActivities(strategyId: Int): Result<List<Activity>> = try {
        val activities = apiService.getActivities(strategyId)
        Result.success(activities)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Mark activity as completed and sync with backend
     */
    suspend fun completeActivity(
        userId: String,
        activityId: Int
    ): Result<Boolean> = try {
        val completion = ActivityCompletion(
            user_id = userId,
            activity_id = activityId,
            completed_at = System.currentTimeMillis()
        )

        // Sync with backend
        apiService.markActivityComplete(
            userId = userId,
            activityId = activityId
        )

        // Save locally
        saveCompletedActivity(activityId)

        Result.success(true)
    } catch (e: Exception) {
        // Still save locally even if sync fails (for offline support)
        saveCompletedActivity(activityId)
        Result.success(true)
    }

    /**
     * Save completed activity to local DataStore
     */
    private suspend fun saveCompletedActivity(activityId: Int) {
        dataStore.edit { preferences ->
            val current = preferences[COMPLETED_ACTIVITIES] ?: emptySet()
            preferences[COMPLETED_ACTIVITIES] = current + activityId.toString()
            preferences[LAST_SYNC] = System.currentTimeMillis()
        }
    }

    /**
     * Get completed activities as Flow
     */
    fun getCompletedActivities(): Flow<Set<Int>> =
        dataStore.data.map { preferences ->
            preferences[COMPLETED_ACTIVITIES]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        }

    /**
     * Check if specific activity is completed
     */
    fun isActivityCompleted(activityId: Int): Flow<Boolean> =
        getCompletedActivities().map { completed ->
            completed.contains(activityId)
        }

    /**
     * Get today's recommended activity
     */
    suspend fun getDailyActivity(userId: String, childAge: Int): Result<DailyActivityResponse> =
        try {
            val activity = apiService.getDailyActivity(userId, childAge)
            Result.success(activity)
        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * Get progress summary
     */
    suspend fun getProgressSummary(userId: String): Result<ProgressSummary> = try {
        val summary = apiService.getProgressSummary(userId)
        Result.success(summary)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Save child age preference
     */
    suspend fun setChildAge(age: Int) {
        dataStore.edit { preferences ->
            preferences[CHILD_AGE] = age
        }
    }

    /**
     * Get child age preference
     */
    fun getChildAge(): Flow<Int> =
        dataStore.data.map { preferences ->
            preferences[CHILD_AGE] ?: 3
        }

    /**
     * Save user ID
     */
    suspend fun setUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
    }

    /**
     * Get user ID
     */
    fun getUserId(): Flow<String> =
        dataStore.data.map { preferences ->
            preferences[USER_ID] ?: "default_user"
        }

    /**
     * Clear all local data (for testing/logout)
     */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
