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
import de.egril.defender.ui.settings.DifficultyDisplay
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.NewRepositoryDataDialog
import de.egril.defender.editor.RepositoryManager
import de.egril.defender.utils.isPlatformMobile
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontStyle

// Button sizing constants for world map bottom bar
private val BUTTON_WIDTH_MOBILE_IMAGE_MAP = 133.dp  // ~33% smaller than default for compact mobile layout
private val BUTTON_WIDTH_DEFAULT = 200.dp  // Standard button width for desktop and mobile level cards view

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
    var selectedLocation by remember { mutableStateOf<Pair<WorldMapLocation, List<WorldLevel>>?>(null) }
    
    // Watch the setting for world map style
    val useLevelCards = AppSettings.useLevelCards.value
    
    val scope = rememberCoroutineScope()
    
    // Start background music when entering world map
    LaunchedEffect(Unit) {
        de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
            de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
            loop = true
        )
    }
    
    // Stop background music when leaving world map
    DisposableEffect(Unit) {
        onDispose {
            de.egril.defender.audio.GlobalBackgroundMusicManager.stopMusic()
        }
    }
    
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Content area - switches between image map and level cards based on setting
            if (useLevelCards) {
                // Level cards view - grid of level cards
                LevelCardsView(
                    worldLevels = worldLevels,
                    onLevelSelected = onLevelSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp, bottom = 80.dp)  // Leave space for top/bottom bars
                )
            } else {
                // Image-based World Map as background with clickable locations
                ImageWorldMapView(
                    worldLevels = worldLevels,
                    onLocationClicked = { location, levelsAtLocation ->
                        selectedLocation = location to levelsAtLocation
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Top bar with title and buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and subtitle - clickable for cheat code access (less obvious than a button)
                Column(
                    modifier = Modifier
                        .then(
                            if (onCheatCode != null) {
                                Modifier.clickable { showCheatDialog = true }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Text(
                        text = stringResource(Res.string.world_map_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(Res.string.world_map_subtitle),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // Difficulty and Settings button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Difficulty display (clickable to open dropdown)
                    DifficultyDisplay(
                        isClickable = true,
                        modifier = Modifier
                    )
                    
                    // Settings button
                    SettingsButton()
                }
            }
            
            // Bottom bar with action buttons
            // Determine button arrangement based on platform and view mode
            val isMobileImageMap = isPlatformMobile && !useLevelCards
            val isMobileLevelCards = isPlatformMobile && useLevelCards
            val buttonArrangement = when {
                isMobileLevelCards -> Arrangement.Center
                isMobileImageMap -> Arrangement.Start
                else -> Arrangement.Center  // Desktop
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = buttonArrangement,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button width: smaller on mobile in image map view, standard otherwise
                val buttonMinWidth = if (isMobileImageMap) BUTTON_WIDTH_MOBILE_IMAGE_MAP else BUTTON_WIDTH_DEFAULT
                
                // Button layout varies by platform and view mode:
                // - Mobile + Image Map: Column layout, left-aligned, smaller buttons
                // - Mobile + Level Cards: Row layout, centered, normal buttons
                // - Desktop: Row layout, centered
                when {
                    isMobileImageMap -> {
                        // Mobile + Image Map View: Column layout, left-aligned, smaller buttons
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Button(
                                onClick = onLoadGame,
                                modifier = Modifier.widthIn(min = buttonMinWidth)
                            ) {
                                Text(stringResource(Res.string.load_game))
                            }
                            
                            Button(
                                onClick = onShowRules,
                                modifier = Modifier.widthIn(min = buttonMinWidth)
                            ) {
                                Text(stringResource(Res.string.rules))
                            }
                            
                            Button(
                                onClick = onBackToMenu,
                                modifier = Modifier.widthIn(min = buttonMinWidth)
                            ) {
                                Text(stringResource(Res.string.back))
                            }
                        }
                    }
                    isMobileLevelCards -> {
                        // Mobile + Level Cards View: Row layout, centered, normal buttons
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
                    }
                    else -> {
                        // Desktop: Row layout, centered
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
                    }
                }
                
                // Spacer to push editor button to the right (only on desktop/wasm)
                if (!isPlatformMobile && isEditorAvailable()) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Editor Button at the right end (only on desktop/wasm)
                if (isEditorAvailable()) {
                    EditorButtonCard(onClick = onOpenEditor)
                }
            }
        }
    }
    
    // Level location dialog - shows all levels at the clicked location
    if (selectedLocation != null) {
        val (location, levels) = selectedLocation!!
        LevelLocationDialog(
            location = location,
            levelsAtLocation = levels,
            onPlayLevel = { levelId ->
                onLevelSelected(levelId)
                selectedLocation = null
            },
            onDismiss = { selectedLocation = null }
        )
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
