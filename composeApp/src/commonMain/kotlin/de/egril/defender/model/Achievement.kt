package de.egril.defender.model

/**
 * Unique identifier for each achievement
 * Used for persistence and tracking
 */
enum class AchievementId {
    // Tower achievements
    BUILD_TOWER,
    UPGRADE_TOWER,
    SELL_TOWER,
    UNDO_TOWER,
    
    // Raft achievements
    BUILD_RAFT,
    UPGRADE_RAFT,
    SELL_RAFT,
    UNDO_RAFT,
    LOSE_RAFT_MAP_EDGE,
    LOSE_RAFT_MAELSTROM,
    
    // Level achievements
    WIN_LEVEL,
    LOSE_LEVEL,
    WIN_LEVEL_FULL_HP,
    WIN_LEVEL_ONE_HP,
    
    // Combat achievements
    KILL_TWO_ENEMIES_SAME_TURN,
    KILL_TWO_ENEMIES_SAME_ATTACK,
    KILL_OGRE,
    KILL_EVIL_WIZARD,
    KILL_DEMON,
    KILL_EWHAD,
    KILL_WITCH,
    
    // Mining achievements
    DIG_FIRST_TIME,
    FIND_GOLD,
    FIND_DIAMOND,
    
    // Dragon achievements
    SUMMON_DRAGON,
    REDUCE_DRAGON_LEVEL,
    INCREASE_DRAGON_LEVEL,
    KILL_DRAGON,
    
    // Building achievements
    BUILD_TEN_TOWERS,
    BUILD_TEN_RAFTS,
    
    // Bridge and barricade achievements
    DESTROY_BRIDGE,
    BUILD_BARRICADE,
    ADD_HEALTH_BARRICADE
}

/**
 * Represents an achievement that a player can earn
 */
data class Achievement(
    val id: AchievementId,
    val earnedAt: Long  // Timestamp when the achievement was earned
)

/**
 * Achievement definitions with metadata
 * This defines the static information about each achievement
 */
object AchievementDefinitions {
    
    data class AchievementInfo(
        val id: AchievementId,
        val nameKey: String,  // String resource key for the achievement name
        val descriptionKey: String  // String resource key for the achievement description
    )
    
    /**
     * All available achievements with their metadata
     */
    val allAchievements = listOf(
        // Tower achievements
        AchievementInfo(AchievementId.BUILD_TOWER, "achievement_build_tower_name", "achievement_build_tower_desc"),
        AchievementInfo(AchievementId.UPGRADE_TOWER, "achievement_upgrade_tower_name", "achievement_upgrade_tower_desc"),
        AchievementInfo(AchievementId.SELL_TOWER, "achievement_sell_tower_name", "achievement_sell_tower_desc"),
        AchievementInfo(AchievementId.UNDO_TOWER, "achievement_undo_tower_name", "achievement_undo_tower_desc"),
        
        // Raft achievements
        AchievementInfo(AchievementId.BUILD_RAFT, "achievement_build_raft_name", "achievement_build_raft_desc"),
        AchievementInfo(AchievementId.UPGRADE_RAFT, "achievement_upgrade_raft_name", "achievement_upgrade_raft_desc"),
        AchievementInfo(AchievementId.SELL_RAFT, "achievement_sell_raft_name", "achievement_sell_raft_desc"),
        AchievementInfo(AchievementId.UNDO_RAFT, "achievement_undo_raft_name", "achievement_undo_raft_desc"),
        AchievementInfo(AchievementId.LOSE_RAFT_MAP_EDGE, "achievement_lose_raft_map_edge_name", "achievement_lose_raft_map_edge_desc"),
        AchievementInfo(AchievementId.LOSE_RAFT_MAELSTROM, "achievement_lose_raft_maelstrom_name", "achievement_lose_raft_maelstrom_desc"),
        
        // Level achievements
        AchievementInfo(AchievementId.WIN_LEVEL, "achievement_win_level_name", "achievement_win_level_desc"),
        AchievementInfo(AchievementId.LOSE_LEVEL, "achievement_lose_level_name", "achievement_lose_level_desc"),
        AchievementInfo(AchievementId.WIN_LEVEL_FULL_HP, "achievement_win_level_full_hp_name", "achievement_win_level_full_hp_desc"),
        AchievementInfo(AchievementId.WIN_LEVEL_ONE_HP, "achievement_win_level_one_hp_name", "achievement_win_level_one_hp_desc"),
        
        // Combat achievements
        AchievementInfo(AchievementId.KILL_TWO_ENEMIES_SAME_TURN, "achievement_kill_two_enemies_same_turn_name", "achievement_kill_two_enemies_same_turn_desc"),
        AchievementInfo(AchievementId.KILL_TWO_ENEMIES_SAME_ATTACK, "achievement_kill_two_enemies_same_attack_name", "achievement_kill_two_enemies_same_attack_desc"),
        AchievementInfo(AchievementId.KILL_OGRE, "achievement_kill_ogre_name", "achievement_kill_ogre_desc"),
        AchievementInfo(AchievementId.KILL_EVIL_WIZARD, "achievement_kill_evil_wizard_name", "achievement_kill_evil_wizard_desc"),
        AchievementInfo(AchievementId.KILL_DEMON, "achievement_kill_demon_name", "achievement_kill_demon_desc"),
        AchievementInfo(AchievementId.KILL_EWHAD, "achievement_kill_ewhad_name", "achievement_kill_ewhad_desc"),
        AchievementInfo(AchievementId.KILL_WITCH, "achievement_kill_witch_name", "achievement_kill_witch_desc"),
        
        // Mining achievements
        AchievementInfo(AchievementId.DIG_FIRST_TIME, "achievement_dig_first_time_name", "achievement_dig_first_time_desc"),
        AchievementInfo(AchievementId.FIND_GOLD, "achievement_find_gold_name", "achievement_find_gold_desc"),
        AchievementInfo(AchievementId.FIND_DIAMOND, "achievement_find_diamond_name", "achievement_find_diamond_desc"),
        
        // Dragon achievements
        AchievementInfo(AchievementId.SUMMON_DRAGON, "achievement_summon_dragon_name", "achievement_summon_dragon_desc"),
        AchievementInfo(AchievementId.REDUCE_DRAGON_LEVEL, "achievement_reduce_dragon_level_name", "achievement_reduce_dragon_level_desc"),
        AchievementInfo(AchievementId.INCREASE_DRAGON_LEVEL, "achievement_increase_dragon_level_name", "achievement_increase_dragon_level_desc"),
        AchievementInfo(AchievementId.KILL_DRAGON, "achievement_kill_dragon_name", "achievement_kill_dragon_desc"),
        
        // Building achievements
        AchievementInfo(AchievementId.BUILD_TEN_TOWERS, "achievement_build_ten_towers_name", "achievement_build_ten_towers_desc"),
        AchievementInfo(AchievementId.BUILD_TEN_RAFTS, "achievement_build_ten_rafts_name", "achievement_build_ten_rafts_desc"),
        
        // Bridge and barricade achievements
        AchievementInfo(AchievementId.DESTROY_BRIDGE, "achievement_destroy_bridge_name", "achievement_destroy_bridge_desc"),
        AchievementInfo(AchievementId.BUILD_BARRICADE, "achievement_build_barricade_name", "achievement_build_barricade_desc"),
        AchievementInfo(AchievementId.ADD_HEALTH_BARRICADE, "achievement_add_health_barricade_name", "achievement_add_health_barricade_desc")
    )
    
    /**
     * Get achievement info by ID
     */
    fun getInfo(id: AchievementId): AchievementInfo? {
        return allAchievements.find { it.id == id }
    }
}
