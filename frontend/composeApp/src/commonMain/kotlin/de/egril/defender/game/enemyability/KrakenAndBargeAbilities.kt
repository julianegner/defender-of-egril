package de.egril.defender.game

import de.egril.defender.config.LogConfig
import de.egril.defender.model.*

/**
 * Abilities that target player barges (raft-mounted towers): Cap'n Roderich's broadside and gold
 * treasure, and The Kraken's dive and barge grip. Extracted from [EnemyAbilitySystem].
 */
internal class KrakenAndBargeAbilities(
    private val state: GameState,
) {
    companion object {
        /** Fixed cooldown (enemy turns) between consecutive Kraken dives. */
        private const val KRAKEN_DIVE_COOLDOWN = 3

        /** How many enemy turns the Kraken stays submerged per dive. */
        private const val KRAKEN_DIVE_DURATION = 2
    }

    /**
     * Broadside (Cap'n Roderich): every [AttackerType.broadsideCooldown] rounds Roderich fires a
     * heavy cannonball at the nearest barge (raft-mounted tower). The barge is immediately sunk —
     * no coins are refunded to the player. Instead, the tower's total build cost is added to
     * Roderich's personal treasure chest ([Attacker.treasureCoins]).
     *
     * The ballista attack animation is reused for the cannonball visual.
     */
    internal fun handleRoderichBroadside(roderich: Attacker) {
        val cooldown = roderich.type.broadsideCooldown ?: return
        if (roderich.villainCooldown.value > 0) {
            roderich.villainCooldown.value--
            return
        }

        // Calculate broadside range: 9 + level
        val broadsideRange = 9 + roderich.level.value

        // Find the nearest barge (any defender mounted on a raft) within range.
        val nearestBarge =
            state.rafts
                .filter { raft -> raft.isActive }
                .mapNotNull { raft ->
                    val defender = state.defenders.find { it.id == raft.defenderId } ?: return@mapNotNull null
                    val distance = roderich.position.value.hexDistanceTo(raft.currentPosition.value)
                    if (distance <= broadsideRange) {
                        Pair(raft, defender)
                    } else {
                        null
                    }
                }.minByOrNull { (raft, _) ->
                    roderich.position.value.hexDistanceTo(raft.currentPosition.value)
                }

        if (nearestBarge != null) {
            val (raft, defender) = nearestBarge
            val bargePosition = raft.currentPosition.value

            // Fire the cannonball animation (reuse ballista effect).
            state.ballistaAttackEffects.add(
                BallistaAttackEffect(
                    sourcePosition = roderich.position.value,
                    targetPosition = bargePosition,
                    turnNumber = state.turnNumber.value,
                ),
            )

            // Queue the barge for deletion after the animation completes.
            // The barge removal is deferred to allow the cannonball animation to play.
            state.pendingBargeDeletions.add(
                PendingBargeDeletion(
                    raftId = raft.id,
                    defenderId = defender.id,
                    towerCost = defender.totalCost,
                    bargePosition = bargePosition,
                ),
            )

            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println(
                    "DEBUG: Roderich Broadside fired at ${defender.type} id=${defender.id} at $bargePosition; " +
                        "range=$broadsideRange (9 + level ${roderich.level.value})",
                )
            }

            // Set cooldown since broadside was fired
            roderich.villainCooldown.value = cooldown
        }
        // If no barge in range, cannonball stays ready (don't set cooldown, don't fire)
    }

    /**
     * Gold Treasure passive (Cap'n Roderich): each enemy turn, Roderich loots [AttackerType.coinsPerTurn]
     * coins and stashes them in his treasure chest ([Attacker.treasureCoins]). The full treasure
     * is awarded to the player when Roderich is eventually defeated.
     */
    internal fun handleRoderichCoinGain(roderich: Attacker) {
        val gain = roderich.type.coinsPerTurn
        if (gain <= 0) return
        roderich.treasureCoins.value += gain

        // Show a coin gain animation at Roderich's position to signal the looting.
        state.coinGainEffects.add(
            CoinGainEffect(
                position = roderich.position.value,
                amount = gain,
                turnNumber = state.turnNumber.value,
            ),
        )
    }

    /**
     * Process pending barge deletions from Roderich's Broadside attacks.
     * Called after animation effects have been displayed to clean up the barges.
     */
    fun processPendingBargeDeletions() {
        for (deletion in state.pendingBargeDeletions) {
            // Find and destroy the raft
            val raft = state.rafts.find { it.id == deletion.raftId }
            if (raft != null) {
                raft.isDestroyed.value = true
            }

            // Remove the defender
            val defender = state.defenders.find { it.id == deletion.defenderId }
            if (defender != null) {
                state.defenders.remove(defender)
            }

            // Find Roderich and add treasure (he should exist since this was queued from his broadside)
            val roderich = state.attackers.find { it.type == AttackerType.CAPTAIN_RODERICH && !it.isDefeated.value }
            if (roderich != null) {
                roderich.treasureCoins.value += deletion.towerCost

                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Processed barge deletion - sank tower at ${deletion.bargePosition}, " +
                            "treasure now ${roderich.treasureCoins.value}",
                    )
                }
            }
        }

        state.pendingBargeDeletions.clear()
    }

    /**
     * Dive tick: called every enemy turn for The Kraken.
     *
     * While diving, the Kraken is invisible and unattackable. Each enemy turn the remaining dive
     * counter decrements. When it reaches zero the Kraken re-surfaces.
     */
    internal fun handleKrakenDiveTick(kraken: Attacker) {
        if (!kraken.isDiving.value) return
        kraken.diveTurnsRemaining.value--
        if (kraken.diveTurnsRemaining.value <= 0) {
            kraken.isDiving.value = false
            kraken.diveTurnsRemaining.value = 0
            if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                println("DEBUG: Kraken resurfaces at ${kraken.position.value}")
            }
        }
    }

    /**
     * Main Kraken ability dispatch: called each enemy turn (after the dive tick).
     *
     * Priority order:
     * 1. If currently diving → skip all other abilities.
     * 2. If a barge grip is active (phase 1 or 2) → advance the grip.
     * 3. If the barge-grip cooldown has expired → look for a barge to grip.
     * 4. If the dive cooldown ([Attacker.summonCooldown]) has expired → trigger a dive.
     */
    internal fun handleKrakenAbilities(kraken: Attacker) {
        if (kraken.isDiving.value) return // Submerged — do nothing

        // ── Barge Grip ──────────────────────────────────────────────────────
        when (kraken.bargeGripPhase.value) {
            1 -> {
                // Phase 1 → Phase 2: drag the barge under and sink it
                val raftId = kraken.grippedRaftId.value
                if (raftId != null) {
                    sinkKrakenGrippedBarge(kraken, raftId)
                }
                kraken.bargeGripPhase.value = 0
                kraken.grippedRaftId.value = null
                val cooldown = kraken.type.bargeGripCooldown ?: 4
                kraken.villainCooldown.value = cooldown
                return
            }
            2 -> {
                // Should not reach phase 2 via this path; treat as complete
                kraken.bargeGripPhase.value = 0
                kraken.grippedRaftId.value = null
                return
            }
        }

        // ── Barge Grip cooldown ─────────────────────────────────────────────
        if (kraken.villainCooldown.value > 0) {
            kraken.villainCooldown.value--
        }

        if (kraken.villainCooldown.value == 0 && kraken.bargeGripPhase.value == 0) {
            val range = kraken.type.bargeGripRange
            val nearestBarge =
                state.rafts
                    .filter { it.isActive }
                    .filter { raft ->
                        kraken.position.value.hexDistanceTo(raft.currentPosition.value) <= range
                    }.minByOrNull { raft ->
                        kraken.position.value.hexDistanceTo(raft.currentPosition.value)
                    }

            if (nearestBarge != null) {
                // Grip phase 1: lock the barge (tower cannot be sold)
                kraken.grippedRaftId.value = nearestBarge.id
                kraken.bargeGripPhase.value = 1

                // Mark the defender on this raft as gripped so the UI can disable selling
                val grippedDefender = state.defenders.find { it.raftId.value == nearestBarge.id }
                grippedDefender?.isGrippedByKraken?.value = true

                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Kraken grips raft ${nearestBarge.id} at ${nearestBarge.currentPosition.value}",
                    )
                }
                return // Gripped this turn — skip dive check
            }
        }

        // ── Dive cooldown / trigger ─────────────────────────────────────────
        if (kraken.type.canDive) {
            if (kraken.summonCooldown.value > 0) {
                kraken.summonCooldown.value--
            }
            if (kraken.summonCooldown.value == 0) {
                kraken.isDiving.value = true
                kraken.diveTurnsRemaining.value = KRAKEN_DIVE_DURATION
                kraken.summonCooldown.value = KRAKEN_DIVE_COOLDOWN
                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println("DEBUG: Kraken dives at ${kraken.position.value}")
                }
            }
        }
    }

    /**
     * Sink a barge that the Kraken has dragged under (end of grip phase 1).
     *
     * The raft is destroyed and the tower on it is removed with no coin refund.
     * Any [isGrippedByKraken] flag on the defender is also cleared.
     */
    private fun sinkKrakenGrippedBarge(
        kraken: Attacker,
        raftId: Int,
    ) {
        val raft = state.rafts.find { it.id == raftId } ?: return
        val defender = state.defenders.find { it.raftId.value == raftId }

        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
            println(
                "DEBUG: Kraken sinks raft $raftId at ${raft.currentPosition.value}" +
                    (defender?.let { "; tower ${it.type} id=${it.id}" } ?: ""),
            )
        }

        // Destroy the raft
        raft.isDestroyed.value = true

        // Remove the tower (no refund to player)
        if (defender != null) {
            defender.isGrippedByKraken.value = false
            state.defenders.remove(defender)
        }

        // Visual: reuse the ballista-attack animation as a "drag under" splash effect
        state.ballistaAttackEffects.add(
            BallistaAttackEffect(
                sourcePosition = kraken.position.value,
                targetPosition = raft.currentPosition.value,
                turnNumber = state.turnNumber.value,
            ),
        )
    }
}
