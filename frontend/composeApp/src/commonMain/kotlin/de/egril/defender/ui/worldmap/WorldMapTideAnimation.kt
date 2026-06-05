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
import defender_of_egril.composeapp.generated.resources.world_map_tide_band1
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
 * Uses a single pre-computed band image representing the innermost distance slice
 * from the coastline. The band's alpha is animated so the lighter blue appears to
 * grow slightly outward from the coast and then recede.
 *
 * Only active when [AppSettings.enableAnimations] is true.
 */
@Composable
fun WorldMapTideAnimation(modifier: Modifier = Modifier) {
    if (!AppSettings.enableAnimations.value) return

    val infiniteTransition = rememberInfiniteTransition(label = "tide")

    val tideAlpha by infiniteTransition.animateFloat(
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
        label = "tideAlpha",
    )

    Image(
        painter = painterResource(Res.drawable.world_map_tide_band1),
        contentDescription = null,
        modifier = modifier.graphicsLayer(alpha = tideAlpha),
        contentScale = ContentScale.Fit,
    )
}
