package com.knoux.ai.data

/**
 * PC02 — byte-identical duplicate rules.
 *
 * These helpers are deliberately pure so the keeper guarantee can be proven by a
 * JVM test: exactly one copy of every group is preserved, it is never selectable,
 * and a group with a single surviving member ceases to be a duplicate group.
 *
 * A duplicate only exists when two files have the same byte length *and* the same
 * full-content SHA-256. Similar name, similar size, or a partial fingerprint are
 * never treated as proof.
 */

/** A file mapped to the SHA-256 of its complete content. */
data class HashedFile(val candidate: FileCandidate, val contentHash: String)

/** One verified duplicate set: one preserved original plus removable copies. */
data class DuplicateGroup(
    /** Stable within a scan; equals the content hash. */
    val id: String,
    val contentHash: String,
    val bytes: Long,
    val keeper: FileCandidate,
    val duplicates: List<FileCandidate>,
) {
    val memberCount: Int get() = duplicates.size + 1
    val redundantBytes: Long get() = duplicates.sumOf { it.bytes }
}

object DuplicateRules {

    /** Files below this size are not worth a content read; the reason stays visible. */
    const val MIN_BYTES: Long = 1_024L

    const val KEEPER_REASON = "Original copy — kept, never offered for removal"

    /**
     * Chooses the preserved copy: the oldest file wins so the customer's original
     * is the one that stays. Ties break on URI so the result is deterministic and
     * testable; without a tie-break a group could flip keeper between scans.
     */
    fun keeper(a: FileCandidate, b: FileCandidate): FileCandidate = when {
        a.lastModifiedMillis != b.lastModifiedMillis ->
            if (a.lastModifiedMillis < b.lastModifiedMillis) a else b
        else -> if (a.uri <= b.uri) a else b
    }

    /**
     * Collapses aliases that point at the same physical file.
     *
     * The same photo can be enumerated by MediaStore and again through a granted
     * SAF folder. Those are two URIs for one file, not two copies; offering one
     * for deletion would remove the file the group calls its original. When two
     * aliases share a key, the MediaStore row wins because its removal is
     * OS-mediated. A null key is always kept as a distinct file.
     */
    fun distinctPhysicalFiles(files: List<FileCandidate>): List<FileCandidate> {
        val chosen = LinkedHashMap<String, FileCandidate>()
        files.forEach { candidate ->
            val key = candidate.physicalKey
            if (key == null) {
                chosen["uri:${candidate.uri}"] = candidate
                return@forEach
            }
            val existing = chosen[key]
            if (existing == null || provenanceRank(candidate) < provenanceRank(existing)) {
                chosen[key] = candidate
            }
        }
        return chosen.values.toList()
    }

    private fun provenanceRank(candidate: FileCandidate): Int = when (candidate.provenance) {
        DataProvenance.MEDIASTORE -> 0
        DataProvenance.SAF_DOCUMENT -> 1
        DataProvenance.APP_OWNED_CACHE -> 2
    }

    /** Groups hashed files, marking exactly one keeper and the rest as copies. */
    fun group(hashed: List<HashedFile>): List<DuplicateGroup> =
        hashed
            .filter { it.contentHash.isNotBlank() && this.isHash(it.contentHash) }
            .groupBy { it.contentHash }
            .mapValues { (_, rows) ->
                rows.distinctBy { it.candidate.physicalKey ?: it.candidate.uri }
            }
            .filter { (_, rows) -> rows.size >= 2 && rows.map { it.candidate.bytes }.distinct().size == 1 }
            .map { (hash, rows) ->
                val members = rows.map { it.candidate }.sortedWith(
                    compareBy<FileCandidate> { it.lastModifiedMillis }.thenBy { it.uri }
                )
                val keeper = members.first()
                DuplicateGroup(
                    id = hash,
                    contentHash = hash,
                    bytes = keeper.bytes,
                    keeper = asKeeper(keeper),
                    duplicates = members.drop(1).map { asDuplicate(it, keeper) },
                )
            }
            // Largest recoverable space first, then the biggest groups.
            .sortedWith(
                compareByDescending<DuplicateGroup> { it.redundantBytes }
                    .thenByDescending { it.memberCount }
                    .thenBy { it.keeper.displayName }
            )

    private fun isHash(value: String): Boolean =
        value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }

    /** The keeper is protected regardless of what its original eligibility allowed. */
    fun asKeeper(candidate: FileCandidate): FileCandidate = candidate.copy(
        eligibility = Eligibility.PROTECTED,
        reason = KEEPER_REASON,
    )

    /** Copies keep the provider's own eligibility; an unselectable copy stays unselectable. */
    fun asDuplicate(candidate: FileCandidate, keeper: FileCandidate): FileCandidate = candidate.copy(
        eligibility = if (candidate.eligibility == Eligibility.PROTECTED) Eligibility.UNSELECTABLE
        else candidate.eligibility,
        reason = "Byte-identical copy of ${keeper.displayName} (SHA-256 verified)",
    )

    /** Selectable copies only: the keeper can never enter a plan. */
    fun selectableUris(group: DuplicateGroup): Set<String> =
        group.duplicates.filter { CleanupRules.isSelectable(it) }.map { it.uri }.toSet()

    /** Applying a group checkbox never adds the keeper or an unselectable copy. */
    fun toggleGroup(group: DuplicateGroup, selected: Set<String>, checked: Boolean): Set<String> {
        val groupUris = selectableUris(group)
        return if (checked) selected + groupUris else selected - groupUris
    }

    fun selectedBytes(groups: List<DuplicateGroup>, selected: Set<String>): Long =
        groups.flatMap { it.duplicates }
            .filter { it.uri in selected && CleanupRules.isSelectable(it) }
            .sumOf { it.bytes }

    /**
     * Resolves a frozen selection back to candidates.
     *
     * A keeper URI that somehow reached the caller is discarded here as well as in
     * the UI, so a corrupted selection cannot delete the preserved original.
     */
    fun selectedCandidates(groups: List<DuplicateGroup>, selected: Set<String>): List<FileCandidate> {
        val keepers = groups.map { it.keeper.uri }.toSet()
        return groups.flatMap { it.duplicates }
            .filter { it.uri in selected && it.uri !in keepers && CleanupRules.isSelectable(it) }
            .distinctBy { it.uri }
    }

    /** Trash is offered only when every selected copy is shared media. */
    fun canTrash(candidates: List<FileCandidate>): Boolean =
        candidates.isNotEmpty() && candidates.all { it.provenance == DataProvenance.MEDIASTORE }

    /** Running measurement used while hashing; cheap, but must agree with [group]. */
    data class ProgressSummary(val groups: Int, val redundantBytes: Long)

    /**
     * O(n) equivalent of the totals [group] would produce.
     *
     * Recomputing the full grouping after every hashed file would be quadratic on a
     * large library, so the progress readout uses this instead. A JVM test pins it to
     * the real grouping so the number on screen cannot drift from the result.
     */
    fun summarize(hashed: List<HashedFile>): ProgressSummary {
        val counts = HashMap<String, Int>()
        val sizes = HashMap<String, Long>()
        hashed.forEach { file ->
            val hash = file.contentHash
            if (!isHash(hash)) return@forEach
            counts[hash] = (counts[hash] ?: 0) + 1
            sizes[hash] = file.candidate.bytes
        }
        var groups = 0
        var redundant = 0L
        counts.forEach { (hash, count) ->
            if (count >= 2) {
                groups++
                redundant += (count - 1).toLong() * (sizes[hash] ?: 0L)
            }
        }
        return ProgressSummary(groups, redundant)
    }

    /** Contract check used by the UI and tests: no keeper may ever be selected. */
    fun selectionKeepsEveryOriginal(groups: List<DuplicateGroup>, selected: Set<String>): Boolean =
        groups.all { it.keeper.uri !in selected }

    /**
     * Rebuilds groups after an OS-verified removal.
     *
     * - a group with fewer than two surviving members stops being a duplicate group;
     * - if the original itself vanished outside KNOUX, the oldest surviving copy is
     *   promoted so the customer still keeps one file;
     * - the promoted copy keeps its real eligibility, then becomes protected.
     */
    fun afterVerifiedRemoval(groups: List<DuplicateGroup>, removed: Set<String>): List<DuplicateGroup> =
        groups.mapNotNull { group ->
            val members = (listOf(group.keeper) + group.duplicates)
                .distinctBy { it.uri }
                .filter { it.uri !in removed }
                .sortedWith(compareBy<FileCandidate> { it.lastModifiedMillis }.thenBy { it.uri })
            if (members.size < 2) return@mapNotNull null
            val keeper = members.first()
            DuplicateGroup(
                id = group.id,
                contentHash = group.contentHash,
                bytes = keeper.bytes,
                keeper = asKeeper(keeper),
                duplicates = members.drop(1).map { asDuplicate(it, keeper) },
            )
        }.sortedWith(
            compareByDescending<DuplicateGroup> { it.redundantBytes }
                .thenByDescending { it.memberCount }
                .thenBy { it.keeper.displayName }
        )
}
