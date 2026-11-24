package de.egril.defender.ui.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.egril.defender.model.DigOutcome
import de.egril.defender.ui.drawTowerBase
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.barricade
import defender_of_egril.composeapp.generated.resources.dig_outcome_brass
import defender_of_egril.composeapp.generated.resources.dig_outcome_diamond
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_blue
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_green
import defender_of_egril.composeapp.generated.resources.dig_outcome_gem_red
import defender_of_egril.composeapp.generated.resources.dig_outcome_gold
import defender_of_egril.composeapp.generated.resources.dig_outcome_rubble
import defender_of_egril.composeapp.generated.resources.dig_outcome_silver
import defender_of_egril.composeapp.generated.resources.dragon_destroying_mine
import defender_of_egril.composeapp.generated.resources.emoji_warning
import defender_of_egril.composeapp.generated.resources.gate
import dev.vicart.compose.material.symbols.MaterialSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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
    size: Dp = 64.dp,
) {
    val resource =
        when (outcome) {
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
        modifier = modifier.size(displaySize),
    )
}

/**
 * Displays a warning emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+26A0)
 */
@Composable
fun WarningIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_warning),
        contentDescription = "Warning",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a stylized spider web icon using Canvas.
 */
@Composable
fun WebIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    color: Color = Color(0xFFE6E0F8),
) {
    Canvas(
        modifier = modifier.size(size),
    ) {
        val radius = minOf(this.size.width, this.size.height) * 0.42f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val spokeAngles = List(8) { it * (PI / 4.0) }
        val stroke = radius * 0.12f

        spokeAngles.forEach { angle ->
            val end =
                Offset(
                    x = center.x + (radius * cos(angle)).toFloat(),
                    y = center.y + (radius * sin(angle)).toFloat(),
                )
            drawLine(
                color = color,
                start = center,
                end = end,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        listOf(0.35f, 0.62f, 0.88f).forEach { scale ->
            val points =
                spokeAngles.map { angle ->
                    Offset(
                        x = center.x + (radius * scale * cos(angle)).toFloat(),
                        y = center.y + (radius * scale * sin(angle)).toFloat(),
                    )
                }
            for (i in points.indices) {
                val start = points[i]
                val end = points[(i + 1) % points.size]
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = stroke * 0.75f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * Displays a right arrow icon
 */
@Composable
fun RightArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_FORWARD,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a red circle icon used to indicate errors
 */
@Composable
fun RedCircleIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 12.dp,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.CIRCLE,
        size = size,
        tint = Color.Red,
        modifier = modifier,
    )
}

/**
 * Displays a pentagram (5-pointed star) icon using Canvas for magical traps
 */
@Composable
fun PentagramIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
    color: Color = Color(0xFFAA00FF), // Purple/magenta color for magical trap
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(size),
    ) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val radius = minOf(canvasWidth, canvasHeight) / 2f * 0.9f

        // Calculate the 5 points of the star
        val points = mutableListOf<androidx.compose.ui.geometry.Offset>()
        for (i in 0 until 5) {
            val angle = (i * 72 - 90) * (PI / 180.0) // Convert degrees to radians
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            points.add(
                androidx.compose.ui.geometry
                    .Offset(x, y),
            )
        }

        // Draw the pentagram by connecting every second point
        val path =
            androidx.compose.ui.graphics.Path().apply {
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
            alpha = 0.3f,
        )

        // Draw the outline
        drawPath(
            path = path,
            color = color,
            style =
                androidx.compose.ui.graphics.drawscope
                    .Stroke(width = 2f),
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
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_DOWNWARD,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays an upload icon (up arrow)
 */
@Composable
fun UploadIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_UPWARD,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a low volume speaker icon
 */
@Composable
fun SpeakerLowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.VOLUME_DOWN,
        size = size,
        modifier = modifier,
    )
}

/**
 * Displays a high volume speaker icon
 */
@Composable
fun SpeakerHighIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.VOLUME_UP,
        size = size,
        modifier = modifier,
    )
}

/**
 * Displays a wood/log icon using Image for cross-platform compatibility
 * Source: Custom wood icon representing barricade material
 */
@Composable
fun WoodIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
) {
    Image(
        painter = painterResource(Res.drawable.barricade),
        contentDescription = "Wood",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a gate icon using Image for cross-platform compatibility
 * Source: Custom gate icon for barricades between two towers
 */
@Composable
fun GateIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
) {
    Image(
        painter = painterResource(Res.drawable.gate),
        contentDescription = "Gate",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a plus sign icon
 */
@Composable
fun PlusIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ADD,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a cross/X mark icon
 */
@Composable
fun CrossIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.CLOSE,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a pencil icon
 */
@Composable
fun PencilIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.EDIT,
        size = size,
        modifier = modifier,
    )
}

/**
 * Displays a trophy icon
 */
@Composable
fun TrophyIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.WORKSPACE_PREMIUM,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays an open book icon (used for the story logbook)
 */
@Composable
fun BookIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.BOOK_2,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a chevron right icon (simple arrow)
 */
@Composable
fun ChevronRightIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color = Color.Black,
) {
    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        val arrowPath =
            Path().apply {
                moveTo(width * 0.3f, height * 0.2f)
                lineTo(width * 0.7f, height * 0.5f)
                lineTo(width * 0.3f, height * 0.8f)
            }
        drawPath(
            path = arrowPath,
            color = tint,
            style = Stroke(width = width * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round),
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
    lineColor: Color = Color.Gray,
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
 * Displays a narrow passage (two converging lines with a bottleneck) drawn with Canvas.
 * The [contentDescription] is the localized label, since the icon replaces visible text.
 */
@Composable
fun NarrowPathIcon(
    contentDescription: String,
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color = LocalContentColor.current,
) {
    Canvas(
        modifier =
            modifier
                .size(size)
                .semantics { this.contentDescription = contentDescription },
    ) {
        val s = this.size.minDimension
        val strokeWidth = s * 0.1f
        drawLine(
            color = tint,
            start = Offset(0f, s * 0.2f),
            end = Offset(s * 0.5f, s * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(0f, s * 0.8f),
            end = Offset(s * 0.5f, s * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(s * 0.5f, s * 0.5f),
            end = Offset(s, s * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(s * 0.5f, s * 0.5f),
            end = Offset(s, s * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Displays a crosshair (ring with a centre cross) drawn with Canvas.
 * The [contentDescription] is the localized label, since the icon replaces visible text.
 */
@Composable
fun CrosshairIcon(
    contentDescription: String,
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color = LocalContentColor.current,
) {
    Canvas(
        modifier =
            modifier
                .size(size)
                .semantics { this.contentDescription = contentDescription },
    ) {
        val s = this.size.minDimension
        val center = Offset(s / 2f, s / 2f)
        val strokeWidth = s * 0.1f
        drawCircle(
            color = tint,
            radius = s * 0.36f,
            center = center,
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = tint,
            start = Offset(center.x, 0f),
            end = Offset(center.x, s * 0.3f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(center.x, s),
            end = Offset(center.x, s * 0.7f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(0f, center.y),
            end = Offset(s * 0.3f, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(s, center.y),
            end = Offset(s * 0.7f, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
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
    tint: Color = Color(0xFF2196F3), // Blue color
) {
    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()

        // Draw wave pattern
        val wavePath =
            Path().apply {
                moveTo(0f, height * 0.5f)
                // First wave
                cubicTo(
                    width * 0.15f,
                    height * 0.3f,
                    width * 0.25f,
                    height * 0.7f,
                    width * 0.4f,
                    height * 0.5f,
                )
                // Second wave
                cubicTo(
                    width * 0.55f,
                    height * 0.3f,
                    width * 0.65f,
                    height * 0.7f,
                    width * 0.8f,
                    height * 0.5f,
                )
                // Third wave
                cubicTo(
                    width * 0.9f,
                    height * 0.4f,
                    width * 0.95f,
                    height * 0.5f,
                    width,
                    height * 0.5f,
                )
            }
        drawPath(
            path = wavePath,
            color = tint,
            style = Stroke(width = width * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round),
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
    color: Color = Color(0xFFFFD700), // Gold color
) {
    Canvas(
        modifier = modifier.size(size),
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
            val angle = (i * 36 - 90) * (PI / 180.0) // Convert degrees to radians
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            points.add(Offset(x, y))
        }

        // Draw the star
        val path =
            Path().apply {
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
            alpha = 0.3f,
        )

        // Draw the outline
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2f),
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
    color: Color = Color(0xFF795548), // Brown color
) {
    Canvas(
        modifier = modifier.size(size),
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
            size = Size(headWidth, headHeight),
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
            cap = StrokeCap.Round,
        )
    }
}
