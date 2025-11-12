package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.defenderofegril.model.TutorialStep
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Tutorial card that shows step-by-step instructions in the upper right corner
 */
@Composable
fun TutorialOverlay(
    currentStep: TutorialStep,
    isNextEnabled: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    if (currentStep == TutorialStep.NONE || currentStep == TutorialStep.COMPLETE) {
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
