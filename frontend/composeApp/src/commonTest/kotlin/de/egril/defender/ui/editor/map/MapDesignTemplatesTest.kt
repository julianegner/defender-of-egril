package de.egril.defender.ui.editor.map

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorTargetInfo
import de.egril.defender.editor.MapTemplateDefinition
import de.egril.defender.editor.TileType
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.TargetType
import de.egril.defender.ui.editor.level.MapLaneShape
import de.egril.defender.ui.editor.level.TravelBand
import de.egril.defender.ui.editor.level.analyzeMapFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapDesignTemplatesTest {
    @Test
    fun straightApproachTemplateCreatesReadyLinearMap() {
        val map = createMapFromTemplate("straight", "Straight", 12, 7, "tester", straightTemplate())
        val flow = analyzeMapFlow(map)

        assertTrue(map.readyToUse)
        assertEquals(1, map.getSpawnPoints().size)
        assertEquals(1, map.getTargets().size)
        assertEquals(MapLaneShape.STRAIGHT, flow.laneShape)
    }

    @Test
    fun splitLanesTemplateCreatesBranchingFlow() {
        val map = createMapFromTemplate("split", "Split", 15, 9, "tester", splitTemplate())
        val flow = analyzeMapFlow(map)

        assertEquals(2, flow.spawnCount)
        assertEquals(MapLaneShape.BRANCHING, flow.laneShape)
        assertEquals(1, flow.targetCount)
    }

    @Test
    fun riverCrossingTemplateAddsWaterSpawn() {
        val map = createMapFromTemplate("river", "River", 14, 8, "tester", riverTemplate())
        val flow = analyzeMapFlow(map)

        assertTrue(map.getSpawnPoints().any { map.getSpawnPointType(it) == SpawnPointType.WATER })
        assertTrue(map.getRiverCells().isNotEmpty())
        assertEquals(TravelBand.LONG, flow.travelLength)
    }

    private fun straightTemplate(): MapTemplateDefinition =
        MapTemplateDefinition(
            id = "straight_template",
            name = "Straight template",
            templateMap =
                EditorMap(
                    id = "straight_template",
                    name = "Straight template",
                    width = 12,
                    height = 7,
                    tiles =
                        mutableMapOf(
                            "0,3" to TileType.SPAWN_POINT,
                            "1,3" to TileType.PATH,
                            "2,3" to TileType.PATH,
                            "3,3" to TileType.PATH,
                            "4,3" to TileType.PATH,
                            "5,3" to TileType.PATH,
                            "6,3" to TileType.PATH,
                            "7,3" to TileType.PATH,
                            "8,3" to TileType.PATH,
                            "9,3" to TileType.PATH,
                            "10,3" to TileType.PATH,
                            "11,3" to TileType.TARGET,
                        ),
                    targetInfoMap = mutableMapOf("11,3" to EditorTargetInfo(name = "Town Hall", type = TargetType.STANDARD)),
                    spawnPointInfoMap = mutableMapOf("0,3" to SpawnPointType.LAND),
                ),
        )

    private fun splitTemplate(): MapTemplateDefinition =
        MapTemplateDefinition(
            id = "split_template",
            name = "Split template",
            templateMap =
                EditorMap(
                    id = "split_template",
                    name = "Split template",
                    width = 15,
                    height = 9,
                    tiles =
                        mutableMapOf(
                            "0,3" to TileType.SPAWN_POINT,
                            "1,3" to TileType.PATH,
                            "2,3" to TileType.PATH,
                            "3,3" to TileType.PATH,
                            "4,3" to TileType.PATH,
                            "5,3" to TileType.PATH,
                            "6,3" to TileType.PATH,
                            "7,3" to TileType.PATH,
                            "8,3" to TileType.PATH,
                            "9,3" to TileType.PATH,
                            "10,3" to TileType.PATH,
                            "10,4" to TileType.PATH,
                            "11,4" to TileType.PATH,
                            "12,4" to TileType.PATH,
                            "13,4" to TileType.PATH,
                            "14,4" to TileType.TARGET,
                            "0,6" to TileType.SPAWN_POINT,
                            "1,6" to TileType.PATH,
                            "2,6" to TileType.PATH,
                            "3,6" to TileType.PATH,
                            "4,6" to TileType.PATH,
                            "5,6" to TileType.PATH,
                            "6,6" to TileType.PATH,
                            "7,6" to TileType.PATH,
                            "8,6" to TileType.PATH,
                            "9,6" to TileType.PATH,
                            "10,6" to TileType.PATH,
                            "10,5" to TileType.PATH,
                        ),
                    targetInfoMap = mutableMapOf("14,4" to EditorTargetInfo(name = "Gate", type = TargetType.STANDARD)),
                    spawnPointInfoMap =
                        mutableMapOf(
                            "0,3" to SpawnPointType.LAND,
                            "0,6" to SpawnPointType.LAND,
                        ),
                ),
        )

    private fun riverTemplate(): MapTemplateDefinition =
        MapTemplateDefinition(
            id = "river_template",
            name = "River template",
            templateMap =
                EditorMap(
                    id = "river_template",
                    name = "River template",
                    width = 14,
                    height = 8,
                    tiles =
                        mutableMapOf(
                            "0,4" to TileType.SPAWN_POINT,
                            "1,4" to TileType.PATH,
                            "2,4" to TileType.PATH,
                            "3,4" to TileType.PATH,
                            "4,4" to TileType.PATH,
                            "5,4" to TileType.PATH,
                            "6,4" to TileType.PATH,
                            "7,4" to TileType.PATH,
                            "8,4" to TileType.PATH,
                            "9,4" to TileType.PATH,
                            "10,4" to TileType.PATH,
                            "11,4" to TileType.PATH,
                            "12,4" to TileType.PATH,
                            "13,4" to TileType.TARGET,
                            "0,7" to TileType.SPAWN_POINT,
                            "1,6" to TileType.RIVER,
                            "2,5" to TileType.RIVER,
                            "3,4" to TileType.RIVER,
                            "4,3" to TileType.RIVER,
                        ),
                    targetInfoMap = mutableMapOf("13,4" to EditorTargetInfo(name = "Harbor", type = TargetType.STANDARD)),
                    spawnPointInfoMap =
                        mutableMapOf(
                            "0,4" to SpawnPointType.LAND,
                            "0,7" to SpawnPointType.WATER,
                        ),
                ),
        )
}
