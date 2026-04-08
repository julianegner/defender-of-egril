package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Enemy spawn portal overlay shown at the spawn point tile when an enemy appears.
 * When [animate] is true, shows a Lottie purple expanding-rings portal animation (plays once).
 * When [animate] is false, no additional visual is shown (spawn point label already indicates the area).
 */
@Composable
fun EnemySpawnAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedEnemySpawn(modifier)
    }
    // No static fallback needed: the spawn point tile label already communicates the area.
}

@Composable
private fun AnimatedEnemySpawn(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.ENEMY_SPAWN,
        modifier = modifier.fillMaxSize(),
        iterations = 1
    )
}
