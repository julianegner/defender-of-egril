package de.egril.defender.ui.worldmap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.common.LevelInfoEnemiesColumn
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.LockIcon
import de.egril.defender.ui.icon.SwordIcon
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Dialog overlay showing level details with a "Play Level" button
 */
@Composable
fun LevelCardOverlay(
    levelInfo: WorldMapLevelInfo,
    worldLevels: List<WorldLevel>,
    onPlayLevel: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkMode = AppSettings.isDarkMode.value
    val currentDifficulty = AppSettings.difficulty.value
    
    // Find the actual WorldLevel for this levelInfo
    val worldLevel = worldLevels.find { it.level.editorLevelId == levelInfo.levelId }
    
    if (worldLevel == null) {
        // If we can't find the level, dismiss the dialog
        onDismiss()
        return
    }
    
    val backgroundColor = when (levelInfo.status) {
        LevelStatus.LOCKED -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF9E9E9E)
        LevelStatus.UNLOCKED -> if (isDarkMode) Color(0xFF0D47A1) else Color(0xFF2196F3)
        LevelStatus.WON -> if (isDarkMode) Color(0xFF1B5E20) else Color(0xFF4CAF50)
    }
    
    val textColor = if (isDarkMode) Color.Black else Color.White
    val isPlayable = levelInfo.status != LevelStatus.LOCKED
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Level name and subtitle
                Text(
                    text = levelInfo.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor
                )
                
                if (levelInfo.subtitle.isNotBlank()) {
                    Text(
                        text = levelInfo.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status indicator row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (levelInfo.status) {
                        LevelStatus.LOCKED -> {
                            LockIcon(size = 16.dp)
                            Text(
                                text = stringResource(Res.string.locked),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                        }
                        LevelStatus.UNLOCKED -> {
                            SwordIcon(size = 16.dp)
                            Text(
                                text = stringResource(Res.string.available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                        }
                        LevelStatus.WON -> {
                            CheckmarkIcon(size = 16.dp, tint = textColor)
                            Text(
                                text = stringResource(Res.string.completed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Level info content (similar to LevelCard)
                Row(
                    // we need the height because LevelInfoEnemiesColumn uses fillMaxHeight
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left column: Level info with enemies
                    LevelInfoEnemiesColumn(
                        level = worldLevel.level.toLevelInfoEnemiesLevelData(currentDifficulty),
                        textColor = textColor
                    )
                    
                    // Right column: Minimap
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .height(120.dp)
                    ) {
                        HexagonMinimap(
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
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Prerequisites info (if any and level is locked)
                if (levelInfo.status == LevelStatus.LOCKED && levelInfo.prerequisites.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.requires_completing),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    
                    val prereqNames = levelInfo.prerequisites.mapNotNull { prereqId ->
                        worldLevels.find { it.level.editorLevelId == prereqId }?.level?.name
                    }
                    
                    Text(
                        text = prereqNames.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    // Close button
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textColor
                        )
                    ) {
                        Text(stringResource(Res.string.close))
                    }
                    
                    // Play Level button (only active if level is unlocked)
                    Button(
                        onClick = { 
                            if (isPlayable) {
                                onPlayLevel(worldLevel.level.id)
                            }
                        },
                        enabled = isPlayable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayable) {
                                if (isDarkMode) Color(0xFF2E7D32) else Color(0xFF4CAF50)
                            } else {
                                Color.Gray
                            },
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(stringResource(Res.string.play_level))
                    }
                }
            }
        }
    }
}
