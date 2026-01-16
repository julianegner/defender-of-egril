#!/bin/bash

# Build Windows EXE installer from bash
# This script can be run on any platform with JDK 11+ and Gradle

set -e

echo "Building Windows EXE installer..."
echo "This may take several minutes..."
echo ""

# Navigate to project root if running from scripts directory
if [ -d "../composeApp" ]; then
    cd ..
fi

# Run the Gradle task
./gradlew :composeApp:packageExe

echo ""
echo "Build complete!"
echo "The Windows EXE installer can be found at:"
echo "composeApp/build/compose/binaries/main/exe/"
echo ""
ls -lh composeApp/build/compose/binaries/main/exe/*.exe 2>/dev/null || echo "Note: EXE file will be available after a successful build"
