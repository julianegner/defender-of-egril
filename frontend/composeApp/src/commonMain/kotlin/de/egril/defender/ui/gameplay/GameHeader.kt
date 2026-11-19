package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.hyperether.resources.stringResource
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.TooltipWrapper
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.feedback.FeedbackButton
import de.egril.defender.ui.icon.HelpIcon
import de.egril.defender.ui.icon.KeyboardKeyIcon
import de.egril.defender.ui.icon.SaveIcon
import de.egril.defender.ui.icon.SwordIcon
import de.egril.defender.ui.icon.ToolsIcon
import de.egril.defender.ui.icon.TriangleLeftIcon
import de.egril.defender.ui.icon.TriangleRightIcon
import de.egril.defender.ui.infopage.HowToPlayContent
import de.egril.defender.ui.infopage.KeyboardShortcutsInfo
import de.egril.defender.ui.isMobileWebBrowser
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.DifficultyDisplay
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.ui.settings.formatShortcutBindingForDisplay
import de.egril.defender.utils.isLimitedInputDevice
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.launch

@Composable
fun GameHeader(
    gameState: GameState,
    showOverlay: Boolean,
    onShowOverlayChange: (Boolean) -> Unit,
    onBackToMap: () -> Unit,
    onSaveGame: (() -> Unit)?,
    onCheatCode: (() -> Unit)?,
    onEnemyCountClick: (() -> Unit)? = null,
    onManaClick: (() -> Unit)? = null,
    onWinLevelInfoClick: (() -> Unit)? = null,
    onSandboxTools: (() -> Unit)? = null,
    isDemoMode: Boolean = false,
    onDemoTitleClick: (() -> Unit)? = null,
    externalShowShortcuts: Boolean = false,
    onExternalShowShortcutsHandled: () -> Unit = {},
    externalShowHelp: Boolean = false,
    onExternalShowHelpHandled: () -> Unit = {},
    externalShowFeedback: Boolean = false,
    onExternalShowFeedbackHandled: () -> Unit = {},
    externalShowSettings: Boolean = false,
    onExternalShowSettingsHandled: () -> Unit = {},
) {
    de.egril.defender.ui.a11y.FontSizeUnscaled {
        val headerTextSize = de.egril.defender.ui.settings.AppSettings.headerTextSize.value
        var showDebugMenu by remember { mutableStateOf(false) }
        val showDebugOptions = AppSettings.showDebugOptions.value
        var showShortcutsDialog by remember { mutableStateOf(false) }
        var showTutorialsHelpDialog by remember { mutableStateOf(false) }
        var showLevelCardDialog by remember { mutableStateOf(false) }

        // Handle externally triggered dialogs
        LaunchedEffect(externalShowShortcuts) {
            if (externalShowShortcuts) {
                showShortcutsDialog = true
                onExternalShowShortcutsHandled()
            }
        }
        LaunchedEffect(externalShowHelp) {
            if (externalShowHelp) {
                showTutorialsHelpDialog = true
                onExternalShowHelpHandled()
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
                    .zIndex(2f),
        ) {
            val locale = com.hyperether.resources.currentLanguage.value
            val titleFontSize =
                when (headerTextSize) {
                    de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.TextSizes.Body
                    de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.TextSizes.Medium
                    de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.TextSizes.Large
                }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (isDemoMode) {
                    Row(
                        modifier =
                            Modifier.then(if (onDemoTitleClick != null) Modifier.clickable { onDemoTitleClick() } else Modifier),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "*** DEMO MODE ***",
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "  ${gameState.level.getLocalizedTitle(locale)}  ",
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "*** DEMO MODE ***",
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    SelectableText(
                        text =
                            if (gameState.level.isSandbox) {
                                "${stringResource(Res.string.sandbox)} — ${gameState.level.getLocalizedTitle(locale)}"
                            } else {
                                gameState.level.getLocalizedTitle(locale)
                            },
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .wrapContentWidth()
                                .clickable { showLevelCardDialog = true },
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GamePlayConstants.Spacing.Items),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameStats(
                        gameState = gameState,
                        onCheatCode = onCheatCode,
                        headerTextSize = headerTextSize,
                        onEnemyCountClick = onEnemyCountClick,
                        onManaClick = onManaClick,
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                val buttonHeight =
                    when (headerTextSize) {
                        de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.ButtonSizes.CompactHeight
                        de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> 40.dp
                        de.egril.defender.ui.settings.HeaderTextSize.LARGE -> 48.dp
                    }
                val buttonIconSize =
                    when (headerTextSize) {
                        de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.IconSizes.Medium
                        de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.IconSizes.Large
                        de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.IconSizes.ExtraLarge
                    }
                val buttonTextSize =
                    when (headerTextSize) {
                        de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.TextSizes.Body
                        de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.TextSizes.Medium
                        de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.TextSizes.Large
                    }
                Row(
                    modifier = Modifier.wrapContentWidth(unbounded = true),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WaaghBar(
                        gameState = gameState,
                        slotHeight = buttonHeight,
                    )

                    LevelHeaderIcons(
                        gameState = gameState,
                        iconSize = buttonHeight,
                        onWinLevelInfoClick = onWinLevelInfoClick,
                    )

                    TooltipWrapper(text = stringResource(Res.string.difficulty)) {
                        DifficultyDisplay(
                            isClickable = false,
                        )
                    }

                    if (showDebugOptions) {
                        Box {
                            val debugOptionsLabel = stringResource(Res.string.debug_options)
                            IconButton(
                                onClick = { showDebugMenu = !showDebugMenu },
                                modifier = Modifier.size(buttonHeight).semantics { contentDescription = debugOptionsLabel },
                            ) {
                                ToolsIcon(size = buttonIconSize)
                            }

                            DropdownMenu(
                                expanded = showDebugMenu,
                                onDismissRequest = { showDebugMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(stringResource(Res.string.debug_display_tile_borders))
                                            Spacer(modifier = Modifier.weight(1f))
                                            Switch(
                                                checked = AppSettings.showTileBorders.value,
                                                onCheckedChange = { AppSettings.showTileBorders.value = it },
                                            )
                                        }
                                    },
                                    onClick = { AppSettings.showTileBorders.value = !AppSettings.showTileBorders.value },
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(stringResource(Res.string.debug_display_tile_positions))
                                            Spacer(modifier = Modifier.weight(1f))
                                            Switch(
                                                checked = AppSettings.showTilePositions.value,
                                                onCheckedChange = { AppSettings.showTilePositions.value = it },
                                            )
                                        }
                                    },
                                    onClick = { AppSettings.showTilePositions.value = !AppSettings.showTilePositions.value },
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(stringResource(Res.string.debug_display_map_size))
                                            Spacer(modifier = Modifier.weight(1f))
                                            Switch(
                                                checked = AppSettings.showMapSizeOverlay.value,
                                                onCheckedChange = { AppSettings.showMapSizeOverlay.value = it },
                                            )
                                        }
                                    },
                                    onClick = { AppSettings.showMapSizeOverlay.value = !AppSettings.showMapSizeOverlay.value },
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(stringResource(Res.string.debug_display_enemy_path))
                                            Spacer(modifier = Modifier.weight(1f))
                                            Switch(
                                                checked = AppSettings.showEnemyPathfinding.value,
                                                onCheckedChange = { AppSettings.showEnemyPathfinding.value = it },
                                            )
                                        }
                                    },
                                    onClick = { AppSettings.showEnemyPathfinding.value = !AppSettings.showEnemyPathfinding.value },
                                )
                            }
                        }
                    }

                    if (!isPlatformMobile && !isLimitedInputDevice && !isMobileWebBrowser()) {
                        val shortcutsLabel = stringResource(Res.string.tooltip_shortcuts)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TooltipWrapper(text = shortcutsLabel) {
                                IconButton(
                                    onClick = { showShortcutsDialog = true },
                                    modifier = Modifier.size(buttonHeight).semantics { contentDescription = shortcutsLabel },
                                ) {
                                    KeyboardKeyIcon(size = buttonIconSize)
                                }
                            }
                            ShortcutKeyChip(
                                text = "/",
                            )
                        }
                    }

                    val levelTitle = gameState.level.getLocalizedTitle(locale)
                    FeedbackButton(
                        modifier = Modifier.size(buttonHeight),
                        gameContext =
                            de.egril.defender.ui.feedback.GameFeedbackContext(
                                levelName = levelTitle,
                                turnNumber = gameState.turnNumber.value,
                                gameStateJson =
                                    buildString {
                                        val escapedTitle =
                                            de.egril.defender.ui.feedback
                                                .escapeForJson(levelTitle)
                                        append("{")
                                        append("\"levelId\":${gameState.level.id},")
                                        append("\"levelName\":\"$escapedTitle\",")
                                        append("\"turn\":${gameState.turnNumber.value},")
                                        append("\"hp\":${gameState.healthPoints.value},")
                                        append("\"coins\":${gameState.coins.value},")
                                        append("\"defenders\":${gameState.defenders.size},")
                                        append("\"attackers\":${gameState.attackers.size},")
                                        append("\"phase\":\"${gameState.phase.value.name}\"")
                                        append("}")
                                    },
                            ),
                        shortcutKey = ".",
                        triggerOpen = externalShowFeedback,
                        onTriggerHandled = onExternalShowFeedbackHandled,
                    )

                    SettingsButton(
                        modifier = Modifier.size(buttonHeight),
                        shortcutKey = ",",
                        triggerOpen = externalShowSettings,
                        onTriggerHandled = onExternalShowSettingsHandled,
                        pageBackgroundMusic =
                            if (gameState.healthPoints.value < 5) {
                                de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH
                            } else {
                                de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL
                            },
                    )

                    if (onSandboxTools != null && gameState.level.isSandbox) {
                        var sandboxHintDismissed by rememberSaveable { mutableStateOf(false) }
                        var sandboxButtonWidthPx by remember { mutableStateOf(0) }
                        Box {
                            TooltipWrapper(text = stringResource(Res.string.sandbox_tools)) {
                                Button(
                                    onClick = onSandboxTools,
                                    modifier =
                                        Modifier
                                            .height(buttonHeight)
                                            .semantics { contentDescription = "sandbox_tools" }
                                            .onGloballyPositioned { sandboxButtonWidthPx = it.size.width },
                                    contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp),
                                ) {
                                    SwordIcon(size = buttonIconSize)
                                }
                            }
                            if (!sandboxHintDismissed) {
                                HeaderButtonSpeechBubble(
                                    text = stringResource(Res.string.sandbox_tools_hint),
                                    anchorWidthPx = sandboxButtonWidthPx,
                                    anchorHeight = buttonHeight,
                                    onClose = { sandboxHintDismissed = true },
                                )
                            }
                        }
                    }

                    if (onSaveGame != null) {
                        TooltipWrapper(text = stringResource(Res.string.tooltip_save_the_game)) {
                            Button(
                                onClick = onSaveGame,
                                modifier = Modifier.height(buttonHeight),
                                contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    SaveIcon(
                                        size = buttonIconSize,
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                    )
                                    ShortcutKeyChip(
                                        text = formatShortcutBindingForDisplay(AppSettings.shortcutSaveGame.value),
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                                    )
                                }
                            }
                        }
                    }

                    TooltipWrapper(text = stringResource(Res.string.tooltip_return_to_worldmap)) {
                        Button(
                            onClick = onBackToMap,
                            modifier = Modifier.height(buttonHeight),
                            contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp),
                        ) {
                            Text(
                                stringResource(Res.string.map_label),
                                fontSize = buttonTextSize,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                            ShortcutKeyChip(
                                text = formatShortcutBindingForDisplay(AppSettings.shortcutBackToWorldMap.value),
                                color = LocalContentColor.current.copy(alpha = 0.75f),
                            )
                        }
                    }

                    TooltipWrapper(text = stringResource(Res.string.tooltip_enemy_list_and_legend)) {
                        val overlayButtonBackgroundColor = if (showOverlay) GamePlayColors.Success else GamePlayColors.Info
                        val overlayButtonContentColor = GamePlayColors.readableContentColor(overlayButtonBackgroundColor)
                        Button(
                            onClick = { onShowOverlayChange(!showOverlay) },
                            modifier = Modifier.height(buttonHeight),
                            contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = overlayButtonBackgroundColor,
                                    contentColor = overlayButtonContentColor,
                                ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val overlayIconSize = buttonIconSize * 1.35f
                                Box(
                                    modifier =
                                        Modifier
                                            .size(overlayIconSize),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (showOverlay) {
                                        TriangleRightIcon(size = overlayIconSize)
                                    } else {
                                        TriangleLeftIcon(size = overlayIconSize)
                                    }
                                }
                                ShortcutKeyChip(
                                    text = formatShortcutBindingForDisplay(AppSettings.shortcutToggleEnemyList.value),
                                    color = overlayButtonContentColor.copy(alpha = 0.75f),
                                )
                            }
                        }
                    }

                    val tutorialsAndHelpLabel = stringResource(Res.string.tutorials_and_help)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TooltipWrapper(text = tutorialsAndHelpLabel) {
                            IconButton(
                                onClick = { showTutorialsHelpDialog = true },
                                modifier =
                                    Modifier
                                        .size(buttonHeight)
                                        .semantics { contentDescription = tutorialsAndHelpLabel },
                            ) {
                                HelpIcon(
                                    size = 32.dp,
                                )
                            }
                        }
                        ShortcutKeyChip(
                            text = "H",
                        )
                    }
                }
            }
        }

        if (showShortcutsDialog) {
            Dialog(
                onDismissRequest = { showShortcutsDialog = false },
            ) {
                Card(
                    modifier =
                        Modifier
                            .width(600.dp)
                            .heightIn(max = 600.dp)
                            .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                    ) {
                        // Shortcut hint toggle
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(Res.string.shortcut_bindings_show_on_buttons),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Switch(
                                checked = AppSettings.showButtonShortcutHints.value,
                                onCheckedChange = { AppSettings.saveShowButtonShortcutHints(it) },
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            KeyboardShortcutsInfo(
                                enableBindingEdit = true,
                                showResetButton = true,
                            )
                        }
                        Button(
                            onClick = { showShortcutsDialog = false },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Text(stringResource(Res.string.got_it))
                        }
                    }
                }
            }
        }

        if (showTutorialsHelpDialog) {
            Dialog(
                onDismissRequest = { showTutorialsHelpDialog = false },
            ) {
                val helpFocusRequester = remember { FocusRequester() }
                val helpScrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(Unit) { helpFocusRequester.requestFocus() }
                Card(
                    modifier =
                        Modifier
                            .widthIn(max = 700.dp)
                            .fillMaxHeight(fraction = 0.85f)
                            .padding(8.dp)
                            .focusRequester(helpFocusRequester)
                            .focusTarget()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.Escape, Key.Back -> {
                                            showTutorialsHelpDialog = false
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            coroutineScope.launch { helpScrollState.animateScrollTo(helpScrollState.value + 150) }
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            coroutineScope.launch {
                                                helpScrollState.animateScrollTo(
                                                    (helpScrollState.value - 150).coerceAtLeast(0),
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
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                                .fillMaxHeight(),
                    ) {
                        Text(
                            text = stringResource(Res.string.tutorials_and_help),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            HowToPlayContent(scrollState = helpScrollState)
                        }
                        // Scroll hint
                        if (AppSettings.showButtonShortcutHints.value) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ShortcutKeyChip(text = "\u2191\u2193")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(Res.string.keyboard_nav_scroll),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showTutorialsHelpDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.got_it))
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "Esc", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                    }
                }
            }
        }

        if (showLevelCardDialog) {
            InGameLevelCardDialog(
                gameState = gameState,
                onDismiss = { showLevelCardDialog = false },
            )
        }
    } // FontSizeUnscaled
}
