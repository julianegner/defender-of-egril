# Enemy Level Display - Visual Implementation Guide

## Overview
This document provides a visual guide to the enemy level display feature implementation.

## 1. Enemy List Panel - Level Badge Display

### Before (without level badge):
```
┌─────────────────────────────────────┐
│ On Map:                             │
├─────────────────────────────────────┤
│ ┌───────────────────────────────┐  │
│ │ [👹] Goblin                   │  │
│ │      HP: 20/20                │  │
│ │      Pos: (5,3)               │  │
│ └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### After (with level badge for level > 1):
```
┌─────────────────────────────────────┐
│ On Map:                             │
├─────────────────────────────────────┤
│ ┌───────────────────────────────┐  │
│ │ [👹] Goblin Lv3               │  │  ← Red badge added
│ │      HP: 60/60                │  │  ← Health scaled by level
│ │      Pos: (5,3)               │  │
│ └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

## 2. Enemy Click Interaction Flow

### Step 1: Normal View (No Selection)
```
┌──────────────────────────────────────────────────────────────────┐
│ Game Map                                                          │
│                                                                    │
│  [Path] [Enemy🔴] [Path] [Tower🔵] [Build]                       │
│  [Path] [Path]    [Path] [Path]    [Build]                       │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘

Bottom Panel:
┌──────────────────────────────────────────────────────────────────┐
│ Buy Towers:                                                       │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐              │
│ │Pike│ │Spear│ │Bow │ │Wiz │ │Alch│ │Bal │ │Mine│              │
│ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘              │
│                                                                    │
│                      [Start Battle / End Turn]                    │
└──────────────────────────────────────────────────────────────────┘
```

### Step 2: Click on Enemy (NEW FEATURE)
```
┌──────────────────────────────────────────────────────────────────┐
│ Game Map                                                          │
│                                                                    │
│  [Path] [Enemy🔴*] [Path] [Tower🔵] [Build]  ← Enemy selected   │
│  [Path] [Path]     [Path] [Path]    [Build]                      │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘

Bottom Panel (NEW LAYOUT):
┌──────────────────────────────────────────────────────────────────┐
│ ┌──────────────────────────────┐ ┌────────────────────────────┐ │
│ │ Enemy Details:               │ │ Buy Towers (4x2):          │ │
│ │                              │ │ ┌───┐┌───┐┌───┐┌───┐      │ │
│ │  [Large Enemy Icon]          │ │ │Pike││Spear││Bow││Wiz│    │ │
│ │                              │ │ └───┘└───┘└───┘└───┘      │ │
│ │  Evil Mage Lv3               │ │ ┌───┐┌───┐┌───┐┌───┐      │ │
│ │  HP: 90/120                  │ │ │Alch││Bal││Mine││End│     │ │
│ │  Position: (8,4)             │ │ └───┘└───┘└───┘│Turn│     │ │
│ │                              │ │                └───┘      │ │
│ │  ⚡ Can summon minions       │ │                            │ │
│ │                              │ │                            │ │
│ └──────────────────────────────┘ └────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
     ↑                                      ↑
  Enemy info                         Buttons moved right
  (same position as tower details)
```

### Step 3: Click on Tower (Existing Feature, for Comparison)
```
┌──────────────────────────────────────────────────────────────────┐
│ Game Map                                                          │
│                                                                    │
│  [Path] [Enemy🔴] [Path] [Tower🔵*] [Build]  ← Tower selected   │
│  [Path] [Path]    [Path] [Path]     [Build]                      │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘

Bottom Panel:
┌──────────────────────────────────────────────────────────────────┐
│ ┌──────────────────────────────┐ ┌────────────────────────────┐ │
│ │ Tower Details:               │ │ Buy Towers (4x2):          │ │
│ │                              │ │ ┌───┐┌───┐┌───┐┌───┐      │ │
│ │  [Large Tower Icon]          │ │ │Pike││Spear││Bow││Wiz│    │ │
│ │                              │ │ └───┘└───┘└───┘└───┘      │ │
│ │  Bow Tower                   │ │ ┌───┐┌───┐┌───┐┌───┐      │ │
│ │  Level 2                     │ │ │Alch││Bal││Mine││End│     │ │
│ │  Damage: 15  Range: 3        │ │ └───┘└───┘└───┘│Turn│     │ │
│ │  Actions: 1/1                │ │                └───┘      │ │
│ │                              │ │                            │ │
│ │  [Upgrade] [Sell] [Attack]   │ │                            │ │
│ └──────────────────────────────┘ └────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

## 3. Enemy Abilities Display

Different enemies show different abilities:

### Evil Mage (Can Summon):
```
┌──────────────────────────────┐
│ [Icon] Evil Mage Lv2         │
│        HP: 60/80             │
│        Position: (8,4)       │
│                              │
│ ⚡ Can summon minions        │
└──────────────────────────────┘
```

### Red Witch (Can Disable):
```
┌──────────────────────────────┐
│ [Icon] Red Witch Lv1         │
│        HP: 25/30             │
│        Position: (10,5)      │
│                              │
│ 🔒 Can disable towers        │
└──────────────────────────────┘
```

### Green Witch (Can Heal):
```
┌──────────────────────────────┐
│ [Icon] Green Witch Lv1       │
│        HP: 25/25             │
│        Position: (6,3)       │
│                              │
│ 💚 Can heal other enemies    │
└──────────────────────────────┘
```

### Blue Demon (Immune to Acid):
```
┌──────────────────────────────┐
│ [Icon] Blue Demon Lv4        │
│        HP: 60/60             │
│        Position: (12,4)      │
│                              │
│ 🛡️ Immune to acid           │
└──────────────────────────────┘
```

### Red Demon (Immune to Fireball):
```
┌──────────────────────────────┐
│ [Icon] Red Demon Lv2         │
│        HP: 120/120           │
│        Position: (9,6)       │
│                              │
│ 🛡️ Immune to fireball       │
└──────────────────────────────┘
```

## 4. Level Badge Behavior

| Enemy Level | Display             | Color | Notes                |
|-------------|---------------------|-------|----------------------|
| 1           | (no badge)          | N/A   | Default, not shown   |
| 2           | "Lv2"              | Red   | Small text           |
| 3           | "Lv3"              | Red   | Small text           |
| 5           | "Lv5"              | Red   | Small text           |
| 10          | "Lv10"             | Red   | Small text           |

## 5. Multi-Language Support

The ability descriptions are localized:

| Ability              | English                  | German                      | Spanish                    |
|----------------------|--------------------------|----------------------------|----------------------------|
| Can summon           | Can summon minions       | Kann Diener beschwören     | Puede invocar secuaces     |
| Can heal             | Can heal other enemies   | Kann andere Feinde heilen  | Puede curar otros enemigos |
| Can disable towers   | Can disable towers       | Kann Türme deaktivieren    | Puede desactivar torres    |
| Immune to acid       | Immune to acid           | Immun gegen Säure          | Inmune al ácido            |
| Immune to fireball   | Immune to fireball       | Immun gegen Feuerball      | Inmune a la bola de fuego  |

(French and Italian translations also available)

## 6. Interaction States

### Selecting Enemy:
- Click enemy tile → Enemy details appear, buy buttons move right
- Click same enemy again → Deselect, return to normal layout
- Click different enemy → Switch to new enemy details
- Click tower → Enemy deselected, tower details shown instead

### Selecting Tower:
- Click tower tile → Tower details appear, buy buttons move right
- Click same tower again → Deselect, return to normal layout
- Click enemy → Tower deselected, enemy details shown instead

## 7. Responsive Design

### Desktop (uiScale = 1.0):
- Icon size: 96dp
- Button height: 60dp
- Full spacing and padding

### Mobile (uiScale < 1.0):
- Icon size: 64dp
- Button height: 100dp
- Reduced spacing and padding (0.5x)
- Text scaled up (1.5x) for readability

## Implementation Notes

- Enemy level defaults to 1 if not specified
- Health calculation: `maxHealth = baseHealth × level`
- Level badge only shows when level > 1
- UI layout mirrors tower selection pattern
- Compatible with save/load system
- Works with level editor (Desktop/WASM only)
