package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.config.GameLogBuffer
import de.egril.defender.editor.TileType
import de.egril.defender.model.Attacker
import de.egril.defender.model.Barricade
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EnemyDeathEffect
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.Raft
import de.egril.defender.model.getHexNeighbors

/**
 * Switches [de.egril.defender.model.TileZone]s on and off at runtime and resolves the consequences
 * for everything standing on the changed tiles.
 *
 * When a path, build area, or river becomes NO_PLAY, everything on it is destroyed except enemies
 * that can fly or hover.
 *
 * Flooding (tile becomes river):
 * - Units that can neither swim, fly nor hover drown (removed without reward, not counted as kills),
 *   except units that [survive submersion][de.egril.defender.model.AttackerType.survivesSubmersion],
 *   which sink to the river bed (see [GameState.submergedAttackers]) until the water recedes.
 * - Traps, fiefs and mushrooms are destroyed; barricades without a tower are washed away.
 * - A tower standing on the tile (e.g. on a barricade tower base) is lifted onto a raft; the raft
 *   keeps the barricade's health (capped at [Raft.RAFT_MAX_HEALTH]). Dwarven mines cannot float and
 *   are destroyed.
 *
 * Drying (river tile becomes land):
 * - Water-only units strand and die (no reward).
 * - Bridges on the tile are removed.
 * - Stranded rafts become barricades carrying their tower. The barricade gets the raft's health
 *   minus [Raft.STRANDING_DAMAGE]; below 100 health it cannot carry the tower, which is destroyed
 *   without refund. A barricade with no health left is not created.
 * - Submerged units whose tile is dry again re-surface.
 */
class TileZoneSystem(
    private val state: GameState,
) {
    fun isZoneActive(zoneId: String): Boolean = state.activeTileZoneIds.contains(zoneId)

    /** Activate [zoneId]. Returns false when the zone does not exist or is already active. */
    fun applyZone(zoneId: String): Boolean {
        val zone = state.level.tileZones.firstOrNull { it.id == zoneId } ?: return false
        if (isZoneActive(zoneId)) return false
        state.activeTileZoneIds.add(zoneId)
        GameLogBuffer.log("EVENT", "Tile zone '$zoneId' applied (${zone.tiles.size} tiles)")
        resolveChanges(state.refreshZoneTiles(zone.tiles.keys))
        return true
    }

    /** Deactivate [zoneId]. Returns false when the zone does not exist or is not active. */
    fun revertZone(zoneId: String): Boolean {
        val zone = state.level.tileZones.firstOrNull { it.id == zoneId } ?: return false
        if (!isZoneActive(zoneId)) return false
        state.activeTileZoneIds.remove(zoneId)
        GameLogBuffer.log("EVENT", "Tile zone '$zoneId' reverted (${zone.tiles.size} tiles)")
        resolveChanges(state.refreshZoneTiles(zone.tiles.keys))
        return true
    }

    fun toggleZone(zoneId: String): Boolean = if (isZoneActive(zoneId)) revertZone(zoneId) else applyZone(zoneId)

    /**
     * Re-apply the tiles of all active zones without resolving object consequences. Used after
     * loading a save game, whose objects already reflect the zone state.
     */
    fun restoreActiveZones() {
        val positions = state.level.tileZones.filter { isZoneActive(it.id) }.flatMap { it.tiles.keys }
        state.refreshZoneTiles(positions)
    }

    private fun resolveChanges(changes: Map<Position, Pair<TileType, TileType>>) {
        if (changes.isEmpty()) return
        val noPlay =
            changes
                .filter { (_, change) ->
                    (change.first == TileType.RIVER ||
                        change.first == TileType.BUILD_AREA ||
                        change.first == TileType.PATH) &&
                        change.second == TileType.NO_PLAY
                }.keys
        if (noPlay.isNotEmpty()) destroyOnNoPlay(noPlay)

        val remainingChanges = changes.filterKeys { it !in noPlay }
        val flooded = remainingChanges.filter { (_, change) -> change.first != TileType.RIVER && change.second == TileType.RIVER }.keys
        val dried = remainingChanges.filter { (_, change) -> change.first == TileType.RIVER && change.second != TileType.RIVER }.keys
        if (flooded.isNotEmpty()) flood(flooded)
        if (dried.isNotEmpty()) dry(dried)
        resurfaceSubmergedUnits()
    }

    private fun destroyOnNoPlay(positions: Set<Position>) {
        for (attacker in state.attackers.toList()) {
            if (attacker.isDefeated.value || attacker.position.value !in positions || attacker.canFlyOrHover()) continue
            removeWithoutReward(attacker, "destroyed by terrain")
        }
        for (attacker in state.submergedAttackers.toList()) {
            if (attacker.position.value !in positions || attacker.canFlyOrHover()) continue
            state.submergedAttackers.remove(attacker)
            GameLogBuffer.log("EVENT", "${attacker.type.name} #${attacker.id} destroyed by terrain at ${attacker.position.value}")
        }

        state.traps.removeAll { it.position in positions }
        state.fiefs.removeAll { it.position in positions }
        state.mushrooms.removeAll { it.position in positions }
        state.bridges.removeAll { bridge -> bridge.positions.any { it in positions } }
        state.scrapPiles.removeAll { it.position in positions }
        state.activePortals.removeAll { it.entryPosition in positions || it.exitPosition in positions }
        state.fieldEffects.removeAll { it.position in positions }
        state.activeSpellEffects.removeAll { it.position != null && it.position in positions }

        val destroyedBarricades = state.barricades.filter { it.position in positions }
        val supportedTowerIds = destroyedBarricades.mapNotNull { it.supportedTowerId.value }.toSet()
        state.barricades.removeAll(destroyedBarricades.toSet())

        val destroyedRafts =
            state.rafts
                .filter { it.currentPosition.value in positions }
        for (raft in destroyedRafts) {
            raft.isDestroyed.value = true
            releaseKrakenGrip(raft.id)
        }
        state.rafts.removeAll(destroyedRafts.toSet())
        state.defenders.removeAll { it.position.value in positions || it.id in supportedTowerIds }
        destroyedRafts.forEach { raft ->
            state.defenders.removeAll { it.id == raft.defenderId }
        }
    }

    private fun flood(positions: Set<Position>) {
        for (attacker in state.attackers.toList()) {
            if (attacker.isDefeated.value || attacker.position.value !in positions) continue
            when {
                attacker.canStayOnWater() -> Unit
                attacker.type.survivesSubmersion -> submerge(attacker)
                else -> removeWithoutReward(attacker, "drowned")
            }
        }

        state.traps.removeAll { it.position in positions }
        state.fiefs.removeAll { it.position in positions }
        state.mushrooms.removeAll { it.position in positions }

        for (defender in state.defenders.toList()) {
            val position = defender.position.value
            if (position !in positions || defender.raftId.value != null) continue
            val baseBarricade = defender.towerBaseBarricadeId.value?.let { id -> state.barricades.firstOrNull { it.id == id } }
            if (defender.type == DefenderType.DWARVEN_MINE || defender.type == DefenderType.DRAGONS_LAIR) {
                state.defenders.remove(defender)
                GameLogBuffer.log("EVENT", "${defender.type.name} at $position destroyed by flooding")
                continue
            }
            val raftHealth = (baseBarricade?.healthPoints?.value ?: Raft.RAFT_MAX_HEALTH).coerceIn(1, Raft.RAFT_MAX_HEALTH)
            val raft =
                Raft(
                    id = state.nextRaftId.value++,
                    defenderId = defender.id,
                    currentPosition = mutableStateOf(position),
                    healthPoints = mutableStateOf(raftHealth),
                )
            state.rafts.add(raft)
            defender.raftId.value = raft.id
            defender.towerBaseBarricadeId.value = null
            GameLogBuffer.log("EVENT", "${defender.type.name} at $position lifted onto raft ${raft.id} ($raftHealth HP)")
        }

        state.barricades.removeAll { it.position in positions }
    }

    private fun dry(positions: Set<Position>) {
        for (attacker in state.attackers.toList()) {
            if (attacker.isDefeated.value || attacker.position.value !in positions) continue
            if (attacker.type.canOnlyMoveOnWater) {
                removeWithoutReward(attacker, "stranded")
            }
        }

        state.bridges.removeAll { bridge -> bridge.positions.any { it in positions } }

        for (raft in state.rafts.toList()) {
            if (!raft.isActive) continue
            val position = raft.currentPosition.value
            if (position !in positions) continue
            strandRaft(raft, position)
        }
        state.rafts.removeAll { !it.isActive }
    }

    private fun strandRaft(
        raft: Raft,
        position: Position,
    ) {
        raft.isDestroyed.value = true
        val defender = state.defenders.firstOrNull { it.id == raft.defenderId }
        releaseKrakenGrip(raft.id)
        val barricadeHealth = raft.healthPoints.value - Raft.STRANDING_DAMAGE
        val barricade =
            if (barricadeHealth >= 1) {
                Barricade(
                    id = state.nextBarricadeId.value++,
                    position = position,
                    healthPoints = mutableStateOf(barricadeHealth),
                    defenderId = -1,
                ).also { state.barricades.add(it) }
            } else {
                null
            }
        if (defender == null) return
        defender.raftId.value = null
        defender.isGrippedByKraken.value = false
        if (barricade != null && barricade.canSupportTower()) {
            barricade.supportedTowerId.value = defender.id
            defender.towerBaseBarricadeId.value = barricade.id
            GameLogBuffer.log("EVENT", "Raft ${raft.id} stranded at $position: ${defender.type.name} now on barricade ($barricadeHealth HP)")
        } else {
            state.defenders.remove(defender)
            GameLogBuffer.log("EVENT", "Raft ${raft.id} stranded at $position: ${defender.type.name} destroyed ($barricadeHealth HP left)")
        }
    }

    private fun releaseKrakenGrip(raftId: Int) {
        state.attackers.forEach { attacker ->
            if (attacker.grippedRaftId.value == raftId) {
                attacker.grippedRaftId.value = null
                attacker.bargeGripPhase.value = 0
            }
        }
    }

    private fun submerge(attacker: Attacker) {
        state.attackers.remove(attacker)
        state.submergedAttackers.add(attacker)
        GameLogBuffer.log("EVENT", "${attacker.type.name} #${attacker.id} submerged at ${attacker.position.value}")
    }

    /**
     * Let submerged units whose tile is dry again return to the surface. A unit whose tile is
     * occupied moves to the nearest free walkable neighbor; if there is none it stays submerged
     * and tries again later.
     */
    fun resurfaceSubmergedUnits() {
        for (attacker in state.submergedAttackers.toList()) {
            val position = attacker.position.value
            if (state.level.isRiverTile(position)) continue
            val target =
                if (isFreeForResurfacing(position)) {
                    position
                } else {
                    position.getHexNeighbors().firstOrNull { isFreeForResurfacing(it) }
                } ?: continue
            attacker.position.value = target
            state.submergedAttackers.remove(attacker)
            state.attackers.add(attacker)
            GameLogBuffer.log("EVENT", "${attacker.type.name} #${attacker.id} resurfaced at $target")
        }
    }

    private fun isFreeForResurfacing(position: Position): Boolean =
        position.x in 0 until state.level.gridWidth &&
            position.y in 0 until state.level.gridHeight &&
            !state.level.isRiverTile(position) &&
            state.level.isEnemyTraversable(position) &&
            state.attackers.none { !it.isDefeated.value && it.position.value == position } &&
            state.defenders.none { it.position.value == position } &&
            state.barricades.none { it.position == position }

    private fun removeWithoutReward(
        attacker: Attacker,
        reason: String,
    ) {
        state.attackers.remove(attacker)
        state.defeatedEnemyEffects.add(
            EnemyDeathEffect(
                position = attacker.position.value,
                turnNumber = state.turnNumber.value,
                attackerType = attacker.type,
                attackerLevel = attacker.level.value,
            ),
        )
        GameLogBuffer.log("EVENT", "${attacker.type.name} #${attacker.id} $reason at ${attacker.position.value}")
    }

    private fun Attacker.canStayOnWater(): Boolean =
        type.canTraverseRiver || type.canOnlyMoveOnWater || type.canFlyOverTerrain || type.isDragon

    private fun Attacker.canFlyOrHover(): Boolean = type.canFlyOverTerrain || type.isDragon
}
