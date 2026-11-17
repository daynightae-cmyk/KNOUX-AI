package com.knoux.ai.visual

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.dp
import com.knoux.ai.Copy
import com.knoux.ai.LocalArabic
import com.knoux.ai.NativePalette
import com.knoux.ai.R
import com.knoux.ai.data.PrivacyCheck
import com.knoux.ai.data.PrivacyObservation
import com.knoux.ai.data.PrivacyStatus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vault Radar — the signature instrument for PC04 Privacy Sweep.
 *
 * The mechanism made visible: a privacy sweep is a radar around a vault. Twelve
 * nodes ring the dial, one per real [PrivacyCheck]; each is lit by the status the
 * platform actually returned — green pass, amber attention, hollow fog for the
 * checks Android will not let KNOUX measure. Before a sweep every node is a dim
 * hollow, never a green "all clear".
 *
 * The vault's seal sits at the centre and reflects KNOUX's own FLAG_SECURE window
 * flag: a solid gold double ring when the seal is verified on, an open dashed ring
 * when off. The sweep wedge turns only while a measurement is running.
 */
@Composable
internal fun VaultRadarInstrument(
    rows: List<PrivacyObservation>,
    measuring: Boolean,
    measured: Boolean,
    /** The live window flag read back after applying — the only truth the seal may draw. */
    actualSecureWindow: Boolean,
    modifier: Modifier = Modifier,
    motion: Boolean = instrumentMotion(),
) {
    val arabic = LocalArabic.current
    val sweep = rememberMotionPhase(measuring && motion, 3000, "vault sweep")
    val checks = PrivacyCheck.entries
    val byCheck = rows.associateBy { it.check }
    val attention = rows.count { it.status == PrivacyStatus.ATTENTION }

    val description = buildString {
        append(if (arabic) "رادار الخزنة" else "Vault radar")
        when {
            measuring -> append(if (arabic) "، يفحص" else ", sweeping")
            measured -> {
                append(if (arabic) "، " else ", ")
                append(KnouxFormatNumber(rows.size))
                append(if (arabic) " فحصًا، " else " checks, ")
                append(KnouxFormatNumber(attention))
                append(if (arabic) " تنبيه" else " attention")
            }
            else -> append(if (arabic) "، لم يُقس" else ", not measured")
        }
        append(if (actualSecureWindow) (if (arabic) "، الختم مغلق" else ", seal closed") else (if (arabic) "، الختم مفتوح" else ", seal open"))
    }

    val wedgePath = remember { Path() }
    Column(
        modifier.fillMaxWidth().testTag("instrument-vault-radar").semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.fillMaxWidth().height(188.dp)) {
            drawVault(checks, byCheck, measured, sweep.value, measuring, actualSecureWindow, motion, wedgePath)
        }
        val status = when {
            measuring -> Copy(R.string.k_measuring_device)
            measured -> Copy.args(R.string.vault_nodes, rows.size, attention)
            else -> Copy(R.string.k_not_measured_yet)
        }
        val seal = if (actualSecureWindow) Copy(R.string.vault_seal_verified) else Copy(R.string.vault_seal_open)
        InstrumentCaption(Copy(R.string.inst_vault_radar), status, NativePalette.cyan, Modifier.padding(top = 8.dp))
        InstrumentCaption(seal, null, if (actualSecureWindow) NativePalette.amber else NativePalette.muted, Modifier.padding(top = 3.dp))
    }
}

private fun KnouxFormatNumber(value: Int): String = com.knoux.ai.KnouxFormat.number(value)

private fun DrawScope.drawVault(
    checks: List<PrivacyCheck>,
    byCheck: Map<PrivacyCheck, PrivacyObservation>,
    measured: Boolean,
    sweep: Float,
    measuring: Boolean,
    actualSecureWindow: Boolean,
    motion: Boolean,
    wedge: Path,
) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val cx = w / 2f
    val cy = h / 2f
    val outer = minOf(w, h) / 2f - 10.dp.toPx()
    val nodeRing = outer * .86f

    // Concentric quiet rings + crosshair: the radar chrome.
    listOf(.40f, .62f, .86f, 1f).forEach { f ->
        drawCircle(
            NativePalette.border.copy(alpha = .55f),
            radius = outer * f,
            center = Offset(cx, cy),
            style = Stroke(1.dp.toPx()),
        )
    }
    drawLine(NativePalette.border.copy(alpha = .4f), Offset(cx - outer, cy), Offset(cx + outer, cy), strokeWidth = 1.dp.toPx())
    drawLine(NativePalette.border.copy(alpha = .4f), Offset(cx, cy - outer), Offset(cx, cy + outer), strokeWidth = 1.dp.toPx())

    // Sweep wedge: only while measuring. A radial cyan wedge at the live angle.
    if (measuring) {
        val angle = (if (motion) sweep else 0.5f) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val spread = 0.7f
        wedge.rewind()
        wedge.moveTo(cx, cy)
        val steps = 14
        for (s in 0..steps) {
            val a = angle - spread * s / steps
            wedge.lineTo(cx + cos(a) * outer, cy + sin(a) * outer)
        }
        wedge.close()
        drawPath(
            wedge,
            Brush.radialGradient(
                listOf(NativePalette.cyan.copy(alpha = .26f), Color.Transparent),
                center = Offset(cx, cy), radius = outer,
            ),
        )
        drawLine(
            NativePalette.cyan.copy(alpha = .8f),
            Offset(cx, cy),
            Offset(cx + cos(angle) * outer, cy + sin(angle) * outer),
            strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round,
        )
    }

    // Twelve nodes, one per real check, lit by its returned status.
    checks.forEachIndexed { index, check ->
        val a = index.toFloat() / checks.size * 2f * PI.toFloat() - PI.toFloat() / 2f
        val px = cx + cos(a) * nodeRing
        val py = cy + sin(a) * nodeRing
        val row = byCheck[check]
        val pos = Offset(px, py)
        when {
            !measured || row == null -> {
                drawCircle(NativePalette.muted.copy(alpha = .35f), 4.5.dp.toPx(), pos, style = Stroke(1.2.dp.toPx()))
            }
            row.status == PrivacyStatus.PASS -> {
                drawCircle(NativePalette.green.copy(alpha = .22f), 7.dp.toPx(), pos)
                drawCircle(NativePalette.green, 4.dp.toPx(), pos)
            }
            row.status == PrivacyStatus.ATTENTION -> {
                drawCircle(NativePalette.amber.copy(alpha = .24f), 8.dp.toPx(), pos)
                drawCircle(NativePalette.amber, 4.5.dp.toPx(), pos)
            }
            else -> {
                // UNMEASURABLE: hollow dashed fog node.
                drawCircle(
                    InstrumentFog.copy(alpha = .8f), 4.5.dp.toPx(), pos,
                    style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5.dp.toPx(), 2.5.dp.toPx()))),
                )
            }
        }
    }

    // The vault seal at the centre: KNOUX's own FLAG_SECURE state.
    val sealR = outer * .22f
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF101A3E), InstrumentWell), center = Offset(cx, cy), radius = sealR * 1.6f),
        radius = sealR * 1.5f, center = Offset(cx, cy),
    )
    if (actualSecureWindow) {
        drawCircle(NativePalette.amber.copy(alpha = .9f), sealR, Offset(cx, cy), style = Stroke(2.2.dp.toPx()))
        drawCircle(NativePalette.amber.copy(alpha = .5f), sealR * .66f, Offset(cx, cy), style = Stroke(1.4.dp.toPx()))
        drawCircle(NativePalette.amber, sealR * .2f, Offset(cx, cy))
    } else {
        drawCircle(
            NativePalette.muted.copy(alpha = .8f), sealR, Offset(cx, cy),
            style = Stroke(1.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        )
        drawCircle(NativePalette.muted.copy(alpha = .6f), sealR * .16f, Offset(cx, cy))
    }
}
