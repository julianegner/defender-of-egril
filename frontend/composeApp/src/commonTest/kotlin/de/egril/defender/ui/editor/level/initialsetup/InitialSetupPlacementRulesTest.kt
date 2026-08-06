package de.egril.defender.ui.editor.level.initialsetup

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InitialSetupPlacementRulesTest {
    private fun mapWithFlowingWaterAt(waterPos: Position): EditorMap =
        EditorMap(
            id = "test-map",
            width = 4,
            height = 4,
            tiles = emptyMap(),
            riverTiles =
                mapOf(
                    "${waterPos.x},${waterPos.y}" to
                        RiverTile(
                            position = waterPos,
                            flowDirection = RiverFlow.EAST,
                            flowSpeed = 1,
                        ),
                ),
        )

    @Test
    fun defenderCanBePlacedOnFlowingWaterTile() {
        val waterPos = Position(1, 1)
        val map = mapWithFlowingWaterAt(waterPos)

        assertTrue(
            isValidPlacement(
                position = waterPos,
                mode = PlacementMode.DEFENDER,
                map = map,
                selectedDefenderType = DefenderType.SPIKE_TOWER,
            ),
        )
    }

    @Test
    fun dwarvenMineCannotBePlacedOnFlowingWaterTile() {
        val waterPos = Position(1, 1)
        val map = mapWithFlowingWaterAt(waterPos)

        assertFalse(
            isValidPlacement(
                position = waterPos,
                mode = PlacementMode.DEFENDER,
                map = map,
                selectedDefenderType = DefenderType.DWARVEN_MINE,
            ),
        )
    }

    @Test
    fun waterCapableAttackerCanBePlacedOnWaterTile() {
        val waterPos = Position(2, 2)
        val map = mapWithFlowingWaterAt(waterPos)

        assertTrue(
            isValidPlacement(
                position = waterPos,
                mode = PlacementMode.ATTACKER,
                map = map,
                selectedAttackerType = AttackerType.PIRATE,
            ),
        )
    }

    @Test
    fun nonWaterAttackerCannotBePlacedOnWaterTile() {
        val waterPos = Position(2, 2)
        val map = mapWithFlowingWaterAt(waterPos)

        assertFalse(
            isValidPlacement(
                position = waterPos,
                mode = PlacementMode.ATTACKER,
                map = map,
                selectedAttackerType = AttackerType.GOBLIN,
            ),
        )
    }

    @Test
    fun noPlayTileIsNotTreatedAsWater() {
        val map =
            EditorMap(
                id = "test-map",
                width = 4,
                height = 4,
                tiles = mapOf("0,0" to TileType.BUILD_AREA),
            )
        val noPlayPos = Position(3, 3)

        assertFalse(
            isValidPlacement(
                position = noPlayPos,
                mode = PlacementMode.DEFENDER,
                map = map,
                selectedDefenderType = DefenderType.SPIKE_TOWER,
            ),
        )
    }
}
