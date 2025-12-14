# Raft on River - Visual Example

This document describes what a raft on a river looks like in the Defender of Egril game.

## Visual Layout

```
Game Map (Hexagonal Grid):
┌─────────────────────────────────────────┐
│                                         │
│  [Build]  [Build]  [River→] [River→]   │
│                      ╔════╗   ~  ~  ~   │
│  [Path]   [Path]    ║BOW ║  [River→]   │
│     ↓       ↓       ╚════╝              │
│                     RAFT ON RIVER       │
│  [Path]   [Spike]   [River→] [River→]  │
│     ↓     TOWER                         │
│                                         │
└─────────────────────────────────────────┘
```

## Key Features Shown:

1. **River Tiles** (indicated by ~~~~~): 
   - Blue-tinted hexagonal tiles
   - Arrows show flow direction (→)
   - Flow speed determines movement distance

2. **Raft with Tower** (╔BOW╗):
   - Bow tower placed on a river tile
   - Tower is displayed with a raft base/platform underneath
   - Tower can still attack enemies within range
   - Raft moves with river flow at end of turn

3. **Regular Tower on Land**:
   - Spike tower on a build area (not on river)
   - For comparison - doesn't move

## Game State Details:

- **Tower on Raft**: Bow Tower (Level 1)
  - Position: River tile at (10, 3)
  - Has full attack capabilities
  - Will move 1 tile east each turn with river flow
  - Can be destroyed if it reaches a maelstrom or map edge

- **River Properties**:
  - Flow Direction: EAST (→)
  - Flow Speed: 1 (moves 1 tile per turn)
  - Blocks: Bridges can block raft movement

## Raft Behavior:

✓ Towers placed on river tiles create rafts automatically
✓ Rafts move with the river flow at end of each turn
✓ Movement distance = flow speed (1 or 2 tiles)
✓ Rafts are destroyed at maelstroms or map boundaries
✓ Bridges block raft movement
✓ Multiple rafts move in order (downstream first)
