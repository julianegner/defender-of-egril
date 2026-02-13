# Autosave Card Color Distinction

## Overview

This document describes the implementation of distinct colors for autosave cards in the Load Game screen.

## Issue

The autosave card in the saved games list needed a distinct color to make it easily identifiable from regular save game cards.

## Solution

### Implementation

The solution was implemented in `SavedGameCard.kt` by:

1. **Detecting autosave cards**: Check if `saveGame.id == "autosave_game"`
2. **Applying distinct color**: Use `MaterialTheme.colorScheme.tertiaryContainer` for autosave cards
3. **Preserving regular card appearance**: Regular saves continue to use `MaterialTheme.colorScheme.surface`

### Code Changes

```kotlin
// Check if this is an autosave
val isAutosave = saveGame.id == "autosave_game"

Card(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onLoad() },
    elevation = CardDefaults.cardElevation(defaultElevation = if (isMobileCard) 2.dp else 4.dp),
    colors = CardDefaults.cardColors(
        containerColor = if (isAutosave) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    )
)
```

## Color Scheme

The `tertiaryContainer` color provides distinct colors in both light and dark themes:

- **Light Mode**: `#FFD8E4` (light pinkish/rose color) - Based on `Color(0xFFFFD8E4)` from AppTheme.kt
- **Dark Mode**: `#633B48` (darker reddish/burgundy color) - Based on `Color(0xFF633B48)` from AppTheme.kt

Regular save cards use `MaterialTheme.colorScheme.surface`:
- **Light Mode**: Standard light surface color
- **Dark Mode**: Standard dark surface color

This ensures the autosave card is visually distinguishable in all theme modes while maintaining consistency with Material Design 3 principles.

## Testing

A unit test was added in `SavedGameCardColorTest.kt` to verify:
- Autosave ID detection (`autosave_game`)
- Logic for distinguishing autosaves from regular saves
- Edge cases with similar but different IDs

## Autosave Mechanism

Autosaves are created:
- At the beginning of each new turn during gameplay
- Using a fixed ID: `"autosave_game"`
- With comment: `"Autosave"`
- Overwriting the previous autosave file

This ensures only one autosave exists at a time, and it's always identifiable by its fixed ID.

## Files Modified

1. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/SavedGameCard.kt` - Main implementation
2. `composeApp/src/commonTest/kotlin/de/egril/defender/ui/loadgame/SavedGameCardColorTest.kt` - Unit test

## Visual Impact

### What Users Will See

**In Light Mode:**
- Autosave card: Light pinkish/rose background (`#FFD8E4`)
- Regular save cards: Standard light background (white/light gray)
- The autosave card stands out with a subtle rose tint

**In Dark Mode:**
- Autosave card: Dark reddish/burgundy background (`#633B48`)
- Regular save cards: Standard dark background (dark gray)
- The autosave card stands out with a subtle burgundy tint

### Where to See the Change

The distinct coloring is visible in:
1. **Load Game Screen**: Main menu → Load Game
2. **Both Desktop and Mobile**: The color distinction works on all platforms
3. **All Theme Modes**: Works in both light and dark theme

### User Experience Benefits

- **Quick Identification**: Users can instantly spot the autosave among their save files
- **Reduced Confusion**: Clear visual distinction prevents accidentally selecting/deleting the autosave
- **Consistent Design**: Uses Material Design 3 color system for professional appearance
- **Theme-Aware**: Automatically adapts colors to match the current theme
