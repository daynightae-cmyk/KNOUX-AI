package com.knoux.ai.visual

import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.knoux.ai.Copy
import com.knoux.ai.KnouxFormat
import com.knoux.ai.Label
import com.knoux.ai.LocalArabic
import com.knoux.ai.NativePalette
import com.knoux.ai.R
import kotlin.math.exp
import kotlin.math.PI

/**
 * Pulse — the signature instrument for the Battery diagnostics hero.
 *
 * The mechanism made visible: a battery is a live signal, so the instrument is an
 * ECG trace. The spike amplitude is the real charge level — a full battery beats
 * tall and confident, a low one beats small. While the device is charging the
 * trace travels and a bright node leads it; discharging it drifts slowly; with no
 * reading the line is flat and dashed and no beat is drawn at all.
 *
 * The level numeral is printed exactly once, here, so the fact rows below never
 * repeat it. Under reduced motion the trace is a single composed still frame.
 */
@Composable
internal fun PulseInstrument(
    levelPercent: Int?,
    status: Int?,
    modifier: Modifier = Modifier,
    motion: Boolean = instrumentMotion(),
) {
    val arabic = LocalArabic.current
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val live = levelPercent != null
    val travel = rememberMotionPhase(live && motion, if (charging) 2600 else 5200, "pulse travel")
    val trace = remember { Path() }

    val description = if (live) {
        Copy.args(R.string.pulse_trace_desc, KnouxFormat.percent(levelPercent!!), chargeWord(status, arabic)).text(arabic)
    } else {
        if (arabic) "نبض البطارية، لا قراءة" else "Battery pulse, no reading"
    }

    Column(modifier.fillMaxWidth().testTag("instrument-pulse").semantics { contentDescription = description }) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                drawPulse(trace, levelPercent, travel.value, charging, live, motion)
            }
            // The one level numeral, top-leading, isolated so bidi never flips it.
            Column(Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 2.dp)) {
                if (live) {
                    Label(KnouxFormat.isolate("$levelPercent%"), 30, NativePalette.text, FontWeight.ExtraBold)
                } else {
                    Label("—", 26, NativePalette.muted, FontWeight.Bold)
                }
                Label(
                    if (live) Copy(R.string.pulse_live) else Copy(R.string.battery_not_reported),
                    10, if (charging) NativePalette.cyan else NativePalette.muted, FontWeight.SemiBold,
                )
            }
        }
        InstrumentCaption(
            Copy(R.string.inst_pulse),
            if (live) Copy.runtime(chargeWord(status, arabic)) else null,
            if (charging) NativePalette.cyan else NativePalette.blue,
            Modifier.padding(top = 8.dp),
        )
    }
}

private fun chargeWord(status: Int?, arabic: Boolean): String = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> if (arabic) "يشحن" else "Charging"
    BatteryManager.BATTERY_STATUS_FULL -> if (arabic) "ممتلئ" else "Full"
    BatteryManager.BATTERY_STATUS_DISCHARGING,
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> if (arabic) "لا يشحن" else "Not charging"
    else -> if (arabic) "غير مُبلّغ" else "Not reported"
}

/** One PQRST-like beat as a sum of gaussians over local phase p in [0,1]. */
private fun beat(p: Float): Float {
    fun g(center: Float, width: Float, amp: Float): Float =
        amp * exp(-((p - center) * (p - center)) / (2f * width * width))
    return g(0.16f, 0.030f, 0.14f) +   // P
        g(0.30f, 0.012f, -0.12f) +     // Q
        g(0.34f, 0.014f, 1.00f) +      // R
        g(0.38f, 0.014f, -0.24f) +     // S
        g(0.58f, 0.045f, 0.30f)        // T
}

private fun DrawScope.drawPulse(
    trace: Path,
    level: Int?,
    travel: Float,
    charging: Boolean,
    live: Boolean,
    motion: Boolean,
) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val baseline = h * .64f

    // Quiet monitor grid.
    for (i in 1..3) {
        val y = h * i / 4f
        drawLine(NativePalette.border.copy(alpha = .30f), Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
    }

    if (!live) {
        // No reading: a flat dashed midline. No beat, no colour that reads as data.
        drawLine(
            InstrumentFog.copy(alpha = .7f),
            Offset(0f, baseline), Offset(w, baseline),
            strokeWidth = 1.6.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        )
        return
    }

    val amp = (level!!.coerceIn(0, 100) / 100f) * h * .46f
    val beats = 4f
    val shift = if (motion) travel else 0.31f
    trace.rewind()
    val samples = 180
    var leading = Offset(0f, baseline)
    for (s in 0..samples) {
        val x = w * s / samples
        val p = ((x / w) * beats + shift * beats) % 1f
        val y = baseline - beat(p) * amp
        if (s == 0) trace.moveTo(x, y) else trace.lineTo(x, y)
        leading = Offset(x, y)
    }
    // Halo under the trace, then the trace itself in the brand order.
    drawPath(
        trace,
        Brush.horizontalGradient(listOf(NativePalette.blue.copy(alpha = .18f), NativePalette.cyan.copy(alpha = .18f))),
        style = Stroke(6.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        trace,
        Brush.horizontalGradient(listOf(NativePalette.blue, NativePalette.cyan)),
        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
    )
    // Leading node: bright while charging, quiet otherwise.
    val nodeTone = if (charging) NativePalette.cyan else NativePalette.blue
    drawCircle(nodeTone.copy(alpha = .30f), 7.dp.toPx(), leading)
    drawCircle(Color.White.copy(alpha = .95f), 3.dp.toPx(), leading)
}
