terraform {
  required_providers {
    keycloak = {
      source  = "mrparkers/keycloak"
      version = "~> 4.4"
    }
  }
  required_version = ">= 1.3"
}

provider "keycloak" {
  client_id = "admin-cli"
  username  = var.keycloak_admin_user
  password  = var.keycloak_admin_password
  url       = var.keycloak_url
}

resource "keycloak_realm" "egril" {
  realm   = "egril"
  enabled = true

  display_name      = "Egril"
  display_name_html = "<b>Egril</b>"

  login_theme = "keycloak"

  registration_allowed           = true
  registration_email_as_username = false
  remember_me                    = true
  reset_password_allowed         = true
  edit_username_allowed          = true

  access_token_lifespan = "5m"

  # NOTE: ssl_required = "none" is only safe for local development.
  # Set to "all" or "external" for any non-local deployment.
  ssl_required = "none"
}

resource "keycloak_openid_client" "defender_of_egril" {
  realm_id  = keycloak_realm.egril.id
  client_id = "defender-of-egril"

  name    = "Defender of Egril"
  enabled = true

  access_type           = "PUBLIC"
  standard_flow_enabled = true

  # The wildcard localhost port pattern is required because the Desktop OAuth2 PKCE
  # flow picks a random free port for the local callback server. For production
  # deployments, restrict to the actual application port(s) only.
  valid_redirect_uris = [
    "http://localhost:8080/*",
    "http://localhost:8888/*",
    "http://localhost:*/*",
  ]

  valid_post_logout_redirect_uris = [
    "http://localhost:8080/*",
    "http://localhost:8888/*",
    "http://localhost:*/*",
    "+",
  ]

  web_origins = [
    "http://localhost:8080",
    "http://localhost:8888",
    "+",
  ]

  pkce_code_challenge_method = "S256"
}
