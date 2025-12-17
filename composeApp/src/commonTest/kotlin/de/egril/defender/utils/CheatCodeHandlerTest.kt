package de.egril.defender.utils

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DigOutcome
import de.egril.defender.model.Level
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.Position
import de.egril.defender.model.WorldLevel
import de.egril.defender.model.AttackerWave
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CheatCodeHandler
 */
class CheatCodeHandlerTest {
    
    // Helper function to create a test level with common defaults
    private fun createTestLevel(
        id: Int = 1,
        name: String = "Test Level $id",
        editorLevelId: String? = null
    ): Level {
        return Level(
            id = id,
            name = name,
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = emptySet(),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            initialCoins = 100,
            healthPoints = 10,
            editorLevelId = editorLevelId
        )
    }
    
    @Test
    fun testCashCheatCode() {
        var coinsAdded = 0
        val (success, digOutcome) = CheatCodeHandler.applyCheatCode(
            code = "cash",
            addCoins = { amount -> coinsAdded = amount },
            setCoins = { },
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
            setCoins = { },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertTrue(success, "Mmmoney cheat code should be recognized")
        assertEquals(1000000, coinsAdded, "Mmmoney cheat should add 1000000 coins")
        assertEquals(null, digOutcome, "Mmmoney cheat should not return dig outcome")
    }
    
    @Test
    fun testEmptypocketCheatCode() {
        var coinsSet = -1
        val (success, digOutcome) = CheatCodeHandler.applyCheatCode(
            code = "emptypocket",
            addCoins = { },
            setCoins = { amount -> coinsSet = amount },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertTrue(success, "Emptypocket cheat code should be recognized")
        assertEquals(0, coinsSet, "Emptypocket cheat should set coins to 0")
        assertEquals(null, digOutcome, "Emptypocket cheat should not return dig outcome")
    }
    
    @Test
    fun testCheatCodeCaseInsensitive() {
        var coinsAdded = 0
        val (success1, _) = CheatCodeHandler.applyCheatCode(
            code = "CASH",
            addCoins = { amount -> coinsAdded = amount },
            setCoins = { },
            performMineDigWithOutcome = { null },
            spawnEnemy = { _, _ -> }
        )
        
        assertTrue(success1, "Uppercase cheat code should work")
        assertEquals(1000, coinsAdded)
        
        coinsAdded = 0
        val (success2, _) = CheatCodeHandler.applyCheatCode(
            code = "CaSh",
            addCoins = { amount -> coinsAdded = amount },
            setCoins = { },
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
                setCoins = { },
                performMineDigWithOutcome = { outcome -> 
                    requestedOutcome = outcome
                    outcome // Return the same outcome
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
                setCoins = { },
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
            setCoins = { },
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
            setCoins = { },
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
    fun testUnlockLevelByIndex() {
        // Create test levels with editor level IDs
        val level1 = createTestLevel(id = 1, name = "First Level", editorLevelId = "first_level")
        val level2 = createTestLevel(id = 2, name = "Second Level", editorLevelId = "second_level")
        val level3 = createTestLevel(id = 3, name = "Third Level", editorLevelId = "third_level")
        
        val worldLevels = listOf(
            WorldLevel(level1, LevelStatus.UNLOCKED),
            WorldLevel(level2, LevelStatus.LOCKED),
            WorldLevel(level3, LevelStatus.LOCKED)
        )
        
        // Test unlocking by 1-based index
        val testCases = listOf(
            Pair("unlock 2", "second_level"),
            Pair("unlock 3", "third_level"),
            Pair("unlock level 2", "second_level")
        )
        
        for ((code, expectedLevelId) in testCases) {
            var unlockedLevelId: String? = null
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { },
                unlockLevel = { levelId -> unlockedLevelId = levelId },
                worldLevels = worldLevels
            )
            
            assertTrue(success, "Cheat code '$code' should be recognized")
            assertEquals(expectedLevelId, unlockedLevelId, "Cheat code '$code' should unlock level $expectedLevelId")
        }
    }
    
    @Test
    fun testUnlockLevelByEditorLevelId() {
        // Create test levels with editor level IDs
        val level1 = createTestLevel(id = 1, name = "Dark Magic Rises", editorLevelId = "dark_magic_rises")
        val level2 = createTestLevel(id = 2, name = "Forest Battle", editorLevelId = "forest_battle")
        
        val worldLevels = listOf(
            WorldLevel(level1, LevelStatus.LOCKED),
            WorldLevel(level2, LevelStatus.LOCKED)
        )
        
        // Test unlocking by editor level ID (with various formats)
        val testCases = listOf(
            Pair("unlock dark_magic_rises", "dark_magic_rises"),
            Pair("unlock dark magic rises", "dark_magic_rises"),
            Pair("unlock level dark_magic_rises", "dark_magic_rises"),
            Pair("unlock level dark magic rises", "dark_magic_rises"),
            Pair("unlock forest_battle", "forest_battle"),
            Pair("unlock forest battle", "forest_battle")
        )
        
        for ((code, expectedLevelId) in testCases) {
            var unlockedLevelId: String? = null
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { },
                unlockLevel = { levelId -> unlockedLevelId = levelId },
                worldLevels = worldLevels
            )
            
            assertTrue(success, "Cheat code '$code' should be recognized")
            assertEquals(expectedLevelId, unlockedLevelId, "Cheat code '$code' should unlock level $expectedLevelId")
        }
    }
    
    @Test
    fun testUnlockLevelByInvalidIndex() {
        val level1 = createTestLevel(editorLevelId = "test_level")
        
        val worldLevels = listOf(WorldLevel(level1, LevelStatus.LOCKED))
        
        // Test invalid indices
        val testCases = listOf("unlock 0", "unlock 5", "unlock -1", "unlock 999")
        
        for (code in testCases) {
            var unlockCalled = false
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { },
                unlockLevel = { unlockCalled = true },
                worldLevels = worldLevels
            )
            
            assertFalse(success, "Cheat code '$code' with invalid index should not be recognized")
            assertFalse(unlockCalled, "Cheat code '$code' should not call unlockLevel")
        }
    }
    
    @Test
    fun testUnlockLevelByInvalidLevelId() {
        val level1 = createTestLevel(editorLevelId = "test_level")
        
        val worldLevels = listOf(WorldLevel(level1, LevelStatus.LOCKED))
        
        // Test invalid level IDs
        val testCases = listOf("unlock nonexistent_level", "unlock invalid level")
        
        for (code in testCases) {
            var unlockCalled = false
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { },
                unlockLevel = { unlockCalled = true },
                worldLevels = worldLevels
            )
            
            assertFalse(success, "Cheat code '$code' with invalid level ID should not be recognized")
            assertFalse(unlockCalled, "Cheat code '$code' should not call unlockLevel")
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
            targetPositions = listOf(Position(9, 3)),
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
    fun testUnlockSpecificLevel() {
        val level1 = createTestLevel(id = 1, name = "Test Level 1", editorLevelId = "level_1")
        val level2 = createTestLevel(id = 2, name = "Test Level 2", editorLevelId = "level_2")
        val level3 = createTestLevel(id = 3, name = "Test Level 3", editorLevelId = "level_3")
        
        val worldLevels = listOf(
            WorldLevel(level1, LevelStatus.WON),
            WorldLevel(level2, LevelStatus.LOCKED),
            WorldLevel(level3, LevelStatus.LOCKED)
        )
        
        // Unlock level 2 specifically
        val unlockedLevels = CheatCodeHandler.unlockLevel(worldLevels, "level_2")
        
        assertEquals(LevelStatus.WON, unlockedLevels[0].status, "WON status should remain")
        assertEquals(LevelStatus.UNLOCKED, unlockedLevels[1].status, "LOCKED status should change to UNLOCKED for level_2")
        assertEquals(LevelStatus.LOCKED, unlockedLevels[2].status, "LOCKED status should remain for level_3")
    }
    
    @Test
    fun testUnlockSpecificLevelThatDoesNotExist() {
        val level1 = createTestLevel(editorLevelId = "level_1")
        
        val worldLevels = listOf(WorldLevel(level1, LevelStatus.LOCKED))
        
        // Try to unlock a non-existent level
        val unlockedLevels = CheatCodeHandler.unlockLevel(worldLevels, "nonexistent_level")
        
        assertEquals(LevelStatus.LOCKED, unlockedLevels[0].status, "Status should remain unchanged")
    }
    
    @Test
    fun testUnlockSpecificLevelThatIsAlreadyUnlocked() {
        val level1 = createTestLevel(editorLevelId = "level_1")
        
        val worldLevels = listOf(WorldLevel(level1, LevelStatus.UNLOCKED))
        
        // Try to unlock a level that is already unlocked
        val unlockedLevels = CheatCodeHandler.unlockLevel(worldLevels, "level_1")
        
        assertEquals(LevelStatus.UNLOCKED, unlockedLevels[0].status, "UNLOCKED status should remain")
    }
    
    @Test
    fun testCheatCodeWithWhitespace() {
        var coinsAdded = 0
        val (success, _) = CheatCodeHandler.applyCheatCode(
            code = "  cash  ",
            addCoins = { amount -> coinsAdded = amount },
            setCoins = { },
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
            setCoins = { },
            performMineDigWithOutcome = { null }, // Simulate failure
            spawnEnemy = { _, _ -> }
        )
        
        assertFalse(success, "Dig cheat should fail when performMineDigWithOutcome returns null")
        assertEquals(null, digOutcome, "No dig outcome should be returned when mine dig fails")
    }
    
    @Test
    fun testLockAllCheatCode() {
        val testCases = listOf("lockall", "lock all")
        
        for (code in testCases) {
            var lockCalled = false
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { },
                lockAllLevels = { lockCalled = true }
            )
            
            assertTrue(success, "Cheat code '$code' should be recognized")
            assertTrue(lockCalled, "Cheat code '$code' should call lockAllLevels")
        }
    }
    
    @Test
    fun testLockLevelByIndex() {
        // Create test levels with editor level IDs
        val level1 = createTestLevel(id = 1, name = "First Level", editorLevelId = "first_level")
        val level2 = createTestLevel(id = 2, name = "Second Level", editorLevelId = "second_level")
        val level3 = createTestLevel(id = 3, name = "Third Level", editorLevelId = "third_level")
        
        val worldLevels = listOf(
            WorldLevel(level1, LevelStatus.UNLOCKED),
            WorldLevel(level2, LevelStatus.UNLOCKED),
            WorldLevel(level3, LevelStatus.UNLOCKED)
        )
        
        // Test locking by 1-based index
        val testCases = listOf(
            Pair("lock 2", "second_level"),
            Pair("lock 3", "third_level"),
            Pair("lock level 2", "second_level")
        )
        
        for ((code, expectedLevelId) in testCases) {
            var lockedLevelId: String? = null
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { },
                lockLevel = { levelId -> lockedLevelId = levelId },
                worldLevels = worldLevels
            )
            
            assertTrue(success, "Cheat code '$code' should be recognized")
            assertEquals(expectedLevelId, lockedLevelId, "Cheat code '$code' should lock level $expectedLevelId")
        }
    }
    
    @Test
    fun testLockLevelByEditorLevelId() {
        // Create test levels with editor level IDs
        val level1 = createTestLevel(id = 1, name = "Dark Magic Rises", editorLevelId = "dark_magic_rises")
        val level2 = createTestLevel(id = 2, name = "Forest Battle", editorLevelId = "forest_battle")
        
        val worldLevels = listOf(
            WorldLevel(level1, LevelStatus.UNLOCKED),
            WorldLevel(level2, LevelStatus.UNLOCKED)
        )
        
        // Test locking by editor level ID (with various formats)
        val testCases = listOf(
            Pair("lock dark_magic_rises", "dark_magic_rises"),
            Pair("lock dark magic rises", "dark_magic_rises"),
            Pair("lock level dark_magic_rises", "dark_magic_rises"),
            Pair("lock level dark magic rises", "dark_magic_rises"),
            Pair("lock forest_battle", "forest_battle"),
            Pair("lock forest battle", "forest_battle")
        )
        
        for ((code, expectedLevelId) in testCases) {
            var lockedLevelId: String? = null
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { },
                lockLevel = { levelId -> lockedLevelId = levelId },
                worldLevels = worldLevels
            )
            
            assertTrue(success, "Cheat code '$code' should be recognized")
            assertEquals(expectedLevelId, lockedLevelId, "Cheat code '$code' should lock level $expectedLevelId")
        }
    }
    
    @Test
    fun testLockLevelByInvalidIndex() {
        val level1 = createTestLevel(editorLevelId = "test_level")
        
        val worldLevels = listOf(WorldLevel(level1, LevelStatus.UNLOCKED))
        
        // Test invalid indices
        val testCases = listOf("lock 0", "lock 5", "lock -1", "lock 999")
        
        for (code in testCases) {
            var lockCalled = false
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { },
                lockLevel = { lockCalled = true },
                worldLevels = worldLevels
            )
            
            assertFalse(success, "Cheat code '$code' with invalid index should not be recognized")
            assertFalse(lockCalled, "Cheat code '$code' should not call lockLevel")
        }
    }
    
    @Test
    fun testLockLevelByInvalidLevelId() {
        val level1 = createTestLevel(editorLevelId = "test_level")
        
        val worldLevels = listOf(WorldLevel(level1, LevelStatus.UNLOCKED))
        
        // Test invalid level IDs
        val testCases = listOf("lock nonexistent_level", "lock invalid level")
        
        for (code in testCases) {
            var lockCalled = false
            val success = CheatCodeHandler.applyWorldMapCheatCode(
                code = code,
                unlockAllLevels = { },
                lockLevel = { lockCalled = true },
                worldLevels = worldLevels
            )
            
            assertFalse(success, "Cheat code '$code' with invalid level ID should not be recognized")
            assertFalse(lockCalled, "Cheat code '$code' should not call lockLevel")
        }
    }
    
    @Test
    fun testLockAllLevelsLocksOnlyLevelsWithPrerequisites() {
        // Create test levels - entry levels (no prerequisites) should NOT be locked
        val entryLevel1 = createTestLevel(id = 1, name = "Entry 1", editorLevelId = "entry_1")
        val entryLevel2 = createTestLevel(id = 2, name = "Entry 2", editorLevelId = "entry_2")
        
        val worldLevels = listOf(
            WorldLevel(entryLevel1, LevelStatus.UNLOCKED),
            WorldLevel(entryLevel2, LevelStatus.WON)
        )
        
        // Lock all - should NOT lock levels without prerequisites
        val lockedLevels = CheatCodeHandler.lockAllLevels(worldLevels)
        
        // Both entry levels should remain unchanged (not locked)
        assertEquals(LevelStatus.UNLOCKED, lockedLevels[0].status, "Entry level 1 should NOT be locked (no prerequisites)")
        assertEquals(LevelStatus.WON, lockedLevels[1].status, "Entry level 2 (WON) should NOT be locked (no prerequisites)")
    }
    
    @Test
    fun testLockLevelOnlyLocksLevelWithPrerequisites() {
        // Create entry level (no prerequisites) - should NOT be locked
        val entryLevel = createTestLevel(id = 1, name = "Entry Level", editorLevelId = "entry_level")
        
        val worldLevels = listOf(
            WorldLevel(entryLevel, LevelStatus.UNLOCKED)
        )
        
        // Try to lock entry level - should NOT lock because it has no prerequisites
        val lockedLevels = CheatCodeHandler.lockLevel(worldLevels, "entry_level")
        
        // Entry level should remain unlocked
        assertEquals(LevelStatus.UNLOCKED, lockedLevels[0].status, "Entry level should NOT be locked (no prerequisites)")
    }
}
