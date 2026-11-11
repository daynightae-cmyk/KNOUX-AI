package com.knoux.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * The tiny glassy tap that lands when the neon smile completes.
 *
 * The sample is preloaded and the tap is skipped if it misses its cue.
 * SoundPool and its load callback are owned by the main thread.
 */
internal class SplashChime(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var loaded = false
    private var played = false
    private val sample: Int

    init {
        pool.setOnLoadCompleteListener { _, _, status ->
            loaded = status == 0
        }
        sample = pool.load(app, R.raw.knoux_smile_click, 1)
    }

    fun play() {
        if (!loaded || played || !audible()) return
        played = pool.play(sample, .85f, .85f, 1, 0, 1f) != 0
        if (BuildConfig.DEBUG && played) Log.d("KnouxSplash", "Smile click started")
    }

    private fun audible(): Boolean {
        val manager = audio ?: return false
        val stream = AudioManager.STREAM_SYSTEM
        return manager.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
            !manager.isStreamMute(stream) && manager.getStreamVolume(stream) > 0
    }

    fun release() {
        pool.setOnLoadCompleteListener(null)
        pool.release()
    }
}

@Composable
internal fun rememberSplashChime(enabled: Boolean): SplashChime? {
    val context = LocalContext.current
    var chime by remember { mutableStateOf<SplashChime?>(null) }
    DisposableEffect(context, enabled) {
        val player = if (enabled) SplashChime(context) else null
        chime = player
        onDispose { player?.release() }
    }
    return chime
}
