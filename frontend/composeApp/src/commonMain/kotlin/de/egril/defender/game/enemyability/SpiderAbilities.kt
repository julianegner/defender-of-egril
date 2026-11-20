package de.egril.defender.game

import de.egril.defender.model.*

/**
 * Araxxa's spider web and spiderling summons.
 */
internal class SpiderAbilities(
    private val state: GameState,
    private val swarm: SwarmAbilities,
    private val isTileOccupiedByStaticObject: (Position) -> Boolean,
    private val maxSwarmSpawnSearchRings: Int,
) {
    /**
     * Araxxa spreads a persistent spider web that grows by one tile per enemy turn and always
     * remains under the villain herself.
     */
    fun handleAraxxaWeb(araxxa: Attacker) {
        val araxxaPosition = araxxa.position.value
        val turnStartPosition = state.enemyTurnStartPositions[araxxa.id]
        val didMoveThisTurn = turnStartPosition != null && turnStartPosition != araxxaPosition

        val webPositions = mutableSetOf<Position>()
        webPositions.add(araxxaPosition)
        webPositions.addAll(
            araxxaPosition.getHexNeighbors().filter { state.level.isEnemyTraversable(it) },
        )

        if (!didMoveThisTurn) {
            webPositions.addAll(
                araxxaPosition
                    .getHexNeighborsWithinRadius(2, state.level.gridWidth, state.level.gridHeight)
                    .filter { it.hexDistanceTo(araxxaPosition) == 2 && state.level.isEnemyTraversable(it) },
            )
        }

        for (position in webPositions) {
            if (isTileOccupiedByStaticObject(position)) continue
            refreshSpiderWebAt(position)
        }
    }

    /**
     * Araxxa summons spiderlings on adjacent enemy-traversable tiles. Spiderlings are swarm units
     * and may share tiles with other spiderlings, just like snotlings.
     */
    fun handleAraxxaSpiderlings(araxxa: Attacker) {
        val inheritedTarget =
            araxxa.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        val spawnPositions =
            swarm.resolveSwarmSpawnPositions(
                candidates = araxxa.position.value.getHexNeighbors(),
                stackableType = AttackerType.SPIDERLING,
                isBaseTraversable = { state.level.isEnemyTraversable(it) },
                maxSearchRings = maxSwarmSpawnSearchRings,
            )

        if (spawnPositions.isEmpty()) return

        for (spawnPos in spawnPositions) {
            swarm.summonSwarmUnit(
                type = AttackerType.SPIDERLING,
                spawnPos = spawnPos,
                level = araxxa.level.value,
                currentTarget = inheritedTarget,
            )
            state.enemySpawnEffects.add(
                EnemySpawnEffect(
                    position = spawnPos,
                    turnNumber = state.turnNumber.value,
                    attackerType = AttackerType.SPIDERLING,
                    suppressPortalAnimation = true,
                ),
            )
        }
    }

    private fun refreshSpiderWebAt(position: Position) {
        val existingEffect =
            state.fieldEffects.find {
                it.type == FieldEffectType.WEB && it.position == position
            }
        if (existingEffect != null) {
            existingEffect.turnsRemaining = WEB_DURATION_TURNS
            return
        }

        state.fieldEffects.add(
            FieldEffect(
                position = position,
                type = FieldEffectType.WEB,
                damage = 0,
                turnsRemaining = WEB_DURATION_TURNS,
                defenderId = 0,
            ),
        )
    }

    fun applySpiderWebBonuses() {
        val webPositions =
            state.fieldEffects
                .filter { it.type == FieldEffectType.WEB }
                .mapTo(mutableSetOf()) { it.position }

        for (attacker in state.attackers) {
            if (attacker.isDefeated.value || !attacker.type.isSpider()) continue
            if (attacker.position.value in webPositions) {
                attacker.speedBonus.value = maxOf(attacker.speedBonus.value, SPIDER_WEB_SPEED_BONUS)
            }
        }
    }

    private companion object {
        const val WEB_DURATION_TURNS = 10
        const val SPIDER_WEB_SPEED_BONUS = 1
    }
}
