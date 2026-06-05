package de.egril.defender.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.infopage.NewVersionInfo
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer
import kotlinx.coroutines.yield

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
    val focusRequester = remember { FocusRequester() }
    val openReleasePage = remember(info.releasePageUrl, uriHandler, onDismiss) {
        {
            uriHandler.openUri(info.releasePageUrl)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        yield()
        try {
            focusRequester.requestFocus()
        } catch (_: IllegalStateException) {
            // Dialog content may not be attached on the first frame in some environments.
        }
    }

    AlertDialog(
        modifier = Modifier
            .testTag("newVersionDialog")
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            if (!event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed) {
                                openReleasePage()
                                true
                            } else {
                                false
                            }
                        }

                        Key.Escape, Key.Back -> {
                            onDismiss()
                            true
                        }

                        else -> false
                    }
                }
            },
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.new_version_available_title)) },
        text = {
            SelectionContainer {
                Text(stringResource(Res.string.new_version_available_message, info.version))
            }
        },
        confirmButton = {
            Button(onClick = openReleasePage) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(stringResource(Res.string.new_version_go_to_releases))
                    ShortcutKeyChip(
                        text = "Enter",
                        color = LocalContentColor.current.copy(alpha = 0.75f)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
