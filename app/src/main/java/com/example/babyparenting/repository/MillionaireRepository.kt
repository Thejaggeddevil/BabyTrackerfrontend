package com.example.babyparenting.data.repository

import android.content.Context
import android.util.Log
import com.example.babyparenting.data.api.MillionaireApiService
import com.example.babyparenting.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MillionaireRepository @Inject constructor(
    private val apiService: MillionaireApiService,
    private val context: Context
) {

    // ===== GET ALL STRATEGIES =====
    suspend fun getStrategies(): List<Strategy> = withContext(Dispatchers.IO) {
        try {
            Log.d("MillionaireRepo", "Fetching strategies from API...")
            val response = apiService.getStrategies()
            Log.d("MillionaireRepo", "Got ${response.size} strategies from API")
            response
        } catch (e: Exception) {
            Log.e("MillionaireRepo", "Error fetching strategies: ${e.message}")
            throw Exception("Failed to fetch strategies: ${e.message}")
        }
    }

    // ===== GET ACTIVITIES FOR STRATEGY =====
    suspend fun getActivitiesForStrategy(strategyId: Int): List<Activity> = withContext(Dispatchers.IO) {
        try {
            Log.d("MillionaireRepo", "Fetching activities for strategy $strategyId...")
            val response = apiService.getActivitiesByStrategy(strategyId)
            Log.d("MillionaireRepo", "Got ${response.size} activities for strategy $strategyId")
            response
        } catch (e: Exception) {
            Log.e("MillionaireRepo", "Error fetching activities: ${e.message}")
            throw Exception("Failed to fetch activities: ${e.message}")
        }
    }

    // ===== GET ACTIVITY DETAIL (WITH PARENT GUIDANCE) =====
    suspend fun getActivityDetail(activityId: Int): Activity = withContext(Dispatchers.IO) {
        try {
            Log.d("MillionaireRepo", "Fetching activity detail for activity $activityId...")
            val response = apiService.getActivityWithGuidance(activityId)

            // The response includes full parent guidance
            val activity = response["activity"] as? Map<String, Any>

            if (activity != null) {
                Log.d("MillionaireRepo", "Got activity detail with guidance")
                // Parse the activity from the response
                // This should return a fully populated Activity object
                return@withContext parseActivityFromResponse(activity)
            } else {
                throw Exception("Activity not found in response")
            }
        } catch (e: Exception) {
            Log.e("MillionaireRepo", "Error fetching activity detail: ${e.message}")
            throw Exception("Failed to fetch activity detail: ${e.message}")
        }
    }

    // ===== GET DAILY ACTIVITY =====
    suspend fun getDailyActivity(userId: String, childAge: Int): DailyActivityResponse = withContext(Dispatchers.IO) {
        try {
            Log.d("MillionaireRepo", "Fetching daily activity for user $userId...")
            val response = apiService.getDailyActivity(userId, childAge)
            Log.d("MillionaireRepo", "Got daily activity: ${response.activity?.title ?: "No activity"}")
            response
        } catch (e: Exception) {
            Log.e("MillionaireRepo", "Error fetching daily activity: ${e.message}")
            throw Exception("Failed to fetch daily activity: ${e.message}")
        }
    }

    // ===== GET PROGRESS =====
    suspend fun getProgress(userId: String): ProgressSummary = withContext(Dispatchers.IO) {
        try {
            Log.d("MillionaireRepo", "Fetching progress for user $userId...")
            val response = apiService.getProgressSummary(userId)
            Log.d("MillionaireRepo", "Got progress: ${response.completed_activities}/${response.total_activities}")
            response
        } catch (e: Exception) {
            Log.e("MillionaireRepo", "Error fetching progress: ${e.message}")
            throw Exception("Failed to fetch progress: ${e.message}")
        }
    }

    // ===== COMPLETE ACTIVITY =====
    suspend fun completeActivity(userId: String, activityId: Int): ActivityCompletionResponse = withContext(Dispatchers.IO) {
        try {
            Log.d("MillionaireRepo", "Marking activity $activityId as completed for user $userId...")
            val response = apiService.completeActivity(userId, activityId)
            Log.d("MillionaireRepo", "Activity completion response: ${response.status}")
            response
        } catch (e: Exception) {
            Log.e("MillionaireRepo", "Error completing activity: ${e.message}")
            throw Exception("Failed to complete activity: ${e.message}")
        }
    }

    // ===== HELPER: Parse Activity from Response =====
    private fun parseActivityFromResponse(activityMap: Map<String, Any>): Activity {
        return Activity(
        id = (activityMap["id"] as? Number)?.toInt(),
        title = activityMap["title"] as? String,
        description = activityMap["description"] as? String,
        strategy_id = (activityMap["strategy_id"] as? Number)?.toInt() ?: 0,

        age_min = (activityMap["age_min"] as? Number)?.toInt(),
        age_max = (activityMap["age_max"] as? Number)?.toInt(),

        duration = (activityMap["duration_min"] as? Number)?.toInt() ?: 30,
        level = (activityMap["level"] as? Number)?.toInt() ?: 1,
        basic = parseBasicInfo(activityMap["basic"]),
        parentGuidance = parseParentGuidance(activityMap["parent_guidance"]),
        help = parseHelpSection(activityMap["help"]),
        meta = parseMetaInfo(activityMap["meta"])
    )
    }

    private fun parseBasicInfo(data: Any?): BasicInfo? {
        return if (data is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val map = data as Map<String, Any>
            BasicInfo(
                plan = map["plan"] as? String,
                do_ = map["do"] as? String,
                review = map["review"] as? String
            )
        } else null
    }

    private fun parseParentGuidance(data: Any?): ParentGuidance? {
        return if (data is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val map = data as Map<String, Any>
            ParentGuidance(
                setup = map["setup"] as? String,
                planQuestions = parseStringList(map["plan_questions"]),
                do_ = map["do"] as? String,
                reviewPrompts = parseStringList(map["review_prompts"]),
                repeat = map["repeat"] as? String
            )
        } else null
    }

    private fun parseHelpSection(data: Any?): HelpSection? {
        return if (data is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val map = data as Map<String, Any>
            HelpSection(
                indicators = parseStringList(map["success_indicators"]),
                mistakes = parseStringList(map["common_mistakes"]),
                examples = parseStringList(map["tips"]),
                dialogue = map["example_dialogue"] as? String
            )
        } else null
    }

    private fun parseMetaInfo(data: Any?): MetaInfo? {
        return if (data is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val map = data as Map<String, Any>
            MetaInfo(
                materials = parseStringList(map["materials"]),
                timeMinutes = (map["time_minutes"] as? Number)?.toInt(),
                difficulty = map["difficulty"] as? String,
                tags = parseStringList(map["tags"])
            )
        } else null
    }

    private fun parseStringList(data: Any?): List<String>? {
        return when (data) {
            is List<*> -> data.mapNotNull { it as? String }
            else -> null
        }
    }
}