package de.egril.defender.iam

/**
 * Represents the current IAM authentication state.
 *
 * @param isAuthenticated Whether the user is currently logged in via Keycloak
 * @param username The Keycloak username (preferred_username claim from the JWT)
 * @param token The current access token to be sent as Bearer token in API calls
 */
data class IamState(
    val isAuthenticated: Boolean = false,
    val username: String? = null,
    val token: String? = null
)
