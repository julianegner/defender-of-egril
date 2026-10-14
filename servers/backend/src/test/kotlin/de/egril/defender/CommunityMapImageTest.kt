package de.egril.defender

import de.egril.defender.mapgen.core.MapImageTileKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommunityMapImageTest {
    private val mapJson =
        """
        {
          "id": "unit-map",
          "width": 4,
          "height": 3,
          "tiles": {"0,0": "PATH", "1,0": "BUILD_AREA", "2,0": "SPAWN_POINT", "3,0": "BOGUS"},
          "spawnPointInfo": {"2,0": "WATER", "0,1": "LAND"},
          "tileZones": [
            {"id": "z0", "name": "First", "zoneTiles": {"1,1": {"type": "NO_PLAY"}, "2,1": {"type": "TARGET", "x": 1}}},
            {"id": "z1", "name": "Second", "zoneTiles": {}}
          ]
        }
        """.trimIndent()

    @Test
    fun `parser extracts base map input including water spawn points`() {
        val source = assertNotNull(parseCommunityMapImageSource(mapJson))

        assertEquals("unit-map", source.base.seed)
        assertEquals(4, source.base.width)
        assertEquals(3, source.base.height)
        assertEquals(MapImageTileKind.PATH, source.base.tiles[0 to 0])
        assertEquals(MapImageTileKind.BUILD_AREA, source.base.tiles[1 to 0])
        assertEquals(MapImageTileKind.NO_PLAY, source.base.tiles[3 to 0])
        assertEquals(setOf(2 to 0), source.base.waterSpawnPoints)
    }

    @Test
    fun `parser extracts one tile map per tile zone in order`() {
        val source = assertNotNull(parseCommunityMapImageSource(mapJson))

        assertEquals(2, source.zoneTiles.size)
        assertEquals(
            mapOf(1 to 1 to MapImageTileKind.NO_PLAY, 2 to 1 to MapImageTileKind.TARGET),
            source.zoneTiles[0],
        )
        assertTrue(source.zoneTiles[1].isEmpty())
    }

    @Test
    fun `parser reads the movable flag of each tile zone`() {
        val json =
            """
            {"id": "m", "width": 2, "height": 2, "tiles": {},
             "tileZones": [
               {"id": "a", "zoneTiles": {}, "movable": true},
               {"id": "b", "zoneTiles": {}}
             ]}
            """.trimIndent()
        val source = assertNotNull(parseCommunityMapImageSource(json))

        assertEquals(listOf(true, false), source.zoneMovable)
    }

    @Test
    fun `parser returns null for invalid or dimensionless JSON`() {
        assertNull(parseCommunityMapImageSource("not json"))
        assertNull(parseCommunityMapImageSource("""{"id":"x","tiles":{}}"""))
    }

    @Test
    fun `render produces one PNG for the map and one per zone`() {
        val source = assertNotNull(parseCommunityMapImageSource(mapJson))

        val images = renderCommunityMapImages(source)

        assertEquals(2, images.zones.size)
        assertTrue(images.map.size > 100)
        assertEquals(0x89.toByte(), images.map[0])
        images.zones.forEach { assertEquals(0x89.toByte(), it[0]) }
    }
}
