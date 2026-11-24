package de.egril.defender.editor

import java.io.File

/**
 * Base JVM implementation of FileStorage using java.io.File API.
 * Platform-specific implementations provide the base directory.
 */
abstract class JvmFileStorage : FileStorage {
    protected abstract val baseDir: File?

    override fun writeFile(
        path: String,
        content: String,
    ) {
        val dir = baseDir ?: return
        val file = File(dir, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    override fun readFile(path: String): String? {
        val dir = baseDir ?: return null
        val file = File(dir, path)
        return if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }

    override fun listFiles(directory: String): List<String> {
        val dir = baseDir ?: return emptyList()
        val targetDir = File(dir, directory)
        return if (targetDir.exists() && targetDir.isDirectory) {
            targetDir.listFiles()?.map { it.name } ?: emptyList()
        } else {
            emptyList()
        }
    }

    override fun fileExists(path: String): Boolean {
        val dir = baseDir ?: return false
        return File(dir, path).exists()
    }

    override fun createDirectory(path: String) {
        val dir = baseDir ?: return
        File(dir, path).mkdirs()
    }

    override fun deleteFile(path: String) {
        val dir = baseDir ?: return
        File(dir, path).delete()
    }

    override fun renameDirectory(
        oldPath: String,
        newPath: String,
    ): Boolean {
        val dir = baseDir ?: return false
        val oldDir = File(dir, oldPath)
        val newDir = File(dir, newPath)

        if (!oldDir.exists() || !oldDir.isDirectory) {
            return false
        }

        return oldDir.renameTo(newDir)
    }

    override fun copyDirectory(
        sourcePath: String,
        targetPath: String,
    ): Boolean {
        val dir = baseDir ?: return false
        val sourceDir = File(dir, sourcePath)
        val targetDir = File(dir, targetPath)

        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            return false
        }
        // Never clean up a pre-existing target: only a partial copy made by this call may be removed.
        if (targetDir.exists()) {
            return false
        }

        // The target did not exist before this call, so anything left in it after a failure is our own partial copy.
        return try {
            sourceDir.copyRecursively(targetDir, overwrite = false)
            if (fileManifest(sourceDir) == fileManifest(targetDir)) {
                true
            } else {
                println("Verification failed copying $sourcePath to $targetPath; removing partial copy")
                targetDir.deleteRecursively()
                false
            }
        } catch (e: Exception) {
            println("Error copying directory: ${e.message}")
            targetDir.deleteRecursively()
            false
        }
    }

    /** Maps each file's relative path to its size, used to verify that a copy is complete. */
    private fun fileManifest(root: File): Map<String, Long> =
        root
            .walkTopDown()
            .filter { it.isFile }
            .associate { it.relativeTo(root).invariantSeparatorsPath to it.length() }

    /**
     * Refuses to delete user or community content and repository backups. Those must only be
     * removed by explicit user action on individual files, never as a side effect of a directory delete.
     */
    override fun deleteDirectory(path: String): Boolean {
        val dir = baseDir ?: return false
        val targetDir = File(dir, path)

        if (FileStorageSafety.isProtectedDirectory(path)) {
            println("Refusing to delete protected directory: $path")
            return false
        }

        if (!targetDir.exists()) {
            return true // Already doesn't exist
        }

        return targetDir.deleteRecursively()
    }

    override fun getAbsolutePath(path: String): String {
        val dir = baseDir ?: return path
        return File(dir, path).absolutePath
    }

    override fun writeBinaryFile(
        path: String,
        content: ByteArray,
    ) {
        val dir = baseDir ?: return
        val file = File(dir, path)
        file.parentFile?.mkdirs()
        file.writeBytes(content)
    }

    override fun readBinaryFile(path: String): ByteArray? {
        val dir = baseDir ?: return null
        val file = File(dir, path)
        return if (file.exists()) {
            file.readBytes()
        } else {
            null
        }
    }
}
