package com.example.babyparenting.data.api

import com.example.babyparenting.data.model.*
import retrofit2.http.*

interface MillionaireApiService {

    companion object {
        // ⚠️ CHANGE THIS TO YOUR ACTUAL BACKEND URL
        const val BASE_URL = "http://192.168.1.21:8000"  // Your FastAPI backend
        // For emulator: http://10.0.2.2:8000
        // For real device: http://your-server-ip:8000
    }

    // ===== GET STRATEGIES =====
    @GET("/millionaire/strategies")
    suspend fun getStrategies(): List<Strategy>

    // ===== GET ACTIVITIES BY STRATEGY =====
    @GET("/millionaire/activities/{strategy_id}")
    suspend fun getActivitiesByStrategy(@Path("strategy_id") strategyId: Int): List<Activity>

    // ===== GET ACTIVITY WITH PARENT GUIDANCE =====
    @GET("/millionaire/activity/{activity_id}/with-guidance")
    suspend fun getActivityWithGuidance(@Path("activity_id") activityId: Int): Map<String, Any>

    // ===== GET DAILY ACTIVITY =====
    @GET("/millionaire/daily-activity")
    suspend fun getDailyActivity(
        @Query("user_id") userId: String,
        @Query("child_age") childAge: Int
    ): DailyActivityResponse

    // ===== GET PROGRESS SUMMARY =====
    @GET("/millionaire/progress/summary")
    suspend fun getProgressSummary(
        @Query("user_id") userId: String
    ): ProgressSummary

    // ===== COMPLETE ACTIVITY =====
    @POST("/millionaire/complete")
    suspend fun completeActivity(
        @Query("user_id") userId: String,
        @Query("activity_id") activityId: Int
    ): ActivityCompletionResponse
}