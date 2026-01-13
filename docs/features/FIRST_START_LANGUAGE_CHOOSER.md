# First-Start Language Chooser Implementation

## Overview

This document describes the implementation of the first-start language chooser feature, which shows a language selection dialog before the player name input on the first app launch.

## Implementation Details

### 1. Platform Language Detection

Added platform-specific language detection capability to identify the system's default language:

**Common Interface** (`Platform.kt`):
- Added `expect fun getSystemLanguageCode(): String?` to detect system language

**Platform-Specific Implementations**:
- **Desktop (JVM)**: Uses `Locale.getDefault().language.lowercase()`
- **Android**: Uses `Locale.getDefault().language.lowercase()`
- **iOS**: Uses `NSLocale.currentLocale.languageCode`
- **WASM (Web)**: Uses JavaScript `navigator.language` API

### 2. App Settings Extensions

Extended `AppSettings` to manage first-start language selection:

- Added `KEY_LANGUAGE_CHOSEN` setting to track if user has chosen language
- Added `hasChosenLanguage(): Boolean` to check if language was chosen
- Added `markLanguageChosen()` to mark language as chosen after selection
- Added `detectAndPreselectPlatformLanguage(): AppLocale?` to detect and preselect system language

### 3. Initial Language Chooser Dialog

Created new `InitialLanguageChooserDialog` component:

**Features**:
- Shows language selection dialog on first app start
- Automatically preselects platform language if supported
- Reuses existing `LanguageChooser` component for consistency
- Cannot be dismissed - user must make a selection
- Saves selected language and marks it as chosen on continue

**UI Structure**:
```kotlin
Dialog (non-dismissible)
├── Title: "Choose Your Language"
├── Prompt: "Select your preferred language for the game"
├── LanguageChooser (existing component)
└── Continue Button
```

### 4. App Flow Integration

Updated `App.kt` to integrate the language chooser into the first-start flow:

**Flow on First Start**:
1. Check if player profile exists (`needsPlayerSelection`)
2. If no player profile:
   - Check if language has been chosen (`hasChosenLanguage()`)
   - If not chosen: Show `InitialLanguageChooserDialog`
   - After language selection: Show `CreatePlayerDialog`
3. If player profile exists: Normal app flow

**Dialog Chaining**:
```
First Start → Language Chooser → Player Creation → Main Menu
Subsequent Starts → Main Menu (if player exists)
```

### 5. Localization

Added string resources in all supported languages:

| Key | English | German | Spanish | French | Italian |
|-----|---------|--------|---------|--------|---------|
| `initial_language_title` | Choose Your Language | Wähle deine Sprache | Elige tu idioma | Choisissez votre langue | Scegli la tua lingua |
| `initial_language_prompt` | Select your preferred language for the game | Wähle deine bevorzugte Sprache für das Spiel | Selecciona tu idioma preferido para el juego | Sélectionnez votre langue préférée pour le jeu | Seleziona la tua lingua preferita per il gioco |
| `initial_language_continue` | Continue | Weiter | Continuar | Continuer | Continua |

## Testing

### Automated Tests

Created `InitialLanguageChooserDialogTest` with the following test cases:

1. **testInitialLanguageChooserDialogRendersCorrectly**: Verifies dialog renders with all required elements
2. **testInitialLanguageChooserDialogContinueButton**: Verifies continue button functionality and language saving
3. **testLanguageChooserPreselectsSystemLanguage**: Verifies system language preselection

All tests pass successfully.

### Manual Testing Procedure

To test the first-start experience:

1. Clear app data:
   ```bash
   rm -rf ~/.defender-of-egril/settings*
   rm -rf ~/.defender-of-egril/players.json
   ```

2. Launch the application:
   ```bash
   ./gradlew :composeApp:run
   ```

3. Verify the flow:
   - Language chooser dialog appears first
   - System language is preselected (if supported)
   - User can change language selection
   - Clicking "Continue" saves language and shows player creation dialog
   - Cannot dismiss language dialog without selecting

4. Verify subsequent launches:
   - Close and relaunch the app
   - Language chooser should NOT appear again
   - App should use previously selected language

## Design Decisions

1. **System Language Preselection**: Automatically preselects the system language to reduce friction for users. Users can still change it if desired.

2. **Non-Dismissible Dialog**: The language chooser cannot be dismissed without making a selection to ensure users consciously choose their language.

3. **One-Time Only**: The language chooser only appears on the very first start. Users can change language later via the Settings dialog.

4. **Platform-Agnostic Detection**: Language detection is implemented using platform-specific APIs but provides a consistent interface across all platforms.

5. **Fallback to Default**: If system language cannot be detected or is not supported, the app falls back to English (default language).

## Files Modified

### New Files
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/InitialLanguageChooserDialog.kt`
- `composeApp/src/desktopTest/kotlin/de/egril/defender/ui/InitialLanguageChooserDialogTest.kt`

### Modified Files
- `composeApp/src/commonMain/kotlin/de/egril/defender/utils/Platform.kt`
- `composeApp/src/androidMain/kotlin/de/egril/defender/utils/Platform.android.kt`
- `composeApp/src/desktopMain/kotlin/de/egril/defender/utils/Platform.jvm.kt`
- `composeApp/src/iosMain/kotlin/de/egril/defender/utils/Platform.ios.kt`
- `composeApp/src/wasmJsMain/kotlin/de/egril/defender/utils/Platform.wasmJs.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/AppSettings.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/App.kt`
- All string resource files (values/, values-de/, values-es/, values-fr/, values-it/)

## Future Enhancements

Potential improvements for future consideration:

1. **Language Preview**: Show sample text in each language when selecting
2. **Country Flag Enhancement**: Consider region-specific variations (e.g., US vs. UK English, Spain vs. Latin America Spanish)
3. **Automatic Language Switching**: Detect system language changes and prompt user to switch
4. **Language Change Confirmation**: Show a confirmation dialog when changing language in settings to ensure user understands the change

## Compatibility

This implementation is compatible with all supported platforms:
- ✅ Desktop (JVM)
- ✅ Android
- ✅ iOS
- ✅ Web (WASM)

Language detection works on all platforms with appropriate fallbacks if detection fails.
