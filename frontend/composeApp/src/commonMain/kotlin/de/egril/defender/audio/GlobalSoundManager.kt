package de.egril.defender.audio

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Global sound manager instance
 * Provides access to sound playback throughout the application
 */
object GlobalSoundManager {
    private var soundManager: SoundManager? = null
    private val _soundEvents = MutableSharedFlow<SoundEvent>(extraBufferCapacity = 64)
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
        _soundEvents.tryEmit(event)
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
