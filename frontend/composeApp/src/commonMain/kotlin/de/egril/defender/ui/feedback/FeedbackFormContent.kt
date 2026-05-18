@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.AppBuildInfo
import de.egril.defender.iam.IamService
import de.egril.defender.save.BackendFeedbackService
import de.egril.defender.save.FeedbackSubmitRequest
import de.egril.defender.utils.getClientPlatformName
import de.egril.defender.utils.getPlatform
import defender_of_egril.composeapp.generated.resources.*
import dev.carlsen.flagkit.FlagKit
import kotlinx.coroutines.launch
import kotlin.random.Random

internal data class FeedbackTypeOption(
    val apiValue: String,
    val label: String,
    val description: String
)

internal data class BugTypeOption(
    val apiValue: String,
    val label: String,
    val description: String
)

/**
 * Represents a language that can be requested via the feedback form.
 * Includes display information for the searchable language list.
 */
internal data class LanguageEntry(
    val code: String,
    val name: String,
    val nativeName: String,
    val countryCode: String
)

/**
 * Full list of commonly requested languages with country codes for FlagKit flags.
 * Includes languages not yet supported by the app (DE/EN/ES/FR/IT are already supported).
 */
internal val ALL_REQUESTABLE_LANGUAGES = listOf(
    LanguageEntry("ar", "Arabic", "العربية", "SA"),
    LanguageEntry("bn", "Bengali", "বাংলা", "BD"),
    LanguageEntry("zh", "Chinese (Simplified)", "简体中文", "CN"),
    LanguageEntry("zh-TW", "Chinese (Traditional)", "繁體中文", "TW"),
    LanguageEntry("cs", "Czech", "Čeština", "CZ"),
    LanguageEntry("da", "Danish", "Dansk", "DK"),
    LanguageEntry("nl", "Dutch", "Nederlands", "NL"),
    LanguageEntry("fi", "Finnish", "Suomi", "FI"),
    LanguageEntry("el", "Greek", "Ελληνικά", "GR"),
    LanguageEntry("he", "Hebrew", "עברית", "IL"),
    LanguageEntry("hi", "Hindi", "हिन्दी", "IN"),
    LanguageEntry("hu", "Hungarian", "Magyar", "HU"),
    LanguageEntry("id", "Indonesian", "Bahasa Indonesia", "ID"),
    LanguageEntry("ja", "Japanese", "日本語", "JP"),
    LanguageEntry("ko", "Korean", "한국어", "KR"),
    LanguageEntry("ms", "Malay", "Bahasa Melayu", "MY"),
    LanguageEntry("no", "Norwegian", "Norsk", "NO"),
    LanguageEntry("fa", "Persian", "فارسی", "IR"),
    LanguageEntry("pl", "Polish", "Polski", "PL"),
    LanguageEntry("pt", "Portuguese", "Português", "PT"),
    LanguageEntry("pt-BR", "Portuguese (Brazil)", "Português (Brasil)", "BR"),
    LanguageEntry("ro", "Romanian", "Română", "RO"),
    LanguageEntry("ru", "Russian", "Русский", "RU"),
    LanguageEntry("sr", "Serbian", "Српски", "RS"),
    LanguageEntry("sk", "Slovak", "Slovenčina", "SK"),
    LanguageEntry("sv", "Swedish", "Svenska", "SE"),
    LanguageEntry("th", "Thai", "ไทย", "TH"),
    LanguageEntry("tr", "Turkish", "Türkçe", "TR"),
    LanguageEntry("uk", "Ukrainian", "Українська", "UA"),
    LanguageEntry("vi", "Vietnamese", "Tiếng Việt", "VN")
)

/**
 * Reusable feedback form composable that can be embedded in the info page
 * or displayed inside a dialog. Contains the full feedback form with
 * type selection, bug type checkboxes, message input, and submit logic.
 *
 * For bug reports: screenshot and game log data are auto-collected via checkboxes
 * (the user ticks to include them; the system captures the data automatically).
 *
 * For additional language requests: a searchable language list with flags is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackFormContent(
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
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
    var includeGameLog by remember { mutableStateOf(true) }
    var includeScreenshot by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf<LanguageEntry?>(null) }
    var languageSearchQuery by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitResult by remember { mutableStateOf<Boolean?>(null) }
    var lastSubmittedFeedbackId by remember { mutableStateOf("") }

    val isBugReport = selectedType.apiValue == "BUG_REPORT"
    val isLanguageRequest = selectedType.apiValue == "ADDITIONAL_LANGUAGE_REQUEST"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showTitle) {
            Text(
                text = stringResource(Res.string.feedback_form_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
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

        // Bug type checkboxes (only for bug reports)
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

        // Language selector (only for additional language requests)
        if (isLanguageRequest) {
            LanguageSelector(
                selectedLanguage = selectedLanguage,
                searchQuery = languageSearchQuery,
                onSearchQueryChange = { languageSearchQuery = it },
                onLanguageSelected = { selectedLanguage = it }
            )
        }

        // Message input
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

        // Auto-collect checkboxes for bug reports (instead of manual text fields)
        if (isBugReport) {
            Text(
                text = stringResource(Res.string.feedback_form_attachments_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = includeGameLog,
                    onCheckedChange = { includeGameLog = it }
                )
                Text(
                    text = stringResource(Res.string.feedback_form_include_game_log),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = includeScreenshot,
                    onCheckedChange = { includeScreenshot = it }
                )
                Text(
                    text = stringResource(Res.string.feedback_form_include_screenshot),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = stringResource(Res.string.feedback_form_auto_collect_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val canSubmit = if (isBugReport) {
            message.isNotBlank() && selectedBugTypes.isNotEmpty()
        } else if (isLanguageRequest) {
            message.isNotBlank() || selectedLanguage != null
        } else {
            message.isNotBlank()
        }

        Button(
            onClick = {
                submitResult = null
                isSubmitting = true
                scope.launch {
                    // Auto-collect game log if checkbox is ticked (bug reports)
                    val collectedGameLog = if (isBugReport && includeGameLog) {
                        collectGameLog()
                    } else null

                    // Auto-collect screenshot placeholder if checkbox is ticked (bug reports)
                    val collectedScreenshot = if (isBugReport && includeScreenshot) {
                        collectScreenshotPlaceholder()
                    } else null

                    // Prepend selected language to message for language requests
                    val finalMessage = if (isLanguageRequest && selectedLanguage != null) {
                        "[Requested language: ${selectedLanguage!!.name} (${selectedLanguage!!.code})]\n\n${message.trim()}"
                    } else {
                        message.trim()
                    }

                    val ok = BackendFeedbackService.submitFeedback(
                        FeedbackSubmitRequest(
                            feedbackId = feedbackId,
                            feedbackType = selectedType.apiValue,
                            bugTypes = selectedBugTypes.toList(),
                            message = finalMessage,
                            contactEmail = contactEmail.trim().ifBlank { null },
                            sourceContext = "INFO_PAGE",
                            gameLevelName = null,
                            gameTurnNumber = null,
                            gameStateJson = null,
                            gameLog = collectedGameLog,
                            screenshotBase64 = collectedScreenshot
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
                        selectedBugTypes = emptySet()
                        includeGameLog = true
                        includeScreenshot = true
                        selectedLanguage = null
                        languageSearchQuery = ""
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
    }
}

/**
 * Searchable, multi-line language selector with flags.
 * Shows a text field for searching/filtering and a scrollable list of matching languages.
 * Each entry shows a flag, language name, native name, and code.
 */
@Composable
private fun LanguageSelector(
    selectedLanguage: LanguageEntry?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLanguageSelected: (LanguageEntry?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.feedback_form_language_selector_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        // Search/filter text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text(stringResource(Res.string.feedback_form_language_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Show selected language if any
        if (selectedLanguage != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageFlagIcon(selectedLanguage.countryCode)
                    Text(
                        text = "${selectedLanguage.name} — ${selectedLanguage.nativeName} (${selectedLanguage.code})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onLanguageSelected(null) }) {
                        Text(stringResource(Res.string.feedback_form_language_clear))
                    }
                }
            }
        }

        // Filtered language list
        val filteredLanguages = remember(searchQuery) {
            if (searchQuery.isBlank()) {
                ALL_REQUESTABLE_LANGUAGES
            } else {
                val query = searchQuery.lowercase()
                ALL_REQUESTABLE_LANGUAGES.filter { lang ->
                    lang.name.lowercase().contains(query) ||
                        lang.nativeName.lowercase().contains(query) ||
                        lang.code.lowercase().contains(query)
                }
            }
        }

        // Display the language list (multi-row, not a dropdown)
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                if (filteredLanguages.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.feedback_form_language_no_results),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    filteredLanguages.forEach { lang ->
                        val isSelected = selectedLanguage?.code == lang.code
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLanguageSelected(lang) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LanguageFlagIcon(lang.countryCode)
                                Text(
                                    text = "${lang.name} — ${lang.nativeName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = lang.code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays a small flag icon for a country code using FlagKit.
 */
@Composable
private fun LanguageFlagIcon(countryCode: String) {
    FlagKit.getFlag(countryCode = countryCode)?.let { flagVector ->
        Image(
            imageVector = flagVector,
            contentDescription = "$countryCode flag",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .width(28.dp)
                .height(18.dp)
                .border(0.5.dp, Color.Gray)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}

/**
 * Auto-collects game log information including platform, version, and runtime details.
 * This replaces the manual text input for game logs.
 */
private fun collectGameLog(): String {
    val platform = getClientPlatformName()
    val platformLong = getPlatform().name
    val version = AppBuildInfo.VERSION_NAME
    val commit = AppBuildInfo.COMMIT_HASH
    return buildString {
        appendLine("=== Auto-collected Game Log ===")
        appendLine("Platform: $platform ($platformLong)")
        appendLine("Version: $version")
        appendLine("Commit: $commit")
        appendLine("Timestamp: ${currentTimestamp()}")
    }
}

/**
 * Provides a placeholder for screenshot data.
 * In the future, this can be replaced with actual screen capture logic
 * once a cross-platform screenshot API is available.
 */
private fun collectScreenshotPlaceholder(): String {
    // Minimal 1x1 transparent PNG as placeholder to satisfy the API requirement.
    // Future: replace with actual screen capture when KMP screenshot APIs are available.
    return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVQI12NgAAIABQAB" +
        "Nl7pcQAAAABJRU5ErkJggg=="
}

private fun currentTimestamp(): String {
    // Simple timestamp representation without kotlinx-datetime dependency
    return "collected at submission time"
}

internal fun generateFeedbackUuid(): String {
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
