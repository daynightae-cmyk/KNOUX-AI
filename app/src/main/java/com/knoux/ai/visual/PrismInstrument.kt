package com.knoux.ai.visual

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.knoux.ai.data.CleanupHistory
import kotlin.math.PI
import kotlin.math.sin

/**
 * Prism — the signature instrument for Reports & Insights.
 *
 * The mechanism made visible: a report splits real work into its spectrum. One
 * white beam (the customer's effort) enters a glass prism; each *completed*
 * cleanup leaves as a coloured band whose thickness is that cleanup's measured
 * bytes over the largest of the set. No cleanup, no band — the prism waits dim
 * rather than emitting a fabricated rainbow.
 *
 * The reveal is one finite grow when the chart first appears; the bands never
 * loop and never invent a value. Under reduced motion the spectrum is drawn at
 * full length immediately. Figures stay in the bar chart below; the prism itself
 * carries no numeral, so a value appears once.
 */
@Composable
internal fun PrismInstrument(
    completed: List<CleanupHistory>,
    modifier: Modifier = Modifier,
    motion: Boolean = instrumentMotion(),
) {
    val arabic = LocalArabic.current
    val bands = remember(completed) { completed.take(6) }
    val maxBytes = (bands.maxOfOrNull { it.bytes } ?: 0L).coerceAtLeast(1L)
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(bands) { if (bands.isNotEmpty()) entered = true }
    val reveal by animateFloatAsState(
        if (entered) 1f else 0f,
        tween(if (motion) 620 else 0), label = "prism reveal",
    )

    val description = buildString {
        append(if (arabic) "طيف التنظيف" else "Cleanup spectrum")
        if (bands.isEmpty()) {
            append(if (arabic) "، لا تنظيف مكتمل بعد" else ", no completed cleanup yet")
        } else {
            append(if (arabic) "، " else ", ")
            append(KnouxFormat.number(bands.size))
            append(if (arabic) " عملية تنظيف" else " cleanups")
        }
    }

    val trianglePath = remember { Path() }
    Column(modifier.fillMaxWidth().testTag("instrument-prism").semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(132.dp)) {
            drawPrism(bands, maxBytes, reveal, motion, trianglePath)
        }
        val status = if (bands.isEmpty()) {
            Copy(R.string.prism_empty)
        } else {
            Copy(R.string.prism_spectrum)
        }
        InstrumentCaption(
            Copy(R.string.inst_prism),
            if (bands.isEmpty()) Copy(R.string.prism_spectrum) else status,
            NativePalette.violet,
            Modifier.padding(top = 8.dp),
        )
    }
}

private val spectrum = listOf(
    NativePalette.violet, NativePalette.blue, NativePalette.cyan,
    NativePalette.green, NativePalette.amber, NativePalette.magenta,
)

private fun DrawScope.drawPrism(
    bands: List<CleanupHistory>,
    maxBytes: Long,
    reveal: Float,
    motion: Boolean,
    triangle: Path,
) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val prismX = w * .30f
    val cy = h * .52f
    val prismR = h * .30f

    // Incoming white beam from the leading edge to the prism face.
    val beamIn = if (motion) 1f else 1f
    drawLine(
        Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .85f)), startX = 0f, endX = prismX),
        Offset(0f, cy), Offset(prismX - prismR * .4f, cy),
        strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round,
    )

    // The glass prism: a triangle with a quiet refractive gradient.
    triangle.rewind()
    triangle.moveTo(prismX, cy - prismR)
    triangle.lineTo(prismX + prismR * .9f, cy + prismR * .7f)
    triangle.lineTo(prismX - prismR * .9f, cy + prismR * .7f)
    triangle.close()
    drawPath(
        triangle,
        Brush.linearGradient(
            listOf(NativePalette.cyan.copy(alpha = .16f), NativePalette.violet.copy(alpha = .10f), Color(0xFF101A3E)),
            start = Offset(prismX - prismR, cy - prismR), end = Offset(prismX + prismR, cy + prismR),
        ),
    )
    drawPath(triangle, NativePalette.cyan.copy(alpha = .45f), style = Stroke(1.4.dp.toPx()))
    // A facet highlight.
    drawLine(
        Color.White.copy(alpha = .25f),
        Offset(prismX, cy - prismR), Offset(prismX - prismR * .5f, cy + prismR * .5f),
        strokeWidth = 1.dp.toPx(),
    )

    if (bands.isEmpty()) {
        // No real light to split: a faint stub at the exit face, nothing more.
        drawCircle(InstrumentFog.copy(alpha = .4f), 2.5.dp.toPx(), Offset(prismX + prismR * .9f, cy))
        return
    }

    // Output spectrum: one band per completed cleanup, thickness = real bytes.
    val exitX = prismX + prismR * .9f
    val span = h * .62f
    val n = bands.size
    bands.forEachIndexed { index, band ->
        val tone = spectrum[index % spectrum.size]
        val frac = index.toFloat() / (n.coerceAtLeast(2) - 1).coerceAtLeast(1)
        val y = cy - span / 2f + span * frac
        val thickness = (2f + 7f * (band.bytes.toFloat() / maxBytes)).dp.toPx()
        val length = (w - exitX - 8.dp.toPx()) * reveal
        val drift = if (motion) sin(index * 1.3f) * h * .01f else 0f
        drawLine(
            Brush.horizontalGradient(listOf(tone.copy(alpha = .9f), tone.copy(alpha = .25f)), startX = exitX, endX = exitX + length),
            Offset(exitX, cy + drift * .3f), Offset(exitX + length, y + drift),
            strokeWidth = thickness.coerceAtLeast(1.5.dp.toPx()), cap = StrokeCap.Round,
        )
        // A bright node where the band leaves the prism.
        drawCircle(tone.copy(alpha = .9f), thickness * .4f, Offset(exitX, cy + drift * .3f))
    }
}
