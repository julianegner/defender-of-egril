@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import de.egril.defender.utils.CrashReporter
import de.egril.defender.utils.WindowCloseHandler
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body ?: error("Document body not found")

    // Install global JS error handlers so any uncaught exception (including
    // ones thrown deep inside Compose input handling, e.g. text selection)
    // is funnelled into the shared CrashReporter and surfaced via the error
    // boundary dialog instead of silently freezing/crashing the page.
    setupGlobalErrorHandlers()

    // Set up beforeunload event handler for browser
    setupBeforeUnloadHandler()
    setupPageHideHandler()

    ComposeViewport(body) {
        App()
    }
}

private fun setupGlobalErrorHandlers() {
    installWindowErrorHandler { message ->
        CrashReporter.report(errorType = "JavaScript Error", errorMessage = message)
    }
    installUnhandledRejectionHandler { message ->
        CrashReporter.report(errorType = "Unhandled Promise Rejection", errorMessage = message)
    }
}

@JsFun(
    "(callback) => { window.onerror = (message, source, lineno, colno, error) => { " +
        "callback(String(error && error.stack ? error.stack : message)); return true; }; }",
)
private external fun installWindowErrorHandler(callback: (String) -> Unit)

@JsFun(
    "(callback) => { window.addEventListener('unhandledrejection', (event) => { " +
        "callback(String(event.reason)); }); }",
)
private external fun installUnhandledRejectionHandler(callback: (String) -> Unit)

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
