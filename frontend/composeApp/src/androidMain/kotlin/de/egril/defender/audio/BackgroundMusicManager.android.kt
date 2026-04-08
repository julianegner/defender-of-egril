package de.egril.defender.audio

import android.content.Context
import android.media.MediaPlayer
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

/**
 * Android implementation of background music manager
 * Uses MediaPlayer for music playback with files loaded from compose multiplatform resources
 */
class AndroidBackgroundMusicManager(private val context: Context) : BackgroundMusicManager {
    private var enabled = true
    private var volume = 1.0f
    private var currentMusic: BackgroundMusic? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playing = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Cache for temporary music files
    private val musicFileCache = mutableMapOf<BackgroundMusic, File>()
    
    override fun initialize() {
        enabled = AppSettings.isMusicEnabled.value
        volume = AppSettings.musicVolume.value
    }
    
    override fun playMusic(music: BackgroundMusic, loop: Boolean, volume: Float) {
        // Check if music should be playing
        val categoryEnabled = when (music) {
            BackgroundMusic.WORLD_MAP -> AppSettings.isWorldMapMusicEnabled.value
            BackgroundMusic.GAMEPLAY_NORMAL, BackgroundMusic.GAMEPLAY_LOW_HEALTH -> AppSettings.isGameplayMusicEnabled.value
            BackgroundMusic.FINAL_CREDITS -> AppSettings.isGameplayMusicEnabled.value
        }
        
        if (!enabled || !AppSettings.isSoundEnabled.value || !AppSettings.isMusicEnabled.value || !categoryEnabled) {
            stopMusic()
            return
        }
        
        // Stop current music if different from requested, or if not playing
        if (currentMusic != music || !playing) {
            stopMusic()
            currentMusic = music
            
            // Load and play music asynchronously
            scope.launch {
                // Map music enum to file name
                val fileName = when (music) {
                    BackgroundMusic.WORLD_MAP -> "background/atmosphere-mystic-fantasy-orchestral-music-335263.mp3"
                    BackgroundMusic.GAMEPLAY_NORMAL -> "background/2021-02-23_-_Fantasy_Ambience_-_David_Fesliyan.mp3"
                    BackgroundMusic.GAMEPLAY_LOW_HEALTH -> "background/2017-06-16_-_The_Dark_Castle_-_David_Fesliyan.mp3"
                    BackgroundMusic.FINAL_CREDITS -> "background/Happy_Music-2018-09-18_-_Beautiful_Memories_-_David_Fesliyan.mp3"
                }
                
                try {
                    // Get or create cached file
                    val musicFile = musicFileCache.getOrPut(music) {
                        // Load from compose resources using Res.readBytes
                        val resourcePath = "files/sounds/$fileName"
                        val bytes = Res.readBytes(resourcePath)
                        
                        // Create cache file for MediaPlayer
                        val cacheFileName = fileName.replace("/", "_")
                        val cacheFile = File(context.cacheDir, "music_$cacheFileName")
                        FileOutputStream(cacheFile).use { it.write(bytes) }
                        
                        cacheFile
                    }
                    
                    // Create MediaPlayer from file
                    withContext(Dispatchers.Main) {
                        try {
                            val player = MediaPlayer()
                            mediaPlayer = player
                            
                            player.setDataSource(musicFile.absolutePath)
                            player.prepare()
                            
                            player.isLooping = loop
                            
                            // Get track-specific relative volume
                            val trackVolume = BackgroundMusicSettings.getRelativeVolume(music)
                            
                            // Get base multiplier for this music type
                            val baseMultiplier = BackgroundMusicSettings.getBaseMultiplier(music)
                            
                            // Get the category-specific volume setting
                            val categoryVolume = when (music) {
                                BackgroundMusic.WORLD_MAP -> AppSettings.worldMapMusicVolume.value
                                BackgroundMusic.GAMEPLAY_NORMAL, BackgroundMusic.GAMEPLAY_LOW_HEALTH -> AppSettings.gameplayMusicVolume.value
                                BackgroundMusic.FINAL_CREDITS -> AppSettings.gameplayMusicVolume.value
                            }
                            
                            // Set volume (master * category * track * baseMultiplier)
                            val effectiveVolume = (AppSettings.soundVolume.value * categoryVolume * trackVolume * baseMultiplier).coerceIn(0f, 1f)
                            player.setVolume(effectiveVolume, effectiveVolume)
                            
                            player.start()
                            playing = true
                            
                            player.setOnCompletionListener {
                                if (!loop) {
                                    playing = false
                                }
                            }
                        } catch (e: Exception) {
                            println("Could not create MediaPlayer: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    println("Could not play background music: ${music.name}")
                    println("Expected file in compose resources: files/sounds/$fileName")
                    println("Error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    override fun stopMusic() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
            mediaPlayer = null
        }
        playing = false
        // Don't clear currentMusic to allow re-enabling
        // currentMusic = null
    }
    
    override fun pauseMusic() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                playing = false
            }
        }
    }
    
    override fun resumeMusic() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying && enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                player.start()
                playing = true
            }
        }
    }
    
    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        AppSettings.saveMusicVolume(this.volume)
        
        // Update volume of currently playing music
        mediaPlayer?.let { player ->
            val music = currentMusic
            val trackVolume = if (music != null) BackgroundMusicSettings.getRelativeVolume(music) else 1.0f
            val effectiveVolume = (AppSettings.soundVolume.value * this.volume * trackVolume * 0.3f).coerceIn(0f, 1f)
            player.setVolume(effectiveVolume, effectiveVolume)
        }
    }
    
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        AppSettings.saveMusicEnabled(enabled)
        
        if (!enabled || !AppSettings.isSoundEnabled.value) {
            pauseMusic()
        } else if (currentMusic != null) {
            // Try to resume existing music, or restart if needed
            if (mediaPlayer != null) {
                resumeMusic()
            } else {
                // Restart playback if mediaPlayer was released
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
        scope.cancel()
        
        // Clean up cached music files
        musicFileCache.values.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        musicFileCache.clear()
    }
}

// Platform context is set by MainActivity
private var androidContext: Context? = null

fun setAndroidContext(context: Context) {
    androidContext = context
}

/**
 * Android platform implementation to create background music manager
 */
actual fun createBackgroundMusicManager(): BackgroundMusicManager {
    return AndroidBackgroundMusicManager(androidContext ?: throw IllegalStateException("Android context not initialized"))
}
