package de.egril.defender.ui.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.egril.defender.model.DigOutcome
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.dig_outcome_brass
import defender_of_egril.composeapp.generated.resources.dig_outcome_diamond
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_blue
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_green
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_red
import defender_of_egril.composeapp.generated.resources.dig_outcome_gold
import defender_of_egril.composeapp.generated.resources.dig_outcome_rubble
import defender_of_egril.composeapp.generated.resources.dig_outcome_silver
import defender_of_egril.composeapp.generated.resources.dragon_destroying_mine
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
import defender_of_egril.composeapp.generated.resources.emoji_warning
import defender_of_egril.composeapp.generated.resources.emoji_wood
import defender_of_egril.composeapp.generated.resources.emoji_right_arrow
import defender_of_egril.composeapp.generated.resources.emoji_red_circle
import defender_of_egril.composeapp.generated.resources.emoji_map
import defender_of_egril.composeapp.generated.resources.emoji_number_1
import defender_of_egril.composeapp.generated.resources.emoji_number_2
import defender_of_egril.composeapp.generated.resources.emoji_speaker_low
import defender_of_egril.composeapp.generated.resources.emoji_speaker_high
import defender_of_egril.composeapp.generated.resources.emoji_coffee
import defender_of_egril.composeapp.generated.resources.emoji_bed
import defender_of_egril.composeapp.generated.resources.emoji_plus
import defender_of_egril.composeapp.generated.resources.emoji_cross
import defender_of_egril.composeapp.generated.resources.emoji_pencil
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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
 * Displays a coffee cup emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2615)
 */
@Composable
fun CoffeeIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_coffee),
        contentDescription = "Coffee",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a bed emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F6CF)
 */
@Composable
fun BedIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_bed),
        contentDescription = "Bed",
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
        DigOutcome.DRAGON -> Res.drawable.dragon_destroying_mine
    }

    // Dragon image is displayed at double size (2x in both dimensions)
    val displaySize = if (outcome == DigOutcome.DRAGON) size * 2 else size

    Image(
        painter = painterResource(resource),
        contentDescription = outcome.displayName,
        modifier = modifier.size(displaySize)
    )
}

/**
 * Displays a warning triangle icon using Image for cross-platform compatibility
 * Source: Custom icon (U+26A0)
 */
@Composable
fun WarningIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_warning),
        contentDescription = "Warning",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a right arrow icon using Image for cross-platform compatibility
 * Source: Custom icon (U+2192)
 */
@Composable
fun RightArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_right_arrow),
        contentDescription = "Right Arrow",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a red circle icon using Image for cross-platform compatibility
 * Used to indicate circular dependencies or errors
 */
@Composable
fun RedCircleIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 12.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_red_circle),
        contentDescription = "Error",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a map icon using Image for cross-platform compatibility
 * Source: Custom icon (U+1F5FA)
 */
@Composable
fun MapIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_map),
        contentDescription = "Map",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a number 1 icon using Image for cross-platform compatibility
 */
@Composable
fun Number1Icon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_number_1),
        contentDescription = "1",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a number 2 icon using Image for cross-platform compatibility
 */
@Composable
fun Number2Icon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_number_2),
        contentDescription = "2",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a pentagram (5-pointed star) icon using Canvas for magical traps
 */
@Composable
fun PentagramIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
    color: Color = Color(0xFFAA00FF)  // Purple/magenta color for magical trap
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(size)
    ) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val radius = minOf(canvasWidth, canvasHeight) / 2f * 0.9f
        
        // Calculate the 5 points of the star
        val points = mutableListOf<androidx.compose.ui.geometry.Offset>()
        for (i in 0 until 5) {
            val angle = (i * 72 - 90) * (PI / 180.0)  // Convert degrees to radians
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            points.add(androidx.compose.ui.geometry.Offset(x, y))
        }
        
        // Draw the pentagram by connecting every second point
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[2].x, points[2].y)
            lineTo(points[4].x, points[4].y)
            lineTo(points[1].x, points[1].y)
            lineTo(points[3].x, points[3].y)
            close()
        }
        
        // Fill the pentagram
        drawPath(
            path = path,
            color = color,
            alpha = 0.3f
        )
        
        // Draw the outline
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }
}

/**
 * Displays a download icon (down arrow) using Image for cross-platform compatibility
 * Source: Noto Emoji down arrow
 */
@Composable
fun DownloadIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_down_arrow),
        contentDescription = "Download",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays an upload icon (up arrow) using Image for cross-platform compatibility
 * Source: Noto Emoji up arrow
 */
@Composable
fun UploadIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_up_arrow),
        contentDescription = "Upload",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a low volume speaker icon using Image for cross-platform compatibility
 * Source: Custom icon (U+1F508 speaker low volume)
 */
@Composable
fun SpeakerLowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_speaker_low),
        contentDescription = "Speaker Low",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a high volume speaker icon using Image for cross-platform compatibility
 * Source: Custom icon (U+1F50A speaker high volume)
 */
@Composable
fun SpeakerHighIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_speaker_high),
        contentDescription = "Speaker High",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a wood/log icon using Image for cross-platform compatibility
 * Source: Custom wood icon representing barricade material
 */
@Composable
fun WoodIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_wood),
        contentDescription = "Wood",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a plus sign icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2795)
 */
@Composable
fun PlusIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_plus),
        contentDescription = "Plus",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a cross/X mark icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2716)
 */
@Composable
fun CrossIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Image(
        painter = painterResource(Res.drawable.emoji_cross),
        contentDescription = "Cross",
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.Companion.tint(it) }
    )
}

/**
 * Displays a pencil icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+270F)
 */
@Composable
fun PencilIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_pencil),
        contentDescription = "Pencil",
        modifier = modifier.size(size)
    )
}
