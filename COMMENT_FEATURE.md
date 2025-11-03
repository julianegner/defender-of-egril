# Save Game Comment Feature

## Overview
This feature adds the ability for players to add an optional comment when saving a game. The comment is displayed in the load game screen, helping players identify and distinguish between different save files.

## User Experience

### Saving a Game with a Comment
When the player clicks the "Save Game" button during gameplay:

1. A dialog appears titled "Save Game"
2. The dialog contains:
   - Explanatory text: "Add an optional comment to help identify this save:"
   - A multi-line text field with placeholder text: "e.g., 'Before final wave', 'Good position'..."
   - "Save" button to confirm
   - "Cancel" button to dismiss
3. The player can:
   - Enter a comment (up to 3 lines visible)
   - Leave it blank (comment will be saved as null)
   - Cancel the save operation
4. After clicking "Save":
   - The game is saved with the comment
   - The save dialog closes
   - A confirmation dialog appears: "Game Saved"

### Viewing Comments in Load Game Screen
When viewing the list of saved games:

1. Each save game card displays (if a comment exists):
   - A speech bubble emoji (💬) before the comment
   - The comment text in italic style
   - The comment appears below the Turn/Coins row and above the unit counts

2. If no comment exists:
   - The comment section is not displayed (seamless integration)

## Technical Implementation

### Data Model Changes

#### SavedGame
```kotlin
data class SavedGame(
    // ... existing fields ...
    val comment: String? = null  // Optional player comment
)
```

#### SaveGameMetadata
```kotlin
data class SaveGameMetadata(
    // ... existing fields ...
    val comment: String? = null  // Optional player comment
)
```

### JSON Serialization
The comment is stored in the save file JSON with proper escaping:

```json
{
  "id": "savegame_1234567890",
  "levelName": "The First Wave",
  // ... other fields ...
  "comment": "Before final wave - good position"
}
```

Special characters are properly escaped:
- Newlines: `\n`
- Quotes: `\"`
- Backslashes: `\\`
- Tabs: `\t`
- Carriage returns: `\r`

### UI Components

#### Save Dialog (GamePlayScreen.kt)
```kotlin
// Save game dialog (with optional comment input)
if (showSaveDialog && onSaveGame != null) {
    AlertDialog(
        onDismissRequest = { /* ... */ },
        title = { Text("Save Game") },
        text = {
            Column {
                Text("Add an optional comment to help identify this save:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = saveCommentInput,
                    onValueChange = { saveCommentInput = it },
                    placeholder = { Text("e.g., 'Before final wave'...") },
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val comment = if (saveCommentInput.isBlank()) null else saveCommentInput.trim()
                onSaveGame(comment)
                // ... show confirmation
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = { /* cancel */ }) {
                Text("Cancel")
            }
        }
    )
}
```

#### Comment Display (LoadGameScreen.kt)
```kotlin
// Display comment if present
if (!saveGame.comment.isNullOrBlank()) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "💬", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = saveGame.comment,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

## Backward Compatibility

The implementation is fully backward compatible:

1. **Old save files without comments:**
   - Load successfully with `comment = null`
   - No comment section displayed in load screen
   - No errors or warnings

2. **New save files with comments:**
   - Store comment in JSON
   - Display comment in load screen
   - Comment can be null if player leaves it blank

3. **JSON parsing:**
   - Uses try-catch to handle missing comment field
   - Defaults to null if field doesn't exist
   - Custom `extractCommentValue()` function handles escaped characters

## Testing

### Unit Tests
Three new tests were added to `SaveDataTest.kt`:

1. **testSavedGameWithComment**: Verifies serialization/deserialization of a comment
2. **testSavedGameWithoutComment**: Verifies null comment handling
3. **testCommentWithSpecialCharacters**: Verifies proper escaping of quotes, newlines, backslashes

All tests pass successfully.

### Manual Testing Scenarios

1. **Save with comment:**
   - Start a level
   - Click "Save Game"
   - Enter a comment like "Before boss wave"
   - Verify save succeeds
   - Load the save and verify comment appears

2. **Save without comment:**
   - Start a level
   - Click "Save Game"
   - Leave comment field blank
   - Verify save succeeds
   - Load the save and verify no comment section appears

3. **Multi-line comment:**
   - Save with comment containing newlines
   - Verify comment displays correctly with line breaks

4. **Special characters:**
   - Save with comment containing quotes, special symbols
   - Verify proper escaping and display

## File Changes Summary

- `SaveModels.kt`: Added `comment` field to `SavedGame` and `SaveGameMetadata`
- `SaveJsonSerializer.kt`: Added comment serialization/deserialization with escaping
- `SaveFileStorage.kt`: Updated to pass comment through save/load pipeline
- `GameViewModel.kt`: Updated `saveCurrentGame()` to accept optional comment
- `GamePlayScreen.kt`: Added save dialog with comment input field
- `LoadGameScreen.kt`: Added comment display in save game cards
- `App.kt`: Updated to pass comment parameter
- `SaveDataTest.kt`: Added tests for comment functionality

## Example Usage

```kotlin
// Save game with comment
viewModel.saveCurrentGame("Good defensive setup")

// Save game without comment
viewModel.saveCurrentGame(null)

// Comment appears in metadata
val metadata: SaveGameMetadata = // ... load from file
if (metadata.comment != null) {
    println("Comment: ${metadata.comment}")
}
```

## Design Decisions

1. **Optional field**: Comment is optional to not force users to enter text
2. **Multi-line support**: Allows longer descriptions (up to 3 visible lines)
3. **Placeholder text**: Provides examples to guide users
4. **Italic styling**: Distinguishes comment from other metadata
5. **Speech bubble icon**: Visual indicator of user-generated content
6. **Conditional display**: Only shows when comment exists (cleaner UI)
