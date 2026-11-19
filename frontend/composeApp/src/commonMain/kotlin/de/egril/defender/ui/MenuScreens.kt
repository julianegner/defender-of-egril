@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.AppBuildInfo
import de.egril.defender.WithImpressum
import de.egril.defender.iam.IamState
import de.egril.defender.ui.announcements.villains.VillainsAnnouncementTriangle
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.infopage.ImpressumWrapper
import de.egril.defender.ui.settings.*
import de.egril.defender.ui.worldmap.EditorButtonCard
import de.egril.defender.utils.isPlatformDesktop
import de.egril.defender.utils.isPlatformIos
import de.egril.defender.utils.isPlatformMobile
import de.egril.defender.utils.isPlatformWasm
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

internal fun shouldUseStackedMainMenuLayout(
    isNativeMobile: Boolean,
    isMobileWeb: Boolean,
    isPortrait: Boolean,
    isNarrowWindow: Boolean = false,
): Boolean = isNativeMobile || (isMobileWeb && isPortrait) || isNarrowWindow

internal fun shouldUseCompactMainMenuLayout(
    isNativeMobile: Boolean,
    isMobileWeb: Boolean,
    isNarrowWindow: Boolean = false,
): Boolean = isNativeMobile || isMobileWeb || isNarrowWindow

internal fun shouldShowOverlayMainMenuVersionInfo(
    usesStackedLayout: Boolean,
    isMobileWeb: Boolean,
): Boolean = !usesStackedLayout || isMobileWeb

internal fun shouldShowInlineMainMenuVersionInfo(
    usesStackedLayout: Boolean,
    isMobileWeb: Boolean,
): Boolean = usesStackedLayout && !isMobileWeb

/** Returns true when LevelComplete screen should use a compact, scrollable mobile layout. */
internal fun shouldUseMobileLevelCompleteLayout(
    isNativeMobile: Boolean,
    isMobileWeb: Boolean,
): Boolean = isNativeMobile || isMobileWeb

/** Returns true when LevelComplete screen buttons should be stacked vertically (portrait phone or narrow mobile web). */
internal fun shouldStackLevelCompleteButtons(
    isMobileLayout: Boolean,
    isPortrait: Boolean,
): Boolean = isMobileLayout && isPortrait

/**
 * Compact row of main menu action buttons for mobile and mobile-web layouts.
 * @param buttonHeight Height of each button (40.dp for native mobile, 30.dp for mobile web)
 * @param textStyle Typography style for button labels
 * @param contentPadding Internal padding for each button (null uses default)
 */
@Composable
internal fun MainMenuButtonRow(
    onStartGame: () -> Unit,
    onContinueGame: () -> Unit,
    hasAutosave: Boolean,
    isDataLoaded: Boolean,
    onShowRules: () -> Unit,
    onOpenEditor: () -> Unit = {},
    showEditorButton: Boolean = false,
    buttonHeight: androidx.compose.ui.unit.Dp,
    textStyle: androidx.compose.ui.text.TextStyle,
    contentPadding: PaddingValues? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onStartGame,
            enabled = isDataLoaded,
            modifier = Modifier.weight(1f).heightIn(min = buttonHeight),
            contentPadding = contentPadding ?: ButtonDefaults.ContentPadding,
        ) {
            Text(stringResource(Res.string.start_game), style = textStyle, maxLines = 1)
            if (AppSettings.showButtonShortcutHints.value) {
                Spacer(modifier = Modifier.width(4.dp))
                de.egril.defender.ui.gameplay.ShortcutKeyChip(
                    text = "Enter",
                    color = LocalContentColor.current.copy(alpha = 0.75f),
                )
            }
        }

        if (hasAutosave) {
            Button(
                onClick = onContinueGame,
                modifier = Modifier.weight(1f).heightIn(min = buttonHeight),
                contentPadding = contentPadding ?: ButtonDefaults.ContentPadding,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
            ) {
                Text(stringResource(Res.string.continue_game), style = textStyle, maxLines = 1)
                if (AppSettings.showButtonShortcutHints.value) {
                    Spacer(modifier = Modifier.width(4.dp))
                    de.egril.defender.ui.gameplay.ShortcutKeyChip(
                        text = "C",
                        color = LocalContentColor.current.copy(alpha = 0.75f),
                    )
                }
            }
        }

        Button(
            onClick = onShowRules,
            modifier = Modifier.weight(1f).heightIn(min = buttonHeight),
            contentPadding = contentPadding ?: ButtonDefaults.ContentPadding,
        ) {
            Text(stringResource(Res.string.rules), style = textStyle, maxLines = 1)
            if (AppSettings.showButtonShortcutHints.value) {
                Spacer(modifier = Modifier.width(4.dp))
                de.egril.defender.ui.gameplay.ShortcutKeyChip(
                    text = "H",
                    color = LocalContentColor.current.copy(alpha = 0.75f),
                )
            }
        }

        if (showEditorButton) {
            Box(
                modifier = Modifier.weight(1f).heightIn(min = buttonHeight),
                contentAlignment = Alignment.Center,
            ) {
                EditorButtonCard(onClick = onOpenEditor)
            }
        }
    }
}

@Composable
internal fun MainMenuVersionInfo(
    modifier: Modifier = Modifier,
    onShowCommitInfo: () -> Unit,
) {
    TooltipWrapper(
        text = stringResource(Res.string.commit_info_title),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "v${AppBuildInfo.VERSION_NAME} (${AppBuildInfo.COMMIT_HASH})",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onShowCommitInfo() },
            )
            ShortcutKeyChip(text = "V")
        }
    }
}

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onContinueGame: () -> Unit,
    hasAutosave: Boolean,
    onShowRules: () -> Unit,
    onOpenEditor: () -> Unit = {},
    onShowInstallationInfo: () -> Unit,
    onShowDownloadInfo: () -> Unit = {},
    onShowBackendInfo: () -> Unit = {},
    onOpenVillainsAnnouncement: () -> Unit = {},
    onEditPlayerName: () -> Unit,
    currentPlayerName: String?,
    iamState: IamState = IamState(),
    iamLoginInProgress: Boolean = false,
    onIamLogin: () -> Unit = {},
    onIamLogout: () -> Unit = {},
    onIamLoginCancel: () -> Unit = {},
    openSettingsInitially: Boolean = false,
    settingsInitialTab: SettingsTab = SettingsTab.GENERAL,
    onSettingsInitialOpenHandled: () -> Unit = {},
    isDataLoaded: Boolean = true,
    loadingProgress: LoadingProgress? = null,
) {
    // Track if settings hint should be shown
    val showSettingsHint by AppSettings.settingsHintShown
    val showEditorButton = isEditorAvailable()

    // Track if accessibility banner should be shown
    val showAccessibilityBanner by AppSettings.accessibilityBannerShown

    // Track if commit info dialog should be shown
    var showCommitInfo by remember { mutableStateOf(false) }

    // Track if exit confirmation dialog should be shown
    var showExitConfirmation by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Track keyboard-triggered feedback/settings/iam-logout
    var triggerFeedback by remember { mutableStateOf(false) }
    var triggerSettings by remember { mutableStateOf(false) }
    var triggerOpenAccessibilitySettings by remember { mutableStateOf(false) }
    // Computed value used by all three SettingsButton instances to open the Accessibility tab
    val accessibilityTabTrigger: SettingsTab? = if (triggerOpenAccessibilitySettings) SettingsTab.ACCESSIBILITY else null
    var triggerImpressumOpen by remember { mutableStateOf(false) }
    var triggerImpressumClose by remember { mutableStateOf(false) }
    var impressumIsOpen by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: IllegalStateException) {
        }
    }

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusTarget()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when {
                            // Enter → Start Game (if data is loaded and no dialog open)
                            event.key == Key.Enter &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                isDataLoaded &&
                                !showExitConfirmation -> {
                                onStartGame()
                                true
                            }
                            // C → Continue game (if autosave exists)
                            event.key == Key.C &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                hasAutosave &&
                                isDataLoaded &&
                                !showExitConfirmation -> {
                                onContinueGame()
                                true
                            }
                            // H → Show Rules (if no dialog open)
                            event.key == Key.H &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation -> {
                                onShowRules()
                                true
                            }
                            // I → Show Info / Installation page
                            event.key == Key.I &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation -> {
                                onShowInstallationInfo()
                                true
                            }
                            // Period (.) → Open feedback
                            event.key == Key.Period &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation -> {
                                triggerFeedback = true
                                true
                            }
                            // Comma (,) → Open settings
                            event.key == Key.Comma &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation -> {
                                triggerSettings = true
                                true
                            }
                            // L → IAM login/logout
                            event.key == Key.L &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation -> {
                                if (iamState.isAuthenticated) onIamLogout() else onIamLogin()
                                true
                            }
                            // P → Edit player name
                            event.key == Key.P &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation -> {
                                onEditPlayerName()
                                true
                            }
                            // Slash (?) → Show backend/privacy info
                            event.key == Key.Slash &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation -> {
                                onShowBackendInfo()
                                true
                            }
                            // D → Show download page (WASM only)
                            event.key == Key.D &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation &&
                                isPlatformWasm &&
                                WithImpressum.withImpressum -> {
                                onShowDownloadInfo()
                                true
                            }
                            // V → Show commit/version info
                            event.key == Key.V &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation -> {
                                showCommitInfo = true
                                true
                            }
                            // N → Toggle impressum (WASM only)
                            event.key == Key.N &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showExitConfirmation &&
                                isPlatformWasm -> {
                                if (impressumIsOpen) {
                                    triggerImpressumClose = true
                                } else {
                                    triggerImpressumOpen = true
                                }
                                impressumIsOpen = !impressumIsOpen
                                // Re-request focus on main surface so shortcuts keep working after close
                                if (!impressumIsOpen) {
                                    try {
                                        focusRequester.requestFocus()
                                    } catch (_: IllegalStateException) {
                                    }
                                }
                                true
                            }
                            // X → Dismiss accessibility banner (when banner is visible)
                            event.key == Key.X &&
                                !event.isCtrlPressed &&
                                !event.isAltPressed &&
                                !showAccessibilityBanner &&
                                !showExitConfirmation -> {
                                AppSettings.markAccessibilityBannerShown()
                                true
                            }
                            // Esc → show exit confirmation (non-iOS)
                            (event.key == Key.Escape || event.key == Key.Back) &&
                                !isPlatformIos &&
                                !showExitConfirmation -> {
                                showExitConfirmation = true
                                true
                            }
                            // Esc when exit dialog is open → cancel it
                            (event.key == Key.Escape || event.key == Key.Back) && showExitConfirmation -> {
                                showExitConfirmation = false
                                true
                            }
                            // Enter when exit dialog is open → confirm exit
                            event.key == Key.Enter && !event.isCtrlPressed && showExitConfirmation -> {
                                showExitConfirmation = false
                                exitApplication()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            val isMobileWeb = isMobileWebBrowser()
            val isPortrait = maxHeight > maxWidth
            val isNarrowWindow = maxWidth < 900.dp
            val usesStackedLayout =
                shouldUseStackedMainMenuLayout(
                    isNativeMobile = isPlatformMobile,
                    isMobileWeb = isMobileWeb,
                    isPortrait = isPortrait,
                    isNarrowWindow = isNarrowWindow,
                )
            val usesCompactLayout =
                shouldUseCompactMainMenuLayout(
                    isNativeMobile = isPlatformMobile,
                    isMobileWeb = isMobileWeb,
                    isNarrowWindow = isNarrowWindow,
                )
            val showExitGameButton = isPlatformDesktop
            // Pre-compute at BoxWithConstraints scope where maxHeight/maxWidth are in scope
            val mobileLandscapeBannerHeight = (maxHeight * 0.40f).coerceAtLeast(72.dp).coerceAtMost(140.dp)
            // Desktop banner: height = available width / (aspect-ratio × 1.5) — 2/3 of the full-width size.
            // Capped at 200dp so very wide monitors don't get an oversized banner.
            val desktopBannerHeight = (maxWidth / 4.44f).coerceAtLeast(67.dp).coerceAtMost(200.dp)
            if (usesCompactLayout) {
                MainMenuCompactLayout(
                    isPortrait = isPortrait,
                    isMobileWeb = isMobileWeb,
                    usesStackedLayout = usesStackedLayout,
                    mobileLandscapeBannerHeight = mobileLandscapeBannerHeight,
                    showExitGameButton = showExitGameButton,
                    showEditorButton = showEditorButton,
                    showAccessibilityBanner = showAccessibilityBanner,
                    triggerFeedback = triggerFeedback,
                    triggerSettings = triggerSettings,
                    accessibilityTabTrigger = accessibilityTabTrigger,
                    openSettingsInitially = openSettingsInitially,
                    settingsInitialTab = settingsInitialTab,
                    isDataLoaded = isDataLoaded,
                    hasAutosave = hasAutosave,
                    currentPlayerName = currentPlayerName,
                    iamState = iamState,
                    iamLoginInProgress = iamLoginInProgress,
                    loadingProgress = loadingProgress,
                    onStartGame = onStartGame,
                    onContinueGame = onContinueGame,
                    onShowRules = onShowRules,
                    onOpenEditor = onOpenEditor,
                    onShowDownloadInfo = onShowDownloadInfo,
                    onShowInstallationInfo = onShowInstallationInfo,
                    onShowBackendInfo = onShowBackendInfo,
                    onEditPlayerName = onEditPlayerName,
                    onIamLogin = onIamLogin,
                    onIamLogout = onIamLogout,
                    onIamLoginCancel = onIamLoginCancel,
                    onSettingsInitialOpenHandled = onSettingsInitialOpenHandled,
                    onExitGameClick = { showExitConfirmation = true },
                    onFeedbackTriggerHandled = { triggerFeedback = false },
                    onSettingsTriggerHandled = { triggerSettings = false },
                    onAccessibilityTabTriggerHandled = { triggerOpenAccessibilitySettings = false },
                    onAccessibilitySettingsRequested = { triggerOpenAccessibilitySettings = true },
                    onShowCommitInfo = { showCommitInfo = true },
                )
            } else {
                MainMenuDesktopLayout(
                    showExitGameButton = showExitGameButton,
                    showEditorButton = showEditorButton,
                    showAccessibilityBanner = showAccessibilityBanner,
                    triggerFeedback = triggerFeedback,
                    triggerSettings = triggerSettings,
                    accessibilityTabTrigger = accessibilityTabTrigger,
                    openSettingsInitially = openSettingsInitially,
                    settingsInitialTab = settingsInitialTab,
                    isDataLoaded = isDataLoaded,
                    hasAutosave = hasAutosave,
                    currentPlayerName = currentPlayerName,
                    iamState = iamState,
                    iamLoginInProgress = iamLoginInProgress,
                    loadingProgress = loadingProgress,
                    desktopBannerHeight = desktopBannerHeight,
                    onStartGame = onStartGame,
                    onContinueGame = onContinueGame,
                    onShowRules = onShowRules,
                    onOpenEditor = onOpenEditor,
                    onShowDownloadInfo = onShowDownloadInfo,
                    onShowInstallationInfo = onShowInstallationInfo,
                    onShowBackendInfo = onShowBackendInfo,
                    onEditPlayerName = onEditPlayerName,
                    onIamLogin = onIamLogin,
                    onIamLogout = onIamLogout,
                    onIamLoginCancel = onIamLoginCancel,
                    onSettingsInitialOpenHandled = onSettingsInitialOpenHandled,
                    onExitGameClick = { showExitConfirmation = true },
                    onFeedbackTriggerHandled = { triggerFeedback = false },
                    onSettingsTriggerHandled = { triggerSettings = false },
                    onAccessibilityTabTriggerHandled = { triggerOpenAccessibilitySettings = false },
                    onAccessibilitySettingsRequested = { triggerOpenAccessibilitySettings = true },
                )
            }

            // Version info at the bottom on desktop/mobile-web - clickable to show commit info
            if (shouldShowOverlayMainMenuVersionInfo(usesStackedLayout, isMobileWeb)) {
                MainMenuVersionInfo(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 8.dp),
                    onShowCommitInfo = { showCommitInfo = true },
                )
            }

            // Impressum at bottom center (WASM only, when flag is enabled)
            if (isPlatformWasm) {
                ImpressumWrapper(
                    rowModifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                    triggerOpen = triggerImpressumOpen,
                    onTriggerOpenHandled = { triggerImpressumOpen = false },
                    triggerClose = triggerImpressumClose,
                    onTriggerCloseHandled = { triggerImpressumClose = false },
                    onDismissed = {
                        impressumIsOpen = false
                        try {
                            focusRequester.requestFocus()
                        } catch (_: IllegalStateException) {
                        }
                    },
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
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 56.dp, end = 8.dp)
                            .heightIn(max = (maxHeight - 56.dp).coerceAtLeast(140.dp)),
                )
            }
            VillainsAnnouncementTriangle(
                modifier = Modifier.fillMaxSize(),
                onClick = onOpenVillainsAnnouncement,
            )
        }

        // Exit confirmation dialog
        if (showExitConfirmation) {
            val exitDialogFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                try {
                    exitDialogFocusRequester.requestFocus()
                } catch (_: IllegalStateException) {
                }
            }
            AlertDialog(
                onDismissRequest = { showExitConfirmation = false },
                modifier =
                    Modifier
                        .focusRequester(exitDialogFocusRequester)
                        .focusTarget()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.Enter -> {
                                        showExitConfirmation = false
                                        exitApplication()
                                        true
                                    }
                                    Key.Escape, Key.Back -> {
                                        showExitConfirmation = false
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        },
                title = { Text(stringResource(Res.string.exit_game_confirm_title)) },
                text = { Text(stringResource(Res.string.exit_game_confirm_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitConfirmation = false
                            exitApplication()
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(stringResource(Res.string.exit))
                            ShortcutKeyChip(
                                text = "Enter",
                                color = LocalContentColor.current.copy(alpha = 0.75f),
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirmation = false }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(stringResource(Res.string.cancel))
                            ShortcutKeyChip(
                                text = "Esc",
                                color = LocalContentColor.current.copy(alpha = 0.75f),
                            )
                        }
                    }
                },
            )
        }

        // Commit info dialog
        if (showCommitInfo) {
            CommitInfoDialog(
                onDismiss = { showCommitInfo = false },
            )
        }
    }
}
