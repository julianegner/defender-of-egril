# Sound Files for Defender of Egril

This directory contains audio files for game sound effects.

## Required Sound Files

Place WAV audio files in this directory with the following names:

### Tower Attacks
- `attack_melee.wav` - Melee attack sound (Spike/Pike towers)
- `attack_ranged.wav` - Ranged attack sound (Bow/Spear/Ballista)
- `attack_area.wav` - Area attack sound (Wizard fireball)
- `attack_lasting.wav` - Lasting damage sound (Alchemy acid)

### Enemy Events
- `enemy_spawn.wav` - Enemy spawning sound
- `enemy_move.wav` - Enemy movement sound
- `enemy_destroyed.wav` - Enemy destruction sound

### Mine Events
- `mine_dig.wav` - Mine digging action
- `mine_coin.wav` - Coins found sound
- `mine_trap.wav` - Trap built sound
- `mine_dragon.wav` - Dragon awakening sound

### Other Game Events
- `trap_trigger.wav` - Trap activation sound
- `life_lost.wav` - Life lost when enemy reaches target

### Special Events
- `dragon_eat.wav` - Dragon eating another unit

### UI Sounds (Optional)
- `button_click.wav` - Button click sound
- `tower_placed.wav` - Tower placement sound
- `tower_upgraded.wav` - Tower upgrade sound
- `tower_sold.wav` - Tower selling sound

## Audio Format Requirements

- **Format**: WAV (recommended for compatibility)
- **Sample Rate**: 44100 Hz or 22050 Hz
- **Bit Depth**: 16-bit
- **Channels**: Mono (preferred) or Stereo
- **Duration**: Keep sounds short (0.1 - 1.0 seconds)

## Finding or Creating Sounds

### Free Sound Resources
1. **Freesound.org** - https://freesound.org/ (CC-licensed sounds)
2. **OpenGameArt.org** - https://opengameart.org/ (game-specific sounds)
3. **Zapsplat** - https://www.zapsplat.com/ (free sound effects)

### Creating Your Own
Use tools like:
- **Audacity** (free, cross-platform)
- **LMMS** (free music/sound creation)
- **Bfxr** (free retro game sound generator)

## Adding Sounds

1. Place WAV files in this directory
2. Ensure file names match exactly (case-sensitive)
3. Test in-game by:
   - Opening Settings → Enable Sound
   - Playing a level and performing actions

## Fallback Behavior

If sound files are not found, the system will:
- Print a warning to console
- Continue without playing the sound
- Not crash the game

## Platform-Specific Notes

### Desktop (JVM)
- Files loaded from classpath: `/files/sounds/`
- Uses javax.sound.sampled API

### Android
- Files can be in `res/raw/` or assets
- Uses SoundPool API
- Requires context initialization

### iOS
- Files bundled in app resources
- Uses AVAudioPlayer
- Path: `files/sounds/`

### Web/WASM
- Files served from web server
- Uses HTML5 Audio API
- Path relative to web root

## Testing

After adding sounds:
```bash
# Desktop
./gradlew :composeApp:run

# Android
./gradlew :composeApp:installDebug

# Web
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Then:
1. Open Settings → Enable Sound → Adjust Volume
2. Start a level
3. Perform actions (attack, spawn enemies, etc.)
4. Verify sounds play correctly
