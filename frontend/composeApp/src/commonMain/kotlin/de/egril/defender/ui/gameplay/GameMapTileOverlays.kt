package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.animations.CoolingAreaAnimation
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.settings.AppSettings

@Composable
internal fun BoxScope.TileStatusOverlays(
    gameState: GameState,
    position: Position,
    isInCoolingArea: Boolean,
    coolingAreaTurnsRemaining: Int?,
    isCoolingSpellPreview: Boolean,
    previewDefenderType: DefenderType?,
    mapZoom: () -> Float,
    showTrapPreview: Boolean,
    supportObjectPreviewType: SupportObjectType?,
    selectedMineAction: MineAction?,
    selectedWizardAction: WizardAction?,
) {
    // Show cooling spell snowflake animation on affected tiles
    if (isInCoolingArea) {
        CoolingAreaAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize(),
        )
    }

    // Show turns count for cooling spell (placement preview: 3, active effect: actual remaining)
    val coolingTurns = coolingAreaTurnsRemaining ?: if (isCoolingSpellPreview) 3 else null
    if (coolingTurns != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                "${coolingTurns}T",
                style = MaterialTheme.typography.labelSmall,
                color = TargetCircleConstants.COOLING_SPELL_COLOR,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }

    // Show half-transparent tower icon on hovered build tile.
    // previewDefenderType is non-null only for the 1 cell showing the placement preview
    // (pre-computed in GameGrid to avoid passing selectedDefenderType to all cells).
    if (previewDefenderType != null) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.5f),
            // 50% transparency
            contentAlignment = Alignment.Center,
        ) {
            TowerTypeIcon(
                defenderType = previewDefenderType,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Cost of the previewed tower in the same color as the buy button, centered on the tile.
        // Auto tower base tiles on PATH include the surcharge.
        val previewCost =
            previewDefenderType.baseCost +
                if (gameState.isAutoTowerBasePathSpot(previewDefenderType, position)) {
                    gameState.level.autoTowerBaseExtraCost
                } else {
                    0
                }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1f / mapZoom()
                        scaleY = 1f / mapZoom()
                    },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                MoneyIcon(size = 18.dp)
                Text(
                    text = "$previewCost",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    // Show half-transparent trap icon on hovered path tile (when in trap placement mode)
    if (showTrapPreview ||
        supportObjectPreviewType == SupportObjectType.DWARVEN_TRAP ||
        supportObjectPreviewType == SupportObjectType.MAGICAL_TRAP
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.5f),
            // 50% transparency
            contentAlignment = Alignment.Center,
        ) {
            // Show different icon based on trap type
            when {
                selectedMineAction == MineAction.BUILD_TRAP ||
                    supportObjectPreviewType == SupportObjectType.DWARVEN_TRAP -> {
                    // Dwarven trap - show trap icon
                    TrapIcon(size = GamePlayConstants.TileIconSizes.TrapPreview)
                }
                selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP ||
                    supportObjectPreviewType == SupportObjectType.MAGICAL_TRAP -> {
                    // Magical trap - show pentagram icon
                    PentagramIcon(size = GamePlayConstants.TileIconSizes.TrapPreview)
                }
            }
        }
    }
}
