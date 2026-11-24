package de.egril.defender.ui.settings

import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.ui.a11y.ColorBlindPalette
import de.egril.defender.utils.isPlatformMobile

fun AppSettings.getDefaultShortcutBindings(): Map<String, String> =
    mapOf(
        KEY_SHORTCUT_ATTACK_SELECTED_TARGET to DEFAULT_SHORTCUT_ATTACK_SELECTED_TARGET,
        KEY_SHORTCUT_SELECT_NEXT_TOWER to DEFAULT_SHORTCUT_SELECT_NEXT_TOWER,
        KEY_SHORTCUT_SELECT_PREVIOUS_TOWER to DEFAULT_SHORTCUT_SELECT_PREVIOUS_TOWER,
        KEY_SHORTCUT_AUTO_ATTACK_END_TURN to DEFAULT_SHORTCUT_AUTO_ATTACK_END_TURN,
        KEY_SHORTCUT_CHEAT to DEFAULT_SHORTCUT_CHEAT,
        KEY_SHORTCUT_TOGGLE_ENEMY_LIST to DEFAULT_SHORTCUT_TOGGLE_ENEMY_LIST,
        KEY_SHORTCUT_END_TURN_START_BATTLE to DEFAULT_SHORTCUT_END_TURN_START_BATTLE,
        KEY_SHORTCUT_SAVE_GAME to DEFAULT_SHORTCUT_SAVE_GAME,
        KEY_SHORTCUT_PAN_UP to DEFAULT_SHORTCUT_PAN_UP,
        KEY_SHORTCUT_PAN_DOWN to DEFAULT_SHORTCUT_PAN_DOWN,
        KEY_SHORTCUT_PAN_LEFT to DEFAULT_SHORTCUT_PAN_LEFT,
        KEY_SHORTCUT_PAN_RIGHT to DEFAULT_SHORTCUT_PAN_RIGHT,
        KEY_SHORTCUT_CENTER_SELECTED_TOWER to DEFAULT_SHORTCUT_CENTER_SELECTED_TOWER,
        KEY_SHORTCUT_CENTER_NEXT_SPAWN_POINT to DEFAULT_SHORTCUT_CENTER_NEXT_SPAWN_POINT,
        KEY_SHORTCUT_UPGRADE_SELECTED_TOWER to DEFAULT_SHORTCUT_UPGRADE_SELECTED_TOWER,
        KEY_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER to DEFAULT_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER,
        KEY_SHORTCUT_TOGGLE_SPELL_MENU to DEFAULT_SHORTCUT_TOGGLE_SPELL_MENU,
        KEY_SHORTCUT_SWITCH_TO_TOWER_MODE to DEFAULT_SHORTCUT_SWITCH_TO_TOWER_MODE,
        KEY_SHORTCUT_TOGGLE_AUDIO to DEFAULT_SHORTCUT_TOGGLE_AUDIO,
    )

/**
 * Serialize all relevant (non-debug, non-hint) settings into a flat map for remote storage.
 * Keys match the internal KEY_* constants; values are String representations.
 */
fun AppSettings.toSettingsMap(): Map<String, String> =
    buildMap {
        put(KEY_DARK_MODE, isDarkMode.value.toString())
        put(KEY_LANGUAGE, currentLanguage.value.code)
        put(KEY_SOUND_ENABLED, isSoundEnabled.value.toString())
        put(KEY_SOUND_VOLUME, soundVolume.value.toString())
        put(KEY_EFFECTS_ENABLED, isEffectsEnabled.value.toString())
        put(KEY_EFFECTS_VOLUME, effectsVolume.value.toString())
        put(KEY_MUSIC_ENABLED, isMusicEnabled.value.toString())
        put(KEY_MUSIC_VOLUME, musicVolume.value.toString())
        put(KEY_WORLDMAP_MUSIC_ENABLED, isWorldMapMusicEnabled.value.toString())
        put(KEY_WORLDMAP_MUSIC_VOLUME, worldMapMusicVolume.value.toString())
        put(KEY_GAMEPLAY_MUSIC_ENABLED, isGameplayMusicEnabled.value.toString())
        put(KEY_GAMEPLAY_MUSIC_VOLUME, gameplayMusicVolume.value.toString())
        put(KEY_SHOW_CONTROL_PAD, showControlPad.value.toString())
        put(KEY_DIFFICULTY, difficulty.value.name)
        put(KEY_USE_LEVEL_CARDS, useLevelCards.value.toString())
        put(KEY_USE_TILE_IMAGES, useTileImages.value.toString())
        put(KEY_USE_TILE_SMOOTH_TRANSITIONS, useTileSmoothTransitions.value.toString())
        put(KEY_SHOW_TESTING_LEVELS, showTestingLevels.value.toString())
        put(KEY_HEADER_TEXT_SIZE, headerTextSize.value.name)
        put(KEY_USE_LEVEL_MAP_IMAGE, useLevelMapImage.value.toString())
        put(KEY_ENABLE_ANIMATIONS, enableAnimations.value.toString())
        put(KEY_ENABLE_WORLDMAP_ANIMATIONS, enableWorldMapAnimations.value.toString())
        put(KEY_CHECK_FOR_UPDATES, checkForUpdates.value.toString())
        put(KEY_AUTO_JUMP_TO_NEXT_TOWER, autoJumpToNextTower.value.toString())
        put(KEY_SHOW_UNIT_TOWER_BACKGROUND, showUnitTowerBackground.value.toString())
        put(KEY_SPLIT_BUILD_TOWER_BUTTON, splitBuildTowerButton.value.toString())
        put(KEY_HIGH_CONTRAST, highContrastEnabled.value.toString())
        put(KEY_COLOR_BLIND_PALETTE, colorBlindPalette.value.name)
        put(KEY_CAPTIONS_ENABLED, captionsEnabled.value.toString())
        put(KEY_HOLD_TO_CONFIRM, holdToConfirmEnabled.value.toString())
        put(KEY_FONT_SIZE, fontSize.value.name)
        put(KEY_SHOW_BUTTON_SHORTCUT_HINTS, showButtonShortcutHints.value.toString())
        put(KEY_MOUSE_POINTER_SOURCE, mousePointerSource.value.name)
        put(KEY_MOUSE_POINTER_SIZE, mousePointerSize.value.name)
        put(KEY_MOUSE_POINTER_DIRECTION, mousePointerDirection.value.name)
        put(KEY_MOUSE_POINTER_SKIN_BRIGHTNESS, mousePointerSkinBrightness.value.toString())
        put(KEY_SHORTCUT_ATTACK_SELECTED_TARGET, shortcutAttackSelectedTarget.value)
        put(KEY_SHORTCUT_SELECT_NEXT_TOWER, shortcutSelectNextTower.value)
        put(KEY_SHORTCUT_SELECT_PREVIOUS_TOWER, shortcutSelectPreviousTower.value)
        put(KEY_SHORTCUT_AUTO_ATTACK_END_TURN, shortcutAutoAttackEndTurn.value)
        put(KEY_SHORTCUT_CHEAT, shortcutCheat.value)
        put(KEY_SHORTCUT_TOGGLE_ENEMY_LIST, shortcutToggleEnemyList.value)
        put(KEY_SHORTCUT_END_TURN_START_BATTLE, shortcutEndTurnStartBattle.value)
        put(KEY_SHORTCUT_SAVE_GAME, shortcutSaveGame.value)
        put(KEY_SHORTCUT_PAN_UP, shortcutPanUp.value)
        put(KEY_SHORTCUT_PAN_DOWN, shortcutPanDown.value)
        put(KEY_SHORTCUT_PAN_LEFT, shortcutPanLeft.value)
        put(KEY_SHORTCUT_PAN_RIGHT, shortcutPanRight.value)
        put(KEY_SHORTCUT_CENTER_SELECTED_TOWER, shortcutCenterSelectedTower.value)
        put(KEY_SHORTCUT_CENTER_NEXT_SPAWN_POINT, shortcutCenterNextSpawnPoint.value)
        put(KEY_SHORTCUT_UPGRADE_SELECTED_TOWER, shortcutUpgradeSelectedTower.value)
        put(KEY_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER, shortcutUndoOrSellSelectedTower.value)
        put(KEY_SHORTCUT_TOGGLE_SPELL_MENU, shortcutToggleSpellMenu.value)
        put(KEY_SHORTCUT_SWITCH_TO_TOWER_MODE, shortcutSwitchToTowerMode.value)
        put(KEY_SHORTCUT_NEXT_ENEMY_TARGET, shortcutNextEnemyTarget.value)
        put(KEY_SHORTCUT_PREV_ENEMY_TARGET, shortcutPrevEnemyTarget.value)
        put(KEY_SHORTCUT_BACK_TO_WORLDMAP, shortcutBackToWorldMap.value)
        put(KEY_SHORTCUT_TOGGLE_AUDIO, shortcutToggleAudio.value)
    }

/**
 * Apply settings from a remote map (downloaded from the backend).
 * Unknown keys are silently ignored for forward compatibility.
 * The [onPersist] callback is suppressed during application to prevent re-upload loops.
 */
fun AppSettings.applyFromSettingsMap(map: Map<String, String>) {
    val savedCallback = onPersist
    onPersist = null
    try {
        map[KEY_DARK_MODE]?.toBooleanStrictOrNull()?.let { saveDarkMode(it) }
        map[KEY_LANGUAGE]?.let { code ->
            AppLocale.entries.find { it.code == code }?.let { saveLanguage(it) }
        }
        map[KEY_SOUND_ENABLED]?.toBooleanStrictOrNull()?.let { saveSoundEnabled(it) }
        map[KEY_SOUND_VOLUME]?.toFloatOrNull()?.let { saveSoundVolume(it) }
        map[KEY_EFFECTS_ENABLED]?.toBooleanStrictOrNull()?.let { saveEffectsEnabled(it) }
        map[KEY_EFFECTS_VOLUME]?.toFloatOrNull()?.let { saveEffectsVolume(it) }
        map[KEY_MUSIC_ENABLED]?.toBooleanStrictOrNull()?.let { saveMusicEnabled(it) }
        map[KEY_MUSIC_VOLUME]?.toFloatOrNull()?.let { saveMusicVolume(it) }
        map[KEY_WORLDMAP_MUSIC_ENABLED]?.toBooleanStrictOrNull()?.let { saveWorldMapMusicEnabled(it) }
        map[KEY_WORLDMAP_MUSIC_VOLUME]?.toFloatOrNull()?.let { saveWorldMapMusicVolume(it) }
        map[KEY_GAMEPLAY_MUSIC_ENABLED]?.toBooleanStrictOrNull()?.let { saveGameplayMusicEnabled(it) }
        map[KEY_GAMEPLAY_MUSIC_VOLUME]?.toFloatOrNull()?.let { saveGameplayMusicVolume(it) }
        map[KEY_SHOW_CONTROL_PAD]?.toBooleanStrictOrNull()?.let { saveShowControlPad(it) }
        map[KEY_DIFFICULTY]?.let { name ->
            try {
                saveDifficulty(DifficultyLevel.valueOf(name))
            } catch (_: Exception) {
            }
        }
        map[KEY_USE_LEVEL_CARDS]?.toBooleanStrictOrNull()?.let { saveUseLevelCards(it) }
        map[KEY_USE_TILE_IMAGES]?.toBooleanStrictOrNull()?.let { saveUseTileImages(it) }
        map[KEY_USE_TILE_SMOOTH_TRANSITIONS]?.toBooleanStrictOrNull()?.let { saveUseTileSmoothTransitions(it) }
        map[KEY_SHOW_TESTING_LEVELS]?.toBooleanStrictOrNull()?.let { saveShowTestingLevels(it) }
        map[KEY_HEADER_TEXT_SIZE]?.let { name ->
            try {
                saveHeaderTextSize(HeaderTextSize.valueOf(name))
            } catch (_: Exception) {
            }
        }
        map[KEY_USE_LEVEL_MAP_IMAGE]?.toBooleanStrictOrNull()?.let { saveUseLevelMapImage(it) }
        map[KEY_ENABLE_ANIMATIONS]?.toBooleanStrictOrNull()?.let { saveEnableAnimations(it) }
        map[KEY_ENABLE_WORLDMAP_ANIMATIONS]?.toBooleanStrictOrNull()?.let { saveEnableWorldMapAnimations(it) }
        map[KEY_CHECK_FOR_UPDATES]?.toBooleanStrictOrNull()?.let { saveCheckForUpdates(it) }
        map[KEY_AUTO_JUMP_TO_NEXT_TOWER]?.toBooleanStrictOrNull()?.let { saveAutoJumpToNextTower(it) }
        map[KEY_SHOW_UNIT_TOWER_BACKGROUND]?.toBooleanStrictOrNull()?.let { saveShowUnitTowerBackground(it) }
        map[KEY_SPLIT_BUILD_TOWER_BUTTON]?.toBooleanStrictOrNull()?.let { saveSplitBuildTowerButton(it) }
        map[KEY_HIGH_CONTRAST]?.toBooleanStrictOrNull()?.let { saveHighContrastEnabled(it) }
        map[KEY_COLOR_BLIND_PALETTE]?.let { name ->
            try {
                saveColorBlindPalette(ColorBlindPalette.valueOf(name))
            } catch (_: Exception) {
            }
        }
        map[KEY_CAPTIONS_ENABLED]?.toBooleanStrictOrNull()?.let { saveCaptionsEnabled(it) }
        map[KEY_HOLD_TO_CONFIRM]?.toBooleanStrictOrNull()?.let { saveHoldToConfirmEnabled(it) }
        map[KEY_FONT_SIZE]?.let { name ->
            try {
                saveFontSize(FontSize.valueOf(name))
            } catch (_: Exception) {
            }
        }
        map[KEY_SHOW_BUTTON_SHORTCUT_HINTS]?.toBooleanStrictOrNull()?.let { saveShowButtonShortcutHints(it) }
        map[KEY_MOUSE_POINTER_SOURCE]?.let { name ->
            parseMousePointerSource(name)?.let { saveMousePointerSource(it) }
        }
        map[KEY_MOUSE_POINTER_SIZE]?.let { name ->
            parseMousePointerSize(name)?.let { saveMousePointerSize(it) }
        }
        map[KEY_MOUSE_POINTER_DIRECTION]?.let { name ->
            parseMousePointerDirection(name)?.let { saveMousePointerDirection(it) }
        }
        map[KEY_MOUSE_POINTER_SKIN_BRIGHTNESS]?.toFloatOrNull()?.let { saveMousePointerSkinBrightness(it) }
        map[KEY_SHORTCUT_ATTACK_SELECTED_TARGET]?.let { saveShortcutAttackSelectedTarget(it) }
        map[KEY_SHORTCUT_SELECT_NEXT_TOWER]?.let { saveShortcutSelectNextTower(it) }
        map[KEY_SHORTCUT_SELECT_PREVIOUS_TOWER]?.let { saveShortcutSelectPreviousTower(it) }
        map[KEY_SHORTCUT_AUTO_ATTACK_END_TURN]?.let { saveShortcutAutoAttackEndTurn(it) }
        map[KEY_SHORTCUT_CHEAT]?.let { saveShortcutCheat(it) }
        map[KEY_SHORTCUT_TOGGLE_ENEMY_LIST]?.let { saveShortcutToggleEnemyList(it) }
        map[KEY_SHORTCUT_END_TURN_START_BATTLE]?.let { saveShortcutEndTurnStartBattle(it) }
        map[KEY_SHORTCUT_SAVE_GAME]?.let { saveShortcutSaveGame(it) }
        map[KEY_SHORTCUT_PAN_UP]?.let { saveShortcutPanUp(it) }
        map[KEY_SHORTCUT_PAN_DOWN]?.let { saveShortcutPanDown(it) }
        map[KEY_SHORTCUT_PAN_LEFT]?.let { saveShortcutPanLeft(it) }
        map[KEY_SHORTCUT_PAN_RIGHT]?.let { saveShortcutPanRight(it) }
        map[KEY_SHORTCUT_CENTER_SELECTED_TOWER]?.let { saveShortcutCenterSelectedTower(it) }
        map[KEY_SHORTCUT_CENTER_NEXT_SPAWN_POINT]?.let { saveShortcutCenterNextSpawnPoint(it) }
        map[KEY_SHORTCUT_UPGRADE_SELECTED_TOWER]?.let { saveShortcutUpgradeSelectedTower(it) }
        map[KEY_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER]?.let { saveShortcutUndoOrSellSelectedTower(it) }
        map[KEY_SHORTCUT_TOGGLE_SPELL_MENU]?.let { saveShortcutToggleSpellMenu(it) }
        map[KEY_SHORTCUT_SWITCH_TO_TOWER_MODE]?.let { saveShortcutSwitchToTowerMode(it) }
        map[KEY_SHORTCUT_TOGGLE_AUDIO]?.let { saveShortcutToggleAudio(it) }
    } finally {
        onPersist = savedCallback
    }
}

private fun AppSettings.parseMousePointerSource(name: String): MousePointerSource? =
    when (name) {
        "DEFAULT" -> MousePointerSource.GAME
        else -> MousePointerSource.entries.firstOrNull { it.name == name }
    }

private fun AppSettings.parseMousePointerSize(name: String): MousePointerSize? =
    when (name) {
        "DEFAULT" -> MousePointerSize.DEFAULT
        else -> MousePointerSize.entries.firstOrNull { it.name == name }
    }

private fun AppSettings.parseMousePointerDirection(name: String): MousePointerDirection? =
    when (name) {
        "DEFAULT" -> MousePointerDirection.DEFAULT
        else -> MousePointerDirection.entries.firstOrNull { it.name == name }
    }

/**
 * Reset all settings to defaults
 */
fun AppSettings.resetToDefaults() {
    // Reset dark mode
    saveDarkMode(false)

    // Reset language to default
    saveLanguage(AppLocale.DEFAULT)

    // Reset sound settings
    saveSoundEnabled(true)
    saveSoundVolume(0.7f)
    saveEffectsEnabled(true)
    saveEffectsVolume(0.7f)
    saveMusicEnabled(true)
    saveMusicVolume(0.5f)
    saveWorldMapMusicEnabled(true)
    saveWorldMapMusicVolume(0.7f)
    saveGameplayMusicEnabled(true)
    saveGameplayMusicVolume(0.5f)

    // Reset control pad to platform default
    saveShowControlPad(isPlatformMobile)

    // Reset difficulty to default
    saveDifficulty(DifficultyLevel.DEFAULT)

    // Reset world map style to image map
    saveUseLevelCards(false)

    // Reset tile images to ON
    saveUseTileImages(true)

    // Reset tile smooth transitions to ON
    saveUseTileSmoothTransitions(true)

    // Reset show testing levels to OFF
    saveShowTestingLevels(false)

    // Reset header text size to default (SMALL)
    saveHeaderTextSize(HeaderTextSize.DEFAULT)

    // Reset animations to ON
    saveEnableAnimations(true)
    saveEnableWorldMapAnimations(true)

    // Reset check for updates to ON
    saveCheckForUpdates(true)

    // Reset unit/tower background to OFF
    saveShowUnitTowerBackground(false)

    // Reset accessibility preferences
    saveHighContrastEnabled(false)
    saveColorBlindPalette(ColorBlindPalette.OFF)
    saveCaptionsEnabled(false)
    saveHoldToConfirmEnabled(false)
    saveFontSize(FontSize.DEFAULT)
    saveShowButtonShortcutHints(false)
    saveMousePointerSource(MousePointerSource.DEFAULT)
    saveMousePointerSize(MousePointerSize.DEFAULT)
    saveMousePointerDirection(MousePointerDirection.DEFAULT)
    saveMousePointerSkinBrightness(0f)
    resetShortcutBindings()

    // Note: Don't reset settings hint shown state when resetting settings
    // as user has already seen it once
}
