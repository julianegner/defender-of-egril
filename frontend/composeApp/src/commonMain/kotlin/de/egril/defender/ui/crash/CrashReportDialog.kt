@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hyperether.resources.stringResource
import de.egril.defender.iam.IamService
import de.egril.defender.save.BackendCrashService
import de.egril.defender.save.CrashReportSubmitRequest
import de.egril.defender.save.serializeSettingsJson
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.CrashInfo
import de.egril.defender.utils.CrashReporter
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.launch

/**
 * Modal dialog displayed by the global error boundary when an unhandled
 * error has been captured by [CrashReporter]. Blocks the rest of the UI
 * until the user acknowledges it.
 *
 * Behaviour required by the issue:
 *  - localized text describing that an error occurred
 *  - a "send error information to maintainer" checkbox, default ON
 *  - a single OK button
 *  - fully modal (nothing else clickable while open)
 *  - keyboard shortcuts on every actionable control and shortcut chips
 *    rendered when the user has enabled `showButtonShortcutHints`
 *
 * Default shortcuts:
 *  - ENTER / O = acknowledge (OK)
 *  - S         = toggle the "send info" checkbox
 *  - ESC       = acknowledge (treated as OK so the user is never trapped)
 */
@Composable
fun CrashReportDialog(
    crash: CrashInfo,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sendInfo by remember(crash.crashId) { mutableStateOf(true) }
    var submitting by remember(crash.crashId) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(crash.crashId) {
        runCatching { focusRequester.requestFocus() }
    }

    val confirm: () -> Unit = confirm@{
        if (submitting) return@confirm
        if (sendInfo) {
            submitting = true
            scope.launch {
                val settingsJson = runCatching {
                    serializeSettingsJson(AppSettings.toSettingsMap())
                }.getOrNull()
                val gameLog = runCatching {
                    de.egril.defender.config.GameLogBuffer.getFormattedLogs()
                }.getOrNull()
                val token = runCatching { IamService.getToken() }.getOrNull()
                runCatching {
                    BackendCrashService.submitCrashReport(
                        request = CrashReportSubmitRequest(
                            crashId = crash.crashId,
                            errorType = crash.errorType,
                            errorMessage = crash.errorMessage,
                            stackTrace = crash.stackTrace,
                            gameLog = gameLog,
                            settingsJson = settingsJson
                        ),
                        token = token
                    )
                }
                onDismiss()
            }
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { /* modal: must press OK */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 560.dp)
                .focusRequester(focusRequester)
                .focusTarget()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter, Key.O -> { confirm(); true }
                        Key.Escape -> { confirm(); true }
                        Key.S -> { sendInfo = !sendInfo; true }
                        else -> false
                    }
                },
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.crash_dialog_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(Res.string.crash_dialog_message),
                    fontSize = 14.sp
                )
                // Show the error type and message in a selectable, scrollable
                // block so users can copy it for their own records.
                val errorPreview = buildString {
                    append(crash.errorType)
                    if (!crash.errorMessage.isNullOrBlank()) {
                        append(": ")
                        append(crash.errorMessage)
                    }
                }
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = errorPreview,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // "Send error information" checkbox row, toggleable via S.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = sendInfo,
                            onValueChange = { sendInfo = it },
                            role = Role.Checkbox,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = sendInfo, onCheckedChange = null)
                    Text(
                        text = stringResource(Res.string.crash_dialog_send_info_checkbox),
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp
                    )
                    ShortcutKeyChip(text = "S")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Button(
                        onClick = confirm,
                        enabled = !submitting,
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(stringResource(Res.string.ok))
                            ShortcutKeyChip(text = "O", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Global error boundary. Place once at the very top of the composition; it
 * renders [content] normally and overlays a modal [CrashReportDialog] when
 * [CrashReporter] has a captured crash.
 */
@Composable
fun ErrorBoundary(content: @Composable () -> Unit) {
    val crash by CrashReporter.current
    content()
    crash?.let { info ->
        CrashReportDialog(crash = info, onDismiss = { CrashReporter.clear() })
    }
}
