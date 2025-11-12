package com.defenderofegril.model

/**
 * Represents the current step in the tutorial
 */
enum class TutorialStep {
    WELCOME,           // Introduction to the game
    RESOURCES,         // Explain coins and health
    TOWER_TYPES,       // Explain available towers first
    BUILD_TOWER,       // Guide to place first tower
    ENEMIES_INCOMING,  // Show incoming enemies
    START_COMBAT,      // Explain combat phase and start battle button
    ATTACKING,         // Guide player on how to attack
    CHECK_RANGE,       // Show how to check if enemy is in range
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
    val hasStartedFirstTurn: Boolean = false,
    val hasAttackedEnemy: Boolean = false
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
            TutorialStep.RESOURCES -> TutorialStep.TOWER_TYPES
            TutorialStep.TOWER_TYPES -> TutorialStep.BUILD_TOWER
            TutorialStep.BUILD_TOWER -> {
                if (hasPlacedFirstTower) TutorialStep.ENEMIES_INCOMING
                else TutorialStep.BUILD_TOWER
            }
            TutorialStep.ENEMIES_INCOMING -> TutorialStep.START_COMBAT
            TutorialStep.START_COMBAT -> {
                if (hasStartedFirstTurn) TutorialStep.ATTACKING
                else TutorialStep.START_COMBAT
            }
            TutorialStep.ATTACKING -> {
                if (hasAttackedEnemy) TutorialStep.CHECK_RANGE
                else TutorialStep.ATTACKING
            }
            TutorialStep.CHECK_RANGE -> TutorialStep.COMPLETE
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
     * Mark that the player has attacked an enemy
     */
    fun markAttackedEnemy(): TutorialState {
        return copy(hasAttackedEnemy = true)
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
