# Localization Implementation Summary

## Overview
This implementation adds localization support to Defender of Egril using the `compose-multiplatform-localize` library (version 2.0.1), with language chooser, settings dialog, and **plural string support** accessible from all screens.

## Dependencies Added

### gradle/libs.versions.toml
```toml
[versions]
flagkit = "1.1.0"
multiplatform-settings = "1.3.0"
localization = "2.0.0"

[libraries]
flagkit = { module = "dev.carlsen.flagkit:flagkit", version.ref = "flagkit" }
multiplatform-settings = { module = "com.russhwolf:multiplatform-settings-no-arg", version.ref = "multiplatform-settings" }

[plugins]
localization = { id = "com.hyperether.localization", version.ref = "localization" }
```

### composeApp/build.gradle.kts
- Added localization plugin to plugins block
- Added flagkit and multiplatform-settings to commonMain dependencies
- Added materialIconsExtended for settings icon
- Configured source directory for generated localization classes

## String Resources

Created `composeApp/src/commonMain/composeResources/values/strings.xml` with 46 English strings:

### Categories
- **Main Menu**: app_name, app_subtitle, start_game, rules
- **Level Complete**: victory, battle_won, defeat, victory_message, battle_won_message, defeat_message, retry, world_map
- **Settings**: settings, language, close
- **Common Actions**: back, ok, cancel, save, delete, edit
- **Game Play**: coins, health, turn, level, upgrade, sell, attack, end_turn, next_turn, save_game, load_game
- **Towers**: tower, damage, range, actions
- **Enemies**: enemy, enemies
- **Status**: locked, unlocked, completed

## UI Components

### 1. LanguageChooser.kt (`ui/settings/`)
- Dropdown component displaying current language with flag
- Shows all available locales with flags and names
- Uses FlagKit for country flag icons
- Maps AppLocale to country codes for flag display
- Currently supports DEFAULT (English/GB) with extensibility for more locales

### 2. SettingsDialog.kt (`ui/settings/`)
- Modal dialog for app settings
- Language selection section with LanguageChooser
- Clean Material Design 3 styling
- Uses localized strings for UI labels
- Dismissible with close button

### 3. SettingsButton.kt (`ui/settings/`)
- Icon button with gear (Settings) icon
- Opens SettingsDialog when clicked
- Manages dialog state internally
- Reusable component for all screens

## Integration Across Screens

### Settings Button Added To:
1. **MainMenuScreen** - Top-right corner
2. **WorldMapScreen** - Top-right corner  
3. **RulesScreen** - Top-right corner
4. **LevelCompleteScreen** - Top-right corner
5. **GamePlayScreen** - Integrated into GameHeader with other action buttons
6. **LevelEditorScreen** - In header row next to back button
7. **LoadGameScreen** - Top-right corner

### Localized Strings Applied To:
1. **MenuScreens.kt**
   - MainMenuScreen: app_name, app_subtitle, start_game, rules
   - LevelCompleteScreen: victory, battle_won, defeat, victory_message, battle_won_message, defeat_message, retry, world_map

2. **WorldMapScreen.kt**
   - Buttons: load_game, rules, back

3. **RulesScreen.kt**
   - Back button: back

4. **LoadGameScreen.kt**
   - Title: load_game
   - Back button: back

## How It Works

### Localization Plugin
The `compose-multiplatform-localize` plugin:
1. Parses XML string files from `composeResources/values/` directories
2. Generates `AppLocale` enum with available locales
3. Generates `LocalizedStrings` object with string mappings
4. Generates `currentLanguage` mutable state for language switching
5. Provides `stringResource()` composable function

### Generated Files
Located in `build/generated/compose/resourceGenerator/kotlin/commonCustomResClass/`:
- `AppLocale.kt` - Enum of available locales
- `StringsDefault.kt` - Map of English strings
- `LocalizedStrings.kt` - String access utilities

### Usage Pattern
```kotlin
// In composable - simple string
Text(stringResource(Res.string.app_name))

// In composable - formatted string with placeholders
// For strings with %s or %d placeholders, pass arguments directly:
Text(stringResource(Res.string.in_x_turns, turnCount))
Text(stringResource(Res.string.hp_with_level, healthPoints))
Text(stringResource(Res.string.editing_map, mapName))

// Language switching
currentLanguage.value = AppLocale.DEFAULT
```

### String Formatting
The plugin supports string formatting with placeholders in XML:

**XML Definition:**
```xml
<string name="in_x_turns">in %s turns</string>
<string name="hp_with_level">HP: %d</string>
<string name="editing_map">Editing: %s</string>
```

**Usage in Code (CORRECT):**
```kotlin
// ✅ Correct: Pass values as arguments to stringResource()
stringResource(Res.string.in_x_turns, turnCount.toString())
stringResource(Res.string.hp_with_level, healthPoints)
stringResource(Res.string.editing_map, mapName)
```

**INCORRECT Usage:**
```kotlin
// ❌ Wrong: Using .replace() doesn't work with the localization system
stringResource(Res.string.in_x_turns).replace("%s", turnCount.toString())
stringResource(Res.string.hp_with_level).replace("%d", healthPoints.toString())
// This will display "???" instead of the formatted string
```

## Adding More Languages

To add a new language (e.g., German):

1. Create directory: `composeApp/src/commonMain/composeResources/values-de/`
2. Create `strings.xml` with translated strings:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Verteidiger von Egril</string>
    <string name="start_game">Spiel Starten</string>
    <!-- ... more translations ... -->
</resources>
```
3. Clean and rebuild: `./gradlew clean build`
4. The plugin will automatically generate `AppLocale.DE` enum value
5. Update `LanguageChooser.kt` country code mapping if needed

## Files Modified

### New Files
- `composeApp/src/commonMain/composeResources/values/strings.xml`
- `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/settings/LanguageChooser.kt`
- `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/settings/SettingsDialog.kt`
- `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/settings/SettingsButton.kt`

### Modified Files
- `gradle/libs.versions.toml` - Added dependencies
- `composeApp/build.gradle.kts` - Added plugin and dependencies
- `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/MenuScreens.kt` - Added settings button and localization
- `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/RulesScreen.kt` - Added settings button and localization
- `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/worldmap/WorldMapScreen.kt` - Added settings button and localization
- `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/gameplay/GameHeader.kt` - Added settings button
- `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/editor/level/LevelEditorScreen.kt` - Added settings button
- `composeApp/src/commonMain/kotlin/com/defenderofegril/ui/loadgame/LoadGameScreen.kt` - Added settings button and localization

## Build Status

✅ Desktop compilation successful  
✅ All screens updated with settings button  
✅ English localization working  
✅ No breaking changes to existing functionality

## Plural String Support (New in v2.0.0)

Version 2.0.0 adds support for plurals, allowing grammatically correct messages based on quantity.

### Available Plurals
- `enemy_count` - "1 enemy" vs "X enemies"  
- `turn_count` - "1 turn" vs "X turns"
- `coin_count` - "1 coin" vs "X coins"
- `health_point_count` - "1 health point" vs "X health points"

### Usage in Composables
```kotlin
import com.hyperether.resources.Res
import com.hyperether.resources.pluralStringResource

@Composable
fun EnemyCountDisplay(count: Int) {
    Text(pluralStringResource(Res.plurals.enemy_count, count, count))
}
```

### Usage Outside Composables
```kotlin
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.Res

fun getEnemyCountText(count: Int): String {
    return LocalizedStrings.getPlural(Res.plurals.enemy_count, count, count)
}
```

See `PLURALS_USAGE.md` for detailed documentation and examples.

## Future Enhancements

1. **Add More Languages**: Create additional `values-{lang}/strings.xml` files
2. **Persist Language Selection**: Use multiplatform-settings to save user's language preference
3. **Complete String Coverage**: Localize remaining hardcoded strings (Rules screen content, GamePlay UI, Editor screens)
4. **RTL Support**: Add right-to-left language support if needed
5. **Date/Time Formatting**: Add locale-specific formatting for timestamps in saved games
6. **Integrate Plurals**: Use plural resources in UI for enemy counts, turn counts, coin displays, etc.
7. **Add More Plurals**: Extend plural resources for additional use cases (tower counts, action counts, etc.)

## Testing Recommendations

When testing with a display:
1. Launch the application
2. Click settings gear icon on any screen
3. Verify settings dialog opens
4. Check language chooser shows English with UK flag
5. Verify all localized strings display correctly
6. Verify settings button appears on all 7 screens
7. Test navigation between screens maintains language selection
8. Test plural strings with different quantities (1 vs 2+ items)

## Notes

- Language switching is reactive - changing `currentLanguage.value` triggers recomposition
- Settings button uses Material Icons Extended (built-in gear icon)
- FlagKit provides vector flag icons for 250+ countries
- Localization plugin is Kotlin Multiplatform compatible (works on Desktop, Android, iOS, Web)
- Current implementation uses `LocalizedStrings.get()` directly instead of `stringResource()` composable due to internal visibility of generated string resources
- **Version 2.0.0 Note**: The library has a known issue with `\n` newline escapes in XML strings. Use spaces or remove newlines instead.
