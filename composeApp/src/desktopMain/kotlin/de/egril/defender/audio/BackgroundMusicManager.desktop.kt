package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.SampleBuffer
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import javax.sound.sampled.*

/**
 * Desktop implementation of background music manager
 * Uses JLayer for MP3 decoding and javax.sound.sampled for playback with volume control
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
        
        // Get base multiplier for this music type
        val baseMultiplier = BackgroundMusicSettings.getBaseMultiplier(music)
        
        // Get the category-specific volume setting
        val categoryVolume = when (music) {
            BackgroundMusic.WORLD_MAP -> AppSettings.worldMapMusicVolume.value
            BackgroundMusic.GAMEPLAY_NORMAL, BackgroundMusic.GAMEPLAY_LOW_HEALTH -> AppSettings.gameplayMusicVolume.value
        }
        
        // Calculate effective volume (master * category * track * baseMultiplier)
        val effectiveVolume = (AppSettings.soundVolume.value * categoryVolume * trackVolume * baseMultiplier).coerceIn(0f, 1f)
        
        println("Starting background music: ${music.name}, effectiveVolume=$effectiveVolume (master=${AppSettings.soundVolume.value}, category=$categoryVolume, track=$trackVolume, base=$baseMultiplier)")
        
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
        
        // Update volume of currently playing music directly
        musicPlayer?.setVolume(this.volume)
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
 * Desktop platform implementation to create background music manager
 */
actual fun createBackgroundMusicManager(): BackgroundMusicManager {
    return DesktopBackgroundMusicManager()
}

/**
 * Music player interface
 */
internal interface MusicPlayer {
    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun setVolume(volume: Float)
}

/**
 * MP3 music player using JLayer for decoding and Clip for playback with proper volume control
 */
private class Mp3MusicPlayer(
    private val mp3Data: ByteArray,
    private var volume: Float,
    private val loop: Boolean,
    private val manager: DesktopBackgroundMusicManager
) : MusicPlayer {
    
    private var clip: Clip? = null
    private var playbackThread: Thread? = null
    @Volatile private var shouldStop = false
    @Volatile private var isPaused = false
    
    override fun start() {
        shouldStop = false
        isPaused = false
        
        playbackThread = Thread {
            try {
                // Decode MP3 to PCM
                val pcmData = decodeMp3ToPcm(mp3Data)
                
                if (shouldStop) return@Thread
                
                // Get audio format from decoded data
                val format = pcmData.format
                
                // Create and open clip
                val audioClip = AudioSystem.getClip()
                clip = audioClip
                
                audioClip.open(format, pcmData.data, 0, pcmData.data.size)
                
                // Ensure we start from the beginning
                audioClip.setFramePosition(0)
                
                // Flush any existing data in the line
                audioClip.flush()
                
                // Set volume before starting playback
                updateVolume(audioClip, volume)
                
                // Small delay to ensure audio system is ready
                Thread.sleep(50)
                
                // Set looping
                if (loop) {
                    audioClip.loop(Clip.LOOP_CONTINUOUSLY)
                } else {
                    audioClip.start()
                }
                
                // Keep thread alive while playing
                while (!shouldStop && manager.enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                    if (audioClip.isRunning) {
                        Thread.sleep(100)
                    } else if (!loop) {
                        break
                    }
                }
                
            } catch (e: Exception) {
                if (!shouldStop) {
                    println("Error in MP3 playback: ${e.message}")
                    e.printStackTrace()
                }
            } finally {
                clip?.stop()
                clip?.close()
            }
        }.apply {
            isDaemon = true
            name = "MP3-Music-Playback"
            start()
        }
    }
    
    private data class PcmData(val data: ByteArray, val format: AudioFormat)
    
    private fun decodeMp3ToPcm(mp3Bytes: ByteArray): PcmData {
        val bitstream = Bitstream(ByteArrayInputStream(mp3Bytes))
        val decoder = Decoder()
        val pcmBuffer = mutableListOf<Short>()
        var audioFormat: AudioFormat? = null
        
        try {
            var header: Header? = bitstream.readFrame()
            
            while (header != null && !shouldStop) {
                val sampleBuffer = decoder.decodeFrame(header, bitstream) as SampleBuffer
                
                // Create audio format from first frame
                if (audioFormat == null) {
                    audioFormat = AudioFormat(
                        sampleBuffer.sampleFrequency.toFloat(),
                        16, // 16-bit
                        sampleBuffer.channelCount,
                        true, // signed
                        false // little-endian
                    )
                }
                
                // Add samples to buffer
                val buffer = sampleBuffer.buffer
                val length = sampleBuffer.bufferLength
                for (i in 0 until length) {
                    pcmBuffer.add(buffer[i])
                }
                
                bitstream.closeFrame()
                header = bitstream.readFrame()
            }
        } finally {
            bitstream.close()
        }
        
        if (audioFormat == null) {
            throw IllegalStateException("Could not decode MP3 - no frames found")
        }
        
        // Convert shorts to bytes
        val byteArray = ByteArray(pcmBuffer.size * 2)
        for (i in pcmBuffer.indices) {
            val sample = pcmBuffer[i]
            byteArray[i * 2] = (sample.toInt() and 0xFF).toByte()
            byteArray[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        
        return PcmData(byteArray, audioFormat)
    }
    
    private fun updateVolume(audioClip: Clip, vol: Float) {
        try {
            // Calculate effective volume including track-specific volume
            val music = manager.getCurrentMusic()
            val trackVolume = if (music != null) BackgroundMusicSettings.getRelativeVolume(music) else 1.0f
            // Always read from AppSettings to get current value
            val currentMusicVolume = AppSettings.musicVolume.value
            val effectiveVolume = (AppSettings.soundVolume.value * currentMusicVolume * trackVolume * 0.3f).coerceIn(0f, 1f)
            
            if (audioClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = audioClip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                
                // Convert linear volume to decibels
                // Minimum volume should mute, not just reduce
                val gain = if (effectiveVolume <= 0.001f) {
                    gainControl.minimum
                } else {
                    val db = (20.0 * kotlin.math.ln(effectiveVolume.toDouble()) / kotlin.math.ln(10.0)).toFloat()
                    db.coerceIn(gainControl.minimum, gainControl.maximum)
                }
                
                gainControl.value = gain
                println("Set music gain to $gain dB (effectiveVolume=$effectiveVolume, currentMusicVolume=$currentMusicVolume, track=$trackVolume, min=${gainControl.minimum}, max=${gainControl.maximum})")
            } else {
                println("MASTER_GAIN control not supported for music")
            }
        } catch (e: Exception) {
            println("Failed to set music volume: ${e.message}")
        }
    }
    
    override fun stop() {
        shouldStop = true
        clip?.stop()
        clip?.close()
        playbackThread?.interrupt()
        try {
            playbackThread?.join(500)
        } catch (e: InterruptedException) {
            // Ignore
        }
    }
    
    override fun pause() {
        clip?.stop()
        isPaused = true
    }
    
    override fun resume() {
        if (isPaused && manager.enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
            clip?.start()
            isPaused = false
        }
    }
    
    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        clip?.let { updateVolume(it, this.volume) }
    }
}
