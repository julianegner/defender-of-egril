package de.egril.defender.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import de.egril.defender.ui.settings.AppSettings
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Duration of a single coin's flight from the reward source to the coin counter. Deliberately slow
 * so the coins are clearly visible travelling from the defeated enemy to the counter.
 */
private const val COIN_FLIGHT_DURATION_MS = 2800

/**
 * Golden fill of a flying coin. Matches the gold used by the coin-gain "bubbling" Lottie
 * (`files/animations/coin_gain.json`, RGB 1.0/0.843/0.0) so the flying coins look identical to the
 * coins that rise up from the defeated enemy.
 */
private val COIN_FILL_COLOR = Color(0xFFFFD700)

/** Fraction of the flight (near the end) over which the coin fades out as it reaches the counter. */
private const val COIN_FADE_START = 0.85f

/**
 * Z-index for the overlay. It is a sibling of the gameplay `Surface` (which hosts the header and
 * map) within the top-level `BoxWithConstraints`, so any positive value stacks it above that
 * sibling. The map's internal tile overlays (enemy death, coin gain, tower impacts) use z-indices
 * up to ~19f, so 50f keeps flying coins above all of them for their whole flight.
 */
private const val COIN_FLIGHT_OVERLAY_Z_INDEX = 50f

/**
 * Full-screen overlay that renders "coin fly-to-counter" animations queued in [CoinFlightController].
 *
 * Each coin sprite eases along an arced path from its reward source to the coin counter and then
 * removes itself, crediting its share of the reward to the counter as it lands (see
 * [CoinFlightController.onArrived]). The whole overlay is a no-op when the animations setting is OFF,
 * so disabling animations fully suppresses the effect (any reserved coins are then credited by
 * [CoinFlightController.clear], and new rewards update the total via the existing flow).
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

    Box(modifier = modifier.fillMaxSize().zIndex(COIN_FLIGHT_OVERLAY_Z_INDEX)) {
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
        if (flight.delayMillis > 0) {
            delay(flight.delayMillis.toLong())
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = COIN_FLIGHT_DURATION_MS, easing = FastOutSlowInEasing),
        )
        CoinFlightController.onArrived(flight)
    }

    val t = progress.value
    val position = quadraticBezierPoint(flight.start, flight.control, flight.target, t)
    val spriteAlpha =
        if (t > COIN_FADE_START) {
            (1f - (t - COIN_FADE_START) / (1f - COIN_FADE_START)).coerceIn(0f, 1f)
        } else {
            1f
        }
    val halfPx = flight.sizePx / 2f
    val sizeDp = with(density) { flight.sizePx.toDp() }

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
        Coin(size = sizeDp)
    }
}

/** A small golden coin: a plain filled gold disc, matching the coins in the coin-gain animation. */
@Composable
private fun Coin(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = COIN_FILL_COLOR, radius = radius, center = center)
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
