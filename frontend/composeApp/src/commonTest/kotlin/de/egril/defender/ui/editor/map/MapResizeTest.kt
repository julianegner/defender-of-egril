package de.egril.defender.ui.editor.map

import de.egril.defender.editor.EditorTargetInfo
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapResizeTest {
    @Test
    fun resizeShiftsCoordinatesWhenAddingOnLeftAndTop() {
        val resized =
            applyResizeToMapData(
                width = 3,
                height = 3,
                leftDelta = 1,
                rightDelta = 0,
                topDelta = 1,
                bottomDelta = 0,
                tiles = mapOf("0,0" to TileType.SPAWN_POINT, "2,2" to TileType.TARGET),
                riverTiles =
                    mapOf(
                        "1,1" to RiverTile(position = Position(1, 1), flowDirection = RiverFlow.EAST, flowSpeed = 1),
                    ),
                targetInfoMap = mapOf("2,2" to EditorTargetInfo(name = "Gate", type = TargetType.STANDARD)),
                spawnPointInfoMap = mapOf("0,0" to SpawnPointType.LAND),
            )

        assertEquals(4, resized.width)
        assertEquals(4, resized.height)
        assertTrue("1,1" in resized.tiles)
        assertTrue("3,3" in resized.tiles)
        assertTrue("2,2" in resized.riverTiles)
        assertTrue("1,1" in resized.spawnPointInfoMap)
        assertTrue("3,3" in resized.targetInfoMap)
    }

    @Test
    fun resizeDropsTrimmedCoordinatesWhenShrinkingFromLeft() {
        val resized =
            applyResizeToMapData(
                width = 3,
                height = 2,
                leftDelta = -1,
                rightDelta = 0,
                topDelta = 0,
                bottomDelta = 0,
                tiles = mapOf("0,0" to TileType.SPAWN_POINT, "1,0" to TileType.PATH, "2,0" to TileType.TARGET),
                riverTiles = emptyMap(),
                targetInfoMap = mapOf("2,0" to EditorTargetInfo(name = "Goal", type = TargetType.STANDARD)),
                spawnPointInfoMap = mapOf("0,0" to SpawnPointType.LAND),
            )

        assertEquals(2, resized.width)
        assertFalse("0,0" in resized.spawnPointInfoMap)
        assertTrue("1,0" in resized.targetInfoMap)
    }

    @Test
    fun safeEndExpansionOnlyAllowsBottomRightGrowth() {
        assertTrue(isSafeEndExpansion(leftDelta = 0, rightDelta = 2, topDelta = 0, bottomDelta = 1))
        assertFalse(isSafeEndExpansion(leftDelta = 1, rightDelta = 0, topDelta = 0, bottomDelta = 0))
        assertFalse(isSafeEndExpansion(leftDelta = 0, rightDelta = -1, topDelta = 0, bottomDelta = 0))
    }
}
