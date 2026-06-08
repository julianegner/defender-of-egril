package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.world_map_tide_band01
import defender_of_egril.composeapp.generated.resources.world_map_tide_band02
import defender_of_egril.composeapp.generated.resources.world_map_tide_band03
import defender_of_egril.composeapp.generated.resources.world_map_tide_band04
import defender_of_egril.composeapp.generated.resources.world_map_tide_band05
import defender_of_egril.composeapp.generated.resources.world_map_tide_band06
import defender_of_egril.composeapp.generated.resources.world_map_tide_band07
import defender_of_egril.composeapp.generated.resources.world_map_tide_band08
import defender_of_egril.composeapp.generated.resources.world_map_tide_band09
import defender_of_egril.composeapp.generated.resources.world_map_tide_band10
import defender_of_egril.composeapp.generated.resources.world_map_tide_band11
import defender_of_egril.composeapp.generated.resources.world_map_tide_band12
import org.jetbrains.compose.resources.painterResource

private const val TIDE_CYCLE_MILLIS = 36000
private const val TIDE_SLICE_COUNT = 12

private val tideSliceResources = listOf(
    Res.drawable.world_map_tide_band01,
    Res.drawable.world_map_tide_band02,
    Res.drawable.world_map_tide_band03,
    Res.drawable.world_map_tide_band04,
    Res.drawable.world_map_tide_band05,
    Res.drawable.world_map_tide_band06,
    Res.drawable.world_map_tide_band07,
    Res.drawable.world_map_tide_band08,
    Res.drawable.world_map_tide_band09,
    Res.drawable.world_map_tide_band10,
    Res.drawable.world_map_tide_band11,
    Res.drawable.world_map_tide_band12,
)

@Composable
fun WorldMapTideAnimation(modifier: Modifier = Modifier) {
    if (!AppSettings.enableAnimations.value) return

    val infiniteTransition = rememberInfiniteTransition(label = "tide")
    val visibleSliceProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = TIDE_CYCLE_MILLIS
                0f at 0 using EaseInOut
                TIDE_SLICE_COUNT.toFloat() at 12000 using EaseInOut
                TIDE_SLICE_COUNT.toFloat() at 18000 using EaseInOut
                0f at 30000 using EaseInOut
                0f at TIDE_CYCLE_MILLIS
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "visibleSliceProgress",
    )

    val visibleSliceCount = visibleSliceProgress.toInt().coerceIn(0, TIDE_SLICE_COUNT)

    Box(modifier = modifier) {
        tideSliceResources.take(visibleSliceCount).forEach { resource ->
            Image(
                painter = painterResource(resource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
