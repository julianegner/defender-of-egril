package de.egril.defender.utils

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Singleton to manage unsaved changes state for window close events.
 * This is shared across the application to coordinate between the UI and platform-specific window handlers.
 */
object WindowCloseHandler {
    /**
     * Mutable state to hold the current unsaved changes checker function.
     * This can be observed by platform-specific code to determine if there are unsaved changes.
     */
    val unsavedChangesChecker: MutableState<(() -> Boolean)?> = mutableStateOf(null)
    
    /**
     * Mutable state to hold the save game callback.
     * This allows platform-specific code to trigger a save before exiting.
     */
    val saveGameCallback: MutableState<(() -> Unit)?> = mutableStateOf(null)
    
    /**
     * Check if there are unsaved changes.
     * Returns true if there are unsaved changes, false otherwise.
     */
    fun hasUnsavedChanges(): Boolean {
        return unsavedChangesChecker.value?.invoke() ?: false
    }
    
    /**
     * Save the game (if save callback is available).
     */
    fun saveGame() {
        saveGameCallback.value?.invoke()
    }
}
