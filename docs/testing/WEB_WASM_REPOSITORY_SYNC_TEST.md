# Web/WASM Repository Sync Testing

## Background

Game data (levels, maps, world map, tile PNGs) is bundled as Compose resources. Reading from
`Res.readBytes(…)` on every launch would re-parse and re-deserialise everything from scratch.
The storage layer caches this data so that subsequent launches can skip the full reload via a
fingerprint fast-path check, making startup significantly faster.

Storage is backed by the **Origin Private File System (OPFS)** API (`navigator.storage.getDirectory()`),
which is a sandboxed filesystem in the browser that supports large binary files and is not subject to
the 5–10 MB `localStorage` quota. On the very first launch (or when OPFS is unavailable), the
implementation falls back to / migrates from `localStorage`.

## Fix (original issue)
`OPFSFileStorage.fileExists()` checks both:
1. If path exists as a file (key present in the in-memory cache)
2. If path exists as a virtual directory (any cached path starts with `path + "/"`)

## Manual Test Scenarios

### Test 1: Initial State - No Cached Data
**Setup**: Clear OPFS data (DevTools → Application → Storage → Clear site data) and any
`defender-of-egril:` localStorage keys.
1. Navigate to World Map screen
2. **Expected**: "Add New Content" dialog should show all repository levels as new
3. Click "Add New Content" button
4. **Expected**: Dialog closes, levels are synced to OPFS
5. Verify OPFS contains the gamedata directory (DevTools → Application → Storage → Origin Private File System)

### Test 2: After Sync - Gamedata Exists
**Setup**: Complete Test 1 first
1. Navigate away from World Map (e.g., go to Main Menu)
2. Navigate back to World Map screen
3. **Expected**: "Add New Content" dialog should NOT show
4. **Verification**: `fileExists("gamedata")` returns true because the in-memory cache has paths starting with `"gamedata/"`

### Test 3: Partial Gamedata
**Setup**:
1. Clear OPFS / localStorage
2. Pre-populate one map file in OPFS via console:
   ```javascript
   const root = await navigator.storage.getDirectory();
   const app = await root.getDirectoryHandle('defender-of-egril', {create:true});
   const gd = await app.getDirectoryHandle('gamedata', {create:true});
   const maps = await gd.getDirectoryHandle('maps', {create:true});
   const fh = await maps.getFileHandle('test.json', {create:true});
   const w = await fh.createWritable();
   await w.write('{}');
   await w.close();
   ```

**Steps**:
1. Navigate to World Map screen
2. **Expected**: "Add New Content" dialog should NOT show all files as new, only missing ones
3. **Verification**: `fileExists("gamedata")` returns true due to the existing map file in cache

### Test 4: File vs Directory Name Collision
**Setup**: Pre-populate in-memory cache by writing two files via the API:
- write `"game"` → `"content"`
- write `"gamedata/test.json"` → `"{}"`

**Verification**:
1. Call `fileExists("game")` → should return true (file exists)
2. Call `fileExists("gamedata")` → should return true (directory exists)
3. Call `fileExists("gamedataother")` → should return false (doesn't exist)

### Test 5: localStorage → OPFS Migration
**Setup**:
1. Manually set localStorage keys as if from the old implementation:
   ```javascript
   localStorage.setItem('defender-of-egril:gamedata/maps/test.json', '{}');
   localStorage.setItem('defender-of-egril:gamedata/sequence.json', '[]');
   ```
2. Ensure OPFS `defender-of-egril` directory is empty (or doesn't exist)
3. Reload the app

**Verification**:
1. App should start normally
2. Console should log: `OPFSFileStorage: migrated 2 entries from localStorage to OPFS`
3. OPFS should now contain the migrated files

## Expected OPFS Structure After Sync

After syncing repository content, OPFS should contain a `defender-of-egril/` tree like:
```
defender-of-egril/
  gamedata/
    official/
      maps/{mapId}.json
      maps/{mapId}.png
      levels/{levelId}.json
      sequence.json
      worldmap.json
    version.txt
    repository_fingerprint.txt
```

## Browser Console Verification

```javascript
// List all files in OPFS
const root = await navigator.storage.getDirectory();
const app = await root.getDirectoryHandle('defender-of-egril');
const files = [];
async function walk(dir, prefix) {
  for await (const [name, handle] of dir.entries()) {
    if (handle.kind === 'file') files.push(prefix + name);
    else await walk(handle, prefix + name + '/');
  }
}
await walk(app, '');
console.log(files);

// Check OPFS quota usage
const estimate = await navigator.storage.estimate();
console.log('OPFS used:', estimate.usage, 'quota:', estimate.quota);
```

## Success Criteria

✅ First visit to World Map shows "Add New Content" dialog (if repository has content)
✅ After accepting, content is synced to OPFS
✅ Second visit to World Map does NOT show the dialog
✅ OPFS contains all expected gamedata files
✅ `fileExists("gamedata")` returns true after sync
✅ Existing localStorage data is migrated to OPFS on first launch with the new code
