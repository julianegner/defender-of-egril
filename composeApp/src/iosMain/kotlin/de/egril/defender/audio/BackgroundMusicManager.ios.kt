package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings
import platform.AVFAudio.*
import platform.Foundation.NSURL
import kotlinx.cinterop.*

/**
 * iOS implementation of background music manager
 * Uses AVAudioPlayer for music playback
 */
class IOSBackgroundMusicManager : BackgroundMusicManager {
    private var enabled = true
    private var volume = 1.0f
    private var currentMusic: BackgroundMusic? = null
    private var audioPlayer: AVAudioPlayer? = null
    private var playing = false
    
    override fun initialize() {
        enabled = AppSettings.isMusicEnabled.value
        volume = AppSettings.musicVolume.value
        
        // Configure audio session
        try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryPlayback, null)
            audioSession.setActive(true, null)
        } catch (e: Exception) {
            println("Could not configure iOS audio session: ${e.message}")
        }
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
            
            try {
                // Map music enum to file name
                val fileName = when (music) {
                    BackgroundMusic.WORLD_MAP -> "atmosphere-mystic-fantasy-orchestral-music-335263"
                    BackgroundMusic.GAMEPLAY_NORMAL -> "2021-02-23_-_Fantasy_Ambience_-_David_Fesliyan"
                    BackgroundMusic.GAMEPLAY_LOW_HEALTH -> "2017-06-16_-_The_Dark_Castle_-_David_Fesliyan"
                }
                
                // Get file URL from bundle (placeholder - needs actual implementation)
                val bundle = platform.Foundation.NSBundle.mainBundle
                val fileURL = bundle.URLForResource(fileName, "mp3", "sounds/background")
                
                if (fileURL == null) {
                    println("Background music file not found: $fileName")
                    return
                }
                
                val player = AVAudioPlayer(fileURL, null)
                audioPlayer = player
                
                // Set number of loops (-1 for infinite loop)
                player.numberOfLoops = if (loop) -1 else 0
                
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
                player.volume = effectiveVolume
                
                // Prepare and play
                player.prepareToPlay()
                player.play()
                playing = true
            } catch (e: Exception) {
                println("Could not play background music: ${music.name} - ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    override fun stopMusic() {
        audioPlayer?.let { player ->
            if (player.playing) {
                player.stop()
            }
            audioPlayer = null
        }
        playing = false
        // Don't clear currentMusic to allow re-enabling
        // currentMusic = null
    }
    
    override fun pauseMusic() {
        audioPlayer?.let { player ->
            if (player.playing) {
                player.pause()
                playing = false
            }
        }
    }
    
    override fun resumeMusic() {
        audioPlayer?.let { player ->
            if (!player.playing && enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                player.play()
                playing = true
            }
        }
    }
    
    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        AppSettings.saveMusicVolume(this.volume)
        
        // Update volume of currently playing music
        audioPlayer?.let { player ->
            val music = currentMusic
            val trackVolume = if (music != null) BackgroundMusicSettings.getRelativeVolume(music) else 1.0f
            val effectiveVolume = (AppSettings.soundVolume.value * this.volume * trackVolume * 0.3f).coerceIn(0f, 1f)
            player.volume = effectiveVolume
        }
    }
    
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        AppSettings.saveMusicEnabled(enabled)
        
        if (!enabled || !AppSettings.isSoundEnabled.value) {
            pauseMusic()
        } else if (currentMusic != null) {
            // Try to resume existing music, or restart if needed
            if (audioPlayer != null) {
                resumeMusic()
            } else {
                // Restart playback if audioPlayer was released
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

/**
 * iOS platform implementation to create background music manager
 */
actual fun createBackgroundMusicManager(): BackgroundMusicManager {
    return IOSBackgroundMusicManager()
}
