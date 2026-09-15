package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Silas the Maskmaster, an illusionist villain who hides behind a theatrical mask and cloak.
 */
fun DrawScope.drawSilasSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val cloakColor = Color(0xFF3A2459)
    val cloakHighlight = Color(0xFF5D3A8C)
    val maskColor = Color(0xFFF3F0E6)
    val eyeColor = Color(0xFF22D3EE)
    val trimColor = Color(0xFFB38CFF)
    val pathOutlineWidth = 3f
    val headCenterY = centerY - size * 0.1f

    if (headScale == 1.0f) {
        val cloak =
            Path().apply {
                moveTo(centerX, centerY + size * 0.02f)
                lineTo(centerX - size * 0.32f, centerY + size * 0.42f)
                lineTo(centerX + size * 0.32f, centerY + size * 0.42f)
                close()
            }
        if (outlineColor != null) {
            drawPath(cloak, outlineColor, style = Stroke(width = pathOutlineWidth))
        }
        drawPath(cloak, cloakColor)
        drawRect(
            color = cloakHighlight,
            topLeft = Offset(centerX - size * 0.07f, centerY + size * 0.12f),
            size = Size(size * 0.14f, size * 0.2f),
        )
    }

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        val hood =
            Path().apply {
                moveTo(centerX - size * 0.28f, headCenterY + size * 0.12f)
                lineTo(centerX - size * 0.18f, headCenterY - size * 0.3f)
                lineTo(centerX, headCenterY - size * 0.42f)
                lineTo(centerX + size * 0.18f, headCenterY - size * 0.3f)
                lineTo(centerX + size * 0.28f, headCenterY + size * 0.12f)
                close()
            }
        if (outlineColor != null) {
            drawPath(hood, outlineColor, style = Stroke(width = pathOutlineWidth))
        }
        drawPath(hood, cloakColor)

        val mask =
            Path().apply {
                moveTo(centerX, headCenterY - size * 0.24f)
                lineTo(centerX - size * 0.19f, headCenterY - size * 0.08f)
                lineTo(centerX - size * 0.12f, headCenterY + size * 0.18f)
                lineTo(centerX, headCenterY + size * 0.28f)
                lineTo(centerX + size * 0.12f, headCenterY + size * 0.18f)
                lineTo(centerX + size * 0.19f, headCenterY - size * 0.08f)
                close()
            }
        if (outlineColor != null) {
            drawPath(mask, outlineColor, style = Stroke(width = pathOutlineWidth))
        }
        drawPath(mask, maskColor)

        drawCircle(
            color = eyeColor,
            radius = size * 0.05f,
            center = Offset(centerX - size * 0.075f, headCenterY - size * 0.035f),
        )
        drawCircle(
            color = eyeColor,
            radius = size * 0.05f,
            center = Offset(centerX + size * 0.075f, headCenterY - size * 0.035f),
        )
        drawCircle(
            color = Color.Black,
            radius = size * 0.022f,
            center = Offset(centerX - size * 0.075f, headCenterY - size * 0.035f),
        )
        drawCircle(
            color = Color.Black,
            radius = size * 0.022f,
            center = Offset(centerX + size * 0.075f, headCenterY - size * 0.035f),
        )

        drawLine(
            color = trimColor,
            start = Offset(centerX - size * 0.16f, headCenterY - size * 0.18f),
            end = Offset(centerX + size * 0.16f, headCenterY - size * 0.18f),
            strokeWidth = size * 0.04f,
        )
        drawLine(
            color = trimColor,
            start = Offset(centerX - size * 0.08f, headCenterY + size * 0.09f),
            end = Offset(centerX + size * 0.08f, headCenterY + size * 0.09f),
            strokeWidth = size * 0.03f,
        )
    }
}
