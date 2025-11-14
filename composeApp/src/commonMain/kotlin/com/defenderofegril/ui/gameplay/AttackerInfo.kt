package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.defenderofegril.model.*
import com.defenderofegril.ui.*
import com.defenderofegril.ui.icon.enemy.EnemyIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Display details about a selected enemy attacker
 * Similar to DefenderInfo but for enemies
 */
@Composable
fun AttackerInfo(
    attacker: Attacker,
    isMobile: Boolean = false
) {
    val locale = com.hyperether.resources.currentLanguage.value
    
    // Use key to force recomposition when attacker stats change
    key(
        attacker.id,
        attacker.level,
        attacker.currentHealth.value,
        attacker.position.value.x,
        attacker.position.value.y
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isMobile) 4.dp else 8.dp)
        ) {
            // Enemy icon, name, and details in one row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Enemy icon - double the original size (matching tower icon size)
                val iconSize = if (isMobile) 64.dp else 96.dp
                val iconInnerSize = if (isMobile) 56.dp else 88.dp
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center
                ) {
                    EnemyIcon(attacker = attacker, modifier = Modifier.size(iconInnerSize))
                }

                val horizontalSpacing = if (isMobile) 4.dp else 8.dp
                Spacer(modifier = Modifier.width(horizontalSpacing))

                // Enemy name and level
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            attacker.type.getLocalizedName(locale),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (attacker.level > 1) {
                            Text(
                                "Lvl ${attacker.level}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = GamePlayColors.ErrorDark
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "${stringResource(Res.string.hp_short)}: ${attacker.currentHealth.value}/${attacker.maxHealth}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "${stringResource(Res.string.position_label)}: (${attacker.position.value.x},${attacker.position.value.y})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    // Additional info about special abilities
                    if (attacker.type.canSummon) {
                        Text(
                            "⚡ ${stringResource(Res.string.can_summon)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamePlayColors.Warning
                        )
                    }
                    if (attacker.type.canHeal) {
                        Text(
                            "💚 ${stringResource(Res.string.can_heal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamePlayColors.Success
                        )
                    }
                    if (attacker.type.canDisableTowers) {
                        Text(
                            "🔒 ${stringResource(Res.string.can_disable_towers)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamePlayColors.ErrorDark
                        )
                    }
                    if (attacker.type.immuneToAcid) {
                        Text(
                            "🛡️ ${stringResource(Res.string.immune_to_acid)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamePlayColors.InfoDark
                        )
                    }
                    if (attacker.type.immuneToFireball) {
                        Text(
                            "🛡️ ${stringResource(Res.string.immune_to_fireball)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamePlayColors.InfoDark
                        )
                    }
                }
            }
        }
    }
}
