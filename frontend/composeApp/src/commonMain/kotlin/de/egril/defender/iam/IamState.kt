package de.egril.defender.iam

/**
 * Represents the current IAM authentication state.
 *
 * @param isAuthenticated Whether the user is currently logged in via Keycloak
 * @param username The Keycloak username (preferred_username claim from the JWT)
 * @param token The current access token to be sent as Bearer token in API calls
 * @param email The user's email address from the JWT (email claim), if present
 * @param firstName The user's first name from the JWT (given_name claim), if present
 * @param lastName The user's last name from the JWT (family_name claim), if present
 */
data class IamState(
    val isAuthenticated: Boolean = false,
    val username: String? = null,
    val token: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)
