package com.defenderofegril.ui.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

/**
 * Manages application settings using multiplatform-settings library
 * Persists dark mode preference and language selection
 */
object AppSettings {
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_LANGUAGE = "language"
    
    private val settings: Settings = Settings()
    
    /**
     * Dark mode state - automatically saved when changed
     */
    val isDarkMode: MutableState<Boolean> = mutableStateOf(settings[KEY_DARK_MODE, false])
    
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
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        // Reset dark mode
        saveDarkMode(false)
        
        // Reset language to default
        saveLanguage(AppLocale.DEFAULT)
    }
}
