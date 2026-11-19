package de.egril.defender.ui.settings

import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import com.russhwolf.settings.set
import de.egril.defender.ui.a11y.ColorBlindPalette

/**
 * Save dark mode preference
 */
fun AppSettings.saveDarkMode(enabled: Boolean) {
    isDarkMode.value = enabled
    settings[KEY_DARK_MODE] = enabled
    onPersist?.invoke()
}

/**
 * Save language preference
 */
fun AppSettings.saveLanguage(locale: AppLocale) {
    currentLanguage.value = locale
    settings[KEY_LANGUAGE] = locale.code
    onPersist?.invoke()
}

/**
 * Save sound enabled preference
 */
fun AppSettings.saveSoundEnabled(enabled: Boolean) {
    isSoundEnabled.value = enabled
    settings[KEY_SOUND_ENABLED] = enabled
    onPersist?.invoke()
}

/**
 * Save sound volume preference
 */
fun AppSettings.saveSoundVolume(volume: Float) {
    val clampedVolume = volume.coerceIn(0f, 1f)
    soundVolume.value = clampedVolume
    settings.putFloat(KEY_SOUND_VOLUME, clampedVolume)
    onPersist?.invoke()
}

/**
 * Save effect sounds enabled preference
 */
fun AppSettings.saveEffectsEnabled(enabled: Boolean) {
    isEffectsEnabled.value = enabled
    settings.putBoolean(KEY_EFFECTS_ENABLED, enabled)
    onPersist?.invoke()
}

/**
 * Save effect sounds volume preference
 */
fun AppSettings.saveEffectsVolume(volume: Float) {
    val clampedVolume = volume.coerceIn(0f, 1f)
    effectsVolume.value = clampedVolume
    settings.putFloat(KEY_EFFECTS_VOLUME, clampedVolume)
    onPersist?.invoke()
}

/**
 * Save background music enabled preference
 */
fun AppSettings.saveMusicEnabled(enabled: Boolean) {
    isMusicEnabled.value = enabled
    settings.putBoolean(KEY_MUSIC_ENABLED, enabled)
    onPersist?.invoke()
}

/**
 * Save background music volume preference
 */
fun AppSettings.saveMusicVolume(volume: Float) {
    val clampedVolume = volume.coerceIn(0f, 1f)
    musicVolume.value = clampedVolume
    settings.putFloat(KEY_MUSIC_VOLUME, clampedVolume)
    onPersist?.invoke()
}

/**
 * Save world map music enabled preference
 */
fun AppSettings.saveWorldMapMusicEnabled(enabled: Boolean) {
    isWorldMapMusicEnabled.value = enabled
    settings.putBoolean(KEY_WORLDMAP_MUSIC_ENABLED, enabled)
    onPersist?.invoke()
}

/**
 * Save world map music volume preference
 */
fun AppSettings.saveWorldMapMusicVolume(volume: Float) {
    val clampedVolume = volume.coerceIn(0f, 1f)
    worldMapMusicVolume.value = clampedVolume
    settings.putFloat(KEY_WORLDMAP_MUSIC_VOLUME, clampedVolume)
    onPersist?.invoke()
}

/**
 * Save gameplay music enabled preference
 */
fun AppSettings.saveGameplayMusicEnabled(enabled: Boolean) {
    isGameplayMusicEnabled.value = enabled
    settings.putBoolean(KEY_GAMEPLAY_MUSIC_ENABLED, enabled)
    onPersist?.invoke()
}

/**
 * Save gameplay music volume preference
 */
fun AppSettings.saveGameplayMusicVolume(volume: Float) {
    val clampedVolume = volume.coerceIn(0f, 1f)
    gameplayMusicVolume.value = clampedVolume
    settings.putFloat(KEY_GAMEPLAY_MUSIC_VOLUME, clampedVolume)
    onPersist?.invoke()
}

/**
 * Save control pad visibility preference
 */
fun AppSettings.saveShowControlPad(show: Boolean) {
    showControlPad.value = show
    settings.putBoolean(KEY_SHOW_CONTROL_PAD, show)
    onPersist?.invoke()
}

/**
 * Save difficulty preference
 */
fun AppSettings.saveDifficulty(level: DifficultyLevel) {
    difficulty.value = level
    settings[KEY_DIFFICULTY] = level.name
    onPersist?.invoke()
}

/**
 * Save world map display style preference
 */
fun AppSettings.saveUseLevelCards(useLevelCards: Boolean) {
    this.useLevelCards.value = useLevelCards
    settings.putBoolean(KEY_USE_LEVEL_CARDS, useLevelCards)
    onPersist?.invoke()
}

/**
 * Mark settings hint as shown
 */
fun AppSettings.markSettingsHintShown() {
    settingsHintShown.value = true
    try {
        settings.putBoolean(KEY_SETTINGS_HINT_SHOWN, true)
    } catch (_: Throwable) {
        // localStorage quota exceeded or unavailable; in-memory state is already updated
    }
}

/**
 * Mark accessibility banner as shown
 */
fun AppSettings.markAccessibilityBannerShown() {
    accessibilityBannerShown.value = true
    try {
        settings.putBoolean(KEY_ACCESSIBILITY_BANNER_SHOWN, true)
    } catch (_: Throwable) {
        // localStorage quota exceeded or unavailable; in-memory state is already updated
    }
}

/**
 * Record that the daily hint banner has been shown today.
 *
 * @param date YYYY-MM-DD calendar date when the banner was shown.
 * @param index Index of the hint that was selected so the next call can rotate.
 */
fun AppSettings.markDailyHintShown(
    date: String,
    index: Int,
) {
    dailyHintLastShownDate.value = date
    dailyHintLastIndex.value = index
    try {
        settings[KEY_DAILY_HINT_LAST_SHOWN_DATE] = date
        settings.putInt(KEY_DAILY_HINT_LAST_INDEX, index)
    } catch (_: Throwable) {
        // localStorage quota exceeded or unavailable; in-memory state is already updated
    }
}

/**
 * Save tile background images preference
 */
fun AppSettings.saveUseTileImages(useTiles: Boolean) {
    useTileImages.value = useTiles
    settings.putBoolean(KEY_USE_TILE_IMAGES, useTiles)
    onPersist?.invoke()
}

/**
 * Save tile smooth transitions preference
 * Note: The value is saved regardless of useTileImages state, but only used when useTileImages is true
 */
fun AppSettings.saveUseTileSmoothTransitions(useTransitions: Boolean) {
    useTileSmoothTransitions.value = useTransitions
    settings.putBoolean(KEY_USE_TILE_SMOOTH_TRANSITIONS, useTransitions)
    onPersist?.invoke()
}

/**
 * Save show testing levels preference
 */
fun AppSettings.saveShowTestingLevels(show: Boolean) {
    showTestingLevels.value = show
    settings.putBoolean(KEY_SHOW_TESTING_LEVELS, show)
    onPersist?.invoke()
}

/**
 * Save level header text size preference
 */
fun AppSettings.saveHeaderTextSize(size: HeaderTextSize) {
    headerTextSize.value = size
    settings[KEY_HEADER_TEXT_SIZE] = size.name
    onPersist?.invoke()
}

fun AppSettings.saveUseLevelMapImage(use: Boolean) {
    useLevelMapImage.value = use
    settings.putBoolean(KEY_USE_LEVEL_MAP_IMAGE, use)
    onPersist?.invoke()
}

fun AppSettings.saveShowDebugOptions(show: Boolean) {
    showDebugOptions.value = show
    settings.putBoolean(KEY_SHOW_DEBUG_OPTIONS, show)
}

/**
 * Save enable animations preference
 */
fun AppSettings.saveEnableAnimations(enabled: Boolean) {
    enableAnimations.value = enabled
    settings.putBoolean(KEY_ENABLE_ANIMATIONS, enabled)
    onPersist?.invoke()
}

/**
 * Save world map animations preference
 */
fun AppSettings.saveEnableWorldMapAnimations(enabled: Boolean) {
    enableWorldMapAnimations.value = enabled
    settings.putBoolean(KEY_ENABLE_WORLDMAP_ANIMATIONS, enabled)
    onPersist?.invoke()
}

/**
 * Save check for updates preference
 */
fun AppSettings.saveCheckForUpdates(enabled: Boolean) {
    checkForUpdates.value = enabled
    settings.putBoolean(KEY_CHECK_FOR_UPDATES, enabled)
    onPersist?.invoke()
}

/**
 * Save auto-jump to next tower preference
 */
fun AppSettings.saveAutoJumpToNextTower(enabled: Boolean) {
    autoJumpToNextTower.value = enabled
    settings.putBoolean(KEY_AUTO_JUMP_TO_NEXT_TOWER, enabled)
    onPersist?.invoke()
}

/**
 * Save show unit/tower background color preference
 */
fun AppSettings.saveShowUnitTowerBackground(enabled: Boolean) {
    showUnitTowerBackground.value = enabled
    settings.putBoolean(KEY_SHOW_UNIT_TOWER_BACKGROUND, enabled)
    onPersist?.invoke()
}

/**
 * Save split build-tower button preference
 */
fun AppSettings.saveSplitBuildTowerButton(enabled: Boolean) {
    splitBuildTowerButton.value = enabled
    settings.putBoolean(KEY_SPLIT_BUILD_TOWER_BUTTON, enabled)
    onPersist?.invoke()
}

fun AppSettings.saveHighContrastEnabled(enabled: Boolean) {
    highContrastEnabled.value = enabled
    settings.putBoolean(KEY_HIGH_CONTRAST, enabled)
    onPersist?.invoke()
}

fun AppSettings.saveColorBlindPalette(palette: ColorBlindPalette) {
    colorBlindPalette.value = palette
    settings[KEY_COLOR_BLIND_PALETTE] = palette.name
    onPersist?.invoke()
}

fun AppSettings.saveCaptionsEnabled(enabled: Boolean) {
    captionsEnabled.value = enabled
    settings.putBoolean(KEY_CAPTIONS_ENABLED, enabled)
    onPersist?.invoke()
}

fun AppSettings.saveHoldToConfirmEnabled(enabled: Boolean) {
    holdToConfirmEnabled.value = enabled
    settings.putBoolean(KEY_HOLD_TO_CONFIRM, enabled)
    onPersist?.invoke()
}

fun AppSettings.saveFontSize(size: FontSize) {
    fontSize.value = size
    settings[KEY_FONT_SIZE] = size.name
    onPersist?.invoke()
}

fun AppSettings.saveShowButtonShortcutHints(enabled: Boolean) {
    showButtonShortcutHints.value = enabled
    settings.putBoolean(KEY_SHOW_BUTTON_SHORTCUT_HINTS, enabled)
    onPersist?.invoke()
}

fun AppSettings.saveMousePointerSource(source: MousePointerSource) {
    mousePointerSource.value = source
    settings[KEY_MOUSE_POINTER_SOURCE] = source.name
    onPersist?.invoke()
}

fun AppSettings.saveMousePointerSize(size: MousePointerSize) {
    mousePointerSize.value = size
    settings[KEY_MOUSE_POINTER_SIZE] = size.name
    onPersist?.invoke()
}

fun AppSettings.saveMousePointerDirection(direction: MousePointerDirection) {
    mousePointerDirection.value = direction
    settings[KEY_MOUSE_POINTER_DIRECTION] = direction.name
    onPersist?.invoke()
}

fun AppSettings.saveMousePointerSkinBrightness(brightness: Float) {
    val clamped = brightness.coerceIn(MOUSE_POINTER_SKIN_BRIGHTNESS_MIN, MOUSE_POINTER_SKIN_BRIGHTNESS_MAX)
    mousePointerSkinBrightness.value = clamped
    settings.putFloat(KEY_MOUSE_POINTER_SKIN_BRIGHTNESS, clamped)
    onPersist?.invoke()
}

fun AppSettings.saveShortcutAttackSelectedTarget(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_ATTACK_SELECTED_TARGET)
    shortcutAttackSelectedTarget.value = normalized
    settings[KEY_SHORTCUT_ATTACK_SELECTED_TARGET] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutSelectNextTower(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_SELECT_NEXT_TOWER)
    shortcutSelectNextTower.value = normalized
    settings[KEY_SHORTCUT_SELECT_NEXT_TOWER] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutSelectPreviousTower(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_SELECT_PREVIOUS_TOWER)
    shortcutSelectPreviousTower.value = normalized
    settings[KEY_SHORTCUT_SELECT_PREVIOUS_TOWER] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutAutoAttackEndTurn(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_AUTO_ATTACK_END_TURN)
    shortcutAutoAttackEndTurn.value = normalized
    settings[KEY_SHORTCUT_AUTO_ATTACK_END_TURN] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutCheat(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_CHEAT)
    shortcutCheat.value = normalized
    settings[KEY_SHORTCUT_CHEAT] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutToggleEnemyList(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_TOGGLE_ENEMY_LIST)
    shortcutToggleEnemyList.value = normalized
    settings[KEY_SHORTCUT_TOGGLE_ENEMY_LIST] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutEndTurnStartBattle(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_END_TURN_START_BATTLE)
    shortcutEndTurnStartBattle.value = normalized
    settings[KEY_SHORTCUT_END_TURN_START_BATTLE] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutSaveGame(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_SAVE_GAME)
    shortcutSaveGame.value = normalized
    settings[KEY_SHORTCUT_SAVE_GAME] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutPanUp(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PAN_UP)
    shortcutPanUp.value = normalized
    settings[KEY_SHORTCUT_PAN_UP] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutPanDown(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PAN_DOWN)
    shortcutPanDown.value = normalized
    settings[KEY_SHORTCUT_PAN_DOWN] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutPanLeft(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PAN_LEFT)
    shortcutPanLeft.value = normalized
    settings[KEY_SHORTCUT_PAN_LEFT] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutPanRight(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PAN_RIGHT)
    shortcutPanRight.value = normalized
    settings[KEY_SHORTCUT_PAN_RIGHT] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutCenterSelectedTower(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_CENTER_SELECTED_TOWER)
    shortcutCenterSelectedTower.value = normalized
    settings[KEY_SHORTCUT_CENTER_SELECTED_TOWER] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutCenterNextSpawnPoint(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_CENTER_NEXT_SPAWN_POINT)
    shortcutCenterNextSpawnPoint.value = normalized
    settings[KEY_SHORTCUT_CENTER_NEXT_SPAWN_POINT] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutUpgradeSelectedTower(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_UPGRADE_SELECTED_TOWER)
    shortcutUpgradeSelectedTower.value = normalized
    settings[KEY_SHORTCUT_UPGRADE_SELECTED_TOWER] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutUndoOrSellSelectedTower(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER)
    shortcutUndoOrSellSelectedTower.value = normalized
    settings[KEY_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutToggleSpellMenu(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_TOGGLE_SPELL_MENU)
    shortcutToggleSpellMenu.value = normalized
    settings[KEY_SHORTCUT_TOGGLE_SPELL_MENU] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutSwitchToTowerMode(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_SWITCH_TO_TOWER_MODE)
    shortcutSwitchToTowerMode.value = normalized
    settings[KEY_SHORTCUT_SWITCH_TO_TOWER_MODE] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutNextEnemyTarget(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_NEXT_ENEMY_TARGET)
    shortcutNextEnemyTarget.value = normalized
    settings[KEY_SHORTCUT_NEXT_ENEMY_TARGET] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutPrevEnemyTarget(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PREV_ENEMY_TARGET)
    shortcutPrevEnemyTarget.value = normalized
    settings[KEY_SHORTCUT_PREV_ENEMY_TARGET] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutBackToWorldMap(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_BACK_TO_WORLDMAP)
    shortcutBackToWorldMap.value = normalized
    settings[KEY_SHORTCUT_BACK_TO_WORLDMAP] = normalized
    onPersist?.invoke()
}

fun AppSettings.saveShortcutToggleAudio(shortcut: String) {
    val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_TOGGLE_AUDIO)
    shortcutToggleAudio.value = normalized
    settings[KEY_SHORTCUT_TOGGLE_AUDIO] = normalized
    onPersist?.invoke()
}

fun AppSettings.resetShortcutBindings() {
    saveShortcutAttackSelectedTarget(DEFAULT_SHORTCUT_ATTACK_SELECTED_TARGET)
    saveShortcutSelectNextTower(DEFAULT_SHORTCUT_SELECT_NEXT_TOWER)
    saveShortcutSelectPreviousTower(DEFAULT_SHORTCUT_SELECT_PREVIOUS_TOWER)
    saveShortcutAutoAttackEndTurn(DEFAULT_SHORTCUT_AUTO_ATTACK_END_TURN)
    saveShortcutCheat(DEFAULT_SHORTCUT_CHEAT)
    saveShortcutToggleEnemyList(DEFAULT_SHORTCUT_TOGGLE_ENEMY_LIST)
    saveShortcutEndTurnStartBattle(DEFAULT_SHORTCUT_END_TURN_START_BATTLE)
    saveShortcutSaveGame(DEFAULT_SHORTCUT_SAVE_GAME)
    saveShortcutPanUp(DEFAULT_SHORTCUT_PAN_UP)
    saveShortcutPanDown(DEFAULT_SHORTCUT_PAN_DOWN)
    saveShortcutPanLeft(DEFAULT_SHORTCUT_PAN_LEFT)
    saveShortcutPanRight(DEFAULT_SHORTCUT_PAN_RIGHT)
    saveShortcutCenterSelectedTower(DEFAULT_SHORTCUT_CENTER_SELECTED_TOWER)
    saveShortcutCenterNextSpawnPoint(DEFAULT_SHORTCUT_CENTER_NEXT_SPAWN_POINT)
    saveShortcutUpgradeSelectedTower(DEFAULT_SHORTCUT_UPGRADE_SELECTED_TOWER)
    saveShortcutUndoOrSellSelectedTower(DEFAULT_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER)
    saveShortcutToggleSpellMenu(DEFAULT_SHORTCUT_TOGGLE_SPELL_MENU)
    saveShortcutSwitchToTowerMode(DEFAULT_SHORTCUT_SWITCH_TO_TOWER_MODE)
    saveShortcutNextEnemyTarget(DEFAULT_SHORTCUT_NEXT_ENEMY_TARGET)
    saveShortcutPrevEnemyTarget(DEFAULT_SHORTCUT_PREV_ENEMY_TARGET)
    saveShortcutBackToWorldMap(DEFAULT_SHORTCUT_BACK_TO_WORLDMAP)
    saveShortcutToggleAudio(DEFAULT_SHORTCUT_TOGGLE_AUDIO)
}
