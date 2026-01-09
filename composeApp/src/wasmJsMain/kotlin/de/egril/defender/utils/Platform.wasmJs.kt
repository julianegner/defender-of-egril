package de.egril.defender.utils

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val isAndroidTV: Boolean = false
}

actual fun getPlatform(): Platform = WasmPlatform()
