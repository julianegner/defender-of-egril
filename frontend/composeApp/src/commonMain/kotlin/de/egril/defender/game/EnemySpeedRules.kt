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
    val goblinWaaghActive = state.level.waaghEnabled && state.waaghFrenzyActive.value && attacker.type == AttackerType.GOBLIN
    val goblinRunnerWaaghActive = state.level.waaghEnabled && state.waaghFrenzyActive.value && attacker.type == AttackerType.GOBLIN_RUNNER
    val baseSpeed =
        if (goblinWaaghActive) {
            attacker.type.speed * 2
        } else {
            var speed =
                if (goblinRunnerWaaghActive) {
                    maxOf(1, attacker.currentBaseMovementSpeed + attacker.type.speed - attacker.movementPenalty.value)
                } else {
                    maxOf(1, attacker.currentBaseMovementSpeed - attacker.movementPenalty.value)
                }
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
        effectiveSpeed *= 2
    }
    if (!goblinWaaghActive) {
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
