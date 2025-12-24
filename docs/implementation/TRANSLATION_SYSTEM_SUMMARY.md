# Translation System Implementation Summary

## Problem Statement
The game loads level names, map names, and world map location names from JSON files that can be changed or added at runtime. The standard static string resource approach (values/strings.xml) doesn't work for user-created or downloaded content.

## Solution Overview
Implemented an **optional translation key system** with automatic fallback:
- Built-in content can be translated via string resources
- User-created content uses direct names (no translation)
- Fully backward compatible with existing JSON files

## Implementation Details

### 1. Data Model Changes (EditorModels.kt, WorldMapModels.kt)
Added optional translation key fields:

```kotlin
data class EditorMap(
    val name: String = "",
    val nameKey: String? = null,  // NEW
    // ...
)

data class EditorLevel(
    val title: String,
    val titleKey: String? = null,  // NEW
    val subtitle: String = "",
    val subtitleKey: String? = null,  // NEW
    // ...
)

data class WorldMapLocationData(
    val name: String,
    val nameKey: String? = null,  // NEW
    // ...
)
```

### 2. JSON Serialization (EditorJsonSerializer.kt)
Updated serialization to include optional keys:
- Serialize: Include `nameKey` if not null
- Deserialize: Handle missing keys gracefully (backward compatible)

### 3. Localization Utilities (NameLocalizationUtils.kt)
Created extension functions for getting localized names:

```kotlin
fun EditorLevel.getLocalizedTitle(): String
fun EditorLevel.getLocalizedSubtitle(): String
fun EditorMap.getLocalizedName(): String
fun WorldMapLocationData.getLocalizedName(): String
```

Logic:
1. If `titleKey` is not null, try to look it up in string resources
2. If lookup fails or key is null, fall back to direct `title`

### 4. String Resources (values/strings.xml, values-XX/strings.xml)
Added example translations for built-in levels:
- English, German, Spanish, French, Italian
- Tutorial, First Wave, Mixed Forces, Ork Invasion, Dark Magic, Final Stand

### 5. Documentation
Created comprehensive documentation:
- `DYNAMIC_CONTENT_TRANSLATION.md` - Complete guide
- `TranslationExamples.kt` - Code examples

## Usage

### For Built-in Content (Translatable)
```kotlin
EditorLevel(
    id = "level_tutorial",
    title = "Welcome to Defender of Egril",  // English fallback
    titleKey = "level_tutorial_title",        // Translation key
    subtitle = "Tutorial",
    subtitleKey = "level_tutorial_subtitle",
    // ...
)
```

### For User Content (No Translation)
```kotlin
EditorLevel(
    id = "custom_level",
    title = "Bob's Challenge",  // Direct name
    titleKey = null,             // No translation
    // ...
)
```

### In UI
```kotlin
// OLD:
Text(level.title)

// NEW:
Text(level.getLocalizedTitle())
```

## Benefits

1. **Translatable Built-in Content**: Game levels can be translated into multiple languages
2. **User-Friendly**: Custom levels don't require translation setup
3. **Backward Compatible**: Old JSON files work without modification
4. **Flexible**: Downloaded content can optionally include translation keys
5. **Fail-Safe**: Always falls back to direct names if translation fails

## Testing

The system is designed to work immediately:
- Old JSON files → Use direct names
- New JSON files with keys → Use translations if available
- Missing translations → Automatically fall back to direct names

## Future Enhancements (Optional)

1. **UI Integration**: Update all UI to use `getLocalizedTitle()` methods
2. **Level Editor**: Add UI for setting translation keys
3. **Bulk Translation**: Tool to add translation keys to all built-in content
4. **Translation Verification**: Check that all built-in content has translations

## Files Changed

### Core Implementation
- `composeApp/src/commonMain/kotlin/de/egril/defender/editor/EditorModels.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/editor/WorldMapModels.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/editor/EditorJsonSerializer.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/NameLocalizationUtils.kt`

### Documentation & Examples
- `docs/implementation/DYNAMIC_CONTENT_TRANSLATION.md`
- `composeApp/src/commonMain/kotlin/de/egril/defender/editor/TranslationExamples.kt`

### String Resources (5 languages)
- `composeApp/src/commonMain/composeResources/values/strings.xml`
- `composeApp/src/commonMain/composeResources/values-de/strings.xml`
- `composeApp/src/commonMain/composeResources/values-es/strings.xml`
- `composeApp/src/commonMain/composeResources/values-fr/strings.xml`
- `composeApp/src/commonMain/composeResources/values-it/strings.xml`

## Conclusion

The implementation provides a complete solution that:
- ✅ Enables translation of built-in level/map/location names
- ✅ Preserves user experience for custom content
- ✅ Maintains full backward compatibility
- ✅ Requires no changes to existing JSON files
- ✅ Provides clear documentation and examples

The system is production-ready and can be adopted incrementally by:
1. Adding translation keys to built-in content
2. Updating UI components to use localization functions
3. Adding more translations over time
