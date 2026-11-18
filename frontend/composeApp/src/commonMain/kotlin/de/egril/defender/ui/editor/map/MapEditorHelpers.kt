package de.egril.defender.ui.editor.map

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorTargetInfo
import de.egril.defender.editor.MapSizeLimits
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.getHexDirectionTo
import de.egril.defender.model.getHexNeighbors

/**
 * Converts a human-readable map name into a stable map ID component, e.g. "My Map" → "my_map".
 */
internal fun nameToMapId(name: String): String {
    val sanitized =
        name
            .trim()
            .lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")
    return if (sanitized.isNotEmpty()) "map_$sanitized" else ""
}

internal data class ResizedMapData(
    val width: Int,
    val height: Int,
    val tiles: MutableMap<String, TileType>,
    val riverTiles: MutableMap<String, RiverTile>,
    val targetInfoMap: MutableMap<String, EditorTargetInfo>,
    val spawnPointInfoMap: MutableMap<String, SpawnPointType>,
)

internal data class MapEditorSnapshot(
    val width: Int,
    val height: Int,
    val tiles: MutableMap<String, TileType>,
    val riverTiles: MutableMap<String, RiverTile>,
    val targetInfoMap: MutableMap<String, EditorTargetInfo>,
    val spawnPointInfoMap: MutableMap<String, SpawnPointType>,
    val mapName: String,
    val mapAuthor: String,
    val mapToolingInfo: String,
    val allowNoBuildableTiles: Boolean,
    val allowNoDirectPath: Boolean,
    val tileZones: List<de.egril.defender.model.TileZone>,
)

internal data class MapRegionClipboard(
    val width: Int,
    val height: Int,
    val tiles: Map<String, TileType>,
    val riverTiles: Map<String, RiverTile>,
    val targetInfoMap: Map<String, EditorTargetInfo>,
    val spawnPointInfoMap: Map<String, SpawnPointType>,
)

internal data class MapPathPreview(
    val spawn: Position,
    val target: Position?,
    val path: List<Position>,
    val isReachable: Boolean,
    val isAmbiguous: Boolean,
)

internal fun applyRiverRing(
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
    mapWidth: Int,
    mapHeight: Int,
    start: Position,
    clockwise: Boolean,
    innerToOuter: Boolean,
    flowSpeed: Int,
): Pair<MutableMap<String, TileType>, MutableMap<String, RiverTile>> {
    val newTiles = tiles.toMutableMap()
    val newRivers = riverTiles.toMutableMap()
    val anchor = start.takeIf { it.x in 0 until mapWidth && it.y in 0 until mapHeight } ?: return newTiles to newRivers

    // The spiral is always generated from the center (anchor) outwards so that consecutive
    // rings connect into a single continuous path. Reversing the traversal order (for
    // outer-to-inner filling) also reverses the tangential direction of travel within each
    // ring, so we compensate by flipping the rotation used to build the base spiral, keeping
    // the visual "clockwise"/"counterclockwise" choice consistent regardless of fill direction.
    val baseClockwise = if (innerToOuter) clockwise else !clockwise
    val spiralFromCenter = buildIslandAwareSpiralPath(anchor, tiles, riverTiles, mapWidth, mapHeight, baseClockwise)
    if (spiralFromCenter.size <= 1) return newTiles to newRivers

    val path = if (innerToOuter) spiralFromCenter else spiralFromCenter.asReversed()
    path.forEachIndexed { index, position ->
        if (index == path.lastIndex) return@forEachIndexed
        val key = "${position.x},${position.y}"
        val existing = newRivers[key]
        if (existing == null || existing.flowDirection == RiverFlow.NONE) {
            val next = path[index + 1]
            val flow = flowFromTo(position, next)
            if (flow != RiverFlow.NONE) {
                newTiles[key] = TileType.RIVER
                newRivers[key] = RiverTile(position = position, flowDirection = flow, flowSpeed = flowSpeed)
            }
        }
    }
    return newTiles to newRivers
}

/**
 * Builds a single continuous spiral path starting at [anchor], expanding outward ring by ring
 * (each ring being one hex-step further from the anchor, following the shape of the connected,
 * non-solid area so islands and obstacles are respected).
 *
 * Rather than treating every ring as an independent closed loop, each ring is walked in full and
 * then the path steps outward to the neighboring ring right next to where it left off - which,
 * since the last tile of a ring sits right next to its own start, naturally reads as "go around,
 * and one field before closing the loop, go one more out". This keeps the flow direction
 * continuous across the whole spiral instead of jumping/closing at each ring boundary.
 */
internal fun buildIslandAwareSpiralPath(
    anchor: Position,
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
    mapWidth: Int,
    mapHeight: Int,
    clockwise: Boolean,
): List<Position> {
    val visited = mutableSetOf<Position>()
    val rawRings = mutableListOf<List<Position>>()
    var frontier = setOf(anchor)
    var guard = 0

    while (frontier.isNotEmpty() && guard++ < mapWidth * mapHeight) {
        val ring =
            frontier
                .filter { it.x in 0 until mapWidth && it.y in 0 until mapHeight }
                .filter { !isSolidTile(it, tiles, riverTiles) }
                .filter { visited.add(it) }
        if (ring.isEmpty()) break
        rawRings += ring
        frontier =
            ring
                .flatMap { position ->
                    position
                        .getHexNeighbors()
                        .filter { neighbor ->
                            neighbor.x in 0 until mapWidth &&
                                neighbor.y in 0 until mapHeight &&
                                !visited.contains(neighbor) &&
                                !isSolidTile(neighbor, tiles, riverTiles)
                        }
                }.toSet()
    }
    if (rawRings.isEmpty()) return emptyList()

    val spiral = mutableListOf<Position>()
    var previousTail: Position? = null
    rawRings.forEach { ringPositions ->
        val ringSet = ringPositions.toSet()
        val start =
            previousTail?.getHexNeighbors()?.firstOrNull { it in ringSet }
                ?: ringPositions.minByOrNull { it.y * mapWidth + it.x }
                ?: return@forEach
        val ordered = orderRingChain(start, ringSet, tiles, riverTiles, clockwise)
        spiral += ordered
        previousTail = ordered.lastOrNull()
    }
    return spiral
}

/**
 * Walks all positions of a single ring starting at [start], following hex-adjacency within
 * [ringSet] in a consistent rotational direction ([clockwise]).
 */
internal fun orderRingChain(
    start: Position,
    ringSet: Set<Position>,
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
    clockwise: Boolean,
): List<Position> {
    if (ringSet.size <= 1) return listOf(start)
    val ordered = mutableListOf(start)
    val used = mutableSetOf(start)
    var current = start
    var guard = 0
    while (guard++ < ringSet.size * 2) {
        val neighbors = current.getHexNeighbors().let { if (clockwise) it else it.asReversed() }
        val next =
            neighbors.firstOrNull { it in ringSet && it !in used && !isSolidTile(it, tiles, riverTiles) }
                ?: break
        ordered += next
        used += next
        current = next
    }
    return ordered
}

internal fun isSolidTile(
    position: Position,
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
): Boolean {
    val tile = tiles["${position.x},${position.y}"]
    if (tile == TileType.RIVER) {
        val river = riverTiles["${position.x},${position.y}"]
        return river != null && river.flowDirection != RiverFlow.NONE && river.flowDirection != RiverFlow.MAELSTROM
    }
    return tile != null && tile != TileType.NO_PLAY
}

internal fun flowFromTo(
    from: Position,
    to: Position,
): RiverFlow =
    // Hex tiles have 6 neighbor directions (E, NE, NW, W, SW, SE), not just the 4 that a naive
    // x/y comparison would suggest. Delegate to getHexDirectionTo (which already accounts for
    // the even/odd row offset) so NORTH_EAST and SOUTH_WEST are also produced correctly -
    // otherwise those two directions get misclassified as NORTH_WEST/SOUTH_EAST, making a
    // spiral flow reverse direction partway around a ring.
    when (from.getHexDirectionTo(to)) {
        0 -> RiverFlow.EAST
        1 -> RiverFlow.NORTH_EAST
        2 -> RiverFlow.NORTH_WEST
        3 -> RiverFlow.WEST
        4 -> RiverFlow.SOUTH_WEST
        5 -> RiverFlow.SOUTH_EAST
        else -> RiverFlow.NONE
    }

internal fun applyResizeToMapData(
    width: Int,
    height: Int,
    leftDelta: Int,
    rightDelta: Int,
    topDelta: Int,
    bottomDelta: Int,
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
    targetInfoMap: Map<String, EditorTargetInfo>,
    spawnPointInfoMap: Map<String, SpawnPointType>,
): ResizedMapData {
    val newWidth = width + leftDelta + rightDelta
    val newHeight = height + topDelta + bottomDelta
    require(newWidth > 0 && newHeight > 0)
    require(MapSizeLimits.isWithinLimits(newWidth, newHeight))

    fun shiftedPosition(position: Position): Position? {
        val shifted = Position(position.x + leftDelta, position.y + topDelta)
        return shifted.takeIf { it.x in 0 until newWidth && it.y in 0 until newHeight }
    }

    val resizedTiles = mutableMapOf<String, TileType>()
    tiles.forEach { (key, value) ->
        val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
        shiftedPosition(Position(x, y))?.let { resizedTiles["${it.x},${it.y}"] = value }
    }

    val resizedRiverTiles = mutableMapOf<String, RiverTile>()
    riverTiles.forEach { (_, riverTile) ->
        shiftedPosition(riverTile.position)?.let { shifted ->
            resizedRiverTiles["${shifted.x},${shifted.y}"] =
                riverTile.copy(position = shifted)
        }
    }

    val resizedTargetInfo = mutableMapOf<String, EditorTargetInfo>()
    targetInfoMap.forEach { (key, info) ->
        val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
        shiftedPosition(Position(x, y))?.let { resizedTargetInfo["${it.x},${it.y}"] = info }
    }

    val resizedSpawnPointInfo = mutableMapOf<String, SpawnPointType>()
    spawnPointInfoMap.forEach { (key, type) ->
        val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
        shiftedPosition(Position(x, y))?.let { resizedSpawnPointInfo["${it.x},${it.y}"] = type }
    }

    return ResizedMapData(
        width = newWidth,
        height = newHeight,
        tiles = resizedTiles,
        riverTiles = resizedRiverTiles,
        targetInfoMap = resizedTargetInfo,
        spawnPointInfoMap = resizedSpawnPointInfo,
    )
}

internal fun isSafeEndExpansion(
    leftDelta: Int,
    rightDelta: Int,
    topDelta: Int,
    bottomDelta: Int,
): Boolean = leftDelta == 0 && topDelta == 0 && rightDelta >= 0 && bottomDelta >= 0

internal fun createTemplateId(name: String): String = nameToMapId(name).removePrefix("map_").let { "template_$it" }

internal fun EditorMap.previewPaths(): List<MapPathPreview> {
    val traversableCells = getPathCells() + getSpawnPoints() + getTargets() + getRiverCells()
    val targets = getTargets().toSet()
    return getSpawnPoints().sortedWith(compareBy(Position::y, Position::x)).map { spawn ->
        val path = findShortestPath(spawn, targets, traversableCells, width, height)
        val isAmbiguous =
            path.size > 1 &&
                path.dropLast(1).anyIndexed { index, current ->
                    val previous = path.getOrNull(index - 1)
                    0 > 1
                }
        MapPathPreview(
            spawn = spawn,
            target = path.lastOrNull()?.takeIf { it in targets },
            path = path,
            isReachable = path.isNotEmpty(),
            isAmbiguous = isAmbiguous,
        )
    }
}

internal fun findShortestPath(
    start: Position,
    targets: Set<Position>,
    traversableCells: Set<Position>,
    width: Int,
    height: Int,
): List<Position> {
    if (start in targets) return listOf(start)
    val queue = ArrayDeque<Position>()
    val visited = mutableSetOf(start)
    val previous = mutableMapOf<Position, Position?>()
    queue.add(start)
    previous[start] = null
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        current
            .getHexNeighbors()
            .filter { it.isInside(width, height) && it in traversableCells }
            .forEach { neighbor ->
                if (!visited.add(neighbor)) return@forEach
                previous[neighbor] = current
                if (neighbor in targets) {
                    val path = mutableListOf<Position>()
                    var cursor: Position? = neighbor
                    while (cursor != null) {
                        path += cursor
                        cursor = previous[cursor]
                    }
                    return path.reversed()
                }
                queue.add(neighbor)
            }
    }
    return emptyList()
}

internal fun copyMapRegion(
    from: Position,
    to: Position,
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
    targetInfoMap: Map<String, EditorTargetInfo>,
    spawnPointInfoMap: Map<String, SpawnPointType>,
): MapRegionClipboard {
    val minX = minOf(from.x, to.x)
    val maxX = maxOf(from.x, to.x)
    val minY = minOf(from.y, to.y)
    val maxY = maxOf(from.y, to.y)

    fun relativeKey(
        x: Int,
        y: Int,
    ): String = "${x - minX},${y - minY}"

    val copiedTiles =
        tiles
            .mapNotNull { (key, tileType) ->
                val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
                if (x in minX..maxX && y in minY..maxY) relativeKey(x, y) to tileType else null
            }.toMap()
    val copiedRiverTiles =
        riverTiles
            .mapNotNull { (_, riverTile) ->
                val x = riverTile.position.x
                val y = riverTile.position.y
                if (x in minX..maxX && y in minY..maxY) {
                    val relative = Position(x - minX, y - minY)
                    relativeKey(x, y) to riverTile.copy(position = relative)
                } else {
                    null
                }
            }.toMap()
    val copiedTargetInfo =
        targetInfoMap
            .mapNotNull { (key, info) ->
                val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
                if (x in minX..maxX && y in minY..maxY) relativeKey(x, y) to info else null
            }.toMap()
    val copiedSpawnInfo =
        spawnPointInfoMap
            .mapNotNull { (key, type) ->
                val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
                if (x in minX..maxX && y in minY..maxY) relativeKey(x, y) to type else null
            }.toMap()
    return MapRegionClipboard(
        width = maxX - minX + 1,
        height = maxY - minY + 1,
        tiles = copiedTiles,
        riverTiles = copiedRiverTiles,
        targetInfoMap = copiedTargetInfo,
        spawnPointInfoMap = copiedSpawnInfo,
    )
}

internal fun pasteMapRegion(
    clipboard: MapRegionClipboard,
    at: Position,
    mapWidth: Int,
    mapHeight: Int,
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
    targetInfoMap: Map<String, EditorTargetInfo>,
    spawnPointInfoMap: Map<String, SpawnPointType>,
): ResizedMapData {
    val updatedTiles = tiles.toMutableMap()
    val updatedRiverTiles = riverTiles.toMutableMap()
    val updatedTargetInfoMap = targetInfoMap.toMutableMap()
    val updatedSpawnPointInfoMap = spawnPointInfoMap.toMutableMap()

    fun destinationPosition(relativeKey: String): Position? {
        val (x, y) = relativeKey.split(",").let { it[0].toInt() to it[1].toInt() }
        val destination = Position(at.x + x, at.y + y)
        return destination.takeIf { it.isInside(mapWidth, mapHeight) }
    }

    clipboard.tiles.forEach { (relativeKey, tileType) ->
        val destination = destinationPosition(relativeKey) ?: return@forEach
        val destinationKey = "${destination.x},${destination.y}"
        updatedTiles[destinationKey] = tileType
        if (tileType != TileType.RIVER) updatedRiverTiles.remove(destinationKey)
        if (tileType != TileType.TARGET) updatedTargetInfoMap.remove(destinationKey)
        if (tileType != TileType.SPAWN_POINT) updatedSpawnPointInfoMap.remove(destinationKey)
    }
    clipboard.riverTiles.forEach { (relativeKey, riverTile) ->
        val destination = destinationPosition(relativeKey) ?: return@forEach
        val destinationKey = "${destination.x},${destination.y}"
        updatedRiverTiles[destinationKey] = riverTile.copy(position = destination)
    }
    clipboard.targetInfoMap.forEach { (relativeKey, info) ->
        val destination = destinationPosition(relativeKey) ?: return@forEach
        updatedTargetInfoMap["${destination.x},${destination.y}"] = info
    }
    clipboard.spawnPointInfoMap.forEach { (relativeKey, type) ->
        val destination = destinationPosition(relativeKey) ?: return@forEach
        updatedSpawnPointInfoMap["${destination.x},${destination.y}"] = type
    }

    return ResizedMapData(
        width = mapWidth,
        height = mapHeight,
        tiles = updatedTiles,
        riverTiles = updatedRiverTiles,
        targetInfoMap = updatedTargetInfoMap,
        spawnPointInfoMap = updatedSpawnPointInfoMap,
    )
}

internal fun Position.isInside(
    width: Int,
    height: Int,
): Boolean = x in 0 until width && y in 0 until height

internal inline fun <T> Iterable<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
    var index = 0
    for (item in this) {
        if (predicate(index, item)) return true
        index++
    }
    return false
}
