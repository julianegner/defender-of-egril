# Sound Files Update

## Changes Made

The sound system has been updated to use **audio files** instead of harsh synthesized beeps.

### What Changed

1. **New FileSoundManager**: Replaces SimpleSoundManager to load and play WAV audio files
2. **Platform Implementations**: 
   - Desktop: Uses `javax.sound.sampled` with Clip API
   - Android: Uses SoundPool API for efficient playback
   - iOS: Uses AVAudioPlayer for native audio
   - Web/WASM: Uses HTML5 Audio API

3. **Placeholder Sound Files**: 18 placeholder WAV files have been added to get started
4. **Easy to Replace**: Simply replace the WAV files in `composeApp/src/commonMain/composeResources/files/sounds/` with your own

### Sound Files Included

All sounds are stored in: `composeApp/src/commonMain/composeResources/files/sounds/`

**Tower Attacks:**
- `attack_melee.wav` - Spike/Pike tower attacks
- `attack_ranged.wav` - Bow/Spear/Ballista attacks
- `attack_area.wav` - Wizard fireball
- `attack_lasting.wav` - Alchemy acid

**Enemy Events:**
- `enemy_spawn.wav` - Enemy spawning
- `enemy_move.wav` - Enemy movement
- `enemy_destroyed.wav` - Enemy destruction

**Mine Events:**
- `mine_dig.wav` - Digging action
- `mine_coin.wav` - Coins found
- `mine_trap.wav` - Trap built
- `mine_dragon.wav` - Dragon awakens

**Other Events:**
- `trap_trigger.wav` - Trap activation
- `life_lost.wav` - Life lost
- `dragon_eat.wav` - Dragon eating

**UI Sounds:**
- `button_click.wav` - Button clicks
- `tower_placed.wav` - Tower placement
- `tower_upgraded.wav` - Tower upgrade
- `tower_sold.wav` - Tower selling

### How to Replace Sounds

1. **Find or Create Better Sounds**:
   - Use free resources like Freesound.org, OpenGameArt.org, Zapsplat
   - Create your own with Audacity, LMMS, or Bfxr
   - Commission custom sounds from audio designers

2. **Replace Files**:
   - Keep the same filenames
   - Use WAV format (44100 Hz, 16-bit, mono preferred)
   - Keep sounds short (0.1 - 1.0 seconds)

3. **Test**:
   ```bash
   ./gradlew :composeApp:run
   ```
   - Open Settings → Enable Sound
   - Play the game and hear your new sounds!

### Technical Details

The placeholder sounds are generated programmatically:
- Simple sine wave tones
- Different frequencies for different events
- Short durations (50-400ms)
- Small file sizes (~2-11 KB each)

While functional, **these should be replaced with proper game audio** for the best experience.

### File Format Requirements

- **Format**: WAV (recommended)
- **Sample Rate**: 44100 Hz or 22050 Hz
- **Bit Depth**: 16-bit
- **Channels**: Mono (smaller files) or Stereo
- **Duration**: 0.1 - 1.0 seconds

### Benefits Over Synthesized Beeps

✅ **Better Sound Quality**: Real audio samples sound more natural
✅ **Customizable**: Easy to replace with any WAV file
✅ **Professional**: Can use high-quality sound effects
✅ **Variety**: Each event can have unique character
✅ **Immersive**: Enhances gameplay experience

### Migration Path

The old SimpleSoundManager with synthesized tones is still available if needed. To switch back:

```kotlin
// In SoundManager.*.kt files, change:
actual fun createSoundManager(): SoundManager = FileSoundManager()
// To:
actual fun createSoundManager(): SoundManager = SimpleSoundManager()
```

### Future Improvements

- Add sound variations (multiple files per event, randomly chosen)
- Support different enemy destruction sounds per enemy type
- Add background music system
- Implement 3D positional audio
- Add audio compression (MP3/OGG support)
