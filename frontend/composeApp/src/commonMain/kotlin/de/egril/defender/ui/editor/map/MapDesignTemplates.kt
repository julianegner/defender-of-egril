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
import kotlin.random.Random

internal fun createMapFromTemplate(
    id: String,
    name: String,
    width: Int,
    height: Int,
    author: String,
    template: MapTemplateDefinition?,
    variationSeed: Int? = null,
): EditorMap {
    val templateMap =
        when {
            template == null -> createBlankMap(id, name, width, height, author)
            template.layoutKind != null ->
                createProceduralTemplateMap(id, name, width, height, author, template.layoutKind, variationSeed)
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
    variationSeed: Int?,
): EditorMap {
    val draft = MutableMapDraft(width = width, height = height)
    val random = Random(variationSeed ?: 0)
    when (layoutKind) {
        MapTemplateLayoutKind.STRAIGHT_APPROACH -> populateStraightApproach(draft, random)
        MapTemplateLayoutKind.BENT_APPROACH -> populateBentApproach(draft, random)
        MapTemplateLayoutKind.SPLIT_LANES -> populateSplitLanes(draft, random)
        MapTemplateLayoutKind.DUAL_FRONT -> populateDualFront(draft, random)
        MapTemplateLayoutKind.RIVER_CROSSING -> populateRiverCrossing(draft, random)
        MapTemplateLayoutKind.RIVER_DELTA -> populateRiverDelta(draft, random)
        MapTemplateLayoutKind.ISLAND_CHAIN -> populateIslandChain(draft, random)
        MapTemplateLayoutKind.SPIRAL_SIEGE -> populateSpiralSiege(draft, random)
        MapTemplateLayoutKind.SERPENTINE_MARCH -> populateSerpentineMarch(draft, random)
        MapTemplateLayoutKind.SPIDER_WEB -> populateSpiderWeb(draft, random)
        MapTemplateLayoutKind.RING_ROAD -> populateRingRoad(draft, random)
        MapTemplateLayoutKind.CROSSROADS -> populateCrossroads(draft, random)
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

private fun populateStraightApproach(
    draft: MutableMapDraft,
    random: Random,
) {
    val midY = jitteredRow(draft.height / 2, draft.height, random, maxOffset = 2)
    carveLine(draft, Position(0, midY), Position(draft.width - 1, midY))
    draft.setSpawn(Position(0, midY))
    draft.setTarget(Position(draft.width - 1, midY), "Town Hall")
}

private fun populateBentApproach(
    draft: MutableMapDraft,
    random: Random,
) {
    val startY = jitteredRow(draft.height / 2, draft.height, random, maxOffset = 3)
    val endY = jitteredRow(draft.height / 2, draft.height, random, maxOffset = 3)
    val bendX = jitteredColumn(draft.width * 2 / 5, draft.width, random, maxOffset = 3)
    val start = Position(0, startY)
    val cornerA = Position(bendX, startY)
    val cornerB = Position(bendX, endY)
    val target = Position(draft.width - 1, endY)
    carveLine(draft, start, cornerA)
    carveLine(draft, cornerA, cornerB)
    carveLine(draft, cornerB, target)
    draft.setSpawn(start)
    draft.setTarget(target, "Gate")
}

private fun populateSplitLanes(
    draft: MutableMapDraft,
    random: Random,
) {
    val upperY = jitteredRow((draft.height / 3).coerceAtLeast(1), draft.height, random, maxOffset = 1)
    val lowerY = jitteredRow((draft.height * 2 / 3).coerceAtMost(draft.height - 2), draft.height, random, maxOffset = 1)
    val mergeY = jitteredRow(draft.height / 2, draft.height, random, maxOffset = 1)
    val mergeX = jitteredColumn((draft.width * 2 / 3).coerceAtLeast(2), draft.width, random, maxOffset = 3)
    val upperSpawn = Position(0, upperY)
    val lowerSpawn = Position(0, lowerY)
    val merge = Position(mergeX, mergeY)
    val target = Position(draft.width - 1, mergeY)
    carveLine(draft, upperSpawn, merge)
    carveLine(draft, lowerSpawn, merge)
    carveLine(draft, merge, target)
    draft.setSpawn(upperSpawn)
    draft.setSpawn(lowerSpawn)
    draft.setTarget(target, "Gate")
}

private fun populateDualFront(
    draft: MutableMapDraft,
    random: Random,
) {
    val topY = jitteredRow((draft.height / 4).coerceAtLeast(1), draft.height, random, maxOffset = 1)
    val bottomY = jitteredRow((draft.height * 3 / 4).coerceAtMost(draft.height - 2), draft.height, random, maxOffset = 1)
    val leftMergeX = jitteredColumn((draft.width / 3).coerceAtLeast(1), draft.width, random, maxOffset = 2)
    val rightMergeX = jitteredColumn((draft.width * 2 / 3).coerceAtMost(draft.width - 2), draft.width, random, maxOffset = 2)
    val centerY = jitteredRow(draft.height / 2, draft.height, random, maxOffset = 1)
    val topSpawn = Position(0, topY)
    val bottomSpawn = Position(0, bottomY)
    val leftMerge = Position(leftMergeX, centerY)
    val rightMerge = Position(rightMergeX, centerY)
    val target = Position(draft.width - 1, centerY)

    carveLine(draft, topSpawn, leftMerge)
    carveLine(draft, bottomSpawn, leftMerge)
    carveLine(draft, leftMerge, rightMerge)
    carveLine(draft, rightMerge, target)
    draft.setSpawn(topSpawn)
    draft.setSpawn(bottomSpawn)
    draft.setTarget(target, "Stronghold")
}

private fun populateRiverCrossing(
    draft: MutableMapDraft,
    random: Random,
) {
    val midY = jitteredRow(draft.height / 2, draft.height, random, maxOffset = 2)
    val landSpawn = Position(0, midY)
    val target = Position(draft.width - 1, midY)
    carveLine(draft, landSpawn, target)

    val riverStartY = jitteredRow(draft.height - 1, draft.height, random, maxOffset = 0)
    val riverTurn = Position(jitteredColumn(draft.width / 3, draft.width, random, maxOffset = 2), jitteredRow(midY, draft.height, random, maxOffset = 2))
    setRiverPath(draft, Position(0, riverStartY), riverTurn)
    setRiverPath(draft, riverTurn, Position(jitteredColumn(draft.width * 3 / 5, draft.width, random, maxOffset = 2), jitteredRow(1, draft.height, random, maxOffset = 1)))

    draft.setSpawn(landSpawn)
    draft.setSpawn(Position(0, riverStartY), SpawnPointType.WATER)
    draft.setTarget(target, "Harbor")
}

private fun populateRiverDelta(
    draft: MutableMapDraft,
    random: Random,
) {
    val midY = jitteredRow(draft.height / 2, draft.height, random, maxOffset = 1)
    val target = Position(draft.width - 1, midY)
    val landSpawn = Position(0, midY)
    carveLine(draft, landSpawn, target)

    val topWaterSpawn = Position(0, jitteredRow((draft.height / 4).coerceAtLeast(0), draft.height, random, maxOffset = 1))
    val bottomWaterSpawn = Position(0, jitteredRow((draft.height * 3 / 4).coerceAtMost(draft.height - 1), draft.height, random, maxOffset = 1))
    val riverJoin = Position(jitteredColumn(draft.width / 2, draft.width, random, maxOffset = 2), midY)
    setRiverPath(draft, topWaterSpawn, riverJoin)
    setRiverPath(draft, bottomWaterSpawn, riverJoin)
    setRiverPath(draft, riverJoin, Position(draft.width - 2, midY))

    draft.setSpawn(landSpawn)
    draft.setSpawn(topWaterSpawn, SpawnPointType.WATER)
    draft.setSpawn(bottomWaterSpawn, SpawnPointType.WATER)
    draft.setTarget(target, "Delta Port")
}

private fun populateIslandChain(
    draft: MutableMapDraft,
    random: Random,
) {
    val midY = jitteredRow(draft.height / 2, draft.height, random, maxOffset = 1)
    val target = Position(draft.width - 1, midY)
    val landSpawn = Position(0, midY)
    val firstBend = Position(jitteredColumn(draft.width / 4, draft.width, random, maxOffset = 2), jitteredRow(midY - 2, draft.height, random, maxOffset = 2))
    val secondBend = Position(jitteredColumn(draft.width / 2, draft.width, random, maxOffset = 2), jitteredRow(midY + 2, draft.height, random, maxOffset = 2))
    carveLine(draft, landSpawn, firstBend)
    carveLine(draft, firstBend, secondBend)
    carveLine(draft, secondBend, target)

    val waterSpawn = Position(0, jitteredRow((draft.height * 3 / 4).coerceAtMost(draft.height - 1), draft.height, random, maxOffset = 1))
    val riverWaypointA = Position(jitteredColumn(draft.width / 3, draft.width, random, maxOffset = 2), jitteredRow(midY + 3, draft.height, random, maxOffset = 1))
    val riverWaypointB = Position(jitteredColumn(draft.width * 2 / 3, draft.width, random, maxOffset = 2), jitteredRow(midY - 1, draft.height, random, maxOffset = 1))
    setRiverPath(draft, waterSpawn, riverWaypointA)
    setRiverPath(draft, riverWaypointA, riverWaypointB)
    setRiverPath(draft, riverWaypointB, Position(draft.width - 2, midY))

    draft.setSpawn(landSpawn)
    draft.setSpawn(waterSpawn, SpawnPointType.WATER)
    draft.setTarget(target, "Island Harbor")
}

private fun populateSpiralSiege(
    draft: MutableMapDraft,
    random: Random,
) {
    val rows =
        listOf(
            1,
            (draft.height / 2).coerceAtLeast(2),
            (draft.height - 2).coerceAtLeast(1),
        ).map { jitteredRow(it, draft.height, random, maxOffset = 1) }
            .distinct()
            .filter { it in 0 until draft.height }
    var leftToRight = true
    rows.forEachIndexed { index, row ->
        val start = Position(if (leftToRight) 0 else draft.width - 1, row)
        val end = Position(if (leftToRight) draft.width - 1 else 0, row)
        carveLine(draft, start, end)
        if (index < rows.lastIndex) {
            val connectorX = if (leftToRight) draft.width - 1 else 0
            val nextRow = rows[index + 1]
            carveLine(draft, Position(connectorX, row), Position(connectorX, nextRow))
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
private fun populateSerpentineMarch(
    draft: MutableMapDraft,
    random: Random,
) {
    val rowStep = (draft.height / 5).coerceAtLeast(2)
    val baseRows = (1 until draft.height - 1 step rowStep).toList().ifEmpty { listOf(draft.height / 2) }
    val rows = baseRows.map { jitteredRow(it, draft.height, random, maxOffset = 1) }.distinct().sorted()
    var leftToRight = true
    var lastEnd = Position(draft.width - 1, rows.first())

    rows.forEachIndexed { index, row ->
        val start = Position(if (leftToRight) 0 else draft.width - 1, row)
        val end = Position(if (leftToRight) draft.width - 1 else 0, row)
        carveLine(draft, start, end)
        lastEnd = end
        if (index < rows.lastIndex) {
            carveLine(draft, end, Position(end.x, rows[index + 1]))
        }
        leftToRight = !leftToRight
    }

    draft.setSpawn(Position(0, rows.first()))
    draft.setTarget(lastEnd, "Outpost")
}

private fun populateSpiderWeb(
    draft: MutableMapDraft,
    random: Random,
) {
    val center = Position(draft.width / 2, draft.height / 2)
    val spokeCount = if (random.nextBoolean()) 6 else 8
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

private fun populateRingRoad(
    draft: MutableMapDraft,
    random: Random,
) {
    val center = Position(draft.width / 2, draft.height / 2)
    val ringPoints =
        listOf(
            Position(jitteredColumn(draft.width / 2, draft.width, random, maxOffset = 1), 1),
            Position(draft.width - 2, jitteredRow(draft.height / 3, draft.height, random, maxOffset = 1)),
            Position(draft.width - 2, jitteredRow(draft.height * 2 / 3, draft.height, random, maxOffset = 1)),
            Position(jitteredColumn(draft.width / 2, draft.width, random, maxOffset = 1), draft.height - 2),
            Position(1, jitteredRow(draft.height * 2 / 3, draft.height, random, maxOffset = 1)),
            Position(1, jitteredRow(draft.height / 3, draft.height, random, maxOffset = 1)),
        )
    ringPoints.zipWithNext().forEach { (from, to) -> carveLine(draft, from, to) }
    carveLine(draft, ringPoints.last(), ringPoints.first())

    val spawns = listOf(ringPoints[0], ringPoints[2], ringPoints[4])
    spawns.forEach { spawn ->
        carveLine(draft, spawn, center)
        draft.setSpawn(spawn)
    }
    draft.setTarget(center, "Keep")
}

private fun populateCrossroads(
    draft: MutableMapDraft,
    random: Random,
) {
    val center = Position(draft.width / 2, draft.height / 2)
    val left = Position(0, jitteredRow(center.y, draft.height, random, maxOffset = 1))
    val right = Position(draft.width - 1, jitteredRow(center.y, draft.height, random, maxOffset = 1))
    val top = Position(jitteredColumn(center.x, draft.width, random, maxOffset = 2), 0)
    val bottom = Position(jitteredColumn(center.x, draft.width, random, maxOffset = 2), draft.height - 1)

    listOf(left, right, top, bottom).forEach { spawn ->
        carveLine(draft, spawn, center)
        draft.setSpawn(spawn)
    }
    draft.setTarget(center, "Cross Keep")
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

private fun carveLine(
    draft: MutableMapDraft,
    from: Position,
    to: Position,
) {
    hexLine(from, to, draft.width, draft.height).forEach { position ->
        draft.setTile(position, TileType.PATH)
        addAdjacentBuildAreas(draft, position)
    }
}

private fun setRiverPath(
    draft: MutableMapDraft,
    from: Position,
    to: Position,
) {
    hexLine(from, to, draft.width, draft.height).forEach { position ->
        draft.setRiver(position, RiverFlow.SOUTH_EAST)
    }
}

private fun jitteredRow(
    base: Int,
    height: Int,
    random: Random,
    maxOffset: Int,
): Int {
    val shifted = if (maxOffset > 0) base + random.nextInt(-maxOffset, maxOffset + 1) else base
    return shifted.coerceIn(0, height - 1)
}

private fun jitteredColumn(
    base: Int,
    width: Int,
    random: Random,
    maxOffset: Int,
): Int {
    val shifted = if (maxOffset > 0) base + random.nextInt(-maxOffset, maxOffset + 1) else base
    return shifted.coerceIn(0, width - 1)
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
