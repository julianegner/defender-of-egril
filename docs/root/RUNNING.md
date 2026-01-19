# Running Defender of Egril

## Platform Requirements

### Desktop (JVM)
- **Windows**: Windows 10 or later, JDK 11+
- **macOS**: macOS 10.14 or later, JDK 11+
- **Linux**: Any modern distribution, JDK 11+

### Android
- Android 7.0 (API 24) or later
- ~10MB storage space

### iOS
- iOS 13.0 or later
- Requires Xcode for building (macOS only)

### Web/Browser
- Modern web browser (Chrome 119+, Firefox 120+, Safari 17+, or Edge 119+)
- JavaScript and WebAssembly enabled
- ~12MB download size for initial load

## Running on Desktop

### All Platforms (Windows, macOS, Linux)

1. Ensure you have JDK 11 or higher installed
2. Navigate to the project root directory
3. Run the following command:

```bash
./gradlew :composeApp:run
```

On Windows, use:
```cmd
gradlew.bat :composeApp:run
```

## Building a Distributable Package

### Windows (EXE Installer)
```bash
./gradlew :composeApp:packageExe
```
The installer will be created in `composeApp/build/compose/binaries/main/exe/`

### Windows (MSI Installer)
```bash
./gradlew :composeApp:packageMsi
```
The installer will be created in `composeApp/build/compose/binaries/main/msi/`

### macOS (DMG)
```bash
./gradlew :composeApp:packageDmg
```
The DMG will be created in `composeApp/build/compose/binaries/main/dmg/`

### Linux (DEB Package)
```bash
./gradlew :composeApp:packageDeb
```
The package will be created in `composeApp/build/compose/binaries/main/deb/`

## Running on Android

### Option 1: Install Pre-built APK

1. Build the APK:
   ```bash
   ./gradlew :composeApp:assembleDebug
   ```

2. The APK will be located at:
   `composeApp/build/outputs/apk/debug/de.egril.defender-debug.apk`

3. Transfer the APK to your Android device

4. Enable "Install from Unknown Sources" in your device settings

5. Install the APK

### Option 2: Using Android Studio

1. Open the project in Android Studio
2. Select the Android configuration
3. Click Run or press Shift+F10

## Running on iOS

### Requirements
- macOS with Xcode installed
- iOS Simulator or a physical iOS device

### Steps

1. Open the project in Xcode:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

2. Select your target device (Simulator or physical device)

3. Click the Run button or press Cmd+R

### Building iOS Framework

To build the iOS framework for integration:

```bash
# For iOS Simulator (ARM64)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# For physical devices
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

## Running on Web/Browser

### Development Server

Run the development server with hot reload:

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

The game will be available at: http://localhost:8080

The development server supports:
- Auto-reload on code changes
- Source maps for debugging
- Development mode optimizations

### Production Build

To build an optimized production bundle:

```bash
./gradlew :composeApp:wasmJsBrowserProductionWebpack
```

The production build will be in:
`composeApp/build/kotlin-webpack/wasmJs/productionExecutable/`

You can serve these files with any static web server:
```bash
cd composeApp/build/kotlin-webpack/wasmJs/productionExecutable/
python3 -m http.server 8080
```

### Browser Compatibility

The web version requires a modern browser with WebAssembly support:
- Chrome 119 or later
- Firefox 120 or later
- Safari 17 or later
- Edge 119 or later

### Web Features

The web version includes:
- ✅ Full game functionality
- ✅ Save games using browser localStorage
- ✅ Mouse wheel zoom
- ✅ Click and drag to pan
- ❌ Level editor (desktop-only feature)

### Clearing Save Data

To clear saved games in the browser:
1. Open browser Developer Tools (F12)
2. Go to Application/Storage tab
3. Select Local Storage
4. Clear items with prefix "defender-of-egril:"

## How to Play

1. **Main Menu**: Click "Start Game" to begin
2. **World Map**: Select an unlocked level to play
3. **Game Board**: 
   - In planning phase, select a tower type and click on an empty cell to place it
   - Click on placed towers to see upgrade options
   - Click "Start Combat" when ready
4. **Combat Phase**:
   - Click "Next Turn" to advance the combat
   - Watch as your towers attack and enemies move
   - Click "Return to Planning" to place more towers
5. **Win Condition**: Defeat all enemies
6. **Lose Condition**: Run out of health points

## Tower Types and Costs

- **Spike Tower** (10 coins): Melee, 1 range, 5 damage
- **Spear Tower** (15 coins): Ranged, 2 range, 8 damage  
- **Bow Tower** (20 coins): Ranged, 3 range, 10 damage
- **Wizard Tower** (50 coins): Area-of-Effect, 3 range, 30 damage
- **Alchemy Tower** (40 coins): Damage-over-Time, 2 range, 15 damage

## Enemy Types

- **Goblin**: 20 HP, Speed 2, Reward 5 coins
- **Skeleton**: 15 HP, Speed 2, Reward 7 coins
- **Ork**: 40 HP, Speed 1, Reward 10 coins
- **Witch**: 25 HP, Speed 2, Reward 12 coins
- **Evil Wizard**: 30 HP, Speed 1, Reward 15 coins
- **Ogre**: 80 HP, Speed 1, Reward 20 coins

## Tips

- Place Spike Towers near the path for early defense
- Save up for Wizard Towers for their area-of-effect damage
- Upgrade your towers to increase damage and range
- Don't forget about Alchemy Towers - their damage-over-time effect is powerful!
