package de.egril.defender.model

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.TileType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Sandbox level behaviour on [GameState]:
 *  - A sandbox level can never be won, even when all enemies are gone.
 *  - A sandbox level can still be lost by losing all health points.
 *  - The instant "Win Level now" is never offered for sandbox levels.
 */
class SandboxLevelTest {
    private fun buildLevel(
        isSandbox: Boolean,
        healthPoints: Int = 10,
        spawnPlan: List<PlannedEnemySpawn> = emptyList(),
    ): Level =
        Level(
            id = 1,
            name = "Sandbox Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(9, 2)),
            pathCells = (0..9).map { Position(it, 2) }.toSet(),
            attackerWaves = emptyList(),
            directSpawnPlan = spawnPlan,
            healthPoints = healthPoints,
            isSandbox = isSandbox,
        )

    private fun playerTurnState(level: Level): GameState {
        val state = GameState(level)
        state.phase.value = GamePhase.PLAYER_TURN
        state.turnNumber.value = 5
        return state
    }

    @Test
    fun sandboxLevelIsNeverWonEvenWithNoEnemies() {
        val state = playerTurnState(buildLevel(isSandbox = true))
        // No enemies alive and nothing left to spawn: a normal level would be won here.
        assertFalse(state.isLevelWon())
    }

    @Test
    fun nonSandboxLevelIsWonWhenNoEnemiesRemain() {
        val state = playerTurnState(buildLevel(isSandbox = false))
        assertTrue(state.isLevelWon())
    }

    @Test
    fun sandboxLevelCanStillBeLostByLosingAllHealth() {
        val state = playerTurnState(buildLevel(isSandbox = true, healthPoints = 3))
        assertFalse(state.isLevelLost())
        state.healthPoints.value = 0
        assertTrue(state.isLevelLost())
    }

    @Test
    fun sandboxLevelNeverOffersInstantWin() {
        val state = playerTurnState(buildLevel(isSandbox = true, healthPoints = 100))
        state.attackers.add(
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(1, 2)),
            ),
        )
        assertFalse(state.canWinLevelNow())
    }

    @Test
    fun sandboxLevelAllowsFreePlacementAndUpgradeWithoutCoins() {
        val level =
            buildLevel(isSandbox = true).copy(
                buildAreas = setOf(Position(3, 2)),
                availableTowers = setOf(DefenderType.SPIKE_TOWER),
            )
        val state = playerTurnState(level)
        state.coins.value = 0
        // With no coins, a sandbox level still allows placement and upgrade.
        assertTrue(state.canPlaceDefender(DefenderType.SPIKE_TOWER))
    }

    @Test
    fun nonSandboxLevelRequiresCoinsToPlace() {
        val level =
            buildLevel(isSandbox = false).copy(
                buildAreas = setOf(Position(3, 2)),
                availableTowers = setOf(DefenderType.SPIKE_TOWER),
            )
        val state = playerTurnState(level)
        state.coins.value = 0
        assertFalse(state.canPlaceDefender(DefenderType.SPIKE_TOWER))
    }

    @Test
    fun sandboxPaintTileConvertsPathToBuildAreaAndBack() {
        val state = playerTurnState(buildLevel(isSandbox = true))
        val target = Position(4, 2)
        assertTrue(state.level.isOnPath(target))
        val versionBefore = state.mapEditVersion.value

        state.sandboxPaintTile(target, TileType.BUILD_AREA)
        assertFalse(state.level.isOnPath(target))
        assertTrue(state.level.isBuildArea(target))
        assertEquals(versionBefore + 1, state.mapEditVersion.value)

        state.sandboxPaintTile(target, TileType.PATH)
        assertTrue(state.level.isOnPath(target))
        assertFalse(state.level.isBuildArea(target))
    }

    @Test
    fun sandboxPaintTileNoPlayClearsTile() {
        val state = playerTurnState(buildLevel(isSandbox = true))
        val target = Position(4, 2)
        state.sandboxPaintTile(target, TileType.NO_PLAY)
        assertFalse(state.level.isOnPath(target))
        assertFalse(state.level.isBuildArea(target))
    }

    @Test
    fun sandboxPaintTileRecordsPaintedTilesForOverlay() {
        val state = playerTurnState(buildLevel(isSandbox = true))
        val target = Position(4, 2)
        assertTrue(state.sandboxPaintedTiles.isEmpty())

        state.sandboxPaintTile(target, TileType.BUILD_AREA)
        assertEquals(TileType.BUILD_AREA, state.sandboxPaintedTiles[target])

        // Repainting the same tile updates the recorded type.
        state.sandboxPaintTile(target, TileType.NO_PLAY)
        assertEquals(TileType.NO_PLAY, state.sandboxPaintedTiles[target])
    }

    @Test
    fun sandboxPaintTileDoesNotRecordForNonSandboxLevel() {
        val state = playerTurnState(buildLevel(isSandbox = false))
        state.sandboxPaintTile(Position(4, 2), TileType.BUILD_AREA)
        assertTrue(state.sandboxPaintedTiles.isEmpty())
    }

    @Test
    fun sandboxPaintTileIsNoOpForNonSandboxLevel() {
        val state = playerTurnState(buildLevel(isSandbox = false))
        val target = Position(4, 2)
        state.sandboxPaintTile(target, TileType.BUILD_AREA)
        // Unchanged: still on path, still no build area, version untouched.
        assertTrue(state.level.isOnPath(target))
        assertEquals(0, state.mapEditVersion.value)
    }
}
