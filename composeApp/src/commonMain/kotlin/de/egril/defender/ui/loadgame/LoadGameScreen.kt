@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.loadgame

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.egril.defender.save.SaveGameMetadata
import de.egril.defender.save.getFileExportImport
import de.egril.defender.save.SaveFileStorage
import de.egril.defender.editor.getFileStorage
import de.egril.defender.ui.settings.SettingsButton
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.launch

@Composable
fun LoadGameScreen(
    savedGames: List<SaveGameMetadata>,
    onLoadGame: (String) -> Unit,
    onDeleteGame: (String) -> Unit,
    onDownloadGame: (String, Boolean) -> Unit,  // Added includeGameState parameter
    onDownloadAll: (Boolean) -> Unit,  // Added includeGameState parameter
    onExportGameProgress: () -> Unit,  // New: Export just game progress
    onImportGameProgress: (String) -> Unit,  // New: Import game progress
    onUpload: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showFileOverrideDialog by remember { mutableStateOf<String?>(null) }
    var filesImported by remember { mutableStateOf(0) }
    var showImportSuccess by remember { mutableStateOf(false) }
    var showImportError by remember { mutableStateOf(false) }
    var gameDataTransferEnabled by remember { mutableStateOf(false) }  // New: Toggle state
    
    // Pending imports waiting for override decision
    var pendingImports by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var currentImportIndex by remember { mutableStateOf(0) }
    var overrideAll by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    val handleUpload: () -> Unit = {
        scope.launch {
            val fileExportImport = getFileExportImport()
            val importedFiles = fileExportImport.importFiles()
            
            if (importedFiles != null && importedFiles.isNotEmpty()) {
                var successCount = 0
                val conflicts = mutableListOf<Pair<String, String>>()
                
                // First pass: Check file types and import
                importedFiles.forEach { file ->
                    // Check if this is a game progress file (contains only levelStatuses)
                    val isGameProgressFile = file.content.contains("\"levelStatuses\"") && 
                                           !file.content.contains("\"levelId\"") &&
                                           !file.content.contains("\"defenders\"")
                    
                    if (isGameProgressFile) {
                        // This is a game progress file - import it via the callback
                        onImportGameProgress(file.content)
                        successCount++
                    } else {
                        // This is a save game file
                        if (SaveFileStorage.saveGameExists(file.filename)) {
                            conflicts.add(Pair(file.filename, file.content))
                        } else {
                            if (SaveFileStorage.importSaveGame(file.filename, file.content, overwrite = false)) {
                                successCount++
                            }
                        }
                    }
                }
                
                if (conflicts.isNotEmpty()) {
                    // Show override dialog for first conflict
                    pendingImports = conflicts
                    currentImportIndex = 0
                    overrideAll = false
                    showFileOverrideDialog = conflicts[0].first
                } else if (successCount > 0) {
                    filesImported = successCount
                    showImportSuccess = true
                } else {
                    showImportError = true
                }
                
                if (successCount > 0) {
                    // Refresh the list to show newly imported saves
                    onUpload() // This triggers a refresh in the parent
                }
            }
        }
    }
    
    val handleOverrideDecision: (Boolean) -> Unit = { override ->
        scope.launch {
            if (currentImportIndex < pendingImports.size) {
                val (filename, content) = pendingImports[currentImportIndex]
                
                if (override || overrideAll) {
                    if (SaveFileStorage.importSaveGame(filename, content, overwrite = true)) {
                        filesImported++
                    }
                }
                
                currentImportIndex++
                
                // Check if there are more conflicts
                if (currentImportIndex < pendingImports.size && !overrideAll) {
                    showFileOverrideDialog = pendingImports[currentImportIndex].first
                } else {
                    // Done with all imports
                    showFileOverrideDialog = null
                    if (filesImported > 0) {
                        showImportSuccess = true
                        onUpload() // Refresh
                    }
                    // Reset state
                    pendingImports = emptyList()
                    currentImportIndex = 0
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
                    text = stringResource(Res.string.load_game),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Game Data Transfer toggle and Export Game Progress button
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        // Toggle row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.game_data_transfer),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(Res.string.game_data_transfer_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = gameDataTransferEnabled,
                                onCheckedChange = { gameDataTransferEnabled = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Export Game Progress button
                        Button(
                            onClick = onExportGameProgress,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            de.egril.defender.ui.icon.DownloadIcon(size = 16.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(Res.string.export_game_progress))
                        }
                    }
                }
                
                // Download All and Upload buttons row (only show if there are saved games)
                if (savedGames.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = { onDownloadAll(gameDataTransferEnabled) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            de.egril.defender.ui.icon.DownloadIcon(size = 16.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(Res.string.download_all_savefiles))
                        }
                        
                        Button(
                            onClick = handleUpload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            de.egril.defender.ui.icon.UploadIcon(size = 16.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(Res.string.upload_savefiles))
                        }
                    }
                }
            
            if (savedGames.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.no_saved_games),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Show upload button even when no saves exist
                        Button(onClick = handleUpload) {
                            de.egril.defender.ui.icon.UploadIcon(size = 16.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(Res.string.upload_savefiles))
                        }
                    }
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
                            onDelete = { showDeleteDialog = saveGame.id },
                            onDownload = { onDownloadGame(saveGame.id, gameDataTransferEnabled) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onBack) {
                Text(stringResource(Res.string.back))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display savegame folder path at the bottom
            val fileStorage = remember { getFileStorage() }
            val savegamePath = remember {
                // Get the path for the savefiles directory
                val playerId = SaveFileStorage.getCurrentPlayer()
                val savefilesPath = if (playerId != null) {
                    "players/$playerId/savefiles"
                } else {
                    "savefiles"
                }
                fileStorage.getAbsolutePath(savefilesPath)
            }
            
            Text(
                text = "${stringResource(Res.string.savegame_folder)} $savegamePath",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
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
    
    // File override dialog
    FileOverrideDialog(
        filename = showFileOverrideDialog,
        onSkip = {
            handleOverrideDecision(false)
        },
        onOverride = {
            handleOverrideDecision(true)
        },
        onOverrideAll = {
            overrideAll = true
            handleOverrideDecision(true)
        },
        onDismiss = {
            showFileOverrideDialog = null
            pendingImports = emptyList()
            currentImportIndex = 0
            if (filesImported > 0) {
                showImportSuccess = true
            }
        }
    )
    
    // Import success dialog
    ImportSuccessDialog(
        filesImported = if (showImportSuccess) filesImported else 0,
        onDismiss = {
            showImportSuccess = false
            filesImported = 0
        }
    )
    
    // Import error dialog
    ImportErrorDialog(
        showError = showImportError,
        onDismiss = { showImportError = false }
    )
}
