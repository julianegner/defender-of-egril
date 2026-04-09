package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*

/**
 * Dialog shown on first app start to allow user to choose their language
 * Automatically preselects platform language if supported
 */
@Composable
fun InitialLanguageChooserDialog(
    onLanguageSelected: () -> Unit
) {
    // Detect and preselect platform language on first composition
    LaunchedEffect(Unit) {
        AppSettings.detectAndPreselectPlatformLanguage()
    }
    
    Dialog(onDismissRequest = { /* Cannot dismiss - must choose language */ }) {
        Surface(
            modifier = Modifier.widthIn(max = 500.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SelectableText(
                    text = stringResource(Res.string.initial_language_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SelectableText(
                    text = stringResource(Res.string.initial_language_prompt),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Language chooser component
                LanguageChooser(
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        // Save the selected language and mark as chosen
                        val selectedLanguage = com.hyperether.resources.currentLanguage.value
                        AppSettings.saveLanguage(selectedLanguage)
                        AppSettings.markLanguageChosen()
                        onLanguageSelected()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.initial_language_continue))
                }
            }
        }
    }
}
