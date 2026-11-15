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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The hub's device instrument.
 *
 * It is drawn, not photographed: a lit handset held inside two orbit rings whose
 * satellites carry the brand's two colours. Nothing here encodes a measurement,
 * so the artwork stays decorative — a real figure is always printed as a number
 * beside it, never implied by the drawing.
 *
 * Motion is a slow orbit drift. It is dropped entirely when the customer turns
 * motion off, leaving a composed still frame rather than a frozen animation.
 */
@Composable
internal fun DeviceHero(modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    val drift = if (motion) {
        val transition = rememberInfiniteTransition(label = "hero orbit")
        transition.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
            label = "orbit angle",
        )
    } else null
    val angle = drift?.value ?: 318f

    // Decorative: the instrument states no measurement, so it exposes no reading.
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.50f
        val ringR = w * 0.46f

        // Floor light: the instrument sits on a pool of the brand, not in a void.
        drawOval(
            Brush.radialGradient(
                listOf(NativePalette.violet.copy(alpha = .30f), Color.Transparent),
                center = Offset(cx, h * 0.855f),
                radius = w * 0.34f,
            ),
            topLeft = Offset(cx - w * 0.28f, h * 0.82f),
            size = Size(w * 0.56f, h * 0.07f),
        )

        // Two tilted orbits, each with one travelling satellite. The rings are drawn
        // bright enough to read as rails, so the satellites sit on a visible path.
        listOf(-20f to NativePalette.cyan, 22f to NativePalette.violet).forEachIndexed { index, (tilt, tint) ->
            rotate(tilt, pivot = Offset(cx, cy)) {
                drawOval(
                    Brush.sweepGradient(
                        listOf(
                            tint.copy(alpha = .10f),
                            tint.copy(alpha = .85f),
                            tint.copy(alpha = .20f),
                            Color.Transparent,
                            tint.copy(alpha = .10f),
                        ),
                        center = Offset(cx, cy),
                    ),
                    topLeft = Offset(cx - ringR, cy - ringR * 0.30f),
                    size = Size(ringR * 2f, ringR * 0.60f),
                    style = Stroke(width = w * 0.012f, cap = StrokeCap.Round),
                )
                val satellite = angle + if (index == 0) 0f else 180f
                val sx = cx + ringR * cos(Math.toRadians(satellite.toDouble())).toFloat()
                val sy = cy + ringR * 0.30f * sin(Math.toRadians(satellite.toDouble())).toFloat()
                drawCircle(tint.copy(alpha = .22f), w * 0.042f, Offset(sx, sy))
                drawCircle(tint, w * 0.017f, Offset(sx, sy))
            }
        }

        // The handset: a lit slab with a gradient rim and a real screen inset.
        rotate(-9f, pivot = Offset(cx, cy)) {
            val bodyW = w * 0.42f
            val bodyH = h * 0.58f
            val body = Offset(cx - bodyW / 2f, cy - bodyH / 2f)
            val screen = CornerRadius(w * 0.065f, w * 0.065f)
            drawRoundRect(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E2A5C), Color(0xFF0A0F26)),
                    startY = body.y, endY = body.y + bodyH,
                ),
                topLeft = body, size = Size(bodyW, bodyH), cornerRadius = screen,
            )
            drawRoundRect(
                Brush.linearGradient(
                    listOf(NativePalette.violet, NativePalette.blue, NativePalette.cyan),
                    start = Offset(body.x, body.y), end = Offset(body.x + bodyW, body.y + bodyH),
                ),
                topLeft = body, size = Size(bodyW, bodyH), cornerRadius = screen,
                style = Stroke(width = w * 0.011f),
            )
            // Screen bloom, cast from the lower edge like the reference field.
            val inset = w * 0.020f
            drawRoundRect(
                Brush.radialGradient(
                    listOf(NativePalette.cyan.copy(alpha = .26f), NativePalette.violet.copy(alpha = .10f), Color.Transparent),
                    center = Offset(cx, cy + bodyH * 0.16f),
                    radius = bodyW * 0.80f,
                ),
                topLeft = Offset(body.x + inset, body.y + inset),
                size = Size(bodyW - inset * 2f, bodyH - inset * 2f),
                cornerRadius = CornerRadius(w * 0.050f, w * 0.050f),
            )
            // Earpiece slot: one honest hardware detail, so the slab reads as a handset.
            drawRoundRect(
                Color.White.copy(alpha = .22f),
                topLeft = Offset(cx - bodyW * 0.10f, body.y + bodyH * 0.055f),
                size = Size(bodyW * 0.20f, bodyH * 0.010f),
                cornerRadius = CornerRadius(w * 0.006f, w * 0.006f),
            )
            // The traced K, at the scale of a mark rather than a logo lockup.
            val kx = cx
            val ky = cy + bodyH * 0.02f
            val stem = bodyH * 0.135f
            val mark = Path().apply {
                moveTo(kx - bodyW * 0.115f, ky - stem)
                lineTo(kx - bodyW * 0.115f, ky + stem)
                moveTo(kx - bodyW * 0.115f, ky)
                lineTo(kx + bodyW * 0.145f, ky - stem * 0.88f)
                moveTo(kx - bodyW * 0.115f, ky)
                lineTo(kx + bodyW * 0.145f, ky + stem * 0.88f)
            }
            drawPath(
                mark,
                Brush.linearGradient(
                    listOf(NativePalette.violet, NativePalette.cyan),
                    start = Offset(kx - bodyW * 0.115f, ky - stem),
                    end = Offset(kx + bodyW * 0.145f, ky + stem),
                ),
                style = Stroke(width = w * 0.024f, cap = StrokeCap.Round),
            )
        }
    }
}
