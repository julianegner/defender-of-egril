package de.egril.defender.ui.editor.level.supports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.CooldownPower
import de.egril.defender.model.CooldownPowerType
import de.egril.defender.model.INDEFINITE_SUPPORT_COUNT
import de.egril.defender.model.LevelSupports
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObject
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.SupportSpell
import de.egril.defender.model.isIndefiniteSupportCount
import de.egril.defender.model.supportCountDisplayText
import de.egril.defender.ui.gameplay.SpellTargetIcon
import de.egril.defender.ui.gameplay.localizedCooldownPowerName
import de.egril.defender.ui.gameplay.localizedSupportName
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.HammerIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WoodIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.cooldown_turns_label
import defender_of_egril.composeapp.generated.resources.damage_label
import defender_of_egril.composeapp.generated.resources.health_points
import defender_of_egril.composeapp.generated.resources.start_active_label
import defender_of_egril.composeapp.generated.resources.supports_cooldown_powers_section
import defender_of_egril.composeapp.generated.resources.supports_count_indefinitely
import defender_of_egril.composeapp.generated.resources.supports_intro
import defender_of_egril.composeapp.generated.resources.supports_objects_section
import defender_of_egril.composeapp.generated.resources.supports_spells_section

private const val MAX_SUPPORT_COUNT = 99

/**
 * Level editor tab for configuring the level's player-usable supports:
 * placable objects (traps, magical traps, barricades) and spell tokens.
 *
 * Supports added here appear as boxes above the tower buttons during gameplay and are
 * mentioned in the level's story message. They can be used regardless of the player's towers,
 * tech level, or mana.
 */
@Composable
fun SupportsTab(
    supports: LevelSupports,
    onSupportsChange: (LevelSupports) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.supports_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Placable objects section
        item {
            SectionHeader(stringResource(Res.string.supports_objects_section))
        }
        items(SupportObjectType.entries) { type ->
            val existing = supports.objects.firstOrNull { it.type == type }
            SupportObjectRow(
                type = type,
                supportObject = existing,
                onToggle = { checked ->
                    val newObjects =
                        if (checked) {
                            supports.objects + SupportObject(type = type)
                        } else {
                            supports.objects.filterNot { it.type == type }
                        }
                    onSupportsChange(supports.copy(objects = newObjects))
                },
                onCountChange = { newCount ->
                    val newObjects =
                        supports.objects.map {
                            if (it.type == type) it.copy(count = newCount) else it
                        }
                    onSupportsChange(supports.copy(objects = newObjects))
                },
                onDamageChange = { newDamage ->
                    val newObjects =
                        supports.objects.map {
                            if (it.type == type) it.copy(damage = newDamage) else it
                        }
                    onSupportsChange(supports.copy(objects = newObjects))
                },
                onHealthChange = { newHealth ->
                    val newObjects =
                        supports.objects.map {
                            if (it.type == type) it.copy(healthPoints = newHealth) else it
                        }
                    onSupportsChange(supports.copy(objects = newObjects))
                },
            )
        }

        // Spell tokens section
        item {
            SectionHeader(stringResource(Res.string.supports_spells_section))
        }
        items(SpellType.entries) { spell ->
            val existing = supports.spells.firstOrNull { it.spell == spell }
            SupportSpellRow(
                spell = spell,
                supportSpell = existing,
                onToggle = { checked ->
                    val newSpells =
                        if (checked) {
                            supports.spells + SupportSpell(spell = spell)
                        } else {
                            supports.spells.filterNot { it.spell == spell }
                        }
                    onSupportsChange(supports.copy(spells = newSpells))
                },
                onCountChange = { newCount ->
                    val newSpells =
                        supports.spells.map {
                            if (it.spell == spell) it.copy(count = newCount) else it
                        }
                    onSupportsChange(supports.copy(spells = newSpells))
                },
            )
        }

        // Cooldown powers section
        item {
            SectionHeader(stringResource(Res.string.supports_cooldown_powers_section))
        }
        items(CooldownPowerType.entries) { type ->
            val existing = supports.cooldownPowers.firstOrNull { it.type == type }
            CooldownPowerRow(
                type = type,
                power = existing,
                onToggle = { checked ->
                    val newPowers =
                        if (checked) {
                            supports.cooldownPowers + CooldownPower(type = type)
                        } else {
                            supports.cooldownPowers.filterNot { it.type == type }
                        }
                    onSupportsChange(supports.copy(cooldownPowers = newPowers))
                },
                onCooldownChange = { newCooldown ->
                    val newPowers =
                        supports.cooldownPowers.map {
                            if (it.type == type) it.copy(cooldownTurns = newCooldown) else it
                        }
                    onSupportsChange(supports.copy(cooldownPowers = newPowers))
                },
                onStartActiveChange = { startActive ->
                    val newPowers =
                        supports.cooldownPowers.map {
                            if (it.type == type) it.copy(startActive = startActive) else it
                        }
                    onSupportsChange(supports.copy(cooldownPowers = newPowers))
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SupportObjectRow(
    type: SupportObjectType,
    supportObject: SupportObject?,
    onToggle: (Boolean) -> Unit,
    onCountChange: (Int) -> Unit,
    onDamageChange: (Int) -> Unit,
    onHealthChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = supportObject != null,
            onCheckedChange = onToggle,
        )
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            when (type) {
                SupportObjectType.DWARVEN_TRAP -> TrapIcon(size = 24.dp)
                SupportObjectType.MAGICAL_TRAP -> PentagramIcon(size = 24.dp)
                SupportObjectType.BARRICADE -> WoodIcon(size = 24.dp)
            }
        }
        Text(
            text = type.localizedSupportName(),
            modifier = Modifier.width(120.dp),
        )
        if (supportObject != null) {
            CountStepper(
                count = supportObject.count,
                onCountChange = onCountChange,
            )
            when (type) {
                SupportObjectType.DWARVEN_TRAP ->
                    ValueStepper(
                        label = stringResource(Res.string.damage_label),
                        value = supportObject.damage,
                        step = 5,
                        onValueChange = onDamageChange,
                    )
                SupportObjectType.BARRICADE ->
                    ValueStepper(
                        label = stringResource(Res.string.health_points),
                        value = supportObject.healthPoints,
                        step = 10,
                        onValueChange = onHealthChange,
                    )
                SupportObjectType.MAGICAL_TRAP -> {
                    // Magical traps have no damage/health configuration
                }
            }
        }
    }
}

@Composable
private fun SupportSpellRow(
    spell: SpellType,
    supportSpell: SupportSpell?,
    onToggle: (Boolean) -> Unit,
    onCountChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = supportSpell != null,
            onCheckedChange = onToggle,
        )
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            SpellTargetIcon(spell = spell, size = 24.dp)
        }
        Text(
            text = spell.getLocalizedName(),
            modifier = Modifier.width(160.dp),
        )
        if (supportSpell != null) {
            CountStepper(
                count = supportSpell.count,
                onCountChange = onCountChange,
            )
        }
    }
}

@Composable
private fun CountStepper(
    count: Int,
    onCountChange: (Int) -> Unit,
) {
    val isIndefinite = isIndefiniteSupportCount(count)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedButton(
            onClick = { onCountChange((count - 1).coerceAtLeast(1)) },
            enabled = !isIndefinite && count > 1,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(32.dp),
        ) {
            Text("-")
        }
        Text(
            text = supportCountDisplayText(count),
            modifier = Modifier.width(28.dp),
            fontWeight = FontWeight.Bold,
        )
        OutlinedButton(
            onClick = { onCountChange((count + 1).coerceAtMost(MAX_SUPPORT_COUNT)) },
            enabled = !isIndefinite && count < MAX_SUPPORT_COUNT,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(32.dp),
        ) {
            Text("+")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Checkbox(
                checked = isIndefinite,
                onCheckedChange = { checked ->
                    onCountChange(if (checked) INDEFINITE_SUPPORT_COUNT else 1)
                },
            )
            Text(
                text = stringResource(Res.string.supports_count_indefinitely),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CooldownPowerRow(
    type: CooldownPowerType,
    power: CooldownPower?,
    onToggle: (Boolean) -> Unit,
    onCooldownChange: (Int) -> Unit,
    onStartActiveChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = power != null,
            onCheckedChange = onToggle,
        )
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            when (type) {
                CooldownPowerType.COIN_SURGE -> MoneyIcon(size = 24.dp)
                CooldownPowerType.SKY_IS_FALLING -> ExplosionIcon(size = 24.dp)
                CooldownPowerType.CONSTRUCTION_REPAIRS -> HammerIcon(size = 24.dp)
                CooldownPowerType.MANA_WELL -> PentagramIcon(size = 24.dp)
                CooldownPowerType.DEEP_MANA_WELL -> PentagramIcon(size = 24.dp)
            }
        }
        Text(
            text = type.localizedCooldownPowerName(),
            modifier = Modifier.width(160.dp),
        )
        if (power != null) {
            ValueStepper(
                label = stringResource(Res.string.cooldown_turns_label),
                value = power.cooldownTurns,
                step = 1,
                onValueChange = { onCooldownChange(it.coerceAtLeast(1)) },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Checkbox(
                    checked = power.startActive,
                    onCheckedChange = onStartActiveChange,
                )
                Text(
                    text = stringResource(Res.string.start_active_label),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ValueStepper(
    label: String,
    value: Int,
    step: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(
            onClick = { onValueChange((value - step).coerceAtLeast(0)) },
            enabled = value > 0,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(32.dp),
        ) {
            Text("-")
        }
        Text(
            text = "$value",
            modifier = Modifier.width(36.dp),
            fontWeight = FontWeight.Bold,
        )
        OutlinedButton(
            onClick = { onValueChange(value + step) },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(32.dp),
        ) {
            Text("+")
        }
    }
}
