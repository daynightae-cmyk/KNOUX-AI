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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * The observatory sky — the one atmosphere every utility screen sits on.
 *
 * The product reads the device the way an observatory reads the night: a deep
 * navy field, two slow aurora ribbons in the logo's own colours, a faint fixed
 * starfield and the three brand arcs the shell has always carried. Nothing here
 * encodes a measurement; it is the room the instruments live in, so it stays
 * quiet enough that a measured number always out-shouts it.
 *
 * Motion is one shared 48s phase. With reduced motion the same composition is
 * drawn at a fixed phase — a still sky, not a frozen animation. Star positions
 * are seeded once and remembered, and the draw loop allocates nothing per
 * frame beyond the two ribbon paths.
 */
@Composable
internal fun NativeAtmosphere(modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    val phase = if (motion) {
        val transition = rememberInfiniteTransition(label = "observatory sky")
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(48_000, easing = LinearEasing), RepeatMode.Restart),
            label = "sky phase",
        ).value
    } else 0.31f

    // A fixed, seeded starfield: the sky is the same sky on every launch, so a
    // screenshot and a reduced-motion frame agree with the live one.
    val stars = remember {
        val random = kotlin.random.Random(20261115L)
        List(52) {
            StarSeed(
                x = random.nextFloat(),
                y = random.nextFloat() * .92f,
                radius = .5f + random.nextFloat() * 1.1f,
                baseAlpha = .05f + random.nextFloat() * .16f,
                twinkleOffset = random.nextFloat(),
                warm = random.nextFloat() < .22f,
            )
        }
    }

    Canvas(modifier.semantics { contentDescription = "" }) {
        drawSky(phase, stars, motion)
    }
}

private data class StarSeed(
    val x: Float,
    val y: Float,
    val radius: Float,
    val baseAlpha: Float,
    val twinkleOffset: Float,
    val warm: Boolean,
)

private fun DrawScope.drawSky(phase: Float, stars: List<StarSeed>, motion: Boolean) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val drift = phase * 2f * PI.toFloat()

    // Two light pools anchor the field diagonally: violet at the top-trailing
    // corner, cyan at the bottom-leading corner. They breathe by ±12% with the
    // shared phase so the room feels lit rather than painted.
    val breath = if (motion) 1f + 0.12f * sin(drift) else 1f
    drawRect(
        Brush.radialGradient(
            listOf(NativePalette.violet.copy(alpha = .062f * breath), Color.Transparent),
            center = Offset(w * .96f, h * .01f),
            radius = w * .92f,
        )
    )
    drawRect(
        Brush.radialGradient(
            listOf(NativePalette.cyan.copy(alpha = .052f * (2f - breath)), Color.Transparent),
            center = Offset(w * .03f, h * .93f),
            radius = w * .85f,
        )
    )

    // Aurora ribbons: one violet→magenta band high in the field, one cyan band
    // lower. Each is a single cubic whose control points slide a few percent
    // with the phase — a slow drift, never a wave the eye can chase.
    val ribbonStroke = (1.6.dp.toPx()).coerceAtLeast(1f)
    val slide = sin(drift) * h * .012f
    val upper = Path().apply {
        moveTo(-w * .05f, h * .052f + slide)
        cubicTo(w * .30f, h * .006f - slide, w * .62f, h * .112f + slide, w * 1.05f, h * .040f - slide)
    }
    drawPath(
        upper,
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                NativePalette.violet.copy(alpha = .20f),
                NativePalette.magenta.copy(alpha = .12f),
                NativePalette.cyan.copy(alpha = .16f),
                Color.Transparent,
            ),
            startX = 0f, endX = w,
        ),
        style = Stroke(ribbonStroke),
    )
    val lower = Path().apply {
        moveTo(-w * .05f, h * .906f - slide)
        cubicTo(w * .32f, h * .952f + slide, w * .66f, h * .878f - slide, w * 1.05f, h * .924f + slide)
    }
    drawPath(
        lower,
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                NativePalette.cyan.copy(alpha = .14f),
                NativePalette.violet.copy(alpha = .11f),
                Color.Transparent,
            ),
            startX = 0f, endX = w,
        ),
        style = Stroke(ribbonStroke * .8f),
    )

    // The starfield. Twinkle is a per-star sine on the same shared phase, so
    // the whole sky costs one animation value. A still sky keeps every star at
    // its base alpha — present, not blinking.
    stars.forEach { star ->
        val twinkle = if (motion) {
            .65f + .35f * sin(drift * 2f + star.twinkleOffset * 2f * PI.toFloat())
        } else 1f
        val tint = if (star.warm) NativePalette.violet else NativePalette.cyan
        drawCircle(
            tint.copy(alpha = star.baseAlpha * twinkle),
            radius = star.radius.dp.toPx(),
            center = Offset(star.x * w, star.y * h),
        )
    }

    // The floor beam: one quiet horizon line where the dock floats, so the
    // bottom of the field reads as ground rather than a cut edge.
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, NativePalette.blue.copy(alpha = .045f)),
            startY = h * .86f, endY = h,
        ),
        topLeft = Offset(0f, h * .86f),
        size = androidx.compose.ui.geometry.Size(w, h * .14f),
    )
}