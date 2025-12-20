# Sticker Merchandise Preview Page

## Overview
The sticker merchandise preview page is accessible via the "sticker" cheat code from the World Map screen. It displays a preview layout that can be used to design merchandise stickers for the game.

## How to Access
1. Start the game and navigate to the World Map
2. Click on the title to open the cheat code dialog
3. Enter "sticker" (case insensitive)
4. Press Apply or Enter

## Layout Description

The screen is divided into two main sections in a row layout:

### Left Side (Game Elements)
The left side displays game elements arranged vertically:

1. **Game Map Section**
   - Shows a 5x5 hexagonal grid
   - Different colored tiles representing game elements:
     - Red tiles: Spawn points (left edge)
     - Cyan tiles: Target points (right edge)
     - Light green tiles: Build areas (checkered pattern)
     - Yellow tiles: Path tiles
   - Hexagons are outlined in black

2. **Enemy Units Group**
   - Title: "Enemies"
   - Three enemy cards displayed horizontally:
     - Goblin: Light green creature with pointy ears
     - Ork: Dark olive green creature with tusks
     - Wizard: Purple figure with pointed hat and staff
   - Each unit is in an 80x80 dp box with border

3. **Tower Units Group**
   - Title: "Towers"
   - Two tower cards displayed horizontally:
     - Bow: Tower with bow and arrow symbol
     - Wizard: Tower with golden star symbol
   - Each unit is in an 80x80 dp box with border

### Right Side (Branding)
The right side displays game branding:

1. **Application Banner**
   - "Defender of" in Great Vibes handwritten font (32sp)
   - "Egril" in Great Vibes handwritten font (56sp)
   - Shield logo with crossed swords (120dp)

2. **Back Button**
   - Returns to World Map
   - 200dp wide x 50dp tall

## Features
- Settings button in top-right corner
- Fully localized (English, German, Spanish, French, Italian)
- Responsive layout with proper spacing
- All elements are rendered as composables (not image files) for easy adjustment

## Localization
All text elements use string resources:
- `sticker_game_map`: "Game Map" / "Spielkarte" / "Mapa del Juego" / "Carte du Jeu" / "Mappa del Gioco"
- `sticker_enemies`: "Enemies" / "Feinde" / "Enemigos" / "Ennemis" / "Nemici"
- `sticker_towers`: "Towers" / "Türme" / "Torres" / "Tours" / "Torri"
- `sticker_goblin`: "Goblin" (same in all languages)
- `sticker_ork`: "Ork" / "Ork" / "Orco" / "Orc" / "Orco"
- `sticker_wizard`: "Wizard" / "Zauberer" / "Mago" / "Sorcier" / "Mago"
- `sticker_bow`: "Bow" / "Bogen" / "Arco" / "Arc" / "Arco"

## Implementation Details
- File: `composeApp/src/commonMain/kotlin/de/egril/defender/ui/StickerScreen.kt`
- Screen type: `Screen.Sticker` sealed class
- Navigation: Via `GameViewModel.navigateToSticker()`
- Cheat code: "sticker" (handled in `GameViewModel.applyWorldMapCheatCode()`)

## Testing
- Unit tests: `GameViewModelStickerTest.kt` - Tests navigation and cheat code
- UI tests: `StickerScreenTest.kt` - Tests screen rendering and interaction

## Usage for Merchandise
The elements can be easily adjusted by:
1. Modifying the spacing between elements
2. Changing the size of unit cards
3. Adjusting the grid dimensions
4. Repositioning elements using Compose modifiers
5. Exporting individual sections for different sticker designs
