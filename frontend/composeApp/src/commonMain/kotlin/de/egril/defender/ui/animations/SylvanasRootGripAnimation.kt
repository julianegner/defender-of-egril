package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import io.github.alexzhirkevich.compottie.Compottie

/**
 * Thorny vine animation overlay for towers disabled by Sylvanas's Root Grip.
 * When [animate] is true, shows a Lottie animation of vines growing and engulfing the tower.
 * When [animate] is false, shows a static vine symbol.
 */
@Composable
fun SylvanasRootGripAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    if (animate) {
        AnimatedSylvanasRootGrip(modifier)
    } else {
        StaticSylvanasRootGrip(modifier)
    }
}

@Composable
private fun AnimatedSylvanasRootGrip(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.SYLVANAS_ROOT_GRIP,
        modifier = modifier.fillMaxSize(),
        iterations = Compottie.IterateForever,
    )
}

@Composable
private fun StaticSylvanasRootGrip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Vine symbol using text
        Text(
            "V",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF2D5016),
            fontWeight = FontWeight.Bold,
        )
    }
}
