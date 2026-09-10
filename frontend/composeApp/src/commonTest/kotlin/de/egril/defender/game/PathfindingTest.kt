package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.RepositoryLoader
import de.egril.defender.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for enemy pathfinding to ensure units don't get stuck
 */
class PathfindingTest {
    /**
     * Test that enemy units can find a path and don't get stuck when all moves have equal cost.
     * This reproduces the bug where units would get stuck because A* pathfinding didn't have
     * a proper tiebreaker when multiple positions had the same fScore.
     */
    @Test
    fun testEnemyDoesNotGetStuckWithEqualCosts() {
        // Create a simple level with a path from (0,0) to (5,0)
        val level =
            Level(
                id = 1,
                name = "Test Level",
                gridWidth = 10,
                gridHeight = 6,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(9, 0)),
                pathCells = (0..9).map { x -> Position(x, 0) }.toSet(),
                attackerWaves =
                    listOf(
                        AttackerWave(
                            attackers = listOf(AttackerType.GOBLIN),
                            spawnDelay = 0,
                        ),
                    ),
                initialCoins = 100,
                healthPoints = 10,
            )

        val state = GameState(level)
        val engine = GameEngine(state)

        // Spawn an enemy at start position
        val enemy =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 0)),
                level = mutableStateOf(1),
            )
        state.attackers.add(enemy)

        // Record starting position
        val startPos = enemy.position.value

        // Simulate enemy movement for a turn (goblin has speed 5)
        // In a turn, goblin should move 5 steps closer to the target
        val movements = engine.calculateEnemyTurnMovements()

        // Apply movements
        for (movementStep in movements.allMovementSteps) {
            for ((attackerId, newPosition) in movementStep) {
                engine.applyMovement(attackerId, newPosition)
            }
        }

        val endPos = enemy.position.value

        // Verify that the enemy moved towards the target (should have moved right, increasing x)
        assertTrue(
            endPos.x > startPos.x,
            "Enemy should have moved towards target. Started at ${startPos.x}, ended at ${endPos.x}",
        )

        // Verify that enemy moved the expected distance (5 steps for goblin)
        val distanceMoved = endPos.x - startPos.x
        assertTrue(
            distanceMoved == 5,
            "Goblin should have moved 5 steps (speed=5), but moved $distanceMoved",
        )
    }

    /**
     * Test pathfinding with multiple valid paths of equal cost
     */
    @Test
    fun testPathfindingChoosesConsistentPath() {
        // Create a level with Y-shaped path where enemy can go left or right
        // Both paths have equal cost, but we want consistent behavior
        val pathCells =
            setOf(
                // Main path
                Position(0, 2),
                Position(1, 2),
                Position(2, 2),
                // Fork at (2,2) - can go to (2,1) or (2,3)
                Position(2, 1),
                Position(3, 1),
                Position(4, 1),
                Position(5, 1),
                Position(2, 3),
                Position(3, 3),
                Position(4, 3),
                Position(5, 3),
                // Both paths merge at (6,2)
                Position(6, 2),
                Position(7, 2),
                Position(8, 2),
                Position(9, 2),
            )

        val level =
            Level(
                id = 1,
                name = "Fork Test",
                gridWidth = 10,
                gridHeight = 6,
                startPositions = listOf(Position(0, 2)),
                targetPositions = listOf(Position(9, 2)),
                pathCells = pathCells,
                attackerWaves =
                    listOf(
                        AttackerWave(
                            attackers = listOf(AttackerType.GOBLIN),
                            spawnDelay = 0,
                        ),
                    ),
                initialCoins = 100,
                healthPoints = 10,
            )

        val state = GameState(level)
        val engine = GameEngine(state)

        // Spawn an enemy at (2, 2) - the fork position
        val enemy =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(2, 2)),
                level = mutableStateOf(1),
            )
        state.attackers.add(enemy)

        // Calculate movements
        val movements = engine.calculateEnemyTurnMovements()

        // Apply movements (ork has speed 1)
        for (movementStep in movements.allMovementSteps) {
            for ((attackerId, newPosition) in movementStep) {
                engine.applyMovement(attackerId, newPosition)
            }
        }

        val endPos = enemy.position.value

        // Enemy should have moved from (2,2) and chosen one of the paths
        // The key is that it should choose consistently (not randomly)
        assertTrue(
            endPos != Position(2, 2),
            "Enemy should have moved from the fork position",
        )

        // Verify enemy is still on a valid path cell
        assertTrue(
            pathCells.contains(endPos),
            "Enemy should be on a valid path cell. Position: $endPos",
        )
    }

    /**
     * Test that pathfinding is deterministic when positions have equal fScores.
     * This specifically tests the tiebreaker fix - when multiple neighbors have the same
     * fScore, the algorithm should consistently prefer the one closest to the goal.
     */
    @Test
    fun testPathfindingIsDeterministic() {
        // Create a simple horizontal path
        val level =
            Level(
                id = 1,
                name = "Determinism Test",
                gridWidth = 10,
                gridHeight = 6,
                startPositions = listOf(Position(0, 2)),
                targetPositions = listOf(Position(9, 2)),
                pathCells =
                    (0..9)
                        .flatMap { x ->
                            listOf(Position(x, 1), Position(x, 2), Position(x, 3))
                        }.toSet(),
                attackerWaves =
                    listOf(
                        AttackerWave(
                            attackers = listOf(AttackerType.GOBLIN),
                            spawnDelay = 0,
                        ),
                    ),
                initialCoins = 100,
                healthPoints = 10,
            )

        val state = GameState(level)
        val engine = GameEngine(state)

        // Run the same scenario multiple times and ensure we get the same path
        val paths = mutableListOf<List<Position>>()

        repeat(5) {
            // Spawn an enemy at start
            val enemy =
                Attacker(
                    id = 1 + it,
                    type = AttackerType.GOBLIN,
                    position = mutableStateOf(Position(0, 2)),
                    level = mutableStateOf(1),
                )

            // Clear attackers and add this one
            state.attackers.clear()
            state.attackers.add(enemy)

            // Calculate movements
            val movements = engine.calculateEnemyTurnMovements()

            // Record path taken
            val path = mutableListOf(Position(0, 2))
            for (movementStep in movements.allMovementSteps) {
                for ((attackerId, newPosition) in movementStep) {
                    path.add(newPosition)
                }
            }
            paths.add(path)
        }

        // All paths should be the same (deterministic)
        val firstPath = paths[0]
        for (i in 1 until paths.size) {
            assertTrue(
                paths[i] == firstPath,
                "Path $i should be the same as first path. Expected: $firstPath, Got: ${paths[i]}",
            )
        }
    }

    @Test
    fun ghostCanMoveThroughBarricadesButLandsOnEmptyPathTile() {
        val pathCells =
            setOf(
                Position(0, 0),
                Position(1, 0),
                Position(2, 0),
                Position(3, 0),
                Position(4, 0),
            )
        val level =
            Level(
                id = 1,
                name = "Ghost Test",
                gridWidth = 5,
                gridHeight = 3,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                pathCells = pathCells,
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
            )

        val state = GameState(level)
        state.barricades.add(Barricade(id = 1, position = Position(1, 0), healthPoints = mutableStateOf(50), defenderId = 0))
        state.barricades.add(Barricade(id = 2, position = Position(2, 0), healthPoints = mutableStateOf(50), defenderId = 0))

        val ghost =
            Attacker(
                id = 1,
                type = AttackerType.GHOST,
                position = mutableStateOf(Position(0, 0)),
                level = mutableStateOf(1),
            )
        state.attackers.add(ghost)

        val engine = GameEngine(state)
        val movements = engine.calculateEnemyTurnMovements()

        assertEquals(1, movements.allMovementSteps.size)
        assertEquals(listOf(1 to Position(3, 0)), movements.allMovementSteps.single())
    }

    @Test
    fun pirateCannotMoveOntoRaftTile() {
        val level =
            Level(
                id = 1,
                name = "Pirate Raft Block Test",
                gridWidth = 4,
                gridHeight = 3,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(2, 0)),
                pathCells = emptySet(),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
                riverTiles =
                    mapOf(
                        Position(0, 0) to RiverTile(Position(0, 0), RiverFlow.EAST, 1),
                        Position(1, 0) to RiverTile(Position(1, 0), RiverFlow.EAST, 1),
                        Position(2, 0) to RiverTile(Position(2, 0), RiverFlow.EAST, 1),
                    ),
            )

        val state = GameState(level)
        state.rafts.add(
            Raft(
                id = 1,
                defenderId = 42,
                currentPosition = mutableStateOf(Position(1, 0)),
            ),
        )

        val pirate =
            Attacker(
                id = 1,
                type = AttackerType.PIRATE,
                position = mutableStateOf(Position(0, 0)),
                level = mutableStateOf(1),
            )
        state.attackers.add(pirate)

        val pathfinding = PathfindingSystem(state)
        val path = pathfinding.findPath(Position(0, 0), Position(2, 0), pirate)

        assertTrue(path.none { it == Position(1, 0) }, "Pirate path must not include a tile occupied by a raft")
    }

    @Test
    fun landUnitsAvoidRiverButBridgeBuildersMayUseRiverTiles() {
        val level =
            Level(
                id = 2,
                name = "River Traversal Rules",
                gridWidth = 5,
                gridHeight = 3,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(Position(4, 1)),
                pathCells =
                    setOf(
                        Position(0, 1),
                        Position(0, 0),
                        Position(1, 0),
                        Position(2, 0),
                        Position(3, 0),
                        Position(4, 0),
                    ),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
                riverTiles =
                    mapOf(
                        Position(1, 1) to RiverTile(Position(1, 1), RiverFlow.EAST, 1),
                        Position(2, 1) to RiverTile(Position(2, 1), RiverFlow.EAST, 1),
                        Position(3, 1) to RiverTile(Position(3, 1), RiverFlow.EAST, 1),
                    ),
            )

        val state = GameState(level)
        val pathfinding = PathfindingSystem(state)
        val goblin = Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(Position(0, 1)))
        val ork = Attacker(id = 2, type = AttackerType.ORK, position = mutableStateOf(Position(0, 1)))

        val goblinPath = pathfinding.findPath(Position(0, 1), Position(4, 1), goblin)
        val orkPath = pathfinding.findPath(Position(0, 1), Position(4, 1), ork)
        val riverTiles = setOf(Position(1, 1), Position(2, 1), Position(3, 1))

        assertTrue(goblinPath.none { it in riverTiles }, "Land units without bridge-building must avoid river tiles")
        assertTrue(orkPath.any { it in riverTiles }, "Bridge-building land units should be able to use river tiles")
    }

    @Test
    fun landUnitsUseBridgesAsPassableTiles() {
        val level =
            Level(
                id = 3,
                name = "Bridge Is Passable",
                gridWidth = 4,
                gridHeight = 3,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(Position(3, 1)),
                pathCells = setOf(Position(0, 1), Position(3, 1)),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
                riverTiles =
                    mapOf(
                        Position(1, 1) to RiverTile(Position(1, 1), RiverFlow.EAST, 1),
                        Position(2, 1) to RiverTile(Position(2, 1), RiverFlow.EAST, 1),
                    ),
            )
        val state = GameState(level)
        state.bridges.add(
            Bridge(
                id = 1,
                type = BridgeType.WOODEN,
                positions = listOf(Position(1, 1), Position(2, 1)),
                currentHealth = mutableStateOf(50),
                isDestroyed = mutableStateOf(false),
                turnsRemaining = mutableStateOf(0),
                createdByAttackerId = 0,
                createdOnTurn = 1,
            ),
        )
        val pathfinding = PathfindingSystem(state)
        val goblin = Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(Position(0, 1)))

        val path = pathfinding.findPath(Position(0, 1), Position(3, 1), goblin)

        assertTrue(path.contains(Position(1, 1)) && path.contains(Position(2, 1)), "Land units should traverse active bridge tiles")
    }

    @Test
    fun goblinCanReachWaypointGoalViaBridgeInsteadOfGreedyRiverEdgeStall() {
        val waypointGoal = Position(5, 0) // Not on path; used as explicit current target (waypoint-like)
        val bridgeTile = Position(3, 0)
        val level =
            Level(
                id = 4,
                name = "Waypoint Bridge Routing",
                gridWidth = 7,
                gridHeight = 3,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(Position(6, 1)),
                pathCells =
                    setOf(
                        Position(0, 1),
                        Position(1, 1),
                        Position(2, 1),
                        Position(2, 0),
                        Position(4, 0),
                        Position(4, 1),
                        Position(5, 1),
                        Position(6, 1),
                    ),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
                riverTiles = mapOf(Position(3, 1) to RiverTile(Position(3, 1), RiverFlow.EAST, 1)),
            )

        val state = GameState(level)
        state.bridges.add(
            Bridge(
                id = 1,
                type = BridgeType.WOODEN,
                positions = listOf(bridgeTile),
                currentHealth = mutableStateOf(50),
                isDestroyed = mutableStateOf(false),
                turnsRemaining = mutableStateOf(0),
                createdByAttackerId = 0,
                createdOnTurn = 1,
            ),
        )
        val pathfinding = PathfindingSystem(state)
        val goblin = Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(Position(0, 1)))

        val path = pathfinding.findPath(Position(0, 1), waypointGoal, goblin)

        assertEquals(waypointGoal, path.last(), "Pathfinding should reach the current waypoint target instead of stalling at river edge")
        assertTrue(path.contains(bridgeTile), "Goblin should be able to route via bridge tiles")
        assertTrue(path.none { it == Position(3, 1) }, "Goblin must not treat plain river tiles as traversable")
    }

    @Test
    fun goblinFindsRouteOnTownAtRiverLevelUsingBridgeOrLandPath() =
        runTest {
            val editorMap = RepositoryLoader.loadMap("map_the_town_at_the_river") ?: return@runTest
            val editorLevel = RepositoryLoader.loadLevel("the_town_at_the_river") ?: return@runTest
            val runtimeLevel = createRuntimeLevelForPathfinding(editorMap, editorLevel)
            val state = GameState(runtimeLevel).also { it.initializePrePlacedElements() }
            val pathfinding = PathfindingSystem(state)

            val spawn = Position(2, 2)
            val waypointGoal = Position(9, 49)
            val goblin = Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(spawn), level = mutableStateOf(1))

            val path = pathfinding.findPath(spawn, waypointGoal, goblin)
            val activeBridgeTiles = state.bridges.filter { !it.isDestroyed.value }.flatMap { it.positions }.toSet()
            val illegalRiverTiles = path.filter { runtimeLevel.isRiverTile(it) && it !in activeBridgeTiles }

            assertEquals(waypointGoal, path.last(), "Goblin should find a complete path to the waypoint target on this level")
            assertTrue(illegalRiverTiles.isEmpty(), "Goblin path must not traverse plain river tiles: $illegalRiverTiles")
        }

    private fun createRuntimeLevelForPathfinding(
        map: EditorMap,
        level: EditorLevel,
    ): Level {
        val waypoints = level.waypoints.map { waypoint -> Waypoint(waypoint.position, waypoint.nextTargetPosition) }
        val pathCells = map.getPathCells().toMutableSet().apply { addAll(waypoints.map { it.position }) }
        return Level(
            id = 999,
            name = level.title,
            subtitle = level.subtitle,
            titleKey = level.titleKey,
            subtitleKey = level.subtitleKey,
            gridWidth = map.width,
            gridHeight = map.height,
            startPositions = map.getSpawnPoints(),
            targetPositions = map.getTargets(),
            pathCells = pathCells,
            buildAreas = map.getBuildAreas(),
            attackerWaves = emptyList(),
            initialCoins = level.startCoins,
            healthPoints = level.startHealthPoints,
            directSpawnPlan =
                level.enemySpawns.map { spawn ->
                    PlannedEnemySpawn(spawn.attackerType, spawn.level, spawn.spawnTurn, spawn.spawnPoint)
                },
            availableTowers = level.availableTowers,
            waypoints = waypoints,
            mapId = level.mapId,
            riverTiles = map.getRiverTilesMap(),
            allowAutoAttack = level.allowAutoAttack,
            connectedToPreviousLevel = level.connectedToPreviousLevel,
            isSandbox = level.isSandbox,
            waaghEnabled = level.waaghEnabled,
            supports = level.supports,
            events = level.events,
            initialData = level.getEffectiveInitialData(),
        )
    }
}
