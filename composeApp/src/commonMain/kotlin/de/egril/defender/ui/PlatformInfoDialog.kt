package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog that displays platform information.
 * Used by the "platform" cheat code.
 */
@Composable
fun PlatformInfoDialog(
    platformInfo: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Platform Information") },
        text = {
            Column {
                Text("Current platform:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(platformInfo, style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
