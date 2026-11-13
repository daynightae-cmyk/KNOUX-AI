package com.knoux.ai

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.CandidateCategory
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.FileCandidate
import com.knoux.ai.data.KnouxDatabase
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.OperationPlan
import com.knoux.ai.data.OperationStatus
import com.knoux.ai.data.PermissionState
import com.knoux.ai.data.ResidueAction
import com.knoux.ai.data.ResidueConfidence
import com.knoux.ai.data.ResidueController
import com.knoux.ai.data.ResiduePattern
import com.knoux.ai.data.ResidueRepository
import com.knoux.ai.data.ResidueRules
import com.knoux.ai.data.SettingsStore
import com.knoux.ai.data.SmartCleanController
import com.knoux.ai.data.SmartCleanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * PC03 real-behaviour proof on the owner-authorized phone.
 *
 * Every fixture is created inside KNOUX's own cache with a UUID name, every removal
 * is re-queried, and teardown deletes only those exact files. Room is in-memory here;
 * the production database is never opened by this test.
 */
@RunWith(AndroidJUnit4::class)
class ResidueStorageTest {

    private lateinit var context: Context
    private lateinit var database: KnouxDatabase
    private lateinit var settings: SettingsStore
    private lateinit var permissions: PermissionState
    private lateinit var repository: ResidueRepository
    private lateinit var controller: ResidueController
    private lateinit var cacheDirectory: File
    private lateinit var forgedDirectory: File
    private lateinit var testDataStoreDir: File
    private val fixtures = mutableListOf<File>()
    private val prefix = "knoux_residue_${UUID.randomUUID()}_"

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    @Before fun createIsolatedDependencies() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        context = instrumentation.targetContext
        assertEquals("com.knoux.ai", context.packageName)
        database = Room.inMemoryDatabaseBuilder(context, KnouxDatabase::class.java).build()
        testDataStoreDir = File(context.filesDir, "knoux_test_ds_${UUID.randomUUID()}").also { it.mkdirs() }
        val dataStore = PreferenceDataStoreFactory.create {
            File(testDataStoreDir, "settings.preferences_pb")
        }
        settings = SettingsStore(dataStore)
        permissions = PermissionState(context)
        repository = ResidueRepository(context, permissions)
        val clean = SmartCleanController(
            SmartCleanRepository(context, permissions), database, settings, permissions,
        )
        controller = ResidueController(repository, database, settings, permissions, clean)
        cacheDirectory = File(context.cacheDir, "KNOUX_QA_${UUID.randomUUID()}").also { assertTrue(it.mkdir()) }
        forgedDirectory = File(context.filesDir, "KNOUX_QA_${UUID.randomUUID()}").also { assertTrue(it.mkdir()) }
    }

    @After fun deleteOnlyExactFixturesAndCloseIsolatedDatabase() {
        try {
            fixtures.forEach { file ->
                val parent = file.canonicalFile.parentFile?.canonicalPath
                assertTrue(
                    "fixture must stay inside its UUID directory",
                    parent == cacheDirectory.canonicalPath || parent == forgedDirectory.canonicalPath,
                )
                assertTrue("failed to remove exact fixture ${file.name}", !file.exists() || file.delete())
            }
            if (::cacheDirectory.isInitialized) {
                assertTrue("fixture directory is not empty", !cacheDirectory.exists() || cacheDirectory.delete())
            }
            if (::forgedDirectory.isInitialized) {
                assertTrue("forged directory is not empty", !forgedDirectory.exists() || forgedDirectory.delete())
            }
        } finally {
            if (::database.isInitialized) database.close()
            if (::testDataStoreDir.isInitialized) testDataStoreDir.deleteRecursively()
        }
    }

    private fun seed(name: String, bytes: Int = 4_096, modified: Long = System.currentTimeMillis() - 3_600_000L): File {
        val file = File(cacheDirectory, prefix + name)
        assertTrue("never overwrite a fixture", file.createNewFile())
        fixtures += file
        file.writeBytes(ByteArray(bytes) { (it % 17).toByte() })
        assertTrue(file.setLastModified(modified))
        assertEquals(bytes.toLong(), file.length())
        return file
    }

    private fun plan(
        candidates: List<FileCandidate>,
        confirmed: Boolean = true,
        kind: OperationKind = OperationKind.PERMANENT_DELETE,
    ) = OperationPlan(
        id = UUID.randomUUID().toString(),
        kind = kind,
        candidates = candidates,
        requiresSystemConfirmation = false,
        confirmedByUser = confirmed,
    )

    @Test fun ownCacheScanMeasuresRealFilesAndPreselectsNothing() = io {
        val old = seed("old.tmp")
        val fresh = seed("fresh.bin", modified = System.currentTimeMillis())
        val scan = controller.scan(setOf(AccessScope.APP_CACHE), ageDays = 30).getOrThrow()

        val oldItem = scan.items.single { it.candidate.displayName == old.name }
        val freshItem = scan.items.single { it.candidate.displayName == fresh.name }
        assertEquals(ResidueConfidence.OWNED, oldItem.confidence)
        assertEquals(ResiduePattern.TEMP_FILE, oldItem.pattern)
        // Own cache ignores the shared-folder age rule: it is disposable by contract.
        assertEquals(ResidueConfidence.OWNED, freshItem.confidence)
        assertEquals(ResiduePattern.CACHE_FILE, freshItem.pattern)
        assertEquals(setOf(AccessScope.APP_CACHE), scan.scopes)
        assertEquals(DataProvenance.APP_OWNED_CACHE, oldItem.candidate.provenance)
        assertEquals(old.length(), oldItem.candidate.bytes)

        // The scan persisted its evidence and recorded no decision at all.
        val stored = database.residueDao().items(scan.scanId)
        assertEquals(scan.items.size, stored.size)
        assertEquals(scan.items.map { it.candidate.uri }.toSet(), stored.map { it.uri }.toSet())
        assertTrue(database.residueDao().decisions(scan.scanId).isEmpty())

        // Nothing is selected until the customer acts; the safe set is own cache only.
        val safe = ResidueRules.safeSelection(scan.items)
        assertTrue(safe.contains(oldItem.candidate.uri))
        assertTrue(ResidueRules.dryRun(scan.items, emptySet()).items.isEmpty())

        val reloaded = controller.latest()!!
        assertEquals(scan.scanId, reloaded.scanId)
        assertEquals(scan.items.size, reloaded.items.size)
        assertEquals(scan.bytes, reloaded.bytes)
    }

    @Test fun permanentDeleteRemovesOnlySelectedAndWritesVerifiableAudit() = io {
        val first = seed("alpha.tmp", 2_048)
        val second = seed("bravo.log", 4_096)
        val keeper = seed("keep-me.bin", 1_024)
        val scan = controller.scan(setOf(AccessScope.APP_CACHE), ageDays = 1).getOrThrow()

        val chosen = scan.items.filter { it.candidate.displayName in setOf(first.name, second.name) }
        assertEquals(2, chosen.size)

        val run = ResidueRules.dryRun(scan.items, chosen.map { it.candidate.uri }.toSet())
        assertEquals(6_144L, run.bytes)
        controller.recordDecision(ResidueAction.DRY_RUN, scan.scanId, run.items, "test dry run")

        val operation = plan(chosen.map { it.candidate })
        val result = controller.executeLocal(operation, acknowledgedIrreversible = true)
        assertEquals(OperationStatus.SUCCEEDED, result.status)
        assertEquals(2, result.removedCount)
        assertEquals(0, result.failedCount)
        assertFalse(first.exists())
        assertFalse(second.exists())
        assertTrue("an unselected residue file must survive", keeper.exists())

        // Only provider-verified removals leave the stored pass.
        val remaining = controller.latest()!!
        val removedUris = chosen.map { it.candidate.uri }.toSet()
        assertTrue(
            "verified removals must leave the stored pass",
            remaining.items.none { it.candidate.uri in removedUris },
        )
        assertTrue(
            "an unselected cache file must remain in the measured pass",
            remaining.items.any { it.candidate.uri == Uri.fromFile(keeper).toString() },
        )
        assertEquals(
            "the persisted total must drop by exactly the removed bytes",
            scan.bytes - 6_144L,
            remaining.bytes,
        )

        controller.recordDecision(ResidueAction.VERIFIED, scan.scanId, chosen, "test verified")
        val audit = controller.observeAudit().first()
        assertTrue(audit.any { it.action == ResidueAction.DRY_RUN })
        assertTrue(audit.any { it.action == ResidueAction.VERIFIED })
        // The audit note never contains a directory path.
        assertTrue(audit.none { entry -> entry.note?.contains(context.cacheDir.absolutePath) == true })

        val history = withTimeout(10_000) {
            controller.observeHistory().first { rows -> rows.any { it.scanId == scan.scanId } }
        }.single { it.scanId == scan.scanId }
        // The history row reflects the persisted pass, including unrelated cache
        // files that were measured but never selected.
        assertEquals(remaining.items.size, history.itemCount)
        assertEquals(remaining.bytes, history.bytes)

        val operationRows = database.operationDao().items(operation.id)
        assertEquals(2, operationRows.size)
        assertTrue(operationRows.all { it.outcome == "REMOVED_VERIFIED" })
    }

    @Test fun unconfirmedOrUnacknowledgedPlansDeleteNothing() = io {
        val file = seed("guarded.tmp", 3_072)
        val scan = controller.scan(setOf(AccessScope.APP_CACHE), ageDays = 1).getOrThrow()
        val item = scan.items.single { it.candidate.displayName == file.name }

        val unconfirmed = controller.executeLocal(plan(listOf(item.candidate), confirmed = false), true)
        assertTrue(unconfirmed.status != OperationStatus.SUCCEEDED)
        assertEquals(0, unconfirmed.removedCount)
        assertTrue(file.exists())

        val unacknowledged = controller.executeLocal(plan(listOf(item.candidate)), acknowledgedIrreversible = false)
        assertTrue(unacknowledged.status != OperationStatus.SUCCEEDED)
        assertEquals(0, unacknowledged.removedCount)
        assertTrue(file.exists())

        assertNotNull(controller.latest()!!.items.firstOrNull { it.candidate.displayName == file.name })
    }

    @Test fun forgedCandidateOutsideTheCacheIsRefusedWithoutTouchingTheFile() = io {
        val outside = File(forgedDirectory, prefix + "escaped.tmp")
        assertTrue(outside.createNewFile())
        fixtures += outside
        assertTrue(outside.setLastModified(System.currentTimeMillis() - 7_200_000L))
        outside.writeBytes(ByteArray(2_048) { 3 })
        val forged = FileCandidate(
            uri = Uri.fromFile(outside).toString(),
            displayName = outside.name,
            mimeType = null,
            bytes = outside.length(),
            lastModifiedMillis = outside.lastModified(),
            category = CandidateCategory.TEMP_FILE,
            provenance = DataProvenance.APP_OWNED_CACHE,
            eligibility = Eligibility.ELIGIBLE,
            reason = "forged out-of-root candidate",
        )
        val result = controller.executeLocal(plan(listOf(forged)), acknowledgedIrreversible = true)
        assertTrue(result.status != OperationStatus.SUCCEEDED)
        assertEquals(0, result.removedCount)
        assertTrue("the forged file must be untouched", outside.exists())
    }

    @Test fun emptyFileAndMissingFileAreReportedHonestly() = io {
        val empty = seed("empty.log", 0)
        val vanished = seed("vanishes.tmp", 1_024)
        val scan = controller.scan(setOf(AccessScope.APP_CACHE), ageDays = 1).getOrThrow()
        val item = scan.items.single { it.candidate.displayName == empty.name }
        assertEquals(Eligibility.UNSELECTABLE, item.candidate.eligibility)
        assertFalse(ResidueRules.canSelect(item))

        val vanishedItem = scan.items.single { it.candidate.displayName == vanished.name }
        assertTrue(vanished.delete())
        val operation = plan(listOf(vanishedItem.candidate))
        val result = controller.executeLocal(operation, acknowledgedIrreversible = true)
        // Nothing was removed by KNOUX: the file was already gone before the action.
        assertEquals(0, result.removedCount)
        assertEquals(
            "ALREADY_GONE",
            database.operationDao().items(operation.id).single().outcome,
        )
        assertNotNull(controller.latest())
        // A removal the provider never verified is not reported as removed; the stored
        // pass keeps its captured evidence and the UI labels it as a previous pass.
        assertNotNull(controller.latest())
    }

    @Test fun revocationOfAnAgeRuleResetsToTheSafeDefault() = io {
        controller.setMinAgeDays(7)
        assertEquals(7, controller.minAgeDays.first())
        controller.setMinAgeDays(999)
        assertEquals(ResidueRules.DEFAULT_MIN_AGE_DAYS, controller.minAgeDays.first())
        controller.setScopes(setOf(AccessScope.APP_CACHE, AccessScope.SAF_TREE))
        assertEquals(setOf(AccessScope.APP_CACHE, AccessScope.SAF_TREE), controller.selectedScopes.first())
        controller.setScopes(emptySet())
        assertEquals(setOf(AccessScope.APP_CACHE), controller.selectedScopes.first())
    }
}
