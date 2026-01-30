# Spike Tower Barricade Level Requirement Update

## Summary
Updated spike tower barricade requirements from level 10 to level 20, with an adjusted HP formula to balance the higher level requirement.

## Changes Made

### 1. Spike Tower Barricade Requirements
**Before:**
- Minimum level: 10
- HP formula: `level - 10` (minimum 1)
- Button visibility: Level 10+

**After:**
- Minimum level: 20
- HP formula: `(level - 20) / 2` (minimum 1)
- Button visibility: Level 20+

### 2. Spear Tower (Unchanged)
- Minimum level: 10
- HP formula: `level - 10` (minimum 1)
- Button visibility: Level 10+

### 3. Code Changes

#### DefenderInfo.kt
1. **Button visibility logic** (lines 306-323):
   - Changed from checking `defender.level.value >= 10` for both types
   - Now uses type-specific checks:
     - Spike tower: `defender.level.value >= 20`
     - Spear tower: `defender.level.value >= 10`

2. **HP calculation** (lines 579-600):
   - Changed from single formula `maxOf(1, defender.level.value - 10)`
   - Now uses type-specific formula:
     - Spike tower: `maxOf(1, (defender.level.value - 20) / 2)`
     - Spear tower: `maxOf(1, defender.level.value - 10)`

#### String Resources (All Languages)
Updated `barricade_info_message` in:
- `values/strings.xml` (English)
- `values-de/strings.xml` (German)
- `values-es/strings.xml` (Spanish)
- `values-fr/strings.xml` (French)
- `values-it/strings.xml` (Italian)

**Before:**
> "Your Spike or Spear tower has reached level 10 and can now build barricades!
> Each barricade has health points equal to the tower level minus 10 (minimum 1)."

**After:**
> "Your Spear tower has reached level 10 (or Spike tower level 20) and can now build barricades!
> Spear tower barricades have HP = level - 10 (minimum 1). Spike tower barricades have HP = (level - 20) / 2 (minimum 1)."

## HP Comparison Table

| Tower Level | Spike Tower HP (Old) | Spike Tower HP (New) | Spear Tower HP |
|-------------|---------------------|---------------------|----------------|
| 10          | 0 → 1 (min)         | N/A (not unlocked)  | 0 → 1 (min)    |
| 15          | 5                   | N/A (not unlocked)  | 5              |
| 20          | 10                  | 0 → 1 (min)         | 10             |
| 22          | 12                  | 1                   | 12             |
| 24          | 14                  | 2                   | 14             |
| 30          | 20                  | 5                   | 20             |
| 40          | 30                  | 10                  | 30             |
| 50          | 40                  | 15                  | 40             |

## Rationale
The spike tower barricade was considered too powerful when unlocked at level 10, especially given:
1. Spike towers are the cheapest and most basic tower (10 coins)
2. Early access to barricades could trivialize early game difficulty
3. Spear towers (15 coins) are meant to be the primary barricade-building tower

The new level 20 requirement for spike towers:
- Maintains spear tower as the primary barricade builder (level 10-19)
- Creates a meaningful progression unlock for spike towers
- Reduces HP output at high levels to prevent excessive barricade spam

## Testing
- ✅ All BarricadeSystemTest tests pass
- ✅ Build completes successfully
- ✅ String resources validated in all 5 languages

## Backward Compatibility
This is a **balance change** that affects gameplay:
- Existing spike towers below level 20 will lose barricade ability
- HP values for spike tower barricades at level 20+ will be lower
- Does not affect save file format or data structures
