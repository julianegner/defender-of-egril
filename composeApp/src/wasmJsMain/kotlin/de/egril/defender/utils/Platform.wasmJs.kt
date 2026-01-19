@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.utils

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val isAndroidTV: Boolean = false
}

actual fun getPlatform(): Platform = WasmPlatform()

// External JS function to get browser language
@JsFun("() => { try { return navigator.language.split('-')[0].toLowerCase(); } catch (e) { return null; } }")
private external fun getBrowserLanguage(): String?

actual fun getSystemLanguageCode(): String? {
    return getBrowserLanguage()
}
