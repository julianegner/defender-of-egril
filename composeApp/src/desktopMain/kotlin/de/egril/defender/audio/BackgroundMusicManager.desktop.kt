package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import javax.sound.sampled.*

/**
 * Desktop implementation of background music manager
 * Uses javax.sound.sampled API for music playback
 */
class DesktopBackgroundMusicManager : BackgroundMusicManager {
    private var enabled = true
    private var volume = 1.0f
    private var currentMusic: BackgroundMusic? = null
    private var currentClip: Clip? = null
    private var playing = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun initialize() {
        enabled = AppSettings.isMusicEnabled.value
        volume = AppSettings.musicVolume.value
    }
    
    override fun playMusic(music: BackgroundMusic, loop: Boolean, volume: Float) {
        if (!enabled || !AppSettings.isSoundEnabled.value || !AppSettings.isMusicEnabled.value) return
        
        // Stop current music if different from requested
        if (currentMusic != music) {
            stopMusic()
            currentMusic = music
            
            scope.launch {
                try {
                    // Map music enum to file name
                    val fileName = when (music) {
                        BackgroundMusic.WORLD_MAP -> "background/atmosphere-mystic-fantasy-orchestral-music-335263.mp3"
                        BackgroundMusic.GAMEPLAY_NORMAL -> "background/2021-02-23_-_Fantasy_Ambience_-_David_Fesliyan.mp3"
                        BackgroundMusic.GAMEPLAY_LOW_HEALTH -> "background/2017-06-16_-_The_Dark_Castle_-_David_Fesliyan.mp3"
                    }
                    
                    // Load audio file from compose resources
                    val resourcePath = "files/sounds/$fileName"
                    val bytes = try {
                        Res.readBytes(resourcePath)
                    } catch (e: Exception) {
                        println("Could not load background music from compose resources: $resourcePath - ${e.message}")
                        return@launch
                    }
                    
                    // Create audio input stream from bytes
                    val audioInputStream = AudioSystem.getAudioInputStream(
                        BufferedInputStream(ByteArrayInputStream(bytes))
                    )
                    
                    val clip = AudioSystem.getClip()
                    clip.open(audioInputStream)
                    currentClip = clip
                    
                    // Set volume (master * music * track-specific)
                    val effectiveVolume = (AppSettings.soundVolume.value * this@DesktopBackgroundMusicManager.volume * volume).coerceIn(0f, 1f)
                    if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                        val range = gainControl.maximum - gainControl.minimum
                        val gain = gainControl.minimum + range * effectiveVolume
                        gainControl.value = gain.coerceIn(gainControl.minimum, gainControl.maximum)
                    }
                    
                    // Set loop mode
                    if (loop) {
                        clip.loop(Clip.LOOP_CONTINUOUSLY)
                    }
                    
                    // Play music
                    clip.start()
                    playing = true
                    
                    // Handle clip stop
                    clip.addLineListener { event ->
                        if (event.type == LineEvent.Type.STOP && !loop) {
                            playing = false
                        }
                    }
                } catch (e: Exception) {
                    println("Could not play background music: ${music.name} - ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    override fun stopMusic() {
        currentClip?.let { clip ->
            if (clip.isRunning) {
                clip.stop()
            }
            clip.close()
            currentClip = null
        }
        playing = false
        currentMusic = null
    }
    
    override fun pauseMusic() {
        currentClip?.let { clip ->
            if (clip.isRunning) {
                clip.stop()
                playing = false
            }
        }
    }
    
    override fun resumeMusic() {
        currentClip?.let { clip ->
            if (!clip.isRunning && enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                clip.start()
                playing = true
            }
        }
    }
    
    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        AppSettings.saveMusicVolume(this.volume)
        
        // Update volume of currently playing clip
        currentClip?.let { clip ->
            val effectiveVolume = (AppSettings.soundVolume.value * this.volume).coerceIn(0f, 1f)
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val range = gainControl.maximum - gainControl.minimum
                val gain = gainControl.minimum + range * effectiveVolume
                gainControl.value = gain.coerceIn(gainControl.minimum, gainControl.maximum)
            }
        }
    }
    
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        AppSettings.saveMusicEnabled(enabled)
        
        if (!enabled) {
            pauseMusic()
        } else {
            resumeMusic()
        }
    }
    
    override fun isEnabled(): Boolean = enabled
    
    override fun getVolume(): Float = volume
    
    override fun isPlaying(): Boolean = playing
    
    override fun getCurrentMusic(): BackgroundMusic? = currentMusic
    
    override fun release() {
        stopMusic()
        scope.cancel()
    }
}

/**
 * Desktop platform implementation to create background music manager
 */
actual fun createBackgroundMusicManager(): BackgroundMusicManager {
    return DesktopBackgroundMusicManager()
}
