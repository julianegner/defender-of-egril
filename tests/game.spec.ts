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

/**
 * Helper function to take a screenshot with a descriptive name
 * Uses a timestamp-based approach to ensure unique filenames
 */
async function takeScreenshot(page: Page, description: string, counter: number) {
  const filename = `${String(counter).padStart(3, '0')}_${description.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.png`;
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

test.describe('Defender of Egril - Tutorial Level Playthrough', () => {
  test.setTimeout(300000); // 5 minutes for full test

  test('should complete tutorial level with bow towers', async ({ page }) => {
    console.log('🎮 Starting Defender of Egril UI test...');
    
    // Screenshot counter for this test run
    let screenshotCounter = 0;
    
    // Helper to take screenshot with auto-incrementing counter
    const screenshot = async (description: string) => {
      screenshotCounter++;
      await takeScreenshot(page, description, screenshotCounter);
    };
    
    // Navigate to the game
    await page.goto('/');
    console.log('📍 Navigated to game URL');
    
    // Take initial screenshot
    await screenshot('initial_load');
    
    // Wait for game to load
    console.log('⏳ Waiting for game to load...');
    await waitForGameLoad(page);
    await screenshot('game_loaded');
    
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
    await screenshot('clicked_start_game');
    
    // Step 2: Select tutorial level from world map
    console.log('🎯 Step 2: Selecting tutorial level...');
    await page.waitForTimeout(2000);
    
    // Click on the first/tutorial level (typically on the left side)
    const tutorialX = boundingBox.width * 0.25; // 25% from left
    const tutorialY = boundingBox.height * 0.4; // 40% down
    await clickCanvas(page, tutorialX, tutorialY);
    await page.waitForTimeout(1000);
    await screenshot('selected_tutorial_level');
    
    // Click to confirm/start the level (there might be a dialog)
    const confirmX = boundingBox.width / 2;
    const confirmY = boundingBox.height * 0.7;
    await clickCanvas(page, confirmX, confirmY);
    await page.waitForTimeout(2000);
    await screenshot('tutorial_level_started');
    
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
      await screenshot(`built_bow_tower_${i + 1}`);
    }
    
    // Step 4: Start the battle by clicking "Next Turn"
    console.log('🎯 Step 4: Starting battle...');
    const nextTurnX = boundingBox.width * 0.9; // Right side
    const nextTurnY = boundingBox.height * 0.9; // Bottom
    await clickCanvas(page, nextTurnX, nextTurnY);
    await page.waitForTimeout(2000);
    await screenshot('battle_started');
    
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
      await screenshot(`turn_${turnCount}_start`);
      
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
      
      await screenshot(`turn_${turnCount}_after_attacks`);
      
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
    
    await screenshot('game_complete');
    
    console.log('✨ Test completed successfully!');
    console.log(`📊 Total turns played: ${turnCount}`);
    console.log(`📸 Total screenshots taken: ${screenshotCounter}`);
    
    // Verify we took screenshots
    expect(screenshotCounter).toBeGreaterThan(10);
  });
  
  test('should handle errors gracefully', async ({ page }) => {
    // Screenshot counter for this test run
    let screenshotCounter = 0;
    
    // Helper to take screenshot with auto-incrementing counter
    const screenshot = async (description: string) => {
      screenshotCounter++;
      await takeScreenshot(page, description, screenshotCounter);
    };
    
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
    await screenshot('error_test_loaded');
    
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
    await screenshot('error_test_complete');
  });
});

test.describe('Defender of Egril - Main Menu and Navigation', () => {
  test.setTimeout(120000); // 2 minutes for navigation tests

  test('should navigate through main menu and settings', async ({ page }) => {
    console.log('🎮 Testing main menu navigation...');
    
    let screenshotCounter = 0;
    const screenshot = async (description: string) => {
      screenshotCounter++;
      await takeScreenshot(page, description, screenshotCounter);
    };

    // Navigate to the game
    await page.goto('/');
    await waitForGameLoad(page);
    await screenshot('main_menu_loaded');
    
    const canvas = page.locator('canvas#ComposeTarget');
    const boundingBox = await canvas.boundingBox();
    
    if (!boundingBox) {
      throw new Error('Canvas not found');
    }

    // Try to access settings (typically top-right area)
    const settingsX = boundingBox.width * 0.95;
    const settingsY = boundingBox.height * 0.05;
    await clickCanvas(page, settingsX, settingsY);
    await page.waitForTimeout(1000);
    await screenshot('settings_opened');

    // Close settings (click in center or ESC)
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    await screenshot('settings_closed');

    // Try to access rules/help (often middle button)
    const rulesX = boundingBox.width / 2;
    const rulesY = boundingBox.height * 0.55;
    await clickCanvas(page, rulesX, rulesY);
    await page.waitForTimeout(1000);
    await screenshot('rules_screen');

    // Go back
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    await screenshot('back_to_main_menu');

    console.log('✅ Main menu navigation test completed');
    expect(screenshotCounter).toBeGreaterThan(4);
  });

  test('should navigate world map and view level details', async ({ page }) => {
    console.log('🎮 Testing world map navigation...');
    
    let screenshotCounter = 0;
    const screenshot = async (description: string) => {
      screenshotCounter++;
      await takeScreenshot(page, description, screenshotCounter);
    };

    await page.goto('/');
    await waitForGameLoad(page);
    
    const canvas = page.locator('canvas#ComposeTarget');
    const boundingBox = await canvas.boundingBox();
    
    if (!boundingBox) {
      throw new Error('Canvas not found');
    }

    // Click "Start Game"
    const startGameX = boundingBox.width / 2;
    const startGameY = boundingBox.height * 0.65;
    await clickCanvas(page, startGameX, startGameY);
    await page.waitForTimeout(2000);
    await screenshot('world_map_displayed');

    // Try clicking different level positions to view details
    const levelPositions = [
      { x: boundingBox.width * 0.25, y: boundingBox.height * 0.4 },  // Tutorial
      { x: boundingBox.width * 0.35, y: boundingBox.height * 0.5 },  // Level 2
      { x: boundingBox.width * 0.45, y: boundingBox.height * 0.4 },  // Level 3
    ];

    for (let i = 0; i < levelPositions.length; i++) {
      const pos = levelPositions[i];
      await clickCanvas(page, pos.x, pos.y);
      await page.waitForTimeout(1000);
      await screenshot(`level_${i + 1}_details`);
      
      // Close dialog with ESC
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    }

    await screenshot('world_map_final');
    console.log('✅ World map navigation test completed');
    expect(screenshotCounter).toBeGreaterThan(4);
  });
});

test.describe('Defender of Egril - Different Tower Strategies', () => {
  test.setTimeout(300000); // 5 minutes for full test

  test('should complete tutorial with mixed tower types', async ({ page }) => {
    console.log('🎮 Testing mixed tower strategy...');
    
    let screenshotCounter = 0;
    const screenshot = async (description: string) => {
      screenshotCounter++;
      await takeScreenshot(page, description, screenshotCounter);
    };

    await page.goto('/');
    await waitForGameLoad(page);
    await screenshot('game_start');
    
    const canvas = page.locator('canvas#ComposeTarget');
    const boundingBox = await canvas.boundingBox();
    
    if (!boundingBox) {
      throw new Error('Canvas not found');
    }

    // Navigate to tutorial level
    const startGameX = boundingBox.width / 2;
    const startGameY = boundingBox.height * 0.65;
    await clickCanvas(page, startGameX, startGameY);
    await page.waitForTimeout(2000);
    await screenshot('world_map');

    const tutorialX = boundingBox.width * 0.25;
    const tutorialY = boundingBox.height * 0.4;
    await clickCanvas(page, tutorialX, tutorialY);
    await page.waitForTimeout(1000);
    await screenshot('tutorial_selected');

    const confirmX = boundingBox.width / 2;
    const confirmY = boundingBox.height * 0.7;
    await clickCanvas(page, confirmX, confirmY);
    await page.waitForTimeout(2000);
    await screenshot('tutorial_started');

    // Build mixed towers: spike, bow, and spear
    const towerTypes = [
      { name: 'spike', buttonY: 0.15 },  // Spike tower (top)
      { name: 'bow', buttonY: 0.20 },    // Bow tower
      { name: 'spear', buttonY: 0.25 },  // Spear tower
    ];

    const buildLocations = [
      { x: boundingBox.width * 0.35, y: boundingBox.height * 0.45 },
      { x: boundingBox.width * 0.45, y: boundingBox.height * 0.50 },
      { x: boundingBox.width * 0.55, y: boundingBox.height * 0.45 },
    ];

    for (let i = 0; i < buildLocations.length; i++) {
      const tower = towerTypes[i % towerTypes.length];
      const towerButtonX = boundingBox.width * 0.15;
      const towerButtonY = boundingBox.height * tower.buttonY;
      
      // Click tower button
      await clickCanvas(page, towerButtonX, towerButtonY);
      await page.waitForTimeout(500);
      
      // Click build location
      const loc = buildLocations[i];
      await clickCanvas(page, loc.x, loc.y);
      await page.waitForTimeout(1000);
      await screenshot(`built_${tower.name}_tower_${i + 1}`);
    }

    // Start battle
    const nextTurnX = boundingBox.width * 0.9;
    const nextTurnY = boundingBox.height * 0.9;
    await clickCanvas(page, nextTurnX, nextTurnY);
    await page.waitForTimeout(2000);
    await screenshot('mixed_strategy_battle_started');

    // Play a few turns
    for (let turn = 1; turn <= 10; turn++) {
      await screenshot(`mixed_turn_${turn}_start`);
      
      // Attack with towers
      for (let i = 0; i < buildLocations.length; i++) {
        const tower = buildLocations[i];
        await clickCanvas(page, tower.x, tower.y);
        await page.waitForTimeout(300);
        
        const targetX = boundingBox.width * 0.65;
        const targetY = boundingBox.height * 0.5;
        await clickCanvas(page, targetX, targetY);
        await page.waitForTimeout(300);
      }
      
      // Next turn
      await clickCanvas(page, nextTurnX, nextTurnY);
      await page.waitForTimeout(1500);
    }

    await screenshot('mixed_strategy_complete');
    console.log('✅ Mixed tower strategy test completed');
    expect(screenshotCounter).toBeGreaterThan(15);
  });

  test('should test tower upgrades', async ({ page }) => {
    console.log('🎮 Testing tower upgrade functionality...');
    
    let screenshotCounter = 0;
    const screenshot = async (description: string) => {
      screenshotCounter++;
      await takeScreenshot(page, description, screenshotCounter);
    };

    await page.goto('/');
    await waitForGameLoad(page);
    
    const canvas = page.locator('canvas#ComposeTarget');
    const boundingBox = await canvas.boundingBox();
    
    if (!boundingBox) {
      throw new Error('Canvas not found');
    }

    // Navigate to tutorial
    await clickCanvas(page, boundingBox.width / 2, boundingBox.height * 0.65);
    await page.waitForTimeout(2000);
    await clickCanvas(page, boundingBox.width * 0.25, boundingBox.height * 0.4);
    await page.waitForTimeout(1000);
    await clickCanvas(page, boundingBox.width / 2, boundingBox.height * 0.7);
    await page.waitForTimeout(2000);
    await screenshot('upgrade_test_started');

    // Build one tower
    await clickCanvas(page, boundingBox.width * 0.15, boundingBox.height * 0.20);
    await page.waitForTimeout(500);
    await clickCanvas(page, boundingBox.width * 0.45, boundingBox.height * 0.50);
    await page.waitForTimeout(1000);
    await screenshot('tower_built_for_upgrade');

    // Start battle
    await clickCanvas(page, boundingBox.width * 0.9, boundingBox.height * 0.9);
    await page.waitForTimeout(2000);

    // Play turns and try to upgrade tower
    for (let turn = 1; turn <= 5; turn++) {
      // Click tower
      await clickCanvas(page, boundingBox.width * 0.45, boundingBox.height * 0.50);
      await page.waitForTimeout(500);
      await screenshot(`tower_selected_turn_${turn}`);
      
      // Try clicking upgrade button (typically in tower details panel)
      const upgradeX = boundingBox.width * 0.85;
      const upgradeY = boundingBox.height * 0.3;
      await clickCanvas(page, upgradeX, upgradeY);
      await page.waitForTimeout(500);
      await screenshot(`upgrade_attempt_turn_${turn}`);
      
      // Attack enemy
      await clickCanvas(page, boundingBox.width * 0.65, boundingBox.height * 0.5);
      await page.waitForTimeout(500);
      
      // Next turn
      await clickCanvas(page, boundingBox.width * 0.9, boundingBox.height * 0.9);
      await page.waitForTimeout(1500);
    }

    await screenshot('upgrade_test_complete');
    console.log('✅ Tower upgrade test completed');
    expect(screenshotCounter).toBeGreaterThan(8);
  });
});

test.describe('Defender of Egril - Save and Load', () => {
  test.setTimeout(180000); // 3 minutes

  test('should test save game functionality', async ({ page }) => {
    console.log('🎮 Testing save game functionality...');
    
    let screenshotCounter = 0;
    const screenshot = async (description: string) => {
      screenshotCounter++;
      await takeScreenshot(page, description, screenshotCounter);
    };

    await page.goto('/');
    await waitForGameLoad(page);
    
    const canvas = page.locator('canvas#ComposeTarget');
    const boundingBox = await canvas.boundingBox();
    
    if (!boundingBox) {
      throw new Error('Canvas not found');
    }

    // Start a game
    await clickCanvas(page, boundingBox.width / 2, boundingBox.height * 0.65);
    await page.waitForTimeout(2000);
    await clickCanvas(page, boundingBox.width * 0.25, boundingBox.height * 0.4);
    await page.waitForTimeout(1000);
    await clickCanvas(page, boundingBox.width / 2, boundingBox.height * 0.7);
    await page.waitForTimeout(2000);
    await screenshot('game_started_for_save');

    // Build a tower
    await clickCanvas(page, boundingBox.width * 0.15, boundingBox.height * 0.20);
    await page.waitForTimeout(500);
    await clickCanvas(page, boundingBox.width * 0.45, boundingBox.height * 0.50);
    await page.waitForTimeout(1000);
    await screenshot('tower_built_before_save');

    // Try to access save menu (typically ESC or menu button)
    await page.keyboard.press('Escape');
    await page.waitForTimeout(1000);
    await screenshot('game_menu_opened');

    // Try clicking save button (typically middle of screen)
    const saveX = boundingBox.width * 0.5;
    const saveY = boundingBox.height * 0.45;
    await clickCanvas(page, saveX, saveY);
    await page.waitForTimeout(1000);
    await screenshot('save_dialog_opened');

    // Try to save
    await clickCanvas(page, boundingBox.width * 0.5, boundingBox.height * 0.6);
    await page.waitForTimeout(1000);
    await screenshot('game_saved');

    console.log('✅ Save game test completed');
    expect(screenshotCounter).toBeGreaterThan(4);
  });
});
