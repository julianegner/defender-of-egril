package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.config.GameLogBuffer
import de.egril.defender.editor.TileType
import de.egril.defender.model.Attacker
import de.egril.defender.model.Barricade
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EnemyDeathEffect
import de.egril.defender.model.FiefType
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.Raft
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.MIN_ROTATABLE_REGION_TILES
import de.egril.defender.model.RegionMove
import de.egril.defender.model.Bridge
import de.egril.defender.model.Portal
import de.egril.defender.model.Waypoint
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

    /**
     * Activate [zoneId]. Returns false when the zone does not exist, is already active, or is a
     * movable region (those are moved with [moveZone] instead).
     */
    fun applyZone(zoneId: String): Boolean {
        val zone = state.level.tileZones.firstOrNull { it.id == zoneId } ?: return false
        if (zone.movable || isZoneActive(zoneId)) return false
        state.activeTileZoneIds.add(zoneId)
        GameLogBuffer.log("EVENT", "Tile zone '$zoneId' applied (${zone.tiles.size} tiles)")
        refreshAndResolve(zone.tiles.keys)
        return true
    }

    /** Deactivate [zoneId]. Returns false when the zone does not exist or is not active. */
    fun revertZone(zoneId: String): Boolean {
        val zone = state.level.tileZones.firstOrNull { it.id == zoneId } ?: return false
        if (!isZoneActive(zoneId)) return false
        state.activeTileZoneIds.remove(zoneId)
        GameLogBuffer.log("EVENT", "Tile zone '$zoneId' reverted (${zone.tiles.size} tiles)")
        refreshAndResolve(zone.tiles.keys)
        return true
    }

    fun toggleZone(zoneId: String): Boolean = if (isZoneActive(zoneId)) revertZone(zoneId) else applyZone(zoneId)

    /**
     * Re-apply the tiles of all active zones without resolving object consequences. Used after
     * loading a save game, whose objects already reflect the zone state.
     */
    fun restoreActiveZones() {
        val positions =
            state.level.tileZones
                .filter { isZoneActive(it.id) }
                .flatMap { it.tiles.keys }
        val movedRegions = state.movedTileRegions.values
        state.refreshZoneTiles(positions + movedRegions.flatMap { it.keys })
        // A save stores moved regions at their current tiles while the level still describes the home tiles.
        relocateLevelData(movedRegions.flatMap { region -> region.entries.map { (position, tile) -> tile.homePosition to position } }.toMap())
    }

    /**
     * Move the movable region [zoneId] by [move], carrying its terrain and everything standing on it
     * to the new tiles. Objects that do not belong to the region on the destination tiles are
     * destroyed. The vacated tiles fall back to the base map. Flooding, drying and no-play rules then
     * apply to the moved objects as they would to a zone switch.
     *
     * Returns false and changes nothing when the zone is not movable, the move is a no-op, or the
     * destination leaves the map, lands on a spawn point or overlaps another movable region.
     */
    fun moveZone(
        zoneId: String,
        move: RegionMove,
    ): Boolean {
        val zone = state.level.tileZones.firstOrNull { it.id == zoneId && it.movable } ?: return false
        if (move.isIdentity) return false
        val region = state.regionTilesOf(zone)
        if (region.isEmpty()) return false

        val oldPositions = region.keys
        val pivot = state.movedTileRegionPivots[zoneId] ?: RegionMove.centreOf(oldPositions)
        val mapper = move.mapperFor(oldPositions, pivot)
        val moved = oldPositions.associateWith(mapper)
        val destination = moved.values.toSet()
        val rejection =
            when {
                move.rotationSteps.mod(6) != 0 && region.size < MIN_ROTATABLE_REGION_TILES ->
                    "rotation needs at least $MIN_ROTATABLE_REGION_TILES tiles"
                destination.any { !isOnMap(it) } -> "leaves the map"
                destination.any { it in state.level.startPositions } -> "lands on a spawn point"
                state.level.tileZones.any { other ->
                    other.movable && other.id != zoneId && state.regionTilesOf(other).keys.any { it in destination }
                } -> "overlaps another movable region"
                else -> null
            }
        if (rejection != null) {
            GameLogBuffer.log("EVENT", "Tile zone '$zoneId' not moved: destination $rejection")
            return false
        }

        destroyAt(destination - oldPositions, spareFliers = false)
        moveObjects(oldPositions, mapper)

        state.movedTileRegions[zoneId] = moved.entries.associate { (from, to) -> to to region.getValue(from) }
        state.movedTileRegionPivots[zoneId] = mapper(pivot)
        val (changes, maelstroms) = refreshTiles(oldPositions + destination)
        relocateLevelData(moved)
        GameLogBuffer.log("EVENT", "Tile zone '$zoneId' moved (${zone.tiles.size} tiles, ${move.rotationSteps * 60} deg, dx=${move.dx}, dy=${move.dy})")
        resolveChanges(changes, maelstroms)
        return true
    }

    private fun isOnMap(position: Position): Boolean =
        position.x in 0 until state.level.gridWidth && position.y in 0 until state.level.gridHeight

    /**
     * Re-map the level data that refers to tile positions — waypoints, the SINGLE_HIT order and target
     * info — for the tiles in [relocation] (old position -> new position). Must run after the tiles
     * were refreshed, since [GameState.replaceTiles] resets target info of changed tiles.
     */
    private fun relocateLevelData(relocation: Map<Position, Position>) {
        if (relocation.isEmpty()) return
        fun Position.relocated(): Position = relocation[this] ?: this

        val targetInfo =
            state.level.targetInfoMap
                .filterKeys { it in relocation }
                .mapKeys { relocation.getValue(it.key) }
        state.level =
            state.level.copy(
                waypoints =
                    state.level.waypoints.map { waypoint: Waypoint ->
                        Waypoint(waypoint.position.relocated(), waypoint.nextTarget.relocated())
                    },
                singleHitTargetOrder = state.level.singleHitTargetOrder.map { it.relocated() },
                targetInfoMap =
                    state.level.targetInfoMap.filterKeys { it !in relocation && it !in relocation.values } + targetInfo,
            )
    }

    /** Carry every object standing on the region [region] to its mapped position. */
    private fun moveObjects(
        region: Set<Position>,
        mapper: (Position) -> Position,
    ) {
        fun Position.moved(): Position = if (this in region) mapper(this) else this
        fun Position?.isInRegion(): Boolean = this != null && this in region

        for (attacker in state.attackers + state.submergedAttackers) {
            if (attacker.position.value in region) attacker.position.value = mapper(attacker.position.value)
            attacker.currentTarget?.let { target -> if (target.value in region) target.value = mapper(target.value) }
        }
        for (defender in state.defenders) {
            if (defender.position.value in region) defender.position.value = mapper(defender.position.value)
        }
        for (raft in state.rafts) {
            if (raft.currentPosition.value in region) raft.currentPosition.value = mapper(raft.currentPosition.value)
        }
        relocateEach(state.barricades, { it.position in region }) { it.copy(position = mapper(it.position)) }
        relocateEach(state.traps, { it.position in region }) { it.copy(position = mapper(it.position)) }
        relocateEach(state.fiefs, { it.position in region }) { it.copy(position = mapper(it.position)) }
        relocateEach(state.mushrooms, { it.position in region }) { it.copy(position = mapper(it.position)) }
        relocateEach(state.scrapPiles, { it.position in region }) { it.copy(position = mapper(it.position)) }
        relocateEach(state.fieldEffects, { it.position in region }) { it.copy(position = mapper(it.position)) }
        relocateEach(state.activeSpellEffects, { it.position.isInRegion() }) { it.copy(position = it.position?.let(mapper)) }
        relocateEach(state.bridges, { bridge -> bridge.positions.any { it in region } }) { bridge: Bridge ->
            bridge.copy(positions = bridge.positions.map { it.moved() })
        }
        relocateEach(state.activePortals, { it.entryPosition in region || it.exitPosition in region }) { portal: Portal ->
            portal.copy(entryPosition = portal.entryPosition.moved(), exitPosition = portal.exitPosition.moved())
        }
        relocateEach(state.takenTargets, { it in region }) { mapper(it) }
        relocateEach(state.destroyedMinePositions, { it in region }) { mapper(it) }
        relocateEach(state.activeAltarLinks, { it.from in region || it.to in region }) { link ->
            link.copy(from = link.from.moved(), to = link.to.moved())
        }
    }

    private fun <T> relocateEach(
        list: MutableList<T>,
        isInRegion: (T) -> Boolean,
        move: (T) -> T,
    ) {
        for (index in list.indices) {
            val item = list[index]
            if (isInRegion(item)) list[index] = move(item)
        }
    }

    private fun refreshAndResolve(positions: Collection<Position>) {
        val (changes, maelstroms) = refreshTiles(positions)
        resolveChanges(changes, maelstroms)
    }

    /**
     * Refresh [positions] from the active zones. Also reports river tiles whose flow newly became
     * [RiverFlow.MAELSTROM], since such a change is not a tile-type change and is missing from the returned map.
     */
    private fun refreshTiles(positions: Collection<Position>): Pair<Map<Position, Pair<TileType, TileType>>, Set<Position>> {
        val flowsBefore = positions.associateWith { state.level.riverTiles[it]?.flowDirection }
        val changes = state.refreshZoneTiles(positions)
        val newMaelstroms =
            positions
                .filter { position ->
                    state.level.riverTiles[position]?.flowDirection == RiverFlow.MAELSTROM &&
                        flowsBefore[position] != RiverFlow.MAELSTROM
                }.toSet()
        return changes to newMaelstroms
    }

    private fun resolveChanges(
        changes: Map<Position, Pair<TileType, TileType>>,
        maelstroms: Set<Position> = emptySet(),
    ) {
        state.fiefs.removeAll { it.position in maelstroms && it.type == FiefType.FISHER_BOAT }
        if (changes.isEmpty()) return
        val noPlay =
            changes
                .filter { (_, change) ->
                    (
                        change.first == TileType.RIVER ||
                            change.first == TileType.BUILD_AREA ||
                            change.first == TileType.PATH
                    ) &&
                        change.second == TileType.NO_PLAY
                }.keys
        if (noPlay.isNotEmpty()) destroyAt(noPlay, spareFliers = true)

        val remainingChanges = changes.filterKeys { it !in noPlay }
        val flooded = remainingChanges.filter { (_, change) -> change.first != TileType.RIVER && change.second == TileType.RIVER }.keys
        val dried = remainingChanges.filter { (_, change) -> change.first == TileType.RIVER && change.second != TileType.RIVER }.keys
        if (flooded.isNotEmpty()) flood(flooded)
        if (dried.isNotEmpty()) dry(dried)
        resurfaceSubmergedUnits()
    }

    /**
     * Destroy everything standing on [positions]. Flying and hovering enemies survive unless
     * [spareFliers] is false.
     */
    private fun destroyAt(
        positions: Set<Position>,
        spareFliers: Boolean,
    ) {
        fun Attacker.survives(): Boolean = spareFliers && canFlyOrHover()

        for (attacker in state.attackers.toList()) {
            if (attacker.isDefeated.value || attacker.position.value !in positions || attacker.survives()) continue
            removeWithoutReward(attacker, "destroyed by terrain")
        }
        for (attacker in state.submergedAttackers.toList()) {
            if (attacker.position.value !in positions || attacker.survives()) continue
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
        state.fiefs.removeAll { it.position in positions && it.type == FiefType.FISHER_BOAT }

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
            state.barricades.none { it.position == position } &&
            !state.isInactiveGargoyleAt(position)

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

    private fun Attacker.canStayOnWater(): Boolean = type.canTraverseRiver || type.canOnlyMoveOnWater || type.canFlyOverTerrain || type.isDragon

    private fun Attacker.canFlyOrHover(): Boolean = type.canFlyOverTerrain || type.isDragon
}
