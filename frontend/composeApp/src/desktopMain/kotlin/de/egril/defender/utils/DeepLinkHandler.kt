package de.egril.defender.utils

/**
 * Desktop (JVM) implementation: Deep linking via URL not supported.
 */
actual fun getCurrentPathname(): String? {
    return null
}
