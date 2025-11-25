# Repository Data Sync Feature

## Overview

This feature automatically detects when new map and level files have been added to the game's repository that are not yet in the user's gamedata folder. When detected, it prompts the user to add the new content.

## How It Works

1. **Detection**: When the user navigates to the World Map screen, the app asynchronously checks for new repository files
2. **User Prompt**: If new files are found, a dialog displays:
   - List of new maps (up to 5 shown, with "and N more..." if there are more)
   - List of new levels (up to 5 shown, with "and N more..." if there are more)
   - Warning that the sequence file will be replaced
   - Note that the current sequence will be backed up
3. **User Action**:
   - **Accept**: New files are synced, sequence is backed up and replaced
   - **Dismiss**: No changes are made, dialog won't show again until app restart

## Implementation Details

### Key Components

**RepositoryManager.kt**
- `detectNewRepositoryFiles()`: Compares repository files with user's gamedata
- `syncNewRepositoryFiles()`: Syncs new files and backs up sequence
- `NewRepositoryData`: Data class holding detection results

**NewRepositoryDataDialog.kt**
- UI dialog component showing new content
- Fully localized in 5 languages (EN, DE, ES, FR, IT)

**WorldMapScreen.kt**
- Integrates the check on first load
- `checkForNewRepositoryData` parameter allows disabling in tests

### Sequence File Backup

When new content is synced:
1. Current sequence file is read from `gamedata/sequence.json`
2. It's backed up to `gamedata/sequence-N.json` where N is the next available number (1, 2, 3, etc.)
3. Repository's sequence file replaces the current one
4. Safety limit: Maximum 999 backups to prevent filesystem issues

### Localization

The feature is fully localized with strings in:
- English (default)
- German (Deutsch)
- Spanish (Español)
- French (Français)
- Italian (Italiano)

String keys:
- `new_repository_data_title`
- `new_repository_data_message`
- `new_maps_found`
- `new_levels_found`
- `and_more`
- `sequence_will_be_replaced`
- `sequence_backup_warning`
- `add_data`

## Testing

The feature includes:
- Unit tests for `NewRepositoryData` data class
- Integration into existing WorldMapScreen tests
- Parameter to disable repository check in test environments

## Error Handling

- Errors during detection are caught and logged but don't disrupt user experience
- If backup limit is reached (999 files), a warning is logged but sync continues
- Missing or invalid repository files are handled gracefully

## User Experience

- Non-intrusive: Check happens asynchronously after UI loads
- One-time per session: Dialog only shows once after navigating to World Map
- Informative: Shows exactly what will be added
- Safe: Always backs up user's sequence before replacing
- Localized: Shows in user's selected language
