package de.egril.defender.ui.loadgame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Dialog displayed when trying to import a save file that already exists
 */
@Composable
fun FileOverrideDialog(
    filename: String?,
    onSkip: () -> Unit,
    onOverride: () -> Unit,
    onOverrideAll: () -> Unit,
    onDismiss: () -> Unit
) {
    if (filename != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(Res.string.file_override_title))
            },
            text = {
                SelectionContainer {
                Text(
                    text = stringResource(Res.string.file_override_message, filename)
                )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(Res.string.skip_file))
                    }
                    TextButton(onClick = onOverride) {
                        Text(stringResource(Res.string.override_file))
                    }
                    TextButton(onClick = onOverrideAll) {
                        Text(stringResource(Res.string.override_all))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

/**
 * Dialog displayed after successful import
 */
@Composable
fun ImportSuccessDialog(
    filesImported: Int,
    onDismiss: () -> Unit
) {
    if (filesImported > 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(Res.string.files_imported))
            },
            text = {
                SelectionContainer {
                Text(
                    text = stringResource(Res.string.files_imported_message, filesImported)
                )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }
}

/**
 * Dialog displayed when import fails
 */
@Composable
fun ImportErrorDialog(
    showError: Boolean,
    onDismiss: () -> Unit
) {
    if (showError) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(Res.string.import_error))
            },
            text = {
                SelectionContainer {
                Text(text = stringResource(Res.string.import_error_message))
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }
}
