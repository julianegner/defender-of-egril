# Save Button Icon Replacement

## Change
Replaced the 💾 unicode emoji in the save button with an SVG icon for better cross-platform compatibility and visual consistency.

## Files Changed

### 1. New SVG Icon
**File**: `composeApp/src/commonMain/composeResources/drawable/emoji_save.svg`

Created a custom floppy disk/save icon SVG with:
- Blue gradient background (matching game theme)
- Label area at top (white rectangle)
- Metal shutter detail (gray with lines)
- Write protection notch
- Clean, recognizable design at any size

### 2. Icon Utility Function
**File**: `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/IconUtils.kt`

Added `SaveIcon()` composable function:
```kotlin
@Composable
fun SaveIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_save),
        contentDescription = "Save",
        modifier = modifier.size(size)
    )
}
```

Also added import for `emoji_save` resource.

### 3. GamePlayScreen Update
**File**: `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/GamePlayScreen.kt`

**Before** (line 429):
```kotlin
Text("💾", fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
```

**After**:
```kotlin
SaveIcon(size = 16.dp, modifier = Modifier.align(Alignment.CenterVertically))
```

## Benefits

### Cross-Platform Compatibility
- ✅ Consistent appearance across Android, iOS, and Desktop
- ✅ No font-dependent rendering (emojis can vary by system)
- ✅ Guaranteed visual fidelity

### Visual Consistency
- ✅ Matches other icons in the game (all use SVG)
- ✅ Scales perfectly at any size
- ✅ Professional, polished appearance

### Maintainability
- ✅ Easy to customize colors/styling
- ✅ Follows established icon pattern in project
- ✅ Standard Compose resource loading

## Icon Design Details

The floppy disk icon features:
- **Size**: 128x128 viewBox (scales to any size)
- **Main body**: Blue gradient (#4A90E2 to #357ABD)
- **Label area**: Dark gray (#34495E) with white label rectangle
- **Shutter**: Gray (#95A5A6, #BDC3C7) with detail lines
- **Border**: Dark outline (#2C3E50) for definition
- **Rounded corners**: 4px radius for modern look

## Testing
- Resource generation: ✅ Successful
- Icon follows same pattern as other emoji_*.svg files
- Size (16.dp) matches original emoji size intent
- Alignment preserved with `Modifier.align(Alignment.CenterVertically)`
