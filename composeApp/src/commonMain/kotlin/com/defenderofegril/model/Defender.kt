package com.defenderofegril.model

enum class DefenderType(
    val displayName: String,
    val baseCost: Int,
    val baseDamage: Int,
    val baseRange: Int,
    val attackType: AttackType
) {
    SPIKE_TOWER("Spike Tower", baseCost = 10, baseDamage = 5, baseRange = 1, attackType = AttackType.MELEE),
    SPEAR_TOWER("Spear Tower", baseCost = 15, baseDamage = 8, baseRange = 2, attackType = AttackType.RANGED),
    BOW_TOWER("Bow Tower", baseCost = 20, baseDamage = 10, baseRange = 3, attackType = AttackType.RANGED),
    WIZARD_TOWER("Wizard Tower", baseCost = 50, baseDamage = 30, baseRange = 3, attackType = AttackType.AOE),
    ALCHEMY_TOWER("Alchemy Tower", baseCost = 40, baseDamage = 15, baseRange = 2, attackType = AttackType.DOT)
}

enum class AttackType {
    MELEE,    // Single target, close range
    RANGED,   // Single target, long range
    AOE,      // Area of Effect - affects multiple enemies
    DOT       // Damage over Time - lower damage but lasts multiple rounds
}

data class Defender(
    val id: Int,
    val type: DefenderType,
    val position: Position,
    var level: Int = 1,
    var dotRoundsRemaining: MutableMap<Int, Int> = mutableMapOf() // attackerId -> rounds
) {
    val damage: Int get() = type.baseDamage + (level - 1) * 5
    val range: Int get() = type.baseRange + (level - 1) / 2
    val upgradeCost: Int get() = type.baseCost * level
    
    fun canAttack(attacker: Attacker): Boolean {
        return position.distanceTo(attacker.position) <= range
    }
}
