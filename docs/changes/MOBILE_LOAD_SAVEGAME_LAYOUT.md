# Mobile Load Savegame Screen Layout Changes

## Overview
This document describes the changes made to improve the mobile layout of the load savegame screen, addressing the issue where cards and UI elements were too large on mobile devices.

## Problem
The original load savegame screen had several issues on mobile phones:
- Cards were too large and took up too much vertical space
- Header and footer elements consumed excessive screen real estate
- The toggle switch and its description area were oversized
- Download/upload buttons were unnecessarily large
- Overall layout was not optimized for narrow mobile screens

## Solution
Implemented a responsive layout using `BoxWithConstraints` that detects screen width and provides two distinct layouts:

### Mobile Layout (< 600dp width)
- **Left Sidebar (200dp width)**: Contains all controls in a compact vertical arrangement
  - Title (smaller typography: `titleMedium` instead of `titleLarge`)
  - Game Data Transfer toggle in a compact card with smaller padding (8dp vs 12dp)
  - Export Game Progress button (when toggle is enabled) with vertical icon layout
  - Download All button (vertical icon layout, small font 10sp)
  - Upload button (vertical icon layout, small font 10sp)
  - Back button at bottom
  - Savegame folder path with very small font (8sp) and line breaks
  
- **Right Side (flexible width)**: Saved games list
  - Settings button in top-right corner
  - Saved game cards with reduced padding (8dp vs 16dp)
  - Smaller card elevation (2dp vs 4dp)
  - Compact typography throughout

### Desktop Layout (≥ 600dp width)
- Maintains original layout with all elements centered
- Full-width title, toggle area, and button rows
- Original padding and typography sizes preserved

## SavedGameCard Mobile Optimizations
Cards automatically adapt to mobile screens with:
- Reduced padding: 8dp instead of 16dp
- Smaller elevation: 2dp instead of 4dp
- Compact typography:
  - Title: `titleSmall` vs `titleMedium`
  - Date: 10sp vs 12sp font size
  - Stats icons: 12dp vs 16dp
  - Stats text: 11sp vs 14sp
  - Unit entries: 20dp icons vs 32dp
  - Unit entry text: 9sp vs 11sp

### Card Component Layout Changes
**Mobile layout changes:**
- Towers and enemies shown side-by-side in a row (instead of 3 columns)
- Minimap moved below with smaller size (120dp x 60dp vs 300dp x 120dp)
- Action buttons (download/delete) shown as compact icons only (no text)
- Enemy list simplified to show only first 2 enemies with "..." if more exist

**Desktop layout:**
- Maintains original 3-column layout (towers, enemies, minimap)
- All original sizing and spacing preserved

## Technical Implementation

### Files Modified
1. **LoadGameScreen.kt**: Main screen component
   - Added `BoxWithConstraints` wrapper to detect screen width
   - Split into `LoadGameScreenMobile` and `LoadGameScreenDesktop` functions
   - Threshold: 600dp width

2. **SavedGameCard.kt**: Individual save game card
   - Added `BoxWithConstraints` to detect card width
   - Passes `isMobile` parameter to all sub-components
   - Adapts padding and elevation based on screen size

3. **SavedGameCardComponents.kt**: Card sub-components
   - Updated all components to accept `isMobile` parameter
   - Implemented size/spacing adjustments for mobile
   - Simplified enemy list display for mobile (first 2 entries only)

### Responsive Breakpoints
- **Mobile**: maxWidth < 600dp
- **Desktop**: maxWidth ≥ 600dp

This threshold aligns with Material Design's compact width breakpoint and ensures a good experience on most phones while preserving the desktop layout on tablets and larger devices.

## Benefits
1. **Better Space Utilization**: Controls in sidebar leaves more vertical space for saved games
2. **Improved Readability**: Smaller fonts and tighter spacing prevent overwhelming mobile users
3. **Easier Navigation**: All controls accessible in one column on the left
4. **Maintained Desktop Experience**: No changes to desktop/tablet layouts
5. **Progressive Enhancement**: Automatically adapts to screen size without user intervention

## Testing
- Build verified on Android target (assembleDebug succeeded)
- All unit tests pass (testDebugUnitTest)
- Compilation successful across all platforms

## Screenshot Comparison
See the issue screenshot for the before state. The after state features:
- Left sidebar with vertically stacked controls
- More space for saved game cards
- Reduced visual clutter through compact sizing
- Better use of horizontal space on narrow screens
