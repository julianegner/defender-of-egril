@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.utils

// External JS function to get browser user agent
@JsFun("() => { try { return navigator.userAgent; } catch (e) { return null; } }")
private external fun getBrowserUserAgent(): String?

class WasmPlatform: Platform {
    override val name: String = getBrowserUserAgent()?.let { "Web with Kotlin/Wasm $it" } ?: "Web with Kotlin/Wasm"
    override val isAndroidTV: Boolean = false
}

actual fun getPlatform(): Platform = WasmPlatform()

// External JS function to get browser language
@JsFun("() => { try { return navigator.language.split('-')[0].toLowerCase(); } catch (e) { return null; } }")
private external fun getBrowserLanguage(): String?

actual fun getSystemLanguageCode(): String? {
    return getBrowserLanguage()
}

actual fun getCurrentUsername(): String = ""
