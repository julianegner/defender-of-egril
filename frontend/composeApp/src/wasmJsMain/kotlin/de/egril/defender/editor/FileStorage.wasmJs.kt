@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.editor

import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlin.js.Promise

// ─── JS helpers ───────────────────────────────────────────────────────────────

/**
 * Returns true if the browser supports the Origin Private File System API.
 */
@JsFun("() => typeof navigator !== 'undefined' && 'storage' in navigator && typeof navigator.storage.getDirectory === 'function'")
private external fun jsIsOpfsSupported(): Boolean

/**
 * Enumerates every file path under [appDir] in OPFS depth-first.
 * Resolves to a newline-separated list of relative paths, or an empty string
 * if the directory does not yet exist.
 */
@JsFun("""
(appDir) => (async () => {
  const paths = [];
  async function walk(dir, prefix) {
    for await (const [name, handle] of dir.entries()) {
      if (handle.kind === 'file') paths.push(prefix + name);
      else await walk(handle, prefix + name + '/');
    }
  }
  try {
    const root = await navigator.storage.getDirectory();
    const d = await root.getDirectoryHandle(appDir);
    await walk(d, '');
  } catch(_) {}
  return paths.join('\n');
})()
""")
private external fun jsOpfsListAll(appDir: String): Promise<JsAny?>

/**
 * Reads [path] (relative to [appDir]) as UTF-8 text.
 * Resolves to the file content, or null if the file does not exist.
 */
@JsFun("""
(appDir, path) => (async () => {
  try {
    const parts = (appDir + '/' + path).split('/');
    const root = await navigator.storage.getDirectory();
    let d = root;
    for (let i = 0; i < parts.length - 1; i++) d = await d.getDirectoryHandle(parts[i]);
    const file = await (await d.getFileHandle(parts[parts.length - 1])).getFile();
    return await file.text();
  } catch(_) { return null; }
})()
""")
private external fun jsOpfsReadOne(appDir: String, path: String): Promise<JsAny?>

/**
 * Writes (or overwrites) [path] with the given UTF-8 [content].
 * Intermediate directories are created automatically.
 */
@JsFun("""
(appDir, path, content) => (async () => {
  const parts = (appDir + '/' + path).split('/');
  const root = await navigator.storage.getDirectory();
  let d = root;
  for (let i = 0; i < parts.length - 1; i++)
    d = await d.getDirectoryHandle(parts[i], { create: true });
  const fh = await d.getFileHandle(parts[parts.length - 1], { create: true });
  const w = await fh.createWritable();
  await w.write(content);
  await w.close();
})()
""")
private external fun jsOpfsWriteOne(appDir: String, path: String, content: String): Promise<JsAny?>

/**
 * Deletes the file at [path]. Silently ignores missing files.
 */
@JsFun("""
(appDir, path) => (async () => {
  try {
    const parts = (appDir + '/' + path).split('/');
    const root = await navigator.storage.getDirectory();
    let d = root;
    for (let i = 0; i < parts.length - 1; i++) d = await d.getDirectoryHandle(parts[i]);
    await d.removeEntry(parts[parts.length - 1]);
  } catch(_) {}
})()
""")
private external fun jsOpfsDeleteOne(appDir: String, path: String): Promise<JsAny?>

/**
 * Recursively deletes [dirPath]. Silently ignores missing directories.
 */
@JsFun("""
(appDir, dirPath) => (async () => {
  try {
    const parts = (appDir + '/' + dirPath).split('/');
    const root = await navigator.storage.getDirectory();
    let d = root;
    for (let i = 0; i < parts.length - 1; i++) d = await d.getDirectoryHandle(parts[i]);
    await d.removeEntry(parts[parts.length - 1], { recursive: true });
  } catch(_) {}
})()
""")
private external fun jsOpfsDeleteDirRecursive(appDir: String, dirPath: String): Promise<JsAny?>

// ─── OPFSFileStorage ──────────────────────────────────────────────────────────

/**
 * WASM/browser [FileStorage] backed by the Origin Private File System (OPFS).
 *
 * All synchronous [FileStorage] methods operate on an in-memory cache so the
 * synchronous interface contract is preserved. Writes are reflected in the cache
 * immediately and then persisted to OPFS asynchronously in the background.
 *
 * [initializeAsync] must be called once (by [de.egril.defender.editor.EditorStorage])
 * before the first storage operation so that the in-memory cache is pre-populated
 * from OPFS. On the very first launch (empty OPFS), existing localStorage entries
 * under the "defender-of-egril:" prefix are migrated automatically and the
 * localStorage copies are left in place for backwards compatibility.
 *
 * When OPFS is not available (e.g. some embedded browsers) the implementation
 * transparently falls back to localStorage.
 *
 * Binary files are stored in the same base64-encoded format used by the old
 * localStorage implementation ("base64:…" prefix) to allow seamless migration.
 */
class OPFSFileStorage : FileStorage {

    private companion object {
        private const val OPFS_APP_DIR = "defender-of-egril"
        private const val LS_PREFIX = "defender-of-egril:"

        /**
         * Background coroutine scope for fire-and-forget OPFS writes.
         *
         * [fileStorageInstance] is a per-page-load singleton so the scope exists for the
         * entire lifetime of the page and is reclaimed automatically when the page unloads.
         */
        private val writeScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    // ── in-memory cache ──────────────────────────────────────────────────────

    /** Plain-text files (JSON, version files, etc.). */
    private val textCache = mutableMapOf<String, String>()

    /**
     * Binary files stored as a base64-encoded string with the "base64:" prefix
     * (same format used by the legacy localStorage implementation).
     */
    private val binaryCache = mutableMapOf<String, String>()

    private var cacheInitialized = false

    // ── async initialisation ─────────────────────────────────────────────────

    /**
     * Pre-populates the in-memory cache from OPFS.
     *
     * If OPFS is empty on first launch, existing localStorage entries are
     * migrated into OPFS so users do not lose their saved games.
     *
     * This method is idempotent: subsequent calls are no-ops.
     */
    override suspend fun initializeAsync() {
        if (cacheInitialized) return
        cacheInitialized = true

        if (!jsIsOpfsSupported()) {
            // OPFS not available – fall back to localStorage for reads.
            loadFromLocalStorage()
            return
        }

        try {
            val raw = jsOpfsListAll(OPFS_APP_DIR).await<JsAny?>()?.toString() ?: ""
            val opfsPaths = if (raw.isBlank()) emptyList() else raw.split('\n')

            if (opfsPaths.isEmpty()) {
                // First launch with OPFS: migrate any existing localStorage data.
                migrateFromLocalStorage()
            } else {
                // Subsequent launches: read every file into the in-memory cache.
                for (path in opfsPaths) {
                    val content = try {
                        jsOpfsReadOne(OPFS_APP_DIR, path).await<JsAny?>()?.toString()
                    } catch (_: Throwable) { null }
                    if (content == null) continue
                    if (content.startsWith("base64:")) {
                        binaryCache[path] = content
                    } else {
                        textCache[path] = content
                    }
                }
                // Clean up any leftover localStorage entries from a prior migration run
                // that retained the copies (older app version). This frees quota for
                // app settings which still use localStorage.
                cleanUpLocalStorageLegacyEntries()
            }
        } catch (e: Throwable) {
            println("OPFSFileStorage: initializeAsync error – ${e.message}")
        }
    }

    /** Reads all "defender-of-egril:" localStorage keys into the in-memory cache. */
    private fun loadFromLocalStorage() {
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i) ?: continue
            if (!key.startsWith(LS_PREFIX)) continue
            val value = localStorage.getItem(key) ?: continue
            val path = key.removePrefix(LS_PREFIX)
            if (value.startsWith("base64:")) {
                binaryCache[path] = value
            } else {
                textCache[path] = value
            }
        }
    }

    /**
     * One-time migration: copies every "defender-of-egril:" localStorage entry
     * into OPFS and the in-memory cache, then removes the localStorage copies to
     * free quota for app settings.
     */
    private suspend fun migrateFromLocalStorage() {
        val entries = mutableListOf<Pair<String, String>>()
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i) ?: continue
            if (!key.startsWith(LS_PREFIX)) continue
            val value = localStorage.getItem(key) ?: continue
            entries.add(key.removePrefix(LS_PREFIX) to value)
        }
        for ((path, content) in entries) {
            try {
                jsOpfsWriteOne(OPFS_APP_DIR, path, content).await<JsAny?>()
                // Remove from localStorage after a successful OPFS write to free quota.
                localStorage.removeItem(LS_PREFIX + path)
            } catch (e: Throwable) {
                println("OPFSFileStorage: migration failed for $path – ${e.message}")
            }
            if (content.startsWith("base64:")) {
                binaryCache[path] = content
            } else {
                textCache[path] = content
            }
        }
        if (entries.isNotEmpty()) {
            println("OPFSFileStorage: migrated ${entries.size} entries from localStorage to OPFS")
        }
    }

    /**
     * Removes any leftover "defender-of-egril:" localStorage entries that were not
     * cleaned up during a previous migration run (e.g. when migrating with an older
     * version of the app that retained the copies).
     */
    private fun cleanUpLocalStorageLegacyEntries() {
        val keys = (0 until localStorage.length)
            .mapNotNull { localStorage.key(it) }
            .filter { it.startsWith(LS_PREFIX) }
        keys.forEach { localStorage.removeItem(it) }
        if (keys.isNotEmpty()) {
            println("OPFSFileStorage: removed ${keys.size} legacy localStorage entries to free quota")
        }
    }

    // ── background persistence helpers ────────────────────────────────────────

    private fun persistAsync(path: String, content: String) {
        if (!jsIsOpfsSupported()) {
            try { localStorage.setItem(LS_PREFIX + path, content) } catch (_: Throwable) {}
            return
        }
        writeScope.launch {
            try {
                jsOpfsWriteOne(OPFS_APP_DIR, path, content).await<JsAny?>()
            } catch (e: Throwable) {
                println("OPFSFileStorage: write failed for $path – ${e.message}")
            }
        }
    }

    private fun deleteFileAsync(path: String) {
        if (!jsIsOpfsSupported()) {
            localStorage.removeItem(LS_PREFIX + path)
            return
        }
        writeScope.launch {
            try { jsOpfsDeleteOne(OPFS_APP_DIR, path).await<JsAny?>() } catch (_: Throwable) {}
        }
    }

    private fun deleteDirAsync(dirPath: String) {
        if (!jsIsOpfsSupported()) {
            val prefix = LS_PREFIX + dirPath + "/"
            val toRemove = (0 until localStorage.length)
                .mapNotNull { localStorage.key(it) }
                .filter { it.startsWith(prefix) }
            toRemove.forEach { localStorage.removeItem(it) }
            return
        }
        writeScope.launch {
            try { jsOpfsDeleteDirRecursive(OPFS_APP_DIR, dirPath).await<JsAny?>() } catch (_: Throwable) {}
        }
    }

    // ── FileStorage interface ─────────────────────────────────────────────────

    override fun writeFile(path: String, content: String) {
        textCache[path] = content
        binaryCache.remove(path)
        persistAsync(path, content)
    }

    override fun readFile(path: String): String? = textCache[path]

    override fun writeBinaryFile(path: String, content: ByteArray) {
        val builder = StringBuilder(content.size)
        content.forEach { builder.append((it.toInt() and 0xFF).toChar()) }
        val encoded = "base64:" + window.btoa(builder.toString())
        binaryCache[path] = encoded
        textCache.remove(path)
        persistAsync(path, encoded)
    }

    override fun readBinaryFile(path: String): ByteArray? {
        val stored = binaryCache[path] ?: return null
        if (!stored.startsWith("base64:")) return null
        val binary = window.atob(stored.removePrefix("base64:"))
        return ByteArray(binary.length) { idx -> binary[idx].code.toByte() }
    }

    override fun listFiles(directory: String): List<String> {
        val prefix = "$directory/"
        return (textCache.keys.asSequence() + binaryCache.keys.asSequence())
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
            .filter { !it.contains('/') }
            .distinct()
            .toList()
    }

    override fun fileExists(path: String): Boolean {
        if (textCache.containsKey(path) || binaryCache.containsKey(path)) return true
        val dirPrefix = "$path/"
        return textCache.keys.any { it.startsWith(dirPrefix) } ||
               binaryCache.keys.any { it.startsWith(dirPrefix) }
    }

    override fun createDirectory(path: String) {
        // No-op – directories are virtual (derived from file paths).
    }

    override fun deleteFile(path: String) {
        textCache.remove(path)
        binaryCache.remove(path)
        deleteFileAsync(path)
    }

    override fun renameDirectory(oldPath: String, newPath: String): Boolean {
        val oldPrefix = "$oldPath/"
        val newPrefix = "$newPath/"
        val allKeys = (textCache.keys.toList() + binaryCache.keys.toList()).distinct()
        val affected = allKeys.filter { it.startsWith(oldPrefix) }
        if (affected.isEmpty()) return false
        for (old in affected) {
            val new = old.replaceFirst(oldPrefix, newPrefix)
            val text = textCache.remove(old)
            val binary = binaryCache.remove(old)
            if (text != null) { textCache[new] = text; persistAsync(new, text) }
            if (binary != null) { binaryCache[new] = binary; persistAsync(new, binary) }
            deleteFileAsync(old)
        }
        return true
    }

    override fun copyDirectory(sourcePath: String, targetPath: String): Boolean {
        val sourcePrefix = "$sourcePath/"
        val targetPrefix = "$targetPath/"
        var copied = false
        val allKeys = (textCache.keys.toList() + binaryCache.keys.toList()).distinct()
        for (src in allKeys.filter { it.startsWith(sourcePrefix) }) {
            val dst = src.replaceFirst(sourcePrefix, targetPrefix)
            val text = textCache[src]
            val binary = binaryCache[src]
            if (text != null) { textCache[dst] = text; persistAsync(dst, text); copied = true }
            if (binary != null) { binaryCache[dst] = binary; persistAsync(dst, binary); copied = true }
        }
        return copied
    }

    override fun deleteDirectory(path: String): Boolean {
        val prefix = "$path/"
        val allKeys = (textCache.keys.toList() + binaryCache.keys.toList())
            .filter { it.startsWith(prefix) }
        allKeys.forEach { textCache.remove(it); binaryCache.remove(it) }
        deleteDirAsync(path)
        return true
    }

    override fun getAbsolutePath(path: String): String = "OPFS: $OPFS_APP_DIR/$path"
}

private val fileStorageInstance: FileStorage = OPFSFileStorage()

actual fun getFileStorage(): FileStorage = fileStorageInstance
