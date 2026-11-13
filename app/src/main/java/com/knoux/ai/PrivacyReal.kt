package com.knoux.ai

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.knoux.ai.data.DataStatus
import com.knoux.ai.data.KnouxData
import com.knoux.ai.data.PrivacyAction
import com.knoux.ai.data.PrivacyAuditEntry
import com.knoux.ai.data.PrivacyCheck
import com.knoux.ai.data.PrivacyController
import com.knoux.ai.data.PrivacyObservation
import com.knoux.ai.data.PrivacyRemedy
import com.knoux.ai.data.PrivacyRules
import com.knoux.ai.data.PrivacySeverity
import com.knoux.ai.data.PrivacyStatus
import com.knoux.ai.data.PrivacySummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * PC04 — Privacy Sweep, wired to real Android state.
 *
 * The screen reports only what PackageManager, NetworkSecurityPolicy,
 * NotificationManager and the live window flag returned in this pass. Other apps'
 * permissions are shown as explicitly unmeasurable, and every Android screen is
 * opened as an OS-owned control — never as a change KNOUX made.
 */

internal enum class PrivacyPhase { IDLE, MEASURING, READY, ERROR }

internal data class PrivacyState(
    val phase: PrivacyPhase = PrivacyPhase.IDLE,
    val rows: List<PrivacyObservation> = emptyList(),
    val summary: PrivacySummary = PrivacyRules.summarize(emptyList()),
    val capturedAt: Long? = null,
    val scanId: String? = null,
    val previousScan: Boolean = false,
    val status: DataStatus? = null,
    val note: String? = null,
    val audit: List<PrivacyAuditEntry> = emptyList(),
    val secureWindow: Boolean = false,
    val busy: Boolean = false,
    val reobserveOnResume: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val measured: Boolean get() = phase == PrivacyPhase.READY
}

internal class PrivacyViewModel(private val controller: PrivacyController) : ViewModel() {

    private val _state = mutableStateOf(PrivacyState())
    val state: State<PrivacyState> get() = _state

    init {
        viewModelScope.launch {
            try {
                controller.secureWindow.collect { enabled ->
                    _state.value = _state.value.copy(secureWindow = enabled)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A preference read failure must not take down a measured screen.
            }
        }
        viewModelScope.launch {
            try {
                controller.observeAudit().collect { entries ->
                    _state.value = _state.value.copy(audit = entries)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    fun loadLatest() {
        if (_state.value.busy || _state.value.measured) return
        viewModelScope.launch {
            val report = runCatching { controller.latest() }.getOrNull() ?: return@launch
            _state.value = _state.value.copy(
                phase = PrivacyPhase.READY,
                rows = report.rows,
                summary = report.summary,
                capturedAt = report.capturedAtMillis,
                scanId = report.scanId,
                previousScan = true,
                status = report.status,
                note = report.note,
            )
        }
    }

    fun measure(windowSecure: Boolean) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(phase = PrivacyPhase.MEASURING, busy = true, error = null, message = null)
        viewModelScope.launch {
            val result = controller.measure(windowSecure)
            result.fold(
                onSuccess = { report ->
                    _state.value = _state.value.copy(
                        phase = PrivacyPhase.READY,
                        rows = report.rows,
                        summary = report.summary,
                        capturedAt = report.capturedAtMillis,
                        scanId = report.scanId,
                        previousScan = false,
                        status = report.status,
                        note = report.note,
                        busy = false,
                        reobserveOnResume = false,
                        message = if (report.attention.isEmpty()) {
                            "Measured on this device — no scored attention item"
                        } else {
                            "Measured on this device — ${report.attention.size} scored attention item(s)"
                        },
                    )
                },
                onFailure = { failure ->
                    _state.value = _state.value.copy(
                        phase = PrivacyPhase.ERROR,
                        busy = false,
                        error = failure.message ?: "The privacy pass could not be measured",
                    )
                },
            )
        }
    }

    /** Records that an Android screen is about to be launched. No change is claimed. */
    fun recordOsScreen(check: PrivacyCheck, resolved: Boolean, reobserve: Boolean) {
        viewModelScope.launch {
            runCatching { controller.recordOsScreenOpened(check, resolved, _state.value.scanId) }
            if (resolved && reobserve) {
                _state.value = _state.value.copy(reobserveOnResume = true)
            }
        }
    }

    fun recordReviewed() {
        val scanId = _state.value.scanId ?: return
        viewModelScope.launch {
            runCatching { controller.recordReviewed(scanId, _state.value.summary.attentionCount) }
        }
    }

    /**
     * Applies KNOUX's only privacy setting and then verifies the *actual* window
     * flag. [actualSecure] is read back from the live window after applying it.
     */
    fun setSecureWindow(enabled: Boolean, actualSecure: () -> Boolean, onApplied: (Boolean) -> Unit) {
        onApplied(enabled)
        viewModelScope.launch {
            val match = runCatching {
                controller.setSecureWindowPreference(enabled)
                val actual = actualSecure()
                controller.verifySecureWindow(enabled, actual, _state.value.scanId)
            }.getOrDefault(false)
            _state.value = _state.value.copy(
                secureWindow = enabled,
                message = if (match) {
                    "Screenshot protection ${if (enabled) "verified on" else "verified off"} by the window flag"
                } else {
                    "Android did not apply the window flag; the setting is stored but unverified"
                },
            )
            if (match) measure(actualSecure())
        }
    }
}

@Composable
internal fun rememberPrivacyViewModel(): PrivacyViewModel = viewModel(
    factory = viewModelFactory {
        initializer { PrivacyViewModel(KnouxData.privacy) }
    }
)

// ── Screen ───────────────────────────────────────────────────────────────────

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/**
 * Applies the persistent screenshot-protection preference to the live window.
 *
 * FLAG_SECURE is enforced by the OS, not by Compose: once set, Android refuses
 * screenshots and blanks the recents thumbnail for this task. The preference is
 * read from DataStore, so the flag survives navigation and process restarts.
 */
@Composable
internal fun SecureWindowEffect() {
    val view = LocalView.current
    val preference = remember { KnouxData.settings.privacySecureWindow }
    val secure by preference.collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(secure) {
        val activity = view.context.findActivity() ?: return@LaunchedEffect
        if (secure) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

@Composable
internal fun RealPrivacyScreen(page: Page) {
    val motion = LocalPreviewMotion.current
    val vm = rememberPrivacyViewModel()
    val state by vm.state
    val context = LocalContext.current
    val activity = context.findActivity()

    fun windowSecure(): Boolean =
        activity?.window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0

    fun applyWindowFlag(enabled: Boolean) {
        activity?.window?.let { window ->
            if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(Unit) { vm.loadLatest() }

    // Returning from an Android screen is the only moment at which the sweep
    // re-observes automatically: the customer may have changed a grant there.
    val reobserve = state.reobserveOnResume
    val currentReobserve by rememberUpdatedState(reobserve)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && currentReobserve) {
                vm.measure(windowSecure())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pill = when {
        state.phase == PrivacyPhase.MEASURING -> Copy(R.string.k_measuring_on_device)
        state.measured && state.previousScan -> Copy(R.string.k_previous_pass_measure_again_to_verify)
        state.measured && state.status == DataStatus.PARTIAL -> Copy(R.string.k_partial_some_checks_not_read)
        state.measured -> Copy(R.string.k_measured_on_device)
        state.phase == PrivacyPhase.ERROR -> Copy(R.string.k_measurement_failed_2)
        else -> Copy(R.string.k_not_measured_yet)
    }

    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.SHIELD, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle(
            Copy(R.string.k_privacy_2), Copy(R.string.k_sweep),
            Copy(R.string.k_real_android_state_for_this_app_knoux_cannot_read_anothe),
            "screen-${page.id}"
        )
        MasterPreviewPill(pill)

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).testTag("privacy-list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item { PrivacyScoreCard(state) }
            state.note?.let { note ->
                item { Label(note, 10, Gold, modifier = Modifier.fillMaxWidth().testTag("privacy-note")) }
            }
            state.message?.let { message ->
                item { Label(message, 10, Cyan, modifier = Modifier.fillMaxWidth().testTag("privacy-message")) }
            }
            state.error?.let { error ->
                item { Label(error, 10, Pink, modifier = Modifier.fillMaxWidth().testTag("privacy-error")) }
            }
            if (!state.measured || state.rows.isEmpty()) {
                item { PrivacyEmptyCard() }
            }
            items(state.rows, key = { it.check.name }) { row ->
                PrivacyRowCard(
                    row = row,
                    enabled = !state.busy && state.phase != PrivacyPhase.MEASURING,
                    onOpenOsScreen = {
                        val intent = KnouxData.privacy.osScreenIntent(row.check)
                        if (intent != null) {
                            val opened = runCatching { context.startActivity(intent) }.isSuccess
                            vm.recordOsScreen(row.check, opened, reobserve = true)
                        } else {
                            vm.recordOsScreen(row.check, false, reobserve = false)
                        }
                    },
                )
            }
            item {
                PrivacySecureCard(
                    enabled = state.secureWindow,
                    busy = state.busy,
                    onToggle = { enabled ->
                        applyWindowFlag(enabled)
                        vm.setSecureWindow(enabled, ::windowSecure, ::applyWindowFlag)
                    },
                )
            }
            if (state.audit.isNotEmpty()) {
                item { PrivacyAuditCard(state.audit) }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            AccentButton(
                Action(Copy(R.string.k_android_controls), Mark.GEAR),
                Modifier.weight(1f), primary = false, tag = "privacy-open-dashboard",
                onClick = {
                    val intent: Intent? = KnouxData.privacy.osScreenIntent(PrivacyCheck.ANDROID_PRIVACY_DASHBOARD)
                    if (intent != null) {
                        val opened = runCatching { context.startActivity(intent) }.isSuccess
                        vm.recordOsScreen(PrivacyCheck.ANDROID_PRIVACY_DASHBOARD, opened, reobserve = true)
                    } else {
                        vm.recordOsScreen(PrivacyCheck.ANDROID_PRIVACY_DASHBOARD, false, reobserve = false)
                    }
                },
            )
            AccentButton(
                if (state.busy) Action(Copy(R.string.k_measuring_2), Mark.RADAR)
                else Action(Copy(R.string.k_measure_now), Mark.RADAR),
                Modifier.weight(1f), primary = true, enabled = !state.busy,
                tag = "privacy-measure",
                onClick = {
                    vm.recordReviewed()
                    vm.measure(windowSecure())
                },
            )
        }
    }
}

// ── Pieces ───────────────────────────────────────────────────────────────────

private fun checkTitle(check: PrivacyCheck): Copy = when (check) {
    PrivacyCheck.NETWORK_EGRESS -> Copy(R.string.k_network_access)
    PrivacyCheck.ALL_FILES_ACCESS -> Copy(R.string.k_all_files_access)
    PrivacyCheck.SENSITIVE_PERMISSIONS -> Copy(R.string.k_sensitive_permissions)
    PrivacyCheck.MEDIA_ACCESS -> Copy(R.string.k_media_library_grant)
    PrivacyCheck.SAF_WRITE_GRANTS -> Copy(R.string.k_chosen_folders)
    PrivacyCheck.BACKUP_ALLOWED -> Copy(R.string.k_os_backup)
    PrivacyCheck.CLEARTEXT_TRAFFIC -> Copy(R.string.k_cleartext_traffic)
    PrivacyCheck.NOTIFICATIONS -> Copy(R.string.k_notifications)
    PrivacyCheck.SCREENSHOT_PROTECTION -> Copy(R.string.k_screenshot_protection)
    PrivacyCheck.DEBUGGABLE_BUILD -> Copy(R.string.k_build_type)
    PrivacyCheck.ANDROID_PRIVACY_DASHBOARD -> Copy(R.string.k_android_privacy_dashboard)
    PrivacyCheck.OTHER_APP_PRIVACY -> Copy(R.string.k_other_apps_2)
}

private fun severityTint(severity: PrivacySeverity): Color = when (severity) {
    PrivacySeverity.HIGH -> Pink
    PrivacySeverity.MEDIUM -> Gold
    PrivacySeverity.LOW -> Cyan
    PrivacySeverity.INFO -> Mist
}

private fun statusLabel(row: PrivacyObservation): Copy = when (row.status) {
    PrivacyStatus.PASS -> Copy(R.string.k_pass)
    PrivacyStatus.ATTENTION -> Copy(R.string.k_attention)
    PrivacyStatus.UNMEASURABLE -> Copy(R.string.k_not_measurable)
}

@Composable
private fun PrivacyScoreCard(state: PrivacyState) {
    GlassPanel(Modifier.fillMaxWidth(), tint = Pink) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(
                        state.summary.score?.let { "$it" } ?: "—",
                        34, if (state.measured) Pink else Mist, FontWeight.ExtraBold,
                        modifier = Modifier.testTag("privacy-score"),
                    )
                    Label(
                        if (state.measured) Copy(R.string.k_knoux_privacy_posture_measured_checks_only)
                        else Copy(R.string.k_not_measured_yet_3),
                        11, Mist,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Label(
                        "${state.summary.scoredCount} scored / ${state.rows.size} checks",
                        11, Chalk, FontWeight.Bold,
                        modifier = Modifier.testTag("privacy-scored-count"),
                    )
                    Label(
                        "${state.summary.attentionCount} attention · ${state.summary.unmeasurableCount} unmeasurable",
                        10, Mist,
                    )
                    state.capturedAt?.let { at ->
                        Label(
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(at)),
                            9, Mist,
                        )
                    }
                }
            }
            Label(state.summary.label, 10, Gold, modifier = Modifier.testTag("privacy-attention"))
            Label(
                "A score is 100 minus fixed penalties for scored items. It is not a security rating and it never includes checks Android did not expose.",
                9, Mist,
            )
        }
    }
}

@Composable
private fun PrivacyEmptyCard() {
    GlassPanel(Modifier.fillMaxWidth(), tint = Cyan) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Label(Copy(R.string.k_this_is_a_real_permission_sweep_not_a_preview), 13, Chalk, FontWeight.ExtraBold)
            Label(
                "Nothing has been measured in this session yet. Tap Measure now — the pass reads this app's manifest, grants, network policy, backup flag and the live window flag.",
                11, Mist,
            )
        }
    }
}

@Composable
private fun PrivacyRowCard(
    row: PrivacyObservation,
    enabled: Boolean,
    onOpenOsScreen: () -> Unit,
) {
    val tint = severityTint(row.severity)
    GlassPanel(Modifier.fillMaxWidth().testTag("privacy-check-${row.check.tag()}"), tint = tint) {
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(if (row.status == PrivacyStatus.UNMEASURABLE) Mark.EYE else Mark.SHIELD, tint, diameter = 28)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Label(checkTitle(row.check), 13, Chalk, FontWeight.ExtraBold)
                    Label(row.observed, 10, tint)
                }
                Pill(statusLabel(row), tint)
            }
            Label(row.detail, 10, Mist)
            when (row.remedy) {
                PrivacyRemedy.OS_SCREEN -> AccentButton(
                    Action(Copy(R.string.k_open_android_settings), Mark.GEAR),
                    Modifier.fillMaxWidth(), primary = false, enabled = enabled,
                    tag = "privacy-action-${row.check.tag()}", onClick = onOpenOsScreen,
                )
                PrivacyRemedy.APP_SETTING -> Label(
                    Copy(R.string.k_controls_below_in_screenshot_protection),
                    9, Cyan,
                )
                PrivacyRemedy.NONE, PrivacyRemedy.NOT_POSSIBLE -> Label(
                    Copy(R.string.k_no_action_inside_knoux), 9, Mist,
                )
            }
        }
    }
}

@Composable
private fun PrivacySecureCard(enabled: Boolean, busy: Boolean, onToggle: (Boolean) -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().testTag("privacy-secure-card"), tint = Cyan) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .toggleable(
                    value = enabled,
                    enabled = !busy,
                    role = Role.Switch,
                    onValueChange = onToggle,
                )
                .testTag("privacy-toggle-secure")
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconBadge(Mark.LOCK, Cyan, diameter = 34)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Label(Copy(R.string.k_screenshot_protection), 13, Chalk, FontWeight.ExtraBold)
                Label(
                    "Applies FLAG_SECURE to the KNOUX window. Android then blocks screenshots and the recents thumbnail for this app. KNOUX re-reads the flag afterwards.",
                    10, Mist,
                )
            }
            SwitchIndicator(checked = enabled)
        }
    }
}

@Composable
private fun SwitchIndicator(checked: Boolean) {
    Box(
        Modifier.width(48.dp).height(28.dp).clip(RoundedCornerShape(14.dp))
            .background(if (checked) Cyan.copy(alpha = .30f) else Mist.copy(alpha = .18f)),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(3.dp).width(22.dp).height(22.dp).clip(RoundedCornerShape(11.dp))
                .background(if (checked) Cyan else Mist)
        )
    }
}

@Composable
private fun PrivacyAuditCard(audit: List<PrivacyAuditEntry>) {
    GlassPanel(Modifier.fillMaxWidth().testTag("privacy-audit"), tint = Violet) {
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label(Copy(R.string.k_action_history), 12, Chalk, FontWeight.ExtraBold)
            audit.take(4).forEach { entry ->
                val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.createdAtMillis))
                Label(
                    "$time · ${entry.action.name.lowercase().replace('_', ' ')} · " +
                        "${entry.check?.tag() ?: "all checks"} · ${entry.outcome}",
                    9, Mist,
                )
            }
            Label(
                "Opening an Android screen is recorded as OPENED, not as a fix. Only a re-read flag is recorded as VERIFIED.",
                9, Mist,
            )
        }
    }
}

@Composable
private fun Pill(label: Copy, tint: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = .16f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Label(label, 9, tint, FontWeight.Bold)
    }
}

private fun PrivacyCheck.tag(): String = name.lowercase().replace('_', '-')
