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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleUpIcon
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
    var expanded by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableStateOf(-1) }

    // Handle external trigger to open dropdown
    LaunchedEffect(triggerOpen) {
        if (triggerOpen) {
            expanded = true
            onTriggerOpenHandled()
        }
    }

    // Reset highlighted index when dropdown opens
    LaunchedEffect(expanded) {
        if (expanded) {
            highlightedIndex = AppLocale.entries.indexOf(currentLanguage.value).coerceAtLeast(0)
        }
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        expanded && event.key == Key.DirectionDown -> {
                            highlightedIndex = (highlightedIndex + 1).coerceAtMost(AppLocale.entries.size - 1)
                            true
                        }
                        expanded && event.key == Key.DirectionUp -> {
                            highlightedIndex = (highlightedIndex - 1).coerceAtLeast(0)
                            true
                        }
                        expanded && event.key == Key.Enter -> {
                            val locale = AppLocale.entries.getOrNull(highlightedIndex)
                            if (locale != null) {
                                currentLanguage.value = locale
                                onLanguageChanged?.invoke(locale)
                            }
                            expanded = false
                            true
                        }
                        expanded && event.key == Key.Escape -> {
                            expanded = false
                            true
                        }
                        !expanded && (event.key == Key.Enter || event.key == Key.Spacebar) -> {
                            expanded = true
                            true
                        }
                        else -> false
                    }
                } else false
            }
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
            AppLocale.entries.forEachIndexed { index, locale ->
                DropdownMenuItem(
                    text = {
                        LanguageFlagAndName(locale)
                    },
                    onClick = {
                        currentLanguage.value = locale
                        onLanguageChanged?.invoke(locale)
                        expanded = false
                    },
                    colors = if (index == highlightedIndex) {
                        MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        MenuDefaults.itemColors()
                    },
                    modifier = if (index == highlightedIndex) {
                        Modifier.border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            RoundedCornerShape(4.dp)
                        )
                    } else {
                        Modifier
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
