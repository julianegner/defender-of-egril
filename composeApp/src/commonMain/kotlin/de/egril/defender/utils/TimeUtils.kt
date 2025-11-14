package de.egril.defender.utils

/**
 * Get current time in milliseconds since epoch.
 * Platform-specific implementation.
 */
expect fun currentTimeMillis(): Long

/**
 * Format a timestamp to a human-readable date string.
 * Platform-specific implementation.
 */
expect fun formatTimestamp(timestamp: Long): String
