package de.egril.defender.model

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
        assertFalse(state.hasAttackedEnemy, "Should not have attacked enemy yet")
        assertFalse(state.canSkipAttacking, "Should not be able to skip attacking yet")
        assertTrue(state.shouldShowOverlay(), "Should show overlay for WELCOME step")
        assertTrue(state.isNextEnabled(), "Next should be enabled for WELCOME step")
    }
    
    @Test
    fun testNextButtonDisabling() {
        // Build tower step should have Next disabled until tower placed
        var state = TutorialState(isActive = true, currentStep = TutorialStep.BUILD_TOWER)
        assertFalse(state.isNextEnabled(), "Next should be disabled at BUILD_TOWER without tower")
        
        state = state.markTowerPlaced()
        assertTrue(state.isNextEnabled(), "Next should be enabled at BUILD_TOWER after tower placed")
        
        // Start combat step should have Next disabled until turn started
        state = TutorialState(isActive = true, currentStep = TutorialStep.START_COMBAT)
        assertFalse(state.isNextEnabled(), "Next should be disabled at START_COMBAT without turn start")
        
        state = state.markTurnStarted()
        assertTrue(state.isNextEnabled(), "Next should be enabled at START_COMBAT after turn started")
        
        // Attacking step should have Next disabled until attack or skip allowed
        state = TutorialState(isActive = true, currentStep = TutorialStep.ATTACKING)
        assertFalse(state.isNextEnabled(), "Next should be disabled at ATTACKING without attack")
        
        state = state.markAttackedEnemy()
        assertTrue(state.isNextEnabled(), "Next should be enabled at ATTACKING after attack")
        
        // Or if skip is allowed
        state = TutorialState(isActive = true, currentStep = TutorialStep.ATTACKING, canSkipAttacking = true)
        assertTrue(state.isNextEnabled(), "Next should be enabled at ATTACKING if skip allowed")
    }
    
    @Test
    fun testStepProgression() {
        var state = TutorialState(isActive = true, currentStep = TutorialStep.WELCOME)
        
        // WELCOME -> RESOURCES
        state = state.advanceStep()
        assertEquals(TutorialStep.RESOURCES, state.currentStep, "Should advance to RESOURCES")
        
        // RESOURCES -> TOWER_TYPES
        state = state.advanceStep()
        assertEquals(TutorialStep.TOWER_TYPES, state.currentStep, "Should advance to TOWER_TYPES")
        
        // TOWER_TYPES -> LEGEND_INFO
        state = state.advanceStep()
        assertEquals(TutorialStep.LEGEND_INFO, state.currentStep, "Should advance to LEGEND_INFO")
        
        // LEGEND_INFO -> ENEMY_LIST_INFO
        state = state.advanceStep()
        assertEquals(TutorialStep.ENEMY_LIST_INFO, state.currentStep, "Should advance to ENEMY_LIST_INFO")
        
        // ENEMY_LIST_INFO -> BUILD_TOWER
        state = state.advanceStep()
        assertEquals(TutorialStep.BUILD_TOWER, state.currentStep, "Should advance to BUILD_TOWER")
        
        // BUILD_TOWER -> BUILD_TOWER (waiting for tower placement)
        state = state.advanceStep()
        assertEquals(TutorialStep.BUILD_TOWER, state.currentStep, "Should stay at BUILD_TOWER until tower is placed")
        
        // Mark tower placed
        state = state.markTowerPlaced()
        
        // BUILD_TOWER -> INITIAL_BUILDING
        state = state.advanceStep()
        assertEquals(TutorialStep.INITIAL_BUILDING, state.currentStep, "Should advance to INITIAL_BUILDING")
        
        // INITIAL_BUILDING -> UNDO_TOWER
        state = state.advanceStep()
        assertEquals(TutorialStep.UNDO_TOWER, state.currentStep, "Should advance to UNDO_TOWER")
        
        // UNDO_TOWER -> START_COMBAT
        state = state.advanceStep()
        assertEquals(TutorialStep.START_COMBAT, state.currentStep, "Should advance to START_COMBAT")
        
        // START_COMBAT -> START_COMBAT (waiting for turn start)
        state = state.advanceStep()
        assertEquals(TutorialStep.START_COMBAT, state.currentStep, "Should stay at START_COMBAT until turn is started")
        
        // Mark turn started
        state = state.markTurnStarted()
        
        // START_COMBAT -> ENEMIES_INCOMING (after turn started)
        state = state.advanceStep()
        assertEquals(TutorialStep.ENEMIES_INCOMING, state.currentStep, "Should advance to ENEMIES_INCOMING after turn started")
        
        // ENEMIES_INCOMING -> CHECK_RANGE
        state = state.advanceStep()
        assertEquals(TutorialStep.CHECK_RANGE, state.currentStep, "Should advance to CHECK_RANGE")
        
        // CHECK_RANGE -> ATTACKING
        state = state.advanceStep()
        assertEquals(TutorialStep.ATTACKING, state.currentStep, "Should advance to ATTACKING")
        
        // ATTACKING -> ATTACKING (waiting for attack or skip)
        state = state.advanceStep()
        assertEquals(TutorialStep.ATTACKING, state.currentStep, "Should stay at ATTACKING until enemy attacked or skip allowed")
        
        // Mark enemy attacked
        state = state.markAttackedEnemy()
        
        // ATTACKING -> UPGRADE_TOWER
        state = state.advanceStep()
        assertEquals(TutorialStep.UPGRADE_TOWER, state.currentStep, "Should advance to UPGRADE_TOWER")
        
        // UPGRADE_TOWER -> SELL_TOWER
        state = state.advanceStep()
        assertEquals(TutorialStep.SELL_TOWER, state.currentStep, "Should advance to SELL_TOWER")
        
        // SELL_TOWER -> SAVE_GAME
        state = state.advanceStep()
        assertEquals(TutorialStep.SAVE_GAME, state.currentStep, "Should advance to SAVE_GAME")
        
        // SAVE_GAME -> COMPLETE
        state = state.advanceStep()
        assertEquals(TutorialStep.COMPLETE, state.currentStep, "Should advance to COMPLETE")
        
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
    fun testAttackingWithSkip() {
        var state = TutorialState(isActive = true, currentStep = TutorialStep.ATTACKING)
        
        // Should not advance without attack or skip permission
        assertEquals(TutorialStep.ATTACKING, state.getNextStep(), "Should stay at ATTACKING")
        
        // Allow skip (tower can't reach or no actions)
        state = state.allowSkipAttacking()
        assertTrue(state.canSkipAttacking, "Should mark skip as allowed")
        
        // Should now advance to next step
        assertEquals(TutorialStep.UPGRADE_TOWER, state.getNextStep(), "Should advance to UPGRADE_TOWER when skip allowed")
    }
}
