package de.egril.defender.ui.worldmap

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.icon.ToolsIcon
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun EditorButtonCard(onClick: () -> Unit) {
    val isDarkMode = AppSettings.isDarkMode.value
    val backgroundColor = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFFF9800) // Lighter orange in dark mode
    // Text color - white for good contrast on orange backgrounds
    val textColor = Color.White

    Button(
        onClick = onClick,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = textColor,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Distinctive symbol - wrench/hammer icon
            ToolsIcon(size = 20.dp)

            Text(
                text = stringResource(Res.string.level_editor),
                style = MaterialTheme.typography.bodyMedium,
            )
            ShortcutKeyChip(text = "O", color = Color.White.copy(alpha = 0.75f))
        }
    }
}
