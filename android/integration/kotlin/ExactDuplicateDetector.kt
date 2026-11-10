package ai.knoux.integration

import android.content.ContentResolver
import android.net.Uri
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Local-only exact duplicate detection. Near duplicates require a separate algorithm and consent. */
data class ExactDuplicateGroup(val sha256: String, val bytes: Long, val items: List<Uri>)

class ExactDuplicateDetector(private val resolver: ContentResolver) {
    suspend fun find(items: List<VisibleMedia>): List<ExactDuplicateGroup> = withContext(Dispatchers.IO) {
        // Only stream hashes for matching sizes. Never download or upload user content.
        val groups = mutableListOf<ExactDuplicateGroup>()
        for (sizeGroup in items.filter { it.sizeBytes > 0L }.groupBy { it.sizeBytes }.values) {
            if (sizeGroup.size < 2) continue
            val byHash = mutableMapOf<String, MutableList<Uri>>()
            for (item in sizeGroup) {
                // Permission or I/O failure means "not scanned", not "unique".
                val hash = try { digest(item.uri) } catch (_: Exception) { null }
                if (hash != null) byHash.getOrPut(hash) { mutableListOf() }.add(item.uri)
            }
            for ((hash, uris) in byHash) if (uris.size > 1) {
                groups += ExactDuplicateGroup(hash, sizeGroup.first().sizeBytes, uris.toList())
            }
        }
        groups
    }

    private fun digest(uri: Uri): String? {
        val md = MessageDigest.getInstance("SHA-256")
        val input = resolver.openInputStream(uri) ?: return null
        input.use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                if (n > 0) md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
