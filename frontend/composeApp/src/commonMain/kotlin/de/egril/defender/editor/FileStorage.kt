package de.egril.defender.editor

/**
 * Platform-specific file storage interface
 */
interface FileStorage {
    fun writeFile(
        path: String,
        content: String,
    )

    fun readFile(path: String): String?

    fun listFiles(directory: String): List<String>

    fun fileExists(path: String): Boolean

    fun createDirectory(path: String)

    fun deleteFile(path: String)

    fun renameDirectory(
        oldPath: String,
        newPath: String,
    ): Boolean

    fun copyDirectory(
        sourcePath: String,
        targetPath: String,
    ): Boolean

    fun deleteDirectory(path: String): Boolean

    fun getAbsolutePath(path: String): String

    fun writeBinaryFile(
        path: String,
        content: ByteArray,
    )

    fun readBinaryFile(path: String): ByteArray?

    /**
     * Optional async initialisation step. Called once before the first storage
     * operation when the platform needs to pre-populate an in-memory cache from an
     * asynchronous backing store (e.g. OPFS on WASM/browser). The default
     * implementation is a no-op so existing non-WASM implementations require no
     * changes.
     */
    suspend fun initializeAsync() {}
}

expect fun getFileStorage(): FileStorage
