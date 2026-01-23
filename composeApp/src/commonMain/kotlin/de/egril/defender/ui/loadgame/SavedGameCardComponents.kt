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
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res


@Composable
fun SavedGameCardHeader(
    levelName: String,
    dateStr: String
) {
    Text(
        text = levelName,
        style = MaterialTheme.typography.titleMedium
    )
    
    Spacer(modifier = Modifier.height(4.dp))
    
    Text(
        text = dateStr,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun SavedGameCardStats(
    turnNumber: Int,
    coins: Int,
    healthPoints: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Turn number
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${stringResource(Res.string.turn)} $turnNumber",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Coins
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoneyIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$coins",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Health Points
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            de.egril.defender.ui.icon.HeartIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$healthPoints",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SavedGameCardComment(comment: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "💬",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = comment,
            style = MaterialTheme.typography.bodyMedium,
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
    onDownload: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Column 1: Built towers
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top
        ) {
            TowersList(defenderCounts = saveGame.defenderCounts)
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
                barricadeCount = saveGame.barricadeCount
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
                onDownload = onDownload
            )
        }
    }
}

@Composable
fun TowersList(defenderCounts: Map<DefenderType, Int>) {
    if (defenderCounts.isNotEmpty()) {
        Text(
            text = stringResource(Res.string.built_towers),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            defenderCounts.entries.forEach { (type, count) ->
                UnitEntry(
                    icon = { DefenderTypeIconSimple(type) },
                    name = type.getLocalizedName(locale = com.hyperether.resources.currentLanguage.value),
                    count = count
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
    barricadeCount: Int = 0
) {
    val locale = com.hyperether.resources.currentLanguage.value
    
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
                            count = count
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
                            count = count
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
                                    de.egril.defender.ui.icon.HoleIcon(size = 20.dp)
                                }
                            },
                            name = stringResource(Res.string.trap),
                            count = dwarvenTrapCount
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
                            count = magicalTrapCount
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
                            count = barricadeCount
                        )
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
    onDownload: () -> Unit = {}
) {
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

@Composable
private fun UnitEntry(
    icon: @Composable () -> Unit,
    name: String,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = Modifier.size(32.dp)) {
            icon()
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$name: $count",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DefenderTypeIconSimple(defenderType: DefenderType) {
    // Use the proper tower icon with hexagon shape and game color
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(HexagonShape())
            .background(Color(0xFF2196F3)) // same color as in the game
    ) {
        TowerTypeIcon(defenderType = defenderType)
    }
}
