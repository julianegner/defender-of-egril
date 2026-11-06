package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnCondition
import de.egril.defender.model.SpawnGroup
import de.egril.defender.model.SpawnGroupSpawn
import de.egril.defender.model.SpawnGroupTurn
import de.egril.defender.model.SpawnRepeatMode
import de.egril.defender.model.SpawnSequenceEntry
import de.egril.defender.model.EventActionType
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.allSpawnGroups
import de.egril.defender.model.isRealVillain
import de.egril.defender.model.mapSpawnEntries

enum class SpawnGroupIssue {
    STRUCTURE,
    GROUP_ID,
    TURNS,
    REPEAT_COUNT,
    SPAWN_VALUES,
    UNIT_ID,
    CONDITION_TARGET,
    SPAWN_POINT,
    VILLAIN,

    /** An infinite loop that no event stops with a "Stop spawn loop" action. */
    INFINITE_NOT_STOPPED,
}

/**
 * A loop in the editor's spawn timeline. The editor shows one linear list of turns (1..maxTurn);
 * a loop wraps the inclusive turn range [startTurn]..[endTurn]. Loops nest by containment; when two
 * loops cover the same range, the one listed first is the outer loop.
 */
data class EditorSpawnLoop(
    val id: String,
    val startTurn: Int,
    val endTurn: Int,
    val repeatMode: SpawnRepeatMode = SpawnRepeatMode.COUNT,
    val repeatCount: Int = 2,
    val condition: SpawnCondition? = null,
    val targetUnitId: String? = null,
) {
    val turnCount: Int get() = endTurn - startTurn + 1

    fun contains(turn: Int): Boolean = turn in startTurn..endTurn

    fun containsRange(other: EditorSpawnLoop): Boolean = startTurn <= other.startTurn && other.endTurn <= endTurn
}

/**
 * The editor's view of a level's spawn plan: a linear list of turns (each [EditorEnemySpawn] uses its
 * position in this list as [EditorEnemySpawn.spawnTurn]) plus the loops wrapping turn ranges.
 * Without loops this is exactly the classic flat spawn plan.
 */
data class EditorSpawnTimeline(
    val spawns: List<EditorEnemySpawn>,
    val maxTurn: Int,
    val loops: List<EditorSpawnLoop> = emptyList(),
)

/** A loop with its resolved nesting information. */
data class EditorSpawnLoopNode(
    val loop: EditorSpawnLoop,
    val parentId: String?,
    val depth: Int,
)

/**
 * Orders loops so that parents come before their children (outer before inner, then by start) and
 * resolves each loop's parent. Assumes a valid structure (see [spawnTimelineStructureValid]).
 */
fun resolveLoopNesting(loops: List<EditorSpawnLoop>): List<EditorSpawnLoopNode> {
    val ordered =
        loops
            .withIndex()
            .sortedWith(compareBy({ it.value.startTurn }, { -it.value.endTurn }, { it.index }))
            .map { it.value }
    val result = mutableListOf<EditorSpawnLoopNode>()
    val stack = ArrayDeque<EditorSpawnLoopNode>()
    for (loop in ordered) {
        while (stack.isNotEmpty() && !stack.last().loop.containsRange(loop)) stack.removeLast()
        val node = EditorSpawnLoopNode(loop, stack.lastOrNull()?.loop?.id, stack.size)
        result.add(node)
        stack.addLast(node)
    }
    return result
}

/** Loops must lie within 1..maxTurn, contain a turn, and nest without partial overlaps. */
fun spawnTimelineStructureValid(timeline: EditorSpawnTimeline): Boolean {
    val loops = timeline.loops
    if (loops.any { it.startTurn < 1 || it.endTurn > timeline.maxTurn || it.startTurn > it.endTurn }) return false
    for (i in loops.indices) {
        for (j in i + 1 until loops.size) {
            val a = loops[i]
            val b = loops[j]
            val disjoint = a.endTurn < b.startTurn || b.endTurn < a.startTurn
            if (!disjoint && !a.containsRange(b) && !b.containsRange(a)) return false
        }
    }
    return true
}

/** Loop ids of [loopId] and all its ancestors. */
fun loopWithAncestors(
    loops: List<EditorSpawnLoop>,
    loopId: String,
): Set<String> {
    val parents = resolveLoopNesting(loops).associate { it.loop.id to it.parentId }
    val result = mutableSetOf<String>()
    var current: String? = loopId
    while (current != null && result.add(current)) current = parents[current]
    return result
}

/** Loop ids of [loopId] and all loops nested inside it. */
fun loopWithDescendants(
    loops: List<EditorSpawnLoop>,
    loopId: String,
): Set<String> {
    val nodes = resolveLoopNesting(loops)
    val result = mutableSetOf(loopId)
    nodes.forEach { node -> if (node.parentId != null && node.parentId in result) result.add(node.loop.id) }
    return result
}

/**
 * Converts the timeline into the runtime spawn sequence: plain turns and nested loop groups.
 * Returns `null` when the timeline has no loops (the classic flat plan is used then) or its loop
 * structure is invalid.
 */
fun buildSpawnSequence(timeline: EditorSpawnTimeline): List<SpawnSequenceEntry>? {
    if (timeline.loops.isEmpty() || !spawnTimelineStructureValid(timeline)) return null
    val nodes = resolveLoopNesting(timeline.loops)
    val spawnsByTurn = timeline.spawns.groupBy { it.spawnTurn }

    fun turnSpawns(turn: Int): List<SpawnGroupSpawn> =
        spawnsByTurn[turn].orEmpty().map {
            SpawnGroupSpawn(it.attackerType, 1, it.level, it.spawnPoint, it.loopUnitId, it.spawnsInFirstIterationOnly)
        }

    fun sequence(
        from: Int,
        to: Int,
        parentId: String?,
    ): List<SpawnSequenceEntry> {
        val children = nodes.filter { it.parentId == parentId }.map { it.loop }
        val entries = mutableListOf<SpawnSequenceEntry>()
        var segment = mutableListOf<SpawnGroupTurn>()
        var segmentStart = from

        fun flush(segmentEnd: Int) {
            if (segmentEnd < segmentStart) return
            // Keep only turns with enemies plus the segment's last turn (which fixes its length).
            for (turn in segmentStart..segmentEnd) {
                val spawns = turnSpawns(turn)
                if (spawns.isNotEmpty() || turn == segmentEnd) segment.add(SpawnGroupTurn(turn - segmentStart + 1, spawns))
            }
            entries.addAll(segment)
            segment = mutableListOf()
        }

        var turn = from
        while (turn <= to) {
            val child = children.firstOrNull { it.startTurn == turn }
            if (child == null) {
                turn++
                continue
            }
            flush(turn - 1)
            entries.add(
                SpawnGroup(
                    groupId = child.id,
                    repeatMode = child.repeatMode,
                    repeatCount = child.repeatCount,
                    condition = if (child.repeatMode == SpawnRepeatMode.CONDITION) child.condition ?: SpawnCondition.UNIT_ALIVE else null,
                    targetUnitId = if (child.repeatMode == SpawnRepeatMode.CONDITION) child.targetUnitId else null,
                    entries = sequence(child.startTurn, child.endTurn, child.id),
                ),
            )
            turn = child.endTurn + 1
            segmentStart = turn
        }
        flush(to)
        return entries
    }

    return sequence(1, timeline.maxTurn, null)
}

/**
 * Converts a runtime spawn sequence into the editor timeline. Segment gaps become empty turns,
 * `count` expands into individual entries, and loops without any turn get one empty turn so they
 * stay visible and editable.
 */
fun spawnTimelineFromSequence(entries: List<SpawnSequenceEntry>): EditorSpawnTimeline {
    val spawns = mutableListOf<EditorEnemySpawn>()
    val loops = mutableListOf<EditorSpawnLoop>()

    // Returns the next free timeline turn after laying out [sequence] starting at [start].
    fun layout(
        sequence: List<SpawnSequenceEntry>,
        start: Int,
    ): Int {
        var next = start
        var index = 0
        while (index < sequence.size) {
            when (val entry = sequence[index]) {
                is SpawnGroupTurn -> {
                    val segment = sequence.drop(index).takeWhile { it is SpawnGroupTurn }.filterIsInstance<SpawnGroupTurn>()
                    segment.forEach { turn ->
                        turn.spawns.forEach { spawn ->
                            repeat(spawn.count.coerceAtLeast(1)) {
                                spawns.add(
                                    EditorEnemySpawn(
                                        attackerType = spawn.attackerType,
                                        level = spawn.level,
                                        spawnTurn = next + turn.turnOffset - 1,
                                        spawnPoint = spawn.spawnPoint,
                                        unitId = spawn.unitId,
                                        firstIterationOnly = spawn.spawnsInFirstIterationOnly,
                                    ),
                                )
                            }
                        }
                    }
                    next += segment.maxOf { it.turnOffset }
                    index += segment.size
                }
                is SpawnGroup -> {
                    val loopIndex = loops.size
                    loops.add(EditorSpawnLoop(entry.groupId, next, next))
                    var end = layout(entry.entries, next)
                    if (end == next) end++ // loop without turns: give it one empty turn
                    loops[loopIndex] =
                        EditorSpawnLoop(
                            id = entry.groupId,
                            startTurn = next,
                            endTurn = end - 1,
                            repeatMode = entry.repeatMode,
                            repeatCount = entry.repeatCount,
                            condition = entry.condition,
                            targetUnitId = entry.targetUnitId,
                        )
                    next = end
                    index++
                }
            }
        }
        return next
    }

    val end = layout(entries, 1)
    // Villains are always identified by their name; conditions that used an older custom id follow it.
    val villainIdRenames =
        spawns
            .filter { it.attackerType.isRealVillain && it.unitId != null && it.unitId != it.loopUnitId }
            .associate { it.unitId to it.loopUnitId }
    val renamedLoops = loops.map { loop -> villainIdRenames[loop.targetUnitId]?.let { loop.copy(targetUnitId = it) } ?: loop }
    return EditorSpawnTimeline(spawns.map { if (it.attackerType.isRealVillain) it.copy(unitId = it.loopUnitId) else it }, end - 1, renamedLoops)
}

/** Builds the editor timeline for [level] (flat plan or nested spawn sequence). */
fun EditorLevel.spawnTimeline(): EditorSpawnTimeline =
    spawnGroups?.let { spawnTimelineFromSequence(it) }
        ?: EditorSpawnTimeline(enemySpawns, enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0)

/**
 * Stores [timeline] in the level: a flat plan when there are no loops, otherwise the nested spawn
 * sequence (the flat list is then cleared, since the runtime would ignore it).
 */
fun EditorLevel.withSpawnTimeline(timeline: EditorSpawnTimeline): EditorLevel {
    if (timeline.loops.isEmpty()) {
        return copy(enemySpawns = timeline.spawns.map { it.copy(unitId = null, firstIterationOnly = it.attackerType.isRealVillain) }, spawnGroups = null)
    }
    // An invalid loop structure keeps the content as flat data; validation blocks saving it.
    val sequence = buildSpawnSequence(timeline) ?: return copy(enemySpawns = timeline.spawns, spawnGroups = null)
    return copy(enemySpawns = emptyList(), spawnGroups = sequence)
}

/** All issues of the editor timeline (structure plus the issues of the resulting sequence). */
fun spawnTimelineIssues(
    timeline: EditorSpawnTimeline,
    map: EditorMap? = null,
    stoppedLoopIds: Set<String>? = null,
): Set<SpawnGroupIssue> {
    if (timeline.loops.isEmpty()) return emptySet()
    if (!spawnTimelineStructureValid(timeline)) return setOf(SpawnGroupIssue.STRUCTURE)
    val issues = mutableSetOf<SpawnGroupIssue>()
    if (timeline.loops.any { it.id.isBlank() } || timeline.loops.map { it.id }.distinct().size != timeline.loops.size) {
        issues.add(SpawnGroupIssue.GROUP_ID)
    }
    val sequence = buildSpawnSequence(timeline) ?: return issues + SpawnGroupIssue.STRUCTURE
    return issues + spawnGroupIssues(sequence, map, stoppedLoopIds)
}

data class FlatSpawnPlan(
    val spawns: List<EditorEnemySpawn>,
    val maxTurn: Int,
)

/**
 * Expands a spawn sequence into a flat plan (e.g. for the design preview). Fixed-count loops are
 * expanded; dynamic loops (CONDITION/INFINITE) make the result `null` unless [sampleDynamicLoops]
 * is set, in which case they are shown with a single iteration. Expansion is limited to 10,000
 * turns and 100,000 enemies so an accidental large repetition cannot exhaust memory.
 */
fun flattenSpawnSequence(
    entries: List<SpawnSequenceEntry>,
    sampleDynamicLoops: Boolean = false,
): FlatSpawnPlan? {
    val result = mutableListOf<EditorEnemySpawn>()
    val maxTurns = 10_000
    val maxSpawns = 100_000

    // Returns the number of turns consumed, or null if the expansion is not possible.
    fun expand(
        sequence: List<SpawnSequenceEntry>,
        startTurn: Int,
        repetition: Int,
    ): Int? {
        var next = startTurn
        var index = 0
        while (index < sequence.size) {
            when (val entry = sequence[index]) {
                is SpawnGroupTurn -> {
                    val segment = sequence.drop(index).takeWhile { it is SpawnGroupTurn }.filterIsInstance<SpawnGroupTurn>()
                    segment.forEach { turn ->
                        turn.spawns.filter { !it.spawnsInFirstIterationOnly || repetition == 0 }.forEach { spawn ->
                            if (result.size + spawn.count > maxSpawns) return null
                            repeat(spawn.count.coerceAtLeast(1)) {
                                result.add(EditorEnemySpawn(spawn.attackerType, spawn.level, next + turn.turnOffset - 1, spawn.spawnPoint))
                            }
                        }
                    }
                    next += segment.maxOf { it.turnOffset }
                    index += segment.size
                }
                is SpawnGroup -> {
                    val iterations =
                        when (entry.repeatMode) {
                            SpawnRepeatMode.COUNT -> entry.repeatCount
                            else -> if (sampleDynamicLoops) 1 else return null
                        }
                    for (iteration in 0 until iterations) {
                        val consumed = expand(entry.entries, next, iteration) ?: return null
                        if (consumed == 0) break
                        next += consumed
                        if (next - 1 > maxTurns) return null
                    }
                    index++
                }
            }
            if (next - 1 > maxTurns) return null
        }
        return next - startTurn
    }

    val length = expand(entries, 1, 0) ?: return null
    return FlatSpawnPlan(result, length)
}

/**
 * Checks the executable sequence structure and references, optionally including map compatibility.
 * When [stoppedLoopIds] (the loops stopped by events, see [LevelEvents.stoppedSpawnLoopIds]) is
 * given, every infinite loop must be among them.
 */
fun spawnGroupIssues(
    entries: List<SpawnSequenceEntry>,
    map: EditorMap? = null,
    stoppedLoopIds: Set<String>? = null,
): Set<SpawnGroupIssue> {
    val issues = mutableSetOf<SpawnGroupIssue>()
    val groupIds = mutableSetOf<String>()
    val seenUnitIds = mutableSetOf<String>()
    val seenVillains = mutableSetOf<AttackerType>()

    fun SpawnGroup.repeats(): Boolean = repeatMode != SpawnRepeatMode.COUNT || repeatCount > 1

    fun hasTurns(sequence: List<SpawnSequenceEntry>): Boolean =
        sequence.any { it is SpawnGroupTurn || it is SpawnGroup && hasTurns(it.entries) }

    // [enclosing] lists the groups around [sequence], innermost last.
    fun check(
        sequence: List<SpawnSequenceEntry>,
        enclosing: List<SpawnGroup>,
    ) {
        val segmentOffsets = mutableSetOf<Int>()
        sequence.forEach { entry ->
            when (entry) {
                is SpawnGroupTurn -> {
                    if (entry.turnOffset < 1 || !segmentOffsets.add(entry.turnOffset)) issues.add(SpawnGroupIssue.TURNS)
                    entry.spawns.forEach { spawn ->
                        if (spawn.count < 1 || spawn.level < 1 || spawn.attackerType.isMirrorImage) issues.add(SpawnGroupIssue.SPAWN_VALUES)
                        spawn.unitId?.let { id ->
                            if (id.isBlank() || id != id.trim() || !seenUnitIds.add(id) || spawn.count != 1) issues.add(SpawnGroupIssue.UNIT_ID)
                        }
                        if (spawn.attackerType.isRealVillain) {
                            val innermost = enclosing.lastOrNull()
                            val respawns =
                                innermost != null && innermost.repeats() && !spawn.spawnsInFirstIterationOnly ||
                                    enclosing.dropLast(1).any { it.repeats() }
                            if (spawn.count != 1 || !seenVillains.add(spawn.attackerType) || respawns) issues.add(SpawnGroupIssue.VILLAIN)
                        }
                        if (map != null) {
                            val compatible = map.getCompatibleSpawnPoints(spawn.attackerType)
                            if (compatible.isEmpty() || spawn.spawnPoint != null && spawn.spawnPoint !in compatible) {
                                issues.add(SpawnGroupIssue.SPAWN_POINT)
                            }
                        }
                    }
                }
                is SpawnGroup -> {
                    segmentOffsets.clear()
                    if (entry.groupId.isBlank() || !groupIds.add(entry.groupId)) issues.add(SpawnGroupIssue.GROUP_ID)
                    if (!hasTurns(entry.entries)) issues.add(SpawnGroupIssue.TURNS)
                    if (entry.repeatMode == SpawnRepeatMode.COUNT && entry.repeatCount < 1) issues.add(SpawnGroupIssue.REPEAT_COUNT)
                    if (entry.repeatMode == SpawnRepeatMode.INFINITE && stoppedLoopIds != null && entry.groupId !in stoppedLoopIds) {
                        issues.add(SpawnGroupIssue.INFINITE_NOT_STOPPED)
                    }
                    check(entry.entries, enclosing + entry)
                    // The target unit must be spawned before the end of this loop's first iteration.
                    if (entry.repeatMode == SpawnRepeatMode.CONDITION &&
                        (
                            entry.condition != SpawnCondition.UNIT_ALIVE || entry.targetUnitId.isNullOrBlank() ||
                                entry.targetUnitId !in seenUnitIds
                        )
                    ) {
                        issues.add(SpawnGroupIssue.CONDITION_TARGET)
                    }
                }
            }
        }
    }

    check(entries, emptyList())
    return issues
}

/** Ids of all infinite loops in [entries] (including nested ones) that no event stops. */
fun unstoppedInfiniteSpawnLoops(
    entries: List<SpawnSequenceEntry>?,
    events: LevelEvents,
): List<String> {
    val stopped = events.stoppedSpawnLoopIds()
    return entries.orEmpty().allSpawnGroups().filter { it.repeatMode == SpawnRepeatMode.INFINITE && it.groupId !in stopped }.map { it.groupId }
}

/** Number of "Stop spawn loop" actions in [events] whose target loop is missing or unknown. */
fun invalidSpawnLoopStopCount(
    events: LevelEvents,
    loopIds: Set<String>,
): Int =
    events.events
        .flatMap { it.allActions() }
        .count { it.type == EventActionType.STOP_SPAWN_LOOP && it.spawnLoopId !in loopIds }

/** Applies mappings simultaneously (including swaps), preserving all loop metadata. */
fun remapSpawnGroupPoints(
    entries: List<SpawnSequenceEntry>,
    remappings: Map<Position, Position>,
): List<SpawnSequenceEntry> = entries.mapSpawnEntries { spawn -> spawn.copy(spawnPoint = spawn.spawnPoint?.let { remappings[it] ?: it }) }

// ---------------------------------------------------------------------------------------------
// Timeline editing operations
// ---------------------------------------------------------------------------------------------

/** Generates a loop id that is not used yet. */
fun nextSpawnLoopId(loops: List<EditorSpawnLoop>): String {
    val used = loops.map { it.id }.toSet()
    var number = loops.size + 1
    while ("loop_$number" in used) number++
    return "loop_$number"
}

/** Wraps [turn] into a new innermost loop. */
fun EditorSpawnTimeline.createLoop(turn: Int): EditorSpawnTimeline = copy(loops = loops + EditorSpawnLoop(nextSpawnLoopId(loops), turn, turn))

/**
 * Inserts [count] empty turns after [afterTurn]. Loops whose ids are in [extendLoopIds] and that end
 * exactly at [afterTurn] grow to include the new turns; all later content shifts down.
 */
fun EditorSpawnTimeline.insertTurns(
    afterTurn: Int,
    count: Int,
    extendLoopIds: Set<String> = emptySet(),
): EditorSpawnTimeline =
    copy(
        spawns = spawns.map { if (it.spawnTurn > afterTurn) it.copy(spawnTurn = it.spawnTurn + count) else it },
        maxTurn = maxTurn + count,
        loops =
            loops.map { loop ->
                loop.copy(
                    startTurn = if (loop.startTurn > afterTurn) loop.startTurn + count else loop.startTurn,
                    endTurn =
                        if (loop.endTurn > afterTurn || loop.endTurn == afterTurn && loop.id in extendLoopIds) {
                            loop.endTurn + count
                        } else {
                            loop.endTurn
                        },
                )
            },
    )

/** Appends an empty turn at the end of [loopId] (inside the loop, not inside its nested loops). */
fun EditorSpawnTimeline.addTurnToLoop(loopId: String): EditorSpawnTimeline {
    val loop = loops.firstOrNull { it.id == loopId } ?: return this
    return insertTurns(loop.endTurn, 1, loopWithAncestors(loops, loopId))
}

/** Removes [turn] with its enemies; later turns move up and loops shrink (empty loops disappear). */
fun EditorSpawnTimeline.deleteTurn(turn: Int): EditorSpawnTimeline =
    copy(
        spawns = spawns.filter { it.spawnTurn != turn }.map { if (it.spawnTurn > turn) it.copy(spawnTurn = it.spawnTurn - 1) else it },
        maxTurn = (maxTurn - 1).coerceAtLeast(0),
        loops =
            loops.mapNotNull { loop ->
                val start = if (loop.startTurn > turn) loop.startTurn - 1 else loop.startTurn
                val end = if (loop.endTurn >= turn) loop.endTurn - 1 else loop.endTurn
                if (end < start) null else loop.copy(startTurn = start, endTurn = end)
            },
    )

/** Removes only the loop wrapper; its turns and nested loops stay in place. */
fun EditorSpawnTimeline.unwrapLoop(loopId: String): EditorSpawnTimeline = copy(loops = loops.filterNot { it.id == loopId })

/** Deletes the loop together with all of its turns and nested loops. */
fun EditorSpawnTimeline.deleteLoopWithTurns(loopId: String): EditorSpawnTimeline {
    val loop = loops.firstOrNull { it.id == loopId } ?: return this
    var result = this
    repeat(loop.turnCount) { result = result.deleteTurn(loop.startTurn) }
    return result
}

/**
 * Inserts a copy of the loop (with its turns, enemies, and nested loops) directly after it, inside
 * the same parent loop. Copied loops get new ids; copied unit ids are renamed so they stay unique,
 * and conditions inside the copy refer to the copied units.
 */
fun EditorSpawnTimeline.copyLoop(loopId: String): EditorSpawnTimeline {
    val loop = loops.firstOrNull { it.id == loopId } ?: return this
    val length = loop.turnCount
    val subtreeIds = loopWithDescendants(loops, loopId)
    val ancestors = loopWithAncestors(loops, loopId) - loopId
    val shifted = insertTurns(loop.endTurn, length, ancestors)

    val usedUnitIds = spawns.mapNotNull { it.unitId }.toMutableSet()
    val unitIdMapping = mutableMapOf<String, String>()
    spawns
        .filter { loop.contains(it.spawnTurn) }
        .mapNotNull { it.unitId }
        .forEach { id ->
            var number = 2
            while ("${id}_$number" in usedUnitIds) number++
            unitIdMapping[id] = "${id}_$number"
            usedUnitIds.add("${id}_$number")
        }

    val copiedSpawns =
        spawns
            .filter { loop.contains(it.spawnTurn) }
            .map { it.copy(spawnTurn = it.spawnTurn + length, unitId = it.unitId?.let { id -> unitIdMapping[id] }) }

    val newLoops = shifted.loops.toMutableList()
    loops
        .filter { it.id in subtreeIds }
        .forEach { original ->
            newLoops.add(
                original.copy(
                    id = nextSpawnLoopId(newLoops),
                    startTurn = original.startTurn + length,
                    endTurn = original.endTurn + length,
                    targetUnitId = original.targetUnitId?.let { unitIdMapping[it] ?: it },
                ),
            )
        }
    return shifted.copy(spawns = shifted.spawns + copiedSpawns, loops = newLoops)
}

/** Changes the range of [loopId]; returns `null` if the result would break the nesting structure. */
fun EditorSpawnTimeline.resizeLoop(
    loopId: String,
    startTurn: Int,
    endTurn: Int,
): EditorSpawnTimeline? {
    val updated = copy(loops = loops.map { if (it.id == loopId) it.copy(startTurn = startTurn, endTurn = endTurn) else it })
    return updated.takeIf { spawnTimelineStructureValid(it) }
}
