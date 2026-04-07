package de.egril.defender.ui

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.ui.editor.level.waypoint.findUltimateTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for waypoint minimap connection logic
 */
class WaypointMinimapConnectionTest {
    
    @Test
    fun testFindUltimateTargetDirectConnection() {
        // Setup: waypoint -> target
        val target = Position(10, 10)
        val waypoint = EditorWaypoint(Position(5, 5), target)
        val targets = listOf(target)
        
        val result = findUltimateTarget(waypoint, listOf(waypoint), targets)
        
        assertEquals(target, result)
    }
    
    @Test
    fun testFindUltimateTargetChain() {
        // Setup: waypoint1 -> waypoint2 -> target
        val target = Position(10, 10)
        val waypoint2 = EditorWaypoint(Position(7, 7), target)
        val waypoint1 = EditorWaypoint(Position(5, 5), Position(7, 7))
        val targets = listOf(target)
        
        val result = findUltimateTarget(waypoint1, listOf(waypoint1, waypoint2), targets)
        
        assertEquals(target, result)
    }
    
    @Test
    fun testFindUltimateTargetLongChain() {
        // Setup: waypoint1 -> waypoint2 -> waypoint3 -> target
        val target = Position(10, 10)
        val waypoint3 = EditorWaypoint(Position(9, 9), target)
        val waypoint2 = EditorWaypoint(Position(7, 7), Position(9, 9))
        val waypoint1 = EditorWaypoint(Position(5, 5), Position(7, 7))
        val targets = listOf(target)
        
        val result = findUltimateTarget(
            waypoint1, 
            listOf(waypoint1, waypoint2, waypoint3), 
            targets
        )
        
        assertEquals(target, result)
    }
    
    @Test
    fun testFindUltimateTargetCircularDependency() {
        // Setup: waypoint1 -> waypoint2 -> waypoint1 (circular)
        val waypoint2 = EditorWaypoint(Position(7, 7), Position(5, 5))
        val waypoint1 = EditorWaypoint(Position(5, 5), Position(7, 7))
        val targets = listOf(Position(10, 10))
        
        val result = findUltimateTarget(waypoint1, listOf(waypoint1, waypoint2), targets)
        
        assertNull(result, "Circular dependency should return null")
    }
    
    @Test
    fun testFindUltimateTargetNoTarget() {
        // Setup: waypoint -> position that is not a target
        val waypoint = EditorWaypoint(Position(5, 5), Position(7, 7))
        val targets = listOf(Position(10, 10))
        
        val result = findUltimateTarget(waypoint, listOf(waypoint), targets)
        
        assertNull(result, "Waypoint not leading to a target should return null")
    }
    
    @Test
    fun testFindUltimateTargetMultipleTargets() {
        // Setup: multiple targets, waypoint leads to second one
        val target1 = Position(10, 10)
        val target2 = Position(15, 15)
        val waypoint = EditorWaypoint(Position(5, 5), target2)
        val targets = listOf(target1, target2)
        
        val result = findUltimateTarget(waypoint, listOf(waypoint), targets)
        
        assertEquals(target2, result)
    }
    
    @Test
    fun testFindUltimateTargetBranchingChains() {
        // Setup: waypoint1 and waypoint2 both lead to same target
        val target = Position(10, 10)
        val waypoint1 = EditorWaypoint(Position(5, 5), target)
        val waypoint2 = EditorWaypoint(Position(6, 6), target)
        val targets = listOf(target)
        
        val result1 = findUltimateTarget(waypoint1, listOf(waypoint1, waypoint2), targets)
        val result2 = findUltimateTarget(waypoint2, listOf(waypoint1, waypoint2), targets)
        
        assertEquals(target, result1)
        assertEquals(target, result2)
    }
}
