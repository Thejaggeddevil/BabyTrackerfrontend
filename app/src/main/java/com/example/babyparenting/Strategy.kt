package com.example.babyparenting.data.model

import com.google.gson.annotations.SerializedName

// ===== STRATEGY MODEL =====

data class Strategy(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("age_min")
    val age_min: Int,

    @SerializedName("age_max")
    val age_max: Int,

    @SerializedName("icon")
    val icon: String? = null,

    @SerializedName("completed_count")
    val completed_count: Int = 0,

    @SerializedName("total_activities")
    val total_activities: Int = 0
)

// ===== ACTIVITY MODEL =====

data class Activity(
    @SerializedName("id")
    val id: Int,

    @SerializedName("strategy_id")
    val strategy_id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("plan")
    val plan: String,

    @SerializedName("do_instruction")
    val do_instruction: String,

    @SerializedName("review")
    val review: String,

    @SerializedName("level")
    val level: Int,

    @SerializedName("duration_minutes")
    val duration_minutes: Int = 10,

    @SerializedName("materials")
    val materials: String? = null
)

// ===== ACTIVITY COMPLETION REQUEST =====

data class ActivityCompletion(
    @SerializedName("user_id")
    val user_id: String,

    @SerializedName("activity_id")
    val activity_id: Int,

    @SerializedName("completed_at")
    val completed_at: Long? = null
)

// ===== ACTIVITY COMPLETION RESPONSE =====

data class ActivityCompletionResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String? = null
)

// ===== DAILY ACTIVITY RESPONSE =====

data class DailyActivityResponse(
    @SerializedName("activity_id")
    val activity_id: Int,

    @SerializedName("strategy_id")
    val strategy_id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("plan")
    val plan: String,

    @SerializedName("do_instruction")
    val do_instruction: String,

    @SerializedName("review")
    val review: String,

    @SerializedName("level")
    val level: Int
)

// ===== ACTIVITY WITH STATUS (LOCAL) =====

data class ActivityWithStatus(
    val activity: Activity,
    val isCompleted: Boolean
)