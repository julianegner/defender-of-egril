package de.egril.defender.editor

/**
 * Returns the location the player starts from: the first location holding a level of the
 * sequence, falling back to the first location of the map.
 */
fun worldMapStartLocation(sequence: List<String>, worldMap: WorldMapData): WorldMapLocationData? =
    sequence.firstNotNullOfOrNull { levelId ->
        worldMap.locations.firstOrNull { levelId in it.levelIds }
    } ?: worldMap.locations.firstOrNull()

/**
 * Returns the ids of all locations reachable from [worldMapStartLocation] via paths.
 */
fun worldMapReachableLocationIds(sequence: List<String>, worldMap: WorldMapData): Set<String> {
    val start = worldMapStartLocation(sequence, worldMap) ?: return emptySet()
    val adjacency = mutableMapOf<String, MutableSet<String>>()
    worldMap.paths.forEach { path ->
        adjacency.getOrPut(path.fromLocationId) { mutableSetOf() }.add(path.toLocationId)
        adjacency.getOrPut(path.toLocationId) { mutableSetOf() }.add(path.fromLocationId)
    }
    val reached = mutableSetOf(start.id)
    val queue = ArrayDeque(listOf(start.id))
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        adjacency[current].orEmpty().forEach { neighbor ->
            if (reached.add(neighbor)) queue.addLast(neighbor)
        }
    }
    return reached
}
