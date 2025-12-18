# Collapsible Map Editor Header - Visual Guide

This document provides a visual representation of the collapsible header feature in the Map Editor.

## Expanded State (Default)

```
┌──────────────────────────────────────────────────────────────────┐
│  Map Editor Header (Full Controls)                    [▲ Collapse]│
├──────────────────────────────────────────────────────────────────┤
│  Editing Map: map_30x8                                           │
│                                                                  │
│  Map Name: ┌─────────────────────────────────────────┐          │
│            │ Generated Map 30x8                        │          │
│            └─────────────────────────────────────────┘          │
│                                                                  │
│  Select Tile Type:                                              │
│  ┌──────┬──────┬──────┬──────┬──────┬──────┬──────┐            │
│  │ PATH │ BUILD│ISLAND│NO_PLAY│SPAWN│TARGET│RIVER │            │
│  │      │_AREA │      │       │_POINT│      │      │            │
│  └──────┴──────┴──────┴──────┴──────┴──────┴──────┘            │
│                                                                  │
│  [when RIVER selected]                                          │
│  ┌───────────────────────────────────────────────────┐          │
│  │ River Properties                                  │          │
│  │                                                   │          │
│  │ Flow Direction:                                   │          │
│  │ [NONE][MAELSTROM][EAST][SE][SW][WEST][NW][NE]   │          │
│  │                                                   │          │
│  │ Flow Speed:                                       │          │
│  │ [1 (Slow)] [2 (Fast)]                            │          │
│  └───────────────────────────────────────────────────┘          │
│                                                                  │
│  [Change All NO_PLAY to PATH]                                   │
│                                                                  │
│  Click hexagons to paint (30x8)    [-]  50%  [+]              │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
    (Total height: ~280dp)
```

## Collapsed State

```
┌─────────────────────────────────────┐
│ [  Current: PATH   ▼ ]  [▼]         │  ← Small card on left side
└─────────────────────────────────────┘
    (Total height: ~56dp)

When dropdown is clicked:
┌─────────────────────────────────────┐
│ [  Current: PATH   ▼ ]  [▼]         │
├─────────────────────────────────────┤
│ PATH                                │  ← Dropdown menu
│ BUILD_AREA                          │
│ ISLAND                              │
│ NO_PLAY                             │
│ SPAWN_POINT                         │
│ TARGET                              │
│ RIVER                               │  ← Clicking this opens dialog
└─────────────────────────────────────┘
```

## River Properties Dialog (in Collapsed Mode)

When RIVER is selected from the dropdown in collapsed mode:

```
          ┌────────────────────────────────────┐
          │  River Properties                 │
          ├────────────────────────────────────┤
          │                                   │
          │  Flow Direction:                  │
          │  [NONE] [MAELSTROM] [EAST]       │
          │  [SE] [SW] [WEST] [NW] [NE]      │
          │                                   │
          │  Flow Speed:                      │
          │  [1 (Slow)] [2 (Fast)]           │
          │                                   │
          │                          [OK]    │
          └────────────────────────────────────┘
```

## Map Area Comparison

### With Expanded Header
```
┌──────────────────────────────────────┐
│ ████████████████████████████████████ │ ← Header (280dp)
├──────────────────────────────────────┤
│                                      │
│      ◯  ◯  ◯  ◯  ◯  ◯  ◯           │ ← Map area
│     ◯  ◯  ◯  ◯  ◯  ◯  ◯  ◯         │   (Limited space)
│      ◯  ◯  ◯  ◯  ◯  ◯  ◯           │
│     ◯  ◯  ◯  ◯  ◯  ◯  ◯  ◯         │
│      ...                             │
│                                      │
│ [Save] [Save As] [Cancel]           │
└──────────────────────────────────────┘
```

### With Collapsed Header
```
┌──────────────────┐
│ ██ [PATH ▼] [▼] │ ← Header (56dp)
└──────────────────┘
┌──────────────────────────────────────┐
│                                      │
│      ◯  ◯  ◯  ◯  ◯  ◯  ◯           │
│     ◯  ◯  ◯  ◯  ◯  ◯  ◯  ◯         │
│      ◯  ◯  ◯  ◯  ◯  ◯  ◯           │ ← Map area
│     ◯  ◯  ◯  ◯  ◯  ◯  ◯  ◯         │   (~224dp more space!)
│      ◯  ◯  ◯  ◯  ◯  ◯  ◯           │
│     ◯  ◯  ◯  ◯  ◯  ◯  ◯  ◯         │
│      ◯  ◯  ◯  ◯  ◯  ◯  ◯           │
│     ◯  ◯  ◯  ◯  ◯  ◯  ◯  ◯         │
│      ◯  ◯  ◯  ◯  ◯  ◯  ◯           │
│     ◯  ◯  ◯  ◯  ◯  ◯  ◯  ◯         │
│      ...                             │
│                                      │
│ [Save] [Save As] [Cancel]           │
└──────────────────────────────────────┘
```

## User Interaction Flow

### Collapsing the Header
```
1. User sees expanded header with all controls
   ↓
2. User clicks [▲ Collapse] button
   ↓
3. Header smoothly transitions to collapsed state
   ↓
4. Map area gains ~224dp of vertical space
   ↓
5. User continues editing with compact controls
```

### Selecting RIVER Tile in Collapsed Mode
```
1. User clicks [PATH ▼] dropdown button
   ↓
2. Dropdown menu appears with all 7 tile types
   ↓
3. User clicks "RIVER" option
   ↓
4. Dropdown closes
   ↓
5. River Properties dialog automatically opens
   ↓
6. User configures flow direction and speed
   ↓
7. User clicks [OK] to close dialog
   ↓
8. Ready to paint RIVER tiles on map
```

### Expanding the Header
```
1. User working with collapsed header
   ↓
2. User wants access to more controls
   ↓
3. User clicks [▼] expand button
   ↓
4. Header restores to full expanded state
   ↓
5. All controls now visible and accessible
```

## Benefits Visualization

### Space Savings
- **Expanded**: 280dp header + remaining space for map
- **Collapsed**: 56dp header + remaining space for map
- **Gain**: ~224dp additional vertical space (80% reduction in header size)

### Control Accessibility
- **Expanded**: All controls immediately visible
- **Collapsed**: Essential controls (tile selection) remain accessible via dropdown

### Workflow Flexibility
- **Initial Setup**: Use expanded header for configuration
- **Active Editing**: Use collapsed header for maximum map visibility
- **Quick Changes**: Use dropdown for rapid tile type switching
