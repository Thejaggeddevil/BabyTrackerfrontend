package com.example.babyparenting.data.local

import android.content.Context
import com.example.babyparenting.data.model.AgeGroup
import com.example.babyparenting.data.model.DatasetSource
import com.example.babyparenting.data.model.Milestone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * LAZY dataset loader — only loads the ONE age group that the user is currently on.
 *
 * ── THE BUG FIX (floor/ceiling method) ───────────────────────────────────────
 * Old behaviour: If child was 7 years old, the app was loading ALL milestones
 * from 0 months to 84 months → thousands of CSV rows → app hang.
 *
 * New behaviour (floor ONLY on init, ceiling on demand):
 *   - We map the child's age to EXACTLY ONE age group bucket.
 *   - FLOOR  = the group that contains the child's current age (startingGroupId).
 *   - On app start: ONLY floor group loads, NO ceiling.
 *   - CEILING = floor + 1 (loaded only on-demand when user scrolls or needs it).
 *   - We NEVER load groups BELOW the child's current age group on first load.
 *
 * Example:
 *   Child = 8 years 9 months = 105 months → startingGroupId = 11 (7–9 yrs)
 *   On app open, ONLY group 11 data loads. Groups 1–10 are ignored entirely.
 *   Groups 12+ load only when explicitly requested via loadForGroup().
 *
 * ── FIXES IN THIS VERSION ────────────────────────────────────────────────────
 * FIX 1: ageRawToMonths() — old code used String.contains() which caused
 *         "108".contains("8") == true → wrong bucket. Now uses exact word match.
 * FIX 2: loadGroup_9_12() — Groups 12 and 13 previously both used minMo=108
 *         maxMo=144, loading identical data twice. Now Group 12 = 108–131 mo,
 *         Group 13 = 132–144 mo, each gets its own distinct slice.
 * FIX 3: clearCache() — exposed so MilestoneRepository can wipe stale group
 *         data when the child's age is changed by the parent.
 * FIX 4: groupsToPreload() returns ONLY floor group (not ceiling).
 *         Ceiling loads on-demand to reduce initial app startup time
 *         and prevent loading unnecessary CSV data.
 *
 * Age Group → CSV mapping:
 *   Group 1–6  (0–24 mo)   : 0_24_month_data.csv + 0_24_data_parent.csv
 *   Group 7–9  (24–60 mo)  : 24_60_month_data.csv + 25_60_data_parent.csv
 *                            + 2_5_academics_data.csv
 *   Group 10   (5–7 yr)    : 5_12_year_data.csv + 5_12_year_data_parent.csv
 *                            + language + maths + safety
 *   Group 11   (7–9 yr)    : science + social + civics
 *   Group 12   (9–11 yr)   : cs + foreign + remaining   [108–131 mo]
 *   Group 13   (11–12 yr)  : cs + foreign + lang        [132–144 mo]
 */
class LazyDatasetLoader(private val context: Context) {

    // In-memory cache — ek baar load hua group dobara CSV nahi padhega.
    // clearCache() wipes this so a new child age starts completely fresh.
    private val loadedGroups = mutableMapOf<Int, List<Milestone>>()

    /**
     * Wipe the in-memory group cache.
     * Must be called whenever the child's age changes so stale groups
     * from the old age are not served to the new session.
     */
    fun clearCache() = loadedGroups.clear()

    fun getAgeGroups(): List<AgeGroup> = listOf(
        AgeGroup(1,  "0 – 3 Months",   "Sensory foundation, feeding & safe sleep",        0,   3,   0xFFFF8B94),
        AgeGroup(2,  "3 – 6 Months",   "Tummy time, head control & social smiles",         3,   6,   0xFFFFB347),
        AgeGroup(3,  "6 – 9 Months",   "Sitting, solid foods & babbling",                 6,   9,   0xFFFFC75F),
        AgeGroup(4,  "9 – 12 Months",  "Crawling, standing & first words",                9,   12,  0xFF98D8C8),
        AgeGroup(5,  "1 – 1.5 Years",  "Walking, vocabulary burst & play skills",         12,  18,  0xFFB5EAD7),
        AgeGroup(6,  "1.5 – 2 Years",  "Running, emotional growth & self-feeding",        18,  24,  0xFF7C83FD),
        AgeGroup(7,  "2 – 3 Years",    "Language, potty training & pre-academics",        24,  36,  0xFFADD8E6),
        AgeGroup(8,  "3 – 4 Years",    "Pre-K: drawing, safety & creativity",             36,  48,  0xFFD4A5F5),
        AgeGroup(9,  "4 – 5 Years",    "Kindergarten: reading, writing & body safety",    48,  60,  0xFFFDDB92),
        AgeGroup(10, "5 – 7 Years",    "School: core subjects, language & safety",        60,  84,  0xFF66BB6A),
        AgeGroup(11, "7 – 9 Years",    "Advanced academics, STEM & social skills",        84,  108, 0xFF42A5F5),
        AgeGroup(12, "9 – 11 Years",   "Critical thinking, CS & foreign language",        108, 132, 0xFFFF6B6B),
        AgeGroup(13, "11 – 12 Years",  "Pre-teen independence, coding & digital ethics",  132, 144, 0xFF26C6DA)
    )

    /**
     * Load milestones for ONE specific age group (lazy, cached).
     *
     * Called on-demand: only when the user's screen shows a particular group.
     * The MilestoneRepository calls this with:
     *   - startingGroupId(childAgeMonths) on first load
     *   - loadNextGroupIfNeeded(groupId) when user reaches end of group
     */
    suspend fun loadForGroup(groupId: Int): List<Milestone> =
        withContext(Dispatchers.IO) {
            loadedGroups.getOrPut(groupId) {
                when (groupId) {
                    in 1..6  -> loadGroup_0_24(groupId)
                    in 7..9  -> loadGroup_24_60(groupId)
                    10       -> loadGroup_5_7(groupId)
                    11       -> loadGroup_7_9(groupId)
                    else     -> loadGroup_9_12(groupId)
                }
            }
        }

    /**
     * ── FLOOR/CEILING METHOD ──────────────────────────────────────────────────
     *
     * Maps child's age in months to the FLOOR group (the group they currently
     * belong to). This is the ONLY group loaded on app start.
     *
     * FLOOR: the group whose [startMonth, endMonth] contains childAgeMonths.
     *
     * Examples:
     *   0   months → Group 1  (newborn)
     *   7   months → Group 3  ← loads ONLY group 3, NOT groups 1–2
     *   84  months → Group 11 ← loads ONLY group 11, NOT groups 1–10
     *   105 months → Group 11 (8 yr 9 mo) ← same, NOT groups 1–10
     *   108 months → Group 12
     *   132 months → Group 13
     */
    fun startingGroupId(childAgeMonths: Int): Int = when (childAgeMonths) {
        in 0..2    -> 1
        in 3..5    -> 2
        in 6..8    -> 3
        in 9..11   -> 4
        in 12..17  -> 5
        in 18..23  -> 6
        in 24..35  -> 7
        in 36..47  -> 8
        in 48..59  -> 9
        in 60..83  -> 10
        in 84..107 -> 11
        in 108..131-> 12
        else       -> 13
    }

    /**
     * CEILING group = startingGroupId + 1 (next group after current).
     * Optional: load this after the floor group loads for smooth scrolling.
     */
    fun ceilingGroupId(childAgeMonths: Int): Int =
        (startingGroupId(childAgeMonths) + 1).coerceAtMost(totalGroups())

    fun totalGroups(): Int = 13

    /**
     * ✅ FIXED: Groups that should be pre-loaded on app start.
     *
     * Old (WRONG): (1..startingGroupId).toList()  ← caused hang loading all groups
     * Also WRONG: listOf(floor, floor+1)           ← loaded ceiling unnecessarily
     * New (CORRECT): listOf(floor)                 ← ONLY floor group on first load
     *
     * Ceiling loads on-demand when:
     *   - User scrolls to next age group
     *   - JourneyViewModel calls loadNextGroupIfNeeded(groupId + 1)
     *
     * This significantly reduces app startup time and memory usage.
     */
    fun groupsToPreload(childAgeMonths: Int): List<Int> {
        val floor = startingGroupId(childAgeMonths)
        // ✅ FIXED: Return ONLY floor, not ceiling
        return listOf(floor)
    }

    fun totalGroups_count(): Int = 13

    // ── Group loaders ─────────────────────────────────────────────────────────

    /** Groups 1–6: 0–24 months */
    private fun loadGroup_0_24(groupId: Int): List<Milestone> {
        val (start, end) = groupMonthRange(groupId)

        val child = openCsv("0_24_month_data.csv")
            .filter { row ->
                val age = parseAgeMonth(row.col("age_month"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val age    = parseAgeMonth(row.col("age_month"))
                val domain = row.col("domain")
                val skill  = row.col("skill")
                Milestone(
                    id          = "c0_24_g${groupId}_$idx",
                    title       = skill.take(50),
                    subtitle    = row.col("development_goal").take(70),
                    domain      = domain,
                    ageMonths   = age,
                    ageRange    = "${age}mo",
                    ageGroupId  = groupId,
                    source      = DatasetSource.CHILD_0_24,
                    apiQuery    = "Baby $domain $skill $age months",
                    iconEmoji   = emoji0to24(domain),
                    accentColor = DatasetSource.CHILD_0_24.colorHex
                )
            }

        val parent = openCsv("0_24_data_parent.csv")
            .filter { row ->
                val age = parseAgeMonth(row.col("age_month"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val age    = parseAgeMonth(row.col("age_month"))
                val domain = row.col("domain")
                val title  = row.col("guidance")
                Milestone(
                    id          = "p0_24_g${groupId}_$idx",
                    title       = title.take(50),
                    subtitle    = row.col("implementation").take(70),
                    domain      = domain,
                    ageMonths   = age,
                    ageRange    = "${age}mo",
                    ageGroupId  = groupId,
                    source      = DatasetSource.PARENT_0_24,
                    apiQuery    = "Parent guide $domain $age months",
                    iconEmoji   = emojiParent(domain),
                    accentColor = DatasetSource.PARENT_0_24.colorHex
                )
            }

        return child + parent
    }

    /** Groups 7–9: 24–60 months (2–5 years) */
    private fun loadGroup_24_60(groupId: Int): List<Milestone> {
        val (start, end) = groupMonthRange(groupId)

        val child = openCsv("24_60_month_data.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age")
                val age    = parseAgeYearToMonths(ageRaw)
                val domain = row.col("domain")
                val skill  = row.col("skill")
                Milestone(
                    id          = "c24_60_g${groupId}_$idx",
                    title       = skill.take(50),
                    subtitle    = row.col("development_goal").take(70),
                    domain      = domain,
                    ageMonths   = age,
                    ageRange    = ageRaw,
                    ageGroupId  = groupId,
                    source      = DatasetSource.CHILD_24_60,
                    apiQuery    = "Toddler $domain $skill $ageRaw",
                    iconEmoji   = emoji24to60(domain),
                    accentColor = DatasetSource.CHILD_24_60.colorHex
                )
            }

        val parent = openCsv("25_60_data_parent.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age")
                val age    = parseAgeYearToMonths(ageRaw)
                val domain = row.col("domain")
                val title  = row.col("guidance")
                Milestone(
                    id          = "p24_60_g${groupId}_$idx",
                    title       = title.take(50),
                    subtitle    = row.col("implementation").take(70),
                    domain      = domain,
                    ageMonths   = age,
                    ageRange    = ageRaw,
                    ageGroupId  = groupId,
                    source      = DatasetSource.PARENT_24_60,
                    apiQuery    = "Parent guide $domain $ageRaw",
                    iconEmoji   = emojiParent(domain),
                    accentColor = DatasetSource.PARENT_24_60.colorHex
                )
            }

        val academics = openCsv("2_5_academics_data.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age")
                val age    = parseAgeYearToMonths(ageRaw)
                val topic  = row.col("topic")
                Milestone(
                    id          = "ac2_5_g${groupId}_$idx",
                    title       = topic.take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = "Academics",
                    ageMonths   = age,
                    ageRange    = ageRaw,
                    ageGroupId  = groupId,
                    source      = DatasetSource.PRE_ACADEMICS,
                    apiQuery    = "Pre-academics $topic $ageRaw",
                    iconEmoji   = "📝",
                    accentColor = DatasetSource.PRE_ACADEMICS.colorHex
                )
            }

        return child + parent + academics
    }

    /** Group 10: 5–7 years */
    private fun loadGroup_5_7(groupId: Int): List<Milestone> {
        val (start, end) = groupMonthRange(groupId)

        val academics = openCsv("5_12_year_data.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age")
                val age    = parseAgeYearToMonths(ageRaw)
                val subject = row.col("subject")
                val skill   = row.col("skill")
                Milestone(
                    id          = "ac5_7_g${groupId}_$idx",
                    title       = skill.take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = subject,
                    ageMonths   = age,
                    ageRange    = ageRaw,
                    ageGroupId  = groupId,
                    source      = DatasetSource.CHILD_5_12,
                    apiQuery    = "$subject $skill $ageRaw years",
                    iconEmoji   = emojiAcademics(subject),
                    accentColor = DatasetSource.CHILD_5_12.colorHex
                )
            }

        val parent = openCsv("5_12_year_data_parent.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age")
                val age    = parseAgeYearToMonths(ageRaw)
                val domain = row.col("domain")
                val title  = row.col("guidance")
                Milestone(
                    id          = "p5_7_g${groupId}_$idx",
                    title       = title.take(50),
                    subtitle    = row.col("implementation").take(70),
                    domain      = domain,
                    ageMonths   = age,
                    ageRange    = ageRaw,
                    ageGroupId  = groupId,
                    source      = DatasetSource.PARENT_5_12,
                    apiQuery    = "Parent guide $domain $ageRaw years",
                    iconEmoji   = emojiParent(domain),
                    accentColor = DatasetSource.PARENT_5_12.colorHex
                )
            }

        val language = loadLanguageDataset(groupId, "5-7")
        val maths    = loadMathsDataset(groupId, "5-7")
        val safety   = loadSafetyDataset(groupId, "5-7")

        return academics + parent + language + maths + safety
    }

    /** Group 11: 7–9 years */
    private fun loadGroup_7_9(groupId: Int): List<Milestone> {
        val (start, end) = groupMonthRange(groupId)

        val science = openCsv("science_5_12_data.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age"); val topic = row.col("topic")
                Milestone(
                    id          = "sci7_9_g${groupId}_$idx",
                    title       = topic.take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = "Science",
                    ageMonths   = parseAgeYearToMonths(ageRaw),
                    ageRange    = "$ageRaw yr",
                    ageGroupId  = groupId,
                    source      = DatasetSource.SCIENCE,
                    apiQuery    = "Science $topic $ageRaw years",
                    iconEmoji   = "🔬",
                    accentColor = DatasetSource.SCIENCE.colorHex
                )
            }

        val social = openCsv("social_5_12_data.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age"); val topic = row.col("topic")
                Milestone(
                    id          = "soc7_9_g${groupId}_$idx",
                    title       = topic.take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = "Social Studies",
                    ageMonths   = parseAgeYearToMonths(ageRaw),
                    ageRange    = "$ageRaw yr",
                    ageGroupId  = groupId,
                    source      = DatasetSource.SOCIAL_STUDIES,
                    apiQuery    = "Social studies $topic $ageRaw years",
                    iconEmoji   = "🏘️",
                    accentColor = DatasetSource.SOCIAL_STUDIES.colorHex
                )
            }

        val civics = openCsv("civics_evs_5_12_data.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age"); val topic = row.col("topic")
                Milestone(
                    id          = "civ7_9_g${groupId}_$idx",
                    title       = topic.take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = "Civics",
                    ageMonths   = parseAgeYearToMonths(ageRaw),
                    ageRange    = "$ageRaw yr",
                    ageGroupId  = groupId,
                    source      = DatasetSource.CIVICS,
                    apiQuery    = "Civics $topic $ageRaw years",
                    iconEmoji   = "🏛️",
                    accentColor = DatasetSource.CIVICS.colorHex
                )
            }

        return science + social + civics
    }

    /** Groups 12–13: 9–12 years */
    private fun loadGroup_9_12(groupId: Int): List<Milestone> {
        val (start, end) = groupMonthRange(groupId)

        val cs = openCsv("cs_5_12_data.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age"); val topic = row.col("topic")
                Milestone(
                    id          = "cs9_12_g${groupId}_$idx",
                    title       = topic.take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = "Computer Science",
                    ageMonths   = parseAgeYearToMonths(ageRaw),
                    ageRange    = "$ageRaw yr",
                    ageGroupId  = groupId,
                    source      = DatasetSource.COMPUTER_SCIENCE,
                    apiQuery    = "Computer science $topic $ageRaw years",
                    iconEmoji   = "💻",
                    accentColor = DatasetSource.COMPUTER_SCIENCE.colorHex
                )
            }

        val foreign = openCsv("foreign_5_12_data.csv")
            .filter { row ->
                val age = parseAgeYearToMonths(row.col("age"))
                age in start..end
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age"); val lang = row.col("language"); val topic = row.col("topic")
                Milestone(
                    id          = "for9_12_g${groupId}_$idx",
                    title       = "$lang: $topic".take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = "Foreign Language",
                    ageMonths   = parseAgeYearToMonths(ageRaw),
                    ageRange    = "$ageRaw yr",
                    ageGroupId  = groupId,
                    source      = DatasetSource.FOREIGN_LANGUAGE,
                    apiQuery    = "Foreign language $lang $topic $ageRaw years",
                    iconEmoji   = emojiLang(lang),
                    accentColor = DatasetSource.FOREIGN_LANGUAGE.colorHex
                )
            }

        return cs + foreign
    }

    private fun loadLanguageDataset(groupId: Int, agePrefix: String): List<Milestone> =
        openCsv("language_5_12_data.csv")
            .filter { row -> row.col("age_group").contains(agePrefix) }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age_group"); val topic = row.col("topic")
                Milestone(
                    id          = "lang_g${groupId}_$idx",
                    title       = topic.take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = "Language",
                    ageMonths   = ageRawToMonths(ageRaw),
                    ageRange    = "$ageRaw yr",
                    ageGroupId  = groupId,
                    source      = DatasetSource.LANGUAGE,
                    apiQuery    = "Language $topic $ageRaw years",
                    iconEmoji   = "🗣️",
                    accentColor = DatasetSource.LANGUAGE.colorHex
                )
            }

    private fun loadMathsDataset(groupId: Int, agePrefix: String): List<Milestone> =
        openCsv("maths_5_12_data.csv")
            .filter { row -> row.col("age_group").contains(agePrefix) }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age_group"); val topic = row.col("topic")
                Milestone(
                    id          = "math_g${groupId}_$idx",
                    title       = topic.take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = "Mathematics",
                    ageMonths   = ageRawToMonths(ageRaw),
                    ageRange    = "$ageRaw yr",
                    ageGroupId  = groupId,
                    source      = DatasetSource.MATHEMATICS,
                    apiQuery    = "Mathematics $topic $ageRaw years",
                    iconEmoji   = "🔢",
                    accentColor = DatasetSource.MATHEMATICS.colorHex
                )
            }

    private fun loadSafetyDataset(groupId: Int, agePrefix: String): List<Milestone> =
        openCsv("good_bad_touch_data.csv")
            .filter { row -> row.col("age_group").contains(agePrefix) }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age_group"); val env = row.col("environment"); val topic = row.col("topic")
                Milestone(
                    id          = "safety_g${groupId}_$idx",
                    title       = topic.take(50),
                    subtitle    = row.col("learning_goal").take(70),
                    domain      = "Safety",
                    ageMonths   = when {
                        ageRaw.contains("4") -> 48; ageRaw.contains("6") -> 72; else -> 108
                    },
                    ageRange    = "$ageRaw yr",
                    ageGroupId  = groupId,
                    source      = DatasetSource.SAFETY,
                    apiQuery    = "child safety $topic $env $ageRaw years",
                    iconEmoji   = emojiSafety(env),
                    accentColor = DatasetSource.SAFETY.colorHex
                )
            }

    // ── CSV helpers ───────────────────────────────────────────────────────────

    private fun openCsv(filename: String): List<Map<String, String>> {
        return try {
            val stream  = context.assets.open("datasets/$filename")
            val reader  = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            val lines   = reader.readLines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return emptyList()
            val headers = parseCsvLine(lines[0].trimStart('\uFEFF'))
            lines.drop(1).mapNotNull { line ->
                val values = parseCsvLine(line)
                if (values.size < headers.size) return@mapNotNull null
                headers.zip(values).toMap()
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>(); val current = StringBuilder()
        var inQuotes = false; var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && !inQuotes -> inQuotes = true
                ch == '"' && inQuotes  -> {
                    if (i + 1 < line.length && line[i + 1] == '"') { current.append('"'); i++ }
                    else inQuotes = false
                }
                ch == ',' && !inQuotes -> { result.add(current.toString().trim()); current.clear() }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun Map<String, String>.col(vararg keys: String): String {
        for (k in keys) {
            val v = (this as Map<*, *>)[k]
            if (v is String && v.isNotBlank()) return v.trim()
        }
        return ""
    }

    // ── Age helpers ───────────────────────────────────────────────────────────

    private fun groupMonthRange(groupId: Int): Pair<Int, Int> = when (groupId) {
        1  -> 0  to 2;  2  -> 3  to 5;  3  -> 6  to 8
        4  -> 9  to 11; 5  -> 12 to 17; 6  -> 18 to 23
        7  -> 24 to 35; 8  -> 36 to 47; 9  -> 48 to 59
        10 -> 60 to 83; 11 -> 84 to 107; 12 -> 108 to 131
        else -> 132 to 144
    }

    private fun parseAgeMonth(raw: String): Int {
        val s = raw.trim()
        return when {
            s.toIntOrNull() != null -> s.toInt()
            s.contains("-")        -> s.split("-")[0].trim().toIntOrNull() ?: 0
            s.contains("–")        -> s.split("–")[0].trim().toIntOrNull() ?: 0
            else                   -> 0
        }
    }

    private fun parseAgeYearToMonths(raw: String): Int {
        val s = raw.trim()
        return when {
            s.contains("-")            -> (s.split("-")[0].trim().toDoubleOrNull() ?: 0.0).times(12).toInt()
            s.contains("–")            -> (s.split("–")[0].trim().toDoubleOrNull() ?: 0.0).times(12).toInt()
            s.toDoubleOrNull() != null -> (s.toDouble() * 12).toInt()
            else                       -> 60
        }
    }

    /**
     * Convert a raw age_group string from CSV to an approximate month value.
     *
     * FIX: Old code used String.contains() which caused false matches:
     *   "108".contains("8") == true  → 8-year bucket hit for an 11-year row
     *   "11".contains("1")  == true  → 1-year bucket hit for an 11-year row
     *
     * New code: extract the FIRST integer token from the string, then switch
     * on its exact numeric value. "9-11" → firstInt=9 → 108 mo. Safe.
     */
    private fun ageRawToMonths(raw: String): Int {
        // Extract the first numeric token, e.g. "9-11 yr" → 9, "11" → 11
        val firstInt = raw.trim()
            .split(Regex("[^0-9]"))   // split on any non-digit
            .firstOrNull { it.isNotEmpty() }
            ?.toIntOrNull() ?: return 60

        return when (firstInt) {
            5    -> 60
            6    -> 72
            7    -> 84
            8    -> 96
            9    -> 108
            10   -> 120
            11   -> 132
            12   -> 144
            else -> firstInt * 12   // fallback: treat as year and convert
        }
    }

    // ── Emoji helpers ─────────────────────────────────────────────────────────

    private fun emoji0to24(d: String) = when {
        d.contains("motor")      -> "💪"; d.contains("language")   -> "💬"
        d.contains("sensory")    -> "👀"; d.contains("emotional")  -> "😊"
        d.contains("montessori") -> "🎨"; d.contains("social")     -> "🤝"
        d.contains("sound")      -> "🔊"; else                     -> "🌱"
    }

    private fun emojiParent(d: String) = when {
        d.contains("feed")         -> "🍼"; d.contains("sleep")        -> "🌙"
        d.contains("hygiene")      -> "🛁"; d.contains("safety")       -> "🛡️"
        d.contains("emotional")    -> "❤️"; d.contains("development")  -> "🌱"
        d.contains("teamwork")     -> "🤝"; d.contains("behavior")     -> "⭐"
        d.contains("independence") -> "🦅"; d.contains("cognitive")    -> "🧠"
        d.contains("language")     -> "🗣️"; d.contains("academic")     -> "📚"
        d.contains("social")       -> "👥"; d.contains("motor")        -> "🏃"
        else                       -> "👨‍👩‍👧"
    }

    private fun emoji24to60(d: String) = when {
        d.contains("cognitive")      -> "🧠"; d.contains("creativity")     -> "🎨"
        d.contains("daily life")     -> "🏠"; d.contains("early learning") -> "📖"
        d.contains("emotional")      -> "❤️"; d.contains("language")       -> "💬"
        d.contains("social")         -> "🤝"; d.contains("nature")         -> "🌿"
        d.contains("motor")          -> "🏃"; else                         -> "⭐"
    }

    private fun emojiAcademics(d: String) = when {
        d.contains("math")     -> "🔢"; d.contains("literacy") -> "🔤"
        d.contains("reading")  -> "📖"; d.contains("writing")  -> "✏️"
        d.contains("art")      -> "🎨"; d.contains("music")    -> "🎵"
        d.contains("science")  -> "🔬"; d.contains("social")   -> "🏘️"
        d.contains("physical") -> "⚽"; else                   -> "📚"
    }

    private fun emojiLang(lang: String) = when (lang.lowercase()) {
        "spanish" -> "🇪🇸"; "french" -> "🇫🇷"; "german" -> "🇩🇪"
        "arabic"  -> "🇦🇪"; "italian" -> "🇮🇹"; else   -> "🌐"
    }

    private fun emojiSafety(env: String) = when (env.lowercase()) {
        "home"    -> "🏠"; "school"  -> "🏫"; "outdoor" -> "🌳"
        "online"  -> "💻"; "public"  -> "🏙️"; else      -> "🛡️"
    }
}