package de.egril.defender.audio

import de.egril.defender.ui.settings.AppSettings

/**
 * Simple sound manager implementation using synthesized tones
 * This provides immediate audio feedback without requiring sound files
 * Can be enhanced later with actual sound effects
 */
class SimpleSoundManager : SoundManager {
    private var enabled = true
    private var volume = 1.0f
    
    override fun initialize() {
        // Load sound enabled state from settings
        enabled = AppSettings.isSoundEnabled.value && AppSettings.isEffectsEnabled.value
        volume = AppSettings.effectsVolume.value
    }
    
    override fun playSound(event: SoundEvent, volume: Float) {
        if (!enabled || this.volume <= 0f || !AppSettings.isSoundEnabled.value || !AppSettings.isEffectsEnabled.value) return
        
        // Calculate effective volume (master * effects * event-specific)
        val effectiveVolume = (AppSettings.soundVolume.value * this.volume * volume).coerceIn(0f, 1f)
        
        // Map events to tone characteristics (frequency, duration)
        val (frequency, duration) = when (event) {
            // Tower attacks - different pitches for different attacks
            SoundEvent.ATTACK_MELEE -> 400 to 100
            SoundEvent.ATTACK_RANGED -> 600 to 150
            SoundEvent.ATTACK_AREA -> 800 to 200  // Higher pitch for fireball
            SoundEvent.ATTACK_LASTING -> 300 to 250  // Lower, longer for acid
            SoundEvent.ATTACK_BALLISTA -> 500 to 200  // Deep, powerful
            
            // Enemy events
            SoundEvent.ENEMY_SPAWN -> 500 to 100
            SoundEvent.ENEMY_MOVE -> 350 to 50  // Short, low
            SoundEvent.ENEMY_DESTROYED -> 700 to 200
            
            // Mine events
            SoundEvent.MINE_DIG -> 250 to 150
            SoundEvent.MINE_COIN_FOUND -> 1200 to 100  // High ping
            SoundEvent.MINE_TRAP_BUILT -> 450 to 120
            SoundEvent.MINE_DRAGON_SPAWN -> 200 to 300  // Deep roar
            
            // Trap events
            SoundEvent.TRAP_TRIGGERED -> 900 to 150
            
            // Life loss
            SoundEvent.LIFE_LOST -> 150 to 400  // Low, long warning
            
            // Dragon special
            SoundEvent.DRAGON_EAT -> 300 to 250
            
            // UI sounds
            SoundEvent.TOWER_UPGRADED -> 1000 to 150
            SoundEvent.TOWER_SOLD -> 400 to 100
            
            // Game phase sounds
            SoundEvent.BATTLE_START -> 650 to 300  // Fanfare-like tone
        }
        
        // Play the tone on the platform
        playTone(frequency, duration, effectiveVolume)
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
        // Nothing to release for synthesized sounds
    }
    
    /**
     * Platform-specific tone generation
     * To be implemented in platform-specific files
     */
    private fun playTone(frequency: Int, durationMs: Int, volume: Float) {
        // This will be implemented in platform-specific expect/actual
        playToneImpl(frequency, durationMs, volume)
    }
}

/**
 * Expect function for platform-specific tone playback
 */
expect fun playToneImpl(frequency: Int, durationMs: Int, volume: Float)
