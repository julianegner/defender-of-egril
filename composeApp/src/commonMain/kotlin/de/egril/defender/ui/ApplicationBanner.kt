package de.egril.defender.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource

/**
 * Application banner component that displays the game title with the application logo.
 * Layout: Two rows of text on the left, logo on the right.
 * - First row: "Defender of" in flowing handwritten-style font
 * - Second row: "Egril" in larger flowing handwritten-style font
 * - Right side: Application logo (shield with crossed swords)
 */
@Composable
fun ApplicationBanner(
    modifier: Modifier = Modifier
) {
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
            // First row: "Defender of" - smaller size, handwritten style
            Text(
                text = "Defender of",
                fontSize = 32.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            // Second row: "Egril" - larger size, handwritten style
            Text(
                text = "Egril",
                fontSize = 56.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // Right side: Application logo
        Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = "Defender of Egril Logo",
            modifier = Modifier.size(120.dp)
        )
    }
}
