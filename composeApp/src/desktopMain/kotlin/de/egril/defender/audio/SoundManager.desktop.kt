package de.egril.defender.audio

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.PI
import kotlin.math.sin

/**
 * Desktop (JVM) implementation of SoundManager factory
 */
actual fun createSoundManager(): SoundManager = FileSoundManager()

/**
 * Desktop implementation of tone playback using javax.sound
 */
@OptIn(DelicateCoroutinesApi::class)
actual fun playToneImpl(frequency: Int, durationMs: Int, volume: Float) {
    // Play tone asynchronously to avoid blocking UI
    GlobalScope.launch(Dispatchers.IO) {
        try {
            val sampleRate = 44100f
            val numSamples = (durationMs * sampleRate / 1000).toInt()
            val samples = ByteArray(numSamples * 2) // 16-bit samples
            
            // Generate sine wave
            for (i in 0 until numSamples) {
                val sample = (sin(2.0 * PI * i / (sampleRate / frequency)) * 32767 * volume).toInt()
                samples[i * 2] = (sample and 0xFF).toByte()
                samples[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
            }
            
            // Play the audio
            val audioFormat = AudioFormat(
                sampleRate,
                16,  // 16-bit
                1,   // mono
                true,  // signed
                false  // little-endian
            )
            
            val line: SourceDataLine = AudioSystem.getSourceDataLine(audioFormat)
            line.open(audioFormat)
            line.start()
            line.write(samples, 0, samples.size)
            line.drain()
            line.stop()
            line.close()
        } catch (e: Exception) {
            // Silently fail if audio playback fails
            e.printStackTrace()
        }
    }
}
