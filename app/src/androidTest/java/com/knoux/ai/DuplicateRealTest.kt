package com.knoux.ai

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.DuplicateController
import com.knoux.ai.data.DuplicateGroup
import com.knoux.ai.data.DuplicateRepository
import com.knoux.ai.data.DuplicateRules
import com.knoux.ai.data.DuplicateSnapshot
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.KnouxDatabase
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.OperationPlan
import com.knoux.ai.data.OperationStatus
import com.knoux.ai.data.PermissionState
import com.knoux.ai.data.SettingsStore
import com.knoux.ai.data.SmartCleanController
import com.knoux.ai.data.SmartCleanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Real PC02 repository/controller tests on the authorized device.
 *
 * Room is private and in-memory; settings live in a fresh UUID DataStore. Every
 * file is created inside a new UUID cache directory, so the only deletion here
 * targets files this test just wrote.
 */
@RunWith(AndroidJUnit4::class)
class DuplicateRealTest {

    private lateinit var context: Context
    private lateinit var database: KnouxDatabase
    private lateinit var settings: SettingsStore
    private lateinit var permissions: PermissionState
    private lateinit var cleanRepository: SmartCleanRepository
    private lateinit var clean: SmartCleanController
    private lateinit var repository: DuplicateRepository
    private lateinit var controller: DuplicateController
    private lateinit var fixtureDirectory: File
    private var testDataStoreDir: File? = null
    private val fixtures = mutableListOf<File>()
    private val prefix = "knoux_dup_${UUID.randomUUID()}_"

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    @Before fun createIsolatedDependencies() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        context = instrumentation.targetContext
        assertEquals("com.knoux.ai", context.packageName)
        database = Room.inMemoryDatabaseBuilder(context, KnouxDatabase::class.java).build()
        val dataStoreDir = File(context.filesDir, "knoux_dup_ds_${UUID.randomUUID()}")
        assertTrue(dataStoreDir.mkdir())
        testDataStoreDir = dataStoreDir
        settings = SettingsStore(PreferenceDataStoreFactory.create {
            File(dataStoreDir, "settings.preferences_pb")
        })
        permissions = PermissionState(context)
        cleanRepository = SmartCleanRepository(context, permissions)
        clean = SmartCleanController(cleanRepository, database, settings, permissions)
        repository = DuplicateRepository(context, permissions, cleanRepository)
        controller = DuplicateController(repository, database, settings, clean)
        fixtureDirectory = File(context.cacheDir, "KNOUX_QA_${UUID.randomUUID()}")
        assertTrue(fixtureDirectory.mkdir())
    }

    @After fun deleteOnlyExactFixturesAndCloseDatabase() {
        try {
            fixtures.forEach { file ->
                assertEquals(fixtureDirectory.canonicalPath, file.canonicalFile.parentFile?.canonicalPath)
                assertTrue("failed to remove exact fixture ${file.name}", !file.exists() || file.delete())
            }
            if (::fixtureDirectory.isInitialized) {
                assertTrue("fixture directory is not empty", !fixtureDirectory.exists() || fixtureDirectory.delete())
            }
        } finally {
            if (::database.isInitialized) database.close()
            testDataStoreDir?.deleteRecursively()
        }
    }

    private fun seed(name: String, content: ByteArray, modified: Long): File {
        val file = File(fixtureDirectory, prefix + name)
        assertTrue("fixture must not replace an existing file", file.createNewFile())
        fixtures += file
        file.writeBytes(content)
        assertTrue(file.setLastModified(modified))
        assertEquals(fixtureDirectory.canonicalPath, file.canonicalFile.parentFile?.canonicalPath)
        assertEquals(content.size.toLong(), file.length())
        return file
    }

    private fun fixtureGroup(snapshot: DuplicateSnapshot): DuplicateGroup? =
        snapshot.groups.firstOrNull { group ->
            (listOf(group.keeper) + group.duplicates).any { it.displayName.startsWith(prefix) }
        }

    @Test
    fun identicalContentIsGroupedPersistedAndDeletedWithoutTheOriginal() = io {
        val bytes = ByteArray(8_192) { (it % 37).toByte() }
        val different = ByteArray(8_192) { (it % 41).toByte() }
        val now = System.currentTimeMillis()
        val original = seed("original.bin", bytes, now - 600_000L)
        val copy = seed("copy.bin", bytes, now - 300_000L)
        val lookalike = seed("same-size-different.bin", different, now - 100_000L)

        val snapshot = controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow()
        assertEquals(setOf(AccessScope.APP_CACHE), snapshot.scopes)
        assertTrue("the scan must have read the fixtures", snapshot.fileCount >= 3)

        val group = fixtureGroup(snapshot) ?: error("a byte-identical pair must form a group")
        assertEquals(2, group.memberCount)
        assertEquals(8_192L, group.bytes)
        assertEquals(8_192L, group.redundantBytes)
        assertEquals(64, group.contentHash.length)
        assertEquals(Uri.fromFile(original).toString(), group.keeper.uri)
        assertEquals(Eligibility.PROTECTED, group.keeper.eligibility)
        assertEquals(listOf(Uri.fromFile(copy).toString()), group.duplicates.map { it.uri })
        assertTrue(
            "same-size different content must never be grouped",
            snapshot.groups.none { g -> (listOf(g.keeper) + g.duplicates).any { it.uri == Uri.fromFile(lookalike).toString() } },
        )
        assertTrue("no group may contain the protected keeper as a duplicate", group.keeper.uri !in group.duplicates.map { it.uri })

        // Persisted evidence: the group, its keeper flag and the measured counts.
        val scanRow = database.duplicateDao().latestFinishedScan()
            ?: error("the measured pass must be persisted")
        assertEquals(snapshot.scanId, scanRow.id)
        assertEquals(snapshot.groups.size, scanRow.groupCount)
        assertEquals(snapshot.redundantBytes, scanRow.redundantBytes)
        val memberRows = database.duplicateDao().members(scanRow.id)
        val fixtureRows = memberRows.filter { it.displayName.startsWith(prefix) }
        assertEquals(2, fixtureRows.size)
        assertEquals(setOf(Uri.fromFile(original).toString(), Uri.fromFile(copy).toString()), fixtureRows.map { it.uri }.toSet())
        assertEquals(Uri.fromFile(original).toString(), fixtureRows.single { it.isKeeper }.uri)

        val selected = DuplicateRules.toggleGroup(group, emptySet(), checked = true)
        assertEquals(setOf(Uri.fromFile(copy).toString()), selected)
        val plan = OperationPlan(
            id = UUID.randomUUID().toString(),
            kind = OperationKind.PERMANENT_DELETE,
            candidates = DuplicateRules.selectedCandidates(listOf(group), selected),
            requiresSystemConfirmation = false,
            confirmedByUser = true,
        )
        val result = controller.executeLocal(plan, acknowledgedIrreversible = true)
        assertEquals(OperationStatus.SUCCEEDED, result.status)
        assertEquals(1, result.removedCount)
        assertEquals(0, result.failedCount)
        assertEquals(setOf(Uri.fromFile(copy).toString()), clean.verifiedRemovedUris(plan.id))
        assertFalse("the selected copy must be gone", copy.exists())
        assertTrue("the preserved original must survive", original.isFile && original.length() == 8_192L)
        assertTrue("the unrelated lookalike must survive", lookalike.isFile)

        // Reconciled from verified evidence: a one-member group is no longer a duplicate group.
        val after = controller.latest()!!
        assertNull("the singleton group must disappear", fixtureGroup(after))
        assertEquals(snapshot.groups.size - 1, after.groups.size)
        assertEquals(snapshot.redundantBytes - group.redundantBytes, after.redundantBytes)
        assertEquals(snapshot.fileCount - 1, after.fileCount)
        assertEquals(after.groups.size, database.duplicateDao().latestFinishedScan()!!.groupCount)
        assertTrue(
            "the dissolved group must not remain as a one-member duplicate row",
            database.duplicateDao().members(after.scanId).none { it.displayName.startsWith(prefix) },
        )
    }

    @Test
    fun keptCopyIsExcludedFromLaterPassesAndASnapshotSurvivesANewController() = io {
        val bytes = ByteArray(4_096) { (it % 13).toByte() }
        val now = System.currentTimeMillis()
        val original = seed("kept-original.bin", bytes, now - 400_000L)
        val copy = seed("kept-copy.bin", bytes, now - 200_000L)

        val first = controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow()
        val copyCandidate = (fixtureGroup(first) ?: error("the pair must be detected before exclusion"))
            .duplicates.single()
        assertEquals(Uri.fromFile(copy).toString(), copyCandidate.uri)

        // A real persisted exclusion, not a UI flag.
        clean.keepForever(copyCandidate)
        val excluded = controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow()
        assertNull("a kept copy cannot be offered again", fixtureGroup(excluded))

        clean.removeExclusion(copyCandidate.uri)
        val restored = controller.scan(setOf(AccessScope.APP_CACHE)).getOrThrow()
        val group = fixtureGroup(restored) ?: error("removing the exclusion must offer the pair again")
        assertEquals(Uri.fromFile(original).toString(), group.keeper.uri)
        assertEquals(Uri.fromFile(copy).toString(), group.duplicates.single().uri)

        // A fresh controller with the same Room store rehydrates the exact keeper.
        val fresh = DuplicateController(
            DuplicateRepository(context, permissions, cleanRepository),
            database, settings, clean,
        )
        val reloaded = fresh.latest() ?: error("a persisted snapshot must reload")
        val reloadedGroup = fixtureGroup(reloaded) ?: error("the keeper group must survive a reload")
        assertEquals(group.id, reloadedGroup.id)
        assertEquals(Uri.fromFile(original).toString(), reloadedGroup.keeper.uri)
        assertEquals(group.duplicates.map { it.uri }, reloadedGroup.duplicates.map { it.uri })
    }

    @Test
    fun emptyScopeSetMeasuresNothingAndCreatesNoFabricatedGroup() = io {
        val snapshot = controller.scan(emptySet()).getOrThrow()
        assertTrue(snapshot.scopes.isEmpty())
        assertEquals(0, snapshot.fileCount)
        assertEquals(0, snapshot.hashedCount)
        assertEquals(0, snapshot.groups.size)
        assertEquals(0L, snapshot.bytesHashed)
        val stored = database.duplicateDao().latestFinishedScan()
            ?: error("even a zero-result pass must persist its scope evidence")
        assertEquals(0, stored.groupCount)
        assertTrue(database.duplicateDao().groups(stored.id).isEmpty())
    }
}
