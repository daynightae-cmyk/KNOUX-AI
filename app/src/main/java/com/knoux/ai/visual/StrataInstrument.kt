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
import com.knoux.ai.data.ResidueConfidence
import com.knoux.ai.data.ResidueItem
import kotlin.math.PI
import kotlin.math.sin

/**
 * Strata — the signature instrument for PC03 App Residue.
 *
 * The mechanism made visible: residue is buried, so the instrument is a soil
 * cross-section. Four strata darken with depth; one root descends per measured
 * leftover, and its depth is the file's real age — an older file roots deeper.
 * Owned cache roots end in a green bulb, shared/provider roots in amber, so the
 * confidence the engine assigned is legible without a word.
 *
 * A digging beam marks the sources actually read (`scopesDone / scopesTotal`);
 * before any source completes it pulses at the surface to listen. Sources the
 * platform refused leave a fog band at the bottom — never an empty "clean" soil.
 */
@Composable
internal fun StrataInstrument(
    scanning: Boolean,
    measured: Boolean,
    items: List<ResidueItem>,
    measuredBytes: Long?,
    scopesDone: Int,
    scopesTotal: Int,
    fogged: Boolean,
    modifier: Modifier = Modifier,
    motion: Boolean = instrumentMotion(),
) {
    val arabic = LocalArabic.current
    val dig = rememberMotionPhase(scanning && motion, 2600, "strata dig")
    val roots = remember(items) {
        val now = System.currentTimeMillis()
        val sample = items.take(7)
        val ages = sample.map { item ->
            val ts = item.candidate.lastModifiedMillis
            if (ts in 1..now) (now - ts).toFloat() else 0f
        }
        val maxAge = (ages.maxOrNull() ?: 0f).coerceAtLeast(1f)
        sample.mapIndexed { index, item ->
            val depth = if (ages[index] <= 0f) 0.5f else 0.22f + 0.72f * (ages[index] / maxAge)
            RootSeed(
                x = (index + 1f) / (sample.size + 1f),
                depth = depth.coerceIn(0.2f, 0.94f),
                owned = item.confidence == ResidueConfidence.OWNED,
                sway = (index * 0.37f) % 1f,
            )
        }
    }

    val description = buildString {
        append(if (arabic) "طبقات البقايا" else "Residue strata")
        when {
            scanning -> append(if (arabic) "، يقرأ المصادر" else ", reading sources")
            measured -> {
                append(if (arabic) "، " else ", ")
                append(KnouxFormat.number(items.size))
                append(if (arabic) " بقايا، " else " leftovers, ")
                append(KnouxFormat.bytes(measuredBytes ?: 0L))
            }
            else -> append(if (arabic) "، لم يُقس" else ", not measured")
        }
        if (fogged) append(if (arabic) "، مصادر غير مقاسة" else ", unmeasured sources")
    }

    val bandPath = remember { Path() }
    val rootPath = remember { Path() }
    // An unmeasured well must not pretend to be measured soil: it is shorter, so
    // it reads as an empty stratum waiting to be read.
    val wellHeight = if (!measured && !scanning) 96.dp else 140.dp
    Column(modifier.fillMaxWidth().testTag("instrument-strata").semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(wellHeight)) {
            drawStrata(roots, dig.value, scanning, measured, scopesDone, scopesTotal, fogged, motion, bandPath, rootPath)
        }
        val status = when {
            scanning -> Copy(R.string.strata_digging)
            measured -> Copy.args(R.string.strata_roots, items.size, KnouxFormat.bytes(measuredBytes ?: 0L))
            else -> Copy(R.string.k_not_measured_yet)
        }
        InstrumentCaption(Copy(R.string.inst_strata), status, NativePalette.amber, Modifier.padding(top = 8.dp))
    }
}

private data class RootSeed(val x: Float, val depth: Float, val owned: Boolean, val sway: Float)

private fun DrawScope.drawStrata(
    roots: List<RootSeed>,
    dig: Float,
    scanning: Boolean,
    measured: Boolean,
    scopesDone: Int,
    scopesTotal: Int,
    fogged: Boolean,
    motion: Boolean,
    bandPath: Path,
    rootPath: Path,
) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return

    // Not measured: an honest unread stratum. No soil colour and no roots — the
    // well, a dashed horizon and a low fog — so "unknown" never reads as "clean".
    if (!measured && !scanning) {
        drawRect(InstrumentWell, topLeft = Offset.Zero, size = size)
        val fogTop = h * .42f
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, InstrumentFog.copy(alpha = .14f)),
                startY = fogTop, endY = h,
            ),
            topLeft = Offset(0f, fogTop),
            size = androidx.compose.ui.geometry.Size(w, h - fogTop),
        )
        drawLine(
            InstrumentFog.copy(alpha = .55f),
            Offset(0f, fogTop), Offset(w, fogTop),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        )
        for (i in 1..2) {
            val y = fogTop + h * .16f * i
            drawLine(
                InstrumentFog.copy(alpha = (.18f - i * .05f).coerceAtLeast(.04f)),
                Offset(w * .08f, y), Offset(w * .92f, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        return
    }

    // Four strata, each a band with a gently wavy lower boundary. Tones descend
    // from a lit topsoil to a deep rust — all drawn from the amber family so the
    // instrument keeps its own identity against the blue shell.
    val bands = listOf(
        Color(0xFF2A2138) to 0.26f,
        Color(0xFF241B30) to 0.50f,
        Color(0xFF1C1626) to 0.74f,
        Color(0xFF14101E) to 1.00f,
    )
    var top = 0f
    bands.forEachIndexed { index, (tint, bottomFrac) ->
        val bottom = h * bottomFrac
        bandPath.rewind()
        bandPath.moveTo(0f, top)
        bandPath.lineTo(w, top)
        bandPath.lineTo(w, bottom)
        // Wavy boundary back to the start.
        val steps = 16
        for (s in steps downTo 0) {
            val x = w * s / steps
            val y = bottom + sin(x / w * 3f * PI.toFloat() + index) * h * .012f
            bandPath.lineTo(x, y)
        }
        bandPath.close()
        drawPath(bandPath, tint)
        top = bottom
    }
    // Topsoil light: one quiet amber horizon.
    drawRect(
        Brush.verticalGradient(listOf(NativePalette.amber.copy(alpha = .10f), Color.Transparent), startY = 0f, endY = h * .12f),
        topLeft = Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(w, h * .12f),
    )

    // Roots: one bezier per measured leftover, depth = real age. The bulb at the
    // tip carries the confidence tone.
    roots.forEach { root ->
        val x = root.x * w
        val tipY = root.depth * h
        val sway = (root.sway - .5f) * w * .06f
        rootPath.rewind()
        rootPath.moveTo(x, h * .04f)
        rootPath.cubicTo(x + sway, tipY * .4f, x - sway, tipY * .7f, x + sway * .3f, tipY)
        val tone = if (root.owned) NativePalette.green else NativePalette.amber
        drawPath(rootPath, tone.copy(alpha = .55f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(tone.copy(alpha = .22f), 6.dp.toPx(), Offset(x + sway * .3f, tipY))
        drawCircle(tone.copy(alpha = .95f), 3.dp.toPx(), Offset(x + sway * .3f, tipY))
    }

    // Digging beam: a vertical light at the real source fraction while scanning.
    if (scanning) {
        val frac = if (scopesTotal > 0) scopesDone.toFloat() / scopesTotal else null
        val x = when {
            frac != null -> frac * w
            motion -> dig * w
            else -> w * .5f
        }
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.Transparent, NativePalette.amber.copy(alpha = .30f), Color.Transparent),
                startX = x - w * .05f, endX = x + w * .05f,
            ),
            topLeft = Offset(x - w * .05f, 0f),
            size = androidx.compose.ui.geometry.Size(w * .10f, h),
        )
        drawLine(
            NativePalette.amber.copy(alpha = .8f),
            Offset(x, 0f), Offset(x, h),
            strokeWidth = 1.4.dp.toPx(),
        )
    }

    // Fog band: sources the platform refused. Hollow dashed boundary + fog wash,
    // never a measured-looking soil.
    if (fogged) {
        val fogTop = h * .80f
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, InstrumentFog.copy(alpha = .16f)), startY = fogTop, endY = h),
            topLeft = Offset(0f, fogTop),
            size = androidx.compose.ui.geometry.Size(w, h - fogTop),
        )
        drawLine(
            InstrumentFog.copy(alpha = .6f),
            Offset(0f, fogTop), Offset(w, fogTop),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
        )
    }
}
