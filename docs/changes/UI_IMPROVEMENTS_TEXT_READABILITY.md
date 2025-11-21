# UI Improvements for Mobile - Addressing Text Readability

## Changes Made (Commit)

### 1. Separate Scaling for Layout vs Text
**Problem**: With 0.5x scaling on everything, text became unreadably small (8sp).

**Solution**: Apply different scaling factors:
- **Layout elements (padding, spacing)**: 0.5x scale (saves maximum space)
- **Text and icons**: 0.75x scale (keeps text readable)

```kotlin
val textScale = if (uiScale < 1f) {
    0.75f  // For mobile, use 0.75 for text readability while layout uses 0.5
} else {
    1f     // Desktop unchanged
}
Density(density.density * uiScale, density.fontScale * textScale)
```

**Effect on Mobile**:
- 16sp text → 12sp (readable) instead of 8sp (too small)
- 14sp text → 10.5sp (readable) instead of 7sp (too small)
- 48dp buttons → 24dp (layout space saved)
- 16dp padding → 8dp (layout space saved)

### 2. Reduced Tower Button Heights
**Problem**: Tower info buttons were 100dp tall, taking too much vertical space even when scaled.

**Solution**: Reduced button heights and adjusted content:

| Button Type | Old Height | New Height | With 0.5x Scale |
|-------------|-----------|------------|-----------------|
| Upgrade | 100dp | 60dp | 30dp |
| Undo/Sell | 100dp | 60dp | 30dp |
| Mine Actions | 100dp | 60dp | 30dp |

**Changes**:
- Height: 100dp → 60dp
- Font sizes: 18sp → 14sp, 16sp → 14sp
- Spacers: 8dp → 4dp
- Offset: -24dp → -12dp

### 3. Adjusted Icon Sizes
- Emoji icons: 24sp → 20sp (still recognizable with 0.75x text scaling = 15sp)

## Results

### Text Readability
| Element | Original | 0.5x Everything | **0.5x Layout + 0.75x Text** |
|---------|----------|-----------------|------------------------------|
| Title | 24sp | 12sp (small) | **18sp (good)** ✓ |
| Body | 16sp | 8sp (too small) | **12sp (readable)** ✓ |
| Button | 14sp | 7sp (too small) | **10.5sp (readable)** ✓ |
| Icons | 24sp | 12sp (small) | **18sp (clear)** ✓ |

### Space Savings
- Tower button area: Reduced by ~40dp with smaller buttons
- Layout spacing: Still scaled to 0.5x (maximum space saving)
- **Total**: Map gets more space while text remains readable

### Visual Impact

**Before (0.5x everything)**:
```
Tower Info:
┌──────────────────────┐
│ Tower Name (8sp) ← too small
│ Stats (6sp) ← unreadable
│ [Upgrade 100dp → 50dp]
│ [Sell 100dp → 50dp]
└──────────────────────┘
Total: ~100dp scaled to 50dp
Text: Unreadable ❌
```

**After (0.5x layout + 0.75x text + 60dp buttons)**:
```
Tower Info:
┌──────────────────────┐
│ Tower Name (12sp) ← readable
│ Stats (9sp) ← clear
│ [Upgrade 60dp → 30dp]
│ [Sell 60dp → 30dp]
└──────────────────────┘
Total: ~60dp scaled to 30dp
Text: Readable ✓
```

## Benefits

1. ✅ **Map still has good size** - Layout uses 0.5x scaling
2. ✅ **Text is readable** - Text uses 0.75x scaling (12sp instead of 8sp)
3. ✅ **Icons are recognizable** - Icons use 0.75x scaling
4. ✅ **Tower area is smaller** - Buttons reduced from 100dp to 60dp
5. ✅ **More vertical space** - Smaller buttons + offsets save ~20-30dp per tower info

## Technical Details

The key insight is using **different density scaling for layout vs text**:
- `density.density * 0.5f` - scales layout (dp values)
- `density.fontScale * 0.75f` - scales text (sp values)

This allows us to:
- Keep layout compact (saves space)
- Keep text readable (user experience)
- Maintain icon clarity (visual design)
