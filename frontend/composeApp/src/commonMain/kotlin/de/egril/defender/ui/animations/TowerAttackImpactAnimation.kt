package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.icon.ExplosionIcon

/**
 * Tower attack impact overlay shown at the target tile when a tower attacks.
 * When [animate] is true, shows a Lottie yellow-white flash burst (plays once).
 * When [animate] is false, shows a static explosion icon.
 */
@Composable
fun TowerAttackImpactAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedTowerAttackImpact(modifier)
    } else {
        StaticTowerAttackImpact(modifier)
    }
}

@Composable
private fun AnimatedTowerAttackImpact(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.TOWER_ATTACK_IMPACT,
        modifier = modifier.fillMaxSize(),
        iterations = 1
    )
}

@Composable
private fun StaticTowerAttackImpact(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ExplosionIcon(size = 20.dp)
    }
}
