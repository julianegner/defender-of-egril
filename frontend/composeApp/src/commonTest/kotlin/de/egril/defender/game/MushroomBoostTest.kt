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
import de.egril.defender.model.displayLevel
import de.egril.defender.model.effectiveLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MushroomBoostTest {
    private fun createTestLevel(): Level =
        Level(
            id = 1,
            name = "Mushroom Test",
            gridWidth = 20,
            gridHeight = 5,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(19, 2)),
            pathCells = (0..19).map { Position(it, 2) }.toSet(),
            buildAreas = setOf(Position(4, 1), Position(5, 1), Position(6, 1)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
        )

    @Test
    fun goblinMushroomBoostDoublesDisplayedAndEffectiveLevel() {
        val goblin =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(1, 2)),
                level = mutableStateOf(2),
                mushroomTurnsRemaining = mutableStateOf(2),
                mushroomLevelBonus = mutableStateOf(2),
            )
        goblin.currentHealth.value = 80

        assertEquals(4, goblin.effectiveLevel)
        assertEquals(4, goblin.displayLevel)
        assertEquals(80, goblin.maxHealth)
        assertEquals(80, goblin.currentHealth.value)
        assertEquals(1, goblin.calculateTargetDamage())
    }

    @Test
    fun levelOneGoblinWithMushroomStillDealsOneTargetDamage() {
        val goblin =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(1, 2)),
                level = mutableStateOf(1),
                mushroomTurnsRemaining = mutableStateOf(2),
                mushroomLevelBonus = mutableStateOf(1),
            )

        assertEquals(1, goblin.calculateTargetDamage())
    }

    @Test
    fun mushroomBoostDoublesMovementAndExpiresAfterTwoEnemyTurns() {
        val state = GameState(createTestLevel())
        val engine = GameEngine(state)
        val goblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(1, 2)),
                level = mutableStateOf(2),
                mushroomTurnsRemaining = mutableStateOf(2),
                mushroomLevelBonus = mutableStateOf(2),
            )
        goblin.currentHealth.value = 80
        state.attackers.add(goblin)

        assertEquals(10, calculateEffectiveEnemySpeed(state, goblin, goblin.position.value))
        assertEquals(4, goblin.effectiveLevel)
        assertEquals(80, goblin.maxHealth)

        state.phase.value = GamePhase.PLAYER_TURN
        engine.startEnemyTurn()
        engine.completeEnemyTurn()
        assertEquals(1, goblin.mushroomTurnsRemaining.value)
        assertEquals(4, goblin.effectiveLevel)
        assertEquals(80, goblin.currentHealth.value)

        state.phase.value = GamePhase.PLAYER_TURN
        engine.startEnemyTurn()
        engine.completeEnemyTurn()
        assertEquals(0, goblin.mushroomTurnsRemaining.value)
        assertEquals(0, goblin.mushroomLevelBonus.value)
        assertEquals(2, goblin.effectiveLevel)
        assertEquals(40, goblin.maxHealth)
        assertEquals(40, goblin.currentHealth.value)
    }

    @Test
    fun mushroomBoostMakesGreenWitchHealTwiceAtDoubleLevel() {
        val state = GameState(createTestLevel())
        val witch =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GREEN_WITCH,
                position = mutableStateOf(Position(5, 2)),
                level = mutableStateOf(2),
                mushroomTurnsRemaining = mutableStateOf(2),
                mushroomLevelBonus = mutableStateOf(2),
            )
        val ogre =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.OGRE,
                position = mutableStateOf(Position(6, 2)),
                level = mutableStateOf(1),
            )
        ogre.currentHealth.value = 10
        state.attackers.addAll(listOf(witch, ogre))

        EnemyAbilitySystem(state, PathfindingSystem(state)).processEnemyAbilities()

        assertEquals(50, ogre.currentHealth.value)
    }

    @Test
    fun mushroomBoostLetsRedWitchDisableTwoTowers() {
        val state = GameState(createTestLevel())
        val firstTower =
            Defender(
                id = state.nextDefenderId.value++,
                type = DefenderType.SPIKE_TOWER,
                position = mutableStateOf(Position(4, 1)),
                level = mutableStateOf(1),
            )
        firstTower.buildTimeRemaining.value = 0
        val secondTower =
            Defender(
                id = state.nextDefenderId.value++,
                type = DefenderType.SPIKE_TOWER,
                position = mutableStateOf(Position(5, 1)),
                level = mutableStateOf(1),
            )
        secondTower.buildTimeRemaining.value = 0
        state.defenders.addAll(listOf(firstTower, secondTower))

        val witch =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.RED_WITCH,
                position = mutableStateOf(Position(5, 2)),
                level = mutableStateOf(1),
                mushroomTurnsRemaining = mutableStateOf(2),
                mushroomLevelBonus = mutableStateOf(1),
            )
        state.attackers.add(witch)

        EnemyAbilitySystem(state, PathfindingSystem(state)).processEnemyAbilities()

        assertTrue(firstTower.isDisabled.value)
        assertTrue(secondTower.isDisabled.value)
    }

}
