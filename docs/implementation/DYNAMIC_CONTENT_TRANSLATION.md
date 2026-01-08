# Dynamic Content Translation

## Overview

This document describes how to translate level names, map names, and world map location names that can be loaded at runtime from JSON files.

## The Challenge

Unlike static UI strings, game content (levels, maps, locations) can be:
1. **Built-in** - Shipped with the game
2. **User-created** - Added via the level editor
3. **Downloaded** - Loaded from external sources at runtime

Static string resources (`values/strings.xml`) don't work for user-created content since those strings aren't known at compile time.

## The Solution

We use an **optional translation key** system with fallback to direct names:

### Data Model Changes

Three data classes now have optional translation key fields:

```kotlin
data class EditorMap(
    val id: String,
    val name: String = "",
    val nameKey: String? = null,  // Optional translation key
    // ...
)

data class EditorLevel(
    val id: String,
    val mapId: String,
    val title: String,
    val titleKey: String? = null,  // Optional translation key for title
    val subtitle: String = "",
    val subtitleKey: String? = null,  // Optional translation key for subtitle
    // ...
)

data class WorldMapLocationData(
    val id: String,
    val name: String,
    val nameKey: String? = null,  // Optional translation key
    // ...
)
```

### Translation Logic

The `NameLocalizationUtils.kt` file provides extension functions that:

1. Check if a translation key exists
2. Try to look up the key in string resources
3. Fall back to the direct name if the key is null or not found

```kotlin
fun EditorLevel.getLocalizedTitle(locale: AppLocale = currentLanguage.value): String {
    return if (titleKey != null) {
        try {
            LocalizedStrings.get(titleKey, locale)
        } catch (e: Exception) {
            title  // Fallback to direct title if key not found
        }
    } else {
        title
    }
}
```

## Usage in UI

Instead of directly using `level.title`, `map.name`, or `location.name`, use the localization extension functions:

```kotlin
// Before:
Text(level.title)
Text(map.name)
Text(location.name)

// After:
Text(level.getLocalizedTitle())
Text(map.getLocalizedName())
Text(location.getLocalizedName())
```

## Adding Translations for Built-in Content

### Step 1: Add String Resources

Add translation keys to `values/strings.xml` and all language files:

```xml
<!-- English: values/strings.xml -->
<string name="level_first_wave_title">The First Wave</string>
<string name="level_first_wave_subtitle"></string>
<string name="level_tutorial_title">Welcome to Defender of Egril</string>
<string name="level_tutorial_subtitle">Tutorial</string>

<!-- German: values-de/strings.xml -->
<string name="level_first_wave_title">Die Erste Welle</string>
<string name="level_tutorial_title">Willkommen zu Defender of Egril</string>
<string name="level_tutorial_subtitle">Tutorial</string>
```

### Step 2: Update Built-in Content

When creating built-in levels/maps, provide both the English name AND the translation key:

```kotlin
EditorLevel(
    id = "level_first_wave",
    title = "The First Wave",         // English fallback
    titleKey = "level_first_wave_title",  // Translation key
    subtitle = "",
    subtitleKey = "level_first_wave_subtitle",
    // ...
)
```

### Step 3: Update JSON Files (if already saved)

For existing JSON files, manually add the translation keys:

```json
{
  "id": "level_first_wave",
  "title": "The First Wave",
  "titleKey": "level_first_wave_title",
  "subtitle": "",
  "subtitleKey": "level_first_wave_subtitle",
  ...
}
```

## How It Works for Different Content Types

### Built-in Game Content
- Has `titleKey`/`nameKey` set → Uses string resources → Gets translated
- Falls back to `title`/`name` if translation not found

### User-Created Content
- `titleKey`/`nameKey` is null → Uses `title`/`name` directly → No translation
- Users can name their levels in any language they want

### Downloaded Content
- If creator provided translation keys → Gets translated if strings exist
- Otherwise → Uses direct names

## Example Scenario

1. **English user** plays level "The First Wave":
   - `titleKey = "level_first_wave_title"`
   - Looks up `level_first_wave_title` in English strings
   - Displays: "The First Wave"

2. **German user** plays same level:
   - `titleKey = "level_first_wave_title"`
   - Looks up `level_first_wave_title` in German strings
   - Displays: "Die Erste Welle"

3. **Any user** plays user-created level "Bob's Challenge":
   - `titleKey = null`
   - Uses `title` directly
   - Displays: "Bob's Challenge" (in all languages)

4. **Any user** plays level with non-existent key:
   - `titleKey = "nonexistent_key"`
   - Key not found in strings
   - Falls back to `title`
   - Displays: Whatever is in the `title` field

## Backward Compatibility

The system is fully backward compatible:

- Old JSON files without `nameKey`/`titleKey` fields work perfectly
- They simply use the direct `name`/`title` values
- No migration needed for existing user content

## JSON Format

### Map JSON
```json
{
  "id": "map_tutorial",
  "name": "Tutorial Map",
  "nameKey": "map_tutorial_name",
  "width": 15,
  "height": 8,
  ...
}
```

### Level JSON
```json
{
  "id": "level_tutorial",
  "title": "Welcome to Defender of Egril",
  "titleKey": "level_tutorial_title",
  "subtitle": "Tutorial",
  "subtitleKey": "level_tutorial_subtitle",
  ...
}
```

### World Map Location JSON
```json
{
  "id": "starting_village",
  "name": "Starting Village",
  "nameKey": "location_starting_village",
  "position": {"x": 200, "y": 300},
  "levelIds": ["level_tutorial", "level_first_wave"]
}
```

## Best Practices

1. **Always provide both name and key** for built-in content
   - `name` serves as English fallback
   - `nameKey` enables translation

2. **Use descriptive key names**:
   - `level_first_wave_title` not `level1_title`
   - `map_tutorial_name` not `map1_name`
   - `location_starting_village` not `loc1`

3. **Leave subtitle keys empty if subtitle is empty**:
   ```kotlin
   subtitle = "",
   subtitleKey = null,  // or omit entirely
   ```

4. **Test with multiple languages** to ensure fallbacks work correctly

5. **Don't worry about user content** - it automatically uses direct names

## Implementation Files

- **Data Models**: `EditorModels.kt`, `WorldMapModels.kt`
- **Serialization**: `EditorJsonSerializer.kt`
- **Localization Utils**: `NameLocalizationUtils.kt`
- **String Resources**: `values/strings.xml`, `values-de/strings.xml`, etc.

## Future Enhancements

Possible improvements:

1. **Editor UI Support**: Add fields in the level editor to set translation keys
2. **Translation Pack System**: Allow community-created translation packs for popular custom content
3. **Automatic Key Generation**: Suggest translation keys based on level/map IDs
4. **Translation Verification Tool**: Check that all built-in content has translation keys and translated strings
