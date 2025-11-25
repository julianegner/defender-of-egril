# Testing Guide for Tower and Enemy Icons

This guide explains how to manually test the new graphical icons for towers and enemy units.

## Running the Game

### Desktop (Recommended for Testing)
```bash
./gradlew :composeApp:run
```

### Android
```bash
./gradlew :composeApp:installDebug
```
Then launch the app from your device.

### iOS (macOS with Xcode required)
Open `iosApp/iosApp.xcodeproj` in Xcode and click Run.

## What to Test

### 1. Tower Icons - Visual Appearance

Start a game and place different tower types on the build areas (green zones).

**For each tower type, verify:**
- [ ] Spike Tower shows yellow spikes pointing upward
- [ ] Spear Tower shows a brown spear with silver tip
- [ ] Bow Tower shows a curved bow with arrow
- [ ] Wizard Tower shows a gold star symbol
- [ ] Alchemy Tower shows a green potion flask
- [ ] Ballista Tower shows a crossbow with bolt

**All towers should:**
- [ ] Have a white trapezoid base
- [ ] Have small battlement squares on top
- [ ] Display level number (L1, L2, etc.) at bottom in white
- [ ] Show action counter (⚡1) in yellow when ready and has actions
- [ ] Show build timer (⏱2) in orange when being built

### 2. Tower States

**Test different tower states:**
- [ ] **Initial Building Phase**: Place a tower → should show immediately with level L1 and ⚡1
- [ ] **During Game**: Place a tower → should show gray background with ⏱1 or ⏱2
- [ ] **Ready Tower**: Wait for build to complete → background changes to blue, shows ⚡1
- [ ] **After Attack**: Use a tower to attack → action counter disappears, background becomes blue-gray
- [ ] **Upgrade**: Upgrade a tower → level increases (L1→L2→L3, etc.)

### 3. Enemy Icons - Visual Appearance

Start a game and observe enemies as they spawn and move.

**For each enemy type, verify:**
- [ ] Goblin: Small green creature with pointy ears and red eyes
- [ ] Ork: Larger with dark green color, white tusks, yellow eyes
- [ ] Ogre: Very large, brown color, big eyes
- [ ] Skeleton: White skull with black eye sockets and crossbones
- [ ] Evil Wizard: Purple/indigo pointed hat, glowing eyes, staff
- [ ] Witch: Black pointed hat, green face, broom

**All enemies should:**
- [ ] Have red background
- [ ] Display health (e.g., "20/20") at bottom in white
- [ ] Show current health decreasing when damaged (e.g., "15/20", "10/20")

### 4. Game Grid Integration

**Verify the icons work well in the game context:**
- [ ] Icons are clearly visible in 48dp grid cells
- [ ] Different tower types are easily distinguishable
- [ ] Different enemy types are easily distinguishable
- [ ] Level and action indicators don't obscure the main icon
- [ ] Health bars on enemies are readable
- [ ] Selection borders still work (yellow for selected tower)
- [ ] Range indicators still work (green borders on path cells)

### 5. Different Scenarios

**Test various game situations:**

1. **Multiple Towers**
   - [ ] Place multiple different tower types
   - [ ] Verify each type has its unique icon
   - [ ] Check that levels are visible on all towers

2. **Combat**
   - [ ] Select a tower and attack an enemy
   - [ ] Verify action counter decreases
   - [ ] Verify enemy health decreases
   - [ ] Check that tower icon remains clear during combat

3. **Wave Spawning**
   - [ ] Watch enemies spawn
   - [ ] Verify different enemy types are distinguishable
   - [ ] Check that health values are visible for all enemies

4. **End Game**
   - [ ] Complete a level or lose
   - [ ] Ensure icons still render correctly in end-game state

## Known Limitations

The icons are drawn programmatically, not from image files, which means:
- Icons scale perfectly but are simplified designs
- No bitmap-level detail (intentional - keeps them clear at small size)
- Consistent across all platforms (JVM, Android, iOS)

## Screenshot Locations

If you want to capture screenshots for documentation:
1. Desktop: Use your OS screenshot tool
2. Android: Power + Volume Down
3. iOS: Side Button + Volume Up

Save screenshots showing:
- All 6 tower types side by side
- All 6 enemy types side by side
- A tower in each state (building, ready, no actions)
- An enemy at different health levels

## Troubleshooting

**If icons don't appear:**
1. Verify the build was successful
2. Check console for errors
3. Try a clean build: `./gradlew clean build`

**If icons are too small/large:**
- This is expected - icons are designed for 48dp cells
- Zoom may vary by platform/display

**If colors look wrong:**
- Check that towers have blue/gray backgrounds
- Check that enemies have red backgrounds
- Verify build area colors (green shades) are still visible

## Feedback

When providing feedback, please include:
1. Platform tested (Desktop/Android/iOS)
2. Which icons were tested
3. Screenshots if possible
4. Specific issues or suggestions

## Success Criteria

The implementation is successful if:
- ✅ All tower types have distinct, recognizable icons
- ✅ All enemy types have distinct, recognizable icons
- ✅ Level, actions, and health are always visible
- ✅ Icons are clear and distinguishable from each other
- ✅ Game remains playable and enjoyable
- ✅ Performance is not negatively impacted
