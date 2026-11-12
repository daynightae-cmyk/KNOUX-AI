package com.knoux.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var chapter by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val goNext: () -> Unit = {
        if (chapter < 2) {
            scope.launch { chapter++ }
        } else {
            context.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("knoux_onboarding_done", true).apply()
            onComplete()
        }
    }

    val goBack: () -> Unit = {
        if (chapter > 0) scope.launch { chapter-- }
    }

    // Intercept system back: go to previous chapter, or treat as skip on chapter 0
    BackHandler(enabled = true) {
        if (chapter > 0) {
            scope.launch { chapter-- }
        } else {
            context.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("knoux_onboarding_done", true).apply()
            onComplete()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xff190c52), Color(0xff090426), Color(0xff020008))
                )
            )
    ) {
        // Atmospheric background lighting
        Canvas(Modifier.fillMaxSize()) {
            val topRight = Offset(size.width * .78f, size.height * .04f)
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xff6d24ff).copy(alpha = .40f), Color(0xff2c1570).copy(alpha = .18f), Color.Transparent),
                    topRight, size.width * .90f
                ), size.width * .90f, topRight
            )
            val midLeft = Offset(size.width * .10f, size.height * .50f)
            drawCircle(
                Brush.radialGradient(
                    listOf(Cyan.copy(alpha = .12f), Color.Transparent), midLeft, size.width * .60f
                ), size.width * .60f, midLeft
            )
            val bottom = Offset(size.width * .60f, size.height * .92f)
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xffee00ff).copy(alpha = .10f), Color.Transparent), bottom, size.width * .55f
                ), size.width * .55f, bottom
            )
        }

        Column(Modifier.fillMaxSize()) {
            // Top bar with back / skip
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chapter > 0) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(role = Role.Button, onClick = goBack)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Label(("← Back" ar "→ رجوع").localized(), 13, Mist)
                    }
                } else {
                    Spacer(Modifier.width(72.dp))
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button, onClick = {
                            context.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("knoux_onboarding_done", true).apply()
                            onComplete()
                        })
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Label(("Skip" ar "تخطي").localized(), 13, Mist)
                }
            }

            // Chapter content
            Box(Modifier.weight(1f)) {
                when (chapter) {
                    0 -> OnboardingChapter1()
                    1 -> OnboardingChapter2()
                    2 -> OnboardingChapter3(onComplete)
                }
            }

            // Dot indicators + Next button
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        val active = index == chapter
                        Box(
                            Modifier
                                .height(6.dp)
                                .width(if (active) 24.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) Cyan else Mist.copy(alpha = .35f))
                        )
                    }
                }
                if (chapter < 2) {
                    AccentButton(
                        Action(("Next" ar "التالي"), Mark.NEXT),
                        Modifier.fillMaxWidth(),
                        primary = true
                    ) { goNext() }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chapter I — The Awakening
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OnboardingChapter1() {
    val motion = LocalPreviewMotion.current
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse = if (motion) transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb pulse"
    ) else null
    val spin = if (motion) transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "orb spin"
    ) else null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Central intelligence orb
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val breath = pulse?.value ?: 0f

                drawCircle(
                    Brush.radialGradient(
                        listOf(Violet.copy(alpha = .18f + breath * .10f), Color.Transparent),
                        Offset(cx, cy), size.width * .50f
                    ), size.width * .50f, Offset(cx, cy)
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf(Cyan.copy(alpha = .12f + breath * .08f), Color.Transparent),
                        Offset(cx, cy), size.width * .40f
                    ), size.width * .40f, Offset(cx, cy)
                )

                // Concentric rings
                val radii = listOf(80f, 65f, 50f, 36f, 22f)
                val alphas = listOf(.22f, .32f, .44f, .60f, .80f)
                radii.forEachIndexed { i, r ->
                    val pxR = r.dp.toPx() / 2f
                    val ringColor = if (i % 2 == 0) Cyan else Violet
                    drawCircle(
                        ringColor.copy(alpha = alphas[i] * (1f + breath * .25f)),
                        pxR, Offset(cx, cy), style = Stroke(1.4f.dp.toPx())
                    )
                }

                // Rotating arc segments
                val spinVal = spin?.value ?: 0f
                val arcR = 42.dp.toPx()
                val arcSize = Size(arcR * 2, arcR * 2)
                drawArc(
                    Cyan.copy(alpha = .70f), spinVal - 20f, 55f, false,
                    Offset(cx - arcR, cy - arcR), arcSize,
                    style = Stroke(2.4f.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    Violet.copy(alpha = .60f), spinVal + 160f, 42f, false,
                    Offset(cx - arcR, cy - arcR), arcSize,
                    style = Stroke(2.0f.dp.toPx(), cap = StrokeCap.Round)
                )

                // Inner glowing core
                drawCircle(
                    Brush.radialGradient(
                        listOf(Cyan.copy(alpha = .85f + breath * .15f), Violet.copy(alpha = .55f), Color(0xff1a0040)),
                        Offset(cx, cy), 20.dp.toPx()
                    ), 20.dp.toPx(), Offset(cx, cy)
                )
            }
            Glyph(Mark.K, Modifier.size(48.dp), Chalk, true)
        }

        Spacer(Modifier.height(16.dp))

        // Product identity: knoux ai wordmark (LTR always)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Label("knoux", 28, Chalk, FontWeight.ExtraBold, maxLines = 1)
                Label("ai", 28, Pink, FontWeight.ExtraBold, maxLines = 1)
            }
        }

        Spacer(Modifier.height(16.dp))

        Label(
            ("Intelligence. Beautifully Engineered." ar "ذكاء صُمم بإتقان.").localized(),
            20, Chalk, FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth(),
            align = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Label(
            ("A Product of KNOUX" ar "أحد منتجات KNOUX").localized(),
            14, Mist.copy(alpha = .70f),
            modifier = Modifier.fillMaxWidth(),
            align = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chapter II — The Intelligence Ecosystem
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OnboardingChapter2() {
    val allServices = Page.references   // first 10 Page entries
    val visibleStates = remember { List(allServices.size) { Animatable(0f) } }

    LaunchedEffect(Unit) {
        allServices.indices.forEach { i ->
            launch {
                delay(i * 90L)
                visibleStates[i].animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Label(
            ("One Intelligent Ecosystem." ar "منظومة ذكية واحدة.").localized(),
            22, Chalk, FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth(),
            align = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Label(
            ("10 intelligent services, one unified platform." ar "١٠ خدمات ذكية، منصة واحدة متكاملة.").localized(),
            13, Mist,
            modifier = Modifier.fillMaxWidth(),
            align = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // Central hub with energy lines
        Box(
            Modifier.fillMaxWidth().height(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val lineCount = 8
                (0 until lineCount).forEach { i ->
                    val angle = (i * 360f / lineCount) * PI.toFloat() / 180f
                    val startR = 18.dp.toPx()
                    val endR = 32.dp.toPx()
                    val sx = cx + cos(angle) * startR
                    val sy = cy + sin(angle) * startR
                    val ex = cx + cos(angle) * endR
                    val ey = cy + sin(angle) * endR
                    drawLine(
                        Brush.linearGradient(
                            listOf(Cyan.copy(alpha = .55f), Violet.copy(alpha = .18f)),
                            Offset(sx, sy), Offset(ex, ey)
                        ),
                        Offset(sx, sy), Offset(ex, ey),
                        1.4f.dp.toPx()
                    )
                }
                drawCircle(
                    Brush.radialGradient(
                        listOf(Cyan.copy(alpha = .90f), Violet.copy(alpha = .45f), Color.Transparent),
                        Offset(cx, cy), 20.dp.toPx()
                    ), 20.dp.toPx(), Offset(cx, cy)
                )
                drawCircle(Chalk.copy(alpha = .90f), 4.dp.toPx(), Offset(cx, cy))
            }
            NeonPill(("Intelligence Core" ar "نواة الذكاء").localized(), Cyan)
        }

        Spacer(Modifier.height(12.dp))

        // 3-column grid of 10 service modules
        allServices.chunked(3).forEach { rowServices ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowServices.forEach { page ->
                    val idx = allServices.indexOf(page)
                    val alpha = visibleStates[idx].value
                    val tint = when (idx % 4) {
                        0 -> Cyan; 1 -> Violet; 2 -> Pink; else -> Gold
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .graphicsLayer { this.alpha = alpha }
                    ) {
                        GlassPanel(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                            radius = 14, tint = tint
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Glyph(page.hero, Modifier.size(24.dp), tint, true)
                                Label(
                                    page.title.localized(), 9, Chalk, FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    align = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
                // Fill remaining slots so row always has 3 columns
                repeat(3 - rowServices.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chapter III — The Creator's Signature
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OnboardingChapter3(onComplete: () -> Unit) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Label(
            ("Engineered by Vision. Built for You." ar "رؤية هندسية. تجربة صُممت من أجلك.").localized(),
            20, Chalk, FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth(),
            align = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // KNOUX corporate logo in a circular glass panel
        GlassPanel(
            Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(68.dp)),
            radius = 68, tint = Violet
        ) {
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.knoux_logo),
                    contentDescription = "KNOUX",
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Founder glass card
        GlassPanel(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            radius = 20, tint = Gold
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .width(48.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5f.dp))
                        .background(Brush.horizontalGradient(listOf(Gold, Chalk, Gold)))
                )
                Spacer(Modifier.height(6.dp))

                Label(
                    ("Eng. Sadek Elgazar" ar "م. صادق الجزار").localized(),
                    20, Chalk, FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth(),
                    align = TextAlign.Center
                )
                Label(
                    ("Founder & Software Engineer" ar "المؤسس ومهندس البرمجيات").localized(),
                    13, Mist,
                    modifier = Modifier.fillMaxWidth(),
                    align = TextAlign.Center
                )
                Label(
                    ("Founder of KNOUX" ar "مؤسس KNOUX").localized(),
                    12, Mist.copy(alpha = .70f),
                    modifier = Modifier.fillMaxWidth(),
                    align = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Official site tappable link
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Gold.copy(alpha = .10f), RoundedCornerShape(12.dp))
                        .border(1.dp, Gold.copy(alpha = .60f), RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://knoux.store/"))
                                )
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(Mark.LINK, Modifier.size(14.dp), Gold)
                        Label("knoux.store", 13, Gold, FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // CTA: Enter KNOUX AI
        AccentButton(
            Action(("Enter KNOUX AI" ar "ابدأ رحلتك مع KNOUX AI"), Mark.SPARKLE),
            Modifier.fillMaxWidth(),
            primary = true
        ) {
            context.getSharedPreferences("knoux_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("knoux_onboarding_done", true).apply()
            onComplete()
        }

        Spacer(Modifier.height(24.dp))
    }
}
