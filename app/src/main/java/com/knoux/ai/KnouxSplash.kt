package com.knoux.ai

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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

private val upperBrush = Brush.linearGradient(listOf(Color(0xfffc5bff), Color(0xffaf18ee), Color(0xff5423f8)), Offset(505f, 241f), Offset(420f, 453f))
private val bladeBrush = Brush.linearGradient(listOf(Color(0xfffb69ff), Color(0xff7924ef), Color(0xff267aff)), Offset(680f, 320f), Offset(330f, 545f))
private val lowerBrush = Brush.linearGradient(listOf(Color(0xff174ffd), Color(0xff00ecff)), Offset(500f, 425f), Offset(645f, 548f))

// ---- Neon smile geometry (traced luminous curve beneath the wordmark) ----
private val smileTip = Offset(936f, 772f)
private val smilePath: Path by lazy {
    Path().apply {
        moveTo(72f, 757f)
        cubicTo(285f, 915f, 705f, 930f, smileTip.x, smileTip.y)
    }
}
private val smileMeasure: PathMeasure by lazy { PathMeasure().apply { setPath(smilePath, false) } }
private val smileGlowPoints by lazy { List(27) { smileMeasure.getPosition(smileMeasure.length * it / 26f) } }
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

private fun DrawScope.strokeGlow(path: Path, brush: Brush, tint: Color) {
    drawPath(path, tint.copy(alpha = .055f), style = Stroke(22f, join = StrokeJoin.Round))
    drawPath(path, tint.copy(alpha = .12f), style = Stroke(11f, join = StrokeJoin.Round))
    drawPath(path, tint.copy(alpha = .33f), style = Stroke(4.8f, join = StrokeJoin.Round))
    drawPath(path, brush)
    drawPath(path, tint.copy(alpha = .85f), style = Stroke(1.6f, join = StrokeJoin.Round))
    drawPath(path, Chalk.copy(alpha = .52f), style = Stroke(.7f, join = StrokeJoin.Round))
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
    if (p1 > 0f) clipSweep(Offset(500f, 235f), Offset(-0.35f, 0.94f), 250f, p1, Pink) { strokeGlow(kUpper, upperBrush, Pink) }
    // Lower cyan is composited beneath the blade; it reveals last to complete the silhouette.
    if (p3 > 0f) clipSweep(Offset(560f, 412f), Offset(0.30f, 0.95f), 175f, p3, Cyan) { strokeGlow(kLower, lowerBrush, Cyan) }
    if (p2 > 0f) clipSweep(Offset(308f, 556f), Offset(0.845f, -0.536f), 480f, p2, Violet) { strokeGlow(kBlade, bladeBrush, Violet) }
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
private fun DrawScope.drawNeonSmile(c: SplashMotionController, segment: Path) {
    val end = SPLASH_SMILE_END_MS.toFloat()
    val p = c.eased(SMILE_START, end, PenEasing)
    if (p <= 0f) return
    val distance = smileMeasure.length * p
    segment.reset()
    smileMeasure.getSegment(0f, distance, segment, true)
    val bloom = if (c.reduced) 1f else c.scene(end, BLOOM_END)
    // Smooth bloom: overlapping soft radial stamps follow the curve (no hard stroke bands).
    val haze = p * (0.45f + 0.55f * bloom)
    val last = smileGlowPoints.size - 1f
    for (i in smileGlowPoints.indices) {
        val t = i / last
        if (t > p) break
        val edge = ((p - t) * 10f).coerceIn(0f, 1f)
        val col = smileColor(t)
        val a = haze * edge * (minOf(t, 1f - t) * 8f).coerceIn(0f, 1f)
        val pt = smileGlowPoints[i]
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.16f * a), Color.Transparent), pt, 46f), 46f, pt)
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.20f * a), Color.Transparent), pt, 24f), 24f, pt)
    }
    // Luminous body and hot core.
    drawPath(segment, smileBrush, alpha = .22f + .10f * bloom, style = Stroke(28f, cap = StrokeCap.Round))
    drawPath(segment, smileBrush, alpha = .90f, style = Stroke(13f, cap = StrokeCap.Round))
    drawPath(segment, smileCore, alpha = .93f, style = Stroke(3.7f, cap = StrokeCap.Round))
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
        drawSplashFlare(smileTip, if (c.reduced) .72f else 1f - .53f * bloom)
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
            listOf(Color(0xfff28bff), Color(0xffa952ff)), startY = 0f, endY = single.size.height.toFloat()
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
 */
@Composable
internal fun KnouxSplashRoute(
    reduced: Boolean, freezeAtMs: Long?, onSmileCompleted: () -> Unit = {}, onFinished: () -> Unit
) {
    val controller = remember(reduced, freezeAtMs) { SplashMotionController(reduced) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val smileCompleted by rememberUpdatedState(onSmileCompleted)
    val smileSegment = remember { Path() }
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (!finished) { finished = true; SplashGate.consumed = true; onFinished() }
    }
    LaunchedEffect(controller) {
        if (freezeAtMs != null) {
            controller.clock.snapTo(freezeAtMs.toFloat().coerceIn(0f, controller.totalMs))
        } else {
            if (!reduced) {
                controller.clock.animateTo(SPLASH_SMILE_END_MS.toFloat(), tween(SPLASH_SMILE_END_MS.toInt(), easing = LinearEasing))
                // Hold the completed smile for a frame even when the renderer skips over its end.
                withFrameNanos { }
                if (!finished) smileCompleted()
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
            val target = 775f * s
            if (full.size.width > 0) full = layoutAt(150f * s * target / full.size.width)
            val letters = text.mapIndexedNotNull { i, ch ->
                if (ch == ' ') null else Triple(i, full.getBoundingBox(i),
                    measurer.measure(AnnotatedString(ch.toString()), full.layoutInput.style))
            }
            WordmarkLayout(full, letters)
        }
        Canvas(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xff080521), Color(0xff040418), Color(0xff0a0422))))) {
            val bg = if (controller.reduced) 1f else controller.scene(0f, 300f)
            val center = Offset(size.width / 2f, size.height * 0.46f)
            drawCircle(Brush.radialGradient(listOf(Color(0xff351184).copy(alpha = .54f * bg),
                Color(0xff13104e).copy(alpha = .35f * bg), Color.Transparent), center, size.minDimension * 0.80f),
                size.minDimension * 0.80f, center)
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.10f * bg), Color.Transparent),
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
                    scale(1.57f, 1.57f, Offset(500f, 550f)) { drawKnouxMark(controller) }
                    scale(.80f, 1f, Offset(500f, 800f)) { drawNeonSmile(controller, smileSegment) }
                }
                drawWordmark(controller, wordmark, s)
            }
        }
    }
}
