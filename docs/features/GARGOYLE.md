# Gargoyle Neutral Unit

Gargoyles are pre-placed guardians that the player does not control. They are placed in the Level Editor under the **Neutral units** section of the Initial Setup tab, on path or buildable tiles.

## Stats

| Property | Value |
|----------|-------|
| Health | 500 × level |
| Attacks per turn | 2 |
| Attack range | 2 tiles |
| Damage per attack | 50 × level |
| XP when destroyed | 50 × level (same as a dragon; paid at level end) |
| Coins when destroyed | None |

## Inactive Form (stone)

- Stands on its tile and does not move.
- Attacks enemies twice at the end of the player turn, after the normal tower auto-attacks, even when the player does not attack.
- Can be hit by player towers. Auto-targeting does **not** pick inactive gargoyles; the player must target them manually.
- Fireball (area) and acid (lasting) attacks hit gargoyles in their area, whether active or inactive. Manual single-target attacks hit them too.
- Pike, spear and bow towers consume their action but deal no damage. The gargoyle shows the immunity shield (like a red demon against fireball) when one of these towers is in range.
- Destroying it grants XP only.

## Blocking

- No unit may move onto a tile where an inactive gargoyle stands (enemies, flyers, dragons, spawns, and portal redirects).
- An active gargoyle cannot move onto a tile occupied by another unit, even on a target tile.

## Placement

- While an inactive gargoyle stands on a tile, the player cannot place towers (including on barricade tower bases), barricades, traps, magical traps, or fiefs there.
- Gargoyles on build-area tiles therefore block tower placement on that tile.

## Activation

- When an evil wizard or evil mage is within 2 tiles, the gargoyle becomes an **active** enemy unit for 3 turns, keeping its current health.
- Active form: red eyes and wings spread wider.
- The unit info area shows an active-specific description (`gargoyle_active_description`) instead of the stone-guardian text.
- Active gargoyles walk the enemy path at speed 2 tiles per turn, and count as enemy units.
- After 3 turns it returns to the inactive form and stands where it is.

## Active Gargoyle Reaching the Target

- The player loses HP equal to the gargoyle's level.
- The gargoyle is removed (it does not return to the inactive form).

## Kills and Rewards

- Kills made by a gargoyle's attacks still award coins and kill counts to the player as normal.
- Destroying a gargoyle (active or inactive) grants XP only: no coins, no kill counts.

## Implementation

- Model: `model/Gargoyle.kt` (`Gargoyle`, `GargoyleRules`), `GARGOYLE` in `AttackerType` (`model/Attacker.kt`).
- Logic: `game/GargoyleLogic.kt` (attacks, activation, countdown); `game/CombatSystem.kt` (manual hits, XP on destruction); `game/AutoAttackSelector.kt` (pike/spear/bow exclusion).
- Placement: `editor/EditorModels.kt` (`InitialGargoyle`), `ui/editor/level/initialsetup/`.
- Rendering: `ui/icon/enemy/GargoyleSymbol.kt`, `GameMap.kt`.
- Tests: `GargoyleLogicTest`, `GargoyleCombatTest`.
