package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

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
        title = { Text(stringResource(Res.string.platform_information_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.platform_information_label))
                Spacer(modifier = Modifier.height(8.dp))
                Text(platformInfo, style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        }
    )
}
