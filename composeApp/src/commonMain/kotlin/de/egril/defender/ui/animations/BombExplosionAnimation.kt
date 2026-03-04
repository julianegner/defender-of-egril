package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.icon.ExplosionIcon

/**
 * Explosion overlay for bomb spell impact.
 * When [animate] is true, shows a Lottie explosion animation (plays once).
 * When [animate] is false, shows a static explosion icon.
 */
@Composable
fun BombExplosionAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedBombExplosion(modifier)
    } else {
        StaticBombExplosion(modifier)
    }
}

@Composable
private fun AnimatedBombExplosion(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.BOMB_EXPLOSION,
        modifier = modifier.fillMaxSize(),
        iterations = 1
    )
}

@Composable
private fun StaticBombExplosion(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ExplosionIcon(size = 36.dp)
    }
}
