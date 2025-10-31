# GitHub Copilot Instructions for Defender of Egril

## Project Overview

Defender of Egril is a turn-based tower defense game built with Kotlin Multiplatform and Compose Multiplatform, supporting JVM/Desktop, Android, and iOS platforms.

## Project Architecture

### Module Structure
- `composeApp/src/commonMain/`: Platform-independent code (game logic, models, UI)
- `composeApp/src/desktopMain/`: Desktop-specific code
- `composeApp/src/androidMain/`: Android-specific code
- `composeApp/src/iosMain/`: iOS-specific code
- `composeApp/src/commonTest/`: Platform-independent tests

### Core Components
- **Model Layer** (`model/`): Domain entities (Position, Attacker, Defender, Level, GameState)
- **Game Engine** (`game/`): Game logic (GameEngine, LevelData)
- **UI Layer** (`ui/`): Compose Multiplatform screens and ViewModels

## Development Guidelines

### Code Organization
- Keep domain logic platform-independent in `commonMain`
- Use Kotlin coding conventions
- Maintain immutability where possible for state management
- Use Jetpack Compose/Compose Multiplatform for all UI

### Attack Types
- **MELEE**: Single target, close range
- **RANGED**: Single target, long range
- **AREA**: Area of Effect - affects multiple enemies
- **LASTING**: Damage over Time - lower damage but lasts multiple rounds

### Adding New Features

#### New Enemy Type
1. Add entry to `AttackerType` enum in `Attacker.kt`
2. Define stats: health, speed, reward
3. Add to level waves in `LevelData.kt`
4. Create icon in `UnitIcons.kt` if visual representation needed

#### New Tower Type
1. Add entry to `DefenderType` enum in `Defender.kt`
2. Define: baseCost, baseDamage, baseRange, attackType
3. Create icon in `UnitIcons.kt` if visual representation needed
4. UI will automatically include it in tower selection

#### New Level
Add to `LevelData.createLevels()` with:
- Unique level ID
- Level name
- AttackerWave configuration
- Initial coins and health points

## Build and Test Commands

### Building
```bash
# Build all platforms
./gradlew build

# Build Android APK
./gradlew :composeApp:assembleDebug

# Build iOS framework (macOS only)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

### Running
```bash
# Run desktop version
./gradlew :composeApp:run

# Install on Android device/emulator
./gradlew :composeApp:installDebug
```

### Testing
```bash
# Run all tests
./gradlew test

# Run common tests only
./gradlew :composeApp:cleanTestDebugUnitTest :composeApp:testDebugUnitTest
```

## Code Style

### Naming Conventions
- Classes: PascalCase
- Functions/variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Enum values: UPPER_SNAKE_CASE

### File Structure
- One class per file (unless nested/sealed classes)
- Group related functionality
- Keep files focused and cohesive

### Comments
- Use KDoc for public APIs
- Avoid obvious comments
- Explain "why" not "what" for complex logic

## Testing Strategy

### Test Structure
- Unit tests in `commonTest/kotlin/`
- Test game logic in isolation
- Use descriptive test names

### Test Coverage
- Core game mechanics (tower placement, attacks, movement)
- Edge cases (boundary conditions, invalid inputs)
- State transitions

### Cheat Codes (for testing)
**In-Game** (click coins display):
- `cash`: Add 1000 coins
- `mmmoney`: Add 1000000 coins
- `spawn <type> <level>`: Spawn enemy
  - Valid types: `goblin`, `ork`, `ogre`, `skeleton`, `wizard` (maps to EVIL_WIZARD), `witch`
  - Level is optional (scales enemy health)

**World Map** (click title):
- `unlock`, `unlockall`: Unlock all levels

## Common Pitfalls to Avoid

1. **Platform-specific code in commonMain**: Keep platform logic in respective source sets
2. **Breaking save compatibility**: Maintain backward compatibility for GameState
3. **Performance**: Avoid excessive recomposition; use remember and derivedStateOf
4. **State mutations**: Be careful with mutable state; prefer immutable updates where possible
5. **Grid constraints**: Game grid is fixed size (10x6); respect boundaries

## Dependencies

### Required
- JDK 11 or higher
- Gradle 8.9 (included via wrapper)

### Platform-Specific
- **Android**: Android SDK, compileSdk 34, minSdk 24, targetSdk 34
- **iOS**: macOS with Xcode

## Documentation

- `README.md`: Overview and quick start
- `DEVELOPMENT.md`: Detailed architecture and development guide
- `GAMEPLAY.md`: Game mechanics and rules
- `TESTING_GUIDE.md`: Manual testing procedures for UI
- `RUNNING.md`: Platform-specific running instructions

## Version Control

### Commit Messages
- Use descriptive commit messages
- Reference issue numbers when applicable
- Keep commits focused and atomic

### Pull Requests
- Test on at least one platform before submitting
- Update documentation for new features
- Ensure all tests pass

## Important Notes

- Game uses simple greedy pathfinding (move horizontal first, then vertical)
- Towers cannot be destroyed (game design choice)
- Each enemy reaching the end costs 1 health point
- Tower upgrades: +5 damage, +0.5 range per level, cost = baseCost × currentLevel
- Initial building phase allows instant tower placement
- Subsequent tower placements require build time (1-2 turns)

## Resources

For more details, refer to:
- Architecture: `DEVELOPMENT.md`
- Game rules: `GAMEPLAY.md`
- Testing: `TESTING_GUIDE.md`
