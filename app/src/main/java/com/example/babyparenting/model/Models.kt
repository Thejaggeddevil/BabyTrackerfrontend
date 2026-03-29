package com.example.babyparenting.data.model

/**
 * Maps each dataset to its exact .pkl model name on the backend.
 *
 * Backend pkl files in models/ directory:
 *   baby_0_24  parent_0_24  child_24_60  parent_25_60  academics_2_5
 *   academics_5_12  parent_5_12  good_bad_touch  language_5_12
 *   maths_5_12  science_5_12  social_5_12  civics_evs_5_12  cs_5_12  foreign_5_12
 */
enum class DatasetSource(
    val displayName: String,
    val emoji: String,
    val colorHex: Long,
    val apiModel: String           // exact pkl filename without .pkl
) {
    CHILD_0_24(       "Baby Activity",    "🧒", 0xFFFF8B94, "baby_0_24"),
    PARENT_0_24(      "Parent Guide",     "👨‍👩‍👧", 0xFFFFB347, "parent_0_24"),
    CHILD_24_60(      "Toddler Activity", "🎠", 0xFFFFC75F, "child_24_60"),
    PARENT_24_60(     "Parent Guide",     "👨‍👩‍👦", 0xFF98D8C8, "parent_25_60"),
    PRE_ACADEMICS(    "Pre-Academics",    "📝", 0xFFB5EAD7, "academics_2_5"),
    CHILD_5_12(       "Child Activity",   "🏃", 0xFFADD8E6, "academics_5_12"),
    PARENT_5_12(      "Parent Guide",     "👩‍👧", 0xFFD4A5F5, "parent_5_12"),
    SAFETY(           "Safety",           "🛡️", 0xFF7C83FD, "good_bad_touch"),
    LANGUAGE(         "Language",         "🗣️", 0xFFFDDB92, "language_5_12"),
    MATHEMATICS(      "Mathematics",      "🔢", 0xFF66BB6A, "maths_5_12"),
    SCIENCE(          "Science",          "🔬", 0xFF42A5F5, "science_5_12"),
    SOCIAL_STUDIES(   "Social Studies",   "🏘️", 0xFFAB47BC, "social_5_12"),
    CIVICS(           "Civics & EVS",     "🏛️", 0xFFFF6B6B, "civics_evs_5_12"),
    COMPUTER_SCIENCE( "Computer Science", "💻", 0xFF26C6DA, "cs_5_12"),
    FOREIGN_LANGUAGE( "Foreign Language", "🌐", 0xFFFFCC02, "foreign_5_12"),
    ADMIN_CUSTOM(     "Custom",           "⭐", 0xFF9E9E9E, "parent_0_24");  // admin-added milestones

    companion object {
        /** All sources that come from CSV datasets (cannot be deleted by admin) */
        val CSV_SOURCES: Set<DatasetSource> = values().toSet() - ADMIN_CUSTOM
    }
}

// ── Age Group ─────────────────────────────────────────────────────────────────
data class AgeGroup(
    val id: Int,
    val label: String,
    val description: String,
    val startMonth: Int,
    val endMonth: Int,
    val accentColor: Long
)

// ── Milestone ─────────────────────────────────────────────────────────────────
data class Milestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val domain: String,
    val ageMonths: Int,
    val ageRange: String,
    val ageGroupId: Int,
    val source: DatasetSource,
    val apiQuery: String,
    val iconEmoji: String,
    val accentColor: Long,
    val isCompleted: Boolean = false,
    val isAdminAdded: Boolean = false   // true = can be deleted; false = CSV milestone, protected
)

// ── Admin milestone (stored in SharedPreferences) ─────────────────────────────
data class AdminMilestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val domain: String,
    val ageMonths: Int,
    val ageRange: String,
    val ageGroupId: Int,
    val apiQuery: String,
    val iconEmoji: String,
    val sourceApiModel: String = "parent_0_24"
)

// ── API Response from POST /predict ──────────────────────────────────────────
data class AdviceApiResponse(
    val domain: String = "",
    val title: String = "",
    val goal: String = "",
    val why: String = "",
    val how: String = "",
    val dos: List<String> = emptyList(),
    val donts: List<String> = emptyList(),
    val tip: String = "",
    val steps: List<String> = emptyList(),
    val difficulty: String = "",
    val example: String = "",
    val answer: String = "",
    val scenario: String = ""
)

// ── UI State ──────────────────────────────────────────────────────────────────
sealed class UiState<out T> {
    object Idle    : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T)                                   : UiState<T>()
    data class Error(val message: String, val retryable: Boolean = true) : UiState<Nothing>()
}



// ── Journey Progress ──────────────────────────────────────────────────────────
data class JourneyProgress(
    val totalMilestones: Int,
    val completedMilestones: Int,
    val childAgeMonths: Int,
    val childName: String = ""
) {
    val progressFraction: Float
        get() = if (totalMilestones == 0) 0f
        else completedMilestones.toFloat() / totalMilestones.toFloat()
    val progressPercent: Int get() = (progressFraction * 100).toInt()
}