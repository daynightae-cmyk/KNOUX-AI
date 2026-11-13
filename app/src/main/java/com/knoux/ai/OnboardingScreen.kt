package com.knoux.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// KNOUX AI — three-chapter onboarding.
//
// Contract sources: references 01 (awakening), 02 (ecosystem), 03 (creator) and
// 04 as the English locale equivalent — never a fourth chapter.
//
// Geometry rules enforced here:
//  * WindowInsets.safeDrawing is applied exactly once, on the root column, so the
//    Skip/Back labels can never sit under the Android clock, Wi-Fi or cutout.
//  * Every interactive control is at least 48dp tall even though the glyph or
//    label inside is small.
//  * Content is allowed to scroll adaptively on short viewports / large font
//    scale instead of clipping the artwork or the call to action.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun OnboardingScreen(onComplete: () -> Unit, previewChapter: Int? = null) {
    val context = LocalContext.current
    var chapter by remember { mutableIntStateOf(previewChapter?.coerceIn(0, 2) ?: 0) }

    // Completing or skipping persists the same first-run flag the previous
    // implementation used, so existing installs keep their state.
    // A debug capture route (previewChapter) never writes the flag: photographs
    // of a chapter must not consume the owner's real first-run experience.
    val complete: () -> Unit = {
        if (previewChapter == null) {
            context.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("knoux_onboarding_done", true).apply()
        }
        onComplete()
    }

    val goNext: () -> Unit = {
        if (chapter < 2) chapter++ else complete()
    }

    // Back walks the chapters in reverse. On chapter 1 there is no chapter to
    // return to, so it behaves exactly like Skip — documented, and it can never
    // trap the user on the intro.
    val goBack: () -> Unit = {
        if (chapter > 0) chapter-- else complete()
    }

    BackHandler(enabled = true) { goBack() }

    Box(Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xff1a0b4d), Color(0xff0b0426), Color(0xff03000c))
                )
            )
    ) {
        Atmosphere()

        Column(Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            ChapterBar(onBack = goBack, onSkip = complete)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                Crossfade(targetState = chapter, animationSpec = tween(420), label = "chapter") { index ->
                    when (index) {
                        0 -> ChapterAwakening()
                        1 -> ChapterEcosystem()
                        else -> ChapterCreator(onEnter = complete)
                    }
                }
            }

            ChapterFooter(chapter, onNext = goNext)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared chrome
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Reference: Skip sits on the trailing edge and Back on the leading edge, both
 * as quiet text labels. The 48dp hit box is larger than the visible label.
 */
@Composable
private fun ChapterBar(onBack: () -> Unit, onSkip: () -> Unit) {
    Row(Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuietTextAction(label = (Copy(R.string.k_back_2)).localized(),
            leadingGlyph = Mark.BACK,
            tag = "onboarding-back",
            onClick = onBack
        )
        QuietTextAction(label = (Copy(R.string.k_skip)).localized(),
            tag = "onboarding-skip",
            onClick = onSkip
        )
    }
}

@Composable
private fun QuietTextAction(label: String,
    tag: String,
    leadingGlyph: Mark? = null,
    onClick: () -> Unit
) {
    Row(Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingGlyph != null) {
            // In RTL the control still sits on the leading (right) edge, but a
            // "back" arrow must then point right; the raw vector only points left.
            val flip = if (LocalArabic.current && leadingGlyph == Mark.BACK) 180f else 0f
            Glyph(leadingGlyph, Modifier.size(15.dp).rotate(flip), Mist.copy(alpha = .92f))
        }
        Label(label, 14, Mist.copy(alpha = .92f), FontWeight.Medium, maxLines = 1)
    }
}

/** Bottom progress dots plus the single chapter call to action. */
@Composable
private fun ColumnScope.ChapterFooter(chapter: Int, onNext: () -> Unit) {
    Column(Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 6.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProgressDots(chapter)

        when (chapter) {
            0 -> GradientCta((Copy(R.string.k_explore_knoux)).localized(), onNext, "onboarding-cta-1")
            1 -> GradientCta((Copy(R.string.k_next)).localized(), onNext, "onboarding-cta-2")
            else -> GradientCta((Copy(R.string.k_start_your_journey_with_knoux_ai)).localized(), onNext, "onboarding-cta-3")
        }
    }
}

@Composable
private fun ProgressDots(active: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val on = index == active
            val width by animateDpAsState(if (on) 26.dp else 8.dp, tween(260), label = "dot width")
            Box(Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (on) Brush.horizontalGradient(listOf(Cyan, Color(0xff5ce8ff)))
                        else Brush.horizontalGradient(listOf(Mist.copy(alpha = .34f), Mist.copy(alpha = .34f)))
                    )
                    .testTag("onboarding-dot-$index")
            )
        }
    }
}

/**
 * The signature gradient pill. Label first, chevron second, so RTL mirrors the
 * chevron to the trailing edge exactly like reference 01.
 */
@Composable
private fun GradientCta(label: String, onClick: () -> Unit, tag: String? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .972f else 1f, tween(120), label = "cta scale")
    val motion = LocalPreviewMotion.current
    val pulse = if (motion) {
        val transition = rememberInfiniteTransition(label = "cta pulse")
        transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "cta glow"
        )
    } else null
    val shimmer = if (motion) {
        val transition = rememberInfiniteTransition(label = "cta shimmer")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "cta travel")
    } else null

    Row(Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .buttonBloom(true, pulse)
            .clip(RoundedCornerShape(30.dp))
            .primarySurface(shimmer, pulse)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Label(label, 17, Chalk, FontWeight.ExtraBold, align = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.width(9.dp))
        // The chevron tracks the reading direction: it points right in LTR and
        // left in RTL, so a mirrored layout never shows a "next" arrow aimed back.
        Glyph(Mark.NEXT, Modifier.size(17.dp).rotate(if (LocalArabic.current) 180f else 0f), Chalk, true)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chapter I — The Awakening (reference 01)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChapterAwakening() {
    Column(Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .testTag("onboarding-chapter-1"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLockup(compact = true)
        Spacer(Modifier.height(6.dp))

        HeroOrb(Modifier.fillMaxWidth().aspectRatio(1.02f), glyphFraction = .50f)

        Spacer(Modifier.height(10.dp))

        GradientHeadline(lead = (Copy(R.string.k_welcome_to)).localized(),
            accent = "KNOUX AI",
            size = 27
        )

        Spacer(Modifier.height(14.dp))

        OutlinePill((Copy(R.string.k_intelligence_beautifully_engineered)).localized())

        Spacer(Modifier.height(16.dp))

        Label((Copy(R.string.k_begin_your_journey_into_a_world_that_is_smarter_stronger)).localized(),
            13, Mist.copy(alpha = .82f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            align = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chapter II — The Ecosystem (reference 02)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Ten services arranged as the reference draws them: two rows of inset cards at
 * the top and bottom, three rows pinned to the screen edges, and the illuminated
 * intelligence nucleus holding the centre. The list is derived from the live
 * `Page` entries so every tile keeps a valid route identity.
 */
private data class OrbitSlot(val page: Page, val fx: Float, val fy: Float, val tint: Color)

private val ORBIT_SLOTS = listOf(OrbitSlot(Page.PRIVACY, 0.285f, 0.100f, Color(0xffa86bff)),
    OrbitSlot(Page.DUPLICATES, 0.715f, 0.100f, Color(0xffffb02e)),
    OrbitSlot(Page.HOME, 0.140f, 0.300f, Color(0xff3fa9ff)),
    OrbitSlot(Page.BATTERY, 0.860f, 0.300f, Color(0xff22e6a4)),
    OrbitSlot(Page.RESIDUE, 0.140f, 0.500f, Color(0xffc46bff)),
    OrbitSlot(Page.STORAGE, 0.860f, 0.500f, Color(0xff4a7dff)),
    OrbitSlot(Page.REPORTS, 0.140f, 0.700f, Color(0xff3fd0ff)),
    OrbitSlot(Page.APPS, 0.860f, 0.700f, Color(0xffb06bff)),
    OrbitSlot(Page.SCAN, 0.315f, 0.900f, Color(0xffb46bff)),
    OrbitSlot(Page.OPTIMIZE, 0.685f, 0.900f, Color(0xffffc247))
)

private fun orbitGlyph(page: Page): Mark = when (page) {
    Page.HOME -> Mark.MEMORY
    Page.OPTIMIZE -> Mark.CHIP
    Page.SCAN -> Mark.SEARCH
    else -> page.hero
}

@Composable
private fun ChapterEcosystem() {
    Column(Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .testTag("onboarding-chapter-2"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLockup(compact = true)

        Spacer(Modifier.height(2.dp))

        Text((Copy(R.string.k_one_intelligent_ecosystem)).localized(),
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(fontSize = 25.sp,
                lineHeight = 30.sp,
                fontFamily = if (LocalArabic.current) FontFamily.Default else DisplayFace,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                color = Chalk
            )
        )
        Spacer(Modifier.height(4.dp))
        Label((Copy(R.string.k_t_10_intelligent_services_one_unified_platform)).localized(),
            13, Mist.copy(alpha = .84f),
            modifier = Modifier.fillMaxWidth(),
            align = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        ServiceOrbit()

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ServiceOrbit() {
    val motion = LocalPreviewMotion.current
    BoxWithConstraints(Modifier
            .fillMaxWidth()
            .aspectRatio(0.70f)
    ) {
        val w = maxWidth
        val h = maxHeight
        val cardW = w * 0.262f
        val cardH = cardW * 1.04f
        val centre = Offset(w.value * 0.5f, h.value * 0.5f)

        val rise = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            if (motion) rise.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) else rise.snapTo(1f)
        }

        // Energy filaments from every tile into the nucleus.
        Canvas(Modifier.fillMaxSize()) {
            val unit = 1f
            ORBIT_SLOTS.forEach { slot ->
                val from = Offset(w.value * slot.fx * unit, h.value * slot.fy * unit)
                val path = Path().apply {
                    moveTo(from.x, from.y)
                    val midX = (from.x + centre.x) / 2f
                    val midY = (from.y + centre.y) / 2f
                    quadraticTo(from.x, midY, midX, midY)
                }
                drawPath(path = path,
                    brush = Brush.linearGradient(listOf(slot.tint.copy(alpha = .55f), Cyan.copy(alpha = .10f)),
                        from, centre
                    ),
                    style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        ORBIT_SLOTS.forEach { slot ->
            Box(Modifier
                    .offset(x = w * slot.fx - cardW / 2, y = h * slot.fy - cardH / 2)
                    .size(cardW, cardH)
                    .graphicsLayer { alpha = rise.value; translationY = (1f - rise.value) * 14f }
            ) {
                ServiceTile(slot)
            }
        }

        Nucleus(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ServiceTile(slot: OrbitSlot) {
    val shape = RoundedCornerShape(17.dp)
    val motion = LocalPreviewMotion.current
    val glow = if (motion) {
        val transition = rememberInfiniteTransition(label = "tile")
        transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(2400 + (slot.page.ordinal % 5) * 220, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "tile glow"
        )
    } else null
    val breath = glow?.value ?: .4f

    Box(Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(slot.tint.copy(alpha = .20f + .10f * breath),
                        Color(0xff150c37).copy(alpha = .92f),
                        Color(0xff0b0524)
                    )
                ),
                shape
            )
            .border(1.dp,
                Brush.linearGradient(listOf(slot.tint.copy(alpha = .90f), slot.tint.copy(alpha = .22f), Color.White.copy(alpha = .30f))
                ),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(slot.tint.copy(alpha = .26f + .16f * breath), Color.Transparent),
                    Offset(size.width * .5f, size.height * .22f), size.width * .62f
                ),
                size.width * .62f, Offset(size.width * .5f, size.height * .22f)
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Glyph(orbitGlyph(slot.page), Modifier.size(24.dp), slot.tint, true)
            Label(slot.page.title.localized(), 9, Chalk, FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                align = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/** The illuminated "intelligence nucleus" at the centre of the orbit. */
@Composable
private fun Nucleus(modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    val transition = rememberInfiniteTransition(label = "nucleus")
    val pulse = if (motion) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nucleus pulse"
    ) else null
    val spin = if (motion) transition.animateFloat(0f, 360f,
        infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "nucleus spin"
    ) else null
    val breath = pulse?.value ?: 0f

    // The caller's modifier MUST be applied: it carries the Alignment.Center that
    // seats the nucleus in the middle of the orbit. Dropping it left the nucleus at
    // the container's default alignment, which in RTL puts it top-right, on top of
    // the right-hand service tiles.
    Box(modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * .34f
            drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha = .34f + .14f * breath), Cyan.copy(alpha = .12f), Color.Transparent),
                    c, r * 1.75f
                ), r * 1.75f, c
            )
            drawCircle(Brush.radialGradient(listOf(Color(0xff3a1a86).copy(alpha = .95f), Color(0xff150a3c), Color(0xff06010f)),
                    c, r * 1.2f
                ), r, c
            )
            drawCircle(Brush.sweepGradient(listOf(Cyan, Violet, Pink, Color(0xff7a5bff), Cyan)),
                r, c, style = Stroke(1.5.dp.toPx())
            )
            rotate(spin?.value ?: 0f, c) {
                drawOval(Cyan.copy(alpha = .55f),
                    topLeft = Offset(c.x - r * 1.42f, c.y - r * .30f),
                    size = Size(r * 2.84f, r * .60f),
                    style = Stroke(1.2.dp.toPx())
                )
            }
            // Pedestal
            val py = c.y + r * 1.30f
            drawOval(Brush.radialGradient(listOf(Cyan.copy(alpha = .55f + .2f * breath), Violet.copy(alpha = .26f), Color.Transparent),
                    Offset(c.x, py), r * 1.25f
                ),
                topLeft = Offset(c.x - r * 1.25f, py - r * .22f),
                size = Size(r * 2.5f, r * .44f)
            )
        }
        Glyph(Mark.K, Modifier.size(62.dp), Chalk, true)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chapter III — The Creator's Signature (reference 03)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChapterCreator(onEnter: () -> Unit) {
    val context = LocalContext.current

    Column(Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .testTag("onboarding-chapter-3"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLockup(compact = true)

        Spacer(Modifier.height(8.dp))

        HeroOrb(Modifier.fillMaxWidth(.62f).aspectRatio(0.92f), glyphFraction = .46f, pedestal = true)

        Spacer(Modifier.height(16.dp))

        DividedCaption((Copy(R.string.k_a_knoux_product)).localized())

        Spacer(Modifier.height(6.dp))

        Text("KNOUX AI",
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(fontSize = 38.sp,
                lineHeight = 42.sp,
                fontFamily = DisplayFace,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                brush = Brush.horizontalGradient(listOf(Color(0xfff0e6ff), Color(0xffc9a8ff), Color(0xffb06bff), Color(0xffe07bff))
                )
            )
        )

        Spacer(Modifier.height(10.dp))

        Text(buildAnnotatedString {
                withStyle(SpanStyle(color = Chalk)) { append((Copy(R.string.k_a_vision_of_engineering)).localized()) }
                withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(Color(0xffff5cd6), Color(0xffc65bff), Color(0xff9b8bff))
                        )
                    )
                ) { append((Copy(R.string.k_an_experience_built_for_you)).localized()) }
            },
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(fontSize = 21.sp,
                lineHeight = 27.sp,
                fontFamily = if (LocalArabic.current) FontFamily.Default else DisplayFace,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
            )
        )

        Spacer(Modifier.height(8.dp))

        Label((Copy(R.string.k_discover_the_innovation_behind_knoux_ai)).localized(),
            13, Mist.copy(alpha = .82f),
            modifier = Modifier.fillMaxWidth(),
            align = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        FounderCard(onOpenSite = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://knoux.store/")))
            }
        })

        Spacer(Modifier.height(18.dp))
    }
}

/**
 * Luxury glass founder plate. Bilingual by design, so the row direction is
 * pinned to LTR: Arabic always reads on the right, English on the left, matching
 * reference 03 in both app locales. The Arabic run keeps its own bidi shaping.
 */
@Composable
private fun FounderCard(onOpenSite: () -> Unit) {
    val shimmer = if (LocalPreviewMotion.current) {
        val transition = rememberInfiniteTransition(label = "founder")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "founder rim")
    } else null

    val shape = RoundedCornerShape(24.dp)
    Box(Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xff2a1150).copy(alpha = .94f), Color(0xff120735).copy(alpha = .96f), Color(0xff1b0a38))
                ),
                shape
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            val travel = (shimmer?.value ?: 0f) * size.width * 1.5f - size.width * .25f
            drawRoundRect(Brush.linearGradient(listOf(Color.Transparent, Gold.copy(alpha = .30f), Color.Transparent),
                    start = Offset(travel - size.width * .22f, 0f),
                    end = Offset(travel + size.width * .22f, size.height)
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
            )
            drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .18f), Color.Transparent),
                    Offset(0f, size.height * .5f), size.width * .40f
                ),
                0f, Offset(0f, size.height * .5f)
            )
        }

        Box(Modifier
                .matchParentSize()
                .border(width = 1.4.dp,
                    brush = Brush.linearGradient(listOf(Gold.copy(alpha = .95f), Color(0xffb06bff),
                            Color.White.copy(alpha = .85f), Gold.copy(alpha = .80f)
                        )
                    ),
                    shape = shape
                )
        )

        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Label("Eng. Sadek Elgazar", 13, Chalk, FontWeight.ExtraBold, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Label("Founder & Software Engineer", 10, Mist.copy(alpha = .82f), maxLines = 2)
                    }
                    Box(Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Gold.copy(alpha = .90f), Color.Transparent)
                                )
                            )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Label("م. صادق الجزار", 13, Chalk, FontWeight.ExtraBold, maxLines = 1,
                            align = TextAlign.End)
                        Spacer(Modifier.height(2.dp))
                        Label("المؤسس ومهندس البرمجيات", 10, Mist.copy(alpha = .82f),
                            align = TextAlign.End, maxLines = 2)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ComposedCaption((Copy(R.string.k_founder_of_knoux)).localized())

            Spacer(Modifier.height(10.dp))

            // Website chip — a real, 48dp-tall interactive target.
            Row(Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClick = onOpenSite)
                    .testTag("onboarding-founder-site")
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Glyph(Mark.LINK, Modifier.size(15.dp), Gold)
                Label("knoux.store", 13, Chalk, FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable pieces
// ─────────────────────────────────────────────────────────────────────────────

/** "K knoux ai" — single line, always LTR, italic brand lockup with the lit underline. */
@Composable
private fun BrandLockup(compact: Boolean = false) {
    val wordSize = if (compact) 20 else 24
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Glyph(Mark.K, Modifier.size(if (compact) 26.dp else 30.dp), Chalk, true)
                Label("knoux", wordSize, Chalk, FontWeight.ExtraBold, maxLines = 1)
                Label("ai", wordSize, Color(0xffff5cf0), FontWeight.ExtraBold, maxLines = 1)
            }
            Canvas(Modifier.width(150.dp).height(7.dp)) {
                val path = Path().apply {
                    moveTo(4f, size.height * .42f)
                    cubicTo(size.width * .30f, size.height * .10f,
                        size.width * .58f, size.height * .88f,
                        size.width - 4f, size.height * .32f
                    )
                }
                drawPath(path,
                    Brush.horizontalGradient(listOf(Color(0xffff4ff0), Violet, Color(0xff5a6bff), Cyan)
                    ),
                    style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Large central K sphere with orbital ribbons and a lit pedestal. Used by
 * chapters 01 and 03.
 */
@Composable
private fun HeroOrb(modifier: Modifier = Modifier,
    glyphFraction: Float = .50f,
    pedestal: Boolean = true
) {
    val motion = LocalPreviewMotion.current
    val transition = rememberInfiniteTransition(label = "hero orb")
    val pulse = if (motion) transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb breath"
    ) else null
    val spin = if (motion) transition.animateFloat(0f, 360f,
        infiniteRepeatable(tween(19000, easing = LinearEasing)),
        label = "orb spin"
    ) else null
    val breath = pulse?.value ?: 0f

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * .48f)
            val r = size.minDimension * .34f

            drawCircle(Brush.radialGradient(listOf(Pink.copy(alpha = .20f + .10f * breath), Violet.copy(alpha = .16f), Cyan.copy(alpha = .08f), Color.Transparent),
                    c, r * 1.85f
                ), r * 1.85f, c
            )

            // Glass sphere: dark interior with a violet floor and cyan shoulder.
            drawCircle(Brush.radialGradient(0f to Violet.copy(alpha = .26f),
                    .55f to Color(0xff1c0d4a).copy(alpha = .92f),
                    1f to Color(0xff05010f),
                    center = Offset(c.x - r * .30f, c.y - r * .34f),
                    radius = r * 1.30f
                ), r, c
            )

            drawCircle(Brush.sweepGradient(listOf(Cyan, Color(0xff7a5bff), Pink, Violet, Cyan)
                ), r, c, style = Stroke(1.4.dp.toPx())
            )

            // Top-left specular so the sphere reads as glass, not a flat disc.
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .30f), Color.Transparent),
                    Offset(c.x - r * .36f, c.y - r * .44f), r * .60f
                ), r * .60f, Offset(c.x - r * .36f, c.y - r * .44f)
            )

            // Orbit ribbons.
            rotate(spin?.value ?: 0f, c) {
                drawOval(Cyan.copy(alpha = .58f),
                    topLeft = Offset(c.x - r * 1.34f, c.y - r * .26f),
                    size = Size(r * 2.68f, r * .52f),
                    style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            rotate(-(spin?.value ?: 0f) * .72f, c) {
                drawOval(Pink.copy(alpha = .46f),
                    topLeft = Offset(c.x - r * 1.16f, c.y - r * .20f),
                    size = Size(r * 2.32f, r * .40f),
                    style = Stroke(1.1.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            if (pedestal) {
                val py = c.y + r * 1.24f
                drawOval(Brush.radialGradient(listOf(Cyan.copy(alpha = .58f + .18f * breath), Violet.copy(alpha = .28f), Color.Transparent),
                        Offset(c.x, py), r * 1.20f
                    ),
                    topLeft = Offset(c.x - r * 1.20f, py - r * .22f),
                    size = Size(r * 2.40f, r * .44f)
                )
                drawOval(Cyan.copy(alpha = .70f),
                    topLeft = Offset(c.x - r * .74f, py - r * .10f),
                    size = Size(r * 1.48f, r * .20f),
                    style = Stroke(1.2.dp.toPx())
                )
            }
        }

        Glyph(Mark.K, Modifier.size(maxWidth * glyphFraction), Chalk, true)
    }
}

/** White lead + gradient accent headline, a single bidi-correct text run. */
@Composable
private fun GradientHeadline(lead: String, accent: String, size: Int) {
    Text(buildAnnotatedString {
            withStyle(SpanStyle(color = Chalk)) { append(lead) }
            withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(Color(0xfff4ecff), Color(0xffb9dcff), Color(0xff4ff0ff))
                    )
                )
            ) { append(accent) }
        },
        modifier = Modifier.fillMaxWidth(),
        style = TextStyle(fontSize = size.sp,
            lineHeight = (size * 1.14f).sp,
            fontFamily = if (LocalArabic.current) FontFamily.Default else DisplayFace,
            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
        )
    )
}

/** Slim outlined capsule holding a single supporting line, e.g. «ذكاء صُمّم بإتقان.» */
@Composable
private fun OutlinePill(label: String) {
    val shape = RoundedCornerShape(22.dp)
    Box(Modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xff2b1560).copy(alpha = .55f), Color(0xff183a6b).copy(alpha = .45f))
                ),
                shape
            )
            .border(1.dp,
                Brush.horizontalGradient(listOf(Violet.copy(alpha = .95f), Cyan.copy(alpha = .85f), Color.White.copy(alpha = .35f))
                ),
                shape
            )
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Label(label, 15, Chalk, FontWeight.SemiBold, align = TextAlign.Center, maxLines = 1)
    }
}

/** Small caption flanked by tapered rules, e.g. "— أحد منتجات KNOUX —". */
@Composable
private fun DividedCaption(label: String) {
    Row(Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Rule(Modifier.weight(1f))
        Label(label, 12, Mist.copy(alpha = .78f), FontWeight.Medium, maxLines = 1)
        Rule(Modifier.weight(1f))
    }
}

@Composable
private fun Rule(modifier: Modifier = Modifier) {
    Box(modifier
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = .55f), Color.Transparent)
                )
            )
    )
}

/** Centre caption with gold rules and a hairline capsule, used inside the founder card. */
@Composable
private fun ComposedCaption(label: String) {
    Row(Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Rule(Modifier.weight(1f))
        Box(Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Gold.copy(alpha = .62f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Label(label, 11, Gold, FontWeight.Bold, maxLines = 1)
        }
        Rule(Modifier.weight(1f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Atmosphere — flowing luminous ribbons and fine particle dust.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Atmosphere() {
    val motion = LocalPreviewMotion.current
    val drift = if (motion) {
        val transition = rememberInfiniteTransition(label = "ribbon")
        transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(9000, easing = LinearEasing)),
            label = "ribbon drift"
        )
    } else null

    Canvas(Modifier.fillMaxSize()) {
        ribbon(colour = listOf(Pink.copy(alpha = .55f), Violet.copy(alpha = .30f), Color.Transparent),
            span = 1.35f,
            shift = drift?.value ?: 0f
        )
        ribbon(colour = listOf(Cyan.copy(alpha = .40f), Color(0xff3a6bff).copy(alpha = .22f), Color.Transparent),
            span = -1.15f,
            shift = 1f - (drift?.value ?: 0f)
        )
        // Soft corner bloom, both sides, as the references show.
        drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha = .30f), Color.Transparent),
                Offset(size.width * .06f, size.height * .06f), size.width * .70f
            ), size.width * .70f, Offset(size.width * .06f, size.height * .06f)
        )
        drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .14f), Color.Transparent),
                Offset(size.width * .96f, size.height * .88f), size.width * .62f
            ), size.width * .62f, Offset(size.width * .96f, size.height * .88f)
        )
        // Fine dust.
        val dust = listOf(Offset(.16f, .12f), Offset(.28f, .20f), Offset(.10f, .34f), Offset(.40f, .08f),
            Offset(.06f, .58f), Offset(.22f, .70f), Offset(.90f, .18f), Offset(.84f, .36f),
            Offset(.94f, .52f), Offset(.76f, .64f), Offset(.88f, .78f), Offset(.66f, .90f)
        )
        dust.forEachIndexed { i, p ->
            drawCircle(Color.White.copy(alpha = .10f + .05f * (i % 3)),
                1.2f.dp.toPx(),
                Offset(size.width * p.x, size.height * p.y)
            )
        }
    }
}

/** One broad luminous ribbon sweeping across a corner. */
private fun DrawScope.ribbon(colour: List<Color>, span: Float, shift: Float) {
    val w = size.width
    val h = size.height
    val startY = if (span > 0) -h * .04f + h * .05f * shift else h * 1.02f - h * .05f * shift
    val path = Path().apply {
        moveTo(-w * .10f, startY)
        cubicTo(w * .28f, startY + h * .12f * span,
            w * .42f, startY + h * .20f * span,
            w * .60f, startY + h * .10f * span
        )
        cubicTo(w * .82f, startY - h * .02f * span,
            w * .50f, startY + h * .26f * span,
            w * 1.10f, startY + h * .16f * span
        )
    }
    drawPath(path, Brush.linearGradient(colour), style = Stroke(34.dp.toPx(), cap = StrokeCap.Round))
    drawPath(path, Brush.linearGradient(colour.map { it.copy(alpha = it.alpha * .55f) }), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
}
