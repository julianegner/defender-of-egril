# Production Keycloak Realm Configuration

This directory contains the production Keycloak realm for Defender of Egril.

## Differences from Local

| Setting | Local (`keycloak/local/`) | Production (`keycloak/production/`) |
|---------|--------------------------|--------------------------------------|
| Test user (`tester@test.org`) | ✅ present | ❌ removed |
| `sslRequired` | `none` | `external` |
| Redirect URIs | localhost dev ports | localhost wildcard + `egril://` |

## Before Going Live: Configure Production URIs

The realm JSON ships with minimal redirect URIs suitable for native-app
(desktop/mobile) logins.  Before the first deployment you **must** add your
production web-app URLs.

Edit `egril-realm.json` and update the `defender-of-egril` client:

```json
"redirectUris": [
  "http://localhost:*/*",
  "egril://callback",
  "https://your-domain.example.com/*"
],
"webOrigins": [
  "https://your-domain.example.com"
],
"attributes": {
  "pkce.code.challenge.method": "S256",
  "post.logout.redirect.uris": "http://localhost:*/*##egril://callback##+##https://your-domain.example.com/*"
}
```

Replace `https://your-domain.example.com` with the actual public URL where
the Defender of Egril web app is served.

## Realm Import Behaviour

Keycloak imports the realm **once** on first startup (when the realm does not
yet exist in the database).  Subsequent container restarts do **not**
re-import, so changes to `egril-realm.json` are not applied automatically to
an already-running instance.

To apply client or realm changes without losing user data, the
`deploy-keycloak` GitHub Actions workflow includes a post-deploy step that
calls the Keycloak Admin REST API directly.  This step is idempotent and runs
on every deployment.  Add similar API calls there whenever `egril-realm.json`
is updated with settings that must be pushed to the live instance (e.g.
`consentRequired`, redirect URIs, protocol mappers).

To do a full re-import (destructive – wipes all users and sessions), drop the
`keycloak` database on the DB server and restart the Keycloak container.
