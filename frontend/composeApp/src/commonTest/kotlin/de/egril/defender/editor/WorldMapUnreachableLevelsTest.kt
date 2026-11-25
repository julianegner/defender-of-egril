package de.egril.defender.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class WorldMapUnreachableLevelsTest {
    private fun level(
        id: String,
        isOfficial: Boolean = true,
        testingOnly: Boolean = false,
    ) = EditorLevel(
        id = id,
        mapId = "map",
        title = id,
        startCoins = 0,
        enemySpawns = emptyList(),
        availableTowers = emptySet(),
        isOfficial = isOfficial,
        testingOnly = testingOnly,
    )

    private val worldMap =
        WorldMapData(
            locations = listOf(
                WorldMapLocationData(id = "a", name = "a", position = WorldMapPoint(0, 0), levelIds = listOf("l1")),
                WorldMapLocationData(id = "b", name = "b", position = WorldMapPoint(0, 0), levelIds = listOf("l2")),
                WorldMapLocationData(id = "isle", name = "isle", position = WorldMapPoint(0, 0), levelIds = listOf("l3")),
            ),
            paths = listOf(WorldMapPathData(fromLocationId = "a", toLocationId = "b")),
        )

    private val sequence = listOf("l1", "l2", "l3", "l4", "l5")

    private val levels =
        listOf(
            level("l1"),
            level("l2"),
            level("l3"),
            level("l4"),
            level("l5", isOfficial = false),
            level("t1", isOfficial = true, testingOnly = true),
            level("t2", isOfficial = false, testingOnly = true),
        )

    @Test
    fun defaultFilterShowsOnlyOfficialNonTestLevelsNotReachable() {
        val result = findUnreachableLevels(levels, sequence, worldMap, officialOnly = true, includeTestLevels = false)
        assertEquals(listOf("l3", "l4"), result.map { it.id })
    }

    @Test
    fun disablingOfficialOnlyIncludesUserLevels() {
        val result = findUnreachableLevels(levels, sequence, worldMap, officialOnly = false, includeTestLevels = false)
        assertEquals(listOf("l3", "l4", "l5"), result.map { it.id })
    }

    @Test
    fun showingTestLevelsIncludesTestingOnlyLevels() {
        val result = findUnreachableLevels(levels, sequence, worldMap, officialOnly = true, includeTestLevels = true)
        assertEquals(listOf("l3", "l4", "t1"), result.map { it.id })
    }

    @Test
    fun levelsOnUnreachableLocationAreReported() {
        val disconnected = worldMap.copy(paths = emptyList())
        val result = findUnreachableLevels(levels, sequence, disconnected, officialOnly = true, includeTestLevels = false)
        assertEquals(listOf("l2", "l3", "l4"), result.map { it.id })
    }
}
