package com.jideeh.kanjilock

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.jideeh.kanjilock.DailyWordManager.Status
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

//check adds to accepted, word stays and button goes gray
//x puts it in rejected then shows the next one
//reload just gives another word
object DailyWordManager {

    enum class Status { ACTIVE, DISMISSED, EMPTY }

    data class Today(
        val status: Status,
        val word: Word?,
        val index: Int,
        val total: Int,
        val accepted: Boolean,
        val rejected: Boolean = false
    ) {
        //no point rejecting if its already in a list
        val canReject: Boolean get() = !accepted && !rejected
    }

    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fun today(): String = dayFmt.format(Date())

    fun ensureToday(ctx: Context, force: Boolean = false) {
        val today = today()
        val sameDay = Prefs.day(ctx) == today && (Prefs.todayIds(ctx).isNotEmpty() || Prefs.dismissed(ctx))
        if (!force && sameDay) return
        val ids = pickIds(ctx, Prefs.wordsPerDay(ctx), emptySet())
        Prefs.saveDailyState(ctx, today, ids, 0, false)
    }
    fun reselectToday(ctx: Context) {
        val ids = pickIds(ctx, Prefs.wordsPerDay(ctx), emptySet())
        Prefs.saveDailyState(ctx, today(), ids, 0, false)
        refresh(ctx)
    }

    fun state(ctx: Context): Today {
        ensureToday(ctx)
        val ids = Prefs.todayIds(ctx)
        if (Prefs.dismissed(ctx)) return Today(Status.DISMISSED, null, 0, 0, false)
        if (ids.isEmpty()) return Today(Status.EMPTY, null, 0, 0, false)
        var words = ids.mapNotNull { id -> Dictionary.byId(ctx, id)?.let { id to it } }
        if (words.isEmpty()) {
            //ids from an old version or a deleted mined word, just pick new ones
            val fresh = pickIds(ctx, Prefs.wordsPerDay(ctx), emptySet())
            Prefs.saveDailyState(ctx, today(), fresh, 0, false)
            words = fresh.mapNotNull { id -> Dictionary.byId(ctx, id)?.let { id to it } }
            if (words.isEmpty()) return Today(Status.EMPTY, null, 0, 0, false)
        }
        val idx = Prefs.index(ctx).coerceIn(0, words.size - 1)
        val w = words[idx].second
        val valid = words
        return Today(Status.ACTIVE, w, idx, valid.size, AcceptedStore.contains(ctx, w), w.key in RejectedStore.keys(ctx))
    }

    fun check(ctx: Context) {
        val s = state(ctx)
        val w = s.word ?: return
        if (s.accepted) return
        AcceptedStore.add(ctx, w)
        RejectedStore.remove(ctx, w.key)
        refresh(ctx)
    }

    fun reject(ctx: Context) {
        val s = state(ctx)
        val w = s.word ?: return
        if (!s.canReject) return
        RejectedStore.add(ctx, w)
        val ids = Prefs.todayIds(ctx).toMutableList()
        ids.removeAt(s.index.coerceIn(0, ids.size - 1))
        val dismissed = ids.isEmpty()
        val newIndex = if (dismissed) 0 else s.index.coerceAtMost(ids.size - 1)
        Prefs.saveDailyState(ctx, Prefs.day(ctx), ids, newIndex, dismissed)
        refresh(ctx)
    }

    fun reload(ctx: Context) {
        val s = state(ctx)
        if (s.status != Status.ACTIVE) return
        val ids = Prefs.todayIds(ctx).toMutableList()
        val i = s.index
        if (i !in ids.indices) return
        val replacement = pickIds(ctx, 1, ids.toSet()).firstOrNull() ?: return
        ids[i] = replacement
        Prefs.saveDailyState(ctx, Prefs.day(ctx), ids, i, false)
        refresh(ctx)
    }

    // for the snackbar undo
    fun undoReject(ctx: Context, w: Word, index: Int) {
        RejectedStore.remove(ctx, w.key)
        val ids = Prefs.todayIds(ctx).toMutableList()
        if (w.id !in ids) ids.add(index.coerceIn(0, ids.size), w.id)
        Prefs.saveDailyState(ctx, today(), ids, ids.indexOf(w.id), false)
        refresh(ctx)
    }

    //change words per day without reshuffling whats already there
    fun resizeToday(ctx: Context) {
        ensureToday(ctx)
        if (Prefs.dismissed(ctx)) { refresh(ctx); return }
        val want = Prefs.wordsPerDay(ctx)
        val ids = Prefs.todayIds(ctx).toMutableList()
        if (ids.size > want) {
            while (ids.size > want) ids.removeAt(ids.size - 1)
        } else if (ids.size < want) {
            ids += pickIds(ctx, want - ids.size, ids.toSet())
        }
        Prefs.saveDailyState(ctx, today(), ids, Prefs.index(ctx).coerceIn(0, (ids.size - 1).coerceAtLeast(0)), false)
        refresh(ctx)
    }

    fun step(ctx: Context, delta: Int) {
        val s = state(ctx)
        if (s.status != Status.ACTIVE || s.total < 2) return
        val next = ((s.index + delta) % s.total + s.total) % s.total
        Prefs.saveDailyState(ctx, Prefs.day(ctx), Prefs.todayIds(ctx), next, false)
        refresh(ctx)
    }

    //update widget notif and the app all at once
    fun refresh(ctx: Context) {
        KanjiWidgetProvider.updateAll(ctx)
        Widgets.updateAll(ctx)
        LockScreenNotifier.refresh(ctx)
        _changes.value = _changes.value + 1
    }

    fun refreshOnMain(ctx: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).post { refresh(ctx) }
    }

    private fun pickIds(ctx: Context, count: Int, exclude: Set<String>): List<String> {
        val seen = AcceptedStore.keys(ctx) + RejectedStore.keys(ctx)
        val used = exclude.toMutableSet()
        val out = ArrayList<String>()
        repeat(count) {
            val pick = Dictionary.pick(ctx, used, seen) ?: Dictionary.pick(ctx, used, emptySet())
            if (pick != null) { out.add(pick); used.add(pick) }
        }
        return out
    }
}

//review queue for local decks
object Study {
    private const val LEARN_AHEAD = 20 * Scheduler.MINUTE

    data class Queue(
        val card: DeckCard?,
        val srs: SrsCard?,
        val newLeft: Int,
        val learning: Int,
        val review: Int,
        val nextDue: Long?,
        val deckSize: Int
    ) {
        val remaining: Int get() = newLeft + learning + review
    }

    fun queue(ctx: Context, deckId: String = DeckStore.ACCEPTED, now: Long = System.currentTimeMillis()): Queue {
        val cards = DeckStore.cards(ctx, deckId)
        val srs = SrsStore.all(ctx)
        val endOfToday = Scheduler.startOfDay(now) + Scheduler.DAY

        val learning = ArrayList<Pair<DeckCard, SrsCard>>()
        val review = ArrayList<Pair<DeckCard, SrsCard>>()
        val fresh = ArrayList<Pair<DeckCard, SrsCard>>()
        var nextDue: Long? = null

        for (card in cards) {
            val key = DeckStore.srsKey(deckId, card.id)
            val c = srs[key] ?: SrsCard(key)
            when (c.state) {
                SrsCard.NEW -> fresh += card to c
                SrsCard.LEARNING, SrsCard.RELEARNING ->
                    if (c.due <= now + LEARN_AHEAD) learning += card to c else nextDue = minOf(nextDue ?: c.due, c.due)
                else -> if (c.due < endOfToday) review += card to c else nextDue = minOf(nextDue ?: c.due, c.due)
            }
        }
        learning.sortBy { it.second.due }
        review.sortBy { it.second.due }

        val newAllowed = (Prefs.newPerDay(ctx) - Prefs.newSeenToday(ctx, ActivityLog.dayKey(now), deckId)).coerceAtLeast(0)
        val newLeft = minOf(newAllowed, fresh.size)
        val pick = learning.firstOrNull { it.second.due <= now }
            ?: review.firstOrNull()
            ?: fresh.take(newLeft).firstOrNull()
            ?: learning.firstOrNull()

        return Queue(pick?.first, pick?.second, newLeft, learning.size, review.size, nextDue, cards.size)
    }

    //what we need to take an answer back
    data class Undo(val deckId: String, val card: DeckCard, val before: SrsCard, val day: String)

    fun answer(ctx: Context, deckId: String, card: SrsCard, grade: Grade, now: Long = System.currentTimeMillis()) {
        if (card.state == SrsCard.NEW) Prefs.markNewSeen(ctx, ActivityLog.dayKey(now), deckId)
        SrsStore.put(ctx, Scheduler.answer(card, grade, now))
        logReview(ctx, grade.ordinal + 1, card.state, deckId)
    }

    fun undo(ctx: Context, u: Undo) {
        SrsStore.put(ctx, u.before)
        if (u.before.state == SrsCard.NEW) Prefs.unmarkNewSeen(ctx, u.day, u.deckId)
        RevLog.removeLast(ctx, u.deckId)
        ActivityLog.unrecordReview(ctx, u.day)
        DailyWordManager.refresh(ctx)
    }

    //practice only needs cards u can type, order is random everytime
    fun practiceCards(ctx: Context, deck: Deck): List<DeckCard> {
        val cards = if (deck.isLinked) AnkiDroid.notes(ctx, deck.ankiDeckName) else DeckStore.cards(ctx, deck.id)
        return cards.filter { it.answers.isNotEmpty() }.shuffled()
    }

    fun logReview(ctx: Context, grade: Int, stateBefore: Int, deckId: String) {
        RevLog.add(ctx, grade, stateBefore, deckId)
        ActivityLog.recordReview(ctx)
        DailyWordManager.refresh(ctx)
    }

    //ankidroid decks not counted here, ankidroid does that
    fun dueNow(ctx: Context): Int = DeckStore.all(ctx)
        .filter { it.source != DeckSource.ANKIDROID }
        .sumOf { queue(ctx, it.id).remaining }

    data class Insights(
        val reviewsToday: Int,
        val retention: Int?,
        val newCount: Int,
        val learning: Int,
        val young: Int,
        val mature: Int,
        val forecast: List<Int>,
        val leeches: List<String>
    )

    fun insights(ctx: Context, deckId: String, now: Long = System.currentTimeMillis()): Insights {
        val cards = DeckStore.cards(ctx, deckId)
        val srs = SrsStore.all(ctx)
        var n = 0; var l = 0; var y = 0; var m = 0
        val forecast = IntArray(7)
        val start = Scheduler.startOfDay(now)
        val leeches = ArrayList<String>()
        for (card in cards) {
            val c = srs[DeckStore.srsKey(deckId, card.id)] ?: SrsCard("")
            when (c.state) {
                SrsCard.NEW -> n++
                SrsCard.LEARNING, SrsCard.RELEARNING -> l++
                else -> if (c.interval >= 21) m++ else y++
            }
            if (c.state != SrsCard.NEW) {
                val d = ((c.due - start) / Scheduler.DAY).toInt().coerceAtLeast(0)
                if (d < 7) forecast[d]++
            }
            if (c.lapses >= 6) leeches += card.front
        }
        val log = RevLog.all(ctx).filter { it.deck == deckId }
        val todayStart = start
        val month = log.filter { it.t >= now - 30 * Scheduler.DAY && it.stateBefore == SrsCard.REVIEW }
        val retention = if (month.size >= 5) month.count { it.grade > 1 } * 100 / month.size else null
        return Insights(log.count { it.t >= todayStart }, retention, n, l, y, m, forecast.toList(), leeches)
    }
}

//runs around midnight to change the word
//inexact alarm so no exact alarm permision needed
class DailyResetReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                schedule(ctx)
                Reminders.scheduleAll(ctx)
                DailyWordManager.refresh(ctx)
            }
            ACTION_DAILY_RESET -> {
                DailyWordManager.ensureToday(ctx, force = true)
                DailyWordManager.refresh(ctx)
                schedule(ctx)
            }
        }
    }

    companion object {
        const val ACTION_DAILY_RESET = "com.jideeh.kanjilock.DAILY_RESET"

        fun schedule(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val next = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, next, AlarmManager.INTERVAL_DAY, pending(ctx)
            )
        }

        private fun pending(ctx: Context): PendingIntent {
            val i = Intent(ctx, DailyResetReceiver::class.java).setAction(ACTION_DAILY_RESET)
            return PendingIntent.getBroadcast(
                ctx, 31, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

//study one only if cards are due, streak one only if u did nothing today
object Reminders {
    private const val CHANNEL = "reminders"
    const val ACTION_STUDY = "com.jideeh.kanjilock.REMIND_STUDY"
    const val ACTION_STREAK = "com.jideeh.kanjilock.REMIND_STREAK"

    fun scheduleAll(ctx: Context) {
        schedule(ctx, ACTION_STUDY, Prefs.remindDaily(ctx), Prefs.remindMinutes(ctx), 41)
        schedule(ctx, ACTION_STREAK, Prefs.remindStreak(ctx), Prefs.streakMinutes(ctx), 42)
    }

    private fun schedule(ctx: Context, action: String, on: Boolean, minutes: Int, code: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, code, Intent(ctx, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        if (!on) return
        val at = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 30_000) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        am.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, pi) //10 min window is fine for a reminder
    }

    fun ensureChannel(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, ctx.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun show(ctx: Context, id: Int, title: String, body: String) {
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 43, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_kanji)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) { }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val active = ActivityLog.active(ctx)
        val streak = ActivityLog.streak(active)
        when (intent.action) {
            Reminders.ACTION_STUDY -> {
                val due = Study.dueNow(ctx)
                if (due > 0) {
                    Reminders.show(
                        ctx, 2001, ctx.getString(R.string.remind_study_title, due),
                        if (streak.current > 0) ctx.getString(R.string.remind_study_body_streak, streak.current)
                        else ctx.getString(R.string.remind_study_body)
                    )
                }
            }
            Reminders.ACTION_STREAK -> {
                if (!streak.todayDone && streak.current > 0) {
                    Reminders.show(
                        ctx, 2002, ctx.getString(R.string.remind_streak_title, streak.current),
                        ctx.getString(R.string.remind_streak_body)
                    )
                }
            }
        }
        Reminders.scheduleAll(ctx)
    }
}

object LockScreenNotifier {
    private const val CHANNEL_ID = "kanji_lockscreen"
    private const val NOTIF_ID = 1001

    fun ensureChannel(ctx: Context) {
        val ch = NotificationChannel(
            CHANNEL_ID, ctx.getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = ctx.getString(R.string.channel_desc)
            setShowBadge(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun refresh(ctx: Context) {
        ensureChannel(ctx)
        val nm = NotificationManagerCompat.from(ctx)
        if (!Prefs.lockNotif(ctx)) { nm.cancel(NOTIF_ID); return }

        val status = DailyWordManager.state(ctx).status
        if (status != Status.ACTIVE) { nm.cancel(NOTIF_ID); return }

        val collapsed = RemoteViews(ctx.packageName, R.layout.notification_kanji_collapsed)
        CardBinder.bind(ctx, collapsed, R.layout.notification_kanji_collapsed, 300)

        val expanded = RemoteViews(ctx.packageName, R.layout.notification_kanji)
        CardBinder.bind(ctx, expanded, R.layout.notification_kanji, 400)
        val nudgePx = (Prefs.notifNudge(ctx) * ctx.resources.displayMetrics.density).toInt()
        expanded.setViewPadding(R.id.btn_row, 0, 0, nudgePx, 0) //see DEFAULT_NUDGE_DP

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_kanji)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(ctx))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            nm.notify(NOTIF_ID, notif)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)

    private fun openApp(ctx: Context): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            ctx, 2, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

// same widget works on the lockscreen if the phone lets u, good lock on samsung or android 16 qpr2
class KanjiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        DailyWordManager.ensureToday(ctx)
        for (id in ids) mgr.updateAppWidget(id, build(ctx, mgr, id))
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
        mgr.updateAppWidget(id, build(ctx, mgr, id))
    }

    override fun onEnabled(ctx: Context) {
        DailyWordManager.ensureToday(ctx)
    }

    companion object {
        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, KanjiWidgetProvider::class.java))
            for (id in ids) mgr.updateAppWidget(id, build(ctx, mgr, id))
        }

        fun requestPin(ctx: Context): Boolean {
            val mgr = AppWidgetManager.getInstance(ctx)
            if (!mgr.isRequestPinAppWidgetSupported) return false
            return mgr.requestPinAppWidget(ComponentName(ctx, KanjiWidgetProvider::class.java), null, null)
        }

        fun hasWidgets(ctx: Context): Boolean =
            AppWidgetManager.getInstance(ctx)
                .getAppWidgetIds(ComponentName(ctx, KanjiWidgetProvider::class.java)).isNotEmpty()

        private fun build(ctx: Context, mgr: AppWidgetManager, id: Int): RemoteViews {
            val wide = layout(ctx, R.layout.widget_kanji, 100)
            val small = layout(ctx, R.layout.widget_kanji_small, 200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return RemoteViews(
                    mapOf(
                        SizeF(110f, 110f) to small,
                        SizeF(240f, 110f) to wide
                    )
                )
            }
            val opts = mgr.getAppWidgetOptions(id)
            val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            return if (minW < 220) small else wide
        }

        private fun layout(ctx: Context, layoutId: Int, requestBase: Int): RemoteViews {
            val rv = RemoteViews(ctx.packageName, layoutId)
            CardBinder.bind(ctx, rv, layoutId, requestBase)
            val bg = Widgets.bgRes(ctx)
            rv.setInt(R.id.widget_root, "setBackgroundResource", bg)
            if (Prefs.widgetBg(ctx) != Prefs.BG_SOLID) {
                rv.setTextColor(R.id.tv_reading, 0xFFFFFFFF.toInt())
                rv.setTextColor(R.id.tv_status, 0xFFFFFFFF.toInt())
                if (layoutId == R.layout.widget_kanji) rv.setTextColor(R.id.tv_example, 0xF2FFFFFF.toInt())
            }
            rv.setOnClickPendingIntent(R.id.widget_root, openApp(ctx))
            return rv
        }

        private fun openApp(ctx: Context): PendingIntent {
            val i = Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                ctx, 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

//all the layouts use the same ids so one function binds all of them
object CardBinder {

    fun bind(
        ctx: Context,
        rv: RemoteViews,
        layoutId: Int,
        requestBase: Int,
        showExample: Boolean = true
    ) {
        val t = DailyWordManager.state(ctx)
        val has = optionalIds[layoutId] ?: emptySet()

        rv.setOnClickPendingIntent(R.id.btn_x, ActionReceiver.pending(ctx, ActionReceiver.ACTION_X, requestBase + 1))
        rv.setOnClickPendingIntent(R.id.btn_reload, ActionReceiver.pending(ctx, ActionReceiver.ACTION_RELOAD, requestBase + 2))
        rv.setOnClickPendingIntent(R.id.btn_check, ActionReceiver.pending(ctx, ActionReceiver.ACTION_CHECK, requestBase + 3))
        if (R.id.tv_progress in has) {
            rv.setOnClickPendingIntent(R.id.tv_progress, ActionReceiver.pending(ctx, ActionReceiver.ACTION_NEXT, requestBase + 4))
            rv.setOnClickPendingIntent(R.id.tv_prev, ActionReceiver.pending(ctx, ActionReceiver.ACTION_PREV, requestBase + 5))
        }

        val w = t.word
        if (t.status == Status.ACTIVE && w != null) {
            rv.setViewVisibility(R.id.content_group, View.VISIBLE)
            rv.setViewVisibility(R.id.tv_status, View.GONE)
            rv.setViewVisibility(R.id.btn_row, View.VISIBLE)

            rv.setTextViewText(R.id.tv_kanji, w.word)
            rv.setFloat(R.id.tv_kanji, "setTextSize", kanjiSize(layoutId, w.word.length))
            rv.setTextViewText(R.id.tv_reading, w.reading)
            rv.setTextViewText(R.id.tv_meaning, w.meaning)

            if (R.id.tv_example in has) {
                val ex = w.exampleLine()
                val show = showExample && ex.isNotBlank()
                rv.setViewVisibility(R.id.tv_example, if (show) View.VISIBLE else View.GONE)
                rv.setTextViewText(R.id.tv_example, ex)
            }
            if (R.id.tv_progress in has) {
                //widgets cant do swipes so its ‹ and › chips
                val many = if (t.total > 1) View.VISIBLE else View.GONE
                rv.setViewVisibility(R.id.tv_progress, many)
                rv.setViewVisibility(R.id.tv_prev, many)
                rv.setTextViewText(R.id.tv_progress, "${t.index + 1}/${t.total}  ›")
            }

            setButtonEnabled(rv, R.id.btn_x, t.canReject, if (t.canReject) R.drawable.btn_x else R.drawable.btn_check_done)
            setButtonEnabled(rv, R.id.btn_reload, true, R.drawable.btn_reload)
            setButtonEnabled(
                rv, R.id.btn_check, !t.accepted,
                if (t.accepted) R.drawable.btn_check_done else R.drawable.btn_check
            )
            rv.setContentDescription(
                R.id.btn_check,
                ctx.getString(if (t.accepted) R.string.cd_check_done else R.string.cd_check)
            )
        } else {
            rv.setViewVisibility(R.id.content_group, View.GONE)
            rv.setViewVisibility(R.id.btn_row, View.GONE)
            rv.setViewVisibility(R.id.tv_status, View.VISIBLE)
            if (R.id.tv_progress in has) { rv.setViewVisibility(R.id.tv_progress, View.GONE); rv.setViewVisibility(R.id.tv_prev, View.GONE) }
            rv.setTextViewText(
                R.id.tv_status,
                ctx.getString(if (t.status == Status.DISMISSED) R.string.status_dismissed else R.string.status_empty)
            )
        }
    }

    private fun setButtonEnabled(rv: RemoteViews, id: Int, enabled: Boolean, bg: Int) {
        rv.setInt(id, "setBackgroundResource", bg)
        rv.setInt(id, "setImageAlpha", if (enabled) 255 else 120)
        rv.setBoolean(id, "setEnabled", enabled)
    }

    private fun kanjiSize(layoutId: Int, len: Int): Float {
        val base = when (layoutId) {
            R.layout.widget_kanji -> 52f
            R.layout.widget_kanji_small -> 40f
            R.layout.notification_kanji -> 44f
            else -> 26f
        }
        return when {
            len <= 1 -> base
            len == 2 -> base * 0.82f
            len == 3 -> base * 0.66f
            else -> base * 0.54f
        }
    }

    private val optionalIds: Map<Int, Set<Int>> = mapOf(
        R.layout.widget_kanji to setOf(R.id.tv_example, R.id.tv_progress),
        R.layout.widget_kanji_small to setOf(R.id.tv_progress),
        R.layout.notification_kanji to setOf(R.id.tv_example, R.id.tv_progress),
        R.layout.notification_kanji_collapsed to emptySet()
    )
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_X -> DailyWordManager.reject(context)
            ACTION_RELOAD -> DailyWordManager.reload(context)
            ACTION_CHECK -> DailyWordManager.check(context)
            ACTION_NEXT -> DailyWordManager.step(context, +1)
            ACTION_PREV -> DailyWordManager.step(context, -1)
        }
    }

    companion object {
        const val ACTION_X = "com.jideeh.kanjilock.action.X"
        const val ACTION_RELOAD = "com.jideeh.kanjilock.action.RELOAD"
        const val ACTION_CHECK = "com.jideeh.kanjilock.action.CHECK"
        const val ACTION_NEXT = "com.jideeh.kanjilock.action.NEXT"
        const val ACTION_PREV = "com.jideeh.kanjilock.action.PREV"

        fun pending(ctx: Context, action: String, requestCode: Int): PendingIntent {
            val i = Intent(ctx, ActionReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                ctx, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

//talks to ankidroid thru its content provider
//ankiweb has no api so this is the only way, ankidroid syncs it after
object AnkiDroid {
    const val PACKAGE = "com.ichi2.anki"
    const val PERMISSION = "com.ichi2.anki.permission.READ_WRITE_DATABASE"
    private val AUTHORITY = Uri.parse("content://com.ichi2.anki.flashcards")
    private val DECKS = Uri.withAppendedPath(AUTHORITY, "decks")
    private val SCHEDULE = Uri.withAppendedPath(AUTHORITY, "schedule")
    private val NOTES = Uri.withAppendedPath(AUTHORITY, "notes")

    data class AdDeck(val id: Long, val name: String, val learn: Int, val review: Int, val new: Int)
    data class AdCard(val noteId: Long, val ord: Int, val card: DeckCard, val buttons: Int, val times: List<String>)

    fun installed(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo(PACKAGE, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun hasPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun decks(ctx: Context): List<AdDeck> =
        ctx.contentResolver.query(DECKS, arrayOf("deck_id", "deck_name", "deck_count"), null, null, null)?.use { c ->
            val out = ArrayList<AdDeck>()
            while (c.moveToNext()) {
                val counts = runCatching { JSONArray(c.getString(2)) }.getOrNull()
                out += AdDeck(
                    c.getLong(0), c.getString(1),
                    counts?.optInt(0) ?: 0, counts?.optInt(1) ?: 0, counts?.optInt(2) ?: 0
                )
            }
            out.sortedBy { it.name.lowercase() }
        } ?: emptyList()

    fun deck(ctx: Context, id: Long): AdDeck? = decks(ctx).firstOrNull { it.id == id }

    //cards of a deck for the words list and practice, read only
    fun notes(ctx: Context, deckName: String, limit: Int = 3000): List<DeckCard> {
        val q = "deck:\"" + deckName.replace("\"", "\\\"") + "\""
        return ctx.contentResolver.query(NOTES, arrayOf("_id", "flds"), q, null, null)?.use { c ->
            val out = ArrayList<DeckCard>()
            while (c.moveToNext() && out.size < limit) {
                val id = c.getLong(0)
                FieldMapper.map("n$id", emptyList(), c.getString(1).split('\u001f'))?.let { out += it }
            }
            out
        } ?: emptyList()
    }

    //asks ankidroid to sync with ankiweb, same intent tasker uses
    //ankidroid only allows this every few minutes and needs u logged in there
    fun requestSync(ctx: Context): Boolean = runCatching {
        ctx.startActivity(
            Intent("com.ichi2.anki.DO_SYNC").setPackage(PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess

    fun open(ctx: Context): Boolean {
        val i = ctx.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return false
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true
    }

    fun findByName(ctx: Context, name: String): AdDeck? =
        decks(ctx).firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: decks(ctx).firstOrNull { it.name.substringAfterLast("::").equals(name.substringAfterLast("::"), ignoreCase = true) }

    fun next(ctx: Context, deckId: Long): AdCard? {
        val (nid, ord, buttons, times) = ctx.contentResolver.query(
            SCHEDULE, null, "limit=?, deckID=?", arrayOf("1", deckId.toString()), null
        )?.use { c ->
            if (!c.moveToFirst()) return null
            val times = runCatching {
                val a = JSONArray(c.getString(c.getColumnIndexOrThrow("next_review_times")))
                (0 until a.length()).map { a.getString(it) }
            }.getOrDefault(emptyList())
            Quad(
                c.getLong(c.getColumnIndexOrThrow("note_id")),
                c.getInt(c.getColumnIndexOrThrow("ord")),
                c.getInt(c.getColumnIndexOrThrow("button_count")),
                times
            )
        } ?: return null

        val noteUri = Uri.withAppendedPath(NOTES, nid.toString())
        val fields = ctx.contentResolver.query(noteUri, arrayOf("flds"), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0).split('\u001f') else emptyList()
        } ?: emptyList()
        val qa = ctx.contentResolver.query(
            Uri.withAppendedPath(noteUri, "cards/$ord"), arrayOf("question_simple", "answer_pure"), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) to c.getString(1) else null }

        val mapped = FieldMapper.map("n$nid", emptyList(), fields)
        val card = when {
            ord == 0 && mapped != null -> mapped
            qa != null -> DeckCard("n$nid-$ord", Clean.html(qa.first).lines().firstOrNull().orEmpty(), meaning = Clean.html(qa.second))
            mapped != null -> mapped
            else -> DeckCard("n$nid-$ord", "?")
        }
        return AdCard(nid, ord, card, buttons, times)
    }

    //ease 1 again 2 hard 3 good 4 easy
    fun answer(ctx: Context, c: AdCard, ease: Int, timeTakenMs: Long): Boolean {
        val v = ContentValues().apply {
            put("note_id", c.noteId); put("ord", c.ord)
            put("answer_ease", ease.coerceIn(1, c.buttons.coerceAtLeast(2)))
            put("time_taken", timeTakenMs)
        }
        return ctx.contentResolver.update(SCHEDULE, v, null, null) > 0
    }

    private data class Quad(val a: Long, val b: Int, val c: Int, val d: List<String>)
}


//one file in the users drive, pull on open and push on close
object DriveSync {
    private const val SCOPE = "https://www.googleapis.com/auth/drive.file"
    private const val FOLDER = "KanjiLock"
    private const val FILE = "kanjilock-data.json"
    private const val API = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var token: String? = null

    enum class Outcome { PULLED, PUSHED, MERGED, UP_TO_DATE }

    fun request(): AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build()

    //null if the user has to approve again
    suspend fun authorize(activity: Activity): AuthorizationResult? = suspendCancellableCoroutine { cont ->
        Identity.getAuthorizationClient(activity).authorize(request())
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }

    fun resultFromIntent(activity: Activity, data: Intent?): AuthorizationResult? =
        runCatching { Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data) }.getOrNull()

    fun setToken(t: String?) { token = t }

    fun sessionStart(activity: Activity) {
        if (!Prefs.driveLinked(activity)) return
        val app = activity.applicationContext
        scope.launch {
            val auth = withContext(Dispatchers.Main) { authorize(activity) }
            if (auth == null || auth.hasResolution() || auth.accessToken == null) {
                Prefs.setDriveState(app, "reconnect"); DailyWordManager.refreshOnMain(app); return@launch
            }
            token = auth.accessToken
            runSync(app)
        }
    }


    fun sessionEnd(ctx: Context) {
        if (!Prefs.driveLinked(ctx) || token == null) return
        val app = ctx.applicationContext
        scope.launch { runCatching { if (Backup.hash(Backup.snapshot(app)) != Prefs.syncedHash(app)) push(app, findFile(folderId())) } }
    }

    fun syncNow(ctx: Context) {
        if (token == null) return
        scope.launch { runSync(ctx.applicationContext) }
    }

    private suspend fun runSync(app: Context) {
        val r = runCatching { sync(app) }
        Prefs.setDriveState(app, r.fold({ "ok:${it.name}" }, { "error:${it.message?.take(80)}" }))
        DailyWordManager.refreshOnMain(app)
    }

    //if only one side changed take that one, if both changed merge
    fun sync(app: Context): Outcome {
        val folder = folderId()
        val remoteMeta = findFile(folder)
        val local = Backup.snapshot(app)
        val localHash = Backup.hash(local)
        val localDirty = localHash != Prefs.syncedHash(app)

        if (remoteMeta == null) { push(app, null, local); return Outcome.PUSHED }
        val remoteChanged = remoteMeta.second != Prefs.remoteModified(app)
        return when {
            !remoteChanged && !localDirty -> Outcome.UP_TO_DATE
            !remoteChanged -> { push(app, remoteMeta, local); Outcome.PUSHED }
            !localDirty || Prefs.syncedHash(app).isEmpty() && isFresh(local) -> {
                val remote = JSONObject(get("$API/files/${remoteMeta.first}?alt=media"))
                Backup.apply(app, remote)
                Prefs.setSyncMeta(app, Backup.hash(Backup.snapshot(app)), remoteMeta.second)
                Outcome.PULLED
            }
            else -> {
                val remote = JSONObject(get("$API/files/${remoteMeta.first}?alt=media"))
                Backup.apply(app, Backup.merge(local, remote))
                push(app, remoteMeta, Backup.snapshot(app))
                Outcome.MERGED
            }
        }
    }

    //new install with nothing in it, just take drives copy
    private fun isFresh(local: JSONObject) =
        (local.optJSONArray("accepted.json")?.length() ?: 0) == 0 && (local.optJSONArray("decks.json")?.length() ?: 0) == 0

    private fun push(app: Context, meta: Pair<String, String>?, snap: JSONObject = Backup.snapshot(app)) {
        val body = JSONObject(snap.toString()).put("savedAt", System.currentTimeMillis()).toString()
        val resp = if (meta == null) {
            val boundary = "kl${System.nanoTime()}"
            val metaJson = JSONObject().put("name", FILE).put("parents", org.json.JSONArray().put(folderId())).put("mimeType", "application/json")
            val multipart = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metaJson\r\n" +
                "--$boundary\r\nContent-Type: application/json\r\n\r\n$body\r\n--$boundary--"
            send("POST", "$UPLOAD/files?uploadType=multipart&fields=id,modifiedTime", multipart, "multipart/related; boundary=$boundary")
        } else {
            send("PATCH", "$UPLOAD/files/${meta.first}?uploadType=media&fields=id,modifiedTime", body, "application/json")
        }
        val o = JSONObject(resp)
        Prefs.setSyncMeta(app, Backup.hash(snap), o.optString("modifiedTime"))
    }

    @Volatile private var folderCache: String? = null

    private fun findFolder(): String? {
        val q = "name='$FOLDER' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val found = JSONObject(get("$API/files?q=${enc(q)}&fields=files(id)&spaces=drive")).optJSONArray("files")
        return if (found != null && found.length() > 0) found.getJSONObject(0).getString("id") else null
    }

    private fun folderId(): String {
        folderCache?.let { return it }
        val id = findFolder() ?: JSONObject(
            send("POST", "$API/files?fields=id", JSONObject().put("name", FOLDER).put("mimeType", "application/vnd.google-apps.folder").toString(), "application/json")
        ).getString("id")
        folderCache = id
        return id
    }

    private fun findFile(folder: String): Pair<String, String>? {
        val q = "name='$FILE' and '$folder' in parents and trashed=false"
        val files = JSONObject(get("$API/files?q=${enc(q)}&fields=files(id,modifiedTime)&orderBy=modifiedTime desc")).optJSONArray("files")
        if (files == null || files.length() == 0) return null
        val f = files.getJSONObject(0)
        return f.getString("id") to f.getString("modifiedTime")
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String = send("GET", url, null, null)

    private fun send(method: String, url: String, body: String?, type: String?): String {
        val c = URL(url).openConnection() as HttpURLConnection
        //HttpURLConnection dosent do PATCH
        if (method == "PATCH") { c.requestMethod = "POST"; c.setRequestProperty("X-HTTP-Method-Override", "PATCH") } else c.requestMethod = method
        c.setRequestProperty("Authorization", "Bearer ${token ?: error("Not signed in")}")
        c.connectTimeout = 15_000; c.readTimeout = 30_000
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", type)
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code == 401) { token = null; error("Google sign-in expired") }
        if (code !in 200..299) error("Drive $code")
        return text
    }

    // deletes the whole folder not just the file
    suspend fun deleteRemote(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        val ok = runCatching {
            findFolder()?.let { send("DELETE", "$API/files/$it", null, null) }
        }.isSuccess
        if (ok) unlink(ctx)
        ok
    }

    fun unlink(ctx: Context) {
        token = null; folderCache = null
        Prefs.setDriveLinked(ctx, false); Prefs.setDriveState(ctx, ""); Prefs.clearSyncMeta(ctx)
    }
}

//little buzz when u type something wrong
object Haptics {
    fun wrong(ctx: Context) {
        if (!Prefs.vibrate(ctx)) return
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 35, 60, 35), -1)) }
    }
}

class KanjiTileWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        DailyWordManager.ensureToday(ctx); Widgets.updateAll(ctx)
    }
}

class StreakWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = Widgets.updateAll(ctx)
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) = Widgets.updateAll(ctx)
}

class StudyWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = Widgets.updateAll(ctx)
}

// the 3 small widgets, they all get redrawn from DailyWordManager.refresh
object Widgets {
    const val EXTRA_TAB = "tab"

    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        fun ids(c: Class<*>) = mgr.getAppWidgetIds(ComponentName(ctx, c))
        runCatching { ids(KanjiTileWidget::class.java).forEach { mgr.updateAppWidget(it, tile(ctx)) } }
        runCatching { ids(StreakWidget::class.java).forEach { mgr.updateAppWidget(it, streak(ctx)) } }
        runCatching { ids(StudyWidget::class.java).forEach { mgr.updateAppWidget(it, study(ctx)) } }
    }

    //dark is for light wallpapers, same 3 styles just a lot darker behind the text
    fun bgRes(ctx: Context): Int {
        val dark = Prefs.widgetDark(ctx)
        return when (Prefs.widgetBg(ctx)) {
            Prefs.BG_SEMI -> if (dark) R.drawable.widget_bg_semi_dark else R.drawable.widget_bg_semi
            Prefs.BG_CLEAR -> if (dark) R.drawable.widget_bg_clear_dark else R.drawable.widget_bg_clear
            else -> if (dark) R.drawable.widget_bg_dark else R.drawable.widget_bg
        }
    }

    private fun base(ctx: Context, layout: Int): RemoteViews {
        val rv = RemoteViews(ctx.packageName, layout)
        rv.setInt(R.id.widget_root, "setBackgroundResource", bgRes(ctx))
        return rv
    }

    private fun open(ctx: Context, tab: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).putExtra(EXTRA_TAB, tab)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(ctx, 40 + tab, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun tile(ctx: Context): RemoteViews {
        val rv = base(ctx, R.layout.widget_tile)
        val w = DailyWordManager.state(ctx).word
        rv.setTextViewText(R.id.tv_kanji, w?.word ?: "鍵")
        rv.setTextViewText(R.id.tv_reading, w?.reading.orEmpty())
        rv.setViewVisibility(R.id.tv_reading, if (w == null) View.GONE else View.VISIBLE)
        rv.setOnClickPendingIntent(R.id.widget_root, open(ctx, 0))
        return rv
    }

    fun streak(ctx: Context): RemoteViews {
        val rv = base(ctx, R.layout.widget_streak)
        val days = ActivityLog.days(ctx)
        val st = ActivityLog.streak(days.keys)
        val goal = Prefs.dailyGoal(ctx)
        val density = ctx.resources.displayMetrics.density
        rv.setTextViewText(R.id.tv_streak, st.current.toString())
        rv.setTextViewText(
            R.id.tv_streak_sub,
            ctx.getString(if (st.todayDone) R.string.widget_streak_done else R.string.widget_streak_todo, st.longest)
        )
        rv.setImageViewBitmap(R.id.iv_flame, FlameArt.single(if (st.current > 0) 1f else 0f, (26 * density).toInt()))
        rv.setImageViewBitmap(R.id.iv_week, FlameArt.week(days, goal, density))
        rv.setOnClickPendingIntent(R.id.widget_root, open(ctx, 0))
        return rv
    }

    fun study(ctx: Context): RemoteViews {
        val rv = base(ctx, R.layout.widget_study)
        val queues = DeckStore.all(ctx).filter { it.source != DeckSource.ANKIDROID }.map { Study.queue(ctx, it.id) }
        val new = queues.sumOf { it.newLeft }
        val rev = queues.sumOf { it.learning + it.review }
        rv.setTextViewText(R.id.tv_due, (new + rev).toString())
        rv.setTextViewText(
            R.id.tv_due_label,
            ctx.getString(if (new + rev == 0) R.string.widget_due_none else R.string.widget_due_label)
        )
        rv.setTextViewText(R.id.tv_due_sub, ctx.getString(R.string.widget_due_sub, new, rev))
        rv.setOnClickPendingIntent(R.id.widget_root, open(ctx, 1))
        return rv
    }
}

//draws the flames for the streak widget since remoteviews cant do the bucket fill
object FlameArt {
    private val GRAY = 0x33FFFFFF
    private val ORANGE = 0xFFFF9A3D.toInt()
    private val flame: Path by lazy { PathParser.createPathFromPathData(Ic.FLAME_PATH) }

    private fun draw(c: Canvas, x: Float, y: Float, size: Float, fill: Float) {
        val m = Matrix().apply { setScale(size / 24f, size / 24f); postTranslate(x, y) }
        val p = Path(flame).apply { transform(m) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = GRAY; c.drawPath(p, paint)
        if (fill > 0f) {
            c.save()
            val top = y + size * (1f - fill.coerceIn(0f, 1f))
            c.clipRect(x, top, x + size, y + size)
            paint.color = ORANGE; c.drawPath(p, paint)
            c.restore()
        }
    }

    fun single(fill: Float, px: Int): Bitmap {
        val b = Bitmap.createBitmap(px.coerceAtLeast(8), px.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        draw(Canvas(b), 0f, 0f, px.toFloat(), fill)
        return b
    }

    //last 7 days, today on the right
    fun week(days: Map<String, ActivityLog.Day>, goal: Int, density: Float): Bitmap {
        val cell = 36 * density
        val flame = 24 * density
        val h = 50 * density
        val b = Bitmap.createBitmap((cell * 7).toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10 * density; textAlign = Paint.Align.CENTER }
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        val letters = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 0..6) {
            val d = days[ActivityLog.dayKey(cal)]
            val fill = if (d == null || d.total == 0) 0f else 0.12f + 0.88f * (d.total.toFloat() / goal.coerceAtLeast(1)).coerceAtMost(1f)
            val cx = cell * i + cell / 2
            draw(c, cx - flame / 2, 2 * density, flame, fill)
            text.color = if (i == 6) 0xFFFFFFFF.toInt() else 0xFFA9BABC.toInt()
            c.drawText(letters.format(cal.time).take(1).uppercase(), cx, h - 4 * density, text)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return b
    }
}

//checks github releases for a newer apk, only in the github build
object Updater {
    const val REPO = "Jideeh1/KanjiLock"
    private const val API = "https://api.github.com/repos/$REPO/releases/latest"
    private const val EVERY = 6 * 60 * 60 * 1000L

    data class Release(val tag: String, val version: String, val notes: String, val apkUrl: String?, val apkSize: Long, val page: String)

    val enabled: Boolean get() = BuildConfig.SELF_UPDATE

    fun current(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()

    fun parse(json: String): Release? {
        val o = JSONObject(json)
        if (o.optBoolean("draft") || o.optBoolean("prerelease")) return null
        val tag = o.optString("tag_name").ifBlank { return null }
        val assets = o.optJSONArray("assets") ?: JSONArray()
        var url: String? = null; var size = 0L
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                url = a.optString("browser_download_url"); size = a.optLong("size"); break
            }
        }
        return Release(tag, tag.trimStart('v', 'V'), cleanNotes(o.optString("body")), url, size, o.optString("html_url"))
    }

    //1.10.0 beats 1.9.2, anything after a dash is ignored
    fun newer(candidate: String, installed: String): Boolean {
        fun parts(v: String) = v.trimStart('v', 'V').substringBefore('-').split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val a = parts(candidate); val b = parts(installed)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    //release notes are markdown with banners and badges, keep only the readable bits
    fun cleanNotes(body: String): String = body.replace("\r", "").lines()
        .map { it.trim() }
        .filterNot { it.startsWith("<") || it.startsWith("![") || it.startsWith("> [!") || it == "---" }
        .map { l ->
            l.replace(Regex("<[^>]+>"), "").replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")
                .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
                .replace(Regex("^#+\\s*"), "").replace(Regex("^[-*]\\s+"), "• ").replace(Regex("^>\\s*"), "")
                .replace("**", "").replace("`", "")
        }
        .joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()

    fun fetch(url: String = API): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "KanjiLock-updater")
        c.connectTimeout = 10_000; c.readTimeout = 15_000
        if (c.responseCode != 200) error("GitHub ${c.responseCode}")
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    //null when theres nothing new, auto checks run at most every 6 hours and skip a version the user said no to
    suspend fun check(ctx: Context, manual: Boolean): Result<Release?> = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext Result.success(null)
        if (!manual && (!Prefs.autoUpdate(ctx) || System.currentTimeMillis() - Prefs.updateChecked(ctx) < EVERY)) {
            return@withContext Result.success(null)
        }
        runCatching {
            val r = parse(fetch())
            Prefs.setUpdateChecked(ctx, System.currentTimeMillis())
            r?.takeIf { newer(it.version, current(ctx)) && (manual || it.tag != Prefs.skippedTag(ctx)) }
        }
    }

    suspend fun download(ctx: Context, r: Release, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "KanjiLock-${r.version}.apk")
        val c = URL(r.apkUrl ?: error("no apk in this release")).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "KanjiLock-updater")
        c.instanceFollowRedirects = true
        c.connectTimeout = 15_000; c.readTimeout = 30_000
        if (c.responseCode != 200) error("download ${c.responseCode}")
        val total = c.contentLengthLong.takeIf { it > 0 } ?: r.apkSize
        c.inputStream.use { input ->
            out.outputStream().use { o ->
                val buf = ByteArray(64 * 1024); var done = 0L
                while (true) {
                    val n = input.read(buf); if (n < 0) break
                    o.write(buf, 0, n); done += n
                    if (total > 0) progress((done.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        out
    }

    //android asks the user once to alow installs from this app
    fun canInstall(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    fun askInstallPermission(ctx: Context) {
        ctx.startActivity(
            Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(ctx: Context, apk: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
