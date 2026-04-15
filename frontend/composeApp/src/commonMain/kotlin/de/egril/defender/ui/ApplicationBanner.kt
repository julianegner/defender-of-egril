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
    // Components: Canvas (80dp) + Spacer (80dp) + Text (~200dp) + Spacer (24dp) + Shield (120dp)
    // naturalTextWidth is an approximation of the "Defender of / Egril" text block width.
    // It is used solely to estimate naturalBannerWidth for the fit-scale calculation; the text
    // Column inside the Row is not width-capped — it receives whatever space remains after the
    // other fixed-size elements, so small deviations from the approximation are harmless.
    val naturalCanvasWidth = 80.dp
    val naturalSpacerWidth = 80.dp
    val naturalTextWidth = 200.dp
    val naturalTextSpacerWidth = 24.dp
    val naturalShieldSize = 120.dp
    val naturalBannerWidth = naturalCanvasWidth + naturalSpacerWidth + naturalTextWidth +
            naturalTextSpacerWidth + naturalShieldSize  // ~504dp

    // Minimum scale below which the banner would become unreadable.
    // 0.5f keeps all elements visible even on very narrow screens (~252dp effective width).
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
            // Canvas with enemy and tower symbols
            Box(
                modifier = Modifier
                    .height(naturalCanvasWidth * effectiveScale)
                    .width(canvasWidth)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = (size.height / 2) + 20f
                    // iconSize scales with canvas pixel dimensions — all offsets are
                    // expressed as fractions of iconSize so they remain correct at any
                    // screen density or effective scale.
                    val iconSize = minOf(size.width, size.height)

                    // Draw enemy symbols with theme-aware outline
                    // Offsets as fractions of iconSize so they scale with canvas size
                    drawGoblinSymbol(centerX + iconSize * 0.125f, centerY - iconSize * 0.125f, iconSize * 0.7f, outlineColor)
                    drawOrkSymbol(centerX, centerY - iconSize * 0.0625f, iconSize * 0.7f, outlineColor)
                    drawEvilWizardSymbol(centerX - iconSize * 0.125f, centerY, iconSize * 0.7f, outlineColor)

                    // Draw bow tower
                    drawTower(DefenderType.BOW_TOWER, centerX + iconSize * 0.5f, centerY - iconSize * 0.125f, iconSize, lineColor)

                    // Draw background with same trapezoid shape as wizard tower
                    // to prevent bow tower from showing through
                    val wizardCenterX = centerX + iconSize * 0.625f
                    val wizardCenterY = centerY
                    val wizardBaseSize = iconSize * 0.8f
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

                    // Draw battlements with background color
                    val battlement = wizardBaseSize * 0.08f
                    for (i in 0..2) {
                        val x = wizardCenterX - topWidth / 2 + (topWidth / 3) * i
                        drawRect(
                            color = backgroundColor,
                            topLeft = Offset(x, top - battlement),
                            size = androidx.compose.ui.geometry.Size(battlement, battlement)
                        )
                    }

                    // Draw wizard tower
                    drawTower(DefenderType.WIZARD_TOWER, wizardCenterX, wizardCenterY, iconSize, lineColor)
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
