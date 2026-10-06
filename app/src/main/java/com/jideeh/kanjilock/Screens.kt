package com.jideeh.kanjilock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.jideeh.kanjilock.DailyWordManager.Status
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TodayScreen(tick: Int, snackbar: SnackbarHostState, onStudy: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember(tick) { DailyWordManager.state(ctx) }
    val days = remember(tick) { ActivityLog.days(ctx) }
    val active = days.keys
    val goal = remember(tick) { Prefs.dailyGoal(ctx) }
    val streak = remember(tick) { ActivityLog.streak(active) }
    val deckSize = remember(tick) { AcceptedStore.keys(ctx).size }
    val minedSize = remember(tick) { MinedStore.all(ctx).size }
    val due = remember(tick) { Study.dueNow(ctx) }
    val hasCards = remember(tick) { DeckStore.all(ctx).any { it.cardCount > 0 || it.isLinked } }
    val speaker = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    var calendarOpen by rememberSaveable { mutableStateOf(false) }

    val now = Date()
    val kicker = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(now)
    val dateLine = SimpleDateFormat("EEE, d MMM, yyyy", Locale.getDefault()).format(now)

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(BarClearance)
    ) {
        ScreenHeader(kicker = kicker, title = stringResource(R.string.tab_today), subtitle = dateLine) {
            GlassIconButton(
                if (calendarOpen) Ic.expandLess else Ic.calendar,
                stringResource(R.string.cd_calendar),
                selected = calendarOpen
            ) { calendarOpen = !calendarOpen }
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakCard(streak, deckSize, due, minedSize, days, goal, calendarOpen) { calendarOpen = !calendarOpen }

            WordCard(
                today = today,
                onSpeak = { w -> speaker.speak(w.reading.ifBlank { w.word }) },
                onStep = { DailyWordManager.step(ctx, it) }
            )

            if (today.status == Status.ACTIVE) {
                ActionRow(
                    canReject = today.canReject,
                    accepted = today.accepted,
                    onReject = {
                        val w = today.word ?: return@ActionRow
                        val idx = today.index
                        DailyWordManager.reject(ctx)
                        scope.launch {
                            val r = snackbar.showSnackbar(
                                ctx.getString(R.string.snack_rejected, w.word),
                                actionLabel = ctx.getString(R.string.undo),
                                duration = SnackbarDuration.Short
                            )
                            if (r == SnackbarResult.ActionPerformed) DailyWordManager.undoReject(ctx, w, idx)
                        }
                    },
                    onReload = { DailyWordManager.reload(ctx) },
                    onAccept = {
                        DailyWordManager.check(ctx)
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.snack_accepted)) }
                    }
                )
            }

            ReviewCard(due, hasCards, onStudy)

            if (Prefs.lockNotif(ctx) && !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    CardTitle(Ic.notifications, stringResource(R.string.notif_off_title))
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.notif_off_body), style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                    Spacer(Modifier.height(14.dp))
                    WhitePill(stringResource(R.string.open_settings)) {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        )
                    }
                }
            }

            LockScreenCard(tick)
        }
    }
}


@Composable
private fun CardTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = Ink.Muted)
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.Text)
    }
}


@Composable
fun StreakCard(
    streak: ActivityLog.Streak,
    deck: Int,
    due: Int,
    mined: Int,
    days: Map<String, ActivityLog.Day>,
    goal: Int,
    calendarOpen: Boolean,
    onToggle: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Ic.flame, null,
                        tint = if (streak.todayDone) Ink.Flame else Ink.Muted,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("${streak.current}", style = MaterialTheme.typography.displaySmall, color = Ink.Text)
                }
                Text(
                    stringResource(if (streak.todayDone) R.string.streak_done else R.string.streak_pending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.Muted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                GlassIconButton(
                    if (calendarOpen) Ic.expandLess else Ic.expandMore,
                    stringResource(R.string.cd_calendar), size = 34.dp, onClick = onToggle
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.streak_best, streak.longest), style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        InnerTile(Modifier.fillMaxWidth()) {
            Stat(deck, stringResource(R.string.stat_in_deck), Modifier.weight(1f))
            Stat(due, stringResource(R.string.stat_due), Modifier.weight(1f))
            Stat(mined, stringResource(R.string.stat_mined), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        StreakCalendar(days, goal, calendarOpen)
    }
}

@Composable
private fun Stat(n: Int, label: String, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        Text("$n", style = MaterialTheme.typography.titleLarge, color = Ink.Text)
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, modifier = Modifier.padding(bottom = 2.dp))
    }
}


@Composable
private fun WordCard(today: DailyWordManager.Today, onSpeak: (Word) -> Unit, onStep: (Int) -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (today.total > 1) stringResource(R.string.words_today, today.total) else stringResource(R.string.word_of_the_day),
                style = MaterialTheme.typography.titleMedium,
                color = Ink.Text,
                modifier = Modifier.weight(1f)
            )
            if (today.status == Status.ACTIVE && today.total > 1) {
                GlassIconButton(Ic.chevronLeft, stringResource(R.string.cd_prev), size = 34.dp) { onStep(-1) }
                Text(
                    "${today.index + 1}/${today.total}",
                    style = MaterialTheme.typography.labelLarge, color = Ink.Muted,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                GlassIconButton(Ic.chevronRight, stringResource(R.string.cd_next), size = 34.dp) { onStep(+1) }
                Spacer(Modifier.width(8.dp))
            }
            val w = today.word
            if (w != null) GlassIconButton(Ic.volume, stringResource(R.string.cd_speak), size = 34.dp) { onSpeak(w) }
        }
        Spacer(Modifier.height(14.dp))

        AnimatedContent(
            targetState = today.word?.key to today.status,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220))) togetherWith fadeOut(tween(120))
            },
            label = "word"
        ) { _ ->
            val w = today.word
            if (today.status == Status.ACTIVE && w != null) {
                WordBody(w, today.accepted, today.rejected)
            } else if (today.status == Status.DISMISSED) {
                EmptyState("休", stringResource(R.string.dismissed_title), stringResource(R.string.dismissed_body))
            } else {
                EmptyState("空", stringResource(R.string.empty_title), stringResource(R.string.empty_body))
            }
        }
    }
}


@Composable
private fun WordBody(w: Word, accepted: Boolean, rejected: Boolean) {
    GlassCard(Modifier.fillMaxWidth(), high = true, padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ic.sun, null, Modifier.size(15.dp), tint = Ink.Muted)
            Spacer(Modifier.width(8.dp))
            Kicker(SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()), Modifier.weight(1f))
            Tag(stringResource(if (w.isKanji) R.string.tag_kanji else R.string.tag_word))
            if (w.isMined) { Spacer(Modifier.width(6.dp)); Tag(stringResource(R.string.tag_mined)) }
            if (accepted) { Spacer(Modifier.width(6.dp)); Tag(stringResource(R.string.tag_in_deck), Ink.Accept) }
            if (rejected) { Spacer(Modifier.width(6.dp)); Tag(stringResource(R.string.tag_rejected), Ink.Reject) }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .widthIn(min = 104.dp)
                    .heightIn(min = 104.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val size = when (w.word.length) { 1 -> 68.sp; 2 -> 48.sp; 3 -> 38.sp; else -> 28.sp }
                Text(w.word, style = KanjiStyle.copy(fontSize = size), color = Ink.Text, maxLines = 1)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(w.reading, style = MaterialTheme.typography.titleLarge.merge(JapaneseText), color = Ink.Muted)
                Spacer(Modifier.height(2.dp))
                Text(w.meaning, style = MaterialTheme.typography.headlineSmall, color = Ink.Text, fontWeight = FontWeight.Medium)
            }
        }
        if (w.example.isNotBlank() || w.exampleMeaning.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.GlassStroke))
            Spacer(Modifier.height(12.dp))
            Kicker(stringResource(R.string.example))
            Spacer(Modifier.height(6.dp))
            if (w.example.isNotBlank()) Text(w.example, style = MaterialTheme.typography.titleMedium.merge(JapaneseText), color = Ink.Text)
            if (w.exampleReading.isNotBlank()) Text(w.exampleReading, style = MaterialTheme.typography.bodySmall.merge(JapaneseText), color = Ink.Muted)
            if (w.exampleMeaning.isNotBlank()) Text(w.exampleMeaning, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
            TatoebaLink(w.exampleId)
        }
    }
}

@Composable
private fun Tag(text: String, dot: Color? = null) {
    Row(
        Modifier.clip(CircleShape).background(Ink.Glass).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot != null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
    }
}

@Composable
private fun ActionRow(canReject: Boolean, accepted: Boolean, onReject: () -> Unit, onReload: () -> Unit, onAccept: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
        RoundAction(Ic.close, stringResource(R.string.action_reject), Ink.Reject, canReject, onReject)
        Spacer(Modifier.width(34.dp))
        RoundAction(Ic.refresh, stringResource(R.string.action_reload), Ink.Reload, true, onReload)
        Spacer(Modifier.width(34.dp))
        RoundAction(
            Ic.check,
            stringResource(if (accepted) R.string.action_in_deck else R.string.action_accept),
            Ink.Accept, !accepted, onAccept
        )
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    val bg by animateColorAsState(if (enabled) color else Ink.GlassHigh, label = "bg")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = bg,
            interactionSource = interaction,
            modifier = Modifier.size(64.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, label,
                    tint = if (enabled) Color.White else Ink.Faint,
                    modifier = Modifier.size(28.dp).alpha(if (enabled) 1f else 0.8f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (enabled) Ink.Text else Ink.Faint)
    }
}


@Composable
internal fun ReviewCard(due: Int, hasCards: Boolean, onStudy: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.review_title), style = MaterialTheme.typography.titleMedium, color = Ink.Text)
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        !hasCards -> stringResource(R.string.review_empty)
                        due == 0 -> stringResource(R.string.review_caught_up)
                        else -> stringResource(R.string.review_due, due)
                    },
                    style = MaterialTheme.typography.bodyMedium, color = Ink.Muted
                )
            }
            WhitePill(stringResource(R.string.study_now), enabled = hasCards, onClick = onStudy)
        }
    }
}

@Composable
private fun LockScreenCard(tick: Int) {
    val ctx = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val hasWidget = remember(tick) { KanjiWidgetProvider.hasWidgets(ctx) }
    GlassCard(Modifier.fillMaxWidth()) {
        CardTitle(Ic.lock, stringResource(R.string.lock_card_title))
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(if (hasWidget) R.string.lock_card_body_has_widget else R.string.lock_card_body),
            style = MaterialTheme.typography.bodyMedium, color = Ink.Muted
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WhitePill(
                stringResource(if (hasWidget) R.string.add_another_widget else R.string.add_widget),
                icon = Ic.widgets
            ) {
                if (!KanjiWidgetProvider.requestPin(ctx)) Toast.makeText(ctx, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
            }
            GhostPill(stringResource(if (expanded) R.string.hide_steps else R.string.show_steps)) { expanded = !expanded }
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Step(stringResource(R.string.steps_samsung_title), stringResource(R.string.steps_samsung_body))
                Step(stringResource(R.string.steps_android16_title), stringResource(R.string.steps_android16_body))
                Step(stringResource(R.string.steps_other_title), stringResource(R.string.steps_other_body))
            }
        }
    }
}

@Composable
private fun Step(title: String, body: String) {
    Column(Modifier.clickable(enabled = false) {}) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Ink.Text)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
    }
}


fun sourceColor(s: DeckSource) = when (s) {
    DeckSource.KANJILOCK -> Ink.Flame
    DeckSource.ANKI -> Ink.Easy
    DeckSource.ANKIDROID -> Ink.Good
    DeckSource.KOTOBA -> Color(0xFFB48CE0)
}

@Composable
fun StudyScreen(tick: Int) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var local by remember { mutableIntStateOf(0) }
    val decks = remember(tick, local) { DeckStore.all(ctx) }
    var selectedId by rememberSaveable { mutableStateOf(Prefs.selectedDeck(ctx)) }
    val deck = decks.firstOrNull { it.id == selectedId } ?: decks.first()
    var showLinkPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Deck?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { Sfx.init(ctx) }

    fun select(id: String) { selectedId = id; Prefs.setSelectedDeck(ctx, id) }
    fun importWith(block: () -> Deck) {
        busy = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching(block) }
            busy = false
            r.onSuccess { select(it.id); local++; DailyWordManager.refresh(ctx); Toast.makeText(ctx, ctx.getString(R.string.deck_imported, it.name, it.cardCount), Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(ctx, ctx.getString(R.string.deck_import_failed, it.message ?: ""), Toast.LENGTH_LONG).show() }
        }
    }

    val ankiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importWith { DeckStore.importAnki(ctx, uri) }
    }
    val kotobaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importWith { DeckStore.importKotoba(ctx, uri) }
    }
    var pendingLinkDeck by remember { mutableStateOf<Deck?>(null) }
    var askAnki by remember { mutableStateOf(false) } //explain first before the permision popup
    val ankiPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) Toast.makeText(ctx, R.string.ankidroid_permission_denied, Toast.LENGTH_LONG).show()
        else if (pendingLinkDeck != null) linkByName(ctx, pendingLinkDeck!!) { local++ } else showLinkPicker = true
    }
    fun withAnkiDroid(forDeck: Deck?, then: () -> Unit) {
        if (!AnkiDroid.installed(ctx)) {
            Toast.makeText(ctx, R.string.ankidroid_missing, Toast.LENGTH_LONG).show()
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${AnkiDroid.PACKAGE}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (!AnkiDroid.hasPermission(ctx)) { pendingLinkDeck = forDeck; askAnki = true; return }
        then()
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(BarClearance)
    ) {
        ScreenHeader(
            kicker = stringResource(R.string.study_kicker),
            title = stringResource(R.string.tab_study),
            subtitle = stringResource(R.string.study_decks_sub, decks.size)
        )

        DeckShelf(
            decks = decks,
            selected = deck.id,
            busy = busy,
            onSelect = { select(it) },
            onImportAnki = { ankiPicker.launch(arrayOf("application/octet-stream", "application/zip", "application/apkg", "*/*")) },
            onImportKotoba = { kotobaPicker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "text/tab-separated-values")) },
            onLinkAnkiDroid = { withAnkiDroid(null) { showLinkPicker = true } }
        )
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SourceTag(deck.source)
            Text(
                when {
                    deck.isLinked -> stringResource(R.string.deck_synced_note)
                    deck.source == DeckSource.ANKI -> stringResource(R.string.deck_local_anki_note)
                    else -> ""
                },
                style = MaterialTheme.typography.labelMedium, color = Ink.Muted,
                modifier = Modifier.weight(1f), maxLines = 2
            )
            if (deck.source == DeckSource.ANKI) {
                GhostPill(stringResource(R.string.sync_ankiweb)) { withAnkiDroid(deck) { linkByName(ctx, deck) { local++ } } }
            }
            if (deck.id != DeckStore.ACCEPTED) {
                GlassIconButton(Ic.delete, stringResource(R.string.delete), size = 34.dp, tint = Ink.Muted) { confirmDelete = deck }
            }
        }
        Spacer(Modifier.height(12.dp))

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (deck.isLinked) AnkiDroidSession(deck, tick) else LocalSession(deck, tick)
            if (!deck.isLinked) InsightsCard(deck.id, tick)
        }
    }

    if (showLinkPicker) {
        val list = remember { runCatching { AnkiDroid.decks(ctx) }.getOrDefault(emptyList()) }
        AlertDialog(
            onDismissRequest = { showLinkPicker = false },
            containerColor = Ink.BgMid,
            title = { Text(stringResource(R.string.pick_ankidroid_deck)) },
            text = {
                if (list.isEmpty()) Text(stringResource(R.string.ankidroid_no_decks), color = Ink.Muted)
                else LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(list, key = { it.id }) { d ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ink.Glass)
                                .clickable {
                                    val created = DeckStore.create(ctx, d.name.substringAfterLast("::"), DeckSource.ANKIDROID, emptyList(), d.name, d.id)
                                    showLinkPicker = false; select(created.id); local++
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(d.name, color = Ink.Text, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Counts(d.new, d.learn, d.review)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLinkPicker = false }) { Text(stringResource(R.string.cancel), color = Ink.Text) } }
        )
    }

    if (askAnki) {
        AlertDialog(
            onDismissRequest = { askAnki = false },
            containerColor = Ink.BgMid,
            title = { Text(stringResource(R.string.ankidroid_consent_title)) },
            text = { Text(stringResource(R.string.ankidroid_consent_body), color = Ink.Muted) },
            confirmButton = {
                TextButton(onClick = { askAnki = false; ankiPermission.launch(AnkiDroid.PERMISSION) }) {
                    Text(stringResource(R.string.continue_btn), color = Ink.Text)
                }
            },
            dismissButton = { TextButton(onClick = { askAnki = false }) { Text(stringResource(R.string.cancel), color = Ink.Muted) } }
        )
    }

    confirmDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = Ink.BgMid,
            title = { Text(stringResource(R.string.delete_deck_title, d.name)) },
            text = { Text(stringResource(if (d.isLinked) R.string.delete_linked_body else R.string.delete_deck_body), color = Ink.Muted) },
            confirmButton = {
                TextButton(onClick = { DeckStore.delete(ctx, d.id); confirmDelete = null; select(DeckStore.ACCEPTED); local++ }) {
                    Text(stringResource(R.string.delete), color = Ink.Reject)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel), color = Ink.Text) } }
        )
    }
}

private fun linkByName(ctx: android.content.Context, deck: Deck, done: () -> Unit) {
    val match = runCatching { AnkiDroid.findByName(ctx, deck.ankiDeckName.ifBlank { deck.name }) }.getOrNull()
    if (match == null) {
        Toast.makeText(ctx, ctx.getString(R.string.ankidroid_no_match, deck.ankiDeckName.ifBlank { deck.name }), Toast.LENGTH_LONG).show()
        return
    }
    DeckStore.update(ctx, deck.copy(source = DeckSource.ANKIDROID, ankiDroidDeckId = match.id, ankiDeckName = match.name))
    Toast.makeText(ctx, ctx.getString(R.string.ankidroid_linked, match.name), Toast.LENGTH_SHORT).show()
    done()
}

@Composable
fun SourceTag(s: DeckSource) {
    Row(
        Modifier.clip(CircleShape).background(Ink.Glass).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(sourceColor(s), 6.dp)
        Spacer(Modifier.width(5.dp))
        Text(s.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
    }
}

@Composable
private fun Counts(new: Int, learn: Int, due: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$new", color = Ink.Easy, style = MaterialTheme.typography.labelLarge)
        Text("$learn", color = Ink.Hard, style = MaterialTheme.typography.labelLarge)
        Text("$due", color = Ink.Good, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DeckShelf(
    decks: List<Deck>,
    selected: String,
    busy: Boolean,
    onSelect: (String) -> Unit,
    onImportAnki: () -> Unit,
    onImportKotoba: () -> Unit,
    onLinkAnkiDroid: () -> Unit
) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(decks, key = { it.id }) { d ->
            val on = d.id == selected
            val counts = remember(d.id, d.cardCount) {
                if (d.isLinked) null else Study.queue(ctx, d.id).let { Triple(it.newLeft, it.learning, it.review) }
            }
            Column(
                Modifier
                    .width(164.dp)
                    .height(116.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) Ink.GlassHigh else Ink.Glass)
                    .border(if (on) 1.5.dp else 1.dp, if (on) Color.White.copy(alpha = 0.85f) else Ink.GlassStroke, RoundedCornerShape(18.dp))
                    .clickable { onSelect(d.id) }
                    .padding(14.dp)
            ) {
                SourceTag(d.source)
                Spacer(Modifier.height(8.dp))
                Text(
                    d.name, style = MaterialTheme.typography.titleSmall, color = Ink.Text,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                if (counts != null) Counts(counts.first, counts.second, counts.third)
                else Text(stringResource(R.string.via_ankidroid), style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
            }
        }
        item {
            Box {
                Column(
                    Modifier
                        .width(120.dp)
                        .height(116.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Ink.GlassStroke, RoundedCornerShape(18.dp))
                        .clickable(enabled = !busy) { menu = true }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Ic.add, null, Modifier.size(26.dp), tint = Ink.Muted)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(if (busy) R.string.importing else R.string.add_deck), style = MaterialTheme.typography.labelLarge, color = Ink.Muted)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Ink.BgMid, shape = RoundedCornerShape(14.dp)) {
                    MenuItem(R.string.import_anki, R.string.import_anki_sub) { menu = false; onImportAnki() }
                    MenuItem(R.string.import_kotoba, R.string.import_kotoba_sub) { menu = false; onImportKotoba() }
                    MenuItem(R.string.link_ankidroid, R.string.link_ankidroid_sub) { menu = false; onLinkAnkiDroid() }
                }
            }
        }
    }
}


@Composable
private fun MenuItem(title: Int, sub: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Column {
                Text(stringResource(title), color = Ink.Text)
                Text(stringResource(sub), style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
            }
        },
        onClick = onClick
    )
}

@Composable
private fun LocalSession(deck: Deck, tick: Int) {
    val ctx = LocalContext.current
    var bump by remember { mutableIntStateOf(0) }
    val q = remember(tick, bump, deck.id) { Study.queue(ctx, deck.id) }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CountTile(q.newLeft, stringResource(R.string.count_new), Ink.Easy, Modifier.weight(1f))
        CountTile(q.learning, stringResource(R.string.count_learning), Ink.Hard, Modifier.weight(1f))
        CountTile(q.review, stringResource(R.string.count_review), Ink.Good, Modifier.weight(1f))
    }
    val card = q.card
    val srs = q.srs
    when {
        q.deckSize == 0 -> GlassCard(Modifier.fillMaxWidth()) {
            EmptyState("覚", stringResource(R.string.study_empty_title), stringResource(R.string.study_empty_body))
        }
        card == null || srs == null -> GlassCard(Modifier.fillMaxWidth()) {
            EmptyState(
                "済", stringResource(R.string.study_done_title),
                q.nextDue?.let { stringResource(R.string.study_done_next, Scheduler.label(it - System.currentTimeMillis())) }
                    ?: stringResource(R.string.study_done_body)
            )
        }
        else -> {
            val preview = remember(srs) { Scheduler.preview(srs, System.currentTimeMillis()) }
            AnimatedContent(
                targetState = card to srs,
                transitionSpec = { (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { it / 8 }) togetherWith fadeOut(tween(100)) },
                contentKey = { it.first.id + it.second.reps },
                label = "card"
            ) { (c, s) ->
                Flashcard(
                    card = c,
                    stateLabel = stringResource(
                        when (s.state) { SrsCard.NEW -> R.string.count_new; SrsCard.REVIEW -> R.string.count_review; else -> R.string.count_learning }
                    ),
                    labels = Grade.entries.map { Scheduler.label(preview.getValue(it)) },
                    buttons = 4
                ) { g ->
                    Sfx.play(ctx, g)
                    Study.answer(ctx, deck.id, s, Grade.entries[g])
                    bump++
                }
            }
        }
    }
}


@Composable
private fun AnkiDroidSession(deck: Deck, tick: Int) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bump by remember { mutableIntStateOf(0) }
    var shownAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val state by produceState<Result<Pair<AnkiDroid.AdDeck?, AnkiDroid.AdCard?>>?>(null, deck.id, bump, tick) {
        value = withContext(Dispatchers.IO) {
            runCatching { AnkiDroid.deck(ctx, deck.ankiDroidDeckId) to AnkiDroid.next(ctx, deck.ankiDroidDeckId) }
        }
        shownAt = System.currentTimeMillis()
    }

    val r = state
    when {
        r == null -> GlassCard(Modifier.fillMaxWidth()) { Text(stringResource(R.string.loading_ankidroid), color = Ink.Muted) }
        r.isFailure -> GlassCard(Modifier.fillMaxWidth()) {
            EmptyState("鍵", stringResource(R.string.ankidroid_error_title), stringResource(R.string.ankidroid_error_body))
        }
        else -> {
            val (info, next) = r.getOrThrow()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountTile(info?.new ?: 0, stringResource(R.string.count_new), Ink.Easy, Modifier.weight(1f))
                CountTile(info?.learn ?: 0, stringResource(R.string.count_learning), Ink.Hard, Modifier.weight(1f))
                CountTile(info?.review ?: 0, stringResource(R.string.count_review), Ink.Good, Modifier.weight(1f))
            }
            if (next == null) {
                GlassCard(Modifier.fillMaxWidth()) {
                    EmptyState("済", stringResource(R.string.study_done_title), stringResource(R.string.ankidroid_done_body))
                }
            } else {
                val four = next.buttons >= 4
                val labels = if (four) next.times.take(4) else listOf(next.times.getOrElse(0) { "" }, "", next.times.getOrElse(1) { "" }, next.times.getOrElse(2) { "" })
                Flashcard(next.card, stringResource(R.string.via_ankidroid), labels, if (four) 4 else 3) { g ->
                    Sfx.play(ctx, g)
                    val ease = if (four) g + 1 else when (g) { 0 -> 1; 3 -> 3; else -> 2 }
                    val taken = System.currentTimeMillis() - shownAt
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { runCatching { AnkiDroid.answer(ctx, next, ease, taken) }.getOrDefault(false) }
                        if (ok) Study.logReview(ctx, g + 1, SrsCard.REVIEW, deck.id)
                        else Toast.makeText(ctx, R.string.ankidroid_answer_failed, Toast.LENGTH_SHORT).show()
                        bump++
                    }
                }
            }
        }
    }
}

@Composable
fun CountTile(n: Int, label: String, color: Color, modifier: Modifier) {
    GlassCard(modifier, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
        }
        Spacer(Modifier.height(4.dp))
        Text("$n", style = MaterialTheme.typography.headlineMedium, color = Ink.Text)
    }
}

//if the answer is kana, romaji gets turned to kana while u type
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Flashcard(
    card: DeckCard,
    stateLabel: String,
    labels: List<String>,
    buttons: Int,
    startRevealed: Boolean = false,
    startTyped: String = "",
    onGrade: (Int) -> Unit
) {
    var revealed by rememberSaveable(card.id) { mutableStateOf(startRevealed) }
    var typed by remember(card.id) { mutableStateOf(TextFieldValue(startTyped, TextRange(startTyped.length))) }
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val bring = remember { BringIntoViewRequester() } //keeps the card above the keyboard
    val kanaMode = card.answers.any { Romaji.hasKana(it) }
    val canType = card.answers.isNotEmpty()

    fun reveal() {
        if (kanaMode) typed = TextFieldValue(Romaji.toHiragana(typed.text, final = true))
        focus.clearFocus(); revealed = true
    }

    Column(Modifier.bringIntoViewRequester(bring), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.fillMaxWidth(), high = true, padding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Kicker(card.instructions.ifBlank { stringResource(if (canType) R.string.instruction else R.string.instruction_recall) }, Modifier.weight(1f))
                Kicker(stateLabel)
            }
            Spacer(Modifier.height(16.dp))
            val size = when (card.front.length) { 1 -> 92.sp; 2 -> 70.sp; 3 -> 54.sp; in 4..6 -> 38.sp; else -> 26.sp }
            Text(
                card.front, style = KanjiStyle.copy(fontSize = size), color = Ink.Text,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            if (canType) {
                BasicTextField(
                    value = typed,
                    onValueChange = { v ->
                        typed = if (kanaMode) {
                            val conv = Romaji.toHiragana(v.text)
                            TextFieldValue(conv, TextRange(conv.length))
                        } else v
                    },
                    enabled = !revealed,
                    singleLine = true,
                    textStyle = TextStyle(color = Ink.Text, fontSize = 22.sp, textAlign = TextAlign.Center).merge(JapaneseText),
                    cursorBrush = SolidColor(Ink.Text),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (kanaMode) KeyboardType.Ascii else KeyboardType.Text,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { reveal() }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (typed.text.isEmpty()) {
                                Text(
                                    stringResource(if (kanaMode) R.string.type_hint_romaji else R.string.type_hint_text),
                                    color = Ink.Faint, fontSize = 16.sp, textAlign = TextAlign.Center
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Ink.Glass)
                        .border(1.dp, Ink.GlassStroke, RoundedCornerShape(12.dp))
                        .onFocusChanged { if (it.isFocused) scope.launch { delay(350); bring.bringIntoView() } }
                        .padding(vertical = 14.dp, horizontal = 12.dp)
                )
            }

            AnimatedVisibility(revealed, enter = fadeIn() + expandVertically()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.GlassStroke))
                    Spacer(Modifier.height(16.dp))
                    if (card.reading.isNotBlank()) {
                        Text(card.reading, style = MaterialTheme.typography.headlineMedium.merge(JapaneseText), color = Ink.Text, textAlign = TextAlign.Center)
                    }
                    if (canType && typed.text.isNotBlank()) {
                        val ok = Romaji.matches(typed.text, card.answers)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (ok) stringResource(R.string.typed_ok) else stringResource(R.string.typed_wrong, typed.text),
                            style = MaterialTheme.typography.labelLarge.merge(JapaneseText),
                            color = if (ok) Ink.Good else Ink.Again
                        )
                    }
                    if (card.meaning.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(card.meaning, style = MaterialTheme.typography.titleLarge, color = Ink.Text, textAlign = TextAlign.Center)
                    }
                    if (card.extra.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(card.extra, style = MaterialTheme.typography.bodyMedium.merge(JapaneseText), color = Ink.Muted, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (!revealed) {
            WhitePill(stringResource(R.string.show_answer), Modifier.fillMaxWidth()) { reveal() }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val names = listOf(R.string.grade_again, R.string.grade_hard, R.string.grade_good, R.string.grade_easy)
                val colors = listOf(Ink.Again, Ink.Hard, Ink.Good, Ink.Easy)
                for (i in 0..3) {
                    if (buttons < 4 && i == 1) continue
                    GradeButton(stringResource(names[i]), labels.getOrElse(i) { "" }, colors[i], Modifier.weight(1f)) { onGrade(i) }
                }
            }
        }
    }
}

@Composable
private fun GradeButton(label: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.18f),
        contentColor = Ink.Text,
        modifier = modifier.border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
        }
    }
}


@Composable
fun InsightsCard(deckId: String, tick: Int) {
    val ctx = LocalContext.current
    val ins = remember(tick, deckId) { Study.insights(ctx, deckId) }
    InsightsBody(ins)
}

@Composable
fun InsightsBody(ins: Study.Insights) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.insights), style = MaterialTheme.typography.titleMedium, color = Ink.Text)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(ins.reviewsToday.toString(), stringResource(R.string.reviews_today), Modifier.weight(1f))
            Metric(ins.retention?.let { "$it%" } ?: "—", stringResource(R.string.retention_30d), Modifier.weight(1f))
            Metric(ins.mature.toString(), stringResource(R.string.mature_cards), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Kicker(stringResource(R.string.forecast_7d))
        Spacer(Modifier.height(8.dp))
        val max = (ins.forecast.maxOrNull() ?: 0).coerceAtLeast(1)
        Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            ins.forecast.forEachIndexed { i, v ->
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (v > 0) Text("$v", style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
                    Box(
                        Modifier.fillMaxWidth().height((44f * v / max).dp.coerceAtLeast(3.dp))
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (i == 0) Color.White else Ink.GlassHigh)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(if (i == 0) stringResource(R.string.today_short) else "+$i", style = MaterialTheme.typography.labelSmall, color = Ink.Faint)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(Ink.Easy, stringResource(R.string.count_new), ins.newCount)
            Legend(Ink.Hard, stringResource(R.string.count_learning), ins.learning)
            Legend(Ink.Good, stringResource(R.string.young), ins.young)
            Legend(Color.White, stringResource(R.string.mature), ins.mature)
        }
        if (ins.leeches.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.leeches, ins.leeches.size, ins.leeches.take(6).joinToString("・")),
                style = MaterialTheme.typography.bodySmall.merge(JapaneseText), color = Ink.Again
            )
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Ink.Glass).padding(12.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Ink.Text)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


@Composable
private fun Legend(c: Color, label: String, n: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(c, 7.dp); Spacer(Modifier.width(5.dp))
        Text("$label $n", style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
    }
}

@Composable
fun WordsScreen(tick: Int, snackbar: SnackbarHostState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }

    val accepted = remember(tick) { AcceptedStore.all(ctx) }
    val rejected = remember(tick) { RejectedStore.all(ctx) }
    val mined = remember(tick) { MinedStore.all(ctx).reversed() }
    val deckKeys = remember(tick) { accepted.map { it.key }.toSet() }
    val source = when (mode) { 0 -> accepted; 1 -> rejected; else -> mined }
    val q = query.trim()
    val shown = if (q.isEmpty()) source else source.filter {
        it.word.contains(q) || it.reading.contains(q) || it.meaning.contains(q, ignoreCase = true)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val res = Importer.read(ctx, uri)
        val added = MinedStore.add(ctx, res.words)
        DailyWordManager.refresh(ctx)
        val msg = if (res.skipped > 0) ctx.getString(R.string.snack_imported_skipped, added, res.skipped)
        else ctx.getString(R.string.snack_imported, added)
        scope.launch { snackbar.showSnackbar(msg) }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(
            kicker = stringResource(R.string.words_kicker, accepted.size + mined.size),
            title = stringResource(R.string.lists_title),
            subtitle = stringResource(R.string.lists_sub)
        ) {
            when (mode) {
                0 -> Box {
                    GlassIconButton(Ic.share, stringResource(R.string.export)) { menu = true }
                    DropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false },
                        containerColor = Ink.BgMid,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Column { Text(stringResource(R.string.export_csv)); Text(stringResource(R.string.export_csv_short), style = MaterialTheme.typography.labelSmall, color = Ink.Muted) } },
                            onClick = { menu = false; Exporter.shareCsv(ctx) }
                        )
                        DropdownMenuItem(
                            text = { Column { Text(stringResource(R.string.export_anki)); Text(stringResource(R.string.export_anki_short), style = MaterialTheme.typography.labelSmall, color = Ink.Muted) } },
                            onClick = { menu = false; Exporter.shareAnki(ctx) }
                        )
                    }
                }
                2 -> GlassIconButton(Ic.upload, stringResource(R.string.import_words)) {
                    picker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "text/tab-separated-values", "application/json"))
                }
            }
        }

        PillTabs(
            options = listOf(
                stringResource(R.string.tab_accepted, accepted.size),
                stringResource(R.string.tab_rejected, rejected.size),
                stringResource(R.string.tab_mined_n, mined.size),
                stringResource(R.string.tab_dictionary)
            ),
            selected = mode,
            onSelect = { mode = it; query = "" },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        if (mode == 3) {
            DictionaryPane(tick, snackbar, deckKeys)
        } else {
        if (source.isNotEmpty()) {
            SearchField(query, { query = it }, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }

        if (source.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                when (mode) {
                    0 -> EmptyState("覚", stringResource(R.string.empty_accepted_title), stringResource(R.string.empty_accepted_body))
                    1 -> EmptyState("無", stringResource(R.string.empty_rejected_title), stringResource(R.string.empty_rejected_body))
                    else -> EmptyState("掘", stringResource(R.string.empty_mined_title), stringResource(R.string.empty_mined_body))
                }
            }
        } else {
            GlassCard(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).weight(1f, fill = false),
                padding = PaddingValues(8.dp)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(shown, key = { it.key + it.id }) { w ->
                        WordRow(
                            w = w,
                            highlight = mode == 2 && w.key in deckKeys,
                            actionIcon = if (mode == 1) Ic.undo else Ic.delete,
                            actionLabel = stringResource(
                                when (mode) { 0 -> R.string.remove_from_deck; 1 -> R.string.restore; else -> R.string.delete }
                            )
                        ) {
                            when (mode) {
                                0 -> {
                                    AcceptedStore.remove(ctx, w.key); DailyWordManager.refresh(ctx)
                                    scope.launch {
                                        val r = snackbar.showSnackbar(
                                            ctx.getString(R.string.snack_removed, w.word), ctx.getString(R.string.undo),
                                            duration = SnackbarDuration.Short
                                        )
                                        if (r == SnackbarResult.ActionPerformed) { AcceptedStore.add(ctx, w); DailyWordManager.refresh(ctx) }
                                    }
                                }
                                1 -> {
                                    RejectedStore.remove(ctx, w.key); DailyWordManager.refresh(ctx)
                                    scope.launch {
                                        val r = snackbar.showSnackbar(
                                            ctx.getString(R.string.snack_restored, w.word), ctx.getString(R.string.undo),
                                            duration = SnackbarDuration.Short
                                        )
                                        if (r == SnackbarResult.ActionPerformed) { RejectedStore.add(ctx, w); DailyWordManager.refresh(ctx) }
                                    }
                                }
                                else -> { MinedStore.remove(ctx, w.id); DailyWordManager.refresh(ctx) }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier, hint: Int = R.string.search_hint) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.Glass)
            .border(1.dp, Ink.GlassStroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Ic.search, null, Modifier.size(18.dp), tint = Ink.Muted)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(stringResource(hint), color = Ink.Faint, fontSize = 15.sp)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = Ink.Text, fontSize = 15.sp).merge(JapaneseText),
                cursorBrush = SolidColor(Ink.Text),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


private val dateIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val dateOut = SimpleDateFormat("EEE, d MMM", Locale.getDefault())


@Composable
fun WordRow(w: Word, highlight: Boolean, actionIcon: ImageVector, actionLabel: String, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.Glass)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KanjiTile(w.word, highlight = highlight)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val date = w.addedOn.takeIf { it.isNotBlank() }?.let { runCatching { dateOut.format(dateIn.parse(it)!!) }.getOrNull() }
            Kicker(listOfNotNull(date, w.reading).joinToString("  ·  "))
            Spacer(Modifier.height(2.dp))
            Text(
                w.meaning, style = MaterialTheme.typography.bodyLarge, color = Ink.Text,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        GlassIconButton(actionIcon, actionLabel, size = 34.dp, tint = Ink.Muted, onClick = onAction)
    }
}


@Composable
private fun DictionaryPane(tick: Int, snackbar: SnackbarHostState, deckKeys: Set<String>) {
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Word>>(emptyList()) }
    var counts by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var open by remember { mutableStateOf<Word?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        counts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { Dictionary.counts(ctx) }.getOrNull() }
    }
    androidx.compose.runtime.LaunchedEffect(query) {
        if (query.isBlank()) { results = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(220)
        results = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { Dictionary.search(ctx, query) }.getOrDefault(emptyList())
        }
    }

    SearchField(query, { query = it }, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), hint = R.string.dict_search_hint)
    if (query.isBlank()) {
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            EmptyState(
                "辞",
                counts?.let { stringResource(R.string.dict_empty_title, it.first, it.second) } ?: stringResource(R.string.dict_loading),
                stringResource(R.string.dict_empty_body)
            )
        }
    } else if (results.isEmpty()) {
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            EmptyState("無", stringResource(R.string.dict_none_title), stringResource(R.string.dict_none_body))
        }
    } else {
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = PaddingValues(8.dp)) {
            LazyColumn(contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(results, key = { it.id }) { w -> DictRow(w, w.key in deckKeys) { open = w } }
            }
        }
    }

    open?.let { w -> WordSheet(w, w.key in deckKeys, snackbar) { open = null } }
}

@Composable
fun DictRow(w: Word, inDeck: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ink.Glass)
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KanjiTile(w.word, highlight = inDeck)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Kicker(listOf(w.reading, w.info.substringBefore(" · ")).filter { it.isNotBlank() }.joinToString("  ·  "))
            Spacer(Modifier.height(2.dp))
            Text(w.meaning, style = MaterialTheme.typography.bodyLarge, color = Ink.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (inDeck) Icon(Ic.check, null, Modifier.size(18.dp), tint = Ink.Good)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun WordSheet(w: Word, inDeck: Boolean, snackbar: SnackbarHostState, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val speaker = remember { Speaker(ctx) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink.BgMid, contentColor = Ink.Text
    ) {
        WordDetail(w, inDeck,
            onSpeak = { speaker.speak(w.reading.ifBlank { w.word }) },
            onAdd = {
                AcceptedStore.add(ctx, w); RejectedStore.remove(ctx, w.key); DailyWordManager.refresh(ctx)
                onDismiss(); scope.launch { snackbar.showSnackbar(ctx.getString(R.string.snack_accepted)) }
            },
            onMine = {
                val n = MinedStore.add(ctx, listOf(w.copy(source = "mined")))
                DailyWordManager.refresh(ctx); onDismiss()
                scope.launch { snackbar.showSnackbar(ctx.getString(if (n > 0) R.string.snack_word_added else R.string.snack_word_exists, w.word)) }
            })
    }
}

//cc by needs credit so every sentence links back to its page
@Composable
private fun TatoebaLink(id: String) {
    if (id.isBlank()) return
    val uri = LocalUriHandler.current
    Text(
        stringResource(R.string.tatoeba_link, id),
        Modifier.padding(top = 4.dp).clickable { uri.openUri("https://tatoeba.org/sentences/show/$id") },
        style = MaterialTheme.typography.labelSmall, color = Ink.Faint, textDecoration = TextDecoration.Underline
    )
}


@Composable
fun WordDetail(w: Word, inDeck: Boolean, onSpeak: () -> Unit, onAdd: () -> Unit, onMine: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp).navigationBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KanjiTile(w.word, size = 92.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(w.reading, style = MaterialTheme.typography.titleLarge.merge(JapaneseText), color = Ink.Muted)
                Text(w.meaning, style = MaterialTheme.typography.titleMedium, color = Ink.Text, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            GlassIconButton(Ic.volume, stringResource(R.string.cd_speak), size = 36.dp, onClick = onSpeak)
        }
        if (w.info.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(w.info, style = MaterialTheme.typography.bodySmall.merge(JapaneseText), color = Ink.Muted)
        }
        if (w.example.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Kicker(stringResource(R.string.example))
            Spacer(Modifier.height(6.dp))
            Text(
                if (w.exampleReading.isNotBlank()) "${w.example}（${w.exampleReading}）" else w.example,
                style = MaterialTheme.typography.titleMedium.merge(JapaneseText), color = Ink.Text
            )
            if (w.exampleMeaning.isNotBlank()) Text(w.exampleMeaning, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
            TatoebaLink(w.exampleId)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WhitePill(
                stringResource(if (inDeck) R.string.action_in_deck else R.string.add_to_deck),
                Modifier.weight(1f), enabled = !inDeck, icon = Ic.check, onClick = onAdd
            )
            GhostPill(stringResource(R.string.save_to_mined), icon = Ic.book, onClick = onMine)
        }
    }
}

@Composable
fun SettingsScreen(tick: Int, snackbar: SnackbarHostState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var perDay by remember { mutableIntStateOf(Prefs.wordsPerDay(ctx)) }
    var newPerDay by remember { mutableIntStateOf(Prefs.newPerDay(ctx)) }
    var mode by remember { mutableStateOf(Prefs.displayMode(ctx)) }
    var source by remember { mutableStateOf(Prefs.source(ctx)) }
    var widgetBg by remember { mutableStateOf(Prefs.widgetBg(ctx)) }
    var lockNotif by remember { mutableStateOf(Prefs.lockNotif(ctx)) }
    var nudge by remember { mutableFloatStateOf(Prefs.notifNudge(ctx).toFloat()) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var goal by remember { mutableIntStateOf(Prefs.dailyGoal(ctx)) }
    var sfx by remember { mutableStateOf(Prefs.sfx(ctx)) }
    var remindDaily by remember { mutableStateOf(Prefs.remindDaily(ctx)) }
    var remindStreak by remember { mutableStateOf(Prefs.remindStreak(ctx)) }
    var remindAt by remember { mutableIntStateOf(Prefs.remindMinutes(ctx)) }
    var streakAt by remember { mutableIntStateOf(Prefs.streakMinutes(ctx)) }
    var picking by remember { mutableStateOf<String?>(null) }
    val dictCounts = remember { runCatching { Dictionary.counts(ctx) }.getOrDefault(0 to 0) }
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty() }
    var reading by remember { mutableStateOf<String?>(null) }

    fun toast(ok: Boolean, good: Int, bad: Int) =
        scope.launch { snackbar.showSnackbar(ctx.getString(if (ok) good else bad)) }

    val backupOut = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) toast(Backup.write(ctx, uri), R.string.backup_saved, R.string.backup_failed)
    }
    val backupIn = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = Backup.restore(ctx, uri)
            if (ok) {
                perDay = Prefs.wordsPerDay(ctx); newPerDay = Prefs.newPerDay(ctx); mode = Prefs.displayMode(ctx)
                source = Prefs.source(ctx); widgetBg = Prefs.widgetBg(ctx); lockNotif = Prefs.lockNotif(ctx)
                DailyWordManager.reselectToday(ctx)
            }
            toast(ok, R.string.restore_done, R.string.restore_failed)
        }
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(BarClearance)) {
        ScreenHeader(kicker = stringResource(R.string.settings_kicker), title = stringResource(R.string.settings))

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Section(stringResource(R.string.section_daily)) {
                StepperRow(stringResource(R.string.words_per_day), stringResource(R.string.words_per_day_sub), perDay, 1..Prefs.MAX_WORDS_PER_DAY) {
                    perDay = it; Prefs.setWordsPerDay(ctx, it); DailyWordManager.resizeToday(ctx)
                }
                Divider()
                Choice(
                    stringResource(R.string.mode_label),
                    listOf(Prefs.MODE_WORD to R.string.mode_word, Prefs.MODE_KANJI to R.string.mode_kanji, Prefs.MODE_BOTH to R.string.mode_both),
                    mode
                ) { mode = it; Prefs.setDisplayMode(ctx, it); DailyWordManager.reselectToday(ctx) }
                Divider()
                Choice(
                    stringResource(R.string.source_label),
                    listOf(Prefs.SOURCE_DICT to R.string.source_dict, Prefs.SOURCE_MINED to R.string.source_mined, Prefs.SOURCE_BOTH to R.string.source_both),
                    source
                ) { source = it; Prefs.setSource(ctx, it); DailyWordManager.reselectToday(ctx) }
            }

            Section(stringResource(R.string.section_study)) {
                StepperRow(stringResource(R.string.new_per_day), stringResource(R.string.new_per_day_sub), newPerDay, 0..100) {
                    newPerDay = it; Prefs.setNewPerDay(ctx, it); DailyWordManager.refresh(ctx)
                }
                Divider()
                StepperRow(stringResource(R.string.daily_goal), stringResource(R.string.daily_goal_sub), goal, 1..200) {
                    goal = it; Prefs.setDailyGoal(ctx, it); DailyWordManager.refresh(ctx)
                }
                Divider()
                ToggleRow(stringResource(R.string.sfx_label), stringResource(R.string.sfx_sub), sfx) {
                    sfx = it; Prefs.setSfx(ctx, it); if (it) Sfx.play(ctx, 2)
                }
            }

            Section(stringResource(R.string.section_reminders)) {
                ReminderRow(stringResource(R.string.remind_daily_label), stringResource(R.string.remind_daily_sub), remindDaily, remindAt,
                    onToggle = { remindDaily = it; Prefs.setRemindDaily(ctx, it); Reminders.scheduleAll(ctx) },
                    onTime = { picking = "daily" })
                Divider()
                ReminderRow(stringResource(R.string.remind_streak_label), stringResource(R.string.remind_streak_sub), remindStreak, streakAt,
                    onToggle = { remindStreak = it; Prefs.setRemindStreak(ctx, it); Reminders.scheduleAll(ctx) },
                    onTime = { picking = "streak" })
            }

            Section(stringResource(R.string.section_drive)) {
                DriveRow(tick)
            }

            Section(stringResource(R.string.section_lock)) {
                Choice(
                    stringResource(R.string.widget_bg_label),
                    listOf(Prefs.BG_SOLID to R.string.bg_solid, Prefs.BG_SEMI to R.string.bg_semi, Prefs.BG_CLEAR to R.string.bg_clear),
                    widgetBg
                ) { widgetBg = it; Prefs.setWidgetBg(ctx, it); KanjiWidgetProvider.updateAll(ctx) }
                Divider()
                ToggleRow(stringResource(R.string.lock_notif_label), stringResource(R.string.lock_notif_hint), lockNotif) {
                    lockNotif = it; Prefs.setLockNotif(ctx, it)
                    if (!it) LockScreenNotifier.cancel(ctx)
                    DailyWordManager.refresh(ctx)
                }
                if (lockNotif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Divider()
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.nudge_title), style = MaterialTheme.typography.titleMedium, color = Ink.Text)
                                Text(stringResource(R.string.nudge_sub, nudge.roundToInt()), style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                            }
                            TextButton(onClick = {
                                nudge = Prefs.DEFAULT_NUDGE_DP.toFloat(); Prefs.setNotifNudge(ctx, Prefs.DEFAULT_NUDGE_DP); LockScreenNotifier.refresh(ctx)
                            }) { Text(stringResource(R.string.reset), color = Ink.Text) }
                        }
                        Slider(
                            value = nudge, onValueChange = { nudge = it },
                            onValueChangeFinished = { Prefs.setNotifNudge(ctx, nudge.roundToInt()); LockScreenNotifier.refresh(ctx) },
                            valueRange = 0f..96f, steps = 23,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Ink.GlassHigh,
                                activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent
                            )
                        )
                    }
                }
                Divider()
                LinkRow(Ic.widgets, stringResource(R.string.add_widget), stringResource(R.string.add_widget_sub)) {
                    if (!KanjiWidgetProvider.requestPin(ctx)) Toast.makeText(ctx, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
                }
            }

            Section(stringResource(R.string.section_data)) {
                LinkRow(Ic.share, stringResource(R.string.export_csv), stringResource(R.string.export_csv_sub)) { Exporter.shareCsv(ctx) }
                Divider()
                LinkRow(Ic.cards, stringResource(R.string.export_anki), stringResource(R.string.export_anki_sub)) { Exporter.shareAnki(ctx) }
                Divider()
                LinkRow(Ic.download, stringResource(R.string.backup), stringResource(R.string.backup_sub)) {
                    backupOut.launch("KanjiLock-backup-${ActivityLog.dayKey()}.json")
                }
                Divider()
                LinkRow(Ic.upload, stringResource(R.string.restore_backup), stringResource(R.string.restore_sub)) {
                    backupIn.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
                }
                Divider()
                LinkRow(Ic.delete, stringResource(R.string.clear_accepted), null, danger = true) { confirm = "accepted" }
                Divider()
                LinkRow(Ic.delete, stringResource(R.string.clear_rejected), null, danger = true) { confirm = "rejected" }
            }

            Section(stringResource(R.string.section_about)) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text(stringResource(R.string.about_line, version, dictCounts.first, dictCounts.second), style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.about_credits), style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.about_csv), style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                }
                Divider()
                LinkRow(Ic.lock, stringResource(R.string.privacy_policy), stringResource(R.string.privacy_sub)) { reading = "privacy" }
                Divider()
                LinkRow(Ic.book, stringResource(R.string.licenses), stringResource(R.string.licenses_sub)) { reading = "licenses" }
            }
        }
    }

    when (reading) {
        "privacy" -> TextDialog(stringResource(R.string.privacy_policy), stringResource(R.string.privacy_body)) { reading = null }
        "licenses" -> {
            val text = remember { runCatching { ctx.assets.open("licenses.txt").bufferedReader().use { it.readText() } }.getOrDefault("") }
            TextDialog(stringResource(R.string.licenses), text) { reading = null }
        }
    }

    picking?.let { which ->
        val initial = if (which == "daily") remindAt else streakAt
        TimeDialog(initial, onDismiss = { picking = null }) { m ->
            if (which == "daily") { remindAt = m; Prefs.setRemindMinutes(ctx, m) } else { streakAt = m; Prefs.setStreakMinutes(ctx, m) }
            Reminders.scheduleAll(ctx); picking = null
        }
    }

    confirm?.let { which ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Ink.BgMid,
            title = { Text(stringResource(if (which == "accepted") R.string.clear_accepted else R.string.clear_rejected)) },
            text = { Text(stringResource(if (which == "accepted") R.string.confirm_clear_accepted else R.string.confirm_clear_rejected), color = Ink.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    if (which == "accepted") AcceptedStore.clear(ctx) else RejectedStore.clear(ctx)
                    DailyWordManager.refresh(ctx)
                    confirm = null
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.snack_cleared)) }
                }) { Text(stringResource(R.string.clear), color = Ink.Reject) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel), color = Ink.Text) } }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Kicker(title, Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp))
    GlassCard(Modifier.fillMaxWidth(), padding = PaddingValues(0.dp), content = content)
}

@Composable
private fun Divider() = Box(Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(1.dp).background(Ink.GlassStroke))

@Composable
private fun StepperRow(title: String, sub: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.Text)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        }
        Spacer(Modifier.width(10.dp))
        NumberStepper(value, range, onChange)
    }
}

@Composable
private fun Choice(title: String, options: List<Pair<String, Int>>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.Text)
        Spacer(Modifier.height(10.dp))
        PillTabs(
            options = options.map { stringResource(it.second) },
            selected = options.indexOfFirst { it.first == selected }.coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        ) { onSelect(options[it].first) }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.Text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color.White, checkedThumbColor = Ink.OnWhite,
                uncheckedTrackColor = Ink.Glass, uncheckedThumbColor = Ink.Muted, uncheckedBorderColor = Ink.GlassStroke
            )
        )
    }
}


@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String?, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (danger) Ink.Reject else Ink.Muted)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (danger) Ink.Reject else Ink.Text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        }
        Icon(Ic.chevronRight, null, Modifier.size(18.dp), tint = Ink.Faint)
    }
}

private fun timeLabel(m: Int): String {
    val c = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, m / 60); set(java.util.Calendar.MINUTE, m % 60) }
    return java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(c.time)
}


@Composable
private fun ReminderRow(title: String, sub: String, on: Boolean, minutes: Int, onToggle: (Boolean) -> Unit, onTime: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.Text)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            Spacer(Modifier.height(8.dp))
            Text(
                timeLabel(minutes),
                style = MaterialTheme.typography.labelLarge,
                color = if (on) Ink.Text else Ink.Faint,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(Ink.Glass)
                    .clickable(enabled = on, onClick = onTime)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = on, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color.White, checkedThumbColor = Ink.OnWhite,
                uncheckedTrackColor = Ink.Glass, uncheckedThumbColor = Ink.Muted, uncheckedBorderColor = Ink.GlassStroke
            )
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val state = androidx.compose.material3.rememberTimePickerState(initial / 60, initial % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ink.BgMid,
        text = {
            androidx.compose.material3.TimePicker(
                state = state,
                colors = androidx.compose.material3.TimePickerDefaults.colors(
                    clockDialColor = Ink.Glass, selectorColor = Color.White, clockDialSelectedContentColor = Ink.OnWhite,
                    timeSelectorSelectedContainerColor = Color.White, timeSelectorSelectedContentColor = Ink.OnWhite,
                    timeSelectorUnselectedContainerColor = Ink.Glass, periodSelectorSelectedContainerColor = Color.White,
                    periodSelectorSelectedContentColor = Ink.OnWhite
                )
            )
        },
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.save), color = Ink.Text) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Ink.Muted) } }
    )
}


@Composable
private fun DriveRow(tick: Int) {
    val ctx = LocalContext.current
    val activity = ctx as android.app.Activity
    val scope = rememberCoroutineScope()
    var linked by remember { mutableStateOf(Prefs.driveLinked(ctx)) }
    var askDelete by remember { mutableStateOf(false) }
    val state = remember(tick, linked) { Prefs.driveState(ctx) }
    val last = remember(tick, linked) { Prefs.lastSyncAt(ctx) }

    fun onAuthorized(token: String?) {
        if (token == null) { Toast.makeText(ctx, R.string.drive_link_failed, Toast.LENGTH_LONG).show(); return }
        DriveSync.setToken(token); Prefs.setDriveLinked(ctx, true); linked = true
        Prefs.setDriveState(ctx, "syncing"); DriveSync.syncNow(ctx)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        onAuthorized(DriveSync.resultFromIntent(activity, res.data)?.accessToken)
    }
    fun link() = scope.launch {
        val r = DriveSync.authorize(activity)
        when {
            r == null -> Toast.makeText(ctx, R.string.drive_link_failed, Toast.LENGTH_LONG).show()
            r.hasResolution() -> r.pendingIntent?.let { launcher.launch(IntentSenderRequest.Builder(it.intentSender).build()) }
            else -> onAuthorized(r.accessToken)
        }
    }

    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ic.cloud, null, Modifier.size(20.dp), tint = if (linked) Ink.Good else Ink.Muted)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (linked) R.string.drive_linked else R.string.drive_not_linked), style = MaterialTheme.typography.titleMedium, color = Ink.Text)
                Text(
                    when {
                        !linked -> stringResource(R.string.drive_sub)
                        state == "reconnect" -> stringResource(R.string.drive_reconnect)
                        state == "syncing" -> stringResource(R.string.drive_syncing)
                        state.startsWith("error:") -> stringResource(R.string.drive_error, state.removePrefix("error:"))
                        last > 0 -> stringResource(R.string.drive_last, android.text.format.DateUtils.getRelativeTimeSpanString(last).toString())
                        else -> stringResource(R.string.drive_sub)
                    },
                    style = MaterialTheme.typography.bodySmall, color = Ink.Muted
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!linked || state == "reconnect") WhitePill(stringResource(if (linked) R.string.drive_reconnect_btn else R.string.drive_link), icon = Ic.cloud) { link() }
            if (linked && state != "reconnect") WhitePill(stringResource(R.string.drive_sync_now)) {
                Prefs.setDriveState(ctx, "syncing"); DailyWordManager.refresh(ctx); scope.launch { link() }
            }
            if (linked) GhostPill(stringResource(R.string.drive_unlink)) { DriveSync.unlink(ctx); linked = false }
        }
        if (linked && state != "reconnect") {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { askDelete = true }, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.drive_delete), style = MaterialTheme.typography.labelLarge, color = Ink.Reject)
            }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = Ink.BgMid,
            title = { Text(stringResource(R.string.drive_delete_title)) },
            text = { Text(stringResource(R.string.drive_delete_body), color = Ink.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    askDelete = false
                    scope.launch {
                        val ok = DriveSync.deleteRemote(ctx)
                        if (ok) linked = false
                        Toast.makeText(ctx, if (ok) R.string.drive_deleted else R.string.drive_delete_failed, Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.delete), color = Ink.Reject) }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text(stringResource(R.string.cancel), color = Ink.Text) } }
        )
    }
}

@Composable
fun AddWordForm(onSave: (Word) -> Unit) {
    var word by rememberSaveable { mutableStateOf("") }
    var reading by rememberSaveable { mutableStateOf("") }
    var meaning by rememberSaveable { mutableStateOf("") }
    var example by rememberSaveable { mutableStateOf("") }
    var exampleMeaning by rememberSaveable { mutableStateOf("") }
    val readingError = reading.isNotBlank() && !isKana(reading)
    val canSave = word.isNotBlank() && isKana(reading) && meaning.isNotBlank()

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Kicker(stringResource(R.string.tab_mined))
        Text(stringResource(R.string.add_word_title), style = MaterialTheme.typography.headlineMedium, color = Ink.Text)
        Text(stringResource(R.string.add_word_sub), style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
        Spacer(Modifier.height(16.dp))
        Field(word, { word = it }, R.string.field_word, R.string.field_word_hint, ja = true)
        Field(
            reading, { reading = it }, R.string.field_reading, R.string.field_reading_hint, ja = true,
            error = if (readingError) stringResource(R.string.err_kana_only) else null
        )
        Field(meaning, { meaning = it }, R.string.field_meaning, R.string.field_meaning_hint)
        Field(example, { example = it }, R.string.field_example, R.string.field_example_hint, ja = true)
        Field(exampleMeaning, { exampleMeaning = it }, R.string.field_example_meaning, R.string.field_example_meaning_hint, last = true)
        Spacer(Modifier.height(10.dp))
        WhitePill(stringResource(R.string.save), Modifier.fillMaxWidth(), enabled = canSave) {
            onSave(
                Word(
                    id = "", word = word.trim(), reading = reading.trim(), meaning = meaning.trim(),
                    example = example.trim(), exampleMeaning = exampleMeaning.trim(),
                    type = if (word.trim().length == 1) "kanji" else "word", source = "mined"
                )
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}


@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    hint: Int,
    ja: Boolean = false,
    error: String? = null,
    last: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        placeholder = { Text(stringResource(hint), color = Ink.Faint) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink.Muted,
            unfocusedBorderColor = Ink.GlassStroke,
            focusedContainerColor = Ink.Glass,
            unfocusedContainerColor = Ink.Glass,
            focusedLabelColor = Ink.Text,
            unfocusedLabelColor = Ink.Muted,
            cursorColor = Ink.Text
        ),
        textStyle = if (ja) MaterialTheme.typography.bodyLarge.merge(JapaneseText) else MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = if (last) ImeAction.Done else ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}
