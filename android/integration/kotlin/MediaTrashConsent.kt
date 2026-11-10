package ai.knoux.integration

import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/** Consented media trash request; it does NOT perform deletion by itself. */
object MediaTrashConsent {
    fun makeRequest(resolver: ContentResolver, chosen: List<Uri>): PendingIntent {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "Use appropriate RecoverableSecurityException / SAF path on API 26-29"
        }
        require(chosen.isNotEmpty()) { "No files selected" }
        // UI should first show the exact selected media and preserve at least one user-chosen copy.
        // Launch this PendingIntent through ActivityResultLauncher<IntentSenderRequest>.
        return MediaStore.createTrashRequest(resolver, chosen, true)
    }
}
