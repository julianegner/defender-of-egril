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
 * Mouse pointer source.
 */
enum class MousePointerSource {
    GAME,
    GAUNTLET,
    SYSTEM,
    ;

    companion object {
        val DEFAULT = GAME
    }
}

/**
 * Mouse pointer size presets.
 */
enum class MousePointerSize(
    val scale: Float,
) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.2f),
    EXTRA_LARGE(1.45f),
    HUGE(1.7f),
    ;

    companion object {
        val DEFAULT = MEDIUM
    }
}

/**
 * Mouse pointer hand direction.
 */
enum class MousePointerDirection {
    LEFT,
    RIGHT,
    ;

    companion object {
        val DEFAULT = RIGHT
    }
}

/**
 * Manages application settings using multiplatform-settings library
 * Persists dark mode preference, language selection, sound settings, control pad visibility, difficulty level, and world map style
 */
object AppSettings {
    const val MOUSE_POINTER_SKIN_BRIGHTNESS_MIN = -0.35f
    const val MOUSE_POINTER_SKIN_BRIGHTNESS_MAX = 0.35f

    internal const val KEY_DARK_MODE = "dark_mode"
    internal const val KEY_LANGUAGE = "language"
    private const val KEY_LANGUAGE_CHOSEN = "language_chosen"
    internal const val KEY_SOUND_ENABLED = "sound_enabled"
    internal const val KEY_SOUND_VOLUME = "sound_volume"
    internal const val KEY_EFFECTS_ENABLED = "effects_enabled"
    internal const val KEY_EFFECTS_VOLUME = "effects_volume"
    internal const val KEY_MUSIC_ENABLED = "music_enabled"
    internal const val KEY_MUSIC_VOLUME = "music_volume"
    internal const val KEY_WORLDMAP_MUSIC_ENABLED = "worldmap_music_enabled"
    internal const val KEY_WORLDMAP_MUSIC_VOLUME = "worldmap_music_volume"
    internal const val KEY_GAMEPLAY_MUSIC_ENABLED = "gameplay_music_enabled"
    internal const val KEY_GAMEPLAY_MUSIC_VOLUME = "gameplay_music_volume"
    internal const val KEY_SHOW_CONTROL_PAD = "show_control_pad"
    internal const val KEY_DIFFICULTY = "difficulty"
    internal const val KEY_USE_LEVEL_CARDS = "use_level_cards"
    internal const val KEY_SETTINGS_HINT_SHOWN = "settings_hint_shown"
    internal const val KEY_ACCESSIBILITY_BANNER_SHOWN = "accessibility_banner_shown"
    internal const val KEY_DAILY_HINT_LAST_SHOWN_DATE = "daily_hint_last_shown_date"
    internal const val KEY_DAILY_HINT_LAST_INDEX = "daily_hint_last_index"
    internal const val KEY_USE_TILE_IMAGES = "use_tile_images"
    internal const val KEY_USE_TILE_SMOOTH_TRANSITIONS = "use_tile_smooth_transitions"
    internal const val KEY_SHOW_TESTING_LEVELS = "show_testing_levels"
    internal const val KEY_HEADER_TEXT_SIZE = "header_text_size"
    internal const val KEY_USE_LEVEL_MAP_IMAGE = "use_level_map_image"
    internal const val KEY_SHOW_DEBUG_OPTIONS = "show_debug_options"
    internal const val KEY_ENABLE_ANIMATIONS = "enable_animations"
    internal const val KEY_ENABLE_WORLDMAP_ANIMATIONS = "enable_worldmap_animations"
    internal const val KEY_CHECK_FOR_UPDATES = "check_for_updates"
    internal const val KEY_AUTO_JUMP_TO_NEXT_TOWER = "auto_jump_to_next_tower"
    internal const val KEY_SHOW_UNIT_TOWER_BACKGROUND = "show_unit_tower_background"
    internal const val KEY_SPLIT_BUILD_TOWER_BUTTON = "split_build_tower_button"
    internal const val KEY_HIGH_CONTRAST = "high_contrast"
    internal const val KEY_COLOR_BLIND_PALETTE = "color_blind_palette"
    internal const val KEY_CAPTIONS_ENABLED = "captions_enabled"
    internal const val KEY_HOLD_TO_CONFIRM = "hold_to_confirm"
    internal const val KEY_FONT_SIZE = "font_size"
    internal const val KEY_SHOW_BUTTON_SHORTCUT_HINTS = "show_button_shortcut_hints"
    internal const val KEY_MOUSE_POINTER_SOURCE = "mouse_pointer_source"
    internal const val KEY_MOUSE_POINTER_SIZE = "mouse_pointer_size"
    internal const val KEY_MOUSE_POINTER_DIRECTION = "mouse_pointer_direction"
    internal const val KEY_MOUSE_POINTER_SKIN_BRIGHTNESS = "mouse_pointer_skin_brightness"
    internal const val KEY_SHORTCUT_ATTACK_SELECTED_TARGET = "shortcut_attack_selected_target"
    internal const val KEY_SHORTCUT_SELECT_NEXT_TOWER = "shortcut_select_next_tower"
    internal const val KEY_SHORTCUT_SELECT_PREVIOUS_TOWER = "shortcut_select_previous_tower"
    internal const val KEY_SHORTCUT_AUTO_ATTACK_END_TURN = "shortcut_auto_attack_end_turn"
    internal const val KEY_SHORTCUT_CHEAT = "shortcut_cheat"
    internal const val KEY_SHORTCUT_TOGGLE_ENEMY_LIST = "shortcut_toggle_enemy_list"
    internal const val KEY_SHORTCUT_END_TURN_START_BATTLE = "shortcut_end_turn_start_battle"
    internal const val KEY_SHORTCUT_SAVE_GAME = "shortcut_save_game"
    internal const val KEY_SHORTCUT_PAN_UP = "shortcut_pan_up"
    internal const val KEY_SHORTCUT_PAN_DOWN = "shortcut_pan_down"
    internal const val KEY_SHORTCUT_PAN_LEFT = "shortcut_pan_left"
    internal const val KEY_SHORTCUT_PAN_RIGHT = "shortcut_pan_right"
    internal const val KEY_SHORTCUT_CENTER_SELECTED_TOWER = "shortcut_center_selected_tower"
    internal const val KEY_SHORTCUT_CENTER_NEXT_SPAWN_POINT = "shortcut_center_next_spawn_point"
    internal const val KEY_SHORTCUT_UPGRADE_SELECTED_TOWER = "shortcut_upgrade_selected_tower"
    internal const val KEY_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER = "shortcut_undo_or_sell_selected_tower"
    internal const val KEY_SHORTCUT_TOGGLE_SPELL_MENU = "shortcut_toggle_spell_menu"
    internal const val KEY_SHORTCUT_SWITCH_TO_TOWER_MODE = "shortcut_switch_to_tower_mode"
    internal const val KEY_SHORTCUT_NEXT_ENEMY_TARGET = "shortcut_next_enemy_target"
    internal const val KEY_SHORTCUT_PREV_ENEMY_TARGET = "shortcut_prev_enemy_target"
    internal const val KEY_SHORTCUT_BACK_TO_WORLDMAP = "shortcut_back_to_worldmap"
    internal const val KEY_SHORTCUT_TOGGLE_AUDIO = "shortcut_toggle_audio"
    internal const val DEFAULT_SHORTCUT_ATTACK_SELECTED_TARGET = "F"
    internal const val DEFAULT_SHORTCUT_SELECT_NEXT_TOWER = "Tab"
    internal const val DEFAULT_SHORTCUT_SELECT_PREVIOUS_TOWER = "Shift+Tab"
    internal const val DEFAULT_SHORTCUT_AUTO_ATTACK_END_TURN = "Ctrl+A"
    internal const val DEFAULT_SHORTCUT_CHEAT = "C"
    internal const val DEFAULT_SHORTCUT_TOGGLE_ENEMY_LIST = "E"
    internal const val DEFAULT_SHORTCUT_END_TURN_START_BATTLE = "Ctrl+Enter"
    internal const val DEFAULT_SHORTCUT_SAVE_GAME = "Ctrl+S"
    internal const val DEFAULT_SHORTCUT_PAN_UP = "W"
    internal const val DEFAULT_SHORTCUT_PAN_DOWN = "S"
    internal const val DEFAULT_SHORTCUT_PAN_LEFT = "A"
    internal const val DEFAULT_SHORTCUT_PAN_RIGHT = "D"
    internal const val DEFAULT_SHORTCUT_CENTER_SELECTED_TOWER = "R"
    internal const val DEFAULT_SHORTCUT_CENTER_NEXT_SPAWN_POINT = "G"
    internal const val DEFAULT_SHORTCUT_UPGRADE_SELECTED_TOWER = "U"
    internal const val DEFAULT_SHORTCUT_UNDO_OR_SELL_SELECTED_TOWER = "X"
    internal const val DEFAULT_SHORTCUT_TOGGLE_SPELL_MENU = "M"
    internal const val DEFAULT_SHORTCUT_SWITCH_TO_TOWER_MODE = "T"
    internal const val DEFAULT_SHORTCUT_NEXT_ENEMY_TARGET = "N"
    internal const val DEFAULT_SHORTCUT_PREV_ENEMY_TARGET = "Shift+N"
    internal const val DEFAULT_SHORTCUT_BACK_TO_WORLDMAP = "Escape"
    internal const val DEFAULT_SHORTCUT_TOGGLE_AUDIO = "P"

    internal val settings: Settings = Settings()

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
     * Mouse pointer source.
     */
    val mousePointerSource: MutableState<MousePointerSource> =
        mutableStateOf(
            try {
                MousePointerSource.valueOf(settings[KEY_MOUSE_POINTER_SOURCE, MousePointerSource.DEFAULT.name])
            } catch (_: Exception) {
                MousePointerSource.DEFAULT
            },
        )

    /**
     * Mouse pointer size preset.
     */
    val mousePointerSize: MutableState<MousePointerSize> =
        mutableStateOf(
            try {
                MousePointerSize.valueOf(settings[KEY_MOUSE_POINTER_SIZE, MousePointerSize.DEFAULT.name])
            } catch (_: Exception) {
                MousePointerSize.DEFAULT
            },
        )

    /**
     * Mouse pointer direction.
     */
    val mousePointerDirection: MutableState<MousePointerDirection> =
        mutableStateOf(
            try {
                MousePointerDirection.valueOf(settings[KEY_MOUSE_POINTER_DIRECTION, MousePointerDirection.DEFAULT.name])
            } catch (_: Exception) {
                MousePointerDirection.DEFAULT
            },
        )

    /**
     * Mouse pointer skin brightness shift.
     * 0f is neutral, negative values darken the skin areas, positive values brighten them.
     */
    val mousePointerSkinBrightness: MutableState<Float> =
        mutableStateOf(
            settings.getFloat(KEY_MOUSE_POINTER_SKIN_BRIGHTNESS, 0f).coerceIn(MOUSE_POINTER_SKIN_BRIGHTNESS_MIN, MOUSE_POINTER_SKIN_BRIGHTNESS_MAX),
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
    val showEnemyPathfinding: MutableState<Boolean> = mutableStateOf(false)

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

    fun getAccessibilityPreferences(): AccessibilityPreferences =
        AccessibilityPreferences(
            highContrastEnabled = highContrastEnabled.value,
            colorBlindPalette = colorBlindPalette.value,
            captionsEnabled = captionsEnabled.value,
            holdToConfirmEnabled = holdToConfirmEnabled.value,
            reduceMotionEnabled = !enableAnimations.value,
        )
}
