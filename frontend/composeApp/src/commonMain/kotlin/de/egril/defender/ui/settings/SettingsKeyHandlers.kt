package de.egril.defender.ui.settings

import androidx.compose.ui.input.key.Key
import de.egril.defender.ui.a11y.ColorBlindPalette

/**
 * Handle number key shortcuts to toggle settings in the current settings tab.
 * Returns true if the key was handled.
 */
internal fun handleSettingsNumberKey(
    tab: SettingsTab,
    number: Int,
): Boolean =
    when (tab) {
        SettingsTab.GENERAL ->
            when (number) {
                1 -> {
                    AppSettings.saveDarkMode(!AppSettings.isDarkMode.value)
                    true
                }
                2 -> {
                    AppSettings.saveShowDebugOptions(!AppSettings.showDebugOptions.value)
                    true
                }
                3 -> {
                    AppSettings.saveCheckForUpdates(!AppSettings.checkForUpdates.value)
                    true
                }
                else -> false
            }
        SettingsTab.WORLD_MAP ->
            when (number) {
                1 -> {
                    AppSettings.saveUseLevelCards(!AppSettings.useLevelCards.value)
                    true
                }
                2 -> {
                    AppSettings.saveEnableWorldMapAnimations(!AppSettings.enableWorldMapAnimations.value)
                    true
                }
                3 -> {
                    AppSettings.saveShowTestingLevels(!AppSettings.showTestingLevels.value)
                    true
                }
                else -> false
            }
        SettingsTab.LEVEL ->
            when (number) {
                1 -> {
                    AppSettings.saveUseTileImages(!AppSettings.useTileImages.value)
                    true
                }
                2 -> {
                    if (AppSettings.useTileImages.value) {
                        AppSettings.saveUseTileSmoothTransitions(!AppSettings.useTileSmoothTransitions.value)
                    }
                    true
                }
                3 -> {
                    AppSettings.saveEnableAnimations(!AppSettings.enableAnimations.value)
                    true
                }
                4 -> {
                    AppSettings.saveUseLevelMapImage(!AppSettings.useLevelMapImage.value)
                    true
                }
                5 -> {
                    AppSettings.saveShowUnitTowerBackground(!AppSettings.showUnitTowerBackground.value)
                    true
                }
                6 -> {
                    AppSettings.saveShowControlPad(!AppSettings.showControlPad.value)
                    true
                }
                7 -> {
                    AppSettings.saveAutoJumpToNextTower(!AppSettings.autoJumpToNextTower.value)
                    true
                }
                8 -> {
                    AppSettings.saveSplitBuildTowerButton(!AppSettings.splitBuildTowerButton.value)
                    true
                }
                else -> false
            }
        SettingsTab.SOUND ->
            when (number) {
                1 -> {
                    val newVal = !AppSettings.isSoundEnabled.value
                    AppSettings.saveSoundEnabled(newVal)
                    de.egril.defender.audio.GlobalSoundManager
                        .getInstance()
                        ?.setEnabled(newVal && AppSettings.isEffectsEnabled.value)
                    if (newVal && AppSettings.isMusicEnabled.value) {
                        val currentMusic =
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .getCurrentMusic()
                        if (currentMusic != null) {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .playMusic(currentMusic, loop = true)
                        }
                    } else {
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .stopMusic()
                    }
                    true
                }
                2 -> {
                    val newVal = !AppSettings.captionsEnabled.value
                    AppSettings.saveCaptionsEnabled(newVal)
                    true
                }
                3 -> {
                    val newVal = !AppSettings.isEffectsEnabled.value
                    AppSettings.saveEffectsEnabled(newVal)
                    de.egril.defender.audio.GlobalSoundManager
                        .getInstance()
                        ?.setEnabled(newVal && AppSettings.isSoundEnabled.value)
                    true
                }
                4 -> {
                    val newVal = !AppSettings.isMusicEnabled.value
                    AppSettings.saveMusicEnabled(newVal)
                    if (newVal && AppSettings.isSoundEnabled.value) {
                        val currentMusic =
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .getCurrentMusic()
                        if (currentMusic != null) {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .playMusic(currentMusic, loop = true)
                        }
                    } else {
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .stopMusic()
                    }
                    true
                }
                5 -> {
                    val newVal = !AppSettings.isWorldMapMusicEnabled.value
                    AppSettings.saveWorldMapMusicEnabled(newVal)
                    val currentMusic =
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .getCurrentMusic()
                    if (currentMusic == de.egril.defender.audio.BackgroundMusic.WORLD_MAP) {
                        if (newVal && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                            de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
                                loop = true,
                            )
                        } else {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .stopMusic()
                        }
                    }
                    true
                }
                6 -> {
                    val newVal = !AppSettings.isGameplayMusicEnabled.value
                    AppSettings.saveGameplayMusicEnabled(newVal)
                    val currentMusic =
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .getCurrentMusic()
                    if (currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL ||
                        currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH
                    ) {
                        if (newVal && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .playMusic(currentMusic, loop = true)
                        } else {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .stopMusic()
                        }
                    }
                    true
                }
                else -> false
            }
        SettingsTab.ACCESSIBILITY ->
            when (number) {
                1 -> {
                    AppSettings.saveHighContrastEnabled(!AppSettings.highContrastEnabled.value)
                    true
                }
                2 -> {
                    AppSettings.saveCaptionsEnabled(!AppSettings.captionsEnabled.value)
                    true
                }
                3 -> {
                    AppSettings.saveHoldToConfirmEnabled(!AppSettings.holdToConfirmEnabled.value)
                    true
                }
                4 -> {
                    AppSettings.saveEnableAnimations(!AppSettings.enableAnimations.value)
                    true
                }
                5 -> {
                    AppSettings.saveEnableWorldMapAnimations(!AppSettings.enableWorldMapAnimations.value)
                    true
                }
                6 -> {
                    AppSettings.saveColorBlindPalette(ColorBlindPalette.OFF)
                    true
                }
                7 -> {
                    AppSettings.saveColorBlindPalette(ColorBlindPalette.DEUTERANOPIA)
                    true
                }
                8 -> {
                    AppSettings.saveColorBlindPalette(ColorBlindPalette.PROTANOPIA)
                    true
                }
                9 -> {
                    AppSettings.saveColorBlindPalette(ColorBlindPalette.TRITANOPIA)
                    true
                }
                else -> false
            }
        SettingsTab.MOUSE_POINTERS ->
            when (number) {
                1 -> {
                    val next =
                        when (AppSettings.mousePointerSource.value) {
                            MousePointerSource.GAME -> MousePointerSource.GAUNTLET
                            MousePointerSource.GAUNTLET -> MousePointerSource.SYSTEM
                            MousePointerSource.SYSTEM -> MousePointerSource.GAME
                        }
                    AppSettings.saveMousePointerSource(next)
                    true
                }
                2 -> {
                    if (AppSettings.mousePointerSource.value != MousePointerSource.SYSTEM) {
                        val next =
                            if (AppSettings.mousePointerDirection.value == MousePointerDirection.RIGHT) {
                                MousePointerDirection.LEFT
                            } else {
                                MousePointerDirection.RIGHT
                            }
                        AppSettings.saveMousePointerDirection(next)
                    }
                    true
                }
                else -> false
            }
        SettingsTab.SHORTCUTS ->
            when (number) {
                1 -> {
                    AppSettings.saveShowButtonShortcutHints(!AppSettings.showButtonShortcutHints.value)
                    true
                }
                else -> false
            }
    }

/**
 * Handle letter/special key shortcuts for settings that aren't simple toggles.
 * Returns true if the key was handled.
 */
internal fun handleSettingsLetterKey(
    tab: SettingsTab,
    key: Key,
): Boolean =
    when (tab) {
        SettingsTab.GENERAL -> false // L, D, R handled inline in composable
        SettingsTab.SOUND ->
            when (key) {
                Key.D -> {
                    // Toggle show details - handled via composable state, signal via dummy toggle
                    // This will be handled separately in the composable
                    false
                }
                // +/- now handled inline with selectedVolumeIndex
                else -> false
            }
        SettingsTab.LEVEL ->
            when (key) {
                Key.Plus, Key.Equals -> {
                    adjustHeaderTextSize(increase = true)
                    true
                }
                Key.Minus -> {
                    adjustHeaderTextSize(increase = false)
                    true
                }
                else -> false
            }
        SettingsTab.MOUSE_POINTERS -> false
        SettingsTab.ACCESSIBILITY -> false
        SettingsTab.SHORTCUTS ->
            when (key) {
                Key.R -> {
                    AppSettings.resetShortcutBindings()
                    true
                }
                else -> false
            }
        else -> false
    }
