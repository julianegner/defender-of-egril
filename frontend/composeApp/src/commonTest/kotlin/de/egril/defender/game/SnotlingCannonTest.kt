package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SnotlingCannonTest {
    private fun createStraightLevel(): Level {
        val pathCells = (0 until 9).map { x -> Position(x, 0) }.toSet()
        return Level(
            id = 1,
            name = "Snotling Cannon Test",
            gridWidth = 9,
            gridHeight = 4,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(8, 0)),
            pathCells = pathCells,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
        )
    }

    @Test
    fun movedSnotlingWithAtLeast120HealthThrowsForwardAndLosesSomeOnLanding() {
        val state = GameState(createStraightLevel())
        val abilities = EnemyAbilitySystem(state, PathfindingSystem(state))
        val snotling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(2, 0)),
                level = mutableStateOf(1),
                currentTarget = mutableStateOf(Position(8, 0)),
            )
        snotling.currentHealth.value = 150
        state.attackers.add(snotling)
        state.enemyTurnStartPositions[snotling.id] = Position(1, 0)

        abilities.processEnemyAbilities()

        assertEquals(100, snotling.currentHealth.value, "The source stack should keep exactly 100 snotlings after a max throw")
        val landingStack =
            state.attackers.firstOrNull {
                !it.isDefeated.value && it.type == AttackerType.SNOTLING && it.position.value == Position(5, 0)
            }
        assertNotNull(landingStack, "A new snotling stack should be created 3 tiles forward on path")
        assertTrue(
            landingStack.currentHealth.value in 40..45,
            "Thrown stack should lose between 10% and 20% of 50 snotlings",
        )
        val throwEffect = state.snotlingCannonThrowEffects.firstOrNull()
        assertNotNull(throwEffect, "A throw animation effect should be registered")
        assertEquals(Position(2, 0), throwEffect.sourcePosition)
        assertEquals(Position(5, 0), throwEffect.targetPosition)
        assertEquals(landingStack.currentHealth.value, throwEffect.thrownCount)
    }

    @Test
    fun snotlingCannonDoesNotTriggerWhenStackDidNotMoveThisTurn() {
        val state = GameState(createStraightLevel())
        val abilities = EnemyAbilitySystem(state, PathfindingSystem(state))
        val snotling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(2, 0)),
                level = mutableStateOf(1),
                currentTarget = mutableStateOf(Position(8, 0)),
            )
        snotling.currentHealth.value = 150
        state.attackers.add(snotling)
        state.enemyTurnStartPositions[snotling.id] = Position(2, 0)

        abilities.processEnemyAbilities()

        assertEquals(150, snotling.currentHealth.value)
        assertTrue(
            state.attackers.none {
                it.id != snotling.id && !it.isDefeated.value && it.type == AttackerType.SNOTLING
            },
        )
        assertTrue(state.snotlingCannonThrowEffects.isEmpty())
    }

    @Test
    fun snotlingCannonDoesNotThrowOntoNonSnotlingOccupiedTile() {
        val state = GameState(createStraightLevel())
        val abilities = EnemyAbilitySystem(state, PathfindingSystem(state))
        val snotling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(2, 0)),
                level = mutableStateOf(1),
                currentTarget = mutableStateOf(Position(8, 0)),
            )
        snotling.currentHealth.value = 150
        val blocker =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(5, 0)),
                level = mutableStateOf(1),
            )
        state.attackers.addAll(listOf(snotling, blocker))
        state.enemyTurnStartPositions[snotling.id] = Position(1, 0)

        abilities.processEnemyAbilities()

        assertEquals(150, snotling.currentHealth.value, "Snotling should not throw when landing tile is occupied by a non-snotling")
        assertTrue(state.snotlingCannonThrowEffects.isEmpty())
    }
}
