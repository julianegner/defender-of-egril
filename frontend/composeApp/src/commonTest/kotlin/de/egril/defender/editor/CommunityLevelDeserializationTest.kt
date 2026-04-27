package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.utils.JsonUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for deserialization and readiness-checking of community levels.
 *
 * Community levels come in two flavours:
 *   1. Levels that reference an **official** map  (map exists in the bundled repository)
 *   2. Levels that reference a **community** map  (map was downloaded alongside the level)
 *
 * The test covers the complete path from raw JSON → EditorMap / EditorLevel → isReadyToPlay.
 */
class CommunityLevelDeserializationTest {

    // -----------------------------------------------------------------------
    // Helpers that build minimal valid JSON
    // -----------------------------------------------------------------------

    /** Minimal valid map JSON with a spawn → path → target route. */
    private fun buildMapJson(
        id: String,
        name: String = "Test Map",
        isCommunity: Boolean = false,
        isOfficial: Boolean = false,
        author: String = ""
    ): String {
        val authorJson = if (author.isNotEmpty()) """,
  "author": "$author"""" else ""
        val data = """{
  "id": "$id",
  "name": "$name",
  "width": 5,
  "height": 1,
  "readyToUse": true,
  "isOfficial": $isOfficial$authorJson,
  "tiles": {
    "0,0": "SPAWN_POINT",
    "1,0": "PATH",
    "2,0": "PATH",
    "3,0": "PATH",
    "4,0": "TARGET"
  }
}"""
        return """{
  "metadata": {
    "program": "Defender of Egril",
    "type": "map"
  },
  "data": $data
}"""
    }

    /** Minimal valid level JSON referencing a given map ID. */
    private fun buildLevelJson(
        id: String,
        mapId: String,
        title: String = "Test Level",
        isCommunity: Boolean = false
    ): String {
        val data = """{
  "id": "$id",
  "mapId": "$mapId",
  "title": "$title",
  "subtitle": "",
  "startCoins": 100,
  "startHealthPoints": 10,
  "enemySpawns": [
    {"attackerType": "GOBLIN", "level": 1, "spawnTurn": 1, "spawnPoint": {"x": 0, "y": 0}}
  ],
  "availableTowers": ["SPIKE_TOWER"],
  "waypoints": [],
  "prerequisites": []
}"""
        return """{
  "metadata": {
    "program": "Defender of Egril",
    "type": "level"
  },
  "data": $data
}"""
    }

    // -----------------------------------------------------------------------
    // 1. Map deserialization — basic cases
    // -----------------------------------------------------------------------

    @Test
    fun testDeserializeOfficialMapJson() {
        val json = buildMapJson("official_map_1", isOfficial = true)
        val map = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(map, "Official map should deserialize successfully")
        assertEquals("official_map_1", map.id)
        assertEquals(5, map.tiles.size)
        assertTrue(map.tiles.containsKey("0,0"))
        assertEquals(TileType.SPAWN_POINT, map.tiles["0,0"])
        assertEquals(TileType.TARGET, map.tiles["4,0"])
    }

    @Test
    fun testDeserializeCommunityMapJson() {
        val json = buildMapJson("community_map_1", isCommunity = true, author = "test_user")
        val map = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(map, "Community map should deserialize successfully")
        assertEquals("community_map_1", map.id)
        assertEquals("test_user", map.author)
        assertEquals(5, map.tiles.size)
    }

    @Test
    fun testDeserializedMapPassesValidateReadyToUse() {
        val json = buildMapJson("valid_map")
        val map = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(map)
        // The map has SPAWN_POINT → PATH → TARGET — it must pass validation
        assertTrue(map.validateReadyToUse(), "Map with valid spawn->path->target should be ready to use")
    }

    @Test
    fun testDeserializedMapWithNoSpawnPointFailsValidation() {
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {
    "id": "no_spawn",
    "name": "No Spawn",
    "width": 5,
    "height": 1,
    "readyToUse": false,
    "isOfficial": false,
    "tiles": {
      "1,0": "PATH",
      "4,0": "TARGET"
    }
  }
}"""
        val map = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(map)
        assertFalse(map.validateReadyToUse(), "Map without spawn point should not be ready to use")
    }

    @Test
    fun testDeserializedMapWithNoTargetFailsValidation() {
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {
    "id": "no_target",
    "name": "No Target",
    "width": 5,
    "height": 1,
    "readyToUse": false,
    "isOfficial": false,
    "tiles": {
      "0,0": "SPAWN_POINT",
      "1,0": "PATH"
    }
  }
}"""
        val map = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(map)
        assertFalse(map.validateReadyToUse(), "Map without target should not be ready to use")
    }

    // -----------------------------------------------------------------------
    // 2. Level deserialization — basic cases
    // -----------------------------------------------------------------------

    @Test
    fun testDeserializeCommunityLevelWithOfficialMap() {
        val levelJson = buildLevelJson("community_level_1", "official_map_1")
        val level = EditorJsonSerializer.deserializeLevel(levelJson)
        assertNotNull(level, "Community level (official map) should deserialize")
        assertEquals("community_level_1", level.id)
        assertEquals("official_map_1", level.mapId)
        assertEquals(1, level.enemySpawns.size)
        assertEquals(AttackerType.GOBLIN, level.enemySpawns[0].attackerType)
        assertEquals(setOf(DefenderType.SPIKE_TOWER), level.availableTowers)
    }

    @Test
    fun testDeserializeCommunityLevelWithCommunityMap() {
        val levelJson = buildLevelJson("community_level_2", "community_map_1")
        val level = EditorJsonSerializer.deserializeLevel(levelJson)
        assertNotNull(level, "Community level (community map) should deserialize")
        assertEquals("community_level_2", level.id)
        assertEquals("community_map_1", level.mapId)
    }

    @Test
    fun testLevelIsReadyToPlayWithValidMap() {
        val mapJson = buildMapJson("ready_map")
        val map = EditorJsonSerializer.deserializeMap(mapJson)
        assertNotNull(map)

        val levelJson = buildLevelJson("ready_level", "ready_map")
        val level = EditorJsonSerializer.deserializeLevel(levelJson)
        assertNotNull(level)

        // Level itself passes its own check
        assertTrue(level.isReadyToPlay(), "Level with towers, enemies, coins, hp should pass isReadyToPlay()")

        // Map passes structural validation
        assertTrue(map.validateReadyToUse(), "Map should pass validateReadyToUse()")
    }

    // -----------------------------------------------------------------------
    // 3. Round-trip: serialize then deserialize
    // -----------------------------------------------------------------------

    @Test
    fun testMapRoundTripForOfficialMap() {
        val original = EditorMap(
            id = "round_trip_map",
            name = "Round Trip",
            width = 5,
            height = 1,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.PATH,
                "2,0" to TileType.PATH,
                "3,0" to TileType.PATH,
                "4,0" to TileType.TARGET
            ),
            readyToUse = true,
            isOfficial = true
        )

        val json = EditorJsonSerializer.serializeMap(original)
        val restored = EditorJsonSerializer.deserializeMap(json)

        assertNotNull(restored, "Round-tripped official map should not be null")
        assertEquals(original.id, restored.id)
        assertEquals(original.width, restored.width)
        assertEquals(original.height, restored.height)
        assertEquals(original.tiles.size, restored.tiles.size)
        assertEquals(TileType.SPAWN_POINT, restored.tiles["0,0"])
        assertEquals(TileType.TARGET, restored.tiles["4,0"])
    }

    @Test
    fun testMapRoundTripForCommunityMap() {
        val original = EditorMap(
            id = "community_round_trip_map",
            name = "Community Round Trip",
            width = 5,
            height = 1,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.PATH,
                "2,0" to TileType.PATH,
                "3,0" to TileType.PATH,
                "4,0" to TileType.TARGET
            ),
            readyToUse = true,
            isOfficial = false,
            isCommunity = true,
            author = "community_author",
            communityAuthorUsername = "community_author"
        )

        val json = EditorJsonSerializer.serializeMap(original)
        val restored = EditorJsonSerializer.deserializeMap(json)

        assertNotNull(restored, "Round-tripped community map should not be null")
        assertEquals(original.id, restored.id)
        assertEquals(original.author, restored.author)
        assertEquals(original.tiles.size, restored.tiles.size)
        assertTrue(restored.validateReadyToUse(), "Round-tripped community map should pass validation")
    }

    @Test
    fun testLevelRoundTripForCommunityLevel() {
        val original = EditorLevel(
            id = "community_level_rt",
            mapId = "community_map_rt",
            title = "Community Level RT",
            subtitle = "subtitle",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(0, 0))
            ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            isCommunity = true,
            communityAuthorUsername = "the_author"
        )

        val json = EditorJsonSerializer.serializeLevel(original)
        val restored = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(restored, "Round-tripped community level should not be null")
        assertEquals(original.id, restored.id)
        assertEquals(original.mapId, restored.mapId)
        assertEquals(original.title, restored.title)
        assertEquals(original.startCoins, restored.startCoins)
        assertEquals(1, restored.enemySpawns.size)
        assertEquals(AttackerType.GOBLIN, restored.enemySpawns[0].attackerType)
        assertEquals(original.availableTowers, restored.availableTowers)
    }

    // -----------------------------------------------------------------------
    // 4. Robustness: special characters in map name / author
    // -----------------------------------------------------------------------

    /**
     * Reproduces the bug where a `}` in the map name caused [JsonUtils.extractDataSection]
     * to prematurely close the data section — leaving the tiles block unparsed and returning
     * an EditorMap with empty tiles (which fails validateReadyToUse).
     */
    @Test
    fun testMapWithClosingBraceInNameDeserializesCorrectly() {
        val mapWithBraceName = EditorMap(
            id = "brace_map",
            name = "Map {with} braces}",  // contains '}' — previously broke the parser
            width = 5,
            height = 1,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.PATH,
                "2,0" to TileType.PATH,
                "3,0" to TileType.PATH,
                "4,0" to TileType.TARGET
            ),
            readyToUse = true
        )

        val json = EditorJsonSerializer.serializeMap(mapWithBraceName)
        val restored = EditorJsonSerializer.deserializeMap(json)

        assertNotNull(restored, "Map with '}' in name should deserialize (not return null)")
        assertEquals("brace_map", restored.id)
        assertEquals(5, restored.tiles.size, "All tiles must be present even when name contains '}'")
        assertEquals(TileType.SPAWN_POINT, restored.tiles["0,0"], "SPAWN_POINT tile must survive round-trip")
        assertEquals(TileType.TARGET, restored.tiles["4,0"], "TARGET tile must survive round-trip")
        assertTrue(restored.validateReadyToUse(), "Map with '}' in name must still pass validation")
    }

    @Test
    fun testMapWithClosingBraceInAuthorDeserializesCorrectly() {
        val mapWithBraceAuthor = EditorMap(
            id = "brace_author_map",
            name = "Normal Name",
            author = "Author}Name",  // contains '}' in author field
            width = 5,
            height = 1,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.PATH,
                "2,0" to TileType.PATH,
                "3,0" to TileType.PATH,
                "4,0" to TileType.TARGET
            ),
            readyToUse = true
        )

        val json = EditorJsonSerializer.serializeMap(mapWithBraceAuthor)
        val restored = EditorJsonSerializer.deserializeMap(json)

        assertNotNull(restored, "Map with '}' in author should deserialize")
        assertEquals(5, restored.tiles.size, "All tiles must survive even with '}' in author field")
        assertTrue(restored.validateReadyToUse(), "Map with '}' in author must still pass validation")
    }

    @Test
    fun testMapWithSpecialCharsInNameDeserializesCorrectly() {
        // Map name containing characters that could confuse naive parsers:
        // curly braces inside the name value
        val mapWithSpecialName = EditorMap(
            id = "special_chars_map",
            name = "Map {with} curly {braces}",
            width = 5,
            height = 1,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.PATH,
                "2,0" to TileType.PATH,
                "3,0" to TileType.PATH,
                "4,0" to TileType.TARGET
            ),
            readyToUse = true
        )

        val json = EditorJsonSerializer.serializeMap(mapWithSpecialName)
        val restored = EditorJsonSerializer.deserializeMap(json)

        assertNotNull(restored, "Map with special chars in name should deserialize")
        assertEquals(5, restored.tiles.size, "All tiles must survive even with special chars in name field")
        assertTrue(restored.validateReadyToUse(), "Map with special chars must still pass validation")
    }

    // -----------------------------------------------------------------------
    // 5. Robustness: unknown / future tile types
    // -----------------------------------------------------------------------

    /**
     * If the JSON contains a tile type not present in the current [TileType] enum
     * (e.g. from a newer version of the game), the whole map must still deserialize —
     * only the unknown tile is skipped.
     */
    @Test
    fun testMapWithUnknownTileTypeSkipsBadTileAndDeserializesRest() {
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {
    "id": "unknown_tile_map",
    "name": "Map With Unknown Tile",
    "width": 5,
    "height": 1,
    "readyToUse": true,
    "isOfficial": false,
    "tiles": {
      "0,0": "SPAWN_POINT",
      "1,0": "PATH",
      "2,0": "UNKNOWN_FUTURE_TILE_TYPE",
      "3,0": "PATH",
      "4,0": "TARGET"
    }
  }
}"""
        val map = EditorJsonSerializer.deserializeMap(json)

        assertNotNull(map, "Map with unknown tile type should NOT return null — only the bad tile is skipped")
        assertEquals("unknown_tile_map", map.id)
        // Known tiles must be present
        assertEquals(TileType.SPAWN_POINT, map.tiles["0,0"])
        assertEquals(TileType.TARGET, map.tiles["4,0"])
        // The unknown tile is absent (skipped), not null/crashed
        assertFalse(map.tiles.containsKey("2,0"), "Unknown tile should be silently skipped")
    }

    /** ISLAND is a legacy alias for BUILD_AREA — must be mapped correctly. */
    @Test
    fun testLegacyIslandTileIsMappedToBuildArea() {
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {
    "id": "island_tile_map",
    "name": "Map With ISLAND Tile",
    "width": 5,
    "height": 1,
    "readyToUse": true,
    "isOfficial": false,
    "tiles": {
      "0,0": "SPAWN_POINT",
      "1,0": "PATH",
      "2,0": "ISLAND",
      "3,0": "PATH",
      "4,0": "TARGET"
    }
  }
}"""
        val map = EditorJsonSerializer.deserializeMap(json)

        assertNotNull(map, "Map with legacy ISLAND tile should deserialize")
        assertEquals(TileType.BUILD_AREA, map.tiles["2,0"], "ISLAND must be converted to BUILD_AREA")
    }

    // -----------------------------------------------------------------------
    // 6. Metadata wrapper — old vs new format
    // -----------------------------------------------------------------------

    /** Old format has no metadata wrapper — must still deserialize for backwards compatibility. */
    @Test
    fun testOldFormatMapWithoutMetadataWrapperDeserializes() {
        val json = """{
  "id": "old_format_map",
  "name": "Old Format",
  "width": 5,
  "height": 1,
  "readyToUse": true,
  "isOfficial": false,
  "tiles": {
    "0,0": "SPAWN_POINT",
    "1,0": "PATH",
    "4,0": "TARGET"
  }
}"""
        val map = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(map, "Old format map (no metadata wrapper) should deserialize")
        assertEquals("old_format_map", map.id)
        assertEquals(3, map.tiles.size)
    }

    @Test
    fun testOldFormatLevelWithoutMetadataWrapperDeserializes() {
        val json = """{
  "id": "old_format_level",
  "mapId": "some_map",
  "title": "Old Level",
  "subtitle": "",
  "startCoins": 100,
  "startHealthPoints": 10,
  "enemySpawns": [
    {"attackerType": "GOBLIN", "level": 1, "spawnTurn": 1}
  ],
  "availableTowers": ["SPIKE_TOWER"],
  "waypoints": [],
  "prerequisites": []
}"""
        val level = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(level, "Old format level (no metadata wrapper) should deserialize")
        assertEquals("old_format_level", level.id)
        assertEquals("some_map", level.mapId)
    }

    // -----------------------------------------------------------------------
    // 7. isReadyToPlay checks
    // -----------------------------------------------------------------------

    @Test
    fun testLevelIsReadyToPlayChecks() {
        // Missing towers → not ready
        val noTowers = EditorLevel(
            id = "l1", mapId = "m1", title = "T", startCoins = 100, startHealthPoints = 10,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = emptySet()
        )
        assertFalse(noTowers.isReadyToPlay(), "Level with no towers must not be ready")

        // Missing enemies → not ready
        val noEnemies = EditorLevel(
            id = "l2", mapId = "m1", title = "T", startCoins = 100, startHealthPoints = 10,
            enemySpawns = emptyList(),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        assertFalse(noEnemies.isReadyToPlay(), "Level with no enemies must not be ready")

        // Zero coins → not ready
        val noCoins = EditorLevel(
            id = "l3", mapId = "m1", title = "T", startCoins = 0, startHealthPoints = 10,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        assertFalse(noCoins.isReadyToPlay(), "Level with 0 coins must not be ready")

        // Zero health points → not ready
        val noHp = EditorLevel(
            id = "l4", mapId = "m1", title = "T", startCoins = 100, startHealthPoints = 0,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        assertFalse(noHp.isReadyToPlay(), "Level with 0 HP must not be ready")

        // All fields OK → ready
        val ready = EditorLevel(
            id = "l5", mapId = "m1", title = "T", startCoins = 100, startHealthPoints = 10,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        assertTrue(ready.isReadyToPlay(), "Level with all valid fields must be ready")
    }

    // -----------------------------------------------------------------------
    // 8. JsonUtils.extractDataSection — string-awareness
    // -----------------------------------------------------------------------

    @Test
    fun testExtractDataSectionIgnoresBracesInsideStrings() {
        // Simulates a map JSON where the "name" value contains `}` characters.
        // extractDataSection must not terminate the data block at the first `}`
        // inside the string, but must find the true closing brace.
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {
    "name": "Map }name} with braces",
    "id": "x",
    "other": "value"
  }
}"""
        val section = JsonUtils.extractDataSection(json)
        // The section must contain the "other" key which comes AFTER the braces in "name"
        assertTrue(section.contains("\"other\""), "extractDataSection must not stop at '}' inside a string value")
        assertTrue(section.contains("\"id\""), "The 'id' key must be inside the extracted section")
    }

    @Test
    fun testExtractDataSectionHandlesEscapedQuoteInsideString() {
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {
    "name": "Map with \"quoted\" word",
    "id": "escaped_quote_map",
    "tiles": {}
  }
}"""
        val section = JsonUtils.extractDataSection(json)
        assertTrue(section.contains("\"id\""), "The 'id' key must survive escaped quotes in strings")
        assertTrue(section.contains("escaped_quote_map"), "The id value must be present")
    }

    @Test
    fun testExtractDataSectionWithNoMetadataReturnsOriginalJson() {
        val json = """{"id": "plain", "name": "plain map"}"""
        val section = JsonUtils.extractDataSection(json)
        // Without metadata wrapper, must return the input unchanged
        assertEquals(json, section, "Without metadata wrapper the original JSON must be returned as-is")
    }
}
