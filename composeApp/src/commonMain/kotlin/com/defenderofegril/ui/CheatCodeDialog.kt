package com.defenderofegril.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Unified cheat code dialog used in both WorldMapScreen and GamePlayScreen.
 * Supports both internal state management and external state management.
 */
@Composable
fun CheatCodeDialog(
    onDismiss: () -> Unit,
    onApplyCheatCode: (String) -> Boolean,
    showHints: Boolean = false,
    initialInput: String = "",
    onInputChange: ((String) -> Unit)? = null
) {
    var internalInput by remember { mutableStateOf(initialInput) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Use external state if callback provided, otherwise use internal state
    val cheatCodeInput = if (onInputChange != null) initialInput else internalInput
    val handleInputChange: (String) -> Unit = if (onInputChange != null) {
        { newValue ->
            onInputChange(newValue)
            errorMessage = ""
        }
    } else {
        { newValue ->
            internalInput = newValue
            errorMessage = ""
        }
    }
    
    AlertDialog(
        onDismissRequest = {
            onDismiss()
            if (onInputChange == null) {
                internalInput = ""
            }
            errorMessage = ""
        },
        title = { Text("Cheat Code") },
        text = {
            Column {
                Text("Enter cheat code:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = cheatCodeInput,
                    onValueChange = handleInputChange,
                    singleLine = true,
                    placeholder = { Text(if (showHints) "e.g. moneybags" else "unlock") },
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
                
                if (showHints) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Available codes:", style = MaterialTheme.typography.labelSmall)
                    Text("• cash - Get 1000 coins", style = MaterialTheme.typography.bodySmall)
                    Text("• mmmoney - Get a million coins", style = MaterialTheme.typography.bodySmall)
                    Text("• dragon - Spawn dragon from mine", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = onApplyCheatCode(cheatCodeInput)
                    if (success) {
                        onDismiss()
                        if (onInputChange == null) {
                            internalInput = ""
                        }
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
                    if (onInputChange == null) {
                        internalInput = ""
                    }
                    errorMessage = ""
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
