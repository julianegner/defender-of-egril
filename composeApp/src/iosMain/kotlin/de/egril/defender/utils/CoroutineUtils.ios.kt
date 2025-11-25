package de.egril.defender.utils

import kotlinx.coroutines.runBlocking

/**
 * iOS implementation using kotlinx.coroutines.runBlocking
 */
actual fun <T> runBlockingCompat(block: suspend () -> T): T {
    return runBlocking { block() }
}
