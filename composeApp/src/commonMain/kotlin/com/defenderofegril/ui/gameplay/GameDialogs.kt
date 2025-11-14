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
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

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
                    DigOutcome.DRAGON -> "${stringResource(Res.string.dragon_awakens)}\n${stringResource(Res.string.mine_destroyed)}"
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
fun DragonInfoOverlay(
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .padding(8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = stringResource(Res.string.dragon_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Content
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.dragon_info_movement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.dragon_info_eating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.dragon_info_level),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Got it button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.got_it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
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
