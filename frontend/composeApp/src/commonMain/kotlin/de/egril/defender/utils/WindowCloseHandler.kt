package de.egril.defender.utils

/**
 * Singleton to manage unsaved changes state for window close events.
 * This is shared across the application to coordinate between the UI and platform-specific window handlers.
 */
object WindowCloseHandler {
    /**
     * Callback to check if there are unsaved changes.
     * Should return true if there are unsaved changes, false otherwise.
     */
    private var unsavedChangesChecker: (() -> Boolean)? = null
    
    /**
     * Callback to save the game.
     * Allows platform-specific code to trigger a save before exiting.
     */
    private var saveGameCallback: (() -> Unit)? = null
    
    /**
     * Callback to check if official data has been modified.
     * Returns true if official data has been modified, false otherwise.
     */
    private var officialDataChangedChecker: (() -> Boolean)? = null

    /**
     * Callback to save the game state when the app goes to the background.
     * Used on Android to preserve game progress when the process may be killed.
     */
    private var backgroundSaveCallback: (() -> Unit)? = null

    /**
     * Set the callback to check for unsaved changes.
     */
    fun setUnsavedChangesChecker(checker: (() -> Boolean)?) {
        unsavedChangesChecker = checker
    }
    
    /**
     * Set the callback to save the game.
     */
    fun setSaveGameCallback(callback: (() -> Unit)?) {
        saveGameCallback = callback
    }
    
    /**
     * Set the callback to check if official data has been modified.
     */
    fun setOfficialDataChangedChecker(checker: (() -> Boolean)?) {
        officialDataChangedChecker = checker
    }
    
    /**
     * Set the callback to save the game state when the app goes to the background.
     */
    fun setBackgroundSaveCallback(callback: (() -> Unit)?) {
        backgroundSaveCallback = callback
    }

    /**
     * Save the game to the background save slot.
     * Should be called when the app is paused (goes to background).
     */
    fun saveOnBackground() {
        backgroundSaveCallback?.invoke()
    }

    /**
     * Check if there are unsaved changes.
     * Returns true if there are unsaved changes, false otherwise.
     */
    fun hasUnsavedChanges(): Boolean {
        return unsavedChangesChecker?.invoke() ?: false
    }
    
    /**
     * Save the game (if save callback is available).
     */
    fun saveGame() {
        saveGameCallback?.invoke()
    }
    
    /**
     * Check if official data has been modified.
     * Returns true if official data has been modified, false otherwise.
     */
    fun hasOfficialDataChanged(): Boolean {
        return officialDataChangedChecker?.invoke() ?: false
    }
}
