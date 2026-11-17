package com.knoux.ai.visual

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.knoux.ai.Copy
import com.knoux.ai.Label
import com.knoux.ai.LocalPreviewMotion
import com.knoux.ai.NativePalette

/**
 * Shared chrome for the eight signature instruments.
 *
 * Every instrument obeys the same contract (`docs/design/KNOUX_SERVICE_VISUAL_IDENTITY_20261113.md`):
 * one `instrument-*` test tag, a content description built from real state, motion
 * only while work is observable, a truthful still frame under reduced motion, fog
 * for anything unmeasured, and no numeral the engine did not count. This kit holds
 * the two pieces every instrument repeats so the geometry files stay about geometry.
 */

/**
 * One looping phase in [0,1], or a fixed mid-phase when motion is off.
 *
 * The fixed value is deliberately not 0: a still instrument should read as a
 * composed frame, not as an animation caught at its first tick. Callers gate
 * [active] on both the system preference and whether work is actually observable,
 * so an idle instrument never spins.
 */
@Composable
internal fun rememberMotionPhase(active: Boolean, periodMillis: Int, label: String): State<Float> {
    if (!active) return remember { mutableStateOf(0.5f) }
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "$label phase",
    )
}

/**
 * The one-line caption under an instrument: a tone dot, the instrument's own name
 * in micro-caps, and a short real status. The status is a [Copy] so it localises;
 * it is never engine prose. Text budget stays at name + a few words.
 */
@Composable
internal fun InstrumentCaption(
    name: Copy,
    status: Copy?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(6.dp).clip(CircleShape).background(tint),
        )
        Label(name, 10, tint, FontWeight.Bold, maxLines = 1)
        if (status != null) {
            Label(
                status, 10, NativePalette.muted,
                modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The quiet fog tone used for every unmeasured scope across the instruments. */
internal val InstrumentFog = Color(0xFF6E7BA0)

/** A shared deep well colour so instrument faces sit one step below the card. */
internal val InstrumentWell = Color(0xFF0A0F26)

/** System reduced-motion preference, read once per instrument. */
@Composable
internal fun instrumentMotion(): Boolean = LocalPreviewMotion.current
