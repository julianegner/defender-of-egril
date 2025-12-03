package de.egril.defender.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.TileType
import de.egril.defender.model.RiverFlow
import de.egril.defender.ui.icon.*

/**
 * Button for selecting a tile type
 */
@Composable
fun TileTypeButton(
    tileType: TileType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    val textColor = if (isDarkMode) Color.White else Color.Black
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) getTileColor(tileType).copy(alpha = 0.8f) else getTileColor(tileType).copy(alpha = 0.4f)
        )
    ) {
        Text(tileType.name, color = textColor)
    }
}

/**
 * Get the color for a specific tile type
 */
fun getTileColor(tileType: TileType): Color {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    
    return when (tileType) {
        TileType.PATH -> if (isDarkMode) Color(0xFF4A2F1A) else Color(0xFF8B4513)  // Darker brown
        TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF456C2E) else Color(0xFF90EE90)  // Much darker green
        TileType.ISLAND -> if (isDarkMode) Color(0xFF1B4D0E) else Color(0xFF228B22)  // Much darker forest green
        TileType.NO_PLAY -> if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFF404040)  // Much darker gray
        TileType.SPAWN_POINT -> if (isDarkMode) Color(0xFF8B0000) else Color(0xFFFF0000)  // Dark red
        TileType.TARGET -> if (isDarkMode) Color(0xFF00008B) else Color(0xFF0000FF)  // Dark blue
        TileType.RIVER -> if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFF4682B4)  // Steel blue for water
    }
}

/**
 * Displays flow indicators for river tiles based on flow direction and speed
 */
@Composable
fun RiverFlowIndicator(
    flowDirection: RiverFlow,
    flowSpeed: Int,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (flowDirection) {
            RiverFlow.NONE -> {
                // No flow - display a simple circle or dot
                Text("•", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }
            RiverFlow.MAELSTROM -> {
                // Maelstrom - display whirlpool symbol (using hole icon as approximation)
                HoleIcon(size = size)
            }
            RiverFlow.NORTH_EAST -> {
                // Display arrow(s) pointing NE (diagonal up-right)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    UpArrowIcon(size = size)
                    RightArrowIcon(size = size)
                    if (flowSpeed == 2) {
                        RightArrowIcon(size = size)
                    }
                }
            }
            RiverFlow.EAST -> {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RightArrowIcon(size = size)
                    if (flowSpeed == 2) {
                        RightArrowIcon(size = size)
                    }
                }
            }
            RiverFlow.SOUTH_EAST -> {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    DownArrowIcon(size = size)
                    RightArrowIcon(size = size)
                    if (flowSpeed == 2) {
                        RightArrowIcon(size = size)
                    }
                }
            }
            RiverFlow.SOUTH_WEST -> {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    DownArrowIcon(size = size)
                    LeftArrowIcon(size = size)
                    if (flowSpeed == 2) {
                        LeftArrowIcon(size = size)
                    }
                }
            }
            RiverFlow.WEST -> {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    LeftArrowIcon(size = size)
                    if (flowSpeed == 2) {
                        LeftArrowIcon(size = size)
                    }
                }
            }
            RiverFlow.NORTH_WEST -> {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    UpArrowIcon(size = size)
                    LeftArrowIcon(size = size)
                    if (flowSpeed == 2) {
                        LeftArrowIcon(size = size)
                    }
                }
            }
        }
    }
}
