package de.egril.defender.game.gameengine

import de.egril.defender.game.BarricadeSystem
import de.egril.defender.game.MineOperations
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.effectiveLevel

class GameEngineBarricadeLogic(
    private val state: GameState,
    private val barricadeSystem: BarricadeSystem,
    private val mineOperations: MineOperations,
    private val applyTargetDamage: (Attacker) -> Unit,
    private val destroyFiefAt: (Position, Attacker) -> Unit,
    private val consumeMushroomAt: (Position, Attacker) -> Unit,
) {
    fun attackBarricade(
        newPosition: Position,
        attacker: Attacker,
    ): Boolean {
        val barricadeAtPosition = barricadeSystem.getBarricadeAt(newPosition)
        val isFlying = attacker.isFlying.value

        if (barricadeAtPosition != null && !barricadeAtPosition.isDestroyed() && !isFlying) {
            println(
                "Attack barricade: Attacker ${attacker.id} (${attacker.type}) at ${attacker.position.value} attacks barricade at $newPosition (HP: ${barricadeAtPosition.healthPoints.value})",
            )
            val damage = getBarricadeDamageForEnemyUnit(attacker)
            val wasDestroyed = barricadeSystem.handleEnemyAttackBarricade(attacker, barricadeAtPosition, damage)
            if (wasDestroyed) {
                attacker.position.value = newPosition
                mineOperations.checkAndActivateTrapForAttacker(attacker)
                destroyFiefAt(newPosition, attacker)
                consumeMushroomAt(newPosition, attacker)

                if (!attacker.isDefeated.value) {
                    if (state.level.isWaypoint(newPosition) && attacker.currentTarget?.value == newPosition) {
                        val waypoint = state.level.getWaypointAt(newPosition)
                        if (waypoint != null) {
                            attacker.currentTarget.value = state.resolveWaypointNextTarget(waypoint.nextTarget, newPosition)
                        }
                    }
                    if (state.isActiveTargetPosition(newPosition)) {
                        applyTargetDamage(attacker)
                    }
                }
            }
            return true
        }
        return false
    }

    fun getBarricadeDamageForEnemyUnit(attacker: Attacker): Int {
        val baseDamage =
            when {
                attacker.type == AttackerType.SNOTLING || attacker.type == AttackerType.SPIDERLING ->
                    maxOf(1, attacker.currentHealth.value / 5)
                attacker.type.isDragon -> attacker.effectiveLevel * 5
                else -> attacker.effectiveLevel
            }
        val frenzyMultiplier =
            if (state.waaghFrenzyActive.value && attacker.type in setOf(AttackerType.ORK, AttackerType.OGRE)) 2 else 1
        return baseDamage * attacker.type.barricadeDamageMultiplier * frenzyMultiplier
    }
}
