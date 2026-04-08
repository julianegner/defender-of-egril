#!/usr/bin/env python3
"""
Script to replace Text( with SelectableText( in Compose UI files,
keeping Text( inside buttons, tabs, dropdownmenu items, placeholder/label params.
"""

import re
import sys
from pathlib import Path

# Contexts where Text( should remain as Text(
EXCLUDED_CONTEXTS = [
    'Button',
    'TextButton',
    'OutlinedButton',
    'ElevatedButton',
    'FilledTonalButton',
    'IconButton',
    'Tab',
    'DropdownMenuItem',
    # For parameter contexts like placeholder = { Text(...) } and label = { Text(...) }
    # we handle these separately
]

def find_text_calls_in_file(content):
    """
    Find all positions of Text( in the file and determine if each should be replaced.
    Returns list of (start_pos, should_replace) tuples.
    """
    results = []
    i = 0
    n = len(content)
    
    while i < n:
        # Find next Text( occurrence
        pos = content.find('Text(', i)
        if pos == -1:
            break
        
        # Check if it's actually 'Text(' and not something like 'SelectableText(' or 'SomeText('
        # It should be preceded by whitespace, newline, or specific chars
        before = content[pos-1] if pos > 0 else ''
        if before not in (' ', '\n', '\t', '{', ',', '(', '='):
            i = pos + 1
            continue
        
        # Check it's not SelectableText( (already replaced)
        if pos >= 8 and content[pos-8:pos] == 'Selectable':
            i = pos + 1
            continue
            
        # Determine if this Text( should be replaced by looking at the enclosing context
        should_replace = not is_in_excluded_context(content, pos)
        results.append((pos, should_replace))
        i = pos + 1
    
    return results


def is_in_excluded_context(content, text_pos):
    """
    Check if the Text( at text_pos is inside an excluded context.
    We look backwards from text_pos to find enclosing constructs.
    """
    # Look back to find what surrounds this Text(
    # We need to track brace nesting
    
    # Check for parameter patterns: placeholder = { Text( or label = { Text(
    # Look backwards for `= {` pattern within recent lines
    recent_content = content[max(0, text_pos-200):text_pos]
    
    # Check for label = { or placeholder = { immediately before
    param_pattern = re.search(r'\b(placeholder|label)\s*=\s*\{[^}]*$', recent_content)
    if param_pattern:
        return True
    
    # Also check for text = { ... } in Tab context
    # Tab( ... text = { Text( ...
    
    # Now check for enclosing button/tab contexts
    # We need to find the innermost enclosing lambda block
    # and check if it's a button/tab/etc.
    
    # Scan backwards tracking brace nesting
    depth = 0
    pos = text_pos - 1
    
    while pos >= 0:
        c = content[pos]
        if c == '}':
            depth += 1
        elif c == '{':
            if depth == 0:
                # This is the opening brace of the lambda that contains our Text(
                # Look before this brace to find the context name
                context_before = content[max(0, pos-100):pos]
                # Check if this matches any excluded context
                for ctx in EXCLUDED_CONTEXTS:
                    # Pattern: ContextName( ... ) { or ContextName { 
                    # Look for the pattern: word followed by potential args then {
                    if re.search(rf'\b{ctx}\s*\(', context_before) or re.search(rf'\b{ctx}\s*$', context_before.rstrip()):
                        return True
                    
                # Also check for the `text = {` parameter pattern (Tab label)
                if re.search(r'\btext\s*=\s*$', context_before.rstrip()):
                    return True
                    
                # Check for `label = {` pattern
                if re.search(r'\blabel\s*=\s*$', context_before.rstrip()):
                    return True
                    
                # Check for `placeholder = {` pattern  
                if re.search(r'\bplaceholder\s*=\s*$', context_before.rstrip()):
                    return True
                    
                # Go up one more level
                depth = 0
            else:
                depth -= 1
        pos -= 1
    
    return False


def process_file(filepath):
    """
    Process a single file and return (modified_content, num_changes).
    """
    content = Path(filepath).read_text(encoding='utf-8')
    original_content = content
    
    # Get package name
    pkg_match = re.search(r'^package\s+([\w.]+)', content, re.MULTILINE)
    package = pkg_match.group(1) if pkg_match else ''
    
    # Check if already has the import
    has_import = 'import de.egril.defender.ui.common.SelectableText' in content
    is_common_pkg = package == 'de.egril.defender.ui.common'
    
    # Find all Text( positions and whether to replace
    replacements = find_text_calls_in_file(content)
    
    # Apply replacements in reverse order to preserve positions
    changes = 0
    new_content = list(content)
    
    for pos, should_replace in reversed(replacements):
        if should_replace:
            # Replace 'Text(' with 'SelectableText('
            new_content[pos:pos+5] = list('SelectableText(')
            changes += 1
    
    content = ''.join(new_content)
    
    # Add import if needed
    if changes > 0 and not has_import and not is_common_pkg:
        # Find where to insert the import (alphabetically)
        import_to_add = 'import de.egril.defender.ui.common.SelectableText'
        
        # Find existing imports to insert alphabetically
        import_lines = list(re.finditer(r'^import .+$', content, re.MULTILINE))
        
        if import_lines:
            # Find the right position alphabetically
            insert_after = None
            for match in import_lines:
                if match.group() < import_to_add:
                    insert_after = match
            
            if insert_after:
                # Insert after this import
                insert_pos = insert_after.end()
                content = content[:insert_pos] + '\n' + import_to_add + content[insert_pos:]
            else:
                # Insert before the first import
                first_import = import_lines[0]
                insert_pos = first_import.start()
                content = content[:insert_pos] + import_to_add + '\n' + content[insert_pos:]
        else:
            # No imports found, add after package declaration
            pkg_match = re.search(r'^package .+$', content, re.MULTILINE)
            if pkg_match:
                insert_pos = pkg_match.end()
                content = content[:insert_pos] + '\n\n' + import_to_add + content[insert_pos:]
    
    return content, changes


files = [
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/CommitInfoDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/PlatformInfoDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/NewVersionDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/MissingRepositoryDataDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/NewRepositoryDataDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/PlayerDialogs.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/CheatCodeDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/AchievementNotificationDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/RulesScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/FinalCreditsScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/StickerScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/ApplicationBanner.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/Tooltip.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/MenuScreens.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/UnitIcons.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/HowToPlayContent.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/InstallationInfo.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/InfoPageScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/LicenseInfo.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/AudioLicensesInfo.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/BackendInfo.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/DownloadInfo.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/KeyboardShortcutsInfo.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/ImpressumWrapper.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/infopage/PlayerProfileScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/DefenderInfo.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/AttackerInfo.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/BarricadeInfoPanel.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameLegend.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/NarrativeMessageDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/TutorialOverlay.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/CommonComponents.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameDialogs.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameHeader.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameMap.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameControls.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/ActionButtons.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/DefenderButtons.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/MagicPanel.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GamePlayScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/LevelLoadingScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/SettingsDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/SettingsHintBox.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/DifficultyChooser.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/DifficultyDisplay.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/GenericSwitch.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/DualLabelSwitch.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/InitialLanguageChooserDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/settings/LanguageChooser.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/LoadGameScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/DeleteConfirmationDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/ImportExportDialogs.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/SavedGameCardComponents.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/WorldMapConflictDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/worldmap/LevelLocationDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/worldmap/LevelCard.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/worldmap/LevelCardOverlay.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/worldmap/WorldMapScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/worldmap/EditorButtonCard.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/EditorDialogs.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/EditorHowToContent.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/EditorInfoPage.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/OfficialDataChangedDialog.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/TileUtils.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/LevelEditor.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/LevelEditorComponents.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/LevelEditorScreen.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/LevelInfoTab.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/LevelSequence.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/enemies/EnemySpawnsTab.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/initialsetup/InitialSetupSidebar.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/initialsetup/InitialSetupTab.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/tower/TowersTab.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/waypoint/WaypointChainNode.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/waypoint/WaypointConnectionCard.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/waypoint/WaypointTreeView.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/waypoint/WaypointsTab.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/map/MapEditorContent.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/map/MapEditorHeader.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/map/MapEditorView.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/map/MapListCard.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/map/MapSelectionCard.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/worldmap/WorldMapPositionEditor.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/common/LevelInfoEnemiesColumn.kt",
    "frontend/composeApp/src/commonMain/kotlin/de/egril/defender/ui/animations/AnimationTestScreen.kt",
]

base_dir = Path("/home/runner/work/defender-of-egril-fork/defender-of-egril-fork")

total_changes = 0
for rel_path in files:
    filepath = base_dir / rel_path
    if not filepath.exists():
        print(f"MISSING: {rel_path}")
        continue
    
    content, changes = process_file(filepath)
    
    if changes > 0:
        filepath.write_text(content, encoding='utf-8')
        print(f"Modified ({changes} changes): {rel_path}")
        total_changes += changes
    else:
        print(f"No changes: {rel_path}")

print(f"\nTotal: {total_changes} Text( replaced with SelectableText(")
