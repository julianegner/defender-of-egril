package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.config.GameLogBuffer
import de.egril.defender.config.LogConfig
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DragonNames
import de.egril.defender.model.EnemySpawnEffect
import de.egril.defender.model.GameMessage
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.isUniqueEnemyAlreadyPresent
import de.egril.defender.model.isRealVillain
import de.egril.defender.model.isSwarmUnit

class SpawnLogic(
    private val state: GameState,
    private val enemyMovement: EnemyMovementSystem,
    private val dragonLogic: DragonLogic,
) {
    fun findClosestTargetPosition(from: Position): Position {
        val active = state.getActiveTargetPositions()
        return (if (active.isNotEmpty()) active else state.level.targetPositions)
            .minByOrNull { from.distanceTo(it) } ?: state.level.targetPositions.firstOrNull() ?: from
    }

    fun processSoulCallResurrections() {
        val dueResurrections = state.pendingSoulCalls.filter { it.reviveTurn <= state.turnNumber.value }
        if (dueResurrections.isEmpty()) return

        val reservedPositions = mutableSetOf<Position>()
        for (pending in dueResurrections) {
            val spawnPos =
                if (canSpawnSoulCallAt(pending.position, reservedPositions)) {
                    pending.position
                } else {
                    enemyMovement.findFreePositionNear(
                        pending.position,
                        waterOnly = pending.attackerType.canOnlyMoveOnWater,
                        canUseRiver = pending.attackerType.canTraverseRiver,
                    )
                } ?: continue

            reservedPositions.add(spawnPos)
            val currentTarget = pending.currentTarget ?: enemyMovement.getInitialTarget(spawnPos)
            val attacker =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = pending.attackerType,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(pending.level),
                    dragonName = pending.dragonName,
                    currentTarget = mutableStateOf(currentTarget),
                )
            dragonLogic.applyDragonLevelChangeCallback(attacker)
            state.attackers.add(attacker)
            state.enemySpawnEffects.add(
                EnemySpawnEffect(
                    position = spawnPos,
                    turnNumber = state.turnNumber.value,
                    attackerType = pending.attackerType,
                    suppressPortalAnimation = pending.attackerType == AttackerType.SKELETON || pending.attackerType.isSwarmUnit(),
                ),
            )
        }

        state.pendingSoulCalls.removeAll(dueResurrections.toSet())
    }

    fun spawnInitialEnemies() {
        val turn1Spawns = state.spawnPlan.filter { it.spawnTurn == 1 }

        turn1Spawns.forEachIndexed { index, plannedSpawn ->
            val preferredSpawnPoint =
                plannedSpawn.spawnPoint
                    ?: run {
                        val compatiblePoints = state.level.getCompatibleSpawnPoints(plannedSpawn.attackerType)
                        compatiblePoints[index % compatiblePoints.size]
                    }

            val spawnPos =
                enemyMovement.findFreePositionNear(
                    preferredSpawnPoint,
                    waterOnly = plannedSpawn.attackerType.canOnlyMoveOnWater,
                    canUseRiver = plannedSpawn.attackerType.canTraverseRiver,
                )

            if (spawnPos == null) {
                return@forEachIndexed
            }

            if (isUniqueEnemyAlreadyPresent(plannedSpawn.attackerType, state.attackers)) {
                return@forEachIndexed
            }

            val initialTarget = enemyMovement.getInitialTarget(preferredSpawnPoint)
            val attacker =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = plannedSpawn.attackerType,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(plannedSpawn.level),
                    currentTarget = mutableStateOf(initialTarget),
                )
            state.attackers.add(attacker)
            GameLogBuffer.log("SPAWN", "${attacker.type} Lv${attacker.level.value} spawned at $spawnPos")
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println(
                    "Spawned attacker ${attacker.id} at $spawnPos (preferred: $preferredSpawnPoint) with initial target: $initialTarget",
                )
            }

            if (plannedSpawn.attackerType == AttackerType.EWHAD) {
                state.pendingMessages.add(
                    GameMessage(type = GameMessageType.EWHAD_ENTERS),
                )
            }

            if (plannedSpawn.attackerType.isRealVillain && plannedSpawn.attackerType != AttackerType.EWHAD) {
                state.pendingMessages.add(
                    GameMessage(type = GameMessageType.VILLAIN_ENTERS, name = plannedSpawn.attackerType.name),
                )
            }
        }

        enemyMovement.moveGoblinsAfterSpawn()
    }

    fun spawnEnemy(
        type: AttackerType,
        level: Int = 1,
        preferredSpawnPoint: Position? = null,
    ) {
        val spawnPos =
            if (preferredSpawnPoint != null) {
                enemyMovement.findFreePositionNear(
                    preferredSpawnPoint,
                    waterOnly = type.canOnlyMoveOnWater,
                    canUseRiver = type.canTraverseRiver,
                )
            } else {
                enemyMovement.findFreeSpawnPosition()
            } ?: return

        val scaledHealth = type.health * level
        val attacker =
            Attacker(
                id = state.nextAttackerId.value++,
                type = type,
                position = mutableStateOf(spawnPos),
                currentHealth = mutableStateOf(scaledHealth),
                dragonName = if (type.isDragon) DragonNames.getRandomName() else null,
                currentTarget = mutableStateOf(enemyMovement.getInitialTarget(spawnPos)),
            )

        dragonLogic.applyDragonLevelChangeCallback(attacker)
        state.attackers.add(attacker)

        if (type == AttackerType.GOBLIN) {
            enemyMovement.moveGoblinsAfterSpawn()
        }
    }

    private fun canSpawnSoulCallAt(
        position: Position,
        reservedPositions: Set<Position>,
    ): Boolean =
        position.x >= 0 &&
            position.x < state.level.gridWidth &&
            position.y >= 0 &&
            position.y < state.level.gridHeight &&
            position !in reservedPositions &&
            state.level.isEnemyTraversable(position) &&
            !state.isPortalTile(position) &&
            !isOccupiedByStaticObject(position) &&
            state.attackers.none { !it.isDefeated.value && it.position.value == position }

    private fun isOccupiedByStaticObject(position: Position): Boolean =
        state.defenders.any { it.position.value == position } ||
            state.barricades.any { it.position == position && !it.isDestroyed() } ||
            state.traps.any { it.position == position } ||
            state.fiefs.any { it.position == position } ||
            state.mushrooms.any { it.position == position }
}
