package de.egril.defender.editor

/**
 * Shared safety rules for all [FileStorage] implementations.
 *
 * Directory deletes must never remove user or community content or repository backups
 * (`gamedata-N`). Those are only removed by explicit per-file user action.
 */
internal object FileStorageSafety {
    private val PROTECTED_DIR_NAMES = setOf("user", "community")
    private const val BACKUP_DIR_PREFIX = "gamedata-"

    fun isProtectedDirectory(path: String): Boolean {
        val segments = path.split('/', '\\').filter { it.isNotEmpty() }
        return segments.any { it.startsWith(BACKUP_DIR_PREFIX) } ||
            segments.zipWithNext().any { (parent, child) -> parent == "gamedata" && child in PROTECTED_DIR_NAMES }
    }
}
