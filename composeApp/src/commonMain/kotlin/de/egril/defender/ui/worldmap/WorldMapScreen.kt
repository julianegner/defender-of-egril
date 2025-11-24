@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.worldmap

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
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel
import de.egril.defender.ui.CheatCodeDialog
import de.egril.defender.ui.isEditorAvailable
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.ui.NewRepositoryDataDialog
import de.egril.defender.editor.RepositoryManager
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.launch

@Composable
fun WorldMapScreen(
    worldLevels: List<WorldLevel>,
    onLevelSelected: (Int) -> Unit,
    onBackToMenu: () -> Unit,
    onShowRules: () -> Unit,
    onOpenEditor: () -> Unit,
    onLoadGame: () -> Unit,
    onCheatCode: ((String) -> Boolean)? = null,  // Callback for processing cheat codes, returns true if code was valid
    onReloadWorldMap: (() -> Unit)? = null,  // Callback to reload world map after syncing repository files
    checkForNewRepositoryData: Boolean = true  // Set to false in tests to avoid repository checks
) {
    var showCheatDialog by remember { mutableStateOf(false) }
    var showNewRepoDataDialog by remember { mutableStateOf(false) }
    var newRepoData by remember { mutableStateOf<RepositoryManager.NewRepositoryData?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Check for new repository data on first load (if enabled)
    LaunchedEffect(checkForNewRepositoryData) {
        if (checkForNewRepositoryData) {
            scope.launch {
                try {
                    val detectedData = RepositoryManager.detectNewRepositoryFiles()
                    if (detectedData != null) {
                        newRepoData = detectedData
                        showNewRepoDataDialog = true
                    }
                } catch (e: Exception) {
                    // Silently ignore repository check errors to avoid disrupting the user experience
                    println("Info: Repository check skipped - ${e.message}")
                }
            }
        }
    }
    
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Centered buttons group
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
                }
                
                // Spacer to push editor button to the right
                Spacer(modifier = Modifier.weight(1f))
                
                // Editor Button at the right end (only on desktop/wasm)
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
    
    // New repository data dialog
    if (showNewRepoDataDialog && newRepoData != null) {
        NewRepositoryDataDialog(
            newData = newRepoData!!,
            onAccept = {
                scope.launch {
                    try {
                        val success = RepositoryManager.syncNewRepositoryFiles()
                        if (success) {
                            println("Successfully synced new repository files")
                            // Reload the world map to show the new levels
                            onReloadWorldMap?.invoke()
                        } else {
                            println("Failed to sync repository files")
                        }
                    } catch (e: Exception) {
                        println("Error syncing repository files: ${e.message}")
                        e.printStackTrace()
                    }
                    showNewRepoDataDialog = false
                }
            },
            onDismiss = {
                showNewRepoDataDialog = false
            }
        )
    }
}
