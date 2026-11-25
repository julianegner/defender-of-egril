package de.egril.defender.editor

/**
 * Returns the levels the player cannot reach through the world map: levels that are not placed
 * on any location reachable from the start location.
 *
 * @param officialOnly if true, only levels from the official repository are returned
 * @param includeTestLevels if false, levels flagged [EditorLevel.testingOnly] are excluded
 */
fun findUnreachableLevels(
    levels: List<EditorLevel>,
    sequence: List<String>,
    worldMap: WorldMapData,
    officialOnly: Boolean,
    includeTestLevels: Boolean,
): List<EditorLevel> {
    val reachableLocations = worldMapReachableLocationIds(sequence, worldMap)
    val reachableLevelIds = worldMap.locations
        .filter { it.id in reachableLocations }
        .flatMap { it.levelIds }
        .toSet()
    val sequenceIndex = sequence.withIndex().associate { it.value to it.index }

    return levels
        .filter { it.id !in reachableLevelIds }
        .filter { !officialOnly || it.isOfficial }
        .filter { includeTestLevels || !it.testingOnly }
        .sortedBy { sequenceIndex[it.id] ?: Int.MAX_VALUE }
}
