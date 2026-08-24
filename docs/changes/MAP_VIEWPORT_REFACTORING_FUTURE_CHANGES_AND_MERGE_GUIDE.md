# Map Viewport Refactoring: Future Changes and Merge Guide

## Why this guide exists

This pull request introduced a new map-rendering structure that improves performance by:

- centralizing viewport math in `MapViewportUtils.kt`
- culling off-screen tiles in `HexagonalMapView.kt`
- reducing per-cell recomposition pressure in `GameMap.kt`

Use this guide to keep future work compatible with that structure.

## How to implement future changes (new way)

When changing gameplay map rendering, follow these rules:

1. Keep viewport calculations in `frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/MapViewportUtils.kt`.
2. Keep tile culling decisions in `frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/hexagon/HexagonalMapView.kt`.
3. Keep `GameMap.kt` focused on game-state mapping (what to render for a tile), not viewport geometry math.
4. Precompute derived data in `GameMap.kt` before `GridCell(...)` calls instead of recalculating inside each cell.
5. If pan/zoom transform behavior changes, update both:
   - `computeVisibleTileRange(...)`
   - `constrainMapOffsets(...)`
6. Keep off-screen tile layout stable by preserving spacer-based row/column structure in `HexagonalMapView`.
7. Add or update tests in `frontend/composeApp/src/commonTest/kotlin/de/egril/defender/ui/ViewportTileCullingTest.kt` whenever viewport math changes.

## How to merge older branches into this new structure

If another branch was created before this refactor and also edits map rendering:

1. Merge/rebase and resolve conflicts in this order:
   - `MapViewportUtils.kt`
   - `HexagonalMapView.kt`
   - `GameMap.kt`
2. In conflicts, prefer the refactored viewport pipeline from this PR, then re-apply feature logic from the older branch on top.
3. Do not restore old full-grid rendering behavior in `HexagonalMapView`; keep viewport culling and apply feature changes inside visible-tile rendering.
4. If the older branch adds new tile visuals/effects, integrate them in `GameMap`/`GridCellContent` without reintroducing per-cell expensive computations.
5. If the older branch changes pan/zoom rules, port those rules into `MapViewportUtils.kt` instead of duplicating math in multiple files.
6. After merge, run targeted validation:
   - `./gradlew :composeApp:testDebugUnitTest --tests "de.egril.defender.ui.ViewportTileCullingTest"`
   - `./gradlew :composeApp:testDebugUnitTest`
7. Manually verify:
   - panning does not move map outside allowed bounds
   - zooming still keeps interactions stable
   - minimap viewport tracking still matches map position

## Conflict hotspots to check first

- `HexagonalMapView`: tile loops, visible-range checks, spacer fallback
- `GameMap`: precomputed maps/sets passed to grid cells, control callbacks, offset clamping
- `MapViewportUtils`: coordinate conversion assumptions between content space and viewport space

