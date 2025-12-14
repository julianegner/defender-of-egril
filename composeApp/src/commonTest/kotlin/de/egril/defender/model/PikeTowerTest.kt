package de.egril.defender.model

import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for Pike (Spike) Tower finetuning:
 * - Maximum range of 2 at level 5+
 * - More actions with every 5 levels (max 3 actions)
 */
class PikeTowerTest {
    
    @Test
    fun testPikeTowerRangeProgression() {
        val position = Position(0, 0)
        
        // Level 1: baseRange(1) + (1-1)/2 = 1
        val tower1 = Defender(1, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(1))
        assertEquals(1, tower1.range, "Level 1 pike tower should have range 1")
        
        // Level 2: baseRange(1) + (2-1)/2 = 1
        val tower2 = Defender(2, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(2))
        assertEquals(1, tower2.range, "Level 2 pike tower should have range 1")
        
        // Level 3: baseRange(1) + (3-1)/2 = 2
        val tower3 = Defender(3, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(3))
        assertEquals(2, tower3.range, "Level 3 pike tower should have range 2")
        
        // Level 4: baseRange(1) + (4-1)/2 = 2
        val tower4 = Defender(4, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(4))
        assertEquals(2, tower4.range, "Level 4 pike tower should have range 2")
        
        // Level 5: baseRange(1) + (5-1)/2 = 3, but capped at 2
        val tower5 = Defender(5, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(5))
        assertEquals(2, tower5.range, "Level 5 pike tower should have range 2 (capped)")
        
        // Level 10: baseRange(1) + (10-1)/2 = 5, but capped at 2
        val tower10 = Defender(10, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(10))
        assertEquals(2, tower10.range, "Level 10 pike tower should have range 2 (capped)")
        
        // Level 20: baseRange(1) + (20-1)/2 = 10, but capped at 2
        val tower20 = Defender(20, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(20))
        assertEquals(2, tower20.range, "Level 20 pike tower should have range 2 (capped)")
    }
    
    @Test
    fun testPikeTowerActionsPerTurn() {
        val position = Position(0, 0)
        
        // Level 1-4: 1 action (base)
        val tower1 = Defender(1, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(1))
        assertEquals(1, tower1.actionsPerTurnCalculated, "Level 1 pike tower should have 1 action")
        
        val tower4 = Defender(4, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(4))
        assertEquals(1, tower4.actionsPerTurnCalculated, "Level 4 pike tower should have 1 action")
        
        // Level 5-9: 2 actions (base 1 + bonus 1)
        val tower5 = Defender(5, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(5))
        assertEquals(2, tower5.actionsPerTurnCalculated, "Level 5 pike tower should have 2 actions")
        
        val tower9 = Defender(9, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(9))
        assertEquals(2, tower9.actionsPerTurnCalculated, "Level 9 pike tower should have 2 actions")
        
        // Level 10-14: 3 actions (base 1 + bonus 2), capped at 3
        val tower10 = Defender(10, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(10))
        assertEquals(3, tower10.actionsPerTurnCalculated, "Level 10 pike tower should have 3 actions (capped)")
        
        val tower14 = Defender(14, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(14))
        assertEquals(3, tower14.actionsPerTurnCalculated, "Level 14 pike tower should have 3 actions (capped)")
        
        // Level 15+: still 3 actions (capped)
        val tower15 = Defender(15, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(15))
        assertEquals(3, tower15.actionsPerTurnCalculated, "Level 15 pike tower should have 3 actions (capped)")
        
        val tower20 = Defender(20, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(20))
        assertEquals(3, tower20.actionsPerTurnCalculated, "Level 20 pike tower should have 3 actions (capped)")
    }
    
    @Test
    fun testOtherTowersUnaffected() {
        val position = Position(0, 0)
        
        // Verify that other tower types are not affected by pike tower changes
        
        // Spear tower - should follow normal range calculation
        val spear5 = Defender(1, DefenderType.SPEAR_TOWER, mutableStateOf(position), mutableStateOf(5))
        assertEquals(4, spear5.range, "Level 5 spear tower should have range 4 (2 + (5-1)/2)")
        assertEquals(1, spear5.actionsPerTurnCalculated, "Spear tower should have 1 action")
        
        // Bow tower
        val bow10 = Defender(2, DefenderType.BOW_TOWER, mutableStateOf(position), mutableStateOf(10))
        assertEquals(7, bow10.range, "Level 10 bow tower should have range 7 (3 + (10-1)/2)")
        assertEquals(1, bow10.actionsPerTurnCalculated, "Bow tower should have 1 action")
        
        // Wizard tower
        val wizard5 = Defender(3, DefenderType.WIZARD_TOWER, mutableStateOf(position), mutableStateOf(5))
        assertEquals(5, wizard5.range, "Level 5 wizard tower should have range 5 (3 + (5-1)/2)")
        assertEquals(1, wizard5.actionsPerTurnCalculated, "Wizard tower should have 1 action")
    }
    
    @Test
    fun testPikeTowerResetActions() {
        val position = Position(0, 0)
        
        // Test that resetActions() correctly uses the calculated actions per turn
        val tower5 = Defender(5, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(5))
        tower5.buildTimeRemaining.value = 0  // Mark as ready
        tower5.resetActions()
        assertEquals(2, tower5.actionsRemaining.value, "Level 5 pike tower should reset to 2 actions")
        
        val tower10 = Defender(10, DefenderType.SPIKE_TOWER, mutableStateOf(position), mutableStateOf(10))
        tower10.buildTimeRemaining.value = 0  // Mark as ready
        tower10.resetActions()
        assertEquals(3, tower10.actionsRemaining.value, "Level 10 pike tower should reset to 3 actions")
    }
}
