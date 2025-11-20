package de.egril.defender.save

import de.egril.defender.model.*

/**
 * World map save data
 * Stores the status of each level (locked, unlocked, or won)
 */
data class WorldMapSave(
    val levelStatuses: Map<Int, LevelStatus>  // levelId -> status
)

/**
 * Represents a saved game state
 */
data class SavedGame(
    val id: String,  // Unique identifier for this save
    val timestamp: Long,  // When the game was saved
    val levelId: Int,
    val levelName: String,
    val turnNumber: Int,
    val coins: Int,
    val healthPoints: Int,
    val phase: GamePhase,
    val defenders: List<SavedDefender>,
    val attackers: List<SavedAttacker>,
    val nextDefenderId: Int,
    val nextAttackerId: Int,
    val currentWaveIndex: Int,
    val spawnCounter: Int,
    val attackersToSpawn: List<AttackerType>,
    val fieldEffects: List<SavedFieldEffect>,
    val traps: List<SavedTrap>,
    val comment: String? = null,  // Optional player comment
    val mapId: String? = null  // Map identifier (for ensuring correct map is loaded)
)

data class SavedDefender(
    val id: Int,
    val type: DefenderType,
    val position: Position,
    val level: Int,
    val buildTimeRemaining: Int,
    val placedOnTurn: Int,
    val actionsRemaining: Int = 0,  // Default to 0 for backward compatibility with old saves
    val dragonName: String? = null  // Dragon's name (for dragon's lair only)
)

data class SavedAttacker(
    val id: Int,
    val type: AttackerType,
    val position: Position,
    val level: Int,
    val currentHealth: Int,
    val isDefeated: Boolean,
    val dragonName: String? = null  // Dragon's name (for dragons only)
)

data class SavedFieldEffect(
    val position: Position,
    val type: FieldEffectType,
    val damage: Int,
    val turnsRemaining: Int,
    val defenderId: Int,
    val attackerId: Int?
)

data class SavedTrap(
    val position: Position,
    val damage: Int,
    val mineId: Int
)

/**
 * Metadata about a saved game (for display in load game list)
 */
data class SaveGameMetadata(
    val id: String,
    val timestamp: Long,
    val levelId: Int,
    val levelName: String,
    val turnNumber: Int,
    val coins: Int,
    val towerCount: Int,
    val enemyCount: Int,
    val defenderCounts: Map<DefenderType, Int>,  // Count of each tower type
    val attackerCounts: Map<AttackerType, Int>,  // Count of each enemy type currently on map
    val remainingSpawnCounts: Map<AttackerType, Int>,  // Count of enemies still to spawn
    val comment: String? = null,  // Optional player comment
    val defenderPositions: List<SavedDefender> = emptyList(),  // Positions for minimap display
    val attackerPositions: List<SavedAttacker> = emptyList()  // Positions for minimap display
)
