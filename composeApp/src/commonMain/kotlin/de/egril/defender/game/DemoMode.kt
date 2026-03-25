package de.egril.defender.game

import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.InitialDefender
import de.egril.defender.model.*

/**
 * Configuration and helper for the demo mode.
 *
 * Demo mode cycles through 3 maps (straight, creek, plains) with pre-placed towers
 * and auto-plays the game (auto-attack + end turn each round).
 * It is started from the world map via the cheat code "demo".
 */
object DemoMode {

    /** Map IDs cycled through in demo mode, in order */
    val DEMO_MAP_IDS = listOf("map_straight", "map_the_creek", "map_plains")

    /** Delay in ms before auto-starting from the initial building phase */
    const val INITIAL_BUILDING_DELAY_MS = 1500L

    /** How long (ms) the tower placement preview is shown before the tower is actually placed */
    const val TOWER_PLACE_DELAY_MS = 800L

    /** How long (ms) to wait after a tower has been placed before continuing */
    const val TOWER_AFTER_PLACE_MS = 500L

    /** How long (ms) the aiming circles are shown before an attack is executed */
    const val ATTACK_AIM_MS = 800L

    /** How long (ms) to wait after an attack before the next one */
    const val ATTACK_AFTER_MS = 200L

    /** Delay in ms before auto-attacking and ending the player turn */
    const val PLAYER_TURN_DELAY_MS = 2000L

    /** Delay in ms between polls while the enemy turn is being processed */
    const val ENEMY_TURN_POLL_MS = 300L

    /** Delay in ms to show the final game state before loading the next demo level */
    const val LEVEL_END_DELAY_MS = 2000L

    /** Delay in ms after level start before any showing info dialogs are dismissed */
    const val INFO_DISMISS_DELAY_MS = 3000L

    /**
     * Pre-placed tower configurations for each demo map.
     * Each list defines the towers that are placed one by one during the INITIAL_BUILDING phase.
     * The second and third maps include wizard towers for area-of-effect coverage.
     */
    val DEMO_TOWERS: Map<String, List<InitialDefender>> = mapOf(
        "map_straight" to listOf(
            InitialDefender(DefenderType.BOW_TOWER, Position(8, 3)),
            InitialDefender(DefenderType.BOW_TOWER, Position(9, 4)),
            InitialDefender(DefenderType.SPEAR_TOWER, Position(12, 2)),
            InitialDefender(DefenderType.BOW_TOWER, Position(13, 3)),
            InitialDefender(DefenderType.SPEAR_TOWER, Position(16, 5)),
            InitialDefender(DefenderType.BOW_TOWER, Position(20, 3)),
            InitialDefender(DefenderType.SPEAR_TOWER, Position(20, 4)),
            InitialDefender(DefenderType.BOW_TOWER, Position(24, 2)),
            InitialDefender(DefenderType.SPEAR_TOWER, Position(28, 5)),
            InitialDefender(DefenderType.BOW_TOWER, Position(32, 3))
        ),
        "map_the_creek" to listOf(
            InitialDefender(DefenderType.BOW_TOWER, Position(8, 1)),
            InitialDefender(DefenderType.SPEAR_TOWER, Position(2, 8)),
            InitialDefender(DefenderType.WIZARD_TOWER, Position(7, 7)),
            InitialDefender(DefenderType.BOW_TOWER, Position(13, 14)),  // raft on river
            InitialDefender(DefenderType.SPEAR_TOWER, Position(9, 19)),
            InitialDefender(DefenderType.BOW_TOWER, Position(10, 18)),
            InitialDefender(DefenderType.WIZARD_TOWER, Position(18, 18)),
            InitialDefender(DefenderType.BOW_TOWER, Position(20, 14)),  // raft on river
            InitialDefender(DefenderType.BOW_TOWER, Position(10, 24)),
            InitialDefender(DefenderType.SPEAR_TOWER, Position(26, 24)),
            InitialDefender(DefenderType.BOW_TOWER, Position(25, 28)),
            InitialDefender(DefenderType.SPEAR_TOWER, Position(25, 29))
        ),
        "map_plains" to listOf(
            InitialDefender(DefenderType.BOW_TOWER, Position(16, 19)),
            InitialDefender(DefenderType.BOW_TOWER, Position(17, 19)),
            InitialDefender(DefenderType.WIZARD_TOWER, Position(19, 16)),
            InitialDefender(DefenderType.WIZARD_TOWER, Position(19, 22)),
            InitialDefender(DefenderType.BOW_TOWER, Position(22, 19)),
            InitialDefender(DefenderType.SPEAR_TOWER, Position(23, 19))
        )
    )

    /** Tower types available for building/upgrading during gameplay for each demo map */
    val DEMO_AVAILABLE_TOWERS: Map<String, Set<DefenderType>> = mapOf(
        "map_straight" to setOf(DefenderType.BOW_TOWER, DefenderType.SPEAR_TOWER),
        "map_the_creek" to setOf(DefenderType.BOW_TOWER, DefenderType.SPEAR_TOWER, DefenderType.WIZARD_TOWER),
        "map_plains" to setOf(DefenderType.BOW_TOWER, DefenderType.SPEAR_TOWER, DefenderType.WIZARD_TOWER)
    )

    /** Starting coins for each demo level (enough to build all initial towers with some extra) */
    val DEMO_START_COINS: Map<String, Int> = mapOf(
        "map_straight" to 250,   // 6 BOW (120) + 4 SPEAR (60) = 180, surplus for upgrades
        "map_the_creek" to 360,  // 4 BOW (80) + 4 SPEAR (60) + 2 WIZARD (100) + 2 rafts BOW (40) = 280, surplus for upgrades
        "map_plains" to 250      // 3 BOW (60) + 1 SPEAR (15) + 2 WIZARD (100) = 175, surplus for upgrades
    )

    /**
     * Simple enemy spawns for the straight map demo.
     * Goblins and orks keep streaming in so the demo stays active.
     */
    private val STRAIGHT_DEMO_SPAWNS: List<PlannedEnemySpawn> = buildList {
        val spawnPoints = listOf(Position(0, 1), Position(0, 4), Position(0, 7))
        for (turn in 1..20) {
            for (sp in spawnPoints) {
                val type = if (turn <= 5) AttackerType.GOBLIN
                else if (turn <= 12) AttackerType.ORK
                else AttackerType.OGRE
                add(PlannedEnemySpawn(type, turn, level = 1, spawnPoint = sp))
            }
        }
    }

    /**
     * Simple enemy spawns for the creek demo.
     */
    private val CREEK_DEMO_SPAWNS: List<PlannedEnemySpawn> = buildList {
        val spawnPoints = listOf(Position(1, 1), Position(1, 4), Position(4, 1))
        for (turn in 1..20) {
            for (sp in spawnPoints) {
                val type = if (turn <= 5) AttackerType.GOBLIN
                else if (turn <= 12) AttackerType.ORK
                else AttackerType.OGRE
                add(PlannedEnemySpawn(type, turn, level = 1, spawnPoint = sp))
            }
        }
    }

    /**
     * Simple enemy spawns for the plains demo.
     */
    private val PLAINS_DEMO_SPAWNS: List<PlannedEnemySpawn> = buildList {
        val spawnPoints = listOf(
            Position(0, 0), Position(39, 0), Position(0, 39), Position(39, 39)
        )
        for (turn in 1..20) {
            for (sp in spawnPoints) {
                val type = if (turn <= 5) AttackerType.GOBLIN
                else if (turn <= 12) AttackerType.ORK
                else AttackerType.OGRE
                add(PlannedEnemySpawn(type, turn, level = 1, spawnPoint = sp))
            }
        }
    }

    private val DEMO_SPAWNS: Map<String, List<PlannedEnemySpawn>> = mapOf(
        "map_straight" to STRAIGHT_DEMO_SPAWNS,
        "map_the_creek" to CREEK_DEMO_SPAWNS,
        "map_plains" to PLAINS_DEMO_SPAWNS
    )

    /** Numeric IDs reserved for the three demo levels (won't overlap with real levels). */
    const val DEMO_LEVEL_ID_BASE = 9000

    /**
     * Creates a demo [Level] for the given [demoIndex] (0, 1, or 2).
     * Towers are NOT pre-placed — they are placed one by one during the INITIAL_BUILDING phase
     * by the auto-play loop so the 800 ms delay is visible.
     * Returns `null` if the map cannot be loaded.
     */
    fun createDemoLevel(demoIndex: Int): Level? {
        val mapId = DEMO_MAP_IDS.getOrNull(demoIndex) ?: return null
        val map = EditorStorage.getMap(mapId) ?: return null

        val spawns = DEMO_SPAWNS[mapId] ?: emptyList()
        val availableTowers = DEMO_AVAILABLE_TOWERS[mapId] ?: emptySet()
        val startCoins = DEMO_START_COINS[mapId] ?: 300

        val targets = map.getTargets()
        if (targets.isEmpty()) return null

        return Level(
            id = DEMO_LEVEL_ID_BASE + demoIndex,
            name = map.name.ifBlank { "Demo" },  // Use map's display name (e.g. "The Creek"); titleKey handles localization
            titleKey = map.nameKey,               // Use map's localized string key (e.g. "map_the_creek_name")
            subtitle = "",
            gridWidth = map.width,
            gridHeight = map.height,
            startPositions = map.getSpawnPoints(),
            targetPositions = targets,
            pathCells = map.getPathCells(),
            buildAreas = map.getBuildAreas(),
            attackerWaves = emptyList(),
            initialCoins = startCoins,
            healthPoints = 10,
            directSpawnPlan = spawns,
            availableTowers = availableTowers,
            riverTiles = map.getRiverTilesMap(),
            mapId = mapId
            // No initialData: towers are placed dynamically by startDemoAutoPlay()
        )
    }
}
