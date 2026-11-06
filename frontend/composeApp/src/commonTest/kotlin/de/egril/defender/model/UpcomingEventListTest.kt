package de.egril.defender.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpcomingEventListTest {
    private val condition = EventCondition(type = EventConditionType.TURN_START, fromTurn = 3)

    @Test
    fun flaggedEventIsListedUntilTriggered() {
        val event = LevelEvent(id = "tide", condition = condition, showInUpcomingList = true)
        assertTrue(event.isListedAsUpcoming(emptyList()))
        assertFalse(event.isListedAsUpcoming(listOf("tide")))
    }

    @Test
    fun unflaggedEventIsNeverListed() {
        val event = LevelEvent(id = "tide", condition = condition, showInUpcomingList = false)
        assertFalse(event.isListedAsUpcoming(emptyList()))
    }

    @Test
    fun repeatableEventStaysListedAfterTriggering() {
        val event = LevelEvent(
            id = "tide",
            condition = condition,
            showInUpcomingList = true,
            repeatable = true,
        )
        assertTrue(event.isListedAsUpcoming(listOf("tide")))
    }

    @Test
    fun turnBasedEventReportsItsStartRound() {
        val event = LevelEvent(id = "tide", condition = condition, showInUpcomingList = true)
        assertEquals(3, event.scheduledTurn(currentTurn = 1))
        assertEquals(3, event.scheduledTurn(currentTurn = 2))
    }

    @Test
    fun eventWithoutStartTurnFiresInFirstRound() {
        val event = LevelEvent(
            id = "start",
            condition = EventCondition(type = EventConditionType.ENEMY_TURN_START),
            showInUpcomingList = true,
        )
        assertEquals(1, event.scheduledTurn(currentTurn = 1))
    }

    @Test
    fun stateDependentEventHasNoPredictableRound() {
        val event = LevelEvent(
            id = "kills",
            condition = EventCondition(type = EventConditionType.ENEMIES_KILLED, threshold = 5),
            showInUpcomingList = true,
        )
        assertNull(event.scheduledTurn(currentTurn = 1))
    }
}
