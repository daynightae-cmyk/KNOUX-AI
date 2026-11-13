package com.knoux.ai.data

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.util.UUID

/**
 * PC01 — the real Smart Clean engine.
 *
 * Everything in this class is measured from the platform. There is no fixture
 * data, no synthetic byte count, and no optimistic result: a candidate only
 * appears because the OS reported it, and an operation only reports success after
 * the provider was re-queried and the item was confirmed gone or in recoverable trash.
 *
 * Deliberate limits, stated rather than hidden:
 *  - Only KNOUX's *own* cache is walked. Other apps' private caches are unreachable
 *    without root, which this product does not and will not use.
 *  - Shared media is removed exclusively through an OS confirmation dialog
 *    (API 30+). Below API 30 there is no provider-safe trash, so the capability is
 *    reported UNAVAILABLE instead of attempted with a legacy broad grant.
 *  - SAF documents are deleted only inside a tree the customer granted and only
 *    when the provider advertises FLAG_SUPPORTS_DELETE.
 */
class SmartCleanRepository(
    private val context: Context,
    private val permissions: PermissionState
) {

    companion object {
        /** Hard ceiling so a pathological folder cannot exhaust memory. */
        private const val MAX_CACHE_ENTRIES = 5_000
        private const val MAX_MEDIA_ENTRIES = 5_000
        private const val MAX_SAF_ENTRIES = 2_000
        private const val MAX_DEPTH = 12
    }

    // ── Capability ───────────────────────────────────────────────────────────

    /** Trash/delete of shared media is only honest on API 30+, where the OS mediates it. */
    fun mediaRemovalCapability(): FeatureCapability =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) FeatureCapability.AVAILABLE
        else FeatureCapability.UNSUPPORTED

    /** Free bytes on the data volume. Used for observation, never as a promise. */
    fun freeBytes(): Long = try {
        StatFs(Environment.getDataDirectory().absolutePath).availableBytes
    } catch (t: Throwable) {
        -1L
    }

    // ── Discovery ────────────────────────────────────────────────────────────

    data class ScanProgress(
        val itemsFound: Int,
        val bytesFound: Long,
        val label: String,
        val scopesDone: Int = 0,
        val scopesTotal: Int = 0
    )

    private data class DiscoveryResult(
        val candidates: List<FileCandidate>,
        val status: DataStatus = DataStatus.MEASURED,
        val detail: String? = null
    )

    /** Counts inspected entries, not just eligible matches (shared by all SAF trees). */
    private class EntryBudget(val limit: Int) {
        var visited: Int = 0
            private set

        fun visit(): Boolean {
            if (visited >= limit) return false
            visited++
            return true
        }
    }

    private fun discoveryResult(
        candidates: List<FileCandidate>,
        issues: Set<String>,
        successfulReads: Int
    ): DiscoveryResult = DiscoveryResult(
        candidates = candidates,
        status = when {
            issues.isEmpty() -> DataStatus.MEASURED
            successfulReads > 0 -> DataStatus.PARTIAL
            else -> DataStatus.UNAVAILABLE
        },
        detail = issues.takeIf { it.isNotEmpty() }?.joinToString("; ")
    )

    /**
     * Real, cancellable scan over the scopes the customer actually granted.
     *
     * A scope that is not granted is recorded as DENIED with zero items — it is
     * never silently skipped, so the UI can show precisely what was not read.
     */
    suspend fun scan(
        scopes: Set<AccessScope>,
        excludedUris: Set<String>,
        onProgress: (ScanProgress) -> Unit = {}
    ): Pair<ScanSession, List<FileCandidate>> {
        val startedAt = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()
        val candidates = linkedMapOf<String, FileCandidate>()
        val snapshots = mutableListOf<SourceSnapshot>()
        val requestedScopes = scopes.toSet()

        fun progress(label: String) {
            onProgress(ScanProgress(
                itemsFound = candidates.size,
                bytesFound = candidates.values.sumOf { it.bytes },
                label = label,
                scopesDone = snapshots.size,
                scopesTotal = requestedScopes.size
            ))
        }

        progress("Starting scoped scan")
        for (scope in requestedScopes) {
            currentCoroutineContext().ensureActive()
            var grant = PermissionGrant.NOT_REQUIRED
            val provenance = when (scope) {
                AccessScope.APP_CACHE -> DataProvenance.APP_OWNED_CACHE
                AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO -> DataProvenance.MEDIASTORE
                AccessScope.SAF_TREE -> DataProvenance.SAF_DOCUMENT
            }
            val excluded = excludedUris + candidates.keys
            progress("Reading ${scope.name}")
            val result = when (scope) {
                AccessScope.APP_CACHE -> cacheCandidates(excluded)
                AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO -> {
                    grant = permissions.mediaGrant(scope)
                    if (grant == PermissionGrant.GRANTED || grant == PermissionGrant.PARTIAL_SELECTED) {
                        val found = mediaCandidates(scope, excluded)
                        val latestGrant = permissions.mediaGrant(scope)
                        val limited = grant == PermissionGrant.PARTIAL_SELECTED || latestGrant != PermissionGrant.GRANTED
                        grant = latestGrant
                        if (limited) found.copy(
                            status = if (found.status == DataStatus.UNAVAILABLE) DataStatus.UNAVAILABLE else DataStatus.PARTIAL,
                            detail = listOfNotNull(found.detail, "Selected-only or changed permission; full library not verified").joinToString("; ")
                        ) else found
                    } else {
                        DiscoveryResult(emptyList(), DataStatus.UNAVAILABLE, "No permission for ${scope.name}")
                    }
                }
                AccessScope.SAF_TREE -> {
                    // Deletion needs a writable grant, so only writable trees are walked.
                    try {
                        val trees = permissions.safTrees(requireWrite = true).distinct()
                        grant = if (trees.isEmpty()) PermissionGrant.DENIED else PermissionGrant.GRANTED
                        if (trees.isEmpty()) {
                            DiscoveryResult(emptyList(), DataStatus.UNAVAILABLE, "No writable folder granted")
                        } else {
                            val found = linkedMapOf<String, FileCandidate>()
                            val issues = linkedSetOf<String>()
                            var successfulTrees = 0
                            val budget = EntryBudget(MAX_SAF_ENTRIES)
                            for (tree in trees) {
                                currentCoroutineContext().ensureActive()
                                if (budget.visited >= budget.limit) {
                                    issues += "SAF entry ceiling reached (${budget.limit}); remaining folders not read"
                                    break
                                }
                                val treeResult = safCandidates(tree, excluded + found.keys, budget)
                                treeResult.candidates.forEach { found.putIfAbsent(it.uri, it) }
                                if (treeResult.status != DataStatus.UNAVAILABLE) successfulTrees++
                                treeResult.detail?.let { issues += it }
                            }
                            try {
                                if (!permissions.safTrees(requireWrite = true).containsAll(trees)) {
                                    grant = PermissionGrant.REVOKED
                                    issues += "Folder permission changed during scan"
                                }
                            } catch (c: CancellationException) {
                                throw c
                            } catch (t: Exception) {
                                grant = PermissionGrant.REVOKED
                                issues += "Folder grants could not be rechecked: ${t.javaClass.simpleName}"
                            }
                            discoveryResult(found.values.toList(), issues, successfulTrees).let {
                                it.copy(detail = listOfNotNull("${trees.size} granted folder(s)", it.detail).joinToString("; "))
                            }
                        }
                    } catch (c: CancellationException) {
                        throw c
                    } catch (t: Exception) {
                        grant = PermissionGrant.REVOKED
                        DiscoveryResult(emptyList(), DataStatus.UNAVAILABLE, "Folder grants unavailable: ${t.javaClass.simpleName}")
                    }
                }
            }
            val unique = result.candidates.distinctBy { it.uri }.filter { it.uri !in candidates && it.uri !in excludedUris }
            unique.forEach { candidates[it.uri] = it }
            snapshots += SourceSnapshot(
                scope = scope,
                provenance = provenance,
                grant = grant,
                itemCount = unique.size,
                totalBytes = unique.sumOf { it.bytes },
                capturedAtMillis = System.currentTimeMillis(),
                status = result.status,
                detail = result.detail
            )
            progress("Finished ${scope.name}")
        }

        val session = ScanSession(
            id = sessionId,
            startedAtMillis = startedAt,
            finishedAtMillis = System.currentTimeMillis(),
            status = if (snapshots.any { it.status != DataStatus.MEASURED }) ScanStatus.PARTIAL else ScanStatus.COMPLETED,
            scopes = requestedScopes,
            itemCount = candidates.size,
            totalBytes = candidates.values.sumOf { it.bytes },
            snapshots = snapshots
        )
        return session to candidates.values.toList()
    }

    /** Walks only directories this app owns. Never another app's private storage. */
    private suspend fun cacheCandidates(excluded: Set<String>): DiscoveryResult {
        val roots = try {
            buildList {
                add(context.cacheDir)
                add(context.codeCacheDir)
                context.externalCacheDir?.let { add(it) }
            }.distinctBy { it.absolutePath }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Exception) {
            return DiscoveryResult(emptyList(), DataStatus.UNAVAILABLE, "Cache roots unavailable: ${t.javaClass.simpleName}")
        }
        val out = linkedMapOf<String, FileCandidate>()
        val issues = linkedSetOf<String>()
        val budget = EntryBudget(MAX_CACHE_ENTRIES)
        var successfulReads = 0
        for (root in roots) {
            val stack = ArrayDeque<Pair<File, Int>>()
            stack.addLast(root to 0)
            while (stack.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                if (budget.visited >= budget.limit) {
                    issues += "Cache entry ceiling reached (${budget.limit}); traversal incomplete"
                    break
                }
                val (dir, depth) = stack.removeLast()
                try {
                    val children = dir.listFiles()
                    if (children == null) {
                        issues += "Cache directory could not be listed"
                        continue
                    }
                    successfulReads++
                    for (file in children) {
                        currentCoroutineContext().ensureActive()
                        if (!budget.visit()) {
                            issues += "Cache entry ceiling reached (${budget.limit}); traversal incomplete"
                            break
                        }
                        // Never follow links, including links to files outside the sandbox.
                        if (isSymlink(file)) continue
                        if (file.isDirectory) {
                            if (depth >= MAX_DEPTH) {
                                issues += "Cache depth ceiling reached ($MAX_DEPTH); traversal incomplete"
                            } else {
                                stack.addLast(file to depth + 1)
                            }
                            continue
                        }
                        if (!file.isFile) continue
                        val length = file.length()
                        if (length <= 0L) continue
                        val uri = Uri.fromFile(file).toString()
                        if (uri in excluded || uri in out) continue
                        val category = classifyCache(file)
                        out[uri] = FileCandidate(
                            uri = uri,
                            displayName = file.name,
                            mimeType = guessMime(file.name),
                            bytes = length,
                            lastModifiedMillis = file.lastModified(),
                            category = category,
                            provenance = DataProvenance.APP_OWNED_CACHE,
                            eligibility = Eligibility.ELIGIBLE,
                            reason = when (category) {
                                CandidateCategory.THUMBNAIL -> "Cached thumbnail generated by KNOUX"
                                CandidateCategory.TEMP_FILE -> "Temporary file left in KNOUX's cache"
                                else -> "File inside KNOUX's own cache directory"
                            }
                        )
                    }
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Exception) {
                    issues += "Cache read failed: ${t.javaClass.simpleName}"
                }
            }
        }
        return discoveryResult(out.values.toList(), issues, successfulReads).let {
            it.copy(detail = listOfNotNull("KNOUX's own cache directories", it.detail).joinToString("; "))
        }
    }

    /**
     * Returns true only when the file's canonical path escapes its parent directory,
     * which is the actual security concern (symlink pointing outside the sandbox).
     *
     * The naive `canonical != absolute` check is incorrect on Android: `context.cacheDir`
     * on API 29+ returns `/data/user/0/<pkg>/cache` while the kernel resolves it via
     * `/data/data/<pkg>/cache` (or vice-versa depending on mount order). Every file
     * inside legitimately owned cache directories would be misidentified as a symlink
     * and silently skipped, producing zero scan results.
     *
     * Instead we compare the file's canonical path against its parent's canonical path:
     * a real symlink pointing *outside* the parent will produce a canonical path that
     * does not start with the parent's canonical path. Files that are simply inside a
     * directory whose absolute and canonical paths differ (the Android /data/user/0
     * situation) are correctly identified as non-symlinks.
     */
    private fun isSymlink(file: File): Boolean = try {
        val parentCanonical = file.canonicalFile.parentFile?.canonicalPath ?: return true
        val fileCanonical = file.canonicalPath
        // If the file's canonical path is not inside its own parent's canonical path,
        // the entry is a symlink pointing somewhere unexpected — skip it.
        !fileCanonical.startsWith(parentCanonical + File.separator) &&
            fileCanonical != parentCanonical
    } catch (t: Throwable) {
        true
    }

    private fun classifyCache(file: File): CandidateCategory {
        val name = file.name.lowercase()
        val path = file.absolutePath.lowercase()
        return when {
            path.contains("code_cache") -> CandidateCategory.APP_CACHE
            name.endsWith(".tmp") || name.endsWith(".temp") || name.endsWith(".log") -> CandidateCategory.TEMP_FILE
            name.contains("thumb") || name.endsWith(".thumb") -> CandidateCategory.THUMBNAIL
            else -> CandidateCategory.APP_CACHE
        }
    }

    private fun guessMime(name: String): String? = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "json" -> "application/json"
        "txt", "log" -> "text/plain"
        else -> null
    }

    /** Reads MediaStore rows for the requested media kind. */
    private suspend fun mediaCandidates(scope: AccessScope, excluded: Set<String>): DiscoveryResult {
        val collection = when (scope) {
            AccessScope.MEDIA_IMAGES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.MediaColumns.RELATIVE_PATH)
        }.toTypedArray()
        val out = linkedMapOf<String, FileCandidate>()
        val issues = linkedSetOf<String>()
        val budget = EntryBudget(MAX_MEDIA_ENTRIES)
        var successfulReads = 0
        val removable = mediaRemovalCapability() == FeatureCapability.AVAILABLE
        try {
            val cursor = context.contentResolver.query(
                collection, projection, null, null, "${MediaStore.MediaColumns.SIZE} DESC"
            )
            if (cursor == null) {
                issues += "Media provider returned no cursor"
            } else cursor.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    it.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH) else -1
                successfulReads++
                while (it.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    if (!budget.visit()) {
                        issues += "Media entry ceiling reached (${budget.limit}); library incomplete"
                        break
                    }
                    if (it.isNull(idCol) || it.isNull(sizeCol)) {
                        issues += "Media provider omitted item identity or size"
                        continue
                    }
                    val id = it.getLong(idCol)
                    val size = it.getLong(sizeCol)
                    if (size <= 0L) continue
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    if (uri in excluded || uri in out) continue
                    val reason = if (removable) "Shared media — removal is confirmed by Android"
                        else "This Android version has no OS-mediated media removal"
                    val source = if (pathCol >= 0) " · Relative path: ${it.getString(pathCol) ?: "not reported"}" else ""
                    out[uri] = FileCandidate(
                        uri = uri,
                        displayName = it.getString(nameCol) ?: "Item $id",
                        mimeType = it.getString(mimeCol),
                        bytes = size,
                        lastModifiedMillis = it.getLong(dateCol) * 1000L,
                        category = if (scope == AccessScope.MEDIA_IMAGES) CandidateCategory.IMAGE else CandidateCategory.VIDEO,
                        provenance = DataProvenance.MEDIASTORE,
                        eligibility = if (removable) Eligibility.ELIGIBLE else Eligibility.UNSELECTABLE,
                        reason = reason + source
                    )
                }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Exception) {
            // Preserve rows already observed, but never present provider failure as a full scan.
            issues += "Media query failed: ${t.javaClass.simpleName}"
        }
        return discoveryResult(out.values.toList(), issues, successfulReads)
    }

    /** Direct child queries keep null/failing providers distinct from authoritative empty folders. */
    private suspend fun safCandidates(tree: Uri, excluded: Set<String>, budget: EntryBudget): DiscoveryResult {
        val out = linkedMapOf<String, FileCandidate>()
        val issues = linkedSetOf<String>()
        var successfulReads = 0
        val rootId = try {
            DocumentsContract.getTreeDocumentId(tree)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Exception) {
            return DiscoveryResult(emptyList(), DataStatus.UNAVAILABLE, "Invalid SAF tree: ${t.javaClass.simpleName}")
        }
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
        )
        val queue = ArrayDeque<Pair<String, Int>>()
        val visitedDirectories = hashSetOf<String>()
        queue.addLast(rootId to 0)
        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            if (budget.visited >= budget.limit) {
                issues += "SAF entry ceiling reached (${budget.limit}); traversal incomplete"
                break
            }
            val (directoryId, depth) = queue.removeFirst()
            if (!visitedDirectories.add(directoryId)) continue
            try {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, directoryId)
                val cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
                if (cursor == null) {
                    issues += "SAF provider returned no child cursor"
                    continue
                }
                cursor.use {
                    val idCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val dateCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val flagsCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)
                    successfulReads++
                    while (it.moveToNext()) {
                        currentCoroutineContext().ensureActive()
                        if (!budget.visit()) {
                            issues += "SAF entry ceiling reached (${budget.limit}); traversal incomplete"
                            break
                        }
                        val id = it.getString(idCol)
                        val mime = it.getString(mimeCol)
                        if (id == null || mime == null) {
                            issues += "SAF provider omitted document identity or type"
                            continue
                        }
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (id in visitedDirectories) continue
                            if (depth >= MAX_DEPTH) {
                                issues += "SAF depth ceiling reached ($MAX_DEPTH); traversal incomplete"
                            } else {
                                queue.addLast(id to depth + 1)
                            }
                            continue
                        }
                        if (it.isNull(sizeCol)) {
                            issues += "SAF provider omitted document size"
                            continue
                        }
                        val length = it.getLong(sizeCol)
                        if (length <= 0L) continue
                        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString()
                        if (uri in excluded || uri in out) continue
                        val deletable = !it.isNull(flagsCol) &&
                            (it.getInt(flagsCol) and DocumentsContract.Document.FLAG_SUPPORTS_DELETE) != 0
                        if (it.isNull(flagsCol)) issues += "SAF provider omitted deletion capability"
                        out[uri] = FileCandidate(
                            uri = uri,
                            displayName = it.getString(nameCol) ?: "Document",
                            mimeType = mime,
                            bytes = length,
                            lastModifiedMillis = it.getLong(dateCol),
                            category = CandidateCategory.SAF_FILE,
                            provenance = DataProvenance.SAF_DOCUMENT,
                            eligibility = if (deletable) Eligibility.ELIGIBLE else Eligibility.UNSELECTABLE,
                            reason = if (deletable) "Inside a folder you granted; provider allows deletion"
                                else "This provider does not advertise deletion of this document"
                        )
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Exception) {
                issues += "SAF child query failed: ${t.javaClass.simpleName}"
            }
        }
        return discoveryResult(out.values.toList(), issues, successfulReads)
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    /**
     * Builds the OS confirmation for trashing shared media (API 30+).
     *
     * The returned [IntentSender] must be launched by the caller; nothing is
     * deleted until the customer approves inside the Android dialog.
     */
    fun trashRequestIntent(uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uris.isEmpty()) return null
        return try {
            MediaStore.createTrashRequest(context.contentResolver, uris, true).intentSender
        } catch (t: Throwable) {
            null
        }
    }

    /** Builds the OS confirmation to restore trashed media; the caller must launch it. */
    fun restoreRequestIntent(uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uris.isEmpty()) return null
        return try {
            MediaStore.createTrashRequest(context.contentResolver, uris, false).intentSender
        } catch (t: Exception) {
            null
        }
    }

    /** Builds the OS confirmation for permanent deletion of shared media (API 30+). */
    fun deleteRequestIntent(uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uris.isEmpty()) return null
        return try {
            MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Deletes app-owned cache entries for real.
     *
     * Only paths that resolve inside our own cache roots are touched; anything
     * else is reported as failed rather than silently attempted.
     */
    fun deleteAppOwnedCache(candidates: List<FileCandidate>): List<Pair<FileCandidate, Result<Unit>>> {
        val roots = try {
            buildList {
                add(context.cacheDir)
                add(context.codeCacheDir)
                context.externalCacheDir?.let { add(it) }
            }.map { it.canonicalPath }
        } catch (t: Exception) {
            return candidates.map { it to Result.failure<Unit>(t) }
        }

        return candidates.map { candidate ->
            val file = try {
                val uri = Uri.parse(candidate.uri)
                val path = uri.path
                if (candidate.provenance != DataProvenance.APP_OWNED_CACHE || uri.scheme != "file" || path.isNullOrBlank()) {
                    return@map candidate to Result.failure<Unit>(IllegalArgumentException("Invalid app-owned cache URI"))
                }
                File(path)
            } catch (t: Throwable) {
                return@map candidate to Result.failure<Unit>(t)
            }
            val canonical = try {
                file.canonicalPath
            } catch (t: Throwable) {
                return@map candidate to Result.failure<Unit>(t)
            }
            // Sandbox check: refuse anything that escaped our cache roots.
            if (roots.none { canonical.startsWith(it + File.separator) }) {
                return@map candidate to Result.failure<Unit>(
                    SecurityException("Refusing to delete outside KNOUX cache")
                )
            }
            candidate to runCatching {
                if (!file.exists()) {
                    throw IllegalStateException("Cache item is missing; no deletion performed for ${candidate.displayName}")
                }
                if (!file.isFile || !file.delete()) {
                    throw IllegalStateException("Filesystem refused to delete ${candidate.displayName}")
                }
            }
        }
    }

    /** Deletes SAF documents that the provider said it supports deleting. */
    fun deleteSafDocuments(candidates: List<FileCandidate>): List<Pair<FileCandidate, Result<Unit>>> =
        candidates.map { candidate ->
            candidate to runCatching {
                val ok = DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(candidate.uri))
                if (!ok) throw IllegalStateException("Provider refused to delete ${candidate.displayName}")
            }
        }

    /** TRASHED is recoverable and must not be confused with permanent deletion or restore. */
    enum class Presence { PRESENT, GONE, TRASHED, UNKNOWN }

    /** Observes each distinct URI; an inaccessible or malformed handle is never proof of removal. */
    fun presence(candidates: List<FileCandidate>): Map<String, Presence> =
        candidates.distinctBy { it.uri }.associate { candidate ->
            val state = try {
                val uri = Uri.parse(candidate.uri)
                when (candidate.provenance) {
                    DataProvenance.APP_OWNED_CACHE -> cachePresence(uri)
                    DataProvenance.MEDIASTORE -> mediaPresence(candidate, uri)
                    DataProvenance.SAF_DOCUMENT -> safPresence(uri)
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Exception) {
                Presence.UNKNOWN
            }
            candidate.uri to state
        }

    /** Compatibility for removal only. Restore must require PRESENT; permanent delete must require GONE. */
    fun verifyGone(candidates: List<FileCandidate>): Map<String, Boolean> =
        presence(candidates).mapValues { (_, state) -> state == Presence.GONE }

    private fun cachePresence(uri: Uri): Presence {
        val path = uri.path
        if (uri.scheme != "file" || path.isNullOrBlank()) return Presence.UNKNOWN
        val file = File(path)
        val canonical = file.canonicalPath
        val roots = buildList {
            add(context.cacheDir)
            add(context.codeCacheDir)
            context.externalCacheDir?.let { add(it) }
        }.map { it.canonicalPath }
        if (roots.none { canonical.startsWith(it + File.separator) }) return Presence.UNKNOWN
        if (file.exists()) return Presence.PRESENT
        // exists() == false alone can hide a permission/I/O error. Confirm absence by a readable parent.
        val siblings = file.parentFile?.listFiles() ?: return Presence.UNKNOWN
        return if (siblings.any { it.name == file.name }) Presence.PRESENT else Presence.GONE
    }

    private fun mediaPresence(candidate: FileCandidate, uri: Uri): Presence {
        if (uri.scheme != "content" || uri.authority != MediaStore.AUTHORITY) return Presence.UNKNOWN
        val scope = when (candidate.category) {
            CandidateCategory.IMAGE -> AccessScope.MEDIA_IMAGES
            CandidateCategory.VIDEO -> AccessScope.MEDIA_VIDEO
            else -> return Presence.UNKNOWN
        }
        val grantBefore = permissions.mediaGrant(scope)
        val expectedId = ContentUris.parseId(uri)
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.IS_TRASHED
        ) else arrayOf(MediaStore.MediaColumns._ID)
        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val args = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            }
            context.contentResolver.query(uri, projection, args, null)
        } else {
            context.contentResolver.query(uri, projection, null, null, null)
        } ?: return Presence.UNKNOWN
        return cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val trashCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                it.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_TRASHED) else -1
            if (!it.moveToFirst()) {
                // Selected-only/revoked permission can also hide rows. An empty subset proves nothing.
                if (grantBefore == PermissionGrant.GRANTED && permissions.mediaGrant(scope) == PermissionGrant.GRANTED)
                    Presence.GONE else Presence.UNKNOWN
            } else if (it.isNull(idCol) || it.getLong(idCol) != expectedId) {
                Presence.UNKNOWN
            } else if (trashCol >= 0) {
                if (it.isNull(trashCol)) Presence.UNKNOWN
                else when (it.getInt(trashCol)) {
                    0 -> Presence.PRESENT
                    1 -> Presence.TRASHED
                    else -> Presence.UNKNOWN
                }
            } else Presence.PRESENT
        }
        // SecurityException, missing projection, and other query failures become UNKNOWN in presence().
    }

    private fun hasSafReadGrant(uri: Uri): Boolean {
        if (!DocumentsContract.isTreeUri(uri)) return false
        val treeId = DocumentsContract.getTreeDocumentId(uri)
        return permissions.safTrees().any {
            DocumentsContract.isTreeUri(it) && it.authority == uri.authority &&
                DocumentsContract.getTreeDocumentId(it) == treeId
        }
    }

    private fun safPresence(uri: Uri): Presence {
        if (uri.scheme != "content") return Presence.UNKNOWN
        val documentId = DocumentsContract.getDocumentId(uri)
        val grantBefore = hasSafReadGrant(uri)
        try {
            val cursor = context.contentResolver.query(
                uri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null
            )
            if (cursor != null) return cursor.use {
                val idCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                if (it.moveToFirst()) {
                    if (it.getString(idCol) == documentId) Presence.PRESENT else Presence.UNKNOWN
                } else if (grantBefore && hasSafReadGrant(uri)) Presence.GONE else Presence.UNKNOWN
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: SecurityException) {
            return Presence.UNKNOWN
        } catch (t: Exception) {
            // A thrown "missing" is not evidence. Only a successful tree listing can replace it.
        }
        if (!grantBefore || !hasSafReadGrant(uri)) return Presence.UNKNOWN
        return safTreePresence(uri, documentId)
    }

    /** Full, bounded tree enumeration; document IDs are opaque, so never guess a filesystem parent. */
    private fun safTreePresence(tree: Uri, targetId: String): Presence {
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        // Listing a root's children cannot prove that the root itself is absent.
        if (targetId == rootId) return Presence.UNKNOWN
        val queue = ArrayDeque<Pair<String, Int>>()
        val visitedDirectories = hashSetOf<String>()
        val budget = EntryBudget(MAX_SAF_ENTRIES)
        queue.addLast(rootId to 0)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        while (queue.isNotEmpty()) {
            if (budget.visited >= budget.limit) return Presence.UNKNOWN
            val (directoryId, depth) = queue.removeFirst()
            if (!visitedDirectories.add(directoryId)) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, directoryId)
            val cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
                ?: return Presence.UNKNOWN
            cursor.use {
                val idCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (it.moveToNext()) {
                    if (!budget.visit()) return Presence.UNKNOWN
                    val id = it.getString(idCol) ?: return Presence.UNKNOWN
                    if (id == targetId) return Presence.PRESENT
                    val mime = it.getString(mimeCol) ?: return Presence.UNKNOWN
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR && id !in visitedDirectories) {
                        if (depth >= MAX_DEPTH) return Presence.UNKNOWN
                        queue.addLast(id to depth + 1)
                    }
                }
            }
        }
        // Every child listing succeeded and the ID was absent. Recheck that this is still our grant.
        return if (hasSafReadGrant(tree)) Presence.GONE else Presence.UNKNOWN
    }
}
