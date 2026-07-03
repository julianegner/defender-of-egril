# Enemy Sprites

Enemy units can be rendered from directional spritesheets instead of the built-in drawn vector
icons. This is controlled by the **Use enemy sprites** setting (Settings → Level → Appearance),
which is **on by default**. When the setting is on and a spritesheet exists for an enemy, the
sprite is shown; otherwise the game falls back to the drawn vector icon.

## Facing direction

Each enemy remembers the direction it last moved (`Attacker.facing`, a [`HexDirection`]). On the
map the sprite is drawn facing that direction, so a unit looks the way it is travelling. In the
enemy info area the sprite is always shown facing forward (`HexDirection.DEFAULT`).

The six directions correspond to the six neighbours of a pointy-top hexagon and are computed by
`hexDirectionBetween(from, to)`:

```
E, NE, NW, W, SW, SE
```

## Spritesheet convention

Add one spritesheet PNG per enemy type at either of these locations (the first that exists wins):

```
frontend/composeApp/src/commonMain/composeResources/files/sprites/{key}.png      (recommended, works on all platforms)
frontend/composeApp/src/commonMain/composeResources/drawable/sprites/{key}.png
```

where `{key}` is the lower-cased `AttackerType` name:

| AttackerType | File               |
| ------------ | ------------------ |
| GOBLIN       | `goblin.png`       |
| ORK          | `ork.png`          |
| OGRE         | `ogre.png`         |
| SKELETON     | `skeleton.png`     |
| EVIL_WIZARD  | `evil_wizard.png`  |
| BLUE_DEMON   | `blue_demon.png`   |
| RED_DEMON    | `red_demon.png`    |
| RED_WITCH    | `red_witch.png`    |
| GREEN_WITCH  | `green_witch.png`  |
| EWHAD        | `ewhad.png`        |
| DRAGON       | `dragon.png`       |

Each sheet is a single **horizontal strip of 6 equally-sized frames**, one per direction, laid out
left-to-right in the order `E, NE, NW, W, SW, SE`. The frame for a direction is located at
`frameWidth * direction.ordinal`, where `frameWidth = imageWidth / 6`.

```
+--------+--------+--------+--------+--------+--------+
|   E    |   NE   |   NW   |   W    |   SW   |   SE   |
+--------+--------+--------+--------+--------+--------+
```

Loading, caching and frame cropping are handled by
`de.egril.defender.ui.EnemySpriteProvider`. If a sheet is missing or the setting is off, the
provider returns `null` and the drawn icon is used, so the game keeps working without any sprite
assets.

> **Note:** If your spritesheets use a different layout (e.g. a different frame order or a grid),
> adjust the frame math in `EnemySpriteProvider.framePainter` and the ordering documented above.
