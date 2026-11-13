package com.knoux.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.ai.data.KnouxDatabase
import com.knoux.ai.data.PermissionState
import com.knoux.ai.data.PrivacyAction
import com.knoux.ai.data.PrivacyCheck
import com.knoux.ai.data.PrivacyController
import com.knoux.ai.data.PrivacyRemedy
import com.knoux.ai.data.PrivacyRepository
import com.knoux.ai.data.PrivacyRules
import com.knoux.ai.data.PrivacySeverity
import com.knoux.ai.data.PrivacyStatus
import com.knoux.ai.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * PC04 real-behaviour proof on the owner-authorized phone.
 *
 * The sweep inspects this app's own package only. No personal file, MediaStore row,
 * clipboard or other app is read or changed, so there is nothing to clean up beyond
 * the in-memory database and a temporary DataStore directory.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyStorageTest {

    private lateinit var context: Context
    private lateinit var database: KnouxDatabase
    private lateinit var settings: SettingsStore
    private lateinit var repository: PrivacyRepository
    private lateinit var controller: PrivacyController
    private lateinit var testDataStoreDir: File

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    @Before fun createIsolatedDependencies() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        context = instrumentation.targetContext
        assertEquals("com.knoux.ai", context.packageName)
        database = Room.inMemoryDatabaseBuilder(context, KnouxDatabase::class.java).build()
        testDataStoreDir = File(context.filesDir, "knoux_privacy_ds_${UUID.randomUUID()}").also { it.mkdirs() }
        val dataStore = PreferenceDataStoreFactory.create {
            File(testDataStoreDir, "settings.preferences_pb")
        }
        settings = SettingsStore(dataStore)
        repository = PrivacyRepository(context, PermissionState(context))
        controller = PrivacyController(repository, database, settings)
    }

    @After fun removeOnlyTheTemporaryStore() {
        database.close()
        if (::testDataStoreDir.isInitialized) {
            assertTrue(!testDataStoreDir.exists() || testDataStoreDir.deleteRecursively())
        }
    }

    @Test fun theSweepMeasuresEveryCheckExactlyOnceAndPersistsIt() {
        val report = io { controller.measure(windowSecure = false).getOrThrow() }
        assertEquals(PrivacyCheck.entries.size, report.rows.size)
        assertEquals(report.rows.size, report.rows.map { it.check }.distinct().size)
        assertEquals(PrivacyCheck.entries.toSet(), report.rows.map { it.check }.toSet())
        assertTrue("the pass must be structurally coherent", PrivacyRules.validate(report.rows).isEmpty())

        // The network row must match the manifest this build really has.
        @Suppress("DEPRECATION")
        val declared = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toSet()
        val network = report.rows.single { it.check == PrivacyCheck.NETWORK_EGRESS }
        assertEquals(
            if (Manifest.permission.INTERNET in declared) PrivacyStatus.ATTENTION else PrivacyStatus.PASS,
            network.status,
        )
        assertTrue(network.observed.isNotBlank())

        // Other apps are never claimed measurable.
        val other = report.rows.single { it.check == PrivacyCheck.OTHER_APP_PRIVACY }
        assertEquals(PrivacyStatus.UNMEASURABLE, other.status)
        assertEquals(PrivacyRemedy.NOT_POSSIBLE, other.remedy)

        // A window without FLAG_SECURE is an attention item, not a pass.
        val secure = report.rows.single { it.check == PrivacyCheck.SCREENSHOT_PROTECTION }
        assertEquals(PrivacyStatus.ATTENTION, secure.status)
        assertEquals(PrivacyRemedy.APP_SETTING, secure.remedy)

        assertEquals(PrivacyRules.score(report.rows), report.summary.score)
        assertNotNull("the scored checks must produce a real score", report.summary.score)

        val stored = io { controller.latest() }!!
        assertEquals(report.scanId, stored.scanId)
        assertEquals(report.rows.size, io { database.privacyDao().findings(report.scanId).size })
        assertTrue(
            io { controller.observeAudit().first() }.any {
                it.scanId == report.scanId && it.action == PrivacyAction.MEASURED
            }
        )
    }

    @Test fun theRealWindowFlagChangesTheRowAndTheScore() {
        val off = io { controller.measure(windowSecure = false).getOrThrow() }
        val on = io { controller.measure(windowSecure = true).getOrThrow() }
        assertNotEquals(off.scanId, on.scanId)
        assertEquals(
            PrivacyStatus.ATTENTION,
            off.rows.single { it.check == PrivacyCheck.SCREENSHOT_PROTECTION }.status,
        )
        assertEquals(
            PrivacyStatus.PASS,
            on.rows.single { it.check == PrivacyCheck.SCREENSHOT_PROTECTION }.status,
        )
        assertEquals(PrivacySeverity.LOW, on.rows.single { it.check == PrivacyCheck.SCREENSHOT_PROTECTION }.severity)
        assertEquals(
            "turning the real flag on must raise the score by exactly the low penalty",
            off.summary.score!! + 8,
            on.summary.score,
        )
        assertEquals(on.scanId, io { controller.latest() }!!.scanId)
    }

    @Test fun androidScreensAreOfferedOnlyWhenTheyResolve() {
        assertNotNull(
            "the app details screen must exist for the media-permission row",
            repository.osScreenIntent(PrivacyCheck.MEDIA_ACCESS),
        )
        assertNotNull(repository.osScreenIntent(PrivacyCheck.SENSITIVE_PERMISSIONS))
        assertNull("there is no OS screen for the build's debuggable flag", repository.osScreenIntent(PrivacyCheck.DEBUGGABLE_BUILD))
        assertNull(repository.osScreenIntent(PrivacyCheck.BACKUP_ALLOWED))

        val dashboardResolvable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            Intent(Settings.ACTION_PRIVACY_SETTINGS).resolveActivity(context.packageManager) != null
        assertEquals(dashboardResolvable, repository.privacyDashboardIntent() != null)
        assertEquals(
            dashboardResolvable,
            repository.osScreenIntent(PrivacyCheck.ANDROID_PRIVACY_DASHBOARD) != null,
        )
    }

    @Test fun launchingAnAndroidScreenIsRecordedAsOpenedNotVerified() {
        val report = io { controller.measure(windowSecure = false).getOrThrow() }
        io { controller.recordOsScreenOpened(PrivacyCheck.MEDIA_ACCESS, resolved = true, scanId = report.scanId) }
        val entry = io { controller.observeAudit().first() }
            .first { it.scanId == report.scanId && it.check == PrivacyCheck.MEDIA_ACCESS }
        assertEquals(PrivacyAction.OS_SCREEN_OPENED, entry.action)
        assertEquals("OPENED", entry.outcome)
        assertFalse(
            "opening Settings is not a verified privacy fix",
            entry.action == PrivacyAction.SETTING_VERIFIED,
        )
    }

    @Test fun screenshotProtectionIsVerifiedOnlyWhenTheWindowFlagMatches() {
        io { controller.setSecureWindowPreference(true) }
        assertTrue(io { settings.privacySecureWindow.first() })

        assertFalse("a requested flag that the window does not hold is a mismatch", io {
            controller.verifySecureWindow(requested = true, actualSecure = false, scanId = null)
        })
        assertTrue(io {
            controller.verifySecureWindow(requested = true, actualSecure = true, scanId = null)
        })

        val audit = io { controller.observeAudit().first() }
        val screenshot = audit.filter { it.check == PrivacyCheck.SCREENSHOT_PROTECTION }
        assertTrue(screenshot.any { it.outcome == "MISMATCH" })
        assertTrue(screenshot.any { it.action == PrivacyAction.SETTING_VERIFIED && it.outcome == "VERIFIED" })

        io { controller.setSecureWindowPreference(false) }
        assertFalse(io { settings.privacySecureWindow.first() })
    }
}
