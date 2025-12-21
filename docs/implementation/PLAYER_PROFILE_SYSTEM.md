# Player Profile System Implementation

## Overview

The player profile system allows multiple players to use the same machine while keeping their game progress and save files completely separate. Each player has:
- Their own world map progress (locked/unlocked levels)
- Their own save game files
- A unique player name chosen at first launch

## Architecture

### File Structure

```
~/.defender-of-egril/
├── players.json                    # Registry of all player profiles
└── players/
    ├── player_1/
    │   ├── level_progress.json     # World map progress for Player 1
    │   └── savefiles/
    │       ├── savegame_*.json     # Individual save games
    │       └── ...
    ├── player_2/
    │   ├── level_progress.json     # World map progress for Player 2
    │   └── savefiles/
    │       └── ...
    └── ...
```

### Data Models

#### PlayerProfile
```kotlin
data class PlayerProfile(
    val id: String,          // Sanitized name (e.g., "john_doe")
    val name: String,        // Display name (e.g., "John Doe")
    val createdAt: Long,     // Creation timestamp
    val lastPlayedAt: Long   // Last activity timestamp
)
```

#### PlayerProfiles
```kotlin
data class PlayerProfiles(
    val profiles: List<PlayerProfile>,
    val lastUsedPlayerId: String?  // Auto-select on next launch
)
```

### Core Components

#### PlayerProfileStorage

Manages player profile operations:
- `createProfile(name: String): PlayerProfile?` - Creates a new player
- `getProfile(playerId: String): PlayerProfile?` - Retrieves a profile
- `getAllProfiles(): PlayerProfiles` - Lists all profiles
- `deleteProfile(playerId: String): Boolean` - Removes a profile
- `updateLastPlayed(playerId: String)` - Updates activity timestamp
- `migrateExistingSaves(): PlayerProfile?` - One-time migration of legacy saves

#### SaveFileStorage (Updated)

Enhanced to support player-specific directories:
- `setCurrentPlayer(playerId: String?)` - Sets the active player context
- `getCurrentPlayer(): String?` - Gets the active player ID
- All save/load operations now use player-specific paths

#### GameViewModel (Updated)

Manages player profile lifecycle:
- `currentPlayer: StateFlow<PlayerProfile?>` - Currently active player
- `allPlayers: StateFlow<List<PlayerProfile>>` - All available players
- `needsPlayerSelection: StateFlow<Boolean>` - First-time setup flag
- `createPlayer(name: String): Boolean` - Creates and switches to new player
- `switchPlayer(playerId: String)` - Changes active player
- `deletePlayer(playerId: String): Boolean` - Removes a player profile

### User Flow

#### First Launch
1. App starts with no player profiles
2. Check for existing legacy saves (pre-migration)
3. If legacy saves exist:
   - Create "Player 1" profile
   - Move saves to player's directory
   - Auto-select "Player 1"
4. If no legacy saves:
   - Show "Create Player" dialog (mandatory)
   - User enters name
   - Profile created and selected

#### Subsequent Launches
1. App loads player profiles from `players.json`
2. Auto-selects the last used player
3. Loads that player's world map progress and saves

#### Player Switching
1. User clicks "Switch Player" button (main menu or world map)
2. "Select Player" dialog appears
3. User can:
   - Select a different existing player
   - Create a new player
   - Delete a player (except current one)
4. On selection:
   - Game switches context to new player
   - World map reloads with new player's progress
   - Save games list updates

### UI Components

#### CreatePlayerDialog
- Text input for player name
- Validation (1-50 characters, non-empty)
- Error messages for invalid input
- Cancel button (only if a player already exists)

#### SelectPlayerDialog
- List of all players with last played timestamp
- Visual indicator for current player
- "Select" button for other players
- "Delete" button with confirmation dialog
- "New Player" button to create additional profiles

#### Main Menu Integration
- Player name displayed in top-left corner
- "Switch Player" button next to name
- Settings button remains in top-right

#### World Map Integration
- Player name shown below title/subtitle
- Compact "Switch Player" button
- Non-intrusive placement

### Migration Strategy

For backward compatibility with existing installations:

1. On first launch, check for legacy saves in old location (`savefiles/`)
2. If found, call `PlayerProfileStorage.migrateExistingSaves()`
3. Migration creates "Player 1" profile
4. Moves `level_progress.json` to player directory
5. Moves all save game files to player's `savefiles/` subdirectory
6. Legacy directories can be manually cleaned up later

### Localization

All UI strings are translated to 5 languages:
- English (EN) - Default
- German (DE)
- Spanish (ES)
- French (FR)
- Italian (IT)

Key strings:
- `player_create_title` - "Create New Player"
- `player_select_title` - "Select Player"
- `player_name` - "Player Name"
- `switch_player` - "Switch Player"
- Plus validation errors and confirmation dialogs

### Security Considerations

- Player IDs are sanitized (lowercase, alphanumeric + underscore)
- No path traversal vulnerabilities (IDs are sanitized)
- Player names limited to 50 characters
- File operations use safe paths through FileStorage abstraction

### Platform Support

Works on all supported platforms:
- Desktop (JVM) - Linux, macOS, Windows
- Android
- iOS
- Web (WASM)

Each platform uses its own FileStorage implementation to determine the base directory location.

## Testing Checklist

- [ ] Create first player on fresh install
- [ ] Create multiple players
- [ ] Switch between players
- [ ] Verify save file isolation
- [ ] Verify world map progress isolation
- [ ] Delete a player (not current)
- [ ] Attempt to delete current player (should fail)
- [ ] Migration from legacy saves
- [ ] Long player names (50 characters)
- [ ] Special characters in player names
- [ ] Empty player name validation
- [ ] Duplicate player name handling
- [ ] UI on mobile devices
- [ ] UI on desktop
- [ ] All 5 languages

## Future Enhancements

Possible improvements for future versions:
- Player avatars/icons
- Player statistics (games played, levels completed, etc.)
- Export/import player profiles
- Cloud sync for player profiles
- Player rename functionality
- Player merge functionality
