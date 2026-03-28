package com.example.babyparenting.data.api

import com.example.babyparenting.data.model.*
import com.example.babyparenting.data.model.ProgressSummary
import retrofit2.http.*

interface MillionaireApiService {

    companion object {
        const val BASE_URL = "http://192.168.1.20:8000/"
    }

    /**
     * Fetch all available strategies
     */
    @GET("millionaire/strategies")
    suspend fun getStrategies(): List<Strategy>

    // ✅ Fixed: was "millionaire/strategies/{strategy_id}/activities"
    @GET("millionaire/activities/{strategy_id}")
    suspend fun getActivities(
        @Path("strategy_id") strategyId: Int
    ): List<Activity>

    @POST("millionaire/complete")
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

//data class ProgressSummary(
//    val total_activities: Int,
//    val completed_activities: Int,
//    val completion_percentage: Float,
//    val current_level: Int
//)