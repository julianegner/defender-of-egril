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
 */
@Composable
fun ApplicationBanner(
    modifier: Modifier = Modifier
) {
    // Get theme-aware colors
    val lineColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background
    
    // Load the Great Vibes font
    val greatVibesFont = FontFamily(Font(Res.font.greatvibes_regular))
    
    // Platform-specific spacing values
    // Mobile (Android/iOS) needs more spacing to prevent overlap
    // Desktop and WASM use original tighter spacing
    val bowTowerOffset = if (isPlatformMobile) 100f else 80f
    val wizardTowerOffset = if (isPlatformMobile) 120f else 100f
    val spacerWidth = if (isPlatformMobile) 100.dp else 80.dp
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Canvas with enemy and tower symbols
        Box(
            modifier = Modifier
                .height(80.dp)
                .width(80.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = (size.height / 2) + 20f
                val iconSize = minOf(size.width, size.height)

                // Draw enemy symbols
                drawGoblinSymbol(centerX.plus(20), centerY.minus(20), iconSize * 0.7f)
                drawOrkSymbol(centerX, centerY.minus(10), iconSize * 0.7f)
                drawEvilWizardSymbol(centerX.minus(20), centerY, iconSize * 0.7f)

                // Draw bow tower (platform-specific offset for mobile spacing)
                drawTower(DefenderType.BOW_TOWER, centerX.plus(bowTowerOffset), centerY.minus(20), iconSize, lineColor)
                
                // Draw background with same trapezoid shape as wizard tower to prevent bow tower from showing through
                val wizardCenterX = centerX.plus(wizardTowerOffset)
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
                
                // Draw wizard tower (platform-specific offset for mobile spacing)
                drawTower(DefenderType.WIZARD_TOWER, centerX.plus(wizardTowerOffset), centerY, iconSize, lineColor)
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
                fontSize = 32.sp,
                fontFamily = greatVibesFont,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            // Second row: "Egril" - larger size, Great Vibes font
            Text(
                text = "Egril",
                fontSize = 56.sp,
                fontFamily = greatVibesFont,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // Right side: Application logo
        Image(
            painter = painterResource(Res.drawable.black_shield),
            contentDescription = "Defender of Egril Logo",
            modifier = Modifier.size(120.dp)
        )
    }
}
