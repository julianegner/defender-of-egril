@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.iam.IamState
import de.egril.defender.save.PlayerProfile
import de.egril.defender.ui.ProfileTabScrollbar
import de.egril.defender.ui.feedback.FeedbackButton
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.icon.UnlockIcon
import de.egril.defender.ui.isMobileWebBrowser
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.launch

/**
 * Screen displaying player profile information and achievements
 */
@Composable
fun PlayerProfileScreen(
    playerProfile: PlayerProfile,
    onBack: () -> Unit,
    onEditName: () -> Unit,
    onSelectPlayer: () -> Unit = {},
    onNavigateToStats: (() -> Unit)? = null,
    onUpgradeAbility: ((de.egril.defender.model.AbilityType) -> Unit)? = null,
    onUnlockSpell: ((de.egril.defender.model.SpellType) -> Unit)? = null,
    iamState: IamState = IamState(),
    iamLoginInProgress: Boolean = false,
    onIamLogin: () -> Unit = {},
    onIamLogout: () -> Unit = {},
    onManageAccount: () -> Unit = {},
    onAlwaysLoginChanged: (Boolean) -> Unit = {},
    onUseRemoteSettingsChanged: (Boolean) -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val focusRequester = remember { FocusRequester() }
        val scope = rememberCoroutineScope()
        // Keyboard-triggered open triggers for feedback/settings
        var triggerFeedback by remember { mutableStateOf(false) }
        var triggerSettings by remember { mutableStateOf(false) }
        // Tab index state hoisted here for keyboard access
        var selectedTabIndex by remember { mutableStateOf(1) }
        var headerCollapsed by remember { mutableStateOf(false) }
        // Shared scroll state for keyboard arrow scrolling
        val tabScrollState = rememberScrollState()

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when {
                                // Escape / Back → go back
                                event.key == Key.Escape || event.key == Key.Back -> {
                                    onBack()
                                    true
                                }
                                // Left/Right arrows → switch tabs
                                event.key == Key.DirectionLeft && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    if (selectedTabIndex > 0) selectedTabIndex--
                                    true
                                }
                                event.key == Key.DirectionRight && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    if (selectedTabIndex < 1) selectedTabIndex++
                                    true
                                }
                                // Period (.) → Open feedback
                                event.key == Key.Period && !event.isCtrlPressed -> {
                                    triggerFeedback = true
                                    true
                                }
                                // Comma (,) → Open settings
                                event.key == Key.Comma && !event.isCtrlPressed -> {
                                    triggerSettings = true
                                    true
                                }
                                // K → Toggle collapse/expand header
                                event.key == Key.K && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    headerCollapsed = !headerCollapsed
                                    true
                                }
                                // E → Edit player name
                                event.key == Key.E && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    onEditName()
                                    true
                                }
                                // A → Toggle always log in
                                event.key == Key.A && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    onAlwaysLoginChanged(!playerProfile.alwaysLogin)
                                    true
                                }
                                // R → Toggle use remote settings
                                event.key == Key.R && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    onUseRemoteSettingsChanged(!playerProfile.useRemoteSettings)
                                    true
                                }
                                // S → Switch player
                                event.key == Key.S && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    onSelectPlayer()
                                    true
                                }
                                // M → Manage account
                                event.key == Key.M && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    onManageAccount()
                                    true
                                }
                                // L → Logout
                                event.key == Key.L && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    onIamLogout()
                                    true
                                }
                                // Up/Down arrows → scroll tab content
                                event.key == Key.DirectionUp && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    scope.launch { tabScrollState.animateScrollTo((tabScrollState.value - 120).coerceAtLeast(0)) }
                                    true
                                }
                                event.key == Key.DirectionDown && !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed -> {
                                    scope.launch {
                                        tabScrollState.animateScrollTo(
                                            (tabScrollState.value + 120).coerceAtMost(tabScrollState.maxValue),
                                        )
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
        ) {
            LaunchedEffect(Unit) {
                try {
                    focusRequester.requestFocus()
                } catch (_: IllegalStateException) {
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header: toggle between full and compact view

                if (headerCollapsed) {
                    // Compact single-row header: name | remote account | XP | ability points
                    val remoteAccountName =
                        if (iamState.isAuthenticated && iamState.username != null) {
                            iamState.username
                        } else {
                            playerProfile.remoteUsername
                        }
                    val stats = playerProfile.abilities
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Player name and info on the LEFT, taking remaining space
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SelectionContainer {
                                // Player name
                                Text(
                                    text = playerProfile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (remoteAccountName != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        UnlockIcon(size = 12.dp)
                                        Text(
                                            text = remoteAccountName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                }
                                // XP
                                Text(
                                    text =
                                        stringResource(
                                            Res.string.xp_progress,
                                            stats.totalXP,
                                            de.egril.defender.model.PlayerAbilities
                                                .getXPForNextLevel(stats.level),
                                        ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                // Available ability points
                                if (stats.availableAbilityPoints > 0) {
                                    Text(
                                        text =
                                            stringResource(
                                                Res.string.available_stat_points,
                                                stats.availableAbilityPoints,
                                            ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        // Expand button
                        TextButton(
                            onClick = { headerCollapsed = false },
                            modifier = Modifier.defaultMinSize(minHeight = 28.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.expand),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "K", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                        // Switch player button
                        OutlinedButton(
                            onClick = onSelectPlayer,
                            modifier = Modifier.defaultMinSize(minHeight = 28.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.switch_player),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "S", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                        // Feedback and Settings buttons
                        FeedbackButton(
                            shortcutKey = ".",
                            triggerOpen = triggerFeedback,
                            onTriggerHandled = { triggerFeedback = false },
                        )
                        SettingsButton(
                            shortcutKey = ",",
                            triggerOpen = triggerSettings,
                            onTriggerHandled = { triggerSettings = false },
                        )
                    }
                } else {
                    // Full header with player name title, switch player button, and collapse button
                    val isMobileUI = isPlatformMobile || isMobileWebBrowser()
                    val headerBottomPadding = if (isMobileUI) 8.dp else 24.dp
                    val headerButtonHeight = if (isMobileUI) 28.dp else 36.dp
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = headerBottomPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Title on the LEFT, centered in remaining space
                        Text(
                            text = stringResource(Res.string.player_profile),
                            style = if (isMobileUI) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        // Collapse button
                        TextButton(
                            onClick = { headerCollapsed = true },
                            modifier = Modifier.defaultMinSize(minHeight = headerButtonHeight),
                        ) {
                            Text(
                                text = stringResource(Res.string.collapse),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "K", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                        // Switch player button
                        OutlinedButton(
                            onClick = onSelectPlayer,
                            modifier = Modifier.defaultMinSize(minHeight = headerButtonHeight),
                        ) {
                            Text(
                                text = stringResource(Res.string.switch_player),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "S", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                        // Feedback and Settings buttons
                        FeedbackButton(
                            shortcutKey = ".",
                            triggerOpen = triggerFeedback,
                            onTriggerHandled = { triggerFeedback = false },
                        )
                        SettingsButton(
                            shortcutKey = ",",
                            triggerOpen = triggerSettings,
                            onTriggerHandled = { triggerSettings = false },
                        )
                    }
                }

                // Scrollable content

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                ) {
                    if (!headerCollapsed) {
                        // Cards: side-by-side on landscape, stacked+scrollable on portrait
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            SelectionContainer {
                                val isPortrait = maxHeight > maxWidth
                                if (isPortrait) {
                                    // Mobile/portrait: stacked, scrollable
                                    val cardsScrollState = rememberScrollState()
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .verticalScroll(cardsScrollState),
                                    ) {
                                        PlayerInfoCard(
                                            playerProfile = playerProfile,
                                            onEditName = onEditName,
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        UserAccountCard(
                                            iamState = iamState,
                                            iamLoginInProgress = iamLoginInProgress,
                                            onIamLogin = onIamLogin,
                                            onIamLogout = onIamLogout,
                                            onManageAccount = onManageAccount,
                                            alwaysLogin = playerProfile.alwaysLogin,
                                            onAlwaysLoginChanged = onAlwaysLoginChanged,
                                            useRemoteSettings = playerProfile.useRemoteSettings,
                                            onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                                        )
                                    }
                                } else {
                                    // Desktop/landscape: side by side, ~30% profile / ~70% account
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Box(modifier = Modifier.weight(0.3f)) {
                                            PlayerInfoCard(
                                                playerProfile = playerProfile,
                                                onEditName = onEditName,
                                            )
                                        }
                                        Box(modifier = Modifier.weight(0.7f)) {
                                            UserAccountCard(
                                                iamState = iamState,
                                                iamLoginInProgress = iamLoginInProgress,
                                                onIamLogin = onIamLogin,
                                                onIamLogout = onIamLogout,
                                                onManageAccount = onManageAccount,
                                                alwaysLogin = playerProfile.alwaysLogin,
                                                onAlwaysLoginChanged = onAlwaysLoginChanged,
                                                useRemoteSettings = playerProfile.useRemoteSettings,
                                                onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Tabs (only show if stats callback is provided)
                    if (onNavigateToStats != null) {
                        PrimaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        ShortcutKeyChip(text = "\u2190", color = LocalContentColor.current.copy(alpha = 0.6f))
                                        Text(stringResource(Res.string.achievements))
                                    }
                                },
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(stringResource(Res.string.abilities))
                                        ShortcutKeyChip(text = "\u2192", color = LocalContentColor.current.copy(alpha = 0.6f))
                                    }
                                },
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tab content (scrollable) with vertical scrollbar
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(tabScrollState)
                                        .padding(end = 12.dp),
                            ) {
                                when (selectedTabIndex) {
                                    0 -> {
                                        // Achievements tab - show achievements directly
                                        AchievementsListDirect(achievements = playerProfile.achievements)
                                    }
                                    1 -> {
                                        // Stats & Abilities tab - show stats directly if callbacks provided
                                        if (onUpgradeAbility != null && onUnlockSpell != null) {
                                            StatsAndAbilitiesContent(
                                                playerProfile = playerProfile,
                                                onUpgradeAbility = onUpgradeAbility,
                                                onUnlockSpell = onUnlockSpell,
                                            )
                                        } else {
                                            // Fallback to button if callbacks not provided
                                            Button(
                                                onClick = onNavigateToStats,
                                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary,
                                                    ),
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.abilities),
                                                    style = MaterialTheme.typography.titleMedium,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            ProfileTabScrollbar(scrollState = tabScrollState)
                        }
                    } else {
                        // No stats callback - just show achievements directly (old behavior)
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(tabScrollState)
                                        .padding(end = 12.dp),
                            ) {
                                AchievementsListDirect(achievements = playerProfile.achievements)
                            }
                            ProfileTabScrollbar(scrollState = tabScrollState)
                        }
                    }
                }

                // Back button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                ) {
                    Text(stringResource(Res.string.back))
                    if (AppSettings.showButtonShortcutHints.value) {
                        Spacer(modifier = Modifier.width(4.dp))
                        ShortcutKeyChip(text = "Esc", color = LocalContentColor.current.copy(alpha = 0.75f))
                    }
                }

                // Navigation hints (shown when shortcut hints are ON)
                if (AppSettings.showButtonShortcutHints.value) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left/Right → Switch tab
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ShortcutKeyChip(text = "\u2190/\u2192")
                            Text(
                                text = stringResource(Res.string.keyboard_nav_switch_tab),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // Up/Down → Scroll
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ShortcutKeyChip(text = "\u2191/\u2193")
                            Text(
                                text = stringResource(Res.string.keyboard_nav_scroll),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
