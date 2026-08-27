package de.egril.defender.game.gameengine

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.game.BarricadeSystem
import de.egril.defender.game.MineOperations
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.effectiveLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class GameEngineBarricadeLogicTest {
    private fun createOpenLevel(): Level {
        val allCells = (0 until 8).flatMap { x -> (0 until 8).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Barricade Test",
            gridWidth = 8,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(7, 3)),
            pathCells = allCells,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
        )
    }

    private fun createLogic(state: GameState): GameEngineBarricadeLogic {
        val mineOperations = MineOperations(state) { _, _ -> }
        return GameEngineBarricadeLogic(
            state = state,
            barricadeSystem = BarricadeSystem(state),
            mineOperations = mineOperations,
            applyTargetDamage = {},
            destroyFiefAt = { _, _ -> },
            consumeMushroomAt = { _, _ -> },
        )
    }

    @Test
    fun getBarricadeDamageForEnemyUnitAppliesDragonMultiplier() {
        val state = GameState(createOpenLevel())
        val logic = createLogic(state)
        val dragon =
            Attacker(
                id = 1,
                type = AttackerType.DRAGON,
                position = mutableStateOf(Position(2, 2)),
                level = mutableStateOf(3),
            )
        state.waaghFrenzyActive.value = true

        val damage = logic.getBarricadeDamageForEnemyUnit(dragon)

        assertEquals(dragon.effectiveLevel * 5 * dragon.type.barricadeDamageMultiplier, damage)
    }

    @Test
    fun getBarricadeDamageForEnemyUnitDoublesOrkDamageDuringFrenzy() {
        val state = GameState(createOpenLevel())
        val logic = createLogic(state)
        val ork =
            Attacker(
                id = 1,
                type = AttackerType.ORK,
                position = mutableStateOf(Position(2, 2)),
                level = mutableStateOf(2),
            )
        val baseDamage = logic.getBarricadeDamageForEnemyUnit(ork)
        state.waaghFrenzyActive.value = true

        val frenzyDamage = logic.getBarricadeDamageForEnemyUnit(ork)

        assertEquals(baseDamage * 2, frenzyDamage)
    }
}
