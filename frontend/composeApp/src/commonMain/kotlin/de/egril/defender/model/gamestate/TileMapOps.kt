package de.egril.defender.model

/**
 * Tile-map mutation logic extracted from [GameState]. Each function runs with the state as receiver
 * (via `with(state)`), so it reads and writes the same fields as before. [GameState] keeps the public
 * members and delegates here, so call sites are unchanged.
 */
internal object TileMapOps {

    fun sandboxPaintTile(
        state: GameState,
        position: Position,
        tileType: de.egril.defender.editor.TileType,
        riverFlow: RiverFlow,
        riverSpeed: Int,
    ) {
        with(state) {
            if (!level.isSandbox) return
            // Never repaint an occupied tile (defender/barricade/trap) to avoid orphaning game objects.
            if (defenders.any { it.position.value == position }) return
            if (barricades.any { it.position == position }) return

            replaceTiles(mapOf(position to (tileType to RiverTile(position = position, flowDirection = riverFlow, flowSpeed = riverSpeed))))
            val targetInfoMap = level.targetInfoMap
            // Record the repaint so the map can overlay the new tile image over the original map
            // background — but only when it genuinely differs from the original map. Repainting a tile
            // back to its original type removes it from the tracked differences.
            val originalType = originalSandboxTileTypes[position] ?: de.egril.defender.editor.TileType.NO_PLAY
            val originalRiverTile = originalSandboxRiverTiles[position]
            val isSameAsOriginal =
                if (tileType == de.egril.defender.editor.TileType.RIVER) {
                    originalType == de.egril.defender.editor.TileType.RIVER &&
                        originalRiverTile != null &&
                        originalRiverTile.flowDirection == riverFlow &&
                        originalRiverTile.flowSpeed == riverSpeed
                } else if (tileType == de.egril.defender.editor.TileType.TARGET) {
                    tileType == originalType && targetInfoMap[position] == originalSandboxTargetInfoMap[position]
                } else {
                    tileType == originalType
                }
            if (isSameAsOriginal) {
                sandboxPaintedTiles.remove(position)
            } else {
                sandboxPaintedTiles[position] = tileType
            }
            // Track the chosen river flow separately so it can be persisted and restored across saves.
            if (tileType == de.egril.defender.editor.TileType.RIVER) {
                val paintedRiverTile = RiverTile(position = position, flowDirection = riverFlow, flowSpeed = riverSpeed)
                if (originalType == de.egril.defender.editor.TileType.RIVER && originalRiverTile == paintedRiverTile) {
                    sandboxPaintedRiverTiles.remove(position)
                } else {
                    sandboxPaintedRiverTiles[position] = paintedRiverTile
                }
            } else {
                sandboxPaintedRiverTiles.remove(position)
            }
            mapEditVersion.value++
        }
    }

    fun replaceTiles(
        state: GameState,
        changes: Map<Position, Pair<de.egril.defender.editor.TileType, RiverTile?>>,
    ) {
        with(state) {
            if (changes.isEmpty()) return
            val pathCells = level.pathCells.toMutableSet()
            val buildAreas = level.buildAreas.toMutableSet()
            val startPositions = level.startPositions.toMutableList()
            val targetPositions = level.targetPositions.toMutableList()
            val riverTiles = level.riverTiles.toMutableMap()
            val targetInfoMap = level.targetInfoMap.toMutableMap()

            for ((position, change) in changes) {
                val (tileType, riverTile) = change
                // Clear the tile from every collection first so the new type fully replaces the old one.
                pathCells.remove(position)
                buildAreas.remove(position)
                startPositions.remove(position)
                targetPositions.remove(position)
                riverTiles.remove(position)
                targetInfoMap.remove(position)

                when (tileType) {
                    de.egril.defender.editor.TileType.PATH -> pathCells.add(position)
                    de.egril.defender.editor.TileType.BUILD_AREA -> buildAreas.add(position)
                    de.egril.defender.editor.TileType.SPAWN_POINT -> if (!startPositions.contains(position)) startPositions.add(position)
                    de.egril.defender.editor.TileType.TARGET -> {
                        if (!targetPositions.contains(position)) {
                            targetPositions.add(position)
                        }
                        originalLevel.targetInfoMap[position]?.let { originalTargetInfo ->
                            targetInfoMap[position] = originalTargetInfo
                        }
                    }
                    de.egril.defender.editor.TileType.RIVER ->
                        riverTiles[position] = (riverTile ?: RiverTile(position = position)).copy(position = position)
                    de.egril.defender.editor.TileType.NO_PLAY -> {} // Already cleared from all collections.
                }
            }

            level =
                level.copy(
                    pathCells = pathCells.toSet(),
                    buildAreas = buildAreas.toSet(),
                    startPositions = startPositions.toList(),
                    targetPositions = targetPositions.toList(),
                    riverTiles = riverTiles.toMap(),
                    targetInfoMap = targetInfoMap.toMap(),
                )
        }
    }

    fun regionTilesOf(state: GameState, zone: TileZone): Map<Position, RegionTile> =
        with(state) {
            movedTileRegions[zone.id] ?: zone.tiles.mapValues { (position, zoneType) ->
                RegionTile(
                    zoneType,
                    (zone.riverTiles[position] ?: level.riverTiles[position])?.takeIf { zoneType == de.egril.defender.editor.TileType.RIVER },
                    position,
                )
            }
        }

    fun refreshZoneTiles(
        state: GameState,
        positions: Collection<Position>,
    ): Map<Position, Pair<de.egril.defender.editor.TileType, de.egril.defender.editor.TileType>> =
        with(state) {
            val activeZones = activeTileZoneIds.mapNotNull { id -> level.tileZones.firstOrNull { it.id == id } }
            val carriedTiles = movedTileRegions.values.flatMap { it.entries }.associate { it.key to it.value }
            val changes = mutableMapOf<Position, Pair<de.egril.defender.editor.TileType, RiverTile?>>()
            val typeChanges = mutableMapOf<Position, Pair<de.egril.defender.editor.TileType, de.egril.defender.editor.TileType>>()
            for (position in positions.toSet()) {
                val carried = carriedTiles[position]
                val zone = activeZones.lastOrNull { it.tiles.containsKey(position) }
                val newType = carried?.type ?: zone?.tiles?.get(position) ?: originalTileTypeAt(position)
                val newRiver =
                    if (newType == de.egril.defender.editor.TileType.RIVER) {
                        carried?.riverTile ?: zone?.riverTiles?.get(position) ?: originalRiverTileAt(position) ?: RiverTile(position = position)
                    } else {
                        null
                    }
                val oldType = currentTileTypeAt(position)
                val oldRiver = level.riverTiles[position]
                if (oldType != newType || oldRiver != newRiver?.copy(position = position)) {
                    changes[position] = newType to newRiver
                }
                if (oldType != newType) {
                    typeChanges[position] = oldType to newType
                }
                if ((carried != null || zone != null) && (newType != originalTileTypeAt(position) || newRiver != originalRiverTileAt(position))) {
                    zonePaintedTiles[position] = newType
                    if (newRiver != null) zonePaintedRiverTiles[position] = newRiver.copy(position = position) else zonePaintedRiverTiles.remove(position)
                } else {
                    zonePaintedTiles.remove(position)
                    zonePaintedRiverTiles.remove(position)
                }
            }
            replaceTiles(changes)
            if (changes.isNotEmpty()) mapEditVersion.value++
            typeChanges
        }
}
