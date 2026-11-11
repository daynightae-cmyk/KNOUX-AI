package com.knoux.ai

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

private enum class DetailDialog { ITEM, REVIEW, CONFIRM, DONE }

@Composable
internal fun DetailPreview(page: Page) {
    val design = detailDesigns.getValue(page)
    var mask by rememberSaveable(page) { mutableIntStateOf(design.selected ?: 0) }
    var filter by rememberSaveable(page) { mutableIntStateOf(0) }
    var category by rememberSaveable(page) { mutableIntStateOf(0) }
    var reverse by rememberSaveable(page) { mutableStateOf(false) }
    var duplicateSort by rememberSaveable(page) { mutableIntStateOf(0) }
    var grid by rememberSaveable(page) { mutableStateOf(true) }
    var dialog by rememberSaveable(page) { mutableStateOf<DetailDialog?>(null) }
    var currentItem by rememberSaveable(page) { mutableIntStateOf(0) }
    val selected = design.rows.indices.filter { mask and (1 shl it) != 0 }.toSet()
    val total = PreviewFixtures.selectedMb(design.rows, selected)
    fun toggle(index: Int) { mask = mask xor (1 shl index) }
    fun open(index: Int) { currentItem = index; dialog = DetailDialog.ITEM }
    val actions: @Composable () -> Unit = {
        AdaptiveActions(
            design.review, design.proceed,
            secondEnabled = design.selected == null || selected.isNotEmpty(),
            onFirst = {
                if (page == Page.DUPLICATE_REVIEW) mask = (1 shl design.rows.size) - 1
                dialog = DetailDialog.REVIEW
            },
            onSecond = { dialog = DetailDialog.CONFIRM }
        )
    }
    val sticky = LocalTall.current
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .testTag("detail-scroll-${page.id}"), horizontalAlignment = Alignment.CenterHorizontally) {
            HeroHeading(page, design.subtitle, artBeside = page == Page.CLEAN_CENTER || page == Page.BATTERY_DIAGNOSTICS)
            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                DetailOverview(page, design)
                when (page) {
                    Page.DUPLICATE_REVIEW -> {
                        DetailChips(listOf(
                            listOf("Group order" ar "ترتيب المجموعات", "Size ↑" ar "الحجم ↑", "Size ↓" ar "الحجم ↓")[duplicateSort],
                            "Filter" ar "فلترة", (if (grid) "Grid" else "List") ar (if (grid) "شبكة" else "قائمة")
                        ), filter) {
                            when (it) {
                                0 -> duplicateSort = (duplicateSort + 1) % 3
                                1 -> filter = if (filter == 1) 0 else 1
                                2 -> grid = !grid
                            }
                        }
                        val indexes = design.rows.indices.filter { filter == 0 || it in selected }
                        val ordered = when (duplicateSort) {
                            1 -> indexes.sortedBy { design.rows[it].mb }
                            2 -> indexes.sortedByDescending { design.rows[it].mb }
                            else -> indexes
                        }
                        ordered.forEach { index ->
                            DuplicateSampleGroup(design.rows[index], index, index in selected, grid, { toggle(index) }, { open(index) })
                        }
                        if (ordered.isEmpty()) Label(("No selected groups" ar "مفيش مجموعات محددة").localized(), 13, Mist)
                    }
                    Page.STORAGE_BREAKDOWN -> StorageDetail(design.rows, category, { category = it }, { open(category) })
                    Page.REPORT_DETAIL -> ReportTrends(design.rows, ::open)
                    else -> {
                        when (page) {
                            Page.PRIVACY_INSPECTOR -> {
                                val chips = listOf("All" ar "الكل", "Ad Trackers" ar "تتبع الإعلانات",
                                    "Location" ar "الموقع", "Clipboard" ar "الحافظة", "Overlay" ar "التراكب", "Notifications" ar "الإشعارات")
                                DetailChips(chips, filter) { filter = it }
                            }
                            Page.SCAN_SUMMARY -> DetailTimeline(false)
                            Page.RESIDUE_REVIEW -> DetailChips(listOf("All (5)" ar "الكل (٥)",
                                "Leftovers" ar "بقايا", "Orphan Folders" ar "مجلدات", "Cache" ar "ذاكرة مؤقتة"), filter) { filter = it }
                            Page.APPS_REVIEW -> InsightPanel("AI Insight" ar "رؤية الذكاء الاصطناعي",
                                "The sample identifies storage, background activity, and startup opportunities." ar
                                    "المثال بيعرض فرص لتحسين التخزين ونشاط الخلفية وبدء التشغيل.", Mark.CHIP)
                            Page.BATTERY_DIAGNOSTICS -> BatteryRanking()
                            Page.AI_RECOMMENDATIONS -> InsightPanel("AI Insight" ar "رؤية الذكاء الاصطناعي",
                                "12 example opportunities could recover 2.4 GB and extend battery life by up to 24%." ar
                                    "١٢ فرصة في المثال ممكن توفّر 2.4 GB وتحسن عمر البطارية لحد ٢٤٪.", Mark.CHIP)
                            else -> Unit
                        }
                        val ordered = when {
                            page == Page.RESIDUE_REVIEW && filter != 0 ->
                                listOf(emptyList(), listOf(0, 4), listOf(2, 3), listOf(1))[filter]
                            page == Page.PRIVACY_INSPECTOR && filter != 0 ->
                                listOf(emptyList(), listOf(0, 1, 2, 4), listOf(0, 1, 3), listOf(1, 2), listOf(1), listOf(4))[filter]
                            page == Page.PRIVACY_INSPECTOR && reverse -> design.rows.indices.reversed().toList()
                            else -> design.rows.indices.toList()
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Label("${design.listHeading.localized()} (${ordered.size})", 16,
                                weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (page == Page.PRIVACY_INSPECTOR) {
                                DetailChip(if (reverse) "Risk ↑" ar "المخاطر ↑" else "Risk ↓" ar "المخاطر ↓", reverse) { reverse = !reverse }
                            }
                            if (page == Page.APPS_REVIEW) {
                                val all = selected.size == design.rows.size
                                SelectChip(("Select All" ar "اختيار الكل").localized(), all) {
                                    mask = if (it) (1 shl design.rows.size) - 1 else 0
                                }
                            }
                        }
                        ordered.forEach { index ->
                            DetailRow(page, design.rows[index], index, if (design.selected == null) null else index in selected,
                                { toggle(index) }, { open(index) })
                        }
                    }
                }
                when (page) {
                    Page.CLEAN_CENTER -> InsightPanel("AI Recommendation" ar "توصية الذكاء الاصطناعي",
                        "The sample selection would recover ${formatMb(total)}." ar
                            "المساحة المقدّرة للاختيار في المثال ${formatMb(total)}.", Mark.CHIP)
                    Page.PRIVACY_INSPECTOR -> InsightPanel("Risk Insights" ar "ملخص المخاطر",
                        "High: 2 · Medium: 2 · Low: 1" ar "مرتفعة: ٢ · متوسطة: ٢ · منخفضة: ١", Mark.SHIELD)
                    Page.SCAN_SUMMARY -> InsightPanel("Suggested Next Actions" ar "الإجراءات التالية",
                        "Free up space · Review privacy · Improve performance" ar "حرّر مساحة · راجع الخصوصية · حسّن الأداء", Mark.BOLT)
                    Page.APPS_REVIEW -> InsightPanel("Smart Actions" ar "إجراءات ذكية",
                        "Review uninstall, hibernation, and startup options for selected sample apps." ar
                            "راجع اختيارات الإزالة والإسبات وبدء التشغيل لتطبيقات المثال المحددة.", Mark.CHIP)
                    Page.AI_RECOMMENDATIONS -> RecommendationNotes()
                    else -> Unit
                }
                if (design.selected != null) {
                    GlassPanel(Modifier.fillMaxWidth(), tint = Cyan) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Glyph(Mark.CHECK, Modifier.size(24.dp), Cyan, true)
                            Label(("${selected.size} of ${design.rows.size} selected" ar
                                "${selected.size} من ${design.rows.size} محدد").localized(), 13, weight = FontWeight.Bold,
                                modifier = Modifier.testTag("detail-selection-total"))
                            if (design.rows.any { it.mb > 0 }) Label(formatMb(total), 16, Pink, FontWeight.Bold)
                        }
                    }
                }
                if (!sticky) actions()
            }
        }
        if (sticky) Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)) { actions() }
    }
    dialog?.let { mode ->
        val entry = design.rows[currentItem]
        val title = when (mode) {
            DetailDialog.ITEM -> entry.title
            DetailDialog.REVIEW -> design.review.label
            DetailDialog.CONFIRM -> design.proceed.label
            DetailDialog.DONE -> "Preview finished" ar "انتهت المعاينة"
        }
        InfoSheet(title, { dialog = null }) {
            when (mode) {
                DetailDialog.ITEM -> {
                    Label(entry.detail.localized(), 14, Mist)
                    Label(entry.value, 18, Pink, FontWeight.Bold)
                    entry.note?.let { Label(it.localized(), 13, Cyan) }
                }
                DetailDialog.REVIEW, DetailDialog.CONFIRM -> {
                    val rows = when {
                        page == Page.DUPLICATE_REVIEW && mode == DetailDialog.REVIEW -> design.rows
                        page == Page.STORAGE_BREAKDOWN -> listOf(design.rows[category])
                        design.selected == null -> design.rows
                        else -> selected.map(design.rows::get)
                    }
                    if (rows.isEmpty()) Label(("No example items selected" ar "مفيش عناصر محددة في المثال").localized(), 13, Mist)
                    if (page == Page.STORAGE_BREAKDOWN && mode == DetailDialog.REVIEW && category == 0) {
                        listOf("Asphalt9_Data.obb · 4.2 GB", "Game_Update_v12.4.apk · 2.8 GB", "Game_Resources.zip · 1.9 GB")
                            .forEach { Label(it, 13) }
                    } else {
                        rows.forEach { Label("${it.title.localized()} · ${it.value}", 13) }
                    }
                    if (mode == DetailDialog.CONFIRM) {
                        AccentButton(Action("Confirm preview" ar "تأكيد المعاينة", Mark.CHECK), Modifier.fillMaxWidth(),
                            tag = "confirm-detail-preview") { dialog = DetailDialog.DONE }
                    }
                }
                DetailDialog.DONE -> Label(NoChange.localized(), 16, Cyan, FontWeight.Bold,
                    Modifier.testTag("detail-preview-result"))
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
}

private fun formatMb(mb: Int) = if (mb >= 1000) String.format(Locale.ROOT, "%.2f GB", mb / 1000.0) else "$mb MB"

@Composable
private fun DetailOverview(page: Page, design: DetailDesign) {
    GlassPanel(Modifier.fillMaxWidth(), radius = 17) {
        Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (page != Page.DUPLICATE_REVIEW) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (design.score != null) {
                        ProgressDonut(design.score, requireNotNull(design.scoreLabel).localized(), 80,
                            displayValue = if (page == Page.CLEAN_CENTER) "2.86 GB" else "${design.score}%")
                    } else IconBadge(page.hero, Cyan, diameter = 43)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Label(design.heading.localized(), 17, weight = FontWeight.ExtraBold)
                        Label(design.summary.localized(), 12, Mist)
                        if (page == Page.STORAGE_BREAKDOWN) GradientBar(.5f, thickness = 9)
                        if (page == Page.AI_RECOMMENDATIONS) Label("94   →   98  ↑ +4", 22, Cyan, FontWeight.ExtraBold)
                    }
                }
            }
            if (design.metrics.isNotEmpty()) MiniMetricStrip(design.metrics, vertical = design.metrics.size >= 4)
        }
    }
}

@Composable
private fun DetailChip(title: Copy, selected: Boolean, action: () -> Unit) {
    Row(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
        .selectable(selected, role = Role.Tab, onClick = action).padding(vertical = 5.dp)
        .glass(14, active = selected, ghost = true).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Label(title.localized(), 11, weight = FontWeight.Bold)
    }
}

@Composable
private fun DetailChips(chips: List<Copy>, active: Int, select: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        chips.forEachIndexed { index, title -> DetailChip(title, active == index) { select(index) } }
    }
}

private val sampleAppIcons = listOf(R.drawable.sample_instagram, R.drawable.sample_facebook,
    R.drawable.sample_whatsapp, R.drawable.sample_tiktok, R.drawable.sample_x)
private val privacyAppIcons = listOf(R.drawable.sample_instagram, R.drawable.sample_tiktok, R.drawable.sample_facebook)

@Composable
private fun DetailRow(page: Page, entry: Entry, index: Int, checked: Boolean?, toggle: () -> Unit, open: () -> Unit) {
    val tone = if (index % 2 == 0) Pink else Cyan
    val destination = if (checked == true) Cyan else tone
    val animated by animateColorAsState(destination, animationSpec = tween(if (LocalPreviewMotion.current) 180 else 0),
        label = "selected rim")
    val itemTitle = entry.title.localized()
    GlassPanel(Modifier.fillMaxWidth().testTag("detail-row-$index"), tint = animated) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 51.dp).padding(start = 7.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = open)
                    .padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    val appIcon = when (page) {
                        Page.APPS_REVIEW -> sampleAppIcons[index]
                        Page.PRIVACY_INSPECTOR -> privacyAppIcons.getOrNull(index)
                        else -> null
                    }
                    if (appIcon != null) Image(painterResource(appIcon), null, Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)))
                    else IconBadge(entry.icon, tone, diameter = 30)
                    Column(Modifier.weight(1f)) {
                        Label(itemTitle, 13, weight = FontWeight.Bold)
                        Label(entry.detail.localized(), 10, Mist)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Label(entry.value, 12, tone, FontWeight.Bold, align = TextAlign.End)
                        entry.note?.let { Label(it.localized(), 9, Mist, align = TextAlign.End) }
                    }
                    if (checked == null) {
                        val mirror = if (LocalArabic.current) 180f else 0f
                        Glyph(Mark.NEXT, Modifier.padding(end = 5.dp).size(15.dp).rotate(mirror))
                    }
                }
                if (checked != null) Box(Modifier.size(48.dp).testTag("detail-select-$index")
                    .semantics { contentDescription = itemTitle }
                    .toggleable(checked, role = Role.Checkbox, onValueChange = { toggle() }),
                    contentAlignment = Alignment.Center) { SelectionIndicator(checked) }
            }
            if (page == Page.RESIDUE_REVIEW) {
                Row(Modifier.fillMaxWidth().padding(start = 9.dp, end = 9.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    NeonPill((if (index == 3) "Needs Review" ar "تحتاج مراجعة" else "Safe to Clean" ar "آمنة في المثال").localized(),
                        if (index == 3) Gold else Mint)
                    DetailChip("Preview" ar "معاينة", false, open)
                }
            }
        }
    }
}

@Composable
private fun DuplicateSampleGroup(entry: Entry, index: Int, selected: Boolean, grid: Boolean, toggle: () -> Unit, open: () -> Unit) {
    val pictures = listOf(R.drawable.sample_selfie, R.drawable.sample_mountains, R.drawable.sample_dog)
    val sizes = listOf(listOf("2.6 MB", "2.5 MB", "2.5 MB", "2.4 MB"),
        listOf("7.9 MB", "7.8 MB", "7.8 MB", "7.6 MB"), listOf("48.2 MB", "48.1 MB", "48.1 MB", "47.9 MB"))
    val groupTitle = entry.title.localized()
    GlassPanel(Modifier.fillMaxWidth().testTag("detail-row-$index"), tint = if (selected) Cyan else Violet) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(48.dp).testTag("detail-select-$index")
                    .semantics { contentDescription = groupTitle }
                    .toggleable(selected, role = Role.Checkbox, onValueChange = { toggle() }),
                    contentAlignment = Alignment.Center) { SelectionIndicator(selected) }
                Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button, onClick = open),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconBadge(entry.icon, Pink, diameter = 30)
                    Column(Modifier.weight(1f)) {
                        Label(groupTitle, 14, weight = FontWeight.Bold)
                        Label(entry.note?.localized().orEmpty(), 10, Mist)
                    }
                    Label(entry.value, 14, Pink, FontWeight.Bold)
                }
            }
            if (grid && index < pictures.size) {
                Row(Modifier.fillMaxWidth().padding(bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    sizes[index].forEachIndexed { copy, size ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1.24f).clip(RoundedCornerShape(7.dp))
                                .glass(7, active = copy == 0).padding(2.dp)) {
                                Image(painterResource(pictures[index]), null,
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                if (copy == 0) Box(Modifier.align(Alignment.TopStart)) {
                                    NeonPill(("Best" ar "الأفضل").localized(), Cyan, solid = false)
                                } else if (selected) Box(Modifier.align(Alignment.TopEnd)) { SelectionIndicator(true) }
                            }
                            Label(size, 10, align = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightPanel(title: Copy, text: Copy, mark: Mark) {
    GlassPanel(Modifier.fillMaxWidth(), tint = Violet) {
        Row(Modifier.fillMaxWidth().padding(9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconBadge(mark, Pink, diameter = 38)
            Column {
                Label(title.localized(), 15, weight = FontWeight.Bold)
                Label(text.localized(), 12, Mist)
            }
        }
    }
}

@Composable
private fun ReportTrends(rows: List<Entry>, open: (Int) -> Unit) {
    rows.take(4).chunked(if (LocalRoomy.current) 1 else 2).forEachIndexed { row, entries ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEachIndexed { column, item ->
                val index = row * entries.size + column
                GlassPanel(Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button) { open(index) },
                    tint = if (index % 2 == 0) Pink else Cyan) {
                    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Label(item.title.localized(), 13, weight = FontWeight.Bold)
                        Label(item.value, 16, Cyan, FontWeight.Bold)
                        Sparkline(index, Modifier.fillMaxWidth().height(48.dp))
                    }
                }
            }
        }
    }
    DetailTimeline(true)
    InsightPanel(rows.last().title, rows.last().detail, Mark.CHIP)
}

@Composable
private fun DetailTimeline(report: Boolean) {
    val steps = if (report) listOf("Scan" ar "الفحص", "Cleanup" ar "التنظيف", "Privacy" ar "الخصوصية", "Optimization" ar "التحسين")
        else listOf("Scan started" ar "بدأ الفحص", "Storage" ar "التخزين", "Apps & privacy" ar "التطبيقات والخصوصية", "Completed" ar "اكتمل")
    GlassPanel(Modifier.fillMaxWidth(), tint = Violet) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label((if (report) "Actions Timeline" ar "تسلسل الإجراءات" else "Scan Timeline" ar "تسلسل الفحص").localized(),
                15, weight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                steps.forEachIndexed { index, step ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Glyph(listOf(Mark.RADAR, Mark.DISK, Mark.SHIELD, Mark.CHECK)[index], Modifier.size(23.dp), Cyan, true)
                        Label("10:${if (report) 21 + index else 22 + index}", 10, Pink, FontWeight.Bold)
                        Label(step.localized(), 10, Mist, align = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageDetail(rows: List<Entry>, category: Int, select: (Int) -> Unit, open: () -> Unit) {
    val entry = rows[category]
    val shares = listOf(38, 29, 5, 7, 3, 17)
    GlassPanel(Modifier.fillMaxWidth(), tint = Violet) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ProgressDonut(50, ("Used Space" ar "المساحة المستخدمة").localized(), 86, split = true, displayValue = "128 GB")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label(("Category Detail" ar "تفاصيل الفئة").localized(), 13, weight = FontWeight.Bold)
                Label(entry.title.localized(), 16, Pink, FontWeight.ExtraBold, Modifier.testTag("storage-category-detail"))
                Label(entry.detail.localized(), 11, Mist)
                Label("${entry.value}  ·  ${shares[category]}%", 14, Cyan, FontWeight.Bold)
            }
        }
    }
    Label(("All Categories" ar "كل الفئات").localized(), 16, weight = FontWeight.Bold)
    DetailChips(rows.map { it.title }, category, select)
    GlassPanel(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = open), tint = Cyan) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconBadge(entry.icon, Cyan)
            Label(entry.title.localized(), 14, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Label(entry.value, 15, Pink, FontWeight.Bold)
        }
    }
    InsightPanel("Cleanup Suggestions" ar "اقتراحات التنظيف",
        "Review cache, unused apps, and old downloads in the sample." ar
            "راجع الذاكرة المؤقتة والتطبيقات غير المستخدمة والتنزيلات القديمة في المثال.", Mark.BROOM)
}

@Composable
private fun BatteryRanking() {
    val apps = listOf("YouTube" to 24.8f, "Instagram" to 18.9f, "Maps" to 11.7f, "Spotify" to 8.4f, "Facebook" to 6.1f)
    GlassPanel(Modifier.fillMaxWidth(), tint = Violet) {
        Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Label(("Drain Ranking by App" ar "ترتيب استهلاك التطبيقات").localized(), 15, weight = FontWeight.Bold)
            apps.forEach { (app, share) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label(app, 11, modifier = Modifier.width(63.dp))
                    GradientBar(share / 26f, Modifier.weight(1f), thickness = 5)
                    Label("$share%", 11, Pink, FontWeight.Bold)
                }
            }
        }
    }
    MiniMetricStrip(listOf(Metric("37°C", "Thermal status" ar "الحالة الحرارية", Mark.TEMP),
        Metric("5h 18m", "Screen time" ar "وقت الشاشة", Mark.CLOCK),
        Metric("18 apps", "Background activity" ar "نشاط الخلفية", Mark.LAYERS)), vertical = true)
}

@Composable
private fun RecommendationNotes() {
    InsightPanel("Safe Actions Ranking" ar "ترتيب الإجراءات",
        "1. Cache · 2. Background apps · 3. Startup · 4. Memory · 5. Thermal policy" ar
            "١. الذاكرة المؤقتة · ٢. الخلفية · ٣. بدء التشغيل · ٤. الذاكرة · ٥. الحرارة", Mark.TROPHY)
    InsightPanel("Why It’s Safe" ar "أمان المعاينة",
        "Review the suggested actions before applying. Preview choices are reversible." ar
            "راجع الإجراءات المقترحة قبل التطبيق. اختيارات المعاينة قابلة للتراجع.", Mark.SHIELD)
}
