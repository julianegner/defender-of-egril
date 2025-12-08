# Background Music for Defender of Egril

This directory contains background music files for the game.

## Required Music Files

The following MP3 files should be placed in this directory:

### World Map Music
- **File**: `atmosphere-mystic-fantasy-orchestral-music-335263.mp3`
- **Usage**: Plays in a loop on the world map screen
- **Description**: Mystic fantasy orchestral music

### Gameplay Music (Normal)
- **File**: `2021-02-23_-_Fantasy_Ambience_-_David_Fesliyan.mp3`
- **Usage**: Plays in a loop during normal gameplay
- **Description**: Fantasy ambience background music

### Gameplay Music (Low Health)
- **File**: `2017-06-16_-_The_Dark_Castle_-_David_Fesliyan.mp3`
- **Usage**: Plays when health points drop below 5
- **Description**: Dark castle music for tense moments

## Audio Settings

Background music can be controlled through the game's Settings dialog:

1. **Overall Sound**: Master on/off switch and volume (affects both effects and music)
2. **Effect Sounds**: Separate on/off switch and volume for game sound effects
3. **Background Music**: Separate on/off switch and volume for background music

By default, background music is set to 50% volume (quieter than sound effects at 70%).

## Audio Format Requirements

**Recommended Settings:**
- **Format**: MP3 or WAV
- **Sample Rate**: 44100 Hz
- **Bit Depth**: 16-bit (for WAV)
- **Channels**: Stereo
- **Duration**: 2-5 minutes (for looping music)
- **File Size**: Keep under 10 MB for reasonable loading times

## Platform Support

Background music is supported on all platforms:
- **Desktop**: Uses javax.sound.sampled API
- **Android**: Uses MediaPlayer API
- **iOS**: Uses AVAudioPlayer
- **Web/WASM**: Uses HTML5 Audio API

## Music Attribution

When adding music files, please ensure proper attribution and licensing:
- Include license information in this README
- Ensure the license allows use in this open-source project
- Provide attribution as required by the license

## Finding Free Music

### Free Music Resources
1. **Pixabay Music** - https://pixabay.com/music/
2. **Incompetech** - https://incompetech.com/music/royalty-free/music.html
3. **Free Music Archive** - https://freemusicarchive.org/
4. **OpenGameArt.org** - https://opengameart.org/

### Music Mentioned in Issue
The files mentioned in the issue should be placed here:
- `atmosphere-mystic-fantasy-orchestral-music-335263.mp3` (from Pixabay or similar)
- `2021-02-23_-_Fantasy_Ambience_-_David_Fesliyan.mp3` (from David Fesliyan)
- `2017-06-16_-_The_Dark_Castle_-_David_Fesliyan.mp3` (from David Fesliyan)

## Testing

After adding music files:
1. Run the game
2. Navigate to Settings → Sound section
3. Ensure "Overall Sound" and "Background Music" are enabled
4. Go to the World Map - music should play automatically
5. Start a level - gameplay music should play
6. Adjust volume sliders to verify volume control works
7. Toggle music on/off to verify enable/disable works

## Fallback Behavior

If music files are not found:
- The game will print a warning to the console
- No music will play, but the game will continue normally
- Sound effects will still work independently
