@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.egril.defender.audio

import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * External JS functions for Blob and Audio API
 */
@JsFun("(size) => new Uint8Array(size)")
external fun createUint8Array(size: Int): JsAny

@JsFun("(array, index, value) => array[index] = value")
external fun setUint8ArrayValue(array: JsAny, index: Int, value: Byte)

@JsFun("(array, mimeType) => new Blob([array], { type: mimeType })")
external fun createBlob(array: JsAny, mimeType: String): JsAny

@JsFun("(blob) => URL.createObjectURL(blob)")
external fun createObjectURL(blob: JsAny): String

@JsFun("(url) => URL.revokeObjectURL(url)")
external fun revokeObjectURL(url: String)

@JsFun("(src) => new Audio(src)")
external fun createAudio(src: String): JsAny

@JsFun("(audio, volume) => audio.volume = volume")
external fun setAudioVolume(audio: JsAny, volume: Double)

@JsFun("(audio) => audio.play()")
external fun playAudio(audio: JsAny)

/**
 * Web/WASM implementation of file-based sound manager
 */

private val audioCache = mutableMapOf<String, String>()

actual fun initializeAudioSystem() {
    // No specific initialization needed for web
}

actual fun playSoundFile(fileName: String, volume: Float) {
    GlobalScope.launch {
        try {
            // Get or create data URL for this sound
            val dataUrl = audioCache.getOrPut(fileName) {
                try {
                    // Load bytes from compose resources using Res.readBytes
                    val resourcePath = "files/sounds/$fileName"
                    val bytes = Res.readBytes(resourcePath)
                    
                    // Create Uint8Array and copy bytes
                    val uint8Array = createUint8Array(bytes.size)
                    bytes.forEachIndexed { index, byte ->
                        setUint8ArrayValue(uint8Array, index, byte)
                    }
                    
                    // Create blob from Uint8Array
                    val blob = createBlob(uint8Array, "audio/wav")
                    
                    // Create object URL
                    createObjectURL(blob)
                } catch (e: Exception) {
                    println("Could not load sound: $fileName - ${e.message}")
                    e.printStackTrace()
                    ""
                }
            }
            
            if (dataUrl.isNotEmpty()) {
                // Create and play audio element
                val audio = createAudio(dataUrl)
                setAudioVolume(audio, volume.toDouble())
                playAudio(audio)
            }
        } catch (e: Exception) {
            println("Could not play sound: $fileName - ${e.message}")
            e.printStackTrace()
        }
    }
}

actual fun releaseAudioSystem() {
    // Revoke object URLs
    audioCache.values.forEach { url ->
        try {
            revokeObjectURL(url)
        } catch (e: Exception) {
            // Ignore
        }
    }
    audioCache.clear()
}
