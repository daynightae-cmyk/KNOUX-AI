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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.knoux.ai.visual.MirrorFrame
import com.knoux.ai.visual.MirrorInstrument
import com.knoux.ai.visual.MirrorPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * PC02 — Duplicate Finder, wired to real content.
 *
 * The approved visual language (hero, split title, measured pill, glass cards) is
 * preserved. Every count below it comes from a real MediaStore/SAF/cache read and
 * a full-content SHA-256 comparison; there is no sample group and no placeholder
 * percentage. The original of each group is always protected and can never enter
 * a removal plan.
 */

// ── State ────────────────────────────────────────────────────────────────────

internal enum class DuplicatePhase { IDLE, SCANNING, READY, ERROR }

internal data class DuplicateState(
    val phase: DuplicatePhase = DuplicatePhase.IDLE,
    val groups: List<DuplicateGroup> = emptyList(),
    val selected: Set<String> = emptySet(),
    val scopes: Set<AccessScope> = setOf(AccessScope.APP_CACHE),
    val mediaGrant: PermissionGrant = PermissionGrant.DENIED,
    val safTrees: Int = 0,
    val busy: Boolean = false,
    val irreversibleAck: Boolean = false,
    val filesInspected: Int = 0,
    val filesHashed: Int = 0,
    val bytesHashed: Long = 0L,
    val groupsFound: Int = 0,
    val progressLabel: String = "",
    val scopesDone: Int = 0,
    val scopesTotal: Int = 0,
    val capturedAt: Long? = null,
    val scannedScopes: Set<AccessScope> = emptySet(),
    val status: DataStatus? = null,
    val note: String? = null,
    val previousScan: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val pendingSender: Pair<String, IntentSender>? = null,
) {
    val measured: Boolean get() = phase == DuplicatePhase.READY
    val redundantBytes: Long get() = groups.sumOf { it.redundantBytes }
    val selectedCandidates: List<FileCandidate>
        get() = DuplicateRules.selectedCandidates(groups, selected)
    val selectedBytes: Long get() = selectedCandidates.sumOf { it.bytes }
    val trashAvailable: Boolean get() = DuplicateRules.canTrash(selectedCandidates)
    val keeperUris: Set<String> get() = groups.map { it.keeper.uri }.toSet()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

internal class DuplicateViewModel(
    private val controller: DuplicateController,
    private val permissions: PermissionState,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = mutableStateOf(DuplicateState())
    val state: State<DuplicateState> get() = _state

    private val progress = MutableStateFlow<DuplicateRepository.Progress?>(null)
    private var scanJob: Job? = null
    private var localResultMessage: String? = null
    private val pendingPlanId: String? get() = savedState.get<String>("duplicate-pending-plan")

    init {
        viewModelScope.launch {
            try {
                controller.selectedScopes.collect { scopes ->
                    _state.value = _state.value.copy(scopes = scopes)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Scope observation failing must not take down a measured screen.
            }
        }
        viewModelScope.launch {
            progress.collect { value ->
                if (value != null && _state.value.phase == DuplicatePhase.SCANNING) {
                    _state.value = _state.value.copy(
                        progressLabel = value.label,
                        filesInspected = value.filesInspected,
                        filesHashed = value.filesHashed,
                        bytesHashed = value.bytesHashed,
                        groupsFound = value.groupsFound,
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

    /**
     * A plan remembered across process death can never be completed: the OS result
     * is gone with that process. Apply whatever the provider actually verified,
     * close the row honestly, and say so instead of implying success.
     */
    private suspend fun recoverRememberedPlan() {
        val planId = pendingPlanId ?: return
        _state.value = _state.value.copy(busy = true, message = null, error = null)
        try {
            // Recovery closes the interrupted row and re-queries the provider first;
            // only then can reconcileRemoved see which removals were actually proven.
            controller.recoverInterrupted(exceptPlanId = null)
            val verified = controller.reconcileRemoved(planId)
            savedState["duplicate-pending-plan"] = null
            _state.value = _state.value.copy(
                busy = false,
                message = if (verified.isEmpty()) {
                    "The previous Android confirmation was never verified — nothing is reported as removed"
                } else {
                    "Verified ${verified.size} removal(s) that completed before the app was interrupted"
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
            safTrees = permissions.safTrees().size,
        )
    }

    fun scopeDescription(scope: AccessScope): String = controller.describeScope(scope)

    fun requestableMediaPermissions(): Array<String> = permissions.mediaPermissions()

    fun afterMediaGrant() {
        refreshGrants()
        val add = if (permissions.mediaGrant(AccessScope.MEDIA_IMAGES) in
            setOf(PermissionGrant.GRANTED, PermissionGrant.PARTIAL_SELECTED)
        ) setOf(AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO) else emptySet()
        setScopes(_state.value.scopes + add, scanAfter = true)
    }

    fun afterFolderGrant() {
        refreshGrants()
        setScopes(_state.value.scopes + AccessScope.SAF_TREE, scanAfter = true)
    }

    fun setScopes(scopes: Set<AccessScope>, scanAfter: Boolean = false) {
        if (_state.value.busy || _state.value.phase == DuplicatePhase.SCANNING) return
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

    /** Duplicate discovery is explicit: no silent background read on screen entry. */
    fun scan() {
        val state = _state.value
        if (state.busy || state.phase == DuplicatePhase.SCANNING) return
        if (state.scopes.isEmpty()) {
            _state.value = state.copy(error = "Choose at least one source before scanning")
            return
        }
        _state.value = state.copy(
            phase = DuplicatePhase.SCANNING, groups = emptyList(), selected = emptySet(),
            irreversibleAck = false, message = null, error = null, note = null,
            filesInspected = 0, filesHashed = 0, bytesHashed = 0L, groupsFound = 0,
            progressLabel = "Starting", scopesDone = 0, scopesTotal = state.scopes.size,
        )
        scanJob = viewModelScope.launch {
            controller.scan(state.scopes) { value -> progress.value = value }.onSuccess { snapshot ->
                _state.value = _state.value.copy(
                    phase = DuplicatePhase.READY,
                    groups = snapshot.groups,
                    selected = emptySet(),
                    capturedAt = snapshot.capturedAtMillis,
                    scannedScopes = snapshot.scopes,
                    filesInspected = snapshot.fileCount,
                    filesHashed = snapshot.hashedCount,
                    bytesHashed = snapshot.bytesHashed,
                    groupsFound = snapshot.groups.size,
                    status = snapshot.status,
                    note = snapshot.note,
                    previousScan = false,
                    busy = false,
                    message = if (snapshot.groups.isEmpty()) {
                        "No byte-identical copies were found in the sources you granted"
                    } else null,
                    error = null,
                )
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                _state.value = _state.value.copy(
                    phase = DuplicatePhase.ERROR, busy = false,
                    error = "Duplicate scan failed: ${failure.message ?: failure.javaClass.simpleName}",
                )
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _state.value = _state.value.copy(
            phase = DuplicatePhase.IDLE,
            message = "Scan cancelled — incomplete results are not treated as verified",
            progressLabel = "",
            groupsFound = 0,
        )
        // An interrupted pass is not a result; show the last persisted measured pass
        // again instead of claiming no pass ever ran.
        viewModelScope.launch { reload(fromSavedSnapshot = true) }
    }

    fun toggle(uri: String) {
        val state = _state.value
        if (state.busy || state.phase == DuplicatePhase.SCANNING) return
        if (uri in state.keeperUris) {
            _state.value = state.copy(error = "The original copy is protected and is never removed")
            return
        }
        val candidate = state.groups.flatMap { it.duplicates }.firstOrNull { it.uri == uri } ?: return
        if (!CleanupRules.isSelectable(candidate)) {
            _state.value = state.copy(error = "This copy cannot be removed with the current access")
            return
        }
        _state.value = state.copy(
            selected = if (uri in state.selected) state.selected - uri else state.selected + uri,
            irreversibleAck = false,
        )
    }

    fun toggleGroup(group: DuplicateGroup, checked: Boolean) {
        if (_state.value.busy || _state.value.phase == DuplicatePhase.SCANNING) return
        _state.value = _state.value.copy(
            selected = DuplicateRules.toggleGroup(group, _state.value.selected, checked),
            irreversibleAck = false,
        )
    }

    fun selectAll() {
        val state = _state.value
        val all = state.groups.flatMap { DuplicateRules.selectableUris(it) }.toSet()
        _state.value = state.copy(selected = all, irreversibleAck = false)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selected = emptySet(), irreversibleAck = false)
    }

    fun acknowledgeIrreversible(value: Boolean) {
        _state.value = _state.value.copy(irreversibleAck = value)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    fun execute(kind: OperationKind) {
        val state = _state.value
        if (state.busy || state.phase == DuplicatePhase.SCANNING) return
        val items = state.selectedCandidates
        if (items.isEmpty()) {
            _state.value = state.copy(error = "Select at least one non-original copy first")
            return
        }
        // Defence in depth: a stale selection can never include a preserved original.
        if (items.any { it.uri in state.keeperUris }) {
            _state.value = state.copy(error = "Selection contains a protected original; nothing was executed")
            return
        }
        if (kind == OperationKind.PERMANENT_DELETE && !state.irreversibleAck) {
            _state.value = state.copy(error = "Acknowledge permanent deletion first")
            return
        }
        if (kind == OperationKind.MOVE_TO_TRASH && !DuplicateRules.canTrash(items)) {
            _state.value = state.copy(error = "Trash is available only when every selected copy is shared media")
            return
        }
        _state.value = state.copy(busy = true, error = null, message = null)
        localResultMessage = null
        viewModelScope.launch {
            try {
                val local = items.filter { it.provenance != DataProvenance.MEDIASTORE }
                if (local.isNotEmpty()) {
                    val plan = OperationPlan(controller.newPlanId(), kind, local, false, true)
                    localResultMessage = describe(controller.executeLocal(plan, state.irreversibleAck))
                    reload()
                }
                val media = items.filter { it.provenance == DataProvenance.MEDIASTORE }
                if (media.isNotEmpty()) {
                    val plan = OperationPlan(controller.newPlanId(), kind, media, true, true)
                    val sender = controller.prepareMedia(plan, state.irreversibleAck)
                    if (sender == null) {
                        reload()
                        _state.value = _state.value.copy(
                            busy = false, irreversibleAck = false, message = localResultMessage,
                            error = "Android request unavailable — media was not changed; retry after checking access",
                        )
                    } else {
                        savedState["duplicate-pending-plan"] = plan.id
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
        val id = _state.value.pendingSender?.first ?: savedState.get<String>("duplicate-pending-plan") ?: return
        savedState["duplicate-pending-plan"] = null
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

    private suspend fun reload(fromSavedSnapshot: Boolean = false) {
        if (_state.value.phase == DuplicatePhase.SCANNING) return
        val snapshot = try {
            controller.latest()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return
        val selectable = snapshot.groups.flatMap { DuplicateRules.selectableUris(it) }.toSet()
        _state.value = _state.value.copy(
            phase = DuplicatePhase.READY,
            groups = snapshot.groups,
            groupsFound = snapshot.groups.size,
            selected = _state.value.selected.intersect(selectable),
            capturedAt = snapshot.capturedAtMillis,
            scannedScopes = snapshot.scopes,
            filesInspected = snapshot.fileCount,
            filesHashed = snapshot.hashedCount,
            bytesHashed = snapshot.bytesHashed,
            status = snapshot.status,
            note = snapshot.note,
            previousScan = fromSavedSnapshot || _state.value.previousScan,
        )
    }

    private fun describe(result: OperationResult): String = buildString {
        when (result.status) {
            OperationStatus.SUCCEEDED -> if (result.kind == OperationKind.RESTORE_FROM_TRASH)
                append("Restored ${result.removedCount} original(s) — verified present")
            else append("Removed ${result.removedCount} duplicate(s) — verified by the provider")
            OperationStatus.PARTIAL -> append("Partially removed: ${result.removedCount} verified, ${result.failedCount} not")
            OperationStatus.REJECTED_BY_OS -> append("Android rejected the request — nothing removed")
            OperationStatus.CANCELLED -> append("Cancelled — nothing removed")
            else -> append("Failed — ${result.errors.firstOrNull() ?: "no copy could be verified"}")
        }
        result.observed?.let { observed ->
            if (observed.deltaBytes > 0) append(" · volume free +${humanBytes(observed.deltaBytes)} (observation)")
        }
        result.errors.firstOrNull { it.isNotBlank() }?.let { append(" · ${it.take(160)}") }
    }
}

@Composable
internal fun rememberDuplicateViewModel(): DuplicateViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            DuplicateViewModel(
                controller = KnouxData.duplicates,
                permissions = KnouxData.permissions,
                savedState = createSavedStateHandle(),
            )
        }
    }
)

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
internal fun RealDuplicateScreen(page: Page, open: (Page) -> Unit) {
    val motion = LocalPreviewMotion.current
    val vm = rememberDuplicateViewModel()
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
        state.phase == DuplicatePhase.SCANNING -> Copy(R.string.k_hashing_content)
        state.measured && state.previousScan -> Copy(R.string.k_previous_pass_scan_again_to_verify)
        state.measured && state.status == DataStatus.PARTIAL -> Copy(R.string.k_partial_some_files_not_verified)
        state.measured -> Copy(R.string.k_sha_256_verified_on_device)
        state.phase == DuplicatePhase.ERROR -> Copy(R.string.k_pass_failed)
        else -> Copy(R.string.k_not_measured_yet)
    }

    Column(Modifier.fillMaxSize()) {
        NeonHero(Mark.PICTURES, motion, Modifier.padding(top = 4.dp, bottom = 6.dp))
        MasterTitle(
            Copy(R.string.k_duplicate), Copy(R.string.k_finder),
            Copy(R.string.k_only_byte_identical_copies_are_shown_one_original_per_gr),
            "screen-${page.id}"
        )
        MasterPreviewPill(pill)

        // ── Mirror instrument: twin orbs joined by a tether ──────────────────
        val mirrorPhase = when (state.phase) {
            DuplicatePhase.IDLE -> if (state.groups.isNotEmpty()) MirrorPhase.RESULT else MirrorPhase.IDLE
            DuplicatePhase.SCANNING -> MirrorPhase.RUNNING
            DuplicatePhase.READY -> MirrorPhase.RESULT
            DuplicatePhase.ERROR -> MirrorPhase.FAILED
        }
        val mirrorFrame = MirrorFrame(
            phase = mirrorPhase,
            tension = if (state.groups.isNotEmpty()) 1f else null,
            heroValue = state.redundantBytes,
            groupCount = state.groups.size,
            motion = motion,
            caption = when {
                state.phase == DuplicatePhase.SCANNING -> Copy(R.string.k_hashing)
                state.measured && state.groups.isNotEmpty() -> Copy.args(R.string.k_groups_count, state.groups.size)
                state.measured -> Copy(R.string.k_no_copies)
                else -> Copy(R.string.k_not_measured_2)
            },
            hasFindings = state.groups.isNotEmpty(),
            widthDp = 340f,
            lastResultAgeMillis = state.capturedAt,
        )
        // P0 layout repair: the 320dp instrument used to consume the entire
        // viewport above this weighted results list on 360dp phones.
        // Keep the signature artwork, but place it INSIDE the list so real
        // result cards remain reachable and the action rail stays fixed.
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).testTag("duplicate-list"),
            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item { DuplicateSummaryCard(state) }
            item {
                MirrorInstrument(
                    frame = mirrorFrame,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
            state.note?.let { note ->
                item {
                    Label(
                        note, 10, Gold,
                        modifier = Modifier.fillMaxWidth().testTag("duplicate-note"),
                    )
                }
            }
            if (state.phase == DuplicatePhase.IDLE && state.groups.isEmpty()) {
                item { DuplicateEmptyCard(state) }
            }
            if (state.measured && state.groups.isEmpty()) {
                item { DuplicateEmptyCard(state) }
            }
            itemsIndexed(state.groups, key = { _, group -> group.id }) { index, group ->
                DuplicateGroupCard(
                    index = index,
                    group = group,
                    selected = state.selected,
                    enabled = !state.busy && state.phase != DuplicatePhase.SCANNING,
                    onToggleMember = vm::toggle,
                    onToggleGroup = { checked -> vm.toggleGroup(group, checked) },
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
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 7.dp)) {
            MasterActions(
                Action(Copy(R.string.k_sources_access), Mark.SHIELD),
                when {
                    state.phase == DuplicatePhase.SCANNING -> Action(Copy(R.string.k_cancel_scan), Mark.PAUSE)
                    state.measured -> Action(Copy(R.string.k_review_copies), Mark.COPY)
                    else -> Action(Copy(R.string.k_find_duplicates), Mark.RADAR)
                },
                onFirst = { sheet = "sources" },
                onSecond = {
                    when {
                        state.phase == DuplicatePhase.SCANNING -> vm.cancelScan()
                        state.measured -> {
                            // A new review always starts unacknowledged; the controller owns that flag.
                            vm.acknowledgeIrreversible(false)
                            sheet = "review"
                        }
                        else -> vm.scan()
                    }
                },
            )
            DuplicateStatusStrip(state)
        }
    }

    if (sheet == "sources") {
        DuplicateSourcesSheet(
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
        DuplicateReviewSheet(
            state = state,
            // One source of truth: the sheet renders the controller's acknowledgement,
            // and ticking it is what actually unblocks a permanent deletion.
            confirmPermanent = state.irreversibleAck,
            onConfirmPermanentChange = { vm.acknowledgeIrreversible(it) },
            onTrash = { vm.execute(OperationKind.MOVE_TO_TRASH) },
            onPermanent = { vm.execute(OperationKind.PERMANENT_DELETE) },
            onDismiss = { sheet = null },
        )
    }
}

// ── Pieces ───────────────────────────────────────────────────────────────────

@Composable
private fun DuplicateSummaryCard(state: DuplicateState) {
    val scanning = state.phase == DuplicatePhase.SCANNING
    GlassPanel(Modifier.fillMaxWidth(), tint = Pink) {
        Column(
            Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(
                        if (state.measured) humanBytes(state.redundantBytes) else "—",
                        30, if (state.measured) Pink else Mist, FontWeight.ExtraBold,
                        modifier = Modifier.testTag("duplicate-redundant"),
                    )
                    Label(
                        if (state.measured) Copy(R.string.k_redundant_copies_measured)
                        else Copy(R.string.k_not_measured_yet_2),
                        11, Mist,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Label(
                        "${state.groups.size}",
                        20, Chalk, FontWeight.Bold,
                        modifier = Modifier.testTag("duplicate-group-count"),
                    )
                    Label(Copy(R.string.k_groups), 10, Mist)
                }
            }
            GradientBar(
                if (state.filesHashed > 0) state.filesHashed.toFloat() / state.filesInspected.coerceAtLeast(1) else 0f,
                thickness = 8,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Label("${state.filesInspected} files inspected", 10, Mist, modifier = Modifier.weight(1f))
                Label("${state.filesHashed} hashed", 10, Mist, modifier = Modifier.weight(1f))
                Label(humanBytes(state.bytesHashed) + " read", 10, Mist)
            }
            state.capturedAt?.let { captured ->
                Label(
                    (if (state.previousScan) "Previous pass " else "Measured ") + dupAgo(captured),
                    10, Cyan,
                    modifier = Modifier.testTag("duplicate-captured"),
                )
            }
            if (scanning) {
                Label(
                    Copy.args(R.string.k_groups_found_progress, state.progressLabel, state.groupsFound),
                    11, Cyan, FontWeight.Bold,
                    modifier = Modifier.testTag("duplicate-progress"),
                )
            }
        }
    }
}

@Composable
private fun DuplicateEmptyCard(state: DuplicateState) {
    GlassPanel(Modifier.fillMaxWidth(), tint = Cyan) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label(
                when {
                    state.phase == DuplicatePhase.SCANNING -> Copy(R.string.k_reading_and_hashing_file_content)
                    state.measured -> Copy.args(R.string.k_no_byte_identical_copies_in_sources, state.scannedScopes.size)
                    else -> Copy(R.string.k_no_duplicate_pass_has_run_yet)
                },
                13, Chalk, FontWeight.Bold,
                modifier = Modifier.testTag("duplicate-empty"),
            )
            Label(
                (Copy(R.string.k_a_file_only_appears_here_after_its_full_content_was_read)),
                11, Mist,
            )
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    index: Int,
    group: DuplicateGroup,
    selected: Set<String>,
    enabled: Boolean,
    onToggleMember: (String) -> Unit,
    onToggleGroup: (Boolean) -> Unit,
    onOpen: (FileCandidate) -> Unit,
) {
    val selectable = DuplicateRules.selectableUris(group)
    val checked = selectable.isNotEmpty() && selected.containsAll(selectable)
    GlassPanel(
        Modifier.fillMaxWidth().testTag("dup-group-$index"),
        radius = 18,
        tint = Pink,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Mark.COPY, Pink, diameter = 28)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Label(
                        "${group.memberCount} identical copies", 13, Chalk, FontWeight.Bold,
                    )
                    Label(
                        "${humanBytes(group.bytes)} each · ${humanBytes(group.redundantBytes)} recoverable",
                        10, Mist,
                    )
                }
                Box(
                    Modifier.heightIn(min = 48.dp).testTag("dup-group-select-$index")
                        .clip(RoundedCornerShape(14.dp))
                        .toggleable(
                            value = checked,
                            enabled = enabled && selectable.isNotEmpty(),
                            role = Role.Checkbox,
                        ) { onToggleGroup(it) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SelectionIndicator(checked, Pink)
                }
            }

            // Preserved original — visibly locked, never toggleable.
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    .testTag("dup-keeper-${group.keeper.displayName}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Glyph(Mark.LOCK, Modifier.size(16.dp), Mint, true)
                Column(Modifier.weight(1f)) {
                    Label(group.keeper.displayName, 12, Mint, FontWeight.Bold, maxLines = 2)
                    Label("Original — kept", 10, Mint)
                }
                Label(humanBytes(group.keeper.bytes), 11, Mint, FontWeight.Bold)
            }

            group.duplicates.forEach { candidate ->
                val removable = candidate.uri in selectable
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("dup-member-${candidate.displayName}")
                        .clip(RoundedCornerShape(12.dp))
                        .toggleable(
                            value = candidate.uri in selected,
                            enabled = enabled && removable,
                            role = Role.Checkbox,
                        ) { onToggleMember(candidate.uri) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    val isImage = candidate.mimeType?.startsWith("image/") == true
                    val isVideo = candidate.mimeType?.startsWith("video/") == true

                    Row(
                        Modifier.fillMaxWidth().heightIn(max = 64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isVideo) Color(0xff00E8FF).copy(alpha = 0.2f)
                                    else if (isImage) Color(0xffFF00EE).copy(alpha = 0.2f)
                                    else Color(0xff1A1642)
                                )
                                .clickable { onOpen(candidate) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Glyph(
                                if (isVideo) Mark.VIDEO else if (isImage) Mark.PICTURES else Mark.FILE,
                                Modifier.size(16.dp),
                                Mist,
                                true,
                            )
                        }

                        SelectionIndicator(candidate.uri in selected, Cyan)

                        Column(Modifier.weight(1f)) {
                            Label(candidate.displayName, 12, Chalk, maxLines = 2)
                            Label(
                                when {
                                    !removable -> "This provider does not allow removal"
                                    candidate.provenance == DataProvenance.MEDIASTORE ->
                                        "Shared media — Android confirms removal"
                                    candidate.provenance == DataProvenance.APP_OWNED_CACHE ->
                                        "KNOUX cache copy — removed directly"
                                    else -> "Inside a folder you granted"
                                },
                                10, Mist,
                            )
                        }
                        Label(
                            humanBytes(candidate.bytes), 11, Chalk, FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .clickable { onOpen(candidate) }.padding(6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateStatusStrip(state: DuplicateState) {
    val text = when {
        state.error != null -> Copy(state.error ?: "", state.error ?: "")
        state.message != null -> Copy(state.message ?: "", state.message ?: "")
        state.phase == DuplicatePhase.SCANNING -> Copy(
            "Hashing ${state.filesHashed}/${state.filesInspected} · ${humanBytes(state.bytesHashed)}",
            "حساب البصمات ${state.filesHashed}/${state.filesInspected} · ${humanBytes(state.bytesHashed)}",
        )
        state.measured && state.selected.isNotEmpty() -> Copy(
            "${state.selectedCandidates.size} copy(ies) · ${humanBytes(state.selectedBytes)} selected",
            "${state.selectedCandidates.size} نسخة · ${humanBytes(state.selectedBytes)} محددة",
        )
        else -> Copy(
            "One original per group is always protected.",
            "يُحمى أصل واحد في كل مجموعة دائمًا.",
        )
    }
    val tint = when {
        state.error != null -> Gold
        state.message != null -> Mint
        else -> Mist
    }
    Label(
        text, 11, tint,
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
            .testTag(if (state.error != null) "duplicate-error" else "duplicate-status"),
        align = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

// ── Sheets ───────────────────────────────────────────────────────────────────

@Composable
private fun DuplicateSourcesSheet(
    state: DuplicateState,
    vm: DuplicateViewModel,
    onRequestMedia: () -> Unit,
    onAddFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    InfoSheet(Copy(R.string.k_duplicate_sources), onDismiss) {
        Label(
            (Copy(R.string.k_knoux_reads_only_the_sources_you_include_a_source_that_i)),
            12, Mist,
        )
        DuplicateScopeRow(
            mark = Mark.PICTURES,
            title = Copy(R.string.k_photos_videos),
            detail = when (state.mediaGrant) {
                PermissionGrant.GRANTED -> "Granted — library visible"
                PermissionGrant.PARTIAL_SELECTED -> "Partial — only selected items are visible"
                else -> "Not granted"
            },
            action = if (state.mediaGrant == PermissionGrant.GRANTED)
                Copy(R.string.k_manage_in_settings) else Copy(R.string.k_grant_access),
            tag = "dup-scope-media",
            onAction = if (state.mediaGrant == PermissionGrant.GRANTED) onOpenSettings else onRequestMedia,
        )
        DuplicateScopeRow(
            mark = Mark.FOLDER,
            title = Copy(R.string.k_folders_you_granted),
            detail = if (state.safTrees == 0) "No folder added" else "${state.safTrees} folder(s) granted",
            action = Copy(R.string.k_add_folder),
            tag = "dup-scope-saf",
            onAction = onAddFolder,
        )
        Label((Copy(R.string.k_include_in_the_next_pass)), 13, Cyan, FontWeight.Bold)
        AccessScope.entries.forEach { scope ->
            val title = when (scope) {
                AccessScope.APP_CACHE -> Copy(R.string.k_knoux_cache)
                AccessScope.MEDIA_IMAGES -> Copy(R.string.k_photos_2)
                AccessScope.MEDIA_VIDEO -> Copy(R.string.k_videos_3)
                AccessScope.SAF_TREE -> Copy(R.string.k_granted_folders)
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(
                        value = scope in state.scopes,
                        enabled = !state.busy && state.phase != DuplicatePhase.SCANNING,
                        role = Role.Checkbox,
                    ) { vm.includeScope(scope, it) }
                    .testTag("dup-include-${scope.name}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SelectionIndicator(scope in state.scopes, Cyan)
                Column(Modifier.weight(1f)) {
                    Label(title, 13, weight = FontWeight.Bold)
                    Label(vm.scopeDescription(scope), 11, Mist)
                }
            }
        }
        AccentButton(
            Action(Copy(R.string.k_scan_selected_sources), Mark.RADAR),
            Modifier.fillMaxWidth(),
            enabled = !state.busy && state.scopes.isNotEmpty() && state.phase != DuplicatePhase.SCANNING,
            tag = "dup-scope-scan",
        ) { vm.scan() }
    }
}

@Composable
private fun DuplicateScopeRow(
    mark: Mark,
    title: Copy,
    detail: String,
    action: Copy?,
    tag: String,
    onAction: (() -> Unit)?,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(mark, Cyan, diameter = 28)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Label(title, 13, Chalk, FontWeight.Bold)
            Label(detail, 11, Mist)
        }
        action?.let {
            AccentButton(Action(it, mark), primary = false, tag = tag) { onAction?.invoke() }
        }
    }
}

@Composable
private fun DuplicateReviewSheet(
    state: DuplicateState,
    confirmPermanent: Boolean,
    onConfirmPermanentChange: (Boolean) -> Unit,
    onTrash: () -> Unit,
    onPermanent: () -> Unit,
    onDismiss: () -> Unit,
) {
    InfoSheet(Copy(R.string.k_review_duplicate_removal), onDismiss) {
        state.message?.let {
            Label(it, 12, Mint, modifier = Modifier.testTag("duplicate-result"))
        }
        state.error?.let {
            Label(it, 12, Gold, modifier = Modifier.testTag("duplicate-review-error"))
        }
        if (state.busy) Label((Copy(R.string.k_working)), 12, Cyan)

        val copies = state.selectedCandidates
        val hasLocalCopies = copies.any { it.provenance != DataProvenance.MEDIASTORE }
        val hasSharedMedia = copies.any { it.provenance == DataProvenance.MEDIASTORE }
        if (copies.isEmpty()) {
            Label(
                (Copy(R.string.k_select_at_least_one_non_original_copy_the_original_in_ev)),
                13, Mist, modifier = Modifier.testTag("duplicate-no-selection"),
            )
        } else {
            Label(
                Copy.args(R.string.k_copies_summary, copies.size, humanBytes(state.selectedBytes)),
                15, Pink, FontWeight.Bold,
            )
            copies.take(40).forEach { copy ->
                val group = state.groups.firstOrNull { g -> g.duplicates.any { it.uri == copy.uri } }
                Label(
                    "${copy.displayName} · ${humanBytes(copy.bytes)} · original: ${group?.keeper?.displayName ?: "kept"}",
                    11, Mist, maxLines = 2,
                )
            }
        }

        Label(
            when {
                copies.isEmpty() -> Copy(
                    "One original per group is always preserved. Trash is recoverable; permanent delete is not.",
                    "يُحفظ أصل واحد في كل مجموعة دائمًا. السلة قابلة للاستعادة؛ الحذف النهائي لا.",
                )
                hasLocalCopies && hasSharedMedia -> Copy(
                    "KNOUX cache copies are deleted immediately. Android's dialog covers only the shared media; cancelling it leaves that media untouched.",
                    "تُحذف نسخ ذاكرة KNOUX المؤقتة فورًا. حوار أندرويد يغطي الوسائط المشتركة فقط؛ إلغاؤه يترك تلك الوسائط دون تغيير.",
                )
                hasSharedMedia -> Copy(
                    "Android shows its own confirmation for shared media. Cancelling there means nothing was removed.",
                    "يعرض أندرويد تأكيده الخاص للوسائط المشتركة. الإلغاء هناك يعني عدم إزالة أي شيء.",
                )
                else -> Copy(
                    "These copies are inside KNOUX's own cache and are deleted directly; no Android dialog is involved.",
                    "هذه النسخ داخل ذاكرة KNOUX المؤقتة وتُحذف مباشرة؛ لا يظهر حوار أندرويد.",
                )
            },
            11, Gold,
        )
        AccentButton(
            Action(Copy(R.string.k_move_copies_to_trash), Mark.TRASH),
            Modifier.fillMaxWidth(),
            primary = false,
            enabled = state.trashAvailable && !state.busy,
            tag = "duplicate-action-trash",
        ) { onTrash() }

        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(confirmPermanent, enabled = !state.busy, role = Role.Checkbox) {
                    onConfirmPermanentChange(it)
                }
                .testTag("duplicate-ack-irreversible"),
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
            Action(Copy(R.string.k_delete_copies_permanently), Mark.WARN),
            Modifier.fillMaxWidth(),
            enabled = confirmPermanent && copies.isNotEmpty() && !state.busy,
            tag = "duplicate-action-delete",
        ) { onPermanent() }
        Label(
            (Copy(R.string.k_local_cache_copies_are_removed_immediately_shared_media)),
            10, Mist,
        )
    }
}

private fun dupAgo(millis: Long): String {
    if (millis <= 0L) return ""
    val delta = System.currentTimeMillis() - millis
    return when {
        delta < 60_000L -> "just now"
        delta < 3_600_000L -> "${delta / 60_000L} min ago"
        delta < 86_400_000L -> "${delta / 3_600_000L} h ago"
        else -> "${delta / 86_400_000L} d ago"
    }
}
