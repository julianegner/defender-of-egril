package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.save.SaveFileStorage
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
            name = "test_level",
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
    fun goblinRunnerStartsSlowBuildsMomentumAfterFirstRoundAndResetsAfterDamage() {
        val state = GameState(createTestLevel())
        val runner =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN_RUNNER,
                position = mutableStateOf(Position(0, 1)),
            )

        assertEquals(3, runner.currentBaseMovementSpeed)
        assertEquals(3, calculateEffectiveEnemySpeed(state, runner, runner.position.value))

        runner.updateGoblinRunnerSpeedStateAtEnemyTurnStart(currentTurnNumber = 2)
        assertTrue(runner.goblinRunnerMomentumReady.value)
        assertEquals(0, runner.goblinRunnerUndamagedRounds.value)
        assertEquals(3, runner.currentBaseMovementSpeed)
        assertEquals(3, calculateEffectiveEnemySpeed(state, runner, runner.position.value))

        runner.updateGoblinRunnerSpeedStateAtEnemyTurnStart(currentTurnNumber = 3)
        assertEquals(1, runner.goblinRunnerUndamagedRounds.value)
        assertEquals(4, runner.currentBaseMovementSpeed)
        assertEquals(4, calculateEffectiveEnemySpeed(state, runner, runner.position.value))

        runner.updateGoblinRunnerSpeedStateAtEnemyTurnStart(currentTurnNumber = 4)
        assertEquals(2, runner.goblinRunnerUndamagedRounds.value)
        assertEquals(5, runner.currentBaseMovementSpeed)
        assertEquals(5, calculateEffectiveEnemySpeed(state, runner, runner.position.value))

        runner.recordDamageTaken(1)
        runner.updateGoblinRunnerSpeedStateAtEnemyTurnStart(currentTurnNumber = 5)
        assertEquals(0, runner.goblinRunnerUndamagedRounds.value)
        assertFalse(runner.goblinRunnerTookDamageSinceLastTurn.value)
        assertEquals(3, calculateEffectiveEnemySpeed(state, runner, runner.position.value))
    }

    @Test
    fun firstEnemyTurnKeepsFreshGoblinRunnerAtBaseSpeed() {
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

        assertTrue(runner.goblinRunnerMomentumReady.value)
        assertEquals(0, runner.goblinRunnerUndamagedRounds.value)
        assertEquals(3, calculateEffectiveEnemySpeed(state, runner, runner.position.value))
    }

    @Test
    fun spawnedGoblinRunnerGainsMomentumOnNextEnemyTurn() {
        val state = GameState(createTestLevel())
        val runner =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN_RUNNER,
                position = mutableStateOf(Position(0, 1)),
                goblinRunnerSpawnTurnNumber = mutableStateOf(4),
            )

        runner.updateGoblinRunnerSpeedStateAtEnemyTurnStart(currentTurnNumber = 5)

        assertTrue(runner.goblinRunnerMomentumReady.value)
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
                levelName = "test_level",
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
                            goblinRunnerSpawnTurnNumber = 6,
                            goblinRunnerMomentumReady = true,
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
        assertEquals(6, runner.goblinRunnerSpawnTurnNumber)
        assertTrue(runner.goblinRunnerMomentumReady)
    }

    @Test
    fun saveFileStorageRoundTripPreservesGoblinRunnerMomentumState() {
        val level = createTestLevel()
        val savedGame =
            SavedGame(
                id = "runner-state",
                timestamp = 2L,
                levelId = level.id,
                levelName = level.name,
                turnNumber = 4,
                coins = 10,
                healthPoints = 8,
                phase = GamePhase.PLAYER_TURN,
                defenders = emptyList(),
                attackers =
                    listOf(
                        SavedAttacker(
                            id = 9,
                            type = AttackerType.GOBLIN_RUNNER,
                            position = Position(2, 1),
                            level = 1,
                            currentHealth = 11,
                            isDefeated = false,
                            goblinRunnerUndamagedRounds = 1,
                            goblinRunnerTookDamageSinceLastTurn = true,
                            goblinRunnerSpawnTurnNumber = 3,
                            goblinRunnerMomentumReady = true,
                        ),
                    ),
                nextDefenderId = 1,
                nextAttackerId = 10,
                currentWaveIndex = 0,
                spawnCounter = 0,
                attackersToSpawn = emptyList(),
                fieldEffects = emptyList(),
                traps = emptyList(),
            )

        val restored = SaveFileStorage.convertSavedGameToGameState(savedGame, level)
        val runner = restored.attackers.single()

        assertEquals(AttackerType.GOBLIN_RUNNER, runner.type)
        assertEquals(1, runner.goblinRunnerUndamagedRounds.value)
        assertTrue(runner.goblinRunnerTookDamageSinceLastTurn.value)
        assertEquals(3, runner.goblinRunnerSpawnTurnNumber.value)
        assertTrue(runner.goblinRunnerMomentumReady.value)
    }

    @Test
    fun deserializingOldSaveWithoutGoblinRunnerMomentumFieldsUsesDefaults() {
        val oldSaveJson =
            """{
  "id": "old_runner_save",
  "timestamp": 1234567890,
  "levelId": 1,
  "levelName": "test_level",
  "turnNumber": 5,
  "coins": 100,
  "healthPoints": 10,
  "phase": "PLAYER_TURN",
  "defenders": [],
  "attackers": [
    {
      "id": 1,
      "type": "GOBLIN_RUNNER",
      "position": {"x": 3, "y": 1},
      "level": 1,
      "currentHealth": 12,
      "isDefeated": false,
      "dragonName": null,
      "movementPenalty": 0,
      "bloodlustRoundsLeft": 0,
      "mushroomTurnsRemaining": 0,
      "mushroomLevelBonus": 0
    }
  ],
  "nextDefenderId": 1,
  "nextAttackerId": 2,
  "currentWaveIndex": 0,
  "spawnCounter": 0,
  "attackersToSpawn": [],
  "fieldEffects": [],
  "traps": [],
  "comment": null
}"""

        val loaded = assertNotNull(SaveJsonSerializer.deserializeSavedGame(oldSaveJson))
        val runner = loaded.attackers.single()

        assertEquals(AttackerType.GOBLIN_RUNNER, runner.type)
        assertEquals(0, runner.goblinRunnerUndamagedRounds)
        assertFalse(runner.goblinRunnerTookDamageSinceLastTurn)
        assertEquals(-1, runner.goblinRunnerSpawnTurnNumber)
        assertFalse(runner.goblinRunnerMomentumReady)
    }
}
