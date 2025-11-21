# UI Testing Guide

This document describes the UI/Frontend testing infrastructure for Defender of Egril.

## Overview

The project includes automated UI tests for all major screens and components using Compose Multiplatform's testing framework. These tests verify that UI components render correctly and capture screenshots for visual verification.

## Test Structure

### Test Location
- **Desktop Tests**: `composeApp/src/desktopTest/kotlin/com/defenderofegril/ui/`
- **Screenshot Output**: `test-screenshots/`

### Test Files
1. **MainMenuScreenTest.kt** - Tests for the main menu screen
2. **LevelCompleteScreenTest.kt** - Tests for victory/defeat screens
3. **WorldMapScreenTest.kt** - Tests for the world map with different level states
4. **GamePlayScreenTest.kt** - Tests for the gameplay screen
5. **SettingsDialogTest.kt** - Tests for the settings dialog
6. **RulesScreenTest.kt** - Tests for the rules screen

### Screenshot Test Utility
The `ScreenshotTestUtils.kt` provides functionality to:
- Capture screenshots of Compose UI components during tests
- Save screenshots to the `test-screenshots/` directory
- Make screenshots accessible for visual verification

## Running Tests

### Run All UI Tests
```bash
./gradlew :composeApp:desktopTest
```

### Run Specific Test Class
```bash
./gradlew :composeApp:desktopTest --tests "com.defenderofegril.ui.MainMenuScreenTest"
```

### View Test Results
After running tests, view the HTML report at:
```
composeApp/build/reports/tests/desktopTest/index.html
```

## Test Coverage

The UI tests cover:

### Main Menu Screen
- ✅ Screen renders correctly with title and buttons
- ✅ Start Game button is clickable
- ✅ Rules button is clickable
- ✅ Settings button is present

### Level Complete Screen
- ✅ Victory state (battle won)
- ✅ Defeat state
- ✅ Final victory state (game completed)
- ✅ Button interactions

### World Map Screen
- ✅ Map renders with multiple levels
- ✅ All levels locked state
- ✅ All levels unlocked state
- ✅ All levels won state
- ✅ Button interactions (Back, Rules, Load Game)

### Game Play Screen
- ✅ Initial state rendering
- ✅ Game with enemies
- ✅ Initial building phase
- ✅ Map rendering

### Settings Dialog
- ✅ Dialog renders correctly
- ✅ Language section is present
- ✅ Close button works
- ✅ Language chooser is displayed

### Rules Screen
- ✅ Screen renders correctly
- ✅ Content is displayed
- ✅ Scrollable content

## Screenshots

Screenshots are automatically captured during test execution and saved to the `test-screenshots/` directory. Each test captures one or more screenshots showing different states of the UI.

### Screenshot Files
- `main-menu-screen.png` - Main menu
- `level-complete-victory.png` - Victory screen
- `level-complete-defeat.png` - Defeat screen
- `level-complete-final-victory.png` - Final victory screen
- `world-map-screen.png` - World map with mixed level states
- `world-map-all-locked.png` - All levels locked
- `world-map-all-unlocked.png` - All levels unlocked
- `world-map-all-won.png` - All levels completed
- `gameplay-screen-initial.png` - Gameplay initial state
- `gameplay-screen-with-enemies.png` - Gameplay with enemies
- `gameplay-screen-building-phase.png` - Initial building phase
- `settings-dialog.png` - Settings dialog
- `rules-screen.png` - Rules screen

## Test Implementation Details

### Testing Framework
- **Compose UI Test** - JetBrains Compose Multiplatform testing library
- **JUnit 4** - Test runner
- **Desktop Target** - Tests run on JVM/Desktop platform

### Test Patterns

#### Basic Screen Test
```kotlin
@Test
fun testScreenRendersCorrectly() {
    composeTestRule.setContent {
        MyScreen(onAction = {})
    }
    
    composeTestRule.waitForIdle()
    
    // Verify UI elements
    composeTestRule.onNodeWithText("Expected Text")
        .assertExists()
    
    // Capture screenshot
    ScreenshotTestUtils.captureScreenshot(
        composeTestRule,
        "screen-name",
        width = 1200,
        height = 800
    )
}
```

#### Testing User Interactions
```kotlin
@Test
fun testButtonClick() {
    var clicked = false
    
    composeTestRule.setContent {
        MyScreen(onButtonClick = { clicked = true })
    }
    
    composeTestRule.onNodeWithText("Button")
        .performClick()
    
    assert(clicked) { "Button should trigger callback" }
}
```

#### Testing Different States
```kotlin
@Test
fun testStateVariations() {
    val states = listOf(State.INITIAL, State.LOADING, State.SUCCESS)
    
    states.forEach { state ->
        composeTestRule.setContent {
            MyComponent(state = state)
        }
        
        // Verify state-specific UI
        // Capture screenshot for each state
    }
}
```

## Continuous Integration

The UI tests are designed to run in CI environments:
- Tests run on Linux/Ubuntu
- No display/graphics required (headless testing)
- Screenshots captured to git-tracked directory
- Test reports generated as HTML artifacts

## Accessibility to GitHub Copilot

Screenshots and test results are:
1. **Saved to `test-screenshots/`** - A git-tracked directory accessible to all users and tools
2. **Referenced in test reports** - HTML reports link to screenshots
3. **Generated during CI** - Available in CI artifacts and logs
4. **Viewable in repository** - Screenshots committed to git can be viewed by GitHub Copilot and users

## Adding New UI Tests

### Steps to Add a New Test

1. Create a new test file in `composeApp/src/desktopTest/kotlin/com/defenderofegril/ui/`
2. Follow the naming convention: `<ComponentName>Test.kt`
3. Use `@get:Rule val composeTestRule = createComposeRule()`
4. Write tests using `@Test` annotation
5. Capture screenshots using `ScreenshotTestUtils.captureScreenshot()`
6. Run tests with `./gradlew :composeApp:desktopTest`

### Example New Test

```kotlin
package com.defenderofegril.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class NewComponentTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testNewComponentRendersCorrectly() {
        composeTestRule.setContent {
            NewComponent()
        }
        
        composeTestRule.waitForIdle()
        
        // Add assertions
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "new-component",
            width = 800,
            height = 600
        )
    }
}
```

## Limitations

### Current Limitations
1. **Screenshot Quality**: Screenshots are currently placeholder images. Full rendering capture requires platform-specific implementations.
2. **Platform Support**: Tests currently run on desktop/JVM only. Android and iOS UI tests would require platform-specific test infrastructure.
3. **Localization**: Tests avoid checking specific text strings since the app supports multiple languages.

### Future Improvements
- Implement actual screenshot capture using platform-specific rendering APIs
- Add visual regression testing (screenshot comparison)
- Extend to Android instrumented tests
- Add iOS UI tests using XCTest
- Implement accessibility testing
- Add performance benchmarks for UI rendering

## Troubleshooting

### Tests Fail with "Node not found"
This usually means localized text is being tested. Avoid testing specific text strings. Instead:
- Test for component existence using `onRoot()`
- Test for clickable elements using `assertHasClickAction()`
- Test for display using `assertIsDisplayed()`

### Screenshots Not Generated
Check that:
- The `test-screenshots/` directory exists
- Tests are actually running (check test reports)
- ScreenshotTestUtils is being called in tests

### Tests Pass Locally But Fail in CI
- Verify that the CI environment has the required dependencies
- Check that the test environment is headless-compatible
- Review CI logs for specific error messages

## References

- [Compose Multiplatform Testing Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html)
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Gradle Testing Documentation](https://docs.gradle.org/current/userguide/java_testing.html)
