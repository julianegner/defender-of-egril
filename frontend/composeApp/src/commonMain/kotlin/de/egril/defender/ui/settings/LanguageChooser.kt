package de.egril.defender.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.ui.common.DropdownItem
import de.egril.defender.ui.common.KeyboardNavigableDropdown
import dev.carlsen.flagkit.FlagKit

/**
 * Language chooser dropdown component
 * Displays the current language with its flag and allows switching between supported languages.
 * Keyboard navigation: Enter/Space to open, ↑/↓ to navigate, Enter to select, Esc to close.
 */
@Composable
fun LanguageChooser(
    modifier: Modifier = Modifier,
    onLanguageChanged: ((AppLocale) -> Unit)? = null,
    triggerOpen: Boolean = false,
    onTriggerOpenHandled: () -> Unit = {}
) {
    val items = remember {
        AppLocale.entries.map { locale ->
            DropdownItem(
                value = locale,
                content = { LanguageFlagAndName(locale) }
            )
        }
    }

    KeyboardNavigableDropdown(
        items = items,
        selectedValue = currentLanguage.value,
        onItemSelected = { locale ->
            currentLanguage.value = locale
            onLanguageChanged?.invoke(locale)
        },
        selectedContent = { LanguageFlagAndName(currentLanguage.value) },
        modifier = modifier.fillMaxWidth(),
        triggerOpen = triggerOpen,
        onTriggerOpenHandled = onTriggerOpenHandled
    )
}

/**
 * Displays a language flag and its display name
 */
@Composable
private fun LanguageFlagAndName(appLocale: AppLocale) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Get country code from locale
        val countryCode = getCountryCode(appLocale)
        
        // Display flag if available
        FlagKit.getFlag(countryCode = countryCode)?.let { flagVector ->
            Image(
                imageVector = flagVector,
                contentDescription = "${appLocale.displayName} flag",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .width(40.dp)
                    .height(24.dp)
                    .border(1.dp, Color.Gray)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
        
        Text(
            text = "${appLocale.nativeName} (${appLocale.code})",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Maps AppLocale to country code for flag display
 * UK flag for English (default language)
 * other languages: country code for flag matches locale code for language
 */
private fun getCountryCode(appLocale: AppLocale): String =
    if (appLocale == AppLocale.DEFAULT) "GB" else appLocale.code
