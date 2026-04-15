@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Composable displaying installation instructions for all platforms
 */
@Composable
fun InstallationInfo() {
    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
        // Header
        Text(
            text = stringResource(Res.string.installation_info_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Windows Section
            PlatformSection(
                title = stringResource(Res.string.installation_windows_title),
                content = {
                    InstallationStep(stringResource(Res.string.installation_windows_step1))
                    InstallationStep(stringResource(Res.string.installation_windows_step2))
                    InstallationStep(stringResource(Res.string.installation_windows_step3))
                    InstallationSubStep(stringResource(Res.string.installation_windows_step3a))
                    InstallationSubStep(stringResource(Res.string.installation_windows_step3b))
                    InstallationNote(stringResource(Res.string.installation_windows_note))
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Android Section
            PlatformSection(
                title = stringResource(Res.string.installation_android_title),
                content = {
                    InstallationStep(stringResource(Res.string.installation_android_step1))
                    InstallationSubStep(stringResource(Res.string.installation_android_step1a))
                    InstallationSubStep(stringResource(Res.string.installation_android_step1b))
                    InstallationStep(stringResource(Res.string.installation_android_step2))
                    InstallationStep(stringResource(Res.string.installation_android_step3))
                    InstallationStep(stringResource(Res.string.installation_android_step4))
                    InstallationNote(stringResource(Res.string.installation_android_note))
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // iOS Section
            PlatformSection(
                title = stringResource(Res.string.installation_ios_title),
                content = {
                    InstallationStep(stringResource(Res.string.installation_ios_step1))
                    InstallationStep(stringResource(Res.string.installation_ios_step2))
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Linux Section
            PlatformSection(
                title = stringResource(Res.string.installation_linux_title),
                content = {
                    InstallationStep(stringResource(Res.string.installation_linux_step1))
                    InstallationSubStep(stringResource(Res.string.installation_linux_step1a))
                    InstallationSubStep(stringResource(Res.string.installation_linux_step1b))
                    InstallationStep(stringResource(Res.string.installation_linux_step2))
                    InstallationSubStep(stringResource(Res.string.installation_linux_step2a))
                    InstallationSubStep(stringResource(Res.string.installation_linux_step2b))
                    InstallationStep(stringResource(Res.string.installation_linux_step3))
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // macOS Section
            PlatformSection(
                title = stringResource(Res.string.installation_macos_title),
                content = {
                    InstallationStep(stringResource(Res.string.installation_macos_step1))
                    InstallationStep(stringResource(Res.string.installation_macos_step2))
                    InstallationStep(stringResource(Res.string.installation_macos_step3))
                    InstallationNote(stringResource(Res.string.installation_macos_note))
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Web/Browser Section
            PlatformSection(
                title = stringResource(Res.string.installation_web_title),
                content = {
                    InstallationStep(stringResource(Res.string.installation_web_step1))
                    InstallationStep(stringResource(Res.string.installation_web_step2))
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Steam Deck Section
            PlatformSection(
                title = stringResource(Res.string.installation_steam_deck_title),
                content = {
                    InstallationStep(stringResource(Res.string.installation_steam_deck_step1))
                    InstallationStep(stringResource(Res.string.installation_steam_deck_step2))
                    InstallationStep(stringResource(Res.string.installation_steam_deck_step3))
                    InstallationStep(stringResource(Res.string.installation_steam_deck_step4))
                    InstallationStep(stringResource(Res.string.installation_steam_deck_step5))
                    InstallationStep(stringResource(Res.string.installation_steam_deck_step6))
                    InstallationStep(stringResource(Res.string.installation_steam_deck_step7))
                    InstallationNote(stringResource(Res.string.installation_steam_deck_note))
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Link to detailed documentation
            Text(
                text = stringResource(Res.string.installation_details_link),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
        }
    }
}

@Composable
private fun PlatformSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        content()
    }
}

@Composable
private fun InstallationStep(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun InstallationSubStep(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun InstallationNote(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun CommandText(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Platform-specific impressum section
 * Implemented only for WASM platform
 */
@Composable
fun ImpressumSection() {
    // Only show impressum if the compile flag is set
    if (!de.egril.defender.WithImpressum.withImpressum) {
        return
    }
    
    Spacer(modifier = Modifier.height(20.dp))
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = de.egril.defender.ui.ImpressumConstants.IMPRESSUM_TITLE,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Impressum content
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = buildString {
                    append(de.egril.defender.ui.ImpressumConstants.IMPRESSUM_NAME)
                    append("\n")
                    append(de.egril.defender.ui.ImpressumConstants.IMPRESSUM_STREET)
                    append("\n")
                    append(de.egril.defender.ui.ImpressumConstants.IMPRESSUM_POSTAL_CODE)
                    append(" ")
                    append(de.egril.defender.ui.ImpressumConstants.IMPRESSUM_CITY)
                    append("\n")
                    append(de.egril.defender.ui.ImpressumConstants.IMPRESSUM_COUNTRY)
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "${de.egril.defender.ui.ImpressumConstants.IMPRESSUM_EMAIL_LABEL}${de.egril.defender.ui.ImpressumConstants.IMPRESSUM_EMAIL}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
