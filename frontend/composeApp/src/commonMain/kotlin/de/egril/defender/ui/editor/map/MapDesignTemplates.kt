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
    }
    return EditorMap(
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
}

private data class MutableMapDraft(
    val width: Int,
    val height: Int,
    val tiles: MutableMap<String, TileType> = mutableMapOf(),
    val riverTiles: MutableMap<String, RiverTile> = mutableMapOf(),
    val targetInfoMap: MutableMap<String, EditorTargetInfo> = mutableMapOf(),
    val spawnPointInfoMap: MutableMap<String, SpawnPointType> = mutableMapOf(),
) {
    fun setTile(position: Position, type: TileType) {
        if (position.x !in 0 until width || position.y !in 0 until height) return
        tiles["${position.x},${position.y}"] = type
    }

    fun setSpawn(position: Position, spawnPointType: SpawnPointType = SpawnPointType.LAND) {
        setTile(position, TileType.SPAWN_POINT)
        spawnPointInfoMap["${position.x},${position.y}"] = spawnPointType
    }

    fun setTarget(position: Position, name: String = "") {
        setTile(position, TileType.TARGET)
        targetInfoMap["${position.x},${position.y}"] = EditorTargetInfo(name = name, type = TargetType.STANDARD)
    }

    fun setRiver(position: Position, flow: RiverFlow) {
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

private fun addAdjacentBuildAreas(
    draft: MutableMapDraft,
    position: Position,
) {
    position.getHexNeighbors()
        .filter { it.x in 0 until draft.width && it.y in 0 until draft.height }
        .forEach { neighbor ->
            val key = "${neighbor.x},${neighbor.y}"
            if (draft.tiles[key] == null) {
                draft.tiles[key] = TileType.BUILD_AREA
            }
        }
}
