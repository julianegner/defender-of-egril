package de.egril.defender.game

import de.egril.defender.model.*
import de.egril.defender.save.PlayerProfileStorage
import de.egril.defender.utils.currentTimeMillis

/**
 * Manages achievement tracking and awarding
 * Integrates with PlayerProfileStorage to persist achievements
 */
class AchievementManager(private val playerId: String) {
    
    // Callback when an achievement is earned
    var onAchievementEarned: ((Achievement) -> Unit)? = null
    
    // Track session-specific counts (reset when level restarts)
    private var currentLevelKillCount = 0
    private var currentTurnKillCount = 0
    private var lastAttackKillCount = 0
    private var towersBuiltThisLevel = 0
    private var raftsBuiltThisLevel = 0
    private var initialHealthPoints = 0
    
    /**
     * Initialize tracking for a new level
     */
    fun startLevel(initialHP: Int) {
        currentLevelKillCount = 0
        currentTurnKillCount = 0
        lastAttackKillCount = 0
        towersBuiltThisLevel = 0
        raftsBuiltThisLevel = 0
        initialHealthPoints = initialHP
    }
    
    /**
     * Reset turn-specific counters at the start of a turn
     */
    fun startTurn() {
        currentTurnKillCount = 0
    }
    
    /**
     * Check and award an achievement if not already earned
     */
    private fun checkAndAward(achievementId: AchievementId) {
        // Check if already earned
        if (PlayerProfileStorage.hasAchievement(playerId, achievementId)) {
            return
        }
        
        // Award the achievement
        val achievement = Achievement(
            id = achievementId,
            earnedAt = currentTimeMillis()
        )
        
        if (PlayerProfileStorage.addAchievement(playerId, achievement)) {
            onAchievementEarned?.invoke(achievement)
        }
    }
    
    // Tower-related achievements
    fun onBuildTower() {
        checkAndAward(AchievementId.BUILD_TOWER)
        towersBuiltThisLevel++
        if (towersBuiltThisLevel >= 10) {
            checkAndAward(AchievementId.BUILD_TEN_TOWERS)
        }
    }
    
    fun onUpgradeTower() {
        checkAndAward(AchievementId.UPGRADE_TOWER)
    }
    
    fun onSellTower() {
        checkAndAward(AchievementId.SELL_TOWER)
    }
    
    fun onUndoTower() {
        checkAndAward(AchievementId.UNDO_TOWER)
    }
    
    // Raft-related achievements
    fun onBuildRaft() {
        checkAndAward(AchievementId.BUILD_RAFT)
        raftsBuiltThisLevel++
        if (raftsBuiltThisLevel >= 10) {
            checkAndAward(AchievementId.BUILD_TEN_RAFTS)
        }
    }
    
    fun onUpgradeRaft() {
        checkAndAward(AchievementId.UPGRADE_RAFT)
    }
    
    fun onSellRaft() {
        checkAndAward(AchievementId.SELL_RAFT)
    }
    
    fun onUndoRaft() {
        checkAndAward(AchievementId.UNDO_RAFT)
    }
    
    fun onRaftLostToMapEdge() {
        checkAndAward(AchievementId.LOSE_RAFT_MAP_EDGE)
    }
    
    fun onRaftLostToMaelstrom() {
        checkAndAward(AchievementId.LOSE_RAFT_MAELSTROM)
    }
    
    // Level completion achievements
    fun onWinLevel(currentHP: Int) {
        checkAndAward(AchievementId.WIN_LEVEL)
        
        // Check for full HP victory
        if (currentHP == initialHealthPoints && initialHealthPoints > 0) {
            checkAndAward(AchievementId.WIN_LEVEL_FULL_HP)
        }
        
        // Check for 1 HP victory
        if (currentHP == 1 && initialHealthPoints > 1) {
            checkAndAward(AchievementId.WIN_LEVEL_ONE_HP)
        }
    }
    
    fun onLoseLevel() {
        checkAndAward(AchievementId.LOSE_LEVEL)
    }
    
    // Combat-related achievements
    fun onEnemyKilled(enemyType: AttackerType, killsInThisAttack: Int) {
        currentTurnKillCount++
        lastAttackKillCount = killsInThisAttack
        
        // Check for multiple kills
        if (currentTurnKillCount >= 2) {
            checkAndAward(AchievementId.KILL_TWO_ENEMIES_SAME_TURN)
        }
        
        if (killsInThisAttack >= 2) {
            checkAndAward(AchievementId.KILL_TWO_ENEMIES_SAME_ATTACK)
        }
        
        // Check for specific enemy type kills
        when (enemyType) {
            AttackerType.OGRE -> checkAndAward(AchievementId.KILL_OGRE)
            AttackerType.EVIL_WIZARD -> checkAndAward(AchievementId.KILL_EVIL_WIZARD)
            AttackerType.BLUE_DEMON, AttackerType.RED_DEMON -> checkAndAward(AchievementId.KILL_DEMON)
            AttackerType.EWHAD -> checkAndAward(AchievementId.KILL_EWHAD)
            AttackerType.RED_WITCH, AttackerType.GREEN_WITCH -> checkAndAward(AchievementId.KILL_WITCH)
            AttackerType.DRAGON -> checkAndAward(AchievementId.KILL_DRAGON)
            else -> {} // No special achievement for other enemy types
        }
    }
    
    // Mining achievements
    fun onDigFirstTime() {
        checkAndAward(AchievementId.DIG_FIRST_TIME)
    }
    
    fun onFindGold() {
        checkAndAward(AchievementId.FIND_GOLD)
    }
    
    fun onFindDiamond() {
        checkAndAward(AchievementId.FIND_DIAMOND)
    }
    
    // Dragon achievements
    fun onSummonDragon() {
        checkAndAward(AchievementId.SUMMON_DRAGON)
    }
    
    fun onReduceDragonLevel() {
        checkAndAward(AchievementId.REDUCE_DRAGON_LEVEL)
    }
    
    fun onIncreaseDragonLevel() {
        checkAndAward(AchievementId.INCREASE_DRAGON_LEVEL)
    }
    
    // Bridge and barricade achievements
    fun onDestroyBridge() {
        checkAndAward(AchievementId.DESTROY_BRIDGE)
    }
    
    fun onBuildBarricade() {
        checkAndAward(AchievementId.BUILD_BARRICADE)
    }
    
    fun onAddHealthBarricade() {
        checkAndAward(AchievementId.ADD_HEALTH_BARRICADE)
    }
}
