package de.egril.defender.save

import de.egril.defender.editor.getFileStorage
import de.egril.defender.utils.currentTimeMillis

/**
 * Manages player profiles and storage
 * Each player has their own directory with:
 * - level_progress.json (world map progress)
 * - savefiles/ directory (game saves)
 */
object PlayerProfileStorage {
    private val fileStorage = getFileStorage()
    
    private const val PLAYERS_DIR = "players"
    private const val PLAYERS_FILE = "players.json"
    
    init {
        fileStorage.createDirectory(PLAYERS_DIR)
    }
    
    /**
     * Get all player profiles
     */
    fun getAllProfiles(): PlayerProfiles {
        val json = fileStorage.readFile(PLAYERS_FILE)
        return if (json != null) {
            SaveJsonSerializer.deserializePlayerProfiles(json) ?: PlayerProfiles(emptyList(), null)
        } else {
            PlayerProfiles(emptyList(), null)
        }
    }
    
    /**
     * Save all player profiles
     */
    private fun saveProfiles(profiles: PlayerProfiles) {
        val json = SaveJsonSerializer.serializePlayerProfiles(profiles)
        fileStorage.writeFile(PLAYERS_FILE, json)
    }
    
    /**
     * Create a new player profile
     * @param name The player's display name
     * @return The created profile, or null if name is invalid or already exists
     */
    fun createProfile(name: String): PlayerProfile? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || trimmedName.length > 50) {
            return null
        }
        
        // Generate a safe ID from the name
        val id = sanitizeName(trimmedName)
        
        val profiles = getAllProfiles()
        
        // Check if profile with this ID already exists
        if (profiles.profiles.any { it.id == id }) {
            return null
        }
        
        val now = currentTimeMillis()
        val newProfile = PlayerProfile(
            id = id,
            name = trimmedName,
            createdAt = now,
            lastPlayedAt = now
        )
        
        // Create player directory
        fileStorage.createDirectory("$PLAYERS_DIR/$id")
        fileStorage.createDirectory("$PLAYERS_DIR/$id/savefiles")
        
        // Add to profiles list
        val updatedProfiles = profiles.copy(
            profiles = profiles.profiles + newProfile,
            lastUsedPlayerId = id
        )
        saveProfiles(updatedProfiles)
        
        return newProfile
    }
    
    /**
     * Get a profile by ID
     */
    fun getProfile(playerId: String): PlayerProfile? {
        return getAllProfiles().profiles.find { it.id == playerId }
    }
    
    /**
     * Update last played timestamp for a player
     */
    fun updateLastPlayed(playerId: String) {
        val profiles = getAllProfiles()
        val updatedProfiles = profiles.copy(
            profiles = profiles.profiles.map { profile ->
                if (profile.id == playerId) {
                    profile.copy(lastPlayedAt = currentTimeMillis())
                } else {
                    profile
                }
            },
            lastUsedPlayerId = playerId
        )
        saveProfiles(updatedProfiles)
    }
    
    /**
     * Update a player profile (for stats changes)
     */
    fun updateProfile(updatedProfile: PlayerProfile) {
        val profiles = getAllProfiles()
        val updatedProfiles = profiles.copy(
            profiles = profiles.profiles.map { profile ->
                if (profile.id == updatedProfile.id) {
                    updatedProfile
                } else {
                    profile
                }
            }
        )
        saveProfiles(updatedProfiles)
    }
    
    /**
     * Rename a player profile
     * @param playerId The ID of the player to rename
     * @param newName The new display name
     * @return The updated profile, or null if rename failed
     */
    fun renameProfile(playerId: String, newName: String): PlayerProfile? {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty() || trimmedName.length > 50) {
            return null
        }
        
        val profiles = getAllProfiles()
        val profile = profiles.profiles.find { it.id == playerId } ?: return null
        
        // Generate new ID from the new name
        val newId = sanitizeName(trimmedName)
        
        // Check if new ID conflicts with an existing profile (unless it's the same profile)
        if (newId != playerId && profiles.profiles.any { it.id == newId }) {
            return null
        }
        
        // If the ID changed, we need to rename the directory
        if (newId != playerId) {
            val oldDir = "$PLAYERS_DIR/$playerId"
            val newDir = "$PLAYERS_DIR/$newId"
            
            // Rename the directory
            try {
                fileStorage.renameDirectory(oldDir, newDir)
            } catch (e: Exception) {
                println("Error renaming player directory: ${e.message}")
                return null
            }
        }
        
        // Update the profile with the new name and ID
        val updatedProfile = profile.copy(
            id = newId,
            name = trimmedName
        )
        
        // Update profiles list
        val updatedProfiles = profiles.copy(
            profiles = profiles.profiles.map { p ->
                if (p.id == playerId) updatedProfile else p
            },
            lastUsedPlayerId = if (profiles.lastUsedPlayerId == playerId) newId else profiles.lastUsedPlayerId
        )
        saveProfiles(updatedProfiles)
        
        return updatedProfile
    }
    
    /**
     * Delete a player profile and all their data
     */
    fun deleteProfile(playerId: String): Boolean {
        val profiles = getAllProfiles()
        val profile = profiles.profiles.find { it.id == playerId } ?: return false
        
        // Delete player directory and all contents
        fileStorage.deleteDirectory("$PLAYERS_DIR/$playerId")
        
        // Remove from profiles list
        val updatedProfiles = profiles.copy(
            profiles = profiles.profiles.filter { it.id != playerId },
            lastUsedPlayerId = if (profiles.lastUsedPlayerId == playerId) {
                // If this was the last used player, set to first remaining player or null
                profiles.profiles.firstOrNull { it.id != playerId }?.id
            } else {
                profiles.lastUsedPlayerId
            }
        )
        saveProfiles(updatedProfiles)
        
        return true
    }
    
    /**
     * Get the directory path for a player's data
     */
    fun getPlayerDirectory(playerId: String): String {
        return "$PLAYERS_DIR/$playerId"
    }
    
    /**
     * Get the directory path for a player's save files
     */
    fun getPlayerSavefilesDirectory(playerId: String): String {
        return "$PLAYERS_DIR/$playerId/savefiles"
    }
    
    /**
     * Get the file path for a player's level progress
     */
    fun getPlayerLevelProgressFile(playerId: String): String {
        return "$PLAYERS_DIR/$playerId/level_progress.json"
    }
    
    /**
     * Sanitize a name to create a safe file system ID
     * - Convert to lowercase
     * - Replace spaces and special characters with underscore
     * - Remove consecutive underscores
     * - Trim underscores from ends
     */
    private fun sanitizeName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(50)  // Limit length
    }
    
    /**
     * Add an achievement to a player profile
     * @param playerId The ID of the player
     * @param achievement The achievement to add
     * @return True if the achievement was added, false if already earned or player not found
     */
    fun addAchievement(playerId: String, achievement: de.egril.defender.model.Achievement): Boolean {
        val profiles = getAllProfiles()
        val profile = profiles.profiles.find { it.id == playerId } ?: return false
        
        // Check if achievement is already earned
        if (profile.achievements.any { it.id == achievement.id }) {
            return false
        }
        
        // Add achievement to profile
        val updatedProfile = profile.copy(
            achievements = profile.achievements + achievement
        )
        
        // Update profiles list
        val updatedProfiles = profiles.copy(
            profiles = profiles.profiles.map { p ->
                if (p.id == playerId) updatedProfile else p
            }
        )
        saveProfiles(updatedProfiles)
        
        return true
    }
    
    /**
     * Check if a player has earned a specific achievement
     */
    fun hasAchievement(playerId: String, achievementId: de.egril.defender.model.AchievementId): Boolean {
        val profile = getProfile(playerId) ?: return false
        return profile.achievements.any { it.id == achievementId }
    }
    
    /**
     * Migrate existing saves to a default player profile
     * This should be called once on first launch with the new system
     */
    fun migrateExistingSaves(): PlayerProfile? {
        // Check if there are any existing saves or progress
        val hasExistingSaves = fileStorage.fileExists("savefiles/level_progress.json") ||
                              fileStorage.listFiles("savefiles").isNotEmpty()
        
        if (!hasExistingSaves) {
            return null
        }
        
        // Create a default profile
        val defaultProfile = createProfile("Player 1") ?: return null
        
        // Move existing level progress
        try {
            val existingProgress = fileStorage.readFile("savefiles/level_progress.json")
            if (existingProgress != null) {
                fileStorage.writeFile(getPlayerLevelProgressFile(defaultProfile.id), existingProgress)
            }
        } catch (e: Exception) {
            println("Error migrating level progress: ${e.message}")
        }
        
        // Move existing save files
        try {
            val savefiles = fileStorage.listFiles("savefiles")
            for (filename in savefiles) {
                if (filename == "level_progress.json") continue
                val content = fileStorage.readFile("savefiles/$filename")
                if (content != null) {
                    fileStorage.writeFile("${getPlayerSavefilesDirectory(defaultProfile.id)}/$filename", content)
                }
            }
        } catch (e: Exception) {
            println("Error migrating save files: ${e.message}")
        }
        
        return defaultProfile
    }
}
