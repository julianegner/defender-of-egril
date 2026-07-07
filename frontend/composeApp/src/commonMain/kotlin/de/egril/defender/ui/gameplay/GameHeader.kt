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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.hyperether.resources.stringResource
import de.egril.defender.editor.DEFAULT_MAP_TOOLING_INFO
import de.egril.defender.editor.EditorStorage
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.TooltipWrapper
import de.egril.defender.ui.common.LevelInfoEnemiesColumn
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.common.SpeechBubble
import de.egril.defender.ui.common.SpeechBubblePointer
import de.egril.defender.ui.feedback.FeedbackButton
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.icon.HelpIcon
import de.egril.defender.ui.icon.KeyboardKeyIcon
import de.egril.defender.ui.icon.SaveIcon
import de.egril.defender.ui.icon.ToolsIcon
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleLeftIcon
import de.egril.defender.ui.icon.TriangleRightIcon
import de.egril.defender.ui.icon.TrophyIcon
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Statistics at far left
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

                // Level name in center
                val locale = com.hyperether.resources.currentLanguage.value
                val titleFontSize =
                    when (headerTextSize) {
                        de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.TextSizes.Body
                        de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.TextSizes.Medium
                        de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.TextSizes.Large
                    }

                if (isDemoMode) {
                    // Demo title: "*** DEMO MODE ***  [original title]  *** DEMO MODE ***"
                    // The "*** DEMO MODE ***" parts are in red and clickable to stop the demo
                    Row(
                        modifier =
                            Modifier
                                .weight(1f)
                                .then(if (onDemoTitleClick != null) Modifier.clickable { onDemoTitleClick() } else Modifier),
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
                        text = gameState.level.getLocalizedTitle(locale),
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .weight(1f)
                                .clickable { showLevelCardDialog = true },
                        textAlign = TextAlign.Center,
                    )
                }

                // Buttons and difficulty at far right
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Level header icons (water and/or tower) before difficulty
                    LevelHeaderIcons(
                        gameState = gameState,
                        iconSize = buttonHeight, // Use button height for larger icons (double size)
                        onWinLevelInfoClick = onWinLevelInfoClick,
                    )

                    // Difficulty display (non-clickable on gameplay screen)
                    TooltipWrapper(text = stringResource(Res.string.difficulty)) {
                        DifficultyDisplay(
                            isClickable = false,
                        )
                    }

                    // Debug options button (only visible when debug options enabled)
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
                            }
                        }
                    }

                    // Shortcuts button (not shown on mobile platforms or mobile web browsers)
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

                    // Feedback button (icon only to save space)
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

                    // Settings button (icon only to save space)
                    SettingsButton(
                        modifier = Modifier.size(buttonHeight),
                        shortcutKey = ",",
                        triggerOpen = externalShowSettings,
                        onTriggerHandled = onExternalShowSettingsHandled,
                    )

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

@Composable
private fun InGameLevelCardDialog(
    gameState: GameState,
    onDismiss: () -> Unit,
) {
    val currentDifficulty = AppSettings.difficulty.value
    val level = gameState.level
    val locale = com.hyperether.resources.currentLanguage.value
    val mapToolingInfo =
        remember(level.mapId) {
            val mapId = level.mapId
            if (mapId == null) {
                DEFAULT_MAP_TOOLING_INFO
            } else {
                EditorStorage.getMap(mapId)?.mapToolingInfo
                    ?: EditorStorage.getCommunityMap(mapId)?.mapToolingInfo
                    ?: DEFAULT_MAP_TOOLING_INFO
            }
        }
    val localizedMapToolingInfo = localizeMapToolingInfo(mapToolingInfo, locale)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = level.getLocalizedTitle(locale),
                    style = MaterialTheme.typography.headlineSmall,
                )
                val localizedSubtitle = level.getLocalizedSubtitle(locale)
                if (localizedSubtitle.isNotBlank()) {
                    Text(
                        text = localizedSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LevelInfoEnemiesColumn(
                        level = level.toLevelInfoEnemiesLevelData(currentDifficulty),
                        textColor = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier =
                            Modifier
                                .weight(2f),
                    ) {
                        HexagonMinimap(
                            level = level,
                            config =
                                MinimapConfig(
                                    showSpawnPoints = true,
                                    showTarget = true,
                                    showTowers = false,
                                    showEnemies = false,
                                    showViewport = false,
                                ),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Text(
                    text = stringResource(Res.string.map_tooling_info_label, localizedMapToolingInfo),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.close))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameStats(
    gameState: GameState,
    onCheatCode: (() -> Unit)?,
    headerTextSize: de.egril.defender.ui.settings.HeaderTextSize,
    onEnemyCountClick: (() -> Unit)? = null,
    onManaClick: (() -> Unit)? = null,
) {
    val iconSize =
        when (headerTextSize) {
            de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.IconSizes.Large
            de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.IconSizes.ExtraLarge
            de.egril.defender.ui.settings.HeaderTextSize.LARGE -> 32.dp
        }
    val textStyle =
        when (headerTextSize) {
            de.egril.defender.ui.settings.HeaderTextSize.SMALL -> MaterialTheme.typography.bodyLarge
            de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> MaterialTheme.typography.titleMedium
            de.egril.defender.ui.settings.HeaderTextSize.LARGE -> MaterialTheme.typography.titleLarge
        }

    GameStatsDisplay(
        coins = gameState.coins.value,
        health = gameState.healthPoints.value,
        turn = gameState.turnNumber.value,
        activeEnemyCount = gameState.getActiveEnemyCount(),
        remainingEnemyCount = gameState.getRemainingEnemyCount(),
        currentMana = if (gameState.maxMana.value > 0) gameState.currentMana.value else null,
        maxMana = if (gameState.maxMana.value > 0) gameState.maxMana.value else null,
        iconSize = iconSize,
        textStyle = textStyle,
        onCoinsClick = onCheatCode,
        onEnemyCountClick = onEnemyCountClick,
        onManaClick = onManaClick,
    )
}

/**
 * Level header icons showing water and/or tower info
 */
@Composable
private fun LevelHeaderIcons(
    gameState: GameState,
    iconSize: Dp,
    onWinLevelInfoClick: (() -> Unit)? = null,
) {
    val hasRiver = gameState.level.riverTiles.isNotEmpty()
    val specialTowers =
        gameState.level.availableTowers.filter {
            it in listOf(DefenderType.WIZARD_TOWER, DefenderType.ALCHEMY_TOWER, DefenderType.BALLISTA_TOWER, DefenderType.DWARVEN_MINE)
        }
    val hasSpecialTowers = specialTowers.isNotEmpty()

    // Win-level info icon with a speech bubble pointing at it (only shown when the level is
    // guaranteed to be won). Clicking the badge opens the full win-level popup.
    if (onWinLevelInfoClick != null && gameState.canWinLevelNow()) {
        var bubbleDismissed by rememberSaveable { mutableStateOf(false) }
        Box {
            TrophyIcon(
                size = iconSize,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onWinLevelInfoClick() },
            )
            if (!bubbleDismissed) {
                WinLevelSpeechBubble(
                    text = stringResource(Res.string.win_level_now_description),
                    badgeSize = iconSize,
                    onClose = { bubbleDismissed = true },
                )
            }
        }
    }

    // Water icon (if level has river) - blue color
    if (hasRiver) {
        de.egril.defender.ui.icon.WaterIcon(
            size = iconSize,
            tint = Color(0xFF2196F3), // Blue color
            modifier =
                Modifier.clickable {
                    val infoState = gameState.infoState.value
                    // Toggle behavior: if already showing, close it; otherwise show it
                    if (infoState.currentInfo == InfoType.RIVER_INFO) {
                        // Close the dialog
                        gameState.infoState.value = infoState.dismissInfo()
                    } else {
                        // Show the info dialog
                        gameState.infoState.value = infoState.showInfo(InfoType.RIVER_INFO)
                    }
                },
        )
    }

    // Tower icon (if level has special towers)
    if (hasSpecialTowers) {
        de.egril.defender.ui.icon.TowerIcon(
            size = iconSize,
            lineColor = MaterialTheme.colorScheme.onSurface, // Use header text color
            modifier =
                Modifier.clickable {
                    val infoState = gameState.infoState.value
                    // Toggle behavior: if already showing, close it; otherwise show it
                    if (infoState.currentInfo == InfoType.SPECIAL_TOWERS_INFO) {
                        // Close the dialog
                        gameState.infoState.value = infoState.dismissInfo()
                    } else {
                        // Show the info dialog
                        gameState.infoState.value = infoState.showInfo(InfoType.SPECIAL_TOWERS_INFO)
                    }
                },
        )
    }
}

/**
 * A speech bubble rendered just below the win-level badge, its pointer aimed at the badge centre.
 * Drawn as a zero-size overlay so it floats over the map without affecting the header row layout.
 */
@Composable
private fun WinLevelSpeechBubble(
    text: String,
    badgeSize: Dp,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val badgeSizePx = with(density) { badgeSize.roundToPx() }
    val gapPx = with(density) { 2.dp.roundToPx() }
    // Distance from the bubble's left edge to the pointer tip. Matches SpeechBubble's minimum
    // clamp for an UP pointer (cornerRadius + pointerWidth / 2 = 12dp + 8dp). The bubble is shifted
    // left by this amount (minus half the badge) so the pointer lands on the badge centre.
    val pointerInset = 20.dp
    val pointerInsetPx = with(density) { pointerInset.roundToPx() }

    Box(
        modifier =
            Modifier
                .zIndex(50f)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    // Report zero size so the bubble does not affect the header row layout.
                    layout(0, 0) {
                        placeable.place(badgeSizePx / 2 - pointerInsetPx, badgeSizePx + gapPx)
                    }
                },
    ) {
        SpeechBubble(
            pointer = SpeechBubblePointer.UP,
            // Aim the pointer tip at the badge centre (see pointerInset above).
            pointerOffset = pointerInset,
            onClose = onClose,
            closeContentDescription = stringResource(Res.string.close),
            modifier = Modifier.widthIn(max = 260.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Dialog showing info for all special towers in the level with collapsible sections
 */
@Composable
internal fun LevelSpecialTowersInfoDialog(
    specialTowers: List<DefenderType>,
    onDismiss: () -> Unit,
) {
    var expandedTower by remember { mutableStateOf<DefenderType?>(specialTowers.firstOrNull()) }

    ScrollableInfoCard(
        width = 600.dp,
        maxHeight = 600.dp,
        onDismiss = onDismiss,
    ) {
        // Title on the right side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = stringResource(Res.string.special_towers_info_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Info section at top showing how to reopen
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(8.dp),
                    ).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tower icon (same as in header)
            de.egril.defender.ui.icon.TowerIcon(
                size = 32.dp,
                lineColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(Res.string.special_towers_info_reopen),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        specialTowers.forEach { towerType ->
            val isExpanded = expandedTower == towerType

            // Collapsible header with tower icon and name
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expandedTower = if (isExpanded) null else towerType }
                        .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Tower-specific icon using in-game tower representation with gray background
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .background(Color.Gray, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    de.egril.defender.ui.TowerTypeIcon(
                        defenderType = towerType,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Text(
                    text = towerType.getLocalizedName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )

                // Expand/collapse indicator
                if (isExpanded) {
                    TriangleDownIcon(size = 14.dp)
                } else {
                    TriangleRightIcon(size = 14.dp)
                }
            }

            // Expanded content
            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 32.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val infoMessage =
                        when (towerType) {
                            DefenderType.WIZARD_TOWER -> stringResource(Res.string.wizard_first_use_message)
                            DefenderType.ALCHEMY_TOWER -> stringResource(Res.string.alchemy_first_use_message)
                            DefenderType.BALLISTA_TOWER -> stringResource(Res.string.ballista_first_use_message)
                            DefenderType.DWARVEN_MINE -> stringResource(Res.string.mine_first_use_message)
                            else -> ""
                        }

                    SelectableText(
                        text = infoMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Divider between towers (except last)
            if (towerType != specialTowers.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
