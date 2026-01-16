package de.egril.defender.model

/**
 * Represents the current step in the tutorial
 */
enum class TutorialStep {
    WELCOME,                // Introduction to the game
    RESOURCES,              // Explain coins and health
    TOWER_TYPES,            // Explain available towers first
    LEGEND_INFO,            // Explain the legend and what it shows
    ENEMY_LIST_INFO,        // Explain the enemy list and upcoming enemies
    BUILD_TOWER,            // Guide to place first tower
    INITIAL_BUILDING,       // Explain initial building phase
    UNDO_TOWER,             // Explain undo in initial phase
    ENEMIES_INCOMING,       // Show incoming enemies
    START_COMBAT,           // Explain combat phase and start battle button
    CHECK_RANGE,            // Show how to check if enemy is in range (before attacking)
    ATTACKING,              // Guide player on how to attack
    UPGRADE_TOWER,          // Explain upgrading towers
    SELL_TOWER,             // Explain selling towers
    SAVE_GAME,              // Explain saving and loading games
    COMPLETE,               // Tutorial finished
    NONE                    // Not in tutorial or tutorial skipped
}

/**
 * Tutorial state data
 */
data class TutorialState(
    val isActive: Boolean = false,
    val currentStep: TutorialStep = TutorialStep.NONE,
    val hasPlacedFirstTower: Boolean = false,
    val hasStartedFirstTurn: Boolean = false,
    val hasAttackedEnemy: Boolean = false,
    val canSkipAttacking: Boolean = false  // Allow skip if tower can't reach or action used
) {
    /**
     * Check if we should show the tutorial overlay for the current step
     */
    fun shouldShowOverlay(): Boolean {
        return isActive && currentStep != TutorialStep.NONE
    }
    
    /**
     * Check if the Next button should be enabled for the current step
     * @param defendersCount Number of defenders currently on the map (optional)
     */
    fun isNextEnabled(defendersCount: Int = 0): Boolean {
        return when (currentStep) {
            TutorialStep.BUILD_TOWER -> hasPlacedFirstTower || defendersCount > 0
            TutorialStep.START_COMBAT -> hasStartedFirstTurn
            TutorialStep.ATTACKING -> hasAttackedEnemy || canSkipAttacking
            else -> true  // All other steps can advance immediately
        }
    }
    
    /**
     * Get the next tutorial step based on current state
     */
    fun getNextStep(): TutorialStep {
        return when (currentStep) {
            TutorialStep.WELCOME -> TutorialStep.RESOURCES
            TutorialStep.RESOURCES -> TutorialStep.TOWER_TYPES
            TutorialStep.TOWER_TYPES -> TutorialStep.LEGEND_INFO
            TutorialStep.LEGEND_INFO -> TutorialStep.ENEMY_LIST_INFO
            TutorialStep.ENEMY_LIST_INFO -> TutorialStep.BUILD_TOWER
            TutorialStep.BUILD_TOWER -> {
                if (hasPlacedFirstTower) TutorialStep.INITIAL_BUILDING
                else TutorialStep.BUILD_TOWER
            }
            TutorialStep.INITIAL_BUILDING -> TutorialStep.UNDO_TOWER
            TutorialStep.UNDO_TOWER -> TutorialStep.START_COMBAT
            TutorialStep.START_COMBAT -> {
                if (hasStartedFirstTurn) TutorialStep.ENEMIES_INCOMING
                else TutorialStep.START_COMBAT
            }
            TutorialStep.ENEMIES_INCOMING -> TutorialStep.CHECK_RANGE
            TutorialStep.CHECK_RANGE -> TutorialStep.ATTACKING
            TutorialStep.ATTACKING -> {
                if (hasAttackedEnemy || canSkipAttacking) TutorialStep.UPGRADE_TOWER
                else TutorialStep.ATTACKING
            }
            TutorialStep.UPGRADE_TOWER -> TutorialStep.SELL_TOWER
            TutorialStep.SELL_TOWER -> TutorialStep.SAVE_GAME
            TutorialStep.SAVE_GAME -> TutorialStep.COMPLETE
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
            isActive = nextStep != TutorialStep.NONE
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
     * Allow skipping the attacking step (tower can't reach or action used)
     */
    fun allowSkipAttacking(): TutorialState {
        return copy(canSkipAttacking = true)
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
