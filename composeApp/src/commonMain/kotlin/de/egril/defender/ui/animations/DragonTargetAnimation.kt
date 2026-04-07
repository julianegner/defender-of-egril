package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * Dragon target indicator animation shown on Dwarven Mine tiles when a dragon is approaching
 * to destroy the mine.
 *
 * When [animate] is true, shows a looping Lottie dark-red shrinking-ring animation (the same
 * visual used for defeat) to warn the player that the mine is in danger.
 * When [animate] is false, no overlay is shown (the mine info panel already has a text warning).
 */
@Composable
fun DragonTargetAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        Box(
            modifier = modifier.fillMaxSize().alpha(0.75f),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                animationType = AnimationType.DRAGON_TARGET,
                modifier = Modifier.fillMaxSize(),
                iterations = 1
            )
        }
    }
}
