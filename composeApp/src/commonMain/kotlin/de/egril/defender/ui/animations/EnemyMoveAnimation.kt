package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Movement trail overlay shown on tiles that an enemy stepped through during the enemy turn.
 * When [animate] is true, shows a Lottie green-yellow chevron that fades out (plays once).
 * When [animate] is false, no additional visual is shown (the enemy icon communicates location).
 */
@Composable
fun EnemyMoveAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        LottieAnimation(
            animationType = AnimationType.ENEMY_MOVE,
            modifier = modifier.fillMaxSize(),
            iterations = 1
        )
    }
    // No static fallback — the tile colour on path tiles already shows the area.
}
