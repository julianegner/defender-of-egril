package de.egril.defender.game

import de.egril.defender.config.GameLogBuffer
import de.egril.defender.config.LogConfig
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Fief
import de.egril.defender.model.FiefType
import de.egril.defender.model.GameMessage
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.TargetType
import de.egril.defender.model.canEatMushroom
import de.egril.defender.model.displayLevel
import de.egril.defender.model.isRealVillain

class SupportLogic(
    private val state: GameState,
) {
    fun placeSupportFief(
        position: Position,
        type: FiefType,
    ): Boolean {
        if (!state.level.isOnPath(position)) return false
        if (type == FiefType.FISHER && !state.level.hasAdjacentWaterTile(position)) return false
        val hasEnemy = state.attackers.any { !it.isDefeated.value && it.position.value == position }
        val hasTrap = state.traps.any { it.position == position }
        val hasBarricade = state.barricades.any { it.position == position }
        val hasFief = state.fiefs.any { it.position == position }
        val hasMushroom = state.mushrooms.any { it.position == position }
        if (hasEnemy || hasTrap || hasBarricade || hasFief || hasMushroom) return false
        state.fiefs.add(Fief(position = position, type = type))
        return true
    }

    fun performWizardGenerateMana(wizardId: Int): Boolean {
        val wizard = state.defenders.find { it.id == wizardId } ?: return false
        if (wizard.type != DefenderType.WIZARD_TOWER) return false
        if (wizard.actionsRemaining.value <= 0) return false
        if (state.currentMana.value >= state.maxMana.value) return false

        val manaAmount = 5 + (wizard.level.value / 5)
        val newMana = minOf(state.currentMana.value + manaAmount, state.maxMana.value)
        val actualManaGenerated = newMana - state.currentMana.value
        state.currentMana.value = newMana
        wizard.actionsRemaining.value -= 1

        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println(
                "=== SPELL: Wizard tower $wizardId generated $actualManaGenerated mana (${state.currentMana.value}/${state.maxMana.value})",
            )
        }

        return true
    }

    fun applyTargetDamage(attacker: Attacker) {
        val position = attacker.position.value
        val targetInfo = state.level.targetInfoMap[position]

        if (targetInfo?.type == TargetType.SINGLE_HIT) {
            if (!state.takenTargets.contains(position)) {
                state.takenTargets.add(position)
                val name = targetInfo.name.takeIf { it.isNotBlank() }
                GameLogBuffer.log("DAMAGE", "Target '${name ?: position}' taken by ${attacker.type} Lv${attacker.displayLevel}")
                state.pendingMessages.add(
                    GameMessage(
                        type = GameMessageType.TARGET_TAKEN,
                        name = name,
                    ),
                )
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println(
                        "!!! SINGLE_HIT TARGET TAKEN !!! Turn ${state.turnNumber.value}: ${attacker.type} (ID ${attacker.id}) took target '${name ?: position}'",
                    )
                }
                state.retargetEnemiesFromTakenTarget(position)
            }
        } else {
            val damage = attacker.calculateTargetDamage()
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println(
                    "!!! ENEMY ENTERED TARGET !!! Turn ${state.turnNumber.value}: ${attacker.type} (ID ${attacker.id}) at $position dealt $damage damage. HP: ${state.healthPoints.value} -> ${state.healthPoints.value - damage}",
                )
            }
            GameLogBuffer.log(
                "DAMAGE",
                "${attacker.type} Lv${attacker.displayLevel} reached target — dealt $damage damage (HP: ${state.healthPoints.value} -> ${state.healthPoints.value - damage})",
            )
            state.healthPoints.value = maxOf(0, state.healthPoints.value - damage)
        }
        if (attacker.type.isRealVillain) {
            state.villainReachedTarget.value = true
        }
        attacker.isDefeated.value = true
    }

    fun addCoins(amount: Int) {
        state.coins.value += amount
    }

    fun setCoins(amount: Int) {
        state.coins.value = amount
    }

    fun addMana(amount: Int) {
        state.currentMana.value = minOf(state.maxMana.value, state.currentMana.value + amount)
    }

    fun removeMana(amount: Int) {
        state.currentMana.value = maxOf(0, state.currentMana.value - amount)
    }

    fun destroyFiefAt(
        position: Position,
        attacker: Attacker? = null,
    ) {
        val fief = state.fiefs.find { it.position == position }
        if (fief != null) {
            state.fiefs.remove(fief)
            if (attacker?.type == AttackerType.CAPTAIN_RODERICH) {
                attacker.treasureCoins.value += fief.type.incomePerTurn * 10
            }
        }
    }

    fun consumeMushroomAt(
        position: Position,
        attacker: Attacker,
    ) {
        if (!attacker.type.canEatMushroom) return
        if (attacker.mushroomTurnsRemaining.value > 0) return
        val mushroom = state.mushrooms.find { it.position == position }
        if (mushroom != null) {
            state.mushrooms.remove(mushroom)
            attacker.mushroomLevelBonus.value = attacker.level.value
            attacker.mushroomTurnsRemaining.value = 2
            attacker.currentHealth.value += attacker.mushroomBonusHealth
        }
    }
}
