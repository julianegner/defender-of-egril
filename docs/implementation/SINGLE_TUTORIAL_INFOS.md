# Single Tutorial Infos - Implementation Details

## Overview

The Single Tutorial Infos system provides a unified, reusable framework for displaying one-time informational dialogs during gameplay. This system separates standalone info popups from the sequential tutorial flow, allowing for better code organization and easier addition of new info types.

## Architecture

### InfoType Enum

Defines the types of single tutorial infos that can be displayed:

```kotlin
enum class InfoType {
    DRAGON_INFO,        // Dragon behavior explanation
    GREED_INFO,         // Dragon greed explanation (greed > 0)
    VERY_GREEDY_INFO,   // Dragon very greedy explanation (greed > 5)
    MINE_WARNING,       // Mine under threat from dragon
    NONE                // No info to show
}
```

### InfoState Data Class

Manages the state of single tutorial infos:

```kotlin
data class InfoState(
    val currentInfo: InfoType = InfoType.NONE,
    val seenInfos: Set<InfoType> = emptySet(),
    val mineWarningId: Int? = null  // For mine warnings, track which mine
)
```

**Key Features:**
- Tracks currently displayed info
- Maintains set of seen infos to prevent re-showing
- Stores mine ID for mine warnings
- Immutable state with helper methods

**Methods:**
- `shouldShowOverlay()`: Check if overlay should be displayed
- `hasSeen(type)`: Check if a specific info has been seen
- `showInfo(type, mineId?)`: Display a specific info
- `dismissInfo()`: Dismiss current info and mark as seen
- `clearSeenInfos()`: Reset seen infos (for testing)

## Integration Points

### GameState

Added `infoState: MutableState<InfoState>` to manage single tutorial infos:

```kotlin
val infoState: MutableState<InfoState> = mutableStateOf(InfoState())
```

**Note:** The `mineWarnings` SnapshotStateList is maintained in GameState as it's used by the GameEngine to track which mines are under threat. The InfoState system reads from this list to display warnings.

### TutorialOverlay

Updated to support both sequential tutorials and single infos:

**Parameters:**
- `currentInfo: InfoType = InfoType.NONE`
- `onDismissInfo: (() -> Unit)? = null`

**Priority System:**
1. Single info (if currentInfo != NONE)
2. Sequential tutorial (if currentStep != NONE)

### GamePlayScreen

**Unified LaunchedEffect for Dragon Infos:**

```kotlin
LaunchedEffect(gameState.attackers.size, gameState.infoState.value) {
    val infoState = gameState.infoState.value
    
    // Skip if already showing an info
    if (infoState.currentInfo != InfoType.NONE) return@LaunchedEffect
    
    val dragons = gameState.attackers.filter { it.type.isDragon && !it.isDefeated.value }
    
    if (dragons.isNotEmpty()) {
        // Priority: Very greedy > Greed > Dragon info
        when {
            dragons.any { it.greed > 5 } && !infoState.hasSeen(InfoType.VERY_GREEDY_INFO) -> {
                gameState.infoState.value = infoState.showInfo(InfoType.VERY_GREEDY_INFO)
            }
            dragons.any { it.greed > 0 } && !infoState.hasSeen(InfoType.GREED_INFO) -> {
                gameState.infoState.value = infoState.showInfo(InfoType.GREED_INFO)
            }
            !infoState.hasSeen(InfoType.DRAGON_INFO) -> {
                gameState.infoState.value = infoState.showInfo(InfoType.DRAGON_INFO)
            }
        }
    }
}
```

**Mine Warning LaunchedEffect:**

```kotlin
LaunchedEffect(gameState.mineWarnings.size, gameState.infoState.value) {
    val infoState = gameState.infoState.value
    
    // Skip if already showing an info
    if (infoState.currentInfo != InfoType.NONE) return@LaunchedEffect
    
    if (gameState.mineWarnings.isNotEmpty()) {
        val mineId = gameState.mineWarnings.first()
        gameState.infoState.value = infoState.showInfo(InfoType.MINE_WARNING, mineId)
    }
}
```

**Overlay Display:**

```kotlin
TutorialOverlay(
    currentStep = gameState.tutorialState.value.currentStep,
    isNextEnabled = gameState.tutorialState.value.isNextEnabled(),
    onNext = { /* tutorial next logic */ },
    onSkip = { /* tutorial skip logic */ },
    currentInfo = gameState.infoState.value.currentInfo,
    onDismissInfo = {
        val currentInfoState = gameState.infoState.value
        val dismissedInfo = currentInfoState.dismissInfo()
        gameState.infoState.value = dismissedInfo
        
        // Remove mine warning from the list if it was a mine warning
        if (currentInfoState.currentInfo == InfoType.MINE_WARNING) {
            currentInfoState.mineWarningId?.let { gameState.mineWarnings.remove(it) }
        }
    }
)
```

## Info Types

### Dragon Info

**Trigger:** First time a dragon appears (not defeated)

**Content:**
- Dragon movement pattern (walk/fly alternation)
- Dragon eating behavior (devours enemies)
- Dragon level system (HP ÷ 500)

### Greed Info

**Trigger:** Dragon greed > 0 (first time)

**Content:**
- Greed mechanic explanation
- Adjacent unit devouring behavior

**Priority:** Shown after Dragon Info if both conditions are met

### Very Greedy Info

**Trigger:** Dragon greed > 5 (first time)

**Content:**
- Very greedy behavior (mine destruction)
- Mine targeting and health absorption
- Defense recommendations

**Priority:** Highest priority, shown before Greed Info if both conditions are met

### Mine Warning

**Trigger:** Dragon can reach a mine next turn (from GameEngine)

**Content:**
- Warning that mine is under threat
- Dwarves preparing to defend

**Special Behavior:**
- Can show multiple times (one per mine)
- Tracks specific mine ID
- Removed from queue when dismissed

## Testing

Comprehensive test coverage in `InfoStateTest.kt`:

**Test Cases:**
- Initial state verification
- Info display and dismissal
- Multiple sequential infos
- Seen tracking
- Mine warning ID handling
- Clear seen infos functionality
- Edge cases (dismiss without showing, repeated shows)

**All tests passing:**
- Unit tests for InfoState logic
- Compilation tests for Android and Desktop platforms
- Integration with existing tutorial system

## Benefits

### Code Organization
- Single, consistent system for all tutorial infos
- Reduced code duplication
- Clearer separation of concerns

### Maintainability
- Easy to add new info types
- Centralized state management
- Better testability

### User Experience
- Consistent info display behavior
- Prevents re-showing seen infos
- Priority system ensures important infos shown first

## Future Enhancements

### Potential Additions
- Additional info types for new game mechanics
- Configurable priority system
- Info queuing for multiple simultaneous triggers
- Persistent storage of seen infos across sessions
- Analytics tracking for info effectiveness

## Related Files

**Model:**
- `composeApp/src/commonMain/kotlin/de/egril/defender/model/InfoState.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/model/GameState.kt`

**UI:**
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/TutorialOverlay.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GamePlayScreen.kt`

**Tests:**
- `composeApp/src/commonTest/kotlin/de/egril/defender/model/InfoStateTest.kt`

**Documentation:**
- `docs/implementation/SINGLE_TUTORIAL_INFOS.md` (this file)
- `docs/implementation/TUTORIAL_IMPLEMENTATION.md` (sequential tutorial documentation)
