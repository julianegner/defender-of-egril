package com.defenderofegril.editor

import com.defenderofegril.game.LevelData
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
            "5,5" to TileType.ISLAND
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
}
