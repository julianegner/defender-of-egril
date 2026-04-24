@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.AppBuildInfo
import de.egril.defender.WithImpressum
import de.egril.defender.iam.IamState
import de.egril.defender.ui.infopage.ImpressumWrapper
import de.egril.defender.ui.icon.LockIcon
import de.egril.defender.ui.icon.UnlockIcon
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.ui.settings.SettingsHintBox
import de.egril.defender.utils.isPlatformMobile
import de.egril.defender.utils.isPlatformWasm
import de.egril.defender.ui.isMobileWebBrowser
import com.hyperether.resources.stringResource
import de.egril.defender.utils.isPlatformIos
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.emoji_sword
import defender_of_egril.composeapp.generated.resources.emoji_crown
import defender_of_egril.composeapp.generated.resources.emoji_skull
import org.jetbrains.compose.resources.painterResource

/** Top padding for the main content column on mobile when a player area is visible. */
private val MobileTopPaddingWithPlayer = 150.dp
/** Top padding for the main content column on mobile when no player area is shown (only exit button). */
private val MobileTopPaddingWithoutPlayer = 60.dp

/**
 * Compact row of main menu action buttons for mobile and mobile-web layouts.
 * @param buttonHeight Height of each button (40.dp for native mobile, 30.dp for mobile web)
 * @param textStyle Typography style for button labels
 * @param contentPadding Internal padding for each button (null uses default)
 */
@Composable
private fun MainMenuButtonRow(
    onStartGame: () -> Unit,
    onContinueGame: () -> Unit,
    hasAutosave: Boolean,
    isDataLoaded: Boolean,
    onShowRules: () -> Unit,
    buttonHeight: androidx.compose.ui.unit.Dp,
    textStyle: androidx.compose.ui.text.TextStyle,
    contentPadding: PaddingValues? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onStartGame,
            enabled = isDataLoaded,
            modifier = Modifier.weight(1f).height(buttonHeight),
            contentPadding = contentPadding ?: ButtonDefaults.ContentPadding
        ) {
            Text(stringResource(Res.string.start_game), style = textStyle, maxLines = 1)
        }

        if (hasAutosave) {
            Button(
                onClick = onContinueGame,
                modifier = Modifier.weight(1f).height(buttonHeight),
                contentPadding = contentPadding ?: ButtonDefaults.ContentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(Res.string.continue_game), style = textStyle, maxLines = 1)
            }
        }

        Button(
            onClick = onShowRules,
            modifier = Modifier.weight(1f).height(buttonHeight),
            contentPadding = contentPadding ?: ButtonDefaults.ContentPadding
        ) {
            Text(stringResource(Res.string.rules), style = textStyle, maxLines = 1)
        }
    }
}

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onContinueGame: () -> Unit,
    hasAutosave: Boolean,
    onShowRules: () -> Unit,
    onShowInstallationInfo: () -> Unit,
    onShowDownloadInfo: () -> Unit = {},
    onShowBackendInfo: () -> Unit = {},
    onEditPlayerName: () -> Unit,
    currentPlayerName: String?,
    iamState: IamState = IamState(),
    iamLoginInProgress: Boolean = false,
    onIamLogin: () -> Unit = {},
    onIamLogout: () -> Unit = {},
    onIamLoginCancel: () -> Unit = {},
    isDataLoaded: Boolean = true,
    loadingProgress: LoadingProgress? = null
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
                // Info button (all platforms)
                TooltipWrapper(text = stringResource(Res.string.tooltip_info_installation)) {
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
                        // Show Keycloak username below the local player name when logged in
                        if (iamState.isAuthenticated && iamState.username != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                UnlockIcon(size = 12.dp)
                                Text(
                                    text = iamState.username,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    
                    // IAM login / logout button
                    if (iamState.isAuthenticated) {
                        TooltipWrapper(text = stringResource(Res.string.tooltip_log_out_from_remote)) {
                            OutlinedButton(
                                onClick = onIamLogout,
                                modifier = Modifier.height(36.dp)
                            ) {
                                LockIcon(size = 14.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(Res.string.iam_logout),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else if (iamLoginInProgress) {
                        OutlinedButton(
                            onClick = onIamLoginCancel,
                            modifier = Modifier.height(36.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(Res.string.iam_login_waiting),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onIamLogin,
                            modifier = Modifier.height(36.dp)
                        ) {
                            UnlockIcon(size = 14.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(Res.string.iam_login),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // "?" info button about backend/account
                    val backendInfoDesc = stringResource(Res.string.backend_info_title)
                    TooltipWrapper(text = backendInfoDesc) {
                        IconButton(
                            onClick = onShowBackendInfo,
                            modifier = Modifier
                                .size(28.dp)
                                .semantics { contentDescription = backendInfoDesc }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFB3E5FC), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "?",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            // On mobile and mobile-web browsers, add enough top padding to clear the player area and exit button.
            // If a player name is shown (incl. IAM login/logout button), use a larger offset.
            val mobileTopPadding = if (currentPlayerName != null) MobileTopPaddingWithPlayer else MobileTopPaddingWithoutPlayer
            val isMobileUI = isPlatformMobile || isMobileWebBrowser()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isPlatformMobile) Modifier.padding(top = mobileTopPadding) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isMobileUI) Arrangement.Top else Arrangement.Center
            ) {
                // Add top spacer on mobile/mobile-web to position banner in the upper third of the screen
                if (isMobileUI) {
                    Spacer(modifier = Modifier.weight(0.5f))
                }
                
                // Application banner with logo and styled text
                // Cap the banner width on mobile/mobile-web so it stays readable on wide landscape screens
                ApplicationBannerImage(
                    modifier = if (isMobileUI) Modifier.widthIn(max = 700.dp) else Modifier
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(Res.string.app_subtitle),
                    style = if (isMobileUI) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // On mobile, buttons are in a single row; on mobile web, same row layout but smaller; on desktop, in a row/column layout
                if (isPlatformMobile) {
                    MainMenuButtonRow(
                        onStartGame = onStartGame,
                        onContinueGame = onContinueGame,
                        hasAutosave = hasAutosave,
                        isDataLoaded = isDataLoaded,
                        onShowRules = onShowRules,
                        buttonHeight = 40.dp,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                } else if (isMobileWebBrowser()) {
                    // Mobile web browser: compact row layout at 50% button size
                    MainMenuButtonRow(
                        onStartGame = onStartGame,
                        onContinueGame = onContinueGame,
                        hasAutosave = hasAutosave,
                        isDataLoaded = isDataLoaded,
                        onShowRules = onShowRules,
                        buttonHeight = 30.dp,
                        textStyle = MaterialTheme.typography.labelSmall,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onStartGame,
                            enabled = isDataLoaded,
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
                    
                    // Download button (only for WASM with impressum)
                    if (isPlatformWasm && WithImpressum.withImpressum) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onShowDownloadInfo,
                            modifier = Modifier.width(200.dp).height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(stringResource(Res.string.download_button), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                
                // Loading progress indicator (shown on WASM while repository files are loading)
                if (!isDataLoaded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(Res.string.loading_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (loadingProgress != null) {
                            Text(
                                text = "${loadingProgress.loadedCount}/${loadingProgress.totalCount}: ${loadingProgress.currentFile}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Add bottom spacer for mobile/mobile-web to push content up and leave room for version
                if (isMobileUI) {
                    Spacer(modifier = Modifier.weight(1.3f))
                }
            }
            
            // Version info at the bottom - clickable to show commit info
            TooltipWrapper(
                text = stringResource(Res.string.commit_info_title),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "v${AppBuildInfo.VERSION_NAME} (${AppBuildInfo.COMMIT_HASH})",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { showCommitInfo = true }
                )
            }
            
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
    onBackToMap: () -> Unit,
    onShowFinalCredits: (() -> Unit)? = null,
    isDemoMode: Boolean = false,
    onStopDemoMode: (() -> Unit)? = null
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
        modifier = Modifier
            .fillMaxSize()
            .then(
                when {
                    isDemoMode -> Modifier.clickable { showStopDemoDialog = true }
                    navigateToCredits != null -> Modifier.clickable { navigateToCredits() }
                    else -> Modifier
                }
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Settings button in top-right corner (hidden in demo mode to keep UI clean)
            if (!isDemoMode) {
            SettingsButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
            }
            
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
                
                if (isDemoMode) {
                    // In demo mode, show a hint that the user can click to stop
                    Text(
                        text = stringResource(Res.string.demo_click_to_stop),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                } else {
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

        // Stop demo confirmation dialog
        if (showStopDemoDialog && onStopDemoMode != null) {
            de.egril.defender.ui.editor.ConfirmationDialog(
                title = stringResource(Res.string.stop_demo_title),
                message = stringResource(Res.string.stop_demo_message),
                onConfirm = { onStopDemoMode() },
                onDismiss = { showStopDemoDialog = false }
            )
        }
    }
}
