package de.egril.defender.model

/**
 * Win/loss rules extracted from [GameState]. Each function runs with the state as receiver
 * (via `with(state)`). [GameState] keeps the public members and delegates here, so call sites are unchanged.
 */
internal object WinLossRules {
    fun isLevelWon(state: GameState): Boolean {
        with(state) {
            // Sandbox levels can never be won, even when all enemies are gone.
            if (level.isSandbox) return false
            if (scriptedVictory.value) return true
            // Check if all planned spawns have occurred and all enemies are defeated.
            // Spawn-loop levels (issue #694) are "all spawned" once the group cursor has finished, or
            // once the remaining scripted schedule is statically exhausted (every future spawn is in the
            // past). The latter keeps victory prompt even though the cursor advances lazily (the loop
            // decision is deferred one turn so CONDITION checks see fully-resolved combat). A forecast of
            // null means the schedule is unbounded (an active CONDITION/INFINITE group), so such levels
            // cannot be won by clearing the field.
            val allSpawned =
                if (spawnGroups != null) {
                    spawnGroupCursor.value.finished || forecastRemainingGroupSpawns()?.isEmpty() == true
                } else {
                    spawnPlan.all { it.spawnTurn <= turnNumber.value }
                }
            return allSpawned &&
                attackers
                    .filter { it.type != AttackerType.THE_KRAKEN }
                    .all { it.isDefeated.value }
        }
    }

    fun isLevelLost(state: GameState): Boolean {
        with(state) {
            if (healthPoints.value <= 0) return true
            // A villain breaching a target loses the level immediately, regardless of remaining health.
            if (villainReachedTarget.value) return true
            // Level is also lost when all SINGLE_HIT targets have been taken
            val singleHitTargets = level.targetInfoMap.filter { it.value.type == TargetType.SINGLE_HIT }.keys
            if (singleHitTargets.isNotEmpty() && takenTargets.containsAll(singleHitTargets)) return true
            return false
        }
    }

    /**
     * Total worst-case health-point damage the player can still take, assuming every remaining
     * enemy (both those alive on the field and those still to spawn) reaches the target unhindered.
     * Uses [Long] so summoner/boss "all HP" markers ([Int.MAX_VALUE]) can be summed without overflow.
     */
    fun getRemainingEnemyThreat(state: GameState): Long {
        with(state) {
            var total = 0L
            for (attacker in attackers) {
                if (attacker.isDefeated.value) continue
                total += attacker.calculateTargetDamage().toLong()
            }
            for (spawn in spawnPlan) {
                if (spawn.spawnTurn > turnNumber.value) {
                    total += attackerTargetDamage(spawn.attackerType, spawn.level).toLong()
                }
            }
            // Spawn-loop levels: add the worst-case threat of all remaining scripted spawns. When the
            // remaining spawns are unbounded (an active CONDITION/INFINITE group), the threat is treated
            // as unbounded so no guaranteed win is ever offered.
            if (spawnGroups != null && !spawnGroupCursor.value.finished) {
                val forecast = forecastRemainingGroupSpawns()
                if (forecast == null) {
                    return Long.MAX_VALUE
                }
                for (spawn in forecast) {
                    total += attackerTargetDamage(spawn.attackerType, spawn.level).toLong()
                }
            }
            return total
        }
    }

    /**
     * Returns true when the level is guaranteed to be won: even if every remaining enemy reached the
     * target, the player would still have health points left (or, for SINGLE_HIT-only levels, not
     * enough remaining enemies to take all remaining targets). Used to offer an instant "Win Level now".
     *
     * Excluded cases where a win cannot be guaranteed:
     *  - Not during the player's turn (e.g. building phase or enemy turn).
     *  - When a summoner enemy remains, since it can create an unbounded number of additional units.
     *  - When a villain remains (on the field or still to spawn), since a villain reaching a target
     *    loses the level outright, regardless of remaining health.
     *
     * Levels mixing SINGLE_HIT and STANDARD targets (or with no SINGLE_HIT targets at all) fall back
     * to the HP-based calculation, since at least one STANDARD target always remains reachable.
     */
    fun canWinLevelNow(state: GameState): Boolean {
        with(state) {
            // Sandbox levels can never be won, so never offer the instant win.
            if (level.isSandbox) return false
            if (phase.value != GamePhase.PLAYER_TURN) return false

            val singleHitTargets = level.targetInfoMap.filter { it.value.type == TargetType.SINGLE_HIT }.keys
            val onlySingleHitTargets = singleHitTargets.isNotEmpty() && singleHitTargets.size == level.targetInfoMap.size
            if (isLevelLost() || isLevelWon()) return false

            val aliveEnemies = attackers.filter { !it.isDefeated.value }
            // A spawn-loop level with an unbounded remaining schedule (active CONDITION/INFINITE group)
            // can never offer a guaranteed win, since the number of future enemies is not bounded.
            if (spawnGroups != null && !spawnGroupCursor.value.finished && forecastRemainingGroupSpawns() == null) {
                return false
            }
            val enemiesToSpawn =
                if (spawnGroups != null) {
                    forecastRemainingGroupSpawns() ?: return false
                } else {
                    spawnPlan.filter { it.spawnTurn > turnNumber.value }
                }
            // There must be at least one remaining enemy (otherwise the level is already won).
            if (aliveEnemies.isEmpty() && enemiesToSpawn.isEmpty()) return false
            // Summoners can create additional enemies, so the total threat cannot be bounded.
            if (aliveEnemies.any { it.type.isSummoner() } || enemiesToSpawn.any { it.attackerType.isSummoner() }) return false
            // A villain (on the field or still to spawn) loses the level the moment it reaches a target,
            // regardless of remaining health, so a guaranteed win can never be offered while one remains.
            if (aliveEnemies
                    .filter { it.type != AttackerType.THE_KRAKEN }
                    .any { it.type.isRealVillain } ||
                enemiesToSpawn
                    .filter { it.attackerType != AttackerType.THE_KRAKEN }
                    .any { it.attackerType.isRealVillain }
            ) {
                return false
            }

            if (onlySingleHitTargets) {
                // Each remaining enemy can take at most one SINGLE_HIT target. If there are fewer
                // remaining enemies than remaining (untaken) targets, not all targets can be taken,
                // so the level is guaranteed to be won once all enemies are defeated.
                val remainingSingleHitTargets = singleHitTargets.count { !takenTargets.contains(it) }
                return aliveEnemies.size + enemiesToSpawn.size < remainingSingleHitTargets
            }

            return getRemainingEnemyThreat(state) < healthPoints.value.toLong()
        }
    }
}
