package com.defenderofegril.model

/**
 * Represents the current step in the tutorial
 */
enum class TutorialStep {
    WELCOME,           // Introduction to the game
    RESOURCES,         // Explain coins and health
    BUILD_TOWER,       // Guide to place first tower
    TOWER_TYPES,       // Explain available towers
    ENEMIES_INCOMING,  // Show incoming enemies
    START_COMBAT,      // Explain combat phase
    COMPLETE,          // Tutorial finished
    NONE               // Not in tutorial or tutorial skipped
}

/**
 * Tutorial state data
 */
data class TutorialState(
    val isActive: Boolean = false,
    val currentStep: TutorialStep = TutorialStep.NONE,
    val hasPlacedFirstTower: Boolean = false,
    val hasStartedFirstTurn: Boolean = false
) {
    /**
     * Check if we should show the tutorial overlay for the current step
     */
    fun shouldShowOverlay(): Boolean {
        return isActive && currentStep != TutorialStep.NONE && currentStep != TutorialStep.COMPLETE
    }
    
    /**
     * Get the next tutorial step based on current state
     */
    fun getNextStep(): TutorialStep {
        return when (currentStep) {
            TutorialStep.WELCOME -> TutorialStep.RESOURCES
            TutorialStep.RESOURCES -> TutorialStep.BUILD_TOWER
            TutorialStep.BUILD_TOWER -> {
                if (hasPlacedFirstTower) TutorialStep.TOWER_TYPES
                else TutorialStep.BUILD_TOWER
            }
            TutorialStep.TOWER_TYPES -> TutorialStep.ENEMIES_INCOMING
            TutorialStep.ENEMIES_INCOMING -> TutorialStep.START_COMBAT
            TutorialStep.START_COMBAT -> {
                if (hasStartedFirstTurn) TutorialStep.COMPLETE
                else TutorialStep.START_COMBAT
            }
            TutorialStep.COMPLETE -> TutorialStep.NONE
            TutorialStep.NONE -> TutorialStep.NONE
        }
    }
    
    /**
     * Advance to the next step
     */
    fun advanceStep(): TutorialState {
        val nextStep = getNextStep()
        return copy(
            currentStep = nextStep,
            isActive = nextStep != TutorialStep.NONE && nextStep != TutorialStep.COMPLETE
        )
    }
    
    /**
     * Mark that the player has placed their first tower
     */
    fun markTowerPlaced(): TutorialState {
        return copy(hasPlacedFirstTower = true)
    }
    
    /**
     * Mark that the player has started their first turn
     */
    fun markTurnStarted(): TutorialState {
        return copy(hasStartedFirstTurn = true)
    }
    
    /**
     * Skip the tutorial
     */
    fun skip(): TutorialState {
        return copy(
            isActive = false,
            currentStep = TutorialStep.NONE
        )
    }
}
