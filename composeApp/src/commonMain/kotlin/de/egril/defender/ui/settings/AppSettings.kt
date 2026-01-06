package de.egril.defender.ui.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import de.egril.defender.utils.isPlatformMobile

/**
 * Game difficulty levels
 */
enum class DifficultyLevel {
    BABY,
    EASY,
    MEDIUM,
    HARD,
    NIGHTMARE;
    
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
    private const val KEY_USE_TILE_IMAGES = "use_tile_images"
    private const val KEY_SHOW_TESTING_LEVELS = "show_testing_levels"
    
    private val settings: Settings = Settings()
    
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
     * Default is 0.5 (quieter than effects)
     */
    val musicVolume: MutableState<Float> = mutableStateOf(settings.getFloat(KEY_MUSIC_VOLUME, 0.5f))
    
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
    val showControlPad: MutableState<Boolean> = mutableStateOf(
        settings.getBoolean(KEY_SHOW_CONTROL_PAD, isPlatformMobile)
    )
    
    /**
     * Game difficulty level - automatically saved when changed
     * Default is MEDIUM
     */
    val difficulty: MutableState<DifficultyLevel> = mutableStateOf(
        try {
            DifficultyLevel.valueOf(settings[KEY_DIFFICULTY, DifficultyLevel.DEFAULT.name])
        } catch (e: Exception) {
            DifficultyLevel.DEFAULT
        }
    )
    
    /**
     * World map display style - use level cards instead of image map
     * Default is false (use image-based map)
     */
    val useLevelCards: MutableState<Boolean> = mutableStateOf(
        settings.getBoolean(KEY_USE_LEVEL_CARDS, false)
    )
    
    /**
     * Settings hint shown state - track if first-time settings hint has been shown
     * Default is false (hint should be shown on first run)
     */
    val settingsHintShown: MutableState<Boolean> = mutableStateOf(
        settings.getBoolean(KEY_SETTINGS_HINT_SHOWN, false)
    )
    
    /**
     * Use tile background images - show tile images instead of solid colors
     * Default is true (tile images ON)
     */
    val useTileImages: MutableState<Boolean> = mutableStateOf(
        settings.getBoolean(KEY_USE_TILE_IMAGES, true)
    )
    
    /**
     * Show testing levels - show levels marked as testing only on world map
     * Default is false (testing levels hidden)
     */
    val showTestingLevels: MutableState<Boolean> = mutableStateOf(
        settings.getBoolean(KEY_SHOW_TESTING_LEVELS, false)
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
    }
    
    /**
     * Save dark mode preference
     */
    fun saveDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        settings[KEY_DARK_MODE] = enabled
    }
    
    /**
     * Save language preference
     */
    fun saveLanguage(locale: AppLocale) {
        currentLanguage.value = locale
        settings[KEY_LANGUAGE] = locale.code
    }
    
    /**
     * Save sound enabled preference
     */
    fun saveSoundEnabled(enabled: Boolean) {
        isSoundEnabled.value = enabled
        settings[KEY_SOUND_ENABLED] = enabled
    }
    
    /**
     * Save sound volume preference
     */
    fun saveSoundVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        soundVolume.value = clampedVolume
        settings.putFloat(KEY_SOUND_VOLUME, clampedVolume)
    }
    
    /**
     * Save effect sounds enabled preference
     */
    fun saveEffectsEnabled(enabled: Boolean) {
        isEffectsEnabled.value = enabled
        settings.putBoolean(KEY_EFFECTS_ENABLED, enabled)
    }
    
    /**
     * Save effect sounds volume preference
     */
    fun saveEffectsVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        effectsVolume.value = clampedVolume
        settings.putFloat(KEY_EFFECTS_VOLUME, clampedVolume)
    }
    
    /**
     * Save background music enabled preference
     */
    fun saveMusicEnabled(enabled: Boolean) {
        isMusicEnabled.value = enabled
        settings.putBoolean(KEY_MUSIC_ENABLED, enabled)
    }
    
    /**
     * Save background music volume preference
     */
    fun saveMusicVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        musicVolume.value = clampedVolume
        settings.putFloat(KEY_MUSIC_VOLUME, clampedVolume)
    }
    
    /**
     * Save world map music enabled preference
     */
    fun saveWorldMapMusicEnabled(enabled: Boolean) {
        isWorldMapMusicEnabled.value = enabled
        settings.putBoolean(KEY_WORLDMAP_MUSIC_ENABLED, enabled)
    }
    
    /**
     * Save world map music volume preference
     */
    fun saveWorldMapMusicVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        worldMapMusicVolume.value = clampedVolume
        settings.putFloat(KEY_WORLDMAP_MUSIC_VOLUME, clampedVolume)
    }
    
    /**
     * Save gameplay music enabled preference
     */
    fun saveGameplayMusicEnabled(enabled: Boolean) {
        isGameplayMusicEnabled.value = enabled
        settings.putBoolean(KEY_GAMEPLAY_MUSIC_ENABLED, enabled)
    }
    
    /**
     * Save gameplay music volume preference
     */
    fun saveGameplayMusicVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        gameplayMusicVolume.value = clampedVolume
        settings.putFloat(KEY_GAMEPLAY_MUSIC_VOLUME, clampedVolume)
    }
    
    /**
     * Save control pad visibility preference
     */
    fun saveShowControlPad(show: Boolean) {
        showControlPad.value = show
        settings.putBoolean(KEY_SHOW_CONTROL_PAD, show)
    }
    
    /**
     * Save difficulty preference
     */
    fun saveDifficulty(level: DifficultyLevel) {
        difficulty.value = level
        settings[KEY_DIFFICULTY] = level.name
    }
    
    /**
     * Save world map display style preference
     */
    fun saveUseLevelCards(useLevelCards: Boolean) {
        this.useLevelCards.value = useLevelCards
        settings.putBoolean(KEY_USE_LEVEL_CARDS, useLevelCards)
    }
    
    /**
     * Mark settings hint as shown
     */
    fun markSettingsHintShown() {
        settingsHintShown.value = true
        settings.putBoolean(KEY_SETTINGS_HINT_SHOWN, true)
    }
    
    /**
     * Save tile background images preference
     */
    fun saveUseTileImages(useTiles: Boolean) {
        useTileImages.value = useTiles
        settings.putBoolean(KEY_USE_TILE_IMAGES, useTiles)
    }
    
    /**
     * Save show testing levels preference
     */
    fun saveShowTestingLevels(show: Boolean) {
        showTestingLevels.value = show
        settings.putBoolean(KEY_SHOW_TESTING_LEVELS, show)
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
        
        // Reset show testing levels to OFF
        saveShowTestingLevels(false)
        
        // Note: Don't reset settings hint shown state when resetting settings
        // as user has already seen it once
    }
}
