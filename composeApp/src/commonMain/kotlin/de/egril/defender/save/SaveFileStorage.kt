package de.egril.defender.save

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.getFileStorage
import de.egril.defender.game.LevelData
import de.egril.defender.model.*
import de.egril.defender.utils.currentTimeMillis

/**
 * File-based storage for save games
 * Uses the same FileStorage infrastructure as the editor
 */
object SaveFileStorage {
    private val fileStorage = getFileStorage()
    
    private const val SAVEFILES_DIR = "savefiles"
    private const val LEVEL_PROGRESS_FILE = "savefiles/level_progress.json"
    
    // Cache levels to avoid reloading on every call to getAllSavedGames()
    private var cachedLevels: List<Level>? = null
    
    init {
        fileStorage.createDirectory(SAVEFILES_DIR)
    }
    
    /**
     * Save world map status
     */
    fun saveWorldMapStatus(worldLevels: List<WorldLevel>) {
        val statusMap = worldLevels.mapNotNull { worldLevel ->
            // Only save levels that have an editorLevelId
            if (worldLevel.level.editorLevelId == null) {
                println("WARNING: Skipping level ${worldLevel.level.id} (${worldLevel.level.name}) - no editorLevelId")
            }
            worldLevel.level.editorLevelId?.let { editorLevelId ->
                editorLevelId to worldLevel.status
            }
        }.toMap()
        val worldMapSave = WorldMapSave(statusMap)
        val json = SaveJsonSerializer.serializeWorldMapSave(worldMapSave)
        fileStorage.writeFile(LEVEL_PROGRESS_FILE, json)
    }
    
    /**
     * Load world map status
     */
    fun loadWorldMapStatus(): Map<String, LevelStatus>? {
        val json = fileStorage.readFile(LEVEL_PROGRESS_FILE) ?: return null
        val worldMapSave = SaveJsonSerializer.deserializeWorldMapSave(json)
        return worldMapSave?.levelStatuses
    }
    
    /**
     * Save current game state
     */
    fun saveGameState(gameState: GameState, comment: String? = null): String {
        val saveId = "savegame_${currentTimeMillis()}"
        val savedGame = convertGameStateToSavedGame(gameState, saveId, comment)
        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        fileStorage.writeFile("$SAVEFILES_DIR/$saveId.json", json)
        return saveId
    }
    
    /**
     * Load a saved game
     */
    fun loadGameState(saveId: String): SavedGame? {
        val json = fileStorage.readFile("$SAVEFILES_DIR/$saveId.json") ?: return null
        return SaveJsonSerializer.deserializeSavedGame(json)
    }
    
    /**
     * Get all saved games (metadata only)
     */
    fun getAllSavedGames(): List<SaveGameMetadata> {
        val files = fileStorage.listFiles(SAVEFILES_DIR)
        val savedGames = mutableListOf<SaveGameMetadata>()
        
        // Load levels to get spawn plans (cache for performance)
        val levels = cachedLevels ?: LevelData.createLevels().also { cachedLevels = it }
        
        for (filename in files) {
            if (!filename.endsWith(".json") || filename == "level_progress.json") continue
            
            val saveId = filename.removeSuffix(".json")
            val json = fileStorage.readFile("$SAVEFILES_DIR/$filename")
            if (json != null) {
                val savedGame = SaveJsonSerializer.deserializeSavedGame(json)
                if (savedGame != null) {
                    // Count defenders by type (only built towers)
                    val defenderCounts = savedGame.defenders
                        .filter { it.buildTimeRemaining == 0 }
                        .groupingBy { it.type }
                        .eachCount()
                    
                    // Count active attackers by type
                    val attackerCounts = savedGame.attackers
                        .filter { !it.isDefeated }
                        .groupingBy { it.type }
                        .eachCount()
                    
                    // Count remaining spawns from level descriptor
                    // Find the level to get the spawn plan
                    val level = levels.find { it.id == savedGame.levelId }
                    val remainingSpawnCounts = if (level != null) {
                        // Get the spawn plan (either from editor or generated from waves)
                        val spawnPlan = level.directSpawnPlan ?: generateSpawnPlan(level.attackerWaves)
                        
                        // Filter to get only future spawns (turn > current turn)
                        spawnPlan
                            .filter { it.spawnTurn > savedGame.turnNumber }
                            .map { it.attackerType }
                            .groupingBy { it }
                            .eachCount()
                    } else {
                        // Fallback to old behavior if level not found
                        savedGame.attackersToSpawn
                            .groupingBy { it }
                            .eachCount()
                    }
                    
                    savedGames.add(
                        SaveGameMetadata(
                            id = savedGame.id,
                            timestamp = savedGame.timestamp,
                            levelId = savedGame.levelId,
                            levelName = savedGame.levelName,
                            turnNumber = savedGame.turnNumber,
                            coins = savedGame.coins,
                            towerCount = savedGame.defenders.size,
                            enemyCount = savedGame.attackers.count { !it.isDefeated },
                            defenderCounts = defenderCounts,
                            attackerCounts = attackerCounts,
                            remainingSpawnCounts = remainingSpawnCounts,
                            comment = savedGame.comment,
                            defenderPositions = savedGame.defenders,
                            attackerPositions = savedGame.attackers,
                            mapId = savedGame.mapId  // Include map ID for minimap display
                        )
                    )
                }
            }
        }
        
        return savedGames.sortedByDescending { it.timestamp }
    }
    
    /**
     * Delete a saved game
     */
    fun deleteSavedGame(saveId: String) {
        fileStorage.deleteFile("$SAVEFILES_DIR/$saveId.json")
    }
    
    /**
     * Get the JSON content of a saved game for export
     */
    fun getSaveGameJson(saveId: String): String? {
        return fileStorage.readFile("$SAVEFILES_DIR/$saveId.json")
    }
    
    /**
     * Get all saved games as a map of filename to JSON content
     */
    fun getAllSaveGamesJson(): Map<String, String> {
        val files = fileStorage.listFiles(SAVEFILES_DIR)
        val result = mutableMapOf<String, String>()
        
        files.forEach { filename ->
            if (filename.endsWith(".json") && filename != "level_progress.json") {
                val content = fileStorage.readFile("$SAVEFILES_DIR/$filename")
                if (content != null) {
                    result[filename] = content
                }
            }
        }
        
        return result
    }
    
    /**
     * Import a save game from JSON content
     * @return true if import was successful
     */
    fun importSaveGame(filename: String, jsonContent: String, overwrite: Boolean = false): Boolean {
        return try {
            // Validate JSON by trying to deserialize
            val savedGame = SaveJsonSerializer.deserializeSavedGame(jsonContent)
            if (savedGame == null) {
                println("Invalid save game JSON: $filename")
                return false
            }
            
            // Check if file already exists
            val targetPath = "$SAVEFILES_DIR/$filename"
            if (fileStorage.fileExists(targetPath) && !overwrite) {
                println("Save game already exists: $filename")
                return false
            }
            
            // Write the file
            fileStorage.writeFile(targetPath, jsonContent)
            true
        } catch (e: Exception) {
            println("Error importing save game $filename: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Check if a save game with the given filename exists
     */
    fun saveGameExists(filename: String): Boolean {
        return fileStorage.fileExists("$SAVEFILES_DIR/$filename")
    }
    
    /**
     * Convert GameState to SavedGame
     */
    private fun convertGameStateToSavedGame(gameState: GameState, saveId: String, comment: String? = null): SavedGame {
        val defenders = gameState.defenders.map { defender ->
            SavedDefender(
                id = defender.id,
                type = defender.type,
                position = defender.position.value,
                level = defender.level.value,
                buildTimeRemaining = defender.buildTimeRemaining.value,
                placedOnTurn = defender.placedOnTurn,
                actionsRemaining = defender.actionsRemaining.value,
                dragonName = defender.dragonName,
                raftId = defender.raftId.value
            )
        }
        
        val attackers = gameState.attackers.map { attacker ->
            SavedAttacker(
                id = attacker.id,
                type = attacker.type,
                position = attacker.position.value,
                level = attacker.level.value,
                currentHealth = attacker.currentHealth.value,
                isDefeated = attacker.isDefeated.value,
                dragonName = attacker.dragonName
            )
        }
        
        val fieldEffects = gameState.fieldEffects.map { effect ->
            SavedFieldEffect(
                position = effect.position,
                type = effect.type,
                damage = effect.damage,
                turnsRemaining = effect.turnsRemaining,
                defenderId = effect.defenderId,
                attackerId = effect.attackerId
            )
        }
        
        val traps = gameState.traps.map { trap ->
            SavedTrap(
                position = trap.position,
                damage = trap.damage,
                defenderId = trap.defenderId,
                type = trap.type.name
            )
        }
        
        val rafts = gameState.rafts.map { raft ->
            SavedRaft(
                id = raft.id,
                defenderId = raft.defenderId,
                position = raft.currentPosition.value
            )
        }
        
        return SavedGame(
            id = saveId,
            timestamp = currentTimeMillis(),
            levelId = gameState.level.id,
            levelName = gameState.level.name,
            turnNumber = gameState.turnNumber.value,
            coins = gameState.coins.value,
            healthPoints = gameState.healthPoints.value,
            phase = gameState.phase.value,
            defenders = defenders,
            attackers = attackers,
            nextDefenderId = gameState.nextDefenderId.value,
            nextAttackerId = gameState.nextAttackerId.value,
            currentWaveIndex = gameState.currentWaveIndex.value,
            spawnCounter = gameState.spawnCounter.value,
            attackersToSpawn = gameState.attackersToSpawn.toList(),
            fieldEffects = fieldEffects,
            traps = traps,
            comment = comment,
            mapId = gameState.level.mapId,  // Save the map ID for verification on load
            rafts = rafts,
            nextRaftId = gameState.nextRaftId.value
        )
    }
    
    /**
     * Convert SavedGame back to GameState
     */
    fun convertSavedGameToGameState(savedGame: SavedGame, level: Level): GameState {
        val gameState = GameState(level = level)
        
        // Restore basic state
        gameState.phase.value = savedGame.phase
        gameState.coins.value = savedGame.coins
        gameState.healthPoints.value = savedGame.healthPoints
        gameState.nextDefenderId.value = savedGame.nextDefenderId
        gameState.nextAttackerId.value = savedGame.nextAttackerId
        gameState.currentWaveIndex.value = savedGame.currentWaveIndex
        gameState.spawnCounter.value = savedGame.spawnCounter
        gameState.turnNumber.value = savedGame.turnNumber
        gameState.nextRaftId.value = savedGame.nextRaftId
        
        // Restore rafts first
        gameState.rafts.clear()
        for (savedRaft in savedGame.rafts) {
            val raft = Raft(
                id = savedRaft.id,
                defenderId = savedRaft.defenderId,
                currentPosition = mutableStateOf(savedRaft.position)
            )
            gameState.rafts.add(raft)
        }
        
        // Restore defenders
        gameState.defenders.clear()
        for (savedDefender in savedGame.defenders) {
            val defender = Defender(
                id = savedDefender.id,
                type = savedDefender.type,
                position = mutableStateOf(savedDefender.position),
                placedOnTurn = savedDefender.placedOnTurn,
                dragonName = savedDefender.dragonName
            )
            defender.level.value = savedDefender.level
            defender.buildTimeRemaining.value = savedDefender.buildTimeRemaining
            defender.actionsRemaining.value = savedDefender.actionsRemaining
            defender.raftId.value = savedDefender.raftId  // Restore raft linkage
            gameState.defenders.add(defender)
        }
        
        // Restore attackers
        gameState.attackers.clear()
        for (savedAttacker in savedGame.attackers) {
            val attacker = Attacker(
                id = savedAttacker.id,
                type = savedAttacker.type,
                position = mutableStateOf(savedAttacker.position),
                level = mutableStateOf(savedAttacker.level),
                dragonName = savedAttacker.dragonName
            )
            attacker.currentHealth.value = savedAttacker.currentHealth
            attacker.isDefeated.value = savedAttacker.isDefeated
            gameState.attackers.add(attacker)
        }
        
        // Restore attackers to spawn
        gameState.attackersToSpawn.clear()
        gameState.attackersToSpawn.addAll(savedGame.attackersToSpawn)
        
        // Restore field effects
        gameState.fieldEffects.clear()
        gameState.fieldEffects.addAll(savedGame.fieldEffects.map { effect ->
            FieldEffect(
                position = effect.position,
                type = effect.type,
                damage = effect.damage,
                turnsRemaining = effect.turnsRemaining,
                defenderId = effect.defenderId,
                attackerId = effect.attackerId
            )
        })
        
        // Restore traps
        gameState.traps.clear()
        gameState.traps.addAll(savedGame.traps.map { trap ->
            Trap(
                position = trap.position,
                damage = trap.damage,
                defenderId = trap.defenderId,
                type = try { TrapType.valueOf(trap.type) } catch (e: Exception) { TrapType.DWARVEN }
            )
        })
        
        return gameState
    }
}
