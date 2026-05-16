@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.iam.IamService
import de.egril.defender.save.BackendFeedbackService
import de.egril.defender.save.FeedbackSubmitRequest
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Composable displaying information about the backend server, user accounts, and logging.
 */
@Composable
fun BackendInfo() {
    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
        Text(
            text = stringResource(Res.string.backend_info_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logging section
            BackendInfoSection(
                heading = stringResource(Res.string.backend_info_logging_heading),
                body = stringResource(Res.string.backend_info_logging_body)
            )

            // Optional account section
            BackendInfoSection(
                heading = stringResource(Res.string.backend_info_account_heading),
                body = stringResource(Res.string.backend_info_account_body)
            )

            // How to get an account section
            BackendInfoSection(
                heading = stringResource(Res.string.backend_info_account_how_heading),
                body = stringResource(Res.string.backend_info_account_how_body)
            )

            BackendInfoSection(
                heading = stringResource(Res.string.backend_info_feedback_heading),
                body = stringResource(Res.string.backend_info_feedback_body)
            )

            FeedbackFormSection()

            Spacer(modifier = Modifier.height(8.dp))
        }
        }
    }
}

private data class FeedbackTypeOption(
    val apiValue: String,
    val label: String,
    val description: String
)

private data class BugTypeOption(
    val apiValue: String,
    val label: String,
    val description: String
)

@Composable
private fun BackendInfoSection(
    heading: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackFormSection() {
    val scope = rememberCoroutineScope()
    var feedbackId by remember { mutableStateOf(generateFeedbackUuid()) }
    val feedbackTypes = listOf(
        FeedbackTypeOption("BUG_REPORT", stringResource(Res.string.feedback_type_bug_report), stringResource(Res.string.feedback_type_bug_report_desc)),
        FeedbackTypeOption("TYPO_TRANSLATION_TEXT", stringResource(Res.string.feedback_type_typo_translation_text), stringResource(Res.string.feedback_type_typo_translation_text_desc)),
        FeedbackTypeOption("FEATURE_REQUEST", stringResource(Res.string.feedback_type_feature_request), stringResource(Res.string.feedback_type_feature_request_desc)),
        FeedbackTypeOption("ADDITIONAL_LANGUAGE_REQUEST", stringResource(Res.string.feedback_type_additional_language_request), stringResource(Res.string.feedback_type_additional_language_request_desc)),
        FeedbackTypeOption("INFO_REQUEST", stringResource(Res.string.feedback_type_info_request), stringResource(Res.string.feedback_type_info_request_desc)),
        FeedbackTypeOption("LEGAL_PROBLEM", stringResource(Res.string.feedback_type_legal_problem), stringResource(Res.string.feedback_type_legal_problem_desc)),
        FeedbackTypeOption("OTHER", stringResource(Res.string.feedback_type_other), stringResource(Res.string.feedback_type_other_desc))
    )
    val bugTypeOptions = listOf(
        BugTypeOption("VISUAL", stringResource(Res.string.feedback_bug_type_visual), stringResource(Res.string.feedback_bug_type_visual_desc)),
        BugTypeOption("UI", stringResource(Res.string.feedback_bug_type_ui), stringResource(Res.string.feedback_bug_type_ui_desc)),
        BugTypeOption("GAMEPLAY", stringResource(Res.string.feedback_bug_type_gameplay), stringResource(Res.string.feedback_bug_type_gameplay_desc)),
        BugTypeOption("PERFORMANCE", stringResource(Res.string.feedback_bug_type_performance), stringResource(Res.string.feedback_bug_type_performance_desc)),
        BugTypeOption("SOUND", stringResource(Res.string.feedback_bug_type_sound), stringResource(Res.string.feedback_bug_type_sound_desc)),
        BugTypeOption("CRASH", stringResource(Res.string.feedback_bug_type_crash), stringResource(Res.string.feedback_bug_type_crash_desc))
    )
    var selectedType by remember { mutableStateOf(feedbackTypes.first()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedBugTypes by remember { mutableStateOf(setOf<String>()) }
    var message by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var gameLog by remember { mutableStateOf("") }
    var screenshotBase64 by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitResult by remember { mutableStateOf<Boolean?>(null) }
    var lastSubmittedFeedbackId by remember { mutableStateOf("") }

    val isBugReport = selectedType.apiValue == "BUG_REPORT"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.feedback_form_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(Res.string.feedback_form_github_hint),
            style = MaterialTheme.typography.bodyMedium
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedType.label,
                onValueChange = {},
                label = { Text(stringResource(Res.string.feedback_form_type_label)) },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                feedbackTypes.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(option.label, fontWeight = FontWeight.Bold)
                                Text(
                                    option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            selectedType = option
                            if (selectedType.apiValue != "BUG_REPORT") {
                                selectedBugTypes = emptySet()
                            }
                            expanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = selectedType.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isBugReport) {
            Text(
                text = stringResource(Res.string.feedback_form_bug_types_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            bugTypeOptions.forEach { option ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = selectedBugTypes.contains(option.apiValue),
                        onCheckedChange = { checked ->
                            selectedBugTypes = if (checked) {
                                selectedBugTypes + option.apiValue
                            } else {
                                selectedBugTypes - option.apiValue
                            }
                        }
                    )
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(option.label, fontWeight = FontWeight.Bold)
                        Text(
                            option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text(stringResource(Res.string.feedback_form_message_label)) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = contactEmail,
            onValueChange = { contactEmail = it },
            label = { Text(stringResource(Res.string.feedback_form_contact_email_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = gameLog,
            onValueChange = { gameLog = it },
            label = { Text(stringResource(Res.string.feedback_form_game_log_label)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = screenshotBase64,
            onValueChange = { screenshotBase64 = it },
            label = { Text(stringResource(Res.string.feedback_form_screenshot_base64_label)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        val canSubmit = if (isBugReport) {
            message.isNotBlank() && gameLog.isNotBlank() && screenshotBase64.isNotBlank() && selectedBugTypes.isNotEmpty()
        } else {
            message.isNotBlank()
        }

        Button(
            onClick = {
                submitResult = null
                isSubmitting = true
                scope.launch {
                    val ok = BackendFeedbackService.submitFeedback(
                        FeedbackSubmitRequest(
                            feedbackId = feedbackId,
                            feedbackType = selectedType.apiValue,
                            bugTypes = selectedBugTypes.toList(),
                            message = message.trim(),
                            contactEmail = contactEmail.trim().ifBlank { null },
                            sourceContext = "INFO_PAGE",
                            gameLevelName = null,
                            gameTurnNumber = null,
                            gameStateJson = null,
                            gameLog = gameLog.ifBlank { null },
                            screenshotBase64 = screenshotBase64.ifBlank { null }
                        ),
                        token = IamService.getToken()
                    )
                    submitResult = ok
                    isSubmitting = false
                    if (ok) {
                        lastSubmittedFeedbackId = feedbackId
                        feedbackId = generateFeedbackUuid()
                        message = ""
                        contactEmail = ""
                        gameLog = ""
                        screenshotBase64 = ""
                        selectedBugTypes = emptySet()
                    }
                }
            },
            enabled = !isSubmitting && canSubmit
        ) {
            Text(stringResource(Res.string.feedback_form_submit))
        }

        if (submitResult == true) {
            Text(
                text = stringResource(Res.string.feedback_form_submit_success, lastSubmittedFeedbackId),
                color = MaterialTheme.colorScheme.primary
            )
        } else if (submitResult == false) {
            Text(
                text = stringResource(Res.string.feedback_form_submit_error),
                color = MaterialTheme.colorScheme.error
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun generateFeedbackUuid(): String {
    val bytes = ByteArray(16) { Random.Default.nextInt(256).toByte() }
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte() // version 4
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() // variant 10xx
    val hexChars = "0123456789abcdef"
    return buildString {
        bytes.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            append(hexChars[value ushr 4])
            append(hexChars[value and 0x0F])
            if (index == 3 || index == 5 || index == 7 || index == 9) append('-')
        }
    }
}
