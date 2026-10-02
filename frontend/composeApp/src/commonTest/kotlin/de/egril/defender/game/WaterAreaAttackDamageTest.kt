package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import kotlin.test.Test
import kotlin.test.assertTrue

class WaterAreaAttackDamageTest {
    private fun createWaterAttackLevel(): Level {
        val riverPos = Position(2, 2)
        return Level(
            id = 1,
            name = "Water attack test",
            gridWidth = 6,
            gridHeight = 6,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 5)),
            pathCells = setOf(Position(0, 0), Position(1, 0), Position(2, 0)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            riverTiles = mapOf(riverPos to RiverTile(position = riverPos, flowDirection = RiverFlow.EAST, flowSpeed = 1)),
        )
    }

    @Test
    fun wizardFireballDamagesPirateOnWater() {
        val level = createWaterAttackLevel()
        val gameState = GameState(level)
        val engine = GameEngine(gameState)

        val wizard =
            Defender(
                id = 1,
                type = DefenderType.WIZARD_TOWER,
                position = mutableStateOf(Position(2, 1)),
                level = mutableStateOf(1),
                buildTimeRemaining = mutableStateOf(0),
            )
        wizard.resetActions()
        gameState.defenders.add(wizard)

        val pirate =
            Attacker(
                id = 1,
                type = AttackerType.PIRATE,
                position = mutableStateOf(Position(2, 2)),
                level = mutableStateOf(1),
            )
        gameState.attackers.add(pirate)
        val beforeHp = pirate.currentHealth.value

        assertTrue(engine.defenderAttackPosition(wizard.id, pirate.position.value))
        assertTrue(pirate.currentHealth.value < beforeHp, "Fireball should damage pirate on water")
    }

    @Test
    fun alchemyAcidDamagesPirateOnWater() {
        val level = createWaterAttackLevel()
        val gameState = GameState(level)
        val engine = GameEngine(gameState)

        val alchemy =
            Defender(
                id = 1,
                type = DefenderType.ALCHEMY_TOWER,
                position = mutableStateOf(Position(2, 1)),
                level = mutableStateOf(1),
                buildTimeRemaining = mutableStateOf(0),
            )
        alchemy.resetActions()
        gameState.defenders.add(alchemy)

        val pirate =
            Attacker(
                id = 1,
                type = AttackerType.PIRATE,
                position = mutableStateOf(Position(2, 2)),
                level = mutableStateOf(1),
            )
        gameState.attackers.add(pirate)
        val beforeHp = pirate.currentHealth.value

        assertTrue(engine.defenderAttackPosition(alchemy.id, pirate.position.value))
        assertTrue(pirate.currentHealth.value < beforeHp, "Acid should damage pirate on water")
    }
}
