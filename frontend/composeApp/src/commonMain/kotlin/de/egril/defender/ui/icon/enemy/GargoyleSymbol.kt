package de.egril.defender.ui.icon.enemy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a gargoyle symbol.
 *
 * The inactive form is a grey stone statue with folded wings and dim eyes. The active form is an
 * enemy with spread wings, a reddish body and glowing red eyes.
 */
fun DrawScope.drawGargoyleSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    active: Boolean,
    headScale: Float = 1.0f,
) {
    val bodyColor = if (active) Color(0xFF9C6259) else Color(0xFF8D9399)
    val wingColor = if (active) Color(0xFF6A3B3B) else Color(0xFF6E757B)
    val headCenterY = centerY - size * 0.24f

    if (active) {
        drawCircle(
            color = Color(0x33FF1744),
            radius = size * 0.45f,
            center = Offset(centerX, centerY),
        )
    }

    // Wings are drawn before the body so the body covers their inner edges.
    val wingSign = listOf(-1f, 1f)
    for (sign in wingSign) {
        drawPath(
            path = if (active) spreadWingPath(centerX, centerY, size, sign) else foldedWingPath(centerX, centerY, size, sign),
            color = wingColor,
        )
    }

    drawPath(path = bodyPath(centerX, centerY, size), color = bodyColor)

    drawRect(
        color = bodyColor,
        topLeft = Offset(centerX - size * 0.22f, centerY + size * 0.28f),
        size = Size(size * 0.44f, size * 0.08f),
    )

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        drawPath(path = hornPath(centerX, headCenterY, size, -1f), color = wingColor)
        drawPath(path = hornPath(centerX, headCenterY, size, 1f), color = wingColor)
        drawCircle(
            color = bodyColor,
            radius = size * 0.12f,
            center = Offset(centerX, headCenterY),
        )
        if (active) {
            drawEyes(centerX, headCenterY, size, glowColor = Color(0x88FF1744), coreColor = Color(0xFFFF1744), pupilColor = Color(0xFFFFCDD2))
        } else {
            drawEyes(centerX, headCenterY, size, glowColor = null, coreColor = Color(0xFFC8B98A), pupilColor = null)
        }
    }
}

/** Folded wings hug the body along its sides. */
private fun foldedWingPath(
    centerX: Float,
    centerY: Float,
    size: Float,
    sign: Float,
): Path =
    Path().apply {
        moveTo(centerX + sign * size * 0.12f, centerY - size * 0.10f)
        lineTo(centerX + sign * size * 0.24f, centerY - size * 0.02f)
        lineTo(centerX + sign * size * 0.22f, centerY + size * 0.26f)
        lineTo(centerX + sign * size * 0.12f, centerY + size * 0.18f)
        close()
    }

/** Spread wings with a scalloped trailing edge. */
private fun spreadWingPath(
    centerX: Float,
    centerY: Float,
    size: Float,
    sign: Float,
): Path =
    Path().apply {
        moveTo(centerX + sign * size * 0.12f, centerY - size * 0.12f)
        lineTo(centerX + sign * size * 0.46f, centerY - size * 0.42f)
        lineTo(centerX + sign * size * 0.40f, centerY - size * 0.18f)
        lineTo(centerX + sign * size * 0.50f, centerY - size * 0.10f)
        lineTo(centerX + sign * size * 0.42f, centerY + size * 0.08f)
        lineTo(centerX + sign * size * 0.16f, centerY + size * 0.14f)
        close()
    }

/** Trapezoidal stone torso, narrower at the shoulders. */
private fun bodyPath(
    centerX: Float,
    centerY: Float,
    size: Float,
): Path =
    Path().apply {
        moveTo(centerX - size * 0.13f, centerY - size * 0.14f)
        lineTo(centerX + size * 0.13f, centerY - size * 0.14f)
        lineTo(centerX + size * 0.18f, centerY + size * 0.28f)
        lineTo(centerX - size * 0.18f, centerY + size * 0.28f)
        close()
    }

private fun hornPath(
    centerX: Float,
    headCenterY: Float,
    size: Float,
    sign: Float,
): Path =
    Path().apply {
        moveTo(centerX + sign * size * 0.07f, headCenterY - size * 0.08f)
        lineTo(centerX + sign * size * 0.16f, headCenterY - size * 0.22f)
        lineTo(centerX + sign * size * 0.02f, headCenterY - size * 0.11f)
        close()
    }

private fun DrawScope.drawEyes(
    centerX: Float,
    headCenterY: Float,
    size: Float,
    glowColor: Color?,
    coreColor: Color,
    pupilColor: Color?,
) {
    val eyeOffsets = listOf(-0.05f, 0.05f)
    for (offset in eyeOffsets) {
        val eyeCenter = Offset(centerX + offset * size, headCenterY - size * 0.02f)
        if (glowColor != null) {
            drawCircle(color = glowColor, radius = size * 0.09f, center = eyeCenter)
        }
        drawCircle(color = coreColor, radius = size * 0.035f, center = eyeCenter)
        if (pupilColor != null) {
            drawCircle(color = pupilColor, radius = size * 0.015f, center = eyeCenter)
        }
    }
}

/** Stone gargoyle standing on a tile, used for inactive guardians. */
@Composable
fun GargoyleIcon(
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val iconSize = minOf(this.size.width, this.size.height)
        drawGargoyleSymbol(
            centerX = this.size.width / 2,
            centerY = this.size.height / 2,
            size = iconSize,
            active = active,
        )
    }
}
