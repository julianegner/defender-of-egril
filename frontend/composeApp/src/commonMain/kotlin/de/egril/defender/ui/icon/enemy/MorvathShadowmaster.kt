package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Morvath the Shadowmaster as a hooded dark sorcerer shrouded in violet shadow.
 *
 * Unlike dragon villains, Morvath is represented by a robed spellcaster silhouette with glowing
 * violet eyes and a shadow staff orb to make him visually distinct from Xarithon.
 */
fun DrawScope.drawMorvathShadowmasterSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val outlineWidth = 2f
    val pathOutlineWidth = 2.5f
    val headCenterY = centerY + size * 0.10f

    val hoodColor = Color(0xFF16081F)
    val robeColor = Color(0xFF241032)
    val faceColor = Color(0xFFBFA9CF)
    val eyeGlowColor = Color(0xFFB400FF)
    val auraColor = Color(0xFF6A00AA).copy(alpha = 0.28f)
    val staffColor = Color(0xFF4A2E1F)
    val orbColor = Color(0xFF7A1BFF)
    val mouthColor = Color(0xFF8C7599)

    drawCircle(
        color = auraColor,
        radius = size * 0.46f,
        center = Offset(centerX, centerY),
    )

    if (headScale == 1.0f) {
        val robePath =
            Path().apply {
                moveTo(centerX, centerY - size * 0.02f)
                lineTo(centerX - size * 0.26f, centerY + size * 0.38f)
                lineTo(centerX + size * 0.26f, centerY + size * 0.38f)
                close()
            }
        if (outlineColor != null) {
            drawPath(robePath, outlineColor, style = Stroke(width = pathOutlineWidth))
        }
        drawPath(robePath, robeColor)

        val staffStart = Offset(centerX - size * 0.24f, centerY + size * 0.04f)
        val staffEnd = Offset(centerX - size * 0.34f, centerY + size * 0.40f)
        if (outlineColor != null) {
            drawLine(
                color = outlineColor,
                start = staffStart,
                end = staffEnd,
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = outlineColor,
                radius = size * 0.08f + outlineWidth / 2,
                center = Offset(centerX - size * 0.22f, centerY - size * 0.02f),
                style = Stroke(width = outlineWidth),
            )
        }
        drawLine(
            color = staffColor,
            start = staffStart,
            end = staffEnd,
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = orbColor.copy(alpha = 0.35f),
            radius = size * 0.11f,
            center = Offset(centerX - size * 0.22f, centerY - size * 0.02f),
        )
        drawCircle(
            color = orbColor,
            radius = size * 0.07f,
            center = Offset(centerX - size * 0.22f, centerY - size * 0.02f),
        )
    }

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        val hoodPath =
            Path().apply {
                moveTo(centerX, centerY - size * 0.42f)
                quadraticTo(
                    centerX - size * 0.24f,
                    centerY - size * 0.26f,
                    centerX - size * 0.26f,
                    centerY - size * 0.02f,
                )
                lineTo(centerX - size * 0.16f, centerY + size * 0.12f)
                lineTo(centerX + size * 0.16f, centerY + size * 0.12f)
                lineTo(centerX + size * 0.26f, centerY - size * 0.02f)
                quadraticTo(
                    centerX + size * 0.24f,
                    centerY - size * 0.26f,
                    centerX,
                    centerY - size * 0.42f,
                )
                close()
            }
        if (outlineColor != null) {
            drawPath(hoodPath, outlineColor, style = Stroke(width = pathOutlineWidth))
            drawCircle(
                color = outlineColor,
                radius = size * 0.16f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth),
            )
        }
        drawPath(hoodPath, hoodColor)

        drawCircle(
            color = faceColor,
            radius = size * 0.16f,
            center = Offset(centerX, headCenterY),
        )

        drawCircle(
            color = eyeGlowColor.copy(alpha = 0.35f),
            radius = size * 0.06f,
            center = Offset(centerX - size * 0.07f, headCenterY - size * 0.02f),
        )
        drawCircle(
            color = eyeGlowColor.copy(alpha = 0.35f),
            radius = size * 0.06f,
            center = Offset(centerX + size * 0.07f, headCenterY - size * 0.02f),
        )
        drawCircle(
            color = eyeGlowColor,
            radius = size * 0.035f,
            center = Offset(centerX - size * 0.07f, headCenterY - size * 0.02f),
        )
        drawCircle(
            color = eyeGlowColor,
            radius = size * 0.035f,
            center = Offset(centerX + size * 0.07f, headCenterY - size * 0.02f),
        )

        drawLine(
            color = mouthColor,
            start = Offset(centerX - size * 0.05f, headCenterY + size * 0.08f),
            end = Offset(centerX + size * 0.05f, headCenterY + size * 0.08f),
            strokeWidth = size * 0.02f,
            cap = StrokeCap.Round,
        )

        drawRect(
            color = hoodColor,
            topLeft = Offset(centerX - size * 0.24f, centerY + size * 0.10f),
            size = Size(size * 0.48f, size * 0.06f),
        )
    }
}
