package com.knoux.ai

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/**
 * Cinematic KNOUX AI brand signature.
 *
 * Geometry is traced from the owner-supplied logo (design/brand/knoux-logo-owner.png,
 * 1125x1125) and normalized to a 1000x1000 design space. Only the K mark, the
 * "knoux ai" wordmark and the neon smile are drawn — never the circular badge —
 * so no raster background artifact surrounds the animated brand.
 *
 * Sequence: darkness -> K forms -> wordmark writes -> neon smile -> Home.
 */
internal object SplashGate {
    /** Set once the brand signature has played in this process; prevents replay on resume. */
    @Volatile var consumed = false
}

internal class SplashMotionController(val reduced: Boolean) {
    val clock = Animatable(0f)
    val totalMs: Float get() = if (reduced) 750f else 3480f
    val fadeStart: Float get() = if (reduced) 450f else 3080f

    /** Linear 0..1 progress of a scene window, read in the draw/layer phase. */
    fun scene(start: Float, end: Float): Float = ((clock.value - start) / (end - start)).coerceIn(0f, 1f)
    fun eased(start: Float, end: Float, easing: Easing = FastOutSlowInEasing): Float =
        if (reduced) 1f else easing.transform(scene(start, end))
}

// ---- Scene timing (ms) ----
private const val K1_START = 300f;  private const val K1_END = 650f
private const val K2_START = 650f;  private const val K2_END = 1020f
private const val K3_START = 1020f; private const val K3_END = 1260f
private const val SWEEP_START = 1150f; private const val SWEEP_END = 1520f
private const val WORD_START = 1260f; private const val WORD_STAGGER = 80f; private const val WORD_DUR = 250f
private const val SMILE_START = 2080f
internal const val SPLASH_SMILE_END_MS = 2800L
private const val BLOOM_END = 3020f
private const val FACE_START = 2840f   // face reveals just after smile completes
private const val FACE_END   = 3080f

private val PenEasing = CubicBezierEasing(.32f, 0f, .64f, 1f)

// ---- K geometry (1000-unit design space, traced from the owner logo) ----
private val kUpper: Path by lazy {
    Path().apply {
        moveTo(448f, 240.9f); lineTo(561.8f, 240.9f); lineTo(484.4f, 377.8f)
        lineTo(364.4f, 453.3f); lineTo(435.6f, 253.5f); quadraticTo(435.6f, 240.9f, 448f, 240.9f); close()
    }
}
private val kBlade: Path by lazy {
    Path().apply {
        moveTo(316.4f, 550.2f); quadraticTo(370.7f, 554.5f, 424.9f, 550.2f)
        cubicTo(497.8f, 471.1f, 604.4f, 391.1f, 680f, 320f)
        cubicTo(524.4f, 362.7f, 373.3f, 475.6f, 316.4f, 550.2f); close()
    }
}
private val kLower: Path by lazy {
    Path().apply {
        moveTo(493.3f, 419.6f); lineTo(565.3f, 419.6f); lineTo(653.3f, 551.1f); lineTo(539.6f, 551.1f); close()
    }
}
private val kCombined: Path by lazy { Path().apply { addPath(kUpper); addPath(kLower); addPath(kBlade) } }

private val upperBrush = Brush.linearGradient(listOf(Color(0xffff7cff), Color(0xffc218f9), Color(0xff3c1ad6)), Offset(505f, 241f), Offset(420f, 453f))
private val bladeBrush = Brush.linearGradient(
    0f to Color(0xffff8bff), .22f to Color(0xffd33bff), .52f to Color(0xff6322f6),
    .78f to Color(0xff2a5cff), 1f to Color(0xff7ceeff), start = Offset(680f, 320f), end = Offset(316f, 550f)
)
private val lowerBrush = Brush.linearGradient(listOf(Color(0xff123fd8), Color(0xff0d7dff), Color(0xff00f0ff)), Offset(500f, 425f), Offset(645f, 548f))

// ---- Neon smile geometry (traced luminous curve beneath the wordmark) ----
private val smileTip = Offset(936f, 772f)
private val smilePath: Path by lazy {
    Path().apply {
        moveTo(72f, 757f)
        cubicTo(285f, 915f, 705f, 930f, smileTip.x, smileTip.y)
    }
}
private val smileMeasure: PathMeasure by lazy { PathMeasure().apply { setPath(smilePath, false) } }
private data class SmileEdges(val upper: Offset, val lower: Offset)

private const val SMILE_SAMPLES = 40

private fun smileEdges(progress: Float): SmileEdges {
    val distance = smileMeasure.length * progress
    val point = smileMeasure.getPosition(distance)
    val tangent = smileMeasure.getTangent(distance)
    val normal = Offset(-tangent.y, tangent.x)
    val taper = 4f * progress * (1f - progress)
    return SmileEdges(point - normal * (2.5f * taper), point + normal * (23f * taper))
}

private val smileSamples by lazy { List(SMILE_SAMPLES + 1) { smileEdges(it.toFloat() / SMILE_SAMPLES) } }
private val smileBrush = Brush.horizontalGradient(
    0f to Color.Transparent, 0.11f to Color(0xfff02bff), 0.37f to Color(0xffa749fb),
    0.62f to Color(0xff4a73ff), 0.93f to Color(0xff00e4ff), 1f to Color.Transparent,
    startX = 60f, endX = 940f
)
private val smileCore = Brush.horizontalGradient(
    0f to Color(0xffe9d5ff).copy(alpha = 0f), 0.10f to Color(0xffe9d5ff), 0.5f to Color(0xffbfdbfe),
    0.90f to Color(0xffcffafe), 1f to Color(0xffcffafe).copy(alpha = 0f), startX = 60f, endX = 940f
)
private val smileStops = listOf(
    0f to Color(0xfff02bff), 0.37f to Color(0xffa749fb), 0.62f to Color(0xff4a73ff), 1f to Color(0xff00e4ff)
)
private val kTrail = Path().apply {
    moveTo(227f, 283f)
    cubicTo(338f, 195f, 472f, 195f, 558f, 240f)
}
private val kTrailLight = Brush.horizontalGradient(
    listOf(Color.Transparent, Color(0xffa32dff), Color(0xfff58bff)), 227f, 558f
)
private val upperHighlight = Brush.radialGradient(
    listOf(Color(0xffffd7ff), Color(0xffff6eff).copy(alpha = .32f), Color.Transparent),
    Offset(551f, 241f), 95f
)
private val bladeHighlight = Brush.radialGradient(
    listOf(Color(0xffbaf5ff), Color(0xff24c7ff).copy(alpha = .39f), Color.Transparent),
    Offset(476f, 491f), 91f
)
private fun smileColor(t: Float): Color {
    for (i in 1 until smileStops.size) {
        if (t <= smileStops[i].first) {
            val (a, ca) = smileStops[i - 1]; val (b, cb) = smileStops[i]
            return lerp(ca, cb, ((t - a) / (b - a)).coerceIn(0f, 1f))
        }
    }
    return smileStops.last().second
}

/** Reveals a stroke by sweeping a clip frontier along [dir] from [start]. */
private fun DrawScope.clipSweep(
    start: Offset, dir: Offset, length: Float, progress: Float,
    tint: Color, block: DrawScope.() -> Unit
) {
    if (progress <= 0f) return
    if (progress >= 1f) { block(); return }
    val d = dir / sqrt(dir.x * dir.x + dir.y * dir.y)
    val n = Offset(-d.y, d.x)
    val f = start + d * (length * progress)
    val m = 1600f
    val poly = Path().apply {
        moveTo(f.x + n.x * m, f.y + n.y * m); lineTo(f.x - n.x * m, f.y - n.y * m)
        lineTo(f.x - n.x * m - d.x * 2 * m, f.y - n.y * m - d.y * 2 * m)
        lineTo(f.x + n.x * m - d.x * 2 * m, f.y + n.y * m - d.y * 2 * m); close()
    }
    clipPath(poly) { block() }
    // Luminous nib at the frontier: the K reads as if a light pen were writing it.
    drawPenTip(f, d, tint, if (progress < 0.08f) progress / 0.08f else 1f)
}

/** Hot core plus a short comet tail; all positions are in the 1000-unit design space. */
private fun DrawScope.drawPenTip(point: Offset, direction: Offset, tint: Color, strength: Float) {
    if (strength <= 0f) return
    for (i in 1..5) {
        val back = point - direction * (i * 10.5f)
        val alpha = (0.16f - i * 0.025f).coerceAtLeast(0.025f) * strength
        val radius = (6.4f - i * 1.0f).coerceAtLeast(1.8f)
        drawCircle(tint.copy(alpha = alpha), radius, back)
    }
    drawCircle(
        Brush.radialGradient(
            listOf(Chalk.copy(alpha = .62f * strength), tint.copy(alpha = .34f * strength), Color.Transparent),
            point, 38f
        ), 38f, point
    )
    drawCircle(Chalk.copy(alpha = .94f * strength), 3.4f, point)
    drawCircle(Chalk.copy(alpha = strength), 1.5f, point)
}

private fun DrawScope.strokeGlow(path: Path, brush: Brush, tint: Color, highlight: Brush? = null) {
    drawPath(path, tint.copy(alpha = .09f), style = Stroke(26f, join = StrokeJoin.Round))
    drawPath(path, tint.copy(alpha = .20f), style = Stroke(13f, join = StrokeJoin.Round))
    drawPath(path, tint.copy(alpha = .52f), style = Stroke(5.2f, join = StrokeJoin.Round))
    drawPath(path, brush)
    if (highlight != null) {
        drawPath(path, highlight, alpha = .50f)
        drawPath(path, highlight, alpha = 1f, style = Stroke(2.8f, join = StrokeJoin.Round))
    }
    drawPath(path, tint.copy(alpha = .98f), style = Stroke(1.7f, join = StrokeJoin.Round))
    drawPath(path, Chalk.copy(alpha = .78f), style = Stroke(.8f, join = StrokeJoin.Round))
}

/** Scene B — the K constructs itself stroke by stroke, then a restrained light sweep. */
private fun DrawScope.drawKnouxMark(c: SplashMotionController) {
    val p1 = c.eased(K1_START, K1_END, PenEasing)
    val p2 = c.eased(K2_START, K2_END, PenEasing)
    val p3 = c.eased(K3_START, K3_END, PenEasing)
    val trail = if (c.reduced) .48f else c.eased(K1_END, K2_END) * .72f
    if (trail > 0f) {
        drawPath(kTrail, kTrailLight, alpha = trail * .17f, style = Stroke(12f))
        drawPath(kTrail, kTrailLight, alpha = trail, style = Stroke(1.65f))
    }
    if (p1 > 0f) clipSweep(Offset(500f, 235f), Offset(-0.35f, 0.94f), 250f, p1, Pink) {
        strokeGlow(kUpper, upperBrush, Pink, upperHighlight)
    }
    // Lower cyan is composited beneath the blade; it reveals last to complete the silhouette.
    if (p3 > 0f) clipSweep(Offset(560f, 412f), Offset(0.30f, 0.95f), 175f, p3, Cyan) { strokeGlow(kLower, lowerBrush, Cyan) }
    if (p2 > 0f) clipSweep(Offset(308f, 556f), Offset(0.845f, -0.536f), 480f, p2, Violet) {
        strokeGlow(kBlade, bladeBrush, Violet, bladeHighlight)
    }
    val sw = c.scene(SWEEP_START, SWEEP_END)
    if (!c.reduced && sw > 0.02f && sw < 0.98f && p3 >= 1f) {
        val a = Offset(300f, 560f); val b = Offset(700f, 300f)
        val center = a + (b - a) * sw
        val half = (b - a) * 0.10f
        clipPath(kCombined) {
            drawRect(Brush.linearGradient(listOf(Color.Transparent, Chalk.copy(alpha = 0.22f), Color.Transparent), center - half, center + half),
                topLeft = Offset(290f, 225f), size = Size(430f, 345f))
        }
    }
}

/** Scene D — the luminous smile draws left to right, then blooms softly. */
private fun DrawScope.drawNeonSmile(c: SplashMotionController, segment: Path, ribbon: Path) {
    val end = SPLASH_SMILE_END_MS.toFloat()
    val p = c.eased(SMILE_START, end, PenEasing)
    if (p <= 0f) return
    val distance = smileMeasure.length * p
    segment.reset()
    smileMeasure.getSegment(0f, distance, segment, true)
    val bloom = if (c.reduced) 1f else c.scene(end, BLOOM_END)
    val sampleEnd = (p * SMILE_SAMPLES).toInt()
    val endEdges = smileEdges(p)
    ribbon.reset()
    ribbon.moveTo(smileSamples.first().upper.x, smileSamples.first().upper.y)
    for (i in 1..sampleEnd) smileSamples[i].upper.let { ribbon.lineTo(it.x, it.y) }
    ribbon.lineTo(endEdges.upper.x, endEdges.upper.y)
    ribbon.lineTo(endEdges.lower.x, endEdges.lower.y)
    for (i in sampleEnd downTo 0) smileSamples[i].lower.let { ribbon.lineTo(it.x, it.y) }
    ribbon.close()
    val glow = .62f + .38f * bloom
    drawPath(segment, smileBrush, alpha = .10f * glow, style = Stroke(64f, cap = StrokeCap.Round))
    drawPath(segment, smileBrush, alpha = .19f * glow, style = Stroke(42f, cap = StrokeCap.Round))
    drawPath(segment, smileBrush, alpha = .34f * glow, style = Stroke(25f, cap = StrokeCap.Round))
    drawPath(ribbon, smileBrush)
    drawPath(ribbon, smileCore, alpha = .52f)
    drawPath(segment, smileCore, alpha = 1f, style = Stroke(3.2f, cap = StrokeCap.Round))
    if (!c.reduced && p < 1f) {
        val tip = smileMeasure.getPosition(distance)
        val behind = smileMeasure.getPosition((distance - 14f).coerceAtLeast(0f))
        val delta = tip - behind
        val magnitude = delta.getDistance()
        val direction = if (magnitude > .01f) delta / magnitude else Offset(1f, 0f)
        val strength = (p / .04f).coerceAtMost(1f)
        drawPenTip(tip, direction, smileColor(p), strength)
    }
    if (p >= 1f) {
        drawSplashFlare(smileTip, if (c.reduced) .72f else 1f - .33f * bloom)
    }
}

private class WordmarkLayout(val full: TextLayoutResult, val letters: List<Triple<Int, androidx.compose.ui.geometry.Rect, TextLayoutResult>>)

/** Scene C — "knoux ai" writes itself letter by letter with preserved kerning. */
private fun DrawScope.drawWordmark(c: SplashMotionController, wm: WordmarkLayout, s: Float) {
    val full = wm.full
    val left = (1000f * s - full.size.width) / 2f
    val top = 639f * s - full.size.height / 2f
    val baseline = full.firstBaseline
    for ((i, box, single) in wm.letters) {
        val start = WORD_START + i * WORD_STAGGER
        val p = c.eased(start, start + WORD_DUR, PenEasing)
        if (p <= 0f) continue
        val x = left + box.left
        val y = top + baseline - single.firstBaseline
        val brush = if (i >= 6) Brush.verticalGradient(
            listOf(Color(0xffff7dff), Color(0xffe052ff), Color(0xffa32bff)), startY = 0f, endY = single.size.height.toFloat()
        ) else SolidColor(Chalk)
        val reveal = x + single.size.width * p
        // Compose fakes italics with a horizontal skew, which pushes tall ink
        // (the i-dot, stem tops) past the glyph advance. While a letter is still
        // being written the clip stays tight so the pen edge stays crisp; once
        // the letter has fully landed, widen the clip by that skew overhang so
        // completed glyphs are not sheared off at a vertical edge.
        val settle = if (p >= 0.999f) single.size.height * 0.34f else 4f * s
        clipRect(left = x - 4f * s, right = reveal + settle, top = y, bottom = y + single.size.height) {
            drawText(single, brush = brush, topLeft = Offset(x, y))
        }
    }
}

/**
 * The brand signature overlay. Composed above the app scaffold; fades into Home.
 * [freezeAtMs] (debug only) holds a single frame for deterministic scene capture.
 * [onSmileCompleted] fires once at the end of the moving smile on a live launch.
 * [onEnter] is called when the user taps the CTA button at the end of the sequence.
 */
@Composable
internal fun KnouxSplashRoute(
    reduced: Boolean, freezeAtMs: Long?, onSmileCompleted: () -> Unit = {},
    onEnter: (() -> Unit)? = null,
    onFinished: () -> Unit
) {
    val controller = remember(reduced, freezeAtMs) { SplashMotionController(reduced) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val smileCompleted by rememberUpdatedState(onSmileCompleted)
    val smileSegment = remember { Path() }
    val smileRibbon = remember { Path() }
    var finished by remember { mutableStateOf(false) }
    // CTA button state: appears after smile completes, only when onEnter is set
    var showEnterButton by remember { mutableStateOf(false) }
    val buttonAlpha by animateFloatAsState(
        if (showEnterButton) 1f else 0f,
        tween(600, easing = FastOutSlowInEasing), label = "btn-alpha"
    )
    val buttonOffset by animateFloatAsState(
        if (showEnterButton) 0f else 40f,
        tween(600, easing = FastOutSlowInEasing), label = "btn-offset"
    )
    fun finish() {
        if (!finished) { finished = true; SplashGate.consumed = true; onFinished() }
    }
    LaunchedEffect(controller) {
        if (freezeAtMs != null) {
            controller.clock.snapTo(freezeAtMs.toFloat().coerceIn(0f, controller.totalMs))
        } else {
            if (!reduced) {
                controller.clock.animateTo(SPLASH_SMILE_END_MS.toFloat(), tween(SPLASH_SMILE_END_MS.toInt(), easing = LinearEasing))
                withFrameNanos { }
                if (!finished) smileCompleted()
                // If there's an enter button, show it and wait for the tap instead of auto-finishing
                if (onEnter != null) {
                    showEnterButton = true
                    return@LaunchedEffect  // don't auto-finish; wait for user tap
                }
            }
            val remainingMs = (controller.totalMs - controller.clock.value).toInt()
            controller.clock.animateTo(controller.totalMs, tween(remainingMs, easing = LinearEasing))
            finish()
        }
    }
    // Watchdog: never trap the user on the splash even if the animation stalls.
    LaunchedEffect(controller) {
        if (freezeAtMs == null) { delay(controller.totalMs.toLong() + 2500L); finish() }
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("knoux-splash").graphicsLayer {
        alpha = if (freezeAtMs != null) 1f else 1f - controller.scene(controller.fadeStart, controller.totalMs)
    }) {
        val s = with(density) { minOf(maxWidth.toPx(), maxHeight.toPx()) } * 0.92f / 1000f
        val wordmark = remember(measurer, s, density) {
            val text = "knoux ai"
            fun layoutAt(fontPx: Float): TextLayoutResult {
                val size = with(density) { fontPx.toSp() }
                return measurer.measure(AnnotatedString(text),
                    TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic, fontSize = size))
            }
            var full = layoutAt(150f * s)
            val target = 820f * s
            if (full.size.width > 0) full = layoutAt(150f * s * target / full.size.width)
            val letters = text.mapIndexedNotNull { i, ch ->
                if (ch == ' ') null else Triple(i, full.getBoundingBox(i),
                    measurer.measure(AnnotatedString(ch.toString()), full.layoutInput.style))
            }
            WordmarkLayout(full, letters)
        }
        Canvas(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xff170a48), Color(0xff050318), Color(0xff120530))))) {
            val bg = if (controller.reduced) 1f else controller.scene(0f, 300f)
            val center = Offset(size.width / 2f, size.height * 0.46f)
            // Deep violet core, then two off-axis accents so the darkness is lit, not flat.
            drawCircle(Brush.radialGradient(listOf(Color(0xff4a16b4).copy(alpha = .74f * bg),
                Color(0xff1a1266).copy(alpha = .50f * bg), Color.Transparent), center, size.minDimension * 0.80f),
                size.minDimension * 0.80f, center)
            drawCircle(Brush.radialGradient(listOf(Color(0xffff3df0).copy(alpha = .20f * bg), Color.Transparent),
                Offset(size.width * 0.92f, size.height * 0.10f), size.minDimension * 0.46f),
                size.minDimension * 0.46f, Offset(size.width * 0.92f, size.height * 0.10f))
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .20f * bg), Color.Transparent),
                Offset(size.width * 0.18f, size.height * 0.90f), size.minDimension * 0.52f),
                size.minDimension * 0.52f, Offset(size.width * 0.18f, size.height * 0.90f))
            val light = controller.eased(120f, 940f)
            val drift = if (controller.reduced) 0f else controller.scene(120f, controller.totalMs) * 34f - 17f
            drawSplashRibbons(light, drift)
            val ox = (size.width - 1000f * s) / 2f
            val oy = (size.height - 1000f * s) / 2f - 58f * s
            translate(ox, oy) {
                scale(s, s, Offset.Zero) {
                    drawSplashOrbit(controller.eased(400f, 1780f), controller.scene(400f, 2700f))
                    scale(1.62f, 1.62f, Offset(500f, 550f)) { drawKnouxMark(controller) }
                    scale(.80f, 1f, Offset(500f, 800f)) { drawNeonSmile(controller, smileSegment, smileRibbon) }
                    // Happy face blooms beside the smile tip after it finishes
                    val faceP = controller.eased(FACE_START, FACE_END, PenEasing)
                    if (faceP > 0f) drawHappyFaceMark(faceP)
                }
                drawWordmark(controller, wordmark, s)
            }
        }

        // ── Enter CTA button — appears after smile completes ─────────────
        if (onEnter != null && (showEnterButton || freezeAtMs != null)) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 40.dp, bottom = 64.dp)
                    .graphicsLayer {
                        alpha = buttonAlpha
                        translationY = buttonOffset
                    }
            ) {
                SplashEnterButton(onTap = {
                    finish()
                    onEnter()
                })
            }
        }
    }
}

/**
 * A tiny neon happy face (circle + two eyes + smile arc) that blooms
 * at the tip of the neon smile curve once it fully completes.
 * Drawn in the 1000-unit design space; center is near the smile tip.
 */
private fun DrawScope.drawHappyFaceMark(progress: Float) {
    if (progress <= 0f) return
    // Center: just left of the smile tip, slightly above
    val cx = 870f; val cy = 730f; val r = 42f * progress
    if (r < 1f) return

    val faceColor = Color(0xffffd700) // warm golden
    val eyeColor  = Color(0xffffffff)

    // Outer glow halo
    drawCircle(Brush.radialGradient(
        listOf(faceColor.copy(alpha = .22f * progress), Color.Transparent),
        Offset(cx, cy), r * 2.4f
    ), r * 2.4f, Offset(cx, cy))

    // Face circle (outline only, neon style)
    val faceGlow = listOf(16f to .08f, 10f to .18f, 5f to .36f, 2.6f to .68f)
    for ((w, a) in faceGlow) {
        drawCircle(faceColor.copy(alpha = a * progress), r, Offset(cx, cy), style = Stroke(w))
    }
    // Bright inner rim
    drawCircle(Color.White.copy(alpha = .82f * progress), r, Offset(cx, cy), style = Stroke(1.1f))

    // Eyes — two bright dots with a small glow
    val eyeOffX = r * 0.34f; val eyeOffY = r * 0.26f
    for (sign in listOf(-1f, 1f)) {
        val ex = cx + sign * eyeOffX; val ey = cy - eyeOffY
        drawCircle(faceColor.copy(alpha = .22f * progress), r * 0.22f, Offset(ex, ey))
        drawCircle(eyeColor.copy(alpha = progress), r * 0.09f, Offset(ex, ey))
    }

    // Smile arc — a cubic bezier mouth drawn as a path
    val mouthPath = Path().apply {
        val mx = cx - r * 0.42f; val my = cy + r * 0.14f
        moveTo(mx, my)
        cubicTo(cx - r * 0.20f, cy + r * 0.68f,
                cx + r * 0.20f, cy + r * 0.68f,
                cx + r * 0.42f, my)
    }
    for ((w, a) in listOf(8f to .10f, 5f to .22f, 2.8f to .52f)) {
        drawPath(mouthPath, faceColor.copy(alpha = a * progress), style = Stroke(w, cap = StrokeCap.Round))
    }
    drawPath(mouthPath, Color.White.copy(alpha = .88f * progress), style = Stroke(1.1f, cap = StrokeCap.Round))

    // Cheek blushes
    for (sign in listOf(-1f, 1f)) {
        val bx = cx + sign * r * 0.60f; val by = cy + r * 0.30f
        drawCircle(Color(0xffff88aa).copy(alpha = .30f * progress), r * 0.20f, Offset(bx, by))
    }
}

/**
 * The glowing brand CTA that sits at the bottom of the splash screen once the
 * smile animation completes. Full brand gradient, travelling shimmer, haptic tap.
 */
@Composable
private fun SplashEnterButton(onTap: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(100), label = "btn-press")
    val infinite = rememberInfiniteTransition(label = "btn-shimmer")
    val shimmer = infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "shimmer")
    val glow = infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }) {
        // Outer bloom
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            val cx = size.width / 2f; val cy = size.height / 2f
            val pulse = glow.value
            for ((r, a) in listOf(size.width * .55f to .22f, size.width * .38f to .30f, size.width * .22f to .20f)) {
                drawCircle(Brush.radialGradient(
                    listOf(Color(0xffff36f5).copy(alpha = a * (0.6f + pulse * 0.4f)),
                        Color(0xff5522ff).copy(alpha = a * .7f), Color.Transparent),
                    Offset(cx, cy), r), r, Offset(cx, cy))
            }
        }

        // Button body
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .drawBehind {
                    val w = size.width; val h = size.height
                    val rad = CornerRadius(h / 2f)
                    // Brand gradient
                    drawRoundRect(Brush.horizontalGradient(listOf(
                        Color(0xffff36f5), Color(0xffa826ff), Color(0xff344dff), Color(0xff00d8ff)
                    )), cornerRadius = rad)
                    // Shimmer
                    val travel = shimmer.value * w * 1.6f - w * .3f
                    drawRoundRect(Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = .32f), Color.Transparent),
                        start = Offset(travel - w * .22f, 0f), end = Offset(travel + w * .22f, h)
                    ), cornerRadius = rad)
                    // Top lip
                    drawRoundRect(Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = .40f), Color.Transparent), 0f, h * .52f
                    ), cornerRadius = rad)
                    // Rim
                    drawRoundRect(Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = .90f), Color.Transparent, Cyan.copy(alpha = .80f))
                    ), cornerRadius = rad, style = Stroke(1.4.dp.toPx()))
                }
                .clickable(interactionSource = interaction, indication = null, role = androidx.compose.ui.semantics.Role.Button) {
                    TouchFeedback.tap(context)
                    onTap()
                }
                .testTag("splash-enter"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Glyph(Mark.K, Modifier.size(20.dp), Chalk, true)
                Label(
                    ("Enter KNOUX AI" ar "ادخل KNOUX AI").localized(),
                    17, Chalk, FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    Glyph(Mark.NEXT, Modifier.size(13.dp), Chalk)
                    Glyph(Mark.NEXT, Modifier.size(13.dp), Chalk.copy(alpha = .5f))
                }
            }
        }
    }
}
