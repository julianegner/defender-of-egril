package de.egril.defender.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleUpIcon
import dev.carlsen.flagkit.FlagKit

/**
 * Language chooser dropdown component
 * Displays the current language with its flag and allows switching between supported languages
 */
@Composable
fun LanguageChooser(
    modifier: Modifier = Modifier,
    onLanguageChanged: ((AppLocale) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageFlagAndName(currentLanguage.value)
        }
        
        // Dropdown arrow icon
        if (expanded) {
            TriangleUpIcon(
                size = 14.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )
        } else {
            TriangleDownIcon(
                size = 14.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLocale.entries.forEach { locale ->
                DropdownMenuItem(
                    text = {
                        LanguageFlagAndName(locale)
                    },
                    onClick = {
                        currentLanguage.value = locale
                        onLanguageChanged?.invoke(locale)
                        expanded = false
                    }
                )
            }
        }
    }
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
