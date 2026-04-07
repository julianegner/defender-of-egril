# Defender of Egril - Installation Guide

This guide provides detailed installation instructions for all supported platforms.

## Table of Contents

- [Windows](#windows)
- [Android](#android)
- [Linux](#linux)
- [macOS](#macos)
- [iOS](#ios)
- [Web/Browser](#webbrowser)

---

## Windows

### Downloading the Game

1. Download the latest Windows installer (`.exe` or `.msi`) from the [GitHub Releases page](https://github.com/qvest-digital/defender-of-egril-fork/releases)
2. Save the installer to your preferred location

### Installing the Game

1. Double-click the downloaded installer file
2. **If Windows Defender SmartScreen appears:**
   - Windows may show a warning: "Windows protected your PC"
   - Click **"More info"** (the small link in the warning dialog)
   - Then click **"Run anyway"** to proceed with the installation
   
   This warning appears because the game is not signed with a commercial certificate. The game is safe to install.

3. Follow the installation wizard prompts
4. Once installed, you can launch the game from the Start Menu or Desktop shortcut

### Running from Source

Alternatively, you can run the game directly from source without installing:

```cmd
gradlew.bat :composeApp:run
```

---

## Android

### Method 1: Installing via GitHub Releases (Sideloading)

Since the game is not available on the Google Play Store, you'll need to install it by sideloading the APK file.

#### Step 1: Enable Installation from Unknown Sources

Before you can install APK files, you need to allow your Android device to install apps from sources other than the Play Store:

**For Android 8.0 (Oreo) and newer:**
1. Go to **Settings**
2. Navigate to **Apps & notifications** (or **Apps**)
3. Tap **Advanced** (if available)
4. Tap **Special app access**
5. Tap **Install unknown apps**
6. Select the browser or file manager you'll use to download the APK (e.g., Chrome, Firefox, Files)
7. Toggle **Allow from this source** to ON

**For Android 7.1 (Nougat) and older:**
1. Go to **Settings**
2. Navigate to **Security** (or **Security & location**)
3. Enable **Unknown sources**
4. Confirm the action when prompted

#### Step 2: Download the APK

1. On your Android device, open a web browser
2. Navigate to the [GitHub Releases page](https://github.com/qvest-digital/defender-of-egril-fork/releases)
3. Download the latest `de.egril.defender-release.apk` (or `de.egril.defender-debug.apk`) file
4. Your browser may warn you about the file type - tap **OK** or **Download anyway**

#### Step 3: Install the APK

1. Once the download is complete, tap the notification to open the APK file
   - Alternatively, open your **Downloads** folder or file manager and tap the APK file
2. You may see a warning asking if you want to install this application - tap **Install**
3. Wait for the installation to complete
4. Tap **Done** or **Open** to launch the game

#### Step 4: Disable Unknown Sources (Optional but Recommended)

For security reasons, you should disable the "Unknown sources" permission after installing the game:

1. Return to the settings used in Step 1
2. Toggle OFF the permission for the app you used to install the APK

### Method 2: Building from Source

If you have Android development tools installed, you can build and install the APK yourself:

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
```

The APK will be located at: `composeApp/build/outputs/apk/debug/de.egril.defender-debug.apk`

### Troubleshooting

- **Installation blocked:** Make sure you've enabled "Install unknown apps" for the correct application (browser or file manager)
- **Parse error:** The APK file may be corrupted. Try downloading it again
- **App not installed:** You may have an older version installed. Uninstall the old version first
- **Insufficient storage:** Free up space on your device and try again

For more detailed information about APK sideloading, see: [Android APK Installation Guide (German)](https://www.heise.de/tipps-tricks/Externe-Apps-APK-Dateien-bei-Android-installieren-so-klappt-s-3714330.html)

---

## Linux

### Installing from GitHub Releases

1. Go to the [GitHub Releases page](https://github.com/qvest-digital/defender-of-egril-fork/releases)
2. Download the appropriate package for your distribution:
   - `.deb` for Debian/Ubuntu
   - `.rpm` for Fedora/Red Hat
3. Install the package from terminal:

   **Debian/Ubuntu:**
   ```bash
   sudo dpkg -i defender-of-egril.deb
   ```

   **Fedora/Red Hat:**
   ```bash
   sudo rpm -i defender-of-egril.rpm
   ```

4. Launch the game from the application menu

### Building from Source

Alternatively, you can run the game directly from source:

```bash
./gradlew :composeApp:run
```

Or build a distributable package:

```bash
./gradlew :composeApp:packageDeb  # For Debian/Ubuntu
./gradlew :composeApp:packageRpm  # For Fedora/Red Hat
```

---

## macOS

### Installing from GitHub Releases

1. Go to the [GitHub Releases page](https://github.com/qvest-digital/defender-of-egril-fork/releases)
2. Download the `.dmg` file
3. Open the `.dmg` file and drag **Defender of Egril** to the Applications folder
4. Launch the game from the Applications folder

> **Note:** If macOS Gatekeeper prevents the app from launching, open **System Settings → Privacy & Security** and click **"Open Anyway"**. This warning appears because the app is not signed with an Apple Developer certificate.

### Building from Source

Alternatively, you can run the game directly from source:

```bash
./gradlew :composeApp:run
```

Or build a native macOS application:

```bash
./gradlew :composeApp:packageDmg
```

---

## iOS

### Requirements

- macOS with Xcode installed
- iOS Simulator or a physical iOS device

### Installation Steps

1. Clone or download the repository
2. Open `iosApp/iosApp.xcodeproj` in Xcode
3. Select your target device (Simulator or physical device)
4. Click the **Run** button (or press `Cmd + R`)
5. The app will build and launch on your selected device

**Note:** To install on a physical device, you'll need an Apple Developer account and proper code signing configured.

---

## Web/Browser

The easiest way to play Defender of Egril!

### Playing Online

Simply visit the web version at: [https://qvest-digital.github.io/defender-of-egril-fork/](https://qvest-digital.github.io/defender-of-egril-fork/)

No installation required - just open the link in a modern web browser:
- Chrome
- Firefox  
- Safari
- Edge

### Running Locally

If you want to run the web version locally:

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Then open http://localhost:8080 in your browser.

---

## System Requirements

### Minimum Requirements

- **Windows:** Windows 10 or later, 2GB RAM
- **Android:** Android 7.0 (Nougat) or later, 1GB RAM
- **Linux:** Any modern distribution with JDK 11+, 2GB RAM
- **macOS:** macOS 10.14 or later, 2GB RAM
- **iOS:** iOS 13.0 or later
- **Web:** Modern browser with WebAssembly support

### Recommended

- 4GB RAM or more
- Dedicated graphics for smoother animations

---

## Getting Help

If you encounter any issues during installation:

1. Check the [GitHub Issues](https://github.com/qvest-digital/defender-of-egril-fork/issues) page
2. Open a new issue with details about your problem
3. Include your platform, OS version, and any error messages

---

## Building from Source

For all platforms, you can build from source. See the main [README.md](README.md) for development setup instructions.
