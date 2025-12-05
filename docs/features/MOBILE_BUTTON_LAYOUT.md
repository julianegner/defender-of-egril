# Mobile Button Layout - Main Menu Screen

## Overview

The Main Menu screen now has platform-specific button layouts:

- **Desktop/Web**: Buttons are displayed in a **vertical column**
- **Mobile (Android/iOS)**: Buttons are displayed in a **horizontal row**

## Implementation Details

### Code Changes

Modified `MenuScreens.kt` to use conditional layout based on `isPlatformMobile`:

```kotlin
// On mobile, buttons are in a row; on desktop, in a column
if (isPlatformMobile) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onStartGame,
            modifier = Modifier.weight(1f).height(60.dp)
        ) {
            Text(stringResource(Res.string.start_game), ...)
        }
        
        Button(
            onClick = onShowRules,
            modifier = Modifier.weight(1f).height(60.dp)
        ) {
            Text(stringResource(Res.string.rules), ...)
        }
    }
} else {
    // Desktop: Vertical column with fixed width buttons
    Button(
        onClick = onStartGame,
        modifier = Modifier.width(200.dp).height(60.dp)
    ) {
        Text(stringResource(Res.string.start_game), ...)
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Button(
        onClick = onShowRules,
        modifier = Modifier.width(200.dp).height(60.dp)
    ) {
        Text(stringResource(Res.string.rules), ...)
    }
}
```

### Platform Detection

Uses `isPlatformMobile` from `Platform.kt`:
- Returns `true` for Android and iOS
- Returns `false` for Desktop/JVM and Web/WASM

## Visual Comparison

### Desktop Layout (Column)

![Desktop Layout](https://github.com/user-attachments/assets/cad8ca0a-d977-400c-af3b-786e47ca32c5)

Buttons are stacked vertically with:
- Fixed width: 200.dp
- Height: 60.dp
- Spacing: 16.dp between buttons

### Mobile Layout (Row)

On mobile devices (Android/iOS), the buttons will appear side-by-side:

```
┌─────────────────────────────────────────┐
│                                         │
│         Defender of Egril               │
│                                         │
│  ┌──────────────┐  ┌─────────────┐    │
│  │ Start Game   │  │   Rules     │    │
│  └──────────────┘  └─────────────┘    │
│                                         │
└─────────────────────────────────────────┘
```

Buttons use:
- `weight(1f)`: Equal width distribution
- Height: 60.dp (same as desktop)
- Spacing: 16.dp between buttons

## Benefits

1. **Better Mobile UX**: Horizontal button layout makes better use of mobile screen width
2. **Consistent Desktop UX**: Vertical layout remains unchanged for desktop users
3. **Platform-Appropriate**: Each platform gets a layout optimized for its form factor

## Testing

- Desktop layout verified with screenshot test
- Mobile layout will be visible when running on Android or iOS devices
- Both layouts maintain button functionality (onClick handlers work correctly)
