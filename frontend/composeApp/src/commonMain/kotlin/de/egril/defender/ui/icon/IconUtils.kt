package de.egril.defender.ui.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.egril.defender.model.DigOutcome
import de.egril.defender.ui.drawTowerBase
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.bomb
import defender_of_egril.composeapp.generated.resources.dig_outcome_brass
import defender_of_egril.composeapp.generated.resources.dig_outcome_diamond
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_blue
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_green
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_red
import defender_of_egril.composeapp.generated.resources.dig_outcome_gold
import defender_of_egril.composeapp.generated.resources.dig_outcome_rubble
import defender_of_egril.composeapp.generated.resources.dig_outcome_silver
import defender_of_egril.composeapp.generated.resources.dragon_destroying_mine
import defender_of_egril.composeapp.generated.resources.emoji_explosion
import defender_of_egril.composeapp.generated.resources.emoji_hole
import defender_of_egril.composeapp.generated.resources.emoji_pick
import defender_of_egril.composeapp.generated.resources.emoji_sword
import defender_of_egril.composeapp.generated.resources.emoji_target
import defender_of_egril.composeapp.generated.resources.barricade
import defender_of_egril.composeapp.generated.resources.gate
import defender_of_egril.composeapp.generated.resources.trap
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Displays a lightning bolt icon
 */
@Composable
fun LightningIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Bolt,
        contentDescription = "Lightning",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a timer icon
 */
@Composable
fun TimerIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 10.dp
) {
    Icon(
        imageVector = Icons.Filled.Timer,
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
 * Displays a heart icon
 */
@Composable
fun HeartIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = "Heart",
        tint = Color.Red,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a reload icon
 */
@Composable
fun ReloadIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Refresh,
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
 * Displays the bomb image icon using Image for cross-platform compatibility
 */
@Composable
fun BombIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.bomb),
        contentDescription = "Bomb",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a test tube icon
 */
@Composable
fun TestTubeIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Science,
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
 * Displays a trap icon using the trap.png resource
 * Shows a detailed pit trap with grass and flowers
 */
@Composable
fun TrapIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(Res.drawable.trap),
        contentDescription = "Trap",
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
 * Displays a money icon
 */
@Composable
fun MoneyIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.MonetizationOn,
        contentDescription = "Money",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a coffee icon
 */
@Composable
fun CoffeeIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Coffee,
        contentDescription = "Coffee",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a bed icon
 */
@Composable
fun BedIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Hotel,
        contentDescription = "Bed",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a triangle up icon
 */
@Composable
fun TriangleUpIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.ArrowDropUp,
        contentDescription = "Triangle Up",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a triangle right icon
 */
@Composable
fun TriangleRightIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowRight,
        contentDescription = "Triangle Right",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a triangle left icon
 */
@Composable
fun TriangleLeftIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
        contentDescription = "Triangle Left",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a triangle down icon
 */
@Composable
fun TriangleDownIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.ArrowDropDown,
        contentDescription = "Triangle Down",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a trash icon
 */
@Composable
fun TrashIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp
) {
    Icon(
        imageVector = Icons.Filled.Delete,
        contentDescription = "Trash",
        modifier = modifier.size(size)
    )
}

/**
 * Displays an info icon
 */
@Composable
fun InfoIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Info,
        contentDescription = "Info",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a door icon
 */
@Composable
fun DoorIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.MeetingRoom,
        contentDescription = "Door",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a pushpin icon
 */
@Composable
fun PushpinIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.PushPin,
        contentDescription = "Pushpin",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a left arrow icon
 */
@Composable
fun LeftArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Left Arrow",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays an up arrow icon
 */
@Composable
fun UpArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.ArrowUpward,
        contentDescription = "Up Arrow",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a down arrow icon
 */
@Composable
fun DownArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.ArrowDownward,
        contentDescription = "Down Arrow",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a checkmark icon
 */
@Composable
fun CheckmarkIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = "Checkmark",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a tools icon
 */
@Composable
fun ToolsIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp
) {
    Icon(
        imageVector = Icons.Filled.Handyman,
        contentDescription = "Tools",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a lock icon
 */
@Composable
fun LockIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = "Lock",
        modifier = modifier.size(size)
    )
}

/**
 * Displays an unlock icon
 */
@Composable
fun UnlockIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.LockOpen,
        contentDescription = "Unlock",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a magnifying glass icon
 */
@Composable
fun MagnifyingGlassIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.Search,
        contentDescription = "Magnifying Glass",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a save icon
 */
@Composable
fun SaveIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Save,
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
 * Displays a warning icon
 */
@Composable
fun WarningIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Warning,
        contentDescription = "Warning",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a right arrow icon
 */
@Composable
fun RightArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = "Right Arrow",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a red circle icon used to indicate errors
 */
@Composable
fun RedCircleIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 12.dp
) {
    Icon(
        imageVector = Icons.Filled.Circle,
        contentDescription = "Error",
        tint = Color.Red,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a map icon
 */
@Composable
fun MapIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Map,
        contentDescription = "Map",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a number 1 icon
 */
@Composable
fun Number1Icon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.LooksOne,
        contentDescription = "1",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a number 2 icon
 */
@Composable
fun Number2Icon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.LooksTwo,
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
 * Displays a download icon (down arrow)
 */
@Composable
fun DownloadIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.ArrowDownward,
        contentDescription = "Download",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays an upload icon (up arrow)
 */
@Composable
fun UploadIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.ArrowUpward,
        contentDescription = "Upload",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a low volume speaker icon
 */
@Composable
fun SpeakerLowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.VolumeDown,
        contentDescription = "Speaker Low",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a high volume speaker icon
 */
@Composable
fun SpeakerHighIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
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
        painter = painterResource(Res.drawable.barricade),
        contentDescription = "Wood",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a gate icon using Image for cross-platform compatibility
 * Source: Custom gate icon for barricades between two towers
 */
@Composable
fun GateIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(Res.drawable.gate),
        contentDescription = "Gate",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a plus sign icon
 */
@Composable
fun PlusIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.Add,
        contentDescription = "Plus",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a cross/X mark icon
 */
@Composable
fun CrossIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = "Cross",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a pencil icon
 */
@Composable
fun PencilIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Edit,
        contentDescription = "Pencil",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a trophy icon
 */
@Composable
fun TrophyIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.WorkspacePremium,
        contentDescription = "Trophy/Achievement",
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Displays a chevron right icon (simple arrow)
 */
@Composable
fun ChevronRightIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        val arrowPath = Path().apply {
            moveTo(width * 0.3f, height * 0.2f)
            lineTo(width * 0.7f, height * 0.5f)
            lineTo(width * 0.3f, height * 0.8f)
        }
        drawPath(
            path = arrowPath,
            color = tint,
            style = Stroke(width = width * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Displays a simple tower icon using drawTowerBase
 * Represents a generic tower for level header, without type-specific icon inside
 */
@Composable
fun TowerIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    lineColor: Color = Color.Gray
) {
    Canvas(modifier = modifier.size(size)) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2
        val iconSize = minOf(this.size.width, this.size.height)
        
        // Draw tower base using the same function as in-game towers
        drawTowerBase(centerX, centerY, iconSize * 0.8f, lineColor)
    }
}

/**
 * Displays a simple water/wave icon drawn with Canvas
 * Represents water/river for level header
 */
@Composable
fun WaterIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color = Color(0xFF2196F3)  // Blue color
) {
    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        
        // Draw wave pattern
        val wavePath = Path().apply {
            moveTo(0f, height * 0.5f)
            // First wave
            cubicTo(
                width * 0.15f, height * 0.3f,
                width * 0.25f, height * 0.7f,
                width * 0.4f, height * 0.5f
            )
            // Second wave
            cubicTo(
                width * 0.55f, height * 0.3f,
                width * 0.65f, height * 0.7f,
                width * 0.8f, height * 0.5f
            )
            // Third wave
            cubicTo(
                width * 0.9f, height * 0.4f,
                width * 0.95f, height * 0.5f,
                width, height * 0.5f
            )
        }
        drawPath(
            path = wavePath,
            color = tint,
            style = Stroke(width = width * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Displays a simple star icon using Canvas
 */
@Composable
fun StarIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
    color: Color = Color(0xFFFFD700)  // Gold color
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val outerRadius = minOf(canvasWidth, canvasHeight) / 2f * 0.9f
        val innerRadius = outerRadius * 0.4f
        
        // Calculate the 5 points of the star (outer and inner)
        val points = mutableListOf<Offset>()
        for (i in 0 until 10) {
            val angle = (i * 36 - 90) * (PI / 180.0)  // Convert degrees to radians
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            points.add(Offset(x, y))
        }
        
        // Draw the star
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
            close()
        }
        
        // Fill the star
        drawPath(
            path = path,
            color = color,
            alpha = 0.3f
        )
        
        // Draw the outline
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Displays a simple hammer icon using Canvas for construction/building
 */
@Composable
fun HammerIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
    color: Color = Color(0xFF795548)  // Brown color
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height
        
        // Draw hammer head (rectangle)
        val headWidth = canvasWidth * 0.5f
        val headHeight = canvasHeight * 0.2f
        val headLeft = canvasWidth * 0.1f
        val headTop = canvasHeight * 0.15f
        
        drawRect(
            color = color,
            topLeft = Offset(headLeft, headTop),
            size = Size(headWidth, headHeight)
        )
        
        // Draw hammer handle (line)
        val handleStartX = canvasWidth * 0.5f
        val handleStartY = headTop + headHeight
        val handleEndX = canvasWidth * 0.8f
        val handleEndY = canvasHeight * 0.85f
        
        drawLine(
            color = color,
            start = Offset(handleStartX, handleStartY),
            end = Offset(handleEndX, handleEndY),
            strokeWidth = canvasWidth * 0.1f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Displays an Attack Area spell icon: 3 purple target circles — 1 on top centered, 2 below
 */
@Composable
fun AttackAreaSpellIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = minOf(w, h) * 0.18f
        val innerRadius = radius * 0.4f
        val strokeWidth = radius * 0.3f
        val purple = Color(0xFFAA00FF)

        // 3 positions: 1 centered on top row, 2 on bottom row
        val topRowY = h * 0.3f
        val bottomRowY = h * 0.72f
        val positions = listOf(
            Offset(w / 2f, topRowY),            // top center
            Offset(w * 0.27f, bottomRowY),       // bottom left
            Offset(w * 0.73f, bottomRowY)        // bottom right
        )

        for (pos in positions) {
            drawCircle(color = purple, radius = radius, center = pos, style = Stroke(width = strokeWidth))
            drawCircle(color = purple, radius = innerRadius, center = pos)
        }
    }
}

/**
 * Displays an Attack Aimed spell icon: a single purple target circle
 */
@Composable
fun AttackAimedSpellIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = minOf(w, h) * 0.33f
        val innerRadius = radius * 0.4f
        val strokeWidth = radius * 0.3f
        val purple = Color(0xFFAA00FF)
        val center = Offset(w / 2f, h / 2f)

        drawCircle(color = purple, radius = radius, center = center, style = Stroke(width = strokeWidth))
        drawCircle(color = purple, radius = innerRadius, center = center)
    }
}

/**
 * Draws a small goblin head (green circle with pointy ears and red eyes) at the given center and scale.
 */
private fun DrawScope.drawSmallGoblinHead(cx: Float, cy: Float, headRadius: Float) {
    val earW = headRadius * 0.55f
    val earH = headRadius * 0.65f
    val leftEar = Path().apply {
        moveTo(cx - headRadius * 0.75f, cy - headRadius * 0.2f)
        lineTo(cx - headRadius * 0.75f - earW, cy - headRadius * 0.2f - earH)
        lineTo(cx - headRadius * 0.4f, cy - headRadius * 0.55f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(cx + headRadius * 0.75f, cy - headRadius * 0.2f)
        lineTo(cx + headRadius * 0.75f + earW, cy - headRadius * 0.2f - earH)
        lineTo(cx + headRadius * 0.4f, cy - headRadius * 0.55f)
        close()
    }
    drawPath(leftEar, Color(0xFF90EE90))
    drawPath(rightEar, Color(0xFF90EE90))
    drawCircle(color = Color(0xFF90EE90), radius = headRadius, center = Offset(cx, cy))
    drawCircle(color = Color.Red, radius = headRadius * 0.18f, center = Offset(cx - headRadius * 0.32f, cy - headRadius * 0.1f))
    drawCircle(color = Color.Red, radius = headRadius * 0.18f, center = Offset(cx + headRadius * 0.32f, cy - headRadius * 0.1f))
}

/**
 * Draws a small ogre head (brown circle with white eyes) at the given center and scale.
 */
private fun DrawScope.drawSmallOgreHead(cx: Float, cy: Float, headRadius: Float) {
    drawCircle(color = Color(0xFFA0522D), radius = headRadius, center = Offset(cx, cy))
    drawCircle(color = Color.White, radius = headRadius * 0.22f, center = Offset(cx - headRadius * 0.35f, cy - headRadius * 0.1f))
    drawCircle(color = Color.White, radius = headRadius * 0.22f, center = Offset(cx + headRadius * 0.35f, cy - headRadius * 0.1f))
    drawCircle(color = Color.Black, radius = headRadius * 0.1f, center = Offset(cx - headRadius * 0.35f, cy - headRadius * 0.1f))
    drawCircle(color = Color.Black, radius = headRadius * 0.1f, center = Offset(cx + headRadius * 0.35f, cy - headRadius * 0.1f))
}

/**
 * Draws fear scribbles (chaotic zigzag lines in black) above a head center.
 */
private fun DrawScope.drawFearScribbles(cx: Float, topY: Float, width: Float, strokeWidth: Float) {
    val halfW = width / 2f
    val zigzagH = width * 0.35f
    val scribblePaint = Color.Black

    // Two zigzag scribble lines
    for (i in 0..1) {
        val yBase = topY + i * zigzagH * 0.9f
        val scribble = Path().apply {
            moveTo(cx - halfW, yBase)
            lineTo(cx - halfW * 0.4f, yBase - zigzagH * 0.5f)
            lineTo(cx + halfW * 0.2f, yBase + zigzagH * 0.3f)
            lineTo(cx + halfW, yBase - zigzagH * 0.4f)
        }
        drawPath(scribble, scribblePaint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Displays a Fear Spell icon: goblin head with black fear scribbles on top
 */
@Composable
fun FearSpellIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = minOf(w, h)
        val headRadius = s * 0.27f
        val cx = w / 2f
        // Position head in lower portion so scribbles fit above
        val cy = h * 0.65f

        drawSmallGoblinHead(cx, cy, headRadius)

        // Fear scribbles above the head
        val scribbleTopY = cy - headRadius - s * 0.05f
        drawFearScribbles(cx, scribbleTopY - s * 0.12f, headRadius * 1.6f, s * 0.05f)
    }
}

/**
 * Displays a Fear Spell (Area) icon: 2 goblins and 1 ogre with black fear scribbles
 */
@Composable
fun FearSpellAreaIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = minOf(w, h)

        // Layout: 2 small goblins on the left (stacked), 1 ogre on the right
        val goblinRadius = s * 0.18f
        val ogreRadius = s * 0.22f
        val leftX = w * 0.27f
        val rightX = w * 0.73f
        val topY = h * 0.3f
        val bottomY = h * 0.72f
        val ogreCY = (topY + bottomY) / 2f

        // Draw 2 goblins
        drawSmallGoblinHead(leftX, topY, goblinRadius)
        drawSmallGoblinHead(leftX, bottomY, goblinRadius)

        // Draw ogre
        drawSmallOgreHead(rightX, ogreCY, ogreRadius)

        // Fear scribbles above each head
        val scribbleStroke = s * 0.045f
        drawFearScribbles(leftX, topY - goblinRadius - s * 0.01f, goblinRadius * 1.5f, scribbleStroke)
        drawFearScribbles(leftX, bottomY - goblinRadius - s * 0.01f, goblinRadius * 1.5f, scribbleStroke)
        drawFearScribbles(rightX, ogreCY - ogreRadius - s * 0.01f, ogreRadius * 1.5f, scribbleStroke)
    }
}

/**
 * Displays a shield icon
 */
@Composable
fun ShieldIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.Filled.Shield,
        contentDescription = "Shield",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a speech bubble icon
 */
@Composable
fun SpeechBubbleIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.Chat,
        contentDescription = "Speech Bubble",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a snowflake icon using Canvas for cross-platform compatibility
 */
@Composable
fun SnowflakeIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
    tint: Color = Color.Cyan
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension / 2.5f
        val strokeWidth = this.size.minDimension * 0.08f
        
        // Draw 6 arms of the snowflake (60 degrees apart)
        for (i in 0 until 6) {
            val angle = i * PI.toFloat() / 3f
            val cos = kotlin.math.cos(angle)
            val sin = kotlin.math.sin(angle)
            
            // Main arm
            drawLine(
                color = tint,
                start = center,
                end = Offset(
                    center.x + cos * radius,
                    center.y + sin * radius
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            
            // Small branches on each arm
            val branchLength = radius * 0.3f
            val branchPos = radius * 0.7f
            
            // Branch point
            val branchX = center.x + cos * branchPos
            val branchY = center.y + sin * branchPos
            
            // Left branch
            val leftAngle = angle - PI.toFloat() / 6f
            drawLine(
                color = tint,
                start = Offset(branchX, branchY),
                end = Offset(
                    branchX + kotlin.math.cos(leftAngle) * branchLength,
                    branchY + kotlin.math.sin(leftAngle) * branchLength
                ),
                strokeWidth = strokeWidth * 0.7f,
                cap = StrokeCap.Round
            )
            
            // Right branch
            val rightAngle = angle + PI.toFloat() / 6f
            drawLine(
                color = tint,
                start = Offset(branchX, branchY),
                end = Offset(
                    branchX + kotlin.math.cos(rightAngle) * branchLength,
                    branchY + kotlin.math.sin(rightAngle) * branchLength
                ),
                strokeWidth = strokeWidth * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Displays a single keyboard key shape using Canvas for cross-platform compatibility.
 * Draws a rounded rectangle with a smaller raised key surface inside.
 */
@Composable
fun KeyboardKeyIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
    color: Color = Color.Unspecified
) {
    val resolvedColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.08f
        val cornerRadius = w * 0.18f

        // Outer key border
        val outerInset = strokeWidth / 2f
        drawRoundRect(
            color = resolvedColor,
            topLeft = Offset(outerInset, outerInset),
            size = Size(w - strokeWidth, h - strokeWidth),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth)
        )

        // Inner raised key surface (slightly inset)
        val innerInset = w * 0.2f
        val innerCorner = cornerRadius * 0.6f
        drawRoundRect(
            color = resolvedColor,
            topLeft = Offset(innerInset, innerInset),
            size = Size(w - innerInset * 2f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(innerCorner, innerCorner),
            style = Stroke(width = strokeWidth * 0.7f)
        )
    }
}

/**
 * Displays a sell-tower symbol: a tower with a diagonal strikethrough line
 * from bottom-left to top-right, indicating demolition/sale.
 */
@Composable
fun SellTowerIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
    lineColor: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2
        val iconSize = minOf(this.size.width, this.size.height)

        // Draw the tower base
        drawTowerBase(centerX, centerY, iconSize * 0.8f, lineColor)

        // Draw red diagonal strikethrough from bottom-left to top-right
        drawLine(
            color = Color.Red,
            start = Offset(this.size.width * 0.1f, this.size.height * 0.9f),
            end = Offset(this.size.width * 0.9f, this.size.height * 0.1f),
            strokeWidth = iconSize * 0.1f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Width multiplier for UpgradeTowerIcon: the canvas is 1.6× as wide as tall,
 * leaving room for the tower on the left and the arrow on the right.
 */
private const val UPGRADE_ICON_WIDTH_RATIO = 1.6f

/**
 * Displays an upgrade-tower symbol: a tower on the left with a red upward arrow directly adjacent.
 * Both elements are drawn in a single Canvas so there is no gap between them.
 */
@Composable
fun UpgradeTowerIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
    lineColor: Color = Color.White
) {
    Canvas(modifier = modifier.width(size * UPGRADE_ICON_WIDTH_RATIO).height(size)) {
        val s = size.toPx()
        val h = this.size.height

        // Draw tower in the left portion, centred vertically
        val towerCenterX = s * 0.45f
        val towerCenterY = h / 2
        drawTowerBase(towerCenterX, towerCenterY, s * 0.75f, lineColor)

        // Draw red upward arrow in the right portion, directly next to the tower
        val arrowX = s * 0.95f
        val arrowHeight = s * 0.78f
        val arrowWidth = s * 0.32f
        val arrowTop = towerCenterY - arrowHeight / 2f
        val arrowBottom = towerCenterY + arrowHeight / 2f

        // Arrow stem
        drawLine(
            color = Color.Red,
            start = Offset(arrowX, arrowBottom),
            end = Offset(arrowX, arrowTop + arrowWidth),
            strokeWidth = s * 0.1f,
            cap = StrokeCap.Round
        )

        // Arrow head (filled triangle pointing up)
        val head = Path().apply {
            moveTo(arrowX, arrowTop)
            lineTo(arrowX - arrowWidth / 2f, arrowTop + arrowWidth)
            lineTo(arrowX + arrowWidth / 2f, arrowTop + arrowWidth)
            close()
        }
        drawPath(head, Color.Red)
    }
}
