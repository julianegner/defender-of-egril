package de.egril.defender.ui.loadgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.egril.defender.game.LevelData
import de.egril.defender.model.*
import de.egril.defender.save.SaveGameMetadata
import de.egril.defender.utils.formatTimestamp
import de.egril.defender.ui.getLocalizedTitle

@Composable
fun SavedGameCard(
    saveGame: SaveGameMetadata,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit = {}
) {
    val dateStr = formatTimestamp(saveGame.timestamp)
    
    // Get the level to access map for minimap
    val levels = remember { LevelData.createLevels() }
    val level = remember(saveGame.levelId, saveGame.mapId) {
        // Try to find level by mapId first (more reliable)
        if (saveGame.mapId != null) {
            levels.find { it.mapId == saveGame.mapId }
        } else {
            // Fallback to levelId for backward compatibility
            levels.find { it.id == saveGame.levelId }
        }
    }
    
    // Create a minimal GameState for minimap rendering
    val minimapGameState = remember(saveGame.id) {
        if (level != null && (saveGame.defenderPositions.isNotEmpty() || saveGame.attackerPositions.isNotEmpty())) {
            // Create minimal Defender/Attacker objects for minimap display
            val defenders = saveGame.defenderPositions.map { saved ->
                Defender(
                    id = saved.id,
                    type = saved.type,
                    position = mutableStateOf(saved.position),
                    level = mutableStateOf(saved.level),
                    buildTimeRemaining = mutableStateOf(saved.buildTimeRemaining),
                    actionsRemaining = mutableStateOf(0),
                    placedOnTurn = saved.placedOnTurn,
                    hasBeenUsed = mutableStateOf(false),
                    dragonId = mutableStateOf(null)
                )
            }
            val attackers = saveGame.attackerPositions.filter { !it.isDefeated }.map { saved ->
                Attacker(
                    id = saved.id,
                    type = saved.type,
                    position = mutableStateOf(saved.position),
                    level = mutableStateOf(saved.level),
                    currentHealth = mutableStateOf(saved.currentHealth),
                    isDefeated = mutableStateOf(false)
                )
            }
            // Create a minimal GameState with just the data needed for rendering
            GameState(
                level = level,
                defenders = androidx.compose.runtime.snapshots.SnapshotStateList<Defender>().apply { addAll(defenders) },
                attackers = androidx.compose.runtime.snapshots.SnapshotStateList<Attacker>().apply { addAll(attackers) }
            )
        } else {
            null
        }
    }
    
    BoxWithConstraints {
        val isMobileCard = maxWidth < 600.dp
        val cardPadding = if (isMobileCard) 8.dp else 16.dp
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLoad() },
            elevation = CardDefaults.cardElevation(defaultElevation = if (isMobileCard) 2.dp else 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(cardPadding)) {
                // Get localized level name if level is available
                val locale = com.hyperether.resources.currentLanguage.value
                val displayName = if (level != null) {
                    level.getLocalizedTitle(locale)
                } else {
                    saveGame.levelName  // Fallback to saved name if level not found
                }
                
                SavedGameCardHeader(
                    levelName = displayName,
                    dateStr = dateStr,
                    isMobile = isMobileCard
                )
                
                Spacer(modifier = Modifier.height(if (isMobileCard) 4.dp else 8.dp))
                
                SavedGameCardStats(
                    turnNumber = saveGame.turnNumber,
                    coins = saveGame.coins,
                    healthPoints = saveGame.healthPoints,
                    isMobile = isMobileCard
                )
                
                // Display comment if present
                if (!saveGame.comment.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(if (isMobileCard) 4.dp else 8.dp))
                    SavedGameCardComment(
                        comment = saveGame.comment,
                        isMobile = isMobileCard
                    )
                }
                
                Spacer(modifier = Modifier.height(if (isMobileCard) 6.dp else 12.dp))
                
                SavedGameCardUnitsAndMinimap(
                    saveGame = saveGame,
                    level = level,
                    minimapGameState = minimapGameState,
                    onDelete = onDelete,
                    onDownload = onDownload,
                    isMobile = isMobileCard
                )
            }
        }
    }
}
