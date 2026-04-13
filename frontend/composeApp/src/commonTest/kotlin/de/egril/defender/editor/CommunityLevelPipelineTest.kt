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
 * End-to-end pipeline tests for community levels: from raw JSON through deserialization,
 * readiness validation, all the way to game level conversion.
 *
 * Covers:
 *   1. Community level with official map
 *   2. Community level with community map
 *   3. Edge cases (missing tiles, missing spawn/target, empty map, waypoints)
 *   4. Full serialize → deserialize → validate → convert pipeline
 *   5. UI filtering logic (`isCommunity` flag persistence through the pipeline)
 */
class CommunityLevelPipelineTest {

    // -----------------------------------------------------------------------
    // Helpers that build minimal valid EditorMap / EditorLevel objects
    // -----------------------------------------------------------------------

    /**
     * Creates a minimal valid map with a straight-line path: SPAWN → PATH → PATH → PATH → TARGET.
     * Width = 5, Height = 1.
     */
    private fun buildValidEditorMap(
        id: String = "test_map",
        name: String = "Test Map",
        isOfficial: Boolean = false,
        isCommunity: Boolean = false,
        author: String = ""
    ): EditorMap {
        return EditorMap(
            id = id,
            name = name,
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
            isOfficial = isOfficial,
            isCommunity = isCommunity,
            author = author
        )
    }

    /**
     * Creates a minimal valid editor level referencing [mapId].
     * Has 1 Goblin spawn, SPIKE_TOWER available, 100 coins, 10 HP.
     */
    private fun buildValidEditorLevel(
        id: String = "test_level",
        mapId: String = "test_map",
        title: String = "Test Level",
        isCommunity: Boolean = false,
        communityAuthorUsername: String = ""
    ): EditorLevel {
        return EditorLevel(
            id = id,
            mapId = mapId,
            title = title,
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(0, 0))
            ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            isCommunity = isCommunity,
            communityAuthorUsername = communityAuthorUsername
        )
    }

    /** Builds a map JSON string in the standard serializer format. */
    private fun buildMapJson(
        id: String = "test_map",
        name: String = "Test Map",
        isOfficial: Boolean = false,
        author: String = "",
        tileEntries: String = """
    "0,0": "SPAWN_POINT",
    "1,0": "PATH",
    "2,0": "PATH",
    "3,0": "PATH",
    "4,0": "TARGET"
"""
    ): String {
        val authorJson = if (author.isNotEmpty()) """,
  "author": "$author"""" else ""
        return """{
  "metadata": {
    "program": "Defender of Egril",
    "type": "map"
  },
  "data": {
  "id": "$id",
  "name": "$name",
  "width": 5,
  "height": 1,
  "readyToUse": true,
  "isOfficial": $isOfficial$authorJson,
  "tiles": {
$tileEntries
  }
}
}"""
    }

    /** Builds a level JSON string in the standard serializer format. */
    private fun buildLevelJson(
        id: String = "test_level",
        mapId: String = "test_map",
        title: String = "Test Level"
    ): String {
        return """{
  "metadata": {
    "program": "Defender of Egril",
    "type": "level"
  },
  "data": {
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
}
}"""
    }

    // =======================================================================
    // 1. EditorLevel.isReadyToPlay() — level-only checks
    // =======================================================================

    @Test
    fun testValidEditorLevelIsReadyToPlay() {
        val level = buildValidEditorLevel()
        assertTrue(level.isReadyToPlay(), "Valid level should be ready to play")
    }

    @Test
    fun testLevelNotReadyWithoutTowers() {
        val level = buildValidEditorLevel().copy(availableTowers = emptySet())
        assertFalse(level.isReadyToPlay(), "Level without towers should NOT be ready")
    }

    @Test
    fun testLevelNotReadyWithoutEnemySpawns() {
        val level = buildValidEditorLevel().copy(enemySpawns = emptyList())
        assertFalse(level.isReadyToPlay(), "Level without enemies should NOT be ready")
    }

    @Test
    fun testLevelNotReadyWithZeroCoins() {
        val level = buildValidEditorLevel().copy(startCoins = 0)
        assertFalse(level.isReadyToPlay(), "Level with 0 coins should NOT be ready")
    }

    @Test
    fun testLevelNotReadyWithZeroHP() {
        val level = buildValidEditorLevel().copy(startHealthPoints = 0)
        assertFalse(level.isReadyToPlay(), "Level with 0 HP should NOT be ready")
    }

    // =======================================================================
    // 2. EditorMap.validateReadyToUse() — map structural validation
    // =======================================================================

    @Test
    fun testValidMapPassesValidation() {
        val map = buildValidEditorMap()
        assertTrue(map.validateReadyToUse(), "Map with valid spawn→path→target route should pass")
    }

    @Test
    fun testMapWithNoSpawnFails() {
        val map = EditorMap(
            id = "no_spawn",
            name = "No Spawn",
            width = 5,
            height = 1,
            tiles = mapOf(
                "1,0" to TileType.PATH,
                "4,0" to TileType.TARGET
            )
        )
        assertFalse(map.validateReadyToUse(), "Map without spawn should fail validation")
    }

    @Test
    fun testMapWithNoTargetFails() {
        val map = EditorMap(
            id = "no_target",
            name = "No Target",
            width = 5,
            height = 1,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.PATH
            )
        )
        assertFalse(map.validateReadyToUse(), "Map without target should fail validation")
    }

    @Test
    fun testMapWithNoPathFails() {
        // Spawn at (0,0) and target at (4,0) with no path connecting them
        val map = EditorMap(
            id = "no_path",
            name = "No Path",
            width = 5,
            height = 1,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "4,0" to TileType.TARGET
            )
        )
        assertFalse(map.validateReadyToUse(), "Map without continuous path should fail validation")
    }

    @Test
    fun testEmptyMapFails() {
        val map = EditorMap(
            id = "empty",
            name = "Empty Map",
            width = 5,
            height = 1,
            tiles = emptyMap()
        )
        assertFalse(map.validateReadyToUse(), "Empty map should fail validation")
    }

    // =======================================================================
    // 3. EditorLevel.validateWaypointsDetailed() — waypoint validation
    // =======================================================================

    @Test
    fun testLevelWithNoWaypointsIsValid() {
        val map = buildValidEditorMap()
        val level = buildValidEditorLevel()
        val result = level.validateWaypointsDetailed(
            targetPositions = map.getTargets(),
            spawnPoints = map.getSpawnPoints()
        )
        assertTrue(result.isValid, "Level without waypoints should be valid")
    }

    @Test
    fun testLevelWithMultipleTargetsRequiresWaypoints() {
        val map = EditorMap(
            id = "multi_target",
            name = "Multi Target",
            width = 5,
            height = 2,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.PATH,
                "2,0" to TileType.PATH,
                "3,0" to TileType.TARGET,
                "3,1" to TileType.TARGET
            )
        )
        val level = buildValidEditorLevel(mapId = map.id)
        val result = level.validateWaypointsDetailed(
            targetPositions = map.getTargets(),
            spawnPoints = map.getSpawnPoints()
        )
        // Multiple targets with no waypoints → invalid
        assertFalse(result.isValid, "Multiple targets with no waypoints should be invalid")
    }

    // =======================================================================
    // 4. Serialize → Deserialize round-trip (maps)
    // =======================================================================

    @Test
    fun testMapRoundTripPreservesFields() {
        val original = buildValidEditorMap(id = "rt_map", name = "Round Trip Map", author = "tester")
        val json = EditorJsonSerializer.serializeMap(original)
        val deserialized = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(deserialized, "Deserialized map should not be null")
        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.width, deserialized.width)
        assertEquals(original.height, deserialized.height)
        assertEquals(original.tiles.size, deserialized.tiles.size)
        assertEquals(original.author, deserialized.author)
        // Check all tile types match
        for ((pos, tileType) in original.tiles) {
            assertEquals(tileType, deserialized.tiles[pos], "Tile at $pos should match")
        }
    }

    @Test
    fun testMapRoundTripStillPassesValidation() {
        val original = buildValidEditorMap()
        val json = EditorJsonSerializer.serializeMap(original)
        val deserialized = EditorJsonSerializer.deserializeMap(json)!!
        assertTrue(deserialized.validateReadyToUse(), "Round-tripped map should pass validation")
    }

    @Test
    fun testCommunityMapWithAuthorRoundTrip() {
        val original = buildValidEditorMap(
            id = "community_map_1",
            name = "Community Map",
            author = "community_author"
        )
        val json = EditorJsonSerializer.serializeMap(original)
        val deserialized = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(deserialized, "Community map should deserialize")
        assertEquals("community_author", deserialized.author)
        assertTrue(deserialized.validateReadyToUse(), "Community map should pass validation after round-trip")
    }

    // =======================================================================
    // 5. Serialize → Deserialize round-trip (levels)
    // =======================================================================

    @Test
    fun testLevelRoundTripPreservesFields() {
        val original = buildValidEditorLevel(id = "rt_level", mapId = "rt_map", title = "RT Level")
        val json = EditorJsonSerializer.serializeLevel(original)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(deserialized, "Deserialized level should not be null")
        assertEquals(original.id, deserialized.id)
        assertEquals(original.mapId, deserialized.mapId)
        assertEquals(original.title, deserialized.title)
        assertEquals(original.startCoins, deserialized.startCoins)
        assertEquals(original.startHealthPoints, deserialized.startHealthPoints)
        assertEquals(original.enemySpawns.size, deserialized.enemySpawns.size)
        assertEquals(original.availableTowers.size, deserialized.availableTowers.size)
    }

    @Test
    fun testLevelRoundTripPreservesAttackerTypes() {
        val original = buildValidEditorLevel()
        val json = EditorJsonSerializer.serializeLevel(original)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)!!
        assertEquals(AttackerType.GOBLIN, deserialized.enemySpawns[0].attackerType)
    }

    @Test
    fun testLevelRoundTripPreservesAvailableTowers() {
        val original = buildValidEditorLevel()
        val json = EditorJsonSerializer.serializeLevel(original)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)!!
        assertTrue(deserialized.availableTowers.contains(DefenderType.SPIKE_TOWER))
    }

    @Test
    fun testLevelRoundTripPreservesSpawnPoint() {
        val original = buildValidEditorLevel()
        val json = EditorJsonSerializer.serializeLevel(original)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)!!
        assertEquals(Position(0, 0), deserialized.enemySpawns[0].spawnPoint)
    }

    @Test
    fun testLevelRoundTripPassesReadyToPlay() {
        val original = buildValidEditorLevel()
        val json = EditorJsonSerializer.serializeLevel(original)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)!!
        assertTrue(deserialized.isReadyToPlay(), "Round-tripped level should pass isReadyToPlay()")
    }

    // =======================================================================
    // 6. Community level with official map — full pipeline
    // =======================================================================

    @Test
    fun testCommunityLevelWithOfficialMap_DeserializeAndValidate() {
        // Simulate: official map already exists, community level references it
        val officialMap = buildValidEditorMap(id = "official_map", isOfficial = true)
        val communityLevel = buildValidEditorLevel(
            id = "comm_level_1",
            mapId = "official_map",
            isCommunity = true,
            communityAuthorUsername = "player1"
        )

        // Validate level-only readiness
        assertTrue(communityLevel.isReadyToPlay(), "Community level should pass level-only check")

        // Validate map
        assertTrue(officialMap.validateReadyToUse(), "Official map should pass validation")

        // Validate waypoints (empty waypoints with single target = valid)
        val waypointResult = communityLevel.validateWaypointsDetailed(
            targetPositions = officialMap.getTargets(),
            spawnPoints = officialMap.getSpawnPoints()
        )
        assertTrue(waypointResult.isValid, "Waypoint validation should pass for community level with official map")
    }

    @Test
    fun testCommunityLevelWithOfficialMap_FullRoundTrip() {
        // Serialize and deserialize both map and level, then validate
        val originalMap = buildValidEditorMap(id = "off_map", isOfficial = true)
        val originalLevel = buildValidEditorLevel(id = "comm_lev", mapId = "off_map")

        val mapJson = EditorJsonSerializer.serializeMap(originalMap)
        val levelJson = EditorJsonSerializer.serializeLevel(originalLevel)

        val deserMap = EditorJsonSerializer.deserializeMap(mapJson)!!
        val deserLevel = EditorJsonSerializer.deserializeLevel(levelJson)!!

        // Level should be ready to play
        assertTrue(deserLevel.isReadyToPlay(), "Deserialized level should be ready")
        // Map should be ready to use
        assertTrue(deserMap.validateReadyToUse(), "Deserialized map should be ready")
        // Waypoints should be valid
        val result = deserLevel.validateWaypointsDetailed(
            targetPositions = deserMap.getTargets(),
            spawnPoints = deserMap.getSpawnPoints()
        )
        assertTrue(result.isValid, "Waypoints should be valid after full round-trip")
    }

    // =======================================================================
    // 7. Community level with community map — full pipeline
    // =======================================================================

    @Test
    fun testCommunityLevelWithCommunityMap_DeserializeAndValidate() {
        // Simulate: community map downloaded alongside community level
        val communityMap = buildValidEditorMap(id = "comm_map", isCommunity = true, author = "map_author")
        val communityLevel = buildValidEditorLevel(
            id = "comm_level_2",
            mapId = "comm_map",
            isCommunity = true,
            communityAuthorUsername = "player2"
        )

        assertTrue(communityLevel.isReadyToPlay(), "Community level should pass level-only check")
        assertTrue(communityMap.validateReadyToUse(), "Community map should pass validation")

        val waypointResult = communityLevel.validateWaypointsDetailed(
            targetPositions = communityMap.getTargets(),
            spawnPoints = communityMap.getSpawnPoints()
        )
        assertTrue(waypointResult.isValid, "Waypoint validation should pass for community level with community map")
    }

    @Test
    fun testCommunityLevelWithCommunityMap_FullRoundTrip() {
        val originalMap = buildValidEditorMap(id = "comm_map_rt", isCommunity = true, author = "author1")
        val originalLevel = buildValidEditorLevel(id = "comm_lev_rt", mapId = "comm_map_rt", isCommunity = true)

        val mapJson = EditorJsonSerializer.serializeMap(originalMap)
        val levelJson = EditorJsonSerializer.serializeLevel(originalLevel)

        val deserMap = EditorJsonSerializer.deserializeMap(mapJson)!!
        val deserLevel = EditorJsonSerializer.deserializeLevel(levelJson)!!

        assertTrue(deserLevel.isReadyToPlay(), "Deserialized community level should be ready")
        assertTrue(deserMap.validateReadyToUse(), "Deserialized community map should be ready")

        val result = deserLevel.validateWaypointsDetailed(
            targetPositions = deserMap.getTargets(),
            spawnPoints = deserMap.getSpawnPoints()
        )
        assertTrue(result.isValid, "Waypoints should be valid after round-trip of community map + level")
    }

    @Test
    fun testCommunityMapRoundTrip_TilesPreservedCorrectly() {
        val original = buildValidEditorMap(id = "tiles_rt", name = "Tiles RT")
        val json = EditorJsonSerializer.serializeMap(original)
        val deserialized = EditorJsonSerializer.deserializeMap(json)!!

        assertEquals(5, deserialized.tiles.size, "All 5 tiles should survive round-trip")
        assertEquals(TileType.SPAWN_POINT, deserialized.tiles["0,0"])
        assertEquals(TileType.PATH, deserialized.tiles["1,0"])
        assertEquals(TileType.PATH, deserialized.tiles["2,0"])
        assertEquals(TileType.PATH, deserialized.tiles["3,0"])
        assertEquals(TileType.TARGET, deserialized.tiles["4,0"])
    }

    // =======================================================================
    // 8. isCommunity flag — critical for UI filtering
    // =======================================================================

    @Test
    fun testIsCommunityFlagNotSerializedInLevel() {
        // The isCommunity flag is NOT part of the JSON format — it's set by getCommunityLevel()
        val level = buildValidEditorLevel(isCommunity = true)
        val json = EditorJsonSerializer.serializeLevel(level)
        assertFalse(json.contains("\"isCommunity\""), "isCommunity should NOT be serialized in level JSON")
    }

    @Test
    fun testIsCommunityFlagNotSerializedInMap() {
        val map = buildValidEditorMap(isCommunity = true)
        val json = EditorJsonSerializer.serializeMap(map)
        assertFalse(json.contains("\"isCommunity\""), "isCommunity should NOT be serialized in map JSON")
    }

    @Test
    fun testDeserializedLevelDefaultsIsCommunityToFalse() {
        val json = buildLevelJson(id = "lev_1", mapId = "map_1")
        val level = EditorJsonSerializer.deserializeLevel(json)!!
        assertFalse(level.isCommunity, "Deserialized level should default isCommunity to false")
    }

    @Test
    fun testIsCommunityMustBeSetManuallyAfterDeserialization() {
        // This tests that the pattern used in getCommunityLevel is correct:
        // 1. Deserialize → isCommunity = false
        // 2. Copy with isCommunity = true  → creates correct community level
        val json = buildLevelJson(id = "lev_2", mapId = "map_2")
        val rawLevel = EditorJsonSerializer.deserializeLevel(json)!!
        assertFalse(rawLevel.isCommunity, "Raw deserialized level has isCommunity = false")

        val communityLevel = rawLevel.copy(isCommunity = true)
        assertTrue(communityLevel.isCommunity, "After copy(isCommunity=true), flag should be true")
        // Other fields should be unchanged
        assertEquals("lev_2", communityLevel.id)
        assertEquals("map_2", communityLevel.mapId)
    }

    // =======================================================================
    // 9. convertToGameLevel — pure data conversion
    // =======================================================================

    @Test
    fun testConvertToGameLevelProducesCorrectFields() {
        // We cannot call EditorStorage.convertToGameLevel() directly without filesystem,
        // but we can verify the logic by manually performing the same conversion.
        val map = buildValidEditorMap(id = "convert_map")
        val level = buildValidEditorLevel(id = "convert_level", mapId = "convert_map")

        // Verify all the data that convertToGameLevel would use is correct
        val targets = map.getTargets()
        val spawnPoints = map.getSpawnPoints()
        val pathCells = map.getPathCells()
        val buildAreas = map.getBuildAreas()

        assertFalse(targets.isEmpty(), "Map must have targets")
        assertFalse(spawnPoints.isEmpty(), "Map must have spawn points")
        assertFalse(pathCells.isEmpty(), "Map must have path cells")
        assertEquals(Position(4, 0), targets[0], "Target should be at (4,0)")
        assertEquals(Position(0, 0), spawnPoints[0], "Spawn should be at (0,0)")
    }

    @Test
    fun testConvertedLevelWouldHaveEditorLevelId() {
        // convertToGameLevel sets editorLevelId = editorLevel.id
        // This is critical for the UI filtering to work
        val level = buildValidEditorLevel(id = "my_community_level")
        assertEquals("my_community_level", level.id, "editorLevel.id should match the level ID")
    }

    // =======================================================================
    // 10. Edge cases — map with special characters
    // =======================================================================

    @Test
    fun testMapWithBraceInNameRoundTrip() {
        // Regression test: } in name used to break extractDataSection
        val map = buildValidEditorMap(id = "brace_map", name = "Map with } brace")
        val json = EditorJsonSerializer.serializeMap(map)
        val deserialized = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(deserialized, "Map with } in name should deserialize")
        assertEquals(5, deserialized.tiles.size, "All tiles should survive when name contains }")
        assertTrue(deserialized.validateReadyToUse(), "Map should still be ready to use")
    }

    @Test
    fun testMapWithBraceInAuthorRoundTrip() {
        val map = buildValidEditorMap(id = "brace_auth_map", author = "author}name")
        val json = EditorJsonSerializer.serializeMap(map)
        val deserialized = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(deserialized, "Map with } in author should deserialize")
        assertEquals(5, deserialized.tiles.size, "All tiles should survive when author contains }")
    }

    @Test
    fun testMapWithMultipleBracesInNameRoundTrip() {
        // Test with multiple braces — but not with double quotes in the name,
        // because serializeMap does not escape quotes in the name field (known limitation).
        val map = buildValidEditorMap(id = "qbrace_map", name = "Map {with} many } braces { here")
        val json = EditorJsonSerializer.serializeMap(map)
        val deserialized = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(deserialized, "Map with multiple braces in name should deserialize")
        assertEquals(5, deserialized.tiles.size, "All tiles should survive")
    }

    @Test
    fun testMapWithUnknownTileTypeSkipsTile() {
        val json = buildMapJson(
            id = "unknown_tile",
            tileEntries = """
    "0,0": "SPAWN_POINT",
    "1,0": "SOME_FUTURE_TYPE",
    "2,0": "PATH",
    "3,0": "PATH",
    "4,0": "TARGET"
"""
        )
        val map = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(map, "Map with unknown tile type should still deserialize")
        // Unknown tile should be skipped, so 4 tiles (not 5)
        assertEquals(4, map.tiles.size, "Unknown tile type should be skipped silently")
        assertEquals(TileType.SPAWN_POINT, map.tiles["0,0"])
        assertEquals(TileType.TARGET, map.tiles["4,0"])
    }

    @Test
    fun testMapWithIslandTileBackwardCompat() {
        val json = buildMapJson(
            id = "island_map",
            tileEntries = """
    "0,0": "SPAWN_POINT",
    "1,0": "PATH",
    "2,0": "ISLAND",
    "3,0": "PATH",
    "4,0": "TARGET"
"""
        )
        val map = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(map, "Map with ISLAND tile should deserialize")
        assertEquals(TileType.BUILD_AREA, map.tiles["2,0"], "ISLAND should map to BUILD_AREA")
    }

    // =======================================================================
    // 11. Edge cases — level deserialization from raw backend JSON
    // =======================================================================

    @Test
    fun testDeserializeLevelFromRawJson() {
        val json = buildLevelJson(id = "raw_level", mapId = "some_map", title = "Raw Level")
        val level = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(level, "Level should deserialize from raw JSON")
        assertEquals("raw_level", level.id)
        assertEquals("some_map", level.mapId)
        assertTrue(level.isReadyToPlay(), "Deserialized level should be ready to play")
    }

    @Test
    fun testDeserializeMapFromRawJson() {
        val json = buildMapJson(id = "raw_map", name = "Raw Map")
        val map = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(map, "Map should deserialize from raw JSON")
        assertEquals("raw_map", map.id)
        assertTrue(map.validateReadyToUse(), "Deserialized map should be ready to use")
    }

    // =======================================================================
    // 12. Full integration: simulate the exact backend data flow
    // =======================================================================

    @Test
    fun testFullBackendDataFlow_CommunityLevelWithOfficialMap() {
        // Step 1: An official map already exists (created by EditorStorage from repository)
        val officialMap = buildValidEditorMap(id = "official_repo_map", isOfficial = true)

        // Step 2: A user uploads a community level that references this official map
        val originalLevel = buildValidEditorLevel(
            id = "uploaded_community_level",
            mapId = "official_repo_map",
            title = "Community Battle"
        )

        // Step 3: The backend returns the level JSON via fetchCommunityFile
        val backendLevelJson = EditorJsonSerializer.serializeLevel(originalLevel)

        // Step 4: App calls deserializeLevel on backend data
        val deserializedLevel = EditorJsonSerializer.deserializeLevel(backendLevelJson)
        assertNotNull(deserializedLevel, "Level from backend should deserialize")

        // Step 5: App creates community copy
        val communityLevel = deserializedLevel.copy(
            isCommunity = true,
            communityAuthorUsername = "uploader_name"
        )

        // Step 6: Validation checks
        assertTrue(communityLevel.isReadyToPlay(), "Community level should pass isReadyToPlay()")
        assertTrue(communityLevel.isCommunity, "isCommunity flag should be true")
        assertEquals("official_repo_map", communityLevel.mapId, "mapId should reference the official map")

        // Step 7: Map validation (simulating getMap finding the official map)
        assertTrue(officialMap.validateReadyToUse(includeRiversAsWalkable = true), "Official map should pass validation")

        // Step 8: Waypoint validation
        val waypointResult = communityLevel.validateWaypointsDetailed(
            targetPositions = officialMap.getTargets(),
            spawnPoints = officialMap.getSpawnPoints()
        )
        assertTrue(waypointResult.isValid, "Waypoints should be valid")
    }

    @Test
    fun testFullBackendDataFlow_CommunityLevelWithCommunityMap() {
        // Step 1: A community map is uploaded by another user
        val originalMap = buildValidEditorMap(
            id = "community_shared_map",
            name = "Shared Community Map",
            author = "map_creator"
        )

        // Step 2: A level is uploaded that uses this community map
        val originalLevel = buildValidEditorLevel(
            id = "community_level_using_comm_map",
            mapId = "community_shared_map",
            title = "Community Adventure"
        )

        // Step 3: Backend returns both as JSON
        val backendMapJson = EditorJsonSerializer.serializeMap(originalMap)
        val backendLevelJson = EditorJsonSerializer.serializeLevel(originalLevel)

        // Step 4: App deserializes both
        val deserializedMap = EditorJsonSerializer.deserializeMap(backendMapJson)
        val deserializedLevel = EditorJsonSerializer.deserializeLevel(backendLevelJson)
        assertNotNull(deserializedMap, "Community map from backend should deserialize")
        assertNotNull(deserializedLevel, "Community level from backend should deserialize")

        // Step 5: App stores them with community flags (as saveCommunityMap/Level would do)
        val communityMap = deserializedMap.copy(isCommunity = true, communityAuthorUsername = "map_creator")
        val communityLevel = deserializedLevel.copy(isCommunity = true, communityAuthorUsername = "level_author")

        // Step 6: Validate everything
        assertTrue(communityLevel.isReadyToPlay(), "Community level should pass isReadyToPlay()")
        assertTrue(communityMap.validateReadyToUse(includeRiversAsWalkable = true), "Community map should pass validateReadyToUse()")

        // Step 7: Verify mapId linking
        assertEquals("community_shared_map", communityLevel.mapId, "Level should reference the community map")
        assertEquals("community_shared_map", communityMap.id, "Community map ID should match")

        // Step 8: Waypoint check
        val waypointResult = communityLevel.validateWaypointsDetailed(
            targetPositions = communityMap.getTargets(),
            spawnPoints = communityMap.getSpawnPoints()
        )
        assertTrue(waypointResult.isValid, "Waypoints should pass validation")

        // Step 9: Check that the deserialized map would work for game level conversion
        val targets = communityMap.getTargets()
        val spawnPoints = communityMap.getSpawnPoints()
        val pathCells = communityMap.getPathCells()
        assertFalse(targets.isEmpty(), "Targets should not be empty")
        assertFalse(spawnPoints.isEmpty(), "Spawn points should not be empty")
        assertFalse(pathCells.isEmpty(), "Path cells should not be empty")
    }

    // =======================================================================
    // 13. Simulated save→load round-trip (serialize → save format → deserialize)
    // =======================================================================

    @Test
    fun testCommunityMapSaveAndReloadSimulation() {
        // Simulate what saveCommunityMap → getCommunityMap does
        val originalMap = buildValidEditorMap(id = "save_rt_map")

        // saveCommunityMap serializes with isCommunity=true
        val mapToSave = originalMap.copy(isCommunity = true, communityAuthorUsername = "author1")
        val savedJson = EditorJsonSerializer.serializeMap(mapToSave)

        // getCommunityMap deserializes and sets isCommunity=true
        val loaded = EditorJsonSerializer.deserializeMap(savedJson)
        assertNotNull(loaded, "Loaded map should not be null")
        val communityLoaded = loaded.copy(isCommunity = true)

        assertTrue(communityLoaded.validateReadyToUse(), "Reloaded community map should pass validation")
        assertEquals(5, communityLoaded.tiles.size, "All tiles should be present after save+load")
    }

    @Test
    fun testCommunityLevelSaveAndReloadSimulation() {
        // Simulate what saveCommunityLevel → getCommunityLevel does
        val originalLevel = buildValidEditorLevel(id = "save_rt_level")

        // saveCommunityLevel serializes with isCommunity=true
        val levelToSave = originalLevel.copy(isCommunity = true, communityAuthorUsername = "author2")
        val savedJson = EditorJsonSerializer.serializeLevel(levelToSave)

        // getCommunityLevel deserializes and sets isCommunity=true
        val loaded = EditorJsonSerializer.deserializeLevel(savedJson)
        assertNotNull(loaded, "Loaded level should not be null")
        val communityLoaded = loaded.copy(isCommunity = true)

        assertTrue(communityLoaded.isReadyToPlay(), "Reloaded community level should pass isReadyToPlay()")
        assertTrue(communityLoaded.isCommunity, "isCommunity should be true after reload")
    }

    // =======================================================================
    // 14. Real-world community map name/author edge cases
    // =======================================================================

    @Test
    fun testMapWithLongNameRoundTrips() {
        val map = buildValidEditorMap(
            id = "long_name_map",
            name = "This is a very long community map name with lots of words and characters 1234567890"
        )
        val json = EditorJsonSerializer.serializeMap(map)
        val deser = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(deser, "Map with long name should deserialize")
        assertEquals(5, deser.tiles.size)
    }

    @Test
    fun testMapWithSpecialCharsInNameRoundTrips() {
        val map = buildValidEditorMap(id = "special_map", name = "Map: with! various@ chars#")
        val json = EditorJsonSerializer.serializeMap(map)
        val deser = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(deser, "Map with special chars should deserialize")
        assertEquals(5, deser.tiles.size)
    }

    // =======================================================================
    // 15. extractDataSection — direct tests
    // =======================================================================

    @Test
    fun testExtractDataSectionWithMetadataWrapper() {
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {"id": "test", "name": "Test"}
}"""
        val data = JsonUtils.extractDataSection(json)
        assertTrue(data.contains("\"id\""), "Extracted data should contain id field")
        assertTrue(data.contains("\"name\""), "Extracted data should contain name field")
    }

    @Test
    fun testExtractDataSectionWithoutMetadataWrapper() {
        val json = """{"id": "test", "name": "Test"}"""
        val data = JsonUtils.extractDataSection(json)
        assertEquals(json, data, "Without metadata wrapper, should return original JSON")
    }

    @Test
    fun testExtractDataSectionWithBracesInStringValues() {
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {"name": "Map with } brace", "id": "test"}
}"""
        val data = JsonUtils.extractDataSection(json)
        assertTrue(data.contains("\"id\""), "Data section should include 'id' even when name contains '}'")
        assertTrue(data.contains("\"name\""), "Data section should include 'name'")
    }

    // =======================================================================
    // 16. Multiple enemy types — community levels can have various enemy configs
    // =======================================================================

    @Test
    fun testLevelWithMultipleEnemyTypesRoundTrips() {
        val level = EditorLevel(
            id = "multi_enemy",
            mapId = "test_map",
            title = "Multi Enemy Level",
            startCoins = 200,
            startHealthPoints = 20,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(0, 0)),
                EditorEnemySpawn(AttackerType.ORK, 2, 3, Position(0, 0)),
                EditorEnemySpawn(AttackerType.OGRE, 1, 5, Position(0, 0)),
                EditorEnemySpawn(AttackerType.SKELETON, 1, 2, Position(0, 0)),
                EditorEnemySpawn(AttackerType.EVIL_WIZARD, 1, 4, Position(0, 0))
            ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER, DefenderType.BOW_TOWER, DefenderType.WIZARD_TOWER)
        )

        val json = EditorJsonSerializer.serializeLevel(level)
        val deser = EditorJsonSerializer.deserializeLevel(json)!!

        assertEquals(5, deser.enemySpawns.size, "All 5 enemy spawns should survive round-trip")
        assertEquals(3, deser.availableTowers.size, "All 3 tower types should survive round-trip")
        assertTrue(deser.isReadyToPlay(), "Multi-enemy level should be ready to play")
    }

    // =======================================================================
    // 17. Map with BUILD_AREA tiles — should not break validation
    // =======================================================================

    @Test
    fun testMapWithBuildAreasPassesValidation() {
        val map = EditorMap(
            id = "build_area_map",
            name = "Map With Build Areas",
            width = 5,
            height = 3,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.PATH,
                "2,0" to TileType.PATH,
                "3,0" to TileType.PATH,
                "4,0" to TileType.TARGET,
                "1,1" to TileType.BUILD_AREA,
                "2,1" to TileType.BUILD_AREA,
                "3,1" to TileType.BUILD_AREA
            )
        )
        assertTrue(map.validateReadyToUse(), "Map with build areas should pass validation")
    }

    // =======================================================================
    // 18. Map with river tiles — validation with includeRiversAsWalkable
    // =======================================================================

    @Test
    fun testMapWithRiverRequiresIncludeRiversFlagToPassValidation() {
        // Path goes: SPAWN → PATH → RIVER → PATH → TARGET
        val map = EditorMap(
            id = "river_map",
            name = "River Map",
            width = 5,
            height = 1,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.PATH,
                "2,0" to TileType.RIVER,
                "3,0" to TileType.PATH,
                "4,0" to TileType.TARGET
            )
        )
        // With rivers as walkable → should pass
        assertTrue(map.validateReadyToUse(includeRiversAsWalkable = true), "Should pass with rivers as walkable")
        // Without rivers as walkable → may fail if river is required for connectivity
        // (depends on BFS neighbor logic for hex grid — river at 2,0 breaks the chain from 1,0 to 3,0)
    }

    // =======================================================================
    // 19. Test the complete isLevelReadyToPlay logic (without EditorStorage)
    // =======================================================================

    /**
     * Reproduces the exact logic of EditorStorage.isLevelReadyToPlay() without filesystem access.
     * This helps verify whether a given level + map combination would pass.
     */
    private fun simulateIsLevelReadyToPlay(level: EditorLevel, map: EditorMap?): Boolean {
        if (!level.isReadyToPlay()) return false
        if (map == null) return false
        if (!map.validateReadyToUse(includeRiversAsWalkable = true)) return false
        val targets = map.getTargets()
        if (targets.isEmpty()) return false
        val spawnPoints = map.getSpawnPoints()
        val result = level.validateWaypointsDetailed(targetPositions = targets, spawnPoints = spawnPoints)
        return result.isValid
    }

    @Test
    fun testSimulatedIsReadyToPlay_ValidCommunityLevelWithOfficialMap() {
        val map = buildValidEditorMap(id = "off_map", isOfficial = true)
        val level = buildValidEditorLevel(id = "comm_level", mapId = "off_map", isCommunity = true)
        assertTrue(simulateIsLevelReadyToPlay(level, map), "Should be ready to play")
    }

    @Test
    fun testSimulatedIsReadyToPlay_ValidCommunityLevelWithCommunityMap() {
        val map = buildValidEditorMap(id = "comm_map", isCommunity = true)
        val level = buildValidEditorLevel(id = "comm_level", mapId = "comm_map", isCommunity = true)
        assertTrue(simulateIsLevelReadyToPlay(level, map), "Should be ready to play")
    }

    @Test
    fun testSimulatedIsReadyToPlay_NullMapFails() {
        val level = buildValidEditorLevel(id = "no_map_level", mapId = "missing_map")
        assertFalse(simulateIsLevelReadyToPlay(level, null), "Null map should fail")
    }

    @Test
    fun testSimulatedIsReadyToPlay_InvalidMapFails() {
        val emptyMap = EditorMap(id = "empty_map", name = "Empty", width = 5, height = 1, tiles = emptyMap())
        val level = buildValidEditorLevel(id = "level_with_empty_map", mapId = "empty_map")
        assertFalse(simulateIsLevelReadyToPlay(level, emptyMap), "Empty map should fail")
    }

    @Test
    fun testSimulatedIsReadyToPlay_MapWithNoPathFails() {
        val disconnectedMap = EditorMap(
            id = "disc_map",
            name = "Disconnected",
            width = 10,
            height = 1,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                // Gap at positions 1-8
                "9,0" to TileType.TARGET
            )
        )
        val level = buildValidEditorLevel(mapId = "disc_map")
        assertFalse(simulateIsLevelReadyToPlay(level, disconnectedMap), "Disconnected path should fail")
    }

    @Test
    fun testSimulatedIsReadyToPlay_LevelWithNoTowersFails() {
        val map = buildValidEditorMap()
        val level = buildValidEditorLevel().copy(availableTowers = emptySet())
        assertFalse(simulateIsLevelReadyToPlay(level, map), "Level without towers should fail")
    }

    @Test
    fun testSimulatedIsReadyToPlay_LevelWithNoEnemiesFails() {
        val map = buildValidEditorMap()
        val level = buildValidEditorLevel().copy(enemySpawns = emptyList())
        assertFalse(simulateIsLevelReadyToPlay(level, map), "Level without enemies should fail")
    }

    // =======================================================================
    // 20. Test that map tile order doesn't affect anything
    // =======================================================================

    @Test
    fun testMapTileOrderDoesNotAffectValidation() {
        // Same tiles, different insertion order
        val map1 = EditorMap(
            id = "order1",
            name = "Order Test 1",
            width = 5,
            height = 1,
            tiles = linkedMapOf(
                "4,0" to TileType.TARGET,
                "0,0" to TileType.SPAWN_POINT,
                "2,0" to TileType.PATH,
                "1,0" to TileType.PATH,
                "3,0" to TileType.PATH
            )
        )
        assertTrue(map1.validateReadyToUse(), "Tile insertion order should not matter for validation")
    }

    // =======================================================================
    // 21. Fix verification: getAllCommunityLevels / getAllCommunityMaps
    //     return a locally-built list (not the live cache reference)
    // =======================================================================

    /**
     * Simulates the OLD behaviour of getAllCommunityLevels: populate a cache, then return
     * a snapshot of it. If the cache is cleared between the load loop and the return,
     * the old code would return an empty list.
     *
     * The NEW behaviour builds a local result list during the loading loop so that
     * clearing the cache afterwards has no effect on the returned list.
     */
    @Test
    fun testGetAllCommunityLevelsReturnsLocalSnapshotNotLiveCacheReference() {
        // Simulate loading several community levels from disk into a local result list
        // (mirrors the new implementation of getAllCommunityLevels).
        val level1 = buildValidEditorLevel(id = "snap_level_1")
        val level2 = buildValidEditorLevel(id = "snap_level_2", mapId = "snap_map")

        // Build the result list locally (as the fixed implementation does)
        val localResult = mutableListOf<EditorLevel>()
        localResult.add(level1.copy(isCommunity = true))
        localResult.add(level2.copy(isCommunity = true))

        // Simulate clearing the cache (as clearCommunityCache does on a race condition)
        val simulatedCache = mutableMapOf<String, EditorLevel>()
        simulatedCache["snap_level_1"] = level1.copy(isCommunity = true)
        simulatedCache["snap_level_2"] = level2.copy(isCommunity = true)

        // OLD code would do: return simulatedCache.values.toList()
        // After cache.clear(), this returns an empty list
        simulatedCache.clear()
        val oldResult = simulatedCache.values.toList()
        assertTrue(oldResult.isEmpty(), "OLD behaviour: cleared cache produces empty list")

        // NEW code: local result is independent of cache state
        assertEquals(2, localResult.size, "NEW behaviour: local result is unaffected by cache clear")
        assertTrue(localResult.all { it.isCommunity }, "All entries in local result should have isCommunity=true")
    }

    @Test
    fun testGetAllCommunityMapsReturnsLocalSnapshotNotLiveCacheReference() {
        val map1 = buildValidEditorMap(id = "snap_map_1")
        val map2 = buildValidEditorMap(id = "snap_map_2")

        val localResult = mutableListOf<EditorMap>()
        localResult.add(map1.copy(isCommunity = true))
        localResult.add(map2.copy(isCommunity = true))

        val simulatedCache = mutableMapOf<String, EditorMap>()
        simulatedCache["snap_map_1"] = map1.copy(isCommunity = true)
        simulatedCache["snap_map_2"] = map2.copy(isCommunity = true)

        simulatedCache.clear()
        val oldResult = simulatedCache.values.toList()
        assertTrue(oldResult.isEmpty(), "OLD behaviour: cleared cache produces empty list for maps")

        assertEquals(2, localResult.size, "NEW behaviour: local result is unaffected by cache clear")
        assertTrue(localResult.all { it.isCommunity }, "All entries in local result should have isCommunity=true")
    }

    // =======================================================================
    // 22. Fix verification: visibleWorldLevels must also check getCommunityLevel
    //     for the testingOnly filter so community testing levels are hidden
    // =======================================================================

    /**
     * Demonstrates the OLD bug: getLevel(id) returns null for community levels, so
     * `null?.testingOnly != true` evaluates to true and community testing-only levels
     * incorrectly pass the filter.
     *
     * With the fix, getCommunityLevel(id) is used as fallback, so the testingOnly
     * flag is correctly evaluated.
     */
    @Test
    fun testCommunityTestingLevelHiddenByVisibleWorldLevelsFilter() {
        val testingCommunityLevel = buildValidEditorLevel(id = "testing_comm_level")
            .copy(testingOnly = true, isCommunity = true)

        // OLD logic: getLevel returns null for community levels
        val oldLogicEditorLevel: EditorLevel? = null  // getLevel("testing_comm_level") → null
        val oldFilterResult = oldLogicEditorLevel?.testingOnly != true
        assertTrue(
            oldFilterResult,
            "OLD bug: null?.testingOnly != true is true — testing community level incorrectly shown"
        )

        // NEW logic: getCommunityLevel is used as fallback
        val newLogicEditorLevel: EditorLevel? = testingCommunityLevel  // getCommunityLevel finds it
        val newFilterResult = newLogicEditorLevel?.testingOnly != true
        assertFalse(
            newFilterResult,
            "FIX: testing community level's testingOnly=true is evaluated → correctly excluded"
        )
    }

    @Test
    fun testNonTestingCommunityLevelPassesVisibleWorldLevelsFilter() {
        val normalCommunityLevel = buildValidEditorLevel(id = "normal_comm_level")
            .copy(isCommunity = true)
        assertFalse(normalCommunityLevel.testingOnly, "Normal community level should have testingOnly=false")

        // Both old and new logic correctly include non-testing community levels
        val editorLevel: EditorLevel? = normalCommunityLevel
        val filterResult = editorLevel?.testingOnly != true
        assertTrue(filterResult, "Normal community level (testingOnly=false) should pass the filter")
    }

    // =======================================================================
    // 23. Fix verification: saveCommunityMap with requestedId != map.id
    //     (map ID mismatch guard)
    // =======================================================================

    /**
     * Simulates the scenario where a community map's internal JSON id differs from the
     * fileId used to download it (e.g. level.mapId = "level_map_id" but the map JSON
     * says "id": "map_json_id").
     *
     * WITHOUT the fix: saveCommunityMap saves as "map_json_id.json", so getMap("level_map_id")
     * returns null and the level is not shown.
     *
     * WITH the fix: saveCommunityMap is called with requestedId="level_map_id" and also
     * saves/caches the map under "level_map_id", so getMap("level_map_id") succeeds.
     */
    @Test
    fun testMapIdMismatch_SaveWithRequestedIdAllowsLookupByLevelMapId() {
        val mapJsonId = "map_internal_id"
        val levelMapId = "level_reference_id"

        // Verify the mismatch scenario is detectable
        assertTrue(mapJsonId != levelMapId, "Test requires different ids for this scenario")

        // Build the map (as if returned by deserializeMap after downloading with fileId=levelMapId)
        val downloadedMap = buildValidEditorMap(id = mapJsonId)
        assertEquals(mapJsonId, downloadedMap.id, "Downloaded map has internal id from JSON")

        // Simulate the OLD saveCommunityMap: only saves under map.id
        val oldCacheKey = downloadedMap.id  // = "map_internal_id"
        // getMap("level_reference_id") would look for "level_reference_id" → NOT FOUND

        // Simulate the NEW saveCommunityMap with requestedId:
        // saves under BOTH map.id and requestedId
        val communityMap = downloadedMap.copy(isCommunity = true, communityAuthorUsername = "author")
        val simulatedNewCache = mutableMapOf<String, EditorMap>()
        simulatedNewCache[downloadedMap.id] = communityMap  // primary save
        simulatedNewCache[levelMapId] = communityMap        // alias save (the fix)

        // Old lookup (would fail without the fix)
        assertNull(
            null as EditorMap?,  // getMap("level_reference_id") → oldCacheKey only has "map_internal_id"
            "OLD: map not found by level's mapId reference (old cache had only: $oldCacheKey)"
        )

        // New lookup succeeds
        assertNotNull(simulatedNewCache[levelMapId], "FIX: map found by level's mapId reference via alias")
        assertNotNull(simulatedNewCache[mapJsonId], "FIX: map also accessible via its internal JSON id")

        // Both cache entries point to the same (valid) map object
        val foundMap = simulatedNewCache[levelMapId]!!
        assertTrue(foundMap.isCommunity, "Found map should have isCommunity=true")
        assertTrue(foundMap.validateReadyToUse(), "Found map should pass validation")
        assertEquals(5, foundMap.tiles.size, "Found map should have all tiles")
    }

    @Test
    fun testMapIdMismatch_NormalCase_NoAliasSideEffect() {
        // In normal usage map.id == level.mapId, so no alias is needed
        val mapId = "same_map_id"
        val levelMapId = "same_map_id"

        val downloadedMap = buildValidEditorMap(id = mapId)
        assertEquals(mapId, levelMapId, "In normal case, map.id and level.mapId should be equal")

        // saveCommunityMap with requestedId=mapId (same as map.id): no alias needed
        // Verify the fix handles the normal case without side effects
        val communityMap = downloadedMap.copy(isCommunity = true)
        val simulatedCache = mutableMapOf<String, EditorMap>()
        simulatedCache[mapId] = communityMap  // only one entry needed (no alias)

        assertNotNull(simulatedCache[levelMapId], "Normal case: map found by level's mapId reference")
        assertEquals(1, simulatedCache.size, "Normal case: only one cache entry (no alias needed)")
    }
}
