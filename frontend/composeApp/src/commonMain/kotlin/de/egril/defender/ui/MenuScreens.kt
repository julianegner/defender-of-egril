@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import de.egril.defender.ui.icon.TrophyIcon
import dev.vicart.compose.material.symbols.FilledSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.ui.settings.SettingsHintBox
import de.egril.defender.ui.settings.SettingsTab
import de.egril.defender.ui.feedback.FeedbackButton
import de.egril.defender.utils.isPlatformDesktop
import de.egril.defender.utils.isPlatformMobile
import de.egril.defender.utils.isPlatformWasm
import de.egril.defender.ui.isMobileWebBrowser
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import com.hyperether.resources.stringResource
import de.egril.defender.ui.icon.HeartIcon
import de.egril.defender.ui.icon.HelpIcon
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.settings.AppSettings.isDarkMode
import de.egril.defender.utils.isPlatformIos
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.emoji_sword
import defender_of_egril.composeapp.generated.resources.emoji_skull
import org.jetbrains.compose.resources.painterResource


internal fun shouldUseStackedMainMenuLayout(
    isNativeMobile: Boolean,
    isMobileWeb: Boolean,
    isPortrait: Boolean
): Boolean = isNativeMobile || (isMobileWeb && isPortrait)

internal fun shouldUseCompactMainMenuLayout(
    isNativeMobile: Boolean,
    isMobileWeb: Boolean
): Boolean = isNativeMobile || isMobileWeb

/** Returns true when LevelComplete screen should use a compact, scrollable mobile layout. */
internal fun shouldUseMobileLevelCompleteLayout(
    isNativeMobile: Boolean,
    isMobileWeb: Boolean
): Boolean = isNativeMobile || isMobileWeb

/** Returns true when LevelComplete screen buttons should be stacked vertically (portrait phone or narrow mobile web). */
internal fun shouldStackLevelCompleteButtons(
    isMobileLayout: Boolean,
    isPortrait: Boolean
): Boolean = isMobileLayout && isPortrait

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
            if (AppSettings.showButtonShortcutHints.value) {
                Spacer(modifier = Modifier.width(4.dp))
                de.egril.defender.ui.gameplay.ShortcutKeyChip(
                    text = "Enter",
                    color = LocalContentColor.current.copy(alpha = 0.75f)
                )
            }
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
                if (AppSettings.showButtonShortcutHints.value) {
                    Spacer(modifier = Modifier.width(4.dp))
                    de.egril.defender.ui.gameplay.ShortcutKeyChip(
                        text = "C",
                        color = LocalContentColor.current.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Button(
            onClick = onShowRules,
            modifier = Modifier.weight(1f).height(buttonHeight),
            contentPadding = contentPadding ?: ButtonDefaults.ContentPadding
        ) {
            Text(stringResource(Res.string.rules), style = textStyle, maxLines = 1)
            if (AppSettings.showButtonShortcutHints.value) {
                Spacer(modifier = Modifier.width(4.dp))
                de.egril.defender.ui.gameplay.ShortcutKeyChip(
                    text = "H",
                    color = LocalContentColor.current.copy(alpha = 0.75f)
                )
            }
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
    openSettingsInitially: Boolean = false,
    settingsInitialTab: SettingsTab = SettingsTab.GENERAL,
    onSettingsInitialOpenHandled: () -> Unit = {},
    isDataLoaded: Boolean = true,
    loadingProgress: LoadingProgress? = null
) {
    // Track if settings hint should be shown
    val showSettingsHint by AppSettings.settingsHintShown
    
    // Track if commit info dialog should be shown
    var showCommitInfo by remember { mutableStateOf(false) }
    
    // Track if exit confirmation dialog should be shown
    var showExitConfirmation by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    // Track keyboard-triggered feedback/settings/iam-logout
    var triggerFeedback by remember { mutableStateOf(false) }
    var triggerSettings by remember { mutableStateOf(false) }
    var triggerImpressumOpen by remember { mutableStateOf(false) }
    var triggerImpressumClose by remember { mutableStateOf(false) }
    var impressumIsOpen by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: IllegalStateException) {}
    }

    Surface(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when {
                    // Enter → Start Game (if data is loaded and no dialog open)
                    event.key == Key.Enter && !event.isCtrlPressed && !event.isAltPressed
                            && isDataLoaded && !showExitConfirmation -> {
                        onStartGame()
                        true
                    }
                    // C → Continue game (if autosave exists)
                    event.key == Key.C && !event.isCtrlPressed && !event.isAltPressed
                            && hasAutosave && isDataLoaded && !showExitConfirmation -> {
                        onContinueGame()
                        true
                    }
                    // H → Show Rules (if no dialog open)
                    event.key == Key.H && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation -> {
                        onShowRules()
                        true
                    }
                    // I → Show Info / Installation page
                    event.key == Key.I && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation -> {
                        onShowInstallationInfo()
                        true
                    }
                    // Period (.) → Open feedback
                    event.key == Key.Period && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation -> {
                        triggerFeedback = true
                        true
                    }
                    // Comma (,) → Open settings
                    event.key == Key.Comma && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation -> {
                        triggerSettings = true
                        true
                    }
                    // L → IAM login/logout
                    event.key == Key.L && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation -> {
                        if (iamState.isAuthenticated) onIamLogout() else onIamLogin()
                        true
                    }
                    // P → Edit player name
                    event.key == Key.P && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation -> {
                        onEditPlayerName()
                        true
                    }
                    // Slash (?) → Show backend/privacy info
                    event.key == Key.Slash && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation -> {
                        onShowBackendInfo()
                        true
                    }
                    // D → Show download page (WASM only)
                    event.key == Key.D && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation && isPlatformWasm && WithImpressum.withImpressum -> {
                        onShowDownloadInfo()
                        true
                    }
                    // V → Show commit/version info
                    event.key == Key.V && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation -> {
                        showCommitInfo = true
                        true
                    }
                    // N → Toggle impressum (WASM only)
                    event.key == Key.N && !event.isCtrlPressed && !event.isAltPressed
                            && !showExitConfirmation && isPlatformWasm -> {
                        if (impressumIsOpen) {
                            triggerImpressumClose = true
                        } else {
                            triggerImpressumOpen = true
                        }
                        impressumIsOpen = !impressumIsOpen
                        // Re-request focus on main surface so shortcuts keep working after close
                        if (!impressumIsOpen) {
                            try { focusRequester.requestFocus() } catch (_: IllegalStateException) {}
                        }
                        true
                    }
                    // Esc → show exit confirmation (non-iOS)
                    (event.key == Key.Escape || event.key == Key.Back)
                            && !isPlatformIos && !showExitConfirmation -> {
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
            } else false
        },
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            val isMobileWeb = isMobileWebBrowser()
            val isPortrait = maxHeight > maxWidth
            val usesStackedLayout = shouldUseStackedMainMenuLayout(
                isNativeMobile = isPlatformMobile,
                isMobileWeb = isMobileWeb,
                isPortrait = isPortrait
            )
            val usesCompactLayout = shouldUseCompactMainMenuLayout(
                isNativeMobile = isPlatformMobile,
                isMobileWeb = isMobileWeb
            )
            val showExitGameButton = isPlatformDesktop
            // Pre-compute at BoxWithConstraints scope where maxHeight/maxWidth are in scope
            val mobileLandscapeBannerHeight = (maxHeight * 0.40f).coerceAtLeast(72.dp).coerceAtMost(140.dp)
            // Desktop banner: height = available width / (aspect-ratio × 1.5) — 2/3 of the full-width size.
            // Capped at 200dp so very wide monitors don't get an oversized banner.
            val desktopBannerHeight = (maxWidth / 4.44f).coerceAtLeast(67.dp).coerceAtMost(200.dp)
            if (usesCompactLayout) {
                // ===== MOBILE / MOBILE-WEB LAYOUT =====
                // Landscape: 3-column Row (left controls | center banner | right controls).
                // Portrait: controls row at top, then full-width banner below.
                // In both cases the banner height is capped so the scrollable content gets adequate room.
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isPortrait) {
                        // LANDSCAPE: banner sits in the center column, height derived from available space
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left column: exit button, player name, IAM login + help buttons
                            Column(
                                modifier = Modifier.padding(end = 4.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                if (showExitGameButton) {
                                    Button(
                                        onClick = { showExitConfirmation = true },
                                        modifier = Modifier.height(32.dp).widthIn(min = 80.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.exit_game),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = currentPlayerName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (iamState.isAuthenticated) {
                                            TooltipWrapper(text = stringResource(Res.string.tooltip_log_out_from_remote)) {
                                                OutlinedButton(
                                                    onClick = onIamLogout,
                                                    modifier = Modifier.height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    LockIcon(size = 12.dp)
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(text = stringResource(Res.string.iam_logout), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        } else if (iamLoginInProgress) {
                                            OutlinedButton(
                                                onClick = onIamLoginCancel,
                                                modifier = Modifier.height(30.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(text = stringResource(Res.string.iam_login_waiting), style = MaterialTheme.typography.labelSmall)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = onIamLogin,
                                                modifier = Modifier.height(30.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                UnlockIcon(size = 12.dp)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(text = stringResource(Res.string.iam_login), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        val backendInfoDesc = stringResource(Res.string.backend_info_title)
                                        TooltipWrapper(text = backendInfoDesc) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = onShowBackendInfo,
                                                    modifier = Modifier.size(28.dp).semantics { contentDescription = backendInfoDesc }
                                                ) {
                                                    HelpIcon(size = 28.dp, tint = if (isDarkMode.value) Color(0xFFB3E5FC) else Color.Blue)
                                                }
                                                    ShortcutKeyChip(text = "?")
                                            }
                                        }
                                    }
                                }
                            }

                            // Center: banner scales to fill the available width within the fixed height
                            Box(
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
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
                                            modifier = Modifier.size(40.dp).semantics { contentDescription = installationInfoLabel }
                                        ) {
                                            FilledSymbol(icon = MaterialSymbols.INFO, size = 28.dp)
                                        }
                                        if (AppSettings.showButtonShortcutHints.value) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            ShortcutKeyChip(text = "I")
                                        }
                                    }
                                }
                                FeedbackButton(
                                    shortcutKey = ".",
                                    triggerOpen = triggerFeedback,
                                    onTriggerHandled = { triggerFeedback = false }
                                )
                                SettingsButton(
                                    initiallyOpen = openSettingsInitially,
                                    initialTab = settingsInitialTab,
                                    onInitialOpenHandled = onSettingsInitialOpenHandled,
                                    shortcutKey = ",",
                                    triggerOpen = triggerSettings,
                                    onTriggerHandled = { triggerSettings = false }
                                )
                            }
                        }
                    } else {
                        // PORTRAIT: controls row at top, then full-width banner below
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left column: exit button, player name, IAM login + help buttons
                            Column(
                                modifier = Modifier.padding(end = 4.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                if (showExitGameButton) {
                                    Button(
                                        onClick = { showExitConfirmation = true },
                                        modifier = Modifier.height(32.dp).widthIn(min = 80.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.exit_game),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = currentPlayerName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (iamState.isAuthenticated) {
                                            TooltipWrapper(text = stringResource(Res.string.tooltip_log_out_from_remote)) {
                                                OutlinedButton(
                                                    onClick = onIamLogout,
                                                    modifier = Modifier.height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    LockIcon(size = 12.dp)
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(text = stringResource(Res.string.iam_logout), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        } else if (iamLoginInProgress) {
                                            OutlinedButton(
                                                onClick = onIamLoginCancel,
                                                modifier = Modifier.height(30.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(text = stringResource(Res.string.iam_login_waiting), style = MaterialTheme.typography.labelSmall)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = onIamLogin,
                                                modifier = Modifier.height(30.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                UnlockIcon(size = 12.dp)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(text = stringResource(Res.string.iam_login), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        val backendInfoDesc = stringResource(Res.string.backend_info_title)
                                        TooltipWrapper(text = backendInfoDesc) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = onShowBackendInfo,
                                                    modifier = Modifier.size(28.dp).semantics { contentDescription = backendInfoDesc }
                                                ) {
                                                    HelpIcon(size = 28.dp, tint = if (isDarkMode.value) Color(0xFFB3E5FC) else Color.Blue)
                                                }
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
                                            modifier = Modifier.size(40.dp).semantics { contentDescription = installationInfoLabel }
                                        ) {
                                            FilledSymbol(icon = MaterialSymbols.INFO, size = 28.dp)
                                        }
                                        if (AppSettings.showButtonShortcutHints.value) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            ShortcutKeyChip(text = "I")
                                        }
                                    }
                                }
                                FeedbackButton(
                                    shortcutKey = ".",
                                    triggerOpen = triggerFeedback,
                                    onTriggerHandled = { triggerFeedback = false }
                                )
                                SettingsButton(
                                    initiallyOpen = openSettingsInitially,
                                    initialTab = settingsInitialTab,
                                    onInitialOpenHandled = onSettingsInitialOpenHandled,
                                    shortcutKey = ",",
                                    triggerOpen = triggerSettings,
                                    onTriggerHandled = { triggerSettings = false }
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
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.app_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MainMenuButtonRow(
                            onStartGame = onStartGame,
                            onContinueGame = onContinueGame,
                            hasAutosave = hasAutosave,
                            isDataLoaded = isDataLoaded,
                            onShowRules = onShowRules,
                            buttonHeight = if (isPlatformMobile) 40.dp else 34.dp,
                            textStyle = if (isPlatformMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isPlatformWasm && WithImpressum.withImpressum) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onShowDownloadInfo,
                                modifier = Modifier.width(200.dp).height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                    Text(text = stringResource(Res.string.loading_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
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
                        // Bottom spacer for version/impressum overlay clearance
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            } else {
                // ===== DESKTOP LAYOUT =====
                // Settings and Info buttons in top-right corner
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val installationInfoLabel = stringResource(Res.string.tooltip_info_installation)
                    TooltipWrapper(text = installationInfoLabel) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onShowInstallationInfo,
                                modifier = Modifier.size(48.dp).semantics { contentDescription = installationInfoLabel }
                            ) {
                                FilledSymbol(icon = MaterialSymbols.INFO, size = 32.dp)
                            }
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(2.dp))
                                ShortcutKeyChip(text = "I")
                            }
                        }
                    }
                    FeedbackButton(
                        shortcutKey = ".",
                        triggerOpen = triggerFeedback,
                        onTriggerHandled = { triggerFeedback = false }
                    )
                    SettingsButton(
                        initiallyOpen = openSettingsInitially,
                        initialTab = settingsInitialTab,
                        onInitialOpenHandled = onSettingsInitialOpenHandled,
                        shortcutKey = ",",
                        triggerOpen = triggerSettings,
                        onTriggerHandled = { triggerSettings = false }
                    )
                }

                // Exit button in top-left corner
                if (showExitGameButton) {
                    Button(
                        onClick = { showExitConfirmation = true },
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp).height(32.dp).widthIn(min = 80.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 48.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.clickable { onEditPlayerName() }) {
                            Text(text = stringResource(Res.string.player_name), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = currentPlayerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    ShortcutKeyChip(text = "P")
                            }
                            if (iamState.isAuthenticated && iamState.username != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    UnlockIcon(size = 12.dp)
                                    Text(text = iamState.username, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                        if (iamState.isAuthenticated) {
                            TooltipWrapper(text = stringResource(Res.string.tooltip_log_out_from_remote)) {
                                OutlinedButton(onClick = onIamLogout, modifier = Modifier.height(36.dp)) {
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
                            OutlinedButton(onClick = onIamLoginCancel, modifier = Modifier.height(36.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(Res.string.iam_login_waiting), style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            OutlinedButton(onClick = onIamLogin, modifier = Modifier.height(36.dp)) {
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
                                IconButton(onClick = onShowBackendInfo, modifier = Modifier.size(34.dp).semantics { contentDescription = backendInfoDesc }) {
                                    HelpIcon(size = 34.dp, tint = if (isDarkMode.value) Color(0xFFB3E5FC) else Color.Blue)
                                }
                                    ShortcutKeyChip(text = "?")
                            }
                        }
                    }
                }

                // Desktop centered column with banner + subtitle + buttons
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ApplicationBannerImage(modifier = Modifier.height(desktopBannerHeight))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.app_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(onClick = onStartGame, enabled = isDataLoaded, modifier = Modifier.defaultMinSize(minWidth = 200.dp, minHeight = 60.dp)) {
                            Text(stringResource(Res.string.start_game), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Spacer(modifier = Modifier.width(4.dp))
                            ShortcutKeyChip(text = "Enter", color = LocalContentColor.current.copy(alpha = 0.75f))
                        }
                        if (hasAutosave) {
                            Button(
                                onClick = onContinueGame,
                                modifier = Modifier.defaultMinSize(minWidth = 200.dp, minHeight = 60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(stringResource(Res.string.continue_game), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "C", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onShowRules, modifier = Modifier.defaultMinSize(minWidth = 200.dp, minHeight = 60.dp)) {
                        Text(stringResource(Res.string.rules), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Spacer(modifier = Modifier.width(4.dp))
                        ShortcutKeyChip(text = "H", color = LocalContentColor.current.copy(alpha = 0.75f))
                    }
                    if (isPlatformWasm && WithImpressum.withImpressum) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onShowDownloadInfo,
                            modifier = Modifier.defaultMinSize(minWidth = 200.dp, minHeight = 60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Text(text = stringResource(Res.string.loading_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
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
                }
            }
            
            // Version info at the bottom on desktop/mobile-web - clickable to show commit info
            if (!usesStackedLayout || isMobileWeb) {
                TooltipWrapper(
                    text = stringResource(Res.string.commit_info_title),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "v${AppBuildInfo.VERSION_NAME} (${AppBuildInfo.COMMIT_HASH})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { showCommitInfo = true }
                        )
                        ShortcutKeyChip(text = "V")
                    }
                }
            }
            
            // Impressum at bottom center (WASM only, when flag is enabled)
            if (isPlatformWasm) {
                ImpressumWrapper(
                    rowModifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    triggerOpen = triggerImpressumOpen,
                    onTriggerOpenHandled = { triggerImpressumOpen = false },
                    triggerClose = triggerImpressumClose,
                    onTriggerCloseHandled = { triggerImpressumClose = false },
                    onDismissed = {
                        impressumIsOpen = false
                        try { focusRequester.requestFocus() } catch (_: IllegalStateException) {}
                    }
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
                        .padding(top = 56.dp, end = 8.dp)
                        .heightIn(max = (maxHeight - 56.dp).coerceAtLeast(140.dp))
                )
            }
        }
        
        // Exit confirmation dialog
        if (showExitConfirmation) {
            val exitDialogFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                try { exitDialogFocusRequester.requestFocus() } catch (_: IllegalStateException) {}
            }
            AlertDialog(
                onDismissRequest = { showExitConfirmation = false },
                modifier = Modifier
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
                        } else false
                    },
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(stringResource(Res.string.exit))
                                ShortcutKeyChip(
                                    text = "Enter",
                                    color = LocalContentColor.current.copy(alpha = 0.75f)
                                )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirmation = false }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(stringResource(Res.string.cancel))
                                ShortcutKeyChip(
                                    text = "Esc",
                                    color = LocalContentColor.current.copy(alpha = 0.75f)
                                )
                        }
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
    newPlayerLevel: Int = 0,
    playerLevelGained: Int = 0,
    abilityPointsGained: Int = 0,
    onRestart: () -> Unit,
    onBackToMap: () -> Unit,
    onNextLevel: (() -> Unit)? = null,
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
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            val isMobileLayout = remember { shouldUseMobileLevelCompleteLayout(
                isNativeMobile = isPlatformMobile,
                isMobileWeb = isMobileWebBrowser()
            ) }
            val isPortrait = maxHeight > maxWidth
            val stackButtons = shouldStackLevelCompleteButtons(
                isMobileLayout = isMobileLayout,
                isPortrait = isPortrait
            )

            // Settings and Feedback buttons in top-right corner (hidden in demo mode to keep UI clean)
            if (!isDemoMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedbackButton(shortcutKey = ".")
                SettingsButton(shortcutKey = ",")
            }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isMobileLayout) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isMobileLayout) Arrangement.Top else Arrangement.Center
            ) {
                if (isMobileLayout) Spacer(modifier = Modifier.height(8.dp))

                // Icon/Image
                val iconSize = if (isMobileLayout) 40.dp else 64.dp
                when {
                    won && isLastLevel -> TrophyIcon(size = iconSize)
                    won -> Image(painter = painterResource(Res.drawable.emoji_sword), contentDescription = title, modifier = Modifier.size(iconSize))
                    else -> Image(painter = painterResource(Res.drawable.emoji_skull), contentDescription = title, modifier = Modifier.size(iconSize))
                }

                
                Spacer(modifier = Modifier.height(if (isMobileLayout) 8.dp else 16.dp))
                
                Text(
                    text = title,
                    style = if (isMobileLayout) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(if (isMobileLayout) 8.dp else 16.dp))
                
                Text(
                    text = message,
                    style = if (isMobileLayout) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                if (xpEarned > 0) {
                    LevelCompleteXpSummary(
                        xpEarned = xpEarned,
                        newPlayerLevel = newPlayerLevel,
                        playerLevelGained = playerLevelGained,
                        abilityPointsGained = abilityPointsGained
                    )
                }
                
                // Check if this is level 5 (Dark Magic Rises) to show XP system unlock message
                val isDarkMagicRisesLevel = levelId == 5  // Assuming level 5 ID
                if (won && isDarkMagicRisesLevel) {
                    Spacer(modifier = Modifier.height(if (isMobileLayout) 8.dp else 16.dp))
                    
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
                
                Spacer(modifier = Modifier.height(if (isMobileLayout) 16.dp else 48.dp))
                
                if (isDemoMode) {
                    // In demo mode, show a hint that the user can click to stop
                    Text(
                        text = stringResource(Res.string.demo_click_to_stop),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val btnHeight = if (isMobileLayout) 44.dp else 50.dp
                    val btnWidth = if (isMobileLayout) 120.dp else 150.dp
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.width(btnWidth).height(btnHeight)
                    ) {
                        Text(stringResource(Res.string.retry))
                    }
                    
                    Button(
                        onClick = onBackToMap,
                        modifier = Modifier.width(btnWidth).height(btnHeight)
                    ) {
                        Text(stringResource(Res.string.world_map))
                    }
                    
                    if (won && onNextLevel != null) {
                        Button(
                            onClick = onNextLevel,
                            modifier = Modifier.width(btnWidth).height(btnHeight)
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
                onDismiss = { showStopDemoDialog = false }
            )
        }
    }
}

@Composable
private fun LevelCompleteXpSummary(
    xpEarned: Int,
    newPlayerLevel: Int,
    playerLevelGained: Int,
    abilityPointsGained: Int
) {
    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(Res.string.xp_earned, xpEarned),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = Color.Gray
    )

    if (playerLevelGained > 0) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.level_up_reached, newPlayerLevel),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }

    if (abilityPointsGained > 0) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.ability_points_gained, abilityPointsGained),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(Res.string.xp_info_brief),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = Color.Gray.copy(alpha = 0.8f)
    )
}
