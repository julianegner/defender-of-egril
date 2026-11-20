package de.egril.defender.ui.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
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
import de.egril.defender.ui.drawTowerBase
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.emoji_shield
import defender_of_egril.composeapp.generated.resources.emoji_speech_bubble
import dev.vicart.compose.material.symbols.MaterialSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Displays an Attack Area spell icon: 3 purple target circles — 1 on top centered, 2 below
 */
@Composable
fun AttackAreaSpellIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
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
        val positions =
            listOf(
                Offset(w / 2f, topRowY), // top center
                Offset(w * 0.27f, bottomRowY), // bottom left
                Offset(w * 0.73f, bottomRowY), // bottom right
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
    size: Dp = 24.dp,
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
private fun DrawScope.drawSmallGoblinHead(
    cx: Float,
    cy: Float,
    headRadius: Float,
) {
    val earW = headRadius * 0.55f
    val earH = headRadius * 0.65f
    val leftEar =
        Path().apply {
            moveTo(cx - headRadius * 0.75f, cy - headRadius * 0.2f)
            lineTo(cx - headRadius * 0.75f - earW, cy - headRadius * 0.2f - earH)
            lineTo(cx - headRadius * 0.4f, cy - headRadius * 0.55f)
            close()
        }
    val rightEar =
        Path().apply {
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
private fun DrawScope.drawSmallOgreHead(
    cx: Float,
    cy: Float,
    headRadius: Float,
) {
    drawCircle(color = Color(0xFFA0522D), radius = headRadius, center = Offset(cx, cy))
    drawCircle(color = Color.White, radius = headRadius * 0.22f, center = Offset(cx - headRadius * 0.35f, cy - headRadius * 0.1f))
    drawCircle(color = Color.White, radius = headRadius * 0.22f, center = Offset(cx + headRadius * 0.35f, cy - headRadius * 0.1f))
    drawCircle(color = Color.Black, radius = headRadius * 0.1f, center = Offset(cx - headRadius * 0.35f, cy - headRadius * 0.1f))
    drawCircle(color = Color.Black, radius = headRadius * 0.1f, center = Offset(cx + headRadius * 0.35f, cy - headRadius * 0.1f))
}

/**
 * Draws fear scribbles (chaotic zigzag lines in black) above a head center.
 */
private fun DrawScope.drawFearScribbles(
    cx: Float,
    topY: Float,
    width: Float,
    strokeWidth: Float,
) {
    val halfW = width / 2f
    val zigzagH = width * 0.35f
    val scribblePaint = Color.Black

    // Two zigzag scribble lines
    for (i in 0..1) {
        val yBase = topY + i * zigzagH * 0.9f
        val scribble =
            Path().apply {
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
    size: Dp = 24.dp,
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
    size: Dp = 24.dp,
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
 * Displays a shield emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F6E1)
 */
@Composable
fun ShieldIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_shield),
        contentDescription = "Shield",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a speech bubble emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4AC)
 */
@Composable
fun SpeechBubbleIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_speech_bubble),
        contentDescription = "Speech Bubble",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a question mark
 */
@Composable
fun HelpIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.HELP,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a snowflake icon using Canvas for cross-platform compatibility
 */
@Composable
fun SnowflakeIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
    tint: Color = Color.Cyan,
) {
    Canvas(
        modifier = modifier.size(size),
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
                end =
                    Offset(
                        center.x + cos * radius,
                        center.y + sin * radius,
                    ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
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
                end =
                    Offset(
                        branchX + kotlin.math.cos(leftAngle) * branchLength,
                        branchY + kotlin.math.sin(leftAngle) * branchLength,
                    ),
                strokeWidth = strokeWidth * 0.7f,
                cap = StrokeCap.Round,
            )

            // Right branch
            val rightAngle = angle + PI.toFloat() / 6f
            drawLine(
                color = tint,
                start = Offset(branchX, branchY),
                end =
                    Offset(
                        branchX + kotlin.math.cos(rightAngle) * branchLength,
                        branchY + kotlin.math.sin(rightAngle) * branchLength,
                    ),
                strokeWidth = strokeWidth * 0.7f,
                cap = StrokeCap.Round,
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
    color: Color = Color.Unspecified,
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
            cornerRadius =
                androidx.compose.ui.geometry
                    .CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth),
        )

        // Inner raised key surface (slightly inset)
        val innerInset = w * 0.2f
        val innerCorner = cornerRadius * 0.6f
        drawRoundRect(
            color = resolvedColor,
            topLeft = Offset(innerInset, innerInset),
            size = Size(w - innerInset * 2f, h * 0.55f),
            cornerRadius =
                androidx.compose.ui.geometry
                    .CornerRadius(innerCorner, innerCorner),
            style = Stroke(width = strokeWidth * 0.7f),
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
    lineColor: Color = Color.White,
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
            cap = StrokeCap.Round,
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
    lineColor: Color = Color.White,
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
            cap = StrokeCap.Round,
        )

        // Arrow head (filled triangle pointing up)
        val head =
            Path().apply {
                moveTo(arrowX, arrowTop)
                lineTo(arrowX - arrowWidth / 2f, arrowTop + arrowWidth)
                lineTo(arrowX + arrowWidth / 2f, arrowTop + arrowWidth)
                close()
            }
        drawPath(head, Color.Red)
    }
}

/**
 * Displays a mushroom icon drawn on a canvas.
 * Mushrooms boost horde units and witches: 2x speed and 2x level for 2 turns.
 */
@Composable
fun MushroomIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    capColor: Color = Color(0xFFFF6D00), // Orange-red cap
    spotColor: Color = Color(0xFFFFFFFF), // White spots
    stemColor: Color = Color(0xFFFFF9C4), // Pale yellow stem
) {
    Canvas(modifier = modifier.size(size)) {
        val minDimension = minOf(this.size.width, this.size.height)
        val capColors =
            listOf(
                capColor,
                Color(0xFFE53935),
                Color(0xFF8E24AA),
            )

        fun drawTinyMushroom(
            centerX: Float,
            baseY: Float,
            mushroomHeight: Float,
            mushroomCapColor: Color,
        ) {
            val stemHeight = mushroomHeight * 0.58f
            val stemWidth = mushroomHeight * 0.18f
            val stemLeft = centerX - stemWidth / 2f
            val stemTop = baseY - stemHeight
            drawRoundRect(
                color = stemColor,
                topLeft = Offset(stemLeft, stemTop),
                size = Size(stemWidth, stemHeight),
                cornerRadius =
                    androidx.compose.ui.geometry
                        .CornerRadius(stemWidth * 0.45f),
            )

            val capWidth = mushroomHeight * 0.58f
            val capHeight = mushroomHeight * 0.26f
            val capTop = stemTop - capHeight * 0.7f
            drawOval(
                color = mushroomCapColor,
                topLeft = Offset(centerX - capWidth / 2f, capTop),
                size = Size(capWidth, capHeight),
            )

            drawCircle(
                color = spotColor,
                radius = capWidth * 0.08f,
                center = Offset(centerX - capWidth * 0.12f, capTop + capHeight * 0.42f),
            )
        }

        val mushroomHeight = minDimension * 0.22f
        drawTinyMushroom(
            centerX = minDimension * 0.28f,
            baseY = minDimension * 0.78f,
            mushroomHeight = mushroomHeight,
            mushroomCapColor = capColors[0],
        )
        drawTinyMushroom(
            centerX = minDimension * 0.50f,
            baseY = minDimension * 0.60f,
            mushroomHeight = mushroomHeight,
            mushroomCapColor = capColors[1],
        )
        drawTinyMushroom(
            centerX = minDimension * 0.72f,
            baseY = minDimension * 0.78f,
            mushroomHeight = mushroomHeight,
            mushroomCapColor = capColors[2],
        )
    }
}
