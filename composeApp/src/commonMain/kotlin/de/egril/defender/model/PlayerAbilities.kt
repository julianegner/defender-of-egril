package de.egril.defender.model

/**
 * Player abilities and progression system
 */
data class PlayerAbilities(
    val totalXP: Int = 0,  // Total XP earned across all levels
    val level: Int = 1,  // Player level (based on XP)
    val availableAbilityPoints: Int = 0,  // Unspent ability points
    
    // Ability allocations
    val healthAbility: Int = 0,  // Each point gives bonus health
    val treasuryAbility: Int = 0,  // Each point gives +50 start coins
    val incomeAbility: Int = 0,  // Each point gives +10% coins from enemies
    val constructionAbility: Int = 0,  // Enables tower special abilities
    val manaAbility: Int = 0,  // Each point gives +5 max mana
    
    // Unlocked spells (spell IDs)
    val unlockedSpells: Set<SpellType> = emptySet()
) {
    companion object {
        // XP required for each level (cumulative)
        // Level 1: 0 XP, Level 2: 100 XP, Level 3: 250 XP, etc.
        private val XP_PER_LEVEL = listOf(
            0, 100, 250, 450, 700, 1000, 1350, 1750, 2200, 2700,
            3250, 3850, 4500, 5200, 5950, 6750, 7600, 8500, 9450, 10450,
            11500, 12600, 13750, 14950, 16200, 17500, 18850, 20250, 21700, 23200
        )
        
        // Construction levels unlock specific abilities
        const val CONSTRUCTION_LEVEL_1 = 1  // Spear level 10 barricade, Spike level 10 spike with barb
        const val CONSTRUCTION_LEVEL_2 = 2  // Spike level 20 barricade
        const val CONSTRUCTION_LEVEL_3 = 3  // Tower base (barricade level 100+ can hold tower)
        
        fun calculateLevel(xp: Int): Int {
            var level = 1
            for (i in XP_PER_LEVEL.indices) {
                if (xp >= XP_PER_LEVEL[i]) {
                    level = i + 1
                } else {
                    break
                }
            }
            return level
        }
        
        fun getXPForLevel(level: Int): Int {
            return if (level <= XP_PER_LEVEL.size) {
                XP_PER_LEVEL[level - 1]
            } else {
                // For levels beyond the table, use exponential growth
                val lastXP = XP_PER_LEVEL.last()
                val increment = 1550  // Increment increases
                lastXP + (level - XP_PER_LEVEL.size) * increment
            }
        }
        
        fun getXPForNextLevel(currentLevel: Int): Int {
            return getXPForLevel(currentLevel + 1)
        }
        
        fun getXPProgressInCurrentLevel(totalXP: Int, currentLevel: Int): Int {
            val currentLevelXP = getXPForLevel(currentLevel)
            return totalXP - currentLevelXP
        }
        
        fun getXPRequiredForCurrentLevel(currentLevel: Int): Int {
            val currentLevelXP = getXPForLevel(currentLevel)
            val nextLevelXP = getXPForNextLevel(currentLevel)
            return nextLevelXP - currentLevelXP
        }
    }
    
    /**
     * Calculate bonus health from health ability
     */
    fun getBonusHealth(): Int = healthAbility
    
    /**
     * Calculate bonus start coins from treasury ability
     */
    fun getBonusStartCoins(): Int = treasuryAbility * 50
    
    /**
     * Calculate income multiplier from income ability
     * Returns a multiplier (e.g., 1.2 for 20% bonus)
     */
    fun getIncomeMultiplier(): Double = 1.0 + (incomeAbility * 0.1)
    
    /**
     * Calculate max mana from mana ability
     */
    fun getMaxMana(): Int = manaAbility * 5
    
    /**
     * Check if a construction level is unlocked
     */
    fun isConstructionLevelUnlocked(level: Int): Boolean = constructionAbility >= level
    
    /**
     * Check if a spell is unlocked
     */
    fun isSpellUnlocked(spell: SpellType): Boolean = unlockedSpells.contains(spell)
    
    /**
     * Check if player has unlocked any spells
     */
    fun hasAnySpellUnlocked(): Boolean = unlockedSpells.isNotEmpty()
    
    /**
     * Add XP and return updated abilities with new level and ability points if leveled up
     */
    fun addXP(xp: Int): PlayerAbilities {
        val newTotalXP = totalXP + xp
        val newLevel = calculateLevel(newTotalXP)
        val levelDifference = newLevel - level
        val newAbilityPoints = availableAbilityPoints + levelDifference
        
        return copy(
            totalXP = newTotalXP,
            level = newLevel,
            availableAbilityPoints = newAbilityPoints
        )
    }
    
    /**
     * Spend an ability point on a specific ability
     */
    fun spendAbilityPoint(ability: AbilityType): PlayerAbilities? {
        if (availableAbilityPoints <= 0) return null
        
        return when (ability) {
            AbilityType.HEALTH -> copy(
                healthAbility = healthAbility + 1,
                availableAbilityPoints = availableAbilityPoints - 1
            )
            AbilityType.TREASURY -> copy(
                treasuryAbility = treasuryAbility + 1,
                availableAbilityPoints = availableAbilityPoints - 1
            )
            AbilityType.INCOME -> copy(
                incomeAbility = incomeAbility + 1,
                availableAbilityPoints = availableAbilityPoints - 1
            )
            AbilityType.CONSTRUCTION -> copy(
                constructionAbility = constructionAbility + 1,
                availableAbilityPoints = availableAbilityPoints - 1
            )
            AbilityType.MANA -> copy(
                manaAbility = manaAbility + 1,
                availableAbilityPoints = availableAbilityPoints - 1
            )
        }
    }
    
    /**
     * Unlock a spell by spending an ability point
     */
    fun unlockSpell(spell: SpellType): PlayerAbilities? {
        if (availableAbilityPoints <= 0) return null
        if (unlockedSpells.contains(spell)) return null
        
        return copy(
            unlockedSpells = unlockedSpells + spell,
            availableAbilityPoints = availableAbilityPoints - 1
        )
    }
}

/**
 * Ability types that can be upgraded
 */
enum class AbilityType {
    HEALTH,
    TREASURY,
    INCOME,
    CONSTRUCTION,
    MANA
}

/**
 * Spell types available in the game
 */
enum class SpellType(
    val displayName: String,
    val manaCost: Int,
    val description: String,
    val requiresTarget: Boolean = false,
    val targetType: SpellTargetType = SpellTargetType.NONE
) {
    ATTACK_AREA(
        displayName = "Attack Area",
        manaCost = 30,
        description = "Deal damage to all enemies in a large area",
        requiresTarget = true,
        targetType = SpellTargetType.POSITION
    ),
    ATTACK_AIMED(
        displayName = "Attack Aimed",
        manaCost = 15,
        description = "Deal heavy damage to the enemy on a targeted tile",
        requiresTarget = true,
        targetType = SpellTargetType.POSITION
    ),
    HEAL(
        displayName = "Heal",
        manaCost = 25,
        description = "Restore health points",
        requiresTarget = false,
        targetType = SpellTargetType.NONE
    ),
    INSTANT_TOWER(
        displayName = "Instant Tower",
        manaCost = 20,
        description = "Deploy the next tower you build instantly without any build time",
        requiresTarget = false,
        targetType = SpellTargetType.NONE
    ),
    BOMB(
        displayName = "Bomb",
        manaCost = 40,
        description = "Place a bomb that explodes after 2 turns, damaging enemies and structures",
        requiresTarget = true,
        targetType = SpellTargetType.POSITION
    ),
    DOUBLE_TOWER_LEVEL(
        displayName = "Double Tower Level",
        manaCost = 35,
        description = "Double a tower's level for one turn",
        requiresTarget = true,
        targetType = SpellTargetType.TOWER
    ),
    COOLING_SPELL(
        displayName = "Cooling Spell",
        manaCost = 30,
        description = "Create an area that slows enemies (lose 1 movement point)",
        requiresTarget = true,
        targetType = SpellTargetType.POSITION
    ),
    FREEZE_SPELL(
        displayName = "Freeze Spell",
        manaCost = 10,  // Base cost, can be increased for more turns
        description = "Freeze an enemy for one or more turns (does not work on Demons, Dragons, Ewhad)",
        requiresTarget = true,
        targetType = SpellTargetType.ENEMY
    ),
    DOUBLE_TOWER_REACH(
        displayName = "Double Tower Reach",
        manaCost = 25,
        description = "Double a tower's range for one turn",
        requiresTarget = true,
        targetType = SpellTargetType.TOWER
    )
}

/**
 * Target type for spell casting
 */
enum class SpellTargetType {
    NONE,      // No target required
    POSITION,  // Target a position on the map
    ENEMY,     // Target a specific enemy
    TOWER      // Target a specific tower
}

/**
 * Active spell effect in the game
 */
data class ActiveSpellEffect(
    val spell: SpellType,
    val position: Position? = null,
    val defenderId: Int? = null,
    val attackerId: Int? = null,
    val turnsRemaining: Int = 0,
    val castTurn: Int = 0
)
