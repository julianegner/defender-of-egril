package de.egril.defender.game

import de.egril.defender.config.LogConfig
import de.egril.defender.model.*
import kotlin.math.min

/**
 * Witch healing and tower disabling, plus the Coven Synergy boosts that villain auras grant to witches.
 */
internal class WitchAbilities(
    private val state: GameState,
    private val pathfinding: PathfindingSystem,
) {
    companion object {
        /** Heal multiplier applied to green witches empowered by the Coven Synergy aura (50 % extra). */
        internal const val COVEN_HEAL_BOOST_MULTIPLIER = 1.5f

        /** Extra disable rounds granted to red witches empowered by the Coven Synergy aura. */
        internal const val COVEN_DISABLE_EXTRA_TURNS = 1
    }

    /**
     * Attacker IDs of GREEN_WITCH units that benefit from a Coven Heal Boost this round.
     * Recomputed at the start of every [EnemyAbilitySystem.processEnemyAbilities] call.
     */
    internal val covenEnhancedHealWitchIds = mutableSetOf<Int>()

    /**
     * Attacker IDs of RED_WITCH units that benefit from a Coven Disable Boost this round.
     * Recomputed at the start of every [EnemyAbilitySystem.processEnemyAbilities] call.
     */
    internal val covenEnhancedDisableWitchIds = mutableSetOf<Int>()

    /**
     * Apply green-witch healing to adjacent units.
     *
     * @param witch     The healer (GREEN_WITCH, HAGA, or GRAND_COVEN_MOTHER_SYBILLA).
     * @param enhanced  When true, healing is multiplied by [COVEN_HEAL_BOOST_MULTIPLIER] (coven synergy).
     */
    fun applyGreenWitchHealing(
        witch: Attacker,
        enhanced: Boolean = false,
    ) {
        val adjacentPositions = witch.position.value.getHexNeighbors()
        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println(
                "DEBUG: Green witch ${witch.id} at ${witch.position.value} checking ${adjacentPositions.size} adjacent positions" +
                    if (enhanced) " (COVEN BOOST)" else "",
            )
        }
        var healedCount = 0
        for (adjacent in adjacentPositions) {
            val adjacentEnemy =
                state.attackers.find {
                    !it.isDefeated.value && it.id != witch.id && it.position.value == adjacent
                }
            if (adjacentEnemy != null) {
                // Heal 5x witch level (×1.5 with coven boost), capped at missing HP
                val baseHeal = witch.effectiveLevel * 5
                val scaledHeal = if (enhanced) (baseHeal * COVEN_HEAL_BOOST_MULTIPLIER).toInt() else baseHeal
                val healAmount = min(scaledHeal, adjacentEnemy.maxHealth - adjacentEnemy.currentHealth.value)
                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Found ${adjacentEnemy.type} at $adjacent, HP ${adjacentEnemy.currentHealth.value}/${adjacentEnemy.maxHealth}, heal amount: $healAmount",
                    )
                }
                if (healAmount > 0) {
                    adjacentEnemy.currentHealth.value += healAmount
                    healedCount++
                    state.healingEffects.add(
                        HealingEffect(
                            position = adjacent,
                            type = HealingEffectType.GREEN_WITCH,
                            healAmount = healAmount,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
                    if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                        println(
                            "DEBUG: Healed ${adjacentEnemy.type} for $healAmount HP (new HP: ${adjacentEnemy.currentHealth.value})",
                        )
                    }
                }

                // Remove up to 3 barbs from adjacent enemy
                if (adjacentEnemy.movementPenalty.value > 0) {
                    val barbsToRemove = minOf(3, adjacentEnemy.movementPenalty.value)
                    adjacentEnemy.movementPenalty.value -= barbsToRemove
                    if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                        println(
                            "DEBUG: Green witch removed $barbsToRemove barbs from ${adjacentEnemy.type}, remaining penalty: ${adjacentEnemy.movementPenalty.value}",
                        )
                    }
                }
            }
        }
        if (healedCount > 0) {
            println("DEBUG: Green witch ${witch.id} healed $healedCount enemies${if (enhanced) " (coven boost)" else ""}")
        } else {
            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println("DEBUG: Green witch ${witch.id} found no adjacent damaged enemies to heal")
            }
        }
    }

    /**
     * Mark all GREEN_WITCH attackers within the villain's aura range as having enhanced healing.
     * The boost is applied when [applyGreenWitchHealing] is called with enhanced=true.
     */
    fun applyCovenHealBoost(
        villain: Attacker,
        ability: VillainAbility,
    ) {
        for (ally in state.attackers) {
            if (ally.isDefeated.value || ally.id == villain.id) continue
            if (ally.type != AttackerType.GREEN_WITCH) continue
            val inRange =
                ability.range == VillainAbility.FULL_BATTLEFIELD ||
                    villain.position.value.hexDistanceTo(ally.position.value) <= ability.range
            if (inRange) covenEnhancedHealWitchIds.add(ally.id)
        }
    }

    /**
     * Mark all RED_WITCH attackers within the villain's aura range as having enhanced disabling.
     * The boost adds [COVEN_DISABLE_EXTRA_TURNS] to the disable duration in [disableNearestTowerInRange].
     */
    fun applyCovenDisableBoost(
        villain: Attacker,
        ability: VillainAbility,
    ) {
        for (ally in state.attackers) {
            if (ally.isDefeated.value || ally.id == villain.id) continue
            if (ally.type != AttackerType.RED_WITCH) continue
            val inRange =
                ability.range == VillainAbility.FULL_BATTLEFIELD ||
                    villain.position.value.hexDistanceTo(ally.position.value) <= ability.range
            if (inRange) covenEnhancedDisableWitchIds.add(ally.id)
        }
    }

    /**
     * Coven Swap: Sybilla teleports to a witch within [range] tiles (and that witch moves to
     * Sybilla's former position). Prioritises the witch that is closest to Sybilla.
     * Does nothing if no suitable witch is found.
     */
    fun handleCovenSwap(
        sybilla: Attacker,
        range: Int,
    ) {
        val sybillaPos = sybilla.position.value
        val sybillaStartPos = state.enemyTurnStartPositions[sybilla.id] ?: return

        // Sybilla can't swap if she's already moved this turn
        if (sybillaPos != sybillaStartPos) {
            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println("DEBUG: Sybilla ${sybilla.id} skipped swap because she already moved this turn")
            }
            return
        }

        val witchTypes = setOf(AttackerType.GREEN_WITCH, AttackerType.RED_WITCH, AttackerType.HAGA, AttackerType.ZUSSA)
        val nearbyWitches =
            state.attackers
                .filter { ally ->
                    !ally.isDefeated.value &&
                        ally.id != sybilla.id &&
                        ally.type in witchTypes &&
                        sybillaPos.hexDistanceTo(ally.position.value) <= range
                }

        // Find witches close to target (1-2 moves away)
        val targetAdjacentWitches =
            nearbyWitches
                .mapNotNull { witch ->
                    val target = witch.currentTarget?.value ?: state.level.targetPositions.first()
                    val distanceToTarget = pathfinding.findPath(witch.position.value, target, witch).size - 1
                    if (distanceToTarget in 1..2) witch else null
                }

        // Prefer target-adjacent witches; if none, consider witches in a crowd (3+ witches)
        val swapTarget =
            if (targetAdjacentWitches.isNotEmpty()) {
                targetAdjacentWitches.minByOrNull { sybillaPos.hexDistanceTo(it.position.value) }
            } else if (nearbyWitches.size >= 3) {
                nearbyWitches.minByOrNull { sybillaPos.hexDistanceTo(it.position.value) }
            } else {
                null
            }

        if (swapTarget != null) {
            val targetPos = swapTarget.position.value
            swapTarget.position.value = sybillaPos
            sybilla.position.value = targetPos

            // Queue the swap message with highlight positions
            state.pendingMessages.add(
                GameMessage(
                    type = GameMessageType.COVEN_SWAP,
                    name = null,
                    highlightPositions = sybillaStartPos to targetPos,
                ),
            )

            // Add visual effects for both positions to show the swap
            state.enemyMoveEffects.add(EnemyMoveEffect(sybillaStartPos, state.turnNumber.value))
            state.enemyMoveEffects.add(EnemyMoveEffect(targetPos, state.turnNumber.value))

            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println("DEBUG: Sybilla ${sybilla.id} swapped with ${swapTarget.type} ${swapTarget.id} ($sybillaStartPos <-> $targetPos)")
            }
        }
    }

    /**
     * Red Witch disables towers within [range] hex distance.
     * Disables one tower per turn.
     * Duration: 1 turn base, +1 turn for every 5 levels (level 5=2 turns, level 10=3 turns, etc.)
     * +1 for immediate decrement at end of this turn.
     * An optional [extraDisableTurns] is added when a Coven Synergy villain is nearby.
     * Can only disable towers where tower level <= witch level.
     */
    fun disableNearestTowerInRange(
        witch: Attacker,
        range: Int,
        extraDisableTurns: Int = 0,
    ) {
        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println(
                "DEBUG: Disabler ${witch.id} level ${witch.level.value} at ${witch.position.value} checking range=$range" +
                    if (extraDisableTurns > 0) " (coven boost +$extraDisableTurns)" else "",
            )
        }

        // Log all towers in the game for debugging
        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println("DEBUG: All towers in game (${state.defenders.size}):")
        }
        state.defenders.forEach { tower ->
            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println(
                    "DEBUG:   ${tower.type} id=${tower.id} level=${tower.level.value} at ${tower.position.value} isReady=${tower.isReady} isDisabled=${tower.isDisabled.value}",
                )
            }
        }

        // Find towers within range that:
        // - Are ready (not building)
        // - Are not already disabled
        // - Are within the given hex range
        // - Can be disabled by this witch (tower level <= witch level)
        val eligibleTowers =
            state.defenders.filter { tower ->
                val isReady = tower.isReady
                val notDisabled = !tower.isDisabled.value
                val withinRange =
                    if (range == 1) {
                        witch.position.value
                            .getHexNeighbors()
                            .contains(tower.position.value)
                    } else {
                        witch.position.value.hexDistanceTo(tower.position.value) <= range
                    }
                val canDisable = tower.level.value <= witch.effectiveLevel

                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Checking tower ${tower.type} id=${tower.id}: isReady=$isReady, notDisabled=$notDisabled, withinRange=$withinRange, canDisable=$canDisable",
                    )
                }

                isReady && notDisabled && withinRange && canDisable
            }

        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println("DEBUG: Found ${eligibleTowers.size} eligible towers to disable")
        }

        if (eligibleTowers.isEmpty()) {
            println("DEBUG: Disabler ${witch.id} found no eligible towers to disable")
            return
        }

        // Pick the closest tower (any eligible tower is valid; prefer closest)
        val targetTower =
            eligibleTowers.minByOrNull { tower ->
                witch.position.value.hexDistanceTo(tower.position.value)
            }

        if (targetTower != null) {
            // Calculate disable duration: 1 turn base + 1 per 5 levels
            // +1 to account for immediate decrement at end of this turn
            // Level 1-4: 2 turns (disabled for 1 player turn)
            // Level 5-9: 3 turns (disabled for 2 player turns)
            // Level 10-14: 4 turns (disabled for 3 player turns)
            // Level 20-24: 5 turns (disabled for 4 player turns), etc.
            val disableDuration = 1 + (witch.effectiveLevel / 5) + 1 + extraDisableTurns

            targetTower.isDisabled.value = true
            targetTower.disabledTurnsRemaining.value = disableDuration

            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println(
                    "DEBUG: Disabler ${witch.id} disabled ${targetTower.type} id=${targetTower.id} at ${targetTower.position.value} for $disableDuration turns",
                )
            }
        }
    }

    /**
     * Update tower disable status - decrement timers and re-enable towers
     */
    fun updateTowerDisableStatus() {
        for (tower in state.defenders) {
            if (tower.isDisabled.value) {
                tower.disabledTurnsRemaining.value--
                if (tower.disabledTurnsRemaining.value <= 0) {
                    tower.isDisabled.value = false
                    tower.hasRootGripAnimation.value = false // Clear vine animation when disable expires
                    tower.hasShadowSpewAnimation.value = false // Clear shadow cloud animation when disable expires
                }
            }
        }
    }

    /**
     * Find the nearest active tower for Red Witch to target
     */
    fun findNearestActiveTower(witch: Attacker): Defender? {
        val eligibleTowers =
            state.defenders.filter { tower ->
                tower.isReady && !tower.isDisabled.value && tower.level.value <= witch.effectiveLevel
            }

        if (eligibleTowers.isEmpty()) return null

        return eligibleTowers.minByOrNull { tower ->
            tower.position.value.distanceTo(witch.position.value)
        }
    }

    /**
     * Find a position on the path near the target tower for Red Witch to move towards
     */
    fun findPathPositionNearTower(towerPosition: Position): Position {
        // Find all path positions adjacent to the tower
        val adjacentPathPositions =
            towerPosition.getHexNeighbors().filter { pos ->
                pos.x >= 0 &&
                    pos.x < state.level.gridWidth &&
                    pos.y >= 0 &&
                    pos.y < state.level.gridHeight &&
                    state.level.isOnPath(pos)
            }

        // Return the first adjacent path position, or tower position if none found
        return adjacentPathPositions.firstOrNull() ?: towerPosition
    }

    /**
     * Find the nearest damaged enemy for Green Witch to move towards and heal.
     * Prioritizes Ewhad if he exists and has damaged health.
     */
    fun findHealingTarget(witch: Attacker): Attacker? {
        // First check if Ewhad exists and is damaged
        val ewhad =
            state.attackers.find {
                it.type == AttackerType.EWHAD &&
                    !it.isDefeated.value &&
                    it.currentHealth.value < it.maxHealth
            }

        if (ewhad != null) {
            return ewhad // Always prioritize healing Ewhad
        }

        // Otherwise, find nearest damaged enemy
        val damagedEnemies =
            state.attackers.filter {
                !it.isDefeated.value &&
                    it.id != witch.id &&
                    it.currentHealth.value < it.maxHealth
            }

        if (damagedEnemies.isEmpty()) return null

        return damagedEnemies.minByOrNull { enemy ->
            witch.position.value.distanceTo(enemy.position.value)
        }
    }

    /**
     * Find the nearest tower that is not disabled for Red Witch to move towards and disable.
     * Returns the tower's position if found.
     * Only targets towers that the witch can actually disable (tower level <= witch level).
     */
    fun findTowerTarget(witch: Attacker): Position? {
        // Find ready towers that are not disabled and can be disabled by this witch
        val availableTowers =
            state.defenders.filter { tower ->
                tower.isReady && !tower.isDisabled.value && tower.level.value <= witch.effectiveLevel
            }

        if (availableTowers.isEmpty()) return null

        // Find the closest tower
        val nearestTower =
            availableTowers.minByOrNull { tower ->
                witch.position.value.distanceTo(tower.position.value)
            }

        return nearestTower?.position?.value
    }
}
