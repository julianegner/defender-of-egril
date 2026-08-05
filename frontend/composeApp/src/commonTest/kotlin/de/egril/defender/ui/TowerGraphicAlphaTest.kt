package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.ui.gameplay.GamePlayConstants
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [towerGraphicAlpha], which controls the opacity used to visually distinguish
 * active towers (fully opaque) from inactive ones (reduced opacity).
 */
class TowerGraphicAlphaTest {
    private fun tower(
        type: DefenderType,
        level: Int = 1,
    ): Defender = Defender(1, type, mutableStateOf(Position(0, 0)), mutableStateOf(level))

    @Test
    fun activeTowerIsFullyOpaque() {
        val defender = tower(DefenderType.SPIKE_TOWER)
        defender.buildTimeRemaining.value = 0
        defender.actionsRemaining.value = 1
        assertEquals(1f, towerGraphicAlpha(defender), "A ready tower with actions left should be fully opaque")
    }

    @Test
    fun towerWithoutActionsIsDimmed() {
        val defender = tower(DefenderType.SPIKE_TOWER)
        defender.buildTimeRemaining.value = 0
        defender.actionsRemaining.value = 0
        assertEquals(
            GamePlayConstants.Opacity.InactiveTower,
            towerGraphicAlpha(defender),
            "A ready tower that has used all its actions should be dimmed",
        )
    }

    @Test
    fun buildingTowerIsDimmed() {
        val defender = tower(DefenderType.WIZARD_TOWER)
        defender.buildTimeRemaining.value = 2
        defender.actionsRemaining.value = 0
        assertEquals(
            GamePlayConstants.Opacity.InactiveTower,
            towerGraphicAlpha(defender),
            "A tower that is still building should be dimmed",
        )
    }

    @Test
    fun disabledTowerIsDimmed() {
        val defender = tower(DefenderType.BOW_TOWER)
        defender.buildTimeRemaining.value = 0
        defender.isDisabled.value = true
        defender.actionsRemaining.value = 0
        assertEquals(
            GamePlayConstants.Opacity.InactiveTower,
            towerGraphicAlpha(defender),
            "A disabled tower should be dimmed",
        )
    }

    @Test
    fun dragonsLairIsAlwaysFullyOpaque() {
        // Dragon's Lair never takes actions (actionsPerTurn 0) so it must not be dimmed.
        val defender = tower(DefenderType.DRAGONS_LAIR)
        defender.buildTimeRemaining.value = 0
        defender.actionsRemaining.value = 0
        assertEquals(
            1f,
            towerGraphicAlpha(defender),
            "The Dragon's Lair never acts and should always be fully opaque",
        )
    }

    @Test
    fun dwarvenMineWithoutActionsIsDimmed() {
        val defender = tower(DefenderType.DWARVEN_MINE)
        defender.buildTimeRemaining.value = 0
        defender.actionsRemaining.value = 0
        assertEquals(
            GamePlayConstants.Opacity.InactiveTower,
            towerGraphicAlpha(defender),
            "A dwarven mine that has finished digging should be dimmed",
        )
    }
}
