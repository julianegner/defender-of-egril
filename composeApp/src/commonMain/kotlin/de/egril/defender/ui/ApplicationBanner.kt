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
import de.egril.defender.utils.isPlatformMobile
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
    
    // Platform-specific spacing values
    // Mobile (Android/iOS) needs more spacing to prevent overlap
    // Desktop and WASM use original tighter spacing

    val elementOffsetX = if (isPlatformMobile) 60f else 0f
    val elementOffsetY = if (isPlatformMobile) -80f else 0f

    val goblinOffsetX = elementOffsetX + (if (isPlatformMobile) 15f else 20f)
    val goblinOffsetY = elementOffsetY + (if (isPlatformMobile) -15f else -20f)
    val orkOffsetX = elementOffsetX + (if (isPlatformMobile) -50f else 0f)
    val orkOffsetY = elementOffsetY + (if (isPlatformMobile) 20f else -10f)
    val wizardOffsetX = elementOffsetX + (if (isPlatformMobile) -100f else -20f)
    val wizardOffsetY = elementOffsetY + (if (isPlatformMobile) 60f else 0f)

    val bowTowerOffsetX = elementOffsetX + (if (isPlatformMobile) 160f else 80f)
    val bowTowerOffsetY = elementOffsetY + (if (isPlatformMobile) 0f else -20f)
    val wizardTowerOffsetX = elementOffsetX + (if (isPlatformMobile) 240f else 100f)
    val wizardTowerOffsetY = elementOffsetY + (if (isPlatformMobile) 60f else 0f)

    val spacerWidth = (if (isPlatformMobile) 80.dp else 80.dp) * scale
    val canvasWidth = (if (isPlatformMobile) 80.dp else 80.dp) * scale
    
    // Calculate banner width from component widths to prevent stretching on different screen sizes
    // Components: Canvas (80dp) + Spacer (80dp) + Text (~200dp) + Spacer (24dp) + Shield (120dp)
    val textApproximateWidth = 200.dp * scale  // Approximate width for "Defender of Egril" text
    val totalBannerWidth = canvasWidth + spacerWidth + textApproximateWidth + 24.dp * scale + 120.dp * scale
    
    // Fixed width container to prevent banner stretching on different screen sizes
    // Uses widthIn to cap maximum width while adapting to smaller screens
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = totalBannerWidth)
                .padding(horizontal = 8.dp),  // Prevent clipping on narrow screens
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
        // Canvas with enemy and tower symbols
        Box(
            modifier = Modifier
                .height(80.dp * scale)
                .width(canvasWidth)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = (size.height / 2) + 20f
                val iconSize = minOf(size.width, size.height)

                // Draw enemy symbols with theme-aware outline
                drawGoblinSymbol(centerX.plus(goblinOffsetX), centerY.plus(goblinOffsetY), iconSize * 0.7f, outlineColor)
                drawOrkSymbol(centerX.plus(orkOffsetX), centerY.plus(orkOffsetY), iconSize * 0.7f, outlineColor)
                drawEvilWizardSymbol(centerX.plus(wizardOffsetX), centerY.plus(wizardOffsetY), iconSize * 0.7f, outlineColor)

                // Draw bow tower (platform-specific offset for mobile spacing)
                drawTower(DefenderType.BOW_TOWER, centerX.plus(bowTowerOffsetX), centerY.plus(bowTowerOffsetY), iconSize, lineColor)
                
                // Draw background with same trapezoid shape as wizard tower to prevent bow tower from showing through
                val wizardCenterX = centerX.plus(wizardTowerOffsetX)
                val wizardCenterY = centerY.plus(wizardTowerOffsetY)
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
                
                // Draw wizard tower (platform-specific offset for mobile spacing)
                drawTower(DefenderType.WIZARD_TOWER, centerX.plus(wizardTowerOffsetX), centerY.plus(wizardTowerOffsetY), iconSize, lineColor)
            }
        }
        
        Spacer(modifier = Modifier.width(spacerWidth))
        
        // Left side: Two rows of text
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            // First row: "Defender of" - smaller size, Great Vibes font
            Text(
                text = "Defender of",
                fontSize = (32 * scale).sp,
                fontFamily = greatVibesFont,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            // Second row: "Egril" - larger size, Great Vibes font
            Text(
                text = "Egril",
                fontSize = (56 * scale).sp,
                fontFamily = greatVibesFont,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Spacer(modifier = Modifier.width(24.dp * scale))
        
        // Right side: Application logo
        Image(
            painter = painterResource(Res.drawable.black_shield),
            contentDescription = "Defender of Egril Logo",
            modifier = Modifier.size(120.dp * scale)
        )
        }
    }
}
