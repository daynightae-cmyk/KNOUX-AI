package com.knoux.ai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The hub's device instrument — the observatory orb.
 *
 * One glass sphere holds the traced K, lit from above by the brand's own
 * magenta→violet→blue→cyan ramp and resting on a pool of light. A single orbit
 * rail carries a travelling satellite. Nothing here encodes a measurement: the
 * orb is atmosphere, and every real figure is printed as a number beside it.
 *
 * Motion is one slow satellite drift and one breathing halo. Under reduced
 * motion both stop at a composed frame instead of freezing mid-animation.
 */
@Composable
internal fun DeviceHero(modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    val drift = if (motion) {
        val transition = rememberInfiniteTransition(label = "hub orb")
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart),
            label = "orb satellite",
        )
    } else null
    val phase = drift?.value ?: 0.32f

    // Decorative: the orb states no measurement, so it exposes no reading.
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w * .5f
        val cy = h * .46f
        val r = w * .315f

        // ── Halo: the orb sits in its own light, not on a flat field ──────────
        drawCircle(
            Brush.radialGradient(
                listOf(
                    NativePalette.violet.copy(alpha = .30f),
                    NativePalette.blue.copy(alpha = .10f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy), radius = r * 1.85f,
            ),
            radius = r * 1.85f, center = Offset(cx, cy),
        )

        // ── Ground: three perspective rings and a light pool ──────────────────
        val floorY = cy + r * 1.00f
        drawOval(
            Brush.radialGradient(
                listOf(NativePalette.violet.copy(alpha = .34f), NativePalette.cyan.copy(alpha = .10f), Color.Transparent),
                center = Offset(cx, floorY), radius = r * 1.10f,
            ),
            topLeft = Offset(cx - r * 1.08f, floorY - r * .16f),
            size = Size(r * 2.16f, r * .34f),
        )
        listOf(1.00f to .34f, .74f to .22f, .48f to .14f).forEach { (scale, alpha) ->
            drawOval(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        NativePalette.cyan.copy(alpha = alpha),
                        NativePalette.violet.copy(alpha = alpha * .8f),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(cx - r * 1.02f * scale, floorY - r * .13f * scale),
                size = Size(r * 2.04f * scale, r * .26f * scale),
                style = Stroke(1.2.dp.toPx()),
            )
        }

        // ── The glass sphere ─────────────────────────────────────────────────
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF3A1E82), Color(0xFF1B0F4A), Color(0xFF0A0620)),
                center = Offset(cx - r * .28f, cy - r * .34f),
                radius = r * 1.42f,
            ),
            radius = r, center = Offset(cx, cy),
        )
        // Inner violet floor light, as if the sphere were lit from its base.
        drawCircle(
            Brush.radialGradient(
                listOf(NativePalette.violet.copy(alpha = .34f), Color.Transparent),
                center = Offset(cx, cy + r * .58f), radius = r * .86f,
            ),
            radius = r, center = Offset(cx, cy),
        )
        // Fine dotted texture, deterministic — the sphere reads as a volume.
        for (i in 0 until 108) {
            val angle = i * 2.399963f
            val radius = r * sqrt((i + .5f) / 108f) * .92f
            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius * .96f
            val alpha = .05f + .07f * ((i * 37 % 11) / 11f)
            drawCircle(Color.White.copy(alpha = alpha), .7.dp.toPx(), Offset(x, y))
        }
        // Reflected window: one soft specular highlight at the top-left.
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = .22f), Color.Transparent),
                center = Offset(cx - r * .34f, cy - r * .40f), radius = r * .58f,
            ),
            radius = r * .58f, center = Offset(cx - r * .34f, cy - r * .40f),
        )
        // Rim: the whole brand ramp, with a brighter arc where light enters.
        drawCircle(
            Brush.sweepGradient(
                listOf(
                    NativePalette.violet, NativePalette.magenta, NativePalette.blue,
                    NativePalette.cyan, NativePalette.violet,
                ),
                center = Offset(cx, cy),
            ),
            radius = r, center = Offset(cx, cy),
            style = Stroke(1.8.dp.toPx()),
            alpha = .85f,
        )
        drawCircle(
            Color.White.copy(alpha = .40f),
            radius = r, center = Offset(cx, cy),
            style = Stroke(1.dp.toPx()),
            alpha = .30f,
        )

        // ── The traced K, at the centre of the sphere ────────────────────────
        val kx = cx
        val ky = cy + r * .02f
        val stem = r * .40f
        val mark = Path().apply {
            moveTo(kx - r * .26f, ky - stem)
            lineTo(kx - r * .26f, ky + stem)
            moveTo(kx - r * .26f, ky)
            lineTo(kx + r * .38f, ky - stem * .88f)
            moveTo(kx - r * .26f, ky)
            lineTo(kx + r * .38f, ky + stem * .88f)
        }
        val markBrush = Brush.linearGradient(
            listOf(Knx.magenta, Knx.violet, Knx.blue, Knx.cyan),
            start = Offset(kx - r * .26f, ky - stem),
            end = Offset(kx + r * .38f, ky + stem),
        )
        // Phosphor under the stroke, then the stroke itself.
        drawPath(mark, markBrush, alpha = .28f, style = Stroke(r * .22f, cap = StrokeCap.Round))
        drawPath(mark, markBrush, style = Stroke(r * .105f, cap = StrokeCap.Round))
        drawPath(mark, Color.White.copy(alpha = .55f), style = Stroke(r * .026f, cap = StrokeCap.Round))

        // ── Orbit rail and its satellite ─────────────────────────────────────
        rotate(-18f, pivot = Offset(cx, cy)) {
            drawOval(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        NativePalette.cyan.copy(alpha = .55f),
                        NativePalette.violet.copy(alpha = .55f),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(cx - r * 1.42f, cy - r * .30f),
                size = Size(r * 2.84f, r * .60f),
                style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round),
            )
            val angle = phase * 2f * PI.toFloat()
            val sx = cx + r * 1.42f * cos(angle)
            val sy = cy + r * .30f * sin(angle)
            drawCircle(NativePalette.cyan.copy(alpha = .22f), r * .10f, Offset(sx, sy))
            drawCircle(Color.White.copy(alpha = .95f), r * .032f, Offset(sx, sy))
        }
    }
}
