package de.egril.defender.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.TileType
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for raft functionality - towers on river tiles
 */
class RaftTest {
    
    /**
     * Test that placing a tower on a river tile creates a raft
     */
    @Test
    fun testPlaceTowerOnRiverCreatesRaft() {
        // Create a simple map with river tiles
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.PATH
        tiles["4,0"] = TileType.TARGET
        
        val riverTilesMap = mapOf(
            Position(2, 0) to RiverTile(
                position = Position(2, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            )
        )
        
        
        val level = Level(
            id = 1,
            name = "Test Raft Level",
            pathCells = setOf(Position(1, 0), Position(3, 0)),
            buildAreas = setOf(Position(1, 0), Position(2, 0), Position(3, 0)),  // Can build on river
            attackerWaves = emptyList(),
            initialCoins = 100,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(4, 0)),
            riverTiles = riverTilesMap
        )
        
        val state = GameState(level = level)
        val engine = GameEngine(state)
        
        // Place a tower on the river tile
        val success = engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 0))
        
        assertTrue(success, "Should be able to place tower on river")
        assertEquals(1, state.defenders.size, "Should have one defender")
        assertEquals(1, state.rafts.size, "Should have one raft")
        
        val defender = state.defenders.first()
        val raft = state.rafts.first()
        
        assertEquals(defender.id, raft.defenderId, "Raft should reference the defender")
        assertEquals(Position(2, 0), raft.currentPosition.value, "Raft should be at river position")
        assertEquals(raft.id, defender.raftId.value, "Defender should reference the raft")
    }
    
    /**
     * Test that rafts move according to river flow
     */
    @Test
    fun testRaftMovesWithRiverFlow() {
        // Create a map with flowing river
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.RIVER
        tiles["4,0"] = TileType.PATH
        tiles["5,0"] = TileType.TARGET
        
        val riverTilesMap = mapOf(
            Position(2, 0) to RiverTile(
                position = Position(2, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            ),
            Position(3, 0) to RiverTile(
                position = Position(3, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            )
        )
        
        
        val level = Level(
            id = 1,
            name = "Test Raft Flow Level",
            pathCells = setOf(Position(1, 0), Position(4, 0)),
            buildAreas = setOf(Position(1, 0), Position(2, 0), Position(3, 0), Position(4, 0)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            riverTiles = riverTilesMap
        )
        
        val state = GameState(level = level)
        val engine = GameEngine(state)
        
        // Place a tower on the first river tile
        engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 0))
        
        val defender = state.defenders.first()
        val raft = state.rafts.first()
        
        assertEquals(Position(2, 0), raft.currentPosition.value, "Raft starts at position (2,0)")
        assertEquals(Position(2, 0), defender.position.value, "Defender starts at position (2,0)")
        
        // Process raft movements (simulate end of turn)
        val raftSystem = RaftSystem(state)
        raftSystem.processRaftMovements()
        
        // Raft should move one tile to the east
        assertEquals(Position(3, 0), raft.currentPosition.value, "Raft should move to (3,0)")
        assertEquals(Position(3, 0), defender.position.value, "Defender should move with raft to (3,0)")
    }
    
    /**
     * Test that rafts move 2 tiles with flow speed 2
     */
    @Test
    fun testRaftMovesWithFlowSpeed2() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.RIVER
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.RIVER
        tiles["4,0"] = TileType.PATH
        tiles["5,0"] = TileType.TARGET
        
        val riverTilesMap = mapOf(
            Position(1, 0) to RiverTile(
                position = Position(1, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 2
            ),
            Position(2, 0) to RiverTile(
                position = Position(2, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 2
            ),
            Position(3, 0) to RiverTile(
                position = Position(3, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 2
            )
        )
        
        val level = Level(
            id = 1,
            name = "Test Fast Flow Level",
            pathCells = setOf(Position(4, 0)),
            buildAreas = setOf(Position(1, 0), Position(2, 0), Position(3, 0), Position(4, 0)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            riverTiles = riverTilesMap
        )
        
        val state = GameState(level = level)
        val engine = GameEngine(state)
        
        engine.placeDefender(DefenderType.BOW_TOWER, Position(1, 0))
        
        val raft = state.rafts.first()
        assertEquals(Position(1, 0), raft.currentPosition.value)
        
        val raftSystem = RaftSystem(state)
        raftSystem.processRaftMovements()
        
        // With flow speed 2, raft should move 2 tiles east
        assertEquals(Position(3, 0), raft.currentPosition.value, "Raft should move 2 tiles with speed 2")
    }
    
    /**
     * Test that rafts are destroyed in maelstroms
     */
    @Test
    fun testRaftDestroyedInMaelstrom() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.RIVER  // Start here - flowing east
        tiles["2,0"] = TileType.RIVER  // Maelstrom
        tiles["3,0"] = TileType.PATH
        tiles["4,0"] = TileType.TARGET
        
        val riverTilesMap = mapOf(
            Position(1, 0) to RiverTile(
                position = Position(1, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            ),
            Position(2, 0) to RiverTile(
                position = Position(2, 0),
                flowDirection = RiverFlow.MAELSTROM,
                flowSpeed = 1
            )
        )
        
        val level = Level(
            id = 1,
            name = "Test Maelstrom Level",
            pathCells = setOf(Position(3, 0)),
            buildAreas = setOf(Position(1, 0), Position(2, 0), Position(3, 0)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(4, 0)),
            riverTiles = riverTilesMap
        )
        
        val state = GameState(level = level)
        val engine = GameEngine(state)
        
        // Place tower on flowing river tile - it will flow into the maelstrom on movement
        engine.placeDefender(DefenderType.BOW_TOWER, Position(1, 0))
        
        assertEquals(1, state.defenders.size)
        assertEquals(1, state.rafts.size)
        
        val raftSystem = RaftSystem(state)
        raftSystem.processRaftMovements()
        
        // Raft and tower should be destroyed by maelstrom
        assertEquals(0, state.defenders.size, "Tower should be destroyed in maelstrom")
        assertTrue(state.rafts.isEmpty() || !state.rafts.first().isActive, "Raft should be destroyed in maelstrom")
    }
    
    /**
     * Test that rafts are destroyed when moved out of bounds
     */
    @Test
    fun testRaftDestroyedWhenMovedOutOfBounds() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER  // Last tile, flow goes out of map
        
        val riverTilesMap = mapOf(
            Position(2, 0) to RiverTile(
                position = Position(2, 0),
                flowDirection = RiverFlow.EAST,  // Points out of map
                flowSpeed = 1
            )
        )
        
        val level = Level(
            id = 1,
            name = "Test Out of Bounds Level",
            gridWidth = 3,
            gridHeight = 1,
            pathCells = setOf(Position(1, 0)),
            buildAreas = setOf(Position(1, 0), Position(2, 0)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(1, 0)),
            riverTiles = riverTilesMap
        )
        
        val state = GameState(level = level)
        val engine = GameEngine(state)
        
        engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 0))
        
        assertEquals(1, state.defenders.size)
        assertEquals(1, state.rafts.size)
        
        val raftSystem = RaftSystem(state)
        raftSystem.processRaftMovements()
        
        // Raft and tower should be destroyed when moved out of bounds
        assertEquals(0, state.defenders.size, "Tower should be destroyed when raft goes out of bounds")
    }
    
    /**
     * Test that rafts are blocked by bridges
     */
    @Test
    fun testRaftBlockedByBridge() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.RIVER
        tiles["2,0"] = TileType.RIVER  // Bridge here
        tiles["3,0"] = TileType.PATH
        tiles["4,0"] = TileType.TARGET
        
        val riverTilesMap = mapOf(
            Position(1, 0) to RiverTile(
                position = Position(1, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            ),
            Position(2, 0) to RiverTile(
                position = Position(2, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            )
        )
        
        val level = Level(
            id = 1,
            name = "Test Bridge Block Level",
            pathCells = setOf(Position(3, 0)),
            buildAreas = setOf(Position(1, 0), Position(2, 0), Position(3, 0)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(4, 0)),
            riverTiles = riverTilesMap
        )
        
        val state = GameState(level = level)
        val engine = GameEngine(state)
        
        // Create a bridge at position (2,0)
        val bridge = Bridge(
            id = 1,
            type = BridgeType.WOODEN,
            positions = listOf(Position(2, 0)),
            currentHealth = mutableStateOf(40),
            createdByAttackerId = 1,
            createdOnTurn = 1
        )
        state.bridges.add(bridge)
        
        // Place tower upstream of bridge
        engine.placeDefender(DefenderType.BOW_TOWER, Position(1, 0))
        
        val raft = state.rafts.first()
        assertEquals(Position(1, 0), raft.currentPosition.value)
        
        val raftSystem = RaftSystem(state)
        raftSystem.processRaftMovements()
        
        // Raft should not move because bridge blocks it
        assertEquals(Position(1, 0), raft.currentPosition.value, "Raft should be blocked by bridge")
    }
    
    /**
     * Test that rafts cannot pass through other rafts
     */
    @Test
    fun testRaftsBlockedByOtherRafts() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.RIVER
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.RIVER
        tiles["4,0"] = TileType.PATH
        tiles["5,0"] = TileType.TARGET
        
        val riverTilesMap = mapOf(
            Position(1, 0) to RiverTile(
                position = Position(1, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            ),
            Position(2, 0) to RiverTile(
                position = Position(2, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            ),
            Position(3, 0) to RiverTile(
                position = Position(3, 0),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            )
        )
        
        val level = Level(
            id = 1,
            name = "Test Raft Blocking Level",
            pathCells = setOf(Position(4, 0)),
            buildAreas = setOf(Position(1, 0), Position(2, 0), Position(3, 0), Position(4, 0)),
            attackerWaves = emptyList(),
            initialCoins = 200,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            riverTiles = riverTilesMap
        )
        
        val state = GameState(level = level)
        val engine = GameEngine(state)
        
        // Place two towers on consecutive river tiles
        engine.placeDefender(DefenderType.BOW_TOWER, Position(1, 0))
        engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 0))
        
        assertEquals(2, state.defenders.size)
        assertEquals(2, state.rafts.size)
        
        val raft1 = state.rafts[0]
        val raft2 = state.rafts[1]
        
        assertEquals(Position(1, 0), raft1.currentPosition.value)
        assertEquals(Position(2, 0), raft2.currentPosition.value)
        
        val raftSystem = RaftSystem(state)
        raftSystem.processRaftMovements()
        
        // Raft2 should move first (it's downstream), then raft1 can move into its old position
        assertEquals(Position(2, 0), raft1.currentPosition.value, "Raft1 should move to raft2's old position")
        assertEquals(Position(3, 0), raft2.currentPosition.value, "Raft2 should move downstream")
    }

    /**
     * Test that placing a tower on a NONE flow river tile is not allowed
     */
    @Test
    fun testCannotPlaceTowerOnNoneFlowRiverTile() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.PATH
        tiles["4,0"] = TileType.TARGET

        val riverTilesMap = mapOf(
            Position(2, 0) to RiverTile(
                position = Position(2, 0),
                flowDirection = RiverFlow.NONE,
                flowSpeed = 1
            )
        )

        val level = Level(
            id = 1,
            name = "Test Still River Level",
            pathCells = setOf(Position(1, 0), Position(3, 0)),
            buildAreas = setOf(Position(1, 0), Position(2, 0), Position(3, 0)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(4, 0)),
            riverTiles = riverTilesMap
        )

        val state = GameState(level = level)
        val engine = GameEngine(state)

        val success = engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 0))

        assertFalse(success, "Should not be able to place tower on NONE flow river tile")
        assertEquals(0, state.defenders.size, "Should have no defenders")
        assertEquals(0, state.rafts.size, "Should have no rafts")
    }

    /**
     * Test that placing a tower on a MAELSTROM river tile is not allowed
     */
    @Test
    fun testCannotPlaceTowerOnMaelstromRiverTile() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.PATH
        tiles["4,0"] = TileType.TARGET

        val riverTilesMap = mapOf(
            Position(2, 0) to RiverTile(
                position = Position(2, 0),
                flowDirection = RiverFlow.MAELSTROM,
                flowSpeed = 1
            )
        )

        val level = Level(
            id = 1,
            name = "Test Maelstrom River Level",
            pathCells = setOf(Position(1, 0), Position(3, 0)),
            buildAreas = setOf(Position(1, 0), Position(2, 0), Position(3, 0)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(4, 0)),
            riverTiles = riverTilesMap
        )

        val state = GameState(level = level)
        val engine = GameEngine(state)

        val success = engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 0))

        assertFalse(success, "Should not be able to place tower on MAELSTROM river tile")
        assertEquals(0, state.defenders.size, "Should have no defenders")
        assertEquals(0, state.rafts.size, "Should have no rafts")
    }
}
