package de.egril.defender.editor

import de.egril.defender.model.Position
import de.egril.defender.model.TileZone
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A movable zone flagged rotatable must have at least seven tiles, otherwise the map is not ready. */
class RotatableZoneValidationTest {
    private fun mapWithZone(zone: TileZone) =
        EditorMap(
            id = "rotatable_map",
            width = 4,
            height = 1,
            tiles = mapOf("0,0" to TileType.SPAWN_POINT, "1,0" to TileType.PATH, "2,0" to TileType.TARGET, "3,0" to TileType.BUILD_AREA),
            tileZones = listOf(zone),
        )

    private fun zoneWithTiles(count: Int, rotatable: Boolean, movable: Boolean = true) =
        TileZone(
            id = "zone",
            tiles = (0 until count).associate { Position(it, 3) to TileType.PATH },
            movable = movable,
            rotatable = rotatable,
        )

    @Test
    fun smallRotatableZoneMakesMapInvalid() {
        val map = mapWithZone(zoneWithTiles(count = 2, rotatable = true))

        assertTrue(map.getValidationIssues().contains(MapValidationIssue.ROTATABLE_ZONE_TOO_SMALL))
        assertFalse(map.validateReadyToUse())
    }

    @Test
    fun rotatableZoneWithSevenTilesIsValid() {
        val map = mapWithZone(zoneWithTiles(count = 7, rotatable = true))

        assertFalse(map.getValidationIssues().contains(MapValidationIssue.ROTATABLE_ZONE_TOO_SMALL))
        assertTrue(map.validateReadyToUse())
    }

    @Test
    fun smallZoneWithoutRotatableFlagStaysValid() {
        val map = mapWithZone(zoneWithTiles(count = 2, rotatable = false))

        assertTrue(map.validateReadyToUse())
    }

    @Test
    fun rotatableFlagOnFixedZoneIsIgnored() {
        val map = mapWithZone(zoneWithTiles(count = 2, rotatable = true, movable = false))

        assertTrue(map.validateReadyToUse())
    }
}
