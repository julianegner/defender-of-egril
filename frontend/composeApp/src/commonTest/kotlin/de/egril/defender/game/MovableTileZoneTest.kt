package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.TileType
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.MIN_ROTATABLE_REGION_TILES
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.RegionMove
import de.egril.defender.model.TileZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for moving movable tile regions: the terrain and every object on the region travel together,
 * rotation happens in place, and spawn points or map borders reject the move.
 */
class MovableTileZoneTest {
    private val islandA = Position(1, 2)
    private val islandB = Position(2, 2)

    /**
     * 8x3 map. Row 0 is the path with spawn (0,0) and target (6,0). The island is a movable zone
     * covering (1,2) and (2,2), which are build areas on the base map.
     */
    private fun createLevel(startPositions: List<Position> = listOf(Position(0, 0))): Level {
        val island =
            TileZone(
                id = "island",
                name = "Island",
                tiles = mapOf(islandA to TileType.PATH, islandB to TileType.PATH),
                movable = true,
            )
        return Level(
            id = 2,
            name = "Floating Island",
            gridWidth = 8,
            gridHeight = 3,
            startPositions = startPositions,
            targetPositions = listOf(Position(6, 0)),
            pathCells = (1..5).map { Position(it, 0) }.toSet(),
            buildAreas = setOf(islandA, islandB),
            attackerWaves = emptyList(),
            initialCoins = 100,
            tileZones = listOf(island),
        )
    }

    /** A seven-hex flower centred on (3,1) has an exact centre, so a spin around it is a true rotation. */
    private fun sevenHexFlower() = setOf(Position(3, 1), Position(4, 1), Position(2, 1), Position(3, 0), Position(4, 0), Position(3, 2), Position(4, 2))

    private fun flowerLevel(flower: Set<Position>): Level =
        createLevel().copy(
            tileZones = listOf(TileZone(id = "island", name = "Island", tiles = flower.associateWith { TileType.PATH }, movable = true)),
        )

    private fun attacker(
        id: Int,
        type: AttackerType,
        position: Position,
    ) = Attacker(id = id, type = type, position = mutableStateOf(position))

    @Test
    fun translatedRegionCarriesTerrainAndUnits() {
        val state = GameState(createLevel())
        state.attackers.add(attacker(1, AttackerType.GOBLIN, islandB))

        assertTrue(TileZoneSystem(state).moveZone("island", RegionMove(0, 4, 0)))

        assertEquals(setOf(Position(5, 2), Position(6, 2)), state.movedTileRegions["island"]?.keys)
        assertEquals(TileType.PATH, state.paintedTileTypeAt(Position(6, 2)))
        assertNull(state.paintedTileTypeAt(islandA), "The vacated tile reverts to the base map")
        assertEquals(Position(6, 2), state.attackers.single().position.value)
    }

    @Test
    fun destinationObjectsAreDestroyedWithoutReward() {
        val state = GameState(createLevel())
        state.attackers.add(attacker(1, AttackerType.GOBLIN, Position(5, 2)))

        assertTrue(TileZoneSystem(state).moveZone("island", RegionMove(0, 4, 0)))

        assertTrue(state.attackers.isEmpty(), "The unit on the destination is removed")
        assertEquals(100, state.coins.value, "Removal grants no reward")
    }

    @Test
    fun rotationKeepsRegionInPlace() {
        val flower = sevenHexFlower()
        val state = GameState(flowerLevel(flower))

        assertTrue(TileZoneSystem(state).moveZone("island", RegionMove(1, 0, 0)))
        // The flower is symmetric, so a 60 degree spin around its centre maps it onto itself.
        assertEquals(flower, state.movedTileRegions["island"]?.keys, "The region stays in place")
    }

    @Test
    fun sixRotationsOfSymmetricRegionAreAFullTurn() {
        val flower = sevenHexFlower()
        val state = GameState(flowerLevel(flower))
        val system = TileZoneSystem(state)

        repeat(6) { assertTrue(system.moveZone("island", RegionMove(1, 0, 0))) }
        assertEquals(flower, state.movedTileRegions["island"]?.keys, "Six rotations are a full turn")
    }

    @Test
    fun rotationOfRegionSmallerThanMinimumIsRejected() {
        val state = GameState(createLevel())
        val before = state.regionTilesOf(state.level.tileZones.single()).keys

        assertFalse(TileZoneSystem(state).moveZone("island", RegionMove(1, 0, 0)))
        assertTrue(state.movedTileRegions.isEmpty(), "A rejected rotation changes nothing")
        assertEquals(before, setOf(islandA, islandB))
    }

    @Test
    fun translationOfRegionSmallerThanMinimumIsStillAllowed() {
        val state = GameState(createLevel())

        assertTrue(TileZoneSystem(state).moveZone("island", RegionMove(0, 4, 0)))
        assertEquals(2, state.movedTileRegions["island"]?.size)
    }

    @Test
    fun minimumRotatableSizeIsSeven() {
        assertEquals(7, MIN_ROTATABLE_REGION_TILES)
    }

    @Test
    fun movingOntoSpawnPointIsRejected() {
        val state = GameState(createLevel(startPositions = listOf(Position(2, 2))))

        assertFalse(TileZoneSystem(state).moveZone("island", RegionMove(0, 1, 0)))
        assertTrue(state.movedTileRegions.isEmpty())
        assertEquals(setOf(islandA, islandB), state.regionTilesOf(state.level.tileZones.single()).keys)
    }

    @Test
    fun nonMovableZoneIsNotMoved() {
        val level = createLevel().let { it.copy(tileZones = it.tileZones.map { zone -> zone.copy(movable = false) }) }
        val state = GameState(level)

        assertFalse(TileZoneSystem(state).moveZone("island", RegionMove(0, 4, 0)))
    }

    /** Seven tiles with no symmetry, so its rounded centre is not the centre of the shape. */
    private fun irregularSevenTiles() =
        setOf(Position(5, 4), Position(6, 4), Position(4, 4), Position(5, 3), Position(5, 5), Position(6, 5), Position(7, 4))

    /** A larger map so that the irregular region can turn without touching the border or the spawn. */
    private fun irregularLevel(): Level =
        flowerLevel(irregularSevenTiles()).copy(gridWidth = 12, gridHeight = 9)

    @Test
    fun irregularRegionReturnsToStartAfterFullTurn() {
        val region = irregularSevenTiles()
        val state = GameState(irregularLevel())
        val system = TileZoneSystem(state)

        repeat(6) { assertTrue(system.moveZone("island", RegionMove(1, 0, 0))) }
        assertEquals(region, state.movedTileRegions["island"]?.keys, "Six rotations are a full turn")
    }

    @Test
    fun rotationKeepsPivotFixed() {
        val region = irregularSevenTiles()
        val state = GameState(irregularLevel())
        val system = TileZoneSystem(state)
        val pivot = RegionMove.centreOf(region)

        assertTrue(system.moveZone("island", RegionMove(1, 0, 0)))
        assertEquals(pivot, state.movedTileRegionPivots["island"], "A rotation does not move the pivot")
        assertTrue(system.moveZone("island", RegionMove(2, 0, 0)))
        assertEquals(pivot, state.movedTileRegionPivots["island"])
    }

    @Test
    fun shiftMovesPivotWithRegion() {
        val state = GameState(irregularLevel())
        val system = TileZoneSystem(state)
        val pivot = RegionMove.centreOf(irregularSevenTiles())

        assertTrue(system.moveZone("island", RegionMove(0, 1, 0)))
        assertEquals(Position(pivot.x + 1, pivot.y), state.movedTileRegionPivots["island"])
    }

    @Test
    fun movedRegionIsRestoredFromSaveData() {
        val original = GameState(createLevel())
        assertTrue(TileZoneSystem(original).moveZone("island", RegionMove(0, 4, 0)))

        val restored = GameState(createLevel())
        restored.movedTileRegions.putAll(original.movedTileRegions)
        TileZoneSystem(restored).restoreActiveZones()

        assertEquals(TileType.PATH, restored.paintedTileTypeAt(Position(6, 2)))
        assertNull(restored.paintedTileTypeAt(islandA))
    }
}

/** Move events fired by [EventScriptSystem] reach the same runtime move as the direct API. */
class MoveTileZoneEventTest {
    private fun levelWithMoveEvent(action: EventAction): Level {
        val flower = setOf(Position(3, 1), Position(4, 1), Position(2, 1), Position(3, 0), Position(4, 0), Position(3, 2), Position(4, 2))
        return Level(
            id = 4,
            name = "Event Move",
            gridWidth = 8,
            gridHeight = 4,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(7, 3)),
            pathCells = (0..7).map { Position(it, 3) }.toSet(),
            buildAreas = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            tileZones = listOf(TileZone(id = "flower", name = "Flower", tiles = flower.associateWith { TileType.PATH }, movable = true)),
            events =
                LevelEvents(
                    listOf(
                        LevelEvent(
                            id = "shift",
                            condition = EventCondition(type = EventConditionType.TURN_START, fromTurn = 1),
                            actions = listOf(action),
                        ),
                    ),
                ),
        )
    }

    @Test
    fun moveActionTranslatesZone() {
        val state = GameState(levelWithMoveEvent(EventAction(type = EventActionType.MOVE_TILE_ZONE, zoneId = "flower", moveDx = 1)))
        state.turnNumber.value = 1

        EventScriptSystem(state).evaluate(EventTrigger.PLAYER_TURN_START)

        assertEquals(setOf(Position(4, 1), Position(5, 1), Position(3, 1), Position(4, 0), Position(5, 0), Position(4, 2), Position(5, 2)), state.movedTileRegions["flower"]?.keys)
    }

    @Test
    fun rotateActionOnSmallZoneDoesNothing() {
        val level = levelWithMoveEvent(EventAction(type = EventActionType.MOVE_TILE_ZONE, zoneId = "flower", rotationSteps = 1))
        val small = level.copy(tileZones = listOf(TileZone(id = "flower", name = "Flower", tiles = mapOf(Position(3, 1) to TileType.PATH, Position(4, 1) to TileType.PATH), movable = true)))
        val state = GameState(small)
        state.turnNumber.value = 1

        EventScriptSystem(state).evaluate(EventTrigger.PLAYER_TURN_START)

        assertTrue(state.movedTileRegions.isEmpty())
    }
}
