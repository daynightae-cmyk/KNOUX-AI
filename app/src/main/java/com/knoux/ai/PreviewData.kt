package com.knoux.ai

// `Copy`, the resource resolver and the number formatter live in KnouxStrings.kt.

internal enum class Mark {
    K, MENU, BACK, NEXT, GEAR, CROWN, HOME, BROOM, GRID, BARS, SHIELD, PICTURES, RADAR,
    LAYERS, DISK, BATTERY, CHIP, SEARCH, FILE, CAMERA, MIC, CLIP, LOCATION, OVERLAY, BELL,
    PERSON, VIDEO, MUSIC, SHARE, CHECK, WARN, CALENDAR, CLOCK, EYE, PLAY, PAUSE, TRASH,
    FOLDER, LINK, CHAT, GAME, BOX, DOWNLOAD, HEART, BOLT, SUN, SYNC, POWER, TEMP, ROCKET,
    GROUP, SPEED, TROPHY, MEMORY, KEY, GOOGLE, SPARKLE, LOCK, HISTORY, COPY, CHART, RIGHT, MAIL
}

internal enum class Page(val id: String, val title: Copy, val hero: Mark) {
    HOME("home", Copy(R.string.k_smart_device_hub), Mark.K),
    PRIVACY("privacy", Copy(R.string.k_privacy_sweep), Mark.SHIELD),
    DUPLICATES("duplicates", Copy(R.string.k_duplicate_files_finder), Mark.PICTURES),
    SCAN("deep-scan", Copy(R.string.k_deep_scan), Mark.RADAR),
    REPORTS("reports", Copy(R.string.k_reports_insights), Mark.BARS),
    RESIDUE("residue", Copy(R.string.k_app_residue_cleanup), Mark.LAYERS),
    APPS("app-manager", Copy(R.string.k_app_manager), Mark.GRID),
    STORAGE("storage", Copy(R.string.k_storage_analyzer), Mark.DISK),
    BATTERY("battery", Copy(R.string.k_battery_saver), Mark.BATTERY),
    OPTIMIZE("ai-optimization", Copy(R.string.k_ai_optimization), Mark.CHIP),
    CLEAN("clean", Copy(R.string.k_smart_clean), Mark.BROOM),
    TOOLS("tools", Copy(R.string.k_device_tools), Mark.GEAR),
    KNOUX_UNIVERSE("knoux-universe", Copy(R.string.k_knoux_universe), Mark.SPARKLE),
    CLEAN_CENTER("clean-center", Copy(R.string.k_smart_clean_center), Mark.TRASH),
    DUPLICATE_REVIEW("duplicate-group-review", Copy(R.string.k_duplicate_group_review), Mark.PICTURES),
    PRIVACY_INSPECTOR("privacy-risk-inspector", Copy(R.string.k_privacy_risk_inspector), Mark.SHIELD),
    SCAN_SUMMARY("deep-scan-summary", Copy(R.string.k_deep_scan_summary), Mark.RADAR),
    REPORT_DETAIL("performance-report-detail", Copy(R.string.k_performance_report_detail), Mark.BARS),
    RESIDUE_REVIEW("residue-item-review", Copy(R.string.k_residue_item_review), Mark.LAYERS),
    APPS_REVIEW("installed-apps-review", Copy(R.string.k_installed_apps_review), Mark.GROUP),
    STORAGE_BREAKDOWN("storage-breakdown", Copy(R.string.k_storage_breakdown_2), Mark.DISK),
    BATTERY_DIAGNOSTICS("battery-diagnostics", Copy(R.string.k_battery_diagnostics), Mark.BATTERY),
    AI_RECOMMENDATIONS("ai-recommendations", Copy(R.string.k_ai_recommendations), Mark.CHIP);

    companion object {
        val references = entries.take(10)
        val detailRoutes = entries.dropWhile { it != CLEAN_CENTER }
        fun from(id: String?) = entries.find { it.id == id }
    }
}

internal val Page.reviewPage: Page?
    get() = when (this) {
        Page.CLEAN -> Page.CLEAN_CENTER
        Page.DUPLICATES -> Page.DUPLICATE_REVIEW
        Page.PRIVACY -> Page.PRIVACY_INSPECTOR
        Page.SCAN -> Page.SCAN_SUMMARY
        Page.REPORTS -> Page.REPORT_DETAIL
        Page.RESIDUE -> Page.RESIDUE_REVIEW
        Page.APPS -> Page.APPS_REVIEW
        Page.STORAGE -> Page.STORAGE_BREAKDOWN
        Page.BATTERY -> Page.BATTERY_DIAGNOSTICS
        Page.OPTIMIZE -> Page.AI_RECOMMENDATIONS
        else -> null
    }

internal val Page.reviewTab: Page?
    get() = when (this) {
        Page.CLEAN_CENTER, Page.DUPLICATE_REVIEW, Page.SCAN_SUMMARY, Page.RESIDUE_REVIEW -> Page.CLEAN
        // Privacy Sweep and AI Optimization are reached from Device Tools, so the
        // Tools tab stays lit - this is what the approved references show.
        Page.PRIVACY, Page.OPTIMIZE, Page.STORAGE, Page.APPS, Page.BATTERY,
        Page.PRIVACY_INSPECTOR, Page.APPS_REVIEW, Page.STORAGE_BREAKDOWN,
        Page.BATTERY_DIAGNOSTICS, Page.AI_RECOMMENDATIONS -> Page.TOOLS
        Page.REPORT_DETAIL -> Page.REPORTS
        else -> null
    }

internal data class Metric(val value: String, val label: Copy, val icon: Mark)
internal data class Fact(val label: Copy, val value: Copy, val icon: Mark)
internal data class Entry(
    val title: Copy, val detail: Copy, val value: String, val icon: Mark,
    val action: Copy? = null, val note: Copy? = null, val mb: Int = 0, val progress: Int? = null
)
internal data class Action(val label: Copy, val icon: Mark)
internal enum class Selection { NONE, TRACKERS, RESIDUE, OPTIMIZATION }
internal data class PreviewScreen(
    val subtitle: Copy, val primary: List<Metric>, val compact: List<Metric>,
    val score: Int, val scoreLabel: Copy, val facts: List<Fact>, val section: Copy?,
    val entries: List<Entry>, val review: Action, val proceed: Action,
    val selection: Selection = Selection.NONE, val grouped: Boolean = false
)
internal enum class PreviewSource { DESIGN_PREVIEW, LIVE_UNAVAILABLE }

