package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.egril.defender.model.TutorialStep
import de.egril.defender.model.InfoType
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

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
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
            // Title
            Text(
                text = getTutorialTitle(currentStep),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Content
            Text(
                text = getTutorialContent(currentStep),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Buttons
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

@Composable
private fun getTutorialTitle(step: TutorialStep): String {
    return when (step) {
        TutorialStep.WELCOME -> stringResource(Res.string.tutorial_welcome_title)
        TutorialStep.RESOURCES -> stringResource(Res.string.tutorial_resources_title)
        TutorialStep.TOWER_TYPES -> stringResource(Res.string.tutorial_towers_title)
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
        TutorialStep.RESOURCES -> stringResource(Res.string.tutorial_resources)
        TutorialStep.TOWER_TYPES -> stringResource(Res.string.tutorial_towers)
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
        TutorialStep.RESOURCES,
        TutorialStep.TOWER_TYPES,
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
        InfoType.GREED_INFO -> GreedInfoContent(onDismiss)
        InfoType.VERY_GREEDY_INFO -> VeryGreedyInfoContent(onDismiss)
        InfoType.MINE_WARNING -> MineWarningContent(onDismiss)
        InfoType.ONE_HP_WARNING -> OneHpWarningContent(onDismiss)
        InfoType.MAGICAL_TRAP_INFO -> MagicalTrapInfoContent(onDismiss)
        InfoType.EXTENDED_AREA_INFO -> ExtendedAreaInfoContent(onDismiss)
        InfoType.NONE -> { /* No content to show */ }
    }
}

/**
 * Dragon info content shown in the tutorial overlay
 */
@Composable
private fun DragonInfoContent(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
            // Title
            Text(
                text = stringResource(Res.string.dragon_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Content
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
            
            // Got it button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.got_it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Dragon greed info dialog (greed > 0)
 */
@Composable
private fun GreedInfoContent(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
            Text(
                text = stringResource(Res.string.dragon_greed_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(Res.string.dragon_greed_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.got_it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Dragon very greedy info dialog (greed > 5)
 */
@Composable
private fun VeryGreedyInfoContent(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
            Text(
                text = stringResource(Res.string.dragon_very_greedy_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(Res.string.dragon_very_greedy_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.got_it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Mine warning dialog
 */
@Composable
private fun MineWarningContent(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
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
            Text(
                text = stringResource(Res.string.mine_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = stringResource(Res.string.mine_warning_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = stringResource(Res.string.got_it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * One HP warning dialog - shown when player starts with only 1 health point
 */
@Composable
private fun OneHpWarningContent(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
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
            Text(
                text = stringResource(Res.string.one_hp_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = stringResource(Res.string.one_hp_warning_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = stringResource(Res.string.got_it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Magical trap info content shown when wizard tower reaches level 10
 */
@Composable
private fun MagicalTrapInfoContent(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .width(400.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
            // containerColor = Color(0xFF9C27B0) // .copy(alpha = 0.15f)  // Purple background for magical theme
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
            // Title with pentagram icon
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
            
            // Message
            Text(
                text = stringResource(Res.string.magical_trap_tutorial_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)  // Purple button
                )
            ) {
                Text(
                    text = stringResource(Res.string.got_it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Extended area attack info content shown when wizard/alchemy tower reaches level 20
 */
@Composable
private fun ExtendedAreaInfoContent(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .width(400.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
            // Title with explosion icon
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
            
            // Message
            Text(
                text = stringResource(Res.string.extended_area_tutorial_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5722)  // Deep orange button
                )
            ) {
                Text(
                    text = stringResource(Res.string.got_it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
