package de.egril.defender.utils

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun getSystemLanguageCode(): String? {
    return try {
        // Access browser's navigator.language
        js("navigator.language.split('-')[0].toLowerCase()") as? String
    } catch (e: Exception) {
        null
    }
}
