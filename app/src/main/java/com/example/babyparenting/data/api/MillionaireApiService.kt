package com.example.babyparenting.data.api

import com.example.babyparenting.data.model.*
import retrofit2.http.*

interface MillionaireApiService {

    companion object {
        const val BASE_URL = "http://192.168.1.20:8000/"
    }

    /**
     * Fetch all available strategies
     */
    @GET("millionaire/strategies")  // ✅ FIXED: Added millionaire prefix
    suspend fun getStrategies(): List<Strategy>

    /**
     * Fetch activities for a specific strategy
     */
    @GET("millionaire/activities/{strategy_id}")  // ✅ FIXED: Added millionaire prefix
    suspend fun getActivities(
        @Path("strategy_id") strategyId: Int
    ): List<Activity>

    /**
     * Mark an activity as completed
     */
    @POST("millionaire/complete")  // ✅ FIXED: Added millionaire prefix
    suspend fun markActivityComplete(
        @Query("user_id") userId: String,  // ✅ FIXED: Added @Query annotation
        @Query("activity_id") activityId: Int  // ✅ FIXED: Added @Query annotation
    ): ActivityCompletionResponse

    /**
     * Get today's recommended activity
     */
    @GET("millionaire/daily-activity")  // ✅ FIXED: Added millionaire prefix
    suspend fun getDailyActivity(
        @Query("user_id") userId: String,
        @Query("child_age") childAge: Int
    ): DailyActivityResponse

    /**
     * Get progress summary
     */
    @GET("millionaire/progress/summary")  // ✅ FIXED: Added millionaire prefix
    suspend fun getProgressSummary(
        @Query("user_id") userId: String
    ): ProgressSummary
}