package com.knoux.ai.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/**
 * PC03 — real residue discovery.
 *
 * Three sources, three different levels of evidence:
 *
 *  - KNOUX's own cache: every file is disposable by Android's own cache contract.
 *    It is measured directly on the filesystem, bounded by an entry ceiling.
 *  - Granted SAF folders: every candidate is proven to exist and to be deletable by
 *    the provider, but its *ownership* is only a filename pattern. Those items are
 *    never pre-selected and never described as verified leftovers.
 *  - Shared media: read through the proven PC01 enumerator, kept only when the
 *    display name matches a residue pattern, and removable only through Android's
 *    own trash/delete dialog.
 *
 * No scope is ever scanned silently: the caller passes the exact scope set and tree
 * list it observed from the OS on this pass.
 */
class ResidueRepository(
    private val context: Context,
    private val permissions: PermissionState,
    private val scanner: SmartCleanRepository = SmartCleanRepository(context, permissions),
) {

    companion object {
        /** Files inspected inside KNOUX's own cache before the pass reports itself partial. */
        const val MAX_CACHE_ENTRIES: Int = 4_000

        /** Documents inspected across all granted folders per pass. */
        const val MAX_SAF_ENTRIES: Int = 3_000

        const val MAX_DEPTH: Int = 10

        /** A cache file bigger than this is real data by accident; still listed, never read. */
        const val MAX_LISTED_CACHE_BYTES: Long = 512L * 1024L * 1024L
    }

    data class Progress(
        val label: String,
        val filesInspected: Int,
        val residueFound: Int,
        val residueBytes: Long,
        val scopesDone: Int,
        val scopesTotal: Int,
    )

    /** Shared media can only be moved to the OS trash on API 30+. */
    fun trashCapability(): FeatureCapability =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            FeatureCapability.AVAILABLE
        } else {
            FeatureCapability.UNSUPPORTED
        }

    /**
     * Measures residue in [scopes], using exactly [trees] for the SAF scope.
     *
     * [minAgeDays] is applied to shared folders and media only. Own cache ignores
     * it: the folder is disposable by definition, and pretending a 2-hour-old cache
     * entry is "in use" would only produce a fake finding.
     */
    suspend fun scan(
        scopes: Set<AccessScope>,
        trees: List<Uri>,
        minAgeDays: Int,
        excludedUris: Set<String>,
        onProgress: (Progress) -> Unit = {},
    ): ResidueScanOutcome {
        val startedAt = System.currentTimeMillis()
        val sessionId = java.util.UUID.randomUUID().toString()
        val now = startedAt
        val requested = scopes.toSet()
        val items = mutableListOf<ResidueItem>()
        val snapshots = mutableListOf<SourceSnapshot>()
        val issues = linkedSetOf<String>()
        var filesInspected = 0

        fun publish(label: String) {
            onProgress(
                Progress(
                    label = label,
                    filesInspected = filesInspected,
                    residueFound = items.size,
                    residueBytes = items.sumOf { it.candidate.bytes },
                    scopesDone = snapshots.size,
                    scopesTotal = requested.size,
                )
            )
        }

        fun record(scope: AccessScope, provenance: DataProvenance, grant: PermissionGrant, found: List<ResidueItem>, status: DataStatus, detail: String?) {
            items += found
            snapshots += SourceSnapshot(
                scope = scope,
                provenance = provenance,
                grant = grant,
                itemCount = found.size,
                totalBytes = found.sumOf { it.candidate.bytes },
                capturedAtMillis = System.currentTimeMillis(),
                status = status,
                detail = detail,
            )
        }

        publish("Starting residue review")
        for (scope in requested) {
            currentCoroutineContext().ensureActive()
            when (scope) {
                AccessScope.APP_CACHE -> {
                    publish("Reading KNOUX cache")
                    val found = cacheItems(excludedUris, issues)
                    filesInspected += found.inspected
                    val residues = found.items
                    record(
                        scope, DataProvenance.APP_OWNED_CACHE, PermissionGrant.NOT_REQUIRED,
                        residues,
                        if (found.truncated) DataStatus.PARTIAL else DataStatus.MEASURED,
                        found.detail,
                    )
                }

                AccessScope.SAF_TREE -> {
                    publish("Reading granted folders")
                    if (trees.isEmpty()) {
                        record(
                            scope, DataProvenance.SAF_DOCUMENT, PermissionGrant.DENIED, emptyList(),
                            DataStatus.UNAVAILABLE, "No folder grant is currently accessible",
                        )
                        continue
                    }
                    val found = mutableListOf<ResidueItem>()
                    var readableTrees = 0
                    trees.forEach { tree ->
                        currentCoroutineContext().ensureActive()
                        val result = safItems(tree, minAgeDays, now, excludedUris, issues)
                        filesInspected += result.inspected
                        readableTrees += if (result.read) 1 else 0
                        found += result.items
                    }
                    record(
                        scope, DataProvenance.SAF_DOCUMENT,
                        if (readableTrees == 0) PermissionGrant.DENIED else PermissionGrant.GRANTED,
                        found,
                        if (readableTrees == trees.size) DataStatus.MEASURED else DataStatus.PARTIAL,
                        if (readableTrees == 0) "No granted folder could be read" else null,
                    )
                }

                AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO -> {
                    publish("Reading ${scope.name}")
                    val grant = permissions.mediaGrant(scope)
                    if (grant != PermissionGrant.GRANTED && grant != PermissionGrant.PARTIAL_SELECTED) {
                        record(
                            scope, DataProvenance.MEDIASTORE, grant, emptyList(),
                            DataStatus.UNAVAILABLE, "No permission for ${scope.name}",
                        )
                        continue
                    }
                    val (mediaSession, mediaCandidates) = scanner.scan(setOf(scope), excludedUris)
                    filesInspected += mediaCandidates.size
                    val matched = mediaCandidates.mapNotNull { candidate ->
                        ResidueRules.detect(candidate, minAgeDays, now)
                    }
                    val latestGrant = permissions.mediaGrant(scope)
                    record(
                        scope, DataProvenance.MEDIASTORE, latestGrant, matched,
                        if (latestGrant == PermissionGrant.GRANTED && mediaSession.status == ScanStatus.COMPLETED) {
                            DataStatus.MEASURED
                        } else {
                            DataStatus.PARTIAL
                        },
                        mediaSession.snapshots.firstOrNull()?.detail,
                    )
                }
            }
        }

        val unique = ResidueRules.distinctPhysicalItems(items)
        if (unique.size != items.size) {
            issues += "${items.size - unique.size} alias(es) of the same physical file were combined"
        }
        val status = when {
            snapshots.any { it.status == DataStatus.UNAVAILABLE } -> DataStatus.PARTIAL
            issues.isNotEmpty() -> DataStatus.PARTIAL
            else -> DataStatus.MEASURED
        }
        val session = ScanSession(
            id = sessionId,
            startedAtMillis = startedAt,
            finishedAtMillis = System.currentTimeMillis(),
            status = if (status == DataStatus.MEASURED) ScanStatus.COMPLETED else ScanStatus.PARTIAL,
            scopes = requested,
            itemCount = unique.size,
            totalBytes = unique.sumOf { it.candidate.bytes },
            snapshots = snapshots,
        )
        return ResidueScanOutcome(
            session = session,
            items = unique,
            filesInspected = filesInspected,
            status = status,
            note = issues.takeIf { it.isNotEmpty() }?.joinToString("; "),
        )
    }

    // ── Own cache ────────────────────────────────────────────────────────────

    private class CacheDiscovery(
        val items: List<ResidueItem>,
        val inspected: Int,
        val truncated: Boolean,
        val detail: String?,
    )

    private fun cacheItems(excludedUris: Set<String>, issues: MutableSet<String>): CacheDiscovery {
        val roots = cacheRoots()
        val out = linkedMapOf<String, ResidueItem>()
        var inspected = 0
        var truncated = false
        val stack = ArrayDeque<Pair<File, Int>>()
        roots.forEach { root -> stack.addLast(root to 0) }
        while (stack.isNotEmpty()) {
            if (inspected >= MAX_CACHE_ENTRIES) {
                truncated = true
                issues += "Own-cache entry ceiling reached ($MAX_CACHE_ENTRIES); listing is incomplete"
                break
            }
            val (directory, depth) = stack.removeLast()
            if (depth > MAX_DEPTH) {
                issues += "Own-cache depth ceiling reached ($MAX_DEPTH)"
                continue
            }
            val children = try {
                directory.listFiles()
            } catch (_: Throwable) {
                issues += "A cache directory could not be listed"
                continue
            } ?: continue
            children.forEach { child ->
                if (inspected >= MAX_CACHE_ENTRIES) return@forEach
                if (isSymlink(child)) return@forEach
                if (child.isDirectory) {
                    stack.addLast(child to depth + 1)
                    return@forEach
                }
                if (!child.isFile) return@forEach
                inspected++
                val uri = Uri.fromFile(child).toString()
                if (uri in excludedUris) return@forEach
                val length = try {
                    child.length()
                } catch (_: Throwable) {
                    0L
                }
                if (length > MAX_LISTED_CACHE_BYTES) {
                    issues += "1 cache file larger than 512 MB was listed but not read"
                }
                val candidate = FileCandidate(
                    uri = uri,
                    displayName = child.name,
                    mimeType = null,
                    bytes = length,
                    lastModifiedMillis = child.lastModified(),
                    category = candidateCategory(child.name),
                    provenance = DataProvenance.APP_OWNED_CACHE,
                    eligibility = Eligibility.ELIGIBLE,
                    reason = "Inside KNOUX's own cache",
                    physicalKey = "file:" + runCatching { child.canonicalPath }.getOrDefault(child.absolutePath),
                )
                val item = ResidueRules.detect(candidate) ?: return@forEach
                out[uri] = item
            }
        }
        return CacheDiscovery(
            items = out.values.toList(),
            inspected = inspected,
            truncated = truncated,
            detail = if (truncated) "Own-cache listing incomplete" else null,
        )
    }

    private fun candidateCategory(name: String): CandidateCategory {
        val lower = name.lowercase()
        return when {
            lower.startsWith("thumb") || lower.contains(".thumb") -> CandidateCategory.THUMBNAIL
            lower.endsWith(".log") || lower.endsWith(".logcat") -> CandidateCategory.TEMP_FILE
            else -> CandidateCategory.APP_CACHE
        }
    }

    private fun cacheRoots(): List<File> = try {
        buildList {
            add(context.cacheDir)
            add(context.codeCacheDir)
            context.externalCacheDir?.let { add(it) }
        }.filter { it.isDirectory }
    } catch (_: Throwable) {
        emptyList()
    }

    private fun isSymlink(file: File): Boolean = try {
        val parent = file.canonicalFile.parentFile?.canonicalPath ?: return true
        val canonical = file.canonicalPath
        canonical != File(parent, file.name).path
    } catch (_: Throwable) {
        true
    }

    // ── Granted folders ─────────────────────────────────────────────────────

    private class SafDiscovery(
        val items: List<ResidueItem>,
        val inspected: Int,
        val read: Boolean,
    )

    /**
     * Walks one granted tree depth-first through the provider.
     *
     * The provider's own flags decide deletion eligibility. A document whose flags
     * omit `FLAG_SUPPORTS_DELETE` is listed as unselectable instead of being offered
     * for an action that would fail.
     */
    private suspend fun safItems(
        tree: Uri,
        minAgeDays: Int,
        now: Long,
        excludedUris: Set<String>,
        issues: MutableSet<String>,
    ): SafDiscovery {
        val out = linkedMapOf<String, ResidueItem>()
        var inspected = 0
        val rootId = try {
            DocumentsContract.getTreeDocumentId(tree)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Exception) {
            issues += "Invalid SAF tree: ${t.javaClass.simpleName}"
            return SafDiscovery(emptyList(), 0, false)
        }
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
        )
        val queue = ArrayDeque<Pair<String, Int>>()
        val visited = hashSetOf<String>()
        queue.addLast(rootId to 0)
        var readable = false
        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            if (inspected >= MAX_SAF_ENTRIES) {
                issues += "Granted-folder entry ceiling reached ($MAX_SAF_ENTRIES); traversal incomplete"
                break
            }
            val (directoryId, depth) = queue.removeFirst()
            if (!visited.add(directoryId)) continue
            try {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, directoryId)
                val cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
                if (cursor == null) {
                    issues += "A folder provider returned no child cursor"
                    continue
                }
                readable = true
                cursor.use {
                    val idCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val dateCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val flagsCol = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)
                    while (it.moveToNext()) {
                        currentCoroutineContext().ensureActive()
                        if (inspected >= MAX_SAF_ENTRIES) break
                        inspected++
                        val id = it.getString(idCol) ?: continue
                        val name = it.getString(nameCol) ?: continue
                        val mime = it.getString(mimeCol) ?: continue
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (depth >= MAX_DEPTH) {
                                issues += "Granted-folder depth ceiling reached ($MAX_DEPTH)"
                            } else {
                                queue.addLast(id to depth + 1)
                            }
                            continue
                        }
                        val match = ResidueRules.classify(name, mime) ?: continue
                        if (it.isNull(sizeCol)) {
                            issues += "A document size was omitted by its provider"
                            continue
                        }
                        val length = it.getLong(sizeCol)
                        if (length <= 0L) continue
                        val modified = if (it.isNull(dateCol)) 0L else it.getLong(dateCol)
                        if (modified <= 0L) continue
                        val ageDays = (now - modified) / ResidueRules.DAY_MILLIS
                        if (ageDays < minAgeDays) continue
                        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString()
                        if (uri in excludedUris || uri in out) continue
                        val deletable = !it.isNull(flagsCol) &&
                            (it.getInt(flagsCol) and DocumentsContract.Document.FLAG_SUPPORTS_DELETE) != 0
                        val candidate = FileCandidate(
                            uri = uri,
                            displayName = name,
                            mimeType = mime,
                            bytes = length,
                            lastModifiedMillis = modified,
                            category = CandidateCategory.SAF_FILE,
                            provenance = DataProvenance.SAF_DOCUMENT,
                            eligibility = if (deletable) Eligibility.ELIGIBLE else Eligibility.UNSELECTABLE,
                            reason = if (deletable) {
                                "Inside a folder you granted; provider allows deletion"
                            } else {
                                "This provider does not advertise deletion of this document"
                            },
                            physicalKey = safPhysicalKey(uri),
                        )
                        val item = ResidueRules.detect(candidate, minAgeDays, now) ?: continue
                        out[uri] = item
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Exception) {
                issues += "A folder query failed: ${t.javaClass.simpleName}"
            }
        }
        return SafDiscovery(out.values.toList(), inspected, readable)
    }

    private fun safPhysicalKey(uri: String): String? = try {
        val parsed = Uri.parse(uri)
        val authority = parsed.authority?.lowercase() ?: return null
        val documentId = DocumentsContract.getDocumentId(parsed)
        val volume = when (authority) {
            "com.android.externalstorage.documents" -> {
                val raw = documentId.substringBefore(':', "")
                when (raw.lowercase()) {
                    "primary" -> "emulated/0"
                    "" -> ""
                    else -> raw.lowercase()
                }
            }
            else -> authority
        }
        "$volume:${documentId.substringAfter(':', documentId).lowercase()}"
    } catch (_: Throwable) {
        null
    }
}

/** Measured result of one residue pass. */
data class ResidueScanOutcome(
    val session: ScanSession,
    val items: List<ResidueItem>,
    val filesInspected: Int,
    val status: DataStatus,
    val note: String?,
)
