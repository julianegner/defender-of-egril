package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.defenderofegril.model.*
import com.defenderofegril.ui.*
import com.defenderofegril.ui.icon.DigOutcomeIcon

@Composable
fun DigOutcomeDialog(
    outcome: DigOutcome,
    onDismiss: () -> Unit,
    onResetSelections: () -> Unit
) {
    // Reset selections when dragon awakes
    if (outcome == DigOutcome.DRAGON) {
        onResetSelections()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mining Result") },
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
                    DigOutcome.NOTHING -> "You found nothing..."
                    DigOutcome.BRASS -> "You found brass!\n+${outcome.coins} coins"
                    DigOutcome.SILVER -> "You found silver!\n+${outcome.coins} coins"
                    DigOutcome.GOLD -> "You found gold!\n+${outcome.coins} coins"
                    DigOutcome.GEMS -> "You found gems!\n+${outcome.coins} coins"
                    DigOutcome.DIAMOND -> "You found a diamond!\n+${outcome.coins} coins"
                    DigOutcome.DRAGON -> "A DRAGON AWAKENS!\nThe mine is destroyed!"
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
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
        title = { Text("Save Game") },
        text = {
            Column {
                Text(
                    "Add an optional comment to help identify this save:",
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
                    placeholder = { Text("e.g., 'Before final wave', 'Good position'...") },
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
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
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
        title = { Text("Game Saved") },
        text = {
            Text("Your game has been saved successfully!", style = MaterialTheme.typography.bodyLarge)
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
