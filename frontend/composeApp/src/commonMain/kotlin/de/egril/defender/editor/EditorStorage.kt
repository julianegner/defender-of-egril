package de.egril.defender.editor

import de.egril.defender.config.LogConfig
import de.egril.defender.utils.runBlockingCompat

/**
 * File-based storage for maps and levels
 * Stores data in ~/.defender-of-egril/gamedata/ directory on desktop
 *
 * Map, level, sequence and world map operations live in the EditorStorage*.kt files as
 * extension functions on this object.
 */
object EditorStorage {
    internal val fileStorage = getFileStorage()
    internal val mapsCache = mutableMapOf<String, EditorMap>()
    internal val pendingMapImages = mutableSetOf<String>()
    internal val pendingZoneImages = mutableMapOf<String, Set<Int>>()
    internal val levelsCache = mutableMapOf<String, EditorLevel>()
    internal val communityMapsCache = mutableMapOf<String, EditorMap>()
    internal val communityLevelsCache = mutableMapOf<String, EditorLevel>()
    internal var levelSequenceCache: LevelSequence? = null
    internal var userLevelSequenceCache: LevelSequence? = null

    // Official content directories (read from repository)
    internal val OFFICIAL_MAPS_DIR = "gamedata/official/maps"
    internal val OFFICIAL_LEVELS_DIR = "gamedata/official/levels"
    internal val OFFICIAL_SEQUENCE_FILE = "gamedata/official/sequence.json"
    internal val OFFICIAL_WORLDMAP_FILE = "gamedata/official/worldmap.json"
    internal val OFFICIAL_MAP_TEMPLATES_DIR = "gamedata/official/editor/map-templates"
    internal val OFFICIAL_SPAWN_TEMPLATES_DIR = "gamedata/official/editor/spawn-templates"

    // User content directories (created by users in editor)
    internal val USER_MAPS_DIR = "gamedata/user/maps"
    internal val USER_LEVELS_DIR = "gamedata/user/levels"
    internal val USER_SEQUENCE_FILE = "gamedata/user/sequence.json"
    internal val USER_MAP_TEMPLATES_DIR = "gamedata/user/editor/map-templates"
    internal val USER_SPAWN_TEMPLATES_DIR = "gamedata/user/editor/spawn-templates"

    // Community content directories (downloaded from backend)
    internal val COMMUNITY_MAPS_DIR = "gamedata/community/maps"
    internal val COMMUNITY_LEVELS_DIR = "gamedata/community/levels"

    // Legacy directories (for backward compatibility)
    internal val LEGACY_MAPS_DIR = "gamedata/maps"
    internal val LEGACY_LEVELS_DIR = "gamedata/levels"
    internal val LEGACY_SEQUENCE_FILE = "gamedata/sequence.json"
    internal val LEGACY_WORLDMAP_FILE = "gamedata/worldmap.json"

    private val VERSION_FILE = "gamedata/version.txt"
    private val CURRENT_VERSION = "10" // Increment when level data format changes - v10: added metadata wrapper to all JSON files

    internal var worldMapDataCache: WorldMapData? = null
    private var initialized = false

    /**
     * Initialize and validate repository data
     * Must be called before using EditorStorage in production code
     * Silently skips initialization if repository files aren't available (e.g., in test environments)
     */
    fun ensureInitialized() {
        if (initialized) return

        // Always try to load repository files first
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Initializing EditorStorage - loading repository files...")
        }
        val repositoryLoaded = tryLoadRepositoryFiles()

        if (!repositoryLoaded) {
            println("Repository files could not be loaded - this may be a test environment")
            // Continue anyway - we'll try to load from other paths or use defaults
        }

        // Validate that we have all required data categories
        val missingCategories = validateRepositoryData()
        if (missingCategories.isNotEmpty()) {
            // In production builds, repository files are complete, so we should never reach here
            // If we do, we're in a test environment with incomplete data
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Missing data categories: $missingCategories - continuing anyway (test environment)")
            }
        }

        // Load sequence to populate cache
        val sequence = getLevelSequence()
        if (sequence.sequence.isEmpty()) {
            println("Level sequence is empty - continuing anyway (test environment)")
        }

        initialized = true
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("EditorStorage initialized successfully. Repository loaded: $repositoryLoaded")
        }
    }

    /**
     * Async version of ensureInitialized that supports loading progress reporting.
     * Use this on platforms where file loading is inherently asynchronous (e.g. WASM).
     * Reports loading progress via [onProgress] as (loaded, total, currentFilename).
     */
    suspend fun ensureInitializedAsync(onProgress: ((loaded: Int, total: Int, filename: String) -> Unit)? = null) {
        if (initialized) return

        // Pre-populate the in-memory cache from the platform's async backing store
        // (e.g. OPFS on WASM/browser) before checking the fingerprint fast path.
        fileStorage.initializeAsync()

        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Initializing EditorStorage asynchronously - loading repository files...")
        }
        val repositoryLoaded = RepositoryLoader.loadAndSaveRepositoryFiles(fileStorage, onProgress)

        if (!repositoryLoaded) {
            println("Repository files could not be loaded (async) - this may be a test environment")
        }

        val missingCategories = validateRepositoryData()
        if (missingCategories.isNotEmpty()) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Missing data categories: $missingCategories - continuing anyway (test environment)")
            }
        }

        val sequence = getLevelSequence()
        if (sequence.sequence.isEmpty()) {
            println("Level sequence is empty - continuing anyway (test environment)")
        }

        initialized = true
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("EditorStorage initialized asynchronously. Repository loaded: $repositoryLoaded")
        }
    }

    /**
     * Async initialization that loads the first level in the sequence and its map first, then
     * invokes [onFirstLevelReady] before loading the rest of the repository in the background.
     *
     * Use this when the player arrives via a deep link that targets the first level (e.g. /tutorial)
     * so they can start playing immediately while the remaining game data loads.
     *
     * If storage is already up-to-date [onFirstLevelReady] is called right away (fast path).
     * After the full load completes [initialized] is set to true so subsequent calls are no-ops.
     */
    suspend fun ensureInitializedAsyncWithPriority(
        onFirstLevelReady: suspend () -> Unit,
        onProgress: ((loaded: Int, total: Int, filename: String) -> Unit)? = null,
    ) {
        if (initialized) {
            // Already fully initialised - signal ready immediately.
            onFirstLevelReady()
            return
        }

        // Pre-populate the in-memory cache from the platform's async backing store
        // (e.g. OPFS on WASM/browser) before checking the fingerprint fast path.
        fileStorage.initializeAsync()

        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Initializing EditorStorage asynchronously (priority mode) - loading first level first...")
        }
        val repositoryLoaded =
            RepositoryLoader.loadAndSaveRepositoryFilesWithPriority(
                storage = fileStorage,
                onFirstLevelReady = onFirstLevelReady,
                onProgress = onProgress,
            )

        if (!repositoryLoaded) {
            println("Repository files could not be loaded (async priority) - this may be a test environment")
        }

        val missingCategories = validateRepositoryData()
        if (missingCategories.isNotEmpty()) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Missing data categories: $missingCategories - continuing anyway (test environment)")
            }
        }

        val sequence = getLevelSequence()
        if (sequence.sequence.isEmpty()) {
            println("Level sequence is empty - continuing anyway (test environment)")
        }

        initialized = true
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("EditorStorage initialized asynchronously (priority mode). Repository loaded: $repositoryLoaded")
        }
    }

    /**
     * Validate that all required repository data exists and is not empty
     * @return List of missing or empty categories
     */
    private fun validateRepositoryData(): List<String> {
        val missing = mutableListOf<String>()

        // Check maps
        val maps = getAllMaps()
        if (maps.isEmpty()) {
            missing.add("maps")
        }

        // Check levels
        val levels = getAllLevels()
        if (levels.isEmpty()) {
            missing.add("levels")
        }

        // Check sequence
        val sequence = getLevelSequence()
        if (sequence.sequence.isEmpty()) {
            missing.add("sequence")
        }

        // Check worldmap (optional but good to have)
        val worldMapData = getWorldMapData()
        if (worldMapData.locations.isEmpty() && worldMapData.paths.isEmpty()) {
            missing.add("worldmap")
        }

        return missing
    }

    /**
     * Save the map. Returns true if the map image was (re)generated, false if it was skipped
     * because the image already existed and no tile type changes were detected.
     *
     * @param map The map to save.
     * @param oldId If the map was renamed (ID changed), pass the old ID here so that the old
     *   JSON and PNG files are deleted after saving under the new name.
     */

    /**
     * Result of [saveMapData] indicating whether the image needs regeneration.
     */
    data class MapSaveResult(
        val imageNeedsRegeneration: Boolean,
        val validatedMap: EditorMap,
        val zoneIndicesToRegenerate: List<Int>,
    ) {
        val zoneImagesNeedRegeneration: Boolean get() = zoneIndicesToRegenerate.isNotEmpty()
    }

    /**
     * Try to load maps and levels from repository resources.
     * Returns true if repository files were found and loaded successfully.
     *
     * Note: Uses runBlocking because it's called from the init block which is synchronous.
     * This is acceptable since it only runs once during app initialization.
     */
    private fun tryLoadRepositoryFiles(): Boolean =
        try {
            // Use runBlockingCompat to make this synchronous
            runBlockingCompat {
                RepositoryLoader.loadAndSaveRepositoryFiles(fileStorage)
            }
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Could not load repository files: ${e.message}")
            }
            false
        }

    /**
     * Check if the gamedata directory contains any existing user files
     * @return true if any user data files exist, false if directory is empty
     */
    private fun hasExistingGamedataFiles(): Boolean {
        // Check official sequence file
        if (fileStorage.fileExists(OFFICIAL_SEQUENCE_FILE)) {
            return true
        }

        // Check legacy sequence file (for backward compatibility)
        if (fileStorage.fileExists(LEGACY_SEQUENCE_FILE)) {
            return true
        }

        // Check if any official maps exist
        if (fileStorage.listFiles(OFFICIAL_MAPS_DIR).isNotEmpty()) {
            return true
        }

        // Check if any user maps exist
        if (fileStorage.listFiles(USER_MAPS_DIR).isNotEmpty()) {
            return true
        }

        // Check legacy maps directory
        if (fileStorage.listFiles(LEGACY_MAPS_DIR).isNotEmpty()) {
            return true
        }

        // Check if any official levels exist
        if (fileStorage.listFiles(OFFICIAL_LEVELS_DIR).isNotEmpty()) {
            return true
        }

        // Check if any user levels exist
        if (fileStorage.listFiles(USER_LEVELS_DIR).isNotEmpty()) {
            return true
        }

        // Check legacy levels directory
        if (fileStorage.listFiles(LEGACY_LEVELS_DIR).isNotEmpty()) {
            return true
        }

        // Check official worldmap file
        if (fileStorage.fileExists(OFFICIAL_WORLDMAP_FILE)) {
            return true
        }

        // Check legacy worldmap file (for backward compatibility)
        if (fileStorage.fileExists(LEGACY_WORLDMAP_FILE)) {
            return true
        }

        // No user data found
        return false
    }
}
