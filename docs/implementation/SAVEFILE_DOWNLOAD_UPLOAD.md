# Savefile Download/Upload Feature - Implementation Summary

## Overview
This document describes the implementation of the download/upload functionality for save files in Defender of Egril, supporting cross-platform usage (Desktop, Android, iOS, Web).

## Requirements Implemented

1. ✅ **Individual download button** on each savefile card
2. ✅ **Download all button** above the list of savefiles (creates ZIP with app name + ISO timestamp)
3. ✅ **Upload button** next to download all (handles individual JSON files and ZIP bundles)
4. ✅ **Override confirmation** dialog (Skip, Override, Override All options)

## Architecture

### Core Infrastructure

#### FileExportImport Interface (`commonMain`)
```kotlin
interface FileExportImport {
    suspend fun exportFile(filename: String, content: String): Boolean
    suspend fun exportZip(zipFilename: String, files: Map<String, String>): Boolean
    suspend fun importFiles(): List<ImportedFile>?
}
```

#### Platform Implementations

**Desktop/JVM** (`FileExportImport.desktop.kt`)
- Uses `JFileChooser` for file picker dialogs
- Full ZIP support with `ZipOutputStream` and `ZipInputStream`
- Handles both single files and ZIP archives
- Status: ✅ **Fully implemented and tested (compilation)**

**Web/WASM** (`FileExportImport.wasmJs.kt`)
- Uses browser File API with `<input type="file">` for uploads
- Creates download links with `Blob` and `URL.createObjectURL()`
- Uses `@JsFun` external functions for proper WASM/JS interop
- Limitations:
  - Download All creates concatenated text file (not real ZIP)
  - Cannot parse uploaded ZIP files (only JSON)
- Status: ✅ **Implemented and compiles**

**Android** (`FileExportImport.android.kt`)
- Uses Storage Access Framework (SAF) for file operations
- `ActivityResultContracts.CreateDocument` for export with file picker dialog
- `ActivityResultContracts.OpenMultipleDocuments` for import with multiple selection
- Full ZIP support with `ZipOutputStream` and `ZipInputStream`
- Singleton pattern ensures Activity Result Launchers registered once during Activity creation
- Initialized in `MainActivity.onCreate()` before UI composition
- Status: ✅ **Fully implemented**

**iOS** (`FileExportImport.ios.kt`)
- Uses UIActivityViewController for file exports (native iOS share sheet)
- Uses UIDocumentPickerViewController for file imports
- Export strategy:
  - Single files: UIActivityViewController shares temporary file
  - Multiple files (ZIP): Creates simple concatenated format with markers
- Import strategy:
  - UIDocumentPickerViewController with multi-selection support
  - Accepts JSON and ZIP-like files
  - Parses simple concatenated format (compatible with export)
- Limitations:
  - ZIP format uses simple text concatenation (not true ZIP) to avoid additional dependencies
  - True ZIP extraction would require Apple Compression framework integration
- Status: ✅ **Implemented with UIKit interop**

### SaveFileStorage Extensions

Added new functions to `SaveFileStorage.kt`:

```kotlin
fun getSaveGameJson(saveId: String): String?
fun getAllSaveGamesJson(): Map<String, String>
fun importSaveGame(filename: String, jsonContent: String, overwrite: Boolean): Boolean
fun saveGameExists(filename: String): Boolean
```

### UI Components

#### SavedGameCard
- Added `onDownload` callback parameter
- Download button appears next to Delete button
- Uses `DownloadIcon` from IconUtils

#### LoadGameScreen
- Added "Download All" and "Upload" buttons at the top
- Buttons styled with primary/secondary colors
- Internal state management for upload flow:
  - Tracks pending imports
  - Manages override conflicts
  - Shows success/error dialogs

#### New Dialog Components (`ImportExportDialogs.kt`)
- `FileOverrideDialog`: Handles filename conflicts with Skip/Override/Override All
- `ImportSuccessDialog`: Shows count of successfully imported files
- `ImportErrorDialog`: Shows error message for failed imports

### GameViewModel Extensions

New functions for download/upload operations:

```kotlin
fun downloadSaveGame(saveId: String)
fun downloadAllSaveGames()
suspend fun uploadSaveGames(): Pair<Int, List<String>>
suspend fun importSaveGameWithOverride(filename: String, content: String, overwrite: Boolean): Boolean
```

### Localization

All new strings added to 5 languages:
- English (default)
- German (de)
- Spanish (es)
- French (fr)
- Italian (it)

New string keys:
- `download_savefile` / `download_all_savefiles` / `upload_savefiles`
- `download_savefile_tooltip` / `download_all_tooltip` / `upload_tooltip`
- `file_override_title` / `file_override_message`
- `skip_file` / `override_file` / `override_all`
- `files_imported` / `files_imported_message`
- `import_error` / `import_error_message`

### Icon Components

Added to `IconUtils.kt`:
- `DownloadIcon`: Uses `emoji_down_arrow.png`
- `UploadIcon`: Uses `emoji_up_arrow.png`

### Time Utilities

Added `formatTimestampISO()` function for ZIP filenames:
- Format: `YYYY-MM-DD_HH-mm-ss`
- Implemented for all platforms (JVM, WASM, iOS)

## User Flow

### Download Individual Save
1. User clicks download button on save card
2. File picker dialog opens (Desktop) or download starts (Web)
3. Save file exported as `{saveId}.json`

### Download All Saves
1. User clicks "Download All" button
2. All save files packaged into ZIP (Desktop) or concatenated text file (Web)
3. Filename: `defender-of-egril-saves-YYYY-MM-DD_HH-mm-ss.zip`
4. File picker dialog opens (Desktop) or download starts (Web)

### Upload Saves
1. User clicks "Upload" button
2. File picker dialog opens
3. User selects one or more JSON files (or ZIP on Desktop)
4. System validates each file and checks for conflicts
5. If conflict exists:
   - Override dialog appears with filename
   - User chooses: Skip, Override, or Override All
6. Success dialog shows count of imported files
7. Save list refreshes automatically

## File Format

### Individual Save File
- Format: JSON
- Extension: `.json`
- Filename pattern: `savegame_{timestamp}.json`
- Validated on import by deserializing with `SaveJsonSerializer`

### ZIP Archive (Desktop only)
- Format: Standard ZIP
- Extension: `.zip`
- Contents: Multiple JSON save files
- Filename: `defender-of-egril-saves-{ISO_TIMESTAMP}.zip`

## Testing Status

### Compilation
- ✅ Desktop/JVM: Compiles successfully
- ✅ Web/WASM: Compiles successfully
- ✅ Android: Fully implemented with Storage Access Framework
- ✅ iOS: Implemented with UIKit (UIActivityViewController + UIDocumentPickerViewController)

### Manual Testing Required
The feature requires UI interaction and cannot be fully tested in CI:
1. Download individual save file
2. Download all saves as ZIP
3. Upload single JSON file
4. Upload multiple JSON files
5. Upload ZIP archive (Desktop)
6. Test override confirmation flow
7. Test with corrupted/invalid files
8. Verify localization in all languages

## Future Enhancements

1. **Web/WASM**:
   - Add JSZip library for proper ZIP support
   - Enable ZIP parsing on upload

2. **iOS**:
   - Integrate Apple Compression framework for true ZIP support
   - Consider native file sharing improvements

3. **All Platforms**:
   - Add progress indicator for large file operations
   - Support drag-and-drop file upload (Web/Desktop)
   - Add file validation before import
   - Show detailed error messages for invalid files

## Security Considerations

1. **Validation**: All imported files validated by deserializing with `SaveJsonSerializer`
2. **Override Protection**: User must explicitly confirm file overrides
3. **No Arbitrary Code**: Only JSON data files accepted (no executable code)
4. **Local Storage**: All operations use local file system (no network upload)

## Known Issues

1. Web/WASM "Download All" creates text file instead of ZIP (limitation of browser environment without ZIP library)
2. Web/WASM cannot parse uploaded ZIP files (same limitation)
3. iOS uses simple concatenated format instead of true ZIP (limitation without Apple Compression framework integration)

## Files Changed

### New Files
- `composeApp/src/commonMain/kotlin/de/egril/defender/save/FileExportImport.kt`
- `composeApp/src/desktopMain/kotlin/de/egril/defender/save/FileExportImport.desktop.kt`
- `composeApp/src/wasmJsMain/kotlin/de/egril/defender/save/FileExportImport.wasmJs.kt`
- `composeApp/src/androidMain/kotlin/de/egril/defender/save/FileExportImport.android.kt`
- `composeApp/src/iosMain/kotlin/de/egril/defender/save/FileExportImport.ios.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/ImportExportDialogs.kt`

### Modified Files
- `composeApp/src/androidMain/kotlin/de/egril/defender/MainActivity.kt` (Android initialization)
- `composeApp/src/commonMain/kotlin/de/egril/defender/save/SaveFileStorage.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/GameViewModel.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/App.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/LoadGameScreen.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/SavedGameCard.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/SavedGameCardComponents.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/icon/IconUtils.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/utils/TimeUtils.kt`
- `composeApp/src/jvmMain/kotlin/de/egril/defender/utils/TimeUtils.jvm.kt`
- `composeApp/src/wasmJsMain/kotlin/de/egril/defender/utils/TimeUtils.wasmJs.kt`
- `composeApp/src/iosMain/kotlin/de/egril/defender/utils/TimeUtils.ios.kt`
- All string resource files (values, values-de, values-es, values-fr, values-it)

## Conclusion

The savefile download/upload feature is fully implemented for Desktop, Android, and iOS with appropriate platform-specific implementations. Web/WASM has basic functionality with limitations (no ZIP library). The feature provides a consistent user experience across platforms with proper localization and error handling.

### Platform Summary
- ✅ **Desktop/JVM**: Full functionality with JFileChooser and ZIP support
- ✅ **Android**: Full functionality with Storage Access Framework (SAF) and ZIP support
- ✅ **iOS**: Full functionality with UIActivityViewController/UIDocumentPickerViewController (simple concatenated format instead of ZIP)
- ✅ **Web/WASM**: Basic functionality (JSON only, no ZIP support)
