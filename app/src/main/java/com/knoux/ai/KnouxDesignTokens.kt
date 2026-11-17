package com.knoux.ai

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * ONE KNOUX IDENTITY — the single colour authority for the whole application.
 *
 * Before this file the product spoke two different visual languages: the
 * approved neutron authentication surface sat on deep ink (#07030F) with
 * magenta/violet/cyan light, while every screen after login fell back to a
 * separate navy-blue palette (#05091E / #090A1E). The customer crossed a border
 * at the moment of signing in, and the app looked like another product.
 *
 * Everything below is sampled from the owner's approved references
 * (2026-11-16/17): the near-black field, the violet glass panel, the field
 * gradient of the inputs, and the one magenta → violet → blue → cyan action.
 * Splash, onboarding, authentication and the whole signed-in shell now read
 * from these exact tokens, so no surface can drift away again.
 *
 * Rules that stay intact:
 *  - green / amber / rose remain MEASURED-STATE colours and are never used for
 *    decoration, so honesty outranks beauty.
 *  - gold belongs to the Pro entitlement alone.
 *  - a service may still own its own hue (World), but it lives inside this
 *    field, on these panels, under this light.
 */
internal object Knx {

    // ── Canvas ───────────────────────────────────────────────────────────────
    /** The approved field: a near-black violet, not navy. */
    val ink = Color(0xFF07030F)
    val inkDeep = Color(0xFF04010B)
    val aurora = Color(0xFF120A2E)

    // ── Glass surfaces (approved login panel) ────────────────────────────────
    val panelTop = Color(0xFF1C1044)
    val panelMid = Color(0xFF100933)
    val panelBottom = Color(0xFF0B0B2A)

    /** The inset field of an input / secondary pill. */
    val fieldTop = Color(0xFF241352)
    val fieldMid = Color(0xFF140A33)
    val fieldBottom = Color(0xFF0D1336)

    // ── Identity ─────────────────────────────────────────────────────────────
    val violet = Color(0xFF8938FF)
    val magenta = Color(0xFFE62BFF)
    val cyan = Color(0xFF00DEFF)
    val blue = Color(0xFF1968FF)
    val gold = Color(0xFFF5B942)

    // ── Measured states (semantic only) ──────────────────────────────────────
    val green = Color(0xFF2ED3A4)
    val amber = Color(0xFFF3B23C)
    val rose = Color(0xFFF24E6A)

    // ── Type ─────────────────────────────────────────────────────────────────
    val text = Color(0xFFF7F2FF)
    val muted = Color(0xFFB3A2E0)
    val faint = Color(0xFF7C6BA6)

    // ── Lines ────────────────────────────────────────────────────────────────
    val border = Color(0xFF3A2568)
    val borderLit = Color(0xFF6B45C8)

    // ── The shared brushes ───────────────────────────────────────────────────

    /** Every panel stands on this: the exact violet glass of the login card. */
    val panel: Brush = Brush.verticalGradient(listOf(panelTop, panelMid, panelBottom))

    /** The lit rim of a panel: violet into blue into cyan, as approved. */
    val panelBorder: Brush = Brush.linearGradient(
        listOf(violet.copy(alpha = .85f), blue.copy(alpha = .55f), cyan.copy(alpha = .75f)),
    )

    /**
     * The same rim, one stop quieter, for lists that repeat many cards. It keeps
     * the violet language without turning every row into a lit hero.
     */
    val panelBorderQuiet: Brush = Brush.linearGradient(
        listOf(violet.copy(alpha = .42f), blue.copy(alpha = .26f), cyan.copy(alpha = .32f)),
    )

    /** The inset field of an input / secondary pill. */
    val field: Brush = Brush.horizontalGradient(listOf(fieldTop, fieldMid, fieldBottom))

    val fieldBorder: Brush = Brush.horizontalGradient(
        listOf(violet.copy(alpha = .72f), blue.copy(alpha = .48f), cyan.copy(alpha = .58f)),
    )

    /** The single call-to-action: magenta → violet → blue → cyan. */
    val cta: Brush = Brush.horizontalGradient(
        listOf(magenta, Color(0xFFC22BFF), violet, blue, cyan),
    )

    /** A quiet halo for a lit element, used sparingly. */
    fun glow(tint: Color, radius: Float): Brush = Brush.radialGradient(
        listOf(tint.copy(alpha = .22f), Color.Transparent),
        center = Offset.Zero,
        radius = radius,
    )
}
