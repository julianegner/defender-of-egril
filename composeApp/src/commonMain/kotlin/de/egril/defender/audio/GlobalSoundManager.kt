package de.egril.defender.audio

/**
 * Global sound manager instance
 * Provides access to sound playback throughout the application
 */
object GlobalSoundManager {
    private var soundManager: SoundManager? = null
    
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
