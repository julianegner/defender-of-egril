package de.egril.defender.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.TileType
import de.egril.defender.model.RiverFlow
import de.egril.defender.ui.common.SelectableText
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
        SelectableText(tileType.name, color = textColor)
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
        TileType.NO_PLAY -> if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFF404040)  // Much darker gray
        TileType.SPAWN_POINT -> if (isDarkMode) Color(0xFF8B0000) else Color(0xFFFF0000)  // Dark red
        TileType.TARGET -> if (isDarkMode) Color(0xFF00008B) else Color(0xFF0000FF)  // Dark blue
        TileType.RIVER -> if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFF4682B4)  // Steel blue for water
    }
}

/**
 * Displays flow indicators for river tiles based on flow direction and speed
 * For hexagonal grids, arrows point to the middle of each of the 6 hex sides
 * Arrows are white/light gray for visibility on blue river tiles
 */
@Composable
fun RiverFlowIndicator(
    flowDirection: RiverFlow,
    flowSpeed: Int,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    // Use white/light gray color for arrows to stand out on blue river background
    val arrowTint = Color.White
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (flowDirection) {
            RiverFlow.NONE -> {
                // No flow - display a simple circle or dot
                SelectableText("•", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }
            RiverFlow.MAELSTROM -> {
                // Maelstrom - display whirlpool symbol (using hole icon as approximation)
                HoleIcon(size = size)
            }
            RiverFlow.EAST -> {
                // East: 0° (right) - use right arrow
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RightArrowIcon(size = size, tint = arrowTint)
                    if (flowSpeed == 2) {
                        RightArrowIcon(size = size, tint = arrowTint)
                    }
                }
            }
            RiverFlow.SOUTH_EAST -> {
                // SouthEast: 60° - rotate right arrow clockwise
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RightArrowIcon(
                        modifier = Modifier.graphicsLayer { rotationZ = 60f },
                        size = size,
                        tint = arrowTint
                    )
                    if (flowSpeed == 2) {
                        RightArrowIcon(
                            modifier = Modifier.graphicsLayer { rotationZ = 60f },
                            size = size,
                            tint = arrowTint
                        )
                    }
                }
            }
            RiverFlow.SOUTH_WEST -> {
                // SouthWest: 120° - rotate right arrow clockwise
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RightArrowIcon(
                        modifier = Modifier.graphicsLayer { rotationZ = 120f },
                        size = size,
                        tint = arrowTint
                    )
                    if (flowSpeed == 2) {
                        RightArrowIcon(
                            modifier = Modifier.graphicsLayer { rotationZ = 120f },
                            size = size,
                            tint = arrowTint
                        )
                    }
                }
            }
            RiverFlow.WEST -> {
                // West: 180° - rotate right arrow
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RightArrowIcon(
                        modifier = Modifier.graphicsLayer { rotationZ = 180f },
                        size = size,
                        tint = arrowTint
                    )
                    if (flowSpeed == 2) {
                        RightArrowIcon(
                            modifier = Modifier.graphicsLayer { rotationZ = 180f },
                            size = size,
                            tint = arrowTint
                        )
                    }
                }
            }
            RiverFlow.NORTH_WEST -> {
                // NorthWest: 240° - rotate right arrow clockwise (or -120° counterclockwise)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RightArrowIcon(
                        modifier = Modifier.graphicsLayer { rotationZ = 240f },
                        size = size,
                        tint = arrowTint
                    )
                    if (flowSpeed == 2) {
                        RightArrowIcon(
                            modifier = Modifier.graphicsLayer { rotationZ = 240f },
                            size = size,
                            tint = arrowTint
                        )
                    }
                }
            }
            RiverFlow.NORTH_EAST -> {
                // NorthEast: 300° (or -60°) - rotate right arrow counterclockwise
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RightArrowIcon(
                        modifier = Modifier.graphicsLayer { rotationZ = -60f },
                        size = size,
                        tint = arrowTint
                    )
                    if (flowSpeed == 2) {
                        RightArrowIcon(
                            modifier = Modifier.graphicsLayer { rotationZ = -60f },
                            size = size,
                            tint = arrowTint
                        )
                    }
                }
            }
        }
    }
}
