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
import defenderofegril.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

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
                text = stringResource(Res.string.cheat_codes_title),
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
            CheatSection(stringResource(Res.string.cheat_section_general)) {
                CheatCodeItem("cheat / cheats / help", stringResource(Res.string.cheat_help_desc))
                CheatCodeItem("platform", stringResource(Res.string.cheat_platform_desc))
            }
            
            if (isInGameplay) {
                // Gameplay cheat codes
                CheatSection(stringResource(Res.string.cheat_section_coins)) {
                    CheatCodeItem("cash", stringResource(Res.string.cheat_cash_desc))
                    CheatCodeItem("mmmoney", stringResource(Res.string.cheat_mmmoney_desc))
                    CheatCodeItem("emptypocket", stringResource(Res.string.cheat_emptypocket_desc))
                }
                
                CheatSection(stringResource(Res.string.cheat_section_mana)) {
                    CheatCodeItem("addmana <amount>", stringResource(Res.string.cheat_addmana_desc))
                    CheatCodeItem("removemana <amount>", stringResource(Res.string.cheat_removemana_desc))
                }
                
                CheatSection(stringResource(Res.string.cheat_section_enemies)) {
                    CheatCodeItem("spawn <type> <level>", stringResource(Res.string.cheat_spawn_desc))
                    Text(stringResource(Res.string.cheat_spawn_types), 
                         style = MaterialTheme.typography.bodySmall,
                         modifier = Modifier.padding(start = 16.dp))
                }
                
                CheatSection(stringResource(Res.string.cheat_section_mining)) {
                    CheatCodeItem("dig nothing / dig rubble", stringResource(Res.string.cheat_dig_nothing_desc))
                    CheatCodeItem("dig brass / silver / gold", stringResource(Res.string.cheat_dig_treasure_desc))
                    CheatCodeItem("dig gems / diamond", stringResource(Res.string.cheat_dig_gems_desc))
                    CheatCodeItem("dig dragon / dragon", stringResource(Res.string.cheat_dig_dragon_desc))
                }
            } else {
                // World map cheat codes
                CheatSection(stringResource(Res.string.cheat_section_xp_stats)) {
                    CheatCodeItem("addxp <amount>", stringResource(Res.string.cheat_addxp_desc))
                    CheatCodeItem("removexp <amount>", stringResource(Res.string.cheat_removexp_desc))
                    CheatCodeItem("addstat <stat> <amount>", stringResource(Res.string.cheat_addstat_desc))
                    CheatCodeItem("removestat <stat> <amount>", stringResource(Res.string.cheat_removestat_desc))
                    Text(stringResource(Res.string.cheat_stat_types), 
                         style = MaterialTheme.typography.bodySmall,
                         modifier = Modifier.padding(start = 16.dp))
                }
                
                CheatSection(stringResource(Res.string.cheat_section_spells)) {
                    CheatCodeItem("unlockspell <spell>", stringResource(Res.string.cheat_unlockspell_desc))
                    CheatCodeItem("lockspell <spell>", stringResource(Res.string.cheat_lockspell_desc))
                    Text(stringResource(Res.string.cheat_spell_types), 
                         style = MaterialTheme.typography.bodySmall,
                         modifier = Modifier.padding(start = 16.dp))
                }
                
                CheatSection(stringResource(Res.string.cheat_section_levels)) {
                    CheatCodeItem("unlockall / unlock all", stringResource(Res.string.cheat_unlockall_desc))
                    CheatCodeItem("unlock / unlock <level>", stringResource(Res.string.cheat_unlock_desc))
                    CheatCodeItem("lockall / lock all", stringResource(Res.string.cheat_lockall_desc))
                    CheatCodeItem("lock <level>", stringResource(Res.string.cheat_lock_desc))
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
