# Web/WASM Repository Sync Testing

## Issue
On web/WASM, the "Add New Content" dialog kept showing after accepting new repository content, even though the content was successfully synced. This was caused by `fileExists()` not recognizing virtual directories in localStorage.

## Fix
Updated `WasmJsFileStorage.fileExists()` to check both:
1. If path exists as a file (localStorage key exists)
2. If path exists as a virtual directory (any localStorage keys start with path + "/")

## Manual Test Scenarios

### Test 1: Initial State - No Gamedata
**Setup**: Clear browser localStorage completely
1. Navigate to World Map screen
2. **Expected**: "Add New Content" dialog should show all repository levels as new
3. Click "Add New Content" button
4. **Expected**: Dialog closes, levels are synced to localStorage
5. Verify localStorage contains keys like:
   - `defender-of-egril:gamedata/maps/...`
   - `defender-of-egril:gamedata/levels/...`
   - `defender-of-egril:gamedata/sequence.json`

### Test 2: After Sync - Gamedata Exists
**Setup**: Complete Test 1 first
1. Navigate away from World Map (e.g., go to Main Menu)
2. Navigate back to World Map screen
3. **Expected**: "Add New Content" dialog should NOT show
4. **Verification**: `fileExists("gamedata")` returns true because localStorage has keys starting with "defender-of-egril:gamedata/"

### Test 3: Partial Gamedata
**Setup**: 
1. Clear localStorage
2. Manually add one map file: `localStorage.setItem("defender-of-egril:gamedata/maps/test.json", "{}")`
1. Navigate to World Map screen
2. **Expected**: "Add New Content" dialog should NOT show all files as new, only missing ones
3. **Verification**: `fileExists("gamedata")` returns true due to the existing map file

### Test 4: File vs Directory Name Collision
**Setup**:
1. Clear localStorage
2. Create a file: `localStorage.setItem("defender-of-egril:game", "content")`
3. Create directory content: `localStorage.setItem("defender-of-egril:gamedata/test.json", "{}")`
1. Call `fileExists("game")` → should return true (file exists)
2. Call `fileExists("gamedata")` → should return true (directory exists)
3. Call `fileExists("gamedataother")` → should return false (doesn't exist)

## Expected localStorage Keys After Sync

After syncing repository content, localStorage should contain keys like:
- `defender-of-egril:gamedata/maps/{mapId}.json`
- `defender-of-egril:gamedata/levels/{levelId}.json`
- `defender-of-egril:gamedata/sequence.json`
- `defender-of-egril:gamedata/worldmap.json`
- `defender-of-egril:gamedata/sequence-1.json` (backup, if sequence existed before)
- `defender-of-egril:gamedata/worldmap-1.json` (backup, if worldmap existed before)

## Browser Console Verification

Open browser console and check:
```javascript
// List all defender-of-egril keys
Object.keys(localStorage).filter(k => k.startsWith('defender-of-egril:'))

// Check specific files
localStorage.getItem('defender-of-egril:gamedata/sequence.json')
```

## Success Criteria

✅ First visit to World Map shows "Add New Content" dialog (if repository has content)
✅ After accepting, content is synced to localStorage
✅ Second visit to World Map does NOT show the dialog
✅ localStorage contains all expected gamedata files
✅ `fileExists("gamedata")` returns true after sync
