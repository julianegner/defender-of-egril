package de.egril.defender.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.infopage.NewVersionInfo
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.yield

/**
 * Dialog shown at start-up when a newer version of the app is available on GitHub.
 *
 * @param info       Details about the available update.
 * @param onDismiss  Called when the user dismisses the dialog.
 */
@Composable
fun NewVersionDialog(
    infos: List<NewVersionInfo>,
    onDismiss: () -> Unit,
) {
    if (infos.isEmpty()) {
        return
    }

    val uriHandler = LocalUriHandler.current
    val focusRequester = remember { FocusRequester() }
    val primaryInfo = infos.first()
    val secondaryInfos = infos.drop(1)
    val showChannelLabels = infos.size > 1
    val openReleasePage =
        remember(uriHandler, onDismiss) {
            { releasePageUrl: String ->
                uriHandler.openUri(releasePageUrl)
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
        modifier =
            Modifier
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
                                    openReleasePage(primaryInfo.releasePageUrl)
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionContainer {
                    Text(updateMessage(primaryInfo, showChannelLabels))
                }
                secondaryInfos.forEach { info ->
                    SelectionContainer {
                        Text(updateMessage(info, showChannelLabels))
                    }
                    TextButton(onClick = { openReleasePage(info.releasePageUrl) }) {
                        Text(updateButtonLabel(info, showChannelLabels))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { openReleasePage(primaryInfo.releasePageUrl) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(updateButtonLabel(primaryInfo, showChannelLabels))
                    ShortcutKeyChip(
                        text = "Enter",
                        color = LocalContentColor.current.copy(alpha = 0.75f),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(Res.string.close))
                    ShortcutKeyChip(
                        text = "Esc",
                        color = LocalContentColor.current.copy(alpha = 0.75f),
                    )
                }
            }
        },
    )
}

@Composable
private fun updateMessage(
    info: NewVersionInfo,
    showChannelLabels: Boolean,
): String {
    return if (showChannelLabels) {
        stringResource(
            Res.string.new_version_available_message_with_channel,
            info.version,
            channelLabel(info),
        )
    } else {
        stringResource(Res.string.new_version_available_message, info.version)
    }
}

@Composable
private fun updateButtonLabel(
    info: NewVersionInfo,
    showChannelLabels: Boolean,
): String {
    return if (showChannelLabels) {
        stringResource(
            Res.string.new_version_go_to_releases_with_version_and_channel,
            info.version,
            channelLabel(info),
        )
    } else {
        stringResource(Res.string.new_version_go_to_releases_with_version, info.version)
    }
}

@Composable
private fun channelLabel(info: NewVersionInfo): String {
    return if (info.isBetaRelease) {
        stringResource(Res.string.new_version_release_type_beta)
    } else {
        stringResource(Res.string.new_version_release_type_stable)
    }
}
