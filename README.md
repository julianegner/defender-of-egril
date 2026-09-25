# Defender of Egril

Defender of Egril is a turn-based tower defense game built with Kotlin Multiplatform and Compose Multiplatform. You defend Egril by placing towers, upgrading them, and surviving enemy waves across a campaign map.

## Features

- Campaign progression with a world map and multiple levels
- Hex-grid battlefield with tower placement, upgrades, and manual targeting
- Multiple tower and enemy types with distinct abilities
- Save/load support, settings, and local profile switching
- Runs on desktop, Android, iOS, and Web/WASM

### Supported Platforms

- ✅ **JVM/Desktop**: Fully implemented and tested on Linux
- ✅ **Android**: Fully implemented and tested (APK builds successfully)
- ✅ **iOS**: Fully implemented (requires macOS with Xcode for building)
- ✅ **Web/Wasm**: Fully implemented and runs in modern browsers

## Quick start

Prerequisites: JDK 11+.

### Desktop

Linux/macOS
```bash
./gradlew :composeApp:run
```

Windows
```cmd
gradlew.bat :composeApp:run
```

### Web

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
Then open http://localhost:8080.

Build flag: Run webapp with impressum 
```bash
# Run development server with impressum
./gradlew :composeApp:wasmJsBrowserDevelopmentRun -PwithImpressum=true

# Build production distribution with impressum
./gradlew :composeApp:wasmJsBrowserDistribution -PwithImpressum=true
```



### Android

```bash
./gradlew :composeApp:installDebug
```

### iOS

Open the project in Xcode on macOS and run the iOS target.

## Build commands

```bash
./gradlew build
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDevelopmentWebpack
```
## run tests

```bash
./gradlew test
```

./gradlew :composeApp:desktopTest
./gradlew :composeApp:wasmJsBrowserTest

### UI Tests
End-to-end UI tests using Playwright test the game in a real browser, simulating actual user interactions.

**Running UI Tests Locally:**

```bash
# Install dependencies (first time only)
npm install
npx playwright install chromium

# Run UI tests
npx playwright test

# Run in headed mode (see the browser)
npx playwright test --headed
```

### Backend

run backend:
./gradlew server:run

compile backend:
./gradlew server:build

## Repository layout

The project is split into a few main areas:

- `shared/` — shared application modules and supporting code in both backend and frontend.
- `frontend/` — frontend apps, assets, integration points, and web-related configuration.
- `frontend/composeApp/` — the main Kotlin Multiplatform game app, including shared game logic and UI plus platform-specific integrations for desktop, Android, iOS, and WASM.
- `frontend/composeApp/src/commonMain/` — core game domain, systems, and UI code shared by all platforms.
- `frontend/composeApp/src/desktopMain/`, `androidMain/`, `iosMain/`, and `wasmJsMain/` — platform-specific implementations and entry points.
- `servers/` — backend, database and IAM.
- `docs/` — project documentation, architecture notes, gameplay guides, and reference material.
- `deploy/` — deployment and environment configuration.
- `scripts/` — utility scripts and automation helpers.

## License

This project is licensed under the GNU Affero General Public License (AGPL) - See LICENSE file for details

## Audio Files

The game uses sound effects and background music from various sources. For detailed information about the audio files, their sources, and licenses, see:

- [Sound Effects Documentation](composeApp/src/commonMain/composeResources/files/sounds/README.md)
- [Background Music Documentation](composeApp/src/commonMain/composeResources/files/sounds/background/README.md)

All audio files are used under their respective licenses with proper attribution where required.
