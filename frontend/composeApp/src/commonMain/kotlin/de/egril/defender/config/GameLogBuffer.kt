package de.egril.defender.config

import de.egril.defender.utils.currentTimeMillis

/**
 * In-memory ring buffer that captures game log entries for bug reports.
 * Stores the most recent [MAX_ENTRIES] log lines so the feedback form can include
 * real gameplay logging alongside platform info.
 *
 * All access happens on the main/UI thread (Compose single-threaded model),
 * so no synchronization is needed. This ensures compatibility across all
 * KMP targets (JVM, Android, iOS, WASM).
 *
 * Usage:
 * ```
 * GameLogBuffer.log("COMBAT", "Tower dealt 15 damage to Goblin")
 * ```
 *
 * When the user submits a bug report with "Include game logs" checked,
 * [getFormattedLogs] returns the buffered entries as a single string.
 */
object GameLogBuffer {
    private const val MAX_ENTRIES = 200

    private val entries = ArrayDeque<LogEntry>(MAX_ENTRIES)

    data class LogEntry(
        val timestamp: Long,
        val category: String,
        val message: String
    )

    /**
     * Appends a log entry to the ring buffer and also prints it to stdout.
     * If the buffer is full, the oldest entry is discarded.
     */
    fun log(category: String, message: String) {
        val entry = LogEntry(
            timestamp = currentTimeMillis(),
            category = category,
            message = message
        )
        if (entries.size >= MAX_ENTRIES) {
            entries.removeFirst()
        }
        entries.addLast(entry)
        println("[$category] $message")
    }

    /**
     * Returns all buffered log entries formatted as a multi-line string.
     * Each line is: `[timestamp] [CATEGORY] message`
     */
    fun getFormattedLogs(): String {
        if (entries.isEmpty()) return "(no game log entries captured)"
        return buildString {
            for (entry in entries) {
                appendLine("[${entry.timestamp}] [${entry.category}] ${entry.message}")
            }
        }
    }

    /**
     * Returns the number of entries currently in the buffer.
     */
    fun size(): Int = entries.size

    /**
     * Clears all buffered log entries (e.g. on game restart).
     */
    fun clear() {
        entries.clear()
    }
}
