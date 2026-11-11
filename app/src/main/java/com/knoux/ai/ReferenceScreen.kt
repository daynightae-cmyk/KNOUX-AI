package com.knoux.ai

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal const val PANEL_DETAIL = "detail"
internal const val PANEL_REVIEW = "review"
internal const val PANEL_PROCEED = "proceed"
internal const val PANEL_ACK = "ack"

@Composable
internal fun ReferenceScreen(
    page: Page, source: PreviewSource = PreviewSource.DESIGN_PREVIEW, openReview: (Page) -> Unit = {}
) {
    val data = PreviewFixtures.screen(page, source)
    if (data == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroHeading(page, "Device information is unavailable" ar "بيانات الجهاز غير متاحة", false)
            Label(("No device measurements are available." ar "لا توجد قياسات متاحة من الجهاز.").localized(), 15, Mist)
        }
        return
    }
    val all = (1 shl data.entries.size) - 1
    var mask by rememberSaveable(page.id) { mutableIntStateOf(all) }
    var paused by rememberSaveable(page.id) { mutableStateOf(false) }
    var sheet by rememberSaveable(page.id) { mutableStateOf<String?>(null) }
    var entryIndex by rememberSaveable(page.id) { mutableIntStateOf(0) }
    val selection = data.entries.indices.filter { mask and (1 shl it) != 0 }.toSet()
    fun toggle(index: Int) { mask = mask xor (1 shl index) }
    fun detail(index: Int) { entryIndex = index; sheet = PANEL_DETAIL }
    val roomy = LocalRoomy.current
    val selectable = data.selection == Selection.OPTIMIZATION || data.selection == Selection.RESIDUE
    val stickyActions = LocalTall.current
    val actions: @Composable () -> Unit = {
        if (page == Page.SCAN) {
            AdaptiveActions(data.review, data.proceed, !paused, paused, { paused = true }, { paused = false })
        } else {
            AdaptiveActions(data.review, data.proceed,
                secondEnabled = selection.isNotEmpty() || data.selection == Selection.NONE,
                onFirst = { sheet = PANEL_REVIEW }, onSecond = { sheet = PANEL_PROCEED })
        }
    }
    Column(Modifier.fillMaxSize()) {
      Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        HeroHeading(page, data.subtitle, !paused)
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val primary: @Composable () -> Unit = {
                when (page) {
                    Page.SCAN -> ScanProgress(paused)
                    Page.STORAGE -> StorageCapacity()
                    else -> MetricPairPanel(data.primary, page == Page.PRIVACY)
                }
            }
            if (data.grouped) GlassPanel(Modifier.fillMaxWidth()) {
                Column {
                    primary()
                    GradientBar(0f, Modifier.padding(horizontal = 7.dp), thickness = 1)
                    MiniMetricStrip(data.compact, vertical = page == Page.STORAGE)
                }
            } else {
                GlassPanel(Modifier.fillMaxWidth()) { primary() }
                GlassPanel(Modifier.fillMaxWidth()) { MiniMetricStrip(data.compact) }
            }
            StatusSummaryPanel(data, page)
            if (data.section != null) {
                val trackerCount = if (0 in selection) 7 else 0
                val mb = PreviewFixtures.selectedMb(data.entries, selection)
                val chip = when (data.selection) {
                    Selection.NONE -> null
                    Selection.TRACKERS -> "Select All ($trackerCount Trackers)" ar "اختيار الكل ($trackerCount أدوات تتبع)"
                    Selection.RESIDUE -> "Select All ($mb MB)" ar "اختيار الكل ($mb MB)"
                    Selection.OPTIMIZATION -> "Enable All" ar "تفعيل الكل"
                }
                if (roomy) {
                    Label(data.section.localized(), 16, weight = FontWeight.Bold)
                    chip?.let { SelectChip(it.localized(), mask == all) { checked -> mask = if (checked) all else 0 } }
                } else Row(Modifier.fillMaxWidth().padding(start = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Label(data.section.localized(), 15, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    chip?.let { SelectChip(it.localized(), mask == all) { checked -> mask = if (checked) all else 0 } }
                }
            }
            if (page == Page.STORAGE) {
                data.entries.chunked(if (roomy) 1 else 2).forEachIndexed { row, items ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items.forEachIndexed { column, entry ->
                            val index = if (roomy) row else row * 2 + column
                            CategoryCard(entry, index, Modifier.weight(1f).fillMaxHeight(), "category-$index") { detail(index) }
                        }
                    }
                }
            } else data.entries.forEachIndexed { i, entry ->
                FeatureRow(page, entry, i, selectable, i in selection,
                    action = { if (selectable) toggle(i) else detail(i) })
            }
            if (page == Page.SCAN) {
                AccentButton(Action(Page.SCAN_SUMMARY.title, Mark.BARS), Modifier.fillMaxWidth(),
                    primary = false, tag = "open-deep-scan-summary") { openReview(Page.SCAN_SUMMARY) }
            }
            if (!stickyActions) {
                Spacer(Modifier.height(3.dp))
                actions()
            }
            Spacer(Modifier.height(5.dp))
        }
      }
      if (stickyActions) {
          Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)) { actions() }
      }
    }
    val mode = sheet
    if (mode != null) {
        val entry = data.entries[entryIndex]
        val title = when (mode) {
            PANEL_DETAIL -> entry.title
            PANEL_REVIEW -> data.review.label
            PANEL_PROCEED -> data.proceed.label
            else -> "Preview finished" ar "انتهت المعاينة"
        }
        InfoSheet(title, { sheet = null }) {
            when (mode) {
                PANEL_DETAIL -> {
                    Label(entry.detail.localized(), 14, Mist)
                    Label(entry.value, 18, Pink, FontWeight.Bold)
                    entry.note?.let { Label(it.localized(), 12, Mist) }
                    if (page == Page.PRIVACY) PreviewChoice(entry, entryIndex in selection, "detail-select") { toggle(entryIndex) }
                }
                PANEL_REVIEW, PANEL_PROCEED -> {
                    if (mode == PANEL_REVIEW) page.reviewPage?.let { next ->
                        AccentButton(Action(next.title, next.hero), Modifier.fillMaxWidth(), tag = "open-${next.id}") {
                            sheet = null
                            openReview(next)
                        }
                    }
                    val allowSelection = page == Page.DUPLICATES && mode == PANEL_PROCEED || page == Page.PRIVACY
                    data.entries.forEachIndexed { i, item ->
                        if (allowSelection) PreviewChoice(item, i in selection, "sheet-choice-$i") { toggle(i) }
                        else if (data.selection == Selection.NONE || i in selection) {
                            Label("${item.title.localized()} · ${item.value}", 13, Mist)
                        }
                    }
                    if (data.selection != Selection.NONE && selection.isEmpty()) {
                        Label(("No sample items selected" ar "لم تختر أي عناصر من المثال").localized(), 13, Mist)
                    }
                    if (mode == PANEL_PROCEED) {
                        AccentButton(Action("Confirm preview" ar "تأكيد المعاينة", Mark.CHECK),
                            Modifier.fillMaxWidth(), enabled = selection.isNotEmpty(), tag = "confirm-preview") { sheet = PANEL_ACK }
                    }
                }
                PANEL_ACK -> {
                    val message = if (page == Page.RESIDUE) "No cleanup performed — UI preview only" ar "لم يحدث أي تنظيف — معاينة للواجهة فقط" else NoChange
                    Label(message.localized(), 16, Cyan, FontWeight.Bold, Modifier.testTag("preview-only-result"))
                }
            }
            Label(Disclosure.localized(), 12, Mist)
        }
    }
}

@Composable
private fun PreviewChoice(entry: Entry, selected: Boolean, tag: String, toggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag).clip(RoundedCornerShape(12.dp))
        .glass(12).toggleable(selected, role = Role.Checkbox, onValueChange = { toggle() }).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectionIndicator(selected)
        Label(entry.title.localized(), 13, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Label(entry.value, 12, Pink, FontWeight.Bold)
    }
}

@Composable
private fun ScanProgress(paused: Boolean) {
    val pulse = if (!paused && LocalPreviewMotion.current) {
        val transition = rememberInfiniteTransition(label = "scan pulse")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse), label = "scan pulse")
    } else null
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProgressDonut(72, if (LocalRoomy.current) "" else ("Scan Progress" ar "تقدم الفحص").localized(), live = !paused)
        Separator(Modifier.width(1.dp).height(50.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val label = if (paused) "Preview paused" ar "المعاينة متوقفة"
                else "Analyzing device state" ar "تحليل حالة الجهاز"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).background(Cyan.copy(alpha = .32f + .68f * (pulse?.value ?: 1f)), RoundedCornerShape(4.dp)))
                Label(label.localized(), 15, weight = FontWeight.Bold, modifier = Modifier.testTag("scan-state"))
            }
            Label((if (paused) "Sample progress stays at 72%" ar "التقدم التجريبي ثابت عند ٧٢٪"
                else "Demo animation · no live scan" ar "حركة تجريبية · لا يوجد فحص فعلي").localized(), 10, Mist)
            GradientBar(.72f, thickness = 9, live = !paused)
        }
    }
}

@Composable
private fun StorageCapacity() {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        IconBadge(Mark.DISK, Pink, diameter = 38)
        Separator(Modifier.width(1.dp).height(33.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label(("128 GB / 256 GB used" ar "128 GB مستخدم من 256 GB").localized(), 17, weight = FontWeight.Bold)
            GradientBar(.5f, thickness = 8, live = true)
        }
        Label("50%", 12, Mist, FontWeight.Bold)
    }
}

@Composable
private fun FeatureRow(page: Page, entry: Entry, index: Int, selectable: Boolean, selected: Boolean, action: () -> Unit) {
    val roomy = LocalRoomy.current
    val color = if (entry.icon == Mark.CHAT) Mint else if (index % 2 == 0 || page == Page.OPTIMIZE) Pink else Cyan
    val actionModifier = if (selectable) Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { action() })
        else Modifier.clickable(role = Role.Button, onClick = action)
    val arrow = Modifier.size(13.dp).then(if (LocalArabic.current) Modifier.rotate(180f) else Modifier)
    GlassPanel(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("row-$index").clip(RoundedCornerShape(12.dp)).then(actionModifier), 12) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconBadge(entry.icon, color, diameter = 36)
            Column(Modifier.weight(1f)) {
                Label(entry.title.localized(), 12, weight = FontWeight.Bold)
                Label(entry.detail.localized(), 10, Mist)
                if (roomy) {
                    Label(entry.value, 12, Pink, FontWeight.Bold)
                    entry.note?.let { Label(it.localized(), 10, Mist) }
                    entry.progress?.let { GradientBar(it / 100f, thickness = 6, live = page == Page.SCAN) }
                }
            }
            if (!roomy) {
                if (page == Page.REPORTS && index < 4) Sparkline(index, Modifier.width(57.dp).height(27.dp))
                if (entry.progress != null) GradientBar(entry.progress / 100f, Modifier.width(70.dp), 6, live = page == Page.SCAN)
                else Separator(Modifier.width(1.dp).height(27.dp))
                Column(Modifier.widthIn(min = 36.dp, max = if (entry.action != null) 61.dp else 77.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (page == Page.DUPLICATES) entry.note?.let { Label(it.localized(), 11, Chalk, FontWeight.Bold) }
                    Label(entry.value, if (entry.action == null) 12 else 11, if (entry.progress == null) Pink else Chalk, FontWeight.Bold)
                    if (page != Page.DUPLICATES) entry.note?.let { Label(it.localized(), 9, Mist) }
                }
            }
            when {
                selectable -> SelectionIndicator(selected)
                entry.action != null && !roomy -> NeonPill(entry.action.localized(), solid = page != Page.BATTERY)
                else -> Glyph(Mark.NEXT, arrow)
            }
        }
    }
}

@Composable
internal fun Sparkline(index: Int, modifier: Modifier) {
    Canvas(modifier) {
        val color = if (index < 2) Violet else Cyan
        val heights = listOf(.10f,.22f,.35f,.50f,.65f,.84f)
        if (index == 0 || index == 2) {
            val w = size.width / 8.5f
            heights.forEachIndexed { i, v ->
                val h = size.height * v
                drawRoundRect(Brush.verticalGradient(listOf(color, color.copy(alpha = .47f)), size.height - h, size.height),
                    Offset(i * (w + 2.dp.toPx()), size.height - h), Size(w, h), CornerRadius(1.dp.toPx()))
            }
        } else {
            val series = if (index == 1) listOf(.72f, .65f, .44f, .60f, .33f, .25f, .11f)
                else listOf(.82f, .52f, .61f, .36f, .42f, .28f, .10f)
            val dots = series.mapIndexed { i, v -> Offset(size.width * i / (series.size - 1), size.height * v) }
            val shape = Path().apply {
                moveTo(0f, size.height)
                dots.forEach { lineTo(it.x, it.y) }
                lineTo(size.width, size.height); close()
            }
            drawPath(shape, Brush.verticalGradient(listOf(color.copy(alpha = .5f), Color.Transparent)))
            dots.zipWithNext().forEach { (a, b) -> drawLine(color, a, b, 1.2.dp.toPx(), StrokeCap.Round) }
            drawCircle(color, 2.dp.toPx(), dots.last())
        }
    }
}
