package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.egril.defender.save.PlayerProfile
import de.egril.defender.utils.formatTimestamp
import de.egril.defender.ui.icon.TrashIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Dialog for creating or editing a player profile
 */
@Composable
fun PlayerNameDialog(
    initialName: String = "",
    isEdit: Boolean = false,
    showCancelButton: Boolean = true,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var playerName by remember { mutableStateOf(initialName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Pre-fetch error messages for use in non-composable contexts
    val emptyNameError = stringResource(Res.string.player_name_empty_error)
    val tooLongNameError = stringResource(Res.string.player_name_too_long_error)
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 400.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            SelectionContainer {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEdit) {
                        stringResource(Res.string.player_edit_title)
                    } else {
                        stringResource(Res.string.player_create_title)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isEdit) {
                        stringResource(Res.string.player_edit_prompt)
                    } else {
                        stringResource(Res.string.player_create_prompt)
                    },
                    style = MaterialTheme.typography.bodyMedium
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
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (showCancelButton) Arrangement.spacedBy(8.dp) else Arrangement.End
                ) {
                    if (showCancelButton) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.cancel))
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
                        modifier = if (showCancelButton) Modifier.weight(1f) else Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEdit) {
                            stringResource(Res.string.save)
                        } else {
                            stringResource(Res.string.create)
                        })
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
    onDismiss: () -> Unit
) {
    PlayerNameDialog(
        isEdit = false,
        showCancelButton = showCancelButton,
        onSave = onCreatePlayer,
        onDismiss = onDismiss
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
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 500.dp).heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.player_select_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (players.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.player_no_profiles),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(players.sortedByDescending { it.lastPlayedAt }) { player ->
                            PlayerProfileCard(
                                player = player,
                                isSelected = player.id == currentPlayerId,
                                onSelect = { onSelectPlayer(player.id) },
                                onDelete = { onDeletePlayer(player.id) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.close))
                    }
                    
                    Button(
                        onClick = onCreateNewPlayer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.player_new))
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
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(
                        Res.string.player_last_played,
                        formatTimestamp(player.lastPlayedAt)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isSelected) {
                    Button(onClick = onSelect) {
                        Text(stringResource(Res.string.select))
                    }
                }
                
                IconButton(
                    onClick = { showDeleteConfirm = true }
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
                        player.name
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}
