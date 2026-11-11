package com.knoux.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Premium rendering primitives for the KNOUX AI surface.
 *
 * Everything here is native vector drawing (Canvas paths, gradients, particles);
 * no bitmap is used as a screen background. The primitives are shared by the
 * heroes, the icon system and the glass material so every screen keeps one
 * coherent light language instead of twenty bespoke effects.
 */

// ---------------------------------------------------------------------------
// Light language helpers
// ---------------------------------------------------------------------------

internal object Light {
    val deep = Color(0xff04020e)
    val midnight = Color(0xff080422)
    val indigo = Color(0xff1d0f5c)
    val violet = Color(0xff9b3dff)
    val magenta = Color(0xfff43dff)
    val pink = Color(0xffff7bff)
    val cyan = Color(0xff00e8ff)
    val azure = Color(0xff24bfff)
    val ice = Color(0xffe4faff)
    val white = Color(0xfffffdff)
}

/** Directional film lighting used by every hero and material in the app. */
internal val keyLight = Offset(-0.52f, -0.62f)
internal val fillLight = Offset(0.66f, 0.52f)

internal fun lerpColor(a: Color, b: Color, t: Float) = lerp(a, b, t.coerceIn(0f, 1f))

/** Soft volumetric bloom that keeps deep shadows in the composition. */
internal fun DrawScope.bloom(center: Offset, radius: Float, tint: Color, strength: Float = .5f) {
    drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = .56f * strength), tint.copy(alpha = .20f * strength), Color.Transparent),
        center, radius), radius, center)
}

// ---------------------------------------------------------------------------
// Background: layered silk light ribbons
// ---------------------------------------------------------------------------

/**
 * Sweeping light ribbons behind the hero. Each ribbon is a closed, tapered band
 * with a bright upper edge so it reads as luminous silk rather than a flat line.
 */
internal fun DrawScope.ribbonField(width: Float, height: Float, phase: Float, intensity: Float = 1f) {
    val bands = listOf(
        Triple(0.06f, 0.52f, 0.20f),
        Triple(0.34f, 0.44f, 0.15f),
        Triple(0.66f, 0.50f, 0.13f)
    )
    bands.forEachIndexed { index, (baseY, amp, thick) ->
        val drift = sin(phase * .12f + index * 1.7f) * height * .012f
        val steps = 54
        fun edge(i: Int, side: Float): Offset {
            val u = i / steps.toFloat()
            val wave = sin(u * PI.toFloat() * 2.1f + index * .9f + phase * .05f) * height * amp * .33f
            val fall = sin(u * PI.toFloat()).coerceAtLeast(0f).pow(1.25f)
            val y = height * baseY + wave + drift - fall * height * .045f
            val t = height * thick * (.35f + .65f * fall)
            return Offset(width * u, y + side * t / 2)
        }
        val top = Path().apply {
            val start = edge(0, -1f)
            moveTo(start.x, start.y)
            for (i in 1..steps) edge(i, -1f).let { lineTo(it.x, it.y) }
        }
        val band = Path().apply {
            val start = edge(0, -1f)
            moveTo(start.x, start.y)
            for (i in 1..steps) edge(i, -1f).let { lineTo(it.x, it.y) }
            for (i in steps downTo 0) edge(i, 1f).let { lineTo(it.x, it.y) }
            close()
        }
        val left = if (index == 0) Light.magenta else Light.violet
        val right = if (index == 2) Light.cyan else Light.azure
        val brush = Brush.horizontalGradient(
            listOf(left.copy(alpha = .34f * intensity), left.copy(alpha = .20f * intensity),
                Color.Transparent, right.copy(alpha = .27f * intensity), right.copy(alpha = .36f * intensity)),
            startX = 0f, endX = width
        )
        drawPath(band, brush)
        // Silk highlight along the upper edge of the ribbon.
        val sheen = Brush.horizontalGradient(.00f to left.copy(alpha = .38f * intensity),
            .19f to left.copy(alpha = 1f * intensity), .49f to Color.Transparent,
            .80f to right.copy(alpha = 1f * intensity), 1f to right.copy(alpha = .56f * intensity), endX = width)
        // Broad atmospheric spill first, then the concentrated silk edge.
        drawPath(top, sheen, alpha = .14f, style = Stroke(height * .13f, cap = StrokeCap.Round))
        drawPath(top, sheen, alpha = .30f, style = Stroke(height * .062f, cap = StrokeCap.Round))
        drawPath(top, sheen, alpha = .60f, style = Stroke(height * .024f, cap = StrokeCap.Round))
        drawPath(top, sheen, style = Stroke(height * .006f, cap = StrokeCap.Round))
    }
    // Fine depth particles that sit behind and around the ribbons.
    var seed = 17
    for (i in 0..96) {
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val u = ((seed % 1000) / 1000f)
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val v = ((seed % 1000) / 1000f)
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val r = (.35f + (seed % 100) / 380f)
        val tint = if (u < .5f) Light.magenta else Light.cyan
        val y = height * (.22f + v * .60f) + sin(u * 7f) * height * .02f
        drawCircle(tint.copy(alpha = (.20f + v * .54f) * intensity), height * r * .018f, Offset(width * u, y))
    }
}

/**
 * Deep, controlled ambience: pools of light over the existing dark background.
 * No opaque fill is drawn here - the screen keeps its own base so the hero
 * never shows a hard rectangular edge.
 */
internal fun DrawScope.atmosphere(width: Float, height: Float, tint: Color = Light.violet) {
    bloom(Offset(width * .5f, height * .40f), height * .82f, tint, .74f)
    bloom(Offset(width * .02f, height * .96f), height * .60f, Light.magenta, .34f)
    bloom(Offset(width * .98f, height * .10f), height * .52f, Light.cyan, .32f)
    bloom(Offset(width * .56f, height * .02f), height * .46f, Light.violet, .26f)
}

// ---------------------------------------------------------------------------
// Hero: particle globe, arcs and pedestal
// ---------------------------------------------------------------------------

internal class GlobePoint(val x: Float, val y: Float, val z: Float, val color: Color, val alpha: Float, val size: Float)

/**
 * A volumetric globe built from lit particles. The cloud is sampled once with a
 * Fibonacci lattice and every colour is resolved up front, so drawing it is a
 * plain loop of small circles with no per-frame allocation.
 */
@Composable
internal fun rememberGlobePoints(count: Int = 760): List<GlobePoint> = remember(count) {
    val points = ArrayList<GlobePoint>(count)
    val golden = PI * (3.0 - sqrt(5.0))
    for (i in 0 until count) {
        val y = 1.0 - (i.toDouble() / (count - 1)) * 2.0
        val ring = sqrt(1.0 - y * y)
        val theta = golden * i
        val px = (cos(theta) * ring).toFloat()
        val py = y.toFloat()
        val pz = (sin(theta) * ring).toFloat()
        // Diffuse term from the key light plus a dim rim term.
        val diffuse = (px * keyLight.x + py * keyLight.y + pz * 0.55f)
        val land = (noise3(px, py, pz) + 1.5f) / 3f
        val lit = (diffuse + 1f) / 2f
        val depth = (pz + 1f) / 2f
        val alpha = ((.20f + .72f * lit.pow(1.4f)) * (.50f + .80f * land) * (.45f + .65f * depth)).coerceIn(0f, .92f)
        val tint = when {
            lit > .86f -> Light.ice
            lit > .60f -> Light.cyan
            lit > .34f -> Light.azure
            else -> Light.violet
        }
        points.add(GlobePoint(px, py, pz, tint, alpha,
            (.55f + .60f * lit) * (.60f + .80f * land) * (.40f + .70f * depth)))
    }
    points
}

private fun noise3(x: Float, y: Float, z: Float): Float {
    var v = sin(x * 3.1f + 1.3f) * cos(y * 2.7f - .7f) * sin(z * 3.9f + 2.1f)
    v += .5f * sin(x * 7.3f - .4f) * cos(y * 6.1f + 1.1f) * sin(z * 5.7f)
    v += .3f * cos(x * 12.4f) * sin(y * 11.7f + .9f)
    return v
}

/** Draws the lit particle sphere; call inside a rotating layer. */
internal fun DrawScope.drawGlobe(radius: Float, points: List<GlobePoint>) {
    // Dark core so the particle shell reads as a lit sphere, not a milky ball.
    drawCircle(Brush.radialGradient(listOf(Color(0xff0a0620).copy(alpha = .78f), Color(0xff140b3c).copy(alpha = .55f), Color.Transparent),
        Offset.Zero, radius * 1.02f), radius * 1.02f, Offset.Zero)
    // Soft inner atmosphere behind the particle shell (kept faint so it never
    // reads as a second solid ball inside the globe).
    drawCircle(Brush.radialGradient(listOf(Color(0xff3a1f8c).copy(alpha = .16f), Color.Transparent),
        Offset.Zero, radius * 1.05f), radius * 1.05f, Offset.Zero)
    for (p in points) {
        if (p.z < -.35f) continue
        val depth = (p.z + 1f) / 2f
        drawCircle(p.color.copy(alpha = (p.alpha * 1.08f).coerceAtMost(1f)), radius * .0205f * p.size * (.60f + .45f * depth),
            Offset(p.x * radius, p.y * radius))
    }
    // Volumetric specular highlight on the upper-left shoulder.
    drawCircle(Brush.radialGradient(listOf(Color(0xffeaf7ff).copy(alpha = .36f), Color(0xff8fd4ff).copy(alpha = .14f), Color.Transparent),
        Offset(-radius * .40f, -radius * .46f), radius * .54f), radius * .54f, Offset(-radius * .40f, -radius * .46f))
    // Cyan bounce on the lower right: it keeps the shadow side readable and
    // draws the silhouette instead of leaving a dark void.
    drawCircle(Brush.radialGradient(listOf(Light.cyan.copy(alpha = .32f), Light.azure.copy(alpha = .13f), Color.Transparent),
        Offset(radius * .30f, radius * .38f), radius * .54f), radius * .54f, Offset(radius * .30f, radius * .38f))
    val rim = Brush.sweepGradient(listOf(Light.cyan, Light.ice, Light.violet, Light.magenta, Light.ice, Light.cyan), Offset.Zero)
    drawCircle(rim, radius, Offset.Zero, alpha = .32f, style = Stroke(radius * .24f))
    drawCircle(rim, radius, Offset.Zero, alpha = .88f, style = Stroke(radius * .10f))
    drawCircle(rim, radius, Offset.Zero, style = Stroke(radius * .028f))
    for (strand in 0..1) {
        val thread = Path().apply {
            for (sample in 0..80) {
                val angle = sample * 2f * PI.toFloat() / 80f
                val band = radius * (.92f - strand * .026f + .075f * sin(angle * 4f + strand * 1.7f))
                val x = band * cos(angle)
                val y = band * sin(angle)
                if (sample == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(thread, rim, alpha = .36f, style = Stroke(radius * .044f))
        drawPath(thread, rim, alpha = .92f, style = Stroke(radius * .012f))
    }
}

/** Segmented orbital rings with fine tick marks, like the reference lock-up. */
internal fun DrawScope.heroArcs(radius: Float, phase: Float) {
    val segments = listOf(
        Triple(-96f, 62f, Light.white),
        Triple(120f, 44f, Light.magenta),
        Triple(28f, 34f, Light.cyan),
        Triple(196f, 30f, Light.violet)
    )
    val box = Size(radius * 2, radius * 2)
    val topLeft = Offset(-radius, -radius)
    segments.forEach { (start, extent, tint) ->
        drawArc(tint.copy(alpha = .28f), start, extent, false, topLeft, box, style = Stroke(radius * .19f))
        drawArc(Brush.sweepGradient(listOf(tint, tint.copy(alpha = .96f), tint.copy(alpha = .55f)), Offset.Zero),
            start, extent, false, topLeft, box, style = Stroke(radius * .065f))
        drawArc(Color.White.copy(alpha = .92f), start, extent, false, topLeft, box,
            style = Stroke(radius * .016f, cap = StrokeCap.Round))
    }
    // Fine measurement ticks drawn as explicit segments: dash path effects are
    // far too expensive to rasterise every frame on a software GPU.
    val outer = radius * 1.18f
    for (i in 0..59) {
        val angle = (i * 6f + phase * .05f) * PI.toFloat() / 180f
        val v = Offset(cos(angle), sin(angle))
        val inner = outer - radius * (.055f + (i % 5) * .012f)
        drawLine(Color(0xffc9d6ff).copy(alpha = if (i % 5 == 0) .78f else .42f), v * inner, v * outer, radius * .014f)
    }
}

/** Glowing elliptical pedestal that anchors a hero to the scene. */
internal fun DrawScope.lightPedestal(radius: Float, y: Float, tone: Color = Light.magenta) {
    val w = radius * 2.15f
    val h = radius * .46f
    // The pool itself: a bright core that falls off into the shadow.
    drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = .56f), tone.copy(alpha = .56f), Color.Transparent),
        Offset(0f, y), w * .52f), Offset(-w / 2f, y - h / 2f), Size(w, h))
    drawOval(Brush.horizontalGradient(listOf(Light.violet.copy(alpha = .80f), tone.copy(alpha = .96f),
        Light.cyan.copy(alpha = .86f), Light.violet.copy(alpha = .52f)), -w / 2f, w / 2f),
        Offset(-w / 2f, y - h / 2f), Size(w, h), style = Stroke(h * .13f))
    drawOval(Brush.horizontalGradient(listOf(Color.White.copy(alpha = .92f), tone.copy(alpha = .46f),
        Color.White.copy(alpha = .74f)), -w / 2f, w / 2f),
        Offset(-w / 2f, y - h * .14f), Size(w * .90f, h * .30f), style = Stroke(h * .06f))
    // Faint reflection under the pool.
    drawOval(Brush.verticalGradient(listOf(Color.Transparent, tone.copy(alpha = .14f)), y, y + h * 2.2f),
        Offset(-w * .38f, y), Size(w * .76f, h * 2.2f))
}

// ---------------------------------------------------------------------------
// Iconography: volumetric, filled glyphs
// ---------------------------------------------------------------------------

/**
 * Filled, luminous glyphs used by the Home service tiles and the metric strip.
 * Each glyph is drawn as a solid gradient body with a bright edge light and an
 * internal highlight, so it reads as a lit object rather than a thin outline.
 */
@Composable
internal fun VolumetricGlyph(mark: Mark, tone: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { volumetricMark(mark, tone, size.toPx() / 2f) }
}

internal fun DrawScope.volumetricMark(mark: Mark, tone: Color, radius: Float) {
    // The glyph paths live in a 24x24 box, so scaling by radius*2/24 maps that
    // box exactly onto the canvas - no extra translation is wanted here.
    val unit = radius * 2f / 24f
    scale(unit, unit, Offset.Zero) {
            when (mark) {
                Mark.TRASH -> volumetricTrash(tone)
                Mark.OVERLAY, Mark.FILE, Mark.PICTURES -> volumetricDocuments(tone)
                Mark.DISK -> translate(12f, 0f) { volumetricStorage(tone) }
                Mark.SHIELD -> volumetricShield(tone)
                Mark.BATTERY -> volumetricBattery(tone)
                Mark.CHIP -> volumetricChip(tone)
                Mark.LAYERS -> translate(12f, 2f) { volumetricLayers(tone) }
                Mark.RADAR -> translate(12f, 5f) { volumetricRadar(tone) }
                Mark.GROUP -> volumetricGroup(tone)
                Mark.GOOGLE -> volumetricGoogle()
                else -> {
                    val p = glyphPath(mark) ?: return@scale
                    drawPath(p, Brush.verticalGradient(listOf(tone.copy(alpha = .85f), tone.copy(alpha = .35f)), -12f, 12f),
                        style = Stroke(1.9f, join = StrokeJoin.Round, cap = StrokeCap.Round))
                }
            }
        }
}

private fun DrawScope.volumetricGroup(tone: Color) {
    val shell = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(1f, 1f, 23f, 23f, CornerRadius(5f)))
    }
    litBody(shell, Color(0xff2631a2), tone, 18f)
    val people = glyphPath(Mark.GROUP) ?: return
    scale(.78f, .78f, Offset(12f, 12f)) {
        drawPath(people, tone.copy(alpha = .52f), style = Stroke(3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(people, Light.ice, style = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * The four-colour provider mark used by the "Continue with Google" row.
 *
 * The arc is drawn from the classic four-segment arrangement - red across the
 * top, yellow down the left, green along the bottom and blue closing the right
 * side with the horizontal bar - so the row reads as the reference does.
 */
private fun DrawScope.volumetricGoogle() {
    val red = Color(0xffea4335)
    val yellow = Color(0xfffbbc05)
    val green = Color(0xff34a853)
    val blue = Color(0xff4285f4)
    val r = 7.1f
    val w = 2.7f
    val box = Size(r * 2f, r * 2f)
    val topLeft = Offset(12f - r, 12f - r)
    // Angles follow the screen convention: 0 degrees is three o'clock and
    // positive sweeps run clockwise, so 210-330 covers the top arc.
    drawArc(red, 210f, 120f, false, topLeft, box, style = Stroke(w, cap = StrokeCap.Butt))
    drawArc(yellow, 150f, 60f, false, topLeft, box, style = Stroke(w, cap = StrokeCap.Butt))
    drawArc(green, 90f, 60f, false, topLeft, box, style = Stroke(w, cap = StrokeCap.Butt))
    drawArc(blue, 330f, 120f, false, topLeft, box, style = Stroke(w, cap = StrokeCap.Butt))
    // The bar of the G reaching into the centre of the mark.
    drawRoundRect(blue, Offset(12f - .2f, 12f - w / 2f), Size(r * .78f, w),
        androidx.compose.ui.geometry.CornerRadius(w / 2f))
}

private fun DrawScope.litBody(path: Path, tone: Color, top: Color = Light.white, radius: Float = 24f) {
    // Outer bloom first so it sits behind the body instead of eating the shape.
    drawPath(path, tone.copy(alpha = .50f), style = Stroke(3.6f, join = StrokeJoin.Round))
    drawPath(path, tone.copy(alpha = .68f), style = Stroke(1.7f, join = StrokeJoin.Round))
    val middle = Offset(12f, 12f)
    val body = Brush.linearGradient(listOf(lerpColor(top, tone, .58f).copy(alpha = .98f), tone.copy(alpha = 1f), tone.copy(alpha = .76f)),
        middle - Offset(radius * .45f, radius * .48f), middle + Offset(radius * .45f, radius * .48f))
    drawPath(path, body)
    // Fine edge light only - a thin bright contour keeps the glyph readable.
    drawPath(path, Color.White.copy(alpha = .62f), style = Stroke(.48f, join = StrokeJoin.Round))
}

private fun DrawScope.volumetricTrash(tone: Color) {
    val body = Path().apply {
        moveTo(5f, 7f); lineTo(19f, 7f); lineTo(17.6f, 21f); lineTo(6.4f, 21f); close()
    }
    litBody(body, tone)
    val lid = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(3f, 4.4f, 21f, 6.6f, CornerRadius(1.1f))) }
    drawPath(lid, Brush.verticalGradient(listOf(Light.white.copy(alpha = .92f), tone.copy(alpha = .95f))))
    val handle = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(9f, 2.2f, 15f, 4.6f, CornerRadius(1.2f))) }
    drawPath(handle, Brush.verticalGradient(listOf(tone.copy(alpha = .95f), tone.copy(alpha = .6f))))
    for (x in listOf(9.2f, 12f, 14.8f)) {
        drawLine(Color.White.copy(alpha = .38f), Offset(x, 9.2f), Offset(x - .5f, 19.4f), .85f, StrokeCap.Round)
    }
}

private fun DrawScope.volumetricDocuments(tone: Color) {
    val back = Path().apply {
        moveTo(8f, 2f); lineTo(16.4f, 2f); lineTo(21.4f, 7f); lineTo(21.4f, 20f); lineTo(8f, 20f); close()
    }
    drawPath(back, Brush.linearGradient(listOf(tone.copy(alpha = .85f), tone.copy(alpha = .35f)), Offset(8f, 2f), Offset(21f, 20f)))
    drawPath(back, Light.ice.copy(alpha = .55f), style = Stroke(.55f, join = StrokeJoin.Round))
    val front = Path().apply {
        moveTo(3f, 6f); lineTo(11.4f, 6f); lineTo(16.4f, 11f); lineTo(16.4f, 22f); lineTo(3f, 22f); close()
    }
    litBody(front, tone)
    val fold = Path().apply { moveTo(11.4f, 6f); lineTo(11.4f, 11f); lineTo(16.4f, 11f); close() }
    drawPath(fold, Light.white.copy(alpha = .78f))
    for (y in listOf(13.5f, 16.2f, 18.9f)) {
        drawLine(Light.ice.copy(alpha = .52f), Offset(5.6f, y), Offset(13.6f, y), .85f, StrokeCap.Round)
    }
}

private fun DrawScope.volumetricStorage(tone: Color) {
    // Three separated platters, each with a lit top edge and a shaded body.
    for (y in listOf(17.5f, 11.5f, 5.5f)) {
        val body = Path().apply {
            moveTo(-8.6f, y)
            cubicTo(-8.6f, y - 4.2f, 8.6f, y - 4.2f, 8.6f, y)
            lineTo(8.6f, y + 2.2f)
            cubicTo(8.6f, y + 5.8f, -8.6f, y + 5.8f, -8.6f, y + 2.2f)
            close()
        }
        drawPath(body, Brush.verticalGradient(listOf(tone.copy(alpha = .92f), tone.copy(alpha = .40f)), y - 4f, y + 5f))
        drawPath(body, Color.White.copy(alpha = .42f), style = Stroke(.38f))
        val top = Path().apply {
            moveTo(-8.6f, y)
            cubicTo(-8.6f, y - 4.2f, 8.6f, y - 4.2f, 8.6f, y)
            cubicTo(8.6f, y + 2.0f, -8.6f, y + 2.0f, -8.6f, y)
            close()
        }
        drawPath(top, Brush.verticalGradient(listOf(Light.white.copy(alpha = .88f), tone.copy(alpha = .80f)), y - 4f, y + 2f))
    }
}

private fun DrawScope.volumetricShield(tone: Color) {
    val shield = Path().apply {
        moveTo(12f, 1.8f); cubicTo(15.6f, 4.2f, 18.4f, 4.8f, 21f, 5.4f)
        lineTo(21f, 12f); cubicTo(20.2f, 16.6f, 16.8f, 20.4f, 12f, 22.2f)
        cubicTo(7.2f, 20.4f, 3.8f, 16.6f, 3f, 12f); lineTo(3f, 5.4f)
        cubicTo(5.6f, 4.8f, 8.4f, 4.2f, 12f, 1.8f); close()
    }
    litBody(shield, tone)
    val lock = Path().apply {
        moveTo(9.2f, 11.4f); lineTo(9.2f, 9.6f); cubicTo(9.2f, 6.8f, 14.8f, 6.8f, 14.8f, 9.6f)
        lineTo(14.8f, 11.4f); close()
    }
    drawPath(lock, Light.white.copy(alpha = .80f))
    val body = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(7.6f, 10.8f, 16.4f, 18.4f, CornerRadius(1.8f)))
    }
    drawPath(body, Brush.verticalGradient(listOf(Light.white.copy(alpha = .92f), tone.copy(alpha = .85f))))
    drawCircle(Light.deep.copy(alpha = .70f), 1.15f, Offset(12f, 14f))
}

private fun DrawScope.volumetricBattery(tone: Color) {
    val cap = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(8.6f, 1.6f, 15.4f, 4.2f, CornerRadius(1.2f))) }
    drawPath(cap, Brush.verticalGradient(listOf(Light.white.copy(alpha = .90f), tone.copy(alpha = .85f))))
    val body = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(4.4f, 4.2f, 19.6f, 21.6f, CornerRadius(3.2f))) }
    litBody(body, tone)
    val bolt = Path().apply {
        moveTo(13.4f, 6.4f); lineTo(8.2f, 13.2f); lineTo(11.6f, 13.2f); lineTo(10.6f, 19.4f)
        lineTo(15.8f, 12.2f); lineTo(12.4f, 12.2f); close()
    }
    drawPath(bolt, Brush.verticalGradient(listOf(Light.white, tone.copy(alpha = .95f))))
}

private fun DrawScope.volumetricChip(tone: Color) {
    for (i in listOf(6.8f, 12f, 17.2f)) {
        for (side in listOf(-1f, 1f)) {
            val pin = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(i - .9f, if (side < 0) 1.4f else 19.6f, i + .9f, if (side < 0) 5f else 23.2f, CornerRadius(.8f))) }
            val plating = Brush.verticalGradient(listOf(Light.ice, tone.copy(alpha = .8f)), 0f, 24f)
            drawPath(pin, plating)
            rotate(90f, Offset(12f, 12f)) { drawPath(pin, plating) }
        }
    }
    val body = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(3.4f, 3.4f, 20.6f, 20.6f, CornerRadius(4.4f))) }
    litBody(body, tone)
    val inner = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(6.4f, 6.4f, 17.6f, 17.6f, CornerRadius(2.6f))) }
    drawPath(inner, Brush.radialGradient(listOf(Color(0xff342c82), Color(0xff120c36)), Offset(12f, 11f), 8f))
    drawPath(inner, Light.ice.copy(alpha = .35f), style = Stroke(.5f))
    val a = Path().apply {
        moveTo(8.6f, 15.4f); lineTo(10.9f, 8.4f); lineTo(12.1f, 8.4f); lineTo(14.4f, 15.4f)
        lineTo(13f, 15.4f); lineTo(12.5f, 13.4f); lineTo(10.6f, 13.4f); lineTo(10.1f, 15.4f); close()
        moveTo(10.9f, 12f); lineTo(12.2f, 12f); lineTo(11.55f, 9.9f); close()
    }
    drawPath(a, Brush.verticalGradient(listOf(Light.white, tone.copy(alpha = .9f))))
    val i = Path().apply {
        moveTo(15.6f, 8.4f); lineTo(17f, 8.4f); lineTo(17f, 15.4f); lineTo(15.6f, 15.4f); close()
    }
    drawPath(i, Brush.verticalGradient(listOf(Light.white, tone.copy(alpha = .9f))))
}

private fun DrawScope.volumetricLayers(tone: Color) {
    for (y in listOf(15.4f, 9.4f, 3.4f)) {
        val slab = Path().apply {
            moveTo(-9.4f, y); lineTo(0f, y - 5f); lineTo(9.4f, y); lineTo(0f, y + 5f); close()
        }
        litBody(slab, tone)
    }
}

private fun DrawScope.volumetricRadar(tone: Color) {
    val dish = Path().apply {
        moveTo(-8.4f, 6.4f); cubicTo(-8.4f, -6.4f, 8.4f, -6.4f, 8.4f, 6.4f)
        cubicTo(8.4f, 12.4f, -8.4f, 12.4f, -8.4f, 6.4f); close()
    }
    litBody(dish, tone)
    drawLine(Light.ice.copy(alpha = .7f), Offset(0f, 5.6f), Offset(0f, 15.6f), 1.1f, StrokeCap.Round)
    drawCircle(tone.copy(alpha = .85f), 2.2f, Offset(0f, 16.4f))
}
