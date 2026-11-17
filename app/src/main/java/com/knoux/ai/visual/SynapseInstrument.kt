package com.knoux.ai.visual

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One recommendation drawn as one synapse node. [lit] means real evidence exists. */
internal data class SynapseNode(
    val id: String,
    val lit: Boolean,
    val accepted: Boolean,
    val tone: Color,
)

/**
 * Synapse — the signature instrument for AI Recommendations.
 *
 * The mechanism made visible: recommendations are connections that fire from real
 * evidence. A central core (KNOUX's on-device rules) links to one node per
 * recommendation. A node lights only when a measured pass actually found something
 * actionable; accepting it sends one finite pulse down its axon and keeps it bright.
 * With no evidence the mesh stays dormant and dim — it never sparks on its own.
 *
 * This is deliberately not a "thinking AI" animation: there is no idle firing, no
 * random sparkle. Every light traces to a finding the customer can open and verify.
 */
@Composable
internal fun SynapseInstrument(
    nodes: List<SynapseNode>,
    modifier: Modifier = Modifier,
    motion: Boolean = instrumentMotion(),
) {
    val arabic = LocalArabic.current
    val lit = nodes.count { it.lit }
    val accepted = nodes.count { it.accepted }
    val positions = remember(nodes.size) {
        val n = nodes.size.coerceAtLeast(1)
        List(nodes.size) { index ->
            val a = index.toFloat() / n * 2f * PI.toFloat() - PI.toFloat() / 2f
            Offset(cos(a), sin(a))
        }
    }
    // One finite pulse per node, computed here in composition (never inside the
    // DrawScope). Accepting a node drives its pulse 0 -> 1 once; it then stays lit.
    val pulses = nodes.map { node ->
        animateFloatAsState(
            if (node.accepted) 1f else 0f,
            tween(if (motion) 700 else 0), label = "synapse pulse ${node.id}",
        ).value
    }

    val description = buildString {
        append(if (arabic) "مشبك التوصيات" else "Recommendation synapse")
        append(if (arabic) "، " else ", ")
        append(KnouxFormat.number(lit))
        append(if (arabic) " من " else " of ")
        append(KnouxFormat.number(nodes.size))
        append(if (arabic) " مضاءة" else " lit")
    }

    Column(modifier.fillMaxWidth().testTag("instrument-synapse").semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            drawSynapse(nodes, positions, pulses, motion)
        }
        InstrumentCaption(
            Copy(R.string.inst_synapse),
            if (nodes.isEmpty()) Copy(R.string.synapse_empty_title)
            else Copy.args(R.string.synapse_lit, lit, nodes.size),
            NativePalette.magenta,
            Modifier.padding(top = 8.dp),
        )
    }
}

private fun DrawScope.drawSynapse(
    nodes: List<SynapseNode>,
    positions: List<Offset>,
    pulses: List<Float>,
    motion: Boolean,
) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val cx = w / 2f
    val cy = h / 2f
    val ring = minOf(w, h) / 2f - 16.dp.toPx()

    // Core glow: bright only when at least one node is lit by real evidence.
    val anyLit = nodes.any { it.lit }
    drawCircle(
        Brush.radialGradient(
            listOf((if (anyLit) NativePalette.magenta else NativePalette.muted).copy(alpha = if (anyLit) .30f else .10f), Color.Transparent),
            center = Offset(cx, cy), radius = ring * .5f,
        ),
        radius = ring * .5f, center = Offset(cx, cy),
    )

    // Axons + nodes.
    nodes.forEachIndexed { index, node ->
        val dir = positions.getOrElse(index) { Offset(1f, 0f) }
        val nx = cx + dir.x * ring
        val ny = cy + dir.y * ring * .82f
        val nodePos = Offset(nx, ny)
        val tone = if (node.lit) node.tone else InstrumentFog

        // Axon: dashed fog when unlit, solid tone when lit.
        if (node.lit) {
            drawLine(tone.copy(alpha = .5f), Offset(cx, cy), nodePos, strokeWidth = 1.6.dp.toPx())
        } else {
            drawLine(
                InstrumentFog.copy(alpha = .4f), Offset(cx, cy), nodePos,
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
        }

        // Accepted: one finite pulse travels the axon, then the node stays bright.
        if (node.accepted) {
            val pulse = pulses.getOrElse(index) { 1f }
            val px = cx + (nx - cx) * pulse
            val py = cy + (ny - cy) * pulse
            drawCircle(tone.copy(alpha = .35f), 6.dp.toPx(), Offset(px, py))
            drawCircle(Color.White.copy(alpha = .9f), 2.4.dp.toPx(), Offset(px, py))
        }

        // Node body.
        val r = if (node.accepted) 7.dp.toPx() else if (node.lit) 5.5.dp.toPx() else 4.dp.toPx()
        if (node.lit) drawCircle(tone.copy(alpha = .22f), r * 2f, nodePos)
        if (node.lit) {
            drawCircle(tone.copy(alpha = .95f), r, nodePos)
        } else {
            drawCircle(InstrumentFog.copy(alpha = .6f), r, nodePos, style = Stroke(1.2.dp.toPx()))
        }
        if (node.accepted) drawCircle(Color.White.copy(alpha = .9f), r * .4f, nodePos)
    }

    // Core node last, on top.
    drawCircle(NativePalette.card, 9.dp.toPx(), Offset(cx, cy))
    drawCircle(
        (if (anyLit) NativePalette.magenta else NativePalette.muted).copy(alpha = .9f),
        9.dp.toPx(), Offset(cx, cy), style = Stroke(1.8.dp.toPx()),
    )
    drawCircle((if (anyLit) NativePalette.magenta else NativePalette.muted).copy(alpha = .8f), 3.dp.toPx(), Offset(cx, cy))
}
