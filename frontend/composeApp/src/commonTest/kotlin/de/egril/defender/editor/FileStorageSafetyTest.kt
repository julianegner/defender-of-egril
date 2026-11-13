package de.egril.defender.editor

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileStorageSafetyTest {
    @Test
    fun protectsUserCommunityAndBackupDirectories() {
        assertTrue(FileStorageSafety.isProtectedDirectory("gamedata/user"))
        assertTrue(FileStorageSafety.isProtectedDirectory("gamedata/user/maps"))
        assertTrue(FileStorageSafety.isProtectedDirectory("gamedata/community"))
        assertTrue(FileStorageSafety.isProtectedDirectory("gamedata-1"))
        assertTrue(FileStorageSafety.isProtectedDirectory("gamedata-1/user/levels"))
    }

    @Test
    fun allowsOrdinaryDirectoriesIncludingPlayerUserFolders() {
        assertFalse(FileStorageSafety.isProtectedDirectory("players/user"))
        assertFalse(FileStorageSafety.isProtectedDirectory("players/abc"))
        assertFalse(FileStorageSafety.isProtectedDirectory("gamedata/tmp"))
    }
}
