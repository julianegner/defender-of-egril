@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

/**
 * Settings dialog that provides access to app settings like language selection and dark mode
 */
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 500.dp)
                .wrapContentHeight()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = stringResource(Res.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                HorizontalDivider()
                
                // Appearance section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.appearance),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Dark mode switch
                    GenericSwitch(
                        state = AppSettings.isDarkMode,
                        checkedText = stringResource(Res.string.dark_mode_on),
                        uncheckedText = stringResource(Res.string.dark_mode_off),
                        onCheckedChange = { enabled ->
                            AppSettings.saveDarkMode(enabled)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                HorizontalDivider()
                
                // Sound section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.sound),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Sound enabled/disabled switch
                    GenericSwitch(
                        state = AppSettings.isSoundEnabled,
                        checkedText = stringResource(Res.string.sound_enabled),
                        uncheckedText = stringResource(Res.string.sound_disabled),
                        onCheckedChange = { enabled ->
                            AppSettings.saveSoundEnabled(enabled)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Volume slider (only shown when sound is enabled)
                    if (AppSettings.isSoundEnabled.value) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.sound_volume),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔈",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Slider(
                                    value = AppSettings.soundVolume.value,
                                    onValueChange = { volume ->
                                        AppSettings.saveSoundVolume(volume)
                                    },
                                    modifier = Modifier.weight(1f),
                                    valueRange = 0f..1f
                                )
                                Text(
                                    text = "🔊",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Language section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    LanguageChooser(
                        modifier = Modifier.fillMaxWidth(),
                        onLanguageChanged = { locale ->
                            AppSettings.saveLanguage(locale)
                        }
                    )
                }
                
                HorizontalDivider()
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset button
                    OutlinedButton(
                        onClick = {
                            AppSettings.resetToDefaults()
                        }
                    ) {
                        Text(stringResource(Res.string.reset_settings))
                    }
                    
                    // Close button
                    Button(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(Res.string.close))
                    }
                }
            }
        }
    }
}
