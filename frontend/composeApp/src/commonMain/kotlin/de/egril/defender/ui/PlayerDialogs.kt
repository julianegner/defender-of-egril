package de.egril.defender.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.save.PlayerProfile
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.icon.TrashIcon
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.formatTimestamp
import defender_of_egril.composeapp.generated.resources.*

/**
 * Dialog for creating or editing a player profile
 */
@Composable
fun PlayerNameDialog(
    initialName: String = "",
    isEdit: Boolean = false,
    showCancelButton: Boolean = true,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var playerName by remember { mutableStateOf(initialName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Pre-fetch error messages for use in non-composable contexts
    val emptyNameError = stringResource(Res.string.player_name_empty_error)
    val tooLongNameError = stringResource(Res.string.player_name_too_long_error)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier =
                Modifier
                    .widthIn(max = 400.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            SelectionContainer {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text =
                            if (isEdit) {
                                stringResource(Res.string.player_edit_title)
                            } else {
                                stringResource(Res.string.player_create_title)
                            },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text =
                            if (isEdit) {
                                stringResource(Res.string.player_edit_prompt)
                            } else {
                                stringResource(Res.string.player_create_prompt)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = playerName,
                        onValueChange = {
                            playerName = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(Res.string.player_name)) },
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )

                    // Auto-focus the text field
                    LaunchedEffect(Unit) {
                        try {
                            focusRequester.requestFocus()
                        } catch (_: IllegalStateException) {
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (showCancelButton) Arrangement.spacedBy(8.dp) else Arrangement.End,
                    ) {
                        if (showCancelButton) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(Res.string.cancel))
                                if (AppSettings.showButtonShortcutHints.value) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    ShortcutKeyChip(text = "Esc", color = LocalContentColor.current.copy(alpha = 0.75f))
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val trimmed = playerName.trim()
                                when {
                                    trimmed.isEmpty() -> errorMessage = emptyNameError
                                    trimmed.length > 50 -> errorMessage = tooLongNameError
                                    else -> onSave(trimmed)
                                }
                            },
                            modifier = if (showCancelButton) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (isEdit) {
                                    stringResource(Res.string.save)
                                } else {
                                    stringResource(Res.string.create)
                                },
                            )
                            if (AppSettings.showButtonShortcutHints.value) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ShortcutKeyChip(text = "Enter", color = LocalContentColor.current.copy(alpha = 0.75f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for creating a new player profile (wrapper for backward compatibility)
 */
@Composable
fun CreatePlayerDialog(
    showCancelButton: Boolean = true,
    onCreatePlayer: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerNameDialog(
        isEdit = false,
        showCancelButton = showCancelButton,
        onSave = onCreatePlayer,
        onDismiss = onDismiss,
    )
}

/**
 * Dialog for selecting a player profile or creating a new one
 */
@Composable
fun SelectPlayerDialog(
    players: List<PlayerProfile>,
    currentPlayerId: String?,
    onSelectPlayer: (String) -> Unit,
    onCreateNewPlayer: () -> Unit,
    onDeletePlayer: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Sort players by most recently played
    val sortedPlayers =
        remember(players, currentPlayerId) {
            players.sortedByDescending { it.lastPlayedAt }
        }
    val selectablePlayers =
        remember(sortedPlayers, currentPlayerId) {
            sortedPlayers.filter { it.id != currentPlayerId }
        }
    // Track which player is currently preselected (keyboard focus)
    var preselectedIndex by remember { mutableStateOf(if (selectablePlayers.isNotEmpty()) 0 else -1) }

    Dialog(onDismissRequest = onDismiss) {
        val dialogFocusRequester = remember { FocusRequester() }
        Surface(
            modifier =
                Modifier
                    .widthIn(max = 500.dp)
                    .heightIn(max = 600.dp)
                    .focusRequester(dialogFocusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.Escape, Key.Back -> {
                                    onDismiss()
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (selectablePlayers.isNotEmpty()) {
                                        preselectedIndex = (preselectedIndex - 1).coerceAtLeast(0)
                                    }
                                    true
                                }
                                Key.DirectionDown -> {
                                    if (selectablePlayers.isNotEmpty()) {
                                        preselectedIndex = (preselectedIndex + 1).coerceAtMost(selectablePlayers.size - 1)
                                    }
                                    true
                                }
                                Key.Tab -> {
                                    if (!event.isShiftPressed && selectablePlayers.isNotEmpty()) {
                                        preselectedIndex = (preselectedIndex + 1) % selectablePlayers.size
                                    } else if (event.isShiftPressed && selectablePlayers.isNotEmpty()) {
                                        preselectedIndex = (preselectedIndex - 1 + selectablePlayers.size) % selectablePlayers.size
                                    }
                                    true
                                }
                                Key.Enter -> {
                                    if (preselectedIndex in selectablePlayers.indices) {
                                        onSelectPlayer(selectablePlayers[preselectedIndex].id)
                                    }
                                    true
                                }
                                Key.N -> {
                                    if (!event.isCtrlPressed && !event.isAltPressed) {
                                        onCreateNewPlayer()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            LaunchedEffect(Unit) {
                try {
                    dialogFocusRequester.requestFocus()
                } catch (_: IllegalStateException) {
                }
            }
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.player_select_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (players.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.player_no_profiles),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedPlayers) { player ->
                            val isCurrentPlayer = player.id == currentPlayerId
                            val selectableIndex = selectablePlayers.indexOfFirst { it.id == player.id }
                            val isPreselected = !isCurrentPlayer && selectableIndex == preselectedIndex
                            PlayerProfileCard(
                                player = player,
                                isSelected = isCurrentPlayer,
                                isPreselected = isPreselected,
                                onSelect = { if (!isCurrentPlayer) onSelectPlayer(player.id) },
                                onDelete = { onDeletePlayer(player.id) },
                            )
                        }
                    }
                }

                // Navigation hints
                if (AppSettings.showButtonShortcutHints.value && selectablePlayers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ShortcutKeyChip(text = "\u2191/\u2193")
                            Text(
                                text = stringResource(Res.string.keyboard_nav_switch_tab),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ShortcutKeyChip(text = "Enter")
                            Text(
                                text = stringResource(Res.string.select),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.close))
                        if (AppSettings.showButtonShortcutHints.value) {
                            Spacer(modifier = Modifier.width(4.dp))
                            ShortcutKeyChip(text = "Esc", color = LocalContentColor.current.copy(alpha = 0.75f))
                        }
                    }

                    Button(
                        onClick = onCreateNewPlayer,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.player_new))
                        if (AppSettings.showButtonShortcutHints.value) {
                            Spacer(modifier = Modifier.width(4.dp))
                            ShortcutKeyChip(text = "N", color = LocalContentColor.current.copy(alpha = 0.75f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a player profile
 */
@Composable
private fun PlayerProfileCard(
    player: PlayerProfile,
    isSelected: Boolean,
    isPreselected: Boolean = false,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier.fillMaxWidth().then(
                if (isPreselected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium,
                    )
                } else {
                    Modifier
                },
            ),
        colors =
            if (isSelected) {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            } else if (isPreselected) {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            } else {
                CardDefaults.cardColors()
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text =
                        stringResource(
                            Res.string.player_last_played,
                            formatTimestamp(player.lastPlayedAt),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isSelected) {
                    if (isPreselected && AppSettings.showButtonShortcutHints.value) {
                        // Show [Enter] to select chip when preselected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ShortcutKeyChip(text = "Enter")
                            Text(
                                text = stringResource(Res.string.select),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Button(onClick = onSelect) {
                            Text(stringResource(Res.string.select))
                        }
                    }
                }

                val deleteLabel = stringResource(Res.string.delete)
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.semantics { contentDescription = deleteLabel },
                ) {
                    TrashIcon(size = 20.dp)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.player_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        Res.string.player_delete_confirm_message,
                        player.name,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
