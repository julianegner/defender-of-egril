# Egril Keycloak Theme

Custom Keycloak login theme for Defender of Egril, built with [Keycloakify](https://www.keycloakify.dev/) v11.

## Features

- **Defender of Egril banner** – shows the game's shield logo, character icons, and title on every Keycloak page.
- **cosha.nu banner** – shows the cosha.nu brand with its coloured geometric shapes.
- **Dark / Light mode** – reads the `KEYCLOAKIFY_DARK_MODE` cookie set by the game app before redirecting to Keycloak:
  - Cookie value `dark` → dark gray background (`#2a2a2a`)
  - Cookie value `light` → very light gray background (`#f5f5f5`)
  - No cookie → falls back to OS / browser color-scheme preference.
- **Locale support** – Keycloak receives the app's selected language via the `ui_locales` URL parameter (desktop) or `locale` login option (web/WASM).

## Requirements

- Node.js 18+
- npm 9+

## Development

```bash
# Install dependencies
npm install

# Start Vite dev server (theme preview)
npm run dev
```

> **Note:** When running with `npm run dev`, the app throws an error because
> `window.kcContext` is not injected. To preview individual pages during
> development, use the [Keycloakify Storybook integration](https://docs.keycloakify.dev/testing-your-theme/in-storybook).

## Building

```bash
npm install
npm run build
```

This produces:
- A Vite bundle in `dist/`
- A Keycloak theme JAR in `dist_keycloak/` (ready to deploy)

## Deploying to Local Keycloak

The `keycloak/local/start.sh` script builds the theme JAR (if absent) and starts Keycloak in
one step:

```bash
./keycloak/local/start.sh        # from the repo root, foreground
./keycloak/local/start.sh -d     # detached (background)
```

Or build manually and use docker compose directly:

```bash
# 1. Build the theme
cd keycloak/theme
npm install && npm run build

# 2. Start / restart Keycloak
cd ..
docker compose -f keycloak/local/docker-compose.yml up -d --force-recreate
```

> **Note:** `dist_keycloak/` is git-ignored; the JARs must be built locally
> before starting Keycloak.

After Keycloak starts, the `egril` realm is automatically configured to use
the `egril` login theme.

## Dark Mode Integration

The Defender of Egril web app sets the `KEYCLOAKIFY_DARK_MODE` cookie before
redirecting to Keycloak for login:

```javascript
// Set in IamService.wasmJs.kt via jsKcLoginWithOptions()
document.cookie = "KEYCLOAKIFY_DARK_MODE=" + (isDark ? "dark" : "light") + "; path=/; SameSite=Strict";
```

Because cookies are scoped to the domain (not the port), the cookie set on
`localhost:8080` is automatically readable by the Keycloak page on
`localhost:8081`.

For the desktop app, the dark mode preference is passed via the
`kc_dark=true` URL parameter as a fallback, and the theme reads it from
`window.location.search`.

## Locale Integration

The selected language is forwarded to Keycloak in two ways:

| Platform | Mechanism |
|----------|-----------|
| Desktop  | `&ui_locales=de` appended to the OIDC authorization URL |
| Web/WASM | `locale: 'de'` passed to `keycloak.login()` options |

Keycloak must have `internationalizationEnabled: true` and the locale listed
in `supportedLocales` for this to take effect (already configured in
`egril-realm.json`).
