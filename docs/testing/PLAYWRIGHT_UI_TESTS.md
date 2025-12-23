# Playwright UI Tests Guide

## Overview

Defender of Egril now includes automated UI tests using Playwright that test the game by simulating real user interactions through a web browser. The tests automatically play through the tutorial level, building towers, attacking enemies, and progressing through turns until the level is won or lost.

## Purpose

The Playwright UI tests serve several purposes:

1. **Automated Game Testing**: Verify that the game works end-to-end in a real browser environment
2. **Visual Documentation**: Capture screenshots at each step of gameplay for debugging and documentation
3. **Regression Prevention**: Detect UI and gameplay issues before they reach production
4. **Cross-Browser Verification**: Test the game in different browsers (currently Chromium)

## How It Works

The test suite:

1. **Builds the WASM version** of the game
2. **Starts a development server** on localhost:8080
3. **Launches a browser** (Chromium) with Playwright
4. **Simulates user interactions**:
   - Clicks "Start Game" button
   - Selects the tutorial level from the world map
   - Builds bow towers at strategic locations
   - Attacks enemies each turn
   - Advances through turns until the level is complete
5. **Captures screenshots** at each step for visual verification
6. **Reports results** and uploads artifacts to GitHub

## Running Tests

### Prerequisites

- Node.js 20 or later
- JDK 24 (for building the WASM app)
- Gradle (included via wrapper)

### Local Execution

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Install Playwright browsers**:
   ```bash
   npx playwright install chromium
   ```

3. **Run the tests**:
   ```bash
   npx playwright test
   ```

   This will:
   - Build the WASM app
   - Start the development server
   - Run the tests
   - Generate a report

4. **Run tests in headed mode** (see the browser):
   ```bash
   npm run test:headed
   ```

5. **Debug tests interactively**:
   ```bash
   npm run test:debug
   ```

6. **View the test report**:
   ```bash
   npx playwright show-report
   ```

### GitHub Actions Execution

The tests can be run via GitHub Actions with manual triggering:

1. **Navigate to the Actions tab** in the GitHub repository
2. **Select "Playwright UI Tests"** from the workflows list
3. **Click "Run workflow"** button
4. **Wait for the workflow to complete** (typically 10-15 minutes)
5. **Download artifacts**:
   - `test-screenshots`: All screenshots captured during the test
   - `playwright-report`: Interactive HTML report with test results
   - `test-videos`: Video recordings of failed tests (if any)

The artifacts are retained for 30 days.

## Test Structure

### Configuration (`playwright.config.ts`)

The configuration file defines:
- Test timeout: 5 minutes per test
- Web server: Automatic startup of the WASM development server
- Browsers: Chromium (can be extended to Firefox and WebKit)
- Reporters: HTML report and console list
- Screenshot/video settings: Capture on failure

### Test Suite (`tests/game.spec.ts`)

The main test suite includes:

#### Test 1: Complete Tutorial Level with Bow Towers

This test plays through the entire tutorial level:

1. **Load the game**: Navigate to localhost:8080 and wait for the game to load
2. **Start game**: Click the "Start Game" button on the main menu
3. **Select level**: Choose the tutorial level from the world map
4. **Build towers**: Place 3 bow towers at strategic locations during the initial building phase
5. **Start battle**: Click "Next Turn" to begin the enemy waves
6. **Play turns**: For each turn (up to 30 turns):
   - Select each tower
   - Attack enemies in range
   - Advance to the next turn
7. **Capture screenshots**: Take a screenshot after each major action
8. **Complete level**: Continue until victory or defeat

#### Test 2: Error Detection

This test monitors for JavaScript errors during game load:

1. Listen for console errors and page errors
2. Load the game
3. Wait for initialization
4. Report any errors detected

### Screenshot Organization

Screenshots are saved to `test-screenshots/playwright/` with sequential numbering:

- `001_initial_load.png` - Initial page load
- `002_game_loaded.png` - Game fully loaded
- `003_clicked_start_game.png` - After clicking "Start Game"
- `004_selected_tutorial_level.png` - Tutorial level selected
- `005_tutorial_level_started.png` - Level started
- `006_built_bow_tower_1.png` - First tower built
- `007_built_bow_tower_2.png` - Second tower built
- `008_built_bow_tower_3.png` - Third tower built
- `009_battle_started.png` - Battle phase begins
- `010_turn_1_start.png` - Start of turn 1
- `011_turn_1_after_attacks.png` - After attacks in turn 1
- `...` - Continues for each turn
- `XXX_game_complete.png` - Final screenshot

## Canvas-Based UI Challenges

Since Defender of Egril uses a canvas-based UI (Compose for Web), traditional DOM-based testing approaches don't work. The test suite uses position-based clicking:

### Approach

1. **Get canvas dimensions**: Determine the canvas bounding box
2. **Calculate positions**: Use percentages of canvas width/height to locate UI elements
3. **Click at positions**: Use Playwright's `click({ position: { x, y } })` method

### Position Map

The test uses these approximate positions (as percentages of canvas dimensions):

- **Start Game button**: 50% width, 65% height (center-bottom)
- **Tutorial level**: 25% width, 40% height (left side)
- **Tower buttons**: 15% width, 20% height (upper-left)
- **Build locations**: Various positions in the middle area
- **Next Turn button**: 90% width, 90% height (bottom-right)
- **Enemies**: 60-70% width, 45-50% height (middle-right)

### Limitations

- **No text detection**: Can't search for specific text on canvas
- **Approximate positions**: UI element positions must be estimated
- **No element state**: Can't check if buttons are enabled/disabled directly
- **Visual verification**: Requires manual review of screenshots to confirm correct behavior

## Improving Test Reliability

### Position Tuning

If tests fail to click the correct elements:

1. **Review screenshots**: Check where clicks are landing
2. **Adjust positions**: Update the position percentages in `game.spec.ts`
3. **Add delays**: Increase `waitForTimeout` values if UI isn't ready

### Adding Wait Conditions

For more robust tests, consider:

- Waiting for specific pixel colors to appear (e.g., enemy sprites)
- Using image recognition to detect UI elements
- Adding accessibility attributes to UI elements for easier testing

## Artifacts and Output

### Test Screenshots

- **Location**: `test-screenshots/playwright/`
- **Format**: PNG images
- **Naming**: Sequential numbering with descriptive names
- **Usage**: Visual verification of test execution

### Playwright Report

- **Location**: `playwright-report/`
- **Format**: Interactive HTML report
- **Contents**:
  - Test results (passed/failed)
  - Execution timeline
  - Screenshots and videos
  - Error messages and stack traces
  - Performance metrics

### Test Videos

- **Location**: `test-results/`
- **Format**: WebM video files
- **Capture**: Only on test failure
- **Usage**: Debugging failed tests

## Troubleshooting

### Server Fails to Start

**Symptoms**: Tests timeout waiting for server

**Solutions**:
- Check if port 8080 is already in use
- Verify JDK 24 is installed
- Check Gradle build logs for errors
- Increase server startup timeout in `playwright.config.ts`

### Tests Click Wrong Elements

**Symptoms**: Screenshots show clicks in wrong locations

**Solutions**:
- Review canvas dimensions in test output
- Adjust position percentages in `game.spec.ts`
- Add more delays between actions
- Test in headed mode to see the browser

### Screenshots Are Missing

**Symptoms**: Artifacts don't contain expected screenshots

**Solutions**:
- Check if `test-screenshots/playwright/` directory exists
- Verify file permissions
- Check disk space
- Review test logs for errors

### Tests Pass But Game Didn't Play Correctly

**Symptoms**: All clicks succeed but game state is wrong

**Solutions**:
- Manually review all screenshots
- Compare with expected game behavior
- Adjust wait times between actions
- Add more validation checks

## Future Enhancements

Potential improvements for the test suite:

1. **Image Recognition**: Use ML-based image recognition to detect UI elements
2. **More Levels**: Extend tests to cover all game levels
3. **Different Strategies**: Test various tower placement and upgrade strategies
4. **Browser Matrix**: Run tests on Firefox, Safari, and Edge
5. **Visual Regression**: Compare screenshots against baseline images
6. **Performance Metrics**: Track game performance (FPS, load times)
7. **Accessibility Tests**: Verify keyboard navigation and screen reader support
8. **Mobile Tests**: Test on mobile browsers with touch interactions

## Contributing

When modifying the tests:

1. **Test locally first**: Run tests in headed mode to verify changes
2. **Update positions carefully**: Canvas positions are fragile
3. **Add descriptive screenshots**: Help reviewers understand test flow
4. **Document changes**: Update this guide if test behavior changes
5. **Run full suite**: Ensure all tests still pass

## Resources

- [Playwright Documentation](https://playwright.dev/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlin/Wasm](https://kotlinlang.org/docs/wasm-overview.html)
- [GitHub Actions Artifacts](https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts)

## See Also

- [test-screenshots/README.md](../test-screenshots/README.md) - Screenshot documentation
- [WEB_WASM_GUIDE.md](../docs/guides/WEB_WASM_GUIDE.md) - Web platform guide
- [.github/workflows/playwright-ui-tests.yml](../.github/workflows/playwright-ui-tests.yml) - Workflow configuration
