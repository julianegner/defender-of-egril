package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.EditorLevel
import de.egril.defender.game.GameEngine
import de.egril.defender.model.*
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.absoluteValue

fun GameViewModel.navigateToMainMenu() {
    stopTimeTracking()
    _currentScreen.value = Screen.MainMenu
}

fun GameViewModel.reportAppClosed() {
    val isGameplayScreen = _currentScreen.value is Screen.GamePlay
    val currentState = _gameState.value
    val levelName: String?
    val turnNumber: Int?
    val difficulty: String?
    if (isGameplayScreen) {
        levelName = currentState?.level?.name
        turnNumber = currentState?.turnNumber?.value
        difficulty = currentState?.difficulty?.name
    } else {
        levelName = null
        turnNumber = null
        difficulty = null
    }

    de.egril.defender.analytics.reportEvent(
        de.egril.defender.analytics.GameEventType.APP_CLOSED,
        levelName,
        turnNumber,
        difficulty,
    )
}

fun GameViewModel.navigateToWorldMap() {
    if (_currentScreen.value is Screen.GamePlay) {
        val levelName = _gameState.value?.level?.name ?: "unknown"
        val turnNumber = _gameState.value?.turnNumber?.value
        de.egril.defender.analytics.reportEvent(
            de.egril.defender.analytics.GameEventType.LEVEL_LEFT,
            levelName,
            turnNumber,
            _gameState.value?.difficulty?.name ?: AppSettings.difficulty.value.name,
        )
    }
    stopTimeTracking()
    // Player explicitly left gameplay – remove any background save so it is not
    // restored on the next cold start.
    deleteBackgroundSave()
    editorPlaytestSession = null
    // Reload levels from disk to ensure latest changes are visible
    reloadWorldMap()
    _currentScreen.value = Screen.WorldMap
}

fun GameViewModel.navigateToRules() {
    _currentScreen.value = Screen.Rules
}

fun GameViewModel.navigateToInstallationInfo() {
    _currentScreen.value = Screen.InstallationInfo
}

fun GameViewModel.navigateToBackendInfo() {
    _currentScreen.value = Screen.InstallationInfoAtTab(de.egril.defender.ui.infopage.InfoTab.BACKEND)
}

fun GameViewModel.navigateToDownloadInfo() {
    _currentScreen.value = Screen.InstallationInfoAtTab(de.egril.defender.ui.infopage.InfoTab.DOWNLOAD)
}

fun GameViewModel.navigateToVillainsAnnouncement() {
    _currentScreen.value = Screen.VillainsAnnouncement
}

fun GameViewModel.navigateToLevelEditor(openLevelId: String? = null) {
    stopTimeTracking()
    deleteBackgroundSave()
    editorPlaytestSession = null
    _currentScreen.value = Screen.LevelEditor(openLevelId)
}

fun GameViewModel.navigateBackFromGameplay() {
    val playtestSession = editorPlaytestSession
    if (playtestSession != null) {
        stopTimeTracking()
        deleteBackgroundSave()
        editorPlaytestSession = null
        _currentScreen.value = Screen.LevelEditor(playtestSession.editorLevel.id)
        return
    }
    navigateToWorldMap()
}

fun GameViewModel.navigateToSticker() {
    _currentScreen.value = Screen.Sticker
}

fun GameViewModel.navigateToLoadingSpinnerDemo() {
    _currentScreen.value = Screen.LoadingSpinnerDemo
}

fun GameViewModel.navigateToPlayerProfile() {
    _currentScreen.value = Screen.PlayerProfile
}

fun GameViewModel.navigateToStatsUpgrade() {
    _currentScreen.value = Screen.StatsUpgrade
}

fun GameViewModel.navigateToNextLevel(
    nextLevelId: Int,
    nextLevelName: String,
) {
    val availableAbilityPoints = _currentPlayer.value?.abilities?.availableAbilityPoints ?: 0
    if (availableAbilityPoints > 0) {
        _currentScreen.value = Screen.StatsUpgradeWithNextLevel(nextLevelId, nextLevelName)
    } else {
        startLevel(nextLevelId)
    }
}

fun GameViewModel.navigateToFinalCredits() {
    _currentScreen.value = Screen.FinalCredits
}

fun GameViewModel.navigateToAnimationTest() {
    _currentScreen.value = Screen.AnimationTest
}
