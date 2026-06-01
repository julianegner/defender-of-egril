#!/usr/bin/env python3
"""Detect likely river tiles from a generated map PNG and map JSON geometry."""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

from PIL import Image, ImageDraw

HEX_SIZE = 40.0
HEX_WIDTH = HEX_SIZE * math.sqrt(3.0)
HEX_HEIGHT = HEX_SIZE * 2.0
VERTICAL_SPACING = HEX_HEIGHT * 0.75
HORIZONTAL_SPACING = -10.0
ODD_ROW_OFFSET_RATIO = 0.42
PADDING = 20.0

TileKey = Tuple[int, int]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Detect likely river tiles by sampling map image colors at hex centers and "
            "comparing blue dominance against a threshold."
        )
    )
    parser.add_argument("--map-json", required=True, type=Path, help="Path to map_*.json")
    parser.add_argument(
        "--map-image",
        type=Path,
        help="Path to map PNG. Defaults to same directory/name as map JSON with .png extension",
    )
    parser.add_argument(
        "--threshold",
        type=float,
        default=30.0,
        help="Blue-dominance threshold (meanBlue - max(meanRed, meanGreen)); default: 30",
    )
    parser.add_argument(
        "--sample-radius",
        type=int,
        default=8,
        help="Pixel radius around center for color sampling; default: 8",
    )
    parser.add_argument(
        "--sample-step",
        type=int,
        default=4,
        help="Sampling step in pixels; default: 4",
    )
    parser.add_argument(
        "--min-river-neighbors",
        type=int,
        default=1,
        help="Require at least N detected hex-neighbors; default: 1",
    )
    parser.add_argument(
        "--output-candidates",
        type=Path,
        help="Where to write candidate river tiles JSON",
    )
    parser.add_argument(
        "--output-preview",
        type=Path,
        help="Where to write PNG preview overlay",
    )
    parser.add_argument(
        "--output-map",
        type=Path,
        help="Optional map JSON with RIVER tiles + riverTiles updated from detection",
    )
    return parser.parse_args()


def hex_center(gx: int, gy: int) -> Tuple[float, float]:
    row_offset = HEX_WIDTH * ODD_ROW_OFFSET_RATIO if gy % 2 == 1 else 0.0
    cx = gx * (HEX_WIDTH + HORIZONTAL_SPACING) + row_offset + HEX_WIDTH / 2 + PADDING
    cy = gy * VERTICAL_SPACING + HEX_HEIGHT / 2 + PADDING
    return cx, cy


def in_bounds(x: int, y: int, width: int, height: int) -> bool:
    return 0 <= x < width and 0 <= y < height


def hex_neighbors(x: int, y: int) -> List[TileKey]:
    if y % 2 == 0:
        offsets = [(1, 0), (0, -1), (-1, -1), (-1, 0), (-1, 1), (0, 1)]
    else:
        offsets = [(1, 0), (1, -1), (0, -1), (-1, 0), (0, 1), (1, 1)]
    return [(x + dx, y + dy) for dx, dy in offsets]


def tile_scores(
    image: Image.Image,
    map_width: int,
    map_height: int,
    sample_radius: int,
    sample_step: int,
) -> Dict[TileKey, Dict[str, float]]:
    rgb = image.convert("RGB")
    px = rgb.load()
    scores: Dict[TileKey, Dict[str, float]] = {}

    for x in range(map_width):
        for y in range(map_height):
            cx, cy = hex_center(x, y)
            samples = []
            for dx in range(-sample_radius, sample_radius + 1, sample_step):
                for dy in range(-sample_radius, sample_radius + 1, sample_step):
                    sx = max(0, min(rgb.width - 1, int(round(cx + dx))))
                    sy = max(0, min(rgb.height - 1, int(round(cy + dy))))
                    samples.append(px[sx, sy])

            mean_r = sum(s[0] for s in samples) / len(samples)
            mean_g = sum(s[1] for s in samples) / len(samples)
            mean_b = sum(s[2] for s in samples) / len(samples)
            blue_dominance = mean_b - max(mean_r, mean_g)

            scores[(x, y)] = {
                "meanR": mean_r,
                "meanG": mean_g,
                "meanB": mean_b,
                "blueDominance": blue_dominance,
            }

    return scores


def refine_with_neighbor_rule(
    candidates: Iterable[TileKey],
    map_width: int,
    map_height: int,
    min_neighbors: int,
) -> List[TileKey]:
    if min_neighbors <= 0:
        return sorted(candidates)

    candidate_set = set(candidates)
    refined = []
    for x, y in sorted(candidate_set):
        neighbor_count = sum(
            1
            for nx, ny in hex_neighbors(x, y)
            if in_bounds(nx, ny, map_width, map_height) and (nx, ny) in candidate_set
        )
        if neighbor_count >= min_neighbors:
            refined.append((x, y))
    return refined


def path_key(x: int, y: int) -> str:
    return f"{x},{y}"


def write_candidates(
    output_path: Path,
    map_id: str,
    candidates: List[TileKey],
    current_rivers: set[str],
    scores: Dict[TileKey, Dict[str, float]],
) -> None:
    payload = {
        "mapId": map_id,
        "detectedRiverCount": len(candidates),
        "newlyDetected": [
            path_key(x, y) for (x, y) in candidates if path_key(x, y) not in current_rivers
        ],
        "missingComparedToCurrent": sorted(
            list(current_rivers - {path_key(x, y) for (x, y) in candidates})
        ),
        "candidates": [
            {
                "key": path_key(x, y),
                "blueDominance": round(scores[(x, y)]["blueDominance"], 3),
                "meanR": round(scores[(x, y)]["meanR"], 3),
                "meanG": round(scores[(x, y)]["meanG"], 3),
                "meanB": round(scores[(x, y)]["meanB"], 3),
            }
            for (x, y) in candidates
        ],
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_preview(
    output_path: Path,
    source_image: Image.Image,
    candidates: List[TileKey],
    current_rivers: set[str],
) -> None:
    preview = source_image.convert("RGBA")
    draw = ImageDraw.Draw(preview, "RGBA")

    candidate_keys = {path_key(x, y) for x, y in candidates}

    for key in current_rivers | candidate_keys:
        x_str, y_str = key.split(",")
        x, y = int(x_str), int(y_str)
        cx, cy = hex_center(x, y)
        bbox = [cx - 7, cy - 7, cx + 7, cy + 7]

        in_current = key in current_rivers
        in_detected = key in candidate_keys

        if in_current and in_detected:
            fill, outline = (0, 255, 255, 180), (0, 255, 255, 255)  # cyan
        elif in_detected and not in_current:
            fill, outline = (255, 230, 0, 190), (255, 200, 0, 255)  # yellow
        else:
            fill, outline = (255, 80, 80, 170), (255, 80, 80, 255)  # red

        draw.ellipse(bbox, fill=fill, outline=outline, width=2)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    preview.save(output_path)


def write_output_map(
    output_path: Path,
    map_payload: dict,
    candidates: List[TileKey],
) -> None:
    updated = json.loads(json.dumps(map_payload))
    data = updated["data"]
    tiles = data["tiles"]
    river_tiles = data.get("riverTiles", {})

    detected_keys = {path_key(x, y) for x, y in candidates}

    for x in range(data["width"]):
        for y in range(data["height"]):
            key = path_key(x, y)
            if key in detected_keys:
                tiles[key] = "RIVER"
                river_tiles[key] = river_tiles.get(
                    key,
                    {
                        "flowDirection": "NONE",
                        "flowSpeed": 1,
                    },
                )
            elif tiles.get(key) == "RIVER":
                tiles[key] = "PATH"
                river_tiles.pop(key, None)

    data["riverTiles"] = river_tiles
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(updated, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    args = parse_args()

    map_json_path = args.map_json.resolve()
    if not map_json_path.exists():
        raise SystemExit(f"Map JSON not found: {map_json_path}")

    map_image_path = args.map_image.resolve() if args.map_image else map_json_path.with_suffix(".png")
    if not map_image_path.exists():
        raise SystemExit(f"Map image not found: {map_image_path}")

    payload = json.loads(map_json_path.read_text(encoding="utf-8"))
    data = payload["data"]

    map_id = data["id"]
    map_width = int(data["width"])
    map_height = int(data["height"])
    tiles: Dict[str, str] = data["tiles"]

    current_rivers = {k for k, v in tiles.items() if v == "RIVER"}

    image = Image.open(map_image_path)
    scores = tile_scores(image, map_width, map_height, args.sample_radius, args.sample_step)

    raw_candidates = [
        pos for pos, score in scores.items() if score["blueDominance"] >= args.threshold
    ]
    candidates = refine_with_neighbor_rule(
        raw_candidates,
        map_width,
        map_height,
        args.min_river_neighbors,
    )

    candidate_keys = {path_key(x, y) for x, y in candidates}
    added = sorted(candidate_keys - current_rivers)
    removed = sorted(current_rivers - candidate_keys)

    default_base = map_json_path.with_suffix("")
    output_candidates = args.output_candidates or default_base.with_name(f"{default_base.name}_river_candidates.json")
    output_preview = args.output_preview or default_base.with_name(f"{default_base.name}_river_preview.png")

    write_candidates(output_candidates, map_id, candidates, current_rivers, scores)
    write_preview(output_preview, image, candidates, current_rivers)

    if args.output_map:
        write_output_map(args.output_map, payload, candidates)

    print(f"Map: {map_id} ({map_width}x{map_height})")
    print(f"Threshold: {args.threshold:.2f}, minRiverNeighbors: {args.min_river_neighbors}")
    print(f"Current river tiles: {len(current_rivers)}")
    print(f"Detected river tiles: {len(candidate_keys)}")
    print(f"Newly detected (not currently RIVER): {len(added)}")
    print(f"Missing vs current (currently RIVER but not detected): {len(removed)}")
    print(f"Candidates JSON: {output_candidates}")
    print(f"Preview image:   {output_preview}")
    if args.output_map:
        print(f"Updated map JSON: {args.output_map}")


if __name__ == "__main__":
    main()
