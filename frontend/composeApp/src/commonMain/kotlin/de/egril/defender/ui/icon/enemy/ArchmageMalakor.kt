package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Archmage Malakor the Renegade: a powerful mage corrupted by forbidden astral magic.
 *
 * He floats meditating above the ground. His icon shows a gaunt, pale-skinned face framed by a
 * tall, starfield-dark wizard hat adorned with astral runes, glowing white-blue eyes consumed by
 * forbidden magic, and a deep void-blue robe trailing faint astral energy wisps. A glowing astral
 * orb hovers above the tip of his hat, marking him as unmistakably distinct from the plain
 * EvilWizard icon.
 */
fun DrawScope.drawArchmageMalakorSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val outlineWidth = 2f
    val pathOutlineWidth = 2.5f
    val headCenterY = centerY + size * 0.12f

    val robeColor = Color(0xFF0D1B3E)       // Deep void-blue robe
    val hatColor = Color(0xFF0A0F2E)        // Near-black astral hat
    val skinColor = Color(0xFFD8D0C8)       // Gaunt, pale skin
    val eyeColor = Color(0xFFB0C8FF)        // Cold astral-white/blue glow
    val orbColor = Color(0xFF7FAAFF)        // Bright astral orb
    val runeColor = Color(0xFF3A5A9A)       // Subtle rune marks on hat
    val wispColor = Color(0xFF5580CC)       // Faint energy wisps

    // --- Robe / body (not scaled) ---
    if (headScale == 1.0f) {
        // Main robe body
        if (outlineColor != null) {
            drawRect(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.22f, centerY + size * 0.08f),
                size = Size(size * 0.44f, size * 0.40f),
                style = Stroke(width = pathOutlineWidth),
            )
        }
        drawRect(
            color = robeColor,
            topLeft = Offset(centerX - size * 0.22f, centerY + size * 0.08f),
            size = Size(size * 0.44f, size * 0.40f),
        )

        // Floating wisps trailing downward from the robe (astral energy)
        val leftWisp =
            Path().apply {
                moveTo(centerX - size * 0.14f, centerY + size * 0.46f)
                quadraticTo(
                    centerX - size * 0.22f, centerY + size * 0.56f,
                    centerX - size * 0.18f, centerY + size * 0.64f,
                )
            }
        val rightWisp =
            Path().apply {
                moveTo(centerX + size * 0.10f, centerY + size * 0.46f)
                quadraticTo(
                    centerX + size * 0.20f, centerY + size * 0.56f,
                    centerX + size * 0.14f, centerY + size * 0.64f,
                )
            }
        drawPath(leftWisp, wispColor, style = Stroke(width = size * 0.035f))
        drawPath(rightWisp, wispColor, style = Stroke(width = size * 0.035f))

        // Astral orb / staff in left hand
        val staffX = centerX - size * 0.30f
        if (outlineColor != null) {
            drawLine(
                color = outlineColor,
                start = Offset(staffX, centerY + size * 0.10f),
                end = Offset(staffX, centerY + size * 0.42f),
                strokeWidth = pathOutlineWidth + 1.5f,
            )
            drawCircle(
                color = outlineColor,
                radius = size * 0.06f + outlineWidth / 2,
                center = Offset(staffX, centerY + size * 0.06f),
                style = Stroke(width = outlineWidth),
            )
        }
        drawLine(
            color = runeColor,
            start = Offset(staffX, centerY + size * 0.10f),
            end = Offset(staffX, centerY + size * 0.42f),
            strokeWidth = 2.5f,
        )
        drawCircle(
            color = orbColor,
            radius = size * 0.06f,
            center = Offset(staffX, centerY + size * 0.06f),
        )
    }

    // --- Head elements with scaling ---
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        // Tall wizard hat (higher and narrower than EvilWizard's hat)
        val hatPath =
            Path().apply {
                moveTo(centerX, centerY - size * 0.55f) // Taller peak
                lineTo(centerX - size * 0.22f, centerY - size * 0.05f)
                lineTo(centerX + size * 0.22f, centerY - size * 0.05f)
                close()
            }
        if (outlineColor != null) {
            drawPath(hatPath, outlineColor, style = Stroke(width = pathOutlineWidth))
        }
        drawPath(hatPath, hatColor)

        // Astral rune band on hat (horizontal stripe near brim)
        drawRect(
            color = runeColor,
            topLeft = Offset(centerX - size * 0.22f, centerY - size * 0.14f),
            size = Size(size * 0.44f, size * 0.06f),
        )

        // Hat brim
        if (outlineColor != null) {
            drawRect(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.28f, centerY - size * 0.06f),
                size = Size(size * 0.56f, size * 0.07f),
                style = Stroke(width = pathOutlineWidth),
            )
        }
        drawRect(
            color = hatColor,
            topLeft = Offset(centerX - size * 0.28f, centerY - size * 0.06f),
            size = Size(size * 0.56f, size * 0.07f),
        )

        // Glowing astral orb at hat tip
        drawCircle(
            color = orbColor.copy(alpha = 0.5f),
            radius = size * 0.07f,
            center = Offset(centerX, centerY - size * 0.55f),
        )
        drawCircle(
            color = orbColor,
            radius = size * 0.045f,
            center = Offset(centerX, centerY - size * 0.55f),
        )

        // Face (gaunt, slightly elongated)
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.22f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth),
            )
        }
        drawCircle(
            color = skinColor,
            radius = size * 0.22f,
            center = Offset(centerX, headCenterY),
        )

        // Cold, glowing astral eyes
        drawCircle(
            color = eyeColor,
            radius = size * 0.065f,
            center = Offset(centerX - size * 0.09f, headCenterY - size * 0.04f),
        )
        drawCircle(
            color = eyeColor,
            radius = size * 0.065f,
            center = Offset(centerX + size * 0.09f, headCenterY - size * 0.04f),
        )
        // Bright inner pupils
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = size * 0.025f,
            center = Offset(centerX - size * 0.09f, headCenterY - size * 0.04f),
        )
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = size * 0.025f,
            center = Offset(centerX + size * 0.09f, headCenterY - size * 0.04f),
        )

        // Small nose
        drawCircle(
            color = Color(0xFFBBB5AE),
            radius = size * 0.022f,
            center = Offset(centerX, headCenterY + size * 0.06f),
        )

        // Stern thin-lipped mouth
        val mouth =
            Path().apply {
                moveTo(centerX - size * 0.09f, headCenterY + size * 0.12f)
                lineTo(centerX + size * 0.09f, headCenterY + size * 0.12f)
            }
        drawPath(mouth, Color(0xFF9A9090), style = Stroke(width = size * 0.02f))

        // Short beard — thin strokes below chin
        val beard =
            Path().apply {
                moveTo(centerX - size * 0.06f, headCenterY + size * 0.20f)
                quadraticTo(
                    centerX, headCenterY + size * 0.28f,
                    centerX + size * 0.06f, headCenterY + size * 0.20f,
                )
            }
        drawPath(beard, Color(0xFFCCCCCC), style = Stroke(width = size * 0.022f))
    }
}
