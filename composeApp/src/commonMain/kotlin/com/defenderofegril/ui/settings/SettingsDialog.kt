@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package com.defenderofegril.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.currentLanguage
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
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
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
                    val darkModeState = remember { AppSettings.isDarkMode }
                    GenericSwitch(
                        state = darkModeState,
                        checkedText = stringResource(Res.string.dark_mode_on),
                        uncheckedText = stringResource(Res.string.dark_mode_off),
                        onCheckedChange = { enabled ->
                            AppSettings.saveDarkMode(enabled)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
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
