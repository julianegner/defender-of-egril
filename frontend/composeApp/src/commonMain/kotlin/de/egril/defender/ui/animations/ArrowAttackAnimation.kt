package de.egril.defender.ui.animations

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Arrow/bolt projectile animation shown on the source tower tile when a ranged tower attacks
 * (Bow Tower, Spear Tower, Ballista Tower).
 *
 * When [animate] is true, shows a Lottie bolt animation (plays once) rotated toward the target.
 * When [animate] is false, no static fallback is shown (the attack impact at the target tile
 * already provides feedback).
 *
 * @param directionAngle Angle in degrees (0 = right, 90 = down) representing the direction
 *   from the tower toward the target. The Lottie asset fires horizontally to the right; the
 *   rotation is applied via [graphicsLayer] so any direction is supported at runtime.
 * @param isTargetTile When true, the arrow is clipped to the entry half of the tile so it only
 *   travels to the center and does not cross the entire tile. The clip is applied in the
 *   animation's local coordinate space (before rotation) so it works correctly for all directions.
 */
@Composable
fun ArrowAttackAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier,
    directionAngle: Float = 0f,
    isTargetTile: Boolean = false
) {
    if (animate) {
        // When on the target tile, clip the right half of the animation in local space so the
        // arrow only travels to the center of the tile.  The clip modifier is placed AFTER
        // graphicsLayer in the chain so it operates in the pre-rotation coordinate system.
        val innerClip: Modifier = if (isTargetTile)
            Modifier.drawWithContent {
                drawContext.canvas.save()
                drawContext.canvas.clipRect(0f, 0f, size.width / 2f, size.height)
                drawContent()
                drawContext.canvas.restore()
            }
        else
            Modifier
        LottieAnimation(
            animationType = AnimationType.ARROW_ATTACK,
            modifier = modifier.graphicsLayer { rotationZ = directionAngle }.then(innerClip),
            iterations = 1
        )
    }
    // No static fallback — the attack impact animation at the target tile already signals the hit.
}
