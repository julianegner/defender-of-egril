package de.egril.defender.audio

import de.egril.defender.config.LogConfig

/**
 * Configuration for relative volume levels of background music tracks
 * These values are multiplied by the user's music volume setting
 */
object BackgroundMusicSettings {
    /**
     * Base volume multiplier for world map music
     * World map music is louder (0.6x) to be more prominent
     */
    const val WORLD_MAP_BASE_MULTIPLIER = 0.6f
    
    /**
     * Base volume multiplier for gameplay music
     * Gameplay music is quieter (0.3x) to not interfere with game sounds
     */
    const val GAMEPLAY_BASE_MULTIPLIER = 0.3f
    
    /**
     * Relative volume for world map music (1.0 = full volume within world map category)
     */
    const val WORLD_MAP_VOLUME = 1.0f
    
    /**
     * Relative volume for normal gameplay music (0.6 = 60% of world map volume within gameplay category)
     * This makes gameplay music about 40% quieter than world map music
     */
    const val GAMEPLAY_NORMAL_VOLUME = 0.6f
    
    /**
     * Relative volume for low health gameplay music (0.6 = 60% of world map volume within gameplay category)
     */
    const val GAMEPLAY_LOW_HEALTH_VOLUME = 0.6f
    
    /**
     * Get the relative volume for a specific background music track
     */
    fun getRelativeVolume(music: BackgroundMusic): Float {
        val volume = when (music) {
            BackgroundMusic.WORLD_MAP -> WORLD_MAP_VOLUME
            BackgroundMusic.GAMEPLAY_NORMAL -> GAMEPLAY_NORMAL_VOLUME
            BackgroundMusic.GAMEPLAY_LOW_HEALTH -> GAMEPLAY_LOW_HEALTH_VOLUME
        }
        if (LogConfig.ENABLE_UI_LOGGING) {
        println("BackgroundMusicSettings.getRelativeVolume(${music.name}) = $volume")
        }
        return volume
    }
    
    /**
     * Get the base multiplier for a specific background music track
     */
    fun getBaseMultiplier(music: BackgroundMusic): Float {
        return when (music) {
            BackgroundMusic.WORLD_MAP -> WORLD_MAP_BASE_MULTIPLIER
            BackgroundMusic.GAMEPLAY_NORMAL, BackgroundMusic.GAMEPLAY_LOW_HEALTH -> GAMEPLAY_BASE_MULTIPLIER
        }
    }
}
