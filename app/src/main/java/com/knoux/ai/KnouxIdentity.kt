package com.knoux.ai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * KNOUX identity layer.
 *
 * Three things live here and nowhere else:
 * 1. the official lockup — the traced K monogram, the "knoux ai" wordmark and the
 *    neon smile signature that the owner's logo carries;
 * 2. the fourteen service worlds — one dominant hue and one instrument geometry
 *    per service, so a screen is recognisable before a single word is read;
 * 3. the shared signature marks used by cards, footers and empty states.
 *
 * Nothing in this file invents a measurement. An instrument may glow, but every
 * value it shows still comes from the state the screen already owns.
 */

// ---------------------------------------------------------------------------
// Palette
// ---------------------------------------------------------------------------

internal object Brand {
    val ink = Color(0xFF04020D)
    val indigo = Color(0xFF0A0624)
    val plaque = Color(0xFF120C2E)
    val hairline = Color(0xFF2E2558)
    val violet = Color(0xFFB040FF)
    val magenta = Color(0xFFFF4FD8)
    val cyan = Color(0xFF22E4F5)
    val ice = Color(0xFFEAF7FF)
    val gold = Color(0xFFFFC24B)

    /** The signature ramp: magenta → violet → azure → cyan, left to right. */
    val signature = listOf(
        Color(0xFFFF6BFF), Color(0xFFC14BFF), Color(0xFF6D5BFF),
        Color(0xFF3AA8FF), Color(0xFF22E4F5),
    )
    val wordmarkAi = Brush.linearGradient(
        listOf(Color(0xFFFF5CF0), Color(0xFFB45CFF), Color(0xFF38BDF8)),
    )
}

// ---------------------------------------------------------------------------
// The fourteen worlds
// ---------------------------------------------------------------------------

/**
 * One service = one dominant hue + one instrument geometry. The rest of the
 * interface stays quiet so the active world owns the screen.
 */
internal enum class World(
    val primary: Color,
    val secondary: Color,
    val deep: Color,
    val mark: Mark,
) {
    HUB(Color(0xFFB040FF), Color(0xFF22E4F5), Color(0xFF3B1A8E), Mark.K),
    TIDE(Color(0xFF2BE8F5), Color(0xFF3B82F6), Color(0xFF0B3B6B), Mark.BROOM),
    MIRROR(Color(0xFFFF4FD8), Color(0xFF8B5CF6), Color(0xFF4A1266), Mark.PICTURES),
    STRATA(Color(0xFFFFB35C), Color(0xFFE4572E), Color(0xFF5A2A12), Mark.LAYERS),
    RADAR(Color(0xFF22E4F5), Color(0xFFFFC24B), Color(0xFF0C4A57), Mark.SHIELD),
    ABYSS(Color(0xFF2DE2C5), Color(0xFF2563EB), Color(0xFF06283D), Mark.RADAR),
    PRISM(Color(0xFFFF6BD6), Color(0xFF38BDF8), Color(0xFF3B1470), Mark.BARS),
    CONSTELLATION(Color(0xFF9AD8FF), Color(0xFF7C6BFF), Color(0xFF1B2A6B), Mark.GRID),
    TERRAIN(Color(0xFF3DFFB0), Color(0xFF10B981), Color(0xFF064E3B), Mark.DISK),
    PULSE(Color(0xFFFF7A45), Color(0xFF22E4F5), Color(0xFF5B1E14), Mark.BATTERY),
    SYNAPSE(Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF3B0B57), Mark.CHIP),
    IRIS(Color(0xFFFFC24B), Color(0xFFF59E0B), Color(0xFF4A2B06), Mark.PERSON),
    SEAL(Color(0xFFFFD166), Color(0xFF34D399), Color(0xFF4A3A08), Mark.CROWN),
    GATE(Color(0xFFFFE9B0), Color(0xFFA78BFA), Color(0xFF3B2E6B), Mark.SPARKLE),
    COMPASS(Color(0xFF60A5FA), Color(0xFF22E4F5), Color(0xFF12274A), Mark.GEAR),
}

/** The world a hero mark belongs to. Every page reaches its world through this. */
internal fun worldOf(mark: Mark): World = when (mark) {
    Mark.K, Mark.HOME -> World.HUB
    Mark.BROOM, Mark.TRASH -> World.TIDE
    Mark.PICTURES, Mark.COPY -> World.MIRROR
    Mark.LAYERS -> World.STRATA
    Mark.SHIELD, Mark.LOCK, Mark.KEY, Mark.EYE -> World.RADAR
    Mark.RADAR, Mark.SEARCH -> World.ABYSS
    Mark.BARS, Mark.CHART -> World.PRISM
    Mark.GRID, Mark.GROUP -> World.CONSTELLATION
    Mark.DISK, Mark.FOLDER -> World.TERRAIN
    Mark.BATTERY, Mark.BOLT, Mark.POWER -> World.PULSE
    Mark.CHIP -> World.SYNAPSE
    Mark.PERSON, Mark.HEART -> World.IRIS
    Mark.CROWN, Mark.TROPHY -> World.SEAL
    Mark.SPARKLE, Mark.ROCKET -> World.GATE
    Mark.GEAR, Mark.MENU -> World.COMPASS
    else -> World.HUB
}

internal fun worldOf(page: Page): World = worldOf(page.hero)

// ---------------------------------------------------------------------------
// Official lockup
// ---------------------------------------------------------------------------

/**
 * The traced K from the owner's logo, fitted into a box. The three planes keep
 * their original brushes, so the monogram in the header is the same object as
 * the one the splash draws.
 */
private fun DrawScope.drawOfficialK(boxW: Float, boxH: Float, glow: Boolean) {
    val bboxW = 364f
    val bboxH = 312f
    val s = min(boxW / bboxW, boxH / bboxH) * .96f
    val dx = (boxW - bboxW * s) / 2f - 316f * s
    val dy = (boxH - bboxH * s) / 2f - 240f * s
    if (glow) {
        val center = Offset(boxW * .5f, boxH * .52f)
        drawCircle(
            Brush.radialGradient(
                listOf(
                    Color(0xFFFF4FD8).copy(alpha = .34f),
                    Color(0xFF7C3AED).copy(alpha = .22f),
                    Color.Transparent,
                ),
                center, boxH * .72f,
            ),
            boxH * .72f, center,
        )
    }
    translate(dx, dy) {
        scale(s, s, Offset.Zero) {
            drawPath(kUpper, upperBrush)
            drawPath(kLower, lowerBrush)
            drawPath(kBlade, bladeBrush)
            drawPath(
                kUpper, Color(0xFF06021A).copy(alpha = .55f),
                style = Stroke(4f, join = StrokeJoin.Round),
            )
            drawPath(
                kLower, Color(0xFF06021A).copy(alpha = .55f),
                style = Stroke(4f, join = StrokeJoin.Round),
            )
            drawPath(
                kBlade, Color(0xFF06021A).copy(alpha = .55f),
                style = Stroke(3f, join = StrokeJoin.Round),
            )
            drawPath(kUpper, Light.ice.copy(alpha = .92f), style = Stroke(2.4f, join = StrokeJoin.Round))
            drawPath(kLower, Light.ice.copy(alpha = .52f), style = Stroke(1.8f, join = StrokeJoin.Round))
            drawPath(kBlade, Light.white.copy(alpha = .78f), style = Stroke(1.6f, join = StrokeJoin.Round))
        }
    }
}

/** Offscreen measure of the wordmark; shared by the lockup and the signature. */
private fun DrawScope.drawSmileRibbon(x: Float, y: Float, w: Float, h: Float) {
    if (w <= 0f || h <= 0f) return
    val bboxW = 864f
    val bboxH = 173f
    val s = min(w / bboxW, h / bboxH)
    translate(x - 72f * s, y - 757f * s) {
        scale(s, s, Offset.Zero) {
            val ribbon = Path()
            val samples = smileSamples
            samples.firstOrNull()?.let { first ->
                ribbon.moveTo(first.upper.x, first.upper.y)
                for (index in 1 until samples.size) {
                    val edge = samples[index]
                    ribbon.lineTo(edge.upper.x, edge.upper.y)
                }
                for (index in samples.indices.reversed()) {
                    val edge = samples[index]
                    ribbon.lineTo(edge.lower.x, edge.lower.y)
                }
                ribbon.close()
            }
            drawPath(smilePath, smileBrush, alpha = .10f, style = Stroke(72f, cap = StrokeCap.Round))
            drawPath(smilePath, smileBrush, alpha = .20f, style = Stroke(40f, cap = StrokeCap.Round))
            drawPath(ribbon, smileBrush)
            drawPath(ribbon, smileCore, alpha = .58f)
            drawPath(smilePath, smileCore, alpha = .96f, style = Stroke(3.4f, cap = StrokeCap.Round))
            drawCircle(Brush.radialGradient(listOf(Light.white, Brand.cyan.copy(alpha = .5f), Color.Transparent), smileTip, 42f), 42f, smileTip)
        }
    }
}

/** A bare monogram, sized for a badge or a header. */
@Composable
internal fun KnouxMonogram(modifier: Modifier = Modifier, size: Int = 26, glow: Boolean = true) {
    Canvas(modifier.size(size.dp)) {
        drawOfficialK(this.size.width, this.size.height, glow)
    }
}

/**
 * The official lockup: traced K, `knoux` in Outfit ExtraBold, `ai` on the brand
 * gradient, and the neon smile beneath — the owner's signature, not a generic
 * underline. One canvas, so the smile always ends exactly under the wordmark.
 */
@Composable
internal fun KnouxWordmark(
    modifier: Modifier = Modifier,
    size: Int = 20,
    withSignature: Boolean = true,
    glow: Boolean = true,
) {
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val density = LocalDensity.current
    val lead = remember(size) {
        measurer.measure(
            "knoux",
            TextStyle(
                fontFamily = BrandFace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = size.sp,
                letterSpacing = (-0.6).sp,
                color = KnouxColors.textPrimary,
            ),
        )
    }
    val tail = remember(size) {
        measurer.measure(
            " ai",
            TextStyle(
                fontFamily = BrandFace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = size.sp,
                letterSpacing = (-0.4).sp,
                brush = Brand.wordmarkAi,
            ),
        )
    }
    val fontPx = with(density) { size.sp.toPx() }
    val mono = fontPx * 1.12f
    val gap = fontPx * .26f
    val textW = lead.size.width + tail.size.width
    val textH = maxOf(lead.size.height, tail.size.height).toFloat()
    val signH = if (withSignature) fontPx * .46f else 0f
    val widthPx = mono + gap + textW
    val heightPx = textH + signH
    Canvas(
        modifier.size(
            width = (widthPx / density.density).dp,
            height = (heightPx / density.density).dp,
        )
    ) {
        drawOfficialK(mono, textH, glow)
        drawText(lead, topLeft = Offset(mono + gap, (textH - lead.size.height) / 2f))
        drawText(tail, topLeft = Offset(mono + gap + lead.size.width, (textH - tail.size.height) / 2f))
        if (withSignature) {
            drawSmileRibbon(mono + gap, textH - fontPx * .10f, textW.toFloat(), signH)
        }
    }
}

/** The signature on its own: the neon smile, for footers and empty states. */
@Composable
internal fun KnouxSignature(modifier: Modifier = Modifier, width: Int = 132, height: Int = 26) {
    Canvas(modifier.size(width.dp, height.dp)) {
        drawSmileRibbon(0f, 0f, this.size.width, this.size.height)
    }
}

// ---------------------------------------------------------------------------
// Sigils — the small, crisp identity glyph a card can carry
// ---------------------------------------------------------------------------

private fun DrawScope.unit(block: DrawScope.() -> Unit) {
    val side = size.minDimension
    translate((size.width - side) / 2f, (size.height - side) / 2f) {
        scale(side / 100f, side / 100f, Offset.Zero) { block() }
    }
}

/** The tiny emblem of a world. Bold shapes only, legible at 24 dp. */
internal fun DrawScope.drawSigil(world: World, ink: Color, alpha: Float = 1f) {
    unit {
        val line = Stroke(3.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val hair = Stroke(1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (world) {
            World.HUB -> {
                drawArc(ink.copy(alpha = .85f * alpha), 200f, 140f, false, Offset(20f, 20f), Size(60f, 60f), style = line)
                drawArc(ink.copy(alpha = .45f * alpha), 20f, 140f, false, Offset(20f, 20f), Size(60f, 60f), style = line)
                drawCircle(ink.copy(alpha = alpha), 8f, Offset(50f, 50f))
            }
            World.TIDE -> {
                val wave = Path().apply {
                    moveTo(12f, 42f)
                    quadraticTo(30f, 26f, 50f, 42f)
                    quadraticTo(70f, 58f, 88f, 42f)
                }
                val wave2 = Path().apply {
                    moveTo(12f, 62f)
                    quadraticTo(30f, 46f, 50f, 62f)
                    quadraticTo(70f, 78f, 88f, 62f)
                }
                drawPath(wave, ink, style = line)
                drawPath(wave2, ink.copy(alpha = .45f * alpha), style = hair)
            }
            World.MIRROR -> {
                drawCircle(ink.copy(alpha = alpha), 20f, Offset(32f, 50f), style = line)
                drawCircle(ink.copy(alpha = .45f * alpha), 20f, Offset(68f, 50f), style = line)
                drawLine(ink.copy(alpha = .7f * alpha), Offset(44f, 50f), Offset(56f, 50f), strokeWidth = 2.6f, cap = StrokeCap.Round)
            }
            World.STRATA -> {
                for (index in 0..3) {
                    val y = 30f + index * 14f
                    drawLine(
                        ink.copy(alpha = (1f - index * .18f) * alpha), Offset(16f, y), Offset(84f, y),
                        strokeWidth = if (index == 0) 3.4f else 2.2f, cap = StrokeCap.Round,
                    )
                }
            }
            World.RADAR -> {
                drawCircle(ink.copy(alpha = alpha), 26f, Offset(50f, 50f), style = hair)
                drawLine(ink.copy(alpha = .5f * alpha), Offset(50f, 18f), Offset(50f, 82f), strokeWidth = 1.6f)
                drawLine(ink.copy(alpha = .5f * alpha), Offset(18f, 50f), Offset(82f, 50f), strokeWidth = 1.6f)
                drawCircle(ink.copy(alpha = alpha), 6f, Offset(64f, 36f))
            }
            World.ABYSS -> {
                drawLine(ink.copy(alpha = .6f * alpha), Offset(50f, 14f), Offset(50f, 84f), strokeWidth = 2f)
                for (index in 0..2) {
                    val y = 28f + index * 20f
                    val chevron = Path().apply {
                        moveTo(36f, y)
                        lineTo(50f, y + 9f)
                        lineTo(64f, y)
                    }
                    drawPath(chevron, ink.copy(alpha = (1f - index * .24f) * alpha), style = line)
                }
            }
            World.PRISM -> {
                val tri = Path().apply {
                    moveTo(34f, 24f); lineTo(34f, 70f); lineTo(64f, 47f); close()
                }
                drawPath(tri, ink, style = line)
                for (index in -1..1) {
                    drawLine(
                        ink.copy(alpha = .6f * alpha), Offset(64f, 47f),
                        Offset(90f, 47f + index * 18f), strokeWidth = 1.8f, cap = StrokeCap.Round,
                    )
                }
            }
            World.CONSTELLATION -> {
                val points = listOf(Offset(26f, 32f), Offset(50f, 22f), Offset(70f, 42f), Offset(40f, 60f), Offset(70f, 72f))
                val links = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 0 to 3)
                links.forEach { (a, b) ->
                    drawLine(ink.copy(alpha = .38f * alpha), points[a], points[b], strokeWidth = 1.5f)
                }
                points.forEachIndexed { index, point ->
                    drawCircle(ink.copy(alpha = if (index == 2) alpha else .75f * alpha), if (index == 2) 4.6f else 3f, point)
                }
            }
            World.TERRAIN -> {
                for (index in 0..1) {
                    val path = Path()
                    for (step in 0..40) {
                        val a = step / 40f * 2f * PI.toFloat()
                        val r = 22f - index * 10f + sin(a * 3f + index) * 3.4f
                        val point = Offset(50f + cos(a) * r, 50f + sin(a) * r * .82f)
                        if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    path.close()
                    drawPath(path, ink.copy(alpha = (1f - index * .35f) * alpha), style = if (index == 0) line else hair)
                }
            }
            World.PULSE -> {
                val trace = Path().apply {
                    moveTo(10f, 54f); lineTo(30f, 54f); lineTo(38f, 34f); lineTo(46f, 72f)
                    lineTo(54f, 44f); lineTo(62f, 54f); lineTo(90f, 54f)
                }
                drawPath(trace, ink, style = Stroke(3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            World.SYNAPSE -> {
                val center = Offset(50f, 50f)
                val nodes = listOf(Offset(24f, 30f), Offset(24f, 70f), Offset(76f, 30f), Offset(76f, 70f))
                nodes.forEach { node ->
                    drawLine(ink.copy(alpha = .40f * alpha), center, node, strokeWidth = 1.6f)
                    drawCircle(ink.copy(alpha = alpha), 4f, node)
                }
                drawCircle(ink.copy(alpha = alpha), 6f, center)
            }
            World.IRIS -> {
                drawCircle(ink.copy(alpha = alpha), 28f, Offset(50f, 50f), style = hair)
                drawCircle(ink.copy(alpha = alpha), 10f, Offset(50f, 50f), style = line)
                for (index in 0 until 6) {
                    val a = index / 6f * 2f * PI.toFloat()
                    drawLine(
                        ink.copy(alpha = .55f * alpha),
                        Offset(50f + cos(a) * 13f, 50f + sin(a) * 13f),
                        Offset(50f + cos(a) * 26f, 50f + sin(a) * 26f),
                        strokeWidth = 1.7f,
                    )
                }
            }
            World.SEAL -> {
                drawCircle(ink.copy(alpha = alpha), 27f, Offset(50f, 50f), style = line)
                val check = Path().apply {
                    moveTo(36f, 51f); lineTo(46f, 62f); lineTo(66f, 38f)
                }
                drawPath(check, ink, style = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            World.GATE -> {
                val arch = Path().apply {
                    moveTo(28f, 78f); lineTo(28f, 44f)
                    quadraticTo(50f, 18f, 72f, 44f); lineTo(72f, 78f)
                }
                drawPath(arch, ink, style = line)
                drawLine(ink.copy(alpha = .45f * alpha), Offset(38f, 78f), Offset(38f, 50f), strokeWidth = 1.8f)
                drawLine(ink.copy(alpha = .45f * alpha), Offset(62f, 78f), Offset(62f, 50f), strokeWidth = 1.8f)
            }
            World.COMPASS -> {
                drawCircle(ink.copy(alpha = .45f * alpha), 28f, Offset(50f, 50f), style = hair)
                val star = Path().apply {
                    moveTo(50f, 16f); lineTo(57f, 43f); lineTo(84f, 50f); lineTo(57f, 57f)
                    lineTo(50f, 84f); lineTo(43f, 57f); lineTo(16f, 50f); lineTo(43f, 43f); close()
                }
                drawPath(star, ink.copy(alpha = alpha), style = Stroke(2.2f, join = StrokeJoin.Round))
            }
        }
    }
}

/** Circular world badge: bloom + sigil. Replaces the generic service icon. */
@Composable
internal fun WorldCrest(world: World, diameter: Int = 40, alpha: Float = 1f) {
    Canvas(Modifier.size(diameter.dp)) {
        val radius = size.minDimension / 2f
        drawCircle(
            Brush.radialGradient(
                listOf(
                    world.primary.copy(alpha = .52f * alpha),
                    world.deep.copy(alpha = .48f * alpha),
                    Color.Transparent,
                ),
                Offset(radius, radius), radius,
            ),
            radius, Offset(radius, radius),
        )
        drawCircle(world.primary.copy(alpha = .55f * alpha), radius * .94f, Offset(radius, radius), style = Stroke(1f))
        drawSigil(world, Light.white.copy(alpha = alpha), alpha = alpha)
    }
}

// ---------------------------------------------------------------------------
// Full instruments — the hero of each service
// ---------------------------------------------------------------------------

/** Draws the signature instrument of a world in a 100-unit square. */
internal fun DrawScope.drawWorld(world: World, t: Float, intensity: Float = 1f) {
    unit { drawWorldUnit(world, t, intensity) }
}

/**
 * Draws a world inside a [box]-sized design space centred on the current origin.
 *
 * The hero already draws in its own design units, so it must not use [drawWorld]:
 * that helper rescales to the *canvas* bounds and the instrument would land off
 * its intended pedestal. This keeps the instrument exactly where the hero puts it.
 */
internal fun DrawScope.drawWorldCentered(world: World, t: Float, intensity: Float = 1f, box: Float = 100f) {
    if (box <= 0f) return
    translate(-box / 2f, -box / 2f) {
        scale(box / 100f, box / 100f, Offset.Zero) { drawWorldUnit(world, t, intensity) }
    }
}

private fun DrawScope.drawWorldUnit(world: World, t: Float, intensity: Float) {
    when (world) {
        World.HUB -> drawHub(t, intensity)
        World.TIDE -> drawTide(t, intensity)
        World.MIRROR -> drawMirror(t, intensity)
        World.STRATA -> drawStrata(t, intensity)
        World.RADAR -> drawRadar(t, intensity)
        World.ABYSS -> drawAbyss(t, intensity)
        World.PRISM -> drawPrism(t, intensity)
        World.CONSTELLATION -> drawConstellation(t, intensity)
        World.TERRAIN -> drawTerrain(t, intensity)
        World.PULSE -> drawPulse(t, intensity)
        World.SYNAPSE -> drawSynapse(t, intensity)
        World.IRIS -> drawIris(t)
        World.SEAL -> drawSeal(t)
        World.GATE -> drawGate(t)
        World.COMPASS -> drawCompass(t)
    }
}

private fun DrawScope.drawHub(t: Float, intensity: Float) {
    val center = Offset(50f, 50f)
    val colors = listOf(World.HUB.primary, World.HUB.secondary, World.CONSTELLATION.primary, World.TIDE.primary)
    for (ring in 1..3) {
        val radius = 16f + ring * 11f
        drawCircle(Light.ice.copy(alpha = .10f * intensity), radius, center, style = Stroke(1f))
        if (ring == 3) {
            drawArc(
                Brush.sweepGradient(listOf(Color.Transparent, World.HUB.primary.copy(alpha = .7f), Color.Transparent), center),
                0f, 120f, false, Offset(50f - radius, 50f - radius), Size(radius * 2f, radius * 2f),
                style = Stroke(1.6f, cap = StrokeCap.Round),
            )
        }
    }
    drawCircle(Brush.radialGradient(listOf(World.HUB.primary.copy(alpha = .55f * intensity), Color.Transparent), center, 22f), 22f, center)
    drawCircle(World.HUB.secondary.copy(alpha = .9f * intensity), 6.5f, center)
    drawCircle(Light.white.copy(alpha = .9f * intensity), 2.4f, center)
    for (index in 0 until 8) {
        val angle = index / 8f * 2f * PI.toFloat() + t * 2f * PI.toFloat() * .05f
        val radius = if (index % 2 == 0) 38f else 49f
        val point = Offset(50f + cos(angle) * radius, 50f + sin(angle) * radius)
        val tint = colors[index % colors.size]
        drawLine(tint.copy(alpha = .22f * intensity), center, point, strokeWidth = .8f)
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = .9f), Color.Transparent), point, 5f), 5f, point)
        drawCircle(tint.copy(alpha = intensity), 1.9f, point)
    }
}

private fun DrawScope.drawTide(t: Float, intensity: Float) {
    // The tide is light on a stage, not a body of water. A filled wave path used
    // to end in a hard rectangle at the canvas edge; a radial pool keeps the same
    // hue with no straight edge to read as a panel.
    val pool = Offset(50f, 68f)
    drawCircle(
        Brush.radialGradient(
            listOf(
                World.TIDE.primary.copy(alpha = .26f * intensity),
                World.TIDE.secondary.copy(alpha = .10f * intensity),
                Color.Transparent,
            ),
            pool,
            52f,
        ),
        52f,
        pool,
    )
    for (band in 0..4) {
        val path = Path()
        for (step in 0..60) {
            val x = step / 60f * 104f - 2f
            val y = 40f + band * 10f +
                sin(x * .075f + t * 2f * PI.toFloat() + band * .8f) * (4.4f - band * .5f) +
                sin(x * .16f + band) * 1.6f
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    World.TIDE.primary.copy(alpha = (.85f - band * .14f) * intensity),
                    World.TIDE.secondary.copy(alpha = (.65f - band * .1f) * intensity),
                    Color.Transparent,
                ),
            ),
            style = Stroke(1.5f - band * .16f, cap = StrokeCap.Round),
        )
    }
    val moon = Offset(76f, 24f)
    drawCircle(Brush.radialGradient(listOf(Light.ice.copy(alpha = .9f), World.TIDE.primary.copy(alpha = .5f), Color.Transparent), moon, 12f), 12f, moon)
    drawCircle(Light.ice.copy(alpha = .85f * intensity), 5.2f, moon)
    for (index in 0 until 7) {
        val x = 16f + index * 11f
        val y = 20f + (index % 3) * 7f + sin(t * 2f * PI.toFloat() + index) * 2.4f
        drawCircle(Light.ice.copy(alpha = (.30f + (index % 3) * .18f) * intensity), 1.3f, Offset(x, y))
    }
}

private fun DrawScope.drawMirror(t: Float, intensity: Float) {
    val left = Offset(33f, 50f)
    val right = Offset(67f, 50f)
    drawCircle(Brush.radialGradient(listOf(World.MIRROR.primary.copy(alpha = .50f * intensity), Color.Transparent), left, 24f), 24f, left)
    drawCircle(Brush.radialGradient(listOf(World.MIRROR.secondary.copy(alpha = .40f * intensity), Color.Transparent), right, 24f), 24f, right)
    drawLine(
        Light.ice.copy(alpha = .35f * intensity), Offset(46f, 50f), Offset(54f, 50f),
        strokeWidth = 1.2f, cap = StrokeCap.Round,
    )
    val diamond = Path().apply {
        moveTo(50f, 46f); lineTo(53f, 50f); lineTo(50f, 54f); lineTo(47f, 50f); close()
    }
    drawPath(diamond, Light.ice.copy(alpha = .8f * intensity))
    drawCircle(
        Brush.linearGradient(listOf(World.MIRROR.primary, World.MIRROR.secondary), left + Offset(-14f, -14f), left + Offset(14f, 14f)),
        15f, left,
    )
    drawCircle(Light.white.copy(alpha = .35f * intensity), 4.4f, left + Offset(-4f, -5f))
    drawCircle(
        World.MIRROR.secondary.copy(alpha = .55f * intensity), 15f, right,
        style = Stroke(1.4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.4f, 3.2f))),
    )
    drawCircle(World.MIRROR.secondary.copy(alpha = .22f * intensity), 15f, right)
    val tetherPhase = t * 2f * PI.toFloat()
    val spark = left + (right - left) * (.5f + .42f * sin(tetherPhase))
    drawCircle(Brush.radialGradient(listOf(Light.white, World.MIRROR.primary.copy(alpha = .6f), Color.Transparent), spark, 7f), 7f, spark)
    drawOval(
        Brush.radialGradient(listOf(World.MIRROR.primary.copy(alpha = .22f * intensity), Color.Transparent), Offset(50f, 74f), 34f),
        Offset(18f, 68f), Size(64f, 14f),
    )
}

private fun DrawScope.drawStrata(t: Float, intensity: Float) {
    val tints = listOf(World.STRATA.primary, World.STRATA.secondary, lerp(World.STRATA.secondary, World.STRATA.deep, .5f), World.STRATA.deep)
    for (layer in 0..3) {
        val top = 34f + layer * 12f
        val band = Path().apply {
            moveTo(6f, top)
            for (step in 0..40) {
                val x = 6f + step / 40f * 88f
                lineTo(x, top + sin(x * .1f + layer) * 2f)
            }
            lineTo(94f, top + 13f)
            lineTo(6f, top + 13f)
            close()
        }
        drawPath(
            band,
            Brush.horizontalGradient(listOf(tints[layer].copy(alpha = .42f * intensity), tints[layer].copy(alpha = .12f * intensity))),
        )
        drawLine(
            tints[layer].copy(alpha = .55f * intensity), Offset(6f, top), Offset(94f, top),
            strokeWidth = 1f, cap = StrokeCap.Round,
        )
    }
    for (root in 0..2) {
        val x = 34f + root * 16f
        val path = Path().apply {
            moveTo(x, 52f)
            cubicTo(x - 4f, 62f, x + 6f, 72f, x - 2f, 88f)
        }
        drawPath(
            path, World.STRATA.primary.copy(alpha = (.7f - root * .16f) * intensity),
            style = Stroke(1.6f - root * .3f, cap = StrokeCap.Round),
        )
    }
    val seed = Offset(64f, 59f + sin(t * 2f * PI.toFloat()) * 1.2f)
    drawCircle(Brush.radialGradient(listOf(Light.white, World.STRATA.primary.copy(alpha = .8f), Color.Transparent), seed, 8f), 8f, seed)
    drawCircle(Light.ice.copy(alpha = intensity), 2.2f, seed)
}

private fun DrawScope.drawRadar(t: Float, intensity: Float) {
    val center = Offset(50f, 50f)
    for (radius in listOf(14f, 26f, 38f, 47f)) {
        drawCircle(World.RADAR.primary.copy(alpha = .22f * intensity), radius, center, style = Stroke(.9f))
    }
    drawLine(World.RADAR.primary.copy(alpha = .18f * intensity), Offset(50f, 2f), Offset(50f, 98f), strokeWidth = .8f)
    drawLine(World.RADAR.primary.copy(alpha = .18f * intensity), Offset(2f, 50f), Offset(98f, 50f), strokeWidth = .8f)
    val sweep = t * 360f
    drawArc(
        Brush.sweepGradient(
            listOf(Color.Transparent, World.RADAR.primary.copy(alpha = .05f), World.RADAR.primary.copy(alpha = .38f * intensity), World.RADAR.primary.copy(alpha = .7f)),
            center,
        ),
        sweep - 46f, 46f, true, Offset(3f, 3f), Size(94f, 94f),
    )
    drawArc(
        World.RADAR.primary.copy(alpha = .55f * intensity),
        sweep - 46f, 46f, false, Offset(3f, 3f), Size(94f, 94f),
        style = Stroke(1.2f, cap = StrokeCap.Round),
    )
    for (index in 0 until 12) {
        val angle = index / 12f * 2f * PI.toFloat()
        val point = Offset(50f + cos(angle) * 38f, 50f + sin(angle) * 38f)
        val measured = index % 3 != 0
        if (measured) {
            drawCircle(Brush.radialGradient(listOf(World.RADAR.secondary.copy(alpha = .9f), Color.Transparent), point, 6f), 6f, point)
            drawCircle(World.RADAR.secondary.copy(alpha = intensity), 2.4f, point)
        } else {
            drawCircle(
                World.RADAR.primary.copy(alpha = .45f * intensity), 2.4f, point,
                style = Stroke(.9f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.2f, 1.6f))),
            )
        }
    }
    drawCircle(Light.ice.copy(alpha = intensity), 1.8f, center)
}

private fun DrawScope.drawAbyss(t: Float, intensity: Float) {
    val core = Offset(50f, 84f)
    drawCircle(Brush.radialGradient(listOf(World.ABYSS.primary.copy(alpha = .55f * intensity), Color.Transparent), core, 26f), 26f, core)
    drawLine(World.ABYSS.primary.copy(alpha = .25f * intensity), Offset(38f, 8f), Offset(38f, 92f), strokeWidth = .9f)
    drawLine(World.ABYSS.primary.copy(alpha = .25f * intensity), Offset(62f, 8f), Offset(62f, 92f), strokeWidth = .9f)
    for (tick in 0..8) {
        val y = 12f + tick * 9f
        drawLine(World.ABYSS.primary.copy(alpha = .18f * intensity), Offset(44f, y), Offset(56f, y), strokeWidth = .8f)
    }
    drawArc(
        World.ABYSS.primary.copy(alpha = .65f * intensity),
        200f, 140f, false, Offset(30f, 2f), Size(40f, 40f),
        style = Stroke(1.4f, cap = StrokeCap.Round),
    )
    val pulse = .5f + .5f * sin(t * 2f * PI.toFloat())
    for (echo in 0..2) {
        val y = 34f + echo * 17f
        val spread = 10f + echo * 6f
        drawOval(
            World.ABYSS.secondary.copy(alpha = (.5f - echo * .13f) * intensity * (.6f + .4f * pulse)),
            Offset(50f - spread, y - 3.4f), Size(spread * 2f, 6.8f),
            style = Stroke(1.1f),
        )
    }
    val beam = Path().apply {
        moveTo(44f, 14f); lineTo(56f, 14f); lineTo(52f, 80f); lineTo(48f, 80f); close()
    }
    drawPath(beam, Brush.verticalGradient(listOf(World.ABYSS.primary.copy(alpha = .34f * intensity), Color.Transparent)))
    drawCircle(World.ABYSS.primary.copy(alpha = .9f * intensity), 9f, core)
    drawCircle(World.ABYSS.primary.copy(alpha = .35f * intensity), 9f, core, style = Stroke(4f))
    drawCircle(Light.ice.copy(alpha = intensity), 2.6f, core)
}

private fun DrawScope.drawPrism(t: Float, intensity: Float) {
    val apex = Offset(46f, 50f)
    val beam = Path().apply {
        moveTo(2f, 46f); lineTo(2f, 54f); lineTo(34f, 54f); lineTo(34f, 46f); close()
    }
    drawPath(beam, Brush.horizontalGradient(listOf(Light.ice.copy(alpha = .05f), Light.ice.copy(alpha = .8f * intensity))))
    val triangle = Path().apply {
        moveTo(34f, 34f); lineTo(34f, 66f); lineTo(58f, 50f); close()
    }
    drawPath(triangle, Light.ice.copy(alpha = .10f * intensity))
    drawPath(triangle, Light.ice.copy(alpha = .85f * intensity), style = Stroke(1.3f, join = StrokeJoin.Round))
    val spectrum = listOf(
        Color(0xFFFF5C8A), Color(0xFFFFB35C), Color(0xFFFFF07A),
        Color(0xFF5CFFB0), Color(0xFF5CD8FF), Color(0xFF9C7CFF),
    )
    spectrum.forEachIndexed { index, tint ->
        val endY = 16f + index * 13f
        val ray = Path().apply {
            moveTo(58f, 50f)
            quadraticTo(74f, 50f, 96f, endY)
        }
        drawPath(ray, tint.copy(alpha = .16f * intensity), style = Stroke(5f, cap = StrokeCap.Round))
        drawPath(ray, tint.copy(alpha = .85f * intensity), style = Stroke(1.2f, cap = StrokeCap.Round))
    }
    drawCircle(Brush.radialGradient(listOf(Light.white, Color.Transparent), apex, 12f), 12f, apex)
    drawCircle(Light.white.copy(alpha = intensity), 1.8f, apex)
}

private fun DrawScope.drawConstellation(t: Float, intensity: Float) {
    val stars = listOf(
        Triple(24f, 30f, 2.6f), Triple(38f, 18f, 1.8f), Triple(56f, 26f, 3.4f),
        Triple(72f, 16f, 1.6f), Triple(82f, 36f, 2.2f), Triple(66f, 48f, 2.0f),
        Triple(46f, 42f, 3.0f), Triple(30f, 56f, 1.7f), Triple(52f, 66f, 2.6f),
        Triple(74f, 64f, 1.9f), Triple(86f, 78f, 1.5f), Triple(20f, 78f, 2.2f),
    )
    val links = listOf(0 to 2, 1 to 2, 2 to 3, 2 to 5, 2 to 6, 6 to 7, 6 to 8, 8 to 9, 9 to 10, 7 to 11, 5 to 9)
    links.forEach { (a, b) ->
        drawLine(
            World.CONSTELLATION.secondary.copy(alpha = .20f * intensity),
            Offset(stars[a].first, stars[a].second), Offset(stars[b].first, stars[b].second),
            strokeWidth = .9f,
        )
    }
    val twinkle = .65f + .35f * sin(t * 2f * PI.toFloat())
    stars.forEachIndexed { index, (x, y, radius) ->
        val tint = if (index % 3 == 0) World.CONSTELLATION.secondary else World.CONSTELLATION.primary
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = .75f * intensity), Color.Transparent), Offset(x, y), radius * 3.4f), radius * 3.4f, Offset(x, y))
        drawCircle(Light.white.copy(alpha = (if (index == 2) twinkle else .85f) * intensity), radius, Offset(x, y))
    }
    val hero = Offset(56f, 26f)
    drawLine(Light.white.copy(alpha = .45f * intensity), hero + Offset(-9f, 0f), hero + Offset(9f, 0f), strokeWidth = .8f)
    drawLine(Light.white.copy(alpha = .45f * intensity), hero + Offset(0f, -9f), hero + Offset(0f, 9f), strokeWidth = .8f)
}

private fun DrawScope.drawTerrain(t: Float, intensity: Float) {
    val center = Offset(50f, 52f)
    for (ring in 0..5) {
        val base = 8f + ring * 8f
        val path = Path()
        for (step in 0..48) {
            val angle = step / 48f * 2f * PI.toFloat()
            val wobble = sin(angle * 3f + ring * .9f) * (2.6f - ring * .22f) + sin(angle * 5f + ring) * 1.2f
            val radius = base + wobble
            val point = Offset(50f + cos(angle) * radius, 52f + sin(angle) * radius * .82f)
            if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(
            path,
            World.TERRAIN.primary.copy(alpha = (.58f - ring * .08f) * intensity),
            style = Stroke(if (ring < 2) 1.5f else 1f),
        )
    }
    drawPath(
        Path().apply {
            moveTo(50f, 44f); lineTo(50f, 60f)
        },
        World.TERRAIN.secondary.copy(alpha = .65f * intensity),
        style = Stroke(1.2f, cap = StrokeCap.Round),
    )
    val pin = Offset(50f + sin(t * 2f * PI.toFloat()) * 1.4f, 42f)
    drawCircle(Brush.radialGradient(listOf(Light.white, World.TERRAIN.primary.copy(alpha = .7f), Color.Transparent), pin, 9f), 9f, pin)
    drawCircle(Light.ice.copy(alpha = intensity), 2f, pin)
}

private fun DrawScope.drawPulse(t: Float, intensity: Float) {
    for (line in 0..4) {
        val y = 26f + line * 12f
        drawLine(Light.ice.copy(alpha = .08f * intensity), Offset(6f, y), Offset(94f, y), strokeWidth = .7f)
    }
    val trace = Path().apply {
        moveTo(4f, 52f)
        lineTo(22f, 52f)
        quadraticTo(27f, 52f, 30f, 46f)
        lineTo(35f, 30f)
        lineTo(41f, 72f)
        lineTo(46f, 44f)
        quadraticTo(49f, 38f, 54f, 44f)
        lineTo(59f, 52f)
        lineTo(72f, 52f)
        quadraticTo(76f, 52f, 79f, 48f)
        quadraticTo(82f, 44f, 85f, 52f)
        lineTo(96f, 52f)
    }
    drawPath(trace, World.PULSE.primary.copy(alpha = .16f * intensity), style = Stroke(7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(
        trace,
        Brush.horizontalGradient(listOf(World.PULSE.primary, lerp(World.PULSE.primary, World.PULSE.secondary, .6f), World.PULSE.secondary)),
        style = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    val measure = PathMeasure().apply { setPath(trace, false) }
    val distance = measure.length * (t % 1f)
    val head = measure.getPosition(distance)
    drawCircle(Brush.radialGradient(listOf(Light.white, World.PULSE.secondary.copy(alpha = .8f), Color.Transparent), head, 9f), 9f, head)
    drawCircle(Light.white.copy(alpha = intensity), 2.2f, head)
}

private fun DrawScope.drawSynapse(t: Float, intensity: Float) {
    val layers = listOf(
        listOf(Offset(20f, 28f), Offset(20f, 50f), Offset(20f, 72f)),
        listOf(Offset(50f, 20f), Offset(50f, 40f), Offset(50f, 60f), Offset(50f, 80f)),
        listOf(Offset(80f, 32f), Offset(80f, 52f), Offset(80f, 70f)),
    )
    for (left in layers[0]) for (middle in layers[1]) {
        drawLine(World.SYNAPSE.primary.copy(alpha = .16f * intensity), left, middle, strokeWidth = .8f)
    }
    for (middle in layers[1]) for (right in layers[2]) {
        drawLine(World.SYNAPSE.secondary.copy(alpha = .16f * intensity), middle, right, strokeWidth = .8f)
    }
    val lit = (t * 100f).toInt() % 8
    var edge = 0
    for (middle in layers[1]) for (right in layers[2]) {
        if (edge++ == lit) {
            drawLine(
                World.SYNAPSE.secondary.copy(alpha = .9f * intensity), middle, right, strokeWidth = 1.8f,
                cap = StrokeCap.Round,
            )
            val point = middle + (right - middle) * ((t * 3f) % 1f)
            drawCircle(Brush.radialGradient(listOf(Light.white, World.SYNAPSE.secondary.copy(alpha = .8f), Color.Transparent), point, 8f), 8f, point)
        }
    }
    layers.forEachIndexed { layerIndex, nodes ->
        nodes.forEach { node ->
            val tint = if (layerIndex == 1) World.SYNAPSE.secondary else World.SYNAPSE.primary
            drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = .7f * intensity), Color.Transparent), node, 8f), 8f, node)
            drawCircle(Light.white.copy(alpha = .9f * intensity), 2.4f, node)
        }
    }
}

private fun DrawScope.drawIris(t: Float, intensity: Float = 1f) {
    val center = Offset(50f, 50f)
    drawCircle(World.IRIS.primary.copy(alpha = .18f * intensity), 46f, center, style = Stroke(1f))
    for (blade in 0 until 10) {
        val angle = blade / 10f * 2f * PI.toFloat() + t * .35f
        val inner = 16f
        val outer = 41f
        val halfWidth = .16f
        val path = Path().apply {
            val a0 = angle - halfWidth
            val a1 = angle + halfWidth
            moveTo(center.x + cos(angle) * inner, center.y + sin(angle) * inner)
            lineTo(center.x + cos(a0) * outer, center.y + sin(a0) * outer)
            lineTo(center.x + cos(a1) * outer, center.y + sin(a1) * outer)
            close()
        }
        drawPath(path, World.IRIS.primary.copy(alpha = (.30f + (blade % 3) * .10f) * intensity))
        drawPath(path, World.IRIS.secondary.copy(alpha = .55f * intensity), style = Stroke(.8f, join = StrokeJoin.Round))
    }
    drawCircle(Brush.radialGradient(listOf(World.IRIS.primary.copy(alpha = .55f * intensity), Color.Transparent), center, 20f), 20f, center)
    drawCircle(Brand.ink.copy(alpha = .92f), 12f, center)
    drawCircle(World.IRIS.primary.copy(alpha = .9f * intensity), 12f, center, style = Stroke(1.4f))
    drawCircle(Light.white.copy(alpha = .85f * intensity), 2.2f, center + Offset(-4f, -5f))
}

private fun DrawScope.drawSeal(t: Float, intensity: Float = 1f) {
    val count = 5
    val points = (0 until count).map { index ->
        val u = index / (count - 1f)
        Offset(18f + u * 64f, 66f - sin(u * PI.toFloat()) * 30f)
    }
    for (index in 0 until count - 1) {
        drawLine(World.SEAL.primary.copy(alpha = .30f * intensity), points[index], points[index + 1], strokeWidth = 1f)
    }
    val verified = 2
    points.forEachIndexed { index, point ->
        val solid = index <= verified
        if (index == verified) {
            val ring = 14f + sin(t * 2f * PI.toFloat()) * 1.6f
            drawCircle(Brush.radialGradient(listOf(World.SEAL.secondary.copy(alpha = .45f * intensity), Color.Transparent), point, ring), ring, point)
        }
        drawCircle(
            if (solid) World.SEAL.primary else Color.Transparent,
            9f, point,
            style = if (solid) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2.6f))),
        )
        if (solid) {
            drawCircle(Color.Transparent, 9f, point, style = Stroke(1f))
            val check = Path().apply {
                moveTo(point.x - 4f, point.y); lineTo(point.x - .8f, point.y + 3.4f); lineTo(point.x + 4.4f, point.y - 3.2f)
            }
            drawPath(check, Brand.ink.copy(alpha = .9f), style = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawCircle(World.SEAL.primary.copy(alpha = .35f * intensity), 9f, point, style = Stroke(1.2f))
        }
    }
}

private fun DrawScope.drawGate(t: Float, intensity: Float = 1f) {
    for (index in 0..4) {
        val inset = 16f - index * 2f
        val width = 68f - inset
        val left = 50f - width / 2f
        val right = 50f + width / 2f
        val top = 22f + index * 5f
        val arch = Path().apply {
            moveTo(left, 82f)
            lineTo(left, top + 22f)
            quadraticTo(50f, top - 6f, right, top + 22f)
            lineTo(right, 82f)
        }
        val alpha = (.55f - index * .08f) * intensity
        drawPath(arch, World.GATE.primary.copy(alpha = alpha), style = Stroke(if (index == 0) 1.6f else 1f))
        if (index == 4) {
            drawPath(arch, Brush.verticalGradient(listOf(World.GATE.primary.copy(alpha = .30f), Color.Transparent)))
        }
    }
    drawLine(World.GATE.primary.copy(alpha = .45f * intensity), Offset(10f, 82f), Offset(90f, 82f), strokeWidth = 1.2f)
    val glow = .6f + .4f * sin(t * 2f * PI.toFloat())
    drawCircle(Brush.radialGradient(listOf(World.GATE.primary.copy(alpha = .35f * glow), Color.Transparent), Offset(50f, 44f), 26f), 26f, Offset(50f, 44f))
}

private fun DrawScope.drawCompass(t: Float, intensity: Float = 1f) {
    val center = Offset(50f, 50f)
    drawCircle(World.COMPASS.primary.copy(alpha = .22f * intensity), 42f, center, style = Stroke(1f))
    drawCircle(World.COMPASS.primary.copy(alpha = .14f * intensity), 32f, center, style = Stroke(.8f))
    for (index in 0 until 8) {
        val angle = index / 8f * 2f * PI.toFloat()
        val inner = if (index % 2 == 0) 30f else 36f
        drawLine(
            World.COMPASS.primary.copy(alpha = .35f * intensity),
            Offset(50f + cos(angle) * inner, 50f + sin(angle) * inner),
            Offset(50f + cos(angle) * 41f, 50f + sin(angle) * 41f),
            strokeWidth = 1f,
        )
    }
    val needle = t * 2f * PI.toFloat() * .08f
    val north = Path().apply {
        moveTo(50f, 12f); lineTo(58f, 50f); lineTo(50f, 88f); lineTo(42f, 50f); close()
    }
    val rotated = Path()
    val nodes = listOf(Offset(50f, 12f), Offset(58f, 50f), Offset(50f, 88f), Offset(42f, 50f))
    nodes.forEachIndexed { index, point ->
        val dx = point.x - 50f
        val dy = point.y - 50f
        val x = 50f + dx * cos(needle) - dy * sin(needle)
        val y = 50f + dx * sin(needle) + dy * cos(needle)
        if (index == 0) rotated.moveTo(x, y) else rotated.lineTo(x, y)
    }
    rotated.close()
    drawPath(rotated, Brush.linearGradient(listOf(World.COMPASS.secondary, World.COMPASS.primary), Offset(42f, 12f), Offset(58f, 88f)))
    drawPath(rotated, Light.white.copy(alpha = .55f * intensity), style = Stroke(1f, join = StrokeJoin.Round))
    drawPath(north, Color.Transparent, style = Stroke(1f))
    drawCircle(Brush.radialGradient(listOf(Light.white, World.COMPASS.secondary.copy(alpha = .8f), Color.Transparent), center, 9f), 9f, center)
    drawCircle(Brand.ink.copy(alpha = .9f), 2.4f, center)
}

/**
 * The weather of a service that has nothing measured yet.
 *
 * It is atmosphere only: soft bands of the world's hue, a horizon hairline and
 * no number, no bar and no claim. Unmeasured scope is drawn as fog because fog
 * is exactly what "we have not read this yet" looks like.
 */
@Composable
internal fun WorldFog(world: World, modifier: Modifier = Modifier, alpha: Float = .34f) {
    Canvas(modifier) {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, world.deep.copy(alpha = alpha * .85f), Color.Transparent),
                0f, h,
            ),
        )
        for (band in 0 until 6) {
            val y = h * (.18f + band * .14f)
            val path = Path()
            for (step in 0..30) {
                val x = step / 30f * w
                val drift = sin(x / w * 6.2f + band * .9f) * (7f + band)
                if (step == 0) path.moveTo(x, y + drift) else path.lineTo(x, y + drift)
            }
            drawPath(
                path,
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        world.primary.copy(alpha = alpha * (.62f - band * .07f)),
                        world.secondary.copy(alpha = alpha * (.34f - band * .04f)),
                        Color.Transparent,
                    )
                ),
                style = Stroke(1.6f),
            )
        }
        // The horizon: where the measured part of the screen would begin.
        drawLine(
            Brush.horizontalGradient(
                listOf(Color.Transparent, world.primary.copy(alpha = alpha * 1.9f), Color.Transparent),
            ),
            Offset(0f, h * .58f), Offset(w, h * .58f), strokeWidth = 1f,
        )
    }
}

/**
 * The animated hero instrument of a service. Motion is the only thing that
 * changes; with reduced motion the same frame is drawn at rest, so shape and
 * colour still carry the identity.
 */@Composable
internal fun WorldInstrument(
    world: World,
    modifier: Modifier = Modifier,
    motion: Boolean = LocalPreviewMotion.current,
    intensity: Float = 1f,
) {
    val transition = rememberInfiniteTransition(label = "world-${world.name}")
    val phase = if (motion) {
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(9_000, easing = LinearEasing)),
            label = "phase",
        ).value
    } else 0f
    Canvas(modifier) { drawWorld(world, phase, intensity) }
}
