package de.egril.defender.game

import de.egril.defender.config.LogConfig
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.DigOutcome
import de.egril.defender.model.DragonLevelChangeEffect
import de.egril.defender.model.GameState
import de.egril.defender.model.getHexNeighbors

class DragonLogic(
    private val state: GameState,
    private val mineOperations: MineOperations,
) {
    private var dragonLevelChangeCallback: ((oldLevel: Int, newLevel: Int) -> Unit)? = null

    fun applyDragonLevelChangeCallback(attacker: Attacker) {
        if (attacker.type.isDragon) {
            attacker.onDragonLevelChanged = dragonLevelChangeCallback
        }
    }

    fun setDragonLevelChangeCallback(callback: (oldLevel: Int, newLevel: Int) -> Unit) {
        val wrappedCallback: (oldLevel: Int, newLevel: Int) -> Unit = { oldLevel, newLevel ->
            if (oldLevel > newLevel) {
                val levelsLost = oldLevel - newLevel
                val xpPerLevel = AttackerType.DRAGON.xp
                val xpEarned = xpPerLevel * levelsLost
                state.xpEarnedThisLevel.value += xpEarned
            }
            callback(oldLevel, newLevel)
        }
        dragonLevelChangeCallback = wrappedCallback
        state.attackers.filter { it.type.isDragon }.forEach { dragon ->
            dragon.onDragonLevelChanged = wrappedCallback
        }
    }

    fun spawnDragonCheat(): Boolean {
        val mine = state.defenders.find { it.type == DefenderType.DWARVEN_MINE }
        if (mine != null) {
            mineOperations.performMineDigWithOutcome(DigOutcome.DRAGON)
            return true
        }
        return false
    }

    fun processDragonGreed(dragon: Attacker) {
        if (dragon.type != AttackerType.DRAGON || dragon.greed <= 0) return

        val dragonPos = dragon.position.value
        val neighbors = dragonPos.getHexNeighbors()
        val adjacentUnits =
            state.attackers.filter { unit ->
                unit.id != dragon.id &&
                    !unit.isDefeated.value &&
                    unit.type != AttackerType.EWHAD &&
                    neighbors.contains(unit.position.value)
            }

        val unitsToEat = adjacentUnits.sortedBy { it.currentHealth.value }.take(dragon.greed)
        for (unit in unitsToEat) {
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println(
                    "Dragon ${dragon.id} (greed ${dragon.greed}) eating adjacent ${unit.type} at ${unit.position.value}, gaining ${unit.currentHealth.value} HP",
                )
            }
            dragon.currentHealth.value += unit.currentHealth.value
            unit.isDefeated.value = true
        }

        if (unitsToEat.isNotEmpty()) {
            val oldLevel = dragon.level.value
            dragon.updateDragonLevel()
            recordDragonLevelChange(dragon, oldLevel)
        }
    }

    fun recordDragonLevelChange(
        dragon: Attacker,
        oldLevel: Int,
    ) {
        val newLevel = dragon.level.value
        if (newLevel != oldLevel) {
            state.dragonLevelChangeEffects.add(
                DragonLevelChangeEffect(
                    position = dragon.position.value,
                    isLevelUp = newLevel > oldLevel,
                    turnNumber = state.turnNumber.value,
                ),
            )
        }
    }
}
