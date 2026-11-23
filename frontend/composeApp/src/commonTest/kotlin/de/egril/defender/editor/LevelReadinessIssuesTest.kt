package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests that the editor reports every reason a level is not ready, not just a boolean.
 * Uses a map id that is never registered, so no filesystem map is required.
 */
class LevelReadinessIssuesTest {
    private val unknownMapId = "readiness_test_missing_map"

    private fun buildLevel(
        startCoins: Int = 100,
        startHealthPoints: Int = 10,
        availableTowers: Set<DefenderType> = setOf(DefenderType.SPIKE_TOWER),
        spawns: List<EditorEnemySpawn> = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(0, 0))),
    ): EditorLevel =
        EditorLevel(
            id = "readiness_level",
            mapId = unknownMapId,
            title = "Readiness Level",
            startCoins = startCoins,
            startHealthPoints = startHealthPoints,
            enemySpawns = spawns,
            availableTowers = availableTowers,
        )

    @Test
    fun missingMapIsReportedAlongsideOtherReasons() {
        val level = buildLevel(startCoins = 0, availableTowers = emptySet())

        val issues = EditorStorage.getLevelReadinessIssues(level)

        assertTrue(LevelReadinessIssue.NoTowers in issues)
        assertTrue(LevelReadinessIssue.NoStartCoins in issues)
        assertTrue(LevelReadinessIssue.MapMissing in issues)
        assertFalse(EditorStorage.isLevelReadyToPlay(level))
    }

    @Test
    fun missingSpawnsAndHealthAreReported() {
        val level = buildLevel(startHealthPoints = 0, spawns = emptyList())

        val issues = EditorStorage.getLevelReadinessIssues(level)

        assertTrue(LevelReadinessIssue.NoSpawns in issues)
        assertTrue(LevelReadinessIssue.NoStartHealth in issues)
        assertFalse(LevelReadinessIssue.NoTowers in issues)
        assertFalse(LevelReadinessIssue.NoStartCoins in issues)
    }

    @Test
    fun levelWithOnlyMissingMapHasSingleReason() {
        val issues = EditorStorage.getLevelReadinessIssues(buildLevel())

        assertEquals(listOf<LevelReadinessIssue>(LevelReadinessIssue.MapMissing), issues)
    }

    @Test
    fun autoTowerBaseCountsAsNoBuildableTileFallback() {
        assertFalse(buildLevel().hasNoBuildableTileFallback())
        assertTrue(buildLevel().copy(autoTowerBase = true).hasNoBuildableTileFallback())
    }
}
