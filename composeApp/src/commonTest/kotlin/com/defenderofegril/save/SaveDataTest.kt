package com.defenderofegril.save

import com.defenderofegril.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for save/load data structures and serialization
 */
class SaveDataTest {
    
    @Test
    fun testWorldMapSaveSerialization() {
        val worldMapSave = WorldMapSave(
            levelStatuses = mapOf(
                1 to LevelStatus.WON,
                2 to LevelStatus.UNLOCKED,
                3 to LevelStatus.LOCKED
            )
        )
        
        // Test serialization
        val json = SaveJsonSerializer.serializeWorldMapSave(worldMapSave)
        assertNotNull(json)
        
        // Test deserialization
        val deserialized = SaveJsonSerializer.deserializeWorldMapSave(json)
        assertNotNull(deserialized)
        assertEquals(3, deserialized.levelStatuses.size)
        assertEquals(LevelStatus.WON, deserialized.levelStatuses[1])
        assertEquals(LevelStatus.UNLOCKED, deserialized.levelStatuses[2])
        assertEquals(LevelStatus.LOCKED, deserialized.levelStatuses[3])
    }
    
    @Test
    fun testSavedGameMetadataStructure() {
        val metadata = SaveGameMetadata(
            id = "save_123",
            timestamp = 1234567890L,
            levelId = 1,
            levelName = "Test Level",
            turnNumber = 5,
            towerCount = 3,
            enemyCount = 10
        )
        
        assertEquals("save_123", metadata.id)
        assertEquals(5, metadata.turnNumber)
        assertEquals(3, metadata.towerCount)
        assertEquals(10, metadata.enemyCount)
    }
    
    @Test
    fun testSavedDefenderStructure() {
        val defender = SavedDefender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = Position(5, 3),
            level = 2,
            buildTimeRemaining = 0,
            placedOnTurn = 1
        )
        
        assertEquals(1, defender.id)
        assertEquals(DefenderType.SPIKE_TOWER, defender.type)
        assertEquals(2, defender.level)
        assertEquals(Position(5, 3), defender.position)
    }
    
    @Test
    fun testSavedAttackerStructure() {
        val attacker = SavedAttacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = Position(10, 4),
            level = 1,
            currentHealth = 15,
            isDefeated = false
        )
        
        assertEquals(1, attacker.id)
        assertEquals(AttackerType.GOBLIN, attacker.type)
        assertEquals(15, attacker.currentHealth)
        assertEquals(false, attacker.isDefeated)
    }
    
    @Test
    fun testSavedGameSerialization() {
        val savedGame = SavedGame(
            id = "test_save",
            timestamp = System.currentTimeMillis(),
            levelId = 1,
            levelName = "The First Wave",
            turnNumber = 3,
            coins = 100,
            healthPoints = 10,
            phase = GamePhase.PLAYER_TURN,
            defenders = listOf(
                SavedDefender(
                    id = 1,
                    type = DefenderType.SPIKE_TOWER,
                    position = Position(5, 3),
                    level = 1,
                    buildTimeRemaining = 0,
                    placedOnTurn = 1
                )
            ),
            attackers = listOf(
                SavedAttacker(
                    id = 1,
                    type = AttackerType.GOBLIN,
                    position = Position(2, 1),
                    level = 1,
                    currentHealth = 20,
                    isDefeated = false
                )
            ),
            nextDefenderId = 2,
            nextAttackerId = 2,
            currentWaveIndex = 0,
            spawnCounter = 0,
            attackersToSpawn = emptyList(),
            fieldEffects = emptyList(),
            traps = emptyList()
        )
        
        // Test serialization
        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        assertNotNull(json)
        
        // Test deserialization
        val deserialized = SaveJsonSerializer.deserializeSavedGame(json)
        assertNotNull(deserialized)
        assertEquals("test_save", deserialized.id)
        assertEquals(1, deserialized.levelId)
        assertEquals("The First Wave", deserialized.levelName)
        assertEquals(3, deserialized.turnNumber)
        assertEquals(100, deserialized.coins)
        assertEquals(10, deserialized.healthPoints)
        assertEquals(GamePhase.PLAYER_TURN, deserialized.phase)
        assertEquals(1, deserialized.defenders.size)
        assertEquals(1, deserialized.attackers.size)
        
        // Verify defender data
        val defender = deserialized.defenders[0]
        assertEquals(1, defender.id)
        assertEquals(DefenderType.SPIKE_TOWER, defender.type)
        assertEquals(Position(5, 3), defender.position)
        
        // Verify attacker data
        val attacker = deserialized.attackers[0]
        assertEquals(1, attacker.id)
        assertEquals(AttackerType.GOBLIN, attacker.type)
        assertEquals(Position(2, 1), attacker.position)
        assertEquals(20, attacker.currentHealth)
    }
}
