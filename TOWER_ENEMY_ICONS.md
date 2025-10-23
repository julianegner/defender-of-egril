# Tower and Enemy Icon Designs

This document describes the visual design of the tower and enemy icons implemented in the game.

## Overview

Previously, towers and enemies were displayed as text labels. Now they are displayed as **graphical icons** drawn using Jetpack Compose Canvas primitives.

### Example Visual Comparison

**Before (Text-based):**
```
┌──────┐
│ Bow  │  ← Just text on colored background
│Tower │
│ Lvl 1│
│ ⚡1  │
└──────┘
```

**After (Icon-based):**
```
┌──────┐
│  ╱╲  │  ← Tower battlements
│ ╱  ╲ │  ← Tower structure
│ ⌒ →  │  ← Bow and arrow symbol
│ L1⚡1 │  ← Level and actions
└──────┘
```

## Tower Icons

All towers are displayed with a **trapezoid base with battlements** (like castle towers), and each has a unique symbol inside representing its type.

### Tower Visual Structure

```
    ╔═╗ ╔═╗ ╔═╗   ← Battlements (3 small squares on top)
    ║   ║   ║
    ╲   Tower  ╱   ← Trapezoid base (wider at bottom)
     ╲ Symbol ╱    ← Type-specific symbol in center
      ╲      ╱
       ╲____╱
        L2⚡1       ← Level and actions overlay
```

### Tower Design Details

1. **Spike Tower**
   - Base: White outlined trapezoid with battlements on top
   - Symbol: Three upward-pointing yellow spikes
   - Level: Displayed as "L1", "L2", etc. in white text at bottom
   - Actions: Shown as "⚡1" in yellow when ready and has actions

2. **Spear Tower**
   - Base: White outlined trapezoid with battlements
   - Symbol: Vertical brown spear with silver spearhead
   - Level and Actions: Same overlay as Spike Tower

3. **Bow Tower**
   - Base: White outlined trapezoid with battlements
   - Symbol: Brown curved bow with string and an arrow
   - Level and Actions: Same overlay

4. **Wizard Tower**
   - Base: White outlined trapezoid with battlements
   - Symbol: Gold 5-pointed star with orange outline and white sparkle in center
   - Level and Actions: Same overlay

5. **Alchemy Tower**
   - Base: White outlined trapezoid with battlements
   - Symbol: Green transparent potion flask with gray neck and green bubbles
   - Level and Actions: Same overlay

6. **Ballista Tower**
   - Base: White outlined trapezoid with battlements
   - Symbol: Brown crossbow mechanism with bolt
   - Level and Actions: Same overlay

### Tower Status Indicators

- **Building**: When not ready, shows "⏱2" (or number of turns remaining) in orange
- **Ready with Actions**: Shows "⚡1" in yellow
- **Used Actions**: No action indicator shown
- **Level**: Always shows "L1", "L2", etc. in white

## Enemy Icons

Each enemy type has a distinct visual appearance:

### Enemy Design Details

1. **Goblin**
   - Light green circular head with pointy ears
   - Red eyes
   - Small brown body
   - Health bar: "20/20" in white at bottom

2. **Ork**
   - Dark olive green square-ish head
   - White tusks protruding from mouth
   - Yellow eyes
   - Gray armored body

3. **Ogre**
   - Large brown/sienna circular head
   - Big white eyes with black pupils
   - Black mouth line
   - Massive burlywood-colored body

4. **Skeleton**
   - White skull with black eye sockets
   - Black triangular nose hole
   - White crossbones below

5. **Evil Wizard**
   - Indigo pointed wizard hat
   - Purple-ish face
   - Magenta/pink glowing eyes
   - Brown staff with purple orb

6. **Witch**
   - Black pointed hat with brim
   - Greenish face
   - Orange/red eyes
   - Brown broom with golden bristles

### Enemy Status Indicators

- **Health**: Always shows current/max HP in white at bottom (e.g., "15/20")

## Visual Implementation

All icons are drawn using Jetpack Compose Canvas with:
- Paths for complex shapes
- Circles for round elements
- Lines for straight elements
- Rectangles for basic shapes

The icons maintain visibility of critical game information (level, actions, health) while providing clear visual distinction between unit types.

## Color Scheme

- **Tower backgrounds**: Blue when ready (#2196F3), Gray when building (#9E9E9E), Blue-gray when actions used (#7986CB)
- **Enemy backgrounds**: Red (#F44336)
- **Text overlays**: White for maximum contrast
- **Action indicators**: Yellow (#FFFF00)
- **Build time indicators**: Orange (#FFA500)

All icons are designed to work within the 48dp grid cells used in the game board.
