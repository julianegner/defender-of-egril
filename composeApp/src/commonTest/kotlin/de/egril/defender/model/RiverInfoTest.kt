package de.egril.defender.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for River Info popup functionality
 * Verifies that InfoType.RIVER_INFO is properly handled when levels with rivers are started
 */
class RiverInfoTest {
    
    @Test
    fun testRiverInfoInitialState() {
        val state = InfoState()
        
        assertFalse(state.hasSeen(InfoType.RIVER_INFO), "Should not have seen river info initially")
        assertEquals(InfoType.NONE, state.currentInfo, "Should have no current info")
    }
    
    @Test
    fun testShowRiverInfo() {
        var state = InfoState()
        
        // Simulate showing river info when a level with river tiles is started
        state = state.showInfo(InfoType.RIVER_INFO)
        
        assertEquals(InfoType.RIVER_INFO, state.currentInfo, "Should show river info")
        assertTrue(state.shouldShowOverlay(), "Should show overlay")
        assertFalse(state.hasSeen(InfoType.RIVER_INFO), "Should not have seen until dismissed")
    }
    
    @Test
    fun testDismissRiverInfo() {
        var state = InfoState()
        
        // Show river info
        state = state.showInfo(InfoType.RIVER_INFO)
        assertEquals(InfoType.RIVER_INFO, state.currentInfo, "Should show river info")
        
        // Dismiss river info
        state = state.dismissInfo()
        
        assertEquals(InfoType.NONE, state.currentInfo, "Should have no current info after dismiss")
        assertTrue(state.hasSeen(InfoType.RIVER_INFO), "Should mark river info as seen")
        assertFalse(state.shouldShowOverlay(), "Should not show overlay after dismiss")
    }
    
    @Test
    fun testRiverInfoShownOnlyOnce() {
        var state = InfoState()
        
        // Show and dismiss river info
        state = state.showInfo(InfoType.RIVER_INFO)
        state = state.dismissInfo()
        
        assertTrue(state.hasSeen(InfoType.RIVER_INFO), "Should have seen river info")
        
        // The caller should check hasSeen() before showing again
        // Here we verify the state tracks it correctly
        val shouldShowAgain = !state.hasSeen(InfoType.RIVER_INFO)
        assertFalse(shouldShowAgain, "Should not show river info again")
    }
    
    @Test
    fun testLevelWithRiverTiles() {
        // Create a test level with river tiles
        val riverTiles = mapOf(
            Position(5, 3) to RiverTile(
                position = Position(5, 3),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            ),
            Position(6, 3) to RiverTile(
                position = Position(6, 3),
                flowDirection = RiverFlow.EAST,
                flowSpeed = 1
            )
        )
        
        val level = Level(
            id = 1,
            name = "Test River Level",
            pathCells = setOf(Position(0, 0), Position(1, 0)),
            attackerWaves = emptyList(),
            riverTiles = riverTiles
        )
        
        // Verify level has river tiles
        assertTrue(level.riverTiles.isNotEmpty(), "Level should have river tiles")
        assertEquals(2, level.riverTiles.size, "Level should have 2 river tiles")
        assertTrue(level.isRiverTile(Position(5, 3)), "Position (5,3) should be a river tile")
        assertTrue(level.isRiverTile(Position(6, 3)), "Position (6,3) should be a river tile")
    }
    
    @Test
    fun testLevelWithoutRiverTiles() {
        // Create a test level without river tiles
        val level = Level(
            id = 1,
            name = "Test Normal Level",
            pathCells = setOf(Position(0, 0), Position(1, 0)),
            attackerWaves = emptyList(),
            riverTiles = emptyMap()
        )
        
        // Verify level has no river tiles
        assertTrue(level.riverTiles.isEmpty(), "Level should have no river tiles")
        assertFalse(level.isRiverTile(Position(5, 3)), "Position should not be a river tile")
    }
    
    @Test
    fun testRiverInfoNotShownWhenOtherInfoActive() {
        var state = InfoState()
        
        // Show dragon info first
        state = state.showInfo(InfoType.DRAGON_INFO)
        assertEquals(InfoType.DRAGON_INFO, state.currentInfo, "Should show dragon info")
        
        // The caller should check if currentInfo != NONE before showing river info
        val shouldShowRiverInfo = state.currentInfo == InfoType.NONE && !state.hasSeen(InfoType.RIVER_INFO)
        assertFalse(shouldShowRiverInfo, "Should not show river info when another info is active")
    }
    
    @Test
    fun testRiverInfoCanBeShownAfterOtherInfoDismissed() {
        var state = InfoState()
        
        // Show and dismiss dragon info
        state = state.showInfo(InfoType.DRAGON_INFO)
        state = state.dismissInfo()
        
        // Now river info can be shown
        val shouldShowRiverInfo = state.currentInfo == InfoType.NONE && !state.hasSeen(InfoType.RIVER_INFO)
        assertTrue(shouldShowRiverInfo, "Should be able to show river info after other info is dismissed")
        
        // Show river info
        state = state.showInfo(InfoType.RIVER_INFO)
        assertEquals(InfoType.RIVER_INFO, state.currentInfo, "Should show river info")
    }
    
    @Test
    fun testMultipleInfosIncludingRiver() {
        var state = InfoState()
        
        // Show and dismiss multiple infos including river info
        state = state.showInfo(InfoType.DRAGON_INFO)
        state = state.dismissInfo()
        
        state = state.showInfo(InfoType.RIVER_INFO)
        state = state.dismissInfo()
        
        state = state.showInfo(InfoType.GREEN_WITCH_INFO)
        state = state.dismissInfo()
        
        assertEquals(InfoType.NONE, state.currentInfo, "Should have no current info")
        assertTrue(state.hasSeen(InfoType.DRAGON_INFO), "Should have seen dragon info")
        assertTrue(state.hasSeen(InfoType.RIVER_INFO), "Should have seen river info")
        assertTrue(state.hasSeen(InfoType.GREEN_WITCH_INFO), "Should have seen green witch info")
        assertEquals(3, state.seenInfos.size, "Should have 3 seen infos")
    }
}
