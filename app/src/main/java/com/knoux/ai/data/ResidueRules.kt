package com.knoux.ai.data

/**
 * PC03 — App Residue rules.
 *
 * A "residue" is a file whose *name* proves it is a leftover of an interrupted or
 * disposable write. Nothing here inspects content, guesses ownership, or claims to
 * know which app wrote a file in a shared folder.
 *
 * Two hard guarantees are encoded in pure functions so a JVM test can prove them:
 *
 *  1. A file found outside KNOUX's own cache is never pre-selected. The evidence is
 *     a filename pattern, which is a hint — not proof that the file is abandoned.
 *  2. Only files inside KNOUX's own cache are `OWNED`: Android's own rules already
 *     let the system delete them at any time, so removing them takes nothing the
 *     customer is keeping.
 */

/** The filename evidence that made a file look like residue. */
enum class ResiduePattern {
    /** Inside KNOUX's own cache and not matching a sharper pattern. */
    CACHE_FILE,

    /** `.tmp` / `.temp` — an unfinished write. */
    TEMP_FILE,

    /** `.part` / `.partial` / `.crdownload` — an interrupted download. */
    PARTIAL_DOWNLOAD,

    /** `.trashed-` prefix produced by Android/MediaStore trash operations. */
    INTERRUPTED_TRASH,

    /** `.pending-` prefix produced while a delete is being staged. */
    PENDING_DELETE,

    /** `name~` or `.bak` — an editor backup copy. */
    EDITOR_BACKUP,

    /** `.log` / `.logcat` / `.dmp` — diagnostic output. */
    LOG_FILE,

    /** Thumbnail cache names. */
    THUMBNAIL,
}

/**
 * How much the evidence actually proves.
 *
 * [OWNED]  the file is inside our own cache; deletion is ours to make.
 * [STRONG] the OS or another writer named it as an in-progress/trashed artifact.
 * [WEAK]   a generic leftover name that any app could also use for real data.
 */
enum class ResidueConfidence { OWNED, STRONG, WEAK }

/** One reviewed residue candidate. */
data class ResidueItem(
    val candidate: FileCandidate,
    val pattern: ResiduePattern,
    val confidence: ResidueConfidence,
    /** The exact filename evidence shown to the customer. */
    val matchedOn: String,
)

/** Result of matching a filename against the residue taxonomy. */
data class ResidueMatch(
    val pattern: ResiduePattern,
    val confidence: ResidueConfidence,
    val matchedOn: String,
)

/**
 * A dry run: what the current selection would actually do.
 *
 * It is computed from the measured items and never from a stored total, so the
 * number the customer confirms is the number the plan contains.
 */
data class ResidueDryRun(
    val items: List<ResidueItem>,
    val bytes: Long,
    val ownedCount: Int,
    val sharedCount: Int,
    val weakCount: Int,
    val needsAndroidDialog: Boolean,
    val warnings: List<String>,
) {
    val canExecute: Boolean get() = items.isNotEmpty()
    val patternSummary: String
        get() = items.groupingBy { it.pattern }.eachCount()
            .entries.sortedByDescending { it.value }
            .joinToString(" · ") { (pattern, count) -> "${pattern.name.lowercase()} ×$count" }
}

object ResidueRules {

    const val DAY_MILLIS: Long = 24L * 60L * 60L * 1000L

    /** Shared-folder residue is only offered once it is genuinely old. */
    const val DEFAULT_MIN_AGE_DAYS: Int = 1

    val AGE_CHOICES: List<Int> = listOf(0, 1, 7, 30)

    private val TEMP_EXTENSIONS = setOf("tmp", "temp", "swp", "swo")
    private val PARTIAL_EXTENSIONS = setOf("part", "partial", "crdownload", "download", "opdownload")
    private val LOG_EXTENSIONS = setOf("log", "logcat", "dmp")
    private val BACKUP_EXTENSIONS = setOf("bak", "old", "orig")

    /**
     * Classifies a filename. Order matters: the OS-produced prefixes are checked
     * before a generic extension so `.trashed-…tmp` reports the stronger reason.
     */
    fun classify(name: String, mimeType: String? = null): ResidueMatch? {
        val lower = name.trim().lowercase()
        if (lower.isEmpty()) return null
        if (lower.startsWith(".trashed-")) {
            return ResidueMatch(ResiduePattern.INTERRUPTED_TRASH, ResidueConfidence.STRONG, "name starts with .trashed-")
        }
        if (lower.startsWith(".pending-")) {
            return ResidueMatch(ResiduePattern.PENDING_DELETE, ResidueConfidence.STRONG, "name starts with .pending-")
        }
        val extension = lower.substringAfterLast('.', "")
        if (extension in PARTIAL_EXTENSIONS) {
            return ResidueMatch(ResiduePattern.PARTIAL_DOWNLOAD, ResidueConfidence.WEAK, "extension .$extension")
        }
        if (extension in TEMP_EXTENSIONS) {
            return ResidueMatch(ResiduePattern.TEMP_FILE, ResidueConfidence.WEAK, "extension .$extension")
        }
        if (lower.endsWith("~")) {
            return ResidueMatch(ResiduePattern.EDITOR_BACKUP, ResidueConfidence.WEAK, "name ends with ~")
        }
        if (extension in BACKUP_EXTENSIONS) {
            return ResidueMatch(ResiduePattern.EDITOR_BACKUP, ResidueConfidence.WEAK, "extension .$extension")
        }
        if (extension in LOG_EXTENSIONS) {
            return ResidueMatch(ResiduePattern.LOG_FILE, ResidueConfidence.WEAK, "extension .$extension")
        }
        if (mimeType?.startsWith("image/") == true &&
            (lower.startsWith("thumb") || lower.contains(".thumb") || lower.startsWith("image_cache"))
        ) {
            return ResidueMatch(ResiduePattern.THUMBNAIL, ResidueConfidence.WEAK, "thumbnail naming")
        }
        return null
    }

    /**
     * Turns a measured file into a residue item, or returns null when it is not one.
     *
     * Own-cache files are residue by definition and ignore [minAgeDays]: the folder
     * exists for disposable data. Shared files must match a filename pattern *and*
     * be at least [minAgeDays] old; an unknown modification time disqualifies them
     * because "we could not tell" must never become "old enough".
     */
    fun detect(candidate: FileCandidate, minAgeDays: Int = DEFAULT_MIN_AGE_DAYS, nowMillis: Long = System.currentTimeMillis()): ResidueItem? {
        val own = candidate.provenance == DataProvenance.APP_OWNED_CACHE
        val match = classify(candidate.displayName, candidate.mimeType)
        if (!own) {
            val shared = match ?: return null
            if (candidate.lastModifiedMillis <= 0L) return null
            if (candidate.bytes <= 0L) return null
            val age = (nowMillis - candidate.lastModifiedMillis) / DAY_MILLIS
            if (age < minAgeDays) return null
            return ResidueItem(
                candidate = candidate.copy(
                    eligibility = if (candidate.eligibility == Eligibility.PROTECTED) Eligibility.UNSELECTABLE
                    else candidate.eligibility,
                    reason = "Filename evidence only (${shared.matchedOn}); KNOUX did not verify the writer",
                ),
                pattern = shared.pattern,
                confidence = shared.confidence,
                matchedOn = shared.matchedOn,
            )
        }
        val effective = match ?: ResidueMatch(
            ResiduePattern.CACHE_FILE,
            ResidueConfidence.OWNED,
            "inside KNOUX's own cache",
        )
        val eligibility = when {
            candidate.bytes <= 0L -> Eligibility.UNSELECTABLE
            candidate.eligibility == Eligibility.PROTECTED -> Eligibility.UNSELECTABLE
            else -> candidate.eligibility
        }
        return ResidueItem(
            candidate = candidate.copy(
                eligibility = eligibility,
                reason = if (eligibility == Eligibility.ELIGIBLE) {
                    "Inside KNOUX's own cache — Android may delete it at any time"
                } else {
                    "Empty file — nothing to reclaim, so it is not offered"
                },
            ),
            pattern = effective.pattern,
            confidence = ResidueConfidence.OWNED,
            matchedOn = effective.matchedOn,
        )
    }

    /**
     * Collapses the same physical file seen through more than one source.
     *
     * MediaStore wins over SAF (its removal is OS-mediated), SAF over cache. A null
     * key is kept as a distinct file: two unknown files must not be merged.
     */
    fun distinctPhysicalItems(items: List<ResidueItem>): List<ResidueItem> {
        val chosen = LinkedHashMap<String, ResidueItem>()
        items.forEach { item ->
            val key = item.candidate.physicalKey
            if (key == null) {
                chosen["uri:${item.candidate.uri}"] = item
                return@forEach
            }
            val existing = chosen[key]
            if (existing == null || provenanceRank(item.candidate) < provenanceRank(existing.candidate)) {
                chosen[key] = item
            }
        }
        return chosen.values.toList()
    }

    private fun provenanceRank(candidate: FileCandidate): Int = when (candidate.provenance) {
        DataProvenance.MEDIASTORE -> 0
        DataProvenance.SAF_DOCUMENT -> 1
        DataProvenance.APP_OWNED_CACHE -> 2
    }

    /**
     * Auto-safe means: ours, and the provider agrees it can be removed.
     *
     * This is the only predicate that may drive a bulk selection. A shared item is
     * still selectable — but only through an explicit tap on that one row.
     */
    fun isAutoSafe(item: ResidueItem): Boolean =
        item.confidence == ResidueConfidence.OWNED && CleanupRules.isSelectable(item.candidate)

    fun canSelect(item: ResidueItem): Boolean = CleanupRules.isSelectable(item.candidate)

    /**
     * The only bulk selection the UI is allowed to offer.
     *
     * It contains own-cache items exclusively. A shared-folder item can be selected
     * one by one, after the customer reads its evidence; no button pre-selects it.
     */
    fun safeSelection(items: List<ResidueItem>): Set<String> =
        items.filter { isAutoSafe(it) }.map { it.candidate.uri }.toSet()

    /** Toggling a shared item is always an explicit, single-item act. */
    fun toggle(items: List<ResidueItem>, selected: Set<String>, uri: String): Set<String> {
        val item = items.firstOrNull { it.candidate.uri == uri } ?: return selected
        if (!canSelect(item)) return selected
        return if (uri in selected) selected - uri else selected + uri
    }

    fun selectedItems(items: List<ResidueItem>, selected: Set<String>): List<ResidueItem> =
        items.filter { it.candidate.uri in selected && CleanupRules.isSelectable(it.candidate) }
            .distinctBy { it.candidate.uri }

    fun selectedBytes(items: List<ResidueItem>, selected: Set<String>): Long =
        selectedItems(items, selected).sumOf { it.candidate.bytes }

    /**
     * Trash is only offered when every selected item is shared media: the OS has no
     * trash for cache or SAF documents, so pretending otherwise would be a lie.
     */
    fun canTrash(items: List<ResidueItem>, selected: Set<String>): Boolean {
        val chosen = selectedItems(items, selected)
        return chosen.isNotEmpty() && chosen.all { it.candidate.provenance == DataProvenance.MEDIASTORE }
    }

    /**
     * Builds the dry-run the customer confirms.
     *
     * [OperationKind.MOVE_TO_TRASH] is checked here because it changes what the
     * confirmation can promise; the caller must have already validated the action.
     */
    fun dryRun(
        items: List<ResidueItem>,
        selected: Set<String>,
        kind: OperationKind = OperationKind.PERMANENT_DELETE,
    ): ResidueDryRun {
        val chosen = selectedItems(items, selected)
        val owned = chosen.count { it.confidence == ResidueConfidence.OWNED }
        val shared = chosen.size - owned
        val weak = chosen.count { it.confidence == ResidueConfidence.WEAK }
        val warnings = buildList {
            if (shared > 0) {
                add("$shared shared-folder item(s) match by filename only — review each one")
            }
            if (weak > 0) {
                add("$weak selected item(s) use a generic leftover name; a real file could share it")
            }
            if (chosen.any { CleanupRules.requiresSystemDialog(kind, it.candidate.provenance) }) {
                add("Android will show its own confirmation for shared media")
            }
            val skipped = items.count { it.candidate.uri in selected && !CleanupRules.isSelectable(it.candidate) }
            if (skipped > 0) {
                add("$skipped selected item(s) cannot be removed with the current access and are excluded")
            }
            if (chosen.isEmpty()) {
                add("Nothing is selected")
            }
        }
        return ResidueDryRun(
            items = chosen,
            bytes = chosen.sumOf { it.candidate.bytes },
            ownedCount = owned,
            sharedCount = shared,
            weakCount = weak,
            needsAndroidDialog = chosen.any { CleanupRules.requiresSystemDialog(kind, it.candidate.provenance) },
            warnings = warnings,
        )
    }

    /** Drops only URIs an operation proved gone; everything else stays visible. */
    fun afterVerifiedRemoval(items: List<ResidueItem>, removed: Set<String>): List<ResidueItem> =
        items.filter { it.candidate.uri !in removed }

    data class Summary(val count: Int, val bytes: Long, val sharedCount: Int, val sharedBytes: Long, val patterns: Map<ResiduePattern, Int>) {
        val ownedCount: Int get() = count - sharedCount
        val ownedBytes: Long get() = bytes - sharedBytes
    }

    fun summarize(items: List<ResidueItem>): Summary {
        var sharedCount = 0
        var sharedBytes = 0L
        items.forEach { item ->
            if (item.confidence != ResidueConfidence.OWNED) {
                sharedCount++
                sharedBytes += item.candidate.bytes
            }
        }
        return Summary(
            count = items.size,
            bytes = items.sumOf { it.candidate.bytes },
            sharedCount = sharedCount,
            sharedBytes = sharedBytes,
            patterns = items.groupingBy { it.pattern }.eachCount(),
        )
    }
}
