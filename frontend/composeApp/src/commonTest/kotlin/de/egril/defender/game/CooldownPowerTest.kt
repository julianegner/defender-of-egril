package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.CooldownPower
import de.egril.defender.model.CooldownPowerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.LevelSupports
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for cooldown-based support powers (reusable per-level abilities).
 */
class CooldownPowerTest {
    private fun createLevel(supports: LevelSupports): Level =
        Level(
            id = 1,
            name = "Test Level",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            pathCells = (0..5).map { Position(it, 0) }.toSet(),
            buildAreas = setOf(Position(2, 2), Position(3, 2)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            supports = supports,
        )

    @Test
    fun testStartActivePowerIsReadyImmediately() {
        val supports =
            LevelSupports(
                cooldownPowers = listOf(CooldownPower(CooldownPowerType.MANA_WELL, startActive = true)),
            )
        val state = GameState(createLevel(supports))
        state.initializePrePlacedElements()

        assertEquals(0, state.cooldownPowerReadyIn[CooldownPowerType.MANA_WELL], "Start-active power should be ready at once")
    }

    @Test
    fun testInactivePowerStartsOnCooldown() {
        val supports =
            LevelSupports(
                cooldownPowers =
                    listOf(CooldownPower(CooldownPowerType.COIN_SURGE, cooldownTurns = 4, startActive = false)),
            )
        val state = GameState(createLevel(supports))
        state.initializePrePlacedElements()

        assertEquals(4, state.cooldownPowerReadyIn[CooldownPowerType.COIN_SURGE], "Inactive power should start on cooldown")
    }

    @Test
    fun testCooldownTicksDownEachRound() {
        val supports =
            LevelSupports(
                cooldownPowers = listOf(CooldownPower(CooldownPowerType.MANA_WELL, startActive = true)),
            )
        val state = GameState(createLevel(supports))
        val engine = GameEngine(state)
        engine.startFirstPlayerTurn()

        // Simulate the power being used: put it on a 3-turn cooldown
        state.cooldownPowerReadyIn[CooldownPowerType.MANA_WELL] = 3

        engine.startEnemyTurn()
        engine.completeEnemyTurn()

        assertEquals(2, state.cooldownPowerReadyIn[CooldownPowerType.MANA_WELL], "Cooldown should tick down by one per round")
    }

    @Test
    fun testCoinSurgeMultiplierResetsAfterRound() {
        val supports =
            LevelSupports(
                cooldownPowers = listOf(CooldownPower(CooldownPowerType.COIN_SURGE, startActive = true)),
            )
        val state = GameState(createLevel(supports))
        val engine = GameEngine(state)
        engine.startFirstPlayerTurn()

        state.coinSurgeActive.value = true
        assertEquals(2, state.coinSurgeMultiplier(), "Coin surge should double coins while active")

        engine.startEnemyTurn()
        engine.completeEnemyTurn()

        assertFalse(state.coinSurgeActive.value, "Coin surge should be cleared at the end of the round")
        assertEquals(1, state.coinSurgeMultiplier())
    }

    @Test
    fun testCoinSurgeDoublesCombatRewards() {
        val state = GameState(createLevel(LevelSupports()))
        val engine = GameEngine(state)
        engine.startFirstPlayerTurn()

        val goblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(2, 0)),
                level = mutableStateOf(1),
            )
        goblin.isDefeated.value = true
        state.attackers.add(goblin)

        state.coinSurgeActive.value = true
        engine.processDefeatedAttackers()

        // Goblin reward is 5 per level; with Coin Surge active it is doubled to 10.
        assertEquals(10, state.pendingCoinGains.value, "Coin Surge should double the coin reward")
    }

    @Test
    fun testLevelSupportsEmptinessConsidersCooldownPowers() {
        val supports = LevelSupports(cooldownPowers = listOf(CooldownPower(CooldownPowerType.MANA_WELL)))
        assertTrue(supports.isNotEmpty(), "Supports with only a cooldown power should not be empty")
        assertTrue(LevelSupports().isEmpty(), "Supports with nothing should be empty")
    }
}
