# UI Test Screenshots

This directory contains screenshots captured during UI test execution.

## Purpose

Screenshots are automatically generated when running UI tests to provide visual verification of the application's appearance in different states.

## Contents

During test execution, the following screenshots are generated:

### Main Menu
- `main-menu-screen.png` - The main application menu

### Level Complete Screens
- `level-complete-victory.png` - Victory screen after winning a level
- `level-complete-defeat.png` - Defeat screen after losing a level
- `level-complete-final-victory.png` - Final victory screen after completing the game

### World Map
- `world-map-screen.png` - World map with mixed level states
- `world-map-all-locked.png` - All levels locked
- `world-map-all-unlocked.png` - All levels unlocked
- `world-map-all-won.png` - All levels completed

### Gameplay
- `gameplay-screen-initial.png` - Gameplay screen in initial state
- `gameplay-screen-with-enemies.png` - Gameplay screen with enemies spawned
- `gameplay-screen-building-phase.png` - Initial building phase

### Settings & Rules
- `settings-dialog.png` - Settings dialog with language selection
- `rules-screen.png` - Rules and help screen

## Generating Screenshots

Run the UI tests to regenerate screenshots:

```bash
./gradlew :composeApp:desktopTest
```

## Usage

These screenshots can be used for:
- Visual verification of UI changes
- Documentation and presentations
- Comparison with previous versions (visual regression testing)
- Accessibility review

## Notes

- Screenshots are regenerated on each test run
- Image files are excluded from git (see `.gitignore`)
- Screenshot dimensions can be configured in test files
- The README file (this file) is committed to git for documentation

## Accessibility

Screenshots are stored in this directory to make them easily accessible to:
- Developers reviewing UI changes
- GitHub Copilot for understanding the UI state
- CI/CD tools for visual verification
- Documentation generators

For more information, see [UI_TESTING_GUIDE.md](../UI_TESTING_GUIDE.md)
