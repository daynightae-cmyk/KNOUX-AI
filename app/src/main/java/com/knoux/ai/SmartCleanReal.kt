package com.knoux.ai

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.knoux.ai.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * PC01 — Smart Clean, wired to real device data.
 *
 * The approved visual language is untouched: same hero, same title block, same
 * gold disclosure capsule, same glass donut card, same category rows, same ghost
 * plus primary action pair. What changed is where the numbers come from.
 *
 * Everything below the title is either measured from the platform or explicitly
 * reported as not measured. There is no branch that prints a sample figure.
 */

// ── Formatting ───────────────────────────────────────────────────────────────

/** 1.24 GB style. Negative lengths are reported as unknown, never as 0 B. */
internal fun humanBytes(bytes: Long): String {
    if (bytes < 0L) return "—"
    if (bytes < 1024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024.0
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (value >= 100.0 || unit == 0) "%.0f %s".format(value, units[unit])
    else "%.2f %s".format(value, units[unit])
}

private fun agoLabel(millis: Long, arabic: Boolean = false): String {
    if (millis <= 0L) return ""
    val delta = System.currentTimeMillis() - millis
    fun say(@androidx.annotation.StringRes id: Int, value: Long): String =
        KnouxStrings.get(id, arabic, arrayOf<Any?>(KnouxFormat.number(value)))
    return when {
        delta < 60_000L -> KnouxStrings.get(R.string.time_just_now, arabic)
        delta < 3_600_000L -> say(R.string.time_minutes_ago, delta / 60_000L)
        delta < 86_400_000L -> say(R.string.time_hours_ago, delta / 3_600_000L)
        else -> say(R.string.time_days_ago, delta / 86_400_000L)
    }
}

// ── State ────────────────────────────────────────────────────────────────────

internal enum class CleanPhase { IDLE, SCANNING, READY, ERROR }

internal enum class CleanFilter(val label: Copy, val category: CandidateCategory?) {
    ALL(Copy(R.string.k_all), null),
    CACHE(Copy(R.string.k_cache_2), CandidateCategory.APP_CACHE),
    TEMP(Copy(R.string.k_temp), CandidateCategory.TEMP_FILE),
    THUMBS(Copy(R.string.k_thumbs), CandidateCategory.THUMBNAIL),
    IMAGES(Copy(R.string.k_images), CandidateCategory.IMAGE),
    VIDEOS(Copy(R.string.k_videos_4), CandidateCategory.VIDEO),
    SAF(Copy(R.string.k_folders), CandidateCategory.SAF_FILE)
}

internal enum class SizeFilter(val label: Copy, val minBytes: Long) {
    ANY(Copy(R.string.k_any_size), 0L),
    ONE_MB(Copy(R.string.k_t_1_mb), 1L shl 20),
    TEN_MB(Copy(R.string.k_t_10_mb), 10L shl 20)
}

internal enum class AgeFilter(val label: Copy, val minAgeMillis: Long) {
    ANY(Copy(R.string.k_any_age_2), 0L),
    WEEK(Copy(R.string.k_t_7_days_2), TimeUnit.DAYS.toMillis(7)),
    MONTH(Copy(R.string.k_t_30_days_2), TimeUnit.DAYS.toMillis(30))
}

internal data class CleanState(
    val phase: CleanPhase = CleanPhase.IDLE,
    val scopes: Set<AccessScope> = setOf(AccessScope.APP_CACHE),
    val candidates: List<FileCandidate> = emptyList(),
    val snapshots: List<SourceSnapshot> = emptyList(),
    val scannedAt: Long = 0L,
    val progressCount: Int = 0,
    val progressBytes: Long = 0L,
    val progressLabel: String = "",
    val scopesDone: Int = 0,
    val scopesTotal: Int = 0,
    val previousScan: Boolean = false,
    val exclusions: List<ExclusionEntity> = emptyList(),
    val pendingSender: Pair<String, IntentSender>? = null,
    val selected: Set<String> = emptySet(),
    val search: String = "",
    val filter: CleanFilter = CleanFilter.ALL,
    val sizeFilter: SizeFilter = SizeFilter.ANY,
    val ageFilter: AgeFilter = AgeFilter.ANY,
    val history: List<CleanupHistory> = emptyList(),
    val message: String? = null,
    val error: String? = null,
    /** A customer-facing sentence in the active language; [message] is engine prose. */
    val notice: Copy? = null,
    /** The file the customer chose to keep; the UI turns it into a friendly name. */
    val keptName: String? = null,
    val busy: Boolean = false,
    val mediaGrant: PermissionGrant = PermissionGrant.DENIED,
    val safTrees: Int = 0,
    val irreversibleAck: Boolean = false,
    val debugSchedule: Boolean = false
) {
    val eligible: List<FileCandidate> get() = candidates.filter { CleanupRules.isSelectable(it) }

    val visible: List<FileCandidate>
        get() {
            val needle = search.trim().lowercase()
            val cutoff = if (ageFilter.minAgeMillis == 0L) Long.MAX_VALUE
            else System.currentTimeMillis() - ageFilter.minAgeMillis
            return eligible
                .filter { filter.category == null || it.category == filter.category }
                .filter { it.bytes >= sizeFilter.minBytes }
                .filter { ageFilter == AgeFilter.ANY || it.lastModifiedMillis in 1..cutoff }
                .filter { needle.isEmpty() || it.displayName.lowercase().contains(needle) }
                .sortedByDescending { it.bytes }
        }

    val selectedCandidates: List<FileCandidate> get() = eligible.filter { it.uri in selected }

    val selectedBytes: Long get() = selectedCandidates.sumOf { it.bytes }

    val measuredBytes: Long get() = candidates.sumOf { it.bytes }

    /**
     * Trash exists only for shared media (Android's own trash). Cache files and
     * granted-folder documents have no trash, so offering it there would promise
     * a recovery that cannot happen. The button stays disabled instead.
     */
    val trashSupported: Boolean
        get() = selectedCandidates.isNotEmpty() &&
            selectedCandidates.all { it.provenance == DataProvenance.MEDIASTORE }
}

// ── ViewModel ────────────────────────────────────────────────────────────────

internal class SmartCleanViewModel(
    private val controller: SmartCleanController,
    private val repository: SmartCleanRepository,
    private val permissions: PermissionState,
    private val settings: SettingsStore,
    private val app: Context,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(CleanState())
    val state: State<CleanState> get() = _state

    private var scanJob: Job? = null
    private val progress = MutableStateFlow(SmartCleanRepository.ScanProgress(0, 0L, ""))
    private var localResultMessage: String? = null
    private val pendingId: String? get() = savedState["clean-pending-plan"]

    init {
        _state.value = _state.value.copy(busy = pendingId != null)
        viewModelScope.launch {
            controller.selectedScopes.collect { scopes ->
                _state.value = _state.value.copy(scopes = scopes)
            }
        }
        viewModelScope.launch {
            controller.observeHistory().collect { history ->
                _state.value = _state.value.copy(history = history)
            }
        }
        viewModelScope.launch {
            controller.observeExclusions().collect { kept ->
                _state.value = _state.value.copy(exclusions = kept)
            }
        }
        viewModelScope.launch {
            progress.collect { p ->
                if (_state.value.phase == CleanPhase.SCANNING) {
                    _state.value = _state.value.copy(
                        progressCount = p.itemsFound, progressBytes = p.bytesFound,
                        progressLabel = p.label, scopesDone = p.scopesDone, scopesTotal = p.scopesTotal
                    )
                }
            }
        }
        viewModelScope.launch {
            try {
                controller.recoverInterruptedOperations(pendingId)
                val prior = controller.latestScan()
                if (prior != null && _state.value.phase == CleanPhase.IDLE) {
                    _state.value = _state.value.copy(
                        phase = CleanPhase.READY, candidates = prior.candidates,
                        snapshots = prior.session.snapshots,
                        scannedAt = prior.session.finishedAtMillis ?: prior.session.startedAtMillis,
                        previousScan = true,
                        message = "Previous scan — scan again to refresh access and file state"
                    )
                }
            } catch (c: CancellationException) { throw c }
            catch (t: Exception) { _state.value = _state.value.copy(error = "History unavailable: ${t.message}") }
        }
        refreshGrants()
    }

    val scheduleGate: ProAccessDecision
        get() = if (_state.value.debugSchedule) {
            ProAccessDecision(
                featureId = "smart_clean_scheduled",
                source = ProEntitlementSource.DEBUG_TEST_ONLY,
                allowed = true,
                blockedReason = "Debug test-only unlock — this is not a subscription"
            )
        } else {
            ProAccessDecision(
                featureId = "smart_clean_scheduled",
                source = ProEntitlementSource.NONE,
                allowed = false,
                blockedReason = "CONFIGURATION_REQUIRED — no billing/entitlement backend is configured in this build"
            )
        }

    fun refreshGrants() {
        _state.value = _state.value.copy(
            mediaGrant = permissions.mediaGrant(),
            safTrees = permissions.safTrees(requireWrite = true).size
        )
    }

    fun scopeDescription(scope: AccessScope): Copy = permissions.describe(scope)

    fun requestableMediaPermissions(): Array<String> = permissions.mediaPermissions()

    /** Real, cancellable scan. Cancelling keeps whatever was already measured. */
    fun scan() {
        if (_state.value.phase == CleanPhase.SCANNING || _state.value.busy) return
        scanJob?.cancel()
        _state.value = _state.value.copy(
            phase = CleanPhase.SCANNING,
            error = null,
            message = null,
            progressCount = 0,
            progressBytes = 0,
            progressLabel = "", scopesDone = 0, scopesTotal = _state.value.scopes.size,
            selected = emptySet(), irreversibleAck = false, previousScan = false
        )
        val scopes = _state.value.scopes
        scanJob = viewModelScope.launch {
            val result = controller.scan(scopes) { progress.value = it }
            result.fold(
                onSuccess = { outcome ->
                    _state.value = _state.value.copy(
                        phase = CleanPhase.READY,
                        candidates = outcome.candidates,
                        snapshots = outcome.session.snapshots,
                        scannedAt = outcome.session.finishedAtMillis ?: System.currentTimeMillis(),
                        selected = emptySet(),
                        message = if (outcome.session.status == ScanStatus.PARTIAL)
                            "Partial coverage — ${outcome.candidates.size} visible item(s), ${humanBytes(outcome.session.totalBytes)}; review source access"
                        else if (outcome.candidates.isEmpty())
                            "Scan finished — nothing eligible in the scopes you granted"
                        else "Measured ${outcome.candidates.size} item(s), ${humanBytes(outcome.session.totalBytes)}"
                    )
                },
                onFailure = { failure ->
                    _state.value = _state.value.copy(
                        phase = CleanPhase.ERROR,
                        error = failure.message ?: "Scan failed"
                    )
                }
            )
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _state.value = _state.value.copy(
            phase = if (_state.value.candidates.isEmpty()) CleanPhase.IDLE else CleanPhase.READY,
            message = "Scan cancelled — nothing was removed"
        )
    }

    fun toggle(uri: String) {
        val eligibleNow = _state.value.eligible.any { it.uri == uri }
        if (_state.value.busy || !eligibleNow) return
        val current = _state.value.selected
        _state.value = _state.value.copy(
            selected = if (uri in current) current - uri else current + uri,
            irreversibleAck = false
        )
    }

    fun toggleGroup(index: Int, checked: Boolean) {
        if (_state.value.busy) return
        val group = _state.value.eligible.filter {
            when (index) {
                0 -> it.provenance == DataProvenance.APP_OWNED_CACHE
                1 -> it.category in setOf(CandidateCategory.TEMP_FILE, CandidateCategory.THUMBNAIL)
                else -> false
            }
        }.map { it.uri }.toSet()
        _state.value = _state.value.copy(
            selected = if (checked) _state.value.selected + group else _state.value.selected - group,
            irreversibleAck = false
        )
    }

    fun selectAllVisible() {
        if (_state.value.busy) return
        val visible = _state.value.visible.map { it.uri }.toSet()
        _state.value = _state.value.copy(selected = _state.value.selected + visible, irreversibleAck = false)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selected = emptySet(), irreversibleAck = false)
    }

    fun setSearch(text: String) {
        _state.value = _state.value.copy(search = text)
    }

    fun setFilter(filter: CleanFilter) {
        _state.value = _state.value.copy(filter = filter)
    }

    fun setSizeFilter(filter: SizeFilter) {
        _state.value = _state.value.copy(sizeFilter = filter)
    }

    fun setAgeFilter(filter: AgeFilter) {
        _state.value = _state.value.copy(ageFilter = filter)
    }

    fun acknowledgeIrreversible(value: Boolean) {
        _state.value = _state.value.copy(irreversibleAck = value)
    }

    fun setScopes(scopes: Set<AccessScope>, scanAfter: Boolean = false) {
        if (_state.value.busy || _state.value.phase == CleanPhase.SCANNING) return
        _state.value = _state.value.copy(scopes = scopes, selected = emptySet(), irreversibleAck = false)
        viewModelScope.launch {
            try {
                controller.setScopes(scopes)
                refreshGrants()
                if (scanAfter) scan()
            } catch (c: CancellationException) { throw c }
            catch (t: Exception) { _state.value = _state.value.copy(error = "Scope change failed: ${t.message}") }
        }
    }

    fun includeScope(scope: AccessScope, included: Boolean) =
        setScopes(if (included) _state.value.scopes + scope else _state.value.scopes - scope)

    fun afterMediaGrant() {
        refreshGrants()
        val add = if (_state.value.mediaGrant in setOf(PermissionGrant.GRANTED, PermissionGrant.PARTIAL_SELECTED))
            setOf(AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO) else emptySet()
        setScopes(_state.value.scopes + add, scanAfter = true)
    }

    fun afterFolderGrant() {
        refreshGrants()
        setScopes(_state.value.scopes + AccessScope.SAF_TREE, scanAfter = true)
    }

    fun unkeep(uri: String) {
        if (_state.value.busy) return
        viewModelScope.launch {
            try {
                controller.removeExclusion(uri)
                scan()
            } catch (c: CancellationException) { throw c }
            catch (t: Exception) { _state.value = _state.value.copy(error = "Could not remove exclusion: ${t.message}") }
        }
    }

    /** "Keep forever" is a real persisted exclusion, not a UI-only flag. */
    fun keep(candidate: FileCandidate) {
        viewModelScope.launch {
            controller.keepForever(candidate)
            _state.value = _state.value.copy(
                candidates = _state.value.candidates.filterNot { it.uri == candidate.uri },
                selected = _state.value.selected - candidate.uri,
                message = "Kept ${candidate.displayName} — it will not be offered again",
                keptName = candidate.displayName,
            )
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    /** Freeze the exact reviewed selection, then handle local and OS-mediated items separately. */
    fun executeSelection(kind: OperationKind) {
        val state = _state.value
        if (state.busy || state.phase == CleanPhase.SCANNING) return
        val items = state.selectedCandidates
        if (items.isEmpty() || (kind == OperationKind.PERMANENT_DELETE && !state.irreversibleAck)) {
            _state.value = state.copy(error = "Select files and acknowledge permanent deletion first")
            return
        }
        if (kind == OperationKind.MOVE_TO_TRASH && !state.trashSupported) {
            _state.value = state.copy(error = "Trash is available only when every selected item is shared media")
            return
        }
        // Set synchronously, before launch, to reject a second tap in the same frame.
        _state.value = state.copy(busy = true, error = null, message = null)
        localResultMessage = null
        viewModelScope.launch {
            try {
                val local = items.filter { it.provenance != DataProvenance.MEDIASTORE }
                if (local.isNotEmpty()) {
                    val plan = OperationPlan(controller.newPlanId(), kind, local, false, true)
                    val result = controller.executeLocal(plan, state.irreversibleAck)
                    localResultMessage = describe(result)
                    dropVerified(plan.id)
                }
                val media = items.filter { it.provenance == DataProvenance.MEDIASTORE }
                if (media.isNotEmpty()) {
                    val plan = OperationPlan(controller.newPlanId(), kind, media, true, true)
                    val sender = controller.prepareMediaOperation(plan, state.irreversibleAck)
                    if (sender == null) {
                        _state.value = _state.value.copy(busy = false, irreversibleAck = false,
                            message = localResultMessage, error = "Android request unavailable — media was not changed; retry after checking access")
                    } else {
                        savedState["clean-pending-plan"] = plan.id
                        _state.value = _state.value.copy(pendingSender = plan.id to sender)
                    }
                } else {
                    _state.value = _state.value.copy(busy = false, irreversibleAck = false, message = localResultMessage)
                }
            } catch (c: CancellationException) { throw c }
            catch (t: Exception) {
                _state.value = _state.value.copy(busy = false, irreversibleAck = false,
                    message = localResultMessage, error = "Operation interrupted: ${t.message}. Check history before retrying.")
            }
        }
    }

    fun senderLaunched(id: String) {
        if (_state.value.pendingSender?.first == id) _state.value = _state.value.copy(pendingSender = null)
    }

    /** The plan is restored from Room, not an in-memory candidate list. */
    fun completeMedia(approved: Boolean) {
        val id = pendingId ?: return
        savedState["clean-pending-plan"] = null
        _state.value = _state.value.copy(busy = true, pendingSender = null)
        viewModelScope.launch {
            try {
                val result = controller.completeMediaOperation(id, approved)
                dropVerified(id)
                _state.value = _state.value.copy(
                    busy = false, irreversibleAck = false,
                    message = listOfNotNull(localResultMessage, describe(result)).joinToString(" · ")
                )
            } catch (c: CancellationException) { throw c }
            catch (t: Exception) {
                _state.value = _state.value.copy(busy = false, irreversibleAck = false,
                    error = "Result could not be verified: ${t.message}. Check history; do not assume success.")
            }
        }
    }

    fun restoreTrash() {
        if (_state.value.busy || _state.value.phase == CleanPhase.SCANNING) return
        _state.value = _state.value.copy(busy = true, error = null)
        localResultMessage = null
        viewModelScope.launch {
            try {
                val request = controller.prepareRestoreLatest()
                if (request == null) {
                    _state.value = _state.value.copy(busy = false,
                        message = "No verified trashed items are still recoverable with the current access")
                } else {
                    savedState["clean-pending-plan"] = request.first
                    _state.value = _state.value.copy(pendingSender = request)
                }
            } catch (c: CancellationException) { throw c }
            catch (t: Exception) { _state.value = _state.value.copy(busy = false, error = "Restore unavailable: ${t.message}") }
        }
    }

    private suspend fun dropVerified(planId: String) {
        val removed = controller.verifiedRemovedUris(planId)
        if (removed.isEmpty()) return
        _state.value = _state.value.copy(
            candidates = _state.value.candidates.filterNot { it.uri in removed },
            selected = _state.value.selected - removed
        )
    }

    private fun describe(result: OperationResult): String = buildString {
        when (result.status) {
            OperationStatus.SUCCEEDED -> if (result.kind == OperationKind.RESTORE_FROM_TRASH)
                append("Restored ${result.removedCount} item(s) — verified present; scan again to refresh")
            else append("Removed ${result.removedCount} item(s) — verified gone")
            OperationStatus.PARTIAL -> append("Partially removed: ${result.removedCount} verified, ${result.failedCount} not")
            OperationStatus.REJECTED_BY_OS -> append("Android rejected the request — nothing removed")
            OperationStatus.CANCELLED -> append("Cancelled — nothing removed")
            else -> append("Failed — ${result.errors.firstOrNull() ?: "no item could be verified"}")
        }
        result.observed?.let { observed ->
            if (observed.deltaBytes > 0) {
                append(" · volume free +${humanBytes(observed.deltaBytes)} (observation)")
            }
        }
    }

    // ── Scheduling (scan only, never delete) ─────────────────────────────────

    fun setDebugSchedule(enabled: Boolean) {
        _state.value = _state.value.copy(debugSchedule = enabled)
    }

    fun enableSchedule(minutes: Long) {
        val gate = scheduleGate
        if (!gate.allowed) {
            _state.value = _state.value.copy(error = gate.blockedReason)
            return
        }
        viewModelScope.launch {
            settings.setSchedule(true, minutes)
            ScanWorker.schedule(app, minutes)
            _state.value = _state.value.copy(
                message = "Scheduled scan every $minutes min — scans only, never deletes",
                notice = Copy.args(R.string.clean_schedule_message, KnouxFormat.number(minutes)),
            )
        }
    }

    fun disableSchedule() {
        viewModelScope.launch {
            settings.setSchedule(false, 0L)
            ScanWorker.cancel(app)
            _state.value = _state.value.copy(
                message = "Scheduled scan cancelled",
                notice = Copy(R.string.clean_schedule_cancelled),
            )
        }
    }

}

@Composable
internal fun rememberSmartCleanViewModel(): SmartCleanViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            SmartCleanViewModel(
                controller = KnouxData.smartClean,
                repository = KnouxData.repository,
                permissions = KnouxData.permissions,
                settings = KnouxData.settings,
                app = KnouxData.application,
                savedState = createSavedStateHandle()
            )
        }
    }
)

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
internal fun RealSmartCleanScreen(open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    val vm = rememberSmartCleanViewModel()
    val state by vm.state
    val context = LocalContext.current

    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPermanent by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        vm.afterMediaGrant()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshGrants()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val treeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (t: SecurityException) {
                // A read-only provider is legitimate; keep the read grant so the
                // folder can still be scanned, but deletion there stays disabled.
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            vm.afterFolderGrant()
        }
    }

    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        vm.completeMedia(result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(state.pendingSender?.first) {
        state.pendingSender?.let { (id, sender) ->
            vm.senderLaunched(id)
            try { mediaLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
            catch (t: Exception) { vm.completeMedia(false) }
        }
    }

    val measured = state.phase == CleanPhase.READY
    val pill = when {
        state.phase == CleanPhase.SCANNING -> Copy(R.string.k_measuring_device)
        measured && state.previousScan -> Copy(R.string.k_previous_scan_refresh_to_verify)
        measured -> Copy(R.string.k_live_device_data)
        state.phase == CleanPhase.ERROR -> Copy(R.string.k_measurement_failed_2)
        else -> Copy(R.string.k_not_measured_yet)
    }

    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.TRASH, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle(
            Copy(R.string.k_smart_2), Copy(R.string.k_clean_2),
            Copy(R.string.k_review_real_files_before_any_cleanup),
            "screen-clean"
        )
        ServiceStatePill(pill)

        Column(
            Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            MeasuredSummaryCard(state)
            CleanGroupRow(
                index = 0,
                tag = "open-clean-center",
                title = Copy(R.string.k_cache_2),
                subtitle = Copy(R.string.k_knoux_s_own_cache_files),
                mark = Mark.LAYERS,
                bytes = state.candidates.filter { it.provenance == DataProvenance.APP_OWNED_CACHE }
                    .sumOf { it.bytes },
                count = state.candidates.count { it.provenance == DataProvenance.APP_OWNED_CACHE },
                available = measured,
                checked = state.candidates.any {
                    it.provenance == DataProvenance.APP_OWNED_CACHE && it.uri in state.selected
                },
                onBody = { open(Page.CLEAN_CENTER) },
                onToggle = { vm.toggleGroup(0, it) }
            )
            CleanGroupRow(
                index = 1,
                tag = "open-deep-scan",
                title = Copy(R.string.k_temporary_files_2),
                subtitle = Copy(R.string.k_temp_and_thumbnail_leftovers),
                mark = Mark.FILE,
                bytes = state.candidates
                    .filter { it.category == CandidateCategory.TEMP_FILE || it.category == CandidateCategory.THUMBNAIL }
                    .sumOf { it.bytes },
                count = state.candidates.count {
                    it.category == CandidateCategory.TEMP_FILE || it.category == CandidateCategory.THUMBNAIL
                },
                available = measured,
                checked = state.candidates.any {
                    (it.category == CandidateCategory.TEMP_FILE || it.category == CandidateCategory.THUMBNAIL) &&
                        it.uri in state.selected
                },
                onBody = { open(Page.SCAN) },
                onToggle = { vm.toggleGroup(1, it) }
            )
            CleanGroupRow(
                index = 2,
                tag = "open-duplicates",
                title = Copy(R.string.k_duplicate_candidates),
                subtitle = Copy(R.string.k_not_measured_by_smart_clean),
                mark = Mark.PICTURES,
                bytes = -1L,
                count = 0,
                available = false,
                checked = false,
                onBody = { open(Page.DUPLICATES) },
                onToggle = {}
            )
            CleanGroupRow(
                index = 3,
                tag = "open-residue",
                title = Copy(R.string.k_app_residue),
                subtitle = Copy(R.string.k_not_measured_by_smart_clean),
                mark = Mark.BOX,
                bytes = -1L,
                count = 0,
                available = false,
                checked = false,
                onBody = { open(Page.RESIDUE) },
                onToggle = {}
            )
        }

        // The action pair and live status stay pinned below the scrollable measurements:
        // on compact phones the cards can be taller than the viewport, and an off-screen
        // primary action would make scanning unreachable.
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 7.dp)) {
            MasterActions(
                Action(Copy(R.string.k_access_history), Mark.SHIELD),
                when {
                    state.phase == CleanPhase.SCANNING -> Action(Copy(R.string.k_cancel_scan), Mark.PAUSE)
                    measured -> Action(Copy(R.string.k_review_files), Mark.BROOM)
                    else -> Action(Copy(R.string.k_scan_device), Mark.RADAR)
                },
                onFirst = { sheet = "scope" },
                onSecond = {
                    when {
                        state.phase == CleanPhase.SCANNING -> vm.cancelScan()
                        measured -> {
                            confirmPermanent = false
                            sheet = "review"
                        }
                        else -> vm.scan()
                    }
                }
            )
            StatusStrip(state)
        }
    }

    if (sheet == "scope") {
        AccessSheet(
            state = state,
            vm = vm,
            onRequestMedia = { permissionLauncher.launch(vm.requestableMediaPermissions()) },
            onAddFolder = { treeLauncher.launch(null) },
            onOpenSettings = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                    )
                }
            },
            onDismiss = { sheet = null }
        )
    }

    if (sheet == "review") {
        ReviewSheet(
            state = state,
            vm = vm,
            confirmPermanent = confirmPermanent,
            onConfirmPermanentChange = { confirmPermanent = it },
            onTrash = { vm.executeSelection(OperationKind.MOVE_TO_TRASH) },
            onPermanent = { vm.executeSelection(OperationKind.PERMANENT_DELETE) },
            onOpenItem = { candidate ->
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(candidate.uri))
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    )
                }
            },
            onDismiss = { sheet = null }
        )
    }
}

// ── Pieces ───────────────────────────────────────────────────────────────────

/**
 * The donut card. When nothing was measured it says so instead of drawing a
 * confident ring around a placeholder number.
 */
@Composable
private fun MeasuredSummaryCard(state: CleanState) {
    val scanning = state.phase == CleanPhase.SCANNING
    val measured = state.phase == CleanPhase.READY
    val bytes = if (measured) state.measuredBytes else -1L
    val percent = when {
        scanning && state.scopesTotal > 0 -> state.scopesDone * 100 / state.scopesTotal
        measured && bytes > 0L -> (state.selectedBytes.toDouble() / bytes * 100).toInt()
        else -> 0
    }
    MasterCard(radius = 22, tint = Violet) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                ProgressDonut(
                    percent,
                    when {
                        scanning -> (Copy(R.string.k_scopes_completed)).localized()
                        measured -> (Copy(R.string.k_selected_share)).localized()
                        else -> (Copy(R.string.k_not_measured)).localized()
                    },
                    diameter = 104,
                    displayValue = if (measured || scanning) "$percent%" else "—"
                )
            }
            Separator(Modifier.width(1.dp).height(88.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MasterIcon(Mark.DISK, Pink, 34)
                Label((Copy(R.string.k_cleanup_potential)).localized(), 11, Mist)
                Label(if (measured) humanBytes(bytes) else "—", 22, weight = FontWeight.ExtraBold)
                Label(
                    when {
                        scanning -> (Copy.args(R.string.k_items_progress_summary, state.progressCount, humanBytes(state.progressBytes))).localized()
                        measured -> (Copy.args(R.string.k_candidates_summary, state.candidates.size, agoLabel(state.scannedAt, LocalArabic.current))).localized()
                        else -> (Copy(R.string.k_tap_scan_device_to_measure)).localized()
                    },
                    11, Mist
                )
            }
        }
        if (scanning) {
            val shown = if (state.scopesTotal > 0) state.scopesDone.toFloat() / state.scopesTotal else 0f
            GradientBar(shown, Modifier.padding(top = 2.dp), live = false)
        }
    }
}

@Composable
private fun CleanGroupRow(
    index: Int,
    tag: String,
    title: Copy,
    subtitle: Copy,
    mark: Mark,
    bytes: Long,
    count: Int,
    available: Boolean,
    checked: Boolean,
    onBody: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    MasterCard(radius = 16, tint = if (checked) Cyan else null) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .testTag(tag)
                    .clickable(role = Role.Button, onClick = onBody)
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                MasterIcon(mark, if (checked) Cyan else Violet, 34)
                Column(Modifier.weight(1f)) {
                    Label(title.localized(), 14, weight = FontWeight.Bold)
                    Label(
                        if (available) (Copy.args(R.string.clean_group_count, count, subtitle.localized())).localized()
                        else subtitle.localized(),
                        11, Mist
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Label(if (available) humanBytes(bytes) else "—", 14, weight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.size(28.dp)
                        .toggleable(checked, enabled = available, role = Role.Checkbox, onValueChange = onToggle)
                        .testTag("clean-toggle-$index"),
                    contentAlignment = Alignment.Center
                ) {
                    SelectionIndicator(checked && available, Cyan)
                }
            }
        }
    }
}

/**
 * The status strip replaces the old "design preview" note. It now reports the
 * true state of the last real operation, or the honest reason nothing ran.
 */
@Composable
private fun StatusStrip(state: CleanState) {
    val arabic = LocalArabic.current
    val text = when {
        state.error != null -> Copy(R.string.status_action_failed)
        state.keptName != null -> Copy.args(
            R.string.clean_kept_message,
            KnouxNames.friendly(state.keptName, arabic),
        )
        state.notice != null -> state.notice!!
        state.phase == CleanPhase.SCANNING -> Copy.args(
            R.string.clean_scanning_progress, KnouxFormat.number(state.progressCount)
        )
        state.candidates.isNotEmpty() -> Copy.args(
            R.string.clean_measured_at, agoLabel(state.scannedAt, arabic)
        )
        else -> Copy(R.string.clean_nothing_scanned_yet)
    }
    StateNote(text, Modifier.testTag("clean-status"))
}

// ── Access sheet ─────────────────────────────────────────────────────────────

@Composable
private fun AccessSheet(
    state: CleanState,
    vm: SmartCleanViewModel,
    onRequestMedia: () -> Unit,
    onAddFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    InfoSheet(Copy(R.string.k_access_history), onDismiss) {
        Label(
            (Copy(R.string.k_knoux_reads_only_what_you_grant_nothing_here_is_a_system)),
            12, Mist
        )

        ScopeRow(
            mark = Mark.LAYERS,
            title = Copy(R.string.k_knoux_cache),
            detail = vm.scopeDescription(AccessScope.APP_CACHE),
            action = null,
            tag = "scope-cache"
        )

        ScopeRow(
            mark = Mark.PICTURES,
            title = Copy(R.string.k_photos_videos),
            detail = when (state.mediaGrant) {
                PermissionGrant.GRANTED -> Copy(R.string.grant_granted)
                PermissionGrant.PARTIAL_SELECTED -> Copy(R.string.grant_partial)
                PermissionGrant.REVOKED -> Copy(R.string.grant_revoked)
                else -> Copy(R.string.grant_denied)
            },
            action = when (state.mediaGrant) {
                PermissionGrant.GRANTED -> Copy(R.string.k_manage_in_settings)
                else -> Copy(R.string.k_grant_access)
            },
            tag = "scope-media",
            onAction = if (state.mediaGrant == PermissionGrant.GRANTED) onOpenSettings else onRequestMedia
        )

        ScopeRow(
            mark = Mark.FOLDER,
            title = Copy(R.string.k_folders_you_granted),
            detail = if (state.safTrees == 0) Copy(R.string.scope_no_folder)
            else Copy.args(R.string.scope_folders_granted, KnouxFormat.number(state.safTrees)),
            action = Copy(R.string.k_add_folder),
            tag = "scope-saf",
            onAction = onAddFolder
        )

        Label((Copy(R.string.k_include_in_the_next_scan)), 13, Cyan, FontWeight.Bold)
        AccessScope.entries.forEach { scope ->
            val title = scopeLabel(scope)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(scope in state.scopes, enabled = !state.busy && state.phase != CleanPhase.SCANNING,
                        role = Role.Checkbox) { vm.includeScope(scope, it) }
                    .testTag("include-${scope.name}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                SelectionIndicator(scope in state.scopes, Cyan)
                Column(Modifier.weight(1f)) {
                    Label(title, 13, weight = FontWeight.Bold)
                    Label(vm.scopeDescription(scope), 11, Mist)
                }
            }
        }
        AccentButton(Action(Copy(R.string.k_scan_selected_sources), Mark.RADAR),
            Modifier.fillMaxWidth(), enabled = !state.busy && state.scopes.isNotEmpty() && state.phase != CleanPhase.SCANNING,
            tag = "scope-scan") { vm.scan() }

        if (state.exclusions.isNotEmpty()) {
            Label((Copy(R.string.k_kept_files_excluded_from_scans)), 13, Gold, FontWeight.Bold)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 180.dp)) {
                itemsIndexed(state.exclusions, key = { _, item -> item.uri }) { _, item ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Label(KnouxNames.friendly(item.displayName, LocalArabic.current), 12, Mist, modifier = Modifier.weight(1f), maxLines = 2)
                        AccentButton(Action(Copy(R.string.k_include_again), Mark.SYNC), primary = false,
                            enabled = !state.busy, tag = "unkeep-${item.uri}") { vm.unkeep(item.uri) }
                    }
                }
            }
        }

        if (state.snapshots.isNotEmpty()) {
            val arabic = LocalArabic.current
            TechnicalDetails(
                tag = "clean-technical",
                rows = state.snapshots.map { snapshot ->
                    scopeLabel(snapshot.scope) to buildString {
                        append(KnouxFormat.number(snapshot.itemCount))
                        append(" · ")
                        append(humanBytes(snapshot.totalBytes))
                        append(" · ")
                        append(dataStatusLabel(snapshot.status).text(arabic))
                    }
                },
            )
        }

        if (state.history.isNotEmpty()) {
            Label((Copy(R.string.technical_history)), 13, Cyan, FontWeight.Bold)
            val arabic = LocalArabic.current
            state.history.take(3).forEach { entry ->
                Label(
                    operationStatusLabel(entry.status).text(arabic) + " · " +
                        KnouxFormat.number(entry.itemCount) + " · " + humanBytes(entry.bytes) + " · " +
                        agoLabel(entry.completedAtMillis, arabic),
                    12, Mist, modifier = Modifier.testTag("clean-history")
                )
            }
            val notes = state.history.take(3).mapNotNull { entry ->
                entry.note?.let { Copy(R.string.technical_note) to it }
            }
            if (notes.isNotEmpty()) TechnicalDetails(rows = notes, tag = "clean-history-note")
        }
        AccentButton(Action(Copy(R.string.k_restore_last_recoverable_trash), Mark.SYNC),
            Modifier.fillMaxWidth(), primary = false, enabled = !state.busy,
            tag = "restore-trash") { vm.restoreTrash() }
        Label((Copy(R.string.k_only_android_verified_trashed_media_can_be_restored_cach)), 11, Gold)

        Label((Copy(R.string.k_scheduled_scan)), 13, Cyan, FontWeight.Bold)
        val gate = vm.scheduleGate
        Label(gate.blockedReason ?: "Allowed", 11, if (gate.allowed) Mint else Gold)
        if (gate.allowed) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentButton(Action(Copy(R.string.k_every_1_h), Mark.CLOCK), Modifier.weight(1f),
                    primary = false, tag = "schedule-1h") { vm.enableSchedule(60) }
                AccentButton(Action(Copy(R.string.k_cancel), Mark.WARN), Modifier.weight(1f),
                    primary = false, tag = "schedule-cancel") { vm.disableSchedule() }
            }
        }
        if (BuildConfig.DEBUG) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(state.debugSchedule, role = Role.Checkbox) { vm.setDebugSchedule(it) }
                    .testTag("debug-test-schedule"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                SelectionIndicator(state.debugSchedule, Gold)
                Label((Copy(R.string.k_debug_allow_test_only_schedule)), 11, Gold)
            }
        }
    }
}

@Composable
private fun ScopeRow(
    mark: Mark,
    title: Copy,
    detail: Copy,
    action: Copy?,
    tag: String,
    onAction: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        MasterIcon(mark, Cyan, 30)
        Column(Modifier.weight(1f)) {
            Label(title.localized(), 13, weight = FontWeight.Bold)
            Label(detail, 11, Mist)
        }
        if (action != null && onAction != null) {
            AccentButton(Action(action, Mark.CHECK), primary = false, tag = "$tag-action", onClick = onAction)
        }
    }
}

// ── Review sheet ─────────────────────────────────────────────────────────────

@Composable
private fun ReviewSheet(
    state: CleanState,
    vm: SmartCleanViewModel,
    confirmPermanent: Boolean,
    onConfirmPermanentChange: (Boolean) -> Unit,
    onTrash: () -> Unit,
    onPermanent: () -> Unit,
    onOpenItem: (FileCandidate) -> Unit,
    onDismiss: () -> Unit
) {
    InfoSheet(Copy(R.string.k_review_cleanup), onDismiss) {
        var showMoreFilters by rememberSaveable { mutableStateOf(false) }
        if (state.busy) Label((Copy(R.string.k_working)), 12, Cyan, modifier = Modifier.testTag("clean-busy"))
        state.notice?.let { Label(it, 12, Cyan, modifier = Modifier.testTag("clean-result")) }
        val technical = buildList {
            state.message?.let { add(Copy(R.string.technical_history) to it) }
            state.error?.let { add(Copy(R.string.technical_status) to it) }
        }
        if (technical.isNotEmpty()) TechnicalDetails(rows = technical, tag = "clean-result-technical")
        when (state.phase) {
            CleanPhase.SCANNING -> {
                Label(
                    Copy.args(R.string.k_scanning, state.progressCount, humanBytes(state.progressBytes)),
                    13, Cyan, FontWeight.Bold
                )
                AccentButton(Action(Copy(R.string.k_cancel_scan_2), Mark.PAUSE), Modifier.fillMaxWidth(),
                    primary = false, tag = "cancel-scan") { vm.cancelScan() }
                return@InfoSheet
            }
            CleanPhase.IDLE -> {
                Label(
                    (Copy(R.string.k_nothing_has_been_measured_yet_run_a_scan_first)),
                    13, Mist
                )
                AccentButton(Action(Copy(R.string.k_scan_device_2), Mark.RADAR), Modifier.fillMaxWidth(),
                    tag = "scan-from-sheet") { vm.scan() }
                return@InfoSheet
            }
            CleanPhase.ERROR -> {
                Label(Copy(R.string.status_action_failed), 13, Gold)
                AccentButton(Action(Copy(R.string.k_try_again), Mark.SYNC), Modifier.fillMaxWidth(),
                    tag = "retry-scan") { vm.scan() }
                return@InfoSheet
            }
            CleanPhase.READY -> Unit
        }

        if (state.candidates.isEmpty()) {
            Label(
                (Copy(R.string.k_no_eligible_items_were_found_in_the_sources_you_granted)),
                13, Mint, FontWeight.Bold, Modifier.testTag("clean-empty")
            )
            Label(
                (Copy(R.string.k_grant_another_source_from_access_history_or_scan_again_l)),
                11, Mist
            )
            return@InfoSheet
        }

        SearchField(state.search, vm::setSearch)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectChip(
                (Copy.args(R.string.k_select_all_3, state.visible.size)).localized(),
                checked = state.visible.isNotEmpty() && state.visible.all { it.uri in state.selected }
            ) { checked -> if (checked) vm.selectAllVisible() else vm.clearSelection() }
            if (state.selected.isNotEmpty()) {
                AccentButton(Action(Copy(R.string.k_clear), Mark.WARN), primary = false, tag = "clear-selection") {
                    vm.clearSelection()
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Label((Copy(R.string.k_filter_2)), 11, Mist, FontWeight.Bold)
            Box(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button) { showMoreFilters = !showMoreFilters }
                .padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                Label(
                    (Copy(if (showMoreFilters) R.string.k_fewer_filters else R.string.k_more_filters)).localized(),
                    12, Cyan, FontWeight.Medium
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CleanFilter.entries.forEach { filter ->
                FilterChip(filter.label, state.filter == filter, "filter-${filter.name}") { vm.setFilter(filter) }
            }
        }
        if (showMoreFilters) {
            Label((Copy(R.string.k_size_filter)), 11, Mist, FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SizeFilter.entries.forEach { filter ->
                    FilterChip(filter.label, state.sizeFilter == filter, "size-${filter.name}") { vm.setSizeFilter(filter) }
                }
            }
            Label((Copy(R.string.k_age_filter)), 11, Mist, FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AgeFilter.entries.forEach { filter ->
                    FilterChip(filter.label, state.ageFilter == filter, "age-${filter.name}") { vm.setAgeFilter(filter) }
                }
            }
        }

        Label(
            (Copy.args(R.string.k_visible_selected_summary, state.visible.size, state.selectedCandidates.size, humanBytes(state.selectedBytes))),
            12, Cyan, FontWeight.Bold, Modifier.testTag("clean-selection-summary")
        )

        // Bounded internal list: the root screen never scrolls, so primary actions
        // stay visible. Only this list scrolls.
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 300.dp).testTag("clean-list"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(state.visible, key = { _, item -> item.uri }) { index, candidate ->
                CandidateRow(
                    index = index,
                    candidate = candidate,
                    checked = candidate.uri in state.selected,
                    onToggle = { vm.toggle(candidate.uri) },
                    onKeep = { vm.keep(candidate) },
                    onOpen = { onOpenItem(candidate) }
                )
            }
        }

        if (state.notice != null) {
            Label(state.notice!!, 11, Cyan, modifier = Modifier.testTag("clean-result"))
        }
        if (state.error != null) {
            Label(Copy(R.string.status_action_failed), 11, Gold, modifier = Modifier.testTag("clean-error"))
        }

        AccentButton(
            Action(Copy.args(R.string.k_move_to_trash, KnouxFormat.number(state.selectedCandidates.size)), Mark.TRASH),
            Modifier.fillMaxWidth(), primary = false, enabled = state.selectedCandidates.isNotEmpty() && state.trashSupported,
            tag = "action-trash", onClick = onTrash
        )
        if (state.selectedCandidates.isEmpty()) {
            Label((Copy(R.string.clean_select_one_reason)), 11, Mist,
                modifier = Modifier.testTag("trash-disabled-reason"))
        } else if (!state.trashSupported) {
            Label(
                (Copy(R.string.k_trash_keeps_photos_and_videos_recoverable_cache_and_fold)),
                11, Gold, modifier = Modifier.testTag("trash-unsupported-reason")
            )
        }

        AccentButton(
            Action(Copy.args(R.string.k_delete_permanently, state.selectedCandidates.size), Mark.WARN),
            Modifier.fillMaxWidth(), primary = true, enabled = state.selectedCandidates.isNotEmpty(),
            tag = "action-delete"
        ) { onConfirmPermanentChange(!confirmPermanent) }
        if (state.selectedCandidates.isEmpty()) {
            Label((Copy(R.string.clean_select_one_reason)), 11, Mist,
                modifier = Modifier.testTag("delete-disabled-reason"))
        }

        if (confirmPermanent) {
            Label(
                (Copy(R.string.k_permanent_deletion_cannot_be_undone_android_will_ask_you)),
                12, Gold, FontWeight.Bold
            )
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(state.irreversibleAck, role = Role.Checkbox) { vm.acknowledgeIrreversible(it) }
                    .testTag("ack-irreversible"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                SelectionIndicator(state.irreversibleAck, Gold)
                Label((Copy(R.string.k_i_understand_this_cannot_be_undone)), 12, Gold)
            }
            AccentButton(
                Action(Copy(R.string.k_confirm_permanent_delete), Mark.WARN),
                Modifier.fillMaxWidth(),
                enabled = state.irreversibleAck && state.selectedCandidates.isNotEmpty() && !state.busy,
                tag = "confirm-delete", onClick = onPermanent
            )
            if (!state.irreversibleAck) {
                Label((Copy(R.string.clean_ack_reason)), 11, Mist,
                    modifier = Modifier.testTag("ack-disabled-reason"))
            }
        }

        if (state.busy) {
            Label((Copy(R.string.k_working)), 12, Cyan, modifier = Modifier.testTag("clean-busy"))
        }
        Label(
            (Copy(R.string.k_trash_keeps_recovery_where_android_supports_it_permanent)),
            11, Mist
        )
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xff150f38), RoundedCornerShape(16.dp))
            .border(1.dp, Cyan.copy(alpha = .45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Chalk, fontSize = 13.sp),
            cursorBrush = SolidColor(Cyan),
            modifier = Modifier.fillMaxWidth().testTag("clean-search"),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text((Copy(R.string.k_search_by_name)).localized(), color = Mist.copy(alpha = .7f), fontSize = 12.sp)
                }
                inner()
            }
        )
    }
}

@Composable
private fun FilterChip(label: Copy, selected: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).testTag(tag)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(20.dp))
                .background(
                    if (selected) Cyan.copy(alpha = .22f) else Color(0xff171037),
                    RoundedCornerShape(20.dp)
                )
                .border(1.dp, if (selected) Cyan else Mist.copy(alpha = .35f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Label(label.localized(), 10, if (selected) Cyan else Mist, FontWeight.Bold)
        }
    }
}

@Composable
private fun CandidateRow(
    index: Int,
    candidate: FileCandidate,
    checked: Boolean,
    onToggle: () -> Unit,
    onKeep: () -> Unit,
    onOpen: () -> Unit
) {
    Box(Modifier.fillMaxWidth().testTag("clean-item-$index")) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xff160f3a), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(30.dp)
                    .toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle() })
                    .testTag("clean-select-${candidate.displayName}"),
                contentAlignment = Alignment.Center
            ) { SelectionIndicator(checked, Cyan) }

            Column(Modifier.weight(1f)) {
                Label(KnouxNames.friendly(candidate.displayName, LocalArabic.current), 12, weight = FontWeight.Bold, maxLines = 1)
                Label(
                    when (candidate.provenance) {
                        DataProvenance.APP_OWNED_CACHE -> Copy(R.string.dup_own_cache)
                        DataProvenance.MEDIASTORE -> Copy(R.string.dup_shared_media)
                        else -> Copy(R.string.dup_granted_folder)
                    },
                    9, Mist, maxLines = 1,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Label(humanBytes(candidate.bytes), 11, Chalk, FontWeight.Bold)
                Label(
                    if (candidate.lastModifiedMillis > 0L) agoLabel(candidate.lastModifiedMillis, LocalArabic.current) else "—",
                    9, Mist
                )
            }

            if (candidate.provenance != DataProvenance.APP_OWNED_CACHE) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onOpen),
                    contentAlignment = Alignment.Center
                ) { Glyph(Mark.EYE, Modifier.size(15.dp), Cyan) }
            }
            Box(
                Modifier.heightIn(min = 30.dp).clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onKeep)
                    .testTag("clean-keep-${candidate.displayName}")
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) { Label((Copy(R.string.k_keep)), 9, Gold, FontWeight.Bold) }
        }
    }
}
