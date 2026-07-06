package de.egril.defender.ui.animations

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset

/**
 * A single coin sprite in flight from a reward source toward the coin counter.
 *
 * @param id Unique identifier used as a stable Compose key and for removal.
 * @param start Screen (root) coordinates where the coin starts, in pixels.
 * @param target Screen (root) coordinates of the coin counter, in pixels.
 * @param curveSign Direction (+1 / -1) the arced flight path bends, alternated per coin
 *                  so a burst fans out instead of overlapping on a single line.
 */
data class CoinFlight(
    val id: Long,
    val start: Offset,
    val target: Offset,
    val curveSign: Float,
)

/**
 * Global controller that queues "coin fly-to-counter" animations.
 *
 * The reward source (e.g. a defeated enemy tile) calls [launch] with its screen position;
 * the coin counter reports its position via [setTarget]. [CoinFlightOverlay] renders and
 * removes the active [flights].
 *
 * The queueing logic here is intentionally free of Compose UI so it can be unit tested.
 */
object CoinFlightController {
    /**
     * Upper bound on coin sprites animating at once. Keeps heavy wave/action scenarios from
     * spawning an unbounded number of sprites (avoids FPS drops and visual glitches).
     */
    const val MAX_ACTIVE_FLIGHTS = 24

    /** Minimum number of coin sprites spawned for a single (positive) gain event. */
    const val MIN_COINS_PER_EVENT = 1

    /** Maximum number of coin sprites spawned for a single gain event, regardless of amount. */
    const val MAX_COINS_PER_EVENT = 6

    /** One extra coin sprite is added per this many coins, up to [MAX_COINS_PER_EVENT]. */
    const val COINS_PER_SPRITE = 10

    private var nextId = 0L

    /** Coin counter center in root coordinates (pixels); null until the header reports it. */
    val targetPosition = mutableStateOf<Offset?>(null)

    /** Currently animating coin sprites. */
    val flights: SnapshotStateList<CoinFlight> = mutableStateListOf()

    /** Report the coin counter center (root coordinates, pixels). */
    fun setTarget(offset: Offset) {
        targetPosition.value = offset
    }

    /**
     * Number of coin sprites to spawn for a coin [amount]. Scales gently with the amount so
     * bigger rewards feel more rewarding, but stays within [MIN_COINS_PER_EVENT]..[MAX_COINS_PER_EVENT].
     */
    fun coinCountForAmount(amount: Int): Int {
        if (amount <= 0) return 0
        val scaled = 1 + (amount - 1) / COINS_PER_SPRITE
        return scaled.coerceIn(MIN_COINS_PER_EVENT, MAX_COINS_PER_EVENT)
    }

    /**
     * Enqueue a coin-flight burst from [source] toward the current target (coin counter).
     *
     * Returns the number of coin sprites actually launched. This is 0 when there is no target
     * yet, the [amount] is not positive, or capacity ([MAX_ACTIVE_FLIGHTS]) is exhausted. Extra
     * rewards arriving while the queue is full are simply dropped so quick successions never
     * glitch or overwhelm the screen.
     */
    fun launch(
        source: Offset,
        amount: Int,
    ): Int {
        val target = targetPosition.value ?: return 0
        val desired = coinCountForAmount(amount)
        if (desired == 0) return 0
        val capacity = (MAX_ACTIVE_FLIGHTS - flights.size).coerceAtLeast(0)
        val count = minOf(desired, capacity)
        for (i in 0 until count) {
            val sign = if (i % 2 == 0) 1f else -1f
            flights.add(
                CoinFlight(
                    id = nextId++,
                    start = source,
                    target = target,
                    curveSign = sign,
                ),
            )
        }
        return count
    }

    /** Remove a completed flight by [id]. */
    fun remove(id: Long) {
        flights.removeAll { it.id == id }
    }

    /** Clear all active flights (e.g. when leaving the gameplay screen). */
    fun clear() {
        flights.clear()
    }
}
