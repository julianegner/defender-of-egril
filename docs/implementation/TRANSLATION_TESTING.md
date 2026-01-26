# Translation Testing Implementation

## Overview

This document describes the comprehensive test suite implemented to detect and prevent missing or broken translation strings that would display as "???" in the application.

## Problem Statement

When running the app in German (or other non-English languages), some texts were not displayed correctly, instead showing "???" where translated strings should appear. This issue is caused by the `compose-multiplatform-localize` plugin returning "???" when a translation key is missing, empty, or otherwise broken.

## Root Causes for "???" Display

The test suite identifies five primary causes for "???" appearing in the application:

1. **Missing keys in translation files** - A key exists in English but not in other language files
2. **Empty or whitespace-only translations** - Key exists but has no value
3. **Keys referenced in code but not defined** - Code references a key that doesn't exist in any strings.xml
4. **Malformed XML** - Invalid XML structure that fails to parse
5. **Duplicate keys** - Multiple definitions of the same key causing conflicts

## Test Suite Implementation

The test suite is located in:
```
composeApp/src/desktopTest/kotlin/de/egril/defender/ui/TranslationCoverageTest.kt
```

### Test 1: testNoHardcodedStringsInUI

**Purpose**: Scans UI code for hardcoded strings that should be localized.

**What it checks**:
- All text displayed in UI uses `stringResource(Res.string.key_name)`
- No hardcoded strings like `Text("Hello World")`

**Exceptions**:
- Cheat codes (not translated by design)
- Single-character symbols (•, X, +, -)
- Variable interpolations ($variable)
- Numeric and formatting strings

**Example failure message**:
```
Found 2 hardcoded string(s) that should be translated:

  ui/GameScreen.kt:42 - "Game Over"
  ui/MenuScreen.kt:15 - "Click here to start"

All user-facing strings should use stringResource(Res.string.key_name)
```

### Test 2: testAllLanguageFilesHaveSameKeys

**Purpose**: Verifies that all language files have identical keys.

**What it checks**:
- German (de), Spanish (es), French (fr), and Italian (it) files have all English keys
- No extra keys exist in non-English files
- All keys are synchronized across languages

**Example failure message**:
```
Translation files are not synchronized:

  values-de/ is missing keys: achievement_new_tower, settings_sound
  values-es/ has extra keys not in English: old_unused_key
```

### Test 3: testNoEmptyOrWhitespaceOnlyTranslations

**Purpose**: Detects empty translation values that would display as "???".

**What it checks**:
- All `<string>` tags have non-empty values
- No whitespace-only translations
- Checks all language files

**Example failure message**:
```
Found 1 empty or whitespace-only translation(s):
Empty translations will display as '???' in the app.

  values-de/strings.xml:20 - Key 'victory' has empty value

All translations must have non-empty values.
```

### Test 4: testAllReferencedKeysExist

**Purpose**: Validates that all keys referenced in code are defined in strings.xml.

**What it checks**:
- Scans ALL UI files for `stringResource(Res.string.xxx)` calls (covers ~680+ keys)
- Also checks `LocalizationUtils.kt`, `NameLocalizationUtils.kt`, and `AchievementLocalization.kt`
- Verifies each key exists in English strings.xml
- Skips comments to avoid false positives

**Coverage**: This test now validates **all** stringResource usages throughout the entire UI codebase, not just specific utility files.

**Example failure message**:
```
Found 2 key(s) referenced in code but not defined in strings.xml:
These will display as '???' in the app.

  - new_tower_type_name
  - settings_advanced_option

All referenced keys must be defined in values/strings.xml
```

### Test 5: testXmlFilesAreWellFormed

**Purpose**: Validates XML structure and prevents parsing errors.

**What it checks**:
- XML declaration present (`<?xml version="1.0"?>`)
- Root `<resources>` element exists
- Matching open and close `<string>` tags
- No duplicate keys within a file

**Example failure message**:
```
Found 2 XML structure issue(s):
Malformed XML will cause translations to fail and display as '???'

  values-de/strings.xml - Mismatched <string> tags (open: 887, close: 886)
  values-es/strings.xml:245 - Duplicate key 'victory' (first defined at line 20)
```

### Test 6: testParameterizedStringsMatchAcrossLanguages

**Purpose**: Validates that parameterized strings use consistent placeholders across all languages.

**What it checks**:
- Extracts all parameterized strings (containing %s, %d, %1$s, %2$d, etc.)
- Compares parameter placeholders between English and each language
- Detects mismatches like English using %s but German using %d
- Parameter mismatches cause "???" or incorrect formatting at runtime

**Example failure message**:
```
Found 2 parameterized string(s) with mismatched parameters:
Parameter mismatches can cause '???' or incorrect formatting at runtime.

  values-de/strings.xml - Key 'player_count' has parameter mismatch
    English: [%d]
    values-de: [%s]
  values-es/strings.xml - Key 'time_remaining' has parameter mismatch
    English: [%1$d, %2$s]
    values-es: [%1$s, %2$d]

All languages must use the same parameter placeholders (e.g., %s, %d, %1$s, %2$d)
```

## Running the Tests

### Run all translation tests:
```bash
./gradlew :composeApp:desktopTest --tests "de.egril.defender.ui.TranslationCoverageTest"
```

### Run a specific test:
```bash
./gradlew :composeApp:desktopTest --tests "de.egril.defender.ui.TranslationCoverageTest.testNoEmptyOrWhitespaceOnlyTranslations"
```

### Run as part of full test suite:
```bash
./gradlew :composeApp:test
```

### Test 7: testNoStringResourceWithReplace

**Purpose**: Detects incorrect parameter passing that causes "???".

**What it checks**:
- Scans all UI files for `stringResource(...).replace(...)` patterns
- This pattern doesn't work with the localization plugin
- Parameters must be passed directly to stringResource, not via .replace()

**Why this matters**: The compose-multiplatform-localize plugin requires parameters to be passed directly to the function, not post-processed with .replace(). Using .replace() causes "???" to appear because the plugin doesn't see the actual string value.

**Example failure message**:
```
Found 1 case(s) of stringResource().replace() pattern:
This pattern causes '???' because parameters are not passed correctly.

  ui/gameplay/GameDialogs.kt:330
    stringResource(Res.string.time_for_break_message).replace("%s", it)

Correct usage: stringResource(Res.string.key, param1, param2)
Wrong usage: stringResource(Res.string.key).replace("%s", param1)
```

## Test Results

All tests currently pass:
- ✅ testNoHardcodedStringsInUI
- ✅ testAllLanguageFilesHaveSameKeys
- ✅ testNoEmptyOrWhitespaceOnlyTranslations
- ✅ testAllReferencedKeysExist (now checks 684+ keys!)
- ✅ testXmlFilesAreWellFormed
- ✅ testParameterizedStringsMatchAcrossLanguages
- ✅ testNoStringResourceWithReplace (NEW)

**Total: 7 tests, 0 failures**

## Continuous Integration

These tests should be run as part of the CI pipeline to prevent broken translations from being merged:

```yaml
# Example CI configuration
- name: Run Translation Tests
  run: ./gradlew :composeApp:desktopTest --tests "de.egril.defender.ui.TranslationCoverageTest"
```

## Fixing Issues

### When testAllLanguageFilesHaveSameKeys fails:

1. Open the missing language file (e.g., `values-de/strings.xml`)
2. Add the missing keys with translated values
3. Run the test again to verify

### When testNoEmptyOrWhitespaceOnlyTranslations fails:

1. Navigate to the file and line number indicated
2. Add a proper translation value between the `<string>` tags
3. Ensure the translation is not just whitespace

### When testAllReferencedKeysExist fails:

1. Add the missing key to `values/strings.xml`
2. Translate it and add to all other language files
3. Verify the key name matches exactly (case-sensitive)

### When testXmlFilesAreWellFormed fails:

1. Check for unclosed tags or missing closing tags
2. Remove any duplicate key definitions
3. Ensure XML declaration is present at the top
4. Validate XML structure using an XML validator

## Benefits

This test suite provides:

1. **Early detection** of translation issues before they reach users
2. **Clear error messages** with exact file locations and key names
3. **Comprehensive coverage** of all common translation problems
4. **Fast feedback** during development (tests run in ~4 seconds)
5. **Prevention** of "???" appearing in production

## Maintenance

When adding new features:

1. Add new string keys to `values/strings.xml` first
2. Translate to all supported languages (de, es, fr, it)
3. Use `stringResource(Res.string.key_name)` in UI code
4. Run translation tests to verify

The test suite will automatically validate new strings without requiring updates to the tests themselves.

## Technical Details

### Key Extraction Algorithm

The test uses regex patterns to extract keys from code:
- Pattern: `["']([a-z_][a-z0-9_]*)["']`
- Filters: Must contain underscore, no file paths, no URLs
- Skips: Comments (lines starting with //, *, or /*)

### XML Parsing

- Uses regex to avoid dependency on XML parsing libraries
- Lightweight and fast for test execution
- Handles all common XML issues

### Project Root Detection

Tests automatically detect project root whether run from:
- Repository root directory
- composeApp subdirectory
- CI environment

This ensures tests work consistently across all environments.
