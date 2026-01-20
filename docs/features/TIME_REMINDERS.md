# Time-Based Reminder Messages

## Overview

This feature adds health-promoting reminder messages to encourage players to take breaks and get adequate sleep.

## Features

### Break Reminder
- **Trigger**: Appears every 2 hours of gameplay
- **Message**: "Time for a break? You have been playing for [time]."
- **Icon**: Coffee cup emoji (☕)
- **Format**: Shows elapsed time in hours and minutes (e.g., "2 hours 30 minutes")

### Sleep Reminder
- **Trigger**: Appears hourly after 23:00 local time (until 06:00)
- **Message**: "Time for Sleep? It is [close to midnight/midnight/after midnight]."
- **Icon**: Bed emoji (🛏️)
- **Time-based Messages**:
  - 23:00 - "It is close to midnight."
  - 00:00 - "It is midnight."
  - 01:00-05:59 - "It is after midnight."

## Implementation Details

### Components

1. **Icons** (`ui/icon/IconUtils.kt`)
   - `CoffeeIcon`: Coffee cup emoji from Noto Emoji (U+2615)
   - `BedIcon`: Bed emoji from Noto Emoji (U+1F6CF)

2. **Dialog** (`ui/gameplay/GameDialogs.kt`)
   - `ReminderDialog`: Displays reminder with icon and message
   - `ReminderType`: Enum for BREAK and SLEEP types

3. **Time Tracking** (`ui/GameViewModel.kt`)
   - `gameSessionStartTime`: Tracks when gameplay started
   - `lastBreakReminderTime`: Tracks last break reminder
   - `lastSleepReminderTime`: Tracks last sleep reminder
   - `checkTimeReminders()`: Coroutine that checks every minute

4. **Platform Support** (`utils/TimeUtils.*`)
   - `getLocalHour()`: Platform-specific function for local time
   - Implemented for JVM, iOS, and WASM platforms

### Localization

Strings are available in 5 languages:
- English (default)
- German (Deutsch)
- Spanish (Español)
- French (Français)
- Italian (Italiano)

String keys:
- `time_for_break_title`
- `time_for_break_message`
- `time_for_sleep_title`
- `time_for_sleep_close_to_midnight`
- `time_for_sleep_midnight`
- `time_for_sleep_after_midnight`
- `hours` / `hour`
- `minutes` / `minute`

## Testing

### Cheat Codes

To test reminders without waiting:

1. **Break Reminder**:
   - Click on coins display to open cheat code dialog
   - Enter: `break` or `breakreminder`
   - Shows break reminder with current session time

2. **Sleep Reminder**:
   - Click on coins display to open cheat code dialog
   - Enter: `sleep` or `sleepreminder`
   - Shows sleep reminder based on current local hour

### Manual Testing

1. **Break Reminder**:
   - Start a new game or load a saved game
   - Wait 2 hours (or use cheat code)
   - Verify dialog appears with coffee icon and correct time
   - Click OK to dismiss
   - Wait another 2 hours for next reminder

2. **Sleep Reminder**:
   - Play during or after 23:00 local time
   - Wait 1 hour (or use cheat code)
   - Verify dialog appears with bed icon and time-appropriate message
   - Click OK to dismiss
   - Verify reminder appears again after 1 hour

3. **Localization**:
   - Change language in Settings
   - Trigger reminders via cheat codes
   - Verify messages appear in selected language

## Technical Notes

- Time tracking starts when a level is loaded (new or saved game)
- Time tracking stops when navigating away from gameplay screen
- Reminders use a coroutine that checks every 60 seconds
- The system uses platform-specific time functions for accuracy
- All strings follow the project's localization requirements
- Icons are PNG images loaded via Compose resources

## Future Enhancements

Possible improvements:
- Make reminder intervals configurable in Settings
- Add "Remind me later" option (snooze for 30 minutes)
- Track total play time across sessions
- Add achievement for taking regular breaks
- Option to disable reminders in Settings
