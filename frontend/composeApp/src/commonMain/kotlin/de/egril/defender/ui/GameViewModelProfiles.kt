package de.egril.defender.ui

import de.egril.defender.ui.infopage.checkForNewerVersions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Player Profile Management

/**
 * Create a default player profile without requiring the user to enter a name.
 * Uses "Player" as the base name, appending a counter if that name is already taken.
 * Called automatically on first launch so the player can start playing immediately.
 */
fun GameViewModel.createDefaultPlayer() {
    if (createPlayer("Player")) return
    var counter = 2
    while (!createPlayer("Player $counter")) {
        counter++
        if (counter > GameViewModel.MAX_PLAYER_NAME_COUNTER) break
    }
}

/**
 * Create a new player profile
 * @return true if successful, false if name is invalid or already exists
 */
fun GameViewModel.createPlayer(name: String): Boolean {
    val profile =
        de.egril.defender.save.PlayerProfileStorage
            .createProfile(name)
    if (profile != null) {
        _currentPlayer.value = profile
        _allPlayers.value =
            de.egril.defender.save.PlayerProfileStorage
                .getAllProfiles()
                .profiles
        de.egril.defender.save.SaveFileStorage
            .setCurrentPlayer(profile.id)
        _needsPlayerSelection.value = false

        // Reload world map for new player
        initializeWorldMap()

        return true
    }
    return false
}

/**
 * Switch to a different player profile
 */
fun GameViewModel.switchPlayer(playerId: String) {
    val profile =
        de.egril.defender.save.PlayerProfileStorage
            .getProfile(playerId)
    if (profile != null) {
        _currentPlayer.value = profile
        de.egril.defender.save.SaveFileStorage
            .setCurrentPlayer(playerId)
        de.egril.defender.save.PlayerProfileStorage
            .updateLastPlayed(playerId)

        // Reload world map for new player
        initializeWorldMap()

        // Reload saved games list
        refreshSavedGames()
    }
}

/**
 * Delete a player profile
 */
fun GameViewModel.deletePlayer(playerId: String): Boolean {
    // Don't allow deleting the current player
    if (_currentPlayer.value?.id == playerId) {
        return false
    }

    val success =
        de.egril.defender.save.PlayerProfileStorage
            .deleteProfile(playerId)
    if (success) {
        _allPlayers.value =
            de.egril.defender.save.PlayerProfileStorage
                .getAllProfiles()
                .profiles
    }
    return success
}

/**
 * Rename the current player profile
 * @return true if successful, false if name is invalid or already exists
 */
fun GameViewModel.renameCurrentPlayer(newName: String): Boolean {
    val currentPlayerId = _currentPlayer.value?.id ?: return false
    val updatedProfile =
        de.egril.defender.save.PlayerProfileStorage
            .renameProfile(currentPlayerId, newName)
    if (updatedProfile != null) {
        _currentPlayer.value = updatedProfile
        _allPlayers.value =
            de.egril.defender.save.PlayerProfileStorage
                .getAllProfiles()
                .profiles

        // Update SaveFileStorage if the ID changed
        if (updatedProfile.id != currentPlayerId) {
            de.egril.defender.save.SaveFileStorage
                .setCurrentPlayer(updatedProfile.id)
        }

        // Sync the new local username to the backend
        uploadUserDataToBackend()
        return true
    }
    return false
}

/**
 * Refresh the list of all player profiles
 */
fun GameViewModel.refreshPlayerProfiles() {
    _allPlayers.value =
        de.egril.defender.save.PlayerProfileStorage
            .getAllProfiles()
            .profiles
}

/**
 * Refresh the current player profile (e.g., after earning an achievement)
 */
internal fun GameViewModel.refreshCurrentPlayer() {
    val currentPlayerId = _currentPlayer.value?.id ?: return
    val updatedProfile =
        de.egril.defender.save.PlayerProfileStorage
            .getProfile(currentPlayerId)
    if (updatedProfile != null) {
        _currentPlayer.value = updatedProfile
    }
}

/**
 * Clear the new achievement notification
 */
fun GameViewModel.clearAchievementNotification() {
    _newAchievement.value = null
}

/**
 * Checks GitHub releases in the background for a version newer than the running build.
 * Updates [newVersionAvailable] when a newer version is found.
 * Should be called once at app start-up; safe to call multiple times (only the first
 * non-null result is stored).
 */
fun GameViewModel.checkForUpdates() {
    viewModelScope.launch {
        val infos = checkForNewerVersions()
        if (infos.isNotEmpty()) {
            _newVersionAvailable.value = infos
        }
    }
}

/** Dismisses the new-version notification banner/dialog. */
fun GameViewModel.dismissNewVersionNotification() {
    _newVersionAvailable.value = emptyList()
}

fun GameViewModel.startTimeTracking() {
    val currentTime =
        de.egril.defender.utils
            .currentTimeMillis()
    gameSessionStartTime = currentTime
    lastBreakReminderTime = currentTime
    lastSleepReminderTime = currentTime

    // Start coroutine to check for reminders
    viewModelScope.launch {
        checkTimeReminders()
    }
}

/**
 * Stop tracking time when leaving the game screen
 */
fun GameViewModel.stopTimeTracking() {
    gameSessionStartTime = null
    lastBreakReminderTime = null
    lastSleepReminderTime = null
}

/**
 * Check and show time reminders periodically
 */
private suspend fun GameViewModel.checkTimeReminders() {
    while (gameSessionStartTime != null) {
        delay(60000L) // Check every minute

        val currentTime =
            de.egril.defender.utils
                .currentTimeMillis()

        // Check break reminder (every 2 hours)
        lastBreakReminderTime?.let { lastBreak ->
            if (currentTime - lastBreak >= BREAK_REMINDER_INTERVAL_MS) {
                gameSessionStartTime?.let { sessionStart ->
                    val elapsedMs = currentTime - sessionStart
                    _reminderMessage.value =
                        ReminderMessage(
                            type = de.egril.defender.ui.gameplay.ReminderType.BREAK,
                            elapsedMs = elapsedMs,
                        )
                    lastBreakReminderTime = currentTime
                }
            }
        }

        // Check sleep reminder (after 23:00, every hour)
        lastSleepReminderTime?.let { lastSleep ->
            if (currentTime - lastSleep >= SLEEP_REMINDER_INTERVAL_MS) {
                val hour = getLocalHour(currentTime)
                if (hour >= SLEEP_START_HOUR || hour < 6) { // Between 23:00 and 06:00
                    val timeDescription =
                        when {
                            hour == 23 -> "close_to_midnight"
                            hour == 0 -> "midnight"
                            else -> "after_midnight"
                        }
                    _reminderMessage.value =
                        ReminderMessage(
                            type = de.egril.defender.ui.gameplay.ReminderType.SLEEP,
                            timeDescription = timeDescription,
                        )
                    lastSleepReminderTime = currentTime
                }
            }
        }
    }
}

/**
 * Clear the current reminder message
 */
fun GameViewModel.clearReminderMessage() {
    _reminderMessage.value = null
}

/**
 * Show [message] as the current game message and record it in the story logbook
 * together with the round in which it is shown.
 */
internal fun GameViewModel.presentGameMessage(message: de.egril.defender.model.GameMessage) {
    val state = _gameState.value
    if (state != null) {
        state.storyLog.add(
            de.egril.defender.model
                .StoryLogEntry(state.turnNumber.value, message),
        )
    }
    _pendingGameMessage.value = message
}

/**
 * Dismiss the current game message and surface the next one (if any).
 */
fun GameViewModel.dismissGameMessage() {
    val state = _gameState.value
    if (state != null && state.pendingMessages.isNotEmpty()) {
        val next = state.pendingMessages.removeAt(0)
        presentGameMessage(next)
    } else {
        _pendingGameMessage.value = null
        if (_currentScreen.value is Screen.GamePlay &&
            state != null &&
            state.scriptedVictory.value &&
            !state.isLevelLost() &&
            state.isLevelWon()
        ) {
            completeLevel(state.level.id, won = true)
        }
    }
}

/**
 * After a trap fires and its animation completes, process any newly-defeated attackers so
 * their death animation plays immediately after the trap animation (rather than being deferred
 * to completeEnemyTurn). Adds a further delay equal to [ENEMY_DEATH_ANIMATION_DELAY_MS] so
 * the death animation finishes before the next movement step begins.
 */
internal suspend fun GameViewModel.processAndDelayForTrapDeaths() {
    val deathCountBefore = _gameState.value?.defeatedEnemyEffects?.size ?: 0
    gameEngine?.processDefeatedAttackers()
    if ((_gameState.value?.defeatedEnemyEffects?.size ?: 0) > deathCountBefore) {
        delay(ENEMY_DEATH_ANIMATION_DELAY_MS)
    }
}

/**
 * Surface the next pending game message if no message is currently being shown.
 * Does nothing when a message is already visible so that the queue is not skipped.
 */
internal fun GameViewModel.surfaceNextPendingMessageIfIdle() {
    val state = _gameState.value ?: return
    if (state.pendingMessages.isNotEmpty() && _pendingGameMessage.value == null) {
        val nextMessage = state.pendingMessages.removeAt(0)
        presentGameMessage(nextMessage)
    }
    if (state.scriptedVictory.value && !state.isLevelLost() && state.isLevelWon()) {
        completeLevel(state.level.id, won = true)
    }
}

/**
 * Blocks enemy-turn progression while a narrative/villain story message is open, so units do
 * not keep moving behind the dialog.
 */
internal suspend fun GameViewModel.waitForBlockingNarrativeMessageDismissal() {
    while (true) {
        val currentMessage = _pendingGameMessage.value ?: return
        if (!currentMessage.type.blocksEnemyTurnMovement()) return
        delay(50L)
    }
}

private fun de.egril.defender.model.GameMessageType.blocksEnemyTurnMovement(): Boolean =
    when (this) {
        de.egril.defender.model.GameMessageType.STORY_INTRO,
        de.egril.defender.model.GameMessageType.EWHAD_ENTERS,
        de.egril.defender.model.GameMessageType.EWHAD_RETREATS,
        de.egril.defender.model.GameMessageType.EWHAD_DEFEATED,
        de.egril.defender.model.GameMessageType.VILLAIN_ENTERS,
        de.egril.defender.model.GameMessageType.VILLAIN_DEFEATED,
        de.egril.defender.model.GameMessageType.WAAAGH_FRENZY,
        -> true
        else -> false
    }

internal fun de.egril.defender.model.GameMessageType.autoDismissesInDemoMode(): Boolean =
    when (this) {
        de.egril.defender.model.GameMessageType.VILLAIN_ENTERS,
        de.egril.defender.model.GameMessageType.VILLAIN_DEFEATED,
        -> true
        else -> false
    }

/**
 * Get the local hour (0-23) from timestamp
 */
private fun GameViewModel.getLocalHour(timestamp: Long): Int =
    de.egril.defender.utils
        .getLocalHour(timestamp)
