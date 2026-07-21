package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw the Snotling Boss "Gribnak the Squealer": a small goblinoid rabble-leader wearing a
 * crooked golden crown. Distinct from the plain goblin so players can recognize the mini-boss.
 */
fun DrawScope.drawSnotlingBossSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val outlineWidth = 2f
    val pathOutlineWidth = 3f
    val headCenterY = centerY - size * 0.1f
    val skinColor = Color(0xFF6B8E23) // Darker olive green than a plain goblin

    // Body outline (not scaled, hidden in bighead mode)
    if (outlineColor != null && headScale == 1.0f) {
        val bodyPath =
            Path().apply {
                addRect(
                    androidx.compose.ui.geometry.Rect(
                        left = centerX - size * 0.18f,
                        top = centerY + size * 0.15f,
                        right = centerX + size * 0.18f,
                        bottom = centerY + size * 0.42f,
                    ),
                )
            }
        drawPath(bodyPath, outlineColor, style = Stroke(width = pathOutlineWidth))
    }

    // Head elements with scaling
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.32f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth),
            )
            val earOutline1 =
                Path().apply {
                    moveTo(centerX - size * 0.32f, centerY - size * 0.1f)
                    lineTo(centerX - size * 0.5f, centerY - size * 0.28f)
                    lineTo(centerX - size * 0.26f, centerY - size * 0.22f)
                    close()
                }
            val earOutline2 =
                Path().apply {
                    moveTo(centerX + size * 0.32f, centerY - size * 0.1f)
                    lineTo(centerX + size * 0.5f, centerY - size * 0.28f)
                    lineTo(centerX + size * 0.26f, centerY - size * 0.22f)
                    close()
                }
            drawPath(earOutline1, outlineColor, style = Stroke(width = pathOutlineWidth))
            drawPath(earOutline2, outlineColor, style = Stroke(width = pathOutlineWidth))
        }

        // Head (circle)
        drawCircle(
            color = skinColor,
            radius = size * 0.32f,
            center = Offset(centerX, headCenterY),
        )

        // Large pointy ears
        val ear1 =
            Path().apply {
                moveTo(centerX - size * 0.32f, centerY - size * 0.1f)
                lineTo(centerX - size * 0.5f, centerY - size * 0.28f)
                lineTo(centerX - size * 0.26f, centerY - size * 0.22f)
                close()
            }
        val ear2 =
            Path().apply {
                moveTo(centerX + size * 0.32f, centerY - size * 0.1f)
                lineTo(centerX + size * 0.5f, centerY - size * 0.28f)
                lineTo(centerX + size * 0.26f, centerY - size * 0.22f)
                close()
            }
        drawPath(ear1, skinColor)
        drawPath(ear2, skinColor)

        // Angry eyes
        drawCircle(color = Color.Yellow, radius = size * 0.06f, center = Offset(centerX - size * 0.11f, centerY - size * 0.15f))
        drawCircle(color = Color.Yellow, radius = size * 0.06f, center = Offset(centerX + size * 0.11f, centerY - size * 0.15f))
        drawCircle(color = Color.Black, radius = size * 0.03f, center = Offset(centerX - size * 0.11f, centerY - size * 0.15f))
        drawCircle(color = Color.Black, radius = size * 0.03f, center = Offset(centerX + size * 0.11f, centerY - size * 0.15f))

        // Crooked golden crown on top of the head
        val crownTop = headCenterY - size * 0.32f
        val crownBottom = headCenterY - size * 0.16f
        val crownLeft = centerX - size * 0.22f
        val crownRight = centerX + size * 0.22f
        val crown =
            Path().apply {
                moveTo(crownLeft, crownBottom)
                lineTo(crownLeft, crownTop + size * 0.05f)
                lineTo(centerX - size * 0.11f, crownBottom - size * 0.05f)
                lineTo(centerX, crownTop)
                lineTo(centerX + size * 0.11f, crownBottom - size * 0.05f)
                lineTo(crownRight, crownTop + size * 0.05f)
                lineTo(crownRight, crownBottom)
                close()
            }
        drawPath(crown, Color(0xFFFFD700)) // Gold
        if (outlineColor != null) {
            drawPath(crown, outlineColor, style = Stroke(width = outlineWidth))
        }
    }

    // Body (small, not scaled) - hidden in bighead mode
    if (headScale == 1.0f) {
        drawRect(
            color = Color(0xFF5C3317), // Dark brown
            topLeft = Offset(centerX - size * 0.18f, centerY + size * 0.15f),
            size = Size(size * 0.36f, size * 0.27f),
        )
    }
}
