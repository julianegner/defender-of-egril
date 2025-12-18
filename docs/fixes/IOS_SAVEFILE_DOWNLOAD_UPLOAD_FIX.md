# iOS Save File Download/Upload Implementation

## Issue
On iOS, the download and upload buttons for save files did nothing. The functionality was not implemented - only stub implementations existed that returned `false` for exports and `null` for imports.

## Root Cause
The `FileExportImport.ios.kt` file contained only stub implementations with TODO comments indicating that UIDocumentPickerViewController integration was needed but not yet implemented.

## Solution
Implemented full iOS file export/import functionality using UIKit's native APIs:

### 1. UIActivityViewController for Exports
- Used `UIActivityViewController` (iOS's native share sheet) for file exports
- Provides familiar iOS sharing experience with multiple destination options
- Works seamlessly with Files app, AirDrop, iCloud Drive, and third-party apps
- No special permissions required in Info.plist

### 2. UIDocumentPickerViewController for Imports
- Used `UIDocumentPickerViewController` for file imports
- Supports multi-selection for importing multiple files at once
- Accepts both JSON and ZIP-like file types using `UTType` API
- Integrates with iOS's document picker UI

### 3. Simple ZIP Format
- Created a simple concatenated text format for "Download All" feature
- Uses markers (`=== FILE: filename ===`) to separate files
- Avoids dependency on Apple's Compression framework
- Cross-compatible with the same format on other platforms (Web/WASM)
- Can be parsed on import to extract individual save files

### 4. Coroutine Integration
- Used `suspendCancellableCoroutine` to bridge UIKit callbacks with suspend functions
- Operations run on `Dispatchers.Main` for UI interaction
- File I/O runs on `Dispatchers.Default` for background processing
- Proper error handling with try-catch blocks

### 5. Build Configuration Fix
- Added iosMain source set to build.gradle.kts
- Connected iOS targets (iosX64, iosArm64, iosSimulatorArm64) to iosMain
- Fixed Kotlin Multiplatform configuration to recognize iOS platform implementations

## Technical Details

### UIActivityViewController (Exports)
iOS's standard sharing mechanism that:
- Presents a native share sheet with app icons
- Supports sharing to Files app, AirDrop, Mail, Messages, etc.
- Works with third-party document apps (Dropbox, Google Drive, etc.)
- Provides native iPad popover presentation
- No special entitlements or permissions needed

### UIDocumentPickerViewController (Imports)
iOS's standard document picker that:
- Presents native file browser UI
- Works with Files app and iCloud Drive
- Supports multiple document providers
- Enables multi-file selection
- Type-safe with `UTType` (Uniform Type Identifiers)
- Requires iOS 14+ for modern `forOpeningContentTypes` API

### Simple ZIP Format
Instead of using true ZIP compression, we use a simple text-based format:
```
=== FILE: savegame_1234567890.json ===
{
  "saveId": "savegame_1234567890",
  ...
}

=== FILE: savegame_9876543210.json ===
{
  "saveId": "savegame_9876543210",
  ...
}
```

**Advantages:**
- No external dependencies (Apple Compression framework not needed)
- Simple to implement and parse
- Cross-platform compatible (matches Web/WASM implementation)
- Human-readable for debugging

**Limitations:**
- Larger file size (no compression)
- Not compatible with standard ZIP tools
- For future: Could integrate Apple Compression framework for true ZIP support

## Files Changed

### Modified Files

#### `composeApp/build.gradle.kts`
- Added `iosMain` source set configuration
- Connected iOS targets to iosMain using `dependsOn()`
- Ensures iOS platform implementations are recognized by compiler

#### `composeApp/src/iosMain/kotlin/de/egril/defender/save/FileExportImport.ios.kt`
- Complete rewrite from stub to full implementation
- ~250 lines of production code
- Three main functions:
  - `exportFile()`: Uses UIActivityViewController
  - `exportZip()`: Creates simple format, uses UIActivityViewController  
  - `importFiles()`: Uses UIDocumentPickerViewController with multi-selection
- Helper functions:
  - `createZipFile()`: Creates simple concatenated format
  - `processImportedFiles()`: Parses both JSON and simple ZIP format

#### `docs/implementation/SAVEFILE_DOWNLOAD_UPLOAD.md`
- Updated iOS section to reflect implementation status
- Changed from "⏳ Stub implementation" to "✅ Implemented"
- Added technical details about iOS-specific implementation
- Updated platform summary and known issues

## Testing Instructions

### Prerequisites
- iOS device or simulator running iOS 14.0 or higher
- macOS with Xcode installed
- Defender of Egril app built for iOS with this fix

### Test Cases

#### 1. Export Single Save File
1. Launch the app on iOS
2. Play a level and save the game
3. Navigate to Load Game screen
4. Tap the download button on a save file card
5. **Expected**: iOS share sheet appears with app icons
6. Select "Save to Files" or other destination
7. **Expected**: File is saved, confirmation appears
8. Verify file exists using Files app

#### 2. Export All Save Files (Simple ZIP)
1. Ensure multiple save files exist
2. Navigate to Load Game screen
3. Tap the "Download All" button
4. **Expected**: iOS share sheet appears with suggested filename `defender-of-egril-saves-YYYY-MM-DD_HH-mm-ss.zip`
5. Select "Save to Files" or other destination
6. **Expected**: ZIP-like file is created
7. Verify file exists and can be opened (shows concatenated format)

#### 3. Import Single JSON File
1. Navigate to Load Game screen
2. Tap the "Upload" button
3. **Expected**: iOS document picker appears
4. Navigate to a previously exported JSON save file
5. Select the file
6. **Expected**: File is imported, success dialog appears
7. **Expected**: Save appears in the list

#### 4. Import Multiple JSON Files
1. Navigate to Load Game screen
2. Tap the "Upload" button
3. **Expected**: iOS document picker appears
4. Tap "Select" in top-right corner (enables multi-selection)
5. Select multiple JSON save files
6. Tap "Open"
7. **Expected**: All files imported, success dialog shows count
8. **Expected**: All saves appear in the list

#### 5. Import Simple ZIP Archive
1. Navigate to Load Game screen
2. Tap the "Upload" button
3. **Expected**: iOS document picker appears
4. Select a previously exported ZIP-like file
5. Confirm selection
6. **Expected**: ZIP is parsed, all saves imported
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

#### 7. Cancel Operations
1. Tap download button, then cancel share sheet
2. **Expected**: Nothing happens, no error
3. Tap upload button, then cancel document picker
4. **Expected**: Nothing happens, no error

#### 8. Share to Other Apps
1. Export a save file
2. **Expected**: Share sheet appears
3. Select "AirDrop" or "Mail" or other app
4. **Expected**: File is shared successfully
5. Verify file is received/sent correctly

## Success Criteria
- ✅ All three operations (export file, export ZIP, import files) work correctly
- ✅ iOS share sheet appears with appropriate configuration
- ✅ Files are correctly shared to various destinations
- ✅ Document picker works with multi-selection
- ✅ Simple ZIP format is created and parsed correctly
- ✅ Override confirmation dialog works correctly
- ✅ No crashes or errors during normal operation
- ✅ Proper error handling for edge cases
- ✅ All platforms (Desktop, Android, Web/WASM, iOS) build successfully

## Platform Comparison

| Feature | Desktop | Android | iOS | Web/WASM |
|---------|---------|---------|-----|----------|
| Export single file | ✅ JFileChooser | ✅ SAF CreateDocument | ✅ UIActivityViewController | ✅ Blob download |
| Export ZIP | ✅ Full ZIP | ✅ Full ZIP | ✅ Simple format | ⚠️ Simple format |
| Import files | ✅ Multi-select | ✅ Multi-select | ✅ Multi-select | ✅ Multi-select |
| Import ZIP | ✅ Full extraction | ✅ Full extraction | ✅ Simple format | ❌ Not supported |
| File Picker UI | Swing JFileChooser | Android SAF | iOS Document Picker | Browser File API |

## Notes
- No changes to iOS app manifest (Info.plist) required
- No special permissions needed (UIActivityViewController doesn't require permissions)
- No changes to UI code required (all changes in platform-specific implementation)
- Compatible with iOS 14.0 and above (UTType API)
- Works with any document provider (Files app, iCloud Drive, third-party apps)
- Share sheet respects user's installed apps and preferences
- Simple ZIP format is compatible with Web/WASM implementation
- For future enhancement: Could integrate Apple Compression framework for true ZIP support

## Comparison with Android Implementation

### Similarities
- Both use native platform file APIs
- Both support multi-file import
- Both integrate with platform's document storage
- Both use coroutine suspension for async operations
- Both have proper error handling

### Differences
- **Export Strategy**: 
  - Android: Uses SAF's CreateDocument (user picks location)
  - iOS: Uses UIActivityViewController (user picks destination app)
- **ZIP Format**:
  - Android: True ZIP with Java's ZipOutputStream/ZipInputStream
  - iOS: Simple concatenated format (no compression)
- **Initialization**:
  - Android: Requires Activity initialization in MainActivity.onCreate()
  - iOS: Works directly without special initialization
- **User Experience**:
  - Android: Traditional "Save As" dialog
  - iOS: Native share sheet with app destinations

## Future Enhancements
1. Integrate Apple Compression framework for true ZIP support
2. Add progress indicators for large file operations
3. Support direct iCloud Drive integration
4. Add file validation before parsing
5. Support drag-and-drop (iPad split-screen)
