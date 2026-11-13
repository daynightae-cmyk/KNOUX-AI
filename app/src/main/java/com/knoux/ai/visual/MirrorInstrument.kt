package com.knoux.ai.visual

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.knoux.ai.Copy
import com.knoux.ai.Label
import com.knoux.ai.LocalArabic
import com.knoux.ai.LocalPreviewMotion
import com.knoux.ai.Mark
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * PC02 — Mirror, the signature instrument for Duplicate Files.
 *
 * Twin glass orbs on a tether: one orb is the protected keeper (original), the
 * other represents the removable copies. Each real duplicate group adds one real
 * satellite pair. Choosing an orb collapses its twin into the keeper; the keeper
 * is always sealed and never removable.
 *
 * Visual contract (KNOUX_SERVICE_VISUAL_IDENTITY_20261113.md):
 * - Dominant hue: magenta (#FF00EE) / violet (#8B5CF6)
 * - Geometry: circular / paired orbs / radial tether, never rectangles
 * - Motion: real group/finding count only; tether tension grows with findings
 * - progress == null -> listening sweep, never a fabricated percentage
 * - Unmeasured scopes = fogged hollow orb, named once
 * - Reduced motion = still frame still communicates state
 */
@Composable
internal fun MirrorInstrument(
    frame: MirrorFrame,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val motion = LocalPreviewMotion.current
    val arabic = LocalArabic.current
    val density = LocalDensity.current

    val magenta = Color(0xffFF00EE)
    val violet = Color(0xff8B5CF6)
    val fog = Color(0xff6E6A92)
    val chalk = Color(0xffE8E6F0)
    val mist = Color(0xffA8A4C8).copy(alpha = 0.64f)
    val gold = Color(0xffFFC24B)
    val ink = Color(0xff07051A)

    // Breath for idle / armed
    val breathTransition = rememberInfiniteTransition(label = "mirror breath")
    val breath: androidx.compose.runtime.State<Float> = breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Pulse for measured pairs
    val pulseTransition = rememberInfiniteTransition(label = "pair pulse")
    val pulse: androidx.compose.runtime.State<Float> = pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Tether tension (grows with real findings, never with time)
    val tension = frame.tensionAnim.value // 0..1, real finding fraction

    val instrumentHeight = 320.dp

    val a11yDesc = description ?: buildContentDescription(frame, arabic)

    Box(
        modifier
            .fillMaxWidth()
            .height(instrumentHeight)
            .testTag("instrument-mirror")
            .semantics { contentDescription = a11yDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            val orbR = min(w, h) * 0.22f
            val pairGap = w * 0.28f

            // ── Tether between orbs ──────────────────────────────────────────
            val leftCx = centerX - pairGap / 2f
            val rightCx = centerX + pairGap / 2f
            val tetherSag = 12f * (1f - tension) * orbR / 40f
            val tetherPath = Path().apply {
                moveTo(leftCx, centerY - orbR)
                cubicTo(
                    centerX, centerY - orbR - tetherSag,
                    centerX, centerY - orbR + tetherSag,
                    rightCx, centerY - orbR
                )
            }
            drawPath(
                path = tetherPath,
                color = if (frame.hasFindings) magenta.copy(alpha = 0.5f + 0.3f * tension)
                else fog.copy(alpha = 0.25f),
                style = Stroke(with(density) { 2.5.dp.toPx() })
            )
            // Tether glow when measured
            if (frame.hasFindings) {
                drawPath(
                    path = tetherPath,
                    color = magenta.copy(alpha = 0.12f + 0.18f * pulse.value),
                    style = Stroke(with(density) { 6.dp.toPx() })
                )
            }

            // ── Left orb: Keeper (original, protected) ───────────────────────
            val keeperGlow = if (frame.hasFindings) 0.35f + 0.25f * pulse.value else 0.2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(magenta.copy(alpha = 0.5f * keeperGlow), Color.Transparent),
                    center = Offset(leftCx, centerY),
                    radius = orbR * 1.9f
                ),
                radius = orbR * 1.9f,
                center = Offset(leftCx, centerY),
            )
            drawCircle(
                color = magenta.copy(alpha = 0.85f),
                radius = orbR,
                center = Offset(leftCx, centerY),
                style = Stroke(with(density) { 2.5.dp.toPx() }),
            )
            drawCircle(
                color = Color(0xff1A1642),
                radius = orbR * 0.72f,
                center = Offset(leftCx, centerY),
            )
            // Keeper badge (lock)
            drawCircle(
                color = Color(0xff3DFFB0).copy(alpha = 0.9f),
                radius = orbR * 0.28f,
                center = Offset(leftCx - orbR * 0.35f, centerY - orbR * 0.35f),
            )
            drawCircle(
                color = ink,
                radius = orbR * 0.14f,
                center = Offset(leftCx - orbR * 0.35f, centerY - orbR * 0.35f),
                style = Stroke(with(density) { 2.dp.toPx() }),
            )

            // ── Right orb: Copies (removable) ────────────────────────────────
            val copyGlow = if (frame.hasFindings) 0.35f + 0.25f * (1f - pulse.value) else 0.2f
            val copyColor = if (frame.hasFindings) violet else fog.copy(alpha = 0.35f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(copyColor.copy(alpha = 0.45f * copyGlow), Color.Transparent),
                    center = Offset(rightCx, centerY),
                    radius = orbR * 1.7f
                ),
                radius = orbR * 1.7f,
                center = Offset(rightCx, centerY),
            )
            drawCircle(
                color = copyColor,
                radius = orbR,
                center = Offset(rightCx, centerY),
                style = Stroke(with(density) { 2.dp.toPx() }),
            )
            drawCircle(
                color = Color(0xff1A1642),
                radius = orbR * 0.68f,
                center = Offset(rightCx, centerY),
            )
            // Copy count mark on the copies orb
            if (frame.groupCount > 0) {
                drawCircle(
                    color = gold.copy(alpha = 0.85f),
                    radius = orbR * 0.22f,
                    center = Offset(rightCx + orbR * 0.3f, centerY - orbR * 0.3f),
                )
            }

            // ── Satellites: one pair per group ───────────────────────────────
            val pairCount = frame.groupCount.coerceAtLeast(1)
            val pairAngleStep = 360f / max(1, pairCount)
            for (i in 0 until pairCount) {
                val angle = (-90f + i * pairAngleStep) * PI.toFloat() / 180f
                val orbitR = orbR * 1.55f
                val sx = centerX + orbitR * cos(angle)
                val sy = centerY + orbitR * sin(angle)
                val isKeeperSide = cos(angle) < 0
                val satColor = if (isKeeperSide) magenta else violet
                drawCircle(
                    color = satColor.copy(alpha = 0.6f),
                    radius = with(density) { 5.dp.toPx() },
                    center = Offset(sx, sy),
                )
                // Tether line from center to satellite
                if (frame.hasFindings) {
                    drawLine(
                        color = satColor.copy(alpha = 0.3f),
                        start = Offset(centerX, centerY),
                        end = Offset(sx, sy),
                        strokeWidth = with(density) { 1.dp.toPx() },
                    )
                }
            }

            // ── Failure / empty marker ───────────────────────────────────────
            if (frame.phase == MirrorPhase.FAILED || frame.phase == MirrorPhase.FOGGED) {
                drawLine(
                    color = Color(0xffFF6B6B).copy(alpha = 0.7f),
                    start = Offset(centerX - with(density) { 14.dp.toPx() }, centerY),
                    end = Offset(centerX + with(density) { 14.dp.toPx() }, centerY),
                    strokeWidth = with(density) { 2.dp.toPx() },
                )
            }
        }

        // ── Overlay: hero numeral + caption ──────────────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .align(Alignment.Center),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)
            ) {
                frame.heroValue?.let { bytes ->
                    Label(
                        text = mirrorFormatBytes(bytes),
                        size = 24,
                        tint = chalk,
                        weight = FontWeight.ExtraBold,
                        align = TextAlign.Center
                    )
                }
                Label(
                    text = frame.caption,
                    size = 11,
                    tint = mist,
                    weight = FontWeight.Medium,
                    align = TextAlign.Center
                )
            }
        }
    }
}

/** Byte formatting used by the instrument only. */
internal fun mirrorFormatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000_000L -> "%.2f TB".format(bytes / 1_000_000_000_000.0)
    bytes >= 1_000_000_000L -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> "%.2f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000L -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

private fun buildContentDescription(frame: MirrorFrame, arabic: Boolean): String {
    val phaseWord = when (frame.phase) {
        MirrorPhase.IDLE -> if (arabic) "سكون" else "idle"
        MirrorPhase.ARMED -> if (arabic) "جاهز" else "armed"
        MirrorPhase.RUNNING -> if (arabic) "فحص" else "scanning"
        MirrorPhase.PAUSED -> if (arabic) "متوقف" else "paused"
        MirrorPhase.RESULT -> if (arabic) "نتيجة" else "result"
        MirrorPhase.PARTIAL -> if (arabic) "جزئي" else "partial"
        MirrorPhase.FAILED -> if (arabic) "فشل" else "failed"
        MirrorPhase.FOGGED -> if (arabic) "غير مقاس" else "fogged"
    }
    val hero = frame.heroValue?.let { mirrorFormatBytes(it) } ?: "0"
    return if (arabic) {
        "مرآة التكرار، $phaseWord، $hero، ${frame.groupCount} مجموعة"
    } else {
        "Mirror, $phaseWord, $hero, ${frame.groupCount} group(s)"
    }
}

/** Phase of the Mirror instrument lifecycle. */
internal enum class MirrorPhase { IDLE, ARMED, RUNNING, PAUSED, RESULT, PARTIAL, FAILED, FOGGED }

/** Complete frame for one Mirror render. */
internal data class MirrorFrame(
    val phase: MirrorPhase,
    /** Completed-group fraction driving tether tension (0..1), null = indeterminate. */
    val tension: Float?,
    /** Measured redundant bytes (hero). Null before any measurement. */
    val heroValue: Long?,
    /** Number of real duplicate groups measured. */
    val groupCount: Int,
    /** Reduced-motion flag. */
    val motion: Boolean,
    /** Short caption (≤ 4 words). */
    val caption: Copy,
    /** Whether any real findings exist. */
    val hasFindings: Boolean,
    /** Device-independent width available to the instrument. */
    val widthDp: Float,
    /** Age of last real result (millis), for idle display. */
    val lastResultAgeMillis: Long? = null
) {
    val tensionAnim: androidx.compose.runtime.State<Float>
        get() {
            val target = when (phase) {
                MirrorPhase.IDLE -> 0f
                MirrorPhase.ARMED -> 0.1f
                MirrorPhase.RUNNING -> tension ?: 0f
                MirrorPhase.PAUSED -> tension ?: 0f
                MirrorPhase.RESULT -> 1f
                MirrorPhase.PARTIAL -> tension ?: 0f
                MirrorPhase.FAILED, MirrorPhase.FOGGED -> 0f
            }
            return androidx.compose.runtime.mutableStateOf(target.coerceIn(0f, 1f))
        }
}
