package de.egril.defender.ui.worldmap

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
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel
import de.egril.defender.ui.getLocalizedTitle
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.LockIcon
import de.egril.defender.ui.icon.SwordIcon
import de.egril.defender.editor.EditorStorage
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.LevelInfoEnemiesColumn
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.loadgame.SavefileLocationChip
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res


@Composable
fun LevelCard(
    worldLevel: WorldLevel,
    onClick: () -> Unit
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    val currentDifficulty = de.egril.defender.ui.settings.AppSettings.difficulty.value
    
    val backgroundColor = when (worldLevel.status) {
        LevelStatus.LOCKED -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF9E9E9E)
        LevelStatus.UNLOCKED -> if (isDarkMode) Color(0xFF0D47A1) else Color(0xFF2196F3)
        LevelStatus.WON -> if (isDarkMode) Color(0xFF1B5E20) else Color(0xFF4CAF50)
    }
    
    // Text color changes based on dark mode - darker text for better readability
    val textColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.White
    
    val statusText = when (worldLevel.status) {
        LevelStatus.LOCKED -> stringResource(Res.string.locked)
        LevelStatus.UNLOCKED -> stringResource(Res.string.available)
        LevelStatus.WON -> stringResource(Res.string.completed)
    }
    
    // Get prerequisite info for locked levels
    val editorLevel = worldLevel.level.editorLevelId?.let { EditorStorage.getLevel(it) }
    val prerequisites = editorLevel?.prerequisites ?: emptySet()
    val requiredCount = editorLevel?.getEffectiveRequiredCount() ?: 0

    // Community level detection: check local community storage
    val communityEditorLevel = worldLevel.level.editorLevelId?.let { EditorStorage.getCommunityLevel(it) }
    val isLocalCommunity = communityEditorLevel != null
    // All locally-stored community levels were downloaded from the backend, so they are also online
    val isOnlineCommunity = isLocalCommunity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (worldLevel.status == LevelStatus.LOCKED && prerequisites.isNotEmpty()) 240.dp else 200.dp)
            .clickable(enabled = worldLevel.status != LevelStatus.LOCKED, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pass difficulty to get modified values
            LevelInfoEnemiesColumn(worldLevel.level.toLevelInfoEnemiesLevelData(currentDifficulty), textColor)

            // Right column: Minimap and status
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Local / Online chips for community levels (same position as savefile card chips)
                if (isLocalCommunity || isOnlineCommunity) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLocalCommunity) {
                            SavefileLocationChip(
                                label = stringResource(Res.string.savefile_chip_local),
                                color = MaterialTheme.colorScheme.tertiary,
                                onColor = MaterialTheme.colorScheme.onTertiary,
                                isMobile = false
                            )
                        }
                        if (isOnlineCommunity) {
                            SavefileLocationChip(
                                label = stringResource(Res.string.savefile_chip_remote),
                                color = MaterialTheme.colorScheme.primary,
                                onColor = MaterialTheme.colorScheme.onPrimary,
                                isMobile = false
                            )
                        }
                    }
                }

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
                
                // Prerequisites info (show when locked)
                if (worldLevel.status == LevelStatus.LOCKED && prerequisites.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val prereqText = if (requiredCount < prerequisites.size) {
                            stringResource(Res.string.requires_any_of, requiredCount.toString())
                        } else {
                            stringResource(Res.string.requires_all)
                        }
                        
                        Text(
                            text = prereqText,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                        
                        // List prerequisite level names (up to 3)
                        val locale = com.hyperether.resources.currentLanguage.value
                        for (prereqId in prerequisites.take(3)) {
                            val prereqLevel = EditorStorage.getLevel(prereqId)
                            val prereqName = prereqLevel?.getLocalizedTitle(locale) ?: prereqId
                            
                            Text(
                                text = "• $prereqName",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        if (prerequisites.size > 3) {
                            Text(
                                text = "• ...",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
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
