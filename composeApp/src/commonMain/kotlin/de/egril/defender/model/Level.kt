package de.egril.defender.model

import de.egril.defender.editor.EditorLevel
import de.egril.defender.ui.common.LevelInfoEnemiesLevelData

/**
 * Represents a waypoint that enemies must pass through
 */
data class Waypoint(
    val position: Position,
    val nextTarget: Position  // Position to head to after reaching this waypoint (can be another waypoint or the final target)
)

data class Level(
    val id: Int,
    val name: String,
    val subtitle: String = "",  // Optional subtitle for the level
    val titleKey: String? = null,  // Optional translation key for the title
    val subtitleKey: String? = null,  // Optional translation key for the subtitle
    val gridWidth: Int = 30,
    val gridHeight: Int = 8,
    val startPositions: List<Position> = listOf(
        Position(0, 1),
        Position(0, 4),
        Position(0, 7)
    ),
    val targetPositions: List<Position> = listOf(Position(gridWidth - 1, gridHeight / 2)),
    val pathCells: Set<Position>,
    val buildIslands: Set<Position>,
    val buildAreas: Set<Position> = emptySet(),  // Explicit build areas from map
    val attackerWaves: List<AttackerWave>,
    val initialCoins: Int = 100,
    val healthPoints: Int = 10,
    val directSpawnPlan: List<PlannedEnemySpawn>? = null,  // Direct spawn plan from editor
    val availableTowers: Set<DefenderType> = DefenderType.entries.toSet(),  // Towers available in this level
    val waypoints: List<Waypoint> = emptyList(),  // Waypoints for complex pathing
    val editorLevelId: String? = null,  // ID of the editor level this was created from
    val mapId: String? = null,  // ID of the map this level uses
    val riverTiles: Map<Position, RiverTile> = emptyMap()  // River tiles with flow direction and speed (not walkable in gameplay, but treated as walkable during map validation for levels with ORK, EVIL_WIZARD, or EWHAD enemies)
) {
    fun isOnPath(position: Position): Boolean {
        return pathCells.contains(position)
    }
    
    fun isBuildIsland(position: Position): Boolean {
        return buildIslands.contains(position)
    }
    
    fun isBuildArea(position: Position): Boolean {
        // Cannot build on path itself, spawn points, or targets
        if (isSpawnPoint(position) || targetPositions.contains(position)) return false
        if (isOnPath(position)) return false
        
        // Can build on islands
        if (isBuildIsland(position)) return true
        
        // Can build on explicitly defined build areas
        return buildAreas.contains(position)
    }
    
    fun isTargetPosition(position: Position): Boolean {
        return targetPositions.contains(position)
    }
    
    fun isSpawnPoint(position: Position): Boolean {
        return startPositions.contains(position)
    }
    
    fun isWaypoint(position: Position): Boolean {
        return waypoints.any { it.position == position }
    }
    
    fun getWaypointAt(position: Position): Waypoint? {
        return waypoints.firstOrNull { it.position == position }
    }
    
    fun isRiverTile(position: Position): Boolean {
        return riverTiles.containsKey(position)
    }
    
    fun getRiverTile(position: Position): RiverTile? {
        return riverTiles[position]
    }

    fun toLevelInfoEnemiesLevelData(): LevelInfoEnemiesLevelData {
        val enemyCounts = getEnemyTypeCounts()
        return LevelInfoEnemiesLevelData(
            id = "" + this.id,
            name = this.name,
            subtitle = this.subtitle,
            titleKey = this.titleKey,  // Include translation key
            subtitleKey = this.subtitleKey,  // Include translation key
            initialCoins = this.initialCoins,
            healthPoints = this.healthPoints,
            enemyTypeCounts = enemyCounts
        )
    }
    
    /**
     * Convert level to LevelInfoEnemiesLevelData with difficulty modifiers applied
     */
    fun toLevelInfoEnemiesLevelData(difficulty: de.egril.defender.ui.settings.DifficultyLevel): LevelInfoEnemiesLevelData {
        val enemyCounts = getEnemyTypeCounts()
        
        // Apply difficulty modifiers to enemy counts (Nightmare: 3x, except Ewhad stays 1)
        val modifiedEnemyCounts = if (difficulty == de.egril.defender.ui.settings.DifficultyLevel.NIGHTMARE) {
            enemyCounts.mapValues { (type, count) ->
                if (type == AttackerType.EWHAD) count else count * 3
            }
        } else {
            enemyCounts
        }
        
        // Apply difficulty modifiers to coins and HP
        val modifiedCoins = DifficultyModifiers.applyCoinsModifier(this.initialCoins, difficulty)
        val modifiedHP = DifficultyModifiers.applyHealthPointsModifier(this.healthPoints, difficulty)
        
        return LevelInfoEnemiesLevelData(
            id = "" + this.id,
            name = this.name,
            subtitle = this.subtitle,
            titleKey = this.titleKey,  // Include translation key
            subtitleKey = this.subtitleKey,  // Include translation key
            initialCoins = modifiedCoins,
            healthPoints = modifiedHP,
            enemyTypeCounts = modifiedEnemyCounts
        )
    }
}

data class AttackerWave(
    val attackers: List<AttackerType>,
    val spawnDelay: Int = 2 // turns between spawns
)

/**
 * Represents a planned enemy spawn with the turn it will spawn
 */
data class PlannedEnemySpawn(
    val attackerType: AttackerType,
    val spawnTurn: Int,
    val level: Int = 1,
    val spawnPoint: Position? = null  // Fixed spawn point for this enemy (null for backward compatibility)
) {
    val healthPoints: Int get() = attackerType.health * level
}

/**
 * Generate a spawn plan for all waves in a level
 * This calculates when each enemy will spawn based on wave delays
 */
fun generateSpawnPlan(waves: List<AttackerWave>): List<PlannedEnemySpawn> {
    val plan = mutableListOf<PlannedEnemySpawn>()
    var currentTurn = 1  // First enemies spawn at turn 1
    
    for (wave in waves) {
        for ((index, attackerType) in wave.attackers.withIndex()) {
            // Spawn 6 enemies at a time (2x spawn points), every turn based on spawnDelay
            val groupIndex = index / 6
            val spawnTurn = currentTurn + (groupIndex * wave.spawnDelay)
            plan.add(PlannedEnemySpawn(attackerType, spawnTurn))
        }
        // Move to next wave - add delay after last enemy of current wave
        if (wave.attackers.isNotEmpty()) {
            val lastGroupIndex = (wave.attackers.size - 1) / 6
            val lastEnemyTurn = currentTurn + (lastGroupIndex * wave.spawnDelay)
            currentTurn = lastEnemyTurn + wave.spawnDelay + 2  // Gap between waves
        }
    }
    
    return plan
}

enum class LevelStatus {
    LOCKED,
    UNLOCKED,
    WON
}

data class WorldLevel(
    val level: Level,
    var status: LevelStatus = LevelStatus.LOCKED
)

/**
 * Get the count of each enemy type in a level.
 * Returns a map of AttackerType to count.
 */
fun Level.getEnemyTypeCounts(): Map<AttackerType, Int> {
    val enemyCounts = mutableMapOf<AttackerType, Int>()
    
    for (wave in attackerWaves) {
        for (attackerType in wave.attackers) {
            enemyCounts[attackerType] = (enemyCounts[attackerType] ?: 0) + 1
        }
    }
    
    return enemyCounts
}
