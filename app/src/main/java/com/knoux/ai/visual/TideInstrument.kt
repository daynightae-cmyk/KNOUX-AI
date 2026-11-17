package com.knoux.ai.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tide — the signature instrument for PC01 Smart Clean.
 *
 * The mechanism made visible: a cleanup is a tide that lifts what it can carry.
 * Three wave layers sit under a night sky; one buoy cluster per *measured* file
 * group floats at the waterline, and the share the customer has selected rises
 * out of the water by exactly `selected / measured`. Groups Smart Clean cannot
 * measure (duplicates, residue) are drawn as hollow fog buoys, never as zero.
 *
 * Motion is honest: the waves travel only while a scan is running. With no scan
 * and no measurement the surface keeps one slow breath; under reduced motion it
 * is a still sea. While scanning but before any byte is counted, a light bar
 * sweeps to listen — it never pretends to be a percentage.
 */
@Composable
internal fun TideInstrument(
    scanning: Boolean,
    measuredBytes: Long?,
    selectedBytes: Long?,
    /** Real item counts for the groups Smart Clean actually measured. */
    grainCounts: List<Int>,
    /**
     * Selected item counts aligned with [grainCounts]. Each buoy cluster lifts by
     * its own share, so selecting one group never raises the others.
     */
    selectedCounts: List<Int> = emptyList(),
    /** Groups the engine could not measure in this pass. */
    fogCount: Int,
    modifier: Modifier = Modifier,
    motion: Boolean = instrumentMotion(),
) {
    val arabic = LocalArabic.current
    val measured = measuredBytes != null
    val fraction = if (measured && measuredBytes!! > 0L && selectedBytes != null) {
        (selectedBytes.toFloat() / measuredBytes).coerceIn(0f, 1f)
    } else null

    val travel = rememberMotionPhase(scanning && motion, 3600, "tide travel")
    val breath = rememberMotionPhase(!scanning && motion, 6400, "tide breath")
    val sweep = rememberMotionPhase(scanning && motion && !measured, 2400, "tide listen")
    val waves = remember { List(3) { Path() } }
    val crest = remember { Path() }
    val diamond = remember { Path() }

    val description = buildString {
        append(if (arabic) "مدّ التنظيف" else "Cleanup tide")
        when {
            scanning && !measured -> append(if (arabic) "، يستمع للملفات" else ", listening for files")
            measured -> {
                append(if (arabic) "، " else ", ")
                append(KnouxFormat.bytes(measuredBytes!!))
                if (fraction != null && fraction > 0f) {
                    append(if (arabic) "، محدد " else ", selected ")
                    append(KnouxFormat.bytes(selectedBytes ?: 0L))
                }
            }
            else -> append(if (arabic) "، لم يُقس" else ", not measured")
        }
        if (fogCount > 0) append(if (arabic) "، $fogCount مجموعة غير مقاسة" else ", $fogCount unmeasured group(s)")
    }

    Column(modifier.fillMaxWidth().testTag("instrument-tide").semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(132.dp)) {
            drawTide(
                waves = waves,
                crest = crest,
                diamond = diamond,
                travel = travel.value,
                breath = breath.value,
                sweep = sweep.value,
                scanning = scanning,
                measured = measured,
                listening = scanning && !measured,
                fraction = fraction,
                grainCounts = grainCounts,
                selectedCounts = selectedCounts,
                fogCount = fogCount,
                motion = motion,
            )
        }
        val status = when {
            scanning && !measured -> Copy(R.string.tide_listening)
            measured && fraction != null && fraction > 0f ->
                Copy.args(R.string.tide_selected, KnouxFormat.bytes(selectedBytes ?: 0L))
            measured -> Copy(R.string.k_live_device_data)
            else -> Copy(R.string.k_not_measured_yet)
        }
        InstrumentCaption(Copy(R.string.inst_tide), status, NativePalette.cyan, Modifier.padding(top = 8.dp))
    }
}

private fun DrawScope.drawTide(
    waves: List<Path>,
    crest: Path,
    diamond: Path,
    travel: Float,
    breath: Float,
    sweep: Float,
    scanning: Boolean,
    measured: Boolean,
    listening: Boolean,
    fraction: Float?,
    grainCounts: List<Int>,
    selectedCounts: List<Int>,
    fogCount: Int,
    motion: Boolean,
) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val waterline = h * .46f
    // Before anything is measured the sea must not look like measured water:
    // it is drawn in fog tones, and only a reading brings the real blues back.
    val known = measured || scanning

    // Night sky over the sea: one quiet vertical gradient, no stars (the shared
    // atmosphere already carries those).
    drawRect(
        Brush.verticalGradient(listOf(InstrumentWell, NativePalette.card), startY = 0f, endY = waterline),
        topLeft = Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(w, waterline),
    )
    // Sea body.
    drawRect(
        Brush.verticalGradient(
            if (known) listOf(Color(0xFF0C1A3A), Color(0xFF081228))
            else listOf(InstrumentFog.copy(alpha = .10f), InstrumentWell),
            startY = waterline, endY = h,
        ),
        topLeft = Offset(0f, waterline),
        size = androidx.compose.ui.geometry.Size(w, h - waterline),
    )

    // Three wave layers. Amplitude breathes slowly when idle; the phase travels
    // only while scanning. Each layer is one remembered path, rewound per frame.
    val breathAmp = if (motion && !scanning) 1f + 0.18f * sin(breath * 2f * PI.toFloat()) else 1f
    val layers = if (known) {
        listOf(
            Triple(NativePalette.violet.copy(alpha = .14f), waterline + h * .04f, h * .030f),
            Triple(NativePalette.blue.copy(alpha = .22f), waterline + h * .12f, h * .038f),
            Triple(NativePalette.cyan.copy(alpha = .30f), waterline + h * .22f, h * .046f),
        )
    } else {
        listOf(
            Triple(InstrumentFog.copy(alpha = .10f), waterline + h * .04f, h * .030f),
            Triple(InstrumentFog.copy(alpha = .14f), waterline + h * .12f, h * .038f),
            Triple(InstrumentFog.copy(alpha = .18f), waterline + h * .22f, h * .046f),
        )
    }
    layers.forEachIndexed { index, (tint, baseY, amp) ->
        val path = waves[index]
        path.rewind()
        val phase = (if (scanning) travel else 0.31f) * 2f * PI.toFloat() + index * 1.7f
        val a = amp * breathAmp
        val steps = 28
        path.moveTo(0f, baseY + sin(phase) * a)
        for (s in 1..steps) {
            val x = w * s / steps
            val y = baseY + sin(phase + x / w * 4f * PI.toFloat()) * a
            path.lineTo(x, y)
        }
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        drawPath(path, tint)
        // A lit crest line on the front wave only.
        if (index == 2) {
            crest.rewind()
            crest.moveTo(0f, baseY + sin(phase) * a)
            for (s in 1..steps) {
                val x = w * s / steps
                crest.lineTo(x, baseY + sin(phase + x / w * 4f * PI.toFloat()) * a)
            }
            drawPath(
                crest,
                (if (known) NativePalette.cyan else InstrumentFog).copy(alpha = if (known) .5f else .28f),
                style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }

    // Listening sweep: a vertical light bar crosses the sea while scanning with
    // no bytes yet. It is chrome for "working", never a progress fraction.
    if (listening) {
        val x = if (motion) sweep * w else w * .5f
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.Transparent, NativePalette.cyan.copy(alpha = .28f), Color.Transparent),
                startX = x - w * .08f, endX = x + w * .08f,
            ),
            topLeft = Offset(x - w * .08f, waterline),
            size = androidx.compose.ui.geometry.Size(w * .16f, h - waterline),
        )
    }

    // Measured groups: one buoy cluster each, grains bobbing at the waterline.
    // A cluster lifts by the share of ITS OWN items that are selected, so a
    // selection in one group never raises another group's buoys.
    val groups = grainCounts.filter { it > 0 }
    var visibleIndex = 0
    grainCounts.forEachIndexed { sourceIndex, rawCount ->
        val count = rawCount
        if (count <= 0) return@forEachIndexed
        val gi = visibleIndex++
        val selected = (selectedCounts.getOrNull(sourceIndex) ?: 0).coerceIn(0, count)
        val share = selected.toFloat() / count.toFloat()
        val lift = share * h * .16f
        val cx = w * (gi + 1f) / (groups.size + 1f)
        val grains = count.coerceAtMost(5)
        for (g in 0 until grains) {
            val gx = cx + (g - (grains - 1) / 2f) * 7.dp.toPx()
            val bob = if (motion && scanning) sin(travel * 2f * PI.toFloat() + g) * 2.dp.toPx() else 0f
            val gy = waterline - lift + bob
            val tone = if (gi == 0) NativePalette.cyan else NativePalette.violet
            val r = 3.2.dp.toPx()
            // A small diamond grain with a halo when this group is selected.
            if (lift > 1f) drawCircle(tone.copy(alpha = .25f), r * 2.1f, Offset(gx, gy))
            diamond.rewind()
            diamond.moveTo(gx, gy - r)
            diamond.lineTo(gx + r, gy)
            diamond.lineTo(gx, gy + r)
            diamond.lineTo(gx - r, gy)
            diamond.close()
            drawPath(diamond, tone.copy(alpha = .92f))
        }
    }

    // Unmeasured groups: hollow dashed buoys. No fill, no number — fog.
    for (f in 0 until fogCount.coerceAtMost(4)) {
        val cx = w * (groups.size + f + 1f) / (groups.size + fogCount.coerceAtMost(4) + 1f)
        drawCircle(
            InstrumentFog.copy(alpha = .7f),
            radius = 4.dp.toPx(),
            center = Offset(cx, waterline),
            style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
        )
    }

    // Waterline hairline so the sea reads as a surface, not a cut.
    drawLine(
        (if (known) NativePalette.cyan else InstrumentFog).copy(alpha = .25f),
        Offset(0f, waterline), Offset(w, waterline),
        strokeWidth = 1.dp.toPx(),
    )
}
