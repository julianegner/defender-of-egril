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
import de.egril.defender.editor.RepositoryManager
import de.egril.defender.utils.isPlatformMobile
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontStyle
import de.egril.defender.config.LogConfig

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
    checkForNewRepositoryData: Boolean = true,  // Set to false in tests to avoid repository checks
    onSwitchPlayer: (() -> Unit)? = null,  // Callback to switch player
    onEditPlayerName: (() -> Unit)? = null,  // Callback to edit player name
    currentPlayerName: String? = null,  // Current player name for display
    showPlatformInfo: Boolean = false,  // Show platform info from cheat code
    onClearPlatformInfo: (() -> Unit)? = null,  // Callback to clear platform info
    showCheatHelp: Boolean = false,  // Show cheat code help screen
    onClearCheatHelp: (() -> Unit)? = null  // Callback to clear cheat help
) {
    var showCheatDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<Pair<WorldMapLocation, List<WorldLevel>>?>(null) }
    
    // Track whether to show user levels tab view in image map mode
    // false = show image map with button, true = show tab view with user levels
    var showUserLevelsTabView by remember { mutableStateOf(false) }
    
    // Watch the setting for world map style
    val useLevelCards = AppSettings.useLevelCards.value
    
    // Watch the setting for showing testing levels
    val showTestingLevels = AppSettings.showTestingLevels.value
    
    // Filter world levels based on testingOnly flag
    val visibleWorldLevels = remember(worldLevels, showTestingLevels) {
        if (showTestingLevels) {
            worldLevels
        } else {
            // Filter out levels marked as testing only
            worldLevels.filter { worldLevel ->
                val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(worldLevel.level.editorLevelId ?: "")
                editorLevel?.testingOnly != true
            }
        }
    }
    
    // Check if there are any user levels
    val hasUserLevels = remember(worldLevels) {
        worldLevels.any { worldLevel ->
            val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(worldLevel.level.editorLevelId ?: "")
            editorLevel?.isOfficial == false
        }
    }
    
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
    
    // Check for new repository data on first load and auto-sync (if enabled)
    LaunchedEffect(checkForNewRepositoryData) {
        if (checkForNewRepositoryData) {
            scope.launch {
                try {
                    val detectedData = RepositoryManager.detectNewRepositoryFiles()
                    if (detectedData != null) {
                        // Automatically sync official content without asking
                        if (LogConfig.ENABLE_UI_LOGGING) {
                        println("Detected new official content, auto-syncing...")
                        }
                        val success = RepositoryManager.syncNewRepositoryFiles()
                        if (success) {
                            println("Successfully synced new official content")
                            // Reload the world map to show the new levels
                            onReloadWorldMap?.invoke()
                        } else {
                            if (LogConfig.ENABLE_UI_LOGGING) {
                            println("Failed to sync official content")
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Silently ignore repository check errors to avoid disrupting the user experience
                    if (LogConfig.ENABLE_UI_LOGGING) {
                    println("Info: Repository check skipped - ${e.message}")
                    }
                }
            }
        }
    }
    
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val windowSize = remember(maxWidth, maxHeight) {
            "Window: ${maxWidth.value.toInt()} x ${maxHeight.value.toInt()} dp"
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
                // Level cards view - grid of level cards with tabs
                LevelCardsView(
                    worldLevels = visibleWorldLevels,
                    onLevelSelected = onLevelSelected,
                    showUserLevelsTab = isEditorAvailable(),  // Show tabs on desktop/wasm
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp, bottom = 80.dp)  // Leave space for top/bottom bars
                )
            } else {
                // Image Map View
                if (isEditorAvailable() && hasUserLevels && showUserLevelsTabView) {
                    // Show tab view with Official and User Levels tabs
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp, bottom = 80.dp)  // Leave space for top/bottom bars
                    ) {
                        // Tabs for Official (Image Map) and User Levels
                        androidx.compose.material3.PrimaryTabRow(selectedTabIndex = 1) {  // Always select User Levels tab (index 1)
                            androidx.compose.material3.Tab(
                                selected = false,
                                onClick = { 
                                    // Switch back to image map view with button
                                    showUserLevelsTabView = false 
                                },
                                text = { Text(stringResource(Res.string.official)) }
                            )
                            androidx.compose.material3.Tab(
                                selected = true,
                                onClick = { /* Already on User Levels tab */ },
                                text = { Text(stringResource(Res.string.user_levels)) }
                            )
                        }
                        
                        // Show user levels grid
                        LevelCardsView(
                            worldLevels = visibleWorldLevels,
                            onLevelSelected = onLevelSelected,
                            filterToUserLevelsOnly = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Show image-based World Map (no tabs)
                    ImageWorldMapView(
                        worldLevels = visibleWorldLevels,
                        onLocationClicked = { location, levelsAtLocation ->
                            selectedLocation = location to levelsAtLocation
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
                // Title/subtitle area and player info
                // In Image Map View: Stack player info below title
                // In Level Cards View: Show player info in same row with spacing
                if (useLevelCards) {
                    // Level Cards View: Title and player info in same row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title and subtitle - clickable for cheat code access
                        Column(
                            modifier = Modifier.then(
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
                        
                        // Player name and switch button (if available)
                        if (currentPlayerName != null && onSwitchPlayer != null && onEditPlayerName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentPlayerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onEditPlayerName() }
                                )
                                
                                TextButton(
                                    onClick = onSwitchPlayer,
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.switch_player),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Image Map View: Title and player info stacked
                    Column{
                        Column(
                            modifier = Modifier.then(
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
                        
                        // Player name and switch button (if available) - shown below title
                        if (currentPlayerName != null && onSwitchPlayer != null && onEditPlayerName != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentPlayerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onEditPlayerName() }
                                )
                                
                                TextButton(
                                    onClick = onSwitchPlayer,
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.switch_player),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
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
                
                // Spacer to push buttons to the right (only on desktop/wasm)
                if (!isPlatformMobile && isEditorAvailable()) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // User Levels Button (only in Image Map View when not showing tab view)
                // Show when: editor available, has user levels, NOT in level cards view, NOT showing tab view
                if (isEditorAvailable() && hasUserLevels && !useLevelCards && !showUserLevelsTabView) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Button(
                            onClick = { showUserLevelsTabView = true }
                        ) {
                            Text(stringResource(Res.string.user_levels))
                        }
                        
                        // Editor Button below User Levels button
                        EditorButtonCard(onClick = onOpenEditor)
                    }
                } else if (isEditorAvailable()) {
                    // Just Editor Button (when no user levels or already in tab view)
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
    
    // Platform info dialog (from platform cheat code)
    if (showPlatformInfo && onClearPlatformInfo != null) {
        de.egril.defender.ui.PlatformInfoDialog(
            platformInfo = de.egril.defender.utils.getPlatform().name,
            windowSize = windowSize,
            onDismiss = onClearPlatformInfo
        )
    }
    
    // Cheat code help screen (from cheat/cheats/help cheat code)
    if (showCheatHelp && onClearCheatHelp != null) {
        de.egril.defender.ui.CheatCodeHelpScreen(
            onDismiss = onClearCheatHelp,
            isInGameplay = false
        )
    }
    }
}
