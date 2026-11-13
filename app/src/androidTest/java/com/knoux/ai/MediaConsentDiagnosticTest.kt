package com.knoux.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.knoux.ai.data.CandidateCategory
import com.knoux.ai.data.DataProvenance
import com.knoux.ai.data.Eligibility
import com.knoux.ai.data.FileCandidate
import com.knoux.ai.data.KnouxDatabase
import com.knoux.ai.data.OperationKind
import com.knoux.ai.data.OperationPlan
import com.knoux.ai.data.PermissionState
import com.knoux.ai.data.SettingsStore
import com.knoux.ai.data.SmartCleanController
import com.knoux.ai.data.SmartCleanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * READ-ONLY diagnostic for the OS media consent path.
 *
 * It inserts one UUID-named synthetic PNG, moves it to the provider's trash, then
 * starts the real restore request and dumps the resulting UI hierarchy/screenshot
 * WITHOUT ever approving it. The dialog is dismissed with BACK and the exact
 * fixture is deleted in teardown.
 *
 * This exists because a timeout must never become a passing test: the exact UI
 * that owns the restore confirmation has to be observed, not guessed.
 */
@RunWith(AndroidJUnit4::class)
class MediaConsentDiagnosticTest {
    private lateinit var context: Context
    private lateinit var database: KnouxDatabase
    private lateinit var settings: SettingsStore
    private lateinit var permissions: PermissionState
    private lateinit var repository: SmartCleanRepository
    private lateinit var controller: SmartCleanController
    private var testDataStoreDir: File? = null
    private var fixtureUri: Uri? = null
    private var fixtureName: String? = null
    private var fixturePath: String? = null

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    @After fun removeExactDiagnosticFixtureOnly() {
        try {
            val uri = fixtureUri ?: return
            val name = fixtureName ?: return
            val path = fixturePath ?: return
            val args = android.os.Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            }
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.OWNER_PACKAGE_NAME,
                ),
                args,
                null,
            )
            val matched = cursor?.use { c ->
                if (!c.moveToFirst()) return@use false
                c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)) > 0L &&
                    c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) == name &&
                    c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)) == path &&
                    c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)) == "com.knoux.ai"
            } ?: false
            if (matched) context.contentResolver.delete(uri, null, null)
        } finally {
            if (::database.isInitialized) database.close()
            testDataStoreDir?.deleteRecursively()
        }
    }

    @Test fun observeRealRestoreConsentWithoutApprovingIt() {
        assumeTrue("MediaStore trash requires API 30+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        context = instrumentation.targetContext
        assertEquals("com.knoux.ai", context.packageName)
        database = Room.inMemoryDatabaseBuilder(context, KnouxDatabase::class.java).build()
        val dsDir = File(context.filesDir, "knoux_diag_ds_${UUID.randomUUID()}").apply { mkdirs() }
        testDataStoreDir = dsDir
        settings = SettingsStore(PreferenceDataStoreFactory.create { File(dsDir, "settings.preferences_pb") })
        permissions = PermissionState(context)
        repository = SmartCleanRepository(context, permissions)
        controller = SmartCleanController(repository, database, settings, permissions)

        val path = "Pictures/KNOUX_QA_DIAG_${UUID.randomUUID()}/"
        val name = "knoux_diag_${UUID.randomUUID()}.png"
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }) ?: throw UnsupportedOperationException("Media provider cannot insert synthetic PNG")
        fixtureUri = uri
        fixtureName = name
        fixturePath = path
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(android.graphics.Color.MAGENTA)
            context.contentResolver.openOutputStream(uri)!!.use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
        assertEquals(1, context.contentResolver.update(uri, ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }, null, null))

        val items = listOf(candidate(uri, name))
        assertEquals(SmartCleanRepository.Presence.PRESENT, repository.presence(items).values.single())
        println("KNOUX_DIAG fixture=$uri name=$name path=$path")
        println("KNOUX_DIAG grants images=${permissions.mediaGrant(com.knoux.ai.data.AccessScope.MEDIA_IMAGES)}")

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val device = UiDevice.getInstance(instrumentation)
            val trash = OperationPlan(
                id = UUID.randomUUID().toString(),
                kind = OperationKind.MOVE_TO_TRASH,
                candidates = items,
                requiresSystemConfirmation = true,
                confirmedByUser = true,
            )
            val trashSender = io { controller.prepareMediaOperation(trash, acknowledgedIrreversible = false) }
                ?: throw AssertionError("Provider cannot prepare a trash request for its own fixture")
            scenario.onActivity { it.startIntentSenderForResult(trashSender, 4817, null, 0, 0, 0) }
            val trashDeadline = System.currentTimeMillis() + 20_000L
            while (System.currentTimeMillis() < trashDeadline &&
                repository.presence(items).values.single() != SmartCleanRepository.Presence.TRASHED
            ) {
                val positive = device.findObject(By.res("android", "button1"))
                if (positive != null) runCatching { positive.click() }
                Thread.sleep(200)
            }
            println("KNOUX_DIAG state_after_trash=${repository.presence(items).values.single()}")
            assertEquals(
                "trash must be observable before restore is attempted",
                SmartCleanRepository.Presence.TRASHED,
                repository.presence(items).values.single(),
            )

            val restore = io {
                val completed = controller.completeMediaOperation(trash.id, approved = true)
                val row = database.operationDao().operation(trash.id)
                val rows = database.operationDao().items(trash.id)
                println("KNOUX_DIAG trash_completion=${completed.status} removed=${completed.removedCount} note=${row?.note} outcomes=${rows.map { it.outcome }}")
                assertEquals(
                    "trash completion must be verified before restore; status=${completed.status} note=${row?.note} outcomes=${rows.map { it.outcome }} errors=${rows.map { it.error }}",
                    com.knoux.ai.data.OperationStatus.SUCCEEDED,
                    completed.status,
                )
                controller.prepareRestoreLatest() ?: throw AssertionError(
                    "A verified trash batch must be restorable; operation=$row outcomes=${rows.map { it.outcome }} " +
                        "latestTrash=${database.operationDao().latestVerifiedTrash()}"
                )
            }
            println("KNOUX_DIAG restore_plan=${restore.first}")
            scenario.onActivity { it.startIntentSenderForResult(restore.second, 4818, null, 0, 0, 0) }
            var sawButton = false
            val restoreDeadline = System.currentTimeMillis() + 10_000L
            while (System.currentTimeMillis() < restoreDeadline) {
                val positive = device.findObject(By.res("android", "button1"))
                if (positive != null) { sawButton = true; break }
                if (repository.presence(items).values.single() == SmartCleanRepository.Presence.PRESENT) break
                Thread.sleep(200)
            }
            val pkg = runCatching { device.currentPackageName }.getOrNull()
            println("KNOUX_DIAG restore_has_button1=$sawButton currentPackage=$pkg state=${repository.presence(items).values.single()}")
            val dump = File(context.cacheDir, "knoux_restore_window.xml")
            device.dumpWindowHierarchy(dump)
            println("KNOUX_DIAG restore_hierarchy_bytes=${dump.length()} path=${dump.absolutePath}")
            val shot = File(context.cacheDir, "knoux_restore_window.png")
            device.takeScreenshot(shot)
            println("KNOUX_DIAG restore_screenshot_bytes=${shot.length()} path=${shot.absolutePath}")

            // Dismiss only: this diagnostic never approves a restore/delete consent.
            device.pressBack()
            Thread.sleep(1500)
            println("KNOUX_DIAG state_after_back=${repository.presence(items).values.single()}")
            assertEquals(
                "A dismissed restore dialog must not silently mutate the fixture",
                SmartCleanRepository.Presence.TRASHED,
                repository.presence(items).values.single(),
            )
        }
    }

    private fun candidate(uri: Uri, name: String) = FileCandidate(
        uri = uri.toString(),
        displayName = name,
        mimeType = "image/png",
        bytes = 107L,
        lastModifiedMillis = System.currentTimeMillis(),
        category = CandidateCategory.IMAGE,
        provenance = DataProvenance.MEDIASTORE,
        eligibility = Eligibility.ELIGIBLE,
        reason = "Diagnostic fixture",
    )
}
