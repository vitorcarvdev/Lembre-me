package com.vitor.melembre.focus

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Som curto de "gota", gerado por código (sem arquivo de áudio). */
object FocusChime {
    private const val RATE = 22050

    private val samples: ShortArray by lazy {
        val count = (RATE * 0.5).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / RATE
            val freq = 520.0 + 700.0 * exp(-24.0 * t)
            phase += 2 * PI * freq / RATE
            val envelope = exp(-8.0 * t) * minOf(1.0, t * 400)
            out[i] = (sin(phase) * envelope * 0.28 * Short.MAX_VALUE).toInt().toShort()
        }
        out
    }

    fun play() {
        thread(name = "focus-chime") {
            try {
                val data = samples
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(data.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
                    .build()
                track.write(data, 0, data.size)
                track.play()
                Thread.sleep(700)
                track.release()
            } catch (_: Exception) {
            }
        }
    }
}
