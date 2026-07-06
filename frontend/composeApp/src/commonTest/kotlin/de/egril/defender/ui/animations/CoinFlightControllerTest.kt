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
    fun burstsAlternateArcDirectionAndHaveUniqueIds() {
        CoinFlightController.setTarget(Offset(300f, 25f))
        CoinFlightController.launch(Offset(0f, 0f), amount = 100000) // maximal burst

        val ids = CoinFlightController.flights.map { it.id }.toSet()
        assertEquals(CoinFlightController.flights.size, ids.size, "Every flight has a unique id")

        val signs = CoinFlightController.flights.map { it.curveSign }
        assertTrue(signs.contains(1f) && signs.contains(-1f), "Arc directions alternate within a burst")
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
}
