package de.egril.defender.ui.gameplay

import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.math.PI
import kotlin.math.abs

class CircularSegmentDrawerTest {
    
    @Test
    fun testCalculateAngleToNeighbor_East() {
        val center = Position(5, 5)
        val east = Position(6, 5)
        val angle = CircularSegmentDrawer.calculateAngleToNeighbor(center, east)
        
        // East should be 0 radians
        assertTrue(abs(angle) < 0.1f, "East neighbor should be at 0 radians, got $angle")
    }
    
    @Test
    fun testCalculateAngleToNeighbor_West() {
        val center = Position(5, 5)
        val west = Position(4, 5)
        val angle = CircularSegmentDrawer.calculateAngleToNeighbor(center, west)
        
        // West should be PI radians (or -PI)
        assertTrue(abs(abs(angle) - PI.toFloat()) < 0.1f, "West neighbor should be at PI radians, got $angle")
    }
    
    @Test
    fun testCalculateAngleToNeighbor_North() {
        val center = Position(5, 6)
        val north = Position(5, 5)
        val angle = CircularSegmentDrawer.calculateAngleToNeighbor(center, north)
        
        // North should be positive (screen Y is inverted)
        // Due to hexagonal grid offset, the exact angle may vary
        assertTrue(angle > 0, "North neighbor should have positive angle, got $angle")
    }
    
    @Test
    fun testCalculateAngleToNeighbor_South() {
        val center = Position(5, 5)
        val south = Position(5, 6)
        val angle = CircularSegmentDrawer.calculateAngleToNeighbor(center, south)
        
        // South should be negative (screen Y is inverted)
        // Due to hexagonal grid offset, the exact angle may vary
        assertTrue(angle < 0, "South neighbor should have negative angle, got $angle")
    }
    
    @Test
    fun testHexagonalGridNeighbors_EvenRow() {
        // Test that we handle even row neighbors correctly
        val center = Position(5, 4) // even row
        val neighbors = center.getHexNeighbors()
        
        // Should have 6 neighbors
        assertTrue(neighbors.size == 6, "Should have 6 neighbors, got ${neighbors.size}")
        
        // All neighbors should have different angles
        val angles = neighbors.map { neighbor ->
            CircularSegmentDrawer.calculateAngleToNeighbor(center, neighbor)
        }
        
        // Check that angles are reasonably distributed
        assertTrue(angles.distinct().size == 6, "All neighbor angles should be distinct")
    }
    
    @Test
    fun testHexagonalGridNeighbors_OddRow() {
        // Test that we handle odd row neighbors correctly
        val center = Position(5, 5) // odd row
        val neighbors = center.getHexNeighbors()
        
        // Should have 6 neighbors
        assertTrue(neighbors.size == 6, "Should have 6 neighbors, got ${neighbors.size}")
        
        // All neighbors should have different angles
        val angles = neighbors.map { neighbor ->
            CircularSegmentDrawer.calculateAngleToNeighbor(center, neighbor)
        }
        
        // Check that angles are reasonably distributed
        assertTrue(angles.distinct().size == 6, "All neighbor angles should be distinct")
    }
}
