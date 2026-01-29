# Tile Icon Size Visual Comparison

## Before and After

### Before Changes
```
Barricade Icon:  ████████  (48dp - large, highly visible)
Trap Icon:       ███       (20dp - small, hard to see)
Magical Trap:    ████      (24dp - small, hard to see)
```

### After Changes
```
Barricade Icon:  ████████  (48dp - large, highly visible)
Trap Icon:       ████████  (48dp - large, highly visible)
Magical Trap:    ████████  (48dp - large, highly visible)
```

## Size Increases

| Icon Type | Old Size | New Size | Increase |
|-----------|----------|----------|----------|
| Dwarven Trap | 20dp | 48dp | +140% (2.4x larger) |
| Magical Trap | 24dp | 48dp | +100% (2x larger) |
| Trap Preview | 24dp | 48dp | +100% (2x larger) |
| Barricade | 48dp | 48dp | No change |

## Visual Impact

### On Game Map
- All tile-based obstacles (traps and barricades) now have uniform visual weight
- Traps are more noticeable and easier to identify at a glance
- Better visual hierarchy - all defensive structures have consistent sizing

### During Trap Placement
- Preview icons are now the same size as the final placed trap
- Provides better visual feedback about the trap size
- More accurate representation of what will be placed

## Game Elements Affected

### Dwarven Traps
- Icon: Trap symbol (⚠️ style icon)
- Display: Icon + damage number (e.g., "-10")
- Location: On path tiles where dwarven mines have placed traps
- **Size change**: 20dp → 48dp

### Magical Traps
- Icon: Pentagram symbol (⭐ style icon)
- Display: Icon only (no damage display)
- Location: On path tiles where wizard towers have placed magical traps
- **Size change**: 24dp → 48dp

### Barricades
- Icon: Wood/barricade symbol (🪵 style icon)
- Display: Icon + HP value
- Location: On path tiles where spike/spear towers have built barricades
- **Size**: Unchanged at 48dp (reference size)

### Trap Previews
- Icon: Semi-transparent trap or pentagram symbol
- Display: Shows on hover during trap placement mode
- Location: Valid path tiles when placing traps
- **Size change**: 24dp → 48dp

## User Experience Benefits

1. **Improved Visibility**: Players can now easily spot traps on the map without needing to zoom in
2. **Visual Consistency**: All defensive tile elements have the same size, creating a more cohesive visual design
3. **Better Planning**: Larger trap previews help players visualize trap placement more accurately
4. **Reduced Eye Strain**: Larger icons are easier to see, especially during extended gameplay sessions
5. **Accessibility**: Larger icons benefit players with visual impairments

## Technical Implementation

All icon sizes are now defined in `GamePlayConstants.TileIconSizes`:
```kotlin
object TileIconSizes {
    val Trap = 48.dp
    val Barricade = 48.dp
    val TrapPreview = 48.dp
}
```

This centralized approach ensures:
- Easy maintenance and updates
- Consistent sizing across all tile-based elements
- Clear documentation for future developers
- Simple to adjust if needed (change in one place)

## Backward Compatibility

- No changes to game logic or mechanics
- No changes to save file format
- Visual-only change with no gameplay impact
- All existing levels and saves work identically
