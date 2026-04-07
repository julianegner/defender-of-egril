package de.egril.defender.iam

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [IamConfig] constants and URL construction.
 * These tests verify that the OIDC client is configured with the correct values
 * without requiring a running Keycloak instance.
 */
class IamConfigTest {

    @Test
    fun `realm is egril`() {
        assertEquals("egril", IamConfig.REALM)
    }

    @Test
    fun `client id is defender-of-egril`() {
        assertEquals("defender-of-egril", IamConfig.CLIENT_ID)
    }

    @Test
    fun `auth url contains realm and openid-connect path`() {
        val url = IamConfig.authUrl
        assertTrue(
            url.contains("/realms/${IamConfig.REALM}/protocol/openid-connect/auth"),
            "authUrl should contain the expected OIDC authorization path, but was: $url"
        )
    }

    @Test
    fun `token url contains realm and openid-connect path`() {
        val url = IamConfig.tokenUrl
        assertTrue(
            url.contains("/realms/${IamConfig.REALM}/protocol/openid-connect/token"),
            "tokenUrl should contain the expected OIDC token path, but was: $url"
        )
    }

    @Test
    fun `logout url contains realm and openid-connect path`() {
        val url = IamConfig.logoutUrl
        assertTrue(
            url.contains("/realms/${IamConfig.REALM}/protocol/openid-connect/logout"),
            "logoutUrl should contain the expected OIDC logout path, but was: $url"
        )
    }
}
