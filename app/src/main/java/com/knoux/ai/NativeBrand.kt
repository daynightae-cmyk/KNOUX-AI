package com.knoux.ai

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The KNOUX brand layer for the utility shell.
 *
 * Every colour here is one of the four logo tones - cobalt blue, cyan, violet
 * and magenta. Amber, red and green stay reserved for a *measured* warning or
 * success; they are never used as service decoration. That rule is what makes
 * the hub read as one product instead of eight tinted cards.
 */
internal object BrandTone {
    val blue: Color get() = NativePalette.blue
    val cyan: Color get() = NativePalette.cyan
    val violet: Color get() = NativePalette.violet
    val magenta: Color get() = NativePalette.magenta

    /** Violet -> magenta -> cyan: the logo's own order, for one thin line. */
    val signature: List<Color> get() = listOf(violet, magenta, cyan)

    /**
     * One service, one tone. The mapping is stable across the hub, the tools
     * list and the section title so a customer learns the colour once.
     */
    fun forTag(tag: String): Color = when {
        tag.contains("duplicate") -> magenta
        tag.contains("clean") || tag.contains("residue") -> violet
        tag.contains("privacy") -> cyan
        tag.contains("battery") || tag.contains("report") -> cyan
        tag.contains("ai") || tag.contains("optimize") -> magenta
        tag.contains("storage") || tag.contains("app") -> blue
        else -> blue
    }
}

/**
 * A service icon on its own tinted plate.
 *
 * The plate is a gradient rather than one flat wash: a lit top edge, a quiet
 * bottom and a hairline in the service tone. It keeps the glyph legible without
 * a glow and gives every service a recognisable colour without painting the
 * whole card.
 */
@Composable
internal fun ServiceIconPlate(
    mark: Mark,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 19.dp,
) {
    val shape = RoundedCornerShape(size * .34f)
    Box(
        modifier.size(size).clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = .26f), tint.copy(alpha = .075f)),
                )
            )
            .border(1.dp, tint.copy(alpha = .34f), shape)
            .drawWithContent {
                drawContent()
                drawLine(
                    Brush.horizontalGradient(
                        listOf(tint.copy(alpha = .55f), tint.copy(alpha = .10f), Color.Transparent),
                    ),
                    Offset(this.size.width * .16f, 1.dp.toPx()),
                    Offset(this.size.width * .86f, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(mark, Modifier.size(iconSize), tint)
    }
}

/** One thin brand line. Used only to separate, never as a border on a card. */
@Composable
internal fun BrandHairline(modifier: Modifier = Modifier, alpha: Float = .32f) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    NativePalette.violet.copy(alpha = alpha * .7f),
                    NativePalette.cyan.copy(alpha = alpha),
                    Color.Transparent,
                ),
            ),
            Offset(0f, size.height / 2f),
            Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
        )
    }
}

/**
 * The section title of every native screen.
 *
 * One 3dp logo-gradient bar, the name, then one muted sentence. The bar is the
 * only ornament: it gives the ten sections a shared opening beat without
 * pushing a second header into the content.
 */
@Composable
internal fun NativeScreenTitle(
    title: Copy,
    subtitle: Copy,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(BrandTone.signature))
            )
            Label(
                title, 21, NativePalette.text, FontWeight.Bold,
                modifier = Modifier.weight(1f).testTag(tag),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Label(
            subtitle, 12, NativePalette.muted,
            modifier = Modifier.padding(start = 13.dp), maxLines = 3,
        )
    }
}

/**
 * A press that answers: the surface compresses by 1.5% for 90ms. The ripple is
 * kept for the platform's own feedback; this only adds the tactile part a
 * customer feels on a phone.
 */
@Composable
internal fun Modifier.brandClickable(
    enabled: Boolean = true,
    pressedScale: Float = .985f,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) pressedScale else 1f,
        tween(90, easing = FastOutSlowInEasing), label = "press scale",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/**
 * A quiet entrance: 10dp rise and a fade, staggered by the block's position.
 * Finite, 300ms, and skipped entirely when the customer turns motion off.
 */
@Composable
internal fun Modifier.enterRise(index: Int = 0, enabled: Boolean = true): Modifier {
    val motion = LocalPreviewMotion.current && enabled
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val progress by animateFloatAsState(
        if (motion && !entered) 0f else 1f,
        tween(
            durationMillis = 300,
            delayMillis = if (motion) (index * 40).coerceAtMost(240) else 0,
            easing = FastOutSlowInEasing,
        ),
        label = "enter rise",
    )
    val lift = with(LocalDensity.current) { 10.dp.toPx() }
    return this.graphicsLayer {
        alpha = if (motion) progress else 1f
        translationY = if (motion) (1f - progress) * lift else 0f
    }
}
