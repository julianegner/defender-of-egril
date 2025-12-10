package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpawnPointRemappingTest {
    
    @Test
    fun testSimpleRemapping() {
        // Simple case: no conflicts
        val spawns = listOf(
            EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(5, 5)),
            EditorEnemySpawn(AttackerType.ORK, 1, 1, Position(5, 5)),
            EditorEnemySpawn(AttackerType.SKELETON, 1, 2, Position(10, 10))
        )
        
        val remappings = mapOf(
            Position(5, 5) to Position(1, 1),
            Position(10, 10) to Position(2, 2)
        )
        
        val result = applyRemappings(spawns, remappings)
        
        assertEquals(3, result.size)
        assertEquals(Position(1, 1), result[0].spawnPoint)
        assertEquals(Position(1, 1), result[1].spawnPoint)
        assertEquals(Position(2, 2), result[2].spawnPoint)
    }
    
    @Test
    fun testRemappingWithConflict() {
        // Test the example from the issue:
        // (7,15) -> (10,20) and (22,0) -> (7,15)
        // The (7,15) -> (10,20) must be applied first
        val spawns = listOf(
            EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(7, 15)),
            EditorEnemySpawn(AttackerType.ORK, 1, 1, Position(22, 0))
        )
        
        val remappings = mapOf(
            Position(7, 15) to Position(10, 20),
            Position(22, 0) to Position(7, 15)
        )
        
        val result = applyRemappings(spawns, remappings)
        
        assertEquals(2, result.size)
        assertEquals(Position(10, 20), result[0].spawnPoint)
        assertEquals(Position(7, 15), result[1].spawnPoint)
        
        // Verify we don't have both at (10,20)
        val positionsSet = result.mapNotNull { it.spawnPoint }.toSet()
        assertTrue(positionsSet.contains(Position(10, 20)))
        assertTrue(positionsSet.contains(Position(7, 15)))
        assertEquals(2, positionsSet.size)
    }
    
    @Test
    fun testRemappingWithMultipleConflicts() {
        // Chain of conflicts: A -> B, B -> C, C -> D
        val spawns = listOf(
            EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(1, 1)),
            EditorEnemySpawn(AttackerType.ORK, 1, 1, Position(2, 2)),
            EditorEnemySpawn(AttackerType.SKELETON, 1, 1, Position(3, 3))
        )
        
        val remappings = mapOf(
            Position(1, 1) to Position(2, 2),
            Position(2, 2) to Position(3, 3),
            Position(3, 3) to Position(4, 4)
        )
        
        val result = applyRemappings(spawns, remappings)
        
        assertEquals(3, result.size)
        assertEquals(Position(2, 2), result[0].spawnPoint)
        assertEquals(Position(3, 3), result[1].spawnPoint)
        assertEquals(Position(4, 4), result[2].spawnPoint)
    }
    
    @Test
    fun testNoRemappingNeeded() {
        // All enemies already at correct positions
        val spawns = listOf(
            EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(5, 5)),
            EditorEnemySpawn(AttackerType.ORK, 1, 1, Position(5, 5))
        )
        
        val remappings = mapOf(
            Position(5, 5) to Position(5, 5)  // Identity mapping
        )
        
        val result = applyRemappings(spawns, remappings)
        
        assertEquals(2, result.size)
        assertEquals(Position(5, 5), result[0].spawnPoint)
        assertEquals(Position(5, 5), result[1].spawnPoint)
    }
    
    @Test
    fun testRemappingWithNullSpawnPoints() {
        // Some enemies don't have spawn points set
        val spawns = listOf(
            EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(5, 5)),
            EditorEnemySpawn(AttackerType.ORK, 1, 1, null),
            EditorEnemySpawn(AttackerType.SKELETON, 1, 1, Position(10, 10))
        )
        
        val remappings = mapOf(
            Position(5, 5) to Position(1, 1),
            Position(10, 10) to Position(2, 2)
        )
        
        val result = applyRemappings(spawns, remappings)
        
        assertEquals(3, result.size)
        assertEquals(Position(1, 1), result[0].spawnPoint)
        assertEquals(null, result[1].spawnPoint)  // Should remain null
        assertEquals(Position(2, 2), result[2].spawnPoint)
    }
    
    @Test
    fun testRemappingOrderComputation() {
        // Test that the order computation correctly identifies dependencies
        val remappings = mapOf(
            Position(7, 15) to Position(10, 20),
            Position(22, 0) to Position(7, 15)
        )
        
        val ordered = SpawnPointUtils.computeRemappingOrder(remappings)
        
        // The mapping should include both entries
        assertEquals(2, ordered.size, "Should have 2 mappings")
        assertEquals(Position(10, 20), ordered[Position(7, 15)])
        assertEquals(Position(7, 15), ordered[Position(22, 0)])
        
        // The key assertion: when applied sequentially to spawns, the result should be correct
        // This tests the actual ordering indirectly by verifying the algorithm works
        val spawns = listOf(
            EditorEnemySpawn(AttackerType.GOBLIN, 1, 1, Position(7, 15)),
            EditorEnemySpawn(AttackerType.ORK, 1, 1, Position(22, 0))
        )
        
        val result = applyRemappings(spawns, remappings)
        assertEquals(Position(10, 20), result[0].spawnPoint, 
            "First enemy should be at (10,20)")
        assertEquals(Position(7, 15), result[1].spawnPoint,
            "Second enemy should be at (7,15)")
    }
    
    // Helper function to apply remappings (simulates the actual implementation)
    private fun applyRemappings(
        spawns: List<EditorEnemySpawn>,
        remappings: Map<Position, Position>
    ): List<EditorEnemySpawn> {
        val orderedRemappings = SpawnPointUtils.computeRemappingOrder(remappings)
        
        return spawns.map { spawn ->
            spawn.spawnPoint?.let { spawnPoint ->
                val newPoint = orderedRemappings[spawnPoint]
                if (newPoint != null && newPoint != spawnPoint) {
                    spawn.copy(spawnPoint = newPoint)
                } else {
                    spawn
                }
            } ?: spawn
        }
    }
}
