package com.defenderofegril.ui.worldmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.LevelStatus
import com.defenderofegril.model.WorldLevel
import com.defenderofegril.model.getEnemyTypeCounts
import com.defenderofegril.ui.*
import com.defenderofegril.ui.icon.CheckmarkIcon
import com.defenderofegril.ui.icon.HeartIcon
import com.defenderofegril.ui.icon.LockIcon
import com.defenderofegril.ui.icon.MoneyIcon
import com.defenderofegril.ui.icon.SwordIcon
import com.defenderofegril.ui.icon.enemy.EnemyTypeIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res


@Composable
fun LevelCard(
    worldLevel: WorldLevel,
    onClick: () -> Unit
) {
    val isDarkMode = com.defenderofegril.ui.settings.AppSettings.isDarkMode.value
    
    val backgroundColor = when (worldLevel.status) {
        LevelStatus.LOCKED -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF9E9E9E)
        LevelStatus.UNLOCKED -> if (isDarkMode) Color(0xFF0D47A1) else Color(0xFF2196F3)
        LevelStatus.WON -> if (isDarkMode) Color(0xFF1B5E20) else Color(0xFF4CAF50)
    }
    
    // Text color changes based on dark mode - darker text for better readability
    val textColor = if (isDarkMode) Color.Black else Color.White
    
    val statusText = when (worldLevel.status) {
        LevelStatus.LOCKED -> stringResource(Res.string.locked)
        LevelStatus.UNLOCKED -> stringResource(Res.string.available)
        LevelStatus.WON -> stringResource(Res.string.completed)
    }
    
    // Get enemy counts for this level
    val enemyCounts = worldLevel.level.getEnemyTypeCounts()
    val enemyList = enemyCounts.entries.toList()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(enabled = worldLevel.status != LevelStatus.LOCKED, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left column: Level info, coins, health, and enemies
            Column(
                modifier = Modifier.weight(2f).fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                Row {
                    // Header: level number and name
                    Column {
                        Text(
                            text = "Level ${worldLevel.level.id}",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontSize = 18.sp
                        )

                        Text(
                            text = worldLevel.level.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Coins and Health display
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoneyIcon(size = 12.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${worldLevel.level.initialCoins}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor,
                                    fontSize = 12.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HeartIcon(size = 12.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${worldLevel.level.healthPoints}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    // Enemy units display
                    if (enemyList.isNotEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            enemyList.forEachIndexed { index, (attackerType, count) ->
                                if (index % 2 == 0) {
                                    EnemyUnitEntry(attackerType, count, textColor)
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            enemyList.forEachIndexed { index, (attackerType, count) ->
                                if (index % 2 == 1) {
                                    EnemyUnitEntry(attackerType, count, textColor)
                                }
                            }
                        }
                    }
                }
            }

            // Right column: Minimap and status
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                // horizontalAlignment = Alignment.End
            ) {
                // Minimap preview
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(120.dp)
                        .padding(top = 20.dp)
                ) {
                    val mapName = HexagonMinimap(
                        level = worldLevel.level,
                        config = MinimapConfig(
                            showSpawnPoints = true,
                            showTarget = true,
                            showTowers = false,
                            showEnemies = false,
                            showViewport = false,
                            backgroundColor = Color.Transparent,
                            borderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = mapName,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        fontSize = 12.sp,
                        modifier = Modifier.absoluteOffset(x = 0.dp, y = (-20).dp)
                    )
                }
                
                // Status at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (worldLevel.status) {
                        LevelStatus.LOCKED -> LockIcon(size = 13.dp)
                        LevelStatus.UNLOCKED -> SwordIcon(size = 13.dp)
                        LevelStatus.WON -> CheckmarkIcon(size = 13.dp, tint = textColor)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        textAlign = TextAlign.End,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EnemyUnitEntry(attackerType: AttackerType, count: Int, textColor: Color) {
    val locale = com.hyperether.resources.currentLanguage.value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier.size(24.dp)
        ) {
            EnemyTypeIcon(attackerType = attackerType)
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "${attackerType.getLocalizedName(locale)}: ${count}",
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontSize = 11.sp
        )
    }
}
