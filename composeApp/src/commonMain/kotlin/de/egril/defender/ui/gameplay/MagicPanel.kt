package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.egril.defender.model.PlayerStats
import de.egril.defender.model.SpellType
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.PentagramIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Magic Panel UI - displays available spells for casting
 * Opens when player clicks on mana display in header
 */
@Composable
fun MagicPanel(
    playerStats: PlayerStats,
    currentMana: Int,
    maxMana: Int,
    onCastSpell: (SpellType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.magic_panel_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Mana display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PentagramIcon(
                        size = 20.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$currentMana/$maxMana",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = stringResource(Res.string.magic_panel_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Spell list
            val unlockedSpells = SpellType.entries.filter { spell ->
                playerStats.unlockedSpells.contains(spell)
            }
            
            if (unlockedSpells.isEmpty()) {
                // No spells unlocked message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.no_spells_unlocked),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(unlockedSpells) { spell ->
                        SpellCard(
                            spell = spell,
                            currentMana = currentMana,
                            onCast = { onCastSpell(spell) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Close button
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.close))
            }
        }
    }
}

/**
 * Individual spell card in the magic panel
 */
@Composable
private fun SpellCard(
    spell: SpellType,
    currentMana: Int,
    onCast: () -> Unit
) {
    val canCast = currentMana >= spell.manaCost
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canCast) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spell icon
            LightningIcon(
                size = 32.dp
            )
            
            // Spell info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = spell.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (canCast) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                
                Text(
                    text = spell.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (canCast) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Mana cost
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PentagramIcon(
                        size = 14.dp,
                        color = if (canCast) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                    Text(
                        text = "${spell.manaCost} ${stringResource(Res.string.mana_label)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (canCast) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
            }
            
            // Cast button
            Button(
                onClick = onCast,
                enabled = canCast,
                modifier = Modifier.width(80.dp)
            ) {
                Text(stringResource(Res.string.cast))
            }
        }
    }
}

/**
 * Spell confirmation dialog
 * Shows before actually casting a spell to confirm the action
 */
@Composable
fun SpellConfirmationDialog(
    spell: SpellType,
    currentMana: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val remainingMana = currentMana - spell.manaCost
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.confirm_spell_cast))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.cast_spell_confirmation, spell.displayName),
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Cost and remaining mana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.mana_cost_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PentagramIcon(size = 16.dp, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "${spell.manaCost}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.remaining_mana_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PentagramIcon(size = 16.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$remainingMana",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(Res.string.cast))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
