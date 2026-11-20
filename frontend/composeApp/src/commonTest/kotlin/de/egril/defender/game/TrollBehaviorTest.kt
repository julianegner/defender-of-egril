package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Barricade
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrollBehaviorTest {
    private fun createLinearLevel(): Level =
        Level(
            id = 1,
            name = "Troll Test Level",
            gridWidth = 8,
            gridHeight = 3,
            startPositions = listOf(Position(0, 1)),
            targetPositions = listOf(Position(7, 1)),
            pathCells = (0..7).map { Position(it, 1) }.toSet(),
            attackerWaves = emptyList(),
            initialCoins = 0,
            healthPoints = 10,
        )

    @Test
    fun trollTrampleDoesNotGrantCoinRewardOrCoinAnimation() {
        val state = GameState(createLinearLevel())
        val engine = GameEngine(state)

        val troll =
            Attacker(
                id = 1,
                type = AttackerType.TROLL,
                position = mutableStateOf(Position(1, 1)),
                level = mutableStateOf(2),
            )
        val goblin =
            Attacker(
                id = 2,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(2, 1)),
                level = mutableStateOf(3),
            )
        state.attackers.addAll(listOf(troll, goblin))

        val movementPlan = engine.calculateEnemyTurnMovements()
        for (step in movementPlan.allMovementSteps) {
            for ((attackerId, newPosition) in step) {
                engine.applyMovement(attackerId, newPosition)
            }
        }

        engine.processDefeatedAttackers()

        assertEquals(0, state.pendingCoinGains.value, "Troll trample should not stage coin gains")
        assertTrue(state.coinGainEffects.isEmpty(), "Troll trample should not queue coin gain animation")
    }

    @Test
    fun trollAttacksBarricadeDuringMovementWithTenTimesDamage() {
        val state = GameState(createLinearLevel())
        val engine = GameEngine(state)

        val troll =
            Attacker(
                id = 1,
                type = AttackerType.TROLL,
                position = mutableStateOf(Position(1, 1)),
                level = mutableStateOf(2),
            )
        state.attackers.add(troll)

        val barricade =
            Barricade(
                id = 1,
                position = Position(2, 1),
                healthPoints = mutableStateOf(25),
                defenderId = 1,
            )
        state.barricades.add(barricade)

        engine.applyMovement(troll.id, Position(2, 1))

        assertEquals(5, barricade.healthPoints.value, "Troll should deal 20 damage (level 2 x 10) to barricade")
        assertEquals(Position(1, 1), troll.position.value, "Troll should stay in place if the barricade survives")
    }

    @Test
    fun trollAttacksAdjacentBarricadeOnPausedTurn() {
        val state = GameState(createLinearLevel())
        val engine = GameEngine(state)

        val troll =
            Attacker(
                id = 1,
                type = AttackerType.TROLL,
                position = mutableStateOf(Position(1, 1)),
                level = mutableStateOf(2),
            )
        // Simulate that one movement turn already elapsed so the next turn is the pause turn.
        troll.movementTurnsElapsed.value = 1
        state.attackers.add(troll)

        val barricade =
            Barricade(
                id = 1,
                position = Position(2, 1),
                healthPoints = mutableStateOf(25),
                defenderId = 1,
            )
        state.barricades.add(barricade)

        val movementPlan = engine.calculateEnemyTurnMovements()
        assertTrue(movementPlan.allMovementSteps.isEmpty(), "Paused troll should not move this turn")
        assertTrue(
            movementPlan.attackersStoppedByBarricade.any { it.first.id == troll.id && it.second == barricade.position },
            "Paused troll should still pick an adjacent barricade to attack",
        )

        movementPlan.attackersStoppedByBarricade.forEach { (attacker, position) ->
            engine.attackBarricade(position, attacker)
        }

        assertEquals(5, barricade.healthPoints.value, "Paused troll should still deal 20 damage to adjacent barricade")
        assertEquals(Position(1, 1), troll.position.value, "Paused troll must stay in place while attacking")
    }
}
