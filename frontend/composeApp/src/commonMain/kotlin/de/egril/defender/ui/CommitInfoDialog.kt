package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.AppBuildInfo
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Dialog that displays commit information including hash, date, and message.
 * Shown when the user clicks on the version text on the main menu.
 */
@Composable
fun CommitInfoDialog(
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: IllegalStateException) {}
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Escape || event.key == Key.Back)) {
                    onDismiss()
                    true
                } else false
            },
        title = { Text(stringResource(Res.string.commit_info_title)) },
        text = {
            SelectionContainer {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Commit Hash
                Column {
                    Text(
                        text = stringResource(Res.string.commit_hash_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AppBuildInfo.COMMIT_HASH,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // Commit Date
                Column {
                    Text(
                        text = stringResource(Res.string.commit_date_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AppBuildInfo.COMMIT_DATE,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // Commit Message
                Column {
                    Text(
                        text = stringResource(Res.string.commit_message_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AppBuildInfo.COMMIT_MESSAGE,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(stringResource(Res.string.close))
                    ShortcutKeyChip(
                        text = "Esc",
                        color = LocalContentColor.current.copy(alpha = 0.75f)
                    )
                }
            }
        }
    )
}
