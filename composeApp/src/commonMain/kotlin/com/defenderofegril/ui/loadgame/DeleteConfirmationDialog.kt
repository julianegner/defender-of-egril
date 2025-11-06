package com.defenderofegril.ui.loadgame

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun DeleteConfirmationDialog(
    saveIdToDelete: String?,
    onConfirmDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    saveIdToDelete?.let { saveId ->
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Save Game") },
            text = { Text("Are you sure you want to delete this saved game?") },
            confirmButton = {
                Button(
                    onClick = { onConfirmDelete(saveId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
