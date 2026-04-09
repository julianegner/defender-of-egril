package de.egril.defender.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.hyperether.resources.stringResource
import de.egril.defender.ui.infopage.NewVersionInfo
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Dialog shown at start-up when a newer version of the app is available on GitHub.
 *
 * @param info       Details about the available update.
 * @param onDismiss  Called when the user dismisses the dialog.
 */
@Composable
fun NewVersionDialog(
    info: NewVersionInfo,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.new_version_available_title)) },
        text = {
            SelectionContainer {
                Text(stringResource(Res.string.new_version_available_message, info.version))
            }
        },
        confirmButton = {
            Button(onClick = {
                uriHandler.openUri(info.releasePageUrl)
                onDismiss()
            }) {
                Text(stringResource(Res.string.new_version_go_to_releases))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        }
    )
}
