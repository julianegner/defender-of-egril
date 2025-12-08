package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings
import kotlinx.browser.document
import org.w3c.dom.Audio
import org.w3c.dom.events.Event

/**
 * WASM/JS implementation of background music manager
 * Uses HTML5 Audio API for music playback
 */
class WasmBackgroundMusicManager : BackgroundMusicManager {
    private var enabled = true
    private var volume = 1.0f
    private var currentMusic: BackgroundMusic? = null
    private var audioElement: Audio? = null
    private var playing = false
    
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
            
            try {
                // Map music enum to file name
                val fileName = when (music) {
                    BackgroundMusic.WORLD_MAP -> "atmosphere-mystic-fantasy-orchestral-music-335263.mp3"
                    BackgroundMusic.GAMEPLAY_NORMAL -> "2021-02-23_-_Fantasy_Ambience_-_David_Fesliyan.mp3"
                    BackgroundMusic.GAMEPLAY_LOW_HEALTH -> "2017-06-16_-_The_Dark_Castle_-_David_Fesliyan.mp3"
                }
                
                val audio = Audio("files/sounds/background/$fileName")
                audioElement = audio
                
                // Set loop mode
                audio.loop = loop
                
                // Set volume (master * music * track-specific)
                val effectiveVolume = (AppSettings.soundVolume.value * this.volume * volume).coerceIn(0.0, 1.0)
                audio.volume = effectiveVolume
                
                // Set event listeners
                audio.onplay = { _: Event ->
                    playing = true
                }
                
                audio.onended = { _: Event ->
                    if (!loop) {
                        playing = false
                    }
                }
                
                audio.onerror = { _: Event ->
                    println("Error loading background music: $fileName")
                    playing = false
                }
                
                // Play audio
                audio.play()
            } catch (e: Exception) {
                println("Could not play background music: ${music.name} - ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    override fun stopMusic() {
        audioElement?.let { audio ->
            audio.pause()
            audio.currentTime = 0.0
            audioElement = null
        }
        playing = false
        currentMusic = null
    }
    
    override fun pauseMusic() {
        audioElement?.let { audio ->
            audio.pause()
            playing = false
        }
    }
    
    override fun resumeMusic() {
        audioElement?.let { audio ->
            if (enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                audio.play()
                playing = true
            }
        }
    }
    
    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        AppSettings.saveMusicVolume(this.volume)
        
        // Update volume of currently playing music
        audioElement?.let { audio ->
            val effectiveVolume = (AppSettings.soundVolume.value * this.volume).coerceIn(0.0, 1.0)
            audio.volume = effectiveVolume
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
    }
}

/**
 * WASM/JS platform implementation to create background music manager
 */
actual fun createBackgroundMusicManager(): BackgroundMusicManager {
    return WasmBackgroundMusicManager()
}
