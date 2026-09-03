package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draw a regular pirate enemy symbol.
 *
 * The pirate deliberately reuses the same base silhouette as Cap'n Roderich, but adds a distinct
 * red bandana and a gold earring so players can differentiate the regular unit from the villain.
 */
fun DrawScope.drawPirateSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
    showBarge: Boolean = false,
) {
    drawCaptainRoderichSymbol(
        centerX = centerX,
        centerY = centerY,
        size = size,
        outlineColor = outlineColor,
        headScale = headScale,
        showBarge = showBarge,
    )

    // Mirror the same upward shift that CaptainRoderich applies to the head when on a barge,
    // so the bandana stays on the forehead rather than drifting down to eye level.
    val renderCenterY = if (showBarge) centerY - size * 0.05f else centerY

    // Distinctive red bandana across the forehead.
    drawRect(
        color = Color(0xFF9E1A1A),
        topLeft = Offset(centerX - size * 0.20f, renderCenterY - size * 0.34f),
        size = Size(size * 0.40f, size * 0.08f),
    )
    if (outlineColor != null) {
        drawRect(
            color = outlineColor,
            topLeft = Offset(centerX - size * 0.20f, renderCenterY - size * 0.34f),
            size = Size(size * 0.40f, size * 0.08f),
            style =
                androidx.compose.ui.graphics.drawscope
                    .Stroke(width = 2f),
        )
    }

    // Bandana knot/tail.
    val knotPath =
        Path().apply {
            moveTo(centerX + size * 0.20f, renderCenterY - size * 0.30f)
            lineTo(centerX + size * 0.30f, renderCenterY - size * 0.26f)
            lineTo(centerX + size * 0.20f, renderCenterY - size * 0.22f)
            close()
        }
    drawPath(knotPath, Color(0xFF9E1A1A))
    if (outlineColor != null) {
        drawPath(
            knotPath,
            outlineColor,
            style =
                androidx.compose.ui.graphics.drawscope
                    .Stroke(width = 2f),
        )
    }

    // Gold earring — positioned relative to face center.
    val headCenterY = renderCenterY - size * 0.12f
    drawCircle(
        color = Color(0xFFD4A017),
        radius = size * 0.028f,
        center = Offset(centerX + size * 0.17f, headCenterY + size * 0.10f),
    )
}
