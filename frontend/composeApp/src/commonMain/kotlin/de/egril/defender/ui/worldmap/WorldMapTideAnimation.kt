package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.world_map_tide_band1
import defender_of_egril.composeapp.generated.resources.world_map_tide_band2
import defender_of_egril.composeapp.generated.resources.world_map_tide_band3
import org.jetbrains.compose.resources.painterResource

// Total animation cycle: expand (12s) → hold (6s) → shrink (12s) → hold (6s) = 36s
private const val TIDE_CYCLE_MILLIS = 36000

/**
 * Draws an animated tide effect over the world map ocean.
 *
 * The effect simulates a tide cycle: the lighter blue coastal water slowly expands
 * into the adjacent darker blue ocean, holds briefly, then slowly retreats back.
 * This creates a gentle breathing/pulsing effect along all coastlines.
 *
 * The overlay consists of 3 pre-computed band images, each representing a distance
 * slice from the coastline:
 * - Band 1: closest to coast (appears first during expansion, disappears last)
 * - Band 2: middle distance
 * - Band 3: farthest from coast (appears last, disappears first)
 *
 * Each band's alpha is animated with staggered timing so the overall visual effect
 * is the lighter blue spatially growing outward from the coast and then receding.
 *
 * Only active when [AppSettings.enableAnimations] is true.
 */
@Composable
fun WorldMapTideAnimation(modifier: Modifier = Modifier) {
    if (!AppSettings.enableAnimations.value) return

    val infiniteTransition = rememberInfiniteTransition(label = "tide")

    // Band 1 (closest to coast): appears early, disappears late
    val band1Alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = TIDE_CYCLE_MILLIS
                0f at 0 using EaseInOut
                1f at 4000 using EaseInOut        // fully visible by 4s
                1f at 18000 using EaseInOut       // hold through expansion + hold
                0f at 30000 using EaseInOut       // disappear by 30s
                0f at TIDE_CYCLE_MILLIS
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "tideBand1",
    )

    // Band 2 (middle): appears slightly later, disappears slightly earlier
    val band2Alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = TIDE_CYCLE_MILLIS
                0f at 0 using EaseInOut
                0f at 2000 using EaseInOut         // starts after band 1
                1f at 8000 using EaseInOut         // fully visible by 8s
                1f at 18000 using EaseInOut        // hold
                0f at 26000 using EaseInOut        // disappear by 26s
                0f at TIDE_CYCLE_MILLIS
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "tideBand2",
    )

    // Band 3 (farthest from coast): appears last, disappears first
    val band3Alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = TIDE_CYCLE_MILLIS
                0f at 0 using EaseInOut
                0f at 4000 using EaseInOut         // starts after band 2
                1f at 12000 using EaseInOut        // fully visible by 12s
                1f at 18000 using EaseInOut        // hold
                0f at 22000 using EaseInOut        // disappear by 22s
                0f at TIDE_CYCLE_MILLIS
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "tideBand3",
    )

    Box(modifier = modifier) {
        // Band 1: closest to coast - visible most of the cycle
        Image(
            painter = painterResource(Res.drawable.world_map_tide_band1),
            contentDescription = null,
            modifier = Modifier.matchParentSize().graphicsLayer(alpha = band1Alpha),
            contentScale = ContentScale.Fit,
        )
        // Band 2: middle distance
        Image(
            painter = painterResource(Res.drawable.world_map_tide_band2),
            contentDescription = null,
            modifier = Modifier.matchParentSize().graphicsLayer(alpha = band2Alpha),
            contentScale = ContentScale.Fit,
        )
        // Band 3: farthest from coast - visible for shortest time
        Image(
            painter = painterResource(Res.drawable.world_map_tide_band3),
            contentDescription = null,
            modifier = Modifier.matchParentSize().graphicsLayer(alpha = band3Alpha),
            contentScale = ContentScale.Fit,
        )
    }
}
