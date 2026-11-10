package ai.knoux.integration

import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

/** Requires a real Activity/Compose launcher. Picking files does NOT give whole-disk access. */
object SafSelection {
    val openDocuments = ActivityResultContracts.OpenMultipleDocuments()
    fun singleDocumentMimeTypes(): Array<String> = arrayOf("application/pdf", "text/plain", "application/zip")
    // After callback: resolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)
    // only if the returned intent actually grants persistable access.
    fun readableUri(uri: Uri): String = uri.toString()
    const val readFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION
}
