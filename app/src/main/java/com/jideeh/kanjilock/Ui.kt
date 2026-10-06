package com.jideeh.kanjilock

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Shapes
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Thread { Dictionary.warmUp(applicationContext) }.start()
        DailyResetReceiver.schedule(this)
        Reminders.scheduleAll(this)
        LockScreenNotifier.ensureChannel(this)
        DailyWordManager.ensureToday(this)
        setContent { KanjiTheme { KanjiApp() } }
    }

    override fun onStart() { // drive pull
        super.onStart()
        DriveSync.sessionStart(this)
    }

    override fun onStop() { //push if anything changed
        DriveSync.sessionEnd(this)
        super.onStop()
    }
}

//colors from the teal glass reference
object Ink {
    val BgTop = Color(0xFF111D20)
    val BgMid = Color(0xFF172629)
    val BgBottom = Color(0xFF2A4247)

    val Glass = Color(0x12FFFFFF)
    val GlassHigh = Color(0x1CFFFFFF)
    val GlassStroke = Color(0x14FFFFFF)

    val Text = Color(0xFFEDF3F3)
    val Muted = Color(0xFF93A6A8)
    val Faint = Color(0xFF63777A)
    val OnWhite = Color(0xFF172427)

    val Reject = Color(0xFFE0605A)
    val Reload = Color(0xFF56696C)
    val Accept = Color(0xFF3FA877)
    val Flame = Color(0xFFFF9A3D)

    val Again = Color(0xFFE0605A)
    val Hard = Color(0xFFD9A54A)
    val Good = Color(0xFF3FA877)
    val Easy = Color(0xFF5B9BD5)

    val background = Brush.verticalGradient(0f to BgTop, 0.45f to BgMid, 1f to BgBottom)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val Inter = FontFamily(inter(300), inter(400), inter(500), inter(600), inter(700))

private val Colors = darkColorScheme(
    primary = Color(0xFFEDF3F3),
    onPrimary = Ink.OnWhite,
    primaryContainer = Color(0xFF2E4448),
    onPrimaryContainer = Ink.Text,
    secondary = Ink.Muted,
    secondaryContainer = Color(0xFF2B3E41),
    onSecondaryContainer = Ink.Text,
    tertiary = Ink.Flame,
    tertiaryContainer = Color(0xFF3A3328),
    onTertiaryContainer = Color(0xFFFFD8B0),
    background = Ink.BgMid,
    onBackground = Ink.Text,
    surface = Color(0xFF1A2A2D),
    onSurface = Ink.Text,
    surfaceVariant = Color(0xFF243538),
    onSurfaceVariant = Ink.Muted,
    surfaceContainerLowest = Color(0xFF13201F),
    surfaceContainerLow = Color(0xFF1A2A2D),
    surfaceContainer = Color(0xFF1E2F32),
    surfaceContainerHigh = Color(0xFF243639),
    surfaceContainerHighest = Color(0xFF2C3F42),
    inverseSurface = Color(0xFFEDF3F3),
    inverseOnSurface = Ink.OnWhite,
    outline = Color(0xFF4A5E61),
    outlineVariant = Color(0xFF2F4245),
    error = Color(0xFFF08A84),
    errorContainer = Color(0xFF4A2725),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val base = Typography()
private fun TextStyle.inter() = copy(fontFamily = Inter)

private val Type = Typography(
    displayLarge = base.displayLarge.inter().copy(fontWeight = FontWeight.Light),
    displayMedium = base.displayMedium.inter().copy(fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    displaySmall = base.displaySmall.inter().copy(fontWeight = FontWeight.Normal, fontSize = 40.sp, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.inter().copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.inter().copy(fontWeight = FontWeight.Normal),
    headlineSmall = base.headlineSmall.inter().copy(fontWeight = FontWeight.Normal),
    titleLarge = base.titleLarge.inter().copy(fontWeight = FontWeight.Normal),
    titleMedium = base.titleMedium.inter().copy(fontWeight = FontWeight.Normal),
    titleSmall = base.titleSmall.inter(),
    bodyLarge = base.bodyLarge.inter(),
    bodyMedium = base.bodyMedium.inter(),
    bodySmall = base.bodySmall.inter(),
    labelLarge = base.labelLarge.inter().copy(fontWeight = FontWeight.Normal),
    labelMedium = base.labelMedium.inter().copy(fontWeight = FontWeight.Normal),
    labelSmall = base.labelSmall.inter().copy(letterSpacing = 0.6.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val JapaneseLocale = LocaleList("ja-JP")


//ja locale so kanji use the japanese shapes not chinese ones
val KanjiStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, localeList = JapaneseLocale)

val JapaneseText = TextStyle(localeList = JapaneseLocale)

@Composable
fun KanjiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, shapes = AppShapes, content = content)
}


val CardShape = RoundedCornerShape(20.dp)
val TileShape = RoundedCornerShape(14.dp)


//the see thru card thats used everywhere
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    high: Boolean = false,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(CardShape)
            .background(if (high) Ink.GlassHigh else Ink.Glass)
            .border(1.dp, Ink.GlassStroke, CardShape)
            .padding(padding),
        content = content
    )
}

@Composable
fun InnerTile(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .clip(TileShape)
            .background(Ink.Glass)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun Kicker(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Ink.Muted,
        modifier = modifier
    )
}

@Composable
fun ScreenHeader(
    kicker: String,
    title: String,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Kicker(kicker)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.displaySmall, color = Ink.Text)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Ink.Muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = trailing)
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    size: Dp = 40.dp,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) Color.White else Ink.GlassHigh, label = "gib")
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription,
            tint = tint ?: if (selected) Ink.OnWhite else Ink.Text,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun WhitePill(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) Color.White else Ink.GlassHigh,
        contentColor = if (enabled) Ink.OnWhite else Ink.Faint,
        modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun GhostPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Ink.GlassHigh,
        contentColor = Ink.Text,
        border = BorderStroke(1.dp, Ink.GlassStroke),
        modifier = modifier
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PillTabs(
    options: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.Glass)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Color.White else Color.Transparent, label = "pill")
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) Ink.OnWhite else Ink.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


//u can type the middle number too
@Composable
fun NumberStepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!focused) text = value.toString() }
    val focus = LocalFocusManager.current
    fun commit() {
        val n = text.toIntOrNull()?.coerceIn(range) ?: value
        text = n.toString()
        if (n != value) onChange(n)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassIconButton(Ic.remove, "Less", size = 36.dp) {
            val n = (value - 1).coerceIn(range); text = n.toString(); onChange(n)
        }
        BasicTextField(
            value = text,
            onValueChange = { s ->
                val digits = s.filter { it.isDigit() }.take(3)
                text = digits
                digits.toIntOrNull()?.let { if (it in range && it != value) onChange(it) }
            },
            singleLine = true,
            textStyle = TextStyle(
                color = Ink.Text, fontSize = 18.sp, textAlign = TextAlign.Center,
                fontFamily = Inter, fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(Ink.Text),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit(); focus.clearFocus() }),
            modifier = Modifier
                .width(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Ink.Glass)
                .border(1.dp, if (focused) Ink.Muted else Ink.GlassStroke, RoundedCornerShape(10.dp))
                .onFocusChanged { f -> if (focused && !f.isFocused) commit(); focused = f.isFocused }
                .padding(vertical = 8.dp)
        )
        GlassIconButton(Ic.add, "More", size = 36.dp) {
            val n = (value + 1).coerceIn(range); text = n.toString(); onChange(n)
        }
    }
}

// font gets smaller for long words
@Composable
fun KanjiTile(text: String, size: Dp = 52.dp, highlight: Boolean = false) {
    val fs: TextUnit = when (text.length) {
        1 -> (size.value * 0.55f).sp
        2 -> (size.value * 0.40f).sp
        3 -> (size.value * 0.30f).sp
        else -> (size.value * 0.24f).sp
    }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlight) Color(0x22FFFFFF) else Ink.GlassHigh),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = KanjiStyle.copy(fontSize = fs), color = Ink.Text, maxLines = 1)
    }
}

@Composable
fun EmptyState(glyph: String, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(22.dp)).background(Ink.GlassHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, style = KanjiStyle.copy(fontSize = 42.sp), color = Ink.Muted)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

private val WEEKDAYS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun StreakCalendar(days: Map<String, ActivityLog.Day>, goal: Int, expanded: Boolean, today: Calendar = Calendar.getInstance()) {
    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            (fadeIn(tween(220)) + expandVertically(tween(260))) togetherWith (fadeOut(tween(120)) + shrinkVertically(tween(220)))
        },
        label = "calendar"
    ) { open ->
        if (open) MonthGrid(days, goal, today) else WeekStrip(days, goal, today)
    }
}

@Composable
private fun WeekStrip(active: Map<String, ActivityLog.Day>, goal: Int, today: Calendar) {
    val start = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -(get(Calendar.DAY_OF_WEEK) - 1)) }
    Row(Modifier.fillMaxWidth()) {
        for (i in 0 until 7) {
            val d = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(WEEKDAYS[i], style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
                Spacer(Modifier.height(6.dp))
                DayCell(d, active, goal, today, Modifier.size(42.dp))
            }
        }
    }
}

@Composable
private fun MonthGrid(active: Map<String, ActivityLog.Day>, goal: Int, today: Calendar) {
    var offset by remember { mutableIntStateOf(0) }
    val month = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, offset) }
    val title = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
    val lead = month.get(Calendar.DAY_OF_WEEK) - 1
    val days = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val prefix = SimpleDateFormat("yyyy-MM-", Locale.US).format(month.time)
    val flames = active.keys.count { it.startsWith(prefix) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.Text)
                Text("$flames flame ${if (flames == 1) "day" else "days"}", style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
            }
            GlassIconButton(Ic.chevronLeft, "Previous month", size = 34.dp) { offset-- }
            Spacer(Modifier.size(8.dp))
            GlassIconButton(Ic.chevronRight, "Next month", size = 34.dp) { if (offset < 0) offset++ }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            WEEKDAYS.forEach {
                Text(
                    it, style = MaterialTheme.typography.labelMedium, color = Ink.Muted,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val cells = lead + days
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (idx in 1..days) {
                            val d = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, idx) }
                            DayCell(d, active, goal, today, Modifier.fillMaxWidth().aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

//gray flame that fills orange from the bottom like a bucket, depends on how much u did
@Composable
private fun DayCell(d: Calendar, active: Map<String, ActivityLog.Day>, goal: Int, today: Calendar, modifier: Modifier) {
    val key = ActivityLog.dayKey(d)
    val isToday = key == ActivityLog.dayKey(today)
    val isFuture = d.after(today) && !isToday
    val total = active[key]?.total ?: 0
    val fill = (total.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isToday) Ink.GlassHigh else androidx.compose.ui.graphics.Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (!isFuture) FlameGauge(fill, Modifier.fillMaxWidth(0.78f).aspectRatio(1f))
        Text(
            "${d.get(Calendar.DAY_OF_MONTH)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (total > 0 || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isFuture -> Ink.Faint
                fill >= 0.5f -> androidx.compose.ui.graphics.Color.White
                else -> Ink.Text
            },
            modifier = Modifier.padding(top = if (isFuture) 0.dp else 6.dp)
        )
    }
}


@Composable
fun FlameGauge(fill: Float, modifier: Modifier) {
    Box(modifier) {
        Icon(Ic.flame, null, tint = androidx.compose.ui.graphics.Color(0x33FFFFFF), modifier = Modifier.matchParentSize())
        if (fill > 0f) {
            Icon(
                Ic.flame, null, tint = Ink.Flame,
                modifier = Modifier.matchParentSize().drawWithContent {
                    clipRect(top = size.height * (1f - (0.12f + 0.88f * fill))) { this@drawWithContent.drawContent() }
                }
            )
        }
    }
}

private enum class Tab(val icon: ImageVector, val label: String) {
    TODAY(Ic.home, "Today"),
    STUDY(Ic.cards, "Study"),
    WORDS(Ic.bookmark, "Words"),
    SETTINGS(Ic.settings, "Settings"),
}

val BarClearance = PaddingValues(bottom = 108.dp)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KanjiApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val tick by DailyWordManager.changes.collectAsStateWithLifecycle()
    val due = remember(tick) { Study.dueNow(ctx) }

    LifecycleResumeEffect(Unit) {
        DailyWordManager.refresh(ctx)
        onPauseOrDispose { }
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        DailyWordManager.refresh(ctx)
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Box(Modifier.fillMaxSize().background(Ink.background)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "tab",
            modifier = Modifier.fillMaxSize().statusBarsPadding()
        ) { current ->
            when (Tab.entries[current]) {
                Tab.TODAY -> TodayScreen(tick, snackbar, onStudy = { tab = Tab.STUDY.ordinal })
                Tab.STUDY -> StudyScreen(tick)
                Tab.WORDS -> WordsScreen(tick, snackbar)
                Tab.SETTINGS -> SettingsScreen(tick, snackbar)
            }
        }

        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp)
        )

        //hide the bar when keyboard is up or it covers the input
        if (!WindowInsets.isImeVisible) FloatingBar(
            selected = tab,
            studyBadge = due,
            onSelect = { tab = it },
            onAdd = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp)
        )
    }

    if (showAdd) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAdd = false },
            sheetState = sheet,
            containerColor = Ink.BgMid,
            contentColor = Ink.Text
        ) {
            AddWordForm { w ->
                val added = MinedStore.add(ctx, listOf(w))
                DailyWordManager.refresh(ctx)
                showAdd = false
                scope.launch {
                    snackbar.showSnackbar(
                        ctx.getString(if (added > 0) R.string.snack_word_added else R.string.snack_word_exists, w.word)
                    )
                }
            }
        }
    }
}


@Composable
private fun FloatingBar(
    selected: Int,
    studyBadge: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .clip(shape)
                .background(Ink.BgMid.copy(alpha = 0.92f))
                .border(1.dp, Ink.GlassStroke, shape)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tab.entries.forEachIndexed { i, t ->
                Box {
                    GlassIconButton(t.icon, t.label, selected = selected == i, size = 48.dp) { onSelect(i) }
                    if (t == Tab.STUDY && studyBadge > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Ink.Flame),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (studyBadge > 99) "99" else "$studyBadge",
                                color = Ink.OnWhite, fontSize = 10.sp, lineHeight = 10.sp
                            )
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .clip(shape)
                .background(Ink.BgMid.copy(alpha = 0.92f))
                .border(1.dp, Ink.GlassStroke, shape)
                .padding(8.dp)
        ) {
            GlassIconButton(Ic.add, "Add word", size = 48.dp, onClick = onAdd)
        }
    }
}


private val KANA_ONLY = Regex("^[\\p{InHiragana}\\p{InKatakana}ー・ 　]+$")

//furigana HAS to be kana no romaji
fun isKana(s: String): Boolean = s.isNotBlank() && KANA_ONLY.matches(s.trim())

object Exporter {
    fun shareCsv(ctx: Context) {
        if (AcceptedStore.keys(ctx).isEmpty()) { Toast.makeText(ctx, R.string.err_no_accepted, Toast.LENGTH_SHORT).show(); return }
        AcceptedStore.rebuildCsv(ctx)
        share(ctx, AcceptedStore.csvFile(ctx), "text/csv", "KanjiLock accepted words")
    }

    //tab seperated with anki headers so it imports straight in
    fun shareAnki(ctx: Context) {
        if (AcceptedStore.keys(ctx).isEmpty()) { Toast.makeText(ctx, R.string.err_no_accepted, Toast.LENGTH_SHORT).show(); return }
        share(ctx, AcceptedStore.ankiFile(ctx), "text/plain", "KanjiLock for Anki")
    }

    private fun share(ctx: Context, f: java.io.File, mime: String, subject: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

class Speaker(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(appCtx) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    fun speak(text: String) {
        if (!ready) return
        val res = tts.setLanguage(Locale.JAPAN)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(appCtx, R.string.err_no_japanese_voice, Toast.LENGTH_LONG).show()
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kanjilock")
    }

    fun shutdown() = tts.shutdown()
}

object Importer {
    data class Result(val words: List<Word>, val skipped: Int)

    fun read(ctx: Context, uri: Uri): Result {
        val text = try {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (_: Exception) { "" }
        if (text.isBlank()) return Result(emptyList(), 0)
        val trimmed = text.trimStart('﻿', ' ', '\n', '\r', '\t')
        val raw = if (trimmed.startsWith("{") || trimmed.startsWith("[")) parseJson(trimmed) else parseCsv(trimmed)
        val ok = raw.filter { it.word.isNotBlank() && isKana(it.reading) }
        return Result(ok, raw.size - ok.size)
    }

    private fun parseJson(text: String): List<Word> = try {
        val arr = if (text.startsWith("[")) JSONArray(text)
        else JSONObject(text).optJSONArray("entries") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Word(
                id = "", word = o.optString("word").trim(), reading = o.optString("reading").trim(),
                meaning = o.optString("meaning").trim(), example = o.optString("example").trim(),
                exampleReading = o.optString("exampleReading").trim(),
                exampleMeaning = o.optString("exampleMeaning").trim(),
                type = o.optString("type", "word").ifBlank { "word" }, source = "mined"
            )
        }
    } catch (_: Exception) { emptyList() }

    //word reading meaning, then the example stuff is optional
    private fun parseCsv(text: String): List<Word> {
        val lines = text.split(Regex("\r\n|\n|\r")).filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val delim = if (lines[0].count { it == '\t' } > lines[0].count { it == ',' }) '\t' else ','
        val first = lines[0].lowercase()
        val start = if (listOf("word", "reading", "question", "meaning").any { first.contains(it) }) 1 else 0
        return lines.drop(start).mapNotNull { line ->
            val c = split(line, delim)
            if (c.isEmpty() || c[0].isBlank()) return@mapNotNull null
            val word = c[0].trim()
            Word(
                id = "", word = word,
                reading = c.getOrElse(1) { "" }.trim(),
                meaning = c.getOrElse(2) { "" }.trim(),
                example = c.getOrElse(3) { "" }.trim(),
                exampleReading = c.getOrElse(4) { "" }.trim(),
                exampleMeaning = c.getOrElse(5) { "" }.trim(),
                type = c.getOrElse(6) { "" }.trim().ifBlank { if (word.length == 1) "kanji" else "word" },
                source = "mined"
            )
        }
    }

    private fun split(line: String, delim: Char): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var q = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && q && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                ch == '"' -> q = !q
                ch == delim && !q -> { out.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(ch)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}

//sounds for again hard good easy
object Sfx {
    private var pool: android.media.SoundPool? = null
    private val ids = IntArray(4)

    fun init(ctx: Context) {
        if (pool != null) return
        val p = android.media.SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
        val app = ctx.applicationContext
        ids[0] = p.load(app, R.raw.sfx_again, 1)
        ids[1] = p.load(app, R.raw.sfx_hard, 1)
        ids[2] = p.load(app, R.raw.sfx_good, 1)
        ids[3] = p.load(app, R.raw.sfx_easy, 1)
        pool = p
    }
    fun play(ctx: Context, grade: Int) {
        if (!Prefs.sfx(ctx)) return
        init(ctx)
        pool?.play(ids[grade.coerceIn(0, 3)], 0.9f, 0.9f, 1, 0, 1f)
    }
}

//icons as code instead of 25 xml files
object Ic {
    private fun v(name: String, d: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.White)).build()

    val add by lazy { v("add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }
    val book by lazy { v("book", "M18,2H6c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM6,4h5v8l-2.5,-1.5L6,12V4z") }
    val bookmark by lazy { v("bookmark", "M17,3H7c-1.1,0 -2,0.9 -2,2v16l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z") }
    val calendar by lazy { v("calendar", "M20,3h-1V1h-2v2H7V1H5v2H4c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM20,21H4V8h16v13z") }
    val cards by lazy { v("cards", "M2.53,19.65l1.34,0.56v-9.03l-2.43,5.86c-0.41,1.02 0.08,2.19 1.09,2.61zM22.03,15.95L17.07,3.98c-0.31,-0.75 -1.04,-1.21 -1.81,-1.23 -0.26,0 -0.53,0.04 -0.79,0.15L7.1,5.95c-0.75,0.31 -1.21,1.03 -1.23,1.8 -0.01,0.27 0.04,0.54 0.15,0.8l4.96,11.97c0.31,0.76 1.05,1.22 1.83,1.23 0.26,0 0.52,-0.05 0.77,-0.15l7.36,-3.05c1.02,-0.42 1.51,-1.59 1.09,-2.6zM7.88,8.75c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1 1,0.45 1,1 -0.45,1 -1,1zM5.88,19.75c0,1.1 0.9,2 2,2h1.45l-3.45,-8.34v6.34z") }
    val check by lazy { v("check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z") }
    val chevronLeft by lazy { v("chevronLeft", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z") }
    val chevronRight by lazy { v("chevronRight", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z") }
    val close by lazy { v("close", "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z") }
    val cloud by lazy { v("cloud", "M19.35,10.04C18.67,6.59 15.64,4 12,4 9.11,4 6.6,5.64 5.35,8.04 2.34,8.36 0,10.91 0,14c0,3.31 2.69,6 6,6h13c2.76,0 5,-2.24 5,-5 0,-2.64 -2.05,-4.78 -4.65,-4.96z") }
    val delete by lazy { v("delete", "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z") }
    val download by lazy { v("download", "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z") }
    val expandLess by lazy { v("expandLess", "M12,8l-6,6 1.41,1.41L12,10.83l4.59,4.58L18,14z") }
    val expandMore by lazy { v("expandMore", "M16.59,8.59L12,13.17 7.41,8.59 6,10l6,6 6,-6z") }
    val flame by lazy { v("flame", "M13.5,0.67s0.74,2.65 0.74,4.8c0,2.06 -1.35,3.73 -3.41,3.73 -2.07,0 -3.63,-1.67 -3.63,-3.73l0.03,-0.36C5.21,7.51 4,10.62 4,14c0,4.42 3.58,8 8,8s8,-3.58 8,-8C20,8.61 17.41,3.8 13.5,0.67zM11.71,19c-1.78,0 -3.22,-1.4 -3.22,-3.14 0,-1.62 1.05,-2.76 2.81,-3.12 1.77,-0.36 3.6,-1.21 4.62,-2.58 0.39,1.29 0.59,2.65 0.59,4.04 0,2.65 -2.15,4.8 -4.8,4.8z") }
    val home by lazy { v("home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z") }
    val lock by lazy { v("lock", "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z") }
    val notifications by lazy { v("notifications", "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z") }
    val refresh by lazy { v("refresh", "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z") }
    val remove by lazy { v("remove", "M19,13H5v-2h14v2z") }
    val search by lazy { v("search", "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z") }
    val settings by lazy { v("settings", "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z") }
    val share by lazy { v("share", "M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z") }
    val sun by lazy { v("sun", "M6.76,4.84l-1.8,-1.79 -1.41,1.41 1.79,1.79 1.42,-1.41zM4,10.5H1v2h3v-2zM13,0.55h-2V3.5h2V0.55zM20.45,4.46l-1.41,-1.41 -1.79,1.79 1.41,1.41 1.79,-1.79zM17.24,18.16l1.79,1.8 1.41,-1.41 -1.8,-1.79 -1.4,1.4zM20,10.5v2h3v-2h-3zM12,5.5c-3.31,0 -6,2.69 -6,6s2.69,6 6,6 6,-2.69 6,-6 -2.69,-6 -6,-6zM11,22.45h2V19.5h-2v2.95zM3.55,18.54l1.41,1.41 1.79,-1.8 -1.41,-1.41 -1.79,1.8z") }
    val undo by lazy { v("undo", "M12.5,8c-2.65,0 -5.05,0.99 -6.9,2.6L2,7v9h9l-3.62,-3.62c1.39,-1.16 3.16,-1.88 5.12,-1.88 3.54,0 6.55,2.31 7.6,5.5l2.37,-0.78C21.08,11.03 17.15,8 12.5,8z") }
    val upload by lazy { v("upload", "M9,16h6v-6h4l-7,-7 -7,7h4zM5,18h14v2H5z") }
    val volume by lazy { v("volume", "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z") }
    val widgets by lazy { v("widgets", "M13,13v8h8v-8h-8zM3,21h8v-8H3v8zM3,3v8h8V3H3zM16.66,1.69L11,7.34 16.66,13l5.66,-5.66 -5.66,-5.65z") }
}

private val urlRe = Regex("https?://[^\\s)]+")

//makes urls clickable in the license text
fun linkify(text: String): AnnotatedString = buildAnnotatedString {
    var at = 0
    for (m in urlRe.findAll(text)) {
        val url = m.value.trimEnd('.', ',')
        append(text.substring(at, m.range.first))
        withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = Ink.Text, textDecoration = TextDecoration.Underline)))) { append(url) }
        at = m.range.first + url.length
    }
    append(text.substring(at))
}

@Composable
fun TextDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ink.BgMid,
        title = { Text(title) },
        text = {
            Text(
                linkify(body), Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall, color = Ink.Muted
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = Ink.Text) } }
    )
}
