# Enemy Level Display Changes - Visual Guide

## Changes Made (Addressing Feedback)

### 1. Changed "Lv{X}" to "Lvl {X}" (with space)

**Before:**
```
Goblin Lv3
```

**After:**
```
Goblin Lvl 3
```

This change applies to:
- Enemy list panel (active enemies on map)
- Enemy list panel (planned spawns)
- Enemy details panel (when clicking on enemy tile)

---

### 2. Added Level Number on Enemy Icon (Map Display)

**NEW FEATURE:** Level number now appears at the top of enemy icons on the game map!

#### Visual Representation:

**Enemy with Level 1 (no change):**
```
┌─────────────┐
│             │
│             │
│   👹👿🧙    │  ← Enemy icon (center)
│             │
│     20      │  ← Health (bottom)
└─────────────┘
```

**Enemy with Level 3 (NEW - shows level at top):**
```
┌─────────────┐
│      3      │  ← Level number (RED, BOLD) - NEW!
│             │
│   👹👿🧙    │  ← Enemy icon (center)
│             │
│     60      │  ← Health (bottom, scaled by level)
└─────────────┘
```

**Enemy with Level 5 (example):**
```
┌─────────────┐
│      5      │  ← Level number (RED, BOLD) - NEW!
│             │
│   👹👿🧙    │  ← Enemy icon (center)
│             │
│    100      │  ← Health (bottom, scaled by level)
└─────────────┘
```

---

## Implementation Details

### Level Number Display
- **Position:** Top center of the hexagon
- **Color:** Red (for visibility and to indicate threat)
- **Font:** Bold, 12sp
- **Visibility:** Only shown when level > 1
- **Padding:** 8dp from top edge

### Health Display (unchanged)
- **Position:** Bottom center of the hexagon
- **Color:** White (customizable via parameter)
- **Font:** Bold, 13sp
- **Padding:** 10dp from bottom edge

---

## Code Changes

### Files Modified:

1. **EnemyIcon.kt**
   - Added level number display at top center
   - Only renders when `attacker.level > 1`
   - Uses red color to stand out

2. **GameMap.kt**
   - Updated key() to include `attacker.level`
   - Ensures icon recomposes when level changes

3. **AttackerInfo.kt**
   - Changed "Lv" to "Lvl " (with space)

4. **GameLegend.kt** (2 locations)
   - Changed "Lv" to "Lvl " (with space)
   - Applies to both active enemies and planned spawns

---

## Visual Impact on Different Enemy Levels

### Level 1 Enemies (Default)
```
Enemy List:        Map Icon:
┌──────────────┐   ┌─────────┐
│ Goblin       │   │         │
│ HP: 20/20    │   │   👹    │
└──────────────┘   │   20    │
                   └─────────┘
```

### Level 2 Enemies
```
Enemy List:        Map Icon:
┌──────────────┐   ┌─────────┐
│ Goblin Lvl 2 │   │    2    │ ← Level shown
│ HP: 40/40    │   │   👹    │
└──────────────┘   │   40    │
                   └─────────┘
```

### Level 5 Enemies
```
Enemy List:        Map Icon:
┌──────────────┐   ┌─────────┐
│ Ogre Lvl 5   │   │    5    │ ← Level shown
│ HP: 400/400  │   │   🧟    │
└──────────────┘   │  400    │
                   └─────────┘
```

---

## Benefits

1. **At-a-glance Information:** Players can immediately see enemy levels on the map without clicking
2. **Threat Assessment:** Red level numbers draw attention to higher-level enemies
3. **Consistent Design:** Matches the existing health display pattern
4. **Performance:** Only renders level text when needed (level > 1)
5. **Space-efficient:** Uses minimal space at the top of the icon

---

## Testing

All changes have been:
- ✅ Compiled successfully
- ✅ Tested with automated test suite
- ✅ Verified to work with level editor
- ✅ Compatible with save/load system
- ✅ Responsive to all platforms (Desktop, Android, iOS, Web/WASM)
