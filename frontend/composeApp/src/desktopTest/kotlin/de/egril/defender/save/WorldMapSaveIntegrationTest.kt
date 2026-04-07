package de.egril.defender.save

import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Integration test for world map progress in savegame files
 */
class WorldMapSaveIntegrationTest {
    
    @Test
    fun testSaveGameIncludesWorldMapProgress() {
        // Create a world map with some progress
        val worldMapSave = WorldMapSave(
            levelStatuses = mapOf(
                "level_001" to LevelStatus.WON,
                "level_002" to LevelStatus.UNLOCKED,
                "level_003" to LevelStatus.LOCKED
            )
        )
        
        // Create a saved game with world map progress
        val savedGame = SavedGame(
            id = "test_integration",
            timestamp = System.currentTimeMillis(),
            levelId = 1,
            levelName = "Test Level",
            turnNumber = 5,
            coins = 100,
            healthPoints = 10,
            phase = GamePhase.PLAYER_TURN,
            defenders = emptyList(),
            attackers = emptyList(),
            nextDefenderId = 1,
            nextAttackerId = 1,
            currentWaveIndex = 0,
            spawnCounter = 0,
            attackersToSpawn = emptyList(),
            fieldEffects = emptyList(),
            traps = emptyList(),
            worldMapSave = worldMapSave
        )
        
        // Serialize to JSON
        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        assertNotNull(json)
        
        // Verify world map data is in the JSON
        assert(json.contains("worldMapSave"))
        assert(json.contains("level_001"))
        assert(json.contains("WON"))
        
        // Deserialize back
        val deserialized = SaveJsonSerializer.deserializeSavedGame(json)
        assertNotNull(deserialized)
        
        // Verify world map data is preserved
        assertNotNull(deserialized.worldMapSave)
        assertEquals(3, deserialized.worldMapSave!!.levelStatuses.size)
        assertEquals(LevelStatus.WON, deserialized.worldMapSave!!.levelStatuses["level_001"])
        assertEquals(LevelStatus.UNLOCKED, deserialized.worldMapSave!!.levelStatuses["level_002"])
        assertEquals(LevelStatus.LOCKED, deserialized.worldMapSave!!.levelStatuses["level_003"])
    }
    
    @Test
    fun testConflictDetectionLogic() {
        // Create a saved world map
        val savedWorldMap = WorldMapSave(
            levelStatuses = mapOf(
                "level_001" to LevelStatus.WON,
                "level_002" to LevelStatus.UNLOCKED,
                "level_003" to LevelStatus.LOCKED
            )
        )
        
        // Create a different current world map
        val currentWorldMap = mapOf(
            "level_001" to LevelStatus.WON,  // Same
            "level_002" to LevelStatus.WON,   // Different (was UNLOCKED)
            "level_003" to LevelStatus.UNLOCKED  // Different (was LOCKED)
        )
        
        // Find differences
        val allLevelIds = (savedWorldMap.levelStatuses.keys + currentWorldMap.keys).toSet()
        val differences = allLevelIds.mapNotNull { levelId ->
            val savedStatus = savedWorldMap.levelStatuses[levelId]
            val currentStatus = currentWorldMap[levelId]
            
            if (savedStatus != currentStatus) {
                Triple(levelId, savedStatus, currentStatus)
            } else {
                null
            }
        }
        
        // Verify conflicts are detected
        assertEquals(2, differences.size)
        
        // Verify specific conflicts
        val level002Conflict = differences.find { it.first == "level_002" }
        assertNotNull(level002Conflict)
        assertEquals(LevelStatus.UNLOCKED, level002Conflict.second)  // Saved
        assertEquals(LevelStatus.WON, level002Conflict.third)        // Current
        
        val level003Conflict = differences.find { it.first == "level_003" }
        assertNotNull(level003Conflict)
        assertEquals(LevelStatus.LOCKED, level003Conflict.second)     // Saved
        assertEquals(LevelStatus.UNLOCKED, level003Conflict.third)    // Current
    }
    
    @Test
    fun testNoConflictWhenIdentical() {
        // Create identical world maps
        val savedWorldMap = WorldMapSave(
            levelStatuses = mapOf(
                "level_001" to LevelStatus.WON,
                "level_002" to LevelStatus.UNLOCKED,
                "level_003" to LevelStatus.LOCKED
            )
        )
        
        val currentWorldMap = mapOf(
            "level_001" to LevelStatus.WON,
            "level_002" to LevelStatus.UNLOCKED,
            "level_003" to LevelStatus.LOCKED
        )
        
        // Check for conflicts
        val hasConflict = savedWorldMap.levelStatuses != currentWorldMap
        
        // Verify no conflict
        assertEquals(false, hasConflict)
    }
    
    @Test
    fun testBackwardCompatibility() {
        // Simulate an old save without world map data
        val oldSaveJson = """{
  "id": "old_save",
  "timestamp": 1234567890,
  "levelId": 1,
  "levelName": "Test Level",
  "turnNumber": 5,
  "coins": 100,
  "healthPoints": 10,
  "phase": "PLAYER_TURN",
  "defenders": [],
  "attackers": [],
  "nextDefenderId": 1,
  "nextAttackerId": 1,
  "currentWaveIndex": 0,
  "spawnCounter": 0,
  "attackersToSpawn": [],
  "fieldEffects": [],
  "traps": [],
  "rafts": [],
  "nextRaftId": 1,
  "comment": null,
  "mapId": null
}"""
        
        // Deserialize old save
        val deserialized = SaveJsonSerializer.deserializeSavedGame(oldSaveJson)
        assertNotNull(deserialized)
        
        // Verify world map is null (backward compatibility)
        assertNull(deserialized.worldMapSave)
        
        // Verify other fields are correct
        assertEquals("old_save", deserialized.id)
        assertEquals(1, deserialized.levelId)
    }
}
