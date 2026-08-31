# First Level Enhancement Ideas

Three concrete suggestions for the level that follows the tutorial (`welcome_to_defender_of_egril`).

## Why change anything?

The current first level after the tutorial, **The First Wave** (`the_first_wave` on `map_straight`),
sends 30 identical level‑1 goblins down a straight corridor over five turns. It teaches nothing new
and shows none of the mechanics the game has gained since: villains, player supports (traps,
barricades, spell tokens, cooldown powers), fiefs, the Waaagh! horde mechanic and scripted story
events. Players who stop after the tutorial never see any of it.

Each suggestion below therefore introduces **one** new mechanic as its hook, stays inside the
beginner tower set (spike, spear, bow) and lasts about ten turns. All three are shipped as playable
official content at the world‑map location *The Beginning*, unlocked directly by the tutorial, so
they can be play‑tested side by side before one of them replaces *The First Wave*.

| # | Level | Map | Hook |
|---|-------|-----|------|
| 1 | Gribnak's Ambush (`gribnaks_ambush`) | `map_goblin_gorge` | First named **villain** + support tokens |
| 2 | Hold the Gate (`hold_the_gate`) | `map_the_watch_gate` | **Barricades / gate** defence + **Waaagh!** |
| 3 | The Golden Road (`the_golden_road`) | `map_golden_road` | **Fiefs**: economy under threat |

## Suggestion 1 — Gribnak's Ambush

*A villain leads the horde.*

- **Map** (`map_goblin_gorge`, 31×15): three spawn points feed a northern, a central and a southern
  lane. The lanes merge in front of a narrow gorge, so everything the player builds around the gorge
  mouth pays off — a first, readable lesson in choke points.
- **Hook**: on turn 5 **Gribnak the Squealer** (`SNOTLING_BOSS`, level 2) walks in. He carries a name
  plate instead of a health bar, rallies snotlings around himself and is the first enemy the player
  cannot simply out‑shoot with one tower.
- **Supports**: two dwarven trap tokens, one freeze‑spell token (spell tokens work even though the
  player has not unlocked any spell yet) and the *Sky is falling* cooldown power.
- **Events**: two emergency barricades when the player drops to four health points.
- **Why it hooks**: it turns a wave of goblins into a duel with a named opponent and hands the player
  three brand‑new toys in a level where they cannot lose much.

## Suggestion 2 — Hold the Gate

*The Waaagh! rolls in.*

- **Map** (`map_the_watch_gate`, 29×19): a northern and a southern road converge on the gate corridor
  of a watch fort; the corridor is the only way to the target.
- **Hook**: the level starts with a **named gate** — two barricades called *The Watch Gate* — and two
  pre‑placed spear towers on the ramparts. The player does not start on an empty field but inherits a
  fortification that is already under attack, and the growing ork pressure charges the
  **Waaagh!** meter until the horde goes into frenzy.
- **Villain**: **Morguk Bonewhisper** arrives on turn 7, speeds up the horde with his war totem and
  hexes the ramparts, forcing the player to repair and rebuild instead of turtling.
- **Supports**: two barricade tokens plus the *Construction repairs* cooldown power, so the gate can
  actually be held.
- **Events**: reinforcements (coins and an extra barricade) after twelve kills, a *hold the line*
  message when Morguk appears and a heal‑spell token at three health points.
- **Why it hooks**: it is a siege, not a corridor. Losing the gate is visible and dramatic, and
  rebuilding it is an immediate, satisfying goal.

## Suggestion 3 — The Golden Road

*Protect the villages.*

- **Map** (`map_golden_road`, 33×15): one long, winding trade road with generous farmland on both
  sides — plenty of room for towers, but a long way to defend.
- **Hook**: three **fiefs** sit on the road (a marketplace early, a quarry in the middle, a
  woodcutter close to the target). They pay coins every turn but are destroyed the moment any enemy
  walks over them, so the player is rewarded for stopping enemies *early* instead of at the last
  tile. Two extra woodcutter tokens and the *Coin surge* cooldown power let the player gamble on
  more income.
- **Villain**: **Zussa** (turn 7) and her red witches shut towers down, which is exactly when the
  early defence line matters most. Her bounty is 100 coins.
- **Events**: an emergency grant when the treasury runs dry, a warning before the witches arrive and
  the villain bounty.
- **Why it hooks**: it gives the player something to protect other than an abstract health counter,
  and every fief that survives is visible proof that their defence worked.

## Files added

```text
frontend/composeApp/src/commonMain/composeResources/files/repository/
├── maps/map_goblin_gorge.json
├── maps/map_the_watch_gate.json
├── maps/map_golden_road.json
├── levels/gribnaks_ambush.json
├── levels/hold_the_gate.json
└── levels/the_golden_road.json
```

`sequence.json` lists the three levels right after `the_first_wave`, and they share the world‑map
location *The Beginning* with it. Titles, subtitles and map names are translated into all five
supported languages. `FirstLevelSuggestionsTest` validates that the maps are playable, that the
spawn points, fiefs, barricades and towers are placed on legal tiles and that every scripted event
uses a known story message.

Once a favourite has been picked in play‑testing, the other two can either be removed or kept as
optional side levels.
