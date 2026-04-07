package de.egril.defender.ui

import androidx.compose.runtime.Composable
import com.hyperether.resources.stringResource
import de.egril.defender.model.AchievementId
import defender_of_egril.composeapp.generated.resources.*

/**
 * Get localized name for an achievement
 */
@Composable
fun AchievementId.getLocalizedName(): String {
    return when (this) {
        // Tower achievements
        AchievementId.BUILD_TOWER -> stringResource(Res.string.achievement_build_tower_name)
        AchievementId.UPGRADE_TOWER -> stringResource(Res.string.achievement_upgrade_tower_name)
        AchievementId.SELL_TOWER -> stringResource(Res.string.achievement_sell_tower_name)
        AchievementId.UNDO_TOWER -> stringResource(Res.string.achievement_undo_tower_name)
        
        // Raft achievements
        AchievementId.BUILD_RAFT -> stringResource(Res.string.achievement_build_raft_name)
        AchievementId.UPGRADE_RAFT -> stringResource(Res.string.achievement_upgrade_raft_name)
        AchievementId.SELL_RAFT -> stringResource(Res.string.achievement_sell_raft_name)
        AchievementId.UNDO_RAFT -> stringResource(Res.string.achievement_undo_raft_name)
        AchievementId.LOSE_RAFT_MAP_EDGE -> stringResource(Res.string.achievement_lose_raft_map_edge_name)
        AchievementId.LOSE_RAFT_MAELSTROM -> stringResource(Res.string.achievement_lose_raft_maelstrom_name)
        
        // Level achievements
        AchievementId.WIN_LEVEL -> stringResource(Res.string.achievement_win_level_name)
        AchievementId.LOSE_LEVEL -> stringResource(Res.string.achievement_lose_level_name)
        AchievementId.WIN_LEVEL_FULL_HP -> stringResource(Res.string.achievement_win_level_full_hp_name)
        AchievementId.WIN_LEVEL_ONE_HP -> stringResource(Res.string.achievement_win_level_one_hp_name)
        
        // Combat achievements
        AchievementId.KILL_TWO_ENEMIES_SAME_TURN -> stringResource(Res.string.achievement_kill_two_enemies_same_turn_name)
        AchievementId.KILL_TWO_ENEMIES_SAME_ATTACK -> stringResource(Res.string.achievement_kill_two_enemies_same_attack_name)
        AchievementId.KILL_OGRE -> stringResource(Res.string.achievement_kill_ogre_name)
        AchievementId.KILL_EVIL_WIZARD -> stringResource(Res.string.achievement_kill_evil_wizard_name)
        AchievementId.KILL_DEMON -> stringResource(Res.string.achievement_kill_demon_name)
        AchievementId.KILL_EWHAD -> stringResource(Res.string.achievement_kill_ewhad_name)
        AchievementId.KILL_WITCH -> stringResource(Res.string.achievement_kill_witch_name)
        
        // Mining achievements
        AchievementId.DIG_FIRST_TIME -> stringResource(Res.string.achievement_dig_first_time_name)
        AchievementId.FIND_GOLD -> stringResource(Res.string.achievement_find_gold_name)
        AchievementId.FIND_DIAMOND -> stringResource(Res.string.achievement_find_diamond_name)
        
        // Dragon achievements
        AchievementId.SUMMON_DRAGON -> stringResource(Res.string.achievement_summon_dragon_name)
        AchievementId.REDUCE_DRAGON_LEVEL -> stringResource(Res.string.achievement_reduce_dragon_level_name)
        AchievementId.INCREASE_DRAGON_LEVEL -> stringResource(Res.string.achievement_increase_dragon_level_name)
        AchievementId.KILL_DRAGON -> stringResource(Res.string.achievement_kill_dragon_name)
        
        // Building achievements
        AchievementId.BUILD_TEN_TOWERS -> stringResource(Res.string.achievement_build_ten_towers_name)
        AchievementId.BUILD_TEN_RAFTS -> stringResource(Res.string.achievement_build_ten_rafts_name)
        
        // Bridge and barricade achievements
        AchievementId.DESTROY_BRIDGE -> stringResource(Res.string.achievement_destroy_bridge_name)
        AchievementId.BUILD_BARRICADE -> stringResource(Res.string.achievement_build_barricade_name)
        AchievementId.ADD_HEALTH_BARRICADE -> stringResource(Res.string.achievement_add_health_barricade_name)
        
        // XP and stat achievements
        AchievementId.FIRST_STAT_UPGRADE -> stringResource(Res.string.achievement_first_stat_upgrade_name)
        AchievementId.FIRST_SPELL_UNLOCK -> stringResource(Res.string.achievement_first_spell_unlock_name)
        AchievementId.CONSTRUCTION_LEVEL_3 -> stringResource(Res.string.achievement_construction_level_3_name)
        AchievementId.PLAYER_LEVEL_10 -> stringResource(Res.string.achievement_player_level_10_name)
        AchievementId.PLAYER_LEVEL_100 -> stringResource(Res.string.achievement_player_level_100_name)
    }
}

/**
 * Get localized description for an achievement
 */
@Composable
fun AchievementId.getLocalizedDescription(): String {
    return when (this) {
        // Tower achievements
        AchievementId.BUILD_TOWER -> stringResource(Res.string.achievement_build_tower_desc)
        AchievementId.UPGRADE_TOWER -> stringResource(Res.string.achievement_upgrade_tower_desc)
        AchievementId.SELL_TOWER -> stringResource(Res.string.achievement_sell_tower_desc)
        AchievementId.UNDO_TOWER -> stringResource(Res.string.achievement_undo_tower_desc)
        
        // Raft achievements
        AchievementId.BUILD_RAFT -> stringResource(Res.string.achievement_build_raft_desc)
        AchievementId.UPGRADE_RAFT -> stringResource(Res.string.achievement_upgrade_raft_desc)
        AchievementId.SELL_RAFT -> stringResource(Res.string.achievement_sell_raft_desc)
        AchievementId.UNDO_RAFT -> stringResource(Res.string.achievement_undo_raft_desc)
        AchievementId.LOSE_RAFT_MAP_EDGE -> stringResource(Res.string.achievement_lose_raft_map_edge_desc)
        AchievementId.LOSE_RAFT_MAELSTROM -> stringResource(Res.string.achievement_lose_raft_maelstrom_desc)
        
        // Level achievements
        AchievementId.WIN_LEVEL -> stringResource(Res.string.achievement_win_level_desc)
        AchievementId.LOSE_LEVEL -> stringResource(Res.string.achievement_lose_level_desc)
        AchievementId.WIN_LEVEL_FULL_HP -> stringResource(Res.string.achievement_win_level_full_hp_desc)
        AchievementId.WIN_LEVEL_ONE_HP -> stringResource(Res.string.achievement_win_level_one_hp_desc)
        
        // Combat achievements
        AchievementId.KILL_TWO_ENEMIES_SAME_TURN -> stringResource(Res.string.achievement_kill_two_enemies_same_turn_desc)
        AchievementId.KILL_TWO_ENEMIES_SAME_ATTACK -> stringResource(Res.string.achievement_kill_two_enemies_same_attack_desc)
        AchievementId.KILL_OGRE -> stringResource(Res.string.achievement_kill_ogre_desc)
        AchievementId.KILL_EVIL_WIZARD -> stringResource(Res.string.achievement_kill_evil_wizard_desc)
        AchievementId.KILL_DEMON -> stringResource(Res.string.achievement_kill_demon_desc)
        AchievementId.KILL_EWHAD -> stringResource(Res.string.achievement_kill_ewhad_desc)
        AchievementId.KILL_WITCH -> stringResource(Res.string.achievement_kill_witch_desc)
        
        // Mining achievements
        AchievementId.DIG_FIRST_TIME -> stringResource(Res.string.achievement_dig_first_time_desc)
        AchievementId.FIND_GOLD -> stringResource(Res.string.achievement_find_gold_desc)
        AchievementId.FIND_DIAMOND -> stringResource(Res.string.achievement_find_diamond_desc)
        
        // Dragon achievements
        AchievementId.SUMMON_DRAGON -> stringResource(Res.string.achievement_summon_dragon_desc)
        AchievementId.REDUCE_DRAGON_LEVEL -> stringResource(Res.string.achievement_reduce_dragon_level_desc)
        AchievementId.INCREASE_DRAGON_LEVEL -> stringResource(Res.string.achievement_increase_dragon_level_desc)
        AchievementId.KILL_DRAGON -> stringResource(Res.string.achievement_kill_dragon_desc)
        
        // Building achievements
        AchievementId.BUILD_TEN_TOWERS -> stringResource(Res.string.achievement_build_ten_towers_desc)
        AchievementId.BUILD_TEN_RAFTS -> stringResource(Res.string.achievement_build_ten_rafts_desc)
        
        // Bridge and barricade achievements
        AchievementId.DESTROY_BRIDGE -> stringResource(Res.string.achievement_destroy_bridge_desc)
        AchievementId.BUILD_BARRICADE -> stringResource(Res.string.achievement_build_barricade_desc)
        AchievementId.ADD_HEALTH_BARRICADE -> stringResource(Res.string.achievement_add_health_barricade_desc)
        
        // XP and stat achievements
        AchievementId.FIRST_STAT_UPGRADE -> stringResource(Res.string.achievement_first_stat_upgrade_desc)
        AchievementId.FIRST_SPELL_UNLOCK -> stringResource(Res.string.achievement_first_spell_unlock_desc)
        AchievementId.CONSTRUCTION_LEVEL_3 -> stringResource(Res.string.achievement_construction_level_3_desc)
        AchievementId.PLAYER_LEVEL_10 -> stringResource(Res.string.achievement_player_level_10_desc)
        AchievementId.PLAYER_LEVEL_100 -> stringResource(Res.string.achievement_player_level_100_desc)
    }
}
