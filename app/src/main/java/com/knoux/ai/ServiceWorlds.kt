package com.knoux.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ten distinct in-product service studios. Each scene is a real, selectable,
 * accessible Compose UI; the data remains expressly a design preview.
 *
 * Existing DetailPreview rows, selections, sheets, and tests remain in place
 * underneath these independent service-specific visual workspaces.
 */
@Composable
internal fun ServiceWorld(page: Page, selectedCount: Int, totalCount: Int, selectedMb: Int) {
    when (page) {
        Page.CLEAN_CENTER -> CleanWorld(selectedCount, totalCount, selectedMb)
        Page.DUPLICATE_REVIEW -> DuplicateWorld()
        Page.PRIVACY_INSPECTOR -> PrivacyWorld()
        Page.SCAN_SUMMARY -> ScanWorld()
        Page.REPORT_DETAIL -> ReportsWorld()
        Page.RESIDUE_REVIEW -> ResidueWorld()
        Page.APPS_REVIEW -> AppsWorld()
        Page.STORAGE_BREAKDOWN -> StorageWorld()
        Page.BATTERY_DIAGNOSTICS -> BatteryWorld()
        Page.AI_RECOMMENDATIONS -> AiWorld()
        else -> Unit
    }
}

@Composable
private fun WT(en: String, ar: String, size: Int = 12, tint: Color = Chalk,
               weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier,
               align: TextAlign? = null) {
    Label(if (LocalArabic.current) ar else en, size, tint, weight, modifier, align)
}

@Composable
private fun Studio(page: Page, label: String, labelAr: String, color: Color,
                   content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
        .testTag("service-world-" + page.id), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(page.hero, color, diameter = 30)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                WT(label.uppercase(), labelAr, 12, color, FontWeight.ExtraBold)
                WT("INTERACTIVE SERVICE STUDIO", "استوديو الخدمة التفاعلي", 9, Mist)
            }
            Box(Modifier.clip(RoundedCornerShape(11.dp)).testTag("design-preview")
                .background(color.copy(alpha = .16f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                WT("PREVIEW", "معاينة", 9, color, FontWeight.Bold)
            }
        }
        GlassPanel(Modifier.fillMaxWidth(), radius = 22, tint = color) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                content()
                Spacer(Modifier.height(1.dp))
                WT("DESIGN PREVIEW  •  No device action is performed",
                    "معاينة تصميم  •  لا يتم تنفيذ أي إجراء على الجهاز",
                    10, Mist, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ChoiceBar(options: List<Pair<String, String>>, chosen: Int, color: Color,
                      onChoose: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        options.forEachIndexed { index, pair ->
            val picked = chosen == index
            Box(Modifier.weight(1f).heightIn(min = 48.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (picked) color.copy(alpha = .24f) else Color.White.copy(alpha = .07f))
                .selectable(selected = picked, role = Role.Tab) { onChoose(index) }
                .testTag("studio-choice-" + index).padding(horizontal = 5.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                WT(pair.first, pair.second, 11, if (picked) color else Mist,
                    if (picked) FontWeight.ExtraBold else FontWeight.Medium, align = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SceneTitle(value: String, subtitle: String, valueAr: String,
                       subtitleAr: String, color: Color = Chalk) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        WT(value, valueAr, 23, color, FontWeight.ExtraBold)
        WT(subtitle, subtitleAr, 11, Mist)
    }
}

@Composable
private fun MeterArt(color: Color, progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 8.dp.toPx()
        val pad = stroke * 1.5f
        val frame = Size(size.width - pad * 2, size.height - pad * 2)
        drawArc(color.copy(alpha = .15f), -90f, 360f, false, Offset(pad, pad), frame,
            style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(pad, pad), frame,
            style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color.White.copy(alpha = .36f), -100f, 75f, false,
            Offset(pad * 2, pad * 2), Size(frame.width - pad * 2, frame.height - pad * 2),
            style = Stroke(1.2.dp.toPx()))
        repeat(8) { i ->
            val rad = (i * 45.0 - 90) * PI / 180.0
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * .37f
            drawCircle(if (i % 2 == 0) color else Color.White,
                radius = 1.8.dp.toPx(), center = Offset(center.x + (cos(rad) * r).toFloat(),
                    center.y + (sin(rad) * r).toFloat()))
        }
    }
}

@Composable
private fun CleanWorld(selected: Int, total: Int, mb: Int) {
    var focus by rememberSaveable { mutableIntStateOf(0) }
    Studio(Page.CLEAN_CENTER, "Clean reactor", "مفاعل التنظيف", Mint) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                MeterArt(Mint, selected.toFloat() / total.coerceAtLeast(1), Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WT(mb.toString() + " MB", mb.toString() + " م.ب", 19, Mint, FontWeight.ExtraBold)
                    WT("SELECTED", "المحدد", 10, Mist)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SceneTitle(selected.toString() + " / " + total,
                    "Recovery lanes selected", selected.toString() + " / " + total,
                    "مسارات الاستعادة المحددة", Mint)
                GradientBar(selected.toFloat() / total.coerceAtLeast(1), thickness = 9)
                WT("Magnetic cleanup matrix", "مصفوفة التنظيف المغناطيسية", 12, Chalk, FontWeight.Bold)
            }
        }
        ChoiceBar(listOf("Junk" to "المهملات", "Temporary" to "المؤقتة", "Downloads" to "التحميلات"),
            focus, Mint) { focus = it }
        WT(listOf("Junk groups remain under your control.",
            "Temporary items are sample-only.",
            "Downloads are never touched by this preview.")[focus],
            listOf("يمكنك التحكم في مجموعات المهملات.",
                "الملفات المؤقتة المعروضة أمثلة فقط.",
                "المعاينة لا تلمس ملفات التحميل.")[focus], 12, Mist)
    }
}

@Composable
private fun DuplicateWorld() {
    var group by rememberSaveable { mutableIntStateOf(0) }
    var comparison by rememberSaveable { mutableStateOf(false) }
    Studio(Page.DUPLICATE_REVIEW, "Mirror lab", "معمل الصور المتطابقة", Pink) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(2) { side ->
                Box(Modifier.weight(1f).height(92.dp).clip(RoundedCornerShape(17.dp))
                    .background(Brush.linearGradient(if (side == 0)
                        listOf(Color(0xffb932e5), Color(0xff221359), Color(0xff14a5c9))
                    else listOf(Color(0xff1444a3), Color(0xff44195e), Color(0xffd64ba6)))))
                {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = if (side == 0) Pink else Cyan
                        repeat(6) { index ->
                            val w = size.width * .48f
                            drawCircle(c.copy(alpha = .08f + index * .035f),
                                radius = w * (1f - index * .12f),
                                center = Offset(size.width * .56f, size.height * .48f))
                        }
                        drawLine(Color.White.copy(alpha = .5f),
                            Offset(size.width * .18f, size.height * .78f),
                            Offset(size.width * .78f, size.height * .2f), 2.dp.toPx())
                    }
                    WT(if (side == 0) "ORIGINAL" else if (comparison) "DIFFERENCE" else "MATCH",
                        if (side == 0) "الأصل" else if (comparison) "الاختلاف" else "المطابق",
                        10, Color.White, FontWeight.ExtraBold,
                        Modifier.align(Alignment.BottomStart).padding(10.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SceneTitle("98.7%", "Visual similarity / sample",
                    "٩٨٫٧٪", "التشابه المرئي / مثال", Pink)
            }
            WT(if (comparison) "DIFF ON" else "PAIR VIEW",
                if (comparison) "إظهار الفرق" else "عرض الزوج", 11, Cyan, FontWeight.Bold,
                Modifier.clip(RoundedCornerShape(12.dp)).clickable { comparison = !comparison }
                    .padding(12.dp).testTag("studio-duplicate-compare"))
        }
        ChoiceBar(listOf("Photos" to "الصور", "Videos" to "الفيديو", "Docs" to "المستندات"),
            group, Pink) { group = it }
    }
}

@Composable
private fun PrivacyWorld() {
    var layer by rememberSaveable { mutableIntStateOf(0) }
    Studio(Page.PRIVACY_INSPECTOR, "Privacy shield", "درع الخصوصية", Cyan) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(112.dp)) {
                val ctr = Offset(size.width / 2f, size.height / 2f)
                repeat(4) { i ->
                    val r = size.minDimension * (.14f + i * .105f)
                    drawCircle(Cyan.copy(alpha = 1f - i * .21f), r, ctr,
                        style = Stroke((if (i == layer) 2.8f else 1.1f).dp.toPx()))
                }
                drawLine(Cyan.copy(alpha = .45f), Offset(ctr.x, 0f),
                    Offset(ctr.x, size.height), 1.dp.toPx())
                drawLine(Cyan.copy(alpha = .45f), Offset(0f, ctr.y),
                    Offset(size.width, ctr.y), 1.dp.toPx())
                drawArc(Brush.sweepGradient(listOf(Cyan.copy(alpha = .7f), Color.Transparent)),
                    -100f, 68f, true, Offset(8f, 8f),
                    Size(size.width - 16f, size.height - 16f))
                drawCircle(Pink, 4.dp.toPx(),
                    Offset(ctr.x + size.width * .18f, ctr.y - size.height * .15f))
                drawCircle(Gold, 3.dp.toPx(),
                    Offset(ctr.x - size.width * .29f, ctr.y + size.height * .14f))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SceneTitle(listOf("02", "02", "01")[layer], "Example risk signatures",
                    listOf("٠٢", "٠٢", "٠١")[layer], "إشارات مخاطر تجريبية", Cyan)
                WT(listOf("HIGH PRIORITY", "MEDIUM PRIORITY", "LOW PRIORITY")[layer],
                    listOf("أولوية عالية", "أولوية متوسطة", "أولوية منخفضة")[layer],
                    12, listOf(Pink, Gold, Mint)[layer], FontWeight.Bold)
                WT("Permissions • Trackers • Overlays",
                    "الصلاحيات • المتعقبات • النوافذ العائمة", 10, Mist)
            }
        }
        ChoiceBar(listOf("High" to "عالية", "Medium" to "متوسطة", "Low" to "منخفضة"),
            layer, Cyan) { layer = it }
    }
}

@Composable
private fun ScanWorld() {
    var phase by rememberSaveable { mutableIntStateOf(0) }
    val motion = LocalPreviewMotion.current
    val ticker = if (motion) rememberInfiniteTransition(label = "scan world") else null
    val pulse = ticker?.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "scan sweep")?.value ?: .38f
    Studio(Page.SCAN_SUMMARY, "Quantum scan", "ماسح كمي تفاعلي", Violet) {
        Box(Modifier.fillMaxWidth().height(104.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val mid = Offset(size.width / 2f, size.height / 2f)
                val radius = size.height * .41f
                repeat(4) { i ->
                    drawCircle(Violet.copy(alpha = .25f + i * .11f),
                        radius * (1f - i * .19f), mid, style = Stroke(1.5.dp.toPx()))
                }
                val x = size.width * (.15f + .70f * pulse)
                drawLine(Cyan.copy(alpha = .65f), Offset(x, 5f), Offset(x, size.height - 5f), 3.dp.toPx())
                repeat(24) { i ->
                    val px = size.width * (i / 24f)
                    drawLine(Pink.copy(alpha = if (i % 3 == 0) .3f else .11f),
                        Offset(px, size.height * .12f),
                        Offset(px, size.height * .9f), 1.dp.toPx())
                }
                drawCircle(Cyan.copy(alpha = .75f), 7.dp.toPx(), mid, style = Stroke(2.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WT("SCAN / 03", "فحص / ٠٣", 22, Color.White, FontWeight.ExtraBold)
                WT("SIMULATION", "محاكاة", 10, Cyan)
            }
        }
        ChoiceBar(listOf("Discover" to "اكتشاف", "Classify" to "تصنيف", "Review" to "مراجعة"),
            phase, Violet) { phase = it }
        WT(listOf("Discover sample traces on an illustrated grid.",
            "Sort the example signals into categories.",
            "Inspect the preview results before any real action.")[phase],
            listOf("استكشف مؤشرات تجريبية على الشبكة.",
                "صنف الإشارات التوضيحية حسب الفئات.",
                "راجع نتائج المعاينة قبل أي إجراء حقيقي.")[phase], 12, Mist)
    }
}

@Composable
private fun ReportsWorld() {
    var range by rememberSaveable { mutableIntStateOf(0) }
    val sets = listOf(listOf(.35f, .45f, .59f, .55f, .75f, .7f, .93f),
        listOf(.40f, .63f, .50f, .70f, .54f, .83f, .91f),
        listOf(.30f, .55f, .66f, .62f, .82f, .79f, .96f))
    Studio(Page.REPORT_DETAIL, "Signal observatory", "مرصد الأداء الذكي", Gold) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SceneTitle("+18.4%", "Illustrative performance trend",
                    "+١٨٫٤٪", "اتجاه أداء توضيحي", Gold)
            }
            WT("KNOUX / INSIGHTS", "كنوكس / التحليلات", 10, Mist)
        }
        Canvas(Modifier.fillMaxWidth().height(108.dp)) {
            val pad = 8.dp.toPx()
            repeat(4) { line ->
                val y = size.height * (.17f + line * .22f)
                drawLine(Color.White.copy(alpha = .12f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            sets[range].forEachIndexed { index, value ->
                val barWidth = (size.width - pad * 10) / 7
                val x = pad + index * (barWidth + pad)
                val h = (size.height - pad * 2) * value
                drawRoundRect(Brush.verticalGradient(listOf(Gold, Pink.copy(alpha = .65f))),
                    Offset(x, size.height - h), Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3))
            }
        }
        ChoiceBar(listOf("7 days" to "٧ أيام", "30 days" to "٣٠ يوم", "90 days" to "٩٠ يوم"),
            range, Gold) { range = it }
    }
}

@Composable
private fun ResidueWorld() {
    var node by rememberSaveable { mutableIntStateOf(0) }
    val names = listOf("App remnants", "Orphan folders", "Residual cache")
    val arNames = listOf("بقايا التطبيقات", "المجلدات اليتيمة", "ملفات الكاش")
    Studio(Page.RESIDUE_REVIEW, "Forensic trail", "مسار الأدلة المتبقية", Violet) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.width(54.dp).height(140.dp)) {
                val axis = size.width * .5f
                drawLine(Violet.copy(alpha = .6f),
                    Offset(axis, size.height * .13f),
                    Offset(axis, size.height * .86f), 2.4.dp.toPx())
                repeat(3) { index ->
                    val cy = size.height * (.18f + index * .32f)
                    val tint = if (index == node) Cyan else Violet
                    drawCircle(tint.copy(alpha = .17f), 19.dp.toPx(), Offset(axis, cy))
                    drawCircle(tint, 8.dp.toPx(), Offset(axis, cy))
                    drawCircle(Color.White.copy(alpha = .72f), 3.dp.toPx(), Offset(axis, cy))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                names.forEachIndexed { index, name ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (node == index) Violet.copy(alpha = .18f) else Color.White.copy(alpha = .035f))
                        .clickable { node = index }
                        .testTag("studio-residue-node-" + index)
                        .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        WT(name, arNames[index], 13,
                            if (index == node) Cyan else Mist,
                            if (index == node) FontWeight.Bold else FontWeight.Normal,
                            Modifier.weight(1f))
                        WT(listOf("01", "03", "08")[index], listOf("٠١", "٠٣", "٠٨")[index], 12, Gold)
                    }
                }
            }
        }
        WT("Trail / " + names[node] + " / selected for review",
            "المسار / " + arNames[node] + " / للمراجعة", 11, Mist)
    }
}

@Composable
private fun AppsWorld() {
    var view by rememberSaveable { mutableIntStateOf(0) }
    var activeApp by rememberSaveable { mutableIntStateOf(0) }
    val apps = listOf("Social", "Media", "Utilities")
    val appsAr = listOf("تواصل", "وسائط", "أدوات")
    val marks = listOf(Mark.GROUP, Mark.VIDEO, Mark.GRID)
    Studio(Page.APPS_REVIEW, "App command center", "مركز إدارة التطبيقات", Cyan) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SceneTitle("03", "Sample application groups", "٠٣",
                "مجموعات تطبيقات توضيحية", Cyan)
            Spacer(Modifier.weight(1f))
            WT("APP INTELLIGENCE", "ذكاء التطبيقات", 10, Mist)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            apps.forEachIndexed { index, app ->
                val active = activeApp == index
                Column(Modifier.weight(1f).heightIn(min = 88.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) Cyan.copy(alpha = .16f) else Color.White.copy(alpha = .055f))
                    .clickable { activeApp = index }
                    .testTag("studio-app-tile-" + index).padding(9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    IconBadge(marks[index], if (active) Cyan else Violet, diameter = 39)
                    WT(app, appsAr[index], 12, if (active) Cyan else Mist, FontWeight.Bold)
                }
            }
        }
        ChoiceBar(listOf("Storage" to "التخزين", "Activity" to "النشاط",
            "Permissions" to "الصلاحيات"), view, Cyan) { view = it }
        WT(listOf("Sample storage footprint: 1.8 GB", "Background activity: preview",
            "App permissions: preview")[view],
            listOf("الحجم التوضيحي: ١٫٨ جيجابايت", "نشاط الخلفية: معاينة",
                "صلاحيات التطبيق: معاينة")[view], 12, Mist)
    }
}

@Composable
private fun StorageWorld() {
    var segment by rememberSaveable { mutableIntStateOf(0) }
    val palette = listOf(Pink, Cyan, Violet, Gold, Mint)
    val ratios = listOf(.35f, .26f, .18f, .12f, .09f)
    Studio(Page.STORAGE_BREAKDOWN, "Space atlas", "أطلس التخزين", Pink) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(122.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    var start = -90f
                    ratios.forEachIndexed { index, ratio ->
                        val angle = ratio * 360f - 3f
                        val width = if (index == segment) 18.dp.toPx() else 12.dp.toPx()
                        val pad = 17.dp.toPx()
                        drawArc(palette[index], start, angle, false,
                            Offset(pad, pad), Size(size.width - pad * 2, size.height - pad * 2),
                            style = Stroke(width, cap = StrokeCap.Round))
                        start += ratio * 360f
                    }
                    drawCircle(Color.White.copy(alpha = .08f),
                        size.minDimension * .26f, Offset(size.width / 2, size.height / 2))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WT("128 GB", "١٢٨ جيجا", 19, Color.White, FontWeight.ExtraBold)
                    WT("EXAMPLE", "توضيحي", 9, Mist)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SceneTitle(listOf("45 GB", "33 GB", "23 GB")[segment],
                    listOf("Applications", "Media library", "Other files")[segment],
                    listOf("٤٥ جيجا", "٣٣ جيجا", "٢٣ جيجا")[segment],
                    listOf("التطبيقات", "مكتبة الوسائط", "ملفات أخرى")[segment],
                    palette[segment])
                WT("SPACE DISTRIBUTION", "توزيع مساحة التخزين", 10, Mist)
                GradientBar(ratios[segment], thickness = 9)
            }
        }
        ChoiceBar(listOf("Apps" to "تطبيقات", "Media" to "وسائط", "Other" to "أخرى"),
            segment, Pink) { segment = it }
    }
}

@Composable
private fun BatteryWorld() {
    var mode by rememberSaveable { mutableIntStateOf(1) }
    val levels = listOf(.86f, .72f, .58f)
    Studio(Page.BATTERY_DIAGNOSTICS, "Energy pulse", "نبض الطاقة", Mint) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(102.dp).height(126.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val pad = 12.dp.toPx()
                    val corner = androidx.compose.ui.geometry.CornerRadius(19.dp.toPx())
                    drawRoundRect(Mint.copy(alpha = .7f), Offset(pad, h * .12f),
                        Size(w - 2 * pad, h * .81f), corner, style = Stroke(2.dp.toPx()))
                    drawRoundRect(Mint, Offset(w * .37f, h * .07f),
                        Size(w * .26f, h * .055f), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                    val meter = levels[mode]
                    val base = h * .9f
                    val fillH = h * .7f * meter
                    drawRoundRect(Brush.verticalGradient(listOf(Mint.copy(alpha = .93f),
                            Cyan.copy(alpha = .65f))), Offset(pad * 1.6f, base - fillH),
                        Size(w - pad * 3.2f, fillH),
                        androidx.compose.ui.geometry.CornerRadius(11.dp.toPx()))
                    repeat(4) { i ->
                        val y = h * (.25f + i * .16f)
                        drawLine(Color.White.copy(alpha = .32f), Offset(w * .31f, y),
                            Offset(w * .7f, y), 1.dp.toPx())
                    }
                }
                WT(listOf("86%", "72%", "58%")[mode],
                    listOf("٨٦٪", "٧٢٪", "٥٨٪")[mode], 23, Color.White, FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SceneTitle("6h 42m", "Illustrative remaining time", "٦ س ٤٢ د",
                    "زمن متبقٍ توضيحي", Mint)
                WT(listOf("Efficiency profile", "Balanced profile", "Saver profile")[mode],
                    listOf("وضع الكفاءة", "الوضع المتوازن", "وضع التوفير")[mode],
                    12, Gold, FontWeight.Bold)
                GradientBar(levels[mode], thickness = 9)
                WT("Energy flow simulation", "محاكاة تدفق الطاقة", 11, Mist)
            }
        }
        ChoiceBar(listOf("Performance" to "أداء", "Balanced" to "متوازن",
            "Eco" to "موفر"), mode, Mint) { mode = it }
    }
}

@Composable
private fun AiWorld() {
    var focus by rememberSaveable { mutableIntStateOf(0) }
    val network = listOf(
        Offset(.50f, .18f), Offset(.20f, .43f), Offset(.80f, .43f),
        Offset(.31f, .79f), Offset(.69f, .79f))
    Studio(Page.AI_RECOMMENDATIONS, "Neural constellation", "كوكبة التوصيات الذكية", Violet) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SceneTitle("94 → 98", "Illustrative opportunity index",
                    "٩٤ ← ٩٨", "مؤشر فرص توضيحي", Pink)
                WT("AI / INTELLIGENCE LAYER", "طبقة التحليل الذكي", 10, Mist)
            }
            WT("+4", "+٤", 25, Mint, FontWeight.ExtraBold)
        }
        Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val pts = network.map { Offset(it.x * size.width, it.y * size.height) }
                listOf(0 to 1, 0 to 2, 1 to 3, 2 to 4, 3 to 4, 1 to 2, 0 to 3, 0 to 4)
                    .forEach { (a, b) ->
                        drawLine(Brush.linearGradient(listOf(Violet.copy(alpha = .42f), Cyan.copy(alpha = .36f))),
                            pts[a], pts[b], 2.dp.toPx())
                    }
                pts.forEachIndexed { index, pt ->
                    val tint = if (index == focus + 1) Pink else if (index == 0) Cyan else Violet
                    drawCircle(tint.copy(alpha = .17f),
                        (if (index == focus + 1) 22 else 16).dp.toPx(), pt)
                    drawCircle(tint, (if (index == focus + 1) 9 else 6).dp.toPx(), pt)
                    drawCircle(Color.White, 2.dp.toPx(), pt)
                }
            }
            WT("KNOUX AI", "كنوكس AI", 13, Color.White, FontWeight.ExtraBold,
                Modifier.align(Alignment.Center).padding(top = 15.dp))
        }
        ChoiceBar(listOf("Clean" to "تنظيف", "Privacy" to "خصوصية",
            "Speed" to "سرعة"), focus, Violet) { focus = it }
        WT(listOf("Prioritize sample cleanup opportunities.",
            "Review the example privacy improvements.",
            "Explore performance recommendations.")[focus],
            listOf("رتّب فرص التنظيف التوضيحية.",
                "راجع تحسينات الخصوصية التجريبية.",
                "استكشف توصيات تحسين الأداء.")[focus], 12, Mist)
    }
}
