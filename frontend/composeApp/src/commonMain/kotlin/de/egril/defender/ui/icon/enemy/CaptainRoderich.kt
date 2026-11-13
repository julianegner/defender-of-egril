package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import de.egril.defender.ui.drawRaftBase

/**
 * Draw Cap'n Roderich, Scourge of the Seas — a notorious seafaring pirate villain.
 *
 * His icon shows a sun-weathered pirate captain clad in a tricorn hat adorned with a skull-and-
 * crossbones badge, a long weathered coat, and a golden eye-patch. The look is deliberately
 * distinct from any regular enemy: rich navy and gold tones with a roguish smirk place him firmly
 * in the seafarer archetype.
 */
fun DrawScope.drawCaptainRoderichSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
    showBarge: Boolean = false,
) {
    val pathOutlineWidth = 2.5f
    val outlineWidth = 2f
    val renderCenterY = if (showBarge) centerY - size * 0.05f else centerY

    // ---- Colour palette ----
    val coatColor = Color(0xFF1A3A5C) // Deep navy coat
    val coatHighlight = Color(0xFF2A5A8C) // Lighter navy highlight
    val goldColor = Color(0xFFD4A017) // Pirate gold (epaulettes, buttons, eye-patch)
    val skinColor = Color(0xFFC8975A) // Sun-weathered tan skin
    val hatColor = Color(0xFF0D1F2D) // Near-black tricorn
    val hatBand = Color(0xFFD4A017) // Gold hat-band
    val scullColor = Color(0xFFE8E1CF) // Bone-white skull badge
    val beardColor = Color(0xFF4A3520) // Dark brown beard
    val eyePatchColor = Color(0xFFD4A017) // Gold eye-patch

    val headCenterY = renderCenterY - size * 0.12f

    if (showBarge) {
        drawRaftBase(
            centerX = centerX,
            centerY = centerY + size * 0.34f,
            size = size * 1.20f,
        )
    }

    // ---- Coat / body (not scaled) ----
    if (headScale == 1.0f) {
        // Main coat body
        if (outlineColor != null) {
            drawRect(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.28f, renderCenterY + size * 0.07f),
                size = Size(size * 0.56f, size * 0.38f),
                style = Stroke(width = pathOutlineWidth),
            )
        }
        drawRect(
            color = coatColor,
            topLeft = Offset(centerX - size * 0.28f, renderCenterY + size * 0.07f),
            size = Size(size * 0.56f, size * 0.38f),
        )
        // Coat front opening (V-shape, slightly lighter)
        drawRect(
            color = coatHighlight,
            topLeft = Offset(centerX - size * 0.06f, renderCenterY + size * 0.07f),
            size = Size(size * 0.12f, size * 0.24f),
        )

        // Gold epaulette (left shoulder)
        drawRect(
            color = goldColor,
            topLeft = Offset(centerX - size * 0.36f, renderCenterY + size * 0.07f),
            size = Size(size * 0.12f, size * 0.08f),
        )
        // Gold epaulette (right shoulder)
        drawRect(
            color = goldColor,
            topLeft = Offset(centerX + size * 0.24f, renderCenterY + size * 0.07f),
            size = Size(size * 0.12f, size * 0.08f),
        )

        // Gold coat buttons (3 down the centre)
        for (i in 0..2) {
            drawCircle(
                color = goldColor,
                radius = size * 0.03f,
                center = Offset(centerX, renderCenterY + size * (0.13f + i * 0.1f)),
            )
        }

        // Cutlass handle peeking from left hip
        val cutlassPath =
            Path().apply {
                moveTo(centerX - size * 0.28f, renderCenterY + size * 0.28f)
                lineTo(centerX - size * 0.44f, renderCenterY + size * 0.44f)
            }
        drawPath(cutlassPath, Color(0xFF8B7355), style = Stroke(width = size * 0.055f))
        // Cutlass guard
        drawRect(
            color = goldColor,
            topLeft = Offset(centerX - size * 0.36f, renderCenterY + size * 0.32f),
            size = Size(size * 0.14f, size * 0.04f),
        )
    }

    // ---- Head elements (scaled for big-head mode) ----
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        // Tricorn hat (three-cornered pirate hat)
        val brimLeft = centerX - size * 0.38f
        val brimRight = centerX + size * 0.38f
        val brimY = headCenterY - size * 0.22f
        val hatTop = headCenterY - size * 0.42f

        // Hat brim (wide flat base)
        drawRect(
            color = hatColor,
            topLeft = Offset(brimLeft, brimY),
            size = Size(brimRight - brimLeft, size * 0.08f),
        )
        if (outlineColor != null) {
            drawRect(
                color = outlineColor,
                topLeft = Offset(brimLeft, brimY),
                size = Size(brimRight - brimLeft, size * 0.08f),
                style = Stroke(width = outlineWidth),
            )
        }

        // Hat crown (trapezoid body rising to a narrower top)
        val crownPath =
            Path().apply {
                moveTo(brimLeft + size * 0.08f, brimY)
                lineTo(centerX - size * 0.22f, hatTop + size * 0.04f)
                lineTo(centerX + size * 0.22f, hatTop + size * 0.04f)
                lineTo(brimRight - size * 0.08f, brimY)
                close()
            }
        drawPath(crownPath, hatColor)
        if (outlineColor != null) {
            drawPath(crownPath, outlineColor, style = Stroke(width = outlineWidth))
        }

        // Tricorn peak (left corner folded up)
        val leftPeak =
            Path().apply {
                moveTo(brimLeft, brimY + size * 0.04f)
                lineTo(brimLeft - size * 0.06f, brimY - size * 0.14f)
                lineTo(brimLeft + size * 0.14f, brimY)
                close()
            }
        val rightPeak =
            Path().apply {
                moveTo(brimRight, brimY + size * 0.04f)
                lineTo(brimRight + size * 0.06f, brimY - size * 0.14f)
                lineTo(brimRight - size * 0.14f, brimY)
                close()
            }
        drawPath(leftPeak, hatColor)
        drawPath(rightPeak, hatColor)
        if (outlineColor != null) {
            drawPath(leftPeak, outlineColor, style = Stroke(width = outlineWidth))
            drawPath(rightPeak, outlineColor, style = Stroke(width = outlineWidth))
        }

        // Gold hat-band
        drawRect(
            color = hatBand,
            topLeft = Offset(brimLeft + size * 0.08f, brimY - size * 0.02f),
            size = Size((brimRight - brimLeft) - size * 0.16f, size * 0.05f),
        )

        // Skull-and-crossbones badge on the hat band (simple stylised version)
        val badgeCX = centerX
        val badgeCY = brimY + size * 0.01f
        // Skull circle
        drawCircle(color = scullColor, radius = size * 0.055f, center = Offset(badgeCX, badgeCY))
        // Crossed bones (two short diagonal lines)
        drawLine(
            color = scullColor,
            start = Offset(badgeCX - size * 0.07f, badgeCY - size * 0.04f),
            end = Offset(badgeCX + size * 0.07f, badgeCY + size * 0.04f),
            strokeWidth = size * 0.02f,
        )
        drawLine(
            color = scullColor,
            start = Offset(badgeCX + size * 0.07f, badgeCY - size * 0.04f),
            end = Offset(badgeCX - size * 0.07f, badgeCY + size * 0.04f),
            strokeWidth = size * 0.02f,
        )

        // ---- Face / head ----
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.27f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth),
            )
        }
        drawCircle(color = skinColor, radius = size * 0.27f, center = Offset(centerX, headCenterY))

        // Weather-worn beard / stubble (lower arc of face)
        val beardPath =
            Path().apply {
                moveTo(centerX - size * 0.26f, headCenterY + size * 0.05f)
                quadraticTo(
                    centerX,
                    headCenterY + size * 0.35f,
                    centerX + size * 0.26f,
                    headCenterY + size * 0.05f,
                )
                quadraticTo(
                    centerX,
                    headCenterY + size * 0.22f,
                    centerX - size * 0.26f,
                    headCenterY + size * 0.05f,
                )
                close()
            }
        drawPath(beardPath, beardColor)

        // Left eye (normal, slightly squinting)
        drawCircle(
            color = Color(0xFF2B1A09),
            radius = size * 0.055f,
            center = Offset(centerX - size * 0.10f, headCenterY - size * 0.05f),
        )
        // Right eye: gold eye-patch
        val patchPath =
            Path().apply {
                moveTo(centerX + size * 0.02f, headCenterY - size * 0.10f)
                lineTo(centerX + size * 0.24f, headCenterY - size * 0.10f)
                lineTo(centerX + size * 0.22f, headCenterY + size * 0.01f)
                lineTo(centerX + size * 0.04f, headCenterY + size * 0.01f)
                close()
            }
        drawPath(patchPath, eyePatchColor)
        // Eye-patch strap going up toward hat
        drawLine(
            color = eyePatchColor,
            start = Offset(centerX + size * 0.13f, headCenterY - size * 0.10f),
            end = Offset(centerX + size * 0.16f, headCenterY - size * 0.24f),
            strokeWidth = size * 0.025f,
        )

        // Nose (simple bump)
        drawCircle(
            color = Color(0xFFB07840),
            radius = size * 0.028f,
            center = Offset(centerX, headCenterY + size * 0.05f),
        )

        // Smirk (asymmetric upward curve on left side only)
        val smirkPath =
            Path().apply {
                moveTo(centerX - size * 0.14f, headCenterY + size * 0.13f)
                quadraticTo(
                    centerX - size * 0.04f,
                    headCenterY + size * 0.18f,
                    centerX + size * 0.04f,
                    headCenterY + size * 0.14f,
                )
            }
        drawPath(smirkPath, Color(0xFF7A4820), style = Stroke(width = size * 0.025f))
    }
}
