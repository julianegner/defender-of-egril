package de.egril.defender.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.roundToInt

/** Duration of a single coin's flight from the reward source to the coin counter. */
private const val COIN_FLIGHT_DURATION_MS = 650

/** Size of an individual flying coin sprite. */
private val COIN_FLIGHT_SIZE: Dp = 22.dp

/** Fraction of the flight (near the end) over which the coin fades out as it reaches the counter. */
private const val COIN_FADE_START = 0.85f

/**
 * Full-screen overlay that renders "coin fly-to-counter" animations queued in [CoinFlightController].
 *
 * Each coin sprite eases along an arced path from its reward source to the coin counter and then
 * removes itself. The whole overlay is a no-op when the animations setting is OFF, so disabling
 * animations fully suppresses the effect (the coin total still updates via the existing flow).
 *
 * This must be placed above both the header and the map so coins can visibly travel between them.
 */
@Composable
fun CoinFlightOverlay(modifier: Modifier = Modifier) {
    // Always clear any leftover flights when this overlay leaves the composition (e.g. level exit).
    DisposableEffect(Unit) {
        onDispose { CoinFlightController.clear() }
    }

    if (!AppSettings.enableAnimations.value) {
        // Drop any queued flights so they don't appear if animations are re-enabled mid-flight.
        if (CoinFlightController.flights.isNotEmpty()) {
            CoinFlightController.clear()
        }
        return
    }

    Box(modifier = modifier.fillMaxSize().zIndex(50f)) {
        // Iterate a stable snapshot copy so removals during iteration don't disturb rendering.
        CoinFlightController.flights.toList().forEach { flight ->
            key(flight.id) {
                CoinFlightSprite(flight)
            }
        }
    }
}

@Composable
private fun CoinFlightSprite(flight: CoinFlight) {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }

    // The sprite is keyed by flight.id upstream, so this effect runs exactly once per sprite.
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = COIN_FLIGHT_DURATION_MS, easing = FastOutSlowInEasing),
        )
        CoinFlightController.remove(flight.id)
    }

    val t = progress.value
    val position = quadraticBezierPoint(flight.start, flight.control, flight.target, t)
    val spriteAlpha =
        if (t > COIN_FADE_START) {
            (1f - (t - COIN_FADE_START) / (1f - COIN_FADE_START)).coerceIn(0f, 1f)
        } else {
            1f
        }
    val halfPx = with(density) { (COIN_FLIGHT_SIZE / 2).toPx() }

    Box(
        modifier =
            Modifier
                .offset {
                    IntOffset(
                        x = (position.x - halfPx).roundToInt(),
                        y = (position.y - halfPx).roundToInt(),
                    )
                }.alpha(spriteAlpha),
    ) {
        MoneyIcon(size = COIN_FLIGHT_SIZE)
    }
}

private fun quadraticBezierPoint(
    start: Offset,
    control: Offset,
    end: Offset,
    t: Float,
): Offset {
    val oneMinusT = 1f - t
    return start * (oneMinusT * oneMinusT) +
        control * (2f * oneMinusT * t) +
        end * (t * t)
}
