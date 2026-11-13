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
    val background = Color(0xFF05020E)
    val surface = Color(0xFF0E0A22)
    val surfaceVariant = Color(0xFF171136)

    /** Identity base. */
    val primary = Color(0xFFB040FF)
    val onPrimary = Color(0xFF0B0016)

    /** Identity accent. */
    val accent = Color(0xFF00E5F5)

    val success = Color(0xFF00E8A8)
    val warning = Color(0xFFFFB020)
    val danger = Color(0xFFFF3B5C)

    val textPrimary = Color(0xFFF6F2FF)
    val textSecondary = Color(0xFFBDAFD8)
    val border = Color(0xFF2A2148)

    /** The one primary button per screen. */
    val action = Color(0xFFFF2ED0)
    val onAction = Color(0xFF17001A)

    /** Pro entitlement only. */
    val pro = Color(0xFFFFCC33)
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
