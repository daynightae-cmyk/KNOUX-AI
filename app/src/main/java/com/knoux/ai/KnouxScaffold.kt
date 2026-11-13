package com.knoux.ai

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Minimum touch target the interface allows. */
internal val TOUCH_TARGET = 48.dp

/**
 * An opaque surface with a thin border.
 *
 * The brief keeps glass for the few main cards of a screen; everything else is
 * a quiet solid surface so the interface does not sparkle everywhere.
 */
@Composable
internal fun SurfaceCard(
    modifier: Modifier = Modifier,
    variant: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(if (variant) KnouxColors.surfaceVariant else KnouxColors.surface)
            .border(1.dp, KnouxColors.border, shape),
        content = content,
    )
}

/** A small state chip: measured, not measured, unavailable. */
@Composable
internal fun StatePill(text: Copy, tint: Color, tag: String? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = .14f))
            .border(1.dp, tint.copy(alpha = .45f), RoundedCornerShape(50))
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(tint))
        Label(text, 12, tint, FontWeight.SemiBold)
    }
}

/**
 * One fixed bar above the navigation: a single primary action and one
 * secondary. Nothing else lives here, so the customer always knows where the
 * decision is.
 */
@Composable
internal fun BottomActionBar(
    primary: Action,
    secondary: Action?,
    enabled: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrimaryAction(primary, enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPrimary()
        }
        secondary?.let {
            SecondaryAction(it) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSecondary()
            }
        }
    }
}

@Composable
internal fun PrimaryAction(action: Action, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val tint = if (enabled) KnouxColors.action else KnouxColors.surfaceVariant
    val ink = if (enabled) KnouxColors.onAction else KnouxColors.textSecondary
    Row(
        Modifier.fillMaxWidth().heightIn(min = TOUCH_TARGET).testTag("primary-action")
            .clip(shape).background(tint)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Glyph(action.icon, Modifier.size(18.dp), ink)
        Spacer(Modifier.width(8.dp))
        Label(action.label, 15, ink, FontWeight.Bold, align = TextAlign.Center)
    }
}

@Composable
internal fun SecondaryAction(action: Action, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = TOUCH_TARGET).testTag("secondary-action")
            .clip(shape).background(KnouxColors.surface)
            .border(1.dp, KnouxColors.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Glyph(action.icon, Modifier.size(18.dp), KnouxColors.accent)
        Spacer(Modifier.width(8.dp))
        Label(action.label, 14, KnouxColors.textPrimary, FontWeight.SemiBold, align = TextAlign.Center)
    }
}

/** Which of the four per-screen states is on show. */
internal enum class UiState { LOADING, EMPTY, ERROR, SUCCESS }

/**
 * One visual language for loading, empty, error and success, so a customer
 * never has to relearn the screen.
 */
@Composable
internal fun StateBlock(state: UiState, detail: Copy, onRetry: (() -> Unit)? = null) {
    val (tint, mark, title) = when (state) {
        UiState.LOADING -> Triple(KnouxColors.accent, Mark.SYNC, Copy(R.string.state_loading))
        UiState.EMPTY -> Triple(KnouxColors.textSecondary, Mark.CHECK, Copy(R.string.state_empty))
        UiState.ERROR -> Triple(KnouxColors.danger, Mark.WARN, Copy(R.string.state_error))
        UiState.SUCCESS -> Triple(KnouxColors.success, Mark.CHECK, Copy(R.string.state_success))
    }
    Column(
        Modifier.fillMaxWidth().testTag("state-" + state.name.lowercase())
            .clip(RoundedCornerShape(16.dp)).background(KnouxColors.surface)
            .border(1.dp, KnouxColors.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state == UiState.LOADING) Shimmer()
        Glyph(mark, Modifier.size(28.dp), tint)
        Label(title, 15, tint, FontWeight.SemiBold, align = TextAlign.Center)
        Label(detail, 13, KnouxColors.textSecondary, align = TextAlign.Center)
        onRetry?.let { retry ->
            SecondaryAction(Action(Copy(R.string.action_retry), Mark.SYNC), retry)
        }
    }
}

/** A quiet skeleton line, used while a real measurement is running. */
@Composable
internal fun Shimmer() {
    val motion = LocalPreviewMotion.current
    val alpha = if (motion) {
        val transition = rememberInfiniteTransition(label = "skeleton")
        transition.animateFloat(
            0.18f, 0.42f,
            infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "skeleton",
        ).value
    } else 0.30f
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1f, .8f, .6f).forEach { fraction ->
            Box(
                Modifier.fillMaxWidth(fraction).height(10.dp).clip(RoundedCornerShape(50))
                    .background(KnouxColors.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

/** An icon that is the only content of its control must describe itself. */
internal fun Modifier.iconLabel(description: String): Modifier =
    semantics { contentDescription = description }
