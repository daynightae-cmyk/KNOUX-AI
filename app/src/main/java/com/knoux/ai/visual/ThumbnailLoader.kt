package com.knoux.ai.visual

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes a thumbnail from a content URI using BitmapFactory. Returns null
 * when the file cannot be read.
 */
@Composable
fun rememberThumbnail(
    uriString: String?,
    contentResolver: ContentResolver = LocalContext.current.contentResolver,
    targetSize: Int = 96,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Bitmap? {
    val state = remember { mutableStateOf<Bitmap?>(null) }
    val resolver = rememberUpdatedState(contentResolver)
    val target = rememberUpdatedState(targetSize)
    val uri = remember(uriString) { uriString?.let { Uri.parse(it) } }

    LaunchedEffect(uri, resolver.value, target.value) {
        state.value = null
        if (uri != null) {
            val bitmap = withContext(dispatcher) {
                decodeThumbnail(uri, resolver.value, target.value)
            }
            state.value = bitmap
        }
    }

    return state.value
}

private fun decodeThumbnail(
    uri: Uri,
    resolver: ContentResolver,
    targetSize: Int,
): Bitmap? {
    return try {
        val inputStream = resolver.openInputStream(uri) ?: return null
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize = computeSampleSize(resolver, uri, targetSize)
            BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
            inputStream.close()
        }
    } catch (_: Exception) {
        null
    }
}

private fun computeSampleSize(resolver: ContentResolver, uri: Uri, targetSize: Int): Int {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    return options.outWidth.coerceAtLeast(1) / targetSize.coerceAtLeast(1)
}
