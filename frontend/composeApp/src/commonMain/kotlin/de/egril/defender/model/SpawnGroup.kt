package de.egril.defender.model

/**
 * How a [SpawnGroup] decides whether to repeat (loop back to its first turn) or advance
 * to the next group once its turn sequence has been fully played.
 *
 * See issue #694 (Spawn Loops): spawn groups replace linear turn-by-turn lists with dynamic
 * loop blocks that are evaluated at the end of every cycle.
 */
enum class SpawnRepeatMode {
    /** Loop the group a fixed number of times ([SpawnGroup.repeatCount] total iterations). */
    COUNT,

    /**
     * Loop while a game-state [SpawnCondition] (currently only [SpawnCondition.UNIT_ALIVE]) holds.
     * Evaluated at the end of each iteration: `true` repeats, `false` advances to the next group.
     */
    CONDITION,

    /**
     * Loop indefinitely. An infinite group never completes on its own, so the level cannot be won
     * by simply clearing the field — only scripted victory conditions can end such a level.
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
 * @param firstIterationOnly When true, this entry only spawns during the group's first iteration
 *                           (repetition 0). Used so a boss spawned once can drive a CONDITION loop
 *                           of add-spawns without being respawned after it dies (which would loop
 *                           forever).
 */
data class SpawnGroupSpawn(
    val attackerType: AttackerType,
    val count: Int = 1,
    val level: Int = 1,
    val spawnPoint: Position? = null,
    val unitId: String? = null,
    val firstIterationOnly: Boolean = false,
)

/**
 * One turn of a [SpawnGroup]. [turnOffset] is 1-based and relative to the start of the current
 * group iteration; gaps between offsets produce empty turns (no spawns), and an entry with an
 * empty [spawns] list is an explicit blank turn.
 */
data class SpawnGroupTurn(
    val turnOffset: Int,
    val spawns: List<SpawnGroupSpawn> = emptyList(),
)

/**
 * An ordered loop block of spawn turns that replaces the flat spawn plan when present on a level.
 *
 * @param groupId     Stable identifier for the group (diagnostics / future references).
 * @param repeatMode  How the group loops (see [SpawnRepeatMode]).
 * @param repeatCount Total iterations for [SpawnRepeatMode.COUNT] (minimum 1).
 * @param condition   Condition for [SpawnRepeatMode.CONDITION].
 * @param targetUnitId Logical unit id checked by [SpawnCondition.UNIT_ALIVE].
 * @param turns       The turns of one iteration, addressed by [SpawnGroupTurn.turnOffset].
 */
data class SpawnGroup(
    val groupId: String,
    val repeatMode: SpawnRepeatMode,
    val repeatCount: Int = 1,
    val condition: SpawnCondition? = null,
    val targetUnitId: String? = null,
    val turns: List<SpawnGroupTurn> = emptyList(),
) {
    /** The largest turn offset in this group; defines how many absolute turns one iteration spans. */
    val maxTurnOffset: Int get() = turns.maxOfOrNull { it.turnOffset } ?: 0
}

/**
 * Serializable runtime cursor tracking progress through a level's [SpawnGroup] list.
 *
 * @param groupIndex         Index of the active group in the level's spawn-group list.
 * @param repetition         Current 0-based iteration of the active group.
 * @param iterationStartTurn Absolute game turn at which the current iteration started (offset 1).
 * @param finished           True once all groups have been fully played (never true while an
 *                           [SpawnRepeatMode.INFINITE] group is active).
 * @param lastProcessedTurn  Highest absolute turn already resolved, guarding against double spawns.
 */
data class SpawnGroupCursor(
    val groupIndex: Int = 0,
    val repetition: Int = 0,
    val iterationStartTurn: Int = 1,
    val finished: Boolean = false,
    val lastProcessedTurn: Int = 0,
)
