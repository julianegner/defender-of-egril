package de.egril.defender.model

/**
 * Actions that can be performed by a dwarven mine
 */
enum class MineAction {
    DIG,         // Try to find valuable materials (with risk of dragon)
    BUILD_TRAP   // Place a trap on the path
}

/**
 * Actions that can be performed by a wizard tower
 */
enum class WizardAction {
    PLACE_MAGICAL_TRAP,  // Place a magical trap that teleports enemies (level 10+)
    GENERATE_MANA        // Generate mana for spell casting (always available)
}

/**
 * Actions that can be performed by Spike Tower or Spear Tower (level 10+)
 */
enum class BarricadeAction {
    BUILD_BARRICADE      // Build a new barricade or reinforce an existing one
}

/**
 * Possible outcomes when digging in a mine
 */
enum class DigOutcome(val probability: Int, val coins: Int, val displayName: String) {
    NOTHING(50, 0, "Nothing"),
    BRASS(20, 10, "Brass"),
    SILVER(10, 25, "Silver"),
    GOLD(8, 50, "Gold"),
    GEMS(6, 200, "Gems"),
    DIAMOND(4, 1000, "Diamond"),
    DRAGON(2, 0, "Dragon awakens!");
    
    companion object {
        fun roll(): DigOutcome {
            val roll = (1..100).random()
            var cumulative = 0
            for (outcome in values()) {
                cumulative += outcome.probability
                if (roll <= cumulative) {
                    return outcome
                }
            }
            return NOTHING // Fallback
        }
    }
}
