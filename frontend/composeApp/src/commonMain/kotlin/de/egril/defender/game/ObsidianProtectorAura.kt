package de.egril.defender.game

import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.hexDistanceTo

const val OBSIDIAN_PROTECTOR_AURA_RADIUS = 2

fun GameState.isProtectedByObsidianProtector(target: Attacker): Boolean {
    if (target.type == AttackerType.OBSIDIAN_PROTECTOR) return false
    return attackers.any { protector ->
        !protector.isDefeated.value &&
            protector.type == AttackerType.OBSIDIAN_PROTECTOR &&
            protector.position.value.hexDistanceTo(target.position.value) <= OBSIDIAN_PROTECTOR_AURA_RADIUS
    }
}

fun GameState.canManuallyTargetAttacker(target: Attacker): Boolean = !isProtectedByObsidianProtector(target)

fun GameState.obsidianProtectorAuraPositions(): Set<Position> =
    attackers
        .filter { !it.isDefeated.value && it.type == AttackerType.OBSIDIAN_PROTECTOR }
        .flatMap { protector ->
            (0 until level.gridWidth).flatMap { x ->
                (0 until level.gridHeight).mapNotNull { y ->
                    Position(x, y).takeIf {
                        protector.position.value.hexDistanceTo(it) <= OBSIDIAN_PROTECTOR_AURA_RADIUS
                    }
                }
            }
        }.toSet()
