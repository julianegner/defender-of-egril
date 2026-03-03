package de.egril.defender.audio

/**
 * Enum representing all sound events in the game
 */
enum class SoundEvent {
    // Tower attacks
    ATTACK_MELEE,
    ATTACK_RANGED,
    ATTACK_AREA,  // Fireball
    ATTACK_LASTING,  // Acid
    ATTACK_BALLISTA,  // Ballista
    
    // Enemy events
    ENEMY_SPAWN,
    ENEMY_MOVE,
    ENEMY_DESTROYED,
    
    // Mine events
    MINE_DIG,
    MINE_COIN_FOUND,
    MINE_TRAP_BUILT,
    MINE_DRAGON_SPAWN,
    
    // Trap events
    TRAP_TRIGGERED,
    
    // Life loss
    LIFE_LOST,
    
    // Dragon special
    DRAGON_EAT,
    
    // UI sounds (optional)
    TOWER_UPGRADED,
    TOWER_SOLD,
    
    // Game phase sounds
    BATTLE_START,
    
    // Bomb spell sounds
    BOMB_TICKING,
    BOMB_EXPLOSION
}
