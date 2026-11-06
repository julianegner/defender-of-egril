package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.egril.defender.model.Attacker
import de.egril.defender.ui.getLocalizedName

internal fun bossHealthBarProgress(
    health: Int,
    maxHealth: Int,
): Float = if (maxHealth <= 0) 0f else (health.toFloat() / maxHealth).coerceIn(0f, 1f)

@Composable
internal fun BossHealthBar(attacker: Attacker) {
    val progress = bossHealthBarProgress(attacker.currentHealth.value, attacker.maxHealth)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        Text(
            text = "${attacker.type.getLocalizedName()}  ${attacker.currentHealth.value} / ${attacker.maxHealth}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFB71C1C),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
