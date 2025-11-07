package com.defenderofegril.audio

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise
import kotlin.math.PI
import kotlin.math.sin

/**
 * Web/WASM implementation of SoundManager factory
 */
actual fun createSoundManager(): SoundManager = SimpleSoundManager()

/**
 * Web implementation of tone playback using Web Audio API
 */
actual fun playToneImpl(frequency: Int, durationMs: Int, volume: Float) {
    // Play tone asynchronously using Web Audio API
    GlobalScope.launch {
        try {
            // Create audio context
            val audioContext = js("new AudioContext()") as dynamic
            
            // Create oscillator
            val oscillator = audioContext.createOscillator() as dynamic
            oscillator.type = "sine"
            oscillator.frequency.value = frequency
            
            // Create gain node for volume control
            val gainNode = audioContext.createGain() as dynamic
            gainNode.gain.value = volume
            
            // Connect nodes: oscillator -> gain -> destination
            oscillator.connect(gainNode)
            gainNode.connect(audioContext.destination)
            
            // Play for specified duration
            oscillator.start()
            window.setTimeout({
                oscillator.stop()
                oscillator.disconnect()
                gainNode.disconnect()
            }, durationMs)
        } catch (e: Exception) {
            // Silently fail if audio playback fails
            console.log("Audio playback failed: ${e.message}")
        }
    }
}
