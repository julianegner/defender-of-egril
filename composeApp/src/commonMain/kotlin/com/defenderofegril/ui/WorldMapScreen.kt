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
import com.defenderofegril.model.LevelStatus
import com.defenderofegril.model.WorldLevel

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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(enabled = worldLevel.status != LevelStatus.LOCKED, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Level ${worldLevel.level.id}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            
            Text(
                text = worldLevel.level.name,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
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
