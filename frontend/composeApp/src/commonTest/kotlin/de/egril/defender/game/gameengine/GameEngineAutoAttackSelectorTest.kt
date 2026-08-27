package de.egril.defender.game.gameengine

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameEngineAutoAttackSelectorTest {
    private fun createOpenLevel(): Level {
        val allCells = (0 until 10).flatMap { x -> (0 until 8).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Auto Attack Selector Test",
            gridWidth = 10,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = allCells,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    private fun defender(
        id: Int,
        type: DefenderType,
        position: Position,
    ) = Defender(
        id = id,
        type = type,
        position = mutableStateOf(position),
        actionsRemaining = mutableStateOf(1),
        buildTimeRemaining = mutableStateOf(0),
    )

    private fun attacker(
        id: Int,
        type: AttackerType,
        position: Position,
    ) = Attacker(id, type, mutableStateOf(position), mutableStateOf(1))

    @Test
    fun selectAutoTargetForDefenderPrioritizesHigherThreat() {
        val state = GameState(createOpenLevel())
        val selector =
            GameEngineAutoAttackSelector(
                state = state,
                getEffectiveRange = { it.range },
                findClosestTargetPosition = { Position(9, 3) },
            )
        val tower = defender(1, DefenderType.BOW_TOWER, Position(3, 3))
        val closeGoblin = attacker(1, AttackerType.GOBLIN, Position(4, 3))
        val fartherRedWitch = attacker(2, AttackerType.RED_WITCH, Position(5, 3))

        val selected = selector.selectAutoTargetForDefender(tower, listOf(closeGoblin, fartherRedWitch))

        assertEquals(fartherRedWitch.id, selected?.id)
    }

    @Test
    fun selectBestAreaAttackPositionReturnsNullForOnlyImmuneTargets() {
        val state = GameState(createOpenLevel())
        val selector =
            GameEngineAutoAttackSelector(
                state = state,
                getEffectiveRange = { it.range },
                findClosestTargetPosition = { Position(9, 3) },
            )
        val wizard = defender(1, DefenderType.WIZARD_TOWER, Position(3, 3))
        val redDemon = attacker(1, AttackerType.RED_DEMON, Position(5, 3))

        val selected = selector.selectBestAreaAttackPosition(wizard, listOf(redDemon))

        assertNull(selected)
    }

    @Test
    fun selectAutoTargetForDefenderUsesDistanceTieBreakerWhenThreatIsEqual() {
        val state = GameState(createOpenLevel())
        val selector =
            GameEngineAutoAttackSelector(
                state = state,
                getEffectiveRange = { it.range },
                findClosestTargetPosition = { Position(9, 3) },
            )
        val tower = defender(1, DefenderType.BOW_TOWER, Position(3, 3))
        val fartherFromGoal = attacker(1, AttackerType.GOBLIN, Position(4, 3))
        val closerToGoal = attacker(2, AttackerType.GOBLIN, Position(5, 3))

        val selected = selector.selectAutoTargetForDefender(tower, listOf(fartherFromGoal, closerToGoal))

        assertEquals(closerToGoal.id, selected?.id)
    }

    @Test
    fun selectAutoTargetForDefenderUsesHealthTieBreakerWhenThreatAndDistanceAreEqual() {
        val state = GameState(createOpenLevel())
        val selector =
            GameEngineAutoAttackSelector(
                state = state,
                getEffectiveRange = { it.range },
                findClosestTargetPosition = { Position(9, 3) },
            )
        val tower = defender(1, DefenderType.BOW_TOWER, Position(3, 3))
        val healthyGoblin = attacker(1, AttackerType.GOBLIN, Position(4, 3))
        val woundedGoblin = attacker(2, AttackerType.GOBLIN, Position(4, 3))
        woundedGoblin.currentHealth.value = healthyGoblin.currentHealth.value - 1

        val selected = selector.selectAutoTargetForDefender(tower, listOf(healthyGoblin, woundedGoblin))

        assertEquals(woundedGoblin.id, selected?.id)
    }

    @Test
    fun selectBestAreaAttackPositionReturnsPositionWhenDamageableTargetExists() {
        val state = GameState(createOpenLevel())
        val selector =
            GameEngineAutoAttackSelector(
                state = state,
                getEffectiveRange = { it.range },
                findClosestTargetPosition = { Position(9, 3) },
            )
        val wizard = defender(1, DefenderType.WIZARD_TOWER, Position(3, 3))
        val goblin = attacker(1, AttackerType.GOBLIN, Position(5, 3))

        val selected = selector.selectBestAreaAttackPosition(wizard, listOf(goblin))

        assertNotNull(selected)
    }
}
