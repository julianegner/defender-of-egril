package de.egril.defender.iam

import de.egril.defender.save.PlayerProfile
import de.egril.defender.save.PlayerProfiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests verifying that the guard logic in [GameViewModel.onAuthStateChanged] prevents
 * a stale Keycloak session from silently linking the wrong remote account to a local
 * player profile.
 *
 * Scenario (the bug this guard fixes):
 *  1. PlayerA is authenticated as remotePlayerA.
 *  2. PlayerA logs out locally (Keycloak SSO browser session may still be active).
 *  3. PlayerB logs in locally and tries to connect a remote account.
 *  4. The Keycloak SSO cookie silently authenticates as remotePlayerA again.
 *  5. Without the guard, PlayerB would be incorrectly linked to remotePlayerA.
 *
 * The guard checks: if the authenticated [remoteUsername] is already linked to a
 * *different* local player profile, it is a stale session → terminate it, skip linking.
 */
class RemoteAccountConnectionGuardTest {

    private fun makeProfile(id: String, remoteUsername: String? = null) = PlayerProfile(
        id = id,
        name = id,
        createdAt = 0L,
        lastPlayedAt = 0L,
        remoteUsername = remoteUsername
    )

    /** Simulates the guard logic extracted from [GameViewModel.onAuthStateChanged]. */
    private fun isStaleSession(
        profiles: PlayerProfiles,
        currentPlayerId: String,
        authenticatedUsername: String
    ): Boolean {
        val existingOwner = profiles.profiles.find { it.remoteUsername == authenticatedUsername }
        return existingOwner != null && existingOwner.id != currentPlayerId
    }

    @Test
    fun `no stale session when no profile is linked to the remote username`() {
        val profiles = PlayerProfiles(
            profiles = listOf(
                makeProfile("player_a", remoteUsername = "remoteA"),
                makeProfile("player_b", remoteUsername = null)
            ),
            lastUsedPlayerId = "player_b"
        )
        // PlayerB is current player, authenticated as "remoteB" (never linked before)
        assertFalse(isStaleSession(profiles, "player_b", "remoteB"))
    }

    @Test
    fun `stale session detected when remote username belongs to a different local player`() {
        val profiles = PlayerProfiles(
            profiles = listOf(
                makeProfile("player_a", remoteUsername = "remoteA"),
                makeProfile("player_b", remoteUsername = null)
            ),
            lastUsedPlayerId = "player_b"
        )
        // PlayerB is current player, but SSO returned "remoteA" which belongs to PlayerA
        assertTrue(isStaleSession(profiles, "player_b", "remoteA"))
    }

    @Test
    fun `no stale session when the current player is already linked to the same remote username`() {
        val profiles = PlayerProfiles(
            profiles = listOf(
                makeProfile("player_a", remoteUsername = "remoteA")
            ),
            lastUsedPlayerId = "player_a"
        )
        // PlayerA is current player and is already linked to "remoteA" – not a stale session
        assertFalse(isStaleSession(profiles, "player_a", "remoteA"))
    }

    @Test
    fun `no stale session with single player and no linked account`() {
        val profiles = PlayerProfiles(
            profiles = listOf(makeProfile("player_a")),
            lastUsedPlayerId = "player_a"
        )
        assertFalse(isStaleSession(profiles, "player_a", "remoteA"))
    }

    @Test
    fun `stale session detected in multi-player setup`() {
        val profiles = PlayerProfiles(
            profiles = listOf(
                makeProfile("alice", remoteUsername = "alice_remote"),
                makeProfile("bob", remoteUsername = "bob_remote"),
                makeProfile("charlie", remoteUsername = null)
            ),
            lastUsedPlayerId = "charlie"
        )
        // Charlie is the current player, but the SSO session is still alive as alice_remote
        assertTrue(isStaleSession(profiles, "charlie", "alice_remote"))
        // …or as bob_remote
        assertTrue(isStaleSession(profiles, "charlie", "bob_remote"))
        // A completely fresh remote account would be fine to link
        assertFalse(isStaleSession(profiles, "charlie", "charlie_remote"))
    }

    @Test
    fun `findByRemoteUsername returns correct profile`() {
        val profileA = makeProfile("player_a", remoteUsername = "remoteA")
        val profileB = makeProfile("player_b", remoteUsername = null)
        val profiles = listOf(profileA, profileB)

        val found = profiles.find { it.remoteUsername == "remoteA" }
        assertNotNull(found)
        assertEquals("player_a", found.id)
    }

    @Test
    fun `findByRemoteUsername returns null when no profile matches`() {
        val profiles = listOf(
            makeProfile("player_a", remoteUsername = "remoteA"),
            makeProfile("player_b", remoteUsername = null)
        )
        val found = profiles.find { it.remoteUsername == "remoteB" }
        assertNull(found)
    }
}
