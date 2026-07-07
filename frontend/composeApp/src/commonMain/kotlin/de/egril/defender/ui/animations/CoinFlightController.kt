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
 * @param sizePx Diameter of the coin sprite in pixels. Derived from the source tile's on-screen
 *               size so the flying coins match the coin-gain "bubbling" coins at any zoom level.
 * @param creditAmount Number of coins credited to the counter when THIS sprite reaches it. A burst's
 *                     total reward is split across its coins so the counter ticks up as the coins
 *                     arrive, keeping the visible total in step with the coins landing on it.
 * @param burstId Identifies the burst this coin belongs to (all coins launched together share one).
 */
data class CoinFlight(
    val id: Long,
    val start: Offset,
    val target: Offset,
    val control: Offset,
    val delayMillis: Int,
    val sizePx: Float,
    val creditAmount: Int = 0,
    val burstId: Long = -1L,
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

    /**
     * Fallback coin diameter (pixels) used when a caller doesn't provide a tile-derived size
     * (e.g. unit tests). Real gameplay launches pass a size measured from the source tile.
     */
    const val DEFAULT_COIN_SIZE_PX = 24f

    private var nextId = 0L
    private var nextBurstId = 0L

    /**
     * Per-burst callbacks that credit the counter as the burst's coins arrive. Keyed by burst id and
     * removed once the burst's final coin has landed (or on [clear]).
     */
    private val burstCallbacks = mutableMapOf<Long, (Int) -> Unit>()

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
     * [coinSizePx] is the coin sprite diameter in pixels, derived from the source tile's on-screen
     * size so flying coins match the coin-gain "bubbling" coins at the current zoom.
     *
     * [creditAmount] is the reward (in coins) to add to the counter as this burst's coins arrive; it
     * is split across the launched coins and delivered via [onArrival] when each coin reaches the
     * counter, so the visible total updates in step with the coins landing on it. It defaults to
     * [amount] (the number driving the sprite count).
     *
     * Returns the number of coin sprites actually launched. This is 0 when there is no target
     * yet, the [amount] is not positive, or capacity ([MAX_ACTIVE_FLIGHTS]) is exhausted. Extra
     * rewards arriving while the queue is full are simply dropped so quick successions never
     * glitch or overwhelm the screen. When 0 is returned the caller should credit [creditAmount]
     * itself, since no coin will arrive to trigger [onArrival].
     */
    fun launch(
        source: Offset,
        amount: Int,
        coinSizePx: Float = DEFAULT_COIN_SIZE_PX,
        creditAmount: Int = amount,
        onArrival: (Int) -> Unit = {},
    ): Int {
        val target = targetPosition.value ?: return 0
        val desired = coinCountForAmount(amount)
        if (desired == 0) return 0
        val capacity = (MAX_ACTIVE_FLIGHTS - flights.size).coerceAtLeast(0)
        val count = minOf(desired, capacity)
        if (count == 0) return 0
        val control = arcControlPoint(source, target)
        val burstId = nextBurstId++
        burstCallbacks[burstId] = onArrival
        val credits = distributeCredit(creditAmount, count)
        for (i in 0 until count) {
            flights.add(
                CoinFlight(
                    id = nextId++,
                    start = source,
                    target = target,
                    control = control,
                    delayMillis = i * COIN_STAGGER_MS,
                    sizePx = coinSizePx,
                    creditAmount = credits[i],
                    burstId = burstId,
                ),
            )
        }
        return count
    }

    /**
     * Split [total] coins across [count] sprites as evenly as possible, giving the leftover to the
     * first coins so the whole reward is delivered exactly once across the burst.
     */
    fun distributeCredit(
        total: Int,
        count: Int,
    ): IntArray {
        if (count <= 0) return IntArray(0)
        val base = total / count
        val remainder = total - base * count
        return IntArray(count) { i -> base + if (i < remainder) 1 else 0 }
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

    /**
     * Called by the overlay when a coin sprite reaches the counter. Credits this coin's share of its
     * burst's reward (so the counter updates as coins land) and removes the sprite. When the burst's
     * final coin has arrived, its callback is discarded.
     */
    fun onArrived(flight: CoinFlight) {
        flights.removeAll { it.id == flight.id }
        val callback = burstCallbacks[flight.burstId] ?: return
        callback(flight.creditAmount)
        if (flights.none { it.burstId == flight.burstId }) {
            burstCallbacks.remove(flight.burstId)
        }
    }

    /** Remove a completed flight by [id] without crediting (used for tests and manual cleanup). */
    fun remove(id: Long) {
        flights.removeAll { it.id == id }
    }

    /**
     * Clear all active flights (e.g. when leaving the gameplay screen or disabling animations).
     *
     * Any coins still in flight are credited first via their burst callbacks so a reward reserved at
     * launch time is never lost when its coins are dropped before landing.
     */
    fun clear() {
        flights.forEach { flight ->
            burstCallbacks[flight.burstId]?.invoke(flight.creditAmount)
        }
        burstCallbacks.clear()
        flights.clear()
    }
}
