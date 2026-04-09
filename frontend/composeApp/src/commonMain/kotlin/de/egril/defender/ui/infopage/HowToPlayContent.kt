@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.DoorIcon
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.TargetIcon
import de.egril.defender.ui.icon.TimerIcon
import de.egril.defender.ui.hexagon.EnemyIconOnHexagon
import de.egril.defender.ui.hexagon.TowerIconOnHexagon
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Composable displaying the "How to Play" content.
 * Used both in RulesScreen and as a tab in InfoPageScreen.
 */
@Composable
fun HowToPlayContent() {
    SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
        // Game Overview
        HowToPlaySectionTitle(stringResource(Res.string.game_overview))
        HowToPlaySectionText(stringResource(Res.string.game_overview_text))

        Spacer(modifier = Modifier.height(16.dp))

        // Initial Building Phase
        HowToPlaySectionTitle(stringResource(Res.string.initial_building_phase))
        HowToPlaySectionText(stringResource(Res.string.at_start_of_level))
        HowToPlayBulletPoint(stringResource(Res.string.place_towers_instantly))
        HowToPlayBulletPoint(stringResource(Res.string.use_coins_strategically))
        HowToPlayBulletPoint(stringResource(Res.string.towers_ready_to_attack))
        HowToPlayBulletPoint(stringResource(Res.string.click_start_battle))

        Spacer(modifier = Modifier.height(16.dp))

        // Your Turn
        HowToPlaySectionTitle(stringResource(Res.string.your_turn))
        HowToPlaySectionText(stringResource(Res.string.during_your_turn))
        HowToPlayBulletPointWithIcon("Timer", { TimerIcon(size = 14.dp) }, stringResource(Res.string.place_new_towers))
        HowToPlayBulletPointWithIcon("Lightning", { LightningIcon(size = 14.dp) }, stringResource(Res.string.attack_enemies_desc))
        HowToPlayBulletPoint(stringResource(Res.string.upgrade_towers))
        HowToPlayBulletPoint(stringResource(Res.string.end_your_turn))

        Spacer(modifier = Modifier.height(16.dp))

        // Enemy Turn
        HowToPlaySectionTitle(stringResource(Res.string.enemy_turn))
        HowToPlaySectionText(stringResource(Res.string.after_end_turn))
        HowToPlayBulletPoint(stringResource(Res.string.enemies_move))
        HowToPlayBulletPoint(stringResource(Res.string.new_enemies))
        HowToPlayBulletPointWithIcon("Timer", { TimerIcon(size = 14.dp) }, stringResource(Res.string.build_timers_advance))
        HowToPlayBulletPoint(stringResource(Res.string.dot_effects_applied))

        Spacer(modifier = Modifier.height(16.dp))

        // Winning and Losing
        HowToPlaySectionTitle(stringResource(Res.string.winning_losing))
        HowToPlayBulletPoint(stringResource(Res.string.win_condition))
        HowToPlayBulletPoint(stringResource(Res.string.lose_condition))

        Spacer(modifier = Modifier.height(16.dp))

        // Tower Types
        HowToPlaySectionTitle(stringResource(Res.string.tower_types))
        // Filter out DRAGONS_LAIR as it's not a regular tower
        DefenderType.entries.filter { it != DefenderType.DRAGONS_LAIR }.forEach { defenderType ->
            HowToPlayTowerInfo(defenderType)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Attack Types
        HowToPlaySectionTitle(stringResource(Res.string.attack_types))
        HowToPlayBulletPoint(stringResource(Res.string.melee_ranged_desc))
        HowToPlayBulletPoint(stringResource(Res.string.fireball_desc))
        HowToPlayBulletPoint(stringResource(Res.string.acid_desc))

        Spacer(modifier = Modifier.height(16.dp))

        // Enemies
        HowToPlaySectionTitle(stringResource(Res.string.enemy_types))
        // Filter out DRAGON as it's a boss/special enemy
        AttackerType.entries.filter { it != AttackerType.DRAGON }.forEach { attackerType ->
            HowToPlayEnemyInfo(attackerType)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Victory and Defeat
        HowToPlaySectionTitle(stringResource(Res.string.victory_defeat))
        HowToPlaySectionText(stringResource(Res.string.victory_detail))
        HowToPlaySectionText(stringResource(Res.string.defeat_detail))

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Legend
        HowToPlaySectionTitle(stringResource(Res.string.grid_legend))
        HowToPlayBulletPointWithIcon("Door icon", { DoorIcon(size = 16.dp) }, stringResource(Res.string.spawn_desc))
        HowToPlayBulletPointWithIcon("Target icon", { TargetIcon(size = 16.dp) }, stringResource(Res.string.target_desc))
        HowToPlayBulletPoint(stringResource(Res.string.blue_towers))
        HowToPlayBulletPoint(stringResource(Res.string.gray_towers))
        HowToPlayBulletPoint(stringResource(Res.string.red_enemies))
        HowToPlayBulletPointWithIcon("Timer icon", { TimerIcon(size = 16.dp) }, stringResource(Res.string.build_time_remaining))
        HowToPlayBulletPointWithIcon("Lightning icon", { LightningIcon(size = 16.dp) }, stringResource(Res.string.actions_remaining))

        Spacer(modifier = Modifier.height(16.dp))

        // Tips
        HowToPlaySectionTitle(stringResource(Res.string.strategic_tips))
        HowToPlayBulletPoint(stringResource(Res.string.use_initial_phase))
        HowToPlayBulletPoint(stringResource(Res.string.save_coins))
        HowToPlayBulletPoint(stringResource(Res.string.focus_fire))
        HowToPlayBulletPoint(stringResource(Res.string.wizard_for_aoe))
        HowToPlayBulletPoint(stringResource(Res.string.upgrade_vs_build))

        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HowToPlaySectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun HowToPlaySectionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun HowToPlayBulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    ) {
        Text("• ", style = MaterialTheme.typography.bodyLarge)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HowToPlayBulletPointWithIcon(iconDescription: String, icon: @Composable () -> Unit, text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("• ", style = MaterialTheme.typography.bodyLarge)
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HowToPlayTowerInfo(defenderType: DefenderType) {
    val locale = com.hyperether.resources.currentLanguage.value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tower icon on blue hexagon
        TowerIconOnHexagon(
            defenderType = defenderType,
            size = 40.dp
        )

        // Tower details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = defenderType.getLocalizedName(locale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            // Build description based on tower type
            val description = when {
                defenderType.isMine -> stringResource(Res.string.mine_special_desc)
                defenderType.baseDamage == 0 -> stringResource(Res.string.special_building)
                else -> {
                    val coinsLabel = stringResource(Res.string.coins_label)
                    val rangeLabel = stringResource(Res.string.range)
                    "${defenderType.baseCost} $coinsLabel | ${defenderType.baseDamage} ${stringResource(Res.string.damage)} | $rangeLabel ${defenderType.baseRange} | ${defenderType.attackType.getLocalizedName(locale)}"
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HowToPlayEnemyInfo(attackerType: AttackerType) {
    val locale = com.hyperether.resources.currentLanguage.value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp, end = 8.dp)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Enemy icon on red hexagon
        EnemyIconOnHexagon(
            attackerType = attackerType,
            size = 40.dp
        )

        // Enemy details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attackerType.getLocalizedName(locale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${attackerType.health} ${stringResource(Res.string.hp_label)} | ${stringResource(Res.string.speed_label)} ${attackerType.speed} | ${attackerType.reward} ${stringResource(Res.string.coins_label)}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        }
    }
}
