package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Dragon level change overlay shown on the dragon's tile when its level increases or decreases.
 *
 * Uses Lottie animations:
 *   - Level up ([isLevelUp] = true): orange "+" symbols rising upward (same motion as
 *     Green Witch Healing, colours changed to dragon orange #FF8C00).
 *   - Level down ([isLevelUp] = false): orange "−" symbols drifting downward (same motion as
 *     Barricade Damage, colours changed to dragon orange).
 *
 * When [animate] is false, no additional visual is shown (the dragon icon and level text already
 * communicate state).
 */
@Composable
fun DragonLevelChangeAnimation(
    animate: Boolean,
    isLevelUp: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!animate) return

    LottieAnimation(
        animationType = if (isLevelUp) AnimationType.DRAGON_LEVEL_UP else AnimationType.DRAGON_LEVEL_DOWN,
        modifier = modifier.fillMaxSize(),
        iterations = 1
    )
}


