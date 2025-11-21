# Tutorial Extension: Save/Load Game Step

## Summary

This document describes the addition of the `SAVE_GAME` tutorial step to educate players about the save/load functionality.

## Issue Addressed

**Issue:** "extend tutorial"  
**Requirement:** Add an info text before the last text and give a hint that you can save your game and how to do that. Also inform about the load safegame button on the world map.

## Implementation

### Changes Made

#### 1. New Tutorial Step Added

Added `SAVE_GAME` step between `SELL_TOWER` and `COMPLETE` in the tutorial progression:

```
... -> UPGRADE_TOWER -> SELL_TOWER -> SAVE_GAME -> COMPLETE -> NONE
```

#### 2. Files Modified

1. **TutorialState.kt**
   - Added `SAVE_GAME` enum value to `TutorialStep`
   - Updated `getNextStep()` to include `SAVE_GAME` in progression

2. **TutorialOverlay.kt**
   - Added handling for `SAVE_GAME` step in `getTutorialTitle()`
   - Added handling for `SAVE_GAME` step in `getTutorialContent()`

3. **String Resources (All 5 Languages)**
   - `values/strings.xml` (English)
   - `values-de/strings.xml` (German)
   - `values-es/strings.xml` (Spanish)
   - `values-fr/strings.xml` (French)
   - `values-it/strings.xml` (Italian)

4. **TutorialStateTest.kt**
   - Updated progression test to verify `SELL_TOWER -> SAVE_GAME -> COMPLETE`

### Tutorial Content

#### Step 13: Save Your Progress (TutorialStep.SAVE_GAME)

**English:**
- **Title:** "Save Your Progress"
- **Message:** "You can save your game at any time using the Save Game button. This lets you try different strategies or continue later. To load a saved game, use the Load Game button on the world map screen."

**German:**
- **Title:** "Speichern Sie Ihren Fortschritt"
- **Message:** "Sie können Ihr Spiel jederzeit mit der Schaltfläche Spiel speichern sichern. So können Sie verschiedene Strategien ausprobieren oder später weiterspielen. Um ein gespeichertes Spiel zu laden, verwenden Sie die Schaltfläche Spiel laden auf dem Weltkarten-Bildschirm."

**Spanish:**
- **Title:** "Guarda tu progreso"
- **Message:** "Puedes guardar tu juego en cualquier momento usando el botón Guardar Juego. Esto te permite probar diferentes estrategias o continuar más tarde. Para cargar una partida guardada, usa el botón Cargar Juego en la pantalla del mapa mundial."

**French:**
- **Title:** "Sauvegardez votre progression"
- **Message:** "Vous pouvez sauvegarder votre jeu à tout moment en utilisant le bouton Sauvegarder. Cela vous permet d'essayer différentes stratégies ou de continuer plus tard. Pour charger une partie sauvegardée, utilisez le bouton Charger sur l'écran de la carte du monde."

**Italian:**
- **Title:** "Salva i tuoi progressi"
- **Message:** "Puoi salvare il tuo gioco in qualsiasi momento usando il pulsante Salva gioco. Questo ti permette di provare diverse strategie o continuare più tardi. Per caricare un gioco salvato, usa il pulsante Carica gioco sulla schermata della mappa del mondo."

### Complete Tutorial Flow (14 Steps)

1. **WELCOME** - Introduction to the game
2. **RESOURCES** - Explain coins and health
3. **TOWER_TYPES** - Show available towers
4. **BUILD_TOWER** - Guide to place first tower (gated)
5. **INITIAL_BUILDING** - Explain initial building phase
6. **UNDO_TOWER** - Explain undo functionality
7. **START_COMBAT** - Start the battle (gated)
8. **ENEMIES_INCOMING** - Show incoming enemies
9. **CHECK_RANGE** - How to check tower range
10. **ATTACKING** - How to attack enemies (gated)
11. **UPGRADE_TOWER** - How to upgrade towers
12. **SELL_TOWER** - How to sell towers
13. **SAVE_GAME** - ✨ **NEW:** How to save and load games
14. **COMPLETE** - Tutorial finished

## Key Features

### What the New Step Teaches

1. **Save Functionality:**
   - Players can save at any time
   - Uses the "Save Game" button
   - Allows trying different strategies
   - Can continue later

2. **Load Functionality:**
   - Located on the world map screen
   - Uses the "Load Game" button
   - Restores saved game state

### User Experience

- Appears as step 13 of 14 in the tutorial
- Shows after learning about selling towers
- Shows before the final "Tutorial Complete!" message
- Follows the same UI pattern as other tutorial steps
- Can be advanced with the "Next" button
- No skip button available (tutorial is almost complete)

## Testing

### Unit Tests

✅ **TutorialStateTest.kt** updated to verify:
- `SELL_TOWER` advances to `SAVE_GAME`
- `SAVE_GAME` advances to `COMPLETE`
- Complete tutorial progression includes all 14 steps

### Build Status

✅ Desktop compilation successful  
⚠️ Pre-existing test failures unrelated to this change

### Manual Testing Required

To verify the implementation:
1. Launch the game
2. Start Level 1 ("Welcome to Defender of Egril")
3. Progress through tutorial steps
4. After "Sell Towers" step, verify "Save Your Progress" appears
5. Verify the message explains:
   - Save Game button usage
   - Benefits of saving
   - Load Game button location (world map)
6. Click "Next" to proceed to "Tutorial Complete!"
7. Test in multiple languages to verify translations

## Requirements Fulfilled

✅ Added info text before the last tutorial text (COMPLETE)  
✅ Explains how to save the game  
✅ Mentions the Save Game button  
✅ Informs about the Load Game button on the world map  
✅ Fully localized in all 5 supported languages  
✅ Tests updated to include new step  
✅ Code compiles successfully

## Impact

- **Low Risk:** Minimal code changes, only adds one new step
- **No Breaking Changes:** Existing tutorial flow preserved
- **Backward Compatible:** No save format or API changes
- **User Benefit:** Players learn about save/load functionality during tutorial
- **Localization:** Fully translated, consistent user experience across languages
