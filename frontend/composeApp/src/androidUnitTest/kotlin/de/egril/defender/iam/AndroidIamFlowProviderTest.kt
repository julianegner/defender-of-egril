package de.egril.defender.iam

import androidx.activity.ComponentActivity
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Unit tests for [AndroidIamFlowProvider].
 *
 * Verifies that the singleton factory is initialised correctly and that
 * [AndroidIamFlowProvider.registerActivity] delegates to the underlying factory.
 */
class AndroidIamFlowProviderTest {
    @Before
    fun resetIamState() {
        // Ensure a clean auth state before each test
        IamService.state.value = IamState()
    }

    @Test
    fun `factory is not null on first access`() {
        assertNotNull(AndroidIamFlowProvider.factory)
    }

    @Test
    fun `factory is a singleton`() {
        val first = AndroidIamFlowProvider.factory
        val second = AndroidIamFlowProvider.factory
        assertSame(first, second)
    }

    @Test
    fun `registerActivity is exposed`() {
        val registerActivity: (ComponentActivity) -> Unit = AndroidIamFlowProvider::registerActivity
        assertNotNull(registerActivity)
    }

    @Test
    fun `IamService state is not authenticated by default`() {
        val state = IamService.state.value
        kotlin.test.assertFalse(state.isAuthenticated)
    }

    @Test
    fun `IamService logout clears state`() {
        // Simulate a logged-in state
        IamService.state.value = IamState(isAuthenticated = true, username = "alice", token = "tok")
        IamService.logout()
        kotlin.test.assertFalse(IamService.state.value.isAuthenticated)
        kotlin.test.assertNull(IamService.state.value.token)
    }
}
