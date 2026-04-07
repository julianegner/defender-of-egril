# UI Test Screenshots

This directory contains screenshots captured during UI test execution.

## Purpose

Screenshots are automatically generated when running UI tests to provide visual verification of the application's appearance in different states.

## Contents

### Desktop Tests (`desktopTest`)
During desktop test execution, the following screenshots are generated:

#### Main Menu
- `main-menu-screen.png` - The main application menu

#### Level Complete Screens
- `level-complete-victory.png` - Victory screen after winning a level
- `level-complete-defeat.png` - Defeat screen after losing a level
- `level-complete-final-victory.png` - Final victory screen after completing the game

#### World Map
- `world-map-screen.png` - World map with mixed level states
- `world-map-all-locked.png` - All levels locked
- `world-map-all-unlocked.png` - All levels unlocked
- `world-map-all-won.png` - All levels completed

#### Gameplay
- `gameplay-screen-initial.png` - Gameplay screen in initial state
- `gameplay-screen-with-enemies.png` - Gameplay screen with enemies spawned
- `gameplay-screen-building-phase.png` - Initial building phase

#### Settings & Rules
- `settings-dialog.png` - Settings dialog with language selection
- `rules-screen.png` - Rules and help screen

### Playwright Tests (`playwright/`)
Automated UI tests that play through the game and capture screenshots at each step:

- Screenshots are numbered sequentially (e.g., `001_initial_load.png`, `002_game_loaded.png`)
- Each test step generates a screenshot with a descriptive name
- Includes full gameplay progression from main menu to level completion

## Generating Screenshots

### Desktop Tests
Run the desktop UI tests to regenerate screenshots:

```bash
./gradlew :composeApp:desktopTest
```

### Playwright Tests
Run Playwright tests locally (requires Node.js):

```bash
npm install
npx playwright install chromium
npx playwright test
```

Or run via GitHub Actions (manually triggered):
1. Go to the Actions tab in GitHub
2. Select "Playwright UI Tests" workflow
3. Click "Run workflow"
4. Download artifacts from the completed workflow run

## Accessing Screenshots from GitHub Actions

When Playwright tests run in GitHub Actions:

1. Navigate to the **Actions** tab in the GitHub repository
2. Click on the **Playwright UI Tests** workflow run
3. Scroll down to the **Artifacts** section
4. Download the available artifacts:
   - `test-screenshots` - All captured screenshots from the test run
   - `playwright-report` - Interactive HTML report with test results
   - `test-videos` - Video recordings of failed tests (if any)

The artifacts are retained for 30 days and can be downloaded as ZIP files.

## Usage

These screenshots can be used for:
- Visual verification of UI changes
- Documentation and presentations
- Comparison with previous versions (visual regression testing)
- Accessibility review
- Debugging test failures

## Notes

- Screenshots are regenerated on each test run
- Image files are excluded from git (see `.gitignore`)
- Screenshot dimensions can be configured in test files
- The README file (this file) is committed to git for documentation
- Playwright screenshots are automatically organized by test run

## Accessibility

Screenshots are stored in this directory to make them easily accessible to:
- Developers reviewing UI changes
- GitHub Copilot for understanding the UI state
- CI/CD tools for visual verification
- Documentation generators

For more information, see [UI_TESTING_GUIDE.md](../UI_TESTING_GUIDE.md) (if available)
