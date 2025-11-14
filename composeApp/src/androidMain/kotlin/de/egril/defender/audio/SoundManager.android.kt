package de.egril.defender.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Android implementation of SoundManager factory
 */
actual fun createSoundManager(): SoundManager = FileSoundManager()

/**
 * Android implementation of tone playback using AudioTrack
 */
actual fun playToneImpl(frequency: Int, durationMs: Int, volume: Float) {
    // Play tone asynchronously to avoid blocking UI
    GlobalScope.launch(Dispatchers.Default) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)
            
            // Generate sine wave
            for (i in samples.indices) {
                val sample = (sin(2.0 * PI * i / (sampleRate / frequency.toDouble())) * 32767 * volume).toInt()
                samples[i] = sample.toShort()
            }
            
            // Create and play audio track
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            
            // Clean up after playback
            Thread.sleep(durationMs.toLong())
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            // Silently fail if audio playback fails
            e.printStackTrace()
        }
    }
}
