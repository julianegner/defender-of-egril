package de.egril.defender.ui.editor.map

import de.egril.defender.editor.DEFAULT_MAP_TOOLING_INFO
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorTargetInfo
import de.egril.defender.editor.MapTemplateDefinition
import de.egril.defender.editor.MapTemplateLayoutKind
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.TargetType
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.hexDistanceTo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun createMapFromTemplate(
    id: String,
    name: String,
    width: Int,
    height: Int,
    author: String,
    template: MapTemplateDefinition?,
): EditorMap {
    val templateMap =
        when {
            template == null -> createBlankMap(id, name, width, height, author)
            template.layoutKind != null -> createProceduralTemplateMap(id, name, width, height, author, template.layoutKind)
            template.templateMap != null ->
                template.templateMap.copy(
                    id = id,
                    name = name,
                    author = author,
                    isOfficial = false,
                    isCommunity = false,
                    communityAuthorUsername = "",
                    readyToUse = false,
                    mapToolingInfo = DEFAULT_MAP_TOOLING_INFO,
                )
            else -> createBlankMap(id, name, width, height, author)
        }
    return templateMap.copy(readyToUse = templateMap.validateReadyToUse())
}

private fun createBlankMap(
    id: String,
    name: String,
    width: Int,
    height: Int,
    author: String,
): EditorMap =
    EditorMap(
        id = id,
        name = name,
        width = width,
        height = height,
        tiles = emptyMap(),
        author = author,
        mapToolingInfo = DEFAULT_MAP_TOOLING_INFO,
    )

private fun createProceduralTemplateMap(
    id: String,
    name: String,
    width: Int,
    height: Int,
    author: String,
    layoutKind: MapTemplateLayoutKind,
): EditorMap {
    val draft = MutableMapDraft(width = width, height = height)
    when (layoutKind) {
        MapTemplateLayoutKind.STRAIGHT_APPROACH -> populateStraightApproach(draft)
        MapTemplateLayoutKind.SPLIT_LANES -> populateSplitLanes(draft)
        MapTemplateLayoutKind.RIVER_CROSSING -> populateRiverCrossing(draft)
        MapTemplateLayoutKind.SPIRAL_SIEGE -> populateSpiralSiege(draft)
        MapTemplateLayoutKind.SPIDER_WEB -> populateSpiderWeb(draft)
    }
    val map =
        EditorMap(
            id = id,
            name = name,
            width = width,
            height = height,
            author = author,
            tiles = draft.tiles,
            riverTiles = draft.riverTiles,
            targetInfoMap = draft.targetInfoMap,
            spawnPointInfoMap = draft.spawnPointInfoMap,
            mapToolingInfo = DEFAULT_MAP_TOOLING_INFO,
        )
    return if (map.validateReadyToUse()) {
        map
    } else {
        repairUnreachableGeneratedMap(draft, map)
    }
}

private fun repairUnreachableGeneratedMap(
    draft: MutableMapDraft,
    map: EditorMap,
): EditorMap {
    val target = map.getTarget() ?: return map
    val spawnPoints = map.getSpawnPoints()
    if (spawnPoints.isEmpty()) return map

    val repairedDraft = MutableMapDraft(width = map.width, height = map.height)
    repairedDraft.tiles.putAll(draft.tiles)
    repairedDraft.riverTiles.putAll(draft.riverTiles)
    repairedDraft.targetInfoMap.putAll(draft.targetInfoMap)
    repairedDraft.spawnPointInfoMap.putAll(draft.spawnPointInfoMap)

    val spawnSet = spawnPoints.toSet()
    for (spawn in spawnPoints) {
        val path = hexLine(spawn, target, map.width, map.height)
        path.forEach { position ->
            if (position != target && position !in spawnSet) {
                repairedDraft.setTile(position, TileType.PATH)
            }
        }
    }

    val repairedMap =
        EditorMap(
            id = map.id,
            name = map.name,
            width = map.width,
            height = map.height,
            author = map.author,
            tiles = repairedDraft.tiles,
            riverTiles = repairedDraft.riverTiles,
            targetInfoMap = repairedDraft.targetInfoMap,
            spawnPointInfoMap = repairedDraft.spawnPointInfoMap,
            mapToolingInfo = map.mapToolingInfo,
        )
    return repairedMap.copy(readyToUse = repairedMap.validateReadyToUse())
}

private data class MutableMapDraft(
    val width: Int,
    val height: Int,
    val tiles: MutableMap<String, TileType> = mutableMapOf(),
    val riverTiles: MutableMap<String, RiverTile> = mutableMapOf(),
    val targetInfoMap: MutableMap<String, EditorTargetInfo> = mutableMapOf(),
    val spawnPointInfoMap: MutableMap<String, SpawnPointType> = mutableMapOf(),
) {
    fun setTile(
        position: Position,
        type: TileType,
    ) {
        if (position.x !in 0 until width || position.y !in 0 until height) return
        tiles["${position.x},${position.y}"] = type
    }

    fun setSpawn(
        position: Position,
        spawnPointType: SpawnPointType = SpawnPointType.LAND,
    ) {
        setTile(position, TileType.SPAWN_POINT)
        spawnPointInfoMap["${position.x},${position.y}"] = spawnPointType
    }

    fun setTarget(
        position: Position,
        name: String = "",
    ) {
        setTile(position, TileType.TARGET)
        targetInfoMap["${position.x},${position.y}"] = EditorTargetInfo(name = name, type = TargetType.STANDARD)
    }

    fun setRiver(
        position: Position,
        flow: RiverFlow,
    ) {
        setTile(position, TileType.RIVER)
        riverTiles["${position.x},${position.y}"] = RiverTile(position = position, flowDirection = flow, flowSpeed = 1)
    }
}

private fun populateStraightApproach(draft: MutableMapDraft) {
    val midY = draft.height / 2
    for (x in 0 until draft.width) {
        draft.setTile(Position(x, midY), TileType.PATH)
        addAdjacentBuildAreas(draft, Position(x, midY))
    }
    draft.setSpawn(Position(0, midY))
    draft.setTarget(Position(draft.width - 1, midY), "Town Hall")
}

private fun populateSplitLanes(draft: MutableMapDraft) {
    val upperY = (draft.height / 3).coerceAtLeast(1)
    val lowerY = (draft.height * 2 / 3).coerceAtMost(draft.height - 2)
    val mergeY = draft.height / 2
    val mergeX = (draft.width * 2 / 3).coerceAtLeast(2)
    for (x in 0..mergeX) {
        draft.setTile(Position(x, upperY), TileType.PATH)
        draft.setTile(Position(x, lowerY), TileType.PATH)
        addAdjacentBuildAreas(draft, Position(x, upperY))
        addAdjacentBuildAreas(draft, Position(x, lowerY))
    }
    for (x in mergeX until draft.width) {
        draft.setTile(Position(x, mergeY), TileType.PATH)
        addAdjacentBuildAreas(draft, Position(x, mergeY))
    }
    draft.setSpawn(Position(0, upperY))
    draft.setSpawn(Position(0, lowerY))
    draft.setTarget(Position(draft.width - 1, mergeY), "Gate")
}

private fun populateRiverCrossing(draft: MutableMapDraft) {
    val midY = draft.height / 2
    for (x in 0 until draft.width) {
        draft.setTile(Position(x, midY), TileType.PATH)
        addAdjacentBuildAreas(draft, Position(x, midY))
    }
    for (i in 0 until minOf(draft.width, draft.height)) {
        val y = (draft.height - 1 - i).coerceAtLeast(0)
        draft.setRiver(Position(i, y), RiverFlow.SOUTH_EAST)
    }
    draft.setSpawn(Position(0, midY))
    draft.setSpawn(Position(0, draft.height - 1), SpawnPointType.WATER)
    draft.setTarget(Position(draft.width - 1, midY), "Harbor")
}

private fun populateSpiralSiege(draft: MutableMapDraft) {
    val rows =
        listOf(
            1,
            (draft.height / 2).coerceAtLeast(2),
            (draft.height - 2).coerceAtLeast(1),
        ).distinct().filter { it in 0 until draft.height }
    var leftToRight = true
    rows.forEachIndexed { index, row ->
        val range = if (leftToRight) 0 until draft.width else (draft.width - 1 downTo 0)
        range.forEach { x ->
            draft.setTile(Position(x, row), TileType.PATH)
            addAdjacentBuildAreas(draft, Position(x, row))
        }
        if (index < rows.lastIndex) {
            val connectorX = if (leftToRight) draft.width - 1 else 0
            val nextRow = rows[index + 1]
            val rowRange = if (row <= nextRow) row..nextRow else nextRow..row
            rowRange.forEach { y ->
                draft.setTile(Position(connectorX, y), TileType.PATH)
                addAdjacentBuildAreas(draft, Position(connectorX, y))
            }
        }
        leftToRight = !leftToRight
    }
    draft.setSpawn(Position(0, rows.first()))
    draft.setTarget(Position(draft.width - 1, rows.last()), "Citadel")
}

/**
 * Web-shaped layout: the target sits in the middle of the web, spokes lead outwards to the spawn
 * points on the map border and two rings connect the spokes so the enemies can switch lanes.
 */
private fun populateSpiderWeb(draft: MutableMapDraft) {
    val center = Position(draft.width / 2, draft.height / 2)
    val spokeCount = 6
    val radiusX = (draft.width / 2 - 1).coerceAtLeast(1)
    val radiusY = (draft.height / 2 - 1).coerceAtLeast(1)
    val spokes =
        (0 until spokeCount).map { index ->
            val angle = 2.0 * PI * index / spokeCount
            val end =
                Position(
                    (center.x + cos(angle) * radiusX).roundToInt().coerceIn(0, draft.width - 1),
                    (center.y + sin(angle) * radiusY).roundToInt().coerceIn(0, draft.height - 1),
                )
            hexLine(center, end, draft.width, draft.height)
        }

    val pathTiles = mutableListOf<Position>()
    spokes.forEach { pathTiles += it }
    // Two rings between the center and the outer ends of the spokes.
    listOf(0.45, 0.85).forEach { fraction ->
        spokes.indices.forEach { index ->
            val from = spokes[index].ringTile(fraction)
            val to = spokes[(index + 1) % spokes.size].ringTile(fraction)
            pathTiles += hexLine(from, to, draft.width, draft.height)
        }
    }

    pathTiles.forEach { draft.setTile(it, TileType.PATH) }
    pathTiles.forEach { addAdjacentBuildAreas(draft, it) }
    spokes.forEach { spoke -> draft.setSpawn(spoke.last()) }
    draft.setTarget(center, "Web Heart")
}

private fun List<Position>.ringTile(fraction: Double): Position = this[(size * fraction).roundToInt().coerceIn(1, size - 1)]

/**
 * Walks from [from] to [to] over hex neighbors, so the resulting tiles form a connected path.
 */
private fun hexLine(
    from: Position,
    to: Position,
    width: Int,
    height: Int,
): List<Position> {
    val line = mutableListOf(from)
    var current = from
    var guard = 0
    while (current != to && guard < width * height) {
        guard++
        val next =
            current
                .getHexNeighbors()
                .filter { it.x in 0 until width && it.y in 0 until height }
                .minByOrNull { it.hexDistanceTo(to) } ?: break
        if (next.hexDistanceTo(to) >= current.hexDistanceTo(to)) break
        current = next
        line += current
    }
    return line
}

private fun addAdjacentBuildAreas(
    draft: MutableMapDraft,
    position: Position,
) {
    position
        .getHexNeighbors()
        .filter { it.x in 0 until draft.width && it.y in 0 until draft.height }
        .forEach { neighbor ->
            val key = "${neighbor.x},${neighbor.y}"
            if (draft.tiles[key] == null) {
                draft.tiles[key] = TileType.BUILD_AREA
            }
        }
}
