@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import de.egril.defender.utils.WindowCloseHandler
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body ?: error("Document body not found")

    // Set up beforeunload event handler for browser
    setupBeforeUnloadHandler()
    setupPageHideHandler()

    ComposeViewport(body) {
        App()
    }
}

/**
 * Set up the beforeunload event handler to warn users about unsaved changes
 */
private fun setupBeforeUnloadHandler() {
    window.onbeforeunload = { event ->
        if (WindowCloseHandler.hasUnsavedChanges()) {
            // For modern browsers, setting returnValue triggers the confirmation dialog
            event.returnValue = "You have unsaved changes. Are you sure you want to leave?"
            // Return a string to show the browser's confirmation dialog
            "You have unsaved changes. Are you sure you want to leave?"
        } else {
            null
        }
    }
}

@JsFun("(callback) => { window.addEventListener('pagehide', () => callback()); }")
private external fun addPageHideListener(callback: () -> Unit)

private fun setupPageHideHandler() {
    addPageHideListener {
        WindowCloseHandler.reportAppClosed()
    }
}
