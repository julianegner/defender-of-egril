package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.PlannedEnemySpawn
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnPointType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for water/land spawn point compatibility introduced in issue #689.
 *
 * Rules:
 * - Land spawn points (default): only enemies with [AttackerType.canSpawnOnLand] may use them.
 * - Water spawn points: only enemies with [AttackerType.canSpawnOnWater] may use them.
 * - The Kraken is water-only; Cap'n Roderich can use both; all other enemies are land-only.
 */
class WaterSpawnPointTest {
    // ─── AttackerType flag sanity checks ────────────────────────────────────────

    @Test
    fun krakeniIsWaterOnlySpawn() {
        assertFalse(AttackerType.THE_KRAKEN.canSpawnOnLand, "Kraken must not spawn on land")
        assertTrue(AttackerType.THE_KRAKEN.canSpawnOnWater, "Kraken must spawn on water")
    }

    @Test
    fun roderichCanSpawnOnBothLandAndWater() {
        assertTrue(AttackerType.CAPTAIN_RODERICH.canSpawnOnLand, "Roderich must be able to spawn on land")
        assertTrue(AttackerType.CAPTAIN_RODERICH.canSpawnOnWater, "Roderich must be able to spawn on water")
    }

    @Test
    fun pirateCanSpawnOnBothLandAndWater() {
        assertTrue(AttackerType.PIRATE.canSpawnOnLand, "Pirate must be able to spawn on land")
        assertTrue(AttackerType.PIRATE.canSpawnOnWater, "Pirate must be able to spawn on water")
    }

    @Test
    fun pirateCanTraverseRiverTiles() {
        assertTrue(AttackerType.PIRATE.canTraverseRiver, "Pirate must be able to move on water tiles")
    }

    @Test
    fun pirateCanSpawnOnBothLandAndWaterFromFlags() {
        assertTrue(AttackerType.PIRATE.canSpawnOnLand, "Pirate must be able to spawn on land")
        assertTrue(AttackerType.PIRATE.canSpawnOnWater, "Pirate must be able to spawn on water")
    }

    @Test
    fun regularEnemiesAreSpawnOnLandOnly() {
        val landOnlyEnemies =
            listOf(
                AttackerType.GOBLIN,
                AttackerType.ORK,
                AttackerType.OGRE,
                AttackerType.SKELETON,
                AttackerType.EVIL_WIZARD,
                AttackerType.RED_WITCH,
                AttackerType.GREEN_WITCH,
                AttackerType.BLUE_DEMON,
                AttackerType.RED_DEMON,
            )
        for (type in landOnlyEnemies) {
            assertTrue(type.canSpawnOnLand, "$type must be able to spawn on land")
            assertFalse(type.canSpawnOnWater, "$type must NOT be able to spawn on water")
        }
    }

    // ─── Level.getCompatibleSpawnPoints ─────────────────────────────────────────

    private val landSpawn = Position(0, 0)
    private val waterSpawn = Position(0, 2)

    private fun levelWithBothSpawnTypes(): Level {
        val pathCells =
            (0..5).map { x -> Position(x, 0) }.toSet() +
                (0..5).map { x -> Position(x, 2) }.toSet()
        return Level(
            id = 1,
            name = "Mixed Spawn Test",
            gridWidth = 6,
            gridHeight = 3,
            startPositions = listOf(landSpawn, waterSpawn),
            targetPositions = listOf(Position(5, 0), Position(5, 2)),
            pathCells = pathCells,
            attackerWaves = emptyList(),
            spawnPointTypeMap =
                mapOf(
                    landSpawn to SpawnPointType.LAND,
                    waterSpawn to SpawnPointType.WATER,
                ),
        )
    }

    @Test
    fun krakenOnlyGetsWaterSpawnPoints() {
        val level = levelWithBothSpawnTypes()
        val compatible = level.getCompatibleSpawnPoints(AttackerType.THE_KRAKEN)
        assertEquals(listOf(waterSpawn), compatible, "Kraken must only use the water spawn point")
    }

    @Test
    fun goblinOnlyGetsLandSpawnPoints() {
        val level = levelWithBothSpawnTypes()
        val compatible = level.getCompatibleSpawnPoints(AttackerType.GOBLIN)
        assertEquals(listOf(landSpawn), compatible, "Goblin must only use the land spawn point")
    }

    @Test
    fun roderichGetsBothSpawnPoints() {
        val level = levelWithBothSpawnTypes()
        val compatible = level.getCompatibleSpawnPoints(AttackerType.CAPTAIN_RODERICH)
        assertEquals(2, compatible.size, "Roderich must be compatible with both spawn point types")
        assertTrue(compatible.contains(landSpawn))
        assertTrue(compatible.contains(waterSpawn))
    }

    @Test
    fun pirateGetsBothSpawnPoints() {
        val level = levelWithBothSpawnTypes()
        val compatible = level.getCompatibleSpawnPoints(AttackerType.PIRATE)
        assertEquals(2, compatible.size, "Pirate must be compatible with both spawn point types")
        assertTrue(compatible.contains(landSpawn))
        assertTrue(compatible.contains(waterSpawn))
    }

    @Test
    fun fallsBackToAllSpawnPointsWhenNoneCompatible() {
        // Level with only water spawn points, but a land-only enemy
        val pathCells = (0..5).map { x -> Position(x, 0) }.toSet()
        val level =
            Level(
                id = 2,
                name = "Water Only Map",
                gridWidth = 6,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(5, 0)),
                pathCells = pathCells,
                attackerWaves = emptyList(),
                spawnPointTypeMap = mapOf(Position(0, 0) to SpawnPointType.WATER),
            )
        // GOBLIN cannot spawn on water, but there is no land spawn point → fallback to all
        val compatible = level.getCompatibleSpawnPoints(AttackerType.GOBLIN)
        assertEquals(level.startPositions, compatible, "Should fall back to all spawn points when none are compatible")
    }

    @Test
    fun waterSpawnPointIsTreatedAsRiverTile() {
        val level = levelWithBothSpawnTypes()
        assertTrue(level.isRiverTile(waterSpawn), "Water spawn point must be treated as a river tile")
        assertFalse(level.isRiverTile(landSpawn), "Land spawn point must not be treated as a river tile")
    }

    // ─── Spawn-point filtering during actual game spawning ───────────────────────

    @Test
    fun krakenSpawnsOnWaterSpawnPointNotLand() {
        val level = levelWithBothSpawnTypes()
        val spawnPlan =
            listOf(
                PlannedEnemySpawn(AttackerType.THE_KRAKEN, spawnTurn = 1, level = 1),
            )
        val state = GameState(level = level.copy(directSpawnPlan = spawnPlan))
        val engine = GameEngine(state)
        engine.startFirstPlayerTurn()

        assertEquals(1, state.attackers.size)
        val krakenPos =
            state.attackers
                .first()
                .position
                .value
        assertEquals(waterSpawn, krakenPos, "Kraken must have spawned at the water spawn point")
    }

    @Test
    fun orkSpawnsOnLandSpawnPointNotWater() {
        val level = levelWithBothSpawnTypes()
        val spawnPlan =
            listOf(
                PlannedEnemySpawn(AttackerType.ORK, spawnTurn = 1, level = 1),
            )
        val state = GameState(level = level.copy(directSpawnPlan = spawnPlan))
        val engine = GameEngine(state)
        engine.startFirstPlayerTurn()

        assertEquals(1, state.attackers.size)
        val orkPos =
            state.attackers
                .first()
                .position
                .value
        assertEquals(landSpawn, orkPos, "Ork must have spawned at the land spawn point")
    }

    @Test
    fun krakenMoveTowardsUsesRiverNeighborsWhenNoPathFound() {
        val spawn = Position(0, 0)
        val nextRiver = Position(1, 0)
        val unreachableTarget = Position(5, 5)
        val level =
            Level(
                id = 3,
                name = "Kraken MoveTowards",
                gridWidth = 6,
                gridHeight = 6,
                startPositions = listOf(spawn),
                targetPositions = listOf(Position(5, 0)),
                pathCells = emptySet(),
                attackerWaves = emptyList(),
                riverTiles =
                    mapOf(
                        nextRiver to
                            de.egril.defender.model
                                .RiverTile(nextRiver),
                    ),
                spawnPointTypeMap = mapOf(spawn to SpawnPointType.WATER),
            )
        val state = GameState(level = level)
        val pathfinding = PathfindingSystem(state)
        val kraken =
            Attacker(
                id = 1,
                type = AttackerType.THE_KRAKEN,
                position = mutableStateOf(spawn),
            )

        val next = pathfinding.moveTowards(spawn, unreachableTarget, kraken)
        assertEquals(nextRiver, next, "Kraken fallback movement must follow river-capable neighbors")
    }

    @Test
    fun roderichCanSpawnFromFixedWaterSpawnPoint() {
        val waterSpawn = Position(0, 0)
        val level =
            Level(
                id = 4,
                name = "Roderich Fixed Water Spawn",
                gridWidth = 4,
                gridHeight = 2,
                startPositions = listOf(waterSpawn),
                targetPositions = listOf(Position(3, 0)),
                pathCells = emptySet(),
                attackerWaves = emptyList(),
                spawnPointTypeMap = mapOf(waterSpawn to SpawnPointType.WATER),
                directSpawnPlan =
                    listOf(
                        PlannedEnemySpawn(AttackerType.CAPTAIN_RODERICH, spawnTurn = 1, level = 1, spawnPoint = waterSpawn),
                    ),
            )

        val state = GameState(level = level)
        val engine = GameEngine(state)
        engine.startFirstPlayerTurn()

        assertEquals(1, state.attackers.size, "Roderich should spawn")
        assertEquals(
            waterSpawn,
            state.attackers
                .first()
                .position
                .value,
            "Roderich should spawn at the fixed water spawn point",
        )
    }

    @Test
    fun findFreePositionNearAllowsRiverFallbackForRoderich() {
        val waterSpawn = Position(0, 0)
        val riverNeighbor = Position(1, 0)
        val level =
            Level(
                id = 5,
                name = "Roderich River Fallback",
                gridWidth = 4,
                gridHeight = 2,
                startPositions = listOf(waterSpawn),
                targetPositions = listOf(Position(3, 0)),
                pathCells = emptySet(),
                attackerWaves = emptyList(),
                riverTiles =
                    mapOf(
                        riverNeighbor to
                            de.egril.defender.model
                                .RiverTile(riverNeighbor),
                    ),
                spawnPointTypeMap = mapOf(waterSpawn to SpawnPointType.WATER),
            )
        val state = GameState(level = level)
        val pathfinding = PathfindingSystem(state)
        val movement = EnemyMovementSystem(state, pathfinding)
        state.attackers.add(
            Attacker(
                id = 99,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(waterSpawn),
            ),
        )

        val fallback =
            movement.findFreePositionNear(
                preferredSpawnPoint = waterSpawn,
                waterOnly = false,
                canUseRiver = true,
            )
        assertEquals(riverNeighbor, fallback, "River-capable spawn fallback should use neighboring river tiles")
    }

    @Test
    fun pirateMovementCanEnterWaterAfterLandSpawn() {
            val landSpawn = Position(0, 0)
            val waterTile = Position(1, 0)
            val target = Position(3, 0)
            val level =
                Level(
                    id = 6,
                    name = "Pirate Land To Water",
                    gridWidth = 4,
                    gridHeight = 2,
                    startPositions = listOf(landSpawn),
                    targetPositions = listOf(target),
                    pathCells = setOf(landSpawn, target),
                    attackerWaves = emptyList(),
                    riverTiles = mapOf(waterTile to de.egril.defender.model.RiverTile(waterTile)),
                )
            val state = GameState(level = level)
            val pathfinding = PathfindingSystem(state)
            val pirate =
                Attacker(
                    id = 2,
                    type = AttackerType.PIRATE,
                    position = mutableStateOf(landSpawn),
                )

            val next = pathfinding.moveTowards(landSpawn, target, pirate)

            assertEquals(waterTile, next, "Pirate spawned on land should still be able to move onto water")
    }

    @Test
    fun pirateSidestepsOntoWaterWhenPreferredTileIsPermanentlyCongested() {
        // Regression test for: pirates getting stuck right at the water's edge whenever their
        // preferred next tile is occupied by another attacker that never moves out of the way
        // (e.g. a pile-up of several pirates arriving faster than the front of the queue can
        // clear). Movement.findAlternativePosition used to only consider path/bridge tiles as
        // sidestep candidates, never river tiles, so a river-traversal unit blocked at the
        // shoreline had no water tile to divert to and simply stood still on land forever,
        // instead of stepping onto a different, unobstructed adjacent water tile.
        val landSpawn = Position(3, 3)
        val target = Position(9, 3)
        val waterTiles =
            (0..9).flatMap { x -> (0..9).map { y -> Position(x, y) } }
                .filter { it != landSpawn }
                .associateWith { de.egril.defender.model.RiverTile(it) }
        val level =
            Level(
                id = 7,
                name = "Pirate Congestion At Shoreline",
                gridWidth = 10,
                gridHeight = 10,
                startPositions = listOf(landSpawn),
                targetPositions = listOf(target),
                pathCells = setOf(landSpawn, target),
                attackerWaves = emptyList(),
                riverTiles = waterTiles,
            )
        val state = GameState(level = level)

        // Determine, via a throwaway pathfinding probe, which water tile a lone pirate would
        // pick as its very first step out of landSpawn — that is the tile we permanently occupy.
        val probePirate =
            Attacker(id = -1, type = AttackerType.PIRATE, position = mutableStateOf(landSpawn))
        val preferredFirstStep = PathfindingSystem(state).findPath(landSpawn, target, probePirate)[1]

        val engine = GameEngine(state)

        // Blocker sits permanently on the pirates' shared preferred tile (simulating a queue
        // front that never clears, e.g. because it is itself waiting on something further along).
        val blocker =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.PIRATE,
                position = mutableStateOf(preferredFirstStep),
            )
        // Frozen so it never moves, regardless of speed — simulating a queue front that never
        // clears (e.g. a pirate genuinely unable to advance further for some other reason).
        state.activeSpellEffects.add(
            de.egril.defender.model.ActiveSpellEffect(
                spell = de.egril.defender.model.SpellType.FREEZE_SPELL,
                attackerId = blocker.id,
                turnsRemaining = 100,
            ),
        )
        val stalledPirate =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.PIRATE,
                position = mutableStateOf(landSpawn),
            )
        state.attackers.addAll(listOf(blocker, stalledPirate))

        // Simulate several enemy turns; the blocker never moves (frozen), so the conflict at
        // preferredFirstStep recurs every turn.
        repeat(3) {
            val movements = engine.calculateEnemyTurnMovements()
            for (step in movements.allMovementSteps) {
                for ((attackerId, newPos) in step) {
                    engine.applyMovement(attackerId, newPos)
                }
            }
        }
        assertEquals(preferredFirstStep, blocker.position.value, "Blocker must remain in place (test setup sanity check)")
        assertTrue(
            level.isRiverTile(stalledPirate.position.value),
            "Pirate blocked at its preferred shoreline tile must sidestep onto an adjacent water tile " +
                "instead of staying stuck on land at ${stalledPirate.position.value}",
        )
    }
}
