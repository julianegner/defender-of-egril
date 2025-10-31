package com.defenderofegril.game

import com.defenderofegril.model.*
import com.defenderofegril.editor.EditorStorage

object LevelData {
    
    fun createLevels(): List<Level> {
        // Load levels from editor storage
        val sequence = EditorStorage.getLevelSequence()
        
        // Safety check: if sequence is empty, something went wrong with initialization
        if (sequence.sequence.isEmpty()) {
            println("WARNING: Level sequence is empty! This shouldn't happen.")
            // Return empty list - the EditorStorage init should have created levels
            return emptyList()
        }
        
        return sequence.sequence.mapIndexed { index, levelId ->
            val editorLevel = EditorStorage.getLevel(levelId)
            editorLevel?.let {
                EditorStorage.convertToGameLevel(it, index + 1)
            }
        }.filterNotNull()
    }
}
