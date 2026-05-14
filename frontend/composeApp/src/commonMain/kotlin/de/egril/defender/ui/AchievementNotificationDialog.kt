package de.egril.defender.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.model.Achievement
import de.egril.defender.model.AchievementDefinitions
import de.egril.defender.ui.icon.TrophyIcon
import de.egril.defender.utils.isPlatformMobile
import de.egril.defender.ui.isMobileWebBrowser
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.achievement_unlocked
import defender_of_egril.composeapp.generated.resources.close
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Dialog that shows when a new achievement is earned
 */
@Composable
fun AchievementNotificationDialog(
    achievement: Achievement?,
    onDismiss: () -> Unit
) {
    if (achievement == null) return
    
    val info = AchievementDefinitions.getInfo(achievement.id)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            SelectionContainer {
            val isMobileUI = isPlatformMobile || isMobileWebBrowser()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isMobileUI) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isMobileUI) 4.dp else 16.dp)
            ) {
                // Large trophy icon
                TrophyIcon(
                    size = if (isMobileUI) 28.dp else 64.dp,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                
                // Achievement unlocked text
                Text(
                    text = stringResource(Res.string.achievement_unlocked),
                    style = if (isMobileUI) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // Achievement name
                Text(
                    text = achievement.id.getLocalizedName(),
                    style = if (isMobileUI) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // Achievement description
                Text(
                    text = achievement.id.getLocalizedDescription(),
                    style = if (isMobileUI) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.close))
                }
            }
            }
        }
    }
}
