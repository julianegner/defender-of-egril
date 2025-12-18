package de.egril.defender.audio

import android.content.Context
import android.media.MediaPlayer
import de.egril.defender.ui.settings.AppSettings
import kotlinx.coroutines.*

/**
 * Android implementation of background music manager
 * Uses MediaPlayer for music playback
 */
class AndroidBackgroundMusicManager(private val context: Context) : BackgroundMusicManager {
    private var enabled = true
    private var volume = 1.0f
    private var currentMusic: BackgroundMusic? = null
    private var mediaPlayer: MediaPlayer? = null
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
                // Map music enum to resource ID (placeholder - need actual resources)
                val resourceId = when (music) {
                    BackgroundMusic.WORLD_MAP -> {
                        // Will need to add to res/raw or use compose resources
                        getResourceId("atmosphere_mystic_fantasy_orchestral_music_335263")
                    }
                    BackgroundMusic.GAMEPLAY_NORMAL -> {
                        getResourceId("fantasy_ambience_david_fesliyan")
                    }
                    BackgroundMusic.GAMEPLAY_LOW_HEALTH -> {
                        getResourceId("the_dark_castle_david_fesliyan")
                    }
                }
                
                if (resourceId == 0) {
                    println("Background music resource not found for: ${music.name}")
                    return
                }
                
                mediaPlayer = MediaPlayer.create(context, resourceId)
                mediaPlayer?.let { player ->
                    player.isLooping = loop
                    
                    // Get track-specific relative volume
                    val trackVolume = BackgroundMusicSettings.getRelativeVolume(music)
                    
                    // Get base multiplier for this music type
                    val baseMultiplier = BackgroundMusicSettings.getBaseMultiplier(music)
                    
                    // Get the category-specific volume setting
                    val categoryVolume = when (music) {
                        BackgroundMusic.WORLD_MAP -> AppSettings.worldMapMusicVolume.value
                        BackgroundMusic.GAMEPLAY_NORMAL, BackgroundMusic.GAMEPLAY_LOW_HEALTH -> AppSettings.gameplayMusicVolume.value
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
                }
            } catch (e: Exception) {
                println("Could not play background music: ${music.name} - ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun getResourceId(resourceName: String): Int {
        return try {
            context.resources.getIdentifier(resourceName, "raw", context.packageName)
        } catch (e: Exception) {
            0
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
