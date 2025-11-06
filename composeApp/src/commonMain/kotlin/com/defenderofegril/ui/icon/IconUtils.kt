package com.defenderofegril.ui.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.defenderofegril.model.DigOutcome
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.dig_outcome_brass
import defender_of_egril.composeapp.generated.resources.dig_outcome_diamond
import defender_of_egril.composeapp.generated.resources.dig_outcome_dragon
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_blue
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_green
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_red
import defender_of_egril.composeapp.generated.resources.dig_outcome_gold
import defender_of_egril.composeapp.generated.resources.dig_outcome_rubble
import defender_of_egril.composeapp.generated.resources.dig_outcome_silver
import defender_of_egril.composeapp.generated.resources.emoji_checkmark
import defender_of_egril.composeapp.generated.resources.emoji_door
import defender_of_egril.composeapp.generated.resources.emoji_down_arrow
import defender_of_egril.composeapp.generated.resources.emoji_explosion
import defender_of_egril.composeapp.generated.resources.emoji_heart
import defender_of_egril.composeapp.generated.resources.emoji_hole
import defender_of_egril.composeapp.generated.resources.emoji_info
import defender_of_egril.composeapp.generated.resources.emoji_left_arrow
import defender_of_egril.composeapp.generated.resources.emoji_lightning
import defender_of_egril.composeapp.generated.resources.emoji_lock
import defender_of_egril.composeapp.generated.resources.emoji_magnifying_glass
import defender_of_egril.composeapp.generated.resources.emoji_money
import defender_of_egril.composeapp.generated.resources.emoji_pick
import defender_of_egril.composeapp.generated.resources.emoji_pushpin
import defender_of_egril.composeapp.generated.resources.emoji_reload
import defender_of_egril.composeapp.generated.resources.emoji_save
import defender_of_egril.composeapp.generated.resources.emoji_sword
import defender_of_egril.composeapp.generated.resources.emoji_target
import defender_of_egril.composeapp.generated.resources.emoji_test_tube
import defender_of_egril.composeapp.generated.resources.emoji_timer
import defender_of_egril.composeapp.generated.resources.emoji_tools
import defender_of_egril.composeapp.generated.resources.emoji_trash
import defender_of_egril.composeapp.generated.resources.emoji_triangle_down
import defender_of_egril.composeapp.generated.resources.emoji_triangle_left
import defender_of_egril.composeapp.generated.resources.emoji_triangle_right
import defender_of_egril.composeapp.generated.resources.emoji_triangle_up
import defender_of_egril.composeapp.generated.resources.emoji_unlock
import defender_of_egril.composeapp.generated.resources.emoji_up_arrow
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random

/**
 * Displays a lightning bolt emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+26A1)
 */
@Composable
fun LightningIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_lightning),
        contentDescription = "Lightning",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a timer/stopwatch emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+23F1)
 */
@Composable
fun TimerIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 10.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_timer),
        contentDescription = "Timer",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a crossed swords emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2694)
 */
@Composable
fun SwordIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 14.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_sword),
        contentDescription = "Sword",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a heart emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2764)
 */
@Composable
fun HeartIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_heart),
        contentDescription = "Heart",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a reload/cycle emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F504)
 */
@Composable
fun ReloadIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_reload),
        contentDescription = "Reload",
        modifier = modifier.size(size)
    )
}

/**
 * Displays an explosion emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4A5)
 */
@Composable
fun ExplosionIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_explosion),
        contentDescription = "Explosion",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a test tube emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F9EA)
 */
@Composable
fun TestTubeIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_test_tube),
        contentDescription = "Test Tube",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a hole emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F573)
 */
@Composable
fun HoleIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_hole),
        contentDescription = "Hole",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a target emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F3AF)
 */
@Composable
fun TargetIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_target),
        contentDescription = "Target",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a pick/pickaxe emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+26CF)
 */
@Composable
fun PickIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_pick),
        contentDescription = "Pick",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a money bag emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4B0)
 */
@Composable
fun MoneyIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_money),
        contentDescription = "Money",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a triangle up emoji icon using Image for cross-platform compatibility
 * Source: Unicode (U+25B2)
 */
@Composable
fun TriangleUpIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_triangle_up),
        contentDescription = "Triangle Up",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a triangle right emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+25B6)
 */
@Composable
fun TriangleRightIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_triangle_right),
        contentDescription = "Triangle Right",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a triangle left  emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+25C0)
 */
@Composable
fun TriangleLeftIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_triangle_left),
        contentDescription = "Triangle Left",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a triangle down emoji icon using Image for cross-platform compatibility
 * Source: Unicode (U+25BC)
 */
@Composable
fun TriangleDownIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_triangle_down),
        contentDescription = "Triangle Down",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a trash can emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F5D1)
 */
@Composable
fun TrashIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_trash),
        contentDescription = "Trash",
        modifier = modifier.size(size)
    )
}

/**
 * Displays an info emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2139)
 */
@Composable
fun InfoIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_info),
        contentDescription = "Info",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a door emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F6AA)
 */
@Composable
fun DoorIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_door),
        contentDescription = "Door",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a pushpin emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4CD)
 */
@Composable
fun PushpinIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_pushpin),
        contentDescription = "Pushpin",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a left arrow icon using Image for cross-platform compatibility
 * Source: Unicode (U+2190)
 */
@Composable
fun LeftArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_left_arrow),
        contentDescription = "Left Arrow",
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) },
        modifier = modifier.size(size)
    )
}

/**
 * Displays an up arrow icon using Image for cross-platform compatibility
 * Source: Unicode (U+2191)
 */
@Composable
fun UpArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_up_arrow),
        contentDescription = "Up Arrow",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a down arrow icon using Image for cross-platform compatibility
 * Source: Unicode (U+2193)
 */
@Composable
fun DownArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_down_arrow),
        contentDescription = "Down Arrow",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a checkmark icon using Image for cross-platform compatibility
 * Source: Unicode (U+2713)
 */
@Composable
fun CheckmarkIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_checkmark),
        contentDescription = "Checkmark",
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) },
        modifier = modifier.size(size)
    )
}

/**
 * Displays a tools/hammer and wrench emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F6E0)
 */
@Composable
fun ToolsIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_tools),
        contentDescription = "Tools",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a lock emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F512)
 */
@Composable
fun LockIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_lock),
        contentDescription = "Lock",
        modifier = modifier.size(size)
    )
}

/**
 * Displays an unlock emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F513)
 */
@Composable
fun UnlockIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_unlock),
        contentDescription = "Unlock",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a magnifying glass icon using Image for cross-platform compatibility
 * Source: Generic SVG icon
 */
@Composable
fun MagnifyingGlassIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_magnifying_glass),
        contentDescription = "Magnifying Glass",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a floppy disk/save icon using Image for cross-platform compatibility
 * Source: Custom SVG icon (U+1F4BE floppy disk emoji equivalent)
 */
@Composable
fun SaveIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_save),
        contentDescription = "Save",
        modifier = modifier.size(size)
    )
}

/**
 * Displays an icon for a dig outcome from mining
 *
 * Note: For GEMS outcome, the gem color is randomly selected (red, green, or blue)
 * on each composition. This is intentional per requirements to add visual variety.
 * The color may change on recomposition but this is acceptable as it doesn't affect
 * game state - only the visual representation.
 */
@Composable
fun DigOutcomeIcon(
    outcome: DigOutcome,
    modifier: Modifier = Modifier.Companion,
    size: Dp = 64.dp
) {
    val resource = when (outcome) {
        DigOutcome.NOTHING -> Res.drawable.dig_outcome_rubble
        DigOutcome.BRASS -> Res.drawable.dig_outcome_brass
        DigOutcome.SILVER -> Res.drawable.dig_outcome_silver
        DigOutcome.GOLD -> Res.drawable.dig_outcome_gold
        DigOutcome.GEMS -> {
            // Randomly select gem color (red, green, or blue) as per requirements
            // This adds visual variety and doesn't affect game state
            when (Random.Default.nextInt(3)) {
                0 -> Res.drawable.dig_outcome_gem_red
                1 -> Res.drawable.dig_outcome_gem_green
                else -> Res.drawable.dig_outcome_gem_blue
            }
        }
        DigOutcome.DIAMOND -> Res.drawable.dig_outcome_diamond
        DigOutcome.DRAGON -> Res.drawable.dig_outcome_dragon
    }

    Image(
        painter = painterResource(resource),
        contentDescription = outcome.displayName,
        modifier = modifier.size(size)
    )
}
