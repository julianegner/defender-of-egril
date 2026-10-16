package de.egril.defender.model

/**
 * Runtime position inside one level of a running [EventLoop].
 *
 * @param stepIndex      Index of the current step within its loop.
 * @param turnsRemaining Player turns left before the current step executes.
 * @param iterationsDone Completed passes through the loop.
 * @param stepExecuted   True once the current step's actions ran (its nested loop may still be running).
 */
data class EventLoopFrame(
    val stepIndex: Int = 0,
    val turnsRemaining: Int = 0,
    val iterationsDone: Int = 0,
    val stepExecuted: Boolean = false,
)

/**
 * A running loop of the event [eventId]. [frames] is a stack with one frame per nesting depth:
 * the first frame belongs to the event's top-level loop, the last frame to the innermost running loop.
 */
data class ActiveEventLoop(
    val eventId: String,
    val frames: List<EventLoopFrame>,
)
