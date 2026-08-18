package de.egril.defender.game

import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.hexDistanceTo

internal fun calculateEffectiveEnemySpeed(
    state: GameState,
    attacker: Attacker,
    currentPos: Position,
): Int {
    val ignoresSlowing = state.level.waaghEnabled && state.waaghFrenzyActive.value && attacker.type == AttackerType.GOBLIN
    val baseSpeed =
        if (ignoresSlowing) {
            attacker.type.speed * 2
        } else {
            var speed = maxOf(1, attacker.type.speed - attacker.movementPenalty.value)
            if (attacker.type == AttackerType.ORK &&
                (state.level.waaghEnabled && state.waaghFrenzyActive.value || attacker.bloodlustRoundsLeft.value > 0)
            ) {
                speed *= 2
            }
            speed
        }

    var effectiveSpeed = baseSpeed + attacker.speedBonus.value
    // Mushroom buff: doubles movement speed for 2 turns
    if (attacker.mushroomTurnsRemaining.value > 0) {
        effectiveSpeed += maxOf(1, attacker.type.speed - attacker.movementPenalty.value)
    }
    if (!ignoresSlowing) {
        val isInCoolingArea =
            state.activeSpellEffects.any { effect ->
                effect.spell == SpellType.COOLING_SPELL &&
                    effect.position != null &&
                    currentPos.hexDistanceTo(effect.position) <= 2
            }
        if (isInCoolingArea) {
            effectiveSpeed = maxOf(0, effectiveSpeed - 1)
        }
    }
    return effectiveSpeed
}
