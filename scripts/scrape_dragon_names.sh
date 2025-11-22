#!/bin/bash

# Script to scrape 200 dragon names from mythopedia.com
# This script uses Playwright (via Node.js) to interact with the JavaScript-based name generator
#
# Usage:
#   ./scrape_dragon_names.sh              - Scrape from mythopedia.com
#   ./scrape_dragon_names.sh --test       - Test mode with sample data
#   ./scrape_dragon_names.sh --help       - Show this help

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_FILE="${SCRIPT_DIR}/dragon_names.txt"
TEMP_JS="${SCRIPT_DIR}/.scraper_temp.js"

# Show help
if [[ "$1" == "--help" ]] || [[ "$1" == "-h" ]]; then
    echo "Dragon Names Scraper"
    echo "===================="
    echo ""
    echo "This script scrapes 200 dragon names from mythopedia.com's name generator."
    echo "The generator has 5 text fields and a 'Generate Names' button that this script"
    echo "automatically interacts with to collect unique dragon names."
    echo ""
    echo "Usage:"
    echo "  $0              Scrape dragon names from mythopedia.com"
    echo "  $0 --test       Test mode (generates sample names for testing)"
    echo "  $0 --help       Show this help message"
    echo ""
    echo "Requirements:"
    echo "  - Node.js and npm must be installed"
    echo "  - Internet connection to mythopedia.com"
    echo "  - Playwright will be installed automatically if not present"
    echo ""
    echo "Output:"
    echo "  Names are saved to: $OUTPUT_FILE"
    echo "  One name per line, 200 names total"
    exit 0
fi

# Test mode - generate sample dragon names
if [[ "$1" == "--test" ]]; then
    echo "Running in TEST mode - generating sample dragon names..."
    
    # Generate 200 sample dragon names
    cat > "$OUTPUT_FILE" << 'ENDNAMES'
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
Sunfyre
Dreamfyre
Meleys
Caraxes
Syrax
Seasmoke
Vermax
Arrax
Tyraxes
Moondancer
Stormcloud
Morghul
Shrykos
Tessarion
Grey Ghost
Cannibal
Sheepstealer
Morning
Quicksilver
Terrax
Urrax
Ghiscar
Jadefyre
Tyrfing
Fafnir
Nidhogg
Jormungandr
Ladon
Falkor
Draco
Mushu
Toothless
Hookfang
Meatlug
Stormfly
Barf
Belch
Cloudjumper
Grump
Skullcrusher
Valka
Windshear
Razorwhip
Scauldron
Thunderdrum
Whispering
Nightfury
Deathsong
Snaptrapper
Changewing
Flightmare
Boneknapper
Screaming
Fireworm
Gronckle
Hotburple
Catastrophic
Quaken
Zippleback
Nadder
Nightmare
Bewilderbeast
Redclaw
Sleuther
Woolly
Rumblehorn
Raincutter
Timberjack
Cavern
Crimson
Typhoomerang
Moldruffle
Armorwing
Snafflefang
Eruptodon
Singetail
Flame
Deathgripper
Light
Sentinel
Heatwave
Blazewing
Ashwing
Emberwing
Scorchclaw
Firefang
Pyroscale
Infernox
Vulcanor
Magmawing
Lavacrest
Cinderblaze
Flameheart
Heatseeker
Burnscale
Slagwing
Coalfang
Sparkwing
Glowscale
Radiantclaw
Lusterfang
Shimmerscale
Prismatic
Crystalwing
Gemclaw
Diamondfang
Sapphirescale
Rubyeye
Emeraldwing
Topazfang
Onyxclaw
Obsidianscale
Jadewing
Pearlclaw
Opalheart
Amberscale
Quartzwing
Turquoisefang
Azureclaw
Cobaltscale
Indigowing
Violetfang
Mauveclaw
Crimsoneye
Scarletwing
Vermilionfang
Roseclaw
Carnelianscale
Rustywing
Copperclaw
Bronzefang
Goldenscale
Silverclaw
Platinumwing
Ironfang
Steelclaw
Titanscale
Mithrilwing
Adamantine
Chromeclaw
Mercuryfang
Tinscale
Leadwing
Zincfang
Brassclaw
Pewterscale
Nickelwing
Cobaltfang
Magnesium
Aluminumwing
Tungstenclaw
Vanadiumfang
Titaniumscale
Palladiumwing
Rhodiumclaw
Iridiumfang
Osmiumscale
Rutheniumwing
Platinumclaw
Goldfang
Silverscale
Electrumwing
Coalclaw
Graphitefang
Diamondscale
Carbonwing
Siliconclaw
Sulfurfang
Phosphorscale
Nitrogenwing
Oxygenclaw
Hydrogenfang
Heliumscale
Neonwing
Argonclaw
Kryptonfang
Xenonscale
Radonwing
Astralclaw
Celestialfang
Cosmicscale
Stellarwing
Nebulaclaw
Galaxyfang
Voidscale
Eclipsewing
Solarclaw
Lunarfang
Meteoritescale
Cometowing
Asteroidclaw
Saturncfang
ENDNAMES
    
    NAME_COUNT=$(wc -l < "$OUTPUT_FILE")
    echo ""
    echo "✓ Generated $NAME_COUNT sample dragon names"
    echo "✓ Names saved to: $OUTPUT_FILE"
    echo ""
    echo "First 10 names:"
    echo "---------------"
    head -10 "$OUTPUT_FILE"
    echo "..."
    echo "... (and $((NAME_COUNT - 10)) more)"
    exit 0
fi

# Clean up on exit
cleanup() {
    rm -f "$TEMP_JS"
}
trap cleanup EXIT

echo "Creating Node.js scraper script..."

# Create a Node.js script using Playwright to scrape the names
cat > "$TEMP_JS" << 'ENDOFJS'
const { chromium } = require('playwright');

async function scrapeNames() {
    console.error('Launching browser...');
    const browser = await chromium.launch({
        headless: true,
        args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-dev-shm-usage'
        ]
    });

    try {
        const context = await browser.newContext({
            userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        });
        const page = await context.newPage();
        
        console.error('Navigating to dragon names generator...');
        await page.goto('https://mythopedia.com/name-generator/dragon-names/', {
            waitUntil: 'networkidle',
            timeout: 30000
        });

        console.error('Page loaded, waiting for form...');
        
        // Wait for text inputs to be visible (the 5 name fields)
        await page.waitForSelector('input[type="text"]', { timeout: 10000 });

        const allNames = new Set();
        let iteration = 0;
        const maxIterations = 50; // Safety limit

        while (allNames.size < 200 && iteration < maxIterations) {
            iteration++;
            console.error(`Iteration ${iteration}/${maxIterations} - Names collected: ${allNames.size}/200`);
            
            try {
                // Look for the generate button - try multiple selectors
                const button = await page.locator('button').filter({ hasText: /generate names/i }).first()
                    .or(page.locator('input[type="submit"]'))
                    .or(page.locator('button').first());
                
                // Click the button
                await button.click({ timeout: 5000 });
                
                // Wait a bit for names to be generated
                await page.waitForTimeout(1500);

                // Extract names from text input fields
                const names = await page.evaluate(() => {
                    const inputs = Array.from(document.querySelectorAll('input[type="text"]'));
                    return inputs.map(input => input.value).filter(v => v && v.trim());
                });

                console.error(`  Found ${names.length} names in this iteration`);
                
                if (names.length === 0) {
                    console.error('  Warning: No names found, waiting longer...');
                    await page.waitForTimeout(2000);
                    continue;
                }

                // Add names to set (automatically handles duplicates)
                names.forEach(name => {
                    if (name.trim()) {
                        allNames.add(name.trim());
                    }
                });
                
                // Small delay between iterations to be respectful
                await page.waitForTimeout(300);
                
            } catch (err) {
                console.error(`  Error in iteration ${iteration}: ${err.message}`);
                // Try to continue anyway
                await page.waitForTimeout(1000);
            }
        }

        console.error(`\nTotal unique names collected: ${allNames.size}`);
        
        // Convert set to array and output (one per line)
        const namesArray = Array.from(allNames).slice(0, 200);
        namesArray.forEach(name => console.log(name));

        if (namesArray.length < 200) {
            console.error(`Warning: Only collected ${namesArray.length} names out of 200 requested`);
        }

    } catch (error) {
        console.error('Error during scraping:', error.message);
        console.error('\nNote: If you see ERR_NAME_NOT_RESOLVED or ERR_BLOCKED_BY_CLIENT,');
        console.error('the website may be blocked in your environment.');
        console.error('Try running with --test flag for a demonstration with sample data.');
        throw error;
    } finally {
        await browser.close();
    }
}

scrapeNames().catch(err => {
    console.error('Fatal error:', err.message || err);
    process.exit(1);
});
ENDOFJS

echo "Checking for Playwright..."
cd "$SCRIPT_DIR"

# Check if we're in a node project, if not initialize
if [ ! -f "package.json" ]; then
    echo "Initializing npm project..."
    npm init -y > /dev/null 2>&1
fi

# Check if playwright is installed locally
if ! npm list playwright > /dev/null 2>&1; then
    echo "Installing playwright (this may take a moment)..."
    if ! npm install playwright > /dev/null 2>&1; then
        echo "Warning: Failed to install playwright locally"
        echo "Attempting to continue anyway..."
    fi
fi

# Install playwright browsers if needed
if [ ! -d "$HOME/.cache/ms-playwright/chromium_headless_shell"* ]; then
    echo "Installing Playwright browsers..."
    npx playwright install chromium > /dev/null 2>&1 || echo "Warning: Could not install browsers"
fi

echo "Starting scraper..."
echo "This will collect 200 dragon names from the name generator..."
echo ""

# Run the scraper and save output
TEMP_OUTPUT="${OUTPUT_FILE}.tmp"
if node "$TEMP_JS" > "$TEMP_OUTPUT" 2>&1; then
    # Extract only the actual names (lines that don't start with typical log prefixes)
    # and don't contain "Error", "Warning", etc.
    grep -v "^Launching\|^Navigating\|^Page loaded\|^Iteration\|Found.*names\|^Warning\|^Total\|^Error\|^Fatal\|^  \|^Note:" "$TEMP_OUTPUT" | \
    grep -v "^$" > "$OUTPUT_FILE" || true
    
    # Count the lines
    NAME_COUNT=$(wc -l < "$OUTPUT_FILE" 2>/dev/null || echo "0")
    
    if [ "$NAME_COUNT" -gt 0 ]; then
        echo ""
        echo "✓ Success! Scraped $NAME_COUNT dragon names"
        echo "✓ Names saved to: $OUTPUT_FILE"
        echo ""
        echo "First 10 names:"
        echo "---------------"
        head -10 "$OUTPUT_FILE"
        if [ "$NAME_COUNT" -gt 10 ]; then
            echo "..."
            echo "... (and $(($NAME_COUNT - 10)) more)"
        fi
        rm -f "$TEMP_OUTPUT"
    else
        echo "✗ Error: No names were successfully scraped"
        echo ""
        echo "Full output:"
        cat "$TEMP_OUTPUT"
        echo ""
        echo "If the website is blocked, try running in test mode:"
        echo "  $0 --test"
        rm -f "$TEMP_OUTPUT"
        exit 1
    fi
else
    echo "✗ Error: Scraping failed"
    echo ""
    echo "Output:"
    cat "$TEMP_OUTPUT"
    echo ""
    echo "If the website is blocked, try running in test mode:"
    echo "  $0 --test"
    rm -f "$TEMP_OUTPUT"
    exit 1
fi
