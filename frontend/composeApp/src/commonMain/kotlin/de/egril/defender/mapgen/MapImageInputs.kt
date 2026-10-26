package de.egril.defender.mapgen

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.mapgen.core.MapImageInput
import de.egril.defender.mapgen.core.MapImageTileKind
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.TileZone

/** Converts an editor map into the platform-neutral input used by the shared map renderer. */
internal fun EditorMap.toMapImageInput(): MapImageInput =
    MapImageInput(
        seed = id,
        width = width,
        height = height,
        tiles =
            tiles.mapNotNull { (key, type) ->
                parseGridKey(key)?.let { it to type.toMapImageTileKind() }
            }.toMap(),
        waterSpawnPoints =
            spawnPointInfoMap
                .filterValues { it == SpawnPointType.WATER }
                .keys
                .mapNotNull { parseGridKey(it) }
                .toSet(),
    )

/** Converts the tile overrides of a zone into the shared renderer's representation. */
internal fun TileZone.toMapImageTiles(): Map<Pair<Int, Int>, MapImageTileKind> =
    tiles
        .filterValues { it in TileZone.SUPPORTED_TILE_TYPES }
        .entries
        .associate { (position, type) -> (position.x to position.y) to type.toMapImageTileKind() }

private fun TileType.toMapImageTileKind(): MapImageTileKind = MapImageTileKind.valueOf(name)

private fun parseGridKey(key: String): Pair<Int, Int>? {
    val parts = key.split(",")
    if (parts.size != 2) return null
    val x = parts[0].toIntOrNull() ?: return null
    val y = parts[1].toIntOrNull() ?: return null
    return x to y
}
