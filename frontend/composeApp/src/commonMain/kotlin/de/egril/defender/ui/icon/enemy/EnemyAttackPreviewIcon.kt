package de.egril.defender.ui.icon.enemy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Precomputed information for the enemy attack preview (issue #591).
 *
 * @param damage the damage a single attack from the selected defender would deal.
 * @param isLethal whether that attack would defeat the enemy (damage >= current health).
 * @param isImmune whether the enemy is immune to the selected defender's attack type.
 */
data class EnemyAttackPreview(
    val damage: Int,
    val isLethal: Boolean,
    val isImmune: Boolean,
)

/**
 * Damage / lethality preview shown at the left border of an enemy unit when a defender is
 * selected that could attack it.
 *
 * - [isImmune] takes priority and shows a light grey shield icon (the enemy cannot be damaged
 *   by the selected defender's attack type).
 * - Otherwise, if [isLethal] (the attack would defeat the enemy) a white skull with red outline
 *   and red facial features is shown.
 * - Otherwise the [damage] value is shown in red.
 */
@Composable
fun EnemyAttackPreviewIcon(
    damage: Int,
    isLethal: Boolean,
    isImmune: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    when {
        isImmune ->
            Canvas(modifier = modifier.size(size)) {
                drawImmunityShield()
            }
        isLethal ->
            Canvas(modifier = modifier.size(size)) {
                drawLethalSkull()
            }
        else ->
            Box(
                modifier =
                    modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$damage",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
    }
}

/**
 * Draw a white skull with a red outline and red facial features (eyes, nose) on a red backdrop.
 */
private fun DrawScope.drawLethalSkull() {
    val w = size.width
    val h = size.height
    val red = Color(0xFFD32F2F)

    // Red circular backdrop so the outline/background of non-white areas reads as red.
    drawCircle(color = red, radius = w * 0.5f, center = Offset(w / 2, h / 2))

    val skullCenter = Offset(w / 2, h * 0.42f)
    val skullRadius = w * 0.32f

    // Skull cranium (white) with red outline.
    drawCircle(color = Color.White, radius = skullRadius, center = skullCenter)
    drawCircle(
        color = red,
        radius = skullRadius,
        center = skullCenter,
        style = Stroke(width = w * 0.06f),
    )

    // Jaw (white rounded rectangle beneath the cranium).
    val jawWidth = w * 0.34f
    val jawHeight = h * 0.22f
    val jawLeft = w / 2 - jawWidth / 2
    val jawTop = skullCenter.y + skullRadius * 0.35f
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(jawLeft, jawTop),
        size = Size(jawWidth, jawHeight),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
    )

    // Eye sockets (red - non-white area).
    val eyeRadius = w * 0.09f
    drawCircle(color = red, radius = eyeRadius, center = Offset(w * 0.38f, skullCenter.y))
    drawCircle(color = red, radius = eyeRadius, center = Offset(w * 0.62f, skullCenter.y))

    // Nose (small red triangle).
    val nosePath =
        Path().apply {
            moveTo(w / 2, skullCenter.y + skullRadius * 0.15f)
            lineTo(w / 2 - w * 0.05f, skullCenter.y + skullRadius * 0.45f)
            lineTo(w / 2 + w * 0.05f, skullCenter.y + skullRadius * 0.45f)
            close()
        }
    drawPath(nosePath, red)
}

/**
 * Draw a light grey shield indicating immunity to the selected defender's attack type.
 */
private fun DrawScope.drawImmunityShield() {
    val w = size.width
    val h = size.height
    val lightGrey = Color(0xFFBDBDBD)
    val darkGrey = Color(0xFF757575)

    val shieldPath =
        Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.88f, h * 0.24f)
            lineTo(w * 0.88f, h * 0.52f)
            // Curve down to the pointed bottom tip.
            cubicTo(w * 0.88f, h * 0.78f, w * 0.68f, h * 0.9f, w * 0.5f, h * 0.95f)
            cubicTo(w * 0.32f, h * 0.9f, w * 0.12f, h * 0.78f, w * 0.12f, h * 0.52f)
            lineTo(w * 0.12f, h * 0.24f)
            close()
        }
    drawPath(shieldPath, lightGrey)
    drawPath(shieldPath, darkGrey, style = Stroke(width = w * 0.06f))
}
