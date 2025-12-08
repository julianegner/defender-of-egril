package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.advanced.AdvancedPlayer
import javazoom.jl.player.advanced.PlaybackEvent
import javazoom.jl.player.advanced.PlaybackListener
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import javax.sound.sampled.*

/**
 * Desktop implementation of background music manager
 * Uses JLayer for MP3 playback and javax.sound.sampled for WAV
 */
class DesktopBackgroundMusicManager : BackgroundMusicManager {
    private var enabled = true
    private var volume = 1.0f
    private var currentMusic: BackgroundMusic? = null
    private var musicPlayer: MusicPlayer? = null
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
                    
                    // Calculate effective volume (master * music * track-specific)
                    val effectiveVolume = (AppSettings.soundVolume.value * this@DesktopBackgroundMusicManager.volume * volume).coerceIn(0f, 1f)
                    
                    // Create and start music player
                    val player = Mp3MusicPlayer(bytes, effectiveVolume, loop)
                    musicPlayer = player
                    player.start()
                    playing = true
                } catch (e: Exception) {
                    println("Could not play background music: ${music.name} - ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    override fun stopMusic() {
        musicPlayer?.stop()
        musicPlayer = null
        playing = false
        currentMusic = null
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
        
        // Update volume of currently playing music
        val effectiveVolume = (AppSettings.soundVolume.value * this.volume).coerceIn(0f, 1f)
        musicPlayer?.setVolume(effectiveVolume)
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
 * Interface for music player implementations
 */
private interface MusicPlayer {
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
    private val loop: Boolean
) : MusicPlayer {
    
    private var player: AdvancedPlayer? = null
    private var playbackThread: Thread? = null
    @Volatile private var shouldStop = false
    @Volatile private var isPaused = false
    private var sourceDataLine: SourceDataLine? = null
    
    override fun start() {
        shouldStop = false
        isPaused = false
        playbackThread = Thread {
            try {
                do {
                    if (shouldStop) break
                    
                    val inputStream = BufferedInputStream(ByteArrayInputStream(audioData))
                    player = AdvancedPlayer(inputStream)
                    
                    // Set up playback listener to handle volume control via SourceDataLine
                    player?.setPlayBackListener(object : PlaybackListener() {
                        override fun playbackStarted(evt: PlaybackEvent?) {
                            super.playbackStarted(evt)
                            // Get the SourceDataLine from the player's audio device
                            try {
                                val device = player?.javaClass?.getDeclaredField("audio")?.apply {
                                    isAccessible = true
                                }?.get(player)
                                
                                val line = device?.javaClass?.getDeclaredField("source")?.apply {
                                    isAccessible = true
                                }?.get(device) as? SourceDataLine
                                
                                sourceDataLine = line
                                updateVolume()
                            } catch (e: Exception) {
                                // If we can't access the line, volume control won't work but playback will
                                println("Could not access SourceDataLine for volume control: ${e.message}")
                            }
                        }
                    })
                    
                    player?.play()
                    
                    // Wait a bit before looping to avoid tight loop
                    if (loop && !shouldStop) {
                        Thread.sleep(100)
                    }
                } while (loop && !shouldStop)
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
        sourceDataLine?.close()
        sourceDataLine = null
    }
    
    override fun pause() {
        isPaused = true
        player?.close()
    }
    
    override fun resume() {
        if (isPaused) {
            isPaused = false
            // For simplicity, restart playback
            // A more sophisticated implementation would track position
            start()
        }
    }
    
    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        updateVolume()
    }
    
    private fun updateVolume() {
        sourceDataLine?.let { line ->
            try {
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val range = gainControl.maximum - gainControl.minimum
                    val gain = gainControl.minimum + range * volume
                    gainControl.value = gain.coerceIn(gainControl.minimum, gainControl.maximum)
                }
            } catch (e: Exception) {
                // Volume control failed, but playback continues
            }
        }
    }
}

/**
 * Desktop platform implementation to create background music manager
 */
actual fun createBackgroundMusicManager(): BackgroundMusicManager {
    return DesktopBackgroundMusicManager()
}
