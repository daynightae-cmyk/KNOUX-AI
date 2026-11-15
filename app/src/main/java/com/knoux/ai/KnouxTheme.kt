package com.knoux.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection

/**
 * The only colour vocabulary the interface is allowed to use.
 *
 * Two identity colours carry the brand: violet as the base and cyan as the
 * accent. Pink is reserved for the single primary action of a screen, and gold
 * belongs to the Pro entitlement alone. Everything else is a neutral surface or
 * a semantic state, so no screen invents its own colour.
 */
internal object KnouxColors {
    val background = Nx.ink
    val surface = Nx.card
    val surfaceVariant = Nx.raised

    /** Identity base. */
    val primary = Nx.violet
    val onPrimary = Nx.inkDeep

    /** Identity accent. */
    val accent = Nx.cyan

    val success = Nx.green
    val warning = Nx.amber
    val danger = Nx.rose

    val textPrimary = Nx.text
    val textSecondary = Nx.muted
    val border = Nx.border

    /** The one primary button per screen. */
    val action = Nx.blue
    val onAction = Nx.inkDeep

    /** Pro entitlement only. */
    val pro = Nx.amber
}

/**
 * IBM Plex Sans Arabic in the three weights the interface is allowed to use.
 * The family covers Latin as well, so English and Arabic share one texture
 * instead of mixing a decorative Latin face with a system fallback.
 */
internal val KnouxFont = FontFamily(
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_bold, FontWeight.Bold),
)

/**
 * Collapses every requested weight onto one of the three real weights. Asking
 * for Black or ExtraLight therefore renders a true face instead of a synthetic
 * one, and the interface keeps a single rhythm.
 */
internal fun weightOf(requested: FontWeight): FontWeight = when {
    requested.weight >= 650 -> FontWeight.Bold
    requested.weight >= 500 -> FontWeight.SemiBold
    else -> FontWeight.Normal
}

/** Smallest size any customer-visible text may use. */
internal const val MIN_TEXT_SIZE = 12

@Composable
internal fun KnouxTheme(arabic: Boolean, motion: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = KnouxColors.primary,
            onPrimary = KnouxColors.onPrimary,
            secondary = KnouxColors.accent,
            background = KnouxColors.background,
            surface = KnouxColors.surface,
            onSurface = KnouxColors.textPrimary,
            error = KnouxColors.danger,
        )
    ) {
        CompositionLocalProvider(
            LocalArabic provides arabic,
            LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            LocalPreviewMotion provides motion,
            content = content,
        )
    }
}
