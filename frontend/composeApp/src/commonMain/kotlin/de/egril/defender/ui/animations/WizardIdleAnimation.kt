package de.egril.defender.ui.animations
import io.github.alexzhirkevich.compottie.Compottie

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Ambient idle animation shown on Wizard Tower tiles when the tower is built and active.
 * When [animate] is true, shows a Lottie looping blue-purple orbiting sparkle.
 * When [animate] is false, the tower icon alone is shown (no fallback needed).
 */
@Composable
fun WizardIdleAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        LottieAnimation(
            animationType = AnimationType.WIZARD_IDLE,
            modifier = modifier.fillMaxSize(),
            iterations = Compottie.IterateForever
        )
    }
    // No static fallback — the wizard icon already identifies the tower type.
}
