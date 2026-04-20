package de.egril.defender.ui.gameplay.defenderButtons

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.TowerTypeIcon
import de.egril.defender.ui.androidTVModifier
import de.egril.defender.ui.animations.InstantTowerSpellAnimation
import de.egril.defender.ui.animations.SpellInstantTowerColor
import de.egril.defender.ui.gameplay.GamePlayColors
import de.egril.defender.ui.getLocalizedShortName
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun CompactDefenderButton(
    type: DefenderType,
    isSelected: Boolean,
    canAfford: Boolean,
    instantTowerActive: Boolean = false,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isDarkMode = AppSettings.isDarkMode.value
    val locale = com.hyperether.resources.currentLanguage.value

    // Create accessible content description
    val towerName = type.getLocalizedShortName(locale)
    val description = "$towerName, ${stringResource(Res.string.coins_label)}: ${type.baseCost}" +
        if (isSelected) ", ${stringResource(Res.string.selected)}" else ""

    // Apply Android TV modifiers for accessibility and focus
    val buttonModifier = modifier.androidTVModifier(
        isSelected = isSelected,
        description = description
    )

    Box(modifier = buttonModifier) {
        Button(
            onClick = onClick,
            enabled = canAfford,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) GamePlayColors.InfoDark else MaterialTheme.colorScheme.primary,
                contentColor = if (isSelected && isDarkMode) Color.White else Color.White,  // Brighter text when selected in dark mode
                disabledContainerColor = GamePlayColors.DisabledButton,
                disabledContentColor = GamePlayColors.DisabledButtonText
            ),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Tower icon
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TowerTypeIcon(defenderType = type, modifier = Modifier.size(30.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                val locale = com.hyperether.resources.currentLanguage.value
                Text(
                    type.getLocalizedShortName(locale),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = if (isSelected && isDarkMode) Color.White else Color.White  // Ensure bright text
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Cost
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoneyIcon(size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${type.baseCost}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        // Show glow animation overlay + purple border when Instant Tower spell is active and tower is affordable
        if (instantTowerActive && canAfford) {
            InstantTowerSpellAnimation(
                animate = AppSettings.enableAnimations.value,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .border(2.dp, SpellInstantTowerColor, RoundedCornerShape(percent = 50))
            )
        }
    }
}
