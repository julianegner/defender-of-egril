package de.egril.defender.ui.icon.enemy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.utils.BigHeadMode

/**
 * Composable that draws an enemy unit icon
 *
 * @param healthOverride When provided, this value is shown instead of [attacker.currentHealth]
 *   in the health bar. Used to delay the health display during attack animations so the number
 *   only changes after the projectile impact flash has completed.
 */
@Composable
fun EnemyIcon(
    attacker: Attacker,
    modifier: Modifier = Modifier,
    healthTextColor: Color = Color.White,
    backgroundColor: Color? = null,
    healthOverride: Int? = null
) {
    val bgLuminance = (backgroundColor ?: MaterialTheme.colorScheme.background).luminance()
    val contrastOutlineColor = if (bgLuminance < 0.5f) Color.White else Color.Black

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Draw enemy graphics first (will be behind text)
        val headScale = if (BigHeadMode.isEnabled.value) 2f else 1f
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)
            
            when (attacker.type) {
                AttackerType.GOBLIN -> drawGoblinSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.ORK -> drawOrkSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.OGRE -> drawOgreSymbol(centerX, centerY, iconSize * 0.75f, headScale = headScale)
                AttackerType.SKELETON -> drawSkeletonSymbol(centerX, centerY, iconSize * 0.7f, contrastOutlineColor, headScale)
                AttackerType.EVIL_WIZARD -> drawEvilWizardSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.BLUE_DEMON -> drawBlueDemonSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.RED_DEMON -> drawRedDemonSymbol(centerX, centerY, iconSize * 0.75f, contrastOutlineColor, headScale)
                AttackerType.RED_WITCH -> drawRedWitchSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.GREEN_WITCH -> drawGreenWitchSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.EWHAD -> drawEwhadSymbol(centerX, centerY, iconSize * 0.8f, headScale = headScale)
                AttackerType.DRAGON -> drawDragonSymbol(centerX, centerY, iconSize * 0.9f, headScale = headScale)
            }
        }
        
        // Level number at top center - only if level > 1
        if (attacker.level.value > 1) {
            Text(
                text = "${attacker.level.value}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                color = healthTextColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
        
        // Health number at bottom center - 10dp from bottom edge (hidden for Ewhad)
        if (attacker.type != AttackerType.EWHAD) {
            Text(
                text = "${healthOverride ?: attacker.currentHealth.value}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 13.sp,
                color = healthTextColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)  // 10dp from bottom as requested
            )
        }
    }
}

/**
 * Composable that draws an enemy type icon (for planned spawns)
 */
@Composable
fun EnemyTypeIcon(
    attackerType: AttackerType,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null
) {
    val bgLuminance = (backgroundColor ?: MaterialTheme.colorScheme.background).luminance()
    val contrastOutlineColor = if (bgLuminance < 0.5f) Color.White else Color.Black

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Draw enemy graphics
        val headScale = if (BigHeadMode.isEnabled.value) 2f else 1f
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)
            
            when (attackerType) {
                AttackerType.GOBLIN -> drawGoblinSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.ORK -> drawOrkSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.OGRE -> drawOgreSymbol(centerX, centerY, iconSize * 0.75f, headScale = headScale)
                AttackerType.SKELETON -> drawSkeletonSymbol(centerX, centerY, iconSize * 0.7f, contrastOutlineColor, headScale)
                AttackerType.EVIL_WIZARD -> drawEvilWizardSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.BLUE_DEMON -> drawBlueDemonSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.RED_DEMON -> drawRedDemonSymbol(centerX, centerY, iconSize * 0.75f, contrastOutlineColor, headScale)
                AttackerType.RED_WITCH -> drawRedWitchSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.GREEN_WITCH -> drawGreenWitchSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.EWHAD -> drawEwhadSymbol(centerX, centerY, iconSize * 0.8f, headScale = headScale)
                AttackerType.DRAGON -> drawDragonSymbol(centerX, centerY, iconSize * 0.9f, headScale = headScale)
            }
        }
    }
}
