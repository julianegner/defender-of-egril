package de.egril.defender.game

import de.egril.defender.editor.TileType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.EventLoop
import de.egril.defender.model.EventLoopStep
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import de.egril.defender.model.TileZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for event loops (repeated, nested and endless step sequences) in [EventScriptSystem]. */
class EventLoopTest {
    private val tidePosition = Position(2, 0)

    private fun createLevel(events: List<LevelEvent>): Level =
        Level(
            id = 1,
            name = "Loop Level",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            pathCells = (0..5).map { Position(it, 0) }.toSet(),
            buildAreas = setOf(Position(2, 2)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            events = LevelEvents(events),
            tileZones = listOf(TileZone(id = "tide", tiles = mapOf(tidePosition to TileType.RIVER))),
        )

    private fun coins(amount: Int) = EventAction(type = EventActionType.GIVE_COINS, amount = amount)

    private fun startEvent(
        loop: EventLoop,
        id: String = "loop",
    ) = LevelEvent(id = id, condition = EventCondition(type = EventConditionType.TURN_START), loop = loop)

    /** Run player-turn starts for [turns] (turn numbers), returning the system for further checks. */
    private fun runTurns(
        state: GameState,
        turns: IntRange,
        system: EventScriptSystem = EventScriptSystem(state),
        afterTurn: (Int) -> Unit = {},
    ): EventScriptSystem {
        for (turn in turns) {
            state.turnNumber.value = turn
            system.evaluate(EventTrigger.PLAYER_TURN_START)
            afterTurn(turn)
        }
        return system
    }

    @Test
    fun endlessLoopTogglesTideEveryTwoTurns() {
        val loop =
            EventLoop(
                steps = listOf(EventLoopStep(waitTurns = 2, actions = listOf(EventAction(type = EventActionType.TOGGLE_TILE_ZONE, zoneId = "tide")))),
                repeatCount = 0,
            )
        val state = GameState(createLevel(listOf(startEvent(loop))))
        val flooded = mutableMapOf<Int, Boolean>()

        runTurns(state, 1..9) { turn -> flooded[turn] = state.level.isRiverTile(tidePosition) }

        assertEquals(
            mapOf(1 to false, 2 to false, 3 to true, 4 to true, 5 to false, 6 to false, 7 to true, 8 to true, 9 to false),
            flooded,
        )
        assertEquals(1, state.activeEventLoops.size, "Endless loop keeps running")
    }

    @Test
    fun loopWithRepeatCountStopsAfterRepetitions() {
        val loop = EventLoop(steps = listOf(EventLoopStep(waitTurns = 1, actions = listOf(coins(10)))), repeatCount = 2)
        val state = GameState(createLevel(listOf(startEvent(loop))))

        runTurns(state, 1..6)

        assertEquals(120, state.coins.value)
        assertTrue(state.activeEventLoops.isEmpty())
    }

    @Test
    fun zeroWaitStepRunsImmediately() {
        val loop =
            EventLoop(
                steps = listOf(EventLoopStep(waitTurns = 0, actions = listOf(coins(5))), EventLoopStep(waitTurns = 3)),
                repeatCount = 1,
            )
        val state = GameState(createLevel(listOf(startEvent(loop))))

        runTurns(state, 1..1)

        assertEquals(105, state.coins.value, "A step without waiting runs when the event fires")
    }

    @Test
    fun nestedLoopsRunCompletely() {
        val inner = EventLoop(steps = listOf(EventLoopStep(waitTurns = 1, actions = listOf(coins(100)))), repeatCount = 2)
        val outer = EventLoop(steps = listOf(EventLoopStep(waitTurns = 1, actions = listOf(coins(1)), nestedLoop = inner)), repeatCount = 2)
        val state = GameState(createLevel(listOf(startEvent(outer))))
        val coinsPerTurn = mutableMapOf<Int, Int>()

        runTurns(state, 1..8) { turn -> coinsPerTurn[turn] = state.coins.value }

        assertEquals(
            mapOf(1 to 100, 2 to 101, 3 to 201, 4 to 301, 5 to 302, 6 to 402, 7 to 502, 8 to 502),
            coinsPerTurn,
        )
        assertTrue(state.activeEventLoops.isEmpty())
    }

    @Test
    fun stopEventLoopActionStopsRunningLoop() {
        val loop = EventLoop(steps = listOf(EventLoopStep(waitTurns = 1, actions = listOf(coins(10)))), repeatCount = 0)
        val stopper =
            LevelEvent(
                id = "stop",
                condition = EventCondition(type = EventConditionType.TURN_START, fromTurn = 4),
                actions = listOf(EventAction(type = EventActionType.STOP_EVENT_LOOP, targetEventId = "loop")),
            )
        val state = GameState(createLevel(listOf(startEvent(loop), stopper)))

        runTurns(state, 1..8)

        // Turns 2, 3 and 4 pay out (the loop advances before events are evaluated), then it stops.
        assertEquals(130, state.coins.value)
        assertTrue(state.activeEventLoops.isEmpty())
    }

    @Test
    fun loopWithoutWaitingIsNotStarted() {
        val loop = EventLoop(steps = listOf(EventLoopStep(waitTurns = 0, actions = listOf(coins(10)))), repeatCount = 0)
        val state = GameState(createLevel(listOf(startEvent(loop))))

        runTurns(state, 1..3)

        assertFalse(loop.isValid())
        assertEquals(100, state.coins.value)
        assertTrue(state.activeEventLoops.isEmpty())
    }

    @Test
    fun endlessNestedLoopIsValidButMarksLaterStepsUnreachable() {
        val endlessInner = EventLoop(steps = listOf(EventLoopStep(waitTurns = 1)), repeatCount = 0)
        val outer =
            EventLoop(
                steps = listOf(EventLoopStep(waitTurns = 1, nestedLoop = endlessInner), EventLoopStep(waitTurns = 1)),
                repeatCount = 0,
            )
        assertTrue(outer.isValid())
        assertTrue(outer.hasUnreachableSteps())
        assertFalse(EventLoop(steps = listOf(EventLoopStep(waitTurns = 1, nestedLoop = endlessInner))).hasUnreachableSteps())
    }

    @Test
    fun runningLoopDoesNotRestartWhenRepeatableEventFiresAgain() {
        val loop = EventLoop(steps = listOf(EventLoopStep(waitTurns = 2, actions = listOf(coins(10)))), repeatCount = 0)
        val event = startEvent(loop).copy(repeatable = true)
        val state = GameState(createLevel(listOf(event)))

        runTurns(state, 1..5)

        // Pays on turns 3 and 5; re-firing the event every turn must not reset the countdown.
        assertEquals(120, state.coins.value)
        assertEquals(1, state.activeEventLoops.size)
    }
}
