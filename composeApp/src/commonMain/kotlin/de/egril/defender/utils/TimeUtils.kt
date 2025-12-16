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

/**
 * Format a timestamp to ISO 8601 format for filenames (YYYY-MM-DD_HH-mm-ss).
 * Platform-specific implementation.
 */
expect fun formatTimestampISO(timestamp: Long): String
