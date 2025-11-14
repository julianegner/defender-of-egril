# Testing Guide for Keyboard Navigation Feature

## Manual Testing Checklist

### 1. Gameplay Screen - Keyboard Navigation

#### Basic Arrow Key Navigation
- [ ] Start a game level
- [ ] Press **Up Arrow** - Map should pan upward
- [ ] Press **Down Arrow** - Map should pan downward
- [ ] Press **Left Arrow** - Map should pan leftward
- [ ] Press **Right Arrow** - Map should pan rightward
- [ ] Verify smooth panning in all directions
- [ ] Verify pan stops at map boundaries (can't pan off the map)

#### WASD Key Navigation
- [ ] Press **W** key - Map should pan upward
- [ ] Press **S** key - Map should pan downward
- [ ] Press **A** key - Map should pan leftward
- [ ] Press **D** key - Map should pan rightward
- [ ] Verify WASD behaves identically to arrow keys

#### Combined Controls
- [ ] Pan with arrow keys, then use mouse wheel to zoom
- [ ] Zoom in with mouse wheel, then pan with arrow keys
- [ ] Pan with arrow keys, then drag with mouse
- [ ] Verify all control methods work together smoothly

#### Constraints
- [ ] Zoom in to 2x or more
- [ ] Try to pan beyond the map edges with arrow keys
- [ ] Verify panning is constrained (can't go too far off-screen)
- [ ] Zoom out to 0.5x
- [ ] Try panning with arrow keys
- [ ] Verify panning still works at minimum zoom

### 2. Map Editor Screen

#### Verify Keyboard Navigation is Disabled
- [ ] Open the map editor
- [ ] Create or edit a map
- [ ] Press arrow keys (↑ ↓ ← →)
- [ ] **Verify**: Arrow keys do NOT pan the map
- [ ] Press WASD keys
- [ ] **Verify**: WASD keys do NOT pan the map

#### Zoom Buttons
- [ ] Click the **zoom in (+)** button in the header
- [ ] Verify the map zooms in and zoom percentage increases
- [ ] Click multiple times
- [ ] Verify zoom reaches maximum (300%)
- [ ] Click the **zoom out (-)** button in the header
- [ ] Verify the map zooms out and zoom percentage decreases
- [ ] Click multiple times
- [ ] Verify zoom reaches minimum (50%)

#### Mouse Wheel Zoom
- [ ] Hover over the map
- [ ] Scroll mouse wheel up
- [ ] Verify map zooms in
- [ ] Scroll mouse wheel down
- [ ] Verify map zooms out
- [ ] Verify zoom percentage updates in header

#### Mouse Drag Pan
- [ ] Click and drag on the map (not on a tile)
- [ ] Verify map pans with the drag
- [ ] Release mouse
- [ ] Verify panning stops

#### Brush Painting Feature
- [ ] Select a tile type from the header (e.g., PATH)
- [ ] Click on a tile
- [ ] **Verify**: Tile changes to selected type
- [ ] Click and drag across multiple tiles
- [ ] **Verify**: All tiles under the drag path change to selected type
- [ ] Select a different tile type (e.g., BUILD_AREA)
- [ ] Drag across existing tiles
- [ ] **Verify**: Tiles change to new type as you drag
- [ ] Zoom in to 200%
- [ ] Try brush painting
- [ ] **Verify**: Brush painting still works at different zoom levels

#### Combined Editor Controls
- [ ] Use zoom buttons to zoom in
- [ ] Drag to pan the map
- [ ] Use brush to paint tiles
- [ ] Zoom out with zoom buttons
- [ ] Paint more tiles
- [ ] Use mouse wheel to zoom
- [ ] Paint tiles at different zoom levels
- [ ] **Verify**: All controls work together without interference

### 3. Focus and State Management

#### Gameplay Focus
- [ ] Start a game
- [ ] Click on the map area
- [ ] Press arrow keys
- [ ] **Verify**: Map pans (focus is on the map)
- [ ] Click on a UI button (e.g., tower selection)
- [ ] Press arrow keys
- [ ] **Verify**: Map still pans (focus management works)

#### Map Editor Focus
- [ ] Open map editor
- [ ] Click in the map name text field
- [ ] Press arrow keys
- [ ] **Verify**: Cursor moves in text field, map does NOT pan
- [ ] Click outside text field (on map)
- [ ] Press arrow keys
- [ ] **Verify**: Map does NOT pan (keyboard nav disabled)

### 4. Mobile/Touch Devices (if applicable)

#### Gameplay
- [ ] On a mobile device, start a game
- [ ] Use pinch gesture to zoom
- [ ] **Verify**: Zoom works
- [ ] Drag to pan
- [ ] **Verify**: Pan works
- [ ] **Note**: Keyboard navigation not applicable on mobile

#### Map Editor
- [ ] On a mobile device, open map editor
- [ ] Tap zoom buttons
- [ ] **Verify**: Zoom works
- [ ] Use pinch gesture to zoom
- [ ] **Verify**: Zoom works
- [ ] Drag to pan
- [ ] **Verify**: Pan works
- [ ] Tap and drag to paint tiles
- [ ] **Verify**: Brush painting works

### 5. Cross-Browser Testing (for Web/WASM builds)

Test on:
- [ ] Chrome/Chromium
- [ ] Firefox
- [ ] Safari
- [ ] Edge

For each browser:
- [ ] Verify keyboard navigation works in gameplay
- [ ] Verify mouse wheel zoom works
- [ ] Verify zoom buttons work in editor
- [ ] Verify brush painting works in editor

### 6. Performance Testing

#### Large Maps
- [ ] Create or open a large map (e.g., 50x50)
- [ ] Pan with arrow keys rapidly
- [ ] **Verify**: No lag or stuttering
- [ ] Zoom in and out repeatedly
- [ ] **Verify**: Smooth zooming
- [ ] Paint tiles with brush while zoomed
- [ ] **Verify**: Responsive painting

#### Small Maps
- [ ] Open a small map (e.g., 10x10)
- [ ] Test all controls
- [ ] **Verify**: Everything works smoothly

## Automated Testing

### Unit Tests
Run existing unit tests to ensure no regression:
```bash
./gradlew :composeApp:cleanTestDebugUnitTest :composeApp:testDebugUnitTest
```

**Expected Result**: All tests pass

### Compilation Tests
Compile for all platforms:
```bash
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:wasmJsBrowserDevelopmentWebpack
```

**Expected Result**: All compilations succeed

## Regression Testing

Test that existing features still work:
- [ ] Tower placement in gameplay
- [ ] Enemy spawning and movement
- [ ] Tower attacks
- [ ] Game win/lose conditions
- [ ] Level editor map saving
- [ ] Level editor level creation
- [ ] Save/load game functionality

## Known Limitations

1. **Map Editor Keyboard Navigation**: Intentionally disabled to prevent interference with text input and brush painting
2. **Mobile Devices**: Keyboard navigation only applies to devices with physical keyboards
3. **Focus Management**: On web builds, clicking outside the game area may require clicking back in to regain keyboard focus

## Bug Reporting

If you find issues during testing, please report:
- Platform (Desktop/Android/iOS/Web)
- Operating System and version
- Browser (if web)
- Steps to reproduce
- Expected behavior
- Actual behavior
- Screenshots/videos if applicable
