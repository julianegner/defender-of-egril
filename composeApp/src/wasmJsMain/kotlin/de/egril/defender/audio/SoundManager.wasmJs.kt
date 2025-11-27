@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.egril.defender.audio

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * External JS functions for Web Audio API
 */
@JsFun("() => new AudioContext()")
external fun createAudioContext(): JsAny

@JsFun("(ctx) => ctx.createOscillator()")
external fun createOscillator(context: JsAny): JsAny

@JsFun("(ctx) => ctx.createGain()")
external fun createGain(context: JsAny): JsAny

@JsFun("(ctx) => ctx.destination")
external fun getDestination(context: JsAny): JsAny

@JsFun("(osc, type) => osc.type = type")
external fun setOscillatorType(oscillator: JsAny, type: String)

@JsFun("(osc) => osc.frequency")
external fun getFrequency(oscillator: JsAny): JsAny

@JsFun("(param, value) => param.value = value")
external fun setParamValue(param: JsAny, value: Double)

@JsFun("(gain) => gain.gain")
external fun getGain(gainNode: JsAny): JsAny

@JsFun("(source, dest) => source.connect(dest)")
external fun connect(source: JsAny, destination: JsAny)

@JsFun("(node) => node.disconnect()")
external fun disconnect(node: JsAny)

@JsFun("(osc) => osc.start()")
external fun startOscillator(oscillator: JsAny)

@JsFun("(osc) => osc.stop()")
external fun stopOscillator(oscillator: JsAny)

/**
 * Web/WASM implementation of SoundManager factory
 */
actual fun createSoundManager(): SoundManager = FileSoundManager()

/**
 * Web implementation of tone playback using Web Audio API
 */
actual fun playToneImpl(frequency: Int, durationMs: Int, volume: Float) {
    // Play tone asynchronously using Web Audio API
    GlobalScope.launch {
        try {
            // Create audio context
            val audioContext = createAudioContext()
            
            // Create oscillator
            val oscillator = createOscillator(audioContext)
            setOscillatorType(oscillator, "sine")
            val freqParam = getFrequency(oscillator)
            setParamValue(freqParam, frequency.toDouble())
            
            // Create gain node for volume control
            val gainNode = createGain(audioContext)
            val gainParam = getGain(gainNode)
            setParamValue(gainParam, volume.toDouble())
            
            // Connect nodes: oscillator -> gain -> destination
            val destination = getDestination(audioContext)
            connect(oscillator, gainNode)
            connect(gainNode, destination)
            
            // Play for specified duration
            startOscillator(oscillator)
            window.setTimeout({
                stopOscillator(oscillator)
                disconnect(oscillator)
                disconnect(gainNode)
                null // Return null to match JsAny? expected type
            }, durationMs)
        } catch (e: Exception) {
            // Silently fail if audio playback fails
            println("Audio playback failed: ${e.message}")
        }
    }
}
