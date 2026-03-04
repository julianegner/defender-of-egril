@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import de.egril.defender.BuildConfig
import de.egril.defender.ui.infopage.ImpressumWrapper
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.ui.settings.SettingsHintBox
import de.egril.defender.utils.isPlatformMobile
import de.egril.defender.utils.isPlatformWasm
import com.hyperether.resources.stringResource
import de.egril.defender.utils.isPlatformIos
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.emoji_sword
import defender_of_egril.composeapp.generated.resources.emoji_crown
import defender_of_egril.composeapp.generated.resources.emoji_skull
import org.jetbrains.compose.resources.painterResource

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onContinueGame: () -> Unit,
    hasAutosave: Boolean,
    onShowRules: () -> Unit,
    onShowInstallationInfo: () -> Unit,
    onSelectPlayer: () -> Unit,
    onEditPlayerName: () -> Unit,
    currentPlayerName: String?
) {
    // Track if settings hint should be shown
    val showSettingsHint by AppSettings.settingsHintShown
    
    // Track if commit info dialog should be shown
    var showCommitInfo by remember { mutableStateOf(false) }
    
    // Track if exit confirmation dialog should be shown
    var showExitConfirmation by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Settings and Info buttons in top-right corner
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info button (web version only)
                if (isPlatformWasm) {
                    IconButton(
                        onClick = onShowInstallationInfo,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.emoji_info),
                            contentDescription = stringResource(Res.string.installation_info),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                SettingsButton()
            }
            
            // Exit button in top-left corner (above player name if present)
            // iOS does not support exiting the app programmatically
            if (!isPlatformIos) {
                Button(
                    onClick = { showExitConfirmation = true },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .height(32.dp)
                        .widthIn(min = 80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.exit_game),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Player name and selection button below exit button
            if (currentPlayerName != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 48.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier.clickable { onEditPlayerName() }
                    ) {
                        Text(
                            text = stringResource(Res.string.player_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentPlayerName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onSelectPlayer,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.switch_player),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isPlatformMobile) Arrangement.Top else Arrangement.Center
            ) {
                // Add top spacer for mobile to center content with room for version at bottom
                if (isPlatformMobile) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Application banner with logo and styled text
                ApplicationBanner()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(Res.string.app_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // On mobile, buttons are in a column; on desktop, in a column as well
                if (isPlatformMobile) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onStartGame,
                                modifier = Modifier.weight(1f).height(60.dp)
                            ) {
                                Text(stringResource(Res.string.start_game), style = MaterialTheme.typography.titleMedium)
                            }
                            
                            // Continue Game button (only visible if autosave exists)
                            if (hasAutosave) {
                                Button(
                                    onClick = onContinueGame,
                                    modifier = Modifier.weight(1f).height(60.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(stringResource(Res.string.continue_game), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        
                        Button(
                            onClick = onShowRules,
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        ) {
                            Text(stringResource(Res.string.rules), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onStartGame,
                            modifier = Modifier.width(200.dp).height(60.dp)
                        ) {
                            Text(stringResource(Res.string.start_game), style = MaterialTheme.typography.titleMedium)
                        }
                        
                        // Continue Game button (only visible if autosave exists)
                        if (hasAutosave) {
                            Button(
                                onClick = onContinueGame,
                                modifier = Modifier.width(200.dp).height(60.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(stringResource(Res.string.continue_game), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onShowRules,
                        modifier = Modifier.width(200.dp).height(60.dp)
                    ) {
                        Text(stringResource(Res.string.rules), style = MaterialTheme.typography.titleMedium)
                    }
                }
                
                // Add bottom spacer for mobile to push content up and leave room for version
                if (isPlatformMobile) {
                    Spacer(modifier = Modifier.weight(1.3f))
                }
            }
            
            // Version info at the bottom - clickable to show commit info
            Text(
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH})",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp)
                    .clickable { showCommitInfo = true }
            )
            
            // Impressum at bottom center (WASM only, when flag is enabled)
            if (isPlatformWasm) {
                ImpressumWrapper(
                    rowModifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
            
            // Settings hint box - positioned below and to the left of settings button
            // Only show if hint hasn't been shown yet
            // MUST be drawn last to appear on top of other elements
            if (!showSettingsHint) {
                SettingsHintBox(
                    onDismiss = {
                        AppSettings.markSettingsHintShown()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 60.dp, end = 8.dp)  // Position below settings button
                )
            }
        }
        
        // Exit confirmation dialog
        if (showExitConfirmation) {
            AlertDialog(
                onDismissRequest = { showExitConfirmation = false },
                title = { Text(stringResource(Res.string.exit_game_confirm_title)) },
                text = { Text(stringResource(Res.string.exit_game_confirm_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitConfirmation = false
                            exitApplication()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(Res.string.exit))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirmation = false }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }
        
        // Commit info dialog
        if (showCommitInfo) {
            CommitInfoDialog(
                onDismiss = { showCommitInfo = false }
            )
        }
    }
}

@Composable
fun LevelCompleteScreen(
    levelId: Int,
    won: Boolean,
    isLastLevel: Boolean,
    xpEarned: Int = 0,
    onRestart: () -> Unit,
    onBackToMap: () -> Unit
) {
    // Determine which image/icon and text to show
    val imageResource = when {
        won && isLastLevel -> Res.drawable.emoji_crown  // Crown for winning the game
        won -> Res.drawable.emoji_sword  // Sword for winning a battle
        else -> Res.drawable.emoji_skull  // Skull for defeat
    }
    
    val title = when {
        won && isLastLevel -> stringResource(Res.string.victory)
        won -> stringResource(Res.string.battle_won)
        else -> stringResource(Res.string.defeat)
    }
    
    val message = when {
        won && isLastLevel -> stringResource(Res.string.victory_message)
        won -> stringResource(Res.string.battle_won_message)
        else -> stringResource(Res.string.defeat_message)
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Settings button in top-right corner
            SettingsButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon/Image
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = title,
                    modifier = Modifier.size(64.dp)
                )

                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Show XP earned if won and XP > 0
                if (won && xpEarned > 0) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(Res.string.xp_earned, xpEarned),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Brief info about XP system
                    Text(
                        text = stringResource(Res.string.xp_info_brief),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.Gray.copy(alpha = 0.8f)
                    )
                }
                
                // Check if this is level 5 (Dark Magic Rises) to show XP system unlock message
                val isDarkMagicRisesLevel = levelId == 5  // Assuming level 5 ID
                if (won && isDarkMagicRisesLevel) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(Res.string.xp_system_unlocked),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(Res.string.xp_system_unlock_message),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.width(150.dp).height(50.dp)
                    ) {
                        Text(stringResource(Res.string.retry))
                    }
                    
                    Button(
                        onClick = onBackToMap,
                        modifier = Modifier.width(150.dp).height(50.dp)
                    ) {
                        Text(stringResource(Res.string.world_map))
                    }
                }
            }
        }
    }
}
