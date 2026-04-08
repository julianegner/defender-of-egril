# MapGen - Map Image Generation

This package generates background map images for Defender of Egril game maps.

## Origin

The approach for biome-based terrain generation is inspired by the mapgen4 project:
- **Source**: https://github.com/redblobgames/mapgen4
- **Author**: Red Blob Games (redblobgames.com)
- **License**: Apache 2.0

The original mapgen4 is written in TypeScript. This Kotlin Multiplatform implementation
adapts the core concepts (biome assignment, smooth color blending) for the hexagonal
tile-based maps used in Defender of Egril.

## Biome Mapping

| Tile Type     | Biome          | Color         |
|---------------|----------------|---------------|
| PATH          | Arid/Sandy     | Yellow-brown  |
| SPAWN_POINT   | Sandy (darker) | Dark yellow   |
| TARGET        | Deep Forest    | Dark green    |
| BUILD_AREA    | Grassland      | Green         |
| NO_PLAY       | Mountains      | Gray-brown    |
| RIVER/WATER   | Water          | Blue          |

## How It Works

1. For each tile in the map, a biome color is assigned based on tile type
2. For each pixel in the output image, nearby tile colors are blended using
   distance-weighted interpolation with smooth falloff
3. This creates smooth transitions between different biome areas

## Image Dimensions

The generated image dimensions match the exact hexagonal grid layout:
- Based on `hexSize=40dp` (same as the game's rendering)
- Uses the same coordinate formulas as `HexUtils.kt`
- Pixel coordinates match the tile center positions from the game engine

## Usage

```kotlin
// Generate PNG bytes for a map
val (pixels, width, height) = MapImageGenerator.generatePixels(editorMap)
val pngBytes = MapImageEncoder.encodeToPng(pixels, width, height)
```

## Platform Support

- **Desktop (JVM)**: Full support - generates PNG files
- **Android/iOS/WASM**: Generation not available (encoder returns null)
  - Pre-generated images from repository are used instead
