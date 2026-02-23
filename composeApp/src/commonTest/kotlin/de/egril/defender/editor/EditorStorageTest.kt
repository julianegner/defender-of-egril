package de.egril.defender.editor

import de.egril.defender.game.LevelData
import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.AttackerType
import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.DefenderType
import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import androidx.compose.runtime.mutableStateOf
import kotlin.test.assertEquals
import androidx.compose.runtime.mutableStateOf
import kotlin.test.assertNotNull
import androidx.compose.runtime.mutableStateOf
import kotlin.test.assertTrue

/**
 * Tests for EditorStorage - limited to testing data structures
 * Full file-based tests should be done manually on desktop
 */
class EditorStorageTest {
    
    @Test
    fun testEditorMapStructure() {
        // Test creating an editor map
        val tiles = mapOf(
            "0,0" to TileType.SPAWN_POINT,
            "10,5" to TileType.TARGET,
            "1,0" to TileType.PATH,
            "5,5" to TileType.BUILD_AREA
        )
        
        val map = EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 20,
            height = 10,
            tiles = tiles
        )
        
        assertEquals("test_map", map.id)
        assertEquals(20, map.width)
        assertEquals(10, map.height)
        assertEquals(TileType.SPAWN_POINT, map.getTileType(0, 0))
        assertEquals(TileType.TARGET, map.getTileType(10, 5))
        assertEquals(TileType.NO_PLAY, map.getTileType(15, 8)) // Default for unset tiles
    }
    
    @Test
    fun testEditorEnemySpawn() {
        val spawn = EditorEnemySpawn(
            attackerType = AttackerType.GOBLIN,
            level = 2,
            spawnTurn = 5
        )
        
        assertEquals(AttackerType.GOBLIN, spawn.attackerType)
        assertEquals(2, spawn.level)
        assertEquals(5, spawn.spawnTurn)
        assertEquals(40, spawn.healthPoints) // 20 * 2
    }
    
    @Test
    fun testEditorLevelStructure() {
        val level = EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            subtitle = "A test",
            startCoins = 150,
            startHealthPoints = 15,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 1),
                EditorEnemySpawn(AttackerType.ORK, 1, 2)
            ),
            availableTowers = setOf()
        )
        
        assertEquals("test_level", level.id)
        assertEquals("test_map", level.mapId)
        assertEquals("Test Level", level.title)
        assertEquals(150, level.startCoins)
        assertEquals(2, level.enemySpawns.size)
    }
    
    @Test
    fun testLevelSequenceStructure() {
        val sequence = LevelSequence(listOf("level_1", "level_2", "level_3"))
        
        assertEquals(3, sequence.sequence.size)
        assertEquals("level_1", sequence.sequence[0])
        assertEquals("level_2", sequence.sequence[1])
        assertEquals("level_3", sequence.sequence[2])
    }
    
    @Test
    fun testLevelReadyToPlay() {
        // Level with towers and enemy spawns - should be ready
        val readyLevel = EditorLevel(
            id = "ready_level",
            mapId = "test_map",
            title = "Ready Level",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        assertTrue(readyLevel.isReadyToPlay())
        
        // Level without towers - should not be ready
        val noTowersLevel = EditorLevel(
            id = "no_towers",
            mapId = "test_map",
            title = "No Towers",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = emptySet()
        )
        assertTrue(!noTowersLevel.isReadyToPlay())
        
        // Level without enemy spawns - should not be ready
        val noSpawnsLevel = EditorLevel(
            id = "no_spawns",
            mapId = "test_map",
            title = "No Spawns",
            startCoins = 100,
            enemySpawns = emptyList(),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        assertTrue(!noSpawnsLevel.isReadyToPlay())
        
        // Level with neither - should not be ready
        val emptyLevel = EditorLevel(
            id = "empty",
            mapId = "test_map",
            title = "Empty",
            startCoins = 100,
            enemySpawns = emptyList(),
            availableTowers = emptySet()
        )
        assertTrue(!emptyLevel.isReadyToPlay())
    }
    
    @Test
    fun testIsLevelReadyToPlayFunction() {
        // Note: This test can only validate the logic, not actual file I/O
        // The function would return false for these test levels since maps don't exist in storage
        
        // Test that a level with proper structure would pass initial checks
        val validLevel = EditorLevel(
            id = "valid_test",
            mapId = "valid_map",
            title = "Valid Test",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            waypoints = emptyList() // Valid for a level
        )
        
        // The level itself should be ready (has towers and spawns)
        assertTrue(validLevel.isReadyToPlay())
        
        // Test that a level without essential fields fails initial check
        val invalidLevel = EditorLevel(
            id = "invalid_test",
            mapId = "valid_map",
            title = "Invalid Test",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = emptyList(), // Missing spawns
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        
        assertTrue(!invalidLevel.isReadyToPlay())
    }
    
    @Test
    fun testJsonSerialization() {
        // Test map serialization
        val map = EditorMap(
            id = "test",
            name = "Test",
            width = 10,
            height = 8,
            tiles = mapOf("0,0" to TileType.SPAWN_POINT)
        )
        
        val json = EditorJsonSerializer.serializeMap(map)
        assertNotNull(json)
        assertTrue(json.contains("\"id\": \"test\""))
        assertTrue(json.contains("\"width\": 10"))
        
        val deserialized = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(deserialized)
        assertEquals("test", deserialized.id)
        assertEquals(10, deserialized.width)
    }
    
    @Test
    fun testLevelSequenceAddRemove() {
        // Test adding a level to sequence
        val initialSequence = listOf("level_1", "level_2")
        var sequence = LevelSequence(initialSequence)
        
        // Simulate adding a level
        val newSequence = sequence.sequence + "level_3"
        sequence = LevelSequence(newSequence)
        
        assertEquals(3, sequence.sequence.size)
        assertEquals("level_3", sequence.sequence[2])
        
        // Test adding at specific index
        val insertedSequence = sequence.sequence.toMutableList()
        insertedSequence.add(1, "level_1_5")
        sequence = LevelSequence(insertedSequence)
        
        assertEquals(4, sequence.sequence.size)
        assertEquals("level_1_5", sequence.sequence[1])
        assertEquals("level_2", sequence.sequence[2])
        
        // Simulate removing a level
        val removedSequence = sequence.sequence.filter { it != "level_1_5" }
        sequence = LevelSequence(removedSequence)
        
        assertEquals(3, sequence.sequence.size)
        assertEquals("level_1", sequence.sequence[0])
        assertEquals("level_2", sequence.sequence[1])
        assertEquals("level_3", sequence.sequence[2])
    }
    
    @Test
    fun testMoveLevelToPosition() {
        // Test moving a level forward in the sequence
        var sequence = LevelSequence(listOf("level_1", "level_2", "level_3", "level_4"))
        
        // Move level_2 from index 1 to index 3
        val fromIndex = sequence.sequence.indexOf("level_2")
        val toIndex = 3
        var newSeq = sequence.sequence.toMutableList()
        newSeq.removeAt(fromIndex)
        val adjustedIndex = if (fromIndex < toIndex) toIndex - 1 else toIndex
        newSeq.add(adjustedIndex, "level_2")
        sequence = LevelSequence(newSeq)
        
        assertEquals(4, sequence.sequence.size)
        assertEquals("level_1", sequence.sequence[0])
        assertEquals("level_3", sequence.sequence[1])
        assertEquals("level_2", sequence.sequence[2])
        assertEquals("level_4", sequence.sequence[3])
        
        // Test moving a level backward in the sequence
        sequence = LevelSequence(listOf("level_1", "level_2", "level_3", "level_4"))
        
        // Move level_3 from index 2 to index 0
        val fromIndex2 = sequence.sequence.indexOf("level_3")
        val toIndex2 = 0
        newSeq = sequence.sequence.toMutableList()
        newSeq.removeAt(fromIndex2)
        val adjustedIndex2 = if (fromIndex2 < toIndex2) toIndex2 - 1 else toIndex2
        newSeq.add(adjustedIndex2, "level_3")
        sequence = LevelSequence(newSeq)
        
        assertEquals(4, sequence.sequence.size)
        assertEquals("level_3", sequence.sequence[0])
        assertEquals("level_1", sequence.sequence[1])
        assertEquals("level_2", sequence.sequence[2])
        assertEquals("level_4", sequence.sequence[3])
        
        // Test adding a new level at specific position
        newSeq = sequence.sequence.toMutableList()
        newSeq.add(2, "level_new")
        sequence = LevelSequence(newSeq)
        
        assertEquals(5, sequence.sequence.size)
        assertEquals("level_3", sequence.sequence[0])
        assertEquals("level_1", sequence.sequence[1])
        assertEquals("level_new", sequence.sequence[2])
        assertEquals("level_2", sequence.sequence[3])
        assertEquals("level_4", sequence.sequence[4])
    }
    
    @Test
    fun testLevelPrerequisitesBasic() {
        // Level with no prerequisites
        val entryLevel = EditorLevel(
            id = "entry_level",
            mapId = "test_map",
            title = "Entry Level",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            prerequisites = emptySet()
        )
        assertEquals(0, entryLevel.getEffectiveRequiredCount())
        
        // Level with prerequisites - all required (default)
        val level2 = EditorLevel(
            id = "level_2",
            mapId = "test_map",
            title = "Level 2",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            prerequisites = setOf("entry_level", "another_level"),
            requiredPrerequisiteCount = null  // All required
        )
        assertEquals(2, level2.getEffectiveRequiredCount())
        
        // Level with partial prerequisites required
        val level3 = EditorLevel(
            id = "level_3",
            mapId = "test_map",
            title = "Level 3",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            prerequisites = setOf("level_a", "level_b", "level_c"),
            requiredPrerequisiteCount = 2  // Only 2 of 3 required
        )
        assertEquals(2, level3.getEffectiveRequiredCount())
    }
    
    @Test
    fun testLevelPrerequisitesSerialization() {
        val level = EditorLevel(
            id = "test_prereq_level",
            mapId = "test_map",
            title = "Test Prerequisites",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            prerequisites = setOf("prereq_a", "prereq_b"),
            requiredPrerequisiteCount = 1
        )
        
        val json = EditorJsonSerializer.serializeLevel(level)
        assertNotNull(json)
        assertTrue(json.contains("\"prerequisites\""))
        assertTrue(json.contains("prereq_a"))
        assertTrue(json.contains("prereq_b"))
        assertTrue(json.contains("\"requiredPrerequisiteCount\": 1"))
        
        val deserialized = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(deserialized)
        assertEquals("test_prereq_level", deserialized.id)
        assertEquals(2, deserialized.prerequisites.size)
        assertTrue(deserialized.prerequisites.contains("prereq_a"))
        assertTrue(deserialized.prerequisites.contains("prereq_b"))
        assertEquals(1, deserialized.requiredPrerequisiteCount)
    }
    
    @Test
    fun testEffectiveRequiredCountEdgeCases() {
        // requiredPrerequisiteCount larger than prerequisites size
        val level = EditorLevel(
            id = "test",
            mapId = "map",
            title = "Test",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            prerequisites = setOf("a", "b"),
            requiredPrerequisiteCount = 5  // Larger than 2
        )
        // Should cap at size of prerequisites
        assertEquals(2, level.getEffectiveRequiredCount())
        
        // requiredPrerequisiteCount of 0
        val level0 = level.copy(requiredPrerequisiteCount = 0)
        assertEquals(0, level0.getEffectiveRequiredCount())
    }

    // ==================== Metadata wrapper tests ====================

    @Test
    fun testMapMetadataRoundTrip() {
        val map = EditorMap(
            id = "test_map_roundtrip",
            name = "Round Trip Map",
            width = 20,
            height = 15,
            tiles = mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "5,5" to TileType.BUILD_AREA,
                "10,10" to TileType.TARGET
            )
        )

        val json = EditorJsonSerializer.serializeMap(map)

        // Verify metadata fields are present
        assertTrue(json.contains("\"metadata\""))
        assertTrue(json.contains("\"program\": \"Defender of Egril\""))
        assertTrue(json.contains("\"type\": \"map\""))
        assertTrue(json.contains("\"data\""))

        // Verify round-trip
        val deserialized = EditorJsonSerializer.deserializeMap(json)
        assertNotNull(deserialized)
        assertEquals(map.id, deserialized.id)
        assertEquals(map.name, deserialized.name)
        assertEquals(map.width, deserialized.width)
        assertEquals(map.height, deserialized.height)
        assertEquals(TileType.SPAWN_POINT, deserialized.getTileType(0, 0))
        assertEquals(TileType.BUILD_AREA, deserialized.getTileType(5, 5))
        assertEquals(TileType.TARGET, deserialized.getTileType(10, 10))
    }

    @Test
    fun testLevelMetadataRoundTrip() {
        val level = EditorLevel(
            id = "test_level_roundtrip",
            mapId = "some_map",
            title = "Round Trip Level",
            subtitle = "A test level",
            startCoins = 200,
            startHealthPoints = 20,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 1),
                EditorEnemySpawn(AttackerType.ORK, 2, 3)
            ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER, DefenderType.BOW_TOWER),
            prerequisites = setOf("level_a"),
            requiredPrerequisiteCount = 1
        )

        val json = EditorJsonSerializer.serializeLevel(level)

        // Verify metadata fields are present
        assertTrue(json.contains("\"metadata\""))
        assertTrue(json.contains("\"program\": \"Defender of Egril\""))
        assertTrue(json.contains("\"type\": \"level\""))
        assertTrue(json.contains("\"data\""))

        // Verify round-trip
        val deserialized = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(deserialized)
        assertEquals(level.id, deserialized.id)
        assertEquals(level.mapId, deserialized.mapId)
        assertEquals(level.title, deserialized.title)
        assertEquals(level.subtitle, deserialized.subtitle)
        assertEquals(level.startCoins, deserialized.startCoins)
        assertEquals(level.startHealthPoints, deserialized.startHealthPoints)
        assertEquals(2, deserialized.enemySpawns.size)
        assertEquals(2, deserialized.availableTowers.size)
        assertTrue(deserialized.availableTowers.contains(DefenderType.SPIKE_TOWER))
        assertTrue(deserialized.availableTowers.contains(DefenderType.BOW_TOWER))
        assertEquals(1, deserialized.prerequisites.size)
        assertEquals(1, deserialized.requiredPrerequisiteCount)
    }

    @Test
    fun testSequenceMetadataRoundTrip() {
        val sequence = LevelSequence(listOf("level_one", "level_two", "level_three"))

        val json = EditorJsonSerializer.serializeSequence(sequence)

        // Verify metadata fields are present
        assertTrue(json.contains("\"metadata\""))
        assertTrue(json.contains("\"program\": \"Defender of Egril\""))
        assertTrue(json.contains("\"type\": \"sequence\""))
        assertTrue(json.contains("\"data\""))

        // Verify round-trip
        val deserialized = EditorJsonSerializer.deserializeSequence(json)
        assertNotNull(deserialized)
        assertEquals(3, deserialized.sequence.size)
        assertEquals("level_one", deserialized.sequence[0])
        assertEquals("level_two", deserialized.sequence[1])
        assertEquals("level_three", deserialized.sequence[2])
    }

    @Test
    fun testWorldMapMetadataRoundTrip() {
        val worldMapData = WorldMapData(
            locations = listOf(
                WorldMapLocationData(
                    id = "loc_start",
                    name = "Start",
                    position = WorldMapPoint(100, 200),
                    levelIds = listOf("level_one", "level_two")
                ),
                WorldMapLocationData(
                    id = "loc_end",
                    name = "End",
                    position = WorldMapPoint(500, 600),
                    levelIds = listOf("level_three")
                )
            ),
            paths = listOf(
                WorldMapPathData(
                    fromLocationId = "loc_start",
                    toLocationId = "loc_end",
                    type = ConnectionType.ROAD
                )
            )
        )

        val json = EditorJsonSerializer.serializeWorldMapData(worldMapData)

        // Verify metadata fields are present
        assertTrue(json.contains("\"metadata\""))
        assertTrue(json.contains("\"program\": \"Defender of Egril\""))
        assertTrue(json.contains("\"type\": \"worldmap\""))
        assertTrue(json.contains("\"data\""))

        // Verify round-trip
        val deserialized = EditorJsonSerializer.deserializeWorldMapData(json)
        assertNotNull(deserialized)
        assertEquals(2, deserialized.locations.size)
        assertEquals("loc_start", deserialized.locations[0].id)
        assertEquals("Start", deserialized.locations[0].name)
        assertEquals(100, deserialized.locations[0].position.x)
        assertEquals(200, deserialized.locations[0].position.y)
        assertEquals(2, deserialized.locations[0].levelIds.size)
        assertEquals("loc_end", deserialized.locations[1].id)
        assertEquals(1, deserialized.paths.size)
        assertEquals("loc_start", deserialized.paths[0].fromLocationId)
        assertEquals("loc_end", deserialized.paths[0].toLocationId)
    }

    @Test
    fun testBackwardCompatibilityMap() {
        // Old-style map JSON without metadata wrapper
        val oldJson = """{
  "id": "old_map",
  "name": "Old Map",
  "width": 10,
  "height": 8,
  "readyToUse": false,
  "isOfficial": false,
  "tiles": {
    "0,0": "SPAWN_POINT",
    "9,7": "TARGET"
  }
}"""
        val deserialized = EditorJsonSerializer.deserializeMap(oldJson)
        assertNotNull(deserialized)
        assertEquals("old_map", deserialized.id)
        assertEquals("Old Map", deserialized.name)
        assertEquals(10, deserialized.width)
        assertEquals(8, deserialized.height)
        assertEquals(TileType.SPAWN_POINT, deserialized.getTileType(0, 0))
        assertEquals(TileType.TARGET, deserialized.getTileType(9, 7))
    }

    @Test
    fun testBackwardCompatibilityLevel() {
        // Old-style level JSON without metadata wrapper
        val oldJson = """{
  "id": "old_level",
  "mapId": "old_map",
  "title": "Old Level",
  "subtitle": "",
  "startCoins": 100,
  "startHealthPoints": 10,
  "enemySpawns": [
    {"attackerType": "GOBLIN", "level": 1, "spawnTurn": 1, "spawnPoint": {"x": 0, "y": 1}}
  ],
  "availableTowers": ["SPIKE_TOWER"],
  "waypoints": [],
  "prerequisites": []
}"""
        val deserialized = EditorJsonSerializer.deserializeLevel(oldJson)
        assertNotNull(deserialized)
        assertEquals("old_level", deserialized.id)
        assertEquals("old_map", deserialized.mapId)
        assertEquals("Old Level", deserialized.title)
        assertEquals(100, deserialized.startCoins)
        assertEquals(1, deserialized.enemySpawns.size)
        assertEquals(AttackerType.GOBLIN, deserialized.enemySpawns[0].attackerType)
        assertTrue(deserialized.availableTowers.contains(DefenderType.SPIKE_TOWER))
    }

    @Test
    fun testBackwardCompatibilitySequence() {
        // Old-style sequence JSON without metadata wrapper
        val oldJson = """{
  "sequence": ["level_a", "level_b", "level_c"]
}"""
        val deserialized = EditorJsonSerializer.deserializeSequence(oldJson)
        assertNotNull(deserialized)
        assertEquals(3, deserialized.sequence.size)
        assertEquals("level_a", deserialized.sequence[0])
        assertEquals("level_b", deserialized.sequence[1])
        assertEquals("level_c", deserialized.sequence[2])
    }

    @Test
    fun testBackwardCompatibilityWorldMap() {
        // Old-style worldmap JSON without metadata wrapper
        val oldJson = """{
  "locations": [
    {
      "id": "start",
      "name": "Start Location",
      "position": {"x": 100, "y": 200},
      "levelIds": ["lvl1"]
    }
  ],
  "paths": []
}"""
        val deserialized = EditorJsonSerializer.deserializeWorldMapData(oldJson)
        assertNotNull(deserialized)
        assertEquals(1, deserialized.locations.size)
        assertEquals("start", deserialized.locations[0].id)
        assertEquals("Start Location", deserialized.locations[0].name)
        assertEquals(0, deserialized.paths.size)
    }

    @Test
    fun testExtractDataSection() {
        // Test with metadata wrapper
        val wrappedJson = """{
  "metadata": {
    "program": "Defender of Egril",
    "type": "map"
  },
  "data": {
    "id": "test",
    "name": "Test"
  }
}"""
        val extracted = EditorJsonSerializer.extractDataSection(wrappedJson)
        assertTrue(extracted.contains("\"id\": \"test\""))
        assertTrue(extracted.contains("\"name\": \"Test\""))
        assertTrue(!extracted.contains("\"metadata\""))

        // Test without metadata wrapper (old format)
        val plainJson = """{"id": "plain", "name": "Plain"}"""
        val unchanged = EditorJsonSerializer.extractDataSection(plainJson)
        assertEquals(plainJson, unchanged)
    }
}

