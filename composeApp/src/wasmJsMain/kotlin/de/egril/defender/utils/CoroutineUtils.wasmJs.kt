@file:Suppress("UnsafeCastFromDynamic")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.egril.defender.utils

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

external object console {
    fun error(message: String)
}

/**
 * WasmJS implementation - uses GlobalScope.promise as runBlocking is not available in WASM
 * 
 * Note: This implementation has limitations:
 * - Cannot truly block execution in WASM
 * - Currently only supports Boolean return types (returns false as default)
 * - Designed for initialization code that can gracefully handle false/failure
 * 
 * The asynchronous operation is started but a default "failed" value is returned immediately.
 * This allows the application to start without waiting, and repository loading can be retried later.
 */
@OptIn(DelicateCoroutinesApi::class)
actual fun <T> runBlockingCompat(block: suspend () -> T): T {
    // Start the async operation but don't wait for it
    GlobalScope.promise {
        try {
            block()
        } catch (e: Throwable) {
            console.error("Error in runBlockingCompat: ${e.message}")
            null
        }
    }
    
    // Return a safe default value
    // This implementation assumes T is Boolean (the only current usage)
    // If other types are needed, this should be refactored to use a nullable return or default parameter
    @Suppress("UNCHECKED_CAST")
    return (false as? T) ?: throw UnsupportedOperationException(
        "runBlockingCompat on WASM only supports Boolean return type"
    )
}
