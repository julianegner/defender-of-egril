package de.egril.defender.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for InfoState single tutorial info management
 */
class InfoStateTest {
    
    @Test
    fun testInitialState() {
        val state = InfoState()
        
        assertEquals(InfoType.NONE, state.currentInfo, "Should have no current info")
        assertTrue(state.seenInfos.isEmpty(), "Should have no seen infos")
        assertFalse(state.shouldShowOverlay(), "Should not show overlay")
        assertEquals(null, state.mineWarningId, "Should have no mine warning ID")
    }
    
    @Test
    fun testShowInfo() {
        var state = InfoState()
        
        // Show dragon info
        state = state.showInfo(InfoType.DRAGON_INFO)
        assertEquals(InfoType.DRAGON_INFO, state.currentInfo, "Should show dragon info")
        assertTrue(state.shouldShowOverlay(), "Should show overlay")
        assertEquals(null, state.mineWarningId, "Should have no mine warning ID")
        
        // Show mine warning with ID
        state = InfoState()
        state = state.showInfo(InfoType.MINE_WARNING, mineId = 42)
        assertEquals(InfoType.MINE_WARNING, state.currentInfo, "Should show mine warning")
        assertTrue(state.shouldShowOverlay(), "Should show overlay")
        assertEquals(42, state.mineWarningId, "Should have mine warning ID")
    }
    
    @Test
    fun testDismissInfo() {
        var state = InfoState()
        
        // Show and dismiss dragon info
        state = state.showInfo(InfoType.DRAGON_INFO)
        state = state.dismissInfo()
        
        assertEquals(InfoType.NONE, state.currentInfo, "Should have no current info after dismiss")
        assertTrue(state.seenInfos.contains(InfoType.DRAGON_INFO), "Should mark dragon info as seen")
        assertFalse(state.shouldShowOverlay(), "Should not show overlay after dismiss")
        assertEquals(null, state.mineWarningId, "Should clear mine warning ID")
    }
    
    @Test
    fun testMultipleDismisses() {
        var state = InfoState()
        
        // Show and dismiss multiple infos
        state = state.showInfo(InfoType.DRAGON_INFO)
        state = state.dismissInfo()
        
        state = state.showInfo(InfoType.GREED_INFO)
        state = state.dismissInfo()
        
        state = state.showInfo(InfoType.VERY_GREEDY_INFO)
        state = state.dismissInfo()
        
        assertEquals(InfoType.NONE, state.currentInfo, "Should have no current info")
        assertTrue(state.seenInfos.contains(InfoType.DRAGON_INFO), "Should have seen dragon info")
        assertTrue(state.seenInfos.contains(InfoType.GREED_INFO), "Should have seen greed info")
        assertTrue(state.seenInfos.contains(InfoType.VERY_GREEDY_INFO), "Should have seen very greedy info")
        assertEquals(3, state.seenInfos.size, "Should have 3 seen infos")
    }
    
    @Test
    fun testHasSeen() {
        var state = InfoState()
        
        assertFalse(state.hasSeen(InfoType.DRAGON_INFO), "Should not have seen dragon info initially")
        
        state = state.showInfo(InfoType.DRAGON_INFO)
        assertFalse(state.hasSeen(InfoType.DRAGON_INFO), "Should not have seen until dismissed")
        
        state = state.dismissInfo()
        assertTrue(state.hasSeen(InfoType.DRAGON_INFO), "Should have seen after dismiss")
        assertFalse(state.hasSeen(InfoType.GREED_INFO), "Should not have seen other infos")
    }
    
    @Test
    fun testClearSeenInfos() {
        var state = InfoState()
        
        // Show and dismiss multiple infos
        state = state.showInfo(InfoType.DRAGON_INFO)
        state = state.dismissInfo()
        
        state = state.showInfo(InfoType.GREED_INFO)
        state = state.dismissInfo()
        
        assertEquals(2, state.seenInfos.size, "Should have 2 seen infos")
        
        // Clear seen infos
        state = state.clearSeenInfos()
        
        assertTrue(state.seenInfos.isEmpty(), "Should have no seen infos after clear")
        assertFalse(state.hasSeen(InfoType.DRAGON_INFO), "Should not have seen dragon info after clear")
        assertFalse(state.hasSeen(InfoType.GREED_INFO), "Should not have seen greed info after clear")
    }
    
    @Test
    fun testMineWarningIdHandling() {
        var state = InfoState()
        
        // Show mine warning with ID
        state = state.showInfo(InfoType.MINE_WARNING, mineId = 123)
        assertEquals(123, state.mineWarningId, "Should set mine warning ID")
        
        // Dismiss clears the ID
        state = state.dismissInfo()
        assertEquals(null, state.mineWarningId, "Should clear mine warning ID on dismiss")
        
        // Show different info doesn't set mine warning ID
        state = state.showInfo(InfoType.DRAGON_INFO)
        assertEquals(null, state.mineWarningId, "Should not set mine warning ID for non-mine info")
    }
    
    @Test
    fun testRepeatedInfoShow() {
        var state = InfoState()
        
        // Show dragon info
        state = state.showInfo(InfoType.DRAGON_INFO)
        state = state.dismissInfo()
        
        assertTrue(state.hasSeen(InfoType.DRAGON_INFO), "Should have seen dragon info")
        
        // Show again (shouldn't automatically prevent it - that's controlled by caller)
        state = state.showInfo(InfoType.DRAGON_INFO)
        assertEquals(InfoType.DRAGON_INFO, state.currentInfo, "Should show dragon info again")
        assertTrue(state.hasSeen(InfoType.DRAGON_INFO), "Should still have it in seen list")
    }
    
    @Test
    fun testDismissWithoutShowing() {
        var state = InfoState()
        
        // Dismiss without showing any info
        state = state.dismissInfo()
        
        assertEquals(InfoType.NONE, state.currentInfo, "Should have no current info")
        assertTrue(state.seenInfos.isEmpty(), "Should have no seen infos")
        assertFalse(state.shouldShowOverlay(), "Should not show overlay")
    }
}
