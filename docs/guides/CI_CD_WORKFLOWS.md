# GitHub Actions CI/CD Workflows

This document describes the GitHub Actions workflows set up for the Defender of Egril project.

## Overview

The project uses GitHub Actions for continuous integration and deployment. The workflows are based on the [coshanu](https://github.com/julianegner/coshanu) reference project but adapted for Defender of Egril's structure.

## Workflows

### 1. Tests Workflow (`tests.yml`)

**Trigger:** On pushes to main branch, pull requests to main, or manual dispatch

**Purpose:** Run automated tests across multiple platforms

**Jobs:**
- `run_tests_jvm_linux`: Run desktop tests on Ubuntu (Linux)
- `run_tests_jvm_windows`: Run desktop tests on Windows
- `run_tests_jvm_macos`: Run desktop tests on macOS
- `run_tests_android`: Run Android unit tests on Ubuntu

**Note on WASM Testing:**
WASM browser tests are not currently run in CI because the common test suite uses JVM-specific APIs (`File`, `System`, `runBlocking`) that are not available in the WASM platform. The WASM build itself compiles and runs successfully - only the test code needs refactoring for true multiplatform support. This is a known limitation and does not affect WASM runtime functionality.

**Configuration:**
- JDK: 24 (Temurin distribution)
- Gradle: Uses project wrapper (9.2.1)
- Test command: `:composeApp:desktopTest` (JVM) or `:composeApp:testDebugUnitTest` (Android)

### 2. Release Workflow (`release.yml`)

**Trigger:** Manual dispatch (`workflow_dispatch`) with a `version` input (format: `major.minor.patch`)

**Purpose:** Create a versioned release with platform-specific installers/packages

**Inputs:**
- `version` (required): Release version string in `major.minor.patch` format (e.g. `1.2.3`)

**Jobs:**
- `prepare`: Validate version string and compute Android `versionCode`
- `tag`: Create and push git tag `v<version>` on the current commit
- `build_android`: Build Android APK and AAB (signed if secrets are configured, debug otherwise)
- `build_linux_deb`: Build Linux DEB package
- `build_linux_snap`: Build Linux Snap package (optional, built from DEB artifact)
- `build_linux_flatpak`: Build Linux Flatpak bundle (optional, built from DEB artifact)
- `build_linux_arch`: Build Linux Arch Linux package (optional, built from DEB artifact)
- `build_macos`: Build macOS DMG
- `build_ios`: Build iOS IPA (skipped when Apple signing secrets are absent)
- `build_windows_exe`: Build Windows EXE installer
- `build_windows_msi`: Build Windows MSI installer
- `build_wasm`: Build WebAssembly bundle
- `release`: Create GitHub Release with all artifacts attached

**Build Artifacts (attached to the GitHub Release):**
- Android APK: `de.egril.defender-productionRelease.apk` (or debug suffix without signing)
- Android AAB: `de.egril.defender-productionRelease.aab` (or debug suffix without signing)
- Linux DEB: `defender-of-egril_<version>_amd64.deb`
- Linux Snap: `defender-of-egril_<version>_amd64.snap` (optional)
- Linux Flatpak: `de.egril.defender-<version>.flatpak` (optional)
- Linux Arch: `defender-of-egril-<version>-1-x86_64.pkg.tar.zst` (optional)
- macOS: DMG file from `composeApp/build/compose/binaries/main/dmg/`
- iOS: `*.ipa` (when Apple signing secrets are configured)
- Windows: `DefenderOfEgril-<version>.exe` and `DefenderOfEgril-<version>.msi`
- WASM: `defender-of-egril_<version>_wasm.zip`

**Configuration:**
- JDK: 24 (Temurin distribution)
- Gradle: Uses project wrapper
- Version is passed to Gradle as `-PappVersion=<version>` and sets `versionName`, `versionCode`, `packageVersion`, and `AppBuildInfo.VERSION_NAME`

**Required Secrets (optional – build degrades gracefully without them):**
- `ANDROID_KEYSTORE_BASE64`: Base64-encoded Android release keystore
- `ANDROID_KEY_ALIAS`: Key alias in the keystore
- `ANDROID_KEY_PASSWORD`: Key password
- `ANDROID_STORE_PASSWORD`: Keystore password
- `APPLE_CERTIFICATE`: Base64-encoded Apple P12 signing certificate
- `APPLE_CERTIFICATE_PASSWORD`: Password for the P12 certificate
- `APPLE_PROVISIONING_PROFILE`: Base64-encoded `.mobileprovision` file
- `APPLE_TEAM_ID`: 10-character Apple Developer team identifier

### 3. Build and Release Workflow (`build_and_release.yml`) *(legacy)*

**Trigger:** On version tags (`v*.*.*`), manual dispatch, or pushes to main branch

**Purpose:** Legacy build workflow – superseded by `release.yml` for new releases

**Jobs:**
- `check_version_tag`: Verify version tag (only runs on tags)
- `build_wasm_js`: Build WebAssembly/JavaScript bundle
- `build_jvm`: Build JVM/Desktop JAR
- `build_macos`: Build macOS DMG
- `build_linux_deb`: Build Linux DEB package
- `build_windows`: Build Windows EXE
- `release`: Create GitHub release with all artifacts (only on tags)

**Build Artifacts:**
- WASM: `defender-of-egril_wasm.zip`
- JVM: `defender-of-egril-linux-x64-*.jar`
- macOS: `defender-of-egril_macos.zip`
- Linux: `defender-of-egril_*_amd64.deb`
- Windows: `defender-of-egril-*.exe`

**Configuration:**
- JDK: 24 (Temurin distribution)
- Gradle: Uses project wrapper (9.2.1)
- Builds only run on version tags or manual dispatch

### 4. Deploy WASM to GitHub Pages (`deploy_wasm_to_github_pages.yml`)

**Trigger:** Manual dispatch only (commented out auto-deploy on main branch)

**Purpose:** Deploy the WebAssembly version to GitHub Pages

**Jobs:**
- `build-and-deploy`: Build WASM bundle and deploy to GitHub Pages

**Configuration:**
- JDK: 24 (Temurin distribution)
- Gradle: Uses project wrapper (9.2.1)
- Build command: `:composeApp:wasmJsBrowserDistribution`
- Deployment: Uses GitHub Pages with proper permissions

**Permissions Required:**
- `contents: read` - To fetch repository content
- `pages: write` - To deploy to GitHub Pages
- `id-token: write` - Required by actions/configure-pages

### 5. Build Windows EXE (`build-windows-exe.yml`)

**Trigger:** On pushes to main branch, pull requests to main, or manual dispatch

**Purpose:** Build Windows EXE installer independently

**Jobs:**
- `build_windows_exe`: Build Windows EXE installer on Windows runner

**Configuration:**
- JDK: 24 (Temurin distribution)
- Gradle: Uses project wrapper (9.2.1)
- Build command: `:composeApp:packageExe`
- Runner: `windows-latest`

**Artifacts:**
- Windows EXE installer uploaded as `windows-exe-installer`
- Retention: 30 days
- Can be downloaded from the workflow run

**Usage:**
This workflow provides an easy way to build and test Windows EXE installers without creating a full release. Artifacts can be downloaded from:
1. Go to Actions tab
2. Select "Build Windows EXE" workflow
3. Click on a completed run
4. Download the `windows-exe-installer` artifact

### 6. Debug Android ProGuard (`debug-proguard.yml`)

**Trigger:** Manual dispatch only

**Purpose:** Debug Android ProGuard builds with signed APK

**Jobs:**
- `build_apk_release`: Build signed release APK with ProGuard

**Configuration:**
- JDK: 24 (Temurin distribution)
- Gradle: Uses project wrapper (9.2.1)
- Build command: `:composeApp:packageReleaseApk`

**Required Secrets:**
- `ANDROID_KEYSTORE`: Base64-encoded Android keystore
- `ANDROID_KEYSTORE_PASSWORD`: Keystore password
- `ANDROID_KEY_PASSWORD`: Key password
- `ANDROID_KEY_ALIAS`: Key alias

## Usage

### Running Tests

Tests run automatically on every push and pull request. No action needed.

### Creating a Release

Use the **Release** workflow (`release.yml`) to create a new release:

1. Go to the **Actions** tab in the GitHub repository.
2. Select the **"Release"** workflow in the left sidebar.
3. Click **"Run workflow"**.
4. Enter the version number in `major.minor.patch` format (e.g. `1.2.3`).
5. Click **"Run workflow"** to start.

The workflow will:
- Validate the version string.
- Create and push a git tag `v<version>` for the current commit.
- Build all platform artifacts in parallel (Android, Linux, macOS, Windows, WASM; iOS when signing secrets are configured).
- Create a GitHub Release tagged with the version and attach all artifacts for download.

#### Configuring Signing (optional)

**Android signed release:** Add the following repository secrets:
- `ANDROID_KEYSTORE_BASE64` – Base64-encoded Android release keystore (`base64 release.keystore`)
- `ANDROID_KEY_ALIAS` – Key alias
- `ANDROID_KEY_PASSWORD` – Key password
- `ANDROID_STORE_PASSWORD` – Keystore password

Without these secrets the workflow falls back to an unsigned debug build.

**iOS IPA:** Add the following repository secrets:
- `APPLE_CERTIFICATE` – Base64-encoded Apple P12 signing certificate
- `APPLE_CERTIFICATE_PASSWORD` – Password for the P12
- `APPLE_PROVISIONING_PROFILE` – Base64-encoded `.mobileprovision` file
- `APPLE_TEAM_ID` – 10-character Apple Developer team identifier

Without these secrets the iOS build job is automatically skipped.

### Manual Build Trigger (legacy)

You can still manually trigger the legacy build workflow via the GitHub Actions UI:
1. Go to Actions tab
2. Select "Build (and Release)" workflow
3. Click "Run workflow"
4. Select branch and run

### Deploying to GitHub Pages

1. Go to Actions tab
2. Select "Deploy WasmJS App to GitHub Pages" workflow
3. Click "Run workflow"
4. The WASM version will be deployed to GitHub Pages

### Debug Android Build

For debugging Android ProGuard issues:
1. Set up required secrets in repository settings
2. Go to Actions tab
3. Select "debug android" workflow
4. Click "Run workflow"

## Differences from Reference Project (coshanu)

The workflows have been adapted from the coshanu project with the following changes:

1. **JDK Version**: Using JDK 24 (same as reference, project requires JDK 11 or higher)
2. **Project Names**: Updated from "coshanu" to "defender-of-egril-fork" / "defenderOfEgril"
3. **Paths**: Updated to match project structure
4. **Gradle Version**: Uses wrapper (9.2.1) instead of hardcoded version
5. **Build Conditions**: Fixed to properly work with tags and manual dispatch
6. **Excluded**: itch.io deployment workflow (as requested)

## Troubleshooting

### Tests Failing

Check the test logs in the GitHub Actions run. Common issues:
- Missing dependencies
- Platform-specific test failures
- Compilation errors

### Build Failures

Check the build logs in the GitHub Actions run. Common issues:
- Missing signing keys (Android)
- Gradle configuration errors
- Platform-specific build tool issues

### Gradle Deprecation Warnings

You may see deprecation warnings during builds:
```
Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.
```

These warnings come from the Android Gradle Plugin and other dependencies, not from project code. The warnings include:
- Multi-string notation for dependencies (internal to Android plugin)
- Archives configuration (internal to Android plugin)

These are being tracked by the plugin maintainers and will be fixed in future versions. They do not affect the build output or functionality.

### GitHub Pages Deployment

Ensure GitHub Pages is enabled in repository settings:
1. Go to Settings > Pages
2. Set Source to "GitHub Actions"
3. Save changes

## Maintenance

### Updating Actions Versions

The workflows use specific versions of GitHub Actions:
- `actions/checkout@v4`
- `actions/setup-java@v4`
- `gradle/gradle-build-action@v3`
- `actions/upload-artifact@v4`
- `actions/download-artifact@v4`
- `actions/configure-pages@v5`
- `actions/upload-pages-artifact@v3`
- `actions/deploy-pages@v4`
- `softprops/action-gh-release@v2`
- `montudor/action-zip@v1`

Check for updates periodically and test before updating.

### Adding New Platforms

To add support for new platforms:
1. Add a new job in `release.yml`
2. Configure the appropriate runner (e.g., `runs-on: ubuntu-latest`)
3. Add build steps with appropriate Gradle tasks, passing `-PappVersion="${APP_VERSION}"`
4. Upload artifacts with `actions/upload-artifact@v4`
5. Add the job to the `needs` list of the `release` job and download the artifact there
6. Update this documentation
