package com.knoux.ai

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ribbonLight = Brush.horizontalGradient(
    0f to Color(0xff6829ff), .28f to Color(0xfff03fff), .52f to Color(0xff9272ff),
    .78f to Color(0xff1edbff), 1f to Color(0xff176dff), endX = 1000f
)
private val ribbons = listOf(
    Path().apply { moveTo(-40f, 357f); cubicTo(99f, 193f, 286f, 250f, 495f, 5f) },
    Path().apply { moveTo(578f, 380f); cubicTo(742f, 287f, 844f, 147f, 1034f, 106f) },
    Path().apply { moveTo(-34f, 1560f); cubicTo(355f, 1300f, 434f, 1645f, 1034f, 1260f) }
)
private val ribbonDust = Path().apply {
    for (row in 0..10) {
        for (column in 0..70) {
            val x = column * 14.5f
            val t = x / 1000f
            val envelope = .16f + 2f * (t - .50f) * (t - .50f)
            val offset = row * 18f * envelope
            val radius = .7f + (row + column) % 4 * .29f
            val y = 1510f - 220f * t + 123f * sin(t * PI).toFloat() + offset
            addOval(Rect(x - radius, y - radius, x + radius, y + radius))
            if (column < 29) {
                val upperY = 364f - 590f * t * t - offset
                addOval(Rect(x - radius, upperY - radius, x + radius, upperY + radius))
            }
        }
    }
}

internal fun DrawScope.drawSplashRibbons(opacity: Float, drift: Float) {
    scale(size.width / 1000f, size.height / 1900f, Offset.Zero) {
        translate(0f, drift) {
            drawPath(ribbonDust, ribbonLight, alpha = .49f * opacity)
            for (ribbon in ribbons) {
                drawPath(ribbon, ribbonLight, alpha = .10f * opacity, style = Stroke(56f))
                drawPath(ribbon, ribbonLight, alpha = .18f * opacity, style = Stroke(24f))
                drawPath(ribbon, ribbonLight, alpha = .34f * opacity, style = Stroke(7f))
                drawPath(ribbon, ribbonLight, alpha = .80f * opacity, style = Stroke(1.9f))
            }
        }
    }
}

private val orbitCenter = Offset(500f, 365f)
private val orbitLight = Brush.sweepGradient(
    listOf(Cyan, Color.Transparent, Violet, Pink, Color.Transparent, Cyan), orbitCenter
)

internal fun DrawScope.drawSplashOrbit(reveal: Float, sweep: Float) {
    if (reveal <= 0f) return
    rotate(-15f, orbitCenter) {
        for (index in 0..2) {
            val radius = Offset(406f - index * 20f, 123f - index * 10f)
            val bounds = Size(radius.x * 2, radius.y * 2)
            drawArc(orbitLight, -100f, 359f * reveal, false, orbitCenter - radius, bounds,
                alpha = .12f * reveal, style = Stroke(12f - index * 2f))
            drawArc(orbitLight, -100f, 359f * reveal, false, orbitCenter - radius, bounds,
                alpha = (.64f - index * .15f) * reveal, style = Stroke(2.5f - index * .54f))
        }
        val extent = (-115f + 359f * sweep) * PI / 180
        val head = orbitCenter + Offset(406f * cos(extent).toFloat(), 123f * sin(extent).toFloat())
        drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .46f * reveal), Color.Transparent),
            head, 34f), 34f, head)
    }
}

internal fun DrawScope.drawSplashFlare(point: Offset, strength: Float) {
    if (strength <= 0f) return
    drawCircle(Brush.radialGradient(
        listOf(Chalk.copy(alpha = strength), Pink.copy(alpha = strength * .55f), Color.Transparent),
        point, 42f
    ), 42f, point)
    val horizontal = Offset(47f, 0f)
    val vertical = Offset(0f, 55f)
    for (ray in listOf(horizontal, vertical)) {
        val colors = listOf(Color.Transparent, Chalk.copy(alpha = strength), Color.Transparent)
        drawLine(Brush.linearGradient(colors, point - ray, point + ray), point - ray, point + ray, 2.4f)
    }
    drawCircle(Chalk.copy(alpha = strength), 4f, point)
}
