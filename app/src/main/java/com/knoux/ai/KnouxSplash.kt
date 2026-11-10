package com.knoux.ai

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.setValue
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

internal class SplashMotionController(val reduced: Boolean, val freezeAtMs: Long?) {
    val clock = Animatable(0f)
    val totalMs: Float get() = if (reduced) 750f else 3480f
    val fadeStart: Float get() = if (reduced) 450f else 3080f

    /** Linear 0..1 progress of a scene window, read in the draw/layer phase. */
    fun scene(start: Float, end: Float): Float = ((clock.value - start) / (end - start)).coerceIn(0f, 1f)
    fun eased(start: Float, end: Float, easing: Easing = FastOutSlowInEasing): Float =
        if (reduced) 1f else easing.transform(scene(start, end))
}

// ---- Scene timing (ms) ----
private const val K1_START = 300f;  private const val K1_END = 680f
private const val K2_START = 620f;  private const val K2_END = 1060f
private const val K3_START = 980f;  private const val K3_END = 1260f
private const val SWEEP_START = 1150f; private const val SWEEP_END = 1520f
private const val WORD_START = 1260f; private const val WORD_STAGGER = 80f; private const val WORD_DUR = 250f
private const val SMILE_START = 2080f; private const val SMILE_END = 2640f
private const val BLOOM_START = 2580f; private const val BLOOM_END = 2800f

private val EaseOut = FastOutSlowInEasing

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

private val upperBrush = Brush.linearGradient(listOf(Color(0xfff0a0ff), Color(0xff8b2fe8)), Offset(505f, 241f), Offset(420f, 453f))
private val bladeBrush = Brush.linearGradient(listOf(Color(0xffc06bff), Color(0xff6a3df0), Color(0xff3d8bff)), Offset(680f, 320f), Offset(330f, 545f))
private val lowerBrush = Brush.linearGradient(listOf(Color(0xff3d6bff), Color(0xff35e5ff)), Offset(500f, 425f), Offset(645f, 548f))

// ---- Neon smile geometry (traced luminous curve beneath the wordmark) ----
private val smilePoints = listOf(
    Offset(60f, 776f), Offset(95f, 780f),
    Offset(135.1f, 783.1f), Offset(170.7f, 790.2f), Offset(206.2f, 799.1f), Offset(241.8f, 809.8f),
    Offset(277.3f, 819.6f), Offset(312.9f, 828.4f), Offset(348.4f, 837.3f), Offset(384f, 842.7f),
    Offset(419.6f, 848f), Offset(455.1f, 850.7f), Offset(490.7f, 851.6f), Offset(526.2f, 849.8f),
    Offset(561.8f, 845.3f), Offset(597.3f, 844.4f), Offset(632.9f, 837.3f), Offset(668.4f, 828.4f),
    Offset(704f, 817.8f), Offset(739.6f, 806.2f), Offset(775.1f, 795.6f), Offset(810.7f, 786.7f),
    Offset(842.7f, 778.7f), Offset(880f, 772f), Offset(940f, 760f)
)
private val smilePath: Path by lazy {
    Path().apply {
        moveTo(smilePoints.first().x, smilePoints.first().y)
        for (i in 1 until smilePoints.size - 1) {
            val mid = (smilePoints[i] + smilePoints[i + 1]) * 0.5f
            quadraticTo(smilePoints[i].x, smilePoints[i].y, mid.x, mid.y)
        }
        lineTo(smilePoints.last().x, smilePoints.last().y)
    }
}
private val smileMeasure: PathMeasure by lazy { PathMeasure().apply { setPath(smilePath, false) } }
private val smileSegment = Path()
private val smileBrush = Brush.horizontalGradient(
    0f to Color(0xff8b5cf6).copy(alpha = 0f), 0.08f to Color(0xff8b5cf6), 0.35f to Color(0xffa855f7),
    0.62f to Color(0xff3b82f6), 0.92f to Color(0xff22d3ee), 1f to Color(0xff22d3ee).copy(alpha = 0f),
    startX = 60f, endX = 940f
)
private val smileCore = Brush.horizontalGradient(
    0f to Color(0xffe9d5ff).copy(alpha = 0f), 0.10f to Color(0xffe9d5ff), 0.5f to Color(0xffbfdbfe),
    0.90f to Color(0xffcffafe), 1f to Color(0xffcffafe).copy(alpha = 0f), startX = 60f, endX = 940f
)
private val smileStops = listOf(
    0f to Color(0xff8b5cf6), 0.35f to Color(0xffa855f7), 0.62f to Color(0xff3b82f6), 1f to Color(0xff22d3ee)
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
private fun DrawScope.clipSweep(start: Offset, dir: Offset, length: Float, progress: Float, block: DrawScope.() -> Unit) {
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
}

private fun DrawScope.strokeGlow(path: Path, brush: Brush, tint: Color) {
    drawPath(path, tint.copy(alpha = 0.16f), style = Stroke(5.5f, join = StrokeJoin.Round))
    drawPath(path, brush)
    drawPath(path, Chalk.copy(alpha = 0.10f), style = Stroke(0.8f, join = StrokeJoin.Round))
}

/** Scene B — the K constructs itself stroke by stroke, then a restrained light sweep. */
private fun DrawScope.drawKnouxMark(c: SplashMotionController) {
    val p1 = c.eased(K1_START, K1_END)
    val p2 = c.eased(K2_START, K2_END)
    val p3 = c.eased(K3_START, K3_END)
    if (p1 > 0f) clipSweep(Offset(500f, 235f), Offset(-0.35f, 0.94f), 250f, p1) { strokeGlow(kUpper, upperBrush, Pink) }
    // Lower cyan is composited beneath the blade; it reveals last to complete the silhouette.
    if (p3 > 0f) clipSweep(Offset(560f, 412f), Offset(0.30f, 0.95f), 175f, p3) { strokeGlow(kLower, lowerBrush, Cyan) }
    if (p2 > 0f) clipSweep(Offset(308f, 556f), Offset(0.845f, -0.536f), 480f, p2) { strokeGlow(kBlade, bladeBrush, Violet) }
    val sw = c.scene(SWEEP_START, SWEEP_END)
    if (!c.reduced && sw > 0.02f && sw < 0.98f && p2 >= 1f) {
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
private fun DrawScope.drawNeonSmile(c: SplashMotionController) {
    val p = c.eased(SMILE_START, SMILE_END)
    if (p <= 0f) return
    smileSegment.reset()
    smileMeasure.getSegment(0f, smileMeasure.length * p, smileSegment, true)
    val bloom = if (c.reduced) 1f else c.scene(BLOOM_START, BLOOM_END)
    // Smooth bloom: overlapping soft radial stamps follow the curve (no hard stroke bands).
    val haze = p * (0.45f + 0.55f * bloom)
    val last = smilePoints.size - 1f
    for (i in smilePoints.indices) {
        val t = i / last
        if (t > p) break
        val edge = ((p - t) * 10f).coerceIn(0f, 1f)
        val col = smileColor(t)
        val a = haze * edge
        val pt = smilePoints[i]
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.16f * a), Color.Transparent), pt, 46f), 46f, pt)
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.20f * a), Color.Transparent), pt, 24f), 24f, pt)
    }
    // Luminous body and hot core.
    drawPath(smileSegment, smileBrush, alpha = 0.55f + 0.25f * bloom, style = Stroke(12f, cap = StrokeCap.Round))
    drawPath(smileSegment, smileBrush, alpha = 0.95f, style = Stroke(5f, cap = StrokeCap.Round))
    drawPath(smileSegment, smileCore, alpha = 0.85f, style = Stroke(2.2f, cap = StrokeCap.Round))
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
        val p = c.eased(start, start + WORD_DUR, EaseOut)
        if (p <= 0f) continue
        val rise = (1f - p) * 9f * s
        val x = left + box.left
        val y = top + (baseline - single.firstBaseline) + rise
        if (i >= 6) {
            val brush = Brush.horizontalGradient(listOf(Color(0xffe08bff), Color(0xffa458ff)), startX = x, endX = x + single.size.width)
            drawText(single, brush = brush, topLeft = Offset(x, y), alpha = p)
        } else {
            drawText(single, brush = SolidColor(Chalk), topLeft = Offset(x, y), alpha = p)
        }
    }
}

/**
 * The brand signature overlay. Composed above the app scaffold; fades into Home.
 * [freezeAtMs] (debug only) holds a single frame for deterministic scene capture.
 */
@Composable
internal fun KnouxSplashRoute(reduced: Boolean, freezeAtMs: Long?, onFinished: () -> Unit) {
    val controller = remember(reduced, freezeAtMs) { SplashMotionController(reduced, freezeAtMs) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 16)
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (!finished) { finished = true; SplashGate.consumed = true; onFinished() }
    }
    LaunchedEffect(controller) {
        if (freezeAtMs != null) {
            controller.clock.snapTo(freezeAtMs.toFloat().coerceIn(0f, controller.totalMs))
        } else {
            controller.clock.animateTo(controller.totalMs, tween(controller.totalMs.toInt(), easing = LinearEasing))
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
        val wPx = with(density) { maxWidth.toPx() }
        val s = wPx * 0.92f / 1000f
        val wordmark = remember(measurer, s, density) {
            val text = "knoux ai"
            fun layoutAt(fontPx: Float): TextLayoutResult {
                val size = with(density) { fontPx.toSp() }
                return measurer.measure(AnnotatedString(text),
                    TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.ExtraBold, fontSize = size))
            }
            var full = layoutAt(150f * s)
            val target = 624f * s
            if (full.size.width > 0) full = layoutAt(150f * s * target / full.size.width)
            val letters = text.mapIndexedNotNull { i, ch ->
                if (ch == ' ') null else Triple(i, full.getBoundingBox(i),
                    measurer.measure(AnnotatedString(ch.toString()), full.layoutInput.style))
            }
            WordmarkLayout(full, letters)
        }
        Canvas(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xff0b092e), Ink, Color(0xff050516))))) {
            val bg = if (controller.reduced) 1f else controller.scene(0f, 300f)
            val center = Offset(size.width / 2f, size.height * 0.46f)
            drawCircle(Brush.radialGradient(listOf(Color(0xff241457).copy(alpha = 0.85f * bg),
                Color(0xff120a30).copy(alpha = 0.5f * bg), Color.Transparent), center, size.minDimension * 0.78f),
                size.minDimension * 0.78f, center)
            val ox = (size.width - 1000f * s) / 2f
            val oy = (size.height - 1000f * s) / 2f
            translate(ox, oy) {
                scale(s, s, Offset.Zero) {
                    drawKnouxMark(controller)
                    drawNeonSmile(controller)
                }
                drawWordmark(controller, wordmark, s)
            }
        }
    }
}