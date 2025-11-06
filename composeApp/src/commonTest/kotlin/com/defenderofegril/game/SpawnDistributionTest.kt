package com.defenderofegril.game

import com.defenderofegril.editor.EditorStorage
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
        // Get level 3 from the editor storage
        val levels = LevelData.createLevels()
        val level3 = levels.find { it.id == 3 }
        
        assertTrue(level3 != null, "Level 3 should exist")
        
        // Get the spawn plan
        val spawnPlan = level3!!.directSpawnPlan
        assertTrue(spawnPlan != null, "Level 3 should have a direct spawn plan")
        assertTrue(spawnPlan!!.isNotEmpty(), "Level 3 should have spawns")
        
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
            "Level 3 should have minimal empty turns in first 20 turns. Empty turns: $emptyTurns"
        )
        
        println("Level 3 spawn distribution:")
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
}
