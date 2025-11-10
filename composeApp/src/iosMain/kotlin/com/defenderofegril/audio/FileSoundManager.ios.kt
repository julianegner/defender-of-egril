package com.defenderofegril.audio

import kotlinx.cinterop.ExperimentalForeignApi
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
    try {
        // Get or create audio player for this file
        val player = audioPlayers.getOrPut(fileName) {
            val bundle = NSBundle.mainBundle
            val resourceName = fileName.substringBeforeLast('.')
            val resourceExt = fileName.substringAfterLast('.', "")
            
            val path = bundle.pathForResource(
                "files/sounds/$resourceName",
                ofType = resourceExt
            ) ?: run {
                println("Could not find sound file: $fileName")
                return
            }
            
            val url = NSURL.fileURLWithPath(path)
            val player = AVAudioPlayer(contentsOfURL = url, error = null) ?: run {
                println("Could not create audio player for: $fileName")
                return
            }
            
            player.prepareToPlay()
            player
        }
        
        // Set volume and play
        player.volume = volume.toFloat()
        player.currentTime = 0.0 // Reset to beginning
        player.play()
    } catch (e: Exception) {
        println("Could not play sound: $fileName - ${e.message}")
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
