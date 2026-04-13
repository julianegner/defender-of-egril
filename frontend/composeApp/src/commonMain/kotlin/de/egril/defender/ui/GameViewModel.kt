package de.egril.defender.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.egril.defender.game.GameEngine
import de.egril.defender.game.GameEngine.EnemyTurnMovements
import de.egril.defender.game.LevelData
import de.egril.defender.model.*
import de.egril.defender.model.DifficultyModifiers
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage
import de.egril.defender.utils.CheatCodeHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import de.egril.defender.config.LogConfig
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.editor.EditorJsonSerializer
import de.egril.defender.ui.infopage.NewVersionInfo
import de.egril.defender.ui.infopage.checkForNewerVersion

sealed class Screen {
    object MainMenu : Screen()
    object WorldMap : Screen()
    object Rules : Screen()
    object InstallationInfo : Screen()
    data class InstallationInfoAtTab(val initialTab: de.egril.defender.ui.infopage.InfoTab) : Screen()
    object LevelEditor : Screen()
    object LoadGame : Screen()
    object Sticker : Screen()
    object PlayerProfile : Screen()
    object LoadingSpinnerDemo : Screen()
    object StatsUpgrade : Screen()  // New screen for stats/spells upgrade
    object FinalCredits : Screen()
    object AnimationTest : Screen()  // Developer cheat: animation test/preview screen
    data class GamePlay(val levelId: Int) : Screen()
    data class LevelComplete(
        val levelId: Int,
        val won: Boolean,
        val isLastLevel: Boolean,
        val xpEarned: Int = 0
    ) : Screen()
}

/**
 * Represents a conflict between saved world map progress and current world map progress
 */
data class WorldMapConflict(
    val savedGame: de.egril.defender.save.SavedGame?,  // Null when importing just game state
    val savedWorldMap: de.egril.defender.save.WorldMapSave,
    val currentWorldMap: Map<String, LevelStatus>,
    val level: Level?  // Null when importing just game state (no level to load)
)

/**
 * Data class for reminder messages
 */
data class ReminderMessage(
    val type: de.egril.defender.ui.gameplay.ReminderType,
    val elapsedTime: String? = null,
    val timeDescription: String? = null
)

class GameViewModel {
    
    private val _currentScreen = MutableStateFlow<Screen>(Screen.MainMenu)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    
    private val _worldLevels = MutableStateFlow<List<WorldLevel>>(emptyList())
    val worldLevels: StateFlow<List<WorldLevel>> = _worldLevels.asStateFlow()
    
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()
    
    private val _cheatDigOutcome = MutableStateFlow<DigOutcome?>(null)
    val cheatDigOutcome: StateFlow<DigOutcome?> = _cheatDigOutcome.asStateFlow()
    
    private val _showPlatformInfo = MutableStateFlow(false)
    val showPlatformInfo: StateFlow<Boolean> = _showPlatformInfo.asStateFlow()
    
    private val _showCheatHelp = MutableStateFlow(false)
    val showCheatHelp: StateFlow<Boolean> = _showCheatHelp.asStateFlow()

    // Player profile state
    private val _currentPlayer = MutableStateFlow<de.egril.defender.save.PlayerProfile?>(null)
    val currentPlayer: StateFlow<de.egril.defender.save.PlayerProfile?> = _currentPlayer.asStateFlow()
    
    private val _allPlayers = MutableStateFlow<List<de.egril.defender.save.PlayerProfile>>(emptyList())
    val allPlayers: StateFlow<List<de.egril.defender.save.PlayerProfile>> = _allPlayers.asStateFlow()
    
    private val _needsPlayerSelection = MutableStateFlow(false)
    val needsPlayerSelection: StateFlow<Boolean> = _needsPlayerSelection.asStateFlow()
    
    // World map progress conflict state
    private val _worldMapConflict = MutableStateFlow<WorldMapConflict?>(null)
    val worldMapConflict: StateFlow<WorldMapConflict?> = _worldMapConflict.asStateFlow()
    
    // Special actions remaining after auto-attack
    private val _specialActionsRemaining = MutableStateFlow<List<DefenderType>>(emptyList())
    val specialActionsRemaining: StateFlow<List<DefenderType>> = _specialActionsRemaining.asStateFlow()

    // Time reminders for breaks and sleep
    private val _reminderMessage = MutableStateFlow<ReminderMessage?>(null)
    val reminderMessage: StateFlow<ReminderMessage?> = _reminderMessage.asStateFlow()

    // In-game event messages (target taken, gate destroyed)
    private val _pendingGameMessage = MutableStateFlow<de.egril.defender.model.GameMessage?>(null)
    val pendingGameMessage: StateFlow<de.egril.defender.model.GameMessage?> = _pendingGameMessage.asStateFlow()

    // Achievement system
    private var achievementManager: de.egril.defender.game.AchievementManager? = null
    private val _newAchievement = MutableStateFlow<Achievement?>(null)
    val newAchievement: StateFlow<Achievement?> = _newAchievement.asStateFlow()
    
    // Magic panel state
    private val _showMagicPanel = MutableStateFlow(false)
    val showMagicPanel: StateFlow<Boolean> = _showMagicPanel.asStateFlow()

    private val _selectedSpell = MutableStateFlow<SpellType?>(null)
    val selectedSpell: StateFlow<SpellType?> = _selectedSpell.asStateFlow()

    private val _pendingSpellCast = MutableStateFlow<SpellType?>(null)
    val pendingSpellCast: StateFlow<SpellType?> = _pendingSpellCast.asStateFlow()

    // Position to scroll to (e.g., bomb explosion)
    private val _pendingScrollToPosition = MutableStateFlow<Position?>(null)
    val pendingScrollToPosition: StateFlow<Position?> = _pendingScrollToPosition.asStateFlow()

    // Post-target confirmation dialog state
    private val _showSpellTargetConfirmation = MutableStateFlow<Pair<SpellType, Any>?>(null)
    val showSpellTargetConfirmation: StateFlow<Pair<SpellType, Any>?> = _showSpellTargetConfirmation.asStateFlow()

    private val _showFreezeImmuneWarning = MutableStateFlow<Attacker?>(null)
    val showFreezeImmuneWarning: StateFlow<Attacker?> = _showFreezeImmuneWarning.asStateFlow()

    // Track game session time
    private var gameSessionStartTime: Long? = null
    private var lastBreakReminderTime: Long? = null
    private var lastSleepReminderTime: Long? = null
    
    // Constants for reminder intervals
    private val BREAK_REMINDER_INTERVAL_MS = 2 * 60 * 60 * 1000L  // 2 hours
    private val SLEEP_REMINDER_INTERVAL_MS = 60 * 60 * 1000L       // 1 hour
    private val SLEEP_START_HOUR = 23  // 23:00 (11 PM)

    // Extra pause added after a movement step that triggers a trap, so the trap animation (~830ms)
    // completes before the enemy continues moving.
    private val TRAP_ANIMATION_EXTRA_DELAY_MS = 800L

    // Pause after a trap kill's death animation starts, so the enemy death animation (~1000ms)
    // finishes before the next movement step begins.
    private val ENEMY_DEATH_ANIMATION_DELAY_MS = 1000L

    // Track initial game state to detect unsaved changes
    private var initialGameStateSnapshot: String? = null
    private var lastSaveSnapshot: String? = null

    private var gameEngine: GameEngine? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    // Demo mode state
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()
    private var demoLevelIndex = 0
    private var demoJob: Job? = null

    // Demo visual state – drives placement preview and attack aiming circles in the UI
    private val _demoSelectedDefenderType = MutableStateFlow<DefenderType?>(null)
    val demoSelectedDefenderType: StateFlow<DefenderType?> = _demoSelectedDefenderType.asStateFlow()
    private val _demoHoveredPosition = MutableStateFlow<Position?>(null)
    val demoHoveredPosition: StateFlow<Position?> = _demoHoveredPosition.asStateFlow()
    private val _demoSelectedDefenderId = MutableStateFlow<Int?>(null)
    val demoSelectedDefenderId: StateFlow<Int?> = _demoSelectedDefenderId.asStateFlow()
    private val _demoSelectedTargetPosition = MutableStateFlow<Position?>(null)
    val demoSelectedTargetPosition: StateFlow<Position?> = _demoSelectedTargetPosition.asStateFlow()

    // New version availability check
    private val _newVersionAvailable = MutableStateFlow<NewVersionInfo?>(null)
    val newVersionAvailable: StateFlow<NewVersionInfo?> = _newVersionAvailable.asStateFlow()

    // Remote community level metadata (levels available on the server, may or may not be downloaded locally)
    private val _remoteCommunityLevelsMeta = MutableStateFlow<List<de.egril.defender.save.CommunityFileInfo>>(emptyList())
    val remoteCommunityLevelsMeta: StateFlow<List<de.egril.defender.save.CommunityFileInfo>> = _remoteCommunityLevelsMeta.asStateFlow()

    // Remote community map metadata (maps available on the server, may or may not be downloaded locally)
    private val _remoteCommunityMapsMeta = MutableStateFlow<List<de.egril.defender.save.CommunityFileInfo>>(emptyList())
    val remoteCommunityMapsMeta: StateFlow<List<de.egril.defender.save.CommunityFileInfo>> = _remoteCommunityMapsMeta.asStateFlow()

    init {
        // Ensure EditorStorage is initialized with repository data
        de.egril.defender.editor.EditorStorage.ensureInitialized()
        
        de.egril.defender.analytics.reportEvent(de.egril.defender.analytics.GameEventType.APP_STARTED, null)

        initializePlayerProfile()
        initializeWorldMap()
        // Note: saved games are loaded via onAuthStateChanged() which is called by a
        // LaunchedEffect in App.kt on every change of iamState.isAuthenticated (including
        // the initial composition).  This avoids an NPE that would occur if
        // refreshSavedGames() were called here directly, because _savedGames is declared
        // further down in the file and not yet initialised at this point in the constructor.

        // Upload settings to the dedicated backend table whenever a setting is changed
        // and the user is authenticated.
        de.egril.defender.ui.settings.AppSettings.onPersist = { uploadSettingsToBackend() }
    }
    
    /**
     * Initialize player profile system
     * - Checks for existing players
     * - Migrates old saves if needed
     * - Sets up the current player
     */
    private fun initializePlayerProfile() {
        // Check for existing player profiles
        val profiles = de.egril.defender.save.PlayerProfileStorage.getAllProfiles()
        _allPlayers.value = profiles.profiles
        
        if (profiles.profiles.isEmpty()) {
            // No profiles exist - check if we need to migrate old saves
            val migratedProfile = de.egril.defender.save.PlayerProfileStorage.migrateExistingSaves()
            if (migratedProfile != null) {
                // Migration successful, use the migrated profile
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Migrated existing saves to player profile: ${migratedProfile.name}")
                }
                _currentPlayer.value = migratedProfile
                de.egril.defender.save.SaveFileStorage.setCurrentPlayer(migratedProfile.id)
                _allPlayers.value = listOf(migratedProfile)
            } else {
                // No existing saves, need to create first player
                _needsPlayerSelection.value = true
            }
        } else {
            // Load the last used player or the most recently played one
            val lastPlayerId = profiles.lastUsedPlayerId
            val playerToUse = if (lastPlayerId != null) {
                profiles.profiles.find { it.id == lastPlayerId }
            } else {
                profiles.profiles.maxByOrNull { it.lastPlayedAt }
            }
            
            if (playerToUse != null) {
                _currentPlayer.value = playerToUse
                de.egril.defender.save.SaveFileStorage.setCurrentPlayer(playerToUse.id)
                de.egril.defender.save.PlayerProfileStorage.updateLastPlayed(playerToUse.id)

                // Reload the player profile after updateLastPlayed to ensure we have the latest data
                val reloadedProfile = de.egril.defender.save.PlayerProfileStorage.getProfile(playerToUse.id)
                if (reloadedProfile != null) {
                    _currentPlayer.value = reloadedProfile
                }
            } else {
                // Shouldn't happen, but handle gracefully
                _needsPlayerSelection.value = true
            }
        }
    }
    
    private fun initializeWorldMap() {
        // Load official levels
        val officialLevels = LevelData.createLevels()
        if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
        println("DEBUG: Total official levels loaded: ${officialLevels.size}")
        }

        // Load user levels from user sequence
        val userSequence = de.egril.defender.editor.EditorStorage.getUserLevelSequence()
        val userLevels = userSequence.sequence.mapNotNull { levelId ->
            val editorLevel = de.egril.defender.editor.EditorStorage.reloadLevel(levelId)
            if (editorLevel != null && !editorLevel.isOfficial) {
                // Check if level is ready to play
                if (de.egril.defender.editor.EditorStorage.isLevelReadyToPlay(editorLevel)) {
                    // Convert editor level to game level
                    de.egril.defender.editor.EditorStorage.convertToGameLevel(
                        editorLevel, 
                        officialLevels.size + userSequence.sequence.indexOf(levelId) + 1
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
        if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
        println("DEBUG: Total user levels loaded: ${userLevels.size}")
        }

        // Load community levels from community directory (skip any already present as user levels)
        val userLevelIds = userSequence.sequence.toSet()
        // Pre-populate community maps cache from disk before processing community levels.
        // This ensures isLevelReadyToPlay → getMap() finds community maps in the cache rather than
        // relying on a lazy per-map disk read that can be disrupted by a concurrent clearCommunityCache()
        // call from downloadCommunityContent running on Dispatchers.Default's thread pool.
        val communityMaps = de.egril.defender.editor.EditorStorage.getAllCommunityMaps()
        val communityEditorLevels = de.egril.defender.editor.EditorStorage.getAllCommunityLevels()
            // .filter { it.id !in userLevelIds }  // Avoid duplicating levels the user also has locally
        if (LogConfig.isEnabled { LogConfig.ENABLE_COMMUNITY_DEBUG_LOGGING }) {
            println("COMMUNITY-DEBUG: Found ${communityMaps.size} community maps on disk: ${communityMaps.map { it.id }}")
            println("COMMUNITY-DEBUG: Found ${communityEditorLevels.size} community levels on disk: ${communityEditorLevels.map { "${it.id} (mapId=${it.mapId})" }}")
        }
        val communityLevels = communityEditorLevels.mapIndexedNotNull { index, editorLevel ->
            val map = de.egril.defender.editor.EditorStorage.getMap(editorLevel.mapId)
            val levelReady = editorLevel.isReadyToPlay()
            val mapReady = map?.validateReadyToUse(includeRiversAsWalkable = true) ?: false
            val targets = map?.getTargets() ?: emptyList()
            val spawnPoints = map?.getSpawnPoints() ?: emptyList()
            val waypointResult = if (targets.isNotEmpty()) {
                editorLevel.validateWaypointsDetailed(targetPositions = targets, spawnPoints = spawnPoints)
            } else null
            val waypointsValid = waypointResult?.isValid ?: false
            val isReady = de.egril.defender.editor.EditorStorage.isLevelReadyToPlay(editorLevel)
            if (LogConfig.isEnabled { LogConfig.ENABLE_COMMUNITY_DEBUG_LOGGING }) {
                println("COMMUNITY-DEBUG: Level '${editorLevel.id}': mapId=${editorLevel.mapId}, mapFound=${map != null}, " +
                    "tiles=${map?.tiles?.size ?: 0}, levelReady=$levelReady, mapReady=$mapReady, " +
                    "targets=${targets.size}, spawns=${spawnPoints.size}, waypointsValid=$waypointsValid, " +
                    "isReadyToPlay=$isReady")
            }
            if (isReady) {
                de.egril.defender.editor.EditorStorage.convertToGameLevel(
                    editorLevel,
                    officialLevels.size + userLevels.size + index + 1
                )
            } else {
                null
            }
        }
        if (LogConfig.isEnabled { LogConfig.ENABLE_COMMUNITY_DEBUG_LOGGING }) {
            println("COMMUNITY-DEBUG: Total community levels loaded into worldLevels: ${communityLevels.size}")
        }

        // Combine official, user, and community levels
        val allLevels = officialLevels + userLevels + communityLevels
        
        // Load saved world map status
        val savedStatuses = de.egril.defender.save.SaveFileStorage.loadWorldMapStatus()
        
        // Get the set of won level IDs
        val wonLevelIds = savedStatuses?.filter { it.value == LevelStatus.WON }?.keys?.toSet() ?: emptySet()
        
        _worldLevels.value = allLevels.mapIndexed { index, level ->
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("DEBUG: Loaded Level ${level.id} - Name: ${level.name} - Path Cells: ${level.pathCells.size}")
            }
            // Look up status by editorLevelId if available
            val status = if (level.editorLevelId != null) {
                // Check saved status first
                val savedStatus = savedStatuses?.get(level.editorLevelId)
                if (savedStatus != null) {
                    savedStatus
                } else {
                    // User and community levels are always unlocked
                    val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(level.editorLevelId)
                        ?: de.egril.defender.editor.EditorStorage.getCommunityLevel(level.editorLevelId)
                    if (editorLevel?.isOfficial == false) {
                        LevelStatus.UNLOCKED
                    } else {
                        // Check if official level should be unlocked based on prerequisites
                        if (de.egril.defender.editor.EditorStorage.isLevelUnlocked(level.editorLevelId, wonLevelIds)) {
                            LevelStatus.UNLOCKED
                        } else {
                            LevelStatus.LOCKED
                        }
                    }
                }
            } else {
                // Fallback for legacy levels or levels created without editor (shouldn't happen in normal gameplay)
                if (LogConfig.ENABLE_UI_LOGGING) {
                println("WARNING: Level ${level.id} (${level.name}) has no editorLevelId - using fallback status")
                }
                if (index == 0) LevelStatus.UNLOCKED else LevelStatus.LOCKED
            }
            
            WorldLevel(
                level = level,
                status = status
            )
        }
    }
    
    fun reloadWorldMap() {
        // Reload levels from disk to get latest changes
        if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
        println("Reloading world map from disk...")
        }
        initializeWorldMap()
    }

    /**
     * Fetches the list of community levels and maps available on the server and stores their metadata.
     * Does NOT download the actual level or map files – those are downloaded on demand.
     * Locally-already-downloaded community content is reloaded from disk so the world map reflects it immediately.
     */
    fun downloadCommunityContent() {
        viewModelScope.launch {
            try {
                val communityLevelMeta = de.egril.defender.save.BackendCommunityService
                    .fetchCommunityFileList("LEVEL")
                if (communityLevelMeta != null) {
                    _remoteCommunityLevelsMeta.value = communityLevelMeta
                }
                val communityMapMeta = de.egril.defender.save.BackendCommunityService
                    .fetchCommunityFileList("MAP")
                if (communityMapMeta != null) {
                    _remoteCommunityMapsMeta.value = communityMapMeta
                }
            } catch (e: Exception) {
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Failed to fetch community content metadata: ${e.message}")
                }
            } finally {
                // Always reload locally-downloaded community levels so they appear in the world map,
                // even when the network is unavailable or the metadata fetch fails.
                de.egril.defender.editor.EditorStorage.clearCommunityCache()
                initializeWorldMap()
            }
        }
    }

    /**
     * Downloads a single community level (and its map if needed) on demand and then
     * reloads the world map via [initializeWorldMap] so the level becomes playable immediately.
     *
     * @param fileInfo Metadata of the community level to download
     * @param onComplete Called when the operation finishes; receives true on success, false on failure.
     *   Note: on success, [initializeWorldMap] has already been called and [worldLevels] will be
     *   updated before [onComplete] is invoked.
     */
    fun downloadCommunityLevelOnDemand(
        fileInfo: de.egril.defender.save.CommunityFileInfo,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val fileData = de.egril.defender.save.BackendCommunityService
                    .fetchCommunityFile("LEVEL", fileInfo.fileId)
                if (fileData == null) {
                    onComplete(false)
                    return@launch
                }
                val level = de.egril.defender.editor.EditorJsonSerializer.deserializeLevel(fileData.data)
                if (level == null) {
                    onComplete(false)
                    return@launch
                }
                de.egril.defender.editor.EditorStorage.saveCommunityLevel(
                    level.copy(isCommunity = true, communityAuthorUsername = fileInfo.authorUsername)
                )
                // Also download the map used by this level if not already present locally.
                val mapNeededDownload = de.egril.defender.editor.EditorStorage.getMap(level.mapId) == null
                if (mapNeededDownload) {
                    downloadCommunityMap(level.mapId)
                }
                // Remove this level from the remote-only list now that it has been downloaded
                // locally, so that the level cards view immediately shows it as a local level
                // instead of keeping it in the "download" state.
                _remoteCommunityLevelsMeta.value = _remoteCommunityLevelsMeta.value
                    .filter { it.fileId != fileInfo.fileId }
                // If the community map was also freshly downloaded, remove it from the remote
                // maps list so the map editor list also reflects the local copy.
                if (mapNeededDownload) {
                    _remoteCommunityMapsMeta.value = _remoteCommunityMapsMeta.value
                        .filter { it.fileId != level.mapId }
                }
                initializeWorldMap()
                onComplete(true)
            } catch (e: Exception) {
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Failed to download community level ${fileInfo.fileId}: ${e.message}")
                }
                // If the download failed but the level (and its map) are already stored locally,
                // still reload the world map so the level is surfaced as playable rather than
                // staying in the "remote" card state.
                val alreadyLocal = de.egril.defender.editor.EditorStorage.getCommunityLevel(fileInfo.fileId) != null
                if (alreadyLocal) {
                    _remoteCommunityLevelsMeta.value = _remoteCommunityLevelsMeta.value
                        .filter { it.fileId != fileInfo.fileId }
                    initializeWorldMap()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }
        }
    }

    /** Downloads a single community map (JSON + server-generated image) and stores it locally. */
    private suspend fun downloadCommunityMap(mapId: String) {
        try {
            val mapData = de.egril.defender.save.BackendCommunityService
                .fetchCommunityFile("MAP", mapId)
            if (mapData != null) {
                val map = de.egril.defender.editor.EditorJsonSerializer
                    .deserializeMap(mapData.data)
                if (map != null) {
                    // Pass the requested mapId so saveCommunityMap can also store the map under
                    // that ID when the map's internal JSON id differs (edge case guard).
                    if (map.id != mapId) {
                        if (LogConfig.isEnabled { LogConfig.ENABLE_COMMUNITY_DEBUG_LOGGING }) {
                            println("COMMUNITY-DEBUG: Map id mismatch for download: requested='$mapId', json id='${map.id}'. Saving under both IDs.")
                        }
                    }
                    de.egril.defender.editor.EditorStorage.saveCommunityMap(
                        map,
                        mapData.authorUsername,
                        requestedId = mapId
                    )
                    // Try to fetch the server-generated image and overwrite the locally-generated one
                    val imageBytes = de.egril.defender.save.BackendCommunityService
                        .fetchCommunityMapImage(mapId)
                    if (imageBytes != null) {
                        de.egril.defender.editor.getFileStorage().writeBinaryFile(
                            "gamedata/community/maps/$mapId.png",
                            imageBytes
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to download community map $mapId: ${e.message}")
            }
        }
    }

    /**
     * Downloads a single community map (JSON + server-generated image) on demand from the map editor.
     * Clears the community cache after saving so the editor list refreshes.
     *
     * @param fileInfo Metadata of the community map to download
     * @param onComplete Called when the operation finishes; receives true on success, false on failure
     */
    fun downloadCommunityMapOnDemand(
        fileInfo: de.egril.defender.save.CommunityFileInfo,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                downloadCommunityMap(fileInfo.fileId)
                de.egril.defender.editor.EditorStorage.clearCommunityCache()
                onComplete(true)
            } catch (e: Exception) {
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Failed to download community map on demand ${fileInfo.fileId}: ${e.message}")
                }
                onComplete(false)
            }
        }
    }

    /**
     * Uploads a user level to the community backend.
     * If the level's map is a user map that has not yet been uploaded to the community,
     * the map is uploaded first so that other players can download it together with the level.
     * @param levelId The ID of the level to upload
     * @param token Bearer token for authentication
     * @return true on success, false on failure
     */
    suspend fun uploadCommunityLevel(levelId: String, token: String): Boolean {
        val level = de.egril.defender.editor.EditorStorage.getLevel(levelId) ?: return false
        val username = de.egril.defender.iam.IamService.state.value.username ?: ""

        // Auto-upload the associated map if it is a user map not yet in the community
        val map = de.egril.defender.editor.EditorStorage.getMap(level.mapId)
        if (map != null && !map.isOfficial && !map.isCommunity) {
            val communityMap = de.egril.defender.editor.EditorStorage.getCommunityMap(level.mapId)
            if (communityMap == null) {
                // Map not yet uploaded – upload it now
                val mapJson = de.egril.defender.editor.EditorJsonSerializer.serializeMap(map)
                val mapSuccess = de.egril.defender.save.BackendCommunityService
                    .uploadCommunityFile("MAP", level.mapId, mapJson, token)
                if (mapSuccess) {
                    de.egril.defender.editor.EditorStorage.saveCommunityMap(map, username)
                }
                // Proceed with the level upload even if the map upload failed; callers
                // can inspect the level-upload result and show an appropriate message.
            }
        }

        val json = de.egril.defender.editor.EditorJsonSerializer.serializeLevel(level)
        val success = de.egril.defender.save.BackendCommunityService
            .uploadCommunityFile("LEVEL", levelId, json, token, level.communityDescription)
        if (success) {
            // Store the uploaded version locally in the community directory so we can detect changes
            de.egril.defender.editor.EditorStorage.saveCommunityLevel(
                level.copy(
                    isCommunity = true,
                    communityAuthorUsername = username
                )
            )
        }
        return success
    }

    /**
     * Uploads a user map to the community backend.
     * @param mapId The ID of the map to upload
     * @param token Bearer token for authentication
     * @return true on success, false on failure
     */
    suspend fun uploadCommunityMap(mapId: String, token: String): Boolean {
        val map = de.egril.defender.editor.EditorStorage.getMap(mapId) ?: return false
        val json = de.egril.defender.editor.EditorJsonSerializer.serializeMap(map)
        val success = de.egril.defender.save.BackendCommunityService
            .uploadCommunityFile("MAP", mapId, json, token)
        if (success) {
            val username = de.egril.defender.iam.IamService.state.value.username ?: ""
            de.egril.defender.editor.EditorStorage.saveCommunityMap(map, username)
        }
        return success
    }
    
    fun navigateToMainMenu() {
        stopTimeTracking()
        _currentScreen.value = Screen.MainMenu
    }
    
    fun navigateToWorldMap() {
        if (_currentScreen.value is Screen.GamePlay) {
            val levelName = _gameState.value?.level?.name ?: "unknown"
            val turnNumber = _gameState.value?.turnNumber?.value
            de.egril.defender.analytics.reportEvent(de.egril.defender.analytics.GameEventType.LEVEL_LEFT, levelName, turnNumber)
        }
        stopTimeTracking()
        // Reload levels from disk to ensure latest changes are visible
        reloadWorldMap()
        _currentScreen.value = Screen.WorldMap
    }
    
    fun navigateToRules() {
        _currentScreen.value = Screen.Rules
    }
    
    fun navigateToInstallationInfo() {
        _currentScreen.value = Screen.InstallationInfo
    }

    fun navigateToBackendInfo() {
        _currentScreen.value = Screen.InstallationInfoAtTab(de.egril.defender.ui.infopage.InfoTab.BACKEND)
    }

    fun navigateToDownloadInfo() {
        _currentScreen.value = Screen.InstallationInfoAtTab(de.egril.defender.ui.infopage.InfoTab.DOWNLOAD)
    }
    
    fun navigateToLevelEditor() {
        _currentScreen.value = Screen.LevelEditor
    }
    
    fun navigateToSticker() {
        _currentScreen.value = Screen.Sticker
    }
    
    fun navigateToLoadingSpinnerDemo() {
        _currentScreen.value = Screen.LoadingSpinnerDemo
    }

    fun navigateToPlayerProfile() {
        _currentScreen.value = Screen.PlayerProfile
    }
    
    fun navigateToStatsUpgrade() {
        _currentScreen.value = Screen.StatsUpgrade
    }

    fun navigateToFinalCredits() {
        _currentScreen.value = Screen.FinalCredits
    }

    fun navigateToAnimationTest() {
        _currentScreen.value = Screen.AnimationTest
    }

    fun upgradeAbility(statType: de.egril.defender.model.AbilityType) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities
        val updatedStats = oldStats.spendAbilityPoint(statType) ?: return
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)

        // Check for achievements
        val playerId = currentPlayer.id
        val tempAchievementManager = de.egril.defender.game.AchievementManager(playerId)
        tempAchievementManager.onAchievementEarned = { achievement ->
            _newAchievement.value = achievement
        }

        // First stat upgrade achievement
        val totalSpentBefore = oldStats.healthAbility + oldStats.treasuryAbility + oldStats.incomeAbility +
                               oldStats.constructionAbility + oldStats.manaAbility
        if (totalSpentBefore == 0) {
            tempAchievementManager.onFirstStatUpgrade()
        }

        // Construction level 3 achievement
        if (statType == de.egril.defender.model.AbilityType.CONSTRUCTION && updatedStats.constructionAbility >= 3) {
            tempAchievementManager.onConstructionLevel3()
        }

        // Player level achievements
        val playerLevel = de.egril.defender.model.PlayerAbilities.calculateLevel(updatedStats.totalXP)
        if (playerLevel >= 10) {
            tempAchievementManager.onPlayerLevel10()
        }
        if (playerLevel >= 100) {
            tempAchievementManager.onPlayerLevel100()
        }
        // Sync updated abilities to backend
        uploadUserDataToBackend()
    }

    fun unlockSpell(spell: de.egril.defender.model.SpellType) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities
        val updatedStats = oldStats.unlockSpell(spell) ?: return
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)

        // Check for first spell unlock achievement
        val playerId = currentPlayer.id
        val tempAchievementManager = de.egril.defender.game.AchievementManager(playerId)
        tempAchievementManager.onAchievementEarned = { achievement ->
            _newAchievement.value = achievement
        }

        if (oldStats.unlockedSpells.isEmpty()) {
            tempAchievementManager.onFirstSpellUnlock()
        }
        // Sync updated spell list to backend
        uploadUserDataToBackend()
    }

    fun startLevel(levelId: Int) {
        // Clear any pending message from a previous level
        _pendingGameMessage.value = null
        val worldLevel = _worldLevels.value.find { it.level.id == levelId }
        if (worldLevel != null && worldLevel.status != LevelStatus.LOCKED) {
            val difficulty = AppSettings.difficulty.value
            val level = worldLevel.level
            val playerStats = _currentPlayer.value?.abilities ?: PlayerAbilities()

            // Apply difficulty modifiers to spawn plan
            val modifiedSpawnPlan = if (level.directSpawnPlan != null) {
                DifficultyModifiers.applySpawnPlanModifier(level.directSpawnPlan, difficulty)
            } else {
                val basePlan = generateSpawnPlan(level.attackerWaves)
                DifficultyModifiers.applySpawnPlanModifier(basePlan, difficulty)
            }
            
            // Apply player stats bonuses
            val baseCoins = DifficultyModifiers.applyCoinsModifier(level.initialCoins, difficulty)
            val bonusCoins = playerStats.getBonusStartCoins()
            val totalCoins = baseCoins + bonusCoins

            val baseHealth = DifficultyModifiers.applyHealthPointsModifier(level.healthPoints, difficulty)
            val bonusHealth = playerStats.getBonusHealth()
            val totalHealth = baseHealth + bonusHealth

            val maxMana = playerStats.getMaxMana()
            val incomeMultiplier = playerStats.getIncomeMultiplier()
            val constructionLevel = playerStats.constructionAbility

            // Create GameState with difficulty-modified and stats-bonus values
            val newGameState = GameState(
                level = level,
                difficulty = difficulty,
                coins = mutableStateOf(totalCoins),
                healthPoints = mutableStateOf(totalHealth),
                spawnPlan = modifiedSpawnPlan,
                maxMana = mutableStateOf(maxMana),
                currentMana = mutableStateOf(maxMana),  // Start with full mana
                incomeMultiplier = incomeMultiplier,
                constructionLevel = constructionLevel
            )
            
            // Initialize pre-placed elements if any
            newGameState.initializePrePlacedElements()
            
            _gameState.value = newGameState
            gameEngine = GameEngine(newGameState)
            _currentScreen.value = Screen.GamePlay(levelId)

            // Show story intro message if this level has one (all levels except the tutorial)
            val editorLevelId = level.editorLevelId
            if (editorLevelId != null && editorLevelId != "welcome_to_defender_of_egril") {
                _pendingGameMessage.value = de.egril.defender.model.GameMessage(
                    type = de.egril.defender.model.GameMessageType.STORY_INTRO,
                    name = editorLevelId
                )
            }

            // Capture initial state snapshot
            initialGameStateSnapshot = createGameStateSnapshot(newGameState)
            lastSaveSnapshot = initialGameStateSnapshot

            de.egril.defender.analytics.reportEvent(de.egril.defender.analytics.GameEventType.LEVEL_STARTED, level.name)
            
            // Initialize achievement manager for this level
            val playerId = _currentPlayer.value?.id
            if (playerId != null) {
                achievementManager = de.egril.defender.game.AchievementManager(playerId).apply {
                    onAchievementEarned = { achievement ->
                        _newAchievement.value = achievement
                        // Refresh player profile to show updated achievements
                        refreshCurrentPlayer()
                    }
                    startLevel(newGameState.healthPoints.value)
                }
                
                // Set up combat result callback for kill tracking
                gameEngine?.setCombatResultCallback { result ->
                    // Track kills from this attack
                    result.killedEnemyTypes.forEach { enemyType ->
                        achievementManager?.onEnemyKilled(enemyType, result.killsThisAttack)
                    }
                }
                
                // Set up raft loss callback
                gameEngine?.setRaftLossCallback { reason ->
                    when (reason) {
                        de.egril.defender.game.RaftLossReason.MAP_EDGE -> 
                            achievementManager?.onRaftLostToMapEdge()
                        de.egril.defender.game.RaftLossReason.MAELSTROM -> 
                            achievementManager?.onRaftLostToMaelstrom()
                        de.egril.defender.game.RaftLossReason.OTHER -> {} // No achievement
                    }
                }
                
                // Set up dragon level change callback
                gameEngine?.setDragonLevelChangeCallback { oldLevel, newLevel ->
                    if (newLevel > oldLevel) {
                        achievementManager?.onIncreaseDragonLevel()
                    } else if (newLevel < oldLevel) {
                        achievementManager?.onReduceDragonLevel()
                    }
                }
            }
            
            // Start time tracking for reminders
            startTimeTracking()
        }
    }
    
    fun placeDefender(type: DefenderType, position: Position): Boolean {
        val gameState = _gameState.value
        val isInstantDeploy = gameState?.instantTowerSpellActive?.value == true

        val result = gameEngine?.placeDefender(type, position, isInstantDeploy) ?: false
        if (result) {
            if (isInstantDeploy) {
                // isInstantDeploy=true implies gameState!=null (see initialization above)
                gameState.currentMana.value = (gameState.currentMana.value - SpellType.INSTANT_TOWER.manaCost).coerceAtLeast(0)
                gameState.instantTowerSpellActive.value = false
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: Instant Tower spell consumed - tower placed instantly, mana deducted")
                }
            }
            // Track achievement
            val isRiverTile = gameState?.level?.isRiverTile(position) ?: false
            if (isRiverTile) {
                achievementManager?.onBuildRaft()
            } else {
                achievementManager?.onBuildTower()
            }
        }
        return result
    }
    
    fun upgradeDefender(defenderId: Int): Boolean {
        // Check if this is a raft before upgrading
        val defender = _gameState.value?.defenders?.find { it.id == defenderId }
        val isRaft = defender?.raftId?.value != null
        
        val result = gameEngine?.upgradeDefender(defenderId) ?: false
        if (result) {
            // Track achievement
            if (isRaft) {
                achievementManager?.onUpgradeRaft()
            } else {
                achievementManager?.onUpgradeTower()
            }
        }
        return result
    }
    
    fun undoTower(defenderId: Int): Boolean {
        // Check if this is a raft before undoing
        val defender = _gameState.value?.defenders?.find { it.id == defenderId }
        val isRaft = defender?.raftId?.value != null
        
        val result = gameEngine?.undoTower(defenderId) ?: false
        if (result) {
            // Track achievement
            if (isRaft) {
                achievementManager?.onUndoRaft()
            } else {
                achievementManager?.onUndoTower()
            }
        }
        return result
    }
    
    fun sellTower(defenderId: Int): Boolean {
        // Check if this is a raft before selling
        val defender = _gameState.value?.defenders?.find { it.id == defenderId }
        val isRaft = defender?.raftId?.value != null
        
        val result = gameEngine?.sellTower(defenderId) ?: false
        if (result) {
            // Track achievement
            if (isRaft) {
                achievementManager?.onSellRaft()
            } else {
                achievementManager?.onSellTower()
            }
        }
        return result
    }
    
    /**
     * Generate mana using a wizard tower
     * - Costs 1 action
     * - Generates base 5 mana + (wizard level / 5) bonus mana
     * - Cannot exceed max mana
     */
    fun generateMana(wizardId: Int): Boolean {
        val state = _gameState.value ?: return false
        val wizard = state.defenders.find { it.id == wizardId } ?: return false

        // Validate: must be wizard tower, must be ready, must have actions, not at max mana
        if (wizard.type != DefenderType.WIZARD_TOWER) return false
        if (!wizard.isReady) return false
        if (wizard.actionsRemaining.value <= 0) return false
        if (state.currentMana.value >= state.maxMana.value) return false

        // Calculate mana amount: base 5 + (level / 5)
        val manaAmount = 5 + (wizard.level.value / 5)

        // Add mana (capped at max)
        val newMana = minOf(state.currentMana.value + manaAmount, state.maxMana.value)
        state.currentMana.value = newMana

        // Consume action
        wizard.actionsRemaining.value -= 1

        return true
    }

    fun startFirstPlayerTurn() {
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: startFirstPlayerTurn called")
        }
        val stateBefore = _gameState.value
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: Phase before: ${stateBefore?.phase?.value}")
        }
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: Attackers before: ${stateBefore?.attackers?.size}")
        }

        gameEngine?.startFirstPlayerTurn()

        // Surface any messages queued during initial spawn (e.g. EWHAD_ENTERS) immediately,
        // so they appear as soon as the player's first turn begins rather than after they end it.
        surfaceNextPendingMessageIfIdle()
        
        // Track turn start for achievements
        achievementManager?.startTurn()
        gameEngine?.startTurnTracking()
        
        val stateAfter = _gameState.value
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: Phase after: ${stateAfter?.phase?.value}")
        }
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: Attackers after: ${stateAfter?.attackers?.size}")
        }
        stateAfter?.attackers?.forEach { attacker ->
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("DEBUG: Enemy ${attacker.id} - Type: ${attacker.type}, Position: (${attacker.position.value.x}, ${attacker.position.value.y})")
            }
        }
        
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: startFirstPlayerTurn completed")
        }
    }
    
    fun defenderAttack(defenderId: Int, targetId: Int): Boolean {
        val result = gameEngine?.defenderAttack(defenderId, targetId) ?: false
        if (result) {
            // Surface any messages queued by the attack (e.g. EWHAD_RETREATS/EWHAD_DEFEATED) immediately.
            surfaceNextPendingMessageIfIdle()
            // Check for immediate victory after attack
            val state = _gameState.value
            if (state != null && state.isLevelWon()) {
                completeLevel(state.level.id, won = true)
            }
        }
        return result
    }
    
    fun defenderAttackPosition(defenderId: Int, targetPosition: Position): Boolean {
        val result = gameEngine?.defenderAttackPosition(defenderId, targetPosition) ?: false
        if (result) {
            // triggerStateUpdate()

            // Surface any messages queued by the attack (e.g. EWHAD_RETREATS/EWHAD_DEFEATED) immediately.
            surfaceNextPendingMessageIfIdle()
            // Check for immediate victory after attack
            val state = _gameState.value
            if (state != null && state.isLevelWon()) {
                completeLevel(state.level.id, won = true)
            }
        }
        return result
    }
    
    fun performMineDig(mineId: Int): DigOutcome? {
        val outcome = gameEngine?.performMineDig(mineId)
        if (outcome != null) {
            // Track first dig achievement
            achievementManager?.onDigFirstTime()
            
            // Track specific outcome achievements
            when (outcome) {
                DigOutcome.GOLD -> achievementManager?.onFindGold()
                DigOutcome.DIAMOND -> achievementManager?.onFindDiamond()
                DigOutcome.DRAGON -> achievementManager?.onSummonDragon()
                else -> {} // No specific achievement for other outcomes
            }
        }
        return outcome
    }
    
    fun performMineBuildTrap(mineId: Int, trapPosition: Position): Boolean {
        return gameEngine?.performMineBuildTrap(mineId, trapPosition) ?: false
    }
    
    fun performWizardPlaceMagicalTrap(wizardId: Int, trapPosition: Position): Boolean {
        return gameEngine?.performWizardPlaceMagicalTrap(wizardId, trapPosition) ?: false
    }
    
    /**
     * Perform wizard mana generation
     * Called when player clicks the "Generate Mana" button on a wizard tower
     * Returns true if mana was generated successfully
     */
    fun performWizardGenerateMana(wizardId: Int): Boolean {
        return gameEngine?.performWizardGenerateMana(wizardId) ?: false
    }

    fun performBuildBarricade(towerId: Int, barricadePosition: Position): Boolean {
        val result = gameEngine?.performBuildBarricade(towerId, barricadePosition) ?: false
        if (result) {
            // Check if this is a new barricade or adding health to existing one
            val barricade = _gameState.value?.barricades?.find { it.position == barricadePosition }
            if (barricade != null) {
                // Barricade exists, so this added health to it
                achievementManager?.onAddHealthBarricade()
            } else {
                // New barricade was built
                achievementManager?.onBuildBarricade()
            }
        }
        return result
    }
    
    fun performRemoveBarricade(barricadePosition: Position): Int {
        return gameEngine?.removeBarricade(barricadePosition) ?: 0
    }
    
    fun performMineDigWithOutcome(outcome: DigOutcome): DigOutcome? {
        return gameEngine?.performMineDigWithOutcome(outcome)
    }

    fun endPlayerTurn() {
        val state = _gameState.value ?: return
        val engine = gameEngine ?: return

        // NOTE: Auto-attacks are NOT triggered here when clicking "End Turn".
        // They only happen when clicking "Auto-Attack and End Turn" button (see autoAttackAndEndTurn()).

        // Cancel instant tower spell if active (mana was never consumed, so nothing to refund)
        if (state.instantTowerSpellActive.value) {
            state.instantTowerSpellActive.value = false
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Instant Tower spell cancelled on end turn (no mana consumed)")
            }
        }

        // Process enemy turn with animations
        viewModelScope.launch(Dispatchers.Default) {
            // Start enemy turn: change phase to ENEMY_TURN
            // The UI immediately shows "ENEMY TURN" indicator when phase changes
            engine.startEnemyTurn()
            
            // Calculate all movement steps for existing units
            val enemyTurnMovements = engine.calculateEnemyTurnMovements()
            val movementSteps = enemyTurnMovements.allMovementSteps
            val attackersStoppedByBarricade = enemyTurnMovements.attackersStoppedByBarricade
            
            // Apply each movement step with a delay between steps
            for (stepMovements in movementSteps) {
                val trapCountBefore = _gameState.value?.trapTriggerEffects?.size ?: 0
                // Apply all movements in this step simultaneously
                for ((attackerId, newPosition) in stepMovements) {
                    engine.applyMovement(attackerId, newPosition)
                }
                // Delay between movement steps so user can see the animation (reduced from 400ms to 200ms)
                delay(200)
                // If a trap was triggered in this step, pause long enough for the trap animation to
                // complete (~830ms) before the enemy continues moving.
                val trapCountAfter = _gameState.value?.trapTriggerEffects?.size ?: 0
                if (trapCountAfter > trapCountBefore) {
                    delay(TRAP_ANIMATION_EXTRA_DELAY_MS)
                    // Process any enemies killed by this trap now so their death animation plays
                    // directly after the trap animation, before the next movement step begins.
                    processAndDelayForTrapDeaths()
                }
            }

            attackersStoppedByBarricade.forEach { it ->
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("attackBarricade B")
                }
                // fixit HERE
                engine.attackBarricade(it.second, it.first)
            }
            
            // Add a small delay to see final positions before spawning new units (reduced from 300ms to 150ms)
            if (movementSteps.isNotEmpty()) {
                delay(150)
            }

            // Now spawn new units (spawn points should be clear after movements)
            engine.spawnEnemyTurnAttackers()
            
            // Show spawned units briefly (reduced from 400ms to 200ms)
            delay(200)

            // Surface any pending spawn messages (e.g. Ewhad enters) while units are still at
            // their spawn points, so the message is displayed before they move away.
            surfaceNextPendingMessageIfIdle()
            
            // Move newly spawned units away from spawn points
            val newSpawnMovements = engine.calculateNewlySpawnedMovements()
            for (stepMovements in newSpawnMovements) {
                val trapCountBefore = _gameState.value?.trapTriggerEffects?.size ?: 0
                for ((attackerId, newPosition) in stepMovements) {
                    engine.applyMovement(attackerId, newPosition)
                }
                // Delay between movement steps (reduced from 400ms to 200ms)
                delay(200)
                // Pause extra if a trap fired during this step
                val trapCountAfter = _gameState.value?.trapTriggerEffects?.size ?: 0
                if (trapCountAfter > trapCountBefore) {
                    delay(TRAP_ANIMATION_EXTRA_DELAY_MS)
                    // Process any enemies killed by this trap now so their death animation plays
                    // directly after the trap animation, before the next movement step begins.
                    processAndDelayForTrapDeaths()
                }
            }
            
            // Add a small delay after newly spawned units have moved (reduced from 300ms to 150ms)
            if (newSpawnMovements.isNotEmpty()) {
                delay(150)
            }
            
            // Complete enemy turn: apply effects and return to player turn
            engine.completeEnemyTurn()
            
            // Trigger camera pan to bomb explosion position if any bomb exploded this turn
            val currentStateForBombs = _gameState.value
            if (currentStateForBombs != null && currentStateForBombs.bombExplosionEffects.isNotEmpty()) {
                _pendingScrollToPosition.value = currentStateForBombs.bombExplosionEffects.first().center
            }

            // Surface any remaining pending game messages (target taken, gate destroyed, etc.)
            // Only surface if no message is currently being shown (e.g. from the spawn phase above).
            // Each dismiss triggers the next message via dismissGameMessage().
            surfaceNextPendingMessageIfIdle()

            // Autosave at the beginning of the new player turn (after enemy turn completes)
            // This ensures the phase is PLAYER_TURN when the save is created
            autoSaveGame()
            
            // Check win/loss conditions
            val updatedState = _gameState.value ?: return@launch
            if (updatedState.isLevelWon()) {
                completeLevel(updatedState.level.id, won = true)
            } else if (updatedState.isLevelLost()) {
                completeLevel(updatedState.level.id, won = false)
            }
        }
    }

    fun autoAttackAndEndTurn() {
        // This method is called from the "Auto-Attack and End Turn" button in the confirmation dialog
        // It explicitly calls autoDefenderAttacks() before ending the turn
        val engine = gameEngine ?: return
        val currentState = gameState.value ?: return
        
        // Explicitly trigger auto-attacks for all ready defenders
        engine.autoDefenderAttacks()
        
        // Check if there are special actions remaining (mines, alchemy, wizard traps)
        val specialActionTypes = currentState.getDefenderTypesWithSpecialActions()
        
        if (specialActionTypes.isNotEmpty()) {
            // There are special actions remaining - show warning dialog instead of ending turn
            // Store the types so the UI can display them
            _specialActionsRemaining.value = specialActionTypes
        } else {
            // No special actions remaining - proceed with ending turn
            endPlayerTurn()
        }
    }
    
    fun clearSpecialActionsWarning() {
        _specialActionsRemaining.value = emptyList()
    }

    private fun completeLevel(levelId: Int, won: Boolean) {
        val currentHP = _gameState.value?.healthPoints?.value ?: 0
        val xpEarned = _gameState.value?.xpEarnedThisLevel?.value ?: 0
        val levelName = _gameState.value?.level?.name ?: "unknown"
        val turnNumber = _gameState.value?.turnNumber?.value

        de.egril.defender.analytics.reportEvent(if (won) de.egril.defender.analytics.GameEventType.LEVEL_WON else de.egril.defender.analytics.GameEventType.LEVEL_LOST, levelName, turnNumber)

        // Track achievement for level completion
        if (won) {
            achievementManager?.onWinLevel(currentHP)

            // Award XP to player profile
            val currentPlayer = _currentPlayer.value
            if (currentPlayer != null) {
                val updatedStats = currentPlayer.abilities.addXP(xpEarned)
                val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
                _currentPlayer.value = updatedPlayer
                de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
            }
        } else {
            achievementManager?.onLoseLevel()
        }
        
        val isLastLevel = _worldLevels.value.lastOrNull()?.level?.id == levelId
        if (won) {
            val updatedLevels = _worldLevels.value.toMutableList()
            val currentIndex = updatedLevels.indexOfFirst { it.level.id == levelId }
            if (currentIndex >= 0) {
                updatedLevels[currentIndex] = updatedLevels[currentIndex].copy(status = LevelStatus.WON)
                
                // Get the set of all won level IDs (including the just-won level)
                val wonLevelIds = updatedLevels
                    .filter { it.status == LevelStatus.WON }
                    .mapNotNull { it.level.editorLevelId }
                    .toSet()
                
                // Unlock levels based on prerequisites
                for (i in updatedLevels.indices) {
                    val worldLevel = updatedLevels[i]
                    if (worldLevel.status == LevelStatus.LOCKED && worldLevel.level.editorLevelId != null) {
                        if (de.egril.defender.editor.EditorStorage.isLevelUnlocked(worldLevel.level.editorLevelId, wonLevelIds)) {
                            updatedLevels[i] = worldLevel.copy(status = LevelStatus.UNLOCKED)
                        }
                    }
                }
                
                _worldLevels.value = updatedLevels
                // Save world map status
                saveWorldMapStatus()
            }
            // Sync updated abilities (XP awarded) and level progress to backend
            uploadUserDataToBackend()
        }

        if (_isDemoMode.value) {
            // In demo mode: show the Level Won/Lost screen for 4 seconds, then load the next level
            _currentScreen.value = Screen.LevelComplete(levelId, won, isLastLevel, xpEarned)
            viewModelScope.launch {
                delay(4000L)
                if (_isDemoMode.value) {
                    demoLevelIndex = (demoLevelIndex + 1) % de.egril.defender.game.DemoMode.DEMO_MAP_IDS.size
                    loadDemoLevel(demoLevelIndex)
                }
            }
            return
        }
        _currentScreen.value = Screen.LevelComplete(levelId, won, isLastLevel, xpEarned)
    }
    
    fun restartLevel() {
        val levelId = (_currentScreen.value as? Screen.LevelComplete)?.levelId
            ?: (_currentScreen.value as? Screen.GamePlay)?.levelId
            ?: return
        startLevel(levelId)
    }

    // -------------------------------------------------------------------------
    // Demo Mode
    // -------------------------------------------------------------------------

    fun startDemoMode() {
        _isDemoMode.value = true
        demoLevelIndex = 0
        loadDemoLevel(0)
    }

    fun stopDemoMode() {
        demoJob?.cancel()
        demoJob = null
        _isDemoMode.value = false
        // Clear any pending visual preview state
        _demoSelectedDefenderType.value = null
        _demoHoveredPosition.value = null
        _demoSelectedDefenderId.value = null
        _demoSelectedTargetPosition.value = null
        navigateToWorldMap()
    }

    private fun loadDemoLevel(index: Int) {
        val demoLevel = de.egril.defender.game.DemoMode.createDemoLevel(index) ?: return

        val newGameState = GameState(
            level = demoLevel,
            coins = mutableStateOf(demoLevel.initialCoins),
            healthPoints = mutableStateOf(demoLevel.healthPoints),
            spawnPlan = demoLevel.directSpawnPlan ?: emptyList()
        )
        // Towers are placed dynamically by startDemoAutoPlay() — no pre-placed elements here.

        _gameState.value = newGameState
        gameEngine = GameEngine(newGameState)
        _currentScreen.value = Screen.GamePlay(demoLevel.id)

        // Dismiss any info dialogs (e.g. river info on the creek level) after a short delay
        viewModelScope.launch {
            delay(de.egril.defender.game.DemoMode.INFO_DISMISS_DELAY_MS)
            if (_isDemoMode.value) {
                _gameState.value?.let { state ->
                    if (state.infoState.value.currentInfo != de.egril.defender.model.InfoType.NONE) {
                        state.infoState.value = state.infoState.value.dismissInfo()
                    }
                }
            }
        }

        startDemoAutoPlay()
    }

    /**
     * Place a tower with a visual preview: shows the placement highlight for
     * [DemoMode.TOWER_PLACE_DELAY_MS] ms, then places the tower, then waits
     * [DemoMode.TOWER_AFTER_PLACE_MS] ms before continuing.
     */
    private suspend fun demoPlaceTowerWithPreview(type: DefenderType, position: Position) {
        _demoSelectedDefenderType.value = type
        _demoHoveredPosition.value = position
        delay(de.egril.defender.game.DemoMode.TOWER_PLACE_DELAY_MS)
        _demoSelectedDefenderType.value = null
        _demoHoveredPosition.value = null
        gameEngine?.placeDefender(type, position)
        delay(de.egril.defender.game.DemoMode.TOWER_AFTER_PLACE_MS)
    }

    /**
     * Perform all remaining auto-attacks for the current player turn, one attack at a time.
     * Before each attack the aiming circles are shown for [DemoMode.ATTACK_AIM_MS] ms,
     * and after each attack there is a [DemoMode.ATTACK_AFTER_MS] ms pause.
     * Reuses the same target-selection logic as [GameEngine.autoDefenderAttacks].
     *
     * The selected defender is kept highlighted across all its attacks (no flicker between
     * attacks of the same tower) and switches directly to the next defender without going
     * through an unselected state. Selection is only cleared after all attacks are done.
     *
     * If the level is won or lost mid-attack, [completeLevel] is called immediately so the
     * demo advances to the level-complete screen without waiting for the player to end the turn.
     */
    private suspend fun demoAttackOneByOne() {
        val engine = gameEngine ?: return
        val defenderIds = _gameState.value?.defenders
            ?.filter { it.isReady && !it.isDisabled.value && it.type.attackType != AttackType.NONE }
            ?.map { it.id }
            ?: return

        try {
            for (id in defenderIds) {
                if (!_isDemoMode.value || _gameState.value?.phase?.value != GamePhase.PLAYER_TURN) return
                // Select this defender now — switching directly from the previous one (no unselected gap)
                _demoSelectedDefenderId.value = id
                // Keep attacking while this defender still has action points
                while (_isDemoMode.value && _gameState.value?.phase?.value == GamePhase.PLAYER_TURN) {
                    val defender = _gameState.value?.defenders?.find { it.id == id } ?: break
                    if (defender.actionsRemaining.value <= 0) break
                    val targetPos = engine.getNextAutoAttackTargetPosition(defender) ?: break

                    // Show aiming circles (defender already selected above)
                    _demoSelectedTargetPosition.value = targetPos
                    delay(de.egril.defender.game.DemoMode.ATTACK_AIM_MS)

                    // Clear only the target position before executing the attack;
                    // keep the defender highlighted so the UI stays in tower-selected view
                    _demoSelectedTargetPosition.value = null

                    val success = engine.performOneAutoAttack(id)
                    if (!success) break

                    // Check for immediate level end (last enemy killed) — trigger completeLevel
                    // right now instead of waiting for endPlayerTurn so the demo advances immediately.
                    val stateAfterAttack = _gameState.value
                    if (stateAfterAttack != null) {
                        if (stateAfterAttack.isLevelWon()) {
                            completeLevel(stateAfterAttack.level.id, won = true)
                            return
                        } else if (stateAfterAttack.isLevelLost()) {
                            completeLevel(stateAfterAttack.level.id, won = false)
                            return
                        }
                    }

                    delay(de.egril.defender.game.DemoMode.ATTACK_AFTER_MS)
                }
                // Loop will directly set the next defender id without an unselected frame
            }
        } finally {
            // Clear selection only once, after all attacks are complete
            _demoSelectedDefenderId.value = null
            _demoSelectedTargetPosition.value = null
        }
    }

    private fun startDemoAutoPlay() {
        demoJob?.cancel()
        demoJob = viewModelScope.launch {
            val mapId = de.egril.defender.game.DemoMode.DEMO_MAP_IDS.getOrNull(demoLevelIndex)
            val initialTowers = mapId?.let { de.egril.defender.game.DemoMode.DEMO_TOWERS[it] } ?: emptyList()

            // Phase 1: place initial towers one by one with preview + delays in the INITIAL_BUILDING phase
            delay(de.egril.defender.game.DemoMode.INITIAL_BUILDING_DELAY_MS)
            for (tower in initialTowers) {
                if (!_isDemoMode.value || _gameState.value?.phase?.value != GamePhase.INITIAL_BUILDING) break
                demoPlaceTowerWithPreview(tower.type, tower.position)
            }
            // Brief pause after last tower is placed so the player can see all towers before battle starts
            delay(de.egril.defender.game.DemoMode.INITIAL_BUILDING_DELAY_MS)
            if (_isDemoMode.value && _gameState.value?.phase?.value == GamePhase.INITIAL_BUILDING) {
                startFirstPlayerTurn()
            }

            // Phase 2: auto-play turns
            while (_isDemoMode.value) {
                when (_gameState.value?.phase?.value) {
                    GamePhase.PLAYER_TURN -> {
                        delay(de.egril.defender.game.DemoMode.PLAYER_TURN_DELAY_MS)
                        val currentState = _gameState.value ?: break
                        if (!_isDemoMode.value || currentState.phase.value != GamePhase.PLAYER_TURN) continue
                        if (currentState.isLevelWon() || currentState.isLevelLost()) {
                            delay(de.egril.defender.game.DemoMode.ENEMY_TURN_POLL_MS)
                            continue
                        }

                        // Try to place a new tower if coins allow and a free build area exists
                        val occupiedPositions = currentState.defenders.map { it.position.value }.toSet()
                        val freeBuildAreas = currentState.level.buildAreas - occupiedPositions
                        if (freeBuildAreas.isNotEmpty()) {
                            for (type in currentState.level.availableTowers.sortedByDescending { it.baseCost }) {
                                if (currentState.canPlaceDefender(type)) {
                                    val targetPos = freeBuildAreas.first()
                                    // Show preview, then place
                                    _demoSelectedDefenderType.value = type
                                    _demoHoveredPosition.value = targetPos
                                    delay(de.egril.defender.game.DemoMode.TOWER_PLACE_DELAY_MS)
                                    _demoSelectedDefenderType.value = null
                                    _demoHoveredPosition.value = null
                                    val stateNow = _gameState.value
                                    // Re-check that position is still free after the delay
                                    val stillFree = stateNow?.defenders?.none { it.position.value == targetPos } == true
                                    if (_isDemoMode.value && stateNow?.phase?.value == GamePhase.PLAYER_TURN && stillFree) {
                                        gameEngine?.placeDefender(type, targetPos)
                                        delay(de.egril.defender.game.DemoMode.TOWER_AFTER_PLACE_MS)
                                    }
                                    break
                                }
                            }
                        }

                        // Try to upgrade the cheapest upgradeable tower
                        val stateForUpgrade = _gameState.value
                        if (stateForUpgrade != null && _isDemoMode.value &&
                            stateForUpgrade.phase.value == GamePhase.PLAYER_TURN
                        ) {
                            val upgradeCandidate = stateForUpgrade.defenders
                                .filter { stateForUpgrade.canUpgradeDefender(it) }
                                .minByOrNull { it.upgradeCost }
                            if (upgradeCandidate != null) {
                                delay(de.egril.defender.game.DemoMode.TOWER_PLACE_DELAY_MS)
                                val stateNow = _gameState.value
                                if (_isDemoMode.value && stateNow?.phase?.value == GamePhase.PLAYER_TURN) {
                                    gameEngine?.upgradeDefender(upgradeCandidate.id)
                                }
                            }
                        }

                        // Attack one by one with aiming circles, then end turn
                        val finalState = _gameState.value ?: break
                        if (!_isDemoMode.value || finalState.phase.value != GamePhase.PLAYER_TURN) continue
                        if (finalState.isLevelWon() || finalState.isLevelLost()) continue
                        demoAttackOneByOne()
                        if (_isDemoMode.value && _gameState.value?.phase?.value == GamePhase.PLAYER_TURN &&
                            !(_gameState.value?.isLevelWon() ?: false) && !(_gameState.value?.isLevelLost() ?: false)
                        ) {
                            endPlayerTurn()
                        }
                    }
                    GamePhase.ENEMY_TURN,
                    GamePhase.INITIAL_BUILDING -> delay(de.egril.defender.game.DemoMode.ENEMY_TURN_POLL_MS)
                    null -> break
                }
            }
        }
    }
    
    fun applyCheatCode(code: String): Boolean {
        // Check for reminder testing cheat codes first
        val lowerCode = code.lowercase().trim()
        if (lowerCode == "breakreminder" || lowerCode == "break") {
            // Trigger break reminder with current session time
            val currentTime = de.egril.defender.utils.currentTimeMillis()
            gameSessionStartTime?.let { sessionStart ->
                val elapsedMs = currentTime - sessionStart
                val elapsedTime = formatElapsedTime(elapsedMs)
                _reminderMessage.value = ReminderMessage(
                    type = de.egril.defender.ui.gameplay.ReminderType.BREAK,
                    elapsedTime = elapsedTime
                )
            }
            return true
        }
        
        if (lowerCode == "sleepreminder" || lowerCode == "sleep") {
            // Trigger sleep reminder
            val currentTime = de.egril.defender.utils.currentTimeMillis()
            val hour = de.egril.defender.utils.getLocalHour(currentTime)
            val timeDescription = when {
                hour == 23 -> "close_to_midnight"
                hour == 0 -> "midnight"
                else -> "after_midnight"
            }
            _reminderMessage.value = ReminderMessage(
                type = de.egril.defender.ui.gameplay.ReminderType.SLEEP,
                timeDescription = timeDescription
            )
            return true
        }
        
        val (success, digOutcome) = CheatCodeHandler.applyCheatCode(
            code = code,
            addCoins = { amount -> gameEngine?.addCoins(amount) },
            setCoins = { amount -> gameEngine?.setCoins(amount) },
            performMineDigWithOutcome = { outcome -> performMineDigWithOutcome(outcome) },
            spawnEnemy = { attackerType, level -> gameEngine?.spawnEnemy(attackerType, level) },
            showPlatformInfo = { _showPlatformInfo.value = true },
            setBigHeadMode = { enabled -> de.egril.defender.utils.BigHeadMode.isEnabled.value = enabled },
            addMana = { amount -> gameEngine?.addMana(amount) },
            removeMana = { amount -> gameEngine?.removeMana(amount) },
            showCheatHelp = { _showCheatHelp.value = true }
        )
        
        if (digOutcome != null) {
            _cheatDigOutcome.value = digOutcome
        }
        
        return success
    }
    
    fun clearCheatDigOutcome() {
        _cheatDigOutcome.value = null
    }
    
    fun clearPlatformInfo() {
        _showPlatformInfo.value = false
    }
    
    fun clearCheatHelp() {
        _showCheatHelp.value = false
    }

    // Player stat/XP/spell cheat methods
    private fun addPlayerXP(amount: Int) {
        val currentPlayer = _currentPlayer.value ?: return
        val updatedStats = currentPlayer.abilities.addXP(amount)
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
        uploadUserDataToBackend()
    }

    private fun removePlayerXP(amount: Int) {
        val currentPlayer = _currentPlayer.value ?: return
        val currentXP = currentPlayer.abilities.totalXP
        val newXP = maxOf(0, currentXP - amount)
        val newLevel = de.egril.defender.model.PlayerAbilities.calculateLevel(newXP)
        val updatedStats = currentPlayer.abilities.copy(totalXP = newXP, level = newLevel)
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
        uploadUserDataToBackend()
    }

    private fun addPlayerStat(statName: String, amount: Int) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities

        val updatedStats = when (statName.lowercase()) {
            "health" -> oldStats.copy(healthAbility = oldStats.healthAbility + amount)
            "treasury" -> oldStats.copy(treasuryAbility = oldStats.treasuryAbility + amount)
            "income" -> oldStats.copy(incomeAbility = oldStats.incomeAbility + amount)
            "construction" -> oldStats.copy(constructionAbility = oldStats.constructionAbility + amount)
            "mana" -> oldStats.copy(manaAbility = oldStats.manaAbility + amount)
            else -> return  // Invalid stat name
        }

        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
        uploadUserDataToBackend()
    }

    private fun removePlayerStat(statName: String, amount: Int) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities

        val updatedStats = when (statName.lowercase()) {
            "health" -> oldStats.copy(healthAbility = maxOf(0, oldStats.healthAbility - amount))
            "treasury" -> oldStats.copy(treasuryAbility = maxOf(0, oldStats.treasuryAbility - amount))
            "income" -> oldStats.copy(incomeAbility = maxOf(0, oldStats.incomeAbility - amount))
            "construction" -> oldStats.copy(constructionAbility = maxOf(0, oldStats.constructionAbility - amount))
            "mana" -> oldStats.copy(manaAbility = maxOf(0, oldStats.manaAbility - amount))
            else -> return  // Invalid stat name
        }

        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
        uploadUserDataToBackend()
    }

    private fun unlockPlayerSpell(spellName: String) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities

        // Parse spell name to SpellType
        val spell = when (spellName.lowercase().replace(" ", "_")) {
            "attack_aimed", "attackaimed" -> de.egril.defender.model.SpellType.ATTACK_AIMED
            "attack_area", "attackarea", "fireball" -> de.egril.defender.model.SpellType.ATTACK_AREA
            "heal" -> de.egril.defender.model.SpellType.HEAL
            "instant_tower", "instanttower" -> de.egril.defender.model.SpellType.INSTANT_TOWER
            "bomb" -> de.egril.defender.model.SpellType.BOMB
            "double_level", "doublelevel", "double_tower_level", "doubletowerlevel" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_LEVEL
            "cooling", "cooling_spell", "coolingspell" -> de.egril.defender.model.SpellType.COOLING_SPELL
            "freeze", "freeze_spell", "freezespell" -> de.egril.defender.model.SpellType.FREEZE_SPELL
            "double_reach", "doublereach", "double_tower_reach", "doubletowerreach" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_REACH
            "fear", "fear_spell", "fearspell" -> de.egril.defender.model.SpellType.FEAR_SPELL
            "fear_area", "feararea", "fear_spell_area", "fearspellarea" -> de.egril.defender.model.SpellType.FEAR_SPELL_AREA
            else -> return  // Invalid spell name
        }

        val updatedStats = oldStats.copy(unlockedSpells = oldStats.unlockedSpells + spell)
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
        uploadUserDataToBackend()
    }

    private fun lockPlayerSpell(spellName: String) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities

        // Parse spell name to SpellType
        val spell = when (spellName.lowercase().replace(" ", "_")) {
            "attack_aimed", "attackaimed" -> de.egril.defender.model.SpellType.ATTACK_AIMED
            "attack_area", "attackarea", "fireball" -> de.egril.defender.model.SpellType.ATTACK_AREA
            "heal" -> de.egril.defender.model.SpellType.HEAL
            "instant_tower", "instanttower" -> de.egril.defender.model.SpellType.INSTANT_TOWER
            "bomb" -> de.egril.defender.model.SpellType.BOMB
            "double_level", "doublelevel", "double_tower_level", "doubletowerlevel" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_LEVEL
            "cooling", "cooling_spell", "coolingspell" -> de.egril.defender.model.SpellType.COOLING_SPELL
            "freeze", "freeze_spell", "freezespell" -> de.egril.defender.model.SpellType.FREEZE_SPELL
            "double_reach", "doublereach", "double_tower_reach", "doubletowerreach" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_REACH
            "fear", "fear_spell", "fearspell" -> de.egril.defender.model.SpellType.FEAR_SPELL
            "fear_area", "feararea", "fear_spell_area", "fearspellarea" -> de.egril.defender.model.SpellType.FEAR_SPELL_AREA
            else -> return  // Invalid spell name
        }

        val updatedStats = oldStats.copy(unlockedSpells = oldStats.unlockedSpells - spell)
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
        uploadUserDataToBackend()
    }

    fun applyWorldMapCheatCode(code: String): Boolean {
        // Check for "demo" cheat code (starts automated demo mode)
        if (code.lowercase().trim() == "demo") {
            startDemoMode()
            return true
        }

        // Check for "sticker" cheat code first (navigation cheat)
        if (code.lowercase().trim() == "sticker") {
            navigateToSticker()
            return true
        }

        // Check for "spinner" cheat code (shows loading spinner demo for 30s)
        if (code.lowercase().trim() == "spinner") {
            navigateToLoadingSpinnerDemo()
            return true
        }

        // Check for "credits" cheat code (shows the final credits screen)
        if (code.lowercase().trim() == "credits") {
            navigateToFinalCredits()
            return true
        }

        // Check for "animation" cheat code (shows the animation test/preview screen)
        if (code.lowercase().trim() == "animation") {
            navigateToAnimationTest()
            return true
        }

        return CheatCodeHandler.applyWorldMapCheatCode(
            code = code,
            unlockAllLevels = { unlockAllLevels() },
            unlockLevel = { editorLevelId -> unlockLevel(editorLevelId) },
            lockAllLevels = { lockAllLevels() },
            lockLevel = { editorLevelId -> lockLevel(editorLevelId) },
            worldLevels = _worldLevels.value,
            showPlatformInfo = { _showPlatformInfo.value = true },
            addXP = { amount -> addPlayerXP(amount) },
            removeXP = { amount -> removePlayerXP(amount) },
            addStatLevel = { statName, amount -> addPlayerStat(statName, amount) },
            removeStatLevel = { statName, amount -> removePlayerStat(statName, amount) },
            unlockSpell = { spellName -> unlockPlayerSpell(spellName) },
            lockSpell = { spellName -> lockPlayerSpell(spellName) },
            showCheatHelp = { _showCheatHelp.value = true }
        )
    }
    
    private fun unlockAllLevels() {
        _worldLevels.value = CheatCodeHandler.unlockAllLevels(_worldLevels.value)
        // Save updated world map status
        saveWorldMapStatus()
        uploadUserDataToBackend()
    }
    
    private fun unlockLevel(editorLevelId: String) {
        _worldLevels.value = CheatCodeHandler.unlockLevel(_worldLevels.value, editorLevelId)
        // Save updated world map status
        saveWorldMapStatus()
        uploadUserDataToBackend()
    }
    
    private fun lockAllLevels() {
        _worldLevels.value = CheatCodeHandler.lockAllLevels(_worldLevels.value)
        // Save updated world map status
        saveWorldMapStatus()
        uploadUserDataToBackend()
    }
    
    private fun lockLevel(editorLevelId: String) {
        _worldLevels.value = CheatCodeHandler.lockLevel(_worldLevels.value, editorLevelId)
        // Save updated world map status
        saveWorldMapStatus()
        uploadUserDataToBackend()
    }
    
    // Save/Load functionality
    
    private val _savedGames = MutableStateFlow<List<de.egril.defender.save.SaveGameMetadata>>(emptyList())
    val savedGames: StateFlow<List<de.egril.defender.save.SaveGameMetadata>> = _savedGames.asStateFlow()

    private val _isLoadingRemoteSaves = MutableStateFlow(false)
    val isLoadingRemoteSaves: StateFlow<Boolean> = _isLoadingRemoteSaves.asStateFlow()

    /**
     * In-memory cache of raw JSON for remote-only savefiles. Keyed by saveId.
     * Populated during [refreshSavedGames] so that [loadGame] can download and
     * store a remote-only save locally without a second network round-trip.
     */
    private val remoteFilesCache = mutableMapOf<String, String>()
    
    fun navigateToLoadGame() {
        refreshSavedGames()
        _currentScreen.value = Screen.LoadGame
    }
    
    fun saveCurrentGame(comment: String? = null): String? {
        val state = _gameState.value ?: return null
        val saveId = de.egril.defender.save.SaveFileStorage.saveGameState(state, comment)
        refreshSavedGames()
        // Update the last save snapshot
        lastSaveSnapshot = createGameStateSnapshot(state)
        // Upload to backend in background if the user is logged in
        uploadSavefileToBackend(saveId)
        return saveId
    }
    
    /**
     * Create an autosave at the beginning of a new turn.
     * Autosaves always use the fixed ID "autosave_game" so they overwrite previous autosaves.
     */
    private fun autoSaveGame() {
        val state = _gameState.value ?: return
        // Use fixed ID "autosave_game" and add "Autosave" as comment
        de.egril.defender.save.SaveFileStorage.saveGameState(state, comment = "Autosave", saveId = "autosave_game")
        refreshSavedGames()
        // Upload autosave to backend in background if the user is logged in
        uploadSavefileToBackend("autosave_game")
        // Don't update lastSaveSnapshot for autosaves - we still want to track manual saves separately
    }

    /**
     * Uploads a locally-saved game to the backend if the user is currently logged in.
     * Runs in the background so it never blocks gameplay.
     */
    private fun uploadSavefileToBackend(saveId: String) {
        val token = de.egril.defender.iam.IamService.getToken()
        if (token == null) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Skipping backend upload for $saveId: not authenticated")
            }
            return
        }
        viewModelScope.launch {
            val json = de.egril.defender.save.SaveFileStorage.getSaveGameJson(saveId) ?: return@launch
            try {
                val success = de.egril.defender.save.BackendSaveService.uploadSavefile(saveId, json, token)
                if (success) {
                    // Re-merge so the card shows the "remote" chip
                    refreshSavedGames()
                }
            } catch (e: Exception) {
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Failed to upload savefile $saveId to backend: ${e.message}")
                }
            }
        }
    }

    /**
     * Imports a savefile from [remoteFilesCache] into local storage and loads it.
     * Returns the deserialized [SavedGame] on success, null if the save ID is not in the
     * cache or the import fails.
     */
    private fun importAndLoadFromRemoteCache(saveId: String): de.egril.defender.save.SavedGame? {
        val remoteJson = remoteFilesCache[saveId] ?: return null
        val imported = de.egril.defender.save.SaveFileStorage.importSaveGame(
            filename = "$saveId.json",
            jsonContent = remoteJson,
            overwrite = true
        )
        if (!imported) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to import remote savefile $saveId into local storage")
            }
            return null
        }
        return de.egril.defender.save.SaveFileStorage.loadGameState(saveId)
    }
    
    /**
     * Check if an autosave exists
     */
    fun hasAutosave(): Boolean {
        return de.egril.defender.save.SaveFileStorage.loadGameState("autosave_game") != null
    }
    
    /**
     * Load the autosave and start playing
     */
    fun continueFromAutosave() {
        loadGame("autosave_game")
    }
    
    /**
     * Create a snapshot of the current game state for change detection
     */
    private fun createGameStateSnapshot(state: GameState): String {
        // Create a simple hash/snapshot of key game state properties
        return buildString {
            append("turn:${state.turnNumber.value}")
            append("|coins:${state.coins.value}")
            append("|health:${state.healthPoints.value}")
            append("|phase:${state.phase.value}")
            append("|defenders:${state.defenders.size}")
            state.defenders.sortedBy { it.id }.forEach { defender ->
                append("|d${defender.id}:${defender.type},${defender.position.value.x},${defender.position.value.y},${defender.level.value},${defender.buildTimeRemaining.value}")
            }
            append("|attackers:${state.attackers.size}")
            state.attackers.sortedBy { it.id }.forEach { attacker ->
                append("|a${attacker.id}:${attacker.type},${attacker.position.value.x},${attacker.position.value.y},${attacker.currentHealth.value},${attacker.isDefeated.value}")
            }
            append("|effects:${state.fieldEffects.size}")
            append("|traps:${state.traps.size}")
        }
    }
    
    /**
     * Check if there are unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        val currentState = _gameState.value ?: return false
        val currentSnapshot = createGameStateSnapshot(currentState)
        val referenceSnapshot = lastSaveSnapshot ?: initialGameStateSnapshot ?: return false
        return currentSnapshot != referenceSnapshot
    }
    
    fun loadGame(saveId: String) {
        // Try to load from local storage first; if not present, import from remote cache
        val savedGame = de.egril.defender.save.SaveFileStorage.loadGameState(saveId)
            ?: importAndLoadFromRemoteCache(saveId)
            ?: return
        
        // Find the level by ID
        val level = _worldLevels.value.find { it.level.id == savedGame.levelId }?.level
        
        // Verify map ID matches if both are available
        if (level != null && savedGame.mapId != null && level.mapId != null) {
            if (savedGame.mapId != level.mapId) {
                // Map mismatch - the level at this numeric ID now uses a different map
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("WARNING: Save file has different map ID (saved: ${savedGame.mapId}, current: ${level.mapId})")
                }
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Level sequence may have changed. Attempting to find level with matching map ID...")
                }

                // Try to find any level that uses the same map as the saved game
                val levelWithCorrectMap = _worldLevels.value
                    .map { it.level }
                    .find { it.mapId == savedGame.mapId }
                
                if (levelWithCorrectMap != null) {
                    println("Found level with matching map ID: ${levelWithCorrectMap.name} (ID: ${levelWithCorrectMap.id})")
                    val gameState = de.egril.defender.save.SaveFileStorage.convertSavedGameToGameState(savedGame, levelWithCorrectMap)
                    _gameState.value = gameState
                    gameEngine = GameEngine(gameState)
                    _currentScreen.value = Screen.GamePlay(levelWithCorrectMap.id)
                    // Set snapshots when loading a saved game
                    initialGameStateSnapshot = createGameStateSnapshot(gameState)
                    lastSaveSnapshot = initialGameStateSnapshot
                    
                    // Start time tracking for reminders
                    startTimeTracking()
                    return
                } else {
                    if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("ERROR: Could not find any level with map ID ${savedGame.mapId}. Save file may be incompatible.")
                    }
                    // TODO: Show error dialog to user
                    return
                }
            }
        }
        
        // No map ID mismatch or one/both map IDs are null (backward compatibility)
        if (level != null) {
            val gameState = de.egril.defender.save.SaveFileStorage.convertSavedGameToGameState(savedGame, level)
            _gameState.value = gameState
            gameEngine = GameEngine(gameState)
            _currentScreen.value = Screen.GamePlay(savedGame.levelId)
            // Set snapshots when loading a saved game
            initialGameStateSnapshot = createGameStateSnapshot(gameState)
            lastSaveSnapshot = initialGameStateSnapshot
            
            // Start time tracking for reminders
            startTimeTracking()
        }
    }
    
    /**
     * Import world map progress and check for conflicts
     * Returns true if conflict detected and shown to user
     */
    fun importWorldMapProgress(json: String): Boolean {
        val importedWorldMap = de.egril.defender.save.SaveFileStorage.importWorldMapProgress(json)
        
        if (importedWorldMap != null) {
            // Conflict detected - show dialog
            val currentWorldMap = de.egril.defender.save.SaveFileStorage.loadWorldMapStatus() ?: emptyMap()
            _worldMapConflict.value = WorldMapConflict(
                savedGame = null,  // No associated save game
                savedWorldMap = importedWorldMap,
                currentWorldMap = currentWorldMap,
                level = null  // No level to load
            )
            return true
        }
        
        return false  // No conflict, nothing to do
    }
    
    /**
     * Resolve world map conflict by choosing which version to keep
     */
    fun resolveWorldMapConflict(useSavedVersion: Boolean) {
        val conflict = _worldMapConflict.value ?: return
        
        if (useSavedVersion) {
            // Use the world map from the import/save
            val updatedWorldLevels = de.egril.defender.save.SaveFileStorage.applyWorldMapProgress(
                conflict.savedWorldMap,
                _worldLevels.value
            )
            _worldLevels.value = updatedWorldLevels
        }
        // If not using saved version, keep current world map (do nothing)
        
        // Clear the conflict
        _worldMapConflict.value = null
        
        // If there's an associated saved game and level, load it
        if (conflict.savedGame != null && conflict.level != null) {
            val gameState = de.egril.defender.save.SaveFileStorage.convertSavedGameToGameState(conflict.savedGame, conflict.level)
            _gameState.value = gameState
            gameEngine = GameEngine(gameState)
            _currentScreen.value = Screen.GamePlay(conflict.level.id)
            // Set snapshots when loading a saved game
            initialGameStateSnapshot = createGameStateSnapshot(gameState)
            lastSaveSnapshot = initialGameStateSnapshot
        }
    }
    
    /**
     * Cancel world map conflict resolution
     */
    fun cancelWorldMapConflict() {
        _worldMapConflict.value = null
    }
    
    fun deleteSavedGame(saveId: String) {
        de.egril.defender.save.SaveFileStorage.deleteSavedGame(saveId)
        refreshSavedGames()
    }
    
    // Download/Upload functionality
    
    fun downloadSaveGame(saveId: String, includeGameState: Boolean = false) {
        viewModelScope.launch {
            val jsonContent = if (includeGameState) {
                de.egril.defender.save.SaveFileStorage.getSaveGameWithWorldMapJson(saveId)
            } else {
                de.egril.defender.save.SaveFileStorage.getSaveGameJson(saveId)
            }
            
            if (jsonContent != null) {
                val fileExportImport = de.egril.defender.save.getFileExportImport()
                val filename = if (includeGameState) {
                    "${saveId}_with_progress.json"
                } else {
                    "$saveId.json"
                }
                fileExportImport.exportFile(filename, jsonContent)
            }
        }
    }
    
    fun downloadAllSaveGames(includeGameState: Boolean = false) {
        viewModelScope.launch {
            val allSaves = if (includeGameState) {
                // Get all saves with world map included
                de.egril.defender.save.SaveFileStorage.getAllSaveGamesJson().mapValues { (filename, _) ->
                    val saveId = filename.removeSuffix(".json")
                    de.egril.defender.save.SaveFileStorage.getSaveGameWithWorldMapJson(saveId) ?: ""
                }.filter { it.value.isNotEmpty() }
            } else {
                de.egril.defender.save.SaveFileStorage.getAllSaveGamesJson()
            }
            
            if (allSaves.isNotEmpty()) {
                val timestamp = de.egril.defender.utils.formatTimestampISO(de.egril.defender.utils.currentTimeMillis())
                val zipFilename = if (includeGameState) {
                    "defender-of-egril-saves-with-progress-$timestamp.zip"
                } else {
                    "defender-of-egril-saves-$timestamp.zip"
                }
                val fileExportImport = de.egril.defender.save.getFileExportImport()
                fileExportImport.exportZip(zipFilename, allSaves)
            }
        }
    }
    
    fun downloadGameState() {
        viewModelScope.launch {
            val jsonContent = de.egril.defender.save.SaveFileStorage.exportWorldMapProgress()
            val fileExportImport = de.egril.defender.save.getFileExportImport()
            val timestamp = de.egril.defender.utils.formatTimestampISO(de.egril.defender.utils.currentTimeMillis())
            fileExportImport.exportFile("game-progress-$timestamp.json", jsonContent)
        }
    }
    
    /**
     * Upload save files and handle override conflicts
     * Returns a state flow with import results: (success count, conflicts)
     */
    suspend fun uploadSaveGames(): Pair<Int, List<String>> {
        val fileExportImport = de.egril.defender.save.getFileExportImport()
        val importedFiles = fileExportImport.importFiles() ?: return Pair(0, emptyList())
        
        var successCount = 0
        val conflicts = mutableListOf<String>()
        
        importedFiles.forEach { file ->
            if (de.egril.defender.save.SaveFileStorage.saveGameExists(file.filename)) {
                conflicts.add(file.filename)
            } else {
                if (de.egril.defender.save.SaveFileStorage.importSaveGame(file.filename, file.content, overwrite = false)) {
                    successCount++
                }
            }
        }
        
        refreshSavedGames()
        return Pair(successCount, conflicts)
    }
    
    /**
     * Import a specific file with override option
     */
    suspend fun importSaveGameWithOverride(filename: String, content: String, overwrite: Boolean): Boolean {
        val success = de.egril.defender.save.SaveFileStorage.importSaveGame(filename, content, overwrite)
        if (success) {
            refreshSavedGames()
        }
        return success
    }
    
    /** Public wrapper so App.kt can trigger a refresh when the IAM state changes (e.g. user logs in). */
    fun onAuthStateChanged() {
        // When the user logs in, link their Keycloak username to the current player profile
        // if no remote username is stored yet for this profile
        val iamState = de.egril.defender.iam.IamService.state.value
        if (iamState.isAuthenticated) {
            val username = iamState.username
            val player = _currentPlayer.value
            if (username != null && player != null && player.remoteUsername == null) {
                de.egril.defender.save.PlayerProfileStorage.linkRemoteUser(player.id, username)
                _currentPlayer.value = player.copy(remoteUsername = username)
                // On first SSO login, use the account's first name as the local player name
                val firstName = iamState.firstName
                if (!firstName.isNullOrBlank() && firstName.length <= 50) {
                    renameCurrentPlayer(firstName)
                }
            }
            // Download and merge remote user data (abilities, level progress) on login
            downloadAndMergeUserData()
            // Download and apply remote settings independently (dedicated player_settings table)
            downloadAndApplySettings()
        }
        viewModelScope.launch { refreshSavedGames() }
    }

    /**
     * Persists the "always log in" preference for the current player profile.
     * This is a per-player setting: each local player can independently opt-in to
     * automatic Keycloak login whenever they are the active player.
     */
    fun setAlwaysLogin(value: Boolean) {
        val player = _currentPlayer.value ?: return
        de.egril.defender.save.PlayerProfileStorage.saveAlwaysLogin(player.id, value)
        _currentPlayer.value = player.copy(alwaysLogin = value)
    }

    private fun refreshSavedGames() {
        val localGames = de.egril.defender.save.SaveFileStorage.getAllSavedGames()
        _savedGames.value = localGames
        // Asynchronously enrich with remote save information
        viewModelScope.launch {
            val token = de.egril.defender.iam.IamService.getToken() ?: return@launch
            _isLoadingRemoteSaves.value = true
            try {
                val remoteFiles = de.egril.defender.save.BackendSaveService.fetchSavefiles(token)
                    ?: return@launch
                // Cache all remote file data so loadGame() can import remote-only saves on demand
                remoteFilesCache.clear()
                for (remote in remoteFiles) {
                    remoteFilesCache[remote.saveId] = remote.data
                }
                val localIds = localGames.map { it.id }.toSet()
                // Build a map of remote saves by saveId for timestamp comparison
                val remoteById = remoteFiles.associateBy { it.saveId }
                // Build merged list: union of local and remote saves
                val merged = mutableListOf<de.egril.defender.save.SaveGameMetadata>()
                // For each local save, check if a newer version exists remotely
                for (local in localGames) {
                    val remote = remoteById[local.id]
                    if (remote != null) {
                        // Both local and remote exist for this save ID – prefer the newer one
                        val remoteGame = try {
                            de.egril.defender.save.SaveJsonSerializer.deserializeSavedGame(remote.data)
                        } catch (e: Exception) {
                            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                                println("Failed to parse remote savefile ${remote.saveId}: ${e.message}")
                            }
                            null
                        }
                        if (remoteGame != null && remoteGame.timestamp > local.timestamp) {
                            // Remote version is newer – overwrite local save and use remote metadata
                            de.egril.defender.save.SaveFileStorage.importSaveGame(
                                filename = "${remote.saveId}.json",
                                jsonContent = remote.data,
                                overwrite = true
                            )
                            val metadata = de.egril.defender.save.SaveFileStorage
                                .buildMetadataFromSavedGame(remoteGame)
                            merged.add(metadata.copy(isLocal = true, isRemote = true))
                        } else {
                            merged.add(local.copy(isLocal = true, isRemote = true))
                        }
                    } else {
                        merged.add(local.copy(isLocal = true, isRemote = false))
                    }
                }
                // Add remote-only saves (not present locally) using metadata from remote JSON
                for (remote in remoteFiles) {
                    if (remote.saveId !in localIds) {
                        val savedGame = try {
                            de.egril.defender.save.SaveJsonSerializer.deserializeSavedGame(remote.data)
                        } catch (e: Exception) {
                            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                                println("Failed to parse remote savefile ${remote.saveId}: ${e.message}")
                            }
                            null
                        }
                        if (savedGame != null) {
                            val metadata = de.egril.defender.save.SaveFileStorage
                                .buildMetadataFromSavedGame(savedGame)
                            merged.add(metadata.copy(isLocal = false, isRemote = true))
                        }
                    }
                }
                // Sort by timestamp descending (newest first)
                merged.sortByDescending { it.timestamp }
                _savedGames.value = merged
            } catch (e: Exception) {
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Failed to fetch remote savefiles: ${e.message}")
                }
            } finally {
                _isLoadingRemoteSaves.value = false
            }
        }
    }
    
    private fun saveWorldMapStatus() {
        de.egril.defender.save.SaveFileStorage.saveWorldMapStatus(_worldLevels.value)
    }

    /**
     * Uploads the current player's general user data (abilities, level progress, local username)
     * to the backend if the user is currently logged in.
     * Runs in the background so it never blocks gameplay.
     */
    private fun uploadUserDataToBackend() {
        val token = de.egril.defender.iam.IamService.getToken()
        if (token == null) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Skipping userdata upload: not authenticated")
            }
            return
        }
        val player = _currentPlayer.value ?: return
        val levelProgress = _worldLevels.value
            .filter { it.level.editorLevelId != null }
            .associate { it.level.editorLevelId!! to it.status.name }

        viewModelScope.launch {
            try {
                val jsonData = de.egril.defender.save.serializeUserDataJson(
                    localUsername = player.name,
                    abilities = player.abilities,
                    levelProgress = levelProgress
                )
                val success = de.egril.defender.save.BackendUserDataService.uploadUserData(jsonData, token)
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Userdata upload ${if (success) "succeeded" else "failed"} for player ${player.name}")
                }
            } catch (e: Exception) {
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Failed to upload userdata to backend: ${e.message}")
                }
            }
        }
    }

    /**
     * Uploads the current player's settings to the backend independently of userdata.
     * Settings are stored in a dedicated `player_settings` table on the server.
     */
    private fun uploadSettingsToBackend() {
        val token = de.egril.defender.iam.IamService.getToken() ?: return
        viewModelScope.launch {
            try {
                val settingsJson = de.egril.defender.save.serializeSettingsJson(
                    de.egril.defender.ui.settings.AppSettings.toSettingsMap()
                )
                val success = de.egril.defender.save.BackendSettingsService.uploadSettings(settingsJson, token)
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Settings upload ${if (success) "succeeded" else "failed"}")
                }
            } catch (e: Exception) {
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Failed to upload settings to backend: ${e.message}")
                }
            }
        }
    }

    /**
     * Downloads the user's general data from the backend and merges it with the current local
     * state. Remote data wins only when it contains higher XP than the local profile or when
     * a level has a higher status remotely (UNLOCKED or WON) than locally.
     */
    private fun downloadAndMergeUserData() {
        val token = de.egril.defender.iam.IamService.getToken() ?: return
        viewModelScope.launch {
            try {
                val remote = de.egril.defender.save.BackendUserDataService.fetchUserData(token) ?: return@launch
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Downloaded userdata for ${remote.localUsername}")
                }

                var playerUpdated = false

                // Merge abilities: prefer remote when it has higher XP, or when XP is equal but
                // the ability distribution differs (spending points doesn't change XP, so the
                // remote is more recent in that case – last writer wins).
                val player = _currentPlayer.value ?: return@launch
                val remoteAbilities = remote.abilities
                if (remoteAbilities != null &&
                    (remoteAbilities.totalXP > player.abilities.totalXP ||
                     (remoteAbilities.totalXP == player.abilities.totalXP && remoteAbilities != player.abilities))) {
                    val updatedPlayer = player.copy(abilities = remoteAbilities)
                    _currentPlayer.value = updatedPlayer
                    de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
                    playerUpdated = true
                    if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                        println("Merged remote abilities: totalXP=${remoteAbilities.totalXP}")
                    }
                }

                // Merge level progress: apply any levels that are WON/UNLOCKED remotely
                // but not yet at that status locally
                val remoteLevelProgress = remote.levelProgress
                if (remoteLevelProgress != null && remoteLevelProgress.isNotEmpty()) {
                    val updatedLevels = _worldLevels.value.toMutableList()
                    var levelsChanged = false
                    for (i in updatedLevels.indices) {
                        val wl = updatedLevels[i]
                        val editorId = wl.level.editorLevelId ?: continue
                        val remoteStatusStr = remoteLevelProgress[editorId] ?: continue
                        val remoteStatus = try {
                            de.egril.defender.model.LevelStatus.valueOf(remoteStatusStr)
                        } catch (_: Exception) { continue }
                        // Only upgrade the status (LOCKED → UNLOCKED → WON), never downgrade
                        if (remoteStatus.ordinal > wl.status.ordinal) {
                            updatedLevels[i] = wl.copy(status = remoteStatus)
                            levelsChanged = true
                        }
                    }
                    if (levelsChanged) {
                        _worldLevels.value = updatedLevels
                        de.egril.defender.save.SaveFileStorage.saveWorldMapStatus(updatedLevels)
                        if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                            println("Merged remote level progress into local world map")
                        }
                    }
                }
            } catch (e: Exception) {
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Failed to download/merge userdata: ${e.message}")
                }
            }
        }
    }

    /**
     * Downloads and applies the player's settings from the backend.
     * Only applied when the current player has [useRemoteSettings] set to true.
     * Settings are fetched from the dedicated `player_settings` table, independent of userdata.
     */
    private fun downloadAndApplySettings() {
        val token = de.egril.defender.iam.IamService.getToken() ?: return
        val player = _currentPlayer.value ?: return
        if (!player.useRemoteSettings) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Skipping remote settings download: useRemoteSettings=false for player ${player.name}")
            }
            return
        }
        viewModelScope.launch {
            try {
                val remoteSettings = de.egril.defender.save.BackendSettingsService.fetchSettings(token)
                if (remoteSettings != null && remoteSettings.isNotEmpty()) {
                    de.egril.defender.ui.settings.AppSettings.applyFromSettingsMap(remoteSettings)
                    if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                        println("Applied remote settings for player ${player.name}")
                    }
                }
            } catch (e: Exception) {
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Failed to download/apply settings: ${e.message}")
                }
            }
        }
    }

    /**
     * Persists the "use remote settings" preference for the current player profile.
     */
    fun setUseRemoteSettings(value: Boolean) {
        val player = _currentPlayer.value ?: return
        de.egril.defender.save.PlayerProfileStorage.saveUseRemoteSettings(player.id, value)
        _currentPlayer.value = player.copy(useRemoteSettings = value)
    }
    
    // Player Profile Management

    /**
     * Create a default player profile without requiring the user to enter a name.
     * Uses "Player" as the base name, appending a counter if that name is already taken.
     * Called automatically on first launch so the player can start playing immediately.
     */
    fun createDefaultPlayer() {
        if (createPlayer("Player")) return
        var counter = 2
        while (!createPlayer("Player $counter")) {
            counter++
            if (counter > MAX_PLAYER_NAME_COUNTER) break
        }
    }

    /**
     * Create a new player profile
     * @return true if successful, false if name is invalid or already exists
     */
    fun createPlayer(name: String): Boolean {
        val profile = de.egril.defender.save.PlayerProfileStorage.createProfile(name)
        if (profile != null) {
            _currentPlayer.value = profile
            _allPlayers.value = de.egril.defender.save.PlayerProfileStorage.getAllProfiles().profiles
            de.egril.defender.save.SaveFileStorage.setCurrentPlayer(profile.id)
            _needsPlayerSelection.value = false
            
            // Reload world map for new player
            initializeWorldMap()
            
            return true
        }
        return false
    }
    
    /**
     * Switch to a different player profile
     */
    fun switchPlayer(playerId: String) {
        val profile = de.egril.defender.save.PlayerProfileStorage.getProfile(playerId)
        if (profile != null) {
            _currentPlayer.value = profile
            de.egril.defender.save.SaveFileStorage.setCurrentPlayer(playerId)
            de.egril.defender.save.PlayerProfileStorage.updateLastPlayed(playerId)
            
            // Reload world map for new player
            initializeWorldMap()
            
            // Reload saved games list
            refreshSavedGames()
        }
    }
    
    /**
     * Delete a player profile
     */
    fun deletePlayer(playerId: String): Boolean {
        // Don't allow deleting the current player
        if (_currentPlayer.value?.id == playerId) {
            return false
        }
        
        val success = de.egril.defender.save.PlayerProfileStorage.deleteProfile(playerId)
        if (success) {
            _allPlayers.value = de.egril.defender.save.PlayerProfileStorage.getAllProfiles().profiles
        }
        return success
    }
    
    /**
     * Rename the current player profile
     * @return true if successful, false if name is invalid or already exists
     */
    fun renameCurrentPlayer(newName: String): Boolean {
        val currentPlayerId = _currentPlayer.value?.id ?: return false
        val updatedProfile = de.egril.defender.save.PlayerProfileStorage.renameProfile(currentPlayerId, newName)
        if (updatedProfile != null) {
            _currentPlayer.value = updatedProfile
            _allPlayers.value = de.egril.defender.save.PlayerProfileStorage.getAllProfiles().profiles
            
            // Update SaveFileStorage if the ID changed
            if (updatedProfile.id != currentPlayerId) {
                de.egril.defender.save.SaveFileStorage.setCurrentPlayer(updatedProfile.id)
            }
            
            // Sync the new local username to the backend
            uploadUserDataToBackend()
            return true
        }
        return false
    }
    
    /**
     * Refresh the list of all player profiles
     */
    fun refreshPlayerProfiles() {
        _allPlayers.value = de.egril.defender.save.PlayerProfileStorage.getAllProfiles().profiles
    }
    
    /**
     * Refresh the current player profile (e.g., after earning an achievement)
     */
    private fun refreshCurrentPlayer() {
        val currentPlayerId = _currentPlayer.value?.id ?: return
        val updatedProfile = de.egril.defender.save.PlayerProfileStorage.getProfile(currentPlayerId)
        if (updatedProfile != null) {
            _currentPlayer.value = updatedProfile
        }
    }
    
    /**
     * Clear the new achievement notification
     */
    fun clearAchievementNotification() {
        _newAchievement.value = null
    }

    /**
     * Checks GitHub releases in the background for a version newer than the running build.
     * Updates [newVersionAvailable] when a newer version is found.
     * Should be called once at app start-up; safe to call multiple times (only the first
     * non-null result is stored).
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            val info = checkForNewerVersion()
            if (info != null) {
                _newVersionAvailable.value = info
            }
        }
    }

    /** Dismisses the new-version notification banner/dialog. */
    fun dismissNewVersionNotification() {
        _newVersionAvailable.value = null
    }


    fun startTimeTracking() {
        val currentTime = de.egril.defender.utils.currentTimeMillis()
        gameSessionStartTime = currentTime
        lastBreakReminderTime = currentTime
        lastSleepReminderTime = currentTime
        
        // Start coroutine to check for reminders
        viewModelScope.launch {
            checkTimeReminders()
        }
    }
    
    /**
     * Stop tracking time when leaving the game screen
     */
    fun stopTimeTracking() {
        gameSessionStartTime = null
        lastBreakReminderTime = null
        lastSleepReminderTime = null
    }
    
    /**
     * Check and show time reminders periodically
     */
    private suspend fun checkTimeReminders() {
        while (gameSessionStartTime != null) {
            delay(60000L)  // Check every minute
            
            val currentTime = de.egril.defender.utils.currentTimeMillis()
            
            // Check break reminder (every 2 hours)
            lastBreakReminderTime?.let { lastBreak ->
                if (currentTime - lastBreak >= BREAK_REMINDER_INTERVAL_MS) {
                    gameSessionStartTime?.let { sessionStart ->
                        val elapsedMs = currentTime - sessionStart
                        val elapsedTime = formatElapsedTime(elapsedMs)
                        _reminderMessage.value = ReminderMessage(
                            type = de.egril.defender.ui.gameplay.ReminderType.BREAK,
                            elapsedTime = elapsedTime
                        )
                        lastBreakReminderTime = currentTime
                    }
                }
            }
            
            // Check sleep reminder (after 23:00, every hour)
            lastSleepReminderTime?.let { lastSleep ->
                if (currentTime - lastSleep >= SLEEP_REMINDER_INTERVAL_MS) {
                    val hour = getLocalHour(currentTime)
                    if (hour >= SLEEP_START_HOUR || hour < 6) {  // Between 23:00 and 06:00
                        val timeDescription = when {
                            hour == 23 -> "close_to_midnight"
                            hour == 0 -> "midnight"
                            else -> "after_midnight"
                        }
                        _reminderMessage.value = ReminderMessage(
                            type = de.egril.defender.ui.gameplay.ReminderType.SLEEP,
                            timeDescription = timeDescription
                        )
                        lastSleepReminderTime = currentTime
                    }
                }
            }
        }
    }
    
    /**
     * Clear the current reminder message
     */
    fun clearReminderMessage() {
        _reminderMessage.value = null
    }

    /**
     * Dismiss the current game message and surface the next one (if any).
     */
    fun dismissGameMessage() {
        val state = _gameState.value
        if (state != null && state.pendingMessages.isNotEmpty()) {
            val next = state.pendingMessages.removeAt(0)
            _pendingGameMessage.value = next
        } else {
            _pendingGameMessage.value = null
        }
    }

    /**
     * After a trap fires and its animation completes, process any newly-defeated attackers so
     * their death animation plays immediately after the trap animation (rather than being deferred
     * to completeEnemyTurn). Adds a further delay equal to [ENEMY_DEATH_ANIMATION_DELAY_MS] so
     * the death animation finishes before the next movement step begins.
     */
    private suspend fun processAndDelayForTrapDeaths() {
        val deathCountBefore = _gameState.value?.defeatedEnemyEffects?.size ?: 0
        gameEngine?.processDefeatedAttackers()
        if ((_gameState.value?.defeatedEnemyEffects?.size ?: 0) > deathCountBefore) {
            delay(ENEMY_DEATH_ANIMATION_DELAY_MS)
        }
    }

    /**
     * Surface the next pending game message if no message is currently being shown.
     * Does nothing when a message is already visible so that the queue is not skipped.
     */
    private fun surfaceNextPendingMessageIfIdle() {
        val state = _gameState.value ?: return
        if (state.pendingMessages.isNotEmpty() && _pendingGameMessage.value == null) {
            val nextMessage = state.pendingMessages.removeAt(0)
            _pendingGameMessage.value = nextMessage
        }
    }

    /**
     * Format elapsed time as "X hours Y minutes"
     */
    private fun formatElapsedTime(elapsedMs: Long): String {
        val hours = elapsedMs / (60 * 60 * 1000)
        val minutes = (elapsedMs % (60 * 60 * 1000)) / (60 * 1000)
        val locale = currentLanguage.value

        return buildString {
            if (hours > 0) {
                val key = if (hours == 1L) "hour" else "hours"
                append(LocalizedStrings.get(key, locale).replace("%d", hours.toString()))
            }
            if (minutes > 0) {
                if (hours > 0) append(" ")
                val key = if (minutes == 1L) "minute" else "minutes"
                append(LocalizedStrings.get(key, locale).replace("%d", minutes.toString()))
            }
        }
    }
    
    /**
     * Get the local hour (0-23) from timestamp
     */
    private fun getLocalHour(timestamp: Long): Int {
        return de.egril.defender.utils.getLocalHour(timestamp)
    }

    /**
     * Toggle magic panel display
     */
    fun toggleMagicPanel() {
        _showMagicPanel.value = !_showMagicPanel.value
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Magic panel toggled - now ${if (_showMagicPanel.value) "OPEN" else "CLOSED"}")
        }
    }

    /**
     * Open magic panel
     */
    fun openMagicPanel() {
        _showMagicPanel.value = true
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            val gameState = _gameState.value
            println("=== SPELL: Magic panel OPENED - Current mana: ${gameState?.currentMana?.value}/${gameState?.maxMana?.value}")
            val playerStats = _currentPlayer.value?.abilities
            val unlockedSpells = playerStats?.unlockedSpells?.size ?: 0
            println("=== SPELL: Player has $unlockedSpells unlocked spell(s)")
        }
    }

    /**
     * Close magic panel
     */
    fun closeMagicPanel() {
        _showMagicPanel.value = false
        _pendingSpellCast.value = null
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Magic panel CLOSED")
        }
    }

    /**
     * Cancel the active Instant Tower spell and return mana (mana was never consumed).
     * Called when the player chooses to abort from the abort dialog.
     */
    fun cancelInstantTowerSpell() {
        val state = _gameState.value ?: return
        if (state.instantTowerSpellActive.value) {
            state.instantTowerSpellActive.value = false
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Instant Tower spell cancelled by player (no mana consumed)")
            }
        }
    }

    /**
     * Clear pending scroll to position (called after UI has consumed it)
     */
    fun clearPendingScrollPosition() {
        _pendingScrollToPosition.value = null
    }

    /**
     * Toggle spell selection (select or deselect)
     * No confirmation dialog - enters targeting mode directly
     */
    fun setPendingSpell(spell: SpellType) {
        val gameState = _gameState.value

        // INSTANT_TOWER: toggle the instant deploy mode directly without confirmation dialog.
        // Mana is deferred and only consumed when a tower is actually placed.
        if (spell == SpellType.INSTANT_TOWER) {
            if (gameState == null || gameState.currentMana.value < spell.manaCost) {
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: Cannot cast ${spell.displayName} - Insufficient mana (Need: ${spell.manaCost}, Have: ${gameState?.currentMana?.value ?: 0})")
                }
                return
            }
            if (gameState.instantTowerSpellActive.value) {
                // Already active - cancel the spell mode (mana was never consumed)
                gameState.instantTowerSpellActive.value = false
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: Instant Tower spell cancelled (no mana consumed)")
                }
            } else {
                // Activate instant tower mode (mana deferred until tower is placed)
                gameState.instantTowerSpellActive.value = true
                closeMagicPanel()
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: Instant Tower spell activated - next tower will be built instantly")
                }
            }
            return
        }

        if (gameState != null && gameState.currentMana.value >= spell.manaCost) {
            // Toggle selection: if same spell clicked again, deselect it
            if (_selectedSpell.value == spell) {
                _selectedSpell.value = null
                _pendingSpellCast.value = null
                exitSpellTargetingMode()
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: Spell deselected - ${spell.displayName}")
                }
            } else {
                _selectedSpell.value = spell
                _pendingSpellCast.value = spell
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: Spell selected - ${spell.displayName} (Cost: ${spell.manaCost} mana)")
                }
                // For targeting spells, enter targeting mode immediately
                if (spell.requiresTarget) {
                    enterSpellTargetingMode(spell)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                        println("=== SPELL: Entered targeting mode for ${spell.displayName}")
                    }
                } else {
                    // For non-targeting spells, show confirmation with spell details
                    _showSpellTargetConfirmation.value = Pair(spell, Unit)
                }
            }
        } else {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Cannot cast ${spell.displayName} - Insufficient mana (Need: ${spell.manaCost}, Have: ${gameState?.currentMana?.value ?: 0})")
            }
        }
    }

    /**
     * Cancel pending spell cast
     */
    fun cancelPendingSpell() {
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            val spell = _pendingSpellCast.value
            if (spell != null) {
                println("=== SPELL: Spell cast CANCELLED - ${spell.displayName}")
            }
        }
        _pendingSpellCast.value = null
        _selectedSpell.value = null
    }

    /**
     * Handle target selection for spell (called when player clicks a target)
     * Shows confirmation or warning dialog based on target validity
     */
    fun onSpellTargetSelected(target: Any) {
        val gameState = _gameState.value ?: return
        // Use activeSpell from targeting state (pendingSpellCast was cleared when targeting mode was entered)
        val spell = gameState.spellTargeting.value?.activeSpell ?: return

        // Check for freeze immunity
        if (spell == SpellType.FREEZE_SPELL && target is Attacker) {
            if (isImmuneToFreeze(target.type)) {
                _showFreezeImmuneWarning.value = target
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: ${target.type.displayName} is immune to Freeze!")
                }
                return
            }
        }

        // Show confirmation dialog with target details
        _showSpellTargetConfirmation.value = Pair(spell, target)
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Target selected for ${spell.displayName}")
        }
    }

    /**
     * Confirm spell cast after target selection
     */
    fun confirmSpellCast() {
        val confirmation = _showSpellTargetConfirmation.value
        if (confirmation != null) {
            val (spell, target) = confirmation
            castSpell(spell, target)
            _showSpellTargetConfirmation.value = null
            _selectedSpell.value = null
        }
    }

    /**
     * Dismiss spell target confirmation dialog
     */
    fun dismissSpellConfirmation() {
        _showSpellTargetConfirmation.value = null
        // Keep spell selected and targeting mode active
    }

    /**
     * Dismiss freeze immune warning
     */
    fun dismissFreezeImmuneWarning() {
        _showFreezeImmuneWarning.value = null
        // Keep spell selected and targeting mode active
    }

    /**
     * Check if an enemy type is immune to freeze spell
     */
    private fun isImmuneToFreeze(type: AttackerType): Boolean {
        return type == AttackerType.BLUE_DEMON ||
               type == AttackerType.RED_DEMON ||
               type == AttackerType.DRAGON ||
               type == AttackerType.EWHAD
    }

    /**
     * Cast a spell (called after targeting is complete)
     * For non-targeting spells, this is called immediately after confirmation
     */
    fun castSpell(spell: SpellType, target: Any? = null) {
        val gameState = _gameState.value ?: return
        val currentPlayer = _currentPlayer.value ?: return

        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Cast spell called - ${spell.displayName}")
            println("=== SPELL: Current mana: ${gameState.currentMana.value}/${gameState.maxMana.value}")
        }

        // Validate mana cost
        if (gameState.currentMana.value < spell.manaCost) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: FAILED - Not enough mana to cast ${spell.displayName}")
            }
            return
        }

        // Validate spell is unlocked
        if (!currentPlayer.abilities.unlockedSpells.contains(spell)) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: FAILED - Spell ${spell.displayName} is not unlocked")
            }
            return
        }

        // If spell requires targeting and no target provided, enter targeting mode
        if (spell.requiresTarget && target == null) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Spell requires targeting - Entering targeting mode")
            }
            enterSpellTargetingMode(spell)
            return
        }

        // Deduct mana cost
        val previousMana = gameState.currentMana.value
        gameState.currentMana.value -= spell.manaCost
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Mana deducted - ${previousMana} -> ${gameState.currentMana.value}")
        }

        // Execute spell effect
        executeSpellEffect(spell, target, gameState)

        // Process any enemies defeated by the spell (award coins, remove from list)
        if (spell == SpellType.ATTACK_AIMED || spell == SpellType.ATTACK_AREA) {
            gameEngine?.processDefeatedAttackers()
            // Surface any messages queued by the kill (e.g. EWHAD_RETREATS/EWHAD_DEFEATED) immediately.
            surfaceNextPendingMessageIfIdle()
            // Check if the spell killed the last enemy and the level is now won
            val currentStateAfterSpell = _gameState.value
            if (currentStateAfterSpell != null && currentStateAfterSpell.isLevelWon()) {
                completeLevel(currentStateAfterSpell.level.id, won = true)
            }
        }

        // Clear pending spell and targeting state
        _pendingSpellCast.value = null
        exitSpellTargetingMode()

        // Close magic panel after casting
        closeMagicPanel()

        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Spell cast COMPLETE - ${spell.displayName}")
        }
    }

    /**
     * Execute the effect of a cast spell
     */
    private fun executeSpellEffect(spell: SpellType, target: Any?, gameState: GameState) {
        when (spell) {
            SpellType.ATTACK_AIMED -> {
                // Attack Aimed: Deal 80 damage to the enemy on a targeted tile
                val position = target as? Position
                if (position != null) {
                    val attacker = gameState.attackers.find { !it.isDefeated.value && it.position.value == position }
                    if (attacker != null) {
                        attacker.currentHealth.value -= 80
                        if (attacker.currentHealth.value <= 0) {
                            attacker.currentHealth.value = 0
                            attacker.isDefeated.value = true
                        }
                        if (LogConfig.ENABLE_SPELL_LOGGING) {
                        println("Attack Aimed: Dealt 80 damage to ${attacker.type.displayName} at $position (HP: ${attacker.currentHealth.value})")
                        }
                    }
                }
            }
            SpellType.HEAL -> {
                // Heal: Restore 3 health points (cap at max)
                val currentHP = gameState.healthPoints.value
                val maxHP = gameState.level.healthPoints + (_currentPlayer.value?.abilities?.healthAbility ?: 0)
                val newHP = (currentHP + 3).coerceAtMost(maxHP)
                gameState.healthPoints.value = newHP
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("Heal: Restored health to $newHP/$maxHP")
                }
            }
            SpellType.ATTACK_AREA -> {
                // Attack Area: Deal 50 damage to all enemies within 2 hex range of position
                val position = target as? Position
                if (position != null) {
                    var damagedCount = 0
                    gameState.attackers.filter { !it.isDefeated.value }.forEach { attacker ->
                        val distance = attacker.position.value.hexDistanceTo(position)
                        if (distance <= 2) {
                            attacker.currentHealth.value -= 50
                            if (attacker.currentHealth.value <= 0) {
                                attacker.currentHealth.value = 0
                                attacker.isDefeated.value = true
                            }
                            damagedCount++
                        }
                    }
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Attack Area: Dealt 50 damage to $damagedCount enemies within 2 hex range of $position")
                    }
                }
            }
            SpellType.INSTANT_TOWER -> {
                // Instant Tower is handled via instantTowerSpellActive mode in GameState.
                // Mana is deferred and only consumed when a tower is placed (see placeDefender).
                // This fallback path activates the mode if called directly (e.g., from cheat codes).
                println("=== SPELL: WARN - Instant Tower executeSpellEffect fallback triggered; activating mode")
                gameState.instantTowerSpellActive.value = true
            }
            SpellType.DOUBLE_TOWER_LEVEL -> {
                // Double Tower Level: Double tower level for 1 turn
                val defender = target as? Defender
                if (defender != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.DOUBLE_TOWER_LEVEL,
                        defenderId = defender.id,
                        turnsRemaining = 1,
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Double Tower Level: ${defender.type.displayName} level doubled for 1 turn!")
                    }
                }
            }
            SpellType.DOUBLE_TOWER_REACH -> {
                // Double Tower Reach: Double tower range for 1 turn
                val defender = target as? Defender
                if (defender != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.DOUBLE_TOWER_REACH,
                        defenderId = defender.id,
                        turnsRemaining = 1,
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Double Tower Reach: ${defender.type.displayName} range doubled for 1 turn!")
                    }
                }
            }
            SpellType.BOMB -> {
                // Bomb: Place bomb at position, explodes after 2 turns
                val position = target as? Position
                if (position != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.BOMB,
                        position = position,
                        turnsRemaining = 3,  // Explodes at the start of turn 3 (2 full turns + explosion)
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    // Play ticking sound when bomb is placed
                    GlobalSoundManager.playSound(SoundEvent.BOMB_TICKING)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Bomb: Placed at $position, will explode in 2 turns!")
                    }
                }
            }
            SpellType.FREEZE_SPELL -> {
                // Freeze: Freeze enemy for 1+ turns (base 1 turn for 10 mana)
                val attacker = target as? Attacker
                if (attacker != null) {
                    // Check immunity: Does not work on Demons, Dragons, Ewhad
                    val isImmune = attacker.type.isDragon ||
                                   attacker.type == AttackerType.BLUE_DEMON ||
                                   attacker.type == AttackerType.RED_DEMON ||
                                   attacker.type == AttackerType.EWHAD

                    if (isImmune) {
                        println("Freeze Spell: ${attacker.type.displayName} is immune to freeze!")
                    } else {
                        // For now, freeze for 1 turn (base cost)
                        // TODO: Add duration selection dialog for spending more mana
                        val effect = ActiveSpellEffect(
                            spell = SpellType.FREEZE_SPELL,
                            attackerId = attacker.id,
                            turnsRemaining = 1,
                            castTurn = gameState.turnNumber.value
                        )
                        gameState.activeSpellEffects.add(effect)
                        if (LogConfig.ENABLE_SPELL_LOGGING) {
                        println("Freeze Spell: Froze ${attacker.type.displayName} for 1 turn!")
                        }
                    }
                }
            }
            SpellType.COOLING_SPELL -> {
                // Cooling Spell: Create area that slows enemies for 3 turns
                val position = target as? Position
                if (position != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.COOLING_SPELL,
                        position = position,
                        turnsRemaining = 3,
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Cooling Spell: Created cooling area at $position for 3 turns!")
                    }
                }
            }
            SpellType.FEAR_SPELL -> {
                // Fear Spell: Single target enemy flees towards spawn for 3 turns
                val attacker = target as? Attacker
                if (attacker != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.FEAR_SPELL,
                        attackerId = attacker.id,
                        turnsRemaining = 3,
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Fear Spell: ${attacker.type.displayName} flees for 3 turns!")
                    }
                }
            }
            SpellType.FEAR_SPELL_AREA -> {
                // Fear Spell (Area): Create fear zone that makes enemies flee for 3 turns
                val position = target as? Position
                if (position != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.FEAR_SPELL_AREA,
                        position = position,
                        turnsRemaining = 3,
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Fear Spell (Area): Created fear zone at $position for 3 turns!")
                    }
                }
            }
        }
    }

    /**
     * Enter spell targeting mode
     */
    fun enterSpellTargetingMode(spell: SpellType) {
        val gameState = _gameState.value ?: return

        // Calculate valid targets based on spell type
        val validTargets: Set<Any> = when (spell.targetType) {
            SpellTargetType.POSITION -> {
                if (spell == SpellType.BOMB) {
                    // Bomb can only be placed on empty path tiles:
                    // no enemies, no barricades, no field effects, no traps, no other bombs
                    val occupiedByEnemy = gameState.attackers
                        .filter { !it.isDefeated.value }
                        .map { it.position.value }
                        .toSet()
                    val occupiedByBarricade = gameState.barricades.map { it.position }.toSet()
                    val occupiedByFieldEffect = gameState.fieldEffects.map { it.position }.toSet()
                    val occupiedByTrap = gameState.traps.map { it.position }.toSet()
                    val occupiedByBomb = gameState.activeSpellEffects
                        .filter { it.spell == SpellType.BOMB && it.position != null }
                        .map { it.position!! }
                        .toSet()
                    val blocked = occupiedByEnemy + occupiedByBarricade + occupiedByFieldEffect +
                            occupiedByTrap + occupiedByBomb
                    val positions = mutableSetOf<Position>()
                    for (x in 0 until gameState.level.gridWidth) {
                        for (y in 0 until gameState.level.gridHeight) {
                            val pos = Position(x, y)
                            if (gameState.level.isOnPath(pos) && pos !in blocked) {
                                positions.add(pos)
                            }
                        }
                    }
                    positions
                } else if (spell == SpellType.COOLING_SPELL) {
                    // Cooling spell can only be placed on path/spawn tiles without barricades
                    val occupiedByBarricade = gameState.barricades.map { it.position }.toSet()
                    val positions = mutableSetOf<Position>()
                    for (x in 0 until gameState.level.gridWidth) {
                        for (y in 0 until gameState.level.gridHeight) {
                            val pos = Position(x, y)
                            if (gameState.level.isEnemyTraversable(pos) &&
                                pos !in occupiedByBarricade) {
                                positions.add(pos)
                            }
                        }
                    }
                    positions
                } else if (spell == SpellType.ATTACK_AREA) {
                    // Attack Area: only path tiles without a barricade
                    val occupiedByBarricade = gameState.barricades
                        .filter { !it.isDestroyed() }
                        .map { it.position }
                        .toSet()
                    val positions = mutableSetOf<Position>()
                    for (x in 0 until gameState.level.gridWidth) {
                        for (y in 0 until gameState.level.gridHeight) {
                            val pos = Position(x, y)
                            if (gameState.level.isOnPath(pos) && pos !in occupiedByBarricade) {
                                positions.add(pos)
                            }
                        }
                    }
                    positions
                } else if (spell == SpellType.ATTACK_AIMED) {
                    // Attack Aimed: only tiles that have an enemy on them
                    gameState.attackers
                        .filter { !it.isDefeated.value }
                        .map { it.position.value }
                        .toSet()
                } else {
                    // All tiles on the map are valid positions for other spells
                    val positions = mutableSetOf<Position>()
                    for (x in 0 until gameState.level.gridWidth) {
                        for (y in 0 until gameState.level.gridHeight) {
                            positions.add(Position(x, y))
                        }
                    }
                    positions
                }
            }
            SpellTargetType.ENEMY -> {
                // All active (non-defeated) enemies are valid targets
                gameState.attackers.filter { !it.isDefeated.value }.toSet()
            }
            SpellTargetType.TOWER -> {
                // All placed defenders are valid targets
                gameState.defenders.toSet()
            }
            SpellTargetType.NONE -> emptySet()
        }

        // Set targeting state
        gameState.spellTargeting.value = SpellTargetingState(
            activeSpell = spell,
            validTargets = validTargets
        )

        // Clear pending spell cast (we're now in targeting mode)
        _pendingSpellCast.value = null
        // Close magic panel so the targeting instruction card becomes visible
        _showMagicPanel.value = false
    }

    /**
     * Exit spell targeting mode (cancel)
     */
    fun exitSpellTargetingMode() {
        val gameState = _gameState.value ?: return
        gameState.spellTargeting.value = null
    }

    /**
     * Select a target and cast the spell
     */
    fun selectSpellTarget(target: Any) {
        val gameState = _gameState.value ?: return
        val targeting = gameState.spellTargeting.value ?: return

        // Validate target is in valid targets set
        if (!targeting.validTargets.contains(target)) {
            println("Invalid target for spell")
            return
        }

        // Show confirmation dialog with target details (or warning for immune enemies)
        onSpellTargetSelected(target)
    }

    companion object {
        /** Maximum counter suffix tried when generating a unique default player name. */
        private const val MAX_PLAYER_NAME_COUNTER = 999
    }
}
