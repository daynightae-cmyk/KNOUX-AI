package com.knoux.ai.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.knoux.ai.Copy
import com.knoux.ai.KnouxFormat
import com.knoux.ai.LocalArabic
import com.knoux.ai.NativePalette
import com.knoux.ai.R
import com.knoux.ai.data.DeviceInsights
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Terrain — the signature instrument for the Storage analyzer.
 *
 * The mechanism made visible: storage is a landscape, so the instrument is a
 * topographic map. Each contour's radius is the square root of a real byte share
 * of the partition, so area reads as quantity. The outer contour is total
 * capacity, the next is used space, and inner contours are the categories Android
 * actually measured (images, video, cache). The portion of used space no category
 * accounts for is drawn as a fog band between two dashed contours — known to
 * exist, not measured in detail, never coloured as if it were.
 *
 * The map prints no numeral: the hero beside it carries the figures, so a value
 * appears exactly once. A slow contour shimmer is the only motion and stills
 * under reduced motion.
 */
@Composable
internal fun TerrainInstrument(
    snapshot: DeviceInsights.StorageSnapshot,
    modifier: Modifier = Modifier,
    motion: Boolean = instrumentMotion(),
) {
    val arabic = LocalArabic.current
    val shimmer = rememberMotionPhase(motion, 7000, "terrain shimmer")
    val measured = snapshot.measured
    val total = snapshot.totalBytes.coerceAtLeast(1L)
    val used = snapshot.usedBytes.coerceIn(0L, total)
    val measuredCats = remember(snapshot) {
        snapshot.categories.filter { it.measured && it.bytes > 0 }.sortedByDescending { it.bytes }
    }
    val accounted = (measuredCats.sumOf { it.bytes }).coerceAtMost(used)
    val unmeasured = (used - accounted).coerceAtLeast(0L)

    val description = buildString {
        append(if (arabic) "تضاريس التخزين" else "Storage terrain")
        append(if (arabic) "، " else ", ")
        if (!measured) {
            append(if (arabic) "المساحة غير مقاسة بعد" else "storage not measured yet")
        } else {
            append(KnouxFormat.bytes(accounted))
            append(if (arabic) " مقاس، " else " measured, ")
            append(KnouxFormat.bytes(unmeasured))
            append(if (arabic) " غير مقاس" else " unmeasured")
        }
    }

    Column(modifier.fillMaxWidth().testTag("instrument-terrain").semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(156.dp)) {
            if (!measured) {
                // Storage not measured: an honest fog field. No coastline, no
                // contours, no numerals — unknown is drawn as unknown.
                drawRect(
                    InstrumentWell,
                    topLeft = Offset.Zero,
                    size = size,
                )
                val fogTop = size.height * .40f
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, InstrumentFog.copy(alpha = .16f)), startY = fogTop, endY = size.height),
                    topLeft = Offset(0f, fogTop),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height - fogTop),
                )
                drawLine(
                    InstrumentFog.copy(alpha = .6f),
                    Offset(0f, fogTop), Offset(size.width, fogTop),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                )
            } else {
                drawTerrain(total, used, accounted, measuredCats, shimmer.value, motion)
            }
        }
        InstrumentCaption(
            Copy(R.string.inst_terrain),
            if (!measured) Copy(R.string.k_not_measured_yet)
            else Copy.args(R.string.terrain_contours, KnouxFormat.bytes(accounted), KnouxFormat.bytes(unmeasured)),
            NativePalette.blue,
            Modifier.padding(top = 8.dp),
        )
    }
}

private fun categoryTone(key: String): Color = when (key) {
    "images" -> NativePalette.violet
    "video" -> NativePalette.cyan
    "cache" -> NativePalette.green
    else -> NativePalette.blue
}

private fun wavyRing(path: Path, cx: Float, cy: Float, r: Float, seed: Int, amp: Float, phase: Float) {
    path.rewind()
    val steps = 60
    for (s in 0..steps) {
        val a = s.toFloat() / steps * 2f * PI.toFloat()
        val wobble = 1f + amp * (sin(a * 3f + seed + phase) * .6f + sin(a * 5f + seed * 1.7f) * .4f)
        val x = cx + cos(a) * r * wobble
        val y = cy + sin(a) * r * wobble * .86f
        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
}

private fun DrawScope.drawTerrain(
    total: Long,
    used: Long,
    accounted: Long,
    cats: List<DeviceInsights.CategoryUsage>,
    shimmer: Float,
    motion: Boolean,
) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val cx = w / 2f
    val cy = h / 2f
    val maxR = minOf(w, h) / 2f - 8.dp.toPx()
    val phase = if (motion) shimmer * 2f * PI.toFloat() * .15f else 0f
    fun radius(bytes: Long): Float = maxR * sqrt(bytes.toFloat() / total).coerceIn(0f, 1f)

    val ring = Path()

    // Total capacity boundary: the quiet outer coast.
    wavyRing(ring, cx, cy, maxR, 1, .03f, phase)
    drawPath(ring, NativePalette.border.copy(alpha = .5f), style = Stroke(1.dp.toPx()))

    // Used space: a faint landmass fill inside its contour.
    val rUsed = radius(used)
    wavyRing(ring, cx, cy, rUsed, 2, .04f, phase)
    drawPath(ring, Brush.radialGradient(listOf(Color(0xFF13203F), Color(0xFF0C1430)), center = Offset(cx, cy), radius = rUsed.coerceAtLeast(1f)))
    drawPath(ring, NativePalette.blue.copy(alpha = .55f), style = Stroke(1.4.dp.toPx()))

    // Fog band: used space no category accounts for. Wash between the accounted
    // contour and the used contour, bounded by a dashed line.
    val rAccounted = radius(accounted)
    if (unmeasuredGreaterThan(accounted, used)) {
        wavyRing(ring, cx, cy, rUsed, 2, .04f, phase)
        drawPath(ring, InstrumentFog.copy(alpha = .10f))
        wavyRing(ring, cx, cy, rAccounted.coerceAtLeast(2f), 3, .05f, phase)
        drawPath(
            ring, InstrumentFog.copy(alpha = .7f),
            style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
        )
    }

    // Measured category contours, nested inward, each in its own tone.
    cats.forEachIndexed { index, cat ->
        val r = radius(cat.bytes).coerceAtLeast(3f)
        val tone = categoryTone(cat.key)
        wavyRing(ring, cx, cy, r, 4 + index, .05f, phase)
        drawPath(ring, tone.copy(alpha = .08f))
        drawPath(ring, tone.copy(alpha = .8f), style = Stroke(1.6.dp.toPx()))
    }

    // Summit marker at the centre.
    drawCircle(NativePalette.cyan.copy(alpha = .25f), 6.dp.toPx(), Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = .9f), 2.4.dp.toPx(), Offset(cx, cy))
}

private fun unmeasuredGreaterThan(accounted: Long, used: Long): Boolean = used - accounted > 0L
