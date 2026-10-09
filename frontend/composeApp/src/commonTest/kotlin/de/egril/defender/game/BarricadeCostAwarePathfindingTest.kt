package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.Barricade
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that [PathfindingSystem.findPathThroughBarricades] weighs both distance and barricade
 * destruction time when choosing between alternate barricaded routes, per the requirement that an
 * enemy should prefer a route through a single weak barricade over a route through one or more
 * tougher barricades, even if the tougher route is geometrically shorter.
 */
class BarricadeCostAwarePathfindingTest {
    /**
     * Builds a small level with two parallel horizontal lanes (y=1 and y=2) connecting a shared
     * start (x=0) to a shared target (x=6). Both lanes have identical length, so the only
     * difference between them is the barricade(s) placed on each lane.
     */
    private fun buildTwoLaneLevel(): Level {
        val pathCells =
            (0..6).flatMap { x -> listOf(Position(x, 1), Position(x, 2)) }.toSet() +
                setOf(Position(0, 0), Position(6, 3)) // connect start/target to both lanes
        return Level(
            id = 1,
            name = "Two Lane Barricade Test",
            gridWidth = 8,
            gridHeight = 5,
            startPositions = listOf(Position(0, 1)),
            targetPositions = listOf(Position(6, 1)),
            pathCells = pathCells,
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            initialCoins = 100,
            availableTowers = emptySet(),
        )
    }

    private fun makeAttacker(position: Position): Attacker =
        Attacker(
            1,
            AttackerType.GOBLIN,
            mutableStateOf(position),
            mutableStateOf(1),
        )

    private fun makeBarricade(
        id: Int,
        position: Position,
        hp: Int,
    ): Barricade =
        Barricade(
            id = id,
            position = position,
            healthPoints = mutableStateOf(hp),
            defenderId = -1,
        )

    @Test
    fun prefersRouteThroughSingleWeakerBarricadeOverToughOne() {
        val level = buildTwoLaneLevel()
        val start = Position(0, 1)
        val target = Position(6, 1)
        val gameState = GameState(level)
        val attacker = makeAttacker(start)
        gameState.attackers.add(attacker)

        // Lane y=1: a single very tough barricade (150 HP) directly on the straight route.
        gameState.barricades.add(makeBarricade(1, Position(3, 1), hp = 150))
        // Lane y=2: a single much weaker barricade (50 HP).
        gameState.barricades.add(makeBarricade(2, Position(3, 2), hp = 50))

        val pathfinding = PathfindingSystem(gameState)
        val path = pathfinding.findPathThroughBarricades(start, target, attacker)

        assertEquals(target, path.last())
        assertTrue(path.contains(Position(3, 2)), "Expected the path to route through the weaker barricade's lane")
        assertTrue(!path.contains(Position(3, 1)), "Expected the path to avoid the tougher barricade's lane")
    }

    @Test
    fun stillPrefersSingleWeakBarricadeOverTwoWeakBarricadesOnOtherLane() {
        val level = buildTwoLaneLevel()
        val start = Position(0, 1)
        val target = Position(6, 1)
        val gameState = GameState(level)
        val attacker = makeAttacker(start)
        gameState.attackers.add(attacker)

        // Lane y=1: two barricades of 50 HP each (100 total destruction cost + extra step).
        gameState.barricades.add(makeBarricade(1, Position(2, 1), hp = 50))
        gameState.barricades.add(makeBarricade(2, Position(4, 1), hp = 50))
        // Lane y=2: a single 50 HP barricade.
        gameState.barricades.add(makeBarricade(3, Position(3, 2), hp = 50))

        val pathfinding = PathfindingSystem(gameState)
        val path = pathfinding.findPathThroughBarricades(start, target, attacker)

        assertEquals(target, path.last())
        assertTrue(path.contains(Position(3, 2)), "Expected the path to route through the lane with only one barricade")
    }
}
