package com.knoux.ai.data

/**
 * PC00 — typed local domain contracts.
 *
 * These types exist so the UI can never display a number it cannot trace back to
 * a real observation. Every byte count carries the [DataStatus] that produced it,
 * and every candidate carries the [DataProvenance] it came from. A value that was
 * never measured is representable, but only as [DataStatus.PREVIEW] or
 * [DataStatus.UNAVAILABLE] — never as a silent zero.
 */

/**
 * How much confidence the platform allows in a value.
 *
 * [MEASURED]  read directly from the OS for this item, right now.
 * [ESTIMATED] derived from a real measurement but not exact (e.g. volume deltas).
 * [PREVIEW]   a design fixture, explicitly not real data.
 * [UNAVAILABLE] the platform refused or cannot answer; carries no number.
 * [PARTIAL]   only part of the scope could be read (revoked/selected-only grant).
 * [VERIFIED]  re-queried after an operation and confirmed changed.
 */
enum class DataStatus { MEASURED, ESTIMATED, PREVIEW, UNAVAILABLE, PARTIAL, VERIFIED }

/** Where a candidate physically comes from, and therefore what may be done to it. */
enum class DataProvenance {
    /** Our own `cacheDir` / `codeCacheDir` / `externalCacheDir`. Freely deletable. */
    APP_OWNED_CACHE,

    /** A MediaStore row shared across apps. Mutated only via an OS-mediated request. */
    MEDIASTORE,

    /** A document inside a tree the customer granted through the Storage Access Framework. */
    SAF_DOCUMENT,
}

/** A storage scope the customer can grant or refuse independently. */
enum class AccessScope {
    /** Always available: the app's own cache. No permission needed. */
    APP_CACHE,

    /** API 33+ READ_MEDIA_IMAGES, API ≤32 READ_EXTERNAL_STORAGE. */
    MEDIA_IMAGES,

    /** API 33+ READ_MEDIA_VIDEO, API ≤32 READ_EXTERNAL_STORAGE. */
    MEDIA_VIDEO,

    /** A customer-chosen folder via ACTION_OPEN_DOCUMENT_TREE. */
    SAF_TREE,
}

/**
 * Result of asking the OS whether a scope is usable *right now*.
 *
 * Never cached across permission changes: a grant can be revoked between sessions,
 * so [PermissionState] is re-observed on every scan.
 */
enum class PermissionGrant {
    GRANTED,
    /** API 34+ partial media access: some items are visible, most are not. */
    PARTIAL_SELECTED,
    DENIED,
    /** Was granted, then revoked; a scan started earlier must not assume access. */
    REVOKED,
    /** Scope needs no runtime permission (app-owned cache). */
    NOT_REQUIRED,
}

/** Why a candidate exists, so the customer can judge it before acting. */
enum class CandidateCategory { APP_CACHE, THUMBNAIL, TEMP_FILE, IMAGE, VIDEO, SAF_FILE }

/** Whether the engine is allowed to include an item in an operation plan. */
enum class Eligibility {
    ELIGIBLE,
    /** Must never be offered for removal (e.g. outside the granted tree). */
    PROTECTED,

    /** Visible for review but the provider cannot delete it. */
    UNSELECTABLE,
}

/** Removal and restoration are distinct promises, never interchangeable. */
enum class OperationKind {
    /** Recoverable where the provider supports a trash. */
    MOVE_TO_TRASH,

    /** Irreversible. Requires an explicit irreversible warning plus OS approval. */
    PERMANENT_DELETE,

    /** OS-mediated recovery of items verified to still be in the provider's trash. */
    RESTORE_FROM_TRASH,
}

enum class OperationStatus { PLANNED, AWAITING_USER_CONFIRMATION, RUNNING, SUCCEEDED, PARTIAL, FAILED, CANCELLED, REJECTED_BY_OS }

enum class ScanStatus { RUNNING, COMPLETED, CANCELLED, FAILED, PARTIAL }

/** A real, measured snapshot of one scope. */
data class SourceSnapshot(
    val scope: AccessScope,
    val provenance: DataProvenance,
    val grant: PermissionGrant,
    val itemCount: Int,
    val totalBytes: Long,
    val capturedAtMillis: Long,
    val status: DataStatus,
    val detail: String? = null,
)

/** A real file the customer may choose. `uri` is the only stable handle. */
data class FileCandidate(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val bytes: Long,
    val lastModifiedMillis: Long,
    val category: CandidateCategory,
    val provenance: DataProvenance,
    val eligibility: Eligibility,
    /** Human-readable justification shown in the review sheet. */
    val reason: String,
    val status: DataStatus = DataStatus.MEASURED,
    /**
     * Best-effort identity of the physical file behind this URI.
     *
     * The same file can be visible through MediaStore and through a granted SAF
     * folder at the same time. Those are two URIs for one file, not two copies;
     * removing the "copy" would remove the file the group calls its original.
     * Equal non-null keys are therefore collapsed before duplicate grouping.
     */
    val physicalKey: String? = null,
)

/** Volume deltas are observations, not a guarantee of freed space. */
data class ObservedBytes(
    val volumeFreeBefore: Long,
    val volumeFreeAfter: Long,
    val status: DataStatus,
) {
    val deltaBytes: Long get() = volumeFreeAfter - volumeFreeBefore
}

data class ScanSession(
    val id: String,
    val startedAtMillis: Long,
    val finishedAtMillis: Long?,
    val status: ScanStatus,
    val scopes: Set<AccessScope>,
    val itemCount: Int,
    val totalBytes: Long,
    val snapshots: List<SourceSnapshot>,
)

data class OperationPlan(
    val id: String,
    val kind: OperationKind,
    val candidates: List<FileCandidate>,
    val requiresSystemConfirmation: Boolean,
    /** False until the customer has actually reviewed and accepted the plan. */
    val confirmedByUser: Boolean = false,
) {
    val totalBytes: Long get() = candidates.sumOf { it.bytes }
}

data class OperationResult(
    val planId: String,
    val kind: OperationKind,
    val status: OperationStatus,
    val removedCount: Int,
    val failedCount: Int,
    val observed: ObservedBytes?,
    val errors: List<String>,
    val completedAtMillis: Long,
)

data class CleanupHistory(
    val id: String,
    val kind: OperationKind,
    val status: OperationStatus,
    val itemCount: Int,
    val bytes: Long,
    val completedAtMillis: Long,
    val scopeLabels: List<String>,
    val note: String? = null,
)

/** A Pro feature and whether it can actually do something in this build. */
enum class FeatureCapability {
    /** Fully wired against real platform APIs. */
    AVAILABLE,

    /** Real, but needs a backend/keys the owner has not supplied. */
    CONFIGURATION_REQUIRED,

    /** Genuinely impossible on the supported API range; never sold as working. */
    UNSUPPORTED,
}

/** Proof of the entitlement, never a locally minted boolean. */
enum class ProEntitlementSource { VERIFIED_ENTITLEMENT, DEBUG_TEST_ONLY, NONE }

/**
 * A decision about one Pro feature.
 *
 * [featureId] is the entitlement key taken from `ProFeature.id` in ProSystem.kt,
 * not a copy of that UI descriptor. The domain layer must not depend on a Compose
 * type: doing so would let a UI edit silently change what the customer bought.
 */
data class ProAccessDecision(
    val featureId: String,
    val source: ProEntitlementSource,
    val allowed: Boolean,
    /** Non-null only when [allowed] is false, so the UI can explain the block. */
    val blockedReason: String? = null,
)

/**
 * Pure helpers shared by the UI and the tests.
 *
 * They live here rather than in a composable so they can be proven by a JVM unit
 * test without an emulator.
 */
object CleanupRules {

    /** A candidate may only enter an operation plan when it is explicitly eligible. */
    fun isSelectable(candidate: FileCandidate): Boolean =
        candidate.eligibility == Eligibility.ELIGIBLE && candidate.bytes > 0

    /** Totals must never be claimed for a scope that was not actually read. */
    fun countsAsMeasured(status: DataStatus): Boolean =
        status == DataStatus.MEASURED || status == DataStatus.VERIFIED

    /**
     * Trash and permanent delete are different promises, so the confirmation a
     * plan needs differs: permanent delete always needs the OS dialog *and* an
     * explicit irreversible acknowledgement from the customer.
     */
    fun requiresIrreversibleWarning(kind: OperationKind): Boolean =
        kind == OperationKind.PERMANENT_DELETE

    fun requiresSystemDialog(kind: OperationKind, provenance: DataProvenance): Boolean =
        provenance == DataProvenance.MEDIASTORE ||
            (provenance == DataProvenance.SAF_DOCUMENT && kind == OperationKind.PERMANENT_DELETE)

    /** A plan is only actionable once the customer confirmed it and it has items. */
    fun canExecute(plan: OperationPlan, acknowledgedIrreversible: Boolean): Boolean {
        if (!plan.confirmedByUser) return false
        if (plan.candidates.isEmpty()) return false
        if (plan.candidates.any { !isSelectable(it) }) return false
        if (requiresIrreversibleWarning(plan.kind) && !acknowledgedIrreversible) return false
        // Restore from trash is only meaningful for MediaStore items; OS trash/restore
        // is not supported for app-cache or SAF documents.
        if (plan.kind == OperationKind.RESTORE_FROM_TRASH &&
            plan.candidates.any { it.provenance != DataProvenance.MEDIASTORE }) return false
        return true
    }
}
