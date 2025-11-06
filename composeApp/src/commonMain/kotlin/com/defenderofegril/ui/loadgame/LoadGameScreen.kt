package com.defenderofegril.ui.loadgame

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defenderofegril.save.SaveGameMetadata
import com.defenderofegril.ui.settings.SettingsButton
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage

@Composable
fun LoadGameScreen(
    savedGames: List<SaveGameMetadata>,
    onLoadGame: (String) -> Unit,
    onDeleteGame: (String) -> Unit,
    onBack: () -> Unit
) {
    val locale = currentLanguage.value
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Settings button in top-right corner
        SettingsButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = LocalizedStrings.get("load_game", locale),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (savedGames.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved games found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedGames) { saveGame ->
                        SavedGameCard(
                            saveGame = saveGame,
                            onLoad = { onLoadGame(saveGame.id) },
                            onDelete = { showDeleteDialog = saveGame.id }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onBack) {
                Text(LocalizedStrings.get("back", locale))
            }
        }
    }
    
    // Delete confirmation dialog
    DeleteConfirmationDialog(
        saveIdToDelete = showDeleteDialog,
        onConfirmDelete = { saveId ->
            onDeleteGame(saveId)
            showDeleteDialog = null
        },
        onDismiss = { showDeleteDialog = null }
    )
}
