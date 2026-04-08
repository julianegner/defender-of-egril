package de.egril.defender.audio

import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import platform.AVFAudio.*
import platform.Foundation.*

/**
 * iOS implementation of file-based sound manager
 */

private val audioPlayers = mutableMapOf<String, AVAudioPlayer>()

@OptIn(ExperimentalForeignApi::class)
actual fun initializeAudioSystem() {
    try {
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryPlayback, null)
        audioSession.setActive(true, null)
    } catch (e: Exception) {
        println("Could not initialize audio session: ${e.message}")
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun playSoundFile(fileName: String, volume: Float) {
    GlobalScope.launch(Dispatchers.Main) {
        try {
            // Load audio data from compose resources using Res.readBytes
            val resourcePath = "files/sounds/$fileName"
            val bytes = try {
                Res.readBytes(resourcePath)
            } catch (e: Exception) {
                println("Could not load resource: $resourcePath - ${e.message}")
                return@launch
            }
            
            // Create NSData from bytes
            val data = bytes.toNSData()
            
            // Create audio player from data
            val player = AVAudioPlayer(data = data, error = null) ?: run {
                println("Could not create audio player for: $fileName")
                return@launch
            }
            
            // Set volume and play
            player.volume = volume
            player.prepareToPlay()
            player.play()
            
            // Cache for reuse
            audioPlayers[fileName] = player
        } catch (e: Exception) {
            println("Could not play sound: $fileName - ${e.message}")
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData {
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}

actual fun releaseAudioSystem() {
    audioPlayers.values.forEach { player ->
        try {
            player.stop()
        } catch (e: Exception) {
            // Ignore
        }
    }
    audioPlayers.clear()
}
