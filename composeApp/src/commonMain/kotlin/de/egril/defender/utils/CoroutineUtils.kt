package de.egril.defender.utils

/**
 * Platform-specific implementation of blocking coroutine execution.
 * This is an expect function that must be implemented in each platform.
 */
expect fun <T> runBlockingCompat(block: suspend () -> T): T
