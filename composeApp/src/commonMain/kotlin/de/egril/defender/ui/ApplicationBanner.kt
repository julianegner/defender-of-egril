package de.egril.defender.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.black_shield
import defender_of_egril.composeapp.generated.resources.black_shield2
import defender_of_egril.composeapp.generated.resources.greatvibes_regular
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

/**
 * Application banner component that displays the game title with the application logo.
 * Layout: Two rows of text on the left, logo on the right.
 * - First row: "Defender of" in Great Vibes handwritten font
 * - Second row: "Egril" in larger Great Vibes handwritten font
 * - Right side: Application logo (shield with crossed swords)
 */
@Composable
fun ApplicationBanner(
    modifier: Modifier = Modifier
) {
    // Load the Great Vibes font
    val greatVibesFont = FontFamily(Font(Res.font.greatvibes_regular))
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
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
