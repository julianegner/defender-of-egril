package de.egril.defender.model

/**
 * Spawn-loop resolution logic extracted from [GameState]. Each function runs with the state as
 * receiver (via `with(state)`), so it reads and writes the same fields as before. [GameState] keeps
 * the public members and delegates here, so call sites are unchanged.
 */
internal object SpawnLoopOps {

    fun advanceSpawnGroups(
        state: GameState,
        root: SpawnGroup,
        turn: Int,
    ): List<PlannedEnemySpawn> =
        with(state) {
            val startCursor = spawnGroupCursor.value
            if (startCursor.finished) return emptyList()
            // Only resolve each absolute turn once, in increasing order.
            if (turn <= startCursor.lastProcessedTurn) return emptyList()

            // Resolve any pending end-of-iteration decisions *before* spawning. This is done lazily at
            // the start of the turn so a loop predicate (e.g. CONDITION/UNIT_ALIVE) is evaluated only
            // after the previous iteration's final subturn — including that turn's resolved combat,
            // lasting (acid) damage, traps, and defeated-attacker processing — has fully played out.
            val cursor = SpawnSequenceRunner.resolve(root, startCursor, turn, stoppedSpawnLoops.toSet()) { group, repetition ->
                shouldLoop(state, group, repetition)
            }
            spawnGroupCursor.value = cursor.copy(lastProcessedTurn = turn)
            SpawnSequenceRunner.spawnsAt(root, cursor, turn)
        }

    fun shouldLoop(
        state: GameState,
        group: SpawnGroup,
        repetition: Int,
    ): Boolean =
        when (group.repeatMode) {
            SpawnRepeatMode.COUNT -> repetition < group.repeatCount - 1
            SpawnRepeatMode.CONDITION ->
                group.condition == SpawnCondition.UNIT_ALIVE &&
                    group.targetUnitId != null &&
                    state.isSpawnGroupUnitAlive(group.targetUnitId)
            SpawnRepeatMode.INFINITE -> true
        }

    fun forecastRemainingGroupSpawns(
        state: GameState,
        root: SpawnGroup,
    ): List<PlannedEnemySpawn>? =
        with(state) {
            val cursor = spawnGroupCursor.value
            if (cursor.finished) return emptyList()
            SpawnSequenceRunner.forecast(root, cursor, turnNumber.value, stoppedSpawnLoops.toSet())
        }
}
