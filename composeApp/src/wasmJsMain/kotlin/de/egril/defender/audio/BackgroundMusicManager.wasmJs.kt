@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings
import kotlinx.coroutines.*

/**
 * External JS functions for Audio API with background music support
 */
@JsFun("(src) => new Audio(src)")
external fun createMusicAudio(src: String): JsAny

@JsFun("(audio, loop) => audio.loop = loop")
external fun setMusicAudioLoop(audio: JsAny, loop: Boolean)

@JsFun("(audio, volume) => audio.volume = volume")
external fun setMusicAudioVolume(audio: JsAny, volume: Double)

@JsFun("(audio) => audio.play()")
external fun playMusicAudio(audio: JsAny)

@JsFun("(audio) => audio.pause()")
external fun pauseMusicAudio(audio: JsAny)

@JsFun("(audio, time) => audio.currentTime = time")
external fun setMusicAudioCurrentTime(audio: JsAny, time: Double)

/**
 * WASM/JS implementation of background music manager
 * Uses HTML5 Audio API for music playback with external JS functions
 */
class WasmBackgroundMusicManager : BackgroundMusicManager {
    private var enabled = true
    private var volume = 1.0f
    private var currentMusic: BackgroundMusic? = null
    private var audioElement: JsAny? = null
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
                
                val audio = createMusicAudio("files/sounds/background/$fileName")
                audioElement = audio
                
                // Set loop mode
                setMusicAudioLoop(audio, loop)
                
                // Get track-specific relative volume
                val trackVolume = BackgroundMusicSettings.getRelativeVolume(music)
                
                // Get base multiplier for this music type
                val baseMultiplier = BackgroundMusicSettings.getBaseMultiplier(music)
                
                // Get the category-specific volume setting
                val categoryVolume = when (music) {
                    BackgroundMusic.WORLD_MAP -> AppSettings.worldMapMusicVolume.value
                    BackgroundMusic.GAMEPLAY_NORMAL, BackgroundMusic.GAMEPLAY_LOW_HEALTH -> AppSettings.gameplayMusicVolume.value
                }
                
                // Set volume (master * category * track * baseMultiplier) - convert to Double for JS
                val effectiveVolume = (AppSettings.soundVolume.value.toDouble() * categoryVolume.toDouble() * trackVolume.toDouble() * baseMultiplier.toDouble()).coerceIn(0.0, 1.0)
                setMusicAudioVolume(audio, effectiveVolume)
                
                // Play audio
                playMusicAudio(audio)
                playing = true
            } catch (e: Exception) {
                println("Could not play background music: ${music.name} - ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    override fun stopMusic() {
        audioElement?.let { audio ->
            try {
                pauseMusicAudio(audio)
                setMusicAudioCurrentTime(audio, 0.0)
            } catch (e: Exception) {
                // Ignore
            }
            audioElement = null
        }
        playing = false
        currentMusic = null
    }
    
    override fun pauseMusic() {
        audioElement?.let { audio ->
            try {
                pauseMusicAudio(audio)
                playing = false
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    override fun resumeMusic() {
        audioElement?.let { audio ->
            if (enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                try {
                    playMusicAudio(audio)
                    playing = true
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }
    
    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        AppSettings.saveMusicVolume(this.volume)
        
        // Update volume of currently playing music - convert to Double for JS
        audioElement?.let { audio ->
            try {
                val music = currentMusic
                val trackVolume = if (music != null) BackgroundMusicSettings.getRelativeVolume(music) else 1.0f
                val effectiveVolume = (AppSettings.soundVolume.value.toDouble() * this.volume.toDouble() * trackVolume.toDouble() * 0.3).coerceIn(0.0, 1.0)
                setMusicAudioVolume(audio, effectiveVolume)
            } catch (e: Exception) {
                // Ignore
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
    }
}

/**
 * WASM/JS platform implementation to create background music manager
 */
actual fun createBackgroundMusicManager(): BackgroundMusicManager {
    return WasmBackgroundMusicManager()
}
