package com.knoux.ai.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Observes what the platform will actually let us read *right now*.
 *
 * Nothing here is cached between calls. A grant recorded in a previous session is
 * evidence of nothing: the customer can revoke media access from Settings at any
 * moment, and API 34 partial grants can be re-scoped. Every scan therefore starts
 * by asking the OS again.
 */
class PermissionState(private val context: Context) {

    /** True when the OS version uses granular media permissions (API 33+). */
    val usesGranularMediaPermissions: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** True when the customer may grant access to a chosen subset (API 34+). */
    val supportsPartialVisualGrant: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    /** The permission strings to request, including explicit selected-only access on API 34+. */
    fun mediaPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Observes a specific media scope independently, including API 34 selected-only access.
     * With no scope, retains the legacy "any visual media granted" observation for
     * existing callers; that aggregate is not proof of full images AND video coverage.
     */
    fun mediaGrant(scope: AccessScope? = null): PermissionGrant {
        require(scope == null || scope == AccessScope.MEDIA_IMAGES || scope == AccessScope.MEDIA_VIDEO) {
            "mediaGrant accepts only visual media scopes"
        }
        if (!usesGranularMediaPermissions) {
            return if (granted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                PermissionGrant.GRANTED
            } else {
                PermissionGrant.DENIED
            }
        }
        val images = granted(Manifest.permission.READ_MEDIA_IMAGES)
        val video = granted(Manifest.permission.READ_MEDIA_VIDEO)
        val fullyGranted = when (scope) {
            AccessScope.MEDIA_IMAGES -> images
            AccessScope.MEDIA_VIDEO -> video
            else -> images && video
        }
        if (fullyGranted) return PermissionGrant.GRANTED
        if (supportsPartialVisualGrant && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)) {
            // The customer picked specific items; anything outside that set is invisible.
            return PermissionGrant.PARTIAL_SELECTED
        }
        return if (scope == null && (images || video)) PermissionGrant.GRANTED else PermissionGrant.DENIED
    }

    /** App-owned cache never needs a permission, and saying otherwise would be wrong. */
    fun appCacheGrant(): PermissionGrant = PermissionGrant.NOT_REQUIRED

    /**
     * Persisted SAF trees. Only grants the resolver still holds are returned, so a
     * folder the customer removed from the picker stops being offered immediately.
     *
     * [requireWrite] is used wherever the caller intends to delete: removal needs a
     * writable document URI, so a read-only grant must never be advertised as a
     * folder we can clean.
     */
    fun safTrees(requireWrite: Boolean = false): List<Uri> =
        context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && (!requireWrite || it.isWritePermission) }
            .map { it.uri }

    /** A truthful one-line description for the UI. */
    fun describe(scope: AccessScope): String = when (scope) {
        AccessScope.APP_CACHE -> "Always available — KNOUX's own cache only"
        AccessScope.MEDIA_IMAGES, AccessScope.MEDIA_VIDEO -> when (mediaGrant(scope)) {
            PermissionGrant.GRANTED -> "Granted — library visible"
            PermissionGrant.PARTIAL_SELECTED -> "Partial — only selected items are visible"
            PermissionGrant.REVOKED -> "Revoked — re-grant to scan"
            else -> "Not granted"
        }
        AccessScope.SAF_TREE -> if (safTrees().isEmpty()) "No folder added" else "${safTrees().size} folder(s) granted"
    }
}
