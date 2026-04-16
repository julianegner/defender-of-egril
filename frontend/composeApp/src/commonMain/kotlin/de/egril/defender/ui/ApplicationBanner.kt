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
                    // Place icons at 55 % of canvas height so battlements/ears sit above the
                    // mid-line and feet/base sit below, both within the canvas bounds.
                    val centerY = ch * 0.55f

                    // enemySize is passed to drawXxxSymbol; the ears extend ±0.45*enemySize
                    // from center, so visual half-width ≈ cw * 0.22 * 0.45 ≈ cw * 0.099.
                    val enemySize = cw * 0.22f
                    // towerIconSize is passed to drawTower; the base bottom half-width is
                    // towerIconSize * 0.8 * 0.3 ≈ cw * 0.44 * 0.24 ≈ cw * 0.106.
                    val towerIconSize = cw * 0.44f

                    // --- Enemy symbols: left side, ~25 % overlap left-to-right ---
                    // Step between centers = 0.15 * cw ≈ 77 % of visual width → ~23 % overlap.
                    // First center at 0.10 * cw leaves enemy visual half-width (0.099) of margin.
                    drawGoblinSymbol(cw * 0.10f, centerY, enemySize, outlineColor)
                    drawOrkSymbol(   cw * 0.25f, centerY, enemySize, outlineColor)
                    drawEvilWizardSymbol(cw * 0.40f, centerY, enemySize, outlineColor)

                    // --- Tower symbols: right side, ~25 % overlap left-to-right ---
                    // Gap between last enemy right edge (0.40+0.099 ≈ 0.499) and first tower
                    // left edge (0.655−0.106 ≈ 0.549) = ~0.05 * cw — clearly visible.
                    drawTower(DefenderType.BOW_TOWER, cw * 0.655f, centerY, towerIconSize, lineColor)

                    // Draw background trapezoid before wizard tower so the bow tower body
                    // does not show through its transparent interior.
                    val wizardCenterX = cw * 0.815f
                    val wizardCenterY = centerY
                    val wizardBaseSize = towerIconSize * 0.8f
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

                    drawTower(DefenderType.WIZARD_TOWER, wizardCenterX, wizardCenterY, towerIconSize, lineColor)
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
