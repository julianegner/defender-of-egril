package de.egril.defender.utils

import java.io.File

// Stored in the app's own data directory (same base directory used for saves and
// editor data) instead of java.util.prefs. The JVM Preferences API on Linux relies on a
// shared, lock-protected backing file under the OS user's home directory; if that lock
// cannot be acquired (e.g. quick successive launches, sandboxed/packaged environments,
// read-only home directories) it silently falls back to an in-memory store, so nothing
// gets persisted and a new install id is generated on every launch. A plain file avoids
// that failure mode.
private val installIdFile: File by lazy {
    val baseDir = File(System.getProperty("user.home"), ".defender-of-egril")
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }
    File(baseDir, "install_id")
}

actual fun readPersistedInstallUuid(): String? =
    runCatching {
        installIdFile.takeIf { it.exists() }?.readText()?.trim()
    }.getOrNull()?.ifBlank { null }

actual fun persistInstallUuid(uuid: String) {
    runCatching { installIdFile.writeText(uuid) }
}
