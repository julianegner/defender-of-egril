# Defender of Egril

Defend the meadows of Egril against the Hordes of Gleid Thyae under the Banner of the evil Ewhad.

## Game Description

Defender of Egril is a turn-based tower defense game built with Kotlin Multiplatform and Compose Multiplatform.

### For Players

Defend the meadows of Egril against waves of attackers including goblins, orks, ogres, skeletons, evil wizards, and witches.

### Game Mechanics

- **Turn-Based Gameplay**: Strategic tower placement and upgrades between combat phases
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

## Building and Running

### Prerequisites

- JDK 11 or higher
- Gradle 8.9 (included via wrapper)

### Supported Platforms

Currently supporting:
- **JVM/Desktop** (primary platform)

**Note**: Android and WASM targets are configured but require access to Google Maven Repository (dl.google.com) which may be blocked in some environments.

### Building

```bash
./gradlew build
```

### Running Desktop Version

```bash
./gradlew :composeApp:run
```

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

MIT License - See LICENSE file for details

## Development

This game is built using:
- Kotlin 2.0.21
- Compose Multiplatform 1.7.0
- Kotlin Multiplatform for cross-platform support
