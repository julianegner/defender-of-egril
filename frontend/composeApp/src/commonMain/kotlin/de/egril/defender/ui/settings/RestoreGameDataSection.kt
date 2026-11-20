@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.RepositoryManager
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.utils.isPlatformWasm
import de.egril.defender.utils.reloadApp
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.launch

/**
 * Section with a "Restore Game Data" button that clears cached game data and reloads
 * from the bundled repository. On web it reloads the browser; on other platforms it
 * performs a backup-and-restore via RepositoryManager and reloads the world map data.
 */
@Composable
internal fun RestoreGameDataSection(
    onDismissSettings: () -> Unit,
    triggerRestore: Boolean = false,
    onRestoreHandled: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf<Pair<Boolean, String?>?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    // Handle keyboard trigger
    LaunchedEffect(triggerRestore) {
        if (triggerRestore) {
            showConfirmDialog = true
            onRestoreHandled()
        }
    }

    val confirmMessage =
        if (isPlatformWasm) {
            stringResource(Res.string.restore_game_data_confirm_message_web)
        } else {
            stringResource(Res.string.restore_game_data_confirm_message)
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SelectableText(
            text = stringResource(Res.string.game_data),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedButton(
            onClick = { showConfirmDialog = true },
            enabled = !isRestoring,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.restore_game_data))
            if (AppSettings.showButtonShortcutHints.value) {
                Spacer(modifier = Modifier.width(8.dp))
                de.egril.defender.ui.gameplay
                    .ShortcutKeyChip(text = "R")
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(Res.string.restore_game_data_confirm_title)) },
            text = { SelectionContainer { Text(confirmMessage) } },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isRestoring = true
                        coroutineScope.launch {
                            val backupPath = RepositoryManager.restoreFromRepository()
                            isRestoring = false
                            if (isPlatformWasm) {
                                reloadApp()
                            } else {
                                showResultDialog = (backupPath != null) to backupPath
                            }
                        }
                    },
                ) {
                    Text(stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    val resultDialogState = showResultDialog
    if (resultDialogState != null) {
        val (resultSuccess, resultPath) = resultDialogState
        val title =
            if (resultSuccess) {
                stringResource(Res.string.restore_game_data_success_title)
            } else {
                stringResource(Res.string.restore_game_data_failure_title)
            }
        val message =
            if (resultSuccess && resultPath != null) {
                stringResource(Res.string.restore_game_data_success_message, resultPath)
            } else {
                stringResource(Res.string.restore_game_data_failure_message)
            }
        AlertDialog(
            onDismissRequest = { showResultDialog = null },
            title = { Text(title) },
            text = { SelectionContainer { Text(message) } },
            confirmButton = {
                Button(onClick = {
                    showResultDialog = null
                    if (resultSuccess) {
                        RepositoryManager.onDataRestored?.invoke()
                        onDismissSettings()
                    }
                }) {
                    Text(stringResource(Res.string.close))
                }
            },
        )
    }
}
