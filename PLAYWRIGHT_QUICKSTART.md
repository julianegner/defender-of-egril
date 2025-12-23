# Quick Start: Running Playwright UI Tests

## Via GitHub Actions (Recommended)

The easiest way to run the Playwright UI tests is through GitHub Actions:

### Steps:

1. **Navigate to Actions Tab**
   - Go to the GitHub repository: https://github.com/qvest-digital/defender-of-egril-fork
   - Click on the "Actions" tab at the top

2. **Select Workflow**
   - In the left sidebar, find and click "Playwright UI Tests"

3. **Run Workflow**
   - Click the "Run workflow" dropdown button (top right)
   - Select the branch (usually `main` or your feature branch)
   - Click the green "Run workflow" button

4. **Wait for Completion**
   - The workflow takes approximately 10-15 minutes to complete
   - You'll see real-time progress as each step executes
   - A green checkmark indicates success, red X indicates failure

5. **Download Results**
   - Scroll down to the "Artifacts" section at the bottom of the workflow run page
   - Download any or all of these artifacts:
     - **test-screenshots**: All screenshots captured during gameplay (sequential PNG files)
     - **playwright-report**: Interactive HTML report with test results
     - **test-videos**: Video recordings of any failed tests
   - Artifacts are retained for 30 days

### What the Workflow Does:

1. ✅ Checks out the code
2. ⚙️ Sets up JDK 24 and Gradle
3. 🛠️ Builds the WASM version of the game
4. 📦 Installs Node.js and Playwright
5. 🎭 Installs Chromium browser
6. 🌐 Starts the development server
7. 🎮 Runs the automated gameplay tests
8. 📸 Captures screenshots at each step
9. 📤 Uploads all results as artifacts

## Running Locally

If you want to run the tests on your own machine:

### Prerequisites:
```bash
# Check you have Node.js installed
node --version  # Should be v20 or later

# Check you have JDK installed  
java --version  # Should be JDK 11 or later
```

### Steps:

1. **Install Dependencies**
   ```bash
   npm install
   ```

2. **Install Playwright Browsers**
   ```bash
   npx playwright install chromium
   ```

3. **Run Tests**
   ```bash
   npx playwright test
   ```
   
   Or run in headed mode (see the browser):
   ```bash
   npm run test:headed
   ```

4. **View Results**
   - Screenshots: `test-screenshots/playwright/`
   - HTML Report: Run `npx playwright show-report`

### Troubleshooting:

**Port 8080 already in use:**
```bash
# Find and kill the process using port 8080
lsof -ti:8080 | xargs kill -9  # macOS/Linux
netstat -ano | findstr :8080   # Windows (then kill the PID)
```

**Tests fail immediately:**
- Check the Gradle build completes successfully: `./gradlew :composeApp:wasmJsBrowserDistribution`
- Ensure port 8080 is available
- Try increasing timeouts in `playwright.config.ts`

## Understanding Test Results

### Screenshots
Sequential screenshots show the test progression:
- `001_initial_load.png` - Game starting to load
- `002_game_loaded.png` - Main menu visible
- `003_clicked_start_game.png` - After "Start Game" clicked
- `00X_built_bow_tower_N.png` - Each tower placement
- `0XX_turn_N_start.png` - Start of each turn
- `0XX_turn_N_after_attacks.png` - After attacks executed

### Playwright Report
The HTML report includes:
- Test execution timeline
- Pass/fail status for each test
- Screenshots embedded at each step
- Console logs and errors
- Performance metrics

### Videos
Only created when tests fail, showing:
- Full browser recording from test start to failure
- Helps debug what went wrong visually

## Modifying the Tests

To change test behavior, edit `tests/game.spec.ts`:

- **Adjust click positions**: Modify the coordinate calculations
- **Change tower count**: Update `buildLocations` array
- **Add more turns**: Increase `maxTurns` constant
- **Add wait times**: Increase `page.waitForTimeout()` values

After changes, test locally first before pushing!

## See Also

- [Full Playwright UI Tests Guide](../docs/testing/PLAYWRIGHT_UI_TESTS.md) - Comprehensive documentation
- [Web/WASM Guide](../docs/guides/WEB_WASM_GUIDE.md) - Web platform details
- [test-screenshots/README.md](test-screenshots/README.md) - Screenshot documentation
