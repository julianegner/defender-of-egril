package de.egril.defender.audio

import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.dom.Audio
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

/**
 * Web/WASM implementation of file-based sound manager
 */

private val audioCache = mutableMapOf<String, String>()

actual fun initializeAudioSystem() {
    // No specific initialization needed for web
}

actual fun playSoundFile(fileName: String, volume: Float) {
    kotlinx.coroutines.GlobalScope.launch {
        try {
            // Get or create data URL for this sound
            val dataUrl = audioCache.getOrPut(fileName) {
                try {
                    // Load bytes from compose resources using Res.readBytes
                    val resourcePath = "files/sounds/$fileName"
                    val bytes = Res.readBytes(resourcePath)
                    
                    // Create blob from bytes
                    val blob = Blob(arrayOf(bytes.toTypedArray()), BlobPropertyBag("audio/wav"))
                    
                    // Create object URL
                    org.w3c.dom.url.URL.createObjectURL(blob)
                } catch (e: Exception) {
                    println("Could not load sound: $fileName - ${e.message}")
                    e.printStackTrace()
                    ""
                }
            }
            
            if (dataUrl.isNotEmpty()) {
                // Create and play audio element
                val audio = Audio(dataUrl)
                audio.volume = volume.toDouble()
                audio.play()
            }
        } catch (e: Exception) {
            console.log("Could not play sound: $fileName - ${e.message}")
            e.printStackTrace()
        }
    }
}

actual fun releaseAudioSystem() {
    // Revoke object URLs
    audioCache.values.forEach { url ->
        try {
            org.w3c.dom.url.URL.revokeObjectURL(url)
        } catch (e: Exception) {
            // Ignore
        }
    }
    audioCache.clear()
}
