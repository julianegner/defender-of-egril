# Official Edit Mode - Visual Guide

## Overview

This guide shows the visual differences between normal mode and official edit mode in the level editor.

## Map Editor

### Normal Mode (official=false)

**Map List View:**
```
┌─────────────────────────────────────────┐
│ Official Maps                            │
├─────────────────────────────────────────┤
│ □ map_tutorial                     [📝]  │
│   Status: Official Map                   │
│   Buttons: [Edit] [Copy]                │
│   Delete button: DISABLED (grayed out)  │
├─────────────────────────────────────────┤
│ □ map_spiral                       [📝]  │
│   Status: Official Map                   │
│   Buttons: [Edit] [Copy]                │
│   Delete button: DISABLED (grayed out)  │
└─────────────────────────────────────────┘
```

**Map Editor View (when editing official map):**
```
┌─────────────────────────────────────────┐
│ Map Editor - map_tutorial               │
├─────────────────────────────────────────┤
│ Map Name: [Tutorial Map           ]     │
│           ^^^ DISABLED (read-only)      │
│                                          │
│ Tile Type: [PATH ▼]                    │
│                                          │
│ [Map Grid Display]                      │
│                                          │
│ Actions:                                │
│ [Save Map] ← DISABLED (grayed out)      │
│ [Save As New]                           │
│ [Cancel]                                │
└─────────────────────────────────────────┘
```

### Official Edit Mode (official=true)

**Map List View:**
```
┌─────────────────────────────────────────┐
│ Official Maps                            │
├─────────────────────────────────────────┤
│ □ map_tutorial                     [📝]  │
│   Status: Official Map                   │
│   Buttons: [Edit] [Copy]                │
│   Delete button: ENABLED (can click)    │
├─────────────────────────────────────────┤
│ □ map_spiral                       [📝]  │
│   Status: Official Map                   │
│   Buttons: [Edit] [Copy]                │
│   Delete button: ENABLED (can click)    │
└─────────────────────────────────────────┘
```

**Map Editor View (when editing official map):**
```
┌─────────────────────────────────────────┐
│ Map Editor - map_tutorial               │
├─────────────────────────────────────────┤
│ Map Name: [Tutorial Map           ]     │
│           ^^^ ENABLED (can type)        │
│                                          │
│ Tile Type: [PATH ▼]                    │
│                                          │
│ [Map Grid Display]                      │
│                                          │
│ Actions:                                │
│ [Save Map] ← ENABLED (can click)        │
│ [Save As New]                           │
│ [Cancel]                                │
└─────────────────────────────────────────┘
```

## Level Editor

### Normal Mode (official=false)

**Level Editor View:**
```
┌─────────────────────────────────────────┐
│ Level Editor - welcome_to_defender      │
├─────────────────────────────────────────┤
│ Level Title: [Welcome to Defender  ]    │
│              ^^^ DISABLED (read-only)   │
│                                          │
│ Subtitle:    [Your first battle    ]    │
│              ^^^ DISABLED (read-only)   │
│                                          │
│ Start Coins: [100]                      │
│ Start HP:    [20]                       │
│                                          │
│ Enemy Spawns:                           │
│ [Add Enemy Spawn]                       │
│                                          │
│ Actions:                                │
│ [Copy Level]                            │
│ [Delete] ← DISABLED (grayed out)        │
│ [Save Level] ← DISABLED (grayed out)    │
│ [Save As New]                           │
└─────────────────────────────────────────┘
```

### Official Edit Mode (official=true)

**Level Editor View:**
```
┌─────────────────────────────────────────┐
│ Level Editor - welcome_to_defender      │
├─────────────────────────────────────────┤
│ Level Title: [Welcome to Defender  ]    │
│              ^^^ ENABLED (can type)     │
│                                          │
│ Subtitle:    [Your first battle    ]    │
│              ^^^ ENABLED (can type)     │
│                                          │
│ Start Coins: [100]                      │
│ Start HP:    [20]                       │
│                                          │
│ Enemy Spawns:                           │
│ [Add Enemy Spawn]                       │
│                                          │
│ Actions:                                │
│ [Copy Level]                            │
│ [Delete] ← ENABLED (can click)          │
│ [Save Level] ← ENABLED (can click)      │
│ [Save As New]                           │
└─────────────────────────────────────────┘
```

## Close Dialog

### Normal Mode (official=false)

When closing with unsaved changes in gameplay:
```
┌────────────────────────────────────────┐
│ ⚠️ Unsaved Changes                     │
├────────────────────────────────────────┤
│ You have unsaved game progress.        │
│ Do you want to save before closing?    │
│                                         │
│ [Cancel] [Discard] [Save & Exit]       │
└────────────────────────────────────────┘
```

No dialog when closing after editing official content (can't edit it).

### Official Edit Mode (official=true)

When closing after editing official content:
```
┌────────────────────────────────────────┐
│ ⚠️ Official Game Data Modified         │
├────────────────────────────────────────┤
│ You have made changes to official      │
│ game data (maps or levels).            │
│                                         │
│ These changes will be overwritten when │
│ you restart the game, as official      │
│ content is restored from the           │
│ repository on each launch.             │
│                                         │
│ To preserve your changes, consider     │
│ copying the official content to create │
│ user versions instead.                 │
│                                         │
│ Modified maps: map_tutorial, map_spiral│
│ Modified levels: welcome_to_defender   │
│                                         │
│                     [Understood]        │
└────────────────────────────────────────┘
```

After clicking "Understood", if there are also unsaved game changes:
```
┌────────────────────────────────────────┐
│ ⚠️ Unsaved Changes                     │
├────────────────────────────────────────┤
│ You have unsaved game progress.        │
│ Do you want to save before closing?    │
│                                         │
│ [Cancel] [Discard] [Save & Exit]       │
└────────────────────────────────────────┘
```

## Color Indicators

### Button States

**Disabled (Normal Mode):**
- Background: Light gray (#E0E0E0 in light mode, #3A3A3A in dark mode)
- Text: Medium gray (#9E9E9E)
- Cursor: Not allowed (🚫)

**Enabled (Official Edit Mode):**
- Background: Theme primary color (blue)
- Text: White
- Cursor: Pointer (👆)

**Delete Button Specifically:**
- When disabled: Gray background
- When enabled: Red background (#D32F2F)

### Input Fields

**Disabled (Normal Mode):**
- Background: Light gray (#F5F5F5)
- Border: Gray (#BDBDBD)
- Text: Medium gray (#757575)
- Cursor: Not allowed (🚫)

**Enabled (Official Edit Mode):**
- Background: White (light mode) / Dark gray (dark mode)
- Border: Primary color when focused
- Text: Normal color
- Cursor: Text cursor (|)

## Badge Indicators

Throughout the UI, official content shows a badge:

**Normal Mode & Official Edit Mode (same):**
```
┌────────────────────┐
│ map_tutorial       │
│ [Official] badge   │
└────────────────────┘
```

The badge remains visible in both modes to remind users they're working with official content.

## Summary of Visual Differences

| Element | Normal Mode | Official Edit Mode |
|---------|-------------|-------------------|
| Map name input | 🚫 Disabled | ✅ Enabled |
| Level title input | 🚫 Disabled | ✅ Enabled |
| Level subtitle input | 🚫 Disabled | ✅ Enabled |
| Save map button | 🚫 Disabled | ✅ Enabled |
| Save level button | 🚫 Disabled | ✅ Enabled |
| Delete map button | 🚫 Disabled | ✅ Enabled |
| Delete level button | 🚫 Disabled | ✅ Enabled |
| Close warning | ❌ Not shown | ✅ Shows with changes |

## User Experience Flow

### Normal Mode Flow
1. User opens editor
2. User selects official map/level
3. User sees "Official" badge
4. Edit/Save/Delete controls are disabled
5. User can only view or copy
6. No warning on close

### Official Edit Mode Flow
1. User builds with `-Pofficial=true`
2. User opens editor
3. User selects official map/level
4. User sees "Official" badge (same as normal)
5. All edit controls are enabled
6. User makes changes
7. Changes are tracked automatically
8. User clicks close/exit
9. **Warning dialog appears** listing changes
10. User acknowledges warning
11. App closes normally

## Best Practices

### Visual Feedback
- The "Official" badge is always shown regardless of mode
- In official edit mode, there's no special visual indicator that the mode is active
- The only difference is button/input enabled states
- This is intentional to avoid cluttering the UI

### User Protection
- Even though controls are enabled, the warning dialog provides a safety net
- The warning clearly explains the consequences
- Users must explicitly acknowledge the warning to continue

### Developer Experience
- Developers don't need to remember which mode they're in
- The UI behaves naturally based on the compile flag
- No runtime switches or configuration needed
