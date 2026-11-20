package de.egril.defender.game

import de.egril.defender.config.LogConfig
import de.egril.defender.model.*

/**
 * Shadow Spew ability of Xarithon the Shadow Dragon.
 */
internal class XarithonAbilities(
    private val state: GameState,
) {
    /**
     * Shadow Spew: Xarithon the Shadow Dragon breathes shadowy flames onto a 2×2 area every
     * [AttackerType.shadowSpewCooldown] rounds. All ready towers inside the area are disabled for
     * [AttackerType.shadowSpewDurationTurns] player turns.
     *
     * The 2×2 area is chosen to maximise the number of towers it overlaps. Xarithon does not need
     * to be adjacent to the target — the shadow flames reach any location on the battlefield.
     */
    fun handleXarithonShadowSpew(xarithon: Attacker) {
        val cooldown = xarithon.type.shadowSpewCooldown ?: return
        val duration = xarithon.type.shadowSpewDurationTurns
        if (duration <= 0) return

        // Decrement (or initialise) the spew cooldown via the shared villain cooldown field.
        if (xarithon.villainCooldown.value > 0) {
            xarithon.villainCooldown.value--
            return
        }

        // Cooldown has expired — activate Shadow Spew.
        xarithon.villainCooldown.value = cooldown

        // Find the best 2×2 area: pick the top-left corner of whichever 2×2 block contains the
        // most ready, non-disabled towers.
        val readyTowers =
            state.defenders.filter { tower ->
                tower.isReady && !tower.isDisabled.value
            }
        if (readyTowers.isEmpty()) return

        // Enumerate candidate top-left corners using the positions of existing towers, then pick
        // the corner whose 2×2 square covers the most towers.
        val bestCorner =
            readyTowers
                .flatMap { tower ->
                    val (tx, ty) = tower.position.value
                    // A tower at (tx, ty) is inside a 2×2 block whose top-left is any of:
                    // (tx, ty), (tx-1, ty), (tx, ty-1), (tx-1, ty-1)
                    listOf(
                        Position(tx, ty),
                        Position(tx - 1, ty),
                        Position(tx, ty - 1),
                        Position(tx - 1, ty - 1),
                    )
                }.distinct()
                .maxByOrNull { corner ->
                    val (cx, cy) = corner
                    val affected =
                        setOf(
                            Position(cx, cy),
                            Position(cx + 1, cy),
                            Position(cx, cy + 1),
                            Position(cx + 1, cy + 1),
                        )
                    readyTowers.count { t -> t.position.value in affected }
                } ?: return

        // Disable all ready towers inside the chosen 2×2 area.
        val (cx, cy) = bestCorner
        val spewArea =
            setOf(
                Position(cx, cy),
                Position(cx + 1, cy),
                Position(cx, cy + 1),
                Position(cx + 1, cy + 1),
            )
        // +1 accounts for the immediate decrement at the end of this enemy turn.
        val adjustedDuration = duration + 1
        var anyDisabled = false
        for (tower in state.defenders) {
            if (tower.isReady && !tower.isDisabled.value && tower.position.value in spewArea) {
                tower.isDisabled.value = true
                tower.disabledTurnsRemaining.value = adjustedDuration
                tower.hasShadowSpewAnimation.value = true // Show shadow cloud while disabled
                anyDisabled = true
                if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                    println(
                        "DEBUG: Xarithon Shadow Spew disabled ${tower.type} id=${tower.id} at " +
                            "${tower.position.value} for $duration turns",
                    )
                }
            }
        }

        // Trigger the shadow fireball flying animation from Xarithon to the target area center.
        if (anyDisabled) {
            val targetCenter = Position(cx, cy)
            if (state.shadowSpewEffects.none { it.sourcePosition == xarithon.position.value }) {
                state.shadowSpewEffects.add(
                    ShadowSpewEffect(
                        sourcePosition = xarithon.position.value,
                        targetPosition = targetCenter,
                        turnNumber = state.turnNumber.value,
                    ),
                )
            }
        }
    }
}
