# Mobile Text Visibility Fix

## Issue
"Now" and "Up" labels were not showing on Android devices in the tower details card.

## Root Cause
The Text composables for "Now" and "Up" labels were using `style = MaterialTheme.typography.labelSmall` without an explicit `color` parameter. This could cause the text to inherit a color that matches the background, making it invisible.

## Solution
Explicitly set colors for both labels:
- **"Now"**: `Color.Black` for visibility
- **"Up"**: `Color(0xFF4CAF50)` (green) when upgrade is available, `Color.Gray` otherwise

Also removed the `style` parameter and relied on explicit `fontSize` and `fontWeight` parameters for better control.

## Changes Made

### Before
```kotlin
Text(
    "Now",
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp
)
```

### After
```kotlin
Text(
    text = "Now",
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    color = Color.Black
)
```

## Technical Details

The issue was likely that:
1. `MaterialTheme.typography.labelSmall` might have a default color that's not visible on all backgrounds
2. The `style` parameter could be overriding the intended color
3. Without explicit color, the text might inherit from parent composables

By setting explicit colors:
- "Now" is always visible in black
- "Up" uses semantic colors (green for available, gray for unavailable)

## Testing
- Verified code compiles successfully
- Text should now be visible on all Android devices
- Colors provide good contrast against card background
- Text size (10sp → 15sp effective with 1.5x scaling) remains readable

## Files Modified
- `GamePlayScreen.kt`: Updated "Now" and "Up" Text composables with explicit colors
