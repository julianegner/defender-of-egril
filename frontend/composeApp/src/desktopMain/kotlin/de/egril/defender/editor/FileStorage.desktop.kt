package de.egril.defender.editor

import java.io.File

class DesktopFileStorage : JvmFileStorage() {
    override val baseDir = File(System.getProperty("user.home"), ".defender-of-egril").also {
        // Ensure base directory exists
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

actual fun getFileStorage(): FileStorage = DesktopFileStorage()
