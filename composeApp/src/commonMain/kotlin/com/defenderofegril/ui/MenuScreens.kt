package com.defenderofegril.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onShowRules: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
}

@Composable
fun LevelCompleteScreen(
    levelId: Int,
    won: Boolean,
    onRestart: () -> Unit,
    onBackToMap: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (won) "Victory!" else "Defeat",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (won) 
                "You have successfully defended Egril!" 
            else 
                "The enemies have breached your defenses...",
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
