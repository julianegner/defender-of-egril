package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.model.Achievement
import de.egril.defender.ui.icon.TrophyIcon
import de.egril.defender.ui.isMobileWebBrowser
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.achievement_unlocked
import defender_of_egril.composeapp.generated.resources.close

internal data class AchievementNotificationLayout(
    val cardWidth: Dp,
    val maxHeight: Dp,
    val contentPadding: Dp,
    val contentSpacing: Dp,
    val iconSize: Dp,
    val typographyMode: AchievementNotificationTypographyMode,
    val buttonFillFraction: Float,
)

internal enum class AchievementNotificationTypographyMode {
    DESKTOP,
    COMPACT,
    VERY_COMPACT,
}

internal fun calculateAchievementNotificationLayout(
    availableWidth: Dp,
    availableHeight: Dp,
    isPlatformMobileDevice: Boolean,
    isMobileWeb: Boolean,
): AchievementNotificationLayout {
    val isLandscape = availableWidth > availableHeight
    val useCompactMobileWebLayout = isMobileWeb && isLandscape
    val usePortraitMobileWebLayout = isMobileWeb && !isLandscape
    val useCompactLayout = useCompactMobileWebLayout || (!isPlatformMobileDevice && availableWidth < 900.dp && availableHeight < 600.dp)

    return when {
        useCompactMobileWebLayout ->
            AchievementNotificationLayout(
                cardWidth = (availableWidth * 0.5f).coerceIn(220.dp, 320.dp),
                maxHeight = (availableHeight * 0.5f).coerceIn(180.dp, 260.dp),
                contentPadding = 10.dp,
                contentSpacing = 6.dp,
                iconSize = 28.dp,
                typographyMode = AchievementNotificationTypographyMode.VERY_COMPACT,
                buttonFillFraction = 0.72f,
            )

        usePortraitMobileWebLayout ->
            AchievementNotificationLayout(
                cardWidth = (availableWidth * 0.85f).coerceIn(220.dp, 340.dp),
                maxHeight = (availableHeight * 0.55f).coerceIn(200.dp, 340.dp),
                contentPadding = 12.dp,
                contentSpacing = 8.dp,
                iconSize = 32.dp,
                typographyMode = AchievementNotificationTypographyMode.COMPACT,
                buttonFillFraction = 0.9f,
            )

        isPlatformMobileDevice || useCompactLayout ->
            AchievementNotificationLayout(
                cardWidth = (availableWidth * 0.82f).coerceIn(240.dp, 380.dp),
                maxHeight = (availableHeight * 0.7f).coerceIn(220.dp, 360.dp),
                contentPadding = 14.dp,
                contentSpacing = 10.dp,
                iconSize = 40.dp,
                typographyMode = AchievementNotificationTypographyMode.COMPACT,
                buttonFillFraction = 0.82f,
            )

        else ->
            AchievementNotificationLayout(
                cardWidth = availableWidth.coerceAtMost(520.dp),
                maxHeight = (availableHeight - 32.dp).coerceAtLeast(260.dp),
                contentPadding = 24.dp,
                contentSpacing = 16.dp,
                iconSize = 64.dp,
                typographyMode = AchievementNotificationTypographyMode.DESKTOP,
                buttonFillFraction = 1f,
            )
    }
}

/**
 * Dialog that shows when a new achievement is earned
 */
@Composable
fun AchievementNotificationDialog(
    achievement: Achievement?,
    onDismiss: () -> Unit,
) {
    if (achievement == null) return

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val layout =
                calculateAchievementNotificationLayout(
                    availableWidth = maxWidth,
                    availableHeight = maxHeight,
                    isPlatformMobileDevice = isPlatformMobile,
                    isMobileWeb = isMobileWebBrowser(),
                )
            val titleStyle =
                when (layout.typographyMode) {
                    AchievementNotificationTypographyMode.DESKTOP -> MaterialTheme.typography.headlineSmall
                    AchievementNotificationTypographyMode.COMPACT -> MaterialTheme.typography.titleMedium
                    AchievementNotificationTypographyMode.VERY_COMPACT -> MaterialTheme.typography.titleSmall
                }
            val nameStyle =
                when (layout.typographyMode) {
                    AchievementNotificationTypographyMode.DESKTOP -> MaterialTheme.typography.titleLarge
                    AchievementNotificationTypographyMode.COMPACT -> MaterialTheme.typography.bodyLarge
                    AchievementNotificationTypographyMode.VERY_COMPACT -> MaterialTheme.typography.bodyMedium
                }
            val descriptionStyle =
                when (layout.typographyMode) {
                    AchievementNotificationTypographyMode.DESKTOP -> MaterialTheme.typography.bodyMedium
                    AchievementNotificationTypographyMode.COMPACT -> MaterialTheme.typography.bodySmall
                    AchievementNotificationTypographyMode.VERY_COMPACT -> MaterialTheme.typography.bodySmall
                }

            AchievementNotificationCard(
                achievement = achievement,
                layout = layout,
                titleStyle = titleStyle,
                nameStyle = nameStyle,
                descriptionStyle = descriptionStyle,
                onDismiss = onDismiss,
                modifier = Modifier,
            )
        }
    }
}

@Composable
internal fun AchievementNotificationCard(
    achievement: Achievement,
    layout: AchievementNotificationLayout,
    titleStyle: androidx.compose.ui.text.TextStyle,
    nameStyle: androidx.compose.ui.text.TextStyle,
    descriptionStyle: androidx.compose.ui.text.TextStyle,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .width(layout.cardWidth)
                .heightIn(max = layout.maxHeight)
                .testTag("achievementNotificationCard"),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(layout.contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(layout.contentSpacing),
        ) {
            SelectionContainer {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(layout.contentSpacing),
                ) {
                    TrophyIcon(
                        size = layout.iconSize,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )

                    Text(
                        text = stringResource(Res.string.achievement_unlocked),
                        style = titleStyle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = achievement.id.getLocalizedName(),
                        style = nameStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = achievement.id.getLocalizedDescription(),
                        style = descriptionStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(layout.buttonFillFraction),
            ) {
                Text(stringResource(Res.string.close))
            }
        }
    }
}
