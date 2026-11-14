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

/**
 * Figma 2:2, 12:45 and 12:147 — native utility palette.
 *
 * The base is the measured Figma field (#090A1E), the panels are one elevation
 * step lighter, and blue carries the primary control. Violet, magenta and cyan
 * come from the KNOUX logo and are reserved for a thin ambient wave and one
 * critical data accent — they never outline every card.
 *
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
    val magenta = Color(0xFFE05BD3)
    val amber = Color(0xFFE8B23A)
    val green = Color(0xFF3ECF8E)
    val red = Color(0xFFE5484D)
    val text = Color(0xFFF4F7FF)
    val muted = Color(0xFFA5B4D0)
}

/** Vertical rhythm shared by every native screen (Figma 12/16/20 ladder). */
internal object NativeSpace {
    val edge = 20.dp          // Figma content margin, x20 at the 360dp reference
    val tight = 8.dp
    val row = 12.dp
    val block = 16.dp
    val section = 20.dp
    val tile = 92.dp          // Figma service tile height at 360dp reference
    val gutter = 12.dp        // Figma two-column gutter
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
    val shape = RoundedCornerShape(16.dp)
    val native = LocalNativeApp.current
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(
                if (native) {
                    if (variant) NativePalette.raised else NativePalette.card
                } else if (variant) KnouxColors.surfaceVariant else KnouxColors.surface
            )
            .border(1.dp, if (native) NativePalette.border else KnouxColors.border, shape),
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
    val shape = RoundedCornerShape(50)
    val native = LocalNativeApp.current
    val ink = if (enabled) {
        if (native) NativePalette.text else Light.white
    } else KnouxColors.textSecondary
    val fill = when {
        !enabled -> Modifier.background(if (native) NativePalette.raised else KnouxColors.surfaceVariant)
        native -> Modifier.background(NativePalette.blue)
        else -> Modifier.primarySurface(null, null)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = TOUCH_TARGET).testTag(tag)
            .clip(shape).then(fill)
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
internal fun SecondaryAction(action: Action, onClick: () -> Unit, tag: String = "secondary-action") {
    val shape = RoundedCornerShape(14.dp)
    val native = LocalNativeApp.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = TOUCH_TARGET).testTag(tag)
            .clip(shape)
            .background(if (native) NativePalette.card else KnouxColors.surface)
            .border(1.dp, if (native) NativePalette.border else KnouxColors.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Glyph(action.icon, Modifier.size(18.dp), if (native) NativePalette.cyan else KnouxColors.accent)
        Spacer(Modifier.width(8.dp))
        Label(
            action.label, 14, if (native) NativePalette.text else KnouxColors.textPrimary,
            FontWeight.SemiBold, align = TextAlign.Center,
        )
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
        UiState.EMPTY -> Triple(KnouxColors.textSecondary, Mark.BOX, Copy(R.string.state_empty))
        UiState.ERROR -> Triple(KnouxColors.danger, Mark.WARN, Copy(R.string.state_error))
        UiState.SUCCESS -> Triple(KnouxColors.success, Mark.CHECK, Copy(R.string.state_success))
    }
    val native = LocalNativeApp.current
    val tone = when {
        !native -> tint
        state == UiState.LOADING -> NativePalette.blue
        state == UiState.ERROR -> NativePalette.red
        state == UiState.SUCCESS -> NativePalette.green
        else -> NativePalette.muted
    }
    Column(
        Modifier.fillMaxWidth().testTag("state-" + state.name.lowercase())
            .clip(RoundedCornerShape(16.dp))
            .background(if (native) NativePalette.card else KnouxColors.surface)
            .border(1.dp, if (native) NativePalette.border else KnouxColors.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state == UiState.LOADING) Shimmer()
        Glyph(mark, Modifier.size(28.dp), tone)
        Label(title, 15, tone, FontWeight.SemiBold, align = TextAlign.Center)
        Label(detail, 13, if (native) NativePalette.muted else KnouxColors.textSecondary, align = TextAlign.Center)
        onRetry?.let { retry ->
            SecondaryAction(Action(Copy(R.string.action_retry), Mark.SYNC), retry)
        }
    }
}

/** A quiet skeleton line, used while a real measurement is running. */
@Composable
internal fun Shimmer() {
    val motion = LocalPreviewMotion.current
    val native = LocalNativeApp.current
    val alpha = if (motion) {
        val transition = rememberInfiniteTransition(label = "skeleton")
        transition.animateFloat(
            0.18f, 0.42f,
            infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "skeleton",
        ).value
    } else 0.30f
    val track = if (native) NativePalette.raised else KnouxColors.surfaceVariant
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1f, .8f, .6f).forEach { fraction ->
            Box(
                Modifier.fillMaxWidth(fraction).height(10.dp).clip(RoundedCornerShape(50))
                    .background(track.copy(alpha = alpha))
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
    val native = LocalNativeApp.current
    val quiet = if (native) NativePalette.muted else KnouxColors.textSecondary
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(if (native) NativePalette.card else KnouxColors.surface)
            .border(1.dp, if (native) NativePalette.border else KnouxColors.border, shape),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = TOUCH_TARGET)
                .clickable(role = Role.Button) { open = !open }
                .testTag(tag).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Glyph(Mark.GEAR, Modifier.size(16.dp), quiet)
            Label(
                Copy(R.string.technical_details), 13, quiet, FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Glyph(
                Mark.NEXT, Modifier.size(14.dp).rotate(if (open) 90f else 0f), quiet,
            )
        }
        if (open) {
            Column(
                Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rows.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth()) {
                        Label(label, 12, quiet, modifier = Modifier.weight(.42f))
                        Label(
                            Copy.runtime(value), 12,
                            if (native) NativePalette.text else KnouxColors.textPrimary,
                            modifier = Modifier.weight(.58f), maxLines = 3,
                        )
                    }
                }
            }
        }
    }
}
