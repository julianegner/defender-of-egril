package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.LevelStatus
import com.defenderofegril.model.WorldLevel
import com.defenderofegril.model.getEnemyTypeCounts

@Composable
fun WorldMapScreen(
    worldLevels: List<WorldLevel>,
    onLevelSelected: (Int) -> Unit,
    onBackToMenu: () -> Unit,
    onCheatCode: ((String) -> Boolean)? = null  // Callback for processing cheat codes, returns true if code was valid
) {
    var showCheatDialog by remember { mutableStateOf(false) }
    var cheatCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title text - clickable for cheat code access (less obvious than a button)
        Text(
            text = "World Map - Meadows of Egril",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .then(
                    if (onCheatCode != null) {
                        Modifier.clickable { showCheatDialog = true }
                    } else {
                        Modifier
                    }
                )
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(worldLevels) { worldLevel ->
                LevelCard(
                    worldLevel = worldLevel,
                    onClick = {
                        if (worldLevel.status != LevelStatus.LOCKED) {
                            onLevelSelected(worldLevel.level.id)
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onBackToMenu) {
            Text("Back to Menu")
        }
    }
    
    // Cheat code dialog
    if (showCheatDialog && onCheatCode != null) {
        AlertDialog(
            onDismissRequest = {
                showCheatDialog = false
                cheatCodeInput = ""
                errorMessage = ""
            },
            title = { Text("Cheat Code") },
            text = {
                Column {
                    Text("Enter cheat code:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = cheatCodeInput,
                        onValueChange = { 
                            cheatCodeInput = it
                            errorMessage = ""  // Clear error when user types
                        },
                        placeholder = { Text("unlock") },
                        isError = errorMessage.isNotEmpty()
                    )
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onCheatCode(cheatCodeInput)
                        if (success) {
                            showCheatDialog = false
                            cheatCodeInput = ""
                            errorMessage = ""
                        } else {
                            errorMessage = "Invalid cheat code"
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showCheatDialog = false
                        cheatCodeInput = ""
                        errorMessage = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LevelCard(
    worldLevel: WorldLevel,
    onClick: () -> Unit
) {
    val backgroundColor = when (worldLevel.status) {
        LevelStatus.LOCKED -> Color(0xFF9E9E9E)
        LevelStatus.UNLOCKED -> Color(0xFF2196F3)
        LevelStatus.WON -> Color(0xFF4CAF50)
    }
    
    val statusText = when (worldLevel.status) {
        LevelStatus.LOCKED -> "🔒 Locked"
        LevelStatus.UNLOCKED -> "⚔️ Available"
        LevelStatus.WON -> "✓ Completed"
    }
    
    // Get enemy counts for this level
    val enemyCounts = worldLevel.level.getEnemyTypeCounts()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(enabled = worldLevel.status != LevelStatus.LOCKED, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header row with level number and name
            Column {
                Text(
                    text = "Level ${worldLevel.level.id}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                
                Text(
                    text = worldLevel.level.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Enemy units display
            if (enemyCounts.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    enemyCounts.entries.take(3).forEach { (attackerType, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // Enemy icon
                            Box(
                                modifier = Modifier.size(24.dp)
                            ) {
                                EnemyTypeIcon(attackerType = attackerType)
                            }
                            
                            Spacer(modifier = Modifier.width(6.dp))
                            
                            // Enemy name and count
                            Text(
                                text = "${attackerType.displayName}: ${count}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    // Show "and more" if there are more than 3 enemy types
                    if (enemyCounts.size > 3) {
                        Text(
                            text = "... and ${enemyCounts.size - 3} more type${if (enemyCounts.size - 3 > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            // Status at the bottom
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
