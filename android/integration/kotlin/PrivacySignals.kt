package ai.knoux.integration

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest

/** Visible permission facts about THIS app only. Other-app privacy dashboards require OS UI. */
data class OwnPermissionSignal(val name: String, val granted: Boolean)
class PrivacySignals(private val context: Context) {
    fun ownPermissions(): List<OwnPermissionSignal> = listOf(
        OwnPermissionSignal("camera", context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED),
        OwnPermissionSignal("microphone", context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    )
}
