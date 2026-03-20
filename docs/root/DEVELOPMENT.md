# Development Guide

## Project Architecture

### Module Structure

The project follows a clean architecture pattern:

```
composeApp/
├── src/
│   ├── commonMain/          # Platform-independent code
│   │   └── kotlin/com/defenderofegril/
│   │       ├── model/       # Domain models
│   │       ├── game/        # Game logic/engine
│   │       ├── ui/          # UI components
│   │       └── App.kt       # Main composable
│   └── desktopMain/         # Desktop-specific code
│       └── kotlin/com/defenderofegril/
│           └── main.kt      # JVM entry point
```

### Domain Models

#### Core Entities

1. **Position**: Represents a grid coordinate
2. **Attacker**: Enemy units with health, speed, and rewards
3. **Defender**: Tower entities with damage, range, and attack types
4. **Level**: Defines game level configuration
5. **GameState**: Manages the current game state

#### Attack Types

- **MELEE**: Single target, close range
- **RANGED**: Single target, long range  
- **AOE**: Area of Effect - damages multiple enemies
- **DOT**: Damage over Time - continuous damage for multiple rounds

### Game Engine

The `GameEngine` class manages all game logic:

- **Tower Placement**: Validates positions and deducts coins
- **Tower Upgrades**: Increases damage and range
- **Turn Execution**:
  1. Spawn attackers from waves
  2. Defenders attack in range
  3. Attackers move toward goal
  4. Apply DoT effects
  5. Process defeated enemies

### UI Architecture

Built with Jetpack Compose/Compose Multiplatform:

- **GameViewModel**: State management using Kotlin Flow
- **Screen Navigation**: Sealed class hierarchy for type-safe navigation
- **Reactive UI**: State flows trigger automatic recomposition

#### Screens

1. **MainMenuScreen**: Entry point
2. **WorldMapScreen**: Level selection
3. **GamePlayScreen**: Main gameplay with grid and controls
4. **LevelCompleteScreen**: Victory/defeat screen

### Adding New Features

#### Adding a New Enemy Type

1. Add entry to `AttackerType` enum in `Attacker.kt`
2. Define stats: health, speed, reward
3. Add to level waves in `LevelData.kt`

```kotlin
TROLL("Troll", health = 100, speed = 1, reward = 25)
```

#### Adding a New Tower Type

1. Add entry to `DefenderType` enum in `Defender.kt`
2. Define: baseCost, baseDamage, baseRange, attackType
3. UI will automatically include it

```kotlin
CATAPULT("Catapult", baseCost = 60, baseDamage = 40, baseRange = 4, attackType = AttackType.AOE)
```

#### Adding a New Level (Code Method - Legacy)

Add to `LevelData.createLevels()`:

```kotlin
private fun createLevel6() = Level(
    id = 6,
    name = "The Siege",
    attackerWaves = listOf(
        AttackerWave(
            attackers = List(10) { AttackerType.GOBLIN },
            spawnDelay = 1
        )
    ),
    initialCoins = 250,
    healthPoints = 5
)
```

#### Adding a New Level (Repository Method - Recommended)

1. Create levels using the desktop Level Editor
2. Copy JSON files from `~/.defender-of-egril/gamedata/` to repository:
   ```
   composeApp/src/commonMain/composeResources/files/repository/
   ├── maps/your_map.json
   ├── levels/your_level.json
   └── sequence.json
   ```
3. Rebuild the app - repository files will be deployed to all platforms

On first launch, the app checks for repository files and uses them instead of generating defaults. See `docs/guides/LEVEL_EDITOR.md` for details.


### Game Balance

#### Tower Upgrades

Each upgrade level:
- Increases damage by 5
- Increases range by 0.5 (rounded down)
- Costs: `baseCost * currentLevel`

#### Pathfinding

Current implementation uses simple greedy pathfinding:
- Move horizontal first (towards X goal)
- Then move vertical (towards Y goal)

For more complex maps, consider implementing A* pathfinding.

### Testing Strategies

#### Cheat Codes

For testing and debugging, the game includes cheat codes:

**In-Game Cheat Codes** (accessed by clicking on the coins display during gameplay):
- **cash**: Adds 1000 coins
- **mmmoney**: Adds 1000000 coins
- **emptypocket**: Sets coins to 0
- **spawn <type> <level>**: Spawns an enemy of the specified type and level
  - Types: goblin, ork, ogre, skeleton, wizard, witch
  - Level: optional integer (default 1) that scales enemy health
  - Examples: `spawn goblin 2`, `spawn ogre 5`

**World Map Cheat Codes** (accessed by clicking the "World Map - Meadows of Egril" title):
- **unlock**, **unlockall**, **unlock all**: Unlocks all levels for testing
- **credits**: Opens the final credits screen directly (for testing without completing the game)

#### Unit Tests

Test game logic in isolation:

```kotlin
@Test
fun testPlaceDefender() {
    val level = LevelData.createLevels()[0]
    val state = GameState(level)
    val engine = GameEngine(state)
    
    assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 2)))
    assertEquals(90, state.coins) // 100 - 10
}
```

#### Integration Tests

Test full turn execution:

```kotlin
@Test
fun testCombatTurn() {
    // Setup game state with defenders and attackers
    // Execute turn
    // Verify expected damage and movement
}
```

### Performance Considerations

- **State Immutability**: GameState uses mutable lists for performance during gameplay
- **Recomposition**: State updates trigger minimal UI recomposition
- **Grid Rendering**: Fixed grid size keeps rendering predictable

### Future Enhancements

Potential additions:

1. **Pathfinding Improvements**: A* algorithm for complex paths
2. **Save/Load System**: Persist game progress
3. **Sound Effects**: Audio feedback for actions
4. **Animations**: Smooth transitions for attacks and movement
5. **More Attack Types**: Slowing, stunning, splitting projectiles
6. **Special Abilities**: Tower special moves with cooldowns
7. **Difficulty Modes**: Easy, Normal, Hard
8. **Endless Mode**: Infinite waves with increasing difficulty
9. **Achievements**: Track player accomplishments
10. **Leaderboards**: Online score tracking

### Building for Additional Platforms

#### Adding Android Support

Uncomment Android configuration in `composeApp/build.gradle.kts`:

```kotlin
android {
    namespace = "com.defenderofegril"
    compileSdk = 34
    // ... configuration
}
```

Add Android manifest and MainActivity.

#### Adding WASM Support

Add WASM target configuration and create `wasmJsMain` source set.

**Note**: Both require access to Google Maven Repository (dl.google.com).

### Troubleshooting

#### Build Issues

1. **"Could not resolve..."**: Check internet connection and repository access
2. **"Unresolved reference"**: Clean build: `./gradlew clean build`
3. **JVM version**: Ensure JDK 11+ is installed

#### Runtime Issues

1. **Window doesn't appear**: Check DISPLAY environment variable on Linux
2. **Performance**: Reduce grid size or number of entities
3. **State not updating**: Ensure state flow assignment triggers update

### Contributing

When contributing:

1. Follow Kotlin coding conventions
2. Keep domain logic platform-independent (in commonMain)
3. Test on at least one platform before submitting
4. Update documentation for new features
5. Maintain backward compatibility for save data

#### Updating the Final Credits

The final credits screen is shown after winning "The Final Stand" (the last level).
Its content is defined in `composeApp/src/commonMain/kotlin/de/egril/defender/ui/FinalCreditsData.kt`.

**When a new human developer commits code:**
- Add their name to `FinalCreditsData.developers`.
- Bot accounts (names ending in `[bot]`) are excluded by convention.

**When a contributor should be credited** (testers, designers, non-committing helpers):
- Add their name to `FinalCreditsData.contributors`.
- The Contributors section is only shown in the credits when this list is non-empty.

**When new sound effects are added** (with new Freesound.org credits in `composeResources/files/sounds/README.md`):
- Add a `SoundCreditEntry` to `FinalCreditsData.soundEffectsCredits`.

**When new background music is added** (with new credits in `composeResources/files/sounds/background/README.md`):
- Add a `SoundCreditEntry` to `FinalCreditsData.backgroundMusicCredits`.

**When new third-party software or tools are used** (e.g. AI image generators):
- Add a `SoftwareCreditEntry` to `FinalCreditsData.softwareCredits`.
- Also update `composeResources/drawable/README.MD` if images were generated with the tool.

**When a special acknowledgement is warranted** (e.g. open-source libraries, tutorials):
- Add a `SpecialThanksEntry` to `FinalCreditsData.specialThanks`.

**When new drawable images are added** (without `emoji_` or `tile_` prefixes):
- Add the resource name (without file extension, hyphens replaced by underscores) to `FinalCreditsData.backgroundImageNames`.
- Also add the corresponding `when` branch in `drawableResourceByName()` inside `FinalCreditsScreen.kt`. The branch maps the string name to the Compose `DrawableResource`. Example:
  ```kotlin
  // In FinalCreditsScreen.kt → drawableResourceByName()
  "my_new_image" -> Res.drawable.my_new_image
  ```

The `FinalCreditsDataTest` suite validates all the above invariants automatically – run it to verify:
```bash
./gradlew :composeApp:desktopTest --tests "de.egril.defender.ui.FinalCreditsDataTest"
```

You can test the credits screen directly using the cheat code **credits** on the world map screen (click the title to enter cheat codes).

### License

GNU Affero General Public License - See LICENSE file for details.
