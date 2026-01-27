package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.DigOutcomeIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun DigOutcomeDialog(
    outcome: DigOutcome,
    onDismiss: () -> Unit,
    onResetSelections: () -> Unit,
    dragonName: String? = null  // Optional dragon name for DRAGON outcome
) {
    // Reset selections when dragon awakes
    if (outcome == DigOutcome.DRAGON) {
        onResetSelections()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.mining_result)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image on the left
                DigOutcomeIcon(
                    outcome = outcome,
                    size = 80.dp
                )
                
                // Text on the right
                val message = when (outcome) {
                    DigOutcome.NOTHING -> stringResource(Res.string.found_nothing)
                    DigOutcome.BRASS -> "${stringResource(Res.string.found_brass)}\n+${outcome.coins} ${stringResource(Res.string.coins)}"
                    DigOutcome.SILVER -> "${stringResource(Res.string.found_silver)}\n+${outcome.coins} ${stringResource(Res.string.coins)}"
                    DigOutcome.GOLD -> "${stringResource(Res.string.found_gold)}\n+${outcome.coins} ${stringResource(Res.string.coins)}"
                    DigOutcome.GEMS -> "${stringResource(Res.string.found_gems)}\n+${outcome.coins} ${stringResource(Res.string.coins)}"
                    DigOutcome.DIAMOND -> "${stringResource(Res.string.found_diamond)}\n+${outcome.coins} ${stringResource(Res.string.coins)}"
                    DigOutcome.DRAGON -> {
                        if (dragonName != null) {
                            "${stringResource(Res.string.the_dragon)} $dragonName ${stringResource(Res.string.awakens)}!\n${stringResource(Res.string.mine_destroyed)}"
                        } else {
                            "${stringResource(Res.string.dragon_awakens)}\n${stringResource(Res.string.mine_destroyed)}"
                        }
                    }
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        }
    )
}

@Composable
fun SaveGameDialog(
    saveCommentInput: String,
    onSaveCommentChange: (String) -> Unit,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.save_game)) },
        text = {
            Column {
                Text(
                    stringResource(Res.string.add_optional_comment),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = saveCommentInput,
                    onValueChange = {
                        // Limit comment to 200 characters
                        if (it.length <= 200) {
                            onSaveCommentChange(it)
                        }
                    },
                    placeholder = { Text(stringResource(Res.string.save_comment_placeholder)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            "${saveCommentInput.length}/200",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val comment = if (saveCommentInput.isBlank()) null else saveCommentInput.trim()
                    onSave(comment)
                }
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
fun SaveConfirmationDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.game_saved)) },
        text = {
            Text(stringResource(Res.string.game_saved_successfully), style = MaterialTheme.typography.bodyLarge)
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        }
    )
}

@Composable
fun UnsavedChangesDialog(
    onSaveAndExit: () -> Unit,
    onDiscardChanges: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.unsaved_changes_title)) },
        text = {
            Text(
                stringResource(Res.string.unsaved_changes_message),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    onClick = onDiscardChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.discard_changes))
                }
                Button(onClick = onSaveAndExit) {
                    Text(stringResource(Res.string.save_and_exit))
                }
            }
        }
    )
}



@Composable
fun EndTurnConfirmationDialog(
    onConfirm: () -> Unit,
    onAutoAttackAndConfirm: () -> Unit,
    onCancel: () -> Unit,
    showAutoAttackButton: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.end_turn_confirmation_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(Res.string.end_turn_confirmation_message),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (showAutoAttackButton) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(Res.string.auto_attack_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GamePlayColors.WarningDeep
                        )
                    ) {
                        Text(stringResource(Res.string.end_turn_confirm))
                    }
                }
                if (showAutoAttackButton) {
                    Button(
                        onClick = onAutoAttackAndConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(Res.string.auto_attack_and_end_turn))
                    }
                }
            }
        }
    )
}

@Composable
fun SpecialActionsRemainingDialog(
    remainingTypes: List<DefenderType>,
    onContinueTurn: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinueTurn,
        title = { Text(stringResource(Res.string.special_actions_remaining_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(Res.string.special_actions_remaining_message),
                    style = MaterialTheme.typography.bodyLarge
                )
                
                // List each tower type with remaining actions
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    remainingTypes.forEach { type ->
                        val message = when (type) {
                            DefenderType.DWARVEN_MINE -> stringResource(Res.string.dwarven_mine_actions)
                            DefenderType.ALCHEMY_TOWER -> stringResource(Res.string.alchemy_tower_actions)
                            DefenderType.WIZARD_TOWER -> stringResource(Res.string.wizard_tower_actions)
                            else -> ""
                        }
                        if (message.isNotEmpty()) {
                            Text(
                                text = "• $message",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onContinueTurn) {
                Text(stringResource(Res.string.continue_turn))
            }
        }
    )
}

/**
 * Shows time-based reminder messages to encourage breaks and sleep
 * @param type The type of reminder (BREAK or SLEEP)
 * @param elapsedTime Optional formatted elapsed time for break reminders
 * @param timeDescription Optional time description for sleep reminders (close to midnight/midnight/after midnight)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun ReminderDialog(
    type: ReminderType,
    elapsedTime: String? = null,
    timeDescription: String? = null,
    onDismiss: () -> Unit
) {
    val title = when (type) {
        ReminderType.BREAK -> stringResource(Res.string.time_for_break_title)
        ReminderType.SLEEP -> stringResource(Res.string.time_for_sleep_title)
    }
    
    val message = when (type) {
        ReminderType.BREAK -> {
            elapsedTime?.let { 
                stringResource(Res.string.time_for_break_message, it)
            } ?: ""
        }
        ReminderType.SLEEP -> timeDescription ?: ""
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon on the left
                when (type) {
                    ReminderType.BREAK -> de.egril.defender.ui.icon.CoffeeIcon(size = 60.dp)
                    ReminderType.SLEEP -> de.egril.defender.ui.icon.BedIcon(size = 60.dp)
                }
                
                // Text on the right
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        }
    )
}

/**
 * Type of reminder message
 */
enum class ReminderType {
    BREAK,  // Break reminder every 2 hours
    SLEEP   // Sleep reminder after 23:00
}
