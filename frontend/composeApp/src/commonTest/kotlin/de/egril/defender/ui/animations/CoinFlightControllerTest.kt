package de.egril.defender.ui.animations

import androidx.compose.ui.geometry.Offset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the coin fly-to-counter queueing logic in [CoinFlightController].
 *
 * These validate event handling and queueing behaviour without a running composition:
 * amount-to-sprite mapping, requiring a target, capacity capping, and clearing.
 */
class CoinFlightControllerTest {
    @BeforeTest
    fun reset() {
        CoinFlightController.clear()
        CoinFlightController.targetPosition.value = null
    }

    @AfterTest
    fun cleanup() {
        CoinFlightController.clear()
        CoinFlightController.targetPosition.value = null
    }

    @Test
    fun coinCountForAmountClampsToConfiguredBounds() {
        assertEquals(0, CoinFlightController.coinCountForAmount(0), "Non-positive amount yields no coins")
        assertEquals(0, CoinFlightController.coinCountForAmount(-5), "Negative amount yields no coins")
        assertEquals(
            CoinFlightController.MIN_COINS_PER_EVENT,
            CoinFlightController.coinCountForAmount(1),
            "Smallest positive amount yields the minimum number of coins",
        )
        assertEquals(
            CoinFlightController.MAX_COINS_PER_EVENT,
            CoinFlightController.coinCountForAmount(100000),
            "Very large amount is capped at the maximum number of coins",
        )
    }

    @Test
    fun everyGainLaunchesAtLeastThreeCoins() {
        assertTrue(
            CoinFlightController.MIN_COINS_PER_EVENT >= 3,
            "A coin gain should fly at least three coins to the counter",
        )
        assertTrue(
            CoinFlightController.coinCountForAmount(1) >= 3,
            "Even the smallest reward flies at least three coins",
        )
    }

    @Test
    fun launchDoesNothingWithoutTarget() {
        val launched = CoinFlightController.launch(Offset(10f, 10f), amount = 50)
        assertEquals(0, launched, "No coins should launch until the counter target is known")
        assertTrue(CoinFlightController.flights.isEmpty(), "No flights should be queued without a target")
    }

    @Test
    fun launchDoesNothingForNonPositiveAmount() {
        CoinFlightController.setTarget(Offset(200f, 20f))
        assertEquals(0, CoinFlightController.launch(Offset(10f, 10f), amount = 0))
        assertEquals(0, CoinFlightController.launch(Offset(10f, 10f), amount = -3))
        assertTrue(CoinFlightController.flights.isEmpty())
    }

    @Test
    fun launchQueuesFlightsFromSourceToTarget() {
        val source = Offset(30f, 40f)
        val target = Offset(300f, 25f)
        CoinFlightController.setTarget(target)

        val count = CoinFlightController.launch(source, amount = 5)

        assertEquals(CoinFlightController.coinCountForAmount(5), count)
        assertEquals(count, CoinFlightController.flights.size)
        CoinFlightController.flights.forEach { flight ->
            assertEquals(source, flight.start, "Flight starts at the reward source")
            assertEquals(target, flight.target, "Flight ends at the coin counter")
        }
    }

    @Test
    fun launchAppliesProvidedCoinSize() {
        CoinFlightController.setTarget(Offset(300f, 25f))
        val coinSize = 9.5f
        CoinFlightController.launch(Offset(30f, 40f), amount = 5, coinSizePx = coinSize)
        assertTrue(CoinFlightController.flights.isNotEmpty())
        CoinFlightController.flights.forEach { flight ->
            assertEquals(coinSize, flight.sizePx, "Each flight should use the provided coin size")
        }
    }

    @Test
    fun launchUsesDefaultCoinSizeWhenUnspecified() {
        CoinFlightController.setTarget(Offset(300f, 25f))
        CoinFlightController.launch(Offset(30f, 40f), amount = 5)
        CoinFlightController.flights.forEach { flight ->
            assertEquals(
                CoinFlightController.DEFAULT_COIN_SIZE_PX,
                flight.sizePx,
                "Flight falls back to the default coin size",
            )
        }
    }

    @Test
    fun burstsShareOneArcStaggeredWithUniqueIds() {
        CoinFlightController.setTarget(Offset(300f, 25f))
        CoinFlightController.launch(Offset(0f, 0f), amount = 100000) // maximal burst

        val ids = CoinFlightController.flights.map { it.id }.toSet()
        assertEquals(CoinFlightController.flights.size, ids.size, "Every flight has a unique id")

        // All coins share the same single arc (identical control point) so they follow one curve.
        val controls = CoinFlightController.flights.map { it.control }.toSet()
        assertEquals(1, controls.size, "All coins in a burst follow the same single arc")

        // Coins are staggered so they trail one another along that arc.
        val delays = CoinFlightController.flights.map { it.delayMillis }
        assertEquals(delays.sorted(), delays, "Coins are staggered in launch order")
        assertTrue(delays.toSet().size > 1, "A burst staggers its coins along the arc")
    }

    @Test
    fun launchNeverExceedsMaxActiveFlights() {
        CoinFlightController.setTarget(Offset(300f, 25f))
        // Many quick successive rewards must not overflow the active-flight cap.
        repeat(100) {
            CoinFlightController.launch(Offset(0f, 0f), amount = 100000)
        }
        assertEquals(
            CoinFlightController.MAX_ACTIVE_FLIGHTS,
            CoinFlightController.flights.size,
            "Active flights are capped to avoid glitches under heavy load",
        )

        // Once capacity is exhausted, further launches add nothing.
        val extra = CoinFlightController.launch(Offset(0f, 0f), amount = 100000)
        assertEquals(0, extra, "No new flights are added when capacity is exhausted")
    }

    @Test
    fun removeAndClearManageActiveFlights() {
        CoinFlightController.setTarget(Offset(300f, 25f))
        CoinFlightController.launch(Offset(0f, 0f), amount = 5)
        val firstId = CoinFlightController.flights.first().id
        val remaining = CoinFlightController.flights.size - 1

        CoinFlightController.remove(firstId)
        assertEquals(remaining, CoinFlightController.flights.size)
        assertTrue(CoinFlightController.flights.none { it.id == firstId })

        CoinFlightController.clear()
        assertTrue(CoinFlightController.flights.isEmpty(), "clear() removes all active flights")
    }

    @Test
    fun distributeCreditSplitsAmountExactlyAcrossCoins() {
        assertTrue(CoinFlightController.distributeCredit(10, 0).isEmpty(), "No coins means no shares")

        val even = CoinFlightController.distributeCredit(12, 3)
        assertEquals(12, even.sum(), "Shares always sum to the full reward")
        assertTrue(even.all { it == 4 }, "An evenly divisible reward splits equally")

        val uneven = CoinFlightController.distributeCredit(10, 3)
        assertEquals(10, uneven.sum(), "Shares always sum to the full reward, remainder included")
        assertEquals(listOf(4, 3, 3), uneven.toList(), "Leftover coins go to the first sprites")
    }

    @Test
    fun arrivingCoinsCreditTheirShareUntilTheBurstIsComplete() {
        CoinFlightController.setTarget(Offset(300f, 25f))
        var credited = 0
        val reward = 20
        val count =
            CoinFlightController.launch(
                Offset(0f, 0f),
                amount = reward,
                creditAmount = reward,
            ) { arrived -> credited += arrived }
        assertTrue(count > 0, "Coins should launch when a target is set")

        val flights = CoinFlightController.flights.toList()
        assertEquals(reward, flights.sumOf { it.creditAmount }, "Coins carry the full reward between them")

        // Each coin credits its share exactly once when it lands.
        flights.forEach { flight -> CoinFlightController.onArrived(flight) }
        assertEquals(reward, credited, "The counter is credited the full reward as coins arrive")
        assertTrue(CoinFlightController.flights.isEmpty(), "All coins are removed once they have landed")
    }

    @Test
    fun clearFlushesUncreditedCoinsSoRewardsAreNeverLost() {
        CoinFlightController.setTarget(Offset(300f, 25f))
        var credited = 0
        val reward = 15
        CoinFlightController.launch(
            Offset(0f, 0f),
            amount = reward,
            creditAmount = reward,
        ) { arrived -> credited += arrived }

        // Simulate leaving the screen / disabling animations mid-flight before any coin lands.
        CoinFlightController.clear()
        assertEquals(reward, credited, "Dropping in-flight coins still credits the reserved reward")
        assertTrue(CoinFlightController.flights.isEmpty())
    }
}
