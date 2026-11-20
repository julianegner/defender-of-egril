@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.feedback.FeedbackButton
import de.egril.defender.ui.icon.TrophyIcon
import de.egril.defender.ui.settings.*
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.emoji_skull
import defender_of_egril.composeapp.generated.resources.emoji_sword
import org.jetbrains.compose.resources.painterResource

@Composable
fun LevelCompleteScreen(
    levelId: Int,
    won: Boolean,
    isLastLevel: Boolean,
    xpEarned: Int = 0,
    newPlayerLevel: Int = 0,
    playerLevelGained: Int = 0,
    abilityPointsGained: Int = 0,
    onRestart: () -> Unit,
    onBackToMap: () -> Unit,
    onNextLevel: (() -> Unit)? = null,
    onShowFinalCredits: (() -> Unit)? = null,
    isDemoMode: Boolean = false,
    onStopDemoMode: (() -> Unit)? = null,
) {
    // In demo mode, allow the user to click anywhere to stop the demo
    var showStopDemoDialog by remember { mutableStateOf(false) }
    // After winning the final level, transition to the credits after 5 seconds
    val navigateToCredits: (() -> Unit)? = if (won && isLastLevel) onShowFinalCredits else null
    if (navigateToCredits != null) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(FINAL_CREDITS_TRANSITION_DELAY_MS)
            navigateToCredits()
        }
    }

    // Determine which image/icon and text to show
    val title =
        when {
            won && isLastLevel -> stringResource(Res.string.victory)
            won -> stringResource(Res.string.battle_won)
            else -> stringResource(Res.string.defeat)
        }

    val message =
        when {
            won && isLastLevel -> stringResource(Res.string.victory_message)
            won -> stringResource(Res.string.battle_won_message)
            else -> stringResource(Res.string.defeat_message)
        }

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .then(
                    when {
                        isDemoMode -> Modifier.clickable { showStopDemoDialog = true }
                        navigateToCredits != null -> Modifier.clickable { navigateToCredits() }
                        else -> Modifier
                    },
                ),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            val isMobileLayout =
                remember {
                    shouldUseMobileLevelCompleteLayout(
                        isNativeMobile = isPlatformMobile,
                        isMobileWeb = isMobileWebBrowser(),
                    )
                }
            val isPortrait = maxHeight > maxWidth
            val stackButtons =
                shouldStackLevelCompleteButtons(
                    isMobileLayout = isMobileLayout,
                    isPortrait = isPortrait,
                )

            // Settings and Feedback buttons in top-right corner (hidden in demo mode to keep UI clean)
            if (!isDemoMode) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeedbackButton(shortcutKey = ".")
                    SettingsButton(shortcutKey = ",")
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(if (isMobileLayout) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isMobileLayout) Arrangement.Top else Arrangement.Center,
            ) {
                if (isMobileLayout) Spacer(modifier = Modifier.height(8.dp))

                // Icon/Image
                val iconSize = if (isMobileLayout) 40.dp else 64.dp
                when {
                    won && isLastLevel -> TrophyIcon(size = iconSize)
                    won ->
                        Image(
                            painter = painterResource(Res.drawable.emoji_sword),
                            contentDescription = title,
                            modifier = Modifier.size(iconSize),
                        )
                    else ->
                        Image(
                            painter = painterResource(Res.drawable.emoji_skull),
                            contentDescription = title,
                            modifier = Modifier.size(iconSize),
                        )
                }

                Spacer(modifier = Modifier.height(if (isMobileLayout) 8.dp else 16.dp))

                Text(
                    text = title,
                    style = if (isMobileLayout) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )

                Spacer(modifier = Modifier.height(if (isMobileLayout) 8.dp else 16.dp))

                Text(
                    text = message,
                    style = if (isMobileLayout) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                if (xpEarned > 0) {
                    LevelCompleteXpSummary(
                        xpEarned = xpEarned,
                        newPlayerLevel = newPlayerLevel,
                        playerLevelGained = playerLevelGained,
                        abilityPointsGained = abilityPointsGained,
                    )
                }

                // Check if this is level 5 (Dark Magic Rises) to show XP system unlock message
                val isDarkMagicRisesLevel = levelId == 5 // Assuming level 5 ID
                if (won && isDarkMagicRisesLevel) {
                    Spacer(modifier = Modifier.height(if (isMobileLayout) 8.dp else 16.dp))

                    Text(
                        text = stringResource(Res.string.xp_system_unlocked),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(Res.string.xp_system_unlock_message),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(if (isMobileLayout) 16.dp else 48.dp))

                if (isDemoMode) {
                    // In demo mode, show a hint that the user can click to stop
                    Text(
                        text = stringResource(Res.string.demo_click_to_stop),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                } else if (stackButtons) {
                    // Mobile portrait: stack buttons vertically for better fit
                    val buttonModifier = Modifier.fillMaxWidth().height(44.dp)
                    Button(onClick = onRestart, modifier = buttonModifier) {
                        Text(stringResource(Res.string.retry))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBackToMap, modifier = buttonModifier) {
                        Text(stringResource(Res.string.world_map))
                    }
                    if (won && onNextLevel != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onNextLevel, modifier = buttonModifier) {
                            Text(stringResource(Res.string.next_level))
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val btnHeight = if (isMobileLayout) 44.dp else 50.dp
                        val btnWidth = if (isMobileLayout) 120.dp else 150.dp
                        Button(
                            onClick = onRestart,
                            modifier = Modifier.width(btnWidth).height(btnHeight),
                        ) {
                            Text(stringResource(Res.string.retry))
                        }

                        Button(
                            onClick = onBackToMap,
                            modifier = Modifier.width(btnWidth).height(btnHeight),
                        ) {
                            Text(stringResource(Res.string.world_map))
                        }

                        if (won && onNextLevel != null) {
                            Button(
                                onClick = onNextLevel,
                                modifier = Modifier.width(btnWidth).height(btnHeight),
                            ) {
                                Text(stringResource(Res.string.next_level))
                            }
                        }
                    }
                }

                if (isMobileLayout) Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Stop demo confirmation dialog
        if (showStopDemoDialog && onStopDemoMode != null) {
            de.egril.defender.ui.editor.ConfirmationDialog(
                title = stringResource(Res.string.stop_demo_title),
                message = stringResource(Res.string.stop_demo_message),
                onConfirm = { onStopDemoMode() },
                onDismiss = { showStopDemoDialog = false },
            )
        }
    }
}

@Composable
private fun LevelCompleteXpSummary(
    xpEarned: Int,
    newPlayerLevel: Int,
    playerLevelGained: Int,
    abilityPointsGained: Int,
) {
    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(Res.string.xp_earned, xpEarned),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = Color.Gray,
    )

    if (playerLevelGained > 0) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.level_up_reached, newPlayerLevel),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }

    if (abilityPointsGained > 0) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.ability_points_gained, abilityPointsGained),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(Res.string.xp_info_brief),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = Color.Gray.copy(alpha = 0.8f),
    )
}
