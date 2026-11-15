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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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

/** Figma 2:2, 12:45 and 12:147 — native utility palette.
 * These are deliberately screen-level tokens; approved identity, Splash and
 * authentication artwork retain their original KNOUX signature colors.
 */
internal object NativePalette {
    val ink = Color(0xFF090A1E)
    val card = Color(0xFF0F1734)
    val raised = Color(0xFF152343)
    val border = Color(0xFF263558)
    val blue = Color(0xFF1287F7)
    val cyan = Color(0xFF2AB9FF)
    val violet = Color(0xFFA579EC)
    val text = Color(0xFFF4F7FF)
    val muted = Color(0xFFA5B4D0)
}

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
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .nxCard(18, glow = Nx.violet, glow2 = Nx.cyan, selected = variant),
        content = content,
    )
}

/** A small state chip: measured, not measured, unavailable. */
@Composable
internal fun StatePill(text: Copy, tint: Color, tag: String? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = .15f))
            .border(1.dp, tint.copy(alpha = .50f), RoundedCornerShape(50))
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(tint))
        Label(text, 11, tint, FontWeight.SemiBold, maxLines = 1)
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
    primaryTag: String = "primary-action",
    secondaryTag: String = "secondary-action",
) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrimaryAction(primary, enabled, onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPrimary()
        }, tag = primaryTag)
        secondary?.let {
            SecondaryAction(it, {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSecondary()
            }, secondaryTag)
        }
    }
}

@Composable
internal fun PrimaryAction(action: Action, enabled: Boolean = true, onClick: () -> Unit, tag: String = "primary-action") {
    NxPrimary(action.label, Modifier.fillMaxWidth(), action.icon, enabled = enabled, tag = tag, onClick = onClick)
}

@Composable
internal fun SecondaryAction(action: Action, onClick: () -> Unit, tag: String = "secondary-action") {
    NxSecondary(action.label, Modifier.fillMaxWidth(), action.icon, tag = tag, onClick = onClick)
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
        UiState.LOADING -> Triple(Nx.cyan, Mark.SYNC, Copy(R.string.state_loading))
        UiState.EMPTY -> Triple(Nx.muted, Mark.BOX, Copy(R.string.state_empty))
        UiState.ERROR -> Triple(Nx.rose, Mark.WARN, Copy(R.string.state_error))
        UiState.SUCCESS -> Triple(Nx.green, Mark.CHECK, Copy(R.string.state_success))
    }
    Column(
        Modifier.fillMaxWidth().testTag("state-" + state.name.lowercase())
            .clip(RoundedCornerShape(18.dp))
            .nxCard(18, glow = tint, glow2 = Nx.blue, selected = state == UiState.SUCCESS)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state == UiState.LOADING) Shimmer()
        NxTile(mark, tint, size = 46, glyph = 24)
        Label(title, 15, tint, FontWeight.SemiBold, align = TextAlign.Center)
        Label(detail, 12, Nx.muted, align = TextAlign.Center)
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
                    .background(Nx.raised.copy(alpha = alpha))
            )
        }
    }
}

/** An icon that is the only content of its control must describe itself. */
internal fun Modifier.iconLabel(description: String): Modifier =
    semantics { contentDescription = description }

/**
 * A collapsed panel for engine-level detail.
 *
 * A customer sees one honest sentence on the surface. Raw identifiers, scope
 * enums and byte counts live here, one tap away, so the screen stays readable
 * without hiding anything and without moving debug text into the primary view.
 */
@Composable
internal fun TechnicalDetails(
    rows: List<Pair<Copy, String>>,
    modifier: Modifier = Modifier,
    tag: String = "technical-details",
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).nxCard(16, glow = Nx.violet, glow2 = Nx.blue),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = TOUCH_TARGET)
                .clickable(role = Role.Button) { open = !open }
                .testTag(tag).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Glyph(Mark.GEAR, Modifier.size(16.dp), Nx.muted)
            Label(
                Copy(R.string.technical_details), 12, Nx.muted, FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Glyph(
                Mark.NEXT, Modifier.size(14.dp).rotate(if (open) 90f else 0f),
                Nx.muted,
            )
        }
        if (open) {
            Column(
                Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rows.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth()) {
                        Label(label, 12, Nx.muted, modifier = Modifier.weight(.42f))
                        Label(
                            Copy.runtime(value), 12, Nx.text,
                            modifier = Modifier.weight(.58f), maxLines = 3,
                        )
                    }
                }
            }
        }
    }
}
