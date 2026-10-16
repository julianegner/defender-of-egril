package de.egril.defender.model

/**
 * How a [SpawnGroup] decides whether to repeat (loop back to its first entry) or advance to the
 * next entry of the enclosing sequence once one iteration has been fully played.
 *
 * See issue #694 (Spawn Loops): spawn groups replace linear turn-by-turn lists with dynamic loop
 * blocks that are evaluated at the end of every cycle. Groups can be nested and mixed with plain
 * turns (see [SpawnSequenceEntry]).
 */
enum class SpawnRepeatMode {
    /** Loop the group a fixed number of times ([SpawnGroup.repeatCount] total iterations). */
    COUNT,

    /**
     * Loop while a game-state [SpawnCondition] (currently only [SpawnCondition.UNIT_ALIVE]) holds.
     * Evaluated at the end of each iteration: `true` repeats, `false` advances to the next entry.
     */
    CONDITION,

    /**
     * Loop until an event ends the group with a STOP_SPAWN_LOOP action (required by the editor). The
     * group never completes on its own; once stopped, the sequence continues after it.
     */
    INFINITE,
}

/**
 * Game-state condition that can be checked at the end of a [SpawnGroup] iteration.
 */
enum class SpawnCondition {
    /** True while the unit bound to [SpawnGroup.targetUnitId] is still alive on the battlefield. */
    UNIT_ALIVE,
}

/**
 * A single spawn instruction inside a [SpawnGroupTurn].
 *
 * Reuses the existing game conventions: [attackerType], [level], and an optional fixed
 * [spawnPoint]. [count] expands to that many identical planned spawns.
 *
 * @param unitId             Optional logical identifier. When set, the first actually-spawned
 *                           unit of this entry is bound to this id so [SpawnCondition.UNIT_ALIVE]
 *                           conditions can reference it (e.g. a boss). The binding persists across
 *                           turns and save/load.
 * @param firstIterationOnly When true, this entry only spawns during the first iteration
 *                           (repetition 0) of its directly enclosing group. Used so a boss spawned
 *                           once can drive a CONDITION loop of add-spawns without being respawned
 *                           after it dies (which would loop forever).
 */
data class SpawnGroupSpawn(
    val attackerType: AttackerType,
    val count: Int = 1,
    val level: Int = 1,
    val spawnPoint: Position? = null,
    val unitId: String? = null,
    val firstIterationOnly: Boolean = false,
) {
    /** Villains exist only once per level, so they always spawn only in the first iteration. */
    val spawnsInFirstIterationOnly: Boolean get() = firstIterationOnly || attackerType.isRealVillain
}

/**
 * An element of a spawn sequence: either a plain [SpawnGroupTurn] or a (possibly nested) loop
 * [SpawnGroup].
 *
 * A sequence is played in order. Consecutive [SpawnGroupTurn]s form a *segment*: their
 * [SpawnGroupTurn.turnOffset]s are 1-based and relative to the start of that segment, and the
 * segment occupies `max(turnOffset)` turns (gaps are empty turns). A [SpawnGroup] entry starts on
 * the turn right after the preceding segment/group ended and runs all of its iterations before the
 * sequence continues.
 */
sealed interface SpawnSequenceEntry

/**
 * One turn of a spawn sequence segment. [turnOffset] is 1-based and relative to the start of the
 * segment (the run of consecutive turns this entry belongs to); an entry with an empty [spawns]
 * list is an explicit blank turn.
 */
data class SpawnGroupTurn(
    val turnOffset: Int,
    val spawns: List<SpawnGroupSpawn> = emptyList(),
) : SpawnSequenceEntry

/**
 * An ordered loop block that replaces the flat spawn plan when present on a level. Its [entries]
 * may mix plain turns and nested loop groups.
 *
 * @param groupId     Stable identifier for the group (diagnostics / editor references).
 * @param repeatMode  How the group loops (see [SpawnRepeatMode]).
 * @param repeatCount Total iterations for [SpawnRepeatMode.COUNT] (minimum 1).
 * @param condition   Condition for [SpawnRepeatMode.CONDITION].
 * @param targetUnitId Logical unit id checked by [SpawnCondition.UNIT_ALIVE].
 * @param entries     The turns and nested groups of one iteration, played in order.
 */
data class SpawnGroup(
    val groupId: String,
    val repeatMode: SpawnRepeatMode,
    val repeatCount: Int = 1,
    val condition: SpawnCondition? = null,
    val targetUnitId: String? = null,
    val entries: List<SpawnSequenceEntry> = emptyList(),
) : SpawnSequenceEntry {
    /** Plain turns directly inside this group (not inside nested groups). */
    val directTurns: List<SpawnGroupTurn> get() = entries.filterIsInstance<SpawnGroupTurn>()

    /** Nested groups directly inside this group. */
    val directGroups: List<SpawnGroup> get() = entries.filterIsInstance<SpawnGroup>()
}

/** Id of the implicit, non-repeating root group that wraps a level's top-level spawn sequence. */
const val SPAWN_SEQUENCE_ROOT_ID = "__root__"

/** Wraps a level's top-level spawn sequence in the implicit root group used by the runner. */
fun spawnSequenceRoot(entries: List<SpawnSequenceEntry>): SpawnGroup =
    SpawnGroup(SPAWN_SEQUENCE_ROOT_ID, SpawnRepeatMode.COUNT, repeatCount = 1, entries = entries)

/** All spawn entries anywhere in this sequence (static view, ignoring repetitions). */
fun List<SpawnSequenceEntry>.allSpawnEntries(): List<SpawnGroupSpawn> =
    flatMap { entry ->
        when (entry) {
            is SpawnGroupTurn -> entry.spawns
            is SpawnGroup -> entry.entries.allSpawnEntries()
        }
    }

/** All groups anywhere in this sequence, in pre-order (parents before their children). */
fun List<SpawnSequenceEntry>.allSpawnGroups(): List<SpawnGroup> =
    flatMap { entry ->
        when (entry) {
            is SpawnGroupTurn -> emptyList()
            is SpawnGroup -> listOf(entry) + entry.entries.allSpawnGroups()
        }
    }

/** Replaces every spawn entry (recursively) by the result of [transform], keeping the structure. */
fun List<SpawnSequenceEntry>.flatMapSpawnEntries(transform: (SpawnGroupSpawn) -> List<SpawnGroupSpawn>): List<SpawnSequenceEntry> =
    map { entry ->
        when (entry) {
            is SpawnGroupTurn -> entry.copy(spawns = entry.spawns.flatMap(transform))
            is SpawnGroup -> entry.copy(entries = entry.entries.flatMapSpawnEntries(transform))
        }
    }

/** Maps every spawn entry (recursively), keeping the structure. */
fun List<SpawnSequenceEntry>.mapSpawnEntries(transform: (SpawnGroupSpawn) -> SpawnGroupSpawn): List<SpawnSequenceEntry> =
    flatMapSpawnEntries { listOf(transform(it)) }

/**
 * Number of enemies per type spawned by a spawn sequence.
 *
 * @param endless True if the sequence contains a loop whose number of iterations is not fixed
 *                ([SpawnRepeatMode.CONDITION] or [SpawnRepeatMode.INFINITE]); such loops are counted
 *                with a single iteration.
 */
data class SpawnSequenceEnemyCounts(
    val counts: Map<AttackerType, Int>,
    val endless: Boolean,
)

/**
 * Counts the enemies of a spawn sequence. Fixed-count loops are multiplied out (enemies marked
 * [SpawnGroupSpawn.firstIterationOnly] only count once per run of their loop); loops with a dynamic
 * end are counted with one iteration and mark the result as [SpawnSequenceEnemyCounts.endless].
 */
fun List<SpawnSequenceEntry>.enemyCounts(): SpawnSequenceEnemyCounts {
    var endless = false

    fun add(
        target: MutableMap<AttackerType, Long>,
        source: Map<AttackerType, Long>,
        factor: Long,
    ) {
        source.forEach { (type, count) -> target[type] = ((target[type] ?: 0L) + count * factor).coerceAtMost(Int.MAX_VALUE.toLong()) }
    }

    fun count(
        sequence: List<SpawnSequenceEntry>,
        repetition: Int,
    ): Map<AttackerType, Long> {
        val result = mutableMapOf<AttackerType, Long>()
        sequence.forEach { entry ->
            when (entry) {
                is SpawnGroupTurn ->
                    entry.spawns
                        .filter { !it.spawnsInFirstIterationOnly || repetition == 0 }
                        .forEach { spawn -> add(result, mapOf(spawn.attackerType to spawn.count.coerceAtLeast(0).toLong()), 1) }
                is SpawnGroup -> {
                    val iterations =
                        if (entry.repeatMode == SpawnRepeatMode.COUNT) {
                            entry.repeatCount.coerceAtLeast(1)
                        } else {
                            endless = true
                            1
                        }
                    add(result, count(entry.entries, 0), 1)
                    if (iterations > 1) add(result, count(entry.entries, 1), (iterations - 1).toLong())
                }
            }
        }
        return result
    }

    val counts = count(this, 0).filterValues { it > 0 }.mapValues { it.value.toInt() }
    return SpawnSequenceEnemyCounts(counts, endless)
}

/**
 * Position inside one group of the active nesting chain.
 *
 * @param entryIndex         Index of the current entry in the group's [SpawnGroup.entries]. For a
 *                           segment of turns this is the index of the segment's first turn.
 * @param repetition         Current 0-based iteration of the group.
 * @param iterationStartTurn Absolute turn at which the current iteration started; used to detect
 *                           iterations that consume no turns (those never repeat).
 */
data class SpawnGroupFrame(
    val entryIndex: Int = 0,
    val repetition: Int = 0,
    val iterationStartTurn: Int = 1,
)

/**
 * Serializable runtime cursor tracking progress through a level's (nested) spawn sequence.
 *
 * @param frames            Nesting chain from the implicit root group ([frames]`[0]`) down to the
 *                          innermost active group.
 * @param segmentStartTurn  Absolute turn at which the current entry (segment or group) starts.
 * @param finished          True once the whole sequence has been played (never true while an
 *                          [SpawnRepeatMode.INFINITE] group is active).
 * @param lastProcessedTurn Highest absolute turn already resolved, guarding against double spawns.
 */
data class SpawnGroupCursor(
    val frames: List<SpawnGroupFrame> = listOf(SpawnGroupFrame()),
    val segmentStartTurn: Int = 1,
    val finished: Boolean = false,
    val lastProcessedTurn: Int = 0,
) {
    companion object {
        /**
         * Converts the cursor of the former flat group list format (one group index plus the
         * iteration state of that group) into the nested format. Old groups only contained turns,
         * so the active group is always positioned at its single segment.
         */
        fun fromLegacy(
            topLevelEntryCount: Int,
            groupIndex: Int,
            repetition: Int,
            iterationStartTurn: Int,
            finished: Boolean,
            lastProcessedTurn: Int,
        ): SpawnGroupCursor =
            if (finished || groupIndex >= topLevelEntryCount) {
                SpawnGroupCursor(
                    frames = listOf(SpawnGroupFrame(entryIndex = topLevelEntryCount)),
                    segmentStartTurn = iterationStartTurn,
                    finished = true,
                    lastProcessedTurn = lastProcessedTurn,
                )
            } else {
                SpawnGroupCursor(
                    frames =
                        listOf(
                            SpawnGroupFrame(entryIndex = groupIndex),
                            SpawnGroupFrame(entryIndex = 0, repetition = repetition, iterationStartTurn = iterationStartTurn),
                        ),
                    segmentStartTurn = iterationStartTurn,
                    lastProcessedTurn = lastProcessedTurn,
                )
            }
    }
}

/** Thrown internally when a loop decision cannot be determined statically (forecasting). */
private class UndecidableLoopException : RuntimeException()

/**
 * Pure execution logic for nested spawn sequences. All functions are side-effect free: the caller
 * owns the [SpawnGroupCursor] and supplies the loop decision for completed iterations.
 */
object SpawnSequenceRunner {
    /** Safety bound for forecasting; schedules longer than this are treated as unbounded. */
    private const val MAX_FORECAST_TURNS = 100_000

    /**
     * Advances [cursorIn] so that it points at the segment containing [turn] (or is finished),
     * deciding every iteration that completed before [turn] via [shouldLoop]. [shouldLoop] receives
     * the completed group and its 0-based repetition. Iterations that consumed no turns never repeat.
     * Groups in [stoppedGroupIds] (ended by an event) are skipped when reached and never repeat.
     */
    fun resolve(
        root: SpawnGroup,
        cursorIn: SpawnGroupCursor,
        turn: Int,
        stoppedGroupIds: Set<String> = emptySet(),
        shouldLoop: (SpawnGroup, Int) -> Boolean,
    ): SpawnGroupCursor {
        var cursor = cursorIn
        while (!cursor.finished) {
            val frames = cursor.frames
            if (frames.isEmpty()) return cursor.copy(finished = true)
            val depth = frames.lastIndex
            val group = groupAt(root, frames, depth) ?: return cursor.copy(finished = true)
            val frame = frames[depth]

            if (frame.entryIndex >= group.entries.size) {
                // The current iteration of this group is complete.
                if (depth == 0) return cursor.copy(finished = true)
                val consumedTurns = cursor.segmentStartTurn > frame.iterationStartTurn
                cursor =
                    if (consumedTurns && group.groupId !in stoppedGroupIds && shouldLoop(group, frame.repetition)) {
                        cursor.copy(
                            frames =
                                frames.dropLast(1) +
                                    SpawnGroupFrame(0, frame.repetition + 1, cursor.segmentStartTurn),
                        )
                    } else {
                        val parent = frames[depth - 1]
                        cursor.copy(frames = frames.dropLast(2) + parent.copy(entryIndex = parent.entryIndex + 1))
                    }
                continue
            }

            when (val entry = group.entries[frame.entryIndex]) {
                is SpawnGroupTurn -> {
                    val segment = segmentAt(group, frame.entryIndex)
                    val length = segment.maxOfOrNull { it.turnOffset } ?: 0
                    if (turn < cursor.segmentStartTurn + length) return cursor
                    cursor =
                        cursor.copy(
                            frames = frames.dropLast(1) + frame.copy(entryIndex = frame.entryIndex + segment.size),
                            segmentStartTurn = cursor.segmentStartTurn + length,
                        )
                }
                is SpawnGroup -> {
                    cursor =
                        if (entry.groupId in stoppedGroupIds) {
                            cursor.copy(frames = frames.dropLast(1) + frame.copy(entryIndex = frame.entryIndex + 1))
                        } else {
                            cursor.copy(frames = frames + SpawnGroupFrame(0, 0, cursor.segmentStartTurn))
                        }
                }
            }
        }
        return cursor
    }

    /**
     * Ends the active group [groupId] (and everything nested in it) immediately: the cursor moves to
     * the entry following that group, which starts at [nextTurn]. Returns [cursor] unchanged when the
     * group is not active.
     */
    fun stop(
        root: SpawnGroup,
        cursor: SpawnGroupCursor,
        groupId: String,
        nextTurn: Int,
    ): SpawnGroupCursor {
        if (cursor.finished) return cursor
        val depth = (1..cursor.frames.lastIndex).firstOrNull { groupAt(root, cursor.frames, it)?.groupId == groupId } ?: return cursor
        val parent = cursor.frames[depth - 1]
        return cursor.copy(
            frames = cursor.frames.take(depth - 1) + parent.copy(entryIndex = parent.entryIndex + 1),
            segmentStartTurn = maxOf(cursor.segmentStartTurn, nextTurn),
        )
    }

    /**
     * Spawns of [turn] for a cursor already [resolve]d for [turn]. Entries flagged
     * [SpawnGroupSpawn.firstIterationOnly] only spawn during the first iteration of their directly
     * enclosing group.
     */
    fun spawnsAt(
        root: SpawnGroup,
        cursor: SpawnGroupCursor,
        turn: Int,
    ): List<PlannedEnemySpawn> {
        if (cursor.finished || cursor.frames.isEmpty()) return emptyList()
        val depth = cursor.frames.lastIndex
        val group = groupAt(root, cursor.frames, depth) ?: return emptyList()
        val frame = cursor.frames[depth]
        if (group.entries.getOrNull(frame.entryIndex) !is SpawnGroupTurn) return emptyList()
        val offset = turn - cursor.segmentStartTurn + 1
        if (offset < 1) return emptyList()
        return segmentAt(group, frame.entryIndex)
            .filter { it.turnOffset == offset }
            .flatMap { it.spawns }
            .filter { !(it.spawnsInFirstIterationOnly && frame.repetition > 0) }
            .flatMap { entry ->
                List(entry.count.coerceAtLeast(1)) {
                    PlannedEnemySpawn(
                        attackerType = entry.attackerType,
                        spawnTurn = turn,
                        level = entry.level,
                        spawnPoint = entry.spawnPoint,
                        unitId = entry.unitId,
                    )
                }
            }
    }

    /**
     * Statically forecasts every spawn after [currentTurn]. Returns `null` when the remaining
     * schedule is unbounded or depends on game state (a CONDITION or INFINITE loop must decide).
     */
    fun forecast(
        root: SpawnGroup,
        cursorIn: SpawnGroupCursor,
        currentTurn: Int,
        stoppedGroupIds: Set<String> = emptySet(),
    ): List<PlannedEnemySpawn>? {
        val result = mutableListOf<PlannedEnemySpawn>()
        var cursor = cursorIn
        var turn = maxOf(currentTurn, cursorIn.lastProcessedTurn) + 1
        val staticDecision: (SpawnGroup, Int) -> Boolean = { group, repetition ->
            when (group.repeatMode) {
                SpawnRepeatMode.COUNT -> repetition < group.repeatCount - 1
                SpawnRepeatMode.CONDITION, SpawnRepeatMode.INFINITE -> throw UndecidableLoopException()
            }
        }
        try {
            repeat(MAX_FORECAST_TURNS) {
                cursor = resolve(root, cursor, turn, stoppedGroupIds, staticDecision)
                if (cursor.finished) return result
                result += spawnsAt(root, cursor, turn)
                turn++
            }
        } catch (_: UndecidableLoopException) {
            return null
        }
        return null
    }

    private fun groupAt(
        root: SpawnGroup,
        frames: List<SpawnGroupFrame>,
        depth: Int,
    ): SpawnGroup? {
        var group = root
        for (level in 0 until depth) {
            group = group.entries.getOrNull(frames[level].entryIndex) as? SpawnGroup ?: return null
        }
        return group
    }

    /** The run of consecutive turns in [group] starting at [startIndex]. */
    private fun segmentAt(
        group: SpawnGroup,
        startIndex: Int,
    ): List<SpawnGroupTurn> =
        group.entries
            .drop(startIndex)
            .takeWhile { it is SpawnGroupTurn }
            .filterIsInstance<SpawnGroupTurn>()
}
