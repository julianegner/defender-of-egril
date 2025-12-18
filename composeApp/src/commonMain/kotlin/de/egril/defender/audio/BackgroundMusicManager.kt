package de.egril.defender.audio

/**
 * Enum representing different background music tracks
 */
enum class BackgroundMusic {
    WORLD_MAP,           // Mystic fantasy orchestral for world map
    GAMEPLAY_NORMAL,     // Fantasy ambience for normal gameplay
    GAMEPLAY_LOW_HEALTH  // Dark castle music when HP < 5
}

/**
 * Interface for background music playback
 * Manages looping music tracks with volume control
 */
interface BackgroundMusicManager {
    /**
     * Initialize the background music system
     */
    fun initialize()
    
    /**
     * Play a background music track
     * @param music The background music track to play
     * @param loop Whether to loop the music (default: true)
     * @param volume Volume level (0.0 to 1.0)
     */
    fun playMusic(music: BackgroundMusic, loop: Boolean = true, volume: Float = 1.0f)
    
    /**
     * Stop the currently playing music
     */
    fun stopMusic()
    
    /**
     * Pause the currently playing music
     */
    fun pauseMusic()
    
    /**
     * Resume the currently paused music
     */
    fun resumeMusic()
    
    /**
     * Set the volume for background music
     * @param volume Volume level (0.0 to 1.0)
     */
    fun setVolume(volume: Float)
    
    /**
     * Enable or disable background music playback
     * @param enabled True to enable, false to disable
     */
    fun setEnabled(enabled: Boolean)
    
    /**
     * Check if background music is currently enabled
     */
    fun isEnabled(): Boolean
    
    /**
     * Get the current background music volume
     */
    fun getVolume(): Float
    
    /**
     * Check if music is currently playing
     */
    fun isPlaying(): Boolean
    
    /**
     * Get the currently playing music track
     */
    fun getCurrentMusic(): BackgroundMusic?
    
    /**
     * Release all music resources
     */
    fun release()
}

/**
 * Expect function to get platform-specific BackgroundMusicManager implementation
 */
expect fun createBackgroundMusicManager(): BackgroundMusicManager
