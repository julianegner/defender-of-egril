package de.egril.defender.utils

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Global state for the "bighead" cheat code effect.
 * When enabled, all enemy heads are drawn at double size.
 * Not persisted - resets when the app is closed.
 */
object BigHeadMode {
    val isEnabled: MutableState<Boolean> = mutableStateOf(false)
}
