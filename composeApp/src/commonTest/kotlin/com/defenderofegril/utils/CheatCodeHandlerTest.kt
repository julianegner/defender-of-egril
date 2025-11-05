package com.defenderofegril.utils

import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DigOutcome
import com.defenderofegril.model.Level
import com.defenderofegril.model.LevelStatus
import com.defenderofegril.model.Position
import com.defenderofegril.model.WorldLevel
import com.defenderofegril.model.AttackerWave
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CheatCodeHandler
 */
class CheatCodeHandlerTest {
    
    @Test
    fun testCashCheatCode() {
        var coinsAdded = 0
        val (success, digOutcome) = CheatCodeHandler.applyCheatCode(
            code = "cash",
            addCoins = { amount -> coinsAdded = amount },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertTrue(success, "Cash cheat code should be recognized")
        assertEquals(1000, coinsAdded, "Cash cheat should add 1000 coins")
        assertEquals(null, digOutcome, "Cash cheat should not return dig outcome")
    }
    
    @Test
    fun testMmmoneyCheatCode() {
        var coinsAdded = 0
        val (success, digOutcome) = CheatCodeHandler.applyCheatCode(
            code = "mmmoney",
            addCoins = { amount -> coinsAdded = amount },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertTrue(success, "Mmmoney cheat code should be recognized")
        assertEquals(1000000, coinsAdded, "Mmmoney cheat should add 1000000 coins")
        assertEquals(null, digOutcome, "Mmmoney cheat should not return dig outcome")
    }
    
    @Test
    fun testCheatCodeCaseInsensitive() {
        var coinsAdded = 0
        val (success1, _) = CheatCodeHandler.applyCheatCode(
            code = "CASH",
            addCoins = { amount -> coinsAdded = amount },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertTrue(success1, "Uppercase cheat code should work")
        assertEquals(1000, coinsAdded)
        
        coinsAdded = 0
        val (success2, _) = CheatCodeHandler.applyCheatCode(
            code = "CaSh",
            addCoins = { amount -> coinsAdded = amount },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertTrue(success2, "Mixed case cheat code should work")
        assertEquals(1000, coinsAdded)
    }
    
    @Test
    fun testDigCheatCodes() {
        val testCases = listOf(
            Pair("dig brass", DigOutcome.BRASS),
            Pair("dig silver", DigOutcome.SILVER),
            Pair("dig gold", DigOutcome.GOLD),
            Pair("dig gems", DigOutcome.GEMS),
            Pair("dig gem", DigOutcome.GEMS),
            Pair("dig diamond", DigOutcome.DIAMOND),
            Pair("dig dragon", DigOutcome.DRAGON),
            Pair("dragon", DigOutcome.DRAGON),
            Pair("dig nothing", DigOutcome.NOTHING),
            Pair("dig rubble", DigOutcome.NOTHING)
        )
        
        for ((code, expectedOutcome) in testCases) {
            var requestedOutcome: DigOutcome? = null
            val (success, digOutcome) = CheatCodeHandler.applyCheatCode(
                code = code,
                addCoins = { },
                performMineDigWithOutcome = { outcome -> 
                    requestedOutcome = outcome
                    outcome // Return the outcome back
                },
                spawnEnemy = { _, _ -> }
            )
            
            assertTrue(success, "Cheat code '$code' should be recognized")
            assertEquals(expectedOutcome, requestedOutcome, "Cheat code '$code' should request $expectedOutcome")
            assertEquals(expectedOutcome, digOutcome, "Cheat code '$code' should return $expectedOutcome")
        }
    }
    
    @Test
    fun testSpawnCheatCodes() {
        val testCases = listOf(
            Triple("spawn goblin", AttackerType.GOBLIN, 1),
            Triple("spawn ork", AttackerType.ORK, 1),
            Triple("spawn orc", AttackerType.ORK, 1),
            Triple("spawn ogre", AttackerType.OGRE, 1),
            Triple("spawn skeleton", AttackerType.SKELETON, 1),
            Triple("spawn wizard", AttackerType.EVIL_WIZARD, 1),
            Triple("spawn evil_wizard", AttackerType.EVIL_WIZARD, 1),
            Triple("spawn evilwizard", AttackerType.EVIL_WIZARD, 1),
            Triple("spawn witch", AttackerType.WITCH, 1),
            Triple("spawn goblin 5", AttackerType.GOBLIN, 5),
            Triple("spawn ork 10", AttackerType.ORK, 10)
        )
        
        for ((code, expectedType, expectedLevel) in testCases) {
            var spawnedType: AttackerType? = null
            var spawnedLevel: Int? = null
            val (success, _) = CheatCodeHandler.applyCheatCode(
                code = code,
                addCoins = { },
                performMineDigWithOutcome = { null },
                spawnEnemy = { type, level -> 
                    spawnedType = type
                    spawnedLevel = level
                }
            )
            
            assertTrue(success, "Cheat code '$code' should be recognized")
            assertEquals(expectedType, spawnedType, "Cheat code '$code' should spawn $expectedType")
            assertEquals(expectedLevel, spawnedLevel, "Cheat code '$code' should spawn at level $expectedLevel")
        }
    }
    
    @Test
    fun testInvalidCheatCode() {
        val (success, _) = CheatCodeHandler.applyCheatCode(
            code = "invalid",
            addCoins = { },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertFalse(success, "Invalid cheat code should not be recognized")
    }
    
    @Test
    fun testInvalidSpawnType() {
        val (success, _) = CheatCodeHandler.applyCheatCode(
            code = "spawn invalid",
            addCoins = { },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertFalse(success, "Invalid spawn type should not be recognized")
    }
    
    @Test
    fun testUnlockCheatCode() {
        val testCases = listOf("unlock", "unlockall", "unlock all")
        
        for (code in testCases) {
            var unlockCalled = false
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { unlockCalled = true }
            )
            
            assertTrue(success, "Cheat code '$code' should be recognized")
            assertTrue(unlockCalled, "Cheat code '$code' should call unlockAllLevels")
        }
    }
    
    @Test
    fun testInvalidWorldMapCheatCode() {
        var unlockCalled = false
        val success = CheatCodeHandler.applyWorldMapCheatCode(
            code = "invalid",
            unlockAllLevels = { unlockCalled = true }
        )
        
        assertFalse(success, "Invalid world map cheat code should not be recognized")
        assertFalse(unlockCalled, "Invalid cheat code should not call unlockAllLevels")
    }
    
    @Test
    fun testUnlockAllLevels() {
        // Create a test level
        val level1 = Level(
            id = 1,
            name = "Test Level 1",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPosition = Position(9, 3),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = emptySet(),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val level2 = level1.copy(id = 2, name = "Test Level 2")
        val level3 = level1.copy(id = 3, name = "Test Level 3")
        
        val worldLevels = listOf(
            WorldLevel(level1, LevelStatus.WON),
            WorldLevel(level2, LevelStatus.UNLOCKED),
            WorldLevel(level3, LevelStatus.LOCKED)
        )
        
        val unlockedLevels = CheatCodeHandler.unlockAllLevels(worldLevels)
        
        assertEquals(LevelStatus.WON, unlockedLevels[0].status, "WON status should remain")
        assertEquals(LevelStatus.UNLOCKED, unlockedLevels[1].status, "UNLOCKED status should remain")
        assertEquals(LevelStatus.UNLOCKED, unlockedLevels[2].status, "LOCKED status should change to UNLOCKED")
    }
    
    @Test
    fun testCheatCodeWithWhitespace() {
        var coinsAdded = 0
        val (success, _) = CheatCodeHandler.applyCheatCode(
            code = "  cash  ",
            addCoins = { amount -> coinsAdded = amount },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertTrue(success, "Cheat code with whitespace should be trimmed and recognized")
        assertEquals(1000, coinsAdded)
    }
    
    @Test
    fun testDigCheatWhenMineDigFails() {
        val (success, digOutcome) = CheatCodeHandler.applyCheatCode(
            code = "dig gold",
            addCoins = { },
            performMineDigWithOutcome = { null }, // Simulate failure
            spawnEnemy = { _, _ -> }
        )
        
        assertFalse(success, "Dig cheat should fail when performMineDigWithOutcome returns null")
        assertEquals(null, digOutcome, "No dig outcome should be returned when mine dig fails")
    }
}
