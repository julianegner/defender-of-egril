# GitHub Actions CI/CD Workflows

This document describes the GitHub Actions workflows set up for the Defender of Egril project.

## Overview

The project uses GitHub Actions for continuous integration and deployment. The workflows are based on the [coshanu](https://github.com/julianegner/coshanu) reference project but adapted for Defender of Egril's structure.

## Workflows

### 1. Tests Workflow (`tests.yml`)

**Trigger:** On every push to any branch, pull requests to main, or manual dispatch

**Purpose:** Run automated tests across multiple platforms

**Jobs:**
- `run_tests_jvm_linux`: Run desktop tests on Ubuntu (Linux)
- `run_tests_jvm_windows`: Run desktop tests on Windows
- `run_tests_jvm_macos`: Run desktop tests on macOS
- `run_tests_android`: Run Android unit tests on Ubuntu

**Configuration:**
- JDK: 24 (Temurin distribution)
- Gradle: Uses project wrapper (9.2.1)
- Test command: `:composeApp:desktopTest` (JVM) or `:composeApp:testDebugUnitTest` (Android)

### 2. Build and Release Workflow (`build_and_release.yml`)

**Trigger:** On version tags (`v*.*.*`), manual dispatch, or pushes to main branch

**Purpose:** Build platform-specific artifacts and create releases

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

### 3. Deploy WASM to GitHub Pages (`deploy_wasm_to_github_pages.yml`)

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

### 4. Debug Android ProGuard (`debug-proguard.yml`)

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

1. Tag a commit with a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. The `build_and_release.yml` workflow will automatically:
   - Build artifacts for all platforms
   - Create a GitHub release
   - Upload all artifacts to the release

### Manual Build Trigger

You can manually trigger builds via the GitHub Actions UI:
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
1. Add a new job in `build_and_release.yml`
2. Configure the appropriate runner (e.g., `runs-on: ubuntu-latest`)
3. Add build steps with appropriate Gradle tasks
4. Upload artifacts
5. Add artifact download in `release` job
6. Update this documentation
