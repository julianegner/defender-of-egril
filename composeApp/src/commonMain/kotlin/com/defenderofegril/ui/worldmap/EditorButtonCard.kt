package com.defenderofegril.ui.worldmap

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.defenderofegril.ui.icon.ToolsIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun EditorButtonCard(
    onClick: () -> Unit
) {
    val isDarkMode = com.defenderofegril.ui.settings.AppSettings.isDarkMode.value
    val backgroundColor = if (isDarkMode) Color(0xFF8A5A00) else Color(0xFFFF9800)  // Darker orange in dark mode
    // Text color changes based on dark mode - darker text for better readability
    val textColor = if (isDarkMode) Color.Black else Color.White
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        )
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Distinctive symbol - wrench/hammer icon
            ToolsIcon(size = 20.dp)
            
            Text(
                text = stringResource(Res.string.level_editor),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
