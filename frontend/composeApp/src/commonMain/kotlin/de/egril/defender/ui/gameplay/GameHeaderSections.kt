package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleRightIcon
import de.egril.defender.ui.icon.TrophyIcon
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun InGameLevelCardDialog(
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
internal fun GameStats(
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
        runes = gameState.runes.value,
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
internal fun LevelHeaderIcons(
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
    var showStoryLogbook by rememberSaveable { mutableStateOf(false) }

    // Win-level info icon with a speech bubble pointing at it (only shown when the level is
    // guaranteed to be won). Clicking the badge opens the full win-level popup.
    if (onWinLevelInfoClick != null && gameState.canWinLevelNow()) {
        var bubbleDismissed by rememberSaveable { mutableStateOf(false) }
        Box {
            TooltipWrapper(text = stringResource(Res.string.tooltip_win_level_now)) {
                TrophyIcon(
                    size = iconSize,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onWinLevelInfoClick() },
                )
            }
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
        TooltipWrapper(text = stringResource(Res.string.river_info_title)) {
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
    }

    // Story logbook: lists the story messages of this level, newest first. Only offered once a displayable story message has been shown.
    if (storyLogHasDisplayableEntries(gameState.storyLog)) {
        TooltipWrapper(text = stringResource(Res.string.story_logbook_title)) {
            de.egril.defender.ui.icon.BookIcon(
                size = iconSize,
                tint = Color(0xFF616161), // Dark grey, softer than the default black header icons
                modifier = Modifier.clickable { showStoryLogbook = true },
            )
        }
    }
    if (showStoryLogbook) {
        StoryLogbookDialog(
            gameState = gameState,
            onDismiss = { showStoryLogbook = false },
        )
    }

    // Tower icon (if level has special towers)
    if (hasSpecialTowers) {
        TooltipWrapper(text = stringResource(Res.string.special_towers_info_title)) {
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
 * A speech bubble rendered just below a header button, its pointer aimed at the button's horizontal
 * centre. Drawn as a zero-size overlay so it floats over the map without affecting the header row
 * layout. [anchorWidthPx] is the measured width of the anchor button (0 until first measured).
 */
@Composable
internal fun HeaderButtonSpeechBubble(
    text: String,
    anchorWidthPx: Int,
    anchorHeight: Dp,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val anchorHeightPx = with(density) { anchorHeight.roundToPx() }
    val gapPx = with(density) { 2.dp.roundToPx() }
    // Distance from the bubble's left edge to the pointer tip. Matches SpeechBubble's minimum clamp
    // for an UP pointer (cornerRadius + pointerWidth / 2 = 12dp + 8dp). The bubble is shifted left by
    // this amount (minus half the button width) so the pointer lands on the button centre.
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
                        placeable.place(anchorWidthPx / 2 - pointerInsetPx, anchorHeightPx + gapPx)
                    }
                },
    ) {
        SpeechBubble(
            pointer = SpeechBubblePointer.UP,
            // Aim the pointer tip at the button centre (see pointerInset above).
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

@Composable
internal fun WaaghBar(
    gameState: GameState,
    slotHeight: Dp,
) {
    if (!gameState.level.waaghEnabled || !gameState.hasHordeUnitsInLevel) return

    val slotHeightPx = with(LocalDensity.current) { slotHeight.roundToPx() }
    val waaghMeterHeight = slotHeight * 2f
    val waaghMeterWidth = waaghMeterHeight * 2f
    val waaghMeterVerticalOffset = -(waaghMeterHeight / 4f)
    val waaghPoints = gameState.waaghPoints.value.coerceIn(0, 100)
    val isFrenzyActive = gameState.waaghFrenzyActive.value
    val resource =
        if (isFrenzyActive) {
            Res.drawable.waaagh_title
        } else {
            val roundedDown = (waaghPoints / 5) * 5
            Res.allDrawableResources["waaagh${roundedDown.toString().padStart(2, '0')}"] ?: Res.drawable.waaagh00
        }
    val waaghProgress = waaghPoints / 100f
    val waaghStatusText =
        when {
            isFrenzyActive -> stringResource(Res.string.waagh_meter_frenzy_storming)
            waaghPoints >= 90 -> stringResource(Res.string.waagh_meter_urgency_critical)
            waaghPoints >= 70 -> stringResource(Res.string.waagh_meter_urgency_high)
            waaghPoints >= 45 -> stringResource(Res.string.waagh_meter_urgency_rising)
            waaghPoints >= 20 -> stringResource(Res.string.waagh_meter_urgency_stirring)
            else -> stringResource(Res.string.waagh_meter_urgency_gathering)
        }
    val waaghTooltipText =
        stringResource(Res.string.waagh_meter_tooltip, formatWaaghMeterPercent(waaghPoints), waaghStatusText)

    TooltipWrapper(text = waaghTooltipText) {
        Row(
            modifier =
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minHeight = 0))
                    layout(placeable.width, slotHeightPx) {
                        placeable.placeRelative(0, 0)
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(resource),
                contentDescription = null,
                modifier =
                    Modifier
                        .height(waaghMeterHeight)
                        .width(waaghMeterWidth)
                        .offset(y = waaghMeterVerticalOffset),
            )
            if (!isFrenzyActive) {
                Text(
                    text = formatWaaghMeterPercent(waaghPoints),
                    color = getWaaghMeterColor(waaghProgress),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = (-8).dp, y = waaghMeterVerticalOffset).padding(end = 8.dp),
                )
            }
        }
    }
}
