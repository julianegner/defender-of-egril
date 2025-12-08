package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.advanced.AdvancedPlayer
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream

/**
 * Desktop implementation of background music manager
 * Uses JLayer for MP3 playback and javax.sound.sampled for WAV
 */
class DesktopBackgroundMusicManager : BackgroundMusicManager {
    internal var enabled = true
    private var volume = 1.0f
    private var currentMusic: BackgroundMusic? = null
    internal var musicPlayer: MusicPlayer? = null
    private var playing = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Store current music data for volume changes and re-enabling
    private var currentMusicBytes: ByteArray? = null
    private var currentLoop: Boolean = false
    
    override fun initialize() {
        enabled = AppSettings.isMusicEnabled.value
        volume = AppSettings.musicVolume.value
    }
    
    override fun playMusic(music: BackgroundMusic, loop: Boolean, volume: Float) {
        // Check if music should be playing
        if (!enabled || !AppSettings.isSoundEnabled.value || !AppSettings.isMusicEnabled.value) {
            stopMusic()
            return
        }
        
        // Stop current music if different from requested
        if (currentMusic != music) {
            stopMusic()
            currentMusic = music
            currentLoop = loop
            
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
                    
                    currentMusicBytes = bytes
                    startPlayback()
                } catch (e: Exception) {
                    println("Could not play background music: ${music.name} - ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun startPlayback() {
        val bytes = currentMusicBytes ?: return
        val music = currentMusic ?: return
        
        // Stop any existing playback
        musicPlayer?.stop()
        
        // Get track-specific relative volume
        val trackVolume = BackgroundMusicSettings.getRelativeVolume(music)
        
        // Calculate effective volume (master * music * track * 0.3 multiplier)
        val effectiveVolume = (AppSettings.soundVolume.value * this.volume * trackVolume * 0.3f).coerceIn(0f, 1f)
        
        println("Starting background music: ${music.name}, effectiveVolume=$effectiveVolume (master=${AppSettings.soundVolume.value}, music=${this.volume}, track=$trackVolume)")
        
        // Create and start music player
        val player = Mp3MusicPlayer(bytes, effectiveVolume, currentLoop, this)
        musicPlayer = player
        player.start()
        playing = true
    }
    
    override fun stopMusic() {
        musicPlayer?.stop()
        musicPlayer = null
        playing = false
        currentMusic = null
        currentMusicBytes = null
    }
    
    override fun pauseMusic() {
        musicPlayer?.pause()
        playing = false
    }
    
    override fun resumeMusic() {
        if (enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
            musicPlayer?.resume()
            playing = true
        }
    }
    
    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        AppSettings.saveMusicVolume(this.volume)
        
        println("Background music setVolume called: volume=$volume")
        
        // Restart playback with new volume if music is currently playing
        if (playing && currentMusicBytes != null) {
            startPlayback()
        }
    }
    
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        AppSettings.saveMusicEnabled(enabled)
        
        if (!enabled || !AppSettings.isSoundEnabled.value) {
            stopMusic()
        } else if (currentMusicBytes != null && currentMusic != null) {
            // Restart playback when re-enabled
            startPlayback()
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
 * Interface for music player implementations
 */
internal interface MusicPlayer {
    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun setVolume(volume: Float)
}

/**
 * MP3 music player using JLayer
 */
private class Mp3MusicPlayer(
    private val audioData: ByteArray,
    private var volume: Float,
    private val loop: Boolean,
    private val manager: DesktopBackgroundMusicManager
) : MusicPlayer {
    
    private var player: AdvancedPlayer? = null
    private var playbackThread: Thread? = null
    @Volatile private var shouldStop = false
    @Volatile private var isPaused = false
    @Volatile private var currentVolume: Float = volume
    
    override fun start() {
        shouldStop = false
        isPaused = false
        playbackThread = Thread {
            try {
                do {
                    // Check if we should stop or if music is disabled
                    if (shouldStop || !manager.enabled || !AppSettings.isSoundEnabled.value || !AppSettings.isMusicEnabled.value) {
                        break
                    }
                    
                    val inputStream = BufferedInputStream(ByteArrayInputStream(audioData))
                    player = AdvancedPlayer(inputStream)
                    
                    // Play the audio
                    player?.play()
                    
                    // Check again before looping
                    if (loop && !shouldStop && manager.enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                        Thread.sleep(100)
                    }
                } while (loop && !shouldStop && manager.enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value)
            } catch (e: JavaLayerException) {
                if (!shouldStop) {
                    println("Error playing MP3: ${e.message}")
                }
            } catch (e: InterruptedException) {
                // Thread interrupted, stop playback
            } catch (e: Exception) {
                println("Unexpected error in MP3 playback: ${e.message}")
                e.printStackTrace()
            }
        }.apply {
            isDaemon = true
            name = "MP3-Playback-Thread"
            start()
        }
    }
    
    override fun stop() {
        shouldStop = true
        player?.close()
        playbackThread?.interrupt()
        try {
            playbackThread?.join(500) // Wait up to 500ms for thread to finish
        } catch (e: InterruptedException) {
            // Ignore
        }
    }
    
    override fun pause() {
        isPaused = true
        shouldStop = true
        player?.close()
    }
    
    override fun resume() {
        if (isPaused && manager.enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
            isPaused = false
            // For simplicity, restart playback
            // A more sophisticated implementation would track position
            start()
        }
    }
    
    override fun setVolume(volume: Float) {
        // Volume changes are handled by restarting playback in the manager
        // This method is kept for interface compatibility
        this.currentVolume = volume.coerceIn(0f, 1f)
    }
}

/**
 * Desktop platform implementation to create background music manager
 */
actual fun createBackgroundMusicManager(): BackgroundMusicManager {
    return DesktopBackgroundMusicManager()
}
