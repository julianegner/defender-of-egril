package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Morguk Bonewhisper, the Goblin Shaman villain.
 *
 * A dedicated icon distinct from a regular [drawGoblinSymbol]: Morguk is recognisable by his
 * bone-staff totem, orange-tinted skin (ritual war-paint), glowing yellow eyes and a small
 * skull-ornament on his head so players can immediately spot him in a goblin wave.
 */
fun DrawScope.drawMorgukBonewhisperSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val outlineWidth = 2f
    val pathOutlineWidth = 3f
    val headCenterY = centerY - size * 0.1f
    val shamanSkin = Color(0xFFB87333) // Copper/orange tinted skin – distinct from normal goblin green
    val robeColor = Color(0xFF4B0082) // Dark indigo robe
    val boneColor = Color(0xFFE8E1CF) // Bone-white for staff and skull ornament

    // Robe / body (not scaled, hidden in bighead mode)
    if (headScale == 1.0f) {
        if (outlineColor != null) {
            drawRect(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.18f, centerY + size * 0.12f),
                size = Size(size * 0.36f, size * 0.32f),
                style = Stroke(width = pathOutlineWidth),
            )
        }
        drawRect(
            color = robeColor,
            topLeft = Offset(centerX - size * 0.18f, centerY + size * 0.12f),
            size = Size(size * 0.36f, size * 0.32f),
        )

        // Bone-staff – held to the right of the body
        val staffLeft = centerX + size * 0.22f
        drawRect(
            color = boneColor,
            topLeft = Offset(staffLeft, centerY - size * 0.45f),
            size = Size(size * 0.06f, size * 0.75f),
        )
        // Skull on top of staff
        drawCircle(
            color = boneColor,
            radius = size * 0.1f,
            center = Offset(staffLeft + size * 0.03f, centerY - size * 0.55f),
        )
        // Skull eye sockets
        drawCircle(
            color = Color.Black,
            radius = size * 0.025f,
            center = Offset(staffLeft - size * 0.01f, centerY - size * 0.57f),
        )
        drawCircle(
            color = Color.Black,
            radius = size * 0.025f,
            center = Offset(staffLeft + size * 0.07f, centerY - size * 0.57f),
        )

        // Totem cross-beam on staff
        drawRect(
            color = boneColor,
            topLeft = Offset(staffLeft - size * 0.07f, centerY - size * 0.35f),
            size = Size(size * 0.2f, size * 0.04f),
        )
    }

    // Head elements with scaling
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.3f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth),
            )
            val earOutline1 =
                Path().apply {
                    moveTo(centerX - size * 0.3f, centerY - size * 0.1f)
                    lineTo(centerX - size * 0.45f, centerY - size * 0.25f)
                    lineTo(centerX - size * 0.25f, centerY - size * 0.2f)
                    close()
                }
            val earOutline2 =
                Path().apply {
                    moveTo(centerX + size * 0.3f, centerY - size * 0.1f)
                    lineTo(centerX + size * 0.45f, centerY - size * 0.25f)
                    lineTo(centerX + size * 0.25f, centerY - size * 0.2f)
                    close()
                }
            drawPath(earOutline1, outlineColor, style = Stroke(width = pathOutlineWidth))
            drawPath(earOutline2, outlineColor, style = Stroke(width = pathOutlineWidth))
        }

        // Head (copper-orange tinted, goblin-proportioned)
        drawCircle(
            color = shamanSkin,
            radius = size * 0.3f,
            center = Offset(centerX, headCenterY),
        )

        // Pointy ears
        val ear1 =
            Path().apply {
                moveTo(centerX - size * 0.3f, centerY - size * 0.1f)
                lineTo(centerX - size * 0.45f, centerY - size * 0.25f)
                lineTo(centerX - size * 0.25f, centerY - size * 0.2f)
                close()
            }
        val ear2 =
            Path().apply {
                moveTo(centerX + size * 0.3f, centerY - size * 0.1f)
                lineTo(centerX + size * 0.45f, centerY - size * 0.25f)
                lineTo(centerX + size * 0.25f, centerY - size * 0.2f)
                close()
            }
        drawPath(ear1, shamanSkin)
        drawPath(ear2, shamanSkin)

        // Glowing yellow shaman eyes
        drawCircle(
            color = Color(0xFFFFD700),
            radius = size * 0.06f,
            center = Offset(centerX - size * 0.1f, centerY - size * 0.15f),
        )
        drawCircle(
            color = Color(0xFFFFD700),
            radius = size * 0.06f,
            center = Offset(centerX + size * 0.1f, centerY - size * 0.15f),
        )
        // Pupils
        drawCircle(
            color = Color.Black,
            radius = size * 0.03f,
            center = Offset(centerX - size * 0.1f, centerY - size * 0.15f),
        )
        drawCircle(
            color = Color.Black,
            radius = size * 0.03f,
            center = Offset(centerX + size * 0.1f, centerY - size * 0.15f),
        )

        // Small skull-bone headpiece (totem ornament above forehead)
        drawCircle(
            color = boneColor,
            radius = size * 0.09f,
            center = Offset(centerX, headCenterY - size * 0.32f),
        )
        // Tiny skull eye sockets on the headpiece
        drawCircle(
            color = Color(0xFF4B0082),
            radius = size * 0.025f,
            center = Offset(centerX - size * 0.035f, headCenterY - size * 0.34f),
        )
        drawCircle(
            color = Color(0xFF4B0082),
            radius = size * 0.025f,
            center = Offset(centerX + size * 0.035f, headCenterY - size * 0.34f),
        )
    }
}
