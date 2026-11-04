package com.lelesoftware.app.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object AudioSynthesizer {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playSignalTone(filteredSignal: Float) {
        scope.launch {
            try {
                // If signal near 0 (neutral), subtle tick or silent
                if (kotlin.math.abs(filteredSignal) < 8f) return@launch

                val isVoid = filteredSignal < 0f
                val freq = if (isVoid) {
                    // Deep hum for cavity/void: 120Hz down to 60Hz
                    (120f + (filteredSignal / 100f) * 60f).coerceIn(55f, 220f)
                } else {
                    // High pitch beep for metal/gold: 440Hz up to 1400Hz
                    (440f + (filteredSignal / 100f) * 960f).coerceIn(300f, 1800f)
                }

                val durationMs = if (kotlin.math.abs(filteredSignal) > 60f) 120 else 70
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val angle = 2.0 * PI * i / (sampleRate / freq)
                    // Apply attack/decay envelope
                    val envelope = when {
                        i < numSamples * 0.15f -> i / (numSamples * 0.15f)
                        i > numSamples * 0.8f -> (numSamples - i) / (numSamples * 0.2f)
                        else -> 1f
                    }
                    val sample = (sin(angle) * 16000 * envelope).toInt().toShort()
                    buffer[i] = sample
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 30L)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Audio device unavailable or background
            }
        }
    }
}
