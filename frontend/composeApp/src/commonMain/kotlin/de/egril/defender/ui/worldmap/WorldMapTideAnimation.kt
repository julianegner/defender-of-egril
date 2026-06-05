package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.world_map_tide_overlay
import org.jetbrains.compose.resources.painterResource

// Total animation cycle: expand (3s) → hold (1.5s) → shrink (3s) → hold (1.5s) = 9s
private const val TIDE_CYCLE_MILLIS = 9000

/**
 * Draws an animated tide effect over the world map ocean.
 *
 * The effect simulates a tide cycle: the lighter blue coastal water slowly expands
 * into the adjacent darker blue ocean, holds briefly, then slowly retreats back.
 * This creates a gentle breathing/pulsing effect along all coastlines.
 *
 * The overlay image is pre-computed with an alpha gradient that is strongest near
 * the coastline and fades toward the outer expansion limit (50% of the darker
 * blue band width). The composable animates the layer alpha of the entire overlay
 * image using a graphicsLayer, producing the visual appearance of an expanding
 * and contracting water edge.
 *
 * Only active when [AppSettings.enableAnimations] is true.
 */
@Composable
fun WorldMapTideAnimation(modifier: Modifier = Modifier) {
    if (!AppSettings.enableAnimations.value) return

    val infiniteTransition = rememberInfiniteTransition(label = "tide")

    // Animated phase: 0→1→hold→0→hold using keyframes for the breathing rhythm
    val tideAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = TIDE_CYCLE_MILLIS
                0f at 0 using EaseInOut           // start: no overlay
                1f at 3000 using EaseInOut        // fully expanded at 3s
                1f at 4500 using EaseInOut        // hold at max for 1.5s
                0f at 7500 using EaseInOut        // shrink back by 7.5s
                0f at TIDE_CYCLE_MILLIS           // hold at min for 1.5s
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "tideAlpha",
    )

    Image(
        painter = painterResource(Res.drawable.world_map_tide_overlay),
        contentDescription = null,
        modifier = modifier.graphicsLayer(alpha = tideAlpha),
        contentScale = ContentScale.Fit,
    )
}
