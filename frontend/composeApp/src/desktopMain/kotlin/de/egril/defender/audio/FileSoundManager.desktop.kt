package de.egril.defender.audio

import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl

/**
 * Desktop implementation of file-based sound manager
 */

actual fun initializeAudioSystem() {
    // Initialize audio system - nothing specific needed for desktop
}

@OptIn(DelicateCoroutinesApi::class)
actual fun playSoundFile(fileName: String, volume: Float) {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            // Load audio file from compose resources
            val resourcePath = "files/sounds/$fileName"
            val bytes = try {
                // Use Res.readBytes to load from compose resources
                Res.readBytes(resourcePath)
            } catch (e: Exception) {
                println("Could not load resource from compose resources: $resourcePath - ${e.message}")
                return@launch
            }
            
            // Create audio input stream from bytes
            val audioInputStream = AudioSystem.getAudioInputStream(
                BufferedInputStream(ByteArrayInputStream(bytes))
            )
            
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
            e.printStackTrace()
        }
    }
}

actual fun releaseAudioSystem() {
    // No cleanup needed for desktop
}
