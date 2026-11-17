package com.knoux.ai.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.knoux.ai.Chalk
import com.knoux.ai.Glyph
import com.knoux.ai.Mark
import com.knoux.ai.NativePalette

/**
 * The KNOUX gate seal — the one instrument on the identity surface.
 *
 * A shield carries the brand K, two tilted orbits cross behind it, and a standing
 * pedestal ring holds it in place. The seal draws only states the gateway really
 * has: [SealState.SCANNING] only while a real `BiometricPrompt` is open,
 * [SealState.VERIFIED] only from its success callback, and [SealState.UNAVAILABLE]
 * only when the platform reports no usable biometric. An unavailable seal is drawn
 * in fog, never in an alert colour, because nothing failed — the device simply has
 * no enrolled print to read.
 *
 * Deliberately compact: the identity mark is a seal, not a screen-filling orb.
 */
internal enum class SealState { IDLE, SCANNING, VERIFIED, UNAVAILABLE }

@Composable
internal fun PortalSeal(
    state: SealState,
    size: Dp,
    motion: Boolean,
    modifier: Modifier = Modifier,
    tag: String? = null,
    /** The face mark: the brand K, or the lock of the approved credential seal. */
    face: Mark = Mark.K,
    faceScale: Float = 0.270f,
) {
    val orbit = rememberMotionPhase(motion && state != SealState.UNAVAILABLE, 16_000, "seal orbit")
    val breath = rememberMotionPhase(motion, 5_400, "seal breath")
    val scan = rememberMotionPhase(motion && state == SealState.SCANNING, 2_100, "seal scan")

    val tone = when (state) {
        SealState.IDLE -> NativePalette.cyan
        SealState.SCANNING -> NativePalette.cyan
        SealState.VERIFIED -> NativePalette.green
        SealState.UNAVAILABLE -> InstrumentFog
    }

    Box(
        modifier.size(size).let { if (tag != null) it.testTag(tag) else it },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawSeal(state, tone, orbit.value, breath.value, scan.value)
        }
        // The face mark sits on the shield, never on the orbit.
        Glyph(face, Modifier.size(size * faceScale), Chalk, true)
        if (state == SealState.VERIFIED || state == SealState.UNAVAILABLE) {
            val verified = state == SealState.VERIFIED
            Box(
                Modifier
                    .offset(x = size * 0.145f, y = size * 0.180f)
                    .size(size * 0.190f)
                    .clip(CircleShape)
                    .background(if (verified) NativePalette.green else NativePalette.amber),
                contentAlignment = Alignment.Center,
            ) {
                Glyph(
                    if (verified) Mark.CHECK else Mark.WARN,
                    Modifier.size(size * 0.105f),
                    Color(0xFF05091E),
                )
            }
        }
    }
}

private fun DrawScope.drawSeal(
    state: SealState,
    tone: Color,
    orbit: Float,
    breath: Float,
    scan: Float,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val shieldH = h * 0.720f
    val shieldW = shieldH * 0.780f
    val top = h * 0.055f
    val left = cx - shieldW / 2f
    val bottom = top + shieldH
    val focus = Offset(cx, top + shieldH * 0.46f)
    val settle = if (state == SealState.VERIFIED) 0.24f + breath * 0.12f else 0.18f + breath * 0.10f

    // Standing glow: the seal is lit from inside the shield, not from a halo.
    drawCircle(
        Brush.radialGradient(
            listOf(tone.copy(alpha = settle), tone.copy(alpha = settle * 0.32f), Color.Transparent),
            focus,
            shieldW * 0.98f,
        ),
        radius = shieldW * 0.98f,
        center = focus,
    )

    // Pedestal: one lit ellipse and its rim.
    val pedestalY = bottom + h * 0.045f
    val pedestalHalf = shieldW * 0.60f
    drawOval(
        Brush.radialGradient(
            listOf(tone.copy(alpha = 0.42f), tone.copy(alpha = 0.12f), Color.Transparent),
            Offset(cx, pedestalY),
            pedestalHalf,
        ),
        topLeft = Offset(cx - pedestalHalf, pedestalY - h * 0.026f),
        size = Size(pedestalHalf * 2f, h * 0.052f),
    )
    drawOval(
        tone.copy(alpha = 0.46f),
        topLeft = Offset(cx - pedestalHalf, pedestalY - h * 0.026f),
        size = Size(pedestalHalf * 2f, h * 0.052f),
        style = Stroke(1.dp.toPx()),
    )

    // Two orbits cross behind the shield. The lit one drifts, the faint one is
    // dashed, so a still frame still reads as depth.
    rotate(-17f + (orbit - 0.5f) * 6f, focus) {
        drawOval(
            Brush.sweepGradient(
                listOf(Color.Transparent, tone.copy(alpha = 0.62f), NativePalette.violet.copy(alpha = 0.46f), Color.Transparent),
                focus,
            ),
            topLeft = Offset(cx - shieldW * 0.82f, focus.y - shieldW * 0.24f),
            size = Size(shieldW * 1.64f, shieldW * 0.48f),
            style = Stroke(1.3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
    rotate(21f - (orbit - 0.5f) * 4f, focus) {
        drawOval(
            InstrumentFog.copy(alpha = if (state == SealState.UNAVAILABLE) 0.55f else 0.34f),
            topLeft = Offset(cx - shieldW * 0.70f, focus.y - shieldW * 0.19f),
            size = Size(shieldW * 1.40f, shieldW * 0.38f),
            style = Stroke(
                1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx())),
            ),
        )
    }

    val shield = sealOutline(Offset(left, top), shieldW, shieldH)
    drawPath(
        shield,
        Brush.verticalGradient(
            listOf(Color(0xFF2A1B66), Color(0xFF141541), Color(0xFF0A0E24)),
            startY = top,
            endY = bottom,
        ),
    )
    // A lit core behind the K keeps the glass face from reading as a dark hole.
    drawCircle(
        Brush.radialGradient(
            listOf(tone.copy(alpha = 0.26f), Color.Transparent),
            focus,
            shieldW * 0.46f,
        ),
        radius = shieldW * 0.46f,
        center = focus,
    )
    drawPath(
        shield,
        Brush.linearGradient(
            listOf(tone, NativePalette.violet, tone.copy(alpha = 0.72f)),
            Offset(left, top),
            Offset(left + shieldW, bottom),
        ),
        style = Stroke(2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round),
    )
    // Inner contour keeps the glass face from looking like a flat sticker.
    val inner = sealOutline(
        Offset(left + shieldW * 0.065f, top + shieldH * 0.055f),
        shieldW * 0.870f,
        shieldH * 0.880f,
    )
    drawPath(inner, Chalk.copy(alpha = 0.10f), style = Stroke(1.dp.toPx()))
    drawPath(
        inner,
        Brush.verticalGradient(
            listOf(Chalk.copy(alpha = 0.20f), Color.Transparent),
            startY = top,
            endY = top + shieldH * 0.50f,
        ),
        style = Stroke(1.6.dp.toPx()),
    )

    if (state == SealState.SCANNING) {
        // A single read band clipped to the shield face — the only moving element
        // while a real prompt is open.
        clipPath(shield) {
            val y = top + shieldH * scan.coerceIn(0f, 1f)
            val band = shieldH * 0.090f
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, tone.copy(alpha = 0.46f), Color.Transparent),
                    startY = y - band,
                    endY = y + band,
                ),
                topLeft = Offset(left, y - band),
                size = Size(shieldW, band * 2f),
            )
        }
    }

    if (state == SealState.VERIFIED) {
        drawCircle(
            NativePalette.green.copy(alpha = 0.16f + breath * 0.10f),
            radius = shieldW * 0.76f,
            center = focus,
            style = Stroke(1.2.dp.toPx()),
        )
    }
}

/** A shield with a soft shoulder and a long taper — the KNOUX silhouette. */
private fun sealOutline(topLeft: Offset, width: Float, height: Float): Path {
    val x = topLeft.x
    val y = topLeft.y
    return Path().apply {
        moveTo(x + width / 2f, y)
        lineTo(x + width, y + height * 0.160f)
        lineTo(x + width, y + height * 0.560f)
        cubicTo(
            x + width, y + height * 0.760f,
            x + width * 0.740f, y + height * 0.900f,
            x + width / 2f, y + height,
        )
        cubicTo(
            x + width * 0.260f, y + height * 0.900f,
            x, y + height * 0.760f,
            x, y + height * 0.560f,
        )
        lineTo(x, y + height * 0.160f)
        close()
    }
}
