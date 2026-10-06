package com.jideeh.kanjilock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
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
        AppNav.take(intent)
        Ink.palette = Palettes.byId(Prefs.theme(this))
        setContent {
            //status bar icons have to flip for the light theme
            val light = Ink.palette.light
            val view = androidx.compose.ui.platform.LocalView.current
            SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }
            KanjiTheme { KanjiApp() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent); AppNav.take(intent)
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
class Palette(
    val id: String,
    val name: String,
    val bgTop: Color, val bgMid: Color, val bgBottom: Color,
    val text: Color, val muted: Color, val faint: Color,
    val light: Boolean = false,
    val pill: Color = Color.White,
    val onPill: Color = bgMid,
    val again: Color = Color(0xFFE0605A),
    val hard: Color = Color(0xFFD9A54A),
    val good: Color = Color(0xFF3FA877),
    val easy: Color = Color(0xFF5B9BD5),
    val flame: Color = Color(0xFFFF9A3D),
) {
    private val ink = if (light) Color.Black else Color.White
    val glass = ink.copy(alpha = if (light) 0.05f else 0.07f)
    val glassHigh = ink.copy(alpha = if (light) 0.08f else 0.11f)
    val glassStroke = ink.copy(alpha = if (light) 0.09f else 0.08f)
    val flameEmpty = ink.copy(alpha = 0.2f)
    val scrim = if (light) Color(0xF2F2EEE6) else Color(0xF0060B0C)
}

//all the themes, teal is the original one
object Palettes {
    val Teal = Palette("teal", "Teal", Color(0xFF111D20), Color(0xFF172629), Color(0xFF2A4247), Color(0xFFEDF3F3), Color(0xFF93A6A8), Color(0xFF63777A))
    val Midnight = Palette("midnight", "Midnight", Color(0xFF0D1222), Color(0xFF141B31), Color(0xFF27324F), Color(0xFFEEF1FA), Color(0xFF9AA3BF), Color(0xFF66708C))
    val Sakura = Palette("sakura", "Sakura", Color(0xFF1C1216), Color(0xFF28191F), Color(0xFF4B2C38), Color(0xFFF8EEF1), Color(0xFFB99BA6), Color(0xFF806771), flame = Color(0xFFFF8FA3))
    val Matcha = Palette("matcha", "Matcha", Color(0xFF111912), Color(0xFF172219), Color(0xFF2E4430), Color(0xFFEEF4EC), Color(0xFF9DB09A), Color(0xFF687A66))
    val Sumi = Palette("sumi", "Sumi", Color(0xFF0A0A0B), Color(0xFF111113), Color(0xFF222226), Color(0xFFF2F2F2), Color(0xFFA0A0A6), Color(0xFF6B6B70))
    val Washi = Palette(
        "washi", "Washi", Color(0xFFF6F3EC), Color(0xFFEEE9DE), Color(0xFFDCD3C1), Color(0xFF1E2427), Color(0xFF5B6466), Color(0xFF8A9193),
        light = true, pill = Color(0xFF1E2427), onPill = Color(0xFFF6F3EC),
        again = Color(0xFFC94A44), hard = Color(0xFFB07A1E), good = Color(0xFF2E8A5F), easy = Color(0xFF3F7DB8), flame = Color(0xFFE5791F)
    )
    val Ocean = Palette("ocean", "Ocean", Color(0xFF07161F), Color(0xFF0B2230), Color(0xFF12425A), Color(0xFFEAF6FB), Color(0xFF8FB1C2), Color(0xFF5A7C8C), flame = Color(0xFFFFA45C))
    val Ember = Palette("ember", "Ember", Color(0xFF1A0F0B), Color(0xFF26150F), Color(0xFF52291A), Color(0xFFFBEFE8), Color(0xFFC0A091), Color(0xFF86695C), flame = Color(0xFFFFB347))
    val Lavender = Palette("lavender", "Lavender", Color(0xFF15111F), Color(0xFF1D1730), Color(0xFF3A2D5A), Color(0xFFF3EFFB), Color(0xFFA99EC4), Color(0xFF746A8E))
    val Void = Palette("void", "Void", Color(0xFF000000), Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFF5F5F5), Color(0xFF9A9A9A), Color(0xFF5E5E5E))
    val Snow = Palette(
        "snow", "Snow", Color(0xFFF7F9FB), Color(0xFFEEF2F6), Color(0xFFD9E1EA), Color(0xFF1B2430), Color(0xFF5A6675), Color(0xFF8994A2),
        light = true, pill = Color(0xFF1B2430), onPill = Color(0xFFF7F9FB),
        again = Color(0xFFC94A44), hard = Color(0xFFB07A1E), good = Color(0xFF2E8A5F), easy = Color(0xFF3F7DB8), flame = Color(0xFFE5791F)
    )
    val all = listOf(Teal, Midnight, Sakura, Matcha, Sumi, Ocean, Ember, Lavender, Void, Washi, Snow)
    fun byId(id: String) = all.firstOrNull { it.id == id } ?: Teal
}

//colors from the teal glass reference, now they follow whatever theme is picked
object Ink {
    var palette by mutableStateOf(Palettes.Teal)

    val BgTop get() = palette.bgTop
    val BgMid get() = palette.bgMid
    val BgBottom get() = palette.bgBottom

    val Glass get() = palette.glass
    val GlassHigh get() = palette.glassHigh
    val GlassStroke get() = palette.glassStroke

    val Text get() = palette.text
    val Muted get() = palette.muted
    val Faint get() = palette.faint
    val OnWhite get() = palette.onPill
    val Pill get() = palette.pill
    val FlameEmpty get() = palette.flameEmpty
    val Scrim get() = palette.scrim

    val Reject get() = palette.again
    val Reload = Color(0xFF56696C)
    val Accept get() = palette.good
    val Flame get() = palette.flame

    val Again get() = palette.again
    val Hard get() = palette.hard
    val Good get() = palette.good
    val Easy get() = palette.easy

    val background get() = Brush.verticalGradient(0f to BgTop, 0.45f to BgMid, 1f to BgBottom)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val Inter = FontFamily(inter(300), inter(400), inter(500), inter(600), inter(700))

private fun schemeFor(p: Palette) = if (p.light) lightColorScheme(
    primary = p.text, onPrimary = p.onPill, secondary = p.muted, tertiary = p.flame,
    background = p.bgMid, onBackground = p.text, surface = p.bgMid, onSurface = p.text,
    surfaceVariant = p.bgBottom, onSurfaceVariant = p.muted, surfaceContainer = p.bgMid,
    surfaceContainerHigh = p.bgBottom, surfaceContainerHighest = p.bgBottom,
    outline = p.faint, outlineVariant = p.glassStroke, error = p.again,
) else darkColorScheme(
    primary = p.text,
    onPrimary = p.onPill,
    primaryContainer = p.bgBottom,
    onPrimaryContainer = p.text,
    secondary = p.muted,
    secondaryContainer = p.bgBottom,
    onSecondaryContainer = p.text,
    tertiary = p.flame,
    tertiaryContainer = Color(0xFF3A3328),
    onTertiaryContainer = Color(0xFFFFD8B0),
    background = p.bgMid,
    onBackground = p.text,
    surface = p.bgMid,
    onSurface = p.text,
    surfaceVariant = p.bgBottom,
    onSurfaceVariant = p.muted,
    surfaceContainerLowest = p.bgTop,
    surfaceContainerLow = p.bgMid,
    surfaceContainer = p.bgMid,
    surfaceContainerHigh = p.bgBottom,
    surfaceContainerHighest = p.bgBottom,
    inverseSurface = p.text,
    inverseOnSurface = p.onPill,
    outline = p.faint,
    outlineVariant = p.glassStroke,
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
    val p = Ink.palette
    MaterialTheme(colorScheme = remember(p) { schemeFor(p) }, typography = Type, shapes = AppShapes, content = content)
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
    iconSize: Dp = 20.dp,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) Ink.Pill else Ink.GlassHigh, label = "gib")
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
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun WhitePill(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) Ink.Pill else Ink.GlassHigh,
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
    val scroll = options.size > 4
    val state = rememberScrollState()
    //where each pill sits so the picked one can slide into view
    val spots = remember { mutableStateMapOf<Int, IntRange>() }
    LaunchedEffect(selected, scroll, spots[selected]) {
        val r = spots[selected] ?: return@LaunchedEffect
        if (!scroll) return@LaunchedEffect
        val mid = (r.first + r.last) / 2 - state.viewportSize / 2
        state.animateScrollTo(mid.coerceIn(0, state.maxValue))
    }
    //fade the words out at the edges when theres more to scroll to
    val fade = if (!scroll) Modifier else Modifier
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val edge = 36.dp.toPx() / size.width
            val l = if (state.value > 0) 0f else 1f
            val r = if (state.value < state.maxValue) 0f else 1f
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = l), edge to Color.Black,
                    1f - edge to Color.Black, 1f to Color.Black.copy(alpha = r)
                ),
                blendMode = BlendMode.DstIn
            )
        }
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.Glass)
            .then(fade)
            .then(if (scroll) Modifier.horizontalScroll(state) else Modifier)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val a by animateFloatAsState(if (on) 1f else 0f, label = "pill")
            Box(
                Modifier
                    .then(if (scroll) Modifier else Modifier.weight(1f))
                    .onPlaced { c -> val x = c.positionInParent().x.toInt(); spots[i] = x..(x + c.size.width) }
                    //no clip here or it cuts the feather off
                    .clickable(interactionSource = null, indication = null) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                //very soft highlight, the edges fade out instead of a hard white block
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(horizontal = 7.dp, vertical = 6.dp)
                        .graphicsLayer { alpha = a }
                        .blur(11.dp, BlurredEdgeTreatment.Unbounded)
                        .background(Ink.Pill.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = lerp(Ink.Muted, Ink.OnWhite, a),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 9.dp, horizontal = if (scroll) 14.dp else 4.dp)
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
            .background(if (highlight) Ink.Pill.copy(alpha = 0.14f) else Ink.GlassHigh),
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
        if (!isFuture) FlameGauge(fill, Modifier.fillMaxWidth(0.78f).aspectRatio(1f), animated = isToday && total > 0)
        if (key in Achievements.markedDays) {
            Icon(Ic.star, null, Modifier.align(Alignment.TopEnd).padding(2.dp).size(10.dp), tint = Ink.Hard)
        }
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
fun FlameGauge(fill: Float, modifier: Modifier, animated: Boolean = false) {
    Box(modifier.then(if (animated) Modifier.flicker() else Modifier)) {
        Icon(Ic.flame, null, tint = Ink.FlameEmpty, modifier = Modifier.matchParentSize())
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

//makes a flame look alive, it sways and stretches a bit from the bottom
@Composable
fun Modifier.flicker(): Modifier {
    val t = rememberInfiniteTransition(label = "flame")
    val sy by t.animateFloat(1f, 1.08f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sy")
    val sx by t.animateFloat(1f, 0.95f, infiniteRepeatable(tween(680, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sx")
    val rot by t.animateFloat(-3f, 3f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "rot")
    val glow by t.animateFloat(0.86f, 1f, infiniteRepeatable(tween(340), RepeatMode.Reverse), label = "glow")
    return this.graphicsLayer {
        transformOrigin = TransformOrigin(0.5f, 1f)
        scaleX = sx; scaleY = sy; rotationZ = rot; alpha = glow
    }
}

private enum class Tab(val icon: ImageVector, val label: String) {
    TODAY(Ic.home, "Today"),
    STUDY(Ic.cards, "Study"),
    WORDS(Ic.bookmark, "Words"),
    SETTINGS(Ic.settings, "Settings"),
}

val BarClearance = PaddingValues(bottom = 124.dp)

//widgets can ask the app to open on a tab
object AppNav {
    val tab = kotlinx.coroutines.flow.MutableStateFlow(-1)
    fun take(i: Intent?) {
        val t = i?.getIntExtra(Widgets.EXTRA_TAB, -1) ?: -1
        if (t >= 0) { tab.value = t; i?.removeExtra(Widgets.EXTRA_TAB) }
    }
}

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
    val asked by AppNav.tab.collectAsStateWithLifecycle()
    //back goes to the tab u came from instead of closing the app
    val visited = remember { mutableStateListOf<Int>() }
    fun go(t: Int) { if (t != tab) { visited.remove(t); visited.add(tab); tab = t } }
    BackHandler(enabled = visited.isNotEmpty() || tab != Tab.TODAY.ordinal) {
        tab = if (visited.isNotEmpty()) visited.removeAt(visited.lastIndex) else Tab.TODAY.ordinal
    }
    LaunchedEffect(asked) { if (asked >= 0) { go(asked); AppNav.tab.value = -1 } }
    val haze = rememberHazeState()

    //bar slides away when u scroll down and comes back when u scroll up
    var barShown by remember { mutableStateOf(true) }
    LaunchedEffect(tab) { barShown = true }
    val hideOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f) barShown = false else if (available.y > 6f) barShown = true
                return Offset.Zero
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        DailyWordManager.refresh(ctx)
        onPauseOrDispose { }
    }

    LaunchedEffect(tick) { Achievements.announce(ctx) }
    AchievementQueue()

    var update by remember { mutableStateOf<Updater.Release?>(null) }
    LaunchedEffect(Unit) { Updater.check(ctx, manual = false).getOrNull()?.let { update = it } }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        DailyWordManager.refresh(ctx)
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Box(Modifier.fillMaxSize().background(Ink.background).nestedScroll(hideOnScroll)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "tab",
            modifier = Modifier.fillMaxSize().hazeSource(haze).statusBarsPadding()
        ) { current ->
            when (Tab.entries[current]) {
                Tab.TODAY -> TodayScreen(tick, snackbar, onStudy = { go(Tab.STUDY.ordinal) })
                Tab.STUDY -> StudyScreen(tick)
                Tab.WORDS -> WordsScreen(tick, snackbar)
                Tab.SETTINGS -> SettingsScreen(tick, snackbar)
            }
        }

        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp)
        )

        //blur behind the bar, feathered so theres no line at the top. stuff under it just melts away
        val barVisible = barShown && !WindowInsets.isImeVisible
        val blurAlpha by animateFloatAsState(if (barVisible) 1f else 0f, tween(220), label = "blur")
        val feather = Brush.verticalGradient(0f to Color.Transparent, 0.35f to Color.Black.copy(alpha = 0.55f), 0.7f to Color.Black, 1f to Color.Black)
        val shade = Brush.verticalGradient(0f to Color.Transparent, 0.5f to Ink.BgBottom.copy(alpha = 0.35f), 1f to Ink.BgBottom.copy(alpha = 0.8f))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(170.dp)
                .graphicsLayer { alpha = blurAlpha }
                .then(
                    //real blur needs android 12, older phones just get the soft shade
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.hazeBlur(
                        HazeInput.Sources(haze),
                        style = HazeBlurStyle {
                            blurRadius(24.dp)
                            noiseFactor(0f)
                            mask(feather)
                            progressive(HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f))
                        }
                    ) else Modifier
                )
                .background(shade)
        )

        //hide the bar when keyboard is up or it covers the input
        AnimatedVisibility(
            visible = barVisible,
            enter = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingBar(
                selected = tab,
                studyBadge = due,
                showAdd = tab == Tab.WORDS.ordinal,
                onSelect = { go(it) },
                onAdd = { showAdd = true },
                modifier = Modifier.navigationBarsPadding().padding(bottom = 14.dp)
            )
        }
        AchievementPopup(focus = false, haze = haze)
    }

    update?.let { r -> UpdateDialog(r) { update = null } }

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
    showAdd: Boolean,
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
                    GlassIconButton(t.icon, t.label, selected = selected == i, size = 56.dp, iconSize = 24.dp) { onSelect(i) }
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
        AnimatedVisibility(showAdd, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            Box(
                Modifier
                    .clip(shape)
                    .background(Ink.BgMid.copy(alpha = 0.92f))
                    .border(1.dp, Ink.GlassStroke, shape)
                    .padding(8.dp)
            ) {
                GlassIconButton(Ic.add, "Add word", size = 56.dp, iconSize = 24.dp, onClick = onAdd)
            }
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

    //a real anki package, opens in anki desktop and ankidroid
    fun shareApkg(ctx: Context) {
        val words = AcceptedStore.all(ctx).reversed()
        if (words.isEmpty()) { Toast.makeText(ctx, R.string.err_no_accepted, Toast.LENGTH_SHORT).show(); return }
        val f = java.io.File(ctx.cacheDir, "exports/KanjiLock.apkg").apply { parentFile?.mkdirs() }
        val ok = runCatching {
            f.outputStream().use { out ->
                ApkgWriter.write("KanjiLock", words.map { ApkgWriter.Note(it.key, it.word, it.reading, it.meaning, it.exampleLine()) }, out, java.io.File(ctx.cacheDir, "exports")) { AndroidSqlSink(it) }
            }
        }.isSuccess
        if (!ok) { Toast.makeText(ctx, R.string.export_apkg_failed, Toast.LENGTH_LONG).show(); return }
        share(ctx, f, "application/apkg", "KanjiLock.apkg")
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
    private var waiting: String? = null //listen mode asks before the voice is loaded
    private val tts: TextToSpeech = TextToSpeech(appCtx) { status ->
        ready = status == TextToSpeech.SUCCESS
        waiting?.let { waiting = null; speak(it) }
    }

    fun speak(text: String) {
        if (!ready) { waiting = text; return }
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
    private val ids = IntArray(5)

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
        ids[4] = p.load(app, R.raw.sfx_achievement, 1)
        pool = p
    }
    fun play(ctx: Context, grade: Int) {
        if (!Prefs.sfx(ctx)) return
        init(ctx)
        pool?.play(ids[grade.coerceIn(0, 3)], 0.9f, 0.9f, 1, 0, 1f)
    }
    fun achievement(ctx: Context) {
        if (!Prefs.sfx(ctx)) return
        init(ctx)
        pool?.play(ids[4], 1f, 1f, 2, 0, 1f)
    }
}

//icons as code instead of 25 xml files
object Ic {
    private fun v(name: String, d: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.White)).build()

    val add by lazy { v("add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }
    val star by lazy { v("star", "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z") }
    val trophy by lazy { v("trophy", "M19,5h-2V3H7v2H5C3.9,5 3,5.9 3,7v1c0,2.55 1.92,4.63 4.39,4.94c0.63,1.5 1.98,2.63 3.61,2.96V19H7v2h10v-2h-4v-3.1c1.63,-0.33 2.98,-1.46 3.61,-2.96C19.08,12.63 21,10.55 21,8V7C21,5.9 20.1,5 19,5zM5,8V7h2v3.82C5.84,10.4 5,9.3 5,8zM19,8c0,1.3 -0.84,2.4 -2,2.82V7h2V8z") }
    val stats by lazy { v("stats", "M5,9.2h3V19H5V9.2zM10.6,5h2.8v14h-2.8V5zM16.2,13H19v6h-2.8V13z") }
    val pen by lazy { v("pen", "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z") }
    val play by lazy { v("play", "M8,5v14l11,-7z") }
    val fullscreen by lazy { v("fullscreen", "M7,14H5v5h5v-2H7v-3zM5,10h2V7h3V5H5v5zm12,7h-3v2h5v-5h-2v3zM14,5v2h3v3h2V5h-5z") }
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
    const val FLAME_PATH = "M13.5,0.67s0.74,2.65 0.74,4.8c0,2.06 -1.35,3.73 -3.41,3.73 -2.07,0 -3.63,-1.67 -3.63,-3.73l0.03,-0.36C5.21,7.51 4,10.62 4,14c0,4.42 3.58,8 8,8s8,-3.58 8,-8C20,8.61 17.41,3.8 13.5,0.67zM11.71,19c-1.78,0 -3.22,-1.4 -3.22,-3.14 0,-1.62 1.05,-2.76 2.81,-3.12 1.77,-0.36 3.6,-1.21 4.62,-2.58 0.39,1.29 0.59,2.65 0.59,4.04 0,2.65 -2.15,4.8 -4.8,4.8z"
    val flame by lazy { v("flame", FLAME_PATH) }
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

//the popup when a new github release is out
@Composable
fun UpdateDialog(r: Updater.Release, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<Float?>(null) }
    var apk by remember { mutableStateOf<java.io.File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var needsPermission by remember { mutableStateOf(false) }

    fun install(f: java.io.File) {
        if (Updater.canInstall(ctx)) { needsPermission = false; Updater.install(ctx, f) }
        else { needsPermission = true; Updater.askInstallPermission(ctx) }
    }
    fun start() {
        if (r.apkUrl == null) { error = ctx.getString(R.string.update_no_apk); return }
        error = null; progress = 0f
        scope.launch {
            runCatching { Updater.download(ctx, r) { p -> progress = p } }
                .onSuccess { apk = it; progress = null; install(it) }
                .onFailure { progress = null; error = ctx.getString(R.string.update_failed, it.message ?: "") }
        }
    }

    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        containerColor = Ink.BgMid,
        title = {
            Column {
                Text(stringResource(R.string.update_title), color = Ink.Text)
                Text(stringResource(R.string.update_version, r.version, Updater.current(ctx)), style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
            }
        },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                if (r.notes.isNotBlank()) Text(r.notes, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                progress?.let { p ->
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth(), color = Ink.Good, trackColor = Ink.GlassHigh)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.update_downloading, (p * 100).toInt()), style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
                }
                if (needsPermission) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.update_allow), style = MaterialTheme.typography.labelMedium, color = Ink.Hard)
                }
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = Ink.Again)
                    TextButton(onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(r.page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text(stringResource(R.string.update_open_page), color = Ink.Text) }
                }
            }
        },
        confirmButton = {
            val f = apk
            TextButton(enabled = progress == null, onClick = { if (f != null) install(f) else start() }) {
                Text(stringResource(if (f != null) R.string.update_install else R.string.update_now), color = Ink.Good)
            }
        },
        dismissButton = {
            if (progress == null) Row {
                TextButton(onClick = { Prefs.setSkippedTag(ctx, r.tag); onDismiss() }) { Text(stringResource(R.string.update_skip), color = Ink.Muted) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later), color = Ink.Text) }
            }
        }
    )
}


fun achName(ctx: Context, a: Achievements.A): String {
    val arr = when (a.kind) {
        Achievements.Kind.STREAK -> R.array.ach_names_streak
        Achievements.Kind.KEPT -> R.array.ach_names_kept
        Achievements.Kind.REVIEWS -> R.array.ach_names_reviews
        Achievements.Kind.MATURE -> R.array.ach_names_mature
        Achievements.Kind.READING -> R.array.ach_names_reading
        Achievements.Kind.READING_COMBO -> R.array.ach_names_reading_combo
        Achievements.Kind.MEANING -> R.array.ach_names_meaning
        Achievements.Kind.MEANING_COMBO -> R.array.ach_names_meaning_combo
        Achievements.Kind.REVERSE -> R.array.ach_names_reverse
        Achievements.Kind.REVERSE_COMBO -> R.array.ach_names_reverse_combo
        Achievements.Kind.LISTEN -> R.array.ach_names_listen
        Achievements.Kind.LISTEN_COMBO -> R.array.ach_names_listen_combo
        Achievements.Kind.FLAWLESS -> R.array.ach_names_flawless
    }
    return ctx.resources.getStringArray(arr).getOrElse(a.tier) { "" }
}

fun achDesc(ctx: Context, a: Achievements.A): String = when (a.kind) {
    Achievements.Kind.STREAK -> ctx.getString(R.string.ach_desc_streak, a.goal)
    Achievements.Kind.KEPT -> ctx.getString(R.string.ach_desc_kept, a.goal)
    Achievements.Kind.REVIEWS -> ctx.getString(R.string.ach_desc_reviews, a.goal)
    Achievements.Kind.MATURE -> ctx.getString(R.string.ach_desc_mature, a.goal)
    Achievements.Kind.READING -> ctx.getString(R.string.ach_desc_reading, a.goal)
    Achievements.Kind.READING_COMBO -> ctx.getString(R.string.ach_desc_reading_combo, a.goal)
    Achievements.Kind.MEANING -> ctx.getString(R.string.ach_desc_meaning, a.goal)
    Achievements.Kind.MEANING_COMBO -> ctx.getString(R.string.ach_desc_meaning_combo, a.goal)
    Achievements.Kind.REVERSE -> ctx.getString(R.string.ach_desc_reverse, a.goal)
    Achievements.Kind.REVERSE_COMBO -> ctx.getString(R.string.ach_desc_reverse_combo, a.goal)
    Achievements.Kind.LISTEN -> ctx.getString(R.string.ach_desc_listen, a.goal)
    Achievements.Kind.LISTEN_COMBO -> ctx.getString(R.string.ach_desc_listen_combo, a.goal)
    Achievements.Kind.FLAWLESS -> ctx.getString(R.string.ach_desc_flawless)
}

fun groupName(g: Achievements.Group) = when (g) {
    Achievements.Group.STREAK -> R.string.ach_group_streak
    Achievements.Group.WORDS -> R.string.ach_group_words
    Achievements.Group.REVIEWS -> R.string.ach_group_reviews
    Achievements.Group.READING -> R.string.ach_group_reading
    Achievements.Group.MEANING -> R.string.ach_group_meaning
    Achievements.Group.REVERSE -> R.string.ach_group_reverse
    Achievements.Group.LISTEN -> R.string.ach_group_listen
    Achievements.Group.SPECIAL -> R.string.ach_group_special
}

fun groupIcon(g: Achievements.Group) = when (g) {
    Achievements.Group.STREAK -> Ic.flame
    Achievements.Group.WORDS -> Ic.bookmark
    Achievements.Group.REVIEWS -> Ic.cards
    Achievements.Group.READING -> Ic.book
    Achievements.Group.MEANING -> Ic.search
    Achievements.Group.REVERSE -> Ic.refresh
    Achievements.Group.LISTEN -> Ic.volume
    Achievements.Group.SPECIAL -> Ic.star
}

//whats on screen right now, null when nothing. more is for the "and 5 more" one
class Popup(val a: Achievements.A?, val more: Int = 0)
object Popups {
    var showing by mutableStateOf<Popup?>(null)
    var focusOpen by mutableIntStateOf(0) //focus mode is its own window so it shows them there instead
}

//goes thru the queue one at a time, if a bunch unlock together it shows 2 and then sums up the rest
@Composable
fun AchievementQueue() {
    val ctx = LocalContext.current
    val queued by Achievements.queue.collectAsStateWithLifecycle()
    LaunchedEffect(queued.isNotEmpty()) {
        while (Achievements.queue.value.isNotEmpty()) {
            val batch = generateSequence { Achievements.take() }.toList()
            val shows = if (batch.size > 3) batch.take(2).map { Popup(it) } + Popup(null, batch.size - 2) else batch.map { Popup(it) }
            for (p in shows) {
                Popups.showing = p
                Sfx.achievement(ctx)
                Haptics.tick(ctx)
                delay(4300)
                Popups.showing = null
                delay(650)
            }
        }
    }
}

//the steps of the popup, each one goes 0 to 1
class PopAnim {
    val shade = Animatable(0f) //blur behind it
    val dot = Animatable(0f) //the little circle in the middle
    val stretch = Animatable(0f) //circle stretching into the pill
    val title = Animatable(0f)
    val desc = Animatable(0f)
    val badge = Animatable(0f) //big circle on the left
    val cup = Animatable(0f) //trophy inside it
    suspend fun reset() = listOf(shade, dot, stretch, title, desc, badge, cup).forEach { it.snapTo(0f) }
}

//circle pops in the middle, stretches out, text drops in, then the badge and the trophy pop
private suspend fun PopAnim.enter() = coroutineScope {
    launch { shade.animateTo(1f, tween(320)) }
    dot.animateTo(1f, tween(260, easing = PopOut)) //springs take too long to settle here
    delay(120)
    stretch.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    launch { title.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 520f)) }
    delay(90)
    launch { desc.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 520f)) }
    delay(120)
    launch { badge.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 480f)) }
    delay(110)
    cup.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 440f))
}

//same thing backwards but quick
private suspend fun PopAnim.exit() = coroutineScope {
    cup.animateTo(0f, tween(70, easing = FastOutLinearInEasing))
    badge.animateTo(0f, tween(80, easing = FastOutLinearInEasing))
    launch { desc.animateTo(0f, tween(70)) }
    delay(30)
    title.animateTo(0f, tween(70))
    stretch.animateTo(0f, tween(150, easing = FastOutSlowInEasing))
    launch { shade.animateTo(0f, tween(200)) }
    dot.animateTo(0f, tween(90, easing = FastOutLinearInEasing))
}

@Composable
fun AchievementPopup(focus: Boolean, haze: HazeState? = null) {
    val now = Popups.showing
    val want = now != null && focus == (Popups.focusOpen > 0)
    var shown by remember { mutableStateOf<Popup?>(null) }
    val anim = remember { PopAnim() }
    LaunchedEffect(want, now) {
        if (want) { anim.reset(); shown = now; anim.enter() }
        else if (shown != null) { anim.exit(); shown = null }
    }
    val p = shown ?: return
    Box(Modifier.fillMaxSize()) {
        //blur at the top so the popup reads over anything, fades out going down
        val fade = Brush.verticalGradient(0f to Color.Black, 0.55f to Color.Black.copy(alpha = 0.6f), 1f to Color.Transparent)
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .graphicsLayer { alpha = anim.shade.value }
                .then(
                    if (haze != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.hazeBlur(
                        HazeInput.Sources(haze),
                        style = HazeBlurStyle {
                            blurRadius(22.dp)
                            noiseFactor(0f)
                            mask(fade)
                            progressive(HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f))
                        }
                    ) else Modifier
                )
                .background(Brush.verticalGradient(0f to Ink.BgTop.copy(alpha = 0.75f), 0.6f to Ink.BgTop.copy(alpha = 0.3f), 1f to Color.Transparent))
        )
        AchievementPill(p.a, p.more, anim.dot.value, anim.stretch.value, anim.title.value, anim.desc.value, anim.badge.value, anim.cup.value,
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 14.dp))
    }
}

private val Gold = Color(0xFFF2C434)
private val PopOut = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f) //goes a bit past then back

//pill with a big circle hanging off the left and a trophy in it. all the floats are the animation steps
@Composable
fun AchievementPill(
    a: Achievements.A?,
    more: Int,
    dot: Float = 1f,
    stretch: Float = 1f,
    title: Float = 1f,
    desc: Float = 1f,
    badge: Float = 1f,
    cup: Float = 1f,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val w = 300.dp
    val h = 52.dp
    val d = 64.dp
    val pill = lerp(Ink.BgMid, Ink.Text, 0.07f).copy(alpha = 0.97f)
    val circle = lerp(Ink.BgMid, Ink.Text, 0.2f)
    Box(modifier.width(w).height(d)) {
        //starts as a circle in the middle then stretches both ways
        Box(
            Modifier
                .align(Alignment.Center)
                .size(width = h + (w - 8.dp - h) * stretch, height = h)
                .graphicsLayer { scaleX = dot; scaleY = dot; alpha = dot.coerceIn(0f, 1f) }
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(pill)
                .border(1.dp, Ink.GlassStroke, CircleShape)
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 16.dp + d + 12.dp, end = 18.dp)) {
            Text(
                if (a != null) achName(ctx, a) else stringResource(R.string.ach_more, more),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink.Text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = title.coerceIn(0f, 1f); translationY = (1f - title) * -10.dp.toPx(); scaleY = 0.7f + 0.3f * title; transformOrigin = TransformOrigin(0.5f, 0f) }
            )
            Text(
                if (a != null) achDesc(ctx, a) else stringResource(R.string.ach_more_sub),
                style = MaterialTheme.typography.labelSmall, color = Ink.Muted, maxLines = 2, lineHeight = 13.sp,
                modifier = Modifier.graphicsLayer { alpha = desc.coerceIn(0f, 1f); translationY = (1f - desc) * -8.dp.toPx(); scaleY = 0.7f + 0.3f * desc; transformOrigin = TransformOrigin(0.5f, 0f) }
            )
        }
        //sits a bit in from the left so the pill peeks out behind it
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(d)
                .graphicsLayer { scaleX = badge; scaleY = badge; alpha = badge.coerceIn(0f, 1f) }
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(circle),
            contentAlignment = Alignment.Center
        ) {
            Icon(Ic.trophy, null, Modifier.size(38.dp).graphicsLayer { scaleX = cup; scaleY = cup; alpha = cup.coerceIn(0f, 1f) }, tint = Gold)
        }
    }
}
