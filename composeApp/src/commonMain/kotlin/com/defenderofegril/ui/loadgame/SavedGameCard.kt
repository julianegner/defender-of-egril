package com.defenderofegril.ui.loadgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defenderofegril.game.LevelData
import com.defenderofegril.model.*
import com.defenderofegril.save.SaveGameMetadata
import com.defenderofegril.utils.formatTimestamp

@Composable
fun SavedGameCard(
    saveGame: SaveGameMetadata,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = formatTimestamp(saveGame.timestamp)
    
    // Get the level to access map for minimap
    val levels = remember { LevelData.createLevels() }
    val level = remember(saveGame.levelId) { 
        levels.find { it.id == saveGame.levelId }
    }
    
    // Create a minimal GameState for minimap rendering
    val minimapGameState = remember(saveGame.id) {
        if (level != null && (saveGame.defenderPositions.isNotEmpty() || saveGame.attackerPositions.isNotEmpty())) {
            // Create minimal Defender/Attacker objects for minimap display
            val defenders = saveGame.defenderPositions.map { saved ->
                Defender(
                    id = saved.id,
                    type = saved.type,
                    position = saved.position,
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLoad() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SavedGameCardHeader(
                levelName = saveGame.levelName,
                dateStr = dateStr
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SavedGameCardStats(
                turnNumber = saveGame.turnNumber,
                coins = saveGame.coins
            )
            
            // Display comment if present
            if (!saveGame.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                SavedGameCardComment(comment = saveGame.comment)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SavedGameCardUnitsAndMinimap(
                saveGame = saveGame,
                level = level,
                minimapGameState = minimapGameState,
                onDelete = onDelete
            )
        }
    }
}
