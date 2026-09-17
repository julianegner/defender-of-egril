# Enemy Sprites

Enemy units can be rendered from directional spritesheets instead of the built-in drawn vector
icons. This is controlled by the **Use enemy sprites** setting (Settings → Level → Appearance),
which is **on by default**. When the setting is on and a spritesheet exists for an enemy, the
sprite is shown; otherwise the game falls back to the drawn vector icon.

## Facing direction

Each enemy remembers the direction it last moved (`Attacker.facing`, a [`HexDirection`]). On the
map the sprite is drawn facing that direction, so a unit looks the way it is travelling. In the
enemy info area and planned-spawn previews the **center** frame of the spritesheet is used as a
neutral portrait.

The six movement directions correspond to the six neighbours of a pointy-top hexagon and are
computed by `hexDirectionBetween(from, to)`:

```
E, NE, NW, W, SW, SE
```

## Spritesheet convention

Add one spritesheet PNG per enemy type at:

```
frontend/composeApp/src/commonMain/composeResources/files/sprites/{key}.png
```

> **Important:** Place spritesheets under `files/sprites/`; that is the correct long-term
> location. For backward compatibility, the Gradle build now rewrites any legacy
> `drawable/sprites/` resources into `files/sprites/` before Compose resource accessor generation
> runs, so older local checkouts still compile.

where `{key}` is `sprite_` followed by a canonical lower-case name (see table below):

| AttackerType | File                    |
| ------------ | ----------------------- |
| GOBLIN       | `sprite_goblin.png`     |
| ORK          | `sprite_orc.png`        |
| OGRE         | `sprite_ogre.png`       |
| SKELETON     | `sprite_skeleton.png`   |
| EVIL_WIZARD  | `sprite_evil_mage.png`  |
| BLUE_DEMON   | `sprite_blue_demon.png` |
| RED_DEMON    | `sprite_red_demon.png`  |
| RED_WITCH    | `sprite_red_witch.png`  |
| GREEN_WITCH  | `sprite_green_witch.png`|
| EWHAD        | `sprite_ewhad.png`      |
| DRAGON       | `sprite_dragon.png`     |

Each sheet is a **3 × 3 grid of equally-sized frames**, laid out as follows:

```
+--------+--------+--------+
|   SE   |   S    |   SW   |
+--------+--------+--------+
|   E    |  Ctr   |   W    |
+--------+--------+--------+
|   NE   |   N    |   NW   |
+--------+--------+--------+
```

- The **S** and **N** columns exist in the spritesheet but are not used by the movement system
  (the hex grid has six directions, not eight).
- The **center (Ctr)** frame is the neutral portrait shown in the enemy info area and
  planned-spawn previews.
- `frameWidth  = imageWidth  / 3`
- `frameHeight = imageHeight / 3`

Loading, caching and frame cropping are handled by
`de.egril.defender.ui.EnemySpriteProvider`. If a sheet is missing or the setting is off, the
provider returns `null` and the drawn icon is used, so the game keeps working without any sprite
assets.
