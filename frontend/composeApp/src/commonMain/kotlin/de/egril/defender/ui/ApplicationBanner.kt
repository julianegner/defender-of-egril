package de.egril.defender.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.icon.defender.*
import de.egril.defender.ui.icon.enemy.*
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.black_shield
import defender_of_egril.composeapp.generated.resources.greatvibes_regular
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

/**
 * Application banner component that displays the game title with the application logo and unit symbols.
 * Layout: Canvas with enemy and tower symbols on the left, two rows of text in the middle, logo on the right.
 * - Canvas: Enemy symbols (goblin, ork, wizard) and tower symbols (bow, wizard)
 * - First row: "Defender of" in Great Vibes handwritten font
 * - Second row: "Egril" in larger Great Vibes handwritten font
 * - Right side: Application logo (shield with crossed swords)
 *
 * @param scale A scale factor applied to all banner dimensions (e.g. 0.7f for 30% smaller).
 */
@Composable
fun ApplicationBanner(
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    // Get theme-aware colors
    val lineColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background
    
    // Determine outline color based on dark mode
    // In dark mode: white outline, in light mode: black outline
    val outlineColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color.White
    } else {
        Color.Black
    }
    
    // Load the Great Vibes font
    val greatVibesFont = FontFamily(Font(Res.font.greatvibes_regular))
    
    // Natural (unscaled) component sizes for banner layout
    // Components: Canvas (160dp) + Spacer (16dp) + Text (~200dp) + Spacer (24dp) + Shield (120dp)
    // naturalTextWidth is an approximation used solely to estimate naturalBannerWidth for the
    // fit-scale calculation; the text Column is not width-capped, so deviations are harmless.
    val naturalCanvasWidth = 160.dp  // Wider canvas so all 5 icons fit without overflow
    val naturalCanvasHeight = 80.dp
    val naturalSpacerWidth = 16.dp
    val naturalTextWidth = 200.dp
    val naturalTextSpacerWidth = 24.dp
    val naturalShieldSize = 120.dp
    val naturalBannerWidth = naturalCanvasWidth + naturalSpacerWidth + naturalTextWidth +
            naturalTextSpacerWidth + naturalShieldSize  // ~520dp

    // Minimum scale below which the banner would become unreadable.
    // 0.5f keeps all elements visible even on very narrow screens (~260dp effective width).
    val minBannerScale = 0.5f

    // Responsive container: measures available width and shrinks the banner to fit on narrow screens
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Calculate effective scale: honour the caller's requested scale but also shrink
        // further if the available width is narrower than the banner's natural size.
        val availableWidth = maxWidth
        val fitScale = (availableWidth / naturalBannerWidth).coerceIn(minBannerScale, 1.0f)
        val effectiveScale = minOf(scale, fitScale)

        val canvasWidth = naturalCanvasWidth * effectiveScale
        val spacerWidth = naturalSpacerWidth * effectiveScale

        Row(
            modifier = Modifier.width(naturalBannerWidth * effectiveScale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Canvas with enemy and tower symbols.
            // All positions are expressed as fractions of size.width / size.height so they
            // scale correctly at every screen density and effectiveScale value.
            Box(
                modifier = Modifier
                    .height(naturalCanvasHeight * effectiveScale)
                    .width(canvasWidth)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cw = size.width   // canvas width in px
                    val ch = size.height  // canvas height in px
                    val centerY = ch * 0.6f  // slightly below vertical center for visual balance

                    // Icon sizes relative to canvas dimensions.
                    // Enemies use 20 % of canvas width; towers are proportionally larger
                    // (same 1/0.7 ratio as the original design).
                    val enemySize = cw * 0.20f
                    val towerSize = enemySize / 0.7f

                    // --- Enemy symbols: left side, slightly overlapping left-to-right ---
                    // Center-to-center step = 16.5 % of cw (~18 % overlap for 20 %-wide icons).
                    drawGoblinSymbol(cw * 0.065f, centerY - enemySize * 0.15f, enemySize, outlineColor)
                    drawOrkSymbol(cw * 0.23f,  centerY,                  enemySize, outlineColor)
                    drawEvilWizardSymbol(cw * 0.395f, centerY + enemySize * 0.08f, enemySize, outlineColor)

                    // --- Tower symbols: right side, slightly overlapping left-to-right ---
                    // A ~12dp gap is left between the last enemy's right edge and the first
                    // tower's left edge, giving a clear visual separation.
                    drawTower(DefenderType.BOW_TOWER, cw * 0.71f, centerY - enemySize * 0.15f, towerSize, lineColor)

                    // Draw background trapezoid before wizard tower so bow tower
                    // doesn't show through its transparent interior.
                    val wizardCenterX = cw * 0.875f
                    val wizardCenterY = centerY
                    val wizardBaseSize = towerSize * 0.8f
                    val topWidth = wizardBaseSize * 0.4f
                    val bottomWidth = wizardBaseSize * 0.6f
                    val towerHeight = wizardBaseSize * 0.6f
                    val top = wizardCenterY - towerHeight / 2
                    val bottom = wizardCenterY + towerHeight / 2

                    val trapezoid = Path().apply {
                        moveTo(wizardCenterX - bottomWidth / 2, bottom)
                        lineTo(wizardCenterX + bottomWidth / 2, bottom)
                        lineTo(wizardCenterX + topWidth / 2, top)
                        lineTo(wizardCenterX - topWidth / 2, top)
                        close()
                    }
                    drawPath(trapezoid, backgroundColor)

                    // Battlements with background color
                    val battlement = wizardBaseSize * 0.08f
                    for (i in 0..2) {
                        val x = wizardCenterX - topWidth / 2 + (topWidth / 3) * i
                        drawRect(
                            color = backgroundColor,
                            topLeft = Offset(x, top - battlement),
                            size = androidx.compose.ui.geometry.Size(battlement, battlement)
                        )
                    }

                    drawTower(DefenderType.WIZARD_TOWER, wizardCenterX, wizardCenterY, towerSize, lineColor)
                }
            }

            Spacer(modifier = Modifier.width(spacerWidth))

            // Middle: Two rows of text
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // First row: "Defender of" - smaller size, Great Vibes font
                Text(
                    text = "Defender of",
                    fontSize = (32 * effectiveScale).sp,
                    fontFamily = greatVibesFont,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Second row: "Egril" - larger size, Great Vibes font
                Text(
                    text = "Egril",
                    fontSize = (56 * effectiveScale).sp,
                    fontFamily = greatVibesFont,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(naturalTextSpacerWidth * effectiveScale))

            // Right side: Application logo
            Image(
                painter = painterResource(Res.drawable.black_shield),
                contentDescription = "Defender of Egril Logo",
                modifier = Modifier.size(naturalShieldSize * effectiveScale)
            )
        }
    }
}
