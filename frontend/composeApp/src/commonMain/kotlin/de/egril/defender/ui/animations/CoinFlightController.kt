package de.egril.defender.ui.animations

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/**
 * A single coin sprite in flight from a reward source toward the coin counter.
 *
 * @param id Unique identifier used as a stable Compose key and for removal.
 * @param start Screen (root) coordinates where the coin starts, in pixels.
 * @param target Screen (root) coordinates of the coin counter, in pixels.
 * @param control Quadratic-Bézier control point (root coordinates). Precomputed once at launch so
 *                the arced path isn't recalculated on every animation frame.
 * @param delayMillis Delay before this coin starts moving. Coins in a burst are staggered so they
 *                    trail one another along the same single upper arc, forming a smooth stream of
 *                    coins instead of splitting across separate arcs.
 */
data class CoinFlight(
    val id: Long,
    val start: Offset,
    val target: Offset,
    val control: Offset,
    val delayMillis: Int,
)

/**
 * Global controller that queues "coin fly-to-counter" animations.
 *
 * The reward source (e.g. a defeated enemy tile) calls [launch] with its screen position;
 * the coin counter reports its position via [setTarget]. [CoinFlightOverlay] renders and
 * removes the active [flights].
 *
 * The queueing logic here is intentionally free of Compose UI so it can be unit tested.
 *
 * Not thread-safe by design: it is only ever accessed from the Compose UI thread (header layout
 * callbacks and `LaunchedEffect` coroutines, which run on the main dispatcher), so no locking is
 * needed for [nextId] or the state holders.
 */
object CoinFlightController {
    /**
     * Upper bound on coin sprites animating at once. Keeps heavy wave/action scenarios from
     * spawning an unbounded number of sprites (avoids FPS drops and visual glitches).
     */
    const val MAX_ACTIVE_FLIGHTS = 24

    /** Minimum number of coin sprites spawned for a single (positive) gain event. */
    const val MIN_COINS_PER_EVENT = 3

    /** Maximum number of coin sprites spawned for a single gain event, regardless of amount. */
    const val MAX_COINS_PER_EVENT = 6

    /** One extra coin sprite is added per this many coins, up to [MAX_COINS_PER_EVENT]. */
    const val COINS_PER_SPRITE = 10

    /** How far the arced flight path bows out, as a fraction of the straight-line distance. */
    private const val ARC_FACTOR = 0.25f

    /**
     * Delay added per coin within a single burst (milliseconds). Staggering the coins makes them
     * trail one another along the same single arc, so a burst reads as a stream of coins rather
     * than a couple of overlapping sprites.
     */
    const val COIN_STAGGER_MS = 220

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
        val control = arcControlPoint(source, target)
        for (i in 0 until count) {
            flights.add(
                CoinFlight(
                    id = nextId++,
                    start = source,
                    target = target,
                    control = control,
                    delayMillis = i * COIN_STAGGER_MS,
                ),
            )
        }
        return count
    }

    /**
     * Control point for a coin's quadratic-Bézier flight path: the midpoint of the straight line,
     * pushed perpendicular to it so the coin travels along an arc. The bow always points upward
     * (toward the top of the screen) so every coin in a burst follows the same single upper arc.
     */
    private fun arcControlPoint(
        start: Offset,
        target: Offset,
    ): Offset {
        val mid = Offset((start.x + target.x) / 2f, (start.y + target.y) / 2f)
        val dx = target.x - start.x
        val dy = target.y - start.y
        val length = hypot(dx, dy)
        if (length <= 0f) return mid
        // Always bow upward (negative y) so the arc is the smooth upper curve, never a lower one.
        val perpendicular =
            Offset(-dy / length, dx / length).let { if (it.y > 0f) Offset(-it.x, -it.y) else it }
        return mid + perpendicular * (length * ARC_FACTOR)
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
