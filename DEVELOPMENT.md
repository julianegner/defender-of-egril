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

#### Adding a New Level

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
- **spawn <type> <level>**: Spawns an enemy of the specified type and level
  - Types: goblin, ork, ogre, skeleton, wizard, witch
  - Level: optional integer (default 1) that scales enemy health
  - Examples: `spawn goblin 2`, `spawn ogre 5`

**World Map Cheat Codes** (accessed by clicking the "World Map - Meadows of Egril" title):
- **unlock**, **unlockall**, **unlock all**: Unlocks all levels for testing

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

### License

GNU Affero General Public License - See LICENSE file for details.
