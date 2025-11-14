package de.egril.defender.audio

/**
 * Interface for platform-specific sound playback
 */
interface SoundManager {
    /**
     * Initialize the sound system
     */
    fun initialize()
    
    /**
     * Play a sound event
     * @param event The sound event to play
     * @param volume Volume level (0.0 to 1.0)
     */
    fun playSound(event: SoundEvent, volume: Float = 1.0f)
    
    /**
     * Set the master volume for all sounds
     * @param volume Volume level (0.0 to 1.0)
     */
    fun setVolume(volume: Float)
    
    /**
     * Enable or disable sound playback
     * @param enabled True to enable, false to disable
     */
    fun setEnabled(enabled: Boolean)
    
    /**
     * Check if sound is currently enabled
     */
    fun isEnabled(): Boolean
    
    /**
     * Get the current master volume
     */
    fun getVolume(): Float
    
    /**
     * Release all sound resources
     */
    fun release()
}

/**
 * Expect function to get platform-specific SoundManager implementation
 */
expect fun createSoundManager(): SoundManager
