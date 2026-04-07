package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.TooltipWrapper
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.settings

/**
 * Settings button with gear icon that opens the settings dialog
 * Can be placed in any screen to provide access to settings
 */
@Composable
fun SettingsButton(
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    val settingsLabel = stringResource(Res.string.settings)

    TooltipWrapper(text = settingsLabel) {
        IconButton(
            onClick = { showSettings = true },
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = settingsLabel,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    
    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false }
        )
    }
}
