package com.defenderofegril.audio

import kotlinx.browser.window
import org.w3c.dom.Audio

/**
 * Web/WASM implementation of file-based sound manager
 */

private val audioCache = mutableMapOf<String, Audio>()

actual fun initializeAudioSystem() {
    // No specific initialization needed for web
}

actual fun playSoundFile(fileName: String, volume: Float) {
    try {
        // Get or create audio element
        val audio = audioCache.getOrPut(fileName) {
            Audio("files/sounds/$fileName").apply {
                // Preload the audio
                this.load()
            }
        }
        
        // Clone the audio element for concurrent playback
        val playAudio = Audio(audio.src)
        playAudio.volume = volume.toDouble()
        
        // Play the sound
        playAudio.play()
    } catch (e: Exception) {
        console.log("Could not play sound: $fileName - ${e.message}")
    }
}

actual fun releaseAudioSystem() {
    audioCache.values.forEach { audio ->
        try {
            audio.pause()
            audio.src = ""
        } catch (e: Exception) {
            // Ignore
        }
    }
    audioCache.clear()
}
