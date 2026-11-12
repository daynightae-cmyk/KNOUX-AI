package com.knoux.ai

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

internal enum class ProjectCategory {
    AI_ENGINEERING, SECURITY, UTILITIES, BROWSER_WEB, CORPORATE, SPECIALIZED
}

internal enum class ProjectStatus {
    ACTIVE, IN_DEVELOPMENT, PREVIEW, CLIENT_WORK
}

internal data class KnouxProject(
    val id: String,
    val name: Copy,
    val description: Copy,
    val category: ProjectCategory,
    val platform: Copy,
    val status: ProjectStatus,
    val icon: Mark,
    val tint: Color,
    val githubSlug: String,
    val websiteUrl: String? = null,
    val storeUrl: String? = null
)

internal val knouxProjects = listOf(
    KnouxProject(
        "knoux-ai", "KNOUX AI" ar "KNOUX AI",
        "AI-powered device intelligence: clean, protect, optimize, analyze." ar "ذكاء اصطناعي لجهازك: تنظيف وحماية وتحسين وتحليل.",
        ProjectCategory.AI_ENGINEERING, "Android" ar "Android",
        ProjectStatus.ACTIVE, Mark.CHIP, Cyan, "KNOUX-AI"
    ),
    KnouxProject(
        "kforge", "KForge" ar "KForge",
        "Engineering and development command center by KNOUX." ar "مركز قيادة هندسي وتطويري من KNOUX.",
        ProjectCategory.AI_ENGINEERING, "Multi-platform" ar "متعدد المنصات",
        ProjectStatus.IN_DEVELOPMENT, Mark.BOLT, Violet, "KForge"
    ),
    KnouxProject(
        "knouxcrypt", "KnouxCrypt" ar "KnouxCrypt",
        "Security and encryption software." ar "برنامج أمان وتشفير.",
        ProjectCategory.SECURITY, "Desktop/Mobile" ar "سطح المكتب/الجوال",
        ProjectStatus.IN_DEVELOPMENT, Mark.LOCK, Pink, "KnouxCrypt"
    ),
    KnouxProject(
        "kx-private-vpn", "KX-Private-VPN" ar "KX-Private-VPN",
        "Android privacy application. VPN connectivity requires server configuration." ar "تطبيق خصوصية لأندرويد. تقنية VPN تتطلب ضبط الخادم.",
        ProjectCategory.SECURITY, "Android" ar "Android",
        ProjectStatus.PREVIEW, Mark.SHIELD, Cyan, "KX-Private-VPN"
    ),
    KnouxProject(
        "knoux-gemini", "KNOuX-Gemini-SmartOrganizer" ar "KNOuX-Gemini-SmartOrganizer",
        "Desktop intelligent file organization powered by Gemini AI." ar "تنظيم ذكي للملفات على سطح المكتب بقوة Gemini AI.",
        ProjectCategory.UTILITIES, "Desktop" ar "سطح المكتب",
        ProjectStatus.IN_DEVELOPMENT, Mark.FOLDER, Gold, "KNOuX-Gemini-SmartOrganizer"
    ),
    KnouxProject(
        "knouxrec", "Knouxrec" ar "Knouxrec",
        "Recording and capture utility by KNOUX." ar "أداة تسجيل والتقاط من KNOUX.",
        ProjectCategory.UTILITIES, "Desktop" ar "سطح المكتب",
        ProjectStatus.IN_DEVELOPMENT, Mark.MIC, Violet, "Knouxrec"
    ),
    KnouxProject(
        "knoux-repair", "knoux-Repair" ar "knoux-Repair",
        "System maintenance and repair software." ar "برنامج صيانة وإصلاح النظام.",
        ProjectCategory.UTILITIES, "Desktop" ar "سطح المكتب",
        ProjectStatus.IN_DEVELOPMENT, Mark.SYNC, Pink, "knoux-Repair"
    ),
    KnouxProject(
        "knoux-webpilot", "KNOUX WebPilot" ar "KNOUX WebPilot",
        "Capture Intelligence — convert webpage content into reusable structured formats." ar "التقاط المحتوى وتحويله إلى تنسيقات منظمة.",
        ProjectCategory.BROWSER_WEB, "Microsoft Edge" ar "Microsoft Edge",
        ProjectStatus.ACTIVE, Mark.SEARCH, Cyan, "KNOUX-WebPilot",
        storeUrl = "https://microsoftedge.microsoft.com/addons/search/KNOUX%20WebPilot"
    ),
    KnouxProject(
        "knoux-grabflow", "KNOuX GrabFlow" ar "KNOuX GrabFlow",
        "Media & Video Finder — identify accessible webpage media and file assets." ar "الكشف عن الوسائط والملفات في صفحات الويب.",
        ProjectCategory.BROWSER_WEB, "Microsoft Edge" ar "Microsoft Edge",
        ProjectStatus.ACTIVE, Mark.DOWNLOAD, Gold, "KNOuX-GrabFlow",
        storeUrl = "https://microsoftedge.microsoft.com/addons/search/KNOuX%20GrabFlow"
    ),
    KnouxProject(
        "knoux-x", "knoux-x" ar "knoux-x",
        "KNOUX platform extension and tool." ar "أداة وامتداد منصة KNOUX.",
        ProjectCategory.BROWSER_WEB, "Web" ar "ويب",
        ProjectStatus.IN_DEVELOPMENT, Mark.BOLT, Violet, "knoux-x"
    ),
    KnouxProject(
        "knoux-store", "knoux-store" ar "knoux-store",
        "Official KNOUX digital storefront and web presence." ar "المتجر الرقمي الرسمي لـ KNOUX.",
        ProjectCategory.CORPORATE, "Web" ar "ويب",
        ProjectStatus.ACTIVE, Mark.CROWN, Gold, "knoux-store",
        websiteUrl = "https://knoux.store/"
    ),
    KnouxProject(
        "basma-moussa", "Basma-Moussa-Media-Universe" ar "Basma-Moussa-Media-Universe",
        "Digital media universe — independent brand project." ar "كون وسائط رقمية — مشروع علامة مستقلة.",
        ProjectCategory.SPECIALIZED, "Web" ar "ويب",
        ProjectStatus.CLIENT_WORK, Mark.VIDEO, Pink, "Basma-Moussa-Media-Universe"
    ),
    KnouxProject(
        "swim-fit", "Swim-Fit-Academy" ar "Swim-Fit-Academy",
        "Swimming academy digital platform." ar "منصة أكاديمية سباحة رقمية.",
        ProjectCategory.SPECIALIZED, "Web" ar "ويب",
        ProjectStatus.CLIENT_WORK, Mark.HEART, Cyan, "Swim-Fit-Academy"
    ),
    KnouxProject(
        "united-olympics", "United-Olympics-Sports" ar "United-Olympics-Sports",
        "Bilingual sports development platform." ar "منصة تطوير رياضي ثنائية اللغة.",
        ProjectCategory.SPECIALIZED, "Web" ar "ويب",
        ProjectStatus.CLIENT_WORK, Mark.TROPHY, Gold, "United-Olympics-Sports"
    ),
    KnouxProject(
        "daynightdelivery", "DayNightDeliveryServices1" ar "DayNightDeliveryServices1",
        "Delivery services digital project." ar "مشروع خدمات توصيل رقمي.",
        ProjectCategory.SPECIALIZED, "Web" ar "ويب",
        ProjectStatus.CLIENT_WORK, Mark.BOLT, Violet, "DayNightDeliveryServices1"
    ),
    KnouxProject(
        "yarasoolallah", "YaRasoolAllah" ar "YaRasoolAllah",
        "A dedicated digital project — scope and audience verified from repository." ar "مشروع رقمي متخصص — الهدف موثق من المستودع.",
        ProjectCategory.SPECIALIZED, "Web" ar "ويب",
        ProjectStatus.IN_DEVELOPMENT, Mark.SPARKLE, Gold, "YaRasoolAllah"
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun KnouxUniverseScreen() {
    val context = LocalContext.current

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // ── Section 1: Corporate Hero ─────────────────────────────────────
        GlassPanel(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
            radius = 24, tint = Violet
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xff1a0040).copy(alpha = .60f), RoundedCornerShape(50.dp))
                        .border(1.dp, Violet.copy(alpha = .55f), RoundedCornerShape(50.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.knoux_logo),
                        contentDescription = "KNOUX",
                        modifier = Modifier.size(76.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    "KNOUX",
                    style = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = DisplayFace,
                        brush = Brush.horizontalGradient(listOf(Gold, Chalk, Gold))
                    )
                )

                Label(
                    ("Building the Digital Future" ar "نبني المستقبل الرقمي").localized(),
                    14, Mist,
                    modifier = Modifier.fillMaxWidth(),
                    align = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Section 2: Founder Identity ───────────────────────────────────
        GlassPanel(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            radius = 20, tint = Gold
        ) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(Mark.PERSON, Gold, diameter = 48)
                    Column(Modifier.weight(1f)) {
                        Label(("Eng. Sadek Elgazar" ar "م. صادق الجزار").localized(), 16, Chalk, FontWeight.ExtraBold)
                        Label(("Founder & Software Engineer" ar "المؤسس ومهندس البرمجيات").localized(), 12, Mist)
                        Label(("Founder of KNOUX" ar "مؤسس KNOUX").localized(), 11, Mist.copy(alpha = .70f))
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Gold.copy(alpha = .10f), RoundedCornerShape(12.dp))
                        .border(1.dp, Gold.copy(alpha = .55f), RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { openUrl("https://knoux.store/") }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(Mark.LINK, Modifier.size(14.dp), Gold)
                        Spacer(Modifier.width(6.dp))
                        Label("knoux.store", 13, Gold, FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Section 3: Software Portfolio ────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Label(
                ("Software Portfolio" ar "محفظة البرامج").localized(),
                18, Chalk, FontWeight.ExtraBold
            )
            NeonPill("${knouxProjects.size}", Cyan)
        }

        Spacer(Modifier.height(8.dp))

        ProjectCategory.entries.forEach { category ->
            val projects = knouxProjects.filter { it.category == category }
            if (projects.isEmpty()) return@forEach

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier.height(2.dp).width(20.dp)
                        .background(categoryColor(category))
                )
                Label(categoryLabel(category).localized(), 12, categoryColor(category), FontWeight.Bold)
                Box(
                    Modifier.height(1.dp).weight(1f)
                        .background(categoryColor(category).copy(alpha = .25f))
                )
            }

            projects.forEach { project ->
                Spacer(Modifier.height(6.dp))
                ProjectCard(project) { url -> openUrl(url) }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Section 4: Browser Extensions Featured Cards ──────────────────
        Label(
            ("Browser Extensions" ar "إضافات المتصفح").localized(),
            18, Chalk, FontWeight.ExtraBold
        )

        Spacer(Modifier.height(8.dp))

        knouxProjects.filter { it.id == "knoux-webpilot" || it.id == "knoux-grabflow" }
            .forEach { project ->
                Spacer(Modifier.height(8.dp))
                ExtensionCard(project) { url -> openUrl(url) }
            }

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Project card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProjectCard(project: KnouxProject, openUrl: (String) -> Unit) {
    GlassPanel(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        radius = 16, tint = project.tint
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(project.icon, project.tint, diameter = 40)
                Column(Modifier.weight(1f)) {
                    Label(project.name.localized(), 14, Chalk, FontWeight.ExtraBold)
                    Label(project.description.localized(), 11, Mist, maxLines = 2)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonPill(project.platform.localized(), project.tint, solid = false)
                StatusPill(project.status)
            }
            project.websiteUrl?.let { url ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(project.tint.copy(alpha = .08f))
                        .border(1.dp, project.tint.copy(alpha = .50f), RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { openUrl(url) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(Mark.LINK, Modifier.size(12.dp), project.tint)
                        Label(url.removePrefix("https://").trimEnd('/'), 11, project.tint, FontWeight.Medium)
                    }
                }
            }
            project.storeUrl?.let { url ->
                AccentButton(
                    Action(("View on Store" ar "عرض في المتجر"), Mark.DOWNLOAD),
                    Modifier.fillMaxWidth(), primary = false
                ) { openUrl(url) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension featured card (larger, premium)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExtensionCard(project: KnouxProject, openUrl: (String) -> Unit) {
    GlassPanel(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        radius = 20, tint = project.tint
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(project.icon, project.tint, diameter = 52)
                Column(Modifier.weight(1f)) {
                    Label(project.name.localized(), 16, Chalk, FontWeight.ExtraBold)
                    Label(project.description.localized(), 11, Mist, maxLines = 3)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonPill("Microsoft Edge", Cyan)
                StatusPill(project.status)
            }
            project.storeUrl?.let { url ->
                AccentButton(
                    Action(("View on Edge Add-ons" ar "عرض في إضافات Edge"), Mark.DOWNLOAD),
                    Modifier.fillMaxWidth(), primary = true
                ) { openUrl(url) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusPill(status: ProjectStatus) {
    val (label, tint) = when (status) {
        ProjectStatus.ACTIVE -> ("Active" ar "نشط") to Cyan
        ProjectStatus.IN_DEVELOPMENT -> ("In Development" ar "قيد التطوير") to Violet
        ProjectStatus.PREVIEW -> ("Preview" ar "معاينة") to Gold
        ProjectStatus.CLIENT_WORK -> ("Client Work" ar "مشروع عميل") to Pink
    }
    NeonPill(label.localized(), tint)
}

private fun categoryLabel(category: ProjectCategory): Copy = when (category) {
    ProjectCategory.AI_ENGINEERING -> "AI & Engineering" ar "الذكاء الاصطناعي والهندسة"
    ProjectCategory.SECURITY -> "Security & Privacy" ar "الأمان والخصوصية"
    ProjectCategory.UTILITIES -> "Utilities & Productivity" ar "الأدوات والإنتاجية"
    ProjectCategory.BROWSER_WEB -> "Browser & Web" ar "المتصفح والويب"
    ProjectCategory.CORPORATE -> "Corporate" ar "الشركة"
    ProjectCategory.SPECIALIZED -> "Specialized Projects" ar "المشاريع المتخصصة"
}

private fun categoryColor(category: ProjectCategory): Color = when (category) {
    ProjectCategory.AI_ENGINEERING -> Cyan
    ProjectCategory.SECURITY -> Pink
    ProjectCategory.UTILITIES -> Gold
    ProjectCategory.BROWSER_WEB -> Violet
    ProjectCategory.CORPORATE -> Gold
    ProjectCategory.SPECIALIZED -> Mint
}
