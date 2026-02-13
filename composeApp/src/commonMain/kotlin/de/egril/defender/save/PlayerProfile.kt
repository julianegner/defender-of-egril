package de.egril.defender.save

import de.egril.defender.model.Achievement
import de.egril.defender.model.PlayerStats

/**
 * Represents a player profile
 * Each player has their own world map progress and save files
 */
data class PlayerProfile(
    val id: String,  // Unique identifier (sanitized player name)
    val name: String,  // Display name entered by player
    val createdAt: Long,  // Timestamp when profile was created
    val lastPlayedAt: Long,  // Timestamp when profile was last used
    val achievements: List<Achievement> = emptyList(),  // List of earned achievements
    val stats: PlayerStats = PlayerStats()  // Player stats and XP progression
)

/**
 * Container for all player profiles
 */
data class PlayerProfiles(
    val profiles: List<PlayerProfile>,
    val lastUsedPlayerId: String?  // ID of the last player who played
)
