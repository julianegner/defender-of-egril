# Code Refactoring Analysis - GamePlayScreen Components

## Overview
Analysis of the extracted GamePlayScreen components to identify duplicate code patterns and refactoring opportunities.

## 1. Duplicate Patterns Identified

### 1.1 Expandable Card Pattern ⭐ HIGH PRIORITY
**Location**: `GameLegend.kt` - Used in both `GameLegend` and `EnemyListPanel`

**Duplicate Code**:
```kotlin
// Pattern repeated twice in GameLegend.kt
var isExpanded by remember { mutableStateOf(false) }

Card(modifier = modifier) {
    Column(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (isExpanded) {
                TriangleDownIcon(size = 20.dp)
            } else {
                TriangleLeftIcon(size = 20.dp)
            }
        }
        
        if (isExpanded) {
            // Content
        }
    }
}
```

**Recommendation**: Extract to reusable `ExpandableCard` composable
- Reduces ~40 lines of duplicate code
- Makes the pattern consistent across the app
- Easier to maintain and update expand/collapse behavior

### 1.2 Icon + Text Row Pattern ⭐ HIGH PRIORITY
**Location**: Multiple files - `GameHeader.kt`, `DefenderButtons.kt`, `DefenderInfo.kt`

**Duplicate Code**:
```kotlin
// Repeated 15+ times across files
Row(verticalAlignment = Alignment.CenterVertically) {
    SomeIcon(size = 12.dp)
    Spacer(modifier = Modifier.width(4.dp))
    Text("value", style = MaterialTheme.typography.bodySmall)
}
```

**Specific Instances**:
- `GameHeader.kt` - Coins, HP, Turn display (6 instances: 3 in expanded, 3 in compact)
- `DefenderButtons.kt` - TowerStats function (3 instances)
- `DefenderInfo.kt` - Uses TowerStats (2 instances)

**Recommendation**: Extract to reusable `IconTextRow` composable
- Reduces ~30-40 lines of duplicate code
- Consistent spacing and styling
- Single place to update icon-text patterns

### 1.3 Game Stats Display Pattern ⭐ MEDIUM PRIORITY
**Location**: `GameHeader.kt` - Stats shown in both `ExpandedGameHeader` and `CompactGameHeader`

**Duplicate Code**:
```kotlin
// Stats display duplicated with different icon sizes
Row(verticalAlignment = Alignment.CenterVertically) {
    MoneyIcon(size = 20.dp) // or 16.dp in compact
    Spacer(modifier = Modifier.width(4.dp))
    Text("${gameState.coins.value}", ...)
}
Row(verticalAlignment = Alignment.CenterVertically) {
    HeartIcon(size = 20.dp) // or 16.dp in compact
    Spacer(modifier = Modifier.width(4.dp))
    Text("${gameState.healthPoints.value}", ...)
}
Row(verticalAlignment = Alignment.CenterVertically) {
    ReloadIcon(size = 18.dp) // or 14.dp in compact
    Spacer(modifier = Modifier.width(4.dp))
    Text("Turn ${gameState.turnNumber.value}", ...)
}
```

**Recommendation**: Extract stats display to separate composable
- Reduces ~20-30 lines of duplicate code
- Can parameterize icon sizes for compact vs expanded
- Reusable across different screens if needed

### 1.4 Tower Stats Display ⭐ LOW PRIORITY
**Location**: `DefenderButtons.kt` and `DefenderInfo.kt`

**Current State**: Already well-abstracted with `TowerStats` function
- Used in 3 places consistently
- No refactoring needed - this is good practice

## 2. Code Organization Opportunities

### 2.1 Common UI Components File
**Recommendation**: Create `ui/gameplay/CommonComponents.kt`

**Should contain**:
- `ExpandableCard` - Reusable expandable card pattern
- `IconTextRow` - Icon + spacer + text pattern
- `StatRow` - Game stat display (coins, HP, turn)

**Benefits**:
- Central location for shared UI patterns
- Reduces duplication across 3-4 files
- Easier to maintain consistent styling

### 2.2 Constants Extraction
**Location**: Multiple files with hardcoded values

**Examples**:
- Icon sizes: `12.dp`, `14.dp`, `16.dp`, `20.dp`
- Spacer widths: `4.dp`, `8.dp`
- Padding values: `8.dp`, `12.dp`, `16.dp`
- Colors: `Color(0xFF4CAF50)`, `Color(0xFFFF9800)`, etc.

**Recommendation**: Create `ui/gameplay/GamePlayConstants.kt`
```kotlin
object GamePlayConstants {
    object IconSizes {
        val Small = 12.dp
        val Medium = 16.dp
        val Large = 20.dp
    }
    
    object Spacing {
        val IconText = 4.dp
        val Items = 8.dp
        val Sections = 12.dp
    }
    
    object Colors {
        val Success = Color(0xFF4CAF50)
        val Warning = Color(0xFFFF9800)
        val Error = Color(0xFFF44336)
        val Info = Color(0xFF2196F3)
    }
}
```

## 3. Other Refactoring Opportunities

### 3.1 GameHeader Duplication
**Issue**: Stats display code duplicated between `ExpandedGameHeader` and `CompactGameHeader`

**Solution**: Extract to `GameStatsDisplay` composable with size parameter
```kotlin
@Composable
fun GameStatsDisplay(
    gameState: GameState,
    compact: Boolean = false,
    onCheatCode: (() -> Unit)?
)
```

### 3.2 Button Styling Consistency
**Issue**: Button styling slightly inconsistent across files

**Solution**: Create button style helper functions
```kotlin
@Composable
fun PrimaryActionButton(...)
@Composable
fun SecondaryActionButton(...)
@Composable
fun CompactButton(...)
```

## 4. Estimated Impact

### Lines of Code Reduction
- ExpandableCard extraction: ~40 lines
- IconTextRow extraction: ~30-40 lines
- GameStatsDisplay extraction: ~20-30 lines
- Constants extraction: Organization improvement
- **Total: 90-110 lines reduction** (3-4% of current gameplay module)

### Maintainability Improvements
- Single source of truth for common patterns
- Easier to update styling consistently
- Reduced risk of inconsistencies
- Better code reusability

## 5. Recommended Implementation Order

### Phase 1: High Impact, Low Risk
1. ✅ **Extract IconTextRow composable** - Used 15+ times, simple pattern
2. ✅ **Extract ExpandableCard composable** - Used 2 times, clear pattern

### Phase 2: Organization
3. **Create GamePlayConstants** - Improves consistency
4. **Extract GameStatsDisplay** - Reduces header duplication

### Phase 3: Polish (Optional)
5. **Button style helpers** - If further consistency needed
6. **Additional constant extraction** - As patterns emerge

## 6. Files That Would Change

### New Files
- `ui/gameplay/CommonComponents.kt` - Shared UI components
- `ui/gameplay/GamePlayConstants.kt` - Constants (optional)

### Modified Files
- `ui/gameplay/GameLegend.kt` - Use ExpandableCard
- `ui/gameplay/GameHeader.kt` - Use IconTextRow, GameStatsDisplay
- `ui/gameplay/DefenderButtons.kt` - Use IconTextRow (via TowerStats)
- `ui/gameplay/DefenderInfo.kt` - Use IconTextRow (via TowerStats)

## 7. Non-Gameplay Duplication Opportunities

### WorldMapScreen.kt (403 lines)
Could benefit from similar expandable card patterns if it has them.

### LevelEditorScreen.kt (1,645 lines) ⚠️ LARGE FILE
Should be analyzed separately - potentially the next refactoring target after gameplay components.

### LoadGameScreen.kt (406 lines)
May have card/list patterns that could benefit from shared components.

## Conclusion

The gameplay components are already well-organized with good separation of concerns. The main opportunities are:

1. **Extracting common UI patterns** (ExpandableCard, IconTextRow) - Clear win
2. **Constants organization** - Improves consistency
3. **Header stats duplication** - Small improvement

These changes would reduce duplication while maintaining the clean architecture already established.
