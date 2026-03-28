package com.example.babyparenting.data.local

import android.content.Context
import com.example.babyparenting.data.model.AgeGroup
import com.example.babyparenting.data.model.DatasetSource
import com.example.babyparenting.data.model.Milestone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class LazyDatasetLoader(private val context: Context) {

    private val loadedGroups = mutableMapOf<Int, List<Milestone>>()

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

    suspend fun loadForGroup(groupId: Int): List<Milestone> =
        withContext(Dispatchers.IO) {
            loadedGroups.getOrPut(groupId) {
                val raw = when (groupId) {
                    in 1..6  -> loadGroup_0_24(groupId)
                    in 7..9  -> loadGroup_24_60(groupId)
                    10       -> loadGroup_5_7(groupId)
                    11       -> loadGroup_7_9(groupId)
                    else     -> loadGroup_9_12(groupId)
                }
                // ── FREEZE FIX ────────────────────────────────────────────────
                // Groups 10–13 load 5–6 CSV files = hundreds of rows.
                // Rendering too many cards at once freezes the UI.
                // Cap each group to 40 milestones max — user sees them 4-by-4
                // anyway, so 40 = 10 full batches which is more than enough.
                // Earlier groups (1–9) are small so take() has no effect.
                raw.take(MAX_MILESTONES_PER_GROUP)
            }
        }

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

    fun ceilingGroupId(childAgeMonths: Int): Int =
        (startingGroupId(childAgeMonths) + 1).coerceAtMost(totalGroups())

    fun totalGroups(): Int = 13

    fun groupsToPreload(childAgeMonths: Int): List<Int> {
        val floor   = startingGroupId(childAgeMonths)
        val ceiling = ceilingGroupId(childAgeMonths)
        return if (floor == ceiling) listOf(floor)
        else listOf(floor, ceiling)
    }

    fun totalGroups_count(): Int = 13

    // ── Group loaders ─────────────────────────────────────────────────────────

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
                    ageRange    = "$age mo",
                    ageGroupId  = groupId,
                    source      = DatasetSource.CHILD_0_24,
                    apiQuery    = "$domain $skill $age months",
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
                val skill  = row.col("skill_name")
                Milestone(
                    id          = "p0_24_g${groupId}_$idx",
                    title       = skill.take(50),
                    subtitle    = row.col("parent_learning_goal").take(70),
                    domain      = domain,
                    ageMonths   = age,
                    ageRange    = "$age mo",
                    ageGroupId  = groupId,
                    source      = DatasetSource.PARENT_0_24,
                    apiQuery    = "$domain $skill $age months parent",
                    iconEmoji   = emojiParent(domain),
                    accentColor = DatasetSource.PARENT_0_24.colorHex
                )
            }

        return (child + parent).sortedBy { it.ageMonths }
    }

    private fun loadGroup_24_60(groupId: Int): List<Milestone> {
        val (startMo, endMo) = groupMonthRange(groupId)

        val child = openCsv("24_60_month_data.csv")
            .filter { row ->
                val age = parseAgeMonth(row.col("age_month_range"))
                age in startMo..endMo
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age_month_range")
                val age    = parseAgeMonth(ageRaw)
                val domain = row.col("domain")
                val act    = row.col("activity")
                Milestone(
                    id          = "c24_60_g${groupId}_$idx",
                    title       = act.take(50),
                    subtitle    = row.col("goal").take(70),
                    domain      = domain,
                    ageMonths   = age,
                    ageRange    = "$ageRaw mo",
                    ageGroupId  = groupId,
                    source      = DatasetSource.CHILD_24_60,
                    apiQuery    = "$domain $act $ageRaw months",
                    iconEmoji   = emoji24to60(domain),
                    accentColor = DatasetSource.CHILD_24_60.colorHex
                )
            }

        val parent = openCsv("25_60_data_parent.csv")
            .filter { row ->
                val age = parseAgeMonth(row.col("age_month"))
                age in startMo..endMo
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age_month")
                val age    = parseAgeMonth(ageRaw)
                val domain = row.col("domain")
                val skill  = row.col("skill_name")
                Milestone(
                    id          = "p24_60_g${groupId}_$idx",
                    title       = skill.take(50),
                    subtitle    = row.col("parent_learning_goal").take(70),
                    domain      = domain,
                    ageMonths   = age,
                    ageRange    = "$ageRaw mo",
                    ageGroupId  = groupId,
                    source      = DatasetSource.PARENT_24_60,
                    apiQuery    = "$domain $skill $ageRaw months parent",
                    iconEmoji   = emojiParent(domain),
                    accentColor = DatasetSource.PARENT_24_60.colorHex
                )
            }

        val startYr = startMo / 12.0
        val endYr   = endMo   / 12.0
        val acad = openCsv("2_5_academics_data.csv")
            .filter { row ->
                val yr = parseAgeYearToMonths(row.col("age_range")) / 12.0
                yr in startYr..endYr
            }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age_range")
                val age    = parseAgeYearToMonths(ageRaw)
                val domain = row.col("domain")
                val act    = row.col("activity")
                Milestone(
                    id          = "acad_g${groupId}_$idx",
                    title       = act.take(50),
                    subtitle    = row.col("goal").take(70),
                    domain      = domain,
                    ageMonths   = age,
                    ageRange    = "$ageRaw yr",
                    ageGroupId  = groupId,
                    source      = DatasetSource.PRE_ACADEMICS,
                    apiQuery    = "$domain $act $ageRaw years preschool",
                    iconEmoji   = emojiAcademics(domain),
                    accentColor = DatasetSource.PRE_ACADEMICS.colorHex
                )
            }

        return (child + parent + acad).sortedBy { it.ageMonths }
    }

    /** Group 10: 5–7 years (60–83 months) */
    private fun loadGroup_5_7(groupId: Int): List<Milestone> {
        // ── FREEZE FIX: load each CSV with a per-source cap ──────────────────
        // Each source contributes at most SLICE rows so the combined list
        // stays well under MAX_MILESTONES_PER_GROUP before the outer take().
        val child  = loadSubjectFilter("5_12_year_data.csv", groupId, DatasetSource.CHILD_5_12, "🏃",
            ageKey = "age_range", ageParser = ::parseAgeYearToMonths, minMo = 60, maxMo = 84)
            .take(SLICE)
        val parent = loadParentFilter("5_12_year_data_parent.csv", groupId, DatasetSource.PARENT_5_12,
            ageKey = "age_year", minMo = 60, maxMo = 84)
            .take(SLICE)
        val lang   = loadSubjectFilter("language_5_12_data.csv", groupId, DatasetSource.LANGUAGE, "🗣️",
            ageKey = "age", ageParser = { (it.toDoubleOrNull() ?: 5.0).times(12).toInt() }, minMo = 60, maxMo = 84)
            .take(SLICE)
        val maths  = loadSubjectDataset("maths_5_12_data.csv", groupId, DatasetSource.MATHEMATICS, "🔢", "5")
            .take(SLICE)
        val safety = loadSafetyDataset(groupId, "6")
            .take(SLICE)
        return (child + parent + lang + maths + safety).sortedBy { it.ageMonths }
    }

    /** Group 11: 7–9 years (84–107 months) */
    private fun loadGroup_7_9(groupId: Int): List<Milestone> {
        val child   = loadSubjectFilter("5_12_year_data.csv", groupId, DatasetSource.CHILD_5_12, "🏃",
            ageKey = "age_range", ageParser = ::parseAgeYearToMonths, minMo = 84, maxMo = 108)
            .take(SLICE)
        val parent  = loadParentFilter("5_12_year_data_parent.csv", groupId, DatasetSource.PARENT_5_12,
            ageKey = "age_year", minMo = 84, maxMo = 108)
            .take(SLICE)
        val science = loadSubjectDataset("science_5_12_data.csv", groupId, DatasetSource.SCIENCE, "🔬", "8")
            .take(SLICE)
        val social  = loadSubjectDataset("social_5_12_data.csv", groupId, DatasetSource.SOCIAL_STUDIES, "🏘️", "8")
            .take(SLICE)
        val civics  = loadSubjectDataset("civics_evs_5_12_data.csv", groupId, DatasetSource.CIVICS, "🏛️", "8")
            .take(SLICE)
        return (child + parent + science + social + civics).sortedBy { it.ageMonths }
    }

    /** Groups 12–13: 9–12 years */
    private fun loadGroup_9_12(groupId: Int): List<Milestone> {
        val (minMo, maxMo) = when (groupId) {
            12   -> 108 to 131
            else -> 132 to 144
        }
        val child   = loadSubjectFilter("5_12_year_data.csv", groupId, DatasetSource.CHILD_5_12, "🏃",
            ageKey = "age_range", ageParser = ::parseAgeYearToMonths, minMo = minMo, maxMo = maxMo)
            .take(SLICE)
        val parent  = loadParentFilter("5_12_year_data_parent.csv", groupId, DatasetSource.PARENT_5_12,
            ageKey = "age_year", minMo = minMo, maxMo = maxMo)
            .take(SLICE)
        val cs      = loadSubjectDataset("cs_5_12_data.csv", groupId, DatasetSource.COMPUTER_SCIENCE, "💻", "11")
            .take(SLICE)
        val foreign = loadForeignDataset(groupId, "11")
            .take(SLICE)
        val safety  = loadSafetyDataset(groupId, "9")
            .take(SLICE)
        val lang    = loadSubjectFilter("language_5_12_data.csv", groupId, DatasetSource.LANGUAGE, "🗣️",
            ageKey = "age", ageParser = { (it.toDoubleOrNull() ?: 9.0).times(12).toInt() },
            minMo = minMo, maxMo = maxMo)
            .take(SLICE)
        return (child + parent + cs + foreign + safety + lang).sortedBy { it.ageMonths }
    }

    // ── Generic loaders ───────────────────────────────────────────────────────

    private fun loadSubjectFilter(
        file: String, groupId: Int, source: DatasetSource, emoji: String,
        ageKey: String, ageParser: (String) -> Int, minMo: Int, maxMo: Int
    ): List<Milestone> =
        openCsv(file)
            .filter { row -> ageParser(row.col(ageKey)) in minMo..maxMo }
            .mapIndexed { idx, row ->
                val ageRaw = row.col(ageKey)
                val age    = ageParser(ageRaw)
                val domain = row.col("domain", "subject", "subdomain")
                val skill  = row.col("skill_name", "activity", "skill", "topic")
                Milestone(
                    id          = "${source.name}_g${groupId}_$idx",
                    title       = skill.take(50),
                    subtitle    = row.col("development_goal", "goal", "learning_goal", "parent_learning_goal").take(70),
                    domain      = domain, ageMonths = age, ageRange = ageRaw,
                    ageGroupId  = groupId, source = source,
                    apiQuery    = "$domain $skill $ageRaw years",
                    iconEmoji   = emoji, accentColor = source.colorHex
                )
            }

    private fun loadParentFilter(
        file: String, groupId: Int, source: DatasetSource, ageKey: String, minMo: Int, maxMo: Int
    ): List<Milestone> =
        openCsv(file)
            .filter { row -> parseAgeYearToMonths(row.col(ageKey)) in minMo..maxMo }
            .mapIndexed { idx, row ->
                val ageRaw = row.col(ageKey)
                val age    = parseAgeYearToMonths(ageRaw)
                val domain = row.col("domain")
                val skill  = row.col("skill_name")
                Milestone(
                    id          = "${source.name}_g${groupId}_$idx",
                    title       = skill.take(50),
                    subtitle    = row.col("parent_learning_goal").take(70),
                    domain      = domain, ageMonths = age, ageRange = "$ageRaw yr",
                    ageGroupId  = groupId, source = source,
                    apiQuery    = "$domain $skill $ageRaw years parent",
                    iconEmoji   = emojiParent(domain), accentColor = source.colorHex
                )
            }

    private fun loadSubjectDataset(
        file: String, groupId: Int, source: DatasetSource, emoji: String, agePrefix: String
    ): List<Milestone> =
        openCsv(file)
            .filter { row -> row.col("age_group").contains(agePrefix) }
            .mapIndexed { idx, row ->
                val ageRaw  = row.col("age_group")
                val subject = row.col("subject")
                val topic   = row.col("topic")
                Milestone(
                    id          = "${source.name}_g${groupId}_$idx",
                    title       = topic.take(50).ifBlank { subject.take(50) },
                    subtitle    = row.col("input").take(70).ifBlank { topic.take(70) },
                    domain      = subject, ageMonths = ageRawToMonths(ageRaw),
                    ageRange    = "$ageRaw yr", ageGroupId = groupId, source = source,
                    apiQuery    = "$subject $topic $ageRaw years",
                    iconEmoji   = emoji, accentColor = source.colorHex
                )
            }

    private fun loadForeignDataset(groupId: Int, agePrefix: String): List<Milestone> =
        openCsv("foreign_5_12_data.csv")
            .filter { row -> row.col("age_group").contains(agePrefix) }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age_group"); val lang = row.col("language"); val topic = row.col("topic")
                Milestone(
                    id = "foreign_g${groupId}_$idx", title = "$lang: ${topic.take(35)}",
                    subtitle = row.col("input").take(70), domain = "Foreign Language",
                    ageMonths = ageRawToMonths(ageRaw), ageRange = "$ageRaw yr",
                    ageGroupId = groupId, source = DatasetSource.FOREIGN_LANGUAGE,
                    apiQuery = "Foreign language $lang $topic $ageRaw years",
                    iconEmoji = emojiLang(lang), accentColor = DatasetSource.FOREIGN_LANGUAGE.colorHex
                )
            }

    private fun loadSafetyDataset(groupId: Int, agePrefix: String): List<Milestone> =
        openCsv("good_bad_touch_data.csv")
            .filter { row -> row.col("age_group").contains(agePrefix) }
            .mapIndexed { idx, row ->
                val ageRaw = row.col("age_group"); val env = row.col("environment"); val topic = row.col("topic")
                Milestone(
                    id = "safety_g${groupId}_$idx", title = topic.take(50),
                    subtitle = row.col("learning_goal").take(70), domain = "Safety",
                    ageMonths = when {
                        ageRaw.contains("4") -> 48; ageRaw.contains("6") -> 72; else -> 108
                    },
                    ageRange = "$ageRaw yr", ageGroupId = groupId, source = DatasetSource.SAFETY,
                    apiQuery = "child safety $topic $env $ageRaw years",
                    iconEmoji = emojiSafety(env), accentColor = DatasetSource.SAFETY.colorHex
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

    private fun ageRawToMonths(raw: String): Int {
        val firstInt = raw.trim()
            .split(Regex("[^0-9]"))
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
            else -> firstInt * 12
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

    companion object {
        // Har ek CSV source se max kitne milestones lene hain
        // 6 sources × 8 = 48 → outer take(40) se 40 final milestones
        private const val SLICE = 8

        // Ek group mein total max milestones (UI mein 4-4 batch = 10 batches)
        private const val MAX_MILESTONES_PER_GROUP = 40
    }
}