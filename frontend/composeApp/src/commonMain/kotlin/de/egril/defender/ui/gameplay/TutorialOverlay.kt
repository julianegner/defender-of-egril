package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.TutorialStep
import de.egril.defender.model.InfoType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.icon.WoodIcon
import de.egril.defender.utils.isPlatformMobile
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Tutorial card that shows step-by-step instructions in the upper right corner
 * Can also show single tutorial info dialogs (dragon info, greed info, etc.)
 */
@Composable
fun TutorialOverlay(
    currentStep: TutorialStep,
    isNextEnabled: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    currentInfo: InfoType = InfoType.NONE,
    onDismissInfo: (() -> Unit)? = null
) {
    // Priority: Single info > Tutorial
    // Handle info system
    if (currentInfo != InfoType.NONE) {
        InfoContent(infoType = currentInfo, onDismiss = onDismissInfo ?: {})
        return
    }
    
    if (currentStep == TutorialStep.NONE) {
        return
    }
    
    Card(
        modifier = Modifier
            .width(300.dp)
            .heightIn(max = 400.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        SelectionContainer {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = getTutorialTitle(currentStep),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Scrollable content area
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = getTutorialContent(currentStep),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Buttons (always visible at the bottom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Skip button (only on first few steps)
                if (shouldShowSkipButton(currentStep)) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(Res.string.tutorial_skip),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                // Next/Got it button
                Button(
                    onClick = onNext,
                    enabled = isNextEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getButtonText(currentStep),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun getTutorialTitle(step: TutorialStep): String {
    return when (step) {
        TutorialStep.WELCOME -> stringResource(Res.string.tutorial_welcome_title)
        TutorialStep.MAP_NAVIGATION -> stringResource(Res.string.tutorial_map_navigation_title)
        TutorialStep.RESOURCES -> stringResource(Res.string.tutorial_resources_title)
        TutorialStep.TOWER_TYPES -> stringResource(Res.string.tutorial_towers_title)
        TutorialStep.LEGEND_INFO -> stringResource(Res.string.tutorial_legend_title)
        TutorialStep.ENEMY_LIST_INFO -> stringResource(Res.string.tutorial_enemy_list_title)
        TutorialStep.BUILD_TOWER -> stringResource(Res.string.tutorial_build_title)
        TutorialStep.INITIAL_BUILDING -> stringResource(Res.string.tutorial_initial_building_title)
        TutorialStep.UNDO_TOWER -> stringResource(Res.string.tutorial_undo_title)
        TutorialStep.ENEMIES_INCOMING -> stringResource(Res.string.tutorial_enemies_title)
        TutorialStep.START_COMBAT -> stringResource(Res.string.tutorial_combat_title)
        TutorialStep.CHECK_RANGE -> stringResource(Res.string.tutorial_range_title)
        TutorialStep.ATTACKING -> stringResource(Res.string.tutorial_attacking_title)
        TutorialStep.UPGRADE_TOWER -> stringResource(Res.string.tutorial_upgrade_title)
        TutorialStep.SELL_TOWER -> stringResource(Res.string.tutorial_sell_title)
        TutorialStep.SAVE_GAME -> stringResource(Res.string.tutorial_save_game_title)
        TutorialStep.COMPLETE -> stringResource(Res.string.tutorial_complete_title)
        TutorialStep.NONE -> ""
    }
}

@Composable
private fun getTutorialContent(step: TutorialStep): String {
    return when (step) {
        TutorialStep.WELCOME -> stringResource(Res.string.tutorial_welcome)
        TutorialStep.MAP_NAVIGATION -> {
            if (isPlatformMobile) {
                stringResource(Res.string.tutorial_map_navigation_mobile)
            } else {
                stringResource(Res.string.tutorial_map_navigation_desktop)
            }
        }
        TutorialStep.RESOURCES -> stringResource(Res.string.tutorial_resources)
        TutorialStep.TOWER_TYPES -> stringResource(Res.string.tutorial_towers)
        TutorialStep.LEGEND_INFO -> stringResource(Res.string.tutorial_legend)
        TutorialStep.ENEMY_LIST_INFO -> stringResource(Res.string.tutorial_enemy_list)
        TutorialStep.BUILD_TOWER -> stringResource(Res.string.tutorial_build)
        TutorialStep.INITIAL_BUILDING -> stringResource(Res.string.tutorial_initial_building)
        TutorialStep.UNDO_TOWER -> stringResource(Res.string.tutorial_undo)
        TutorialStep.ENEMIES_INCOMING -> stringResource(Res.string.tutorial_enemies)
        TutorialStep.START_COMBAT -> stringResource(Res.string.tutorial_combat)
        TutorialStep.CHECK_RANGE -> stringResource(Res.string.tutorial_range)
        TutorialStep.ATTACKING -> stringResource(Res.string.tutorial_attacking)
        TutorialStep.UPGRADE_TOWER -> stringResource(Res.string.tutorial_upgrade)
        TutorialStep.SELL_TOWER -> stringResource(Res.string.tutorial_sell)
        TutorialStep.SAVE_GAME -> stringResource(Res.string.tutorial_save_game)
        TutorialStep.COMPLETE -> stringResource(Res.string.tutorial_complete)
        TutorialStep.NONE -> ""
    }
}

@Composable
private fun getButtonText(step: TutorialStep): String {
    return when (step) {
        TutorialStep.COMPLETE -> stringResource(Res.string.tutorial_got_it)
        else -> stringResource(Res.string.tutorial_next)
    }
}

private fun shouldShowSkipButton(step: TutorialStep): Boolean {
    return step in listOf(
        TutorialStep.WELCOME,
        TutorialStep.MAP_NAVIGATION,
        TutorialStep.RESOURCES,
        TutorialStep.TOWER_TYPES,
        TutorialStep.LEGEND_INFO,
        TutorialStep.ENEMY_LIST_INFO,
        TutorialStep.BUILD_TOWER
    )
}

/**
 * Unified info content shown for single tutorial infos
 */
@Composable
private fun InfoContent(infoType: InfoType, onDismiss: () -> Unit) {
    when (infoType) {
        InfoType.DRAGON_INFO -> DragonInfoContent(onDismiss)
        InfoType.GREEN_WITCH_INFO -> GreenWitchInfoContent(onDismiss)
        InfoType.RED_WITCH_INFO -> RedWitchInfoContent(onDismiss)
        InfoType.GREED_INFO -> GreedInfoContent(onDismiss)
        InfoType.VERY_GREEDY_INFO -> VeryGreedyInfoContent(onDismiss)
        InfoType.MINE_WARNING -> MineWarningContent(onDismiss)
        InfoType.ONE_HP_WARNING -> OneHpWarningContent(onDismiss)
        InfoType.MAGICAL_TRAP_INFO -> MagicalTrapInfoContent(onDismiss)
        InfoType.EXTENDED_AREA_INFO -> ExtendedAreaInfoContent(onDismiss)
        InfoType.BARRICADE_INFO -> BarricadeInfoContent(onDismiss)
        InfoType.SPIKE_BARBS_INFO -> SpikeBarbsInfoContent(onDismiss)
        InfoType.WIZARD_FIRST_USE -> WizardFirstUseContent(onDismiss)
        InfoType.ALCHEMY_FIRST_USE -> AlchemyFirstUseContent(onDismiss)
        InfoType.BALLISTA_FIRST_USE -> BallistaFirstUseContent(onDismiss)
        InfoType.MINE_FIRST_USE -> MineFirstUseContent(onDismiss)
        InfoType.RIVER_INFO -> RiverInfoContent(onDismiss)
        InfoType.MINE_ON_RIVER_WARNING -> MineOnRiverWarningContent(onDismiss)
        InfoType.AUTO_ATTACK_INFO -> AutoAttackInfoContent(onDismiss)
        InfoType.SPECIAL_TOWERS_INFO -> { /* Handled specially in GamePlayScreen */ }
        InfoType.TOWER_INFO -> { /* Handled specially in GamePlayScreen */ }
        InfoType.NONE -> { /* No content to show */ }
    }
}

/**
 * Reusable scrollable info card for tutorial/info dialogs.
 * Makes content scrollable to ensure buttons are visible on small screens.
 * 
 * @param title Optional title composable (displayed above the scrollable content)
 * @param containerColor Background color for the card
 * @param onDismiss Callback when the dismiss button is clicked
 * @param buttonText Text for the dismiss button
 * @param buttonColor Color for the dismiss button (defaults to primary)
 * @param width Card width (default 300.dp)
 * @param maxHeight Maximum height for the card (default 400.dp for scrollability)
 * @param content Scrollable content composable
 */
@Composable
fun ScrollableInfoCard(
    title: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onDismiss: () -> Unit,
    buttonText: String = stringResource(Res.string.got_it),
    buttonColor: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 300.dp,
    maxHeight: Dp = 400.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .width(width)
            .heightIn(max = maxHeight)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title (if provided)
            if (title != null) {
                title()
            }
            
            // Scrollable content area
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
            
            // Dismiss button (always visible at the bottom)
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                )
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Dragon info content shown in the tutorial overlay
 */
@Composable
private fun DragonInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.dragon_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.dragon_info_movement),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.dragon_info_eating),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.dragon_info_level),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Dragon greed info dialog (greed > 0)
 */
@Composable
private fun GreedInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.dragon_greed_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.dragon_greed_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Green Witch info dialog
 */
@Composable
private fun GreenWitchInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.green_witch_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = GamePlayColors.Success  // Green theme for green witch
            )
        },
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.green_witch_info_healing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.green_witch_info_strategy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Red Witch info dialog
 */
@Composable
private fun RedWitchInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.red_witch_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = GamePlayColors.ErrorDark  // Red theme for red witch
            )
        },
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.red_witch_info_disabling),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.red_witch_info_strategy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


/**
 * Dragon very greedy info dialog (greed > 5)
 */
@Composable
private fun VeryGreedyInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.dragon_very_greedy_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.dragon_very_greedy_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Mine warning dialog
 */
@Composable
private fun MineWarningContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.mine_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        buttonColor = MaterialTheme.colorScheme.error,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.mine_warning_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/**
 * One HP warning dialog - shown when player starts with only 1 health point
 */
@Composable
private fun OneHpWarningContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.one_hp_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        buttonColor = MaterialTheme.colorScheme.error,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.one_hp_warning_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/**
 * Magical trap info content shown when wizard tower reaches level 10
 */
@Composable
private fun MagicalTrapInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                de.egril.defender.ui.icon.PentagramIcon(size = 32.dp)
                Text(
                    text = stringResource(Res.string.magical_trap_tutorial_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF9C27B0)  // Purple color for magical theme
                )
            }
        },
        buttonColor = Color(0xFF9C27B0),  // Purple button
        width = 400.dp,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.magical_trap_tutorial_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Extended area attack info content shown when wizard/alchemy tower reaches level 20
 */
@Composable
private fun ExtendedAreaInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                de.egril.defender.ui.icon.ExplosionIcon(size = 32.dp)
                Text(
                    text = stringResource(Res.string.extended_area_tutorial_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFF5722)  // Deep orange color for area attack theme
                )
            }
        },
        buttonColor = Color(0xFFFF5722),  // Deep orange button
        width = 400.dp,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.extended_area_tutorial_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Barricade ability info content shown when Spike/Spear tower reaches level 10
 */
@Composable
private fun BarricadeInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WoodIcon(size = 32.dp)
                Text(
                    text = stringResource(Res.string.barricade_info_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF795548)  // Brown color for wood
                )
            }
        },
        buttonColor = Color(0xFF795548),  // Brown button
        width = 600.dp,  // Increased from 400.dp to show German text without scrolling
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.barricade_info_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Spike barbs info popup (spike tower level 10+)
 */
@Composable
private fun SpikeBarbsInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                de.egril.defender.ui.icon.SwordIcon(size = 32.dp)
                Text(
                    text = stringResource(Res.string.spike_barbs_info_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF8B4513)  // SaddleBrown color for spikes
                )
            }
        },
        buttonColor = Color(0xFF8B4513),  // SaddleBrown button
        width = 600.dp,  // Wide to accommodate all translations
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.spike_barbs_info_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Wizard tower first use info content
 */
@Composable
private fun WizardFirstUseContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)  // Doubled from 32.dp
                        .background(Color.Gray, CircleShape),  // Gray background for visibility
                    contentAlignment = Alignment.Center
                ) {
                    de.egril.defender.ui.TowerTypeIcon(
                        defenderType = DefenderType.WIZARD_TOWER,
                        modifier = Modifier.size(56.dp)  // Slightly smaller than container
                    )
                }
                Text(
                    text = stringResource(Res.string.wizard_first_use_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF9C27B0)  // Purple color for wizard theme
                )
            }
        },
        buttonColor = Color(0xFF9C27B0),  // Purple button
        width = 400.dp,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.wizard_first_use_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Alchemy tower first use info content
 */
@Composable
private fun AlchemyFirstUseContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)  // Doubled from 32.dp
                        .background(Color.Gray, CircleShape),  // Gray background for visibility
                    contentAlignment = Alignment.Center
                ) {
                    de.egril.defender.ui.TowerTypeIcon(
                        defenderType = DefenderType.ALCHEMY_TOWER,
                        modifier = Modifier.size(56.dp)  // Slightly smaller than container
                    )
                }
                Text(
                    text = stringResource(Res.string.alchemy_first_use_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF4CAF50)  // Green color for alchemy theme
                )
            }
        },
        buttonColor = Color(0xFF4CAF50),  // Green button
        width = 400.dp,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.alchemy_first_use_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Ballista tower first use info content
 */
@Composable
private fun BallistaFirstUseContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)  // Doubled from 32.dp
                        .background(Color.Gray, CircleShape),  // Gray background for visibility
                    contentAlignment = Alignment.Center
                ) {
                    de.egril.defender.ui.TowerTypeIcon(
                        defenderType = DefenderType.BALLISTA_TOWER,
                        modifier = Modifier.size(56.dp)  // Slightly smaller than container
                    )
                }
                Text(
                    text = stringResource(Res.string.ballista_first_use_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF795548)  // Brown color for ballista theme
                )
            }
        },
        buttonColor = Color(0xFF795548),  // Brown button
        width = 400.dp,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.ballista_first_use_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Dwarven mine first use info content
 */
@Composable
private fun MineFirstUseContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)  // Doubled from 32.dp
                        .background(Color.Gray, CircleShape),  // Gray background for visibility
                    contentAlignment = Alignment.Center
                ) {
                    de.egril.defender.ui.TowerTypeIcon(
                        defenderType = DefenderType.DWARVEN_MINE,
                        modifier = Modifier.size(56.dp)  // Slightly smaller than container
                    )
                }
                Text(
                    text = stringResource(Res.string.mine_first_use_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFFD700)  // Gold color for mine theme
                )
            }
        },
        buttonColor = Color(0xFFFFD700),  // Gold button
        width = 400.dp,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.mine_first_use_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Add mining probabilities section
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.mining_probabilities),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700)
        )
        Spacer(modifier = Modifier.height(8.dp))
        MiningOutcomeGrid()
    }
}

/**
 * River mechanics info content shown when a level with rivers is started for the first time
 */
@Composable
private fun RiverInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.river_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2196F3)  // Blue color for water/river theme
            )
        },
        buttonColor = Color(0xFF2196F3),  // Blue button
        width = 400.dp,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.river_info_placement),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.river_info_movement),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.river_info_destruction),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.river_info_bridges),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MineOnRiverWarningContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.mine_on_river_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFF9800)  // Orange color for warning
            )
        },
        buttonColor = Color(0xFFFF9800),  // Orange button
        width = 350.dp,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.mine_on_river_warning_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Auto-attack feature info content shown the first time auto-attack becomes available
 */
@Composable
private fun AutoAttackInfoContent(onDismiss: () -> Unit) {
    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.auto_attack_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        buttonColor = MaterialTheme.colorScheme.primary,
        width = 450.dp,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.auto_attack_info_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
