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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.defenderofegril.model.TutorialStep
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Tutorial overlay that shows step-by-step instructions
 */
@Composable
fun TutorialOverlay(
    currentStep: TutorialStep,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    if (currentStep == TutorialStep.NONE || currentStep == TutorialStep.COMPLETE) {
        return
    }
    
    Dialog(
        onDismissRequest = { /* Don't dismiss on outside click */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = getTutorialTitle(currentStep),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Content
                Text(
                    text = getTutorialContent(currentStep),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
                            Text(stringResource(Res.string.tutorial_skip))
                        }
                    }
                    
                    // Next/Got it button
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(getButtonText(currentStep))
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
        TutorialStep.RESOURCES -> stringResource(Res.string.tutorial_resources_title)
        TutorialStep.BUILD_TOWER -> stringResource(Res.string.tutorial_build_title)
        TutorialStep.TOWER_TYPES -> stringResource(Res.string.tutorial_towers_title)
        TutorialStep.ENEMIES_INCOMING -> stringResource(Res.string.tutorial_enemies_title)
        TutorialStep.START_COMBAT -> stringResource(Res.string.tutorial_combat_title)
        TutorialStep.COMPLETE -> stringResource(Res.string.tutorial_complete_title)
        TutorialStep.NONE -> ""
    }
}

@Composable
private fun getTutorialContent(step: TutorialStep): String {
    return when (step) {
        TutorialStep.WELCOME -> stringResource(Res.string.tutorial_welcome)
        TutorialStep.RESOURCES -> stringResource(Res.string.tutorial_resources)
        TutorialStep.BUILD_TOWER -> stringResource(Res.string.tutorial_build)
        TutorialStep.TOWER_TYPES -> stringResource(Res.string.tutorial_towers)
        TutorialStep.ENEMIES_INCOMING -> stringResource(Res.string.tutorial_enemies)
        TutorialStep.START_COMBAT -> stringResource(Res.string.tutorial_combat)
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
        TutorialStep.BUILD_TOWER
    )
}
