# Spell System Implementation Plan

## Overview
This document outlines the phased implementation of the spell casting system for Defender of Egril. The spell system allows players to spend mana on powerful magical effects during gameplay.

---

## Phase 1: Wizard Mana Generation ✅ IMPLEMENTING NOW
**Estimated Time: ~30 minutes**
**Status: In Progress**

### Implementation Steps:
1. Add `GENERATE_MANA` to `WizardAction` enum in `MineAction.kt`
2. Create `GenerateManaButton` composable in `DefenderInfo.kt`
3. Add mana generation action button to wizard tower action area
4. Implement mana generation logic in `TowerManager.kt`
5. Update `GameViewModel` to handle wizard mana generation

### Mana Generation Mechanics:
- **Action Cost**: 1 action per turn
- **Mana Generated**: Base 5 mana + (wizard level / 5) bonus mana
  - Level 1-4: 5 mana
  - Level 5-9: 6 mana  
  - Level 10-14: 7 mana
  - Level 15-19: 8 mana
  - etc.
- **UI**: Button shows "Generate Mana" with mana icon and amount to be generated
- **Validation**: Requires wizard to be ready, have actions remaining, and not at max mana

### Tests Required:
- `testWizardGeneratesMana()` - Verify mana generation amount
- `testWizardManaGenerationRequiresAction()` - Verify action consumption
- `testWizardCannotGenerateAtMaxMana()` - Verify max mana cap
- `testWizardManaGenerationScalesWithLevel()` - Verify level scaling

### Files to Modify:
- `composeApp/src/commonMain/kotlin/de/egril/defender/model/MineAction.kt` - Add GENERATE_MANA action
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/DefenderInfo.kt` - Add GenerateManaButton
- `composeApp/src/commonMain/kotlin/de/egril/defender/game/TowerManager.kt` - Add mana generation logic
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/GameViewModel.kt` - Handle wizard action
- `composeApp/src/commonTest/kotlin/de/egril/defender/game/WizardManaGenerationTest.kt` - New test file

---

## Phase 2: Magic Panel UI
**Estimated Time: ~2-3 hours**
**Status: Not Started**

### Implementation Steps:
1. Create `MagicPanel.kt` composable component
2. Add click handler to mana display in `GameHeader.kt`
3. Create spell list UI with icons and cast buttons
4. Implement mana cost validation
5. Add spell confirmation dialog
6. Add localization strings for magic panel

### Magic Panel Features:
- **Display Location**: Replaces tower info panel when active
- **Trigger**: Click on mana display in header
- **Content**:
  - Scrollable list of unlocked spells
  - Each spell shows: icon, name, mana cost, description, cast button
  - Cast button disabled if insufficient mana
  - Clicking cast enters spell targeting mode

### UI Components:
- **Spell Card**: Icon + Name + Mana Cost + Description + Cast Button
- **Confirmation Dialog**: "Cast [Spell Name]? Cost: X mana. Remaining: Y mana."
- **Targeting Overlay**: Highlights valid targets based on spell type

### Tests Required:
- `testMagicPanelShowsUnlockedSpells()` - Verify only unlocked spells appear
- `testMagicPanelCastButtonDisabledWithoutMana()` - Verify mana validation
- `testMagicPanelOpensOnManaClick()` - Verify UI trigger

### Files to Create:
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/MagicPanel.kt` - Magic panel UI

### Files to Modify:
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameHeader.kt` - Add click handler
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GamePlayScreen.kt` - Show magic panel
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/GameViewModel.kt` - Magic panel state management
- `composeApp/src/commonMain/composeResources/values/strings.xml` - Add spell strings (×5 languages)

---

## Phase 3: Spell Targeting System
**Estimated Time: ~2-3 hours**
**Status: Not Started**

### Implementation Steps:
1. Create `SpellTargetingMode` sealed class in `GameState.kt`
2. Add target validation logic for each spell type
3. Implement targeting overlay on game map
4. Add click handlers for different target types (position, enemy, tower)
5. Add visual feedback for valid/invalid targets

### Targeting Types:
- **POSITION** - Click any valid map tile (Attack Area, Bomb, Cooling Spell)
- **ENEMY** - Click any enemy unit (Attack Aimed, Freeze Spell)
- **TOWER** - Click any friendly tower (Double Tower Level, Double Tower Reach)
- **SELF** - No targeting needed (Heal, Instant Tower)

### Visual Feedback:
- **Valid Targets**: Highlighted with green border
- **Invalid Targets**: Grayed out
- **Selected Target**: Yellow highlight
- **Cancel**: ESC key or click outside map

### Tests Required:
- `testSpellTargetingValidation()` - Verify target type validation
- `testSpellTargetingCancellation()` - Verify cancel behavior
- `testSpellTargetingVisualFeedback()` - Verify UI indicators

### Files to Modify:
- `composeApp/src/commonMain/kotlin/de/egril/defender/model/GameState.kt` - Add targeting state
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameMap.kt` - Add targeting overlay
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/GameViewModel.kt` - Targeting logic

---

## Phase 4: Spell Effects Implementation
**Estimated Time: ~6-10 hours total**
**Status: Not Started**

### Spell 1: Attack Area (AREA_ATTACK)
**Time: ~45 minutes**

- **Mana Cost**: 10
- **Effect**: Deal 50 damage to all enemies in a 2-tile radius
- **Target**: Position on map
- **Implementation**: Calculate all positions within radius, damage all enemies at those positions
- **Tests**: 
  - `testAttackAreaDamagesEnemiesInRadius()`
  - `testAttackAreaIgnoresEnemiesOutsideRadius()`

---

### Spell 2: Attack Aimed (AIMED_ATTACK)
**Time: ~30 minutes**

- **Mana Cost**: 5
- **Effect**: Deal 80 damage to single enemy
- **Target**: Enemy unit
- **Implementation**: Direct damage to selected enemy
- **Tests**:
  - `testAttackAimedDamagesSingleEnemy()`
  - `testAttackAimedDoesNotAffectOtherEnemies()`

---

### Spell 3: Heal (HEAL_SELF)
**Time: ~30 minutes**

- **Mana Cost**: 15
- **Effect**: Restore 3 health points
- **Target**: Self (no targeting)
- **Implementation**: Increase GameState.health by 3, cap at max health
- **Tests**:
  - `testHealRestoresHealth()`
  - `testHealDoesNotExceedMaxHealth()`

---

### Spell 4: Instant Tower (INSTANT_TOWER)
**Time: ~45 minutes**

- **Mana Cost**: 20
- **Effect**: Skip build time for next tower placement (one tower only)
- **Target**: Self (no targeting)
- **Implementation**: Add `instantTowerAvailable` flag to GameState, clear after one tower placement
- **Tests**:
  - `testInstantTowerSkipsBuildTime()`
  - `testInstantTowerOnlyAffectsNextTower()`

---

### Spell 5: Bomb (BOMB)
**Time: ~2 hours**

- **Mana Cost**: 25
- **Effect**: Place bomb that ticks down 2 turns, explodes on turn 3
  - Explosion damages all enemies in 2-tile radius (100 damage)
  - Destroys barricades in radius (50% HP damage)
  - Destroys bridges in radius
- **Target**: Position on map (path or build area)
- **Implementation**: 
  - Add `Bomb` data class with position, turnsRemaining
  - Add bombs list to GameState
  - Tick down bombs each turn
  - Explode when turnsRemaining = 0
- **Tests**:
  - `testBombTicksDownOverTurns()`
  - `testBombExplodesAfter3Turns()`
  - `testBombDamagesEnemiesInRadius()`
  - `testBombDamagesBarricades()`
  - `testBombDestroysBridges()`

---

### Spell 6: Double Tower Level (DOUBLE_TOWER_LEVEL)
**Time: ~1 hour**

- **Mana Cost**: 30
- **Effect**: Double tower's effective level for one turn (affects damage and range)
- **Target**: Friendly tower
- **Implementation**:
  - Add `doubledLevelTurnsRemaining` to Defender
  - Modify damage/range calculations when doubled
  - Decrement at end of turn
- **Tests**:
  - `testDoubleTowerLevelDoublesDamage()`
  - `testDoubleTowerLevelDoublesRange()`
  - `testDoubleTowerLevelLastsOneTurn()`

---

### Spell 7: Cooling Spell (COOLING_AREA)
**Time: ~1 hour**

- **Mana Cost**: 20
- **Effect**: Create area (3-tile radius) where enemies lose 1 movement point for 3 turns
- **Target**: Position on map
- **Implementation**:
  - Add `CoolingArea` data class with position, radius, turnsRemaining
  - Add coolingAreas list to GameState
  - Check if enemy is in cooling area during movement
  - Reduce enemy speed by 1 (min 0) for that turn
- **Tests**:
  - `testCoolingAreaSlowsEnemies()`
  - `testCoolingAreaLastsThreeTurns()`
  - `testCoolingAreaDoesNotReduceSpeedBelowZero()`

---

### Spell 8: Freeze Spell (FREEZE_ENEMY)
**Time: ~1.5 hours**

- **Mana Cost**: 10 per turn (player chooses duration)
- **Effect**: Freeze enemy for X turns (player buys turns with mana)
  - Does NOT work on Demons, Dragons, or Ewhad
- **Target**: Enemy unit
- **Implementation**:
  - Add duration selection UI (slider/stepper)
  - Add `frozenTurnsRemaining` to Attacker
  - Skip frozen enemies during movement phase
  - Show freeze visual indicator
- **Tests**:
  - `testFreezeStopsEnemyMovement()`
  - `testFreezeLastsSpecifiedDuration()`
  - `testFreezeDoesNotAffectDemonsOrDragons()`
  - `testFreezeManaCostScalesWithDuration()`

---

### Spell 9: Double Tower Reach (DOUBLE_TOWER_REACH)
**Time: ~1 hour**

- **Mana Cost**: 25
- **Effect**: Double tower's attack range for one turn
- **Target**: Friendly tower
- **Implementation**:
  - Add `doubledRangeTurnsRemaining` to Defender
  - Multiply range by 2 when calculating attacks
  - Decrement at end of turn
- **Tests**:
  - `testDoubleTowerReachDoublesRange()`
  - `testDoubleTowerReachLastsOneTurn()`
  - `testDoubleTowerReachDoesNotAffectDamage()`

---

## Phase 5: Integration & Polish
**Estimated Time: ~2 hours**
**Status: Not Started**

### Implementation Steps:
1. Full spell system integration testing
2. Add spell casting animations/visual effects
3. Add sound effects for spell casting
4. Balance testing and mana cost adjustments
5. Complete localization for all spell strings
6. Update tutorial/help system
7. Add spell achievements if needed

### Tests Required:
- `testSpellSystemFullIntegration()` - End-to-end spell casting
- `testMultipleSpellsCastInSequence()` - Verify mana deduction
- `testSpellEffectsDoNotPersistAcrossLevels()` - Verify cleanup

---

## Summary

### Total Estimated Time: **10-15 hours**

### Phase Breakdown:
- ✅ Phase 1: Wizard Mana Generation - **30 min** (IN PROGRESS)
- Phase 2: Magic Panel UI - **2-3 hours**
- Phase 3: Spell Targeting System - **2-3 hours**
- Phase 4: Spell Effects (9 spells) - **6-10 hours**
  - Simple spells (2, 3): ~1 hour total
  - Medium spells (1, 4, 6, 7, 9): ~4 hours total
  - Complex spells (5, 8): ~3.5 hours total
- Phase 5: Integration & Polish - **2 hours**

### Test Coverage:
- **Phase 1**: 4 tests (mana generation)
- **Phase 2**: 3 tests (magic panel UI)
- **Phase 3**: 3 tests (targeting system)
- **Phase 4**: ~25 tests (spell effects)
- **Phase 5**: 3 tests (integration)

**Total Tests**: ~38 comprehensive tests

---

## Current Status

**Completed**:
- XP system with stats and upgrades ✅
- Mana stat and display ✅
- Construction level gating ✅
- Player progression achievements ✅
- Full localization (5 languages) ✅

**In Progress**:
- Phase 1: Wizard Mana Generation 🔄

**Not Started**:
- Phases 2-5 (Magic Panel, Targeting, Spells, Integration)

---

## Notes

- Each spell requires dedicated implementation, testing, and localization
- Spell system is complex due to multiple targeting types and effect durations
- Visual feedback and UI polish will take additional time
- Localization adds ~30-50 strings × 5 languages = 150-250 translation entries
- Sound effects and animations not included in time estimates

This is a substantial feature that should be developed incrementally with thorough testing at each phase.
