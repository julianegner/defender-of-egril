package de.egril.defender.audio

/**
 * Global background music manager instance
 * Provides access to background music playback throughout the application
 */
object GlobalBackgroundMusicManager {
    private var musicManager: BackgroundMusicManager? = null
    
    /**
     * Initialize the global background music manager
     * Should be called once at app startup
     */
    fun initialize() {
        if (musicManager == null) {
            musicManager = createBackgroundMusicManager()
            musicManager?.initialize()
        }
    }
    
    /**
     * Play background music
     */
    fun playMusic(music: BackgroundMusic, loop: Boolean = true, volume: Float = 1.0f) {
        musicManager?.playMusic(music, loop, volume)
    }
    
    /**
     * Stop currently playing music
     */
    fun stopMusic() {
        musicManager?.stopMusic()
    }
    
    /**
     * Pause currently playing music
     */
    fun pauseMusic() {
        musicManager?.pauseMusic()
    }
    
    /**
     * Resume paused music
     */
    fun resumeMusic() {
        musicManager?.resumeMusic()
    }
    
    /**
     * Set background music volume
     */
    fun setVolume(volume: Float) {
        musicManager?.setVolume(volume)
    }
    
    /**
     * Enable or disable background music
     */
    fun setEnabled(enabled: Boolean) {
        musicManager?.setEnabled(enabled)
    }
    
    /**
     * Get the background music manager instance
     */
    fun getInstance(): BackgroundMusicManager? = musicManager
    
    /**
     * Get the currently playing or loaded music track
     */
    fun getCurrentMusic(): BackgroundMusic? {
        return musicManager?.getCurrentMusic()
    }
    
    /**
     * Release music resources
     */
    fun release() {
        musicManager?.release()
        musicManager = null
    }
}
