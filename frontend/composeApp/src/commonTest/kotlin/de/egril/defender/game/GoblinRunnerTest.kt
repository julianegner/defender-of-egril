package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.save.SaveJsonSerializer
import de.egril.defender.save.SavedAttacker
import de.egril.defender.save.SavedGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoblinRunnerTest {
    private fun createTestLevel(): Level =
        Level(
            id = 1,
            name = "Goblin Runner Test",
            gridWidth = 10,
            gridHeight = 3,
            startPositions = listOf(Position(0, 1)),
            targetPositions = listOf(Position(9, 1)),
            pathCells = (0..9).map { Position(it, 1) }.toSet(),
            buildAreas = setOf(Position(4, 0)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
        )

    @Test
    fun goblinRunnerStartsSlowBuildsMomentumAndResetsAfterDamage() {
        val state = GameState(createTestLevel())
        val runner =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN_RUNNER,
                position = mutableStateOf(Position(0, 1)),
            )

        assertEquals(3, runner.baseMovementSpeed)
        assertEquals(3, calculateEffectiveEnemySpeed(state, runner, runner.position.value))

        runner.updateGoblinRunnerSpeedStateAtEnemyTurnStart()
        assertEquals(1, runner.goblinRunnerUndamagedRounds.value)
        assertEquals(4, runner.baseMovementSpeed)
        assertEquals(4, calculateEffectiveEnemySpeed(state, runner, runner.position.value))

        runner.updateGoblinRunnerSpeedStateAtEnemyTurnStart()
        assertEquals(2, runner.goblinRunnerUndamagedRounds.value)
        assertEquals(5, runner.baseMovementSpeed)
        assertEquals(5, calculateEffectiveEnemySpeed(state, runner, runner.position.value))

        runner.recordDamageTaken(1)
        runner.updateGoblinRunnerSpeedStateAtEnemyTurnStart()
        assertEquals(0, runner.goblinRunnerUndamagedRounds.value)
        assertFalse(runner.goblinRunnerTookDamageSinceLastTurn.value)
        assertEquals(3, calculateEffectiveEnemySpeed(state, runner, runner.position.value))
    }

    @Test
    fun startEnemyTurnIncrementsGoblinRunnerMomentumWhenUndamaged() {
        val state = GameState(createTestLevel(), phase = mutableStateOf(GamePhase.PLAYER_TURN))
        val engine = GameEngine(state)
        val runner =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN_RUNNER,
                position = mutableStateOf(Position(1, 1)),
            )
        state.attackers.add(runner)

        engine.startEnemyTurn()

        assertEquals(1, runner.goblinRunnerUndamagedRounds.value)
        assertEquals(4, calculateEffectiveEnemySpeed(state, runner, runner.position.value))
    }

    @Test
    fun savedGameRoundTripPreservesGoblinRunnerMomentumState() {
        val savedGame =
            SavedGame(
                id = "runner",
                timestamp = 1L,
                levelId = 1,
                levelName = "Goblin Runner Test",
                turnNumber = 7,
                coins = 50,
                healthPoints = 9,
                phase = GamePhase.PLAYER_TURN,
                defenders = emptyList(),
                attackers =
                    listOf(
                        SavedAttacker(
                            id = 1,
                            type = AttackerType.GOBLIN_RUNNER,
                            position = Position(3, 1),
                            level = 1,
                            currentHealth = 12,
                            isDefeated = false,
                            goblinRunnerUndamagedRounds = 2,
                            goblinRunnerTookDamageSinceLastTurn = true,
                        ),
                    ),
                nextDefenderId = 1,
                nextAttackerId = 2,
                currentWaveIndex = 0,
                spawnCounter = 0,
                attackersToSpawn = emptyList(),
                fieldEffects = emptyList(),
                traps = emptyList(),
            )

        val roundTrip = assertNotNull(SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(savedGame)))
        val runner = roundTrip.attackers.single()

        assertEquals(AttackerType.GOBLIN_RUNNER, runner.type)
        assertEquals(2, runner.goblinRunnerUndamagedRounds)
        assertTrue(runner.goblinRunnerTookDamageSinceLastTurn)
    }
}
