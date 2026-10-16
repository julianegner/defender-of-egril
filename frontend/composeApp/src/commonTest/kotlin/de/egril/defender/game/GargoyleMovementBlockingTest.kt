package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Gargoyle
import de.egril.defender.model.GargoyleRules
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GargoyleMovementBlockingTest {
    private fun createState(): GameState {
        val level =
            Level(
                id = 1,
                name = "Gargoyle Movement Test",
                gridWidth = 12,
                gridHeight = 3,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(10, 0)),
                pathCells = (0..10).map { Position(it, 0) }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
                availableTowers = emptySet(),
            )
        return GameState(level = level)
    }

    private fun addInactiveGargoyle(state: GameState, position: Position): Gargoyle {
        val gargoyle =
            Gargoyle(
                id = state.nextGargoyleId.value++,
                position = mutableStateOf(position),
                level = mutableStateOf(1),
                currentHealth = mutableStateOf(GargoyleRules.HEALTH_PER_LEVEL),
            )
        state.gargoyles.add(gargoyle)
        return gargoyle
    }

    private fun addAttacker(state: GameState, type: AttackerType, position: Position): Attacker {
        val attacker =
            Attacker(
                id = state.nextAttackerId.value++,
                type = type,
                position = mutableStateOf(position),
                currentHealth = mutableStateOf(type.health),
            )
        state.attackers.add(attacker)
        return attacker
    }

    @Test
    fun enemyCannotStepOntoInactiveGargoyleTile() {
        val state = createState()
        val engine = GameEngine(state)
        val goblin = addAttacker(state, AttackerType.GOBLIN, Position(1, 0))
        addInactiveGargoyle(state, Position(2, 0))

        engine.applyMovement(goblin.id, Position(2, 0))

        assertEquals(Position(1, 0), goblin.position.value)
    }

    @Test
    fun plannedEnemyMovementNeverEntersInactiveGargoyleTile() {
        val state = createState()
        val engine = GameEngine(state)
        addAttacker(state, AttackerType.GOBLIN, Position(1, 0))
        addInactiveGargoyle(state, Position(2, 0))

        val movements = engine.calculateEnemyTurnMovements()

        assertFalse(movements.allMovementSteps.flatten().any { (_, pos) -> pos == Position(2, 0) })
    }

    @Test
    fun activeGargoyleCannotStepOntoTileOfAnotherUnit() {
        val state = createState()
        val engine = GameEngine(state)
        val gargoyleAttacker = addAttacker(state, AttackerType.GARGOYLE, Position(1, 0))
        val goblin = addAttacker(state, AttackerType.GOBLIN, Position(2, 0))

        engine.applyMovement(gargoyleAttacker.id, Position(2, 0))
        assertEquals(Position(1, 0), gargoyleAttacker.position.value)

        val movements = engine.calculateEnemyTurnMovements()
        val firstStepOfGargoyle =
            movements.allMovementSteps.firstOrNull()
                ?.firstOrNull { (id, _) -> id == gargoyleAttacker.id }
        assertFalse(firstStepOfGargoyle?.second == goblin.position.value)
    }
}
