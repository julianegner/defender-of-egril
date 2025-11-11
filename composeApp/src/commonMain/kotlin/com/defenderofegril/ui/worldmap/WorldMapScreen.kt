@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package com.defenderofegril.ui.worldmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defenderofegril.model.LevelStatus
import com.defenderofegril.model.WorldLevel
import com.defenderofegril.ui.CheatCodeDialog
import com.defenderofegril.ui.isEditorAvailable
import com.defenderofegril.ui.settings.SettingsButton
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

@Composable
fun WorldMapScreen(
    worldLevels: List<WorldLevel>,
    onLevelSelected: (Int) -> Unit,
    onBackToMenu: () -> Unit,
    onShowRules: () -> Unit,
    onOpenEditor: () -> Unit,
    onLoadGame: () -> Unit,
    onCheatCode: ((String) -> Boolean)? = null  // Callback for processing cheat codes, returns true if code was valid
) {
    var showCheatDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Settings button in top-right corner
            SettingsButton(modifier = Modifier.align(Alignment.TopEnd))
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title text - clickable for cheat code access (less obvious than a button)
                Text(
                    text = stringResource(Res.string.world_map_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
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
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = onLoadGame) {
                    Text(stringResource(Res.string.load_game))
                }
                
                Button(onClick = onShowRules) {
                    Text(stringResource(Res.string.rules))
                }
                
                Button(onClick = onBackToMenu) {
                    Text(stringResource(Res.string.back))
                }
                
                // Add Editor Button at the right end (only on desktop/wasm)
                if (isEditorAvailable()) {
                    EditorButtonCard(onClick = onOpenEditor)
                }
            }
        }
        }
    }
    
    // Cheat code dialog
    if (showCheatDialog && onCheatCode != null) {
        CheatCodeDialog(
            onDismiss = { showCheatDialog = false },
            onApplyCheatCode = onCheatCode
        )
    }
}
