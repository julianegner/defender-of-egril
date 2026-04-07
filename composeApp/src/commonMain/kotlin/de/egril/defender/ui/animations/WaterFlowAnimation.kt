package de.egril.defender.ui.animations
import io.github.alexzhirkevich.compottie.Compottie

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import de.egril.defender.model.RiverFlow

/**
 * Water flow animation for river tiles.
 * Displays flowing blue wave lines animated in the direction of the river's flow.
 *
 * The base Lottie animation flows from right to left (WEST direction).
 * For other directions, the animation is rotated accordingly.
 * For eastward directions (EAST, NORTH_EAST, SOUTH_EAST) the animation is mirrored
 * (rotated 180°, 120°, 240° respectively) so the water flows in the correct direction.
 *
 * NONE and MAELSTROM directions are not animated (no flow to display).
 *
 * @param flowDirection the direction of the river flow
 * @param flowSpeed the river flow speed (1 = normal, 2 = double speed animation)
 */
@Composable
fun WaterFlowAnimation(flowDirection: RiverFlow, flowSpeed: Int = 1, modifier: Modifier = Modifier) {
    // Rotation angle for the animation based on flow direction.
    // Base animation flows WEST (right to left, 0° rotation).
    // Positive rotation = clockwise in screen coordinates (y-down).
    val rotationDeg: Float = when (flowDirection) {
        RiverFlow.WEST       ->   0f
        RiverFlow.NORTH_WEST ->  60f   // NW = 240° screen angle; (240-180) = 60°
        RiverFlow.SOUTH_WEST -> 300f   // SW = 120° screen angle; (120-180) = -60° = 300°
        RiverFlow.EAST       -> 180f   // mirror: flow left-to-right
        RiverFlow.NORTH_EAST -> 120f   // mirror of NW equivalent: (300-180) = 120°
        RiverFlow.SOUTH_EAST -> 240f   // mirror of SW equivalent: (60-180) = -120° = 240°
        RiverFlow.NONE, RiverFlow.MAELSTROM -> return  // no animation
    }

    LottieAnimation(
        animationType = AnimationType.WATER_FLOW,
        modifier = modifier.graphicsLayer { rotationZ = rotationDeg },
        iterations = Compottie.IterateForever,
        speed = if (flowSpeed >= 2) 2f else 1f
    )
}
