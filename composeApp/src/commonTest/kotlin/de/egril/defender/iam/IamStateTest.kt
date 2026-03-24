package de.egril.defender.iam

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [IamState] ensuring default values and state transitions behave correctly.
 */
class IamStateTest {

    @Test
    fun `default state is not authenticated`() {
        val state = IamState()
        assertFalse(state.isAuthenticated)
        assertNull(state.username)
        assertNull(state.token)
    }

    @Test
    fun `authenticated state carries username and token`() {
        val state = IamState(isAuthenticated = true, username = "alice", token = "tok123")
        assertTrue(state.isAuthenticated)
        assertEquals("alice", state.username)
        assertEquals("tok123", state.token)
    }

    @Test
    fun `copy clears authentication`() {
        val authenticated = IamState(isAuthenticated = true, username = "alice", token = "tok123")
        val loggedOut = IamState()
        assertFalse(loggedOut.isAuthenticated)
        assertNull(loggedOut.username)
        assertNull(loggedOut.token)
        // Original state is unchanged
        assertTrue(authenticated.isAuthenticated)
    }

    @Test
    fun `two states with same values are equal`() {
        val s1 = IamState(isAuthenticated = true, username = "bob", token = "abc")
        val s2 = IamState(isAuthenticated = true, username = "bob", token = "abc")
        assertEquals(s1, s2)
    }

    @Test
    fun `states with different tokens are not equal`() {
        val s1 = IamState(isAuthenticated = true, username = "bob", token = "old-token")
        val s2 = s1.copy(token = "new-token")
        assertTrue(s1 != s2)
    }
}
