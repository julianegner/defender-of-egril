package de.egril.defender.config

/**
 * Central logging configuration for debugging different parts of the application.
 * Set these flags to true to enable debug logging for specific subsystems.
 * 
 * Usage: Check LogConfig.ENABLE_XP_LOGGING before printing debug messages:
 * ```
 * if (LogConfig.ENABLE_XP_LOGGING) {
 *     System.err.println("=== XP DEBUG: ...")
 * }
 * ```
 */
object LogConfig {
    /**
     * Enable XP (experience points) persistence and loading debug logs.
     * Shows JSON parsing, serialization/deserialization of XP and stats.
     */
    var ENABLE_XP_LOGGING = false
    
    /**
     * Enable level loading debug logs.
     * Shows map and level file loading, parsing, and initialization.
     */
    var ENABLE_LEVEL_LOADING_LOGGING = false
    
    /**
     * Enable game state debug logs.
     * Shows game state changes, turn processing, enemy movement, combat.
     */
    var ENABLE_GAME_STATE_LOGGING = false
    
    /**
     * Enable save/load system debug logs.
     * Shows save file operations, JSON serialization/deserialization.
     */
    var ENABLE_SAVE_LOAD_LOGGING = false
    
    /**
     * Enable UI interaction debug logs.
     * Shows button clicks, screen navigation, user input processing.
     */
    var ENABLE_UI_LOGGING = false
    
    /**
     * Enable spell system debug logs.
     * Shows spell casting, mana usage, spell effects, targeting.
     */
    var ENABLE_SPELL_LOGGING = true
    
    /**
     * Enable enemy AI debug logs.
     * Shows pathfinding, ability usage, summoning, healing.
     */
    var ENABLE_ENEMY_AI_LOGGING = false
    
    /**
     * Enable tower debug logs.
     * Shows tower placement, upgrades, attacks, special abilities.
     */
    var ENABLE_TOWER_LOGGING = false
    
    /**
     * Enable performance debug logs.
     * Shows timing information, frame rates, render performance.
     */
    var ENABLE_PERFORMANCE_LOGGING = false

    /**
     * Enable all debug logging (useful for comprehensive debugging).
     * This overrides individual flags when set to true.
     */
    var ENABLE_ALL_LOGGING = false
    
    /**
     * Helper function to check if a specific logging category is enabled.
     * Automatically returns true if ENABLE_ALL_LOGGING is set.
     */
    fun isEnabled(category: () -> Boolean): Boolean {
        return ENABLE_ALL_LOGGING || category()
    }
}
