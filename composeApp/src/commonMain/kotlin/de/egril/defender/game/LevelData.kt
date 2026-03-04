package de.egril.defender.game

import de.egril.defender.model.*
import de.egril.defender.editor.EditorStorage
import de.egril.defender.config.LogConfig

object LevelData {
    
    fun createLevels(): List<Level> {
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("Creating levels from editor storage...")
        }
        // Load levels from editor storage
        val sequence = EditorStorage.getLevelSequence()
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("Level sequence loaded: ${sequence.sequence.size} levels found.")
        }
        
        // Safety check: if sequence is empty, something went wrong with initialization
        if (sequence.sequence.isEmpty()) {
            println("WARNING: Level sequence is empty! This shouldn't happen.")
            // Return empty list - the EditorStorage init should have created levels
            return emptyList()
        }

        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("Converting editor levels to game levels...")
        }
        // First, filter valid levels, then assign sequential IDs
        val validLevels = sequence.sequence.mapIndexedNotNull { index, levelId ->
            // Reload level from disk to ensure we have the latest version
            val editorLevel = EditorStorage.reloadLevel(levelId)
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Processing level ID: $levelId at index $index - Found: ${editorLevel != null}")
            }
            
            editorLevel?.let { level ->
                // Get the map for validation checks
                val map = EditorStorage.getMap(level.mapId)
                
                // Check if level is ready to play (includes map readiness check)
                if (!EditorStorage.isLevelReadyToPlay(level)) {
                    println("Skipping level $levelId: not ready to play (towers: ${level.availableTowers.size}, spawns: ${level.enemySpawns.size}, map ready: ${map?.readyToUse})")
                    return@mapIndexedNotNull null
                }
                
                // Check if waypoints are valid (all eventually lead to a target)
                val targets = map?.getTargets() ?: emptyList()
                if (targets.isNotEmpty() && !level.validateWaypoints(targets)) {
                    println("Skipping level $levelId: waypoints do not all lead to a target")
                    return@mapIndexedNotNull null
                }
                
                // Return the editor level for conversion
                level
            }
        }
        
        // Now convert valid levels with sequential IDs
        return validLevels.mapIndexed { index, editorLevel ->
            EditorStorage.convertToGameLevel(editorLevel, index + 1)
        }.filterNotNull()
    }
}
