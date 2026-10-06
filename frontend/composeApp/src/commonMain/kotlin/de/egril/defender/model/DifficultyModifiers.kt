package de.egril.defender.model

import de.egril.defender.ui.settings.DifficultyLevel

/**
 * Utility functions for applying difficulty modifiers to game elements
 */
object DifficultyModifiers {
    /**
     * Apply difficulty modifier to initial coins
     * BABY: 2x coins
     * EASY: 2x coins
     * MEDIUM: no change
     * HARD: no change
     * NIGHTMARE: no change
     */
    fun applyCoinsModifier(
        baseCoins: Int,
        difficulty: DifficultyLevel,
    ): Int =
        when (difficulty) {
            DifficultyLevel.BABY, DifficultyLevel.EASY -> baseCoins * 2
            else -> baseCoins
        }

    /**
     * Apply difficulty modifier to initial health points
     * BABY: 2x HP
     * EASY: 2x HP
     * MEDIUM: no change
     * HARD: no change
     * NIGHTMARE: no change
     */
    fun applyHealthPointsModifier(
        baseHP: Int,
        difficulty: DifficultyLevel,
    ): Int =
        when (difficulty) {
            DifficultyLevel.BABY, DifficultyLevel.EASY -> baseHP * 2
            else -> baseHP
        }

    /**
     * Get the initial tower level based on difficulty
     * BABY: level 3
     * All others: level 1
     */
    fun getInitialTowerLevel(difficulty: DifficultyLevel): Int =
        when (difficulty) {
            DifficultyLevel.BABY -> 3
            else -> 1
        }

    /**
     * Apply difficulty modifier to enemy spawn plan
     * HARD: All enemies have 2x level
     * NIGHTMARE: Enemies spawn 3 times with 3x, 2x, and 1x level (Ewhad spawns once with 5x level)
     * Others: no change
     */
    fun applySpawnPlanModifier(
        basePlan: List<PlannedEnemySpawn>,
        difficulty: DifficultyLevel,
    ): List<PlannedEnemySpawn> =
        when (difficulty) {
            DifficultyLevel.HARD -> {
                // Double all enemy levels
                basePlan.map { spawn ->
                    spawn.copy(level = spawn.level * 2)
                }
            }
            DifficultyLevel.NIGHTMARE -> {
                // Triple spawn enemies with 3x, 2x, and 1x levels
                // Ewhad spawns once with 5x level
                val modifiedPlan = mutableListOf<PlannedEnemySpawn>()

                for (spawn in basePlan) {
                    if (spawn.attackerType == AttackerType.EWHAD) {
                        // Ewhad spawns once with 5x level
                        modifiedPlan.add(spawn.copy(level = spawn.level * 5))
                    } else {
                        // Other enemies spawn 3 times with different levels
                        modifiedPlan.add(spawn.copy(level = spawn.level * 3))
                        modifiedPlan.add(spawn.copy(level = spawn.level * 2))
                        modifiedPlan.add(spawn.copy(level = spawn.level))
                    }
                }

                modifiedPlan
            }
            else -> basePlan
        }

    /**
     * Apply difficulty modifier to spawn groups (issue #694), mirroring [applySpawnPlanModifier]
     * while preserving loop and named-unit semantics:
     *  - HARD: every spawn's level is doubled.
     *  - NIGHTMARE: regular enemies are tripled with 3x/2x/1x levels (as in the flat plan); named,
     *    first-iteration-only, and Ewhad spawns stay a single entry (5x level for Ewhad, 3x
     *    otherwise) so a boss keeps a stable, single identity and UNIT_ALIVE bindings are not broken
     *    by duplicate spawns.
     *  - Others: unchanged.
     */
    fun applySpawnGroupsModifier(
        groups: List<SpawnGroup>,
        difficulty: DifficultyLevel,
    ): List<SpawnGroup> =
        when (difficulty) {
            DifficultyLevel.HARD ->
                groups.map { group ->
                    group.copy(
                        turns =
                            group.turns.map { turn ->
                                turn.copy(spawns = turn.spawns.map { it.copy(level = it.level * 2) })
                            },
                    )
                }
            DifficultyLevel.NIGHTMARE ->
                groups.map { group ->
                    group.copy(
                        turns =
                            group.turns.map { turn ->
                                turn.copy(spawns = turn.spawns.flatMap { expandSpawnForNightmare(it) })
                            },
                    )
                }
            else -> groups
        }

    /**
     * Expand a single spawn-group spawn for NIGHTMARE difficulty. Units that must keep a single,
     * stable identity (named via [SpawnGroupSpawn.unitId], [SpawnGroupSpawn.firstIterationOnly], or
     * Ewhad) are scaled in level but not duplicated; all other enemies are tripled with 3x/2x/1x
     * levels, matching the flat-plan behavior.
     */
    private fun expandSpawnForNightmare(spawn: SpawnGroupSpawn): List<SpawnGroupSpawn> {
        val spawnsOnce = spawn.firstIterationOnly || spawn.unitId != null || spawn.attackerType == AttackerType.EWHAD
        return if (spawnsOnce) {
            val multiplier = if (spawn.attackerType == AttackerType.EWHAD) 5 else 3
            listOf(spawn.copy(level = spawn.level * multiplier))
        } else {
            listOf(
                spawn.copy(level = spawn.level * 3),
                spawn.copy(level = spawn.level * 2),
                spawn.copy(level = spawn.level),
            )
        }
    }
}
