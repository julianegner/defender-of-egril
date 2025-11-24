# Quick Start Guide

## Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/julianegner/defender-of-egril.git
   cd defender-of-egril
   ```

2. **Ensure you have JDK 11 or higher**:
   ```bash
   java -version
   ```

3. **Build the project**:
   ```bash
   ./gradlew build
   ```

4. **Run the game**:
   ```bash
   ./gradlew :composeApp:run
   ```

## Your First Game

### Level 1: The First Wave

1. **Starting Resources**: You begin with 100 coins
2. **Objective**: Defeat 5 Goblins
3. **Strategy**:
   - Place 2-3 **Bow Towers** along the path (20 coins each)
   - Save remaining coins for upgrades
   - Start combat when ready
   - Upgrade towers between waves if needed

### Understanding the Grid

- **S**: Start position (enemies spawn here)
- **T**: Target position (don't let enemies reach this!)
- **Blue cells**: Your towers
- **Red cells**: Enemy units

### Basic Strategy

1. **Planning Phase**:
   - Click a tower type to select it
   - Click empty cells to place towers
   - Click placed towers to see upgrade options
   - Plan your defense before starting combat

2. **Combat Phase**:
   - Click "Next Turn" to advance one turn
   - Towers automatically attack enemies in range
   - Enemies move toward the target each turn
   - Watch the coins counter - you earn coins for each defeated enemy!

3. **Between Phases**:
   - Click "Return to Planning" to place more towers
   - Use earned coins to upgrade or place new towers

### Tower Placement Tips

- **Coverage**: Place towers where they can attack the most path cells
- **Range**: Bow Towers (range 3) are great for starting
- **Choke Points**: Focus fire on areas where enemies travel longest
- **Upgrade vs New**: Sometimes upgrading is better than placing a new tower

### Advanced Tactics

- **Wizard Towers**: Expensive but deal area damage - great for groups
- **Alchemy Towers**: Damage over time - place early in the path for maximum effect
- **Spike Towers**: Cheap melee defense - good for last-minute emergency placement

## Game Progression

Complete Level 1 to unlock Level 2, and so on. Each level introduces:
- More enemies
- Different enemy types
- Tougher challenges
- More strategic placement opportunities

## Keyboard Shortcuts

Currently the game uses mouse-only controls. Future versions may add:
- ESC: Return to menu
- Space: Next turn
- 1-5: Quick select tower types

## Troubleshooting

### Game won't start
- Check that JDK 11+ is installed
- Ensure you have internet access (needed for dependency download)
- Try: `./gradlew clean build`

### Performance issues
- Close other applications
- The game should run smoothly on most modern computers

### Questions or Issues?
Open an issue on the GitHub repository with:
- Your operating system
- JDK version (`java -version`)
- Error message or description of the problem

## Next Steps

Once you've mastered the basics:
1. Try to complete all 5 levels
2. Experiment with different tower combinations
3. Find the most coin-efficient strategies
4. Challenge yourself to minimal tower victories

Happy defending! The meadows of Egril need you! 🏰⚔️
