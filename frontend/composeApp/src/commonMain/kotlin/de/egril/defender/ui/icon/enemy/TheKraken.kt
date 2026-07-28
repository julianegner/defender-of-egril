package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw The Kraken — an ancient deep-sea horror that haunts the waterways.
 *
 * The icon shows a massive cephalopod with a domed mantle, two large glowing eyes, and
 * curling tentacles. Dark abyss-teal tones with bioluminescent yellow eyes give the creature
 * a suitably eldritch feel distinct from all land-based villains.
 */
fun DrawScope.drawKrakenSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val pathOutlineWidth = 2.5f
    val outlineWidth = 2f

    // ── Colour palette ───────────────────────────────────────────────────────
    val mantleColor = Color(0xFF0E3342) // Deep abyss teal
    val mantleHighlight = Color(0xFF1A5070) // Slightly lighter teal highlight
    val tentacleColor = Color(0xFF0C2D3C) // Darker tentacle tone
    val suckerColor = Color(0xFF1E6080) // Sucker disc colour
    val eyeGlowColor = Color(0xFFD4E800) // Bioluminescent yellow-green glow
    val eyePupilColor = Color(0xFF080808) // Near-black slit pupil

    val headCenterY = centerY - size * 0.08f

    // ── Tentacles (drawn behind the body) ───────────────────────────────────
    if (headScale == 1.0f) {
        // Left tentacle (curling outward and down)
        val leftTentacle =
            Path().apply {
                moveTo(centerX - size * 0.18f, centerY + size * 0.12f)
                cubicTo(
                    centerX - size * 0.42f,
                    centerY + size * 0.18f,
                    centerX - size * 0.50f,
                    centerY + size * 0.38f,
                    centerX - size * 0.36f,
                    centerY + size * 0.46f,
                )
            }
        if (outlineColor != null) {
            drawPath(leftTentacle, outlineColor, style = Stroke(width = size * 0.13f + pathOutlineWidth))
        }
        drawPath(leftTentacle, tentacleColor, style = Stroke(width = size * 0.13f))

        // Right tentacle (curling the other way)
        val rightTentacle =
            Path().apply {
                moveTo(centerX + size * 0.18f, centerY + size * 0.12f)
                cubicTo(
                    centerX + size * 0.42f,
                    centerY + size * 0.18f,
                    centerX + size * 0.50f,
                    centerY + size * 0.38f,
                    centerX + size * 0.36f,
                    centerY + size * 0.46f,
                )
            }
        if (outlineColor != null) {
            drawPath(rightTentacle, outlineColor, style = Stroke(width = size * 0.13f + pathOutlineWidth))
        }
        drawPath(rightTentacle, tentacleColor, style = Stroke(width = size * 0.13f))

        // Centre tentacle (pointing straight down)
        val centreTentacle =
            Path().apply {
                moveTo(centerX, centerY + size * 0.16f)
                cubicTo(
                    centerX - size * 0.06f,
                    centerY + size * 0.30f,
                    centerX + size * 0.06f,
                    centerY + size * 0.38f,
                    centerX,
                    centerY + size * 0.48f,
                )
            }
        if (outlineColor != null) {
            drawPath(centreTentacle, outlineColor, style = Stroke(width = size * 0.11f + pathOutlineWidth))
        }
        drawPath(centreTentacle, tentacleColor, style = Stroke(width = size * 0.11f))

        // Sucker discs along the tentacles (small circles)
        val suckerPositions =
            listOf(
                Offset(centerX - size * 0.30f, centerY + size * 0.22f),
                Offset(centerX - size * 0.40f, centerY + size * 0.36f),
                Offset(centerX + size * 0.30f, centerY + size * 0.22f),
                Offset(centerX + size * 0.40f, centerY + size * 0.36f),
                Offset(centerX, centerY + size * 0.32f),
            )
        suckerPositions.forEach { pos ->
            drawCircle(color = suckerColor, radius = size * 0.04f, center = pos)
        }
    }

    // ── Mantle / body ────────────────────────────────────────────────────────
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        // Domed mantle (rounded oval tapering to a point at the top)
        val mantle =
            Path().apply {
                moveTo(centerX, headCenterY - size * 0.52f) // Crown / apex
                cubicTo(
                    centerX + size * 0.30f, headCenterY - size * 0.52f,
                    centerX + size * 0.38f, headCenterY - size * 0.10f,
                    centerX + size * 0.32f, headCenterY + size * 0.18f, // Lower right
                )
                cubicTo(
                    centerX + size * 0.24f, headCenterY + size * 0.30f,
                    centerX - size * 0.24f, headCenterY + size * 0.30f,
                    centerX - size * 0.32f, headCenterY + size * 0.18f, // Lower left
                )
                cubicTo(
                    centerX - size * 0.38f, headCenterY - size * 0.10f,
                    centerX - size * 0.30f, headCenterY - size * 0.52f,
                    centerX, headCenterY - size * 0.52f,
                )
                close()
            }
        if (outlineColor != null) {
            drawPath(mantle, outlineColor, style = Stroke(width = pathOutlineWidth))
        }
        drawPath(mantle, mantleColor)

        // Mantle stripe highlight (centre line from apex to mid-body)
        drawRect(
            color = mantleHighlight,
            topLeft = Offset(centerX - size * 0.04f, headCenterY - size * 0.50f),
            size = Size(size * 0.08f, size * 0.48f),
        )

        // ── Eyes ──────────────────────────────────────────────────────────────
        val eyeY = headCenterY - size * 0.10f
        val eyeRadius = size * 0.12f

        // Left eye glow
        drawCircle(color = eyeGlowColor, radius = eyeRadius * 1.15f, center = Offset(centerX - size * 0.14f, eyeY))
        // Left pupil (vertical slit)
        drawRect(
            color = eyePupilColor,
            topLeft = Offset(centerX - size * 0.17f, eyeY - eyeRadius * 0.75f),
            size = Size(size * 0.06f, eyeRadius * 1.50f),
        )

        // Right eye glow
        drawCircle(color = eyeGlowColor, radius = eyeRadius * 1.15f, center = Offset(centerX + size * 0.14f, eyeY))
        // Right pupil (vertical slit)
        drawRect(
            color = eyePupilColor,
            topLeft = Offset(centerX + size * 0.11f, eyeY - eyeRadius * 0.75f),
            size = Size(size * 0.06f, eyeRadius * 1.50f),
        )

        // Eye outline rings
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = eyeRadius * 1.15f,
                center = Offset(centerX - size * 0.14f, eyeY),
                style = Stroke(width = outlineWidth),
            )
            drawCircle(
                color = outlineColor,
                radius = eyeRadius * 1.15f,
                center = Offset(centerX + size * 0.14f, eyeY),
                style = Stroke(width = outlineWidth),
            )
        }

        // ── Beak / maw (subtle downward-curving slash) ───────────────────────
        val beakPath =
            Path().apply {
                moveTo(centerX - size * 0.12f, headCenterY + size * 0.12f)
                cubicTo(
                    centerX - size * 0.04f, headCenterY + size * 0.18f,
                    centerX + size * 0.04f, headCenterY + size * 0.18f,
                    centerX + size * 0.12f, headCenterY + size * 0.12f,
                )
            }
        drawPath(beakPath, eyeGlowColor, style = Stroke(width = size * 0.03f))
    }
}
