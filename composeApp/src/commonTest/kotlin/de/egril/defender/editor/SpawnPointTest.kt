package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SpawnPointTest {
    
    @Test
    fun testEditorEnemySpawnWithSpawnPoint() {
        val spawnPoint = Position(5, 10)
        val spawn = EditorEnemySpawn(
            attackerType = AttackerType.GOBLIN,
            level = 1,
            spawnTurn = 1,
            spawnPoint = spawnPoint
        )
        
        assertEquals(spawnPoint, spawn.spawnPoint)
        assertEquals(AttackerType.GOBLIN, spawn.attackerType)
        assertEquals(1, spawn.level)
        assertEquals(1, spawn.spawnTurn)
    }
    
    @Test
    fun testEditorEnemySpawnWithoutSpawnPoint() {
        // Test backward compatibility - spawn point is optional
        val spawn = EditorEnemySpawn(
            attackerType = AttackerType.ORK,
            level = 2,
            spawnTurn = 5
        )
        
        assertEquals(null, spawn.spawnPoint)
        assertEquals(AttackerType.ORK, spawn.attackerType)
        assertEquals(2, spawn.level)
        assertEquals(5, spawn.spawnTurn)
    }
    
    @Test
    fun testSerializationWithSpawnPoint() {
        val spawnPoint = Position(3, 7)
        val spawn = EditorEnemySpawn(
            attackerType = AttackerType.SKELETON,
            level = 3,
            spawnTurn = 2,
            spawnPoint = spawnPoint
        )
        
        val level = EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            subtitle = "Testing",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(spawn),
            availableTowers = emptySet()
        )
        
        val json = EditorJsonSerializer.serializeLevel(level)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)
        
        assertNotNull(deserialized)
        assertEquals(1, deserialized.enemySpawns.size)
        assertEquals(spawnPoint, deserialized.enemySpawns[0].spawnPoint)
    }
    
    @Test
    fun testSerializationWithoutSpawnPoint() {
        // Test backward compatibility
        val spawn = EditorEnemySpawn(
            attackerType = AttackerType.GOBLIN,
            level = 1,
            spawnTurn = 1,
            spawnPoint = null
        )
        
        val level = EditorLevel(
            id = "test_level_2",
            mapId = "test_map",
            title = "Test Level 2",
            subtitle = "Testing",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(spawn),
            availableTowers = emptySet()
        )
        
        val json = EditorJsonSerializer.serializeLevel(level)
        val deserialized = EditorJsonSerializer.deserializeLevel(json)
        
        assertNotNull(deserialized)
        assertEquals(1, deserialized.enemySpawns.size)
        assertEquals(null, deserialized.enemySpawns[0].spawnPoint)
    }
}
