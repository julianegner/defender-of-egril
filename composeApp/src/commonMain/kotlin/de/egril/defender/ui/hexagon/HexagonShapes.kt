package de.egril.defender.ui.hexagon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.TowerTypeIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import kotlin.math.sqrt

/**
 * Shape for creating hexagonal UI elements.
 * Creates a pointy-top hexagon with flat sides on left and right.
 */
class HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // For pointy-top hexagon:
            // The hexagon has flat sides on left and right
            // Points at top and bottom
            val radius = minOf(width, height) / 2f

            // Calculate the 6 vertices of a pointy-top hexagon
            // Starting from the top and going clockwise
            val sqrt3 = sqrt(3.0).toFloat()

            // Top point
            moveTo(centerX, centerY - radius)
            // Top-right
            lineTo(centerX + radius * sqrt3 / 2f, centerY - radius / 2f)
            // Bottom-right
            lineTo(centerX + radius * sqrt3 / 2f, centerY + radius / 2f)
            // Bottom point
            lineTo(centerX, centerY + radius)
            // Bottom-left
            lineTo(centerX - radius * sqrt3 / 2f, centerY + radius / 2f)
            // Top-left
            lineTo(centerX - radius * sqrt3 / 2f, centerY - radius / 2f)
            // Close the path
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Displays a tower type icon on a blue hexagon background.
 * This composable is used in multiple places (RulesScreen, LevelEditor, etc.)
 * to maintain visual consistency.
 *
 * @param defenderType The type of tower to display
 * @param size The size of the hexagon (default 32.dp)
 * @param modifier Additional modifiers to apply
 */
@Composable
fun TowerIconOnHexagon(
    defenderType: DefenderType,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(HexagonShape())
            .background(Color(0xFF2196F3)), // Blue background for towers
        contentAlignment = Alignment.Center
    ) {
        TowerTypeIcon(defenderType = defenderType)
    }
}

/**
 * Displays an enemy type icon on a colored hexagon background.
 * The hexagon color matches the color used on the game map for enemies.
 * This composable is used in multiple places (RulesScreen, etc.)
 * to maintain visual consistency.
 *
 * @param attackerType The type of enemy to display
 * @param size The size of the hexagon (default 32.dp)
 * @param modifier Additional modifiers to apply
 */
@Composable
fun EnemyIconOnHexagon(
    attackerType: AttackerType,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(HexagonShape())
            .background(Color(0xFFF44336)), // Red background for enemies
        contentAlignment = Alignment.Center
    ) {
        EnemyTypeIcon(attackerType = attackerType)
    }
}
