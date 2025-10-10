# Running Defender of Egril

## Important Note

This game requires access to the Google Maven Repository (dl.google.com) to download Compose Multiplatform dependencies at runtime. If you're in an environment where this domain is blocked, you'll need to use a different network or configure a proxy.

## Running on Desktop

1. Ensure you have JDK 11 or higher installed
2. Navigate to the project root directory
3. Run the following command:

```bash
./gradlew :composeApp:run
```

## Building a Distributable Package

To create a native installer for your platform:

```bash
# For Windows (MSI):
./gradlew :composeApp:packageMsi

# For macOS (DMG):
./gradlew :composeApp:packageDmg

# For Linux (DEB):
./gradlew :composeApp:packageDeb
```

The installer will be created in `composeApp/build/compose/binaries/main/`

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
