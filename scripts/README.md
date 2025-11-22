# Scripts

This directory contains utility scripts for the Defender of Egril project.

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
