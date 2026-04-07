package de.egril.defender.save

import de.egril.defender.model.*
import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import androidx.compose.runtime.mutableStateOf
import kotlin.test.assertEquals
import androidx.compose.runtime.mutableStateOf
import kotlin.test.assertNotNull
import androidx.compose.runtime.mutableStateOf

/**
 * Tests for save/load data structures and serialization
 */
class SaveDataTest {
    
    @Test
    fun testWorldMapSaveSerialization() {
        val worldMapSave = WorldMapSave(
            levelStatuses = mapOf(
                "level_001" to LevelStatus.WON,
                "level_002" to LevelStatus.UNLOCKED,
                "level_003" to LevelStatus.LOCKED
            )
        )
        
        // Test serialization
        val json = SaveJsonSerializer.serializeWorldMapSave(worldMapSave)
        assertNotNull(json)
        
        // Test deserialization
        val deserialized = SaveJsonSerializer.deserializeWorldMapSave(json)
        assertNotNull(deserialized)
        assertEquals(3, deserialized.levelStatuses.size)
        assertEquals(LevelStatus.WON, deserialized.levelStatuses["level_001"])
        assertEquals(LevelStatus.UNLOCKED, deserialized.levelStatuses["level_002"])
        assertEquals(LevelStatus.LOCKED, deserialized.levelStatuses["level_003"])
    }
    
    @Test
    fun testSavedGameMetadataStructure() {
        val metadata = SaveGameMetadata(
            id = "save_123",
            timestamp = 1234567890L,
            levelId = 1,
            levelName = "Test Level",
            turnNumber = 5,
            coins = 150,
            healthPoints = 10,
            towerCount = 3,
            enemyCount = 10,
            defenderCounts = mapOf(DefenderType.SPIKE_TOWER to 2, DefenderType.BOW_TOWER to 1),
            attackerCounts = mapOf(AttackerType.GOBLIN to 5, AttackerType.ORK to 5),
            remainingSpawnCounts = mapOf(AttackerType.SKELETON to 10)
        )
        
        assertEquals("save_123", metadata.id)
        assertEquals(5, metadata.turnNumber)
        assertEquals(150, metadata.coins)
        assertEquals(3, metadata.towerCount)
        assertEquals(10, metadata.enemyCount)
        assertEquals(2, metadata.defenderCounts[DefenderType.SPIKE_TOWER])
        assertEquals(5, metadata.attackerCounts[AttackerType.GOBLIN])
        assertEquals(10, metadata.remainingSpawnCounts[AttackerType.SKELETON])
    }
    
    @Test
    fun testSavedGameMetadataWithBarricades() {
        val barricade = SavedBarricade(
            position = Position(3, 4),
            healthPoints = 50,
            defenderId = 1,
            id = 1,
            supportedTowerId = null
        )
        
        val metadata = SaveGameMetadata(
            id = "save_456",
            timestamp = 1234567890L,
            levelId = 2,
            levelName = "Test Level with Barricades",
            turnNumber = 10,
            coins = 200,
            healthPoints = 8,
            towerCount = 2,
            enemyCount = 5,
            defenderCounts = mapOf(DefenderType.SPIKE_TOWER to 2),
            attackerCounts = mapOf(AttackerType.GOBLIN to 5),
            remainingSpawnCounts = emptyMap(),
            barricadeCount = 1,
            barricadePositions = listOf(barricade)
        )
        
        assertEquals(1, metadata.barricadeCount)
        assertEquals(1, metadata.barricadePositions.size)
        assertEquals(Position(3, 4), metadata.barricadePositions[0].position)
        assertEquals(50, metadata.barricadePositions[0].healthPoints)
        assertEquals(1, metadata.barricadePositions[0].defenderId)
    }
    
    @Test
    fun testSavedDefenderStructure() {
        val defender = SavedDefender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = Position(5, 3),
            level = 2,
            buildTimeRemaining = 0,
            placedOnTurn = 1,
            actionsRemaining = 1
        )
        
        assertEquals(1, defender.id)
        assertEquals(DefenderType.SPIKE_TOWER, defender.type)
        assertEquals(2, defender.level)
        assertEquals(Position(5, 3), defender.position)
        assertEquals(1, defender.actionsRemaining)
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
                    placedOnTurn = 1,
                    actionsRemaining = 1
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
        assertEquals(1, defender.actionsRemaining)
        
        // Verify attacker data
        val attacker = deserialized.attackers[0]
        assertEquals(1, attacker.id)
        assertEquals(AttackerType.GOBLIN, attacker.type)
        assertEquals(Position(2, 1), attacker.position)
        assertEquals(20, attacker.currentHealth)
    }
    
    @Test
    fun testUpcomingSpawnsCalculation() {
        // Test the spawn plan filtering logic
        val waves = listOf(
            AttackerWave(
                attackers = List(12) { AttackerType.GOBLIN },
                spawnDelay = 2
            ),
            AttackerWave(
                attackers = List(12) { AttackerType.ORK },
                spawnDelay = 2
            )
        )
        
        val spawnPlan = generateSpawnPlan(waves)
        
        // At turn 0, all spawns should be in the future
        val upcomingAtTurn0 = spawnPlan.filter { it.spawnTurn > 0 }
        assertEquals(24, upcomingAtTurn0.size)
        
        // At turn 5, some goblins should have spawned
        val upcomingAtTurn5 = spawnPlan.filter { it.spawnTurn > 5 }
        // Should be less than total
        assert(upcomingAtTurn5.size < 24)
        
        // Count by type for turn 5
        val countsByType = upcomingAtTurn5.groupingBy { it.attackerType }.eachCount()
        // Should have some orks and possibly some goblins
        assert(countsByType.containsKey(AttackerType.ORK))
    }
    
    @Test
    fun testSavedGameWithComment() {
        val savedGame = SavedGame(
            id = "test_save_with_comment",
            timestamp = System.currentTimeMillis(),
            levelId = 1,
            levelName = "The First Wave",
            turnNumber = 3,
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
            comment = "Before final wave - good position"
        )
        
        // Test serialization
        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        assertNotNull(json)
        
        // Test deserialization
        val deserialized = SaveJsonSerializer.deserializeSavedGame(json)
        assertNotNull(deserialized)
        assertEquals("Before final wave - good position", deserialized.comment)
    }
    
    @Test
    fun testSavedGameWithoutComment() {
        val savedGame = SavedGame(
            id = "test_save_no_comment",
            timestamp = System.currentTimeMillis(),
            levelId = 1,
            levelName = "The First Wave",
            turnNumber = 3,
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
            comment = null
        )
        
        // Test serialization
        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        assertNotNull(json)
        
        // Test deserialization
        val deserialized = SaveJsonSerializer.deserializeSavedGame(json)
        assertNotNull(deserialized)
        assertEquals(null, deserialized.comment)
    }
    
    @Test
    fun testCommentWithSpecialCharacters() {
        val commentWithSpecialChars = "Quote: \"test\", Newline:\nNew line, Backslash: \\"
        val savedGame = SavedGame(
            id = "test_save_special",
            timestamp = System.currentTimeMillis(),
            levelId = 1,
            levelName = "Test",
            turnNumber = 1,
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
            comment = commentWithSpecialChars
        )
        
        // Test serialization
        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        assertNotNull(json)
        
        // Test deserialization
        val deserialized = SaveJsonSerializer.deserializeSavedGame(json)
        assertNotNull(deserialized)
        assertEquals(commentWithSpecialChars, deserialized.comment)
    }
    
    @Test
    fun testBackwardCompatibilityWithOldSaves() {
        // Test that old saves without actionsRemaining field can still be loaded
        val oldSaveJson = """{
  "id": "old_save",
  "timestamp": 1234567890,
  "levelId": 1,
  "levelName": "Test Level",
  "turnNumber": 5,
  "coins": 100,
  "healthPoints": 10,
  "phase": "PLAYER_TURN",
  "defenders": [
    {
      "id": 1,
      "type": "SPIKE_TOWER",
      "position": {"x": 5, "y": 3},
      "level": 2,
      "buildTimeRemaining": 0,
      "placedOnTurn": 1
    }
  ],
  "attackers": [],
  "nextDefenderId": 2,
  "nextAttackerId": 1,
  "currentWaveIndex": 0,
  "spawnCounter": 0,
  "attackersToSpawn": [],
  "fieldEffects": [],
  "traps": [],
  "comment": null
}"""
        
        // Should deserialize successfully with actionsRemaining defaulting to 0
        val deserialized = SaveJsonSerializer.deserializeSavedGame(oldSaveJson)
        assertNotNull(deserialized)
        assertEquals("old_save", deserialized.id)
        assertEquals(1, deserialized.defenders.size)
        val defender = deserialized.defenders[0]
        assertEquals(0, defender.actionsRemaining)  // Should default to 0 for old saves
    }
    
    @Test
    fun testBackwardCompatibilityWithoutMapId() {
        // Test that old saves without mapId field can still be loaded
        val oldSaveJson = """{
  "id": "old_save_no_map",
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
  "comment": null
}"""
        
        // Should deserialize successfully with mapId defaulting to null
        val deserialized = SaveJsonSerializer.deserializeSavedGame(oldSaveJson)
        assertNotNull(deserialized)
        assertEquals("old_save_no_map", deserialized.id)
        assertEquals(null, deserialized.mapId)  // Should default to null for old saves
    }
    
    @Test
    fun testSavedGameWithMapId() {
        val savedGame = SavedGame(
            id = "test_save_with_map",
            timestamp = System.currentTimeMillis(),
            levelId = 1,
            levelName = "The First Wave",
            turnNumber = 3,
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
            comment = null,
            mapId = "map_test_123"
        )
        
        // Test serialization
        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        assertNotNull(json)
        
        // Test deserialization
        val deserialized = SaveJsonSerializer.deserializeSavedGame(json)
        assertNotNull(deserialized)
        assertEquals("map_test_123", deserialized.mapId)
    }
    
    @Test
    fun testSavedGameWithWorldMapSave() {
        val worldMapSave = WorldMapSave(
            levelStatuses = mapOf(
                "level_001" to LevelStatus.WON,
                "level_002" to LevelStatus.UNLOCKED,
                "level_003" to LevelStatus.LOCKED
            )
        )
        
        val savedGame = SavedGame(
            id = "test_save_with_worldmap",
            timestamp = System.currentTimeMillis(),
            levelId = 1,
            levelName = "The First Wave",
            turnNumber = 3,
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
            comment = null,
            mapId = null,
            worldMapSave = worldMapSave
        )
        
        // Test serialization
        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        assertNotNull(json)
        
        // Test deserialization
        val deserialized = SaveJsonSerializer.deserializeSavedGame(json)
        assertNotNull(deserialized)
        assertNotNull(deserialized.worldMapSave)
        assertEquals(3, deserialized.worldMapSave!!.levelStatuses.size)
        assertEquals(LevelStatus.WON, deserialized.worldMapSave!!.levelStatuses["level_001"])
        assertEquals(LevelStatus.UNLOCKED, deserialized.worldMapSave!!.levelStatuses["level_002"])
        assertEquals(LevelStatus.LOCKED, deserialized.worldMapSave!!.levelStatuses["level_003"])
    }
    
    // ==================== Metadata wrapper tests ====================

    @Test
    fun testWorldMapSaveMetadataRoundTrip() {
        val worldMapSave = WorldMapSave(
            levelStatuses = mapOf(
                "level_001" to LevelStatus.WON,
                "level_002" to LevelStatus.UNLOCKED
            )
        )

        val json = SaveJsonSerializer.serializeWorldMapSave(worldMapSave)

        // Verify metadata present
        assert(json.contains("\"metadata\"")) { "Expected metadata in serialized output" }
        assert(json.contains("\"program\": \"Defender of Egril\"")) { "Expected program name in metadata" }
        assert(json.contains("\"type\": \"level_progress\"")) { "Expected type in metadata" }

        // Verify round-trip
        val deserialized = SaveJsonSerializer.deserializeWorldMapSave(json)
        assertNotNull(deserialized)
        assertEquals(2, deserialized.levelStatuses.size)
        assertEquals(LevelStatus.WON, deserialized.levelStatuses["level_001"])
        assertEquals(LevelStatus.UNLOCKED, deserialized.levelStatuses["level_002"])
    }

    @Test
    fun testWorldMapSaveBackwardCompatibility() {
        // Old format without metadata wrapper
        val oldJson = """{
  "levelStatuses": {
    "level_a": "WON",
    "level_b": "LOCKED"
  }
}"""
        val deserialized = SaveJsonSerializer.deserializeWorldMapSave(oldJson)
        assertNotNull(deserialized)
        assertEquals(2, deserialized.levelStatuses.size)
        assertEquals(LevelStatus.WON, deserialized.levelStatuses["level_a"])
        assertEquals(LevelStatus.LOCKED, deserialized.levelStatuses["level_b"])
    }

    @Test
    fun testSavedGameMetadataRoundTrip() {
        val savedGame = SavedGame(
            id = "meta_test_save",
            timestamp = 1234567890L,
            levelId = 1,
            levelName = "Test Level",
            turnNumber = 3,
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
            traps = emptyList()
        )

        val json = SaveJsonSerializer.serializeSavedGame(savedGame)

        // Verify metadata present
        assert(json.contains("\"metadata\"")) { "Expected metadata in serialized output" }
        assert(json.contains("\"program\": \"Defender of Egril\"")) { "Expected program name in metadata" }
        assert(json.contains("\"type\": \"savegame\"")) { "Expected type in metadata" }

        // Verify round-trip
        val deserialized = SaveJsonSerializer.deserializeSavedGame(json)
        assertNotNull(deserialized)
        assertEquals("meta_test_save", deserialized.id)
        assertEquals(1, deserialized.levelId)
        assertEquals("Test Level", deserialized.levelName)
        assertEquals(3, deserialized.turnNumber)
        assertEquals(100, deserialized.coins)
        assertEquals(10, deserialized.healthPoints)
        assertEquals(GamePhase.PLAYER_TURN, deserialized.phase)
    }

    @Test
    fun testPlayerProfilesMetadataRoundTrip() {
        val profiles = PlayerProfiles(
            profiles = listOf(
                PlayerProfile(
                    id = "player_1",
                    name = "Player One",
                    createdAt = 1000L,
                    lastPlayedAt = 2000L,
                    achievements = emptyList()
                )
            ),
            lastUsedPlayerId = "player_1"
        )

        val json = SaveJsonSerializer.serializePlayerProfiles(profiles)

        // Verify metadata present
        assert(json.contains("\"metadata\"")) { "Expected metadata in serialized output" }
        assert(json.contains("\"program\": \"Defender of Egril\"")) { "Expected program name in metadata" }
        assert(json.contains("\"type\": \"players\"")) { "Expected type in metadata" }

        // Verify round-trip
        val deserialized = SaveJsonSerializer.deserializePlayerProfiles(json)
        assertNotNull(deserialized)
        assertEquals(1, deserialized.profiles.size)
        assertEquals("player_1", deserialized.profiles[0].id)
        assertEquals("Player One", deserialized.profiles[0].name)
        assertEquals(1000L, deserialized.profiles[0].createdAt)
        assertEquals(2000L, deserialized.profiles[0].lastPlayedAt)
        assertEquals("player_1", deserialized.lastUsedPlayerId)
    }

    @Test
    fun testPlayerProfilesBackwardCompatibility() {
        // Old format without metadata wrapper
        val oldJson = """{
  "profiles": [
    {
      "id": "old_player",
      "name": "Old Player",
      "createdAt": 500,
      "lastPlayedAt": 600,
      "achievements": []
    }
  ],
  "lastUsedPlayerId": "old_player"
}"""
        val deserialized = SaveJsonSerializer.deserializePlayerProfiles(oldJson)
        assertNotNull(deserialized)
        assertEquals(1, deserialized.profiles.size)
        assertEquals("old_player", deserialized.profiles[0].id)
        assertEquals("Old Player", deserialized.profiles[0].name)
        assertEquals("old_player", deserialized.lastUsedPlayerId)
    }
}
