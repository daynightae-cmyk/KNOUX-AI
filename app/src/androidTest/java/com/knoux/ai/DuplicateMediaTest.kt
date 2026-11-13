package com.knoux.ai

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.knoux.ai.data.AccessScope
import com.knoux.ai.data.CandidateCategory
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.DuplicateController
import com.knoux.ai.data.DuplicateGroup
import com.knoux.ai.data.DuplicateRepository
import com.knoux.ai.data.DuplicateRules
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.FileCandidate
import com.knoux.ai.data.KnouxDatabase
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.OperationPlan
import com.knoux.ai.data.OperationStatus
import com.knoux.ai.data.PermissionGrant
import com.knoux.ai.data.PermissionState
import com.knoux.ai.data.SettingsStore
import com.knoux.ai.data.SmartCleanController
import com.knoux.ai.data.SmartCleanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * PC02 — real duplicate cleanup of the customer's own shared media.
 *
 * Every media row here is inserted by this test into a brand-new UUID folder and
 * owns `com.knoux.ai` as its owner package. Nothing else in the library is ever
 * selected, trashed or deleted: plans are built only from the two fixture URIs.
 *
 * The OS confirmation dialog is never bypassed. The harness clicks only a dialog
 * that belongs to a known media provider, and every state transition is re-read
 * from MediaStore instead of being assumed.
 *
 * Requires READ_MEDIA_IMAGES to be granted to `com.knoux.ai` for the duration of
 * this run (revoked by the shell afterwards). A missing grant fails the test; it is
 * never converted into a skip.
 */
@RunWith(AndroidJUnit4::class)
class DuplicateMediaTest {
    private companion object {
        const val TAG = "KNOUX_DUP_MEDIA_QA"
    }

    private lateinit var context: android.content.Context
    private lateinit var database: KnouxDatabase
    private lateinit var settings: SettingsStore
    private lateinit var permissions: PermissionState
    private lateinit var cleanRepository: SmartCleanRepository
    private lateinit var clean: SmartCleanController
    private lateinit var repository: DuplicateRepository
    private lateinit var controller: DuplicateController
    private var testDataStoreDir: File? = null
    private val fixtures = mutableListOf<MediaFixture>()
    private val prefix = "knoux_dup_media_${UUID.randomUUID()}_"

    private data class MediaFixture(val uri: Uri, val name: String, val relativePath: String)
    private data class MediaRow(
        val owner: String?,
        val name: String,
        val relativePath: String?,
        val bytes: Long,
        val trashed: Boolean,
    )

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    @Before fun prepareIsolatedEngineAndVerifyTheGrant() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        context = instrumentation.targetContext
        assertEquals("com.knoux.ai", context.packageName)
        SplashGate.consumed = true
        database = Room.inMemoryDatabaseBuilder(context, KnouxDatabase::class.java).build()
        val dataStoreDir = File(context.filesDir, "knoux_dup_media_ds_${UUID.randomUUID()}")
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

        assertEquals(
            "READ_MEDIA_IMAGES must be granted to com.knoux.ai for this run (not skipped)",
            PermissionGrant.GRANTED,
            permissions.mediaGrant(AccessScope.MEDIA_IMAGES),
        )
        assertTrue(
            "OS-mediated media removal requires API 30+",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        )
    }

    @After fun deleteOnlyExactFixtureRows() {
        try {
            fixtures.forEach { fixture ->
                // Exact URI only: a broad delete could touch the owner's library.
                runCatching { context.contentResolver.delete(fixture.uri, null, null) }
                assertNull("exact fixture row must be gone: ${fixture.name}", mediaRow(fixture))
            }
        } finally {
            if (::database.isInitialized) database.close()
            testDataStoreDir?.deleteRecursively()
        }
    }

    private fun mediaRow(fixture: MediaFixture): MediaRow? {
        val projection = arrayOf(
            MediaStore.MediaColumns.OWNER_PACKAGE_NAME,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.IS_TRASHED,
        )
        val args = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
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
                trashed = it.getInt(it.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_TRASHED)) == 1,
            )
        }
    }

    /**
     * Writes two byte-identical, high-entropy PNGs into their own UUID folders and
     * verifies the provider stored them exactly as requested.
     *
     * The content must not be compressible: the product deliberately ignores files
     * below [DuplicateRules.MIN_BYTES] (1 KiB), so a flat 8x8 swatch would never
     * become a duplicate candidate. The same fixed seed for both files keeps them
     * identical byte for byte, which is exactly the invariant under test.
     */
    private fun seedPng(suffix: String, size: Int): MediaFixture {
        val path = "Pictures/KNOUX_QA_${UUID.randomUUID()}/"
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, prefix + suffix)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }) ?: throw UnsupportedOperationException("Media provider cannot insert a synthetic PNG")
        val fixture = MediaFixture(uri, prefix + suffix, path)
        fixtures += fixture
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            val noise = java.util.Random(0x4B4E4F58L)
            val pixels = IntArray(size * size)
            for (index in pixels.indices) {
                pixels[index] = 0xFF000000.toInt() or noise.nextInt(0x1000000)
            }
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw UnsupportedOperationException("Media provider cannot write a synthetic PNG")
            stream.use { assertTrue("PNG compression must succeed", bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
        assertEquals(1, context.contentResolver.update(uri, ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }, null, null))
        val row = mediaRow(fixture) ?: error("the inserted fixture must be readable")
        assertEquals("com.knoux.ai", row.owner)
        assertEquals(fixture.name, row.name)
        assertEquals(fixture.relativePath, row.relativePath)
        assertTrue(
            "a duplicate fixture must exceed the product minimum of ${DuplicateRules.MIN_BYTES} bytes; was ${row.bytes}",
            row.bytes >= DuplicateRules.MIN_BYTES,
        )
        assertFalse("a fresh fixture must not already be trashed", row.trashed)
        return fixture
    }

    /**
     * A MediaStore row is identified by its numeric id, not by the volume alias in
     * the URI: an insert through VOLUME_EXTERNAL_PRIMARY returns
     * `.../external_primary/images/media/<id>` while a library query returns
     * `.../external/images/media/<id>` for the very same row.
     */
    private fun mediaIds(uris: List<Uri>): Set<String> =
        uris.map { it.lastPathSegment ?: error("a media URI must end with its row id: $it") }.toSet()

    private fun fixtureCandidates(): Map<String, FileCandidate> {
        val scope = AccessScope.MEDIA_IMAGES
        val snapshot = io { cleanRepository.scan(setOf(scope), emptySet()) }
        val rows = snapshot.second.filter { it.displayName.startsWith(prefix) }
        assertEquals("both fixtures must be visible to the provider scan", 2, rows.size)
        assertTrue(rows.all { it.provenance == DataProvenance.MEDIASTORE })
        assertTrue(rows.all { it.category == CandidateCategory.IMAGE })
        assertTrue(rows.all { it.status == com.knoux.ai.data.DataStatus.MEASURED })
        assertTrue(rows.all { it.eligibility == Eligibility.ELIGIBLE })
        return rows.associateBy { it.uri }
    }

    private fun fixtureGroup(snapshot: com.knoux.ai.data.DuplicateSnapshot): DuplicateGroup? =
        snapshot.groups.firstOrNull { group ->
            (listOf(group.keeper) + group.duplicates).any { it.displayName.startsWith(prefix) }
        }

    private fun presence(items: List<FileCandidate>) = cleanRepository.presence(items)

    /**
     * Uses the exact candidates captured before the first mutation: a trashed row is
     * hidden from an ordinary library query, so re-scanning would prove nothing.
     */
    private fun assertFixturePresence(items: List<FileCandidate>, expected: SmartCleanRepository.Presence) {
        assertEquals(2, items.size)
        assertEquals(items.map { it.uri }.toSet(), presence(items).keys)
        assertTrue(
            "both fixtures must be ${expected.name}; observed=${presence(items)}",
            presence(items).values.all { it == expected },
        )
        if (expected != SmartCleanRepository.Presence.GONE) {
            fixtures.forEach { fixture ->
                val row = mediaRow(fixture) ?: error("a ${expected.name} row is still a readable row")
                assertEquals(expected == SmartCleanRepository.Presence.TRASHED, row.trashed)
            }
        }
    }

    /**
     * Never clicks an unattributed dialog. If the provider neither transitions nor
     * shows its own confirmation, the test fails with the observed state.
     */
    private fun launchVerifiedRequestAndObserve(
        scenario: ActivityScenario<MainActivity>,
        sender: android.content.IntentSender,
        items: List<FileCandidate>,
        before: SmartCleanRepository.Presence,
        after: SmartCleanRepository.Presence,
    ) {
        assertTrue("the plan must contain only fixture URIs", items.all { it.displayName.startsWith(prefix) })
        assertEquals(before, presence(items).values.single())
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertFalse("an unrelated confirmation is already open", device.hasObject(By.res("android", "button1")))
        Log.i(TAG, "media request before=$before after=$after items=${items.map { it.displayName }}")
        scenario.onActivity { activity ->
            @Suppress("DEPRECATION")
            activity.startIntentSenderForResult(sender, 4818, null, 0, 0, 0)
        }
        val allowedPackages = setOf(
            "com.android.providers.media", "com.android.providers.media.module",
            "com.google.android.providers.media.module",
        )
        var clickedPackage: String? = null
        var unexpectedDialog: String? = null
        val deadline = System.currentTimeMillis() + 30_000L
        while (System.currentTimeMillis() < deadline) {
            if (presence(items).values.all { it == after }) break
            val positive = device.findObject(By.res("android", "button1"))
            if (positive != null) {
                val pkg = runCatching { positive.applicationPackage }.getOrNull()
                if (pkg == null) {
                    // The provider dismissed it already; keep polling the real state.
                } else if (pkg in allowedPackages) {
                    if (presence(items).values.all { it == before }) {
                        Log.i(TAG, "clicking provider consent in $pkg")
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
        assertNull("refusing to approve a dialog owned by $unexpectedDialog", unexpectedDialog)
        val observed = presence(items)
        Log.i(TAG, "media request observed=$observed clicked=$clickedPackage")
        assertTrue(
            "the provider must verify the exact requested state ($after); observed=$observed",
            observed.values.all { it == after },
        )
    }

    private fun plan(kind: OperationKind, items: List<FileCandidate>) = OperationPlan(
        id = UUID.randomUUID().toString(),
        kind = kind,
        candidates = items,
        requiresSystemConfirmation = items.any { it.provenance == DataProvenance.MEDIASTORE },
        confirmedByUser = true,
    )

    @Test
    fun identicalSyntheticPhotosAreTrashedRestoredAndDeletedThroughTheOs() {
        val first = seedPng("alpha.png", 64)
        val second = seedPng("bravo.png", 64)
        Log.i(TAG, "fixtures inserted uris=${fixtures.map { it.uri }}")

        // The screen is opened before any provider work, exactly like the customer flow.
        // This device's OEM power manager freezes a background uid ("Hiber freeze"),
        // which would otherwise suspend the scan mid-run and look like a hang.
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            // 1. Real discovery: identical bytes, one protected original.
            val snapshot = io { controller.scan(setOf(AccessScope.MEDIA_IMAGES)).getOrThrow() }
            val group = fixtureGroup(snapshot) ?: error("the identical PNGs must form one duplicate group")
            assertEquals(2, group.memberCount)
            assertEquals(1, group.duplicates.size)
            assertEquals(group.bytes, group.keeper.bytes)
            assertEquals(group.bytes, group.redundantBytes)
            assertEquals(64, group.contentHash.length)
            assertEquals(Eligibility.PROTECTED, group.keeper.eligibility)
            // MediaStore returns a volume alias for the same row, so identity is the id.
            val fixtureIds = mediaIds(fixtures.map { it.uri })
            assertEquals(
                fixtureIds,
                mediaIds((listOf(group.keeper) + group.duplicates).map { Uri.parse(it.uri) }),
            )

            val selectable = DuplicateRules.selectableUris(group)
            assertEquals("only the copy may be selectable", 1, selectable.size)
            assertTrue("the keeper must never be selectable", group.keeper.uri !in selectable)
            val copy = group.duplicates.single()
            assertEquals(selectable.single(), copy.uri)

            // 2. A keeper URI that somehow reached the selection is still discarded.
            val forged = DuplicateRules.selectedCandidates(listOf(group), selectable + group.keeper.uri)
            assertEquals(listOf(copy.uri), forged.map { it.uri })

            // 3. OS-mediated trash of the copy only.
            val fixtureRows = fixtureCandidates()
            val items = fixtureRows.values.toList()
            assertFixturePresence(items, SmartCleanRepository.Presence.PRESENT)
            val trash = plan(OperationKind.MOVE_TO_TRASH, forged)
            assertNull(
                "trash without the customer's confirmation must prepare nothing",
                io { controller.prepareMedia(plan(OperationKind.MOVE_TO_TRASH, forged).copy(confirmedByUser = false), false) },
            )
            val sender = io { controller.prepareMedia(trash, acknowledgedIrreversible = false) }
            assertNotNull("the provider must prepare a trash request for its own media", sender)
            launchVerifiedRequestAndObserve(
                scenario, sender!!, forged,
                SmartCleanRepository.Presence.PRESENT, SmartCleanRepository.Presence.TRASHED,
            )
            val trashed = io { controller.completeMedia(trash.id, approved = true) }
            assertEquals(OperationStatus.SUCCEEDED, trashed.status)
            assertEquals(1, trashed.removedCount)
            assertEquals(0, trashed.failedCount)
            assertEquals(
                "the trash outcome must name the copy, never the keeper",
                setOf(copy.uri),
                io { clean.verifiedRemovedUris(trash.id) },
            )
            assertTrue(
                "trash never deletes permanently",
                io { cleanRepository.verifyGone(listOf(copy, group.keeper)).values.none { it } },
            )
            val afterTrash = io { controller.latest() } ?: error("the measured snapshot must persist")
            assertNull("a group with one surviving member is no longer a duplicate group", fixtureGroup(afterTrash))

            // 4. Restore through the OS, then prove the pair is re-detected.
            val restore = io { clean.prepareRestoreLatest() }
                ?: error("the verified trash batch must be restorable")
            val restoreItems = io { database.operationDao().items(restore.first) }
            assertEquals(setOf(copy.uri), restoreItems.map { it.uri }.toSet())
            launchVerifiedRequestAndObserve(
                scenario, restore.second, forged,
                SmartCleanRepository.Presence.TRASHED, SmartCleanRepository.Presence.PRESENT,
            )
            assertEquals(
                OperationStatus.SUCCEEDED,
                io { clean.completeMediaOperation(restore.first, approved = true) }.status,
            )
            assertFixturePresence(items, SmartCleanRepository.Presence.PRESENT)
            val rescan = io { controller.scan(setOf(AccessScope.MEDIA_IMAGES)).getOrThrow() }
            val again = fixtureGroup(rescan) ?: error("restored photos must be offered again")
            assertEquals(group.contentHash, again.contentHash)
            assertEquals(group.keeper.uri, again.keeper.uri)
            assertEquals(
                fixtureIds,
                mediaIds((listOf(again.keeper) + again.duplicates).map { Uri.parse(it.uri) }),
            )

            // 5. Permanent delete of the copy only, through the OS confirmation.
            val deletion = plan(OperationKind.PERMANENT_DELETE, listOf(again.duplicates.single()))
            // A rejected plan is durable and cannot be re-claimed, so the negative
            // case must use its own plan id exactly like the trash assertion above.
            assertNull(
                "permanent media deletion without the irreversible acknowledgement must not prepare",
                io {
                    controller.prepareMedia(
                        plan(OperationKind.PERMANENT_DELETE, listOf(again.duplicates.single())),
                        acknowledgedIrreversible = false,
                    )
                },
            )
            val deleteSender = io { controller.prepareMedia(deletion, acknowledgedIrreversible = true) }
            assertNotNull("the provider must prepare a permanent delete request", deleteSender)
            launchVerifiedRequestAndObserve(
                scenario, deleteSender!!, listOf(again.duplicates.single()),
                SmartCleanRepository.Presence.PRESENT, SmartCleanRepository.Presence.GONE,
            )
            val deleted = io { controller.completeMedia(deletion.id, approved = true) }
            assertEquals(OperationStatus.SUCCEEDED, deleted.status)
            assertEquals(1, deleted.removedCount)
            assertEquals(setOf(copy.uri), io { clean.verifiedRemovedUris(deletion.id) })
            assertTrue(
                "the deleted copy must be verified gone",
                io { cleanRepository.verifyGone(listOf(again.duplicates.single())).values.all { it } },
            )
            assertTrue(
                "the preserved original must still exist as a media row",
                mediaRow(
                    fixtures.single {
                        it.uri.lastPathSegment == Uri.parse(again.keeper.uri).lastPathSegment
                    },
                ) != null,
            )
            assertTrue(
                "no keeper may ever appear in a plan",
                io {
                    database.operationDao().items(deletion.id).none { it.uri == again.keeper.uri } &&
                        database.operationDao().items(trash.id).none { it.uri == group.keeper.uri }
                },
            )
        } finally {
            scenario.close()
        }
    }
}
