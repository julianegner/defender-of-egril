package de.egril.defender.model

import de.egril.defender.ui.common.LevelInfoEnemiesLevelData

/**
 * Represents a waypoint that enemies must pass through
 */
data class Waypoint(
    val position: Position,
    val nextTarget: Position, // Position to head to after reaching this waypoint (can be another waypoint or the final target)
)

/**
 * How a target tile behaves when an enemy reaches it.
 * STANDARD: the enemy damages the player's health points (existing behavior).
 * SINGLE_HIT: the target is "taken" (removed from active targets); no HP damage.
 *             If all SINGLE_HIT targets are taken, the level is lost.
 */
enum class TargetType {
    STANDARD,
    SINGLE_HIT,
}

/**
 * Type of a spawn point: determines which enemy units may use it.
 * LAND: standard spawn point on dry ground — only land-capable enemies may spawn here.
 * WATER: spawn point in a river or other water area — only water-capable enemies may spawn here.
 */
enum class SpawnPointType {
    LAND,
    WATER,
}

/**
 * Optional metadata attached to a target tile.
 */
data class TargetInfo(
    val name: String = "", // Display name shown on the tile (e.g. "Marketplace")
    val type: TargetType = TargetType.STANDARD,
)

data class Level(
    val id: Int,
    val name: String,
    val subtitle: String = "", // Optional subtitle for the level
    val titleKey: String? = null, // Optional translation key for the title
    val subtitleKey: String? = null, // Optional translation key for the subtitle
    val gridWidth: Int = 30,
    val gridHeight: Int = 8,
    val startPositions: List<Position> =
        listOf(
            Position(0, 1),
            Position(0, 4),
            Position(0, 7),
        ),
    val targetPositions: List<Position> = listOf(Position(gridWidth - 1, gridHeight / 2)),
    val pathCells: Set<Position>,
    val buildAreas: Set<Position> = emptySet(), // Explicit build areas from map
    val attackerWaves: List<AttackerWave>,
    val initialCoins: Int = 100,
    val healthPoints: Int = 10,
    val directSpawnPlan: List<PlannedEnemySpawn>? = null, // Direct spawn plan from editor
    val availableTowers: Set<DefenderType> = DefenderType.entries.toSet(), // Towers available in this level
    val waypoints: List<Waypoint> = emptyList(), // Waypoints for complex pathing
    val editorLevelId: String? = null, // ID of the editor level this was created from
    val mapId: String? = null, // ID of the map this level uses
    val isCommunity: Boolean = false, // True if this level was loaded from the community directory
    val riverTiles: Map<Position, RiverTile> = emptyMap(), // River tiles with flow direction and speed (not walkable in gameplay, but treated as walkable during map validation for levels with ORK, EVIL_WIZARD, or EWHAD enemies)
    val allowAutoAttack: Boolean = true, // If true, shows auto-attack button in end turn confirmation dialog
    val connectedToPreviousLevel: Boolean = false, // If true, player can carry over towers/coins from the previous level
    val isSandbox: Boolean = false, // If true, level is a Sandbox: free building/spawning, no scripted events, cannot be won, no XP
    val targetInfoMap: Map<Position, TargetInfo> = emptyMap(), // Optional metadata (name, type) per target position
    val spawnPointTypeMap: Map<Position, SpawnPointType> = emptyMap(), // Spawn point type per position (LAND or WATER); defaults to LAND if absent
    val supports: LevelSupports = LevelSupports(), // Player-usable supports (placable objects + spell tokens) for this level
    val events: LevelEvents = LevelEvents(), // Scripted events (conditions + actions + predefined story messages) for this level
    // Initial placements (optional) - new nested structure
    val initialData: de.egril.defender.editor.InitialData? = null,
    // Legacy fields for backward compatibility (deprecated - use initialData instead)
    @Deprecated("Use initialData.defenders instead") val initialDefenders: List<de.egril.defender.editor.InitialDefender> = emptyList(),
    @Deprecated("Use initialData.attackers instead") val initialAttackers: List<de.egril.defender.editor.InitialAttacker> = emptyList(),
    @Deprecated("Use initialData.traps instead") val initialTraps: List<de.egril.defender.editor.InitialTrap> = emptyList(),
    @Deprecated("Use initialData.barricades instead") val initialBarricades: List<de.egril.defender.editor.InitialBarricade> = emptyList(),
) {
    /**
     * Get effective initial data, handling both new and legacy formats
     */
    fun getEffectiveInitialData(): de.egril.defender.editor.InitialData {
        // If new format exists, use it
        if (initialData != null) {
            return initialData
        }
        // Fall back to legacy format
        @Suppress("DEPRECATION")
        return de.egril.defender.editor.InitialData(
            defenders = initialDefenders,
            attackers = initialAttackers,
            traps = initialTraps,
            barricades = initialBarricades,
        )
    }

    fun isOnPath(position: Position): Boolean = pathCells.contains(position)

    /**
     * Returns true if the given position is on the enemy traversal area:
     * on the enemy path or at a spawn point. This covers all tiles enemies walk on or start from.
     * Note: [isBuildArea] already prevents placing towers, barricades, and traps on these tiles.
     */
    fun isEnemyTraversable(position: Position): Boolean = isOnPath(position) || isSpawnPoint(position)

    /**
     * Returns true if any enemy can occupy the given position:
     * on the enemy path, at a spawn point, or on a river tile (for units riding rafts).
     * Note: [isBuildArea] already prevents placing towers, barricades, and traps on these tiles.
     */
    fun isEnemyOccupiable(position: Position): Boolean = isOnPath(position) || isSpawnPoint(position) || isRiverTile(position)

    fun isBuildArea(position: Position): Boolean {
        // Cannot build on path itself, spawn points, or targets
        if (isSpawnPoint(position) || targetPositions.contains(position)) return false
        if (isOnPath(position)) return false

        // Can build on explicitly defined build areas
        return buildAreas.contains(position)
    }

    fun isTargetPosition(position: Position): Boolean = targetPositions.contains(position)

    fun isSpawnPoint(position: Position): Boolean = startPositions.contains(position)

    /**
     * Returns true when this spawn point is explicitly marked as WATER.
     */
    fun isWaterSpawnPoint(position: Position): Boolean {
        return isSpawnPoint(position) && getSpawnPointType(position) == SpawnPointType.WATER
    }

    /**
     * Returns the type of the given spawn point (LAND or WATER).
     * Defaults to LAND if the position has no explicit type entry.
     */
    fun getSpawnPointType(position: Position): SpawnPointType = spawnPointTypeMap[position] ?: SpawnPointType.LAND

    /**
     * Returns all spawn points compatible with the given attacker type based on its
     * [AttackerType.canSpawnOnWater] and [AttackerType.canSpawnOnLand] flags.
     * Falls back to all start positions if no compatible point exists.
     */
    fun getCompatibleSpawnPoints(
        attackerType: de.egril.defender.model.AttackerType
    ): List<Position> {
        val compatible = startPositions.filter { pos ->
            when (getSpawnPointType(pos)) {
                SpawnPointType.WATER -> attackerType.canSpawnOnWater
                SpawnPointType.LAND -> attackerType.canSpawnOnLand
            }
        }
        return compatible.ifEmpty { startPositions }
    }

    fun isWaypoint(position: Position): Boolean = waypoints.any { it.position == position }

    fun getWaypointAt(position: Position): Waypoint? = waypoints.firstOrNull { it.position == position }

    fun isRiverTile(position: Position): Boolean = riverTiles.containsKey(position) || isWaterSpawnPoint(position)

    /**
     * Returns true if at least one in-bounds neighboring tile is water.
     *
     * Water includes river tiles and water spawn points (via [isRiverTile]).
     */
    fun hasAdjacentWaterTile(position: Position): Boolean =
        position
            .getHexNeighbors()
            .any { neighbor ->
                neighbor.x in 0 until gridWidth &&
                    neighbor.y in 0 until gridHeight &&
                    isRiverTile(neighbor)
            }

    fun getRiverTile(position: Position): RiverTile? = riverTiles[position]

    fun toLevelInfoEnemiesLevelData(): LevelInfoEnemiesLevelData {
        val enemyCounts = getEnemyTypeCounts()
        return LevelInfoEnemiesLevelData(
            id = "" + this.id,
            name = this.name,
            subtitle = this.subtitle,
            titleKey = this.titleKey, // Include translation key
            subtitleKey = this.subtitleKey, // Include translation key
            initialCoins = this.initialCoins,
            healthPoints = this.healthPoints,
            enemyTypeCounts = enemyCounts,
        )
    }

    /**
     * Convert level to LevelInfoEnemiesLevelData with difficulty modifiers applied
     */
    fun toLevelInfoEnemiesLevelData(difficulty: de.egril.defender.ui.settings.DifficultyLevel): LevelInfoEnemiesLevelData {
        val enemyCounts = getEnemyTypeCounts()

        // Apply difficulty modifiers to enemy counts (Nightmare: 3x, except Ewhad stays 1)
        val modifiedEnemyCounts =
            if (difficulty == de.egril.defender.ui.settings.DifficultyLevel.NIGHTMARE) {
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
            titleKey = this.titleKey, // Include translation key
            subtitleKey = this.subtitleKey, // Include translation key
            initialCoins = modifiedCoins,
            healthPoints = modifiedHP,
            enemyTypeCounts = modifiedEnemyCounts,
        )
    }
}

data class AttackerWave(
    val attackers: List<AttackerType>,
    val spawnDelay: Int = 2, // turns between spawns
)

/**
 * Represents a planned enemy spawn with the turn it will spawn
 */
data class PlannedEnemySpawn(
    val attackerType: AttackerType,
    val spawnTurn: Int,
    val level: Int = 1,
    val spawnPoint: Position? = null, // Fixed spawn point for this enemy (null for backward compatibility)
) {
    val healthPoints: Int get() = attackerType.health * level
}

/**
 * Generate a spawn plan for all waves in a level
 * This calculates when each enemy will spawn based on wave delays
 */
fun generateSpawnPlan(waves: List<AttackerWave>): List<PlannedEnemySpawn> {
    val plan = mutableListOf<PlannedEnemySpawn>()
    var currentTurn = 1 // First enemies spawn at turn 1

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
            currentTurn = lastEnemyTurn + wave.spawnDelay + 2 // Gap between waves
        }
    }

    return plan
}

enum class LevelStatus {
    LOCKED,
    UNLOCKED,
    WON,
}

data class WorldLevel(
    val level: Level,
    var status: LevelStatus = LevelStatus.LOCKED,
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
