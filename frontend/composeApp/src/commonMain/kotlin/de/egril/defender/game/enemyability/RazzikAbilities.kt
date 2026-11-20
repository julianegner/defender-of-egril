package de.egril.defender.game

import de.egril.defender.model.*
import kotlin.random.Random

private const val RAZZIK_ROCKETS_PER_ROUND = 5
private const val RAZZIK_ROCKET_RANGE = 8

/** A rocket crashes with probability 1/RAZZIK_CRASH_ODDS. */
private const val RAZZIK_CRASH_ODDS = 3

private sealed class RazzikRocketTarget {
    abstract val position: Position

    class BarricadeTarget(
        val barricade: Barricade,
    ) : RazzikRocketTarget() {
        override val position: Position get() = barricade.position
    }

    class BridgeTarget(
        override val position: Position,
    ) : RazzikRocketTarget()
}

/**
 * Razzik fires rockets at barricades and bridges within range. Rockets that crash mid-flight
 * explode on the path and damage every attacker on that tile.
 */
internal class RazzikAbilities(
    private val state: GameState,
    private val barricadeSystem: BarricadeSystem,
    private val bridgeSystem: BridgeSystem,
    private val random: Random = Random.Default,
) {
    /**
     * Razzik fires [RAZZIK_ROCKETS_PER_ROUND] rockets every enemy turn. Each rocket picks a random
     * barricade or bridge within [RAZZIK_ROCKET_RANGE] tiles. A rocket that crashes mid-flight
     * explodes on a tile along its path instead, damaging every attacker standing there.
     */
    fun handleRazzikRockets(razzik: Attacker) {
        repeat(RAZZIK_ROCKETS_PER_ROUND) {
            fireRazzikRocket(razzik)
        }
    }

    private fun fireRazzikRocket(razzik: Attacker) {
        val origin = razzik.position.value
        val target = razzikRocketTargets(origin).randomOrNull(random) ?: return
        val path = hexLine(origin, target.position)
        val damage = random.nextInt(1, 7) * 10

        if (random.nextInt(RAZZIK_CRASH_ODDS) == 0) {
            // Rocket crashes somewhere after leaving the launcher; the tile it lands on is the explosion.
            val crashPosition = path.drop(1).randomOrNull(random) ?: return
            state.rocketAttackEffects.add(
                RocketAttackEffect(
                    sourcePosition = origin,
                    targetPosition = crashPosition,
                    turnNumber = state.turnNumber.value,
                ),
            )
            damageAttackersAt(crashPosition, damage, exclude = razzik)
        } else {
            state.rocketAttackEffects.add(
                RocketAttackEffect(
                    sourcePosition = origin,
                    targetPosition = target.position,
                    turnNumber = state.turnNumber.value,
                ),
            )
            when (target) {
                is RazzikRocketTarget.BarricadeTarget -> barricadeSystem.handleEnemyAttackBarricade(razzik, target.barricade, damage)
                is RazzikRocketTarget.BridgeTarget -> bridgeSystem.damageBridge(target.position, damage)
            }
        }
    }

    /**
     * Barricades with HP and destructible bridges within rocket range. Each bridge is represented by
     * its covered tile closest to Razzik, so multi-tile bridges are not favoured as targets.
     */
    private fun razzikRocketTargets(origin: Position): List<RazzikRocketTarget> {
        val barricades =
            state.barricades
                .filter { it.healthPoints.value > 0 && origin.hexDistanceTo(it.position) <= RAZZIK_ROCKET_RANGE }
                .map { RazzikRocketTarget.BarricadeTarget(it) }
        val bridges =
            state.bridges
                .filter { it.isActive && it.type != BridgeType.MAGICAL && !it.isIndestructible }
                .mapNotNull { bridge ->
                    bridge.positions
                        .filter { origin.hexDistanceTo(it) <= RAZZIK_ROCKET_RANGE }
                        .minByOrNull { origin.hexDistanceTo(it) }
                        ?.let { RazzikRocketTarget.BridgeTarget(it) }
                }
        return barricades + bridges
    }

    /**
     * Crash explosions hit every unit with HP on the tile, friend or foe. Obsidian protector and
     * rune glyph ward are honoured the same way as for direct attacks.
     */
    private fun damageAttackersAt(
        position: Position,
        damage: Int,
        exclude: Attacker,
    ) {
        state.attackers
            .filter { it.id != exclude.id && !it.isDefeated.value && it.position.value == position }
            .forEach { attacker ->
                if (state.isProtectedByObsidianProtector(attacker)) return@forEach
                val effectiveDamage = (damage - state.runeGlyphWardArmor(attacker)).coerceAtLeast(0)
                attacker.recordDamageTaken(minOf(attacker.currentHealth.value, effectiveDamage))
                attacker.currentHealth.value = (attacker.currentHealth.value - effectiveDamage).coerceAtLeast(0)
                if (attacker.currentHealth.value <= 0) {
                    attacker.isDefeated.value = true
                }
            }
    }
}
