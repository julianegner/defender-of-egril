package com.defenderofegril.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.BuildConfig
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.emoji_sword
import defender_of_egril.composeapp.generated.resources.emoji_crown
import defender_of_egril.composeapp.generated.resources.emoji_skull
import org.jetbrains.compose.resources.painterResource

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onShowRules: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Defender of Egril",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Defend the meadows of Egril against\nthe Hordes of Gleid Thyae under\nthe Banner of the evil Ewhad",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onStartGame,
                modifier = Modifier.width(200.dp).height(60.dp)
            ) {
                Text("Start Game", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onShowRules,
                modifier = Modifier.width(200.dp).height(60.dp)
            ) {
                Text("Rules", style = MaterialTheme.typography.titleMedium)
            }
        }
        
        // Version info at the bottom
        Text(
            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_HASH})",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            color = Color.LightGray,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
fun LevelCompleteScreen(
    levelId: Int,
    won: Boolean,
    isLastLevel: Boolean,
    onRestart: () -> Unit,
    onBackToMap: () -> Unit
) {
    // Determine which image/icon and text to show
    val imageResource = when {
        won && isLastLevel -> Res.drawable.emoji_crown  // Crown for winning the game
        won -> Res.drawable.emoji_sword  // Sword for winning a battle
        else -> Res.drawable.emoji_skull  // Skull for defeat
    }
    
    val title = when {
        won && isLastLevel -> "Victory!"
        won -> "Battle Won!"
        else -> "Defeat"
    }
    
    val message = when {
        won && isLastLevel -> "You have successfully defended Egril!"
        won -> "You won this battle!"
        else -> "The enemies have breached your defenses..."
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon/Image
        Text(
            text = icon,
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )

        Image(
            painter = painterResource(imageResource),
            modifier = Modifier.size(14.dp)
        )

        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onRestart,
                modifier = Modifier.width(150.dp).height(50.dp)
            ) {
                Text("Retry")
            }
            
            Button(
                onClick = onBackToMap,
                modifier = Modifier.width(150.dp).height(50.dp)
            ) {
                Text("World Map")
            }
        }
    }
}
