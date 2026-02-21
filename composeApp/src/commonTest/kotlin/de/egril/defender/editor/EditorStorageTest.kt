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
import androidx.compose.runtime.mutableStateOf

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
}

