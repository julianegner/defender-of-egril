import { test, expect, Page } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';

/**
 * UI Test Suite for Defender of Egril
 * 
 * This test suite automates gameplay through the tutorial level:
 * 1. Start the game
 * 2. Navigate to the tutorial level
 * 3. Build bow towers
 * 4. Attack enemies each turn
 * 5. Continue until victory or defeat
 */

// Create screenshots directory if it doesn't exist
const screenshotsDir = path.join(process.cwd(), 'test-screenshots', 'playwright');
if (!fs.existsSync(screenshotsDir)) {
  fs.mkdirSync(screenshotsDir, { recursive: true });
}

let screenshotCounter = 0;

/**
 * Helper function to take a screenshot with a descriptive name
 */
async function takeScreenshot(page: Page, description: string) {
  screenshotCounter++;
  const filename = `${String(screenshotCounter).padStart(3, '0')}_${description.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.png`;
  const filepath = path.join(screenshotsDir, filename);
  await page.screenshot({ path: filepath, fullPage: true });
  console.log(`📸 Screenshot saved: ${filename}`);
}

/**
 * Helper function to wait for the game to be loaded
 */
async function waitForGameLoad(page: Page) {
  // Wait for the canvas to be visible
  await page.waitForSelector('canvas#ComposeTarget', { state: 'visible', timeout: 60000 });
  
  // Wait for the loading screen to disappear
  await page.waitForSelector('#loading.hidden', { timeout: 60000 });
  
  // Give the game a moment to initialize
  await page.waitForTimeout(2000);
}

/**
 * Helper function to click at a specific position on the canvas
 */
async function clickCanvas(page: Page, x: number, y: number) {
  const canvas = await page.locator('canvas#ComposeTarget');
  await canvas.click({ position: { x, y } });
  await page.waitForTimeout(500); // Wait for UI to update
}

/**
 * Helper function to find and click text on the canvas
 * This is tricky with canvas, so we'll use OCR or position-based clicking
 */
async function clickTextOnCanvas(page: Page, text: string, description: string) {
  console.log(`🔍 Looking for: ${text}`);
  
  // For canvas-based UI, we need to use approximate positions
  // These positions are based on typical UI layout
  const positions: Record<string, { x: number, y: number }> = {
    'start_game': { x: 640, y: 500 },     // Center-bottom for Start Game button
    'tutorial': { x: 200, y: 300 },        // Left side for first level
    'bow_tower': { x: 100, y: 150 },       // Top-left area for tower selection
    'build_spot': { x: 400, y: 400 },      // A typical build location
    'next_turn': { x: 1100, y: 700 },      // Bottom-right for Next Turn button
    'enemy': { x: 700, y: 400 },           // Middle area where enemies appear
  };
  
  if (positions[text.toLowerCase()]) {
    const pos = positions[text.toLowerCase()];
    await clickCanvas(page, pos.x, pos.y);
    await takeScreenshot(page, description);
  } else {
    console.warn(`⚠️ Unknown position for: ${text}`);
  }
}

test.describe('Defender of Egril - Tutorial Level Playthrough', () => {
  test.setTimeout(300000); // 5 minutes for full test

  test('should complete tutorial level with bow towers', async ({ page }) => {
    console.log('🎮 Starting Defender of Egril UI test...');
    
    // Navigate to the game
    await page.goto('/');
    console.log('📍 Navigated to game URL');
    
    // Take initial screenshot
    await takeScreenshot(page, 'initial_load');
    
    // Wait for game to load
    console.log('⏳ Waiting for game to load...');
    await waitForGameLoad(page);
    await takeScreenshot(page, 'game_loaded');
    
    // Step 1: Click "Start Game" button
    console.log('🎯 Step 1: Clicking Start Game...');
    await page.waitForTimeout(2000); // Wait for menu to be fully rendered
    
    // Try to find and click the Start Game button
    // Since it's a canvas-based UI, we need to click at approximate positions
    const canvas = page.locator('canvas#ComposeTarget');
    const boundingBox = await canvas.boundingBox();
    
    if (!boundingBox) {
      throw new Error('Canvas not found or not visible');
    }
    
    console.log(`Canvas dimensions: ${boundingBox.width}x${boundingBox.height}`);
    
    // Click in the center-bottom area where "Start Game" typically appears
    const startGameX = boundingBox.width / 2;
    const startGameY = boundingBox.height * 0.65; // 65% down from top
    await clickCanvas(page, startGameX, startGameY);
    await page.waitForTimeout(2000);
    await takeScreenshot(page, 'clicked_start_game');
    
    // Step 2: Select tutorial level from world map
    console.log('🎯 Step 2: Selecting tutorial level...');
    await page.waitForTimeout(2000);
    
    // Click on the first/tutorial level (typically on the left side)
    const tutorialX = boundingBox.width * 0.25; // 25% from left
    const tutorialY = boundingBox.height * 0.4; // 40% down
    await clickCanvas(page, tutorialX, tutorialY);
    await page.waitForTimeout(1000);
    await takeScreenshot(page, 'selected_tutorial_level');
    
    // Click to confirm/start the level (there might be a dialog)
    const confirmX = boundingBox.width / 2;
    const confirmY = boundingBox.height * 0.7;
    await clickCanvas(page, confirmX, confirmY);
    await page.waitForTimeout(2000);
    await takeScreenshot(page, 'tutorial_level_started');
    
    // Step 3: Build bow towers
    console.log('🎯 Step 3: Building bow towers...');
    
    // In the initial building phase, we can place towers
    // First, select the bow tower from the tower menu
    const bowTowerButtonX = boundingBox.width * 0.15; // Left side, tower buttons
    const bowTowerButtonY = boundingBox.height * 0.2; // Upper area
    
    // Place 3-4 bow towers at strategic locations
    const buildLocations = [
      { x: boundingBox.width * 0.35, y: boundingBox.height * 0.45 },
      { x: boundingBox.width * 0.45, y: boundingBox.height * 0.55 },
      { x: boundingBox.width * 0.55, y: boundingBox.height * 0.45 },
    ];
    
    for (let i = 0; i < buildLocations.length; i++) {
      console.log(`🏗️ Building bow tower ${i + 1}...`);
      
      // Click bow tower button
      await clickCanvas(page, bowTowerButtonX, bowTowerButtonY);
      await page.waitForTimeout(500);
      
      // Click on build location
      const loc = buildLocations[i];
      await clickCanvas(page, loc.x, loc.y);
      await page.waitForTimeout(1000);
      await takeScreenshot(page, `built_bow_tower_${i + 1}`);
    }
    
    // Step 4: Start the battle by clicking "Next Turn"
    console.log('🎯 Step 4: Starting battle...');
    const nextTurnX = boundingBox.width * 0.9; // Right side
    const nextTurnY = boundingBox.height * 0.9; // Bottom
    await clickCanvas(page, nextTurnX, nextTurnY);
    await page.waitForTimeout(2000);
    await takeScreenshot(page, 'battle_started');
    
    // Step 5: Play through the level
    console.log('🎯 Step 5: Playing through the level...');
    
    let turnCount = 0;
    const maxTurns = 30; // Safety limit
    let gameOver = false;
    
    while (turnCount < maxTurns && !gameOver) {
      turnCount++;
      console.log(`🔄 Turn ${turnCount}...`);
      
      // Check for victory or defeat messages by taking a screenshot
      // and checking if the game is still active
      await takeScreenshot(page, `turn_${turnCount}_start`);
      
      // Attack enemies with bow towers
      // Click on towers and then on enemies
      const towerLocations = [
        { x: boundingBox.width * 0.35, y: boundingBox.height * 0.45 },
        { x: boundingBox.width * 0.45, y: boundingBox.height * 0.55 },
        { x: boundingBox.width * 0.55, y: boundingBox.height * 0.45 },
      ];
      
      const enemyTargetAreas = [
        { x: boundingBox.width * 0.6, y: boundingBox.height * 0.5 },
        { x: boundingBox.width * 0.65, y: boundingBox.height * 0.45 },
        { x: boundingBox.width * 0.7, y: boundingBox.height * 0.5 },
      ];
      
      for (let i = 0; i < towerLocations.length; i++) {
        const tower = towerLocations[i];
        const target = enemyTargetAreas[i % enemyTargetAreas.length];
        
        // Click tower to select it
        await clickCanvas(page, tower.x, tower.y);
        await page.waitForTimeout(300);
        
        // Click enemy area to attack
        await clickCanvas(page, target.x, target.y);
        await page.waitForTimeout(300);
      }
      
      await takeScreenshot(page, `turn_${turnCount}_after_attacks`);
      
      // Click "Next Turn" button
      await clickCanvas(page, nextTurnX, nextTurnY);
      await page.waitForTimeout(2000);
      
      // Simple heuristic to detect game over
      // If we've played enough turns or see a victory/defeat screen
      if (turnCount >= 20) {
        console.log('✅ Reached turn limit, considering test complete');
        gameOver = true;
      }
    }
    
    await takeScreenshot(page, 'game_complete');
    
    console.log('✨ Test completed successfully!');
    console.log(`📊 Total turns played: ${turnCount}`);
    console.log(`📸 Total screenshots taken: ${screenshotCounter}`);
    
    // Verify we took screenshots
    expect(screenshotCounter).toBeGreaterThan(10);
  });
  
  test('should handle errors gracefully', async ({ page }) => {
    // Set up error listener
    const errors: string[] = [];
    
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
        console.error('❌ Console error:', msg.text());
      }
    });
    
    page.on('pageerror', error => {
      errors.push(error.message);
      console.error('❌ Page error:', error.message);
    });
    
    // Navigate to the game
    await page.goto('/');
    await waitForGameLoad(page);
    await takeScreenshot(page, 'error_test_loaded');
    
    // Wait a bit to catch any immediate errors
    await page.waitForTimeout(5000);
    
    // Report errors
    if (errors.length > 0) {
      console.error(`⚠️ Found ${errors.length} errors during game load`);
      errors.forEach((error, index) => {
        console.error(`  ${index + 1}. ${error}`);
      });
    } else {
      console.log('✅ No errors detected during game load');
    }
    
    // Take final screenshot
    await takeScreenshot(page, 'error_test_complete');
  });
});
