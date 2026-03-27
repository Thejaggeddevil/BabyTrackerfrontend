package com.example.babyparenting.data.api

import com.example.babyparenting.data.model.*
import retrofit2.http.*

interface MillionaireApiService {

    companion object {
        const val BASE_URL = "http://192.168.1.7:8000/"
    }

    /**
     * Fetch all available strategies
     */
    @GET("millionaire/strategies")
    suspend fun getStrategies(): List<Strategy>

    @GET("millionaire/strategies/{strategy_id}/activities")
    suspend fun getActivities(
        @Path("strategy_id") strategyId: Int
    ): List<Activity>

    @POST("millionaire/activity/complete")
    suspend fun markActivityComplete(
        @Body completion: ActivityCompletion
    ): ActivityCompletionResponse

    @GET("millionaire/daily-activity")
    suspend fun getDailyActivity(
        @Query("user_id") userId: String,
        @Query("child_age") childAge: Int
    ): DailyActivityResponse

    @GET("millionaire/progress/summary")
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