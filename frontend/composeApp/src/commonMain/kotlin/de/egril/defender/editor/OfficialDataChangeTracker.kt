package de.egril.defender.editor

import de.egril.defender.config.LogConfig

/**
 * Tracks changes to official game data (maps and levels).
 * Used in conjunction with OfficialEditMode to warn users when official data has been modified.
 */
object OfficialDataChangeTracker {
    
    /**
     * Set of official map IDs that have been modified during this session
     */
    private val modifiedOfficialMaps = mutableSetOf<String>()
    
    /**
     * Set of official level IDs that have been modified during this session
     */
    private val modifiedOfficialLevels = mutableSetOf<String>()
    
    /**
     * Track that an official map has been modified
     */
    fun trackMapModified(mapId: String) {
        if (OfficialContent.isOfficialMap(mapId)) {
            modifiedOfficialMaps.add(mapId)
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Official map modified: $mapId")
            }
        }
    }
    
    /**
     * Track that an official level has been modified
     */
    fun trackLevelModified(levelId: String) {
        if (OfficialContent.isOfficialLevel(levelId)) {
            modifiedOfficialLevels.add(levelId)
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Official level modified: $levelId")
            }
        }
    }
    
    /**
     * Check if any official data has been modified
     */
    fun hasModifiedOfficialData(): Boolean {
        return modifiedOfficialMaps.isNotEmpty() || modifiedOfficialLevels.isNotEmpty()
    }
    
    /**
     * Get list of modified official map IDs
     */
    fun getModifiedOfficialMaps(): List<String> {
        return modifiedOfficialMaps.toList()
    }
    
    /**
     * Get list of modified official level IDs
     */
    fun getModifiedOfficialLevels(): List<String> {
        return modifiedOfficialLevels.toList()
    }
    
    /**
     * Clear all tracked changes (for testing or reset)
     */
    fun clearTracking() {
        modifiedOfficialMaps.clear()
        modifiedOfficialLevels.clear()
    }
}
