# Tutorial Level - Implementation Details

## Overview

The tutorial level "Welcome to Defender of Egril" is an interactive first level designed to teach new players the core mechanics of the game through step-by-step guidance with overlay dialogs.

## Tutorial Map

**Size:** 15 columns × 8 rows (smaller than standard 30×8)

**Features:**
- Single spawn point at position (0, 4)
- Simple path with slight curves
- Target at position (14, 4)
- Two 2×2 build islands for strategic placement
- Abundant build areas adjacent to the path

**Why this design?**
- Small size reduces complexity for new players
- Clear, easy-to-follow path
- Strategic islands teach advanced placement concepts
- Sufficient build space to experiment

## Enemy Configuration

**Total Enemies:** 6
- 5 Goblins (spawn on turns 1-5)
- 1 Ork (spawns on turn 8)

**Why this composition?**
- Goblins are fast but weak - teach basic tower placement
- Ork is slow but tough - demonstrates the need for stronger/upgraded towers
- Gap between goblin waves and ork gives player time to build/upgrade
- Total rewards: 5×5 coins (goblins) + 10 coins (ork) = 35 coins earned during combat

## Starting Resources

- **60 coins** - Enough to build:
  - 6 Spike Towers (10 coins each)
  - 4 Spear Towers (15 coins each)
  - 3 Bow Towers (20 coins each)
  - Or any combination
- **10 health points** - Standard starting health

## Available Towers

Only 3 basic tower types are available:

1. **Spike Tower (Pike)**
   - Cost: 10 coins
   - Range: 1 (close combat)
   - Attack Type: Melee
   - Best for: Early game, cheap defense

2. **Spear Tower**
   - Cost: 15 coins
   - Range: 2 (medium)
   - Attack Type: Ranged
   - Best for: Balanced offense

3. **Bow Tower**
   - Cost: 20 coins
   - Range: 3 (long)
   - Attack Type: Ranged
   - Best for: Long-range coverage

**Why only 3 towers?**
- Reduces decision paralysis for new players
- Teaches fundamental tower defense concepts
- More complex towers (Wizard, Alchemy, Ballista, Mine, Dragon's Lair) are introduced in later levels

## Tutorial Steps

### 1. Welcome (TutorialStep.WELCOME)
**Title:** "Welcome to Defender of Egril!"
**Message:** "This is a tower defense game. Your goal is to stop enemies from reaching the target by building defensive towers."
**Actions:** 
- Next button advances
- Skip button available

### 2. Resources (TutorialStep.RESOURCES)
**Title:** "Resources"
**Message:** "You have coins to build towers and health points. Each enemy reaching the target costs 1 HP."
**Actions:**
- Next button advances
- Skip button available

### 3. Build Tower (TutorialStep.BUILD_TOWER)
**Title:** "Building Towers"
**Message:** "Select a tower type, then click on a green build area to place it. Try placing a Spike Tower!"
**Actions:**
- Next button advances
- Skip button available
- **Gating:** Won't advance past this step until player places at least one tower

### 4. Tower Types (TutorialStep.TOWER_TYPES)
**Title:** "Available Towers"
**Message:** 
```
Spike Tower: Close range, cheap
Spear Tower: Medium range
Bow Tower: Long range
```
**Actions:**
- Next button advances
- No skip button (too far into tutorial)

### 5. Enemies Incoming (TutorialStep.ENEMIES_INCOMING)
**Title:** "Enemies Incoming!"
**Message:** "Goblins are fast but weak. The Ork is slow but tough. Position your towers wisely!"
**Actions:**
- Next button advances
- No skip button

### 6. Start Combat (TutorialStep.START_COMBAT)
**Title:** "Combat Phase"
**Message:** "Click 'Start Turn' to begin. Towers will attack, then enemies move. You can upgrade towers and attack specific enemies."
**Actions:**
- Next button advances
- No skip button
- **Gating:** Won't advance past this step until player starts their first turn

### 7. Complete (TutorialStep.COMPLETE)
**Title:** "You're Ready!"
**Message:** "You've learned the basics! Now defend Egril against the hordes. Good luck!"
**Actions:**
- "Got it!" button dismisses and tutorial ends

## Technical Implementation

### Files Modified/Created

1. **TutorialState.kt** - State management for tutorial progression
2. **TutorialOverlay.kt** - UI component for tutorial dialogs
3. **GameState.kt** - Added tutorialState field
4. **GamePlayScreen.kt** - Integrated tutorial overlay and progress tracking
5. **EditorStorage.kt** - Added tutorial map and level, incremented version
6. **strings.xml** (×5 languages) - Tutorial text strings

### Tutorial Detection

Tutorial is activated when:
```kotlin
level.id == 1 && level.name.contains("Welcome", ignoreCase = true)
```

This ensures:
- Only the first level triggers tutorial
- Level name clearly identifies it
- Other levels are unaffected

### Progress Tracking

Tutorial tracks two key player actions:
1. **hasPlacedFirstTower** - Set when player places any tower
2. **hasStartedFirstTurn** - Set when player clicks "Start Turn" or "End Turn"

These flags gate progression through BUILD_TOWER and START_COMBAT steps respectively.

### Skip Functionality

Players can skip the tutorial at any point during the first 3 steps:
- WELCOME
- RESOURCES  
- BUILD_TOWER

After that, skip is removed since:
- Player has already invested time
- Remaining steps are brief
- Player needs to complete the level anyway

## User Experience Flow

```
┌─────────────────────────────────────────────┐
│ Player clicks on Level 1 "Welcome to...    │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ Level loads, tutorial auto-detects         │
│ TutorialState.isActive = true              │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ Step 1: WELCOME overlay appears            │
│ Player reads, clicks "Next" or "Skip"      │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ Step 2: RESOURCES overlay                  │
│ Explains coins and health                  │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ Step 3: BUILD_TOWER overlay                │
│ Player must place a tower to continue      │
└─────────────────────────────────────────────┘
                    ↓
        [Player places tower]
                    ↓
┌─────────────────────────────────────────────┐
│ Step 4: TOWER_TYPES overlay                │
│ Explains the 3 available tower types       │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ Step 5: ENEMIES_INCOMING overlay           │
│ Describes goblin and ork behavior          │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ Step 6: START_COMBAT overlay               │
│ Player must start turn to continue         │
└─────────────────────────────────────────────┘
                    ↓
      [Player clicks "Start Turn"]
                    ↓
┌─────────────────────────────────────────────┐
│ Step 7: COMPLETE overlay                   │
│ Congratulations message                    │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ Tutorial ends, player continues level      │
│ TutorialState.isActive = false             │
└─────────────────────────────────────────────┘
```

## Localization

Tutorial strings are fully localized in 5 languages:
- English (default)
- German (Deutsch)
- Spanish (Español)
- French (Français)
- Italian (Italiano)

All strings use the `stringResource(Res.string.tutorial_*)` system for automatic language selection based on user settings.

## Testing

Comprehensive unit tests in `TutorialStateTest.kt`:
- 11 test cases covering all state transitions
- Tests for gating logic (tower placement, turn start)
- Tests for skip functionality
- Tests for step progression
- All tests passing ✓

## Design Decisions

### Why overlays instead of tooltips?
- More prominent and harder to miss
- Forces player attention on important information
- Works well on all screen sizes
- Can't be accidentally dismissed

### Why block progression at certain steps?
- Ensures player actually tries the mechanic
- Prevents "next-next-next" clicking through
- Provides hands-on learning experience

### Why only 6 enemies?
- Quick tutorial (5-10 minutes)
- Enough to learn mechanics
- Not overwhelming for new players
- Sets up expectations for longer levels

### Why limit to 3 tower types?
- Reduces cognitive load
- Teaches fundamentals first
- More advanced towers require understanding basic concepts
- Matches common tower defense progression

## Future Enhancements

Potential improvements (not in scope for current implementation):
- Highlight specific UI elements during relevant steps
- Animated arrows pointing to buttons/areas
- In-game tooltips that persist after tutorial
- Optional tutorial replay from settings
- Tutorial hints in later levels for advanced mechanics
- Achievement for completing tutorial without skipping

## Summary

The tutorial level successfully:
✓ Teaches core game mechanics step-by-step
✓ Uses a small, manageable map for learning
✓ Limits complexity with only 3 tower types
✓ Gates progression to ensure engagement
✓ Provides skip option for experienced players
✓ Is fully localized in 5 languages
✓ Has comprehensive test coverage
✓ Integrates seamlessly with existing game flow
