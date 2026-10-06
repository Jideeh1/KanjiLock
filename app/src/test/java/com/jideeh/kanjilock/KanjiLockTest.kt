package com.jideeh.kanjilock

import java.io.File
import java.sql.DriverManager
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {
    private val now = 1_790_000_000_000L
    private val m = Scheduler.MINUTE
    private val d = Scheduler.DAY

    @Test fun newCardButtonsMatchAnkiDefaults() {
        val p = Scheduler.preview(SrsCard("k"), now)
        assertEquals(1 * m, p[Grade.AGAIN])
        assertEquals(5 * m + 30_000, p[Grade.HARD])
        assertEquals(10 * m, p[Grade.GOOD])
        assertEquals(4 * d, p[Grade.EASY])
        listOf("1m", "6m", "10m", "4d").zip(Grade.entries).forEach { (label, g) ->
            assertEquals(label, Scheduler.label(p[g]!!))
        }
    }

    @Test fun goodTwiceGraduatesToOneDay() {
        val a = Scheduler.answer(SrsCard("k"), Grade.GOOD, now)
        assertEquals(SrsCard.LEARNING, a.state)
        val b = Scheduler.answer(a, Grade.GOOD, now)
        assertEquals(SrsCard.REVIEW, b.state)
        assertEquals(1.0, b.interval, 0.0)
    }

    @Test fun reviewIntervalsGrowAndOrder() {
        val c = SrsCard("k", state = SrsCard.REVIEW, interval = 10.0, ease = 2.5)
        val hard = Scheduler.answer(c, Grade.HARD, now)
        val good = Scheduler.answer(c, Grade.GOOD, now)
        val easy = Scheduler.answer(c, Grade.EASY, now)
        assertEquals(12.0, hard.interval, 0.0)
        assertEquals(25.0, good.interval, 0.0)
        assertEquals(33.0, easy.interval, 0.0)
        assertEquals(2.35, hard.ease, 1e-9)
        assertEquals(2.65, easy.ease, 1e-9)
    }

    @Test fun lapseGoesToRelearningAndDropsEase() {
        val c = SrsCard("k", state = SrsCard.REVIEW, interval = 20.0, ease = 2.5)
        val a = Scheduler.answer(c, Grade.AGAIN, now)
        assertEquals(SrsCard.RELEARNING, a.state)
        assertEquals(1, a.lapses)
        assertEquals(2.3, a.ease, 1e-9)
        assertEquals(now + 10 * m, a.due)
        val back = Scheduler.answer(a, Grade.GOOD, now)
        assertEquals(SrsCard.REVIEW, back.state)
        assertEquals(1.0, back.interval, 0.0)
    }

    private fun day(base: Calendar, offset: Int) =
        ActivityLog.dayKey((base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) })

    @Test fun streakCountsBackFromTodayOrYesterday() {
        val today = Calendar.getInstance()
        val s1 = ActivityLog.streak(setOf(day(today, 0), day(today, -1), day(today, -2), day(today, -5)), today)
        assertEquals(3, s1.current); assertTrue(s1.todayDone)

        val s2 = ActivityLog.streak(setOf(day(today, -1), day(today, -2)), today)
        assertEquals(2, s2.current); assertFalse(s2.todayDone)

        val s3 = ActivityLog.streak(setOf(day(today, -2), day(today, -3)), today)
        assertEquals(0, s3.current); assertEquals(2, s3.longest)
    }
}

class JdbcSource(f: File) : SqlSource {
    private val c = DriverManager.getConnection("jdbc:sqlite:${f.absolutePath}")
    override fun rows(sql: String, args: List<String>): List<List<Any?>> = c.prepareStatement(sql).use { st ->
        args.forEachIndexed { i, a -> st.setString(i + 1, a) }
        val rs = st.executeQuery()
        val n = rs.metaData.columnCount
        val out = ArrayList<List<Any?>>()
        while (rs.next()) out.add((1..n).map { rs.getObject(it) })
        out
    }
    override fun close() = c.close()
}

class ImportAndRomajiTest {
    private fun res(name: String) = javaClass.classLoader!!.getResourceAsStream(name)!!
    private val tmp = File(System.getProperty("java.io.tmpdir"), "kl-test").apply { mkdirs() }

    @Test fun romajiBasics() {
        val cases = mapOf(
            "taberu" to "たべる", "gakkou" to "がっこう", "konnichiha" to "こんにちは", "konna" to "こんな",
            "shinbun" to "しんぶん", "kyou" to "きょう", "matcha" to "まっちゃ", "zasshi" to "ざっし",
            "tsukue" to "つくえ", "jisho" to "じしょ", "ryokou" to "りょこう", "kin'en" to "きんえん",
            "onna" to "おんな", "benkyou" to "べんきょう", "ra-men" to "らーめん", "fujisan" to "ふじさん",
            "やま" to "やま", "chotto" to "ちょっと", "tenki" to "てんき", "minna" to "みんな"
        )
        for ((r, k) in cases) assertEquals(r, k, Romaji.toHiragana(r, final = true))
        assertEquals("やまn", Romaji.toHiragana("yaman"))
        assertEquals("やまん", Romaji.toHiragana("yaman", final = true))
    }

    @Test fun romajiMatchesReadings() {
        assertTrue(Romaji.matches("taberu", listOf("たべる")))
        assertTrue(Romaji.matches("ヤマ", listOf("やま")))
        assertTrue(Romaji.matches("asu", listOf("あした", "あす")))
        assertFalse(Romaji.matches("tabera", listOf("たべる")))
    }

    @Test fun modernApkgAnki21b() = checkApkg("modern.apkg")
    @Test fun legacyApkgAnki21() = checkApkg("legacy.apkg")

    private fun checkApkg(name: String) {
        val r = ApkgParser.parse(res(name), tmp) { JdbcSource(it) }
        assertEquals("JLPT N5 Vocab", r.deckName)
        assertEquals(4, r.cards.size)
        val taberu = r.cards.first { it.front == "食べる" }
        assertEquals("たべる", taberu.reading)
        assertEquals("to eat", taberu.meaning)
        assertEquals(listOf("たべる"), taberu.answers)
        assertEquals("school & study", r.cards.first { it.front == "学校" }.meaning)
        val yakusoku = r.cards.first { it.front == "約束" }
        assertEquals("やくそく", yakusoku.reading)
        assertEquals("promise", yakusoku.meaning)
    }

    @Test fun kotobaCsv() {
        val cards = KotobaCsv.parse(res("kotoba.csv").readBytes().toString(Charsets.UTF_8))
        assertEquals(2, cards.size)
        assertEquals("明日", cards[0].front)
        assertEquals(listOf("あした", "あす"), cards[0].answers)
        assertEquals("Tomorrow", cards[0].meaning)
        assertEquals(listOf("たべる", "くう"), cards[1].answers)
        assertEquals("to eat, to consume", cards[1].meaning)
        assertEquals("Type the reading!", cards[1].instructions)
    }
}

class MergeTest {
    private fun w(word: String, reading: String) = JSONObject().put("word", word).put("reading", reading)

    @Test fun mergeKeepsEverythingFromBothPhones() {
        val local = JSONObject().put("app", "KanjiLock")
            .put("accepted.json", JSONArray().put(w("山", "やま")).put(w("水", "みず")))
            .put("srs.json", JSONObject().put("山|やま", JSONObject().put("r", 3).put("i", 4.0)))
            .put("activity.json", JSONObject().put("2026-10-05", JSONObject().put("a", 1).put("r", 2)).put("2026-10-04", 2))
            .put("decks.json", JSONArray().put(JSONObject().put("id", "d1")))
            .put("deckFiles", JSONObject().put("deck_d1.json", JSONArray()))
        val remote = JSONObject().put("app", "KanjiLock")
            .put("accepted.json", JSONArray().put(w("山", "やま")).put(w("川", "かわ")))
            .put("srs.json", JSONObject().put("山|やま", JSONObject().put("r", 5).put("i", 10.0)).put("川|かわ", JSONObject().put("r", 1)))
            .put("activity.json", JSONObject().put("2026-10-05", JSONObject().put("a", 0).put("r", 7)).put("2026-10-06", JSONObject().put("a", 1).put("r", 0)))
            .put("decks.json", JSONArray().put(JSONObject().put("id", "d2")))
            .put("deckFiles", JSONObject().put("deck_d2.json", JSONArray().put(JSONObject().put("id", "x"))))

        val m = Backup.merge(local, remote)
        val words = (0 until m.getJSONArray("accepted.json").length()).map { m.getJSONArray("accepted.json").getJSONObject(it).getString("word") }
        assertEquals(listOf("山", "水", "川"), words)
        assertEquals(5, m.getJSONObject("srs.json").getJSONObject("山|やま").getInt("r"))
        assertEquals(1, m.getJSONObject("srs.json").getJSONObject("川|かわ").getInt("r"))
        val act = m.getJSONObject("activity.json")
        assertEquals(1, act.getJSONObject("2026-10-05").getInt("a"))
        assertEquals(7, act.getJSONObject("2026-10-05").getInt("r"))
        assertEquals(2, act.getInt("2026-10-04"))
        assertEquals(1, act.getJSONObject("2026-10-06").getInt("a"))
        assertEquals(2, m.getJSONArray("decks.json").length())
        assertEquals(setOf("deck_d1.json", "deck_d2.json"), m.getJSONObject("deckFiles").keys().asSequence().toSet())
    }
}

class DictionaryTest {
    private val q = DictQueries(JdbcSource(unpacked()))

    companion object {
        //unzips the real db once into tmp
        fun unpacked(): File {
            val f = File(System.getProperty("java.io.tmpdir"), "kl-dict-test.db")
            val src = File("src/main/assets/dict.db.gz")
            if (!f.exists() || f.lastModified() < src.lastModified()) {
                java.util.zip.GZIPInputStream(src.inputStream()).use { i -> f.outputStream().use { i.copyTo(it) } }
            }
            return f
        }
    }

    @Test fun sizes() {
        val (w, k) = q.counts()
        assertTrue("words=$w", w > 200_000)
        assertTrue("kanji=$k", k > 2_100)
    }

    @Test fun searchByKanjiKanaRomajiEnglish() {
        assertEquals("食べる", q.search("食べる").first().word)
        assertEquals("食べる", q.search("たべる").first().word)
        assertEquals("食べる", q.search("taberu").first().word)
        assertTrue(q.search("eat").take(10).any { it.word == "食べる" })
        assertEquals("ありがとう", q.search("arigatou").first().word)
        val k = q.search("山").first()
        assertEquals("k山", k.id); assertEquals("やま", k.reading)
    }

    @Test fun wordEntryHasRealData() {
        val w = q.search("勉強").first { it.word == "勉強" }
        assertEquals("べんきょう", w.reading)
        assertTrue(w.meaning.startsWith("study"))
        assertTrue(w.example.contains("勉強"))
        assertTrue(w.exampleMeaning.isNotBlank())
        assertTrue(w.exampleId.all { it.isDigit() } && w.exampleId.isNotEmpty())
        assertEquals(w, q.word(w.id))
    }

    @Test fun kanjiAcceptsAllReadings() {
        val k = q.word("k食")!!
        assertTrue(k.altReadings.containsAll(listOf("しょく", "たべる", "た", "くう")))
        assertTrue(k.info.contains("On ショク"))
    }

    @Test fun randomPoolsReturnUsableIds() {
        val words = q.randomWordIds(20).mapNotNull { q.word(it) }
        assertEquals(20, words.size)
        assertTrue(words.all { it.reading.isNotBlank() && it.meaning.isNotBlank() })
        val kanji = q.randomKanjiIds(20).mapNotNull { q.word(it) }
        assertEquals(20, kanji.size)
        assertTrue(kanji.all { it.type == "kanji" && it.word.length == 1 })
    }
}

class UpdaterAndTypingTest {
    @Test fun versionCompare() {
        assertTrue(Updater.newer("v1.1.0", "1.0.0"))
        assertTrue(Updater.newer("1.10.0", "1.9.2"))
        assertTrue(Updater.newer("2", "1.9.9"))
        assertFalse(Updater.newer("v1.0.0", "1.0.0"))
        assertFalse(Updater.newer("1.0.0", "1.0.1"))
        assertFalse(Updater.newer("1.1.0-beta", "1.1.0"))
    }

    //same shape as api.github.com/repos/x/y/releases/latest
    @Test fun parsesGithubRelease() {
        val body = File("../docs/release-v1.0.0.md").readText()
        val json = org.json.JSONObject()
            .put("tag_name", "v1.0.0").put("draft", false).put("prerelease", false)
            .put("html_url", "https://github.com/Jideeh1/KanjiLock/releases/tag/v1.0.0")
            .put("body", body)
            .put("assets", org.json.JSONArray()
                .put(org.json.JSONObject().put("name", "notes.txt").put("size", 10).put("browser_download_url", "https://x/notes.txt"))
                .put(org.json.JSONObject().put("name", "KanjiLock-1.0.0.apk").put("size", 21040110)
                    .put("browser_download_url", "https://github.com/Jideeh1/KanjiLock/releases/download/v1.0.0/KanjiLock-1.0.0.apk")))
        val r = Updater.parse(json.toString())!!
        assertEquals("1.0.0", r.version)
        assertTrue(r.apkUrl!!.endsWith("KanjiLock-1.0.0.apk"))
        assertEquals(21040110L, r.apkSize)
        assertFalse(r.notes.contains("<")); assertFalse(r.notes.contains("![")); assertFalse(r.notes.contains("shields.io"))
        assertTrue(r.notes.contains("• home screen widget"))
        assertTrue(r.notes.contains("whats in it"))
        assertNull(Updater.parse(json.put("draft", true).toString()))
    }

    @Test fun buzzOnlyWhenItCantBeRight() {
        val a = listOf("たべる")
        assertTrue(Romaji.onTrack("ta", a))
        assertTrue(Romaji.onTrack("tab", a)) //ta plus a b thats still pending
        assertTrue(Romaji.onTrack("taber", a))
        assertTrue(Romaji.onTrack("たべ", a))
        assertFalse(Romaji.onTrack("to", a))
        assertFalse(Romaji.onTrack("tabu", a))
        assertTrue(Romaji.onTrack("TABE", listOf("タベル")))
        assertTrue(Romaji.onTrack("kan", listOf("かんじ")))
        assertTrue(Romaji.onTrack("matc", listOf("まっちゃ")))
        assertTrue(Romaji.onTrack("tt", listOf("ちょっと", "ってい")))
        assertTrue(Romaji.onTrack("to e", listOf("to eat")))
        assertFalse(Romaji.onTrack("to x", listOf("to eat")))
        assertTrue(Romaji.onTrack("", a))
    }
}

class JdbcSink(f: File) : SqlSink {
    private val c = DriverManager.getConnection("jdbc:sqlite:${f.absolutePath}")
    override fun exec(sql: String, args: List<Any?>) {
        c.prepareStatement(sql).use { st -> args.forEachIndexed { i, a -> st.setObject(i + 1, a) }; st.execute() }
    }
    override fun close() = c.close()
}

class V2Test {
    private val tmp = File(System.getProperty("java.io.tmpdir"), "kl-test2").apply { mkdirs() }

    @Test fun apkgRoundTrip() {
        val notes = listOf(
            ApkgWriter.Note("食べる|たべる", "食べる", "たべる", "to eat", "パンを食べる"),
            ApkgWriter.Note("学校|がっこう", "学校", "がっこう", "school & study", ""),
            ApkgWriter.Note("約束|やくそく", "約束", "やくそく", "promise <vow>", "")
        )
        val out = java.io.ByteArrayOutputStream()
        ApkgWriter.write("KanjiLock", notes, out, tmp) { JdbcSink(it) }
        val names = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(out.toByteArray())).use { z ->
            generateSequence { z.nextEntry }.map { it.name }.toList()
        }
        assertEquals(listOf("collection.anki2", "media"), names)
        File(tmp, "out.apkg").writeBytes(out.toByteArray()) //copy to open in real anki if u wanna check by hand

        val r = ApkgParser.parse(java.io.ByteArrayInputStream(out.toByteArray()), tmp) { JdbcSource(it) }
        assertEquals("KanjiLock", r.deckName)
        assertEquals(3, r.cards.size)
        val t = r.cards.first { it.front == "食べる" }
        assertEquals("たべる", t.reading)
        assertEquals("to eat", t.meaning)
        assertEquals("school & study", r.cards.first { it.front == "学校" }.meaning)
        assertEquals("promise <vow>", r.cards.first { it.front == "約束" }.meaning)
    }

    @Test fun csumMatchesAnki() {
        //same as anki, first 8 hex of the sha1 as a number
        val hex = java.security.MessageDigest.getInstance("SHA-1").digest("食べる".toByteArray()).joinToString("") { "%02x".format(it) }
        assertEquals(hex.take(8).toLong(16), ApkgWriter.csum("食べる"))
        assertEquals(ApkgWriter.guid("a"), ApkgWriter.guid("a"))
    }

    @Test fun fourChoicesOneRight() {
        val pool = (1..6).map { DeckCard("c$it", "字$it", "じ", "meaning $it") } + DeckCard("dup", "字7", "じ", "Meaning 1")
        repeat(20) { seed ->
            val card = pool[seed % 6]
            val c = Study.choices(pool, card, reverse = false, rnd = kotlin.random.Random(seed))
            assertEquals(4, c.size)
            assertEquals(1, c.count { it == card.meaning })
            assertEquals(4, c.map { it.lowercase() }.distinct().size)
            val r = Study.choices(pool, card, reverse = true, rnd = kotlin.random.Random(seed))
            assertEquals(1, r.count { it == card.front })
        }
        //same seed same order so going back looks the same
        assertEquals(Study.choices(pool, pool[0], false, kotlin.random.Random(5)), Study.choices(pool, pool[0], false, kotlin.random.Random(5)))
    }

    @Test fun achievementsOnlyOnce() {
        val p = Achievements.Progress(streak = 8, kept = 10, reviews = 99, mature = 0,
            game = mapOf("reading" to GameStats.Mode(right = 60, best = 12), "listen" to GameStats.Mode(right = 3, best = 5)))
        val first = Achievements.due(emptySet(), p).map { it.id }
        assertEquals(listOf("streak_3", "streak_7", "kept_10", "reading_50", "reading_combo_10", "listen_combo_5"), first)
        assertTrue(Achievements.due(first.toSet(), p).isEmpty())
        assertEquals(Achievements.all.size, Achievements.all.map { it.id }.toSet().size)
        //every kind has a name for each tier
        Achievements.all.groupBy { it.kind }.forEach { (_, l) -> assertEquals(l.indices.toList(), l.map { it.tier }) }
    }

    @Test fun practiceHasTheFourGames() {
        assertEquals(listOf("reading", "meaning", "reverse", "listen"), GameModes)
    }
}
