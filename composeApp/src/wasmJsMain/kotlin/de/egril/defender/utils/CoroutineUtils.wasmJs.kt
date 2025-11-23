package de.egril.defender.utils

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

/**
 * WasmJS implementation - uses GlobalScope.promise as runBlocking is not available in WASM
 * Note: This is a workaround for initialization code. In production, consider using LaunchedEffect instead.
 */
@OptIn(DelicateCoroutinesApi::class)
actual fun <T> runBlockingCompat(block: suspend () -> T): T {
    // For WASM, we can't truly block, so we use a placeholder
    // This should only be used during initialization where blocking behavior is acceptable
    var result: T? = null
    var exception: Throwable? = null
    
    // Execute the block and capture result/exception
    GlobalScope.promise {
        try {
            result = block()
        } catch (e: Throwable) {
            exception = e
        }
    }
    
    // In WASM, we can't actually wait, so this will return null on first call
    // The caller should handle this gracefully by checking for null or using lazy initialization
    exception?.let { throw it }
    return result ?: throw IllegalStateException("runBlockingCompat on WASM cannot block - use lazy initialization instead")
}
