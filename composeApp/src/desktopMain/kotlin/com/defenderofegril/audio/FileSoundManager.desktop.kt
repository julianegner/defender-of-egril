package com.defenderofegril.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineUnavailableException

/**
 * Desktop implementation of file-based sound manager
 */

actual fun initializeAudioSystem() {
    // Initialize audio system - nothing specific needed for desktop
}

actual fun playSoundFile(fileName: String, volume: Float) {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            // Create a new clip instance for concurrent playback
            val resourceStream = object {}.javaClass.getResourceAsStream("/files/sounds/$fileName")
            if (resourceStream == null) {
                println("Resource not found: /files/sounds/$fileName")
                return@launch
            }
            val audioInputStream = AudioSystem.getAudioInputStream(BufferedInputStream(resourceStream))

            val playClip = AudioSystem.getClip()
            playClip.open(audioInputStream)
            
            // Set volume
            if (playClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = playClip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val range = gainControl.maximum - gainControl.minimum
                val gain = gainControl.minimum + range * volume
                gainControl.value = gain.coerceIn(gainControl.minimum, gainControl.maximum)
            }
            
            // Play sound
            playClip.start()
            
            // Clean up after playback
            playClip.addLineListener { event ->
                if (event.type == javax.sound.sampled.LineEvent.Type.STOP) {
                    playClip.close()
                }
            }
        } catch (e: Exception) {
            // Silently fail if sound file not found or can't be played
            println("Could not play sound: $fileName - ${e.message}")
        }
    }
}

actual fun releaseAudioSystem() {
    // No cleanup needed for desktop
}
