package de.egril.defender.ui.loadgame

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun DeleteConfirmationDialog(
    saveIdToDelete: String?,
    onConfirmDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    saveIdToDelete?.let { saveId ->
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.delete_save_game)) },
            text = { Text(stringResource(Res.string.delete_save_game_confirm)) },
            confirmButton = {
                Button(
                    onClick = { onConfirmDelete(saveId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}
