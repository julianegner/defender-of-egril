import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for Defender of Egril UI tests
 */
export default defineConfig({
  testDir: '../tests',
  
  // Maximum time one test can run for
  timeout: 300 * 1000, // 5 minutes for the full game playthrough
  
  expect: {
    // Maximum time to wait for expect() assertions
    timeout: 30000,
  },
  
  // Run tests in files in parallel
  fullyParallel: false, // Run sequentially since we're testing the same game instance
  
  // Fail the build on CI if you accidentally left test.only in the source code
  forbidOnly: !!process.env.CI,
  
  // Retry on CI only
  retries: process.env.CI ? 2 : 0,
  
  // Opt out of parallel tests on CI
  workers: 1,
  
  // Reporter to use
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list']
  ],
  
  // Shared settings for all the projects below
  use: {
    // Base URL to use in actions like `await page.goto('/')`
    baseURL: 'http://localhost:8080',
    
    // Collect trace when retrying the failed test
    trace: 'on-first-retry',
    
    // Take screenshot on failure
    screenshot: 'only-on-failure',
    
    // Record video on failure
    video: 'retain-on-failure',
  },

  // Configure projects for major browsers
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // Run your local dev server before starting the tests
  webServer: {
    command: './gradlew :composeApp:wasmJsBrowserDevelopmentRun',
    url: 'http://localhost:8080',
    timeout: 180 * 1000, // 3 minutes to start the server
    reuseExistingServer: !process.env.CI,
    stdout: 'pipe',
    stderr: 'pipe',
  },
});
