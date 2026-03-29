package com.example.babyparenting.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

// ===== STRATEGY MODEL =====

data class Strategy(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("age_min")
    val age_min: Int = 0,

    @SerializedName("age_max")
    val age_max: Int = 0,

    @SerializedName("icon")
    val icon: String? = null,

    @SerializedName("completed_count")
    val completed_count: Int = 0,

    @SerializedName("total_activities")
    val total_activities: Int = 0,

    // ✅ ADD THIS to make ActivityDetailScreen work
    val activity: Activity? = null
)

// ===== ACTIVITY MODEL =====

@Serializable
data class Activity(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("strategy_id")
    val strategy_id: Int = 0,

    @SerializedName("title")
    val title: String? = null,
    @SerializedName("age_min")
    val age_min: Int?,
    @SerializedName("age_max")
    val age_max: Int?,


    @SerializedName("description")
    val description: String? = null,

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("age_range")
    val ageRange: String? = null,


    @SerializedName("duration_min")
    val duration: Int? = null,

    @SerializedName("level")
    val level: Int = 1,

    // Meta info
    @SerializedName("materials")
    val materials: List<String>? = null,

    @SerializedName("tags")
    val tags: List<String>? = null,

    @SerializedName("difficulty")
    val difficulty: String? = null,

    // Nested objects for structured access
    val meta: MetaInfo? = null,
    val basic: BasicInfo? = null,
    val parentGuidance: ParentGuidance? = null,
    val help: HelpSection? = null,

    @SerializedName("completed_count")
    val completed_count: Int = 0
)

// ===== BASIC INFO (PLAN, DO, REVIEW) =====

@Serializable
data class BasicInfo(
    @SerializedName("plan")
    val plan: String? = null,

    @SerializedName("do")
    val do_: String? = null,

    @SerializedName("review")
    val review: String? = null
)

// ===== META INFO (MATERIALS, TIME, etc.) =====

@Serializable
data class MetaInfo(
    @SerializedName("materials")
    val materials: List<String>? = null,

    @SerializedName("time_minutes")

    val timeMinutes: Int? = null,

    @SerializedName("difficulty")
    val difficulty: String? = null,

    @SerializedName("tags")
    val tags: List<String>? = null
)

// ===== PARENT GUIDANCE (SETUP, QUESTIONS, etc.) =====

@Serializable
data class ParentGuidance(
    @SerializedName("setup")
    val setup: String? = null,

    @SerializedName("plan_questions")
    val planQuestions: List<String>? = null,
    @SerializedName("examples")
    val examples: List<String>? = null,

    @SerializedName("do")
    val do_: String? = null,

    @SerializedName("review_prompts")
    val reviewPrompts: List<String>? = null,

    @SerializedName("repeat")
    val repeat: String? = null
)

// ===== HELP SECTION (INDICATORS, MISTAKES, DIALOGUE) =====

@Serializable
data class HelpSection(
    @SerializedName("indicators")
    val indicators: List<String>? = null,

    @SerializedName("mistakes")
    val mistakes: List<String>? = null,

    @SerializedName("examples")
    val examples: List<String>? = null,

    @SerializedName("dialogue")
    val dialogue: String? = null
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
    val status: String = "",

    @SerializedName("message")
    val message: String? = null
)

// ===== ACTIVITY DETAIL =====

data class ActivityDetail(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("plan")
    val plan: String = "",

    @SerializedName("do")
    val `do`: String = "",

    @SerializedName("review")
    val review: String = "",

    @SerializedName("strategy_id")
    val strategy_id: Int = 0
    ,
    @SerializedName("duration_min")
    val duration: Int? = null
)

// ===== DAILY ACTIVITY RESPONSE =====

data class DailyActivityResponse(
    @SerializedName("activity")
    val activity: ActivityDetail? = null,
    @SerializedName("description")
    val description: String? = null ,


    @SerializedName("title")
    val title: String? =null,

    @SerializedName("message")
    val message: String? = null
)

// ===== PROGRESS SUMMARY =====

data class ProgressSummary(
    @SerializedName("total_activities")
    val total_activities: Int = 0,

    @SerializedName("completed_activities")
    val completed_activities: Int = 0,

    @SerializedName("completion_percentage")
    val completion_percentage: Float = 0f,

    @SerializedName("current_level")
    val current_level: Int = 1
)

data class CompletedIdsResponse(
    @SerializedName("completed_ids") val completedIds: List<Int>
)

// ===== ACTIVITY WITH STATUS (LOCAL) =====

data class ActivityWithStatus(
    val activity: Activity,
    val isCompleted: Boolean

)