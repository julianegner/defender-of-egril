#!/usr/bin/env python3
"""
Generate level map background images with hex grid overlay for debugging.

Reads map JSON + PNG files from the repository and produces overlay images
where each hexagonal tile cell is tinted with its in-game color.  The images
are written to scripts/map-debug-images/ which is gitignored so they never end
up in a release build.

Usage:
    python3 scripts/generate_hex_grid_debug_images.py [--dark] [--output <dir>]

Options:
    --dark          Use dark-mode tile colors (default: light mode)
    --output <dir>  Output directory (default: scripts/map-debug-images)
    --fill-alpha N  Opacity of the tile fill, 0-255 (default: 80)
    --border-alpha N Opacity of the hex border, 0-255 (default: 200)

Requirements:
    pip install Pillow
"""

import argparse
import json
import math
import os
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:
    print("ERROR: Pillow is required.  Install it with:  pip install Pillow")
    sys.exit(1)

# ---------------------------------------------------------------------------
# Hex grid geometry — must match MapImageGenerator.kt exactly
# ---------------------------------------------------------------------------
HEX_SIZE = 40.0
SQRT3 = math.sqrt(3.0)
HEX_WIDTH = HEX_SIZE * SQRT3          # ~69.28 px
HEX_HEIGHT = HEX_SIZE * 2.0           # 80 px
VERTICAL_SPACING = HEX_HEIGHT * 0.75  # 60 px
HORIZONTAL_SPACING = -10.0
ODD_ROW_OFFSET_RATIO = 0.42
PADDING = 20.0


def hex_center(gx: int, gy: int):
    """Return the pixel centre of the hex tile at grid column gx, row gy."""
    row_offset = HEX_WIDTH * ODD_ROW_OFFSET_RATIO if gy % 2 == 1 else 0.0
    cx = gx * (HEX_WIDTH + HORIZONTAL_SPACING) + row_offset + HEX_WIDTH / 2 + PADDING
    cy = gy * VERTICAL_SPACING + HEX_HEIGHT / 2 + PADDING
    return cx, cy


def hex_vertices(cx: float, cy: float):
    """
    Return the 6 vertices of a pointy-top hexagon centred at (cx, cy).
    Matches the drawHexagon() formula in HexagonMinimap.kt:
        angle = PI * (60 * i - 30) / 180
    """
    verts = []
    for i in range(6):
        angle = math.pi * (60.0 * i - 30.0) / 180.0
        vx = cx + HEX_SIZE * math.cos(angle)
        vy = cy + HEX_SIZE * math.sin(angle)
        verts.append((vx, vy))
    return verts


# ---------------------------------------------------------------------------
# Tile colours — matches getTileColor() in TileUtils.kt
# ---------------------------------------------------------------------------
TILE_COLORS_LIGHT = {
    "PATH":        (0x8B, 0x45, 0x13),   # saddle-brown
    "BUILD_AREA":  (0x90, 0xEE, 0x90),   # light-green
    "NO_PLAY":     (0x40, 0x40, 0x40),   # dark-grey
    "SPAWN_POINT": (0xFF, 0x00, 0x00),   # red
    "TARGET":      (0x00, 0x00, 0xFF),   # blue
    "RIVER":       (0x46, 0x82, 0xB4),   # steel-blue
}

TILE_COLORS_DARK = {
    "PATH":        (0x4A, 0x2F, 0x1A),
    "BUILD_AREA":  (0x45, 0x6C, 0x2E),
    "NO_PLAY":     (0x1A, 0x1A, 0x1A),
    "SPAWN_POINT": (0x8B, 0x00, 0x00),
    "TARGET":      (0x00, 0x00, 0x8B),
    "RIVER":       (0x1E, 0x3A, 0x5F),
}


# ---------------------------------------------------------------------------
# Main generator
# ---------------------------------------------------------------------------

def generate_debug_image(
    map_json_path: Path,
    map_png_path: Path,
    output_dir: Path,
    dark_mode: bool = False,
    fill_alpha: int = 80,
    border_alpha: int = 200,
):
    """Overlay hex grid on the map background image and save to output_dir."""
    tile_colors = TILE_COLORS_DARK if dark_mode else TILE_COLORS_LIGHT

    # --- Parse map JSON ---
    with open(map_json_path, "r", encoding="utf-8") as f:
        raw = json.load(f)

    data = raw.get("data", raw)
    map_id = data.get("id", map_json_path.stem)
    width = int(data["width"])
    height = int(data["height"])
    tiles: dict = data.get("tiles", {})

    # --- Open background PNG ---
    bg = Image.open(map_png_path).convert("RGBA")

    # --- Create overlay ---
    overlay = Image.new("RGBA", bg.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)

    for gy in range(height):
        for gx in range(width):
            tile_type = tiles.get(f"{gx},{gy}", "NO_PLAY")
            color_rgb = tile_colors.get(tile_type, tile_colors["NO_PLAY"])
            verts = hex_vertices(*hex_center(gx, gy))
            polygon = [(round(x), round(y)) for x, y in verts]

            # Semi-transparent fill
            fill_color = (*color_rgb, fill_alpha)
            draw.polygon(polygon, fill=fill_color)

            # Border
            border_color = (*color_rgb, border_alpha)
            draw.polygon(polygon, outline=border_color)

    # --- Composite ---
    result = Image.alpha_composite(bg, overlay)

    # --- Save ---
    output_dir.mkdir(parents=True, exist_ok=True)
    suffix = "_dark" if dark_mode else "_light"
    out_path = output_dir / f"{map_id}_hex_grid{suffix}.png"
    result.convert("RGB").save(out_path, "PNG", optimize=False)
    return out_path


def main():
    parser = argparse.ArgumentParser(
        description="Generate hex-grid debug overlay images for all level maps."
    )
    parser.add_argument(
        "--dark",
        action="store_true",
        help="Use dark-mode tile colours (default: light mode)",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="Output directory (default: scripts/map-debug-images next to this script)",
    )
    parser.add_argument(
        "--fill-alpha",
        type=int,
        default=80,
        metavar="N",
        help="Opacity of the tile-type fill overlay, 0-255 (default: 80)",
    )
    parser.add_argument(
        "--border-alpha",
        type=int,
        default=200,
        metavar="N",
        help="Opacity of the hex border lines, 0-255 (default: 200)",
    )
    args = parser.parse_args()

    # Locate repository root relative to this script
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent
    maps_dir = (
        repo_root
        / "frontend"
        / "composeApp"
        / "src"
        / "commonMain"
        / "composeResources"
        / "files"
        / "repository"
        / "maps"
    )

    if not maps_dir.exists():
        print(f"ERROR: maps directory not found: {maps_dir}")
        sys.exit(1)

    output_dir = Path(args.output) if args.output else script_dir / "map-debug-images"

    json_files = sorted(maps_dir.glob("*.json"))
    if not json_files:
        print(f"No map JSON files found in {maps_dir}")
        sys.exit(1)

    generated = []
    skipped = []
    for json_path in json_files:
        png_path = json_path.with_suffix(".png")
        if not png_path.exists():
            skipped.append(json_path.name)
            continue
        out = generate_debug_image(
            map_json_path=json_path,
            map_png_path=png_path,
            output_dir=output_dir,
            dark_mode=args.dark,
            fill_alpha=args.fill_alpha,
            border_alpha=args.border_alpha,
        )
        generated.append(out)
        print(f"  ✓  {out.name}")

    print(f"\nGenerated {len(generated)} image(s) in: {output_dir}")
    if skipped:
        print(f"Skipped (no PNG): {', '.join(skipped)}")


if __name__ == "__main__":
    main()
