package de.egril.defender.game

import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameMessage
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.GameState
import de.egril.defender.model.hexDistanceTo

class WaaghLogic(
    private val state: GameState,
) {
    fun applyOrkFrenzyTowerAttack(attacker: Attacker) {
        if (!(state.waaghFrenzyActive.value && attacker.type == AttackerType.ORK)) return

        state.defenders
            .filter { defender ->
                defender.isReady &&
                    attacker.position.value.hexDistanceTo(defender.position.value) == 1
            }.forEach { defender ->
                defender.isDisabled.value = true
                defender.disabledTurnsRemaining.value = maxOf(defender.disabledTurnsRemaining.value, 2)
            }
    }

    fun updateWaaghFrenzyAtEnemyTurnStart() {
        if (!state.level.waaghEnabled) return
        if (!state.waaghFrenzyActive.value && state.waaghPoints.value >= 100) {
            state.waaghFrenzyActive.value = true
            state.waaghFrenzyRoundsLeft.value = 2
            if (!state.hasShownWaaghFrenzyMessage.value) {
                state.pendingMessages.add(GameMessage(type = GameMessageType.WAAAGH_FRENZY))
                state.hasShownWaaghFrenzyMessage.value = true
            }
        }
    }

    fun updateWaaghFrenzyAtEnemyTurnEnd() {
        if (!state.level.waaghEnabled) return
        if (!state.waaghFrenzyActive.value) return
        state.waaghFrenzyRoundsLeft.value--
        if (state.waaghFrenzyRoundsLeft.value <= 0) {
            state.waaghFrenzyActive.value = false
            state.waaghFrenzyRoundsLeft.value = 0
            state.waaghPoints.value = 0
        }
    }

    fun getBarricadeFrenzyMultiplier(attacker: Attacker): Int =
        if (state.waaghFrenzyActive.value && attacker.type in setOf(AttackerType.ORK, AttackerType.OGRE)) 2 else 1
}
