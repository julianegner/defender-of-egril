# Settings Hint Box - Visual Description

## Feature Overview
The settings hint box is a first-run help dialog that appears on the main menu screen to inform users about available settings.

## Visual Layout

### Main Menu Screen (First Run)
```
┌─────────────────────────────────────────────────────────────────────┐
│                                              [⚙️ Settings Icon]    │
│                                              ┌──────────────────────┤
│                                              │  Settings Available  │
│                                              │                      │
│          DEFENDER OF EGRIL                   │  Click the settings  │
│                                              │  icon to customize   │
│   Defend the meadows of Egril               │  your experience.    │
│   against the Hordes of Gleid Thyae         │                      │
│   under the Banner of the evil Ewhad        │  Available settings  │
│                                              │  include:            │
│                                              │  • Appearance        │
│          [Start Game]                        │  • Sound             │
│                                              │  • Controls          │
│          [Rules]                             │  • Language          │
│                                              │  • Difficulty        │
│                                              │                      │
│                                              │  [  Got it!  ]       │
│                                              └──────────────────────┘
│                                                                      │
│  v1.0.0 (abc1234)                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

## Component Details

### Settings Hint Box
- **Position**: Top-right corner, 60dp below the settings icon, 8dp from right edge
- **Width**: 280dp
- **Background**: Primary container color (themed)
- **Elevation**: 8dp shadow
- **Corner Radius**: 12dp
- **Padding**: 16dp

### Content Structure
1. **Title** (titleMedium typography, onPrimaryContainer color)
   - "Settings Available" (or localized equivalent)

2. **Message** (bodySmall typography, onPrimaryContainer color)
   - "Click the settings icon to customize your experience. Available settings include:"

3. **Settings List** (bodySmall typography, onPrimaryContainer color)
   - Appearance (with bullet point)
   - Sound (with bullet point)
   - Controls (with bullet point)
   - Language (with bullet point)
   - Difficulty (with bullet point)
   - Each item has a small circular bullet (6dp, filled)
   - 4dp vertical spacing between items

4. **Dismiss Button** (primary color)
   - Text: "Got it!" (or localized equivalent)
   - Full width of card
   - Material 3 filled button style

## Behavior

### First Run
- Hint box appears automatically
- Rest of UI remains interactive (not blocking)
- User can still click "Start Game" or "Rules" buttons
- Settings button is accessible

### After Dismissal
- User clicks "Got it!" button
- Hint box disappears with smooth transition
- State is persisted to local storage
- Hint will not appear on subsequent launches

### Subsequent Runs
- Hint box does not appear
- Main menu shows normally
- Settings icon remains accessible

## Localization

The hint box supports all game languages:
- **English**: "Settings Available" / "Got it!"
- **German**: "Einstellungen verfügbar" / "Verstanden!"
- **Spanish**: "Configuración Disponible" / "¡Entendido!"
- **French**: "Paramètres Disponibles" / "Compris !"
- **Italian**: "Impostazioni Disponibili" / "Capito!"

## Technical Notes

1. **State Management**: Uses `AppSettings.settingsHintShown` boolean state
2. **Persistence**: Stored in platform-specific settings (multiplatform-settings library)
3. **Theming**: Fully integrated with Material 3 theme (dark mode support)
4. **Platform Support**: Works on Desktop, Android, iOS, and Web/WASM
5. **Accessibility**: Proper semantic content descriptions for screen readers

## Testing

The feature includes comprehensive unit tests:
- ✅ Hint shows on first run
- ✅ Hint can be dismissed
- ✅ Hint doesn't show on subsequent runs
- ✅ UI remains functional with hint present
- ✅ All settings categories are listed

All tests pass successfully.
