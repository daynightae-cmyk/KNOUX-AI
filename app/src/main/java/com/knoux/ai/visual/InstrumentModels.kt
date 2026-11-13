package com.knoux.ai.visual

import com.knoux.ai.Copy
import com.knoux.ai.Mark
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.DataStatus
import com.knoux.ai.data.FileCandidate

/**
 * PC05 visual contract types.
 *
 * These describe what the Abyss Sonar is allowed to draw. Every field is fed from
 * a measured [com.knoux.ai.data.FileCandidate] list or a real module status; there
 * is no field for "estimated progress" because the instrument must never invent
 * one. See `docs/design/KNOUX_SERVICE_VISUAL_IDENTITY_20261113.md`.
 */

/** Instrument lifecycle phase. Maps 1:1 onto what the customer can do next. */
internal enum class InstrumentPhase {
    /** Nothing scanned yet in this pass. */
    IDLE,

    /** Modules selected and permissions being observed; no bytes counted yet. */
    ARMED,

    /** A real scan is running. Depth follows completed modules, not the clock. */
    RUNNING,

    /** Stopped by the customer; nothing was removed. */
    PAUSED,

    /** Every selected module was measured. */
    RESULT,

    /** Some module could not be fully measured; the rest is real. */
    PARTIAL,

    /** No module could be measured, or the engine failed. */
    FAILED,

    /** A destructive action is being confirmed or has just completed. */
    UNDO,
}

/** How one module sector is drawn. */
internal enum class FacetState {
    /** Fully measured, real data available. */
    MEASURED,

    /** Only part of the scope could be read (partial grant / provider refused). */
    THIN,

    /** Scope not granted or provider refused — drawn as fog, never as zero bytes. */
    FOGGED,

    /** Explicitly not selected by the customer. */
    BLOCKED,

    /** Was measured and the customer removed it; kept visible as evidence. */
    REMOVED,
}

/**
 * Split of a module's item count into what the customer may act on.
 *
 * A module can find many items and still have nothing removable (a read-only SAF
 * grant). Drawing that as "ready to clean" would be a false promise, so the split
 * travels with the facet.
 */
internal data class FacetBreakdown(
    val removable: Int,
    val protectedCount: Int,
    val unselectable: Int,
)

/** One module drawn as one sonar sector and one core facet. */
internal data class Facet(
    /** Stable domain id, used by the detail sheet. */
    val id: String,
    /** Measured bytes. Zero only when the module really measured zero bytes. */
    val weight: Long,
    val state: FacetState,
    val label: Copy,
    val breakdown: FacetBreakdown = FacetBreakdown(0, 0, 0),
    /** Provenance actually observed for this module's findings. */
    val provenance: String? = null,
    /** One short reason when the module is fogged/thin. */
    val note: String? = null,
)

/** Complete frame for one instrument render, derived from a real StateFlow. */
internal data class InstrumentFrame(
    val phase: InstrumentPhase,
    /**
     * Completed modules / selected modules, or null when nothing has completed.
     * Null means the sonar sweeps to listen; it never means "0 % done".
     */
    val progress: Float?,
    /** Measured bytes for the pass so far. Null before any module reports. */
    val heroValue: Long?,
    /** One facet per module, in sector order (max 8, per contract). */
    val facets: List<Facet>,
    /** System reduced-motion preference for this screen. */
    val motion: Boolean,
    /** Short caption, at most four words (contract text budget). */
    val caption: Copy,
    /** When the scanned evidence was captured, for the idle "last scan" line. */
    val lastResultAgeMillis: Long? = null,
)

/** One deep scan domain. Each module is exactly one sonar sector. */
internal data class DeepScanModule(
    val id: String,
    val order: Int,
    /** Full name for the module sheet. */
    val title: Copy,
    /** Two-word name for the sector caption. */
    val shortTitle: Copy,
    val description: Copy,
    val mark: Mark,
    /** Scopes this module must read. An unread scope makes the module fogged. */
    val scopes: Set<AccessScope>,
    /** On by default only when it is cheap and permission-free. */
    val defaultEnabled: Boolean = true,
)

/** Result of one module's scan, with the files that produced it. */
internal data class ModuleResult(
    val moduleId: String,
    val order: Int,
    val itemCount: Int,
    val totalBytes: Long,
    val candidates: List<FileCandidate>,
    val status: DataStatus,
    val detail: String? = null,
) {
    val removable: List<FileCandidate>
        get() = candidates.filter { it.eligibility == com.knoux.ai.data.Eligibility.ELIGIBLE }
}

/** Overall verdict of one deep scan pass. Derived from module outcomes only. */
internal enum class DeepScanStatus {
    /** Every selected module was measured. */
    COMPLETE,

    /** At least one module was measured and at least one was not. */
    PARTIAL,

    /** No selected module could be measured. Carries no byte totals. */
    UNAVAILABLE,

    /** The customer cancelled; what completed before that is still real. */
    CANCELLED,

    /** The engine threw. Whatever completed before the failure is kept. */
    FAILED,
}

/** Aggregated, measured deep scan outcome. */
internal data class DeepScanOutcome(
    val sessionId: String,
    val startedAtMillis: Long,
    val finishedAtMillis: Long?,
    val moduleResults: List<ModuleResult>,
    val scopesScanned: Set<AccessScope>,
    val status: DeepScanStatus,
    val note: String? = null,
) {
    /** Modules whose measurement is trustworthy. */
    val measuredModules: List<ModuleResult>
        get() = moduleResults.filter {
            it.status == DataStatus.MEASURED || it.status == DataStatus.VERIFIED ||
                it.status == DataStatus.PARTIAL
        }

    /** Modules that returned no trustworthy measurement. */
    val unmeasuredModules: List<ModuleResult>
        get() = moduleResults.filter {
            it.status == DataStatus.UNAVAILABLE || it.status == DataStatus.PREVIEW
        }

    /**
     * Distinct files across modules. One file can legitimately be found by more
     * than one module (a large old video), so totals are de-duplicated by URI.
     */
    val distinctCandidates: List<FileCandidate>
        get() = moduleResults.flatMap { it.candidates }.distinctBy { it.uri }

    val totalItems: Int get() = distinctCandidates.size

    val totalBytes: Long get() = distinctCandidates.sumOf { it.bytes }

    /** Files the customer may actually select right now. */
    val removableCandidates: List<FileCandidate>
        get() = distinctCandidates.filter { it.eligibility == com.knoux.ai.data.Eligibility.ELIGIBLE }

    val removableBytes: Long get() = removableCandidates.sumOf { it.bytes }

    val finishedAtOrStartedMillis: Long get() = finishedAtMillis ?: startedAtMillis
}

/** Progress emitted while a pass runs. Every field is a real counter. */
internal data class DeepScanProgress(
    val currentModuleId: String?,
    val modulesCompleted: Int,
    val modulesTotal: Int,
    val itemsFound: Int,
    val bytesFound: Long,
    val label: String,
    /** Measured bytes per finished module, so the instrument can draw real depth. */
    val completedBytes: Map<String, Long> = emptyMap(),
    /** Measured item count per finished module. */
    val completedItems: Map<String, Int> = emptyMap(),
    /** Real status per finished module. */
    val completedStatus: Map<String, DataStatus> = emptyMap(),
)

/** One row of deep scan history, for the idle state. */
internal data class DeepScanHistory(
    val sessionId: String,
    val capturedAtMillis: Long,
    val status: DeepScanStatus,
    val moduleCount: Int,
    val moduleMeasured: Int,
    val moduleUnavailable: Int,
    val itemCount: Int,
    val totalBytes: Long,
    val note: String?,
)
