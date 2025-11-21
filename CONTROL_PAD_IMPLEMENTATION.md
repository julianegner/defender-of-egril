# Mobile Control Pad Implementation

## Overview

This document describes the implementation of the mobile control pad feature, which provides on-screen directional controls and zoom buttons for navigating the hexagonal game map on mobile devices and other platforms where keyboard navigation is not convenient.

## Feature Description

The control pad consists of two components:

1. **Directional Control Pad**: A circular pad with 4 directional buttons (Up, Down, Left, Right) arranged in quadrants
2. **Zoom Controls**: Two vertically stacked buttons for zoom in (+) and zoom out (-)

Both components appear in the bottom-right corner of the map view when enabled.

### Visual Layout

```
Map View
┌──────────────────────────────────────────┐
│                                          │
│                                          │
│        Hexagonal Game Map                │
│                                          │
│                                          │
│                          ┌────┐          │
│                    ╭─────┴──┴─╮   ┌──┐  │
│                   ╱      ▲      ╲  │ + │  │
│                  │   <   •   >   │ ├──┤  │
│                   ╲      ▼      ╱  │ - │  │
│                    ╰────────────╯  └──┘  │
│                   Control Pad   Zoom     │
└──────────────────────────────────────────┘

Legend:
• Center indicator
▲▼◀▶ Directional arrows
+ - Zoom in/out buttons
```

The control pad is circular (120dp diameter) divided into 4 equal quadrants by perpendicular lines.
Each quadrant contains a directional button (60dp × 60dp touch target).
The zoom controls are a vertical stack (60dp wide, 122dp tall with divider).

## Implementation Details

### Settings

- **Location**: `AppSettings.kt`
- **Setting Key**: `show_control_pad`
- **Default Values**:
  - Mobile (Android/iOS): ON
  - Desktop/Web: OFF
- **User Control**: Toggle via Settings Dialog under "Controls" section

### UI Components

#### ControlPad Component
- **File**: `composeApp/src/commonMain/kotlin/de/egril/defender/ui/ControlPad.kt`
- **Appearance**: 
  - Circular shape (120dp diameter)
  - Semi-transparent background (surfaceVariant with 0.7 alpha)
  - Four directional arrows in quadrants
  - Center indicator dot
- **Actions**:
  - Up: Pans viewport upward
  - Down: Pans viewport downward
  - Left: Pans viewport left
  - Right: Pans viewport right

#### ZoomControls Component
- **File**: `composeApp/src/commonMain/kotlin/de/egril/defender/ui/ControlPad.kt`
- **Appearance**:
  - Vertical rectangle (60dp wide)
  - Semi-transparent background (surfaceVariant with 0.7 alpha)
  - Plus icon (+) on top
  - Minus icon (-) on bottom
  - Divider line between buttons
- **Actions**:
  - Plus (+): Zooms in by 0.1x (respects min/max scale limits)
  - Minus (-): Zooms out by 0.1x (respects min/max scale limits)

### Integration

The control pad is integrated into `HexagonalMapView.kt`:
- Displayed as an overlay in the bottom-right corner
- Visibility controlled by `AppSettings.showControlPad` state
- Uses the same pan/zoom logic as keyboard navigation
- Respects viewport constraints (prevents panning beyond map bounds)
- Automatically re-constrains viewport after zoom changes

### Localization

Control pad settings are localized in all supported languages:

#### English (values/strings.xml)
- `controls`: "Controls"
- `show_control_pad`: "Show Control Pad"
- `control_pad_enabled`: "Control Pad Enabled"
- `control_pad_disabled`: "Control Pad Disabled"

#### German (values-de/strings.xml)
- `controls`: "Steuerung"
- `show_control_pad`: "Steuerkreuz anzeigen"
- `control_pad_enabled`: "Steuerkreuz aktiviert"
- `control_pad_disabled`: "Steuerkreuz deaktiviert"

#### Spanish (values-es/strings.xml)
- `controls`: "Controles"
- `show_control_pad`: "Mostrar panel de control"
- `control_pad_enabled`: "Panel de control activado"
- `control_pad_disabled`: "Panel de control desactivado"

#### French (values-fr/strings.xml)
- `controls`: "Contrôles"
- `show_control_pad`: "Afficher le pavé directionnel"
- `control_pad_enabled`: "Pavé directionnel activé"
- `control_pad_disabled`: "Pavé directionnel désactivé"

#### Italian (values-it/strings.xml)
- `controls`: "Controlli"
- `show_control_pad`: "Mostra pad direzionale"
- `control_pad_enabled`: "Pad direzionale attivato"
- `control_pad_disabled`: "Pad direzionale disattivato"

## Usage

### For Players

1. **Mobile Users**: The control pad is enabled by default. Use the directional pad to navigate the map and the +/- buttons to zoom.

2. **Desktop/Web Users**: The control pad is disabled by default. Enable it via:
   - Open Settings (gear icon on any screen)
   - Scroll to "Controls" section
   - Toggle "Show Control Pad" switch

3. **Changing the Setting**:
   - Setting persists across app restarts
   - Can be reset to platform defaults via "Reset Settings" button

### For Developers

#### Using the ControlPad Component

```kotlin
ControlPad(
    onUp = { /* handle up */ },
    onDown = { /* handle down */ },
    onLeft = { /* handle left */ },
    onRight = { /* handle right */ },
    modifier = Modifier.size(120.dp)
)
```

#### Using the ZoomControls Component

```kotlin
ZoomControls(
    onZoomIn = { /* handle zoom in */ },
    onZoomOut = { /* handle zoom out */ },
    modifier = Modifier.width(60.dp)
)
```

#### Checking Control Pad Visibility

```kotlin
if (AppSettings.showControlPad.value) {
    // Show control pad
}
```

## Design Rationale

### Circular Quadrant Design

The control pad uses a circular design divided into 4 quadrants by perpendicular lines through the center, as specified in the requirements:

- **Mathematical Basis**: Circle divided by two perpendicular diameters creates four 90° quadrants
- **Visual Clarity**: Each direction button occupies exactly one quadrant
- **Touch Target Size**: Each button is 60dp × 60dp, meeting minimum touch target guidelines
- **Center Indicator**: Small circular dot at center provides visual anchor point

### Semi-Transparent Background

The controls use semi-transparent backgrounds (70% opacity) to:
- Remain visible without completely obscuring the game map
- Maintain visual hierarchy (map is primary, controls are secondary)
- Blend naturally with the game's visual design

### Bottom-Right Placement

Controls are positioned in the bottom-right corner to:
- Avoid overlapping critical game UI elements (header, controls, tower info)
- Provide easy thumb access on mobile devices
- Follow common mobile game control patterns
- Leave the map content maximally visible

## Technical Notes

### Performance

- Control pad uses Compose's recomposition optimization
- State changes (pan/zoom) are batched through callbacks
- No continuous rendering or animations that would impact performance

### Accessibility

- All buttons use Material Icons for visual consistency
- Icon sizes (32dp) meet accessibility guidelines
- Touch targets (60dp) exceed minimum accessibility recommendations
- Semantic descriptions provided for screen readers

### Platform Compatibility

- Works on all platforms: Android, iOS, Desktop, Web/WASM
- Platform detection uses existing `isPlatformMobile` utility
- No platform-specific code required in the control pad itself

## Future Enhancements

Potential improvements for future iterations:

1. **Customizable Position**: Allow users to move controls to different corners
2. **Size Adjustment**: Configurable control pad size for different screen sizes
3. **Haptic Feedback**: Vibration feedback on mobile devices
4. **Gesture Recognition**: Support for swipe gestures in addition to button taps
5. **Auto-Hide**: Automatically hide controls after period of inactivity
6. **Transparency Adjustment**: User-configurable opacity level

## Testing

### Manual Testing

1. Enable control pad in settings
2. Verify directional buttons pan the viewport correctly
3. Verify zoom buttons adjust scale within allowed range (0.5x - 3.0x)
4. Verify viewport constraints prevent panning beyond map bounds
5. Test on different platforms to ensure default settings are correct

### Automated Testing

- Settings dialog tests verify the control pad toggle appears and works
- Build tests confirm no compilation errors or warnings
- Icon deprecation warnings resolved by using AutoMirrored icons

## Files Modified

1. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/AppSettings.kt`
   - Added `showControlPad` state
   - Added `saveShowControlPad()` method
   - Updated `initialize()` and `resetToDefaults()`

2. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/SettingsDialog.kt`
   - Added "Controls" section with control pad toggle

3. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/HexagonalMapView.kt`
   - Added control pad overlay in bottom-right corner
   - Integrated pan and zoom callbacks

4. `composeApp/src/commonMain/composeResources/values*/strings.xml` (all languages)
   - Added control pad related strings

## Files Created

1. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/ControlPad.kt`
   - ControlPad composable
   - ZoomControls composable
