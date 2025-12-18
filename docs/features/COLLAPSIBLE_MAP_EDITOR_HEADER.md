# Collapsible Map Editor Header

## Overview

This feature adds a collapsible header to the Map Editor that allows users to maximize screen space for editing maps while retaining quick access to essential controls.

## Problem Statement

The original Map Editor header was too large (approximately 280dp in height), which significantly reduced the visible map area, especially on smaller screens or when working with larger maps. Users needed a way to hide the header controls while still being able to select different tile types.

## Solution

Implemented a toggle mechanism that switches between two header states:

### 1. Expanded State (Default)

The full header with all controls:
- Map name input field
- Complete tile type selector (all 7 tile types visible)
- River properties (flow direction and speed selectors)
- "Change All NO_PLAY to PATH" button
- Zoom controls
- Collapse button with ▲ icon

**Height**: ~280dp

### 2. Collapsed State

A compact card positioned on the left side with minimal controls:
- Tile type dropdown (280dp width)
- All tile types available in dropdown menu
- Expand button with ▼ icon
- River properties dialog (opens when RIVER tile is selected from dropdown)

**Height**: ~56dp (saves ~224dp of vertical space)

## Implementation Details

### Files Modified

1. **MapEditorHeader.kt**
   - Refactored into three composables:
     - `MapEditorHeader`: Main entry point with conditional rendering
     - `ExpandedMapEditorHeader`: Full header (original implementation)
     - `CollapsedMapEditorHeader`: Compact header (new implementation)
   - Added parameters: `isExpanded: Boolean` and `onToggleExpanded: () -> Unit`

2. **MapEditorView.kt**
   - Added `isHeaderExpanded` state variable (default: `true`)
   - Added `headerHeight` derived value (280.dp or 56.dp based on state)
   - Updated spacer to use dynamic `headerHeight` instead of hardcoded value
   - Passed state management parameters to MapEditorHeader

### Key Components

#### Expanded Header
```kotlin
@Composable
private fun ExpandedMapEditorHeader(
    // ... parameters
    onCollapse: () -> Unit
) {
    Card {
        Column {
            // Header with collapse button
            Row {
                Text("Editing Map: ...")
                Button(onClick = onCollapse) {
                    TriangleUpIcon()
                    Text("Collapse")
                }
            }
            // ... rest of controls
        }
    }
}
```

#### Collapsed Header
```kotlin
@Composable
private fun CollapsedMapEditorHeader(
    // ... parameters
    onExpand: () -> Unit
) {
    Card(width = 280.dp) {
        Row {
            // Tile type dropdown
            Box(weight = 1f) {
                Button { Text(selectedTileType.name) }
                DropdownMenu { /* all tile types */ }
            }
            // Expand button
            Button(onClick = onExpand) {
                TriangleDownIcon()
            }
        }
    }
    
    // River properties dialog (shown when needed)
    if (showRiverPropertiesDialog) {
        AlertDialog { /* river settings */ }
    }
}
```

## User Experience

### Expanding/Collapsing
1. User clicks collapse button (▲) in expanded header
2. Header smoothly transitions to collapsed state
3. Map area gains ~224dp of vertical space
4. User can still select tile types via dropdown
5. User clicks expand button (▼) to restore full controls

### Tile Selection in Collapsed Mode
1. Click dropdown button showing current tile type
2. Select desired tile type from menu
3. If RIVER is selected, river properties dialog appears automatically
4. Configure flow direction and speed in dialog
5. Click OK to close dialog

## Benefits

- **More Workspace**: ~224dp additional vertical space for map editing
- **Quick Access**: Tile type selection still available in collapsed mode
- **Flexibility**: Users can choose based on their needs and screen size
- **Non-Breaking**: Default expanded state maintains existing workflow

## Technical Notes

### State Management
- State is local to `MapEditorView` (not persisted)
- Each time the editor is opened, header starts in expanded state
- Future enhancement: Could persist preference to user settings

### Styling
- Uses existing icon components (`TriangleUpIcon`, `TriangleDownIcon`)
- Maintains Material3 design system consistency
- Respects light/dark mode themes

### Accessibility
- Clear visual indicators (▲/▼ icons + text labels)
- All controls remain keyboard-accessible
- Dropdown menu follows Material3 accessibility guidelines

## Testing

### Manual Testing
1. Open Map Editor
2. Verify header is expanded by default
3. Click collapse button → header should minimize
4. Click tile type dropdown → verify all 7 tile types appear
5. Select RIVER → verify river properties dialog appears
6. Configure river settings → click OK
7. Click expand button → header should restore to full size
8. Verify all controls still function correctly

### Automated Testing
- Existing tests still pass (no breaking changes)
- UI component structure allows for future Compose UI tests

## Future Enhancements

1. **Persist Collapsed State**: Remember user preference across sessions
2. **Keyboard Shortcuts**: Add hotkey to toggle header (e.g., 'H' key)
3. **Animation**: Add smooth transition animation between states
4. **Mobile Optimization**: Different collapsed layout for mobile screens
5. **Custom Position**: Allow dragging collapsed header to different positions
