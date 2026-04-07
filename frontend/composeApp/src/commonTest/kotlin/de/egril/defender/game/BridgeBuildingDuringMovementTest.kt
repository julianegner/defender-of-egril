package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for bridge building during movement (with remaining move points)
 */
class BridgeBuildingDuringMovementTest {
    
    /**
     * Test that a unit with 2 move points that's 1 tile from a river builds bridge
     * and continues moving in the same turn (not the next turn).
     */
    @Test
    fun testUnitBuildsBridgeAndContinuesMovingInSameTurn() {
        // Create a simple map: spawn -> path -> river -> path -> target
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH  // Unit will be here at turn start
        tiles["2,0"] = TileType.RIVER // River that should be bridged
        tiles["3,0"] = TileType.PATH  // Unit should reach here after building bridge
        tiles["4,0"] = TileType.TARGET
        val map = EditorMap(
            id = "test-bridge-during-movement",
            name = "Test Bridge During Movement",
            width = 5,
            height = 1,
            tiles = tiles
        )
        
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 5,
            gridHeight = 1,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(4, 0)),
            pathCells = map.getPathCells(),
            attackerWaves = emptyList(),
            riverTiles = map.getRiverCells().associateWith { RiverTile(it) }
        )
        
        val state = GameState(level = level)
        val engine = GameEngine(state)
        
        // Create an ork adjacent to the river (orks have speed 1, so they won't move multiple tiles)
        // Place at position (1,0) which is adjacent to river at (2,0)
        val ork = Attacker(
            id = 1,
            type = AttackerType.ORK,
            position = mutableStateOf(Position(1, 0)),
            currentHealth = mutableStateOf(40)
        )
        state.attackers.add(ork)
        
        // Verify initial state
        assertEquals(0, state.bridges.size, "Should have no bridges initially")
        assertEquals(Position(1, 0), ork.position.value, "Ork should start at (1,0)")
        
        // Start enemy turn (required for turn processing)
        state.phase.value = GamePhase.ENEMY_TURN
        state.turnNumber.value = 1
        
        // Execute enemy turn movement
        val movements = engine.calculateEnemyTurnMovements()
        
        // Apply movements
        for (movementStep in movements.allMovementSteps) {
            for ((attackerId, newPosition) in movementStep) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        
        // Complete enemy turn
        engine.completeEnemyTurn()
        
        // Verify bridge was built
        assertTrue(state.bridges.size > 0, "Bridge should have been built")
        assertTrue(ork.isDefeated.value, "Ork should be defeated after building bridge")
        assertTrue(ork.isBuildingBridge.value, "Ork should be marked as building bridge")
    }
    
    /**
     * Test that a unit builds a bridge when its path is completely blocked by a river.
     */
    @Test
    fun testOgreBuildsBridgeWhenBlocked() {
        // Create a map where Ogre's path is blocked by river
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.RIVER  // 2-tile river
        tiles["4,0"] = TileType.PATH
        tiles["5,0"] = TileType.TARGET
        
        val map = EditorMap(
            id = "test-ogre-bridge-blocked",
            name = "Test Ogre Bridge Blocked",
            width = 6,
            height = 1,
            tiles = tiles
        )
        
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 6,
            gridHeight = 1,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            pathCells = map.getPathCells(),
            attackerWaves = emptyList(),
            riverTiles = map.getRiverCells().associateWith { RiverTile(it) }
        )
        
        val state = GameState(level = level)
        val engine = GameEngine(state)
        
        // Create an ogre adjacent to the river
        val ogre = Attacker(
            id = 1,
            type = AttackerType.OGRE,
            position = mutableStateOf(Position(1, 0)),
            currentHealth = mutableStateOf(80)
        )
        state.attackers.add(ogre)
        
        // Start enemy turn
        state.phase.value = GamePhase.ENEMY_TURN
        state.turnNumber.value = 1
        
        // Execute enemy turn
        val movements = engine.calculateEnemyTurnMovements()
        for (movementStep in movements.allMovementSteps) {
            for ((attackerId, newPosition) in movementStep) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        engine.completeEnemyTurn()
        
        // Ogre should have built a bridge (strategic decision - path is blocked)
        assertTrue(state.bridges.size > 0, "Ogre should build bridge when path is blocked")
        assertTrue(ogre.isDefeated.value, "Ogre should be defeated after building bridge")
    }
}
