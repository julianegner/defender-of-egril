package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.icon.CrossIcon

/**
 * Enemy death animation overlay shown at the position where an enemy was defeated.
 * When [animate] is true, shows a Lottie expanding-ring burst animation (plays once).
 * When [animate] is false, shows a static red cross symbol.
 */
@Composable
fun EnemyDeathAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedEnemyDeath(modifier)
    } else {
        StaticEnemyDeath(modifier)
    }
}

@Composable
private fun AnimatedEnemyDeath(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.ENEMY_DEATH,
        modifier = modifier.fillMaxSize(),
        iterations = 1
    )
}

@Composable
private fun StaticEnemyDeath(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CrossIcon(size = 28.dp)
    }
}
