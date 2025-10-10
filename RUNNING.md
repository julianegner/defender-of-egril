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
   `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

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
