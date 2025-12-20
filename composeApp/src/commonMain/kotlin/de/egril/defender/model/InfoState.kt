package de.egril.defender.model

/**
 * Types of single tutorial info popups that can be shown
 */
enum class InfoType {
    DRAGON_INFO,        // Dragon behavior explanation
    GREED_INFO,         // Dragon greed explanation (greed > 0)
    VERY_GREEDY_INFO,   // Dragon very greedy explanation (greed > 5)
    MINE_WARNING,       // Mine under threat from dragon
    ONE_HP_WARNING,     // Warning when player starts with only 1 HP
    MAGICAL_TRAP_INFO,  // Magical trap unlocked at wizard level 10
    EXTENDED_AREA_INFO, // Extended area attack unlocked at level 20 (wizard/alchemy)
    WIZARD_FIRST_USE,   // First time placing a wizard tower
    ALCHEMY_FIRST_USE,  // First time placing an alchemy tower
    BALLISTA_FIRST_USE, // First time placing a ballista tower
    MINE_FIRST_USE,     // First time placing a dwarven mine
    RIVER_INFO,         // River, bridge, and raft mechanics explanation
    MINE_ON_RIVER_WARNING, // Warning when trying to place mine on river
    GREEN_WITCH_INFO,   // Green witch healing ability explanation
    RED_WITCH_INFO,     // Red witch tower disabling ability explanation
    NONE                // No info to show
}

/**
 * State for managing single tutorial info popups
 * These are one-time informational dialogs that can appear during gameplay
 */
data class InfoState(
    val currentInfo: InfoType = InfoType.NONE,
    val seenInfos: Set<InfoType> = emptySet(),
    val mineWarningId: Int? = null  // For mine warnings, track which mine
) {
    /**
     * Check if we should show the info overlay
     */
    fun shouldShowOverlay(): Boolean {
        return currentInfo != InfoType.NONE
    }
    
    /**
     * Check if an info has been seen
     */
    fun hasSeen(type: InfoType): Boolean {
        return type in seenInfos
    }
    
    /**
     * Show a specific info type
     */
    fun showInfo(type: InfoType, mineId: Int? = null): InfoState {
        return copy(
            currentInfo = type,
            mineWarningId = if (type == InfoType.MINE_WARNING) mineId else null
        )
    }
    
    /**
     * Dismiss the current info and mark it as seen
     */
    fun dismissInfo(): InfoState {
        val updatedSeen = if (currentInfo != InfoType.NONE) {
            seenInfos + currentInfo
        } else {
            seenInfos
        }
        return copy(
            currentInfo = InfoType.NONE,
            seenInfos = updatedSeen,
            mineWarningId = null
        )
    }
    
    /**
     * Clear all seen infos (useful for testing or reset)
     */
    fun clearSeenInfos(): InfoState {
        return copy(seenInfos = emptySet())
    }
}
