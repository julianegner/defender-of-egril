package de.egril.defender.audio

import de.egril.defender.config.LogConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Global sound manager instance
 * Provides access to sound playback throughout the application
 */
object GlobalSoundManager {
    private const val SOUND_EVENT_BUFFER_CAPACITY = 16

    private var soundManager: SoundManager? = null
    // Buffer short event bursts (e.g., clustered combat sounds in one frame) without blocking callers.
    private val _soundEvents = MutableSharedFlow<SoundEvent>(extraBufferCapacity = SOUND_EVENT_BUFFER_CAPACITY)
    val soundEvents: SharedFlow<SoundEvent> = _soundEvents.asSharedFlow()
    
    /**
     * Initialize the global sound manager
     * Should be called once at app startup
     */
    fun initialize() {
        if (soundManager == null) {
            soundManager = createSoundManager()
            soundManager?.initialize()
        }
    }
    
    /**
     * Play a sound event
     */
    fun playSound(event: SoundEvent, volume: Float = 1.0f) {
        soundManager?.playSound(event, volume)
        if (!_soundEvents.tryEmit(event) && (LogConfig.ENABLE_ALL_LOGGING || LogConfig.ENABLE_UI_LOGGING)) {
            println("GlobalSoundManager: dropped sound event $event (caption buffer full)")
        }
    }
    
    /**
     * Get the sound manager instance
     */
    fun getInstance(): SoundManager? = soundManager
    
    /**
     * Release sound resources
     */
    fun release() {
        soundManager?.release()
        soundManager = null
    }
}
