package com.knoux.ai.data

import android.content.Context
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * PC02 — real duplicate discovery.
 *
 * The expensive, dangerous part of duplicate cleanup is deciding that two files are
 * the same. This engine never guesses:
 *
 *  1. [SmartCleanRepository] enumerates only scopes the customer actually granted
 *     and returns the provider's own eligibility for each file.
 *  2. Files are bucketed by exact byte length. A bucket with a single member can
 *     never be a duplicate, so most files cost one integer comparison.
 *  3. Every member of a multi-file bucket is read completely and hashed with
 *     SHA-256. Only identical full-content hashes form a group.
 *
 * Deliberate limits are reported, never hidden: a per-file ceiling, a total read
 * budget and a file-count ceiling keep a pathological library from pinning the
 * device, and every skipped file is counted in the scan note.
 */
class DuplicateRepository(
    private val context: Context,
    private val permissions: PermissionState,
    private val scanner: SmartCleanRepository = SmartCleanRepository(context, permissions),
) {

    companion object {
        /** Larger files are listed as skipped instead of silently hashed for minutes. */
        const val MAX_FILE_BYTES: Long = 512L * 1024L * 1024L

        /** Total bytes this pass may read before it reports an incomplete scan. */
        const val MAX_TOTAL_HASH_BYTES: Long = 2L * 1024L * 1024L * 1024L

        const val MAX_FILES_HASHED: Int = 800

        private const val BUFFER_BYTES = 64 * 1024
        private const val YIELD_EVERY_BYTES = 4L * 1024L * 1024L
    }

    /** Shared media can only be trashed through the OS on API 30+. */
    fun trashCapability(): FeatureCapability =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) FeatureCapability.AVAILABLE
        else FeatureCapability.UNSUPPORTED

    data class Progress(
        val label: String,
        val filesInspected: Int,
        val filesHashed: Int,
        val bytesHashed: Long,
        val groupsFound: Int,
        val redundantBytes: Long,
        val scopesDone: Int,
        val scopesTotal: Int,
    )

    /**
     * Scans the granted scopes and returns groups whose equality was proven by a
     * full content hash. [onProgress] runs on the caller's dispatcher and may be
     * invoked for every hashed file.
     */
    suspend fun scan(
        scopes: Set<AccessScope>,
        excludedUris: Set<String>,
        onProgress: (Progress) -> Unit = {},
    ): DuplicateScanOutcome {
        val (session, files) = scanner.scan(scopes, excludedUris) { progress ->
            onProgress(
                Progress(
                    label = progress.label,
                    filesInspected = progress.itemsFound,
                    filesHashed = 0,
                    bytesHashed = 0L,
                    groupsFound = 0,
                    redundantBytes = 0L,
                    scopesDone = progress.scopesDone,
                    scopesTotal = progress.scopesTotal,
                )
            )
        }

        val issues = linkedSetOf<String>()
        val notes = mutableListOf<String>()
        // One physical file may arrive through both MediaStore and a granted SAF
        // tree. Collapse those aliases before any grouping so a "duplicate" can
        // never be the same file as its own protected original.
        val uniqueFiles = DuplicateRules.distinctPhysicalFiles(files)
        if (uniqueFiles.size != files.size) {
            notes += "${files.size - uniqueFiles.size} alias(es) of the same physical file were combined"
        }
        val buckets = uniqueFiles
            .filter { it.bytes >= DuplicateRules.MIN_BYTES }
            .groupBy { it.bytes }
            .filter { it.value.size >= 2 }
        // Hash the biggest buckets first: they hold the largest recoverable copies.
        val queue = buckets.entries
            .sortedByDescending { it.key }
            .flatMap { entry -> entry.value.map { entry.key to it } }

        val hashed = mutableListOf<HashedFile>()
        var bytesHashed = 0L
        var skippedLarge = 0
        var unreadable = 0
        var budgetHit = false

        for ((size, member) in queue) {
            currentCoroutineContext().ensureActive()
            if (hashed.size >= MAX_FILES_HASHED || bytesHashed + size > MAX_TOTAL_HASH_BYTES) {
                budgetHit = true
                break
            }
            if (size > MAX_FILE_BYTES) {
                skippedLarge++
                continue
            }
            val digest = try {
                sha256(member)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                unreadable++
                continue
            }
            hashed += HashedFile(member, digest)
            bytesHashed += size
            val provisional = DuplicateRules.summarize(hashed)
            onProgress(
                Progress(
                    label = "Hashing content",
                    filesInspected = uniqueFiles.size,
                    filesHashed = hashed.size,
                    bytesHashed = bytesHashed,
                    groupsFound = provisional.groups,
                    redundantBytes = provisional.redundantBytes,
                    scopesDone = session.snapshots.size,
                    scopesTotal = scopes.size,
                )
            )
        }

        if (budgetHit) {
            issues += "Hash budget reached after ${hashed.size} file(s); remaining same-size files were not verified"
        }
        if (skippedLarge > 0) {
            issues += "$skippedLarge file(s) larger than ${MAX_FILE_BYTES / (1024L * 1024L)} MB were not hashed"
        }
        if (unreadable > 0) {
            issues += "$unreadable file(s) could not be read and were excluded"
        }

        val groups = DuplicateRules.group(hashed)
        return DuplicateScanOutcome(
            session = session,
            groups = groups,
            fileCount = uniqueFiles.size,
            hashedCount = hashed.size,
            bytesHashed = bytesHashed,
            status = when {
                session.status != ScanStatus.COMPLETED -> DataStatus.PARTIAL
                issues.isNotEmpty() -> DataStatus.PARTIAL
                else -> DataStatus.MEASURED
            },
            note = (issues + notes).takeIf { it.isNotEmpty() }?.joinToString("; "),
        )
    }

    /** Reads the complete stream; a partial or sampled hash is never returned. */
    private suspend fun sha256(candidate: FileCandidate): String {
        openStream(candidate).use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(BUFFER_BYTES)
            var carry = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                digest.update(buffer, 0, read)
                carry += read
                if (carry >= YIELD_EVERY_BYTES) {
                    carry = 0L
                    currentCoroutineContext().ensureActive()
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    private fun openStream(candidate: FileCandidate): InputStream {
        val uri = Uri.parse(candidate.uri)
        return when (candidate.provenance) {
            DataProvenance.APP_OWNED_CACHE -> {
                require(uri.scheme == "file") { "App cache candidate is not a file URI" }
                val path = uri.path ?: error("App cache candidate has no path")
                val file = File(path)
                val canonical = file.canonicalPath
                // Same sandbox rule as deletion: never read through an escaped path.
                require(cacheRoots().any { canonical.startsWith(it + File.separator) }) {
                    "Refusing to hash a file outside KNOUX cache"
                }
                require(file.isFile) { "App cache candidate is missing" }
                FileInputStream(file)
            }
            DataProvenance.MEDIASTORE, DataProvenance.SAF_DOCUMENT -> {
                require(uri.scheme == "content") { "Provider candidate is not a content URI" }
                context.contentResolver.openInputStream(uri)
                    ?: error("Provider opened no stream for ${candidate.displayName}")
            }
        }
    }

    private fun cacheRoots(): List<String> = try {
        buildList {
            add(context.cacheDir)
            add(context.codeCacheDir)
            context.externalCacheDir?.let { add(it) }
        }.map { it.canonicalPath }
    } catch (_: Throwable) {
        emptyList()
    }
}

/** Measured result of one duplicate pass. */
data class DuplicateScanOutcome(
    val session: ScanSession,
    val groups: List<DuplicateGroup>,
    val fileCount: Int,
    val hashedCount: Int,
    val bytesHashed: Long,
    val status: DataStatus,
    val note: String?,
)
