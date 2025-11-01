package com.defenderofegril.save

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.editor.getFileStorage
import com.defenderofegril.model.*
import com.defenderofegril.utils.currentTimeMillis

/**
 * File-based storage for save games
 * Uses the same FileStorage infrastructure as the editor
 */
object SaveFileStorage {
    private val fileStorage = getFileStorage()
    
    private const val SAVEFILES_DIR = "savefiles"
    private const val WORLDMAP_FILE = "savefiles/worldmap.json"
    
    init {
        fileStorage.createDirectory(SAVEFILES_DIR)
    }
    
    /**
     * Save world map status
     */
    fun saveWorldMapStatus(worldLevels: List<WorldLevel>) {
        val statusMap = worldLevels.associate { it.level.id to it.status }
        val worldMapSave = WorldMapSave(statusMap)
        val json = SaveJsonSerializer.serializeWorldMapSave(worldMapSave)
        fileStorage.writeFile(WORLDMAP_FILE, json)
    }
    
    /**
     * Load world map status
     */
    fun loadWorldMapStatus(): Map<Int, LevelStatus>? {
        val json = fileStorage.readFile(WORLDMAP_FILE) ?: return null
        val worldMapSave = SaveJsonSerializer.deserializeWorldMapSave(json)
        return worldMapSave?.levelStatuses
    }
    
    /**
     * Save current game state
     */
    fun saveGameState(gameState: GameState): String {
        val saveId = "savegame_${currentTimeMillis()}"
        val savedGame = convertGameStateToSavedGame(gameState, saveId)
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
        
        for (filename in files) {
            if (!filename.endsWith(".json") || filename == "worldmap.json") continue
            
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
                    
                    // Count remaining spawns
                    val remainingSpawnCounts = savedGame.attackersToSpawn
                        .groupingBy { it }
                        .eachCount()
                    
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
                            remainingSpawnCounts = remainingSpawnCounts
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
     * Convert GameState to SavedGame
     */
    private fun convertGameStateToSavedGame(gameState: GameState, saveId: String): SavedGame {
        val defenders = gameState.defenders.map { defender ->
            SavedDefender(
                id = defender.id,
                type = defender.type,
                position = defender.position,
                level = defender.level.value,
                buildTimeRemaining = defender.buildTimeRemaining.value,
                placedOnTurn = defender.placedOnTurn
            )
        }
        
        val attackers = gameState.attackers.map { attacker ->
            SavedAttacker(
                id = attacker.id,
                type = attacker.type,
                position = attacker.position.value,
                level = attacker.level,
                currentHealth = attacker.currentHealth.value,
                isDefeated = attacker.isDefeated.value
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
                mineId = trap.mineId
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
            traps = traps
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
        
        // Restore defenders
        gameState.defenders.clear()
        for (savedDefender in savedGame.defenders) {
            val defender = Defender(
                id = savedDefender.id,
                type = savedDefender.type,
                position = savedDefender.position,
                placedOnTurn = savedDefender.placedOnTurn
            )
            defender.level.value = savedDefender.level
            defender.buildTimeRemaining.value = savedDefender.buildTimeRemaining
            gameState.defenders.add(defender)
        }
        
        // Restore attackers
        gameState.attackers.clear()
        for (savedAttacker in savedGame.attackers) {
            val attacker = Attacker(
                id = savedAttacker.id,
                type = savedAttacker.type,
                position = mutableStateOf(savedAttacker.position),
                level = savedAttacker.level
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
                mineId = trap.mineId
            )
        })
        
        return gameState
    }
}
