package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.TowerTypeIcon
import de.egril.defender.ui.hexagon.HexagonShape
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.loading_level
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Draws a simple barge (flat-bottomed boat) icon using Canvas.
 */
@Composable
fun BargeIcon(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 40.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val hullColor = Color(0xFF8B6914)
        val structureColor = Color(0xFFD4A820)
        val waterColor = Color(0xFF1E88E5)
        val strokeColor = Color(0xFF5D4037)

        // Water line
        drawRect(
            color = waterColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.72f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.28f)
        )

        // Hull (trapezoid: wider at top, narrower at bottom)
        val hullPath = Path().apply {
            moveTo(w * 0.08f, h * 0.45f)      // top-left
            lineTo(w * 0.92f, h * 0.45f)      // top-right
            lineTo(w * 0.82f, h * 0.72f)      // bottom-right
            lineTo(w * 0.18f, h * 0.72f)      // bottom-left
            close()
        }
        drawPath(hullPath, hullColor)
        drawPath(hullPath, strokeColor, style = Stroke(width = w * 0.04f))

        // Deck structure (small rectangle on top of hull)
        drawRect(
            color = structureColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.25f),
            size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.20f)
        )
        drawRect(
            color = strokeColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.25f),
            size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.20f),
            style = Stroke(width = w * 0.04f)
        )

        // Mast
        drawLine(
            color = strokeColor,
            start = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.05f),
            end = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.25f),
            strokeWidth = w * 0.05f
        )
    }
}

/**
 * Loading screen shown while a level's map image is being loaded.
 *
 * Displays a spinner in the centre, a "Loading level" label below it,
 * and 8 icons arranged in a circle:  3 enemy unit icons, 3 tower icons,
 * and 2 barge icons.
 */
@Composable
fun LevelLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LoadingCircleWithIcons()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.loading_level),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/**
 * A spinner surrounded by 8 icons (3 enemy, 3 tower, 2 barge) in a circle.
 */
@Composable
private fun LoadingCircleWithIcons() {
    val circleRadius = 90.dp
    val iconSize = 44.dp
    val totalSize = circleRadius * 2 + iconSize

    // Items arranged clockwise starting from the top:
    // index 0 = top (0°), each step is 45°
    // Order: Goblin (enemy), Spike (tower), Barge, Ork (enemy), Bow (tower), Barge, Red Witch (enemy), Wizard (tower)
    val items = listOf<@Composable () -> Unit>(
        { EnemyHexIcon(AttackerType.GOBLIN, iconSize) },
        { TowerHexIcon(DefenderType.SPIKE_TOWER, iconSize) },
        { BargeHexIcon(iconSize) },
        { EnemyHexIcon(AttackerType.ORK, iconSize) },
        { TowerHexIcon(DefenderType.BOW_TOWER, iconSize) },
        { BargeHexIcon(iconSize) },
        { EnemyHexIcon(AttackerType.RED_WITCH, iconSize) },
        { TowerHexIcon(DefenderType.WIZARD_TOWER, iconSize) },
    )

    Box(
        modifier = Modifier.size(totalSize),
        contentAlignment = Alignment.Center
    ) {
        val count = items.size
        items.forEachIndexed { index, itemContent ->
            val angleDeg = -90.0 + index * (360.0 / count)   // start at top
            val angleRad = angleDeg * PI / 180.0
            val offsetX = (cos(angleRad) * circleRadius.value).dp
            val offsetY = (sin(angleRad) * circleRadius.value).dp
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .offset(x = offsetX, y = offsetY)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                itemContent()
            }
        }

        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 5.dp
        )
    }
}

@Composable
private fun EnemyHexIcon(attackerType: AttackerType, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(HexagonShape())
            .background(Color(0xFFF44336)),
        contentAlignment = Alignment.Center
    ) {
        EnemyTypeIcon(attackerType = attackerType)
    }
}

@Composable
private fun TowerHexIcon(defenderType: DefenderType, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(HexagonShape())
            .background(Color(0xFF2196F3)),
        contentAlignment = Alignment.Center
    ) {
        TowerTypeIcon(defenderType = defenderType)
    }
}

@Composable
private fun BargeHexIcon(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(HexagonShape())
            .background(Color(0xFF00796B)),
        contentAlignment = Alignment.Center
    ) {
        BargeIcon(size = size * 0.75f)
    }
}
