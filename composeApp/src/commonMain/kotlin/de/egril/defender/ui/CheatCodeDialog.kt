package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Unified cheat code dialog used in both WorldMapScreen and GamePlayScreen.
 * Supports both internal state management and external state management.
 */
@Composable
fun CheatCodeDialog(
    onDismiss: () -> Unit,
    onApplyCheatCode: (String) -> Boolean,
    showHints: Boolean = false,
    initialInput: String = "",
    onInputChange: ((String) -> Unit)? = null
) {
    var internalInput by remember { mutableStateOf(initialInput) }
    var errorMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // Use external state if callback provided, otherwise use internal state
    val cheatCodeInput = if (onInputChange != null) initialInput else internalInput
    val handleInputChange: (String) -> Unit = if (onInputChange != null) {
        { newValue ->
            onInputChange(newValue)
            errorMessage = ""
        }
    } else {
        { newValue ->
            internalInput = newValue
            errorMessage = ""
        }
    }
    
    // Function to apply the cheat code
    val applyCheat = {
        val success = onApplyCheatCode(cheatCodeInput)
        if (success) {
            onDismiss()
            if (onInputChange == null) {
                internalInput = ""
            }
            errorMessage = ""
        } else {
            errorMessage = "Invalid cheat code"
        }
    }
    
    // Request focus on the input field when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    AlertDialog(
        onDismissRequest = {
            onDismiss()
            if (onInputChange == null) {
                internalInput = ""
            }
            errorMessage = ""
        },
        title = { Text("Cheat Code") },
        text = {
            Column {
                Text("Enter cheat code:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = cheatCodeInput,
                    onValueChange = handleInputChange,
                    singleLine = true,
                    isError = errorMessage.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            applyCheat()
                        }
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (showHints) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Available codes:", style = MaterialTheme.typography.labelSmall)
                    Text("• cash - Get 1000 coins", style = MaterialTheme.typography.bodySmall)
                    Text("• mmmoney - Get a million coins", style = MaterialTheme.typography.bodySmall)
                    Text("• emptypocket - Set coins to 0", style = MaterialTheme.typography.bodySmall)
                    Text("• dragon - Spawn dragon from mine", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = applyCheat
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    onDismiss()
                    if (onInputChange == null) {
                        internalInput = ""
                    }
                    errorMessage = ""
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Cheat code help screen showing all available cheat codes.
 * Opens when user enters "cheat", "cheats", or "help" as a cheat code.
 */
@Composable
fun CheatCodeHelpScreen(
    onDismiss: () -> Unit,
    isInGameplay: Boolean = false
) {
    de.egril.defender.ui.gameplay.ScrollableInfoCard(
        title = {
            Text(
                text = "Cheat Codes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        onDismiss = onDismiss,
        width = 600.dp,
        maxHeight = 600.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General cheat codes
            CheatSection("General") {
                CheatCodeItem("cheat / cheats / help", "Show this help screen")
                CheatCodeItem("platform", "Show platform information")
            }
            
            if (isInGameplay) {
                // Gameplay cheat codes
                CheatSection("Coins & Resources") {
                    CheatCodeItem("cash", "Add 1,000 coins")
                    CheatCodeItem("mmmoney", "Add 1,000,000 coins")
                    CheatCodeItem("emptypocket", "Set coins to 0")
                }
                
                CheatSection("Mana (During Level)") {
                    CheatCodeItem("addmana <amount>", "Add mana (e.g., addmana 50)")
                    CheatCodeItem("removemana <amount>", "Remove mana (e.g., removemana 20)")
                }
                
                CheatSection("Enemies") {
                    CheatCodeItem("spawn <type> <level>", "Spawn enemy (e.g., spawn goblin 5)")
                    Text("Enemy types: goblin, ork, ogre, skeleton, wizard, greenwitch, redwitch", 
                         style = MaterialTheme.typography.bodySmall,
                         modifier = Modifier.padding(start = 16.dp))
                }
                
                CheatSection("Mining") {
                    CheatCodeItem("dig nothing / dig rubble", "Dig outcome: nothing")
                    CheatCodeItem("dig brass / silver / gold", "Dig outcome: treasure")
                    CheatCodeItem("dig gems / diamond", "Dig outcome: gems/diamond")
                    CheatCodeItem("dig dragon / dragon", "Dig outcome: dragon")
                }
            } else {
                // World map cheat codes
                CheatSection("XP & Stats") {
                    CheatCodeItem("addxp <amount>", "Add XP (e.g., addxp 500)")
                    CheatCodeItem("removexp <amount>", "Remove XP (e.g., removexp 100)")
                    CheatCodeItem("addstat <stat> <amount>", "Add stat levels (e.g., addstat health 5)")
                    CheatCodeItem("removestat <stat> <amount>", "Remove stat levels (e.g., removestat mana 2)")
                    Text("Stats: health, treasury, income, construction, mana", 
                         style = MaterialTheme.typography.bodySmall,
                         modifier = Modifier.padding(start = 16.dp))
                }
                
                CheatSection("Spells") {
                    CheatCodeItem("unlockspell <spell>", "Unlock a spell (e.g., unlockspell fireball)")
                    CheatCodeItem("lockspell <spell>", "Lock a spell (e.g., lockspell heal)")
                    Text("Spells: attack_aimed, attack_area, heal, instant_tower, bomb, double_level, cooling, freeze, double_reach", 
                         style = MaterialTheme.typography.bodySmall,
                         modifier = Modifier.padding(start = 16.dp))
                }
                
                CheatSection("Level Unlocking") {
                    CheatCodeItem("unlockall / unlock all", "Unlock all levels")
                    CheatCodeItem("unlock / unlock <level>", "Unlock specific level")
                    CheatCodeItem("lockall / lock all", "Lock all levels")
                    CheatCodeItem("lock <level>", "Lock specific level")
                }
            }
        }
    }
}

@Composable
private fun CheatSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Divider(modifier = Modifier.padding(vertical = 4.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun CheatCodeItem(
    code: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.5f)
        )
    }
}
