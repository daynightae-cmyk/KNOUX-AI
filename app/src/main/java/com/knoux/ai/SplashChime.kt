package com.knoux.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Splash brand chime — a bright, optimistic 3-note ascending chord that lands
 * when the neon smile completes. The sound is synthesised entirely in memory
 * (no raw asset required) so it always matches the brand palette.
 *
 * Notes: C5 (523 Hz) → E5 (659 Hz) → G5 (784 Hz), softly overlapping.
 * Attack is instant, decay is a smooth exponential so the last shimmer of the
 * chord dissolves into the app transition rather than cutting abruptly.
 */
internal class SplashChime(context: Context) {
    private val appCtx = context.applicationContext
    private val audio = appCtx.getSystemService(AudioManager::class.java)
    private val track: AudioTrack? = buildChime()
    private var played = false

    private fun buildChime(): AudioTrack? = runCatching {
        val sampleRate = 44100
        val durationMs = 1200
        val samples = sampleRate * durationMs / 1000
        val buf = ShortArray(samples)

        // Three ascending notes with staggered onsets (ms)
        data class Note(val freq: Double, val onsetMs: Int, val gainDb: Float = 0f)
        val notes = listOf(
            Note(523.25, 0,   -3f),   // C5 — root, full length
            Note(659.25, 90, -1.5f),  // E5 — third, slightly delayed
            Note(783.99, 180, 0f)     // G5 — fifth, arrives last and brightest
        )
        val gain = 0.28f // mix headroom

        for (note in notes) {
            val onsetSample = (note.onsetMs * sampleRate / 1000)
            val ampLin = gain * Math.pow(10.0, note.gainDb / 20.0).toFloat()
            for (i in onsetSample until samples) {
                val t = (i - onsetSample).toDouble() / sampleRate
                // Sine fundamental + soft 2nd harmonic (makes it sound like a bell/chime)
                val sine = sin(2 * PI * note.freq * t) +
                           0.18 * sin(4 * PI * note.freq * t) +
                           0.06 * sin(6 * PI * note.freq * t)
                // Exponential decay: fast attack, slow tail
                val envelope = kotlin.math.exp(-t * 2.8)
                // Fade-out the last 15% of the buffer (smooth exit)
                val fadeOut = if (i > samples * 0.85) (samples - i).toDouble() / (samples * 0.15) else 1.0
                buf[i] = (buf[i] + (ampLin * sine * envelope * fadeOut * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767)).toShort()
            }
        }

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()

        AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(buf.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC).build()
            .also { it.write(buf, 0, buf.size) }
    }.getOrNull()

    fun play() {
        if (played || !audible()) return
        played = true
        runCatching {
            track?.reloadStaticData()
            track?.play()
            if (BuildConfig.DEBUG) Log.d("KnouxSplash", "Chime played")
        }
    }

    private fun audible(): Boolean {
        val am = audio ?: return false
        return am.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
               !am.isStreamMute(AudioManager.STREAM_SYSTEM) &&
               am.getStreamVolume(AudioManager.STREAM_SYSTEM) > 0
    }

    fun release() { runCatching { track?.stop(); track?.release() } }
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
