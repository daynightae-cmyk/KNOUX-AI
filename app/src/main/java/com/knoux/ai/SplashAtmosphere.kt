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
private data class SplashRibbon(val crest: Path, val silk: Path, val light: Brush)

private val ribbons = listOf(
    SplashRibbon(
        Path().apply { moveTo(-40f, 360f); cubicTo(102f, 212f, 323f, 272f, 495f, 18f) },
        Path().apply {
            moveTo(-40f, 360f); cubicTo(102f, 212f, 323f, 272f, 495f, 18f)
            cubicTo(348f, 184f, 201f, 107f, -40f, 280f); close()
        },
        Brush.horizontalGradient(0f to Color(0xff6927ff), .64f to Color(0xfff034ff),
            1f to Color.Transparent, startX = 0f, endX = 500f)
    ),
    SplashRibbon(
        Path().apply { moveTo(573f, 365f); cubicTo(738f, 286f, 861f, 153f, 1034f, 106f) },
        Path().apply {
            moveTo(573f, 365f); cubicTo(738f, 286f, 861f, 153f, 1034f, 106f)
            lineTo(1034f, 151f); cubicTo(838f, 182f, 756f, 292f, 573f, 365f); close()
        },
        Brush.horizontalGradient(0f to Color.Transparent, .74f to Color(0xff22d9ff),
            1f to Color(0xff408aff), startX = 570f, endX = 1030f)
    ),
    SplashRibbon(
        Path().apply { moveTo(-34f, 1560f); cubicTo(300f, 1312f, 476f, 1635f, 1034f, 1260f) },
        Path().apply {
            moveTo(-34f, 1560f); cubicTo(300f, 1312f, 476f, 1635f, 1034f, 1260f)
            cubicTo(512f, 1656f, 251f, 1504f, -34f, 1671f); close()
        },
        ribbonLight
    )
)
private val ribbonDust = Path().apply {
    var seed = 731
    for (row in 0..5) {
        for (column in 0..70 step 2) {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val jitter = (seed % 100) / 150f - .33f
            val x = column * 14.5f + jitter * 9f
            val t = (x / 1000f).coerceIn(0f, 1f)
            val envelope = .16f + 2f * (t - .50f) * (t - .50f)
            val offset = row * 14f * envelope + jitter * 7f
            val radius = .55f + (row + column) % 3 * .34f
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
            drawPath(ribbonDust, ribbonLight, alpha = .82f * opacity)
            for (ribbon in ribbons) {
                drawPath(ribbon.silk, ribbon.light, alpha = .16f * opacity)
                drawPath(ribbon.crest, ribbon.light, alpha = .06f * opacity, style = Stroke(92f))
                drawPath(ribbon.crest, ribbon.light, alpha = .12f * opacity, style = Stroke(52f))
                drawPath(ribbon.crest, ribbon.light, alpha = .24f * opacity, style = Stroke(22f))
                drawPath(ribbon.crest, ribbon.light, alpha = .62f * opacity, style = Stroke(7f))
                drawPath(ribbon.crest, ribbon.light, alpha = opacity, style = Stroke(2.6f))
            }
        }
    }
}

private val orbitCenter = Offset(500f, 365f)

internal fun DrawScope.drawSplashOrbit(reveal: Float, sweep: Float) {
    if (reveal <= 0f) return
    rotate(-15f, orbitCenter) {
        // Two open elliptical trails instead of closed wire loops. The lighter
        // outer arc crosses behind the K like a lens flare ring in the reference.
        val orbits = listOf(Triple(424f, 128f, Cyan), Triple(392f, 114f, Pink))
        orbits.forEachIndexed { index, (rx, ry, tint) ->
            val radius = Offset(rx, ry)
            val bounds = Size(radius.x * 2, radius.y * 2)
            val start = -158f + index * 34f
            val extent = (316f - index * 46f) * reveal
            val light = Brush.sweepGradient(
                listOf(Color.Transparent, tint, Color(0xfff2feff), tint.copy(alpha = .55f), Color.Transparent),
                orbitCenter
            )
            drawArc(light, start, extent, false, orbitCenter - radius, bounds,
                alpha = .22f * reveal, style = Stroke(17f - index * 4f))
            drawArc(light, start, extent, false, orbitCenter - radius, bounds,
                alpha = .95f * reveal, style = Stroke(1.6f - index * .25f))
        }
        // Fine stardust fading out along the main trail.
        for (i in 0 until 46) {
            val angle = (-150f + i * 6.4f) * PI.toFloat() / 180f
            val fade = 1f - i / 46f
            val x = orbitCenter.x + 424f * cos(angle)
            val y = orbitCenter.y + 128f * sin(angle)
            drawCircle(Cyan.copy(alpha = .50f * fade * reveal), .9f + (i % 3) * .5f, Offset(x, y))
        }
        val extent = (-150f + 312f * sweep) * PI / 180
        val head = orbitCenter + Offset(424f * cos(extent).toFloat(), 128f * sin(extent).toFloat())
        drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .55f * reveal), Color.Transparent),
            head, 32f), 32f, head)
        drawCircle(Color(0xffe9fdff).copy(alpha = .95f * reveal), 2.1f, head)
    }
}

internal fun DrawScope.drawSplashFlare(point: Offset, strength: Float) {
    if (strength <= 0f) return
    drawCircle(Brush.radialGradient(
        0f to Chalk.copy(alpha = strength), .14f to Color(0xfffdd4ff).copy(alpha = strength * .84f),
        .38f to Pink.copy(alpha = strength * .31f), 1f to Color.Transparent, center = point, radius = 51f
    ), 51f, point)
    for (ray in listOf(Offset(56f, 0f), Offset(0f, 64f), Offset(25f, 25f), Offset(25f, -25f))) {
        val start = point - ray
        val end = point + ray
        val glow = Brush.linearGradient(listOf(Color.Transparent, Pink.copy(alpha = strength * .45f), Color.Transparent), start, end)
        val core = Brush.linearGradient(listOf(Color.Transparent, Chalk.copy(alpha = strength), Color.Transparent), start, end)
        drawLine(glow, start, end, 5f)
        drawLine(core, start, end, 1.6f)
    }
    drawCircle(Chalk.copy(alpha = strength), 4f, point)
}
