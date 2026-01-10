package de.egril.defender.ui

actual fun isEditorAvailable(): Boolean = true

actual fun getGameplayUIScale(): Float = 1.0f

actual fun exitApplication() {
    kotlin.system.exitProcess(0)
}
