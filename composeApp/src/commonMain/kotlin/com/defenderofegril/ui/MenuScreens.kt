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
import com.defenderofegril.ui.settings.SettingsButton
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage
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
    val locale = currentLanguage.value
    
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Settings button in top-right corner
        SettingsButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = LocalizedStrings.get("app_name", locale),
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = LocalizedStrings.get("app_subtitle", locale),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onStartGame,
                modifier = Modifier.width(200.dp).height(60.dp)
            ) {
                Text(LocalizedStrings.get("start_game", locale), style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onShowRules,
                modifier = Modifier.width(200.dp).height(60.dp)
            ) {
                Text(LocalizedStrings.get("rules", locale), style = MaterialTheme.typography.titleMedium)
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
    val locale = currentLanguage.value
    
    // Determine which image/icon and text to show
    val imageResource = when {
        won && isLastLevel -> Res.drawable.emoji_crown  // Crown for winning the game
        won -> Res.drawable.emoji_sword  // Sword for winning a battle
        else -> Res.drawable.emoji_skull  // Skull for defeat
    }
    
    val title = when {
        won && isLastLevel -> LocalizedStrings.get("victory", locale)
        won -> LocalizedStrings.get("battle_won", locale)
        else -> LocalizedStrings.get("defeat", locale)
    }
    
    val message = when {
        won && isLastLevel -> LocalizedStrings.get("victory_message", locale)
        won -> LocalizedStrings.get("battle_won_message", locale)
        else -> LocalizedStrings.get("defeat_message", locale)
    }
    
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Settings button in top-right corner
        SettingsButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon/Image
            Image(
                painter = painterResource(imageResource),
                contentDescription = title,
                modifier = Modifier.size(64.dp)
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
                    Text(LocalizedStrings.get("retry", locale))
                }
                
                Button(
                    onClick = onBackToMap,
                    modifier = Modifier.width(150.dp).height(50.dp)
                ) {
                    Text(LocalizedStrings.get("world_map", locale))
                }
            }
        }
    }
}
