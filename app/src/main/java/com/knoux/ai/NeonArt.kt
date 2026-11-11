package com.knoux.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlin.math.*

internal val Ink = Color(0xff05020c)
internal val Chalk = Color(0xfffdfbff)
internal val Mist = Color(0xffe2ddfb)
internal val Violet = Color(0xffbd6bff)
internal val Pink = Color(0xfff275ff)
internal val Cyan = Color(0xff1de9ff)
internal val Gold = Color(0xffffcc55)
internal val Mint = Color(0xff4df7c0)
private val Clear = Color.Transparent

private fun path(data: String) = PathParser().parsePathString(data).toPath()
internal fun glyphPath(mark: Mark): Path? = symbol[mark]
private val symbol = mapOf(
    Mark.MENU to "M3 6H21 M3 12H21 M3 18H21",
    Mark.BACK to "M20 12H4 L11 5 M4 12L11 19",
    Mark.NEXT to "M9 5L16 12L9 19",
    Mark.GEAR to "M10 2H14L15 5L18 6L21 6L22 10L20 12L20 14L22 16L20 20L17 19L15 20L14 22H10L9 19L6 18L3 19L2 15L4 13V11L2 9L4 5L7 6L9 5Z M15.5 12A3.5 3.5 0 1 1 8.5 12A3.5 3.5 0 1 1 15.5 12",
    Mark.CROWN to "M2 7L6 11L12 4L18 11L22 7L20 18H4Z M5 21H19",
    Mark.HOME to "M3 10L12 2L21 10V21H15V14H9V21H3Z",
    Mark.BROOM to "M13 11L20 2Q21 1 22 3L16 13 M9 10L17 15L15 18L6 13Z M6 13Q5 17 1 18Q7 23 16 22L15 18 M7 21L9 16",
    Mark.GRID to "M3 3H9V9H3Z M15 3H21V9H15Z M3 15H9V21H3Z M15 15H21V21H15Z",
    Mark.BARS to "M4 21V14H7V21Z M10 21V8H13V21Z M16 21V3H19V21Z",
    Mark.SHIELD to "M12 2Q17 5 21 6V12Q20 18 12 22Q4 18 3 12V6Q7 5 12 2Z M8 12L11 15L16 9",
    Mark.PICTURES to "M3 5H21V20H3Z M3 17L8 12L12 16L16 11L21 16 M9 9H9.2",
    Mark.RADAR to "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12 M18 12A6 6 0 1 1 6 12A6 6 0 1 1 18 12 M12 12L19 5 M10 15H10.2",
    Mark.LAYERS to "M12 2L22 7L12 12L2 7Z M2 12L12 17L22 12 M2 17L12 22L22 17",
    Mark.DISK to "M21 6C21 10 3 10 3 6C3 2 21 2 21 6Z M3 6V18C3 22 21 22 21 18V6 M3 12C3 16 21 16 21 12",
    Mark.BATTERY to "M9 5V2H15V5 M6 5H18Q20 5 20 7V21H4V7Q4 5 6 5Z M13 8L9 14H13L11 18L16 12H12Z",
    Mark.CHIP to "M7 5H17Q19 5 19 7V17Q19 19 17 19H7Q5 19 5 17V7Q5 5 7 5Z M8 2V5 M12 2V5 M16 2V5 M8 19V22 M12 19V22 M16 19V22 M2 8H5 M2 12H5 M2 16H5 M19 8H22 M19 12H22 M19 16H22 M8 15L11 9L14 15 M9 13H13 M16 9V15",
    Mark.SEARCH to "M17 10A7 7 0 1 1 3 10A7 7 0 1 1 17 10 M15 15L21 21",
    Mark.FILE to "M5 2H14L20 8V22H5Z M14 2V8H20 M8 12H17 M8 16H15",
    Mark.CAMERA to "M2 6H7L9 3H15L17 6H22V21H2Z M17 13A5 5 0 1 1 7 13A5 5 0 1 1 17 13",
    Mark.MIC to "M9 5Q9 2 12 2Q15 2 15 5V12Q15 15 12 15Q9 15 9 12Z M5 11V12Q5 19 12 19Q19 19 19 12V11 M12 19V22 M8 22H16",
    Mark.CLIP to "M8 5H4V22H20V5H16 M8 3H16V8H8Z M8 12H16 M8 16H16",
    Mark.LOCATION to "M20 10Q20 15 12 22Q4 15 4 10A8 8 0 1 1 20 10 M15 10A3 3 0 1 1 9 10A3 3 0 1 1 15 10",
    Mark.OVERLAY to "M7 16H2V3H17V7 M7 7H22V21H7Z",
    Mark.BELL to "M3 17Q6 14 6 9A6 6 0 0 1 18 9Q18 14 21 17Z M9 20Q12 24 15 20",
    Mark.PERSON to "M7 3H3V7 M17 3H21V7 M3 17V21H7 M17 21H21V17 M15 8A3 3 0 1 1 9 8A3 3 0 1 1 15 8 M6 17Q6 12 12 12Q18 12 18 17Z",
    Mark.VIDEO to "M2 5H22V20H2Z M10 9L16 13L10 16Z",
    Mark.MUSIC to "M9 17V5L21 2V15 M9 9L21 6 M9 17Q8 21 4 21Q1 20 3 17Q6 14 9 17 M21 15Q20 19 16 19Q13 18 15 15Q18 12 21 15",
    Mark.SHARE to "M8 12A3 3 0 1 1 2 12A3 3 0 1 1 8 12 M22 5A3 3 0 1 1 16 5A3 3 0 1 1 22 5 M22 19A3 3 0 1 1 16 19A3 3 0 1 1 22 19 M8 11L16 6 M8 13L16 18",
    Mark.CHECK to "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12 M7 12L11 16L17 9",
    Mark.WARN to "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12 M12 7V13 M12 17H12.1",
    Mark.CALENDAR to "M3 5H21V22H3Z M3 10H21 M8 2V7 M16 2V7",
    Mark.CLOCK to "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12 M12 6V12L16 15",
    Mark.EYE to "M1 12Q12 0 23 12Q12 24 1 12Z M15 12A3 3 0 1 1 9 12A3 3 0 1 1 15 12",
    Mark.PLAY to "M6 3L21 12L6 21Z",
    Mark.PAUSE to "M8 4V20 M16 4V20",
    Mark.TRASH to "M3 6H21 M9 6V3H15V6 M5 6L6 22H18L19 6 M10 10V18 M14 10V18",
    Mark.FOLDER to "M2 5H9L12 8H22V20H2Z",
    Mark.LINK to "M9 14L15 8 M7 9L4 12Q0 16 4 20Q7 23 11 20L14 17 M10 7L13 4Q17 0 21 4Q24 7 21 11L17 14",
    Mark.CHAT to "M4 17L2 22L8 20Q16 23 21 16Q25 8 18 4Q10 -1 4 5Q0 11 4 17Z M7 12H7.1 M12 12H12.1 M17 12H17.1",
    Mark.GAME to "M7 6H17Q21 6 22 16Q23 20 20 20L16 16H8L4 20Q1 20 2 16Q3 6 7 6Z M7 9V14 M4.5 11.5H9.5 M16 10H16.1 M19 13H19.1",
    Mark.BOX to "M12 2L22 7V17L12 22L2 17V7Z M2 7L12 12L22 7 M12 12V22",
    Mark.DOWNLOAD to "M12 2V15 M7 10L12 15L17 10 M3 15V21H21V15",
    Mark.HEART to "M12 21L3 12Q0 8 4 4Q8 1 12 6Q16 1 20 4Q24 8 21 12Z M5 12H8L10 9L13 15L16 11H19",
    Mark.BOLT to "M14 2L4 14H11L9 22L20 10H13Z",
    Mark.SUN to "M17 12A5 5 0 1 1 7 12A5 5 0 1 1 17 12 M12 1V4 M12 20V23 M1 12H4 M20 12H23 M4 4L6 6 M18 18L20 20 M20 4L18 6 M6 18L4 20",
    Mark.SYNC to "M3 10A9 9 0 0 1 19 5L22 8 M22 2V8H16 M21 14A9 9 0 0 1 5 19L2 16 M2 22V16H8",
    Mark.POWER to "M12 2V12 M7 5A9 9 0 1 0 17 5",
    Mark.TEMP to "M10 15V4A2 2 0 0 1 14 4V15A5 5 0 1 1 10 15 M12 10V18 M12 19H12.1",
    Mark.ROCKET to "M7 15Q9 4 22 2Q22 14 11 17Z M9 10L5 10L2 15L7 15 M14 15L14 19L9 22L9 17 M5 18L2 22 M17 8A1.5 1.5 0 1 1 14 8A1.5 1.5 0 1 1 17 8",
    Mark.GROUP to "M13 7A4 4 0 1 1 5 7A4 4 0 1 1 13 7 M3 21V17Q3 13 9 13Q15 13 15 17V21Z M16 4A4 4 0 0 1 16 11 M18 14Q22 15 22 19V21H18",
    Mark.SPEED to "M3 20A10 10 0 1 1 21 20 M12 13L17 7 M12 3V5 M4 11L6 12 M20 11L18 12",
    Mark.TROPHY to "M7 3H17V11Q17 16 12 16Q7 16 7 11Z M7 5H2Q2 12 7 12 M17 5H22Q22 12 17 12 M12 16V21 M7 21H17",
    Mark.MEMORY to "M2 5H22V18H2Z M6 8H8V14H6Z M11 8H13V14H11Z M16 8H18V14H16Z M6 18V21 M12 18V21 M18 18V21"
).mapValues { (_, svg) -> path(svg) }

// Brand K traced from the owner logo (design/brand/knoux-logo-owner.png),
// normalized from the 1000-unit splash space into this 24-unit glyph box.
private val kPlanes = listOf(
    path("M8.97 0.66H15.87L11.18 8.96L3.9 13.53L8.22 1.42Q8.22 0.66 8.97 0.66Z"),
    path("M0.99 19.41Q4.28 19.67 7.57 19.41C11.99 14.61 18.45 9.76 23.03 5.45C13.6 8.04 4.44 14.88 0.99 19.41Z"),
    path("M11.72 11.49H16.08L21.41 19.46H14.52Z")
)

private fun DrawScope.knouxK(bright: Boolean = false) {
    val colors = listOf(listOf(Color(0xfff7a8ff), Color(0xffa62bff)), listOf(Color(0xffe77dff), Color(0xff6f2ffa), Cyan), listOf(Color(0xff3f63ff), Cyan))
    val axes = listOf(Offset(12f, .7f) to Offset(7f, 13.5f), Offset(23f, 5.5f) to Offset(2f, 19.4f), Offset(12f, 11.5f) to Offset(21f, 19.5f))
    kPlanes.forEachIndexed { index, p ->
        if (bright) {
            drawPath(p, Violet.copy(alpha = .19f), style = Stroke(3f, join = StrokeJoin.Round))
            drawPath(p, Pink.copy(alpha = .4f), style = Stroke(1.2f, join = StrokeJoin.Round))
        }
        drawPath(p, Brush.linearGradient(colors[index], axes[index].first, axes[index].second))
        if (bright) drawPath(p, Chalk.copy(alpha = .65f), style = Stroke(.28f, join = StrokeJoin.Round))
    }
}

@Composable
internal fun Glyph(mark: Mark, modifier: Modifier = Modifier, color: Color = Chalk, glow: Boolean = false) {
    Canvas(modifier.size(22.dp)) {
        val unit = size.minDimension / 24f
        translate((size.width - 24 * unit) / 2, (size.height - 24 * unit) / 2) {
            scale(unit, unit, Offset.Zero) {
                if (mark == Mark.K) knouxK(glow) else symbol[mark]?.let {
                    if (glow) {
                        drawPath(it, color.copy(alpha = .20f), style = Stroke(6.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawPath(it, color.copy(alpha = .40f), style = Stroke(3.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    drawPath(it, color, style = Stroke(1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

private fun DrawScope.neonPath(p: Path, tone: Color = Violet, fill: Brush? = null, width: Float = 1.15f) {
    for ((size, alpha) in listOf(13f to .045f, 8f to .10f, 4f to .24f, 2.5f to .42f))
        drawPath(p, tone.copy(alpha = alpha), style = Stroke(size * width, join = StrokeJoin.Round, cap = StrokeCap.Round))
    if (fill != null) drawPath(p, fill)
    drawPath(p, tone.copy(alpha = .94f), style = Stroke(1.65f * width, join = StrokeJoin.Round, cap = StrokeCap.Round))
    drawPath(p, Chalk.copy(alpha = .90f), style = Stroke(.68f * width, join = StrokeJoin.Round, cap = StrokeCap.Round))
}

private fun glassFill(start: Offset = Offset(-25f, -38f), end: Offset = Offset(30f, 38f)) =
    Brush.linearGradient(listOf(Color(0xffff9bff), Color(0xff6a2be8), Color(0xff24105f), Color(0xff0f66e6), Cyan.copy(alpha = .7f)), start, end)

@Composable
internal fun HomeTileIcon(mark: Mark, tone: Color) {
    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val platform = Offset(size.width / 2f, size.height * .84f)
            // Soft light pool only - no hard ring, so the icon keeps a clean base.
            drawOval(Brush.radialGradient(listOf(tone.copy(alpha = .30f), Color.Transparent),
                platform, size.width * .40f), Offset(size.width * .26f, platform.y - size.height * .10f),
                Size(size.width * .48f, size.height * .20f))
            val rim = Size(size.width * .72f, size.height * .15f)
            drawOval(Brush.horizontalGradient(listOf(Color.Transparent, tone.copy(alpha = .8f), Color.Transparent)),
                platform - Offset(rim.width / 2f, rim.height / 2f), rim, style = Stroke(.9.dp.toPx()))
        }
        VolumetricGlyph(mark, tone, 30.dp)
    }
}

/**
 * The hero composition shared by every screen: a deep atmosphere, layered light
 * ribbons, a luminous pedestal, segmented orbital arcs and the screen-specific
 * artwork sitting on top of it. The Home hero replaces the artwork with the
 * volumetric K globe.
 */
@Composable
internal fun NeonHero(mark: Mark, motion: Boolean, modifier: Modifier = Modifier) {
    val home = mark == Mark.K
    val homeTall = LocalTall.current
    val height = if (home) (if (homeTall) 162.dp else 150.dp) else 112.dp
    val unitDivisor = if (home) (if (homeTall) 150f else 140f) else 134f
    val tone = heroTone(mark)
    val points = rememberGlobePoints(if (home) 520 else 0)
    val phase = if (motion) {
        val transition = rememberInfiniteTransition(label = "hero drift")
        transition.animateFloat(0f, 360f,
            infiniteRepeatable(tween(42000, easing = LinearEasing)), label = "hero phase")
    } else null
    Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        // Layer 1 - static scene: atmosphere, ribbons, pedestal, arcs, artwork.
        Canvas(Modifier.matchParentSize()) {
            atmosphere(size.width, size.height, tone)
            ribbonField(size.width, size.height, 0f, if (home) 1f else .72f)
            val x = size.width / 2f
            val h = size.height
            val cy = h * .44f
            val unit = h / unitDivisor
            translate(x, cy) {
                scale(unit, unit, Offset.Zero) {
                    val radius = if (home) 58f else 52f
                    val pedestalY = if (home) 64f else 56f
                    bloom(Offset.Zero, radius * 1.70f, tone, .50f)
                    lightPedestal(radius * .92f, pedestalY, if (home) Light.magenta else tone)
                    heroArcs(radius * 1.00f, 0f)
                    if (!home) orbitalArt(mark)
                }
            }
        }
        if (home) {
            // Layer 2 - the particle globe spins as a cached layer.
            Canvas(Modifier.matchParentSize().graphicsLayer {
                val radians = (phase?.value ?: 0f) * PI.toFloat() / 180f
                rotationZ = sin(radians) * 5f
            }) {
                val x = size.width / 2f
                val h = size.height
                val cy = h * .44f
                val unit = h / unitDivisor
                translate(x, cy) { scale(unit, unit, Offset.Zero) { drawGlobe(58f * .94f, points) } }
            }
            // Layer 3 - the brand mark, crisp and in front of the globe.
            Canvas(Modifier.matchParentSize()) {
                val x = size.width / 2f
                val h = size.height
                val cy = h * .44f
                val unit = h / unitDivisor
                translate(x, cy) { scale(unit, unit, Offset.Zero) { glowingK(58f * 1.02f) } }
            }
        }
        if (phase != null) Canvas(Modifier.matchParentSize()) {
            val unit = size.height / unitDivisor
            val radius = unit * if (home) 68f else 62f
            val center = Offset(size.width / 2f, size.height * .44f)
            drawArc(Light.cyan.copy(alpha = .68f), phase.value - 100f, 11f, false,
                center - Offset(radius, radius), Size(radius * 2f, radius * 2f),
                style = Stroke(unit * 1.2f, cap = StrokeCap.Round))
        }
    }
}

/** Brand K drawn in front of the globe with its own bloom. */
private fun DrawScope.glowingK(size: Float) {
    val s = size / 24f
    translate(-size / 2f, -size / 2f) {
        scale(s, s, Offset.Zero) {
            // Shadow separation so the mark stays crisp against the particle shell.
            for (p in kPlanes) drawPath(p, Color(0xff06021a).copy(alpha = .60f), style = Stroke(2.3f, join = StrokeJoin.Round))
            for ((w, a) in listOf(6f to .10f, 3.8f to .15f, 1.6f to .30f)) {
                kPlanes.forEach { p -> drawPath(p, Light.magenta.copy(alpha = a), style = Stroke(w, join = StrokeJoin.Round)) }
            }
            knouxK()
            kPlanes.forEach { drawPath(it, Light.ice.copy(alpha = .55f), style = Stroke(.23f, join = StrokeJoin.Round)) }
        }
    }
}

internal fun heroTone(mark: Mark): Color = when (mark) {
    Mark.K, Mark.LAYERS, Mark.ROCKET -> Light.violet
    Mark.SHIELD, Mark.BROOM, Mark.TRASH -> Light.magenta
    Mark.RADAR, Mark.DISK -> Light.cyan
    Mark.BATTERY, Mark.PICTURES -> Light.azure
    Mark.CHIP, Mark.GRID -> Light.pink
    else -> Light.violet
}

private fun DrawScope.orbitalArt(mark: Mark) {
    val orbit = 61f
    // Inner halo keeps the artwork luminous without flattening the composition.
    drawCircle(Brush.radialGradient(listOf(Light.violet.copy(alpha = .42f), Color(0xff1a53d6).copy(alpha = .18f), Clear), Offset.Zero, 86f), 86f, Offset.Zero)
    // Fine measurement ring behind the artwork.
    for (i in 0..54) {
        val radians = (i * 6.66f * PI / 180).toFloat()
        val v = Offset(cos(radians), sin(radians))
        if (i % 8 < 6) drawLine(if (i % 3 == 0) Light.cyan.copy(alpha = .40f) else Light.violet.copy(alpha = .24f),
            v * (orbit - 3), v * (orbit - if (i % 5 == 0) .5f else 2.4f), .62f)
    }
    when (mark) {
        Mark.RADAR -> sphere(false)
        Mark.TRASH -> translate(-46f, -46f) { volumetricMark(Mark.TRASH, Light.pink, 46f) }
        Mark.GROUP -> translate(-43f, -43f) { volumetricMark(Mark.GROUP, Light.azure, 43f) }
        Mark.SHIELD -> {
            neonPath(path("M0 -48Q20 -34 49 -31L46 0Q39 30 0 49Q-39 30 -46 0L-49 -31Q-20 -34 0 -48Z"), Cyan, glassFill())
            neonPath(path("M0 -38Q20 -27 40 -25L37 0Q29 28 0 40Q-30 26 -37 0L-40 -25Q-20 -28 0 -38Z"), Pink, Brush.linearGradient(listOf(Color(0xff3534c9), Color(0xff110b44), Color(0xff8122b8)), Offset(-25f,-32f), Offset(35f,34f)), .55f)
            neonPath(path("M-11 -3V-11A11 11 0 0 1 11 -11V-3 M-14 -4H14Q18 -4 18 1V19Q18 24 13 24H-13Q-18 24 -18 19V1Q-18 -4 -14 -4Z"), Pink, glassFill(), 1.7f)
            drawCircle(Color(0xff261050), 4.2f, Offset(0f, 8f))
            drawRoundRect(Color(0xff261050), Offset(-1.8f, 8f), Size(3.6f, 9f), CornerRadius(1f))
        }
        Mark.PICTURES -> {
            rotate(14f, Offset(21f, -10f)) {
                neonPath(path("M1 -48H37L52 -32V26Q52 33 44 33H-5Q-13 33 -13 25V-39Q-13 -48 1 -48Z M37 -47V-32H51"), Pink, glassFill())
            }
            rotate(6f, Offset(-15f, 7f)) {
                neonPath(path("M-47 -31H14Q25 -31 25 -20V33Q25 44 14 44H-47Q-57 44 -57 33V-20Q-57 -31 -47 -31Z"), Pink, glassFill(), 1.5f)
                neonPath(path("M-50 31L-30 5L-14 22L-1 10L19 32Z M9 -12A6 6 0 1 1 -3 -12A6 6 0 1 1 9 -12"), Pink, glassFill(), .8f)
            }
        }
        Mark.BARS -> {
            for (i in 0..3) {
                val x = -46f + 24f * i
                val y = 15f - i * 13
                val p = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(x, y, x + 18, 45f, CornerRadius(3.5f))) }
                neonPath(p, if (i > 1) Cyan else Pink, Brush.verticalGradient(listOf(Pink, Violet, Color(0xff233dca), Cyan), y, 45f))
            }
            neonPath(path("M-48 0Q-6 -8 31 -40L24 -43L44 -49L39 -29L36 -36Q-5 -3 -48 0Z"), Pink, glassFill(), .8f)
        }
        Mark.LAYERS -> {
            for (y in listOf(23f, 3f, -18f)) translate(0f, y) {
                neonPath(path("M-45 0L0 -22L45 0V5L0 27L-45 5Z"), Pink, Brush.verticalGradient(listOf(Pink, Violet, Color(0xff6022a5), Pink), -23f, 27f))
                neonPath(path("M-45 0L0 -22L45 0L0 22Z"), Pink, glassFill(Offset(-35f,-22f), Offset(33f,27f)), .76f)
            }
        }
        Mark.GRID -> {
            val casing = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(-46f,-42f,46f,44f,CornerRadius(22f))) }
            neonPath(casing, Cyan, Brush.linearGradient(listOf(Pink, Color(0xff7223c6), Color(0xff100b37), Color(0xff1640ba), Cyan), Offset(-42f,-40f), Offset(46f,43f)), 1.55f)
            for (y in listOf(-24f, 7f)) for (x in listOf(-28f, 5f)) {
                val p = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(x,y,x+23,y+23,CornerRadius(6f))) }
                neonPath(p, if (x > 0 && y > 0) Cyan else Pink, glassFill(Offset(x,y), Offset(x+23,y+23)))
            }
        }
        Mark.DISK -> for (y in listOf(26f, 11f, -4f, -21f)) translate(0f,y) {
            neonPath(path("M-41 -7C-41 -26 41 -26 41 -7V6C41 27 -41 27 -41 6Z"), Cyan, Brush.horizontalGradient(listOf(Pink, Color(0xff6532db), Color(0xff19146a), Color(0xff126cc2), Cyan), -42f,42f))
            neonPath(path("M-41 -7C-41 -26 41 -26 41 -7C41 13 -41 13 -41 -7Z"), Pink, glassFill(Offset(-34f,-14f), Offset(39f,4f)), .8f)
        }
        Mark.BATTERY -> {
            neonPath(path("M-13 -40V-48Q0 -54 13 -48V-40Z"), Pink, glassFill())
            neonPath(path("M-32 -33C-32 -47 32 -47 32 -33V38C32 53 -32 53 -32 38Z"), Cyan, glassFill())
            neonPath(path("M-29 -30C-12 -23 15 -23 29 -30V37C29 45 -29 45 -29 37Z"), Cyan, Brush.verticalGradient(listOf(Color(0xff311064), Color(0xff18134e), Color(0xff1542c9), Cyan), -30f,43f), .8f)
            drawOval(Cyan.copy(alpha = .45f), Offset(-27f,24f), Size(54f,12f))
            neonPath(path("M7 -23L-12 2H0L-5 22L15 -5H3Z"), Pink, Brush.verticalGradient(listOf(Chalk,Pink,Violet), -21f,20f))
        }
        Mark.CHIP -> {
            for (i in listOf(-20f, -7f, 7f, 20f)) for (rotation in listOf(0f,90f,180f,270f)) rotate(rotation,Offset.Zero) {
                neonPath(path("M${i-2} -35V-49Q$i -53 ${i+2} -49V-35Z"), Pink, glassFill(), .75f)
            }
            val chip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(-37f,-35f,37f,36f,CornerRadius(13f))) }
            neonPath(chip, Pink, Brush.linearGradient(listOf(Pink, Violet, Color(0xff1d0f56), Color(0xff281a72)), Offset(-35f,-33f), Offset(30f,35f)), 1.8f)
            val inside = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(-29f,-27f,29f,28f,CornerRadius(8f))) }
            drawPath(inside, Brush.radialGradient(listOf(Color(0xff362086), Color(0xff1a1049)), Offset.Zero, 45f))
            neonPath(path("M-22 14L-6 -15H2L6 14H-1L-2 7H-12L-16 14Z M-9 1H-3L-4 -8Z M12 14L18 -15H26L20 14Z"), Pink, glassFill(), .63f)
        }
        else -> symbol[mark]?.let { scale(3.4f, 3.4f, Offset.Zero) { translate(-12f, -12f) { neonPath(it, Pink) } } }
    }
}

private fun DrawScope.sphere(home: Boolean) {
    val r = if (home) 55f else 49f
    drawCircle(Brush.radialGradient(listOf(Pink.copy(alpha = .20f), Color(0xff2549d7).copy(alpha = .12f), Clear), Offset.Zero, r + 24), r + 24, Offset.Zero)
    drawCircle(Brush.radialGradient(listOf(Color(0xff05051e), Color(0xff100944), Color(0xff233adb), Color(0xff6c32f6), Clear), Offset.Zero, r + 11), r + 11, Offset.Zero)
    drawCircle(Brush.radialGradient(listOf(Color(0xff2b3fd6).copy(alpha = .55f), Color(0xff181158).copy(alpha = .25f), Clear), Offset(-r * .18f, -r * .22f), r * 1.05f), r * 1.05f, Offset(-r * .18f, -r * .22f))
    val rim = Brush.sweepGradient(listOf(Cyan, Color(0xff3079fa), Pink, Color(0xff8561ff), Chalk, Cyan), Offset.Zero)
    drawCircle(rim, r, Offset.Zero, style = Stroke(2.4f))
    drawCircle(rim, r + 1, Offset.Zero, alpha = .35f, style = Stroke(7f))
    drawArc(Chalk.copy(alpha = .75f), -150f, 70f, false, Offset(-r, -r), Size(r * 2, r * 2), style = Stroke(1.6f, cap = StrokeCap.Round))
    if (home) {
        for (band in 0..10) {
            val p = Path()
            for (i in 0..100) {
                val t = i * 2 * PI / 100
                val jitter = (sin(t * (3 + band % 4) + band * .8) * 4 + cos(t * 7 - band) * 2).toFloat()
                val xx = cos(t).toFloat() * (r - 4 - band * .55f + jitter)
                val yy = sin(t).toFloat() * (r - 4 - band * .55f + jitter)
                if (i == 0) p.moveTo(xx, yy) else p.lineTo(xx, yy)
                if (i % 2 == 0) drawCircle(if (band % 3 == 0) Cyan.copy(alpha = .6f) else Pink.copy(alpha = .46f), .38f, Offset(xx * (.84f - band * .02f), yy * .91f))
            }
            drawPath(p, rim, alpha = if (band == 1) .92f else .34f, style = Stroke(if (band == 1) .82f else .44f))
        }
        drawOval(Brush.radialGradient(listOf(Chalk.copy(alpha = .22f), Clear), Offset(-r * .38f, -r * .46f), r * .62f),
            Offset(-r * .72f, -r * .78f), Size(r * .8f, r * .5f))
        translate(-27f,-27f) { scale(2.25f,2.25f,Offset.Zero) { knouxK(true) } }
    } else {
        for (i in 1..4) drawCircle(Cyan.copy(alpha = .37f), r * i / 4f, Offset.Zero, style = Stroke(.65f))
        for (i in 0..5) rotate(i * 30f, Offset.Zero) { drawLine(Cyan.copy(alpha = .45f), Offset(-r,0f), Offset(r,0f),.6f) }
        val beam = path("M0 0L22 -45Q31 -42 39 -32Z")
        drawPath(beam, Brush.linearGradient(listOf(Chalk, Pink, Clear), Offset(31f,-33f), Offset(-5f,5f)), alpha = .72f)
        drawLine(Pink, Offset.Zero, Offset(38f,-32f), 1.7f)
        for (p in listOf(Offset.Zero,Offset(32f,-4f),Offset(-18f,18f),Offset(18f,24f))) {
            drawCircle(Brush.radialGradient(listOf(Pink,Clear),p,9f),9f,p)
            drawCircle(Chalk,2.2f,p)
        }
    }
}
