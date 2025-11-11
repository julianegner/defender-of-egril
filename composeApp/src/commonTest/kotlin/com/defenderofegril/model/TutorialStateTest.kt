package com.defenderofegril.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for TutorialState progression and state management
 */
class TutorialStateTest {
    
    @Test
    fun testInitialState() {
        val state = TutorialState(isActive = true, currentStep = TutorialStep.WELCOME)
        
        assertTrue(state.isActive, "Tutorial should be active")
        assertEquals(TutorialStep.WELCOME, state.currentStep, "Should start at WELCOME step")
        assertFalse(state.hasPlacedFirstTower, "Should not have placed tower yet")
        assertFalse(state.hasStartedFirstTurn, "Should not have started turn yet")
        assertTrue(state.shouldShowOverlay(), "Should show overlay for WELCOME step")
    }
    
    @Test
    fun testStepProgression() {
        var state = TutorialState(isActive = true, currentStep = TutorialStep.WELCOME)
        
        // WELCOME -> RESOURCES
        state = state.advanceStep()
        assertEquals(TutorialStep.RESOURCES, state.currentStep, "Should advance to RESOURCES")
        assertTrue(state.shouldShowOverlay(), "Should show overlay for RESOURCES step")
        
        // RESOURCES -> BUILD_TOWER
        state = state.advanceStep()
        assertEquals(TutorialStep.BUILD_TOWER, state.currentStep, "Should advance to BUILD_TOWER")
        
        // BUILD_TOWER -> BUILD_TOWER (waiting for tower placement)
        state = state.advanceStep()
        assertEquals(TutorialStep.BUILD_TOWER, state.currentStep, "Should stay at BUILD_TOWER until tower is placed")
        
        // Mark tower placed
        state = state.markTowerPlaced()
        assertTrue(state.hasPlacedFirstTower, "Should mark tower as placed")
        
        // BUILD_TOWER -> TOWER_TYPES (now that tower is placed)
        state = state.advanceStep()
        assertEquals(TutorialStep.TOWER_TYPES, state.currentStep, "Should advance to TOWER_TYPES after tower placed")
        
        // TOWER_TYPES -> ENEMIES_INCOMING
        state = state.advanceStep()
        assertEquals(TutorialStep.ENEMIES_INCOMING, state.currentStep, "Should advance to ENEMIES_INCOMING")
        
        // ENEMIES_INCOMING -> START_COMBAT
        state = state.advanceStep()
        assertEquals(TutorialStep.START_COMBAT, state.currentStep, "Should advance to START_COMBAT")
        
        // START_COMBAT -> START_COMBAT (waiting for turn start)
        state = state.advanceStep()
        assertEquals(TutorialStep.START_COMBAT, state.currentStep, "Should stay at START_COMBAT until turn is started")
        
        // Mark turn started
        state = state.markTurnStarted()
        assertTrue(state.hasStartedFirstTurn, "Should mark turn as started")
        
        // START_COMBAT -> COMPLETE (now that turn is started)
        state = state.advanceStep()
        assertEquals(TutorialStep.COMPLETE, state.currentStep, "Should advance to COMPLETE after turn started")
        
        // COMPLETE -> NONE
        state = state.advanceStep()
        assertEquals(TutorialStep.NONE, state.currentStep, "Should advance to NONE after COMPLETE")
        assertFalse(state.isActive, "Tutorial should be inactive after COMPLETE")
        assertFalse(state.shouldShowOverlay(), "Should not show overlay after tutorial ends")
    }
    
    @Test
    fun testSkipTutorial() {
        val state = TutorialState(isActive = true, currentStep = TutorialStep.RESOURCES)
        val skippedState = state.skip()
        
        assertFalse(skippedState.isActive, "Tutorial should be inactive after skip")
        assertEquals(TutorialStep.NONE, skippedState.currentStep, "Step should be NONE after skip")
        assertFalse(skippedState.shouldShowOverlay(), "Should not show overlay after skip")
    }
    
    @Test
    fun testInactiveTutorial() {
        val state = TutorialState(isActive = false, currentStep = TutorialStep.NONE)
        
        assertFalse(state.isActive, "Tutorial should be inactive")
        assertEquals(TutorialStep.NONE, state.currentStep, "Step should be NONE")
        assertFalse(state.shouldShowOverlay(), "Should not show overlay when inactive")
    }
    
    @Test
    fun testGetNextStep() {
        val state = TutorialState(isActive = true, currentStep = TutorialStep.WELCOME)
        
        assertEquals(TutorialStep.RESOURCES, state.getNextStep(), "Next step from WELCOME should be RESOURCES")
    }
    
    @Test
    fun testBuildTowerGating() {
        var state = TutorialState(isActive = true, currentStep = TutorialStep.BUILD_TOWER)
        
        // Should not advance past BUILD_TOWER without tower placement
        assertEquals(TutorialStep.BUILD_TOWER, state.getNextStep(), "Should stay at BUILD_TOWER")
        
        // Mark tower placed
        state = state.markTowerPlaced()
        
        // Should now advance to next step
        assertEquals(TutorialStep.TOWER_TYPES, state.getNextStep(), "Should advance to TOWER_TYPES after tower placed")
    }
    
    @Test
    fun testStartCombatGating() {
        var state = TutorialState(isActive = true, currentStep = TutorialStep.START_COMBAT)
        
        // Should not advance past START_COMBAT without turn start
        assertEquals(TutorialStep.START_COMBAT, state.getNextStep(), "Should stay at START_COMBAT")
        
        // Mark turn started
        state = state.markTurnStarted()
        
        // Should now advance to next step
        assertEquals(TutorialStep.COMPLETE, state.getNextStep(), "Should advance to COMPLETE after turn started")
    }
}
