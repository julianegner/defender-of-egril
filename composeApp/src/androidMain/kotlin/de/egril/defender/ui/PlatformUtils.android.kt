package de.egril.defender.ui

import de.egril.defender.AndroidContextProvider

actual fun isEditorAvailable(): Boolean = false

actual fun getGameplayUIScale(): Float = 0.5f

actual fun exitApplication() {
    // On Android, finish the activity
    val activity = AndroidContextProvider.getActivity()
    activity?.finish()
}
