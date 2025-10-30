package com.defenderofegril.game

import com.defenderofegril.model.*
import com.defenderofegril.editor.EditorStorage

object LevelData {
    
    fun createLevels(): List<Level> {
        // Load levels from editor storage
        val editorLevels = EditorStorage.getLevelSequence().sequence
        return editorLevels.mapIndexed { index, levelId ->
            val editorLevel = EditorStorage.getLevel(levelId)
            editorLevel?.let {
                EditorStorage.convertToGameLevel(it, index + 1)
            }
        }.filterNotNull()
    }
}
