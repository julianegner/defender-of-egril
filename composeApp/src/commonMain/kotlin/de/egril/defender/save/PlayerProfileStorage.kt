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
