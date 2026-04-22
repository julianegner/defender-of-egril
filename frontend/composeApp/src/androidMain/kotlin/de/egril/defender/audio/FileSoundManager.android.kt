package de.egril.defender.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Android implementation of file-based sound manager
 */

private var soundPool: SoundPool? = null
private val soundIds = mutableMapOf<String, Int>()
private var appContext: Context? = null
private var isAppInBackground = false

actual fun initializeAudioSystem() {
    // SoundPool will be initialized when context is available
}

/**
 * Notify the audio system that the app has moved to the background or foreground.
 * When in background, sound effects are suppressed.
 */
fun setSoundEffectsAppInBackground(inBackground: Boolean) {
    isAppInBackground = inBackground
}

/**
 * Initialize Android audio with application context
 * Must be called from Android-specific code before playing sounds
 */
fun initializeAndroidAudio(context: Context) {
    // Release old SoundPool (if Activity is recreated without process death) and clear
    // stale sound IDs so they are reloaded against the new pool.
    soundPool?.release()
    soundIds.clear()

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
    if (isAppInBackground) return
    val pool = soundPool ?: return
    val context = appContext ?: return
    
    GlobalScope.launch(Dispatchers.Main) {
        try {
            // Get or load sound ID
            val soundId = soundIds.getOrPut(fileName) {
                try {
                    // Load from compose resources using Res.readBytes
                    val resourcePath = "files/sounds/$fileName"
                    val bytes = Res.readBytes(resourcePath)
                    
                    // Write to temp file for SoundPool
                    val tempFile = File(context.cacheDir, fileName)
                    FileOutputStream(tempFile).use { it.write(bytes) }
                    
                    // Load into SoundPool
                    val id = pool.load(tempFile.absolutePath, 1)
                    
                    // Clean up temp file after loading
                    tempFile.delete()
                    
                    id
                } catch (e: Exception) {
                    println("Could not load sound: $fileName - ${e.message}")
                    e.printStackTrace()
                    -1
                }
            }
            
            if (soundId > 0) {
                pool.play(soundId, volume, volume, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            println("Could not play sound: $fileName - ${e.message}")
            e.printStackTrace()
        }
    }
}

actual fun releaseAudioSystem() {
    soundPool?.release()
    soundPool = null
    soundIds.clear()
    appContext = null
}
