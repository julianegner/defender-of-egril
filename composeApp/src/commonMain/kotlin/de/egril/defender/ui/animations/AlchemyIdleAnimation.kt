package de.egril.defender.ui.animations
import io.github.alexzhirkevich.compottie.Compottie

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Ambient idle animation shown on Alchemy Tower tiles when the tower is built and active.
 * When [animate] is true, shows a Lottie looping green bubble float animation.
 * When [animate] is false, the tower icon alone is shown (no fallback needed).
 */
@Composable
fun AlchemyIdleAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        LottieAnimation(
            animationType = AnimationType.ALCHEMY_IDLE,
            modifier = modifier.fillMaxSize(),
            iterations = Compottie.IterateForever
        )
    }
    // No static fallback — the alchemy icon already identifies the tower type.
}
