package com.knoux.ai

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.KnouxData
import com.knoux.ai.visual.SynapseInstrument
import com.knoux.ai.visual.SynapseNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PC10 — AI Recommendations, wired to real measured passes.
 *
 * This replaces the old empty "no connected engine" placeholder with an honest
 * on-device rule engine: it reads the latest Smart Clean, Duplicate, Residue and
 * Privacy passes from Room plus the live storage partition, and lights one
 * recommendation per real finding. There is no cloud model, no confidence score,
 * no invented "gain" — every node traces to bytes or checks the customer can open
 * and verify. Accepting a recommendation navigates to the service that does the
 * actual work; nothing is cleaned from this screen.
 */
private data class Recommendation(
    val id: String,
    val title: Copy,
    val evidence: Copy,
    val target: Page,
    val tone: Color,
    val mark: Mark,
)

@Composable
internal fun AiRecommendationsScreen(open: (Page) -> Unit) {
    val context = LocalContext.current
    var recs by remember { mutableStateOf<List<Recommendation>?>(null) }
    var accepted by remember { mutableStateOf(setOf<String>()) }
    /** The recommendation accepted but not yet navigated to — lets the synapse pulse finish first. */
    var pendingAccept by remember { mutableStateOf<Pair<String, Page>?>(null) }

    LaunchedEffect(Unit) {
        recs = withContext(Dispatchers.IO) { buildRecommendations() }
    }

    // Finish the accepted pulse before leaving the screen, so the finite flash is observable.
    LaunchedEffect(pendingAccept) {
        val target = pendingAccept
        if (target != null) {
            kotlinx.coroutines.delay(700)
            if (pendingAccept == target) {
                pendingAccept = null
                open(target.second)
            }
        }
    }

    val list = recs
    val nodes = remember(list, accepted) {
        list.orEmpty().map { SynapseNode(it.id, lit = true, accepted = it.id in accepted, tone = it.tone) }
    }

    Column(Modifier.fillMaxSize()) {
        MasterTitle(
            Copy(R.string.k_ai_recommendations), Copy(R.string.synapse_on_device),
            Copy(R.string.synapse_subtitle), "screen-ai-recommendations",
        )
        ServiceStatePill(Copy(R.string.synapse_on_device))

        Column(
            Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NativeSpace.edge, vertical = NativeSpace.row),
            verticalArrangement = Arrangement.spacedBy(NativeSpace.row),
        ) {
            SurfaceCard(Modifier.enterRise(0), accent = NativePalette.magenta) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SynapseInstrument(nodes)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Glyph(Mark.CHIP, Modifier.size(13.dp), NativePalette.magenta)
                        Label(Copy(R.string.synapse_on_device), 11, NativePalette.muted, FontWeight.SemiBold)
                    }
                }
            }

            when {
                list == null -> {
                    Label(Copy(R.string.k_measuring_device), 13, NativePalette.muted, modifier = Modifier.padding(8.dp))
                }
                list.isEmpty() -> {
                    SurfaceCard(Modifier.enterRise(1), accent = NativePalette.violet) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(NativeSpace.row),
                        ) {
                            ServiceIconPlate(Mark.CHIP, NativePalette.violet, size = 52.dp, iconSize = 26.dp)
                            Label(Copy(R.string.synapse_empty_title), 15, NativePalette.text, FontWeight.SemiBold, align = androidx.compose.ui.text.style.TextAlign.Center)
                            Label(Copy(R.string.synapse_empty_body), 12, NativePalette.muted, align = androidx.compose.ui.text.style.TextAlign.Center)
                            AccentButton(
                                Action(Copy(R.string.k_start_deep_scan), Mark.RADAR),
                                Modifier.fillMaxWidth(), primary = true, tag = "synapse-start-scan",
                            ) { open(Page.SCAN) }
                        }
                    }
                }
                else -> {
                    list.forEachIndexed { index, rec ->
                        RecommendationCard(
                            rec = rec,
                            accepted = rec.id in accepted,
                            modifier = Modifier.enterRise(index + 1),
                            onAccept = {
                                accepted = accepted + rec.id
                                pendingAccept = rec.id to rec.target
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        PinnedActions(
            first = Action(Copy(R.string.battery_back_tools), Mark.GRID),
            second = Action(Copy(R.string.k_start_deep_scan), Mark.RADAR),
            onFirst = { open(Page.TOOLS) },
            onSecond = { open(Page.SCAN) },
        )
    }
}

@Composable
private fun RecommendationCard(
    rec: Recommendation,
    accepted: Boolean,
    modifier: Modifier = Modifier,
    onAccept: () -> Unit,
) {
    SurfaceCard(modifier.testTag("rec-${rec.id}"), accent = rec.tone) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServiceIconPlate(rec.mark, rec.tone, size = 40.dp, iconSize = 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Label(rec.title, 14, NativePalette.text, FontWeight.SemiBold, maxLines = 2)
                Label(rec.evidence, 11, rec.tone, maxLines = 1)
            }
            AccentButton(
                Action(Copy(R.string.synapse_accept), if (accepted) Mark.CHECK else Mark.NEXT),
                primary = false, tag = "rec-accept-${rec.id}", labelSize = 12,
                onClick = onAccept,
            )
        }
    }
}

/**
 * Reads the latest real passes and the live partition, and emits one recommendation
 * per finding. Each source is guarded so a single unreadable table cannot blank the
 * whole screen; an absent pass simply contributes no node.
 */
private suspend fun buildRecommendations(): List<Recommendation> {
    val out = mutableListOf<Recommendation>()

    runCatching { KnouxData.smartClean.latestScan() }.getOrNull()?.let { scan ->
        val cache = scan.candidates.filter { it.provenance == DataProvenance.APP_OWNED_CACHE }
        val bytes = cache.sumOf { it.bytes }
        if (bytes > 0L) {
            out += Recommendation(
                "cache", Copy(R.string.rec_cache_title),
                Copy.args(R.string.synapse_evidence, KnouxFormat.bytes(bytes), cache.size),
                Page.CLEAN, NativePalette.cyan, Mark.BROOM,
            )
        }
    }

    runCatching { KnouxData.duplicates.latest() }.getOrNull()?.let { dup ->
        if (dup.redundantBytes > 0L) {
            out += Recommendation(
                "duplicates", Copy(R.string.rec_duplicates_title),
                Copy.args(R.string.synapse_evidence, KnouxFormat.bytes(dup.redundantBytes), dup.duplicateCount),
                Page.DUPLICATES, NativePalette.magenta, Mark.PICTURES,
            )
        }
    }

    runCatching { KnouxData.residue.latest() }.getOrNull()?.let { res ->
        if (res.bytes > 0L) {
            out += Recommendation(
                "residue", Copy(R.string.rec_residue_title),
                Copy.args(R.string.synapse_evidence, KnouxFormat.bytes(res.bytes), res.items.size),
                Page.RESIDUE, NativePalette.violet, Mark.LAYERS,
            )
        }
    }

    runCatching { KnouxData.privacy.latest() }.getOrNull()?.let { report ->
        val attention = report.attention.size
        if (attention > 0) {
            out += Recommendation(
                "privacy", Copy.args(R.string.rec_privacy_title, attention),
                Copy.args(R.string.vault_nodes, report.rows.size, attention),
                Page.PRIVACY_INSPECTOR, NativePalette.amber, Mark.SHIELD,
            )
        }
    }

    runCatching {
        val stats = StatFs(Environment.getDataDirectory().path)
        val total = stats.totalBytes
        if (total > 0L) {
            val used = total - stats.availableBytes
            val percent = ((used * 100) / total).toInt()
            if (percent >= 85) {
                out += Recommendation(
                    "storage", Copy.args(R.string.rec_storage_title, percent),
                    Copy.args(R.string.terrain_contours, KnouxFormat.bytes(used), KnouxFormat.bytes(stats.availableBytes)),
                    Page.STORAGE_BREAKDOWN, NativePalette.blue, Mark.DISK,
                )
            }
        }
    }

    return out
}
