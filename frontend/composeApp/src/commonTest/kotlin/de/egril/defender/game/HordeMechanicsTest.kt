package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HordeMechanicsTest {
    private fun createLevel(pathCells: Set<Position>): Level =
        Level(
            id = 1,
            name = "Horde Test",
            gridWidth = 5,
            gridHeight = 5,
            startPositions = listOf(Position(1, 1)),
            targetPositions = listOf(Position(4, 1)),
            pathCells = pathCells,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            waaghEnabled = true,
        )

    @Test
    fun orkEatsTenSnotlingsAndGainsBloodlust() {
        val level = createLevel(setOf(Position(1, 1), Position(2, 1), Position(3, 1), Position(4, 1)))
        val state = GameState(level)
        val abilities = EnemyAbilitySystem(state, PathfindingSystem(state))

        val ork =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(1, 1)),
                level = mutableStateOf(1),
            )
        ork.currentHealth.value = 35

        val snotlingStack =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(2, 1)),
                level = mutableStateOf(1),
            )
        snotlingStack.currentHealth.value = 12

        state.attackers.addAll(listOf(ork, snotlingStack))

        abilities.processHordeEating()

        assertEquals(40, ork.currentHealth.value)
        assertEquals(1, ork.bloodlustRoundsLeft.value)
        assertEquals(2, snotlingStack.currentHealth.value)
        assertEquals(2, state.waaghPoints.value)
    }

    @Test
    fun ogreEatingGoblinHealsAtLeastTenHp() {
        val level = createLevel(setOf(Position(1, 1), Position(2, 1), Position(3, 1), Position(4, 1)))
        val state = GameState(level)
        val abilities = EnemyAbilitySystem(state, PathfindingSystem(state))

        val ogre =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.OGRE,
                position = mutableStateOf(Position(2, 1)),
                level = mutableStateOf(1),
            )
        ogre.currentHealth.value = 60

        val goblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(2, 1)),
                level = mutableStateOf(1),
            )
        goblin.currentHealth.value = 6

        state.attackers.addAll(listOf(ogre, goblin))

        abilities.processHordeEating()

        assertEquals(70, ogre.currentHealth.value)
        assertTrue(goblin.isDefeated.value)
        assertTrue(goblin.wasMerged.value)
    }

    @Test
    fun snotlingGrowthOverflowsToAdjacentTiles() {
        val center = Position(1, 1)
        val east = Position(2, 1)
        val level = createLevel(setOf(center, east, Position(4, 1)))
        val state = GameState(level)
        val abilities = EnemyAbilitySystem(state, PathfindingSystem(state))

        val stack =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(center),
                level = mutableStateOf(1),
            )
        stack.currentHealth.value = 240
        state.attackers.add(stack)

        abilities.processSnotlingGrowth()

        val overflowStack =
            state.attackers
                .filter { !it.isDefeated.value && it.type == AttackerType.SNOTLING && it.position.value == east }
                .firstOrNull()

        assertEquals(250, stack.currentHealth.value)
        assertNotNull(overflowStack)
        assertEquals(50, overflowStack.currentHealth.value)
        assertEquals(300, state.attackers.filter { !it.isDefeated.value && it.type == AttackerType.SNOTLING }.sumOf { it.currentHealth.value })
    }

    @Test
    fun hittingOrkChargesWaaghMeter() {
        val level =
            Level(
                id = 1,
                name = "Combat Test",
                gridWidth = 4,
                gridHeight = 3,
                startPositions = listOf(Position(1, 0)),
                targetPositions = listOf(Position(3, 0)),
                pathCells = setOf(Position(1, 0), Position(2, 0), Position(3, 0)),
                buildAreas = setOf(Position(0, 0)),
                attackerWaves = emptyList(),
                waaghEnabled = true,
            )
        val state = GameState(level)
        val engine = GameEngine(state)

        val defender =
            Defender(
                id = state.nextDefenderId.value++,
                type = DefenderType.SPIKE_TOWER,
                position = mutableStateOf(Position(0, 0)),
                placedOnTurn = 0,
            )
        defender.buildTimeRemaining.value = 0
        defender.actionsRemaining.value = 1
        state.defenders.add(defender)

        val ork =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(1, 0)),
                level = mutableStateOf(1),
            )
        state.attackers.add(ork)

        assertTrue(engine.defenderAttack(defender.id, ork.id))
        assertEquals(3, state.waaghPoints.value)
    }

    @Test
    fun killingSnotlingChargesWaaghMeterByHpLost() {
        val level =
            Level(
                id = 1,
                name = "Combat Test",
                gridWidth = 4,
                gridHeight = 3,
                startPositions = listOf(Position(1, 0)),
                targetPositions = listOf(Position(3, 0)),
                pathCells = setOf(Position(1, 0), Position(2, 0), Position(3, 0)),
                buildAreas = setOf(Position(0, 0)),
                attackerWaves = emptyList(),
                waaghEnabled = true,
            )
        val state = GameState(level)
        val engine = GameEngine(state)

        val defender =
            Defender(
                id = state.nextDefenderId.value++,
                type = DefenderType.SPIKE_TOWER,
                position = mutableStateOf(Position(0, 0)),
                placedOnTurn = 0,
            )
        defender.buildTimeRemaining.value = 0
        defender.actionsRemaining.value = 1
        state.defenders.add(defender)

        val snotling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(1, 0)),
                level = mutableStateOf(1),
            )
        state.attackers.add(snotling)

        assertTrue(engine.defenderAttack(defender.id, snotling.id))
        assertEquals(1, state.waaghPoints.value)
    }

    @Test
    fun waaghFrenzyLastsTwoEnemyTurnsAndThenResets() {
        val level = createLevel(setOf(Position(1, 1), Position(2, 1), Position(3, 1), Position(4, 1)))
        val state = GameState(level)
        val engine = GameEngine(state)
        state.phase.value = GamePhase.PLAYER_TURN
        state.waaghPoints.value = 100

        engine.startEnemyTurn()
        assertTrue(state.waaghFrenzyActive.value)
        assertEquals(2, state.waaghFrenzyRoundsLeft.value)
        engine.completeEnemyTurn()
        assertEquals(1, state.waaghFrenzyRoundsLeft.value)

        state.phase.value = GamePhase.PLAYER_TURN
        engine.startEnemyTurn()
        engine.completeEnemyTurn()

        assertFalse(state.waaghFrenzyActive.value)
        assertEquals(0, state.waaghPoints.value)
        assertEquals(0, state.waaghFrenzyRoundsLeft.value)
    }
}
