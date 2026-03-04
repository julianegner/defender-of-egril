package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.SaveIcon
import de.egril.defender.ui.icon.ToolsIcon
import de.egril.defender.ui.icon.TriangleLeftIcon
import de.egril.defender.ui.icon.TriangleRightIcon
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.ui.settings.DifficultyDisplay
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun GameHeader(
    gameState: GameState,
    showOverlay: Boolean,
    onShowOverlayChange: (Boolean) -> Unit,
    onBackToMap: () -> Unit,
    onSaveGame: (() -> Unit)?,
    onCheatCode: (() -> Unit)?,
    onEnemyCountClick: (() -> Unit)? = null,
    onManaClick: (() -> Unit)? = null
) {
    val headerTextSize = de.egril.defender.ui.settings.AppSettings.headerTextSize.value
    var showDebugMenu by remember { mutableStateOf(false) }
    val showDebugOptions = AppSettings.showDebugOptions.value
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .zIndex(2f)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Statistics at far left
            Row(
                horizontalArrangement = Arrangement.spacedBy(GamePlayConstants.Spacing.Items),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameStats(
                    gameState = gameState,
                    onCheatCode = onCheatCode,
                    headerTextSize = headerTextSize,
                    onEnemyCountClick = onEnemyCountClick,
                    onManaClick = onManaClick
                )
            }

            // Level name in center (without prefix, bold when collapsed)
            val locale = com.hyperether.resources.currentLanguage.value
            val titleFontSize = when (headerTextSize) {
                de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.TextSizes.Body
                de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.TextSizes.Medium
                de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.TextSizes.Large
            }
            
            Text(
                text = gameState.level.getLocalizedTitle(locale),
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Buttons and difficulty at far right
            val buttonHeight = when (headerTextSize) {
                de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.ButtonSizes.CompactHeight
                de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> 40.dp
                de.egril.defender.ui.settings.HeaderTextSize.LARGE -> 48.dp
            }
            val buttonIconSize = when (headerTextSize) {
                de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.IconSizes.Medium
                de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.IconSizes.Large
                de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.IconSizes.ExtraLarge
            }
            val buttonTextSize = when (headerTextSize) {
                de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.TextSizes.Body
                de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.TextSizes.Medium
                de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.TextSizes.Large
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level header icons (water and/or tower) before difficulty
                LevelHeaderIcons(
                    gameState = gameState,
                    iconSize = buttonHeight  // Use button height for larger icons (double size)
                )
                
                // Difficulty display (non-clickable on gameplay screen)
                DifficultyDisplay(
                    isClickable = false
                )

                // Debug options button (only visible when debug options enabled)
                if (showDebugOptions) {
                    Box {
                        IconButton(
                            onClick = { showDebugMenu = !showDebugMenu },
                            modifier = Modifier.size(buttonHeight)
                        ) {
                            ToolsIcon(size = buttonIconSize)
                        }

                        DropdownMenu(
                            expanded = showDebugMenu,
                            onDismissRequest = { showDebugMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stringResource(Res.string.debug_display_tile_borders))
                                        Spacer(modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = AppSettings.showTileBorders.value,
                                            onCheckedChange = { AppSettings.showTileBorders.value = it }
                                        )
                                    }
                                },
                                onClick = { AppSettings.showTileBorders.value = !AppSettings.showTileBorders.value }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stringResource(Res.string.debug_display_tile_positions))
                                        Spacer(modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = AppSettings.showTilePositions.value,
                                            onCheckedChange = { AppSettings.showTilePositions.value = it }
                                        )
                                    }
                                },
                                onClick = { AppSettings.showTilePositions.value = !AppSettings.showTilePositions.value }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stringResource(Res.string.debug_display_map_size))
                                        Spacer(modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = AppSettings.showMapSizeOverlay.value,
                                            onCheckedChange = { AppSettings.showMapSizeOverlay.value = it }
                                        )
                                    }
                                },
                                onClick = { AppSettings.showMapSizeOverlay.value = !AppSettings.showMapSizeOverlay.value }
                            )
                        }
                    }
                }

                // Settings button (icon only to save space)
                SettingsButton(
                    modifier = Modifier.size(buttonHeight)
                )
                
                if (onSaveGame != null) {
                    Button(
                        onClick = onSaveGame,
                        modifier = Modifier.height(buttonHeight),
                        contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp)
                    ) {
                        SaveIcon(
                            size = buttonIconSize,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

                Button(
                    onClick = onBackToMap,
                    modifier = Modifier.height(buttonHeight),
                    contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(Res.string.map_label),
                        fontSize = buttonTextSize,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Button(
                    onClick = { onShowOverlayChange(!showOverlay) },
                    modifier = Modifier.height(buttonHeight),
                    contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showOverlay) GamePlayColors.Success else GamePlayColors.Info
                    )
                ) {
                    if (showOverlay) {
                        TriangleRightIcon(size = buttonIconSize)
                    } else {
                        TriangleLeftIcon(size = buttonIconSize)
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
    onManaClick: (() -> Unit)? = null
) {
    val iconSize = when (headerTextSize) {
        de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.IconSizes.Large
        de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.IconSizes.ExtraLarge
        de.egril.defender.ui.settings.HeaderTextSize.LARGE -> 32.dp
    }
    val textStyle = when (headerTextSize) {
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
        onManaClick = onManaClick
    )
}

/**
 * Level header icons showing water and/or tower info
 */
@Composable
private fun LevelHeaderIcons(
    gameState: GameState,
    iconSize: Dp
) {
    val hasRiver = gameState.level.riverTiles.isNotEmpty()
    val specialTowers = gameState.level.availableTowers.filter {
        it in listOf(DefenderType.WIZARD_TOWER, DefenderType.ALCHEMY_TOWER, DefenderType.BALLISTA_TOWER, DefenderType.DWARVEN_MINE)
    }
    val hasSpecialTowers = specialTowers.isNotEmpty()
    
    // Water icon (if level has river) - blue color
    if (hasRiver) {
        de.egril.defender.ui.icon.WaterIcon(
            size = iconSize,
            tint = Color(0xFF2196F3),  // Blue color
            modifier = Modifier.clickable {
                val infoState = gameState.infoState.value
                // Toggle behavior: if already showing, close it; otherwise show it
                if (infoState.currentInfo == InfoType.RIVER_INFO) {
                    // Close the dialog
                    gameState.infoState.value = infoState.dismissInfo()
                } else {
                    // Show the info dialog
                    gameState.infoState.value = infoState.showInfo(InfoType.RIVER_INFO)
                }
            }
        )
    }
    
    // Tower icon (if level has special towers)
    if (hasSpecialTowers) {
        de.egril.defender.ui.icon.TowerIcon(
            size = iconSize,
            lineColor = MaterialTheme.colorScheme.onSurface,  // Use header text color
            modifier = Modifier.clickable { 
                val infoState = gameState.infoState.value
                // Toggle behavior: if already showing, close it; otherwise show it
                if (infoState.currentInfo == InfoType.SPECIAL_TOWERS_INFO) {
                    // Close the dialog
                    gameState.infoState.value = infoState.dismissInfo()
                } else {
                    // Show the info dialog
                    gameState.infoState.value = infoState.showInfo(InfoType.SPECIAL_TOWERS_INFO)
                }
            }
        )
    }
}

/**
 * Dialog showing info for all special towers in the level with collapsible sections
 */
@Composable
internal fun LevelSpecialTowersInfoDialog(
    specialTowers: List<DefenderType>,
    onDismiss: () -> Unit
) {
    var expandedTower by remember { mutableStateOf<DefenderType?>(specialTowers.firstOrNull()) }
    
    ScrollableInfoCard(
        width = 600.dp,
        maxHeight = 600.dp,
        onDismiss = onDismiss
    ) {
        // Title on the right side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = stringResource(Res.string.special_towers_info_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Info section at top showing how to reopen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tower icon (same as in header)
            de.egril.defender.ui.icon.TowerIcon(
                size = 32.dp,
                lineColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(Res.string.special_towers_info_reopen),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        specialTowers.forEach { towerType ->
            val isExpanded = expandedTower == towerType
            
            // Collapsible header with tower icon and name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedTower = if (isExpanded) null else towerType }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tower-specific icon using in-game tower representation with gray background
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Gray, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    de.egril.defender.ui.TowerTypeIcon(
                        defenderType = towerType,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Text(
                    text = towerType.getLocalizedName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // Expand/collapse indicator
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Expanded content
            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 32.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val infoMessage = when (towerType) {
                        DefenderType.WIZARD_TOWER -> stringResource(Res.string.wizard_first_use_message)
                        DefenderType.ALCHEMY_TOWER -> stringResource(Res.string.alchemy_first_use_message)
                        DefenderType.BALLISTA_TOWER -> stringResource(Res.string.ballista_first_use_message)
                        DefenderType.DWARVEN_MINE -> stringResource(Res.string.mine_first_use_message)
                        else -> ""
                    }
                    
                    Text(
                        text = infoMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Divider between towers (except last)
            if (towerType != specialTowers.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
