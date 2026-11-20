package de.egril.defender.mapgen.core

/**
 * Platform-neutral description of a map, sufficient to render its background image.
 *
 * @property seed Value that makes the noise pattern deterministic (normally the map id)
 * @property width Grid width in tiles
 * @property height Grid height in tiles
 * @property tiles Tile kind per grid position (x, y); positions without an entry render as NO_PLAY
 * @property waterSpawnPoints Grid positions of spawn points placed on water; these render as RIVER
 */
class MapImageInput(
    val seed: String,
    val width: Int,
    val height: Int,
    val tiles: Map<Pair<Int, Int>, MapImageTileKind>,
    val waterSpawnPoints: Set<Pair<Int, Int>> = emptySet(),
)
