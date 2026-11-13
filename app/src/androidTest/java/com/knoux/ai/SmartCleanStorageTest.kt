package com.knoux.ai

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.CandidateCategory
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.DataStatus
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.FileCandidate
import com.knoux.ai.data.KnouxDatabase
import com.knoux.ai.data.OperationEntity
import com.knoux.ai.data.OperationItemEntity
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.OperationPlan
import com.knoux.ai.data.OperationStatus
import com.knoux.ai.data.PermissionState
import com.knoux.ai.data.ScanOutcome
import com.knoux.ai.data.SettingsStore
import com.knoux.ai.data.SmartCleanController
import com.knoux.ai.data.SmartCleanRepository
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNoException
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Real repository/controller tests. Room is private and in-memory; settings belong
 * to the instrumentation APK. Files are newly created in the target app sandbox.
 * Run only on an explicitly verified emulator; no app/Room data is cleared.
 */
@RunWith(AndroidJUnit4::class)
class SmartCleanStorageTest {
    private companion object {
        const val TAG = "KNOUX_MEDIA_QA"
    }

    private lateinit var context: Context
    private lateinit var database: KnouxDatabase
    private lateinit var settings: SettingsStore
    private lateinit var permissions: PermissionState
    private lateinit var repository: SmartCleanRepository
    private lateinit var controller: SmartCleanController
    private lateinit var cacheDirectory: File
    private lateinit var filesDirectory: File
    private val fixtures = mutableListOf<File>()
    private val prefix = "knoux_storage_${UUID.randomUUID()}_"
    private var originalScanAt: Long? = null
    private val mediaFixtures = mutableListOf<MediaFixture>()
    private var testDataStoreDir: File? = null

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    @Before fun createIsolatedDependencies() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        context = instrumentation.targetContext
        assertEquals("com.knoux.ai", context.packageName)
        database = Room.inMemoryDatabaseBuilder(context, KnouxDatabase::class.java).build()
        // Use PreferenceDataStoreFactory with a fresh UUID-named file in the target
        // app's filesDir so the test runner always has write access.
        val testDataStoreDir = File(context.filesDir, "knoux_test_ds_${UUID.randomUUID()}")
        testDataStoreDir.mkdirs()
        this.testDataStoreDir = testDataStoreDir
        val testDataStoreFile = File(testDataStoreDir, "settings.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create { testDataStoreFile }
        settings = SettingsStore(testDataStore)
        originalScanAt = io { settings.lastScanAt.first() }
        permissions = PermissionState(context)
        repository = SmartCleanRepository(context, permissions)
        controller = freshController()
        cacheDirectory = File(context.cacheDir, "KNOUX_QA_${UUID.randomUUID()}")
        filesDirectory = File(context.filesDir, "KNOUX_QA_${UUID.randomUUID()}")
        assertTrue(cacheDirectory.mkdir())
        assertTrue(filesDirectory.mkdir())
    }

    private fun freshController() = SmartCleanController(repository, database, settings, permissions)

    @After fun deleteOnlyExactFixturesAndCloseIsolatedDatabase() {
        try {
            mediaFixtures.forEach { fixture ->
                // Never walk Pictures or delete a collection. Even teardown revalidates
                // the exact returned URI, owner, relative path and display name.
                val row = mediaRow(fixture)
                if (row != null) {
                    assertMediaIdentity(fixture, row)
                    assertEquals(1, context.contentResolver.delete(fixture.uri, null, null))
                    assertNull("exact fixture row must be gone", mediaRow(fixture))
                }
            }
            fixtures.forEach { file ->
                val parent = file.canonicalFile.parentFile?.canonicalPath
                assertTrue(parent == cacheDirectory.canonicalPath || parent == filesDirectory.canonicalPath)
                assertTrue("failed to remove exact fixture ${file.name}", !file.exists() || file.delete())
            }
            // Nonrecursive: an unexpected file is preserved and makes teardown fail.
            if (::cacheDirectory.isInitialized) assertTrue(!cacheDirectory.exists() || cacheDirectory.delete())
            if (::filesDirectory.isInitialized) assertTrue(!filesDirectory.exists() || filesDirectory.delete())
        } finally {
            try {
                originalScanAt?.let { stamp -> io { settings.recordScanAt(stamp) } }
            } finally {
                if (::database.isInitialized) database.close()
                testDataStoreDir?.deleteRecursively()
            }
        }
    }

    private fun seed(name: String, bytes: Int = 4096, outsideCache: Boolean = false): File {
        val parent = if (outsideCache) filesDirectory else cacheDirectory
        val file = File(parent, prefix + name)
        assertTrue("never overwrite a fixture", file.createNewFile())
        fixtures += file
        file.writeBytes(ByteArray(bytes) { (it % 23).toByte() })
        assertEquals(parent.canonicalPath, file.canonicalFile.parentFile?.canonicalPath)
        assertEquals(bytes.toLong(), file.length())
        return file
    }

    private fun candidate(file: File) = FileCandidate(
        uri = Uri.fromFile(file).toString(),
        displayName = file.name,
        mimeType = null,
        bytes = file.length(),
        lastModifiedMillis = file.lastModified(),
        category = CandidateCategory.TEMP_FILE,
        provenance = DataProvenance.APP_OWNED_CACHE,
        eligibility = Eligibility.ELIGIBLE,
        reason = "Fresh instrumentation fixture"
    )

    private fun plan(
        candidates: List<FileCandidate>,
        confirmed: Boolean = true,
        kind: OperationKind = OperationKind.PERMANENT_DELETE
    ) = OperationPlan(
        id = UUID.randomUUID().toString(),
        kind = kind,
        candidates = candidates,
        requiresSystemConfirmation = candidates.any { it.provenance == DataProvenance.MEDIASTORE },
        confirmedByUser = confirmed
    )

    /** All scans may contain unrelated cache rows; plans may contain only exact fixture rows. */
    private fun assertExactCacheScope(outcome: ScanOutcome, expected: List<File>): List<FileCandidate> {
        assertEquals(setOf(AccessScope.APP_CACHE), outcome.session.scopes)
        val rows = outcome.candidates.filter { it.displayName.startsWith(prefix) }
        assertEquals(expected.size, rows.size)
        assertEquals(expected.map { it.name }.toSet(), rows.map { it.displayName }.toSet())
        assertEquals(expected.map { Uri.fromFile(it).toString() }.toSet(), rows.map { it.uri }.toSet())
        rows.forEach { row ->
            val file = expected.single { Uri.fromFile(it).toString() == row.uri }
            assertEquals(cacheDirectory.canonicalPath, file.canonicalFile.parentFile?.canonicalPath)
            assertEquals(DataProvenance.APP_OWNED_CACHE, row.provenance)
            assertEquals(DataStatus.MEASURED, row.status)
            assertEquals(Eligibility.ELIGIBLE, row.eligibility)
            assertEquals(file.length(), row.bytes)
            assertTrue(file.isFile)
        }
        return rows
    }

    @Test fun actualCacheScanDeletionAndHistoryUseOnlyExactFreshUris() = io {
        val files = listOf(seed("alpha.tmp", 4096), seed("bravo.tmp", 8192), seed("charlie.tmp", 2048))
        val scan = controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow()
        val items = assertExactCacheScope(scan, files)
        assertEquals(scan.session.itemCount, database.scanDao().candidates(scan.session.id).size)
        assertEquals(scan.session.totalBytes, database.scanDao().candidates(scan.session.id).sumOf { it.bytes })
        val operation = plan(items)
        val result = controller.executeLocal(operation, acknowledgedIrreversible = true)
        assertEquals(OperationStatus.SUCCEEDED, result.status)
        assertEquals(files.size, result.removedCount)
        assertEquals(0, result.failedCount)
        assertTrue(files.none { it.exists() })
        assertEquals(items.map { it.uri }.toSet(), controller.verifiedRemovedUris(operation.id).toSet())
        assertTrue(repository.verifyGone(items).values.all { it })
        val history = withTimeout(10_000) {
            controller.observeHistory().first { entries -> entries.any { it.id == operation.id } }
        }.single { it.id == operation.id }
        assertEquals(OperationStatus.SUCCEEDED, history.status)
        assertEquals(OperationKind.PERMANENT_DELETE, history.kind)
        assertEquals(3, history.itemCount)
        assertEquals(14_336L, history.bytes)
        val rows = database.operationDao().items(operation.id)
        assertEquals(items.size, rows.size)
        assertEquals(items.map { it.uri }.toSet(), rows.map { it.uri }.toSet())
        assertTrue(rows.all { it.outcome == "REMOVED_VERIFIED" })
        val retained = freshController().latestScan()!!
        assertFalse(retained.candidates.any { it.uri in items.map { item -> item.uri } })
        assertEquals(retained.candidates.size, retained.session.itemCount)
        assertEquals(retained.candidates.sumOf { it.bytes }, retained.session.totalBytes)
    }

    @Test fun concurrentSamePlanHasExactlyOneAtomicDurableClaim() = io {
        val files = listOf(seed("atomic-alpha.tmp"), seed("atomic-bravo.tmp"))
        val items = assertExactCacheScope(controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow(), files)
        val operation = plan(items)
        val otherController = freshController()
        val results = coroutineScope {
            val ready = Channel<Unit>(Channel.UNLIMITED)
            val start = CompletableDeferred<Unit>()
            val callers = (1..8).map { caller ->
                async(Dispatchers.IO) {
                    ready.send(Unit)
                    start.await()
                    (if (caller % 2 == 0) controller else otherController)
                        .executeLocal(operation, acknowledgedIrreversible = true)
                }
            }
            repeat(callers.size) { ready.receive() }
            start.complete(Unit)
            callers.awaitAll().also { ready.close() }
        }
        assertEquals(1, results.count { it.status == OperationStatus.SUCCEEDED })
        assertEquals(files.size, results.sumOf { it.removedCount })
        assertTrue(results.filter { it.status != OperationStatus.SUCCEEDED }.all { it.removedCount == 0 })
        assertTrue(files.none { it.exists() })
        assertEquals(1, database.operationDao().operationCount(operation.id))
        assertEquals(OperationStatus.SUCCEEDED.name, database.operationDao().operation(operation.id)!!.status)
        assertEquals(items.size, database.operationDao().items(operation.id).size)
        assertEquals(items.map { it.uri }.toSet(), controller.verifiedRemovedUris(operation.id).toSet())
    }

    @Test fun staleUriIsAlreadyGoneAndDoesNotInflateRemovedCountOrBytes() = io {
        val stale = seed("stale.tmp", 8192)
        val live = seed("live.tmp", 2048)
        val items = assertExactCacheScope(controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow(), listOf(stale, live))
        // The test, not the controller, removes this exact fresh file between scan and action.
        assertTrue(stale.delete())
        assertFalse(stale.exists())
        val operation = plan(items)
        val result = controller.executeLocal(operation, acknowledgedIrreversible = true)
        assertEquals(1, result.removedCount)
        assertFalse(live.exists())
        val rows = database.operationDao().items(operation.id)
        assertEquals(2, rows.size)
        assertEquals("ALREADY_GONE", rows.single { it.uri == Uri.fromFile(stale).toString() }.outcome)
        assertEquals("REMOVED_VERIFIED", rows.single { it.uri == Uri.fromFile(live).toString() }.outcome)
        val history = controller.observeHistory().first().single { it.id == operation.id }
        assertEquals(1, history.itemCount)
        assertEquals(2048L, history.bytes)
        assertEquals(setOf(Uri.fromFile(live).toString()), controller.verifiedRemovedUris(operation.id).toSet())
        val replay = controller.executeLocal(operation, acknowledgedIrreversible = true)
        assertEquals(0, replay.removedCount)
        assertEquals(2, database.operationDao().items(operation.id).size)
    }

    @Test fun unconfirmedAndUnacknowledgedPlansNeverDeleteAnyFixture() = io {
        val files = listOf(seed("unconfirmed.tmp"), seed("no-ack.tmp"))
        val items = assertExactCacheScope(controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow(), files)
        val unconfirmed = plan(items, confirmed = false)
        val noAck = plan(items)
        assertEquals(0, controller.executeLocal(unconfirmed, acknowledgedIrreversible = true).removedCount)
        assertTrue(files.all { it.isFile && it.length() == 4096L })
        assertEquals(0, controller.executeLocal(noAck, acknowledgedIrreversible = false).removedCount)
        assertTrue(files.all { it.isFile && it.length() == 4096L })
        assertTrue(controller.verifiedRemovedUris(unconfirmed.id).isEmpty())
        assertTrue(controller.verifiedRemovedUris(noAck.id).isEmpty())
        assertTrue(database.operationDao().items(unconfirmed.id).none { it.outcome == "REMOVED_VERIFIED" })
        assertTrue(database.operationDao().items(noAck.id).none { it.outcome == "REMOVED_VERIFIED" })
    }

    @Test fun persistedKeepExclusionBlocksEvenAPreviouslyPreparedPlanUntilRemoved() = io {
        val kept = seed("keep.tmp")
        val items = assertExactCacheScope(controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow(), listOf(kept))
        val stalePlan = plan(items)
        controller.keepForever(items.single())
        val exclusions = withTimeout(10_000) {
            freshController().observeExclusions().first { rows -> rows.any { it.uri == items.single().uri } }
        }
        assertEquals(1, exclusions.count { it.uri == items.single().uri })
        assertEquals(0, controller.executeLocal(stalePlan, acknowledgedIrreversible = true).removedCount)
        assertTrue(kept.isFile && kept.length() == 4096L)
        assertTrue(controller.verifiedRemovedUris(stalePlan.id).isEmpty())
        assertExactCacheScope(controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow(), emptyList())
        freshController().removeExclusion(items.single().uri)
        withTimeout(10_000) {
            controller.observeExclusions().first { rows -> rows.none { it.uri == items.single().uri } }
        }
        val offered = assertExactCacheScope(controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow(), listOf(kept))
        assertEquals(1, controller.executeLocal(plan(offered), acknowledgedIrreversible = true).removedCount)
        assertFalse(kept.exists())
    }

    @Test fun freshControllerRehydratesPersistedLatestScanAndExactHistory() = io {
        val file = seed("persisted.tmp")
        val first = controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow()
        assertExactCacheScope(first, listOf(file))
        val rehydrated = freshController().latestScan()!!
        assertEquals(first.session.id, rehydrated.session.id)
        assertEquals(first.session.scopes, rehydrated.session.scopes)
        assertEquals(first.session.itemCount, rehydrated.session.itemCount)
        assertEquals(first.session.totalBytes, rehydrated.session.totalBytes)
        assertEquals(first.candidates.associateBy { it.uri }, rehydrated.candidates.associateBy { it.uri })
        assertEquals(first.session.snapshots.toSet(), rehydrated.session.snapshots.toSet())
        val second = controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow()
        assertFalse(first.session.id == second.session.id)
        val latest = database.scanDao().latestFinishedSession()!!
        assertEquals(latest.id, freshController().latestScan()!!.session.id)
        val operation = plan(assertExactCacheScope(second, listOf(file)))
        controller.executeLocal(operation, acknowledgedIrreversible = true)
        val row = database.operationDao().operation(operation.id)!!
        val history = freshController().observeHistory().first().single { it.id == operation.id }
        assertEquals(row.kind, history.kind.name)
        assertEquals(row.status, history.status.name)
        assertEquals(row.itemCount, history.itemCount)
        assertEquals(row.bytes, history.bytes)
        assertEquals(row.note, history.note)
        assertEquals(row.completedAt, history.completedAtMillis)
    }

    @Test fun forgedCacheCandidateInTargetFilesDirIsRefusedWithoutTouchingIt() = io {
        val outside = seed("outside-cache.tmp", outsideCache = true)
        val forged = candidate(outside)
        assertEquals(filesDirectory.canonicalPath, outside.canonicalFile.parentFile?.canonicalPath)
        assertFalse(outside.canonicalPath.startsWith(context.cacheDir.canonicalPath + File.separator))
        assertExactCacheScope(controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow(), emptyList())
        val operation = plan(listOf(forged))
        val result = controller.executeLocal(operation, acknowledgedIrreversible = true)
        assertEquals(0, result.removedCount)
        assertEquals(OperationStatus.FAILED, result.status)
        assertTrue(outside.isFile && outside.length() == 4096L)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(controller.verifiedRemovedUris(operation.id).isEmpty())
        assertFalse(repository.verifyGone(listOf(forged))[forged.uri] == true)
        assertTrue(repository.deleteAppOwnedCache(listOf(forged)).single().second.isFailure)
        assertTrue(outside.isFile && outside.length() == 4096L)
    }

    @Test fun interruptedRecoveryNeverDeletesAndPreservesTheExceptedPendingPlan() = io {
        val file = seed("interrupted.tmp")
        val item = assertExactCacheScope(controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow(), listOf(file)).single()
        val recoverId = UUID.randomUUID().toString()
        val exceptId = UUID.randomUUID().toString()
        listOf(recoverId to OperationStatus.RUNNING, exceptId to OperationStatus.AWAITING_USER_CONFIRMATION).forEach { (id, status) ->
            database.operationDao().claimOperation(OperationEntity(
                id = id, kind = OperationKind.PERMANENT_DELETE.name, status = status.name,
                startedAt = System.currentTimeMillis(), completedAt = null, itemCount = 0, bytes = 0L,
                volumeFreeBefore = null, volumeFreeAfter = null, note = "Synthetic interrupted fixture"
            ))
            database.operationDao().insertItems(listOf(OperationItemEntity(
                operationId = id, uri = item.uri, displayName = item.displayName,
                bytes = item.bytes, outcome = "PENDING", error = null
            )))
        }
        freshController().recoverInterruptedOperations(exceptPlanId = exceptId)
        assertTrue(file.isFile && file.length() == 4096L)
        assertEquals(OperationStatus.AWAITING_USER_CONFIRMATION.name, database.operationDao().operation(exceptId)!!.status)
        assertFalse(database.operationDao().operation(recoverId)!!.status in setOf(
            OperationStatus.RUNNING.name, OperationStatus.AWAITING_USER_CONFIRMATION.name
        ))
        assertTrue(controller.verifiedRemovedUris(recoverId).isEmpty())
        freshController().recoverInterruptedOperations()
        assertTrue(file.isFile && file.length() == 4096L)
        assertFalse(database.operationDao().operation(exceptId)!!.status == OperationStatus.AWAITING_USER_CONFIRMATION.name)
        assertTrue(controller.verifiedRemovedUris(exceptId).isEmpty())
    }

    private data class MediaFixture(val uri: Uri, val name: String, val relativePath: String)
    private data class MediaRow(val owner: String?, val name: String, val relativePath: String?, val bytes: Long, val trashed: Boolean)

    private fun mediaRow(fixture: MediaFixture): MediaRow? {
        val projection = arrayOf(
            MediaStore.MediaColumns.OWNER_PACKAGE_NAME, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.IS_TRASHED
        )
        val args = Bundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
            }
        }
        val cursor = context.contentResolver.query(fixture.uri, projection, args, null)
            ?: throw UnsupportedOperationException("Media provider returned no cursor for exact fixture URI")
        return cursor.use {
            if (!it.moveToFirst()) return@use null
            assertEquals("exact URI must identify exactly one row", 1, it.count)
            MediaRow(
                owner = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)),
                name = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)),
                relativePath = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)),
                bytes = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                trashed = it.getInt(it.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_TRASHED)) == 1
            )
        }
    }

    private fun assertMediaIdentity(fixture: MediaFixture, row: MediaRow) {
        assertTrue(mediaFixtures.any { it == fixture })
        assertEquals("content", fixture.uri.scheme)
        assertEquals("media", fixture.uri.authority)
        assertTrue(ContentUris.parseId(fixture.uri) > 0L)
        assertEquals("com.knoux.ai", row.owner)
        assertEquals(fixture.relativePath, row.relativePath)
        assertEquals(fixture.name, row.name)
    }

    private fun seedPngFixtures(): List<FileCandidate> {
        assumeTrue("Synthetic MediaStore insertion requires API 29+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        assumeTrue("OS trash/restore/delete requests require API 30+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val path = "Pictures/KNOUX_QA_${UUID.randomUUID()}/"
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        try {
            listOf("alpha.png", "bravo.png").forEach { suffix ->
                val name = prefix + suffix
                val uri = context.contentResolver.insert(collection, ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, path)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }) ?: throw UnsupportedOperationException("Media provider cannot insert synthetic PNG")
                val fixture = MediaFixture(uri, name, path)
                mediaFixtures += fixture
                assertMediaIdentity(fixture, mediaRow(fixture)!!)
                val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(android.graphics.Color.MAGENTA)
                    val stream = context.contentResolver.openOutputStream(uri)
                        ?: throw UnsupportedOperationException("Media provider cannot write synthetic PNG")
                    stream.use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                } finally {
                    bitmap.recycle()
                }
                assertEquals(1, context.contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null))
            }
            return mediaFixtures.map { fixture ->
                val row = mediaRow(fixture)!!
                assertMediaIdentity(fixture, row)
                assertTrue("valid PNG must have bytes", row.bytes > 0)
                assertFalse(row.trashed)
                FileCandidate(
                    uri = fixture.uri.toString(), displayName = fixture.name, mimeType = "image/png",
                    bytes = row.bytes, lastModifiedMillis = System.currentTimeMillis(),
                    category = CandidateCategory.IMAGE, provenance = DataProvenance.MEDIASTORE,
                    eligibility = Eligibility.ELIGIBLE, reason = "Exact newly inserted synthetic PNG"
                )
            }
        } catch (failure: SecurityException) {
            assumeNoException("Media provider denies fixture prerequisites", failure)
            throw failure
        } catch (failure: UnsupportedOperationException) {
            assumeNoException("Media provider lacks fixture prerequisites", failure)
            throw failure
        }
    }

    private fun validateMediaPlan(items: List<FileCandidate>, expected: SmartCleanRepository.Presence) {
        assertEquals(2, items.size)
        assertEquals(2, items.map { it.uri }.toSet().size)
        assertEquals(mediaFixtures.map { it.uri.toString() }.toSet(), items.map { it.uri }.toSet())
        items.forEach { item ->
            assertEquals(DataProvenance.MEDIASTORE, item.provenance)
            val fixture = mediaFixtures.single { it.uri.toString() == item.uri }
            val row = mediaRow(fixture)!!
            assertMediaIdentity(fixture, row)
            assertEquals(fixture.name, item.displayName)
            assertEquals(item.bytes, row.bytes)
            assertEquals(expected == SmartCleanRepository.Presence.TRASHED, row.trashed)
        }
        assertEquals(items.associate { it.uri to expected }, repository.presence(items))
    }

    /**
     * Never click arbitrary permission/confirmation dialogs, labels or coordinates.
     *
     * A required provider transition is NEVER converted into a skip. If Android
     * neither transitions nor shows the media provider's own confirmation, the test
     * fails with the observed state instead of reporting a misleading "OK (1 test)".
     */
    private fun launchVerifiedRequestAndObserve(
        scenario: ActivityScenario<MainActivity>,
        sender: IntentSender,
        items: List<FileCandidate>,
        before: SmartCleanRepository.Presence,
        after: SmartCleanRepository.Presence
    ) {
        validateMediaPlan(items, before)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertFalse("unrelated confirmation already open", device.hasObject(By.res("android", "button1")))
        Log.i(TAG, "media request start before=$before after=$after items=${items.size}")
        scenario.onActivity { activity ->
            activity.startIntentSenderForResult(sender, 4817, null, 0, 0, 0)
        }
        val allowedPackages = setOf(
            "com.android.providers.media", "com.android.providers.media.module",
            "com.google.android.providers.media.module"
        )
        var clickedPackage: String? = null
        var unexpectedDialog: String? = null
        val deadline = System.currentTimeMillis() + 30_000L
        while (System.currentTimeMillis() < deadline) {
            if (repository.presence(items).values.all { it == after }) break
            // A dialog can be auto-dismissed by the provider mid-read, so every
            // UiObject2 access is stale-safe and never aborts the request.
            val positive = device.findObject(By.res("android", "button1"))
            if (positive != null) {
                val pkg = runCatching { positive.applicationPackage }.getOrNull()
                if (pkg == null) {
                    // The provider already dismissed it; the state poll keeps going.
                } else if (pkg in allowedPackages) {
                    if (repository.presence(items).values.all { it == before }) {
                        validateMediaPlan(items, before)
                        Log.i(TAG, "clicking media consent positive button in $pkg")
                        runCatching { positive.click() }
                        clickedPackage = pkg
                    }
                } else {
                    unexpectedDialog = pkg
                    break
                }
            }
            Thread.sleep(200)
        }
        assertNull("Refusing to approve an unattributed dialog owned by $unexpectedDialog", unexpectedDialog)
        val verifiedAfter = repository.presence(items).values.all { it == after }
        Log.i(
            TAG,
            "media request result after=$after verified=$verifiedAfter clicked=$clickedPackage observed=${repository.presence(items)}",
        )
        if (!verifiedAfter) {
            val suffix = after.name.lowercase()
            val hierarchy = File(context.cacheDir, "knoux_fail_$suffix.xml")
            runCatching { device.dumpWindowHierarchy(hierarchy) }
            val screenshot = File(context.cacheDir, "knoux_fail_$suffix.png")
            runCatching { device.takeScreenshot(screenshot) }
            Log.i(TAG, "failure evidence hierarchy=${hierarchy.absolutePath} screenshot=${screenshot.absolutePath}")
        }
        assertTrue(
            "Provider must verify the exact requested state ($after); observed=${repository.presence(items)}",
            verifiedAfter,
        )
        assertEquals(items.associate { it.uri to after }, repository.presence(items))
    }

    @Test fun syntheticMediaIsTrashedThenRestoredThenPermanentlyDeletedThroughOs() = io {
        // Platform capability gate, not a result gate: os trash/restore/delete are API 30+.
        assumeTrue("OS media trash/restore/delete require API 30+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val items = seedPngFixtures()
        Log.i(TAG, "media fixtures inserted uris=${items.map { it.uri }}")
        validateMediaPlan(items, SmartCleanRepository.Presence.PRESENT)
        val unconfirmed = plan(items, confirmed = false, kind = OperationKind.MOVE_TO_TRASH)
        assertNull(controller.prepareMediaOperation(unconfirmed, false))
        val noAck = plan(items)
        assertNull(controller.prepareMediaOperation(noAck, false))
        validateMediaPlan(items, SmartCleanRepository.Presence.PRESENT)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Log.i(TAG, "activity launched for OS media requests")
            val trash = plan(items, kind = OperationKind.MOVE_TO_TRASH)
            validateMediaPlan(trash.candidates, SmartCleanRepository.Presence.PRESENT)
            val trashSender = controller.prepareMediaOperation(trash, false)
            assertNotNull("Provider must prepare a trash request for its own API30+ media", trashSender)
            launchVerifiedRequestAndObserve(scenario, trashSender!!, items,
                SmartCleanRepository.Presence.PRESENT, SmartCleanRepository.Presence.TRASHED)
            val trashed = controller.completeMediaOperation(trash.id, approved = true)
            assertEquals(OperationStatus.SUCCEEDED, trashed.status)
            validateMediaPlan(items, SmartCleanRepository.Presence.TRASHED)
            assertEquals(items.map { it.uri }.toSet(), controller.verifiedRemovedUris(trash.id).toSet())
            assertTrue("trash is not permanent removal", repository.verifyGone(items).values.none { it })
            assertEquals(items.map { it.uri }.toSet(), database.operationDao().items(trash.id).map { it.uri }.toSet())
            assertTrue(database.operationDao().items(trash.id).all { it.outcome == "TRASHED_VERIFIED" })
            Log.i(TAG, "trash verified for ${items.size} fixtures")

            val restore = freshController().prepareRestoreLatest()
            assertNotNull("A verified trash batch must be restorable", restore)
            val restoreRows = database.operationDao().items(restore!!.first)
            assertEquals(items.size, restoreRows.size)
            assertEquals(items.map { it.uri }.toSet(), restoreRows.map { it.uri }.toSet())
            launchVerifiedRequestAndObserve(scenario, restore.second, items,
                SmartCleanRepository.Presence.TRASHED, SmartCleanRepository.Presence.PRESENT)
            assertEquals(OperationStatus.SUCCEEDED, freshController().completeMediaOperation(restore.first, approved = true).status)
            validateMediaPlan(items, SmartCleanRepository.Presence.PRESENT)
            assertTrue(database.operationDao().items(restore.first).all { it.outcome == "RESTORED_VERIFIED" })
            assertNull("already restored rows must not be offered again", controller.prepareRestoreLatest())
            Log.i(TAG, "restore verified for ${items.size} fixtures")

            val deletion = plan(items)
            validateMediaPlan(deletion.candidates, SmartCleanRepository.Presence.PRESENT)
            val deleteSender = controller.prepareMediaOperation(deletion, true)
            assertNotNull("Provider must prepare a permanent delete request for its own API30+ media", deleteSender)
            launchVerifiedRequestAndObserve(scenario, deleteSender!!, items,
                SmartCleanRepository.Presence.PRESENT, SmartCleanRepository.Presence.GONE)
            val deleted = controller.completeMediaOperation(deletion.id, approved = true)
            assertEquals(OperationStatus.SUCCEEDED, deleted.status)
            assertEquals(items.size, deleted.removedCount)
            assertEquals(items.map { it.uri }.toSet(), controller.verifiedRemovedUris(deletion.id).toSet())
            assertTrue(repository.verifyGone(items).values.all { it })
            mediaFixtures.forEach { assertNull(mediaRow(it)) }
            val history = freshController().observeHistory().first()
            listOf(trash.id, restore.first, deletion.id).forEach { id ->
                val entry = history.single { it.id == id }
                assertEquals(OperationStatus.SUCCEEDED, entry.status)
                assertEquals(items.size, entry.itemCount)
                // Restoring does not free bytes; only trash/delete report measured removal.
                val expectedBytes = if (id == restore.first) 0L else items.sumOf { it.bytes }
                assertEquals(expectedBytes, entry.bytes)
                if (id == restore.first) assertEquals(OperationKind.RESTORE_FROM_TRASH, entry.kind)
            }
            // A duplicate OS callback must not add a second history row, re-measure bytes, or
            // re-run an action: one operation row owns the outcome. The replay therefore reports
            // that same durable outcome rather than pretending zero work happened.
            val replay = controller.completeMediaOperation(deletion.id, approved = true)
            assertEquals(OperationStatus.SUCCEEDED, replay.status)
            assertEquals("duplicate completion reports the one durable removal count", deleted.removedCount, replay.removedCount)
            val replayHistory = freshController().observeHistory().first()
            assertEquals("duplicate completion must not append history", history.size, replayHistory.size)
            val replayEntry = replayHistory.single { it.id == deletion.id }
            assertEquals(history.single { it.id == deletion.id }.bytes, replayEntry.bytes)
            assertEquals(items.size, replayEntry.itemCount)
        }
    }
}
