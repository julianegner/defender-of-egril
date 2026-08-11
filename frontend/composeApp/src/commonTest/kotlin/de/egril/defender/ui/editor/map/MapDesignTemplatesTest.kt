package de.egril.defender.ui.editor.map

import de.egril.defender.editor.MapTemplateDefinition
import de.egril.defender.editor.MapTemplateLayoutKind
import de.egril.defender.model.SpawnPointType
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
            layoutKind = MapTemplateLayoutKind.STRAIGHT_APPROACH,
        )

    private fun splitTemplate(): MapTemplateDefinition =
        MapTemplateDefinition(
            id = "split_template",
            name = "Split template",
            layoutKind = MapTemplateLayoutKind.SPLIT_LANES,
        )

    private fun riverTemplate(): MapTemplateDefinition =
        MapTemplateDefinition(
            id = "river_template",
            name = "River template",
            layoutKind = MapTemplateLayoutKind.RIVER_CROSSING,
        )
}
