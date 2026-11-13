package de.egril.defender.editor

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the real restore flow of [RepositoryManager] against a temporary directory,
 * so that the user's actual game data is never touched.
 */
class RepositoryManagerRestoreTest {
    private lateinit var root: File
    private lateinit var originalStorage: FileStorage

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("repo-manager-restore").toFile()
        originalStorage = RepositoryManager.fileStorage
    }

    @AfterTest
    fun tearDown() {
        RepositoryManager.fileStorage = originalStorage
        root.deleteRecursively()
    }

    private fun plainStorage(): JvmFileStorage {
        val base = root
        return object : JvmFileStorage() {
            override val baseDir: File? = base
        }
    }

    private fun writeFixture(path: String, content: String) {
        File(root, path).apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    private fun seedUserData() {
        writeFixture("gamedata/user/maps/my_map.json", "{\"id\":\"my_map\"}")
        writeFixture("gamedata/user/levels/my_level.json", "{\"id\":\"my_level\"}")
        writeFixture("gamedata/community/maps/community_map.json", "{\"id\":\"community_map\"}")
    }

    @Test
    fun restorePreservesUserAndCommunityDataAndKeepsBackup() {
        seedUserData()
        RepositoryManager.fileStorage = plainStorage()

        val backupPath = runBlocking { RepositoryManager.restoreFromRepository() }

        assertNotNull(backupPath, "restore should succeed with a repository available")
        assertTrue(File(root, "gamedata-1/user/maps/my_map.json").exists(), "backup must keep user data")
        assertTrue(File(root, "gamedata/user/maps/my_map.json").exists(), "user map must be copied back")
        assertTrue(File(root, "gamedata/user/levels/my_level.json").exists(), "user level must be copied back")
        assertTrue(
            File(root, "gamedata/community/maps/community_map.json").exists(),
            "community map must be copied back",
        )
    }

    @Test
    fun failedUserDataCopyRollsBackAndKeepsOriginalData() {
        seedUserData()
        RepositoryManager.fileStorage =
            object : JvmFileStorage() {
                override val baseDir: File? = root

                override fun copyDirectory(sourcePath: String, targetPath: String): Boolean {
                    if (sourcePath.endsWith("/user")) return false
                    return super.copyDirectory(sourcePath, targetPath)
                }
            }

        val result = runBlocking { RepositoryManager.restoreFromRepository() }

        assertNull(result, "restore must report failure when user data cannot be copied")
        assertTrue(File(root, "gamedata/user/maps/my_map.json").exists(), "original user data must be restored")
        assertTrue(File(root, "gamedata/user/levels/my_level.json").exists(), "original level must be restored")
        assertFalse(File(root, "gamedata-1").exists(), "backup must be renamed back, not left behind")
        assertEquals(
            "{\"id\":\"my_map\"}",
            File(root, "gamedata/user/maps/my_map.json").readText(),
            "original content must be unchanged",
        )
    }
}
