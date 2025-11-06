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
            Text(tileType.name)
        }
    }
}

/**
 * Get the color for a specific tile type
 */
fun getTileColor(tileType: TileType): Color {
    return when (tileType) {
        TileType.PATH -> Color(0xFF8B4513)        // Brown
        TileType.BUILD_AREA -> Color(0xFF90EE90)  // Light green
        TileType.ISLAND -> Color(0xFF228B22)      // Forest green
        TileType.NO_PLAY -> Color(0xFF404040)     // Dark gray
        TileType.SPAWN_POINT -> Color(0xFFFF0000) // Red
        TileType.TARGET -> Color(0xFF0000FF)      // Blue
        TileType.WAYPOINT -> Color(0xFF8B4513)    // Brown (same as PATH)
    }
}
