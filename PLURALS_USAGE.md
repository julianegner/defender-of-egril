# Plurals Usage Guide

## Overview
With version 2.0.0 of the com.hyperether.localization library, plural string resources are now supported. This allows for grammatically correct messages based on quantity.

## Available Plural Resources

### Defined Plurals
- `enemy_count` - For displaying enemy counts ("1 enemy" vs "2 enemies")
- `turn_count` - For displaying turn counts ("1 turn" vs "2 turns")  
- `coin_count` - For displaying coin amounts ("1 coin" vs "2 coins")
- `health_point_count` - For displaying health points ("1 health point" vs "2 health points")

## Usage in Composables

### Basic Usage
```kotlin
import com.hyperether.resources.Res
import com.hyperether.resources.pluralStringResource

@Composable
fun EnemyCountDisplay(count: Int) {
    Text(pluralStringResource(Res.plurals.enemy_count, count, count))
}
```

### Outside Composables
```kotlin
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.Res

fun getEnemyCountText(count: Int): String {
    return LocalizedStrings.getPlural(Res.plurals.enemy_count, count, count)
}
```

## Examples

### English (Default)
- `pluralStringResource(Res.plurals.enemy_count, 1, 1)` → "1 enemy"
- `pluralStringResource(Res.plurals.enemy_count, 5, 5)` → "5 enemies"
- `pluralStringResource(Res.plurals.turn_count, 1, 1)` → "1 turn"
- `pluralStringResource(Res.plurals.coin_count, 100, 100)` → "100 coins"

### German (DE)
- `pluralStringResource(Res.plurals.enemy_count, 1, 1)` → "1 Feind"
- `pluralStringResource(Res.plurals.enemy_count, 5, 5)` → "5 Feinde"
- `pluralStringResource(Res.plurals.turn_count, 1, 1)` → "1 Runde"
- `pluralStringResource(Res.plurals.coin_count, 100, 100)` → "100 Münzen"

### Spanish (ES)
- `pluralStringResource(Res.plurals.enemy_count, 1, 1)` → "1 enemigo"
- `pluralStringResource(Res.plurals.enemy_count, 5, 5)` → "5 enemigos"

### French (FR)
- `pluralStringResource(Res.plurals.enemy_count, 1, 1)` → "1 ennemi"
- `pluralStringResource(Res.plurals.enemy_count, 5, 5)` → "5 ennemis"

### Italian (IT)
- `pluralStringResource(Res.plurals.enemy_count, 1, 1)` → "1 nemico"
- `pluralStringResource(Res.plurals.enemy_count, 5, 5)` → "5 nemici"

## XML Definition Format

Plurals are defined in `strings.xml` files using the `<plurals>` tag:

```xml
<plurals name="enemy_count">
    <item quantity="one">%d enemy</item>
    <item quantity="other">%d enemies</item>
</plurals>
```

Available quantity values:
- `zero` - Used for zero in some languages
- `one` - Singular form
- `two` - Used for two in some languages (Arabic, Slavic languages)
- `few` - Used for few in some languages (Slavic languages)
- `many` - Used for many in some languages (Slavic languages)
- `other` - Default/plural form (required)

## Integration Points

Consider using plurals in these areas of the game:
1. Enemy spawning messages ("3 enemies appear")
2. Turn counter displays ("Turn 5 of 10")
3. Coin rewards ("You earned 50 coins")
4. Health point displays ("10 health points remaining")
5. Save game descriptions ("Saved on turn 15 with 3 enemies remaining")
6. Level completion messages ("Defeated 25 enemies in 20 turns")

## Migration Notes

When upgrading from version 1.1.1 to 2.0.0:
1. Remove any manual plural handling logic (e.g., `count == 1 ? "enemy" : "enemies"`)
2. Replace with `pluralStringResource()` calls
3. Ensure all language files include matching plural definitions
4. Test with various quantities to verify correct grammar
