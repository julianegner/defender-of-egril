package de.egril.defender.utils

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DigOutcome
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel

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
     * @param setCoins Callback to set coins to a specific value
     * @param performMineDigWithOutcome Callback to perform mine dig with a specific outcome
     * @param spawnEnemy Callback to spawn an enemy
     * @param showPlatformInfo Callback to show platform information (optional)
     * @param addMana Callback to add mana during gameplay (optional)
     * @param removeMana Callback to remove mana during gameplay (optional)
     * @param showCheatHelp Callback to show cheat code help screen (optional)
     * @return Pair of (success: Boolean, digOutcome: DigOutcome?). Success is true if the cheat code 
     *         was recognized and applied. DigOutcome is non-null if a dig cheat was applied.
     */
    fun applyCheatCode(
        code: String,
        addCoins: (Int) -> Unit,
        setCoins: (Int) -> Unit,
        performMineDigWithOutcome: (DigOutcome) -> DigOutcome?,
        spawnEnemy: (AttackerType, Int) -> Unit,
        showPlatformInfo: (() -> Unit)? = null,
        addMana: ((Int) -> Unit)? = null,
        removeMana: ((Int) -> Unit)? = null,
        showCheatHelp: (() -> Unit)? = null
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
            "emptypocket" -> {
                setCoins(0)
                return Pair(true, null)
            }
            "platform" -> {
                showPlatformInfo?.invoke()
                return Pair(true, null)
            }
            "cheat", "cheats", "help" -> {
                showCheatHelp?.invoke()
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
                    "greenwitch" -> AttackerType.GREEN_WITCH
                    "redwitch" -> AttackerType.RED_WITCH
                    else -> return Pair(false, null)
                }
                
                spawnEnemy(attackerType, level)
                return Pair(true, null)
            }
        }
        
        // Handle "addmana <amount>" cheatcode  
        if (lowercaseCode.startsWith("addmana ") && addMana != null) {
            val parts = lowercaseCode.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val amount = parts[1].toIntOrNull() ?: return Pair(false, null)
                addMana(amount)
                return Pair(true, null)
            }
        }
        
        // Handle "removemana <amount>" cheatcode
        if (lowercaseCode.startsWith("removemana ") && removeMana != null) {
            val parts = lowercaseCode.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val amount = parts[1].toIntOrNull() ?: return Pair(false, null)
                removeMana(amount)
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
     * @param unlockLevel Callback to unlock a specific level by index or ID
     * @param lockAllLevels Callback to lock all levels
     * @param lockLevel Callback to lock a specific level by index or ID
     * @param worldLevels Current list of world levels (needed to validate level references)
     * @param showPlatformInfo Callback to show platform information (optional)
     * @param addXP Callback to add XP to player (optional)
     * @param removeXP Callback to remove XP from player (optional)
     * @param addStatLevel Callback to add levels to a specific stat (optional)
     * @param removeStatLevel Callback to remove levels from a specific stat (optional)
     * @param unlockSpell Callback to unlock a specific spell (optional)
     * @param lockSpell Callback to lock a specific spell (optional)
     * @param showCheatHelp Callback to show cheat code help screen (optional)
     * @return true if the cheat code was recognized and applied, false otherwise
     */
    fun applyWorldMapCheatCode(
        code: String,
        unlockAllLevels: () -> Unit,
        unlockLevel: ((String) -> Unit)? = null,
        lockAllLevels: (() -> Unit)? = null,
        lockLevel: ((String) -> Unit)? = null,
        worldLevels: List<WorldLevel>? = null,
        showPlatformInfo: (() -> Unit)? = null,
        addXP: ((Int) -> Unit)? = null,
        removeXP: ((Int) -> Unit)? = null,
        addStatLevel: ((String, Int) -> Unit)? = null,
        removeStatLevel: ((String, Int) -> Unit)? = null,
        unlockSpell: ((String) -> Unit)? = null,
        lockSpell: ((String) -> Unit)? = null,
        showCheatHelp: (() -> Unit)? = null
    ): Boolean {
        val lowercaseCode = code.lowercase().trim()
        
        // Handle "unlockall" cheatcode to unlock all levels
        when (lowercaseCode) {
            "unlockall", "unlock all" -> {
                unlockAllLevels()
                return true
            }
            "lockall", "lock all" -> {
                lockAllLevels?.invoke()
                return true
            }
            "platform" -> {
                showPlatformInfo?.invoke()
                return true
            }
            "cheat", "cheats", "help" -> {
                showCheatHelp?.invoke()
                return true
            }
        }
        
        // Handle "unlock <index>" or "unlock <level_id>" cheatcode to unlock a specific level
        if (lowercaseCode.startsWith("unlock ") && unlockLevel != null && worldLevels != null) {
            val levelReference = lowercaseCode.removePrefix("unlock ").trim()
            
            // Try to match by level ID (with or without "level" prefix and with underscores or spaces)
            val normalizedReference = levelReference
                .removePrefix("level ")
                .replace(" ", "_")
            
            // Try to parse as a 1-based index (after removing "level" prefix)
            val index = normalizedReference.toIntOrNull()
            if (index != null) {
                // 1-based index (e.g., "unlock 5" or "unlock level 5" means the 5th level in the list)
                if (index >= 1 && index <= worldLevels.size) {
                    val worldLevel = worldLevels[index - 1]
                    worldLevel.level.editorLevelId?.let { levelId ->
                        unlockLevel(levelId)
                        return true
                    }
                }
                return false
            }
            
            // Check if any level has a matching ID
            val matchingLevel = worldLevels.find { worldLevel ->
                val levelId = worldLevel.level.editorLevelId ?: return@find false
                levelId.lowercase() == normalizedReference
            }
            
            matchingLevel?.level?.editorLevelId?.let { levelId ->
                unlockLevel(levelId)
                return true
            }
            
            return false
        }
        
        // Handle "lock <index>" or "lock <level_id>" cheatcode to lock a specific level
        if (lowercaseCode.startsWith("lock ") && lockLevel != null && worldLevels != null) {
            val levelReference = lowercaseCode.removePrefix("lock ").trim()
            
            // Try to match by level ID (with or without "level" prefix and with underscores or spaces)
            val normalizedReference = levelReference
                .removePrefix("level ")
                .replace(" ", "_")
            
            // Try to parse as a 1-based index (after removing "level" prefix)
            val index = normalizedReference.toIntOrNull()
            if (index != null) {
                // 1-based index (e.g., "lock 5" or "lock level 5" means the 5th level in the list)
                if (index >= 1 && index <= worldLevels.size) {
                    val worldLevel = worldLevels[index - 1]
                    worldLevel.level.editorLevelId?.let { levelId ->
                        lockLevel(levelId)
                        return true
                    }
                }
                return false
            }
            
            // Check if any level has a matching ID
            val matchingLevel = worldLevels.find { worldLevel ->
                val levelId = worldLevel.level.editorLevelId ?: return@find false
                levelId.lowercase() == normalizedReference
            }
            
            matchingLevel?.level?.editorLevelId?.let { levelId ->
                lockLevel(levelId)
                return true
            }
            
            return false
        }
        
        // Handle "addxp <amount>" cheatcode
        if (lowercaseCode.startsWith("addxp ") && addXP != null) {
            val parts = lowercaseCode.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val amount = parts[1].toIntOrNull() ?: return false
                addXP(amount)
                return true
            }
        }
        
        // Handle "removexp <amount>" cheatcode
        if (lowercaseCode.startsWith("removexp ") && removeXP != null) {
            val parts = lowercaseCode.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val amount = parts[1].toIntOrNull() ?: return false
                removeXP(amount)
                return true
            }
        }
        
        // Handle "addstat <statname> <amount>" cheatcode
        if (lowercaseCode.startsWith("addstat ") && addStatLevel != null) {
            val parts = lowercaseCode.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 3) {
                val statName = parts[1]
                val amount = parts[2].toIntOrNull() ?: return false
                addStatLevel(statName, amount)
                return true
            }
        }
        
        // Handle "removestat <statname> <amount>" cheatcode
        if (lowercaseCode.startsWith("removestat ") && removeStatLevel != null) {
            val parts = lowercaseCode.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 3) {
                val statName = parts[1]
                val amount = parts[2].toIntOrNull() ?: return false
                removeStatLevel(statName, amount)
                return true
            }
        }
        
        // Handle "unlockspell <spellname>" cheatcode
        if (lowercaseCode.startsWith("unlockspell ") && unlockSpell != null) {
            val parts = lowercaseCode.split(" ", limit = 2).filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val spellName = parts[1].trim()
                unlockSpell(spellName)
                return true
            }
        }
        
        // Handle "lockspell <spellname>" cheatcode
        if (lowercaseCode.startsWith("lockspell ") && lockSpell != null) {
            val parts = lowercaseCode.split(" ", limit = 2).filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val spellName = parts[1].trim()
                lockSpell(spellName)
                return true
            }
        }
        
        // Legacy support: bare "unlock" still unlocks all levels
        if (lowercaseCode == "unlock") {
            unlockAllLevels()
            return true
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
    
    /**
     * Unlock a specific level in the world map by its editor level ID.
     * 
     * @param worldLevels The current list of world levels
     * @param editorLevelId The editor level ID to unlock
     * @return The updated list of world levels with the specified level unlocked (if found and locked)
     */
    fun unlockLevel(worldLevels: List<WorldLevel>, editorLevelId: String): List<WorldLevel> {
        return worldLevels.map { worldLevel ->
            if (worldLevel.level.editorLevelId == editorLevelId && worldLevel.status == LevelStatus.LOCKED) {
                worldLevel.copy(status = LevelStatus.UNLOCKED)
            } else {
                worldLevel
            }
        }
    }
    
    /**
     * Lock all levels in the world map that HAVE prerequisites.
     * Only levels with prerequisites can be locked.
     * 
     * @param worldLevels The current list of world levels
     * @return The updated list of world levels with levels that have prerequisites changed to LOCKED
     */
    fun lockAllLevels(worldLevels: List<WorldLevel>): List<WorldLevel> {
        // Get all editor level IDs and their prerequisites
        val editorLevelsMap = try {
            de.egril.defender.editor.EditorStorage.getAllLevels().associateBy { it.id }
        } catch (e: Exception) {
            emptyMap()
        }
        
        return worldLevels.map { worldLevel ->
            val editorLevelId = worldLevel.level.editorLevelId
            val hasPrerequisites = if (editorLevelId != null) {
                editorLevelsMap[editorLevelId]?.prerequisites?.isNotEmpty() == true
            } else {
                false
            }
            
            // Lock levels that HAVE prerequisites
            if (hasPrerequisites) {
                worldLevel.copy(status = LevelStatus.LOCKED)
            } else {
                worldLevel
            }
        }
    }
    
    /**
     * Lock a specific level in the world map by its editor level ID.
     * The level can only be locked if it HAS prerequisites (not an entry level).
     * 
     * @param worldLevels The current list of world levels
     * @param editorLevelId The editor level ID to lock
     * @return The updated list of world levels with the specified level locked (if found and has prerequisites)
     */
    fun lockLevel(worldLevels: List<WorldLevel>, editorLevelId: String): List<WorldLevel> {
        // Get the editor level to check prerequisites
        val editorLevel = try {
            de.egril.defender.editor.EditorStorage.getLevel(editorLevelId)
        } catch (e: Exception) {
            null
        }
        
        val hasPrerequisites = editorLevel?.prerequisites?.isNotEmpty() == true
        
        return worldLevels.map { worldLevel ->
            // Lock if matches editorLevelId and HAS prerequisites
            if (worldLevel.level.editorLevelId == editorLevelId && hasPrerequisites) {
                worldLevel.copy(status = LevelStatus.LOCKED)
            } else {
                worldLevel
            }
        }
    }
}
