package de.egril.defender.utils

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.config.GameLogBuffer
import kotlin.random.Random

/**
 * Information about a captured unhandled error that should be presented to
 * the user through the global crash dialog.
 *
 * `errorType` is a non-localized class/qualified name (e.g.
 * `java.lang.IllegalStateException`) used by the backend for grouping.
 * `errorMessage` is the original (non-localized) message of the throwable.
 * `stackTrace` is best-effort and may be null on platforms that do not
 * expose stack traces.
 */
data class CrashInfo(
    val crashId: String,
    val errorType: String,
    val errorMessage: String?,
    val stackTrace: String?,
    val capturedAtMillis: Long
)

/**
 * Process-wide singleton that owns the currently-displayed crash, if any.
 *
 * Anything in the app that could crash – top-level composition, coroutines,
 * platform-specific uncaught handlers – funnels into [report]. The global
 * [de.egril.defender.ui.crash.CrashReportDialog] observes [current] and
 * blocks the rest of the UI while a crash is active.
 *
 * Only the first error is shown; subsequent errors arriving while the
 * dialog is open are dropped so we never stack multiple dialogs.
 */
object CrashReporter {
    val current = mutableStateOf<CrashInfo?>(null)

    /**
     * Records a crash. No-op if a crash dialog is already being shown so we
     * do not stomp on the existing dialog or display nested errors.
     */
    fun report(throwable: Throwable) {
        if (current.value != null) return
        val info = CrashInfo(
            crashId = generateCrashUuid(),
            errorType = (throwable::class.qualifiedName ?: throwable::class.simpleName ?: "UnknownError"),
            errorMessage = throwable.message,
            stackTrace = runCatching { throwable.stackTraceToString() }.getOrNull(),
            capturedAtMillis = currentTimeMillis()
        )
        // Best-effort: also record it in the in-memory game log so the
        // attached log already contains the error if the user later opens
        // the feedback form.
        runCatching { GameLogBuffer.log("CRASH", "${info.errorType}: ${info.errorMessage ?: ""}") }
        current.value = info
    }

    /**
     * Convenience overload for non-`Throwable` failure sources (e.g. coming
     * from JS / Wasm hosts where only a string is available).
     */
    fun report(errorType: String, errorMessage: String?, stackTrace: String? = null) {
        if (current.value != null) return
        current.value = CrashInfo(
            crashId = generateCrashUuid(),
            errorType = errorType,
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            capturedAtMillis = currentTimeMillis()
        )
    }

    fun clear() {
        current.value = null
    }
}

/**
 * Runs [block] and reports any thrown [Throwable] through [CrashReporter]
 * instead of crashing the app. Returns the result of [block] on success,
 * or null when the block threw.
 *
 * Intended for wrapping anything that could lead to the app crashing –
 * event handlers, side-effects, JSON parsing, file I/O, etc.
 */
inline fun <T> safeRun(block: () -> T): T? = try {
    block()
} catch (t: Throwable) {
    CrashReporter.report(t)
    null
}

internal fun generateCrashUuid(): String {
    val bytes = ByteArray(16) { Random.Default.nextInt(256).toByte() }
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte() // version 4
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() // variant 10xx
    val hexChars = "0123456789abcdef"
    return buildString {
        bytes.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            append(hexChars[value ushr 4])
            append(hexChars[value and 0x0F])
            if (index == 3 || index == 5 || index == 7 || index == 9) append('-')
        }
    }
}
