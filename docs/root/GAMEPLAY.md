# Defender of Egril - Gameplay Guide

## How to Start the Game

### Desktop (All Platforms)
```bash
./gradlew :composeApp:run
```

On Windows:
```cmd
gradlew.bat :composeApp:run
```

### Android
1. Build and install the APK:
   ```bash
   ./gradlew :composeApp:installDebug
   ```
2. Or transfer the APK from `composeApp/build/outputs/apk/debug/` to your device

### iOS (requires macOS with Xcode)
1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select your simulator or device
3. Click Run (⌘R)

---

## Game Overview

**Defender of Egril** is a turn-based tower defense game where you defend the meadows of Egril against waves of enemies under the evil banner of Ewhad.

The game features a **single-phase turn system** where you control both tower placement and attacks during your turn, while enemies move during their separate turn.

---

## How to Play

### Tutorial Level

**New players should start with the tutorial level!**

The first level "Welcome to Defender of Egril" is a guided tutorial that teaches you:
- **Resources**: Understanding coins and health points
- **Tower Placement**: How to build defensive towers
- **Tower Types**: Learning about Spike, Spear, and Bow towers
- **Enemy Behavior**: How goblins and orks attack
- **Combat System**: Starting turns and defeating enemies

The tutorial:
- Uses a small, easy-to-understand map (15x8 grid)
- Features only 5 goblins and 1 ork
- Provides step-by-step guidance with overlay messages
- Can be skipped if you're already familiar with tower defense games
- Unlocks all advanced levels after completion

**Tip:** Even experienced tower defense players should try the tutorial to learn Defender of Egril's unique hexagonal grid and turn-based mechanics!

---

### Main Menu
- Click **"Start Game"** to view the World Map

### World Map
- Shows all available levels
- 🔒 **Locked**: Complete previous levels to unlock
- ⚔️ **Available**: Ready to play
- ✓ **Completed**: Already won
- Click on an unlocked level to start

---

## Level Gameplay

### Phase 1: Initial Building Phase
**At the start of each level, you get a special building phase:**

- Place towers **instantly** (no build time)
- Use your starting coins strategically
- Towers are ready to attack immediately
- Click **"Start Battle"** when ready

**Tips for Initial Building:**
- Block the enemy path with long-range towers
- Save some coins for later
- Mix different tower types for better coverage

### Phase 2: Player Turn
**During your turn, you can:**

1. **Place New Towers**
   - Select a tower type from the buttons
   - Click an empty cell to place
   - **Important**: Newly placed towers need build time before they can attack!
   - Build time is shown as ⏱ with a number

2. **Attack Enemies**
   - Click on a ready tower (shows ⚡ with action count)
   - Click on an enemy in range to attack
   - Each tower has limited actions per turn
   - You get coins immediately when enemies are defeated

3. **Upgrade Towers**
   - Click on a tower to select it
   - Click "Upgrade" button to improve damage and range
   - Costs increase with each level

4. **End Your Turn**
   - Click **"End Turn"** when done
   - Enemy turn begins automatically

### Phase 3: Enemy Turn
- Enemies move toward the target
- New enemies spawn
- Damage-over-time effects are applied
- Build timers advance (⏱ counts down)
- Your turn begins automatically after

---

## Tower Types

| Tower | Cost | Build Time | Damage | Range | Type | Actions/Turn |
|-------|------|------------|--------|-------|------|--------------|
| **Spike Tower** | 10 | 1 turn | 5 | 1 | Melee | 1* |
| **Spear Tower** | 15 | 1 turn | 8 | 2 | Ranged | 1 |
| **Bow Tower** | 20 | 1 turn | 10 | 3 | Ranged | 1 |
| **Wizard Tower** | 50 | 2 turns | 30 | 3 | AOE | 1 |
| **Alchemy Tower** | 40 | 1 turn | 15 | 2 | DoT | 1 |

*Spike Tower (Pike Tower) has special upgrade mechanics - see below.

### Tower Abilities

- **Melee/Ranged**: Attacks single target
- **AOE (Area of Effect)**: Damages primary target and all enemies within 1 cell
- **DoT (Damage over Time)**: Initial damage + continuous damage for 3 more turns

### Upgrading
- Each upgrade costs: Base Cost × Current Level
- Damage increases by +5 per level
- Range increases by +0.5 (rounded down) every 2 levels

### Spike Tower (Pike Tower) Special Mechanics
The Spike Tower has unique upgrade characteristics:
- **Range Cap**: Maximum range is capped at 2 when level 5 or higher
- **Action Scaling**: Gains +1 action per turn for every 5 levels (maximum 3 actions)
  - Levels 1-4: 1 action per turn
  - Levels 5-9: 2 actions per turn
  - Levels 10+: 3 actions per turn (maximum)

---

## Enemy Types

| Enemy | HP | Speed | Base Reward |
|-------|----|----|--------|
| **Goblin** | 20 | 2 | 5 coins × level |
| **Skeleton** | 15 | 2 | 7 coins × level |
| **Ork** | 40 | 1 | 10 coins × level |
| **Witch** | 25 | 2 | 12 coins × level |
| **Evil Wizard** | 30 | 1 | 15 coins × level |
| **Ogre** | 80 | 1 | 20 coins × level |

**Note:** Enemy rewards are multiplied by their level. For example, a level 3 Goblin gives 15 coins (5 × 3), and a level 2 Ork gives 20 coins (10 × 2).

---

## Grid Legend

- **S**: Start position (enemies spawn here)
- **T**: Target position (don't let enemies reach!)
- **Blue**: Your ready towers
- **Gray**: Towers still building
- **Red**: Enemies
- **⏱**: Build time remaining
- **⚡**: Actions remaining this turn
- **Numbers**: Health (for enemies) or Level (for towers)

---

## Winning and Losing

### Victory Conditions
- Defeat all enemies in all waves
- At least 1 health point remaining

### Defeat Conditions
- All health points lost (0 HP)
- **Variable Damage:** Different enemies cause different damage when reaching the target:
  - **Weak enemies** (Goblin, Ork, Ogre, Skeleton): 1 HP each
  - **Mighty enemies** (Evil Wizard, Witch, Red Witch, Green Witch, Evil Mage, Blue Demon, Red Demon, Dragon): HP equal to their level
  - **Ewhad (Boss)**: All remaining HP (instant defeat)

---

## Strategic Tips

### Early Game
1. Use the initial building phase wisely
2. Place towers to cover the enemy path
3. Save 20-30 coins for mid-game

### Mid Game
1. Build towers during battle to get coins → build more towers
2. Focus fire on tough enemies (Ogres, Orks)
3. Upgrade key towers for better efficiency

### Late Game
1. Wizard Towers for massive AOE damage
2. Alchemy Towers for continuous damage
3. Upgrade high-level towers rather than building new ones

### Advanced Tactics
- **Build Time Strategy**: Place towers early in your turn so they're ready sooner
- **Coin Efficiency**: Defeat weak enemies to get coins for strong towers
- **Action Management**: Use all tower actions before ending turn
- **Range Optimization**: Upgrade range to cover more path
- **AOE Positioning**: Place Wizard Towers where enemies group up

---

## Example Turn Flow

1. **Your Turn Begins**
   - All ready towers get 1 action
   - Build timers advance

2. **You Place a Bow Tower**
   - Costs 20 coins
   - Starts with ⏱1 (ready next turn)

3. **You Attack with Existing Towers**
   - Select tower with ⚡1
   - Click enemy to attack
   - Enemy defeated → coins based on enemy type and level (e.g., level 1 Goblin = 5 coins, level 2 Goblin = 10 coins)

4. **You Build Another Tower**
   - Use coins from defeated enemy
   - New tower also starts building

5. **Click "End Turn"**
   - Enemy turn: enemies move
   - Build timers count down
   - Your turn starts again

---

## Keyboard Shortcuts

Currently all controls are mouse/touch based:
- **Click** to select towers/enemies
- **Click** tower buttons to build
- **Click** "End Turn" to finish

---

## Troubleshooting

**Towers not attacking?**
- Check if tower is ready (no ⏱ symbol)
- Check if tower has actions (⚡ symbol)
- Check if enemy is in range

**Can't place tower?**
- Check if you have enough coins
- Position may be occupied
- Can't place on Start (S) or Target (T)

**Lost the level?**
- Click "Retry" to try again
- Try different tower placements
- Focus on high-value targets first

---

## Level Progression

Complete levels to unlock new ones:
1. **Welcome to Defender of Egril** - Tutorial level with 5 Goblins and 1 Ork
   - Learn the basics: tower placement, combat, and strategy
   - Only 3 tower types available: Spike Tower, Spear Tower, Bow Tower
   - Interactive tutorial guides you through your first battle
2. **The First Wave** - 30 Goblins
3. **Mixed Forces** - Goblins, Skeletons, Orks
4. **The Ork Invasion** - More Orks appear
5. **Dark Magic Rises** - Wizards and Witches join
6. **The Final Stand** - All enemy types!
7. **Ewhad's Challenge** - Face the evil Ewhad himself

Each level increases in difficulty with more enemies, tougher types, and less health.

---

## Have Fun!

Defend the meadows of Egril and drive back the evil forces of Ewhad!
