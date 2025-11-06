package com.defenderofegril.ui.worldmap

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheatCodeDialog(
    onDismiss: () -> Unit,
    onApplyCheatCode: (String) -> Boolean
) {
    var cheatCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = {
            onDismiss()
            cheatCodeInput = ""
            errorMessage = ""
        },
        title = { Text("Cheat Code") },
        text = {
            Column {
                Text("Enter cheat code:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = cheatCodeInput,
                    onValueChange = { 
                        cheatCodeInput = it
                        errorMessage = ""  // Clear error when user types
                    },
                    placeholder = { Text("unlock") },
                    isError = errorMessage.isNotEmpty()
                )
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = onApplyCheatCode(cheatCodeInput)
                    if (success) {
                        onDismiss()
                        cheatCodeInput = ""
                        errorMessage = ""
                    } else {
                        errorMessage = "Invalid cheat code"
                    }
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    onDismiss()
                    cheatCodeInput = ""
                    errorMessage = ""
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
