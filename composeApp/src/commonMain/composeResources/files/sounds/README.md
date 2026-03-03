# Sound Files for Defender of Egril

This directory contains audio files for game sound effects.

## sounds from freesound.org

# Tower Attacks
attack_melee    
342397__christopherderp__swords-clash-w-swing-1.wav
https://freesound.org/people/SonoFxAudio/sounds/649334/
Attribution 4.0

attack_ranged
649334__sonofxaudio__arrow_loose02.wav
https://freesound.org/people/Christopherderp/sounds/342397/
Creative Commons 0

attack_ballista
459875__quickmusik__warrior-bass-t.wav
https://freesound.org/people/Quickmusik/sounds/459875/
Creative Commons 0

attack_area / fireball
564041__robinhood76__10056-giant-fireball-blow.wav
https://freesound.org/people/Robinhood76/sounds/564041/
Attribution NonCommercial 4.0

attack_lasting / acid
202094__spookymodem__acid-bubbling.wav
https://freesound.org/people/spookymodem/sounds/202094/
Creative Commons 0

# Enemy Events
enemy_spawn and enemy_move
384898__ali_6868__knight-left-footstep-forestgrass-3-with-chainmail.wav
https://freesound.org/people/Ali_6868/sounds/384898/
Creative Commons 0

enemy_destroyed
656726__paladinvii__deathsfx.wav
https://freesound.org/people/PaladinVII/sounds/656726/
Attribution 4.0
Media: DeathSFX.wav
by: PaladinVII
License: Attribution 4.0
https://creativecommons.org/licenses/by/4.0/

# Mine Events
mine_dig
240801__ryanconway__pickaxe-mining-stone.wav
https://freesound.org/people/ryanconway/sounds/240801/
Attribution 4.0

mine_coin
761495__paul-sinnett__coin-clink.wav
https://freesound.org/people/Paul%20Sinnett/sounds/761495/
Attribution 4.0

mine_trap
537430__wavecal22__wood-misc-6.wav
https://freesound.org/people/wavecal22/sounds/537430/
Creative Commons 0

mine_dragon
676474__neartheatmoshphere__beast-1.wav
https://freesound.org/people/NearTheAtmoshphere/sounds/676474/
Creative Commons 0

# Other Game Events
trap_trigger
434898__thebuilder15__trap-switch.wav
https://freesound.org/people/TheBuilder15/sounds/434898/
Creative Commons 0

life_lost
221544__joseppujol__wounded-man-scream.mp3
https://freesound.org/people/joseppujol/sounds/221544/
Creative Commons 0

# Special Events
dragon_eat
389638__stubb__growl-7.wav
https://freesound.org/people/_stubb/sounds/389638/
Creative Commons 0

#UI Sounds
button_click - remove

tower_placed - remove

tower_upgraded
810754__mokasza__level-up-02.mp3
https://freesound.org/people/mokasza/sounds/810754/
Attribution 4.0

tower_sold
same as mine coin
761495__paul-sinnett__coin-clink.wav
https://freesound.org/people/Paul%20Sinnett/sounds/761495/
Attribution 4.0

# Game Phase Sounds
battle_start
188815__porphyr__battle-horn.wav
https://freesound.org/people/Porphyr/sounds/188815/
Attribution 4.0

# bomb effects

bomb ticking
Ticking Timer 05 Sec.wav by LilMati -- https://freesound.org/s/487725/ -- License: Creative Commons 0

bomb exploding
impact_.wav by sangnamsa -- https://freesound.org/s/473941/ -- License: Creative Commons 0



## Recent Updates (November 2024)

All sound files have been upgraded with improved quality synthesized sounds:
- **Better Audio Envelopes**: Proper fade-in/fade-out for natural sound decay
- **Rich Harmonics**: Multiple frequencies combined for more complex tones
- **Appropriate Effects**: Filtered noise, pitch bends, and layering for realistic game sounds
- **Medieval Fantasy Theme**: Sounds designed to fit the tower defense fantasy setting
- **Larger Files**: Increased from 2-18KB to 4-44KB for better audio fidelity
- **Same Format**: Still WAV, 22050 Hz, mono for maximum compatibility

The sounds were created using SoX (Sound eXchange) with carefully tuned synthesis parameters to produce game-appropriate audio effects that match each event type.

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

### Game Phase Sounds
- `battle_start.wav` - Battle start sound (when "Start Battle" is clicked)

## Audio Format Requirements

**Recommended Settings:**
- **Format**: WAV (recommended for compatibility)
- **Sample Rate**: 44100 Hz or 22050 Hz
- **Bit Depth**: 16-bit
- **Channels**: Mono (preferred, smaller file size) or Stereo
- **Duration**: Keep sounds short (0.1 - 1.0 seconds)

**Optimal Length Guidelines:**
- **Attack sounds**: 100-200ms (quick, punchy)
- **Enemy events**: 50-150ms (brief acknowledgment)
- **Mine operations**: 100-250ms (slightly longer for impact)
- **Trap/Life loss**: 150-400ms (noticeable but not intrusive)
- **UI sounds**: 50-100ms (instant feedback)
- **Battle start**: 300-500ms (fanfare/announcement feel)

**Why short sounds?**
- Multiple sounds may play simultaneously during gameplay
- Shorter sounds prevent audio overlap from becoming muddy
- Keeps the game feeling responsive and fast-paced
- Reduces memory usage and loading time

**File Size:**
- Current placeholder sounds: 2-11 KB each
- Keep final sounds under 50 KB for quick loading
- Mono files are half the size of stereo with minimal quality loss for game effects

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
