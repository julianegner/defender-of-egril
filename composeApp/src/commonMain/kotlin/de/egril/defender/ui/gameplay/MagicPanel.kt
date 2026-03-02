package de.egril.defender.ui.gameplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.egril.defender.model.PlayerAbilities
import de.egril.defender.model.SpellType
import de.egril.defender.model.SpellTargetType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.Position
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.MapIcon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.TargetIcon
import de.egril.defender.ui.icon.TowerIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Magic Panel UI - displays available spells for casting
 * Opens when player clicks on mana display in header
 */
@Composable
fun MagicPanel(
    playerStats: PlayerAbilities,
    currentMana: Int,
    maxMana: Int,
    currentHealthPoints: Int,
    maxHealthPoints: Int,
    gamePhase: GamePhase,
    selectedSpell: SpellType?,
    onCastSpell: (SpellType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.magic_panel_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Mana display
                    PentagramIcon(size = 16.dp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "$currentMana/$maxMana",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onClose, contentPadding = PaddingValues(4.dp)) {
                        Text(stringResource(Res.string.close), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Spell grid (3 columns, compact cards)
            val unlockedSpells = SpellType.entries.filter { spell ->
                playerStats.unlockedSpells.contains(spell)
            }
            
            if (unlockedSpells.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.no_spells_unlocked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(unlockedSpells) { spell ->
                        CompactSpellCard(
                            spell = spell,
                            currentMana = currentMana,
                            currentHealthPoints = currentHealthPoints,
                            maxHealthPoints = maxHealthPoints,
                            gamePhase = gamePhase,
                            isSelected = spell == selectedSpell,
                            onCast = { onCastSpell(spell) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact spell card for 3-column grid layout
 */
@Composable
private fun CompactSpellCard(
    spell: SpellType,
    currentMana: Int,
    currentHealthPoints: Int,
    maxHealthPoints: Int,
    gamePhase: GamePhase,
    isSelected: Boolean,
    onCast: () -> Unit
) {
    // Spells can only be cast when the level has started (not during initial build phase)
    // Heal spell is also disabled when the player is already at full health
    val canCast = currentMana >= spell.manaCost && gamePhase != GamePhase.INITIAL_BUILDING &&
            (spell != SpellType.HEAL || currentHealthPoints < maxHealthPoints)
    
    val contentAlpha = if (canCast) 1f else 0.4f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canCast, onClick = onCast),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        border = if (isSelected) {
            BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Spell type icon
            SpellTargetIcon(spell = spell, size = 20.dp, alpha = contentAlpha)

            // Spell name
            Text(
                text = spell.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )

            // Mana cost
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                PentagramIcon(
                    size = 10.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                )
                Text(
                    text = "${spell.manaCost}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                )
            }
        }
    }
}

/**
 * Icon representing the target type of a spell
 */
@Composable
fun SpellTargetIcon(spell: SpellType, size: androidx.compose.ui.unit.Dp, alpha: Float = 1f) {
    val modifier = Modifier.graphicsLayer { this.alpha = alpha }
    when (spell.targetType) {
        SpellTargetType.ENEMY -> TargetIcon(size = size, modifier = modifier)
        SpellTargetType.TOWER -> TowerIcon(size = size, modifier = modifier)
        SpellTargetType.POSITION -> MapIcon(size = size, modifier = modifier)
        SpellTargetType.NONE -> LightningIcon(size = size, modifier = modifier)
    }
}

/**
 * Post-target spell confirmation dialog
 * Shows spell and target details after target is selected
 */
@Composable
fun SpellTargetConfirmationDialog(
    spell: SpellType,
    target: Any,
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
                    text = stringResource(Res.string.cast_spell_on_target, spell.displayName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show target details
                when (target) {
                    is de.egril.defender.model.Attacker -> {
                        Text(
                            text = stringResource(Res.string.target_enemy_details),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                Res.string.enemy_type_level_hp,
                                target.type.displayName,
                                target.level.value.toString(),
                                target.currentHealth.value.toString()
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is de.egril.defender.model.Defender -> {
                        Text(
                            text = stringResource(Res.string.target_tower_details),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                Res.string.tower_type_level,
                                target.type.displayName,
                                target.level.value.toString()
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is Position -> {
                        Text(
                            text = stringResource(Res.string.target_position),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(${target.x}, ${target.y})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Mana cost and remaining
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

/**
 * Warning dialog for freeze-immune enemies
 */
@Composable
fun FreezeImmuneWarningDialog(
    enemy: de.egril.defender.model.Attacker,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.freeze_immune_warning_title))
        },
        text = {
            Text(
                text = stringResource(
                    Res.string.freeze_immune_warning_message,
                    enemy.type.displayName
                ),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        }
    )
}

