package de.egril.defender.editor

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the safeguards that protect user and community data and repository backups from deletion
 * and ensure copies are complete.
 */
class JvmFileStorageSafetyTest {
    private lateinit var root: File
    private lateinit var storage: JvmFileStorage

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("jvm-storage-safety").toFile()
        val base = root
        storage =
            object : JvmFileStorage() {
                override val baseDir: File? = base
            }
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun writeFixture(path: String, content: String) {
        File(root, path).apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    @Test
    fun deleteDirectoryRefusesUserAndCommunityInsideGamedata() {
        writeFixture("gamedata/user/maps/mine.json", "{}")
        writeFixture("gamedata/community/x/level.json", "{}")

        assertFalse(storage.deleteDirectory("gamedata/user"))
        assertFalse(storage.deleteDirectory("gamedata/community"))

        assertTrue(File(root, "gamedata/user/maps/mine.json").exists())
        assertTrue(File(root, "gamedata/community/x/level.json").exists())
    }

    @Test
    fun deleteDirectoryRefusesRepositoryBackups() {
        writeFixture("gamedata-1/user/maps/mine.json", "{}")

        assertFalse(storage.deleteDirectory("gamedata-1"))
        assertTrue(File(root, "gamedata-1/user/maps/mine.json").exists())
    }

    @Test
    fun deleteDirectoryStillDeletesOrdinaryDirectories() {
        writeFixture("gamedata/maps/official.json", "{}")

        assertTrue(storage.deleteDirectory("gamedata"))
        assertFalse(File(root, "gamedata").exists())
    }

    @Test
    fun deleteDirectoryAllowsPlayerIdsNamedLikeProtectedFolders() {
        writeFixture("players/user/profile.json", "{}")

        assertTrue(storage.deleteDirectory("players/user"))
        assertFalse(File(root, "players/user").exists())
    }

    @Test
    fun copyDirectoryCopiesAllFilesAndNestedContent() {
        writeFixture("backup/user/maps/a.json", "alpha")
        writeFixture("backup/user/levels/b.json", "beta-content")

        assertTrue(storage.copyDirectory("backup/user", "target/user"))

        assertEquals("alpha", File(root, "target/user/maps/a.json").readText())
        assertEquals("beta-content", File(root, "target/user/levels/b.json").readText())
    }

    @Test
    fun copyDirectoryFailsWhenTargetAlreadyExists() {
        writeFixture("backup/user/maps/a.json", "alpha")
        writeFixture("target/user/existing.json", "keep")

        assertFalse(storage.copyDirectory("backup/user", "target/user"))
        assertTrue(File(root, "target/user/existing.json").exists())
    }
}
