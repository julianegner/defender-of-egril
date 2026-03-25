package de.egril.defender.ui.loadgame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.*
import de.egril.defender.save.SaveGameMetadata
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.HexagonShape
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.TowerTypeIcon
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.TimerIcon
import de.egril.defender.ui.icon.TrashIcon
import de.egril.defender.ui.icon.SpeechBubbleIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

// Constants for mobile display limits
private const val MOBILE_MAX_ENEMIES_DISPLAYED = 2

@Composable
fun SavedGameCardHeader(
    levelName: String,
    dateStr: String,
    isLocal: Boolean = true,
    isRemote: Boolean = false,
    isMobile: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = levelName,
            style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f, fill = false)
        )
        // Local / Remote presence chips
        if (isLocal) {
            SavefileLocationChip(
                label = stringResource(Res.string.savefile_chip_local),
                color = MaterialTheme.colorScheme.tertiary,
                onColor = MaterialTheme.colorScheme.onTertiary,
                isMobile = isMobile
            )
        }
        if (isRemote) {
            SavefileLocationChip(
                label = stringResource(Res.string.savefile_chip_remote),
                color = MaterialTheme.colorScheme.primary,
                onColor = MaterialTheme.colorScheme.onPrimary,
                isMobile = isMobile
            )
        }
    }

    Spacer(modifier = Modifier.height(if (isMobile) 2.dp else 4.dp))
    
    Text(
        text = dateStr,
        style = MaterialTheme.typography.bodySmall,
        fontSize = if (isMobile) 10.sp else 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SavefileLocationChip(
    label: String,
    color: Color,
    onColor: Color,
    isMobile: Boolean
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .padding(horizontal = if (isMobile) 4.dp else 6.dp, vertical = if (isMobile) 1.dp else 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = if (isMobile) 9.sp else 10.sp,
            color = onColor
        )
    }
}

@Composable
fun SavedGameCardStats(
    turnNumber: Int,
    coins: Int,
    healthPoints: Int,
    isMobile: Boolean = false
) {
    val iconSize = if (isMobile) 12.dp else 16.dp
    val fontSize = if (isMobile) 11.sp else 14.sp
    val spacing = if (isMobile) 8.dp else 16.dp
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Turn number
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerIcon(size = iconSize)
            Spacer(modifier = Modifier.width(if (isMobile) 2.dp else 4.dp))
            Text(
                text = "${stringResource(Res.string.turn)} $turnNumber",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = fontSize
            )
        }
        
        Spacer(modifier = Modifier.width(spacing))
        
        // Coins
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoneyIcon(size = iconSize)
            Spacer(modifier = Modifier.width(if (isMobile) 2.dp else 4.dp))
            Text(
                text = "$coins",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = fontSize
            )
        }
        
        Spacer(modifier = Modifier.width(spacing))
        
        // Health Points
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            de.egril.defender.ui.icon.HeartIcon(size = iconSize)
            Spacer(modifier = Modifier.width(if (isMobile) 2.dp else 4.dp))
            Text(
                text = "$healthPoints",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = fontSize
            )
        }
    }
}

@Composable
fun SavedGameCardComment(
    comment: String,
    isMobile: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        SpeechBubbleIcon(
            size = if (isMobile) 12.dp else 16.dp
        )
        Spacer(modifier = Modifier.width(if (isMobile) 2.dp else 4.dp))
        Text(
            text = comment,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = if (isMobile) 10.sp else 14.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SavedGameCardUnitsAndMinimap(
    saveGame: SaveGameMetadata,
    level: Level?,
    minimapGameState: GameState?,
    onDelete: () -> Unit,
    onDownload: () -> Unit = {},
    isMobile: Boolean = false
) {
    if (isMobile) {
        // Mobile layout: stack vertically
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Towers and enemies in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Top
                ) {
                    TowersList(defenderCounts = saveGame.defenderCounts, isMobile = true)
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Top
                ) {
                    EnemiesList(
                        attackerCounts = saveGame.attackerCounts,
                        remainingSpawnCounts = saveGame.remainingSpawnCounts,
                        dwarvenTrapCount = saveGame.dwarvenTrapCount,
                        magicalTrapCount = saveGame.magicalTrapCount,
                        barricadeCount = saveGame.barricadeCount,
                        isMobile = true
                    )
                }
            }
            
            // Minimap and buttons below
            MinimapAndDeleteButton(
                level = level,
                minimapGameState = minimapGameState,
                onDelete = onDelete,
                onDownload = onDownload,
                isMobile = true
            )
        }
    } else {
        // Desktop layout: row with 3 columns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Column 1: Built towers
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                TowersList(defenderCounts = saveGame.defenderCounts, isMobile = false)
            }
            
            // Column 2: Enemies (current and to come) with defensive items to the right
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                EnemiesList(
                    attackerCounts = saveGame.attackerCounts,
                    remainingSpawnCounts = saveGame.remainingSpawnCounts,
                    dwarvenTrapCount = saveGame.dwarvenTrapCount,
                    magicalTrapCount = saveGame.magicalTrapCount,
                    barricadeCount = saveGame.barricadeCount,
                    isMobile = false
                )
            }
            
            // Column 3: Minimap and delete button
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                MinimapAndDeleteButton(
                    level = level,
                    minimapGameState = minimapGameState,
                    onDelete = onDelete,
                    onDownload = onDownload,
                    isMobile = false
                )
            }
        }
    }
}

@Composable
fun TowersList(
    defenderCounts: Map<DefenderType, Int>,
    isMobile: Boolean = false
) {
    if (defenderCounts.isNotEmpty()) {
        Text(
            text = stringResource(Res.string.built_towers),
            style = MaterialTheme.typography.bodySmall,
            fontSize = if (isMobile) 9.sp else 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(if (isMobile) 2.dp else 4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(if (isMobile) 2.dp else 4.dp)) {
            defenderCounts.entries.forEach { (type, count) ->
                UnitEntry(
                    icon = { DefenderTypeIconSimple(type, isMobile) },
                    name = type.getLocalizedName(locale = com.hyperether.resources.currentLanguage.value),
                    count = count,
                    isMobile = isMobile
                )
            }
        }
    }
}

@Composable
fun EnemiesList(
    attackerCounts: Map<AttackerType, Int>,
    remainingSpawnCounts: Map<AttackerType, Int>,
    dwarvenTrapCount: Int = 0,
    magicalTrapCount: Int = 0,
    barricadeCount: Int = 0,
    isMobile: Boolean = false
) {
    val locale = com.hyperether.resources.currentLanguage.value
    
    if (isMobile) {
        // For mobile, simplify to a single column
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Current enemies
            if (attackerCounts.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.enemies_on_map),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Display first MOBILE_MAX_ENEMIES_DISPLAYED enemies to save space on mobile
                    attackerCounts.entries.take(MOBILE_MAX_ENEMIES_DISPLAYED).forEach { (type, count) ->
                        UnitEntry(
                            icon = { EnemyTypeIcon(attackerType = type) },
                            name = type.getLocalizedName(locale),
                            count = count,
                            isMobile = true
                        )
                    }
                    if (attackerCounts.size > MOBILE_MAX_ENEMIES_DISPLAYED) {
                        Text(
                            text = "...",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Current enemies on map
            if (attackerCounts.isNotEmpty()) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = stringResource(Res.string.enemies_on_map),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        attackerCounts.entries.forEach { (type, count) ->
                            UnitEntry(
                                icon = { EnemyTypeIcon(attackerType = type) },
                                name = type.getLocalizedName(locale),
                                count = count,
                                isMobile = false
                            )
                        }
                    }
                }
            }
            
            // Remaining spawns
            if (remainingSpawnCounts.isNotEmpty()) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = stringResource(Res.string.enemies_to_come),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        remainingSpawnCounts.entries.forEach { (type, count) ->
                            UnitEntry(
                                icon = { EnemyTypeIcon(attackerType = type) },
                                name = type.getLocalizedName(locale),
                                count = count,
                                isMobile = false
                            )
                        }
                    }
                }
            }
            
            // Defensive items (to the right)
            val hasTrapsOrBarricades = dwarvenTrapCount > 0 || magicalTrapCount > 0 || barricadeCount > 0
            if (hasTrapsOrBarricades) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = stringResource(Res.string.defensive_items),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (dwarvenTrapCount > 0) {
                            UnitEntry(
                                icon = { 
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        de.egril.defender.ui.icon.TrapIcon(size = 20.dp)
                                    }
                                },
                                name = stringResource(Res.string.trap),
                                count = dwarvenTrapCount,
                                isMobile = false
                            )
                        }
                        
                        if (magicalTrapCount > 0) {
                            UnitEntry(
                                icon = { 
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        de.egril.defender.ui.icon.PentagramIcon(size = 20.dp)
                                    }
                                },
                                name = stringResource(Res.string.magical_trap),
                                count = magicalTrapCount,
                                isMobile = false
                            )
                        }
                        
                        if (barricadeCount > 0) {
                            UnitEntry(
                                icon = { 
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        de.egril.defender.ui.icon.WoodIcon(size = 24.dp)
                                    }
                                },
                                name = stringResource(Res.string.barricade),
                                count = barricadeCount,
                                isMobile = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinimapAndDeleteButton(
    level: Level?,
    minimapGameState: GameState?,
    onDelete: () -> Unit,
    onDownload: () -> Unit = {},
    isMobile: Boolean = false
) {
    if (isMobile) {
        // Mobile layout: smaller minimap and horizontal button layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minimap on the left (smaller)
            if (level != null) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(60.dp)
                ) {
                    val mapName = HexagonMinimap(
                        level = level,
                        config = MinimapConfig(
                            showSpawnPoints = true,
                            showTarget = true,
                            showTowers = true,
                            showEnemies = true,
                            showViewport = false,
                            backgroundColor = Color.Transparent,
                            borderColor = Color.Transparent
                        ),
                        gameState = minimapGameState,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = mapName,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 8.sp,
                        modifier = Modifier.absoluteOffset(x = 0.dp, y = (-10).dp)
                    )
                }
            }
            
            // Buttons on the right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Download button
                de.egril.defender.ui.icon.DownloadIcon(
                    size = 16.dp,
                    modifier = Modifier.clickable { onDownload() }
                )
                
                // Delete button
                TrashIcon(
                    size = 16.dp,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    } else {
        // Desktop layout: original vertical layout
        Column {
            // Minimap
            if (level != null) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(120.dp)
                        .padding(top = 20.dp)
                ) {
                    val mapName = HexagonMinimap(
                        level = level,
                        config = MinimapConfig(
                            showSpawnPoints = true,
                            showTarget = true,
                            showTowers = true,
                            showEnemies = true,
                            showViewport = false,
                            backgroundColor = Color.Transparent,
                            borderColor = Color.Transparent
                        ),
                        gameState = minimapGameState,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = mapName,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        modifier = Modifier.absoluteOffset(x = 0.dp, y = (-20).dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons row (Download and Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Download button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.clickable { onDownload() }
                ) {
                    Text(
                        text = stringResource(Res.string.download_savefile),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    de.egril.defender.ui.icon.DownloadIcon(size = 20.dp)
                }
                
                // Delete button with text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.clickable { onDelete() }
                ) {
                    Text(
                        text = stringResource(Res.string.delete_savegame),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TrashIcon(size = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun UnitEntry(
    icon: @Composable () -> Unit,
    name: String,
    count: Int,
    isMobile: Boolean = false
) {
    val iconSize = if (isMobile) 20.dp else 32.dp
    val fontSize = if (isMobile) 9.sp else 11.sp
    val spacing = if (isMobile) 2.dp else 4.dp
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = Modifier.size(iconSize)) {
            icon()
        }
        Spacer(modifier = Modifier.width(spacing))
        Text(
            text = "$name: $count",
            style = MaterialTheme.typography.bodySmall,
            fontSize = fontSize
        )
    }
}

@Composable
private fun DefenderTypeIconSimple(
    defenderType: DefenderType,
    isMobile: Boolean = false
) {
    val iconSize = if (isMobile) 20.dp else 32.dp
    
    // Use the proper tower icon with hexagon shape and game color
    Box(
        modifier = Modifier
            .size(iconSize)
            .clip(HexagonShape())
            .background(Color(0xFF2196F3)) // same color as in the game
    ) {
        TowerTypeIcon(defenderType = defenderType)
    }
}
