package com.knoux.ai

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.DataStatus
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.KnouxData
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.OperationPlan
import com.knoux.ai.data.OperationResult
import com.knoux.ai.data.OperationStatus
import com.knoux.ai.data.PermissionGrant
import com.knoux.ai.visual.AbyssSonarInstrument
import com.knoux.ai.visual.DeepScanModule
import com.knoux.ai.visual.DeepScanOutcome
import com.knoux.ai.visual.DeepScanStatus
import com.knoux.ai.visual.FacetState
import com.knoux.ai.visual.InstrumentPhase
import com.knoux.ai.visual.formatBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * PC05 — Deep Scan, presented through the approved Abyss Sonar identity.
 *
 * The screen shows no preview numbers. While a pass runs the only numerals are
 * counters the engine has already published; the depth node stays at the surface
 * until a module really finishes, and unread modules keep their fog instead of
 * turning into a zero.
 *
 * Cleanup is not invented here either. The selection is frozen into an
 * [OperationPlan] and executed by the already-proven Smart Clean controller: local
 * cache/SAF items are removed directly, shared media goes through the Android
 * consent dialog, and every outcome is re-read before the evidence is reconciled.
 */
@Composable
internal fun RealDeepScanScreen(
    open: (Page) -> Unit,
    controller: com.knoux.ai.data.DeepScanController = remember { KnouxData.deepScan },
) {
    val arabic = LocalArabic.current
    val motion = LocalPreviewMotion.current
    val scope = rememberCoroutineScope()

    val frame by controller.instrumentFrame.collectAsStateWithLifecycle()
    val selection by controller.selectedModules.collectAsStateWithLifecycle()
    val outcome by controller.lastOutcome.collectAsStateWithLifecycle()

    var running by remember { mutableStateOf(false) }
    var showModules by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<OperationKind?>(null) }
    var acknowledged by remember { mutableStateOf(false) }
    var pendingSender by remember { mutableStateOf<Pair<String, IntentSender>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { controller.hydrate() }

    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val planId = pendingSender?.first
        pendingSender = null
        if (planId == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val operation = controller.completeMedia(planId, result.resultCode == Activity.RESULT_OK)
                controller.reconcileRemoval(planId)
                message = describe(operation, arabic)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Android request could not be completed"
            }
        }
    }

    LaunchedEffect(pendingSender?.first) {
        val pending = pendingSender ?: return@LaunchedEffect
        try {
            mediaLauncher.launch(IntentSenderRequest.Builder(pending.second).build())
        } catch (failure: Exception) {
            pendingSender = null
            error = "Android consent could not be shown: ${failure.javaClass.simpleName}"
        }
    }

    val permissions = KnouxData.permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) message = if (arabic) "تم منح وصول" else "Access granted"
        else error = if (arabic) "لم يُمنح وصول" else "Access was not granted"
    }
    val treeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { tree: Uri? ->
        if (tree != null) {
            try {
                KnouxData.application.contentResolver.takePersistableUriPermission(
                    tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                message = if (arabic) "أُضيف مجلد" else "Folder added"
            } catch (failure: Exception) {
                error = if (arabic) "تعذر حفظ وصول المجلد" else "Folder grant was not persisted"
            }
        }
    }

    fun startPass() {
        error = null
        message = null
        running = true
        scope.launch {
            try {
                val result = controller.scan(motion)
                result.exceptionOrNull()?.let { error = it.message ?: "Deep scan failed" }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Deep scan failed"
            } finally {
                running = false
            }
        }
    }

    fun execute(kind: OperationKind, items: List<com.knoux.ai.data.FileCandidate>) {
        if (items.isEmpty()) return
        val ack = kind == OperationKind.PERMANENT_DELETE
        val planId = controller.newPlanId()
        val plan = OperationPlan(
            id = planId,
            kind = kind,
            candidates = items,
            requiresSystemConfirmation = items.any { it.provenance == com.knoux.ai.data.DataProvenance.MEDIASTORE },
            confirmedByUser = true,
        )
        error = null
        message = null
        scope.launch {
            try {
                val local = items.filter { it.provenance != com.knoux.ai.data.DataProvenance.MEDIASTORE }
                if (local.isNotEmpty()) {
                    val localPlan = plan.copy(candidates = local, requiresSystemConfirmation = false)
                    val operation = controller.executeLocal(localPlan, ack)
                    controller.reconcileRemoval(localPlan.id)
                    message = describe(operation, arabic)
                }
                val media = items.filter { it.provenance == com.knoux.ai.data.DataProvenance.MEDIASTORE }
                if (media.isNotEmpty()) {
                    val mediaPlan = plan.copy(
                        id = controller.newPlanId(),
                        candidates = media,
                        requiresSystemConfirmation = true,
                    )
                    val sender = controller.prepareRemoval(mediaPlan, ack)
                    if (sender == null) {
                        error = if (arabic) "طلب أندرويد غير متاح — لم يتغير شيء"
                        else "Android request unavailable — nothing was changed"
                    } else {
                        pendingSender = mediaPlan.id to sender
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Operation interrupted; check history before retrying"
            }
        }
    }

    val ready = selection.isNotEmpty()
    val canReview = outcome?.removableCandidates?.isNotEmpty() == true &&
        frame.phase != InstrumentPhase.RUNNING

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Label(
                    text = Copy(R.string.k_deep_scan),
                    size = 22,
                    tint = Chalk,
                    weight = FontWeight.ExtraBold,
                )
                Label(
                    text = when {
                        running -> Copy(R.string.k_scanning_granted_scopes)
                        outcome == null -> Copy(R.string.k_nothing_measured_yet)
                        outcome?.status == DeepScanStatus.COMPLETE -> Copy(R.string.k_measured_on_this_device)
                        outcome?.status == DeepScanStatus.PARTIAL -> Copy(R.string.k_partly_measured)
                        outcome?.status == DeepScanStatus.UNAVAILABLE -> Copy(R.string.k_no_scope_could_be_read)
                        outcome?.status == DeepScanStatus.CANCELLED -> Copy(R.string.k_stopped_nothing_removed)
                        else -> Copy(R.string.k_measurement_failed)
                    },
                    size = 11,
                    tint = Mist,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val measured = frame.facets.count { it.state == FacetState.MEASURED || it.state == FacetState.THIN }
                val fogged = frame.facets.count { it.state == FacetState.FOGGED }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Cyan.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Label("$measured/${frame.facets.size}", 11, Cyan, FontWeight.Bold)
                }
                if (fogged > 0) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Gold.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Label("$fogged ✕", 11, Gold, FontWeight.Bold)
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 11.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AbyssSonarInstrument(
                frame = frame.copy(motion = motion),
                modifier = Modifier.fillMaxWidth(),
            )

            ModuleStrip(
                modules = controller.modules,
                selection = selection,
                frame = frame,
                onEdit = { showModules = true },
            )

            if (running) {
                OutlinedButton(
                    onClick = { controller.cancel() },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("deep-scan-stop"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Pink),
                ) {
                    Label(Copy(R.string.k_stop), 15, Pink, FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { startPass() },
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("deep-scan-start"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xff00D4AA),
                        contentColor = Color(0xff07051A),
                        disabledContainerColor = Color(0xff1A1642),
                        disabledContentColor = Muted,
                    ),
                ) {
                    Label(
                        text = if (outcome == null) Copy(R.string.k_start_deep_scan)
                        else Copy(R.string.k_scan_again),
                        size = 15,
                        tint = if (ready) Color(0xff07051A) else Muted,
                        weight = FontWeight.ExtraBold,
                    )
                }
            }

            error?.let { text ->
                NoticeCard(text, Pink) { error = null }
            }
            message?.let { text ->
                NoticeCard(text, Color(0xff00D4AA)) { message = null }
            }

            outcome?.let { measured ->
                OutcomeCard(
                    outcome = measured,
                    onReview = { showReview = true },
                    reviewEnabled = canReview,
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }

    if (showModules) {
        ModuleSheet(
            modules = controller.modules,
            selection = selection,
            arabic = arabic,
            onToggle = { id, enabled -> scope.launch { controller.toggleModule(id, enabled) } },
            onAll = { enabled -> scope.launch { controller.selectAll(enabled) } },
            onRequestPermissions = { permissionLauncher.launch(permissions.mediaPermissions()) },
            onAddFolder = { treeLauncher.launch(null) },
            onDismiss = { showModules = false },
        )
    }

    if (showReview) {
        outcome?.let { measured ->
            ReviewDialog(
                outcome = measured,
                arabic = arabic,
                onDismiss = { showReview = false },
                onTrash = { items -> showReview = false; confirm = OperationKind.MOVE_TO_TRASH },
                onDelete = { showReview = false; confirm = OperationKind.PERMANENT_DELETE },
            )
        }
    }

    confirm?.let { kind ->
        val removable = outcome?.removableCandidates.orEmpty()
        val bytes = removable.sumOf { it.bytes }
        val irreversible = kind == OperationKind.PERMANENT_DELETE
        AlertDialog(
            onDismissRequest = { confirm = null; acknowledged = false },
            title = {
                Label(
                    text = if (irreversible) Copy(R.string.k_delete_permanently)
                    else Copy(R.string.k_move_to_android_trash),
                    size = 19,
                    tint = Chalk,
                    weight = FontWeight.ExtraBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label(
                        text = Copy.args(R.string.k_files_summary, removable.size, formatBytes(bytes)),
                        size = 13,
                        tint = Mist,
                    )
                    Label(
                        text = if (irreversible)
                            Copy(R.string.k_the_os_may_ask_for_approval_verified_removals_cannot_be)
                        else
                            Copy(R.string.k_shared_media_is_moved_by_android_so_it_can_be_restored_f),
                        size = 12,
                        tint = Mist,
                    )
                    if (irreversible) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { acknowledged = !acknowledged },
                        ) {
                            Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it })
                            Label(Copy(R.string.k_i_understand_this_is_irreversible), 12, Gold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val plan = kind
                        confirm = null
                        acknowledged = false
                        execute(plan, removable)
                    },
                    enabled = !irreversible || acknowledged,
                ) {
                    Label(
                        text = if (irreversible) Copy(R.string.k_delete) else Copy(R.string.k_move),
                        size = 15,
                        tint = if (irreversible) Pink else Color(0xff00D4AA),
                        weight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null; acknowledged = false }) {
                    Label(Copy(R.string.k_cancel), 14, Mist)
                }
            },
        )
    }
}

@Composable
private fun ModuleStrip(
    modules: List<DeepScanModule>,
    selection: Set<String>,
    frame: com.knoux.ai.visual.InstrumentFrame,
    onEdit: () -> Unit,
) {
    val facets = frame.facets.associateBy { it.id }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        modules.forEach { module ->
            val facet = facets[module.id]
            val color = when (facet?.state) {
                FacetState.MEASURED -> Color(0xff00D4AA)
                FacetState.THIN -> Color(0xff00A88A)
                FacetState.FOGGED -> Color(0xff6E6A92)
                FacetState.BLOCKED -> Color(0xff3A3558)
                FacetState.REMOVED -> Color(0xff8B5CF6)
                null -> Color(0xff3A3558)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.18f))
                    .testTag("deep-scan-facet-${module.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            }
        }
        Box(
            Modifier
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xff1A1642))
                .clickable { onEdit() }
                .padding(horizontal = 10.dp)
                .testTag("deep-scan-modules"),
            contentAlignment = Alignment.Center,
        ) {
            Label("${selection.size}", 11, Cyan, FontWeight.Bold)
        }
    }
}

@Composable
private fun OutcomeCard(
    outcome: DeepScanOutcome,
    onReview: () -> Unit,
    reviewEnabled: Boolean,
) {
    val unread = outcome.unmeasuredModules.size
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xff0D0935))
            .padding(14.dp)
            .testTag("deep-scan-outcome"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Metric(formatBytes(outcome.removableBytes), Copy(R.string.k_removable), Color(0xff00D4AA))
            Metric(outcome.removableCandidates.size.toString(), Copy(R.string.k_files), Cyan)
            Metric(
                if (unread == 0) outcome.measuredModules.size.toString() else "$unread ✕",
                if (unread == 0) Copy(R.string.k_modules) else Copy(R.string.k_unread),
                if (unread == 0) Color(0xff00D4AA) else Gold,
            )
        }
        outcome.note?.let { note ->
            Label(note, 10, Mist, FontWeight.Normal, maxLines = 3)
        }
        if (outcome.status == DeepScanStatus.UNAVAILABLE) {
            Label(
                text = Copy(R.string.k_no_scope_could_be_read_grant_access_or_add_a_folder_then),
                12, Gold,
            )
        }
        Button(
            onClick = onReview,
            enabled = reviewEnabled,
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("deep-scan-review"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xff1A1642),
                contentColor = Color(0xff00D4AA),
                disabledContainerColor = Color(0xff141130),
                disabledContentColor = Muted,
            ),
        ) {
            Label(
                if (reviewEnabled) Copy(R.string.k_review_clean) else Copy(R.string.k_nothing_removable),
                14,
                if (reviewEnabled) Color(0xff00D4AA) else Muted,
                FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Metric(value: String, label: Copy, tint: Color) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xff141130))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Label(value, 16, tint, FontWeight.ExtraBold, align = TextAlign.Center)
        Label(label, 10, Mist, align = TextAlign.Center)
    }
}

@Composable
private fun NoticeCard(text: String, tint: Color, onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .clickable { onDismiss() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(text, 12, tint, FontWeight.Medium, maxLines = 3, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Label("✕", 12, tint, FontWeight.Bold)
    }
}

@Composable
private fun ModuleSheet(
    modules: List<DeepScanModule>,
    selection: Set<String>,
    arabic: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onAll: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onAddFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    val permissions = KnouxData.permissions
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xff0D0935))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("deep-scan-module-sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label(Copy(R.string.k_scan_modules), 18, Chalk, FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onAll(true) }) { Label(Copy(R.string.k_all), 12, Cyan, FontWeight.Bold) }
                    TextButton(onClick = { onAll(false) }) { Label(Copy(R.string.k_none), 12, Mist) }
                }
            }

            modules.forEach { module ->
                val granted = moduleReadiness(module)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xff141130))
                        .padding(12.dp)
                        .testTag("deep-scan-module-${module.id}"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape)
                            .background(if (granted) Color(0xff00D4AA).copy(alpha = 0.2f) else Gold.copy(alpha = 0.15f)),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Label(module.title, 14, Chalk, FontWeight.Bold)
                        Label(
                            if (granted) module.description
                            else Copy.args(R.string.k_needs, module.scopes.joinToString { it.name }),
                            10,
                            if (granted) Mist else Gold,
                            maxLines = 2,
                        )
                    }
                    Switch(
                        checked = module.id in selection,
                        onCheckedChange = { enabled -> onToggle(module.id, enabled) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xff00D4AA),
                            checkedTrackColor = Color(0xff00D4AA).copy(alpha = 0.35f),
                        ),
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRequestPermissions,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                ) { Label(Copy(R.string.k_media_access), 12, Cyan, FontWeight.Bold) }
                OutlinedButton(
                    onClick = onAddFolder,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                ) { Label(Copy(R.string.k_add_folder), 12, Cyan, FontWeight.Bold) }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Label(Copy(R.string.k_close), 14, Mist)
            }
        }
    }
}

/**
 * Real readiness for one module: the same grant observation the engine uses, so a
 * switch can never promise a module the platform will refuse to read.
 */
private fun moduleReadiness(module: DeepScanModule): Boolean {
    val permissions = KnouxData.permissions
    return module.scopes.any { scope ->
        when (scope) {
            AccessScope.APP_CACHE -> true
            AccessScope.SAF_TREE -> permissions.safTrees(requireWrite = false).isNotEmpty()
            AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO ->
                permissions.mediaGrant(scope) == PermissionGrant.GRANTED ||
                    permissions.mediaGrant(scope) == PermissionGrant.PARTIAL_SELECTED
        }
    }
}

@Composable
private fun ReviewDialog(
    outcome: DeepScanOutcome,
    arabic: Boolean,
    onDismiss: () -> Unit,
    onTrash: (List<com.knoux.ai.data.FileCandidate>) -> Unit,
    onDelete: (List<com.knoux.ai.data.FileCandidate>) -> Unit,
) {
    val removable = outcome.removableCandidates
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xff0D0935))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("deep-scan-review-sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Label(Copy(R.string.k_review_findings), 19, Chalk, FontWeight.ExtraBold)
            Label(
                Copy.args(R.string.k_eligible_summary, removable.size, formatBytes(removable.sumOf { it.bytes })),
                12,
                Cyan,
                FontWeight.Bold,
            )

            outcome.moduleResults.filter { it.candidates.isNotEmpty() }.forEach { result ->
                val module = KnouxData.deepScan.modules.firstOrNull { it.id == result.moduleId }
                val eligible = result.candidates.count { it.eligibility == Eligibility.ELIGIBLE }
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xff141130))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Label(module?.title ?: Copy(result.moduleId, result.moduleId), 13, Chalk, FontWeight.Bold)
                        Label(
                            Copy.args(R.string.k_scan_found_summary, result.candidates.size, eligible),
                            10, Mist,
                        )
                    }
                    Label(formatBytes(result.totalBytes), 12, Cyan, FontWeight.Bold)
                    if (result.status == DataStatus.UNAVAILABLE) {
                        Label(" ✕", 12, Gold, FontWeight.Bold)
                    }
                }
                if (result.detail != null) {
                    Label(result.detail!!, 10, Mist, maxLines = 2)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onTrash(removable) },
                    modifier = Modifier.weight(1f).height(46.dp).testTag("deep-scan-trash"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xff00D4AA),
                        contentColor = Color(0xff07051A),
                    ),
                ) { Label(Copy(R.string.k_trash), 14, Color(0xff07051A), FontWeight.ExtraBold) }
                OutlinedButton(
                    onClick = { onDelete(removable) },
                    modifier = Modifier.weight(1f).height(46.dp).testTag("deep-scan-delete"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Pink),
                ) { Label(Copy(R.string.k_delete), 14, Pink, FontWeight.Bold) }
            }
            Text(
                text = if (arabic)
                    "لا يزال الحذف النهائي يتطلب موافقة أندرويد. الحذف المتحقق يعني إعادة قراءة النظام للتأكد."
                else
                    "Permanent deletion still requires Android approval. A verified removal means the platform was re-read.",
                color = Mist,
                fontSize = 10.sp,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Label(Copy(R.string.k_close), 14, Mist)
            }
        }
    }
}

private fun describe(result: OperationResult, arabic: Boolean): String {
    val head = when (result.status) {
        OperationStatus.SUCCEEDED -> if (arabic) "تم التحقق" else "Verified removed"
        OperationStatus.PARTIAL -> if (arabic) "تم جزئياً" else "Partly removed"
        OperationStatus.REJECTED_BY_OS -> if (arabic) "رفض النظام" else "Android refused"
        OperationStatus.FAILED -> if (arabic) "فشل" else "Failed"
        OperationStatus.CANCELLED -> if (arabic) "أُلغي" else "Cancelled"
        else -> if (arabic) "حالة غير نهائية" else "Not final"
    }
    return "$head · ${result.removedCount}/${result.removedCount + result.failedCount} " +
        if (arabic) "ملف" else "file(s)"
}

private val Muted = Color(0xff7D7A99)
