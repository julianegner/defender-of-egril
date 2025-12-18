@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
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
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Cache for MP3 blob URLs
    private val musicBlobCache = mutableMapOf<BackgroundMusic, String>()
    
    override fun initialize() {
        enabled = AppSettings.isMusicEnabled.value
        volume = AppSettings.musicVolume.value
    }
    
    override fun playMusic(music: BackgroundMusic, loop: Boolean, volume: Float) {
        // Check if music should be playing
        val categoryEnabled = when (music) {
            BackgroundMusic.WORLD_MAP -> AppSettings.isWorldMapMusicEnabled.value
            BackgroundMusic.GAMEPLAY_NORMAL, BackgroundMusic.GAMEPLAY_LOW_HEALTH -> AppSettings.isGameplayMusicEnabled.value
        }
        
        if (!enabled || !AppSettings.isSoundEnabled.value || !AppSettings.isMusicEnabled.value || !categoryEnabled) {
            stopMusic()
            return
        }
        
        // Stop current music if different from requested, or if not playing
        if (currentMusic != music || !playing) {
            stopMusic()
            currentMusic = music
            
            // Load music asynchronously
            scope.launch {
                try {
                    // Get or create blob URL for this music
                    val blobUrl = musicBlobCache.getOrPut(music) {
                        // Map music enum to file name
                        val fileName = when (music) {
                            BackgroundMusic.WORLD_MAP -> "background/atmosphere-mystic-fantasy-orchestral-music-335263.mp3"
                            BackgroundMusic.GAMEPLAY_NORMAL -> "background/2021-02-23_-_Fantasy_Ambience_-_David_Fesliyan.mp3"
                            BackgroundMusic.GAMEPLAY_LOW_HEALTH -> "background/2017-06-16_-_The_Dark_Castle_-_David_Fesliyan.mp3"
                        }
                        
                        // Load bytes from compose resources using Res.readBytes
                        val resourcePath = "files/sounds/$fileName"
                        val bytes = Res.readBytes(resourcePath)
                        
                        // Create Uint8Array and copy bytes
                        val uint8Array = createUint8Array(bytes.size)
                        bytes.forEachIndexed { index, byte ->
                            setUint8ArrayValue(uint8Array, index, byte)
                        }
                        
                        // Create blob from Uint8Array with audio/mpeg MIME type for MP3
                        val blob = createBlob(uint8Array, "audio/mpeg")
                        
                        // Create object URL
                        createObjectURL(blob)
                    }
                    
                    if (blobUrl.isEmpty()) {
                        println("Failed to create blob URL for background music: ${music.name}")
                        return@launch
                    }
                    
                    // Create audio element with blob URL
                    val audio = createMusicAudio(blobUrl)
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
        // Don't clear currentMusic to allow re-enabling
        // currentMusic = null
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
        
        if (!enabled || !AppSettings.isSoundEnabled.value) {
            pauseMusic()
        } else if (currentMusic != null) {
            // Try to resume existing music, or restart if needed
            if (audioElement != null) {
                resumeMusic()
            } else {
                // Restart playback if audioElement was released
                playMusic(currentMusic!!, loop = true)
            }
        }
    }
    
    override fun isEnabled(): Boolean = enabled
    
    override fun getVolume(): Float = volume
    
    override fun isPlaying(): Boolean = playing
    
    override fun getCurrentMusic(): BackgroundMusic? = currentMusic
    
    override fun release() {
        stopMusic()
        
        // Revoke blob URLs
        musicBlobCache.values.forEach { url ->
            try {
                revokeObjectURL(url)
            } catch (e: Exception) {
                // Ignore
            }
        }
        musicBlobCache.clear()
        
        // Cancel coroutine scope
        scope.cancel()
    }
}

/**
 * WASM/JS platform implementation to create background music manager
 */
actual fun createBackgroundMusicManager(): BackgroundMusicManager {
    return WasmBackgroundMusicManager()
}
