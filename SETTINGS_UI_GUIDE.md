# Settings UI Visual Guide

## Settings Button Location

### Main Menu Screen
```
┌─────────────────────────────────────────────┐
│                              [⚙️ Settings]   │
│                                             │
│         Defender of Egril                   │
│                                             │
│    Defend the meadows of Egril against      │
│   the Hordes of Gleid Thyae under the      │
│      Banner of the evil Ewhad               │
│                                             │
│         ┌─────────────┐                     │
│         │ Start Game  │                     │
│         └─────────────┘                     │
│         ┌─────────────┐                     │
│         │   Rules     │                     │
│         └─────────────┘                     │
│                                             │
│ v1.0 (abc123)                              │
└─────────────────────────────────────────────┘
```

### World Map Screen
```
┌─────────────────────────────────────────────┐
│                              [⚙️ Settings]   │
│                                             │
│      World Map - Meadows of Egril          │
│                                             │
│  ┌──────────┐  ┌──────────┐               │
│  │ Level 1  │  │ Level 2  │               │
│  │ Unlocked │  │ Locked   │               │
│  └──────────┘  └──────────┘               │
│  ┌──────────┐  ┌──────────┐               │
│  │ Level 3  │  │ Editor   │               │
│  │ Locked   │  │          │               │
│  └──────────┘  └──────────┘               │
│                                             │
│  [Load Game] [Rules] [Back]                │
└─────────────────────────────────────────────┘
```

### Game Play Screen (Header)
```
┌─────────────────────────────────────────────────────┐
│ 💰 100  ❤️ 10  🎯 Turn 1   Level Name   [⚙️][💾][Map][◀]│
└─────────────────────────────────────────────────────┘
  Coins   HP    Turn Info    (centered)   Settings Save Back Toggle
```

### Level Complete Screen
```
┌─────────────────────────────────────────────┐
│                              [⚙️ Settings]   │
│                                             │
│                  👑                         │
│                                             │
│              Victory!                       │
│                                             │
│    You have successfully defended Egril!   │
│                                             │
│         [Retry]  [World Map]               │
└─────────────────────────────────────────────┘
```

## Settings Dialog

When settings button is clicked:

```
┌─────────────────────────────────────────────┐
│                                             │
│  ┌─────────────────────────────┐           │
│  │ Settings                    │           │
│  │─────────────────────────────│           │
│  │                             │           │
│  │ Language                    │           │
│  │ ┌─────────────────────────┐ │           │
│  │ │ 🇬🇧 English         ▼  │ │           │
│  │ └─────────────────────────┘ │           │
│  │                             │           │
│  │─────────────────────────────│           │
│  │                             │           │
│  │                      [Close]│           │
│  └─────────────────────────────┘           │
│                                             │
└─────────────────────────────────────────────┘
```

## Language Chooser Expanded

When language dropdown is clicked:

```
┌─────────────────────────────────┐
│ 🇬🇧 English               ▲   │
├─────────────────────────────────┤
│ 🇬🇧 English                    │  ← Current selection
│ 🇩🇪 Deutsch (German)           │  (Will appear when German added)
│ 🇫🇷 Français (French)          │  (Will appear when French added)
│ 🇪🇸 Español (Spanish)          │  (Will appear when Spanish added)
└─────────────────────────────────┘
```

## Color Scheme

- **Settings Icon**: Material Theme onSurface color (adapts to light/dark theme)
- **Dialog Background**: Material Theme surface color with elevation
- **Language Chooser**: Outlined with MaterialTheme.colorScheme.outline
- **Flags**: Full color from FlagKit library
- **Close Button**: Primary color button

## Responsive Behavior

### Desktop
- Settings button: 32dp icon button
- Dialog: Min width 300dp, max width 500dp
- Language chooser: 56dp height

### Mobile (Android/iOS)
- Settings button: Scaled with UI scale factor
- Dialog: Adapts to screen width with padding
- Touch-friendly 48dp minimum touch targets

### GamePlay Screen
- Settings button integrated into compact header
- Icon-only to save space
- Same size as other header buttons for consistency

## Accessibility

- Settings icon has content description: "Settings"
- Language chooser has descriptive labels
- All interactive elements have minimum 48dp touch targets
- Color contrast meets WCAG AA standards
- Screen reader compatible (icon descriptions provided)

## Notes

- Settings button appears consistently in top-right on most screens
- GamePlay screen integrates it into the header for space efficiency
- Dialog is modal and dismissible (click outside or close button)
- Language changes are reactive and immediate
- Flag icons are vector-based and scale without quality loss
