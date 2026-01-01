# GitHub Copilot Instructions for Defender of Egril

## Project Overview

Defender of Egril is a turn-based tower defense game built with Kotlin Multiplatform and Compose Multiplatform, supporting JVM/Desktop, Android, and iOS platforms.

## Project Architecture

### Module Structure
- `composeApp/src/commonMain/`: Platform-independent code (game logic, models, UI)
  - `kotlin/com/defenderofegril/`
    - `model/`: Domain models (Position, Attacker, Defender, Level, GameState, HexUtils, Trap, MineAction)
    - `game/`: Game engine and systems (GameEngine, CombatSystem, PathfindingSystem, EnemyMovementSystem, EnemyAbilitySystem, TowerManager, MineOperations, LevelData)
    - `ui/`: UI components
      - `gameplay/`: Gameplay screen components (GameMap, GameControls, GameHeader, GameLegend, DefenderButtons, DefenderInfo, ActionButtons, GameDialogs)
      - `editor/`: Level editor components (MapEditor, LevelEditor, LevelSequence, EditorDialogs, TileUtils)
      - `icon/`: Icon components (defender/, enemy/, IconUtils)
      - `loadgame/`: Save/load UI components
      - `worldmap/`: World map screen components
      - `settings/`: Settings UI components (SettingsDialog, LanguageChooser, SettingsButton)
    - `editor/`: Level editor logic (EditorModels, EditorJsonSerializer, EditorStorage, FileStorage)
    - `save/`: Save/load system (SaveModels, SaveJsonSerializer, SaveFileStorage)
    - `utils/`: Utilities (CheatCodeHandler, TimeUtils)
  - `composeResources/`: Multiplatform resources
    - `drawable/`: Image assets (icons, sprites)
      - Emoji icons: Files with `emoji_*.png` prefix for UI icons
      - Dig outcome icons: Simple versions `dig_outcome_*.png` and enhanced versions `dig_outcome_*_enhanced.png`
    - `files/tiles/`: Tile background images (organized by TileType: PATH, BUILD_AREA, ISLAND, NO_PLAY, SPAWN_POINT, TARGET)
    - `values/strings.xml`: Default English string resources (~326 strings)
    - `values-de/strings.xml`: German translations
    - `values-es/strings.xml`: Spanish translations
    - `values-fr/strings.xml`: French translations
    - `values-it/strings.xml`: Italian translations
- `composeApp/src/desktopMain/`: Desktop-specific code
- `composeApp/src/androidMain/`: Android-specific code
- `composeApp/src/iosMain/`: iOS-specific code
- `composeApp/src/wasmJsMain/`: Web/WASM-specific code
- `composeApp/src/commonTest/`: Platform-independent tests

### Core Components
- **Model Layer** (`model/`): Domain entities with hexagonal grid support
  - `Position`: Hexagonal grid coordinates with offset coordinate system
  - `Attacker`: Enemy units with abilities (summoning, healing, tower disabling)
  - `Defender`: Tower entities with mines, traps, and special abilities
  - `Level`: Map-based level configuration with tile system
  - `GameState`: Complete game state including field effects and traps
  - `HexUtils`: Hexagonal grid calculations and neighbor detection
  - `Trap`: Dwarven mine traps
  - `MineAction`: Mining action outcomes
  
- **Game Engine** (`game/`): Modular game systems
  - `GameEngine`: Main game orchestrator
  - `CombatSystem`: Attack resolution and damage calculation
  - `PathfindingSystem`: Hexagonal pathfinding with blocked tile detection
  - `EnemyMovementSystem`: Enemy movement and turn execution
  - `EnemyAbilitySystem`: Special enemy abilities (summoning, healing, disabling)
  - `TowerManager`: Tower placement, upgrade, and action management
  - `MineOperations`: Dwarven mine digging and coin generation
  - `LevelData`: Bridge between hardcoded levels and editor levels
  
- **UI Layer** (`ui/`): Modular Compose Multiplatform screens
  - `GameViewModel`: Central state management with save/load integration
  - `GamePlayScreen`: Main gameplay orchestrator (428 lines, refactored from 2,835 lines)
  - `gameplay/`: 8 component files for gameplay UI
  - `editor/`: 8 component files for level editor (desktop and web/wasm only)
  - `icon/`: Organized icon components (defenders, enemies, utilities)
  - `loadgame/`: Save game management UI
  - `worldmap/`: Level selection and world map UI
  - `settings/`: Settings dialog, language chooser, and settings button components
  - `LocalizationUtils.kt`: Extension functions for localizing game types (DefenderType, AttackerType, AttackType)
  
- **Editor System** (`editor/`): Level creation and editing (desktop and web/wasm only)
  - `EditorModels`: Map and level data structures
  - `EditorJsonSerializer`: Manual JSON serialization for editor data
  - `EditorStorage`: Persistent storage for maps, levels, and sequences
  - `FileStorage`: Platform-specific file I/O
  
- **Save/Load System** (`save/`): Game progress persistence
  - `SaveModels`: SavedGame and WorldMapSave data structures
  - `SaveJsonSerializer`: Manual JSON serialization for saves
  - `SaveFileStorage`: Save file management and state conversion

## Development Guidelines

### Code Organization
- Keep domain logic platform-independent in `commonMain`
- Use Kotlin coding conventions
- Maintain immutability where possible for state management
- Use Jetpack Compose/Compose Multiplatform for all UI
- Modular UI structure: Extract large screens into focused component files (see `ui/gameplay/` and `ui/editor/`)
- Manual JSON serialization for cross-platform compatibility (see `EditorJsonSerializer`, `SaveJsonSerializer`)

### Localization System
- **Plugin**: Uses `compose-multiplatform-localize` plugin (version 1.1.1) for string resource management
- **String Resources**: Located in `composeApp/src/commonMain/composeResources/`
  - `values/strings.xml`: Default English strings
  - `values-de/strings.xml`: German translations
  - `values-es/strings.xml`: Spanish translations
  - `values-fr/strings.xml`: French translations
  - `values-it/strings.xml`: Italian translations
- **Generated Code**: Plugin generates `AppLocale` enum, `LocalizedStrings` object, and `currentLanguage` state
- **Usage in Composables**: Use `stringResource(Res.string.key_name)` for UI strings
- **Dynamic Localization**: For runtime string access, use `LocalizedStrings.get("key_name", locale)`
- **Language Switching**: Change `currentLanguage.value` to switch languages (triggers recomposition)
- **Settings UI**: All screens have a settings button that opens `SettingsDialog` with:
  - Language selection
  - Dark mode toggle
  - Enhanced dig outcome images toggle (default: ON) - switches between `dig_outcome_*.png` and `dig_outcome_*_enhanced.png`
  - Tile background images toggle (default: ON) - enables loading random tile images from `files/tiles/{TileType}/`
  - Sound settings, difficulty, and other preferences
- **Type Localization**: `LocalizationUtils.kt` provides extension functions for:
  - `DefenderType.getLocalizedName()` - Tower names
  - `DefenderType.getLocalizedShortName()` - Compact tower names
  - `AttackerType.getLocalizedName()` - Enemy names
  - `AttackType.getLocalizedName()` - Attack type names
- **Translation Requirements**:
  - ALL user-facing strings MUST use `stringResource(Res.string.key_name)` - no hardcoded strings
  - Exceptions: Cheat codes (not translated), single-character symbols (•, ✓, etc.), variable interpolations
  - New strings MUST be added to ALL language files (values/, values-de/, values-es/, values-fr/, values-it/)
  - **IMPORTANT**: Check for duplicate string keys before adding new strings - each `name` attribute must be unique within a file
  - Use this command to check for duplicates: `grep 'string name=' file.xml | sed 's/.*name="\([^"]*\)".*/\1/' | sort | uniq -d`
  - If a string already exists in the codebase, reuse it instead of creating a duplicate
  - Run `TranslationCoverageTest` to verify complete translation coverage

### Icons and Emojis
- **DO NOT use Unicode emojis/icons in Kotlin code** - They don't display correctly on WASM
- **DO NOT include Unicode emojis/icons in string resources** - Use icon components instead
- **Use Icon Components**: All icons are in `ui/icon/IconUtils.kt` (e.g., `WarningIcon`, `InfoIcon`, `LightningIcon`, `HeartIcon`, `LockIcon`)
- **Display Icons**: Use composable icon functions with `Row` and proper spacing
- **Example**: Instead of `"⚠️ Warning text"`, use:
  ```kotlin
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      WarningIcon(size = 14.dp)
      Text("Warning text")
  }
  ```

### Grid System
- **Hexagonal Grid**: Uses offset coordinate system (even-q vertical layout)
- **HexUtils**: Provides neighbor detection, distance calculation, and line-of-sight
- **Tile Types**: PATH, BUILD_AREA, ISLAND (2x2), NO_PLAY, SPAWN_POINT, TARGET, WAYPOINT
- **Tile Rendering**: 
  - Color-based backgrounds (always available as fallback)
  - Optional image backgrounds: Random images loaded from `files/tiles/{TileType}/` subdirectories
  - `TileImageProvider.kt` handles image loading and caching
  - Settings toggle to switch between images and colors (default: images enabled)
- **Pathfinding**: Custom hexagonal pathfinding with blocked tile detection

### Attack Types
- **MELEE**: Single target, close range
- **RANGED**: Single target, long range
- **AREA** (Fireball): Area of Effect - affects multiple enemies in range
- **LASTING** (Acid): Damage over Time - lower damage but lasts multiple rounds
- **NONE**: Special structures (mines, dragon's lair) with no attack capability

### Current Tower Types
- **Spike Tower** (Pike): Melee defense, 1 range, cheapest tower (10 coins), max range 2 at level 5+
- **Spear Tower**: Medium-range piercing, 2 range (15 coins)
- **Bow Tower**: Long-range archery, 3 range (20 coins)
- **Wizard Tower**: Area-of-effect fireball, 3 range, 2 turn build time (50 coins)
- **Alchemy Tower**: Acid damage-over-time, 2 range (40 coins)
- **Ballista Tower**: Long-range siege, 5 range, minimum range 3, 2 turn build time (60 coins)
- **Dwarven Mine**: Generates coins through mining, 0 damage, special dig action, 3 turn build time (100 coins)
- **Dragon's Lair**: Spawns dragons, cannot be sold or upgraded (0 coins - special placement)

### Current Enemy Types
- **Goblin**: Fast, weak (20 HP, speed 2, 5 coins base reward × level)
- **Ork**: Slow, tough (40 HP, speed 1, 10 coins base reward × level)
- **Ogre**: Very tough (80 HP, speed 1, 20 coins base reward × level)
- **Skeleton**: Fast undead (15 HP, speed 2, 7 coins base reward × level)
- **Evil Wizard**: Magic attacker (30 HP, speed 1, 15 coins base reward × level)
- **Witch**: Dark magic (25 HP, speed 2, 12 coins base reward × level)
- **Blue Demon**: Fast, acid immune (15 HP, speed 3, 10 coins base reward × level)
- **Red Demon**: Tough, fireball immune (60 HP, speed 1, 15 coins base reward × level)
- **Evil Mage**: Can summon minions (40 HP, speed 1, 20 coins base reward × level)
- **Red Witch**: Can disable towers (30 HP, speed 2, 18 coins base reward × level)
- **Green Witch**: Can heal other enemies (25 HP, speed 2, 15 coins base reward × level)
- **Ewhad**: Boss with summoning (200 HP, speed 1, 100 coins base reward × level)
- **Dragon**: Powerful boss, starts slow then flies fast (500 HP, variable speed, 0 coins)

### Adding New Features

#### New Enemy Type
1. Add entry to `AttackerType` enum in `Attacker.kt`
2. Define stats: health, speed, reward, special abilities (immunities, summoning, healing, tower disabling)
3. Add icon in `ui/icon/enemy/` directory (follow existing pattern)
4. Add to level editor enemy spawns or hardcoded levels in `LevelData.kt`
5. If enemy has special abilities, update `EnemyAbilitySystem.kt`

#### New Tower Type
1. Add entry to `DefenderType` enum in `Defender.kt`
2. Define: baseCost, baseDamage, baseRange, attackType, actionsPerTurn, buildTime, minRange (optional)
3. Create icon in `ui/icon/defender/` directory (follow existing pattern)
4. Add icon mapping in `UnitIcons.kt`
5. UI will automatically include it in tower selection
6. If tower has special mechanics (like Dwarven Mine), update `TowerManager.kt` and relevant game systems

#### New Level (Editor Method - Recommended)
1. Use the in-game Level Editor (desktop and web/wasm only):
   - Create or select a map in Map Editor tab
   - Create level in Level Editor tab
   - Configure enemy spawns, starting resources, and available towers
   - Arrange in Level Sequence tab
2. Files are saved in `~/.defender-of-egril/gamedata/` (Linux/Mac) or `%USERPROFILE%\.defender-of-egril\gamedata\` (Windows)

#### New Level (Code Method - Legacy)
Add to `LevelData.createLevels()` with:
- Unique level ID
- Map reference (from editor or procedurally generated)
- Level name and subtitle
- Enemy spawn configuration (turn-based)
- Initial coins and health points
- Available tower types

#### New Language (Localization)
1. Create directory: `composeApp/src/commonMain/composeResources/values-{lang}/`
   - Use ISO 639-1 language codes (e.g., `de` for German, `es` for Spanish, `fr` for French)
2. Create `strings.xml` with translated strings:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <resources>
       <string name="app_name">Translated App Name</string>
       <string name="start_game">Translated Start Game</string>
       <!-- ... translate all strings from values/strings.xml ... -->
   </resources>
   ```
3. Clean and rebuild: `./gradlew clean build`
4. Plugin automatically generates `AppLocale.{LANG}` enum value
5. Update `LanguageChooser.kt`'s `getCountryCode()` function if language code differs from country code
6. Test language switching via Settings dialog
7. All ~550+ strings must be translated for complete localization
8. Run `TranslationCoverageTest` to verify all keys are present

#### Adding New UI Strings
1. **Add to English first**: Add new string to `values/strings.xml`
2. **Translate to all languages**: Add identical key to values-de/, values-es/, values-fr/, values-it/
3. **Use in code**: Use `stringResource(Res.string.your_key)` in Composables
4. **Test**: Run `TranslationCoverageTest` to verify all language files are synchronized
5. **Never hardcode**: Do not use hardcoded strings like `Text("Hello")` - always use stringResource
5. Update `LanguageChooser.kt`'s `getCountryCode()` function if language code differs from country code
6. Test language switching via Settings dialog
7. All ~318 strings must be translated for complete localization

## Build and Test Commands

### Building
```bash
# Build all platforms
./gradlew build

# Build Android APK
./gradlew :composeApp:assembleDebug

# Build iOS framework (macOS only)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Build Web/WASM bundle
./gradlew :composeApp:wasmJsBrowserDevelopmentWebpack
```

### Running
```bash
# Run desktop version
./gradlew :composeApp:run

# Install on Android device/emulator
./gradlew :composeApp:installDebug

# Run Web/WASM development server
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
# Then open http://localhost:8080 in browser
```

### Testing
```bash
# Run all tests
./gradlew test

# Run common tests only
./gradlew :composeApp:cleanTestDebugUnitTest :composeApp:testDebugUnitTest

# Run translation coverage test specifically
./gradlew :composeApp:testDebugUnitTest --tests "de.egril.defender.ui.TranslationCoverageTest"
```

### Translation Testing
- **TranslationCoverageTest**: Automated test that verifies all user-facing strings are properly translated
  - Scans all UI code for hardcoded strings (except cheat codes and symbols)
  - Verifies all language files have identical keys
  - Runs as part of the standard test suite
  - **MUST pass** before merging any UI changes

## Code Style

### Naming Conventions
- Classes: PascalCase
- Functions/variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Enum values: UPPER_SNAKE_CASE

### Control Flow
- **Use if-else for boolean conditions**: When checking a boolean for both true and false cases, use `if-else` instead of two separate `if` statements:
  ```kotlin
  // DON'T do this:
  if (condition) {
      doSomething()
  }
  if (!condition) {
      doSomethingElse()
  }
  
  // DO this instead:
  if (condition) {
      doSomething()
  } else {
      doSomethingElse()
  }
  ```

### File Structure
- One class per file (unless nested/sealed classes)
- Group related functionality
- Keep files focused and cohesive

### Documentation Files
- **NEVER** add markdown files to the root directory (except README.md which is allowed)
- **ALWAYS** place documentation files under `/docs` in appropriate subfolders:
  - `/docs/changes/`: Implementation summaries and change documentation
  - `/docs/visual-guides/`: Visual guides and comparisons
  - `/docs/implementation/`: Technical implementation details
  - `/docs/features/`: Feature documentation
  - `/docs/guides/`: User and developer guides
  - `/docs/root/`: Core project documentation
- Use UPPERCASE_WITH_UNDERSCORES naming convention for documentation files

### Comments
- Use KDoc for public APIs
- Avoid obvious comments
- Explain "why" not "what" for complex logic

## Testing Strategy

### Test Structure
- Unit tests in `commonTest/kotlin/`
- UI tests in `desktopTest/kotlin/` for Compose UI testing
- Test game logic in isolation
- Use descriptive test names

### Test Coverage
- Core game mechanics (tower placement, attacks, movement)
- Edge cases (boundary conditions, invalid inputs)
- State transitions
- **Always add tests for new or changed behavior**
  - When adding new features, create tests that verify the feature works as expected
  - When modifying existing behavior, add tests that verify both the old behavior is prevented and new behavior works
  - Example: For end turn warning changes, test scenarios where warning should/shouldn't appear

### UI Testing Guidelines
- Use Compose Test Rule for UI component testing
- Test user interactions (button clicks, text input, etc.)
- Verify UI state changes based on game state
- Capture screenshots for visual verification when appropriate
- See `GamePlayScreenTest.kt` for examples of UI tests

### Cheat Codes (for testing)
**In-Game** (click coins display):
- `cash`: Add 1000 coins
- `mmmoney`: Add 1000000 coins
- `spawn <type> <level>`: Spawn enemy
  - Valid types: `goblin`, `ork`, `ogre`, `skeleton`, `wizard` (maps to EVIL_WIZARD), `witch`, `bluedemon`, `reddemon`, `evilmage`, `redwitch`, `greenwitch`, `ewhad`, `dragon`
  - Level is optional (scales enemy health)

**World Map** (click title):
- `unlock`, `unlockall`: Unlock all levels

## Common Pitfalls to Avoid

1. **Platform-specific code in commonMain**: Keep platform logic in respective source sets
2. **Breaking save compatibility**: Maintain backward compatibility for SavedGame and WorldMapSave models
3. **Performance**: Avoid excessive recomposition; use remember and derivedStateOf
4. **State mutations**: Be careful with mutable state; prefer immutable updates where possible
5. **Hexagonal grid constraints**: Game uses hexagonal offset coordinates (even-q); respect neighbor calculations from HexUtils
6. **JSON serialization**: Use manual serialization (not kotlinx.serialization) to match existing patterns in EditorJsonSerializer and SaveJsonSerializer
7. **Level Editor**: Desktop and web/wasm only - not available on mobile platforms
8. **File storage paths**: Use FileStorage interface for platform-specific paths
9. **Large file refactoring**: Keep UI component files under 500 lines; extract into modular structure (see `ui/gameplay/` pattern)
10. **Hardcoded strings**: NEVER use hardcoded strings in UI (e.g., `Text("Hello")`). Always use `stringResource(Res.string.key_name)`. Add new strings to ALL language files (values/, values-de/, values-es/, values-fr/, values-it/). Run `TranslationCoverageTest` to verify.

## Dependencies

### Required
- JDK 11 or higher
- Gradle 8.9 (included via wrapper)

### Localization
- **compose-multiplatform-localize** plugin (v1.1.1): String resource management and code generation
- **FlagKit** (v1.1.0): Vector flag icons for language selection UI
- **multiplatform-settings** (v1.3.0): Cross-platform settings persistence (prepared for future use)

### Platform-Specific
- **Android**: Android SDK, compileSdk 34, minSdk 24, targetSdk 34
- **iOS**: macOS with Xcode
- **Web/WASM**: Modern browsers (Chrome, Firefox, Safari, Edge) with WebAssembly support

## Documentation

- `README.md`: Overview and quick start
- `docs/root/DEVELOPMENT.md`: Detailed architecture and development guide
- `docs/root/GAMEPLAY.md`: Game mechanics and rules
- `docs/guides/TESTING_GUIDE.md`: Manual testing procedures for UI
- `docs/root/RUNNING.md`: Platform-specific running instructions
- `docs/guides/LEVEL_EDITOR.md`: Level editor features and usage (desktop and web/wasm only)
- `docs/implementation/SAVE_LOAD_IMPLEMENTATION.md`: Save/load system architecture
- `docs/features/GAMEPLAY_SCREEN_EXTRACTION.md`: UI component refactoring details
- `docs/features/LEVEL_EDITOR_REFACTORING.md`: Editor component refactoring
- `docs/features/CODE_REFACTORING_ANALYSIS.md`: Code organization and patterns
- `docs/guides/WEB_WASM_GUIDE.md`: Web/WASM platform guide
- `docs/implementation/LOCALIZATION_IMPLEMENTATION.md`: Localization system implementation and usage
- `docs/changes/APPLICATION_BANNER_THEME_AWARE.md`: ApplicationBanner theme-aware implementation
- `docs/visual-guides/APPLICATION_BANNER_VISUAL_COMPARISON.md`: Visual comparison of banner changes

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

- Game uses hexagonal grid with offset coordinates (even-q vertical layout)
- Pathfinding uses custom hexagonal algorithm with blocked tile detection
- Towers cannot be destroyed (game design choice), except Dragon's Lair can't be sold
- Each enemy reaching the end costs 1 health point
- Tower upgrades: +5 damage, +0.5 range per level (some towers have max ranges)
- Initial building phase allows instant tower placement
- Subsequent tower placements require build time (1-2 turns based on tower type)
- Dwarven Mine: Special tower that generates coins through mining (dig action)
- Dragon's Lair: Special structure that spawns dragons (cannot be sold or upgraded)
- Enemy abilities: Some enemies can summon, heal, disable towers, or have damage immunities
- Save/Load system: Automatic world map progress saving, manual in-game saves
- Level Editor: Desktop and web/wasm only, stores data in `~/.defender-of-egril/gamedata/` (Linux/Mac) or `%USERPROFILE%\.defender-of-egril\gamedata\` (Windows)
- Pan and Zoom: All platforms support panning (drag) and zooming (mouse wheel/pinch, 0.5x to 3x)
- Minimap: Automatically appears when zoomed in to show current viewport position
- Localization: Supports English (default), German, Spanish, French, and Italian. Language can be switched via Settings button on all screens. Uses `compose-multiplatform-localize` plugin for string resource management.

## Resources

For more details, refer to:
- Architecture: `docs/root/DEVELOPMENT.md`
- Game rules: `docs/root/GAMEPLAY.md`
- Testing: `docs/guides/TESTING_GUIDE.md`
- Level Editor: `docs/guides/LEVEL_EDITOR.md`
- Save/Load: `docs/implementation/SAVE_LOAD_IMPLEMENTATION.md`
- UI Refactoring: `docs/features/GAMEPLAY_SCREEN_EXTRACTION.md`, `docs/features/LEVEL_EDITOR_REFACTORING.md`
- Web Platform: `docs/guides/WEB_WASM_GUIDE.md`
- Localization: `docs/implementation/LOCALIZATION_IMPLEMENTATION.md`
