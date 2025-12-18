# Android Save File Download/Upload Fix

## Issue
On Android, tapping the download button on save file cards or the download all/upload buttons above the save file list did nothing. The functionality was not implemented - only stub implementations existed.

## Root Cause
The `FileExportImport.android.kt` file contained only stub implementations that returned `false` for export operations and `null` for import operations. The comments indicated that Storage Access Framework (SAF) integration was needed but not yet implemented.

## Solution
Implemented full Android Storage Access Framework integration with the following approach:

### 1. Activity Result Launcher Pattern
- Used `ActivityResultContracts.CreateDocument` for file export (download)
- Used `ActivityResultContracts.OpenMultipleDocuments` for file import (upload)
- Launchers must be registered during Activity creation (before STARTED state)

### 2. Singleton Pattern
- Created a companion object with singleton instance
- Added `initialize(activity: ComponentActivity)` method called from `MainActivity.onCreate()`
- This ensures launchers are registered exactly once at the right time

### 3. Coroutine Integration
- Used `suspendCancellableCoroutine` to bridge Activity result callbacks with suspend functions
- Continuations stored in private fields and resumed when Activity result arrives
- Operations switch to `Dispatchers.Main` for UI interaction, then `Dispatchers.IO` for file I/O

### 4. Full Feature Implementation

#### Export Single File (`exportFile`)
- Launches file picker with `CreateDocument` contract
- User selects save location and filename
- Writes JSON content to selected URI using `ContentResolver`

#### Export ZIP Archive (`exportZip`)
- Launches file picker with `CreateDocument` contract
- Creates ZIP archive with `ZipOutputStream`
- Adds all save files as entries in the ZIP
- Writes to selected URI using `ContentResolver`

#### Import Files (`importFiles`)
- Launches file picker with `OpenMultipleDocuments` contract
- Accepts JSON and ZIP file types
- Reads JSON files directly
- Extracts JSON files from ZIP archives using `ZipInputStream`
- Returns list of `ImportedFile` objects with filename and content

### 5. Files Changed

#### `FileExportImport.android.kt`
- Complete rewrite from stub to full implementation
- 226 lines of production code
- Singleton pattern with Activity integration
- Full SAF integration with proper error handling

#### `MainActivity.kt`
- Added import for `AndroidFileExportImport`
- Added initialization call: `AndroidFileExportImport.initialize(this)`
- Called after `AndroidContextProvider.initialize(this)` in `onCreate()`

#### `SAVEFILE_DOWNLOAD_UPLOAD.md`
- Updated Android section to reflect implementation status
- Changed from "⏳ Stub implementation" to "✅ Fully implemented"
- Updated Known Issues to remove Android from platform-specific integration note
- Updated Conclusion to include Android with Desktop as fully functional

## Technical Details

### Storage Access Framework (SAF)
Android's recommended approach for file operations that:
- Respects user privacy and file permissions
- Works with any document provider (local storage, cloud storage, etc.)
- Provides native file picker UI
- No special permissions needed in manifest

### Activity Result API
Modern Android approach for handling Activity results:
- Type-safe contracts (`CreateDocument`, `OpenMultipleDocuments`)
- Lifecycle-aware (no manual result handling)
- Must be registered before Activity STARTED state

### Coroutine Integration Pattern
```kotlin
val uri = suspendCancellableCoroutine { continuation ->
    createDocumentContinuation = continuation
    createDocumentLauncher?.launch(filename)
}
// Continuation resumed in Activity result callback
```

## Testing Instructions

### Prerequisites
- Android device or emulator running Android 5.0 (API 21) or higher
- Defender of Egril app installed with this fix

### Test Cases

#### 1. Export Single Save File
1. Launch the app on Android
2. Play a level and save the game
3. Navigate to Load Game screen
4. Tap the download button on a save file card
5. **Expected**: Android file picker appears
6. Select a location and confirm
7. **Expected**: File is saved, file picker closes
8. Verify file exists in selected location using a file manager app

#### 2. Export All Save Files as ZIP
1. Ensure multiple save files exist
2. Navigate to Load Game screen
3. Tap the "Download All" button
4. **Expected**: Android file picker appears with suggested filename `defender-of-egril-saves-YYYY-MM-DD_HH-mm-ss.zip`
5. Select a location and confirm
6. **Expected**: ZIP file is created
7. Verify ZIP file contains all save files using a file manager or zip app

#### 3. Import Single JSON File
1. Navigate to Load Game screen
2. Tap the "Upload" button
3. **Expected**: Android file picker appears
4. Select a previously exported JSON save file
5. **Expected**: File is imported, success dialog appears
6. **Expected**: Save appears in the list

#### 4. Import Multiple JSON Files
1. Navigate to Load Game screen
2. Tap the "Upload" button
3. **Expected**: Android file picker appears with multiple selection enabled
4. Select multiple JSON save files (long press to select multiple)
5. Confirm selection
6. **Expected**: All files imported, success dialog shows count
7. **Expected**: All saves appear in the list

#### 5. Import ZIP Archive
1. Navigate to Load Game screen
2. Tap the "Upload" button
3. **Expected**: Android file picker appears
4. Select a previously exported ZIP archive
5. Confirm selection
6. **Expected**: ZIP is extracted, all saves imported
7. **Expected**: Success dialog shows count of imported files
8. **Expected**: All saves appear in the list

#### 6. Override Confirmation
1. Create a save file
2. Export it
3. Delete the original save
4. Upload the exported file (no conflict - should import)
5. Upload the same file again
6. **Expected**: Override dialog appears with filename
7. Test "Skip" button - file should not be imported again
8. Upload again and test "Override" button - file should be replaced
9. Create multiple saves, export them, upload them
10. **Expected**: Override dialog appears for each conflict
11. Test "Override All" button - all conflicts should be resolved automatically

#### 7. Cancel Operations
1. Tap download button, then cancel file picker
2. **Expected**: Nothing happens, no error
3. Tap upload button, then cancel file picker
4. **Expected**: Nothing happens, no error

#### 8. Error Handling
1. Try uploading an invalid file (not JSON or ZIP)
2. **Expected**: File is ignored, no crash
3. Try uploading a corrupted JSON file
4. **Expected**: Import error dialog appears

## Success Criteria
- ✅ All three operations (export file, export ZIP, import files) work correctly
- ✅ Android file picker appears with appropriate configuration
- ✅ Files are correctly written to user-selected locations
- ✅ Files are correctly read from user-selected locations
- ✅ ZIP archives are properly created and extracted
- ✅ Override confirmation dialog works correctly
- ✅ No crashes or errors during normal operation
- ✅ Proper error handling for edge cases

## Platform Comparison

| Feature | Desktop | Android | Web/WASM | iOS |
|---------|---------|---------|----------|-----|
| Export single file | ✅ JFileChooser | ✅ SAF CreateDocument | ✅ Blob download | ⏳ Not implemented |
| Export ZIP | ✅ Full ZIP | ✅ Full ZIP | ⚠️ Text file only | ⏳ Not implemented |
| Import files | ✅ Multi-select | ✅ Multi-select | ✅ Multi-select | ⏳ Not implemented |
| Import ZIP | ✅ Full extraction | ✅ Full extraction | ❌ Not supported | ⏳ Not implemented |

## Notes
- No changes to manifest permissions required (SAF doesn't need storage permissions)
- No changes to UI code required (all changes in platform-specific implementation)
- Compatible with Android 5.0 (API 21) and above
- Works with any document provider (local storage, Google Drive, Dropbox, etc.)
- File picker UI is native Android, respects system theme and language
