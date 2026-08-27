package de.egril.defender.game.gameengine

import de.egril.defender.game.CombatSystem
import de.egril.defender.model.AttackType
import de.egril.defender.model.Defender
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Position

class GameEngineAutoAttackLogic(
    private val state: GameState,
    private val selector: GameEngineAutoAttackSelector,
    private val combatSystem: CombatSystem,
    private val evaluateImmediateEvents: () -> Unit,
) {
    fun autoDefenderAttacks() {
        if (state.phase.value != GamePhase.PLAYER_TURN) return
        if (state.attackers.none { !it.isDefeated.value && !it.isBuildingBridge.value }) return

        for (defender in state.defenders) {
            if (!defender.isReady) continue
            if (defender.actionsRemaining.value <= 0) continue
            if (defender.isDisabled.value) continue
            if (defender.type.attackType == AttackType.NONE) continue

            while (defender.actionsRemaining.value > 0) {
                val activeAttackers =
                    state.attackers.filter { attacker ->
                        !attacker.isDefeated.value && !attacker.isBuildingBridge.value
                    }
                if (activeAttackers.isEmpty()) return

                when (defender.type.attackType) {
                    AttackType.MELEE, AttackType.RANGED -> {
                        val target = selector.selectAutoTargetForDefender(defender, activeAttackers) ?: break
                        val success =
                            combatSystem.defenderAttack(defender.id, target.id) {
                                combatSystem.processDefeatedAttackers()
                            }
                        if (!success) break
                    }
                    AttackType.AREA, AttackType.LASTING -> {
                        val targetPosition = selector.selectBestAreaAttackPosition(defender, activeAttackers) ?: break
                        val success =
                            combatSystem.defenderAttackPosition(defender.id, targetPosition) {
                                combatSystem.processDefeatedAttackers()
                            }
                        if (!success) break
                    }
                    AttackType.NONE -> break
                }
            }
        }
        evaluateImmediateEvents()
    }

    fun getNextAutoAttackTargetPosition(defender: Defender): Position? {
        if (defender.actionsRemaining.value <= 0 || defender.isDisabled.value) return null
        val activeAttackers = state.attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value }
        if (activeAttackers.isEmpty()) return null
        return when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED ->
                selector.selectAutoTargetForDefender(defender, activeAttackers)?.position?.value
            AttackType.AREA, AttackType.LASTING ->
                selector.selectBestAreaAttackPosition(defender, activeAttackers)
            AttackType.NONE -> null
        }
    }

    fun performOneAutoAttack(defenderId: Int): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        if (!defender.isReady || defender.actionsRemaining.value <= 0 || defender.isDisabled.value) return false
        if (defender.type.attackType == AttackType.NONE) return false
        val activeAttackers = state.attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value }
        if (activeAttackers.isEmpty()) return false
        return when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> {
                val target = selector.selectAutoTargetForDefender(defender, activeAttackers) ?: return false
                combatSystem
                    .defenderAttack(defender.id, target.id) { combatSystem.processDefeatedAttackers() }
                    .also { if (it) evaluateImmediateEvents() }
            }
            AttackType.AREA, AttackType.LASTING -> {
                val targetPosition = selector.selectBestAreaAttackPosition(defender, activeAttackers) ?: return false
                combatSystem
                    .defenderAttackPosition(defender.id, targetPosition) { combatSystem.processDefeatedAttackers() }
                    .also { if (it) evaluateImmediateEvents() }
            }
            AttackType.NONE -> false
        }
    }
}
