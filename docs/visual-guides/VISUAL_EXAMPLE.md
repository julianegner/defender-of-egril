# Visual Example of Game Grid with New Icons

This file provides a text-based mockup of what the game grid looks like with the new graphical icons.

## Before (Text-Based Display)

```
┌─────────┬─────────┬─────────┬─────────┐
│   S     │  Path   │  Path   │  Path   │  Spawn points and path
├─────────┼─────────┼─────────┼─────────┤
│  Bow    │  Path   │ Goblin  │  Path   │  Tower and enemy as text
│ Tower   │         │ 15/20   │         │
│  Lvl 2  │         │         │         │
│  ⚡1    │         │         │         │
├─────────┼─────────┼─────────┼─────────┤
│ Island  │  Path   │  Path   │    T    │  Build zones and target
└─────────┴─────────┴─────────┴─────────┘
```

## After (Icon-Based Display)

```
┌─────────┬─────────┬─────────┬─────────┐
│   S     │  Path   │  Path   │  Path   │  Spawn point (unchanged)
├─────────┼─────────┼─────────┼─────────┤
│ ╔═╗╔═╗  │  Path   │   @     │  Path   │  Bow tower icon with bow
│ ║ ⌒→║  │         │  /█\    │         │  symbol and Goblin with
│ ╲   ╱  │         │  ▓▓▓    │         │  green face & pointy ears
│  L2⚡1  │         │  10/20  │         │  Level, actions, HP shown
├─────────┼─────────┼─────────┼─────────┤
│ Island  │  Path   │  Path   │    T    │  Build zones (unchanged)
└─────────┴─────────┴─────────┴─────────┘
```

## Icon Key

### Tower Icons (All have trapezoid base with battlements)

- **Spike Tower**: `╔═╗ ║▲▲▲║` - Three upward spikes in yellow
- **Spear Tower**: `╔═╗ ║ ↑ ║` - Vertical spear with silver head
- **Bow Tower**: `╔═╗ ║⌒→ ║` - Curved bow with arrow
- **Wizard Tower**: `╔═╗ ║ ★ ║` - Gold star (magic symbol)
- **Alchemy Tower**: `╔═╗ ║ ⚗ ║` - Green potion flask
- **Ballista Tower**: `╔═╗ ║═→ ║` - Crossbow with bolt

### Enemy Icons

- **Goblin**: `@` with pointy ears - Small green creature
- **Ork**: `Θ` with tusks - Large olive green warrior
- **Ogre**: `◉` with big eyes - Huge brown brute
- **Skeleton**: `☠` - White skull and crossbones
- **Evil Wizard**: `Ψ` with hat - Purple wizard with staff
- **Witch**: `λ` with hat - Green witch with broom

## Color Coding

### Tower Background Colors:
- **Blue (#2196F3)**: Tower is ready with actions
- **Gray (#9E9E9E)**: Tower is being built
- **Blue-gray (#7986CB)**: Tower has no actions remaining

### Enemy Background Colors:
- **Red (#F44336)**: All enemy units

### Build Area Colors:
- **Light Green (#8BC34A)**: Build islands
- **Medium Green (#A5D6A7)**: Build strips
- **Cream (#FFF8DC)**: Enemy path
- **Gray (#E0E0E0)**: Non-playable area

## Status Overlays

All icons maintain the critical game information overlays:

- **Towers**: 
  - Level: `L1`, `L2`, `L3`, etc. (white text, bottom)
  - Actions: `⚡1` (yellow, when available)
  - Build time: `⏱2` (orange, when building)

- **Enemies**:
  - Health: `15/20` (white text, bottom)

## Grid Cell Size

Each cell is **48dp × 48dp**, which provides enough space for:
1. The icon graphic (main area)
2. Status text overlays (bottom 12dp)
3. Borders for selection/highlighting (2-5dp)

The icons are designed to be clear and distinguishable even at this compact size.
