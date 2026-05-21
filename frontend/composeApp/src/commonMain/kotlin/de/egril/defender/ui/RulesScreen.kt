@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.infopage.HowToPlayContent
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.ui.feedback.FeedbackButton
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

@Composable
fun RulesScreen(
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: IllegalStateException) {}
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Back || event.key == Key.Escape)
                ) {
                    onBack()
                    true
                } else {
                    false
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Settings and Feedback buttons in top-right corner
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedbackButton(shortcutKey = ".")
                SettingsButton(shortcutKey = ",")
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = stringResource(Res.string.how_to_play),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Scrollable content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    HowToPlayContent()
                }

                // Back button
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(stringResource(Res.string.back))
                    if (AppSettings.showButtonShortcutHints.value) {
                        Spacer(modifier = Modifier.width(4.dp))
                        ShortcutKeyChip(text = "Esc", color = LocalContentColor.current.copy(alpha = 0.75f))
                    }
                }
            }
        }
    }
}
