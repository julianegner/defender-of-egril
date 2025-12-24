package de.egril.defender.ui

import com.hyperether.resources.AppLocale
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.WorldMapLocationData

/**
 * Utility functions for getting localized names for maps, levels, and locations.
 * 
 * These functions support dynamic content from JSON files:
 * - If a nameKey/titleKey is provided and exists in string resources, use the translated string
 * - Otherwise, fall back to the direct name/title value
 * 
 * This allows:
 * - Built-in content to be translated via string resources
 * - Custom/user-added content to use direct names without translation
 */

/**
 * Get the localized name for a map.
 * Falls back to the direct name if nameKey is null or not found in resources.
 */
fun EditorMap.getLocalizedName(locale: AppLocale = currentLanguage.value): String {
    return if (nameKey != null) {
        try {
            LocalizedStrings.get(nameKey, locale)
        } catch (e: Exception) {
            name  // Fallback to direct name if key not found
        }
    } else {
        name
    }
}

/**
 * Get the localized title for a level.
 * Falls back to the direct title if titleKey is null or not found in resources.
 */
fun EditorLevel.getLocalizedTitle(locale: AppLocale = currentLanguage.value): String {
    return if (titleKey != null) {
        try {
            LocalizedStrings.get(titleKey, locale)
        } catch (e: Exception) {
            title  // Fallback to direct title if key not found
        }
    } else {
        title
    }
}

/**
 * Get the localized subtitle for a level.
 * Falls back to the direct subtitle if subtitleKey is null or not found in resources.
 */
fun EditorLevel.getLocalizedSubtitle(locale: AppLocale = currentLanguage.value): String {
    return if (subtitleKey != null) {
        try {
            LocalizedStrings.get(subtitleKey, locale)
        } catch (e: Exception) {
            subtitle  // Fallback to direct subtitle if key not found
        }
    } else {
        subtitle
    }
}

/**
 * Get the localized name for a world map location.
 * Falls back to the direct name if nameKey is null or not found in resources.
 */
fun WorldMapLocationData.getLocalizedName(locale: AppLocale = currentLanguage.value): String {
    return if (nameKey != null) {
        try {
            LocalizedStrings.get(nameKey, locale)
        } catch (e: Exception) {
            name  // Fallback to direct name if key not found
        }
    } else {
        name
    }
}
