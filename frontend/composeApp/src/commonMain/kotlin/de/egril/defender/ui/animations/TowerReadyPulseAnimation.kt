package de.egril.defender.ui.animations
import io.github.alexzhirkevich.compottie.Compottie

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pulsing blue border animation for towers that are ready to act (actions remaining > 0).
 * When [animate] is true, shows a Lottie looping pulsing blue ring around the tile.
 * When [animate] is false, no additional visual is shown (the blue tile background already indicates ready state).
 */
@Composable
fun TowerReadyPulseAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedTowerReadyPulse(modifier)
    }
    // No static fallback needed: the blue tile background already clearly indicates ready state.
}

@Composable
private fun AnimatedTowerReadyPulse(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.TOWER_READY_PULSE,
        modifier = modifier.fillMaxSize(),
        iterations = Compottie.IterateForever
    )
}
