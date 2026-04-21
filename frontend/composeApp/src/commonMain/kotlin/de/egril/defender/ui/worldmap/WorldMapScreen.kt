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
import androidx.compose.ui.input.key.*
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
import de.egril.defender.iam.IamState
import de.egril.defender.ui.icon.UnlockIcon

// Button sizing constants for world map bottom bar
private val BUTTON_WIDTH_MOBILE_IMAGE_MAP = 133.dp  // ~33% smaller than default for compact mobile layout
private val BUTTON_WIDTH_DEFAULT = 200.dp  // Standard button width for desktop and mobile level cards view

/**
 * Displays the local player name with an optional Keycloak username below it.
 * Extracted to avoid duplication between the card-list and image-map view sections.
 */
@Composable
private fun PlayerNameWithIam(
    currentPlayerName: String,
    iamState: IamState,
    onEditPlayerName: () -> Unit,
    onSwitchPlayer: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.clickable { onEditPlayerName() }
        ) {
            Text(
                text = currentPlayerName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            // Show Keycloak username below the local player name when logged in
            if (iamState.isAuthenticated && iamState.username != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    UnlockIcon(size = 12.dp)
                    Text(
                        text = iamState.username,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

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
    onDownloadCommunityContent: (() -> Unit)? = null,  // Callback to fetch community level metadata from backend
    remoteCommunityLevels: List<de.egril.defender.save.CommunityFileInfo> = emptyList(),  // Levels available on the server
    onDownloadCommunityLevel: ((de.egril.defender.save.CommunityFileInfo, (Boolean) -> Unit) -> Unit)? = null,  // On-demand level download
    checkForNewRepositoryData: Boolean = true,  // Set to false in tests to avoid repository checks
    onSwitchPlayer: (() -> Unit)? = null,  // Callback to switch player
    onEditPlayerName: (() -> Unit)? = null,  // Callback to edit player name
    currentPlayerName: String? = null,  // Current player name for display
    iamState: IamState = IamState(),  // IAM state for showing Keycloak username
    showPlatformInfo: Boolean = false,  // Show platform info from cheat code
    onClearPlatformInfo: (() -> Unit)? = null,  // Callback to clear platform info
    showCheatHelp: Boolean = false,  // Show cheat code help screen
    onClearCheatHelp: (() -> Unit)? = null  // Callback to clear cheat help
) {
    var showCheatDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<Pair<WorldMapLocation, List<WorldLevel>>?>(null) }

    // ID of the community level currently being downloaded on-demand (null if none)
    var downloadingLevelId by remember { mutableStateOf<String?>(null) }

    // Callback that starts on-demand download for a remote community level and clears
    // downloadingLevelId once the download finishes (success or failure).
    val handleDownloadRemoteLevel: (de.egril.defender.save.CommunityFileInfo) -> Unit = { fileInfo ->
        downloadingLevelId = fileInfo.fileId
        onDownloadCommunityLevel?.invoke(fileInfo) { _ ->
            downloadingLevelId = null
        }
    }

    // Fetch community content from backend when the world map is shown
    LaunchedEffect(Unit) {
        onDownloadCommunityContent?.invoke()
    }

    // Track which tab is active in image map mode.
    // null = show image map, 1 = Community tab, 2 = User Levels tab
    var imageMapActiveTab by remember { mutableStateOf<Int?>(null) }
    
    // Watch the setting for world map style
    val useLevelCards = AppSettings.useLevelCards.value
    
    // Watch the setting for showing testing levels
    val showTestingLevels = AppSettings.showTestingLevels.value
    
    // Filter world levels based on testingOnly flag
    val visibleWorldLevels = remember(worldLevels, showTestingLevels) {
        if (showTestingLevels) {
            worldLevels
        } else {
            // Filter out levels marked as testing only.
            // getLevel() covers official and user levels; getCommunityLevel() covers community levels
            // (getLevel does not search the community directory, so community levels must be checked
            // separately to correctly hide community levels that have testingOnly = true).
            worldLevels.filter { worldLevel ->
                val editorLevelId = worldLevel.level.editorLevelId ?: ""
                val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(editorLevelId)
                    ?: de.egril.defender.editor.EditorStorage.getCommunityLevel(editorLevelId)
                editorLevel?.testingOnly != true
            }
        }
    }
    
    // Check if there are any user levels (non-official, non-community)
    val hasUserLevels = remember(worldLevels) {
        worldLevels.any { worldLevel ->
            val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(worldLevel.level.editorLevelId ?: "")
            editorLevel?.isOfficial == false && editorLevel.isCommunity == false
        }
    }

    // Check if there are any community levels (local or remote)
    val hasCommunityLevels = remember(worldLevels, remoteCommunityLevels) {
        remoteCommunityLevels.isNotEmpty() ||
        worldLevels.any { worldLevel ->
            val editorLevel = de.egril.defender.editor.EditorStorage.getCommunityLevel(worldLevel.level.editorLevelId ?: "")
            editorLevel?.isCommunity == true
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
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        event.key == Key.Back || event.key == Key.Escape -> {
                            onBackToMenu()
                            true
                        }
                        event.key == Key.C && !event.isCtrlPressed && onCheatCode != null -> {
                            showCheatDialog = true
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
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
                    remoteCommunityLevels = remoteCommunityLevels,
                    downloadingLevelId = downloadingLevelId,
                    onDownloadRemoteLevel = handleDownloadRemoteLevel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp, bottom = 80.dp)  // Leave space for top/bottom bars
                )
            } else {
                // Image Map View
                if ((hasUserLevels || hasCommunityLevels) && imageMapActiveTab != null) {
                    // Show tab view – Official tab returns to image map, Community/User tabs show level cards
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp, bottom = 80.dp)
                    ) {
                        val tabIndex = imageMapActiveTab ?: return@Column
                        androidx.compose.material3.PrimaryTabRow(selectedTabIndex = tabIndex) {
                            // Tab 0: Official – click to return to image worldmap
                            androidx.compose.material3.Tab(
                                selected = false,
                                onClick = { imageMapActiveTab = null },
                                text = { Text(stringResource(Res.string.official)) }
                            )
                            // Tab 1: Community (always rendered so indices stay stable)
                            androidx.compose.material3.Tab(
                                selected = tabIndex == 1,
                                onClick = { imageMapActiveTab = 1 },
                                text = { Text(stringResource(Res.string.community_levels)) }
                            )
                            // Tab 2: User Levels
                            androidx.compose.material3.Tab(
                                selected = tabIndex == 2,
                                onClick = { imageMapActiveTab = 2 },
                                text = { Text(stringResource(Res.string.user_levels)) }
                            )
                        }
                        // Show the appropriate level cards for the selected tab
                        when (tabIndex) {
                            1 -> LevelCardsView(
                                worldLevels = visibleWorldLevels,
                                onLevelSelected = onLevelSelected,
                                filterToCommunityOnly = true,
                                remoteCommunityLevels = remoteCommunityLevels,
                                downloadingLevelId = downloadingLevelId,
                                onDownloadRemoteLevel = handleDownloadRemoteLevel,
                                modifier = Modifier.fillMaxSize()
                            )
                            2 -> LevelCardsView(
                                worldLevels = visibleWorldLevels,
                                onLevelSelected = onLevelSelected,
                                filterToUserLevelsOnly = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
                if (useLevelCards || imageMapActiveTab != null) {
                    // Level Cards View or tab view: Title and player info in same row
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
                            PlayerNameWithIam(
                                currentPlayerName = currentPlayerName,
                                iamState = iamState,
                                onEditPlayerName = onEditPlayerName,
                                onSwitchPlayer = onSwitchPlayer
                            )
                        }
                    }
                } else {
                    // Image Map View (no tab overlay): Title and player info stacked
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
                            PlayerNameWithIam(
                                currentPlayerName = currentPlayerName,
                                iamState = iamState,
                                onEditPlayerName = onEditPlayerName,
                                onSwitchPlayer = onSwitchPlayer
                            )
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
                // - Mobile + Image Map + Tab Active: No buttons (community/user tabs shown)
                // - Mobile + Image Map: Column layout, left-aligned, smaller buttons
                // - Mobile + Level Cards: Row layout, centered, normal buttons
                // - Desktop: Row layout, centered
                when {
                    isMobileImageMap && imageMapActiveTab != null -> {
                        // Tab view is active on mobile - do not show worldmap action buttons
                    }
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
                
                // Spacer to push community/user/editor buttons to the right
                if ((!isPlatformMobile && isEditorAvailable()) ||
                    (isMobileImageMap && (hasUserLevels || hasCommunityLevels) && imageMapActiveTab == null)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // User/Community Levels Buttons (only in Image Map View when not showing tab view)
                if ((hasUserLevels || hasCommunityLevels) && !useLevelCards && imageMapActiveTab == null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (hasUserLevels) {
                            Button(onClick = { imageMapActiveTab = 2 }) {
                                Text(stringResource(Res.string.user_levels))
                            }
                        }
                        if (hasCommunityLevels) {
                            Button(onClick = { imageMapActiveTab = 1 }) {
                                Text(stringResource(Res.string.community_levels))
                            }
                        }
                        // Editor Button below the level buttons
                        if (isEditorAvailable()) {
                            EditorButtonCard(onClick = onOpenEditor)
                        }
                    }
                } else if (isEditorAvailable()) {
                    // Just Editor Button (when no user/community levels or already in tab view)
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
