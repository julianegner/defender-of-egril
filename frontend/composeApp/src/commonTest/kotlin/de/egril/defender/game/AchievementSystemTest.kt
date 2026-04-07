package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import de.egril.defender.save.PlayerProfile
import de.egril.defender.save.PlayerProfileStorage
import de.egril.defender.utils.currentTimeMillis
import kotlin.test.*

/**
 * Comprehensive tests for the achievement system
 */
class AchievementSystemTest {
    
    private lateinit var testPlayerId: String
    private lateinit var achievementManager: AchievementManager
    private var awardedAchievements = mutableListOf<Achievement>()
    
    @BeforeTest
    fun setup() {
        // Create a unique test player ID for each test
        testPlayerId = "test_player_${currentTimeMillis()}"
        
        // Create a new test player profile
        PlayerProfileStorage.createProfile("Test Player")
        testPlayerId = PlayerProfileStorage.getAllProfiles().profiles.last().id
        
        // Initialize achievement manager
        achievementManager = AchievementManager(testPlayerId)
        awardedAchievements.clear()
        
        achievementManager.onAchievementEarned = { achievement ->
            awardedAchievements.add(achievement)
        }
    }
    
    @AfterTest
    fun teardown() {
        // Clean up test player
        PlayerProfileStorage.deleteProfile(testPlayerId)
    }
    
    // Tower Achievement Tests
    
    @Test
    fun testBuildTowerAchievement() {
        achievementManager.onBuildTower()
        
        assertEquals(1, awardedAchievements.size, "Should award BUILD_TOWER achievement")
        assertEquals(AchievementId.BUILD_TOWER, awardedAchievements[0].id)
    }
    
    @Test
    fun testUpgradeTowerAchievement() {
        achievementManager.onUpgradeTower()
        
        assertEquals(1, awardedAchievements.size, "Should award UPGRADE_TOWER achievement")
        assertEquals(AchievementId.UPGRADE_TOWER, awardedAchievements[0].id)
    }
    
    @Test
    fun testSellTowerAchievement() {
        achievementManager.onSellTower()
        
        assertEquals(1, awardedAchievements.size, "Should award SELL_TOWER achievement")
        assertEquals(AchievementId.SELL_TOWER, awardedAchievements[0].id)
    }
    
    @Test
    fun testUndoTowerAchievement() {
        achievementManager.onUndoTower()
        
        assertEquals(1, awardedAchievements.size, "Should award UNDO_TOWER achievement")
        assertEquals(AchievementId.UNDO_TOWER, awardedAchievements[0].id)
    }
    
    @Test
    fun testBuildTenTowersAchievement() {
        // Build 9 towers - should not award
        repeat(9) {
            achievementManager.onBuildTower()
        }
        
        // Should have BUILD_TOWER but not BUILD_TEN_TOWERS yet
        val buildTowerCount = awardedAchievements.count { it.id == AchievementId.BUILD_TOWER }
        val buildTenCount = awardedAchievements.count { it.id == AchievementId.BUILD_TEN_TOWERS }
        assertEquals(1, buildTowerCount, "Should only award BUILD_TOWER once")
        assertEquals(0, buildTenCount, "Should not award BUILD_TEN_TOWERS yet")
        
        // Build 10th tower - should award both
        achievementManager.onBuildTower()
        
        val buildTenCountAfter = awardedAchievements.count { it.id == AchievementId.BUILD_TEN_TOWERS }
        assertEquals(1, buildTenCountAfter, "Should award BUILD_TEN_TOWERS after 10th tower")
    }
    
    // Raft Achievement Tests
    
    @Test
    fun testBuildRaftAchievement() {
        achievementManager.onBuildRaft()
        
        assertEquals(1, awardedAchievements.size, "Should award BUILD_RAFT achievement")
        assertEquals(AchievementId.BUILD_RAFT, awardedAchievements[0].id)
    }
    
    @Test
    fun testUpgradeRaftAchievement() {
        achievementManager.onUpgradeRaft()
        
        assertEquals(1, awardedAchievements.size, "Should award UPGRADE_RAFT achievement")
        assertEquals(AchievementId.UPGRADE_RAFT, awardedAchievements[0].id)
    }
    
    @Test
    fun testBuildTenRaftsAchievement() {
        repeat(10) {
            achievementManager.onBuildRaft()
        }
        
        val buildTenCount = awardedAchievements.count { it.id == AchievementId.BUILD_TEN_RAFTS }
        assertEquals(1, buildTenCount, "Should award BUILD_TEN_RAFTS after 10 rafts")
    }
    
    @Test
    fun testRaftLostToMapEdgeAchievement() {
        achievementManager.onRaftLostToMapEdge()
        
        assertEquals(1, awardedAchievements.size, "Should award LOSE_RAFT_MAP_EDGE achievement")
        assertEquals(AchievementId.LOSE_RAFT_MAP_EDGE, awardedAchievements[0].id)
    }
    
    @Test
    fun testRaftLostToMaelstromAchievement() {
        achievementManager.onRaftLostToMaelstrom()
        
        assertEquals(1, awardedAchievements.size, "Should award LOSE_RAFT_MAELSTROM achievement")
        assertEquals(AchievementId.LOSE_RAFT_MAELSTROM, awardedAchievements[0].id)
    }
    
    // Level Achievement Tests
    
    @Test
    fun testWinLevelAchievement() {
        achievementManager.startLevel(10)
        achievementManager.onWinLevel(10)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.WIN_LEVEL }, 
            "Should award WIN_LEVEL achievement")
    }
    
    @Test
    fun testLoseLevelAchievement() {
        achievementManager.startLevel(10)
        achievementManager.onLoseLevel()
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.LOSE_LEVEL },
            "Should award LOSE_LEVEL achievement")
    }
    
    @Test
    fun testFlawlessVictoryAchievement() {
        achievementManager.startLevel(10)
        achievementManager.onWinLevel(10)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.WIN_LEVEL_FULL_HP },
            "Should award WIN_LEVEL_FULL_HP achievement")
    }
    
    @Test
    fun testCloseCallAchievement() {
        achievementManager.startLevel(10)
        achievementManager.onWinLevel(1)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.WIN_LEVEL_ONE_HP },
            "Should award WIN_LEVEL_ONE_HP achievement")
    }
    
    @Test
    fun testCloseCallNotAwardedWithOneInitialHP() {
        // Should not award if starting HP was 1
        achievementManager.startLevel(1)
        achievementManager.onWinLevel(1)
        
        assertFalse(awardedAchievements.any { it.id == AchievementId.WIN_LEVEL_ONE_HP },
            "Should not award WIN_LEVEL_ONE_HP if started with 1 HP")
    }
    
    // Combat Achievement Tests
    
    @Test
    fun testKillOgreAchievement() {
        achievementManager.startTurn()
        achievementManager.onEnemyKilled(AttackerType.OGRE, 1)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.KILL_OGRE },
            "Should award KILL_OGRE achievement")
    }
    
    @Test
    fun testKillEvilWizardAchievement() {
        achievementManager.startTurn()
        achievementManager.onEnemyKilled(AttackerType.EVIL_WIZARD, 1)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.KILL_EVIL_WIZARD },
            "Should award KILL_EVIL_WIZARD achievement")
    }
    
    @Test
    fun testKillDemonAchievement() {
        achievementManager.startTurn()
        achievementManager.onEnemyKilled(AttackerType.BLUE_DEMON, 1)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.KILL_DEMON },
            "Should award KILL_DEMON achievement for Blue Demon")
        
        awardedAchievements.clear()
        achievementManager.onEnemyKilled(AttackerType.RED_DEMON, 1)
        
        // Should not award again (already earned)
        assertFalse(awardedAchievements.any { it.id == AchievementId.KILL_DEMON },
            "Should not award KILL_DEMON again")
    }
    
    @Test
    fun testKillWitchAchievement() {
        achievementManager.startTurn()
        achievementManager.onEnemyKilled(AttackerType.RED_WITCH, 1)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.KILL_WITCH },
            "Should award KILL_WITCH achievement for Red Witch")
    }
    
    @Test
    fun testKillDragonAchievement() {
        achievementManager.startTurn()
        achievementManager.onEnemyKilled(AttackerType.DRAGON, 1)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.KILL_DRAGON },
            "Should award KILL_DRAGON achievement")
    }
    
    @Test
    fun testKillTwoEnemiesSameTurnAchievement() {
        achievementManager.startTurn()
        achievementManager.onEnemyKilled(AttackerType.GOBLIN, 1)
        achievementManager.onEnemyKilled(AttackerType.GOBLIN, 1)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.KILL_TWO_ENEMIES_SAME_TURN },
            "Should award KILL_TWO_ENEMIES_SAME_TURN achievement")
    }
    
    @Test
    fun testKillTwoEnemiesSameAttackAchievement() {
        achievementManager.startTurn()
        achievementManager.onEnemyKilled(AttackerType.GOBLIN, 2)
        
        assertTrue(awardedAchievements.any { it.id == AchievementId.KILL_TWO_ENEMIES_SAME_ATTACK },
            "Should award KILL_TWO_ENEMIES_SAME_ATTACK achievement")
    }
    
    // Mining Achievement Tests
    
    @Test
    fun testDigFirstTimeAchievement() {
        achievementManager.onDigFirstTime()
        
        assertEquals(1, awardedAchievements.size, "Should award DIG_FIRST_TIME achievement")
        assertEquals(AchievementId.DIG_FIRST_TIME, awardedAchievements[0].id)
    }
    
    @Test
    fun testFindGoldAchievement() {
        achievementManager.onFindGold()
        
        assertEquals(1, awardedAchievements.size, "Should award FIND_GOLD achievement")
        assertEquals(AchievementId.FIND_GOLD, awardedAchievements[0].id)
    }
    
    @Test
    fun testFindDiamondAchievement() {
        achievementManager.onFindDiamond()
        
        assertEquals(1, awardedAchievements.size, "Should award FIND_DIAMOND achievement")
        assertEquals(AchievementId.FIND_DIAMOND, awardedAchievements[0].id)
    }
    
    // Dragon Achievement Tests
    
    @Test
    fun testSummonDragonAchievement() {
        achievementManager.onSummonDragon()
        
        assertEquals(1, awardedAchievements.size, "Should award SUMMON_DRAGON achievement")
        assertEquals(AchievementId.SUMMON_DRAGON, awardedAchievements[0].id)
    }
    
    @Test
    fun testIncreaseDragonLevelAchievement() {
        achievementManager.onIncreaseDragonLevel()
        
        assertEquals(1, awardedAchievements.size, "Should award INCREASE_DRAGON_LEVEL achievement")
        assertEquals(AchievementId.INCREASE_DRAGON_LEVEL, awardedAchievements[0].id)
    }
    
    @Test
    fun testReduceDragonLevelAchievement() {
        achievementManager.onReduceDragonLevel()
        
        assertEquals(1, awardedAchievements.size, "Should award REDUCE_DRAGON_LEVEL achievement")
        assertEquals(AchievementId.REDUCE_DRAGON_LEVEL, awardedAchievements[0].id)
    }
    
    // Barricade Achievement Tests
    
    @Test
    fun testBuildBarricadeAchievement() {
        achievementManager.onBuildBarricade()
        
        assertEquals(1, awardedAchievements.size, "Should award BUILD_BARRICADE achievement")
        assertEquals(AchievementId.BUILD_BARRICADE, awardedAchievements[0].id)
    }
    
    @Test
    fun testAddHealthBarricadeAchievement() {
        achievementManager.onAddHealthBarricade()
        
        assertEquals(1, awardedAchievements.size, "Should award ADD_HEALTH_BARRICADE achievement")
        assertEquals(AchievementId.ADD_HEALTH_BARRICADE, awardedAchievements[0].id)
    }
    
    // Achievement Persistence Tests
    
    @Test
    fun testAchievementPersistence() {
        achievementManager.onBuildTower()
        achievementManager.onBuildRaft()
        
        // Verify achievements were saved
        val profile = PlayerProfileStorage.getProfile(testPlayerId)
        assertNotNull(profile, "Profile should exist")
        assertEquals(2, profile.achievements.size, "Should have 2 achievements saved")
        
        assertTrue(profile.achievements.any { it.id == AchievementId.BUILD_TOWER },
            "Should have BUILD_TOWER achievement saved")
        assertTrue(profile.achievements.any { it.id == AchievementId.BUILD_RAFT },
            "Should have BUILD_RAFT achievement saved")
    }
    
    @Test
    fun testAchievementNotAwardedTwice() {
        achievementManager.onBuildTower()
        assertEquals(1, awardedAchievements.size, "Should award achievement once")
        
        // Try to award again
        awardedAchievements.clear()
        achievementManager.onBuildTower()
        assertEquals(0, awardedAchievements.size, "Should not award achievement twice")
    }
    
    @Test
    fun testAchievementTimestamp() {
        val beforeTime = currentTimeMillis()
        achievementManager.onBuildTower()
        val afterTime = currentTimeMillis()
        
        val achievement = awardedAchievements[0]
        assertTrue(achievement.earnedAt >= beforeTime, "Timestamp should be >= start time")
        assertTrue(achievement.earnedAt <= afterTime, "Timestamp should be <= end time")
    }
    
    // Integration Tests
    
    @Test
    fun testMultipleAchievementsInLevel() {
        achievementManager.startLevel(10)
        
        // Perform various actions
        achievementManager.onBuildTower()
        achievementManager.onBuildRaft()
        achievementManager.onDigFirstTime()
        achievementManager.startTurn()
        achievementManager.onEnemyKilled(AttackerType.OGRE, 1)
        achievementManager.onWinLevel(10)
        
        // Should have awarded multiple achievements
        assertTrue(awardedAchievements.size >= 5, 
            "Should award at least 5 achievements")
        
        // Verify all expected achievements
        assertTrue(awardedAchievements.any { it.id == AchievementId.BUILD_TOWER })
        assertTrue(awardedAchievements.any { it.id == AchievementId.BUILD_RAFT })
        assertTrue(awardedAchievements.any { it.id == AchievementId.DIG_FIRST_TIME })
        assertTrue(awardedAchievements.any { it.id == AchievementId.KILL_OGRE })
        assertTrue(awardedAchievements.any { it.id == AchievementId.WIN_LEVEL })
        assertTrue(awardedAchievements.any { it.id == AchievementId.WIN_LEVEL_FULL_HP })
    }
}
