package com.defenderofegril.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Android implementation of file-based sound manager
 */

private var soundPool: SoundPool? = null
private val soundIds = mutableMapOf<String, Int>()
private var appContext: Context? = null

actual fun initializeAudioSystem() {
    // SoundPool will be initialized when context is available
}

/**
 * Initialize Android audio with application context
 * Must be called from Android-specific code before playing sounds
 */
fun initializeAndroidAudio(context: Context) {
    appContext = context.applicationContext
    
    soundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
}

actual fun playSoundFile(fileName: String, volume: Float) {
    val pool = soundPool ?: return
    val context = appContext ?: return
    
    GlobalScope.launch(Dispatchers.Main) {
        try {
            // Get or load sound ID
            val soundId = soundIds.getOrPut(fileName) {
                val resName = fileName.substringBeforeLast('.')
                val resId = context.resources.getIdentifier(
                    resName,
                    "raw",
                    context.packageName
                )
                if (resId != 0) {
                    pool.load(context, resId, 1)
                } else {
                    // Try loading from assets
                    try {
                        val afd = context.assets.openFd("files/sounds/$fileName")
                        pool.load(afd, 1)
                    } catch (e: Exception) {
                        println("Could not load sound: $fileName - ${e.message}")
                        -1
                    }
                }
            }
            
            if (soundId > 0) {
                pool.play(soundId, volume, volume, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            println("Could not play sound: $fileName - ${e.message}")
        }
    }
}

actual fun releaseAudioSystem() {
    soundPool?.release()
    soundPool = null
    soundIds.clear()
    appContext = null
}
