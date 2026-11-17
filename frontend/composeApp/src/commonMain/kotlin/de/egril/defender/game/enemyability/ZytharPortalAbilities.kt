package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*

/**
 * Zythar the Riftcaller's demonling scouts and the rift portals they open. Extracted from
 * [EnemyAbilitySystem].
 */
internal class ZytharPortalAbilities(
    private val state: GameState,
) {
    /**
     * Spawn a single demonling scout near [zythar] on a free path tile.
     */
    internal fun spawnDemonlingNear(zythar: Attacker) {
        val summonerPos = zythar.position.value
        val possiblePositions = mutableListOf<Position>()
        possiblePositions.addAll(summonerPos.getHexNeighbors())
        for (neighbor in summonerPos.getHexNeighbors()) {
            possiblePositions.addAll(neighbor.getHexNeighbors())
        }
        val validPositions =
            possiblePositions
                .filter { pos ->
                    pos.x >= 0 &&
                        pos.x < state.level.gridWidth &&
                        pos.y >= 0 &&
                        pos.y < state.level.gridHeight &&
                        state.level.isOnPath(pos) &&
                        !state.attackers.any { it.position.value == pos && !it.isDefeated.value }
                }.distinct()
        if (validPositions.isEmpty()) return
        val spawnPos = validPositions.random()
        val inheritedTarget =
            zythar.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }
        val demonling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.DEMONLING,
                position = mutableStateOf(spawnPos),
                level = mutableStateOf(zythar.level.value),
                currentTarget = mutableStateOf(inheritedTarget),
            )
        state.attackers.add(demonling)
        state.enemySpawnEffects.add(
            EnemySpawnEffect(
                position = spawnPos,
                turnNumber = state.turnNumber.value,
                attackerType = AttackerType.DEMONLING,
                suppressPortalAnimation = true,
            ),
        )
    }

    /**
     * Called after every demonling movement step to check whether the demonling should sacrifice
     * itself to open a rift portal.
     *
     * Portal creation triggers when the demonling's distance to the nearest target is:
     * - at most [Portal.PORTAL_NEAR_TARGET_DISTANCE], OR
     * - at least [Portal.PORTAL_ADVANCE_THRESHOLD_WHEN_PORTAL_EXISTS] closer than the closest
     *   existing portal exit (or [Portal.PORTAL_ADVANCE_THRESHOLD_INITIAL] closer than Zythar
     *   when no portal exists yet).
     */
    internal fun checkAndCreatePortalForDemonling(demonling: Attacker) {
        if (demonling.isDefeated.value) return
        if (demonling.type != AttackerType.DEMONLING) return

        val demonlingPos = demonling.position.value
        val activeTargets = state.getActiveTargetPositions()
        val demonlingDist =
            activeTargets.minOfOrNull { demonlingPos.hexDistanceTo(it) } ?: return

        // Find the reference distance: min dist-to-target for all existing portal exits and Zythar
        val zythar = state.attackers.firstOrNull { !it.isDefeated.value && it.type == AttackerType.ZYTHAR_THE_RIFTCALLER }
        val portalExitDists =
            state.activePortals.map { portal ->
                activeTargets.minOfOrNull { portal.exitPosition.hexDistanceTo(it) } ?: Int.MAX_VALUE
            }
        val referenceDist: Int =
            if (zythar != null) {
                val zytharDist = activeTargets.minOfOrNull { zythar.position.value.hexDistanceTo(it) } ?: Int.MAX_VALUE
                (portalExitDists + listOf(zytharDist)).min()
            } else if (portalExitDists.isNotEmpty()) {
                portalExitDists.min()
            } else {
                return // No Zythar and no portals — cannot create new portal
            }
        val advanceThreshold =
            if (portalExitDists.isNotEmpty()) {
                Portal.PORTAL_ADVANCE_THRESHOLD_WHEN_PORTAL_EXISTS
            } else {
                Portal.PORTAL_ADVANCE_THRESHOLD_INITIAL
            }

        val shouldCreatePortal =
            demonlingDist <= Portal.PORTAL_NEAR_TARGET_DISTANCE ||
                demonlingDist <= referenceDist - advanceThreshold

        if (!shouldCreatePortal) return

        // Find a free path tile adjacent to Zythar for the portal entry
        // (not a spawn point, not a target tile, and no existing portal entry there)
        val zytharPos = zythar?.position?.value ?: return
        val entryPosition =
            zytharPos.getHexNeighbors().firstOrNull { candidate ->
                state.level.isOnPath(candidate) &&
                    !state.level.isSpawnPoint(candidate) &&
                    !state.level.isTargetPosition(candidate) &&
                    !state.isPortalTile(candidate) &&
                    !state.attackers.any { it.position.value == candidate && !it.isDefeated.value }
            } ?: return

        // The exit must also not be on a spawn point or target tile
        if (state.level.isSpawnPoint(demonlingPos) || state.level.isTargetPosition(demonlingPos) || state.isPortalTile(demonlingPos)) return

        // Create the portal
        val portalId = state.nextPortalId.value++
        val portal =
            Portal(
                id = portalId,
                entryPosition = entryPosition,
                exitPosition = demonlingPos,
                villainId = zythar.id,
                runeIndex = portalId % Portal.RUNE_POOL_SIZE,
            )
        state.activePortals.add(portal)

        // The demonling is consumed by the ritual
        demonling.wasMerged.value = true // suppress reward
        demonling.isDefeated.value = true
    }
}
