package de.egril.defender.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldMapConsistencyCheckerTest {
    private fun location(
        id: String,
        vararg levelIds: String,
    ) = WorldMapLocationData(
        id = id,
        name = id,
        position = WorldMapPoint(0, 0),
        levelIds = levelIds.toList(),
    )

    private fun path(
        from: String,
        to: String,
    ) = WorldMapPathData(fromLocationId = from, toLocationId = to)

    @Test
    fun consistentDataReportsNoIssues() {
        val worldMap =
            WorldMapData(
                locations = listOf(location("a", "l1"), location("b", "l2")),
                paths = listOf(path("a", "b")),
            )
        assertTrue(WorldMapConsistencyChecker.check(listOf("l1", "l2"), worldMap).isEmpty())
    }

    @Test
    fun levelInSequenceButNotPlacedIsWarning() {
        val worldMap = WorldMapData(locations = listOf(location("a", "l1")))
        val issues = WorldMapConsistencyChecker.check(listOf("l1", "l2"), worldMap)
        assertEquals(1, issues.size)
        assertEquals(ConsistencySeverity.WARNING, issues.single().severity)
        assertTrue(issues.single().message.contains("'l2'"))
    }

    @Test
    fun levelPlacedButNotInSequenceIsError() {
        val worldMap = WorldMapData(locations = listOf(location("a", "l1", "ghost")))
        val issues = WorldMapConsistencyChecker.check(listOf("l1"), worldMap)
        assertEquals(1, issues.size)
        assertEquals(ConsistencySeverity.ERROR, issues.single().severity)
        assertTrue(issues.single().message.contains("'ghost'"))
    }

    @Test
    fun levelPlacedOnTwoLocationsIsError() {
        val worldMap = WorldMapData(locations = listOf(location("a", "l1"), location("b", "l1")))
        val issues = WorldMapConsistencyChecker.check(listOf("l1"), worldMap)
        assertTrue(issues.any { it.severity == ConsistencySeverity.ERROR && it.message.contains("multiple") })
    }

    @Test
    fun pathReferencingUnknownLocationIsError() {
        val worldMap =
            WorldMapData(
                locations = listOf(location("a", "l1")),
                paths = listOf(path("a", "missing")),
            )
        val issues = WorldMapConsistencyChecker.check(listOf("l1"), worldMap)
        assertEquals(ConsistencySeverity.ERROR, issues.single().severity)
        assertTrue(issues.single().message.contains("'missing'"))
    }

    @Test
    fun locationWithoutPathFromStartIsWarning() {
        val worldMap =
            WorldMapData(
                locations = listOf(location("a", "l1"), location("b", "l2"), location("c", "l3")),
                paths = listOf(path("a", "b")),
            )
        val issues = WorldMapConsistencyChecker.check(listOf("l1", "l2", "l3"), worldMap)
        assertEquals(1, issues.size)
        assertTrue(issues.single().message.contains("'c'"))
    }

    @Test
    fun pathsAreTreatedAsBidirectional() {
        val worldMap =
            WorldMapData(
                locations = listOf(location("a", "l1"), location("b", "l2"), location("c", "l3")),
                paths = listOf(path("a", "b"), path("c", "b")),
            )
        assertTrue(WorldMapConsistencyChecker.check(listOf("l1", "l2", "l3"), worldMap).isEmpty())
    }
}
