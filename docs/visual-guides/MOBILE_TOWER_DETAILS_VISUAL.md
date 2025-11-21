# Mobile Tower Details - Visual Mockup

## Mobile Layout (Landscape, 0.5x scaling)

### Complete Tower Details Card

```
┌─────────────────────────────────────────────────────────────────┐
│ TOWER DETAILS CARD (Mobile - Landscape Mode)                   │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐│
│ │ [🏹] Archer Tower              Level 2  ⚔️ Ranged    ⚡ 2/2││
│ │  16dp                            Text: 18sp (with 1.5x)    ││
│ │                                                             ││
│ │ Now   Up              [Upgrade 💰100]   [Sell 💰150]      ││
│ │ 💥15   💥20                 50dp            50dp           ││
│ │ 🎯4    🎯4              (Taller buttons)                   ││
│ │ ⚡2    ⚡2               (Easy to tap)                      ││
│ │ 10sp  10sp                                                 ││
│ │ (13.5sp stats)                                             ││
│ │ ↑~30% width  ↑~35% width each                             ││
│ └─────────────────────────────────────────────────────────────┘│
│ Height: ~60dp (with 0.5x scaling = 30dp actual)                │
└─────────────────────────────────────────────────────────────────┘
```

## Detailed Breakdown

### Section 1: Tower Header (Top Row)
```
┌──────────────────────────────────────────────────┐
│ [Icon] Tower Name       Level  Type    Actions   │
│  16dp   18sp (27sp*)     15sp  15sp      15sp    │
│                                                   │
│ [🏹]  Archer Tower    Level 2  ⚔️ Ranged  ⚡ 2/2│
└──────────────────────────────────────────────────┘
* With 1.5x text scaling
Height: ~24dp → 12dp with 0.5x layout scaling
```

### Section 2: Stats + Buttons (Main Row) - NEW COMPACT LAYOUT

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│  Stats Column          Button 1         Button 2          │
│  (30% width)          (35% width)      (35% width)        │
│  ┌──────────┐         ┌──────────┐     ┌──────────┐      │
│  │Now   Up  │         │ Upgrade  │     │   Sell   │      │
│  │💥15  💥20│         │  💰100   │     │  💰150   │      │
│  │🎯4   🎯4 │         └──────────┘     └──────────┘      │
│  │⚡2   ⚡2 │          50dp height      50dp height       │
│  │(9sp*)    │          (25dp actual)   (25dp actual)     │
│  └──────────┘                                             │
│                                                            │
└────────────────────────────────────────────────────────────┘
* 9sp stats become 13.5sp with 1.5x text scaling
Height: ~50dp → 25dp with 0.5x layout scaling
```

## Comparison: Before vs After

### Before (4 Equal Columns)
```
┌────────────────────────────────────────────────────────┐
│ Current:  │ Upgrade:  │ [Upgrade] │  [Sell]           │
│ 💥 15     │ 💥 20     │  Button   │  Button           │
│ 🎯 4      │ 🎯 4      │  50dp     │  50dp             │
│ ⚡ 2      │ ⚡ 2      │           │                   │
│ 25%       │ 25%       │ 25%       │ 25%               │
└────────────────────────────────────────────────────────┘

Issues:
❌ Stats labels too long ("Current:", "Upgrade:")
❌ Each column gets equal 25% width (inefficient)
❌ Buttons cramped despite needing more space
❌ Stats take up 50% of width unnecessarily
❌ Potential line breaks with long labels
```

### After (Compact Layout)
```
┌────────────────────────────────────────────────────────┐
│ Now  Up   │    [Upgrade]    │    [Sell]              │
│ 💥15 💥20 │     Button      │    Button              │
│ 🎯4  🎯4  │     50dp        │    50dp                │
│ ⚡2  ⚡2  │                 │                        │
│ 30%       │ 35%             │ 35%                    │
└────────────────────────────────────────────────────────┘

Benefits:
✅ Short labels ("Now", "Up") - no overflow
✅ Stats side-by-side in one column (space efficient)
✅ Buttons get more width (35% each vs 25%)
✅ Compact stats take only 30% width
✅ Better horizontal distribution
```

## Size Calculations (Mobile with 0.5x Layout + 1.5x Text)

### Text Sizes (Effective)
| Element | Declared | With Scaling | Result |
|---------|----------|--------------|--------|
| Tower name | 16sp | ×1.5 | **24sp** ✓ |
| Level/Type | 12sp | ×1.5 | **18sp** ✓ |
| Labels ("Now"/"Up") | 10sp | ×1.5 | **15sp** ✓ |
| Stats (💥🎯⚡) | 9sp | ×1.5 | **13.5sp** ✓ |
| Button text | 14sp | ×1.5 | **21sp** ✓ |

All text remains highly readable!

### Layout Sizes (Physical)
| Element | Declared | With Scaling | Result |
|---------|----------|--------------|--------|
| Card padding | 4dp | ×0.5 | **2dp** |
| Icon size | 32dp | ×0.5 | **16dp** |
| Button height | 100dp | ×0.5 | **50dp** ✓ Tappable |
| Stats row height | ~50dp | ×0.5 | **~25dp** |
| Total card | ~60dp | ×0.5 | **~30dp** ✓ Compact |

## Full Screen Layout (Landscape Mobile)

```
┌──────────────────────────────────────────────────────────────┐
│ 💰250 ❤️10 🔄3   Forest Defense   💾 Map ▶ ▼  (16dp)        │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │  MAP (10x6 grid - Full visibility)                │     │
│  │  S ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰ T                            │     │
│  │  ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰                            │     │
│  │  ☰ ☰ 🏹☰ ☰ 🧙☰ ☰ ☰ 🗼☰     580dp height          │     │
│  │  ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰     (~290dp actual)        │     │
│  │  ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰     83% of screen!          │     │
│  │  ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰ ☰                            │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │ SELECTED TOWER: Archer Tower (30dp actual height) │     │
│  │ [Icon] Now  Up    [Upgrade 💰100]  [Sell 💰150]  │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
│  [🏹 Archer] [🧙 Mage] [🎯 Ballista] [Controls]  (42.5dp)  │
└──────────────────────────────────────────────────────────────┘
   ~800dp width × ~450dp height (typical landscape phone)

TOWER CARD: Only 30dp tall (was ~32.5dp) - 7% smaller!
```

## Key Improvements Summary

1. **Stats Section**: 50% width → 30% width (40% reduction)
2. **Button Space**: 50% width → 70% width (40% increase)
3. **Card Height**: ~32.5dp → ~30dp (7% reduction)
4. **Label Length**: "Current:"/"Upgrade:" → "Now"/"Up" (60% shorter)
5. **Text Readability**: All text remains readable (13.5sp+ effective)

## Result

✅ More compact tower details card
✅ Better space distribution
✅ No line breaks or overflow
✅ Still fully readable and usable
✅ More map space available
