package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings

/**
 * Sound manager that plays audio files from resources
 * Falls back to synthesized tones if files are not available
 */
class FileSoundManager : SoundManager {
    private var enabled = true
    private var volume = 1.0f
    
    override fun initialize() {
        // Load sound enabled state from settings
        enabled = AppSettings.isSoundEnabled.value && AppSettings.isEffectsEnabled.value
        volume = AppSettings.effectsVolume.value
        
        // Initialize platform-specific audio system
        initializeAudioSystem()
    }
    
    override fun playSound(event: SoundEvent, volume: Float) {
        if (!enabled || this.volume <= 0f || !AppSettings.isSoundEnabled.value || !AppSettings.isEffectsEnabled.value) return
        
        // Calculate effective volume (master * effects * event-specific)
        val effectiveVolume = (AppSettings.soundVolume.value * this.volume * volume).coerceIn(0f, 1f)
        
        // Map events to sound file names
        val soundFileName = when (event) {
            // Tower attacks
            SoundEvent.ATTACK_MELEE -> "attack_melee.wav"
            SoundEvent.ATTACK_RANGED -> "attack_ranged.wav"
            SoundEvent.ATTACK_AREA -> "attack_area.wav"
            SoundEvent.ATTACK_LASTING -> "attack_lasting.wav"
            SoundEvent.ATTACK_BALLISTA -> "attack_ballista.wav"
            
            // Enemy events
            SoundEvent.ENEMY_SPAWN -> "enemy_spawn.wav"
            SoundEvent.ENEMY_MOVE -> "enemy_move.wav"
            SoundEvent.ENEMY_DESTROYED -> "enemy_destroyed.wav"
            
            // Mine events
            SoundEvent.MINE_DIG -> "mine_dig.wav"
            SoundEvent.MINE_COIN_FOUND -> "mine_coin.wav"
            SoundEvent.MINE_TRAP_BUILT -> "mine_trap.wav"
            SoundEvent.MINE_DRAGON_SPAWN -> "mine_dragon.wav"
            
            // Trap events
            SoundEvent.TRAP_TRIGGERED -> "trap_trigger.wav"
            
            // Life loss
            SoundEvent.LIFE_LOST -> "life_lost.wav"
            
            // Dragon special
            SoundEvent.DRAGON_EAT -> "dragon_eat.wav"
            
            // UI sounds
            SoundEvent.TOWER_UPGRADED -> "tower_upgraded.wav"
            SoundEvent.TOWER_SOLD -> "tower_sold.wav"
            
            // Game phase sounds
            SoundEvent.BATTLE_START -> "battle_start.wav"
            
            // Bomb spell sounds: mapped to closest-matching existing files until dedicated sounds are added
            SoundEvent.BOMB_TICKING -> "trap_trigger.wav"    // Click/tick sound stand-in
            SoundEvent.BOMB_EXPLOSION -> "attack_area.wav"   // Explosion stand-in
        }
        
        // Play the sound file on the platform
        playSoundFile(soundFileName, effectiveVolume)
    }
    
    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }
    
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    override fun isEnabled(): Boolean = enabled
    
    override fun getVolume(): Float = volume
    
    override fun release() {
        releaseAudioSystem()
    }
}

/**
 * Platform-specific audio system initialization
 */
expect fun initializeAudioSystem()

/**
 * Platform-specific sound file playback
 */
expect fun playSoundFile(fileName: String, volume: Float)

/**
 * Platform-specific audio system cleanup
 */
expect fun releaseAudioSystem()
