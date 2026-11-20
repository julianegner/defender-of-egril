package de.egril.defender.editor

import de.egril.defender.model.Position

data class TileReplacementArea(
    val from: Position,
    val to: Position,
)

/**
 * Replaces [sourceTileType] with [targetTileType] on a whole map or an optional rectangular area.
 *
 * NO_PLAY replacement also treats missing tile entries as implicit NO_PLAY and will materialize
 * those cells in the returned tile map (including area-limited operations).
 */
fun replaceTilesByType(
    tiles: Map<String, TileType>,
    mapWidth: Int,
    mapHeight: Int,
    sourceTileType: TileType,
    targetTileType: TileType,
    area: TileReplacementArea? = null,
): Pair<Map<String, TileType>, Set<String>> {
    if (mapWidth <= 0 || mapHeight <= 0) {
        return tiles to emptySet()
    }

    val minX =
        if (area == null) {
            0
        } else {
            maxOf(0, minOf(area.from.x, area.to.x))
        }
    val maxX =
        if (area == null) {
            mapWidth - 1
        } else {
            minOf(mapWidth - 1, maxOf(area.from.x, area.to.x))
        }
    val minY =
        if (area == null) {
            0
        } else {
            maxOf(0, minOf(area.from.y, area.to.y))
        }
    val maxY =
        if (area == null) {
            mapHeight - 1
        } else {
            minOf(mapHeight - 1, maxOf(area.from.y, area.to.y))
        }

    if (minX > maxX || minY > maxY) {
        return tiles to emptySet()
    }

    val updatedTiles = tiles.toMutableMap()
    val changedKeys = mutableSetOf<String>()
    for (x in minX..maxX) {
        for (y in minY..maxY) {
            val key = "$x,$y"
            val current = updatedTiles[key]
            val shouldReplace =
                if (sourceTileType == TileType.NO_PLAY) {
                    current == null || current == TileType.NO_PLAY
                } else {
                    current == sourceTileType
                }
            if (shouldReplace) {
                updatedTiles[key] = targetTileType
                changedKeys.add(key)
            }
        }
    }
    return updatedTiles to changedKeys
}
