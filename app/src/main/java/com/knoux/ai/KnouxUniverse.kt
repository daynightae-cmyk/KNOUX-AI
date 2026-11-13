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
        "knoux-ai", Copy(R.string.k_knoux_ai),
        Copy(R.string.k_ai_powered_device_intelligence_clean_protect_optimize_an),
        ProjectCategory.AI_ENGINEERING, Copy(R.string.k_android),
        ProjectStatus.ACTIVE, Mark.CHIP, Cyan, "KNOUX-AI"
    ),
    KnouxProject(
        "kforge", Copy(R.string.k_kforge),
        Copy(R.string.k_engineering_and_development_command_center_by_knoux),
        ProjectCategory.AI_ENGINEERING, Copy(R.string.k_multi_platform),
        ProjectStatus.IN_DEVELOPMENT, Mark.BOLT, Violet, "KForge"
    ),
    KnouxProject(
        "knouxcrypt", Copy(R.string.k_knouxcrypt),
        Copy(R.string.k_security_and_encryption_software),
        ProjectCategory.SECURITY, Copy(R.string.k_desktop_mobile),
        ProjectStatus.IN_DEVELOPMENT, Mark.LOCK, Pink, "KnouxCrypt"
    ),
    KnouxProject(
        "kx-private-vpn", Copy(R.string.k_kx_private_vpn),
        Copy(R.string.k_android_privacy_application_vpn_connectivity_requires_se),
        ProjectCategory.SECURITY, Copy(R.string.k_android),
        ProjectStatus.PREVIEW, Mark.SHIELD, Cyan, "KX-Private-VPN"
    ),
    KnouxProject(
        "knoux-gemini", Copy(R.string.k_knoux_gemini_smartorganizer),
        Copy(R.string.k_desktop_intelligent_file_organization_powered_by_gemini),
        ProjectCategory.UTILITIES, Copy(R.string.k_desktop),
        ProjectStatus.IN_DEVELOPMENT, Mark.FOLDER, Gold, "KNOuX-Gemini-SmartOrganizer"
    ),
    KnouxProject(
        "knouxrec", Copy(R.string.k_knouxrec),
        Copy(R.string.k_recording_and_capture_utility_by_knoux),
        ProjectCategory.UTILITIES, Copy(R.string.k_desktop),
        ProjectStatus.IN_DEVELOPMENT, Mark.MIC, Violet, "Knouxrec"
    ),
    KnouxProject(
        "knoux-repair", Copy(R.string.k_knoux_repair),
        Copy(R.string.k_system_maintenance_and_repair_software),
        ProjectCategory.UTILITIES, Copy(R.string.k_desktop),
        ProjectStatus.IN_DEVELOPMENT, Mark.SYNC, Pink, "knoux-Repair"
    ),
    KnouxProject(
        "knoux-webpilot", Copy(R.string.k_knoux_webpilot),
        Copy(R.string.k_capture_intelligence_convert_webpage_content_into_reusab),
        ProjectCategory.BROWSER_WEB, Copy(R.string.k_microsoft_edge),
        ProjectStatus.ACTIVE, Mark.SEARCH, Cyan, "KNOUX-WebPilot",
        storeUrl = "https://microsoftedge.microsoft.com/addons/search/KNOUX%20WebPilot"
    ),
    KnouxProject(
        "knoux-grabflow", Copy(R.string.k_knoux_grabflow),
        Copy(R.string.k_media_video_finder_identify_accessible_webpage_media_and),
        ProjectCategory.BROWSER_WEB, Copy(R.string.k_microsoft_edge),
        ProjectStatus.ACTIVE, Mark.DOWNLOAD, Gold, "KNOuX-GrabFlow",
        storeUrl = "https://microsoftedge.microsoft.com/addons/search/KNOuX%20GrabFlow"
    ),
    KnouxProject(
        "knoux-x", Copy(R.string.k_knoux_x),
        Copy(R.string.k_knoux_platform_extension_and_tool),
        ProjectCategory.BROWSER_WEB, Copy(R.string.k_web),
        ProjectStatus.IN_DEVELOPMENT, Mark.BOLT, Violet, "knoux-x"
    ),
    KnouxProject(
        "knoux-store", Copy(R.string.k_knoux_store),
        Copy(R.string.k_official_knoux_digital_storefront_and_web_presence),
        ProjectCategory.CORPORATE, Copy(R.string.k_web),
        ProjectStatus.ACTIVE, Mark.CROWN, Gold, "knoux-store",
        websiteUrl = "https://knoux.store/"
    ),
    KnouxProject(
        "basma-moussa", Copy(R.string.k_basma_moussa_media_universe),
        Copy(R.string.k_digital_media_universe_independent_brand_project),
        ProjectCategory.SPECIALIZED, Copy(R.string.k_web),
        ProjectStatus.CLIENT_WORK, Mark.VIDEO, Pink, "Basma-Moussa-Media-Universe"
    ),
    KnouxProject(
        "swim-fit", Copy(R.string.k_swim_fit_academy),
        Copy(R.string.k_swimming_academy_digital_platform),
        ProjectCategory.SPECIALIZED, Copy(R.string.k_web),
        ProjectStatus.CLIENT_WORK, Mark.HEART, Cyan, "Swim-Fit-Academy"
    ),
    KnouxProject(
        "united-olympics", Copy(R.string.k_united_olympics_sports),
        Copy(R.string.k_bilingual_sports_development_platform),
        ProjectCategory.SPECIALIZED, Copy(R.string.k_web),
        ProjectStatus.CLIENT_WORK, Mark.TROPHY, Gold, "United-Olympics-Sports"
    ),
    KnouxProject(
        "daynightdelivery", Copy(R.string.k_daynightdeliveryservices1),
        Copy(R.string.k_delivery_services_digital_project),
        ProjectCategory.SPECIALIZED, Copy(R.string.k_web),
        ProjectStatus.CLIENT_WORK, Mark.BOLT, Violet, "DayNightDeliveryServices1"
    ),
    KnouxProject(
        "yarasoolallah", Copy(R.string.k_yarasoolallah),
        Copy(R.string.k_a_dedicated_digital_project_scope_and_audience_verified),
        ProjectCategory.SPECIALIZED, Copy(R.string.k_web),
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
                    (Copy(R.string.k_building_the_digital_future)).localized(),
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
                        Label((Copy(R.string.k_eng_sadek_elgazar)).localized(), 16, Chalk, FontWeight.ExtraBold)
                        Label((Copy(R.string.k_founder_software_engineer)).localized(), 12, Mist)
                        Label((Copy(R.string.k_founder_of_knoux)).localized(), 11, Mist.copy(alpha = .70f))
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
                (Copy(R.string.k_software_portfolio)).localized(),
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
            (Copy(R.string.k_browser_extensions)).localized(),
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
                    Action((Copy(R.string.k_view_on_store)), Mark.DOWNLOAD),
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
                    Action((Copy(R.string.k_view_on_edge_add_ons)), Mark.DOWNLOAD),
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
        ProjectStatus.ACTIVE -> (Copy(R.string.k_active)) to Cyan
        ProjectStatus.IN_DEVELOPMENT -> (Copy(R.string.k_in_development)) to Violet
        ProjectStatus.PREVIEW -> (Copy(R.string.k_preview)) to Gold
        ProjectStatus.CLIENT_WORK -> (Copy(R.string.k_client_work)) to Pink
    }
    NeonPill(label.localized(), tint)
}

private fun categoryLabel(category: ProjectCategory): Copy = when (category) {
    ProjectCategory.AI_ENGINEERING -> Copy(R.string.k_ai_engineering)
    ProjectCategory.SECURITY -> Copy(R.string.k_security_privacy)
    ProjectCategory.UTILITIES -> Copy(R.string.k_utilities_productivity)
    ProjectCategory.BROWSER_WEB -> Copy(R.string.k_browser_web)
    ProjectCategory.CORPORATE -> Copy(R.string.k_corporate)
    ProjectCategory.SPECIALIZED -> Copy(R.string.k_specialized_projects)
}

private fun categoryColor(category: ProjectCategory): Color = when (category) {
    ProjectCategory.AI_ENGINEERING -> Cyan
    ProjectCategory.SECURITY -> Pink
    ProjectCategory.UTILITIES -> Gold
    ProjectCategory.BROWSER_WEB -> Violet
    ProjectCategory.CORPORATE -> Gold
    ProjectCategory.SPECIALIZED -> Mint
}
