package com.knoux.ai

// `Copy`, the resource resolver and the number formatter live in KnouxStrings.kt.

internal enum class Mark {
    K, MENU, BACK, NEXT, GEAR, CROWN, HOME, BROOM, GRID, BARS, SHIELD, PICTURES, RADAR,
    LAYERS, DISK, BATTERY, CHIP, SEARCH, FILE, CAMERA, MIC, CLIP, LOCATION, OVERLAY, BELL,
    PERSON, VIDEO, MUSIC, SHARE, CHECK, WARN, CALENDAR, CLOCK, EYE, PLAY, PAUSE, TRASH,
    FOLDER, LINK, CHAT, GAME, BOX, DOWNLOAD, HEART, BOLT, SUN, SYNC, POWER, TEMP, ROCKET,
    GROUP, SPEED, TROPHY, MEMORY, KEY, GOOGLE, SPARKLE, LOCK, HISTORY, COPY, CHART, RIGHT
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
    TOOLS("tools", Copy(R.string.k_device_tools), Mark.GRID),
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

private fun m(value: String, label: Copy, icon: Mark) = Metric(value, label, icon)
private fun f(label: Copy, value: String, icon: Mark) = Fact(label, Copy.runtime(value), icon)
private fun time(label: Copy) = Fact(label, Copy(R.string.k_today_10_21_am), Mark.CALENDAR)
private fun a(label: Copy, icon: Mark = Mark.EYE) = Action(label, icon)
private val review = Copy(R.string.k_review)

internal object PreviewFixtures {
    fun screen(page: Page, source: PreviewSource): PreviewScreen? =
        if (source == PreviewSource.DESIGN_PREVIEW) screens[page] else null

    fun selectedMb(entries: List<Entry>, selected: Set<Int>): Int =
        entries.withIndex().sumOf { (index, item) -> if (index in selected) item.mb else 0 }

    private val screens = mapOf(
        Page.PRIVACY to PreviewScreen(
            Copy(R.string.k_review_trackers_risky_permissions_and_privacy_exposure_i),
            listOf(m("7", Copy(R.string.k_trackers_found_2), Mark.PERSON), m("86 /100", Copy(R.string.k_privacy_score), Mark.SHIELD)),
            listOf(m("2", Copy(R.string.k_camera_access_2), Mark.CAMERA), m("1", Copy(R.string.k_microphone_access_2), Mark.MIC),
                m("7", Copy(R.string.k_background_trackers_2), Mark.LAYERS), m("2", Copy(R.string.k_clipboard_watchers_2), Mark.CLIP)),
            86, Copy(R.string.k_privacy_score),
            listOf(f(Copy(R.string.k_protected_apps), "24", Mark.CHECK), f(Copy(R.string.k_needs_review_2), "5", Mark.WARN), time(Copy(R.string.k_last_sweep))),
            Copy(R.string.k_privacy_items_5),
            listOf(
                Entry(Copy(R.string.k_ad_sdk_trackers), Copy(R.string.k_third_party_ad_sdks_that_track_your_activity_across_apps), "7 found", Mark.BELL, review),
                Entry(Copy(R.string.k_background_location), Copy(R.string.k_apps_that_can_access_your_location_in_the_background), "1 app", Mark.LOCATION, review),
                Entry(Copy(R.string.k_clipboard_access), Copy(R.string.k_apps_that_can_read_your_clipboard_content), "2 apps", Mark.CLIP, review),
                Entry(Copy(R.string.k_overlay_permission), Copy(R.string.k_apps_that_can_appear_over_other_apps), "1 app", Mark.OVERLAY, review),
                Entry(Copy(R.string.k_notification_listeners), Copy(R.string.k_apps_that_can_read_and_process_notifications), "2 apps", Mark.BELL, review)),
            a(Copy(R.string.k_review_risks)), a(Copy(R.string.k_improve_privacy), Mark.SHIELD), Selection.TRACKERS
        ),
        Page.DUPLICATES to PreviewScreen(
            Copy(R.string.k_detect_and_review_duplicate_photos_videos_audio_and_docu),
            listOf(m("1,248", Copy(R.string.k_duplicates_found), Mark.FILE), m("8.7 GB", Copy(R.string.k_recoverable), Mark.DISK)),
            listOf(m("612", Copy(R.string.k_similar_photos), Mark.PICTURES), m("238", Copy(R.string.k_repeated_videos), Mark.VIDEO),
                m("316", Copy(R.string.k_copy_documents), Mark.FILE), m("82", Copy(R.string.k_duplicate_audio), Mark.MUSIC)),
            96, Copy(R.string.k_match_confidence),
            listOf(Fact(Copy(R.string.k_auto_grouped_items), Copy(R.string.k_t_218_groups), Mark.CHECK), Fact(Copy(R.string.k_needs_review_2), Copy(R.string.k_t_37_items), Mark.WARN), time(Copy(R.string.k_last_scan))),
            Copy(R.string.k_duplicate_groups_5),
            listOf(
                Entry(Copy(R.string.k_similar_selfies), Copy(R.string.k_duplicates_and_near_identical_selfies), "1.6 GB", Mark.PERSON, note = Copy(R.string.k_t_189_items)),
                Entry(Copy(R.string.k_burst_photos), Copy(R.string.k_multiple_shots_of_the_same_moment), "3.1 GB", Mark.PICTURES, note = Copy(R.string.k_t_423_items)),
                Entry(Copy(R.string.k_duplicate_videos), Copy(R.string.k_repeated_videos_and_saved_copies), "2.4 GB", Mark.VIDEO, note = Copy(R.string.k_t_238_items)),
                Entry(Copy(R.string.k_repeated_documents), Copy(R.string.k_duplicate_pdf_doc_and_xls_files), "1.1 GB", Mark.FILE, note = Copy(R.string.k_t_316_items)),
                Entry(Copy(R.string.k_shared_media_copies), Copy(R.string.k_duplicates_from_chats_and_shared_apps), "521 MB", Mark.SHARE, note = Copy(R.string.k_t_82_items))),
            a(Copy(R.string.k_preview_groups)), a(Copy(R.string.k_review_remove), Mark.TRASH)
        ),
        Page.SCAN to PreviewScreen(
            Copy(R.string.k_preview_a_scan_for_junk_duplicates_privacy_issues_and_sy),
            emptyList(),
            listOf(m("2.4 GB", Copy(R.string.k_temporary_files_2), Mark.DISK), m("1,248", Copy(R.string.k_duplicate_media), Mark.PICTURES),
                m("7", Copy(R.string.k_privacy_alerts), Mark.SHIELD), m("95%", Copy(R.string.k_app_health), Mark.LAYERS)),
            72, Copy(R.string.k_overall_scan),
            listOf(f(Copy(R.string.k_modules_scanned), "5 of 5", Mark.LAYERS), Fact(Copy(R.string.k_findings_so_far), Copy(R.string.k_t_1_656_items), Mark.SEARCH), Fact(Copy(R.string.k_estimated_time), Copy(R.string.k_t_1_min_24_sec), Mark.CLOCK)),
            Copy(R.string.k_scan_modules_5),
            listOf(
                Entry(Copy(R.string.k_scan_storage), Copy(R.string.k_temporary_and_residual_files), "84%", Mark.DISK, progress = 84),
                Entry(Copy(R.string.k_review_media), Copy(R.string.k_duplicate_photos_and_videos), "67%", Mark.PICTURES, progress = 67),
                Entry(Copy(R.string.k_inspect_privacy), Copy(R.string.k_trackers_permissions_and_risks), "73%", Mark.SHIELD, progress = 73),
                Entry(Copy(R.string.k_analyze_apps), Copy(R.string.k_app_health_and_resource_use), "91%", Mark.LAYERS, progress = 91),
                Entry(Copy(R.string.k_battery_diagnosis), Copy(R.string.k_battery_health_and_usage_patterns), "58%", Mark.BATTERY, progress = 58)),
            a(Copy(R.string.k_pause_scan), Mark.PAUSE), a(Copy(R.string.k_continue_scan), Mark.PLAY)
        ),
        Page.REPORTS to PreviewScreen(
            Copy(R.string.k_track_cleanups_storage_gains_optimization_history_and_de),
            listOf(m("12.6 GB", Copy(R.string.k_total_saved_2), Mark.DISK), m("31", Copy(R.string.k_cleanups_completed), Mark.BROOM)),
            listOf(m("4.8 GB", Copy(R.string.k_weekly_savings), Mark.CALENDAR), m("7", Copy(R.string.k_privacy_fixes_2), Mark.SHIELD),
                m("+18%", Copy(R.string.k_battery_gains), Mark.BATTERY), m("1,248", Copy(R.string.k_duplicate_reduction), Mark.LAYERS)),
            92, Copy(R.string.k_device_score_2),
            listOf(Fact(Copy(R.string.k_best_improvement), Copy(R.string.k_storage_cleanup_6_4_gb_saved), Mark.TROPHY),
                f(Copy(R.string.k_current_device_score), "92%  ▲ +6%", Mark.SPEED), time(Copy(R.string.k_last_report))),
            null,
            listOf(
                Entry(Copy(R.string.k_cleanup_history), Copy(R.string.k_past_cleanups_space_saved_and_items_removed), "31", Mark.BROOM, note = Copy(R.string.k_sessions)),
                Entry(Copy(R.string.k_storage_trend), Copy(R.string.k_track_storage_usage_and_savings_over_time), "12.6 GB", Mark.DISK, note = Copy(R.string.k_total_saved_2)),
                Entry(Copy(R.string.k_privacy_improvements), Copy(R.string.k_trackers_cleared_data_and_reduced_risks), "7", Mark.SHIELD, note = Copy(R.string.k_fixes_applied)),
                Entry(Copy(R.string.k_battery_sessions), Copy(R.string.k_optimization_sessions_and_performance_gains), "+18%", Mark.BATTERY, note = Copy(R.string.k_battery_gain_2)),
                Entry(Copy(R.string.k_smart_recommendations), Copy(R.string.k_free_up_2_1_gb_optimize_battery_usage), "3", Mark.CHIP, note = Copy(R.string.k_new))),
            a(Copy(R.string.k_view_history)), a(Copy(R.string.k_generate_report), Mark.FILE)
        ),
        Page.RESIDUE to PreviewScreen(
            Copy(R.string.k_find_leftover_folders_empty_data_and_remnants_from_remov),
            listOf(m("836 MB", Copy(R.string.k_residual_data), Mark.DISK), m("42", Copy(R.string.k_orphan_folders_2), Mark.FOLDER)),
            listOf(m("8", Copy(R.string.k_uninstalled_apps), Mark.TRASH), m("42", Copy(R.string.k_empty_directories), Mark.FOLDER),
                m("516 MB", Copy(R.string.k_old_cache), Mark.LAYERS), m("17", Copy(R.string.k_broken_shortcuts), Mark.LINK)),
            91, Copy(R.string.k_cleanup_safe),
            listOf(f(Copy(R.string.k_safe_to_clean_2), "836 MB", Mark.CHECK), f(Copy(R.string.k_needs_review_2), "28 MB", Mark.WARN), time(Copy(R.string.k_last_scan))),
            Copy(R.string.k_items_found_5),
            listOf(
                Entry(Copy(R.string.k_whatsapp_leftovers), Copy(R.string.k_residual_folders_logs_and_unused_data), "268 MB", Mark.CHAT, mb = 268),
                Entry(Copy(R.string.k_old_game_cache), Copy(R.string.k_obsolete_cache_from_uninstalled_games), "184 MB", Mark.GAME, mb = 184),
                Entry(Copy(R.string.k_empty_media_folders), Copy(R.string.k_media_directories_without_files), "96 MB", Mark.PICTURES, mb = 96),
                Entry(Copy(R.string.k_broken_temp_data), Copy(R.string.k_incomplete_temporary_data_and_sessions), "172 MB", Mark.FILE, mb = 172),
                Entry(Copy(R.string.k_archived_installer_remnants), Copy(R.string.k_leftover_apk_or_obb_files), "116 MB", Mark.BOX, mb = 116)),
            a(Copy(R.string.k_preview_residue)), a(Copy(R.string.k_clean_remnants), Mark.BROOM), Selection.RESIDUE, true
        ),
        Page.APPS to PreviewScreen(
            Copy(R.string.k_review_installed_apps_space_use_startup_impact_and_updat),
            listOf(m("143", Copy(R.string.k_installed_apps), Mark.LAYERS), m("18.4 GB", Copy(R.string.k_app_storage), Mark.DISK)),
            listOf(m("27", Copy(R.string.k_rarely_used), Mark.CALENDAR), m("16", Copy(R.string.k_large_apps_500_mb), Mark.DISK),
                m("9", Copy(R.string.k_startup_apps), Mark.POWER), m("12", Copy(R.string.k_updates_available), Mark.SYNC)),
            87, Copy(R.string.k_app_health),
            listOf(f(Copy(R.string.k_apps_to_review), "27", Mark.WARN), f(Copy(R.string.k_safe_suggestions), "18", Mark.CHECK), time(Copy(R.string.k_last_checked))),
            null,
            listOf(
                Entry(Copy(R.string.k_streaming_apps), Copy(R.string.k_video_music_and_live_streaming_apps), "5.86 GB", Mark.VIDEO, Copy(R.string.k_optimize), Copy(R.string.k_last_used_today)),
                Entry(Copy(R.string.k_social_apps), Copy(R.string.k_social_networking_and_community_apps), "3.12 GB", Mark.GROUP, review, Copy(R.string.k_last_used_today)),
                Entry(Copy(R.string.k_game_collection), Copy(R.string.k_installed_games_and_game_resources), "6.27 GB", Mark.GAME, Copy(R.string.k_manage), Copy(R.string.k_last_used_yesterday)),
                Entry(Copy(R.string.k_utility_tools), Copy(R.string.k_productivity_file_and_system_tools), "2.43 GB", Mark.GRID, Copy(R.string.k_clean_sample_summary), Copy(R.string.k_last_used_today)),
                Entry(Copy(R.string.k_messaging_apps), Copy(R.string.k_chat_calls_and_communication_apps), "744 MB", Mark.CHAT, Copy(R.string.k_open), Copy(R.string.k_last_used_today))),
            a(Copy(R.string.k_review_apps)), a(Copy(R.string.k_manage_apps), Mark.GRID), grouped = true
        ),
        Page.STORAGE to PreviewScreen(
            Copy(R.string.k_understand_storage_usage_large_files_and_space_distribut),
            emptyList(),
            listOf(m("48.6 GB", Copy(R.string.k_apps), Mark.GRID), m("36.8 GB", Copy(R.string.k_media), Mark.PICTURES), m("8.4 GB", Copy(R.string.k_documents), Mark.FILE),
                m("22.2 GB", Copy(R.string.k_system), Mark.GEAR), m("128 GB", Copy(R.string.k_free_space), Mark.LAYERS)),
            50, Copy(R.string.k_used_128_gb),
            listOf(Fact(Copy(R.string.k_largest_category), Copy(R.string.k_apps_games_48_6_gb), Mark.TROPHY),
                f(Copy(R.string.k_unused_space_trend), "+12.6 GB", Mark.BARS), time(Copy(R.string.k_last_updated))),
            Copy(R.string.k_storage_categories),
            listOf(
                Entry(Copy(R.string.k_apps_games), Copy(R.string.k_installed_apps_games_and_app_data), "48.6 GB", Mark.GRID),
                Entry(Copy(R.string.k_photos_videos_2), Copy(R.string.k_photos_videos_and_screen_recordings), "36.8 GB", Mark.PICTURES),
                Entry(Copy(R.string.k_downloads_2), Copy(R.string.k_downloaded_files_and_installers), "6.2 GB", Mark.DOWNLOAD),
                Entry(Copy(R.string.k_documents_2), Copy(R.string.k_pdfs_office_files_and_documents), "8.4 GB", Mark.FILE),
                Entry(Copy(R.string.k_whatsapp_media), Copy(R.string.k_photos_videos_audio_and_shared_files), "4.1 GB", Mark.CHAT),
                Entry(Copy(R.string.k_system_files), Copy(R.string.k_android_system_cache_and_essential_files), "22.2 GB", Mark.GEAR)),
            a(Copy(R.string.k_view_breakdown)), a(Copy(R.string.k_manage_space), Mark.LAYERS), grouped = true
        ),
        Page.BATTERY to PreviewScreen(
            Copy(R.string.k_reduce_drain_control_background_activity_and_extend_batt),
            listOf(m("6h 42m", Copy(R.string.k_estimated_gain_2), Mark.CLOCK), m("91%", Copy(R.string.k_battery_health_2), Mark.HEART)),
            listOf(m("5", Copy(R.string.k_drain_sources), Mark.BOLT), m("18", Copy(R.string.k_background_apps), Mark.LAYERS),
                m("−22%", Copy(R.string.k_brightness_impact), Mark.SUN), m("287", Copy(R.string.k_charging_cycles_2), Mark.SYNC)),
            91, Copy(R.string.k_battery_health),
            listOf(f(Copy(R.string.k_critical_drain_apps), "3", Mark.WARN), Fact(Copy(R.string.k_safe_actions), Copy(R.string.k_t_5_available), Mark.CHECK), time(Copy(R.string.k_last_analysis))),
            Copy(R.string.k_optimization_actions),
            listOf(
                Entry(Copy(R.string.k_screen_brightness), Copy(R.string.k_high_brightness_increases_battery_drain), "−22%", Mark.SUN, Copy(R.string.k_auto_optimize)),
                Entry(Copy(R.string.k_background_sync), Copy(R.string.k_limit_non_essential_sync_to_save_power), "−14%", Mark.SYNC, Copy(R.string.k_restrict_sync)),
                Entry(Copy(R.string.k_app_wakeups), Copy(R.string.k_stop_frequent_background_wakeups), "−18%", Mark.LAYERS, Copy(R.string.k_limit_wakeups)),
                Entry(Copy(R.string.k_gps_usage), Copy(R.string.k_high_accuracy_gps_can_drain_battery), "−11%", Mark.LOCATION, Copy(R.string.k_use_while_active)),
                Entry(Copy(R.string.k_charging_health), Copy(R.string.k_optimize_charging_to_prolong_battery_life), "+7%", Mark.BATTERY, Copy(R.string.k_smart_charging))),
            a(Copy(R.string.k_view_details)), a(Copy(R.string.k_save_battery), Mark.BOLT), grouped = true
        ),
        Page.OPTIMIZE to PreviewScreen(
            Copy(R.string.k_boost_responsiveness_tune_performance_and_optimize_batte),
            listOf(m("94", Copy(R.string.k_performance_score_2), Mark.SPEED), m("+18%", Copy(R.string.k_battery_gain), Mark.BATTERY)),
            listOf(m("24% (−36%)", Copy(R.string.k_cpu_load), Mark.CHIP), m("1.6 GB", Copy(R.string.k_memory_freed), Mark.DISK),
                m("12", Copy(R.string.k_app_hibernation), Mark.LAYERS), m("Normal", Copy(R.string.k_thermal_status_2), Mark.TEMP)),
            94, Copy(R.string.k_optimized),
            listOf(f(Copy(R.string.k_recommended_actions), "5", Mark.CHECK), f(Copy(R.string.k_applied_safely), "5", Mark.SHIELD), time(Copy(R.string.k_last_tune_up))),
            Copy(R.string.k_optimization_tools_5),
            listOf(
                Entry(Copy(R.string.k_memory_boost), Copy(R.string.k_free_up_ram_and_improve_responsiveness), "+1.6 GB", Mark.MEMORY, note = Copy(R.string.k_freed)),
                Entry(Copy(R.string.k_battery_balancing), Copy(R.string.k_intelligently_manage_power_consumption), "+18%", Mark.BATTERY, note = Copy(R.string.k_battery_life_2)),
                Entry(Copy(R.string.k_smart_hibernation), Copy(R.string.k_put_inactive_apps_to_sleep_in_the_preview), "12 apps", Mark.LAYERS, note = Copy(R.string.k_hibernated)),
                Entry(Copy(R.string.k_startup_control), Copy(R.string.k_review_auto_start_apps_and_boot_time), "−4.2 s", Mark.POWER, note = Copy(R.string.k_boot_time)),
                Entry(Copy(R.string.k_thermal_guard), Copy(R.string.k_review_peak_temperature_recommendations), "−6°C", Mark.TEMP, note = Copy(R.string.k_peak_temp))),
            a(Copy(R.string.k_see_recommendations)), a(Copy(R.string.k_optimize_now), Mark.ROCKET), Selection.OPTIMIZATION
        )
    )
}
