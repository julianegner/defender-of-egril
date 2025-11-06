package com.defenderofegril.ui

import androidx.compose.runtime.Composable

/**
 * Re-export LevelEditorScreen from the new location for backward compatibility
 * This file serves as a bridge to maintain existing imports while the actual
 * implementation has been moved to ui/editor package
 */
@Composable
fun LevelEditorScreen(
    onBack: () -> Unit
) {
    com.defenderofegril.ui.editor.LevelEditorScreen(onBack = onBack)
}
