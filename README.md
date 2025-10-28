# Defender of Egril

Defend the meadows of Egril against the Hordes of Gleid Thyae under the Banner of the evil Ewhad.

## Game Description

Defender of Egril is a turn-based tower defense game built with Kotlin Multiplatform and Compose Multiplatform.

### For Players

Defend the meadows of Egril against waves of attackers including goblins, orks, ogres, skeletons, evil wizards, and witches.

### Game Mechanics

- **Single-Phase Turn System**: Place towers, upgrade them, and attack enemies all in the same turn
- **Action Points**: Each tower has a limited number of actions per turn
- **Manual Targeting**: Choose which enemies your towers attack
- **Build Time**: Newly placed towers need time to be ready (except during initial building phase)
- **Initial Building Phase**: Place towers instantly before the battle begins
- **Separate Enemy Turns**: Enemies move during their own turn phase
- **Tower Types**:
  - **Spike Tower**: Close-range melee defense
  - **Spear Tower**: Medium-range piercing attacks
  - **Bow Tower**: Long-range archery
  - **Wizard Tower**: Powerful fireball with area-of-effect damage
  - **Alchemy Tower**: Acid attacks with damage-over-time effects
  
- **Enemy Types**:
  - Goblins: Fast but weak
  - Orks: Slow but tough
  - Ogres: Very tough with high health
  - Skeletons: Fast undead warriors
  - Evil Wizards: Magical attackers
  - Witches: Dark magic wielders

- **Upgrade System**: Earn coins by defeating enemies and use them to upgrade your towers for more damage and range
- **Indestructible Defenses**: Your towers cannot be destroyed
- **Health System**: Each enemy that reaches the end costs you one health point
- **Level Progression**: Five levels with increasing difficulty
- **World Map**: Track your progress and see which levels are unlocked
- **Interactive Game Board**: Pan and zoom the game board for better visibility
  - **Pan/Scroll**: Drag to pan in any direction
  - **Zoom**: Pinch to zoom (0.5x to 3x) on mobile/tablet, Ctrl+scroll on desktop
  - **Minimap**: Automatically appears when zoomed in to show your current view position

## Quick Start

### Running the Game

**Desktop (Linux/macOS):**
```bash
./gradlew :composeApp:run
```

**Desktop (Windows):**
```cmd
gradlew.bat :composeApp:run
```

**Android:**
```bash
./gradlew :composeApp:installDebug
```

**iOS (requires macOS with Xcode):**
Open `iosApp/iosApp.xcodeproj` in Xcode and click Run

📖 **For detailed gameplay instructions, see [GAMEPLAY.md](GAMEPLAY.md)**

## Building and Running

### Prerequisites

- JDK 11 or higher
- Gradle 8.9 (included via wrapper)

### Supported Platforms

- ✅ **JVM/Desktop**: Fully implemented and tested on Linux
- ✅ **Android**: Fully implemented and tested (APK builds successfully)
- ✅ **iOS**: Fully implemented (requires macOS with Xcode for building)

### Building

```bash
# Build all platforms
./gradlew build

# Build Android APK
./gradlew :composeApp:assembleDebug

# Build iOS framework (requires macOS)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

### Running Desktop Version

```bash
./gradlew :composeApp:run
```

### Running Android

Install the APK from `composeApp/build/outputs/apk/debug/composeApp-debug.apk` on an Android device or emulator.

### Running iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode (macOS required)
2. Select a simulator or device
3. Click Run

## Project Structure

```
composeApp/src/
├── commonMain/kotlin/com/defenderofegril/
│   ├── model/           # Game domain models
│   │   ├── Attacker.kt
│   │   ├── Defender.kt
│   │   ├── GameState.kt
│   │   ├── Level.kt
│   │   └── Position.kt
│   ├── game/            # Game logic
│   │   ├── GameEngine.kt
│   │   └── LevelData.kt
│   ├── ui/              # User interface
│   │   ├── GameViewModel.kt
│   │   ├── GamePlayScreen.kt
│   │   ├── WorldMapScreen.kt
│   │   └── MenuScreens.kt
│   └── App.kt           # Main application entry
└── desktopMain/kotlin/com/defenderofegril/
    └── main.kt          # Desktop platform entry point
```

## Game Design

### Level Progression

1. **The First Wave**: 5 Goblins - Tutorial level
2. **Mixed Forces**: Mixed enemies including Goblins, Skeletons, and Orks
3. **The Ork Invasion**: Tougher waves with more Orks
4. **Dark Magic Rises**: Introduction of Wizards and Witches
5. **The Final Stand**: All enemy types in multiple waves

### Combat System

- **Planning Phase**: Place and upgrade towers
- **Combat Phase**: Turn-by-turn execution where towers attack and enemies move
- **Turn Execution**:
  1. Spawn new attackers from waves
  2. Defenders attack enemies in range
  3. Attackers move toward the goal
  4. Apply damage-over-time effects
  5. Process defeated enemies and award coins

## License

GNU Affero General Public License - See LICENSE file for details

## Development

This game is built using:
- Kotlin 2.0.21
- Compose Multiplatform 1.7.0
- Kotlin Multiplatform for cross-platform support
