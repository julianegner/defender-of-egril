package com.defenderofegril.utils

import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DigOutcome
import com.defenderofegril.model.LevelStatus
import com.defenderofegril.model.WorldLevel

/**
 * Handler for cheat codes used in the game.
 * Separates cheat code logic from the GameViewModel.
 */
object CheatCodeHandler {
    
    /**
     * Apply a cheat code during gameplay.
     * 
     * @param code The cheat code to apply
     * @param addCoins Callback to add coins to the game
     * @param performMineDigWithOutcome Callback to perform mine dig with a specific outcome
     * @param spawnEnemy Callback to spawn an enemy
     * @return Pair of (success: Boolean, digOutcome: DigOutcome?). Success is true if the cheat code 
     *         was recognized and applied. DigOutcome is non-null if a dig cheat was applied.
     */
    fun applyCheatCode(
        code: String,
        addCoins: (Int) -> Unit,
        performMineDigWithOutcome: (DigOutcome) -> DigOutcome?,
        spawnEnemy: (AttackerType, Int) -> Unit
    ): Pair<Boolean, DigOutcome?> {
        val lowercaseCode = code.lowercase().trim()
        
        // Helper function to apply dig outcome cheat
        fun applyDigCheat(outcome: DigOutcome): Pair<Boolean, DigOutcome?> {
            val result = performMineDigWithOutcome(outcome)
            return if (result != null) {
                Pair(true, result)
            } else {
                Pair(false, null)
            }
        }
        
        // Handle simple one-word cheatcodes
        when (lowercaseCode) {
            "cash" -> {
                addCoins(1000)
                return Pair(true, null)
            }
            "mmmoney" -> {
                addCoins(1000000)
                return Pair(true, null)
            }
            // Dig outcome cheat codes
            "dig nothing", "dig rubble" -> return applyDigCheat(DigOutcome.NOTHING)
            "dig brass" -> return applyDigCheat(DigOutcome.BRASS)
            "dig silver" -> return applyDigCheat(DigOutcome.SILVER)
            "dig gold" -> return applyDigCheat(DigOutcome.GOLD)
            "dig gems", "dig gem" -> return applyDigCheat(DigOutcome.GEMS)
            "dig diamond" -> return applyDigCheat(DigOutcome.DIAMOND)
            "dig dragon", "dragon" -> return applyDigCheat(DigOutcome.DRAGON)
        }
        
        // Handle "spawn <type> <level>" cheatcode
        if (lowercaseCode.startsWith("spawn ")) {
            val parts = lowercaseCode.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val typeName = parts[1]
                val level = if (parts.size >= 3) parts[2].toIntOrNull() ?: 1 else 1
                
                // Map type name to AttackerType
                val attackerType = when (typeName) {
                    "goblin" -> AttackerType.GOBLIN
                    "ork", "orc" -> AttackerType.ORK
                    "ogre" -> AttackerType.OGRE
                    "skeleton" -> AttackerType.SKELETON
                    "wizard", "evil_wizard", "evilwizard" -> AttackerType.EVIL_WIZARD
                    "witch" -> AttackerType.WITCH
                    else -> return Pair(false, null)
                }
                
                spawnEnemy(attackerType, level)
                return Pair(true, null)
            }
        }
        
        return Pair(false, null)
    }
    
    /**
     * Apply a cheat code on the world map screen.
     * 
     * @param code The cheat code to apply
     * @param unlockAllLevels Callback to unlock all levels
     * @return true if the cheat code was recognized and applied, false otherwise
     */
    fun applyWorldMapCheatCode(
        code: String,
        unlockAllLevels: () -> Unit
    ): Boolean {
        val lowercaseCode = code.lowercase().trim()
        
        // Handle "unlock" or "unlockall" cheatcode to unlock all levels
        when (lowercaseCode) {
            "unlock", "unlockall", "unlock all" -> {
                unlockAllLevels()
                return true
            }
        }
        
        return false
    }
    
    /**
     * Unlock all levels in the world map.
     * 
     * @param worldLevels The current list of world levels
     * @return The updated list of world levels with all LOCKED levels changed to UNLOCKED
     */
    fun unlockAllLevels(worldLevels: List<WorldLevel>): List<WorldLevel> {
        return worldLevels.map { worldLevel ->
            when (worldLevel.status) {
                LevelStatus.LOCKED -> worldLevel.copy(status = LevelStatus.UNLOCKED)
                else -> worldLevel
            }
        }
    }
}
