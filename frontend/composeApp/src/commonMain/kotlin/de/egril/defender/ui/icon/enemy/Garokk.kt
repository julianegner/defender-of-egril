package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Garokk the Skullsplitter, the Horde warchief villain.
 *
 * A dedicated icon that is deliberately distinct from a regular [drawOrkSymbol]: a massive colossus
 * clad in black iron with a horned war helm, glowing red eyes and heavy tusks, so the villain is
 * easy to tell apart from the ordinary Orks he leads.
 */
fun DrawScope.drawGarokkSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val ironDark = Color(0xFF2B2B2B) // Black iron armor
    val ironMid = Color(0xFF4A4A4A) // Lighter iron highlight
    val skinColor = Color(0xFF3B4A22) // Dark, almost black olive skin
    val headCenterY = centerY - size * 0.125f
    val pathOutlineWidth = 3f

    // Body armor (large, not scaled)
    if (outlineColor != null) {
        drawRect(
            color = outlineColor,
            topLeft = Offset(centerX - size * 0.3f, centerY + size * 0.08f),
            size = Size(size * 0.6f, size * 0.34f),
            style = Stroke(width = pathOutlineWidth),
        )
    }
    drawRect(
        color = ironDark,
        topLeft = Offset(centerX - size * 0.3f, centerY + size * 0.08f),
        size = Size(size * 0.6f, size * 0.34f),
    )
    // Chest plate highlight
    drawRect(
        color = ironMid,
        topLeft = Offset(centerX - size * 0.1f, centerY + size * 0.12f),
        size = Size(size * 0.2f, size * 0.26f),
    )

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        // Horned war helm (two curved horns above the head)
        val leftHorn =
            Path().apply {
                moveTo(centerX - size * 0.22f, centerY - size * 0.28f)
                lineTo(centerX - size * 0.42f, centerY - size * 0.5f)
                lineTo(centerX - size * 0.12f, centerY - size * 0.34f)
                close()
            }
        val rightHorn =
            Path().apply {
                moveTo(centerX + size * 0.22f, centerY - size * 0.28f)
                lineTo(centerX + size * 0.42f, centerY - size * 0.5f)
                lineTo(centerX + size * 0.12f, centerY - size * 0.34f)
                close()
            }
        drawPath(leftHorn, Color(0xFFE8E1CF)) // Bone-white horns
        drawPath(rightHorn, Color(0xFFE8E1CF))

        // Head (dark olive)
        if (outlineColor != null) {
            drawRect(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.27f, centerY - size * 0.32f),
                size = Size(size * 0.54f, size * 0.37f),
                style = Stroke(width = pathOutlineWidth),
            )
        }
        drawRect(
            color = skinColor,
            topLeft = Offset(centerX - size * 0.27f, centerY - size * 0.32f),
            size = Size(size * 0.54f, size * 0.37f),
        )

        // Iron helm band across the brow
        drawRect(
            color = ironMid,
            topLeft = Offset(centerX - size * 0.27f, centerY - size * 0.32f),
            size = Size(size * 0.54f, size * 0.1f),
        )

        // Glowing red eyes
        drawCircle(color = Color(0xFFFF2A2A), radius = size * 0.07f, center = Offset(centerX - size * 0.13f, centerY - size * 0.17f))
        drawCircle(color = Color(0xFFFF2A2A), radius = size * 0.07f, center = Offset(centerX + size * 0.13f, centerY - size * 0.17f))

        // Heavy tusks
        val tuskLeft =
            Path().apply {
                moveTo(centerX - size * 0.16f, centerY - size * 0.02f)
                lineTo(centerX - size * 0.28f, centerY + size * 0.17f)
                lineTo(centerX - size * 0.08f, centerY + size * 0.05f)
                close()
            }
        val tuskRight =
            Path().apply {
                moveTo(centerX + size * 0.16f, centerY - size * 0.02f)
                lineTo(centerX + size * 0.28f, centerY + size * 0.17f)
                lineTo(centerX + size * 0.08f, centerY + size * 0.05f)
                close()
            }
        drawPath(tuskLeft, Color.White)
        drawPath(tuskRight, Color.White)
    }
}
