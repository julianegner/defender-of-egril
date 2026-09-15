package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Sylvanas the Molding, the corrupted Wild Nature forest-spirit villain.
 *
 * Once a majestic forest spirit, corrupted by the witches' dark magic. Her icon shows a pale
 * elven face crowned with twisted, rotting vines and glowing sickly-green eyes, set in a
 * dark-emerald robe that trails into withered tendrils — clearly distinct from the witch icons.
 */
fun DrawScope.drawSylvanasTheMoldingSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val outlineWidth = 2f
    val pathOutlineWidth = 2.5f
    val headCenterY = centerY - size * 0.1f

    val skinColor = Color(0xFFD4C8A8) // Pale, slightly ashen elven skin
    val robeColor = Color(0xFF1A3A20) // Deep, dark-emerald corrupted robe
    val vineColor = Color(0xFF2D5A1B) // Dark green rotting vines
    val rotColor = Color(0xFF4A6B1A) // Lighter rot-green for vine highlights
    val eyeColor = Color(0xFF7FFF00) // Sickly chartreuse glow

    // Robe / body (not scaled, hidden in bighead mode)
    if (headScale == 1.0f) {
        if (outlineColor != null) {
            drawRect(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.2f, centerY + size * 0.1f),
                size = Size(size * 0.4f, size * 0.35f),
                style = Stroke(width = pathOutlineWidth),
            )
        }
        drawRect(
            color = robeColor,
            topLeft = Offset(centerX - size * 0.2f, centerY + size * 0.1f),
            size = Size(size * 0.4f, size * 0.35f),
        )

        // Vine tendrils hanging from the robe hem (two downward curling lines)
        val leftTendril =
            Path().apply {
                moveTo(centerX - size * 0.12f, centerY + size * 0.42f)
                quadraticTo(
                    centerX - size * 0.2f,
                    centerY + size * 0.52f,
                    centerX - size * 0.15f,
                    centerY + size * 0.60f,
                )
            }
        val rightTendril =
            Path().apply {
                moveTo(centerX + size * 0.08f, centerY + size * 0.42f)
                quadraticTo(
                    centerX + size * 0.18f,
                    centerY + size * 0.52f,
                    centerX + size * 0.12f,
                    centerY + size * 0.60f,
                )
            }
        drawPath(leftTendril, vineColor, style = Stroke(width = size * 0.04f))
        drawPath(rightTendril, vineColor, style = Stroke(width = size * 0.04f))

        // A twisted vine/branch held in the left hand
        val staffVine =
            Path().apply {
                moveTo(centerX - size * 0.28f, centerY + size * 0.12f)
                cubicTo(
                    centerX - size * 0.38f,
                    centerY - size * 0.08f,
                    centerX - size * 0.32f,
                    centerY - size * 0.30f,
                    centerX - size * 0.26f,
                    centerY - size * 0.48f,
                )
            }
        drawPath(staffVine, vineColor, style = Stroke(width = size * 0.05f))
        // Small leaves/knobs on vine-staff
        drawCircle(
            color = rotColor,
            radius = size * 0.045f,
            center = Offset(centerX - size * 0.34f, centerY - size * 0.16f),
        )
        drawCircle(
            color = rotColor,
            radius = size * 0.04f,
            center = Offset(centerX - size * 0.30f, centerY - size * 0.36f),
        )
    }

    // Head elements with scaling
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.28f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth),
            )
        }

        // Head – pale elven face, oval/slightly elongated
        drawCircle(
            color = skinColor,
            radius = size * 0.28f,
            center = Offset(centerX, headCenterY),
        )

        // Vine crown: three short twisting vine arcs above the head
        val crownLeft =
            Path().apply {
                moveTo(centerX - size * 0.18f, headCenterY - size * 0.26f)
                cubicTo(
                    centerX - size * 0.26f,
                    headCenterY - size * 0.44f,
                    centerX - size * 0.12f,
                    headCenterY - size * 0.52f,
                    centerX - size * 0.06f,
                    headCenterY - size * 0.40f,
                )
            }
        val crownCenter =
            Path().apply {
                moveTo(centerX - size * 0.04f, headCenterY - size * 0.28f)
                cubicTo(
                    centerX - size * 0.02f,
                    headCenterY - size * 0.50f,
                    centerX + size * 0.06f,
                    headCenterY - size * 0.54f,
                    centerX + size * 0.08f,
                    headCenterY - size * 0.40f,
                )
            }
        val crownRight =
            Path().apply {
                moveTo(centerX + size * 0.14f, headCenterY - size * 0.24f)
                cubicTo(
                    centerX + size * 0.24f,
                    headCenterY - size * 0.42f,
                    centerX + size * 0.18f,
                    headCenterY - size * 0.50f,
                    centerX + size * 0.10f,
                    headCenterY - size * 0.38f,
                )
            }
        drawPath(crownLeft, vineColor, style = Stroke(width = size * 0.045f))
        drawPath(crownCenter, vineColor, style = Stroke(width = size * 0.045f))
        drawPath(crownRight, vineColor, style = Stroke(width = size * 0.045f))
        // Small rot-leaf circles at vine tips
        drawCircle(
            color = rotColor,
            radius = size * 0.04f,
            center = Offset(centerX - size * 0.06f, headCenterY - size * 0.40f),
        )
        drawCircle(
            color = rotColor,
            radius = size * 0.04f,
            center = Offset(centerX + size * 0.08f, headCenterY - size * 0.40f),
        )
        drawCircle(
            color = rotColor,
            radius = size * 0.04f,
            center = Offset(centerX + size * 0.10f, headCenterY - size * 0.38f),
        )

        // Sickly-green glowing eyes
        drawCircle(
            color = eyeColor,
            radius = size * 0.07f,
            center = Offset(centerX - size * 0.10f, headCenterY - size * 0.05f),
        )
        drawCircle(
            color = eyeColor,
            radius = size * 0.07f,
            center = Offset(centerX + size * 0.10f, headCenterY - size * 0.05f),
        )
        // Dark pupils
        drawCircle(
            color = Color(0xFF0A1A04),
            radius = size * 0.032f,
            center = Offset(centerX - size * 0.10f, headCenterY - size * 0.05f),
        )
        drawCircle(
            color = Color(0xFF0A1A04),
            radius = size * 0.032f,
            center = Offset(centerX + size * 0.10f, headCenterY - size * 0.05f),
        )

        // Small upturned nose (single dot)
        drawCircle(
            color = Color(0xFFB8A888),
            radius = size * 0.025f,
            center = Offset(centerX, headCenterY + size * 0.06f),
        )

        // Tight-lipped mouth (slight downward curve, giving a stern expression)
        val mouth =
            Path().apply {
                moveTo(centerX - size * 0.10f, headCenterY + size * 0.13f)
                quadraticTo(
                    centerX,
                    headCenterY + size * 0.17f,
                    centerX + size * 0.10f,
                    headCenterY + size * 0.13f,
                )
            }
        drawPath(mouth, Color(0xFF8A7A6A), style = Stroke(width = size * 0.025f))

        // Pointed ears, elf-style
        val leftEar =
            Path().apply {
                moveTo(centerX - size * 0.27f, headCenterY - size * 0.04f)
                lineTo(centerX - size * 0.42f, headCenterY - size * 0.16f)
                lineTo(centerX - size * 0.24f, headCenterY + size * 0.04f)
                close()
            }
        val rightEar =
            Path().apply {
                moveTo(centerX + size * 0.27f, headCenterY - size * 0.04f)
                lineTo(centerX + size * 0.42f, headCenterY - size * 0.16f)
                lineTo(centerX + size * 0.24f, headCenterY + size * 0.04f)
                close()
            }
        if (outlineColor != null) {
            drawPath(leftEar, outlineColor, style = Stroke(width = pathOutlineWidth))
            drawPath(rightEar, outlineColor, style = Stroke(width = pathOutlineWidth))
        }
        drawPath(leftEar, skinColor)
        drawPath(rightEar, skinColor)
    }
}
