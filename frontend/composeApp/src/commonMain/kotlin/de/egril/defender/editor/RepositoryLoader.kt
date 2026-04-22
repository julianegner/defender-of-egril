package de.egril.defender.editor

import de.egril.defender.utils.JsonUtils
import defender_of_egril.composeapp.generated.resources.Res
import de.egril.defender.config.LogConfig

/**
 * Loads pre-built maps and levels from the repository directory in resources.
 * Repository files are stored in composeResources/files/repository/
 */
object RepositoryLoader {
    
    /**
     * Check if repository files exist in resources
     */
    suspend fun hasRepositoryFiles(): Boolean {
        return try {
            // Try to read the sequence file to see if repository exists
            val bytes = Res.readBytes("files/repository/sequence.json")
            bytes.isNotEmpty()
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Repository files not found: ${e.message}")
            }
            false
        }
    }
    
    /**
     * Load version from repository
     */
    suspend fun loadVersion(): String? {
        return try {
            val bytes = Res.readBytes("files/repository/version.txt")
            bytes.decodeToString().trim()
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Could not load version from repository: ${e.message}")
            }
            null
        }
    }

    /**
     * Load sequence from repository
     */
    suspend fun loadSequence(): LevelSequence? {
        return try {
            val bytes = Res.readBytes("files/repository/sequence.json")
            val json = bytes.decodeToString()
            EditorJsonSerializer.deserializeSequence(json)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Could not load sequence from repository: ${e.message}")
            }
            null
        }
    }
    
    /**
     * Load a map from repository
     */
    suspend fun loadMap(mapId: String): EditorMap? {
        return try {
            val bytes = Res.readBytes("files/repository/maps/$mapId.json")
            val json = bytes.decodeToString()
            EditorJsonSerializer.deserializeMap(json)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Could not load map $mapId from repository: ${e.message}")
            }
            null
        }
    }
    
    /**
     * Load a level from repository
     */
    suspend fun loadLevel(levelId: String): EditorLevel? {
        return try {
            val bytes = Res.readBytes("files/repository/levels/$levelId.json")
            val json = bytes.decodeToString()
            EditorJsonSerializer.deserializeLevel(json)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Could not load level $levelId from repository: ${e.message}")
            }
            null
        }
    }
    
    /**
     * Load dragon names from repository
     */
    suspend fun loadDragonNames(): List<String>? {
        return try {
            val bytes = Res.readBytes("files/repository/dragon_names.json")
            val json = bytes.decodeToString()
            parseDragonNames(json)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Could not load dragon names from repository: ${e.message}")
            }
            null
        }
    }
    
    /**
     * Load world map data from repository
     */
    suspend fun loadWorldMapData(): WorldMapData? {
        return try {
            val bytes = Res.readBytes("files/repository/worldmap.json")
            val json = bytes.decodeToString()
            EditorJsonSerializer.deserializeWorldMapData(json)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Could not load worldmap.json from repository: ${e.message}")
            }
            null
        }
    }
    
    /**
     * Parse dragon names from JSON.
     * Supports both old format (plain {"names": [...]}) and new format with metadata wrapper.
     */
    private fun parseDragonNames(json: String): List<String>? {
        return try {
            // Handle new metadata wrapper format
            val dataJson = EditorJsonSerializer.extractDataSection(json)
            // Extract the names array from JSON
            val namesSection = dataJson.substringAfter("\"names\": [").substringBefore("]")
            val names = JsonUtils.splitJsonArray(namesSection)
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
            
            if (names.isEmpty()) null else names
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Error parsing dragon names: ${e.message}")
            }
            null
        }
    }
    
    /**
     * Load all repository files and save them to file storage.
     * Skips the full reload if the stored version already matches the bundled version.
     * @param storage File storage to save to
     * @param onProgress Optional progress callback invoked as (loaded, total, currentFilename)
     * @return true if repository files were successfully loaded and saved (or already up to date)
     */
    suspend fun loadAndSaveRepositoryFiles(
        storage: FileStorage,
        onProgress: ((loaded: Int, total: Int, filename: String) -> Unit)? = null
    ): Boolean {
        return try {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Loading repository files...")
            }

            // Fast path: skip full reload if stored version matches bundled version
            val bundledVersion = loadVersion()
            val storedVersion = storage.readFile("gamedata/version.txt")?.trim()
            if (bundledVersion != null && bundledVersion == storedVersion) {
                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Repository data is up to date (version $storedVersion), skipping reload")
                }
                return true
            }

            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Repository version changed (stored: $storedVersion, bundled: $bundledVersion) - reloading all official data")
            }

            // Load sequence first
            val sequence = loadSequence()
            if (sequence == null || sequence.sequence.isEmpty()) {
                println("Repository sequence is empty or invalid")
                return false
            }
            
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Found ${sequence.sequence.size} levels in repository sequence")
            }

            // Estimated total: levels (N) + maps upper-bound (N, since each level may need a unique map) + 1 worldmap file
            val estimatedTotal = sequence.sequence.size * 2 + 1  // N levels + N maps (max) + 1 worldmap
            var loaded = 0

            // Track which maps we need to load
            val mapsToLoad = mutableSetOf<String>()
            
            // Load all levels in the sequence
            var successCount = 0
            for (levelId in sequence.sequence) {
                val level = loadLevel(levelId)
                if (level != null) {
                    // Mark level as official and save to official directory
                    val officialLevel = level.copy(isOfficial = true)
                    val levelJson = EditorJsonSerializer.serializeLevel(officialLevel)
                    storage.writeFile("gamedata/official/levels/$levelId.json", levelJson)
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Loaded and saved official level: $levelId")
                    }

                    // Track the map ID
                    mapsToLoad.add(level.mapId)
                    successCount++
                } else {
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("WARNING: Could not load level $levelId from repository")
                    }
                }
                loaded++
                onProgress?.invoke(loaded, estimatedTotal, "$levelId.json")
            }

            // Now that we know the actual number of unique maps, compute the real total:
            // N levels + M unique maps + 1 worldmap file (M ≤ N since maps are shared across levels)
            val actualTotal = sequence.sequence.size + mapsToLoad.size + 1  // N levels + M maps + 1 worldmap
            
            // Load all required maps
            var mapCount = 0
            for (mapId in mapsToLoad) {
                val map = loadMap(mapId)
                if (map != null) {
                    // Mark map as official and save to official directory
                    val officialMap = map.copy(isOfficial = true)
                    val mapJson = EditorJsonSerializer.serializeMap(officialMap)
                    storage.writeFile("gamedata/official/maps/$mapId.json", mapJson)
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("Loaded and saved official map: $mapId")
                    }
                    // Also copy the map image PNG if available
                    try {
                        val pngBytes = Res.readBytes("files/repository/maps/$mapId.png")
                        storage.writeBinaryFile("gamedata/official/maps/$mapId.png", pngBytes)
                        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("Loaded and saved official map image: $mapId.png")
                        }
                    } catch (e: Exception) {
                        // PNG might not exist, that's OK
                    }
                    mapCount++
                } else {
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("WARNING: Could not load map $mapId from repository")
                    }
                }
                loaded++
                onProgress?.invoke(loaded, actualTotal, "$mapId.json")
            }
            
            // Always save sequence to official directory from repository to keep it up to date
            val sequenceJson = EditorJsonSerializer.serializeSequence(sequence)
            storage.writeFile("gamedata/official/sequence.json", sequenceJson)
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Saved official sequence")
            }

            // Always load and save world map data from repository to official directory
            val worldMapData = loadWorldMapData()
            if (worldMapData != null) {
                val worldMapJson = EditorJsonSerializer.serializeWorldMapData(worldMapData)
                storage.writeFile("gamedata/official/worldmap.json", worldMapJson)
                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Loaded and saved official worldmap.json from repository")
                }
            } else {
                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("No worldmap.json in repository, skipping")
                }
            }
            loaded++
            onProgress?.invoke(loaded, actualTotal, "worldmap.json")
            
            // Save version file (use bundledVersion if available, otherwise fall back to hardcoded)
            storage.writeFile("gamedata/version.txt", bundledVersion ?: "10")
            
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Repository files loaded successfully: $successCount levels, $mapCount maps")
            }
            successCount > 0
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Error loading repository files: ${e.message}")
            }
            e.printStackTrace()
            false
        }
    }
}
