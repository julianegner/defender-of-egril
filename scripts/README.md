# Scripts

This directory contains utility scripts for the Defender of Egril project.

## Hex Grid Debug Image Generator

### Overview

`generate_hex_grid_debug_images.py` reads the map JSON + background PNG files
from the repository and produces overlay images where each hexagonal tile cell
is tinted with its in-game colour.  The images are useful for visually
verifying tile placement and for using as a reference layer when enhancing map
background images in external tools.

Output is written to `scripts/map-debug-images/` which is gitignored so the
generated files never become part of the release build.

### Requirements

- **Python 3.8+**
- **Pillow**: `pip install Pillow`

### Usage

```bash
# Generate light-mode overlay images (default)
python3 scripts/generate_hex_grid_debug_images.py

# Generate dark-mode overlay images
python3 scripts/generate_hex_grid_debug_images.py --dark

# Custom output directory
python3 scripts/generate_hex_grid_debug_images.py --output /path/to/output

# Adjust overlay transparency (0-255)
python3 scripts/generate_hex_grid_debug_images.py --fill-alpha 60 --border-alpha 220
```

### Options

| Option | Default | Description |
|---|---|---|
| `--dark` | off | Use dark-mode tile colours |
| `--output <dir>` | `scripts/map-debug-images` | Output directory |
| `--fill-alpha N` | 80 | Opacity of tile fill (0 = transparent, 255 = opaque) |
| `--border-alpha N` | 200 | Opacity of hex border lines |

### Output

One PNG per map is written to the output directory, named
`<map_id>_hex_grid_light.png` (or `_dark.png` with `--dark`).

Tile-type colours match `getTileColor()` in `TileUtils.kt`:

| Tile type | Light colour | Dark colour |
|---|---|---|
| PATH | Brown `#8B4513` | Dark brown `#4A2F1A` |
| BUILD_AREA | Light green `#90EE90` | Dark green `#456C2E` |
| NO_PLAY | Dark grey `#404040` | Near-black `#1A1A1A` |
| SPAWN_POINT | Red `#FF0000` | Dark red `#8B0000` |
| TARGET | Blue `#0000FF` | Dark blue `#00008B` |
| RIVER | Steel blue `#4682B4` | Dark steel blue `#1E3A5F` |

### How It Works

1. Locates all `*.json` map files in `frontend/.../files/repository/maps/`
2. For each map, opens the corresponding `*.png` background image
3. Calculates hex cell centres using the same geometry as `MapImageGenerator.kt`:
   - `HEX_SIZE = 40 px`, pointy-top hexagons
   - `HORIZONTAL_SPACING = -10 px` (slight column overlap)
   - Odd rows offset right by `HEX_WIDTH × 0.42`
   - 20 px padding around the whole grid
4. Draws a semi-transparent coloured fill and border for each tile
5. Composites the overlay onto the background and saves the result

## Build Windows EXE

### Overview

`build-windows-exe.sh` is a convenience script that builds a Windows EXE installer for the game using Gradle.

### Requirements

- **JDK 11+** must be installed
- **Gradle** (included via wrapper)
- Can be run from any platform (Linux, macOS, or Windows with bash/WSL)

### Usage

```bash
# Build Windows EXE installer
./build-windows-exe.sh

# Or run directly with Gradle
./gradlew :composeApp:packageExe
```

### Output

The script creates a Windows EXE installer at:
`composeApp/build/compose/binaries/main/exe/`

The installer file will be named: `DefenderOfEgril-1.0.0.exe` (or similar based on version)

### How It Works

1. **Gradle Task**: Executes the `:composeApp:packageExe` Gradle task
2. **JPackage**: Uses Java's jpackage tool to create a native Windows installer
3. **Bundling**: Packages the JVM, application code, and resources into a single EXE
4. **Output**: Creates a self-contained Windows installer

### Note

- Building on Linux/macOS creates a Windows-compatible installer
- The build process may take 5-10 minutes depending on your machine
- The resulting EXE can be installed on any Windows 10+ system

## Dragon Names Scraper

### Overview

`scrape_dragon_names.sh` is a bash script that scrapes dragon names from [mythopedia.com's dragon name generator](https://mythopedia.com/name-generator/dragon-names/).

The name generator has 5 text fields above a "Generate Names" button. This script automates the process of:
1. Clicking the "Generate Names" button
2. Extracting the generated names from the text fields
3. Repeating until 200 unique names are collected
4. Saving all names to a text file (one name per line)

### Requirements

- **Node.js** and **npm** must be installed
- **Internet connection** to access mythopedia.com
- **Playwright** (installed automatically by the script)

### Usage

```bash
# Scrape dragon names from mythopedia.com
./scrape_dragon_names.sh

# Test mode (generates 200 sample dragon names for demonstration)
./scrape_dragon_names.sh --test

# Show help
./scrape_dragon_names.sh --help
```

### Output

The script creates a file named `dragon_names.txt` in the same directory, containing:
- 200 unique dragon names
- One name per line
- No headers or additional formatting

Example output:
```
Alduin
Smaug
Drogon
...
```

### How It Works

1. **Browser Automation**: Uses Playwright to control a headless Chrome browser
2. **Page Navigation**: Navigates to the dragon name generator page
3. **Element Detection**: Finds the 5 text input fields and the "Generate Names" button
4. **Iterative Collection**: 
   - Clicks the button to generate names
   - Extracts names from the text fields
   - Repeats until 200 unique names are collected (typically 40-50 iterations)
5. **Deduplication**: Uses a Set to ensure all names are unique
6. **Output**: Saves names to `dragon_names.txt`

### Troubleshooting

**Issue**: Website is blocked or unreachable
- **Solution**: Use test mode: `./scrape_dragon_names.sh --test`

**Issue**: Playwright installation fails
- **Solution**: Manually install: `npm install playwright && npx playwright install chromium`

**Issue**: Script times out
- **Solution**: The website might be slow or down. Try again later.

**Issue**: Fewer than 200 names collected
- **Solution**: The script will save what it collected. Run again to get more names.

### Test Mode

Test mode (`--test` flag) generates 200+ sample dragon names without accessing the internet. This is useful for:
- Testing the output format
- Demonstrating the script's functionality
- Working in environments where the website is blocked

### Files Generated

- `dragon_names.txt` - The output file with scraped names
- `package.json` - NPM package file (auto-generated)
- `node_modules/` - NPM dependencies (auto-generated, ignored by git)
- `.scraper_temp.js` - Temporary Node.js script (auto-cleaned)

### Technical Details

- **Language**: Bash shell script with embedded Node.js
- **Browser Automation**: Playwright
- **Browser**: Chromium (headless mode)
- **Execution Time**: ~30-60 seconds (depending on website speed)
- **Rate Limiting**: Built-in delays between requests (300-1500ms)

### Example Session

```bash
$ ./scrape_dragon_names.sh
Creating Node.js scraper script...
Checking for Playwright...
Starting scraper...
This will collect 200 dragon names from the name generator...

✓ Success! Scraped 200 dragon names
✓ Names saved to: /home/user/scripts/dragon_names.txt

First 10 names:
---------------
Alduin
Smaug
Drogon
Rhaegal
Viserion
Balerion
Vhagar
Meraxes
Vermithor
Silverwing
...
... (and 190 more)
```
