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
 * Manages application settings using multiplatform-settings library
 * Persists dark mode preference, language selection, sound settings, and control pad visibility
 */
object AppSettings {
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_SOUND_VOLUME = "sound_volume"
    private const val KEY_SHOW_CONTROL_PAD = "show_control_pad"
    
    private val settings: Settings = Settings()
    
    /**
     * Dark mode state - automatically saved when changed
     */
    val isDarkMode: MutableState<Boolean> = mutableStateOf(settings[KEY_DARK_MODE, false])
    
    /**
     * Sound enabled state - automatically saved when changed
     */
    val isSoundEnabled: MutableState<Boolean> = mutableStateOf(settings[KEY_SOUND_ENABLED, true])
    
    /**
     * Sound volume level (0.0 to 1.0) - automatically saved when changed
     */
    val soundVolume: MutableState<Float> = mutableStateOf(settings.getFloat(KEY_SOUND_VOLUME, 0.7f))
    
    /**
     * Control pad visibility state - automatically saved when changed
     * Default is ON for mobile platforms, OFF for desktop/web
     */
    val showControlPad: MutableState<Boolean> = mutableStateOf(
        settings.getBoolean(KEY_SHOW_CONTROL_PAD, isPlatformMobile)
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
     * Save control pad visibility preference
     */
    fun saveShowControlPad(show: Boolean) {
        showControlPad.value = show
        settings.putBoolean(KEY_SHOW_CONTROL_PAD, show)
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
        
        // Reset control pad to platform default
        saveShowControlPad(isPlatformMobile)
    }
}
