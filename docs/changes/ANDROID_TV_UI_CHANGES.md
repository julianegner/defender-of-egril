# Android TV UI Visual Changes

## Overview
This document describes the visual changes made to support Android TV UI in the Defender of Egril game.

## Button Selection Visual Distinction

### Before (Regular Android/Non-TV)
When a tower button is selected on regular Android devices or other platforms:
- Button background color changes to a darker blue (`GamePlayColors.InfoDark`)
- Text color remains white
- No border added

### After (Android TV)
When a tower button is selected on Android TV:
- Button background color changes to a darker blue (`GamePlayColors.InfoDark`) - **same as before**
- Text color remains white - **same as before**
- **NEW**: A bright yellow 4dp border is added around the button

## Visual Representation

```
┌─────────────────────────────────────────────────────────────────┐
│                     Regular Android Device                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Pike Tower]  [Spear Tower]  [Bow Tower]  [Wizard Tower]     │
│   (not sel.)     (SELECTED)    (not sel.)    (not sel.)       │
│                                                                 │
│   Normal BG      Darker BG     Normal BG     Normal BG         │
│   Blue           Blue           Blue          Blue             │
│   No Border      No Border      No Border     No Border        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        Android TV Device                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Pike Tower]  ╔═══════════════╗  [Bow Tower]  [Wizard Tower] │
│   (not sel.)   ║[Spear Tower]  ║   (not sel.)    (not sel.)   │
│                ║  (SELECTED)   ║                               │
│                ║               ║                               │
│   Normal BG    ║   Darker BG   ║   Normal BG     Normal BG    │
│   Blue         ║   Blue        ║   Blue          Blue         │
│   No Border    ║YELLOW BORDER!!║   No Border     No Border    │
│                ╚═══════════════╝                               │
└─────────────────────────────────────────────────────────────────┘
```

## Technical Details

### Border Specification
- **Width**: 4dp (density-independent pixels)
- **Color**: Yellow (`Color.Yellow`)
- **Shape**: Follows Material3 `MaterialTheme.shapes.small` (rounded corners)
- **When Applied**: Only when `getPlatform().isAndroidTV == true` AND `isSelected == true`

### Performance Optimization
- Platform detection is cached using `remember` to avoid recomputation on every recomposition
- Border modifier is only applied conditionally based on platform and selection state

## User Experience Benefits

### For TV Remote Users
1. **High Contrast**: Yellow border on blue background provides excellent visibility
2. **Clear Selection**: Immediately obvious which button is selected
3. **TV-Optimized**: Follows Android TV UI guidelines for focus indication
4. **D-pad Navigation**: Makes navigating with TV remote much easier

### For Non-TV Users
- No visual changes
- Existing UI behavior preserved
- No performance impact

## Code Locations

### Platform Detection
- Interface: `composeApp/src/commonMain/kotlin/de/egril/defender/utils/Platform.kt`
- Android Implementation: `composeApp/src/androidMain/kotlin/de/egril/defender/utils/Platform.android.kt`

### Button Components
- `CompactDefenderButton`: Lines 22-84 in `DefenderButtons.kt`
- `DefenderButton`: Lines 86-220 in `DefenderButtons.kt`
- File: `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/DefenderButtons.kt`

## Testing

### Unit Tests
- Platform detection tests: `composeApp/src/commonTest/kotlin/de/egril/defender/utils/PlatformTest.kt`
- All existing tests continue to pass

### Manual Testing Required
To fully verify this feature:
1. Build the APK: `./gradlew :composeApp:assembleDebug`
2. Install on Android TV device or emulator
3. Launch the game
4. Use D-pad to navigate between tower buttons
5. Verify yellow border appears on selected button
