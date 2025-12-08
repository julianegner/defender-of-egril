package de.egril.defender.audio

/**
 * Configuration for relative volume levels of background music tracks
 * These values are multiplied by the user's music volume setting
 */
object BackgroundMusicSettings {
    /**
     * Relative volume for world map music (1.0 = full volume)
     */
    const val WORLD_MAP_VOLUME = 1.0f
    
    /**
     * Relative volume for normal gameplay music (0.6 = 60% of world map volume)
     * This makes gameplay music about 40% quieter than world map music
     */
    const val GAMEPLAY_NORMAL_VOLUME = 0.6f
    
    /**
     * Relative volume for low health gameplay music (0.6 = 60% of world map volume)
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
        println("BackgroundMusicSettings.getRelativeVolume(${music.name}) = $volume")
        return volume
    }
}
