package ai.knoux.integration

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Real data only for records this app is permitted to see; never bypass scoped storage. */
data class VisibleMedia(val uri: Uri, val kind: String, val sizeBytes: Long)

class MediaInventory(private val context: Context) {
    /** Caller MUST check/request granular Android media permissions or use user-selected Photo Picker. */
    suspend fun listAccessibleImages(): List<VisibleMedia> = withContext(Dispatchers.IO) {
        val base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val columns = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.SIZE)
        val result = mutableListOf<VisibleMedia>()
        context.contentResolver.query(base, columns, null, null, null)?.use { cursor: Cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            while (cursor.moveToNext()) {
                val n = cursor.getLong(sizeIndex)
                if (n >= 0L) result += VisibleMedia(
                    ContentUris.withAppendedId(base, cursor.getLong(idIndex)), "image", n
                )
            }
        }
        result
    }
}
