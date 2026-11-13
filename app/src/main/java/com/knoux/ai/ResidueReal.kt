package com.knoux.ai

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.knoux.ai.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * PC03 — App Residue, wired to real content.
 *
 * The approved visual language is preserved. Every number below the hero comes from
 * a filesystem read of KNOUX's own cache, a provider walk inside a folder the
 * customer granted, or the proven MediaStore enumerator. Filename evidence is shown
 * next to each item; nothing is called "verified leftover" unless it is our cache.
 */

// ── State ────────────────────────────────────────────────────────────────────

internal enum class ResiduePhase { IDLE, SCANNING, READY, ERROR }

internal data class ResidueState(
    val phase: ResiduePhase = ResiduePhase.IDLE,
    val items: List<ResidueItem> = emptyList(),
    val selected: Set<String> = emptySet(),
    val scopes: Set<AccessScope> = setOf(AccessScope.APP_CACHE),
    val ageDays: Int = ResidueRules.DEFAULT_MIN_AGE_DAYS,
    val mediaGrant: PermissionGrant = PermissionGrant.DENIED,
    val safTrees: Int = 0,
    val audit: List<ResidueAuditEntry> = emptyList(),
    val busy: Boolean = false,
    val irreversibleAck: Boolean = false,
    val filesInspected: Int = 0,
    val progressLabel: String = "",
    val scopesDone: Int = 0,
    val scopesTotal: Int = 0,
    val capturedAt: Long? = null,
    val scanId: String? = null,
    val scannedScopes: Set<AccessScope> = emptySet(),
    val status: DataStatus? = null,
    val note: String? = null,
    val previousScan: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    /** A customer-facing sentence in the active language; [message] is engine prose. */
    val notice: Copy? = null,
    val pendingSender: Pair<String, IntentSender>? = null,
) {
    val measured: Boolean get() = phase == ResiduePhase.READY
    val summary: ResidueRules.Summary get() = ResidueRules.summarize(items)
    val selectedItems: List<ResidueItem> get() = ResidueRules.selectedItems(items, selected)
    val selectedBytes: Long get() = selectedItems.sumOf { it.candidate.bytes }
    val safeSelection: Set<String> get() = ResidueRules.safeSelection(items)
    val trashAvailable: Boolean get() = ResidueRules.canTrash(items, selected)
    val dryRun: ResidueDryRun get() = ResidueRules.dryRun(items, selected)
}

// ── ViewModel ────────────────────────────────────────────────────────────────

internal class ResidueViewModel(
    private val controller: ResidueController,
    private val permissions: PermissionState,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = mutableStateOf(ResidueState())
    val state: State<ResidueState> get() = _state

    private val progress = MutableStateFlow<ResidueRepository.Progress?>(null)
    private var scanJob: Job? = null
    private var localResultMessage: String? = null
    private val pendingPlanId: String? get() = savedState.get<String>("residue-pending-plan")

    init {
        viewModelScope.launch {
            try {
                controller.selectedScopes.collect { scopes -> _state.value = _state.value.copy(scopes = scopes) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Scope observation failing must not take down a measured screen.
            }
        }
        viewModelScope.launch {
            try {
                controller.minAgeDays.collect { days -> _state.value = _state.value.copy(ageDays = days) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
        viewModelScope.launch {
            try {
                controller.observeAudit().collect { entries -> _state.value = _state.value.copy(audit = entries) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
        viewModelScope.launch {
            progress.collect { value ->
                if (value != null && _state.value.phase == ResiduePhase.SCANNING) {
                    _state.value = _state.value.copy(
                        progressLabel = value.label,
                        filesInspected = value.filesInspected,
                        scopesDone = value.scopesDone,
                        scopesTotal = value.scopesTotal,
                    )
                }
            }
        }
        refreshGrants()
        viewModelScope.launch {
            recoverRememberedPlan()
            reload(fromSavedSnapshot = true)
        }
    }

    private suspend fun recoverRememberedPlan() {
        val planId = pendingPlanId ?: return
        _state.value = _state.value.copy(busy = true, message = null, error = null)
        try {
            controller.recoverInterrupted(exceptPlanId = null)
            val verified = controller.reconcileRemoved(planId)
            savedState["residue-pending-plan"] = null
            _state.value = _state.value.copy(
                busy = false,
                message = if (verified.isEmpty()) {
                    "The previous Android confirmation was never verified — nothing is reported as removed"
                } else {
                    "Verified ${verified.size} removal(s) that completed before the app was interrupted"
                },
                notice = if (verified.isEmpty()) {
                    Copy(R.string.recover_none_verified)
                } else {
                    Copy.args(R.string.recover_some_verified, KnouxFormat.number(verified.size))
                },
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Exception) {
            _state.value = _state.value.copy(
                busy = false,
                error = "The previous request could not be resolved: ${t.message}. Check history before retrying.",
            )
        }
    }

    fun refreshGrants() {
        _state.value = _state.value.copy(
            mediaGrant = permissions.mediaGrant(AccessScope.MEDIA_IMAGES),
            safTrees = permissions.safTrees(requireWrite = true).size,
        )
    }

    fun scopeDescription(scope: AccessScope): Copy = controller.describeScope(scope)

    fun requestableMediaPermissions(): Array<String> = permissions.mediaPermissions()

    fun afterMediaGrant() {
        refreshGrants()
        val add = if (permissions.mediaGrant(AccessScope.MEDIA_IMAGES) in
            setOf(PermissionGrant.GRANTED, PermissionGrant.PARTIAL_SELECTED)
        ) setOf(AccessScope.MEDIA_IMAGES) else emptySet()
        setScopes(_state.value.scopes + add, scanAfter = true)
    }

    fun afterFolderGrant() {
        refreshGrants()
        setScopes(_state.value.scopes + AccessScope.SAF_TREE, scanAfter = true)
    }

    fun setScopes(scopes: Set<AccessScope>, scanAfter: Boolean = false) {
        if (_state.value.busy || _state.value.phase == ResiduePhase.SCANNING) return
        _state.value = _state.value.copy(scopes = scopes, selected = emptySet(), irreversibleAck = false)
        viewModelScope.launch {
            try {
                controller.setScopes(scopes)
                if (scanAfter) scan()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Exception) {
                _state.value = _state.value.copy(error = "Scope change failed: ${t.message}")
            }
        }
    }

    fun includeScope(scope: AccessScope, included: Boolean) =
        setScopes(if (included) _state.value.scopes + scope else _state.value.scopes - scope)

    /** Own cache is the only scope that needs no grant, so it is never silently dropped. */
    fun setAgeDays(days: Int) {
        if (days !in ResidueRules.AGE_CHOICES) return
        _state.value = _state.value.copy(ageDays = days, selected = emptySet(), irreversibleAck = false)
        viewModelScope.launch {
            try {
                controller.setMinAgeDays(days)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Exception) {
                _state.value = _state.value.copy(error = "Age rule could not be saved: ${t.message}")
            }
        }
    }

    /** Residue discovery is explicit: no silent background read on screen entry. */
    fun scan() {
        val state = _state.value
        if (state.busy || state.phase == ResiduePhase.SCANNING) return
        if (state.scopes.isEmpty()) {
            _state.value = state.copy(error = "Choose at least one source before scanning")
            return
        }
        _state.value = state.copy(
            phase = ResiduePhase.SCANNING, items = emptyList(), selected = emptySet(),
            irreversibleAck = false, message = null, error = null, note = null,
            filesInspected = 0, progressLabel = "Starting", scopesDone = 0,
            scopesTotal = state.scopes.size,
        )
        scanJob = viewModelScope.launch {
            controller.scan(state.scopes, state.ageDays) { value -> progress.value = value }
                .onSuccess { snapshot ->
                    _state.value = _state.value.copy(
                        phase = ResiduePhase.READY,
                        items = snapshot.items,
                        selected = emptySet(),
                        capturedAt = snapshot.capturedAtMillis,
                        scanId = snapshot.scanId,
                        scannedScopes = snapshot.scopes,
                        filesInspected = snapshot.fileCount,
                        status = snapshot.status,
                        note = snapshot.note,
                        previousScan = false,
                        busy = false,
                        message = if (snapshot.items.isEmpty()) {
                            "No residue matched the current sources and age rule"
                        } else null,
                        error = null,
                    )
                }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    _state.value = _state.value.copy(
                        phase = ResiduePhase.ERROR, busy = false,
                        error = "Residue scan failed: ${failure.message ?: failure.javaClass.simpleName}",
                    )
                }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _state.value = _state.value.copy(
            phase = ResiduePhase.IDLE,
            message = "Scan cancelled — incomplete results are not treated as measured",
            notice = Copy(R.string.scan_cancelled_measured),
            progressLabel = "",
        )
        viewModelScope.launch { reload(fromSavedSnapshot = true) }
    }

    fun toggle(uri: String) {
        val state = _state.value
        if (state.busy || state.phase == ResiduePhase.SCANNING) return
        val item = state.items.firstOrNull { it.candidate.uri == uri } ?: return
        if (!ResidueRules.canSelect(item)) {
            _state.value = state.copy(error = "This item cannot be removed with the current access")
            return
        }
        _state.value = state.copy(
            selected = ResidueRules.toggle(state.items, state.selected, uri),
            irreversibleAck = false,
        )
    }

    fun selectSafe() {
        if (_state.value.busy || _state.value.phase == ResiduePhase.SCANNING) return
        _state.value = _state.value.copy(
            selected = _state.value.safeSelection,
            irreversibleAck = false,
            message = null,
            error = null,
        )
        record(ResidueAction.REVIEWED, _state.value.selectedItems, "safe cache selection")
    }

    fun clearSelection() {
        if (_state.value.selected.isNotEmpty()) {
            record(ResidueAction.SKIPPED, _state.value.selectedItems, "selection cleared")
        }
        _state.value = _state.value.copy(selected = emptySet(), irreversibleAck = false)
    }

    fun acknowledgeIrreversible(value: Boolean) {
        _state.value = _state.value.copy(irreversibleAck = value)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    /** Records what the customer was shown when the review opens. */
    fun reviewOpened() {
        val state = _state.value
        val run = state.dryRun
        record(ResidueAction.REVIEWED, run.items, "review opened · ${run.warnings.size} warning(s)")
    }

    /**
     * The dry run is recorded before anything is touched, so the audit shows the
     * exact plan that was confirmed — including a plan that Android later rejects.
     */
    fun confirmDryRun(kind: OperationKind) {
        val state = _state.value
        val run = state.dryRun
        if (!run.canExecute) {
            _state.value = state.copy(error = "Select at least one residue item first")
            return
        }
        record(ResidueAction.DRY_RUN, run.items, kind.name.lowercase())
    }

    private fun record(action: ResidueAction, items: List<ResidueItem>, note: String?) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            try {
                controller.recordDecision(action, _state.value.scanId, items, note)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // An audit write failing must not turn a measured screen into an error.
            }
        }
    }

    fun execute(kind: OperationKind) {
        val state = _state.value
        if (state.busy || state.phase == ResiduePhase.SCANNING) return
        val items = state.selectedItems
        if (items.isEmpty()) {
            _state.value = state.copy(error = "Select at least one residue item first")
            return
        }
        if (kind == OperationKind.PERMANENT_DELETE && !state.irreversibleAck) {
            _state.value = state.copy(error = "Acknowledge permanent deletion first")
            return
        }
        if (kind == OperationKind.MOVE_TO_TRASH && !state.trashAvailable) {
            _state.value = state.copy(error = "Trash is available only when every selected item is shared media")
            return
        }
        record(ResidueAction.ATTEMPTED, items, kind.name.lowercase())
        _state.value = state.copy(busy = true, error = null, message = null)
        localResultMessage = null
        viewModelScope.launch {
            try {
                val local = items.filter { it.candidate.provenance != DataProvenance.MEDIASTORE }
                if (local.isNotEmpty()) {
                    val plan = OperationPlan(
                        id = controller.newPlanId(),
                        kind = kind,
                        candidates = local.map { it.candidate },
                        requiresSystemConfirmation = false,
                        confirmedByUser = true,
                    )
                    localResultMessage = describe(controller.executeLocal(plan, state.irreversibleAck))
                    record(ResidueAction.VERIFIED, local, localResultMessage)
                    reload()
                }
                val media = items.filter { it.candidate.provenance == DataProvenance.MEDIASTORE }
                if (media.isNotEmpty()) {
                    val plan = OperationPlan(
                        id = controller.newPlanId(),
                        kind = kind,
                        candidates = media.map { it.candidate },
                        requiresSystemConfirmation = true,
                        confirmedByUser = true,
                    )
                    val sender = controller.prepareMedia(plan, state.irreversibleAck)
                    if (sender == null) {
                        reload()
                        _state.value = _state.value.copy(
                            busy = false, irreversibleAck = false, message = localResultMessage,
                            error = "Android request unavailable — shared media was not changed; retry after checking access",
                        )
                    } else {
                        savedState["residue-pending-plan"] = plan.id
                        _state.value = _state.value.copy(pendingSender = plan.id to sender)
                    }
                } else {
                    reload()
                    _state.value = _state.value.copy(
                        busy = false, irreversibleAck = false, message = localResultMessage,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Exception) {
                reload()
                _state.value = _state.value.copy(
                    busy = false, irreversibleAck = false, message = localResultMessage,
                    error = "Operation interrupted: ${t.message}. Check history before retrying.",
                )
            }
        }
    }

    fun senderLaunched(id: String) {
        if (_state.value.pendingSender?.first == id) {
            _state.value = _state.value.copy(pendingSender = null)
        }
    }

    fun completeMedia(approved: Boolean) {
        val id = _state.value.pendingSender?.first ?: savedState.get<String>("residue-pending-plan") ?: return
        savedState["residue-pending-plan"] = null
        _state.value = _state.value.copy(busy = true, pendingSender = null)
        viewModelScope.launch {
            try {
                val result = controller.completeMedia(id, approved)
                reload()
                _state.value = _state.value.copy(
                    busy = false, irreversibleAck = false,
                    message = listOfNotNull(localResultMessage, describe(result)).joinToString(" · "),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Exception) {
                reload()
                _state.value = _state.value.copy(
                    busy = false, irreversibleAck = false,
                    error = "Result could not be verified: ${t.message}. Check history; do not assume success.",
                )
            }
        }
    }

    fun scanId(): String? = null

    private suspend fun reload(fromSavedSnapshot: Boolean = false) {
        if (_state.value.phase == ResiduePhase.SCANNING) return
        val snapshot = try {
            controller.latest()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return
        val selectable = snapshot.items.map { it.candidate.uri }.toSet()
        _state.value = _state.value.copy(
            phase = ResiduePhase.READY,
            items = snapshot.items,
            selected = _state.value.selected.intersect(selectable),
            capturedAt = snapshot.capturedAtMillis,
            scanId = snapshot.scanId,
            scannedScopes = snapshot.scopes,
            ageDays = snapshot.minAgeDays,
            filesInspected = snapshot.fileCount,
            status = snapshot.status,
            note = snapshot.note,
            previousScan = fromSavedSnapshot || _state.value.previousScan,
        )
    }

    private fun describe(result: OperationResult): String = buildString {
        when (result.status) {
            OperationStatus.SUCCEEDED -> if (result.kind == OperationKind.RESTORE_FROM_TRASH) {
                append("Restored ${result.removedCount} item(s) — verified present")
            } else {
                append("Removed ${result.removedCount} residue item(s) — verified by the provider")
            }
            OperationStatus.PARTIAL -> append("Partially removed: ${result.removedCount} verified, ${result.failedCount} not")
            OperationStatus.REJECTED_BY_OS -> append("Android rejected the request — nothing removed")
            OperationStatus.CANCELLED -> append("Cancelled — nothing removed")
            else -> append("Failed — ${result.errors.firstOrNull() ?: "no removal could be verified"}")
        }
        result.observed?.let { observed ->
            if (observed.deltaBytes > 0) append(" · volume free +${humanBytes(observed.deltaBytes)} (observation)")
        }
        result.errors.firstOrNull { it.isNotBlank() }?.let { append(" · ${it.take(160)}") }
    }
}

@Composable
internal fun rememberResidueViewModel(): ResidueViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            ResidueViewModel(
                controller = KnouxData.residue,
                permissions = KnouxData.permissions,
                savedState = createSavedStateHandle(),
            )
        }
    }
)

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
internal fun RealResidueScreen(page: Page, open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    val vm = rememberResidueViewModel()
    val state by vm.state
    val context = LocalContext.current

    var sheet by rememberSaveable { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.afterMediaGrant() }

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
            } catch (_: SecurityException) {
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
            try {
                mediaLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } catch (_: Exception) {
                vm.completeMedia(false)
            }
        }
    }

    val pill = when {
        state.phase == ResiduePhase.SCANNING -> Copy(R.string.k_measuring_residue)
        state.measured && state.previousScan -> Copy(R.string.k_previous_pass_scan_again_to_verify)
        state.measured && state.status == DataStatus.PARTIAL -> Copy(R.string.k_partial_some_sources_not_read)
        state.measured -> Copy(R.string.k_measured_on_device)
        state.phase == ResiduePhase.ERROR -> Copy(R.string.k_pass_failed)
        else -> Copy(R.string.k_not_measured_yet)
    }

    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.LAYERS, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle(
            Copy(R.string.k_app), Copy(R.string.k_residue),
            Copy(R.string.k_leftovers_are_matched_by_filename_only_knoux_s_own_cache),
            "screen-${page.id}"
        )
        ServiceStatePill(pill)

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).testTag("residue-list"),
            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item { ResidueSummaryCard(state, vm::selectSafe, vm::clearSelection) }
            item { ResidueAgeCard(state, vm::setAgeDays) }
            state.note?.let { note ->
                item {
                    Label(
                        note, 10, Gold,
                        modifier = Modifier.fillMaxWidth().testTag("residue-note"),
                    )
                }
            }
            if (!state.measured || state.items.isEmpty()) {
                item { ResidueEmptyCard(state) }
            }
            items(state.items, key = { it.candidate.uri }) { item ->
                ResidueItemCard(
                    item = item,
                    checked = item.candidate.uri in state.selected,
                    enabled = !state.busy && state.phase != ResiduePhase.SCANNING,
                    onToggle = { vm.toggle(item.candidate.uri) },
                    onOpen = { candidate ->
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW)
                                    .setData(Uri.parse(candidate.uri))
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            )
                        }
                    },
                )
            }
            if (state.measured && state.items.isNotEmpty()) {
                item { ResidueAuditCard(state) }
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 7.dp)) {
            MasterActions(
                Action(Copy(R.string.k_sources_rules), Mark.SHIELD),
                when {
                    state.phase == ResiduePhase.SCANNING -> Action(Copy(R.string.k_cancel_scan), Mark.PAUSE)
                    state.measured -> Action(Copy(R.string.k_review_leftovers), Mark.TRASH)
                    else -> Action(Copy(R.string.k_measure_residue), Mark.RADAR)
                },
                onFirst = { sheet = "sources" },
                onSecond = {
                    when {
                        state.phase == ResiduePhase.SCANNING -> vm.cancelScan()
                        state.measured -> {
                            vm.acknowledgeIrreversible(false)
                            vm.reviewOpened()
                            sheet = "review"
                        }
                        else -> vm.scan()
                    }
                },
            )
            ResidueStatusStrip(state)
        }
    }

    if (sheet == "sources") {
        ResidueSourcesSheet(
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
            onDismiss = { sheet = null },
        )
    }

    if (sheet == "review") {
        ResidueReviewSheet(
            state = state,
            confirmPermanent = state.irreversibleAck,
            onConfirmPermanentChange = { vm.acknowledgeIrreversible(it) },
            onTrash = { vm.confirmDryRun(OperationKind.MOVE_TO_TRASH); vm.execute(OperationKind.MOVE_TO_TRASH) },
            onPermanent = { vm.confirmDryRun(OperationKind.PERMANENT_DELETE); vm.execute(OperationKind.PERMANENT_DELETE) },
            onDismiss = {
                if (state.selected.isNotEmpty()) vm.clearSelection()
                sheet = null
            },
        )
    }
}

// ── Pieces ───────────────────────────────────────────────────────────────────

@Composable
private fun ResidueSummaryCard(state: ResidueState, onSelectSafe: () -> Unit, onClear: () -> Unit) {
    val scanning = state.phase == ResiduePhase.SCANNING
    val summary = state.summary
    GlassPanel(Modifier.fillMaxWidth(), tint = Pink) {
        Column(
            Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(
                        if (state.measured) humanBytes(summary.bytes) else "—",
                        30, if (state.measured) Pink else Mist, FontWeight.ExtraBold,
                        modifier = Modifier.testTag("residue-total"),
                    )
                    Label(
                        if (state.measured) Copy(R.string.k_measured_residue)
                        else Copy(R.string.k_not_measured_yet_2),
                        11, Mist,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    val countable = state.measured || summary.count > 0
                    Label(
                        if (countable) KnouxFormat.number(summary.count) else "—",
                        20, if (countable) Chalk else Mist, FontWeight.Bold,
                        modifier = Modifier.testTag("residue-count"),
                    )
                    Label(Copy(R.string.k_items), 10, Mist)
                }
            }
            val countable = state.measured || summary.count > 0
            if (countable) {
                GradientBar(
                    if (summary.count > 0) summary.ownedCount.toFloat() / summary.count else 0f,
                    thickness = 8,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Label(
                    if (countable) Copy.args(R.string.residue_own_cache, KnouxFormat.number(summary.ownedCount))
                    else Copy.runtime("—"),
                    10, Mint, modifier = Modifier.weight(1f),
                )
                Label(
                    if (countable) Copy.args(R.string.residue_shared, KnouxFormat.number(summary.sharedCount))
                    else Copy.runtime("—"),
                    10, Gold, modifier = Modifier.weight(1f),
                )
                Label(
                    if (countable) Copy.args(R.string.residue_inspected, KnouxFormat.number(state.filesInspected))
                    else Copy.runtime("—"),
                    10, Mist,
                )
            }
            state.capturedAt?.let { captured ->
                Label(
                    if (state.previousScan) {
                        Copy.args(R.string.residue_previous_pass, residueAgo(captured, LocalArabic.current))
                    } else {
                        Copy.args(R.string.residue_measured_when, residueAgo(captured, LocalArabic.current))
                    },
                    10, Cyan,
                    modifier = Modifier.testTag("residue-captured"),
                )
            }
            if (state.measured && state.items.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label(
                        Copy(R.string.k_select_cache_items),
                        10, Mint, FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectSafe() }.padding(6.dp)
                            .testTag("residue-select-safe"),
                    )
                    Label(
                        Copy(R.string.k_clear_selection),
                        10, Mist,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .clickable { onClear() }.padding(6.dp)
                            .testTag("residue-clear"),
                    )
                }
            }
            if (scanning) {
                Label(
                    Copy.args(R.string.residue_groups_found, KnouxFormat.number(summary.count)),
                    11, Cyan, FontWeight.Bold,
                    modifier = Modifier.testTag("residue-progress"),
                )
            }
        }
    }
}

@Composable
private fun ResidueAgeCard(state: ResidueState, onAge: (Int) -> Unit) {
    GlassPanel(Modifier.fillMaxWidth(), tint = Cyan) {
        Column(
            Modifier.fillMaxWidth().padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Label(
                Copy(R.string.k_shared_folder_age_rule), 12, Chalk, FontWeight.Bold,
            )
            Label(
                Copy.args(R.string.k_residue_age_scope, state.ageDays),
                10, Mist,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ResidueRules.AGE_CHOICES.forEach { days ->
                    val active = days == state.ageDays
                    Box(
                        Modifier.weight(1f).heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) Cyan.copy(alpha = 0.22f) else Chalk.copy(alpha = 0.05f))
                            .toggleable(value = active, role = Role.RadioButton) { onAge(days) }
                            .testTag("residue-age-$days"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Label(
                            if (days == 0) Copy(R.string.k_any_age)
                            else Copy.args(R.string.age_days_short, KnouxFormat.number(days)),
                            10, if (active) Cyan else Mist, FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResidueEmptyCard(state: ResidueState) {
    GlassPanel(Modifier.fillMaxWidth(), tint = Cyan) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label(
                when {
                    state.phase == ResiduePhase.SCANNING -> Copy(R.string.k_reading_cache_and_granted_folders)
                    state.measured -> Copy.args(R.string.k_no_residue_matched_sources, state.scannedScopes.size)
                    else -> Copy(R.string.k_no_residue_pass_has_run_yet)
                },
                13, Chalk, FontWeight.Bold,
                modifier = Modifier.testTag("residue-empty"),
            )
            Label(
                (Copy(R.string.k_only_filenames_are_matched_a_file_is_never_called_abando)),
                11, Mist,
            )
        }
    }
}

@Composable
private fun ResidueItemCard(
    item: ResidueItem,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onOpen: (FileCandidate) -> Unit,
) {
    val candidate = item.candidate
    val selectable = ResidueRules.canSelect(item)
    val tint = if (item.confidence == ResidueConfidence.OWNED) Mint else Gold
    GlassPanel(
        Modifier.fillMaxWidth().testTag("residue-item-${candidate.displayName}"),
        radius = 16,
        tint = if (checked) Pink else tint,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Label(KnouxNames.friendly(candidate.displayName, LocalArabic.current), 12, Chalk, FontWeight.Bold, maxLines = 2)
                Label(
                    humanBytes(candidate.bytes) + " · " +
                        residuePatternLabel(item.pattern).text(LocalArabic.current) + " · " +
                        residueConfidenceLabel(item.confidence).text(LocalArabic.current),
                    10, tint,
                )
                Label(item.matchedOn, 9, Mist, maxLines = 1)
            }
            Box(
                Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .toggleable(
                        value = checked,
                        enabled = enabled && selectable,
                        role = Role.Checkbox,
                    ) { onToggle() }
                    .testTag("residue-select-${candidate.displayName}"),
                contentAlignment = Alignment.Center,
            ) {
                if (selectable) {
                    SelectionIndicator(checked, if (item.confidence == ResidueConfidence.OWNED) Mint else Gold)
                } else {
                    Glyph(Mark.LOCK, Modifier.size(15.dp), Mist, true)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Label(
                when (candidate.provenance) {
                    DataProvenance.APP_OWNED_CACHE -> Copy(R.string.residue_confidence_owned)
                    else -> Copy(R.string.residue_evidence_only)
                },
                9, Mist, modifier = Modifier.weight(1f), maxLines = 2,
            )
            Label(Copy(R.string.action_open_file), 9, Cyan, modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .clickable { onOpen(candidate) }.padding(4.dp))
        }
    }
}

@Composable
private fun ResidueAuditCard(state: ResidueState) {
    GlassPanel(Modifier.fillMaxWidth(), tint = Cyan) {
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Label(Copy(R.string.k_decision_history_on_this_device), 11, Chalk, FontWeight.Bold)
            if (state.audit.isEmpty()) {
                Label(Copy(R.string.k_no_decision_recorded_yet), 10, Mist)
            }
            state.audit.take(3).forEach { entry ->
                Label(
                    Copy.args(
                        R.string.residue_audit_line,
                        residueActionLabel(entry.action).text(LocalArabic.current),
                        KnouxFormat.number(entry.itemCount),
                        humanBytes(entry.bytes),
                    ),
                    10, Mist, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ResidueStatusStrip(state: ResidueState) {
    val text = when {
        state.error != null -> Copy(R.string.status_action_failed)
        state.notice != null -> state.notice!!
        state.phase == ResiduePhase.SCANNING -> Copy.args(
            R.string.residue_inspected,
            KnouxFormat.number(state.filesInspected),
        )
        state.measured && state.selected.isNotEmpty() -> Copy.args(
            R.string.residue_selected_summary,
            KnouxFormat.number(state.selectedItems.size),
            humanBytes(state.selectedBytes),
        )
        else -> Copy(R.string.residue_nothing_selected)
    }
    val tint = if (state.error != null) Gold else Mist
    Label(
        text, 11, tint,
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
            .testTag(if (state.error != null) "residue-error" else "residue-status"),
        align = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

// ── Sheets ───────────────────────────────────────────────────────────────────

@Composable
private fun ResidueSourcesSheet(
    state: ResidueState,
    vm: ResidueViewModel,
    onRequestMedia: () -> Unit,
    onAddFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    InfoSheet(Copy(R.string.k_residue_sources_rules), onDismiss) {
        Label(
            (Copy(R.string.k_knoux_reads_only_the_sources_you_include_a_source_that_i)),
            12, Mist,
        )
        ResidueScopeRow(
            mark = Mark.BOX,
            title = Copy(R.string.k_knoux_own_cache),
            detail = Copy(R.string.scope_cache_detail),
            checked = AccessScope.APP_CACHE in state.scopes,
            tag = "residue-scope-cache",
            onToggle = { vm.includeScope(AccessScope.APP_CACHE, it) },
        )
        ResidueScopeRow(
            mark = Mark.FOLDER,
            title = Copy(R.string.k_folders_you_granted),
            detail = if (state.safTrees == 0) Copy(R.string.scope_no_folder)
            else Copy.args(R.string.scope_folders_granted, KnouxFormat.number(state.safTrees)),
            checked = AccessScope.SAF_TREE in state.scopes,
            tag = "residue-scope-saf",
            onToggle = { vm.includeScope(AccessScope.SAF_TREE, it) },
            action = Copy(R.string.k_add_folder),
            onAction = onAddFolder,
        )
        ResidueScopeRow(
            mark = Mark.PICTURES,
            title = Copy(R.string.k_shared_media_names),
            detail = when (state.mediaGrant) {
                PermissionGrant.GRANTED -> Copy(R.string.grant_granted)
                PermissionGrant.PARTIAL_SELECTED -> Copy(R.string.grant_partial)
                else -> Copy(R.string.grant_denied)
            },
            checked = AccessScope.MEDIA_IMAGES in state.scopes,
            tag = "residue-scope-media",
            onToggle = { vm.includeScope(AccessScope.MEDIA_IMAGES, it) },
            action = if (state.mediaGrant == PermissionGrant.GRANTED)
                Copy(R.string.k_manage_in_settings) else Copy(R.string.k_grant_access),
            onAction = if (state.mediaGrant == PermissionGrant.GRANTED) onOpenSettings else onRequestMedia,
        )
        Label(
            (Copy(R.string.k_shared_media_can_only_be_moved_to_android_s_trash_knoux)),
            10, Gold,
        )
        AccentButton(
            Action(Copy(R.string.k_measure_selected_sources), Mark.RADAR),
            Modifier.fillMaxWidth(),
            tag = "residue-scope-scan",
            enabled = state.scopes.isNotEmpty() && state.phase != ResiduePhase.SCANNING,
        ) { onDismiss(); vm.scan() }
    }
}

@Composable
private fun ResidueScopeRow(
    mark: Mark,
    title: Copy,
    detail: Copy,
    checked: Boolean,
    tag: String,
    onToggle: (Boolean) -> Unit,
    action: Copy? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .toggleable(checked, role = Role.Checkbox) { onToggle(it) }
            .testTag(tag)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBadge(mark, if (checked) Cyan else Mist, diameter = 30)
        Column(Modifier.weight(1f)) {
            Label(title.localized(), 12, Chalk, FontWeight.Bold)
            Label(detail, 10, Mist, maxLines = 2)
        }
        if (action != null && onAction != null) {
            Label(
                action.localized(), 10, Cyan,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onAction() }.padding(6.dp),
            )
        }
        SelectionIndicator(checked, Cyan)
    }
}

@Composable
private fun ResidueReviewSheet(
    state: ResidueState,
    confirmPermanent: Boolean,
    onConfirmPermanentChange: (Boolean) -> Unit,
    onTrash: () -> Unit,
    onPermanent: () -> Unit,
    onDismiss: () -> Unit,
) {
    InfoSheet(Copy(R.string.k_review_residue_removal), onDismiss) {
        if (state.busy) Label((Copy(R.string.k_working)), 12, Cyan)
        state.notice?.let { Label(it, 12, Cyan, modifier = Modifier.testTag("residue-result")) }
        val technical = buildList {
            state.message?.let { add(Copy(R.string.technical_history) to it) }
            state.error?.let { add(Copy(R.string.technical_status) to it) }
        }
        if (technical.isNotEmpty()) TechnicalDetails(rows = technical, tag = "residue-result-technical")

        val run = ResidueRules.dryRun(state.items, state.selected)
        if (run.items.isEmpty()) {
            Label(
                (Copy(R.string.k_select_at_least_one_residue_item_nothing_is_pre_selected)),
                13, Mist, modifier = Modifier.testTag("residue-no-selection"),
            )
        } else {
            Label(
                Copy.args(R.string.k_items_bytes_summary, KnouxFormat.number(run.items.size), humanBytes(run.bytes)),
                15, Pink, FontWeight.Bold,
            )
            Label(
                run.patternSummary.ifBlank { Copy(R.string.residue_selected_items).text(LocalArabic.current) },
                10, Mist,
                modifier = Modifier.testTag("residue-dry-run"),
            )
            run.items.take(30).forEach { item ->
                Label(
                    KnouxNames.friendly(item.candidate.displayName, LocalArabic.current) + " · " +
                        humanBytes(item.candidate.bytes) + " · " +
                        residueConfidenceLabel(item.confidence).text(LocalArabic.current),
                    11, Mist, maxLines = 1,
                )
            }
        }

        if (run.warnings.isNotEmpty()) {
            TechnicalDetails(
                rows = run.warnings.map { Copy(R.string.technical_note) to it },
                tag = "residue-warnings",
            )
        }

        Label(
            when {
                run.sharedCount == 0 -> Copy(R.string.residue_cache_note)
                run.ownedCount == 0 -> Copy(R.string.dup_shared_note)
                else -> Copy(R.string.residue_mixed_note)
            },
            11, Gold,
        )

        AccentButton(
            Action(Copy(R.string.k_move_media_to_trash), Mark.TRASH),
            Modifier.fillMaxWidth(),
            primary = false,
            enabled = state.trashAvailable && !state.busy,
            tag = "residue-action-trash",
        ) { onTrash() }

        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(confirmPermanent, enabled = !state.busy, role = Role.Checkbox) {
                    onConfirmPermanentChange(it)
                }
                .testTag("residue-ack-irreversible"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectionIndicator(confirmPermanent, Gold)
            Label(
                (Copy(R.string.k_i_understand_permanent_deletion_cannot_be_undone)),
                12, Chalk,
            )
        }
        AccentButton(
            Action(Copy(R.string.k_delete_selected_permanently), Mark.WARN),
            Modifier.fillMaxWidth(),
            enabled = confirmPermanent && run.items.isNotEmpty() && !state.busy,
            tag = "residue-action-delete",
        ) { onPermanent() }
        Label(
            (Copy(R.string.k_own_cache_items_are_removed_immediately_shared_media_onl)),
            10, Mist,
        )
    }
}

private fun residueAgo(millis: Long, arabic: Boolean): String {
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
