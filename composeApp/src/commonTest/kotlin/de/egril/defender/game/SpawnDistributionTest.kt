package de.egril.defender.game

import de.egril.defender.editor.EditorStorage
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Test that enemy spawns are distributed correctly across turns
 * without creating empty turns between spawns.
 */
class SpawnDistributionTest {
    
    @Test
    fun testLevel3HasConsecutiveSpawns() {
        // Get levels from the level data
        val levels = LevelData.createLevels()
        
        // Skip test if no levels are available (e.g., in test environment without repository files)
        if (levels.isEmpty()) {
            println("Skipping test: No levels available (likely test environment without repository files)")
            return
        }
        
        // Try to find level with id 3 (old system) or just use the 3rd level if available
        val level3 = levels.find { it.id == 3 } ?: levels.getOrNull(2)
        
        assertTrue(level3 != null, "Level 3 (or 3rd level) should exist")
        
        // Get the spawn plan
        val spawnPlan = level3!!.directSpawnPlan
        assertTrue(spawnPlan != null, "Level should have a direct spawn plan")
        assertTrue(spawnPlan!!.isNotEmpty(), "Level should have spawns")
        
        // Group spawns by turn
        val spawnsByTurn = spawnPlan.groupBy { it.spawnTurn }
        val turns = spawnsByTurn.keys.sorted()
        
        // Check for empty turns in the first 20 turns
        val emptyTurns = mutableListOf<Int>()
        for (turn in 1..minOf(20, turns.maxOrNull() ?: 0)) {
            if (!spawnsByTurn.containsKey(turn)) {
                emptyTurns.add(turn)
            }
        }
        
        // There should be very few empty turns (only intentional gaps between waves)
        assertTrue(
            emptyTurns.size <= 3,
            "Level should have minimal empty turns in first 20 turns. Empty turns: $emptyTurns"
        )
        
        println("Level ${level3.id} spawn distribution:")
        println("Total spawns: ${spawnPlan.size}")
        println("Turns with spawns: ${turns.size}")
        println("Empty turns in first 20: $emptyTurns")
        println("Average spawns per turn: ${spawnPlan.size.toFloat() / turns.size}")
    }
    
    @Test
    fun testAllLevelsHaveSpawns() {
        val levels = LevelData.createLevels()
        
        for (level in levels) {
            val spawnPlan = level.directSpawnPlan
            assertTrue(
                spawnPlan != null && spawnPlan.isNotEmpty(),
                "Level ${level.id} should have spawns"
            )
            
            // Verify at least one spawn on turn 1
            val firstTurnSpawns = spawnPlan!!.count { it.spawnTurn == 1 }
            assertTrue(
                firstTurnSpawns > 0,
                "Level ${level.id} should have at least one spawn on turn 1"
            )
        }
    }
    
    @Test
    fun testSpawnsRespectSpawnPointCount() {
        val levels = LevelData.createLevels()
        
        for (level in levels) {
            val spawnPlan = level.directSpawnPlan ?: continue
            val spawnPoints = level.startPositions
            
            // Group spawns by turn and verify none exceed a reasonable limit
            val spawnsByTurn = spawnPlan.groupBy { it.spawnTurn }
            
            for ((turn, spawns) in spawnsByTurn) {
                // We can spawn any number per turn (cycling through spawn points)
                // Just verify it's not an absurdly high number
                assertTrue(
                    spawns.size <= 20,
                    "Level ${level.id} turn $turn has ${spawns.size} spawns, which seems excessive"
                )
            }
        }
    }
    
    @Test
    fun testInitialSpawnNotLimitedToSix() {
        // Create a test level with more than 6 enemies on turn 1
        // Create a longer path to accommodate all enemies
        val pathCells = mutableSetOf<Position>()
        for (x in 0..9) {
            pathCells.add(Position(x, 2))
            pathCells.add(Position(x, 3))  // Add extra row for more space
        }
        
        // Create spawn plan with 10 enemies on turn 1
        val spawnPlan = List(10) { 
            PlannedEnemySpawn(AttackerType.GOBLIN, 1, 1)
        }
        
        val level = Level(
            id = 99,
            name = "Test Initial Spawn",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(9, 2)),
            pathCells = pathCells,
            buildIslands = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            directSpawnPlan = spawnPlan
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Start the game (this triggers initial spawn)
        engine.startFirstPlayerTurn()
        
        // Verify all 10 enemies were spawned, not just 6
        assertEquals(
            10,
            state.attackers.size,
            "All 10 enemies scheduled for turn 1 should be spawned, not limited to 6"
        )
    }
}
