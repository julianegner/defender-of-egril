package de.egril.defender.ui.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import de.egril.defender.ui.a11y.AccessibilityPreferences
import de.egril.defender.ui.a11y.ColorBlindPalette
import de.egril.defender.utils.getOrCreateInstallUuid
import de.egril.defender.utils.isPlatformMobile

/**
 * Game difficulty levels
 */
enum class DifficultyLevel {
    BABY,
    EASY,
    MEDIUM,
    HARD,
    NIGHTMARE,
    ;

    companion object {
        val DEFAULT = MEDIUM
    }
}

/**
 * Level header text size options
 */
enum class HeaderTextSize {
    SMALL,
    MEDIUM,
    LARGE,
    ;

    companion object {
        val DEFAULT = SMALL
    }
}

/**
 * Accessibility font size options.
 * Controls the scale of most text in the app (except game title, level header, and defender buttons).
 */
enum class FontSize(
    val scale: Float,
) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.30f),
    HUGE(1.45f),
    ;

    companion object {
        val DEFAULT = MEDIUM
    }
}

/**
 * Manages application settings using multiplatform-settings library
 * Persists dark mode preference, language selection, sound settings, control pad visibility, difficulty level, and world map style
 */
object AppSettings {
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_LANGUAGE_CHOSEN = "language_chosen"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_SOUND_VOLUME = "sound_volume"
    private const val KEY_EFFECTS_ENABLED = "effects_enabled"
    private const val KEY_EFFECTS_VOLUME = "effects_volume"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_MUSIC_VOLUME = "music_volume"
    private const val KEY_WORLDMAP_MUSIC_ENABLED = "worldmap_music_enabled"
    private const val KEY_WORLDMAP_MUSIC_VOLUME = "worldmap_music_volume"
    private const val KEY_GAMEPLAY_MUSIC_ENABLED = "gameplay_music_enabled"
    private const val KEY_GAMEPLAY_MUSIC_VOLUME = "gameplay_music_volume"
    private const val KEY_SHOW_CONTROL_PAD = "show_control_pad"
    private const val KEY_DIFFICULTY = "difficulty"
    private const val KEY_USE_LEVEL_CARDS = "use_level_cards"
    private const val KEY_SETTINGS_HINT_SHOWN = "settings_hint_shown"
    private const val KEY_ACCESSIBILITY_BANNER_SHOWN = "accessibility_banner_shown"
    private const val KEY_DAILY_HINT_LAST_SHOWN_DATE = "daily_hint_last_shown_date"
    private const val KEY_DAILY_HINT_LAST_INDEX = "daily_hint_last_index"
    private const val KEY_USE_TILE_IMAGES = "use_tile_images"
    private const val KEY_USE_TILE_SMOOTH_TRANSITIONS = "use_tile_smooth_transitions"
    private const val KEY_SHOW_TESTING_LEVELS = "show_testing_levels"
    private const val KEY_HEADER_TEXT_SIZE = "header_text_size"
    private const val KEY_USE_LEVEL_MAP_IMAGE = "use_level_map_image"
    private const val KEY_SHOW_DEBUG_OPTIONS = "show_debug_options"
    private const val KEY_ENABLE_ANIMATIONS = "enable_animations"
    private const val KEY_ENABLE_WORLDMAP_ANIMATIONS = "enable_worldmap_animations"
    private const val KEY_CHECK_FOR_UPDATES = "check_for_updates"
    private const val KEY_AUTO_JUMP_TO_NEXT_TOWER = "auto_jump_to_next_tower"
    private const val KEY_SHOW_UNIT_TOWER_BACKGROUND = "show_unit_tower_background"
    private const val KEY_SPLIT_BUILD_TOWER_BUTTON = "split_build_tower_button"
    private const val KEY_HIGH_CONTRAST = "high_contrast"
    private const val KEY_COLOR_BLIND_PALETTE = "color_blind_palette"
    private const val KEY_CAPTIONS_ENABLED = "captions_enabled"
    private const val KEY_HOLD_TO_CONFIRM = "hold_to_confirm"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_SHOW_BUTTON_SHORTCUT_HINTS = "show_button_shortcut_hints"
    private const val KEY_SHORTCUT_ATTACK_SELECTED_TARGET = "shortcut_attack_selected_target"
    private const val KEY_SHORTCUT_SELECT_NEXT_TOWER = "shortcut_select_next_tower"
    private const val KEY_SHORTCUT_SELECT_PREVIOUS_TOWER = "shortcut_select_previous_tower"
    private const val KEY_SHORTCUT_AUTO_ATTACK_END_TURN = "shortcut_auto_attack_end_turn"
    private const val KEY_SHORTCUT_CHEAT = "shortcut_cheat"
    private const val KEY_SHORTCUT_TOGGLE_ENEMY_LIST = "shortcut_toggle_enemy_list"
    private const val KEY_SHORTCUT_END_TURN_START_BATTLE = "shortcut_end_turn_start_battle"
    private const val KEY_SHORTCUT_SAVE_GAME = "shortcut_save_game"
    private const val KEY_SHORTCUT_PAN_UP = "shortcut_pan_up"
    private const val KEY_SHORTCUT_PAN_DOWN = "shortcut_pan_down"
    private const val KEY_SHORTCUT_PAN_LEFT = "shortcut_pan_left"
    private const val KEY_SHORTCUT_PAN_RIGHT = "shortcut_pan_right"
    private const val KEY_SHORTCUT_CENTER_SELECTED_TOWER = "shortcut_center_selected_tower"
    private const val KEY_SHORTCUT_CENTER_NEXT_SPAWN_POINT = "shortcut_center_next_spawn_point"
    private const val KEY_SHORTCUT_UPGRADE_SELECTED_TOWER = "shortcut_upgrade_selected_tower"
    private const val KEY_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER = "shortcut_undo_or_sell_selected_tower"
    private const val KEY_SHORTCUT_TOGGLE_SPELL_MENU = "shortcut_toggle_spell_menu"
    private const val KEY_SHORTCUT_SWITCH_TO_TOWER_MODE = "shortcut_switch_to_tower_mode"
    private const val KEY_SHORTCUT_NEXT_ENEMY_TARGET = "shortcut_next_enemy_target"
    private const val KEY_SHORTCUT_PREV_ENEMY_TARGET = "shortcut_prev_enemy_target"
    private const val KEY_SHORTCUT_BACK_TO_WORLDMAP = "shortcut_back_to_worldmap"
    private const val KEY_SHORTCUT_TOGGLE_AUDIO = "shortcut_toggle_audio"
    private const val DEFAULT_SHORTCUT_ATTACK_SELECTED_TARGET = "F"
    private const val DEFAULT_SHORTCUT_SELECT_NEXT_TOWER = "Tab"
    private const val DEFAULT_SHORTCUT_SELECT_PREVIOUS_TOWER = "Shift+Tab"
    private const val DEFAULT_SHORTCUT_AUTO_ATTACK_END_TURN = "Ctrl+A"
    private const val DEFAULT_SHORTCUT_CHEAT = "C"
    private const val DEFAULT_SHORTCUT_TOGGLE_ENEMY_LIST = "E"
    private const val DEFAULT_SHORTCUT_END_TURN_START_BATTLE = "Ctrl+Enter"
    private const val DEFAULT_SHORTCUT_SAVE_GAME = "Ctrl+S"
    private const val DEFAULT_SHORTCUT_PAN_UP = "W"
    private const val DEFAULT_SHORTCUT_PAN_DOWN = "S"
    private const val DEFAULT_SHORTCUT_PAN_LEFT = "A"
    private const val DEFAULT_SHORTCUT_PAN_RIGHT = "D"
    private const val DEFAULT_SHORTCUT_CENTER_SELECTED_TOWER = "R"
    private const val DEFAULT_SHORTCUT_CENTER_NEXT_SPAWN_POINT = "G"
    private const val DEFAULT_SHORTCUT_UPGRADE_SELECTED_TOWER = "U"
    private const val DEFAULT_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER = "X"
    private const val DEFAULT_SHORTCUT_TOGGLE_SPELL_MENU = "M"
    private const val DEFAULT_SHORTCUT_SWITCH_TO_TOWER_MODE = "T"
    private const val DEFAULT_SHORTCUT_NEXT_ENEMY_TARGET = "N"
    private const val DEFAULT_SHORTCUT_PREV_ENEMY_TARGET = "Shift+N"
    private const val DEFAULT_SHORTCUT_BACK_TO_WORLDMAP = "Escape"
    private const val DEFAULT_SHORTCUT_TOGGLE_AUDIO = "P"

    private val settings: Settings = Settings()

    /**
     * Optional callback invoked after any setting is persisted (excluding debug/hint-only fields).
     * Set by GameViewModel to trigger a remote settings upload whenever settings change.
     */
    var onPersist: (() -> Unit)? = null

    /**
     * Dark mode state - automatically saved when changed
     */
    val isDarkMode: MutableState<Boolean> = mutableStateOf(settings[KEY_DARK_MODE, false])

    /**
     * Sound enabled state - automatically saved when changed (master control)
     */
    val isSoundEnabled: MutableState<Boolean> = mutableStateOf(settings[KEY_SOUND_ENABLED, true])

    /**
     * Sound volume level (0.0 to 1.0) - automatically saved when changed (master volume)
     */
    val soundVolume: MutableState<Float> = mutableStateOf(settings.getFloat(KEY_SOUND_VOLUME, 0.7f))

    /**
     * Effect sounds enabled state - automatically saved when changed
     */
    val isEffectsEnabled: MutableState<Boolean> = mutableStateOf(settings.getBoolean(KEY_EFFECTS_ENABLED, true))

    /**
     * Effect sounds volume level (0.0 to 1.0) - automatically saved when changed
     */
    val effectsVolume: MutableState<Float> = mutableStateOf(settings.getFloat(KEY_EFFECTS_VOLUME, 0.7f))

    /**
     * Background music enabled state - automatically saved when changed
     */
    val isMusicEnabled: MutableState<Boolean> = mutableStateOf(settings.getBoolean(KEY_MUSIC_ENABLED, true))

    /**
     * Background music volume level (0.0 to 1.0) - automatically saved when changed
     * Default is 1.0 (full background music volume, scaled further by per-category volume settings)
     */
    val musicVolume: MutableState<Float> = mutableStateOf(settings.getFloat(KEY_MUSIC_VOLUME, 1.0f))

    /**
     * World map music enabled state - automatically saved when changed
     */
    val isWorldMapMusicEnabled: MutableState<Boolean> = mutableStateOf(settings.getBoolean(KEY_WORLDMAP_MUSIC_ENABLED, true))

    /**
     * World map music volume level (0.0 to 1.0) - automatically saved when changed
     * Default is 0.7 (louder than gameplay music)
     */
    val worldMapMusicVolume: MutableState<Float> = mutableStateOf(settings.getFloat(KEY_WORLDMAP_MUSIC_VOLUME, 0.7f))

    /**
     * Gameplay music enabled state - automatically saved when changed
     */
    val isGameplayMusicEnabled: MutableState<Boolean> = mutableStateOf(settings.getBoolean(KEY_GAMEPLAY_MUSIC_ENABLED, true))

    /**
     * Gameplay music volume level (0.0 to 1.0) - automatically saved when changed
     * Default is 0.5 (quieter than world map music)
     */
    val gameplayMusicVolume: MutableState<Float> = mutableStateOf(settings.getFloat(KEY_GAMEPLAY_MUSIC_VOLUME, 0.5f))

    /**
     * Control pad visibility state - automatically saved when changed
     * Default is ON for mobile platforms, OFF for desktop/web
     */
    val showControlPad: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_SHOW_CONTROL_PAD, isPlatformMobile),
        )

    /**
     * Game difficulty level - automatically saved when changed
     * Default is MEDIUM
     */
    val difficulty: MutableState<DifficultyLevel> =
        mutableStateOf(
            try {
                DifficultyLevel.valueOf(settings[KEY_DIFFICULTY, DifficultyLevel.DEFAULT.name])
            } catch (e: Exception) {
                DifficultyLevel.DEFAULT
            },
        )

    /**
     * World map display style - use level cards instead of image map
     * Default is false (use image-based map)
     */
    val useLevelCards: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_USE_LEVEL_CARDS, false),
        )

    /**
     * Settings hint shown state - track if first-time settings hint has been shown
     * Default is false (hint should be shown on first run)
     */
    val settingsHintShown: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_SETTINGS_HINT_SHOWN, false),
        )

    /**
     * Accessibility banner shown state - track if first-time accessibility banner has been shown
     * Default is false (banner should be shown on first run)
     */
    val accessibilityBannerShown: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_ACCESSIBILITY_BANNER_SHOWN, false),
        )

    /**
     * Date (YYYY-MM-DD) when the daily hint banner on the world map was last shown.
     * Empty string means it has never been shown. Used to ensure the banner appears
     * at most once per calendar day.
     */
    val dailyHintLastShownDate: MutableState<String> =
        mutableStateOf(
            settings[KEY_DAILY_HINT_LAST_SHOWN_DATE, ""],
        )

    /**
     * Index of the daily hint that was last shown. The next eligible hint is selected
     * starting from (lastIndex + 1) modulo hint count, so users see hints in rotation.
     */
    val dailyHintLastIndex: MutableState<Int> =
        mutableStateOf(
            settings.getInt(KEY_DAILY_HINT_LAST_INDEX, -1),
        )

    /**
     * Use tile background images - show tile images instead of solid colors
     * Default is true (tile images ON)
     */
    val useTileImages: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_USE_TILE_IMAGES, true),
        )

    /**
     * Use tile smooth transitions - blend adjacent tiles for smoother visual transitions
     * Only applies when useTileImages is true
     * Default is true (smooth transitions ON)
     */
    val useTileSmoothTransitions: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_USE_TILE_SMOOTH_TRANSITIONS, true),
        )

    /**
     * Show testing levels - show levels marked as testing only on world map
     * Default is false (testing levels hidden)
     */
    val showTestingLevels: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_SHOW_TESTING_LEVELS, false),
        )

    /**
     * Use level map image - show PNG map image behind the hexagonal grid
     * Default is true (level map image ON)
     */
    val useLevelMapImage: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_USE_LEVEL_MAP_IMAGE, true),
        )

    /**
     * Show debug options - show debug icon in level header
     * Default is false (debug options hidden)
     */
    val showDebugOptions: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_SHOW_DEBUG_OPTIONS, false),
        )

    /**
     * Enable animations - show Lottie animations (e.g. green witch healing, barricade damage)
     * Default is true (animations ON)
     */
    val enableAnimations: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_ENABLE_ANIMATIONS, true),
        )

    /**
     * Enable world map animations - controls river flow and tide movement overlays on world map.
     * Default is true (animations ON)
     */
    val enableWorldMapAnimations: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_ENABLE_WORLDMAP_ANIMATIONS, true),
        )

    /**
     * Check for updates on startup - query GitHub releases API for a newer version.
     * Skipped on WASM; also skipped on Android when installed via the Play Store.
     * Default is true (update check ON)
     */
    val checkForUpdates: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_CHECK_FOR_UPDATES, true),
        )

    /**
     * Auto-jump to next actionable tower – when ON, the game automatically selects the
     * next tower with remaining action points after the current one is exhausted, and also
     * selects the first actionable tower at the start of each player turn.
     * Default is false (OFF).
     */
    val autoJumpToNextTower: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_AUTO_JUMP_TO_NEXT_TOWER, false),
        )

    /**
     * Show unit/tower background color – when ON, enemy tiles get a red background and tower
     * tiles get a blue/gray background (classic look). When OFF, tiles are see-through.
     * Default is false (OFF = transparent).
     */
    val showUnitTowerBackground: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_SHOW_UNIT_TOWER_BACKGROUND, false),
        )

    /**
     * If true, use split build-tower button in compact controls to free info area space.
     */
    val splitBuildTowerButton: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_SPLIT_BUILD_TOWER_BUTTON, true),
        )

    /**
     * Accessibility: high contrast color mode.
     */
    val highContrastEnabled: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_HIGH_CONTRAST, false),
        )

    /**
     * Accessibility: color blind palette.
     */
    val colorBlindPalette: MutableState<ColorBlindPalette> =
        mutableStateOf(
            try {
                ColorBlindPalette.valueOf(settings[KEY_COLOR_BLIND_PALETTE, ColorBlindPalette.OFF.name])
            } catch (_: Exception) {
                ColorBlindPalette.OFF
            },
        )

    /**
     * Accessibility: show captions for sound-related events.
     */
    val captionsEnabled: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_CAPTIONS_ENABLED, false),
        )

    /**
     * Accessibility: require hold-to-confirm for destructive actions.
     */
    val holdToConfirmEnabled: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_HOLD_TO_CONFIRM, false),
        )

    /**
     * Accessibility: font size scale.
     */
    val fontSize: MutableState<FontSize> =
        mutableStateOf(
            try {
                FontSize.valueOf(settings[KEY_FONT_SIZE, FontSize.DEFAULT.name])
            } catch (_: Exception) {
                FontSize.DEFAULT
            },
        )

    /**
     * Accessibility: show keyboard shortcut hints on matching buttons.
     */
    val showButtonShortcutHints: MutableState<Boolean> =
        mutableStateOf(
            settings.getBoolean(KEY_SHOW_BUTTON_SHORTCUT_HINTS, false),
        )

    /**
     * Keyboard shortcuts (remappable).
     */
    val shortcutAttackSelectedTarget: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_ATTACK_SELECTED_TARGET, DEFAULT_SHORTCUT_ATTACK_SELECTED_TARGET],
                DEFAULT_SHORTCUT_ATTACK_SELECTED_TARGET,
            ),
        )

    val shortcutSelectNextTower: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_SELECT_NEXT_TOWER, DEFAULT_SHORTCUT_SELECT_NEXT_TOWER],
                DEFAULT_SHORTCUT_SELECT_NEXT_TOWER,
            ),
        )

    val shortcutSelectPreviousTower: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_SELECT_PREVIOUS_TOWER, DEFAULT_SHORTCUT_SELECT_PREVIOUS_TOWER],
                DEFAULT_SHORTCUT_SELECT_PREVIOUS_TOWER,
            ),
        )

    val shortcutAutoAttackEndTurn: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_AUTO_ATTACK_END_TURN, DEFAULT_SHORTCUT_AUTO_ATTACK_END_TURN],
                DEFAULT_SHORTCUT_AUTO_ATTACK_END_TURN,
            ),
        )

    val shortcutCheat: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_CHEAT, DEFAULT_SHORTCUT_CHEAT],
                DEFAULT_SHORTCUT_CHEAT,
            ),
        )

    val shortcutToggleEnemyList: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_TOGGLE_ENEMY_LIST, DEFAULT_SHORTCUT_TOGGLE_ENEMY_LIST],
                DEFAULT_SHORTCUT_TOGGLE_ENEMY_LIST,
            ),
        )

    val shortcutEndTurnStartBattle: MutableState<String> =
        mutableStateOf(
            run {
                val raw = settings[KEY_SHORTCUT_END_TURN_START_BATTLE, DEFAULT_SHORTCUT_END_TURN_START_BATTLE]
                val normalized = normalizeShortcutBinding(raw, DEFAULT_SHORTCUT_END_TURN_START_BATTLE)
                // Migrate: plain "ENTER" was the old default; upgrade to the new default "Ctrl+ENTER"
                if (normalized == "ENTER") {
                    settings[KEY_SHORTCUT_END_TURN_START_BATTLE] = DEFAULT_SHORTCUT_END_TURN_START_BATTLE
                    normalizeShortcutBinding(DEFAULT_SHORTCUT_END_TURN_START_BATTLE, DEFAULT_SHORTCUT_END_TURN_START_BATTLE)
                } else {
                    normalized
                }
            },
        )

    val shortcutSaveGame: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_SAVE_GAME, DEFAULT_SHORTCUT_SAVE_GAME],
                DEFAULT_SHORTCUT_SAVE_GAME,
            ),
        )

    val shortcutPanUp: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_PAN_UP, DEFAULT_SHORTCUT_PAN_UP],
                DEFAULT_SHORTCUT_PAN_UP,
            ),
        )

    val shortcutPanDown: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_PAN_DOWN, DEFAULT_SHORTCUT_PAN_DOWN],
                DEFAULT_SHORTCUT_PAN_DOWN,
            ),
        )

    val shortcutPanLeft: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_PAN_LEFT, DEFAULT_SHORTCUT_PAN_LEFT],
                DEFAULT_SHORTCUT_PAN_LEFT,
            ),
        )

    val shortcutPanRight: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_PAN_RIGHT, DEFAULT_SHORTCUT_PAN_RIGHT],
                DEFAULT_SHORTCUT_PAN_RIGHT,
            ),
        )

    /**
     * Accessibility: keyboard shortcut key for centering map on selected tower.
     */
    val shortcutCenterSelectedTower: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_CENTER_SELECTED_TOWER, DEFAULT_SHORTCUT_CENTER_SELECTED_TOWER],
                DEFAULT_SHORTCUT_CENTER_SELECTED_TOWER,
            ),
        )

    /**
     * Accessibility: keyboard shortcut key for centering map on next spawn point.
     */
    val shortcutCenterNextSpawnPoint: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_CENTER_NEXT_SPAWN_POINT, DEFAULT_SHORTCUT_CENTER_NEXT_SPAWN_POINT],
                DEFAULT_SHORTCUT_CENTER_NEXT_SPAWN_POINT,
            ),
        )

    val shortcutUpgradeSelectedTower: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_UPGRADE_SELECTED_TOWER, DEFAULT_SHORTCUT_UPGRADE_SELECTED_TOWER],
                DEFAULT_SHORTCUT_UPGRADE_SELECTED_TOWER,
            ),
        )

    val shortcutUndoOrSellSelectedTower: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER, DEFAULT_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER],
                DEFAULT_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER,
            ),
        )

    val shortcutToggleSpellMenu: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_TOGGLE_SPELL_MENU, DEFAULT_SHORTCUT_TOGGLE_SPELL_MENU],
                DEFAULT_SHORTCUT_TOGGLE_SPELL_MENU,
            ),
        )

    val shortcutSwitchToTowerMode: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_SWITCH_TO_TOWER_MODE, DEFAULT_SHORTCUT_SWITCH_TO_TOWER_MODE],
                DEFAULT_SHORTCUT_SWITCH_TO_TOWER_MODE,
            ),
        )

    val shortcutNextEnemyTarget: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_NEXT_ENEMY_TARGET, DEFAULT_SHORTCUT_NEXT_ENEMY_TARGET],
                DEFAULT_SHORTCUT_NEXT_ENEMY_TARGET,
            ),
        )

    val shortcutPrevEnemyTarget: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_PREV_ENEMY_TARGET, DEFAULT_SHORTCUT_PREV_ENEMY_TARGET],
                DEFAULT_SHORTCUT_PREV_ENEMY_TARGET,
            ),
        )

    val shortcutBackToWorldMap: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_BACK_TO_WORLDMAP, DEFAULT_SHORTCUT_BACK_TO_WORLDMAP],
                DEFAULT_SHORTCUT_BACK_TO_WORLDMAP,
            ),
        )

    val shortcutToggleAudio: MutableState<String> =
        mutableStateOf(
            normalizeShortcutBinding(
                settings[KEY_SHORTCUT_TOGGLE_AUDIO, DEFAULT_SHORTCUT_TOGGLE_AUDIO],
                DEFAULT_SHORTCUT_TOGGLE_AUDIO,
            ),
        )

    // Session-only debug states (not persisted)
    val showTileBorders: MutableState<Boolean> = mutableStateOf(false)
    val showTilePositions: MutableState<Boolean> = mutableStateOf(false)
    val showMapSizeOverlay: MutableState<Boolean> = mutableStateOf(false)

    /**
     * Level header text size - controls the size of text and icons in the game header
     * Default is SMALL (current size)
     */
    val headerTextSize: MutableState<HeaderTextSize> =
        mutableStateOf(
            try {
                HeaderTextSize.valueOf(settings[KEY_HEADER_TEXT_SIZE, HeaderTextSize.DEFAULT.name])
            } catch (e: Exception) {
                HeaderTextSize.DEFAULT
            },
        )

    /**
     * Initialize settings on app start
     * Loads saved language preference
     */
    fun initialize() {
        // Load saved language preference
        val savedLanguage = settings[KEY_LANGUAGE, ""]
        if (savedLanguage.isNotEmpty()) {
            try {
                val locale = AppLocale.entries.find { it.code == savedLanguage }
                if (locale != null) {
                    currentLanguage.value = locale
                }
            } catch (e: Exception) {
                // If saved language is invalid, keep default
            }
        }
        getOrCreateInstallUuid()
    }

    /**
     * Check if user has chosen language on first start
     */
    fun hasChosenLanguage(): Boolean = settings.getBoolean(KEY_LANGUAGE_CHOSEN, false)

    /**
     * Mark language as chosen (after first-time language selection)
     */
    fun markLanguageChosen() {
        settings.putBoolean(KEY_LANGUAGE_CHOSEN, true)
    }

    /**
     * Detect and preselect platform language if supported
     * Returns the detected locale or null if not detected/supported
     */
    fun detectAndPreselectPlatformLanguage(): AppLocale? {
        try {
            val systemLangCode =
                de.egril.defender.utils
                    .getSystemLanguageCode()
            if (systemLangCode != null) {
                // Find matching locale in supported languages
                val matchingLocale =
                    AppLocale.entries.find {
                        it.code.equals(systemLangCode, ignoreCase = true)
                    }
                if (matchingLocale != null) {
                    // Preselect but don't save yet (user needs to confirm)
                    currentLanguage.value = matchingLocale
                    return matchingLocale
                }
            }
        } catch (e: Exception) {
            // If detection fails, keep default
        }
        return null
    }

    /**
     * Save dark mode preference
     */
    fun saveDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        settings[KEY_DARK_MODE] = enabled
        onPersist?.invoke()
    }

    /**
     * Save language preference
     */
    fun saveLanguage(locale: AppLocale) {
        currentLanguage.value = locale
        settings[KEY_LANGUAGE] = locale.code
        onPersist?.invoke()
    }

    /**
     * Save sound enabled preference
     */
    fun saveSoundEnabled(enabled: Boolean) {
        isSoundEnabled.value = enabled
        settings[KEY_SOUND_ENABLED] = enabled
        onPersist?.invoke()
    }

    /**
     * Save sound volume preference
     */
    fun saveSoundVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        soundVolume.value = clampedVolume
        settings.putFloat(KEY_SOUND_VOLUME, clampedVolume)
        onPersist?.invoke()
    }

    /**
     * Save effect sounds enabled preference
     */
    fun saveEffectsEnabled(enabled: Boolean) {
        isEffectsEnabled.value = enabled
        settings.putBoolean(KEY_EFFECTS_ENABLED, enabled)
        onPersist?.invoke()
    }

    /**
     * Save effect sounds volume preference
     */
    fun saveEffectsVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        effectsVolume.value = clampedVolume
        settings.putFloat(KEY_EFFECTS_VOLUME, clampedVolume)
        onPersist?.invoke()
    }

    /**
     * Save background music enabled preference
     */
    fun saveMusicEnabled(enabled: Boolean) {
        isMusicEnabled.value = enabled
        settings.putBoolean(KEY_MUSIC_ENABLED, enabled)
        onPersist?.invoke()
    }

    /**
     * Save background music volume preference
     */
    fun saveMusicVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        musicVolume.value = clampedVolume
        settings.putFloat(KEY_MUSIC_VOLUME, clampedVolume)
        onPersist?.invoke()
    }

    /**
     * Save world map music enabled preference
     */
    fun saveWorldMapMusicEnabled(enabled: Boolean) {
        isWorldMapMusicEnabled.value = enabled
        settings.putBoolean(KEY_WORLDMAP_MUSIC_ENABLED, enabled)
        onPersist?.invoke()
    }

    /**
     * Save world map music volume preference
     */
    fun saveWorldMapMusicVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        worldMapMusicVolume.value = clampedVolume
        settings.putFloat(KEY_WORLDMAP_MUSIC_VOLUME, clampedVolume)
        onPersist?.invoke()
    }

    /**
     * Save gameplay music enabled preference
     */
    fun saveGameplayMusicEnabled(enabled: Boolean) {
        isGameplayMusicEnabled.value = enabled
        settings.putBoolean(KEY_GAMEPLAY_MUSIC_ENABLED, enabled)
        onPersist?.invoke()
    }

    /**
     * Save gameplay music volume preference
     */
    fun saveGameplayMusicVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        gameplayMusicVolume.value = clampedVolume
        settings.putFloat(KEY_GAMEPLAY_MUSIC_VOLUME, clampedVolume)
        onPersist?.invoke()
    }

    /**
     * Save control pad visibility preference
     */
    fun saveShowControlPad(show: Boolean) {
        showControlPad.value = show
        settings.putBoolean(KEY_SHOW_CONTROL_PAD, show)
        onPersist?.invoke()
    }

    /**
     * Save difficulty preference
     */
    fun saveDifficulty(level: DifficultyLevel) {
        difficulty.value = level
        settings[KEY_DIFFICULTY] = level.name
        onPersist?.invoke()
    }

    /**
     * Save world map display style preference
     */
    fun saveUseLevelCards(useLevelCards: Boolean) {
        this.useLevelCards.value = useLevelCards
        settings.putBoolean(KEY_USE_LEVEL_CARDS, useLevelCards)
        onPersist?.invoke()
    }

    /**
     * Mark settings hint as shown
     */
    fun markSettingsHintShown() {
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
    fun markAccessibilityBannerShown() {
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
    fun markDailyHintShown(
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
    fun saveUseTileImages(useTiles: Boolean) {
        useTileImages.value = useTiles
        settings.putBoolean(KEY_USE_TILE_IMAGES, useTiles)
        onPersist?.invoke()
    }

    /**
     * Save tile smooth transitions preference
     * Note: The value is saved regardless of useTileImages state, but only used when useTileImages is true
     */
    fun saveUseTileSmoothTransitions(useTransitions: Boolean) {
        useTileSmoothTransitions.value = useTransitions
        settings.putBoolean(KEY_USE_TILE_SMOOTH_TRANSITIONS, useTransitions)
        onPersist?.invoke()
    }

    /**
     * Save show testing levels preference
     */
    fun saveShowTestingLevels(show: Boolean) {
        showTestingLevels.value = show
        settings.putBoolean(KEY_SHOW_TESTING_LEVELS, show)
        onPersist?.invoke()
    }

    /**
     * Save level header text size preference
     */
    fun saveHeaderTextSize(size: HeaderTextSize) {
        headerTextSize.value = size
        settings[KEY_HEADER_TEXT_SIZE] = size.name
        onPersist?.invoke()
    }

    fun saveUseLevelMapImage(use: Boolean) {
        useLevelMapImage.value = use
        settings.putBoolean(KEY_USE_LEVEL_MAP_IMAGE, use)
        onPersist?.invoke()
    }

    fun saveShowDebugOptions(show: Boolean) {
        showDebugOptions.value = show
        settings.putBoolean(KEY_SHOW_DEBUG_OPTIONS, show)
    }

    /**
     * Save enable animations preference
     */
    fun saveEnableAnimations(enabled: Boolean) {
        enableAnimations.value = enabled
        settings.putBoolean(KEY_ENABLE_ANIMATIONS, enabled)
        onPersist?.invoke()
    }

    /**
     * Save world map animations preference
     */
    fun saveEnableWorldMapAnimations(enabled: Boolean) {
        enableWorldMapAnimations.value = enabled
        settings.putBoolean(KEY_ENABLE_WORLDMAP_ANIMATIONS, enabled)
        onPersist?.invoke()
    }

    /**
     * Save check for updates preference
     */
    fun saveCheckForUpdates(enabled: Boolean) {
        checkForUpdates.value = enabled
        settings.putBoolean(KEY_CHECK_FOR_UPDATES, enabled)
        onPersist?.invoke()
    }

    /**
     * Save auto-jump to next tower preference
     */
    fun saveAutoJumpToNextTower(enabled: Boolean) {
        autoJumpToNextTower.value = enabled
        settings.putBoolean(KEY_AUTO_JUMP_TO_NEXT_TOWER, enabled)
        onPersist?.invoke()
    }

    /**
     * Save show unit/tower background color preference
     */
    fun saveShowUnitTowerBackground(enabled: Boolean) {
        showUnitTowerBackground.value = enabled
        settings.putBoolean(KEY_SHOW_UNIT_TOWER_BACKGROUND, enabled)
        onPersist?.invoke()
    }

    /**
     * Save split build-tower button preference
     */
    fun saveSplitBuildTowerButton(enabled: Boolean) {
        splitBuildTowerButton.value = enabled
        settings.putBoolean(KEY_SPLIT_BUILD_TOWER_BUTTON, enabled)
        onPersist?.invoke()
    }

    fun saveHighContrastEnabled(enabled: Boolean) {
        highContrastEnabled.value = enabled
        settings.putBoolean(KEY_HIGH_CONTRAST, enabled)
        onPersist?.invoke()
    }

    fun saveColorBlindPalette(palette: ColorBlindPalette) {
        colorBlindPalette.value = palette
        settings[KEY_COLOR_BLIND_PALETTE] = palette.name
        onPersist?.invoke()
    }

    fun saveCaptionsEnabled(enabled: Boolean) {
        captionsEnabled.value = enabled
        settings.putBoolean(KEY_CAPTIONS_ENABLED, enabled)
        onPersist?.invoke()
    }

    fun saveHoldToConfirmEnabled(enabled: Boolean) {
        holdToConfirmEnabled.value = enabled
        settings.putBoolean(KEY_HOLD_TO_CONFIRM, enabled)
        onPersist?.invoke()
    }

    fun saveFontSize(size: FontSize) {
        fontSize.value = size
        settings[KEY_FONT_SIZE] = size.name
        onPersist?.invoke()
    }

    fun saveShowButtonShortcutHints(enabled: Boolean) {
        showButtonShortcutHints.value = enabled
        settings.putBoolean(KEY_SHOW_BUTTON_SHORTCUT_HINTS, enabled)
        onPersist?.invoke()
    }

    fun saveShortcutAttackSelectedTarget(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_ATTACK_SELECTED_TARGET)
        shortcutAttackSelectedTarget.value = normalized
        settings[KEY_SHORTCUT_ATTACK_SELECTED_TARGET] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutSelectNextTower(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_SELECT_NEXT_TOWER)
        shortcutSelectNextTower.value = normalized
        settings[KEY_SHORTCUT_SELECT_NEXT_TOWER] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutSelectPreviousTower(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_SELECT_PREVIOUS_TOWER)
        shortcutSelectPreviousTower.value = normalized
        settings[KEY_SHORTCUT_SELECT_PREVIOUS_TOWER] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutAutoAttackEndTurn(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_AUTO_ATTACK_END_TURN)
        shortcutAutoAttackEndTurn.value = normalized
        settings[KEY_SHORTCUT_AUTO_ATTACK_END_TURN] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutCheat(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_CHEAT)
        shortcutCheat.value = normalized
        settings[KEY_SHORTCUT_CHEAT] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutToggleEnemyList(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_TOGGLE_ENEMY_LIST)
        shortcutToggleEnemyList.value = normalized
        settings[KEY_SHORTCUT_TOGGLE_ENEMY_LIST] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutEndTurnStartBattle(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_END_TURN_START_BATTLE)
        shortcutEndTurnStartBattle.value = normalized
        settings[KEY_SHORTCUT_END_TURN_START_BATTLE] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutSaveGame(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_SAVE_GAME)
        shortcutSaveGame.value = normalized
        settings[KEY_SHORTCUT_SAVE_GAME] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutPanUp(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PAN_UP)
        shortcutPanUp.value = normalized
        settings[KEY_SHORTCUT_PAN_UP] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutPanDown(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PAN_DOWN)
        shortcutPanDown.value = normalized
        settings[KEY_SHORTCUT_PAN_DOWN] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutPanLeft(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PAN_LEFT)
        shortcutPanLeft.value = normalized
        settings[KEY_SHORTCUT_PAN_LEFT] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutPanRight(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PAN_RIGHT)
        shortcutPanRight.value = normalized
        settings[KEY_SHORTCUT_PAN_RIGHT] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutCenterSelectedTower(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_CENTER_SELECTED_TOWER)
        shortcutCenterSelectedTower.value = normalized
        settings[KEY_SHORTCUT_CENTER_SELECTED_TOWER] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutCenterNextSpawnPoint(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_CENTER_NEXT_SPAWN_POINT)
        shortcutCenterNextSpawnPoint.value = normalized
        settings[KEY_SHORTCUT_CENTER_NEXT_SPAWN_POINT] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutUpgradeSelectedTower(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_UPGRADE_SELECTED_TOWER)
        shortcutUpgradeSelectedTower.value = normalized
        settings[KEY_SHORTCUT_UPGRADE_SELECTED_TOWER] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutUndoOrSellSelectedTower(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER)
        shortcutUndoOrSellSelectedTower.value = normalized
        settings[KEY_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutToggleSpellMenu(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_TOGGLE_SPELL_MENU)
        shortcutToggleSpellMenu.value = normalized
        settings[KEY_SHORTCUT_TOGGLE_SPELL_MENU] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutSwitchToTowerMode(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_SWITCH_TO_TOWER_MODE)
        shortcutSwitchToTowerMode.value = normalized
        settings[KEY_SHORTCUT_SWITCH_TO_TOWER_MODE] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutNextEnemyTarget(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_NEXT_ENEMY_TARGET)
        shortcutNextEnemyTarget.value = normalized
        settings[KEY_SHORTCUT_NEXT_ENEMY_TARGET] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutPrevEnemyTarget(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_PREV_ENEMY_TARGET)
        shortcutPrevEnemyTarget.value = normalized
        settings[KEY_SHORTCUT_PREV_ENEMY_TARGET] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutBackToWorldMap(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_BACK_TO_WORLDMAP)
        shortcutBackToWorldMap.value = normalized
        settings[KEY_SHORTCUT_BACK_TO_WORLDMAP] = normalized
        onPersist?.invoke()
    }

    fun saveShortcutToggleAudio(shortcut: String) {
        val normalized = normalizeShortcutBinding(shortcut, DEFAULT_SHORTCUT_TOGGLE_AUDIO)
        shortcutToggleAudio.value = normalized
        settings[KEY_SHORTCUT_TOGGLE_AUDIO] = normalized
        onPersist?.invoke()
    }

    fun resetShortcutBindings() {
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

    fun getDefaultShortcutBindings(): Map<String, String> =
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

    fun getAccessibilityPreferences(): AccessibilityPreferences =
        AccessibilityPreferences(
            highContrastEnabled = highContrastEnabled.value,
            colorBlindPalette = colorBlindPalette.value,
            captionsEnabled = captionsEnabled.value,
            holdToConfirmEnabled = holdToConfirmEnabled.value,
            reduceMotionEnabled = !enableAnimations.value,
        )

    /**
     * Serialize all relevant (non-debug, non-hint) settings into a flat map for remote storage.
     * Keys match the internal KEY_* constants; values are String representations.
     */
    fun toSettingsMap(): Map<String, String> =
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
    fun applyFromSettingsMap(map: Map<String, String>) {
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

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
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
        resetShortcutBindings()

        // Note: Don't reset settings hint shown state when resetting settings
        // as user has already seen it once
    }
}
