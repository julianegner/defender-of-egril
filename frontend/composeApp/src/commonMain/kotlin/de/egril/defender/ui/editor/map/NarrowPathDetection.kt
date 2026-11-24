package de.egril.defender.ui.editor.map

import androidx.compose.ui.graphics.Color
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors

/** Highlight color for narrow path tiles; chosen to stand out against brown path tiles. */
internal val NARROW_PATH_HIGHLIGHT_COLOR = Color(0xFFFF00FF)

/** Tile types enemies can walk on. Bridges and rivers are not part of the editor's tile types. */
private val WALKABLE_TILE_TYPES = setOf(TileType.PATH, TileType.SPAWN_POINT, TileType.TARGET)

/**
 * Returns walkable tiles that are bottlenecks: if such a tile were not a path, the path would be
 * cut, i.e. two of its walkable neighbors could no longer reach each other. The neighbor count
 * does not matter, so a tile with 4 path neighbors can still be a bottleneck.
 */
internal fun EditorMap.findNarrowPathPositions(): Set<Position> {
    val walkable =
        tiles
            .mapNotNull { (key, type) ->
                if (type in WALKABLE_TILE_TYPES) parseTileKey(key) else null
            }.toSet()

    return walkable.filter { isBottleneck(it, walkable) }.toSet()
}

/**
 * Checks whether removing [tile] disconnects its walkable neighbors. Searches from the first
 * neighbor (without passing through [tile]) and reports a bottleneck if any other neighbor is
 * not reached.
 */
private fun isBottleneck(tile: Position, walkable: Set<Position>): Boolean {
    val neighbors = tile.getHexNeighbors().filter { it in walkable }
    if (neighbors.size < 2) return false

    val reached = mutableSetOf(neighbors.first())
    val unreached = neighbors.drop(1).toMutableSet()
    val queue = ArrayDeque(listOf(neighbors.first()))

    while (queue.isNotEmpty() && unreached.isNotEmpty()) {
        val current = queue.removeFirst()
        for (next in current.getHexNeighbors()) {
            if (next == tile || next !in walkable || !reached.add(next)) continue
            unreached.remove(next)
            queue.addLast(next)
        }
    }
    return unreached.isNotEmpty()
}

private fun parseTileKey(key: String): Position? {
    val parts = key.split(",")
    if (parts.size != 2) return null
    val x = parts[0].trim().toIntOrNull() ?: return null
    val y = parts[1].trim().toIntOrNull() ?: return null
    return Position(x, y)
}
