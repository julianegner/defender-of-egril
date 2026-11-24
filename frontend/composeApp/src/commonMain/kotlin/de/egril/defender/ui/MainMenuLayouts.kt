@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.WithImpressum
import de.egril.defender.iam.IamState
import de.egril.defender.ui.feedback.FeedbackButton
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.icon.HelpIcon
import de.egril.defender.ui.icon.LockIcon
import de.egril.defender.ui.icon.UnlockIcon
import de.egril.defender.ui.settings.*
import de.egril.defender.ui.settings.AppSettings.isDarkMode
import de.egril.defender.ui.worldmap.EditorButtonCard
import de.egril.defender.utils.isPlatformMobile
import de.egril.defender.utils.isPlatformWasm
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import dev.vicart.compose.material.symbols.MaterialSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols

@Composable
internal fun MainMenuCompactLayout(
    isPortrait: Boolean,
    isMobileWeb: Boolean,
    usesStackedLayout: Boolean,
    mobileLandscapeBannerHeight: Dp,
    showExitGameButton: Boolean,
    showEditorButton: Boolean,
    showAccessibilityBanner: Boolean,
    triggerFeedback: Boolean,
    triggerSettings: Boolean,
    accessibilityTabTrigger: SettingsTab?,
    openSettingsInitially: Boolean,
    settingsInitialTab: SettingsTab,
    isDataLoaded: Boolean,
    hasAutosave: Boolean,
    currentPlayerName: String?,
    iamState: IamState,
    iamLoginInProgress: Boolean,
    loadingProgress: LoadingProgress?,
    onStartGame: () -> Unit,
    onContinueGame: () -> Unit,
    onShowRules: () -> Unit,
    onOpenEditor: () -> Unit,
    onShowDownloadInfo: () -> Unit,
    onShowInstallationInfo: () -> Unit,
    onShowBackendInfo: () -> Unit,
    onEditPlayerName: () -> Unit,
    onIamLogin: () -> Unit,
    onIamLogout: () -> Unit,
    onIamLoginCancel: () -> Unit,
    onSettingsInitialOpenHandled: () -> Unit,
    onExitGameClick: () -> Unit,
    onFeedbackTriggerHandled: () -> Unit,
    onSettingsTriggerHandled: () -> Unit,
    onAccessibilityTabTriggerHandled: () -> Unit,
    onAccessibilitySettingsRequested: () -> Unit,
    onShowCommitInfo: () -> Unit,
) {
    // ===== MOBILE / MOBILE-WEB LAYOUT =====
    // Landscape: 3-column Row (left controls | center banner | right controls).
    // Portrait: controls row at top, then full-width banner below.
    // In both cases the banner height is capped so the scrollable content gets adequate room.
    Column(modifier = Modifier.fillMaxSize()) {
        if (!isPortrait) {
            // LANDSCAPE: banner sits in the center column, height derived from available space
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // Left column: exit button, player name, IAM login + help buttons
                Column(
                    modifier = Modifier.padding(end = 4.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    if (showExitGameButton) {
                        Button(
                            onClick = onExitGameClick,
                            modifier = Modifier.heightIn(min = 32.dp).widthIn(min = 80.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(
                                text = stringResource(Res.string.exit_game),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                            )
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "Esc", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                    }
                    if (currentPlayerName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(modifier = Modifier.clickable { onEditPlayerName() }) {
                            Text(
                                text = stringResource(Res.string.player_name),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = currentPlayerName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (iamState.isAuthenticated && iamState.username != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    UnlockIcon(size = 12.dp)
                                    Text(
                                        text = iamState.username,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (iamState.isAuthenticated) {
                                TooltipWrapper(text = stringResource(Res.string.tooltip_log_out_from_remote)) {
                                    OutlinedButton(
                                        onClick = onIamLogout,
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    ) {
                                        LockIcon(size = 12.dp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = stringResource(Res.string.iam_logout),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            } else if (iamLoginInProgress) {
                                OutlinedButton(
                                    onClick = onIamLoginCancel,
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_login_waiting),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onIamLogin,
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                ) {
                                    UnlockIcon(size = 12.dp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_login),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            val backendInfoDesc = stringResource(Res.string.backend_info_title)
                            TooltipWrapper(text = backendInfoDesc) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onShowBackendInfo,
                                        modifier = Modifier.size(28.dp).semantics { contentDescription = backendInfoDesc },
                                    ) {
                                        HelpIcon(size = 28.dp, tint = if (isDarkMode.value) Color(0xFFB3E5FC) else Color.Blue)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    ShortcutKeyChip(text = "?")
                                }
                            }
                        }
                    }
                }

                // Center: banner scales to fill the available width within the fixed height
                Box(
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ApplicationBannerImage(modifier = Modifier.height(mobileLandscapeBannerHeight))
                }

                // Right column: info + settings buttons
                Column(verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.End) {
                    val installationInfoLabel = stringResource(Res.string.tooltip_info_installation)
                    TooltipWrapper(text = installationInfoLabel) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onShowInstallationInfo,
                                modifier = Modifier.size(40.dp).semantics { contentDescription = installationInfoLabel },
                            ) {
                                MaterialSymbol.Filled(icon = MaterialSymbols.INFO, size = 28.dp)
                            }
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "I")
                            }
                        }
                    }
                    FeedbackButton(
                        shortcutKey = ".",
                        triggerOpen = triggerFeedback,
                        onTriggerHandled = onFeedbackTriggerHandled,
                    )
                    SettingsButton(
                        initiallyOpen = openSettingsInitially,
                        initialTab = settingsInitialTab,
                        onInitialOpenHandled = onSettingsInitialOpenHandled,
                        shortcutKey = ",",
                        triggerOpen = triggerSettings,
                        onTriggerHandled = onSettingsTriggerHandled,
                        triggerOpenWithTab = accessibilityTabTrigger,
                        onTriggerWithTabHandled = onAccessibilityTabTriggerHandled,
                    )
                }
            }
        } else {
            // PORTRAIT: controls row at top, then full-width banner below
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // Left column: exit button, player name, IAM login + help buttons
                Column(
                    modifier = Modifier.padding(end = 4.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    if (showExitGameButton) {
                        Button(
                            onClick = onExitGameClick,
                            modifier = Modifier.heightIn(min = 32.dp).widthIn(min = 80.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(
                                text = stringResource(Res.string.exit_game),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                            )
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "Esc", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                    }
                    if (currentPlayerName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(modifier = Modifier.clickable { onEditPlayerName() }) {
                            Text(
                                text = stringResource(Res.string.player_name),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = currentPlayerName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (iamState.isAuthenticated && iamState.username != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    UnlockIcon(size = 12.dp)
                                    Text(
                                        text = iamState.username,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (iamState.isAuthenticated) {
                                TooltipWrapper(text = stringResource(Res.string.tooltip_log_out_from_remote)) {
                                    OutlinedButton(
                                        onClick = onIamLogout,
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    ) {
                                        LockIcon(size = 12.dp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = stringResource(Res.string.iam_logout),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            } else if (iamLoginInProgress) {
                                OutlinedButton(
                                    onClick = onIamLoginCancel,
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_login_waiting),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onIamLogin,
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                ) {
                                    UnlockIcon(size = 12.dp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_login),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            val backendInfoDesc = stringResource(Res.string.backend_info_title)
                            TooltipWrapper(text = backendInfoDesc) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onShowBackendInfo,
                                        modifier = Modifier.size(28.dp).semantics { contentDescription = backendInfoDesc },
                                    ) {
                                        HelpIcon(size = 28.dp, tint = if (isDarkMode.value) Color(0xFFB3E5FC) else Color.Blue)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    ShortcutKeyChip(text = "?")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Right column: info + settings buttons
                Column(verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.End) {
                    val installationInfoLabel = stringResource(Res.string.tooltip_info_installation)
                    TooltipWrapper(text = installationInfoLabel) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onShowInstallationInfo,
                                modifier = Modifier.size(40.dp).semantics { contentDescription = installationInfoLabel },
                            ) {
                                MaterialSymbol.Filled(icon = MaterialSymbols.INFO, size = 28.dp)
                            }
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "I")
                            }
                        }
                    }
                    FeedbackButton(
                        shortcutKey = ".",
                        triggerOpen = triggerFeedback,
                        onTriggerHandled = onFeedbackTriggerHandled,
                    )
                    SettingsButton(
                        initiallyOpen = openSettingsInitially,
                        initialTab = settingsInitialTab,
                        onInitialOpenHandled = onSettingsInitialOpenHandled,
                        shortcutKey = ",",
                        triggerOpen = triggerSettings,
                        onTriggerHandled = onSettingsTriggerHandled,
                        triggerOpenWithTab = accessibilityTabTrigger,
                        onTriggerWithTabHandled = onAccessibilityTabTriggerHandled,
                    )
                }
            }
            // Full-width banner (aspect ratio ~3:1, so height ≈ width/3; natural fit)
            ApplicationBannerImage(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }

        // Scrollable content: subtitle + action buttons
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuButtonRow(
                onStartGame = onStartGame,
                onContinueGame = onContinueGame,
                hasAutosave = hasAutosave,
                isDataLoaded = isDataLoaded,
                onShowRules = onShowRules,
                onOpenEditor = onOpenEditor,
                showEditorButton = showEditorButton,
                buttonHeight = if (isPlatformMobile) 40.dp else 34.dp,
                textStyle = if (isPlatformMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isPlatformWasm && WithImpressum.withImpressum) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onShowDownloadInfo,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 280.dp).heightIn(min = 34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                ) {
                    Text(stringResource(Res.string.download_button), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(4.dp))
                    ShortcutKeyChip(text = "D", color = LocalContentColor.current.copy(alpha = 0.75f))
                }
            }
            if (!isDataLoaded) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(Res.string.loading_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    if (loadingProgress != null) {
                        Text(
                            text = "${loadingProgress.loadedCount}/${loadingProgress.totalCount}: ${loadingProgress.currentFile}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // Accessibility banner - shown only on first run, above bottom spacer
            if (!showAccessibilityBanner) {
                Spacer(modifier = Modifier.height(8.dp))
                AccessibilityBanner(
                    onOpenAccessibilitySettings = {
                        AppSettings.markAccessibilityBannerShown()
                        onAccessibilitySettingsRequested()
                    },
                    onDismiss = {
                        AppSettings.markAccessibilityBannerShown()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (shouldShowInlineMainMenuVersionInfo(usesStackedLayout, isMobileWeb)) {
                Spacer(modifier = Modifier.height(12.dp))
                MainMenuVersionInfo(onShowCommitInfo = onShowCommitInfo)
            }
            // Bottom spacer for version/impressum overlay clearance
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
internal fun BoxScope.MainMenuDesktopLayout(
    showExitGameButton: Boolean,
    showEditorButton: Boolean,
    showAccessibilityBanner: Boolean,
    triggerFeedback: Boolean,
    triggerSettings: Boolean,
    accessibilityTabTrigger: SettingsTab?,
    openSettingsInitially: Boolean,
    settingsInitialTab: SettingsTab,
    isDataLoaded: Boolean,
    hasAutosave: Boolean,
    currentPlayerName: String?,
    iamState: IamState,
    iamLoginInProgress: Boolean,
    loadingProgress: LoadingProgress?,
    desktopBannerHeight: Dp,
    onStartGame: () -> Unit,
    onContinueGame: () -> Unit,
    onShowRules: () -> Unit,
    onOpenEditor: () -> Unit,
    onShowDownloadInfo: () -> Unit,
    onShowInstallationInfo: () -> Unit,
    onShowBackendInfo: () -> Unit,
    onEditPlayerName: () -> Unit,
    onIamLogin: () -> Unit,
    onIamLogout: () -> Unit,
    onIamLoginCancel: () -> Unit,
    onSettingsInitialOpenHandled: () -> Unit,
    onExitGameClick: () -> Unit,
    onFeedbackTriggerHandled: () -> Unit,
    onSettingsTriggerHandled: () -> Unit,
    onAccessibilityTabTriggerHandled: () -> Unit,
    onAccessibilitySettingsRequested: () -> Unit,
) {
    // ===== DESKTOP LAYOUT =====
    // Settings and Info buttons in top-right corner
    Row(
        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val installationInfoLabel = stringResource(Res.string.tooltip_info_installation)
        TooltipWrapper(text = installationInfoLabel) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onShowInstallationInfo,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = installationInfoLabel },
                ) {
                    MaterialSymbol.Filled(icon = MaterialSymbols.INFO, size = 32.dp)
                }
                if (AppSettings.showButtonShortcutHints.value) {
                    Spacer(modifier = Modifier.width(4.dp))
                    ShortcutKeyChip(text = "I")
                }
            }
        }
        FeedbackButton(
            shortcutKey = ".",
            triggerOpen = triggerFeedback,
            onTriggerHandled = onFeedbackTriggerHandled,
        )
        SettingsButton(
            initiallyOpen = openSettingsInitially,
            initialTab = settingsInitialTab,
            onInitialOpenHandled = onSettingsInitialOpenHandled,
            shortcutKey = ",",
            triggerOpen = triggerSettings,
            onTriggerHandled = onSettingsTriggerHandled,
            triggerOpenWithTab = accessibilityTabTrigger,
            onTriggerWithTabHandled = onAccessibilityTabTriggerHandled,
        )
    }

    // Exit button and player name stacked in top-left corner
    Column(
        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        if (showExitGameButton) {
            Button(
                onClick = onExitGameClick,
                modifier = Modifier.heightIn(min = 32.dp).widthIn(min = 80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(Res.string.exit_game), style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                if (AppSettings.showButtonShortcutHints.value) {
                    Spacer(modifier = Modifier.width(4.dp))
                    ShortcutKeyChip(text = "Esc", color = LocalContentColor.current.copy(alpha = 0.75f))
                }
            }
        }

        // Player name and IAM login button below exit button
        if (currentPlayerName != null) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.clickable { onEditPlayerName() }) {
                    Text(
                        text = stringResource(Res.string.player_name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = currentPlayerName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        ShortcutKeyChip(text = "P")
                    }
                    if (iamState.isAuthenticated && iamState.username != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            UnlockIcon(size = 12.dp)
                            Text(
                                text = iamState.username,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                if (iamState.isAuthenticated) {
                    TooltipWrapper(text = stringResource(Res.string.tooltip_log_out_from_remote)) {
                        OutlinedButton(onClick = onIamLogout, modifier = Modifier.defaultMinSize(minHeight = 36.dp)) {
                            LockIcon(size = 14.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(Res.string.iam_logout), style = MaterialTheme.typography.bodySmall)
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "L", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                    }
                } else if (iamLoginInProgress) {
                    OutlinedButton(onClick = onIamLoginCancel, modifier = Modifier.defaultMinSize(minHeight = 36.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(Res.string.iam_login_waiting), style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    OutlinedButton(onClick = onIamLogin, modifier = Modifier.defaultMinSize(minHeight = 36.dp)) {
                        UnlockIcon(size = 14.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(Res.string.iam_login), style = MaterialTheme.typography.bodySmall)
                        if (AppSettings.showButtonShortcutHints.value) {
                            Spacer(modifier = Modifier.width(4.dp))
                            ShortcutKeyChip(text = "L", color = LocalContentColor.current.copy(alpha = 0.75f))
                        }
                    }
                }
                val backendInfoDesc = stringResource(Res.string.backend_info_title)
                TooltipWrapper(text = backendInfoDesc) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onShowBackendInfo,
                            modifier =
                                Modifier.size(34.dp).semantics {
                                    contentDescription =
                                        backendInfoDesc
                                },
                        ) {
                            HelpIcon(size = 34.dp, tint = if (isDarkMode.value) Color(0xFFB3E5FC) else Color.Blue)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        ShortcutKeyChip(text = "?")
                    }
                }
            }
        }
    } // end top-left Column

    // Desktop centered column with banner + subtitle + buttons
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ApplicationBannerImage(modifier = Modifier.height(desktopBannerHeight))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.app_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(24.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = onStartGame,
                enabled = isDataLoaded,
                modifier = Modifier.defaultMinSize(minWidth = 200.dp, minHeight = 60.dp),
            ) {
                Text(stringResource(Res.string.start_game), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Spacer(modifier = Modifier.width(4.dp))
                ShortcutKeyChip(text = "Enter", color = LocalContentColor.current.copy(alpha = 0.75f))
            }
            if (hasAutosave) {
                Button(
                    onClick = onContinueGame,
                    modifier = Modifier.defaultMinSize(minWidth = 200.dp, minHeight = 60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text(stringResource(Res.string.continue_game), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Spacer(modifier = Modifier.width(4.dp))
                    ShortcutKeyChip(text = "C", color = LocalContentColor.current.copy(alpha = 0.75f))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onShowRules, modifier = Modifier.defaultMinSize(minWidth = 200.dp, minHeight = 60.dp)) {
                Text(stringResource(Res.string.rules), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Spacer(modifier = Modifier.width(4.dp))
                ShortcutKeyChip(text = "H", color = LocalContentColor.current.copy(alpha = 0.75f))
            }
            if (showEditorButton) {
                EditorButtonCard(onClick = onOpenEditor)
            }
        }
        if (isPlatformWasm && WithImpressum.withImpressum) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onShowDownloadInfo,
                modifier = Modifier.defaultMinSize(minWidth = 200.dp, minHeight = 60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            ) {
                Text(stringResource(Res.string.download_button), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Spacer(modifier = Modifier.width(4.dp))
                ShortcutKeyChip(text = "D", color = LocalContentColor.current.copy(alpha = 0.75f))
            }
        }
        if (!isDataLoaded) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(Res.string.loading_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (loadingProgress != null) {
                    Text(
                        text = "${loadingProgress.loadedCount}/${loadingProgress.totalCount}: ${loadingProgress.currentFile}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // Accessibility banner - shown only on first run, below buttons/above version info
        if (!showAccessibilityBanner) {
            Spacer(modifier = Modifier.height(16.dp))
            AccessibilityBanner(
                onOpenAccessibilitySettings = {
                    AppSettings.markAccessibilityBannerShown()
                    onAccessibilitySettingsRequested()
                },
                onDismiss = {
                    AppSettings.markAccessibilityBannerShown()
                },
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }
    }
}
