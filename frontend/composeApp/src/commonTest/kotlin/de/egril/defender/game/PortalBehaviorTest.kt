package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.PlannedEnemySpawn
import de.egril.defender.model.Portal
import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.hexDistanceTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PortalBehaviorTest {
    @Test
    fun zytharWithinTenTilesMovesTowardTargetEvenWithoutPortal() {
        val start = Position(20, 5)
        val target = Position(29, 5)
        val level =
            Level(
                id = 0,
                name = "Zythar Close Push",
                gridWidth = 30,
                gridHeight = 10,
                startPositions = listOf(Position(0, 5)),
                targetPositions = listOf(target),
                pathCells = (0 until 30).flatMap { x -> (0 until 10).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
            )
        val state = GameState(level)
        val zythar =
            Attacker(
                id = 1,
                type = AttackerType.ZYTHAR_THE_RIFTCALLER,
                position = mutableStateOf(start),
                currentTarget = mutableStateOf(target),
            )
        state.attackers.add(zythar)

        val engine = GameEngine(state)
        val beforeDist = start.hexDistanceTo(target)
        val movements = engine.calculateEnemyTurnMovements()
        movements.allMovementSteps.forEach { step ->
            step.forEach { (attackerId, pos) ->
                engine.applyMovement(attackerId, pos)
            }
        }

        val endPos = zythar.position.value
        assertNotEquals(start, endPos, "Zythar should move when already within 10 tiles of a target")
        assertTrue(endPos.hexDistanceTo(target) < beforeDist, "Zythar should move toward the target")
    }

    @Test
    fun spawnOnPortalEntryIsRedirectedToExitAdjacentTile() {
        val entry = Position(2, 5)
        val exit = Position(10, 5)
        val target = Position(14, 5)
        val level =
            Level(
                id = 1,
                name = "Portal Spawn Redirect",
                gridWidth = 15,
                gridHeight = 10,
                startPositions = listOf(entry),
                targetPositions = listOf(target),
                pathCells = (0 until 15).flatMap { x -> (0 until 10).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                directSpawnPlan =
                    listOf(
                        PlannedEnemySpawn(
                            attackerType = AttackerType.GOBLIN,
                            spawnTurn = 1,
                            spawnPoint = entry,
                        ),
                    ),
                initialCoins = 100,
                healthPoints = 10,
            )
        val state = GameState(level)
        state.activePortals.add(
            Portal(
                id = 1,
                entryPosition = entry,
                exitPosition = exit,
                villainId = 99,
                runeIndex = 0,
            ),
        )
        state.turnNumber.value = 1

        val engine = GameEngine(state)
        engine.spawnEnemyTurnAttackers()

        val spawned = state.attackers.single { !it.isDefeated.value }
        val pos = spawned.position.value
        assertNotEquals(entry, pos, "Portal entry must remain empty")
        assertNotEquals(exit, pos, "Portal exit must remain empty")
        assertTrue(exit.getHexNeighbors().contains(pos), "Spawned unit should be moved adjacent to portal exit")
    }

    @Test
    fun enemyUsesPortalRouteWhenItIsShorterToTarget() {
        val start = Position(1, 5)
        val entry = Position(2, 5)
        val exit = Position(18, 5)
        val target = Position(20, 5)
        val level =
            Level(
                id = 2,
                name = "Portal Shorter Path",
                gridWidth = 22,
                gridHeight = 10,
                startPositions = listOf(Position(0, 5)),
                targetPositions = listOf(target),
                pathCells = (0 until 22).flatMap { x -> (0 until 10).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
            )
        val state = GameState(level)
        state.activePortals.add(
            Portal(
                id = 1,
                entryPosition = entry,
                exitPosition = exit,
                villainId = 88,
                runeIndex = 1,
            ),
        )

        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(start),
                currentTarget = mutableStateOf(target),
            )
        state.attackers.add(attacker)

        val engine = GameEngine(state)
        val beforeDist = start.hexDistanceTo(target)
        val movements = engine.calculateEnemyTurnMovements()
        movements.allMovementSteps.forEach { step ->
            step.forEach { (attackerId, pos) ->
                engine.applyMovement(attackerId, pos)
            }
        }

        val endPos = attacker.position.value
        val afterDist = endPos.hexDistanceTo(target)
        assertTrue(afterDist <= beforeDist - 3, "Portal route should provide a shorter jump toward the target")
        assertTrue(!state.isPortalTile(endPos), "Portal tiles must remain empty")
    }

    @Test
    fun demonlingCreatesNextPortalWhenTenTilesCloserThanExistingPortal() {
        val target = Position(39, 6)
        val demonlingPos = Position(30, 6) // distance 9 to target
        val existingExit = Position(20, 6) // distance 19 to target
        val level =
            Level(
                id = 3,
                name = "Portal Advance Threshold",
                gridWidth = 40,
                gridHeight = 12,
                startPositions = listOf(Position(0, 6)),
                targetPositions = listOf(target),
                pathCells = (0 until 40).flatMap { x -> (0 until 12).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
            )
        val state = GameState(level)
        val zythar =
            Attacker(
                id = 1,
                type = AttackerType.ZYTHAR_THE_RIFTCALLER,
                position = mutableStateOf(Position(5, 6)),
                currentTarget = mutableStateOf(target),
            )
        val demonling =
            Attacker(
                id = 2,
                type = AttackerType.DEMONLING,
                position = mutableStateOf(demonlingPos),
                currentTarget = mutableStateOf(target),
            )
        state.attackers.add(zythar)
        state.attackers.add(demonling)
        state.activePortals.add(
            Portal(
                id = 1,
                entryPosition = Position(6, 6),
                exitPosition = existingExit,
                villainId = zythar.id,
                runeIndex = 2,
            ),
        )

        val engine = GameEngine(state)
        engine.applyMovement(demonling.id, demonling.position.value)

        assertTrue(demonling.isDefeated.value, "Demonling should be consumed to create the new portal")
        assertEquals(2, state.activePortals.size, "A new portal should be created when demonling is 10 tiles closer")
        assertTrue(state.activePortals.any { it.exitPosition == demonlingPos }, "New portal exit should be at demonling position")
    }

    @Test
    fun zytharUsesPortalAndPushesWhenTenOrLessTilesRemain() {
        val start = Position(5, 5)
        val entry = Position(6, 5)
        val exit = Position(20, 5)
        val target = Position(29, 5)
        val level =
            Level(
                id = 4,
                name = "Zythar Push After Portal",
                gridWidth = 30,
                gridHeight = 10,
                startPositions = listOf(Position(0, 5)),
                targetPositions = listOf(target),
                pathCells = (0 until 30).flatMap { x -> (0 until 10).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
            )
        val state = GameState(level)
        state.activePortals.add(
            Portal(
                id = 1,
                entryPosition = entry,
                exitPosition = exit,
                villainId = 1,
                runeIndex = 0,
            ),
        )
        val zythar =
            Attacker(
                id = 1,
                type = AttackerType.ZYTHAR_THE_RIFTCALLER,
                position = mutableStateOf(start),
                currentTarget = mutableStateOf(target),
            )
        state.attackers.add(zythar)

        val engine = GameEngine(state)
        val movements = engine.calculateEnemyTurnMovements()
        movements.allMovementSteps.forEach { step ->
            step.forEach { (attackerId, pos) ->
                engine.applyMovement(attackerId, pos)
            }
        }

        val endPos = zythar.position.value
        assertTrue(zythar.teleportedThisTurn.value, "Zythar should move through the portal")
        assertTrue(!state.isPortalTile(endPos), "Portal tiles must remain empty")
        assertTrue(endPos.hexDistanceTo(target) <= 10, "Zythar should push toward the target when close enough after teleport")
    }

    @Test
    fun zytharUsesPortalAndRepositionsSafelyWhenMoreThanTenTilesRemain() {
        val start = Position(5, 5)
        val entry = Position(6, 5)
        val exit = Position(12, 5)
        val target = Position(29, 5)
        val level =
            Level(
                id = 5,
                name = "Zythar Safe Reposition After Portal",
                gridWidth = 30,
                gridHeight = 10,
                startPositions = listOf(Position(0, 5)),
                targetPositions = listOf(target),
                pathCells = (0 until 30).flatMap { x -> (0 until 10).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
            )
        val state = GameState(level)
        state.activePortals.add(
            Portal(
                id = 1,
                entryPosition = entry,
                exitPosition = exit,
                villainId = 1,
                runeIndex = 1,
            ),
        )
        val zythar =
            Attacker(
                id = 1,
                type = AttackerType.ZYTHAR_THE_RIFTCALLER,
                position = mutableStateOf(start),
                currentTarget = mutableStateOf(target),
            )
        state.attackers.add(zythar)

        val landingDistance =
            exit
                .getHexNeighbors()
                .minOf { neighbor -> neighbor.hexDistanceTo(target) }

        val engine = GameEngine(state)
        val movements = engine.calculateEnemyTurnMovements()
        movements.allMovementSteps.forEach { step ->
            step.forEach { (attackerId, pos) ->
                engine.applyMovement(attackerId, pos)
            }
        }

        val endPos = zythar.position.value
        val endDistance = endPos.hexDistanceTo(target)
        assertTrue(zythar.teleportedThisTurn.value, "Zythar should move through the portal")
        assertTrue(!state.isPortalTile(endPos), "Portal tiles must remain empty")
        assertTrue(endDistance > 10, "Zythar should not hard-push to the target when it is still far away")
        assertTrue(
            endDistance >= landingDistance,
            "When the target is still far away, Zythar should move to a safer spot instead of moving closer immediately",
        )
    }
}
