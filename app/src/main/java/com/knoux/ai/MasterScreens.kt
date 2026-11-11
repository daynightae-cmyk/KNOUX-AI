package com.knoux.ai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * The six master screens rebuilt from the ten approved visual references:
 * Smart Device Hub, Smart Clean, Device Tools, Reports & Insights,
 * Privacy Sweep and AI Optimization.
 *
 * Every figure shown on these screens is sample data from the design and stays
 * visibly labelled as a design preview.
 */

// ---------------------------------------------------------------------------
// Shared master primitives
// ---------------------------------------------------------------------------

/** Two-tone italic title used by every master screen (white lead, magenta accent). */
@Composable
private fun MasterTitle(lead: Copy, accent: Copy, subtitle: Copy, tag: String) {
    val roomy = LocalRoomy.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val styled = buildAnnotatedString {
            withStyle(SpanStyle(color = Chalk)) { append(lead.localized()) }
            withStyle(SpanStyle(brush = Brush.verticalGradient(
                listOf(Color(0xffffa3ff), Color(0xffe35bff), Color(0xffb93bff))))) {
                append(" ")
                append(accent.localized())
            }
        }
        Text(styled, Modifier.fillMaxWidth().testTag(tag), style = TextStyle(
            fontSize = (if (roomy) 24 else 29).sp, lineHeight = (if (roomy) 28 else 33).sp,
            fontFamily = DisplayFace, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        ))
        Label(subtitle.localized(), if (roomy) 12 else 13, Mist,
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp), align = TextAlign.Center)
    }
}

/** The compact gold disclosure that marks every master screen as design preview data. */
@Composable
private fun MasterPreviewPill(label: Copy = ("DESIGN PREVIEW" ar "معاينة التصميم")) {
    Box(Modifier.fillMaxWidth().padding(top = 7.dp), contentAlignment = Alignment.Center) {
        DesignPreviewPill(label = label)
    }
}

/** Circular information marker used by the honest-preview notes. */
@Composable
private fun InfoMarker(tint: Color = Mist, modifier: Modifier = Modifier) {
    Canvas(modifier.size(17.dp)) {
        drawCircle(tint.copy(alpha = .18f), radius = size.minDimension / 2f)
        drawCircle(tint.copy(alpha = .85f), radius = size.minDimension / 2f, style = Stroke(1.1.dp.toPx()))
        val w = 1.3.dp.toPx()
        drawLine(tint, Offset(size.width / 2f, size.height * .38f), Offset(size.width / 2f, size.height * .66f), w, StrokeCap.Round)
        drawCircle(tint, w * .62f, Offset(size.width / 2f, size.height * .28f))
    }
}

/** The honest-preview note strip. */
@Composable
private fun PreviewNote(text: Copy, modifier: Modifier = Modifier) {
    GlassPanel(modifier.fillMaxWidth(), radius = 18, tint = Violet) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            InfoMarker()
            Label(text.localized(), 11, Mist)
        }
    }
}

/** Big rounded glass card used for the featured rows of the master screens. */
@Composable
private fun MasterCard(
    modifier: Modifier = Modifier, radius: Int = 22, tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassPanel(modifier.fillMaxWidth(), radius = radius, tint = tint) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

/** Rounded icon tile with a lit rim, matching the reference service icons. */
@Composable
internal fun MasterIcon(mark: Mark, tint: Color, diameter: Int = 44) {
    val shape = RoundedCornerShape((diameter * .30f).dp)
    Box(Modifier.size(diameter.dp).clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = .55f), Color(0xff120a33).copy(alpha = .92f))), shape)
        .border(1.dp, Brush.horizontalGradient(listOf(Color.White.copy(alpha = .7f), tint, Color.Transparent)), shape),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            bloom(Offset(size.width * .30f, size.height * .20f), size.width * .85f, tint, .9f)
        }
        VolumetricGlyph(mark, tint, (diameter * .68f).dp)
    }
}

/** The service row of the Device Tools reference: icon, copy, metric chip, chevron. */
@Composable
private fun ServiceRow(
    title: Copy, copy: Copy, metric: String, metricNote: Copy, mark: Mark, tint: Color,
    tag: String, onClick: () -> Unit
) {
    val mirror = if (LocalArabic.current) 180f else 0f
    GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .testTag(tag).clickable(role = Role.Button, onClick = onClick), radius = 20, tint = tint) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            MasterIcon(mark, tint, 46)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Label(title.localized(), 15, weight = FontWeight.ExtraBold)
                Label(copy.localized(), 10, Mist, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NeonPill(metric, Cyan, solid = false)
                    Label(metricNote.localized(), 10, Mist)
                }
            }
            Glyph(Mark.NEXT, Modifier.size(15.dp).rotate(mirror), Mist)
        }
    }
}

/** The ghost/primary action pair of the references. */
@Composable
private fun MasterActions(first: Action, second: Action, onFirst: () -> Unit, onSecond: () -> Unit) {
    if (LocalRoomy.current) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentButton(first, Modifier.fillMaxWidth(), primary = false, tag = "review-action", onClick = onFirst)
        AccentButton(second, Modifier.fillMaxWidth(), primary = true, tag = "primary-action", onClick = onSecond)
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        AccentButton(first, Modifier.weight(1f), primary = false, tag = "review-action", onClick = onFirst)
        AccentButton(second, Modifier.weight(1f), primary = true, tag = "primary-action", onClick = onSecond)
    }
}

/** Minimal sparkline used by the two report cards of the reference. */
@Composable
private fun TrendSparkline(tint: Color, modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) { entered = true }
    val phase by animateFloatAsState(if (entered) 1f else 0f,
        animationSpec = tween(if (motion) 900 else 0), label = "spark")
    Canvas(modifier) {
        val values = listOf(.18f, .34f, .28f, .52f, .46f, .74f, .92f)
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = step * index
            val y = size.height * (1f - value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Brush.horizontalGradient(listOf(tint.copy(alpha = .55f), tint)), style = Stroke(1.6.dp.toPx()))
        values.forEachIndexed { index, value ->
            drawCircle(Color.White.copy(alpha = .85f), 2.4.dp.toPx(), Offset(step * index, size.height * (1f - value)))
        }
    }
}

/** The segmented day-range control of the Reports reference. */
@Composable
private fun RangeTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("7 Days" ar "٧ أيام", "30 Days" ar "٣٠ يومًا", "90 Days" ar "٩٠ يومًا")
    GlassPanel(Modifier.fillMaxWidth(), radius = 22) {
        Row(Modifier.fillMaxWidth().padding(5.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, title ->
                val active = index == selected
                Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (active) Brush.horizontalGradient(
                        listOf(Color(0xff9a2bff), Color(0xff5a28e8), Color(0xff2448ff)))
                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .border(1.dp, if (active) Color.White.copy(alpha = .40f) else Color.White.copy(alpha = .12f), RoundedCornerShape(18.dp))
                    .selectable(active, role = Role.Tab, onClick = { onSelect(index) })
                    .testTag("range-$index"), contentAlignment = Alignment.Center) {
                    Label(title.localized(), 13, if (active) Chalk else Mist,
                        if (active) FontWeight.ExtraBold else FontWeight.Medium, align = TextAlign.Center)
                }
            }
        }
    }
}

/** The weekly activity bar chart of the Reports reference. */
@Composable
private fun ActivityChart(modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(if (entered) 1f else 0f,
        animationSpec = tween(if (motion) 950 else 0), label = "chart reveal")
    val values = listOf("0.4 GB" to .25f, "0.7 GB" to .44f, "0.6 GB" to .38f, "0.9 GB" to .56f,
        "1.1 GB" to .69f, "1.3 GB" to .81f, "1.0 GB" to .63f)
    val days = listOf("Mon" ar "الاثنين", "Tue" ar "الثلاثاء", "Wed" ar "الأربعاء", "Thu" ar "الخميس",
        "Fri" ar "الجمعة", "Sat" ar "السبت", "Sun" ar "الأحد")
    val dayLabels = days.map { it.localized() }
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val axisHeight = 16.dp.toPx()
        val top = 14.dp.toPx()
        val chartHeight = size.height - axisHeight - top
        val grid = listOf("1.6 GB", "1.2 GB", "0.8 GB", "0.4 GB", "0 GB")
        grid.forEachIndexed { index, label ->
            val y = top + chartHeight * (index / (grid.size - 1f))
            drawLine(Color.White.copy(alpha = .18f), Offset(0f, y), Offset(size.width, y),
                .9.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 7f)))
            val measured = measurer.measure(label, style = TextStyle(fontSize = 8.sp, color = Mist))
            drawText(measured, topLeft = Offset(0f, y - measured.size.height / 2f))        }
        val slot = size.width / values.size
        val barWidth = slot * .52f
        values.forEachIndexed { index, pair ->
            val value = pair.second * reveal
            val barHeight = chartHeight * value
            val x = slot * index + (slot - barWidth) / 2f
            val y = top + chartHeight - barHeight
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xffff54ff), Color(0xffb13aff), Color(0xff4f6bff), Cyan)),
                topLeft = Offset(x, y), size = Size(barWidth, barHeight), cornerRadius = CornerRadius(barWidth * .30f))
            drawRoundRect(Color.White.copy(alpha = .35f), topLeft = Offset(x, y),
                size = Size(barWidth, barHeight * .18f), cornerRadius = CornerRadius(barWidth * .30f))
            val label = measurer.measure(pair.first,
                style = TextStyle(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Chalk))
            drawText(label, topLeft = Offset(x + (barWidth - label.size.width) / 2f,
                max(0f, y - label.size.height - 2.dp.toPx())))
            val day = measurer.measure(dayLabels[index], style = TextStyle(fontSize = 8.5.sp, color = Mist))
            drawText(day, topLeft = Offset(x + (barWidth - day.size.width) / 2f, top + chartHeight + 3.dp.toPx()))
        }
    }
}

/** The segmented risk donut of the Privacy reference. */
@Composable
private fun RiskDonut(modifier: Modifier = Modifier) {
    val motion = LocalPreviewMotion.current
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(if (entered) 1f else 0f,
        animationSpec = tween(if (motion) 900 else 0), label = "risk reveal")
    val segments = listOf(.13f to Color(0xffff4d8d), .25f to Gold, .38f to Cyan, .24f to Color(0xff9f9ad6))
    Canvas(modifier) {
        val band = size.minDimension * .17f
        val inset = size.minDimension * .07f
        val topLeft = Offset(inset, inset)
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        var angle = -90f
        val frontier = -90f + 360f * reveal
        segments.forEach { (share, color) ->
            val sweep = 360f * share
            val lit = (frontier - angle).coerceIn(0f, sweep)
            if (lit > .5f) {
                drawArc(color.copy(alpha = .30f), angle + 1.2f, lit - 2.4f, false, topLeft, arcSize, style = Stroke(band * 1.5f))
                drawArc(color, angle + 1.2f, lit - 2.4f, false, topLeft, arcSize, style = Stroke(band))
                drawArc(Color.White.copy(alpha = .45f), angle + 1.8f, lit - 3.6f, false, topLeft, arcSize, style = Stroke(band * .16f))
            }
            angle += sweep
        }
        drawCircle(Color.White.copy(alpha = .08f), radius = size.minDimension / 2f - inset - band * .6f,
            style = Stroke(1.dp.toPx()))
    }
}

/** A glowing opportunity sphere used by the AI Optimization reference. */
@Composable
private fun OpportunitySphere(
    modifier: Modifier = Modifier, title: Copy, mark: Mark, tint: Color, selected: Boolean,
    tag: String, onToggle: () -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val scale by animateFloatAsState(if (selected) 1f else .82f, tween(220), label = "sphere scale")
        Box(Modifier.size(74.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(37.dp))
            .background(Brush.radialGradient(listOf(tint.copy(alpha = .55f), Color(0xff1b0f52).copy(alpha = .85f))))
            .border(if (selected) 1.6.dp else 1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = .85f), tint, Color.Transparent)),
                RoundedCornerShape(37.dp))
            .toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .testTag(tag), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                bloom(Offset(size.width * .34f, size.height * .26f), size.width * .70f, tint, .85f)
            }
            VolumetricGlyph(mark, if (selected) Chalk else Mist, 30.dp)
        }
        Spacer(Modifier.height(6.dp))
        Label(title.localized(), 13, if (selected) Chalk else Mist,
            if (selected) FontWeight.ExtraBold else FontWeight.Medium, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// 1 - Smart Device Hub (Home)
// ---------------------------------------------------------------------------

private data class HubCardSpec(
    val title: Copy, val copy: Copy, val mark: Mark, val tint: Color, val tag: String, val page: Page
)

@Composable
internal fun SmartHubScreen(open: (Page) -> Unit) {
    val roomy = LocalRoomy.current
    val motion = LocalPreviewMotion.current
    // FIXED VIEWPORT: No root scrolling, BoxWithConstraints for measured layout
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Hero section - now reduced height from Pass 3
            NeonHero(Mark.K, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
            MasterTitle("Smart" ar "مركز", "Device Hub" ar "الجهاز الذكي",
                "Device intelligence, optimization and cleanup — with a clear preview before any changes."
                    ar "ذكاء الجهاز والتحسين والتنظيف — مع معاينة واضحة قبل أي تغيير.",
                "screen-home")
            
            // Content area with weight fills remaining space
            Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 11.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                val cards = listOf(
                    HubCardSpec("Smart Clean" ar "التنظيف الذكي",
                        "Remove junk, duplicates and app residue." ar "أزل الملفات غير المرغوبة والمكررة وبقايا التطبيقات.",
                        Mark.TRASH, Pink, "open-clean-center", Page.CLEAN_CENTER),
                    HubCardSpec("AI Optimization" ar "التحسين الذكي",
                        "Boost performance, tune system and save battery." ar "حسّن الأداء واضبط النظام ووفّر البطارية.",
                        Mark.CHIP, Cyan, "hub-ai-optimization", Page.AI_RECOMMENDATIONS),
                    HubCardSpec("Privacy Sweep" ar "مراجعة الخصوصية",
                        "Review trackers, risky permissions and exposure." ar "راجع أدوات التتبع والأذونات الخطرة والتعرّض.",
                        Mark.SHIELD, Pink, "hub-privacy-sweep", Page.PRIVACY_INSPECTOR),
                    HubCardSpec("Storage Analyzer" ar "تحليل التخزين",
                        "See what's using space and find what's reclaimable." ar "اعرف ما يستهلك المساحة وما يمكن استعادته.",
                        Mark.DISK, Cyan, "hub-storage-analyzer", Page.STORAGE_BREAKDOWN),
                    HubCardSpec("App Manager" ar "إدارة التطبيقات",
                        "Manage installed apps, usage and heavy weights." ar "أدر التطبيقات المثبتة واستخدامها وثقيلها.",
                        Mark.GROUP, Pink, "hub-app-manager", Page.APPS_REVIEW),
                    HubCardSpec("Reports & Insights" ar "التقارير والرؤى",
                        "Track cleanups, optimizations and device health trends." ar "تابع عمليات التنظيف والتحسين ومؤشرات صحة الجهاز.",
                        Mark.BARS, Cyan, "hub-reports", Page.REPORTS)
                )
                // Service grid - 3x2 on normal screens, vertical on roomy
                if (roomy) {
                    cards.forEach { spec ->
                        HubCard(spec.title, spec.copy, spec.mark, spec.tint, spec.tag) { open(spec.page) }
                    }
                } else {
                    cards.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            pair.forEach { spec ->
                                Box(Modifier.weight(1f)) {
                                    HubCard(spec.title, spec.copy, spec.mark, spec.tint, spec.tag) { open(spec.page) }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                // Quick Actions row
                Label(("Quick Actions" ar "إجراءات سريعة").localized(), 15, weight = FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth().padding(start = 2.dp, top = 2.dp, bottom = 3.dp))
                MasterCard(radius = 18, tint = Violet) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        QuickAction(Modifier.weight(1f), "Deep Scan" ar "فحص عميق", Mark.RADAR, Cyan, "start-scan") { open(Page.SCAN_SUMMARY) }
                        Separator(Modifier.width(1.dp).height(34.dp))
                        QuickAction(Modifier.weight(1f), "Save Battery" ar "توفير البطارية", Mark.BATTERY, Pink, "quick-battery") { open(Page.BATTERY_DIAGNOSTICS) }
                        Separator(Modifier.width(1.dp).height(34.dp))
                        QuickAction(Modifier.weight(1f), "Secure Device" ar "تأمين الجهاز", Mark.SHIELD, Gold, "quick-privacy") { open(Page.PRIVACY_INSPECTOR) }
                    }
                }
                
                // Disclosure note - compact
                PreviewNote(("Preview / Demo Sample Only — these are example values and do not represent live results from your device."
                    ar "معاينة / تجريب فقط — هذه قيم أمثلة ولا تمثل نتائج فعلية من جهازك."))
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier, title: Copy, mark: Mark, tint: Color, tag: String, onClick: () -> Unit
) {
    Column(modifier.clickable(role = Role.Button, onClick = onClick).testTag(tag)
        .padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Glyph(mark, Modifier.size(22.dp), tint, true)
        Label(title.localized(), 11, weight = FontWeight.Bold, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// 2 - Smart Clean (Clean)
// ---------------------------------------------------------------------------

private data class CleanSpec(
    val title: Copy, val copy: Copy, val size: String, val mark: Mark, val tag: String, val page: Page
)

@Composable
internal fun SmartCleanScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    var selected by rememberSaveable { mutableIntStateOf(0b0111) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = listOf(
        CleanSpec("Cache" ar "ذاكرة مؤقتة", "App and system cache files" ar "ملفات ذاكرة التطبيقات والنظام",
            "1.24 GB", Mark.LAYERS, "open-clean-center", Page.CLEAN_CENTER),
        CleanSpec("Temporary Files" ar "ملفات مؤقتة", "Short-term and leftover files" ar "ملفات قصيرة الأمد ومتبقية",
            "512 MB", Mark.FILE, "open-deep-scan", Page.SCAN),
        CleanSpec("Duplicate Candidates" ar "مرشحات التكرار", "Similar photos, videos and files" ar "صور وفيديوهات وملفات متشابهة",
            "698 MB", Mark.PICTURES, "open-duplicates", Page.DUPLICATES),
        CleanSpec("App Residue" ar "بقايا التطبيقات", "Leftovers from uninstalled apps" ar "بقايا من تطبيقات غير مثبتة",
            "416 MB", Mark.BOX, "open-residue", Page.RESIDUE)
    )
    // FIXED VIEWPORT — no root scroll
    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.TRASH, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle("Smart" ar "تنظيف", "Clean" ar "ذكي",
            "Review unwanted files before any cleanup." ar "راجع الملفات غير المرغوبة قبل أي تنظيف.",
            "screen-clean")
        MasterPreviewPill()
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            MasterCard(radius = 22, tint = Violet) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(118.dp), contentAlignment = Alignment.Center) {
                        ProgressDonut(76, ("Recoverable (Preview)" ar "قابل للاسترداد (معاينة)").localized(),
                            diameter = 118, displayValue = "2.86 GB")
                    }
                    Separator(Modifier.width(1.dp).height(88.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        MasterIcon(Mark.DISK, Pink, 34)
                        Label(("Cleanup Potential" ar "الإمكان التنظيفي").localized(), 11, Mist)
                        Label("2.86 GB", 22, weight = FontWeight.ExtraBold)
                        Label(("Across 4 categories" ar "عبر ٤ فئات").localized(), 10, Mist)
                    }
                }
            }
            categories.forEachIndexed { index, spec ->
                val checked = selected and (1 shl index) != 0
                MasterCard(radius = 16, tint = if (checked) Cyan else null) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .testTag(spec.tag).clickable(role = Role.Button) { open(spec.page) }
                            .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            MasterIcon(spec.mark, if (checked) Cyan else Violet, 34)
                            Column(Modifier.weight(1f)) {
                                Label(spec.title.localized(), 14, weight = FontWeight.Bold)
                                Label(spec.copy.localized(), 10, Mist)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Label(spec.size, 14, weight = FontWeight.Bold)
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.size(28.dp)
                                .toggleable(checked, role = Role.Checkbox, onValueChange = {
                                    selected = selected xor (1 shl index)
                                })
                                .testTag("clean-category-$index"), contentAlignment = Alignment.Center) {
                                SelectionIndicator(checked, Cyan)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            MasterActions(
                Action("Inspect Categories" ar "فحص الفئات", Mark.LAYERS),
                Action("Review Cleanup" ar "مراجعة التنظيف", Mark.BROOM),
                onFirst = { sheet = "review" }, onSecond = { sheet = "proceed" }
            )
            PreviewNote(("Design Preview Only — these are example values and do not represent live results from your device."
                ar "معاينة تصميم فقط — هذه قيم أمثلة ولا تمثل نتائج فعلية من جهازك."))
        }
    }
    sheet?.let { mode ->
        InfoSheet(if (mode == "review") ("Review Categories" ar "مراجعة الفئات") else ("Review Cleanup" ar "مراجعة التنظيف"),
            { sheet = null }) {
            when (mode) {
                "review" -> {
                    AccentButton(Action(Page.CLEAN_CENTER.title, Page.CLEAN_CENTER.hero), Modifier.fillMaxWidth(),
                        tag = "open-clean-center-review") {
                        sheet = null
                        open(Page.CLEAN_CENTER)
                    }
                    categories.forEach { spec -> Label("${spec.title.localized()} · ${spec.size}", 13, Mist) }
                }
                "proceed" -> {
                    categories.filterIndexed { index, _ -> selected and (1 shl index) != 0 }
                        .forEach { spec -> Label("${spec.title.localized()} · ${spec.size}", 13, Mist) }
                    AccentButton(Action("Confirm preview" ar "تأكيد المعاينة", Mark.CHECK), Modifier.fillMaxWidth(),
                        enabled = selected != 0, tag = "confirm-preview") { sheet = "ack" }
                }
                else -> Label(NoChange.localized(), 16, Cyan, FontWeight.Bold, Modifier.testTag("preview-only-result"))
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
}

// ---------------------------------------------------------------------------
// 3 - Device Tools (Tools)
// ---------------------------------------------------------------------------

@Composable
internal fun DeviceToolsScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    // Tools is an index screen (directory) — controlled scroll is acceptable here
    // but we still tighten the hero and reduce excess padding
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(176.dp), contentAlignment = Alignment.Center) {
            NeonHero(Mark.K, motion, heightOverride = 172.dp)
            SatelliteTile(Mark.DISK, Cyan, Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 12.dp))
            SatelliteTile(Mark.GROUP, Pink, Modifier.align(Alignment.TopEnd).padding(end = 22.dp, top = 6.dp))
            SatelliteTile(Mark.SHIELD, Pink, Modifier.align(Alignment.CenterStart).padding(start = 12.dp, bottom = 22.dp))
            SatelliteTile(Mark.BATTERY, Cyan, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, bottom = 26.dp))
        }
        MasterTitle("Device" ar "أدوات", "Tools" ar "الجهاز",
            "Your device intelligence workspace." ar "مساحة عمل ذكاء جهازك.", "screen-tools")
        MasterPreviewPill()
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .testTag("open-storage").clickable(role = Role.Button) { open(Page.STORAGE_BREAKDOWN) }, radius = 22, tint = Pink) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Glyph(Mark.CHECK, Modifier.size(12.dp), Cyan, true)
                        Label(("FEATURED" ar "مميز").localized(), 10, Cyan, FontWeight.ExtraBold)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Label(("Storage Analyzer" ar "محلل التخزين").localized(), 19, weight = FontWeight.ExtraBold)
                            Label(("See where every byte goes, what is reclaimable and which folders dominate."
                                ar "شاهد أين تذهب كل بايت وما يمكن استعادته وأي المجلدات تهيمن.").localized(), 11, Mist)
                        }
                        MasterIcon(Mark.DISK, Pink, 48)
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Label("128 GB", 17, weight = FontWeight.ExtraBold)
                        Label(("used of 256 GB" ar "مستخدم من 256 GB").localized(), 11, Mist,
                            modifier = Modifier.padding(bottom = 2.dp))
                    }
                    GradientBar(.5f, thickness = 7, live = motion)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Label("50%", 14, Cyan, FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        Glyph(Mark.NEXT, Modifier.size(14.dp), Mist)
                    }
                }
            }
            ServiceRow("App Manager" ar "إدارة التطبيقات",
                "Manage installed apps, usage, heavy weights and rarely used apps."
                    ar "أدر التطبيقات المثبتة واستخدامها والثقيلة وقليلة الاستخدام.",
                "41", "apps installed" ar "تطبيق مثبت", Mark.GROUP, Pink, "open-app-manager") { open(Page.APPS_REVIEW) }
            ServiceRow("Privacy Sweep" ar "مراجعة الخصوصية",
                "Review trackers, risky permissions and privacy exposure."
                    ar "راجع أدوات التتبع والأذونات الخطرة والتعرّض للخصوصية.",
                "7", "trackers found" ar "أدوات تتبع", Mark.SHIELD, Pink, "open-privacy") { open(Page.PRIVACY_INSPECTOR) }
            ServiceRow("Battery Diagnostics" ar "تشخيص البطارية",
                "Check battery health, usage patterns and background drain."
                    ar "افحص صحة البطارية وأنماط الاستخدام والاستهلاك في الخلفية.",
                "+18%", "estimated battery gain" ar "تحسن تقديري للبطارية",
                Mark.BATTERY, Cyan, "open-battery") { open(Page.BATTERY_DIAGNOSTICS) }
            ServiceRow("AI Optimization" ar "التحسين الذكي",
                "Boost performance, tune system and optimize battery usage intelligently."
                    ar "حسّن الأداء واضبط النظام وحسّن استهلاك البطارية بذكاء.",
                "94", "performance score" ar "درجة الأداء", Mark.CHIP, Pink, "open-ai-optimization") { open(Page.AI_RECOMMENDATIONS) }
            ServiceRow("Storage Breakdown" ar "تفاصيل المساحة",
                "A clear view of storage by category (photos, videos, apps, files and more)."
                    ar "عرض واضح للتخزين حسب الفئة (صور، فيديوهات، تطبيقات، ملفات والمزيد).",
                "12.6 GB", "reclaimable space" ar "مساحة قابلة للاسترداد",
                Mark.LAYERS, Cyan, "open-storage-breakdown") { open(Page.STORAGE_BREAKDOWN) }
        }
    }
}

@Composable
private fun SatelliteTile(mark: Mark, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = .40f), Color(0xff150c36).copy(alpha = .9f))),
            RoundedCornerShape(14.dp))
        .border(1.dp, Brush.horizontalGradient(listOf(Color.White.copy(alpha = .7f), tint, Color.Transparent)),
            RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        VolumetricGlyph(mark, tint, 26.dp)
    }
}

// ---------------------------------------------------------------------------
// 4 - Reports & Insights (Reports)
// ---------------------------------------------------------------------------

@Composable
internal fun ReportsScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    var range by rememberSaveable { mutableIntStateOf(0) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    val entries = PreviewFixtures.screen(Page.REPORTS, PreviewSource.DESIGN_PREVIEW)?.entries.orEmpty()
    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.BARS, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle("Reports &" ar "التقارير", "Insights" ar "والرؤى",
            "See device trends and previous actions." ar "شاهد اتجاهات الجهاز والإجراءات السابقة.", "screen-reports")
        MasterPreviewPill()
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            RangeTabs(range) { range = it }
            MasterCard(radius = 20, tint = Violet) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MasterIcon(Mark.BARS, Pink, 32)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Label(("Device Activity (7 Days)" ar "نشاط الجهاز (٧ أيام)").localized(), 14, weight = FontWeight.ExtraBold)
                        Label(("Daily optimization activity (example)" ar "نشاط التحسين اليومي (مثال)").localized(), 10, Mist)
                    }
                    PreviewChip(("DESIGN PREVIEW" ar "معاينة التصميم"), Gold, "preview-chip-chart")
                }
                ActivityChart(Modifier.fillMaxWidth().height(130.dp).padding(top = 5.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MasterCard(Modifier.weight(1f), radius = 18, tint = Cyan) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MasterIcon(Mark.DISK, Pink, 30)
                        Column(Modifier.weight(1f)) {
                            Label(("Storage Trend" ar "اتجاه التخزين").localized(), 12, weight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Label("128 GB", 13, Cyan, FontWeight.ExtraBold)
                                Label("/ 256 GB", 9, Mist)
                            }
                        }
                    }
                    TrendSparkline(Cyan, Modifier.fillMaxWidth().height(32.dp).padding(top = 3.dp))
                    PreviewChip(("DESIGN PREVIEW" ar "معاينة التصميم"), Gold, "preview-chip-storage")
                }
                MasterCard(Modifier.weight(1f), radius = 18, tint = Violet) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MasterIcon(Mark.SHIELD, Cyan, 30)
                        Column(Modifier.weight(1f)) {
                            Label(("Privacy Review" ar "مراجعة الخصوصية").localized(), 12, weight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Label("7", 13, Cyan, FontWeight.ExtraBold)
                                Label(("items" ar "عناصر").localized(), 9, Mist)
                            }
                        }
                    }
                    TrendSparkline(Violet, Modifier.fillMaxWidth().height(32.dp).padding(top = 3.dp))
                    PreviewChip(("DESIGN PREVIEW" ar "معاينة التصميم"), Gold, "preview-chip-privacy")
                }
            }
            GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .testTag("open-report-history").clickable(role = Role.Button) { open(Page.REPORT_DETAIL) },
                radius = 18, tint = Violet) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MasterIcon(Mark.CALENDAR, Pink, 34)
                    Column(Modifier.weight(1f)) {
                        Label(("Historical Activity (Example)" ar "النشاط التاريخي (مثال)").localized(), 13, weight = FontWeight.Bold)
                        Label(("Scan · Analyze · Optimize · Review" ar "فحص · تحليل · تحسين · مراجعة").localized(), 10, Mist)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(12, 20, 28).forEach { h ->
                            Box(Modifier.width(4.dp).height(h.dp).clip(RoundedCornerShape(2.dp))
                                .background(Brush.verticalGradient(listOf(Pink, Cyan))))
                        }
                    }
                    Glyph(Mark.NEXT, Modifier.size(14.dp), Mist)
                }
            }
            Spacer(Modifier.weight(1f))
            MasterActions(
                Action("View History" ar "عرض السجل", Mark.CALENDAR),
                Action("View Report Details" ar "عرض تفاصيل التقرير", Mark.BARS),
                onFirst = { sheet = "review" }, onSecond = { sheet = "proceed" }
            )
        }
    }
    sheet?.let { mode ->
        InfoSheet(if (mode == "review") ("View History" ar "عرض السجل") else ("View Report Details" ar "عرض تفاصيل التقرير"),
            { sheet = null }) {
            when (mode) {
                "review" -> {
                    AccentButton(Action(Page.REPORT_DETAIL.title, Page.REPORT_DETAIL.hero), Modifier.fillMaxWidth(),
                        tag = "open-performance-report-detail") {
                        sheet = null
                        open(Page.REPORT_DETAIL)
                    }
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                }
                "proceed" -> {
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                    AccentButton(Action("Confirm preview" ar "تأكيد المعاينة", Mark.CHECK), Modifier.fillMaxWidth(),
                        tag = "confirm-preview") { sheet = "ack" }
                }
                else -> Label(NoChange.localized(), 16, Cyan, FontWeight.Bold, Modifier.testTag("preview-only-result"))
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
}

/** Small gold preview marker used inside cards (never the canonical design-preview tag). */
@Composable
private fun PreviewChip(label: Copy, tint: Color, tag: String) {
    Box(Modifier.heightIn(min = 26.dp).testTag(tag).clip(RoundedCornerShape(13.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xff24163f), Color(0xff160d2c))), RoundedCornerShape(13.dp))
        .border(1.dp, tint.copy(alpha = .8f), RoundedCornerShape(13.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center) {
        Label(label.localized(), 9, tint, FontWeight.Bold, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// 5 - Privacy Sweep (Privacy)
// ---------------------------------------------------------------------------

private data class RiskSpec(
    val title: Copy, val count: Copy, val risk: Copy, val tint: Color, val mark: Mark
)

@Composable
internal fun PrivacySweepScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var entryIndex by rememberSaveable { mutableIntStateOf(0) }
    val data = PreviewFixtures.screen(Page.PRIVACY, PreviewSource.DESIGN_PREVIEW)
    val risks = listOf(
        RiskSpec("Camera Access" ar "الوصول للكاميرا", "2 apps" ar "تطبيقان", "Medium Risk" ar "خطورة متوسطة", Gold, Mark.CAMERA),
        RiskSpec("Microphone Access" ar "الوصول للميكروفون", "1 app" ar "تطبيق واحد", "High Risk" ar "خطورة عالية",
            Color(0xffff5d8f), Mark.MIC),
        RiskSpec("Background Trackers" ar "أدوات تتبع بالخلفية", "7 trackers" ar "٧ أدوات تتبع",
            "Medium Risk" ar "خطورة متوسطة", Gold, Mark.LAYERS),
        RiskSpec("Clipboard Watchers" ar "مراقبون الحافظة", "2 apps" ar "تطبيقان", "Low Risk" ar "خطورة منخفضة", Cyan, Mark.CLIP)
    )
    val legend = listOf(
        Triple("High Risk" ar "خطورة عالية", "1 (13%)", Color(0xffff5d8f)),
        Triple("Medium Risk" ar "خطورة متوسطة", "2 (25%)", Gold),
        Triple("Low Risk" ar "خطورة منخفضة", "3 (38%)", Cyan),
        Triple("Minimal" ar "ضئيلة", "2 (25%)", Color(0xff9f9ad6))
    )
    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.SHIELD, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle("Privacy" ar "مراجعة", "Sweep" ar "الخصوصية",
            "Understand permissions, trackers and exposure." ar "افهم الأذونات وأدوات التتبع ومدى التعرّض.",
            "screen-privacy")
        MasterPreviewPill(("DESIGN PREVIEW (SAMPLE DATA)" ar "معاينة التصميم (بيانات نموذجية)"))
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            risks.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    pair.forEach { spec ->
                        val index = risks.indexOf(spec)
                        Box(Modifier.weight(1f)) {
                            RiskCard(spec, "row-$index") { entryIndex = index; sheet = "detail" }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            MasterCard(radius = 20, tint = Violet) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                        RiskDonut(Modifier.fillMaxSize())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Label(("Privacy Risk" ar "مخاطر الخصوصية").localized(), 9, Mist, align = TextAlign.Center)
                            Label("38%", 26, weight = FontWeight.ExtraBold)
                            NeonPill("Medium", Gold, solid = false)
                        }
                    }
                    Separator(Modifier.width(1.dp).height(90.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Label(("Sample Risk Breakdown" ar "توزيع المخاطر النموذجي").localized(), 12, weight = FontWeight.ExtraBold)
                        Label(("(Design Preview Values)" ar "(قيم المعاينة)").localized(), 10, Mist)
                        legend.forEach { (name, count, color) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                                Label(name.localized(), 11, modifier = Modifier.weight(1f))
                                Label(count, 11, weight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            PreviewNote(("Sample data only — design preview values to illustrate the Privacy Sweep experience. Not a real device scan."
                ar "بيانات نموذجية فقط — قيم معاينة لتوضيح تجربة مراجعة الخصوصية. ليست فحصًا حقيقيًا للجهاز."))
            Spacer(Modifier.weight(1f))
            MasterActions(
                Action("Review Risks" ar "مراجعة المخاطر", Mark.SHIELD),
                Action("Privacy Recommendations" ar "توصيات الخصوصية", Mark.CHECK),
                onFirst = { sheet = "review" }, onSecond = { sheet = "proceed" }
            )
        }
    }
    sheet?.let { mode ->
        val entries = data?.entries.orEmpty()
        InfoSheet(
            when (mode) {
                "detail" -> risks.getOrNull(entryIndex)?.title ?: ("Privacy Item" ar "عنصر الخصوصية")
                "review" -> ("Review Risks" ar "مراجعة المخاطر")
                else -> ("Privacy Recommendations" ar "توصيات الخصوصية")
            }, { sheet = null }) {
            when (mode) {
                "detail" -> {
                    risks.getOrNull(entryIndex)?.let {
                        Label(it.count.localized(), 18, Pink, FontWeight.Bold)
                        Label(it.risk.localized(), 13, it.tint, FontWeight.Bold)
                        Label(("Sample entry — no permission was read on this device."
                            ar "عنصر نموذجي — لم تُقرأ أي أذونات من هذا الجهاز.").localized(), 12, Mist)
                    }
                    entries.getOrNull(entryIndex)?.let {
                        Label(it.detail.localized(), 13, Mist)
                        Label(it.value, 16, Pink, FontWeight.Bold)
                    }
                }
                "review" -> {
                    AccentButton(Action(Page.PRIVACY_INSPECTOR.title, Page.PRIVACY_INSPECTOR.hero),
                        Modifier.fillMaxWidth(), tag = "open-privacy-risk-inspector") {
                        sheet = null
                        open(Page.PRIVACY_INSPECTOR)
                    }
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                }
                "proceed" -> {
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                    AccentButton(Action("Confirm preview" ar "تأكيد المعاينة", Mark.CHECK), Modifier.fillMaxWidth(),
                        tag = "confirm-preview") { sheet = "ack" }
                }
                else -> Label(NoChange.localized(), 16, Cyan, FontWeight.Bold, Modifier.testTag("preview-only-result"))
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
}

@Composable
private fun RiskCard(spec: RiskSpec, tag: String, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .testTag(tag).clickable(role = Role.Button, onClick = onClick), radius = 20, tint = spec.tint) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MasterIcon(spec.mark, spec.tint, 44)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Label(spec.title.localized(), 14, weight = FontWeight.ExtraBold)
                Label(spec.count.localized(), 16, weight = FontWeight.ExtraBold)
                RiskPill(spec.risk, spec.tint)
            }
        }
    }
}

@Composable
private fun RiskPill(label: Copy, tint: Color) {
    Box(Modifier.heightIn(min = 24.dp).clip(RoundedCornerShape(12.dp))
        .background(tint.copy(alpha = .16f)).border(1.dp, tint.copy(alpha = .8f), RoundedCornerShape(12.dp))
        .padding(horizontal = 9.dp, vertical = 3.dp), contentAlignment = Alignment.Center) {
        Label(label.localized(), 10, tint, FontWeight.Bold, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// 6 - AI Optimization (AI Optimization)
// ---------------------------------------------------------------------------

@Composable
internal fun AiOptimizationScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    var opportunities by rememberSaveable { mutableIntStateOf(0b111) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    val data = PreviewFixtures.screen(Page.OPTIMIZE, PreviewSource.DESIGN_PREVIEW)
    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.CHIP, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle("AI" ar "تحسين", "Optimization" ar "الذكاء الاصطناعي",
            "Explore performance opportunities with KNOUX AI" ar "استكشف فرص الأداء مع KNOUX AI.",
            "screen-ai-optimization")
        MasterPreviewPill()
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            val metrics = listOf(
                Triple("Performance Analysis" ar "تحليل الأداء", "94 /100", Mark.SPEED),
                Triple("Battery Efficiency" ar "كفاءة البطارية", "+18%", Mark.BATTERY),
                Triple("Memory Pressure" ar "ضغط الذاكرة", "1.6 GB Free", Mark.MEMORY),
                Triple("Thermal Observations" ar "ملاحظات حرارية", "Normal", Mark.TEMP)
            )
            metrics.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    pair.forEachIndexed { columnInRow, spec ->
                        val index = row * 2 + columnInRow
                        Box(Modifier.weight(1f)) {
                            AiMetricCard(spec.first, spec.second, spec.third,
                                if (index == 0) "review-action" else "metric-$index") { sheet = "review" }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Separator(Modifier.weight(.16f).height(1.dp))
                Label(("Optimization Opportunities" ar "فرص التحسين").localized(), 15, weight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(.68f), align = TextAlign.Center)
                Separator(Modifier.weight(.16f).height(1.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OpportunitySphere(Modifier.weight(1f), "Performance" ar "الأداء", Mark.BARS, Cyan,
                    opportunities and 1 != 0, "row-0") { opportunities = opportunities xor 1 }
                OpportunitySphere(Modifier.weight(1f), "Privacy" ar "الخصوصية", Mark.SHIELD, Pink,
                    opportunities and 2 != 0, "row-1") { opportunities = opportunities xor 2 }
                OpportunitySphere(Modifier.weight(1f), "Battery" ar "البطارية", Mark.BATTERY, Cyan,
                    opportunities and 4 != 0, "row-2") { opportunities = opportunities xor 4 }
            }
            Spacer(Modifier.weight(1f))
            AccentButton(Action("See Recommendations" ar "عرض التوصيات", Mark.SPARKLE),
                Modifier.fillMaxWidth(), primary = true, tag = "primary-action") { sheet = "proceed" }
            PreviewNote(("DESIGN PREVIEW — Example metrics and opportunities for visual demonstration only."
                ar "معاينة تصميم — أمثلة ومؤشرات لتوضيح التجربة فقط."))
        }
    }
    sheet?.let { mode ->
        val entries = data?.entries.orEmpty()
        InfoSheet(if (mode == "review") ("Review Opportunities" ar "مراجعة الفرص") else ("See Recommendations" ar "عرض التوصيات"),
            { sheet = null }) {
            when (mode) {
                "review" -> {
                    AccentButton(Action(Page.AI_RECOMMENDATIONS.title, Page.AI_RECOMMENDATIONS.hero),
                        Modifier.fillMaxWidth(), tag = "open-ai-recommendations") {
                        sheet = null
                        open(Page.AI_RECOMMENDATIONS)
                    }
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                }
                "proceed" -> {
                    entries.forEach { Label("${it.title.localized()} · ${it.value}", 13, Mist) }
                    AccentButton(Action("Confirm preview" ar "تأكيد المعاينة", Mark.CHECK), Modifier.fillMaxWidth(),
                        tag = "confirm-preview") { sheet = "ack" }
                }
                else -> Label(NoChange.localized(), 16, Cyan, FontWeight.Bold, Modifier.testTag("preview-only-result"))
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
}

@Composable
private fun AiMetricCard(title: Copy, value: String, mark: Mark, tag: String, onReview: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().heightIn(min = 122.dp).clip(RoundedCornerShape(20.dp))
        .testTag(tag).clickable(role = Role.Button, onClick = onReview), radius = 20, tint = Violet) {
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Label(title.localized(), 14, weight = FontWeight.ExtraBold)
            Label(("Sample metric" ar "مؤشر نموذجي").localized(), 9, Mist)
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MasterIcon(mark, if (mark == Mark.BATTERY) Cyan else Pink, 34)
                Label(value, 16, weight = FontWeight.ExtraBold)
            }
        }
    }
}
