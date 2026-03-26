package com.example.babyparenting.data.api

import com.example.babyparenting.data.model.*
import retrofit2.http.*

interface MillionaireApiService {

    companion object {
        const val BASE_URL = "https://api.babyparenting.app/"
    }

    /**
     * Fetch all available strategies
     */
    @GET("strategies")
    suspend fun getStrategies(): List<Strategy>

    /**
     * Fetch activities for a specific strategy
     */
    @GET("strategies/{strategy_id}/activities")
    suspend fun getActivities(
        @Path("strategy_id") strategyId: Int
    ): List<Activity>

    /**
     * Mark an activity as completed
     */
    @POST("activity/complete")
    suspend fun markActivityComplete(
        @Body completion: ActivityCompletion
    ): ActivityCompletionResponse

    /**
     * Get today's recommended activity
     */
    @GET("daily-activity")
    suspend fun getDailyActivity(
        @Query("user_id") userId: String,
        @Query("child_age") childAge: Int
    ): DailyActivityResponse

    /**
     * Get progress summary
     */
    @GET("progress/summary")
    suspend fun getProgressSummary(
        @Query("user_id") userId: String
    ): ProgressSummary
}

data class ProgressSummary(
    val total_activities: Int,
    val completed_activities: Int,
    val completion_percentage: Float,
    val current_level: Int
)