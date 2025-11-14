package com.defenderofegril.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.defenderofegril.editor.TileType
import com.defenderofegril.ui.icon.PushpinIcon

/**
 * Button for selecting a tile type
 */
@Composable
fun TileTypeButton(
    tileType: TileType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDarkMode = com.defenderofegril.ui.settings.AppSettings.isDarkMode.value
    val textColor = if (isDarkMode) Color.White else Color.Black
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) getTileColor(tileType).copy(alpha = 0.8f) else getTileColor(tileType).copy(alpha = 0.4f)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (tileType == TileType.WAYPOINT) {
                PushpinIcon(size = 14.dp)
            }
            Text(tileType.name, color = textColor)
        }
    }
}

/**
 * Get the color for a specific tile type
 */
fun getTileColor(tileType: TileType): Color {
    val isDarkMode = com.defenderofegril.ui.settings.AppSettings.isDarkMode.value
    
    return when (tileType) {
        TileType.PATH -> if (isDarkMode) Color(0xFF4A2F1A) else Color(0xFF8B4513)  // Darker brown
        TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF456C2E) else Color(0xFF90EE90)  // Much darker green
        TileType.ISLAND -> if (isDarkMode) Color(0xFF1B4D0E) else Color(0xFF228B22)  // Much darker forest green
        TileType.NO_PLAY -> if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFF404040)  // Much darker gray
        TileType.SPAWN_POINT -> if (isDarkMode) Color(0xFF8B0000) else Color(0xFFFF0000)  // Dark red
        TileType.TARGET -> if (isDarkMode) Color(0xFF00008B) else Color(0xFF0000FF)  // Dark blue
        TileType.WAYPOINT -> if (isDarkMode) Color(0xFF4A2F1A) else Color(0xFF8B4513)  // Darker brown (same as PATH)
    }
}
