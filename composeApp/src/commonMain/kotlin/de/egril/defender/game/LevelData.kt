package de.egril.defender.game

import de.egril.defender.model.*
import de.egril.defender.editor.EditorStorage

object LevelData {
    
    fun createLevels(): List<Level> {
        println("Creating levels from editor storage...")
        // Load levels from editor storage
        val sequence = EditorStorage.getLevelSequence()
        println("Level sequence loaded: ${sequence.sequence.size} levels found.")
        
        // Safety check: if sequence is empty, something went wrong with initialization
        if (sequence.sequence.isEmpty()) {
            println("WARNING: Level sequence is empty! This shouldn't happen.")
            // Return empty list - the EditorStorage init should have created levels
            return emptyList()
        }

        println("Converting editor levels to game levels...")
        // First, filter valid levels, then assign sequential IDs
        val validLevels = sequence.sequence.mapIndexedNotNull { index, levelId ->
            // Reload level from disk to ensure we have the latest version
            val editorLevel = EditorStorage.reloadLevel(levelId)
            println("Processing level ID: $levelId at index $index - Found: ${editorLevel != null}")
            
            editorLevel?.let { level ->
                // Check if level is ready to play
                if (!level.isReadyToPlay()) {
                    println("Skipping level $levelId: not ready to play (towers: ${level.availableTowers.size}, spawns: ${level.enemySpawns.size})")
                    return@mapIndexedNotNull null
                }
                
                // Check if the map is ready to use
                val map = EditorStorage.getMap(level.mapId)
                if (map == null) {
                    println("Skipping level $levelId: map ${level.mapId} not found")
                    return@mapIndexedNotNull null
                }
                
                if (!map.readyToUse) {
                    println("Skipping level $levelId: map ${level.mapId} is not ready to use")
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
