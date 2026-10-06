package com.jideeh.kanjilock

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.github.luben.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.roundToLong
import org.json.JSONArray
import org.json.JSONObject

data class Word(
    val id: String,
    val word: String,
    val reading: String,
    val meaning: String,
    val example: String = "",
    val exampleReading: String = "",
    val exampleMeaning: String = "",
    val type: String = "word",
    val source: String = "dict",
    val addedOn: String = "",
    //pos for words, on kun stuff for kanji
    val info: String = "",
    val altReadings: List<String> = emptyList(), //other readings that also count as correct
    val exampleId: String = "" // tatoeba sentence id, blank if none
) {
    val key: String get() = "$word|$reading"
    val isKanji: Boolean get() = type == "kanji"
    val isMined: Boolean get() = source == "mined"

    fun exampleLine(): String = buildString {
        if (example.isNotBlank()) append(example)
        if (exampleMeaning.isNotBlank()) {
            if (isNotEmpty()) append("  ・  ")
            append(exampleMeaning)
        }
    }
}

object Prefs {
    private const val FILE = "kanjilock_prefs"

    private const val KEY_WORDS_PER_DAY = "words_per_day"
    private const val KEY_SOURCE = "source"
    private const val KEY_DISPLAY_MODE = "display_mode"
    private const val KEY_LOCK_NOTIF = "lock_notif"
    private const val KEY_NOTIF_NUDGE = "notif_nudge_dp"

    private const val KEY_DAY = "state_day"
    private const val KEY_TODAY_IDS = "state_today_ids"
    private const val KEY_INDEX = "state_index"
    private const val KEY_DISMISSED = "state_dismissed"

    const val SOURCE_DICT = "DICT"
    const val SOURCE_MINED = "MINED"
    const val SOURCE_BOTH = "BOTH"
    const val MODE_WORD = "WORD"
    const val MODE_KANJI = "KANJI"
    const val MODE_BOTH = "BOTH"

    const val MAX_WORDS_PER_DAY = 10

    //android 12+ puts like a 72dp gap on the left for the icon but only 16 on the right
    //so the buttons look off center, this pushes them back
    val DEFAULT_NUDGE_DP: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 56 else 0

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun wordsPerDay(ctx: Context): Int = sp(ctx).getInt(KEY_WORDS_PER_DAY, 1).coerceIn(1, MAX_WORDS_PER_DAY)
    fun setWordsPerDay(ctx: Context, n: Int) =
        sp(ctx).edit().putInt(KEY_WORDS_PER_DAY, n.coerceIn(1, MAX_WORDS_PER_DAY)).apply()

    fun source(ctx: Context): String = sp(ctx).getString(KEY_SOURCE, SOURCE_BOTH) ?: SOURCE_BOTH
    fun setSource(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_SOURCE, v).apply()

    fun displayMode(ctx: Context): String = sp(ctx).getString(KEY_DISPLAY_MODE, MODE_BOTH) ?: MODE_BOTH
    fun setDisplayMode(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_DISPLAY_MODE, v).apply()

    fun lockNotif(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_LOCK_NOTIF, true)
    fun setLockNotif(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_LOCK_NOTIF, v).apply()

    fun notifNudge(ctx: Context): Int = sp(ctx).getInt(KEY_NOTIF_NUDGE, DEFAULT_NUDGE_DP)
    fun setNotifNudge(ctx: Context, dp: Int) = sp(ctx).edit().putInt(KEY_NOTIF_NUDGE, dp.coerceIn(0, 96)).apply()

    const val BG_SOLID = "SOLID"
    const val BG_SEMI = "SEMI"
    const val BG_CLEAR = "CLEAR"

    fun widgetBg(ctx: Context): String = sp(ctx).getString("widget_bg", BG_SOLID) ?: BG_SOLID
    fun setWidgetBg(ctx: Context, v: String) = sp(ctx).edit().putString("widget_bg", v).apply()

    fun newPerDay(ctx: Context): Int = sp(ctx).getInt("new_per_day", 10).coerceIn(0, 999)
    fun setNewPerDay(ctx: Context, n: Int) = sp(ctx).edit().putInt("new_per_day", n.coerceIn(0, 999)).apply()

    fun newSeenToday(ctx: Context, day: String, deck: String = "accepted"): Int =
        if (sp(ctx).getString("new_seen_day_$deck", "") == day) sp(ctx).getInt("new_seen_count_$deck", 0) else 0

    fun markNewSeen(ctx: Context, day: String, deck: String = "accepted") {
        val n = newSeenToday(ctx, day, deck) + 1
        sp(ctx).edit().putString("new_seen_day_$deck", day).putInt("new_seen_count_$deck", n).apply()
    }

    fun selectedDeck(ctx: Context): String = sp(ctx).getString("selected_deck", "accepted") ?: "accepted"
    fun setSelectedDeck(ctx: Context, id: String) = sp(ctx).edit().putString("selected_deck", id).apply()

    fun sfx(ctx: Context): Boolean = sp(ctx).getBoolean("sfx", true)
    fun setSfx(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("sfx", v).apply()

    //how much u need to do for the flame to be full
    fun dailyGoal(ctx: Context): Int = sp(ctx).getInt("daily_goal", 10).coerceIn(1, 500)
    fun setDailyGoal(ctx: Context, n: Int) = sp(ctx).edit().putInt("daily_goal", n.coerceIn(1, 500)).apply()

    fun remindDaily(ctx: Context): Boolean = sp(ctx).getBoolean("remind_daily", true)
    fun setRemindDaily(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("remind_daily", v).apply()
    fun remindMinutes(ctx: Context): Int = sp(ctx).getInt("remind_minutes", 19 * 60)
    fun setRemindMinutes(ctx: Context, m: Int) = sp(ctx).edit().putInt("remind_minutes", m).apply()
    fun remindStreak(ctx: Context): Boolean = sp(ctx).getBoolean("remind_streak", true)
    fun setRemindStreak(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("remind_streak", v).apply()
    fun streakMinutes(ctx: Context): Int = sp(ctx).getInt("streak_minutes", 21 * 60 + 30)
    fun setStreakMinutes(ctx: Context, m: Int) = sp(ctx).edit().putInt("streak_minutes", m).apply()

    fun driveLinked(ctx: Context): Boolean = sp(ctx).getBoolean("drive_linked", false)
    fun setDriveLinked(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("drive_linked", v).apply()
    fun driveState(ctx: Context): String = sp(ctx).getString("drive_state", "") ?: ""
    fun setDriveState(ctx: Context, v: String) = sp(ctx).edit().putString("drive_state", v).apply()
    fun lastSyncAt(ctx: Context): Long = sp(ctx).getLong("drive_last_sync", 0L)
    fun syncedHash(ctx: Context): String = sp(ctx).getString("drive_hash", "") ?: ""
    fun remoteModified(ctx: Context): String = sp(ctx).getString("drive_remote_mod", "") ?: ""
    fun setSyncMeta(ctx: Context, hash: String, remoteMod: String) = sp(ctx).edit()
        .putString("drive_hash", hash).putString("drive_remote_mod", remoteMod)
        .putLong("drive_last_sync", System.currentTimeMillis()).apply()
    fun clearSyncMeta(ctx: Context) = sp(ctx).edit().remove("drive_hash").remove("drive_remote_mod").remove("drive_last_sync").apply()

    //only the settings that go in backups
    fun exportSettings(ctx: Context): Map<String, Any> = mapOf(
        KEY_WORDS_PER_DAY to wordsPerDay(ctx), KEY_SOURCE to source(ctx), KEY_DISPLAY_MODE to displayMode(ctx),
        KEY_LOCK_NOTIF to lockNotif(ctx), KEY_NOTIF_NUDGE to notifNudge(ctx), "widget_bg" to widgetBg(ctx),
        "new_per_day" to newPerDay(ctx), "sfx" to sfx(ctx), "daily_goal" to dailyGoal(ctx),
        "remind_daily" to remindDaily(ctx), "remind_minutes" to remindMinutes(ctx),
        "remind_streak" to remindStreak(ctx), "streak_minutes" to streakMinutes(ctx)
    )

    fun importSettings(ctx: Context, m: Map<String, Any?>) {
        val e = sp(ctx).edit()
        (m[KEY_WORDS_PER_DAY] as? Number)?.let { e.putInt(KEY_WORDS_PER_DAY, it.toInt()) }
        (m[KEY_SOURCE] as? String)?.let { e.putString(KEY_SOURCE, it) }
        (m[KEY_DISPLAY_MODE] as? String)?.let { e.putString(KEY_DISPLAY_MODE, it) }
        (m[KEY_LOCK_NOTIF] as? Boolean)?.let { e.putBoolean(KEY_LOCK_NOTIF, it) }
        (m[KEY_NOTIF_NUDGE] as? Number)?.let { e.putInt(KEY_NOTIF_NUDGE, it.toInt()) }
        (m["widget_bg"] as? String)?.let { e.putString("widget_bg", it) }
        (m["new_per_day"] as? Number)?.let { e.putInt("new_per_day", it.toInt()) }
        (m["sfx"] as? Boolean)?.let { e.putBoolean("sfx", it) }
        (m["daily_goal"] as? Number)?.let { e.putInt("daily_goal", it.toInt()) }
        (m["remind_daily"] as? Boolean)?.let { e.putBoolean("remind_daily", it) }
        (m["remind_minutes"] as? Number)?.let { e.putInt("remind_minutes", it.toInt()) }
        (m["remind_streak"] as? Boolean)?.let { e.putBoolean("remind_streak", it) }
        (m["streak_minutes"] as? Number)?.let { e.putInt("streak_minutes", it.toInt()) }
        e.apply()
    }

    fun day(ctx: Context): String = sp(ctx).getString(KEY_DAY, "") ?: ""
    fun todayIds(ctx: Context): List<String> =
        (sp(ctx).getString(KEY_TODAY_IDS, "") ?: "").split(",").filter { it.isNotBlank() }
    fun index(ctx: Context): Int = sp(ctx).getInt(KEY_INDEX, 0)
    fun dismissed(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_DISMISSED, false)

    fun saveDailyState(ctx: Context, day: String, ids: List<String>, index: Int, dismissed: Boolean) {
        sp(ctx).edit()
            .putString(KEY_DAY, day)
            .putString(KEY_TODAY_IDS, ids.joinToString(","))
            .putInt(KEY_INDEX, index)
            .putBoolean(KEY_DISMISSED, dismissed)
            .apply()
    }
}

private fun readJsonArray(f: File): JSONArray =
    try { if (f.exists()) JSONArray(f.readText()) else JSONArray() } catch (_: Exception) { JSONArray() }

private fun Word.toJson(): JSONObject = JSONObject()
    .put("id", id).put("word", word).put("reading", reading).put("meaning", meaning)
    .put("example", example).put("exampleReading", exampleReading)
    .put("exampleMeaning", exampleMeaning).put("type", type).put("source", source)
    .put("addedOn", addedOn).put("info", info).put("alt", JSONArray(altReadings)).put("exId", exampleId)

private fun wordFromJson(o: JSONObject, fallbackSource: String): Word = Word(
    id = o.optString("id"),
    word = o.optString("word"),
    reading = o.optString("reading"),
    meaning = o.optString("meaning"),
    example = o.optString("example"),
    exampleReading = o.optString("exampleReading"),
    exampleMeaning = o.optString("exampleMeaning"),
    type = o.optString("type", "word"),
    source = o.optString("source", fallbackSource),
    addedOn = o.optString("addedOn"),
    info = o.optString("info"),
    altReadings = o.optJSONArray("alt")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
    exampleId = o.optString("exId")
)

private fun readWords(f: File, fallbackSource: String): List<Word> {
    val arr = readJsonArray(f)
    return (0 until arr.length()).map { wordFromJson(arr.getJSONObject(it), fallbackSource) }
}

private fun writeWords(f: File, words: List<Word>) {
    val arr = JSONArray()
    words.forEach { arr.put(it.toJson()) }
    f.writeText(arr.toString())
}


object MinedStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "mined.json")

    fun all(ctx: Context): List<Word> = readWords(file(ctx), "mined")

    fun add(ctx: Context, words: List<Word>): Int {
        val current = all(ctx).toMutableList()
        val existing = current.map { it.key }.toMutableSet()
        var next = current.mapNotNull { it.id.removePrefix("m").toIntOrNull() }.maxOrNull()?.plus(1) ?: 0
        var added = 0
        for (w in words) {
            if (w.word.isBlank() || w.key in existing) continue
            current.add(w.copy(id = "m$next", source = "mined"))
            existing.add(w.key)
            next++; added++
        }
        writeWords(file(ctx), current)
        return added
    }

    fun remove(ctx: Context, id: String) = writeWords(file(ctx), all(ctx).filterNot { it.id == id })
}

object RejectedStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "rejected.json")

    fun all(ctx: Context): List<Word> = readWords(file(ctx), "dict").reversed()
    fun keys(ctx: Context): Set<String> = readWords(file(ctx), "dict").map { it.key }.toSet()

    fun add(ctx: Context, w: Word) {
        val list = readWords(file(ctx), "dict").filterNot { it.key == w.key } + w
        writeWords(file(ctx), list)
    }

    fun remove(ctx: Context, key: String) =
        writeWords(file(ctx), readWords(file(ctx), "dict").filterNot { it.key == key })

    fun clear(ctx: Context) = writeWords(file(ctx), emptyList())
}

//json is the real data, csv just gets rebuilt everytime so deleting stuff actualy removes the row
object AcceptedStore {
    const val INSTRUCTION = "Type the reading!"
    const val RENDER_AS = "Image"
    private val HEADER = listOf("Questions", "Answers", "Comment", "Instructions", "Render as")

    fun csvFile(ctx: Context) = File(ctx.filesDir, "accepted.csv")
    private fun jsonFile(ctx: Context) = File(ctx.filesDir, "accepted.json")

    fun all(ctx: Context): List<Word> = readWords(jsonFile(ctx), "dict").reversed()
    fun keys(ctx: Context): Set<String> = readWords(jsonFile(ctx), "dict").map { it.key }.toSet()
    fun contains(ctx: Context, w: Word): Boolean = w.key in keys(ctx)

    //accepting today counts for todays flame
    fun add(ctx: Context, w: Word): Boolean {
        val list = readWords(jsonFile(ctx), "dict")
        if (list.any { it.key == w.key }) return false
        val day = w.addedOn.ifBlank { ActivityLog.dayKey() }
        save(ctx, list + w.copy(addedOn = day))
        if (day == ActivityLog.dayKey()) ActivityLog.record(ctx, day, +1)
        return true
    }

    //undo takes the credit back otherwise u could spam accept undo lol
    fun remove(ctx: Context, key: String) {
        val list = readWords(jsonFile(ctx), "dict")
        val gone = list.firstOrNull { it.key == key } ?: return
        save(ctx, list.filterNot { it.key == key })
        if (gone.addedOn == ActivityLog.dayKey()) ActivityLog.record(ctx, gone.addedOn, -1)
    }

    fun clear(ctx: Context) = save(ctx, emptyList())

    fun rebuildCsv(ctx: Context) = save(ctx, readWords(jsonFile(ctx), "dict"))

    fun ankiFile(ctx: Context): File {
        val sb = StringBuilder()
        sb.append("#separator:tab\n#html:true\n#notetype:Basic\n#deck:KanjiLock\n#tags:kanjilock\n#columns:Front\tBack\n")
        for (w in readWords(jsonFile(ctx), "dict")) {
            val back = buildString {
                append("<div style=\"font-size:1.4em\">").append(html(w.reading)).append("</div>")
                append("<div>").append(html(w.meaning)).append("</div>")
                val ex = w.exampleLine()
                if (ex.isNotBlank()) append("<div style=\"opacity:.7;font-size:.9em\">").append(html(ex)).append("</div>")
            }
            sb.append(html(w.word)).append('\t').append(back).append('\n')
        }
        val f = File(ctx.filesDir, "KanjiLock_anki.txt")
        f.writeText(sb.toString())
        return f
    }

    private fun html(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\t", " ").replace("\r", " ").replace("\n", " ")

    private fun save(ctx: Context, list: List<Word>) {
        writeWords(jsonFile(ctx), list)
        val sb = StringBuilder()
        sb.append(HEADER.joinToString(",") { esc(it) }).append("\r\n")
        for (w in list) {
            sb.append(listOf(w.word, w.reading, w.meaning, INSTRUCTION, RENDER_AS).joinToString(",") { esc(it) })
                .append("\r\n")
        }
        csvFile(ctx).writeText(sb.toString())
    }

    private fun esc(s: String): String {
        val needs = s.contains(',') || s.contains('"') || s.contains('\n') || s.contains('\r')
        val body = s.replace("\"", "\"\"")
        return if (needs) "\"$body\"" else body
    }
}

//per day counts, a is accepted r is reviews
//old versions saved just a number so thats handled too
object ActivityLog {
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun file(ctx: Context) = File(ctx.filesDir, "activity.json")

    data class Day(val accepted: Int = 0, val reviews: Int = 0) {
        val total: Int get() = accepted + reviews
    }

    fun dayKey(t: Long = System.currentTimeMillis()): String = fmt.format(Date(t))
    fun dayKey(cal: Calendar): String = fmt.format(cal.time)

    private fun load(ctx: Context): JSONObject =
        try { if (file(ctx).exists()) JSONObject(file(ctx).readText()) else JSONObject() } catch (_: Exception) { JSONObject() }

    fun days(ctx: Context): Map<String, Day> {
        val o = load(ctx)
        return o.keys().asSequence().associateWith { k ->
            when (val v = o.opt(k)) {
                is Number -> Day(accepted = v.toInt())
                is JSONObject -> Day(v.optInt("a"), v.optInt("r"))
                else -> Day()
            }
        }.filterValues { it.total > 0 }
    }

    fun active(ctx: Context): Set<String> = days(ctx).keys

    private fun bump(ctx: Context, day: String, da: Int, dr: Int) {
        val o = load(ctx)
        val cur = when (val v = o.opt(day)) {
            is Number -> Day(accepted = v.toInt())
            is JSONObject -> Day(v.optInt("a"), v.optInt("r"))
            else -> Day()
        }
        val a = (cur.accepted + da).coerceAtLeast(0)
        val r = (cur.reviews + dr).coerceAtLeast(0)
        if (a + r == 0) o.remove(day) else o.put(day, JSONObject().put("a", a).put("r", r))
        file(ctx).writeText(o.toString())
    }

    fun record(ctx: Context, day: String, delta: Int) = bump(ctx, day, delta, 0)
    fun recordReview(ctx: Context, day: String = dayKey()) = bump(ctx, day, 0, 1)

    data class Streak(val current: Int, val longest: Int, val todayDone: Boolean, val activeDays: Int)

    //streak dont die untill the day is over, same as duolingo
    fun streak(days: Set<String>, now: Calendar = Calendar.getInstance()): Streak {
        val c = now.clone() as Calendar
        val todayDone = dayKey(c) in days
        if (!todayDone) c.add(Calendar.DAY_OF_YEAR, -1)
        var current = 0
        while (dayKey(c) in days) { current++; c.add(Calendar.DAY_OF_YEAR, -1) }

        var longest = 0
        var run = 0
        var prev: Calendar? = null
        for (d in days.sorted()) {
            val cal = Calendar.getInstance().apply { time = fmt.parse(d)!! }
            run = if (prev != null && isNextDay(prev, cal)) run + 1 else 1
            longest = maxOf(longest, run)
            prev = cal
        }
        return Streak(current, maxOf(longest, current), todayDone, days.size)
    }

    private fun isNextDay(a: Calendar, b: Calendar): Boolean {
        val x = a.clone() as Calendar
        x.add(Calendar.DAY_OF_YEAR, 1)
        return x.get(Calendar.YEAR) == b.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    fun clear(ctx: Context) = file(ctx).writeText("{}")
}

//tiny review log for the insights card
object RevLog {
    private const val MAX = 20000
    private fun file(ctx: Context) = File(ctx.filesDir, "revlog.json")

    data class Entry(val t: Long, val grade: Int, val stateBefore: Int, val deck: String)

    fun all(ctx: Context): List<Entry> {
        val a = try { if (file(ctx).exists()) JSONArray(file(ctx).readText()) else JSONArray() } catch (_: Exception) { JSONArray() }
        return (0 until a.length()).mapNotNull { i ->
            val e = a.optJSONArray(i) ?: return@mapNotNull null
            Entry(e.optLong(0), e.optInt(1), e.optInt(2), e.optString(3))
        }
    }

    fun add(ctx: Context, grade: Int, stateBefore: Int, deck: String) {
        val a = try { if (file(ctx).exists()) JSONArray(file(ctx).readText()) else JSONArray() } catch (_: Exception) { JSONArray() }
        a.put(JSONArray().put(System.currentTimeMillis()).put(grade).put(stateBefore).put(deck))
        val trimmed = if (a.length() > MAX) JSONArray((a.length() - MAX until a.length()).map { a.get(it) }) else a
        file(ctx).writeText(trimmed.toString())
    }
}


enum class Grade { AGAIN, HARD, GOOD, EASY }

//state 0 new 1 learning 2 review 3 relearning
//interval in days, due in ms
data class SrsCard(
    val key: String,
    val state: Int = NEW,
    val step: Int = 0,
    val interval: Double = 0.0,
    val ease: Double = 2.5,
    val due: Long = 0L,
    val reps: Int = 0,
    val lapses: Int = 0
) {
    companion object {
        const val NEW = 0
        const val LEARNING = 1
        const val REVIEW = 2
        const val RELEARNING = 3
    }
}

// basically anki sm2 defaults. steps 1m 10m, graduate 1d, easy 4d
object Scheduler {
    const val MINUTE = 60_000L
    const val DAY = 86_400_000L
    val learnSteps = listOf(1 * MINUTE, 10 * MINUTE)
    val relearnSteps = listOf(10 * MINUTE)
    const val GRADUATING_DAYS = 1.0
    const val EASY_DAYS = 4.0
    const val MIN_EASE = 1.3
    const val MAX_DAYS = 36500.0

    fun answer(c: SrsCard, g: Grade, now: Long): SrsCard = when (c.state) {
        SrsCard.NEW, SrsCard.LEARNING -> learning(c, g, now, learnSteps)
        SrsCard.RELEARNING -> relearning(c, g, now)
        else -> review(c, g, now)
    }.copy(reps = c.reps + 1)

    //the little times under the buttons
    fun preview(c: SrsCard, now: Long): Map<Grade, Long> =
        Grade.entries.associateWith {
            val n = answer(c, it, now)
            if (n.state == SrsCard.REVIEW) (n.interval * DAY).toLong() else n.due - now
        }

    private fun learning(c: SrsCard, g: Grade, now: Long, steps: List<Long>): SrsCard = when (g) {
        Grade.AGAIN -> c.copy(state = SrsCard.LEARNING, step = 0, due = now + steps[0])
        Grade.HARD -> {
            val delay = if (c.step == 0 && steps.size > 1) (steps[0] + steps[1]) / 2 else steps[c.step.coerceAtMost(steps.size - 1)]
            c.copy(state = SrsCard.LEARNING, due = now + delay)
        }
        Grade.GOOD -> {
            val next = c.step + 1
            if (next < steps.size) c.copy(state = SrsCard.LEARNING, step = next, due = now + steps[next])
            else graduate(c, GRADUATING_DAYS, now)
        }
        Grade.EASY -> graduate(c, EASY_DAYS, now)
    }

    private fun relearning(c: SrsCard, g: Grade, now: Long): SrsCard = when (g) {
        Grade.AGAIN, Grade.HARD -> c.copy(step = 0, due = now + relearnSteps[0])
        Grade.GOOD -> graduate(c, max(1.0, c.interval), now)
        Grade.EASY -> graduate(c, max(1.0, c.interval) + 1, now)
    }

    private fun review(c: SrsCard, g: Grade, now: Long): SrsCard {
        val ivl = max(1.0, c.interval)
        return when (g) {
            Grade.AGAIN -> c.copy(
                state = SrsCard.RELEARNING, step = 0, lapses = c.lapses + 1,
                ease = max(MIN_EASE, c.ease - 0.20), interval = 1.0,
                due = now + relearnSteps[0]
            )
            Grade.HARD -> schedule(c.copy(ease = max(MIN_EASE, c.ease - 0.15)), max(ivl * 1.2, ivl + 1), now)
            Grade.GOOD -> schedule(c, max(ivl * c.ease, hardDays(ivl) + 1), now)
            Grade.EASY -> schedule(c.copy(ease = c.ease + 0.15), max(ivl * c.ease * 1.3, max(ivl * c.ease, hardDays(ivl) + 1) + 1), now)
        }
    }
    private fun hardDays(ivl: Double) = max(ivl * 1.2, ivl + 1)

    private fun graduate(c: SrsCard, days: Double, now: Long) =
        schedule(c.copy(step = 0), days, now)

    private fun schedule(c: SrsCard, days: Double, now: Long): SrsCard {
        val d = days.coerceIn(1.0, MAX_DAYS).roundToLong().toDouble()
        return c.copy(state = SrsCard.REVIEW, interval = d, due = startOfDay(now) + (d * DAY).toLong() + 4 * 3_600_000L)
    }


    fun startOfDay(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun label(ms: Long): String {
        val m = ms / MINUTE.toDouble()
        val d = ms / DAY.toDouble()
        return when {
            m < 1.5 -> "1m"
            m < 60 -> "${m.roundToLong()}m"
            m < 60 * 20 -> "${(m / 60).roundToLong()}h"
            d < 30 -> "${d.roundToLong().coerceAtLeast(1)}d"
            d < 365 -> String.format("%.1fmo", d / 30.0).replace(".0mo", "mo")
            else -> String.format("%.1fy", d / 365.0).replace(".0y", "y")
        }
    }
}

object SrsStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "srs.json")

    fun all(ctx: Context): Map<String, SrsCard> {
        val o = try { if (file(ctx).exists()) JSONObject(file(ctx).readText()) else JSONObject() } catch (_: Exception) { JSONObject() }
        val out = HashMap<String, SrsCard>()
        for (k in o.keys()) {
            val j = o.getJSONObject(k)
            out[k] = SrsCard(
                key = k, state = j.optInt("s"), step = j.optInt("st"), interval = j.optDouble("i", 0.0),
                ease = j.optDouble("e", 2.5), due = j.optLong("d"), reps = j.optInt("r"), lapses = j.optInt("l")
            )
        }
        return out
    }

    fun put(ctx: Context, c: SrsCard) {
        val o = try { if (file(ctx).exists()) JSONObject(file(ctx).readText()) else JSONObject() } catch (_: Exception) { JSONObject() }
        o.put(
            c.key, JSONObject().put("s", c.state).put("st", c.step).put("i", c.interval)
                .put("e", c.ease).put("d", c.due).put("r", c.reps).put("l", c.lapses)
        )
        file(ctx).writeText(o.toString())
    }

    fun clear(ctx: Context) = file(ctx).writeText("{}")
}

//works like a jp keyboard, double consonant gives small tsu and nn gives ん
object Romaji {
    private val table: Map<String, String> = buildMap {
        val vowels = "aiueo"
        put("a", "あ"); put("i", "い"); put("u", "う"); put("e", "え"); put("o", "お")
        val rows = mapOf(
            "k" to "かきくけこ", "g" to "がぎぐげご", "s" to "さしすせそ", "z" to "ざじずぜぞ",
            "t" to "たちつてと", "d" to "だぢづでど", "n" to "なにぬねの", "h" to "はひふへほ",
            "b" to "ばびぶべぼ", "p" to "ぱぴぷぺぽ", "m" to "まみむめも", "r" to "らりるれろ",
            "l" to "らりるれろ"
        )
        for ((c, kana) in rows) for (i in 0..4) put(c + vowels[i], kana[i].toString())
        put("ya", "や"); put("yu", "ゆ"); put("yo", "よ"); put("ye", "いぇ")
        put("wa", "わ"); put("wo", "を"); put("wi", "うぃ"); put("we", "うぇ")
        put("shi", "し"); put("chi", "ち"); put("tsu", "つ"); put("fu", "ふ"); put("ji", "じ")
        put("si", "し"); put("ti", "ち"); put("tu", "つ"); put("hu", "ふ"); put("zi", "じ")
        put("fa", "ふぁ"); put("fi", "ふぃ"); put("fe", "ふぇ"); put("fo", "ふぉ")
        put("va", "ゔぁ"); put("vi", "ゔぃ"); put("vu", "ゔ"); put("ve", "ゔぇ"); put("vo", "ゔぉ")
        put("ja", "じゃ"); put("ju", "じゅ"); put("je", "じぇ"); put("jo", "じょ")
        put("sha", "しゃ"); put("shu", "しゅ"); put("she", "しぇ"); put("sho", "しょ")
        put("cha", "ちゃ"); put("chu", "ちゅ"); put("che", "ちぇ"); put("cho", "ちょ")
        put("tsa", "つぁ"); put("thi", "てぃ"); put("dhi", "でぃ"); put("twu", "とぅ"); put("dwu", "どぅ")
        val yRows = mapOf(
            "k" to "き", "g" to "ぎ", "s" to "し", "z" to "じ", "j" to "じ", "t" to "ち", "c" to "ち",
            "d" to "ぢ", "n" to "に", "h" to "ひ", "b" to "び", "p" to "ぴ", "m" to "み", "r" to "り", "l" to "り"
        )
        for ((c, k) in yRows) { put(c + "ya", k + "ゃ"); put(c + "yu", k + "ゅ"); put(c + "yo", k + "ょ"); put(c + "ye", k + "ぇ") }
        put("xa", "ぁ"); put("xi", "ぃ"); put("xu", "ぅ"); put("xe", "ぇ"); put("xo", "ぉ")
        put("la", "ぁ"); put("li", "ぃ"); put("lu", "ぅ"); put("le", "ぇ"); put("lo", "ぉ")
        put("xya", "ゃ"); put("xyu", "ゅ"); put("xyo", "ょ"); put("lya", "ゃ"); put("lyu", "ゅ"); put("lyo", "ょ")
        put("xtsu", "っ"); put("xtu", "っ"); put("ltsu", "っ"); put("ltu", "っ"); put("xwa", "ゎ")
        put("nn", "ん"); put("n'", "ん"); put("xn", "ん")
        put("-", "ー"); put(".", "。"); put(",", "、"); put("?", "？"); put("!", "！")
    }
    private const val MAX = 4

    //final false means user is still typing so keep the last n as n
    fun toHiragana(input: String, final: Boolean = false): String {
        val s = input.lowercase()
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch !in 'a'..'z' && ch !in "-.,?!'") { out.append(input[i]); i++; continue }

            if (i + 1 < s.length && ch == s[i + 1] && ch in "bcdfghjklmpqrstvwxyz" && ch != 'n') {
                out.append('っ'); i++; continue
            }
            if (ch == 't' && s.startsWith("ch", i + 1)) { out.append('っ'); i++; continue } //matcha
            if (ch == 'c' && i + 2 < s.length && s[i + 1] == 'c' && s[i + 2] == 'h') { out.append('っ'); i++; continue }
            if (ch == 'n') {
                val next = s.getOrNull(i + 1)
                if (next == 'n' && s.getOrNull(i + 2)?.let { it in "aiueoy" } == true) { out.append('ん'); i++; continue }
                if (next == null) { out.append(if (final) "ん" else "n"); i++; continue }
                if (next !in "aiueoy'n") { out.append('ん'); i++; continue }
            }

            var matched = false
            for (len in minOf(MAX, s.length - i) downTo 1) {
                val kana = table[s.substring(i, i + len)]
                if (kana != null) { out.append(kana); i += len; matched = true; break }
            }
            if (!matched) { out.append(input[i]); i++ }
        }
        return out.toString()
    }

    fun hasKana(s: String) = s.any { it in '぀'..'ヿ' }

    //katakana to hiragana so both count
    fun normalize(s: String): String = buildString {
        for (ch in s.trim()) {
            if (ch == ' ' || ch == '　' || ch == '・') continue
            append(if (ch in 'ァ'..'ヶ') (ch - 0x60) else ch)
        }
    }

    fun matches(typed: String, answers: List<String>): Boolean {
        val t = normalize(toHiragana(typed, final = true))
        return t.isNotEmpty() && answers.any { normalize(it) == t || it.trim().equals(typed.trim(), ignoreCase = true) }
    }
}

//anki fields are full of html, this cleans it
object Clean {
    private val br = Regex("(?i)<br\\s*/?>|</div>|</p>|</li>")
    private val tags = Regex("<[^>]+>")
    private val sound = Regex("\\[sound:[^]]*]")
    private val styleBlocks = Regex("(?is)<(style|script)[^>]*>.*?</\\1>")
    private val furigana = Regex("\\s?([^\\s\\[\\]]+)\\[([^\\]]+)]")
    private val spaces = Regex("[ \\t]+")

    fun html(s: String): String {
        var t = styleBlocks.replace(s, "")
        t = br.replace(t, "\n")
        t = tags.replace(t, "")
        t = sound.replace(t, "")
        t = t.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
        return t.lines().map { spaces.replace(it, " ").trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    fun stripFurigana(s: String): String = furigana.replace(s) { it.groupValues[1] } //漢字[かんじ] becomes 漢字

    fun furiganaReading(s: String): String = furigana.replace(s) { it.groupValues[2] }

    fun isKanaOnly(s: String): Boolean =
        s.isNotBlank() && s.all { it in '぀'..'ヿ' || it == 'ー' || it == '・' || it == '、' || it == ' ' || it == '　' }

    fun hasLatin(s: String) = s.any { it in 'a'..'z' || it in 'A'..'Z' }
}


//all the sql for dict.db, no android stuff here so tests can hit the real db
// ids are j + jmdict id for words and k + the kanji itself
class DictQueries(private val db: SqlSource) {

    private val wordCols = "id, word, reading, meaning, pos, ex_jp, ex_en, alt, ex_id"
    private val kanjiCols = "literal, reading, onyomi, kunyomi, meaning, ex_word, ex_reading, ex_meaning, grade, jlpt, strokes"

    fun word(id: String): Word? = when {
        id.startsWith("j") -> db.rows("SELECT $wordCols FROM words WHERE id = ?", listOf(id.drop(1))).firstOrNull()?.let(::toWord)
        id.startsWith("k") -> db.rows("SELECT $kanjiCols FROM kanji WHERE literal = ?", listOf(id.drop(1))).firstOrNull()?.let(::toKanji)
        else -> null
    }

    //common only and no kana only words, nothing to read on those
    fun randomWordIds(n: Int): List<String> =
        db.rows("SELECT id FROM words WHERE common = 1 AND kana_only = 0 ORDER BY RANDOM() LIMIT ?", listOf(n.toString()))
            .map { "j" + (it[0] as Number).toLong() }

    fun randomKanjiIds(n: Int): List<String> =
        db.rows("SELECT literal FROM kanji WHERE grade BETWEEN 1 AND 8 ORDER BY RANDOM() LIMIT ?", listOf(n.toString()))
            .map { "k" + it[0] }

    fun counts(): Pair<Int, Int> {
        val w = (db.rows("SELECT count(*) FROM words").first()[0] as Number).toInt()
        val k = (db.rows("SELECT count(*) FROM kanji").first()[0] as Number).toInt()
        return w to k
    }
    fun credits(): String = db.rows("SELECT value FROM meta WHERE key = 'credits'").firstOrNull()?.get(0)?.toString().orEmpty()

    //exact match first then common then shorter ones
    fun search(raw: String, limit: Int = 60): List<Word> {
        val q = raw.trim().replace(Regex("[*?\\[\\]%_]"), "")
        if (q.isEmpty()) return emptyList()
        val out = LinkedHashMap<String, Word>()
        val latin = q.all { it.code < 0x80 }

        if (q.length == 1 && !latin) word("k$q")?.let { out[it.id] = it }

        val jp = if (latin) Romaji.toHiragana(q.lowercase(), final = true).takeIf { s -> s.none { it in 'a'..'z' } } else q
        if (jp != null) {
            val kata = jp.map { if (it in 'ぁ'..'ゖ') it + 0x60 else it }.joinToString("")
            db.rows(
                """SELECT $wordCols FROM words
                   WHERE word GLOB ? OR reading GLOB ? OR reading GLOB ?
                   ORDER BY (word = ? OR reading = ? OR reading = ?) DESC, common DESC, length(word), id LIMIT ?""",
                listOf("$jp*", "$jp*", "$kata*", jp, jp, kata, limit.toString())
            ).forEach { r -> toWord(r).let { out.putIfAbsent(it.id, it) } }
        }

        if (latin && out.size < limit) {
            val lower = q.lowercase()
            db.rows(
                """SELECT $wordCols FROM words WHERE meaning LIKE ?
                   ORDER BY (meaning LIKE ? OR meaning LIKE ?) DESC, common DESC, length(meaning), id LIMIT ?""",
                listOf("%$lower%", "$lower%", "to $lower%", limit.toString())
            ).forEach { r -> toWord(r).let { out.putIfAbsent(it.id, it) } }
        }
        return out.values.take(limit)
    }

    private fun toWord(r: List<Any?>): Word {
        val text = r[1].toString()
        val isKanji = text.length == 1 && text[0] in '一'..'鿿'
        return Word(
            id = "j" + (r[0] as Number).toLong(),
            word = text,
            reading = r[2].toString(),
            meaning = r[3].toString(),
            example = r[5]?.toString().orEmpty(),
            exampleMeaning = r[6]?.toString().orEmpty(),
            type = if (isKanji) "kanji" else "word",
            source = "dict",
            info = posLabel(r[4]?.toString().orEmpty()),
            altReadings = r[7]?.toString()?.split(';')?.filter { a -> a.isNotEmpty() && a.all { it in '぀'..'ヿ' || it == 'ー' } } ?: emptyList(),
            exampleId = r[8]?.toString().orEmpty()
        )
    }

    private fun toKanji(r: List<Any?>): Word {
        val on = r[2]?.toString()?.split('、')?.filter { it.isNotBlank() } ?: emptyList()
        val kun = r[3]?.toString()?.split('、')?.filter { it.isNotBlank() } ?: emptyList()
        val readings = (on.map { Romaji.normalize(it) } + kun.flatMap { k ->
            val clean = k.replace("-", "")
            listOf(clean.replace(".", ""), clean.substringBefore('.'))
        }).filter { it.isNotBlank() }.distinct()
        val grade = (r[8] as? Number)?.toInt()
        val jlpt = (r[9] as? Number)?.toInt()
        val strokes = (r[10] as? Number)?.toInt()
        val bits = listOfNotNull(
            on.takeIf { it.isNotEmpty() }?.let { "On ${it.joinToString("、")}" },
            kun.takeIf { it.isNotEmpty() }?.let { "Kun ${it.joinToString("、")}" },
            grade?.let { if (it <= 6) "Grade $it" else "Jōyō" },
            jlpt?.let { "Old JLPT $it" },
            strokes?.let { "$it strokes" }
        )
        return Word(
            id = "k" + r[0],
            word = r[0].toString(),
            reading = r[1].toString(),
            meaning = r[4].toString(),
            example = r[5]?.toString().orEmpty(),
            exampleReading = r[6]?.toString().orEmpty(),
            exampleMeaning = r[7]?.toString().orEmpty(),
            type = "kanji",
            source = "dict",
            info = bits.joinToString(" · "),
            altReadings = readings
        )
    }

    companion object {
        private val POS = mapOf(
            "n" to "noun", "v1" to "ichidan verb", "v5" to "godan verb", "vs" to "suru verb", "vt" to "transitive",
            "vi" to "intransitive", "adj-i" to "i-adjective", "adj-na" to "na-adjective", "adj-no" to "no-adjective",
            "adv" to "adverb", "exp" to "expression", "int" to "interjection", "pn" to "pronoun", "prt" to "particle",
            "ctr" to "counter", "suf" to "suffix", "pref" to "prefix", "conj" to "conjunction", "adj-t" to "taru adjective"
        )

        fun posLabel(codes: String): String = codes.split(',').filter { it.isNotBlank() }.map { c ->
            POS[c] ?: if (c.startsWith("v5")) "godan verb" else c
        }.distinct().joinToString(", ")
    }
}


//copies dict.db out of the apk on first run
object Dictionary {
    private const val VERSION = 2
    @Volatile private var queries: DictQueries? = null

    fun queries(ctx: Context): DictQueries {
        queries?.let { return it }
        synchronized(this) {
            queries?.let { return it }
            val app = ctx.applicationContext
            val target = app.getDatabasePath("dict_v$VERSION.db")
            if (!target.exists()) {
                target.parentFile?.mkdirs()
                val tmp = File(target.path + ".tmp")
                //gradle unzips the .gz on build for some reason so check both names
                val input = runCatching { app.assets.open("dict.db") }.getOrNull()
                    ?: java.util.zip.GZIPInputStream(app.assets.open("dict.db.gz"), 1 shl 16)
                input.use { i -> tmp.outputStream().use { i.copyTo(it, 1 shl 16) } }
                tmp.renameTo(target)
                target.parentFile?.listFiles { f -> f.name.startsWith("dict_v") && f.name != target.name }?.forEach { it.delete() }
            }
            return DictQueries(AndroidSql(target)).also { queries = it }
        }
    }

    //do this early on a bg thread so the widget isnt slow the first time
    fun warmUp(ctx: Context) { runCatching { queries(ctx) } }

    fun byId(ctx: Context, id: String): Word? =
        if (id.startsWith("m")) MinedStore.all(ctx).firstOrNull { it.id == id } else queries(ctx).word(id)

    fun search(ctx: Context, q: String): List<Word> = queries(ctx).search(q)

    fun counts(ctx: Context): Pair<Int, Int> = queries(ctx).counts()

    fun credits(ctx: Context): String = queries(ctx).credits()

    // follows the show and draw from settings, skips stuff already accepted or rejected
    fun pick(ctx: Context, skipIds: Set<String>, skipKeys: Set<String>): String? {
        val mined = MinedStore.all(ctx).filter { it.id !in skipIds && it.key !in skipKeys }
        val src = Prefs.source(ctx)
        val useMined = when (src) {
            Prefs.SOURCE_MINED -> true
            Prefs.SOURCE_BOTH -> mined.isNotEmpty() && Math.random() < 0.5
            else -> false
        }
        if (useMined && mined.isNotEmpty()) {
            val mode = Prefs.displayMode(ctx)
            val fit = mined.filter { mode == Prefs.MODE_BOTH || (mode == Prefs.MODE_KANJI) == it.isKanji }
            return (fit.ifEmpty { mined }).random().id
        }
        if (src == Prefs.SOURCE_MINED && mined.isEmpty() && MinedStore.all(ctx).isNotEmpty()) {
            return MinedStore.all(ctx).filter { it.id !in skipIds }.randomOrNull()?.id
        }

        val q = queries(ctx)
        val kanji = when (Prefs.displayMode(ctx)) {
            Prefs.MODE_KANJI -> true
            Prefs.MODE_WORD -> false
            else -> Math.random() < 0.35 //about 1 kanji per 3 words feels right
        }
        repeat(3) {
            val ids = if (kanji) q.randomKanjiIds(30) else q.randomWordIds(30)
            for (id in ids) {
                if (id in skipIds) continue
                val w = q.word(id) ?: continue
                if (w.key !in skipKeys) return id
            }
        }
        return null
    }
}


//shows as the tag on the deck
enum class DeckSource(val label: String) {
    KANJILOCK("KanjiLock"),
    ANKI("Anki"),
    ANKIDROID("AnkiWeb"),
    KOTOBA("Kotoba");

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: ANKI
    }
}

data class Deck(
    val id: String,
    val name: String,
    val source: DeckSource,
    val cardCount: Int = 0,
    val ankiDeckName: String = "",
    val ankiDroidDeckId: Long = 0L, //set after linking, then reviews go thru ankidroid
    val createdAt: Long = 0L
) {
    val isLinked: Boolean get() = source == DeckSource.ANKIDROID && ankiDroidDeckId != 0L
}


//empty answers means just flip it no typing
data class DeckCard(
    val id: String,
    val front: String,
    val reading: String = "",
    val meaning: String = "",
    val extra: String = "",
    val answers: List<String> = emptyList(),
    val instructions: String = ""
)

//accepted deck isnt a real file, its just ur accepted words
object DeckStore {
    const val ACCEPTED = "accepted"

    private fun listFile(ctx: Context) = File(ctx.filesDir, "decks.json")
    fun cardFile(ctx: Context, id: String) = File(ctx.filesDir, "deck_$id.json")

    fun srsKey(deckId: String, cardId: String) = if (deckId == ACCEPTED) cardId else "$deckId::$cardId"

    private fun stored(ctx: Context): List<Deck> {
        val a = try { if (listFile(ctx).exists()) JSONArray(listFile(ctx).readText()) else JSONArray() } catch (_: Exception) { JSONArray() }
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Deck(
                id = o.getString("id"), name = o.optString("name"), source = DeckSource.of(o.optString("source")),
                cardCount = o.optInt("count"), ankiDeckName = o.optString("ankiName"),
                ankiDroidDeckId = o.optLong("ankiDroidId"), createdAt = o.optLong("created")
            )
        }
    }


    private fun save(ctx: Context, decks: List<Deck>) {
        val a = JSONArray()
        decks.forEach {
            a.put(
                JSONObject().put("id", it.id).put("name", it.name).put("source", it.source.name)
                    .put("count", it.cardCount).put("ankiName", it.ankiDeckName)
                    .put("ankiDroidId", it.ankiDroidDeckId).put("created", it.createdAt)
            )
        }
        listFile(ctx).writeText(a.toString())
    }

    fun all(ctx: Context): List<Deck> =
        listOf(Deck(ACCEPTED, "Accepted words", DeckSource.KANJILOCK, AcceptedStore.keys(ctx).size)) + stored(ctx)

    fun get(ctx: Context, id: String): Deck? = all(ctx).firstOrNull { it.id == id }

    fun cards(ctx: Context, id: String): List<DeckCard> {
        if (id == ACCEPTED) {
            return AcceptedStore.all(ctx).reversed().map {
                DeckCard(it.key, it.word, it.reading, it.meaning, it.exampleLine(), (listOf(it.reading) + it.altReadings).distinct(), "Type the reading!")
            }
        }
        val f = cardFile(ctx, id)
        val a = try { if (f.exists()) JSONArray(f.readText()) else JSONArray() } catch (_: Exception) { JSONArray() }
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val ans = o.optJSONArray("ans") ?: JSONArray()
            DeckCard(
                o.getString("id"), o.optString("f"), o.optString("r"), o.optString("m"), o.optString("x"),
                (0 until ans.length()).map { ans.getString(it) }, o.optString("i")
            )
        }
    }

    fun create(ctx: Context, name: String, source: DeckSource, cards: List<DeckCard>, ankiName: String = "", ankiDroidId: Long = 0L): Deck {
        val d = Deck("d${System.currentTimeMillis()}", name, source, cards.size, ankiName, ankiDroidId, System.currentTimeMillis())
        writeCards(ctx, d.id, cards)
        save(ctx, stored(ctx) + d)
        return d
    }

    fun update(ctx: Context, d: Deck) = save(ctx, stored(ctx).map { if (it.id == d.id) d else it })

    fun delete(ctx: Context, id: String) {
        save(ctx, stored(ctx).filterNot { it.id == id })
        cardFile(ctx, id).delete()
    }

    private fun writeCards(ctx: Context, id: String, cards: List<DeckCard>) {
        val a = JSONArray()
        cards.forEach {
            a.put(
                JSONObject().put("id", it.id).put("f", it.front).put("r", it.reading).put("m", it.meaning)
                    .put("x", it.extra).put("ans", JSONArray(it.answers)).put("i", it.instructions)
            )
        }
        cardFile(ctx, id).writeText(a.toString())
    }


    fun displayName(ctx: Context, uri: Uri): String =
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }?.substringBeforeLast('.') ?: "Deck"

    fun importAnki(ctx: Context, uri: Uri): Deck {
        val res = ctx.contentResolver.openInputStream(uri)!!.use { input ->
            ApkgParser.parse(input, File(ctx.cacheDir, "anki")) { AndroidSql(it) }
        }
        require(res.cards.isNotEmpty()) { "No cards found" }
        val name = res.deckName.substringAfterLast("::").ifBlank { displayName(ctx, uri) }
        return create(ctx, name, DeckSource.ANKI, res.cards, ankiName = res.deckName)
    }


    fun importKotoba(ctx: Context, uri: Uri): Deck {
        val text = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
        val cards = KotobaCsv.parse(text)
        require(cards.isNotEmpty()) { "No cards found" }
        return create(ctx, displayName(ctx, uri), DeckSource.KOTOBA, cards)
    }
}

class AndroidSql(file: File) : SqlSource {
    private val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
    override fun rows(sql: String, args: List<String>): List<List<Any?>> = db.rawQuery(sql, args.toTypedArray()).use { c ->
        val out = ArrayList<List<Any?>>()
        while (c.moveToNext()) out.add((0 until c.columnCount).map { value(c, it) })
        out
    }
    private fun value(c: Cursor, i: Int): Any? = when (c.getType(i)) {
        Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
        Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
        Cursor.FIELD_TYPE_STRING -> c.getString(i)
        Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
        else -> null
    }
    override fun close() = db.close()
}

//kotoba csv, comma or tab both work
object KotobaCsv {
    fun parse(raw: String): List<DeckCard> {
        val text = raw.trimStart('﻿')
        val rows = Csv.parse(text, if (text.lineSequence().firstOrNull()?.count { it == '\t' } ?: 0 > 0) '\t' else ',')
        if (rows.isEmpty()) return emptyList()
        val header = rows[0].map { it.trim().lowercase() }
        val hasHeader = header.firstOrNull() == "question" || "answers" in header
        val qi = if (hasHeader) header.indexOf("question").coerceAtLeast(0) else 0
        val ai = if (hasHeader) header.indexOf("answers").takeIf { it >= 0 } ?: 1 else 1
        val ci = if (hasHeader) header.indexOf("comment").takeIf { it >= 0 } ?: 2 else 2
        val ii = if (hasHeader) header.indexOf("instructions").takeIf { it >= 0 } ?: 3 else 3
        return rows.drop(if (hasHeader) 1 else 0).mapIndexedNotNull { n, r ->
            val q = r.getOrElse(qi) { "" }.trim()
            if (q.isEmpty()) return@mapIndexedNotNull null
            val answers = r.getOrElse(ai) { "" }.replace('、', ',').split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val reading = answers.firstOrNull { Clean.isKanaOnly(it) } ?: ""
            DeckCard(
                id = "k$n",
                front = q,
                reading = reading.ifEmpty { answers.joinToString(", ") },
                meaning = r.getOrElse(ci) { "" }.replace("\\n", "\n").trim(),
                answers = answers,
                instructions = r.getOrElse(ii) { "" }.trim()
            )
        }
    }
}

object Csv {
    fun parse(text: String, delim: Char): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var q = false
        var i = 0
        fun endCell() { row.add(cell.toString()); cell.setLength(0) }
        fun endRow() { endCell(); if (row.any { it.isNotBlank() }) rows.add(row); row = ArrayList() }
        while (i < text.length) {
            val c = text[i]
            when {
                q && c == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' -> q = !q
                !q && c == delim -> endCell()
                !q && (c == '\n' || c == '\r') -> { if (c == '\r' && text.getOrNull(i + 1) == '\n') i++; endRow() }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}

//guesses which anki field is the reading and which is the meaning. not perfect
object FieldMapper {
    private val readingNames = listOf("reading", "kana", "furigana", "yomi", "よみ", "読み", "hiragana", "pronunciation")
    private val meaningNames = listOf("meaning", "english", "definition", "gloss", "translation", "意味", "back")

    fun map(id: String, names: List<String>, rawValues: List<String>): DeckCard? {
        val values = rawValues.map { Clean.html(it) }
        val front = Clean.stripFurigana(values.firstOrNull().orEmpty()).lines().firstOrNull().orEmpty().trim()
        if (front.isEmpty()) return null
        val rest = values.indices.drop(1)

        fun named(keys: List<String>) = rest.firstOrNull { i ->
            val n = names.getOrElse(i) { "" }.lowercase()
            keys.any { n.contains(it) } && values[i].isNotBlank()
        }

        var reading = named(readingNames)?.let { Clean.furiganaReading(values[it]).lines().first() } ?: ""
        var meaning = named(meaningNames.dropLast(1))?.let { values[it] } ?: ""

        val lines = rest.flatMap { values[it].lines() }
        if (reading.isEmpty()) reading = lines.firstOrNull { Clean.isKanaOnly(it) } ?: ""
        if (reading.isEmpty() && values.firstOrNull()?.contains('[') == true) {
            val r = Clean.furiganaReading(values[0]).lines().first()
            if (Clean.isKanaOnly(r)) reading = r
        }
        if (meaning.isEmpty()) meaning = lines.filter { it != reading && !Clean.isKanaOnly(it) }.take(2).joinToString("; ")
        if (meaning.isEmpty() && reading.isEmpty()) meaning = lines.joinToString("\n")

        val used = setOf(reading, meaning)
        val extra = lines.filter { it !in used && !meaning.contains(it) }.take(3).joinToString("\n")
        return DeckCard(
            id = id, front = front, reading = reading, meaning = meaning, extra = extra,
            answers = if (reading.isNotEmpty()) listOf(reading) else emptyList()
        )
    }
}


//so the parser works on android and in the jvm tests
interface SqlSource : AutoCloseable {
    fun rows(sql: String): List<List<Any?>> = rows(sql, emptyList())
    fun rows(sql: String, args: List<String>): List<List<Any?>>
}


//handles anki21b anki21 and the old anki2
object ApkgParser {
    data class Result(val deckName: String, val cards: List<DeckCard>)

    fun parse(input: InputStream, workDir: File, open: (File) -> SqlSource): Result {
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (e.name.startsWith("collection.anki")) entries[e.name] = zip.readBytes()
            }
        }
        val bytes = when {
            // newer anki zips it with zstd
            "collection.anki21b" in entries -> ZstdInputStream(ByteArrayInputStream(entries.getValue("collection.anki21b"))).use { it.readBytes() }
            "collection.anki21" in entries -> entries.getValue("collection.anki21")
            "collection.anki2" in entries -> entries.getValue("collection.anki2")
            else -> throw IllegalArgumentException("Not an Anki package")
        }
        workDir.mkdirs()
        val db = File(workDir, "import-${System.nanoTime()}.db")
        db.writeBytes(patchCollation(bytes))
        try {
            return open(db).use { read(it) }
        } finally {
            db.delete(); File(db.path + "-journal").delete(); File(db.path + "-wal").delete()
        }
    }

    private val UNICASE = "COLLATE unicase".toByteArray()
    private val NOCASE = "COLLATE nocase ".toByteArray()

    //anki uses a unicase collation normal sqlite dosent have so some tables wont open
    //swap it for nocase, same lenght so nothing in the file moves
    fun patchCollation(b: ByteArray): ByteArray {
        var i = 0
        val out = b.copyOf()
        outer@ while (i <= out.size - UNICASE.size) {
            for (j in UNICASE.indices) if (out[i + j] != UNICASE[j]) { i++; continue@outer }
            System.arraycopy(NOCASE, 0, out, i, NOCASE.size)
            i += UNICASE.size
        }
        return out
    }

    private fun read(db: SqlSource): Result {
        val tables = db.rows("select name from sqlite_master where type='table'").map { it[0].toString() }.toSet()

        val fieldNames = HashMap<Long, MutableList<String>>()
        val deckNames = HashMap<Long, String>()
        if ("fields" in tables) {
            db.rows("select ntid, ord, name from fields order by ntid, ord").forEach {
                fieldNames.getOrPut((it[0] as Number).toLong()) { ArrayList() }.add(it[2].toString())
            }
            db.rows("select id, name from decks").forEach {
                deckNames[(it[0] as Number).toLong()] = it[1].toString().replace("\u001f", "::")
            }
        } else {
            val col = db.rows("select models, decks from col").first()
            val models = JSONObject(col[0].toString())
            for (k in models.keys()) {
                val flds = models.getJSONObject(k).getJSONArray("flds")
                fieldNames[k.toLong()] = (0 until flds.length()).map { flds.getJSONObject(it).getString("name") }.toMutableList()
            }
            val decks = JSONObject(col[1].toString())
            for (k in decks.keys()) deckNames[k.toLong()] = decks.getJSONObject(k).getString("name")
        }

        val deckOfNote = HashMap<Long, Long>()
        db.rows("select nid, did from cards order by ord").forEach {
            deckOfNote.putIfAbsent((it[0] as Number).toLong(), (it[1] as Number).toLong())
        }

        val cards = ArrayList<DeckCard>()
        db.rows("select id, mid, flds from notes order by id").forEach { r ->
            val nid = (r[0] as Number).toLong()
            val values = r[2].toString().split('\u001f')
            FieldMapper.map("a$nid", fieldNames[(r[1] as Number).toLong()] ?: emptyList(), values)?.let { cards.add(it) }
        }

        val counts = deckOfNote.values.groupingBy { it }.eachCount()
        val main = counts.entries.filter { it.key != 1L || counts.size == 1 }.maxByOrNull { it.value }?.key
        val name = main?.let { deckNames[it] } ?: "Anki deck"
        return Result(name, cards)
    }
}

//one big json of everything, used for the backup file and drive
object Backup {
    private val FILES = listOf("accepted.json", "rejected.json", "mined.json", "srs.json", "activity.json", "revlog.json", "decks.json")

    fun snapshot(ctx: Context): JSONObject {
        val root = JSONObject().put("app", "KanjiLock").put("version", 4)
        for (name in FILES) readJson(File(ctx.filesDir, name))?.let { root.put(name, it) }
        val decks = JSONObject()
        ctx.filesDir.listFiles { f -> f.name.startsWith("deck_") && f.name.endsWith(".json") }?.sortedBy { it.name }?.forEach { f ->
            readJson(f)?.let { decks.put(f.name, it) }
        }
        root.put("deckFiles", decks)
        root.put("settings", JSONObject(Prefs.exportSettings(ctx).toSortedMap()))
        return root
    }

    fun hash(json: JSONObject): String {
        val copy = JSONObject(json.toString()).apply { remove("savedAt") }
        val bytes = MessageDigest.getInstance("SHA-256").digest(copy.toString().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun apply(ctx: Context, root: JSONObject) {
        require(root.optString("app") == "KanjiLock")
        for (name in FILES) {
            val v = root.opt(name) ?: continue
            File(ctx.filesDir, name).writeText(v.toString())
        }
        root.optJSONObject("deckFiles")?.let { d ->
            for (k in d.keys()) if (k.startsWith("deck_")) File(ctx.filesDir, k).writeText(d.get(k).toString())
        }
        root.optJSONObject("settings")?.let { s -> Prefs.importSettings(ctx, s.keys().asSequence().associateWith { s.opt(it) }) }
        AcceptedStore.rebuildCsv(ctx)
    }

    fun write(ctx: Context, uri: Uri): Boolean = try {
        ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(snapshot(ctx).toString(2).toByteArray()) }
        true
    } catch (_: Exception) { false }

    fun restore(ctx: Context, uri: Uri): Boolean = try {
        val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
        apply(ctx, JSONObject(text))
        true
    } catch (_: Exception) { false }

    private fun readJson(f: File): Any? {
        if (!f.exists()) return null
        val t = f.readText().trim()
        return try { when { t.startsWith("[") -> JSONArray(t); t.startsWith("{") -> JSONObject(t); else -> null } } catch (_: Exception) { null }
    }


    //both phones changed stuff so just union it, local settings win
    fun merge(local: JSONObject, remote: JSONObject): JSONObject {
        val out = JSONObject(local.toString())
        for (name in listOf("accepted.json", "rejected.json", "mined.json")) {
            out.put(name, unionBy(local.optJSONArray(name), remote.optJSONArray(name)) { "${it.optString("word")}|${it.optString("reading")}" })
        }
        out.put("decks.json", unionBy(local.optJSONArray("decks.json"), remote.optJSONArray("decks.json")) { it.optString("id") })

        val srs = JSONObject(local.optJSONObject("srs.json")?.toString() ?: "{}")
        remote.optJSONObject("srs.json")?.let { r ->
            for (k in r.keys()) {
                val rc = r.getJSONObject(k)
                val lc = srs.optJSONObject(k)
                if (lc == null || rc.optInt("r") > lc.optInt("r")) srs.put(k, rc)
            }
        }
        out.put("srs.json", srs)

        val act = JSONObject(local.optJSONObject("activity.json")?.toString() ?: "{}")
        remote.optJSONObject("activity.json")?.let { r ->
            for (k in r.keys()) {
                val a = day(act.opt(k)); val b = day(r.opt(k))
                act.put(k, JSONObject().put("a", maxOf(a.first, b.first)).put("r", maxOf(a.second, b.second)))
            }
        }
        out.put("activity.json", act)

        val log = unionBy(local.optJSONArray("revlog.json"), remote.optJSONArray("revlog.json")) { it.toString() }
        out.put("revlog.json", log)

        val files = JSONObject(local.optJSONObject("deckFiles")?.toString() ?: "{}")
        remote.optJSONObject("deckFiles")?.let { r -> for (k in r.keys()) if (!files.has(k)) files.put(k, r.get(k)) }
        out.put("deckFiles", files)
        return out
    }

    private fun day(v: Any?): Pair<Int, Int> = when (v) {
        is Number -> v.toInt() to 0
        is JSONObject -> v.optInt("a") to v.optInt("r")
        else -> 0 to 0
    }

    private fun unionBy(a: JSONArray?, b: JSONArray?, key: (JSONObject) -> String): JSONArray {
        val seen = LinkedHashMap<String, Any>()
        fun add(arr: JSONArray?) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                val v = arr.get(i)
                val k = if (v is JSONObject) key(v) else v.toString()
                if (k !in seen) seen[k] = v
            }
        }
        add(a); add(b)
        return JSONArray(seen.values)
    }
}
