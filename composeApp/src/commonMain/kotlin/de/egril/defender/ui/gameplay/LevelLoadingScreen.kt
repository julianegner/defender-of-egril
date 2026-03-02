package de.egril.defender.ui.gameplay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.DrawRaft
import de.egril.defender.ui.TowerTypeIcon
import de.egril.defender.ui.hexagon.HexagonShape
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.loading_level
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

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
 * The ring of icons rotates continuously, and the CircularProgressIndicator spins at the centre.
 */
@Composable
private fun LoadingCircleWithIcons() {
    val circleRadius = 90.dp
    val iconSize = 44.dp
    val totalSize = circleRadius * 2 + iconSize

    val infiniteTransition = rememberInfiniteTransition(label = "loadingRotation")
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iconRingRotation"
    )

    // Items arranged clockwise starting from the top:
    // index 0 = top (0°), each step is 45°
    // Order: Goblin (enemy), Spike (tower), Spear barge, Ork (enemy), Bow (tower), Wizard barge, Red Witch (enemy), Wizard (tower)
    val items = listOf<@Composable () -> Unit>(
        { EnemyHexIcon(AttackerType.GOBLIN, iconSize) },
        { TowerHexIcon(DefenderType.SPIKE_TOWER, iconSize) },
        { BargeHexIcon(DefenderType.SPEAR_TOWER, iconSize) },
        { EnemyHexIcon(AttackerType.ORK, iconSize) },
        { TowerHexIcon(DefenderType.BOW_TOWER, iconSize) },
        { BargeHexIcon(DefenderType.WIZARD_TOWER, iconSize) },
        { EnemyHexIcon(AttackerType.RED_WITCH, iconSize) },
        { TowerHexIcon(DefenderType.WIZARD_TOWER, iconSize) },
    )

    Box(
        modifier = Modifier.size(totalSize),
        contentAlignment = Alignment.Center
    ) {
        // Rotating ring of icons
        Box(
            modifier = Modifier
                .size(totalSize)
                .graphicsLayer { rotationZ = rotationDeg },
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
                        .align(Alignment.Center)
                        // Counter-rotate so icons stay upright as ring rotates
                        .graphicsLayer { rotationZ = -rotationDeg },
                    contentAlignment = Alignment.Center
                ) {
                    itemContent()
                }
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
private fun BargeHexIcon(defenderType: DefenderType, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(HexagonShape())
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD4A55A),  // light brown (wood/raft) at top
                        Color(0xFF1565C0),  // blue (water) at bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        DrawRaft(defenderType = defenderType)
    }
}
