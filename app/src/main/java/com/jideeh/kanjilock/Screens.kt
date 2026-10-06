package com.jideeh.kanjilock

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.RowScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.SideEffect
import android.view.WindowManager
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
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
    //swipe left right to go thru todays words
    var dir by remember { mutableIntStateOf(0) }
    val step: (Int) -> Unit = { d -> dir = d; onStep(d) }
    val swipe = if (today.status == Status.ACTIVE && today.total > 1) Modifier.pointerInput(today.total) {
        var drag = 0f
        detectHorizontalDragGestures(
            onDragEnd = {
                val min = 56.dp.toPx()
                if (drag < -min) step(+1) else if (drag > min) step(-1)
                drag = 0f
            },
            onDragCancel = { drag = 0f }
        ) { change, dx -> drag += dx; change.consume() }
    } else Modifier
    GlassCard(Modifier.fillMaxWidth().then(swipe)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (today.total > 1) stringResource(R.string.words_today, today.total) else stringResource(R.string.word_of_the_day),
                style = MaterialTheme.typography.titleMedium,
                color = Ink.Text,
                modifier = Modifier.weight(1f)
            )
            if (today.status == Status.ACTIVE && today.total > 1) {
                GlassIconButton(Ic.chevronLeft, stringResource(R.string.cd_prev), size = 34.dp) { step(-1) }
                Text(
                    "${today.index + 1}/${today.total}",
                    style = MaterialTheme.typography.labelLarge, color = Ink.Muted,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                GlassIconButton(Ic.chevronRight, stringResource(R.string.cd_next), size = 34.dp) { step(+1) }
                Spacer(Modifier.width(8.dp))
            }
            val w = today.word
            if (w != null) GlassIconButton(Ic.volume, stringResource(R.string.cd_speak), size = 34.dp) { onSpeak(w) }
        }
        Spacer(Modifier.height(14.dp))

        AnimatedContent(
            targetState = today.word?.key to today.status,
            transitionSpec = {
                if (dir != 0) {
                    (slideInHorizontally(tween(240)) { it * dir / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it * dir / 3 } + fadeOut(tween(140)))
                } else {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220))) togetherWith fadeOut(tween(120))
                }
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
                    .background(Ink.GlassStroke)
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
    var practice by rememberSaveable { mutableStateOf(Prefs.practice(ctx)) }
    var focusOpen by rememberSaveable { mutableStateOf(false) }
    var ankiSheet by remember { mutableStateOf<Deck?>(null) }
    var ankiSheetOpen by remember { mutableStateOf(false) }

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
        if (!AnkiDroid.installed(ctx)) { ankiSheet = forDeck; ankiSheetOpen = true; return }
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
            onLinkAnkiDroid = { ankiSheet = null; ankiSheetOpen = true },
            version = tick + local
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
                GhostPill(stringResource(R.string.sync_ankiweb)) { ankiSheet = deck; ankiSheetOpen = true }
            }
            if (deck.isLinked) {
                GhostPill(stringResource(R.string.btn_sync)) {
                    Toast.makeText(ctx, if (AnkiDroid.requestSync(ctx)) R.string.ankiweb_sync_sent else R.string.ankiweb_sync_failed, Toast.LENGTH_LONG).show()
                }
            }
            if (deck.id != DeckStore.ACCEPTED) {
                GlassIconButton(Ic.delete, stringResource(R.string.delete), size = 34.dp, tint = Ink.Muted) { confirmDelete = deck }
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeChip(stringResource(R.string.practice_mode), practice, Modifier.weight(1f)) {
                practice = !practice; Prefs.setPractice(ctx, practice)
            }
            GhostPill(stringResource(R.string.focus_mode), icon = Ic.fullscreen) { focusOpen = true }
        }
        if (practice) {
            Text(
                stringResource(R.string.practice_sub), style = MaterialTheme.typography.labelMedium, color = Ink.Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(12.dp))

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val close = { focusOpen = false }
            when {
                practice -> PracticeSession(deck, focusOpen, close)
                deck.isLinked -> AnkiDroidSession(deck, tick, focusOpen, close)
                else -> LocalSession(deck, tick, focusOpen, close)
            }
            if (!deck.isLinked && !practice) InsightsCard(deck.id, tick)
        }
    }

    if (ankiSheetOpen) {
        AnkiWebSheet(
            forDeck = ankiSheet,
            onDismiss = { ankiSheetOpen = false },
            onAllow = { withAnkiDroid(ankiSheet) { } },
            onPick = { ankiSheetOpen = false; withAnkiDroid(null) { showLinkPicker = true } },
            onLinkThis = { d -> ankiSheetOpen = false; withAnkiDroid(d) { linkByName(ctx, d) { local++ } } }
        )
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

@Composable
private fun ModeChip(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .clip(shape)
            .background(if (on) Ink.Good.copy(alpha = 0.16f) else Ink.Glass)
            .border(1.dp, if (on) Ink.Good.copy(alpha = 0.5f) else Ink.GlassStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Ink.Text, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(width = 34.dp, height = 20.dp).clip(CircleShape)
                .background(if (on) Ink.Good else Ink.GlassHigh),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.padding(2.dp).size(16.dp).clip(CircleShape).background(Color.White))
        }
    }
}

//ankiweb only lets official anki apps log in so everything goes thru ankidroid
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnkiWebSheet(
    forDeck: Deck?,
    onDismiss: () -> Unit,
    onAllow: () -> Unit,
    onPick: () -> Unit,
    onLinkThis: (Deck) -> Unit
) {
    val ctx = LocalContext.current
    var check by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { check++; onPauseOrDispose { } }
    val installed = remember(check) { AnkiDroid.installed(ctx) }
    val allowed = remember(check) { installed && AnkiDroid.hasPermission(ctx) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Ink.BgMid, contentColor = Ink.Text) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 28.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.ankiweb_title), style = MaterialTheme.typography.headlineSmall, color = Ink.Text)
            Text(stringResource(R.string.ankiweb_body), style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
            Step(1, installed, stringResource(if (installed) R.string.ankiweb_step_installed else R.string.ankiweb_step_install), null) {
                if (!installed) GhostPill(stringResource(R.string.btn_install)) { openStore(ctx) }
            }
            Step(2, false, stringResource(R.string.ankiweb_step_sync), stringResource(R.string.ankiweb_step_sync_sub)) {
                if (installed) {
                    GhostPill(stringResource(R.string.btn_open)) { AnkiDroid.open(ctx) }
                    Spacer(Modifier.width(6.dp))
                    GhostPill(stringResource(R.string.btn_sync)) {
                        Toast.makeText(ctx, if (AnkiDroid.requestSync(ctx)) R.string.ankiweb_sync_sent else R.string.ankiweb_sync_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
            Step(3, allowed, stringResource(if (allowed) R.string.ankiweb_step_allowed else R.string.ankiweb_step_allow), null) {
                if (installed && !allowed) GhostPill(stringResource(R.string.btn_allow), onClick = onAllow)
            }
            Step(
                4, false,
                if (forDeck != null) stringResource(R.string.ankiweb_step_link_this, forDeck.name) else stringResource(R.string.ankiweb_step_pick),
                stringResource(R.string.ankiweb_step_pick_sub)
            ) {
                if (installed && allowed) {
                    if (forDeck != null) WhitePill(stringResource(R.string.btn_link)) { onLinkThis(forDeck) }
                    else WhitePill(stringResource(R.string.btn_pick), onClick = onPick)
                }
            }
        }
    }
}

@Composable
private fun Step(n: Int, done: Boolean, title: String, sub: String?, actions: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (done) Ink.Good else Ink.GlassHigh),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Ic.check, null, Modifier.size(16.dp), tint = Ink.OnWhite)
            else Text("$n", style = MaterialTheme.typography.labelLarge, color = Ink.Text)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (done) Ink.Muted else Ink.Text)
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelMedium, color = Ink.Faint)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

private fun openStore(ctx: android.content.Context) {
    val id = AnkiDroid.PACKAGE
    val ok = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess ||
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    if (!ok) Toast.makeText(ctx, R.string.ankidroid_store_missing, Toast.LENGTH_LONG).show()
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
    onLinkAnkiDroid: () -> Unit,
    version: Int = 0
) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(decks, key = { it.id }) { d ->
            val on = d.id == selected
            //recount after every answer or the tile keeps showing the old new card number
            val counts = remember(d.id, d.cardCount, version) {
                if (d.isLinked) null else Study.queue(ctx, d.id).let { Triple(it.newLeft, it.learning, it.review) }
            }
            Column(
                Modifier
                    .width(164.dp)
                    .height(116.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) Ink.GlassHigh else Ink.Glass)
                    .border(if (on) 1.5.dp else 1.dp, if (on) Ink.Pill.copy(alpha = 0.85f) else Ink.GlassStroke, RoundedCornerShape(18.dp))
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
private fun LocalSession(deck: Deck, tick: Int, focusOpen: Boolean, onCloseFocus: () -> Unit) {
    val ctx = LocalContext.current
    var bump by remember { mutableIntStateOf(0) }
    val q = remember(tick, bump, deck.id) { Study.queue(ctx, deck.id) }
    val history = remember(deck.id) { mutableStateListOf<Study.Undo>() }
    var forced by remember(deck.id) { mutableStateOf<Study.Undo?>(null) }

    val card = forced?.card ?: q.card
    val srs = forced?.before ?: q.srs
    val stateLabel = srs?.let {
        stringResource(when (it.state) { SrsCard.NEW -> R.string.count_new; SrsCard.REVIEW -> R.string.count_review; else -> R.string.count_learning })
    }.orEmpty()
    val labels = remember(srs) {
        srs?.let { s -> Scheduler.preview(s, System.currentTimeMillis()).let { p -> Grade.entries.map { Scheduler.label(p.getValue(it)) } } } ?: emptyList()
    }

    fun grade(g: Int) {
        val c = card ?: return
        val s = srs ?: return
        history.add(Study.Undo(deck.id, c, s, ActivityLog.dayKey()))
        if (history.size > 30) history.removeAt(0)
        Sfx.play(ctx, g)
        Study.answer(ctx, deck.id, s, Grade.entries[g])
        forced = null
        bump++
    }
    //puts the last answer back and shows that card again
    val previous: (() -> Unit)? = if (history.isEmpty()) null else {
        {
            val u = history.removeAt(history.lastIndex)
            Study.undo(ctx, u)
            forced = u
            bump++
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CountTile(q.newLeft, stringResource(R.string.count_new), Ink.Easy, Modifier.weight(1f))
        CountTile(q.learning, stringResource(R.string.count_learning), Ink.Hard, Modifier.weight(1f))
        CountTile(q.review, stringResource(R.string.count_review), Ink.Good, Modifier.weight(1f))
    }
    val empty: @Composable () -> Unit = {
        if (q.deckSize == 0) EmptyState("覚", stringResource(R.string.study_empty_title), stringResource(R.string.study_empty_body))
        else EmptyState(
            "済", stringResource(R.string.study_done_title),
            q.nextDue?.let { stringResource(R.string.study_done_next, Scheduler.label(it - System.currentTimeMillis())) }
                ?: stringResource(R.string.study_done_body)
        )
    }
    if (card == null || srs == null) {
        GlassCard(Modifier.fillMaxWidth()) { empty() }
        if (previous != null) GhostPill(stringResource(R.string.previous), icon = Ic.chevronLeft, onClick = previous)
    } else {
        AnimatedContent(
            targetState = card to srs,
            transitionSpec = { (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { it / 8 }) togetherWith fadeOut(tween(100)) },
            contentKey = { it.first.id + it.second.reps + it.second.due },
            label = "card"
        ) { (c, _) ->
            Flashcard(c, stateLabel, labels, 4, onPrevious = previous) { grade(it) }
        }
    }

    if (focusOpen) {
        val st = card?.let { rememberAnswer(it) }
        FocusModal(
            title = deck.name, sub = stateLabel, onClose = onCloseFocus, onPrevious = previous,
            card = card, st = st,
            hint = stringResource(R.string.focus_hint),
            onSubmit = { if (st != null) { if (st.revealed) grade(2) else revealAnswer(ctx, st) } },
            actions = {
                GradeRow(labels, 4, st?.revealed == true, Modifier.fillMaxWidth()) { g ->
                    if (st?.revealed == true) grade(g) else st?.let { revealAnswer(ctx, it) }
                }
            },
            empty = empty
        )
    }
}

@Composable
private fun AnkiDroidSession(deck: Deck, tick: Int, focusOpen: Boolean, onCloseFocus: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bump by remember { mutableIntStateOf(0) }
    var shownAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var last by remember(deck.id) { mutableStateOf<AnkiDroid.AdCard?>(null) }
    var viewing by remember(deck.id) { mutableStateOf(false) }
    val state by produceState<Result<Pair<AnkiDroid.AdDeck?, AnkiDroid.AdCard?>>?>(null, deck.id, bump, tick) {
        value = withContext(Dispatchers.IO) {
            runCatching { AnkiDroid.deck(ctx, deck.ankiDroidDeckId) to AnkiDroid.next(ctx, deck.ankiDroidDeckId) }
        }
        shownAt = System.currentTimeMillis()
    }

    val r = state
    val (info, next) = r?.getOrNull() ?: (null to null)
    val four = (next?.buttons ?: 4) >= 4
    val labels = next?.let { n -> if (four) n.times.take(4) else listOf(n.times.getOrElse(0) { "" }, "", n.times.getOrElse(1) { "" }, n.times.getOrElse(2) { "" }) } ?: emptyList()

    fun grade(g: Int) {
        val n = next ?: return
        Sfx.play(ctx, g)
        val ease = if (four) g + 1 else when (g) { 0 -> 1; 3 -> 3; else -> 2 }
        val taken = System.currentTimeMillis() - shownAt
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { AnkiDroid.answer(ctx, n, ease, taken) }.getOrDefault(false) }
            if (ok) { Study.logReview(ctx, g + 1, SrsCard.REVIEW, deck.id); last = n }
            else Toast.makeText(ctx, R.string.ankidroid_answer_failed, Toast.LENGTH_SHORT).show()
            bump++
        }
    }
    // ankidroid has no undo in its api so the previous card is look only
    val previous: (() -> Unit)? = if (last == null || viewing) null else { { viewing = true } }

    when {
        r == null -> GlassCard(Modifier.fillMaxWidth()) { Text(stringResource(R.string.loading_ankidroid), color = Ink.Muted) }
        r.isFailure -> GlassCard(Modifier.fillMaxWidth()) {
            EmptyState("鍵", stringResource(R.string.ankidroid_error_title), stringResource(R.string.ankidroid_error_body))
        }
        else -> {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountTile(info?.new ?: 0, stringResource(R.string.count_new), Ink.Easy, Modifier.weight(1f))
                CountTile(info?.learn ?: 0, stringResource(R.string.count_learning), Ink.Hard, Modifier.weight(1f))
                CountTile(info?.review ?: 0, stringResource(R.string.count_review), Ink.Good, Modifier.weight(1f))
            }
            val l = last
            when {
                viewing && l != null -> ReadOnlyCard(l.card) { viewing = false }
                next == null -> {
                    GlassCard(Modifier.fillMaxWidth()) {
                        EmptyState("済", stringResource(R.string.study_done_title), stringResource(R.string.ankidroid_done_body))
                    }
                    if (previous != null) GhostPill(stringResource(R.string.previous), icon = Ic.chevronLeft, onClick = previous)
                }
                else -> Flashcard(next.card, stringResource(R.string.via_ankidroid), labels, if (four) 4 else 3, onPrevious = previous) { grade(it) }
            }
        }
    }

    if (focusOpen) {
        val l = last
        val showing = if (viewing && l != null) l.card else next?.card
        val st = showing?.let { rememberAnswer(it, startRevealed = viewing) }
        FocusModal(
            title = deck.name, sub = stringResource(R.string.via_ankidroid), onClose = onCloseFocus, onPrevious = previous,
            card = showing, st = st,
            hint = if (viewing) stringResource(R.string.previous_readonly) else stringResource(R.string.focus_hint),
            onSubmit = { if (st != null && !viewing) { if (st.revealed) grade(2) else revealAnswer(ctx, st) } },
            actions = {
                if (viewing) WhitePill(stringResource(R.string.back_to_current), Modifier.fillMaxWidth()) { viewing = false }
                else GradeRow(labels, if (four) 4 else 3, st?.revealed == true, Modifier.fillMaxWidth()) { g ->
                    if (st?.revealed == true) grade(g) else st?.let { revealAnswer(ctx, it) }
                }
            },
            empty = { EmptyState("済", stringResource(R.string.study_done_title), stringResource(R.string.ankidroid_done_body)) }
        )
    }
}

//type the reading, right answer jumps to the next one, nothing gets scheduled
@Composable
private fun PracticeSession(deck: Deck, focusOpen: Boolean, onCloseFocus: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var round by remember(deck.id) { mutableIntStateOf(0) }
    val loaded by produceState<Result<List<DeckCard>>?>(null, deck.id, round) {
        value = withContext(Dispatchers.IO) { runCatching { Study.practiceCards(ctx, deck) } }
    }
    var index by remember(deck.id, round) { mutableIntStateOf(0) }
    var right by remember(deck.id, round) { mutableIntStateOf(0) }
    var streak by remember(deck.id, round) { mutableIntStateOf(0) }
    val missed = remember(deck.id, round) { mutableStateMapOf<Int, Boolean>() }

    val list = loaded?.getOrNull().orEmpty()
    val card = list.getOrNull(index)
    val st = card?.let { rememberAnswer(it) }
    val focusReq = remember { FocusRequester() }

    fun miss() { missed[index] = true; streak = 0 }
    fun advance() { index++ }
    fun checkTyped() {
        val s = st ?: return
        if (s.revealed) return
        if (s.correct) {
            if (missed[index] != true) { right++; streak++ }
            s.revealed = true
            Sfx.play(ctx, 2)
            val at = index
            scope.launch { delay(260); if (index == at) advance() }
        } else if (!s.onTrack) miss()
    }
    fun submit() {
        val s = st ?: return
        when {
            s.revealed -> advance()
            s.correct -> checkTyped()
            s.typed.text.isNotBlank() -> { Haptics.wrong(ctx); miss() }
        }
    }
    fun show() { st?.let { if (!it.revealed) { miss(); revealAnswer(ctx, it, buzz = false) } else advance() } }
    val previous: (() -> Unit)? = if (index == 0 || card == null && list.isEmpty()) null else { { index-- } }

    val empty: @Composable () -> Unit = {
        when {
            loaded == null -> Text(stringResource(R.string.deck_linked_loading), color = Ink.Muted)
            list.isEmpty() -> EmptyState("練", stringResource(R.string.practice_empty_title), stringResource(R.string.practice_empty_body))
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                EmptyState("済", stringResource(R.string.practice_done_title), stringResource(R.string.practice_done_body, right, list.size))
                WhitePill(stringResource(R.string.practice_restart)) { round++ }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CountTile(minOf(index + 1, list.size), stringResource(R.string.practice_tile_card, list.size), Ink.Easy, Modifier.weight(1f))
        CountTile(streak, stringResource(R.string.practice_tile_streak), Ink.Flame, Modifier.weight(1f))
        CountTile(right, stringResource(R.string.practice_tile_right), Ink.Good, Modifier.weight(1f))
    }
    if (card == null || st == null) {
        GlassCard(Modifier.fillMaxWidth()) { empty() }
    } else if (!focusOpen) {
        val bring = remember { BringIntoViewRequester() }
        Column(Modifier.bringIntoViewRequester(bring), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.fillMaxWidth(), high = true, padding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Kicker(stringResource(R.string.practice_kicker), Modifier.weight(1f))
                    Kicker(stringResource(R.string.practice_progress, index + 1, list.size))
                }
                Spacer(Modifier.height(16.dp))
                CardFront(card.front)
                Spacer(Modifier.height(16.dp))
                AnswerField(st, keepKeyboard = true, focusRequester = focusReq, bring = bring, onChange = { checkTyped() }, onSubmit = { submit() })
                AnimatedVisibility(st.revealed, enter = fadeIn() + expandVertically()) { RevealBlock(st, showVerdict = false) }
            }
            Row(Modifier.followReveal(st.revealed), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (previous != null) GlassIconButton(Ic.chevronLeft, stringResource(R.string.previous_cd), size = 46.dp, onClick = previous)
                WhitePill(stringResource(if (st.revealed) R.string.practice_next else R.string.practice_show), Modifier.weight(1f)) { show() }
                GhostPill(stringResource(R.string.practice_skip)) { advance() }
            }
        }
    }

    if (focusOpen) {
        FocusModal(
            title = deck.name, sub = stringResource(R.string.practice_progress, minOf(index + 1, list.size), list.size),
            onClose = onCloseFocus, onPrevious = previous,
            card = card, st = st, hint = null,
            onChange = { checkTyped() }, onSubmit = { submit() },
            actions = {
                if (card != null) {
                    WhitePill(stringResource(if (st?.revealed == true) R.string.practice_next else R.string.practice_show), Modifier.weight(1f)) { show() }
                    GhostPill(stringResource(R.string.practice_skip)) { advance() }
                }
            },
            empty = empty
        )
    }
}

@Composable
fun CountTile(n: Int, label: String, color: Color, modifier: Modifier) {
    GlassCard(modifier, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Ink.Muted, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Text("$n", style = MaterialTheme.typography.headlineMedium, color = Ink.Text)
    }
}

//typing state for one card, the normal card, focus mode and practice all use this
@Stable
class AnswerState(val card: DeckCard) {
    var typed by mutableStateOf(TextFieldValue(""))
    var revealed by mutableStateOf(false)
    var onTrack by mutableStateOf(true)
    val kanaMode = card.answers.any { Romaji.hasKana(it) }
    val canType = card.answers.isNotEmpty()
    val correct: Boolean get() = canType && Romaji.matches(typed.text, card.answers)
}

@Composable
fun rememberAnswer(card: DeckCard, startTyped: String = "", startRevealed: Boolean = false): AnswerState =
    remember(card.id, startRevealed) {
        AnswerState(card).apply { typed = TextFieldValue(startTyped, TextRange(startTyped.length)); revealed = startRevealed }
    }

fun revealAnswer(ctx: android.content.Context, st: AnswerState, buzz: Boolean = true) {
    if (st.kanaMode) st.typed = TextFieldValue(Romaji.toHiragana(st.typed.text, final = true))
    if (buzz && st.canType && st.typed.text.isNotBlank() && !st.correct) Haptics.wrong(ctx)
    st.revealed = true
}

@Composable
private fun CardFront(text: String, small: Boolean = false) {
    val size = when (text.length) { 1 -> 92.sp; 2 -> 70.sp; 3 -> 54.sp; in 4..6 -> 38.sp; else -> 26.sp }
    Text(
        text, style = KanjiStyle.copy(fontSize = if (small) size * 0.7f else size), color = Ink.Text,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().animateContentSize()
    )
}

//the input box, buzzes the moment what u typed cant be right anymore
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnswerField(
    st: AnswerState,
    keepKeyboard: Boolean,
    focusRequester: FocusRequester? = null,
    bring: BringIntoViewRequester? = null,
    onChange: () -> Unit = {},
    onSubmit: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val stroke = when {
        st.revealed && st.correct -> Ink.Good.copy(alpha = 0.7f)
        !st.onTrack -> Ink.Again.copy(alpha = 0.7f)
        else -> Ink.GlassStroke
    }
    BasicTextField(
        value = st.typed,
        onValueChange = { v ->
            if (st.revealed || !st.canType) return@BasicTextField
            val t = if (st.kanaMode) Romaji.toHiragana(v.text).let { TextFieldValue(it, TextRange(it.length)) } else v
            st.typed = t
            val ok = Romaji.onTrack(t.text, st.card.answers)
            if (st.onTrack && !ok) Haptics.wrong(ctx)
            st.onTrack = ok
            onChange()
        },
        enabled = keepKeyboard || !st.revealed,
        //flipping readOnly restarts the keyboard, so when it has to stay open the field just ignores input instead
        readOnly = !st.canType || (!keepKeyboard && st.revealed),
        singleLine = true,
        textStyle = TextStyle(color = Ink.Text, fontSize = 22.sp, textAlign = TextAlign.Center).merge(JapaneseText),
        cursorBrush = SolidColor(Ink.Text),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (st.kanaMode) KeyboardType.Ascii else KeyboardType.Text,
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (st.typed.text.isEmpty()) {
                    Text(
                        stringResource(
                            when {
                                !st.canType -> R.string.instruction_recall
                                st.kanaMode -> R.string.type_hint_romaji
                                else -> R.string.type_hint_text
                            }
                        ),
                        color = Ink.Faint, fontSize = 16.sp, textAlign = TextAlign.Center
                    )
                }
                inner()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(Ink.Glass)
            .border(1.dp, stroke, RoundedCornerShape(12.dp))
            .onFocusChanged { if (it.isFocused && bring != null) scope.launch { delay(350); bring.bringIntoView() } }
            .padding(vertical = 14.dp, horizontal = 12.dp)
    )
}

@Composable
private fun RevealBlock(st: AnswerState, showVerdict: Boolean = true) {
    val card = st.card
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.GlassStroke))
        Spacer(Modifier.height(16.dp))
        if (card.reading.isNotBlank()) {
            Text(card.reading, style = MaterialTheme.typography.headlineMedium.merge(JapaneseText), color = Ink.Text, textAlign = TextAlign.Center)
        }
        if (showVerdict && st.canType && st.typed.text.isNotBlank()) {
            val ok = st.correct
            Spacer(Modifier.height(6.dp))
            Text(
                if (ok) stringResource(R.string.typed_ok) else stringResource(R.string.typed_wrong, st.typed.text),
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
    onPrevious: (() -> Unit)? = null,
    onGrade: (Int) -> Unit
) {
    val ctx = LocalContext.current
    val st = rememberAnswer(card, startTyped, startRevealed)
    val focus = LocalFocusManager.current
    val bring = remember { BringIntoViewRequester() } //keeps the card above the keyboard

    fun reveal() { revealAnswer(ctx, st); focus.clearFocus() }

    Column(Modifier.bringIntoViewRequester(bring), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.fillMaxWidth(), high = true, padding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Kicker(card.instructions.ifBlank { stringResource(if (st.canType) R.string.instruction else R.string.instruction_recall) }, Modifier.weight(1f))
                Kicker(stateLabel)
            }
            Spacer(Modifier.height(16.dp))
            CardFront(card.front)
            Spacer(Modifier.height(16.dp))
            if (st.canType) AnswerField(st, keepKeyboard = false, bring = bring, onSubmit = { reveal() })
            AnimatedVisibility(st.revealed, enter = fadeIn() + expandVertically()) { RevealBlock(st) }
        }

        Row(Modifier.followReveal(st.revealed), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onPrevious != null) GlassIconButton(Ic.chevronLeft, stringResource(R.string.previous_cd), size = 46.dp, onClick = onPrevious)
            if (!st.revealed) WhitePill(stringResource(R.string.show_answer), Modifier.weight(1f)) { reveal() }
            else GradeRow(labels, buttons, true, Modifier.weight(1f), onGrade)
        }
    }
}

//once the answer opens up, scroll so the buttons under it arent hidden behind the nav bar
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.followReveal(revealed: Boolean): Modifier {
    val bring = remember { BringIntoViewRequester() }
    var h by remember { mutableIntStateOf(0) }
    val extra = with(LocalDensity.current) { 120.dp.toPx() }
    LaunchedEffect(revealed) {
        if (revealed) { delay(320); bring.bringIntoView(Rect(0f, 0f, 1f, h + extra)) }
    }
    return this.bringIntoViewRequester(bring).onSizeChanged { h = it.height }
}

@Composable
private fun ReadOnlyCard(card: DeckCard, onBack: () -> Unit) {
    val st = rememberAnswer(card, startRevealed = true)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.fillMaxWidth(), high = true, padding = PaddingValues(20.dp)) {
            Kicker(stringResource(R.string.previous))
            Spacer(Modifier.height(16.dp))
            CardFront(card.front)
            RevealBlock(st, showVerdict = false)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.previous_readonly), style = MaterialTheme.typography.labelMedium, color = Ink.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        WhitePill(stringResource(R.string.back_to_current), Modifier.fillMaxWidth(), onClick = onBack)
    }
}

@Composable
private fun GradeRow(labels: List<String>, buttons: Int, enabled: Boolean, modifier: Modifier, onGrade: (Int) -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val names = listOf(R.string.grade_again, R.string.grade_hard, R.string.grade_good, R.string.grade_easy)
        val colors = listOf(Ink.Again, Ink.Hard, Ink.Good, Ink.Easy)
        for (i in 0..3) {
            if (buttons < 4 && i == 1) continue
            GradeButton(stringResource(names[i]), labels.getOrElse(i) { "" }, colors[i], enabled, Modifier.weight(1f)) { onGrade(i) }
        }
    }
}

@Composable
private fun GradeButton(label: String, sub: String, color: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val a = if (enabled) 1f else 0.45f
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.18f * a),
        contentColor = Ink.Text,
        modifier = modifier.border(1.dp, color.copy(alpha = 0.45f * a), RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color.copy(alpha = a))
            Spacer(Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.labelMedium, color = Ink.Muted.copy(alpha = a))
        }
    }
}

//focus mode, the card sits in the middle of a dark screen
//buttons go above the kanji, the input under it, and tapping buttons never closes the keyboard
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusModal(
    title: String,
    sub: String,
    onClose: () -> Unit,
    onPrevious: (() -> Unit)?,
    card: DeckCard?,
    st: AnswerState?,
    hint: String?,
    onSubmit: () -> Unit,
    onChange: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit,
    empty: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { w ->
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                w.setDimAmount(0f)
            }
        }
        FocusBody(title, sub, onClose, onPrevious, card, st, hint, onSubmit, onChange, actions, empty)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusBody(
    title: String,
    sub: String,
    onClose: () -> Unit,
    onPrevious: (() -> Unit)?,
    card: DeckCard?,
    st: AnswerState?,
    hint: String?,
    onSubmit: () -> Unit,
    onChange: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    empty: @Composable () -> Unit
) {
    val focus = LocalFocusManager.current
    val req = remember { FocusRequester() }
    val ime = WindowInsets.isImeVisible
    LaunchedEffect(Unit) { delay(250); runCatching { req.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.Scrim)
            .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .pointerInput(Unit) { detectTapGestures { } },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onPrevious != null) GlassIconButton(Ic.chevronLeft, stringResource(R.string.previous_cd), size = 40.dp, onClick = onPrevious)
                else Spacer(Modifier.size(40.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sub, style = MaterialTheme.typography.labelMedium, color = Ink.Muted, maxLines = 1)
                }
                GlassIconButton(Ic.close, stringResource(R.string.focus_close), size = 40.dp, onClick = onClose)
            }

            if (card == null || st == null) {
                GlassCard(Modifier.fillMaxWidth(), high = true) { empty() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
                GlassCard(Modifier.fillMaxWidth(), high = true, padding = PaddingValues(if (ime) 16.dp else 22.dp)) {
                    Kicker(card.instructions.ifBlank { stringResource(if (st.canType) R.string.instruction else R.string.instruction_recall) })
                    Spacer(Modifier.height(if (ime) 6.dp else 14.dp))
                    CardFront(card.front, small = ime)
                    AnimatedVisibility(st.revealed, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        RevealBlock(st)
                    }
                }
                AnswerField(st, keepKeyboard = true, focusRequester = req, onChange = onChange, onSubmit = onSubmit)
                if (hint != null) Text(hint, style = MaterialTheme.typography.labelSmall, color = Ink.Faint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
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
                            .background(if (i == 0) Ink.Pill else Ink.GlassHigh)
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
            Legend(Ink.Pill, stringResource(R.string.mature), ins.mature)
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
    val imported = remember(tick) { DeckStore.all(ctx).filter { it.id != DeckStore.ACCEPTED } }
    //0 accepted 1 decks 2 rejected 3 mined 4 dictionary
    val source = when (mode) { 0 -> accepted; 2 -> rejected; 3 -> mined; else -> emptyList() }
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
                3 -> GlassIconButton(Ic.upload, stringResource(R.string.import_words)) {
                    picker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "text/tab-separated-values", "application/json"))
                }
            }
        }

        PillTabs(
            options = listOf(
                stringResource(R.string.tab_accepted, accepted.size),
                stringResource(R.string.tab_decks, imported.size),
                stringResource(R.string.tab_rejected, rejected.size),
                stringResource(R.string.tab_mined_n, mined.size),
                stringResource(R.string.tab_dictionary)
            ),
            selected = mode,
            onSelect = { mode = it; query = "" },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        if (mode == 4) {
            DictionaryPane(tick, snackbar, deckKeys)
        } else if (mode == 1) {
            DecksPane(imported)
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
                    2 -> EmptyState("無", stringResource(R.string.empty_rejected_title), stringResource(R.string.empty_rejected_body))
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
                            highlight = mode == 3 && w.key in deckKeys,
                            actionIcon = if (mode == 2) Ic.undo else Ic.delete,
                            actionLabel = stringResource(
                                when (mode) { 0 -> R.string.remove_from_deck; 2 -> R.string.restore; else -> R.string.delete }
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
                                2 -> {
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

//cards from imported decks, ankidroid ones get read live from ankidroid
@Composable
private fun DecksPane(decks: List<Deck>) {
    val ctx = LocalContext.current
    if (decks.isEmpty()) {
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            EmptyState("束", stringResource(R.string.decks_empty_title), stringResource(R.string.decks_empty_body))
        }
        return
    }
    var pick by rememberSaveable { mutableStateOf(decks.first().id) }
    val deck = decks.firstOrNull { it.id == pick } ?: decks.first()
    var query by rememberSaveable(deck.id) { mutableStateOf("") }
    val cards by produceState<Result<List<DeckCard>>?>(null, deck.id) {
        value = withContext(Dispatchers.IO) {
            runCatching { if (deck.isLinked) AnkiDroid.notes(ctx, deck.ankiDeckName) else DeckStore.cards(ctx, deck.id) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(decks, key = { it.id }) { d ->
                val on = d.id == deck.id
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(if (on) Ink.Pill else Ink.Glass)
                        .border(1.dp, if (on) Color.Transparent else Ink.GlassStroke, CircleShape)
                        .clickable { pick = d.id }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Dot(sourceColor(d.source), 6.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(d.name, style = MaterialTheme.typography.labelLarge, color = if (on) Ink.OnWhite else Ink.Text, maxLines = 1)
                }
            }
        }

        val r = cards
        when {
            r == null -> Text(stringResource(R.string.deck_linked_loading), color = Ink.Muted, modifier = Modifier.padding(horizontal = 22.dp))
            r.isFailure -> GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                EmptyState("鍵", stringResource(R.string.ankidroid_error_title), stringResource(R.string.deck_linked_error))
            }
            else -> {
                val all = r.getOrThrow()
                val q = query.trim()
                val shown = if (q.isEmpty()) all else all.filter {
                    it.front.contains(q) || it.reading.contains(q) || it.meaning.contains(q, ignoreCase = true)
                }
                SearchField(query, { query = it }, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
                Row(Modifier.padding(horizontal = 20.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    SourceTag(deck.source)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.deck_cards_count, all.size), style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
                }
                Spacer(Modifier.height(8.dp))
                GlassCard(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).weight(1f, fill = false),
                    padding = PaddingValues(8.dp)
                ) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(shown, key = { it.id }) { c -> DeckCardRow(c) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckCardRow(c: DeckCard) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ink.Glass).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KanjiTile(c.front, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (c.reading.isNotBlank()) Text(c.reading, style = MaterialTheme.typography.labelMedium.merge(JapaneseText), color = Ink.Muted, maxLines = 1)
            Text(c.meaning.ifBlank { c.extra }, style = MaterialTheme.typography.bodyMedium, color = Ink.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    var vibrate by remember { mutableStateOf(Prefs.vibrate(ctx)) }
    var widgetDark by remember { mutableStateOf(Prefs.widgetDark(ctx)) }
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
                Divider()
                ToggleRow(stringResource(R.string.vibrate_title), stringResource(R.string.vibrate_sub), vibrate) {
                    vibrate = it; Prefs.setVibrate(ctx, it); if (it) Haptics.wrong(ctx)
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

            Section(stringResource(R.string.section_theme)) { ThemePicker() }

            Section(stringResource(R.string.section_lock)) {
                Choice(
                    stringResource(R.string.widget_bg_label),
                    listOf(Prefs.BG_SOLID to R.string.bg_solid, Prefs.BG_SEMI to R.string.bg_semi, Prefs.BG_CLEAR to R.string.bg_clear),
                    widgetBg
                ) { widgetBg = it; Prefs.setWidgetBg(ctx, it); DailyWordManager.refresh(ctx) }
                Divider()
                ToggleRow(stringResource(R.string.widget_dark_title), stringResource(R.string.widget_dark_sub), widgetDark) {
                    widgetDark = it; Prefs.setWidgetDark(ctx, it); DailyWordManager.refresh(ctx)
                }
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
                                thumbColor = Ink.Pill, activeTrackColor = Ink.Pill, inactiveTrackColor = Ink.GlassHigh,
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

            if (Updater.enabled) Section(stringResource(R.string.section_updates)) { UpdateRows() }

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
                checkedTrackColor = Ink.Pill, checkedThumbColor = Ink.OnWhite,
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
                checkedTrackColor = Ink.Pill, checkedThumbColor = Ink.OnWhite,
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
                    clockDialColor = Ink.Glass, selectorColor = Ink.Pill, clockDialSelectedContentColor = Ink.OnWhite,
                    timeSelectorSelectedContainerColor = Ink.Pill, timeSelectorSelectedContentColor = Ink.OnWhite,
                    timeSelectorUnselectedContainerColor = Ink.Glass, periodSelectorSelectedContainerColor = Ink.Pill,
                    periodSelectorSelectedContentColor = Ink.OnWhite
                )
            )
        },
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.save), color = Ink.Text) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Ink.Muted) } }
    )
}


//little previews of each theme, tap one and the whole app switches
@Composable
private fun ThemePicker() {
    val ctx = LocalContext.current
    LazyRow(contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Palettes.all, key = { it.id }) { p ->
            val on = Ink.palette.id == p.id
            Column(
                Modifier
                    .width(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(if (on) 2.dp else 1.dp, if (on) Ink.Pill else Ink.GlassStroke, RoundedCornerShape(16.dp))
                    .clickable { Ink.palette = p; Prefs.setTheme(ctx, p.id) }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(11.dp))
                        .background(Brush.verticalGradient(listOf(p.bgTop, p.bgMid, p.bgBottom))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("字", style = KanjiStyle.copy(fontSize = 26.sp), color = p.text)
                    Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(p.again, p.hard, p.good, p.easy).forEach { Box(Modifier.size(6.dp).clip(CircleShape).background(it)) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(p.name, style = MaterialTheme.typography.labelMedium, color = if (on) Ink.Text else Ink.Muted)
            }
        }
    }
}

//update checker in settings, same popup as the one on launch
@Composable
private fun UpdateRows() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var auto by remember { mutableStateOf(Prefs.autoUpdate(ctx)) }
    var checking by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<Updater.Release?>(null) }
    var checkedAt by remember { mutableLongStateOf(Prefs.updateChecked(ctx)) }

    ToggleRow(stringResource(R.string.update_auto_title), stringResource(R.string.update_auto_sub), auto) {
        auto = it; Prefs.setAutoUpdate(ctx, it)
    }
    Divider()
    LinkRow(
        Ic.download,
        stringResource(if (checking) R.string.update_checking else R.string.update_check_title),
        stringResource(
            R.string.update_check_sub, Updater.current(ctx),
            if (checkedAt == 0L) stringResource(R.string.update_never) else android.text.format.DateUtils.getRelativeTimeSpanString(checkedAt).toString()
        )
    ) {
        if (checking) return@LinkRow
        checking = true
        scope.launch {
            val r = Updater.check(ctx, manual = true)
            checking = false; checkedAt = Prefs.updateChecked(ctx)
            r.onSuccess { rel ->
                if (rel != null) found = rel else Toast.makeText(ctx, R.string.update_latest, Toast.LENGTH_SHORT).show()
            }.onFailure { Toast.makeText(ctx, R.string.update_check_failed, Toast.LENGTH_LONG).show() }
        }
    }
    found?.let { UpdateDialog(it) { found = null } }
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
