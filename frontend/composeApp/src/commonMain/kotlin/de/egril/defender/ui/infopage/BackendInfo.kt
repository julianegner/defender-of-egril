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
    val label: String
)

private data class BugTypeOption(
    val apiValue: String,
    val label: String
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
        FeedbackTypeOption("BUG_REPORT", stringResource(Res.string.feedback_type_bug_report)),
        FeedbackTypeOption("TYPO_TRANSLATION_TEXT", stringResource(Res.string.feedback_type_typo_translation_text)),
        FeedbackTypeOption("FEATURE_REQUEST", stringResource(Res.string.feedback_type_feature_request)),
        FeedbackTypeOption("ADDITIONAL_LANGUAGE_REQUEST", stringResource(Res.string.feedback_type_additional_language_request)),
        FeedbackTypeOption("INFO_REQUEST", stringResource(Res.string.feedback_type_info_request)),
        FeedbackTypeOption("LEGAL_PROBLEM", stringResource(Res.string.feedback_type_legal_problem)),
        FeedbackTypeOption("OTHER", stringResource(Res.string.feedback_type_other))
    )
    val bugTypeOptions = listOf(
        BugTypeOption("GRAPHIC", stringResource(Res.string.feedback_bug_type_graphic)),
        BugTypeOption("UI", stringResource(Res.string.feedback_bug_type_ui)),
        BugTypeOption("BEHAVIOUR", stringResource(Res.string.feedback_bug_type_behaviour)),
        BugTypeOption("PERFORMANCE", stringResource(Res.string.feedback_bug_type_performance)),
        BugTypeOption("SOUND", stringResource(Res.string.feedback_bug_type_sound))
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
                        text = { Text(option.label) },
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
                    Text(option.label, modifier = Modifier.padding(top = 12.dp))
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
    val random = Random.Default
    fun nextHex(len: Int): String = buildString {
        repeat(len) {
            append("0123456789abcdef"[random.nextInt(16)])
        }
    }
    return "${nextHex(8)}-${nextHex(4)}-4${nextHex(3)}-${"89ab"[random.nextInt(4)]}${nextHex(3)}-${nextHex(12)}"
}
