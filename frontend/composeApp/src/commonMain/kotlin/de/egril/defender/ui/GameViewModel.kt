package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.config.LogConfig
import de.egril.defender.editor.EditorJsonSerializer
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.InitialDefender
import de.egril.defender.game.GameEngine
import de.egril.defender.game.LevelData
import de.egril.defender.game.isProtectedByObsidianProtector
import de.egril.defender.game.isProtectedByRuneGlyphWard
import de.egril.defender.game.runeGlyphWardArmor
import de.egril.defender.model.*
import de.egril.defender.ui.animations.SKY_IS_FALLING_DURATION_MS
import de.egril.defender.ui.infopage.NewVersionInfo
import de.egril.defender.ui.infopage.checkForNewerVersions
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.DeepLink
import de.egril.defender.utils.checkCurrentDeepLink
import de.egril.defender.utils.isPlatformWasm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Represents the progress of loading repository data files (levels, maps, worldmap).
 * Used on WASM/web where files are loaded asynchronously on startup.
 */
data class LoadingProgress(
    val loadedCount: Int,
    val totalCount: Int,
    val currentFile: String,
)

sealed class Screen {
    object MainMenu : Screen()

    object WorldMap : Screen()

    object Rules : Screen()

    object InstallationInfo : Screen()

    data class InstallationInfoAtTab(
        val initialTab: de.egril.defender.ui.infopage.InfoTab,
    ) : Screen()

    object VillainsAnnouncement : Screen()

    data class LevelEditor(
        val openLevelId: String? = null,
    ) : Screen()

    object LoadGame : Screen()

    object Sticker : Screen()

    object PlayerProfile : Screen()

    object LoadingSpinnerDemo : Screen()

    object StatsUpgrade : Screen() // New screen for stats/spells upgrade (from PlayerProfile)

    data class StatsUpgradeWithNextLevel(
        val nextLevelId: Int,
        val nextLevelName: String,
    ) : Screen() // Stats/spells upgrade before continuing to next level

    object FinalCredits : Screen()

    object AnimationTest : Screen() // Developer cheat: animation test/preview screen

    object TutorialDeepLink : Screen() // Web-only: /tutorial URL shows loading while data/player are prepared

    object DemoDeepLink : Screen() // Web-only: /demo URL shows loading while data/player are prepared

    data class GamePlay(
        val levelId: Int,
    ) : Screen()

    data class LevelComplete(
        val levelId: Int,
        val won: Boolean,
        val isLastLevel: Boolean,
        val xpEarned: Int = 0,
        val newPlayerLevel: Int = 0,
        val playerLevelGained: Int = 0,
        val abilityPointsGained: Int = 0,
        val nextLevelId: Int? = null,
        val nextLevelName: String? = null,
    ) : Screen()
}

/**
 * Represents a conflict between saved world map progress and current world map progress
 */
data class WorldMapConflict(
    val savedGame: de.egril.defender.save.SavedGame?, // Null when importing just game state
    val savedWorldMap: de.egril.defender.save.WorldMapSave,
    val currentWorldMap: Map<String, LevelStatus>,
    val level: Level?, // Null when importing just game state (no level to load)
)

/**
 * Data class for reminder messages
 */
data class ReminderMessage(
    val type: de.egril.defender.ui.gameplay.ReminderType,
    val elapsedMs: Long? = null,
    val timeDescription: String? = null,
)

internal data class EditorPlaytestSession(
    val editorLevel: EditorLevel,
)

class GameViewModel {
    internal val _currentScreen = MutableStateFlow<Screen>(Screen.MainMenu)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    internal val _worldLevels = MutableStateFlow<List<WorldLevel>>(emptyList())
    val worldLevels: StateFlow<List<WorldLevel>> = _worldLevels.asStateFlow()

    internal val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    internal val _cheatDigOutcome = MutableStateFlow<DigOutcome?>(null)
    val cheatDigOutcome: StateFlow<DigOutcome?> = _cheatDigOutcome.asStateFlow()

    internal val _showPlatformInfo = MutableStateFlow(false)
    val showPlatformInfo: StateFlow<Boolean> = _showPlatformInfo.asStateFlow()

    internal val _showCheatHelp = MutableStateFlow(false)
    val showCheatHelp: StateFlow<Boolean> = _showCheatHelp.asStateFlow()

    // Data loading state – used on WASM/web where repository files are loaded asynchronously
    private val _isDataLoaded = MutableStateFlow(!isPlatformWasm)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded.asStateFlow()

    private val _loadingProgress = MutableStateFlow<LoadingProgress?>(null)
    val loadingProgress: StateFlow<LoadingProgress?> = _loadingProgress.asStateFlow()

    // Tutorial deep link state – set when the user visits /tutorial on the web version
    internal val _pendingTutorialDeepLink = MutableStateFlow(false)
    val pendingTutorialDeepLink: StateFlow<Boolean> = _pendingTutorialDeepLink.asStateFlow()

    // Demo deep link state – set when the user visits /demo on the web version
    internal val _pendingDemoDeepLink = MutableStateFlow(false)
    val pendingDemoDeepLink: StateFlow<Boolean> = _pendingDemoDeepLink.asStateFlow()

    // Settings deep link state – set when the user visits /settings on the web version
    internal val _pendingSettingsDeepLink = MutableStateFlow(false)
    val pendingSettingsDeepLink: StateFlow<Boolean> = _pendingSettingsDeepLink.asStateFlow()

    // Player profile state
    internal val _currentPlayer = MutableStateFlow<de.egril.defender.save.PlayerProfile?>(null)
    val currentPlayer: StateFlow<de.egril.defender.save.PlayerProfile?> = _currentPlayer.asStateFlow()

    internal val _allPlayers = MutableStateFlow<List<de.egril.defender.save.PlayerProfile>>(emptyList())
    val allPlayers: StateFlow<List<de.egril.defender.save.PlayerProfile>> = _allPlayers.asStateFlow()

    internal val _needsPlayerSelection = MutableStateFlow(false)
    val needsPlayerSelection: StateFlow<Boolean> = _needsPlayerSelection.asStateFlow()

    // World map progress conflict state
    internal val _worldMapConflict = MutableStateFlow<WorldMapConflict?>(null)
    val worldMapConflict: StateFlow<WorldMapConflict?> = _worldMapConflict.asStateFlow()

    // Level handoff state (connected levels) - shows dialog when starting a connected level
    internal val _pendingLevelHandoff = MutableStateFlow<de.egril.defender.save.LevelHandoffSave?>(null)
    val pendingLevelHandoff: StateFlow<de.egril.defender.save.LevelHandoffSave?> = _pendingLevelHandoff.asStateFlow()

    // The level ID to start when the handoff dialog is resolved
    internal var pendingLevelIdForHandoff: Int? = null

    // Special actions remaining after auto-attack
    internal val _specialActionsRemaining = MutableStateFlow<List<DefenderType>>(emptyList())
    val specialActionsRemaining: StateFlow<List<DefenderType>> = _specialActionsRemaining.asStateFlow()

    // Time reminders for breaks and sleep
    internal val _reminderMessage = MutableStateFlow<ReminderMessage?>(null)
    val reminderMessage: StateFlow<ReminderMessage?> = _reminderMessage.asStateFlow()

    // In-game event messages (target taken, gate destroyed)
    internal val _pendingGameMessage = MutableStateFlow<de.egril.defender.model.GameMessage?>(null)
    val pendingGameMessage: StateFlow<de.egril.defender.model.GameMessage?> = _pendingGameMessage.asStateFlow()

    // Achievement system
    internal var achievementManager: de.egril.defender.game.AchievementManager? = null
    internal val _newAchievement = MutableStateFlow<Achievement?>(null)
    val newAchievement: StateFlow<Achievement?> = _newAchievement.asStateFlow()

    // Magic panel state
    internal val _showMagicPanel = MutableStateFlow(false)
    val showMagicPanel: StateFlow<Boolean> = _showMagicPanel.asStateFlow()

    internal val _selectedSpell = MutableStateFlow<SpellType?>(null)
    val selectedSpell: StateFlow<SpellType?> = _selectedSpell.asStateFlow()

    internal val _pendingSpellCast = MutableStateFlow<SpellType?>(null)
    val pendingSpellCast: StateFlow<SpellType?> = _pendingSpellCast.asStateFlow()

    // When non-null, the spell currently being cast/targeted is being cast via a level support
    // token, which skips mana cost and the unlocked-spell requirement, and consumes a token.
    internal val _pendingTokenSpell = MutableStateFlow<SpellType?>(null)
    val pendingTokenSpell: StateFlow<SpellType?> = _pendingTokenSpell.asStateFlow()

    // Position to scroll to (e.g., bomb explosion)
    internal val _pendingScrollToPosition = MutableStateFlow<Position?>(null)
    val pendingScrollToPosition: StateFlow<Position?> = _pendingScrollToPosition.asStateFlow()

    // Post-target confirmation dialog state
    internal val _showSpellTargetConfirmation = MutableStateFlow<Pair<SpellType, Any>?>(null)
    val showSpellTargetConfirmation: StateFlow<Pair<SpellType, Any>?> = _showSpellTargetConfirmation.asStateFlow()

    internal val _showFreezeImmuneWarning = MutableStateFlow<Attacker?>(null)
    val showFreezeImmuneWarning: StateFlow<Attacker?> = _showFreezeImmuneWarning.asStateFlow()

    // Track game session time
    internal var gameSessionStartTime: Long? = null
    internal var lastBreakReminderTime: Long? = null
    internal var lastSleepReminderTime: Long? = null

    // Constants for reminder intervals
    internal val BREAK_REMINDER_INTERVAL_MS = 2 * 60 * 60 * 1000L // 2 hours
    internal val SLEEP_REMINDER_INTERVAL_MS = 60 * 60 * 1000L // 1 hour
    internal val SLEEP_START_HOUR = 23 // 23:00 (11 PM)

    // Extra pause added after a movement step that triggers a trap, so the trap animation (~830ms)
    // completes before the enemy continues moving.
    internal val TRAP_ANIMATION_EXTRA_DELAY_MS = 800L

    // Pause after a trap kill's death animation starts, so the enemy death animation (~1000ms)
    // finishes before the next movement step begins.
    internal val ENEMY_DEATH_ANIMATION_DELAY_MS = 1000L

    // Track initial game state to detect unsaved changes
    internal var initialGameStateSnapshot: String? = null
    internal var lastSaveSnapshot: String? = null

    internal var gameEngine: GameEngine? = null
    internal val viewModelScope = CoroutineScope(Dispatchers.Default)
    internal var editorPlaytestSession: EditorPlaytestSession? = null

    // Demo mode state
    internal val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()
    internal var demoLevelIndex = 0
    internal var demoJob: Job? = null
    internal var demoScenario = de.egril.defender.game.DemoMode.Scenario.STANDARD
    internal var demoInitialTowers: List<InitialDefender> = emptyList()

    // Demo visual state – drives placement preview and attack aiming circles in the UI
    internal val _demoSelectedDefenderType = MutableStateFlow<DefenderType?>(null)
    val demoSelectedDefenderType: StateFlow<DefenderType?> = _demoSelectedDefenderType.asStateFlow()
    internal val _demoHoveredPosition = MutableStateFlow<Position?>(null)
    val demoHoveredPosition: StateFlow<Position?> = _demoHoveredPosition.asStateFlow()
    internal val _demoSelectedDefenderId = MutableStateFlow<Int?>(null)
    val demoSelectedDefenderId: StateFlow<Int?> = _demoSelectedDefenderId.asStateFlow()
    internal val _demoSelectedTargetPosition = MutableStateFlow<Position?>(null)
    val demoSelectedTargetPosition: StateFlow<Position?> = _demoSelectedTargetPosition.asStateFlow()

    // New version availability check
    internal val _newVersionAvailable = MutableStateFlow<List<NewVersionInfo>>(emptyList())
    val newVersionAvailable: StateFlow<List<NewVersionInfo>> = _newVersionAvailable.asStateFlow()

    // Remote community level metadata (levels available on the server, may or may not be downloaded locally)
    private val _remoteCommunityLevelsMeta = MutableStateFlow<List<de.egril.defender.save.CommunityFileInfo>>(emptyList())
    val remoteCommunityLevelsMeta: StateFlow<List<de.egril.defender.save.CommunityFileInfo>> = _remoteCommunityLevelsMeta.asStateFlow()

    // Remote community map metadata (maps available on the server, may or may not be downloaded locally)
    private val _remoteCommunityMapsMeta = MutableStateFlow<List<de.egril.defender.save.CommunityFileInfo>>(emptyList())
    val remoteCommunityMapsMeta: StateFlow<List<de.egril.defender.save.CommunityFileInfo>> = _remoteCommunityMapsMeta.asStateFlow()

    init {
        de.egril.defender.analytics.reportEvent(
            de.egril.defender.analytics.GameEventType.APP_STARTED,
            null,
            difficulty = AppSettings.difficulty.value.name,
        )

        // Upload settings to the dedicated backend table whenever a setting is changed
        // and the user is authenticated.
        de.egril.defender.ui.settings.AppSettings.onPersist = { uploadSettingsToBackend() }

        // Reload world map data after a successful restore from repository.
        de.egril.defender.editor.RepositoryManager.onDataRestored = { reloadWorldMap() }

        viewModelScope.launch {
            _pendingGameMessage.collectLatest { message ->
                if (_isDemoMode.value && message?.type?.autoDismissesInDemoMode() == true) {
                    delay(de.egril.defender.game.DemoMode.VILLAIN_MESSAGE_DISMISS_DELAY_MS)
                    if (_isDemoMode.value && _pendingGameMessage.value == message) {
                        dismissGameMessage()
                    }
                }
            }
        }

        if (isPlatformWasm) {
            // On WASM, repository files are loaded asynchronously. Initialize with progress
            // tracking and only start the world map after all data is ready.
            initializePlayerProfile()
            val isTutorialDeepLink = checkCurrentDeepLink() is DeepLink.Tutorial
            viewModelScope.launch {
                if (isTutorialDeepLink) {
                    // Priority path: load only the first (tutorial) level and its map first so
                    // the tutorial can start immediately. The remaining levels are loaded in the
                    // background within the same coroutine after the callback returns.
                    // Note: _isDataLoaded is set to true inside onFirstLevelReady once the minimum
                    // data required to start the tutorial level is available. The full repository
                    // continues loading in the background and initializeWorldMap() is called again
                    // afterwards to make all levels available on the world map.
                    de.egril.defender.editor.EditorStorage.ensureInitializedAsyncWithPriority(
                        onFirstLevelReady = {
                            _loadingProgress.value = null
                            _isDataLoaded.value = true
                            initializeWorldMap()
                        },
                        onProgress = { loaded, total, filename ->
                            _loadingProgress.value = LoadingProgress(loaded, total, filename)
                        },
                    )
                    // Full load finished – refresh world map so all levels become available.
                    _loadingProgress.value = null
                    initializeWorldMap()
                } else {
                    de.egril.defender.editor.EditorStorage.ensureInitializedAsync { loaded, total, filename ->
                        _loadingProgress.value = LoadingProgress(loaded, total, filename)
                    }
                    _loadingProgress.value = null
                    _isDataLoaded.value = true
                    initializeWorldMap()
                }
                // Note: background save restore is intentionally skipped on WASM because the
                // platform does not have a persistent process lifecycle the same way Android does.
            }
        } else {
            // On non-WASM platforms, repository files are loaded synchronously before this point.
            de.egril.defender.editor.EditorStorage
                .ensureInitialized()
            initializePlayerProfile()
            initializeWorldMap()
            // Restore game from background save if the process was killed while the app
            // was in the background during gameplay (primarily for Android).
            restoreBackgroundSaveIfExists()
        }
        // Note: saved games are loaded via onAuthStateChanged() which is called by a
        // LaunchedEffect in App.kt on every change of iamState.isAuthenticated (including
        // the initial composition).  This avoids an NPE that would occur if
        // refreshSavedGames() were called here directly, because _savedGames is declared
        // further down in the file and not yet initialised at this point in the constructor.
    }

    /**
     * Initialize player profile system
     * - Checks for existing players
     * - Migrates old saves if needed
     * - Sets up the current player
     */
    private fun initializePlayerProfile() {
        // Check for existing player profiles
        val profiles =
            de.egril.defender.save.PlayerProfileStorage
                .getAllProfiles()
        _allPlayers.value = profiles.profiles

        if (profiles.profiles.isEmpty()) {
            // No profiles exist - check if we need to migrate old saves
            val migratedProfile =
                de.egril.defender.save.PlayerProfileStorage
                    .migrateExistingSaves()
            if (migratedProfile != null) {
                // Migration successful, use the migrated profile
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Migrated existing saves to player profile: ${migratedProfile.name}")
                }
                _currentPlayer.value = migratedProfile
                de.egril.defender.save.SaveFileStorage
                    .setCurrentPlayer(migratedProfile.id)
                _allPlayers.value = listOf(migratedProfile)
            } else {
                // No existing saves, need to create first player
                _needsPlayerSelection.value = true
            }
        } else {
            // Load the last used player or the most recently played one
            val lastPlayerId = profiles.lastUsedPlayerId
            val playerToUse =
                if (lastPlayerId != null) {
                    profiles.profiles.find { it.id == lastPlayerId }
                } else {
                    profiles.profiles.maxByOrNull { it.lastPlayedAt }
                }

            if (playerToUse != null) {
                _currentPlayer.value = playerToUse
                de.egril.defender.save.SaveFileStorage
                    .setCurrentPlayer(playerToUse.id)
                de.egril.defender.save.PlayerProfileStorage
                    .updateLastPlayed(playerToUse.id)

                // Reload the player profile after updateLastPlayed to ensure we have the latest data
                val reloadedProfile =
                    de.egril.defender.save.PlayerProfileStorage
                        .getProfile(playerToUse.id)
                if (reloadedProfile != null) {
                    _currentPlayer.value = reloadedProfile
                }
            } else {
                // Shouldn't happen, but handle gracefully
                _needsPlayerSelection.value = true
            }
        }
    }

    internal fun initializeWorldMap() {
        // Load official levels
        val officialLevels = LevelData.createLevels()
        if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
            println("DEBUG: Total official levels loaded: ${officialLevels.size}")
        }

        // Load user levels from user sequence
        val userSequence =
            de.egril.defender.editor.EditorStorage
                .getUserLevelSequence()
        val userLevels =
            userSequence.sequence.mapNotNull { levelId ->
                val editorLevel =
                    de.egril.defender.editor.EditorStorage
                        .reloadLevel(levelId)
                if (editorLevel != null && !editorLevel.isOfficial) {
                    // Check if level is ready to play
                    if (de.egril.defender.editor.EditorStorage
                            .isLevelReadyToPlay(editorLevel)
                    ) {
                        // Convert editor level to game level
                        de.egril.defender.editor.EditorStorage.convertToGameLevel(
                            editorLevel,
                            officialLevels.size + userSequence.sequence.indexOf(levelId) + 1,
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
        val communityMaps =
            de.egril.defender.editor.EditorStorage
                .getAllCommunityMaps()
        val communityEditorLevels =
            de.egril.defender.editor.EditorStorage
                .getAllCommunityLevels()
        // .filter { it.id !in userLevelIds }  // Avoid duplicating levels the user also has locally
        if (LogConfig.isEnabled { LogConfig.ENABLE_COMMUNITY_DEBUG_LOGGING }) {
            println("COMMUNITY-DEBUG: Found ${communityMaps.size} community maps on disk: ${communityMaps.map { it.id }}")
            println(
                "COMMUNITY-DEBUG: Found ${communityEditorLevels.size} community levels on disk: ${communityEditorLevels.map {
                    "${it.id} (mapId=${it.mapId})"
                }}",
            )
        }
        val communityLevels =
            communityEditorLevels.mapIndexedNotNull { index, editorLevel ->
                val map =
                    de.egril.defender.editor.EditorStorage
                        .getMap(editorLevel.mapId)
                val levelReady = editorLevel.isReadyToPlay()
                val mapReady = map?.validateReadyToUse(includeRiversAsWalkable = true) ?: false
                val targets = map?.getTargets() ?: emptyList()
                val spawnPoints = map?.getSpawnPoints() ?: emptyList()
                val waypointResult =
                    if (targets.isNotEmpty()) {
                        editorLevel.validateWaypointsDetailed(targetPositions = targets, spawnPoints = spawnPoints)
                    } else {
                        null
                    }
                val waypointsValid = waypointResult?.isValid ?: false
                val isReady =
                    de.egril.defender.editor.EditorStorage
                        .isLevelReadyToPlay(editorLevel)
                if (LogConfig.isEnabled { LogConfig.ENABLE_COMMUNITY_DEBUG_LOGGING }) {
                    println(
                        "COMMUNITY-DEBUG: Level '${editorLevel.id}': mapId=${editorLevel.mapId}, mapFound=${map != null}, " +
                            "tiles=${map?.tiles?.size ?: 0}, levelReady=$levelReady, mapReady=$mapReady, " +
                            "targets=${targets.size}, spawns=${spawnPoints.size}, waypointsValid=$waypointsValid, " +
                            "isReadyToPlay=$isReady",
                    )
                }
                if (isReady) {
                    de.egril.defender.editor.EditorStorage.convertToGameLevel(
                        editorLevel,
                        officialLevels.size + userLevels.size + index + 1,
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
        val savedStatuses =
            de.egril.defender.save.SaveFileStorage
                .loadWorldMapStatus()

        // Get the set of won level IDs
        val wonLevelIds = savedStatuses?.filter { it.value == LevelStatus.WON }?.keys?.toSet() ?: emptySet()

        _worldLevels.value =
            allLevels.mapIndexed { index, level ->
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("DEBUG: Loaded Level ${level.id} - Name: ${level.name} - Path Cells: ${level.pathCells.size}")
                }
                // Look up status by editorLevelId if available
                val status =
                    if (level.editorLevelId != null) {
                        // Check saved status first
                        val savedStatus = savedStatuses?.get(level.editorLevelId)
                        if (savedStatus != null) {
                            savedStatus
                        } else {
                            // User and community levels are always unlocked
                            val editorLevel =
                                de.egril.defender.editor.EditorStorage
                                    .getLevel(level.editorLevelId)
                                    ?: de.egril.defender.editor.EditorStorage
                                        .getCommunityLevel(level.editorLevelId)
                            if (editorLevel?.isOfficial == false) {
                                LevelStatus.UNLOCKED
                            } else {
                                // Check if official level should be unlocked based on prerequisites
                                if (de.egril.defender.editor.EditorStorage
                                        .isLevelUnlocked(level.editorLevelId, wonLevelIds)
                                ) {
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
                    status = status,
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
                val communityLevelMeta =
                    de.egril.defender.save.BackendCommunityService
                        .fetchCommunityFileList("LEVEL")
                if (communityLevelMeta != null) {
                    _remoteCommunityLevelsMeta.value = communityLevelMeta
                }
                val communityMapMeta =
                    de.egril.defender.save.BackendCommunityService
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
                de.egril.defender.editor.EditorStorage
                    .clearCommunityCache()
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
        onComplete: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val fileData =
                    de.egril.defender.save.BackendCommunityService
                        .fetchCommunityFile("LEVEL", fileInfo.fileId)
                if (fileData == null) {
                    onComplete(false)
                    return@launch
                }
                val level =
                    de.egril.defender.editor.EditorJsonSerializer
                        .deserializeLevel(fileData.data)
                if (level == null) {
                    onComplete(false)
                    return@launch
                }
                de.egril.defender.editor.EditorStorage.saveCommunityLevel(
                    level.copy(isCommunity = true, communityAuthorUsername = fileInfo.authorUsername),
                )
                // Also download the map used by this level if not already present locally.
                val mapNeededDownload =
                    de.egril.defender.editor.EditorStorage
                        .getMap(level.mapId) == null
                if (mapNeededDownload) {
                    downloadCommunityMap(level.mapId)
                }
                // Remove this level from the remote-only list now that it has been downloaded
                // locally, so that the level cards view immediately shows it as a local level
                // instead of keeping it in the "download" state.
                _remoteCommunityLevelsMeta.value =
                    _remoteCommunityLevelsMeta.value
                        .filter { it.fileId != fileInfo.fileId }
                // If the community map was also freshly downloaded, remove it from the remote
                // maps list so the map editor list also reflects the local copy.
                if (mapNeededDownload) {
                    _remoteCommunityMapsMeta.value =
                        _remoteCommunityMapsMeta.value
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
                val alreadyLocal =
                    de.egril.defender.editor.EditorStorage
                        .getCommunityLevel(fileInfo.fileId) != null
                if (alreadyLocal) {
                    _remoteCommunityLevelsMeta.value =
                        _remoteCommunityLevelsMeta.value
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
            val mapData =
                de.egril.defender.save.BackendCommunityService
                    .fetchCommunityFile("MAP", mapId)
            if (mapData != null) {
                val map =
                    de.egril.defender.editor.EditorJsonSerializer
                        .deserializeMap(mapData.data)
                if (map != null) {
                    // Pass the requested mapId so saveCommunityMap can also store the map under
                    // that ID when the map's internal JSON id differs (edge case guard).
                    if (map.id != mapId) {
                        if (LogConfig.isEnabled { LogConfig.ENABLE_COMMUNITY_DEBUG_LOGGING }) {
                            println(
                                "COMMUNITY-DEBUG: Map id mismatch for download: requested='$mapId', json id='${map.id}'. Saving under both IDs.",
                            )
                        }
                    }
                    de.egril.defender.editor.EditorStorage.saveCommunityMap(
                        map,
                        mapData.authorUsername,
                        requestedId = mapId,
                    )
                    // Try to fetch the server-generated image and overwrite the locally-generated one
                    val imageBytes =
                        de.egril.defender.save.BackendCommunityService
                            .fetchCommunityMapImage(mapId)
                    if (imageBytes != null) {
                        de.egril.defender.editor.getFileStorage().writeBinaryFile(
                            "gamedata/community/maps/$mapId.png",
                            imageBytes,
                        )
                    }
                    // Zone overlays are generated by the backend too; overwrite the local copies
                    map.tileZones.indices.forEach { zoneIndex ->
                        val zoneBytes =
                            de.egril.defender.save.BackendCommunityService
                                .fetchCommunityMapZoneImage(mapId, zoneIndex)
                        if (zoneBytes != null) {
                            de.egril.defender.editor.getFileStorage().writeBinaryFile(
                                "gamedata/community/maps/${de.egril.defender.ui.MapImageProvider.tileZoneImageFileName(map.id, zoneIndex)}",
                                zoneBytes,
                            )
                        }
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
        onComplete: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                downloadCommunityMap(fileInfo.fileId)
                de.egril.defender.editor.EditorStorage
                    .clearCommunityCache()
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
    suspend fun uploadCommunityLevel(
        levelId: String,
        token: String,
    ): Boolean {
        val level =
            de.egril.defender.editor.EditorStorage
                .getLevel(levelId) ?: return false
        val username = de.egril.defender.iam.IamService.state.value.username ?: ""

        // Auto-upload the associated map if it is a user map not yet in the community
        val map =
            de.egril.defender.editor.EditorStorage
                .getMap(level.mapId)
        if (map != null && !map.isOfficial && !map.isCommunity) {
            val communityMap =
                de.egril.defender.editor.EditorStorage
                    .getCommunityMap(level.mapId)
            if (communityMap == null) {
                // Map not yet uploaded – upload it now
                val mapJson =
                    de.egril.defender.editor.EditorJsonSerializer
                        .serializeMap(map)
                val mapSuccess =
                    de.egril.defender.save.BackendCommunityService
                        .uploadCommunityFile("MAP", level.mapId, mapJson, token)
                if (mapSuccess) {
                    de.egril.defender.editor.EditorStorage
                        .saveCommunityMap(map, username)
                }
                // Proceed with the level upload even if the map upload failed; callers
                // can inspect the level-upload result and show an appropriate message.
            }
        }

        val json =
            de.egril.defender.editor.EditorJsonSerializer
                .serializeLevel(level)
        val success =
            de.egril.defender.save.BackendCommunityService
                .uploadCommunityFile("LEVEL", levelId, json, token, level.communityDescription)
        if (success) {
            // Store the uploaded version locally in the community directory so we can detect changes
            de.egril.defender.editor.EditorStorage.saveCommunityLevel(
                level.copy(
                    isCommunity = true,
                    communityAuthorUsername = username,
                ),
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
    suspend fun uploadCommunityMap(
        mapId: String,
        token: String,
    ): Boolean {
        val map =
            de.egril.defender.editor.EditorStorage
                .getMap(mapId) ?: return false
        val json =
            de.egril.defender.editor.EditorJsonSerializer
                .serializeMap(map)
        val success =
            de.egril.defender.save.BackendCommunityService
                .uploadCommunityFile("MAP", mapId, json, token)
        if (success) {
            val username = de.egril.defender.iam.IamService.state.value.username ?: ""
            de.egril.defender.editor.EditorStorage
                .saveCommunityMap(map, username)
        }
        return success
    }

    fun navigateToMainMenu() {
        stopTimeTracking()
        _currentScreen.value = Screen.MainMenu
    }

    fun reportAppClosed() {
        val isGameplayScreen = _currentScreen.value is Screen.GamePlay
        val currentState = _gameState.value
        val levelName: String?
        val turnNumber: Int?
        val difficulty: String?
        if (isGameplayScreen) {
            levelName = currentState?.level?.name
            turnNumber = currentState?.turnNumber?.value
            difficulty = currentState?.difficulty?.name
        } else {
            levelName = null
            turnNumber = null
            difficulty = null
        }

        de.egril.defender.analytics.reportEvent(
            de.egril.defender.analytics.GameEventType.APP_CLOSED,
            levelName,
            turnNumber,
            difficulty,
        )
    }

    fun navigateToWorldMap() {
        if (_currentScreen.value is Screen.GamePlay) {
            val levelName = _gameState.value?.level?.name ?: "unknown"
            val turnNumber = _gameState.value?.turnNumber?.value
            de.egril.defender.analytics.reportEvent(
                de.egril.defender.analytics.GameEventType.LEVEL_LEFT,
                levelName,
                turnNumber,
                _gameState.value?.difficulty?.name ?: AppSettings.difficulty.value.name,
            )
        }
        stopTimeTracking()
        // Player explicitly left gameplay – remove any background save so it is not
        // restored on the next cold start.
        deleteBackgroundSave()
        editorPlaytestSession = null
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

    fun navigateToVillainsAnnouncement() {
        _currentScreen.value = Screen.VillainsAnnouncement
    }

    fun navigateToLevelEditor(openLevelId: String? = null) {
        stopTimeTracking()
        deleteBackgroundSave()
        editorPlaytestSession = null
        _currentScreen.value = Screen.LevelEditor(openLevelId)
    }

    fun navigateBackFromGameplay() {
        val playtestSession = editorPlaytestSession
        if (playtestSession != null) {
            stopTimeTracking()
            deleteBackgroundSave()
            editorPlaytestSession = null
            _currentScreen.value = Screen.LevelEditor(playtestSession.editorLevel.id)
            return
        }
        navigateToWorldMap()
    }

    fun startEditorPlaytest(editorLevel: EditorLevel) {
        val numericId =
            -editorLevel.id
                .hashCode()
                .absoluteValue
                .coerceAtLeast(1)
        val gameLevel =
            de.egril.defender.editor.EditorStorage
                .convertToGameLevel(editorLevel, numericId) ?: return
        editorPlaytestSession = EditorPlaytestSession(editorLevel)
        startEditorPlaytestInternal(gameLevel)
    }

    private fun startEditorPlaytestInternal(level: Level) {
        _pendingGameMessage.value = null
        val difficulty = AppSettings.difficulty.value
        val playerStats = PlayerAbilities()
        val modifiedSpawnPlan =
            if (level.directSpawnPlan != null) {
                DifficultyModifiers.applySpawnPlanModifier(level.directSpawnPlan, difficulty)
            } else {
                val basePlan = generateSpawnPlan(level.attackerWaves)
                DifficultyModifiers.applySpawnPlanModifier(basePlan, difficulty)
            }
        val totalCoins = DifficultyModifiers.applyCoinsModifier(level.initialCoins, difficulty) + playerStats.getBonusStartCoins()
        val totalHealth = DifficultyModifiers.applyHealthPointsModifier(level.healthPoints, difficulty) + playerStats.getBonusHealth()
        val maxMana = playerStats.getMaxMana()
        val newGameState =
            GameState(
                level = level,
                difficulty = difficulty,
                coins = mutableStateOf(totalCoins),
                healthPoints = mutableStateOf(totalHealth),
                spawnPlan = modifiedSpawnPlan,
                spawnGroups = level.spawnGroups?.let { DifficultyModifiers.applySpawnGroupsModifier(it, difficulty) },
                maxMana = mutableStateOf(maxMana),
                currentMana = mutableStateOf(maxMana),
                incomeMultiplier = playerStats.getIncomeMultiplier(),
                constructionLevel = playerStats.constructionAbility,
            )
        newGameState.initializePrePlacedElements()
        _gameState.value = newGameState
        gameEngine = GameEngine(newGameState)
        _currentScreen.value = Screen.GamePlay(level.id)
        initialGameStateSnapshot = createGameStateSnapshot(newGameState)
        lastSaveSnapshot = initialGameStateSnapshot
        achievementManager = null
        startTimeTracking()
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

    fun navigateToNextLevel(
        nextLevelId: Int,
        nextLevelName: String,
    ) {
        val availableAbilityPoints = _currentPlayer.value?.abilities?.availableAbilityPoints ?: 0
        if (availableAbilityPoints > 0) {
            _currentScreen.value = Screen.StatsUpgradeWithNextLevel(nextLevelId, nextLevelName)
        } else {
            startLevel(nextLevelId)
        }
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
        de.egril.defender.save.PlayerProfileStorage
            .updateProfile(updatedPlayer)

        // Check for achievements
        val playerId = currentPlayer.id
        val tempAchievementManager =
            de.egril.defender.game
                .AchievementManager(playerId)
        tempAchievementManager.onAchievementEarned = { achievement ->
            _newAchievement.value = achievement
        }

        // First stat upgrade achievement
        val totalSpentBefore =
            oldStats.healthAbility + oldStats.treasuryAbility + oldStats.incomeAbility +
                oldStats.constructionAbility + oldStats.manaAbility
        if (totalSpentBefore == 0) {
            tempAchievementManager.onFirstStatUpgrade()
        }

        // Construction level 3 achievement
        if (statType == de.egril.defender.model.AbilityType.CONSTRUCTION && updatedStats.constructionAbility >= 3) {
            tempAchievementManager.onConstructionLevel3()
        }

        // Player level achievements
        val playerLevel =
            de.egril.defender.model.PlayerAbilities
                .calculateLevel(updatedStats.totalXP)
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
        de.egril.defender.save.PlayerProfileStorage
            .updateProfile(updatedPlayer)

        // Check for first spell unlock achievement
        val playerId = currentPlayer.id
        val tempAchievementManager =
            de.egril.defender.game
                .AchievementManager(playerId)
        tempAchievementManager.onAchievementEarned = { achievement ->
            _newAchievement.value = achievement
        }

        if (oldStats.unlockedSpells.isEmpty()) {
            tempAchievementManager.onFirstSpellUnlock()
        }
        // Sync updated spell list to backend
        uploadUserDataToBackend()
    }

    // Save/Load functionality

    internal val _savedGames = MutableStateFlow<List<de.egril.defender.save.SaveGameMetadata>>(emptyList())
    val savedGames: StateFlow<List<de.egril.defender.save.SaveGameMetadata>> = _savedGames.asStateFlow()

    internal val _isLoadingRemoteSaves = MutableStateFlow(false)
    val isLoadingRemoteSaves: StateFlow<Boolean> = _isLoadingRemoteSaves.asStateFlow()

    /**
     * In-memory cache of raw JSON for remote-only savefiles. Keyed by saveId.
     * Populated during [refreshSavedGames] so that [loadGame] can download and
     * store a remote-only save locally without a second network round-trip.
     */
    internal val remoteFilesCache = mutableMapOf<String, String>()

    companion object {
        /** Maximum counter suffix tried when generating a unique default player name. */
        internal const val MAX_PLAYER_NAME_COUNTER = 999

        /** Fixed save ID used for the background save (overwritten on every onPause). */
        internal const val BACKGROUND_SAVE_ID = "background_save"

        private const val LOST_LEVEL_XP_DIVISOR = 5

        /** Health points every enemy loses when the "Sky is falling" power is used. */
        internal const val SKY_IS_FALLING_DAMAGE = 10

        /** Health points each barricade gains when the Construction Repairs power is used. */
        internal const val CONSTRUCTION_REPAIRS_HP = 10

        /** Mana granted by the Mana Well power. */
        internal const val MANA_WELL_MANA = 10

        /** Mana granted by the Deep Mana Well power. */
        internal const val DEEP_MANA_WELL_MANA = 50

        /** Level ID of the tutorial level ("Welcome to Defender of Egril"). */
        const val TUTORIAL_LEVEL_ID = 1

        internal fun calculateAwardedXpForLevelCompletion(
            rawXpEarned: Int,
            won: Boolean,
        ): Int =
            if (won) {
                rawXpEarned
            } else {
                rawXpEarned / LOST_LEVEL_XP_DIVISOR
            }
    }
}
