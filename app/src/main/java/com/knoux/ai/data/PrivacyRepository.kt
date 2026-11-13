package com.knoux.ai.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.security.NetworkSecurityPolicy
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PC04 — real privacy observation.
 *
 * Every row is produced by asking Android about **this package only**. The app
 * deliberately does not request QUERY_ALL_PACKAGES and does not read other apps'
 * permission state: an unprivileged app cannot do that, and pretending otherwise
 * would be the exact "privacy score" theatre this service must not ship.
 *
 * Nothing here is cached. A grant can be revoked, a notification channel disabled
 * or a folder unshared between two sweeps, so [observe] re-reads the OS each time.
 */
class PrivacyRepository(
    private val context: Context,
    private val permissions: PermissionState,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /** The measured rows plus an honest note about anything that degraded. */
    data class Observation(
        val rows: List<PrivacyObservation>,
        val note: String?,
    )

    /**
     * @param windowSecure the *actual* FLAG_SECURE state of the calling window,
     *   read by the screen after KNOUX applied its setting. Passing the stored
     *   preference instead would let the screen claim a state the OS never got.
     */
    suspend fun observe(windowSecure: Boolean): Observation = withContext(ioDispatcher) {
        val packageName = context.packageName
        val manager = context.packageManager
        @Suppress("DEPRECATION")
        val info = manager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions?.toSet().orEmpty()
        val app = info.applicationInfo
        val notes = mutableListOf<String>()

        // ── Network egress ───────────────────────────────────────────────────
        val network = if (Manifest.permission.INTERNET !in requested) {
            PrivacyRules.pass(
                PrivacyCheck.NETWORK_EGRESS,
                PrivacySeverity.HIGH,
                "android.permission.INTERNET is not declared",
                "Android cannot open a socket for this app, so the sweep and its history stay on this device.",
            )
        } else {
            PrivacyRules.attention(
                PrivacyCheck.NETWORK_EGRESS,
                PrivacySeverity.HIGH,
                "android.permission.INTERNET is declared",
                "This build can reach the network. KNOUX cannot revoke its own manifest permission; review the app's data access in Android settings.",
                PrivacyRemedy.OS_SCREEN,
            )
        }

        // ── All-files access ─────────────────────────────────────────────────
        val allFiles = if (Manifest.permission.MANAGE_EXTERNAL_STORAGE !in requested) {
            PrivacyRules.pass(
                PrivacyCheck.ALL_FILES_ACCESS,
                PrivacySeverity.HIGH,
                "MANAGE_EXTERNAL_STORAGE is not declared",
                "KNOUX can only see media through a media grant or folders the customer picked in the system picker.",
            )
        } else {
            PrivacyRules.attention(
                PrivacyCheck.ALL_FILES_ACCESS,
                PrivacySeverity.HIGH,
                "MANAGE_EXTERNAL_STORAGE is declared",
                "All-files access is broader than any KNOUX service needs. Review or revoke it in Android settings.",
                PrivacyRemedy.OS_SCREEN,
            )
        }

        // ── Sensitive permission groups ──────────────────────────────────────
        val declaredSensitive = SENSITIVE_PERMISSIONS.filter { it in requested }
        val sensitive = if (declaredSensitive.isEmpty()) {
            PrivacyRules.pass(
                PrivacyCheck.SENSITIVE_PERMISSIONS,
                PrivacySeverity.HIGH,
                "none of ${SENSITIVE_PERMISSIONS.size} sensitive permissions declared",
                "No location, camera, microphone, contacts, calendar, SMS, call-log, body-sensor or nearby-device access is requested.",
            )
        } else {
            PrivacyRules.attention(
                PrivacyCheck.SENSITIVE_PERMISSIONS,
                PrivacySeverity.HIGH,
                declaredSensitive.joinToString(", ") { it.substringAfterLast('.') },
                "These permissions are declared in the manifest. Review their grant state in Android settings; KNOUX does not read them for any service.",
                PrivacyRemedy.OS_SCREEN,
            )
        }

        // ── The media grant KNOUX genuinely uses ─────────────────────────────
        val mediaGrant = permissions.mediaGrant()
        val media = when (mediaGrant) {
            PermissionGrant.GRANTED -> PrivacyRules.pass(
                PrivacyCheck.MEDIA_ACCESS,
                PrivacySeverity.INFO,
                "media library grant: full (${permissions.describeMediaScopes()})",
                "Cleanup needs this while it runs. The customer can keep it limited to selected items or revoke it in Android settings.",
            ).copy(remedy = PrivacyRemedy.OS_SCREEN)

            PermissionGrant.PARTIAL_SELECTED -> PrivacyRules.pass(
                PrivacyCheck.MEDIA_ACCESS,
                PrivacySeverity.INFO,
                "media library grant: selected items only",
                "Only the items chosen in the system picker are visible; everything else stays hidden.",
            ).copy(remedy = PrivacyRemedy.OS_SCREEN)

            PermissionGrant.DENIED, PermissionGrant.REVOKED -> PrivacyRules.pass(
                PrivacyCheck.MEDIA_ACCESS,
                PrivacySeverity.INFO,
                "media library grant: not held",
                "KNOUX cannot read the photo or video library in this state. Media cleanup is unavailable until the customer grants a scope.",
            )

            PermissionGrant.NOT_REQUIRED -> PrivacyRules.pass(
                PrivacyCheck.MEDIA_ACCESS,
                PrivacySeverity.INFO,
                "media library grant: not required",
                "This Android version does not gate visual media behind a runtime grant for this scope.",
            )
        }

        // ── Persisted SAF trees ──────────────────────────────────────────────
        val grantedTrees = permissions.safTrees()
        val writableTrees = permissions.safTrees(requireWrite = true)
        val saf = PrivacyRules.pass(
            PrivacyCheck.SAF_WRITE_GRANTS,
            PrivacySeverity.INFO,
            when {
                grantedTrees.isEmpty() -> "no persisted folder grants"
                writableTrees.isEmpty() -> "${grantedTrees.size} read-only folder grant(s)"
                else -> "${writableTrees.size} writable folder grant(s)"
            },
            "Persisted folders are granted by the customer through the system picker and can be removed there at any time. KNOUX stores only the rules for them.",
        ).copy(remedy = if (grantedTrees.isEmpty()) PrivacyRemedy.NONE else PrivacyRemedy.OS_SCREEN)

        // ── Build properties: measured, but not customer settings ───────────
        val backupAllowed = app != null && (app.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        val backup = PrivacyObservation(
            check = PrivacyCheck.BACKUP_ALLOWED,
            status = if (backupAllowed) PrivacyStatus.ATTENTION else PrivacyStatus.PASS,
            severity = PrivacySeverity.INFO,
            observed = "android:allowBackup=${if (backupAllowed) "true" else "false"}",
            detail = if (backupAllowed) {
                "The OS may include KNOUX data in a device backup. Stock Android has no per-app backup toggle, so this is reported as a build fact, not a customer action."
            } else {
                "KNOUX opts out of OS backup, so its local history is not copied into a cloud backup by Android."
            },
            remedy = PrivacyRemedy.NONE,
        )

        val cleartext = runCatching { NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted }
            .getOrElse {
                notes += "The platform refused the cleartext-traffic query."
                null
            }
        val cleartextRow = when (cleartext) {
            null -> PrivacyRules.unmeasurable(
                PrivacyCheck.CLEARTEXT_TRAFFIC,
                "Android did not answer the network security policy query on this device.",
            )
            true -> PrivacyObservation(
                check = PrivacyCheck.CLEARTEXT_TRAFFIC,
                status = PrivacyStatus.ATTENTION,
                severity = PrivacySeverity.INFO,
                observed = "cleartext traffic permitted",
                detail = "The platform would allow unencrypted HTTP. This build declares no INTERNET permission, so the setting is dormant; it is reported as a build fact, not a customer action.",
                remedy = PrivacyRemedy.NONE,
            )
            false -> PrivacyRules.pass(
                PrivacyCheck.CLEARTEXT_TRAFFIC,
                PrivacySeverity.INFO,
                "cleartext traffic blocked",
                "The platform refuses unencrypted HTTP for this app.",
            )
        }

        val debuggable = app != null && (app.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val debugRow = PrivacyObservation(
            check = PrivacyCheck.DEBUGGABLE_BUILD,
            status = if (debuggable) PrivacyStatus.ATTENTION else PrivacyStatus.PASS,
            severity = PrivacySeverity.INFO,
            observed = if (debuggable) "debuggable=true (test build)" else "debuggable=false",
            detail = if (debuggable) {
                "This is a debug-signed test build, not a release artefact. A debuggable process allows debugging tools to attach; the release build must not be debuggable."
            } else {
                "The installed build is not debuggable."
            },
            remedy = PrivacyRemedy.NONE,
        )

        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val notifications = PrivacyRules.pass(
            PrivacyCheck.NOTIFICATIONS,
            PrivacySeverity.INFO,
            if (notificationsEnabled) "notifications enabled" else "notifications disabled",
            if (notificationsEnabled) {
                "Notification content and lock-screen visibility are controlled by Android, not by this sweep."
            } else {
                "The OS currently suppresses KNOUX notifications."
            },
        ).copy(remedy = if (notificationsEnabled) PrivacyRemedy.OS_SCREEN else PrivacyRemedy.NONE)

        // ── KNOUX's own window protection ────────────────────────────────────
        val secure = if (windowSecure) {
            PrivacyRules.pass(
                PrivacyCheck.SCREENSHOT_PROTECTION,
                PrivacySeverity.LOW,
                "FLAG_SECURE is set on the KNOUX window",
                "Android excludes this window from screenshots and the recents thumbnail while the flag is applied.",
            )
        } else {
            PrivacyRules.attention(
                PrivacyCheck.SCREENSHOT_PROTECTION,
                PrivacySeverity.LOW,
                "FLAG_SECURE is not set on the KNOUX window",
                "Screenshots and the recents thumbnail can capture this screen. Turning on screenshot protection applies a real window flag that KNOUX can verify.",
                PrivacyRemedy.APP_SETTING,
            )
        }

        // ── OS privacy dashboard availability ────────────────────────────────
        val dashboard = privacyDashboardIntent()
        val dashboardRow = if (dashboard == null) {
            PrivacyRules.unmeasurable(
                PrivacyCheck.ANDROID_PRIVACY_DASHBOARD,
                "This Android version does not expose the privacy dashboard to third-party apps; use the system Settings app directly.",
            )
        } else {
            PrivacyRules.pass(
                PrivacyCheck.ANDROID_PRIVACY_DASHBOARD,
                PrivacySeverity.INFO,
                "the Android privacy dashboard is available",
                "The OS privacy dashboard shows which apps used camera, microphone and location. KNOUX can open it; it cannot read or change it.",
            ).copy(remedy = PrivacyRemedy.OS_SCREEN)
        }

        // ── Other apps: always out of reach ──────────────────────────────────
        val otherApps = PrivacyRules.unmeasurable(
            PrivacyCheck.OTHER_APP_PRIVACY,
            "Android does not expose other apps' permission grants, app-ops history or tracker SDKs to an unprivileged app. KNOUX will not invent those numbers; use the OS privacy dashboard or the app's own page in Settings.",
        )

        val rows = listOf(
            network, allFiles, sensitive, media, saf, backup, cleartextRow,
            notifications, secure, debugRow, dashboardRow, otherApps,
        )
        val problems = PrivacyRules.validate(rows)
        if (problems.isNotEmpty()) {
            notes += "Observation validation: ${problems.joinToString("; ")}"
        }
        Observation(PrivacyRules.sanitize(rows), notes.joinToString(" ").takeIf { it.isNotBlank() })
    }

    /** Resolves the OS privacy dashboard; `null` when this platform has none. */
    fun privacyDashboardIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS)
        return if (intent.resolveActivity(context.packageManager) != null) intent else null
    }

    /**
     * The real OS-owned screen for a check, or `null` when the platform has none.
     * Resolution is checked against the live package manager, so the UI never
     * offers a button that opens nothing.
     */
    fun osScreenIntent(check: PrivacyCheck): Intent? {
        val candidate = when (check) {
            PrivacyCheck.ALL_FILES_ACCESS, PrivacyCheck.SENSITIVE_PERMISSIONS,
            PrivacyCheck.MEDIA_ACCESS, PrivacyCheck.SAF_WRITE_GRANTS ->
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )

            PrivacyCheck.NETWORK_EGRESS, PrivacyCheck.OTHER_APP_PRIVACY ->
                Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)

            PrivacyCheck.NOTIFICATIONS ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

            PrivacyCheck.ANDROID_PRIVACY_DASHBOARD -> privacyDashboardIntent()

            PrivacyCheck.SCREENSHOT_PROTECTION,
            PrivacyCheck.BACKUP_ALLOWED,
            PrivacyCheck.CLEARTEXT_TRAFFIC,
            PrivacyCheck.DEBUGGABLE_BUILD,
            -> null
        }
        return candidate?.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    private fun PermissionState.describeMediaScopes(): String =
        if (usesGranularMediaPermissions) "images and video" else "external storage"

    private companion object {
        /**
         * Permission groups that a cleaning product has no reason to hold. Media
         * access is measured separately because it is the one grant KNOUX uses.
         */
        val SENSITIVE_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.GET_ACCOUNTS,
        )
    }
}
